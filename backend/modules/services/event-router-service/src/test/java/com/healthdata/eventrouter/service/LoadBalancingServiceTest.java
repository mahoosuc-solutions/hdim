package com.healthdata.eventrouter.service;

import com.healthdata.eventrouter.dto.EventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Load Balancing Service Tests")
class LoadBalancingServiceTest {

    private LoadBalancingService loadBalancingService;

    @BeforeEach
    void setUp() {
        loadBalancingService = new LoadBalancingService();
    }

    @Test
    @DisplayName("Should distribute events using round-robin")
    void shouldDistributeRoundRobin() {
        // Given
        String targetTopic = "patient.processing";
        int partitionCount = 3;

        // When
        int partition1 = loadBalancingService.selectPartition(createEvent("event-1"), targetTopic, partitionCount);
        int partition2 = loadBalancingService.selectPartition(createEvent("event-2"), targetTopic, partitionCount);
        int partition3 = loadBalancingService.selectPartition(createEvent("event-3"), targetTopic, partitionCount);
        int partition4 = loadBalancingService.selectPartition(createEvent("event-4"), targetTopic, partitionCount);

        // Then
        assertThat(partition1).isEqualTo(0);
        assertThat(partition2).isEqualTo(1);
        assertThat(partition3).isEqualTo(2);
        assertThat(partition4).isEqualTo(0); // Wraps around
    }

    @Test
    @DisplayName("Should distribute by tenant for tenant-based partitioning")
    void shouldDistributeByTenant() {
        // Given
        String targetTopic = "patient.processing";
        int partitionCount = 4;

        EventMessage tenant1Event = createEvent("event-1");
        tenant1Event.setTenantId("tenant-123");

        EventMessage tenant2Event = createEvent("event-2");
        tenant2Event.setTenantId("tenant-456");

        // When
        int partition1a = loadBalancingService.selectPartitionByTenant(tenant1Event, targetTopic, partitionCount);
        int partition1b = loadBalancingService.selectPartitionByTenant(tenant1Event, targetTopic, partitionCount);
        int partition2 = loadBalancingService.selectPartitionByTenant(tenant2Event, targetTopic, partitionCount);

        // Then
        assertThat(partition1a).isEqualTo(partition1b); // Same tenant -> same partition
        assertThat(partition1a).isNotEqualTo(partition2); // Different tenants
    }

    @Test
    @DisplayName("Should distribute by key for key-based partitioning")
    void shouldDistributeByKey() {
        // Given
        String targetTopic = "patient.processing";
        int partitionCount = 4;

        EventMessage event1 = createEvent("event-1");
        event1.getPayload().put("patientId", "patient-123");

        EventMessage event2 = createEvent("event-2");
        event2.getPayload().put("patientId", "patient-123");

        EventMessage event3 = createEvent("event-3");
        event3.getPayload().put("patientId", "patient-456");

        // When
        int partition1 = loadBalancingService.selectPartitionByKey(event1, "patientId", targetTopic, partitionCount);
        int partition2 = loadBalancingService.selectPartitionByKey(event2, "patientId", targetTopic, partitionCount);
        int partition3 = loadBalancingService.selectPartitionByKey(event3, "patientId", targetTopic, partitionCount);

        // Then
        assertThat(partition1).isEqualTo(partition2); // Same key -> same partition
        assertThat(partition1).isNotEqualTo(partition3); // Different keys
    }

    @Test
    @DisplayName("Should track consumer load")
    void shouldTrackConsumerLoad() {
        // Given
        String topic = "patient.processing";
        loadBalancingService.registerConsumer(topic, "consumer-1");
        loadBalancingService.registerConsumer(topic, "consumer-2");

        // When
        loadBalancingService.recordEventSent(topic, "consumer-1");
        loadBalancingService.recordEventSent(topic, "consumer-1");
        loadBalancingService.recordEventSent(topic, "consumer-2");

        // Then
        Map<String, Long> load = loadBalancingService.getConsumerLoad(topic);
        assertThat(load.get("consumer-1")).isEqualTo(2);
        assertThat(load.get("consumer-2")).isEqualTo(1);
    }

    @Test
    @DisplayName("Should select least loaded consumer")
    void shouldSelectLeastLoadedConsumer() {
        // Given
        String topic = "patient.processing";
        loadBalancingService.registerConsumer(topic, "consumer-1");
        loadBalancingService.registerConsumer(topic, "consumer-2");
        loadBalancingService.registerConsumer(topic, "consumer-3");

        loadBalancingService.recordEventSent(topic, "consumer-1"); // Load: 1
        loadBalancingService.recordEventSent(topic, "consumer-1"); // Load: 2
        loadBalancingService.recordEventSent(topic, "consumer-2"); // Load: 1
        // consumer-3 has load: 0

        // When
        String selectedConsumer = loadBalancingService.selectLeastLoadedConsumer(topic);

        // Then
        assertThat(selectedConsumer).isEqualTo("consumer-3");
    }

    @Test
    @DisplayName("Should handle consumer registration and deregistration")
    void shouldHandleConsumerLifecycle() {
        // Given
        String topic = "patient.processing";

        // When
        loadBalancingService.registerConsumer(topic, "consumer-1");
        loadBalancingService.registerConsumer(topic, "consumer-2");

        // Then
        assertThat(loadBalancingService.getActiveConsumers(topic)).hasSize(2);

        // When
        loadBalancingService.deregisterConsumer(topic, "consumer-1");

        // Then
        assertThat(loadBalancingService.getActiveConsumers(topic)).hasSize(1);
        assertThat(loadBalancingService.getActiveConsumers(topic)).contains("consumer-2");
    }

    @Test
    @DisplayName("Should distribute evenly across consumers over time")
    void shouldDistributeEvenly() {
        // Given
        String topic = "patient.processing";
        loadBalancingService.registerConsumer(topic, "consumer-1");
        loadBalancingService.registerConsumer(topic, "consumer-2");
        loadBalancingService.registerConsumer(topic, "consumer-3");

        // When - send 30 events
        Map<String, Integer> distribution = new HashMap<>();
        for (int i = 0; i < 30; i++) {
            String consumer = loadBalancingService.selectLeastLoadedConsumer(topic);
            loadBalancingService.recordEventSent(topic, consumer);
            distribution.merge(consumer, 1, Integer::sum);
        }

        // Then - each consumer should get approximately equal share
        assertThat(distribution.get("consumer-1")).isBetween(8, 12);
        assertThat(distribution.get("consumer-2")).isBetween(8, 12);
        assertThat(distribution.get("consumer-3")).isBetween(8, 12);
    }

    private EventMessage createEvent(String type) {
        EventMessage event = new EventMessage();
        event.setEventType(type);
        event.setTenantId("tenant1");
        event.setPayload(new HashMap<>());
        return event;
    }
}
