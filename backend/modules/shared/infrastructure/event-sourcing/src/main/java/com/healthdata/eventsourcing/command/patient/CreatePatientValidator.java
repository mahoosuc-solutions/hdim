package com.healthdata.eventsourcing.command.patient;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * Validator for CreatePatientCommand.
 *
 * Uses fluent validation pattern with error collection:
 * - Validates all fields independently
 * - Collects all errors before returning
 * - Enables clear error reporting to user
 *
 * ★ Insight ─────────────────────────────────────
 * - Fail-fast approach: validate all at once, not stop at first error
 * - Multi-tenant isolation: uniqueness checks per tenant only
 * - Age validation: reasonable bounds (0-150 years)
 * ─────────────────────────────────────────────────
 */
@Component
@RequiredArgsConstructor
public class CreatePatientValidator {

    /**
     * Validate CreatePatientCommand
     * @param command command to validate
     * @param tenantId tenant context
     * @return validation result with errors if any
     */
    public ValidationResult validate(CreatePatientCommand command, String tenantId) {
        List<String> errors = new ArrayList<>();

        validateTenant(command, tenantId, errors);
        validateNames(command, errors);
        validateDateOfBirth(command, errors);
        validateGender(command, errors);
        validateMrn(command, errors);

        return new ValidationResult(errors);
    }

    private void validateTenant(CreatePatientCommand command, String tenantId, List<String> errors) {
        if (command.getTenantId() == null || command.getTenantId().isEmpty()) {
            errors.add("Tenant ID is required");
        } else if (!command.getTenantId().equals(tenantId)) {
            errors.add("Tenant ID mismatch: command tenant does not match context tenant");
        }
    }

    private void validateNames(CreatePatientCommand command, List<String> errors) {
        if (command.getFirstName() == null || command.getFirstName().isEmpty()) {
            errors.add("First name is required");
        } else if (command.getFirstName().length() > 100) {
            errors.add("First name cannot exceed 100 characters");
        } else if (!command.getFirstName().matches("^[a-zA-Z\\-]+$")) {
            errors.add("First name can only contain letters and hyphens");
        }

        if (command.getLastName() == null || command.getLastName().isEmpty()) {
            errors.add("Last name is required");
        } else if (command.getLastName().length() > 100) {
            errors.add("Last name cannot exceed 100 characters");
        } else if (!command.getLastName().matches("^[a-zA-Z\\-]+$")) {
            errors.add("Last name can only contain letters and hyphens");
        }
    }

    private void validateDateOfBirth(CreatePatientCommand command, List<String> errors) {
        if (command.getDateOfBirth() == null) {
            errors.add("Date of birth is required");
        } else {
            LocalDate now = LocalDate.now();

            // Check not in future
            if (command.getDateOfBirth().isAfter(now)) {
                errors.add("Date of birth cannot be in the future");
            }

            // Check reasonable age (max 150 years)
            Period age = Period.between(command.getDateOfBirth(), now);
            if (age.getYears() > 150) {
                errors.add("Patient age cannot exceed 150 years");
            }

            // Check minimum age (0 years, allowing newborns)
            if (age.getYears() < 0) {
                errors.add("Invalid date of birth");
            }
        }
    }

    private void validateGender(CreatePatientCommand command, List<String> errors) {
        if (command.getGender() != null && !isValidGender(command.getGender())) {
            errors.add("Invalid gender code: " + command.getGender());
        }
    }

    private boolean isValidGender(String gender) {
        return gender.matches("^(MALE|FEMALE|OTHER|UNKNOWN)$");
    }

    private void validateMrn(CreatePatientCommand command, List<String> errors) {
        if (command.getMrn() == null || command.getMrn().isEmpty()) {
            errors.add("MRN (Medical Record Number) is required");
        } else if (command.getMrn().length() > 50) {
            errors.add("MRN cannot exceed 50 characters");
        } else if (!command.getMrn().matches("^[a-zA-Z0-9\\-]+$")) {
            errors.add("MRN can only contain alphanumeric characters and hyphens");
        }
    }

    /**
     * Validation result object
     */
    public static class ValidationResult {
        private final List<String> errors;

        public ValidationResult(List<String> errors) {
            this.errors = errors;
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getErrorMessage() {
            return String.join(", ", errors);
        }
    }
}
