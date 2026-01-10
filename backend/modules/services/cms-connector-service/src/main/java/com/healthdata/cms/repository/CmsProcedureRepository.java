package com.healthdata.cms.repository;

import com.healthdata.cms.model.CmsProcedure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CMS Procedure entities
 */
@Repository
public interface CmsProcedureRepository extends JpaRepository<CmsProcedure, UUID> {

    /**
     * Find all procedures for a patient within a tenant
     */
    List<CmsProcedure> findByPatientIdAndTenantId(String patientId, UUID tenantId);

    /**
     * Find procedures by tenant
     */
    List<CmsProcedure> findByTenantId(UUID tenantId);

    /**
     * Find procedure by ID and tenant (multi-tenant safety)
     */
    Optional<CmsProcedure> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Find procedure by content hash (for deduplication)
     */
    Optional<CmsProcedure> findByContentHash(String contentHash);

    /**
     * Check if procedure exists by content hash
     */
    boolean existsByContentHash(String contentHash);

    /**
     * Find completed procedures for a patient
     */
    @Query("SELECT p FROM CmsProcedure p WHERE p.patientId = :patientId AND p.tenantId = :tenantId AND p.status = 'completed'")
    List<CmsProcedure> findCompletedProcedures(@Param("patientId") String patientId, @Param("tenantId") UUID tenantId);

    /**
     * Find procedures by code
     */
    List<CmsProcedure> findByCodeSystemAndCodeValueAndTenantId(String codeSystem, String codeValue, UUID tenantId);

    /**
     * Find procedures performed after a specific date
     */
    @Query("SELECT p FROM CmsProcedure p WHERE p.tenantId = :tenantId AND p.performedDate >= :startDate")
    List<CmsProcedure> findProceduresAfterDate(@Param("tenantId") UUID tenantId, @Param("startDate") LocalDate startDate);

    /**
     * Find procedures within a date range
     */
    @Query("SELECT p FROM CmsProcedure p WHERE p.patientId = :patientId AND p.tenantId = :tenantId AND p.performedDate BETWEEN :startDate AND :endDate")
    List<CmsProcedure> findProceduresInDateRange(
            @Param("patientId") String patientId,
            @Param("tenantId") UUID tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Count procedures by patient and tenant
     */
    long countByPatientIdAndTenantId(String patientId, UUID tenantId);

    /**
     * Find unprocessed procedures
     */
    List<CmsProcedure> findByTenantIdAndIsProcessedFalse(UUID tenantId);
}
