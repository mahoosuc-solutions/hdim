package com.healthdata.quality.persistence;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Hidden
@Schema(hidden = true)
@Entity
@Table(name = "custom_measures", indexes = {
        @Index(name = "idx_custom_measures_tenant", columnList = "tenant_id"),
        @Index(name = "idx_custom_measures_status", columnList = "status"),
        @Index(name = "idx_custom_measures_name", columnList = "name")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomMeasureEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "version", nullable = false, length = 50)
    private String version;

    @Column(name = "status", nullable = false, length = 50)
    private String status; // DRAFT, ACTIVE, RETIRED

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", length = 50)
    private String category; // HEDIS, CMS, CUSTOM

    @Column(name = "`year`")
    private Integer year;

    @JdbcTypeCode(SqlTypes.CLOB)
    @Column(name = "cql_text", columnDefinition = "TEXT")
    private String cqlText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "value_sets", columnDefinition = "JSONB")
    private String valueSets;

    @Column(name = "created_by", nullable = false, updatable = false, length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "published_date")
    private LocalDateTime publishedDate;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "DRAFT";
        }
        if (version == null) {
            version = "1.0.0";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
