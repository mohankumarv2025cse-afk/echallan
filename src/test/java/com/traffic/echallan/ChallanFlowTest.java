package com.traffic.echallan;

import com.traffic.echallan.dto.Dtos.*;
import com.traffic.echallan.entity.*;
import com.traffic.echallan.exception.ConflictException;
import com.traffic.echallan.repository.*;
import com.traffic.echallan.service.ChallanService;
import com.traffic.echallan.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Integration tests on an in-memory H2 database (profile "h2"). Each test rolls back. */
@SpringBootTest(properties = "echallan.seed-demo-data=false")
@ActiveProfiles("h2")
@Transactional
class ChallanFlowTest {
    @Autowired ChallanService challanService;
    @Autowired PaymentService paymentService;
    @Autowired UserRepository users;
    @Autowired VehicleRepository vehicles;
    @Autowired ViolationTypeRepository types;
    @Autowired PenaltyRuleRepository rules;
    @Autowired ChallanRepository challans;

    Vehicle vehicle;
    ViolationType helmet, signal;

    @BeforeEach
    void setUp() {
        User o = new User();
        o.setUsername("tester"); o.setPasswordHash("x"); o.setFullName("Test Officer"); o.setRole(Role.OFFICER);
        users.save(o);
        vehicle = new Vehicle();
        vehicle.setRegistrationNumber("TN99ZZ0001"); vehicle.setVehicleType(VehicleType.CAR);
        vehicle.setOwnerName("Owner"); vehicle.setOwnerContact("9876543210");
        vehicles.save(vehicle);
        helmet = type("T_NHL", "No helmet", "500");
        signal = type("T_SGJ", "Signal jump", "500");
        rule(1, "1.00"); rule(2, "1.50"); rule(3, "2.00"); rule(4, "3.00");
    }

    private ViolationType type(String code, String d, String fine) {
        ViolationType t = new ViolationType();
        t.setCode(code); t.setDescription(d); t.setBaseFine(new BigDecimal(fine)); t.setSeverity(Severity.MEDIUM);
        return types.save(t);
    }
    private PenaltyRule rule(int from, String m) {
        PenaltyRule r = new PenaltyRule();
        r.setFromOccurrence(from); r.setMultiplier(new BigDecimal(m));
        return rules.save(r);
    }
    private ChallanResponse issue(ViolationType t, LocalDateTime at) {
        return challanService.issue(new ChallanRequest(vehicle.getId(), t.getId(), at, "Test road", null), "tester");
    }

    @Test
    void finesEscalateForRepeatedViolations() {
        LocalDateTime now = LocalDateTime.now();
        assertEquals(0, new BigDecimal("500").compareTo(issue(helmet, now.minusDays(40)).finalFine()));
        assertEquals(0, new BigDecimal("750").compareTo(issue(helmet, now.minusDays(30)).finalFine()));
        assertEquals(0, new BigDecimal("1000").compareTo(issue(helmet, now.minusDays(20)).finalFine()));
        ChallanResponse fourth = issue(helmet, now.minusDays(10));
        assertEquals(0, new BigDecimal("1500").compareTo(fourth.finalFine()));
        assertEquals(4, fourth.occurrenceNumber());
        assertTrue(fourth.repeatOffender());
        assertEquals(3, fourth.previousViolations().size());
    }

    @Test
    void violationsOlderThanTwelveMonthsAreNotCounted() {
        LocalDateTime now = LocalDateTime.now();
        issue(helmet, now.minusMonths(14));
        ChallanResponse r = issue(helmet, now.minusDays(1));
        assertEquals(1, r.occurrenceNumber());
        assertEquals(0, new BigDecimal("500").compareTo(r.finalFine()));
    }

    @Test
    void differentViolationTypesAreCountedSeparately() {
        LocalDateTime now = LocalDateTime.now();
        issue(helmet, now.minusDays(5));
        ChallanResponse r = issue(signal, now.minusDays(2));
        assertEquals(1, r.occurrenceNumber());
    }

    @Test
    void duplicateChallanIsRejected() {
        LocalDateTime at = LocalDateTime.now().minusDays(1);
        issue(helmet, at);
        assertThrows(ConflictException.class, () -> issue(helmet, at));
    }

    @Test
    void historicalFineDoesNotChangeWhenRulesChange() {
        LocalDateTime now = LocalDateTime.now();
        issue(helmet, now.minusDays(9));
        ChallanResponse second = issue(helmet, now.minusDays(8));
        rules.findAll().forEach(r -> { r.setMultiplier(new BigDecimal("9.00")); rules.save(r); });
        helmet.setBaseFine(new BigDecimal("5000")); types.save(helmet);
        ChallanResponse reloaded = challanService.get(second.id());
        assertEquals(0, new BigDecimal("750").compareTo(reloaded.finalFine()));
    }

    @Test
    void failedThenSuccessfulPaymentThenDuplicateRejected() {
        ChallanResponse c = issue(helmet, LocalDateTime.now().minusDays(1));
        PaymentResponse failed = paymentService.pay(new PaymentRequest(c.id(), PaymentMethod.UPI, PaymentStatus.FAILED));
        assertEquals(PaymentStatus.FAILED, failed.status());
        assertEquals(ChallanStatus.UNPAID, challanService.get(c.id()).status());

        PaymentResponse ok = paymentService.pay(new PaymentRequest(c.id(), PaymentMethod.UPI, null));
        assertEquals(PaymentStatus.SUCCESS, ok.status());
        assertEquals(ChallanStatus.PAID, challanService.get(c.id()).status());

        assertThrows(ConflictException.class, () -> paymentService.pay(new PaymentRequest(c.id(), PaymentMethod.CARD, null)));
    }

    @Test
    void pendingPaymentCanBeConfirmedOnlyOnce() {
        ChallanResponse c = issue(helmet, LocalDateTime.now().minusDays(1));
        PaymentResponse pending = paymentService.pay(new PaymentRequest(c.id(), PaymentMethod.NET_BANKING, PaymentStatus.PENDING));
        assertEquals(ChallanStatus.UNPAID, challanService.get(c.id()).status());
        assertThrows(ConflictException.class, () -> paymentService.pay(new PaymentRequest(c.id(), PaymentMethod.UPI, null)));
        assertEquals(PaymentStatus.SUCCESS, paymentService.resolve(pending.id(), PaymentStatus.SUCCESS).status());
        assertEquals(ChallanStatus.PAID, challanService.get(c.id()).status());
        assertThrows(ConflictException.class, () -> paymentService.resolve(pending.id(), PaymentStatus.SUCCESS));
    }
}
