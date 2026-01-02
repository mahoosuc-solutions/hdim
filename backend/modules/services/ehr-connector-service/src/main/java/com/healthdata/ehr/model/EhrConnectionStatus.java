package com.healthdata.ehr.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents the health and status of an EHR connection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EhrConnectionStatus {

    /**
     * Connection identifier
     */
    private String connectionId;

    /**
     * Tenant identifier
     */
    private String tenantId;

    /**
     * Vendor type
     */
    private EhrVendorType vendorType;

    /**
     * Connection status
     */
    private Status status;

    /**
     * Last successful connection timestamp
     */
    private LocalDateTime lastSuccessfulConnection;

    /**
     * Last connection attempt timestamp
     */
    private LocalDateTime lastAttempt;

    /**
     * Error message if connection failed
     */
    private String errorMessage;

    /**
     * Number of consecutive failures
     */
    private Integer consecutiveFailures;

    /**
     * Circuit breaker state
     */
    private CircuitBreakerState circuitBreakerState;

    /**
     * Additional status metadata
     */
    private String metadata;

    /**
     * Connection status enum
     */
    public enum Status {
        CONNECTED,
        DISCONNECTED,
        ERROR,
        CONNECTING,
        AUTHENTICATION_FAILED,
        TIMEOUT
    }

    /**
     * Circuit breaker state enum
     */
    public enum CircuitBreakerState {
        CLOSED,
        OPEN,
        HALF_OPEN
    }
}
