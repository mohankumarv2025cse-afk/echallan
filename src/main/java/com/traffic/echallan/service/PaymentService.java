package com.traffic.echallan.service;

import com.traffic.echallan.dto.Dtos.*;
import com.traffic.echallan.entity.*;
import com.traffic.echallan.exception.BadRequestException;
import com.traffic.echallan.exception.ConflictException;
import com.traffic.echallan.exception.NotFoundException;
import com.traffic.echallan.repository.ChallanRepository;
import com.traffic.echallan.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Simulated payments (no real gateway). The amount always comes from the stored challan fine. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {
    private final PaymentRepository payments;
    private final ChallanRepository challans;
    private final NotificationService notifications;

    @Transactional
    public PaymentResponse pay(PaymentRequest req) {
        Challan c = challans.findWithLockById(req.challanId())          // row lock => no double processing
                .orElseThrow(() -> new NotFoundException("Challan not found: " + req.challanId()));
        if (c.getStatus() == ChallanStatus.PAID)
            throw new ConflictException("Challan " + c.getChallanNumber() + " is already paid");
        if (payments.existsByChallanIdAndStatus(c.getId(), PaymentStatus.PENDING))
            throw new ConflictException("A payment for challan " + c.getChallanNumber() + " is already pending");

        PaymentStatus outcome = req.simulateOutcome() == null ? PaymentStatus.SUCCESS : req.simulateOutcome();
        Payment p = new Payment();
        p.setChallan(c);
        p.setAmount(c.getFinalFine());
        p.setMethod(req.method());
        p.setTransactionReference("TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        p.setStatus(outcome);
        p.setPaymentDate(LocalDateTime.now());
        p.setRemarks(switch (outcome) {
            case SUCCESS -> "Payment successful (simulated)";
            case FAILED -> "Payment failed (simulated)";
            case PENDING -> "Payment pending confirmation (simulated)";
        });
        if (outcome == PaymentStatus.SUCCESS) markPaid(c, p);
        p = payments.saveAndFlush(p);
        if (outcome == PaymentStatus.SUCCESS) notifications.paymentConfirmed(c, p);
        return toResponse(p);
    }

    /** Resolve a PENDING payment to SUCCESS or FAILED. */
    @Transactional
    public PaymentResponse resolve(Long paymentId, PaymentStatus outcome) {
        if (outcome != PaymentStatus.SUCCESS && outcome != PaymentStatus.FAILED)
            throw new BadRequestException("Outcome must be SUCCESS or FAILED");
        Payment p = payments.findById(paymentId).orElseThrow(() -> new NotFoundException("Payment not found: " + paymentId));
        Challan c = challans.findWithLockById(p.getChallan().getId()).orElseThrow();
        if (p.getStatus() != PaymentStatus.PENDING)
            throw new ConflictException("Only a PENDING payment can be resolved (current status: " + p.getStatus() + ")");
        if (outcome == PaymentStatus.SUCCESS) {
            if (c.getStatus() == ChallanStatus.PAID) throw new ConflictException("Challan is already paid");
            markPaid(c, p);
        }
        p.setStatus(outcome);
        p.setPaymentDate(LocalDateTime.now());
        p.setRemarks(outcome == PaymentStatus.SUCCESS ? "Payment successful (simulated)" : "Payment failed (simulated)");
        p = payments.saveAndFlush(p);
        if (outcome == PaymentStatus.SUCCESS) notifications.paymentConfirmed(c, p);
        return toResponse(p);
    }

    private void markPaid(Challan c, Payment p) {
        p.setSuccessChallanId(c.getId());
        c.setStatus(ChallanStatus.PAID);
        c.setPaidAt(LocalDateTime.now());
    }

    public PaymentResponse get(Long id) {
        return toResponse(payments.findById(id).orElseThrow(() -> new NotFoundException("Payment not found: " + id)));
    }

    public List<PaymentResponse> byChallan(Long challanId) {
        if (!challans.existsById(challanId)) throw new NotFoundException("Challan not found: " + challanId);
        return payments.findByChallanIdOrderByPaymentDateDesc(challanId).stream().map(PaymentService::toResponse).toList();
    }

    public PageResponse<PaymentResponse> history(PaymentStatus status, Pageable pageable) {
        Page<Payment> page = status == null ? payments.findAll(pageable) : payments.findByStatus(status, pageable);
        return new PageResponse<>(page.map(PaymentService::toResponse).getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    static PaymentResponse toResponse(Payment p) {
        Challan c = p.getChallan();
        return new PaymentResponse(p.getId(), c.getId(), c.getChallanNumber(), c.getVehicle().getRegistrationNumber(),
                p.getAmount(), p.getMethod(), p.getTransactionReference(), p.getStatus(), p.getPaymentDate(), p.getRemarks());
    }
}
