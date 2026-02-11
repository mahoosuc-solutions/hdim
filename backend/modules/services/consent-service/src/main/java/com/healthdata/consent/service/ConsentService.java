package com.healthdata.consent.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.consent.audit.ConsentAuditIntegration;
import com.healthdata.consent.persistence.ConsentEntity;
import com.healthdata.consent.persistence.ConsentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing patient consents.
 * Implements HIPAA 42 CFR Part 2 and GDPR consent requirements.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentService {

    private final ConsentRepository consentRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ConsentAuditIntegration consentAuditIntegration;

    /**
     * Create a new consent record
     */
    @Transactional
    public ConsentEntity createConsent(String tenantId, ConsentEntity consent, String createdBy) {
        log.info("Creating consent for tenant: {}, patient: {}", tenantId, consent.getPatientId());

        // Validate consent
        validateConsent(consent);

        // Set metadata
        consent.setTenantId(tenantId);
        consent.setCreatedBy(createdBy);
        consent.setLastModifiedBy(createdBy);
        consent.setCreatedAt(Instant.now());
        consent.setLastModifiedAt(Instant.now());

        // Set default status if not provided
        if (consent.getStatus() == null) {
            consent.setStatus("active");
        }

        // Save consent
        ConsentEntity saved = consentRepository.save(consent);
        log.info("Created consent with ID: {}", saved.getId());

        // Publish event
        publishConsentEvent("consent.created", tenantId, saved);

        // Publish audit event
        consentAuditIntegration.publishConsentGrantEvent(
            tenantId, saved, createdBy
        );

        return saved;
    }

    /**
     * Get a consent by ID
     */
    @Transactional(readOnly = true)
    public Optional<ConsentEntity> getConsent(String tenantId, UUID id) {
        log.debug("Retrieving consent {} for tenant: {}", id, tenantId);
        return consentRepository.findByTenantIdAndId(tenantId, id);
    }

    /**
     * Update a consent record
     */
    @Transactional
    public ConsentEntity updateConsent(String tenantId, UUID id, ConsentEntity consent, String modifiedBy) {
        log.info("Updating consent {} for tenant: {}", id, tenantId);

        // Find existing consent
        ConsentEntity existing = consentRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ConsentNotFoundException("Consent not found: " + id));

        // Validate consent
        validateConsent(consent);

        // Update fields
        existing.setScope(consent.getScope());
        existing.setStatus(consent.getStatus());
        existing.setCategory(consent.getCategory());
        existing.setPurpose(consent.getPurpose());
        existing.setAuthorizedPartyType(consent.getAuthorizedPartyType());
        existing.setAuthorizedPartyId(consent.getAuthorizedPartyId());
        existing.setAuthorizedPartyName(consent.getAuthorizedPartyName());
        existing.setDataClass(consent.getDataClass());
        existing.setPolicyRule(consent.getPolicyRule());
        existing.setProvisionType(consent.getProvisionType());
        existing.setValidFrom(consent.getValidFrom());
        existing.setValidTo(consent.getValidTo());
        existing.setVerificationMethod(consent.getVerificationMethod());
        existing.setVerifiedBy(consent.getVerifiedBy());
        existing.setVerificationDate(consent.getVerificationDate());
        existing.setConsentFormVersion(consent.getConsentFormVersion());
        existing.setLanguage(consent.getLanguage());
        existing.setNotes(consent.getNotes());
        existing.setLastModifiedBy(modifiedBy);

        // Save consent
        ConsentEntity updated = consentRepository.save(existing);
        log.info("Updated consent with ID: {}", updated.getId());

        // Publish event
        publishConsentEvent("consent.updated", tenantId, updated);

        // Publish audit event
        consentAuditIntegration.publishConsentUpdateEvent(
            tenantId, existing, updated, modifiedBy
        );

        return updated;
    }

    /**
     * Delete a consent record
     */
    @Transactional
    public void deleteConsent(String tenantId, UUID id, String deletedBy) {
        log.info("Deleting consent {} for tenant: {}", id, tenantId);

        ConsentEntity consent = consentRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ConsentNotFoundException("Consent not found: " + id));

        consentRepository.delete(consent);
        log.info("Deleted consent with ID: {}", id);

        // Publish event
        publishConsentEvent("consent.deleted", tenantId, consent);
    }

    /**
     * Revoke a consent
     */
    @Transactional
    public ConsentEntity revokeConsent(String tenantId, UUID id, String reason, String revokedBy) {
        log.info("Revoking consent {} for tenant: {}", id, tenantId);

        ConsentEntity consent = consentRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ConsentNotFoundException("Consent not found: " + id));

        // Update status to revoked
        consent.setStatus("revoked");
        consent.setRevocationDate(LocalDate.now());
        consent.setRevocationReason(reason);
        consent.setRevokedBy(revokedBy);
        consent.setLastModifiedBy(revokedBy);

        ConsentEntity revoked = consentRepository.save(consent);
        log.info("Revoked consent with ID: {}", revoked.getId());

        // Publish event
        publishConsentEvent("consent.revoked", tenantId, revoked);

        // Publish audit event
        consentAuditIntegration.publishConsentRevokeEvent(
            tenantId, revoked, reason, revokedBy
        );

        return revoked;
    }

    /**
     * Get all consents for a patient
     */
    @Transactional(readOnly = true)
    public List<ConsentEntity> getConsentsByPatient(String tenantId, UUID patientId) {
        log.debug("Retrieving consents for patient: {}, tenant: {}", patientId, tenantId);
        return consentRepository.findByTenantIdAndPatientIdOrderByConsentDateDesc(tenantId, patientId);
    }

    /**
     * Get consents for a patient with pagination
     */
    @Transactional(readOnly = true)
    public Page<ConsentEntity> getConsentsByPatient(String tenantId, UUID patientId, Pageable pageable) {
        log.debug("Retrieving consents for patient: {}, tenant: {} (paginated)", patientId, tenantId);
        return consentRepository.findByTenantIdAndPatientIdOrderByConsentDateDesc(tenantId, patientId, pageable);
    }

    /**
     * Get active consents for a patient
     */
    @Transactional(readOnly = true)
    public List<ConsentEntity> getActiveConsentsByPatient(String tenantId, UUID patientId) {
        log.debug("Retrieving active consents for patient: {}", patientId);
        return consentRepository.findActiveConsentsByPatient(tenantId, patientId, LocalDate.now());
    }

    /**
     * Get active consents for a patient and scope
     */
    @Transactional(readOnly = true)
    public List<ConsentEntity> getActiveConsentsByPatientAndScope(String tenantId, UUID patientId, String scope) {
        log.debug("Retrieving active consents for patient: {}, scope: {}", patientId, scope);
        return consentRepository.findActiveConsentsByPatientAndScope(tenantId, patientId, scope, LocalDate.now());
    }

    /**
     * Get active consents for a patient and category
     */
    @Transactional(readOnly = true)
    public List<ConsentEntity> getActiveConsentsByPatientAndCategory(String tenantId, UUID patientId, String category) {
        log.debug("Retrieving active consents for patient: {}, category: {}", patientId, category);
        return consentRepository.findActiveConsentsByPatientAndCategory(tenantId, patientId, category, LocalDate.now());
    }

    /**
     * Get active consents for a patient and data class
     */
    @Transactional(readOnly = true)
    public List<ConsentEntity> getActiveConsentsByPatientAndDataClass(String tenantId, UUID patientId, String dataClass) {
        log.debug("Retrieving active consents for patient: {}, dataClass: {}", patientId, dataClass);
        return consentRepository.findActiveConsentsByPatientAndDataClass(tenantId, patientId, dataClass, LocalDate.now());
    }

    /**
     * Get revoked consents for a patient
     */
    @Transactional(readOnly = true)
    public List<ConsentEntity> getRevokedConsentsByPatient(String tenantId, UUID patientId) {
        log.debug("Retrieving revoked consents for patient: {}", patientId);
        return consentRepository.findRevokedConsentsByPatient(tenantId, patientId);
    }

    /**
     * Get expired consents for a patient
     */
    @Transactional(readOnly = true)
    public List<ConsentEntity> getExpiredConsentsByPatient(String tenantId, UUID patientId) {
        log.debug("Retrieving expired consents for patient: {}", patientId);
        return consentRepository.findExpiredConsentsByPatient(tenantId, patientId, LocalDate.now());
    }

    /**
     * Get consents expiring soon (within specified days)
     */
    @Transactional(readOnly = true)
    public List<ConsentEntity> getConsentsExpiringSoon(String tenantId, UUID patientId, int daysAhead) {
        log.debug("Retrieving consents expiring within {} days for patient: {}", daysAhead, patientId);
        LocalDate today = LocalDate.now();
        LocalDate expiryDate = today.plusDays(daysAhead);
        return consentRepository.findConsentsExpiringSoon(tenantId, patientId, today, expiryDate);
    }

    /**
     * Check if patient has active consent for scope
     */
    @Transactional(readOnly = true)
    public boolean hasActiveConsentForScope(String tenantId, UUID patientId, String scope) {
        return consentRepository.hasActiveConsentForScope(tenantId, patientId, scope, LocalDate.now());
    }

    /**
     * Check if patient has active consent for category
     */
    @Transactional(readOnly = true)
    public boolean hasActiveConsentForCategory(String tenantId, UUID patientId, String category) {
        return consentRepository.hasActiveConsentForCategory(tenantId, patientId, category, LocalDate.now());
    }

    /**
     * Check if patient has active consent for data class
     */
    @Transactional(readOnly = true)
    public boolean hasActiveConsentForDataClass(String tenantId, UUID patientId, String dataClass) {
        return consentRepository.hasActiveConsentForDataClass(tenantId, patientId, dataClass, LocalDate.now());
    }

    /**
     * Check if authorized party has active consent to access patient data
     */
    @Transactional(readOnly = true)
    public boolean hasActiveConsentForAuthorizedParty(String tenantId, UUID patientId, String authorizedPartyId) {
        return consentRepository.hasActiveConsentForAuthorizedParty(tenantId, patientId, authorizedPartyId, LocalDate.now());
    }

    /**
     * Validate consent data access request
     * This is the main method for enforcing consent policies
     */
    @Transactional(readOnly = true)
    public ConsentValidationResult validateDataAccess(String tenantId, UUID patientId, String scope,
            String category, String dataClass, String authorizedPartyId) {
        log.debug("Validating data access for patient: {}, scope: {}, category: {}, dataClass: {}",
                patientId, scope, category, dataClass);

        // Get all active consents for the patient
        List<ConsentEntity> activeConsents = consentRepository.findActiveConsentsByPatient(
                tenantId, patientId, LocalDate.now());

        if (activeConsents.isEmpty()) {
            return ConsentValidationResult.denied("No active consents found for patient");
        }

        // Check for matching consent
        boolean hasMatchingConsent = activeConsents.stream()
                .anyMatch(consent -> matchesConsentCriteria(consent, scope, category, dataClass, authorizedPartyId));

        if (hasMatchingConsent) {
            return ConsentValidationResult.permitted("Active consent found");
        } else {
            return ConsentValidationResult.denied("No matching consent found for requested access");
        }
    }

    /**
     * Process expired consents (batch job)
     */
    @Transactional
    public int processExpiredConsents(String tenantId) {
        log.info("Processing expired consents for tenant: {}", tenantId);

        List<ConsentEntity> expiredConsents = consentRepository.findExpiredConsentsByPatient(
                tenantId, null, LocalDate.now());

        int count = 0;
        for (ConsentEntity consent : expiredConsents) {
            consent.setStatus("expired");
            consent.setLastModifiedBy("system");
            consentRepository.save(consent);

            // Publish event
            publishConsentEvent("consent.expired", tenantId, consent);
            count++;
        }

        log.info("Processed {} expired consents for tenant: {}", count, tenantId);
        return count;
    }

    // Helper methods

    private void validateConsent(ConsentEntity consent) {
        if (consent.getPatientId() == null) {
            throw new IllegalArgumentException("Patient ID is required");
        }
        if (consent.getScope() == null || consent.getScope().isEmpty()) {
            throw new IllegalArgumentException("Consent scope is required");
        }
        if (consent.getCategory() == null || consent.getCategory().isEmpty()) {
            throw new IllegalArgumentException("Consent category is required");
        }
        if (consent.getValidFrom() == null) {
            throw new IllegalArgumentException("Valid from date is required");
        }
        if (consent.getConsentDate() == null) {
            throw new IllegalArgumentException("Consent date is required");
        }
    }

    private boolean matchesConsentCriteria(ConsentEntity consent, String scope, String category,
            String dataClass, String authorizedPartyId) {
        // Check provision type (must be permit)
        if (!"permit".equals(consent.getProvisionType())) {
            return false;
        }

        // Check scope
        if (scope != null && !scope.equals(consent.getScope()) && !"full".equals(consent.getScope())) {
            return false;
        }

        // Check category
        if (category != null && !category.equals(consent.getCategory())) {
            return false;
        }

        // Check data class
        if (dataClass != null && consent.getDataClass() != null && !dataClass.equals(consent.getDataClass())) {
            return false;
        }

        // Check authorized party
        if (authorizedPartyId != null && consent.getAuthorizedPartyId() != null
                && !authorizedPartyId.equals(consent.getAuthorizedPartyId())) {
            return false;
        }

        return true;
    }

    private void publishConsentEvent(String topic, String tenantId, ConsentEntity consent) {
        try {
            String payload = objectMapper.writeValueAsString(consent);
            kafkaTemplate.send(topic, tenantId + ":" + consent.getId(), payload);
            log.debug("Published consent event to topic: {}", topic);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize consent for event", e);
        } catch (Exception e) {
            log.error("Failed to publish consent event", e);
        }
    }

    public static class ConsentNotFoundException extends RuntimeException {
        public ConsentNotFoundException(String message) {
            super(message);
        }
    }

    public static class ConsentValidationResult {
        private final boolean permitted;
        private final String reason;

        private ConsentValidationResult(boolean permitted, String reason) {
            this.permitted = permitted;
            this.reason = reason;
        }

        public static ConsentValidationResult permitted(String reason) {
            return new ConsentValidationResult(true, reason);
        }

        public static ConsentValidationResult denied(String reason) {
            return new ConsentValidationResult(false, reason);
        }

        public boolean isPermitted() {
            return permitted;
        }

        public String getReason() {
            return reason;
        }
    }
}
