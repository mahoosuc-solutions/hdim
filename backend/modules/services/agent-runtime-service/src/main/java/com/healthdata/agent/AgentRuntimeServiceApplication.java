package com.healthdata.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI Agent Runtime Service - Core execution engine for HDIM AI agents.
 *
 * Provides:
 * - Multi-provider LLM abstraction (Claude, Azure OpenAI, AWS Bedrock)
 * - Tool registry and execution engine
 * - Agent memory management (Redis + PostgreSQL)
 * - HIPAA-compliant audit logging
 * - Guardrail enforcement for clinical safety
 */
@SpringBootApplication(scanBasePackages = {
    "com.healthdata.agent",
    "com.healthdata.authentication",
    "com.healthdata.persistence",
    "com.healthdata.audit",
    "com.healthdata.messaging",
    "com.healthdata.cache"
})
@EnableFeignClients(basePackages = "com.healthdata.agent.client")
@EnableAsync
@EnableScheduling
public class AgentRuntimeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentRuntimeServiceApplication.class, args);
    }
}
