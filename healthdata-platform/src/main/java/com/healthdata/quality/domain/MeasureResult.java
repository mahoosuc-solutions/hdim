package com.healthdata.quality.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Quality Measure Result - Stores calculated measure outcomes
 */
@Entity
@Table(name = "measure_results", schema = "quality")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeasureResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String patientId;

    @Column(nullable = false)
    private String measureId;

    private Double score;
    private Integer numerator;
    private Integer denominator;
    private boolean compliant;

    @Column(nullable = false)
    private LocalDateTime calculationDate;

    private LocalDate periodStart;
    private LocalDate periodEnd;

    @Column(nullable = false)
    private String tenantId;

    @ElementCollection
    @CollectionTable(
        name = "measure_result_details",
        schema = "quality",
        joinColumns = @JoinColumn(name = "result_id")
    )
    @MapKeyColumn(name = "detail_key")
    @Column(name = "detail_value")
    private Map<String, String> details;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        calculationDate = LocalDateTime.now();
    }

    public double getPercentage() {
        if (denominator == null || denominator == 0) {
            return 0.0;
        }
        return (numerator * 100.0) / denominator;
    }

    public String getComplianceStatus() {
        return compliant ? "COMPLIANT" : "NON_COMPLIANT";
    }
}