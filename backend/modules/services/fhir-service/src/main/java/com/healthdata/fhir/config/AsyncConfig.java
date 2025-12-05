package com.healthdata.fhir.config;

import com.healthdata.fhir.bulk.BulkExportConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async configuration for FHIR service
 *
 * Configures thread pool executor for bulk export operations.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    private final BulkExportConfig bulkExportConfig;

    public AsyncConfig(BulkExportConfig bulkExportConfig) {
        this.bulkExportConfig = bulkExportConfig;
    }

    /**
     * Thread pool executor for bulk export jobs
     */
    @Bean(name = "bulkExportExecutor")
    public Executor bulkExportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(bulkExportConfig.getAsyncExecutorPoolSize());
        executor.setMaxPoolSize(bulkExportConfig.getAsyncExecutorPoolSize() * 2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("bulk-export-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
