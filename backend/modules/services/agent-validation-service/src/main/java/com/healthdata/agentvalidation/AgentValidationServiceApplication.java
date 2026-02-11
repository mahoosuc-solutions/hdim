package com.healthdata.agentvalidation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI Agent Validation Service - Testing and validation framework for HDIM AI agents.
 *
 * Provides:
 * - Test suite orchestration for user story-based validation
 * - Jaeger trace integration for debugging and correlation
 * - DeepEval metrics evaluation (relevancy, faithfulness, hallucination)
 * - Agent reflection and self-assessment framework
 * - Multi-provider A/B testing (Claude, Azure OpenAI, Bedrock)
 * - Golden response regression testing
 * - QA integration for human-in-the-loop review
 * - HIPAA-compliant audit logging
 */
@SpringBootApplication(
    scanBasePackages = {
        "com.healthdata.agentvalidation",
        "com.healthdata.authentication.filter",
        "com.healthdata.authentication.context",
        "com.healthdata.authentication.config",
        "com.healthdata.authentication.service",
        "com.healthdata.persistence",
        "com.healthdata.messaging",
        "com.healthdata.tracing"
        // Note: com.healthdata.audit is auto-configured via AuditAutoConfiguration
    },
    exclude = {
        com.healthdata.authentication.config.AuthenticationAutoConfiguration.class
    }
)
@EnableJpaRepositories(basePackages = {
    "com.healthdata.agentvalidation.repository"
})
@EntityScan(basePackages = {
    "com.healthdata.agentvalidation.domain.entity"
})
@EnableFeignClients(basePackages = "com.healthdata.agentvalidation.client")
@EnableAsync
@EnableScheduling
public class AgentValidationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentValidationServiceApplication.class, args);
    }
}
