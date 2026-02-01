package com.healthdata.test.audit;

import com.healthdata.audit.models.ai.AIAgentDecisionEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Utility for verifying audit events in tests.
 * 
 * Provides methods to:
 * - Wait for events to be published
 * - Verify event structure and content
 * - Assert event ordering
 * - Check partition distribution
 */
public class AuditEventVerifier {
    
    private final KafkaConsumer<String, String> consumer;
    private final String topic;
    private final long timeoutMs;
    
    public AuditEventVerifier(KafkaConsumer<String, String> consumer, String topic) {
        this(consumer, topic, 10000L);
    }
    
    public AuditEventVerifier(KafkaConsumer<String, String> consumer, String topic, long timeoutMs) {
        this.consumer = consumer;
        this.topic = topic;
        this.timeoutMs = timeoutMs;
        this.consumer.subscribe(Collections.singletonList(topic));
    }
    
    /**
     * Wait for a single event matching the predicate.
     */
    public ConsumerRecord<String, String> waitForEvent(Predicate<ConsumerRecord<String, String>> predicate) {
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records) {
                if (predicate.test(record)) {
                    return record;
                }
            }
        }
        
        throw new AssertionError("No event matching predicate found within " + timeoutMs + "ms");
    }
    
    /**
     * Wait for multiple events matching the predicate.
     */
    public List<ConsumerRecord<String, String>> waitForEvents(int count, Predicate<ConsumerRecord<String, String>> predicate) {
        List<ConsumerRecord<String, String>> matchingRecords = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        
        while (matchingRecords.size() < count && System.currentTimeMillis() - startTime < timeoutMs) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records) {
                if (predicate.test(record)) {
                    matchingRecords.add(record);
                    if (matchingRecords.size() >= count) {
                        break;
                    }
                }
            }
        }
        
        assertThat(matchingRecords)
            .as("Expected %d events but found %d within %dms", count, matchingRecords.size(), timeoutMs)
            .hasSize(count);
        
        return matchingRecords;
    }
    
    /**
     * Verify partition key format (tenantId:agentId).
     */
    public static void verifyPartitionKey(ConsumerRecord<String, String> record, String expectedTenantId, String expectedAgentId) {
        String expectedKey = expectedTenantId + ":" + expectedAgentId;
        assertThat(record.key())
            .as("Partition key should be tenantId:agentId")
            .isEqualTo(expectedKey);
    }
    
    /**
     * Verify event is in correct partition.
     */
    public static void verifyPartition(ConsumerRecord<String, String> record) {
        assertThat(record.partition())
            .as("Event should be assigned to a partition")
            .isGreaterThanOrEqualTo(0);
    }
    
    /**
     * Verify events are ordered by timestamp.
     */
    public static void verifyEventOrdering(List<ConsumerRecord<String, String>> records) {
        for (int i = 1; i < records.size(); i++) {
            long prevTimestamp = records.get(i - 1).timestamp();
            long currTimestamp = records.get(i).timestamp();
            assertThat(currTimestamp)
                .as("Events should be ordered by timestamp")
                .isGreaterThanOrEqualTo(prevTimestamp);
        }
    }
    
    /**
     * Verify events are distributed across partitions.
     */
    public static void verifyPartitionDistribution(List<ConsumerRecord<String, String>> records, int minPartitions) {
        long uniquePartitions = records.stream()
            .map(ConsumerRecord::partition)
            .distinct()
            .count();
        
        assertThat(uniquePartitions)
            .as("Events should be distributed across at least %d partitions", minPartitions)
            .isGreaterThanOrEqualTo(minPartitions);
    }
    
    /**
     * Close the consumer.
     */
    public void close() {
        if (consumer != null) {
            consumer.close();
        }
    }
}

