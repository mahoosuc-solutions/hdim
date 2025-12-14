package com.healthdata.agentbuilder.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.agentbuilder.client.AgentRuntimeClient;
import com.healthdata.agentbuilder.domain.entity.AgentConfiguration;
import com.healthdata.agentbuilder.domain.entity.AgentConfiguration.AgentStatus;
import com.healthdata.agentbuilder.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AgentBuilderController.
 * Uses Mockito directly without Spring context for faster execution.
 */
@ExtendWith(MockitoExtension.class)
class AgentBuilderControllerTest {

    @Mock
    private AgentConfigurationService agentService;

    @Mock
    private AgentVersionService versionService;

    @Mock
    private AgentTestService testService;

    @Mock
    private PromptTemplateService templateService;

    @Mock
    private AgentRuntimeClient runtimeClient;

    @InjectMocks
    private AgentBuilderController controller;

    private String tenantId;
    private String userId;
    private AgentConfiguration testAgent;

    @BeforeEach
    void setUp() {
        tenantId = "test-tenant";
        userId = "user-123";

        testAgent = AgentConfiguration.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .name("Test Agent")
            .slug("test-agent")
            .description("Test Description")
            .status(AgentStatus.DRAFT)
            .version("1.0.0")
            .modelProvider("claude")
            .modelId("claude-3-sonnet-20240229")
            .systemPrompt("You are a helpful assistant")
            .build();
    }

    @Test
    void createAgent_shouldReturn201() {
        // Given
        when(agentService.create(any(AgentConfiguration.class), eq(userId)))
            .thenReturn(testAgent);

        // When
        ResponseEntity<AgentConfiguration> response = controller.createAgent(
            testAgent, tenantId, userId
        );

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testAgent.getId(), response.getBody().getId());
        assertEquals("Test Agent", response.getBody().getName());
        verify(agentService).create(any(AgentConfiguration.class), eq(userId));
    }

    @Test
    void getAgent_shouldReturn200WhenFound() {
        // Given
        when(agentService.getById(tenantId, testAgent.getId()))
            .thenReturn(Optional.of(testAgent));

        // When
        ResponseEntity<AgentConfiguration> response = controller.getAgent(
            testAgent.getId(), tenantId
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testAgent.getId(), response.getBody().getId());
    }

    @Test
    void getAgent_shouldReturn404WhenNotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(agentService.getById(tenantId, nonExistentId))
            .thenReturn(Optional.empty());

        // When
        ResponseEntity<AgentConfiguration> response = controller.getAgent(
            nonExistentId, tenantId
        );

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void listAgents_shouldReturnPageOfAgents() {
        // Given
        Page<AgentConfiguration> page = new PageImpl<>(
            List.of(testAgent),
            PageRequest.of(0, 20),
            1
        );
        when(agentService.list(eq(tenantId), any()))
            .thenReturn(page);

        // When
        ResponseEntity<Page<AgentConfiguration>> response = controller.listAgents(
            tenantId, PageRequest.of(0, 20)
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        assertEquals("Test Agent", response.getBody().getContent().get(0).getName());
    }

    @Test
    void updateAgent_shouldReturn200() {
        // Given
        when(agentService.update(eq(testAgent.getId()), any(), eq(userId), eq("Updated name")))
            .thenReturn(testAgent);

        AgentConfiguration updates = AgentConfiguration.builder()
            .name("Updated Name")
            .build();

        // When
        ResponseEntity<AgentConfiguration> response = controller.updateAgent(
            testAgent.getId(), updates, "Updated name", tenantId, userId
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(agentService).update(eq(testAgent.getId()), any(), eq(userId), eq("Updated name"));
    }

    @Test
    void publishAgent_shouldReturn200() {
        // Given
        testAgent.setStatus(AgentStatus.ACTIVE);
        when(agentService.publish(tenantId, testAgent.getId(), "user-123"))
            .thenReturn(testAgent);

        // When
        ResponseEntity<AgentConfiguration> response = controller.publishAgent(
            testAgent.getId(), tenantId, userId
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(AgentStatus.ACTIVE, response.getBody().getStatus());
    }

    @Test
    void deleteAgent_shouldReturn204() {
        // Given
        doNothing().when(agentService).delete(tenantId, testAgent.getId(), "user-123");

        // When
        ResponseEntity<Void> response = controller.deleteAgent(
            testAgent.getId(), tenantId, userId
        );

        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(agentService).delete(tenantId, testAgent.getId(), "user-123");
    }

    @Test
    void cloneAgent_shouldReturn201() {
        // Given
        AgentConfiguration cloned = AgentConfiguration.builder()
            .id(UUID.randomUUID())
            .name("Cloned Agent")
            .build();
        when(agentService.clone(tenantId, testAgent.getId(), "Cloned Agent", "user-123"))
            .thenReturn(cloned);

        // When
        ResponseEntity<AgentConfiguration> response = controller.cloneAgent(
            testAgent.getId(), "Cloned Agent", tenantId, userId
        );

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Cloned Agent", response.getBody().getName());
    }

    @Test
    void searchAgents_shouldReturnMatchingAgents() {
        // Given
        Page<AgentConfiguration> page = new PageImpl<>(List.of(testAgent));
        when(agentService.search(eq(tenantId), eq("test"), any()))
            .thenReturn(page);

        // When
        ResponseEntity<Page<AgentConfiguration>> response = controller.searchAgents(
            "test", tenantId, PageRequest.of(0, 20)
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
    }

    @Test
    void getAvailableTools_shouldReturnTools() {
        // Given
        List<AgentRuntimeClient.ToolInfo> tools = List.of(
            new AgentRuntimeClient.ToolInfo(
                "fhir_query",
                "Query FHIR resources",
                "data",
                Map.of(),
                false
            )
        );
        when(runtimeClient.getAvailableTools(tenantId)).thenReturn(tools);

        // When
        ResponseEntity<List<AgentRuntimeClient.ToolInfo>> response = controller.getAvailableTools(
            tenantId
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("fhir_query", response.getBody().get(0).name());
    }

    @Test
    void getSupportedProviders_shouldReturnProviders() {
        // Given
        List<AgentRuntimeClient.ProviderInfo> providers = List.of(
            new AgentRuntimeClient.ProviderInfo(
                "claude",
                "Anthropic Claude",
                true,
                true,
                List.of("us-east-1")
            )
        );
        when(runtimeClient.getSupportedProviders()).thenReturn(providers);

        // When
        ResponseEntity<List<AgentRuntimeClient.ProviderInfo>> response = controller.getSupportedProviders();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("claude", response.getBody().get(0).name());
    }

    @Test
    void deprecateAgent_shouldReturn200() {
        // Given
        testAgent.setStatus(AgentStatus.DEPRECATED);
        when(agentService.deprecate(tenantId, testAgent.getId(), "user-123"))
            .thenReturn(testAgent);

        // When
        ResponseEntity<AgentConfiguration> response = controller.deprecateAgent(
            testAgent.getId(), tenantId, userId
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(AgentStatus.DEPRECATED, response.getBody().getStatus());
    }

    @Test
    void getActiveAgents_shouldReturnList() {
        // Given
        when(agentService.getActiveAgents(tenantId))
            .thenReturn(List.of(testAgent));

        // When
        ResponseEntity<List<AgentConfiguration>> response = controller.getActiveAgents(tenantId);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void checkRuntimeHealth_shouldReturnHealth() {
        // Given
        Map<String, Object> health = Map.of("status", "UP");
        when(runtimeClient.healthCheck()).thenReturn(health);

        // When
        ResponseEntity<Map<String, Object>> response = controller.checkRuntimeHealth();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
    }
}
