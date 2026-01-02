package com.healthdata.fhir.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "immunizations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImmunizationEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    // Vaccine information (CVX codes)
    @Column(name = "vaccine_code", nullable = false)
    private String vaccineCode;          // CVX codes (CDC vaccine codes)

    @Column(name = "vaccine_system")
    private String vaccineSystem;

    @Column(name = "vaccine_display")
    private String vaccineDisplay;

    // Status
    @Column(name = "status", nullable = false)
    private String status;               // completed, entered-in-error, not-done

    @Column(name = "status_reason")
    private String statusReason;

    // Occurrence
    @Column(name = "occurrence_date", nullable = false)
    private LocalDate occurrenceDate;

    // Primary source
    @Column(name = "primary_source")
    private Boolean primarySource;

    // Lot details
    @Column(name = "lot_number")
    private String lotNumber;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    // Administration site and route
    @Column(name = "site")
    private String site;                 // Body site code (e.g., LA for left arm)

    @Column(name = "site_display")
    private String siteDisplay;

    @Column(name = "route")
    private String route;                // Route of administration (e.g., IM for intramuscular)

    @Column(name = "route_display")
    private String routeDisplay;

    // Dose information
    @Column(name = "dose_quantity")
    private Integer doseQuantity;

    @Column(name = "dose_unit")
    private String doseUnit;

    @Column(name = "dose_number")
    private Integer doseNumber;          // Dose number in series (e.g., 1, 2, 3)

    @Column(name = "series_doses")
    private Integer seriesDoses;         // Total doses in series

    // Performer
    @Column(name = "performer_id")
    private String performerId;

    @Column(name = "performer_display")
    private String performerDisplay;

    // Location
    @Column(name = "location_id")
    private String locationId;

    @Column(name = "location_display")
    private String locationDisplay;

    // Manufacturer
    @Column(name = "manufacturer")
    private String manufacturer;

    // Encounter reference
    @Column(name = "encounter_id")
    private UUID encounterId;

    // Reaction
    @Column(name = "had_reaction")
    private Boolean hadReaction;

    @Column(name = "reaction_detail", columnDefinition = "TEXT")
    private String reactionDetail;

    @Column(name = "reaction_date")
    private LocalDateTime reactionDate;

    // Education
    @Column(name = "education_document")
    private String educationDocument;

    @Column(name = "education_publication_date")
    private LocalDate educationPublicationDate;

    @Column(name = "education_presentation_date")
    private LocalDate educationPresentationDate;

    // Funding source
    @Column(name = "funding_source")
    private String fundingSource;

    // Program eligibility
    @Column(name = "program_eligibility")
    private String programEligibility;

    // Notes
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    // Reason codes
    @Column(name = "reason_code")
    private String reasonCode;

    @Column(name = "reason_display")
    private String reasonDisplay;

    // FHIR resource stored as JSONB
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fhir_resource", columnDefinition = "JSONB")
    private String fhirResource;

    // Audit fields
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "last_modified_by")
    private String lastModifiedBy;

    // Optimistic locking
    @Version
    @Column(name = "version")
    private Integer version;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        lastModifiedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastModifiedAt = LocalDateTime.now();
    }
}
