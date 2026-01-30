package com.healthdata.cql.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for Value Sets
 *
 * Stores SNOMED, LOINC, RxNorm, and other code system value sets
 * used in CQL expression evaluation.
 * Maps to the value_sets table created by Liquibase migration.
 */
@Entity
@Table(name = "value_sets",
    indexes = {
        @Index(name = "idx_vs_oid", columnList = "oid"),
        @Index(name = "idx_vs_name", columnList = "name"),
        @Index(name = "idx_vs_tenant", columnList = "tenant_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_vs_tenant_oid_version", columnNames = {"tenant_id", "oid", "version"})
    }
)
public class ValueSet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "oid", nullable = false, length = 255)
    private String oid; // Object Identifier (e.g., 2.16.840.1.113883.3.464.1003.101.12.1001)

    @Column(name = "name", nullable = false, length = 512)
    private String name;

    @Column(name = "version", length = 32)
    private String version;

    @Column(name = "code_system", length = 128)
    private String codeSystem; // SNOMED, LOINC, RxNorm, ICD-10, CPT, HCPCS

    @Column(name = "codes", columnDefinition = "TEXT")
    private String codes; // JSON array of codes

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "publisher", length = 255)
    private String publisher;

    @Column(name = "status", length = 32)
    private String status; // DRAFT, ACTIVE, RETIRED

    @Column(name = "fhir_value_set_id")
    private UUID fhirValueSetId;

    @Column(name = "active")
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Constructors
    public ValueSet() {
    }

    public ValueSet(String tenantId, String oid, String name, String codeSystem) {
        this.tenantId = tenantId;
        this.oid = oid;
        this.name = name;
        this.codeSystem = codeSystem;
        this.status = "ACTIVE";
        this.active = true;
    }

    // JPA Lifecycle Callbacks
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCodeSystem() {
        return codeSystem;
    }

    public void setCodeSystem(String codeSystem) {
        this.codeSystem = codeSystem;
    }

    public String getCodes() {
        return codes;
    }

    public void setCodes(String codes) {
        this.codes = codes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getFhirValueSetId() {
        return fhirValueSetId;
    }

    public void setFhirValueSetId(UUID fhirValueSetId) {
        this.fhirValueSetId = fhirValueSetId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "ValueSet{" +
                "id=" + id +
                ", tenantId='" + tenantId + '\'' +
                ", oid='" + oid + '\'' +
                ", name='" + name + '\'' +
                ", codeSystem='" + codeSystem + '\'' +
                ", active=" + active +
                '}';
    }
}
