package com.healthdata.cms.repository;

import com.healthdata.cms.model.CmsObservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CMS Observation entities
 */
@Repository
public interface CmsObservationRepository extends JpaRepository<CmsObservation, UUID> {

    /**
     * Find all observations for a patient within a tenant
     */
    List<CmsObservation> findByPatientIdAndTenantId(String patientId, UUID tenantId);

    /**
     * Find observations by tenant
     */
    List<CmsObservation> findByTenantId(UUID tenantId);

    /**
     * Find observation by ID and tenant (multi-tenant safety)
     */
    Optional<CmsObservation> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Find observation by content hash (for deduplication)
     */
    Optional<CmsObservation> findByContentHash(String contentHash);

    /**
     * Check if observation exists by content hash
     */
    boolean existsByContentHash(String contentHash);

    /**
     * Find observations by code (LOINC)
     */
    List<CmsObservation> findByCodeSystemAndCodeValueAndTenantId(String codeSystem, String codeValue, UUID tenantId);

    /**
     * Find observations for a patient by code
     */
    @Query("SELECT o FROM CmsObservation o WHERE o.patientId = :patientId AND o.tenantId = :tenantId AND o.codeValue = :codeValue ORDER BY o.effectiveDatetime DESC")
    List<CmsObservation> findPatientObservationsByCode(
            @Param("patientId") String patientId,
            @Param("tenantId") UUID tenantId,
            @Param("codeValue") String codeValue);

    /**
     * Find observations after a specific date
     */
    @Query("SELECT o FROM CmsObservation o WHERE o.tenantId = :tenantId AND o.effectiveDatetime >= :startDate")
    List<CmsObservation> findObservationsAfterDate(@Param("tenantId") UUID tenantId, @Param("startDate") LocalDateTime startDate);

    /**
     * Find observations by category (e.g., vital-signs, laboratory)
     */
    List<CmsObservation> findByPatientIdAndTenantIdAndCategory(String patientId, UUID tenantId, String category);

    /**
     * Find vital signs observations
     */
    @Query("SELECT o FROM CmsObservation o WHERE o.patientId = :patientId AND o.tenantId = :tenantId AND o.category = 'vital-signs' ORDER BY o.effectiveDatetime DESC")
    List<CmsObservation> findVitalSigns(@Param("patientId") String patientId, @Param("tenantId") UUID tenantId);

    /**
     * Find laboratory observations
     */
    @Query("SELECT o FROM CmsObservation o WHERE o.patientId = :patientId AND o.tenantId = :tenantId AND o.category = 'laboratory' ORDER BY o.effectiveDatetime DESC")
    List<CmsObservation> findLabResults(@Param("patientId") String patientId, @Param("tenantId") UUID tenantId);

    /**
     * Find abnormal observations
     */
    @Query("SELECT o FROM CmsObservation o WHERE o.patientId = :patientId AND o.tenantId = :tenantId AND o.interpretation IN ('abnormal', 'critical', 'high', 'low')")
    List<CmsObservation> findAbnormalObservations(@Param("patientId") String patientId, @Param("tenantId") UUID tenantId);

    /**
     * Find most recent observation of a type for a patient
     */
    @Query("SELECT o FROM CmsObservation o WHERE o.patientId = :patientId AND o.tenantId = :tenantId AND o.codeValue = :codeValue ORDER BY o.effectiveDatetime DESC")
    Optional<CmsObservation> findMostRecentByCode(
            @Param("patientId") String patientId,
            @Param("tenantId") UUID tenantId,
            @Param("codeValue") String codeValue);

    /**
     * Count observations by patient and tenant
     */
    long countByPatientIdAndTenantId(String patientId, UUID tenantId);

    /**
     * Find unprocessed observations
     */
    List<CmsObservation> findByTenantIdAndIsProcessedFalse(UUID tenantId);
}
