package com.healthdata.quality.persistence;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Quality Measure Result Entity
 * Stores calculated quality measure results for patients
 */
@Hidden
@Schema(hidden = true)
@Entity
@Table(name = "quality_measure_results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityMeasureResultEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    // Measure information
    @Column(name = "measure_id", nullable = false, length = 100)
    private String measureId;

    @Column(name = "measure_name", nullable = false)
    private String measureName;

    @Column(name = "measure_category", length = 50)
    private String measureCategory; // HEDIS, CMS, custom

    @Column(name = "measure_year")
    private Integer measureYear;

    // Result details
    @Column(name = "numerator_compliant", nullable = false)
    private Boolean numeratorCompliant;

    @Column(name = "denominator_eligible", nullable = false)
    private Boolean denominatorElligible;

    @Column(name = "compliance_rate")
    private Double complianceRate;

    @Column(name = "score")
    private Double score;

    // Calculation details
    @Column(name = "calculation_date", nullable = false)
    private LocalDate calculationDate;

    @Column(name = "cql_library", length = 200)
    private String cqlLibrary;

    @Schema(hidden = true)
    @JsonIgnore
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cql_result", columnDefinition = "JSONB")
    private String cqlResult;

    // Audit fields
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, updatable = false, length = 100)
    private String createdBy;

    @Version
    @Column(name = "version")
    private Integer version;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
    }
}
