package com.healthdata.analytics.repository;

import com.healthdata.analytics.persistence.DashboardEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DashboardRepository extends JpaRepository<DashboardEntity, UUID> {

    List<DashboardEntity> findByTenantId(String tenantId);

    Page<DashboardEntity> findByTenantId(String tenantId, Pageable pageable);

    Optional<DashboardEntity> findByIdAndTenantId(UUID id, String tenantId);

    List<DashboardEntity> findByTenantIdAndCreatedBy(String tenantId, String createdBy);

    Optional<DashboardEntity> findByTenantIdAndIsDefaultTrue(String tenantId);

    @Query("SELECT d FROM DashboardEntity d WHERE d.tenantId = :tenantId AND (d.createdBy = :userId OR d.isShared = true)")
    List<DashboardEntity> findAccessibleDashboards(@Param("tenantId") String tenantId, @Param("userId") String userId);

    @Query("SELECT COUNT(d) FROM DashboardEntity d WHERE d.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") String tenantId);

    boolean existsByIdAndTenantId(UUID id, String tenantId);
}
