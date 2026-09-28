package com.traffic.echallan.controller;

import com.traffic.echallan.dto.Dtos.*;
import com.traffic.echallan.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {
    private final VehicleService service;

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse create(@Valid @RequestBody VehicleRequest r) { return service.create(r); }

    @GetMapping
    public PageResponse<VehicleResponse> list(@RequestParam(required = false) String q,
                                              @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return service.search(q, pageable);
    }

    @GetMapping("/{id}") public VehicleResponse get(@PathVariable Long id) { return service.get(id); }

    @GetMapping("/registration/{registrationNumber}")
    public VehicleResponse byReg(@PathVariable String registrationNumber) { return service.getByRegistration(registrationNumber); }

    @PutMapping("/{id}")
    public VehicleResponse update(@PathVariable Long id, @Valid @RequestBody VehicleRequest r) { return service.update(id, r); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) { service.delete(id); return ResponseEntity.noContent().build(); }
}
