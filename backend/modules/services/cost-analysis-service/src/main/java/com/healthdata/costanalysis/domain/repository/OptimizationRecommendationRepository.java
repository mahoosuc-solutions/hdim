package com.healthdata.costanalysis.domain.repository;

import com.healthdata.costanalysis.domain.model.OptimizationRecommendation;
import com.healthdata.costanalysis.domain.model.OptimizationRecommendation.RecommendationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface OptimizationRecommendationRepository extends JpaRepository<OptimizationRecommendation, UUID> {

    List<OptimizationRecommendation> findByTenantIdAndStatus(String tenantId, RecommendationStatus status);

    default List<OptimizationRecommendation> findByTenantAndStatus(String tenantId, RecommendationStatus status) {
        return findByTenantIdAndStatus(tenantId, status);
    }

    List<OptimizationRecommendation> findByTenantIdAndServiceName(String tenantId, String serviceName);

    @Query("""
        SELECT COALESCE(SUM(r.estimatedSavings), 0)
        FROM OptimizationRecommendation r
        WHERE r.tenantId = :tenantId
          AND r.status IN ('PENDING', 'ACCEPTED', 'IN_PROGRESS')
        """)
    BigDecimal calculateTotalPendingSavings(@Param("tenantId") String tenantId);

    @Query("""
        SELECT COUNT(r)
        FROM OptimizationRecommendation r
        WHERE r.tenantId = :tenantId
          AND r.status = 'COMPLETED'
        """)
    long countImplementedRecommendations(@Param("tenantId") String tenantId);
}
