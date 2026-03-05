package com.healthdata.cqrsquery;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MeasureEvaluationQueryService - Quality measure evaluation queries backed by projections
 */
public class MeasureEvaluationQueryService {
    private final MockMeasureProjectionStore measureStore;
    private final MockCacheStore cacheStore;

    public MeasureEvaluationQueryService(MockMeasureProjectionStore measureStore, MockCacheStore cacheStore) {
        this.measureStore = measureStore;
        this.cacheStore = cacheStore;
    }

    public List<MeasureEvaluationResult> getMeasureScores(String tenantId, String patientId) {
        return measureStore.getByTenantAndPatient(tenantId, patientId).stream()
            .map(e -> new MeasureEvaluationResult(e.getMeasureCode(), e.getStatus(), e.getScore()))
            .toList();
    }

    public MeasureEvaluationResult getMeasure(String tenantId, String patientId, String measureCode) {
        MockMeasureProjectionStore.MeasureEvaluation eval = measureStore.getMeasureEvaluation(tenantId, patientId, measureCode);
        if (eval != null) {
            return new MeasureEvaluationResult(eval.getMeasureCode(), eval.getStatus(), eval.getScore());
        }
        return new MeasureEvaluationResult(measureCode, "UNKNOWN", 0.0f);
    }

    public List<MeasureEvaluationResult> getMeasuresByStatus(String tenantId, String patientId, String status) {
        return measureStore.getByTenantAndPatient(tenantId, patientId).stream()
            .filter(e -> status.equals(e.getStatus()))
            .map(e -> new MeasureEvaluationResult(e.getMeasureCode(), e.getStatus(), e.getScore()))
            .toList();
    }

    public float getMeasureRate(String tenantId, String measureCode) {
        String cacheKey = "measure:" + measureCode + ":" + tenantId;
        Object cached = cacheStore.get(cacheKey);
        if (cached != null) return (float) cached;

        List<MockMeasureProjectionStore.MeasureEvaluation> evals = measureStore.getByTenantAndMeasure(tenantId, measureCode);
        if (evals.isEmpty()) {
            float rate = 0.0f;
            cacheStore.put(cacheKey, rate, 300);
            return rate;
        }
        float rate = (float) evals.stream().mapToDouble(MockMeasureProjectionStore.MeasureEvaluation::getScore).average().orElse(0.0);
        cacheStore.put(cacheKey, rate, 300);
        return rate;
    }

    public float getNumeratorPercentage(String tenantId, String measureCode) {
        List<MockMeasureProjectionStore.MeasureEvaluation> evals = measureStore.getByTenantAndMeasure(tenantId, measureCode);
        if (evals.isEmpty()) return 0.0f;
        long numerator = evals.stream().filter(e -> "MET".equals(e.getStatus()) || "NUMERATOR".equals(e.getStatus())).count();
        return (float) numerator / evals.size();
    }

    public List<MeasureEvaluationResult> getMeasuresSortedByRate(String tenantId) {
        return measureStore.getAllForTenant(tenantId).stream()
            .map(e -> new MeasureEvaluationResult(e.getMeasureCode(), e.getStatus(), e.getScore()))
            .sorted((a, b) -> Float.compare(b.getScore(), a.getScore()))
            .toList();
    }

    public List<MeasureEvaluationResult> getMeasuresBelowTarget(String tenantId, float target) {
        return measureStore.getAllForTenant(tenantId).stream()
            .filter(e -> e.getScore() < target)
            .map(e -> new MeasureEvaluationResult(e.getMeasureCode(), e.getStatus(), e.getScore()))
            .toList();
    }

    public float getMeasureScoreAsOf(String tenantId, String measureCode, LocalDate date) {
        MockMeasureProjectionStore.MeasureEvaluation eval = measureStore.getMeasureEvaluationAtDate(tenantId, measureCode, date);
        return eval != null ? eval.getScore() : 0.0f;
    }
}

class MeasureEvaluationResult {
    private final String measureCode;
    private final String status;
    private final float score;

    MeasureEvaluationResult(String measureCode, String status, float score) {
        this.measureCode = measureCode;
        this.status = status;
        this.score = score;
    }

    String getMeasureCode() { return measureCode; }
    String getStatus() { return status; }
    float getScore() { return score; }
    boolean isCompliant() { return "MET".equals(status); }
    boolean isInNumerator() { return "NUMERATOR".equals(status) || "MET".equals(status); }
    boolean isInDenominator() { return status != null; }
}
