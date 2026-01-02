package com.healthdata.sdoh.repository;

import com.healthdata.sdoh.entity.HrsnScreeningSessionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for HRSN screening session persistence.
 */
@Repository
public interface HrsnScreeningSessionRepository extends JpaRepository<HrsnScreeningSessionEntity, UUID> {

    Optional<HrsnScreeningSessionEntity> findByIdAndTenantId(UUID id, String tenantId);

    List<HrsnScreeningSessionEntity> findByTenantIdAndPatientId(String tenantId, UUID patientId);

    Page<HrsnScreeningSessionEntity> findByTenantIdOrderByScreeningDateDesc(String tenantId, Pageable pageable);

    /**
     * Find the most recent screening session for a patient.
     */
    @Query("SELECT s FROM HrsnScreeningSessionEntity s " +
           "WHERE s.tenantId = :tenantId AND s.patientId = :patientId " +
           "ORDER BY s.screeningDate DESC LIMIT 1")
    Optional<HrsnScreeningSessionEntity> findMostRecentByPatient(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId
    );

    /**
     * Find screening sessions within measurement period.
     */
    @Query("SELECT s FROM HrsnScreeningSessionEntity s " +
           "WHERE s.tenantId = :tenantId AND s.patientId = :patientId " +
           "AND s.screeningDate BETWEEN :periodStart AND :periodEnd " +
           "AND s.status = 'COMPLETED'")
    List<HrsnScreeningSessionEntity> findCompletedInPeriod(
        @Param("tenantId") String tenantId,
        @Param("patientId") UUID patientId,
        @Param("periodStart") LocalDateTime periodStart,
        @Param("periodEnd") LocalDateTime periodEnd
    );

    /**
     * Find patients with complete SDOH-1 screening (all 5 domains).
     */
    @Query("SELECT s FROM HrsnScreeningSessionEntity s " +
           "WHERE s.tenantId = :tenantId " +
           "AND s.allDomainsScreened = true " +
           "AND s.screeningDate BETWEEN :periodStart AND :periodEnd")
    List<HrsnScreeningSessionEntity> findWithAllDomainsScreened(
        @Param("tenantId") String tenantId,
        @Param("periodStart") LocalDateTime periodStart,
        @Param("periodEnd") LocalDateTime periodEnd
    );

    /**
     * Find patients with at least one positive screen (SDOH-2 numerator).
     */
    @Query("SELECT s FROM HrsnScreeningSessionEntity s " +
           "WHERE s.tenantId = :tenantId " +
           "AND s.anyDomainPositive = true " +
           "AND s.screeningDate BETWEEN :periodStart AND :periodEnd")
    List<HrsnScreeningSessionEntity> findWithPositiveScreen(
        @Param("tenantId") String tenantId,
        @Param("periodStart") LocalDateTime periodStart,
        @Param("periodEnd") LocalDateTime periodEnd
    );

    /**
     * Count patients screened by domain for population metrics.
     */
    @Query("SELECT COUNT(DISTINCT s.patientId) FROM HrsnScreeningSessionEntity s " +
           "WHERE s.tenantId = :tenantId " +
           "AND s.foodInsecurityCompleted = true " +
           "AND s.screeningDate BETWEEN :periodStart AND :periodEnd")
    long countFoodInsecurityScreened(
        @Param("tenantId") String tenantId,
        @Param("periodStart") LocalDateTime periodStart,
        @Param("periodEnd") LocalDateTime periodEnd
    );

    @Query("SELECT COUNT(DISTINCT s.patientId) FROM HrsnScreeningSessionEntity s " +
           "WHERE s.tenantId = :tenantId " +
           "AND s.foodInsecurityPositive = true " +
           "AND s.screeningDate BETWEEN :periodStart AND :periodEnd")
    long countFoodInsecurityPositive(
        @Param("tenantId") String tenantId,
        @Param("periodStart") LocalDateTime periodStart,
        @Param("periodEnd") LocalDateTime periodEnd
    );

    /**
     * Get SDOH screening statistics for a tenant.
     */
    @Query("""
        SELECT
            COUNT(DISTINCT s.patientId) as totalScreened,
            SUM(CASE WHEN s.allDomainsScreened = true THEN 1 ELSE 0 END) as completeScreenings,
            SUM(CASE WHEN s.anyDomainPositive = true THEN 1 ELSE 0 END) as positiveScreenings,
            SUM(CASE WHEN s.foodInsecurityPositive = true THEN 1 ELSE 0 END) as foodInsecurityPositive,
            SUM(CASE WHEN s.housingInstabilityPositive = true THEN 1 ELSE 0 END) as housingInstabilityPositive,
            SUM(CASE WHEN s.transportationPositive = true THEN 1 ELSE 0 END) as transportationPositive,
            SUM(CASE WHEN s.utilitiesPositive = true THEN 1 ELSE 0 END) as utilitiesPositive,
            SUM(CASE WHEN s.interpersonalSafetyPositive = true THEN 1 ELSE 0 END) as interpersonalSafetyPositive
        FROM HrsnScreeningSessionEntity s
        WHERE s.tenantId = :tenantId
        AND s.screeningDate BETWEEN :periodStart AND :periodEnd
        AND s.status = 'COMPLETED'
        """)
    Object[] getScreeningStatistics(
        @Param("tenantId") String tenantId,
        @Param("periodStart") LocalDateTime periodStart,
        @Param("periodEnd") LocalDateTime periodEnd
    );

    /**
     * Find patients needing SDOH screening (no completed session in period).
     */
    @Query(value = """
        SELECT p.id as patient_id
        FROM patients p
        WHERE p.tenant_id = :tenantId
        AND NOT EXISTS (
            SELECT 1 FROM sdoh.hrsn_screening_sessions s
            WHERE s.patient_id = p.id
            AND s.tenant_id = :tenantId
            AND s.all_domains_screened = true
            AND s.screening_date BETWEEN :periodStart AND :periodEnd
        )
        """, nativeQuery = true)
    List<UUID> findPatientsNeedingScreening(
        @Param("tenantId") String tenantId,
        @Param("periodStart") LocalDateTime periodStart,
        @Param("periodEnd") LocalDateTime periodEnd
    );
}
