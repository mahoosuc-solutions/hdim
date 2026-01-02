package com.healthdata.eventrouter.service;

import com.healthdata.eventrouter.dto.EventMessage;
import com.healthdata.eventrouter.entity.RoutingRuleEntity.Priority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Priority Queue Service Tests")
class PriorityQueueServiceTest {

    private PriorityQueueService queueService;

    @BeforeEach
    void setUp() {
        queueService = new PriorityQueueService();
    }

    @Test
    @DisplayName("Should process CRITICAL priority events first")
    void shouldProcessCriticalFirst() {
        // Given
        EventMessage lowEvent = createEvent("low-event", Priority.LOW);
        EventMessage criticalEvent = createEvent("critical-event", Priority.CRITICAL);
        EventMessage mediumEvent = createEvent("medium-event", Priority.MEDIUM);

        // When
        queueService.enqueue(lowEvent, Priority.LOW);
        queueService.enqueue(criticalEvent, Priority.CRITICAL);
        queueService.enqueue(mediumEvent, Priority.MEDIUM);

        // Then
        Optional<EventMessage> first = queueService.dequeue();
        assertThat(first).isPresent();
        assertThat(first.get().getEventType()).isEqualTo("critical-event");
    }

    @Test
    @DisplayName("Should process events in priority order: CRITICAL > HIGH > MEDIUM > LOW")
    void shouldProcessInPriorityOrder() {
        // Given
        queueService.enqueue(createEvent("low-1", Priority.LOW), Priority.LOW);
        queueService.enqueue(createEvent("medium-1", Priority.MEDIUM), Priority.MEDIUM);
        queueService.enqueue(createEvent("critical-1", Priority.CRITICAL), Priority.CRITICAL);
        queueService.enqueue(createEvent("high-1", Priority.HIGH), Priority.HIGH);
        queueService.enqueue(createEvent("low-2", Priority.LOW), Priority.LOW);
        queueService.enqueue(createEvent("medium-2", Priority.MEDIUM), Priority.MEDIUM);

        // When
        List<String> processedOrder = new ArrayList<>();
        Optional<EventMessage> event;
        while ((event = queueService.dequeue()).isPresent()) {
            processedOrder.add(event.get().getEventType());
        }

        // Then
        assertThat(processedOrder).containsExactly(
            "critical-1",
            "high-1",
            "medium-1",
            "medium-2",
            "low-1",
            "low-2"
        );
    }

    @Test
    @DisplayName("Should maintain FIFO order within same priority")
    void shouldMaintainFifoWithinPriority() {
        // Given
        queueService.enqueue(createEvent("medium-1", Priority.MEDIUM), Priority.MEDIUM);
        queueService.enqueue(createEvent("medium-2", Priority.MEDIUM), Priority.MEDIUM);
        queueService.enqueue(createEvent("medium-3", Priority.MEDIUM), Priority.MEDIUM);

        // When
        List<String> processedOrder = new ArrayList<>();
        Optional<EventMessage> event;
        while ((event = queueService.dequeue()).isPresent()) {
            processedOrder.add(event.get().getEventType());
        }

        // Then
        assertThat(processedOrder).containsExactly("medium-1", "medium-2", "medium-3");
    }

    @Test
    @DisplayName("Should return empty when queue is empty")
    void shouldReturnEmptyWhenQueueEmpty() {
        // When
        Optional<EventMessage> event = queueService.dequeue();

        // Then
        assertThat(event).isEmpty();
    }

    @Test
    @DisplayName("Should report queue size correctly")
    void shouldReportQueueSize() {
        // Given
        queueService.enqueue(createEvent("event-1", Priority.HIGH), Priority.HIGH);
        queueService.enqueue(createEvent("event-2", Priority.MEDIUM), Priority.MEDIUM);
        queueService.enqueue(createEvent("event-3", Priority.LOW), Priority.LOW);

        // Then
        assertThat(queueService.size()).isEqualTo(3);

        // When
        queueService.dequeue();

        // Then
        assertThat(queueService.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should report size by priority")
    void shouldReportSizeByPriority() {
        // Given
        queueService.enqueue(createEvent("critical-1", Priority.CRITICAL), Priority.CRITICAL);
        queueService.enqueue(createEvent("critical-2", Priority.CRITICAL), Priority.CRITICAL);
        queueService.enqueue(createEvent("high-1", Priority.HIGH), Priority.HIGH);
        queueService.enqueue(createEvent("medium-1", Priority.MEDIUM), Priority.MEDIUM);

        // Then
        assertThat(queueService.sizeByPriority(Priority.CRITICAL)).isEqualTo(2);
        assertThat(queueService.sizeByPriority(Priority.HIGH)).isEqualTo(1);
        assertThat(queueService.sizeByPriority(Priority.MEDIUM)).isEqualTo(1);
        assertThat(queueService.sizeByPriority(Priority.LOW)).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle high volume of events")
    void shouldHandleHighVolume() {
        // Given
        int eventCount = 1000;
        for (int i = 0; i < eventCount; i++) {
            Priority priority = Priority.values()[i % 4];
            queueService.enqueue(createEvent("event-" + i, priority), priority);
        }

        // Then
        assertThat(queueService.size()).isEqualTo(eventCount);

        // When - dequeue all
        int processed = 0;
        while (queueService.dequeue().isPresent()) {
            processed++;
        }

        // Then
        assertThat(processed).isEqualTo(eventCount);
        assertThat(queueService.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should support peek without removing")
    void shouldSupportPeek() {
        // Given
        EventMessage event = createEvent("test-event", Priority.HIGH);
        queueService.enqueue(event, Priority.HIGH);

        // When
        Optional<EventMessage> peeked = queueService.peek();

        // Then
        assertThat(peeked).isPresent();
        assertThat(peeked.get().getEventType()).isEqualTo("test-event");
        assertThat(queueService.size()).isEqualTo(1); // Still in queue
    }

    private EventMessage createEvent(String type, Priority priority) {
        EventMessage event = new EventMessage();
        event.setEventType(type);
        event.setTenantId("tenant1");
        return event;
    }
}
