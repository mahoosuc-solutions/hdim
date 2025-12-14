package com.healthdata.hcc.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface RecaptureOpportunityRepository extends JpaRepository<RecaptureOpportunityEntity, UUID> {

    List<RecaptureOpportunityEntity> findByTenantIdAndPatientIdAndCurrentYear(
        String tenantId, UUID patientId, Integer currentYear);

    @Query("SELECT r FROM RecaptureOpportunityEntity r WHERE r.tenantId = :tenantId " +
           "AND r.currentYear = :year AND r.isRecaptured = false " +
           "AND r.rafValueV28 >= :minValue " +
           "ORDER BY r.rafValueV28 DESC")
    List<RecaptureOpportunityEntity> findHighValueOpportunities(
        @Param("tenantId") String tenantId,
        @Param("year") Integer year,
        @Param("minValue") BigDecimal minValue);

    @Query("SELECT r FROM RecaptureOpportunityEntity r WHERE r.tenantId = :tenantId " +
           "AND r.currentYear = :year AND r.isRecaptured = false " +
           "ORDER BY r.priority, r.rafValueV28 DESC")
    List<RecaptureOpportunityEntity> findPendingByTenant(
        @Param("tenantId") String tenantId,
        @Param("year") Integer year);

    @Query("SELECT COUNT(r), " +
           "SUM(CASE WHEN r.isRecaptured = true THEN 1 ELSE 0 END), " +
           "COALESCE(SUM(r.rafValueV28), 0), " +
           "COALESCE(SUM(CASE WHEN r.isRecaptured = true THEN r.rafValueV28 ELSE 0 END), 0) " +
           "FROM RecaptureOpportunityEntity r " +
           "WHERE r.tenantId = :tenantId AND r.currentYear = :year")
    List<Object[]> getRecaptureStatsByTenant(
        @Param("tenantId") String tenantId,
        @Param("year") Integer year);

    @Query("SELECT r.hccCode, COUNT(r), SUM(r.rafValueV28) " +
           "FROM RecaptureOpportunityEntity r " +
           "WHERE r.tenantId = :tenantId AND r.currentYear = :year AND r.isRecaptured = false " +
           "GROUP BY r.hccCode ORDER BY SUM(r.rafValueV28) DESC")
    List<Object[]> getOpportunitiesByHcc(
        @Param("tenantId") String tenantId,
        @Param("year") Integer year);

    long countByTenantIdAndCurrentYearAndIsRecaptured(
        String tenantId, Integer currentYear, Boolean isRecaptured);
}
