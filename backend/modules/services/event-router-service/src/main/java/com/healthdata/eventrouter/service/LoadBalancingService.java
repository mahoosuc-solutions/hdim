package com.healthdata.eventrouter.service;

import com.healthdata.eventrouter.dto.EventMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class LoadBalancingService {

    private final Map<String, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Long>> consumerLoad = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> activeConsumers = new ConcurrentHashMap<>();

    public int selectPartition(EventMessage event, String targetTopic, int partitionCount) {
        AtomicInteger counter = roundRobinCounters.computeIfAbsent(
            targetTopic,
            k -> new AtomicInteger(0)
        );
        return counter.getAndIncrement() % partitionCount;
    }

    public int selectPartitionByTenant(EventMessage event, String targetTopic, int partitionCount) {
        String tenantId = event.getTenantId();
        return Math.abs(tenantId.hashCode()) % partitionCount;
    }

    public int selectPartitionByKey(EventMessage event, String key, String targetTopic, int partitionCount) {
        Object keyValue = event.getPayload().get(key);
        if (keyValue == null) {
            return selectPartition(event, targetTopic, partitionCount);
        }
        return Math.abs(keyValue.hashCode()) % partitionCount;
    }

    public void registerConsumer(String topic, String consumerId) {
        activeConsumers.computeIfAbsent(topic, k -> ConcurrentHashMap.newKeySet()).add(consumerId);
        consumerLoad.computeIfAbsent(topic, k -> new ConcurrentHashMap<>()).put(consumerId, 0L);
        log.info("Registered consumer {} for topic {}", consumerId, topic);
    }

    public void deregisterConsumer(String topic, String consumerId) {
        Set<String> consumers = activeConsumers.get(topic);
        if (consumers != null) {
            consumers.remove(consumerId);
        }
        Map<String, Long> load = consumerLoad.get(topic);
        if (load != null) {
            load.remove(consumerId);
        }
        log.info("Deregistered consumer {} from topic {}", consumerId, topic);
    }

    public void recordEventSent(String topic, String consumerId) {
        Map<String, Long> load = consumerLoad.computeIfAbsent(topic, k -> new ConcurrentHashMap<>());
        load.merge(consumerId, 1L, Long::sum);
    }

    public Map<String, Long> getConsumerLoad(String topic) {
        return new HashMap<>(consumerLoad.getOrDefault(topic, Collections.emptyMap()));
    }

    public String selectLeastLoadedConsumer(String topic) {
        Set<String> consumers = activeConsumers.get(topic);
        if (consumers == null || consumers.isEmpty()) {
            log.warn("No active consumers for topic: {}", topic);
            return null;
        }

        Map<String, Long> load = consumerLoad.getOrDefault(topic, Collections.emptyMap());

        return consumers.stream()
            .min(Comparator.comparingLong(c -> load.getOrDefault(c, 0L)))
            .orElse(null);
    }

    public Set<String> getActiveConsumers(String topic) {
        return new HashSet<>(activeConsumers.getOrDefault(topic, Collections.emptySet()));
    }

    public void resetLoad(String topic) {
        Map<String, Long> load = consumerLoad.get(topic);
        if (load != null) {
            load.replaceAll((k, v) -> 0L);
        }
    }
}
