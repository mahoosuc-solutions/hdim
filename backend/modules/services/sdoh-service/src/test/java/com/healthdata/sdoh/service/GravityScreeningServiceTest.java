package com.healthdata.sdoh.service;

import com.healthdata.sdoh.model.*;
import com.healthdata.sdoh.repository.SdohAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TDD Tests for GravityScreeningService
 *
 * Testing Gravity Project FHIR Implementation Guide for SDOH screening
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Gravity Screening Service Tests")
class GravityScreeningServiceTest {

    @Mock
    private SdohAssessmentRepository assessmentRepository;

    @Mock
    private ZCodeMapper zCodeMapper;

    @Mock
    private SdohRiskCalculator riskCalculator;

    @InjectMocks
    private GravityScreeningService screeningService;

    private String tenantId;
    private String patientId;
    private List<SdohScreeningResponse> responses;

    @BeforeEach
    void setUp() {
        tenantId = "tenant-001";
        patientId = "patient-001";
        responses = new ArrayList<>();
    }

    @Test
    @DisplayName("Should create AHC-HRSN screening questionnaire")
    void testCreateAhcHrsnQuestionnaire() {
        // When
        List<SdohScreeningQuestion> questions = screeningService.createAhcHrsnQuestionnaire();

        // Then
        assertNotNull(questions);
        assertFalse(questions.isEmpty());
        assertTrue(questions.size() >= 10, "AHC-HRSN should have at least 10 questions");

        // Verify all questions have required fields
        for (SdohScreeningQuestion question : questions) {
            assertNotNull(question.getQuestionId());
            assertNotNull(question.getQuestionText());
            assertNotNull(question.getCategory());
            assertNotNull(question.getLoincCode());
        }
    }

    @Test
    @DisplayName("Should create PRAPARE screening questionnaire")
    void testCreatePrapareQuestionnaire() {
        // When
        List<SdohScreeningQuestion> questions = screeningService.createPrapareQuestionnaire();

        // Then
        assertNotNull(questions);
        assertFalse(questions.isEmpty());
        assertTrue(questions.size() >= 15, "PRAPARE should have at least 15 questions");

        // Verify categories are covered
        Set<SdohCategory> categories = new HashSet<>();
        for (SdohScreeningQuestion question : questions) {
            categories.add(question.getCategory());
        }
        assertTrue(categories.size() >= 5, "PRAPARE should cover multiple SDOH categories");
    }

    @Test
    @DisplayName("Should get screening questions by category")
    void testGetQuestionsByCategory() {
        // Given
        SdohCategory category = SdohCategory.FOOD_INSECURITY;

        // When
        List<SdohScreeningQuestion> questions = screeningService.getQuestionsByCategory(category);

        // Then
        assertNotNull(questions);
        for (SdohScreeningQuestion question : questions) {
            assertEquals(category, question.getCategory());
        }
    }

    @Test
    @DisplayName("Should submit SDOH screening successfully")
    void testSubmitScreening() {
        // Given
        SdohScreeningResponse response1 = SdohScreeningResponse.builder()
                .questionId("food-security-1")
                .answer("Yes")
                .build();
        responses.add(response1);

        SdohAssessment savedAssessment = SdohAssessment.builder()
                .assessmentId("assessment-001")
                .patientId(patientId)
                .tenantId(tenantId)
                .build();

        when(assessmentRepository.save(any(SdohAssessment.class))).thenReturn(savedAssessment);

        // When
        SdohAssessment result = screeningService.submitScreening(tenantId, patientId, "AHC-HRSN", responses);

        // Then
        assertNotNull(result);
        assertEquals(patientId, result.getPatientId());
        assertEquals(tenantId, result.getTenantId());
        verify(assessmentRepository, times(1)).save(any(SdohAssessment.class));
    }

    @Test
    @DisplayName("Should validate screening responses")
    void testValidateScreeningResponses() {
        // Given
        SdohScreeningQuestion question = SdohScreeningQuestion.builder()
                .questionId("q1")
                .questionText("Test question")
                .required(true)
                .build();

        SdohScreeningResponse response = SdohScreeningResponse.builder()
                .questionId("q1")
                .answer("Yes")
                .build();

        List<SdohScreeningQuestion> questions = List.of(question);
        List<SdohScreeningResponse> responses = List.of(response);

        // When
        boolean isValid = screeningService.validateResponses(questions, responses);

        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should fail validation when required question is missing")
    void testValidateScreeningResponsesFailsWhenRequired() {
        // Given
        SdohScreeningQuestion question = SdohScreeningQuestion.builder()
                .questionId("q1")
                .questionText("Test question")
                .required(true)
                .build();

        List<SdohScreeningQuestion> questions = List.of(question);
        List<SdohScreeningResponse> responses = new ArrayList<>();

        // When
        boolean isValid = screeningService.validateResponses(questions, responses);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should identify SDOH needs from responses")
    void testIdentifyNeeds() {
        // Given
        SdohScreeningResponse response = SdohScreeningResponse.builder()
                .questionId("food-security-1")
                .answer("Yes") // Indicates food insecurity
                .build();
        responses.add(response);

        // When
        Map<SdohCategory, Boolean> needs = screeningService.identifyNeeds(responses);

        // Then
        assertNotNull(needs);
        assertFalse(needs.isEmpty());
    }

    @Test
    @DisplayName("Should get patient assessment by ID")
    void testGetAssessmentById() {
        // Given
        String assessmentId = "assessment-001";
        SdohAssessment assessment = SdohAssessment.builder()
                .assessmentId(assessmentId)
                .patientId(patientId)
                .build();

        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

        // When
        Optional<SdohAssessment> result = screeningService.getAssessmentById(assessmentId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(assessmentId, result.get().getAssessmentId());
    }

    @Test
    @DisplayName("Should get all assessments for patient")
    void testGetAssessmentsByPatient() {
        // Given
        List<SdohAssessment> assessments = Arrays.asList(
                SdohAssessment.builder().assessmentId("a1").patientId(patientId).build(),
                SdohAssessment.builder().assessmentId("a2").patientId(patientId).build()
        );

        when(assessmentRepository.findByTenantIdAndPatientId(tenantId, patientId))
                .thenReturn(assessments);

        // When
        List<SdohAssessment> result = screeningService.getAssessmentsByPatient(tenantId, patientId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Should get most recent assessment for patient")
    void testGetMostRecentAssessment() {
        // Given
        SdohAssessment recent = SdohAssessment.builder()
                .assessmentId("recent")
                .patientId(patientId)
                .assessmentDate(LocalDateTime.now())
                .build();

        when(assessmentRepository.findMostRecentByTenantIdAndPatientId(tenantId, patientId))
                .thenReturn(Optional.of(recent));

        // When
        Optional<SdohAssessment> result = screeningService.getMostRecentAssessment(tenantId, patientId);

        // Then
        assertTrue(result.isPresent());
        assertEquals("recent", result.get().getAssessmentId());
    }

    @Test
    @DisplayName("Should update assessment status")
    void testUpdateAssessmentStatus() {
        // Given
        String assessmentId = "assessment-001";
        SdohAssessment assessment = SdohAssessment.builder()
                .assessmentId(assessmentId)
                .status(SdohAssessment.AssessmentStatus.IN_PROGRESS)
                .build();

        when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
        when(assessmentRepository.save(any(SdohAssessment.class))).thenReturn(assessment);

        // When
        screeningService.updateAssessmentStatus(assessmentId, SdohAssessment.AssessmentStatus.COMPLETED);

        // Then
        verify(assessmentRepository, times(1)).save(any(SdohAssessment.class));
    }

    @Test
    @DisplayName("Should map responses to FHIR QuestionnaireResponse")
    void testMapToFhirQuestionnaireResponse() {
        // Given
        SdohAssessment assessment = SdohAssessment.builder()
                .assessmentId("a1")
                .patientId(patientId)
                .responses(responses)
                .build();

        // When
        String fhirResponse = screeningService.mapToFhirQuestionnaireResponse(assessment);

        // Then
        assertNotNull(fhirResponse);
        assertFalse(fhirResponse.isEmpty());
    }

    @Test
    @DisplayName("Should calculate assessment completion percentage")
    void testCalculateCompletionPercentage() {
        // Given
        List<SdohScreeningQuestion> questions = Arrays.asList(
                SdohScreeningQuestion.builder().questionId("q1").build(),
                SdohScreeningQuestion.builder().questionId("q2").build(),
                SdohScreeningQuestion.builder().questionId("q3").build(),
                SdohScreeningQuestion.builder().questionId("q4").build()
        );

        List<SdohScreeningResponse> responses = Arrays.asList(
                SdohScreeningResponse.builder().questionId("q1").answer("Yes").build(),
                SdohScreeningResponse.builder().questionId("q2").answer("No").build()
        );

        // When
        double percentage = screeningService.calculateCompletionPercentage(questions, responses);

        // Then
        assertEquals(50.0, percentage, 0.01);
    }

    @Test
    @DisplayName("Should identify screening tool from assessment")
    void testIdentifyScreeningTool() {
        // Given
        SdohAssessment assessment = SdohAssessment.builder()
                .screeningTool("AHC-HRSN")
                .build();

        // When
        String tool = screeningService.getScreeningTool(assessment);

        // Then
        assertEquals("AHC-HRSN", tool);
    }

    @Test
    @DisplayName("Should integrate with Z-code mapper")
    void testIntegrateWithZCodeMapper() {
        // Given
        Map<SdohCategory, Boolean> needs = new HashMap<>();
        needs.put(SdohCategory.FOOD_INSECURITY, true);

        List<String> zCodes = Arrays.asList("Z59.4");
        when(zCodeMapper.mapNeedsToZCodes(needs)).thenReturn(zCodes);

        // When
        List<String> result = screeningService.generateZCodesFromAssessment(needs);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("Z59.4", result.get(0));
    }

    @Test
    @DisplayName("Should calculate risk score from assessment")
    void testCalculateRiskScore() {
        // Given
        SdohAssessment assessment = SdohAssessment.builder()
                .assessmentId("a1")
                .build();

        SdohRiskScore riskScore = SdohRiskScore.builder()
                .totalScore(65.0)
                .build();

        when(riskCalculator.calculateRiskScore(assessment)).thenReturn(riskScore);

        // When
        SdohRiskScore result = screeningService.calculateRiskScore(assessment);

        // Then
        assertNotNull(result);
        assertEquals(65.0, result.getTotalScore());
    }

    @Test
    @DisplayName("Should archive old assessments")
    void testArchiveOldAssessments() {
        // Given
        LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(6);
        List<SdohAssessment> oldAssessments = Arrays.asList(
                SdohAssessment.builder()
                        .assessmentId("old1")
                        .status(SdohAssessment.AssessmentStatus.COMPLETED)
                        .build()
        );

        when(assessmentRepository.findOldAssessments(cutoffDate))
                .thenReturn(oldAssessments);

        // When
        int archived = screeningService.archiveOldAssessments(cutoffDate);

        // Then
        assertEquals(1, archived);
        verify(assessmentRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Should export assessment as JSON")
    void testExportAssessmentAsJson() {
        // Given
        SdohAssessment assessment = SdohAssessment.builder()
                .assessmentId("a1")
                .patientId(patientId)
                .build();

        // When
        String json = screeningService.exportAssessmentAsJson(assessment);

        // Then
        assertNotNull(json);
        assertTrue(json.contains("assessmentId"));
    }

    @Test
    @DisplayName("Should get screening statistics for tenant")
    void testGetScreeningStatistics() {
        // Given
        when(assessmentRepository.countByTenantId(tenantId)).thenReturn(100L);
        when(assessmentRepository.countByTenantIdAndStatus(tenantId, SdohAssessment.AssessmentStatus.COMPLETED))
                .thenReturn(75L);

        // When
        Map<String, Object> stats = screeningService.getScreeningStatistics(tenantId);

        // Then
        assertNotNull(stats);
        assertEquals(100L, stats.get("totalAssessments"));
        assertEquals(75L, stats.get("completedAssessments"));
    }

    @Test
    @DisplayName("Should handle null tenant ID gracefully")
    void testHandleNullTenantId() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            screeningService.submitScreening(null, patientId, "AHC-HRSN", responses);
        });
    }

    @Test
    @DisplayName("Should handle null patient ID gracefully")
    void testHandleNullPatientId() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            screeningService.submitScreening(tenantId, null, "AHC-HRSN", responses);
        });
    }

    @Test
    @DisplayName("Should handle empty responses list")
    void testHandleEmptyResponses() {
        // Given
        List<SdohScreeningResponse> emptyResponses = new ArrayList<>();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            screeningService.submitScreening(tenantId, patientId, "AHC-HRSN", emptyResponses);
        });
    }
}
