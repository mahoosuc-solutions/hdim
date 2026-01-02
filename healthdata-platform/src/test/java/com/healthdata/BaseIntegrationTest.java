package com.healthdata;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

/**
 * Base integration test class for the HealthData Platform.
 *
 * This class provides:
 * - Spring Boot test environment with autoconfigured MockMvc
 * - TestRestTemplate for HTTP testing
 * - Database transaction management for test isolation
 * - Common assertion and utility methods
 * - Test data management
 *
 * Usage:
 * <code>
 * @SpringBootTest
 * public class MyIntegrationTest extends BaseIntegrationTest {
 *
 *     @Test
 *     public void testSomething() {
 *         // Use mockMvc, restTemplate, and utility methods
 *         MvcResult result = performGet("/api/patients");
 *         String content = getResponseContent(result);
 *     }
 * }
 * </code>
 *
 * Features:
 * - Auto-wired MockMvc for Spring MVC testing
 * - Auto-wired ObjectMapper for JSON serialization
 * - Test REST template with local server support
 * - Helper methods for common HTTP operations (GET, POST, PUT, DELETE)
 * - Authentication and authorization testing support
 * - Response parsing and assertion utilities
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = HealthDataPlatformApplication.class
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(HealthDataTestConfiguration.class)
@Transactional
public abstract class BaseIntegrationTest {

    /**
     * MockMvc for testing Spring MVC endpoints without starting a real HTTP server.
     * Provides fluent API for building HTTP requests and making assertions.
     */
    @Autowired
    protected MockMvc mockMvc;

    /**
     * ObjectMapper for JSON serialization/deserialization in tests.
     */
    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * TestRestTemplate for making HTTP requests to the running test server.
     * Used when full HTTP communication is needed.
     */
    @Autowired
    protected TestRestTemplate restTemplate;

    /**
     * Local server port assigned by Spring for TestRestTemplate.
     */
    @LocalServerPort
    protected int port;

    /**
     * Base URL for REST calls using TestRestTemplate.
     */
    protected String baseUrl;

    /**
     * Initialize common test settings before each test.
     * Sets up base URL and other common configurations.
     */
    @BeforeEach
    public void setUp() {
        this.baseUrl = "http://localhost:" + port;
    }

    /**
     * Perform a GET request using MockMvc.
     *
     * @param path the endpoint path (e.g., "/api/patients")
     * @return MvcResult containing the response
     * @throws Exception if the request fails
     */
    protected MvcResult performGet(String path) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders.get(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andReturn();
    }

    /**
     * Perform a GET request with bearer token authentication.
     *
     * @param path the endpoint path
     * @param token the JWT bearer token
     * @return MvcResult containing the response
     * @throws Exception if the request fails
     */
    protected MvcResult performGetWithAuth(String path, String token) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders.get(path)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
        )
                .andReturn();
    }

    /**
     * Perform a POST request using MockMvc.
     *
     * @param path the endpoint path
     * @param body the request body as a string
     * @return MvcResult containing the response
     * @throws Exception if the request fails
     */
    protected MvcResult performPost(String path, String body) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders.post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(csrf())
        )
                .andReturn();
    }

    /**
     * Perform a POST request with bearer token authentication.
     *
     * @param path the endpoint path
     * @param body the request body as a string
     * @param token the JWT bearer token
     * @return MvcResult containing the response
     * @throws Exception if the request fails
     */
    protected MvcResult performPostWithAuth(String path, String body, String token) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders.post(path)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(csrf())
        )
                .andReturn();
    }

    /**
     * Perform a PUT request using MockMvc.
     *
     * @param path the endpoint path
     * @param body the request body as a string
     * @return MvcResult containing the response
     * @throws Exception if the request fails
     */
    protected MvcResult performPut(String path, String body) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders.put(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(csrf())
        )
                .andReturn();
    }

    /**
     * Perform a PUT request with bearer token authentication.
     *
     * @param path the endpoint path
     * @param body the request body as a string
     * @param token the JWT bearer token
     * @return MvcResult containing the response
     * @throws Exception if the request fails
     */
    protected MvcResult performPutWithAuth(String path, String body, String token) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders.put(path)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(csrf())
        )
                .andReturn();
    }

    /**
     * Perform a DELETE request using MockMvc.
     *
     * @param path the endpoint path
     * @return MvcResult containing the response
     * @throws Exception if the request fails
     */
    protected MvcResult performDelete(String path) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders.delete(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
        )
                .andReturn();
    }

    /**
     * Perform a DELETE request with bearer token authentication.
     *
     * @param path the endpoint path
     * @param token the JWT bearer token
     * @return MvcResult containing the response
     * @throws Exception if the request fails
     */
    protected MvcResult performDeleteWithAuth(String path, String token) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders.delete(path)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .with(csrf())
        )
                .andReturn();
    }

    /**
     * Perform a PATCH request using MockMvc.
     *
     * @param path the endpoint path
     * @param body the request body as a string
     * @return MvcResult containing the response
     * @throws Exception if the request fails
     */
    protected MvcResult performPatch(String path, String body) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders.patch(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(csrf())
        )
                .andReturn();
    }

    /**
     * Perform a PATCH request with bearer token authentication.
     *
     * @param path the endpoint path
     * @param body the request body as a string
     * @param token the JWT bearer token
     * @return MvcResult containing the response
     * @throws Exception if the request fails
     */
    protected MvcResult performPatchWithAuth(String path, String body, String token) throws Exception {
        return mockMvc.perform(
                MockMvcRequestBuilders.patch(path)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(csrf())
        )
                .andReturn();
    }

    /**
     * Extract the response content as a string from MvcResult.
     *
     * @param result the MvcResult from a request
     * @return the response content as a string
     * @throws Exception if extraction fails
     */
    protected String getResponseContent(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString();
    }

    /**
     * Parse the response content into a specified class type.
     *
     * @param result the MvcResult from a request
     * @param clazz the target class to deserialize to
     * @return the deserialized object
     * @throws Exception if parsing fails
     */
    protected <T> T parseResponseContent(MvcResult result, Class<T> clazz) throws Exception {
        String content = getResponseContent(result);
        return objectMapper.readValue(content, clazz);
    }

    /**
     * Get the HTTP status code from MvcResult.
     *
     * @param result the MvcResult from a request
     * @return the HTTP status code
     */
    protected int getStatusCode(MvcResult result) {
        return result.getResponse().getStatus();
    }

    /**
     * Assert that the response has the expected HTTP status code.
     *
     * @param result the MvcResult from a request
     * @param expectedStatus the expected status code
     * @throws AssertionError if status doesn't match
     */
    protected void assertStatus(MvcResult result, int expectedStatus) {
        int actualStatus = getStatusCode(result);
        if (actualStatus != expectedStatus) {
            throw new AssertionError(
                    String.format("Expected status %d but got %d. Response: %s",
                            expectedStatus, actualStatus, getResponseContentSafely(result))
            );
        }
    }

    /**
     * Get response content safely, handling exceptions gracefully.
     *
     * @param result the MvcResult from a request
     * @return the response content or error message
     */
    protected String getResponseContentSafely(MvcResult result) {
        try {
            return getResponseContent(result);
        } catch (Exception e) {
            return "[Unable to get response content: " + e.getMessage() + "]";
        }
    }

    /**
     * Convert an object to JSON string.
     *
     * @param obj the object to convert
     * @return JSON string representation
     * @throws Exception if serialization fails
     */
    protected String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    /**
     * Parse a JSON string into a specified class type.
     *
     * @param json the JSON string
     * @param clazz the target class to deserialize to
     * @return the deserialized object
     * @throws Exception if parsing fails
     */
    protected <T> T fromJson(String json, Class<T> clazz) throws Exception {
        return objectMapper.readValue(json, clazz);
    }

    /**
     * Check if a response status indicates success (2xx).
     *
     * @param result the MvcResult from a request
     * @return true if status is between 200-299
     */
    protected boolean isSuccessResponse(MvcResult result) {
        int status = getStatusCode(result);
        return status >= 200 && status < 300;
    }

    /**
     * Check if a response status indicates client error (4xx).
     *
     * @param result the MvcResult from a request
     * @return true if status is between 400-499
     */
    protected boolean isClientErrorResponse(MvcResult result) {
        int status = getStatusCode(result);
        return status >= 400 && status < 500;
    }

    /**
     * Check if a response status indicates server error (5xx).
     *
     * @param result the MvcResult from a request
     * @return true if status is between 500-599
     */
    protected boolean isServerErrorResponse(MvcResult result) {
        int status = getStatusCode(result);
        return status >= 500 && status < 600;
    }

    /**
     * Get the location header from a response (useful for redirect URLs).
     *
     * @param result the MvcResult from a request
     * @return the Location header value or null if not present
     */
    protected String getLocationHeader(MvcResult result) {
        return result.getResponse().getHeader(HttpHeaders.LOCATION);
    }

    /**
     * Get a specific header value from the response.
     *
     * @param result the MvcResult from a request
     * @param headerName the name of the header
     * @return the header value or null if not present
     */
    protected String getHeader(MvcResult result, String headerName) {
        return result.getResponse().getHeader(headerName);
    }

    /**
     * Verify that a specific header is present in the response.
     *
     * @param result the MvcResult from a request
     * @param headerName the name of the header to check
     * @throws AssertionError if the header is not present
     */
    protected void assertHeaderPresent(MvcResult result, String headerName) {
        if (getHeader(result, headerName) == null) {
            throw new AssertionError("Expected header '" + headerName + "' not found in response");
        }
    }

    /**
     * Build a full URL for use with TestRestTemplate.
     *
     * @param path the path to append to base URL
     * @return the full URL
     */
    protected String buildUrl(String path) {
        return baseUrl + path;
    }
}
