package com.healthdata;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Base class for REST controller and web layer tests.
 *
 * Extends BaseIntegrationTest with additional utilities specific to
 * testing REST endpoints and web controllers.
 *
 * Features:
 * - All features from BaseIntegrationTest
 * - Controller-specific assertion methods
 * - HTTP status code helpers
 * - Response parsing utilities for common scenarios
 *
 * Usage:
 * <code>
 * @SpringBootTest
 * public class PatientControllerTest extends BaseWebControllerTest {
 *
 *     @Test
 *     public void testListPatients() throws Exception {
 *         MvcResult result = performGet("/api/patients");
 *         assertOkStatus(result);
 *         // Additional assertions
 *     }
 * }
 * </code>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = HealthDataPlatformApplication.class
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(HealthDataTestConfiguration.class)
public abstract class BaseWebControllerTest extends BaseIntegrationTest {

    /**
     * Assert that the response has HTTP 200 (OK) status.
     *
     * @param result the MvcResult from a request
     */
    protected void assertOkStatus(MvcResult result) {
        assertStatus(result, 200);
    }

    /**
     * Assert that the response has HTTP 201 (Created) status.
     *
     * @param result the MvcResult from a request
     */
    protected void assertCreatedStatus(MvcResult result) {
        assertStatus(result, 201);
    }

    /**
     * Assert that the response has HTTP 204 (No Content) status.
     *
     * @param result the MvcResult from a request
     */
    protected void assertNoContentStatus(MvcResult result) {
        assertStatus(result, 204);
    }

    /**
     * Assert that the response has HTTP 400 (Bad Request) status.
     *
     * @param result the MvcResult from a request
     */
    protected void assertBadRequestStatus(MvcResult result) {
        assertStatus(result, 400);
    }

    /**
     * Assert that the response has HTTP 401 (Unauthorized) status.
     *
     * @param result the MvcResult from a request
     */
    protected void assertUnauthorizedStatus(MvcResult result) {
        assertStatus(result, 401);
    }

    /**
     * Assert that the response has HTTP 403 (Forbidden) status.
     *
     * @param result the MvcResult from a request
     */
    protected void assertForbiddenStatus(MvcResult result) {
        assertStatus(result, 403);
    }

    /**
     * Assert that the response has HTTP 404 (Not Found) status.
     *
     * @param result the MvcResult from a request
     */
    protected void assertNotFoundStatus(MvcResult result) {
        assertStatus(result, 404);
    }

    /**
     * Assert that the response has HTTP 409 (Conflict) status.
     *
     * @param result the MvcResult from a request
     */
    protected void assertConflictStatus(MvcResult result) {
        assertStatus(result, 409);
    }

    /**
     * Assert that the response has HTTP 422 (Unprocessable Entity) status.
     *
     * @param result the MvcResult from a request
     */
    protected void assertUnprocessableEntityStatus(MvcResult result) {
        assertStatus(result, 422);
    }

    /**
     * Assert that the response has HTTP 500 (Internal Server Error) status.
     *
     * @param result the MvcResult from a request
     */
    protected void assertInternalServerErrorStatus(MvcResult result) {
        assertStatus(result, 500);
    }

    /**
     * Assert that the response is successful (2xx status code).
     *
     * @param result the MvcResult from a request
     * @throws AssertionError if status is not 2xx
     */
    protected void assertSuccessResponse(MvcResult result) {
        if (!isSuccessResponse(result)) {
            throw new AssertionError(
                    String.format("Expected success response (2xx) but got %d. Response: %s",
                            getStatusCode(result), getResponseContentSafely(result))
            );
        }
    }

    /**
     * Assert that the response is a client error (4xx status code).
     *
     * @param result the MvcResult from a request
     * @throws AssertionError if status is not 4xx
     */
    protected void assertClientErrorResponse(MvcResult result) {
        if (!isClientErrorResponse(result)) {
            throw new AssertionError(
                    String.format("Expected client error (4xx) but got %d. Response: %s",
                            getStatusCode(result), getResponseContentSafely(result))
            );
        }
    }

    /**
     * Assert that the response is a server error (5xx status code).
     *
     * @param result the MvcResult from a request
     * @throws AssertionError if status is not 5xx
     */
    protected void assertServerErrorResponse(MvcResult result) {
        if (!isServerErrorResponse(result)) {
            throw new AssertionError(
                    String.format("Expected server error (5xx) but got %d. Response: %s",
                            getStatusCode(result), getResponseContentSafely(result))
            );
        }
    }

    /**
     * Assert that the response has a specific status code and parse content.
     * Combines status assertion with content parsing.
     *
     * @param result the MvcResult from a request
     * @param expectedStatus the expected HTTP status code
     * @param clazz the class to parse the response into
     * @return the parsed response object
     * @throws Exception if parsing fails
     */
    protected <T> T assertStatusAndParse(MvcResult result, int expectedStatus, Class<T> clazz) throws Exception {
        assertStatus(result, expectedStatus);
        return parseResponseContent(result, clazz);
    }

    /**
     * Assert that a successful response can be parsed to the specified type.
     *
     * @param result the MvcResult from a request
     * @param clazz the class to parse the response into
     * @return the parsed response object
     * @throws Exception if parsing fails
     */
    protected <T> T assertOkAndParse(MvcResult result, Class<T> clazz) throws Exception {
        assertOkStatus(result);
        return parseResponseContent(result, clazz);
    }

    /**
     * Assert that a creation response can be parsed to the specified type.
     *
     * @param result the MvcResult from a request
     * @param clazz the class to parse the response into
     * @return the parsed response object
     * @throws Exception if parsing fails
     */
    protected <T> T assertCreatedAndParse(MvcResult result, Class<T> clazz) throws Exception {
        assertCreatedStatus(result);
        return parseResponseContent(result, clazz);
    }

    /**
     * Assert that the response has a Location header (typical for POST requests).
     *
     * @param result the MvcResult from a request
     * @return the Location header value
     */
    protected String assertLocationHeaderPresent(MvcResult result) {
        assertHeaderPresent(result, "Location");
        return getLocationHeader(result);
    }

    /**
     * Assert that the response has a specific Content-Type header.
     *
     * @param result the MvcResult from a request
     * @param expectedContentType the expected content type
     */
    protected void assertContentType(MvcResult result, String expectedContentType) {
        String contentType = getHeader(result, "Content-Type");
        if (contentType == null || !contentType.contains(expectedContentType)) {
            throw new AssertionError(
                    String.format("Expected Content-Type '%s' but got '%s'", expectedContentType, contentType)
            );
        }
    }

    /**
     * Assert that the response has JSON content type.
     *
     * @param result the MvcResult from a request
     */
    protected void assertJsonContentType(MvcResult result) {
        assertContentType(result, "application/json");
    }

    /**
     * Assert that the response content is empty.
     *
     * @param result the MvcResult from a request
     */
    protected void assertEmptyContent(MvcResult result) {
        try {
            String content = getResponseContent(result);
            if (content != null && !content.trim().isEmpty()) {
                throw new AssertionError(
                        String.format("Expected empty content but got: %s", content)
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to check response content", e);
        }
    }

    /**
     * Get the value of a specific JSON field from the response.
     * Useful for quick assertions on specific fields.
     *
     * @param result the MvcResult from a request
     * @param fieldPath the path to the field (e.g., "user.name" for nested fields)
     * @return the field value as a string, or null if not found
     * @throws Exception if parsing fails
     */
    protected String getJsonFieldValue(MvcResult result, String fieldPath) throws Exception {
        String content = getResponseContent(result);
        com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(content);

        String[] parts = fieldPath.split("\\.");
        for (String part : parts) {
            if (node == null) {
                return null;
            }
            node = node.get(part);
        }

        return node != null ? node.asText() : null;
    }

    /**
     * Assert that a specific JSON field exists in the response.
     *
     * @param result the MvcResult from a request
     * @param fieldPath the path to the field
     * @throws Exception if parsing fails
     */
    protected void assertJsonFieldExists(MvcResult result, String fieldPath) throws Exception {
        String value = getJsonFieldValue(result, fieldPath);
        if (value == null) {
            throw new AssertionError("Expected JSON field '" + fieldPath + "' not found");
        }
    }

    /**
     * Assert that a specific JSON field has the expected value.
     *
     * @param result the MvcResult from a request
     * @param fieldPath the path to the field
     * @param expectedValue the expected value
     * @throws Exception if parsing fails
     */
    protected void assertJsonFieldValue(MvcResult result, String fieldPath, String expectedValue) throws Exception {
        String actualValue = getJsonFieldValue(result, fieldPath);
        if (!expectedValue.equals(actualValue)) {
            throw new AssertionError(
                    String.format("Expected JSON field '%s' to be '%s' but was '%s'",
                            fieldPath, expectedValue, actualValue)
            );
        }
    }
}
