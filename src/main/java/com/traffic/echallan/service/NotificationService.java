package com.traffic.echallan.service;

import com.traffic.echallan.dto.Dtos.NotificationResponse;
import com.traffic.echallan.dto.Dtos.PageResponse;
import com.traffic.echallan.entity.*;
import com.traffic.echallan.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Simulated notifications: only a database record is created (no SMS/email). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
    private final NotificationRepository notifications;

    @Transactional
    public void challanIssued(Challan c) {
        save(c, NotificationType.CHALLAN_ISSUED, "UNPAID", String.format(
                "E-Challan %s issued to %s for %s. Fine: Rs.%s. Payment status: UNPAID. Due date: %s.",
                c.getChallanNumber(), c.getVehicle().getRegistrationNumber(), c.getViolationType().getDescription(),
                c.getFinalFine(), c.getDueDate()));
    }

    @Transactional
    public void paymentConfirmed(Challan c, Payment p) {
        save(c, NotificationType.PAYMENT_CONFIRMED, "PAID", String.format(
                "Payment of Rs.%s received for challan %s (ref %s). Payment status: PAID.",
                p.getAmount(), c.getChallanNumber(), p.getTransactionReference()));
    }

    private void save(Challan c, NotificationType type, String status, String message) {
        Notification n = new Notification();
        n.setChallan(c);
        n.setType(type);
        n.setRecipient(c.getVehicle().getOwnerContact());
        n.setMessage(message);
        n.setFineAmount(c.getFinalFine());
        n.setPaymentStatus(status);
        n.setDueDate(c.getDueDate());
        notifications.save(n);
    }

    public PageResponse<NotificationResponse> list(Pageable pageable) {
        Page<Notification> page = notifications.findAll(pageable);
        return new PageResponse<>(page.map(NotificationService::toResponse).getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    public List<NotificationResponse> forChallan(Long challanId) {
        return notifications.findByChallanIdOrderByCreatedAtDesc(challanId).stream().map(NotificationService::toResponse).toList();
    }

    static NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getChallan().getId(), n.getChallan().getChallanNumber(), n.getType(),
                n.getRecipient(), n.getMessage(), n.getFineAmount(), n.getPaymentStatus(), n.getDueDate(), n.getCreatedAt());
    }
}
