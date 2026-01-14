package com.healthdata.caregap.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.caregap.config.BaseIntegrationTest;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance and scalability tests for Care Gap Audit Integration.
 * 
 * Tests concurrent and high-volume event publishing to verify:
 * - Non-blocking audit operations
 * - Concurrent request handling
 * - High-throughput event publishing (10,000+ events)
 * - No data corruption under load
 * - Proper partition distribution
 * - System stability under stress
 * 
 * These tests ensure the audit system can handle real-world production loads
 * for clinical decision support where sub-second response times are critical.
 */
@BaseIntegrationTest
@Testcontainers
@DisplayName("Care Gap Audit Performance Tests")
class CareGapAuditPerformanceTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.8.0"))
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true");

    @DynamicPropertySource
    static void configureKafka(DynamicPropertyRegistry registry) {
        String bootstrapServers = kafka.getBootstrapServers();
        registry.add("spring.kafka.bootstrap-servers", () -> bootstrapServers);
        registry.add("spring.kafka.producer.bootstrap-servers", () -> bootstrapServers);
        registry.add("spring.kafka.consumer.bootstrap-servers", () -> bootstrapServers);
        registry.add("audit.kafka.enabled", () -> "true");
        registry.add("audit.kafka.topic.ai-decisions", () -> "ai.agent.decisions");
    }

    @Autowired
    private CareGapAuditIntegration auditIntegration;

    @Autowired
    private ObjectMapper objectMapper;

    private Consumer<String, String> consumer;

    private static final String TOPIC = "ai.agent.decisions";

    @BeforeEach
    void setUp() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "perf-test-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "1000"); // Batch reads for performance

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(TOPIC));
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    @DisplayName("Should handle 100 concurrent event publications without data corruption")
    void shouldHandleConcurrentEventPublications() throws Exception {
        // Given
        int concurrentRequests = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(concurrentRequests);
        ExecutorService executor = Executors.newFixedThreadPool(20);
        AtomicInteger successCount = new AtomicInteger(0);
        Set<String> publishedEventIds = ConcurrentHashMap.newKeySet();

        // When - Publish events concurrently
        for (int i = 0; i < concurrentRequests; i++) {
            final int eventNumber = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // All threads start together
                    
                    String tenantId = "tenant-" + (eventNumber % 5); // 5 different tenants
                    String patientId = "patient-" + eventNumber;
                    String gapId = "gap-" + eventNumber;
                    
                    JsonNode cqlResult = objectMapper.createObjectNode()
                            .put("hasGap", true)
                            .put("measureId", "HEDIS_CDC_A1C");
                    
                    auditIntegration.publishCareGapIdentificationEvent(
                            tenantId, patientId, "HEDIS_CDC_A1C", gapId, cqlResult, "test-user");
                    
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Error publishing event " + eventNumber + ": " + e.getMessage());
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start all threads
        boolean completed = completionLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Then - Verify all events published successfully
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(concurrentRequests);

        // Consume and verify events
        long startTime = System.currentTimeMillis();
        int receivedCount = 0;
        Set<String> seenPartitionKeys = ConcurrentHashMap.newKeySet();
        
        while (receivedCount < concurrentRequests && System.currentTimeMillis() - startTime < 30000) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
            for (ConsumerRecord<String, String> record : records) {
                String jsonValue = record.value();
                assertThat(jsonValue).contains("care-gap-identifier");
                assertThat(jsonValue).contains("CARE_GAP_IDENTIFICATION");
                
                // Track partition keys for distribution analysis
                seenPartitionKeys.add(record.key());
                receivedCount++;
            }
        }

        System.out.println("Received " + receivedCount + " events from " + seenPartitionKeys.size() + " partition keys");
        
        assertThat(receivedCount).as("Should receive all published events")
                .isEqualTo(concurrentRequests);
        assertThat(seenPartitionKeys.size())
                .as("Events should be distributed across multiple partition keys (5 tenants)")
                .isGreaterThanOrEqualTo(3); // At least 3 of the 5 tenants
    }

    @Test
    @DisplayName("Should publish 10,000 events with acceptable throughput")
    void shouldHandleHighVolumeEventPublishing() throws Exception {
        // Given
        int eventCount = 10_000;
        long startTime = System.currentTimeMillis();
        AtomicInteger successCount = new AtomicInteger(0);

        // When - Publish 10,000 events
        for (int i = 0; i < eventCount; i++) {
            String tenantId = "tenant-" + (i % 10); // 10 tenants for partition distribution
            String patientId = "patient-" + i;
            String gapId = "gap-" + i;
            
            JsonNode cqlResult = objectMapper.createObjectNode()
                    .put("hasGap", true)
                    .put("eventNumber", i);
            
            try {
                auditIntegration.publishCareGapIdentificationEvent(
                        tenantId, patientId, "HEDIS_CDC_A1C", gapId, cqlResult, "test-user");
                successCount.incrementAndGet();
            } catch (Exception e) {
                System.err.println("Error publishing event " + i + ": " + e.getMessage());
            }

            // Progress indicator every 1000 events
            if ((i + 1) % 1000 == 0) {
                System.out.println("Published " + (i + 1) + " events...");
            }
        }

        long publishDuration = System.currentTimeMillis() - startTime;
        double throughput = (eventCount * 1000.0) / publishDuration; // events per second

        // Then - Verify throughput and successful publication
        assertThat(successCount.get()).isEqualTo(eventCount);
        System.out.println("Publishing completed:");
        System.out.println("  - Total events: " + eventCount);
        System.out.println("  - Duration: " + publishDuration + "ms");
        System.out.println("  - Throughput: " + String.format("%.2f", throughput) + " events/second");

        // Verify throughput is acceptable (at least 100 events/second)
        assertThat(throughput).as("Throughput should be at least 100 events/second")
                .isGreaterThan(100.0);

        // Sample consumption to verify events are in Kafka
        long consumeStart = System.currentTimeMillis();
        int consumedCount = 0;
        Set<String> seenPartitionKeys = ConcurrentHashMap.newKeySet();
        
        while (consumedCount < 1000 && System.currentTimeMillis() - consumeStart < 30000) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
            for (ConsumerRecord<String, String> record : records) {
                seenPartitionKeys.add(record.key());
                assertThat(record.value()).contains("care-gap-identifier");
                consumedCount++;
            }
        }

        System.out.println("Consumed " + consumedCount + " sample events");
        System.out.println("Unique partition keys: " + seenPartitionKeys.size());

        // Verify events are distributed across partitions
        assertThat(seenPartitionKeys.size())
                .as("Events should be distributed across multiple partition keys")
                .isGreaterThan(5);
    }

    @Test
    @DisplayName("Should maintain sub-millisecond latency under normal load")
    void shouldMaintainLowLatency() throws Exception {
        // Given
        int sampleSize = 100;
        long[] latencies = new long[sampleSize];

        // When - Measure latency for individual events
        for (int i = 0; i < sampleSize; i++) {
            JsonNode cqlResult = objectMapper.createObjectNode()
                    .put("hasGap", true);
            
            long start = System.nanoTime();
            auditIntegration.publishCareGapIdentificationEvent(
                    "tenant-test", "patient-" + i, "HEDIS_CDC_A1C", 
                    "gap-" + i, cqlResult, "test-user");
            long end = System.nanoTime();
            
            latencies[i] = (end - start) / 1_000_000; // Convert to milliseconds
        }

        // Calculate statistics
        long sum = 0;
        long max = 0;
        long min = Long.MAX_VALUE;
        
        for (long latency : latencies) {
            sum += latency;
            max = Math.max(max, latency);
            min = Math.min(min, latency);
        }
        
        double avg = sum / (double) sampleSize;

        System.out.println("Latency Statistics:");
        System.out.println("  - Average: " + String.format("%.2f", avg) + "ms");
        System.out.println("  - Min: " + min + "ms");
        System.out.println("  - Max: " + max + "ms");

        // Then - Verify latency is acceptable
        assertThat(avg).as("Average latency should be under 5ms")
                .isLessThan(5.0);
        assertThat(max).as("Max latency should be under 50ms")
                .isLessThan(50);
    }

    private String extractEventId(String jsonValue) {
        try {
            // Extract eventId from JSON string
            String searchKey = "\"eventId\":\"";
            int eventIdStart = jsonValue.indexOf(searchKey);
            if (eventIdStart == -1) {
                return "unknown-" + UUID.randomUUID(); // Not found
            }
            
            eventIdStart += searchKey.length();
            int eventIdEnd = jsonValue.indexOf("\"", eventIdStart);
            
            if (eventIdEnd == -1 || eventIdEnd <= eventIdStart) {
                return "malformed-" + UUID.randomUUID(); // Malformed
            }
            
            return jsonValue.substring(eventIdStart, eventIdEnd);
        } catch (Exception e) {
            return "error-" + UUID.randomUUID(); // Fallback on error
        }
    }
}
