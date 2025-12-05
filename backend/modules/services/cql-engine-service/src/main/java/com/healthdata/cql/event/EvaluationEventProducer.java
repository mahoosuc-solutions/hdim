package com.healthdata.cql.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for publishing CQL evaluation events to Kafka.
 * Events are published asynchronously to avoid blocking evaluation processing.
 */
@Service
public class EvaluationEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(EvaluationEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${visualization.kafka.topics.evaluation-started:evaluation.started}")
    private String evaluationStartedTopic;

    @Value("${visualization.kafka.topics.evaluation-completed:evaluation.completed}")
    private String evaluationCompletedTopic;

    @Value("${visualization.kafka.topics.evaluation-failed:evaluation.failed}")
    private String evaluationFailedTopic;

    @Value("${visualization.kafka.topics.batch-progress:batch.progress}")
    private String batchProgressTopic;

    public EvaluationEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publish an evaluation started event.
     * Key: tenantId for partitioning by tenant
     */
    public void publishEvaluationStarted(EvaluationStartedEvent event) {
        publishEvent(evaluationStartedTopic, event.getTenantId(), event);
    }

    /**
     * Publish an evaluation completed event.
     * Key: tenantId for partitioning by tenant
     */
    public void publishEvaluationCompleted(EvaluationCompletedEvent event) {
        publishEvent(evaluationCompletedTopic, event.getTenantId(), event);
    }

    /**
     * Publish an evaluation failed event.
     * Key: tenantId for partitioning by tenant
     */
    public void publishEvaluationFailed(EvaluationFailedEvent event) {
        publishEvent(evaluationFailedTopic, event.getTenantId(), event);
    }

    /**
     * Publish a batch progress event.
     * Key: batchId to ensure all progress events for a batch go to the same partition
     */
    public void publishBatchProgress(BatchProgressEvent event) {
        publishEvent(batchProgressTopic, event.getBatchId(), event);
    }

    /**
     * Generic method to publish an event to Kafka.
     * Handles asynchronous sending and error logging.
     *
     * @param topic The Kafka topic
     * @param key   The partition key
     * @param event The event to publish
     */
    private void publishEvent(String topic, String key, Object event) {
        try {
            CompletableFuture<SendResult<String, Object>> future =
                    kafkaTemplate.send(topic, key, event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.debug("Published event to topic {}: partition={}, offset={}, key={}",
                            topic,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            key);
                } else {
                    logger.error("Failed to publish event to topic {}: key={}, error={}",
                            topic, key, ex.getMessage(), ex);
                }
            });
        } catch (Exception e) {
            logger.error("Exception while publishing event to topic {}: key={}, error={}",
                    topic, key, e.getMessage(), e);
        }
    }

    /**
     * Get topic names for debugging/monitoring
     */
    public String getEvaluationStartedTopic() {
        return evaluationStartedTopic;
    }

    public String getEvaluationCompletedTopic() {
        return evaluationCompletedTopic;
    }

    public String getEvaluationFailedTopic() {
        return evaluationFailedTopic;
    }

    public String getBatchProgressTopic() {
        return batchProgressTopic;
    }
}
