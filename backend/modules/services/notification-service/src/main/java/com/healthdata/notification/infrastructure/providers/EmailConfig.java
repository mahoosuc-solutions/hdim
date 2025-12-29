package com.healthdata.notification.infrastructure.providers;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "notification.email")
@Data
public class EmailConfig {
    private boolean enabled = true;
    private String fromAddress = "noreply@healthdata.com";
    private String fromName = "HealthData Platform";
    private String replyTo;
}
