package com.healthdata.sales.health;

import com.healthdata.sales.client.ZohoOAuthService;
import com.healthdata.sales.config.ZohoConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for Zoho CRM connectivity.
 * Reports the status of Zoho integration.
 */
@Component("zoho")
@RequiredArgsConstructor
@Slf4j
public class ZohoHealthIndicator implements HealthIndicator {

    private final ZohoConfig zohoConfig;
    private final ZohoOAuthService oAuthService;

    @Override
    public Health health() {
        try {
            if (!zohoConfig.getSync().isEnabled()) {
                return Health.up()
                    .withDetail("status", "Zoho sync is disabled")
                    .withDetail("syncEnabled", false)
                    .build();
            }

            if (!oAuthService.isConfigured()) {
                return Health.down()
                    .withDetail("status", "Zoho OAuth not configured")
                    .withDetail("syncEnabled", true)
                    .withDetail("configured", false)
                    .build();
            }

            // Try to get access token to verify connectivity
            try {
                oAuthService.getAccessToken();
                return Health.up()
                    .withDetail("status", "Connected to Zoho CRM")
                    .withDetail("syncEnabled", true)
                    .withDetail("configured", true)
                    .withDetail("tokenValid", true)
                    .build();
            } catch (Exception tokenEx) {
                return Health.down()
                    .withDetail("status", "Zoho token expired or invalid")
                    .withDetail("syncEnabled", true)
                    .withDetail("configured", true)
                    .withDetail("tokenValid", false)
                    .withDetail("error", tokenEx.getMessage())
                    .build();
            }
        } catch (Exception e) {
            log.error("Zoho health check failed: {}", e.getMessage());
            return Health.down()
                .withDetail("status", "Health check failed")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
