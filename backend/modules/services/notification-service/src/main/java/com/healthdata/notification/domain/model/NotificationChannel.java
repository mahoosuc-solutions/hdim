package com.healthdata.notification.domain.model;

/**
 * Supported notification delivery channels.
 */
public enum NotificationChannel {
    EMAIL("email"),
    SMS("sms"),
    PUSH("push"),
    IN_APP("in_app");

    private final String value;

    NotificationChannel(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static NotificationChannel fromValue(String value) {
        for (NotificationChannel channel : values()) {
            if (channel.value.equalsIgnoreCase(value)) {
                return channel;
            }
        }
        throw new IllegalArgumentException("Unknown notification channel: " + value);
    }
}
