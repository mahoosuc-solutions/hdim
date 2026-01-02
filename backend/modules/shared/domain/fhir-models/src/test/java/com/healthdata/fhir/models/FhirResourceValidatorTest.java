package com.healthdata.fhir.models;

import static org.assertj.core.api.Assertions.assertThat;

import com.healthdata.common.validation.ValidationResult;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FhirResourceValidatorTest {

    private FhirResourceValidator validator;

    @BeforeEach
    void setUp() {
        validator = new FhirResourceValidator();
    }

    @Test
    @DisplayName("validate should return success result for valid patient")
    void validate_shouldReturnSuccessForValidPatient() {
        Patient patient = new Patient();
        patient.setId("Patient/123");
        patient.addName().setFamily("Smith").addGiven("John");
        patient.setBirthDateElement(new org.hl7.fhir.r4.model.DateType("1990-01-01"));

        ValidationResult result = validator.validate(patient);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getWarnings()).isNotNull();
    }

    @Test
    @DisplayName("validate should capture structural issues")
    void validate_shouldCaptureStructuralIssues() {
        Observation observation = new Observation();
        observation.setId("Observation/1");
        observation.setSubject(new Reference("Patient/123"));
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.setValue(new Quantity().setValue(120).setUnit("mmHg"));
        // Missing required code element

        ValidationResult result = validator.validate(observation);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).isNotEmpty();
        assertThat(result.getErrors().get(0)).contains("Observation.code");
    }

    @Test
    @DisplayName("validate should treat null resource as invalid and provide error")
    void validate_shouldHandleNullResource() {
        ValidationResult result = validator.validate(null);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).contains("Resource must not be null");
    }
}
