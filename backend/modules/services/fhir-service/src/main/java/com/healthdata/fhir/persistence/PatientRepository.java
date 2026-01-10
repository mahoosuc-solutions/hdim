package com.healthdata.fhir.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PatientRepository extends JpaRepository<PatientEntity, UUID> {

    List<PatientEntity> findByTenantIdAndLastNameContainingIgnoreCaseOrderByLastNameAsc(String tenantId, String lastNameFragment);

    Optional<PatientEntity> findByTenantIdAndId(String tenantId, UUID id);

    /**
     * Find patient by tenant and id with pessimistic write lock for upsert operations.
     * This prevents race conditions during concurrent inserts of the same patient.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PatientEntity p WHERE p.tenantId = :tenantId AND p.id = :id")
    Optional<PatientEntity> findByTenantIdAndIdForUpdate(@Param("tenantId") String tenantId, @Param("id") UUID id);

    /**
     * Find patient by tenant and id, excluding soft-deleted records.
     * This is the primary method for GET operations.
     */
    @Query("SELECT p FROM PatientEntity p WHERE p.tenantId = :tenantId AND p.id = :id AND p.deletedAt IS NULL")
    Optional<PatientEntity> findActiveByTenantIdAndId(@Param("tenantId") String tenantId, @Param("id") UUID id);

    /**
     * Batch find patients by tenant and multiple IDs, excluding soft-deleted records.
     * Issue #137: Optimize FHIR Queries for Primary Care Dashboard
     *
     * This query fetches multiple patients in a single database roundtrip,
     * replacing N+1 query patterns for dashboard loads.
     *
     * @param tenantId Tenant ID for multi-tenant isolation
     * @param ids List of patient UUIDs to fetch
     * @return List of active patient entities matching the IDs
     */
    @Query("SELECT p FROM PatientEntity p WHERE p.tenantId = :tenantId AND p.id IN :ids AND p.deletedAt IS NULL")
    List<PatientEntity> findActiveByTenantIdAndIdIn(@Param("tenantId") String tenantId, @Param("ids") List<UUID> ids);

    long countByTenantId(String tenantId);
}
