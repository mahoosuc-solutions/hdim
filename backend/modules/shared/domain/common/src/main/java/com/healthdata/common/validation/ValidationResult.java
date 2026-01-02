package com.healthdata.common.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable container for validation outcomes across domain layers.
 */
public final class ValidationResult {

    private static final ValidationResult SUCCESS = new ValidationResult(true, List.of(), List.of());

    private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;

    private ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
        this.valid = valid;
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
        this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
    }

    /**
     * Creates a successful validation result with optional warnings.
     */
    public static ValidationResult success(List<String> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return SUCCESS;
        }
        return new ValidationResult(true, List.of(), warnings);
    }

    /**
     * Creates a successful validation result without warnings.
     */
    public static ValidationResult success() {
        return SUCCESS;
    }

    /**
     * Creates a failed validation result with errors and optional warnings.
     */
    public static ValidationResult failure(List<String> errors, List<String> warnings) {
        Objects.requireNonNull(errors, "errors must not be null");
        if (errors.isEmpty()) {
            throw new IllegalArgumentException("failure requires at least one error");
        }
        return new ValidationResult(false, errors, warnings == null ? List.of() : warnings);
    }

    /**
     * Creates a failed validation result with errors.
     */
    public static ValidationResult failure(List<String> errors) {
        return failure(errors, List.of());
    }

    /**
     * Returns whether the validation passed without errors.
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Returns the immutable list of validation errors.
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * Returns the immutable list of validation warnings.
     */
    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * Merges this result with another one, combining messages.
     */
    public ValidationResult merge(ValidationResult other) {
        Objects.requireNonNull(other, "other must not be null");
        if (this.isValid() && other.isValid()) {
            return success(combine(this.warnings, other.warnings));
        }
        List<String> combinedErrors = combine(this.errors, other.errors);
        List<String> combinedWarnings = combine(this.warnings, other.warnings);
        return failure(combinedErrors, combinedWarnings);
    }

    private static List<String> combine(List<String> first, List<String> second) {
        if ((first == null || first.isEmpty()) && (second == null || second.isEmpty())) {
            return List.of();
        }
        List<String> combined = new ArrayList<>();
        if (first != null) {
            combined.addAll(first);
        }
        if (second != null) {
            combined.addAll(second);
        }
        return combined;
    }
}
