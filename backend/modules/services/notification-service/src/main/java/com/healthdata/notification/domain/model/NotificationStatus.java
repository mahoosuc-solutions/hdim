package com.healthdata.notification.domain.model;

/**
 * Notification delivery status.
 */
public enum NotificationStatus {
    PENDING,
    QUEUED,
    SENDING,
    SENT,
    DELIVERED,
    FAILED,
    BOUNCED,
    CANCELLED
}
