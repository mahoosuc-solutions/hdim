package com.healthdata.notification.domain.model;

/**
 * Status of a notification in its delivery lifecycle.
 */
public enum NotificationStatus {
    PENDING("pending"),
    QUEUED("queued"),
    SENDING("sending"),
    SENT("sent"),
    DELIVERED("delivered"),
    FAILED("failed"),
    CANCELLED("cancelled");

    private final String value;

    NotificationStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static NotificationStatus fromValue(String value) {
        for (NotificationStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown notification status: " + value);
    }

    public boolean isTerminal() {
        return this == SENT || this == DELIVERED || this == FAILED || this == CANCELLED;
    }
}
