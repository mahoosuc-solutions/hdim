package com.healthdata.sales.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

/**
 * Health indicator for Email/SMTP connectivity.
 * Reports the status of email sending capability.
 */
@Component("email")
@RequiredArgsConstructor
@Slf4j
public class EmailHealthIndicator implements HealthIndicator {

    private final JavaMailSender mailSender;

    @Override
    public Health health() {
        try {
            if (mailSender instanceof JavaMailSenderImpl javaMailSender) {
                String host = javaMailSender.getHost();
                int port = javaMailSender.getPort();

                // Try to test connection
                try {
                    javaMailSender.testConnection();
                    return Health.up()
                        .withDetail("status", "SMTP server reachable")
                        .withDetail("host", host)
                        .withDetail("port", port)
                        .build();
                } catch (Exception e) {
                    return Health.down()
                        .withDetail("status", "SMTP server unreachable")
                        .withDetail("host", host)
                        .withDetail("port", port)
                        .withDetail("error", e.getMessage())
                        .build();
                }
            }

            return Health.unknown()
                .withDetail("status", "Unable to determine email health")
                .build();
        } catch (Exception e) {
            log.error("Email health check failed: {}", e.getMessage());
            return Health.down()
                .withDetail("status", "Health check failed")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
