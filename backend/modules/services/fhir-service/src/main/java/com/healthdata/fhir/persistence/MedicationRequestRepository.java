package com.healthdata.fhir.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicationRequestRepository extends JpaRepository<MedicationRequestEntity, UUID> {

    /**
     * Find medication requests for a specific patient
     */
    List<MedicationRequestEntity> findByTenantIdAndPatientIdOrderByAuthoredOnDesc(
            String tenantId, UUID patientId);

    /**
     * Find medication requests for a patient with pagination
     */
    Page<MedicationRequestEntity> findByTenantIdAndPatientIdOrderByAuthoredOnDesc(
            String tenantId, UUID patientId, Pageable pageable);

    /**
     * Find medication request by tenant and ID
     */
    Optional<MedicationRequestEntity> findByTenantIdAndId(String tenantId, UUID id);

    /**
     * Find medication requests by patient and medication code
     */
    List<MedicationRequestEntity> findByTenantIdAndPatientIdAndMedicationCodeOrderByAuthoredOnDesc(
            String tenantId, UUID patientId, String medicationCode);

    /**
     * Find medication requests by patient and status
     */
    List<MedicationRequestEntity> findByTenantIdAndPatientIdAndStatusOrderByAuthoredOnDesc(
            String tenantId, UUID patientId, String status);

    /**
     * Find medication requests by patient and category
     */
    List<MedicationRequestEntity> findByTenantIdAndPatientIdAndCategoryOrderByAuthoredOnDesc(
            String tenantId, UUID patientId, String category);

    /**
     * Find active medication requests for a patient (status = active)
     */
    @Query("SELECT m FROM MedicationRequestEntity m WHERE m.tenantId = :tenantId " +
           "AND m.patientId = :patientId AND m.status = 'active' " +
           "ORDER BY m.authoredOn DESC")
    List<MedicationRequestEntity> findActiveRequestsByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find medication requests by patient within a date range
     */
    @Query("SELECT m FROM MedicationRequestEntity m WHERE m.tenantId = :tenantId " +
           "AND m.patientId = :patientId " +
           "AND m.authoredOn BETWEEN :startDate AND :endDate " +
           "ORDER BY m.authoredOn DESC")
    List<MedicationRequestEntity> findByPatientAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find medication requests by medication code
     */
    List<MedicationRequestEntity> findByTenantIdAndMedicationCodeOrderByAuthoredOnDesc(
            String tenantId, String medicationCode);

    /**
     * Find medication requests by status
     */
    List<MedicationRequestEntity> findByTenantIdAndStatusOrderByAuthoredOnDesc(
            String tenantId, String status);

    /**
     * Find medication requests by intent
     */
    List<MedicationRequestEntity> findByTenantIdAndIntentOrderByAuthoredOnDesc(
            String tenantId, String intent);

    /**
     * Count medication requests for a patient
     */
    long countByTenantIdAndPatientId(String tenantId, UUID patientId);

    /**
     * Count active medication requests for a patient
     */
    @Query("SELECT COUNT(m) FROM MedicationRequestEntity m WHERE m.tenantId = :tenantId " +
           "AND m.patientId = :patientId AND m.status = 'active'")
    long countActiveRequestsByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Count medication requests by medication code
     */
    long countByTenantIdAndMedicationCode(String tenantId, String medicationCode);

    /**
     * Search medication requests by medication display text (case-insensitive contains)
     */
    @Query("SELECT m FROM MedicationRequestEntity m WHERE m.tenantId = :tenantId " +
           "AND LOWER(m.medicationDisplay) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY m.authoredOn DESC")
    List<MedicationRequestEntity> searchByMedicationDisplay(
            @Param("tenantId") String tenantId,
            @Param("searchTerm") String searchTerm);

    /**
     * Find prescriptions (intent = order)
     */
    @Query("SELECT m FROM MedicationRequestEntity m WHERE m.tenantId = :tenantId " +
           "AND m.patientId = :patientId AND m.intent = 'order' " +
           "ORDER BY m.authoredOn DESC")
    List<MedicationRequestEntity> findPrescriptionsByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find medication requests by requester
     */
    List<MedicationRequestEntity> findByTenantIdAndRequesterIdOrderByAuthoredOnDesc(
            String tenantId, String requesterId);

    /**
     * Check if patient has active medication request for a specific code
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END " +
           "FROM MedicationRequestEntity m WHERE m.tenantId = :tenantId " +
           "AND m.patientId = :patientId AND m.medicationCode = :medicationCode " +
           "AND m.status = 'active'")
    boolean hasActiveMedication(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("medicationCode") String medicationCode);

    /**
     * Find medication requests with refills remaining
     */
    @Query("SELECT m FROM MedicationRequestEntity m WHERE m.tenantId = :tenantId " +
           "AND m.patientId = :patientId " +
           "AND m.numberOfRepeatsAllowed > 0 " +
           "AND m.status = 'active' " +
           "ORDER BY m.authoredOn DESC")
    List<MedicationRequestEntity> findRequestsWithRefills(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find high-priority medication requests
     */
    @Query("SELECT m FROM MedicationRequestEntity m WHERE m.tenantId = :tenantId " +
           "AND m.patientId = :patientId AND m.priority = 'urgent' " +
           "AND m.status = 'active' " +
           "ORDER BY m.authoredOn DESC")
    List<MedicationRequestEntity> findUrgentRequestsByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);
}
