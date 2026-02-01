package com.healthdata.notification.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Appointment Reminder Sent Entity
 *
 * Tracks SMS reminders sent for appointments to ensure idempotency.
 * Prevents duplicate reminders from being sent.
 *
 * HIPAA Compliance:
 * - Multi-tenant isolation via tenant_id
 * - Phone numbers stored (required for delivery tracking)
 * - Audit trail via sent_at and created_at
 * - Unique constraint prevents duplicate sends
 */
@Entity
@Table(name = "appointment_reminders_sent",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_appointment_reminders_sent_unique",
           columnNames = {"tenant_id", "appointment_id", "reminder_type", "days_before"}
       ),
       indexes = {
           @Index(name = "idx_appointment_reminders_tenant_id", columnList = "tenant_id"),
           @Index(name = "idx_appointment_reminders_appointment_id", columnList = "appointment_id"),
           @Index(name = "idx_appointment_reminders_sent_at", columnList = "sent_at"),
           @Index(name = "idx_appointment_reminders_status", columnList = "status")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentReminderSent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Tenant ID (HIPAA §164.312(d))
     */
    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenantId;

    /**
     * Appointment ID from FHIR service
     */
    @Column(name = "appointment_id", nullable = false)
    private UUID appointmentId;

    /**
     * Patient ID
     */
    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    /**
     * Patient phone number (E.164 format)
     * Stored for delivery tracking and troubleshooting
     */
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    /**
     * Reminder type (e.g., APPOINTMENT_REMINDER)
     */
    @Column(name = "reminder_type", nullable = false, length = 50)
    private String reminderType;

    /**
     * Days before appointment (1, 3, 7)
     */
    @Column(name = "days_before", nullable = false)
    private Integer daysBefore;

    /**
     * When reminder was sent
     */
    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    /**
     * Notification channel (SMS, EMAIL, PUSH)
     */
    @Column(name = "channel", nullable = false, length = 20)
    @Builder.Default
    private String channel = "SMS";

    /**
     * Delivery status (SENT, FAILED, DELIVERED, UNDELIVERED)
     */
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "SENT";

    /**
     * Twilio message SID for tracking
     */
    @Column(name = "message_sid", length = 255)
    private String messageSid;

    /**
     * Error message if failed
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Audit: When record created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (sentAt == null) {
            sentAt = Instant.now();
        }
    }
}
