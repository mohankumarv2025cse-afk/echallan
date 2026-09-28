package com.traffic.echallan.service;

import com.traffic.echallan.dto.Dtos.*;
import com.traffic.echallan.entity.PenaltyRule;
import com.traffic.echallan.entity.ViolationType;
import com.traffic.echallan.exception.BadRequestException;
import com.traffic.echallan.exception.ConflictException;
import com.traffic.echallan.exception.NotFoundException;
import com.traffic.echallan.repository.PenaltyRuleRepository;
import com.traffic.echallan.repository.ViolationTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Violation types and admin-configurable penalty rules. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ViolationService {
    private final ViolationTypeRepository types;
    private final PenaltyRuleRepository rules;

    @Transactional
    public ViolationResponse create(ViolationRequest r) {
        String code = r.code().trim().toUpperCase();
        if (types.existsByCode(code)) throw new ConflictException("Violation code already exists: " + code);
        ViolationType t = new ViolationType();
        apply(t, r, code);
        return toResponse(types.save(t));
    }

    @Transactional
    public ViolationResponse update(Long id, ViolationRequest r) {
        ViolationType t = find(id);
        String code = r.code().trim().toUpperCase();
        types.findByCode(code).ifPresent(o -> {
            if (!o.getId().equals(id)) throw new ConflictException("Violation code already exists: " + code);
        });
        apply(t, r, code);   // old challans keep their stored fine snapshot
        return toResponse(t);
    }

    public List<ViolationResponse> list() { return types.findAll().stream().map(ViolationService::toResponse).toList(); }
    public ViolationResponse get(Long id) { return toResponse(find(id)); }

    ViolationType find(Long id) {
        return types.findById(id).orElseThrow(() -> new NotFoundException("Violation type not found: " + id));
    }

    // ---- penalty rules ----
    @Transactional
    public PenaltyRuleResponse createRule(PenaltyRuleRequest r) {
        ViolationType t = r.violationTypeId() == null ? null : find(r.violationTypeId());
        ensureNoDuplicate(t, r.fromOccurrence(), null);
        PenaltyRule rule = new PenaltyRule();
        applyRule(rule, t, r);
        return toResponse(rules.save(rule));
    }

    @Transactional
    public PenaltyRuleResponse updateRule(Long id, PenaltyRuleRequest r) {
        PenaltyRule rule = rules.findById(id).orElseThrow(() -> new NotFoundException("Penalty rule not found: " + id));
        ViolationType t = r.violationTypeId() == null ? null : find(r.violationTypeId());
        ensureNoDuplicate(t, r.fromOccurrence(), id);
        applyRule(rule, t, r);
        return toResponse(rule);
    }

    public List<PenaltyRuleResponse> listRules() {
        return rules.findAll().stream().map(ViolationService::toResponse).toList();
    }

    private void ensureNoDuplicate(ViolationType t, int from, Long selfId) {
        List<PenaltyRule> same = t == null ? rules.findByViolationTypeIsNullAndActiveTrue()
                : rules.findByViolationTypeIdAndActiveTrue(t.getId());
        boolean dup = same.stream().anyMatch(x -> x.getFromOccurrence() == from && !x.getId().equals(selfId));
        if (dup) throw new ConflictException("An active rule for this violation type and occurrence already exists");
    }

    private void applyRule(PenaltyRule rule, ViolationType t, PenaltyRuleRequest r) {
        if (r.multiplier().signum() <= 0) throw new BadRequestException("Multiplier must be positive");
        rule.setViolationType(t);
        rule.setFromOccurrence(r.fromOccurrence());
        rule.setMultiplier(r.multiplier());
        rule.setActive(r.active() == null || r.active());
        rule.setDescription(r.description());
    }

    private void apply(ViolationType t, ViolationRequest r, String code) {
        t.setCode(code);
        t.setDescription(r.description().trim());
        t.setBaseFine(r.baseFine());
        t.setSeverity(r.severity());
        t.setActive(r.active() == null || r.active());
    }

    static ViolationResponse toResponse(ViolationType t) {
        return new ViolationResponse(t.getId(), t.getCode(), t.getDescription(), t.getBaseFine(), t.getSeverity(), t.isActive());
    }

    static PenaltyRuleResponse toResponse(PenaltyRule r) {
        ViolationType t = r.getViolationType();
        return new PenaltyRuleResponse(r.getId(), t == null ? null : t.getId(), t == null ? "ALL" : t.getCode(),
                r.getFromOccurrence(), r.getMultiplier(), r.isActive(), r.getDescription());
    }
}
