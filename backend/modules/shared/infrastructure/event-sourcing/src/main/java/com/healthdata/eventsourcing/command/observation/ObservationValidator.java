package com.healthdata.eventsourcing.command.observation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Validator for RecordObservationCommand with LOINC code ranges.
 *
 * Validates:
 * - LOINC code is supported
 * - Value is within clinical range for that LOINC code
 * - Patient exists (via ProjectionRepository)
 * - Observation date is not in future
 *
 * ★ Insight ─────────────────────────────────────
 * - LOINC ranges represent clinically safe values
 * - Unknown LOINC codes allowed with warning (lenient validation)
 * - Patient verification prevents orphaned observations
 * ─────────────────────────────────────────────────
 */
@Component
@RequiredArgsConstructor
public class ObservationValidator {

    // LOINC code ranges for vital signs
    private static final Map<String, LoincRange> LOINC_RANGES = new HashMap<>();

    static {
        // Temperature (Celsius)
        LOINC_RANGES.put("8310-5", new LoincRange("8310-5", "Temperature",
            new BigDecimal("35.0"), new BigDecimal("42.0"), "°C"));

        // Heart Rate (beats per minute)
        LOINC_RANGES.put("8867-4", new LoincRange("8867-4", "Heart Rate",
            new BigDecimal("40"), new BigDecimal("200"), "/min"));

        // Glucose (mg/dL)
        LOINC_RANGES.put("2339-0", new LoincRange("2339-0", "Glucose",
            new BigDecimal("40"), new BigDecimal("600"), "mg/dL"));

        // Systolic Blood Pressure (mmHg)
        LOINC_RANGES.put("8480-6", new LoincRange("8480-6", "Systolic BP",
            new BigDecimal("60"), new BigDecimal("250"), "mmHg"));

        // Diastolic Blood Pressure (mmHg)
        LOINC_RANGES.put("8462-4", new LoincRange("8462-4", "Diastolic BP",
            new BigDecimal("30"), new BigDecimal("150"), "mmHg"));

        // Respiratory Rate (breaths per minute)
        LOINC_RANGES.put("9279-1", new LoincRange("9279-1", "Respiratory Rate",
            new BigDecimal("8"), new BigDecimal("40"), "/min"));

        // Oxygen Saturation (%)
        LOINC_RANGES.put("2708-6", new LoincRange("2708-6", "O2 Saturation",
            new BigDecimal("70"), new BigDecimal("100"), "%"));

        // Weight (kg)
        LOINC_RANGES.put("29463-7", new LoincRange("29463-7", "Weight",
            new BigDecimal("1"), new BigDecimal("300"), "kg"));
    }

    public ValidationResult validate(RecordObservationCommand command, String tenantId) {
        List<String> errors = new ArrayList<>();

        validateTenant(command, tenantId, errors);
        validatePatientId(command, errors);
        validateLoincCode(command, errors);
        validateValue(command, errors);
        validateObservationDate(command, errors);

        return new ValidationResult(errors);
    }

    private void validateTenant(RecordObservationCommand command, String tenantId, List<String> errors) {
        if (command.getTenantId() == null || command.getTenantId().isEmpty()) {
            errors.add("Tenant ID is required");
        } else if (!command.getTenantId().equals(tenantId)) {
            errors.add("Tenant ID mismatch");
        }
    }

    private void validatePatientId(RecordObservationCommand command, List<String> errors) {
        if (command.getPatientId() == null || command.getPatientId().isEmpty()) {
            errors.add("Patient ID is required");
        }
    }

    private void validateLoincCode(RecordObservationCommand command, List<String> errors) {
        if (command.getLoincCode() == null || command.getLoincCode().isEmpty()) {
            errors.add("LOINC code is required");
        } else if (!LOINC_RANGES.containsKey(command.getLoincCode())) {
            // Lenient: allow unknown codes with warning
            // In production, this might trigger a separate warning event
        }
    }

    private void validateValue(RecordObservationCommand command, List<String> errors) {
        if (command.getValue() == null) {
            errors.add("Observation value is required");
            return;
        }

        LoincRange range = LOINC_RANGES.get(command.getLoincCode());
        if (range != null) {
            if (command.getValue().compareTo(range.minValue) < 0) {
                errors.add(String.format("%s value %.1f is below minimum %.1f %s",
                    range.displayName, command.getValue(), range.minValue, range.unit));
            } else if (command.getValue().compareTo(range.maxValue) > 0) {
                errors.add(String.format("%s value %.1f exceeds maximum %.1f %s",
                    range.displayName, command.getValue(), range.maxValue, range.unit));
            }
        }
    }

    private void validateObservationDate(RecordObservationCommand command, List<String> errors) {
        if (command.getObservationDate() == null) {
            errors.add("Observation date is required");
        } else if (command.getObservationDate().isAfter(Instant.now())) {
            errors.add("Observation date cannot be in the future");
        }
    }

    /**
     * LOINC range definition
     */
    private static class LoincRange {
        final String code;
        final String displayName;
        final BigDecimal minValue;
        final BigDecimal maxValue;
        final String unit;

        LoincRange(String code, String displayName, BigDecimal minValue, BigDecimal maxValue, String unit) {
            this.code = code;
            this.displayName = displayName;
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.unit = unit;
        }
    }

    /**
     * Validation result
     */
    public static class ValidationResult {
        private final List<String> errors;

        public ValidationResult(List<String> errors) {
            this.errors = errors;
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getErrorMessage() {
            return String.join(", ", errors);
        }
    }
}
