package com.traffic.echallan.service;

import com.traffic.echallan.dto.Dtos.*;
import com.traffic.echallan.entity.*;
import com.traffic.echallan.exception.BadRequestException;
import com.traffic.echallan.exception.ConflictException;
import com.traffic.echallan.exception.NotFoundException;
import com.traffic.echallan.repository.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChallanService {
    private final ChallanRepository challans;
    private final VehicleRepository vehicles;
    private final ViolationTypeRepository types;
    private final UserRepository users;
    private final PenaltyRuleRepository rules;
    private final NotificationService notifications;

    @Value("${echallan.rolling-window-months:12}")
    private int windowMonths;
    @Value("${echallan.due-days:15}")
    private int dueDays;

    /** Issue a challan: validates, counts prior violations in the rolling window, calculates and stores the fine. */
    @Transactional
    public ChallanResponse issue(ChallanRequest req, String officerUsername) {
        Vehicle vehicle = vehicles.findById(req.vehicleId())
                .orElseThrow(() -> new NotFoundException("Vehicle not found: " + req.vehicleId()));
        ViolationType type = types.findById(req.violationTypeId())
                .orElseThrow(() -> new NotFoundException("Violation type not found: " + req.violationTypeId()));
        if (!type.isActive()) throw new BadRequestException("Violation type " + type.getCode() + " is inactive");
        User officer = users.findByUsername(officerUsername)
                .orElseThrow(() -> new NotFoundException("Issuing officer not found: " + officerUsername));

        LocalDateTime at = req.violationDateTime().withNano(0);
        if (challans.existsByVehicleIdAndViolationTypeIdAndViolationDateTime(vehicle.getId(), type.getId(), at))
            throw new ConflictException("Duplicate challan: this vehicle already has this violation recorded at " + at);

        // Repeat count: same vehicle + same violation type + rolling window ending at the violation time
        LocalDateTime from = FineCalculator.windowStart(at, windowMonths);
        long prior = challans.countByVehicleIdAndViolationTypeIdAndViolationDateTimeBetween(vehicle.getId(), type.getId(), from, at);
        int occurrence = (int) prior + 1;

        BigDecimal multiplier = FineCalculator.multiplierFor(occurrence, ruleMap(type.getId()));
        BigDecimal fine = FineCalculator.finalFine(type.getBaseFine(), multiplier);

        Challan c = new Challan();
        c.setChallanNumber(nextChallanNumber());
        c.setVehicle(vehicle);
        c.setViolationType(type);
        c.setOfficer(officer);
        c.setViolationDateTime(at);
        c.setLocation(req.location().trim());
        c.setRemarks(req.remarks());
        c.setBaseFine(type.getBaseFine());
        c.setMultiplier(multiplier);
        c.setFinalFine(fine);
        c.setOccurrenceNumber(occurrence);
        c.setPreviousViolationCount((int) prior);
        c.setDueDate(LocalDate.now().plusDays(dueDays));
        c = challans.saveAndFlush(c);
        notifications.challanIssued(c);
        return toResponse(c, true);
    }

    public ChallanResponse get(Long id) {
        return toResponse(challans.findById(id).orElseThrow(() -> new NotFoundException("Challan not found: " + id)), true);
    }

    public List<ChallanResponse> historyByVehicle(Long vehicleId) {
        if (!vehicles.existsById(vehicleId)) throw new NotFoundException("Vehicle not found: " + vehicleId);
        return challans.findByVehicleIdOrderByViolationDateTimeDesc(vehicleId).stream().map(c -> toResponse(c, false)).toList();
    }

    public List<ChallanResponse> historyByRegistration(String reg) {
        Vehicle v = vehicles.findByRegistrationNumber(VehicleService.normalize(reg))
                .orElseThrow(() -> new NotFoundException("Vehicle not found: " + VehicleService.normalize(reg)));
        return historyByVehicle(v.getId());
    }

    public PageResponse<ChallanResponse> search(String reg, String number, Long typeId, ChallanStatus status,
                                                LocalDate from, LocalDate to, Pageable pageable) {
        Specification<Challan> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (StringUtils.hasText(reg))
                ps.add(cb.like(root.get("vehicle").get("registrationNumber"), "%" + VehicleService.normalize(reg) + "%"));
            if (StringUtils.hasText(number))
                ps.add(cb.like(cb.upper(root.get("challanNumber")), "%" + number.trim().toUpperCase() + "%"));
            if (typeId != null) ps.add(cb.equal(root.get("violationType").get("id"), typeId));
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            if (from != null) ps.add(cb.greaterThanOrEqualTo(root.get("violationDateTime"), from.atStartOfDay()));
            if (to != null) ps.add(cb.lessThan(root.get("violationDateTime"), to.plusDays(1).atStartOfDay()));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<Challan> page = challans.findAll(spec, pageable);
        return new PageResponse<>(page.map(c -> toResponse(c, false)).getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    // ---- helpers ----
    /** Type-specific active rules win; otherwise the global (violationType = null) rules are used. */
    private Map<Integer, BigDecimal> ruleMap(Long typeId) {
        List<PenaltyRule> list = rules.findByViolationTypeIdAndActiveTrue(typeId);
        if (list.isEmpty()) list = rules.findByViolationTypeIsNullAndActiveTrue();
        Map<Integer, BigDecimal> map = new HashMap<>();
        list.forEach(r -> map.put(r.getFromOccurrence(), r.getMultiplier()));
        return map;
    }

    private String nextChallanNumber() {
        String number;
        do {
            number = "EC" + LocalDate.now().toString().replace("-", "") + "-"
                    + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        } while (challans.existsByChallanNumber(number));
        return number;
    }

    ChallanResponse toResponse(Challan c, boolean withPrevious) {
        List<PreviousViolation> previous = List.of();
        if (withPrevious) {
            LocalDateTime at = c.getViolationDateTime();
            previous = challans.findByVehicleIdAndViolationTypeIdAndViolationDateTimeBetweenOrderByViolationDateTimeAsc(
                            c.getVehicle().getId(), c.getViolationType().getId(), FineCalculator.windowStart(at, windowMonths), at)
                    .stream().filter(p -> !p.getId().equals(c.getId()) && p.getViolationDateTime().isBefore(at))
                    .map(p -> new PreviousViolation(p.getChallanNumber(), p.getViolationDateTime(), p.getFinalFine())).toList();
        }
        return new ChallanResponse(c.getId(), c.getChallanNumber(), c.getVehicle().getId(), c.getVehicle().getRegistrationNumber(),
                c.getVehicle().getOwnerName(), c.getViolationType().getId(), c.getViolationType().getCode(),
                c.getViolationType().getDescription(), c.getOfficer().getId(), c.getOfficer().getFullName(),
                c.getViolationDateTime(), c.getLocation(), c.getRemarks(), c.getStatus(), c.getBaseFine(), c.getMultiplier(),
                c.getFinalFine(), c.getOccurrenceNumber(), c.getPreviousViolationCount(), c.getOccurrenceNumber() > 1,
                c.getDueDate(), c.getIssuedAt(), c.getPaidAt(), previous);
    }
}
