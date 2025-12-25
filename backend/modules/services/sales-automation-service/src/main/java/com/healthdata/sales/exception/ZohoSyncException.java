package com.healthdata.sales.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when Zoho CRM sync fails
 */
public class ZohoSyncException extends SalesException {

    public ZohoSyncException(String message) {
        super("Zoho sync failed: " + message, HttpStatus.SERVICE_UNAVAILABLE, "ZOHO_SYNC_FAILED");
    }

    public ZohoSyncException(String message, Throwable cause) {
        super("Zoho sync failed: " + message, cause);
    }
}
