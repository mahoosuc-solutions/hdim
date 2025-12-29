package com.healthdata.agent.agents;

import com.healthdata.agent.core.AgentContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AgentRegistry Tests")
class AgentRegistryTest {

    @Mock
    private AgentDefinition clinicalAgent;

    @Mock
    private AgentDefinition careGapAgent;

    private AgentRegistry registry;

    @BeforeEach
    void setUp() {
        lenient().when(clinicalAgent.getAgentType()).thenReturn("clinical-decision");
        lenient().when(clinicalAgent.getDisplayName()).thenReturn("Clinical Decision Support");
        lenient().when(clinicalAgent.getDescription()).thenReturn("AI-powered clinical decision support");
        lenient().when(clinicalAgent.getEnabledTools()).thenReturn(List.of("fhir_query", "cql_execute"));
        lenient().when(clinicalAgent.getRequiredRoles()).thenReturn(List.of());

        lenient().when(careGapAgent.getAgentType()).thenReturn("care-gap-optimizer");
        lenient().when(careGapAgent.getDisplayName()).thenReturn("Care Gap Optimizer");
        lenient().when(careGapAgent.getDescription()).thenReturn("Optimize care gap closure");
        lenient().when(careGapAgent.getEnabledTools()).thenReturn(List.of("care_gap_query"));
        lenient().when(careGapAgent.getRequiredRoles()).thenReturn(List.of());

        registry = new AgentRegistry(List.of(clinicalAgent, careGapAgent));
    }

    @Nested
    @DisplayName("Agent Registration")
    class RegistrationTests {

        @Test
        @DisplayName("should register agents on initialization")
        void registerAgentsOnInit() {
            assertThat(registry.getAgentCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should list all agents")
        void listAgents() {
            List<AgentDefinition> agents = registry.listAgents();

            assertThat(agents).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Agent Retrieval")
    class RetrievalTests {

        @Test
        @DisplayName("should get agent by type")
        void getAgentByType() {
            Optional<AgentDefinition> agent = registry.getAgent("clinical-decision");

            assertThat(agent).isPresent();
            assertThat(agent.get().getAgentType()).isEqualTo("clinical-decision");
            assertThat(agent.get().getDisplayName()).isEqualTo("Clinical Decision Support");
        }

        @Test
        @DisplayName("should return empty for unknown agent")
        void getUnknownAgent() {
            Optional<AgentDefinition> agent = registry.getAgent("unknown");

            assertThat(agent).isEmpty();
        }

        @Test
        @DisplayName("should get agent or throw when exists")
        void getAgentOrThrowExists() {
            AgentDefinition agent = registry.getAgentOrThrow("care-gap-optimizer");

            assertThat(agent).isNotNull();
            assertThat(agent.getAgentType()).isEqualTo("care-gap-optimizer");
        }

        @Test
        @DisplayName("should throw when agent doesn't exist")
        void getAgentOrThrowNotExists() {
            assertThatThrownBy(() -> registry.getAgentOrThrow("unknown"))
                .isInstanceOf(AgentRegistry.AgentNotFoundException.class)
                .hasMessageContaining("unknown");
        }
    }

    @Nested
    @DisplayName("Agent Availability")
    class AvailabilityTests {

        @Test
        @DisplayName("should check if agent exists")
        void hasAgent() {
            assertThat(registry.hasAgent("clinical-decision")).isTrue();
            assertThat(registry.hasAgent("care-gap-optimizer")).isTrue();
            assertThat(registry.hasAgent("unknown")).isFalse();
        }
    }

    @Nested
    @DisplayName("Agent Info")
    class AgentInfoTests {

        @Test
        @DisplayName("should get agent info list")
        void getAgentInfoList() {
            List<AgentRegistry.AgentInfo> infos = registry.getAgentInfoList();

            assertThat(infos).hasSize(2);
            assertThat(infos)
                .extracting(AgentRegistry.AgentInfo::agentType)
                .containsExactlyInAnyOrder("clinical-decision", "care-gap-optimizer");
        }

        @Test
        @DisplayName("should include display name in info")
        void agentInfoHasDisplayName() {
            List<AgentRegistry.AgentInfo> infos = registry.getAgentInfoList();

            AgentRegistry.AgentInfo clinicalInfo = infos.stream()
                .filter(i -> i.agentType().equals("clinical-decision"))
                .findFirst()
                .orElseThrow();

            assertThat(clinicalInfo.displayName()).isEqualTo("Clinical Decision Support");
            assertThat(clinicalInfo.description()).isEqualTo("AI-powered clinical decision support");
        }

        @Test
        @DisplayName("should include enabled tools in info")
        void agentInfoHasEnabledTools() {
            List<AgentRegistry.AgentInfo> infos = registry.getAgentInfoList();

            AgentRegistry.AgentInfo clinicalInfo = infos.stream()
                .filter(i -> i.agentType().equals("clinical-decision"))
                .findFirst()
                .orElseThrow();

            assertThat(clinicalInfo.enabledTools()).containsExactly("fhir_query", "cql_execute");
        }
    }

    @Nested
    @DisplayName("Context-Based Access")
    class ContextAccessTests {

        @Test
        @DisplayName("should get agent with context")
        void getAgentWithContext() {
            AgentContext context = AgentContext.builder()
                .tenantId("tenant-123")
                .userId("user-456")
                .build();

            Optional<AgentDefinition> agent = registry.getAgent("clinical-decision", context);

            assertThat(agent).isPresent();
        }

        @Test
        @DisplayName("should list available agents for context")
        void listAvailableAgentsForContext() {
            when(clinicalAgent.isAvailable(org.mockito.ArgumentMatchers.any())).thenReturn(true);
            when(careGapAgent.isAvailable(org.mockito.ArgumentMatchers.any())).thenReturn(true);

            AgentContext context = AgentContext.builder()
                .tenantId("tenant-123")
                .userId("user-456")
                .roles(Set.of("ADMIN"))
                .build();

            List<AgentDefinition> available = registry.listAvailableAgents(context);

            assertThat(available).hasSize(2);
        }

        @Test
        @DisplayName("should filter agents by availability")
        void filterAgentsByAvailability() {
            when(clinicalAgent.isAvailable(org.mockito.ArgumentMatchers.any())).thenReturn(true);
            when(careGapAgent.isAvailable(org.mockito.ArgumentMatchers.any())).thenReturn(false);

            AgentContext context = AgentContext.builder()
                .tenantId("tenant-123")
                .userId("user-456")
                .build();

            List<AgentDefinition> available = registry.listAvailableAgents(context);

            assertThat(available).hasSize(1);
            assertThat(available.get(0).getAgentType()).isEqualTo("clinical-decision");
        }
    }
}
