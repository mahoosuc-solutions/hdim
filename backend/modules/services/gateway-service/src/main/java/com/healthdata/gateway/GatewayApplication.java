package com.healthdata.gateway;

import com.healthdata.cache.CacheEvictionService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Gateway Service Application
 *
 * Central authentication and API gateway service for HealthData platform.
 * Handles user authentication, JWT token issuance, and routing to backend services.
 */
@SpringBootApplication(exclude = {
    RedisAutoConfiguration.class,
    RedisRepositoriesAutoConfiguration.class
})
@Import(CacheEvictionService.class)  // Import stub without scanning cache package
@ComponentScan(
    basePackages = {
        "com.healthdata.gateway",
        "com.healthdata.authentication"        // Scan shared authentication module (provides AuthController)
        // Do NOT scan com.healthdata.cache - we manually import only CacheEvictionService
        // Do NOT scan com.healthdata.audit - gateway has its own AuditLog/AuditLogService
    },
    excludeFilters = {
        // Exclude gateway controllers that duplicate AuthController functionality
        // AuthController from authentication module provides comprehensive auth operations
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {
                com.healthdata.gateway.controller.TokenRefreshController.class
            }
        )
    }
)
@EntityScan(basePackages = {
    "com.healthdata.gateway.domain",           // Gateway service entities (AuditLog, etc.)
    "com.healthdata.authentication.domain",
    "com.healthdata.authentication.entity"
})
@EnableJpaRepositories(basePackages = {
    "com.healthdata.gateway.domain",                // Gateway service repositories (includes AuditLogRepository)
    "com.healthdata.authentication.repository"
})
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
