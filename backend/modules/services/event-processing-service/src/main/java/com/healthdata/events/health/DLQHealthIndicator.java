package com.healthdata.events.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import com.healthdata.events.entity.DeadLetterQueueEntity.DLQStatus;
import com.healthdata.events.repository.DeadLetterQueueRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * DLQ Health Indicator
 *
 * Monitors the health of the Dead Letter Queue:
 * - UP: Failed events < 100
 * - WARNING: Failed events >= 100 and < 500, OR exhausted events >= 100
 * - DOWN: Failed events >= 500, OR unable to check DLQ status
 *
 * This health check is exposed via Spring Boot Actuator.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DLQHealthIndicator implements HealthIndicator {

    private static final long WARNING_THRESHOLD = 100L;
    private static final long CRITICAL_THRESHOLD = 500L;

    private final DeadLetterQueueRepository dlqRepository;

    @Override
    public Health health() {
        try {
            long failedCount = dlqRepository.countByStatus(DLQStatus.FAILED);
            long exhaustedCount = dlqRepository.countByStatus(DLQStatus.EXHAUSTED);
            long retryingCount = dlqRepository.countByStatus(DLQStatus.RETRYING);
            long total = failedCount + exhaustedCount + retryingCount;

            Health.Builder builder = new Health.Builder();

            // Add common details
            builder.withDetail("failed", failedCount)
                   .withDetail("exhausted", exhaustedCount)
                   .withDetail("retrying", retryingCount)
                   .withDetail("total", total)
                   .withDetail("warningThreshold", WARNING_THRESHOLD)
                   .withDetail("criticalThreshold", CRITICAL_THRESHOLD);

            // Determine health status
            if (failedCount >= CRITICAL_THRESHOLD) {
                builder.status(Status.DOWN)
                       .withDetail("message", "Critical: DLQ has too many failed events");
                log.warn("DLQ health is DOWN: {} failed events (critical threshold: {})",
                        failedCount, CRITICAL_THRESHOLD);
            } else if (failedCount >= WARNING_THRESHOLD) {
                builder.status(new Status("WARNING"))
                       .withDetail("message", "High number of failed events in DLQ");
                log.warn("DLQ health is WARNING: {} failed events (warning threshold: {})",
                        failedCount, WARNING_THRESHOLD);
            } else if (exhaustedCount >= WARNING_THRESHOLD) {
                builder.status(new Status("WARNING"))
                       .withDetail("message", "High number of exhausted events in DLQ");
                log.warn("DLQ health is WARNING: {} exhausted events (warning threshold: {})",
                        exhaustedCount, WARNING_THRESHOLD);
            } else {
                builder.status(Status.UP)
                       .withDetail("message", "DLQ is healthy");
                log.debug("DLQ health is UP: {} failed, {} exhausted, {} retrying",
                        failedCount, exhaustedCount, retryingCount);
            }

            return builder.build();

        } catch (Exception e) {
            log.error("Failed to check DLQ health", e);
            return Health.down()
                    .withDetail("message", "Failed to check DLQ health")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
