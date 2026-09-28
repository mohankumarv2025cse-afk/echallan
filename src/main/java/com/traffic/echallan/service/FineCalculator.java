package com.traffic.echallan.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Pure fine/escalation logic (no Spring, no database) so it is easy to test and explain.
 * rules: fromOccurrence -> multiplier. The rule with the largest fromOccurrence <= occurrence wins.
 */
public final class FineCalculator {
    private FineCalculator() {}

    public static BigDecimal multiplierFor(int occurrence, Map<Integer, BigDecimal> rules) {
        BigDecimal multiplier = BigDecimal.ONE;
        int best = 0;
        for (Map.Entry<Integer, BigDecimal> e : rules.entrySet()) {
            if (e.getKey() <= occurrence && e.getKey() > best) {
                best = e.getKey();
                multiplier = e.getValue();
            }
        }
        return multiplier;
    }

    public static BigDecimal finalFine(BigDecimal baseFine, BigDecimal multiplier) {
        return baseFine.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    /** Start of the rolling window that ends at the violation time. */
    public static LocalDateTime windowStart(LocalDateTime violationTime, int months) {
        return violationTime.minusMonths(months);
    }
}
