package com.healthdata.fhir.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImmunizationRepository extends JpaRepository<ImmunizationEntity, UUID> {

    // Basic queries
    Optional<ImmunizationEntity> findByTenantIdAndId(String tenantId, UUID id);

    List<ImmunizationEntity> findByTenantIdAndPatientIdOrderByOccurrenceDateDesc(String tenantId, UUID patientId);

    // Completed immunizations
    @Query("SELECT i FROM ImmunizationEntity i WHERE i.tenantId = :tenantId AND i.patientId = :patientId " +
           "AND i.status = 'completed' ORDER BY i.occurrenceDate DESC")
    List<ImmunizationEntity> findCompletedImmunizations(@Param("tenantId") String tenantId,
                                                         @Param("patientId") UUID patientId);

    // Find by vaccine code (CVX code)
    @Query("SELECT i FROM ImmunizationEntity i WHERE i.tenantId = :tenantId AND i.patientId = :patientId " +
           "AND i.vaccineCode = :vaccineCode ORDER BY i.occurrenceDate DESC")
    List<ImmunizationEntity> findByVaccineCode(@Param("tenantId") String tenantId,
                                                @Param("patientId") UUID patientId,
                                                @Param("vaccineCode") String vaccineCode);

    // Check if patient has specific immunization
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM ImmunizationEntity i " +
           "WHERE i.tenantId = :tenantId AND i.patientId = :patientId AND i.vaccineCode = :vaccineCode " +
           "AND i.status = 'completed'")
    boolean hasImmunization(@Param("tenantId") String tenantId,
                           @Param("patientId") UUID patientId,
                           @Param("vaccineCode") String vaccineCode);

    // Count immunizations by vaccine code
    @Query("SELECT COUNT(i) FROM ImmunizationEntity i WHERE i.tenantId = :tenantId " +
           "AND i.patientId = :patientId AND i.vaccineCode = :vaccineCode AND i.status = 'completed'")
    long countByVaccineCode(@Param("tenantId") String tenantId,
                           @Param("patientId") UUID patientId,
                           @Param("vaccineCode") String vaccineCode);

    // Find immunizations in date range
    @Query("SELECT i FROM ImmunizationEntity i WHERE i.tenantId = :tenantId AND i.patientId = :patientId " +
           "AND i.occurrenceDate BETWEEN :startDate AND :endDate ORDER BY i.occurrenceDate DESC")
    List<ImmunizationEntity> findByOccurrenceDateRange(@Param("tenantId") String tenantId,
                                                        @Param("patientId") UUID patientId,
                                                        @Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate);

    // Find by performer
    @Query("SELECT i FROM ImmunizationEntity i WHERE i.tenantId = :tenantId AND i.performerId = :performerId " +
           "ORDER BY i.occurrenceDate DESC")
    List<ImmunizationEntity> findByPerformer(@Param("tenantId") String tenantId,
                                             @Param("performerId") String performerId);

    // Series tracking - find doses for a vaccine series
    @Query("SELECT i FROM ImmunizationEntity i WHERE i.tenantId = :tenantId AND i.patientId = :patientId " +
           "AND i.vaccineCode = :vaccineCode AND i.doseNumber IS NOT NULL " +
           "ORDER BY i.doseNumber ASC")
    List<ImmunizationEntity> findVaccineSeries(@Param("tenantId") String tenantId,
                                               @Param("patientId") UUID patientId,
                                               @Param("vaccineCode") String vaccineCode);

    // Check series completion
    @Query("SELECT CASE WHEN COUNT(i) >= :requiredDoses THEN true ELSE false END " +
           "FROM ImmunizationEntity i WHERE i.tenantId = :tenantId AND i.patientId = :patientId " +
           "AND i.vaccineCode = :vaccineCode AND i.status = 'completed'")
    boolean isSeriesComplete(@Param("tenantId") String tenantId,
                            @Param("patientId") UUID patientId,
                            @Param("vaccineCode") String vaccineCode,
                            @Param("requiredDoses") int requiredDoses);

    // Find immunizations with reactions
    @Query("SELECT i FROM ImmunizationEntity i WHERE i.tenantId = :tenantId AND i.patientId = :patientId " +
           "AND i.hadReaction = true ORDER BY i.occurrenceDate DESC")
    List<ImmunizationEntity> findImmunizationsWithReactions(@Param("tenantId") String tenantId,
                                                             @Param("patientId") UUID patientId);

    // Find by encounter
    List<ImmunizationEntity> findByTenantIdAndEncounterId(String tenantId, UUID encounterId);

    // Find by manufacturer
    @Query("SELECT i FROM ImmunizationEntity i WHERE i.tenantId = :tenantId AND i.manufacturer = :manufacturer " +
           "ORDER BY i.occurrenceDate DESC")
    List<ImmunizationEntity> findByManufacturer(@Param("tenantId") String tenantId,
                                                @Param("manufacturer") String manufacturer);

    // Find by lot number
    @Query("SELECT i FROM ImmunizationEntity i WHERE i.tenantId = :tenantId AND i.lotNumber = :lotNumber " +
           "ORDER BY i.occurrenceDate DESC")
    List<ImmunizationEntity> findByLotNumber(@Param("tenantId") String tenantId,
                                             @Param("lotNumber") String lotNumber);

    // Find by funding source
    @Query("SELECT i FROM ImmunizationEntity i WHERE i.tenantId = :tenantId AND i.patientId = :patientId " +
           "AND i.fundingSource = :fundingSource ORDER BY i.occurrenceDate DESC")
    List<ImmunizationEntity> findByFundingSource(@Param("tenantId") String tenantId,
                                                  @Param("patientId") UUID patientId,
                                                  @Param("fundingSource") String fundingSource);

    // Find primary source immunizations
    @Query("SELECT i FROM ImmunizationEntity i WHERE i.tenantId = :tenantId AND i.patientId = :patientId " +
           "AND i.primarySource = true ORDER BY i.occurrenceDate DESC")
    List<ImmunizationEntity> findPrimarySourceImmunizations(@Param("tenantId") String tenantId,
                                                             @Param("patientId") UUID patientId);

    // Find overdue immunizations (for quality measures)
    @Query("SELECT i.vaccineCode, MAX(i.occurrenceDate) as lastDate FROM ImmunizationEntity i " +
           "WHERE i.tenantId = :tenantId AND i.patientId = :patientId AND i.status = 'completed' " +
           "GROUP BY i.vaccineCode")
    List<Object[]> findLastImmunizationDates(@Param("tenantId") String tenantId,
                                             @Param("patientId") UUID patientId);

    // Count total immunizations
    @Query("SELECT COUNT(i) FROM ImmunizationEntity i WHERE i.tenantId = :tenantId " +
           "AND i.patientId = :patientId AND i.status = 'completed'")
    long countCompletedImmunizations(@Param("tenantId") String tenantId,
                                     @Param("patientId") UUID patientId);

    // Tenant-wide analytics
    @Query("SELECT COUNT(i) FROM ImmunizationEntity i WHERE i.tenantId = :tenantId " +
           "AND i.status = 'completed'")
    long countTenantImmunizations(@Param("tenantId") String tenantId);

    @Query("SELECT i.vaccineCode, COUNT(i) FROM ImmunizationEntity i WHERE i.tenantId = :tenantId " +
           "AND i.status = 'completed' GROUP BY i.vaccineCode ORDER BY COUNT(i) DESC")
    List<Object[]> countByVaccineCodeTenant(@Param("tenantId") String tenantId);

    // Find recent immunizations (last N days)
    @Query("SELECT i FROM ImmunizationEntity i WHERE i.tenantId = :tenantId AND i.patientId = :patientId " +
           "AND i.occurrenceDate >= :sinceDate AND i.status = 'completed' " +
           "ORDER BY i.occurrenceDate DESC")
    List<ImmunizationEntity> findRecentImmunizations(@Param("tenantId") String tenantId,
                                                     @Param("patientId") UUID patientId,
                                                     @Param("sinceDate") LocalDate sinceDate);
}
