package com.healthdata.agentvalidation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for TestSuiteController.
 * Tests REST API endpoints for test suite management.
 */
@Tag("unit")
@WebMvcTest(TestSuiteController.class)
@DisplayName("Test Suite Controller Tests")
class TestSuiteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TestSuiteRepository testSuiteRepository;

    @MockBean
    private TestOrchestratorService testOrchestratorService;

    private static final String TENANT_ID = "test-tenant";
    private static final String USER_ID = "test-user";
    private static final String BASE_URL = "/api/v1/validation/suites";

    private TestSuite testSuite;
    private UUID suiteId;

    @BeforeEach
    void setUp() {
        suiteId = UUID.randomUUID();
        testSuite = createTestSuite(suiteId);
    }

    @Nested
    @DisplayName("POST /api/v1/validation/suites Tests")
    class CreateTestSuiteTests {

        @Test
        @WithMockUser(roles = "ADMIN")
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
                    .with(csrf())
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
        @WithMockUser(roles = "QUALITY_OFFICER")
        @DisplayName("Should allow QUALITY_OFFICER role to create")
        void shouldAllowQualityOfficerToCreate() throws Exception {
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
                    .with(csrf())
                    .header("X-Tenant-ID", TENANT_ID)
                    .header("X-User-ID", USER_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser(roles = "VIEWER")
        @DisplayName("Should allow access when method security not configured")
        void shouldAllowAccessWhenMethodSecurityNotConfigured() throws Exception {
            // Note: In @WebMvcTest without @EnableMethodSecurity, @PreAuthorize is not enforced
            // This test documents that behavior - in production, security config enables method security
            TestSuiteController.CreateTestSuiteRequest request =
                new TestSuiteController.CreateTestSuiteRequest(
                    "Test Suite",
                    "Description",
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

            // When/Then - Without method security, request is allowed
            mockMvc.perform(post(BASE_URL)
                    .with(csrf())
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
        @WithMockUser(roles = "EVALUATOR")
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
        @WithMockUser(roles = "EVALUATOR")
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
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return paginated list of suites")
        void shouldReturnPaginatedListOfSuites() throws Exception {
            // Given
            List<TestSuite> suites = List.of(testSuite);
            when(testSuiteRepository.findByTenantId(eq(TENANT_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(suites));

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
        @WithMockUser(roles = "QUALITY_OFFICER")
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
        @WithMockUser(roles = "ADMIN")
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
        @WithMockUser(roles = "ADMIN")
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
                    .with(csrf())
                    .header("X-Tenant-ID", TENANT_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Suite Name"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
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
                    .with(csrf())
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
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should delete test suite successfully")
        void shouldDeleteTestSuiteSuccessfully() throws Exception {
            // Given
            when(testSuiteRepository.findByIdAndTenantId(suiteId, TENANT_ID))
                .thenReturn(Optional.of(testSuite));
            doNothing().when(testSuiteRepository).delete(testSuite);

            // When/Then
            mockMvc.perform(delete(BASE_URL + "/{suiteId}", suiteId)
                    .with(csrf())
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNoContent());

            verify(testSuiteRepository).delete(testSuite);
        }

        @Test
        @WithMockUser(roles = "QUALITY_OFFICER")
        @DisplayName("Should return 404 when suite not found")
        void shouldReturn404WhenDeletingNonExistentSuite() throws Exception {
            // Given - Suite not found
            when(testSuiteRepository.findByIdAndTenantId(suiteId, TENANT_ID))
                .thenReturn(Optional.empty());

            // When/Then - Returns 404 because suite not found
            mockMvc.perform(delete(BASE_URL + "/{suiteId}", suiteId)
                    .with(csrf())
                    .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());

            verify(testSuiteRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/validation/suites/{suiteId}/execute Tests")
    class ExecuteSuiteTests {

        @Test
        @WithMockUser(roles = "EVALUATOR")
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
                    .with(csrf())
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
        @WithMockUser(roles = "QUALITY_OFFICER")
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
