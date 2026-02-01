package com.healthdata.cql.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for CQL Libraries
 *
 * Stores Clinical Quality Language (CQL) libraries for quality measure evaluation.
 * Maps to the cql_libraries table created by Liquibase migration.
 */
@Entity
@Table(name = "cql_libraries", indexes = {
    @Index(name = "idx_library_tenant", columnList = "tenant_id"),
    @Index(name = "idx_library_name_version", columnList = "library_name, version")
})
public class CqlLibrary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    // Database has both 'name' and 'library_name' columns - keep them in sync
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "library_name", nullable = false, length = 255)
    private String libraryName;

    @Column(name = "version", nullable = false, length = 32)
    private String version;

    @Column(name = "status", length = 32)
    private String status; // DRAFT, ACTIVE, RETIRED

    @Column(name = "cql_content", columnDefinition = "TEXT")
    private String cqlContent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "elm_json", columnDefinition = "JSONB")
    private String elmJson; // Expression Logical Model (compiled CQL)

    @Column(name = "elm_xml", columnDefinition = "TEXT")
    private String elmXml;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "publisher", length = 255)
    private String publisher;

    @Column(name = "fhir_library_id")
    private UUID fhirLibraryId;

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "measure_class", length = 255)
    private String measureClass; // Fully qualified Java class name for HEDIS measure implementation

    @Column(name = "category", length = 64)
    private String category; // Measure category (PREVENTIVE, CHRONIC_DISEASE, BEHAVIORAL_HEALTH, etc.)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    // Constructors
    public CqlLibrary() {
    }

    public CqlLibrary(String tenantId, String libraryName, String version) {
        this.tenantId = tenantId;
        this.libraryName = libraryName;
        this.name = libraryName; // Keep name in sync with libraryName
        this.version = version;
        this.status = "DRAFT";
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLibraryName() {
        return libraryName;
    }

    public void setLibraryName(String libraryName) {
        this.libraryName = libraryName;
        this.name = libraryName; // Keep name in sync with libraryName
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCqlContent() {
        return cqlContent;
    }

    public void setCqlContent(String cqlContent) {
        this.cqlContent = cqlContent;
    }

    public String getElmJson() {
        return elmJson;
    }

    public void setElmJson(String elmJson) {
        this.elmJson = elmJson;
    }

    public String getElmXml() {
        return elmXml;
    }

    public void setElmXml(String elmXml) {
        this.elmXml = elmXml;
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

    public UUID getFhirLibraryId() {
        return fhirLibraryId;
    }

    public void setFhirLibraryId(UUID fhirLibraryId) {
        this.fhirLibraryId = fhirLibraryId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getMeasureClass() {
        return measureClass;
    }

    public void setMeasureClass(String measureClass) {
        this.measureClass = measureClass;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Check if this library has a Java measure implementation
     */
    public boolean hasJavaImplementation() {
        return measureClass != null && !measureClass.isEmpty();
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public String toString() {
        return "CqlLibrary{" +
                "id=" + id +
                ", tenantId='" + tenantId + '\'' +
                ", libraryName='" + libraryName + '\'' +
                ", version='" + version + '\'' +
                ", status='" + status + '\'' +
                ", active=" + active +
                ", category='" + category + '\'' +
                ", measureClass='" + measureClass + '\'' +
                '}';
    }
}
