package com.healthdata.gateway.admin.operations;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ValidationScoringService {

    public Scorecard score(OperationRun run, List<OperationRunStep> steps, String combinedOutput) {
        String output = combinedOutput == null ? "" : combinedOutput;
        String normalized = output.toLowerCase(Locale.ROOT);
        boolean runSucceeded = run.getExitCode() != null && run.getExitCode() == 0
            && run.getStatus() == OperationRun.RunStatus.SUCCEEDED;

        List<GateResult> gates = new ArrayList<>();
        gates.add(gate("validate_script", "Validate Script Exit", true, 25, runSucceeded,
            String.valueOf(run.getExitCode()), "0", "Exit code from validate run"));
        gates.add(gate("stack_health", "Core Services Healthy", true, 25,
            containsHealthySignals(normalized),
            actualFromSignals(normalized, "healthy"), "healthy services", "Checked validation output"));
        gates.add(gate("seed_baseline", "Baseline Seed Evidence", true, 25,
            containsSeedSignals(normalized),
            actualFromSignals(normalized, "seed evidence"), "seed completed", "Checked validation output"));
        gates.add(gate("demo_api_smoke", "Demo API Smoke", false, 15,
            containsApiSignals(normalized),
            actualFromSignals(normalized, "api checks"), "api smoke passing", "Checked validation output"));
        gates.add(gate("log_cleanliness", "Error Log Cleanliness", false, 10,
            !containsFatalSignals(normalized),
            containsFatalSignals(normalized) ? "fatal/error markers found" : "no fatal markers",
            "no fatal markers", "Scanned validation output"));

        int score = gates.stream().filter(GateResult::passed).mapToInt(GateResult::weight).sum();
        boolean criticalPass = gates.stream().filter(GateResult::critical).allMatch(GateResult::passed);
        boolean passed = criticalPass && score >= 85;

        return new Scorecard(score, grade(score), criticalPass, passed, gates, Instant.now());
    }

    private GateResult gate(
        String key,
        String name,
        boolean critical,
        int weight,
        boolean passed,
        String actual,
        String expected,
        String evidence
    ) {
        return new GateResult(key, name, critical, weight, passed, actual, expected, evidence, Instant.now());
    }

    private String grade(int score) {
        if (score >= 90) return "A";
        if (score >= 85) return "B";
        if (score >= 75) return "C";
        if (score >= 65) return "D";
        return "F";
    }

    private boolean containsHealthySignals(String normalized) {
        return normalized.contains("healthy")
            || normalized.contains("up ")
            || normalized.contains("running");
    }

    private boolean containsSeedSignals(String normalized) {
        return normalized.contains("seed")
            || normalized.contains("patients generated")
            || normalized.contains("care gaps");
    }

    private boolean containsApiSignals(String normalized) {
        return normalized.contains("http 200")
            || normalized.contains("passed")
            || normalized.contains("ok");
    }

    private boolean containsFatalSignals(String normalized) {
        return normalized.contains("fatal")
            || normalized.contains("exception")
            || normalized.contains("connection refused");
    }

    private String actualFromSignals(String normalized, String fallback) {
        return normalized.isBlank() ? "no output captured" : fallback;
    }

    public record GateResult(
        String key,
        String name,
        boolean critical,
        int weight,
        boolean passed,
        String actual,
        String expected,
        String evidence,
        Instant measuredAt
    ) {}

    public record Scorecard(
        int score,
        String grade,
        boolean criticalPass,
        boolean passed,
        List<GateResult> gates,
        Instant createdAt
    ) {}
}
