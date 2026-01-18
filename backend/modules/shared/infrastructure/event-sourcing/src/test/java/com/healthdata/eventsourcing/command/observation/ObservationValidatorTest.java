package com.healthdata.eventsourcing.command.observation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for ObservationValidator with LOINC code validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ObservationValidator LOINC Tests")
class ObservationValidatorTest {

    private ObservationValidator validator;
    private final String TENANT_ID = "tenant-123";
    private final String PATIENT_ID = "patient-456";

    @BeforeEach
    void setUp() {
        validator = new ObservationValidator();
    }

    // ========== Temperature Tests (8310-5) ==========
    @Test
    @DisplayName("Should accept valid temperature (37.5°C)")
    void shouldAcceptValidTemperature() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("8310-5")
            .value(new BigDecimal("37.5"))
            .unit("°C")
            .observationDate(Instant.now().minusSeconds(60))
            .build();

        ObservationValidator.ValidationResult result = validator.validate(command, TENANT_ID);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Should reject temperature below 35°C")
    void shouldRejectTemperatureTooLow() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("8310-5")
            .value(new BigDecimal("34.9"))
            .unit("°C")
            .observationDate(Instant.now().minusSeconds(60))
            .build();

        ObservationValidator.ValidationResult result = validator.validate(command, TENANT_ID);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("below minimum"));
    }

    @Test
    @DisplayName("Should reject temperature above 42°C")
    void shouldRejectTemperatureTooHigh() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("8310-5")
            .value(new BigDecimal("42.1"))
            .unit("°C")
            .observationDate(Instant.now().minusSeconds(60))
            .build();

        ObservationValidator.ValidationResult result = validator.validate(command, TENANT_ID);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("exceeds maximum"));
    }

    @Test
    @DisplayName("Should accept temperature at boundaries (35°C and 42°C)")
    void shouldAcceptTemperatureBoundaries() {
        RecordObservationCommand low = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("8310-5")
            .value(new BigDecimal("35.0"))
            .unit("°C")
            .observationDate(Instant.now().minusSeconds(60))
            .build();

        RecordObservationCommand high = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("8310-5")
            .value(new BigDecimal("42.0"))
            .unit("°C")
            .observationDate(Instant.now().minusSeconds(60))
            .build();

        assertThat(validator.validate(low, TENANT_ID).isValid()).isTrue();
        assertThat(validator.validate(high, TENANT_ID).isValid()).isTrue();
    }

    // ========== Heart Rate Tests (8867-4) ==========
    @Test
    @DisplayName("Should accept valid heart rate (72 bpm)")
    void shouldAcceptValidHeartRate() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("8867-4")
            .value(new BigDecimal("72"))
            .unit("/min")
            .observationDate(Instant.now().minusSeconds(60))
            .build();

        ObservationValidator.ValidationResult result = validator.validate(command, TENANT_ID);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Should reject heart rate below 40 bpm")
    void shouldRejectHeartRateTooLow() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("8867-4")
            .value(new BigDecimal("39"))
            .unit("/min")
            .observationDate(Instant.now().minusSeconds(60))
            .build();

        ObservationValidator.ValidationResult result = validator.validate(command, TENANT_ID);
        assertThat(result.isValid()).isFalse();
    }

    @Test
    @DisplayName("Should reject heart rate above 200 bpm")
    void shouldRejectHeartRateTooHigh() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("8867-4")
            .value(new BigDecimal("201"))
            .unit("/min")
            .observationDate(Instant.now().minusSeconds(60))
            .build();

        ObservationValidator.ValidationResult result = validator.validate(command, TENANT_ID);
        assertThat(result.isValid()).isFalse();
    }

    // ========== Glucose Tests (2339-0) ==========
    @Test
    @DisplayName("Should accept valid glucose (100 mg/dL)")
    void shouldAcceptValidGlucose() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("2339-0")
            .value(new BigDecimal("100"))
            .unit("mg/dL")
            .observationDate(Instant.now().minusSeconds(60))
            .build();

        ObservationValidator.ValidationResult result = validator.validate(command, TENANT_ID);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Should reject glucose below 40 mg/dL")
    void shouldRejectGlucoseTooLow() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("2339-0")
            .value(new BigDecimal("39"))
            .unit("mg/dL")
            .observationDate(Instant.now().minusSeconds(60))
            .build();

        ObservationValidator.ValidationResult result = validator.validate(command, TENANT_ID);
        assertThat(result.isValid()).isFalse();
    }

    @Test
    @DisplayName("Should reject glucose above 600 mg/dL")
    void shouldRejectGlucoseTooHigh() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("2339-0")
            .value(new BigDecimal("601"))
            .unit("mg/dL")
            .observationDate(Instant.now().minusSeconds(60))
            .build();

        ObservationValidator.ValidationResult result = validator.validate(command, TENANT_ID);
        assertThat(result.isValid()).isFalse();
    }

    // ========== Blood Pressure Tests ==========
    @Test
    @DisplayName("Should accept valid systolic BP (120 mmHg)")
    void shouldAcceptValidSystolicBP() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("8480-6")
            .value(new BigDecimal("120"))
            .unit("mmHg")
            .observationDate(Instant.now().minusSeconds(60))
            .build();

        assertThat(validator.validate(command, TENANT_ID).isValid()).isTrue();
    }

    @Test
    @DisplayName("Should accept valid diastolic BP (80 mmHg)")
    void shouldAcceptValidDiastolicBP() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("8462-4")
            .value(new BigDecimal("80"))
            .unit("mmHg")
            .observationDate(Instant.now().minusSeconds(60))
            .build();

        assertThat(validator.validate(command, TENANT_ID).isValid()).isTrue();
    }

    // ========== Other Vital Signs Tests ==========
    @Test
    @DisplayName("Should accept valid respiratory rate (16 /min)")
    void shouldAcceptValidRespiratoryRate() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("9279-1")
            .value(new BigDecimal("16"))
            .unit("/min")
            .observationDate(Instant.now().minusSeconds(60))
            .build();

        assertThat(validator.validate(command, TENANT_ID).isValid()).isTrue();
    }

    @Test
    @DisplayName("Should accept valid oxygen saturation (98%)")
    void shouldAcceptValidOxygenSaturation() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("2708-6")
            .value(new BigDecimal("98"))
            .unit("%")
            .observationDate(Instant.now().minusSeconds(60))
            .build();

        assertThat(validator.validate(command, TENANT_ID).isValid()).isTrue();
    }

    @Test
    @DisplayName("Should accept valid weight (75.5 kg)")
    void shouldAcceptValidWeight() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("29463-7")
            .value(new BigDecimal("75.5"))
            .unit("kg")
            .observationDate(Instant.now().minusSeconds(60))
            .build();

        assertThat(validator.validate(command, TENANT_ID).isValid()).isTrue();
    }

    // ========== Required Field Validation ==========
    @Test
    @DisplayName("Should reject missing patient ID")
    void shouldRejectMissingPatientId() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId("")
            .loincCode("8310-5")
            .value(new BigDecimal("37.5"))
            .unit("°C")
            .observationDate(Instant.now().minusSeconds(60))
            .build();

        ObservationValidator.ValidationResult result = validator.validate(command, TENANT_ID);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("Patient ID is required");
    }

    @Test
    @DisplayName("Should reject missing LOINC code")
    void shouldRejectMissingLoincCode() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("")
            .value(new BigDecimal("37.5"))
            .unit("°C")
            .observationDate(Instant.now().minusSeconds(60))
            .build();

        ObservationValidator.ValidationResult result = validator.validate(command, TENANT_ID);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("LOINC code is required");
    }

    @Test
    @DisplayName("Should reject missing value")
    void shouldRejectMissingValue() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("8310-5")
            .value(null)
            .unit("°C")
            .observationDate(Instant.now().minusSeconds(60))
            .build();

        ObservationValidator.ValidationResult result = validator.validate(command, TENANT_ID);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("Observation value is required");
    }

    @Test
    @DisplayName("Should reject future observation date")
    void shouldRejectFutureObservationDate() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("8310-5")
            .value(new BigDecimal("37.5"))
            .unit("°C")
            .observationDate(Instant.now().plusSeconds(60))
            .build();

        ObservationValidator.ValidationResult result = validator.validate(command, TENANT_ID);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("Observation date cannot be in the future");
    }

    @Test
    @DisplayName("Should allow unknown LOINC codes (lenient validation)")
    void shouldAllowUnknownLoincCode() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("9999-9")  // Unknown code
            .value(new BigDecimal("100"))
            .unit("unknown")
            .observationDate(Instant.now().minusSeconds(60))
            .build();

        ObservationValidator.ValidationResult result = validator.validate(command, TENANT_ID);
        // Should not have range validation errors for unknown codes
        assertThat(result.getErrors())
            .noneMatch(e -> e.contains("below minimum") || e.contains("exceeds maximum"));
    }

    @Test
    @DisplayName("Should enforce tenant isolation")
    void shouldEnforceTenantIsolation() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId("different-tenant")
            .patientId(PATIENT_ID)
            .loincCode("8310-5")
            .value(new BigDecimal("37.5"))
            .unit("°C")
            .observationDate(Instant.now().minusSeconds(60))
            .build();

        ObservationValidator.ValidationResult result = validator.validate(command, TENANT_ID);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("Tenant ID mismatch");
    }

    @Test
    @DisplayName("Should collect multiple validation errors")
    void shouldCollectMultipleErrors() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId("different-tenant")
            .patientId("")
            .loincCode("")
            .value(null)
            .unit("°C")
            .observationDate(Instant.now().plusSeconds(60))
            .build();

        ObservationValidator.ValidationResult result = validator.validate(command, TENANT_ID);
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors().size()).isGreaterThanOrEqualTo(5);
    }
}
