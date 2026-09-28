package com.traffic.echallan.service;

import com.traffic.echallan.dto.Dtos.*;
import com.traffic.echallan.entity.Challan;
import com.traffic.echallan.entity.ChallanStatus;
import com.traffic.echallan.entity.Vehicle;
import com.traffic.echallan.repository.ChallanRepository;
import com.traffic.echallan.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {
    private final ChallanRepository challans;
    private final VehicleRepository vehicles;

    @Value("${echallan.rolling-window-months:12}")
    private int windowMonths;

    public ReportResponse summary() {
        BigDecimal paid = nz(challans.sumFineByStatus(ChallanStatus.PAID));
        BigDecimal pending = nz(challans.sumFineByStatus(ChallanStatus.UNPAID));

        YearMonth start = YearMonth.now().minusMonths(11);
        Map<YearMonth, long[]> counts = new TreeMap<>();
        Map<YearMonth, BigDecimal> fines = new TreeMap<>();
        for (int i = 0; i < 12; i++) {
            counts.put(start.plusMonths(i), new long[]{0});
            fines.put(start.plusMonths(i), BigDecimal.ZERO);
        }
        for (Challan c : challans.findByViolationDateTimeGreaterThanEqual(start.atDay(1).atStartOfDay())) {
            YearMonth ym = YearMonth.from(c.getViolationDateTime());
            if (counts.containsKey(ym)) {
                counts.get(ym)[0]++;
                fines.merge(ym, c.getFinalFine(), BigDecimal::add);
            }
        }
        List<MonthlyStat> monthly = new ArrayList<>();
        counts.forEach((ym, n) -> monthly.add(new MonthlyStat(ym.toString(), n[0], fines.get(ym))));

        return new ReportResponse(challans.count(), challans.countByStatus(ChallanStatus.PAID),
                challans.countByStatus(ChallanStatus.UNPAID), paid, pending, repeatOffenders().size(), monthly);
    }

    /** Vehicles with 2+ violations inside the rolling window. 3+ is labelled HABITUAL. */
    public List<RepeatOffenderRow> repeatOffenders() {
        LocalDateTime from = LocalDateTime.now().minusMonths(windowMonths);
        List<RepeatOffenderRow> rows = new ArrayList<>();
        for (Object[] r : challans.repeatOffenderRows(from)) {
            Vehicle v = vehicles.findById((Long) r[0]).orElse(null);
            if (v == null) continue;
            long count = (Long) r[1];
            rows.add(new RepeatOffenderRow(v.getId(), v.getRegistrationNumber(), v.getOwnerName(), v.getOwnerContact(),
                    count, (BigDecimal) r[2], (LocalDateTime) r[3], count >= 3 ? "HABITUAL" : "REPEAT"));
        }
        return rows;
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
