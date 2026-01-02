package com.healthdata.fhir.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatientRepository extends JpaRepository<PatientEntity, UUID> {

    List<PatientEntity> findByTenantIdAndLastNameContainingIgnoreCaseOrderByLastNameAsc(String tenantId, String lastNameFragment);

    Optional<PatientEntity> findByTenantIdAndId(String tenantId, UUID id);

    /**
     * Find patient by tenant and id, excluding soft-deleted records.
     * This is the primary method for GET operations.
     */
    @Query("SELECT p FROM PatientEntity p WHERE p.tenantId = :tenantId AND p.id = :id AND p.deletedAt IS NULL")
    Optional<PatientEntity> findActiveByTenantIdAndId(@Param("tenantId") String tenantId, @Param("id") UUID id);

    long countByTenantId(String tenantId);
}
