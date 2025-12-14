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

/**
 * Repository for MedicationAdministration FHIR resources.
 */
public interface MedicationAdministrationRepository extends JpaRepository<MedicationAdministrationEntity, UUID> {

    /**
     * Find medication administrations for a specific patient
     */
    List<MedicationAdministrationEntity> findByTenantIdAndPatientIdOrderByEffectiveDateTimeDesc(
            String tenantId, UUID patientId);

    /**
     * Find medication administrations for a patient with pagination
     */
    Page<MedicationAdministrationEntity> findByTenantIdAndPatientIdOrderByEffectiveDateTimeDesc(
            String tenantId, UUID patientId, Pageable pageable);

    /**
     * Find medication administration by tenant and ID
     */
    Optional<MedicationAdministrationEntity> findByTenantIdAndId(String tenantId, UUID id);

    /**
     * Find medication administrations by patient and medication code
     */
    List<MedicationAdministrationEntity> findByTenantIdAndPatientIdAndMedicationCodeOrderByEffectiveDateTimeDesc(
            String tenantId, UUID patientId, String medicationCode);

    /**
     * Find medication administrations by patient and status
     */
    List<MedicationAdministrationEntity> findByTenantIdAndPatientIdAndStatusOrderByEffectiveDateTimeDesc(
            String tenantId, UUID patientId, String status);

    /**
     * Find medication administrations by patient and encounter
     */
    List<MedicationAdministrationEntity> findByTenantIdAndPatientIdAndEncounterIdOrderByEffectiveDateTimeDesc(
            String tenantId, UUID patientId, UUID encounterId);

    /**
     * Find medication administrations for a specific encounter
     */
    List<MedicationAdministrationEntity> findByTenantIdAndEncounterIdOrderByEffectiveDateTimeDesc(
            String tenantId, UUID encounterId);

    /**
     * Find completed administrations for a patient
     */
    @Query("SELECT m FROM MedicationAdministrationEntity m WHERE m.tenantId = :tenantId " +
           "AND m.patientId = :patientId AND m.status = 'completed' " +
           "ORDER BY m.effectiveDateTime DESC")
    List<MedicationAdministrationEntity> findCompletedAdministrationsByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find in-progress administrations for a patient
     */
    @Query("SELECT m FROM MedicationAdministrationEntity m WHERE m.tenantId = :tenantId " +
           "AND m.patientId = :patientId AND m.status = 'in-progress' " +
           "ORDER BY m.effectiveDateTime DESC")
    List<MedicationAdministrationEntity> findInProgressAdministrationsByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find medication administrations by patient within a date range
     */
    @Query("SELECT m FROM MedicationAdministrationEntity m WHERE m.tenantId = :tenantId " +
           "AND m.patientId = :patientId " +
           "AND m.effectiveDateTime BETWEEN :startDate AND :endDate " +
           "ORDER BY m.effectiveDateTime DESC")
    List<MedicationAdministrationEntity> findByPatientAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find administrations by medication request (prescription)
     */
    List<MedicationAdministrationEntity> findByTenantIdAndMedicationRequestIdOrderByEffectiveDateTimeDesc(
            String tenantId, UUID medicationRequestId);

    /**
     * Find medication administrations by medication code
     */
    List<MedicationAdministrationEntity> findByTenantIdAndMedicationCodeOrderByEffectiveDateTimeDesc(
            String tenantId, String medicationCode);

    /**
     * Find medication administrations by status
     */
    List<MedicationAdministrationEntity> findByTenantIdAndStatusOrderByEffectiveDateTimeDesc(
            String tenantId, String status);

    /**
     * Find medication administrations by route
     */
    List<MedicationAdministrationEntity> findByTenantIdAndRouteCodeOrderByEffectiveDateTimeDesc(
            String tenantId, String routeCode);

    /**
     * Count medication administrations for a patient
     */
    long countByTenantIdAndPatientId(String tenantId, UUID patientId);

    /**
     * Count completed administrations for a patient
     */
    @Query("SELECT COUNT(m) FROM MedicationAdministrationEntity m WHERE m.tenantId = :tenantId " +
           "AND m.patientId = :patientId AND m.status = 'completed'")
    long countCompletedAdministrationsByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Count administrations by medication code
     */
    long countByTenantIdAndMedicationCode(String tenantId, String medicationCode);

    /**
     * Search medication administrations by medication display text (case-insensitive)
     */
    @Query("SELECT m FROM MedicationAdministrationEntity m WHERE m.tenantId = :tenantId " +
           "AND LOWER(m.medicationDisplay) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY m.effectiveDateTime DESC")
    List<MedicationAdministrationEntity> searchByMedicationDisplay(
            @Param("tenantId") String tenantId,
            @Param("searchTerm") String searchTerm);

    /**
     * Find administrations by performer
     */
    List<MedicationAdministrationEntity> findByTenantIdAndPerformerIdOrderByEffectiveDateTimeDesc(
            String tenantId, String performerId);

    /**
     * Find administrations where medication was not given
     */
    @Query("SELECT m FROM MedicationAdministrationEntity m WHERE m.tenantId = :tenantId " +
           "AND m.patientId = :patientId AND m.status = 'not-done' " +
           "ORDER BY m.effectiveDateTime DESC")
    List<MedicationAdministrationEntity> findNotDoneAdministrationsByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find administrations for a patient with specific lot number (for recalls)
     */
    List<MedicationAdministrationEntity> findByTenantIdAndPatientIdAndLotNumberOrderByEffectiveDateTimeDesc(
            String tenantId, UUID patientId, String lotNumber);

    /**
     * Find all administrations with a specific lot number across all patients
     */
    List<MedicationAdministrationEntity> findByTenantIdAndLotNumberOrderByEffectiveDateTimeDesc(
            String tenantId, String lotNumber);

    /**
     * Get administration history for a specific medication request
     */
    @Query("SELECT m FROM MedicationAdministrationEntity m WHERE m.tenantId = :tenantId " +
           "AND m.medicationRequestId = :requestId " +
           "ORDER BY m.effectiveDateTime ASC")
    List<MedicationAdministrationEntity> findAdministrationHistoryByRequest(
            @Param("tenantId") String tenantId,
            @Param("requestId") UUID requestId);

    /**
     * Check if medication has been administered today
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END " +
           "FROM MedicationAdministrationEntity m WHERE m.tenantId = :tenantId " +
           "AND m.patientId = :patientId AND m.medicationCode = :medicationCode " +
           "AND m.status = 'completed' " +
           "AND m.effectiveDateTime >= :startOfDay AND m.effectiveDateTime < :endOfDay")
    boolean hasMedicationBeenAdministeredToday(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("medicationCode") String medicationCode,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);
}
