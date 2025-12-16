package com.healthdata.analytics.repository;

import com.healthdata.analytics.persistence.DashboardWidgetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DashboardWidgetRepository extends JpaRepository<DashboardWidgetEntity, UUID> {

    List<DashboardWidgetEntity> findByDashboardIdAndTenantId(UUID dashboardId, String tenantId);

    Optional<DashboardWidgetEntity> findByIdAndTenantId(UUID id, String tenantId);

    List<DashboardWidgetEntity> findByDashboardIdAndTenantIdOrderByPositionYAscPositionXAsc(UUID dashboardId, String tenantId);

    List<DashboardWidgetEntity> findByTenantIdAndWidgetType(String tenantId, String widgetType);

    List<DashboardWidgetEntity> findByTenantIdAndDataSource(String tenantId, String dataSource);

    @Modifying
    @Query("DELETE FROM DashboardWidgetEntity w WHERE w.dashboardId = :dashboardId AND w.tenantId = :tenantId")
    void deleteByDashboardIdAndTenantId(@Param("dashboardId") UUID dashboardId, @Param("tenantId") String tenantId);

    @Query("SELECT COUNT(w) FROM DashboardWidgetEntity w WHERE w.dashboardId = :dashboardId")
    long countByDashboardId(@Param("dashboardId") UUID dashboardId);

    boolean existsByIdAndTenantId(UUID id, String tenantId);
}
