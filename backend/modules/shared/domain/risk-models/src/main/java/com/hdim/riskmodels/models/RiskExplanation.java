package com.hdim.riskmodels.models;

import java.io.Serializable;
import java.util.Objects;

/**
 * Explains individual risk factors that contribute to the overall risk score.
 * Provides transparency into why a patient has a specific risk level.
 * Thread-safe and immutable.
 */
public final class RiskExplanation implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String factor;
    private final String description;
    private final double contribution;
    private final String evidenceCode; // ICD-10, CPT, etc.
    private final String evidenceSystem;

    private RiskExplanation(Builder builder) {
        this.factor = Objects.requireNonNull(builder.factor, "Factor cannot be null");
        this.description = builder.description;
        this.contribution = builder.contribution;
        this.evidenceCode = builder.evidenceCode;
        this.evidenceSystem = builder.evidenceSystem;
    }

    public String getFactor() {
        return factor;
    }

    public String getDescription() {
        return description;
    }

    public double getContribution() {
        return contribution;
    }

    public String getEvidenceCode() {
        return evidenceCode;
    }

    public String getEvidenceSystem() {
        return evidenceSystem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RiskExplanation that = (RiskExplanation) o;
        return Double.compare(that.contribution, contribution) == 0 &&
                factor.equals(that.factor) &&
                Objects.equals(evidenceCode, that.evidenceCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(factor, contribution, evidenceCode);
    }

    @Override
    public String toString() {
        return "RiskExplanation{" +
                "factor='" + factor + '\'' +
                ", description='" + description + '\'' +
                ", contribution=" + contribution +
                ", evidenceCode='" + evidenceCode + '\'' +
                ", evidenceSystem='" + evidenceSystem + '\'' +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String factor;
        private String description;
        private double contribution;
        private String evidenceCode;
        private String evidenceSystem;

        public Builder factor(String factor) {
            this.factor = factor;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder contribution(double contribution) {
            this.contribution = contribution;
            return this;
        }

        public Builder evidenceCode(String evidenceCode) {
            this.evidenceCode = evidenceCode;
            return this;
        }

        public Builder evidenceSystem(String evidenceSystem) {
            this.evidenceSystem = evidenceSystem;
            return this;
        }

        public RiskExplanation build() {
            return new RiskExplanation(this);
        }
    }
}
