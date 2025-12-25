package com.healthdata.sales.entity;

/**
 * Status of a sequence enrollment
 */
public enum EnrollmentStatus {
    ACTIVE,            // Currently in the sequence
    PAUSED,            // Temporarily paused
    COMPLETED,         // Finished all steps
    UNSUBSCRIBED,      // User unsubscribed
    BOUNCED,           // Email bounced
    CONVERTED,         // Lead/contact converted (goal achieved)
    CANCELLED          // Manually cancelled
}
