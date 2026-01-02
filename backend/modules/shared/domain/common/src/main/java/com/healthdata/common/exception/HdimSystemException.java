package com.healthdata.common.exception;

/**
 * Exception for internal system and configuration errors.
 *
 * Use this exception hierarchy for:
 * - Configuration errors
 * - Internal processing errors
 * - Unexpected system states
 * - Resource exhaustion
 *
 * These exceptions typically indicate bugs or deployment issues.
 *
 * HTTP Status: 500
 */
public class HdimSystemException extends HdimException {

    public HdimSystemException(String message) {
        super("HDIM-SYS-500", message);
    }

    public HdimSystemException(String message, Throwable cause) {
        super("HDIM-SYS-500", message, cause);
    }

    public HdimSystemException(String errorCode, String message) {
        super(errorCode, message);
    }

    public HdimSystemException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public HdimSystemException(String errorCode, String message, String correlationId) {
        super(errorCode, message, correlationId);
    }

    public HdimSystemException(String errorCode, String message, String correlationId, Throwable cause) {
        super(errorCode, message, correlationId, cause);
    }

    @Override
    public int getHttpStatus() {
        return 500;
    }

    // ==================== Specialized Subclasses ====================

    /**
     * Exception thrown when application configuration is invalid.
     * HTTP Status: 500
     */
    public static class ConfigurationException extends HdimSystemException {
        private final String configKey;

        public ConfigurationException(String message) {
            super("HDIM-SYS-500", message);
            this.configKey = null;
        }

        public ConfigurationException(String configKey, String message) {
            super("HDIM-SYS-500", "Configuration error for '" + configKey + "': " + message);
            this.configKey = configKey;
        }

        public ConfigurationException(String configKey, String message, Throwable cause) {
            super("HDIM-SYS-500", "Configuration error for '" + configKey + "': " + message, cause);
            this.configKey = configKey;
        }

        public String getConfigKey() {
            return configKey;
        }
    }

    /**
     * Exception thrown when an internal processing error occurs.
     * HTTP Status: 500
     */
    public static class InternalErrorException extends HdimSystemException {
        public InternalErrorException(String message) {
            super("HDIM-SYS-500", message);
        }

        public InternalErrorException(String message, Throwable cause) {
            super("HDIM-SYS-500", message, cause);
        }

        public InternalErrorException(String message, String correlationId) {
            super("HDIM-SYS-500", message, correlationId);
        }

        public InternalErrorException(String message, String correlationId, Throwable cause) {
            super("HDIM-SYS-500", message, correlationId, cause);
        }
    }

    /**
     * Exception thrown when a required feature is not implemented.
     * HTTP Status: 501
     */
    public static class NotImplementedException extends HdimSystemException {
        public NotImplementedException(String feature) {
            super("HDIM-SYS-501", "Feature not implemented: " + feature);
        }

        @Override
        public int getHttpStatus() {
            return 501;
        }
    }

    /**
     * Exception thrown when system resources are exhausted.
     * HTTP Status: 503
     */
    public static class ResourceExhaustedException extends HdimSystemException {
        private final String resourceType;

        public ResourceExhaustedException(String resourceType) {
            super("HDIM-SYS-503", resourceType + " exhausted");
            this.resourceType = resourceType;
        }

        public ResourceExhaustedException(String resourceType, String details) {
            super("HDIM-SYS-503", resourceType + " exhausted: " + details);
            this.resourceType = resourceType;
        }

        public String getResourceType() {
            return resourceType;
        }

        @Override
        public int getHttpStatus() {
            return 503;
        }
    }

    /**
     * Exception thrown when an operation is cancelled.
     * HTTP Status: 499 (Client Closed Request - nginx extension)
     */
    public static class OperationCancelledException extends HdimSystemException {
        public OperationCancelledException(String operation) {
            super("HDIM-SYS-499", "Operation cancelled: " + operation);
        }

        public OperationCancelledException(String operation, Throwable cause) {
            super("HDIM-SYS-499", "Operation cancelled: " + operation, cause);
        }

        @Override
        public int getHttpStatus() {
            return 499;
        }
    }
}
