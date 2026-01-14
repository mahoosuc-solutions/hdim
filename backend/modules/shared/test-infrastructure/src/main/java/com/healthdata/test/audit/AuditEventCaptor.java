package com.healthdata.test.audit;

import com.healthdata.audit.models.ai.AIAgentDecisionEvent;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Utility for capturing and asserting audit events in unit tests.
 * 
 * Usage:
 * <pre>
 * {@code
 * @Mock
 * private AIAuditEventPublisher publisher;
 * 
 * @Captor
 * private ArgumentCaptor<AIAgentDecisionEvent> eventCaptor;
 * 
 * @Test
 * void shouldPublishEvent() {
 *     // When
 *     service.doSomething();
 *     
 *     // Then
 *     verify(publisher).publishAIDecision(eventCaptor.capture());
 *     AIAgentDecisionEvent event = eventCaptor.getValue();
 *     
 *     AuditEventCaptor.verifyAgentId(event, "expected-agent-id");
 *     AuditEventCaptor.verifyTenantId(event, "tenant-123");
 * }
 * }
 * </pre>
 */
public class AuditEventCaptor {
    
    /**
     * Verify agentId is set correctly.
     */
    public static void verifyAgentId(AIAgentDecisionEvent event, String expectedAgentId) {
        assertThat(event.getAgentId())
            .as("Agent ID should be set")
            .isNotNull()
            .isEqualTo(expectedAgentId);
    }
    
    /**
     * Verify agentType is set correctly.
     */
    public static void verifyAgentType(AIAgentDecisionEvent event, AIAgentDecisionEvent.AgentType expectedType) {
        assertThat(event.getAgentType())
            .as("Agent type should be set")
            .isNotNull()
            .isEqualTo(expectedType);
    }
    
    /**
     * Verify tenantId is set correctly.
     */
    public static void verifyTenantId(AIAgentDecisionEvent event, String expectedTenantId) {
        assertThat(event.getTenantId())
            .as("Tenant ID should be set")
            .isNotNull()
            .isEqualTo(expectedTenantId);
    }
    
    /**
     * Verify decisionType is set correctly.
     */
    public static void verifyDecisionType(AIAgentDecisionEvent event, AIAgentDecisionEvent.DecisionType expectedType) {
        assertThat(event.getDecisionType())
            .as("Decision type should be set")
            .isNotNull()
            .isEqualTo(expectedType);
    }
    
    /**
     * Verify required fields are present.
     */
    public static void verifyRequiredFields(AIAgentDecisionEvent event) {
        assertThat(event.getEventId())
            .as("Event ID should be set")
            .isNotNull();
        
        assertThat(event.getTimestamp())
            .as("Timestamp should be set")
            .isNotNull();
        
        assertThat(event.getTenantId())
            .as("Tenant ID should be set")
            .isNotNull();
        
        assertThat(event.getAgentId())
            .as("Agent ID should be set")
            .isNotNull();
        
        assertThat(event.getAgentType())
            .as("Agent type should be set")
            .isNotNull();
        
        assertThat(event.getDecisionType())
            .as("Decision type should be set")
            .isNotNull();
    }
    
    /**
     * Verify confidence score is within valid range.
     */
    public static void verifyConfidenceScore(AIAgentDecisionEvent event) {
        if (event.getConfidenceScore() != null) {
            assertThat(event.getConfidenceScore())
                .as("Confidence score should be between 0.0 and 1.0")
                .isBetween(0.0, 1.0);
        }
    }
    
    /**
     * Verify all captured events have required fields.
     */
    public static void verifyAllEvents(List<AIAgentDecisionEvent> events) {
        assertThat(events)
            .as("Should have captured events")
            .isNotEmpty();
        
        events.forEach(AuditEventCaptor::verifyRequiredFields);
    }
}

