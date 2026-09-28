package com.traffic.echallan.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vehicles", indexes = @Index(name = "idx_vehicle_owner", columnList = "owner_name"))
@Getter @Setter @NoArgsConstructor
public class Vehicle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 15)
    private String registrationNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private VehicleType vehicleType;
    @Column(name = "owner_name", nullable = false, length = 100)
    private String ownerName;
    @Column(nullable = false, length = 15)
    private String ownerContact;
    private String ownerEmail;
    @Column(length = 50) private String make;
    @Column(length = 50) private String model;
    @Column(length = 30) private String color;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    @OneToMany(mappedBy = "vehicle")
    private List<Challan> challans = new ArrayList<>();
}
