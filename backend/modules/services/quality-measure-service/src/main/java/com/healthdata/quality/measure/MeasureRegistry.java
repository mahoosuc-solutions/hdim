package com.healthdata.quality.measure;

import com.healthdata.quality.model.MeasureResult;
import com.healthdata.quality.model.PatientData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Central registry for all HEDIS quality measure calculators.
 *
 * This service:
 * - Registers all available measure calculators
 * - Provides measure discovery and lookup
 * - Orchestrates measure calculation
 * - Supports batch calculation for multiple measures
 *
 * Based on JavaScript implementation from hedis-dashboard/measure-registry.js
 */
@Service
@Slf4j
public class MeasureRegistry {

    private final Map<String, MeasureCalculator> measures = new HashMap<>();
    private final List<MeasureCalculator> measureCalculators;

    /**
     * Spring will inject all beans implementing MeasureCalculator
     */
    @Autowired
    public MeasureRegistry(List<MeasureCalculator> measureCalculators) {
        this.measureCalculators = measureCalculators;
    }

    /**
     * Initialize the registry after bean construction
     */
    @PostConstruct
    public void initialize() {
        log.info("Initializing Measure Registry...");

        for (MeasureCalculator calculator : measureCalculators) {
            registerMeasure(calculator);
        }

        log.info("Registered {} HEDIS measures: {}",
            measures.size(),
            String.join(", ", measures.keySet()));
    }

    /**
     * Register a measure calculator
     */
    private void registerMeasure(MeasureCalculator calculator) {
        String measureId = calculator.getMeasureId();
        if (measures.containsKey(measureId)) {
            log.warn("Measure {} is already registered. Replacing with new implementation.", measureId);
        }
        measures.put(measureId, calculator);
        log.debug("Registered measure: {} - {}", measureId, calculator.getMeasureName());
    }

    /**
     * Calculate a single measure for a patient
     *
     * @param measureId Measure ID (e.g., "CDC", "CBP", "BCS")
     * @param patientData Patient's FHIR resources
     * @return Measure calculation result
     * @throws IllegalArgumentException if measure is not found
     */
    public MeasureResult calculateMeasure(String measureId, PatientData patientData) {
        log.debug("Calculating measure {} for patient {}", measureId, patientData.getPatient().getId());

        MeasureCalculator calculator = measures.get(measureId);
        if (calculator == null) {
            throw new IllegalArgumentException("Unknown measure: " + measureId);
        }

        try {
            MeasureResult result = calculator.calculate(patientData);
            log.debug("Measure {} calculation complete. Eligible: {}, Gaps: {}",
                measureId,
                result.isEligible(),
                result.getCareGaps().size());
            return result;
        } catch (Exception e) {
            log.error("Error calculating measure {} for patient {}",
                measureId, patientData.getPatient().getId(), e);
            throw new RuntimeException("Failed to calculate measure: " + measureId, e);
        }
    }

    /**
     * Calculate multiple measures for a patient
     *
     * @param measureIds List of measure IDs to calculate
     * @param patientData Patient's FHIR resources
     * @return Map of measure ID to result
     */
    public Map<String, MeasureResult> calculateMeasures(List<String> measureIds, PatientData patientData) {
        log.debug("Calculating {} measures for patient {}",
            measureIds.size(),
            patientData.getPatient().getId());

        Map<String, MeasureResult> results = new HashMap<>();

        for (String measureId : measureIds) {
            try {
                MeasureResult result = calculateMeasure(measureId, patientData);
                results.put(measureId, result);
            } catch (Exception e) {
                log.error("Skipping measure {} due to error: {}", measureId, e.getMessage());
                // Continue with other measures
            }
        }

        return results;
    }

    /**
     * Calculate ALL registered measures for a patient
     *
     * @param patientData Patient's FHIR resources
     * @return Map of measure ID to result
     */
    public Map<String, MeasureResult> calculateAllMeasures(PatientData patientData) {
        List<String> allMeasureIds = new ArrayList<>(measures.keySet());
        return calculateMeasures(allMeasureIds, patientData);
    }

    /**
     * Get all registered measure IDs
     */
    public List<String> getMeasureIds() {
        return new ArrayList<>(measures.keySet());
    }

    /**
     * Get metadata for all registered measures
     */
    public List<MeasureMetadata> getMeasuresMetadata() {
        return measures.values().stream()
            .map(calc -> new MeasureMetadata(
                calc.getMeasureId(),
                calc.getMeasureName(),
                calc.getVersion()
            ))
            .sorted(Comparator.comparing(MeasureMetadata::getMeasureId))
            .collect(Collectors.toList());
    }

    /**
     * Check if a measure is registered
     */
    public boolean hasMeasure(String measureId) {
        return measures.containsKey(measureId);
    }

    /**
     * Get a specific measure calculator
     */
    public MeasureCalculator getMeasure(String measureId) {
        return measures.get(measureId);
    }

    /**
     * Metadata about a registered measure
     */
    public static class MeasureMetadata {
        private final String measureId;
        private final String measureName;
        private final String version;

        public MeasureMetadata(String measureId, String measureName, String version) {
            this.measureId = measureId;
            this.measureName = measureName;
            this.version = version;
        }

        public String getMeasureId() { return measureId; }
        public String getMeasureName() { return measureName; }
        public String getVersion() { return version; }
    }
}
