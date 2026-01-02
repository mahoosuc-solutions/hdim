package com.hdim.riskmodels.models;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Tracks risk scores over time to identify trends and changes.
 * Useful for monitoring patient risk trajectory and intervention effectiveness.
 * Thread-safe and immutable.
 */
public final class RiskTrend implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String indexName;
    private final String patientId;
    private final List<DataPoint> dataPoints;

    private RiskTrend(Builder builder) {
        this.indexName = Objects.requireNonNull(builder.indexName, "Index name cannot be null");
        this.patientId = Objects.requireNonNull(builder.patientId, "Patient ID cannot be null");
        this.dataPoints = Collections.unmodifiableList(new ArrayList<>(builder.dataPoints));
    }

    public String getIndexName() {
        return indexName;
    }

    public String getPatientId() {
        return patientId;
    }

    public List<DataPoint> getDataPoints() {
        return dataPoints;
    }

    /**
     * Gets the most recent risk score.
     */
    public Double getCurrentScore() {
        if (dataPoints.isEmpty()) {
            return null;
        }
        return dataPoints.get(dataPoints.size() - 1).getScore();
    }

    /**
     * Gets the earliest risk score.
     */
    public Double getBaselineScore() {
        if (dataPoints.isEmpty()) {
            return null;
        }
        return dataPoints.get(0).getScore();
    }

    /**
     * Calculates the change in risk score from baseline to current.
     */
    public Double getScoreChange() {
        Double baseline = getBaselineScore();
        Double current = getCurrentScore();
        if (baseline == null || current == null) {
            return null;
        }
        return current - baseline;
    }

    /**
     * Calculates the percentage change in risk score.
     */
    public Double getPercentageChange() {
        Double baseline = getBaselineScore();
        Double change = getScoreChange();
        if (baseline == null || change == null || baseline == 0) {
            return null;
        }
        return (change / baseline) * 100.0;
    }

    /**
     * Returns true if the risk is trending upward.
     */
    public boolean isTrendingUp() {
        Double change = getScoreChange();
        return change != null && change > 0;
    }

    /**
     * Returns true if the risk is trending downward.
     */
    public boolean isTrendingDown() {
        Double change = getScoreChange();
        return change != null && change < 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RiskTrend riskTrend = (RiskTrend) o;
        return indexName.equals(riskTrend.indexName) &&
                patientId.equals(riskTrend.patientId) &&
                dataPoints.equals(riskTrend.dataPoints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(indexName, patientId, dataPoints);
    }

    @Override
    public String toString() {
        return "RiskTrend{" +
                "indexName='" + indexName + '\'' +
                ", patientId='" + patientId + '\'' +
                ", dataPoints=" + dataPoints.size() + " points" +
                ", currentScore=" + getCurrentScore() +
                ", change=" + getScoreChange() +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Represents a single data point in the risk trend.
     */
    public static final class DataPoint implements Serializable, Comparable<DataPoint> {
        private static final long serialVersionUID = 1L;

        private final Instant timestamp;
        private final double score;
        private final String interpretation;

        public DataPoint(Instant timestamp, double score, String interpretation) {
            this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
            this.score = score;
            this.interpretation = interpretation;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public double getScore() {
            return score;
        }

        public String getInterpretation() {
            return interpretation;
        }

        @Override
        public int compareTo(DataPoint other) {
            return this.timestamp.compareTo(other.timestamp);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DataPoint dataPoint = (DataPoint) o;
            return Double.compare(dataPoint.score, score) == 0 &&
                    timestamp.equals(dataPoint.timestamp);
        }

        @Override
        public int hashCode() {
            return Objects.hash(timestamp, score);
        }

        @Override
        public String toString() {
            return "DataPoint{" +
                    "timestamp=" + timestamp +
                    ", score=" + score +
                    ", interpretation='" + interpretation + '\'' +
                    '}';
        }
    }

    public static class Builder {
        private String indexName;
        private String patientId;
        private List<DataPoint> dataPoints = new ArrayList<>();

        public Builder indexName(String indexName) {
            this.indexName = indexName;
            return this;
        }

        public Builder patientId(String patientId) {
            this.patientId = patientId;
            return this;
        }

        public Builder dataPoints(List<DataPoint> dataPoints) {
            this.dataPoints = new ArrayList<>(dataPoints);
            // Sort by timestamp
            Collections.sort(this.dataPoints);
            return this;
        }

        public Builder addDataPoint(Instant timestamp, double score, String interpretation) {
            this.dataPoints.add(new DataPoint(timestamp, score, interpretation));
            // Sort by timestamp
            Collections.sort(this.dataPoints);
            return this;
        }

        public Builder addDataPoint(DataPoint dataPoint) {
            this.dataPoints.add(dataPoint);
            // Sort by timestamp
            Collections.sort(this.dataPoints);
            return this;
        }

        public RiskTrend build() {
            return new RiskTrend(this);
        }
    }
}
