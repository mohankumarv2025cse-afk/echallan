package com.traffic.echallan.config;

import com.traffic.echallan.dto.Dtos.*;
import com.traffic.echallan.entity.*;
import com.traffic.echallan.repository.*;
import com.traffic.echallan.service.ChallanService;
import com.traffic.echallan.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Loads demo data on first start (only when the users table is empty). */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "echallan.seed-demo-data", havingValue = "true")
public class DataInitializer implements ApplicationRunner {
    private final UserRepository users;
    private final VehicleRepository vehicles;
    private final ViolationTypeRepository types;
    private final PenaltyRuleRepository rules;
    private final ChallanService challanService;
    private final PaymentService paymentService;
    private final PasswordEncoder encoder;

    @Value("${echallan.admin-password}") private String adminPassword;
    @Value("${echallan.officer-password}") private String officerPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() > 0) return;

        user("admin", adminPassword, "System Administrator", null, Role.ADMIN);
        user("officer1", officerPassword, "Officer Ravi Kumar", "TN-1001", Role.OFFICER);
        user("officer2", officerPassword, "Officer Meena Devi", "TN-1002", Role.OFFICER);

        ViolationType ovs = type("OVS", "Over-speeding", "1000", Severity.HIGH);
        ViolationType sgj = type("SGJ", "Signal jumping", "500", Severity.MEDIUM);
        ViolationType nhl = type("NHL", "No helmet", "500", Severity.MEDIUM);
        ViolationType nsb = type("NSB", "No seat belt", "500", Severity.LOW);
        ViolationType ilp = type("ILP", "Illegal parking", "300", Severity.LOW);

        // Global escalation: 1x, 1.5x, 2x, 3x
        rule(null, 1, "1.00"); rule(null, 2, "1.50"); rule(null, 3, "2.00"); rule(null, 4, "3.00");
        // Over-speeding has stricter rules of its own (type-specific rules override global ones)
        rule(ovs, 1, "1.00"); rule(ovs, 2, "2.00"); rule(ovs, 3, "3.00");

        Vehicle a = vehicle("TN34AB1234", VehicleType.TWO_WHEELER, "Arun Kumar", "9876543210", "Honda", "Activa");
        Vehicle b = vehicle("TN28CD5678", VehicleType.CAR, "Priya Sharma", "9123456780", "Hyundai", "i20");
        Vehicle c = vehicle("TN30EF9012", VehicleType.TRUCK, "Selvam Transport", "9988776655", "Tata", "407");
        Vehicle d = vehicle("KA05MN4321", VehicleType.CAR, "Rahul Verma", "9012345678", "Maruti", "Swift");

        LocalDateTime now = LocalDateTime.now().minusHours(1);
        // TN34AB1234 no-helmet history: shows rolling-window behaviour (the 14-month-old one drops out later)
        ChallanResponse first = challan(a, nhl, now.minusMonths(14), "Salem Road, Tiruchengode");
        challan(a, nhl, now.minusMonths(8), "Bus Stand Junction");
        challan(a, nhl, now.minusMonths(3), "Namakkal Bypass");
        ChallanResponse latest = challan(a, nhl, now.minusDays(1), "Sankagiri Road");
        challan(a, sgj, now.minusDays(20), "Main Signal, Erode");
        // Over-speeding uses its own stricter rules
        challan(b, ovs, now.minusMonths(5), "NH44 Toll Plaza");
        ChallanResponse ovs2 = challan(b, ovs, now.minusDays(9), "NH44 Km 132");
        challan(c, ilp, now.minusDays(2), "Market Street");
        challan(d, nsb, now.minusDays(4), "Ring Road");

        // Some payments: one success, one failed attempt then success
        paymentService.pay(new PaymentRequest(first.id(), PaymentMethod.UPI, PaymentStatus.SUCCESS));
        paymentService.pay(new PaymentRequest(ovs2.id(), PaymentMethod.CARD, PaymentStatus.FAILED));
        paymentService.pay(new PaymentRequest(ovs2.id(), PaymentMethod.CARD, PaymentStatus.SUCCESS));
        System.out.println(">>> Demo data loaded. Latest helmet challan " + latest.challanNumber()
                + " fine=" + latest.finalFine());
    }

    private void user(String name, String pw, String full, String badge, Role role) {
        User u = new User();
        u.setUsername(name); u.setPasswordHash(encoder.encode(pw)); u.setFullName(full);
        u.setBadgeNumber(badge); u.setRole(role);
        users.save(u);
    }

    private ViolationType type(String code, String desc, String fine, Severity s) {
        ViolationType t = new ViolationType();
        t.setCode(code); t.setDescription(desc); t.setBaseFine(new BigDecimal(fine)); t.setSeverity(s);
        return types.save(t);
    }

    private void rule(ViolationType t, int from, String mult) {
        PenaltyRule r = new PenaltyRule();
        r.setViolationType(t); r.setFromOccurrence(from); r.setMultiplier(new BigDecimal(mult));
        r.setDescription(t == null ? "Global rule" : t.getCode() + " rule");
        rules.save(r);
    }

    private Vehicle vehicle(String reg, VehicleType vt, String owner, String phone, String make, String model) {
        Vehicle v = new Vehicle();
        v.setRegistrationNumber(reg); v.setVehicleType(vt); v.setOwnerName(owner); v.setOwnerContact(phone);
        v.setMake(make); v.setModel(model);
        return vehicles.save(v);
    }

    private ChallanResponse challan(Vehicle v, ViolationType t, LocalDateTime at, String location) {
        return challanService.issue(new ChallanRequest(v.getId(), t.getId(), at, location, "Demo data"), "officer1");
    }
}
