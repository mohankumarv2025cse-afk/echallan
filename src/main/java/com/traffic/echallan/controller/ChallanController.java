package com.traffic.echallan.controller;

import com.traffic.echallan.dto.Dtos.*;
import com.traffic.echallan.entity.ChallanStatus;
import com.traffic.echallan.service.ChallanService;
import com.traffic.echallan.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/challans")
@RequiredArgsConstructor
public class ChallanController {
    private final ChallanService service;
    private final ReportService reports;

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ChallanResponse issue(@Valid @RequestBody ChallanRequest r, Authentication auth) {
        return service.issue(r, auth.getName());
    }

    /** Search + filter + pagination + sorting: ?registration=&number=&violationTypeId=&status=&from=&to=&page=&size=&sort= */
    @GetMapping
    public PageResponse<ChallanResponse> list(
            @RequestParam(required = false) String registration,
            @RequestParam(required = false) String number,
            @RequestParam(required = false) Long violationTypeId,
            @RequestParam(required = false) ChallanStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 10, sort = "issuedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.search(registration, number, violationTypeId, status, from, to, pageable);
    }

    @GetMapping("/pending")
    public PageResponse<ChallanResponse> pending(@PageableDefault(size = 10, sort = "dueDate") Pageable pageable) {
        return service.search(null, null, null, ChallanStatus.UNPAID, null, null, pageable);
    }

    @GetMapping("/repeat-offenders") public List<RepeatOffenderRow> repeatOffenders() { return reports.repeatOffenders(); }

    @GetMapping("/reports") public ReportResponse report() { return reports.summary(); }

    @GetMapping("/vehicle/{vehicleId}") public List<ChallanResponse> byVehicle(@PathVariable Long vehicleId) { return service.historyByVehicle(vehicleId); }

    @GetMapping("/registration/{registrationNumber}")
    public List<ChallanResponse> byRegistration(@PathVariable String registrationNumber) { return service.historyByRegistration(registrationNumber); }

    @GetMapping("/{id}") public ChallanResponse get(@PathVariable Long id) { return service.get(id); }
}
