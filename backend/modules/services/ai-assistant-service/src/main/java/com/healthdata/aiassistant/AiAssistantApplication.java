package com.healthdata.aiassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI Clinical Assistant Service Application.
 *
 * Provides AI-powered clinical assistance including:
 * - Natural language query processing
 * - Patient summary generation
 * - Care gap analysis
 * - Quality measure interpretation
 *
 * Integrates with Anthropic Claude API for LLM capabilities.
 */
@SpringBootApplication(scanBasePackages = {
    "com.healthdata.aiassistant",
    "com.healthdata.common",
    "com.healthdata.authentication",
    "com.healthdata.persistence",
    "com.healthdata.audit"
})
@EnableJpaRepositories(basePackages = {
    "com.healthdata.aiassistant.repository",
    "com.healthdata.authentication.repository",
    "com.healthdata.persistence.repository"
})
@EntityScan(basePackages = {
    "com.healthdata.aiassistant.entity",
    "com.healthdata.authentication.entity",
    "com.healthdata.persistence.entity"
})
@EnableConfigurationProperties
@EnableCaching
@EnableScheduling
@EnableFeignClients
public class AiAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiAssistantApplication.class, args);
    }
}
