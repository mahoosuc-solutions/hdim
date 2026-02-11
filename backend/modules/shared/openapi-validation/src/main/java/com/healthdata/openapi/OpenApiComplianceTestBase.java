package com.healthdata.openapi;

import com.atlassian.oai.validator.report.ValidationReport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Base class for OpenAPI compliance integration tests.
 * <p>
 * This class provides a standardized way to verify that REST API implementations
 * conform to their OpenAPI specifications. It automatically loads the OpenAPI spec
 * from the running service and provides assertion methods for request/response validation.
 * </p>
 *
 * <h2>Usage:</h2>
 * <pre>{@code
 * @Tag("integration")
 * class PatientControllerOpenApiComplianceTest extends OpenApiComplianceTestBase {
 *
 *     @Test
 *     void getPatient_shouldConformToSpec() throws Exception {
 *         MvcResult result = mockMvc.perform(get("/api/v1/patients/123")
 *                 .header("X-Tenant-ID", "tenant1"))
 *             .andExpect(status().isOk())
 *             .andReturn();
 *
 *         assertResponseMatchesSpec(result);
 *     }
 * }
 * }</pre>
 *
 * <h2>Prerequisites:</h2>
 * <ul>
 *     <li>The service must have SpringDoc OpenAPI configured</li>
 *     <li>The OpenAPI spec must be available at {@code /v3/api-docs}</li>
 *     <li>The test must run with a real server (RANDOM_PORT)</li>
 * </ul>
 *
 * @see OpenApiValidator
 */
@Tag("integration")
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Slf4j
public abstract class OpenApiComplianceTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @LocalServerPort
    protected int port;

    /**
     * The OpenAPI validator instance, initialized before each test.
     * Subclasses can use this directly for advanced validation scenarios.
     */
    protected OpenApiValidator validator;

    /**
     * Sets up the OpenAPI validator before each test by loading the spec
     * from the running service.
     */
    @BeforeEach
    void setUpOpenApiValidator() {
        String specUrl = String.format("http://localhost:%d/v3/api-docs", port);
        log.info("Initializing OpenAPI validator from: {}", specUrl);
        this.validator = OpenApiValidator.forSpecUrl(specUrl);
    }

    /**
     * Asserts that the response from an MvcResult matches the OpenAPI specification.
     * <p>
     * This validates the response status code, headers, and body against
     * the expected response schema defined in the OpenAPI spec.
     * </p>
     *
     * @param result the MvcResult from a MockMvc request
     * @throws AssertionError if the response does not match the spec
     */
    protected void assertResponseMatchesSpec(MvcResult result) {
        MockHttpServletRequest request = result.getRequest();
        MockHttpServletResponse response = result.getResponse();

        ValidationReport report = validator.validate(request, response);

        if (report.hasErrors()) {
            log.error("OpenAPI validation failed for {} {}: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    OpenApiValidator.formatErrors(report));
        }

        assertFalse(report.hasErrors(),
                () -> String.format("Response for %s %s does not match OpenAPI spec:\n%s",
                        request.getMethod(),
                        request.getRequestURI(),
                        OpenApiValidator.formatErrors(report)));
    }

    /**
     * Asserts that the request from an MvcResult matches the OpenAPI specification.
     * <p>
     * This validates the request path, method, parameters, headers, and body
     * against the expected request schema defined in the OpenAPI spec.
     * </p>
     *
     * @param result the MvcResult from a MockMvc request
     * @throws AssertionError if the request does not match the spec
     */
    protected void assertRequestMatchesSpec(MvcResult result) {
        MockHttpServletRequest request = result.getRequest();

        ValidationReport report = validator.validateRequest(request);

        if (report.hasErrors()) {
            log.error("OpenAPI request validation failed for {} {}: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    OpenApiValidator.formatErrors(report));
        }

        assertFalse(report.hasErrors(),
                () -> String.format("Request for %s %s does not match OpenAPI spec:\n%s",
                        request.getMethod(),
                        request.getRequestURI(),
                        OpenApiValidator.formatErrors(report)));
    }

    /**
     * Asserts that both request and response match the OpenAPI specification.
     * <p>
     * This is a convenience method that combines both request and response validation.
     * Use this when you want to ensure full compliance in both directions.
     * </p>
     *
     * @param result the MvcResult from a MockMvc request
     * @throws AssertionError if either request or response does not match the spec
     */
    protected void assertRequestAndResponseMatchSpec(MvcResult result) {
        assertRequestMatchesSpec(result);
        assertResponseMatchesSpec(result);
    }

    /**
     * Gets the base URL for the running test server.
     *
     * @return base URL in the format "http://localhost:{port}"
     */
    protected String getBaseUrl() {
        return String.format("http://localhost:%d", port);
    }

    /**
     * Gets the OpenAPI specification URL for the running test server.
     *
     * @return OpenAPI spec URL in the format "http://localhost:{port}/v3/api-docs"
     */
    protected String getOpenApiSpecUrl() {
        return String.format("%s/v3/api-docs", getBaseUrl());
    }

    /**
     * Override this method to customize the OpenAPI spec URL path.
     * <p>
     * By default, this returns "/v3/api-docs" which is the standard
     * SpringDoc endpoint. Override if your service uses a different path.
     * </p>
     *
     * @return the path to the OpenAPI specification endpoint
     */
    protected String getOpenApiSpecPath() {
        return "/v3/api-docs";
    }
}
