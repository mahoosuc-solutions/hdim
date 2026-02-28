package com.healthdata.payer.revenue;

public enum RevenueErrorCode {
    VALIDATION_ERROR,
    AUTHZ_ERROR,
    UPSTREAM_TIMEOUT,
    RETRYABLE_UPSTREAM,
    NON_RETRYABLE_UPSTREAM
}
