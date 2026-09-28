package com.traffic.echallan.controller;

import com.traffic.echallan.dto.Dtos.*;
import com.traffic.echallan.service.ViolationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/penalty-rules")
@RequiredArgsConstructor
public class PenaltyRuleController {
    private final ViolationService service;

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public PenaltyRuleResponse create(@Valid @RequestBody PenaltyRuleRequest r) { return service.createRule(r); }

    @GetMapping public List<PenaltyRuleResponse> list() { return service.listRules(); }

    @PutMapping("/{id}")
    public PenaltyRuleResponse update(@PathVariable Long id, @Valid @RequestBody PenaltyRuleRequest r) { return service.updateRule(id, r); }
}
