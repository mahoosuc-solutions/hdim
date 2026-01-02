package com.healthdata.audit.models;

/**
 * HIPAA-compliant audit outcomes.
 * Based on FHIR AuditEvent.outcome codes.
 */
public enum AuditOutcome {
    SUCCESS(0, "Success"),
    MINOR_FAILURE(4, "Minor failure"),
    SERIOUS_FAILURE(8, "Serious failure"),
    MAJOR_FAILURE(12, "Major failure");

    private final int code;
    private final String display;

    AuditOutcome(int code, String display) {
        this.code = code;
        this.display = display;
    }

    public int getCode() {
        return code;
    }

    public String getDisplay() {
        return display;
    }

    public static AuditOutcome fromHttpStatus(int httpStatus) {
        if (httpStatus >= 200 && httpStatus < 300) {
            return SUCCESS;
        } else if (httpStatus >= 400 && httpStatus < 500) {
            return MINOR_FAILURE;
        } else if (httpStatus >= 500) {
            return SERIOUS_FAILURE;
        }
        return MAJOR_FAILURE;
    }
}
