package com.healthdata.ecr.persistence;

import com.healthdata.ecr.persistence.converter.MapJsonConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing an Electronic Case Report (eCR).
 *
 * Tracks the lifecycle of reportable condition detection through
 * AIMS platform submission and reportability response processing.
 */
@Entity
@Table(name = "electronic_case_reports", schema = "ecr")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElectronicCaseReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "encounter_id")
    private UUID encounterId;

    /**
     * The code that triggered this case report (ICD-10, SNOMED, LOINC, etc.)
     */
    @Column(name = "trigger_code", nullable = false, length = 128)
    private String triggerCode;

    /**
     * Code system for the trigger code (ICD10CM, SNOMEDCT, LOINC, RXNORM)
     */
    @Column(name = "trigger_code_system", length = 64)
    private String triggerCodeSystem;

    /**
     * Display name of the triggering condition
     */
    @Column(name = "trigger_display", length = 500)
    private String triggerDisplay;

    /**
     * Category of trigger: diagnosis, lab_result, medication, procedure
     */
    @Column(name = "trigger_category", nullable = false, length = 64)
    @Enumerated(EnumType.STRING)
    private TriggerCategory triggerCategory;

    /**
     * The reportable condition name (e.g., "COVID-19", "Measles")
     */
    @Column(name = "condition_name", length = 255)
    private String conditionName;

    /**
     * Status of the eCR: PENDING, GENERATING, TRANSMITTING, SUBMITTED, ACKNOWLEDGED, FAILED
     */
    @Column(name = "status", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private EcrStatus status;

    /**
     * Urgency level: IMMEDIATE, WITHIN_24_HOURS, WITHIN_72_HOURS, ROUTINE
     */
    @Column(name = "urgency", length = 32)
    @Enumerated(EnumType.STRING)
    private EcrUrgency urgency;

    /**
     * The complete eICR FHIR Bundle as JSON
     */
    @Column(name = "eicr_bundle_json", columnDefinition = "jsonb")
    @Convert(converter = MapJsonConverter.class)
    private Map<String, Object> eicrBundleJson;

    /**
     * Reportability Response status: REPORTABLE, MAY_BE_REPORTABLE, NOT_REPORTABLE, NO_RULE_MET
     */
    @Column(name = "rr_status", length = 64)
    @Enumerated(EnumType.STRING)
    private ReportabilityStatus rrStatus;

    /**
     * Full Reportability Response as JSON
     */
    @Column(name = "rr_response_json", columnDefinition = "jsonb")
    @Convert(converter = MapJsonConverter.class)
    private Map<String, Object> rrResponseJson;

    /**
     * AIMS platform tracking ID for the submission
     */
    @Column(name = "aims_tracking_id", length = 255)
    private String aimsTrackingId;

    /**
     * Public health jurisdiction that received the report
     */
    @Column(name = "jurisdiction", length = 128)
    private String jurisdiction;

    /**
     * Error message if status is FAILED
     */
    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    /**
     * Number of transmission retry attempts
     */
    @Column(name = "retry_count")
    private Integer retryCount;

    /**
     * Next scheduled retry time (if applicable)
     */
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    /**
     * Timestamp when the triggering event occurred
     */
    @Column(name = "trigger_detected_at")
    private LocalDateTime triggerDetectedAt;

    /**
     * Timestamp when eICR was generated
     */
    @Column(name = "eicr_generated_at")
    private LocalDateTime eicrGeneratedAt;

    /**
     * Timestamp when submitted to AIMS
     */
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    /**
     * Timestamp when RR was received
     */
    @Column(name = "rr_received_at")
    private LocalDateTime rrReceivedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = EcrStatus.PENDING;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum TriggerCategory {
        DIAGNOSIS,
        LAB_RESULT,
        MEDICATION,
        PROCEDURE
    }

    public enum EcrStatus {
        PENDING,           // Trigger detected, awaiting eICR generation
        GENERATING,        // eICR bundle being generated
        TRANSMITTING,      // Sending to AIMS platform
        SUBMITTED,         // Successfully submitted, awaiting RR
        ACKNOWLEDGED,      // RR received and processed
        FAILED,            // Submission failed after retries
        CANCELLED          // Manually cancelled
    }

    public enum EcrUrgency {
        IMMEDIATE,         // Must transmit immediately (e.g., anthrax, smallpox)
        WITHIN_24_HOURS,   // Within 24 hours (e.g., measles, pertussis)
        WITHIN_72_HOURS,   // Within 72 hours (e.g., hepatitis A)
        ROUTINE            // Standard processing time
    }

    public enum ReportabilityStatus {
        REPORTABLE,           // Condition is reportable to jurisdiction
        MAY_BE_REPORTABLE,    // May be reportable, requires review
        NOT_REPORTABLE,       // Not reportable for this jurisdiction
        NO_RULE_MET           // No reporting rule matched
    }
}
