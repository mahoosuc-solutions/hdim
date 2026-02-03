package com.healthdata.demo.application;

import com.healthdata.demo.domain.model.DemoScenario;
import com.healthdata.demo.domain.model.DemoSession;
import com.healthdata.demo.domain.repository.DemoScenarioRepository;
import com.healthdata.demo.domain.repository.DemoSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ScenarioLoaderService.
 */
@ExtendWith(MockitoExtension.class)
class ScenarioLoaderServiceTest {

    @Mock
    private DemoScenarioRepository scenarioRepository;

    @Mock
    private DemoSessionRepository sessionRepository;

    @Mock
    private DemoSeedingService seedingService;

    @Mock
    private DemoResetService resetService;

    private ScenarioLoaderService service;

    @Mock
    private DemoProgressService progressService;

    @BeforeEach
    void setUp() {
        service = new ScenarioLoaderService(
                scenarioRepository,
                sessionRepository,
                seedingService,
                resetService,
                progressService
        );
    }

    @Test
    @DisplayName("loadScenario should create session and generate data")
    void loadScenario_Success() {
        // Given
        String scenarioName = "hedis-evaluation";
        DemoScenario scenario = createMockScenario(scenarioName, 5000);

        when(scenarioRepository.findByName(scenarioName)).thenReturn(Optional.of(scenario));
        when(sessionRepository.findCurrentSession()).thenReturn(Optional.empty());
        // Set an ID on the session when saved so generatePatientCohort receives a valid UUID
        when(sessionRepository.save(any(DemoSession.class))).thenAnswer(i -> {
            DemoSession s = i.getArgument(0);
            if (s.getId() == null) {
                s.setId(UUID.randomUUID());
            }
            return s;
        });

        DemoSeedingService.GenerationResult genResult = new DemoSeedingService.GenerationResult();
        genResult.setSuccess(true);
        genResult.setPatientCount(5000);
        genResult.setCareGapCount(1400);
        when(seedingService.generatePatientCohort(eq(5000), anyString(), anyInt(), any(UUID.class))).thenReturn(genResult);

        // When
        ScenarioLoaderService.LoadResult result = service.loadScenario(scenarioName);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getScenarioName()).isEqualTo(scenarioName);
        assertThat(result.getPatientCount()).isEqualTo(5000);
        assertThat(result.getCareGapCount()).isEqualTo(1400);
        assertThat(result.getLoadTimeMs()).isGreaterThan(0);

        verify(resetService).resetDemoData(anyString());
        verify(sessionRepository, times(2)).save(any(DemoSession.class));
    }

    @Test
    @DisplayName("loadScenario should return error for unknown scenario")
    void loadScenario_UnknownScenario() {
        // Given
        when(scenarioRepository.findByName("unknown")).thenReturn(Optional.empty());

        // When
        ScenarioLoaderService.LoadResult result = service.loadScenario("unknown");

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Scenario not found");
    }

    @Test
    @DisplayName("loadScenario should end previous session")
    void loadScenario_EndsPreviousSession() {
        // Given
        DemoScenario scenario = createMockScenario("hedis-evaluation", 5000);
        DemoSession previousSession = new DemoSession(scenario, "Previous");

        when(scenarioRepository.findByName(anyString())).thenReturn(Optional.of(scenario));
        when(sessionRepository.findCurrentSession())
                .thenReturn(Optional.of(previousSession))
                .thenReturn(Optional.empty());
        when(sessionRepository.save(any(DemoSession.class))).thenAnswer(i -> i.getArgument(0));

        DemoSeedingService.GenerationResult genResult = new DemoSeedingService.GenerationResult();
        genResult.setSuccess(true);
        genResult.setPatientCount(5000);
        when(seedingService.generatePatientCohort(anyInt(), anyString(), anyInt(), any(UUID.class))).thenReturn(genResult);

        // When
        service.loadScenario("hedis-evaluation");

        // Then
        ArgumentCaptor<DemoSession> sessionCaptor = ArgumentCaptor.forClass(DemoSession.class);
        verify(sessionRepository, atLeastOnce()).save(sessionCaptor.capture());

        // First save should be the ended previous session
        boolean foundEndedSession = sessionCaptor.getAllValues().stream()
                .anyMatch(s -> s.getEndedAt() != null);
        assertThat(foundEndedSession).isTrue();
    }

    @Test
    @DisplayName("getCurrentScenario should return current session info")
    void getCurrentScenario_ReturnsSessionInfo() {
        // Given
        DemoScenario scenario = createMockScenario("hedis-evaluation", 5000);
        DemoSession session = new DemoSession(scenario, "Test Session");
        session.setId(UUID.randomUUID());

        when(sessionRepository.findCurrentSession()).thenReturn(Optional.of(session));

        // When
        Optional<ScenarioLoaderService.ScenarioInfo> info = service.getCurrentScenario();

        // Then
        assertThat(info).isPresent();
        assertThat(info.get().getName()).isEqualTo("hedis-evaluation");
        assertThat(info.get().getPatientCount()).isEqualTo(5000);
    }

    @Test
    @DisplayName("getCurrentScenario should return empty when no session")
    void getCurrentScenario_NoSession() {
        // Given
        when(sessionRepository.findCurrentSession()).thenReturn(Optional.empty());

        // When
        Optional<ScenarioLoaderService.ScenarioInfo> info = service.getCurrentScenario();

        // Then
        assertThat(info).isEmpty();
    }

    @Test
    @DisplayName("reloadCurrentScenario should reload active scenario")
    void reloadCurrentScenario_Success() {
        // Given
        DemoScenario scenario = createMockScenario("hedis-evaluation", 5000);
        DemoSession session = new DemoSession(scenario, "Test Session");
        session.setId(UUID.randomUUID());

        when(sessionRepository.findCurrentSession()).thenReturn(Optional.of(session));
        when(scenarioRepository.findByName("hedis-evaluation")).thenReturn(Optional.of(scenario));
        // Set an ID on the session when saved so generatePatientCohort receives a valid UUID
        when(sessionRepository.save(any(DemoSession.class))).thenAnswer(i -> {
            DemoSession s = i.getArgument(0);
            if (s.getId() == null) {
                s.setId(UUID.randomUUID());
            }
            return s;
        });

        DemoSeedingService.GenerationResult genResult = new DemoSeedingService.GenerationResult();
        genResult.setSuccess(true);
        genResult.setPatientCount(5000);
        when(seedingService.generatePatientCohort(anyInt(), anyString(), anyInt(), any(UUID.class))).thenReturn(genResult);

        // When
        ScenarioLoaderService.LoadResult result = service.reloadCurrentScenario();

        // Then
        assertThat(result.isSuccess()).isTrue();
        verify(resetService).resetDemoData(anyString());
    }

    @Test
    @DisplayName("reloadCurrentScenario should return error when no active session")
    void reloadCurrentScenario_NoSession() {
        // Given
        when(sessionRepository.findCurrentSession()).thenReturn(Optional.empty());

        // When
        ScenarioLoaderService.LoadResult result = service.reloadCurrentScenario();

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("No active session");
    }

    @Test
    @DisplayName("endCurrentSession should mark session as ended")
    void endCurrentSession_Success() {
        // Given
        DemoScenario scenario = createMockScenario("hedis-evaluation", 5000);
        DemoSession session = new DemoSession(scenario, "Test Session");
        session.setId(UUID.randomUUID());

        when(sessionRepository.findCurrentSession()).thenReturn(Optional.of(session));

        // When
        service.endCurrentSession();

        // Then
        ArgumentCaptor<DemoSession> captor = ArgumentCaptor.forClass(DemoSession.class);
        verify(sessionRepository).save(captor.capture());
        assertThat(captor.getValue().getEndedAt()).isNotNull();
    }

    // Helper methods

    private DemoScenario createMockScenario(String name, int patientCount) {
        return new DemoScenario(
                name,
                name + " Display",
                DemoScenario.ScenarioType.HEDIS_EVALUATION,
                patientCount,
                "test-tenant"
        );
    }
}
