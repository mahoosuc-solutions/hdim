package com.healthdata.quality.consumer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.healthdata.quality.service.ChronicDiseaseMonitoringService;
import com.healthdata.quality.service.RiskCalculationService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Risk Assessment Event Consumer Tests")
class RiskAssessmentEventConsumerTest {

    @Mock
    private RiskCalculationService riskCalculationService;

    @Mock
    private ChronicDiseaseMonitoringService chronicDiseaseMonitoringService;

    @InjectMocks
    private RiskAssessmentEventConsumer consumer;

    @Test
    @DisplayName("Should recalculate risk for chronic condition")
    void shouldRecalculateRiskForChronicCondition() {
        UUID patientId = UUID.randomUUID();
        Map<String, Object> conditionData = Map.of(
            "clinicalStatus", Map.of("coding", List.of(Map.of("code", "active"))),
            "category", List.of(Map.of("coding", List.of(Map.of("code", "encounter-diagnosis"))))
        );

        consumer.onConditionCreated(Map.of(
            "tenantId", "tenant-1",
            "patientId", patientId.toString(),
            "resource", conditionData
        ));

        verify(riskCalculationService).recalculateRiskOnCondition(
            "tenant-1", patientId, conditionData
        );
    }

    @Test
    @DisplayName("Should skip non-chronic condition")
    void shouldSkipNonChronicCondition() {
        UUID patientId = UUID.randomUUID();
        Map<String, Object> conditionData = Map.of(
            "clinicalStatus", Map.of("coding", List.of(Map.of("code", "active"))),
            "category", List.of(Map.of("coding", List.of(Map.of("code", "problem-list-item"))))
        );

        consumer.onConditionUpdated(Map.of(
            "tenantId", "tenant-1",
            "patientId", patientId.toString(),
            "resource", conditionData
        ));

        verify(riskCalculationService, never()).recalculateRiskOnCondition(any(), any(), any());
    }

    @Test
    @DisplayName("Should process monitored lab result")
    void shouldProcessMonitoredLabResult() {
        UUID patientId = UUID.randomUUID();
        Map<String, Object> observationData = Map.of(
            "code", Map.of("coding", List.of(Map.of("code", "4548-4")))
        );

        consumer.onObservationCreated(Map.of(
            "tenantId", "tenant-1",
            "patientId", patientId.toString(),
            "resource", observationData
        ));

        verify(chronicDiseaseMonitoringService).processLabResult(
            "tenant-1", patientId, observationData
        );
        verify(riskCalculationService).recalculateRiskOnObservation(
            "tenant-1", patientId, observationData
        );
    }

    @Test
    @DisplayName("Should skip observation without patient")
    void shouldSkipObservationWithoutPatient() {
        consumer.onObservationCreated(Map.of(
            "tenantId", "tenant-1",
            "resource", Map.of()
        ));

        verify(chronicDiseaseMonitoringService, never()).processLabResult(any(), any(), any());
        verify(riskCalculationService, never()).recalculateRiskOnObservation(any(), any(), any());
    }

    @Test
    @DisplayName("Should extract patientId from resource subject reference")
    void shouldExtractPatientIdFromSubjectReference() {
        UUID patientId = UUID.randomUUID();
        Map<String, Object> conditionData = Map.of(
            "clinicalStatus", Map.of("coding", List.of(Map.of("code", "active"))),
            "category", List.of(Map.of("coding", List.of(Map.of("code", "encounter-diagnosis")))),
            "subject", Map.of("reference", "Patient/" + patientId)
        );

        consumer.onConditionCreated(Map.of(
            "tenantId", "tenant-1",
            "resource", conditionData
        ));

        verify(riskCalculationService).recalculateRiskOnCondition(
            "tenant-1", patientId, conditionData
        );
    }

    @Test
    @DisplayName("Should skip inactive condition")
    void shouldSkipInactiveCondition() {
        UUID patientId = UUID.randomUUID();
        Map<String, Object> conditionData = Map.of(
            "clinicalStatus", Map.of("coding", List.of(Map.of("code", "resolved"))),
            "category", List.of(Map.of("coding", List.of(Map.of("code", "encounter-diagnosis"))))
        );

        consumer.onConditionUpdated(Map.of(
            "tenantId", "tenant-1",
            "patientId", patientId.toString(),
            "resource", conditionData
        ));

        verify(riskCalculationService, never()).recalculateRiskOnCondition(any(), any(), any());
    }

    @Test
    @DisplayName("Should skip unmonitored lab results")
    void shouldSkipUnmonitoredLabResults() {
        UUID patientId = UUID.randomUUID();
        Map<String, Object> observationData = Map.of(
            "code", Map.of("coding", List.of(Map.of("code", "0000-0")))
        );

        consumer.onObservationCreated(Map.of(
            "tenantId", "tenant-1",
            "patientId", patientId.toString(),
            "resource", observationData
        ));

        verify(chronicDiseaseMonitoringService, never()).processLabResult(any(), any(), any());
        verify(riskCalculationService, never()).recalculateRiskOnObservation(any(), any(), any());
    }

    @Test
    @DisplayName("Should skip condition when required fields missing")
    void shouldSkipConditionWhenRequiredFieldsMissing() {
        consumer.onConditionCreated(Map.of(
            "tenantId", "tenant-1",
            "patientId", UUID.randomUUID().toString()
        ));

        verify(riskCalculationService, never()).recalculateRiskOnCondition(any(), any(), any());
    }

    @Test
    @DisplayName("Should skip condition when patientId invalid")
    void shouldSkipConditionWhenPatientIdInvalid() {
        Map<String, Object> conditionData = Map.of(
            "clinicalStatus", Map.of("coding", List.of(Map.of("code", "active"))),
            "category", List.of(Map.of("coding", List.of(Map.of("code", "encounter-diagnosis"))))
        );

        consumer.onConditionUpdated(Map.of(
            "tenantId", "tenant-1",
            "patientId", "not-a-uuid",
            "resource", conditionData
        ));

        verify(riskCalculationService, never()).recalculateRiskOnCondition(any(), any(), any());
    }

    @Test
    @DisplayName("Should skip observation when tenantId missing")
    void shouldSkipObservationWhenTenantMissing() {
        consumer.onObservationCreated(Map.of(
            "patientId", UUID.randomUUID().toString(),
            "resource", Map.of()
        ));

        verify(chronicDiseaseMonitoringService, never()).processLabResult(any(), any(), any());
        verify(riskCalculationService, never()).recalculateRiskOnObservation(any(), any(), any());
    }

    @Test
    @DisplayName("Should accept UUID patientId values")
    void shouldAcceptUuidPatientIdValues() {
        UUID patientId = UUID.randomUUID();
        Map<String, Object> conditionData = Map.of(
            "clinicalStatus", Map.of("coding", List.of(Map.of("code", "active"))),
            "category", List.of(Map.of("coding", List.of(Map.of("code", "encounter-diagnosis"))))
        );

        consumer.onConditionCreated(Map.of(
            "tenantId", "tenant-1",
            "patientId", patientId,
            "resource", conditionData
        ));

        verify(riskCalculationService).recalculateRiskOnCondition(
            "tenant-1", patientId, conditionData
        );
    }

    @Test
    @DisplayName("Should skip condition when category missing")
    void shouldSkipConditionWhenCategoryMissing() {
        UUID patientId = UUID.randomUUID();
        Map<String, Object> conditionData = Map.of(
            "clinicalStatus", Map.of("coding", List.of(Map.of("code", "active")))
        );

        consumer.onConditionCreated(Map.of(
            "tenantId", "tenant-1",
            "patientId", patientId.toString(),
            "resource", conditionData
        ));

        verify(riskCalculationService, never()).recalculateRiskOnCondition(any(), any(), any());
    }

    @Test
    @DisplayName("Should skip condition when subject reference invalid")
    void shouldSkipConditionWhenSubjectReferenceInvalid() {
        Map<String, Object> conditionData = Map.of(
            "clinicalStatus", Map.of("coding", List.of(Map.of("code", "active"))),
            "category", List.of(Map.of("coding", List.of(Map.of("code", "encounter-diagnosis")))),
            "subject", Map.of("reference", "Patient/not-a-uuid")
        );

        consumer.onConditionUpdated(Map.of(
            "tenantId", "tenant-1",
            "resource", conditionData
        ));

        verify(riskCalculationService, never()).recalculateRiskOnCondition(any(), any(), any());
    }

    @Test
    @DisplayName("Should skip observation when codings missing")
    void shouldSkipObservationWhenCodingsMissing() {
        Map<String, Object> observationData = Map.of(
            "code", Map.of("coding", List.of())
        );

        consumer.onObservationCreated(Map.of(
            "tenantId", "tenant-1",
            "patientId", UUID.randomUUID().toString(),
            "resource", observationData
        ));

        verify(chronicDiseaseMonitoringService, never()).processLabResult(any(), any(), any());
        verify(riskCalculationService, never()).recalculateRiskOnObservation(any(), any(), any());
    }

    @Test
    @DisplayName("Should skip observation when code missing")
    void shouldSkipObservationWhenCodeMissing() {
        consumer.onObservationCreated(Map.of(
            "tenantId", "tenant-1",
            "patientId", UUID.randomUUID().toString(),
            "resource", Map.of()
        ));

        verify(chronicDiseaseMonitoringService, never()).processLabResult(any(), any(), any());
        verify(riskCalculationService, never()).recalculateRiskOnObservation(any(), any(), any());
    }

    @Test
    @DisplayName("Should swallow condition parsing errors")
    void shouldSwallowConditionParsingErrors() {
        UUID patientId = UUID.randomUUID();
        Map<String, Object> conditionData = Map.of(
            "clinicalStatus", "not-a-map",
            "category", List.of("bad-category")
        );

        consumer.onConditionCreated(Map.of(
            "tenantId", "tenant-1",
            "patientId", patientId.toString(),
            "resource", conditionData
        ));

        verify(riskCalculationService, never()).recalculateRiskOnCondition(any(), any(), any());
    }

    @Test
    @DisplayName("Should process condition when clinical status missing but category matches")
    void shouldProcessConditionWhenClinicalStatusMissing() {
        UUID patientId = UUID.randomUUID();
        Map<String, Object> conditionData = Map.of(
            "category", List.of(Map.of("coding", List.of(Map.of("code", "encounter-diagnosis"))))
        );

        consumer.onConditionCreated(Map.of(
            "tenantId", "tenant-1",
            "patientId", patientId.toString(),
            "resource", conditionData
        ));

        verify(riskCalculationService).recalculateRiskOnCondition(
            "tenant-1", patientId, conditionData
        );
    }

    @Test
    @DisplayName("Should swallow exceptions during condition processing")
    void shouldSwallowExceptionsDuringConditionProcessing() {
        UUID patientId = UUID.randomUUID();
        Map<String, Object> conditionData = Map.of(
            "clinicalStatus", Map.of("coding", List.of(Map.of("code", "active"))),
            "category", List.of(Map.of("coding", List.of(Map.of("code", "encounter-diagnosis"))))
        );

        doThrow(new RuntimeException("boom"))
            .when(riskCalculationService)
            .recalculateRiskOnCondition(eq("tenant-1"), eq(patientId), eq(conditionData));

        assertThatCode(() -> consumer.onConditionCreated(Map.of(
            "tenantId", "tenant-1",
            "patientId", patientId.toString(),
            "resource", conditionData
        ))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should swallow exceptions during observation processing")
    void shouldSwallowExceptionsDuringObservationProcessing() {
        UUID patientId = UUID.randomUUID();
        Map<String, Object> observationData = Map.of(
            "code", Map.of("coding", List.of(Map.of("code", "4548-4")))
        );

        doThrow(new RuntimeException("lab failure"))
            .when(chronicDiseaseMonitoringService)
            .processLabResult(eq("tenant-1"), eq(patientId), eq(observationData));

        assertThatCode(() -> consumer.onObservationCreated(Map.of(
            "tenantId", "tenant-1",
            "patientId", patientId.toString(),
            "resource", observationData
        ))).doesNotThrowAnyException();

        verify(riskCalculationService, never())
            .recalculateRiskOnObservation(any(), any(), any());
    }
}
