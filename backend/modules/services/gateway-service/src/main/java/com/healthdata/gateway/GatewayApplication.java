package com.healthdata.gateway;

import com.healthdata.cache.CacheEvictionService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
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
@ComponentScan(basePackages = {
    "com.healthdata.gateway",
    "com.healthdata.authentication"  // Scan shared authentication module
    // Do NOT scan com.healthdata.cache - we manually import only CacheEvictionService
})
@EntityScan(basePackages = {
    "com.healthdata.authentication.domain",
    "com.healthdata.authentication.entity"
})
@EnableJpaRepositories(basePackages = {
    "com.healthdata.authentication.repository"
})
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
