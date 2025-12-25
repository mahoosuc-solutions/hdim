package com.healthdata.sales.entity;

import lombok.Getter;

/**
 * Opportunity stage in the sales pipeline
 */
@Getter
public enum OpportunityStage {
    DISCOVERY(10, 1),
    DEMO(30, 2),
    PROPOSAL(50, 3),
    NEGOTIATION(70, 4),
    CONTRACT(90, 5),
    CLOSED_WON(100, 6),
    CLOSED_LOST(0, 6);

    private final int defaultProbability;
    private final int order;

    OpportunityStage(int defaultProbability, int order) {
        this.defaultProbability = defaultProbability;
        this.order = order;
    }

    public boolean isOpen() {
        return this != CLOSED_WON && this != CLOSED_LOST;
    }

    public boolean isClosed() {
        return this == CLOSED_WON || this == CLOSED_LOST;
    }

    public boolean isWon() {
        return this == CLOSED_WON;
    }
}
