package com.healthdata.agentvalidation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for the test orchestrator's async execution.
 */
@Configuration
public class TestOrchestratorConfig {

    @Bean("testExecutorPool")
    public Executor testExecutorPool(ValidationProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getOrchestrator().getMaxConcurrentTests());
        executor.setMaxPoolSize(properties.getOrchestrator().getMaxConcurrentTests() * 2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("test-executor-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    @Bean("evaluationWebClient")
    public org.springframework.web.reactive.function.client.WebClient evaluationWebClient(
            ValidationProperties properties) {
        return org.springframework.web.reactive.function.client.WebClient.builder()
            .baseUrl(properties.getEvaluation().getHarnessUrl())
            .codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(4 * 1024 * 1024)) // 4MB
            .build();
    }
}
