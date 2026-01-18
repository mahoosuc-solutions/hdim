package com.healthdata.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async Configuration (Phase 2.0 Team 2)
 *
 * Configures thread pool for asynchronous audit logging
 * Prevents audit logging from blocking request processing
 */
@Configuration
@EnableAsync
public class AsyncConfiguration {

    /**
     * Thread pool executor for audit log operations
     *
     * Tuned for high-throughput, low-latency audit logging:
     * - Core threads: 4 (handle typical load)
     * - Max threads: 16 (handle spikes)
     * - Queue capacity: 500 (buffer requests during load)
     * - Thread name: audit-log- (for debugging)
     */
    @Bean("auditLogExecutor")
    public Executor auditLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Thread pool sizing
        executor.setCorePoolSize(4);           // Always maintain 4 threads
        executor.setMaxPoolSize(16);           // Scale up to 16 threads
        executor.setQueueCapacity(500);        // Queue 500 tasks before rejecting
        executor.setThreadNamePrefix("audit-log-");

        // Task rejection policy
        executor.setRejectedExecutionHandler(
            new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
        );

        // Graceful shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();
        return executor;
    }
}
