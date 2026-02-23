package com.healthdata.fhir.persistence;

import java.time.Instant;
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

/**
 * JPA Entity representing a FHIR Practitioner resource.
 * Stores healthcare provider demographic and qualification data.
 * Practitioners are tenant-scoped but NOT patient-scoped.
 */
@Entity
@Table(name = "practitioners")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PractitionerEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "resource_type", nullable = false, length = 32)
    private String resourceType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resource_json", nullable = false, columnDefinition = "jsonb")
    private String resourceJson;

    @Column(name = "family_name", length = 255)
    private String familyName;

    @Column(name = "given_name", length = 255)
    private String givenName;

    @Column(name = "prefix", length = 64)
    private String prefix;

    @Column(name = "identifier_value", length = 255)
    private String identifierValue;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "qualification_code", length = 128)
    private String qualificationCode;

    @Column(name = "created_by", nullable = false, length = 255, updatable = false)
    private String createdBy;

    @Column(name = "last_modified_by", nullable = false, length = 255)
    private String lastModifiedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_modified_at", nullable = false)
    private Instant lastModifiedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.lastModifiedAt = now;
        if (this.version == null) {
            this.version = 0;
        }
        if (resourceType == null) {
            resourceType = "Practitioner";
        }
        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastModifiedAt = Instant.now();
    }
}
