package com.traffic.echallan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** From occurrence N onward the multiplier applies. violationType == null means a global rule. */
@Entity
@Table(name = "penalty_rules")
@Getter @Setter @NoArgsConstructor
public class PenaltyRule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "violation_type_id")
    private ViolationType violationType;
    @Column(nullable = false)
    private int fromOccurrence;
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal multiplier;
    @Column(nullable = false)
    private boolean active = true;
    private String description;
}
