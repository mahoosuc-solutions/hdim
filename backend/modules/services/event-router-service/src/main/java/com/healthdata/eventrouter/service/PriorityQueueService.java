package com.healthdata.eventrouter.service;

import com.healthdata.eventrouter.dto.EventMessage;
import com.healthdata.eventrouter.entity.RoutingRuleEntity.Priority;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

@Service
@Slf4j
public class PriorityQueueService {

    private static final int MAX_QUEUE_SIZE = 50000;
    private final Map<Priority, Queue<EventMessage>> queues = new ConcurrentHashMap<>();

    public PriorityQueueService() {
        for (Priority priority : Priority.values()) {
            queues.put(priority, new LinkedList<>());
        }
    }

    public void enqueue(EventMessage event, Priority priority) {
        Queue<EventMessage> queue = queues.get(priority);
        synchronized (queue) {
            if (size() >= MAX_QUEUE_SIZE) {
                log.warn("Queue at capacity, event may be dropped");
            }
            queue.offer(event);
        }
    }

    public Optional<EventMessage> dequeue() {
        // Process in priority order: CRITICAL -> HIGH -> MEDIUM -> LOW
        for (Priority priority : Priority.values()) {
            Queue<EventMessage> queue = queues.get(priority);
            synchronized (queue) {
                EventMessage event = queue.poll();
                if (event != null) {
                    return Optional.of(event);
                }
            }
        }
        return Optional.empty();
    }

    public Optional<EventMessage> peek() {
        for (Priority priority : Priority.values()) {
            Queue<EventMessage> queue = queues.get(priority);
            synchronized (queue) {
                EventMessage event = queue.peek();
                if (event != null) {
                    return Optional.of(event);
                }
            }
        }
        return Optional.empty();
    }

    public int size() {
        return queues.values().stream()
            .mapToInt(q -> {
                synchronized (q) {
                    return q.size();
                }
            })
            .sum();
    }

    public int sizeByPriority(Priority priority) {
        Queue<EventMessage> queue = queues.get(priority);
        synchronized (queue) {
            return queue.size();
        }
    }

    public boolean isHealthy() {
        return size() < MAX_QUEUE_SIZE;
    }

    public void clear() {
        queues.values().forEach(q -> {
            synchronized (q) {
                q.clear();
            }
        });
    }
}
