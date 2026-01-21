package com.healthdata.demo.application;

import ca.uhn.fhir.context.FhirContext;
import com.healthdata.demo.client.CareGapServiceClient;
import com.healthdata.demo.client.FhirServiceClient;
import com.healthdata.demo.client.QualityMeasureServiceClient;
import com.healthdata.demo.client.UserSeedingClient;
import com.healthdata.demo.domain.model.DemoScenario;
import com.healthdata.demo.domain.repository.DemoScenarioRepository;
import com.healthdata.demo.domain.repository.DemoSessionRepository;
import com.healthdata.demo.domain.repository.SyntheticPatientTemplateRepository;
import com.healthdata.demo.generator.*;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DemoSeedingService.
 */
@ExtendWith(MockitoExtension.class)
class DemoSeedingServiceTest {

    @Mock
    private SyntheticPatientGenerator patientGenerator;

    @Mock
    private MedicationGenerator medicationGenerator;

    @Mock
    private ObservationGenerator observationGenerator;

    @Mock
    private EncounterGenerator encounterGenerator;

    @Mock
    private ProcedureGenerator procedureGenerator;

    @Mock
    private DemoScenarioRepository scenarioRepository;

    @Mock
    private DemoSessionRepository sessionRepository;

    @Mock
    private SyntheticPatientTemplateRepository templateRepository;

    @Mock
    private FhirServiceClient fhirServiceClient;

    @Mock
    private CareGapServiceClient careGapServiceClient;

    @Mock
    private QualityMeasureServiceClient qualityMeasureServiceClient;

    @Mock
    private UserSeedingClient userSeedingClient;

    @Mock
    private DemoProgressService progressService;

    private DemoSeedingService service;

    @BeforeEach
    void setUp() {
        FhirContext fhirContext = FhirContext.forR4();
        service = new DemoSeedingService(
                patientGenerator,
                medicationGenerator,
                observationGenerator,
                encounterGenerator,
                procedureGenerator,
                scenarioRepository,
                sessionRepository,
                templateRepository,
                fhirContext,
                fhirServiceClient,
                careGapServiceClient,
                qualityMeasureServiceClient,
                userSeedingClient,
                progressService,
                false  // persistToServices = false for unit tests
        );
    }

    @Test
    @DisplayName("generatePatientCohort should return successful result with correct counts")
    void generatePatientCohort_Success() {
        // Given
        int count = 10;
        String tenantId = "test-tenant";
        int careGapPercentage = 30;

        Bundle mockBundle = createMockPatientBundle(count);
        when(patientGenerator.generateCohort(count, tenantId)).thenReturn(mockBundle);

        // When
        DemoSeedingService.GenerationResult result = service.generatePatientCohort(
                count, tenantId, careGapPercentage);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPatientCount()).isEqualTo(count);
        assertThat(result.getTenantId()).isEqualTo(tenantId);
        assertThat(result.getErrorMessage()).isNull();
        assertThat(result.getGenerationTimeMs()).isGreaterThan(0);

        verify(patientGenerator).generateCohort(count, tenantId);
    }

    @Test
    @DisplayName("generatePatientCohort should calculate care gaps based on percentage")
    void generatePatientCohort_CalculatesCareGaps() {
        // Given
        int count = 100;
        int careGapPercentage = 28;
        String tenantId = "test-tenant";

        Bundle mockBundle = createMockPatientBundle(count);
        when(patientGenerator.generateCohort(count, tenantId)).thenReturn(mockBundle);

        // When
        DemoSeedingService.GenerationResult result = service.generatePatientCohort(
                count, tenantId, careGapPercentage);

        // Then
        assertThat(result.isSuccess()).isTrue();
        // Care gap count should be approximately the percentage of total patients
        assertThat(result.getCareGapCount()).isLessThanOrEqualTo(careGapPercentage);
    }

    @Test
    @DisplayName("generatePatientCohort should handle generator exception")
    void generatePatientCohort_HandlesException() {
        // Given
        when(patientGenerator.generateCohort(anyInt(), anyString()))
                .thenThrow(new RuntimeException("Generation failed"));

        // When
        DemoSeedingService.GenerationResult result = service.generatePatientCohort(
                10, "test-tenant", 30);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Generation failed");
    }

    @Test
    @DisplayName("getAvailableScenarios should return active scenarios")
    void getAvailableScenarios_ReturnsActiveScenarios() {
        // Given
        DemoScenario scenario1 = createMockScenario("hedis-evaluation");
        DemoScenario scenario2 = createMockScenario("patient-journey");
        when(scenarioRepository.findByIsActiveTrueOrderByDisplayNameAsc())
                .thenReturn(Arrays.asList(scenario1, scenario2));

        // When
        List<DemoScenario> scenarios = service.getAvailableScenarios();

        // Then
        assertThat(scenarios).hasSize(2);
        assertThat(scenarios.get(0).getName()).isEqualTo("hedis-evaluation");
        assertThat(scenarios.get(1).getName()).isEqualTo("patient-journey");
    }

    @Test
    @DisplayName("getDemoStatus should return correct status")
    void getDemoStatus_ReturnsCorrectStatus() {
        // Given
        DemoScenario scenario = createMockScenario("hedis-evaluation");
        when(scenarioRepository.findByIsActiveTrueOrderByDisplayNameAsc())
                .thenReturn(Arrays.asList(scenario));
        when(sessionRepository.findCurrentSession()).thenReturn(Optional.empty());
        when(templateRepository.findByIsActiveTrueOrderByDisplayNameAsc()).thenReturn(Arrays.asList());

        // When
        DemoSeedingService.DemoStatus status = service.getDemoStatus();

        // Then
        assertThat(status.getScenarioCount()).isEqualTo(1);
        assertThat(status.isReady()).isTrue();
        assertThat(status.getCurrentSessionId()).isNull();
    }

    @Test
    @DisplayName("initializeScenarios should create default scenarios")
    void initializeScenarios_CreatesDefaultScenarios() {
        // Given
        when(scenarioRepository.existsByName(anyString())).thenReturn(false);

        // When
        service.initializeScenarios();

        // Then
        verify(scenarioRepository, times(4)).save(any(DemoScenario.class));
    }

    @Test
    @DisplayName("initializeScenarios should not duplicate existing scenarios")
    void initializeScenarios_DoesNotDuplicateExisting() {
        // Given
        when(scenarioRepository.existsByName("hedis-evaluation")).thenReturn(true);
        when(scenarioRepository.existsByName("patient-journey")).thenReturn(true);
        when(scenarioRepository.existsByName("risk-stratification")).thenReturn(true);
        when(scenarioRepository.existsByName("multi-tenant")).thenReturn(true);

        // When
        service.initializeScenarios();

        // Then
        verify(scenarioRepository, never()).save(any(DemoScenario.class));
    }

    // Helper methods

    private Bundle createMockPatientBundle(int patientCount) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);

        for (int i = 0; i < patientCount; i++) {
            Patient patient = new Patient();
            patient.setId("patient-" + i);
            patient.addName().setFamily("TestFamily" + i).addGiven("TestGiven" + i);

            Bundle.BundleEntryComponent entry = bundle.addEntry();
            entry.setResource(patient);
        }

        return bundle;
    }

    private DemoScenario createMockScenario(String name) {
        DemoScenario scenario = new DemoScenario(
                name,
                name + " Display",
                DemoScenario.ScenarioType.HEDIS_EVALUATION,
                5000,
                "test-tenant"
        );
        return scenario;
    }
}
