package com.healthdata.fhir.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AllergyIntoleranceRepository extends JpaRepository<AllergyIntoleranceEntity, UUID> {

    // Basic queries
    Optional<AllergyIntoleranceEntity> findByTenantIdAndId(String tenantId, UUID id);

    List<AllergyIntoleranceEntity> findByTenantIdAndPatientIdOrderByRecordedDateDesc(String tenantId, UUID patientId);

    // Active allergies (clinicalStatus = 'active')
    // Use CASE to properly order criticality: high=1, low=2, null=3 (lower number = higher priority)
    @Query("SELECT a FROM AllergyIntoleranceEntity a WHERE a.tenantId = :tenantId AND a.patientId = :patientId " +
           "AND a.clinicalStatus = 'active' ORDER BY CASE a.criticality WHEN 'high' THEN 1 WHEN 'low' THEN 2 ELSE 3 END, a.recordedDate DESC")
    List<AllergyIntoleranceEntity> findActiveAllergiesByPatient(@Param("tenantId") String tenantId,
                                                                 @Param("patientId") UUID patientId);

    // Critical allergies (criticality = 'high')
    @Query("SELECT a FROM AllergyIntoleranceEntity a WHERE a.tenantId = :tenantId AND a.patientId = :patientId " +
           "AND a.criticality = 'high' AND a.clinicalStatus = 'active' ORDER BY a.recordedDate DESC")
    List<AllergyIntoleranceEntity> findCriticalAllergies(@Param("tenantId") String tenantId,
                                                          @Param("patientId") UUID patientId);

    // Find by category (food, medication, environment, biologic)
    @Query("SELECT a FROM AllergyIntoleranceEntity a WHERE a.tenantId = :tenantId AND a.patientId = :patientId " +
           "AND a.category = :category AND a.clinicalStatus = 'active' ORDER BY a.recordedDate DESC")
    List<AllergyIntoleranceEntity> findByCategory(@Param("tenantId") String tenantId,
                                                   @Param("patientId") UUID patientId,
                                                   @Param("category") String category);

    // Food allergies
    @Query("SELECT a FROM AllergyIntoleranceEntity a WHERE a.tenantId = :tenantId AND a.patientId = :patientId " +
           "AND a.category = 'food' AND a.clinicalStatus = 'active' ORDER BY a.recordedDate DESC")
    List<AllergyIntoleranceEntity> findFoodAllergies(@Param("tenantId") String tenantId,
                                                      @Param("patientId") UUID patientId);

    // Medication allergies (critical for prescribing)
    @Query("SELECT a FROM AllergyIntoleranceEntity a WHERE a.tenantId = :tenantId AND a.patientId = :patientId " +
           "AND a.category = 'medication' AND a.clinicalStatus = 'active' ORDER BY a.criticality DESC")
    List<AllergyIntoleranceEntity> findMedicationAllergies(@Param("tenantId") String tenantId,
                                                            @Param("patientId") UUID patientId);

    // Find by verification status
    @Query("SELECT a FROM AllergyIntoleranceEntity a WHERE a.tenantId = :tenantId AND a.patientId = :patientId " +
           "AND a.verificationStatus = :status ORDER BY a.recordedDate DESC")
    List<AllergyIntoleranceEntity> findByVerificationStatus(@Param("tenantId") String tenantId,
                                                             @Param("patientId") UUID patientId,
                                                             @Param("status") String status);

    // Confirmed allergies (verificationStatus = 'confirmed')
    @Query("SELECT a FROM AllergyIntoleranceEntity a WHERE a.tenantId = :tenantId AND a.patientId = :patientId " +
           "AND a.verificationStatus = 'confirmed' AND a.clinicalStatus = 'active' ORDER BY a.criticality DESC")
    List<AllergyIntoleranceEntity> findConfirmedAllergies(@Param("tenantId") String tenantId,
                                                           @Param("patientId") UUID patientId);

    // Check if patient has specific allergy (by code)
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM AllergyIntoleranceEntity a " +
           "WHERE a.tenantId = :tenantId AND a.patientId = :patientId AND a.code = :code " +
           "AND a.clinicalStatus = 'active'")
    boolean hasActiveAllergy(@Param("tenantId") String tenantId,
                             @Param("patientId") UUID patientId,
                             @Param("code") String code);

    // Count allergies by criticality
    @Query("SELECT COUNT(a) FROM AllergyIntoleranceEntity a WHERE a.tenantId = :tenantId " +
           "AND a.patientId = :patientId AND a.criticality = :criticality AND a.clinicalStatus = 'active'")
    long countByCriticality(@Param("tenantId") String tenantId,
                           @Param("patientId") UUID patientId,
                           @Param("criticality") String criticality);

    // Count active allergies
    @Query("SELECT COUNT(a) FROM AllergyIntoleranceEntity a WHERE a.tenantId = :tenantId " +
           "AND a.patientId = :patientId AND a.clinicalStatus = 'active'")
    long countActiveAllergies(@Param("tenantId") String tenantId,
                              @Param("patientId") UUID patientId);

    // Find allergies with reactions
    @Query("SELECT a FROM AllergyIntoleranceEntity a WHERE a.tenantId = :tenantId AND a.patientId = :patientId " +
           "AND a.hasReactions = true ORDER BY a.reactionSeverity DESC, a.recordedDate DESC")
    List<AllergyIntoleranceEntity> findAllergiesWithReactions(@Param("tenantId") String tenantId,
                                                               @Param("patientId") UUID patientId);

    // Find by reaction severity
    @Query("SELECT a FROM AllergyIntoleranceEntity a WHERE a.tenantId = :tenantId AND a.patientId = :patientId " +
           "AND a.reactionSeverity = :severity AND a.clinicalStatus = 'active' ORDER BY a.recordedDate DESC")
    List<AllergyIntoleranceEntity> findByReactionSeverity(@Param("tenantId") String tenantId,
                                                           @Param("patientId") UUID patientId,
                                                           @Param("severity") String severity);

    // Find allergies recorded in date range
    @Query("SELECT a FROM AllergyIntoleranceEntity a WHERE a.tenantId = :tenantId AND a.patientId = :patientId " +
           "AND a.recordedDate BETWEEN :startDate AND :endDate ORDER BY a.recordedDate DESC")
    List<AllergyIntoleranceEntity> findByRecordedDateRange(@Param("tenantId") String tenantId,
                                                            @Param("patientId") UUID patientId,
                                                            @Param("startDate") LocalDateTime startDate,
                                                            @Param("endDate") LocalDateTime endDate);

    // Find by asserter (who reported the allergy)
    @Query("SELECT a FROM AllergyIntoleranceEntity a WHERE a.tenantId = :tenantId AND a.asserterId = :asserterId " +
           "ORDER BY a.recordedDate DESC")
    List<AllergyIntoleranceEntity> findByAsserter(@Param("tenantId") String tenantId,
                                                   @Param("asserterId") String asserterId);

    // Find by encounter
    List<AllergyIntoleranceEntity> findByTenantIdAndEncounterId(String tenantId, UUID encounterId);

    // Find resolved allergies
    @Query("SELECT a FROM AllergyIntoleranceEntity a WHERE a.tenantId = :tenantId AND a.patientId = :patientId " +
           "AND a.clinicalStatus = 'resolved' ORDER BY a.lastModifiedAt DESC")
    List<AllergyIntoleranceEntity> findResolvedAllergies(@Param("tenantId") String tenantId,
                                                          @Param("patientId") UUID patientId);

    // Find allergies by type (allergy vs intolerance)
    @Query("SELECT a FROM AllergyIntoleranceEntity a WHERE a.tenantId = :tenantId AND a.patientId = :patientId " +
           "AND a.type = :type AND a.clinicalStatus = 'active' ORDER BY a.recordedDate DESC")
    List<AllergyIntoleranceEntity> findByType(@Param("tenantId") String tenantId,
                                               @Param("patientId") UUID patientId,
                                               @Param("type") String type);

    // Count by category
    @Query("SELECT COUNT(a) FROM AllergyIntoleranceEntity a WHERE a.tenantId = :tenantId " +
           "AND a.patientId = :patientId AND a.category = :category AND a.clinicalStatus = 'active'")
    long countByCategory(@Param("tenantId") String tenantId,
                        @Param("patientId") UUID patientId,
                        @Param("category") String category);

    // Tenant-wide queries for analytics
    @Query("SELECT COUNT(a) FROM AllergyIntoleranceEntity a WHERE a.tenantId = :tenantId " +
           "AND a.clinicalStatus = 'active'")
    long countActiveTenantAllergies(@Param("tenantId") String tenantId);

    @Query("SELECT a.category, COUNT(a) FROM AllergyIntoleranceEntity a WHERE a.tenantId = :tenantId " +
           "AND a.clinicalStatus = 'active' GROUP BY a.category")
    List<Object[]> countByAllCategories(@Param("tenantId") String tenantId);
}
