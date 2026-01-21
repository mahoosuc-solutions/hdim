package com.healthdata.nurseworkflow.domain.repository;

import com.healthdata.nurseworkflow.domain.model.PatientEducationLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for PatientEducationLogEntity
 *
 * Provides data access for patient education tracking with multi-tenant isolation.
 */
@Repository
public interface PatientEducationLogRepository extends JpaRepository<PatientEducationLogEntity, UUID> {

    /**
     * Find patient education logs for a specific patient
     */
    Page<PatientEducationLogEntity> findByTenantIdAndPatientIdOrderByDeliveredAtDesc(
        String tenantId,
        UUID patientId,
        Pageable pageable
    );

    /**
     * Find patient education logs by educator (nurse)
     */
    Page<PatientEducationLogEntity> findByTenantIdAndEducatorIdOrderByDeliveredAtDesc(
        String tenantId,
        UUID educatorId,
        Pageable pageable
    );

    /**
     * Find patient education logs by material type
     */
    Page<PatientEducationLogEntity> findByTenantIdAndMaterialTypeOrderByDeliveredAtDesc(
        String tenantId,
        PatientEducationLogEntity.MaterialType materialType,
        Pageable pageable
    );

    /**
     * Find patient education logs by delivery method
     */
    Page<PatientEducationLogEntity> findByTenantIdAndDeliveryMethodOrderByDeliveredAtDesc(
        String tenantId,
        PatientEducationLogEntity.DeliveryMethod deliveryMethod,
        Pageable pageable
    );

    /**
     * Find education logs for patient within date range
     */
    Page<PatientEducationLogEntity> findByTenantIdAndPatientIdAndDeliveredAtBetweenOrderByDeliveredAtDesc(
        String tenantId,
        UUID patientId,
        Instant startDate,
        Instant endDate,
        Pageable pageable
    );

    /**
     * Find education logs where patient understanding was poor (need follow-up)
     */
    @Query("""
        SELECT p FROM PatientEducationLogEntity p
        WHERE p.tenantId = :tenantId
        AND p.patientUnderstanding IN ('FAIR', 'POOR')
        ORDER BY p.deliveredAt DESC
    """)
    List<PatientEducationLogEntity> findPoorUnderstandingEducation(
        @Param("tenantId") String tenantId
    );

    /**
     * Find education delivered to patient by material type
     */
    List<PatientEducationLogEntity> findByTenantIdAndPatientIdAndMaterialTypeOrderByDeliveredAtDesc(
        String tenantId,
        UUID patientId,
        PatientEducationLogEntity.MaterialType materialType
    );

    /**
     * Count education sessions for patient
     */
    long countByTenantIdAndPatientId(String tenantId, UUID patientId);

    /**
     * Find education logs by FHIR DocumentReference
     */
    List<PatientEducationLogEntity> findByTenantIdAndDocumentReferenceIdOrderByDeliveredAtDesc(
        String tenantId,
        String documentReferenceId
    );

    /**
     * Find education sessions where interpreter was used (track language services)
     */
    @Query("""
        SELECT p FROM PatientEducationLogEntity p
        WHERE p.tenantId = :tenantId
        AND p.interpreterUsed = true
        ORDER BY p.deliveredAt DESC
    """)
    Page<PatientEducationLogEntity> findInterpretedEducationSessions(
        @Param("tenantId") String tenantId,
        Pageable pageable
    );
}
