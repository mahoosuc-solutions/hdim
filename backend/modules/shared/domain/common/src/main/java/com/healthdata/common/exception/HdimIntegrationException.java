package com.healthdata.common.exception;

/**
 * Exception for external service and integration errors.
 *
 * Use this exception hierarchy for:
 * - External API call failures
 * - Database connection errors
 * - Message queue errors
 * - Third-party service unavailability
 * - Integration timeouts
 *
 * HTTP Status: 502, 503, 504
 */
public class HdimIntegrationException extends HdimException {

    private final String serviceName;

    public HdimIntegrationException(String message) {
        super("HDIM-INT-000", message);
        this.serviceName = null;
    }

    public HdimIntegrationException(String message, Throwable cause) {
        super("HDIM-INT-000", message, cause);
        this.serviceName = null;
    }

    public HdimIntegrationException(String serviceName, String message) {
        super("HDIM-INT-000", serviceName + ": " + message);
        this.serviceName = serviceName;
    }

    public HdimIntegrationException(String serviceName, String message, Throwable cause) {
        super("HDIM-INT-000", serviceName + ": " + message, cause);
        this.serviceName = serviceName;
    }

    public HdimIntegrationException(String errorCode, String serviceName, String message) {
        super(errorCode, serviceName + ": " + message);
        this.serviceName = serviceName;
    }

    public HdimIntegrationException(String errorCode, String serviceName, String message, Throwable cause) {
        super(errorCode, serviceName + ": " + message, cause);
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }

    @Override
    public int getHttpStatus() {
        return 502;
    }

    // ==================== Specialized Subclasses ====================

    /**
     * Exception thrown when an external service is unavailable.
     * HTTP Status: 503
     */
    public static class ServiceUnavailableException extends HdimIntegrationException {
        private final boolean retryable;

        public ServiceUnavailableException(String serviceName) {
            super("HDIM-INT-503", serviceName, "Service unavailable");
            this.retryable = true;
        }

        public ServiceUnavailableException(String serviceName, String message) {
            super("HDIM-INT-503", serviceName, message);
            this.retryable = true;
        }

        public ServiceUnavailableException(String serviceName, String message, boolean retryable) {
            super("HDIM-INT-503", serviceName, message);
            this.retryable = retryable;
        }

        public boolean isRetryable() {
            return retryable;
        }

        @Override
        public int getHttpStatus() {
            return 503;
        }
    }

    /**
     * Exception thrown when an external service call times out.
     * HTTP Status: 504
     */
    public static class ServiceTimeoutException extends HdimIntegrationException {
        private final long timeoutMs;

        public ServiceTimeoutException(String serviceName, long timeoutMs) {
            super("HDIM-INT-504", serviceName, "Request timed out after " + timeoutMs + "ms");
            this.timeoutMs = timeoutMs;
        }

        public ServiceTimeoutException(String serviceName, long timeoutMs, Throwable cause) {
            super("HDIM-INT-504", serviceName, "Request timed out after " + timeoutMs + "ms", cause);
            this.timeoutMs = timeoutMs;
        }

        public long getTimeoutMs() {
            return timeoutMs;
        }

        @Override
        public int getHttpStatus() {
            return 504;
        }
    }

    /**
     * Exception thrown when an external API returns an error.
     * HTTP Status: 502
     */
    public static class ExternalApiException extends HdimIntegrationException {
        private final int externalStatusCode;
        private final String externalErrorCode;

        public ExternalApiException(String serviceName, int externalStatusCode, String message) {
            super("HDIM-INT-502", serviceName, message);
            this.externalStatusCode = externalStatusCode;
            this.externalErrorCode = null;
        }

        public ExternalApiException(String serviceName, int externalStatusCode, String externalErrorCode, String message) {
            super("HDIM-INT-502", serviceName, message);
            this.externalStatusCode = externalStatusCode;
            this.externalErrorCode = externalErrorCode;
        }

        public int getExternalStatusCode() {
            return externalStatusCode;
        }

        public String getExternalErrorCode() {
            return externalErrorCode;
        }

        @Override
        public int getHttpStatus() {
            return 502;
        }
    }

    /**
     * Exception thrown when database operations fail.
     * HTTP Status: 503
     */
    public static class DataAccessException extends HdimIntegrationException {
        public DataAccessException(String message) {
            super("HDIM-INT-503", "Database", message);
        }

        public DataAccessException(String message, Throwable cause) {
            super("HDIM-INT-503", "Database", message, cause);
        }

        @Override
        public int getHttpStatus() {
            return 503;
        }
    }

    /**
     * Exception thrown when message queue operations fail.
     * HTTP Status: 503
     */
    public static class MessagingException extends HdimIntegrationException {
        private final String topic;

        public MessagingException(String message) {
            super("HDIM-INT-503", "MessageBroker", message);
            this.topic = null;
        }

        public MessagingException(String topic, String message) {
            super("HDIM-INT-503", "MessageBroker", message);
            this.topic = topic;
        }

        public MessagingException(String topic, String message, Throwable cause) {
            super("HDIM-INT-503", "MessageBroker", message, cause);
            this.topic = topic;
        }

        public String getTopic() {
            return topic;
        }

        @Override
        public int getHttpStatus() {
            return 503;
        }
    }
}
