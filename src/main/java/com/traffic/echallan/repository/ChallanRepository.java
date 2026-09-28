package com.traffic.echallan.repository;

import com.traffic.echallan.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChallanRepository extends JpaRepository<Challan, Long>, JpaSpecificationExecutor<Challan> {

    boolean existsByChallanNumber(String challanNumber);

    boolean existsByVehicleIdAndViolationTypeIdAndViolationDateTime(Long vehicleId, Long typeId, LocalDateTime at);

    /** Rolling-window count: same vehicle + same violation type + between [from, to]. */
    long countByVehicleIdAndViolationTypeIdAndViolationDateTimeBetween(Long vehicleId, Long typeId, LocalDateTime from, LocalDateTime to);

    List<Challan> findByVehicleIdAndViolationTypeIdAndViolationDateTimeBetweenOrderByViolationDateTimeAsc(
            Long vehicleId, Long typeId, LocalDateTime from, LocalDateTime to);

    List<Challan> findByVehicleIdOrderByViolationDateTimeDesc(Long vehicleId);

    boolean existsByVehicleId(Long vehicleId);

    long countByStatus(ChallanStatus status);

    List<Challan> findByViolationDateTimeGreaterThanEqual(LocalDateTime from);

    @Query("select sum(c.finalFine) from Challan c where c.status = :status")
    BigDecimal sumFineByStatus(@Param("status") ChallanStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Challan c where c.id = :id")
    Optional<Challan> findWithLockById(@Param("id") Long id);

    @Query("select c.vehicle.id, count(c), sum(c.finalFine), max(c.violationDateTime) from Challan c " +
           "where c.violationDateTime >= :from group by c.vehicle.id having count(c) > 1 order by count(c) desc")
    List<Object[]> repeatOffenderRows(@Param("from") LocalDateTime from);
}
