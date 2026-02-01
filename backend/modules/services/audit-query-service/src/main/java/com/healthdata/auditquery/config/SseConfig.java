package com.healthdata.auditquery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for Server-Sent Events (SSE) streaming.
 *
 * <p>Provides a dedicated thread pool for SSE polling tasks to avoid blocking
 * the main request handling threads.
 */
@Configuration
public class SseConfig {

    /**
     * Create a thread pool executor for SSE event polling.
     *
     * <p>Configuration:
     * <ul>
     *   <li>Core pool size: 5 threads (for 5 concurrent SSE connections)</li>
     *   <li>Max pool size: 20 threads (supports up to 20 concurrent connections)</li>
     *   <li>Queue capacity: 100 (additional pending connections)</li>
     *   <li>Thread name prefix: "sse-poll-"</li>
     * </ul>
     *
     * @return ThreadPoolTaskExecutor for SSE polling
     */
    @Bean(name = "sseTaskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("sse-poll-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
