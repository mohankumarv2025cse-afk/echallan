package com.traffic.echallan.controller;

import com.traffic.echallan.dto.Dtos.*;
import com.traffic.echallan.service.ViolationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/violations")
@RequiredArgsConstructor
public class ViolationController {
    private final ViolationService service;

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ViolationResponse create(@Valid @RequestBody ViolationRequest r) { return service.create(r); }

    @GetMapping public List<ViolationResponse> list() { return service.list(); }

    @GetMapping("/{id}") public ViolationResponse get(@PathVariable Long id) { return service.get(id); }

    @PutMapping("/{id}")
    public ViolationResponse update(@PathVariable Long id, @Valid @RequestBody ViolationRequest r) { return service.update(id, r); }
}
