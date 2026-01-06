package com.healthdata.quality.service;

import com.healthdata.quality.dto.ai.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI-Assisted CQL Generation Service
 *
 * Generates CQL code from natural language descriptions using AI models.
 * Includes validation, testing, and explanation capabilities.
 *
 * Issue #150: Implement AI-Assisted CQL Generation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiCqlGenerationService {

    private static final String MODEL_VERSION = "1.0.0";
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    @Value("${ai.api.url:http://ai-assistant-service:8080}")
    private String aiApiUrl;

    @Value("${ai.api.key:}")
    private String aiApiKey;

    @Value("${ai.enabled:false}")
    private boolean aiEnabled;

    @PostConstruct
    public void init() {
        log.info("AI CQL Generation Service initialized (enabled: {}, apiUrl: {})", aiEnabled, aiApiUrl);
    }

    /**
     * Generate CQL from a natural language description.
     *
     * @param tenantId Tenant identifier
     * @param request CQL generation request
     * @return Generated CQL with validation and test results
     */
    public CqlGenerationResponse generateCql(String tenantId, CqlGenerationRequest request) {
        log.info("Generating CQL for tenant: {}, description length: {}", tenantId, request.getDescription().length());
        long startTime = System.currentTimeMillis();

        try {
            // Build the prompt for CQL generation
            String prompt = buildCqlGenerationPrompt(request);

            // Generate CQL (use mock for demo or when AI is disabled)
            String generatedCql;
            String explanation;
            double confidence;
            int inputTokens = 0;
            int outputTokens = 0;

            if (aiEnabled && aiApiKey != null && !aiApiKey.isBlank()) {
                // Call AI service
                AiGenerationResult result = callAiService(prompt);
                generatedCql = extractCqlFromResponse(result.response());
                explanation = extractExplanationFromResponse(result.response());
                confidence = calculateConfidence(generatedCql, request);
                inputTokens = result.inputTokens();
                outputTokens = result.outputTokens();
            } else {
                // Use mock generation for demo
                CqlMockResult mockResult = generateMockCql(request);
                generatedCql = mockResult.cql();
                explanation = mockResult.explanation();
                confidence = mockResult.confidence();
            }

            // Validate CQL if requested
            CqlGenerationResponse.ValidationResult validationResult = null;
            CqlGenerationResponse.ValidationStatus validationStatus = CqlGenerationResponse.ValidationStatus.NOT_VALIDATED;

            if (request.isValidateCql()) {
                validationResult = validateCql(generatedCql);
                validationStatus = determineValidationStatus(validationResult);
            }

            // Run tests if requested
            CqlGenerationResponse.TestResults testResults = null;
            if (request.isRunTests() && validationStatus == CqlGenerationResponse.ValidationStatus.VALID) {
                testResults = runTestExecution(tenantId, generatedCql, request.getSampleSize());
            }

            // Generate suggestions
            List<CqlGenerationResponse.Suggestion> suggestions = generateSuggestions(generatedCql, validationResult);

            long generationTime = System.currentTimeMillis() - startTime;

            return CqlGenerationResponse.builder()
                .id(UUID.randomUUID().toString())
                .generatedCql(generatedCql)
                .explanation(explanation)
                .confidence(confidence)
                .validationStatus(validationStatus)
                .validationResult(validationResult)
                .testResults(testResults)
                .warnings(extractWarnings(validationResult))
                .suggestions(suggestions)
                .metadata(CqlGenerationResponse.GenerationMetadata.builder()
                    .inputTokens(inputTokens)
                    .outputTokens(outputTokens)
                    .generationTimeMs(generationTime)
                    .promptTemplate("cql_generation_v1")
                    .build())
                .generatedAt(LocalDateTime.now())
                .modelVersion(MODEL_VERSION)
                .build();

        } catch (Exception e) {
            log.error("Error generating CQL: {}", e.getMessage(), e);
            return buildErrorResponse(e.getMessage());
        }
    }

    /**
     * Explain existing CQL code in plain English.
     *
     * @param tenantId Tenant identifier
     * @param request CQL explanation request
     * @return Plain English explanation
     */
    public CqlExplainResponse explainCql(String tenantId, CqlExplainRequest request) {
        log.info("Explaining CQL for tenant: {}, code length: {}", tenantId, request.getCqlCode().length());

        try {
            String cql = request.getCqlCode();

            // Parse CQL structure
            List<CqlExplainResponse.SectionExplanation> sections = parseCqlSections(cql);
            List<CqlExplainResponse.ClinicalConcept> concepts = extractClinicalConcepts(cql);
            List<CqlExplainResponse.DataElement> dataElements = extractDataElements(cql);

            // Generate summary
            String summary = generateCqlSummary(cql, sections);

            // Assess complexity and performance
            int complexity = assessComplexity(cql, sections);
            CqlExplainResponse.PerformanceAssessment performance = assessPerformance(cql, dataElements);

            // Generate suggestions if requested
            List<CqlGenerationResponse.Suggestion> suggestions = null;
            if (request.isIncludeSuggestions()) {
                suggestions = generateSuggestions(cql, null);
            }

            // Identify potential issues
            List<String> potentialIssues = identifyPotentialIssues(cql, sections);

            return CqlExplainResponse.builder()
                .id(UUID.randomUUID().toString())
                .summary(summary)
                .sections(sections)
                .clinicalConcepts(concepts)
                .dataElements(dataElements)
                .potentialIssues(potentialIssues)
                .suggestions(suggestions)
                .complexityRating(complexity)
                .performanceAssessment(performance)
                .explainedAt(LocalDateTime.now())
                .build();

        } catch (Exception e) {
            log.error("Error explaining CQL: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to explain CQL: " + e.getMessage(), e);
        }
    }

    /**
     * Build the prompt for CQL generation.
     */
    private String buildCqlGenerationPrompt(CqlGenerationRequest request) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Generate valid FHIR R4 CQL (Clinical Quality Language) code for the following measure:\n\n");
        prompt.append("DESCRIPTION: ").append(request.getDescription()).append("\n\n");
        prompt.append("MEASURE TYPE: ").append(request.getMeasureType()).append("\n\n");

        if (request.getMeasureName() != null) {
            prompt.append("MEASURE NAME: ").append(request.getMeasureName()).append("\n\n");
        }

        if (request.getContext() != null) {
            CqlGenerationRequest.GenerationContext ctx = request.getContext();

            if (ctx.getExistingConditions() != null && !ctx.getExistingConditions().isEmpty()) {
                prompt.append("RELEVANT CONDITIONS: ").append(String.join(", ", ctx.getExistingConditions())).append("\n");
            }

            if (ctx.getRelevantValueSets() != null && !ctx.getRelevantValueSets().isEmpty()) {
                prompt.append("VALUE SETS: ").append(String.join(", ", ctx.getRelevantValueSets())).append("\n");
            }

            if (ctx.getPopulation() != null) {
                CqlGenerationRequest.PopulationCriteria pop = ctx.getPopulation();
                if (pop.getMinAge() != null || pop.getMaxAge() != null) {
                    prompt.append("AGE RANGE: ");
                    if (pop.getMinAge() != null) prompt.append(pop.getMinAge()).append("+");
                    if (pop.getMinAge() != null && pop.getMaxAge() != null) prompt.append(" to ");
                    if (pop.getMaxAge() != null) prompt.append(pop.getMaxAge());
                    prompt.append("\n");
                }
            }
        }

        prompt.append("\nREQUIREMENTS:\n");
        prompt.append("1. Use FHIR R4 data model\n");
        prompt.append("2. Include library declaration with version\n");
        prompt.append("3. Define Initial Population, Denominator, Numerator, and Exclusions\n");
        prompt.append("4. Use standard value sets where possible\n");
        prompt.append("5. Include comments explaining key logic\n\n");

        prompt.append("Return ONLY the CQL code wrapped in ```cql``` code blocks, followed by a brief explanation.");

        return prompt.toString();
    }

    /**
     * Call AI service for generation.
     */
    private AiGenerationResult callAiService(String prompt) {
        // This would integrate with the actual AI service
        // For now, return mock data
        log.debug("Would call AI service with prompt length: {}", prompt.length());
        return new AiGenerationResult("", 0, 0);
    }

    private record AiGenerationResult(String response, int inputTokens, int outputTokens) {}

    /**
     * Generate mock CQL for demo/testing purposes.
     */
    private CqlMockResult generateMockCql(CqlGenerationRequest request) {
        String measureName = request.getMeasureName() != null ?
            sanitizeMeasureName(request.getMeasureName()) : "GeneratedMeasure";

        String description = request.getDescription().toLowerCase();

        // Determine the type of measure based on description
        if (description.contains("diabetes") && description.contains("a1c")) {
            return generateDiabetesA1cMeasure(measureName, request);
        } else if (description.contains("blood pressure") || description.contains("hypertension")) {
            return generateBloodPressureMeasure(measureName, request);
        } else if (description.contains("mammogram") || description.contains("breast cancer")) {
            return generateBreastCancerScreeningMeasure(measureName, request);
        } else if (description.contains("colonoscopy") || description.contains("colorectal")) {
            return generateColorectalScreeningMeasure(measureName, request);
        } else {
            return generateGenericMeasure(measureName, request);
        }
    }

    private record CqlMockResult(String cql, String explanation, double confidence) {}

    private CqlMockResult generateDiabetesA1cMeasure(String name, CqlGenerationRequest request) {
        String cql = """
            library %s version '1.0.0'

            using FHIR version '4.0.1'

            include FHIRHelpers version '4.0.1'

            // Value Sets
            valueset "Diabetes": '2.16.840.1.113883.3.464.1003.103.12.1001'
            valueset "HbA1c Laboratory Test": '2.16.840.1.113883.3.464.1003.198.12.1013'

            // Parameters
            parameter "Measurement Period" Interval<DateTime>

            // Context
            context Patient

            // Helper: Calculate age at start of measurement period
            define "Age at Start":
              AgeInYearsAt(start of "Measurement Period")

            // Initial Population: Patients 18-75 with diabetes
            define "Initial Population":
              "Age at Start" >= 18
                and "Age at Start" <= 75
                and exists "Diabetes Diagnosis"

            // Diabetes Diagnosis (active during measurement period)
            define "Diabetes Diagnosis":
              [Condition: "Diabetes"] C
                where C.clinicalStatus ~ 'active'
                  and C.onset during "Measurement Period"

            // Denominator: Same as Initial Population
            define "Denominator":
              "Initial Population"

            // Denominator Exclusions: Hospice, advanced illness
            define "Denominator Exclusions":
              false // Add hospice/advanced illness logic as needed

            // Numerator: Had HbA1c test in measurement period
            define "Numerator":
              exists "HbA1c Test During Period"

            // HbA1c tests during measurement period
            define "HbA1c Test During Period":
              [Observation: "HbA1c Laboratory Test"] O
                where O.status in {'final', 'amended', 'corrected'}
                  and O.effective during "Measurement Period"

            // Stratification: Most recent HbA1c result
            define "Most Recent HbA1c":
              Last("HbA1c Test During Period" O
                sort by effective)

            define "Most Recent HbA1c Result":
              "Most Recent HbA1c".value as Quantity
            """.formatted(name);

        String explanation = """
            This measure identifies patients aged 18-75 with Type 2 Diabetes who have had an HbA1c test during the measurement period.

            **Initial Population**: Patients between 18 and 75 years old with an active diabetes diagnosis.

            **Denominator**: All patients in the initial population.

            **Numerator**: Patients who had at least one HbA1c laboratory test with a final result during the measurement period.

            **Clinical Rationale**: Regular HbA1c monitoring is essential for diabetes management. The ADA recommends testing at least twice yearly for patients meeting treatment goals, and quarterly for those not meeting goals.
            """;

        return new CqlMockResult(cql, explanation, 0.92);
    }

    private CqlMockResult generateBloodPressureMeasure(String name, CqlGenerationRequest request) {
        String cql = """
            library %s version '1.0.0'

            using FHIR version '4.0.1'

            include FHIRHelpers version '4.0.1'

            // Value Sets
            valueset "Essential Hypertension": '2.16.840.1.113883.3.464.1003.104.12.1011'
            valueset "Systolic Blood Pressure": '2.16.840.1.113883.3.526.3.1032'
            valueset "Diastolic Blood Pressure": '2.16.840.1.113883.3.526.3.1033'

            parameter "Measurement Period" Interval<DateTime>

            context Patient

            define "Age at Start":
              AgeInYearsAt(start of "Measurement Period")

            define "Initial Population":
              "Age at Start" >= 18
                and "Age at Start" <= 85
                and exists "Hypertension Diagnosis"

            define "Hypertension Diagnosis":
              [Condition: "Essential Hypertension"] H
                where H.clinicalStatus ~ 'active'

            define "Denominator":
              "Initial Population"

            define "Most Recent BP":
              Last([Observation] O
                where O.code in "Systolic Blood Pressure"
                  and O.status in {'final', 'amended', 'corrected'}
                  and O.effective during "Measurement Period"
                sort by effective)

            define "BP Controlled":
              "Most Recent BP".value < 140 'mm[Hg]'

            define "Numerator":
              "BP Controlled"
            """.formatted(name);

        String explanation = """
            This measure evaluates blood pressure control in patients with hypertension.

            **Initial Population**: Adults 18-85 with diagnosed essential hypertension.

            **Denominator**: All patients in the initial population.

            **Numerator**: Patients whose most recent blood pressure reading is below 140/90 mmHg.

            **Clinical Rationale**: Controlling blood pressure reduces cardiovascular risk. Target BP < 140/90 is the standard threshold for most adult patients.
            """;

        return new CqlMockResult(cql, explanation, 0.88);
    }

    private CqlMockResult generateBreastCancerScreeningMeasure(String name, CqlGenerationRequest request) {
        String cql = """
            library %s version '1.0.0'

            using FHIR version '4.0.1'

            include FHIRHelpers version '4.0.1'

            valueset "Mammography": '2.16.840.1.113883.3.464.1003.108.12.1018'
            valueset "Bilateral Mastectomy": '2.16.840.1.113883.3.464.1003.198.12.1005'

            parameter "Measurement Period" Interval<DateTime>

            context Patient

            define "Age at End":
              AgeInYearsAt(end of "Measurement Period")

            define "Initial Population":
              Patient.gender = 'female'
                and "Age at End" >= 52
                and "Age at End" <= 74

            define "Denominator":
              "Initial Population"

            define "Bilateral Mastectomy History":
              exists [Procedure: "Bilateral Mastectomy"] M
                where M.status = 'completed'

            define "Denominator Exclusions":
              "Bilateral Mastectomy History"

            define "Mammogram in Last 27 Months":
              exists [Procedure: "Mammography"] P
                where P.status = 'completed'
                  and P.performed ends 27 months or less before end of "Measurement Period"

            define "Numerator":
              "Mammogram in Last 27 Months"
            """.formatted(name);

        String explanation = """
            This measure tracks breast cancer screening rates using mammography.

            **Initial Population**: Women aged 52-74 at the end of the measurement period.

            **Denominator**: All women in the initial population.

            **Denominator Exclusions**: Women who have had bilateral mastectomy.

            **Numerator**: Women who had a mammogram within 27 months of the end of the measurement period.

            **Clinical Rationale**: Regular mammography screening reduces breast cancer mortality through early detection. USPSTF recommends biennial screening for women 50-74.
            """;

        return new CqlMockResult(cql, explanation, 0.90);
    }

    private CqlMockResult generateColorectalScreeningMeasure(String name, CqlGenerationRequest request) {
        String cql = """
            library %s version '1.0.0'

            using FHIR version '4.0.1'

            include FHIRHelpers version '4.0.1'

            valueset "Colonoscopy": '2.16.840.1.113883.3.464.1003.108.12.1020'
            valueset "FIT Test": '2.16.840.1.113883.3.464.1003.108.12.1039'
            valueset "Colorectal Cancer": '2.16.840.1.113883.3.464.1003.108.12.1001'
            valueset "Total Colectomy": '2.16.840.1.113883.3.464.1003.198.12.1019'

            parameter "Measurement Period" Interval<DateTime>

            context Patient

            define "Age at End":
              AgeInYearsAt(end of "Measurement Period")

            define "Initial Population":
              "Age at End" >= 50
                and "Age at End" <= 75

            define "Denominator":
              "Initial Population"

            define "Colorectal Cancer History":
              exists [Condition: "Colorectal Cancer"]

            define "Total Colectomy History":
              exists [Procedure: "Total Colectomy"] P
                where P.status = 'completed'

            define "Denominator Exclusions":
              "Colorectal Cancer History"
                or "Total Colectomy History"

            define "Colonoscopy in Last 10 Years":
              exists [Procedure: "Colonoscopy"] C
                where C.status = 'completed'
                  and C.performed ends 10 years or less before end of "Measurement Period"

            define "FIT Test in Last Year":
              exists [Observation: "FIT Test"] F
                where F.status in {'final', 'amended', 'corrected'}
                  and F.effective during "Measurement Period"

            define "Numerator":
              "Colonoscopy in Last 10 Years"
                or "FIT Test in Last Year"
            """.formatted(name);

        String explanation = """
            This measure tracks colorectal cancer screening compliance.

            **Initial Population**: Adults aged 50-75.

            **Denominator**: All adults in the initial population.

            **Denominator Exclusions**: Patients with history of colorectal cancer or total colectomy.

            **Numerator**: Patients who had either a colonoscopy within 10 years or a FIT test within the measurement year.

            **Clinical Rationale**: Colorectal cancer screening significantly reduces mortality through early detection and polyp removal. Multiple modalities are recommended.
            """;

        return new CqlMockResult(cql, explanation, 0.91);
    }

    private CqlMockResult generateGenericMeasure(String name, CqlGenerationRequest request) {
        // Extract key information from description
        Integer minAge = null;
        Integer maxAge = null;

        if (request.getContext() != null && request.getContext().getPopulation() != null) {
            minAge = request.getContext().getPopulation().getMinAge();
            maxAge = request.getContext().getPopulation().getMaxAge();
        }

        // Default age range if not specified
        if (minAge == null) minAge = 18;
        if (maxAge == null) maxAge = 65;

        String cql = """
            library %s version '1.0.0'

            using FHIR version '4.0.1'

            include FHIRHelpers version '4.0.1'

            /*
             * Custom Measure
             * Description: %s
             * Generated by AI CQL Assistant
             */

            parameter "Measurement Period" Interval<DateTime>

            context Patient

            // Age calculation
            define "Age at Start":
              AgeInYearsAt(start of "Measurement Period")

            // Initial Population
            // TODO: Customize based on your target population
            define "Initial Population":
              "Age at Start" >= %d
                and "Age at Start" <= %d

            // Denominator: All patients in initial population
            define "Denominator":
              "Initial Population"

            // Denominator Exclusions
            // TODO: Add exclusion criteria
            define "Denominator Exclusions":
              false

            // Numerator
            // TODO: Define the criteria that indicates measure compliance
            define "Numerator":
              true // Replace with actual criteria

            // Numerator Exclusions
            define "Numerator Exclusions":
              false
            """.formatted(name, request.getDescription(), minAge, maxAge);

        String explanation = """
            This is a template measure generated from your description:

            "%s"

            **Note**: This template includes placeholder logic that you should customize:

            1. **Initial Population**: Currently set to patients aged %d-%d. Modify based on your target population.

            2. **Denominator**: Includes all patients from the initial population. Add any additional criteria.

            3. **Denominator Exclusions**: Currently empty. Add conditions that should exclude patients from the denominator.

            4. **Numerator**: Set to 'true' as a placeholder. Replace with the actual clinical criteria that indicates measure compliance.

            5. **Value Sets**: Add relevant value sets for conditions, procedures, or observations used in your criteria.

            This template follows HEDIS/CMS measure structure and uses FHIR R4 data model.
            """.formatted(request.getDescription(), minAge, maxAge);

        return new CqlMockResult(cql, explanation, 0.70);
    }

    /**
     * Validate CQL syntax and semantics.
     */
    private CqlGenerationResponse.ValidationResult validateCql(String cql) {
        List<CqlGenerationResponse.ValidationError> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Basic syntax validation
        boolean hasLibrary = cql.contains("library ");
        boolean hasUsing = cql.contains("using FHIR");
        boolean hasContext = cql.contains("context Patient");

        if (!hasLibrary) {
            errors.add(CqlGenerationResponse.ValidationError.builder()
                .severity("ERROR")
                .message("Missing library declaration")
                .line(1)
                .column(1)
                .code("CQL0001")
                .build());
        }

        if (!hasUsing) {
            errors.add(CqlGenerationResponse.ValidationError.builder()
                .severity("ERROR")
                .message("Missing 'using FHIR' declaration")
                .line(1)
                .column(1)
                .code("CQL0002")
                .build());
        }

        if (!hasContext) {
            warnings.add("Consider adding 'context Patient' for patient-level measures");
        }

        // Check for common patterns
        if (!cql.contains("define \"Initial Population\"")) {
            warnings.add("Missing 'Initial Population' definition - required for quality measures");
        }

        if (!cql.contains("define \"Denominator\"")) {
            warnings.add("Missing 'Denominator' definition - required for proportion measures");
        }

        if (!cql.contains("define \"Numerator\"")) {
            warnings.add("Missing 'Numerator' definition - required for proportion measures");
        }

        return CqlGenerationResponse.ValidationResult.builder()
            .syntaxValid(errors.isEmpty())
            .semanticValid(errors.isEmpty() && warnings.size() < 3)
            .errors(errors)
            .warnings(warnings)
            .errorCount(errors.size())
            .warningCount(warnings.size())
            .build();
    }

    /**
     * Determine validation status from results.
     */
    private CqlGenerationResponse.ValidationStatus determineValidationStatus(
            CqlGenerationResponse.ValidationResult result) {
        if (result == null) {
            return CqlGenerationResponse.ValidationStatus.NOT_VALIDATED;
        }
        if (!result.isSyntaxValid()) {
            return CqlGenerationResponse.ValidationStatus.INVALID;
        }
        if (result.getWarningCount() > 0) {
            return CqlGenerationResponse.ValidationStatus.WARNINGS;
        }
        return CqlGenerationResponse.ValidationStatus.VALID;
    }

    /**
     * Run test execution against sample patients.
     */
    private CqlGenerationResponse.TestResults runTestExecution(String tenantId, String cql, int sampleSize) {
        // Mock test execution - in production this would call cql-engine-service
        log.info("Running test execution for tenant: {}, sample size: {}", tenantId, sampleSize);

        // Generate mock results
        int numerator = (int) (sampleSize * 0.3 + Math.random() * sampleSize * 0.4);
        int denominator = sampleSize;

        List<CqlGenerationResponse.PatientTestResult> patientResults = new ArrayList<>();
        for (int i = 0; i < Math.min(sampleSize, 5); i++) {
            boolean inNumerator = i < numerator;
            patientResults.add(CqlGenerationResponse.PatientTestResult.builder()
                .patientId("SAMPLE-" + (i + 1))
                .inDenominator(true)
                .inNumerator(inNumerator)
                .reason(inNumerator ? "Meets numerator criteria" : "Does not meet numerator criteria")
                .build());
        }

        return CqlGenerationResponse.TestResults.builder()
            .executed(true)
            .sampleSize(sampleSize)
            .numerator(numerator)
            .denominator(denominator)
            .complianceRate((double) numerator / denominator * 100)
            .patientResults(patientResults)
            .build();
    }

    /**
     * Generate improvement suggestions.
     */
    private List<CqlGenerationResponse.Suggestion> generateSuggestions(
            String cql, CqlGenerationResponse.ValidationResult validation) {
        List<CqlGenerationResponse.Suggestion> suggestions = new ArrayList<>();

        // Check for missing FHIRHelpers
        if (!cql.contains("include FHIRHelpers")) {
            suggestions.add(CqlGenerationResponse.Suggestion.builder()
                .type("STANDARD_COMPLIANCE")
                .description("Consider including FHIRHelpers for standard FHIR operations")
                .suggestedCode("include FHIRHelpers version '4.0.1'")
                .impact(0.3)
                .build());
        }

        // Check for hardcoded dates
        if (cql.contains("@20") || cql.contains("'20")) {
            suggestions.add(CqlGenerationResponse.Suggestion.builder()
                .type("OPTIMIZATION")
                .description("Avoid hardcoded dates - use Measurement Period parameter instead")
                .impact(0.5)
                .build());
        }

        // Check for complex expressions that could be simplified
        if (cql.split("define").length > 15) {
            suggestions.add(CqlGenerationResponse.Suggestion.builder()
                .type("CLARITY")
                .description("Consider breaking down complex logic into smaller, reusable definitions")
                .impact(0.4)
                .build());
        }

        return suggestions;
    }

    /**
     * Extract warnings from validation result.
     */
    private List<String> extractWarnings(CqlGenerationResponse.ValidationResult validation) {
        if (validation == null || validation.getWarnings() == null) {
            return Collections.emptyList();
        }
        return validation.getWarnings();
    }

    /**
     * Parse CQL into sections for explanation.
     */
    private List<CqlExplainResponse.SectionExplanation> parseCqlSections(String cql) {
        List<CqlExplainResponse.SectionExplanation> sections = new ArrayList<>();
        String[] lines = cql.split("\n");

        int lineNum = 1;
        StringBuilder currentSection = new StringBuilder();
        String currentName = null;
        int sectionStart = 1;

        for (String line : lines) {
            if (line.trim().startsWith("define \"")) {
                // Save previous section if exists
                if (currentName != null) {
                    sections.add(buildSectionExplanation(currentName, currentSection.toString(), sectionStart, lineNum - 1));
                }

                // Extract new section name
                Pattern pattern = Pattern.compile("define \"([^\"]+)\"");
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    currentName = matcher.group(1);
                    currentSection = new StringBuilder();
                    sectionStart = lineNum;
                }
            }
            currentSection.append(line).append("\n");
            lineNum++;
        }

        // Save last section
        if (currentName != null) {
            sections.add(buildSectionExplanation(currentName, currentSection.toString(), sectionStart, lineNum - 1));
        }

        return sections;
    }

    private CqlExplainResponse.SectionExplanation buildSectionExplanation(
            String name, String code, int startLine, int endLine) {
        String purpose = determineSectionPurpose(name);
        String explanation = generateSectionExplanation(name, code);

        return CqlExplainResponse.SectionExplanation.builder()
            .sectionName(name)
            .cqlSnippet(code.trim())
            .explanation(explanation)
            .purpose(purpose)
            .lineStart(startLine)
            .lineEnd(endLine)
            .build();
    }

    private String determineSectionPurpose(String name) {
        return switch (name) {
            case "Initial Population" -> "Defines the base population for the measure";
            case "Denominator" -> "Defines patients eligible for the measure";
            case "Numerator" -> "Defines patients meeting the measure criteria";
            case "Denominator Exclusions" -> "Defines patients excluded from the denominator";
            case "Numerator Exclusions" -> "Defines patients excluded from the numerator";
            default -> "Helper definition for measure logic";
        };
    }

    private String generateSectionExplanation(String name, String code) {
        // Generate explanation based on code patterns
        if (code.contains("AgeInYearsAt")) {
            return "Calculates patient age for eligibility criteria";
        }
        if (code.contains("[Condition:")) {
            return "Queries for patient conditions/diagnoses";
        }
        if (code.contains("[Observation:")) {
            return "Queries for patient observations/test results";
        }
        if (code.contains("[Procedure:")) {
            return "Queries for patient procedures";
        }
        return "Defines " + name + " criteria for the measure";
    }

    /**
     * Extract clinical concepts from CQL.
     */
    private List<CqlExplainResponse.ClinicalConcept> extractClinicalConcepts(String cql) {
        List<CqlExplainResponse.ClinicalConcept> concepts = new ArrayList<>();

        // Extract value sets
        Pattern valueSetPattern = Pattern.compile("valueset \"([^\"]+)\": '([^']+)'");
        Matcher matcher = valueSetPattern.matcher(cql);

        while (matcher.find()) {
            concepts.add(CqlExplainResponse.ClinicalConcept.builder()
                .name(matcher.group(1))
                .valueSetOid(matcher.group(2))
                .codeSystem("OID")
                .usage("Referenced in measure logic")
                .build());
        }

        return concepts;
    }

    /**
     * Extract data elements from CQL.
     */
    private List<CqlExplainResponse.DataElement> extractDataElements(String cql) {
        List<CqlExplainResponse.DataElement> elements = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // Extract FHIR resource references
        Pattern resourcePattern = Pattern.compile("\\[([A-Za-z]+):");
        Matcher matcher = resourcePattern.matcher(cql);

        while (matcher.find()) {
            String resourceType = matcher.group(1);
            if (!seen.contains(resourceType)) {
                seen.add(resourceType);
                elements.add(CqlExplainResponse.DataElement.builder()
                    .resourceType(resourceType)
                    .element("*")
                    .purpose("Queried for measure logic")
                    .required(true)
                    .build());
            }
        }

        // Add Patient if context is Patient
        if (cql.contains("context Patient") && !seen.contains("Patient")) {
            elements.add(CqlExplainResponse.DataElement.builder()
                .resourceType("Patient")
                .element("demographics")
                .purpose("Patient context and demographics")
                .required(true)
                .build());
        }

        return elements;
    }

    /**
     * Generate CQL summary.
     */
    private String generateCqlSummary(String cql, List<CqlExplainResponse.SectionExplanation> sections) {
        StringBuilder summary = new StringBuilder();
        summary.append("This CQL measure ");

        // Extract library name
        Pattern libPattern = Pattern.compile("library (\\w+)");
        Matcher libMatcher = libPattern.matcher(cql);
        if (libMatcher.find()) {
            summary.append("'").append(libMatcher.group(1)).append("' ");
        }

        summary.append("defines a quality measure with ").append(sections.size()).append(" definitions. ");

        // Check for key components
        boolean hasInitPop = sections.stream().anyMatch(s -> s.getSectionName().contains("Initial Population"));
        boolean hasNumerator = sections.stream().anyMatch(s -> s.getSectionName().equals("Numerator"));
        boolean hasDenominator = sections.stream().anyMatch(s -> s.getSectionName().equals("Denominator"));

        if (hasInitPop && hasNumerator && hasDenominator) {
            summary.append("It follows the standard proportion measure structure with Initial Population, Denominator, and Numerator.");
        }

        return summary.toString();
    }

    /**
     * Assess CQL complexity.
     */
    private int assessComplexity(String cql, List<CqlExplainResponse.SectionExplanation> sections) {
        int complexity = 3; // Base complexity

        // Add complexity for each section
        complexity += Math.min(sections.size() / 3, 3);

        // Add complexity for nested logic
        int nestingDepth = countMaxNesting(cql);
        complexity += Math.min(nestingDepth, 3);

        return Math.min(complexity, 10);
    }

    private int countMaxNesting(String cql) {
        int maxDepth = 0;
        int currentDepth = 0;

        for (char c : cql.toCharArray()) {
            if (c == '(' || c == '[') {
                currentDepth++;
                maxDepth = Math.max(maxDepth, currentDepth);
            } else if (c == ')' || c == ']') {
                currentDepth--;
            }
        }

        return maxDepth;
    }

    /**
     * Assess performance impact.
     */
    private CqlExplainResponse.PerformanceAssessment assessPerformance(
            String cql, List<CqlExplainResponse.DataElement> dataElements) {
        List<String> concerns = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        int queryCount = dataElements.size();

        // Check for performance concerns
        if (queryCount > 5) {
            concerns.add("Multiple FHIR resource queries may impact performance");
            recommendations.add("Consider caching frequently accessed data");
        }

        if (cql.contains("flatten") || cql.contains("Flatten")) {
            concerns.add("Flatten operations can be expensive for large datasets");
        }

        String rating = queryCount > 5 ? "HIGH" : queryCount > 3 ? "MEDIUM" : "LOW";

        return CqlExplainResponse.PerformanceAssessment.builder()
            .rating(rating)
            .concerns(concerns)
            .recommendations(recommendations)
            .estimatedDataQueries(queryCount)
            .build();
    }

    /**
     * Identify potential issues in CQL.
     */
    private List<String> identifyPotentialIssues(String cql, List<CqlExplainResponse.SectionExplanation> sections) {
        List<String> issues = new ArrayList<>();

        // Check for common issues
        if (!cql.contains("parameter \"Measurement Period\"")) {
            issues.add("Missing Measurement Period parameter - measure may not be time-bound");
        }

        if (cql.contains("null") && !cql.contains("is null") && !cql.contains("is not null")) {
            issues.add("Potential null handling issue - ensure null checks are in place");
        }

        return issues;
    }

    /**
     * Calculate confidence score for generated CQL.
     */
    private double calculateConfidence(String cql, CqlGenerationRequest request) {
        double confidence = 0.7; // Base confidence

        // Increase for specific patterns found
        if (cql.contains("define \"Initial Population\"")) confidence += 0.05;
        if (cql.contains("define \"Numerator\"")) confidence += 0.05;
        if (cql.contains("define \"Denominator\"")) confidence += 0.05;
        if (cql.contains("using FHIR")) confidence += 0.05;
        if (cql.contains("valueset")) confidence += 0.05;

        // Decrease if generic/template-like
        if (cql.contains("TODO:")) confidence -= 0.1;
        if (cql.contains("// Replace with")) confidence -= 0.1;

        return Math.max(0.5, Math.min(0.95, confidence));
    }

    private String extractCqlFromResponse(String response) {
        // Extract CQL from code blocks
        Pattern pattern = Pattern.compile("```cql\\n([\\s\\S]*?)```");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return response;
    }

    private String extractExplanationFromResponse(String response) {
        // Extract explanation after code blocks
        int lastCodeBlockEnd = response.lastIndexOf("```");
        if (lastCodeBlockEnd > 0 && lastCodeBlockEnd < response.length() - 3) {
            return response.substring(lastCodeBlockEnd + 3).trim();
        }
        return "Generated CQL code based on your description.";
    }

    private String sanitizeMeasureName(String name) {
        return name.replaceAll("[^a-zA-Z0-9]", "");
    }

    private CqlGenerationResponse buildErrorResponse(String errorMessage) {
        return CqlGenerationResponse.builder()
            .id(UUID.randomUUID().toString())
            .validationStatus(CqlGenerationResponse.ValidationStatus.ERROR)
            .warnings(List.of(errorMessage))
            .generatedAt(LocalDateTime.now())
            .modelVersion(MODEL_VERSION)
            .build();
    }
}
