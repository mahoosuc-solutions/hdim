package com.healthdata.authentication.context;

import com.healthdata.authentication.constants.AuthHeaderConstants;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Thread-local context for storing authentication headers.
 *
 * This context uses InheritableThreadLocal to ensure that auth headers
 * are propagated to child threads, which is essential for Feign clients
 * that may execute requests in different threads.
 *
 * Usage:
 * 1. AuthHeaderContextFilter captures headers from incoming request
 * 2. AuthHeaderForwardingInterceptor reads headers from this context
 * 3. Headers are automatically available in Feign execution threads
 *
 * Lifecycle:
 * - Set at the start of request processing (in AuthHeaderContextFilter)
 * - Cleared at the end of request processing (in AuthHeaderContextFilter)
 * - Inherited by child threads for the duration of the request
 */
@Slf4j
public final class AuthHeaderContext {

    /**
     * InheritableThreadLocal ensures that the context is available in child threads.
     * This is critical for Feign clients that may execute HTTP calls in thread pools.
     */
    private static final InheritableThreadLocal<Map<String, String>> CONTEXT =
        new InheritableThreadLocal<>() {
            @Override
            protected Map<String, String> initialValue() {
                return new HashMap<>();
            }

            @Override
            protected Map<String, String> childValue(Map<String, String> parentValue) {
                // Create a copy for child threads to ensure thread safety
                return parentValue != null ? new HashMap<>(parentValue) : new HashMap<>();
            }
        };

    private AuthHeaderContext() {
        // Prevent instantiation
    }

    /**
     * Set a header value in the current context.
     *
     * @param headerName the header name
     * @param headerValue the header value
     */
    public static void setHeader(String headerName, String headerValue) {
        if (headerName != null && headerValue != null && !headerValue.isBlank()) {
            CONTEXT.get().put(headerName, headerValue);
            log.trace("AuthHeaderContext: set {} = {}", headerName, maskValue(headerName, headerValue));
        }
    }

    /**
     * Get a header value from the current context.
     *
     * @param headerName the header name
     * @return the header value, or null if not set
     */
    public static String getHeader(String headerName) {
        return CONTEXT.get().get(headerName);
    }

    /**
     * Get all headers from the current context.
     *
     * @return unmodifiable map of all headers
     */
    public static Map<String, String> getAllHeaders() {
        return Collections.unmodifiableMap(CONTEXT.get());
    }

    /**
     * Check if a header is set in the current context.
     *
     * @param headerName the header name
     * @return true if the header is set
     */
    public static boolean hasHeader(String headerName) {
        String value = CONTEXT.get().get(headerName);
        return value != null && !value.isBlank();
    }

    /**
     * Check if any auth headers are present in the context.
     *
     * @return true if any auth headers are set
     */
    public static boolean hasAuthHeaders() {
        Map<String, String> headers = CONTEXT.get();
        for (String headerName : AuthHeaderConstants.getAllAuthHeaders()) {
            if (headers.containsKey(headerName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set all auth headers from a request.
     * Convenience method to capture all standard auth headers at once.
     *
     * @param headers map of header names to values
     */
    public static void setAllHeaders(Map<String, String> headers) {
        if (headers != null) {
            headers.forEach(AuthHeaderContext::setHeader);
        }
    }

    /**
     * Clear all headers from the current context.
     * This should be called at the end of request processing to prevent
     * memory leaks and context pollution between requests.
     */
    public static void clear() {
        CONTEXT.remove();
        log.trace("AuthHeaderContext: cleared");
    }

    /**
     * Mask sensitive header values for logging.
     */
    private static String maskValue(String headerName, String value) {
        if (headerName.equals(AuthHeaderConstants.HEADER_VALIDATED) ||
            headerName.equals(AuthHeaderConstants.HEADER_USER_ID) ||
            headerName.equals(AuthHeaderConstants.HEADER_TOKEN_ID)) {
            if (value.length() > 8) {
                return value.substring(0, 8) + "***";
            }
            return "***";
        }
        return value;
    }
}
