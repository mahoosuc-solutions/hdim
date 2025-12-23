package com.healthdata.fhir.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PatientServiceExceptionTest {

    @Test
    void shouldExposePatientExceptions() {
        PatientService.PatientValidationException validation = new PatientService.PatientValidationException("invalid");
        PatientService.PatientNotFoundException notFound = new PatientService.PatientNotFoundException("patient-1");

        assertThat(validation.getMessage()).isEqualTo("invalid");
        assertThat(notFound.getMessage()).isEqualTo("Patient not found: patient-1");
    }
}
