package com.healthdata.gateway.admin.operations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class OperationsService {

    private static final int MAX_OUTPUT_CHARS = 120_000;

    private final OperationRunRepository runRepository;
    private final OperationRunStepRepository stepRepository;
    private final OperationValidationRunRepository validationRunRepository;
    private final OperationValidationGateRepository validationGateRepository;
    private final ValidationScoringService validationScoringService;
    private final OperationsProperties properties;
    private final ObjectMapper objectMapper;
    private final LocalShellOperationsCommandExecutor localExecutor;
    private final BridgeOperationsCommandExecutor bridgeExecutor;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    public OperationsService(
        OperationRunRepository runRepository,
        OperationRunStepRepository stepRepository,
        OperationValidationRunRepository validationRunRepository,
        OperationValidationGateRepository validationGateRepository,
        ValidationScoringService validationScoringService,
        OperationsProperties properties,
        ObjectMapper objectMapper,
        LocalShellOperationsCommandExecutor localExecutor,
        BridgeOperationsCommandExecutor bridgeExecutor
    ) {
        this.runRepository = runRepository;
        this.stepRepository = stepRepository;
        this.validationRunRepository = validationRunRepository;
        this.validationGateRepository = validationGateRepository;
        this.validationScoringService = validationScoringService;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.localExecutor = localExecutor;
        this.bridgeExecutor = bridgeExecutor;
    }

    @Transactional
    public UUID startStack(String actor, String idempotencyKey) {
        return queueRun(
            OperationRun.OperationType.STACK_START,
            actor,
            idempotencyKey,
            Map.of(),
            stackOperationTypes(),
            List.of(new CommandStep("Start demo stack", properties.getStackStartCommand()))
        );
    }

    @Transactional
    public UUID stopStack(String actor, String idempotencyKey) {
        return queueRun(
            OperationRun.OperationType.STACK_STOP,
            actor,
            idempotencyKey,
            Map.of(),
            stackOperationTypes(),
            List.of(new CommandStep("Stop demo stack", properties.getStackStopCommand()))
        );
    }

    @Transactional
    public UUID restartStack(String actor, String idempotencyKey) {
        return queueRun(
            OperationRun.OperationType.STACK_RESTART,
            actor,
            idempotencyKey,
            Map.of(),
            stackOperationTypes(),
            List.of(
                new CommandStep("Stop demo stack", properties.getStackStopCommand()),
                new CommandStep("Start demo stack", properties.getStackStartCommand())
            )
        );
    }

    @Transactional
    public UUID runSeed(String actor, String profile, String scheduleMode, String idempotencyKey) {
        String normalizedProfile = "full".equalsIgnoreCase(profile) ? "full" : "smoke";
        String normalizedScheduleMode = normalizeScheduleMode(scheduleMode);

        List<CommandStep> steps = new ArrayList<>();
        if ("full".equals(normalizedProfile)) {
            steps.add(new CommandStep("Seed full demo dataset", properties.getSeedFullCommand()));
        } else {
            steps.add(new CommandStep("Seed smoke demo dataset", properties.getSeedCommand()));
        }

        if (!"none".equals(normalizedScheduleMode)) {
            steps.add(new CommandStep(
                "Seed FHIR schedule data",
                "SEED_SCHEDULE_MODE=" + normalizedScheduleMode + " " + properties.getSeedScheduleCommand()
            ));
        }

        OperationRun.OperationType operationType = "full".equals(normalizedProfile)
            ? OperationRun.OperationType.SEED_FULL
            : OperationRun.OperationType.SEED_SMOKE;

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("profile", normalizedProfile);
        parameters.put("scheduleMode", normalizedScheduleMode);

        return queueRun(
            operationType,
            actor,
            idempotencyKey,
            parameters,
            seedOperationTypes(),
            steps
        );
    }

    @Transactional
    public UUID runValidate(String actor, String idempotencyKey) {
        return queueRun(
            OperationRun.OperationType.VALIDATE,
            actor,
            idempotencyKey,
            Map.of(),
            List.of(OperationRun.OperationType.VALIDATE),
            List.of(new CommandStep("Validate system", properties.getValidateCommand()))
        );
    }

    @Transactional
    public boolean cancelRun(UUID runId, String actor) {
        Optional<OperationRun> maybeRun = runRepository.findById(runId);
        if (maybeRun.isEmpty()) {
            return false;
        }

        OperationRun run = maybeRun.get();
        if (run.getStatus() == OperationRun.RunStatus.SUCCEEDED
            || run.getStatus() == OperationRun.RunStatus.FAILED
            || run.getStatus() == OperationRun.RunStatus.CANCELLED) {
            return false;
        }

        run.setCancelRequested(true);
        run.setSummary("Cancel requested by " + (actor == null || actor.isBlank() ? "system" : actor));
        runRepository.save(run);

        currentExecutor().cancel(runId);
        return true;
    }

    public List<OperationRun> listRuns(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return runRepository.findAllByOrderByRequestedAtDesc(PageRequest.of(0, safeLimit));
    }

    public Optional<OperationRun> getRun(UUID runId) {
        return runRepository.findById(runId);
    }

    public List<OperationRunStep> getRunSteps(UUID runId) {
        return stepRepository.findByRun_IdOrderByStepOrderAscCreatedAtAsc(runId);
    }

    public Optional<ValidationScorecard> getValidationScorecard(UUID runId) {
        Optional<OperationValidationRun> validationRun = validationRunRepository.findByOperationRun_Id(runId);
        if (validationRun.isEmpty()) {
            return Optional.empty();
        }
        OperationValidationRun persisted = validationRun.get();
        List<ValidationGate> gates = validationGateRepository
            .findByValidationRun_IdOrderByMeasuredAtAscGateKeyAsc(persisted.getId())
            .stream()
            .map(g -> new ValidationGate(
                g.getGateKey(),
                g.getGateName(),
                g.isCritical(),
                g.getWeight(),
                g.getStatus(),
                g.getActualValue(),
                g.getExpectedValue(),
                g.getEvidenceText(),
                g.getMeasuredAt()
            ))
            .toList();

        return Optional.of(new ValidationScorecard(
            persisted.getScore(),
            persisted.getGrade(),
            persisted.isCriticalPass(),
            persisted.isPassed(),
            gates,
            persisted.getCreatedAt()
        ));
    }

    public OperationsSummary getSummary() {
        long runningCount = runRepository.countByStatus(OperationRun.RunStatus.RUNNING);
        return new OperationsSummary(
            runningCount,
            latestRun(OperationRun.OperationType.STACK_START),
            latestRun(OperationRun.OperationType.STACK_STOP),
            latestRun(OperationRun.OperationType.STACK_RESTART),
            latestRun(OperationRun.OperationType.SEED_SMOKE),
            latestRun(OperationRun.OperationType.SEED_FULL),
            latestRun(OperationRun.OperationType.VALIDATE)
        );
    }

    private Optional<OperationRun> latestRun(OperationRun.OperationType type) {
        return runRepository.findTopByOperationTypeOrderByRequestedAtDesc(type);
    }

    private String normalizeScheduleMode(String scheduleMode) {
        if (scheduleMode == null || scheduleMode.isBlank()) {
            return "none";
        }
        String normalized = scheduleMode.toLowerCase();
        return switch (normalized) {
            case "none", "appointment-task", "encounter", "both" -> normalized;
            default -> "none";
        };
    }

    private List<OperationRun.OperationType> stackOperationTypes() {
        return List.of(
            OperationRun.OperationType.STACK_START,
            OperationRun.OperationType.STACK_STOP,
            OperationRun.OperationType.STACK_RESTART
        );
    }

    private List<OperationRun.OperationType> seedOperationTypes() {
        return List.of(OperationRun.OperationType.SEED_SMOKE, OperationRun.OperationType.SEED_FULL);
    }

    private UUID queueRun(
        OperationRun.OperationType type,
        String actor,
        String idempotencyKey,
        Map<String, Object> parameters,
        List<OperationRun.OperationType> concurrencyGroup,
        List<CommandStep> steps
    ) {
        String normalizedActor = actor != null && !actor.isBlank() ? actor : "system";

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<OperationRun> existing = runRepository
                .findTopByOperationTypeAndIdempotencyKeyOrderByRequestedAtDesc(type, idempotencyKey);
            if (existing.isPresent()) {
                return existing.get().getId();
            }
        }

        boolean hasRunning = runRepository.existsByStatusAndOperationTypeIn(
            OperationRun.RunStatus.RUNNING,
            concurrencyGroup
        );
        if (hasRunning) {
            throw new IllegalStateException("Another operation in this category is already running.");
        }

        OperationRun run = new OperationRun();
        run.setOperationType(type);
        run.setStatus(OperationRun.RunStatus.QUEUED);
        run.setRequestedBy(normalizedActor);
        run.setParametersJson(asJson(parameters));
        run.setSummary("Queued");
        run.setIdempotencyKey(idempotencyKey);
        run = runRepository.save(run);

        for (int i = 0; i < steps.size(); i++) {
            CommandStep step = steps.get(i);
            OperationRunStep runStep = new OperationRunStep();
            runStep.setRun(run);
            runStep.setStepOrder(i + 1);
            runStep.setStepName(step.name());
            runStep.setStatus(OperationRun.RunStatus.QUEUED);
            runStep.setCommandText(step.command());
            runStep.setMessage("Queued");
            stepRepository.save(runStep);
        }

        UUID runId = run.getId();
        dispatchAsyncAfterCommit(runId, steps);
        return runId;
    }

    protected void executeRun(UUID runId, List<CommandStep> steps) {
        OperationRun run = runRepository.findById(runId).orElseThrow();
        run.setStatus(OperationRun.RunStatus.RUNNING);
        run.setStartedAt(Instant.now());
        run.setSummary("Running");
        runRepository.save(run);

        StringBuilder combinedOutput = new StringBuilder();
        int finalExitCode = 0;

        List<OperationRunStep> persistedSteps = stepRepository.findByRun_IdOrderByStepOrderAscCreatedAtAsc(runId);

        for (int i = 0; i < steps.size(); i++) {
            run = runRepository.findById(runId).orElseThrow();
            if (run.isCancelRequested()) {
                finalExitCode = 130;
                break;
            }

            CommandStep commandStep = steps.get(i);
            OperationRunStep step = persistedSteps.get(i);
            step.setStatus(OperationRun.RunStatus.RUNNING);
            step.setStartedAt(Instant.now());
            step.setMessage("Running");
            stepRepository.save(step);

            OperationsCommandExecutor.CommandResult commandResult = currentExecutor().execute(runId, commandStep.command());
            finalExitCode = commandResult.exitCode();

            step.setOutput(commandResult.output());
            step.setCompletedAt(Instant.now());
            if (commandResult.exitCode() == 0) {
                step.setStatus(OperationRun.RunStatus.SUCCEEDED);
                step.setMessage("Completed");
            } else {
                step.setStatus(OperationRun.RunStatus.FAILED);
                step.setMessage("Failed with exit code " + commandResult.exitCode());
            }
            stepRepository.save(step);

            append(combinedOutput, "## " + commandStep.name() + "\n");
            append(combinedOutput, commandResult.output());
            append(combinedOutput, "\n");

            if (commandResult.exitCode() != 0) {
                break;
            }
        }

        run = runRepository.findById(runId).orElseThrow();
        run.setCompletedAt(Instant.now());
        run.setExitCode(finalExitCode);
        run.setLogOutput(combinedOutput.toString());

        if (run.isCancelRequested()) {
            run.setStatus(OperationRun.RunStatus.CANCELLED);
            run.setSummary("Cancelled");
        } else if (finalExitCode == 0) {
            run.setStatus(OperationRun.RunStatus.SUCCEEDED);
            run.setSummary("Completed successfully");
        } else {
            run.setStatus(OperationRun.RunStatus.FAILED);
            run.setSummary("Failed with exit code " + finalExitCode);
        }
        runRepository.save(run);

        if (run.getOperationType() == OperationRun.OperationType.VALIDATE) {
            persistValidationScorecard(run, persistedSteps, combinedOutput.toString());
        }
    }

    private void persistValidationScorecard(OperationRun run, List<OperationRunStep> persistedSteps, String output) {
        validationRunRepository.findByOperationRun_Id(run.getId()).ifPresent(existing -> {
            validationGateRepository.deleteByValidationRun_Id(existing.getId());
            validationRunRepository.delete(existing);
        });

        ValidationScoringService.Scorecard scorecard = validationScoringService.score(run, persistedSteps, output);
        OperationValidationRun validationRun = new OperationValidationRun();
        validationRun.setOperationRun(run);
        validationRun.setScore(scorecard.score());
        validationRun.setGrade(scorecard.grade());
        validationRun.setCriticalPass(scorecard.criticalPass());
        validationRun.setPassed(scorecard.passed());
        validationRun.setSummaryJson(asJson(Map.of(
            "score", scorecard.score(),
            "grade", scorecard.grade(),
            "criticalPass", scorecard.criticalPass(),
            "passed", scorecard.passed()
        )));
        validationRun = validationRunRepository.save(validationRun);

        for (ValidationScoringService.GateResult gate : scorecard.gates()) {
            OperationValidationGate entity = new OperationValidationGate();
            entity.setValidationRun(validationRun);
            entity.setGateKey(gate.key());
            entity.setGateName(gate.name());
            entity.setCritical(gate.critical());
            entity.setWeight(gate.weight());
            entity.setStatus(gate.passed() ? "PASS" : "FAIL");
            entity.setActualValue(gate.actual());
            entity.setExpectedValue(gate.expected());
            entity.setEvidenceText(gate.evidence());
            entity.setMeasuredAt(gate.measuredAt());
            validationGateRepository.save(entity);
        }
    }

    private OperationsCommandExecutor currentExecutor() {
        if (properties.getExecutionMode() == OperationsProperties.ExecutionMode.BRIDGE) {
            return bridgeExecutor;
        }
        return localExecutor;
    }

    private String asJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private void append(StringBuilder output, String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        if (output.length() >= MAX_OUTPUT_CHARS) {
            return;
        }
        if (output.length() + chunk.length() <= MAX_OUTPUT_CHARS) {
            output.append(chunk);
            return;
        }
        output.append(chunk, 0, MAX_OUTPUT_CHARS - output.length());
    }

    private void dispatchAsyncAfterCommit(UUID runId, List<CommandStep> steps) {
        Runnable runnable = () -> CompletableFuture
            .runAsync(() -> executeRun(runId, steps), executor)
            .whenComplete((unused, throwable) -> {
                if (throwable != null) {
                    log.error("Operation run {} failed asynchronously", runId, throwable);
                }
            });

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    runnable.run();
                }
            });
            return;
        }

        runnable.run();
    }

    private record CommandStep(String name, String command) {}

    public record ValidationGate(
        String gateKey,
        String gateName,
        boolean critical,
        int weight,
        String status,
        String actualValue,
        String expectedValue,
        String evidenceText,
        Instant measuredAt
    ) {}

    public record ValidationScorecard(
        int score,
        String grade,
        boolean criticalPass,
        boolean passed,
        List<ValidationGate> gates,
        Instant createdAt
    ) {}

    public record OperationsSummary(
        long runningCount,
        Optional<OperationRun> latestStackStart,
        Optional<OperationRun> latestStackStop,
        Optional<OperationRun> latestStackRestart,
        Optional<OperationRun> latestSeedSmoke,
        Optional<OperationRun> latestSeedFull,
        Optional<OperationRun> latestValidate
    ) {}
}
