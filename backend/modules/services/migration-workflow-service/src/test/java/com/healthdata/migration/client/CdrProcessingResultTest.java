package com.healthdata.migration.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.healthdata.cdr.dto.CdaProcessResponse;
import com.healthdata.cdr.dto.ProcessMessageResponse;

@DisplayName("CdrProcessingResult")
class CdrProcessingResultTest {

    @Test
    @DisplayName("Should build counts from HL7 response bundle")
    void shouldBuildCountsFromHl7Response() {
        Patient patient = new Patient();
        patient.setId("patient-1");
        Observation observation = new Observation();
        observation.setId("obs-1");
        Patient secondPatient = new Patient();
        secondPatient.setId("patient-2");

        Bundle.BundleEntryComponent patientEntry = new Bundle.BundleEntryComponent();
        patientEntry.setResource(patient);
        Bundle.BundleEntryComponent observationEntry = new Bundle.BundleEntryComponent();
        observationEntry.setResource(observation);
        Bundle.BundleEntryComponent secondPatientEntry = new Bundle.BundleEntryComponent();
        secondPatientEntry.setResource(secondPatient);

        Bundle bundle = new Bundle();
        bundle.setEntry(java.util.List.of(patientEntry, observationEntry, secondPatientEntry));

        ProcessMessageResponse response = new ProcessMessageResponse();
        response.setSuccess(true);
        response.setFhirBundle(bundle);
        response.setProcessingTimeMs(120);

        CdrProcessingResult result = CdrProcessingResult.fromHl7Response(response);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResourceCounts()).hasSize(2);
        long totalCount = result.getResourceCounts().values().stream().mapToLong(Long::longValue).sum();
        assertThat(totalCount).isEqualTo(3);
        assertThat(result.getProcessingTimeMs()).isEqualTo(120);
    }

    @Test
    @DisplayName("Should handle null HL7 response")
    void shouldHandleNullHl7Response() {
        CdrProcessingResult result = CdrProcessingResult.fromHl7Response(null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Null response");
    }

    @Test
    @DisplayName("Should handle failed HL7 response")
    void shouldHandleFailedHl7Response() {
        ProcessMessageResponse response = new ProcessMessageResponse();
        response.setSuccess(false);
        response.setErrorMessage("failed");

        CdrProcessingResult result = CdrProcessingResult.fromHl7Response(response);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("failed");
    }

    @Test
    @DisplayName("Should build counts from CDA response bundle")
    void shouldBuildCountsFromCdaResponse() {
        Patient patient = new Patient();
        patient.setId("patient-1");
        Bundle.BundleEntryComponent patientEntry = new Bundle.BundleEntryComponent();
        patientEntry.setResource(patient);

        Bundle bundle = new Bundle();
        bundle.setEntry(java.util.List.of(patientEntry));

        CdaProcessResponse response = new CdaProcessResponse();
        response.setSuccess(true);
        response.setFhirBundle(bundle);
        response.setProcessingTimeMs(45);

        CdrProcessingResult result = CdrProcessingResult.fromCdaResponse(response);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResourceCounts()).hasSize(1);
        long totalCount = result.getResourceCounts().values().stream().mapToLong(Long::longValue).sum();
        assertThat(totalCount).isEqualTo(1);
        assertThat(result.getProcessingTimeMs()).isEqualTo(45);
    }

    @Test
    @DisplayName("Should handle null CDA response")
    void shouldHandleNullCdaResponse() {
        CdrProcessingResult result = CdrProcessingResult.fromCdaResponse(null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Null response");
    }

    @Test
    @DisplayName("Should build success and failure results")
    void shouldBuildSimpleResults() {
        CdrProcessingResult success = CdrProcessingResult.success("Bundle", 1);
        CdrProcessingResult failure = CdrProcessingResult.failure("error");

        assertThat(success.isSuccess()).isTrue();
        assertThat(success.getResourceCounts().get("Bundle")).isEqualTo(1);

        assertThat(failure.isSuccess()).isFalse();
        assertThat(failure.getErrorMessage()).isEqualTo("error");
    }
}
