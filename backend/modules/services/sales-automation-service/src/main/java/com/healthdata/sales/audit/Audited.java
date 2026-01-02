package com.healthdata.sales.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that should be audited.
 * Can be applied to service methods to automatically generate audit events.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /**
     * The action being performed (CREATE, UPDATE, DELETE, etc.)
     */
    AuditEvent.AuditAction action();

    /**
     * The type of entity being operated on
     */
    String entityType();

    /**
     * SpEL expression to extract the entity ID from method parameters or return value
     */
    String entityId() default "";

    /**
     * SpEL expression to extract the entity name from method parameters or return value
     */
    String entityName() default "";
}
