package com.healthdata.openapi;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Enumeration;

/**
 * Utility class for validating HTTP requests and responses against OpenAPI specifications.
 * <p>
 * This class wraps the Atlassian Swagger Request Validator to provide a convenient API
 * for validating Spring MockMvc request/response pairs against OpenAPI 3.0 specifications.
 * </p>
 *
 * <h2>Usage Examples:</h2>
 * <pre>{@code
 * // Create validator from URL
 * OpenApiValidator validator = OpenApiValidator.forSpecUrl("http://localhost:8080/v3/api-docs");
 *
 * // Validate request and response
 * ValidationReport report = validator.validate(request, response);
 * if (report.hasErrors()) {
 *     fail(OpenApiValidator.formatErrors(report));
 * }
 * }</pre>
 *
 * @see OpenApiInteractionValidator
 */
@Slf4j
public class OpenApiValidator {

    private final OpenApiInteractionValidator validator;

    /**
     * Private constructor - use factory methods to create instances.
     */
    private OpenApiValidator(OpenApiInteractionValidator validator) {
        this.validator = validator;
    }

    /**
     * Creates an OpenApiValidator from a URL pointing to an OpenAPI specification.
     * <p>
     * Typically used with the SpringDoc endpoint: {@code http://localhost:PORT/v3/api-docs}
     * </p>
     *
     * @param specUrl URL to the OpenAPI JSON/YAML specification
     * @return configured OpenApiValidator instance
     * @throws IllegalArgumentException if the spec URL is invalid or spec cannot be loaded
     */
    public static OpenApiValidator forSpecUrl(String specUrl) {
        log.info("Creating OpenAPI validator from URL: {}", specUrl);
        OpenApiInteractionValidator validator = OpenApiInteractionValidator
                .createForSpecificationUrl(specUrl)
                .build();
        return new OpenApiValidator(validator);
    }

    /**
     * Creates an OpenApiValidator from inline OpenAPI specification content.
     * <p>
     * Useful for testing with embedded specifications or when the spec
     * is loaded from a file or resource.
     * </p>
     *
     * @param specContent OpenAPI specification as JSON or YAML string
     * @return configured OpenApiValidator instance
     * @throws IllegalArgumentException if the spec content is invalid
     */
    public static OpenApiValidator forSpecContent(String specContent) {
        log.info("Creating OpenAPI validator from inline specification");
        OpenApiInteractionValidator validator = OpenApiInteractionValidator
                .createFor(specContent)
                .build();
        return new OpenApiValidator(validator);
    }

    /**
     * Validates both the request and response against the OpenAPI specification.
     *
     * @param request  Spring MockHttpServletRequest from MockMvc
     * @param response Spring MockHttpServletResponse from MockMvc
     * @return ValidationReport containing any validation errors or warnings
     */
    public ValidationReport validate(MockHttpServletRequest request, MockHttpServletResponse response) {
        Request validatorRequest = convertRequest(request);
        Response validatorResponse = convertResponse(response);

        return validator.validate(validatorRequest, validatorResponse);
    }

    /**
     * Validates only the request against the OpenAPI specification.
     * <p>
     * Useful when you want to verify request parameters, headers, and body
     * without considering the response.
     * </p>
     *
     * @param request Spring MockHttpServletRequest from MockMvc
     * @return ValidationReport containing any request validation errors
     */
    public ValidationReport validateRequest(MockHttpServletRequest request) {
        Request validatorRequest = convertRequest(request);
        return validator.validateRequest(validatorRequest);
    }

    /**
     * Validates only the response against the OpenAPI specification.
     * <p>
     * Useful when you want to verify response status, headers, and body
     * for a specific endpoint path and method.
     * </p>
     *
     * @param response Spring MockHttpServletResponse from MockMvc
     * @param path     the API endpoint path (e.g., "/api/v1/patients/123")
     * @param method   the HTTP method (e.g., Request.Method.GET)
     * @return ValidationReport containing any response validation errors
     */
    public ValidationReport validateResponse(MockHttpServletResponse response, String path, Request.Method method) {
        Response validatorResponse = convertResponse(response);
        return validator.validateResponse(path, method, validatorResponse);
    }

    /**
     * Formats validation errors into a human-readable assertion message.
     * <p>
     * Useful for creating informative test failure messages.
     * </p>
     *
     * @param report the ValidationReport to format
     * @return formatted error message string
     */
    public static String formatErrors(ValidationReport report) {
        if (!report.hasErrors()) {
            return "No validation errors";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("OpenAPI validation failed with ").append(countErrors(report)).append(" error(s):\n\n");

        report.getMessages().stream()
                .filter(msg -> msg.getLevel() == ValidationReport.Level.ERROR)
                .forEach(msg -> {
                    sb.append("  - [").append(msg.getKey()).append("] ");
                    sb.append(msg.getMessage()).append("\n");
                    if (msg.getContext().isPresent()) {
                        sb.append("    Context: ").append(msg.getContext().get()).append("\n");
                    }
                });

        return sb.toString();
    }

    /**
     * Counts the number of error-level messages in the report.
     */
    private static long countErrors(ValidationReport report) {
        return report.getMessages().stream()
                .filter(msg -> msg.getLevel() == ValidationReport.Level.ERROR)
                .count();
    }

    /**
     * Converts a Spring MockHttpServletRequest to the validator's Request format.
     */
    private Request convertRequest(MockHttpServletRequest request) {
        SimpleRequest.Builder builder = new SimpleRequest.Builder(
                Request.Method.valueOf(request.getMethod()),
                request.getRequestURI()
        );

        // Add query parameters
        request.getParameterMap().forEach((name, values) -> {
            for (String value : values) {
                builder.withQueryParam(name, value);
            }
        });

        // Add headers
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            Enumeration<String> headerValues = request.getHeaders(headerName);
            while (headerValues.hasMoreElements()) {
                builder.withHeader(headerName, headerValues.nextElement());
            }
        }

        // Add body if present
        if (request.getContentAsByteArray() != null && request.getContentAsByteArray().length > 0) {
            try {
                String body = request.getContentAsString();
                String contentType = request.getContentType();
                if (contentType != null) {
                    builder.withBody(body);
                } else {
                    builder.withBody(body);
                }
            } catch (UnsupportedEncodingException e) {
                log.warn("Failed to read request body: {}", e.getMessage());
            }
        }

        return builder.build();
    }

    /**
     * Converts a Spring MockHttpServletResponse to the validator's Response format.
     */
    private Response convertResponse(MockHttpServletResponse response) {
        SimpleResponse.Builder builder = new SimpleResponse.Builder(response.getStatus());

        // Add headers
        response.getHeaderNames().forEach(headerName -> {
            Collection<String> headerValues = response.getHeaders(headerName);
            headerValues.forEach(value -> builder.withHeader(headerName, value));
        });

        // Add body if present
        try {
            String body = response.getContentAsString();
            if (body != null && !body.isEmpty()) {
                builder.withBody(body);
            }
        } catch (UnsupportedEncodingException e) {
            log.warn("Failed to read response body: {}", e.getMessage());
        }

        return builder.build();
    }

    /**
     * Gets the underlying OpenApiInteractionValidator for advanced use cases.
     *
     * @return the underlying validator instance
     */
    public OpenApiInteractionValidator getUnderlyingValidator() {
        return validator;
    }
}
