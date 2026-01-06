package com.healthdata.quality.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Result Signatures - Immutable audit trail
 */
@Repository
public interface ResultSignatureRepository extends JpaRepository<ResultSignatureEntity, UUID> {

    /**
     * Check if a result has been signed
     */
    boolean existsByResultId(UUID resultId);

    /**
     * Find signature for a specific result
     */
    Optional<ResultSignatureEntity> findByResultIdAndTenantId(UUID resultId, String tenantId);

    /**
     * Find all signatures by a user within a date range
     */
    List<ResultSignatureEntity> findByTenantIdAndSignedByAndSignedAtBetween(
        String tenantId, String signedBy, LocalDateTime from, LocalDateTime to);

    /**
     * Find all signatures for a patient
     */
    List<ResultSignatureEntity> findByTenantIdAndPatientId(String tenantId, UUID patientId);

    /**
     * Count signatures by user on a specific date
     */
    long countBySignedByAndSignedAtBetween(String signedBy, LocalDateTime from, LocalDateTime to);
}
