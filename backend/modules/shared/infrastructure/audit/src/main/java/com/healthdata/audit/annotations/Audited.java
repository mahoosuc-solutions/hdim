package com.healthdata.audit.annotations;

import com.healthdata.audit.models.AuditAction;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method for HIPAA audit logging.
 *
 * Usage:
 * <pre>
 * {@code
 * @Audited(
 *     action = AuditAction.READ,
 *     resourceType = "Patient",
 *     includePayload = true
 * )
 * public Patient getPatient(String id) {
 *     // ...
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /**
     * The audit action being performed.
     */
    AuditAction action();

    /**
     * The type of resource being accessed (e.g., "Patient", "Observation").
     * Optional if the action is not resource-specific.
     */
    String resourceType() default "";

    /**
     * Purpose of use for the access.
     * Common values: "TREATMENT", "PAYMENT", "OPERATIONS", "RESEARCH"
     */
    String purposeOfUse() default "OPERATIONS";

    /**
     * Whether to include the request payload in the audit log.
     * Set to false for large payloads or when not needed.
     */
    boolean includeRequestPayload() default false;

    /**
     * Whether to include the response payload in the audit log.
     * Set to false for large payloads or when not needed.
     */
    boolean includeResponsePayload() default false;

    /**
     * Whether to encrypt sensitive fields in the audit log.
     * Recommended for PHI data.
     */
    boolean encryptPayload() default true;

    /**
     * Description of the audited operation.
     */
    String description() default "";
}
