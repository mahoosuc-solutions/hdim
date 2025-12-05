package com.healthdata.predictive.model;

/**
 * Risk tier classification for patient risk stratification
 */
public enum RiskTier {
    LOW(0, 25, "Low risk - routine monitoring"),
    MODERATE(25, 50, "Moderate risk - enhanced monitoring"),
    HIGH(50, 75, "High risk - proactive intervention"),
    VERY_HIGH(75, 100, "Very high risk - intensive case management");

    private final int minScore;
    private final int maxScore;
    private final String description;

    RiskTier(int minScore, int maxScore, String description) {
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.description = description;
    }

    public int getMinScore() {
        return minScore;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get risk tier from score (0-100 scale)
     */
    public static RiskTier fromScore(double score) {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("Score must be between 0 and 100, got: " + score);
        }

        for (RiskTier tier : values()) {
            if (score >= tier.minScore && score < tier.maxScore) {
                return tier;
            }
        }

        // Handle score exactly 100
        return VERY_HIGH;
    }
}
