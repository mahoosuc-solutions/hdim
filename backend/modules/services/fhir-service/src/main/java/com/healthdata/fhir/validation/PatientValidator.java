package com.healthdata.fhir.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

@Component
public class PatientValidator {

    public ValidationResult validate(Patient patient) {
        List<String> errors = new ArrayList<>();

        if (patient == null) {
            errors.add("Patient resource must be provided");
            return ValidationResult.error(errors.getFirst());
        }

        if (!patient.hasName() || !patient.getNameFirstRep().hasFamily()) {
            errors.add("Patient must contain a family name");
        }

        if (patient.getNameFirstRep().getGiven().isEmpty()) {
            errors.add("Patient must contain at least one given name");
        }

        if (!patient.hasGender()) {
            errors.add("Patient gender is required");
        }

        if (!errors.isEmpty()) {
            return ValidationResult.error(String.join("; ", errors));
        }

        return ValidationResult.ok();
    }

    public record ValidationResult(boolean success, String message) {
        public static ValidationResult ok() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult error(String message) {
            Objects.requireNonNull(message, "message must not be null");
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return success;
        }
    }
}
