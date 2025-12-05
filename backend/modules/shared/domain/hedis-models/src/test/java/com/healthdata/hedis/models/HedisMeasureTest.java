package com.healthdata.hedis.models;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HedisMeasureTest {

    @Test
    @DisplayName("validate should return no errors for a fully populated measure")
    void validate_shouldReturnNoErrorsForValidMeasure() {
        HedisMeasure measure = buildBaseMeasure();
        measure.setYear(2025);
        measure.setMeasurementPeriodStart(LocalDate.of(2025, 1, 1));
        measure.setMeasurementPeriodEnd(LocalDate.of(2025, 12, 31));

        List<String> errors = measure.validate();

        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("validate should capture missing required identifiers")
    void validate_shouldCaptureMissingIdentifiers() {
        HedisMeasure measure = buildBaseMeasure();
        measure.setMeasureId(null);
        measure.setName(" ");
        measure.setMeasurementPeriodStart(LocalDate.of(2024, 1, 1));
        measure.setMeasurementPeriodEnd(LocalDate.of(2024, 12, 31));

        List<String> errors = measure.validate();

        assertThat(errors)
                .anyMatch(error -> error.contains("measureId"))
                .anyMatch(error -> error.contains("name"));
    }

    @Test
    @DisplayName("validate should detect measurement period inconsistencies")
    void validate_shouldDetectMeasurementPeriodInconsistencies() {
        HedisMeasure measure = buildBaseMeasure();
        measure.setYear(2025);
        measure.setMeasurementPeriodStart(LocalDate.of(2024, 6, 1));
        measure.setMeasurementPeriodEnd(LocalDate.of(2024, 5, 31));

        List<String> errors = measure.validate();

        assertThat(errors)
                .anyMatch(error -> error.contains("measurementPeriodEnd"))
                .anyMatch(error -> error.contains("measurement period year"));
    }

    @Test
    @DisplayName("validate should require data elements and criteria")
    void validate_shouldRequireDataElementsAndCriteria() {
        HedisMeasure measure = buildBaseMeasure();
        measure.setNumeratorCriteria(null);
        measure.setDenominatorCriteria(" ");
        measure.setRequiredDataElements(List.of());

        List<String> errors = measure.validate();

        assertThat(errors)
                .anyMatch(error -> error.contains("numeratorCriteria"))
                .anyMatch(error -> error.contains("denominatorCriteria"))
                .anyMatch(error -> error.contains("requiredDataElements"));
    }

    private HedisMeasure buildBaseMeasure() {
        HedisMeasure measure = new HedisMeasure();
        measure.setMeasureId("CBP");
        measure.setName("Controlling High Blood Pressure");
        measure.setDescription("Patients 18-85 years diagnosed with hypertension whose BP was adequately controlled");
        measure.setDomain("Effectiveness of Care");
        measure.setSubDomain("Cardiovascular Care");
        measure.setYear(2024);
        measure.setUsedForStarRatings(true);
        measure.setNumeratorCriteria("Most recent blood pressure is <140/90 mm Hg");
        measure.setDenominatorCriteria("Adults 18-85 years with hypertension");
        measure.setExclusionCriteria("Patients with ESRD");
        measure.setRequiredDataElements(List.of("Condition(I10)", "Observation(85354-9)"));
        measure.setMeasurementPeriodStart(LocalDate.of(2024, 1, 1));
        measure.setMeasurementPeriodEnd(LocalDate.of(2024, 12, 31));
        measure.setNationalBenchmark(0.64);
        measure.setCqlLibraryName("HEDIS_CBP");
        measure.setFhirMeasureUrl("http://example.org/fhir/Measure/HEDIS-CBP");
        return measure;
    }
}
