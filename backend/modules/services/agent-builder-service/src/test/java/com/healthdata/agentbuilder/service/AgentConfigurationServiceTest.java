package com.healthdata.agentbuilder.service;

import com.healthdata.agentbuilder.domain.entity.AgentConfiguration;
import com.healthdata.agentbuilder.domain.entity.AgentConfiguration.AgentStatus;
import com.healthdata.agentbuilder.domain.entity.AgentVersion;
import com.healthdata.agentbuilder.repository.AgentConfigurationRepository;
import com.healthdata.agentbuilder.repository.AgentVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentConfigurationServiceTest {

    @Mock
    private AgentConfigurationRepository agentRepository;

    @Mock
    private AgentVersionRepository versionRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AgentConfigurationService service;

    private String tenantId;
    private String userId;
    private AgentConfiguration testAgent;

    @BeforeEach
    void setUp() {
        tenantId = "test-tenant";
        userId = "test-user";

        // Set configuration properties
        ReflectionTestUtils.setField(service, "maxAgentsPerTenant", 50);
        ReflectionTestUtils.setField(service, "maxVersionsPerAgent", 100);

        testAgent = AgentConfiguration.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .name("Test Agent")
            .description("Test Description")
            .slug("test-agent")
            .status(AgentStatus.DRAFT)
            .version("1.0.0")
            .modelProvider("claude")
            .modelId("claude-3-sonnet-20240229")
            .systemPrompt("You are a helpful assistant")
            .build();
    }

    @Test
    void create_shouldCreateAgentSuccessfully() {
        // Given
        when(agentRepository.countByTenantId(tenantId)).thenReturn(10L);
        when(agentRepository.existsByTenantIdAndName(tenantId, testAgent.getName())).thenReturn(false);
        when(agentRepository.save(any(AgentConfiguration.class))).thenReturn(testAgent);
        when(objectMapper.convertValue(any(), eq(Map.class))).thenReturn(new HashMap<>());
        when(versionRepository.countByAgentConfigurationId(any())).thenReturn(0L);
        when(versionRepository.save(any(AgentVersion.class))).thenReturn(mock(AgentVersion.class));

        // When
        AgentConfiguration result = service.create(testAgent, userId);

        // Then
        assertNotNull(result);
        assertEquals(AgentStatus.DRAFT, result.getStatus());
        verify(agentRepository).save(any(AgentConfiguration.class));
        verify(versionRepository).save(any(AgentVersion.class));
    }

    @Test
    void create_shouldThrowExceptionWhenTenantLimitReached() {
        // Given
        when(agentRepository.countByTenantId(tenantId)).thenReturn(50L);

        // When & Then
        assertThrows(AgentConfigurationService.AgentBuilderException.class,
            () -> service.create(testAgent, userId));
        verify(agentRepository, never()).save(any());
    }

    @Test
    void create_shouldThrowExceptionWhenNameExists() {
        // Given
        when(agentRepository.countByTenantId(tenantId)).thenReturn(10L);
        when(agentRepository.existsByTenantIdAndName(tenantId, testAgent.getName())).thenReturn(true);

        // When & Then
        assertThrows(AgentConfigurationService.AgentBuilderException.class,
            () -> service.create(testAgent, userId));
        verify(agentRepository, never()).save(any());
    }

    @Test
    void update_shouldUpdateAgentSuccessfully() {
        // Given
        AgentConfiguration existing = AgentConfiguration.builder()
            .id(testAgent.getId())
            .tenantId(tenantId)
            .name("Old Name")
            .version("1.0.0")
            .status(AgentStatus.DRAFT)
            .build();

        AgentConfiguration updates = AgentConfiguration.builder()
            .tenantId(tenantId)
            .name("New Name")
            .description("New Description")
            .build();

        when(agentRepository.findByTenantIdAndId(tenantId, testAgent.getId()))
            .thenReturn(Optional.of(existing));
        when(agentRepository.save(any(AgentConfiguration.class))).thenReturn(existing);
        when(objectMapper.convertValue(any(), eq(Map.class))).thenReturn(new HashMap<>());
        when(versionRepository.countByAgentConfigurationId(any())).thenReturn(5L);
        when(versionRepository.save(any(AgentVersion.class))).thenReturn(mock(AgentVersion.class));

        // When
        AgentConfiguration result = service.update(testAgent.getId(), updates, userId, "Test update");

        // Then
        assertNotNull(result);
        assertEquals("New Name", result.getName());
        assertEquals("New Description", result.getDescription());
        verify(agentRepository).save(any(AgentConfiguration.class));
        verify(versionRepository).save(any(AgentVersion.class));
    }

    @Test
    void update_shouldThrowExceptionForArchivedAgent() {
        // Given
        AgentConfiguration archived = AgentConfiguration.builder()
            .id(testAgent.getId())
            .tenantId(tenantId)
            .status(AgentStatus.ARCHIVED)
            .build();

        when(agentRepository.findByTenantIdAndId(tenantId, testAgent.getId()))
            .thenReturn(Optional.of(archived));

        // When & Then
        assertThrows(AgentConfigurationService.AgentBuilderException.class,
            () -> service.update(testAgent.getId(), testAgent, userId, "Update"));
        verify(agentRepository, never()).save(any());
    }

    @Test
    void getById_shouldReturnAgent() {
        // Given
        when(agentRepository.findByTenantIdAndId(tenantId, testAgent.getId()))
            .thenReturn(Optional.of(testAgent));

        // When
        Optional<AgentConfiguration> result = service.getById(tenantId, testAgent.getId());

        // Then
        assertTrue(result.isPresent());
        assertEquals(testAgent.getId(), result.get().getId());
    }

    @Test
    void list_shouldReturnPageOfAgents() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<AgentConfiguration> page = new PageImpl<>(List.of(testAgent));
        when(agentRepository.findByTenantId(tenantId, pageable)).thenReturn(page);

        // When
        Page<AgentConfiguration> result = service.list(tenantId, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testAgent.getId(), result.getContent().get(0).getId());
    }

    @Test
    void publish_shouldPublishAgent() {
        // Given
        when(agentRepository.findByTenantIdAndId(tenantId, testAgent.getId()))
            .thenReturn(Optional.of(testAgent));
        when(agentRepository.save(any(AgentConfiguration.class))).thenReturn(testAgent);
        when(versionRepository.findByAgentConfigurationIdAndStatus(any(), any()))
            .thenReturn(Optional.empty());
        when(versionRepository.findLatestVersion(any())).thenReturn(Optional.empty());

        // When
        AgentConfiguration result = service.publish(tenantId, testAgent.getId(), userId);

        // Then
        assertNotNull(result);
        assertEquals(AgentStatus.ACTIVE, result.getStatus());
        assertNotNull(result.getPublishedAt());
        verify(agentRepository).save(any(AgentConfiguration.class));
    }

    @Test
    void publish_shouldThrowExceptionForArchivedAgent() {
        // Given
        testAgent.setStatus(AgentStatus.ARCHIVED);
        when(agentRepository.findByTenantIdAndId(tenantId, testAgent.getId()))
            .thenReturn(Optional.of(testAgent));

        // When & Then
        assertThrows(AgentConfigurationService.AgentBuilderException.class,
            () -> service.publish(tenantId, testAgent.getId(), userId));
    }

    @Test
    void delete_shouldArchiveAgent() {
        // Given
        when(agentRepository.findByTenantIdAndId(tenantId, testAgent.getId()))
            .thenReturn(Optional.of(testAgent));
        when(agentRepository.save(any(AgentConfiguration.class))).thenReturn(testAgent);

        // When
        service.delete(tenantId, testAgent.getId(), userId);

        // Then
        verify(agentRepository).save(argThat(agent ->
            agent.getStatus() == AgentStatus.ARCHIVED &&
            agent.getArchivedAt() != null
        ));
    }

    @Test
    void clone_shouldCloneAgentSuccessfully() {
        // Given
        String newName = "Cloned Agent";
        when(agentRepository.findByTenantIdAndId(tenantId, testAgent.getId()))
            .thenReturn(Optional.of(testAgent));
        when(agentRepository.countByTenantId(tenantId)).thenReturn(10L);
        when(agentRepository.existsByTenantIdAndName(tenantId, newName)).thenReturn(false);
        when(agentRepository.save(any(AgentConfiguration.class))).thenReturn(testAgent);
        when(objectMapper.convertValue(any(), eq(Map.class))).thenReturn(new HashMap<>());
        when(versionRepository.countByAgentConfigurationId(any())).thenReturn(0L);
        when(versionRepository.save(any(AgentVersion.class))).thenReturn(mock(AgentVersion.class));

        // When
        AgentConfiguration result = service.clone(tenantId, testAgent.getId(), newName, userId);

        // Then
        assertNotNull(result);
        // Clone calls create() which calls save() once
        verify(agentRepository, atLeastOnce()).save(any(AgentConfiguration.class));
    }

    @Test
    void getActiveAgents_shouldReturnOnlyActiveAgents() {
        // Given
        List<AgentConfiguration> activeAgents = List.of(testAgent);
        when(agentRepository.findActiveAgents(tenantId)).thenReturn(activeAgents);

        // When
        List<AgentConfiguration> result = service.getActiveAgents(tenantId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(agentRepository).findActiveAgents(tenantId);
    }
}
