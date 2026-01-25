package com.healthdata.documentation.persistence;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Document Version Entity
 *
 * Tracks version history for product documentation.
 * Each version stores the content at that point in time.
 */
@Entity
@Table(name = "document_versions",
       indexes = {
           @Index(name = "idx_doc_version_document", columnList = "document_id"),
           @Index(name = "idx_doc_version_tenant", columnList = "tenant_id"),
           @Index(name = "idx_doc_version_number", columnList = "document_id, version_number")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVersionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "document_id", nullable = false, length = 100)
    private String documentId; // Reference to DocumentMetadataEntity

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "version_number", nullable = false, length = 50)
    private String versionNumber; // e.g., "1.0", "2.1"

    @Column(name = "content", columnDefinition = "TEXT")
    private String content; // Full content at this version

    @Column(name = "change_summary", columnDefinition = "TEXT")
    private String changeSummary; // Description of changes

    @Column(name = "changed_by", length = 100)
    private String changedBy;

    @Column(name = "is_major_version")  // Removed nullable=false to match Liquibase schema
    @Builder.Default
    private Boolean isMajorVersion = false;

    @Column(name = "is_published")  // Removed nullable=false to match Liquibase schema
    @Builder.Default
    private Boolean isPublished = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
    }
}
