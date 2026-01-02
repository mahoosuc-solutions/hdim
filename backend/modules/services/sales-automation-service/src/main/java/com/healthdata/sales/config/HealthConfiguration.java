package com.healthdata.sales.config;

import com.healthdata.sales.health.EmailHealthIndicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.util.StringUtils;

/**
 * Health indicator configuration for Sales Automation Service.
 * Conditionally registers the EmailHealthIndicator bean only when
 * mail credentials are properly configured.
 */
@Configuration
@Slf4j
public class HealthConfiguration {

    @Value("${spring.mail.username:}")
    private String mailUsername;

    /**
     * Register EmailHealthIndicator bean only when spring.mail.username is non-empty.
     * This prevents health check failures in development environments where
     * SMTP credentials are not configured.
     */
    @Bean("email")
    @ConditionalOnProperty(prefix = "spring.mail", name = "username")
    public HealthIndicator emailHealthIndicator(JavaMailSender mailSender) {
        if (!StringUtils.hasText(mailUsername)) {
            log.debug("Email health check disabled: spring.mail.username not configured");
            return () -> org.springframework.boot.actuate.health.Health.unknown()
                .withDetail("status", "Email health check disabled (credentials not configured)")
                .build();
        }
        log.info("Email health check enabled for SMTP server: {}", mailUsername);
        return new EmailHealthIndicator(mailSender);
    }
}
