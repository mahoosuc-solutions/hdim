package com.healthdata.payer.revenue;

public enum RevenueClaimState {
    DRAFT,
    PENDING_SUBMIT,
    SUBMITTED,
    ACKNOWLEDGED,
    REJECTED,
    PARTIALLY_PAID,
    PAID,
    DENIED,
    UNDER_REVIEW
}
