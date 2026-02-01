package com.healthdata.cql.websocket;

import com.healthdata.cql.event.audit.CqlEvaluationAuditEvent.DataFlowStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Service for publishing data flow steps in real-time via WebSocket.
 * 
 * This component bridges DataFlowTracker and WebSocketHandler to enable
 * real-time visualization of data processing during evaluation.
 */
@Component
public class DataFlowStepPublisher {

    private static final Logger logger = LoggerFactory.getLogger(DataFlowStepPublisher.class);

    private final EvaluationProgressWebSocketHandler webSocketHandler;

    public DataFlowStepPublisher(EvaluationProgressWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    /**
     * Publish a data flow step to connected WebSocket clients
     *
     * @param step The data flow step to publish
     * @param evaluationId The evaluation ID
     * @param tenantId The tenant ID for filtering
     */
    public void publishStep(DataFlowStep step, String evaluationId, String tenantId) {
        try {
            webSocketHandler.broadcastDataFlowStep(step, evaluationId, tenantId);
            logger.debug("Published data flow step: {} for evaluation: {}", step.getStepName(), evaluationId);
        } catch (Exception e) {
            logger.error("Failed to publish data flow step: {}", e.getMessage(), e);
        }
    }
}
