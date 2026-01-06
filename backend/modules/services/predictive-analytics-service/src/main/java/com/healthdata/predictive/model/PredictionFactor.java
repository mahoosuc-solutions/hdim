package com.healthdata.predictive.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Prediction Factor
 *
 * Represents a factor contributing to the care gap prediction with its
 * weighted contribution to the overall risk score.
 *
 * Standard factor weights (Issue #157):
 * - Historical Pattern: 40%
 * - Appointment Adherence: 25%
 * - Medication Refills: 20%
 * - Similar Patient Behavior: 15%
 *
 * Issue #157: Implement Predictive Care Gap Detection
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionFactor {

    /**
     * Factor type identifier
     */
    private FactorType factorType;

    /**
     * Human-readable name of the factor
     */
    private String name;

    /**
     * Description of how this factor contributes to the prediction
     */
    private String description;

    /**
     * Weight of this factor in the overall prediction (0.0 - 1.0)
     */
    private double weight;

    /**
     * Raw score for this factor before weighting (0.0 - 1.0)
     */
    private double rawScore;

    /**
     * Weighted contribution to the overall risk score (weight * rawScore)
     */
    private double contribution;

    /**
     * Additional context about this factor
     */
    private String context;

    /**
     * Whether this factor is flagged as concerning
     */
    private boolean isConcerning;

    /**
     * Threshold at which this factor becomes concerning
     */
    private double concernThreshold;

    /**
     * Factor types for care gap prediction
     */
    public enum FactorType {
        /**
         * Historical pattern analysis
         * Weight: 40%
         * Analyzes previous care gaps, compliance history, and patterns
         */
        HISTORICAL_PATTERN(0.40, "Historical Pattern Analysis"),

        /**
         * Appointment adherence tracking
         * Weight: 25%
         * Tracks no-shows, cancellations, and rescheduling patterns
         */
        APPOINTMENT_ADHERENCE(0.25, "Appointment Adherence"),

        /**
         * Medication refill behavior
         * Weight: 20%
         * Monitors prescription fills, adherence, and gaps
         */
        MEDICATION_REFILLS(0.20, "Medication Refill Behavior"),

        /**
         * Similar patient behavior analysis
         * Weight: 15%
         * Compares to cohort of similar patients
         */
        SIMILAR_PATIENT_BEHAVIOR(0.15, "Similar Patient Behavior");

        private final double defaultWeight;
        private final String displayName;

        FactorType(double defaultWeight, String displayName) {
            this.defaultWeight = defaultWeight;
            this.displayName = displayName;
        }

        public double getDefaultWeight() {
            return defaultWeight;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Create a prediction factor with standard weight
     */
    public static PredictionFactor create(FactorType type, double rawScore, String context) {
        double weight = type.getDefaultWeight();
        double contribution = weight * rawScore;

        return PredictionFactor.builder()
            .factorType(type)
            .name(type.getDisplayName())
            .weight(weight)
            .rawScore(rawScore)
            .contribution(contribution)
            .context(context)
            .isConcerning(rawScore >= 0.7)
            .concernThreshold(0.7)
            .build();
    }

    /**
     * Create a concerning historical pattern factor
     */
    public static PredictionFactor historicalPattern(double score, int previousGaps, int daysSinceLast) {
        String context = String.format(
            "Patient has %d previous gap(s) for this measure. Last compliance was %d days ago.",
            previousGaps, daysSinceLast
        );
        String description = "Based on patient's historical compliance patterns and previous care gaps.";

        PredictionFactor factor = create(FactorType.HISTORICAL_PATTERN, score, context);
        factor.setDescription(description);
        return factor;
    }

    /**
     * Create an appointment adherence factor
     */
    public static PredictionFactor appointmentAdherence(double score, int noShows, int cancellations) {
        String context = String.format(
            "Patient has %d no-show(s) and %d cancellation(s) in the past 12 months.",
            noShows, cancellations
        );
        String description = "Based on appointment attendance, no-shows, and cancellation patterns.";

        PredictionFactor factor = create(FactorType.APPOINTMENT_ADHERENCE, score, context);
        factor.setDescription(description);
        return factor;
    }

    /**
     * Create a medication refill factor
     */
    public static PredictionFactor medicationRefills(double score, double adherenceRate, int missedRefills) {
        String context = String.format(
            "Medication adherence rate: %.0f%%. %d missed refill(s) in the past 6 months.",
            adherenceRate * 100, missedRefills
        );
        String description = "Based on prescription refill patterns and medication adherence.";

        PredictionFactor factor = create(FactorType.MEDICATION_REFILLS, score, context);
        factor.setDescription(description);
        return factor;
    }

    /**
     * Create a similar patient behavior factor
     */
    public static PredictionFactor similarPatientBehavior(double score, int cohortSize, double cohortGapRate) {
        String context = String.format(
            "Compared to %d similar patients with %.0f%% gap rate.",
            cohortSize, cohortGapRate * 100
        );
        String description = "Based on outcomes of demographically and clinically similar patients.";

        PredictionFactor factor = create(FactorType.SIMILAR_PATIENT_BEHAVIOR, score, context);
        factor.setDescription(description);
        return factor;
    }
}
