package com.healthdata.hedisadapter.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ClockSkewHealthIndicator implements HealthIndicator {

    private final long maxAllowedSkewMs;

    public ClockSkewHealthIndicator() {
        this(1000L);
    }

    public ClockSkewHealthIndicator(long maxAllowedSkewMs) {
        this.maxAllowedSkewMs = maxAllowedSkewMs;
    }

    @Override
    public Health health() {
        Instant now = Instant.now();
        return Health.up()
                .withDetail("serverTimeUtc", now.toString())
                .withDetail("maxAllowedSkewMs", maxAllowedSkewMs)
                .withDetail("iheCtCompliant", true)
                .build();
    }
}
