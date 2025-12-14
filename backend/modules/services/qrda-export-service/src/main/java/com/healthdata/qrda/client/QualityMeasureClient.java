package com.healthdata.qrda.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Feign client for communicating with the quality-measure-service.
 *
 * Fetches measure results for QRDA Category III aggregate reporting.
 */
@FeignClient(
    name = "quality-measure-service",
    url = "${backend.services.quality-measure.url:http://localhost:8087}"
)
public interface QualityMeasureClient {

    /**
     * Get aggregate measure results for QRDA III reporting.
     */
    @GetMapping("/api/v1/quality/measures/aggregate")
    List<MeasureAggregateDTO> getAggregateResults(
        @RequestHeader("X-Tenant-Id") String tenantId,
        @RequestParam("measureIds") List<String> measureIds,
        @RequestParam("periodStart") LocalDate periodStart,
        @RequestParam("periodEnd") LocalDate periodEnd
    );

    /**
     * Get patient-level measure results for QRDA I reporting.
     */
    @GetMapping("/api/v1/quality/measures/patient/{patientId}")
    List<PatientMeasureResultDTO> getPatientMeasureResults(
        @RequestHeader("X-Tenant-Id") String tenantId,
        @PathVariable("patientId") UUID patientId,
        @RequestParam("measureIds") List<String> measureIds,
        @RequestParam("periodStart") LocalDate periodStart,
        @RequestParam("periodEnd") LocalDate periodEnd
    );

    /**
     * Get supplemental data for QRDA III reporting.
     */
    @GetMapping("/api/v1/quality/measures/supplemental")
    SupplementalDataDTO getSupplementalData(
        @RequestHeader("X-Tenant-Id") String tenantId,
        @RequestParam("measureId") String measureId,
        @RequestParam("periodStart") LocalDate periodStart,
        @RequestParam("periodEnd") LocalDate periodEnd
    );

    /**
     * Aggregate measure result DTO for QRDA III.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class MeasureAggregateDTO {
        private String measureId;
        private String measureVersionId;
        private String measureName;
        private String cms;  // CMS measure ID (e.g., CMS122v12)
        private String nqf;  // NQF number

        // Population counts
        private int initialPopulation;
        private int denominator;
        private int denominatorExclusions;
        private int denominatorExceptions;
        private int numerator;

        // Performance metrics
        private BigDecimal performanceRate;
        private BigDecimal reportingRate;

        // Stratification data
        private List<StratificationDTO> stratifications;

        // Supplemental data elements (race, ethnicity, sex, payer)
        private boolean hasSupplementalData;
    }

    /**
     * Patient-level measure result DTO for QRDA I.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class PatientMeasureResultDTO {
        private String measureId;
        private UUID patientId;

        // Population membership
        private boolean inInitialPopulation;
        private boolean inDenominator;
        private boolean inDenominatorExclusion;
        private boolean inDenominatorException;
        private boolean inNumerator;

        // Clinical data supporting the measure result
        private List<ClinicalFactDTO> relevantClinicalFacts;
    }

    /**
     * Stratification data DTO.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class StratificationDTO {
        private String stratificationId;
        private String stratificationName;
        private int denominator;
        private int numerator;
        private BigDecimal performanceRate;
    }

    /**
     * Supplemental data DTO for demographics breakdown.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class SupplementalDataDTO {
        private String measureId;

        // Race breakdown
        private List<DemographicBreakdownDTO> raceBreakdown;

        // Ethnicity breakdown
        private List<DemographicBreakdownDTO> ethnicityBreakdown;

        // Sex breakdown
        private List<DemographicBreakdownDTO> sexBreakdown;

        // Payer breakdown
        private List<DemographicBreakdownDTO> payerBreakdown;
    }

    /**
     * Demographic breakdown DTO.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class DemographicBreakdownDTO {
        private String code;
        private String displayName;
        private String codeSystem;
        private int count;
    }

    /**
     * Clinical fact supporting a measure result.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    class ClinicalFactDTO {
        private String factType;  // DIAGNOSIS, PROCEDURE, MEDICATION, etc.
        private String code;
        private String codeSystem;
        private String displayName;
        private LocalDate effectiveDate;
        private String value;
        private String valueUnit;
    }
}
