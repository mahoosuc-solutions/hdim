package com.healthdata.documentation.persistence;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "document_metadata",
       indexes = {
           @Index(name = "idx_document_portal_type", columnList = "portalType"),
           @Index(name = "idx_document_category", columnList = "category"),
           @Index(name = "idx_document_status", columnList = "status"),
           @Index(name = "idx_document_tenant", columnList = "tenantId")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentMetadataEntity {

    // Core Identifiers
    @Id
    @Column(length = 100, nullable = false)
    private String id;

    @Column(length = 100, nullable = false)
    @NotBlank(message = "Title is required")
    @Size(min = 10, max = 100, message = "Title must be between 10 and 100 characters")
    private String title;

    @Column(length = 10, nullable = false)
    @NotBlank(message = "Portal type is required")
    @Pattern(regexp = "product|user|sales", message = "Invalid portal type")
    private String portalType;

    @Column(length = 255, nullable = false, unique = true)
    @NotBlank(message = "Path is required")
    private String path;

    // Organization
    @Column(length = 50, nullable = false)
    @NotBlank(message = "Category is required")
    private String category;

    @Column(length = 50)
    private String subcategory;

    @Type(StringArrayType.class)
    @Column(columnDefinition = "text[]", nullable = false)
    @NotNull
    @Size(min = 3, max = 10, message = "Must have between 3 and 10 tags")
    private String[] tags;

    @Type(StringArrayType.class)
    @Column(name = "related_documents", columnDefinition = "text[]", nullable = false)
    @NotNull
    @Size(max = 8, message = "Maximum 8 related documents")
    private String[] relatedDocuments = new String[]{};

    // Content Description
    @Column(columnDefinition = "TEXT", nullable = false)
    @NotBlank(message = "Summary is required")
    private String summary;

    @Column(nullable = false)
    @Min(value = 1, message = "Estimated read time must be positive")
    private Integer estimatedReadTime;

    @Column(length = 20)
    @Pattern(regexp = "beginner|intermediate|advanced", message = "Invalid difficulty level")
    private String difficulty;

    @Column(nullable = false)
    @NotNull
    private LocalDate lastUpdated;

    // Access & Governance
    @Type(StringArrayType.class)
    @Column(name = "target_audience", columnDefinition = "text[]", nullable = false)
    @NotNull
    @Size(min = 1, max = 5, message = "Must have between 1 and 5 target audiences")
    private String[] targetAudience;

    @Column(length = 20, nullable = false)
    @NotBlank(message = "Access level is required")
    @Pattern(regexp = "public|internal|restricted", message = "Invalid access level")
    private String accessLevel;

    @Column(length = 100, nullable = false)
    @NotBlank(message = "Owner is required")
    private String owner;

    @Column(length = 20, nullable = false)
    @NotBlank(message = "Review cycle is required")
    @Pattern(regexp = "monthly|quarterly|semi-annual|annual", message = "Invalid review cycle")
    private String reviewCycle;

    @Column(nullable = false)
    @NotNull
    private LocalDate nextReviewDate;

    // Status & Versioning
    @Column(length = 20, nullable = false)
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "draft|published|archived", message = "Invalid status")
    private String status;

    @Column(length = 10, nullable = false)
    @NotBlank(message = "Version is required")
    @Pattern(regexp = "^\\d+\\.\\d+$", message = "Version must follow format X.Y")
    private String version;

    @Column(nullable = false)
    @NotNull
    private LocalDate lastReviewed;

    // SEO & Discovery
    @Type(StringArrayType.class)
    @Column(name = "seo_keywords", columnDefinition = "text[]")
    @Size(max = 10, message = "Maximum 10 SEO keywords")
    private String[] seoKeywords;

    @Type(JsonBinaryType.class)
    @Column(name = "external_links", columnDefinition = "jsonb")
    private Map<String, Object> externalLinks;

    @Column(nullable = false)
    private Boolean hasVideo = false;

    @Column(length = 255)
    private String videoUrl;

    // Content Metrics
    @Column
    private Integer wordCount;

    @Column
    private LocalDate createdDate;

    @Column(nullable = false)
    private Integer viewCount = 0;

    @Column(precision = 2, scale = 1)
    @DecimalMin(value = "1.0", message = "Rating must be at least 1.0")
    @DecimalMax(value = "5.0", message = "Rating must not exceed 5.0")
    private BigDecimal avgRating;

    @Column(nullable = false)
    private Integer feedbackCount = 0;

    // Multi-tenancy
    @Column(length = 100, nullable = false)
    @NotBlank
    private String tenantId = "default";

    // Audit fields
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (createdDate == null) {
            createdDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Business logic
    public boolean isPublished() {
        return "published".equals(status);
    }

    public boolean isDraft() {
        return "draft".equals(status);
    }

    public boolean isArchived() {
        return "archived".equals(status);
    }

    public boolean needsReview() {
        return nextReviewDate != null && nextReviewDate.isBefore(LocalDate.now());
    }
}
