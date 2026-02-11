package com.healthdata.consent.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA Entity representing a patient consent record.
 * Implements HIPAA 42 CFR Part 2 and GDPR consent requirements.
 */
@Entity
@Table(name = "consents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    // Patient who gave consent
    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    // Consent scope: 'read', 'write', 'full', 'research', 'billing'
    @Column(name = "scope", nullable = false, length = 64)
    private String scope;

    // Consent status: 'active', 'rejected', 'revoked', 'expired'
    @Column(name = "status", nullable = false, length = 32)
    private String status;

    // Consent category: 'treatment', 'payment', 'operations', 'research', 'marketing'
    @Column(name = "category", nullable = false, length = 64)
    private String category;

    // Purpose of data use
    @Column(name = "purpose", columnDefinition = "TEXT")
    private String purpose;

    // Who/what is authorized to access (organization, practitioner, application)
    @Column(name = "authorized_party_type", length = 64)
    private String authorizedPartyType;

    @Column(name = "authorized_party_id", length = 255)
    private String authorizedPartyId;

    @Column(name = "authorized_party_name", length = 512)
    private String authorizedPartyName;

    // Data classes covered by consent (e.g., 'substance-abuse', 'mental-health', 'hiv')
    @Column(name = "data_class", length = 128)
    private String dataClass;

    // Policy reference (HIPAA, GDPR, 42 CFR Part 2)
    @Column(name = "policy_rule", length = 128)
    private String policyRule;

    // Consent provision: 'permit', 'deny'
    @Column(name = "provision_type", nullable = false, length = 16)
    private String provisionType;

    // Validity period
    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    // Date when consent was given
    @Column(name = "consent_date", nullable = false)
    private LocalDate consentDate;

    // Verification method: 'electronic-signature', 'written', 'verbal'
    @Column(name = "verification_method", length = 64)
    private String verificationMethod;

    // Who verified the consent
    @Column(name = "verified_by", length = 255)
    private String verifiedBy;

    @Column(name = "verification_date")
    private LocalDate verificationDate;

    // Source document reference
    @Column(name = "source_attachment_id", length = 255)
    private String sourceAttachmentId;

    @Column(name = "source_attachment_url", length = 512)
    private String sourceAttachmentUrl;

    // Consent form version
    @Column(name = "consent_form_version", length = 64)
    private String consentFormVersion;

    // Patient language preference
    @Column(name = "language", length = 16)
    private String language;

    // Additional notes or restrictions
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Revocation details (if revoked)
    @Column(name = "revocation_date")
    private LocalDate revocationDate;

    @Column(name = "revocation_reason", columnDefinition = "TEXT")
    private String revocationReason;

    @Column(name = "revoked_by", length = 255)
    private String revokedBy;

    // Audit fields - using Instant to match database timestamptz type
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_modified_at", nullable = false)
    private Instant lastModifiedAt;

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    @Column(name = "last_modified_by", nullable = false, length = 255)
    private String lastModifiedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (lastModifiedAt == null) {
            lastModifiedAt = Instant.now();
        }
        if (provisionType == null) {
            provisionType = "permit";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastModifiedAt = Instant.now();
    }

    /**
     * Check if consent is currently active
     */
    public boolean isActive() {
        if (!"active".equals(status)) {
            return false;
        }

        LocalDate today = LocalDate.now();
        if (validFrom != null && today.isBefore(validFrom)) {
            return false;
        }
        if (validTo != null && today.isAfter(validTo)) {
            return false;
        }

        return true;
    }

    /**
     * Check if consent is expired
     */
    public boolean isExpired() {
        if (validTo == null) {
            return false;
        }
        return LocalDate.now().isAfter(validTo);
    }
}
