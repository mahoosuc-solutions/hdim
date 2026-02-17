package com.healthdata.costanalysis.domain.repository;

import com.healthdata.costanalysis.domain.model.CostTrackingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CostTrackingRepository extends JpaRepository<CostTrackingEntity, UUID> {

    @Query("""
        SELECT c.tenantId, c.serviceId, c.featureKey, SUM(c.costAmount), COUNT(c)
        FROM CostTrackingEntity c
        WHERE c.timestampUtc >= :startInclusive
          AND c.timestampUtc < :endExclusive
        GROUP BY c.tenantId, c.serviceId, c.featureKey
        """)
    List<Object[]> aggregateDailySummary(
        @Param("startInclusive") Instant startInclusive,
        @Param("endExclusive") Instant endExclusive
    );

    @Query("""
        SELECT COALESCE(SUM(c.costAmount), 0)
        FROM CostTrackingEntity c
        WHERE c.tenantId = :tenantId
          AND c.timestampUtc >= :startInclusive
          AND c.timestampUtc < :endExclusive
        """)
    BigDecimal sumCostForTenant(
        @Param("tenantId") String tenantId,
        @Param("startInclusive") Instant startInclusive,
        @Param("endExclusive") Instant endExclusive
    );
}
