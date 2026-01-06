package com.healthdata.demo.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Arrays;
import java.util.concurrent.Executor;

/**
 * Performance configuration for demo seeding service.
 *
 * Provides:
 * - In-memory caching with appropriate cache names
 * - Async task execution for batch operations
 * - Thread pool configuration for parallel generation
 *
 * Demo data is non-PHI so we can use longer-lived caches for performance.
 */
@Configuration
@EnableCaching
@EnableAsync
public class PerformanceConfig {

    /**
     * In-memory cache manager for demo data.
     * Since demo data is synthetic (non-PHI), we can cache aggressively.
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
                new ConcurrentMapCache("scenarios"),
                new ConcurrentMapCache("demo-patients"),
                new ConcurrentMapCache("snapshots"),
                new ConcurrentMapCache("care-gap-stats"),
                new ConcurrentMapCache("status"),
                new ConcurrentMapCache("patient-templates")
        ));
        return cacheManager;
    }

    /**
     * Async executor for batch patient generation.
     * Configured for demo workloads - moderate parallelism.
     */
    @Bean(name = "demoTaskExecutor")
    public Executor demoTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("demo-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * Dedicated executor for batch FHIR operations.
     * Higher parallelism for I/O-bound FHIR API calls.
     */
    @Bean(name = "fhirBatchExecutor")
    public Executor fhirBatchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("fhir-batch-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        return executor;
    }
}
