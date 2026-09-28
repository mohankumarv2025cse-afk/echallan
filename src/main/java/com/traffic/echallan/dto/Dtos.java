package com.traffic.echallan.dto;

import com.traffic.echallan.entity.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class Dtos {
    private Dtos() {}

    // ---- auth / users ----
    public record LoginRequest(@NotBlank(message = "Username is required") String username,
                               @NotBlank(message = "Password is required") String password) {}
    public record LoginResponse(Long id, String username, String fullName, Role role) {}
    public record UserRequest(
            @NotBlank @Size(min = 3, max = 50, message = "Username must be 3-50 characters") String username,
            @NotBlank @Size(min = 8, max = 100, message = "Password must be at least 8 characters") String password,
            @NotBlank @Size(max = 100) String fullName,
            @Size(max = 30) String badgeNumber,
            @NotNull(message = "Role is required") Role role) {}
    public record UserResponse(Long id, String username, String fullName, String badgeNumber, Role role, boolean active) {}

    // ---- vehicles ----
    public record VehicleRequest(
            @NotBlank(message = "Registration number is required")
            @Pattern(regexp = "^[A-Za-z]{2}[- ]?[0-9]{1,2}[- ]?[A-Za-z]{1,3}[- ]?[0-9]{4}$",
                     message = "Invalid registration number (example: TN34AB1234)") String registrationNumber,
            @NotNull(message = "Vehicle type is required") VehicleType vehicleType,
            @NotBlank(message = "Owner name is required") @Size(max = 100) String ownerName,
            @NotBlank(message = "Owner contact is required")
            @Pattern(regexp = "^[6-9][0-9]{9}$", message = "Owner contact must be a valid 10-digit mobile number") String ownerContact,
            @Email(message = "Invalid email") String ownerEmail,
            @Size(max = 50) String make, @Size(max = 50) String model, @Size(max = 30) String color) {}
    public record VehicleResponse(Long id, String registrationNumber, VehicleType vehicleType, String ownerName,
                                  String ownerContact, String ownerEmail, String make, String model, String color,
                                  LocalDateTime createdAt) {}

    // ---- violation types & rules ----
    public record ViolationRequest(
            @NotBlank(message = "Code is required") @Size(max = 20) String code,
            @NotBlank(message = "Description is required") @Size(max = 200) String description,
            @NotNull(message = "Base fine is required") @DecimalMin(value = "1.00", message = "Base fine must be at least 1.00")
            @Digits(integer = 8, fraction = 2) BigDecimal baseFine,
            @NotNull(message = "Severity is required") Severity severity,
            Boolean active) {}
    public record ViolationResponse(Long id, String code, String description, BigDecimal baseFine, Severity severity, boolean active) {}
    public record PenaltyRuleRequest(
            Long violationTypeId,
            @Min(value = 1, message = "From-occurrence must be at least 1") int fromOccurrence,
            @NotNull(message = "Multiplier is required") @DecimalMin(value = "1.00", message = "Multiplier must be at least 1.00")
            @DecimalMax(value = "20.00") @Digits(integer = 3, fraction = 2) BigDecimal multiplier,
            Boolean active, @Size(max = 255) String description) {}
    public record PenaltyRuleResponse(Long id, Long violationTypeId, String violationTypeCode, int fromOccurrence,
                                      BigDecimal multiplier, boolean active, String description) {}

    // ---- challans ----
    public record ChallanRequest(
            @NotNull(message = "Vehicle is required") Long vehicleId,
            @NotNull(message = "Violation type is required") Long violationTypeId,
            @NotNull(message = "Violation date/time is required") @PastOrPresent(message = "Violation date/time cannot be in the future") LocalDateTime violationDateTime,
            @NotBlank(message = "Location is required") @Size(max = 200) String location,
            @Size(max = 500) String remarks) {}
    public record PreviousViolation(String challanNumber, LocalDateTime violationDateTime, BigDecimal finalFine) {}
    public record ChallanResponse(Long id, String challanNumber, Long vehicleId, String registrationNumber, String ownerName,
                                  Long violationTypeId, String violationCode, String violationDescription,
                                  Long officerId, String officerName, LocalDateTime violationDateTime, String location, String remarks,
                                  ChallanStatus status, BigDecimal baseFine, BigDecimal multiplier, BigDecimal finalFine,
                                  int occurrenceNumber, int previousViolationCount, boolean repeatOffender,
                                  LocalDate dueDate, LocalDateTime issuedAt, LocalDateTime paidAt,
                                  List<PreviousViolation> previousViolations) {}

    // ---- payments / notifications ----
    public record PaymentRequest(@NotNull(message = "Challan is required") Long challanId,
                                 @NotNull(message = "Payment method is required") PaymentMethod method,
                                 PaymentStatus simulateOutcome) {}
    public record PaymentResponse(Long id, Long challanId, String challanNumber, String registrationNumber, BigDecimal amount,
                                  PaymentMethod method, String transactionReference, PaymentStatus status,
                                  LocalDateTime paymentDate, String remarks) {}
    public record NotificationResponse(Long id, Long challanId, String challanNumber, NotificationType type, String recipient,
                                       String message, BigDecimal fineAmount, String paymentStatus, LocalDate dueDate,
                                       LocalDateTime createdAt) {}

    // ---- reports ----
    public record RepeatOffenderRow(Long vehicleId, String registrationNumber, String ownerName, String ownerContact,
                                    long violationsInWindow, BigDecimal totalFines, LocalDateTime lastViolation, String status) {}
    public record MonthlyStat(String month, long challans, BigDecimal fines) {}
    public record ReportResponse(long totalChallans, long paidChallans, long unpaidChallans, BigDecimal paidAmount,
                                 BigDecimal pendingAmount, long repeatOffenderVehicles, List<MonthlyStat> monthly) {}

    // ---- generic ----
    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
    public record ApiError(LocalDateTime timestamp, int status, String message, Map<String, String> errors) {}
}
