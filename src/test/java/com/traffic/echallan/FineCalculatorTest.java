package com.traffic.echallan;

import com.traffic.echallan.service.FineCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FineCalculatorTest {
    private final Map<Integer, BigDecimal> rules = Map.of(
            1, new BigDecimal("1.00"), 2, new BigDecimal("1.50"), 3, new BigDecimal("2.00"), 4, new BigDecimal("3.00"));
    private final BigDecimal base = new BigDecimal("500.00");

    private BigDecimal fine(int n) { return FineCalculator.finalFine(base, FineCalculator.multiplierFor(n, rules)); }

    @Test void escalation() {
        assertEquals(new BigDecimal("500.00"), fine(1));
        assertEquals(new BigDecimal("750.00"), fine(2));
        assertEquals(new BigDecimal("1000.00"), fine(3));
        assertEquals(new BigDecimal("1500.00"), fine(4));
        assertEquals(new BigDecimal("1500.00"), fine(9));   // 4th and subsequent
    }

    @Test void noRulesMeansBaseFine() {
        assertEquals(new BigDecimal("500.00"), FineCalculator.finalFine(base, FineCalculator.multiplierFor(5, Map.of())));
    }

    @Test void roundingIsHalfUp() {
        assertEquals(new BigDecimal("333.75"), FineCalculator.finalFine(new BigDecimal("222.50"), new BigDecimal("1.50")));
    }

    @Test void rollingWindowStart() {
        LocalDateTime at = LocalDateTime.of(2026, 9, 28, 10, 0);
        assertEquals(LocalDateTime.of(2025, 9, 28, 10, 0), FineCalculator.windowStart(at, 12));
    }
}
