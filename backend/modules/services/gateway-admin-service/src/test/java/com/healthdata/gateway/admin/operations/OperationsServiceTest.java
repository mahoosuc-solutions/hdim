package com.healthdata.gateway.admin.operations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OperationsService.
 * Tests queueing, seed, cancel, and query operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operations Service Tests")
@Tag("unit")
class OperationsServiceTest {

    @Mock
    private OperationRunRepository runRepository;

    @Mock
    private OperationRunStepRepository stepRepository;

    @Mock
    private OperationValidationRunRepository validationRunRepository;

    @Mock
    private OperationValidationGateRepository validationGateRepository;

    @Mock
    private ValidationScoringService validationScoringService;

    @Mock
    private OperationsProperties properties;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private LocalShellOperationsCommandExecutor localExecutor;

    @Mock
    private BridgeOperationsCommandExecutor bridgeExecutor;

    @Captor
    private ArgumentCaptor<OperationRun> runCaptor;

    @Captor
    private ArgumentCaptor<OperationRunStep> stepCaptor;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    private OperationsService service;

    private static final String ACTOR = "admin@test.com";
    private static final String IDEMPOTENCY_KEY = "idem-key-1";
    private static final UUID RUN_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() throws JsonProcessingException {
        // Configure properties with sensible defaults
        lenient().when(properties.getExecutionMode()).thenReturn(OperationsProperties.ExecutionMode.LOCAL);
        lenient().when(properties.getStackStartCommand()).thenReturn("docker compose up -d");
        lenient().when(properties.getStackStopCommand()).thenReturn("docker compose down");
        lenient().when(properties.getSeedCommand()).thenReturn("./scripts/seed-smoke.sh");
        lenient().when(properties.getSeedFullCommand()).thenReturn("./scripts/seed-full.sh");
        lenient().when(properties.getSeedScheduleCommand()).thenReturn("./scripts/seed-schedule.sh");
        lenient().when(properties.getValidateCommand()).thenReturn("./validate-system.sh");

        // ObjectMapper returns "{}" for any writeValueAsString call
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // Local executor returns success by default
        lenient().when(localExecutor.execute(any(), any()))
            .thenReturn(new OperationsCommandExecutor.CommandResult(0, "ok"));

        service = new OperationsService(
            runRepository,
            stepRepository,
            validationRunRepository,
            validationGateRepository,
            validationScoringService,
            properties,
            objectMapper,
            localExecutor,
            bridgeExecutor
        );
    }

    /**
     * Helper: configures runRepository mocks so that queueRun + executeRun
     * can run synchronously to completion without NPEs.
     * Returns the run entity that save() will return.
     */
    private OperationRun configureSaveAndExecuteMocks() {
        OperationRun savedRun = new OperationRun();
        savedRun.setId(RUN_ID);
        savedRun.setStatus(OperationRun.RunStatus.QUEUED);
        savedRun.setSummary("Queued");

        // save returns the entity with an ID
        when(runRepository.save(any(OperationRun.class))).thenAnswer(inv -> {
            OperationRun arg = inv.getArgument(0);
            if (arg.getId() == null) {
                arg.setId(RUN_ID);
            }
            return arg;
        });

        // findById is called by executeRun multiple times
        lenient().when(runRepository.findById(any(UUID.class))).thenAnswer(inv -> {
            OperationRun run = new OperationRun();
            run.setId(inv.getArgument(0));
            run.setStatus(OperationRun.RunStatus.QUEUED);
            run.setCancelRequested(false);
            return Optional.of(run);
        });

        // stepRepository returns steps for executeRun
        lenient().when(stepRepository.findByRun_IdOrderByStepOrderAscCreatedAtAsc(any())).thenAnswer(inv -> {
            OperationRunStep step = new OperationRunStep();
            step.setId(UUID.randomUUID());
            step.setStepOrder(1);
            step.setStepName("Test step");
            step.setStatus(OperationRun.RunStatus.QUEUED);
            return List.of(step);
        });

        // stepRepository.save returns the step
        lenient().when(stepRepository.save(any(OperationRunStep.class))).thenAnswer(inv -> inv.getArgument(0));

        return savedRun;
    }

    /**
     * Helper: configures mocks for restart (2 steps).
     */
    private void configureSaveAndExecuteMocksForTwoSteps() {
        when(runRepository.save(any(OperationRun.class))).thenAnswer(inv -> {
            OperationRun arg = inv.getArgument(0);
            if (arg.getId() == null) {
                arg.setId(RUN_ID);
            }
            return arg;
        });

        lenient().when(runRepository.findById(any(UUID.class))).thenAnswer(inv -> {
            OperationRun run = new OperationRun();
            run.setId(inv.getArgument(0));
            run.setStatus(OperationRun.RunStatus.QUEUED);
            run.setCancelRequested(false);
            return Optional.of(run);
        });

        lenient().when(stepRepository.findByRun_IdOrderByStepOrderAscCreatedAtAsc(any())).thenAnswer(inv -> {
            OperationRunStep step1 = new OperationRunStep();
            step1.setId(UUID.randomUUID());
            step1.setStepOrder(1);
            step1.setStepName("Step 1");
            step1.setStatus(OperationRun.RunStatus.QUEUED);
            OperationRunStep step2 = new OperationRunStep();
            step2.setId(UUID.randomUUID());
            step2.setStepOrder(2);
            step2.setStepName("Step 2");
            step2.setStatus(OperationRun.RunStatus.QUEUED);
            return List.of(step1, step2);
        });

        lenient().when(stepRepository.save(any(OperationRunStep.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    @DisplayName("Queueing Tests")
    class QueueingTests {

        @Test
        @DisplayName("Should queue stack start when no concurrent running")
        void shouldQueueStackStart_WhenNoConcurrentRunning() {
            // Given
            when(runRepository.existsByStatusAndOperationTypeIn(
                eq(OperationRun.RunStatus.RUNNING), anyList())).thenReturn(false);
            when(runRepository.findTopByOperationTypeAndIdempotencyKeyOrderByRequestedAtDesc(
                any(), any())).thenReturn(Optional.empty());
            configureSaveAndExecuteMocks();

            // When
            UUID result = service.startStack(ACTOR, IDEMPOTENCY_KEY);

            // Then
            assertThat(result).isEqualTo(RUN_ID);
            verify(runRepository, atLeastOnce()).save(runCaptor.capture());
            OperationRun firstSave = runCaptor.getAllValues().get(0);
            assertThat(firstSave.getOperationType()).isEqualTo(OperationRun.OperationType.STACK_START);
            assertThat(firstSave.getStatus()).isEqualTo(OperationRun.RunStatus.QUEUED);
            assertThat(firstSave.getRequestedBy()).isEqualTo(ACTOR);

            // Verify at least 1 step was saved
            verify(stepRepository, atLeastOnce()).save(any(OperationRunStep.class));
        }

        @Test
        @DisplayName("Should queue stack stop when no concurrent running")
        void shouldQueueStackStop_WhenNoConcurrentRunning() {
            // Given
            when(runRepository.existsByStatusAndOperationTypeIn(
                eq(OperationRun.RunStatus.RUNNING), anyList())).thenReturn(false);
            when(runRepository.findTopByOperationTypeAndIdempotencyKeyOrderByRequestedAtDesc(
                any(), any())).thenReturn(Optional.empty());
            configureSaveAndExecuteMocks();

            // When
            UUID result = service.stopStack(ACTOR, IDEMPOTENCY_KEY);

            // Then
            assertThat(result).isEqualTo(RUN_ID);
            verify(runRepository, atLeastOnce()).save(runCaptor.capture());
            OperationRun firstSave = runCaptor.getAllValues().get(0);
            assertThat(firstSave.getOperationType()).isEqualTo(OperationRun.OperationType.STACK_STOP);
            assertThat(firstSave.getStatus()).isEqualTo(OperationRun.RunStatus.QUEUED);
        }

        @Test
        @DisplayName("Should queue stack restart with two steps")
        void shouldQueueStackRestart_WithTwoSteps() {
            // Given
            when(runRepository.existsByStatusAndOperationTypeIn(
                eq(OperationRun.RunStatus.RUNNING), anyList())).thenReturn(false);
            when(runRepository.findTopByOperationTypeAndIdempotencyKeyOrderByRequestedAtDesc(
                any(), any())).thenReturn(Optional.empty());
            configureSaveAndExecuteMocksForTwoSteps();

            // When
            UUID result = service.restartStack(ACTOR, IDEMPOTENCY_KEY);

            // Then
            assertThat(result).isEqualTo(RUN_ID);
            verify(runRepository, atLeastOnce()).save(runCaptor.capture());
            OperationRun firstSave = runCaptor.getAllValues().get(0);
            assertThat(firstSave.getOperationType()).isEqualTo(OperationRun.OperationType.STACK_RESTART);

            // Restart creates 2 steps (stop + start) in queueRun, plus executeRun saves
            verify(stepRepository, atLeast(2)).save(any(OperationRunStep.class));
        }

        @Test
        @DisplayName("Should throw IllegalStateException when concurrent stack running")
        void shouldThrowIllegalState_WhenConcurrentStackRunning() {
            // Given
            when(runRepository.existsByStatusAndOperationTypeIn(
                eq(OperationRun.RunStatus.RUNNING), anyList())).thenReturn(true);
            when(runRepository.findTopByOperationTypeAndIdempotencyKeyOrderByRequestedAtDesc(
                any(), any())).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> service.startStack(ACTOR, IDEMPOTENCY_KEY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already running");
        }

        @Test
        @DisplayName("Should return existing run ID when idempotency key matches")
        void shouldReturnExistingRunId_WhenIdempotencyKeyMatches() {
            // Given
            UUID existingId = UUID.randomUUID();
            OperationRun existingRun = new OperationRun();
            existingRun.setId(existingId);

            when(runRepository.findTopByOperationTypeAndIdempotencyKeyOrderByRequestedAtDesc(
                eq(OperationRun.OperationType.STACK_START), eq(IDEMPOTENCY_KEY)))
                .thenReturn(Optional.of(existingRun));

            // When
            UUID result = service.startStack(ACTOR, IDEMPOTENCY_KEY);

            // Then
            assertThat(result).isEqualTo(existingId);
            verify(runRepository, never()).save(any(OperationRun.class));
        }

        @Test
        @DisplayName("Should create new run when idempotency key is null")
        void shouldCreateNewRun_WhenIdempotencyKeyIsNull() {
            // Given
            when(runRepository.existsByStatusAndOperationTypeIn(
                eq(OperationRun.RunStatus.RUNNING), anyList())).thenReturn(false);
            configureSaveAndExecuteMocks();

            // When
            UUID result = service.startStack(ACTOR, null);

            // Then
            assertThat(result).isEqualTo(RUN_ID);
            // findTopByOperationTypeAndIdempotencyKeyOrderByRequestedAtDesc should NOT be called
            verify(runRepository, never())
                .findTopByOperationTypeAndIdempotencyKeyOrderByRequestedAtDesc(any(), any());
            verify(runRepository, atLeastOnce()).save(any(OperationRun.class));
        }

        @Test
        @DisplayName("Should use system actor when actor is null")
        void shouldUseSystemActor_WhenActorIsNull() {
            // Given
            when(runRepository.existsByStatusAndOperationTypeIn(
                eq(OperationRun.RunStatus.RUNNING), anyList())).thenReturn(false);
            configureSaveAndExecuteMocks();

            // When
            service.startStack(null, null);

            // Then
            verify(runRepository, atLeastOnce()).save(runCaptor.capture());
            OperationRun firstSave = runCaptor.getAllValues().get(0);
            assertThat(firstSave.getRequestedBy()).isEqualTo("system");
        }

        @Test
        @DisplayName("Should use system actor when actor is blank")
        void shouldUseSystemActor_WhenActorIsBlank() {
            // Given
            when(runRepository.existsByStatusAndOperationTypeIn(
                eq(OperationRun.RunStatus.RUNNING), anyList())).thenReturn(false);
            configureSaveAndExecuteMocks();

            // When
            service.startStack("  ", null);

            // Then
            verify(runRepository, atLeastOnce()).save(runCaptor.capture());
            OperationRun firstSave = runCaptor.getAllValues().get(0);
            assertThat(firstSave.getRequestedBy()).isEqualTo("system");
        }

        @Test
        @DisplayName("Should set idempotency key on queued run")
        void shouldSetIdempotencyKey_OnQueuedRun() {
            // Given
            when(runRepository.existsByStatusAndOperationTypeIn(
                eq(OperationRun.RunStatus.RUNNING), anyList())).thenReturn(false);
            when(runRepository.findTopByOperationTypeAndIdempotencyKeyOrderByRequestedAtDesc(
                any(), any())).thenReturn(Optional.empty());
            configureSaveAndExecuteMocks();

            // When
            service.startStack(ACTOR, IDEMPOTENCY_KEY);

            // Then
            verify(runRepository, atLeastOnce()).save(runCaptor.capture());
            OperationRun firstSave = runCaptor.getAllValues().get(0);
            assertThat(firstSave.getIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
        }
    }

    @Nested
    @DisplayName("Seed Tests")
    class SeedTests {

        @Test
        @DisplayName("Should queue seed smoke when profile is null")
        void shouldQueueSeedSmoke_WhenProfileIsNull() {
            // Given
            when(runRepository.existsByStatusAndOperationTypeIn(
                eq(OperationRun.RunStatus.RUNNING), anyList())).thenReturn(false);
            configureSaveAndExecuteMocks();

            // When
            UUID result = service.runSeed(ACTOR, null, "none", null);

            // Then
            assertThat(result).isEqualTo(RUN_ID);
            verify(runRepository, atLeastOnce()).save(runCaptor.capture());
            OperationRun firstSave = runCaptor.getAllValues().get(0);
            assertThat(firstSave.getOperationType()).isEqualTo(OperationRun.OperationType.SEED_SMOKE);
        }

        @Test
        @DisplayName("Should queue seed full when profile is full")
        void shouldQueueSeedFull_WhenProfileIsFull() {
            // Given
            when(runRepository.existsByStatusAndOperationTypeIn(
                eq(OperationRun.RunStatus.RUNNING), anyList())).thenReturn(false);
            configureSaveAndExecuteMocks();

            // When
            UUID result = service.runSeed(ACTOR, "full", "none", null);

            // Then
            assertThat(result).isEqualTo(RUN_ID);
            verify(runRepository, atLeastOnce()).save(runCaptor.capture());
            OperationRun firstSave = runCaptor.getAllValues().get(0);
            assertThat(firstSave.getOperationType()).isEqualTo(OperationRun.OperationType.SEED_FULL);
        }

        @Test
        @DisplayName("Should add schedule step when schedule mode is encounter")
        void shouldAddScheduleStep_WhenScheduleModeIsEncounter() {
            // Given
            when(runRepository.existsByStatusAndOperationTypeIn(
                eq(OperationRun.RunStatus.RUNNING), anyList())).thenReturn(false);
            configureSaveAndExecuteMocksForTwoSteps();

            // When
            service.runSeed(ACTOR, null, "encounter", null);

            // Then
            // 2 steps in queueRun (seed + schedule), plus executeRun saves
            verify(stepRepository, atLeast(2)).save(any(OperationRunStep.class));
        }

        @Test
        @DisplayName("Should skip schedule step when schedule mode is none")
        void shouldSkipScheduleStep_WhenScheduleModeIsNone() {
            // Given
            when(runRepository.existsByStatusAndOperationTypeIn(
                eq(OperationRun.RunStatus.RUNNING), anyList())).thenReturn(false);
            configureSaveAndExecuteMocks();

            // When
            service.runSeed(ACTOR, null, "none", null);

            // Then
            // Only 1 step created in queueRun, executeRun also saves steps
            verify(stepRepository, atLeastOnce()).save(stepCaptor.capture());
            // The first step(s) saved in queueRun should have "Seed smoke demo dataset" as name
            OperationRunStep firstStep = stepCaptor.getAllValues().get(0);
            assertThat(firstStep.getStepName()).isEqualTo("Seed smoke demo dataset");
        }
    }

    @Nested
    @DisplayName("Cancel Tests")
    class CancelTests {

        @Test
        @DisplayName("Should return false when run not found")
        void shouldReturnFalse_WhenRunNotFound() {
            // Given
            when(runRepository.findById(RUN_ID)).thenReturn(Optional.empty());

            // When
            boolean result = service.cancelRun(RUN_ID, ACTOR);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when run already succeeded")
        void shouldReturnFalse_WhenRunAlreadySucceeded() {
            // Given
            OperationRun run = new OperationRun();
            run.setId(RUN_ID);
            run.setStatus(OperationRun.RunStatus.SUCCEEDED);
            when(runRepository.findById(RUN_ID)).thenReturn(Optional.of(run));

            // When
            boolean result = service.cancelRun(RUN_ID, ACTOR);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when run already failed")
        void shouldReturnFalse_WhenRunAlreadyFailed() {
            // Given
            OperationRun run = new OperationRun();
            run.setId(RUN_ID);
            run.setStatus(OperationRun.RunStatus.FAILED);
            when(runRepository.findById(RUN_ID)).thenReturn(Optional.of(run));

            // When
            boolean result = service.cancelRun(RUN_ID, ACTOR);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when run already cancelled")
        void shouldReturnFalse_WhenRunAlreadyCancelled() {
            // Given
            OperationRun run = new OperationRun();
            run.setId(RUN_ID);
            run.setStatus(OperationRun.RunStatus.CANCELLED);
            when(runRepository.findById(RUN_ID)).thenReturn(Optional.of(run));

            // When
            boolean result = service.cancelRun(RUN_ID, ACTOR);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should set cancel requested when run is queued")
        void shouldSetCancelRequested_WhenRunIsQueued() {
            // Given
            OperationRun run = new OperationRun();
            run.setId(RUN_ID);
            run.setStatus(OperationRun.RunStatus.QUEUED);
            when(runRepository.findById(RUN_ID)).thenReturn(Optional.of(run));

            // When
            boolean result = service.cancelRun(RUN_ID, ACTOR);

            // Then
            assertThat(result).isTrue();
            verify(runRepository).save(runCaptor.capture());
            OperationRun saved = runCaptor.getValue();
            assertThat(saved.isCancelRequested()).isTrue();
            verify(localExecutor).cancel(RUN_ID);
        }

        @Test
        @DisplayName("Should set cancel requested when run is running")
        void shouldSetCancelRequested_WhenRunIsRunning() {
            // Given
            OperationRun run = new OperationRun();
            run.setId(RUN_ID);
            run.setStatus(OperationRun.RunStatus.RUNNING);
            when(runRepository.findById(RUN_ID)).thenReturn(Optional.of(run));

            // When
            boolean result = service.cancelRun(RUN_ID, ACTOR);

            // Then
            assertThat(result).isTrue();
            verify(runRepository).save(runCaptor.capture());
            assertThat(runCaptor.getValue().isCancelRequested()).isTrue();
            verify(localExecutor).cancel(RUN_ID);
        }

        @Test
        @DisplayName("Should use summary with actor name when actor provided")
        void shouldUseSummaryWithActorName_WhenActorProvided() {
            // Given
            OperationRun run = new OperationRun();
            run.setId(RUN_ID);
            run.setStatus(OperationRun.RunStatus.QUEUED);
            when(runRepository.findById(RUN_ID)).thenReturn(Optional.of(run));

            // When
            service.cancelRun(RUN_ID, ACTOR);

            // Then
            verify(runRepository).save(runCaptor.capture());
            assertThat(runCaptor.getValue().getSummary()).contains(ACTOR);
        }

        @Test
        @DisplayName("Should use system in summary when actor is null")
        void shouldUseSystemInSummary_WhenActorIsNull() {
            // Given
            OperationRun run = new OperationRun();
            run.setId(RUN_ID);
            run.setStatus(OperationRun.RunStatus.QUEUED);
            when(runRepository.findById(RUN_ID)).thenReturn(Optional.of(run));

            // When
            service.cancelRun(RUN_ID, null);

            // Then
            verify(runRepository).save(runCaptor.capture());
            assertThat(runCaptor.getValue().getSummary()).contains("system");
        }
    }

    @Nested
    @DisplayName("Query Tests")
    class QueryTests {

        @Test
        @DisplayName("Should clamp limit when exceeds max")
        void shouldClampLimit_WhenExceedsMax() {
            // Given
            when(runRepository.findAllByOrderByRequestedAtDesc(any(Pageable.class)))
                .thenReturn(List.of());

            // When
            service.listRuns(500);

            // Then
            verify(runRepository).findAllByOrderByRequestedAtDesc(pageableCaptor.capture());
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(200);
        }

        @Test
        @DisplayName("Should clamp limit when below minimum")
        void shouldClampLimit_WhenBelowMinimum() {
            // Given
            when(runRepository.findAllByOrderByRequestedAtDesc(any(Pageable.class)))
                .thenReturn(List.of());

            // When
            service.listRuns(-5);

            // Then
            verify(runRepository).findAllByOrderByRequestedAtDesc(pageableCaptor.capture());
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return scorecard when validation run exists")
        void shouldReturnScorecard_WhenValidationRunExists() {
            // Given
            UUID validationRunId = UUID.randomUUID();
            OperationValidationRun valRun = new OperationValidationRun();
            // Use reflection to set the ID since there is no setter
            try {
                var field = OperationValidationRun.class.getDeclaredField("id");
                field.setAccessible(true);
                field.set(valRun, validationRunId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            valRun.setScore(95);
            valRun.setGrade("A");
            valRun.setCriticalPass(true);
            valRun.setPassed(true);

            when(validationRunRepository.findByOperationRun_Id(RUN_ID))
                .thenReturn(Optional.of(valRun));

            OperationValidationGate gate = new OperationValidationGate();
            gate.setGateKey("validate_script");
            gate.setGateName("Validate Script Exit");
            gate.setCritical(true);
            gate.setWeight(25);
            gate.setStatus("PASS");
            gate.setActualValue("0");
            gate.setExpectedValue("0");
            gate.setEvidenceText("Exit code from validate run");
            gate.setMeasuredAt(Instant.now());

            when(validationGateRepository.findByValidationRun_IdOrderByMeasuredAtAscGateKeyAsc(validationRunId))
                .thenReturn(List.of(gate));

            // When
            Optional<OperationsService.ValidationScorecard> result = service.getValidationScorecard(RUN_ID);

            // Then
            assertThat(result).isPresent();
            OperationsService.ValidationScorecard scorecard = result.get();
            assertThat(scorecard.score()).isEqualTo(95);
            assertThat(scorecard.grade()).isEqualTo("A");
            assertThat(scorecard.criticalPass()).isTrue();
            assertThat(scorecard.passed()).isTrue();
            assertThat(scorecard.gates()).hasSize(1);
            assertThat(scorecard.gates().get(0).gateKey()).isEqualTo("validate_script");
        }

        @Test
        @DisplayName("Should return empty when no validation run exists")
        void shouldReturnEmpty_WhenNoValidationRunExists() {
            // Given
            when(validationRunRepository.findByOperationRun_Id(RUN_ID))
                .thenReturn(Optional.empty());

            // When
            Optional<OperationsService.ValidationScorecard> result = service.getValidationScorecard(RUN_ID);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return summary with all operation types")
        void shouldReturnSummaryWithAllOperationTypes() {
            // Given
            when(runRepository.countByStatus(OperationRun.RunStatus.RUNNING)).thenReturn(2L);

            OperationRun stackStart = new OperationRun();
            stackStart.setId(UUID.randomUUID());
            stackStart.setOperationType(OperationRun.OperationType.STACK_START);
            when(runRepository.findTopByOperationTypeOrderByRequestedAtDesc(OperationRun.OperationType.STACK_START))
                .thenReturn(Optional.of(stackStart));

            OperationRun stackStop = new OperationRun();
            stackStop.setId(UUID.randomUUID());
            stackStop.setOperationType(OperationRun.OperationType.STACK_STOP);
            when(runRepository.findTopByOperationTypeOrderByRequestedAtDesc(OperationRun.OperationType.STACK_STOP))
                .thenReturn(Optional.of(stackStop));

            when(runRepository.findTopByOperationTypeOrderByRequestedAtDesc(OperationRun.OperationType.STACK_RESTART))
                .thenReturn(Optional.empty());
            when(runRepository.findTopByOperationTypeOrderByRequestedAtDesc(OperationRun.OperationType.SEED_SMOKE))
                .thenReturn(Optional.empty());
            when(runRepository.findTopByOperationTypeOrderByRequestedAtDesc(OperationRun.OperationType.SEED_FULL))
                .thenReturn(Optional.empty());
            when(runRepository.findTopByOperationTypeOrderByRequestedAtDesc(OperationRun.OperationType.VALIDATE))
                .thenReturn(Optional.empty());

            // When
            OperationsService.OperationsSummary summary = service.getSummary();

            // Then
            assertThat(summary.runningCount()).isEqualTo(2L);
            assertThat(summary.latestStackStart()).isPresent();
            assertThat(summary.latestStackStop()).isPresent();
            assertThat(summary.latestStackRestart()).isEmpty();
            assertThat(summary.latestSeedSmoke()).isEmpty();
            assertThat(summary.latestSeedFull()).isEmpty();
            assertThat(summary.latestValidate()).isEmpty();
        }

        @Test
        @DisplayName("Should delegate getRun to repository findById")
        void shouldDelegateGetRunToRepository() {
            // Given
            OperationRun run = new OperationRun();
            run.setId(RUN_ID);
            when(runRepository.findById(RUN_ID)).thenReturn(Optional.of(run));

            // When
            Optional<OperationRun> result = service.getRun(RUN_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(RUN_ID);
        }

        @Test
        @DisplayName("Should delegate getRunSteps to step repository")
        void shouldDelegateGetRunStepsToRepository() {
            // Given
            OperationRunStep step = new OperationRunStep();
            step.setStepName("Test step");
            when(stepRepository.findByRun_IdOrderByStepOrderAscCreatedAtAsc(RUN_ID))
                .thenReturn(List.of(step));

            // When
            List<OperationRunStep> result = service.getRunSteps(RUN_ID);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStepName()).isEqualTo("Test step");
        }
    }
}
