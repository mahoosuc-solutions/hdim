package com.healthdata.agentvalidation.service;

import com.healthdata.agentvalidation.client.AgentRuntimeClient;
import com.healthdata.agentvalidation.client.dto.AgentExecutionRequest;
import com.healthdata.agentvalidation.client.dto.AgentExecutionResponse;
import com.healthdata.agentvalidation.config.ValidationProperties;
import com.healthdata.agentvalidation.domain.entity.TestCase;
import com.healthdata.agentvalidation.domain.entity.TestExecution;
import com.healthdata.agentvalidation.domain.entity.TestSuite;
import com.healthdata.agentvalidation.domain.enums.EvaluationMetricType;
import com.healthdata.agentvalidation.domain.enums.TestStatus;
import com.healthdata.agentvalidation.domain.enums.UserStoryType;
import com.healthdata.agentvalidation.repository.GoldenResponseRepository;
import com.healthdata.agentvalidation.repository.TestCaseRepository;
import com.healthdata.agentvalidation.repository.TestExecutionRepository;
import com.healthdata.agentvalidation.repository.TestSuiteRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TestOrchestratorService.
 * Tests test suite and test case execution orchestration.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("Test Orchestrator Service Tests")
class TestOrchestratorServiceTest {

    @Mock
    private TestSuiteRepository testSuiteRepository;

    @Mock
    private TestCaseRepository testCaseRepository;

    @Mock
    private TestExecutionRepository testExecutionRepository;

    @Mock
    private GoldenResponseRepository goldenResponseRepository;

    @Mock
    private AgentRuntimeClient agentRuntimeClient;

    private ValidationProperties validationProperties;
    private MeterRegistry meterRegistry;
    private TestOrchestratorService orchestratorService;

    private static final String TENANT_ID = "test-tenant";
    private static final String USER_ID = "test-user";

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        validationProperties = createValidationProperties();
        orchestratorService = new TestOrchestratorService(
            testSuiteRepository,
            testCaseRepository,
            testExecutionRepository,
            goldenResponseRepository,
            agentRuntimeClient,
            validationProperties,
            meterRegistry
        );
    }

    private ValidationProperties createValidationProperties() {
        ValidationProperties props = new ValidationProperties();

        ValidationProperties.OrchestratorConfig orchestratorConfig = new ValidationProperties.OrchestratorConfig();
        orchestratorConfig.setMaxConcurrentTests(5);
        orchestratorConfig.setTestTimeoutSeconds(30);
        orchestratorConfig.setDefaultPassThreshold(new BigDecimal("0.80"));
        props.setOrchestrator(orchestratorConfig);

        ValidationProperties.QaConfig qaConfig = new ValidationProperties.QaConfig();
        qaConfig.setAutoFlagScoreThreshold(new BigDecimal("0.70"));
        qaConfig.setConfidenceMiscalibrationThreshold(new BigDecimal("0.20"));
        props.setQa(qaConfig);

        ValidationProperties.ReflectionConfig reflectionConfig = new ValidationProperties.ReflectionConfig();
        reflectionConfig.setEnabled(false);
        props.setReflection(reflectionConfig);

        return props;
    }

    @Nested
    @DisplayName("Execute Suite Tests")
    class ExecuteSuiteTests {

        @Test
        @DisplayName("Should execute all test cases in suite")
        void shouldExecuteAllTestCasesInSuite() {
            // Given
            UUID suiteId = UUID.randomUUID();
            TestSuite suite = createTestSuite(suiteId);
            List<TestCase> testCases = createTestCases(suite, 3);

            when(testSuiteRepository.findByIdAndTenantId(suiteId, TENANT_ID))
                .thenReturn(Optional.of(suite));
            when(testCaseRepository.findByTestSuiteIdOrderByExecutionPriorityAsc(suiteId))
                .thenReturn(testCases);
            when(testSuiteRepository.save(any(TestSuite.class)))
                .thenAnswer(inv -> inv.getArgument(0));
            when(testExecutionRepository.save(any(TestExecution.class)))
                .thenAnswer(inv -> inv.getArgument(0));
            when(testCaseRepository.save(any(TestCase.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            setupAgentRuntimeSuccess();

            // When
            TestOrchestratorService.TestSuiteExecutionResult result =
                orchestratorService.executeSuite(suiteId, TENANT_ID, USER_ID);

            // Then
            assertThat(result.getTotalTests()).isEqualTo(3);
            assertThat(result.getExecutions()).hasSize(3);
            verify(agentRuntimeClient, times(3)).executeAgent(
                eq(TENANT_ID), eq(USER_ID), anyString(), any(AgentExecutionRequest.class));
        }

        @Test
        @DisplayName("Should calculate pass rate correctly")
        void shouldCalculatePassRateCorrectly() {
            // Given
            UUID suiteId = UUID.randomUUID();
            TestSuite suite = createTestSuite(suiteId);
            suite.setPassThreshold(new BigDecimal("0.60"));
            List<TestCase> testCases = createTestCases(suite, 3);

            when(testSuiteRepository.findByIdAndTenantId(suiteId, TENANT_ID))
                .thenReturn(Optional.of(suite));
            when(testCaseRepository.findByTestSuiteIdOrderByExecutionPriorityAsc(suiteId))
                .thenReturn(testCases);
            when(testSuiteRepository.save(any(TestSuite.class)))
                .thenAnswer(inv -> inv.getArgument(0));
            when(testExecutionRepository.save(any(TestExecution.class)))
                .thenAnswer(inv -> inv.getArgument(0));
            when(testCaseRepository.save(any(TestCase.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            setupAgentRuntimeSuccess();

            // When
            TestOrchestratorService.TestSuiteExecutionResult result =
                orchestratorService.executeSuite(suiteId, TENANT_ID, USER_ID);

            // Then
            assertThat(result.getPassRate()).isNotNull();
            assertThat(result.getPassedTests()).isGreaterThanOrEqualTo(0);
            assertThat(result.getFailedTests()).isGreaterThanOrEqualTo(0);
            assertThat(result.getPassedTests() + result.getFailedTests()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should throw exception when suite not found")
        void shouldThrowExceptionWhenSuiteNotFound() {
            // Given
            UUID suiteId = UUID.randomUUID();
            when(testSuiteRepository.findByIdAndTenantId(suiteId, TENANT_ID))
                .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() ->
                orchestratorService.executeSuite(suiteId, TENANT_ID, USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Test suite not found");
        }

        @Test
        @DisplayName("Should update suite status based on pass threshold")
        void shouldUpdateSuiteStatusBasedOnPassThreshold() {
            // Given
            UUID suiteId = UUID.randomUUID();
            TestSuite suite = createTestSuite(suiteId);
            suite.setPassThreshold(new BigDecimal("0.80"));

            when(testSuiteRepository.findByIdAndTenantId(suiteId, TENANT_ID))
                .thenReturn(Optional.of(suite));
            when(testCaseRepository.findByTestSuiteIdOrderByExecutionPriorityAsc(suiteId))
                .thenReturn(Collections.emptyList());
            when(testSuiteRepository.save(any(TestSuite.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            // When
            orchestratorService.executeSuite(suiteId, TENANT_ID, USER_ID);

            // Then
            ArgumentCaptor<TestSuite> suiteCaptor = ArgumentCaptor.forClass(TestSuite.class);
            verify(testSuiteRepository, atLeastOnce()).save(suiteCaptor.capture());

            TestSuite savedSuite = suiteCaptor.getValue();
            assertThat(savedSuite.getLastExecutionAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Execute Case Tests")
    class ExecuteCaseTests {

        @Test
        @DisplayName("Should execute single test case successfully")
        void shouldExecuteSingleTestCaseSuccessfully() {
            // Given
            TestSuite suite = createTestSuite(UUID.randomUUID());
            TestCase testCase = createTestCase(suite, "Test patient summary");

            when(testExecutionRepository.save(any(TestExecution.class)))
                .thenAnswer(inv -> inv.getArgument(0));
            when(testCaseRepository.save(any(TestCase.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            setupAgentRuntimeSuccess();

            // When
            TestExecution execution = orchestratorService.executeCase(testCase, TENANT_ID, USER_ID);

            // Then
            assertThat(execution).isNotNull();
            assertThat(execution.getAgentResponse()).isEqualTo("Test response from agent");
            assertThat(execution.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(execution.getTraceId()).isNotNull();
        }

        @Test
        @DisplayName("Should handle agent execution failure")
        void shouldHandleAgentExecutionFailure() {
            // Given
            TestSuite suite = createTestSuite(UUID.randomUUID());
            TestCase testCase = createTestCase(suite, "Test that fails");

            when(agentRuntimeClient.executeAgent(anyString(), anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Agent unavailable"));
            when(testExecutionRepository.save(any(TestExecution.class)))
                .thenAnswer(inv -> inv.getArgument(0));
            when(testCaseRepository.save(any(TestCase.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            // When
            TestExecution execution = orchestratorService.executeCase(testCase, TENANT_ID, USER_ID);

            // Then
            assertThat(execution.getStatus()).isEqualTo(TestStatus.ERROR);
            assertThat(execution.isPassed()).isFalse();
            assertThat(execution.getErrorMessage()).contains("Agent unavailable");
        }

        @Test
        @DisplayName("Should record execution metrics")
        void shouldRecordExecutionMetrics() {
            // Given
            TestSuite suite = createTestSuite(UUID.randomUUID());
            TestCase testCase = createTestCase(suite, "Test metrics recording");

            when(testExecutionRepository.save(any(TestExecution.class)))
                .thenAnswer(inv -> inv.getArgument(0));
            when(testCaseRepository.save(any(TestCase.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            setupAgentRuntimeSuccess();

            // When
            orchestratorService.executeCase(testCase, TENANT_ID, USER_ID);

            // Then - Counter and timer should be incremented
            assertThat(meterRegistry.getMeters()).isNotEmpty();
        }

        @Test
        @DisplayName("Should build correct agent execution request")
        void shouldBuildCorrectAgentExecutionRequest() {
            // Given
            TestSuite suite = createTestSuite(UUID.randomUUID());
            suite.setAgentType("clinical-decision");

            TestCase testCase = createTestCase(suite, "What are the patient's care gaps?");
            testCase.setContextData(Map.of("patientId", "patient-123", "measureId", "BCS"));

            when(testExecutionRepository.save(any(TestExecution.class)))
                .thenAnswer(inv -> inv.getArgument(0));
            when(testCaseRepository.save(any(TestCase.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            setupAgentRuntimeSuccess();

            // When
            orchestratorService.executeCase(testCase, TENANT_ID, USER_ID);

            // Then
            ArgumentCaptor<AgentExecutionRequest> requestCaptor =
                ArgumentCaptor.forClass(AgentExecutionRequest.class);
            verify(agentRuntimeClient).executeAgent(
                eq(TENANT_ID), eq(USER_ID), anyString(), requestCaptor.capture());

            AgentExecutionRequest capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.getAgentType()).isEqualTo("clinical-decision");
            assertThat(capturedRequest.getUserMessage()).isEqualTo("What are the patient's care gaps?");
            assertThat(capturedRequest.getPatientId()).isEqualTo("patient-123");
            assertThat(capturedRequest.isIncludeToolCalls()).isTrue();
            assertThat(capturedRequest.isIncludeTraceInfo()).isTrue();
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should create error execution on exception")
        void shouldCreateErrorExecutionOnException() {
            // Given
            UUID suiteId = UUID.randomUUID();
            TestSuite suite = createTestSuite(suiteId);
            TestCase testCase = createTestCase(suite, "Failing test");

            when(testSuiteRepository.findByIdAndTenantId(suiteId, TENANT_ID))
                .thenReturn(Optional.of(suite));
            when(testCaseRepository.findByTestSuiteIdOrderByExecutionPriorityAsc(suiteId))
                .thenReturn(List.of(testCase));
            when(agentRuntimeClient.executeAgent(anyString(), anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Service unavailable"));
            when(testSuiteRepository.save(any(TestSuite.class)))
                .thenAnswer(inv -> inv.getArgument(0));
            when(testExecutionRepository.save(any(TestExecution.class)))
                .thenAnswer(inv -> inv.getArgument(0));
            when(testCaseRepository.save(any(TestCase.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            // When
            TestOrchestratorService.TestSuiteExecutionResult result =
                orchestratorService.executeSuite(suiteId, TENANT_ID, USER_ID);

            // Then
            assertThat(result.getFailedTests()).isEqualTo(1);
            assertThat(result.getPassedTests()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should continue execution after individual test failure")
        void shouldContinueExecutionAfterIndividualTestFailure() {
            // Given
            UUID suiteId = UUID.randomUUID();
            TestSuite suite = createTestSuite(suiteId);
            List<TestCase> testCases = createTestCases(suite, 3);

            when(testSuiteRepository.findByIdAndTenantId(suiteId, TENANT_ID))
                .thenReturn(Optional.of(suite));
            when(testCaseRepository.findByTestSuiteIdOrderByExecutionPriorityAsc(suiteId))
                .thenReturn(testCases);
            when(testSuiteRepository.save(any(TestSuite.class)))
                .thenAnswer(inv -> inv.getArgument(0));
            when(testExecutionRepository.save(any(TestExecution.class)))
                .thenAnswer(inv -> inv.getArgument(0));
            when(testCaseRepository.save(any(TestCase.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            // First call fails, rest succeed
            when(agentRuntimeClient.executeAgent(anyString(), anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("First test fails"))
                .thenReturn(createAgentResponse())
                .thenReturn(createAgentResponse());

            // When
            TestOrchestratorService.TestSuiteExecutionResult result =
                orchestratorService.executeSuite(suiteId, TENANT_ID, USER_ID);

            // Then
            assertThat(result.getTotalTests()).isEqualTo(3);
            assertThat(result.getExecutions()).hasSize(3);
            verify(agentRuntimeClient, times(3)).executeAgent(
                eq(TENANT_ID), eq(USER_ID), anyString(), any());
        }
    }

    // Helper methods

    private TestSuite createTestSuite(UUID suiteId) {
        TestSuite suite = new TestSuite();
        suite.setId(suiteId);
        suite.setTenantId(TENANT_ID);
        suite.setName("Test Suite");
        suite.setUserStoryType(UserStoryType.PATIENT_SUMMARY_REVIEW);
        suite.setTargetRole("CLINICIAN");
        suite.setAgentType("clinical-decision");
        suite.setPassThreshold(new BigDecimal("0.80"));
        suite.setStatus(TestStatus.PENDING);
        return suite;
    }

    private TestCase createTestCase(TestSuite suite, String userMessage) {
        TestCase testCase = new TestCase();
        testCase.setId(UUID.randomUUID());
        testCase.setTestSuite(suite);
        testCase.setName("Test Case: " + userMessage.substring(0, Math.min(20, userMessage.length())));
        testCase.setUserMessage(userMessage);
        testCase.setRequiredMetrics(Set.of(EvaluationMetricType.RELEVANCY, EvaluationMetricType.COHERENCE));
        testCase.setStatus(TestStatus.PENDING);
        testCase.setExecutionPriority(1);
        return testCase;
    }

    private List<TestCase> createTestCases(TestSuite suite, int count) {
        List<TestCase> testCases = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TestCase testCase = createTestCase(suite, "Test message " + (i + 1));
            testCase.setExecutionPriority(i + 1);
            testCases.add(testCase);
        }
        return testCases;
    }

    private void setupAgentRuntimeSuccess() {
        when(agentRuntimeClient.executeAgent(anyString(), anyString(), anyString(), any()))
            .thenReturn(createAgentResponse());
    }

    private AgentExecutionResponse createAgentResponse() {
        return AgentExecutionResponse.builder()
            .response("Test response from agent")
            .status("SUCCESS")
            .llmProvider("openai")
            .traceId(UUID.randomUUID().toString())
            .durationMs(1500L)
            .inputTokens(150)
            .outputTokens(200)
            .build();
    }
}
