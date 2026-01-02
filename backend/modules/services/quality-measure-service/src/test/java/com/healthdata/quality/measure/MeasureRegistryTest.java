package com.healthdata.quality.measure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.healthdata.quality.model.MeasureResult;
import com.healthdata.quality.model.PatientData;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Measure Registry Tests")
class MeasureRegistryTest {

    @Test
    @DisplayName("Should register calculators and return metadata")
    void shouldRegisterCalculatorsAndReturnMetadata() {
        MeasureCalculator calc1 = new TestCalculator("CDC", "Diabetes Care");
        MeasureCalculator calc2 = new TestCalculator("BCS", "Breast Cancer Screening");

        MeasureRegistry registry = new MeasureRegistry(List.of(calc1, calc2));
        registry.initialize();

        assertThat(registry.getMeasureIds()).containsExactlyInAnyOrder("CDC", "BCS");
        assertThat(registry.hasMeasure("CDC")).isTrue();
        assertThat(registry.getMeasuresMetadata()).hasSize(2);
        MeasureRegistry.MeasureMetadata metadata = registry.getMeasuresMetadata().get(0);
        assertThat(metadata.getMeasureId()).isNotEmpty();
        assertThat(metadata.getMeasureName()).isNotEmpty();
        assertThat(metadata.getVersion()).isEqualTo("2024");
    }

    @Test
    @DisplayName("Should calculate measures and skip failures")
    void shouldCalculateMeasuresAndSkipFailures() {
        MeasureCalculator ok = new TestCalculator("CDC", "Diabetes Care");
        MeasureCalculator failing = new MeasureCalculator() {
            @Override
            public MeasureResult calculate(PatientData patientData) {
                throw new RuntimeException("boom");
            }
            @Override
            public String getMeasureId() { return "FAIL"; }
            @Override
            public String getMeasureName() { return "Failing"; }
            @Override
            public String getVersion() { return "2024"; }
        };

        MeasureRegistry registry = new MeasureRegistry(List.of(ok, failing));
        registry.initialize();

        Patient patient = new Patient();
        patient.setId("patient-1");
        PatientData data = PatientData.builder().patient(patient).build();

        MeasureResult result = registry.calculateMeasure("CDC", data);
        assertThat(result.getMeasureId()).isEqualTo("CDC");

        assertThat(registry.calculateMeasures(List.of("CDC", "FAIL"), data)).containsKey("CDC");
    }

    @Test
    @DisplayName("Should replace duplicate measure registrations")
    void shouldReplaceDuplicateMeasureRegistrations() {
        MeasureCalculator first = new TestCalculator("CDC", "First Impl");
        MeasureCalculator second = new TestCalculator("CDC", "Second Impl");

        MeasureRegistry registry = new MeasureRegistry(List.of(first, second));
        registry.initialize();

        assertThat(registry.getMeasuresMetadata()).hasSize(1);
        assertThat(registry.getMeasure("CDC")).isSameAs(second);
    }

    @Test
    @DisplayName("Should skip unknown measures during batch calculation")
    void shouldSkipUnknownMeasuresDuringBatchCalculation() {
        MeasureCalculator ok = new TestCalculator("CDC", "Diabetes Care");
        MeasureRegistry registry = new MeasureRegistry(List.of(ok));
        registry.initialize();

        Patient patient = new Patient();
        patient.setId("patient-2");
        PatientData data = PatientData.builder().patient(patient).build();

        assertThat(registry.calculateMeasures(List.of("CDC", "UNKNOWN"), data))
            .containsKey("CDC")
            .doesNotContainKey("UNKNOWN");
    }

    @Test
    @DisplayName("Should calculate all registered measures")
    void shouldCalculateAllRegisteredMeasures() {
        MeasureCalculator calc1 = new TestCalculator("CDC", "Diabetes Care");
        MeasureCalculator calc2 = new TestCalculator("BCS", "Breast Cancer Screening");
        MeasureRegistry registry = new MeasureRegistry(List.of(calc1, calc2));
        registry.initialize();

        Patient patient = new Patient();
        patient.setId("patient-3");
        PatientData data = PatientData.builder().patient(patient).build();

        assertThat(registry.calculateAllMeasures(data))
            .containsKeys("CDC", "BCS");
    }

    @Test
    @DisplayName("Should wrap calculation failures with context")
    void shouldWrapCalculationFailuresWithContext() {
        MeasureCalculator failing = new MeasureCalculator() {
            @Override
            public MeasureResult calculate(PatientData patientData) {
                throw new IllegalStateException("bad calc");
            }
            @Override
            public String getMeasureId() { return "FAIL"; }
            @Override
            public String getMeasureName() { return "Failing"; }
            @Override
            public String getVersion() { return "2024"; }
        };

        MeasureRegistry registry = new MeasureRegistry(List.of(failing));
        registry.initialize();

        Patient patient = new Patient();
        patient.setId("patient-4");
        PatientData data = PatientData.builder().patient(patient).build();

        assertThatThrownBy(() -> registry.calculateMeasure("FAIL", data))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to calculate measure: FAIL");
    }

    @Test
    @DisplayName("Should throw when measure is unknown")
    void shouldThrowWhenMeasureUnknown() {
        MeasureRegistry registry = new MeasureRegistry(List.of());
        registry.initialize();

        Patient patient = new Patient();
        patient.setId("patient-1");
        PatientData data = PatientData.builder().patient(patient).build();

        assertThatThrownBy(() -> registry.calculateMeasure("UNKNOWN", data))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static final class TestCalculator implements MeasureCalculator {
        private final String id;
        private final String name;

        private TestCalculator(String id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public MeasureResult calculate(PatientData patientData) {
            return MeasureResult.builder()
                .measureId(id)
                .measureName(name)
                .patientId(UUID.randomUUID())
                .build();
        }

        @Override
        public String getMeasureId() { return id; }

        @Override
        public String getMeasureName() { return name; }

        @Override
        public String getVersion() { return "2024"; }
    }
}
