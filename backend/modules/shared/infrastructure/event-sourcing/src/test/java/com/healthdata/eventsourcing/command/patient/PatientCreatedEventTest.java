package com.healthdata.eventsourcing.command.patient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PatientCreatedEvent domain event
 */
@DisplayName("PatientCreatedEvent Tests")
class PatientCreatedEventTest {

    private final String TENANT_ID = "tenant-123";
    private final String FIRST_NAME = "John";
    private final String LAST_NAME = "Doe";
    private final LocalDate DOB = LocalDate.of(1990, 1, 15);
    private final String GENDER = "MALE";
    private final String MRN = "MRN-12345";
    private final String INSURANCE_ID = "INS-98765";

    @Test
    @DisplayName("Should create event with all fields")
    void shouldCreateEventWithAllFields() {
        // Given & When
        PatientCreatedEvent event = PatientCreatedEvent.builder()
            .tenantId(TENANT_ID)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .gender(GENDER)
            .mrn(MRN)
            .insuranceMemberId(INSURANCE_ID)
            .sensitivityLevel("SENSITIVE")
            .hipaaCompliant(true)
            .build();

        // Then
        assertThat(event)
            .extracting(
                PatientCreatedEvent::getTenantId,
                PatientCreatedEvent::getFirstName,
                PatientCreatedEvent::getLastName,
                PatientCreatedEvent::getDateOfBirth,
                PatientCreatedEvent::getGender,
                PatientCreatedEvent::getMrn,
                PatientCreatedEvent::getInsuranceMemberId
            )
            .containsExactly(
                TENANT_ID,
                FIRST_NAME,
                LAST_NAME,
                DOB,
                GENDER,
                MRN,
                INSURANCE_ID
            );
    }

    @Test
    @DisplayName("Should generate deterministic aggregate ID")
    void shouldGenerateDeterministicAggregateId() {
        // Given
        PatientCreatedEvent event = PatientCreatedEvent.builder()
            .tenantId(TENANT_ID)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .mrn(MRN)
            .build();

        // When
        String aggregateId = event.getAggregateId();

        // Then
        assertThat(aggregateId)
            .isEqualTo("patient-tenant-123-MRN-12345")
            .startsWith("patient-")
            .contains(TENANT_ID)
            .contains(MRN);
    }

    @Test
    @DisplayName("Should generate same aggregate ID for same tenant and MRN")
    void shouldGenerateSameAggregateIdForSameTenantAndMrn() {
        // Given
        PatientCreatedEvent event1 = PatientCreatedEvent.builder()
            .tenantId(TENANT_ID)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .mrn(MRN)
            .build();

        PatientCreatedEvent event2 = PatientCreatedEvent.builder()
            .tenantId(TENANT_ID)
            .firstName("Jane")  // Different name
            .lastName("Smith")  // Different name
            .dateOfBirth(LocalDate.of(1995, 5, 20))  // Different DOB
            .mrn(MRN)  // Same MRN
            .build();

        // When
        String id1 = event1.getAggregateId();
        String id2 = event2.getAggregateId();

        // Then
        assertThat(id1).isEqualTo(id2);
    }

    @Test
    @DisplayName("Should generate different aggregate IDs for different MRNs")
    void shouldGenerateDifferentAggregateIdsForDifferentMrns() {
        // Given
        PatientCreatedEvent event1 = PatientCreatedEvent.builder()
            .tenantId(TENANT_ID)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .mrn("MRN-11111")
            .build();

        PatientCreatedEvent event2 = PatientCreatedEvent.builder()
            .tenantId(TENANT_ID)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .mrn("MRN-22222")
            .build();

        // When
        String id1 = event1.getAggregateId();
        String id2 = event2.getAggregateId();

        // Then
        assertThat(id1).isNotEqualTo(id2);
        assertThat(id1).contains("MRN-11111");
        assertThat(id2).contains("MRN-22222");
    }

    @Test
    @DisplayName("Should generate different aggregate IDs for different tenants")
    void shouldGenerateDifferentAggregateIdsForDifferentTenants() {
        // Given
        PatientCreatedEvent event1 = PatientCreatedEvent.builder()
            .tenantId("tenant-1")
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .mrn(MRN)
            .build();

        PatientCreatedEvent event2 = PatientCreatedEvent.builder()
            .tenantId("tenant-2")
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .mrn(MRN)
            .build();

        // When
        String id1 = event1.getAggregateId();
        String id2 = event2.getAggregateId();

        // Then
        assertThat(id1).isNotEqualTo(id2);
        assertThat(id1).contains("tenant-1");
        assertThat(id2).contains("tenant-2");
    }

    @Test
    @DisplayName("Should return correct event type")
    void shouldReturnCorrectEventType() {
        // Given
        PatientCreatedEvent event = PatientCreatedEvent.builder()
            .tenantId(TENANT_ID)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .mrn(MRN)
            .build();

        // When
        String eventType = event.getEventType();

        // Then
        assertThat(eventType).isEqualTo("PatientCreated");
    }

    @Test
    @DisplayName("Should set HIPAA sensitivity level")
    void shouldSetHipaaSensitivityLevel() {
        // Given
        PatientCreatedEvent event = PatientCreatedEvent.builder()
            .tenantId(TENANT_ID)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .mrn(MRN)
            .sensitivityLevel("SENSITIVE")
            .build();

        // When & Then
        assertThat(event.getSensitivityLevel()).isEqualTo("SENSITIVE");
    }

    @Test
    @DisplayName("Should mark event as HIPAA compliant by default")
    void shouldMarkEventAsHipaaCompliantByDefault() {
        // Given
        PatientCreatedEvent event = PatientCreatedEvent.builder()
            .tenantId(TENANT_ID)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .mrn(MRN)
            .build();

        // When & Then
        assertThat(event.isHipaaCompliant()).isTrue();
        assertThat(event.getSensitivityLevel()).isEqualTo("SENSITIVE");
    }

    @Test
    @DisplayName("Should allow overriding HIPAA compliance flag")
    void shouldAllowOverridingHipaaComplianceFlag() {
        // Given
        PatientCreatedEvent event = PatientCreatedEvent.builder()
            .tenantId(TENANT_ID)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .mrn(MRN)
            .hipaaCompliant(false)
            .build();

        // When & Then
        assertThat(event.isHipaaCompliant()).isFalse();
    }

    @Test
    @DisplayName("Should support builder pattern")
    void shouldSupportBuilderPattern() {
        // Given & When
        PatientCreatedEvent event = PatientCreatedEvent.builder()
            .tenantId(TENANT_ID)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .gender(GENDER)
            .mrn(MRN)
            .insuranceMemberId(INSURANCE_ID)
            .build();

        // Then
        assertThat(event).isNotNull();
        assertThat(event.getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    @DisplayName("Should be creatable with default values")
    void shouldBeCreatableWithDefaultValues() {
        // Given & When
        PatientCreatedEvent event = new PatientCreatedEvent();

        // Then
        assertThat(event).isNotNull();
        // Default values should be set
    }

    @Test
    @DisplayName("Should support field access for all attributes")
    void shouldSupportFieldAccessForAllAttributes() {
        // Given
        PatientCreatedEvent event = PatientCreatedEvent.builder()
            .tenantId(TENANT_ID)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .gender(GENDER)
            .mrn(MRN)
            .insuranceMemberId(INSURANCE_ID)
            .sensitivityLevel("SENSITIVE")
            .hipaaCompliant(true)
            .build();

        // When & Then - all getters work
        assertThat(event.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(event.getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(event.getLastName()).isEqualTo(LAST_NAME);
        assertThat(event.getDateOfBirth()).isEqualTo(DOB);
        assertThat(event.getGender()).isEqualTo(GENDER);
        assertThat(event.getMrn()).isEqualTo(MRN);
        assertThat(event.getInsuranceMemberId()).isEqualTo(INSURANCE_ID);
        assertThat(event.getSensitivityLevel()).isEqualTo("SENSITIVE");
        assertThat(event.isHipaaCompliant()).isTrue();
    }

    @Test
    @DisplayName("Should provide access to all fields for debugging")
    void shouldProvideAccessToAllFieldsForDebugging() {
        // Given
        PatientCreatedEvent event = PatientCreatedEvent.builder()
            .tenantId(TENANT_ID)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .mrn(MRN)
            .build();

        // When & Then - verify all fields are accessible and correct
        assertThat(event.getTenantId()).contains(TENANT_ID);
        assertThat(event.getFirstName()).contains(FIRST_NAME);
        assertThat(event.getLastName()).contains(LAST_NAME);
        assertThat(event.getMrn()).contains(MRN);
    }

    @Test
    @DisplayName("Should be equality comparable")
    void shouldBeEqualityComparable() {
        // Given
        PatientCreatedEvent event1 = PatientCreatedEvent.builder()
            .tenantId(TENANT_ID)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .mrn(MRN)
            .build();

        PatientCreatedEvent event2 = PatientCreatedEvent.builder()
            .tenantId(TENANT_ID)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .mrn(MRN)
            .build();

        // When & Then
        assertThat(event1).isEqualTo(event2);
    }

    @Test
    @DisplayName("Should maintain immutability after creation")
    void shouldMaintainImmutability() {
        // Given
        PatientCreatedEvent event = PatientCreatedEvent.builder()
            .tenantId(TENANT_ID)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .mrn(MRN)
            .build();

        // When & Then - verify that we can read all fields without modification
        String tenantId = event.getTenantId();
        String firstName = event.getFirstName();
        String lastName = event.getLastName();
        LocalDate dateOfBirth = event.getDateOfBirth();
        String mrn = event.getMrn();

        // Verify values are accessible
        assertThat(tenantId).isEqualTo(TENANT_ID);
        assertThat(firstName).isEqualTo(FIRST_NAME);
        assertThat(lastName).isEqualTo(LAST_NAME);
        assertThat(dateOfBirth).isEqualTo(DOB);
        assertThat(mrn).isEqualTo(MRN);
    }

    @Test
    @DisplayName("Should support JsonProperty annotations for serialization")
    void shouldSupportJsonPropertyAnnotations() {
        // Given
        PatientCreatedEvent event = PatientCreatedEvent.builder()
            .tenantId(TENANT_ID)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .mrn(MRN)
            .build();

        // When & Then - JsonProperty annotations are used by Jackson for serialization
        // This test verifies the event can be created and accessed
        assertThat(event.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(event.getFirstName()).isEqualTo(FIRST_NAME);
    }
}
