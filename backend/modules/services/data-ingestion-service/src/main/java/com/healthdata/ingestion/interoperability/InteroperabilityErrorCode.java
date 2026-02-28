package com.healthdata.ingestion.interoperability;

public enum InteroperabilityErrorCode {
    VALIDATION_ERROR,
    AUTHZ_ERROR,
    PATIENT_MATCH_FAILED,
    UNSUPPORTED_EVENT_TYPE,
    RETRYABLE_UPSTREAM,
    NON_RETRYABLE_UPSTREAM
}
