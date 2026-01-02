package com.healthdata.agentbuilder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Agent Builder Service Application.
 * Provides no-code agent configuration and management capabilities.
 *
 * Note: This service uses Pattern 3 (No Auth) - accessed through the gateway which
 * handles authentication and passes user identity via X-User-ID header.
 * We do NOT include com.healthdata.authentication or com.healthdata.security
 * to avoid bean conflicts.
 *
 * Security: The gateway validates authentication and forwards requests with X-Tenant-ID
 * and X-User-ID headers. This service trusts the gateway's authentication.
 */
@SpringBootApplication(scanBasePackages = {
    "com.healthdata.agentbuilder",
    "com.healthdata.audit"
})
@EnableJpaRepositories(basePackages = "com.healthdata.agentbuilder.repository")
@EntityScan(basePackages = {
    "com.healthdata.agentbuilder.domain.entity",
    "com.healthdata.audit.entity"
})
@EnableFeignClients(basePackages = "com.healthdata.agentbuilder.client")
@EnableAsync
public class AgentBuilderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentBuilderServiceApplication.class, args);
    }
}
