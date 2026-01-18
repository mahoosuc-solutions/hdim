package com.healthdata.eventsourcing.command.patient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for CreatePatientValidator
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreatePatientValidator Tests")
class CreatePatientValidatorTest {

    private CreatePatientValidator validator;
    private final String TENANT_ID = "tenant-123";

    @BeforeEach
    void setUp() {
        validator = new CreatePatientValidator();
    }

    @Nested
    @DisplayName("Tenant Validation")
    class TenantValidationTests {

        @Test
        @DisplayName("Should fail when tenant ID is null")
        void shouldFailWhenTenantIdNull() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(null)
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .mrn("MRN-123")
                .build();

            // When
            CreatePatientValidator.ValidationResult result = validator.validate(command, TENANT_ID);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).contains("Tenant ID is required");
        }

        @Test
        @DisplayName("Should fail when tenant ID mismatch")
        void shouldFailWhenTenantMismatch() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId("tenant-456")
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .mrn("MRN-123")
                .build();

            // When
            CreatePatientValidator.ValidationResult result = validator.validate(command, TENANT_ID);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).contains("Tenant ID mismatch: command tenant does not match context tenant");
        }

        @Test
        @DisplayName("Should pass when tenant ID matches")
        void shouldPassWhenTenantMatches() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .mrn("MRN-123")
                .build();

            // When
            CreatePatientValidator.ValidationResult result = validator.validate(command, TENANT_ID);

            // Then
            assertThat(result.getErrors()).doesNotContain("Tenant ID mismatch: command tenant does not match context tenant");
        }
    }

    @Nested
    @DisplayName("Name Validation")
    class NameValidationTests {

        @Test
        @DisplayName("Should fail when first name is null")
        void shouldFailWhenFirstNameNull() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName(null)
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .mrn("MRN-123")
                .build();

            // When
            CreatePatientValidator.ValidationResult result = validator.validate(command, TENANT_ID);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).contains("First name is required");
        }

        @Test
        @DisplayName("Should fail when first name is empty")
        void shouldFailWhenFirstNameEmpty() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName("")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .mrn("MRN-123")
                .build();

            // When
            CreatePatientValidator.ValidationResult result = validator.validate(command, TENANT_ID);

            // Then
            assertThat(result.getErrors()).contains("First name is required");
        }

        @Test
        @DisplayName("Should fail when first name exceeds 100 characters")
        void shouldFailWhenFirstNameTooLong() {
            // Given
            String longName = "a".repeat(101);
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName(longName)
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .mrn("MRN-123")
                .build();

            // When
            CreatePatientValidator.ValidationResult result = validator.validate(command, TENANT_ID);

            // Then
            assertThat(result.getErrors()).contains("First name cannot exceed 100 characters");
        }

        @Test
        @DisplayName("Should fail when first name contains invalid characters")
        void shouldFailWhenFirstNameInvalidChars() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName("John123")  // Numbers not allowed
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .mrn("MRN-123")
                .build();

            // When
            CreatePatientValidator.ValidationResult result = validator.validate(command, TENANT_ID);

            // Then
            assertThat(result.getErrors()).contains("First name can only contain letters and hyphens");
        }

        @Test
        @DisplayName("Should accept first name with hyphens")
        void shouldAcceptFirstNameWithHyphens() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName("Jean-Pierre")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .mrn("MRN-123")
                .build();

            // When
            CreatePatientValidator.ValidationResult result = validator.validate(command, TENANT_ID);

            // Then
            assertThat(result.getErrors()).doesNotContain("First name can only contain letters and hyphens");
        }

        @Test
        @DisplayName("Should fail when last name is null")
        void shouldFailWhenLastNameNull() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName("John")
                .lastName(null)
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .mrn("MRN-123")
                .build();

            // When
            CreatePatientValidator.ValidationResult result = validator.validate(command, TENANT_ID);

            // Then
            assertThat(result.getErrors()).contains("Last name is required");
        }
    }

    @Nested
    @DisplayName("Date of Birth Validation")
    class DateOfBirthValidationTests {

        @Test
        @DisplayName("Should fail when date of birth is null")
        void shouldFailWhenDobNull() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(null)
                .mrn("MRN-123")
                .build();

            // When
            CreatePatientValidator.ValidationResult result = validator.validate(command, TENANT_ID);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).contains("Date of birth is required");
        }

        @Test
        @DisplayName("Should fail when date of birth is in future")
        void shouldFailWhenDobInFuture() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().plusYears(1))
                .mrn("MRN-123")
                .build();

            // When
            CreatePatientValidator.ValidationResult result = validator.validate(command, TENANT_ID);

            // Then
            assertThat(result.getErrors()).contains("Date of birth cannot be in the future");
        }

        @Test
        @DisplayName("Should fail when patient age exceeds 150 years")
        void shouldFailWhenAgeTooOld() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now().minusYears(151))
                .mrn("MRN-123")
                .build();

            // When
            CreatePatientValidator.ValidationResult result = validator.validate(command, TENANT_ID);

            // Then
            assertThat(result.getErrors()).contains("Patient age cannot exceed 150 years");
        }

        @Test
        @DisplayName("Should accept valid date of birth")
        void shouldAcceptValidDob() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .mrn("MRN-123")
                .build();

            // When
            CreatePatientValidator.ValidationResult result = validator.validate(command, TENANT_ID);

            // Then
            assertThat(result.getErrors()).doesNotContain("Date of birth is required");
            assertThat(result.getErrors()).doesNotContain("Date of birth cannot be in the future");
            assertThat(result.getErrors()).doesNotContain("Patient age cannot exceed 150 years");
        }

        @Test
        @DisplayName("Should accept newborn (age 0)")
        void shouldAcceptNewborn() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName("Baby")
                .lastName("Doe")
                .dateOfBirth(LocalDate.now())
                .mrn("MRN-123")
                .build();

            // When
            CreatePatientValidator.ValidationResult result = validator.validate(command, TENANT_ID);

            // Then - Should not have age-related errors
            assertThat(result.getErrors()).doesNotContain("Date of birth cannot be in the future");
            assertThat(result.getErrors()).doesNotContain("Patient age cannot exceed 150 years");
        }
    }

    @Nested
    @DisplayName("Gender Validation")
    class GenderValidationTests {

        @Test
        @DisplayName("Should accept MALE gender")
        void shouldAcceptMaleGender() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .gender("MALE")
                .mrn("MRN-123")
                .build();

            // When
            CreatePatientValidator.ValidationResult result = validator.validate(command, TENANT_ID);

            // Then
            assertThat(result.getErrors()).doesNotContain("Invalid gender code: MALE");
        }

        @Test
        @DisplayName("Should accept FEMALE gender")
        void shouldAcceptFemaleGender() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName("Jane")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .gender("FEMALE")
                .mrn("MRN-123")
                .build();

            // When
            CreatePatientValidator.ValidationResult result = validator.validate(command, TENANT_ID);

            // Then
            assertThat(result.getErrors()).doesNotContain("Invalid gender code: FEMALE");
        }

        @Test
        @DisplayName("Should reject invalid gender code")
        void shouldRejectInvalidGender() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .gender("INVALID")
                .mrn("MRN-123")
                .build();

            // When
            CreatePatientValidator.ValidationResult result = validator.validate(command, TENANT_ID);

            // Then
            assertThat(result.getErrors()).contains("Invalid gender code: INVALID");
        }

        @Test
        @DisplayName("Should allow null gender")
        void shouldAllowNullGender() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .gender(null)
                .mrn("MRN-123")
                .build();

            // When
            CreatePatientValidator.ValidationResult result = validator.validate(command, TENANT_ID);

            // Then - Should not fail on gender validation (optional field)
            assertThat(result.getErrors())
                .noneMatch(err -> err.contains("Invalid gender code"));
        }
    }

    @Nested
    @DisplayName("MRN Validation")
    class MrnValidationTests {

        @Test
        @DisplayName("Should fail when MRN is null")
        void shouldFailWhenMrnNull() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .mrn(null)
                .build();

            // When
            CreatePatientValidator.ValidationResult result = validator.validate(command, TENANT_ID);

            // Then
            assertThat(result.getErrors()).contains("MRN (Medical Record Number) is required");
        }

        @Test
        @DisplayName("Should fail when MRN exceeds 50 characters")
        void shouldFailWhenMrnTooLong() {
            // Given
            String longMrn = "M".repeat(51);
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .mrn(longMrn)
                .build();

            // When
            CreatePatientValidator.ValidationResult result = validator.validate(command, TENANT_ID);

            // Then
            assertThat(result.getErrors()).contains("MRN cannot exceed 50 characters");
        }

        @Test
        @DisplayName("Should fail when MRN contains invalid characters")
        void shouldFailWhenMrnInvalidChars() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .mrn("MRN@123#")  // @ and # not allowed
                .build();

            // When
            CreatePatientValidator.ValidationResult result = validator.validate(command, TENANT_ID);

            // Then
            assertThat(result.getErrors()).contains("MRN can only contain alphanumeric characters and hyphens");
        }

        @Test
        @DisplayName("Should accept valid MRN")
        void shouldAcceptValidMrn() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .mrn("MRN-12345-ABC")
                .build();

            // When
            CreatePatientValidator.ValidationResult result = validator.validate(command, TENANT_ID);

            // Then
            assertThat(result.getErrors())
                .doesNotContain("MRN (Medical Record Number) is required")
                .doesNotContain("MRN cannot exceed 50 characters")
                .doesNotContain("MRN can only contain alphanumeric characters and hyphens");
        }
    }

    @Nested
    @DisplayName("Multiple Validation Errors")
    class MultipleErrorsTests {

        @Test
        @DisplayName("Should collect all validation errors")
        void shouldCollectAllErrors() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId("different-tenant")
                .firstName("")
                .lastName("")
                .dateOfBirth(LocalDate.now().plusYears(1))
                .gender("INVALID")
                .mrn("")
                .build();

            // When
            CreatePatientValidator.ValidationResult result = validator.validate(command, TENANT_ID);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors())
                .contains("Tenant ID mismatch: command tenant does not match context tenant")
                .contains("First name is required")
                .contains("Last name is required")
                .contains("Date of birth cannot be in the future")
                .contains("Invalid gender code: INVALID")
                .contains("MRN (Medical Record Number) is required");
        }

        @Test
        @DisplayName("Should return error message combined with comma separator")
        void shouldReturnCombinedErrorMessage() {
            // Given - multiple errors
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName("")  // Empty - error
                .lastName("")   // Empty - error
                .dateOfBirth(LocalDate.of(1990, 1, 15))
                .mrn("MRN-123")
                .build();

            // When
            CreatePatientValidator.ValidationResult result = validator.validate(command, TENANT_ID);

            // Then - multiple errors should be comma-separated
            String errorMessage = result.getErrorMessage();
            assertThat(result.getErrors()).hasSize(2);  // Verify we have 2 errors
            assertThat(errorMessage).contains("First name is required");
            assertThat(errorMessage).contains("Last name is required");
            assertThat(errorMessage).contains(",");      // Verify comma separator
        }
    }
}
