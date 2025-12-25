package com.healthdata.sales.entity;

/**
 * Types of email sequences
 */
public enum SequenceType {
    NURTURE,           // Long-term nurturing sequence
    FOLLOW_UP,         // Post-demo or post-meeting follow-up
    ONBOARDING,        // New customer onboarding
    RE_ENGAGEMENT,     // Re-engage cold leads
    PROMOTION,         // Promotional campaigns
    EVENT,             // Event-related sequence
    TRIAL,             // Trial user sequence
    WELCOME,           // Welcome sequence for new leads
    CUSTOM             // Custom sequence
}
