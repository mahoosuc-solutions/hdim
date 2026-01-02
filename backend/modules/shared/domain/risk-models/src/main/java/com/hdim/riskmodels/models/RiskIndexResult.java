package com.hdim.riskmodels.models;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Standardized result from any risk index calculation.
 * Contains the score, interpretation, and explanation of risk factors.
 * Thread-safe and immutable.
 */
public final class RiskIndexResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String indexName;
    private final double score;
    private final String interpretation; // e.g., "Low Risk", "Medium Risk", "High Risk"
    private final List<RiskExplanation> explanations;
    private final Instant calculatedAt;
    private final String patientId;
    private final String version;

    private RiskIndexResult(Builder builder) {
        this.indexName = Objects.requireNonNull(builder.indexName, "Index name cannot be null");
        this.score = builder.score;
        this.interpretation = builder.interpretation;
        this.explanations = Collections.unmodifiableList(new ArrayList<>(builder.explanations));
        this.calculatedAt = Objects.requireNonNull(builder.calculatedAt, "Calculated time cannot be null");
        this.patientId = builder.patientId;
        this.version = builder.version;
    }

    public String getIndexName() {
        return indexName;
    }

    public double getScore() {
        return score;
    }

    public String getInterpretation() {
        return interpretation;
    }

    public List<RiskExplanation> getExplanations() {
        return explanations;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getVersion() {
        return version;
    }

    /**
     * Returns true if the result indicates high risk (typically score >= threshold).
     */
    public boolean isHighRisk(double threshold) {
        return score >= threshold;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RiskIndexResult that = (RiskIndexResult) o;
        return Double.compare(that.score, score) == 0 &&
                indexName.equals(that.indexName) &&
                calculatedAt.equals(that.calculatedAt) &&
                Objects.equals(patientId, that.patientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(indexName, score, calculatedAt, patientId);
    }

    @Override
    public String toString() {
        return "RiskIndexResult{" +
                "indexName='" + indexName + '\'' +
                ", score=" + score +
                ", interpretation='" + interpretation + '\'' +
                ", explanations=" + explanations.size() + " items" +
                ", calculatedAt=" + calculatedAt +
                ", patientId='" + patientId + '\'' +
                ", version='" + version + '\'' +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String indexName;
        private double score;
        private String interpretation;
        private List<RiskExplanation> explanations = new ArrayList<>();
        private Instant calculatedAt = Instant.now();
        private String patientId;
        private String version;

        public Builder indexName(String indexName) {
            this.indexName = indexName;
            return this;
        }

        public Builder score(double score) {
            this.score = score;
            return this;
        }

        public Builder interpretation(String interpretation) {
            this.interpretation = interpretation;
            return this;
        }

        public Builder explanations(List<RiskExplanation> explanations) {
            this.explanations = new ArrayList<>(explanations);
            return this;
        }

        public Builder addExplanation(RiskExplanation explanation) {
            this.explanations.add(explanation);
            return this;
        }

        public Builder calculatedAt(Instant calculatedAt) {
            this.calculatedAt = calculatedAt;
            return this;
        }

        public Builder patientId(String patientId) {
            this.patientId = patientId;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public RiskIndexResult build() {
            return new RiskIndexResult(this);
        }
    }
}
