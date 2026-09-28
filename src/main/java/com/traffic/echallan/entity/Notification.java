package com.traffic.echallan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Simulated notification record (no real SMS/email is sent). */
@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_notification_challan", columnList = "challan_id"))
@Getter @Setter @NoArgsConstructor
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "challan_id")
    private Challan challan;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private NotificationType type;
    @Column(nullable = false, length = 15)
    private String recipient;
    @Column(nullable = false, length = 600)
    private String message;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fineAmount;
    @Column(nullable = false, length = 10)
    private String paymentStatus;
    private LocalDate dueDate;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
