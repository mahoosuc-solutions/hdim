package com.healthdata.notification.domain.repository;

import com.healthdata.notification.domain.model.AppointmentReminderSent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Appointment Reminder Sent tracking
 *
 * HIPAA Compliance:
 * - All queries enforce multi-tenant isolation via tenantId
 */
@Repository
public interface AppointmentReminderSentRepository extends JpaRepository<AppointmentReminderSent, UUID> {

    /**
     * Check if reminder already sent for this appointment and reminder type
     *
     * Used for idempotency - prevents duplicate reminders.
     *
     * @param tenantId      Tenant ID (HIPAA §164.312(d))
     * @param appointmentId Appointment ID
     * @param reminderType  Reminder type (e.g., APPOINTMENT_REMINDER)
     * @param daysBefore    Days before appointment (1, 3, 7)
     * @return Reminder record if already sent
     */
    @Query("SELECT r FROM AppointmentReminderSent r " +
           "WHERE r.tenantId = :tenantId " +
           "AND r.appointmentId = :appointmentId " +
           "AND r.reminderType = :reminderType " +
           "AND r.daysBefore = :daysBefore")
    Optional<AppointmentReminderSent> findByTenantIdAndAppointmentIdAndTypeAndDays(
            @Param("tenantId") String tenantId,
            @Param("appointmentId") UUID appointmentId,
            @Param("reminderType") String reminderType,
            @Param("daysBefore") Integer daysBefore);

    /**
     * Check if any reminder sent for this appointment
     *
     * @param tenantId      Tenant ID
     * @param appointmentId Appointment ID
     * @return true if any reminder sent
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
           "FROM AppointmentReminderSent r " +
           "WHERE r.tenantId = :tenantId AND r.appointmentId = :appointmentId")
    boolean existsByTenantIdAndAppointmentId(
            @Param("tenantId") String tenantId,
            @Param("appointmentId") UUID appointmentId);
}
