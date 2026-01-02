package com.healthdata.fhir.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for FHIR CarePlan resources.
 * Provides tenant-scoped queries for care coordination plans.
 */
@Repository
public interface CarePlanRepository extends JpaRepository<CarePlanEntity, UUID> {

    /**
     * Find care plan by tenant and ID
     */
    Optional<CarePlanEntity> findByTenantIdAndIdAndDeletedAtIsNull(String tenantId, UUID id);

    /**
     * Find all care plans for a patient
     */
    List<CarePlanEntity> findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByCreatedDateDesc(
            String tenantId, UUID patientId);

    /**
     * Find care plans by patient and status
     */
    List<CarePlanEntity> findByTenantIdAndPatientIdAndStatusAndDeletedAtIsNull(
            String tenantId, UUID patientId, String status);

    /**
     * Find active care plans for a patient
     */
    @Query("SELECT c FROM CarePlanEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId " +
           "AND c.status = 'active' " +
           "AND c.deletedAt IS NULL " +
           "AND (c.periodStart IS NULL OR c.periodStart <= :asOf) " +
           "AND (c.periodEnd IS NULL OR c.periodEnd >= :asOf) " +
           "ORDER BY c.createdDate DESC")
    List<CarePlanEntity> findActiveCarePlansForPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("asOf") Instant asOf);

    /**
     * Find care plans by encounter
     */
    List<CarePlanEntity> findByTenantIdAndEncounterIdAndDeletedAtIsNullOrderByCreatedDateDesc(
            String tenantId, UUID encounterId);

    /**
     * Find care plans by category
     */
    List<CarePlanEntity> findByTenantIdAndPatientIdAndCategoryCodeAndDeletedAtIsNull(
            String tenantId, UUID patientId, String categoryCode);

    /**
     * Find care plans by intent
     */
    List<CarePlanEntity> findByTenantIdAndPatientIdAndIntentAndDeletedAtIsNull(
            String tenantId, UUID patientId, String intent);

    /**
     * Find primary (not part-of) care plans for a patient
     */
    @Query("SELECT c FROM CarePlanEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId " +
           "AND (c.partOfReference IS NULL OR c.partOfReference = '') " +
           "AND c.deletedAt IS NULL " +
           "ORDER BY c.createdDate DESC")
    List<CarePlanEntity> findPrimaryCarePlansForPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find child care plans (part of a specific plan)
     */
    List<CarePlanEntity> findByTenantIdAndPartOfReferenceAndDeletedAtIsNull(
            String tenantId, String partOfReference);

    /**
     * Find care plans addressing a specific condition
     */
    @Query("SELECT c FROM CarePlanEntity c WHERE c.tenantId = :tenantId " +
           "AND c.addressesReferences LIKE %:conditionRef% " +
           "AND c.deletedAt IS NULL " +
           "ORDER BY c.createdDate DESC")
    List<CarePlanEntity> findByAddresses(
            @Param("tenantId") String tenantId,
            @Param("conditionRef") String conditionRef);

    /**
     * Find care plans with a specific goal
     */
    @Query("SELECT c FROM CarePlanEntity c WHERE c.tenantId = :tenantId " +
           "AND c.goalReferences LIKE %:goalRef% " +
           "AND c.deletedAt IS NULL " +
           "ORDER BY c.createdDate DESC")
    List<CarePlanEntity> findByGoal(
            @Param("tenantId") String tenantId,
            @Param("goalRef") String goalRef);

    /**
     * Search care plans with multiple criteria
     */
    @Query("SELECT c FROM CarePlanEntity c WHERE c.tenantId = :tenantId " +
           "AND c.deletedAt IS NULL " +
           "AND (:patientId IS NULL OR c.patientId = :patientId) " +
           "AND (:encounterId IS NULL OR c.encounterId = :encounterId) " +
           "AND (:status IS NULL OR c.status = :status) " +
           "AND (:intent IS NULL OR c.intent = :intent) " +
           "AND (:categoryCode IS NULL OR c.categoryCode = :categoryCode) " +
           "ORDER BY c.createdDate DESC")
    Page<CarePlanEntity> searchCarePlans(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("encounterId") UUID encounterId,
            @Param("status") String status,
            @Param("intent") String intent,
            @Param("categoryCode") String categoryCode,
            Pageable pageable);

    /**
     * Find care plans by period (overlapping with date range)
     */
    @Query("SELECT c FROM CarePlanEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId " +
           "AND (c.periodStart IS NULL OR c.periodStart <= :endDate) " +
           "AND (c.periodEnd IS NULL OR c.periodEnd >= :startDate) " +
           "AND c.deletedAt IS NULL " +
           "ORDER BY c.periodStart DESC")
    List<CarePlanEntity> findByPeriodRange(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Find care plans by author
     */
    List<CarePlanEntity> findByTenantIdAndAuthorReferenceAndDeletedAtIsNull(
            String tenantId, String authorReference);

    /**
     * Count care plans by status for a patient
     */
    @Query("SELECT c.status, COUNT(c) FROM CarePlanEntity c " +
           "WHERE c.tenantId = :tenantId AND c.patientId = :patientId AND c.deletedAt IS NULL " +
           "GROUP BY c.status")
    List<Object[]> countByStatus(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Count care plans by category for a patient
     */
    @Query("SELECT c.categoryCode, c.categoryDisplay, COUNT(c) FROM CarePlanEntity c " +
           "WHERE c.tenantId = :tenantId AND c.patientId = :patientId AND c.deletedAt IS NULL " +
           "GROUP BY c.categoryCode, c.categoryDisplay")
    List<Object[]> countByCategory(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Find care plans that are about to expire
     */
    @Query("SELECT c FROM CarePlanEntity c WHERE c.tenantId = :tenantId " +
           "AND c.status = 'active' " +
           "AND c.periodEnd IS NOT NULL " +
           "AND c.periodEnd BETWEEN :startDate AND :endDate " +
           "AND c.deletedAt IS NULL " +
           "ORDER BY c.periodEnd ASC")
    List<CarePlanEntity> findExpiringCarePlans(
            @Param("tenantId") String tenantId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    /**
     * Find care plans with activities (activity_count > 0)
     */
    @Query("SELECT c FROM CarePlanEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId " +
           "AND c.activityCount > 0 " +
           "AND c.deletedAt IS NULL " +
           "ORDER BY c.createdDate DESC")
    List<CarePlanEntity> findWithActivities(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Full-text search in title and description
     */
    @Query("SELECT c FROM CarePlanEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId " +
           "AND (LOWER(c.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "     OR LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND c.deletedAt IS NULL " +
           "ORDER BY c.createdDate DESC")
    List<CarePlanEntity> searchByText(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("searchTerm") String searchTerm);

    /**
     * Find all care plans for tenant (paginated)
     */
    Page<CarePlanEntity> findByTenantIdAndDeletedAtIsNullOrderByLastModifiedAtDesc(
            String tenantId, Pageable pageable);
}
