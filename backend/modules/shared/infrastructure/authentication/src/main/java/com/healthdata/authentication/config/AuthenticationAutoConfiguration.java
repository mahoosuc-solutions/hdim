package com.healthdata.authentication.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Auto-configuration for the shared authentication module.
 *
 * This configuration automatically registers authentication-related beans including:
 * - User entity scanning for JPA
 * - UserRepository for database access
 * - CustomUserDetailsService for Spring Security
 * - TenantAccessFilter for multi-tenant security
 * - PasswordEncoder (BCrypt)
 *
 * IMPORTANT: Services using this module must configure @EnableJpaRepositories
 * to include BOTH their own repository packages AND the authentication repository package:
 *
 * @EnableJpaRepositories(basePackages = {
 *     "com.yourservice.persistence",
 *     "com.healthdata.authentication.repository"
 * })
 * @EntityScan(basePackages = {
 *     "com.yourservice.persistence",
 *     "com.healthdata.authentication.domain"
 * })
 *
 * This is required because Spring Boot stops default repository scanning when
 * @EnableJpaRepositories is explicitly defined anywhere in the application.
 */
@AutoConfiguration
@ComponentScan(basePackages = {
    "com.healthdata.authentication.filter",
    "com.healthdata.authentication.service",
    "com.healthdata.authentication.config",
    "com.healthdata.cache"
})
@EntityScan(basePackages = {
    "com.healthdata.authentication.domain",
    "com.healthdata.authentication.entity"
})
public class AuthenticationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // NOTE: UserDetailsService disabled - managed by Gateway service
    // NOTE: TenantAccessFilter disabled - managed by Gateway service
}
