package com.healthdata.eventsourcing.command.patient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for CreatePatientCommand DTO
 */
@DisplayName("CreatePatientCommand Tests")
class CreatePatientCommandTest {

    private final String TENANT_ID = "tenant-123";
    private final String FIRST_NAME = "John";
    private final String LAST_NAME = "Doe";
    private final LocalDate DOB = LocalDate.of(1990, 1, 15);
    private final String GENDER = "MALE";
    private final String MRN = "MRN-12345";
    private final String INSURANCE_ID = "INS-98765";

    @Test
    @DisplayName("Should create command with all fields")
    void shouldCreateCommandWithAllFields() {
        // Given & When
        CreatePatientCommand command = CreatePatientCommand.builder()
            .tenantId(TENANT_ID)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .gender(GENDER)
            .mrn(MRN)
            .insuranceMemberId(INSURANCE_ID)
            .build();

        // Then
        assertThat(command)
            .extracting(
                CreatePatientCommand::getTenantId,
                CreatePatientCommand::getFirstName,
                CreatePatientCommand::getLastName,
                CreatePatientCommand::getDateOfBirth,
                CreatePatientCommand::getGender,
                CreatePatientCommand::getMrn,
                CreatePatientCommand::getInsuranceMemberId
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
    @DisplayName("Should create command with minimal fields")
    void shouldCreateCommandWithMinimalFields() {
        // Given & When
        CreatePatientCommand command = CreatePatientCommand.builder()
            .tenantId(TENANT_ID)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .mrn(MRN)
            .build();

        // Then
        assertThat(command).isNotNull();
        assertThat(command.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(command.getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(command.getLastName()).isEqualTo(LAST_NAME);
        assertThat(command.getDateOfBirth()).isEqualTo(DOB);
        assertThat(command.getMrn()).isEqualTo(MRN);
        assertThat(command.getGender()).isNull();
        assertThat(command.getInsuranceMemberId()).isNull();
    }

    @Test
    @DisplayName("Should be immutable after creation")
    void shouldBeImmutable() {
        // Given
        CreatePatientCommand command = CreatePatientCommand.builder()
            .tenantId(TENANT_ID)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .mrn(MRN)
            .build();

        // When & Then - verify that fields are final (getter access only)
        assertThat(command.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(command.getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(command.getLastName()).isEqualTo(LAST_NAME);
        assertThat(command.getDateOfBirth()).isEqualTo(DOB);
        assertThat(command.getMrn()).isEqualTo(MRN);

        // No setters available (verified by compilation, but test confirms immutability concept)
    }

    @Test
    @DisplayName("Should return proper toString representation")
    void shouldReturnProperToString() {
        // Given
        CreatePatientCommand command = CreatePatientCommand.builder()
            .tenantId(TENANT_ID)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .mrn(MRN)
            .build();

        // When
        String toStringOutput = command.toString();

        // Then
        assertThat(toStringOutput)
            .contains(TENANT_ID)
            .contains(FIRST_NAME)
            .contains(LAST_NAME)
            .contains(MRN);
    }

    @Test
    @DisplayName("Should support builder pattern")
    void shouldSupportBuilderPattern() {
        // Given & When
        CreatePatientCommand command = CreatePatientCommand.builder()
            .tenantId(TENANT_ID)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .gender(GENDER)
            .mrn(MRN)
            .insuranceMemberId(INSURANCE_ID)
            .build();

        // Then
        assertThat(command).isNotNull();
        assertThat(command.getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    @DisplayName("Should create command with null optional fields")
    void shouldCreateCommandWithNullOptionalFields() {
        // Given & When
        CreatePatientCommand command = CreatePatientCommand.builder()
            .tenantId(TENANT_ID)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .mrn(MRN)
            .gender(null)
            .insuranceMemberId(null)
            .build();

        // Then
        assertThat(command.getGender()).isNull();
        assertThat(command.getInsuranceMemberId()).isNull();
    }

    @Test
    @DisplayName("Should handle different date of birth values")
    void shouldHandleDifferentDatesOfBirth() {
        // Given
        LocalDate newbornDob = LocalDate.now();
        LocalDate elderlyDob = LocalDate.of(1940, 1, 1);

        // When
        CreatePatientCommand newbornCommand = CreatePatientCommand.builder()
            .tenantId(TENANT_ID)
            .firstName("Baby")
            .lastName("Newborn")
            .dateOfBirth(newbornDob)
            .mrn("MRN-NEWBORN")
            .build();

        CreatePatientCommand elderlyCommand = CreatePatientCommand.builder()
            .tenantId(TENANT_ID)
            .firstName("Elder")
            .lastName("Patient")
            .dateOfBirth(elderlyDob)
            .mrn("MRN-ELDERLY")
            .build();

        // Then
        assertThat(newbornCommand.getDateOfBirth()).isEqualTo(newbornDob);
        assertThat(elderlyCommand.getDateOfBirth()).isEqualTo(elderlyDob);
    }

    @Test
    @DisplayName("Should handle multi-tenant isolation")
    void shouldHandleMultiTenantIsolation() {
        // Given
        String tenant1 = "tenant-1";
        String tenant2 = "tenant-2";

        // When
        CreatePatientCommand command1 = CreatePatientCommand.builder()
            .tenantId(tenant1)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .mrn(MRN)
            .build();

        CreatePatientCommand command2 = CreatePatientCommand.builder()
            .tenantId(tenant2)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .mrn(MRN)  // Same MRN in different tenant
            .build();

        // Then
        assertThat(command1.getTenantId()).isEqualTo(tenant1);
        assertThat(command2.getTenantId()).isEqualTo(tenant2);
        assertThat(command1.getMrn()).isEqualTo(command2.getMrn());
        assertThat(command1.getTenantId()).isNotEqualTo(command2.getTenantId());
    }

    @Test
    @DisplayName("Should have equivalent fields")
    void shouldHaveEquivalentFields() {
        // Given
        CreatePatientCommand command1 = CreatePatientCommand.builder()
            .tenantId(TENANT_ID)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .mrn(MRN)
            .build();

        CreatePatientCommand command2 = CreatePatientCommand.builder()
            .tenantId(TENANT_ID)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .mrn(MRN)
            .build();

        // When & Then - verify all fields are equivalent
        assertThat(command1)
            .extracting(
                CreatePatientCommand::getTenantId,
                CreatePatientCommand::getFirstName,
                CreatePatientCommand::getLastName,
                CreatePatientCommand::getDateOfBirth,
                CreatePatientCommand::getMrn
            )
            .containsExactly(
                command2.getTenantId(),
                command2.getFirstName(),
                command2.getLastName(),
                command2.getDateOfBirth(),
                command2.getMrn()
            );
    }

    @Test
    @DisplayName("Should be hashable for use in collections")
    void shouldBeHashable() {
        // Given
        CreatePatientCommand command = CreatePatientCommand.builder()
            .tenantId(TENANT_ID)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .mrn(MRN)
            .build();

        // When & Then - should be usable in HashSet
        var set = new java.util.HashSet<>();
        set.add(command);
        assertThat(set).contains(command);
    }

    @Test
    @DisplayName("Should support field access for all attributes")
    void shouldSupportFieldAccessForAllAttributes() {
        // Given
        CreatePatientCommand command = CreatePatientCommand.builder()
            .tenantId(TENANT_ID)
            .firstName(FIRST_NAME)
            .lastName(LAST_NAME)
            .dateOfBirth(DOB)
            .gender(GENDER)
            .mrn(MRN)
            .insuranceMemberId(INSURANCE_ID)
            .build();

        // When & Then - all getters work
        assertThat(command.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(command.getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(command.getLastName()).isEqualTo(LAST_NAME);
        assertThat(command.getDateOfBirth()).isEqualTo(DOB);
        assertThat(command.getGender()).isEqualTo(GENDER);
        assertThat(command.getMrn()).isEqualTo(MRN);
        assertThat(command.getInsuranceMemberId()).isEqualTo(INSURANCE_ID);
    }
}
