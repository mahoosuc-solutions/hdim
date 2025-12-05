package com.healthdata.consent.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsentRepository extends JpaRepository<ConsentEntity, UUID> {

    /**
     * Find all consents for a patient
     */
    List<ConsentEntity> findByTenantIdAndPatientIdOrderByConsentDateDesc(
            String tenantId, UUID patientId);

    /**
     * Find consents for a patient with pagination
     */
    Page<ConsentEntity> findByTenantIdAndPatientIdOrderByConsentDateDesc(
            String tenantId, UUID patientId, Pageable pageable);

    /**
     * Find consent by tenant and ID
     */
    Optional<ConsentEntity> findByTenantIdAndId(String tenantId, UUID id);

    /**
     * Find active consents for a patient
     */
    @Query("SELECT c FROM ConsentEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId " +
           "AND c.status = 'active' " +
           "AND c.validFrom <= :today " +
           "AND (c.validTo IS NULL OR c.validTo >= :today) " +
           "ORDER BY c.consentDate DESC")
    List<ConsentEntity> findActiveConsentsByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("today") LocalDate today);

    /**
     * Find active consents for a patient and scope
     */
    @Query("SELECT c FROM ConsentEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId " +
           "AND c.scope = :scope " +
           "AND c.status = 'active' " +
           "AND c.validFrom <= :today " +
           "AND (c.validTo IS NULL OR c.validTo >= :today) " +
           "ORDER BY c.consentDate DESC")
    List<ConsentEntity> findActiveConsentsByPatientAndScope(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("scope") String scope,
            @Param("today") LocalDate today);

    /**
     * Find active consents for a patient and category
     */
    @Query("SELECT c FROM ConsentEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId " +
           "AND c.category = :category " +
           "AND c.status = 'active' " +
           "AND c.validFrom <= :today " +
           "AND (c.validTo IS NULL OR c.validTo >= :today) " +
           "ORDER BY c.consentDate DESC")
    List<ConsentEntity> findActiveConsentsByPatientAndCategory(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("category") String category,
            @Param("today") LocalDate today);

    /**
     * Find active consents for a patient and data class (e.g., substance-abuse)
     */
    @Query("SELECT c FROM ConsentEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId " +
           "AND c.dataClass = :dataClass " +
           "AND c.status = 'active' " +
           "AND c.validFrom <= :today " +
           "AND (c.validTo IS NULL OR c.validTo >= :today) " +
           "ORDER BY c.consentDate DESC")
    List<ConsentEntity> findActiveConsentsByPatientAndDataClass(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("dataClass") String dataClass,
            @Param("today") LocalDate today);

    /**
     * Find consents by patient and status
     */
    List<ConsentEntity> findByTenantIdAndPatientIdAndStatusOrderByConsentDateDesc(
            String tenantId, UUID patientId, String status);

    /**
     * Find consents by patient and category
     */
    List<ConsentEntity> findByTenantIdAndPatientIdAndCategoryOrderByConsentDateDesc(
            String tenantId, UUID patientId, String category);

    /**
     * Find consents by authorized party
     */
    List<ConsentEntity> findByTenantIdAndAuthorizedPartyIdOrderByConsentDateDesc(
            String tenantId, String authorizedPartyId);

    /**
     * Find consents by policy rule (e.g., '42-CFR-Part-2')
     */
    List<ConsentEntity> findByTenantIdAndPolicyRuleOrderByConsentDateDesc(
            String tenantId, String policyRule);

    /**
     * Find expired consents for a patient
     */
    @Query("SELECT c FROM ConsentEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId " +
           "AND c.validTo < :today " +
           "AND c.status = 'active' " +
           "ORDER BY c.validTo DESC")
    List<ConsentEntity> findExpiredConsentsByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("today") LocalDate today);

    /**
     * Find consents expiring soon (within specified days)
     */
    @Query("SELECT c FROM ConsentEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId " +
           "AND c.status = 'active' " +
           "AND c.validTo BETWEEN :today AND :expiryDate " +
           "ORDER BY c.validTo ASC")
    List<ConsentEntity> findConsentsExpiringSoon(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("today") LocalDate today,
            @Param("expiryDate") LocalDate expiryDate);

    /**
     * Find revoked consents for a patient
     */
    @Query("SELECT c FROM ConsentEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId " +
           "AND c.status = 'revoked' " +
           "ORDER BY c.revocationDate DESC")
    List<ConsentEntity> findRevokedConsentsByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId);

    /**
     * Check if patient has active consent for scope
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
           "FROM ConsentEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId " +
           "AND c.scope = :scope " +
           "AND c.status = 'active' " +
           "AND c.provisionType = 'permit' " +
           "AND c.validFrom <= :today " +
           "AND (c.validTo IS NULL OR c.validTo >= :today)")
    boolean hasActiveConsentForScope(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("scope") String scope,
            @Param("today") LocalDate today);

    /**
     * Check if patient has active consent for category
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
           "FROM ConsentEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId " +
           "AND c.category = :category " +
           "AND c.status = 'active' " +
           "AND c.provisionType = 'permit' " +
           "AND c.validFrom <= :today " +
           "AND (c.validTo IS NULL OR c.validTo >= :today)")
    boolean hasActiveConsentForCategory(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("category") String category,
            @Param("today") LocalDate today);

    /**
     * Check if patient has active consent for data class
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
           "FROM ConsentEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId " +
           "AND c.dataClass = :dataClass " +
           "AND c.status = 'active' " +
           "AND c.provisionType = 'permit' " +
           "AND c.validFrom <= :today " +
           "AND (c.validTo IS NULL OR c.validTo >= :today)")
    boolean hasActiveConsentForDataClass(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("dataClass") String dataClass,
            @Param("today") LocalDate today);

    /**
     * Check if authorized party has active consent to access patient data
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END " +
           "FROM ConsentEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId " +
           "AND c.authorizedPartyId = :authorizedPartyId " +
           "AND c.status = 'active' " +
           "AND c.provisionType = 'permit' " +
           "AND c.validFrom <= :today " +
           "AND (c.validTo IS NULL OR c.validTo >= :today)")
    boolean hasActiveConsentForAuthorizedParty(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("authorizedPartyId") String authorizedPartyId,
            @Param("today") LocalDate today);

    /**
     * Count active consents for a patient
     */
    @Query("SELECT COUNT(c) FROM ConsentEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId " +
           "AND c.status = 'active' " +
           "AND c.validFrom <= :today " +
           "AND (c.validTo IS NULL OR c.validTo >= :today)")
    long countActiveConsentsByPatient(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("today") LocalDate today);

    /**
     * Count consents by status for a patient
     */
    long countByTenantIdAndPatientIdAndStatus(
            String tenantId, UUID patientId, String status);

    /**
     * Find consents by consent date range
     */
    @Query("SELECT c FROM ConsentEntity c WHERE c.tenantId = :tenantId " +
           "AND c.patientId = :patientId " +
           "AND c.consentDate BETWEEN :startDate AND :endDate " +
           "ORDER BY c.consentDate DESC")
    List<ConsentEntity> findByPatientAndConsentDateRange(
            @Param("tenantId") String tenantId,
            @Param("patientId") UUID patientId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
