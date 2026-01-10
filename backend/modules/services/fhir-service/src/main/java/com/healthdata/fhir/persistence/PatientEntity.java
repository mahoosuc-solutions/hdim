package com.healthdata.fhir.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.healthdata.security.encryption.Encrypted;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

/**
 * JPA entity for FHIR Patient resources.
 * Implements Persistable to correctly handle pre-assigned UUIDs with JPA.
 */
@Entity
@Table(name = "patients")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PatientEntity implements Persistable<UUID> {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "resource_type", nullable = false, length = 32)
    private String resourceType;

    @Encrypted(value = "FHIR Patient Resource JSON", category = Encrypted.HipaaCategory.PHI)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resource_json", nullable = false, columnDefinition = "text")
    private String resourceJson;

    @Encrypted(value = "Patient First Name", category = Encrypted.HipaaCategory.PHI)
    @Column(name = "first_name", length = 356)
    private String firstName;

    @Encrypted(value = "Patient Last Name", category = Encrypted.HipaaCategory.PHI)
    @Column(name = "last_name", length = 356)
    private String lastName;

    @Column(name = "gender", length = 32)
    private String gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_modified_at", nullable = false)
    private Instant lastModifiedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.lastModifiedAt = now;
        // Don't set version here - let Hibernate's @Version handling manage it
    }

    @PreUpdate
    void onUpdate() {
        this.lastModifiedAt = Instant.now();
    }

    /**
     * Determines if entity is new based on version field.
     * Version is null for new entities (before first persist).
     * After first persist, Hibernate sets version to 0, then increments on updates.
     * This allows Spring Data JPA to use persist() instead of merge()
     * for entities with pre-assigned UUIDs.
     */
    @Override
    public boolean isNew() {
        // Version is null for new entities, non-null after first persist
        return version == null;
    }
}
