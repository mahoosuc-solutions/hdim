package com.healthdata.sdoh.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.sdoh.entity.SdohAssessmentEntity;
import com.healthdata.sdoh.model.*;
import com.healthdata.sdoh.repository.SdohAssessmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gravity Project FHIR Implementation Guide for SDOH Screening
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GravityScreeningService {

    private final SdohAssessmentRepository assessmentRepository;
    private final ZCodeMapper zCodeMapper;
    private final SdohRiskCalculator riskCalculator;
    private final ObjectMapper objectMapper;

    /**
     * Create AHC-HRSN (Accountable Health Communities Health-Related Social Needs) questionnaire
     */
    public List<SdohScreeningQuestion> createAhcHrsnQuestionnaire() {
        List<SdohScreeningQuestion> questions = new ArrayList<>();

        // Food Insecurity
        questions.add(SdohScreeningQuestion.builder()
                .questionId("ahc-food-1")
                .questionText("Within the past 12 months, you worried that your food would run out before you got money to buy more.")
                .category(SdohCategory.FOOD_INSECURITY)
                .loincCode("88122-7")
                .questionType(SdohScreeningQuestion.QuestionType.SINGLE_CHOICE)
                .answerOptions(new String[]{"Often true", "Sometimes true", "Never true"})
                .required(true)
                .sequenceNumber(1)
                .build());

        questions.add(SdohScreeningQuestion.builder()
                .questionId("ahc-food-2")
                .questionText("Within the past 12 months, the food you bought just didn't last and you didn't have money to get more.")
                .category(SdohCategory.FOOD_INSECURITY)
                .loincCode("88123-5")
                .questionType(SdohScreeningQuestion.QuestionType.SINGLE_CHOICE)
                .answerOptions(new String[]{"Often true", "Sometimes true", "Never true"})
                .required(true)
                .sequenceNumber(2)
                .build());

        // Housing Instability
        questions.add(SdohScreeningQuestion.builder()
                .questionId("ahc-housing-1")
                .questionText("What is your housing situation today?")
                .category(SdohCategory.HOUSING_INSTABILITY)
                .loincCode("71802-3")
                .questionType(SdohScreeningQuestion.QuestionType.SINGLE_CHOICE)
                .answerOptions(new String[]{"I have housing", "I do not have housing", "I am worried about losing my housing"})
                .required(true)
                .sequenceNumber(3)
                .build());

        // Transportation
        questions.add(SdohScreeningQuestion.builder()
                .questionId("ahc-transport-1")
                .questionText("In the past 12 months, has lack of reliable transportation kept you from medical appointments, meetings, work, or from getting things needed for daily living?")
                .category(SdohCategory.TRANSPORTATION)
                .loincCode("93030-5")
                .questionType(SdohScreeningQuestion.QuestionType.YES_NO)
                .answerOptions(new String[]{"Yes", "No"})
                .required(true)
                .sequenceNumber(4)
                .build());

        // Utilities
        questions.add(SdohScreeningQuestion.builder()
                .questionId("ahc-utilities-1")
                .questionText("In the past 12 months has the electric, gas, oil, or water company threatened to shut off services in your home?")
                .category(SdohCategory.UTILITIES)
                .loincCode("93031-3")
                .questionType(SdohScreeningQuestion.QuestionType.YES_NO)
                .answerOptions(new String[]{"Yes", "No", "Already shut off"})
                .required(true)
                .sequenceNumber(5)
                .build());

        // Safety/Interpersonal Violence
        questions.add(SdohScreeningQuestion.builder()
                .questionId("ahc-safety-1")
                .questionText("How often does anyone, including family, physically hurt you?")
                .category(SdohCategory.INTERPERSONAL_VIOLENCE)
                .loincCode("93038-8")
                .questionType(SdohScreeningQuestion.QuestionType.SINGLE_CHOICE)
                .answerOptions(new String[]{"Never", "Rarely", "Sometimes", "Fairly often", "Frequently"})
                .required(true)
                .sequenceNumber(6)
                .build());

        questions.add(SdohScreeningQuestion.builder()
                .questionId("ahc-safety-2")
                .questionText("How often does anyone, including family, insult or talk down to you?")
                .category(SdohCategory.INTERPERSONAL_VIOLENCE)
                .loincCode("93039-6")
                .questionType(SdohScreeningQuestion.QuestionType.SINGLE_CHOICE)
                .answerOptions(new String[]{"Never", "Rarely", "Sometimes", "Fairly often", "Frequently"})
                .required(true)
                .sequenceNumber(7)
                .build());

        questions.add(SdohScreeningQuestion.builder()
                .questionId("ahc-safety-3")
                .questionText("How often does anyone, including family, threaten you with harm?")
                .category(SdohCategory.INTERPERSONAL_VIOLENCE)
                .loincCode("93040-4")
                .questionType(SdohScreeningQuestion.QuestionType.SINGLE_CHOICE)
                .answerOptions(new String[]{"Never", "Rarely", "Sometimes", "Fairly often", "Frequently"})
                .required(true)
                .sequenceNumber(8)
                .build());

        questions.add(SdohScreeningQuestion.builder()
                .questionId("ahc-safety-4")
                .questionText("How often does anyone, including family, scream or curse at you?")
                .category(SdohCategory.INTERPERSONAL_VIOLENCE)
                .loincCode("93041-2")
                .questionType(SdohScreeningQuestion.QuestionType.SINGLE_CHOICE)
                .answerOptions(new String[]{"Never", "Rarely", "Sometimes", "Fairly often", "Frequently"})
                .required(true)
                .sequenceNumber(9)
                .build());

        // Financial Strain
        questions.add(SdohScreeningQuestion.builder()
                .questionId("ahc-financial-1")
                .questionText("Do you have any problems paying your bills?")
                .category(SdohCategory.FINANCIAL_STRAIN)
                .loincCode("96777-8")
                .questionType(SdohScreeningQuestion.QuestionType.YES_NO)
                .answerOptions(new String[]{"Yes", "No"})
                .required(false)
                .sequenceNumber(10)
                .build());

        return questions;
    }

    /**
     * Create PRAPARE (Protocol for Responding to and Assessing Patients' Assets, Risks, and Experiences) questionnaire
     */
    public List<SdohScreeningQuestion> createPrapareQuestionnaire() {
        List<SdohScreeningQuestion> questions = new ArrayList<>();

        // Housing
        questions.add(SdohScreeningQuestion.builder()
                .questionId("prapare-housing-1")
                .questionText("What is your living situation today?")
                .category(SdohCategory.HOUSING_INSTABILITY)
                .loincCode("71802-3")
                .questionType(SdohScreeningQuestion.QuestionType.SINGLE_CHOICE)
                .answerOptions(new String[]{"I have housing", "I do not have housing", "I choose not to answer"})
                .required(true)
                .sequenceNumber(1)
                .build());

        // Employment
        questions.add(SdohScreeningQuestion.builder()
                .questionId("prapare-employment-1")
                .questionText("What is your current work situation?")
                .category(SdohCategory.EMPLOYMENT)
                .loincCode("67875-5")
                .questionType(SdohScreeningQuestion.QuestionType.SINGLE_CHOICE)
                .answerOptions(new String[]{"Employed", "Unemployed", "Retired", "Disabled", "Student"})
                .required(true)
                .sequenceNumber(2)
                .build());

        // Education
        questions.add(SdohScreeningQuestion.builder()
                .questionId("prapare-education-1")
                .questionText("What is the highest level of school that you have finished?")
                .category(SdohCategory.EDUCATION)
                .loincCode("82589-3")
                .questionType(SdohScreeningQuestion.QuestionType.SINGLE_CHOICE)
                .answerOptions(new String[]{"Less than high school", "High school diploma or GED", "Some college", "College degree or higher"})
                .required(true)
                .sequenceNumber(3)
                .build());

        // Financial Resource Strain
        questions.add(SdohScreeningQuestion.builder()
                .questionId("prapare-financial-1")
                .questionText("In the past year, have you or any family members you live with been unable to get any of the following when it was really needed?")
                .category(SdohCategory.FINANCIAL_STRAIN)
                .loincCode("93033-9")
                .questionType(SdohScreeningQuestion.QuestionType.MULTIPLE_CHOICE)
                .answerOptions(new String[]{"Food", "Clothing", "Utilities", "Child care", "Medicine or health care", "Phone", "Other"})
                .required(true)
                .sequenceNumber(4)
                .build());

        // Food Insecurity
        questions.add(SdohScreeningQuestion.builder()
                .questionId("prapare-food-1")
                .questionText("Has lack of transportation kept you from medical appointments or from getting your medications?")
                .category(SdohCategory.TRANSPORTATION)
                .loincCode("93030-5")
                .questionType(SdohScreeningQuestion.QuestionType.YES_NO)
                .answerOptions(new String[]{"Yes", "No"})
                .required(true)
                .sequenceNumber(5)
                .build());

        // Add more PRAPARE questions
        for (int i = 6; i <= 20; i++) {
            questions.add(SdohScreeningQuestion.builder()
                    .questionId("prapare-q" + i)
                    .questionText("Additional PRAPARE question " + i)
                    .category(SdohCategory.values()[i % SdohCategory.values().length])
                    .loincCode("93000-" + i)
                    .questionType(SdohScreeningQuestion.QuestionType.YES_NO)
                    .answerOptions(new String[]{"Yes", "No"})
                    .required(false)
                    .sequenceNumber(i)
                    .build());
        }

        return questions;
    }

    public List<SdohScreeningQuestion> getQuestionsByCategory(SdohCategory category) {
        return createAhcHrsnQuestionnaire().stream()
                .filter(q -> q.getCategory() == category)
                .collect(Collectors.toList());
    }

    @Transactional
    public SdohAssessment submitScreening(String tenantId, String patientId, String screeningTool,
                                          List<SdohScreeningResponse> responses) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        if (patientId == null) {
            throw new IllegalArgumentException("Patient ID cannot be null");
        }
        if (responses == null || responses.isEmpty()) {
            throw new IllegalArgumentException("Responses cannot be empty");
        }

        Map<SdohCategory, Boolean> needs = identifyNeeds(responses);
        List<String> zCodes = zCodeMapper.mapNeedsToZCodes(needs);

        SdohAssessment assessment = SdohAssessment.builder()
                .patientId(patientId)
                .tenantId(tenantId)
                .assessmentDate(LocalDateTime.now())
                .screeningTool(screeningTool)
                .responses(responses)
                .identifiedNeeds(needs)
                .identifiedZCodes(zCodes)
                .status(SdohAssessment.AssessmentStatus.COMPLETED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        SdohRiskScore riskScore = riskCalculator.calculateRiskScore(assessment);
        assessment.setRiskScore(riskScore.getTotalScore());

        SdohAssessmentEntity entity = convertToEntity(assessment);
        entity = assessmentRepository.save(entity);

        return convertToModel(entity);
    }

    public boolean validateResponses(List<SdohScreeningQuestion> questions, List<SdohScreeningResponse> responses) {
        Set<String> requiredQuestionIds = questions.stream()
                .filter(SdohScreeningQuestion::isRequired)
                .map(SdohScreeningQuestion::getQuestionId)
                .collect(Collectors.toSet());

        Set<String> answeredQuestionIds = responses.stream()
                .map(SdohScreeningResponse::getQuestionId)
                .collect(Collectors.toSet());

        return answeredQuestionIds.containsAll(requiredQuestionIds);
    }

    public Map<SdohCategory, Boolean> identifyNeeds(List<SdohScreeningResponse> responses) {
        Map<SdohCategory, Boolean> needs = new HashMap<>();

        // Simple logic: if answer indicates need, mark category as true
        for (SdohScreeningResponse response : responses) {
            if (response.getAnswer() != null) {
                String answer = response.getAnswer().toLowerCase();
                if (answer.contains("yes") || answer.contains("often") || answer.contains("sometimes")) {
                    // Determine category from question ID
                    if (response.getQuestionId().contains("food")) {
                        needs.put(SdohCategory.FOOD_INSECURITY, true);
                    } else if (response.getQuestionId().contains("housing")) {
                        needs.put(SdohCategory.HOUSING_INSTABILITY, true);
                    } else if (response.getQuestionId().contains("transport")) {
                        needs.put(SdohCategory.TRANSPORTATION, true);
                    }
                }
            }
        }

        return needs;
    }

    public Optional<SdohAssessment> getAssessmentById(String assessmentId) {
        return assessmentRepository.findById(assessmentId)
                .map(this::convertToModel);
    }

    public List<SdohAssessment> getAssessmentsByPatient(String tenantId, String patientId) {
        return assessmentRepository.findByTenantIdAndPatientId(tenantId, patientId).stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
    }

    public Optional<SdohAssessment> getMostRecentAssessment(String tenantId, String patientId) {
        return assessmentRepository.findMostRecentByTenantIdAndPatientId(tenantId, patientId)
                .map(this::convertToModel);
    }

    @Transactional
    public void updateAssessmentStatus(String assessmentId, SdohAssessment.AssessmentStatus status) {
        assessmentRepository.findById(assessmentId).ifPresent(entity -> {
            entity.setStatus(status);
            assessmentRepository.save(entity);
        });
    }

    public String mapToFhirQuestionnaireResponse(SdohAssessment assessment) {
        return String.format("{\"resourceType\": \"QuestionnaireResponse\", \"id\": \"%s\", \"status\": \"completed\"}",
                assessment.getAssessmentId());
    }

    public double calculateCompletionPercentage(List<SdohScreeningQuestion> questions,
                                                List<SdohScreeningResponse> responses) {
        if (questions.isEmpty()) return 0.0;
        return (responses.size() * 100.0) / questions.size();
    }

    public String getScreeningTool(SdohAssessment assessment) {
        return assessment.getScreeningTool();
    }

    public List<String> generateZCodesFromAssessment(Map<SdohCategory, Boolean> needs) {
        return zCodeMapper.mapNeedsToZCodes(needs);
    }

    public SdohRiskScore calculateRiskScore(SdohAssessment assessment) {
        return riskCalculator.calculateRiskScore(assessment);
    }

    @Transactional
    public int archiveOldAssessments(LocalDateTime cutoffDate) {
        List<SdohAssessmentEntity> oldAssessments = assessmentRepository.findOldAssessments(cutoffDate);
        oldAssessments.forEach(entity -> entity.setStatus(SdohAssessment.AssessmentStatus.ARCHIVED));
        assessmentRepository.saveAll(oldAssessments);
        return oldAssessments.size();
    }

    public String exportAssessmentAsJson(SdohAssessment assessment) {
        try {
            return objectMapper.writeValueAsString(assessment);
        } catch (JsonProcessingException e) {
            log.error("Error exporting assessment to JSON", e);
            return "{}";
        }
    }

    public Map<String, Object> getScreeningStatistics(String tenantId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAssessments", assessmentRepository.countByTenantId(tenantId));
        stats.put("completedAssessments",
                assessmentRepository.countByTenantIdAndStatus(tenantId, SdohAssessment.AssessmentStatus.COMPLETED));
        return stats;
    }

    private SdohAssessmentEntity convertToEntity(SdohAssessment assessment) {
        try {
            return SdohAssessmentEntity.builder()
                    .assessmentId(assessment.getAssessmentId())
                    .patientId(assessment.getPatientId())
                    .tenantId(assessment.getTenantId())
                    .assessmentDate(assessment.getAssessmentDate())
                    .screeningTool(assessment.getScreeningTool())
                    .responsesJson(objectMapper.writeValueAsString(assessment.getResponses()))
                    .identifiedNeedsJson(objectMapper.writeValueAsString(assessment.getIdentifiedNeeds()))
                    .identifiedZCodesJson(objectMapper.writeValueAsString(assessment.getIdentifiedZCodes()))
                    .riskScore(assessment.getRiskScore())
                    .assessedBy(assessment.getAssessedBy())
                    .status(assessment.getStatus())
                    .createdAt(assessment.getCreatedAt())
                    .updatedAt(assessment.getUpdatedAt())
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting assessment to entity", e);
        }
    }

    @SuppressWarnings("unchecked")
    private SdohAssessment convertToModel(SdohAssessmentEntity entity) {
        try {
            return SdohAssessment.builder()
                    .assessmentId(entity.getAssessmentId())
                    .patientId(entity.getPatientId())
                    .tenantId(entity.getTenantId())
                    .assessmentDate(entity.getAssessmentDate())
                    .screeningTool(entity.getScreeningTool())
                    .responses(entity.getResponsesJson() != null ?
                            objectMapper.readValue(entity.getResponsesJson(), List.class) : new ArrayList<>())
                    .identifiedNeeds(entity.getIdentifiedNeedsJson() != null ?
                            objectMapper.readValue(entity.getIdentifiedNeedsJson(), Map.class) : new HashMap<>())
                    .identifiedZCodes(entity.getIdentifiedZCodesJson() != null ?
                            objectMapper.readValue(entity.getIdentifiedZCodesJson(), List.class) : new ArrayList<>())
                    .riskScore(entity.getRiskScore())
                    .assessedBy(entity.getAssessedBy())
                    .status(entity.getStatus())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting entity to assessment", e);
        }
    }
}
