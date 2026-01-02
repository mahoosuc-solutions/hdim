package com.healthdata.cql.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Thread pool configuration for async measure evaluation
 */
@Configuration
public class ThreadPoolConfig {

    @Value("${cql.measure.evaluation.thread-pool.core-size:10}")
    private int corePoolSize;

    @Value("${cql.measure.evaluation.thread-pool.max-size:50}")
    private int maxPoolSize;

    @Value("${cql.measure.evaluation.thread-pool.queue-capacity:1000}")
    private int queueCapacity;

    @Bean(name = "measureEvaluationExecutor")
    public Executor measureEvaluationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("measure-eval-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
