package com.healthdata.quality.service;

import com.healthdata.quality.dto.MentalHealthAssessmentDTO;
import com.healthdata.quality.dto.MentalHealthAssessmentRequest;
import com.healthdata.quality.persistence.MentalHealthAssessmentEntity;
import com.healthdata.quality.persistence.MentalHealthAssessmentRepository;
import com.healthdata.quality.service.notification.MentalHealthNotificationTrigger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MentalHealthAssessmentService
 * Validates mental health scoring algorithms
 */
@ExtendWith(MockitoExtension.class)
class MentalHealthAssessmentServiceTest {

    @Mock
    private MentalHealthAssessmentRepository repository;

    @Mock
    private CareGapService careGapService;

    @Mock
    private MentalHealthNotificationTrigger notificationTrigger;

    @InjectMocks
    private MentalHealthAssessmentService service;

    private String tenantId;
    private String patientId;
    private String assessedBy;

    @BeforeEach
    void setUp() {
        tenantId = "test-tenant";
        patientId = "Patient/123";
        assessedBy = "Practitioner/Dr-Smith";

        // Setup common repository mock behavior
        when(repository.save(any(MentalHealthAssessmentEntity.class))).thenAnswer(invocation -> {
            MentalHealthAssessmentEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            entity.setCreatedAt(Instant.now());
            entity.setUpdatedAt(Instant.now());
            return entity;
        });
    }

    /**
     * PHQ-9 Scoring Tests
     */

    @Test
    void testPHQ9_MinimalDepression() {
        // Arrange: Score of 3 (0-4 = minimal)
        MentalHealthAssessmentRequest request = createPHQ9Request(
            Map.of("q1", 0, "q2", 0, "q3", 1, "q4", 1, "q5", 1, "q6", 0, "q7", 0, "q8", 0, "q9", 0)
        );

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(3, result.getScore());
        assertEquals(27, result.getMaxScore());
        assertEquals("minimal", result.getSeverity());
        assertEquals("Minimal or no depression", result.getInterpretation());
        assertFalse(result.getPositiveScreen());
        assertEquals(10, result.getThresholdScore());
        assertFalse(result.getRequiresFollowup());

        // Verify no care gap created
        verify(careGapService, never()).createMentalHealthFollowupGap(anyString(), any());
    }

    @Test
    void testPHQ9_MildDepression() {
        // Arrange: Score of 7 (5-9 = mild)
        MentalHealthAssessmentRequest request = createPHQ9Request(
            Map.of("q1", 1, "q2", 1, "q3", 1, "q4", 1, "q5", 1, "q6", 1, "q7", 1, "q8", 0, "q9", 0)
        );

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(7, result.getScore());
        assertEquals("mild", result.getSeverity());
        assertFalse(result.getPositiveScreen());
    }

    @Test
    void testPHQ9_ModerateDepression() {
        // Arrange: Score of 12 (10-14 = moderate, positive screen)
        MentalHealthAssessmentRequest request = createPHQ9Request(
            Map.of("q1", 2, "q2", 2, "q3", 1, "q4", 1, "q5", 1, "q6", 2, "q7", 1, "q8", 1, "q9", 1)
        );

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(12, result.getScore());
        assertEquals("moderate", result.getSeverity());
        assertTrue(result.getPositiveScreen());
        assertTrue(result.getRequiresFollowup());

        // Verify care gap created
        ArgumentCaptor<MentalHealthAssessmentEntity> captor =
            ArgumentCaptor.forClass(MentalHealthAssessmentEntity.class);
        verify(careGapService).createMentalHealthFollowupGap(eq(tenantId), captor.capture());
        assertEquals(12, captor.getValue().getScore());
    }

    @Test
    void testPHQ9_ModeratelySevereDepression() {
        // Arrange: Score of 17 (15-19 = moderately-severe)
        MentalHealthAssessmentRequest request = createPHQ9Request(
            Map.of("q1", 2, "q2", 2, "q3", 2, "q4", 2, "q5", 2, "q6", 2, "q7", 2, "q8", 2, "q9", 1)
        );

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(17, result.getScore());
        assertEquals("moderately-severe", result.getSeverity());
        assertEquals("Moderately severe depression", result.getInterpretation());
        assertTrue(result.getPositiveScreen());
    }

    @Test
    void testPHQ9_SevereDepression() {
        // Arrange: Score of 24 (20-27 = severe)
        MentalHealthAssessmentRequest request = createPHQ9Request(
            Map.of("q1", 3, "q2", 3, "q3", 3, "q4", 3, "q5", 3, "q6", 3, "q7", 3, "q8", 3, "q9", 0)
        );

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(24, result.getScore());
        assertEquals("severe", result.getSeverity());
        assertEquals("Severe depression", result.getInterpretation());
        assertTrue(result.getPositiveScreen());
    }

    /**
     * GAD-7 Scoring Tests
     */

    @Test
    void testGAD7_MinimalAnxiety() {
        // Arrange: Score of 3 (0-4 = minimal)
        MentalHealthAssessmentRequest request = createGAD7Request(
            Map.of("q1", 0, "q2", 1, "q3", 1, "q4", 1, "q5", 0, "q6", 0, "q7", 0)
        );

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(3, result.getScore());
        assertEquals(21, result.getMaxScore());
        assertEquals("minimal", result.getSeverity());
        assertFalse(result.getPositiveScreen());
    }

    @Test
    void testGAD7_ModerateAnxiety() {
        // Arrange: Score of 12 (10-14 = moderate, positive screen)
        MentalHealthAssessmentRequest request = createGAD7Request(
            Map.of("q1", 2, "q2", 2, "q3", 2, "q4", 2, "q5", 1, "q6", 1, "q7", 2)
        );

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(12, result.getScore());
        assertEquals("moderate", result.getSeverity());
        assertTrue(result.getPositiveScreen());
        assertTrue(result.getRequiresFollowup());
    }

    @Test
    void testGAD7_SevereAnxiety() {
        // Arrange: Score of 18 (15-21 = severe)
        MentalHealthAssessmentRequest request = createGAD7Request(
            Map.of("q1", 3, "q2", 3, "q3", 2, "q4", 3, "q5", 2, "q6", 3, "q7", 2)
        );

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(18, result.getScore());
        assertEquals("severe", result.getSeverity());
        assertTrue(result.getPositiveScreen());
    }

    /**
     * PHQ-2 Scoring Tests
     */

    @Test
    void testPHQ2_NegativeScreen() {
        // Arrange: Score of 2 (0-2 = negative)
        MentalHealthAssessmentRequest request = createPHQ2Request(
            Map.of("q1", 1, "q2", 1)
        );

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(2, result.getScore());
        assertEquals(6, result.getMaxScore());
        assertEquals("negative", result.getSeverity());
        assertEquals("Negative screen for depression", result.getInterpretation());
        assertFalse(result.getPositiveScreen());
        assertEquals(3, result.getThresholdScore());
    }

    @Test
    void testPHQ2_PositiveScreen() {
        // Arrange: Score of 4 (3-6 = positive)
        MentalHealthAssessmentRequest request = createPHQ2Request(
            Map.of("q1", 2, "q2", 2)
        );

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(4, result.getScore());
        assertEquals("positive", result.getSeverity());
        assertEquals("Positive screen - recommend full PHQ-9 assessment", result.getInterpretation());
        assertTrue(result.getPositiveScreen());
        assertTrue(result.getRequiresFollowup());
    }

    /**
     * PHQ-A Scoring Tests (Adolescent Depression)
     */

    @Test
    void testPHQA_MinimalDepression() {
        // Arrange: Score of 4 (0-4 = minimal)
        MentalHealthAssessmentRequest request = createPHQARequest(
            Map.of("q1", 0, "q2", 1, "q3", 1, "q4", 1, "q5", 1, "q6", 0, "q7", 0, "q8", 0, "q9", 0)
        );

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(4, result.getScore());
        assertEquals(27, result.getMaxScore());
        assertEquals("minimal", result.getSeverity());
        assertEquals("Minimal or no depression", result.getInterpretation());
        assertFalse(result.getPositiveScreen());
        assertEquals(11, result.getThresholdScore());
        assertFalse(result.getRequiresFollowup());
    }

    @Test
    void testPHQA_MildDepression() {
        // Arrange: Score of 8 (5-10 = mild)
        MentalHealthAssessmentRequest request = createPHQARequest(
            Map.of("q1", 1, "q2", 1, "q3", 1, "q4", 1, "q5", 1, "q6", 1, "q7", 1, "q8", 1, "q9", 0)
        );

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(8, result.getScore());
        assertEquals("mild", result.getSeverity());
        assertFalse(result.getPositiveScreen());
    }

    @Test
    void testPHQA_ModerateDepression() {
        // Arrange: Score of 13 (11-15 = moderate, positive screen)
        MentalHealthAssessmentRequest request = createPHQARequest(
            Map.of("q1", 2, "q2", 2, "q3", 1, "q4", 1, "q5", 2, "q6", 1, "q7", 1, "q8", 2, "q9", 1)
        );

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(13, result.getScore());
        assertEquals("moderate", result.getSeverity());
        assertTrue(result.getPositiveScreen());
        assertTrue(result.getRequiresFollowup());
    }

    @Test
    void testPHQA_SevereDepression() {
        // Arrange: Score of 21 (16-27 = severe)
        MentalHealthAssessmentRequest request = createPHQARequest(
            Map.of("q1", 3, "q2", 3, "q3", 2, "q4", 2, "q5", 3, "q6", 2, "q7", 2, "q8", 2, "q9", 2)
        );

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(21, result.getScore());
        assertEquals("severe", result.getSeverity());
        assertEquals("Severe depression", result.getInterpretation());
        assertTrue(result.getPositiveScreen());
    }

    /**
     * AUDIT-C Scoring Tests (Alcohol Use)
     */

    @Test
    void testAUDITC_NegativeScreen_Male() {
        // Arrange: Score of 3 (< 4 for men = negative)
        MentalHealthAssessmentRequest request = createAUDITCRequest(
            Map.of("q1", 1, "q2", 1, "q3", 1),
            "male"
        );

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(3, result.getScore());
        assertEquals(12, result.getMaxScore());
        assertEquals("negative", result.getSeverity());
        assertEquals("Negative screen for hazardous alcohol use", result.getInterpretation());
        assertFalse(result.getPositiveScreen());
        assertEquals(4, result.getThresholdScore());
    }

    @Test
    void testAUDITC_PositiveScreen_Male() {
        // Arrange: Score of 5 (>= 4 for men = positive)
        MentalHealthAssessmentRequest request = createAUDITCRequest(
            Map.of("q1", 2, "q2", 2, "q3", 1),
            "male"
        );

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(5, result.getScore());
        assertEquals("positive", result.getSeverity());
        assertEquals("Positive screen for hazardous alcohol use", result.getInterpretation());
        assertTrue(result.getPositiveScreen());
        assertTrue(result.getRequiresFollowup());
    }

    @Test
    void testAUDITC_NegativeScreen_Female() {
        // Arrange: Score of 2 (< 3 for women = negative)
        MentalHealthAssessmentRequest request = createAUDITCRequest(
            Map.of("q1", 1, "q2", 1, "q3", 0),
            "female"
        );

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(2, result.getScore());
        assertEquals("negative", result.getSeverity());
        assertFalse(result.getPositiveScreen());
        assertEquals(3, result.getThresholdScore());
    }

    @Test
    void testAUDITC_PositiveScreen_Female() {
        // Arrange: Score of 4 (>= 3 for women = positive)
        MentalHealthAssessmentRequest request = createAUDITCRequest(
            Map.of("q1", 2, "q2", 1, "q3", 1),
            "female"
        );

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(4, result.getScore());
        assertEquals("positive", result.getSeverity());
        assertTrue(result.getPositiveScreen());
    }

    /**
     * DAST-10 Scoring Tests (Drug Abuse)
     */

    @Test
    void testDAST10_NoDrugProblem() {
        // Arrange: Score of 0 (0 = none)
        MentalHealthAssessmentRequest request = createDAST10Request(
            Map.of("q1", 0, "q2", 0, "q3", 0, "q4", 0, "q5", 0,
                   "q6", 0, "q7", 0, "q8", 0, "q9", 0, "q10", 0)
        );

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(0, result.getScore());
        assertEquals(10, result.getMaxScore());
        assertEquals("none", result.getSeverity());
        assertEquals("No drug problem reported", result.getInterpretation());
        assertFalse(result.getPositiveScreen());
        assertEquals(1, result.getThresholdScore());
    }

    @Test
    void testDAST10_LowLevel() {
        // Arrange: Score of 2 (1-2 = low)
        MentalHealthAssessmentRequest request = createDAST10Request(
            Map.of("q1", 1, "q2", 0, "q3", 1, "q4", 0, "q5", 0,
                   "q6", 0, "q7", 0, "q8", 0, "q9", 0, "q10", 0)
        );

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(2, result.getScore());
        assertEquals("low", result.getSeverity());
        assertEquals("Low level drug problem", result.getInterpretation());
        assertTrue(result.getPositiveScreen());
    }

    @Test
    void testDAST10_ModerateLevel() {
        // Arrange: Score of 4 (3-5 = moderate)
        MentalHealthAssessmentRequest request = createDAST10Request(
            Map.of("q1", 1, "q2", 1, "q3", 1, "q4", 0, "q5", 1,
                   "q6", 0, "q7", 0, "q8", 0, "q9", 0, "q10", 0)
        );

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(4, result.getScore());
        assertEquals("moderate", result.getSeverity());
        assertTrue(result.getPositiveScreen());
        assertTrue(result.getRequiresFollowup());
    }

    @Test
    void testDAST10_SubstantialLevel() {
        // Arrange: Score of 7 (6-8 = substantial)
        MentalHealthAssessmentRequest request = createDAST10Request(
            Map.of("q1", 1, "q2", 1, "q3", 1, "q4", 1, "q5", 1,
                   "q6", 1, "q7", 1, "q8", 0, "q9", 0, "q10", 0)
        );

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(7, result.getScore());
        assertEquals("substantial", result.getSeverity());
        assertEquals("Substantial drug problem", result.getInterpretation());
        assertTrue(result.getPositiveScreen());
    }

    @Test
    void testDAST10_SevereLevel() {
        // Arrange: Score of 9 (9-10 = severe)
        MentalHealthAssessmentRequest request = createDAST10Request(
            Map.of("q1", 1, "q2", 1, "q3", 1, "q4", 1, "q5", 1,
                   "q6", 1, "q7", 1, "q8", 1, "q9", 1, "q10", 0)
        );

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(9, result.getScore());
        assertEquals("severe", result.getSeverity());
        assertEquals("Severe drug problem", result.getInterpretation());
        assertTrue(result.getPositiveScreen());
    }

    /**
     * PCL-5 Scoring Tests (PTSD)
     */

    @Test
    void testPCL5_NegativeScreen() {
        // Arrange: Score of 25 (< 31 = negative)
        // PCL-5 has 20 items scored 0-4, total max 80
        Map<String, Integer> responses = new java.util.HashMap<>();
        // 13 items with score 1 = 13 points
        for (int i = 1; i <= 13; i++) {
            responses.put("q" + i, 1);
        }
        // 4 items with score 2 = 8 points
        responses.put("q14", 2);
        responses.put("q15", 2);
        responses.put("q16", 2);
        responses.put("q17", 2);
        // 3 items with score 1 = 3 points
        responses.put("q18", 1);
        responses.put("q19", 1);
        responses.put("q20", 1);
        // Total: 13 + 8 + 3 = 24, need 1 more for 25
        responses.put("q13", 2); // Change q13 from 1 to 2 to add 1 more point
        // Total: 12*1 + 5*2 + 3*1 = 12 + 10 + 3 = 25

        MentalHealthAssessmentRequest request = createPCL5Request(responses);

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(25, result.getScore());
        assertEquals(80, result.getMaxScore());
        assertEquals("negative", result.getSeverity());
        assertEquals("Below clinical threshold for PTSD", result.getInterpretation());
        assertFalse(result.getPositiveScreen());
        assertEquals(31, result.getThresholdScore());
    }

    @Test
    void testPCL5_PositiveScreen() {
        // Arrange: Score of 35 (>= 31 = positive)
        Map<String, Integer> responses = new java.util.HashMap<>();
        for (int i = 1; i <= 20; i++) {
            responses.put("q" + i, i <= 12 ? 2 : 1); // 12*2 + 8*1 = 32, adjust to 35
        }
        responses.put("q13", 2);
        responses.put("q14", 2);
        responses.put("q15", 2);

        MentalHealthAssessmentRequest request = createPCL5Request(responses);

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(35, result.getScore());
        assertEquals("positive", result.getSeverity());
        assertEquals("Probable PTSD diagnosis", result.getInterpretation());
        assertTrue(result.getPositiveScreen());
        assertTrue(result.getRequiresFollowup());
    }

    @Test
    void testPCL5_HighScore() {
        // Arrange: Score of 60 (high severity)
        Map<String, Integer> responses = new java.util.HashMap<>();
        for (int i = 1; i <= 20; i++) {
            responses.put("q" + i, 3); // All 3s = 60
        }

        MentalHealthAssessmentRequest request = createPCL5Request(responses);

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(60, result.getScore());
        assertTrue(result.getPositiveScreen());
    }

    /**
     * MDQ Scoring Tests (Mood Disorder / Bipolar)
     */

    @Test
    void testMDQ_NegativeScreen_InsufficientYes() {
        // Arrange: 5 yes answers (< 7 = negative)
        Map<String, Integer> responses = new java.util.HashMap<>();
        for (int i = 1; i <= 13; i++) {
            responses.put("q" + i, i <= 5 ? 1 : 0);
        }
        responses.put("same_time", 1);
        responses.put("problems_caused", 1);

        MentalHealthAssessmentRequest request = createMDQRequest(responses);

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(5, result.getScore());
        assertEquals(13, result.getMaxScore());
        assertEquals("negative", result.getSeverity());
        assertEquals("Negative screen for bipolar disorder", result.getInterpretation());
        assertFalse(result.getPositiveScreen());
        assertEquals(7, result.getThresholdScore());
    }

    @Test
    void testMDQ_NegativeScreen_NoProblems() {
        // Arrange: 8 yes answers but problems_caused = 0
        Map<String, Integer> responses = new java.util.HashMap<>();
        for (int i = 1; i <= 13; i++) {
            responses.put("q" + i, i <= 8 ? 1 : 0);
        }
        responses.put("same_time", 1);
        responses.put("problems_caused", 0);

        MentalHealthAssessmentRequest request = createMDQRequest(responses);

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(8, result.getScore());
        assertEquals("negative", result.getSeverity());
        assertFalse(result.getPositiveScreen());
    }

    @Test
    void testMDQ_NegativeScreen_NotSameTime() {
        // Arrange: 8 yes answers but same_time = 0
        Map<String, Integer> responses = new java.util.HashMap<>();
        for (int i = 1; i <= 13; i++) {
            responses.put("q" + i, i <= 8 ? 1 : 0);
        }
        responses.put("same_time", 0);
        responses.put("problems_caused", 1);

        MentalHealthAssessmentRequest request = createMDQRequest(responses);

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(8, result.getScore());
        assertEquals("negative", result.getSeverity());
        assertFalse(result.getPositiveScreen());
    }

    @Test
    void testMDQ_PositiveScreen() {
        // Arrange: 9 yes answers + problems + same time
        Map<String, Integer> responses = new java.util.HashMap<>();
        for (int i = 1; i <= 13; i++) {
            responses.put("q" + i, i <= 9 ? 1 : 0);
        }
        responses.put("same_time", 1);
        responses.put("problems_caused", 1);

        MentalHealthAssessmentRequest request = createMDQRequest(responses);

        // Act
        MentalHealthAssessmentDTO result = service.submitAssessment(tenantId, request);

        // Assert
        assertEquals(9, result.getScore());
        assertEquals("positive", result.getSeverity());
        assertEquals("Positive screen for bipolar disorder", result.getInterpretation());
        assertTrue(result.getPositiveScreen());
        assertTrue(result.getRequiresFollowup());
    }

    /**
     * Helper Methods
     */

    private MentalHealthAssessmentRequest createPHQ9Request(Map<String, Integer> responses) {
        return MentalHealthAssessmentRequest.builder()
            .patientId(patientId)
            .assessmentType("phq-9")
            .responses(responses)
            .assessedBy(assessedBy)
            .assessmentDate(Instant.now())
            .build();
    }

    private MentalHealthAssessmentRequest createGAD7Request(Map<String, Integer> responses) {
        return MentalHealthAssessmentRequest.builder()
            .patientId(patientId)
            .assessmentType("gad-7")
            .responses(responses)
            .assessedBy(assessedBy)
            .assessmentDate(Instant.now())
            .build();
    }

    private MentalHealthAssessmentRequest createPHQ2Request(Map<String, Integer> responses) {
        return MentalHealthAssessmentRequest.builder()
            .patientId(patientId)
            .assessmentType("phq-2")
            .responses(responses)
            .assessedBy(assessedBy)
            .assessmentDate(Instant.now())
            .build();
    }

    private MentalHealthAssessmentRequest createPHQARequest(Map<String, Integer> responses) {
        return MentalHealthAssessmentRequest.builder()
            .patientId(patientId)
            .assessmentType("phq-a")
            .responses(responses)
            .assessedBy(assessedBy)
            .assessmentDate(Instant.now())
            .build();
    }

    private MentalHealthAssessmentRequest createAUDITCRequest(Map<String, Integer> responses, String gender) {
        MentalHealthAssessmentRequest.MentalHealthAssessmentRequestBuilder builder = MentalHealthAssessmentRequest.builder()
            .patientId(patientId)
            .assessmentType("audit-c")
            .responses(responses)
            .assessedBy(assessedBy)
            .assessmentDate(Instant.now());

        // Add gender as clinical note for AUDIT-C
        if (gender != null) {
            builder.clinicalNotes("gender:" + gender);
        }

        return builder.build();
    }

    private MentalHealthAssessmentRequest createDAST10Request(Map<String, Integer> responses) {
        return MentalHealthAssessmentRequest.builder()
            .patientId(patientId)
            .assessmentType("dast-10")
            .responses(responses)
            .assessedBy(assessedBy)
            .assessmentDate(Instant.now())
            .build();
    }

    private MentalHealthAssessmentRequest createPCL5Request(Map<String, Integer> responses) {
        return MentalHealthAssessmentRequest.builder()
            .patientId(patientId)
            .assessmentType("pcl-5")
            .responses(responses)
            .assessedBy(assessedBy)
            .assessmentDate(Instant.now())
            .build();
    }

    private MentalHealthAssessmentRequest createMDQRequest(Map<String, Integer> responses) {
        return MentalHealthAssessmentRequest.builder()
            .patientId(patientId)
            .assessmentType("mdq")
            .responses(responses)
            .assessedBy(assessedBy)
            .assessmentDate(Instant.now())
            .build();
    }

    private MentalHealthAssessmentEntity createMockEntity(
        MentalHealthAssessmentEntity.AssessmentType type,
        int score,
        int maxScore,
        String severity,
        boolean positiveScreen
    ) {
        return MentalHealthAssessmentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(tenantId)
            .patientId(patientId)
            .type(type)
            .score(score)
            .maxScore(maxScore)
            .severity(severity)
            .positiveScreen(positiveScreen)
            .requiresFollowup(positiveScreen)
            .assessedBy(assessedBy)
            .assessmentDate(Instant.now())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }
}
