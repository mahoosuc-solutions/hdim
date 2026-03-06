package com.healthdata.eventsourcing.intelligence;

/**
 * Human review lifecycle states for generated recommendations.
 */
public enum RecommendationReviewStatus {
    PROPOSED,
    TRIAGED,
    APPROVED,
    REJECTED,
    IMPLEMENTED,
    OUTCOME_OBSERVED
}
