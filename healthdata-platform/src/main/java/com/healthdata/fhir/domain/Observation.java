package com.healthdata.fhir.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * FHIR Observation Entity
 * Represents clinical observations like vital signs, lab results, etc.
 */
@Entity
@Table(name = "observations", schema = "fhir")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Observation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String patientId;

    @Column(nullable = false)
    private String code;  // LOINC code

    private String system;  // Code system (e.g., "http://loinc.org")
    private String display;  // Human-readable name

    @Column(name = "value_quantity")
    private BigDecimal valueQuantity;

    @Column(name = "value_unit")
    private String valueUnit;

    @Column(name = "value_string")
    private String valueString;

    @Column(nullable = false)
    private String status;  // final, preliminary, etc.

    @Column(name = "effective_date")
    private LocalDateTime effectiveDate;

    private String category;  // vital-signs, laboratory, etc.

    @Column(nullable = false)
    private String tenantId;

    @Column(columnDefinition = "jsonb")
    private String fhirResource;  // Original FHIR JSON

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (effectiveDate == null) {
            effectiveDate = LocalDateTime.now();
        }
    }

    // Business methods
    public boolean isAbnormal() {
        // Simplified logic - would check against reference ranges
        return false;
    }

    public boolean isCritical() {
        // Check if value is in critical range
        return false;
    }
}