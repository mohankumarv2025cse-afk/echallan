package com.traffic.echallan.service;

import com.traffic.echallan.dto.Dtos.*;
import com.traffic.echallan.entity.Vehicle;
import com.traffic.echallan.exception.ConflictException;
import com.traffic.echallan.exception.NotFoundException;
import com.traffic.echallan.repository.ChallanRepository;
import com.traffic.echallan.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleService {
    private final VehicleRepository vehicles;
    private final ChallanRepository challans;

    public static String normalize(String reg) {
        return reg == null ? "" : reg.trim().toUpperCase().replaceAll("[\\s-]", "");
    }

    @Transactional
    public VehicleResponse create(VehicleRequest r) {
        String reg = normalize(r.registrationNumber());
        if (vehicles.existsByRegistrationNumber(reg))
            throw new ConflictException("Vehicle " + reg + " is already registered");
        Vehicle v = new Vehicle();
        apply(v, r, reg);
        return toResponse(vehicles.save(v));
    }

    @Transactional
    public VehicleResponse update(Long id, VehicleRequest r) {
        Vehicle v = find(id);
        String reg = normalize(r.registrationNumber());
        vehicles.findByRegistrationNumber(reg).ifPresent(other -> {
            if (!other.getId().equals(id)) throw new ConflictException("Vehicle " + reg + " is already registered");
        });
        apply(v, r, reg);
        return toResponse(v);
    }

    @Transactional
    public void delete(Long id) {
        Vehicle v = find(id);
        if (challans.existsByVehicleId(id))
            throw new ConflictException("Cannot delete a vehicle that has challans (audit history must be kept)");
        vehicles.delete(v);
    }

    public VehicleResponse get(Long id) { return toResponse(find(id)); }

    public VehicleResponse getByRegistration(String reg) {
        String n = normalize(reg);
        return toResponse(vehicles.findByRegistrationNumber(n)
                .orElseThrow(() -> new NotFoundException("Vehicle not found: " + n)));
    }

    public PageResponse<VehicleResponse> search(String q, Pageable pageable) {
        String text = q == null ? "" : q.trim();
        Page<Vehicle> page = vehicles.findByRegistrationNumberContainingIgnoreCaseOrOwnerNameContainingIgnoreCase(
                normalize(text), text, pageable);
        return new PageResponse<>(page.map(VehicleService::toResponse).getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    Vehicle find(Long id) {
        return vehicles.findById(id).orElseThrow(() -> new NotFoundException("Vehicle not found: " + id));
    }

    private void apply(Vehicle v, VehicleRequest r, String reg) {
        v.setRegistrationNumber(reg);
        v.setVehicleType(r.vehicleType());
        v.setOwnerName(r.ownerName().trim());
        v.setOwnerContact(r.ownerContact());
        v.setOwnerEmail(r.ownerEmail());
        v.setMake(r.make());
        v.setModel(r.model());
        v.setColor(r.color());
    }

    static VehicleResponse toResponse(Vehicle v) {
        return new VehicleResponse(v.getId(), v.getRegistrationNumber(), v.getVehicleType(), v.getOwnerName(),
                v.getOwnerContact(), v.getOwnerEmail(), v.getMake(), v.getModel(), v.getColor(), v.getCreatedAt());
    }
}
