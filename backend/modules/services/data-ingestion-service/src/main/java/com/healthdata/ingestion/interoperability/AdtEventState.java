package com.healthdata.ingestion.interoperability;

public enum AdtEventState {
    RECEIVED,
    NORMALIZED,
    ROUTED,
    ACKNOWLEDGED,
    REJECTED,
    PARTIAL_ROUTE_FAILURE
}
