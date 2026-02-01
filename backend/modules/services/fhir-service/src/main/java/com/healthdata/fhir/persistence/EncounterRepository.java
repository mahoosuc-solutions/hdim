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

public interface EncounterRepository extends JpaRepository<EncounterEntity, UUID> {

    /**
     * Find encounters for a specific patient
     */
    List<EncounterEntity> findByTenantIdAndPatientIdOrderByPeriodStartDesc(
            String tenantId, UUID patientId);

    /**
     * Find encounters for a patient with pagination
     */
    Page<EncounterEntity> findByTenantIdAndPatientIdOrderByPeriodStartDesc(
            String tenantId, UUID patientId, Pageable pageable);

    /**
     * Find encounter by tenant and ID
     */
    Optional<EncounterEntity> findByTenantIdAndId(String tenantId, UUID id);

    /**
     * Find encounters by patient and encounter class
     */
    List<EncounterEntity> findByTenantIdAndPatientIdAndEncounterClassOrderByPeriodStartDesc(
            String tenantId, UUID patientId, String encounterClass);

    /**
     * Find encounters by patient and status
     */
    List<EncounterEntity> findByTenantIdAndPatientIdAndStatusOrderByPeriodStartDesc(
            String tenantId, UUID patientId, String status);

    /**
     * Find finished encounters for a patient (status = finished)
     */
    @Query("SELECT e FROM EncounterEntity e WHERE e.tenantId = :tenantId " +
           "AND e.patientId = :patientId AND e.status = 'finished' " +
           "ORDER BY e.periodStart DESC")
    List<EncounterEntity> findFinishedEncountersByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find in-progress encounters for a patient
     */
    @Query("SELECT e FROM EncounterEntity e WHERE e.tenantId = :tenantId " +
           "AND e.patientId = :patientId " +
           "AND e.status IN ('in-progress', 'arrived') " +
           "ORDER BY e.periodStart DESC")
    List<EncounterEntity> findActiveEncountersByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find encounters by patient within a date range
     */
    @Query("SELECT e FROM EncounterEntity e WHERE e.tenantId = :tenantId " +
           "AND e.patientId = :patientId " +
           "AND e.periodStart BETWEEN :startDate AND :endDate " +
           "ORDER BY e.periodStart DESC")
    List<EncounterEntity> findByPatientAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find encounters by tenant within a date range
     */
    @Query("SELECT e FROM EncounterEntity e WHERE e.tenantId = :tenantId " +
           "AND e.periodStart BETWEEN :startDate AND :endDate " +
           "ORDER BY e.periodStart DESC")
    List<EncounterEntity> findByTenantAndDateRange(
            @Param("tenantId") String tenantId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find inpatient encounters (encounter class = inpatient)
     */
    @Query("SELECT e FROM EncounterEntity e WHERE e.tenantId = :tenantId " +
           "AND e.patientId = :patientId " +
           "AND LOWER(e.encounterClass) = 'inpatient' " +
           "ORDER BY e.periodStart DESC")
    List<EncounterEntity> findInpatientEncountersByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find ambulatory encounters (encounter class = ambulatory)
     */
    @Query("SELECT e FROM EncounterEntity e WHERE e.tenantId = :tenantId " +
           "AND e.patientId = :patientId " +
           "AND LOWER(e.encounterClass) = 'ambulatory' " +
           "ORDER BY e.periodStart DESC")
    List<EncounterEntity> findAmbulatoryEncountersByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find emergency encounters (encounter class = emergency)
     */
    @Query("SELECT e FROM EncounterEntity e WHERE e.tenantId = :tenantId " +
           "AND e.patientId = :patientId " +
           "AND LOWER(e.encounterClass) = 'emergency' " +
           "ORDER BY e.periodStart DESC")
    List<EncounterEntity> findEmergencyEncountersByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find encounters by patient and encounter type code
     */
    List<EncounterEntity> findByTenantIdAndPatientIdAndEncounterTypeCodeOrderByPeriodStartDesc(
            String tenantId, UUID patientId, String encounterTypeCode);

    /**
     * Find encounters by patient and reason code
     */
    List<EncounterEntity> findByTenantIdAndPatientIdAndReasonCodeOrderByPeriodStartDesc(
            String tenantId, UUID patientId, String reasonCode);

    /**
     * Find encounters by provider/participant
     */
    List<EncounterEntity> findByTenantIdAndParticipantIdOrderByPeriodStartDesc(
            String tenantId, String participantId);

    /**
     * Find encounters by location
     */
    List<EncounterEntity> findByTenantIdAndLocationIdOrderByPeriodStartDesc(
            String tenantId, String locationId);

    /**
     * Find encounters by service provider
     */
    List<EncounterEntity> findByTenantIdAndServiceProviderIdOrderByPeriodStartDesc(
            String tenantId, String serviceProviderId);

    /**
     * Count encounters for a patient
     */
    long countByTenantIdAndPatientId(String tenantId, UUID patientId);

    /**
     * Count encounters by class for a patient
     */
    long countByTenantIdAndPatientIdAndEncounterClass(
            String tenantId, UUID patientId, String encounterClass);

    /**
     * Count inpatient encounters in date range (for utilization measures)
     */
    @Query("SELECT COUNT(e) FROM EncounterEntity e WHERE e.tenantId = :tenantId " +
           "AND e.patientId = :patientId " +
           "AND LOWER(e.encounterClass) = 'inpatient' " +
           "AND e.periodStart BETWEEN :startDate AND :endDate")
    long countInpatientEncountersInDateRange(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Count emergency encounters in date range
     */
    @Query("SELECT COUNT(e) FROM EncounterEntity e WHERE e.tenantId = :tenantId " +
           "AND e.patientId = :patientId " +
           "AND LOWER(e.encounterClass) = 'emergency' " +
           "AND e.periodStart BETWEEN :startDate AND :endDate")
    long countEmergencyEncountersInDateRange(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find encounters by service type
     */
    List<EncounterEntity> findByTenantIdAndServiceTypeCodeOrderByPeriodStartDesc(
            String tenantId, String serviceTypeCode);

    /**
     * Check if patient has encounter in date range
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END " +
           "FROM EncounterEntity e WHERE e.tenantId = :tenantId " +
           "AND e.patientId = :patientId " +
           "AND e.periodStart BETWEEN :startDate AND :endDate")
    boolean hasEncounterInDateRange(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find most recent encounter for patient
     */
    @Query("SELECT e FROM EncounterEntity e WHERE e.tenantId = :tenantId " +
           "AND e.patientId = :patientId " +
           "ORDER BY e.periodStart DESC LIMIT 1")
    Optional<EncounterEntity> findMostRecentEncounterByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Calculate total encounter duration for patient in date range (for utilization)
     */
    @Query("SELECT COALESCE(SUM(e.durationMinutes), 0) FROM EncounterEntity e " +
           "WHERE e.tenantId = :tenantId " +
           "AND e.patientId = :patientId " +
           "AND e.periodStart BETWEEN :startDate AND :endDate")
    Long calculateTotalDurationInDateRange(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
