package com.healthdata.common.exception;

/**
 * Exception for business logic and validation errors.
 *
 * Use this exception hierarchy for:
 * - Entity not found errors
 * - Validation failures
 * - Business rule violations
 * - Duplicate entity errors
 * - Invalid state transitions
 *
 * HTTP Status: 400-409 range (client errors)
 */
public class HdimBusinessException extends HdimException {

    public HdimBusinessException(String message) {
        super("HDIM-BIZ-000", message);
    }

    public HdimBusinessException(String message, Throwable cause) {
        super("HDIM-BIZ-000", message, cause);
    }

    public HdimBusinessException(String errorCode, String message) {
        super(errorCode, message);
    }

    public HdimBusinessException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public HdimBusinessException(String errorCode, String message, String correlationId) {
        super(errorCode, message, correlationId);
    }

    @Override
    public int getHttpStatus() {
        return 400;
    }

    // ==================== Specialized Subclasses ====================

    /**
     * Exception thrown when a requested entity is not found.
     * HTTP Status: 404
     */
    public static class EntityNotFoundException extends HdimBusinessException {
        private final String entityType;
        private final String entityId;

        public EntityNotFoundException(String entityType, String entityId) {
            super("HDIM-BIZ-404", entityType + " not found: " + entityId);
            this.entityType = entityType;
            this.entityId = entityId;
        }

        public EntityNotFoundException(String entityType, String entityId, String correlationId) {
            super("HDIM-BIZ-404", entityType + " not found: " + entityId, correlationId);
            this.entityType = entityType;
            this.entityId = entityId;
        }

        public String getEntityType() {
            return entityType;
        }

        public String getEntityId() {
            return entityId;
        }

        @Override
        public int getHttpStatus() {
            return 404;
        }
    }

    /**
     * Exception thrown when input validation fails.
     * HTTP Status: 400
     */
    public static class ValidationException extends HdimBusinessException {
        private final String field;
        private final Object rejectedValue;

        public ValidationException(String message) {
            super("HDIM-BIZ-400", message);
            this.field = null;
            this.rejectedValue = null;
        }

        public ValidationException(String field, String message) {
            super("HDIM-BIZ-400", "Validation failed for field '" + field + "': " + message);
            this.field = field;
            this.rejectedValue = null;
        }

        public ValidationException(String field, Object rejectedValue, String message) {
            super("HDIM-BIZ-400", "Validation failed for field '" + field + "': " + message);
            this.field = field;
            this.rejectedValue = rejectedValue;
        }

        public String getField() {
            return field;
        }

        public Object getRejectedValue() {
            return rejectedValue;
        }

        @Override
        public int getHttpStatus() {
            return 400;
        }
    }

    /**
     * Exception thrown when attempting to create a duplicate entity.
     * HTTP Status: 409
     */
    public static class DuplicateEntityException extends HdimBusinessException {
        private final String entityType;
        private final String identifier;

        public DuplicateEntityException(String entityType, String identifier) {
            super("HDIM-BIZ-409", entityType + " already exists: " + identifier);
            this.entityType = entityType;
            this.identifier = identifier;
        }

        public String getEntityType() {
            return entityType;
        }

        public String getIdentifier() {
            return identifier;
        }

        @Override
        public int getHttpStatus() {
            return 409;
        }
    }

    /**
     * Exception thrown when an entity is in an invalid state for the requested operation.
     * HTTP Status: 409
     */
    public static class InvalidStateException extends HdimBusinessException {
        private final String currentState;
        private final String expectedState;

        public InvalidStateException(String message) {
            super("HDIM-BIZ-409", message);
            this.currentState = null;
            this.expectedState = null;
        }

        public InvalidStateException(String currentState, String expectedState, String operation) {
            super("HDIM-BIZ-409",
                "Cannot perform '" + operation + "': current state is '" + currentState +
                "', expected '" + expectedState + "'");
            this.currentState = currentState;
            this.expectedState = expectedState;
        }

        public String getCurrentState() {
            return currentState;
        }

        public String getExpectedState() {
            return expectedState;
        }

        @Override
        public int getHttpStatus() {
            return 409;
        }
    }
}
