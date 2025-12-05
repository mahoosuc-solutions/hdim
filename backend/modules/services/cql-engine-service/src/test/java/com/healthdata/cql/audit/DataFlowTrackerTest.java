package com.healthdata.cql.audit;

import com.healthdata.cql.event.audit.CqlEvaluationAuditEvent.DataFlowStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for DataFlowTracker.
 *
 * Tests the ThreadLocal-based data flow tracking system that captures
 * step-by-step data movement through CQL evaluations.
 */
@DisplayName("DataFlowTracker Unit Tests")
class DataFlowTrackerTest {

    private DataFlowTracker dataFlowTracker;

    @BeforeEach
    void setUp() {
        dataFlowTracker = new DataFlowTracker();
        // Enable tracking for tests
        ReflectionTestUtils.setField(dataFlowTracker, "trackingEnabled", true);
        ReflectionTestUtils.setField(dataFlowTracker, "maxSteps", 50);
    }

    @Test
    @DisplayName("Should start tracking with evaluation ID")
    void shouldStartTrackingWithEvaluationId() {
        // When
        dataFlowTracker.startTracking("eval-123");

        // Then
        assertThat(dataFlowTracker.isTracking()).isTrue();
        assertThat(dataFlowTracker.getCurrentEvaluationId()).isEqualTo("eval-123");
        assertThat(dataFlowTracker.getSteps()).isEmpty();
    }

    @Test
    @DisplayName("Should record data fetch step with all details")
    void shouldRecordDataFetchStepWithAllDetails() {
        // Given
        dataFlowTracker.startTracking("eval-123");

        // When
        dataFlowTracker.recordStep(
            "Fetch Patient Demographics",
            "DATA_FETCH",
            List.of("Patient"),
            "patientId=patient-456",
            "{\"id\": \"patient-456\", \"age\": 45}",
            "Patient age: 45",
            "Retrieved patient resource for age calculation"
        );

        // Then
        List<DataFlowStep> steps = dataFlowTracker.getSteps();
        assertThat(steps).hasSize(1);

        DataFlowStep step = steps.get(0);
        assertThat(step.getStepNumber()).isEqualTo(1);
        assertThat(step.getStepName()).isEqualTo("Fetch Patient Demographics");
        assertThat(step.getStepType()).isEqualTo("DATA_FETCH");
        assertThat(step.getResourcesAccessed()).containsExactly("Patient");
        assertThat(step.getInputData()).isEqualTo("patientId=patient-456");
        assertThat(step.getOutputData()).contains("patient-456");
        assertThat(step.getDecision()).isEqualTo("Patient age: 45");
        assertThat(step.getReasoning()).isEqualTo("Retrieved patient resource for age calculation");
        assertThat(step.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should record simplified step without resources")
    void shouldRecordSimplifiedStepWithoutResources() {
        // Given
        dataFlowTracker.startTracking("eval-123");

        // When
        dataFlowTracker.recordStep(
            "Evaluate Age Criteria",
            "LOGIC_DECISION",
            "Patient IN Numerator",
            "Age 45 >= 18 (minimum) AND < 75 (maximum)"
        );

        // Then
        List<DataFlowStep> steps = dataFlowTracker.getSteps();
        assertThat(steps).hasSize(1);

        DataFlowStep step = steps.get(0);
        assertThat(step.getStepName()).isEqualTo("Evaluate Age Criteria");
        assertThat(step.getStepType()).isEqualTo("LOGIC_DECISION");
        assertThat(step.getDecision()).isEqualTo("Patient IN Numerator");
        assertThat(step.getReasoning()).contains("Age 45");
        assertThat(step.getResourcesAccessed()).isEmpty();
    }

    @Test
    @DisplayName("Should auto-increment step numbers")
    void shouldAutoIncrementStepNumbers() {
        // Given
        dataFlowTracker.startTracking("eval-123");

        // When
        dataFlowTracker.recordStep("Step 1", "DATA_FETCH", "Result 1", "Reason 1");
        dataFlowTracker.recordStep("Step 2", "EXPRESSION_EVAL", "Result 2", "Reason 2");
        dataFlowTracker.recordStep("Step 3", "LOGIC_DECISION", "Result 3", "Reason 3");

        // Then
        List<DataFlowStep> steps = dataFlowTracker.getSteps();
        assertThat(steps).hasSize(3);
        assertThat(steps.get(0).getStepNumber()).isEqualTo(1);
        assertThat(steps.get(1).getStepNumber()).isEqualTo(2);
        assertThat(steps.get(2).getStepNumber()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should enforce max steps limit")
    void shouldEnforceMaxStepsLimit() {
        // Given
        dataFlowTracker.startTracking("eval-123");
        ReflectionTestUtils.setField(dataFlowTracker, "maxSteps", 3);

        // When - Record 5 steps
        for (int i = 1; i <= 5; i++) {
            dataFlowTracker.recordStep("Step " + i, "DATA_FETCH", "Result", "Reason");
        }

        // Then - Only first 3 steps recorded
        List<DataFlowStep> steps = dataFlowTracker.getSteps();
        assertThat(steps).hasSize(3);
        assertThat(steps.get(0).getStepName()).isEqualTo("Step 1");
        assertThat(steps.get(2).getStepName()).isEqualTo("Step 3");
    }

    @Test
    @DisplayName("Should truncate long strings to prevent memory issues")
    void shouldTruncateLongStringsToPreventMemoryIssues() {
        // Given
        dataFlowTracker.startTracking("eval-123");
        String veryLongString = "x".repeat(1000);

        // When
        dataFlowTracker.recordStep(
            "Long Data Step",
            "DATA_FETCH",
            null,
            veryLongString, // Input data
            veryLongString, // Output data
            veryLongString.substring(0, 300), // Decision
            veryLongString  // Reasoning
        );

        // Then
        DataFlowStep step = dataFlowTracker.getSteps().get(0);
        assertThat(step.getInputData()).hasSize(500); // 500 char limit + ...
        assertThat(step.getOutputData()).hasSize(500);
        assertThat(step.getDecision()).hasSize(200); // 200 char limit
        assertThat(step.getReasoning()).hasSize(500);
    }

    @Test
    @DisplayName("Should clear tracking context")
    void shouldClearTrackingContext() {
        // Given
        dataFlowTracker.startTracking("eval-123");
        dataFlowTracker.recordStep("Step 1", "DATA_FETCH", "Result", "Reason");

        // When
        dataFlowTracker.clearTracking();

        // Then
        assertThat(dataFlowTracker.isTracking()).isFalse();
        assertThat(dataFlowTracker.getCurrentEvaluationId()).isNull();
        assertThat(dataFlowTracker.getSteps()).isEmpty();
    }

    @Test
    @DisplayName("Should handle multiple tracking sessions in same thread")
    void shouldHandleMultipleTrackingSessionsInSameThread() {
        // Session 1
        dataFlowTracker.startTracking("eval-1");
        dataFlowTracker.recordStep("Step 1", "DATA_FETCH", "Result 1", "Reason 1");
        List<DataFlowStep> steps1 = dataFlowTracker.getSteps();
        dataFlowTracker.clearTracking();

        // Session 2
        dataFlowTracker.startTracking("eval-2");
        dataFlowTracker.recordStep("Step A", "LOGIC_DECISION", "Result A", "Reason A");
        dataFlowTracker.recordStep("Step B", "EXPRESSION_EVAL", "Result B", "Reason B");
        List<DataFlowStep> steps2 = dataFlowTracker.getSteps();
        dataFlowTracker.clearTracking();

        // Then
        assertThat(steps1).hasSize(1);
        assertThat(steps1.get(0).getStepName()).isEqualTo("Step 1");

        assertThat(steps2).hasSize(2);
        assertThat(steps2.get(0).getStepName()).isEqualTo("Step A");
        assertThat(steps2.get(1).getStepName()).isEqualTo("Step B");
    }

    @Test
    @DisplayName("Should not record steps when tracking is disabled")
    void shouldNotRecordStepsWhenTrackingIsDisabled() {
        // Given
        ReflectionTestUtils.setField(dataFlowTracker, "trackingEnabled", false);

        // When
        dataFlowTracker.startTracking("eval-123");
        dataFlowTracker.recordStep("Step 1", "DATA_FETCH", "Result", "Reason");

        // Then
        assertThat(dataFlowTracker.isTracking()).isFalse();
        assertThat(dataFlowTracker.getSteps()).isEmpty();
    }

    @Test
    @DisplayName("Should not record step if tracking not started")
    void shouldNotRecordStepIfTrackingNotStarted() {
        // When - Attempt to record without starting tracking
        dataFlowTracker.recordStep("Step 1", "DATA_FETCH", "Result", "Reason");

        // Then
        assertThat(dataFlowTracker.getSteps()).isEmpty();
        assertThat(dataFlowTracker.isTracking()).isFalse();
    }

    @Test
    @DisplayName("Should return immutable copy of steps")
    void shouldReturnImmutableCopyOfSteps() {
        // Given
        dataFlowTracker.startTracking("eval-123");
        dataFlowTracker.recordStep("Step 1", "DATA_FETCH", "Result", "Reason");

        // When
        List<DataFlowStep> steps1 = dataFlowTracker.getSteps();
        dataFlowTracker.recordStep("Step 2", "LOGIC_DECISION", "Result", "Reason");
        List<DataFlowStep> steps2 = dataFlowTracker.getSteps();

        // Then - steps1 should not be affected by subsequent recordings
        assertThat(steps1).hasSize(1);
        assertThat(steps2).hasSize(2);
    }

    @Test
    @DisplayName("Should handle null values gracefully")
    void shouldHandleNullValuesGracefully() {
        // Given
        dataFlowTracker.startTracking("eval-123");

        // When
        dataFlowTracker.recordStep(
            "Null Test",
            "DATA_FETCH",
            null, // resources
            null, // input
            null, // output
            null, // decision
            null  // reasoning
        );

        // Then
        DataFlowStep step = dataFlowTracker.getSteps().get(0);
        assertThat(step.getResourcesAccessed()).isEmpty();
        assertThat(step.getInputData()).isNull();
        assertThat(step.getOutputData()).isNull();
        assertThat(step.getDecision()).isNull();
        assertThat(step.getReasoning()).isNull();
    }

    @Test
    @DisplayName("Should record step duration when endCurrentStep is called")
    void shouldRecordStepDurationWhenEndCurrentStepIsCalled() throws InterruptedException {
        // Given
        dataFlowTracker.startTracking("eval-123");
        dataFlowTracker.recordStep("Timed Step", "EXPRESSION_EVAL", "Result", "Reason");

        // When
        Thread.sleep(10); // Simulate some work
        dataFlowTracker.endCurrentStep();

        // Then
        DataFlowStep step = dataFlowTracker.getSteps().get(0);
        assertThat(step.getDurationMs()).isNotNull();
        assertThat(step.getDurationMs()).isGreaterThanOrEqualTo(10);
    }

    @Test
    @DisplayName("Should handle endCurrentStep without active step")
    void shouldHandleEndCurrentStepWithoutActiveStep() {
        // Given
        dataFlowTracker.startTracking("eval-123");

        // When/Then - Should not throw exception
        assertThatCode(() -> dataFlowTracker.endCurrentStep())
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should be thread-safe with ThreadLocal storage")
    void shouldBeThreadSafeWithThreadLocalStorage() throws InterruptedException {
        // This test verifies that each thread has its own tracking context
        final List<DataFlowStep> thread1Steps = new java.util.ArrayList<>();
        final List<DataFlowStep> thread2Steps = new java.util.ArrayList<>();

        Thread thread1 = new Thread(() -> {
            dataFlowTracker.startTracking("eval-thread1");
            dataFlowTracker.recordStep("Thread 1 Step", "DATA_FETCH", "Result 1", "Reason 1");
            thread1Steps.addAll(dataFlowTracker.getSteps());
            dataFlowTracker.clearTracking();
        });

        Thread thread2 = new Thread(() -> {
            dataFlowTracker.startTracking("eval-thread2");
            dataFlowTracker.recordStep("Thread 2 Step", "LOGIC_DECISION", "Result 2", "Reason 2");
            thread2Steps.addAll(dataFlowTracker.getSteps());
            dataFlowTracker.clearTracking();
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        // Then - Each thread should have its own steps
        assertThat(thread1Steps).hasSize(1);
        assertThat(thread1Steps.get(0).getStepName()).isEqualTo("Thread 1 Step");

        assertThat(thread2Steps).hasSize(1);
        assertThat(thread2Steps.get(0).getStepName()).isEqualTo("Thread 2 Step");
    }
}
