package com.healthdata.events.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import com.healthdata.events.entity.DeadLetterQueueEntity.DLQStatus;
import com.healthdata.events.repository.DeadLetterQueueRepository;

/**
 * TDD Test Suite for DLQ Health Indicator
 *
 * Tests verify health check behavior:
 * - UP when failed events < 100
 * - WARNING when failed events >= 100 and < 500
 * - DOWN when failed events >= 500
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DLQ Health Indicator Tests")
class DLQHealthIndicatorTest {

    @Mock
    private DeadLetterQueueRepository dlqRepository;

    private DLQHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new DLQHealthIndicator(dlqRepository);
    }

    @Test
    @DisplayName("Should report UP when no failed events")
    void shouldReportUpWhenNoFailedEvents() {
        // Given
        when(dlqRepository.countByStatus(DLQStatus.FAILED)).thenReturn(0L);
        when(dlqRepository.countByStatus(DLQStatus.EXHAUSTED)).thenReturn(0L);
        when(dlqRepository.countByStatus(DLQStatus.RETRYING)).thenReturn(0L);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsEntry("failed", 0L)
                .containsEntry("exhausted", 0L)
                .containsEntry("retrying", 0L);
    }

    @Test
    @DisplayName("Should report UP when failed events less than 100")
    void shouldReportUpWhenFailedEventsLessThan100() {
        // Given
        when(dlqRepository.countByStatus(DLQStatus.FAILED)).thenReturn(50L);
        when(dlqRepository.countByStatus(DLQStatus.EXHAUSTED)).thenReturn(5L);
        when(dlqRepository.countByStatus(DLQStatus.RETRYING)).thenReturn(10L);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsEntry("failed", 50L)
                .containsEntry("exhausted", 5L)
                .containsEntry("retrying", 10L)
                .containsEntry("message", "DLQ is healthy");
    }

    @Test
    @DisplayName("Should report WARNING when failed events exactly 100")
    void shouldReportWarningWhenFailedEventsExactly100() {
        // Given
        when(dlqRepository.countByStatus(DLQStatus.FAILED)).thenReturn(100L);
        when(dlqRepository.countByStatus(DLQStatus.EXHAUSTED)).thenReturn(10L);
        when(dlqRepository.countByStatus(DLQStatus.RETRYING)).thenReturn(5L);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(new Status("WARNING"));
        assertThat(health.getDetails())
                .containsEntry("failed", 100L)
                .containsEntry("exhausted", 10L)
                .containsEntry("retrying", 5L)
                .containsEntry("message", "High number of failed events in DLQ");
    }

    @Test
    @DisplayName("Should report WARNING when failed events between 100 and 500")
    void shouldReportWarningWhenFailedEventsBetween100And500() {
        // Given
        when(dlqRepository.countByStatus(DLQStatus.FAILED)).thenReturn(250L);
        when(dlqRepository.countByStatus(DLQStatus.EXHAUSTED)).thenReturn(20L);
        when(dlqRepository.countByStatus(DLQStatus.RETRYING)).thenReturn(15L);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(new Status("WARNING"));
        assertThat(health.getDetails())
                .containsEntry("failed", 250L)
                .containsEntry("exhausted", 20L)
                .containsEntry("retrying", 15L)
                .containsEntry("message", "High number of failed events in DLQ");
    }

    @Test
    @DisplayName("Should report DOWN when failed events exactly 500")
    void shouldReportDownWhenFailedEventsExactly500() {
        // Given
        when(dlqRepository.countByStatus(DLQStatus.FAILED)).thenReturn(500L);
        when(dlqRepository.countByStatus(DLQStatus.EXHAUSTED)).thenReturn(50L);
        when(dlqRepository.countByStatus(DLQStatus.RETRYING)).thenReturn(10L);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
                .containsEntry("failed", 500L)
                .containsEntry("exhausted", 50L)
                .containsEntry("retrying", 10L)
                .containsEntry("message", "Critical: DLQ has too many failed events");
    }

    @Test
    @DisplayName("Should report DOWN when failed events greater than 500")
    void shouldReportDownWhenFailedEventsGreaterThan500() {
        // Given
        when(dlqRepository.countByStatus(DLQStatus.FAILED)).thenReturn(1000L);
        when(dlqRepository.countByStatus(DLQStatus.EXHAUSTED)).thenReturn(100L);
        when(dlqRepository.countByStatus(DLQStatus.RETRYING)).thenReturn(20L);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
                .containsEntry("failed", 1000L)
                .containsEntry("exhausted", 100L)
                .containsEntry("retrying", 20L)
                .containsEntry("message", "Critical: DLQ has too many failed events");
    }

    @Test
    @DisplayName("Should include threshold information in details")
    void shouldIncludeThresholdInformationInDetails() {
        // Given
        when(dlqRepository.countByStatus(DLQStatus.FAILED)).thenReturn(50L);
        when(dlqRepository.countByStatus(DLQStatus.EXHAUSTED)).thenReturn(5L);
        when(dlqRepository.countByStatus(DLQStatus.RETRYING)).thenReturn(2L);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getDetails())
                .containsEntry("warningThreshold", 100L)
                .containsEntry("criticalThreshold", 500L);
    }

    @Test
    @DisplayName("Should prioritize exhausted events in warning")
    void shouldPrioritizeExhaustedEventsInWarning() {
        // Given - few failed but many exhausted
        when(dlqRepository.countByStatus(DLQStatus.FAILED)).thenReturn(10L);
        when(dlqRepository.countByStatus(DLQStatus.EXHAUSTED)).thenReturn(150L);
        when(dlqRepository.countByStatus(DLQStatus.RETRYING)).thenReturn(5L);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(new Status("WARNING"));
        assertThat(health.getDetails())
                .containsEntry("message", "High number of exhausted events in DLQ");
    }

    @Test
    @DisplayName("Should handle repository exceptions gracefully")
    void shouldHandleRepositoryExceptionsGracefully() {
        // Given
        when(dlqRepository.countByStatus(DLQStatus.FAILED))
                .thenThrow(new RuntimeException("Database connection error"));

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
                .containsKey("error")
                .containsEntry("message", "Failed to check DLQ health");
    }

    @Test
    @DisplayName("Should report specific message for exhausted events")
    void shouldReportSpecificMessageForExhaustedEvents() {
        // Given
        when(dlqRepository.countByStatus(DLQStatus.FAILED)).thenReturn(50L);
        when(dlqRepository.countByStatus(DLQStatus.EXHAUSTED)).thenReturn(200L);
        when(dlqRepository.countByStatus(DLQStatus.RETRYING)).thenReturn(10L);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(new Status("WARNING"));
        assertThat(health.getDetails())
                .containsEntry("exhausted", 200L)
                .containsEntry("message", "High number of exhausted events in DLQ");
    }

    @Test
    @DisplayName("Should include total events in details")
    void shouldIncludeTotalEventsInDetails() {
        // Given
        when(dlqRepository.countByStatus(DLQStatus.FAILED)).thenReturn(50L);
        when(dlqRepository.countByStatus(DLQStatus.EXHAUSTED)).thenReturn(10L);
        when(dlqRepository.countByStatus(DLQStatus.RETRYING)).thenReturn(5L);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getDetails())
                .containsEntry("total", 65L);
    }
}
