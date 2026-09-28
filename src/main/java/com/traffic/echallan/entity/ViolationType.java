package com.traffic.echallan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "violation_types")
@Getter @Setter @NoArgsConstructor
public class ViolationType {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 20)
    private String code;
    @Column(nullable = false, length = 200)
    private String description;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal baseFine;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10)
    private Severity severity;
    @Column(nullable = false)
    private boolean active = true;
    @OneToMany(mappedBy = "violationType")
    private List<Challan> challans = new ArrayList<>();
}
