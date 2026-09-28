package com.traffic.echallan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "challans",
        uniqueConstraints = @UniqueConstraint(name = "uk_challan_no_duplicate",
                columnNames = {"vehicle_id", "violation_type_id", "violation_date_time"}),
        indexes = {
                @Index(name = "idx_challan_vehicle_type_date", columnList = "vehicle_id, violation_type_id, violation_date_time"),
                @Index(name = "idx_challan_status", columnList = "status"),
                @Index(name = "idx_challan_date", columnList = "violation_date_time")})
@Getter @Setter @NoArgsConstructor
public class Challan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 30)
    private String challanNumber;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "violation_type_id")
    private ViolationType violationType;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "officer_id")
    private User officer;
    @Column(name = "violation_date_time", nullable = false)
    private LocalDateTime violationDateTime;
    @Column(nullable = false, length = 200)
    private String location;
    @Column(length = 500)
    private String remarks;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10)
    private ChallanStatus status = ChallanStatus.UNPAID;

    // Fine snapshot: stored at issue time, never recalculated.
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal baseFine;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal multiplier;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal finalFine;
    @Column(nullable = false) private int occurrenceNumber;
    @Column(nullable = false) private int previousViolationCount;

    @Column(nullable = false) private LocalDate dueDate;
    @Column(nullable = false, updatable = false) private LocalDateTime issuedAt = LocalDateTime.now();
    private LocalDateTime paidAt;

    @OneToMany(mappedBy = "challan")
    private List<Payment> payments = new ArrayList<>();
}
