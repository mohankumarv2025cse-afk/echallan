package com.traffic.echallan.controller;

import com.traffic.echallan.dto.Dtos.*;
import com.traffic.echallan.entity.PaymentStatus;
import com.traffic.echallan.service.NotificationService;
import com.traffic.echallan.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService service;
    private final NotificationService notifications;

    @PostMapping("/api/payments") @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse pay(@Valid @RequestBody PaymentRequest r) { return service.pay(r); }

    @PostMapping("/api/payments/{id}/resolve")
    public PaymentResponse resolve(@PathVariable Long id, @RequestParam PaymentStatus outcome) { return service.resolve(id, outcome); }

    @GetMapping("/api/payments/history")
    public PageResponse<PaymentResponse> history(@RequestParam(required = false) PaymentStatus status,
            @PageableDefault(size = 10, sort = "paymentDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.history(status, pageable);
    }

    @GetMapping("/api/payments/{id}") public PaymentResponse get(@PathVariable Long id) { return service.get(id); }

    @GetMapping("/api/payments/challan/{challanId}")
    public List<PaymentResponse> byChallan(@PathVariable Long challanId) { return service.byChallan(challanId); }

    @GetMapping("/api/notifications")
    public PageResponse<NotificationResponse> notifications(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return notifications.list(pageable);
    }
}
