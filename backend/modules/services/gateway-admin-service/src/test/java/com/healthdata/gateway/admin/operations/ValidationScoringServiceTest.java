package com.healthdata.gateway.admin.operations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ValidationScoringService.
 * Pure logic class with no external dependencies - no mocks needed.
 */
@DisplayName("Validation Scoring Service Tests")
@Tag("unit")
class ValidationScoringServiceTest {

    private ValidationScoringService scoringService;

    @BeforeEach
    void setUp() {
        scoringService = new ValidationScoringService();
    }

    private OperationRun createRun(int exitCode, OperationRun.RunStatus status) {
        OperationRun run = new OperationRun();
        run.setId(UUID.randomUUID());
        run.setExitCode(exitCode);
        run.setStatus(status);
        run.setOperationType(OperationRun.OperationType.VALIDATE);
        run.setRequestedBy("test");
        return run;
    }

    @Nested
    @DisplayName("Grade Calculation")
    class GradeCalculation {

        @Test
        @DisplayName("Should return grade A when all gates pass")
        void shouldReturnGradeA_WhenAllGatesPass() {
            // Given
            OperationRun run = createRun(0, OperationRun.RunStatus.SUCCEEDED);
            String output = "all services healthy, seed completed, api checks ok";

            // When
            ValidationScoringService.Scorecard scorecard = scoringService.score(
                    run, Collections.emptyList(), output);

            // Then
            assertThat(scorecard.score()).isEqualTo(100);
            assertThat(scorecard.grade()).isEqualTo("A");
            assertThat(scorecard.criticalPass()).isTrue();
            assertThat(scorecard.passed()).isTrue();
        }

        @Test
        @DisplayName("Should return grade F when all gates fail")
        void shouldReturnGradeF_WhenAllGatesFail() {
            // Given - exitCode=1 fails validate_script; empty output fails health/seed/api
            // log_cleanliness passes because empty string has no fatal signals
            OperationRun run = createRun(1, OperationRun.RunStatus.FAILED);
            String output = "";

            // When
            ValidationScoringService.Scorecard scorecard = scoringService.score(
                    run, Collections.emptyList(), output);

            // Then - only log_cleanliness (10 points) passes
            assertThat(scorecard.score()).isEqualTo(10);
            assertThat(scorecard.grade()).isEqualTo("F");
            assertThat(scorecard.criticalPass()).isFalse();
            assertThat(scorecard.passed()).isFalse();
        }

        @Test
        @DisplayName("Should set passed false when critical gate fails even with high score")
        void shouldSetPassedFalse_WhenCriticalGateFails() {
            // Given - exitCode=1 fails validate_script (critical), but output has all other signals
            OperationRun run = createRun(1, OperationRun.RunStatus.FAILED);
            String output = "healthy seed ok";

            // When
            ValidationScoringService.Scorecard scorecard = scoringService.score(
                    run, Collections.emptyList(), output);

            // Then - validate_script fails (0), stack_health (25), seed (25), api (15), log (10) = 75
            assertThat(scorecard.score()).isEqualTo(75);
            assertThat(scorecard.grade()).isEqualTo("C");
            assertThat(scorecard.criticalPass()).isFalse();
            assertThat(scorecard.passed()).isFalse();
        }

        @Test
        @DisplayName("Should set passed false when score below 85 even with all critical gates passing")
        void shouldSetPassedFalse_WhenScoreBelow85() {
            // Given - all critical gates pass, but api smoke and log cleanliness fail
            // output has "fatal" which fails log_cleanliness and lacks api signals
            OperationRun run = createRun(0, OperationRun.RunStatus.SUCCEEDED);
            String output = "healthy seed fatal";

            // When
            ValidationScoringService.Scorecard scorecard = scoringService.score(
                    run, Collections.emptyList(), output);

            // Then - validate(25) + stack_health(25) + seed(25) + api(0, no "ok"/"passed"/"http 200") + log(0, "fatal") = 75
            assertThat(scorecard.score()).isEqualTo(75);
            assertThat(scorecard.grade()).isEqualTo("C");
            assertThat(scorecard.criticalPass()).isTrue();
            assertThat(scorecard.passed()).isFalse();
        }
    }

    @Nested
    @DisplayName("Gate Evaluation")
    class GateEvaluation {

        @Test
        @DisplayName("Should fail validate_script gate when exit code is non-zero")
        void shouldFailValidateScriptGate_WhenExitCodeNonZero() {
            // Given
            OperationRun run = createRun(1, OperationRun.RunStatus.FAILED);
            String output = "healthy seed ok";

            // When
            ValidationScoringService.Scorecard scorecard = scoringService.score(
                    run, Collections.emptyList(), output);

            // Then
            ValidationScoringService.GateResult validateGate = findGate(scorecard.gates(), "validate_script");
            assertThat(validateGate.passed()).isFalse();
            assertThat(validateGate.critical()).isTrue();
            assertThat(validateGate.weight()).isEqualTo(25);
        }

        @Test
        @DisplayName("Should fail stack_health gate when no healthy signals present")
        void shouldFailStackHealthGate_WhenNoHealthySignals() {
            // Given - output has seed and api signals but no "healthy"/"up "/"running"
            OperationRun run = createRun(0, OperationRun.RunStatus.SUCCEEDED);
            String output = "seed ok complete";

            // When
            ValidationScoringService.Scorecard scorecard = scoringService.score(
                    run, Collections.emptyList(), output);

            // Then
            ValidationScoringService.GateResult healthGate = findGate(scorecard.gates(), "stack_health");
            assertThat(healthGate.passed()).isFalse();
            assertThat(healthGate.critical()).isTrue();
        }

        @Test
        @DisplayName("Should fail seed_baseline gate when no seed signals present")
        void shouldFailSeedBaselineGate_WhenNoSeedSignals() {
            // Given - output has healthy and api signals but no "seed"/"patients generated"/"care gaps"
            OperationRun run = createRun(0, OperationRun.RunStatus.SUCCEEDED);
            String output = "healthy ok complete";

            // When
            ValidationScoringService.Scorecard scorecard = scoringService.score(
                    run, Collections.emptyList(), output);

            // Then
            ValidationScoringService.GateResult seedGate = findGate(scorecard.gates(), "seed_baseline");
            assertThat(seedGate.passed()).isFalse();
            assertThat(seedGate.critical()).isTrue();
        }

        @Test
        @DisplayName("Should pass demo_api_smoke gate when output contains ok")
        void shouldPassDemoApiSmokeGate_WhenOutputContainsOk() {
            // Given
            OperationRun run = createRun(0, OperationRun.RunStatus.SUCCEEDED);
            String output = "healthy seed ok";

            // When
            ValidationScoringService.Scorecard scorecard = scoringService.score(
                    run, Collections.emptyList(), output);

            // Then
            ValidationScoringService.GateResult apiGate = findGate(scorecard.gates(), "demo_api_smoke");
            assertThat(apiGate.passed()).isTrue();
            assertThat(apiGate.critical()).isFalse();
            assertThat(apiGate.weight()).isEqualTo(15);
        }

        @Test
        @DisplayName("Should fail log_cleanliness gate when fatal signal found")
        void shouldFailLogCleanlinessGate_WhenFatalFound() {
            // Given
            OperationRun run = createRun(0, OperationRun.RunStatus.SUCCEEDED);
            String output = "healthy seed ok fatal error detected";

            // When
            ValidationScoringService.Scorecard scorecard = scoringService.score(
                    run, Collections.emptyList(), output);

            // Then
            ValidationScoringService.GateResult logGate = findGate(scorecard.gates(), "log_cleanliness");
            assertThat(logGate.passed()).isFalse();
            assertThat(logGate.critical()).isFalse();
            assertThat(logGate.weight()).isEqualTo(10);
        }

        @Test
        @DisplayName("Should handle null output without NPE")
        void shouldHandleNullOutput() {
            // Given
            OperationRun run = createRun(0, OperationRun.RunStatus.SUCCEEDED);

            // When
            ValidationScoringService.Scorecard scorecard = scoringService.score(
                    run, Collections.emptyList(), null);

            // Then - validate_script passes (exitCode=0, SUCCEEDED), all signal gates fail (empty), log passes (no fatal)
            assertThat(scorecard).isNotNull();
            ValidationScoringService.GateResult validateGate = findGate(scorecard.gates(), "validate_script");
            assertThat(validateGate.passed()).isTrue();

            ValidationScoringService.GateResult healthGate = findGate(scorecard.gates(), "stack_health");
            assertThat(healthGate.passed()).isFalse();

            ValidationScoringService.GateResult seedGate = findGate(scorecard.gates(), "seed_baseline");
            assertThat(seedGate.passed()).isFalse();

            ValidationScoringService.GateResult apiGate = findGate(scorecard.gates(), "demo_api_smoke");
            assertThat(apiGate.passed()).isFalse();

            ValidationScoringService.GateResult logGate = findGate(scorecard.gates(), "log_cleanliness");
            assertThat(logGate.passed()).isTrue();

            // Score: 25 (validate) + 0 + 0 + 0 + 10 (log) = 35
            assertThat(scorecard.score()).isEqualTo(35);
        }
    }

    private ValidationScoringService.GateResult findGate(
            List<ValidationScoringService.GateResult> gates, String key) {
        return gates.stream()
                .filter(g -> g.key().equals(key))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Gate not found: " + key));
    }
}
