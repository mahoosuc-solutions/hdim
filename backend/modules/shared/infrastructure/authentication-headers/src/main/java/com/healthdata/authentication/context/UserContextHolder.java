package com.healthdata.authentication.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-local holder for the current user context.
 *
 * This class provides static methods to access the current user context
 * from anywhere in the application without passing it through method parameters.
 *
 * The context is set by authentication filters (TrustedHeaderAuthFilter)
 * and Kafka consumer aspects (UserContextKafkaConsumerAspect).
 *
 * IMPORTANT: The context is thread-local, so it's automatically cleaned up
 * when the request completes. However, for async operations or thread pools,
 * you must explicitly propagate the context.
 *
 * Usage:
 * <pre>
 * // Get current context
 * UserContext context = UserContextHolder.getContext();
 *
 * // Check if authenticated
 * if (UserContextHolder.isAuthenticated()) {
 *     String userId = UserContextHolder.requireContext().userIdAsString();
 * }
 * </pre>
 */
public final class UserContextHolder {

    private static final Logger log = LoggerFactory.getLogger(UserContextHolder.class);

    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    private UserContextHolder() {
        // Prevent instantiation
    }

    /**
     * Get the current user context.
     *
     * @return the current UserContext, or null if not authenticated
     */
    public static UserContext getContext() {
        return CONTEXT.get();
    }

    /**
     * Get the current user context, throwing if not authenticated.
     *
     * @return the current UserContext
     * @throws IllegalStateException if no user context is set
     */
    public static UserContext requireContext() {
        UserContext context = CONTEXT.get();
        if (context == null) {
            throw new IllegalStateException("No user context available - user not authenticated");
        }
        return context;
    }

    /**
     * Set the current user context.
     *
     * @param context the UserContext to set
     */
    public static void setContext(UserContext context) {
        if (context != null) {
            CONTEXT.set(context);
            log.trace("User context set for user: {}", context.username());
        } else {
            clearContext();
        }
    }

    /**
     * Clear the current user context.
     * Should be called at the end of request processing.
     */
    public static void clearContext() {
        UserContext previous = CONTEXT.get();
        CONTEXT.remove();
        if (previous != null) {
            log.trace("User context cleared for user: {}", previous.username());
        }
    }

    /**
     * Check if a user context is currently set.
     *
     * @return true if authenticated
     */
    public static boolean isAuthenticated() {
        return CONTEXT.get() != null;
    }

    /**
     * Get the current user ID as a string, or "anonymous" if not authenticated.
     *
     * @return user ID string
     */
    public static String getCurrentUserId() {
        UserContext context = CONTEXT.get();
        return context != null ? context.userIdAsString() : "anonymous";
    }

    /**
     * Get the current username, or "anonymous" if not authenticated.
     *
     * @return username
     */
    public static String getCurrentUsername() {
        UserContext context = CONTEXT.get();
        return context != null && context.username() != null ? context.username() : "anonymous";
    }

    /**
     * Get the current primary tenant ID, or null if not authenticated.
     *
     * @return tenant ID or null
     */
    public static String getCurrentTenantId() {
        UserContext context = CONTEXT.get();
        return context != null ? context.primaryTenantId() : null;
    }

    /**
     * Execute a runnable with the given user context.
     * Useful for async operations that need to propagate context.
     *
     * @param context the UserContext to use
     * @param runnable the code to execute
     */
    public static void runWithContext(UserContext context, Runnable runnable) {
        UserContext previous = CONTEXT.get();
        try {
            setContext(context);
            runnable.run();
        } finally {
            if (previous != null) {
                setContext(previous);
            } else {
                clearContext();
            }
        }
    }

    /**
     * Create a runnable that captures the current context.
     * Useful for submitting tasks to thread pools.
     *
     * @param runnable the original runnable
     * @return a runnable that sets the captured context before execution
     */
    public static Runnable wrap(Runnable runnable) {
        UserContext captured = CONTEXT.get();
        return () -> {
            UserContext previous = CONTEXT.get();
            try {
                if (captured != null) {
                    setContext(captured);
                }
                runnable.run();
            } finally {
                if (previous != null) {
                    setContext(previous);
                } else {
                    clearContext();
                }
            }
        };
    }
}
