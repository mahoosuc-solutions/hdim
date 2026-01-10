package com.healthdata.cms.repository;

import com.healthdata.cms.model.CmsMedicationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CMS Medication Request entities
 */
@Repository
public interface CmsMedicationRequestRepository extends JpaRepository<CmsMedicationRequest, UUID> {

    /**
     * Find all medication requests for a patient within a tenant
     */
    List<CmsMedicationRequest> findByPatientIdAndTenantId(String patientId, UUID tenantId);

    /**
     * Find medication requests by tenant
     */
    List<CmsMedicationRequest> findByTenantId(UUID tenantId);

    /**
     * Find medication request by ID and tenant (multi-tenant safety)
     */
    Optional<CmsMedicationRequest> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Find medication request by content hash (for deduplication)
     */
    Optional<CmsMedicationRequest> findByContentHash(String contentHash);

    /**
     * Check if medication request exists by content hash
     */
    boolean existsByContentHash(String contentHash);

    /**
     * Find active medication requests for a patient
     */
    @Query("SELECT m FROM CmsMedicationRequest m WHERE m.patientId = :patientId AND m.tenantId = :tenantId AND m.status = 'active'")
    List<CmsMedicationRequest> findActiveMedications(@Param("patientId") String patientId, @Param("tenantId") UUID tenantId);

    /**
     * Find medication requests by medication code
     */
    List<CmsMedicationRequest> findByMedicationCodeSystemAndMedicationCodeValueAndTenantId(
            String medicationCodeSystem, String medicationCodeValue, UUID tenantId);

    /**
     * Find medication requests authored after a specific date
     */
    @Query("SELECT m FROM CmsMedicationRequest m WHERE m.tenantId = :tenantId AND m.authoredOn >= :startDate")
    List<CmsMedicationRequest> findMedicationsAfterDate(@Param("tenantId") UUID tenantId, @Param("startDate") LocalDate startDate);

    /**
     * Find medication requests by status
     */
    List<CmsMedicationRequest> findByPatientIdAndTenantIdAndStatus(String patientId, UUID tenantId, String status);

    /**
     * Count medication requests by patient and tenant
     */
    long countByPatientIdAndTenantId(String patientId, UUID tenantId);

    /**
     * Find unprocessed medication requests
     */
    List<CmsMedicationRequest> findByTenantIdAndIsProcessedFalse(UUID tenantId);

    /**
     * Find medications with refills remaining
     */
    @Query("SELECT m FROM CmsMedicationRequest m WHERE m.patientId = :patientId AND m.tenantId = :tenantId AND m.status = 'active' AND m.numberOfRefills > 0")
    List<CmsMedicationRequest> findMedicationsWithRefills(@Param("patientId") String patientId, @Param("tenantId") UUID tenantId);
}
