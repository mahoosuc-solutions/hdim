package com.healthdata.cql.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.cql.event.*;
import com.healthdata.cql.websocket.EvaluationProgressWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Kafka consumer that bridges evaluation events to WebSocket clients.
 *
 * This service listens to all evaluation event topics and forwards them
 * to connected WebSocket clients in real-time.
 *
 * Flow: Kafka Topics → Consumer → WebSocket Handler → Frontend Clients
 */
@Service
@ConditionalOnProperty(name = "visualization.kafka.enabled", havingValue = "true", matchIfMissing = false)
public class EvaluationEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(EvaluationEventConsumer.class);

    private final EvaluationProgressWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    public EvaluationEventConsumer(
            EvaluationProgressWebSocketHandler webSocketHandler,
            ObjectMapper objectMapper) {
        this.webSocketHandler = webSocketHandler;
        this.objectMapper = objectMapper;
    }

    /**
     * Consume batch progress events (PRIMARY VISUALIZATION EVENT)
     * These events are emitted every 5 seconds or 10 patients during batch evaluation
     */
    @KafkaListener(
            topics = "${visualization.kafka.topics.batch-progress}",
            groupId = "cql-engine-visualization-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeBatchProgress(String message) {
        try {
            // Parse the message to extract tenant ID
            Map<String, Object> eventMap = objectMapper.readValue(message, Map.class);
            String tenantId = (String) eventMap.get("tenantId");

            logger.debug("Received batch progress event: batchId={}, tenantId={}, completed={}/{}",
                    eventMap.get("batchId"),
                    tenantId,
                    eventMap.get("completedCount"),
                    eventMap.get("totalPatients"));

            // Broadcast to WebSocket clients (with tenant filtering)
            webSocketHandler.broadcastEvent(eventMap, tenantId);

        } catch (Exception e) {
            logger.error("Error processing batch progress event: {}", e.getMessage(), e);
        }
    }

    /**
     * Consume evaluation started events
     */
    @KafkaListener(
            topics = "${visualization.kafka.topics.evaluation-started}",
            groupId = "cql-engine-visualization-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeEvaluationStarted(String message) {
        try {
            Map<String, Object> eventMap = objectMapper.readValue(message, Map.class);
            String tenantId = (String) eventMap.get("tenantId");

            logger.debug("Received evaluation started event: evaluationId={}, patientId={}",
                    eventMap.get("evaluationId"),
                    eventMap.get("patientId"));

            webSocketHandler.broadcastEvent(eventMap, tenantId);

        } catch (Exception e) {
            logger.error("Error processing evaluation started event: {}", e.getMessage(), e);
        }
    }

    /**
     * Consume evaluation completed events
     */
    @KafkaListener(
            topics = "${visualization.kafka.topics.evaluation-completed}",
            groupId = "cql-engine-visualization-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeEvaluationCompleted(String message) {
        try {
            Map<String, Object> eventMap = objectMapper.readValue(message, Map.class);
            String tenantId = (String) eventMap.get("tenantId");

            logger.debug("Received evaluation completed event: evaluationId={}, inNumerator={}, durationMs={}",
                    eventMap.get("evaluationId"),
                    eventMap.get("inNumerator"),
                    eventMap.get("durationMs"));

            webSocketHandler.broadcastEvent(eventMap, tenantId);

        } catch (Exception e) {
            logger.error("Error processing evaluation completed event: {}", e.getMessage(), e);
        }
    }

    /**
     * Consume evaluation failed events
     */
    @KafkaListener(
            topics = "${visualization.kafka.topics.evaluation-failed}",
            groupId = "cql-engine-visualization-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeEvaluationFailed(String message) {
        try {
            Map<String, Object> eventMap = objectMapper.readValue(message, Map.class);
            String tenantId = (String) eventMap.get("tenantId");

            logger.warn("Received evaluation failed event: evaluationId={}, errorType={}, errorMessage={}",
                    eventMap.get("evaluationId"),
                    eventMap.get("errorType"),
                    eventMap.get("errorMessage"));

            webSocketHandler.broadcastEvent(eventMap, tenantId);

        } catch (Exception e) {
            logger.error("Error processing evaluation failed event: {}", e.getMessage(), e);
        }
    }
}
