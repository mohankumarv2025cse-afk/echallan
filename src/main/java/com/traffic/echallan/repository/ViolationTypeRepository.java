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

public interface ViolationTypeRepository extends JpaRepository<ViolationType, Long> {
    Optional<ViolationType> findByCode(String code);
    boolean existsByCode(String code);
}
