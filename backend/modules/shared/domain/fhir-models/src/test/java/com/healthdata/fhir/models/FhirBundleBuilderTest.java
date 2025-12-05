package com.healthdata.fhir.models;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FhirBundleBuilderTest {

    @Test
    @DisplayName("transactionBundle should build bundle with entries and metadata")
    void transactionBundle_shouldBuildBundleWithEntries() {
        Patient patient = new Patient();
        patient.addName().setFamily("Doe").addGiven("Jane");

        Observation observation = new Observation();
        observation.setSubject(new Reference("Patient/temp"));

        Bundle bundle = FhirBundleBuilder.transactionBundle()
                .addEntry(patient, Bundle.HTTPVerb.POST, "Patient")
                .addEntry(observation, Bundle.HTTPVerb.POST, "Observation")
                .build();

        assertThat(bundle.getType()).isEqualTo(Bundle.BundleType.TRANSACTION);
        assertThat(bundle.getEntry()).hasSize(2);
        assertThat(bundle.getMeta().getLastUpdated()).isNotNull();
        assertThat(bundle.getTimestamp()).isNotNull();
        assertThat(bundle.getId()).isNotBlank();

        Bundle.BundleEntryComponent patientEntry = bundle.getEntry().get(0);
        assertThat(patientEntry.getResource()).isSameAs(patient);
        assertThat(patientEntry.getRequest().getMethod()).isEqualTo(Bundle.HTTPVerb.POST);
        assertThat(patientEntry.getRequest().getUrl()).isEqualTo("Patient");

        Bundle.BundleEntryComponent observationEntry = bundle.getEntry().get(1);
        assertThat(observationEntry.getResource()).isSameAs(observation);
        assertThat(observationEntry.getRequest().getUrl()).isEqualTo("Observation");
    }

    @Test
    @DisplayName("builder should reject missing method or url")
    void builder_shouldRejectMissingMethodOrUrl() {
        Patient patient = new Patient();

        assertThatThrownBy(() -> FhirBundleBuilder.transactionBundle()
                .addEntry(patient, null, "Patient"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("method");

        assertThatThrownBy(() -> FhirBundleBuilder.transactionBundle()
                .addEntry(patient, Bundle.HTTPVerb.POST, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("url");
    }

    @Test
    @DisplayName("withTimestamp should override default timestamp")
    void withTimestamp_shouldOverrideDefaultTimestamp() {
        Instant fixed = Instant.parse("2024-01-01T00:00:00Z");

        Bundle bundle = FhirBundleBuilder.transactionBundle()
                .withTimestamp(fixed)
                .addEntry(new Patient(), Bundle.HTTPVerb.POST, "Patient")
                .build();

        assertThat(bundle.getTimestamp()).isEqualTo(fixed);
        assertThat(bundle.getMeta().getLastUpdated()).isEqualTo(fixed);
    }
}
