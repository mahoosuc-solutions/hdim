package com.healthdata.fhir.persistence;

import java.time.Instant;
import java.time.LocalDate;
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
@Table(name = "conditions")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ConditionEntity {

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

    @Column(name = "code", length = 128)
    private String code;

    @Column(name = "code_system", length = 128)
    private String codeSystem;

    @Column(name = "code_display", length = 512)
    private String codeDisplay;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "clinical_status", length = 32)
    private String clinicalStatus;

    @Column(name = "verification_status", length = 32)
    private String verificationStatus;

    @Column(name = "severity", length = 32)
    private String severity;

    @Column(name = "onset_date")
    private LocalDate onsetDate;

    @Column(name = "abatement_date")
    private LocalDate abatementDate;

    @Column(name = "recorded_date")
    private LocalDate recordedDate;

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
