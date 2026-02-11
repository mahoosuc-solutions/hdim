package com.healthdata.agentvalidation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.healthdata.agentvalidation.domain.entity.TestSuite;
import com.healthdata.agentvalidation.domain.enums.TestStatus;
import com.healthdata.agentvalidation.domain.enums.UserStoryType;
import com.healthdata.agentvalidation.repository.TestSuiteRepository;
import com.healthdata.agentvalidation.service.TestOrchestratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for TestSuiteController.
 * Tests REST API endpoints for test suite management.
 *
 * Uses standalone MockMvc setup with Mockito to avoid loading Spring context
 * and JPA auto-configuration issues.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("Test Suite Controller Tests")
class TestSuiteControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private TestSuiteRepository testSuiteRepository;

    @Mock
    private TestOrchestratorService testOrchestratorService;

    @InjectMocks
    private TestSuiteController testSuiteController;

    private static final String TENANT_ID = "test-tenant";
    private static final String USER_ID = "test-user";
    private static final String BASE_URL = "/api/v1/validation/suites";

    private TestSuite testSuite;
    private UUID suiteId;

    @BeforeEach
    void setUp() {
        // Configure ObjectMapper for JSON serialization
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Create message converter with configured ObjectMapper
        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
        messageConverter.setObjectMapper(objectMapper);

        // Setup standalone MockMvc with pageable support and JSON message converter
        mockMvc = MockMvcBuilders.standaloneSetup(testSuiteController)
            .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
            .setMessageConverters(messageConverter)
            .build();

        suiteId = UUID.randomUUID();
        testSuite = createTestSuite(suiteId);
    }

    @Nested
    @DisplayName("POST /api/v1/validation/suites Tests")
    class CreateTestSuiteTests {

        @Test
        @DisplayName("Should create test suite successfully")
        void shouldCreateTestSuiteSuccessfully() throws Exception {
            // Given
            TestSuiteController.CreateTestSuiteRequest request =
                new TestSuiteController.CreateTestSuiteRequest(
                    "Care Gap Review Suite",
                    "Tests for care gap review workflows",
                    UserStoryType.PATIENT_SUMMARY_REVIEW,
                    "CLINICIAN",
                    "clinical-decision",
                    new BigDecimal("0.80")
                );

            when(testSuiteRepository.save(any(TestSuite.class)))
                .thenAnswer(inv -> {
                    TestSuite saved = inv.getArgument(0);
                    saved.setId(suiteId);
                    return saved;
                });

            // When/Then
            mockMvc.perform(post(BASE_URL)
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-User-ID", USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Care Gap Review Suite"))
                .andExpect(jsonPath("$.tenantId").value(TENANT_ID));

            verify(testSuiteRepository).save(any(TestSuite.class));
        }

        @Test
        @DisplayName("Should create suite with different user story type")
        void shouldCreateSuiteWithDifferentUserStoryType() throws Exception {
            // Given
            TestSuiteController.CreateTestSuiteRequest request =
                new TestSuiteController.CreateTestSuiteRequest(
                    "HEDIS Suite",
                    "HEDIS measure tests",
                    UserStoryType.HEDIS_MEASURE_EVALUATION,
                    "QUALITY_OFFICER",
                    "report-generator",
                    new BigDecimal("0.75")
                );

            when(testSuiteRepository.save(any(TestSuite.class)))
                .thenAnswer(inv -> {
                    TestSuite saved = inv.getArgument(0);
                    saved.setId(suiteId);
                    return saved;
                });

            // When/Then
            mockMvc.perform(post(BASE_URL)
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-User-ID", USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Should create suite with outreach optimization type")
        void shouldCreateSuiteWithOutreachType() throws Exception {
            // Given
            TestSuiteController.CreateTestSuiteRequest request =
                new TestSuiteController.CreateTestSuiteRequest(
                    "Outreach Suite",
                    "Outreach optimization tests",
                    UserStoryType.OUTREACH_OPTIMIZATION,
                    "NURSE",
                    "outreach-agent",
                    new BigDecimal("0.80")
                );

            when(testSuiteRepository.save(any(TestSuite.class)))
                .thenAnswer(inv -> {
                    TestSuite saved = inv.getArgument(0);
                    saved.setId(suiteId);
                    return saved;
                });

            // When/Then
            mockMvc.perform(post(BASE_URL)
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-User-ID", USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/validation/suites/{suiteId} Tests")
    class GetTestSuiteTests {

        @Test
        @DisplayName("Should return test suite when found")
        void shouldReturnTestSuiteWhenFound() throws Exception {
            // Given
            when(testSuiteRepository.findByIdAndTenantId(suiteId, TENANT_ID))
                .thenReturn(Optional.of(testSuite));

            // When/Then
            mockMvc.perform(get(BASE_URL + "/{suiteId}", suiteId)
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(suiteId.toString()))
                .andExpect(jsonPath("$.name").value(testSuite.getName()));
        }

        @Test
        @DisplayName("Should return 404 when suite not found")
        void shouldReturn404WhenSuiteNotFound() throws Exception {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(testSuiteRepository.findByIdAndTenantId(nonExistentId, TENANT_ID))
                .thenReturn(Optional.empty());

            // When/Then
            mockMvc.perform(get(BASE_URL + "/{suiteId}", nonExistentId)
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/validation/suites Tests")
    class ListTestSuitesTests {

        @Test
        @DisplayName("Should return paginated list of suites")
        void shouldReturnPaginatedListOfSuites() throws Exception {
            // Given
            List<TestSuite> suites = List.of(testSuite);
            PageRequest pageable = PageRequest.of(0, 20);
            Page<TestSuite> page = new PageImpl<>(suites, pageable, suites.size());
            when(testSuiteRepository.findByTenantId(eq(TENANT_ID), any(Pageable.class)))
                .thenReturn(page);

            // When/Then
            mockMvc.perform(get(BASE_URL)
                    .header("X-Tenant-ID", TENANT_ID)
                    .param("page", "0")
                    .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(suiteId.toString()));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/validation/suites/by-story-type/{type} Tests")
    class ListByUserStoryTypeTests {

        @Test
        @DisplayName("Should return suites by user story type")
        void shouldReturnSuitesByUserStoryType() throws Exception {
            // Given
            when(testSuiteRepository.findByTenantIdAndUserStoryType(
                TENANT_ID, UserStoryType.PATIENT_SUMMARY_REVIEW))
                .thenReturn(List.of(testSuite));

            // When/Then
            mockMvc.perform(get(BASE_URL + "/by-story-type/{type}",
                    UserStoryType.PATIENT_SUMMARY_REVIEW)
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].userStoryType").value("PATIENT_SUMMARY_REVIEW"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/validation/suites/by-role/{role} Tests")
    class ListByTargetRoleTests {

        @Test
        @DisplayName("Should return suites by target role")
        void shouldReturnSuitesByTargetRole() throws Exception {
            // Given
            when(testSuiteRepository.findByTenantIdAndTargetRole(TENANT_ID, "CLINICIAN"))
                .thenReturn(List.of(testSuite));

            // When/Then
            mockMvc.perform(get(BASE_URL + "/by-role/{role}", "CLINICIAN")
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].targetRole").value("CLINICIAN"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/validation/suites/{suiteId} Tests")
    class UpdateTestSuiteTests {

        @Test
        @DisplayName("Should update test suite successfully")
        void shouldUpdateTestSuiteSuccessfully() throws Exception {
            // Given
            TestSuiteController.UpdateTestSuiteRequest request =
                new TestSuiteController.UpdateTestSuiteRequest(
                    "Updated Suite Name",
                    "Updated description",
                    new BigDecimal("0.85"),
                    true
                );

            when(testSuiteRepository.findByIdAndTenantId(suiteId, TENANT_ID))
                .thenReturn(Optional.of(testSuite));
            when(testSuiteRepository.save(any(TestSuite.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            // When/Then
            mockMvc.perform(put(BASE_URL + "/{suiteId}", suiteId)
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Suite Name"));
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent suite")
        void shouldReturn404WhenUpdatingNonExistentSuite() throws Exception {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            TestSuiteController.UpdateTestSuiteRequest request =
                new TestSuiteController.UpdateTestSuiteRequest(
                    "Updated Name", null, null, null);

            when(testSuiteRepository.findByIdAndTenantId(nonExistentId, TENANT_ID))
                .thenReturn(Optional.empty());

            // When/Then
            mockMvc.perform(put(BASE_URL + "/{suiteId}", nonExistentId)
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/validation/suites/{suiteId} Tests")
    class DeleteTestSuiteTests {

        @Test
        @DisplayName("Should delete test suite successfully")
        void shouldDeleteTestSuiteSuccessfully() throws Exception {
            // Given
            when(testSuiteRepository.findByIdAndTenantId(suiteId, TENANT_ID))
                .thenReturn(Optional.of(testSuite));
            doNothing().when(testSuiteRepository).delete(testSuite);

            // When/Then
            mockMvc.perform(delete(BASE_URL + "/{suiteId}", suiteId)
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNoContent());

            verify(testSuiteRepository).delete(testSuite);
        }

        @Test
        @DisplayName("Should return 404 when suite not found")
        void shouldReturn404WhenDeletingNonExistentSuite() throws Exception {
            // Given - Suite not found
            when(testSuiteRepository.findByIdAndTenantId(suiteId, TENANT_ID))
                .thenReturn(Optional.empty());

            // When/Then - Returns 404 because suite not found
            mockMvc.perform(delete(BASE_URL + "/{suiteId}", suiteId)
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());

            verify(testSuiteRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/validation/suites/{suiteId}/execute Tests")
    class ExecuteSuiteTests {

        @Test
        @DisplayName("Should execute test suite successfully")
        void shouldExecuteTestSuiteSuccessfully() throws Exception {
            // Given
            TestOrchestratorService.TestSuiteExecutionResult result =
                TestOrchestratorService.TestSuiteExecutionResult.builder()
                    .suiteId(suiteId)
                    .totalTests(5)
                    .passedTests(4)
                    .failedTests(1)
                    .passRate(new BigDecimal("0.80"))
                    .passed(true)
                    .build();

            when(testOrchestratorService.executeSuite(suiteId, TENANT_ID, USER_ID))
                .thenReturn(result);

            // When/Then
            mockMvc.perform(post(BASE_URL + "/{suiteId}/execute", suiteId)
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-User-ID", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTests").value(5))
                .andExpect(jsonPath("$.passedTests").value(4))
                .andExpect(jsonPath("$.passRate").value(0.80));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/validation/suites/failing Tests")
    class GetFailingSuitesTests {

        @Test
        @DisplayName("Should return failing test suites")
        void shouldReturnFailingTestSuites() throws Exception {
            // Given
            testSuite.setStatus(TestStatus.FAILED);
            when(testSuiteRepository.findFailingTestSuites(TENANT_ID))
                .thenReturn(List.of(testSuite));

            // When/Then
            mockMvc.perform(get(BASE_URL + "/failing")
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("FAILED"));
        }
    }

    // Helper methods

    private TestSuite createTestSuite(UUID id) {
        TestSuite suite = new TestSuite();
        suite.setId(id);
        suite.setTenantId(TENANT_ID);
        suite.setName("Test Suite");
        suite.setDescription("Test suite for unit tests");
        suite.setUserStoryType(UserStoryType.PATIENT_SUMMARY_REVIEW);
        suite.setTargetRole("CLINICIAN");
        suite.setAgentType("clinical-decision");
        suite.setPassThreshold(new BigDecimal("0.80"));
        suite.setStatus(TestStatus.PENDING);
        suite.setActive(true);
        return suite;
    }
}
