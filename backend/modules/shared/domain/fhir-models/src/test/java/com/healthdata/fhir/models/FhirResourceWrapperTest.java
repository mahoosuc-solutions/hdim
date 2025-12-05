package com.healthdata.fhir.models;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FhirResourceWrapperTest {

    @Test
    @DisplayName("wrap should populate audit metadata and default values")
    void wrap_shouldPopulateAuditMetadata() {
        Patient patient = new Patient();
        patient.setId("Patient/123");

        Instant beforeCall = Instant.now();
        FhirResourceWrapper<Patient> wrapper = FhirResourceWrapper.wrap(patient, "user-1", "org-1");
        Instant afterCall = Instant.now();

        assertThat(wrapper.getResource()).isSameAs(patient);
        assertThat(wrapper.getCreatedBy()).isEqualTo("user-1");
        assertThat(wrapper.getModifiedBy()).isEqualTo("user-1");
        assertThat(wrapper.getOrganizationId()).isEqualTo("org-1");
        assertThat(wrapper.isContainsPHI()).isTrue();
        assertThat(wrapper.getVersion()).isEqualTo(1L);

        assertThat(wrapper.getCreatedAt()).isBetween(beforeCall, afterCall);
        assertThat(wrapper.getLastModified()).isBetween(beforeCall, afterCall);
    }

    @Test
    @DisplayName("markModified should update audit fields and increment version")
    void markModified_shouldUpdateAuditFields() {
        Patient patient = new Patient();
        FhirResourceWrapper<Patient> wrapper = FhirResourceWrapper.wrap(patient, "creator", "org-1");
        Instant originalLastModified = wrapper.getLastModified();

        wrapper.markModified("editor-1");

        assertThat(wrapper.getModifiedBy()).isEqualTo("editor-1");
        assertThat(wrapper.getVersion()).isEqualTo(2L);
        assertThat(wrapper.getLastModified()).isAfterOrEqualTo(originalLastModified);
    }

    @Test
    @DisplayName("markModified should initialize version when null")
    void markModified_shouldInitializeVersionWhenNull() {
        Patient patient = new Patient();
        FhirResourceWrapper<Patient> wrapper = FhirResourceWrapper.wrap(patient, "creator", "org-1");
        wrapper.setVersion(null);

        wrapper.markModified("editor-2");

        assertThat(wrapper.getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getter helpers should return resource type and id")
    void getters_shouldExposeResourceMetadata() {
        Patient patient = new Patient();
        patient.setId("Patient/999");
        FhirResourceWrapper<Patient> wrapper = FhirResourceWrapper.wrap(patient, "creator", "org-1");

        assertThat(wrapper.getResourceType()).isEqualTo("Patient");
        assertThat(wrapper.getResourceId()).isEqualTo("Patient/999");
    }
}
