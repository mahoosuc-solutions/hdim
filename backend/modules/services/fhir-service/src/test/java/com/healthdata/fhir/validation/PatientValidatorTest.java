package com.healthdata.fhir.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

class PatientValidatorTest {

    @Test
    void shouldRejectNullPatient() {
        PatientValidator validator = new PatientValidator();

        PatientValidator.ValidationResult result = validator.validate(null);

        assertThat(result.isValid()).isFalse();
        assertThat(result.message()).isEqualTo("Patient resource must be provided");
    }

    @Test
    void shouldReportMissingNameAndGender() {
        PatientValidator validator = new PatientValidator();
        Patient patient = new Patient();

        PatientValidator.ValidationResult result = validator.validate(patient);

        assertThat(result.isValid()).isFalse();
        assertThat(result.message()).contains("family name");
        assertThat(result.message()).contains("given name");
        assertThat(result.message()).contains("gender");
    }

    @Test
    void shouldAcceptValidPatient() {
        PatientValidator validator = new PatientValidator();
        Patient patient = new Patient();
        patient.addName(new HumanName().setFamily("Doe").addGiven("Jane"));
        patient.setGender(org.hl7.fhir.r4.model.Enumerations.AdministrativeGender.FEMALE);

        PatientValidator.ValidationResult result = validator.validate(patient);

        assertThat(result.isValid()).isTrue();
        assertThat(result.message()).isNull();
    }
}
