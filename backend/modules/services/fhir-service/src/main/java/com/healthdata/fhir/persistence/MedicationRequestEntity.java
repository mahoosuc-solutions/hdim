package com.healthdata.fhir.persistence;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "medication_requests")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MedicationRequestEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "resource_type", nullable = false, length = 32)
    private String resourceType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resource_json", nullable = false, columnDefinition = "jsonb")
    private String resourceJson;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "medication_code", length = 128)
    private String medicationCode;

    @Column(name = "medication_system", length = 128)
    private String medicationSystem;

    @Column(name = "medication_display", length = 512)
    private String medicationDisplay;

    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "intent", length = 32)
    private String intent;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "priority", length = 32)
    private String priority;

    @Column(name = "authored_on")
    private LocalDateTime authoredOn;

    @Column(name = "requester_id", length = 255)
    private String requesterId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dosage_instruction", columnDefinition = "jsonb")
    private String dosageInstruction;

    @Column(name = "dispense_quantity")
    private Double dispenseQuantity;

    @Column(name = "dispense_unit", length = 64)
    private String dispenseUnit;

    @Column(name = "number_of_repeats_allowed")
    private Integer numberOfRepeatsAllowed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_modified_at", nullable = false)
    private Instant lastModifiedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.lastModifiedAt = now;
        if (this.version == null) {
            this.version = 0;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.lastModifiedAt = Instant.now();
    }
}
