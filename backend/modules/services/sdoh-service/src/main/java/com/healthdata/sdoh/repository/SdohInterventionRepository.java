package com.healthdata.sdoh.repository;

import com.healthdata.sdoh.entity.SdohInterventionEntity;
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
 * Repository for SDOH intervention persistence.
 */
@Repository
public interface SdohInterventionRepository extends JpaRepository<SdohInterventionEntity, UUID> {

    Optional<SdohInterventionEntity> findByIdAndTenantId(UUID id, String tenantId);

    List<SdohInterventionEntity> findByTenantIdAndPatientId(String tenantId, UUID patientId);

    List<SdohInterventionEntity> findByScreeningSessionId(UUID screeningSessionId);

    Page<SdohInterventionEntity> findByTenantIdAndStatus(
        String tenantId,
        SdohInterventionEntity.InterventionStatus status,
        Pageable pageable
    );

    /**
     * Find interventions for a specific domain.
     */
    List<SdohInterventionEntity> findByTenantIdAndPatientIdAndDomain(
        String tenantId,
        UUID patientId,
        SdohInterventionEntity.HrsnDomain domain
    );

    /**
     * Find pending interventions that need follow-up.
     */
    @Query("SELECT i FROM SdohInterventionEntity i " +
           "WHERE i.tenantId = :tenantId " +
           "AND i.status IN ('PENDING', 'IN_PROGRESS') " +
           "AND i.createdAt < :cutoff " +
           "ORDER BY i.createdAt ASC")
    List<SdohInterventionEntity> findPendingNeedingFollowUp(
        @Param("tenantId") String tenantId,
        @Param("cutoff") LocalDateTime cutoff
    );

    /**
     * Check if patient has intervention for a specific domain from a screening session.
     */
    @Query("SELECT COUNT(i) > 0 FROM SdohInterventionEntity i " +
           "WHERE i.screeningSessionId = :sessionId " +
           "AND i.domain = :domain " +
           "AND i.status NOT IN ('CANCELLED', 'DECLINED')")
    boolean hasInterventionForDomain(
        @Param("sessionId") UUID sessionId,
        @Param("domain") SdohInterventionEntity.HrsnDomain domain
    );

    /**
     * Get intervention completion rates by domain.
     */
    @Query("""
        SELECT i.domain,
               COUNT(i) as total,
               SUM(CASE WHEN i.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed
        FROM SdohInterventionEntity i
        WHERE i.tenantId = :tenantId
        AND i.createdAt BETWEEN :periodStart AND :periodEnd
        GROUP BY i.domain
        """)
    List<Object[]> getInterventionCompletionByDomain(
        @Param("tenantId") String tenantId,
        @Param("periodStart") LocalDateTime periodStart,
        @Param("periodEnd") LocalDateTime periodEnd
    );

    /**
     * Count patients with positive screen but no intervention.
     */
    @Query(value = """
        SELECT COUNT(DISTINCT s.patient_id)
        FROM sdoh.hrsn_screening_sessions s
        WHERE s.tenant_id = :tenantId
        AND s.any_domain_positive = true
        AND s.screening_date BETWEEN :periodStart AND :periodEnd
        AND NOT EXISTS (
            SELECT 1 FROM sdoh.sdoh_interventions i
            WHERE i.screening_session_id = s.id
            AND i.status NOT IN ('CANCELLED', 'DECLINED')
        )
        """, nativeQuery = true)
    long countPositiveWithoutIntervention(
        @Param("tenantId") String tenantId,
        @Param("periodStart") LocalDateTime periodStart,
        @Param("periodEnd") LocalDateTime periodEnd
    );
}
