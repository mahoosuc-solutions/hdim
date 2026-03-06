package com.healthdata.eventsourcing.intelligence;

import com.healthdata.eventsourcing.command.patient.PatientCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CanonicalEventEnvelope Tests")
class CanonicalEventEnvelopeTest {

    @Test
    @DisplayName("fromDomainEvent should map required fields")
    void fromDomainEventShouldMapRequiredFields() {
        PatientCreatedEvent event = PatientCreatedEvent.builder()
                .tenantId("tenant-a")
                .patientId("patient-1")
                .firstName("Ada")
                .lastName("Lovelace")
                .dateOfBirth(LocalDate.of(1980, 1, 1))
                .mrn("MRN-1")
                .build();

        CanonicalEventEnvelope envelope = CanonicalEventEnvelope.fromDomainEvent(event, "FHIR_R4");

        assertThat(envelope.getTenantId()).isEqualTo("tenant-a");
        assertThat(envelope.getSourceType()).isEqualTo("FHIR_R4");
        assertThat(envelope.getResourceType()).isEqualTo("Patient");
        assertThat(envelope.getSchemaVersion()).isEqualTo("1.0");
        assertThat(envelope.getConfidence()).isEqualTo(1.0);
        assertThat(envelope.getRiskTier()).isEqualTo("MEDIUM");
    }
}
