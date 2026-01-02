package com.healthdata.aiassistant.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ChatRequest DTO.
 *
 * Tests TDD principles:
 * - Cache key generation
 * - Validation of required fields
 * - Builder pattern functionality
 */
@DisplayName("ChatRequest Tests")
class ChatRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should generate cache key from tenant, queryType, and query hash")
    void testCacheKey_Generation() {
        // Given: A chat request with specific values
        ChatRequest request = ChatRequest.builder()
            .tenantId("tenant-123")
            .queryType("care_gaps")
            .query("What are the care gaps?")
            .patientId("patient-456")
            .build();

        // When: Generate cache key
        String cacheKey = request.cacheKey();

        // Then: Cache key should contain tenant and query type
        assertThat(cacheKey)
            .isNotNull()
            .startsWith("tenant-123:care_gaps:")
            .contains(":");
    }

    @Test
    @DisplayName("Should generate consistent cache keys for same request")
    void testCacheKey_Consistency() {
        // Given: Two identical requests
        ChatRequest request1 = ChatRequest.builder()
            .tenantId("tenant-123")
            .queryType("care_gaps")
            .query("What are the care gaps?")
            .patientId("patient-456")
            .build();

        ChatRequest request2 = ChatRequest.builder()
            .tenantId("tenant-123")
            .queryType("care_gaps")
            .query("What are the care gaps?")
            .patientId("patient-456")
            .build();

        // When: Generate cache keys
        String key1 = request1.cacheKey();
        String key2 = request2.cacheKey();

        // Then: Cache keys should be identical
        assertThat(key1).isEqualTo(key2);
    }

    @Test
    @DisplayName("Should generate different cache keys for different queries")
    void testCacheKey_Uniqueness() {
        // Given: Two different requests
        ChatRequest request1 = ChatRequest.builder()
            .tenantId("tenant-123")
            .queryType("care_gaps")
            .query("Query 1")
            .build();

        ChatRequest request2 = ChatRequest.builder()
            .tenantId("tenant-123")
            .queryType("care_gaps")
            .query("Query 2")
            .build();

        // When: Generate cache keys
        String key1 = request1.cacheKey();
        String key2 = request2.cacheKey();

        // Then: Cache keys should be different
        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    @DisplayName("Should generate different cache keys for different tenants")
    void testCacheKey_DifferentTenants() {
        // Given: Requests with different tenants
        ChatRequest request1 = ChatRequest.builder()
            .tenantId("tenant-1")
            .queryType("care_gaps")
            .query("Same query")
            .build();

        ChatRequest request2 = ChatRequest.builder()
            .tenantId("tenant-2")
            .queryType("care_gaps")
            .query("Same query")
            .build();

        // When: Generate cache keys
        String key1 = request1.cacheKey();
        String key2 = request2.cacheKey();

        // Then: Cache keys should be different (tenant isolation)
        assertThat(key1).isNotEqualTo(key2);
        assertThat(key1).startsWith("tenant-1:");
        assertThat(key2).startsWith("tenant-2:");
    }

    @Test
    @DisplayName("Should generate different cache keys for different query types")
    void testCacheKey_DifferentQueryTypes() {
        // Given: Requests with different query types
        ChatRequest request1 = ChatRequest.builder()
            .tenantId("tenant-123")
            .queryType("care_gaps")
            .query("Same query")
            .build();

        ChatRequest request2 = ChatRequest.builder()
            .tenantId("tenant-123")
            .queryType("quality_measures")
            .query("Same query")
            .build();

        // When: Generate cache keys
        String key1 = request1.cacheKey();
        String key2 = request2.cacheKey();

        // Then: Cache keys should be different
        assertThat(key1).isNotEqualTo(key2);
        assertThat(key1).contains(":care_gaps:");
        assertThat(key2).contains(":quality_measures:");
    }

    @Test
    @DisplayName("Should fail validation when queryType is missing")
    void testValidation_MissingQueryType() {
        // Given: Request without queryType
        ChatRequest request = ChatRequest.builder()
            .query("Some query")
            .tenantId("tenant-123")
            .build();

        // When: Validate
        Set<ConstraintViolation<ChatRequest>> violations = validator.validate(request);

        // Then: Should have validation error for queryType
        assertThat(violations).isNotEmpty();
        assertThat(violations)
            .extracting(ConstraintViolation::getMessage)
            .contains("Query type is required");
    }

    @Test
    @DisplayName("Should fail validation when queryType is blank")
    void testValidation_BlankQueryType() {
        // Given: Request with blank queryType
        ChatRequest request = ChatRequest.builder()
            .queryType("   ")
            .query("Some query")
            .tenantId("tenant-123")
            .build();

        // When: Validate
        Set<ConstraintViolation<ChatRequest>> violations = validator.validate(request);

        // Then: Should have validation error
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("Should fail validation when query is missing")
    void testValidation_MissingQuery() {
        // Given: Request without query
        ChatRequest request = ChatRequest.builder()
            .queryType("care_gaps")
            .tenantId("tenant-123")
            .build();

        // When: Validate
        Set<ConstraintViolation<ChatRequest>> violations = validator.validate(request);

        // Then: Should have validation error for query
        assertThat(violations).isNotEmpty();
        assertThat(violations)
            .extracting(ConstraintViolation::getMessage)
            .contains("Query is required");
    }

    @Test
    @DisplayName("Should fail validation when query is blank")
    void testValidation_BlankQuery() {
        // Given: Request with blank query
        ChatRequest request = ChatRequest.builder()
            .queryType("care_gaps")
            .query("   ")
            .tenantId("tenant-123")
            .build();

        // When: Validate
        Set<ConstraintViolation<ChatRequest>> violations = validator.validate(request);

        // Then: Should have validation error
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("Should fail validation when tenantId is missing")
    void testValidation_MissingTenantId() {
        // Given: Request without tenantId
        ChatRequest request = ChatRequest.builder()
            .queryType("care_gaps")
            .query("Some query")
            .build();

        // When: Validate
        Set<ConstraintViolation<ChatRequest>> violations = validator.validate(request);

        // Then: Should have validation error for tenantId
        assertThat(violations).isNotEmpty();
        assertThat(violations)
            .extracting(ConstraintViolation::getMessage)
            .contains("Tenant ID is required");
    }

    @Test
    @DisplayName("Should fail validation when query exceeds max length")
    void testValidation_QueryTooLong() {
        // Given: Request with query exceeding 10000 characters
        String longQuery = "x".repeat(10001);
        ChatRequest request = ChatRequest.builder()
            .queryType("care_gaps")
            .query(longQuery)
            .tenantId("tenant-123")
            .build();

        // When: Validate
        Set<ConstraintViolation<ChatRequest>> violations = validator.validate(request);

        // Then: Should have validation error for query size
        assertThat(violations).isNotEmpty();
        assertThat(violations)
            .extracting(ConstraintViolation::getMessage)
            .contains("Query must be 10000 characters or less");
    }

    @Test
    @DisplayName("Should pass validation with all required fields")
    void testValidation_ValidRequest() {
        // Given: Request with all required fields
        ChatRequest request = ChatRequest.builder()
            .queryType("care_gaps")
            .query("What are the care gaps?")
            .tenantId("tenant-123")
            .build();

        // When: Validate
        Set<ConstraintViolation<ChatRequest>> violations = validator.validate(request);

        // Then: Should have no validation errors
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should build request with all optional fields")
    void testBuilder_WithAllFields() {
        // Given: Build request with all fields
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.user("Hello"));

        ChatRequest request = ChatRequest.builder()
            .queryType("care_gaps")
            .query("What are the care gaps?")
            .messages(messages)
            .patientId("patient-123")
            .tenantId("tenant-456")
            .context("{\"key\":\"value\"}")
            .streaming(true)
            .sessionId("session-789")
            .build();

        // Then: All fields should be set
        assertThat(request.getQueryType()).isEqualTo("care_gaps");
        assertThat(request.getQuery()).isEqualTo("What are the care gaps?");
        assertThat(request.getMessages()).hasSize(1);
        assertThat(request.getPatientId()).isEqualTo("patient-123");
        assertThat(request.getTenantId()).isEqualTo("tenant-456");
        assertThat(request.getContext()).isEqualTo("{\"key\":\"value\"}");
        assertThat(request.isStreaming()).isTrue();
        assertThat(request.getSessionId()).isEqualTo("session-789");
    }

    @Test
    @DisplayName("Should default streaming to false")
    void testBuilder_DefaultStreaming() {
        // Given: Request without explicit streaming value
        ChatRequest request = ChatRequest.builder()
            .queryType("care_gaps")
            .query("What are the care gaps?")
            .tenantId("tenant-123")
            .build();

        // Then: Streaming should default to false
        assertThat(request.isStreaming()).isFalse();
    }

    @Test
    @DisplayName("Should allow query at max length")
    void testValidation_QueryAtMaxLength() {
        // Given: Request with query at exactly 10000 characters
        String maxQuery = "x".repeat(10000);
        ChatRequest request = ChatRequest.builder()
            .queryType("care_gaps")
            .query(maxQuery)
            .tenantId("tenant-123")
            .build();

        // When: Validate
        Set<ConstraintViolation<ChatRequest>> violations = validator.validate(request);

        // Then: Should have no validation errors
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should support lombok getters and setters")
    void testLombokFunctionality() {
        // Given: Create request using constructor
        ChatRequest request = new ChatRequest();

        // When: Use setters
        request.setQueryType("care_gaps");
        request.setQuery("Test query");
        request.setTenantId("tenant-123");
        request.setPatientId("patient-456");
        request.setStreaming(true);

        // Then: Getters should return correct values
        assertThat(request.getQueryType()).isEqualTo("care_gaps");
        assertThat(request.getQuery()).isEqualTo("Test query");
        assertThat(request.getTenantId()).isEqualTo("tenant-123");
        assertThat(request.getPatientId()).isEqualTo("patient-456");
        assertThat(request.isStreaming()).isTrue();
    }

    @Test
    @DisplayName("Should include patientId in cache key hash")
    void testCacheKey_IncludesPatientId() {
        // Given: Two requests with same query but different patients
        ChatRequest request1 = ChatRequest.builder()
            .tenantId("tenant-123")
            .queryType("patient_summary")
            .query("Generate summary")
            .patientId("patient-1")
            .build();

        ChatRequest request2 = ChatRequest.builder()
            .tenantId("tenant-123")
            .queryType("patient_summary")
            .query("Generate summary")
            .patientId("patient-2")
            .build();

        // When: Generate cache keys
        String key1 = request1.cacheKey();
        String key2 = request2.cacheKey();

        // Then: Cache keys should be different (different patient IDs)
        assertThat(key1).isNotEqualTo(key2);
    }
}
