package com.healthdata.cqrsquery;

import java.time.LocalDate;
import java.util.*;

/**
 * In-memory measure projection store for CQRS read model.
 * Placeholder — production would use Spring Data repositories.
 */
public class MockMeasureProjectionStore {
    private final Map<String, MeasureEvaluation> data = new HashMap<>();

    public void addMeasureEvaluation(String tenantId, String patientId, String measureCode, String status, float score) {
        String key = tenantId + ":" + patientId + ":" + measureCode;
        data.put(key, new MeasureEvaluation(measureCode, status, score, tenantId, patientId));
    }

    public void addMeasureEvaluationAtDate(String tenantId, String patientId, String measureCode, String status, float score, LocalDate date) {
        String key = tenantId + ":" + patientId + ":" + measureCode + ":" + date;
        data.put(key, new MeasureEvaluation(measureCode, status, score, tenantId, patientId));
    }

    public MeasureEvaluation getMeasureEvaluation(String tenantId, String patientId, String measureCode) {
        return data.get(tenantId + ":" + patientId + ":" + measureCode);
    }

    public MeasureEvaluation getMeasureEvaluationAtDate(String tenantId, String measureCode, LocalDate date) {
        for (Map.Entry<String, MeasureEvaluation> entry : data.entrySet()) {
            if (entry.getKey().startsWith(tenantId + ":") && entry.getKey().contains(":" + measureCode + ":" + date)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public List<MeasureEvaluation> getByTenantAndPatient(String tenantId, String patientId) {
        List<MeasureEvaluation> results = new ArrayList<>();
        String prefix = tenantId + ":" + patientId + ":";
        for (Map.Entry<String, MeasureEvaluation> entry : data.entrySet()) {
            if (entry.getKey().startsWith(prefix) && !entry.getKey().substring(prefix.length()).contains(":")) {
                results.add(entry.getValue());
            }
        }
        return results;
    }

    public List<MeasureEvaluation> getByTenantAndMeasure(String tenantId, String measureCode) {
        List<MeasureEvaluation> results = new ArrayList<>();
        for (Map.Entry<String, MeasureEvaluation> entry : data.entrySet()) {
            if (entry.getKey().startsWith(tenantId + ":") && entry.getValue().getMeasureCode().equals(measureCode)) {
                // Exclude date-keyed entries
                String[] parts = entry.getKey().split(":");
                if (parts.length == 3) {
                    results.add(entry.getValue());
                }
            }
        }
        return results;
    }

    public Collection<MeasureEvaluation> getAllForTenant(String tenantId) {
        List<MeasureEvaluation> results = new ArrayList<>();
        for (Map.Entry<String, MeasureEvaluation> entry : data.entrySet()) {
            if (entry.getKey().startsWith(tenantId + ":")) {
                results.add(entry.getValue());
            }
        }
        return results;
    }

    public static class MeasureEvaluation {
        private final String measureCode;
        private final String status;
        private final float score;
        private final String tenantId;
        private final String patientId;

        public MeasureEvaluation(String measureCode, String status, float score, String tenantId, String patientId) {
            this.measureCode = measureCode;
            this.status = status;
            this.score = score;
            this.tenantId = tenantId;
            this.patientId = patientId;
        }

        public String getMeasureCode() { return measureCode; }
        public String getStatus() { return status; }
        public float getScore() { return score; }
        public String getTenantId() { return tenantId; }
        public String getPatientId() { return patientId; }
    }
}
