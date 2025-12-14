package com.healthdata.ecr.service;

import com.healthdata.ecr.persistence.*;
import com.healthdata.ecr.persistence.ElectronicCaseReportEntity.*;
import com.healthdata.ecr.trigger.RctcRulesEngine;
import com.healthdata.ecr.trigger.RctcRulesEngine.TriggerMatch;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EcrTriggerService.
 * Tests RCTC trigger detection and eCR creation logic.
 */
@ExtendWith(MockitoExtension.class)
class EcrTriggerServiceTest {

    @Mock
    private RctcRulesEngine rctcRulesEngine;

    @Mock
    private ElectronicCaseReportRepository ecrRepository;

    @Mock
    private EcrProcessingService processingService;

    @InjectMocks
    private EcrTriggerService ecrTriggerService;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID ENCOUNTER_ID = UUID.randomUUID();

    @Nested
    @DisplayName("handleConditionCreated() tests")
    class HandleConditionCreatedTests {

        @Test
        @DisplayName("Should create eCR for reportable diagnosis")
        void handleConditionCreated_withReportableDiagnosis_shouldCreateEcr() {
            // Arrange
            Map<String, Object> event = createConditionEvent("U07.1", "COVID-19");

            TriggerMatch match = TriggerMatch.builder()
                .code("U07.1")
                .codeSystem(RctcRulesEngine.ICD10CM_OID)
                .display("COVID-19")
                .conditionName("COVID-19")
                .urgency(RctcTriggerCodeEntity.Urgency.WITHIN_24_HOURS)
                .triggerType(RctcTriggerCodeEntity.TriggerType.DIAGNOSIS)
                .build();

            when(rctcRulesEngine.isReportableTrigger(eq("U07.1"), anyString())).thenReturn(true);
            when(rctcRulesEngine.evaluateDiagnosis("U07.1")).thenReturn(Optional.of(match));
            when(ecrRepository.existsPendingForTrigger(anyString(), any(UUID.class), anyString(), any(LocalDateTime.class)))
                .thenReturn(false);
            when(ecrRepository.save(any(ElectronicCaseReportEntity.class)))
                .thenAnswer(inv -> {
                    ElectronicCaseReportEntity ecr = inv.getArgument(0);
                    ecr.setId(UUID.randomUUID());
                    return ecr;
                });

            // Act
            ecrTriggerService.handleConditionCreated(event);

            // Assert
            ArgumentCaptor<ElectronicCaseReportEntity> captor = ArgumentCaptor.forClass(ElectronicCaseReportEntity.class);
            verify(ecrRepository).save(captor.capture());

            ElectronicCaseReportEntity savedEcr = captor.getValue();
            assertThat(savedEcr.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(savedEcr.getPatientId()).isEqualTo(PATIENT_ID);
            assertThat(savedEcr.getTriggerCode()).isEqualTo("U07.1");
            assertThat(savedEcr.getTriggerCategory()).isEqualTo(TriggerCategory.DIAGNOSIS);
            assertThat(savedEcr.getConditionName()).isEqualTo("COVID-19");
            assertThat(savedEcr.getStatus()).isEqualTo(EcrStatus.PENDING);
            assertThat(savedEcr.getUrgency()).isEqualTo(EcrUrgency.WITHIN_24_HOURS);
        }

        @Test
        @DisplayName("Should not create eCR for non-reportable diagnosis")
        void handleConditionCreated_withNonReportable_shouldNotCreateEcr() {
            // Arrange
            Map<String, Object> event = createConditionEvent("J06.9", "Common cold");

            when(rctcRulesEngine.isReportableTrigger(eq("J06.9"), anyString())).thenReturn(false);

            // Act
            ecrTriggerService.handleConditionCreated(event);

            // Assert
            verify(ecrRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should skip duplicate eCR within window")
        void handleConditionCreated_withDuplicate_shouldNotCreateEcr() {
            // Arrange
            Map<String, Object> event = createConditionEvent("U07.1", "COVID-19");

            when(rctcRulesEngine.isReportableTrigger(eq("U07.1"), anyString())).thenReturn(true);
            when(rctcRulesEngine.evaluateDiagnosis("U07.1")).thenReturn(Optional.of(createTriggerMatch()));
            when(ecrRepository.existsPendingForTrigger(anyString(), any(UUID.class), anyString(), any(LocalDateTime.class)))
                .thenReturn(true); // Duplicate exists

            // Act
            ecrTriggerService.handleConditionCreated(event);

            // Assert
            verify(ecrRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should trigger immediate processing for IMMEDIATE urgency")
        void handleConditionCreated_withImmediateUrgency_shouldProcessImmediately() {
            // Arrange
            Map<String, Object> event = createConditionEvent("A22.9", "Anthrax");

            TriggerMatch match = TriggerMatch.builder()
                .code("A22.9")
                .codeSystem(RctcRulesEngine.ICD10CM_OID)
                .display("Anthrax")
                .conditionName("Anthrax")
                .urgency(RctcTriggerCodeEntity.Urgency.IMMEDIATE)
                .triggerType(RctcTriggerCodeEntity.TriggerType.DIAGNOSIS)
                .build();

            UUID ecrId = UUID.randomUUID();

            when(rctcRulesEngine.isReportableTrigger(eq("A22.9"), anyString())).thenReturn(true);
            when(rctcRulesEngine.evaluateDiagnosis("A22.9")).thenReturn(Optional.of(match));
            when(ecrRepository.existsPendingForTrigger(anyString(), any(UUID.class), anyString(), any(LocalDateTime.class)))
                .thenReturn(false);
            when(ecrRepository.save(any(ElectronicCaseReportEntity.class)))
                .thenAnswer(inv -> {
                    ElectronicCaseReportEntity ecr = inv.getArgument(0);
                    ecr.setId(ecrId);
                    return ecr;
                });

            // Act
            ecrTriggerService.handleConditionCreated(event);

            // Assert
            verify(processingService).processImmediately(ecrId);
        }
    }

    @Nested
    @DisplayName("handleObservationCreated() tests")
    class HandleObservationCreatedTests {

        @Test
        @DisplayName("Should create eCR for reportable lab result")
        void handleObservationCreated_withReportableLab_shouldCreateEcr() {
            // Arrange
            Map<String, Object> event = createLabEvent("94500-6", "SARS-CoV-2 RNA");

            TriggerMatch match = TriggerMatch.builder()
                .code("94500-6")
                .codeSystem(RctcRulesEngine.LOINC_OID)
                .display("SARS-CoV-2 RNA")
                .conditionName("COVID-19")
                .urgency(RctcTriggerCodeEntity.Urgency.WITHIN_24_HOURS)
                .triggerType(RctcTriggerCodeEntity.TriggerType.LAB_RESULT)
                .build();

            when(rctcRulesEngine.isReportableTrigger(eq("94500-6"), anyString())).thenReturn(true);
            when(rctcRulesEngine.evaluateLabResult("94500-6")).thenReturn(Optional.of(match));
            when(ecrRepository.existsPendingForTrigger(anyString(), any(UUID.class), anyString(), any(LocalDateTime.class)))
                .thenReturn(false);
            when(ecrRepository.save(any(ElectronicCaseReportEntity.class)))
                .thenAnswer(inv -> {
                    ElectronicCaseReportEntity ecr = inv.getArgument(0);
                    ecr.setId(UUID.randomUUID());
                    return ecr;
                });

            // Act
            ecrTriggerService.handleObservationCreated(event);

            // Assert
            ArgumentCaptor<ElectronicCaseReportEntity> captor = ArgumentCaptor.forClass(ElectronicCaseReportEntity.class);
            verify(ecrRepository).save(captor.capture());

            ElectronicCaseReportEntity savedEcr = captor.getValue();
            assertThat(savedEcr.getTriggerCategory()).isEqualTo(TriggerCategory.LAB_RESULT);
            assertThat(savedEcr.getTriggerCode()).isEqualTo("94500-6");
        }

        @Test
        @DisplayName("Should ignore non-laboratory observations")
        void handleObservationCreated_withNonLabObservation_shouldIgnore() {
            // Arrange
            Map<String, Object> event = new HashMap<>();
            event.put("tenantId", TENANT_ID);
            event.put("patientId", PATIENT_ID.toString());
            event.put("code", "8867-4");
            event.put("display", "Heart rate");
            event.put("category", "vital-signs"); // Not laboratory

            // Act
            ecrTriggerService.handleObservationCreated(event);

            // Assert
            verify(rctcRulesEngine, never()).isReportableTrigger(anyString(), anyString());
            verify(ecrRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("evaluatePatientCodes() tests")
    class EvaluatePatientCodesTests {

        @Test
        @DisplayName("Should create eCRs for all matched triggers")
        void evaluatePatientCodes_withMultipleMatches_shouldCreateMultipleEcrs() {
            // Arrange
            List<String> diagnosisCodes = List.of("U07.1", "B05.9");
            List<String> labCodes = List.of("94500-6");

            List<TriggerMatch> matches = List.of(
                TriggerMatch.builder()
                    .code("U07.1")
                    .codeSystem(RctcRulesEngine.ICD10CM_OID)
                    .display("COVID-19")
                    .conditionName("COVID-19")
                    .urgency(RctcTriggerCodeEntity.Urgency.WITHIN_24_HOURS)
                    .triggerType(RctcTriggerCodeEntity.TriggerType.DIAGNOSIS)
                    .build(),
                TriggerMatch.builder()
                    .code("B05.9")
                    .codeSystem(RctcRulesEngine.ICD10CM_OID)
                    .display("Measles")
                    .conditionName("Measles")
                    .urgency(RctcTriggerCodeEntity.Urgency.IMMEDIATE)
                    .triggerType(RctcTriggerCodeEntity.TriggerType.DIAGNOSIS)
                    .build()
            );

            when(rctcRulesEngine.evaluateClinicalEvent(any(RctcRulesEngine.ClinicalEvent.class)))
                .thenReturn(matches);
            when(ecrRepository.save(any(ElectronicCaseReportEntity.class)))
                .thenAnswer(inv -> {
                    ElectronicCaseReportEntity ecr = inv.getArgument(0);
                    ecr.setId(UUID.randomUUID());
                    return ecr;
                });

            // Act
            List<ElectronicCaseReportEntity> results = ecrTriggerService.evaluatePatientCodes(
                TENANT_ID, PATIENT_ID, diagnosisCodes, labCodes);

            // Assert
            assertThat(results).hasSize(2);
            verify(ecrRepository, times(2)).save(any(ElectronicCaseReportEntity.class));
        }

        @Test
        @DisplayName("Should return empty list when no triggers match")
        void evaluatePatientCodes_withNoMatches_shouldReturnEmptyList() {
            // Arrange
            List<String> diagnosisCodes = List.of("J06.9"); // Common cold - not reportable
            List<String> labCodes = Collections.emptyList();

            when(rctcRulesEngine.evaluateClinicalEvent(any(RctcRulesEngine.ClinicalEvent.class)))
                .thenReturn(Collections.emptyList());

            // Act
            List<ElectronicCaseReportEntity> results = ecrTriggerService.evaluatePatientCodes(
                TENANT_ID, PATIENT_ID, diagnosisCodes, labCodes);

            // Assert
            assertThat(results).isEmpty();
            verify(ecrRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Urgency mapping tests")
    class UrgencyMappingTests {

        @Test
        @DisplayName("Should map IMMEDIATE urgency correctly")
        void shouldMapImmediateUrgency() {
            testUrgencyMapping(RctcTriggerCodeEntity.Urgency.IMMEDIATE, EcrUrgency.IMMEDIATE);
        }

        @Test
        @DisplayName("Should map WITHIN_24_HOURS urgency correctly")
        void shouldMapWithin24HoursUrgency() {
            testUrgencyMapping(RctcTriggerCodeEntity.Urgency.WITHIN_24_HOURS, EcrUrgency.WITHIN_24_HOURS);
        }

        @Test
        @DisplayName("Should map WITHIN_72_HOURS urgency correctly")
        void shouldMapWithin72HoursUrgency() {
            testUrgencyMapping(RctcTriggerCodeEntity.Urgency.WITHIN_72_HOURS, EcrUrgency.WITHIN_72_HOURS);
        }

        @Test
        @DisplayName("Should map ROUTINE urgency correctly")
        void shouldMapRoutineUrgency() {
            testUrgencyMapping(RctcTriggerCodeEntity.Urgency.ROUTINE, EcrUrgency.ROUTINE);
        }

        private void testUrgencyMapping(RctcTriggerCodeEntity.Urgency inputUrgency, EcrUrgency expectedUrgency) {
            Map<String, Object> event = createConditionEvent("TEST001", "Test Condition");

            TriggerMatch match = TriggerMatch.builder()
                .code("TEST001")
                .codeSystem(RctcRulesEngine.ICD10CM_OID)
                .display("Test Condition")
                .conditionName("Test Condition")
                .urgency(inputUrgency)
                .triggerType(RctcTriggerCodeEntity.TriggerType.DIAGNOSIS)
                .build();

            when(rctcRulesEngine.isReportableTrigger(anyString(), anyString())).thenReturn(true);
            when(rctcRulesEngine.evaluateDiagnosis(anyString())).thenReturn(Optional.of(match));
            when(ecrRepository.existsPendingForTrigger(anyString(), any(UUID.class), anyString(), any(LocalDateTime.class)))
                .thenReturn(false);
            when(ecrRepository.save(any(ElectronicCaseReportEntity.class)))
                .thenAnswer(inv -> {
                    ElectronicCaseReportEntity ecr = inv.getArgument(0);
                    ecr.setId(UUID.randomUUID());
                    return ecr;
                });

            ecrTriggerService.handleConditionCreated(event);

            ArgumentCaptor<ElectronicCaseReportEntity> captor = ArgumentCaptor.forClass(ElectronicCaseReportEntity.class);
            verify(ecrRepository).save(captor.capture());
            assertThat(captor.getValue().getUrgency()).isEqualTo(expectedUrgency);
        }
    }

    // Helper methods

    private Map<String, Object> createConditionEvent(String code, String display) {
        Map<String, Object> event = new HashMap<>();
        event.put("tenantId", TENANT_ID);
        event.put("patientId", PATIENT_ID.toString());
        event.put("encounterId", ENCOUNTER_ID.toString());
        event.put("code", code);
        event.put("codeSystem", RctcRulesEngine.ICD10CM_OID);
        event.put("display", display);
        return event;
    }

    private Map<String, Object> createLabEvent(String code, String display) {
        Map<String, Object> event = new HashMap<>();
        event.put("tenantId", TENANT_ID);
        event.put("patientId", PATIENT_ID.toString());
        event.put("encounterId", ENCOUNTER_ID.toString());
        event.put("code", code);
        event.put("display", display);
        event.put("category", "laboratory");
        return event;
    }

    private TriggerMatch createTriggerMatch() {
        return TriggerMatch.builder()
            .code("U07.1")
            .codeSystem(RctcRulesEngine.ICD10CM_OID)
            .display("COVID-19")
            .conditionName("COVID-19")
            .urgency(RctcTriggerCodeEntity.Urgency.WITHIN_24_HOURS)
            .triggerType(RctcTriggerCodeEntity.TriggerType.DIAGNOSIS)
            .build();
    }
}
