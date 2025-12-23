package com.healthdata.quality.service;

import com.healthdata.quality.dto.HealthScoreDTO;
import com.healthdata.quality.persistence.CareGapEntity;
import com.healthdata.quality.persistence.CareGapRepository;
import com.healthdata.quality.persistence.HealthScoreEntity;
import com.healthdata.quality.persistence.HealthScoreHistoryEntity;
import com.healthdata.quality.persistence.HealthScoreHistoryRepository;
import com.healthdata.quality.persistence.HealthScoreRepository;
import com.healthdata.quality.persistence.MentalHealthAssessmentEntity;
import com.healthdata.quality.persistence.MentalHealthAssessmentRepository;
import com.healthdata.quality.persistence.RiskAssessmentRepository;
import com.healthdata.quality.service.notification.HealthScoreNotificationTrigger;
import com.healthdata.quality.websocket.HealthScoreWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthScoreServiceTest {

    @Mock
    private HealthScoreRepository healthScoreRepository;

    @Mock
    private HealthScoreHistoryRepository healthScoreHistoryRepository;

    @Mock
    private MentalHealthAssessmentRepository mentalHealthRepository;

    @Mock
    private CareGapRepository careGapRepository;

    @Mock
    private RiskAssessmentRepository riskAssessmentRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private HealthScoreWebSocketHandler webSocketHandler;

    @Mock
    private HealthScoreNotificationTrigger notificationTrigger;

    private HealthScoreService service;

    @BeforeEach
    void setUp() {
        service = new HealthScoreService(
            healthScoreRepository,
            healthScoreHistoryRepository,
            mentalHealthRepository,
            careGapRepository,
            riskAssessmentRepository,
            kafkaTemplate,
            webSocketHandler,
            notificationTrigger
        );
    }

    @Test
    void shouldCalculateHealthScoreAndPublishEvents() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        HealthScoreEntity previous = HealthScoreEntity.builder()
            .overallScore(70.0)
            .build();
        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(previous));

        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        HealthScoreComponents components = HealthScoreComponents.builder()
            .physicalHealthScore(90.0)
            .mentalHealthScore(90.0)
            .socialDeterminantsScore(90.0)
            .preventiveCareScore(90.0)
            .chronicDiseaseScore(90.0)
            .build();

        HealthScoreDTO dto = service.calculateHealthScore(tenantId, patientId, components);

        assertThat(dto.getOverallScore()).isNotNull();
        verify(healthScoreHistoryRepository).save(any(HealthScoreHistoryEntity.class));
        verify(kafkaTemplate).send(eq("health-score.updated"), eq(patientId.toString()), any());
        verify(kafkaTemplate).send(eq("health-score.significant-change"), eq(patientId.toString()), any());
    }

    @Test
    void shouldNotPublishSignificantChangeWhenDeltaSmall() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        HealthScoreEntity previous = HealthScoreEntity.builder()
            .overallScore(80.0)
            .build();
        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(previous));
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        HealthScoreComponents components = HealthScoreComponents.builder()
            .physicalHealthScore(81.0)
            .mentalHealthScore(81.0)
            .socialDeterminantsScore(81.0)
            .preventiveCareScore(81.0)
            .chronicDiseaseScore(81.0)
            .build();

        service.calculateHealthScore(tenantId, patientId, components);

        verify(kafkaTemplate).send(eq("health-score.updated"), eq(patientId.toString()), any());
        verify(kafkaTemplate, never()).send(eq("health-score.significant-change"), eq(patientId.toString()), any());
    }

    @Test
    void shouldCalculateHealthScoreWithoutPreviousScore() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.empty());
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        HealthScoreComponents components = HealthScoreComponents.builder()
            .physicalHealthScore(75.0)
            .mentalHealthScore(75.0)
            .socialDeterminantsScore(75.0)
            .preventiveCareScore(75.0)
            .chronicDiseaseScore(75.0)
            .build();

        HealthScoreDTO dto = service.calculateHealthScore(tenantId, patientId, components);

        assertThat(dto.getPreviousScore()).isNull();
        verify(kafkaTemplate).send(eq("health-score.updated"), eq(patientId.toString()), any());
        verify(kafkaTemplate, never()).send(eq("health-score.significant-change"), eq(patientId.toString()), any());
    }

    @Test
    void shouldRejectInvalidHealthScoreComponents() {
        HealthScoreComponents components = HealthScoreComponents.builder()
            .physicalHealthScore(120.0)
            .mentalHealthScore(80.0)
            .socialDeterminantsScore(80.0)
            .preventiveCareScore(80.0)
            .chronicDiseaseScore(80.0)
            .build();

        assertThatThrownBy(() -> service.calculateHealthScore("tenant-1", UUID.randomUUID(), components))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Physical Health score must be between 0 and 100");
    }

    @Test
    void shouldNotFailWhenNotificationTriggerThrows() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.empty());
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });
        doThrow(new RuntimeException("notify")).when(notificationTrigger)
            .onHealthScoreCalculated(eq(tenantId), any(HealthScoreDTO.class), any());

        HealthScoreComponents components = HealthScoreComponents.builder()
            .physicalHealthScore(80.0)
            .mentalHealthScore(80.0)
            .socialDeterminantsScore(80.0)
            .preventiveCareScore(80.0)
            .chronicDiseaseScore(80.0)
            .build();

        assertThatNoException()
            .isThrownBy(() -> service.calculateHealthScore(tenantId, patientId, components));
    }

    @Test
    void shouldSkipObservationWhenMissingRequiredFields() {
        service.handleObservationEvent(Map.of("tenantId", "tenant-1"));

        verifyNoInteractions(healthScoreRepository);
    }

    @Test
    void shouldSkipObservationWhenMissingResource() {
        Map<String, Object> event = Map.of(
            "tenantId", "tenant-1",
            "patientId", UUID.randomUUID().toString()
        );

        service.handleObservationEvent(event);

        verifyNoInteractions(healthScoreRepository);
    }

    @Test
    void shouldProcessObservationEventWithVitalSign() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        HealthScoreEntity current = HealthScoreEntity.builder()
            .physicalHealthScore(60.0)
            .mentalHealthScore(70.0)
            .socialDeterminantsScore(80.0)
            .preventiveCareScore(90.0)
            .chronicDiseaseScore(75.0)
            .overallScore(70.0)
            .build();

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(current));
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        Map<String, Object> event = Map.of(
            "tenantId", tenantId,
            "patientId", patientId.toString(),
            "resource", Map.of(
                "code", Map.of(
                    "coding", List.of(Map.of("code", "8480-6", "display", "Systolic BP"))
                ),
                "valueQuantity", Map.of("value", 150, "unit", "mmHg")
            )
        );

        service.handleObservationEvent(event);

        verify(healthScoreRepository).save(any(HealthScoreEntity.class));
    }

    @Test
    void shouldSkipObservationWhenMissingCoding() {
        Map<String, Object> event = Map.of(
            "tenantId", "tenant-1",
            "patientId", UUID.randomUUID().toString(),
            "resource", Map.of(
                "code", Map.of("coding", List.of())
            )
        );

        service.handleObservationEvent(event);

        verify(healthScoreRepository, never()).save(any());
    }

    @Test
    void shouldSkipObservationWhenMissingValue() {
        Map<String, Object> event = Map.of(
            "tenantId", "tenant-1",
            "patientId", UUID.randomUUID().toString(),
            "resource", Map.of(
                "code", Map.of(
                    "coding", List.of(Map.of("code", "8867-4", "display", "Heart rate"))
                )
            )
        );

        service.handleObservationEvent(event);

        verify(healthScoreRepository, never()).save(any());
    }

    @Test
    void shouldSkipObservationWhenPatientIdInvalid() {
        Map<String, Object> event = Map.of(
            "tenantId", "tenant-1",
            "patientId", "not-a-uuid",
            "resource", Map.of(
                "code", Map.of(
                    "coding", List.of(Map.of("code", "8867-4", "display", "Heart rate"))
                ),
                "valueQuantity", Map.of("value", 80, "unit", "beats/min")
            )
        );

        service.handleObservationEvent(event);

        verify(healthScoreRepository, never()).save(any());
    }

    @Test
    void shouldSkipObservationWhenValueNotNumeric() {
        Map<String, Object> event = Map.of(
            "tenantId", "tenant-1",
            "patientId", UUID.randomUUID().toString(),
            "resource", Map.of(
                "code", Map.of(
                    "coding", List.of(Map.of("code", "39156-5", "display", "BMI"))
                ),
                "valueQuantity", Map.of("value", "invalid")
            )
        );

        service.handleObservationEvent(event);

        verify(healthScoreRepository, never()).save(any());
    }

    @Test
    void shouldProcessObservationEventWithoutExistingScore() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.empty());
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        Map<String, Object> event = Map.of(
            "tenantId", tenantId,
            "patientId", patientId.toString(),
            "resource", Map.of(
                "code", Map.of(
                    "coding", List.of(Map.of("code", "8867-4", "display", "Heart rate"))
                ),
                "valueQuantity", Map.of("value", 72, "unit", "beats/min")
            )
        );

        service.handleObservationEvent(event);

        verify(healthScoreRepository).save(any(HealthScoreEntity.class));
    }

    @Test
    void shouldProcessObservationEventForA1C() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        HealthScoreEntity current = HealthScoreEntity.builder()
            .physicalHealthScore(70.0)
            .mentalHealthScore(70.0)
            .socialDeterminantsScore(70.0)
            .preventiveCareScore(70.0)
            .chronicDiseaseScore(70.0)
            .overallScore(70.0)
            .build();

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(current));
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        Map<String, Object> event = Map.of(
            "tenantId", tenantId,
            "patientId", patientId.toString(),
            "resource", Map.of(
                "code", Map.of(
                    "coding", List.of(Map.of("code", "4548-4", "display", "HbA1c"))
                ),
                "valueQuantity", Map.of("value", 9.2, "unit", "%")
            )
        );

        service.handleObservationEvent(event);

        verify(healthScoreRepository).save(any(HealthScoreEntity.class));
    }

    @Test
    void shouldProcessObservationEventForOxygenSaturation() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        HealthScoreEntity current = HealthScoreEntity.builder()
            .physicalHealthScore(70.0)
            .mentalHealthScore(70.0)
            .socialDeterminantsScore(70.0)
            .preventiveCareScore(70.0)
            .chronicDiseaseScore(70.0)
            .overallScore(70.0)
            .build();

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(current));
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        Map<String, Object> event = Map.of(
            "tenantId", tenantId,
            "patientId", patientId.toString(),
            "resource", Map.of(
                "code", Map.of(
                    "coding", List.of(Map.of("code", "2708-6", "display", "Oxygen saturation"))
                ),
                "valueQuantity", Map.of("value", 88, "unit", "%")
            )
        );

        service.handleObservationEvent(event);

        verify(healthScoreRepository).save(any(HealthScoreEntity.class));
    }

    @Test
    void shouldProcessObservationEventForBmi() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        HealthScoreEntity current = HealthScoreEntity.builder()
            .physicalHealthScore(70.0)
            .mentalHealthScore(70.0)
            .socialDeterminantsScore(70.0)
            .preventiveCareScore(70.0)
            .chronicDiseaseScore(70.0)
            .overallScore(70.0)
            .build();

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(current));
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        Map<String, Object> event = Map.of(
            "tenantId", tenantId,
            "patientId", patientId.toString(),
            "resource", Map.of(
                "code", Map.of(
                    "coding", List.of(Map.of("code", "39156-5", "display", "BMI"))
                ),
                "valueQuantity", Map.of("value", 32.0, "unit", "kg/m2")
            )
        );

        service.handleObservationEvent(event);

        verify(healthScoreRepository).save(any(HealthScoreEntity.class));
    }

    @Test
    void shouldProcessObservationEventForDiastolicBp() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        HealthScoreEntity current = HealthScoreEntity.builder()
            .physicalHealthScore(70.0)
            .mentalHealthScore(70.0)
            .socialDeterminantsScore(70.0)
            .preventiveCareScore(70.0)
            .chronicDiseaseScore(70.0)
            .overallScore(70.0)
            .build();

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(current));
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        Map<String, Object> event = Map.of(
            "tenantId", tenantId,
            "patientId", patientId.toString(),
            "resource", Map.of(
                "code", Map.of(
                    "coding", List.of(Map.of("code", "8462-4", "display", "Diastolic BP"))
                ),
                "valueQuantity", Map.of("value", 92, "unit", "mmHg")
            )
        );

        service.handleObservationEvent(event);

        verify(healthScoreRepository).save(any(HealthScoreEntity.class));
    }

    @Test
    void shouldProcessObservationEventForGlucoseFasting() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        HealthScoreEntity current = HealthScoreEntity.builder()
            .physicalHealthScore(70.0)
            .mentalHealthScore(70.0)
            .socialDeterminantsScore(70.0)
            .preventiveCareScore(70.0)
            .chronicDiseaseScore(70.0)
            .overallScore(70.0)
            .build();

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(current));
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        Map<String, Object> event = Map.of(
            "tenantId", tenantId,
            "patientId", patientId.toString(),
            "resource", Map.of(
                "code", Map.of(
                    "coding", List.of(Map.of("code", "2345-7", "display", "Glucose fasting"))
                ),
                "valueQuantity", Map.of("value", 110, "unit", "mg/dL")
            )
        );

        service.handleObservationEvent(event);

        verify(healthScoreRepository).save(any(HealthScoreEntity.class));
    }

    @Test
    void shouldProcessObservationEventForBodyTemperature() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        HealthScoreEntity current = HealthScoreEntity.builder()
            .physicalHealthScore(70.0)
            .mentalHealthScore(70.0)
            .socialDeterminantsScore(70.0)
            .preventiveCareScore(70.0)
            .chronicDiseaseScore(70.0)
            .overallScore(70.0)
            .build();

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(current));
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        Map<String, Object> event = Map.of(
            "tenantId", tenantId,
            "patientId", patientId.toString(),
            "resource", Map.of(
                "code", Map.of(
                    "coding", List.of(Map.of("code", "8310-5", "display", "Body temperature"))
                ),
                "valueQuantity", Map.of("value", 37.0, "unit", "C")
            )
        );

        service.handleObservationEvent(event);

        verify(healthScoreRepository).save(any(HealthScoreEntity.class));
    }

    @Test
    void shouldProcessObservationEventForRespiratoryRate() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        HealthScoreEntity current = HealthScoreEntity.builder()
            .physicalHealthScore(70.0)
            .mentalHealthScore(70.0)
            .socialDeterminantsScore(70.0)
            .preventiveCareScore(70.0)
            .chronicDiseaseScore(70.0)
            .overallScore(70.0)
            .build();

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(current));
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        Map<String, Object> event = Map.of(
            "tenantId", tenantId,
            "patientId", patientId.toString(),
            "resource", Map.of(
                "code", Map.of(
                    "coding", List.of(Map.of("code", "9279-1", "display", "Respiratory rate"))
                ),
                "valueQuantity", Map.of("value", 18, "unit", "breaths/min")
            )
        );

        service.handleObservationEvent(event);

        verify(healthScoreRepository).save(any(HealthScoreEntity.class));
    }

    @Test
    void shouldProcessObservationEventForWeight() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        HealthScoreEntity current = HealthScoreEntity.builder()
            .physicalHealthScore(70.0)
            .mentalHealthScore(70.0)
            .socialDeterminantsScore(70.0)
            .preventiveCareScore(70.0)
            .chronicDiseaseScore(70.0)
            .overallScore(70.0)
            .build();

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(current));
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        Map<String, Object> event = Map.of(
            "tenantId", tenantId,
            "patientId", patientId.toString(),
            "resource", Map.of(
                "code", Map.of(
                    "coding", List.of(Map.of("code", "29463-7", "display", "Body weight"))
                ),
                "valueQuantity", Map.of("value", 95, "unit", "kg")
            )
        );

        service.handleObservationEvent(event);

        verify(healthScoreRepository).save(any(HealthScoreEntity.class));
    }

    @Test
    void shouldLeavePhysicalScoreUnchangedForWeight() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        HealthScoreEntity current = HealthScoreEntity.builder()
            .physicalHealthScore(64.0)
            .mentalHealthScore(70.0)
            .socialDeterminantsScore(70.0)
            .preventiveCareScore(70.0)
            .chronicDiseaseScore(70.0)
            .overallScore(70.0)
            .build();

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(current));
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        Map<String, Object> event = Map.of(
            "tenantId", tenantId,
            "patientId", patientId,
            "resource", Map.of(
                "code", Map.of(
                    "coding", List.of(Map.of("code", "29463-7", "display", "Body weight"))
                ),
                "valueQuantity", Map.of("value", 95, "unit", "kg")
            )
        );

        service.handleObservationEvent(event);

        ArgumentCaptor<HealthScoreEntity> captor = ArgumentCaptor.forClass(HealthScoreEntity.class);
        verify(healthScoreRepository).save(captor.capture());
        assertThat(captor.getValue().getPhysicalHealthScore()).isEqualTo(64.0);
    }

    @Test
    void shouldApplySevereGlucoseAdjustment() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        HealthScoreEntity current = HealthScoreEntity.builder()
            .physicalHealthScore(70.0)
            .mentalHealthScore(70.0)
            .socialDeterminantsScore(70.0)
            .preventiveCareScore(70.0)
            .chronicDiseaseScore(70.0)
            .overallScore(70.0)
            .build();

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(current));
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        Map<String, Object> event = Map.of(
            "tenantId", tenantId,
            "patientId", patientId.toString(),
            "resource", Map.of(
                "code", Map.of(
                    "coding", List.of(Map.of("code", "2339-0", "display", "Glucose"))
                ),
                "valueQuantity", Map.of("value", 250, "unit", "mg/dL")
            )
        );

        service.handleObservationEvent(event);

        ArgumentCaptor<HealthScoreEntity> captor = ArgumentCaptor.forClass(HealthScoreEntity.class);
        verify(healthScoreRepository).save(captor.capture());
        assertThat(captor.getValue().getPhysicalHealthScore()).isEqualTo(55.0);
    }

    @Test
    void shouldSkipObservationWhenNotTracked() {
        UUID patientId = UUID.randomUUID();
        Map<String, Object> event = Map.of(
            "tenantId", "tenant-1",
            "patientId", patientId.toString(),
            "resource", Map.of(
                "code", Map.of(
                    "coding", List.of(Map.of("code", "0000-0", "display", "Unknown"))
                ),
                "valueQuantity", Map.of("value", 50, "unit", "unit")
            )
        );

        service.handleObservationEvent(event);

        verify(healthScoreRepository, never()).save(any());
    }

    @Test
    void shouldProcessObservationWhenPatientInSubjectReference() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.empty());
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        Map<String, Object> event = Map.of(
            "tenantId", tenantId,
            "resource", Map.of(
                "subject", Map.of("reference", "Patient/" + patientId),
                "code", Map.of(
                    "coding", List.of(Map.of("code", "2339-0", "display", "Glucose"))
                ),
                "valueQuantity", Map.of("value", 95, "unit", "mg/dL")
            )
        );

        service.handleObservationEvent(event);

        verify(healthScoreRepository).save(any(HealthScoreEntity.class));
    }

    @Test
    void shouldProcessMentalHealthAssessment() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        MentalHealthAssessmentEntity assessment = MentalHealthAssessmentEntity.builder()
            .patientId(patientId)
            .score(18)
            .maxScore(27)
            .severity("severe")
            .build();

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.empty());
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        service.handleMentalHealthAssessment(tenantId, assessment);

        verify(healthScoreRepository).save(any(HealthScoreEntity.class));
    }

    @Test
    void shouldProcessMentalHealthAssessmentWithMildSeverity() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        MentalHealthAssessmentEntity assessment = MentalHealthAssessmentEntity.builder()
            .patientId(patientId)
            .score(6)
            .maxScore(27)
            .severity("mild")
            .build();

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.empty());
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        service.handleMentalHealthAssessment(tenantId, assessment);

        verify(healthScoreRepository).save(any(HealthScoreEntity.class));
    }

    @Test
    void shouldProcessMentalHealthAssessmentWithModeratelySevereSeverity() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        MentalHealthAssessmentEntity assessment = MentalHealthAssessmentEntity.builder()
            .patientId(patientId)
            .score(17)
            .maxScore(27)
            .severity("moderately-severe")
            .build();

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.empty());
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        service.handleMentalHealthAssessment(tenantId, assessment);

        verify(healthScoreRepository).save(any(HealthScoreEntity.class));
    }

    @Test
    void shouldProcessMentalHealthAssessmentWithMinimalSeverity() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        MentalHealthAssessmentEntity assessment = MentalHealthAssessmentEntity.builder()
            .patientId(patientId)
            .score(2)
            .maxScore(27)
            .severity("minimal")
            .build();

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.empty());
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        service.handleMentalHealthAssessment(tenantId, assessment);

        verify(healthScoreRepository).save(any(HealthScoreEntity.class));
    }

    @Test
    void shouldProcessMentalHealthAssessmentWithModerateSeverity() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        MentalHealthAssessmentEntity assessment = MentalHealthAssessmentEntity.builder()
            .patientId(patientId)
            .score(12)
            .maxScore(27)
            .severity("moderate")
            .build();

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.empty());
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        service.handleMentalHealthAssessment(tenantId, assessment);

        verify(healthScoreRepository).save(any(HealthScoreEntity.class));
    }

    @Test
    void shouldProcessMentalHealthAssessmentWithUnknownSeverity() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        MentalHealthAssessmentEntity assessment = MentalHealthAssessmentEntity.builder()
            .patientId(patientId)
            .score(5)
            .maxScore(27)
            .severity("unknown")
            .build();

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.empty());
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        service.handleMentalHealthAssessment(tenantId, assessment);

        verify(healthScoreRepository).save(any(HealthScoreEntity.class));
    }

    @Test
    void shouldHandleCareGapAddressed() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        CareGapEntity careGap = CareGapEntity.builder()
            .patientId(patientId)
            .category(CareGapEntity.GapCategory.PREVENTIVE_CARE)
            .build();

        HealthScoreEntity current = HealthScoreEntity.builder()
            .physicalHealthScore(60.0)
            .mentalHealthScore(70.0)
            .socialDeterminantsScore(80.0)
            .preventiveCareScore(85.0)
            .chronicDiseaseScore(75.0)
            .overallScore(70.0)
            .build();

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(current));
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        service.handleCareGapAddressed(tenantId, careGap);

        ArgumentCaptor<HealthScoreEntity> captor = ArgumentCaptor.forClass(HealthScoreEntity.class);
        verify(healthScoreRepository).save(captor.capture());
        assertThat(captor.getValue().getPreventiveCareScore()).isGreaterThan(85.0);
    }

    @Test
    void shouldHandleCareGapAddressedForChronicDisease() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        CareGapEntity careGap = CareGapEntity.builder()
            .patientId(patientId)
            .category(CareGapEntity.GapCategory.CHRONIC_DISEASE)
            .build();

        HealthScoreEntity current = HealthScoreEntity.builder()
            .physicalHealthScore(60.0)
            .mentalHealthScore(70.0)
            .socialDeterminantsScore(80.0)
            .preventiveCareScore(85.0)
            .chronicDiseaseScore(70.0)
            .overallScore(70.0)
            .build();

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(current));
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        service.handleCareGapAddressed(tenantId, careGap);

        ArgumentCaptor<HealthScoreEntity> captor = ArgumentCaptor.forClass(HealthScoreEntity.class);
        verify(healthScoreRepository).save(captor.capture());
        assertThat(captor.getValue().getChronicDiseaseScore()).isGreaterThan(70.0);
    }

    @Test
    void shouldHandleCareGapAddressedForMentalHealth() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        CareGapEntity careGap = CareGapEntity.builder()
            .patientId(patientId)
            .category(CareGapEntity.GapCategory.MENTAL_HEALTH)
            .build();

        HealthScoreEntity current = HealthScoreEntity.builder()
            .physicalHealthScore(60.0)
            .mentalHealthScore(60.0)
            .socialDeterminantsScore(80.0)
            .preventiveCareScore(85.0)
            .chronicDiseaseScore(70.0)
            .overallScore(70.0)
            .build();

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(current));
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        service.handleCareGapAddressed(tenantId, careGap);

        ArgumentCaptor<HealthScoreEntity> captor = ArgumentCaptor.forClass(HealthScoreEntity.class);
        verify(healthScoreRepository).save(captor.capture());
        assertThat(captor.getValue().getMentalHealthScore()).isGreaterThan(60.0);
    }

    @Test
    void shouldHandleConditionEventForChronicConditionAndPublishAlert() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        HealthScoreEntity current = HealthScoreEntity.builder()
            .physicalHealthScore(80.0)
            .mentalHealthScore(70.0)
            .socialDeterminantsScore(70.0)
            .preventiveCareScore(70.0)
            .chronicDiseaseScore(80.0)
            .overallScore(75.0)
            .build();

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(current));
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        Map<String, Object> event = conditionEvent(
            tenantId,
            patientId,
            "http://snomed.info/sct",
            "13645005",
            "COPD",
            "mild",
            "active"
        );

        service.handleConditionEvent(event);

        ArgumentCaptor<HealthScoreEntity> captor = ArgumentCaptor.forClass(HealthScoreEntity.class);
        verify(healthScoreRepository).save(captor.capture());
        assertThat(captor.getValue().getChronicDiseaseScore()).isCloseTo(69.2, within(0.05));
        assertThat(captor.getValue().getPhysicalHealthScore()).isCloseTo(71.0, within(0.05));
        verify(kafkaTemplate).send(eq("condition.alert.needed"), eq(patientId.toString()), any());
    }

    @Test
    void shouldCoverVitalSignAdjustmentRanges() {
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateBPSystolicAdjustment", 85.0)).isEqualTo(-10.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateBPSystolicAdjustment", 115.0)).isEqualTo(5.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateBPSystolicAdjustment", 125.0)).isEqualTo(2.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateBPSystolicAdjustment", 135.0)).isEqualTo(-3.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateBPSystolicAdjustment", 160.0)).isEqualTo(-8.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateBPSystolicAdjustment", 190.0)).isEqualTo(-15.0);

        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateBPDiastolicAdjustment", 55.0)).isEqualTo(-10.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateBPDiastolicAdjustment", 70.0)).isEqualTo(5.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateBPDiastolicAdjustment", 85.0)).isEqualTo(-3.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateBPDiastolicAdjustment", 100.0)).isEqualTo(-8.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateBPDiastolicAdjustment", 130.0)).isEqualTo(-15.0);

        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateHeartRateAdjustment", 35.0)).isEqualTo(-10.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateHeartRateAdjustment", 55.0)).isEqualTo(-3.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateHeartRateAdjustment", 80.0)).isEqualTo(3.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateHeartRateAdjustment", 110.0)).isEqualTo(-3.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateHeartRateAdjustment", 140.0)).isEqualTo(-10.0);

        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateBMIAdjustment", 15.0)).isEqualTo(-15.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateBMIAdjustment", 17.0)).isEqualTo(-8.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateBMIAdjustment", 23.0)).isEqualTo(5.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateBMIAdjustment", 28.0)).isEqualTo(-3.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateBMIAdjustment", 33.0)).isEqualTo(-8.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateBMIAdjustment", 37.0)).isEqualTo(-12.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateBMIAdjustment", 45.0)).isEqualTo(-18.0);

        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateGlucoseAdjustment", 60.0)).isEqualTo(-8.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateGlucoseAdjustment", 90.0)).isEqualTo(5.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateGlucoseAdjustment", 110.0)).isEqualTo(-2.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateGlucoseAdjustment", 150.0)).isEqualTo(-8.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateGlucoseAdjustment", 220.0)).isEqualTo(-15.0);

        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateA1CAdjustment", 5.4)).isEqualTo(5.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateA1CAdjustment", 6.0)).isEqualTo(-3.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateA1CAdjustment", 6.7)).isEqualTo(-6.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateA1CAdjustment", 8.0)).isEqualTo(-10.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateA1CAdjustment", 10.0)).isEqualTo(-15.0);

        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateOxygenSaturationAdjustment", 96.0)).isEqualTo(3.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateOxygenSaturationAdjustment", 92.0)).isEqualTo(-5.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateOxygenSaturationAdjustment", 87.0)).isEqualTo(-10.0);
        assertThat((double) ReflectionTestUtils.invokeMethod(
            service, "calculateOxygenSaturationAdjustment", 80.0)).isEqualTo(-18.0);
    }

    private Map<String, Object> conditionEvent(
        String tenantId,
        UUID patientId,
        String system,
        String code,
        String display,
        String severity,
        String clinicalStatus
    ) {
        return Map.of(
            "tenantId", tenantId,
            "patientId", patientId.toString(),
            "resource", Map.of(
                "clinicalStatus", Map.of(
                    "coding", List.of(Map.of("code", clinicalStatus))
                ),
                "severity", Map.of(
                    "coding", List.of(Map.of("code", severity))
                ),
                "code", Map.of(
                    "coding", List.of(Map.of(
                        "system", system,
                        "code", code,
                        "display", display
                    ))
                )
            )
        );
    }

    @Test
    void shouldHandleCareGapAddressedForSocialDeterminants() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        CareGapEntity careGap = CareGapEntity.builder()
            .patientId(patientId)
            .category(CareGapEntity.GapCategory.SOCIAL_DETERMINANTS)
            .build();

        HealthScoreEntity current = HealthScoreEntity.builder()
            .physicalHealthScore(60.0)
            .mentalHealthScore(60.0)
            .socialDeterminantsScore(60.0)
            .preventiveCareScore(85.0)
            .chronicDiseaseScore(70.0)
            .overallScore(70.0)
            .build();

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(current));
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        service.handleCareGapAddressed(tenantId, careGap);

        ArgumentCaptor<HealthScoreEntity> captor = ArgumentCaptor.forClass(HealthScoreEntity.class);
        verify(healthScoreRepository).save(captor.capture());
        assertThat(captor.getValue().getSocialDeterminantsScore()).isGreaterThan(60.0);
    }

    @Test
    void shouldIgnoreCareGapCategoryWithoutScoreImpact() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        CareGapEntity careGap = CareGapEntity.builder()
            .patientId(patientId)
            .category(CareGapEntity.GapCategory.MEDICATION)
            .build();

        HealthScoreEntity current = HealthScoreEntity.builder()
            .physicalHealthScore(60.0)
            .mentalHealthScore(60.0)
            .socialDeterminantsScore(60.0)
            .preventiveCareScore(85.0)
            .chronicDiseaseScore(70.0)
            .overallScore(70.0)
            .build();

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(current));
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        service.handleCareGapAddressed(tenantId, careGap);

        ArgumentCaptor<HealthScoreEntity> captor = ArgumentCaptor.forClass(HealthScoreEntity.class);
        verify(healthScoreRepository).save(captor.capture());
        assertThat(captor.getValue().getChronicDiseaseScore()).isEqualTo(70.0);
    }

    @Test
    void shouldReturnWithoutCareGapScoreWhenNoCurrentScore() {
        CareGapEntity careGap = CareGapEntity.builder()
            .patientId(UUID.randomUUID())
            .category(CareGapEntity.GapCategory.PREVENTIVE_CARE)
            .build();

        when(healthScoreRepository.findLatestByPatientId("tenant-1", careGap.getPatientId()))
            .thenReturn(Optional.empty());

        service.handleCareGapAddressed("tenant-1", careGap);

        verify(healthScoreRepository, never()).save(any());
    }

    @Test
    void shouldProcessConditionEventAndPublishAlert() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        HealthScoreEntity current = HealthScoreEntity.builder()
            .physicalHealthScore(80.0)
            .mentalHealthScore(80.0)
            .socialDeterminantsScore(80.0)
            .preventiveCareScore(80.0)
            .chronicDiseaseScore(80.0)
            .overallScore(80.0)
            .build();
        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(current));
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        Map<String, Object> condition = Map.of(
            "clinicalStatus", Map.of("coding", List.of(Map.of("code", "active"))),
            "severity", Map.of("coding", List.of(Map.of("code", "severe"))),
            "code", Map.of("coding", List.of(
                Map.of(
                    "code", "42343007",
                    "display", "Congestive heart failure",
                    "system", "http://snomed.info/sct"
                )
            ))
        );

        Map<String, Object> event = Map.of(
            "tenantId", tenantId,
            "patientId", patientId.toString(),
            "resource", condition
        );

        service.handleConditionEvent(event);

        verify(healthScoreRepository).save(any(HealthScoreEntity.class));
        verify(kafkaTemplate).send(eq("condition.alert.needed"), eq(patientId.toString()), any());
    }

    @Test
    void shouldProcessConditionEventFromSubjectReference() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.empty());
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        Map<String, Object> condition = Map.of(
            "clinicalStatus", Map.of("coding", List.of(Map.of("code", "active"))),
            "code", Map.of("coding", List.of(
                Map.of(
                    "code", "J44",
                    "display", "COPD",
                    "system", "http://hl7.org/fhir/sid/icd-10-cm"
                )
            )),
            "subject", Map.of("reference", "Patient/" + patientId)
        );

        Map<String, Object> event = Map.of(
            "tenantId", tenantId,
            "resource", condition
        );

        service.handleConditionEvent(event);

        verify(healthScoreRepository).save(any(HealthScoreEntity.class));
        verify(kafkaTemplate).send(eq("condition.alert.needed"), eq(patientId.toString()), any());
    }

    @Test
    void shouldSkipConditionEventWhenMissingTenantId() {
        Map<String, Object> event = Map.of(
            "patientId", UUID.randomUUID().toString(),
            "resource", Map.of()
        );

        service.handleConditionEvent(event);

        verify(healthScoreRepository, never()).save(any());
    }

    @Test
    void shouldSkipConditionEventWhenPatientIdInvalid() {
        Map<String, Object> condition = Map.of(
            "clinicalStatus", Map.of("coding", List.of(Map.of("code", "active"))),
            "code", Map.of("coding", List.of(
                Map.of("code", "I50", "system", "http://hl7.org/fhir/sid/icd-10-cm")
            ))
        );

        Map<String, Object> event = Map.of(
            "tenantId", "tenant-1",
            "patientId", "bad-id",
            "resource", condition
        );

        service.handleConditionEvent(event);

        verify(healthScoreRepository, never()).save(any());
    }

    @Test
    void shouldNotCreateAlertForType1DiabetesWithoutSevereSeverity() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.empty());
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        Map<String, Object> condition = Map.of(
            "clinicalStatus", Map.of("coding", List.of(Map.of("code", "active"))),
            "severity", Map.of("coding", List.of(Map.of("code", "255604002"))),
            "code", Map.of("coding", List.of(
                Map.of(
                    "code", "E10",
                    "display", "Type 1 diabetes mellitus",
                    "system", "http://hl7.org/fhir/sid/icd-10-cm"
                )
            ))
        );

        Map<String, Object> event = Map.of(
            "tenantId", tenantId,
            "patientId", patientId.toString(),
            "resource", condition
        );

        service.handleConditionEvent(event);

        verify(healthScoreRepository).save(any(HealthScoreEntity.class));
        verify(kafkaTemplate, never()).send(eq("condition.alert.needed"), any(), any());
    }

    @Test
    void shouldHandleWebSocketBroadcastFailures() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        HealthScoreEntity previous = HealthScoreEntity.builder()
            .overallScore(40.0)
            .build();
        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(previous));
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });
        doThrow(new RuntimeException("ws")).when(webSocketHandler)
            .broadcastHealthScoreUpdate(any(), eq(tenantId));
        doThrow(new RuntimeException("ws")).when(webSocketHandler)
            .broadcastSignificantChange(any(), eq(tenantId));

        HealthScoreComponents components = HealthScoreComponents.builder()
            .physicalHealthScore(90.0)
            .mentalHealthScore(90.0)
            .socialDeterminantsScore(90.0)
            .preventiveCareScore(90.0)
            .chronicDiseaseScore(90.0)
            .build();

        service.calculateHealthScore(tenantId, patientId, components);

        verify(kafkaTemplate).send(eq("health-score.updated"), eq(patientId.toString()), any());
        verify(kafkaTemplate).send(eq("health-score.significant-change"), eq(patientId.toString()), any());
    }

    @Test
    void shouldExtractPatientIdViaReflection() {
        UUID patientId = UUID.randomUUID();

        class EventWithPatientId {
            public UUID getPatientId() { return patientId; }
        }

        UUID extracted = ReflectionTestUtils.invokeMethod(
            service, "extractPatientIdFromEvent", new EventWithPatientId());

        assertThat(extracted).isEqualTo(patientId);
    }

    @Test
    void shouldReturnNullWhenReflectionFails() {
        class EventWithFailingGetter {
            public UUID getPatientId() { throw new RuntimeException("fail"); }
        }

        UUID extracted = ReflectionTestUtils.invokeMethod(
            service, "extractPatientIdFromEvent", new EventWithFailingGetter());

        assertThat(extracted).isNull();
    }
    @Test
    void shouldProcessConditionEventWithSevereSnomeSeverityCode() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.empty());
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        Map<String, Object> condition = Map.of(
            "clinicalStatus", Map.of("coding", List.of(Map.of("code", "active"))),
            "severity", Map.of("coding", List.of(Map.of("code", "24484000"))),
            "code", Map.of("coding", List.of(
                Map.of(
                    "code", "42343007",
                    "display", "Congestive heart failure",
                    "system", "http://snomed.info/sct"
                )
            ))
        );

        Map<String, Object> event = Map.of(
            "tenantId", tenantId,
            "patientId", patientId.toString(),
            "resource", condition
        );

        service.handleConditionEvent(event);

        verify(healthScoreRepository).save(any(HealthScoreEntity.class));
        verify(kafkaTemplate).send(eq("condition.alert.needed"), eq(patientId.toString()), any());
    }

    @Test
    void shouldProcessConditionEventWithoutAlert() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.empty());
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        Map<String, Object> condition = Map.of(
            "clinicalStatus", Map.of("coding", List.of(Map.of("code", "active"))),
            "severity", Map.of("coding", List.of(Map.of("code", "255604002"))),
            "code", Map.of("coding", List.of(
                Map.of(
                    "code", "I10",
                    "display", "Hypertension",
                    "system", "http://hl7.org/fhir/sid/icd-10-cm"
                )
            ))
        );

        Map<String, Object> event = Map.of(
            "tenantId", tenantId,
            "patientId", patientId.toString(),
            "resource", condition
        );

        service.handleConditionEvent(event);

        verify(healthScoreRepository).save(any(HealthScoreEntity.class));
        verify(kafkaTemplate, never()).send(eq("condition.alert.needed"), any(), any());
    }

    @Test
    void shouldApplyModerateSeverityMultiplierForConditionImpact() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        HealthScoreEntity current = HealthScoreEntity.builder()
            .physicalHealthScore(80.0)
            .mentalHealthScore(80.0)
            .socialDeterminantsScore(80.0)
            .preventiveCareScore(80.0)
            .chronicDiseaseScore(80.0)
            .overallScore(80.0)
            .build();
        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(current));
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        Map<String, Object> condition = Map.of(
            "clinicalStatus", Map.of("coding", List.of(Map.of("code", "active"))),
            "severity", Map.of("coding", List.of(Map.of("code", "6736007"))),
            "code", Map.of("coding", List.of(
                Map.of(
                    "code", "44054006",
                    "display", "Type 2 diabetes mellitus",
                    "system", "http://snomed.info/sct"
                )
            ))
        );

        Map<String, Object> event = Map.of(
            "tenantId", tenantId,
            "patientId", patientId.toString(),
            "resource", condition
        );

        service.handleConditionEvent(event);

        ArgumentCaptor<HealthScoreEntity> captor = ArgumentCaptor.forClass(HealthScoreEntity.class);
        verify(healthScoreRepository).save(captor.capture());
        assertThat(captor.getValue().getChronicDiseaseScore()).isCloseTo(68.0, within(0.01));
        assertThat(captor.getValue().getPhysicalHealthScore()).isCloseTo(72.0, within(0.01));
    }

    @Test
    void shouldApplyDefaultSeverityMultiplierWhenMissing() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        HealthScoreEntity current = HealthScoreEntity.builder()
            .physicalHealthScore(80.0)
            .mentalHealthScore(80.0)
            .socialDeterminantsScore(80.0)
            .preventiveCareScore(80.0)
            .chronicDiseaseScore(80.0)
            .overallScore(80.0)
            .build();
        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(current));
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        Map<String, Object> condition = Map.of(
            "clinicalStatus", Map.of("coding", List.of(Map.of("code", "active"))),
            "code", Map.of("coding", List.of(
                Map.of(
                    "code", "53741008",
                    "display", "Coronary artery disease",
                    "system", "http://snomed.info/sct"
                )
            ))
        );

        Map<String, Object> event = Map.of(
            "tenantId", tenantId,
            "patientId", patientId.toString(),
            "resource", condition
        );

        service.handleConditionEvent(event);

        ArgumentCaptor<HealthScoreEntity> captor = ArgumentCaptor.forClass(HealthScoreEntity.class);
        verify(healthScoreRepository).save(captor.capture());
        assertThat(captor.getValue().getChronicDiseaseScore()).isCloseTo(62.0, within(0.01));
        assertThat(captor.getValue().getPhysicalHealthScore()).isCloseTo(65.0, within(0.01));
    }

    @Test
    void shouldApplyMildSeverityMultiplierForConditionImpact() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        HealthScoreEntity current = HealthScoreEntity.builder()
            .physicalHealthScore(80.0)
            .mentalHealthScore(80.0)
            .socialDeterminantsScore(80.0)
            .preventiveCareScore(80.0)
            .chronicDiseaseScore(80.0)
            .overallScore(80.0)
            .build();
        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(current));
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        Map<String, Object> condition = Map.of(
            "clinicalStatus", Map.of("coding", List.of(Map.of("code", "active"))),
            "severity", Map.of("coding", List.of(Map.of("code", "255604002"))),
            "code", Map.of("coding", List.of(
                Map.of(
                    "code", "44054006",
                    "display", "Type 2 diabetes mellitus",
                    "system", "http://snomed.info/sct"
                )
            ))
        );

        Map<String, Object> event = Map.of(
            "tenantId", tenantId,
            "patientId", patientId.toString(),
            "resource", condition
        );

        service.handleConditionEvent(event);

        ArgumentCaptor<HealthScoreEntity> captor = ArgumentCaptor.forClass(HealthScoreEntity.class);
        verify(healthScoreRepository).save(captor.capture());
        assertThat(captor.getValue().getChronicDiseaseScore()).isCloseTo(72.8, within(0.01));
        assertThat(captor.getValue().getPhysicalHealthScore()).isCloseTo(75.2, within(0.01));
    }

    @Test
    void shouldCreateAlertForCancerIcd10Condition() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.empty());
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        Map<String, Object> condition = Map.of(
            "clinicalStatus", Map.of("coding", List.of(Map.of("code", "active"))),
            "code", Map.of("coding", List.of(
                Map.of(
                    "code", "C50",
                    "display", "Breast cancer",
                    "system", "http://hl7.org/fhir/sid/icd-10-cm"
                )
            ))
        );

        Map<String, Object> event = Map.of(
            "tenantId", tenantId,
            "patientId", patientId.toString(),
            "resource", condition
        );

        service.handleConditionEvent(event);

        verify(kafkaTemplate).send(eq("condition.alert.needed"), eq(patientId.toString()), any());
    }
    @Test
    void shouldSkipConditionEventWhenMissingCode() {
        UUID patientId = UUID.randomUUID();
        Map<String, Object> condition = Map.of(
            "clinicalStatus", Map.of("coding", List.of(Map.of("code", "active")))
        );

        Map<String, Object> event = Map.of(
            "tenantId", "tenant-1",
            "patientId", patientId.toString(),
            "resource", condition
        );

        service.handleConditionEvent(event);

        verify(healthScoreRepository, never()).save(any());
    }

    @Test
    void shouldCreateAlertForSevereType1Diabetes() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.empty());
        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(invocation -> {
            HealthScoreEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        Map<String, Object> condition = Map.of(
            "clinicalStatus", Map.of("coding", List.of(Map.of("code", "active"))),
            "severity", Map.of("coding", List.of(Map.of("code", "severe"))),
            "code", Map.of("coding", List.of(
                Map.of(
                    "code", "E10",
                    "display", "Type 1 diabetes mellitus",
                    "system", "http://hl7.org/fhir/sid/icd-10-cm"
                )
            ))
        );

        Map<String, Object> event = Map.of(
            "tenantId", tenantId,
            "patientId", patientId.toString(),
            "resource", condition
        );

        service.handleConditionEvent(event);

        verify(kafkaTemplate).send(eq("condition.alert.needed"), eq(patientId.toString()), any());
    }
    @Test
    void shouldSkipInactiveConditionEvent() {
        UUID patientId = UUID.randomUUID();
        Map<String, Object> condition = Map.of(
            "clinicalStatus", Map.of("coding", List.of(Map.of("code", "inactive"))),
            "code", Map.of("coding", List.of(
                Map.of("code", "I10", "system", "http://hl7.org/fhir/sid/icd-10-cm")
            ))
        );

        Map<String, Object> event = Map.of(
            "tenantId", "tenant-1",
            "patientId", patientId.toString(),
            "resource", condition
        );

        service.handleConditionEvent(event);

        verify(healthScoreRepository, never()).save(any());
    }

    @Test
    void shouldSkipNonChronicConditionEvent() {
        UUID patientId = UUID.randomUUID();
        Map<String, Object> condition = Map.of(
            "clinicalStatus", Map.of("coding", List.of(Map.of("code", "active"))),
            "code", Map.of("coding", List.of(
                Map.of("code", "UNKNOWN", "system", "http://example.com")
            ))
        );

        Map<String, Object> event = Map.of(
            "tenantId", "tenant-1",
            "patientId", patientId.toString(),
            "resource", condition
        );

        service.handleConditionEvent(event);

        verify(healthScoreRepository, never()).save(any());
    }

    @Test
    void shouldReturnCurrentHealthScore() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        HealthScoreEntity entity = HealthScoreEntity.builder()
            .id(UUID.randomUUID())
            .patientId(patientId)
            .tenantId(tenantId)
            .overallScore(78.0)
            .physicalHealthScore(78.0)
            .mentalHealthScore(78.0)
            .socialDeterminantsScore(78.0)
            .preventiveCareScore(78.0)
            .chronicDiseaseScore(78.0)
            .calculatedAt(Instant.now())
            .build();

        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(entity));

        Optional<HealthScoreDTO> result = service.getCurrentHealthScore(tenantId, patientId);

        assertThat(result).isPresent();
        assertThat(result.get().getOverallScore()).isEqualTo(78.0);
    }

    @Test
    void shouldReturnHistoryDtos() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        HealthScoreHistoryEntity history = HealthScoreHistoryEntity.builder()
            .id(UUID.randomUUID())
            .patientId(patientId)
            .tenantId(tenantId)
            .overallScore(80.0)
            .calculatedAt(Instant.now())
            .build();

        when(healthScoreHistoryRepository.findByPatientIdOrderByCalculatedAtDesc(tenantId, patientId))
            .thenReturn(List.of(history));

        List<HealthScoreDTO> dtos = service.getHealthScoreHistory(tenantId, patientId);

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).getOverallScore()).isEqualTo(80.0);
    }

    @Test
    void shouldReturnAtRiskPatients() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        HealthScoreEntity entity = HealthScoreEntity.builder()
            .id(UUID.randomUUID())
            .patientId(patientId)
            .tenantId(tenantId)
            .overallScore(30.0)
            .physicalHealthScore(30.0)
            .mentalHealthScore(30.0)
            .socialDeterminantsScore(30.0)
            .preventiveCareScore(30.0)
            .chronicDiseaseScore(30.0)
            .calculatedAt(Instant.now())
            .build();

        Page<HealthScoreEntity> page = new PageImpl<>(List.of(entity));
        when(healthScoreRepository.findLatestScoresBelowThreshold(eq(tenantId), eq(50.0), any()))
            .thenReturn(page);

        Page<HealthScoreDTO> result =
            service.getAtRiskPatients(tenantId, 50.0, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOverallScore()).isEqualTo(30.0);
    }

    @Test
    void shouldReturnSignificantChanges() {
        UUID patientId = UUID.randomUUID();
        String tenantId = "tenant-1";

        HealthScoreEntity entity = HealthScoreEntity.builder()
            .id(UUID.randomUUID())
            .patientId(patientId)
            .tenantId(tenantId)
            .overallScore(90.0)
            .physicalHealthScore(90.0)
            .mentalHealthScore(90.0)
            .socialDeterminantsScore(90.0)
            .preventiveCareScore(90.0)
            .chronicDiseaseScore(90.0)
            .calculatedAt(Instant.now())
            .build();

        Page<HealthScoreEntity> page = new PageImpl<>(List.of(entity));
        when(healthScoreRepository.findSignificantChangesSince(eq(tenantId), any(), any()))
            .thenReturn(page);

        Page<HealthScoreDTO> result =
            service.getSignificantChanges(tenantId, Instant.now(), PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOverallScore()).isEqualTo(90.0);
    }
}
