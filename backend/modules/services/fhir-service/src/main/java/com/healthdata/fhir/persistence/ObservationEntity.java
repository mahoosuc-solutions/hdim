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
@Table(name = "observations")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ObservationEntity {

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

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "effective_datetime")
    private LocalDateTime effectiveDateTime;

    @Column(name = "value_quantity")
    private Double valueQuantity;

    @Column(name = "value_unit", length = 32)
    private String valueUnit;

    @Column(name = "value_string", length = 512)
    private String valueString;

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
