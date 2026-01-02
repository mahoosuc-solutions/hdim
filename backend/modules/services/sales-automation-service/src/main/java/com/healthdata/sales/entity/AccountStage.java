package com.healthdata.sales.entity;

/**
 * Account stage in the sales lifecycle
 */
public enum AccountStage {
    PROSPECT,
    QUALIFIED,
    DEMO_SCHEDULED,
    DEMO_COMPLETED,
    PROPOSAL,
    NEGOTIATION,
    CLOSED_WON,
    CLOSED_LOST,
    CUSTOMER,
    CHURNED
}
