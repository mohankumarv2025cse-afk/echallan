package com.traffic.echallan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = @Index(name = "idx_payment_challan", columnList = "challan_id"))
@Getter @Setter @NoArgsConstructor
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "challan_id")
    private Challan challan;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private PaymentMethod method;
    @Column(nullable = false, unique = true, length = 40)
    private String transactionReference;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10)
    private PaymentStatus status;
    @Column(nullable = false)
    private LocalDateTime paymentDate = LocalDateTime.now();
    private String remarks;
    /** Set to the challan id only for a SUCCESS payment. UNIQUE => the database itself blocks a 2nd success. */
    @Column(name = "success_challan_id", unique = true)
    private Long successChallanId;
}
