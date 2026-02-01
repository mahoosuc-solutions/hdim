package com.healthdata.demo.orchestrator.integration;

import com.healthdata.demo.orchestrator.model.DevOpsLogMessage;
import com.healthdata.demo.orchestrator.model.DevOpsStatusUpdate;
import com.healthdata.demo.orchestrator.websocket.DevOpsLogWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DevOpsAgentClient WebSocket publishing.
 *
 * Tests verify:
 * - Log publishing to WebSocket clients
 * - Status update publishing
 * - Tenant ID injection
 * - Error resilience (WebSocket failures don't break operations)
 */
@ExtendWith(MockitoExtension.class)
class DevOpsAgentClientTest {

    @Mock
    private DevOpsLogWebSocketHandler webSocketHandler;

    @Captor
    private ArgumentCaptor<DevOpsLogMessage> logCaptor;

    @Captor
    private ArgumentCaptor<DevOpsStatusUpdate> statusCaptor;

    private DevOpsAgentClient devOpsAgentClient;

    private static final String DEMO_TENANT_ID = "demo-tenant";
    private static final String DEVOPS_AGENT_URL = "http://devops-agent-service:8090";

    @BeforeEach
    void setUp() {
        devOpsAgentClient = new DevOpsAgentClient(
            DEVOPS_AGENT_URL,
            DEMO_TENANT_ID,
            webSocketHandler
        );
    }

    @Test
    void publishLog_ShouldPublishToWebSocketWithTenantId() {
        // When
        devOpsAgentClient.publishLog("INFO", "Data seeding started", "SEED");

        // Then: WebSocket handler called with correct log message
        verify(webSocketHandler).publishLog(logCaptor.capture());

        DevOpsLogMessage logMessage = logCaptor.getValue();
        assertThat(logMessage.getLevel()).isEqualTo("INFO");
        assertThat(logMessage.getMessage()).isEqualTo("Data seeding started");
        assertThat(logMessage.getCategory()).isEqualTo("SEED");
        assertThat(logMessage.getTenantId()).isEqualTo(DEMO_TENANT_ID);
        assertThat(logMessage.getComponent()).isEqualTo("demo-orchestrator");
        assertThat(logMessage.getTimestamp()).isNotNull();
    }

    @Test
    void publishLog_ShouldPublishAllLogLevels() {
        // When: Different log levels
        devOpsAgentClient.publishLog("DEBUG", "Debug message", "SEED");
        devOpsAgentClient.publishLog("INFO", "Info message", "CLEAR");
        devOpsAgentClient.publishLog("WARN", "Warning message", "VALIDATION");
        devOpsAgentClient.publishLog("ERROR", "Error message", "DEPLOY");

        // Then: All levels published
        verify(webSocketHandler, times(4)).publishLog(any(DevOpsLogMessage.class));
    }

    @Test
    void publishLog_ShouldContinueOnWebSocketFailure() {
        // Given: WebSocket handler throws exception
        doThrow(new RuntimeException("WebSocket connection lost"))
            .when(webSocketHandler).publishLog(any(DevOpsLogMessage.class));

        // When: Log published
        devOpsAgentClient.publishLog("INFO", "Test message", "SEED");

        // Then: No exception propagated (error handled gracefully)
        verify(webSocketHandler).publishLog(any(DevOpsLogMessage.class));
    }

    @Test
    void updateStatus_ShouldPublishToWebSocketWithDetails() {
        // Given: Status details
        Map<String, Object> details = Map.of(
            "totalChecks", 10,
            "passedChecks", 8,
            "failedChecks", 2,
            "warningChecks", 0
        );

        // When
        devOpsAgentClient.updateStatus("FHIR_VALIDATION", "COMPLETED", details);

        // Then: WebSocket handler called with correct status update
        verify(webSocketHandler).publishStatus(statusCaptor.capture());

        DevOpsStatusUpdate statusUpdate = statusCaptor.getValue();
        assertThat(statusUpdate.getComponent()).isEqualTo("FHIR_VALIDATION");
        assertThat(statusUpdate.getStatus()).isEqualTo("COMPLETED");
        assertThat(statusUpdate.getTenantId()).isEqualTo(DEMO_TENANT_ID);
        assertThat(statusUpdate.getDetails()).isEqualTo(details);
        assertThat(statusUpdate.getTimestamp()).isNotNull();
    }

    @Test
    void updateStatus_ShouldPublishAllStatuses() {
        // When: Different statuses
        devOpsAgentClient.updateStatus("DATA_SEEDING", "PENDING", Map.of());
        devOpsAgentClient.updateStatus("DATA_SEEDING", "IN_PROGRESS", Map.of("progress", 50));
        devOpsAgentClient.updateStatus("DATA_SEEDING", "COMPLETED", Map.of("entitiesCreated", 100));
        devOpsAgentClient.updateStatus("DEPLOYMENT", "FAILED", Map.of("error", "Connection timeout"));

        // Then: All statuses published
        verify(webSocketHandler, times(4)).publishStatus(any(DevOpsStatusUpdate.class));
    }

    @Test
    void updateStatus_ShouldContinueOnWebSocketFailure() {
        // Given: WebSocket handler throws exception
        doThrow(new RuntimeException("WebSocket connection lost"))
            .when(webSocketHandler).publishStatus(any(DevOpsStatusUpdate.class));

        // When: Status update published
        devOpsAgentClient.updateStatus("DEPLOYMENT", "COMPLETED", Map.of());

        // Then: No exception propagated
        verify(webSocketHandler).publishStatus(any(DevOpsStatusUpdate.class));
    }

    @Test
    void publishLog_ShouldIncludeComponentInformation() {
        // When
        devOpsAgentClient.publishLog("INFO", "Operation started", "CLEAR");

        // Then
        verify(webSocketHandler).publishLog(logCaptor.capture());
        assertThat(logCaptor.getValue().getComponent()).isEqualTo("demo-orchestrator");
    }

    @Test
    void updateStatus_ShouldHandleEmptyDetails() {
        // When: Empty details map
        devOpsAgentClient.updateStatus("HEALTH_CHECK", "PASS", Map.of());

        // Then: Status update published successfully
        verify(webSocketHandler).publishStatus(statusCaptor.capture());
        assertThat(statusCaptor.getValue().getDetails()).isEmpty();
    }
}
