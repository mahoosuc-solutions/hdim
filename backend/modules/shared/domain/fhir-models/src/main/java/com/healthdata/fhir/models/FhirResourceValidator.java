package com.healthdata.fhir.models;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.healthdata.common.validation.ValidationResult;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Resource;

/**
 * Wraps HAPI-FHIR validation support with a simplified result object for consumers.
 */
public class FhirResourceValidator {

    private static final FhirContext DEFAULT_CONTEXT = FhirContext.forR4();

    private final FhirValidator validator;

    public FhirResourceValidator() {
        this(DEFAULT_CONTEXT);
    }

    public FhirResourceValidator(FhirContext context) {
        Objects.requireNonNull(context, "context must not be null");
        DefaultProfileValidationSupport defaultSupport = new DefaultProfileValidationSupport(context);
        InMemoryTerminologyServerValidationSupport terminologySupport =
                new InMemoryTerminologyServerValidationSupport(context);
        ValidationSupportChain validationSupport = new ValidationSupportChain(defaultSupport, terminologySupport);
        FhirInstanceValidator instanceValidator = new FhirInstanceValidator(validationSupport);
        instanceValidator.setAnyExtensionsAllowed(true);

        this.validator = context.newValidator();
        this.validator.registerValidatorModule(instanceValidator);
    }

    /**
     * Validates the supplied FHIR resource.
     */
    public ValidationResult validate(Resource resource) {
        if (resource == null) {
            return ValidationResult.failure(List.of("Resource must not be null"));
        }

        ca.uhn.fhir.validation.ValidationResult hapiResult = validator.validateWithResult(resource);
        List<String> errors = hapiResult.getMessages().stream()
                .filter(message -> message.getSeverity() == ResultSeverityEnum.ERROR
                        || message.getSeverity() == ResultSeverityEnum.FATAL)
                .map(this::formatMessage)
                .collect(Collectors.toList());

        List<String> warnings = hapiResult.getMessages().stream()
                .filter(message -> message.getSeverity() == ResultSeverityEnum.WARNING
                        || message.getSeverity() == ResultSeverityEnum.INFORMATION)
                .map(this::formatMessage)
                .collect(Collectors.toList());

        if (errors.isEmpty()) {
            return ValidationResult.success(warnings);
        }
        return ValidationResult.failure(errors, warnings);
    }

    private String formatMessage(SingleValidationMessage message) {
        String location = StringUtils.defaultIfBlank(message.getLocationString(), "Unknown location");
        return String.format("%s: %s", location, message.getMessage());
    }
}
