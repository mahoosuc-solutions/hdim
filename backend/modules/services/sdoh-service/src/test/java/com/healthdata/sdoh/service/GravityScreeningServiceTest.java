package com.healthdata.sdoh.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.sdoh.entity.SdohAssessmentEntity;
import com.healthdata.sdoh.model.*;
import com.healthdata.sdoh.repository.SdohAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for GravityScreeningService
 *
 * Testing Gravity Project FHIR Implementation Guide for SDOH screening.
 * Covers AHC-HRSN and PRAPARE screening instruments.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("Gravity Screening Service Tests")
class GravityScreeningServiceTest {

    @Mock
    private SdohAssessmentRepository assessmentRepository;

    @Mock
    private ZCodeMapper zCodeMapper;

    @Mock
    private SdohRiskCalculator riskCalculator;

    private GravityScreeningService screeningService;
    private ObjectMapper objectMapper;

    private static final String TENANT_ID = "tenant-001";
    private static final String PATIENT_ID = "patient-001";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        screeningService = new GravityScreeningService(
                assessmentRepository, zCodeMapper, riskCalculator, objectMapper);
    }

    @Nested
    @DisplayName("Questionnaire Creation Tests")
    class QuestionnaireCreationTests {

        @Test
        @DisplayName("Should create AHC-HRSN questionnaire with 10 questions")
        void shouldCreateAhcHrsnQuestionnaire() {
            // When
            List<SdohScreeningQuestion> questions = screeningService.createAhcHrsnQuestionnaire();

            // Then
            assertNotNull(questions);
            assertEquals(10, questions.size());

            // Verify key questions are present
            assertTrue(questions.stream().anyMatch(q -> q.getQuestionId().equals("ahc-food-1")));
            assertTrue(questions.stream().anyMatch(q -> q.getQuestionId().equals("ahc-housing-1")));
            assertTrue(questions.stream().anyMatch(q -> q.getQuestionId().equals("ahc-transport-1")));
            assertTrue(questions.stream().anyMatch(q -> q.getQuestionId().equals("ahc-utilities-1")));
            assertTrue(questions.stream().anyMatch(q -> q.getQuestionId().equals("ahc-financial-1")));

            // Verify categories covered
            assertTrue(questions.stream().anyMatch(q -> q.getCategory() == SdohCategory.FOOD_INSECURITY));
            assertTrue(questions.stream().anyMatch(q -> q.getCategory() == SdohCategory.HOUSING_INSTABILITY));
            assertTrue(questions.stream().anyMatch(q -> q.getCategory() == SdohCategory.TRANSPORTATION));
        }

        @Test
        @DisplayName("Should create PRAPARE questionnaire with 20 questions")
        void shouldCreatePrapareQuestionnaire() {
            // When
            List<SdohScreeningQuestion> questions = screeningService.createPrapareQuestionnaire();

            // Then
            assertNotNull(questions);
            assertEquals(20, questions.size());

            // Verify key PRAPARE questions are present
            assertTrue(questions.stream().anyMatch(q -> q.getQuestionId().equals("prapare-housing-1")));
            assertTrue(questions.stream().anyMatch(q -> q.getQuestionId().equals("prapare-employment-1")));
            assertTrue(questions.stream().anyMatch(q -> q.getQuestionId().equals("prapare-education-1")));
        }

        @Test
        @DisplayName("Should include required LOINC codes in questions")
        void shouldIncludeLoincCodes() {
            // When
            List<SdohScreeningQuestion> questions = screeningService.createAhcHrsnQuestionnaire();

            // Then - all questions should have LOINC codes
            for (SdohScreeningQuestion question : questions) {
                assertNotNull(question.getLoincCode(), "Question " + question.getQuestionId() + " missing LOINC code");
                assertTrue(question.getLoincCode().matches("\\d{5}-\\d"),
                        "Invalid LOINC code format: " + question.getLoincCode());
            }
        }

        @Test
        @DisplayName("Should include answer options for each question")
        void shouldIncludeAnswerOptions() {
            // When
            List<SdohScreeningQuestion> questions = screeningService.createAhcHrsnQuestionnaire();

            // Then
            for (SdohScreeningQuestion question : questions) {
                assertNotNull(question.getAnswerOptions());
                assertTrue(question.getAnswerOptions().length >= 2,
                        "Question should have at least 2 answer options");
            }
        }
    }

    @Nested
    @DisplayName("Screening Submission Tests")
    class ScreeningSubmissionTests {

        @Test
        @DisplayName("Should submit screening with valid responses")
        void shouldSubmitScreeningWithValidResponses() {
            // Given
            List<SdohScreeningResponse> responses = Arrays.asList(
                    SdohScreeningResponse.builder().questionId("ahc-food-1").answer("Sometimes true").build(),
                    SdohScreeningResponse.builder().questionId("ahc-food-2").answer("Never true").build(),
                    SdohScreeningResponse.builder().questionId("ahc-housing-1").answer("I have housing").build()
            );

            when(zCodeMapper.mapNeedsToZCodes(any())).thenReturn(Arrays.asList("Z59.4"));

            SdohRiskScore mockRiskScore = SdohRiskScore.builder().totalScore(25.0).build();
            when(riskCalculator.calculateRiskScore(any(SdohAssessment.class))).thenReturn(mockRiskScore);

            when(assessmentRepository.save(any(SdohAssessmentEntity.class)))
                    .thenAnswer(invocation -> {
                        SdohAssessmentEntity entity = invocation.getArgument(0);
                        entity.setAssessmentId("assessment-001");
                        return entity;
                    });

            // When
            SdohAssessment assessment = screeningService.submitScreening(
                    TENANT_ID, PATIENT_ID, "AHC-HRSN", responses);

            // Then
            assertNotNull(assessment);
            assertEquals(TENANT_ID, assessment.getTenantId());
            assertEquals(PATIENT_ID, assessment.getPatientId());
            assertEquals("AHC-HRSN", assessment.getScreeningTool());
            assertEquals(SdohAssessment.AssessmentStatus.COMPLETED, assessment.getStatus());
            assertNotNull(assessment.getRiskScore());

            verify(assessmentRepository).save(any(SdohAssessmentEntity.class));
        }

        @Test
        @DisplayName("Should reject screening with null tenant ID")
        void shouldRejectScreeningWithNullTenantId() {
            // Given
            List<SdohScreeningResponse> responses = Arrays.asList(
                    SdohScreeningResponse.builder().questionId("ahc-food-1").answer("Yes").build()
            );

            // When/Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> screeningService.submitScreening(null, PATIENT_ID, "AHC-HRSN", responses));

            assertEquals("Tenant ID cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should reject screening with null patient ID")
        void shouldRejectScreeningWithNullPatientId() {
            // Given
            List<SdohScreeningResponse> responses = Arrays.asList(
                    SdohScreeningResponse.builder().questionId("ahc-food-1").answer("Yes").build()
            );

            // When/Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> screeningService.submitScreening(TENANT_ID, null, "AHC-HRSN", responses));

            assertEquals("Patient ID cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Should reject screening with empty responses")
        void shouldRejectScreeningWithEmptyResponses() {
            // When/Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> screeningService.submitScreening(TENANT_ID, PATIENT_ID, "AHC-HRSN", Collections.emptyList()));

            assertEquals("Responses cannot be empty", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Needs Identification Tests")
    class NeedsIdentificationTests {

        @Test
        @DisplayName("Should identify food insecurity need from responses")
        void shouldIdentifyFoodInsecurityNeed() {
            // Given - response indicating food insecurity
            List<SdohScreeningResponse> responses = Arrays.asList(
                    SdohScreeningResponse.builder().questionId("ahc-food-1").answer("Often true").build(),
                    SdohScreeningResponse.builder().questionId("ahc-food-2").answer("Sometimes true").build()
            );

            // When
            Map<SdohCategory, Boolean> needs = screeningService.identifyNeeds(responses);

            // Then
            assertTrue(needs.containsKey(SdohCategory.FOOD_INSECURITY));
            assertTrue(needs.get(SdohCategory.FOOD_INSECURITY));
        }

        @Test
        @DisplayName("Should identify housing need from responses")
        void shouldIdentifyHousingNeed() {
            // Given - response indicating housing instability
            List<SdohScreeningResponse> responses = Arrays.asList(
                    SdohScreeningResponse.builder().questionId("ahc-housing-1").answer("Often worried").build()
            );

            // When
            Map<SdohCategory, Boolean> needs = screeningService.identifyNeeds(responses);

            // Then
            assertTrue(needs.containsKey(SdohCategory.HOUSING_INSTABILITY));
            assertTrue(needs.get(SdohCategory.HOUSING_INSTABILITY));
        }

        @Test
        @DisplayName("Should identify transportation need from responses")
        void shouldIdentifyTransportationNeed() {
            // Given - response indicating transportation issues
            List<SdohScreeningResponse> responses = Arrays.asList(
                    SdohScreeningResponse.builder().questionId("ahc-transport-1").answer("Yes").build()
            );

            // When
            Map<SdohCategory, Boolean> needs = screeningService.identifyNeeds(responses);

            // Then
            assertTrue(needs.containsKey(SdohCategory.TRANSPORTATION));
            assertTrue(needs.get(SdohCategory.TRANSPORTATION));
        }

        @Test
        @DisplayName("Should not identify needs when responses are negative")
        void shouldNotIdentifyNeedsWhenResponsesAreNegative() {
            // Given - all negative responses
            List<SdohScreeningResponse> responses = Arrays.asList(
                    SdohScreeningResponse.builder().questionId("ahc-food-1").answer("Never true").build(),
                    SdohScreeningResponse.builder().questionId("ahc-housing-1").answer("No").build()
            );

            // When
            Map<SdohCategory, Boolean> needs = screeningService.identifyNeeds(responses);

            // Then
            assertTrue(needs.isEmpty() || !needs.values().stream().anyMatch(v -> v));
        }
    }

    @Nested
    @DisplayName("Assessment Retrieval Tests")
    class AssessmentRetrievalTests {

        @Test
        @DisplayName("Should get most recent assessment for patient")
        void shouldGetMostRecentAssessment() {
            // Given
            SdohAssessmentEntity recentEntity = createAssessmentEntity(
                    "assessment-002", LocalDateTime.now(), SdohAssessment.AssessmentStatus.COMPLETED);

            when(assessmentRepository.findMostRecentByTenantIdAndPatientId(TENANT_ID, PATIENT_ID))
                    .thenReturn(Optional.of(recentEntity));

            // When
            Optional<SdohAssessment> assessment = screeningService.getMostRecentAssessment(TENANT_ID, PATIENT_ID);

            // Then
            assertTrue(assessment.isPresent());
            assertEquals("assessment-002", assessment.get().getAssessmentId());
        }

        @Test
        @DisplayName("Should return empty when no assessments found")
        void shouldReturnEmptyWhenNoAssessmentsFound() {
            // Given
            when(assessmentRepository.findMostRecentByTenantIdAndPatientId(TENANT_ID, PATIENT_ID))
                    .thenReturn(Optional.empty());

            // When
            Optional<SdohAssessment> assessment = screeningService.getMostRecentAssessment(TENANT_ID, PATIENT_ID);

            // Then
            assertFalse(assessment.isPresent());
        }

        @Test
        @DisplayName("Should get all assessments for patient")
        void shouldGetAllAssessmentsForPatient() {
            // Given
            List<SdohAssessmentEntity> entities = Arrays.asList(
                    createAssessmentEntity("assessment-001", LocalDateTime.now().minusDays(7), SdohAssessment.AssessmentStatus.COMPLETED),
                    createAssessmentEntity("assessment-002", LocalDateTime.now(), SdohAssessment.AssessmentStatus.COMPLETED)
            );

            when(assessmentRepository.findByTenantIdAndPatientId(TENANT_ID, PATIENT_ID))
                    .thenReturn(entities);

            // When
            List<SdohAssessment> assessments = screeningService.getAssessmentsByPatient(TENANT_ID, PATIENT_ID);

            // Then
            assertEquals(2, assessments.size());
        }
    }

    @Nested
    @DisplayName("Assessment Status Tests")
    class AssessmentStatusTests {

        @Test
        @DisplayName("Should update assessment status from IN_PROGRESS to COMPLETED")
        void shouldUpdateAssessmentStatus() {
            // Given
            String assessmentId = "assessment-001";
            SdohAssessmentEntity existingEntity = createAssessmentEntity(
                    assessmentId, LocalDateTime.now(), SdohAssessment.AssessmentStatus.IN_PROGRESS);

            when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(existingEntity));
            when(assessmentRepository.save(any(SdohAssessmentEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            screeningService.updateAssessmentStatus(assessmentId, SdohAssessment.AssessmentStatus.COMPLETED);

            // Then
            ArgumentCaptor<SdohAssessmentEntity> captor = ArgumentCaptor.forClass(SdohAssessmentEntity.class);
            verify(assessmentRepository).save(captor.capture());

            assertEquals(SdohAssessment.AssessmentStatus.COMPLETED, captor.getValue().getStatus());
        }

        @Test
        @DisplayName("Should not save when assessment not found")
        void shouldNotSaveWhenAssessmentNotFound() {
            // Given
            when(assessmentRepository.findById("nonexistent")).thenReturn(Optional.empty());

            // When
            screeningService.updateAssessmentStatus("nonexistent", SdohAssessment.AssessmentStatus.COMPLETED);

            // Then
            verify(assessmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Archive Tests")
    class ArchiveTests {

        @Test
        @DisplayName("Should archive old assessments by cutoff date")
        void shouldArchiveOldAssessments() {
            // Given
            LocalDateTime cutoffDate = LocalDateTime.now().minusYears(1);
            List<SdohAssessmentEntity> oldAssessments = Arrays.asList(
                    createAssessmentEntity("old-001", LocalDateTime.now().minusYears(2), SdohAssessment.AssessmentStatus.COMPLETED),
                    createAssessmentEntity("old-002", LocalDateTime.now().minusYears(3), SdohAssessment.AssessmentStatus.COMPLETED)
            );

            when(assessmentRepository.findOldAssessments(cutoffDate)).thenReturn(oldAssessments);

            // When
            int archivedCount = screeningService.archiveOldAssessments(cutoffDate);

            // Then
            assertEquals(2, archivedCount);
            verify(assessmentRepository).saveAll(oldAssessments);

            // Verify statuses were updated
            for (SdohAssessmentEntity entity : oldAssessments) {
                assertEquals(SdohAssessment.AssessmentStatus.ARCHIVED, entity.getStatus());
            }
        }

        @Test
        @DisplayName("Should return zero when no old assessments found")
        void shouldReturnZeroWhenNoOldAssessments() {
            // Given
            LocalDateTime cutoffDate = LocalDateTime.now().minusYears(1);
            when(assessmentRepository.findOldAssessments(cutoffDate)).thenReturn(Collections.emptyList());

            // When
            int archivedCount = screeningService.archiveOldAssessments(cutoffDate);

            // Then
            assertEquals(0, archivedCount);
        }
    }

    @Nested
    @DisplayName("Completion Percentage Tests")
    class CompletionPercentageTests {

        @Test
        @DisplayName("Should calculate completion percentage correctly")
        void shouldCalculateCompletionPercentage() {
            // Given - 10 questions, 5 answered
            List<SdohScreeningQuestion> questions = screeningService.createAhcHrsnQuestionnaire(); // 10 questions
            List<SdohScreeningResponse> responses = Arrays.asList(
                    SdohScreeningResponse.builder().questionId("ahc-food-1").answer("Yes").build(),
                    SdohScreeningResponse.builder().questionId("ahc-food-2").answer("Yes").build(),
                    SdohScreeningResponse.builder().questionId("ahc-housing-1").answer("Yes").build(),
                    SdohScreeningResponse.builder().questionId("ahc-transport-1").answer("Yes").build(),
                    SdohScreeningResponse.builder().questionId("ahc-utilities-1").answer("Yes").build()
            );

            // When
            double percentage = screeningService.calculateCompletionPercentage(questions, responses);

            // Then
            assertEquals(50.0, percentage, 0.001); // 5/10 = 50%
        }

        @Test
        @DisplayName("Should return 100% when all questions answered")
        void shouldReturn100PercentWhenAllAnswered() {
            // Given
            List<SdohScreeningQuestion> questions = Arrays.asList(
                    SdohScreeningQuestion.builder().questionId("q1").build(),
                    SdohScreeningQuestion.builder().questionId("q2").build()
            );
            List<SdohScreeningResponse> responses = Arrays.asList(
                    SdohScreeningResponse.builder().questionId("q1").answer("Yes").build(),
                    SdohScreeningResponse.builder().questionId("q2").answer("No").build()
            );

            // When
            double percentage = screeningService.calculateCompletionPercentage(questions, responses);

            // Then
            assertEquals(100.0, percentage, 0.001);
        }

        @Test
        @DisplayName("Should return 0% for empty questions")
        void shouldReturnZeroForEmptyQuestions() {
            // Given
            List<SdohScreeningQuestion> questions = Collections.emptyList();
            List<SdohScreeningResponse> responses = Collections.emptyList();

            // When
            double percentage = screeningService.calculateCompletionPercentage(questions, responses);

            // Then
            assertEquals(0.0, percentage, 0.001);
        }
    }

    @Nested
    @DisplayName("Z-Code Generation Tests")
    class ZCodeGenerationTests {

        @Test
        @DisplayName("Should generate Z-codes from identified needs")
        void shouldGenerateZCodesFromNeeds() {
            // Given
            Map<SdohCategory, Boolean> needs = new HashMap<>();
            needs.put(SdohCategory.FOOD_INSECURITY, true);
            needs.put(SdohCategory.HOUSING_INSTABILITY, true);

            when(zCodeMapper.mapNeedsToZCodes(needs)).thenReturn(Arrays.asList("Z59.4"));

            // When
            List<String> zCodes = screeningService.generateZCodesFromAssessment(needs);

            // Then
            assertNotNull(zCodes);
            verify(zCodeMapper).mapNeedsToZCodes(needs);
        }
    }

    @Nested
    @DisplayName("Response Validation Tests")
    class ResponseValidationTests {

        @Test
        @DisplayName("Should validate when all required questions answered")
        void shouldValidateWhenAllRequiredQuestionsAnswered() {
            // Given
            List<SdohScreeningQuestion> questions = Arrays.asList(
                    SdohScreeningQuestion.builder().questionId("q1").required(true).build(),
                    SdohScreeningQuestion.builder().questionId("q2").required(true).build(),
                    SdohScreeningQuestion.builder().questionId("q3").required(false).build()
            );
            List<SdohScreeningResponse> responses = Arrays.asList(
                    SdohScreeningResponse.builder().questionId("q1").answer("Yes").build(),
                    SdohScreeningResponse.builder().questionId("q2").answer("No").build()
            );

            // When
            boolean isValid = screeningService.validateResponses(questions, responses);

            // Then
            assertTrue(isValid);
        }

        @Test
        @DisplayName("Should reject when required questions not answered")
        void shouldRejectWhenRequiredQuestionsNotAnswered() {
            // Given
            List<SdohScreeningQuestion> questions = Arrays.asList(
                    SdohScreeningQuestion.builder().questionId("q1").required(true).build(),
                    SdohScreeningQuestion.builder().questionId("q2").required(true).build()
            );
            List<SdohScreeningResponse> responses = Arrays.asList(
                    SdohScreeningResponse.builder().questionId("q1").answer("Yes").build()
                    // q2 not answered
            );

            // When
            boolean isValid = screeningService.validateResponses(questions, responses);

            // Then
            assertFalse(isValid);
        }
    }

    @Nested
    @DisplayName("FHIR Export Tests")
    class FhirExportTests {

        @Test
        @DisplayName("Should map assessment to FHIR QuestionnaireResponse")
        void shouldMapToFhirQuestionnaireResponse() {
            // Given
            SdohAssessment assessment = SdohAssessment.builder()
                    .assessmentId("assessment-001")
                    .tenantId(TENANT_ID)
                    .patientId(PATIENT_ID)
                    .screeningTool("AHC-HRSN")
                    .status(SdohAssessment.AssessmentStatus.COMPLETED)
                    .build();

            // When
            String fhirJson = screeningService.mapToFhirQuestionnaireResponse(assessment);

            // Then
            assertNotNull(fhirJson);
            assertTrue(fhirJson.contains("\"resourceType\": \"QuestionnaireResponse\""));
            assertTrue(fhirJson.contains("assessment-001"));
            assertTrue(fhirJson.contains("\"status\": \"completed\""));
        }
    }

    // Helper method to create assessment entity for tests
    private SdohAssessmentEntity createAssessmentEntity(String assessmentId, LocalDateTime assessmentDate,
                                                         SdohAssessment.AssessmentStatus status) {
        return SdohAssessmentEntity.builder()
                .assessmentId(assessmentId)
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .assessmentDate(assessmentDate)
                .screeningTool("AHC-HRSN")
                .responsesJson("[]")
                .identifiedNeedsJson("{}")
                .identifiedZCodesJson("[]")
                .riskScore(25.0)
                .status(status)
                .createdAt(assessmentDate)
                .updatedAt(assessmentDate)
                .build();
    }
}
