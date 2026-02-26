package com.healthdata.gateway.admin.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ops")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
public class OperationsController {

    private final OperationsService operationsService;
    private final ObjectMapper objectMapper;

    public OperationsController(OperationsService operationsService, ObjectMapper objectMapper) {
        this.operationsService = operationsService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/stack/start")
    public ResponseEntity<RunQueuedResponse> startStack(
        @RequestBody(required = false) IdempotencyRequest request,
        Authentication authentication
    ) {
        UUID runId = operationsService.startStack(actor(authentication), idempotencyKey(request));
        return ResponseEntity.ok(new RunQueuedResponse(runId, "Stack start queued"));
    }

    @PostMapping("/stack/stop")
    public ResponseEntity<RunQueuedResponse> stopStack(
        @RequestBody(required = false) IdempotencyRequest request,
        Authentication authentication
    ) {
        UUID runId = operationsService.stopStack(actor(authentication), idempotencyKey(request));
        return ResponseEntity.ok(new RunQueuedResponse(runId, "Stack stop queued"));
    }

    @PostMapping("/stack/restart")
    public ResponseEntity<RunQueuedResponse> restartStack(
        @RequestBody(required = false) IdempotencyRequest request,
        Authentication authentication
    ) {
        UUID runId = operationsService.restartStack(actor(authentication), idempotencyKey(request));
        return ResponseEntity.ok(new RunQueuedResponse(runId, "Stack restart queued"));
    }

    @PostMapping("/seed/run")
    public ResponseEntity<RunQueuedResponse> runSeed(
        @RequestBody(required = false) SeedRunRequest request,
        Authentication authentication
    ) {
        String profile = request != null ? request.profile() : null;
        String scheduleMode = request != null ? request.scheduleMode() : null;
        String idempotencyKey = request != null ? request.idempotencyKey() : null;
        UUID runId = operationsService.runSeed(actor(authentication), profile, scheduleMode, idempotencyKey);
        return ResponseEntity.ok(new RunQueuedResponse(runId, "Seeding queued"));
    }

    @PostMapping("/validate/run")
    public ResponseEntity<RunQueuedResponse> runValidate(
        @RequestBody(required = false) IdempotencyRequest request,
        Authentication authentication
    ) {
        UUID runId = operationsService.runValidate(actor(authentication), idempotencyKey(request));
        return ResponseEntity.ok(new RunQueuedResponse(runId, "Validation queued"));
    }

    @PostMapping("/runs/{runId}/cancel")
    public ResponseEntity<RunQueuedResponse> cancelRun(@PathVariable UUID runId, Authentication authentication) {
        boolean cancelled = operationsService.cancelRun(runId, actor(authentication));
        if (!cancelled) {
            return ResponseEntity.badRequest().body(new RunQueuedResponse(runId, "Run cannot be cancelled"));
        }
        return ResponseEntity.ok(new RunQueuedResponse(runId, "Cancellation requested"));
    }

    @GetMapping("/runs")
    public ResponseEntity<List<RunSummaryResponse>> listRuns(@RequestParam(name = "limit", defaultValue = "50") int limit) {
        List<RunSummaryResponse> runs = operationsService.listRuns(limit)
            .stream()
            .map(this::toSummary)
            .toList();
        return ResponseEntity.ok(runs);
    }

    @GetMapping("/runs/{runId}")
    public ResponseEntity<RunDetailResponse> getRun(@PathVariable UUID runId) {
        Optional<OperationRun> run = operationsService.getRun(runId);
        if (run.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<StepResponse> steps = operationsService.getRunSteps(runId)
            .stream()
            .map(this::toStep)
            .toList();

        return ResponseEntity.ok(new RunDetailResponse(toSummary(run.get()), steps));
    }

    @GetMapping("/system-status")
    public ResponseEntity<SystemStatusResponse> getSystemStatus() {
        OperationsService.OperationsSummary summary = operationsService.getSummary();
        return ResponseEntity.ok(new SystemStatusResponse(
            summary.runningCount(),
            summary.latestStackStart().map(this::toSummary).orElse(null),
            summary.latestStackStop().map(this::toSummary).orElse(null),
            summary.latestStackRestart().map(this::toSummary).orElse(null),
            summary.latestSeedSmoke().map(this::toSummary).orElse(null),
            summary.latestSeedFull().map(this::toSummary).orElse(null),
            summary.latestValidate().map(this::toSummary).orElse(null)
        ));
    }

    private RunSummaryResponse toSummary(OperationRun run) {
        OperationsService.ValidationScorecard scorecard = operationsService
            .getValidationScorecard(run.getId())
            .orElse(null);

        return new RunSummaryResponse(
            run.getId(),
            run.getOperationType().name(),
            run.getStatus().name(),
            readJson(run.getParametersJson()),
            run.getRequestedBy(),
            run.getRequestedAt(),
            run.getStartedAt(),
            run.getCompletedAt(),
            run.getSummary(),
            run.getExitCode(),
            run.getLogOutput(),
            run.isCancelRequested(),
            scorecard != null ? toScorecard(scorecard) : null
        );
    }

    private StepResponse toStep(OperationRunStep step) {
        return new StepResponse(
            step.getId(),
            step.getStepOrder(),
            step.getStepName(),
            step.getStatus().name(),
            step.getCommandText(),
            step.getMessage(),
            step.getOutput(),
            step.getStartedAt(),
            step.getCompletedAt()
        );
    }

    private JsonNode readJson(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(payload);
        } catch (Exception e) {
            return null;
        }
    }

    private ValidationScorecardResponse toScorecard(OperationsService.ValidationScorecard scorecard) {
        List<ValidationGateResponse> gates = scorecard.gates()
            .stream()
            .map(gate -> new ValidationGateResponse(
                gate.gateKey(),
                gate.gateName(),
                gate.critical(),
                gate.weight(),
                gate.status(),
                gate.actualValue(),
                gate.expectedValue(),
                gate.evidenceText(),
                gate.measuredAt()
            ))
            .toList();

        return new ValidationScorecardResponse(
            scorecard.score(),
            scorecard.grade(),
            scorecard.criticalPass(),
            scorecard.passed(),
            gates,
            scorecard.createdAt()
        );
    }

    private String actor(Authentication authentication) {
        return authentication != null && authentication.getName() != null
            ? authentication.getName()
            : "system";
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<RunQueuedResponse> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(409).body(new RunQueuedResponse(null, ex.getMessage()));
    }

    private String idempotencyKey(IdempotencyRequest request) {
        return request != null ? request.idempotencyKey() : null;
    }

    public record SeedRunRequest(String profile, String scheduleMode, String idempotencyKey) {}
    public record IdempotencyRequest(String idempotencyKey) {}

    public record RunQueuedResponse(UUID runId, String message) {}

    public record RunSummaryResponse(
        UUID id,
        String operationType,
        String status,
        JsonNode parameters,
        String requestedBy,
        Instant requestedAt,
        Instant startedAt,
        Instant completedAt,
        String summary,
        Integer exitCode,
        String logOutput,
        boolean cancelRequested,
        ValidationScorecardResponse validation
    ) {}

    public record StepResponse(
        UUID id,
        int stepOrder,
        String stepName,
        String status,
        String commandText,
        String message,
        String output,
        Instant startedAt,
        Instant completedAt
    ) {}

    public record RunDetailResponse(
        RunSummaryResponse run,
        List<StepResponse> steps
    ) {}

    public record SystemStatusResponse(
        long runningCount,
        RunSummaryResponse latestStackStart,
        RunSummaryResponse latestStackStop,
        RunSummaryResponse latestStackRestart,
        RunSummaryResponse latestSeedSmoke,
        RunSummaryResponse latestSeedFull,
        RunSummaryResponse latestValidate
    ) {}

    public record ValidationScorecardResponse(
        int score,
        String grade,
        boolean criticalPass,
        boolean passed,
        List<ValidationGateResponse> gates,
        Instant createdAt
    ) {}

    public record ValidationGateResponse(
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
}
