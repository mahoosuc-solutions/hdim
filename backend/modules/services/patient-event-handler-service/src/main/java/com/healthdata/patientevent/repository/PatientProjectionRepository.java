package com.healthdata.patientevent.repository;

import com.healthdata.patientevent.projection.PatientActiveProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PatientActiveProjection
 *
 * Provides database access to patient projections (denormalized read model).
 * Supports finding patients by ID and tenant for multi-tenant isolation.
 */
@Repository
public interface PatientProjectionRepository extends JpaRepository<PatientActiveProjection, String> {

    /**
     * Find a patient projection by patient ID and tenant ID
     *
     * Multi-tenant isolation: ensures patients from different tenants
     * are never mixed in queries.
     *
     * @param patientId Patient identifier
     * @param tenantId Tenant identifier for isolation
     * @return Patient projection if found, empty otherwise
     */
    Optional<PatientActiveProjection> findByPatientIdAndTenantId(String patientId, String tenantId);

    /**
     * Check if a patient exists for a tenant
     *
     * @param patientId Patient identifier
     * @param tenantId Tenant identifier
     * @return true if patient exists for this tenant
     */
    boolean existsByPatientIdAndTenantId(String patientId, String tenantId);

    /**
     * Find all patients that were merged into the target patient
     *
     * Returns direct merges only (one level). For recursive merge chain,
     * use PatientIdentifierResolver.findMergedSourcePatients().
     *
     * @param mergedIntoPatientId Target patient receiving merges
     * @param tenantId Tenant identifier for isolation
     * @return List of source patients merged into target
     */
    List<PatientActiveProjection> findByMergedIntoPatientIdAndTenantId(
        String mergedIntoPatientId, String tenantId
    );

    /**
     * Find all patients in a merge chain using recursive CTE
     *
     * Uses PostgreSQL recursive Common Table Expression (CTE) to traverse
     * the entire merge chain in a single database query, avoiding N+1 queries.
     *
     * Example: A → B → C (A merged into B, B merged into C)
     * Query for C returns [A, B] (all patients merged into chain leading to C)
     *
     * @param targetPatientId Final patient in merge chain
     * @param tenantId Tenant identifier for isolation
     * @param maxDepth Maximum recursion depth (prevents infinite loops)
     * @return List of all patients in merge chain, ordered from leaf to root
     */
    @Query(value = """
        WITH RECURSIVE merge_chain AS (
            -- Base case: Start with patients directly merged into target
            SELECT
                patient_id,
                tenant_id,
                merged_into_patient_id,
                identity_status,
                1 as depth
            FROM patient_projections
            WHERE merged_into_patient_id = :targetPatientId
              AND tenant_id = :tenantId
              AND identity_status = 'MERGED'

            UNION ALL

            -- Recursive case: Find patients merged into current level
            SELECT
                p.patient_id,
                p.tenant_id,
                p.merged_into_patient_id,
                p.identity_status,
                mc.depth + 1
            FROM patient_projections p
            INNER JOIN merge_chain mc ON p.merged_into_patient_id = mc.patient_id
            WHERE p.tenant_id = :tenantId
              AND p.identity_status = 'MERGED'
              AND mc.depth < :maxDepth  -- Prevent infinite recursion
        )
        SELECT DISTINCT patient_id FROM merge_chain ORDER BY depth DESC
        """, nativeQuery = true)
    List<String> findMergeChainRecursive(
        @Param("targetPatientId") String targetPatientId,
        @Param("tenantId") String tenantId,
        @Param("maxDepth") int maxDepth
    );
}
