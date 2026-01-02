package com.healthdata.enrichment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for Data Enrichment Service.
 */
@Configuration
public class EnrichmentConfig {

    @Bean
    public OpenAPI enrichmentOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("AI Data Enrichment Service API")
                .description("AI-powered data enrichment and quality assessment for healthcare data")
                .version("1.0.0")
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }

    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("enrichment-async-");
        executor.initialize();
        return executor;
    }
}
