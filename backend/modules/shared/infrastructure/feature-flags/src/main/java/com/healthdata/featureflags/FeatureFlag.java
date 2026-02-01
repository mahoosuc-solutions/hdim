package com.healthdata.featureflags;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Feature Flag Annotation
 *
 * Marks a method to be conditionally executed based on a tenant feature flag.
 * If the feature is disabled, the method throws FeatureFlagDisabledException.
 *
 * Usage:
 * <pre>
 * @FeatureFlag("twilio-sms-reminders")
 * public void sendAppointmentReminder(String patientId, String appointmentId) {
 *     // Send SMS reminder via Twilio
 * }
 * </pre>
 *
 * The annotation expects:
 * - A @RequestHeader("X-Tenant-ID") parameter in the method
 * - OR SecurityContextHolder populated with TenantAuthenticationToken
 *
 * If the feature is disabled for the tenant, the method throws FeatureFlagDisabledException
 * which can be handled by @ControllerAdvice to return HTTP 403 or custom error.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FeatureFlag {
    /**
     * Feature key to check (e.g., "twilio-sms-reminders")
     */
    String value();

    /**
     * Whether to fail silently (return null) or throw exception
     *
     * Default: false (throw exception)
     */
    boolean failSilently() default false;
}
