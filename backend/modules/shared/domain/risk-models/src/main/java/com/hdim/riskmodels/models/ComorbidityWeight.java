package com.hdim.riskmodels.models;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents the weight assigned to a specific comorbidity condition.
 * Used in various risk scoring algorithms like Charlson and Elixhauser.
 * Thread-safe and immutable.
 */
public final class ComorbidityWeight implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String conditionCode;
    private final String conditionName;
    private final double weight;
    private final String system; // e.g., "Charlson", "Elixhauser", "HCC"
    private final String version;

    private ComorbidityWeight(Builder builder) {
        this.conditionCode = Objects.requireNonNull(builder.conditionCode, "Condition code cannot be null");
        this.conditionName = Objects.requireNonNull(builder.conditionName, "Condition name cannot be null");
        this.weight = builder.weight;
        this.system = Objects.requireNonNull(builder.system, "System cannot be null");
        this.version = builder.version;
    }

    public String getConditionCode() {
        return conditionCode;
    }

    public String getConditionName() {
        return conditionName;
    }

    public double getWeight() {
        return weight;
    }

    public String getSystem() {
        return system;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComorbidityWeight that = (ComorbidityWeight) o;
        return Double.compare(that.weight, weight) == 0 &&
                conditionCode.equals(that.conditionCode) &&
                system.equals(that.system) &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(conditionCode, weight, system, version);
    }

    @Override
    public String toString() {
        return "ComorbidityWeight{" +
                "conditionCode='" + conditionCode + '\'' +
                ", conditionName='" + conditionName + '\'' +
                ", weight=" + weight +
                ", system='" + system + '\'' +
                ", version='" + version + '\'' +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String conditionCode;
        private String conditionName;
        private double weight;
        private String system;
        private String version;

        public Builder conditionCode(String conditionCode) {
            this.conditionCode = conditionCode;
            return this;
        }

        public Builder conditionName(String conditionName) {
            this.conditionName = conditionName;
            return this;
        }

        public Builder weight(double weight) {
            this.weight = weight;
            return this;
        }

        public Builder system(String system) {
            this.system = system;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public ComorbidityWeight build() {
            return new ComorbidityWeight(this);
        }
    }
}
