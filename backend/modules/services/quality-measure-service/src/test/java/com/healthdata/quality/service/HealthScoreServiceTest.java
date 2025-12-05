package com.healthdata.quality.service;

import com.healthdata.quality.dto.HealthScoreDTO;
import com.healthdata.quality.persistence.*;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test-Driven Development for Health Score Service
 *
 * Tests health score calculation from multiple components:
 * - Physical health (vitals, labs, chronic conditions) - 30%
 * - Mental health (PHQ-9, GAD-7 scores) - 25%
 * - Social determinants (SDOH screening) - 15%
 * - Preventive care (screening compliance) - 15%
 * - Chronic disease management (care plan adherence, gaps) - 15%
 */
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
    private com.healthdata.quality.websocket.HealthScoreWebSocketHandler webSocketHandler;

    @Mock
    private com.healthdata.quality.service.notification.HealthScoreNotificationTrigger notificationTrigger;

    @InjectMocks
    private HealthScoreService healthScoreService;

    private String tenantId;
    private String patientId;
    private Instant now;

    @BeforeEach
    void setUp() {
        tenantId = "test-tenant";
        patientId = "Patient/123";
        now = Instant.now();
    }

    /**
     * TEST 1: Health Score Calculation from Components
     * Validates weighted scoring algorithm
     */
    @Test
    void testCalculateHealthScore_AllComponentsOptimal() {
        // Arrange: All components at optimal levels
        HealthScoreComponents components = HealthScoreComponents.builder()
            .physicalHealthScore(95.0)      // 30% weight
            .mentalHealthScore(90.0)        // 25% weight
            .socialDeterminantsScore(85.0)  // 15% weight
            .preventiveCareScore(92.0)      // 15% weight
            .chronicDiseaseScore(88.0)      // 15% weight
            .build();

        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(i -> {
            HealthScoreEntity entity = i.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        // Act
        HealthScoreDTO result = healthScoreService.calculateHealthScore(tenantId, patientId, components);

        // Assert
        assertNotNull(result);
        assertEquals(patientId, result.getPatientId());

        // Verify weighted calculation
        double expectedOverall = (95.0 * 0.30) + (90.0 * 0.25) + (85.0 * 0.15) + (92.0 * 0.15) + (88.0 * 0.15);
        assertEquals(expectedOverall, result.getOverallScore(), 0.5);
        assertTrue(result.getOverallScore() >= 90.0);

        // Verify component scores stored
        assertEquals(95.0, result.getPhysicalHealthScore());
        assertEquals(90.0, result.getMentalHealthScore());
        assertEquals(85.0, result.getSocialDeterminantsScore());
        assertEquals(92.0, result.getPreventiveCareScore());
        assertEquals(88.0, result.getChronicDiseaseScore());

        // Verify entity saved
        verify(healthScoreRepository).save(any(HealthScoreEntity.class));
    }

    @Test
    void testCalculateHealthScore_MixedComponents() {
        // Arrange: Mixed component scores
        HealthScoreComponents components = HealthScoreComponents.builder()
            .physicalHealthScore(75.0)      // Average
            .mentalHealthScore(60.0)        // Below average
            .socialDeterminantsScore(80.0)  // Good
            .preventiveCareScore(50.0)      // Poor
            .chronicDiseaseScore(70.0)      // Average
            .build();

        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(i -> {
            HealthScoreEntity entity = i.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        // Act
        HealthScoreDTO result = healthScoreService.calculateHealthScore(tenantId, patientId, components);

        // Assert
        double expectedOverall = (75.0 * 0.30) + (60.0 * 0.25) + (80.0 * 0.15) + (50.0 * 0.15) + (70.0 * 0.15);
        assertEquals(expectedOverall, result.getOverallScore(), 0.5);
        assertTrue(result.getOverallScore() < 75.0);
    }

    /**
     * TEST 2: Health Score Update on Observation Event
     * Validates response to FHIR observation changes
     */
    @Test
    void testUpdateHealthScoreOnObservationEvent_VitalsImproved() {
        // Arrange: Previous score exists
        HealthScoreEntity previousScore = createHealthScoreEntity(70.0, 65.0, 75.0, 70.0, 68.0, 72.0);
        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(previousScore));

        // New observation indicates improved vitals - create Map structure for Kafka event
        // Use LOINC code 8480-6 for systolic blood pressure (recognized by the service)
        // Build with proper mutable HashMap/ArrayList structures for type compatibility
        Map<String, Object> coding1 = new HashMap<>();
        coding1.put("system", "http://loinc.org");
        coding1.put("code", "8480-6");
        coding1.put("display", "Systolic blood pressure");
        List<Map<String, Object>> codings = new ArrayList<>();
        codings.add(coding1);

        Map<String, Object> codeMap = new HashMap<>();
        codeMap.put("coding", codings);

        Map<String, Object> valueQuantity = new HashMap<>();
        valueQuantity.put("value", 118.0);
        valueQuantity.put("unit", "mmHg");

        Map<String, Object> subject = new HashMap<>();
        subject.put("reference", "Patient/" + patientId);

        Map<String, Object> observationResource = new HashMap<>();
        observationResource.put("resourceType", "Observation");
        observationResource.put("id", "bp-reading");
        observationResource.put("subject", subject);
        observationResource.put("code", codeMap);
        observationResource.put("valueQuantity", valueQuantity);
        observationResource.put("effectiveDateTime", now.toString());

        Map<String, Object> observationEvent = new HashMap<>();
        observationEvent.put("tenantId", tenantId);
        observationEvent.put("patientId", patientId);  // Also add direct patientId for easier extraction
        observationEvent.put("resource", observationResource);

        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(i -> {
            HealthScoreEntity entity = i.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        // Act
        healthScoreService.handleObservationEvent(observationEvent);

        // Assert
        ArgumentCaptor<HealthScoreEntity> captor = ArgumentCaptor.forClass(HealthScoreEntity.class);
        verify(healthScoreRepository).save(captor.capture());

        HealthScoreEntity updated = captor.getValue();
        assertNotNull(updated);
        assertTrue(updated.getPhysicalHealthScore() >= 65.0); // Should improve or stay same
        assertEquals(70.0, updated.getPreviousScore()); // Previous overall score
    }

    /**
     * TEST 3: Health Score Update on Mental Health Assessment
     * Validates integration with mental health screening
     */
    @Test
    void testUpdateHealthScoreOnMentalHealthAssessment_Moderate() {
        // Arrange: Previous score
        HealthScoreEntity previousScore = createHealthScoreEntity(75.0, 70.0, 80.0, 75.0, 72.0, 73.0);
        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(previousScore));

        // Mental health assessment: PHQ-9 score of 12 (moderate depression)
        MentalHealthAssessmentEntity assessment = MentalHealthAssessmentEntity.builder()
            .id(UUID.randomUUID())
            .patientId(patientId)
            .tenantId(tenantId)
            .type(MentalHealthAssessmentEntity.AssessmentType.PHQ_9)
            .score(12)
            .maxScore(27)
            .severity("moderate")
            .positiveScreen(true)
            .requiresFollowup(true)
            .thresholdScore(10)
            .interpretation("Moderate depression")
            .assessedBy("Practitioner/Dr-Smith")
            .assessmentDate(now)
            .responses(Map.of("q1", 2, "q2", 2, "q3", 1, "q4", 1, "q5", 1, "q6", 2, "q7", 1, "q8", 1, "q9", 1))
            .build();

        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(i -> {
            HealthScoreEntity entity = i.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        // Act
        healthScoreService.handleMentalHealthAssessment(tenantId, assessment);

        // Assert
        ArgumentCaptor<HealthScoreEntity> captor = ArgumentCaptor.forClass(HealthScoreEntity.class);
        verify(healthScoreRepository).save(captor.capture());

        HealthScoreEntity updated = captor.getValue();

        // Mental health score should reflect moderate depression (~60-70 range)
        assertTrue(updated.getMentalHealthScore() < 75.0);
        assertTrue(updated.getMentalHealthScore() >= 50.0);

        // Overall score should decrease
        assertTrue(updated.getOverallScore() < previousScore.getOverallScore());
    }

    @Test
    void testUpdateHealthScoreOnMentalHealthAssessment_Severe() {
        // Arrange: Previous score
        HealthScoreEntity previousScore = createHealthScoreEntity(75.0, 70.0, 80.0, 75.0, 72.0, 73.0);
        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(previousScore));

        // Severe depression: PHQ-9 score of 22
        MentalHealthAssessmentEntity assessment = MentalHealthAssessmentEntity.builder()
            .id(UUID.randomUUID())
            .patientId(patientId)
            .tenantId(tenantId)
            .type(MentalHealthAssessmentEntity.AssessmentType.PHQ_9)
            .score(22)
            .maxScore(27)
            .severity("severe")
            .positiveScreen(true)
            .requiresFollowup(true)
            .thresholdScore(10)
            .interpretation("Severe depression")
            .assessedBy("Practitioner/Dr-Smith")
            .assessmentDate(now)
            .responses(Map.of("q1", 3, "q2", 3, "q3", 3, "q4", 3, "q5", 3, "q6", 3, "q7", 2, "q8", 1, "q9", 1))
            .build();

        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(i -> {
            HealthScoreEntity entity = i.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        // Act
        healthScoreService.handleMentalHealthAssessment(tenantId, assessment);

        // Assert
        ArgumentCaptor<HealthScoreEntity> captor = ArgumentCaptor.forClass(HealthScoreEntity.class);
        verify(healthScoreRepository).save(captor.capture());

        HealthScoreEntity updated = captor.getValue();

        // Mental health score should be significantly lower
        assertTrue(updated.getMentalHealthScore() < 50.0);
    }

    /**
     * TEST 4: Health Score Update on Care Gap Change
     * Validates impact of care gap addressing
     */
    @Test
    void testUpdateHealthScoreOnCareGapAddressed_PreventiveCare() {
        // Arrange: Previous score with poor preventive care
        // Overall = 75*0.30 + 70*0.25 + 70*0.15 + 50*0.15 + 75*0.15 = 69.25
        HealthScoreEntity previousScore = createHealthScoreEntity(69.25, 75.0, 70.0, 70.0, 50.0, 75.0);
        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(previousScore));

        // Care gap addressed: Colorectal cancer screening completed
        CareGapEntity careGap = CareGapEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .patientId(patientId)
            .category(CareGapEntity.GapCategory.PREVENTIVE_CARE)
            .gapType("COL")
            .title("Colorectal Cancer Screening")
            .status(CareGapEntity.Status.ADDRESSED)
            .priority(CareGapEntity.Priority.HIGH)
            .qualityMeasure("COL")
            .identifiedDate(now.minusSeconds(86400 * 30))
            .addressedDate(now)
            .addressedBy("Practitioner/Dr-Smith")
            .build();

        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(i -> {
            HealthScoreEntity entity = i.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        // Act
        healthScoreService.handleCareGapAddressed(tenantId, careGap);

        // Assert
        ArgumentCaptor<HealthScoreEntity> captor = ArgumentCaptor.forClass(HealthScoreEntity.class);
        verify(healthScoreRepository).save(captor.capture());

        HealthScoreEntity updated = captor.getValue();

        // Preventive care score should improve
        assertTrue(updated.getPreventiveCareScore() > 50.0);

        // Overall score should improve
        assertTrue(updated.getOverallScore() > previousScore.getOverallScore());
    }

    @Test
    void testUpdateHealthScoreOnCareGapAddressed_ChronicDisease() {
        // Arrange
        HealthScoreEntity previousScore = createHealthScoreEntity(70.0, 72.0, 68.0, 70.0, 65.0, 55.0);
        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(previousScore));

        // Chronic disease care gap addressed
        CareGapEntity careGap = CareGapEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .patientId(patientId)
            .category(CareGapEntity.GapCategory.CHRONIC_DISEASE)
            .gapType("CBP")
            .title("Blood Pressure Control")
            .status(CareGapEntity.Status.ADDRESSED)
            .priority(CareGapEntity.Priority.HIGH)
            .qualityMeasure("CBP")
            .addressedDate(now)
            .build();

        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(i -> {
            HealthScoreEntity entity = i.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        // Act
        healthScoreService.handleCareGapAddressed(tenantId, careGap);

        // Assert
        ArgumentCaptor<HealthScoreEntity> captor = ArgumentCaptor.forClass(HealthScoreEntity.class);
        verify(healthScoreRepository).save(captor.capture());

        HealthScoreEntity updated = captor.getValue();

        // Chronic disease score should improve
        assertTrue(updated.getChronicDiseaseScore() > 55.0);
    }

    /**
     * TEST 5: Health Score Update on Condition Change
     * Validates impact of new or updated diagnoses
     */
    @Test
    void testUpdateHealthScoreOnConditionChange_NewChronicCondition() {
        // Arrange
        HealthScoreEntity previousScore = createHealthScoreEntity(85.0, 90.0, 85.0, 80.0, 85.0, 82.0);
        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(previousScore));

        // New chronic condition diagnosed - create Map structure for Kafka event
        // Build with proper mutable HashMap/ArrayList structures for type compatibility
        Map<String, Object> conditionCoding = new HashMap<>();
        conditionCoding.put("system", "http://hl7.org/fhir/sid/icd-10-cm");
        conditionCoding.put("code", "E11.9");
        conditionCoding.put("display", "Type 2 Diabetes Mellitus");
        List<Map<String, Object>> conditionCodings = new ArrayList<>();
        conditionCodings.add(conditionCoding);

        Map<String, Object> conditionCodeMap = new HashMap<>();
        conditionCodeMap.put("coding", conditionCodings);

        Map<String, Object> statusCoding = new HashMap<>();
        statusCoding.put("code", "active");
        List<Map<String, Object>> statusCodings = new ArrayList<>();
        statusCodings.add(statusCoding);

        Map<String, Object> clinicalStatus = new HashMap<>();
        clinicalStatus.put("coding", statusCodings);

        Map<String, Object> conditionSubject = new HashMap<>();
        conditionSubject.put("reference", "Patient/" + patientId);

        Map<String, Object> conditionResource = new HashMap<>();
        conditionResource.put("resourceType", "Condition");
        conditionResource.put("id", "diabetes-type2");
        conditionResource.put("subject", conditionSubject);
        conditionResource.put("code", conditionCodeMap);
        conditionResource.put("clinicalStatus", clinicalStatus);
        conditionResource.put("recordedDate", now.toString());

        Map<String, Object> conditionEvent = new HashMap<>();
        conditionEvent.put("tenantId", tenantId);
        conditionEvent.put("patientId", patientId);  // Also add direct patientId for easier extraction
        conditionEvent.put("resource", conditionResource);

        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(i -> {
            HealthScoreEntity entity = i.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        // Act
        healthScoreService.handleConditionEvent(conditionEvent);

        // Assert
        ArgumentCaptor<HealthScoreEntity> captor = ArgumentCaptor.forClass(HealthScoreEntity.class);
        verify(healthScoreRepository).save(captor.capture());

        HealthScoreEntity updated = captor.getValue();

        // Chronic disease score should decrease
        assertTrue(updated.getChronicDiseaseScore() < 82.0);

        // Overall score should decrease
        assertTrue(updated.getOverallScore() < previousScore.getOverallScore());
    }

    /**
     * TEST 6: Significant Change Detection (threshold >10 points)
     * Validates event publishing for major health changes
     */
    @Test
    void testSignificantChangeDetection_Above10Points() {
        // Arrange: Previous score of 73.5
        // Overall = 70*0.30 + 75*0.25 + 75*0.15 + 72*0.15 + 78*0.15 = 73.5
        HealthScoreEntity previousScore = createHealthScoreEntity(73.5, 70.0, 75.0, 75.0, 72.0, 78.0);
        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(previousScore));

        // New components result in score of 61.0 (change of -12.5 points)
        // Overall = 60*0.30 + 55*0.25 + 65*0.15 + 60*0.15 + 70*0.15 = 61.0
        HealthScoreComponents components = HealthScoreComponents.builder()
            .physicalHealthScore(60.0)
            .mentalHealthScore(55.0)
            .socialDeterminantsScore(65.0)
            .preventiveCareScore(60.0)
            .chronicDiseaseScore(70.0)
            .build();

        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(i -> {
            HealthScoreEntity entity = i.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        // Act
        HealthScoreDTO result = healthScoreService.calculateHealthScore(tenantId, patientId, components);

        // Assert
        ArgumentCaptor<HealthScoreEntity> captor = ArgumentCaptor.forClass(HealthScoreEntity.class);
        verify(healthScoreRepository).save(captor.capture());

        HealthScoreEntity saved = captor.getValue();

        // Verify significant change flag
        assertTrue(saved.isSignificantChange());
        assertNotNull(saved.getChangeReason());
        assertTrue(saved.getChangeReason().toLowerCase().contains("significant"));

        // Verify event published
        verify(kafkaTemplate).send(eq("health-score.significant-change"), anyString(), any());
    }

    @Test
    void testSignificantChangeDetection_Below10Points() {
        // Arrange: Previous score of 73.5
        // Overall = 70*0.30 + 75*0.25 + 75*0.15 + 72*0.15 + 78*0.15 = 73.5
        HealthScoreEntity previousScore = createHealthScoreEntity(73.5, 70.0, 75.0, 75.0, 72.0, 78.0);
        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(previousScore));

        // New components result in score of 70.65 (change of -2.85 points)
        // Overall = 68*0.30 + 70*0.25 + 72*0.15 + 70*0.15 + 75*0.15 = 70.65
        HealthScoreComponents components = HealthScoreComponents.builder()
            .physicalHealthScore(68.0)
            .mentalHealthScore(70.0)
            .socialDeterminantsScore(72.0)
            .preventiveCareScore(70.0)
            .chronicDiseaseScore(75.0)
            .build();

        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(i -> {
            HealthScoreEntity entity = i.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        // Act
        healthScoreService.calculateHealthScore(tenantId, patientId, components);

        // Assert
        ArgumentCaptor<HealthScoreEntity> captor = ArgumentCaptor.forClass(HealthScoreEntity.class);
        verify(healthScoreRepository).save(captor.capture());

        HealthScoreEntity saved = captor.getValue();

        // Verify NOT significant change
        assertFalse(saved.isSignificantChange());

        // Regular update event published, not significant change event
        verify(kafkaTemplate).send(eq("health-score.updated"), anyString(), any());
        verify(kafkaTemplate, never()).send(eq("health-score.significant-change"), anyString(), any());
    }

    /**
     * TEST 7: Health Score History Tracking
     * Validates trend analysis and history storage
     */
    @Test
    void testHealthScoreHistoryTracking() {
        // Arrange
        HealthScoreComponents components = HealthScoreComponents.builder()
            .physicalHealthScore(80.0)
            .mentalHealthScore(75.0)
            .socialDeterminantsScore(85.0)
            .preventiveCareScore(78.0)
            .chronicDiseaseScore(82.0)
            .build();

        HealthScoreEntity previousScore = createHealthScoreEntity(70.0, 65.0, 70.0, 75.0, 68.0, 72.0);
        when(healthScoreRepository.findLatestByPatientId(tenantId, patientId))
            .thenReturn(Optional.of(previousScore));

        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(i -> {
            HealthScoreEntity entity = i.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        when(healthScoreHistoryRepository.save(any(HealthScoreHistoryEntity.class))).thenAnswer(i -> {
            HealthScoreHistoryEntity entity = i.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        // Act
        healthScoreService.calculateHealthScore(tenantId, patientId, components);

        // Assert - History entry created
        ArgumentCaptor<HealthScoreHistoryEntity> historyCaptor =
            ArgumentCaptor.forClass(HealthScoreHistoryEntity.class);
        verify(healthScoreHistoryRepository).save(historyCaptor.capture());

        HealthScoreHistoryEntity history = historyCaptor.getValue();
        assertNotNull(history);
        assertEquals(patientId, history.getPatientId());
        assertEquals(tenantId, history.getTenantId());
        assertNotNull(history.getOverallScore());
        assertNotNull(history.getPreviousScore());
        assertNotNull(history.getScoreDelta());
    }

    @Test
    void testGetHealthScoreHistory() {
        // Arrange
        List<HealthScoreHistoryEntity> historyList = Arrays.asList(
            createHistoryEntity(now.minusSeconds(86400 * 7), 75.0, 70.0),
            createHistoryEntity(now.minusSeconds(86400 * 14), 70.0, 65.0),
            createHistoryEntity(now.minusSeconds(86400 * 21), 65.0, null)
        );

        when(healthScoreHistoryRepository.findByPatientIdOrderByCalculatedAtDesc(tenantId, patientId))
            .thenReturn(historyList);

        // Act
        List<HealthScoreDTO> history = healthScoreService.getHealthScoreHistory(tenantId, patientId);

        // Assert
        assertNotNull(history);
        assertEquals(3, history.size());

        // Verify chronological order (newest first)
        assertTrue(history.get(0).getCalculatedAt().isAfter(history.get(1).getCalculatedAt()));
        assertTrue(history.get(1).getCalculatedAt().isAfter(history.get(2).getCalculatedAt()));

        // Verify trend data
        assertEquals(75.0, history.get(0).getOverallScore());
        assertEquals(5.0, history.get(0).getScoreDelta()); // 75 - 70
    }

    /**
     * TEST 8: Multi-Tenant Isolation
     * Validates data isolation between tenants
     */
    @Test
    void testMultiTenantIsolation() {
        // Arrange: Two tenants with same patient ID
        String tenant1 = "tenant-1";
        String tenant2 = "tenant-2";
        String commonPatientId = "Patient/123";

        HealthScoreComponents components = HealthScoreComponents.builder()
            .physicalHealthScore(80.0)
            .mentalHealthScore(75.0)
            .socialDeterminantsScore(85.0)
            .preventiveCareScore(78.0)
            .chronicDiseaseScore(82.0)
            .build();

        when(healthScoreRepository.save(any(HealthScoreEntity.class))).thenAnswer(i -> {
            HealthScoreEntity entity = i.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        // Act: Calculate scores for both tenants
        healthScoreService.calculateHealthScore(tenant1, commonPatientId, components);
        healthScoreService.calculateHealthScore(tenant2, commonPatientId, components);

        // Assert: Verify both saves used correct tenant IDs
        ArgumentCaptor<HealthScoreEntity> captor = ArgumentCaptor.forClass(HealthScoreEntity.class);
        verify(healthScoreRepository, times(2)).save(captor.capture());

        List<HealthScoreEntity> saved = captor.getAllValues();
        assertEquals(tenant1, saved.get(0).getTenantId());
        assertEquals(tenant2, saved.get(1).getTenantId());

        // Verify repository called with tenant isolation
        verify(healthScoreRepository).findLatestByPatientId(tenant1, commonPatientId);
        verify(healthScoreRepository).findLatestByPatientId(tenant2, commonPatientId);
    }

    @Test
    void testMultiTenantIsolation_HistoryQueries() {
        // Arrange
        String tenant1 = "tenant-1";
        String tenant2 = "tenant-2";

        when(healthScoreHistoryRepository.findByPatientIdOrderByCalculatedAtDesc(anyString(), anyString()))
            .thenReturn(Collections.emptyList());

        // Act
        healthScoreService.getHealthScoreHistory(tenant1, patientId);
        healthScoreService.getHealthScoreHistory(tenant2, patientId);

        // Assert: Verify tenant isolation in queries
        verify(healthScoreHistoryRepository).findByPatientIdOrderByCalculatedAtDesc(tenant1, patientId);
        verify(healthScoreHistoryRepository).findByPatientIdOrderByCalculatedAtDesc(tenant2, patientId);
    }

    /**
     * Helper Methods
     */

    private HealthScoreEntity createHealthScoreEntity(
        Double overallScore,
        Double physicalHealth,
        Double mentalHealth,
        Double socialDeterminants,
        Double preventiveCare,
        Double chronicDisease
    ) {
        return HealthScoreEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .patientId(patientId)
            .overallScore(overallScore)
            .physicalHealthScore(physicalHealth)
            .mentalHealthScore(mentalHealth)
            .socialDeterminantsScore(socialDeterminants)
            .preventiveCareScore(preventiveCare)
            .chronicDiseaseScore(chronicDisease)
            .calculatedAt(now.minusSeconds(3600))
            .previousScore(null)
            .significantChange(false)
            .build();
    }

    private HealthScoreHistoryEntity createHistoryEntity(Instant calculatedAt, Double score, Double previousScore) {
        return HealthScoreHistoryEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .patientId(patientId)
            .overallScore(score)
            .previousScore(previousScore)
            .scoreDelta(previousScore != null ? score - previousScore : null)
            .calculatedAt(calculatedAt)
            .build();
    }

    /**
     * Inner classes for test data
     */
    public static class ObservationEvent {
        private final String observationId;
        private final String patientId;
        private final String loincCode;
        private final String value;
        private final Instant effectiveDateTime;

        public ObservationEvent(String observationId, String patientId, String loincCode,
                               String value, Instant effectiveDateTime) {
            this.observationId = observationId;
            this.patientId = patientId;
            this.loincCode = loincCode;
            this.value = value;
            this.effectiveDateTime = effectiveDateTime;
        }

        public String getObservationId() { return observationId; }
        public String getPatientId() { return patientId; }
        public String getLoincCode() { return loincCode; }
        public String getValue() { return value; }
        public Instant getEffectiveDateTime() { return effectiveDateTime; }
    }

    public static class ConditionEvent {
        private final String conditionId;
        private final String patientId;
        private final String code;
        private final String display;
        private final String clinicalStatus;
        private final Instant recordedDate;

        public ConditionEvent(String conditionId, String patientId, String code,
                            String display, String clinicalStatus, Instant recordedDate) {
            this.conditionId = conditionId;
            this.patientId = patientId;
            this.code = code;
            this.display = display;
            this.clinicalStatus = clinicalStatus;
            this.recordedDate = recordedDate;
        }

        public String getConditionId() { return conditionId; }
        public String getPatientId() { return patientId; }
        public String getCode() { return code; }
        public String getDisplay() { return display; }
        public String getClinicalStatus() { return clinicalStatus; }
        public Instant getRecordedDate() { return recordedDate; }
    }
}
