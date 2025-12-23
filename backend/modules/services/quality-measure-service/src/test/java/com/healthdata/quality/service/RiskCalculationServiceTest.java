package com.healthdata.quality.service;

import com.healthdata.quality.dto.RiskAssessmentDTO;
import com.healthdata.quality.persistence.RiskAssessmentEntity;
import com.healthdata.quality.persistence.RiskAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for RiskCalculationService
 *
 * Tests continuous risk assessment with:
 * - Risk recalculation on new conditions
 * - Risk recalculation on observations (lab results)
 * - Risk level change detection (LOW → HIGH)
 * - Risk factor extraction from FHIR data
 * - Predicted outcomes calculation
 * - Multi-tenant isolation
 * - Event publishing on risk change
 */
@ExtendWith(MockitoExtension.class)
class RiskCalculationServiceTest {

    @Mock
    private RiskAssessmentRepository riskAssessmentRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private RiskCalculationService riskCalculationService;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

    @BeforeEach
    void setUp() {
        // Reset mocks
        reset(riskAssessmentRepository, kafkaTemplate);
    }

    @Test
    void testRiskRecalculationOnNewCondition() {
        // Given: Patient has existing LOW risk assessment
        RiskAssessmentEntity previousAssessment = RiskAssessmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .riskScore(20)
            .riskLevel(RiskAssessmentEntity.RiskLevel.LOW)
            .chronicConditionCount(1)
            .riskFactors(List.of())
            .predictedOutcomes(List.of())
            .recommendations(List.of())
            .assessmentDate(Instant.now().minusSeconds(3600))
            .build();

        when(riskAssessmentRepository.findLatestByTenantIdAndPatientId(TENANT_ID, PATIENT_ID))
            .thenReturn(Optional.of(previousAssessment));

        // New condition event: Type 2 Diabetes diagnosed
        Map<String, Object> conditionData = Map.of(
            "resourceType", "Condition",
            "code", Map.of(
                "coding", List.of(Map.of(
                    "system", "http://snomed.info/sct",
                    "code", "44054006",
                    "display", "Type 2 Diabetes Mellitus"
                ))
            ),
            "clinicalStatus", Map.of("coding", List.of(Map.of("code", "active"))),
            "category", List.of(Map.of(
                "coding", List.of(Map.of(
                    "system", "http://terminology.hl7.org/CodeSystem/condition-category",
                    "code", "encounter-diagnosis"
                ))
            ))
        );

        RiskAssessmentEntity newAssessment = RiskAssessmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .riskScore(40)
            .riskLevel(RiskAssessmentEntity.RiskLevel.MODERATE)
            .chronicConditionCount(2)
            .riskFactors(List.of(Map.of(
                "factor", "Type 2 Diabetes Mellitus",
                "category", "chronic-disease",
                "weight", 20,
                "severity", "moderate",
                "evidence", "Active diagnosis"
            )))
            .predictedOutcomes(List.of())
            .recommendations(List.of())
            .assessmentDate(Instant.now())
            .build();

        when(riskAssessmentRepository.save(any(RiskAssessmentEntity.class)))
            .thenReturn(newAssessment);

        // When: Risk is recalculated with new condition
        RiskAssessmentDTO result = riskCalculationService.recalculateRiskOnCondition(
            TENANT_ID, PATIENT_ID, conditionData);

        // Then: Risk score should increase
        assertThat(result).isNotNull();
        assertThat(result.getRiskScore()).isGreaterThan(previousAssessment.getRiskScore());
        assertThat(result.getRiskLevel()).isEqualTo("moderate");

        // Verify assessment was saved
        ArgumentCaptor<RiskAssessmentEntity> captor = ArgumentCaptor.forClass(RiskAssessmentEntity.class);
        verify(riskAssessmentRepository).save(captor.capture());
        RiskAssessmentEntity savedEntity = captor.getValue();
        assertThat(savedEntity.getChronicConditionCount()).isEqualTo(2);

        // Verify event was published
        verify(kafkaTemplate, times(1)).send(eq("risk-assessment.updated"), any());
    }

    @Test
    void testRiskRecalculationOnObservation() {
        // Given: Patient has moderate risk
        RiskAssessmentEntity previousAssessment = RiskAssessmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .riskScore(40)
            .riskLevel(RiskAssessmentEntity.RiskLevel.MODERATE)
            .chronicConditionCount(2)
            .riskFactors(List.of())
            .predictedOutcomes(List.of())
            .recommendations(List.of())
            .assessmentDate(Instant.now().minusSeconds(3600))
            .build();

        when(riskAssessmentRepository.findLatestByTenantIdAndPatientId(TENANT_ID, PATIENT_ID))
            .thenReturn(Optional.of(previousAssessment));

        // New observation: Uncontrolled HbA1c
        Map<String, Object> observationData = Map.of(
            "resourceType", "Observation",
            "code", Map.of(
                "coding", List.of(Map.of(
                    "system", "http://loinc.org",
                    "code", "4548-4",
                    "display", "Hemoglobin A1c"
                ))
            ),
            "valueQuantity", Map.of(
                "value", 9.2,
                "unit", "%"
            )
        );

        RiskAssessmentEntity newAssessment = RiskAssessmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .riskScore(55)
            .riskLevel(RiskAssessmentEntity.RiskLevel.HIGH)
            .chronicConditionCount(2)
            .riskFactors(List.of(Map.of(
                "factor", "Uncontrolled Diabetes",
                "category", "lab-result",
                "weight", 15,
                "severity", "high",
                "evidence", "HbA1c 9.2% (target <7.0%)"
            )))
            .predictedOutcomes(List.of())
            .recommendations(List.of())
            .assessmentDate(Instant.now())
            .build();

        when(riskAssessmentRepository.save(any(RiskAssessmentEntity.class)))
            .thenReturn(newAssessment);

        // When: Risk is recalculated with new lab result
        RiskAssessmentDTO result = riskCalculationService.recalculateRiskOnObservation(
            TENANT_ID, PATIENT_ID, observationData);

        // Then: Risk should increase to HIGH
        assertThat(result).isNotNull();
        assertThat(result.getRiskScore()).isEqualTo(55);
        assertThat(result.getRiskLevel()).isEqualTo("high");

        // Verify risk factor was extracted from observation
        assertThat(result.getRiskFactors()).isNotEmpty();
        assertThat(result.getRiskFactors().get(0).getCategory()).isEqualTo("lab-result");
        assertThat(result.getRiskFactors().get(0).getEvidence()).contains("HbA1c 9.2%");
    }

    @Test
    void testRiskLevelChangeDetection() {
        // Given: Patient risk changes from LOW to MODERATE
        RiskAssessmentEntity previousAssessment = RiskAssessmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .riskScore(10)
            .riskLevel(RiskAssessmentEntity.RiskLevel.LOW)
            .chronicConditionCount(0)
            .riskFactors(List.of())
            .predictedOutcomes(List.of())
            .recommendations(List.of())
            .assessmentDate(Instant.now().minusSeconds(3600))
            .build();

        when(riskAssessmentRepository.findLatestByTenantIdAndPatientId(TENANT_ID, PATIENT_ID))
            .thenReturn(Optional.of(previousAssessment));

        // Add a severe condition to push into MODERATE range (15 base + 10 severity = 25)
        Map<String, Object> conditionData = Map.of(
            "resourceType", "Condition",
            "code", Map.of("coding", List.of(Map.of("code", "44054006", "display", "Type 2 Diabetes Mellitus"))),
            "severity", Map.of("coding", List.of(Map.of("code", "severe", "display", "Severe")))
        );

        when(riskAssessmentRepository.save(any(RiskAssessmentEntity.class)))
            .thenAnswer(invocation -> {
                RiskAssessmentEntity entity = invocation.getArgument(0);
                // Set an ID if not already set (simulating database save)
                if (entity.getId() == null) {
                    entity.setId(UUID.randomUUID());
                }
                return entity;
            });

        // When: Risk is recalculated
        RiskAssessmentDTO result = riskCalculationService.recalculateRiskOnCondition(
            TENANT_ID, PATIENT_ID, conditionData);

        // Then: Risk level should change from LOW to MODERATE
        assertThat(result.getRiskLevel()).isEqualTo("moderate");

        // Verify both events were published
        verify(kafkaTemplate).send(eq("risk-assessment.updated"), any());
        verify(kafkaTemplate).send(eq("risk-level.changed"), argThat(event -> {
            if (event instanceof Map) {
                Map<String, Object> eventMap = (Map<String, Object>) event;
                return eventMap.get("previousLevel").equals("LOW") &&
                       eventMap.get("newLevel").equals("MODERATE") &&
                       eventMap.get("patientId").equals(PATIENT_ID);
            }
            return false;
        }));
    }

    @Test
    void testRiskFactorExtractionFromFHIR() {
        // Given: Complex FHIR condition data
        Map<String, Object> conditionData = Map.of(
            "resourceType", "Condition",
            "code", Map.of(
                "coding", List.of(Map.of(
                    "system", "http://snomed.info/sct",
                    "code", "38341003",
                    "display", "Hypertensive disorder"
                )),
                "text", "Essential Hypertension"
            ),
            "clinicalStatus", Map.of(
                "coding", List.of(Map.of("code", "active"))
            ),
            "severity", Map.of(
                "coding", List.of(Map.of(
                    "code", "severe",
                    "display", "Severe"
                ))
            ),
            "onsetDateTime", "2020-01-15",
            "category", List.of(Map.of(
                "coding", List.of(Map.of("code", "encounter-diagnosis"))
            ))
        );

        when(riskAssessmentRepository.findLatestByTenantIdAndPatientId(TENANT_ID, PATIENT_ID))
            .thenReturn(Optional.empty());

        RiskAssessmentEntity savedAssessment = RiskAssessmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .riskScore(30)
            .riskLevel(RiskAssessmentEntity.RiskLevel.MODERATE)
            .chronicConditionCount(1)
            .riskFactors(List.of(Map.of(
                "factor", "Essential Hypertension",
                "category", "chronic-disease",
                "weight", 15,
                "severity", "severe",
                "evidence", "Active diagnosis since 2020-01-15"
            )))
            .predictedOutcomes(List.of())
            .recommendations(List.of())
            .assessmentDate(Instant.now())
            .build();

        when(riskAssessmentRepository.save(any(RiskAssessmentEntity.class)))
            .thenReturn(savedAssessment);

        // When: Risk factor is extracted
        RiskAssessmentDTO result = riskCalculationService.recalculateRiskOnCondition(
            TENANT_ID, PATIENT_ID, conditionData);

        // Then: Risk factor should be properly extracted
        assertThat(result.getRiskFactors()).hasSize(1);
        assertThat(result.getRiskFactors().get(0).getFactor()).isEqualTo("Essential Hypertension");
        assertThat(result.getRiskFactors().get(0).getSeverity()).isEqualTo("severe");
        assertThat(result.getRiskFactors().get(0).getCategory()).isEqualTo("chronic-disease");
    }

    @Test
    void testPredictedOutcomesCalculation() {
        // Given: High risk patient
        when(riskAssessmentRepository.findLatestByTenantIdAndPatientId(TENANT_ID, PATIENT_ID))
            .thenReturn(Optional.empty());

        Map<String, Object> conditionData = Map.of(
            "resourceType", "Condition",
            "code", Map.of("coding", List.of(Map.of("code", "44054006", "display", "Type 2 Diabetes")))
        );

        RiskAssessmentEntity savedAssessment = RiskAssessmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .riskScore(65)
            .riskLevel(RiskAssessmentEntity.RiskLevel.HIGH)
            .chronicConditionCount(2)
            .riskFactors(List.of())
            .predictedOutcomes(List.of(
                Map.of(
                    "outcome", "Hospital admission",
                    "probability", 0.25,
                    "timeframe", "next 90 days"
                ),
                Map.of(
                    "outcome", "ED visit",
                    "probability", 0.40,
                    "timeframe", "next 90 days"
                ),
                Map.of(
                    "outcome", "Disease progression",
                    "probability", 0.50,
                    "timeframe", "next 6 months"
                )
            ))
            .recommendations(List.of())
            .assessmentDate(Instant.now())
            .build();

        when(riskAssessmentRepository.save(any(RiskAssessmentEntity.class)))
            .thenReturn(savedAssessment);

        // When: Predicted outcomes are calculated
        RiskAssessmentDTO result = riskCalculationService.recalculateRiskOnCondition(
            TENANT_ID, PATIENT_ID, conditionData);

        // Then: Outcomes should be calculated based on risk level
        assertThat(result.getPredictedOutcomes()).hasSize(3);
        assertThat(result.getPredictedOutcomes())
            .extracting(RiskAssessmentDTO.PredictedOutcomeDTO::getOutcome)
            .contains("Hospital admission", "ED visit", "Disease progression");

        // High risk should have specific probabilities
        assertThat(result.getPredictedOutcomes().get(0).getProbability()).isEqualTo(0.25);
        assertThat(result.getPredictedOutcomes().get(1).getProbability()).isEqualTo(0.40);
    }

    @Test
    void testMultiTenantIsolation() {
        // Given: Two different tenants
        String tenant1 = "tenant-1";
        String tenant2 = "tenant-2";

        RiskAssessmentEntity tenant1Assessment = RiskAssessmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenant1)
            .patientId(PATIENT_ID)
            .riskScore(30)
            .riskLevel(RiskAssessmentEntity.RiskLevel.MODERATE)
            .chronicConditionCount(1)
            .riskFactors(List.of())
            .predictedOutcomes(List.of())
            .recommendations(List.of())
            .assessmentDate(Instant.now())
            .build();

        when(riskAssessmentRepository.findLatestByTenantIdAndPatientId(eq(tenant1), eq(PATIENT_ID)))
            .thenReturn(Optional.of(tenant1Assessment));
        when(riskAssessmentRepository.findLatestByTenantIdAndPatientId(eq(tenant2), eq(PATIENT_ID)))
            .thenReturn(Optional.empty());

        Map<String, Object> conditionData = Map.of(
            "resourceType", "Condition",
            "code", Map.of("coding", List.of(Map.of("code", "44054006", "display", "Type 2 Diabetes Mellitus")))
        );

        when(riskAssessmentRepository.save(any(RiskAssessmentEntity.class)))
            .thenAnswer(invocation -> {
                RiskAssessmentEntity entity = invocation.getArgument(0);
                // Set an ID if not already set (simulating database save)
                if (entity.getId() == null) {
                    entity.setId(UUID.randomUUID());
                }
                return entity;
            });

        // When: Risk calculations for different tenants
        RiskAssessmentDTO result1 = riskCalculationService.recalculateRiskOnCondition(
            tenant1, PATIENT_ID, conditionData);
        RiskAssessmentDTO result2 = riskCalculationService.recalculateRiskOnCondition(
            tenant2, PATIENT_ID, conditionData);

        // Then: Assessments should be isolated by tenant
        verify(riskAssessmentRepository).findLatestByTenantIdAndPatientId(tenant1, PATIENT_ID);
        verify(riskAssessmentRepository).findLatestByTenantIdAndPatientId(tenant2, PATIENT_ID);

        ArgumentCaptor<RiskAssessmentEntity> captor = ArgumentCaptor.forClass(RiskAssessmentEntity.class);
        verify(riskAssessmentRepository, times(2)).save(captor.capture());

        List<RiskAssessmentEntity> saved = captor.getAllValues();
        assertThat(saved).extracting(RiskAssessmentEntity::getTenantId)
            .containsExactly(tenant1, tenant2);
    }

    @Test
    void testEventPublishingOnRiskChange() {
        // Given: Patient with existing LOW risk assessment
        RiskAssessmentEntity previousAssessment = RiskAssessmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .riskScore(10)
            .riskLevel(RiskAssessmentEntity.RiskLevel.LOW)
            .chronicConditionCount(0)
            .riskFactors(List.of())
            .predictedOutcomes(List.of())
            .recommendations(List.of())
            .assessmentDate(Instant.now().minusSeconds(3600))
            .build();

        when(riskAssessmentRepository.findLatestByTenantIdAndPatientId(TENANT_ID, PATIENT_ID))
            .thenReturn(Optional.of(previousAssessment));

        Map<String, Object> conditionData = Map.of(
            "resourceType", "Condition",
            "code", Map.of("coding", List.of(Map.of("code", "195967001", "display", "Asthma")))
        );

        when(riskAssessmentRepository.save(any(RiskAssessmentEntity.class)))
            .thenAnswer(invocation -> {
                RiskAssessmentEntity entity = invocation.getArgument(0);
                // Set an ID if not already set (simulating database save)
                if (entity.getId() == null) {
                    entity.setId(UUID.randomUUID());
                }
                return entity;
            });

        // When: Risk is recalculated - new condition adds 15 points (total ~15, still LOW)
        RiskAssessmentDTO result = riskCalculationService.recalculateRiskOnCondition(
            TENANT_ID, PATIENT_ID, conditionData);

        // Then: Event should be published
        verify(kafkaTemplate, atLeastOnce()).send(eq("risk-assessment.updated"), argThat(event -> {
            if (event instanceof Map) {
                Map<String, Object> eventMap = (Map<String, Object>) event;
                return eventMap.get("patientId").equals(PATIENT_ID) &&
                       eventMap.get("tenantId").equals(TENANT_ID);
            }
            return false;
        }));

        // Risk level didn't change (LOW → LOW), so no risk-level.changed event
        verify(kafkaTemplate, never()).send(eq("risk-level.changed"), any());

        // Verify the result is LOW risk
        assertThat(result.getRiskLevel()).isEqualTo("low");
    }

    @Test
    void testObservationDefaultsAndScoreCap() {
        RiskAssessmentEntity previousAssessment = RiskAssessmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .riskScore(98)
            .riskLevel(RiskAssessmentEntity.RiskLevel.HIGH)
            .chronicConditionCount(2)
            .riskFactors(List.of())
            .predictedOutcomes(List.of())
            .recommendations(List.of())
            .assessmentDate(Instant.now().minusSeconds(3600))
            .build();

        when(riskAssessmentRepository.findLatestByTenantIdAndPatientId(TENANT_ID, PATIENT_ID))
            .thenReturn(Optional.of(previousAssessment));

        Map<String, Object> observationData = Map.of(
            "resourceType", "Observation",
            "code", Map.of(
                "coding", List.of(Map.of(
                    "system", "http://loinc.org",
                    "code", "9999-9",
                    "display", "Custom Lab"
                ))
            ),
            "valueQuantity", Map.of(
                "value", 123.4,
                "unit", "mg/dL"
            )
        );

        when(riskAssessmentRepository.save(any(RiskAssessmentEntity.class)))
            .thenAnswer(invocation -> {
                RiskAssessmentEntity entity = invocation.getArgument(0);
                if (entity.getId() == null) {
                    entity.setId(UUID.randomUUID());
                }
                return entity;
            });

        RiskAssessmentDTO result = riskCalculationService.recalculateRiskOnObservation(
            TENANT_ID, PATIENT_ID, observationData);

        assertThat(result.getRiskScore()).isEqualTo(100);
        assertThat(result.getRiskLevel()).isEqualTo("very-high");
        assertThat(result.getRiskFactors().get(0).getEvidence()).contains("Custom Lab");
    }

    @Test
    void testConditionWeightForMildSeverity() {
        Map<String, Object> conditionData = Map.of(
            "resourceType", "Condition",
            "code", Map.of(
                "coding", List.of(Map.of(
                    "system", "http://snomed.info/sct",
                    "code", "123",
                    "display", "Unknown Condition"
                ))
            ),
            "severity", Map.of(
                "coding", List.of(Map.of(
                    "code", "mild",
                    "display", "Mild"
                ))
            )
        );

        when(riskAssessmentRepository.findLatestByTenantIdAndPatientId(TENANT_ID, PATIENT_ID))
            .thenReturn(Optional.empty());

        when(riskAssessmentRepository.save(any(RiskAssessmentEntity.class)))
            .thenAnswer(invocation -> {
                RiskAssessmentEntity entity = invocation.getArgument(0);
                if (entity.getId() == null) {
                    entity.setId(UUID.randomUUID());
                }
                return entity;
            });

        RiskAssessmentDTO result = riskCalculationService.recalculateRiskOnCondition(
            TENANT_ID, PATIENT_ID, conditionData);

        assertThat(result.getRiskFactors()).hasSize(1);
        assertThat(result.getRiskFactors().get(0).getFactor()).isEqualTo("Unknown Condition");
        assertThat(result.getRiskFactors().get(0).getWeight()).isEqualTo(10);
    }
}
