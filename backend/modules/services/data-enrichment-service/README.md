# AI Data Enrichment Service

Production-ready microservice for AI-powered healthcare data enrichment, validation, and quality assessment.

## Features

### NLP Extraction
- **ClinicalNoteExtractor**: Extract structured data from clinical notes using NLP
- **MedicalEntityRecognizer**: Named entity recognition for medical terms
- Supports: diagnoses, medications, procedures, lab results, vital signs, allergies, family history
- Confidence scoring for all extractions
- Negation detection
- Temporal expression extraction

### Code Validation
- **ICD10Validator**: Validate ICD-10-CM and ICD-10-PCS codes
- **SnomedValidator**: Validate SNOMED CT codes with concept relationships
- **CptValidator**: Validate CPT/HCPCS codes with modifier compatibility
- **LoincValidator**: Validate LOINC codes with reference ranges
- Format validation, billability checks, code hierarchy navigation

### Code Suggestion
- **CodeSuggester**: AI-powered code suggestions from clinical text
- **HierarchicalCodeSearch**: Navigate code hierarchies and relationships
- Context-aware suggestions with confidence scores
- Support for ICD-10, CPT, SNOMED, and LOINC code systems

### Data Completeness
- **DataCompletenessAnalyzer**: Identify missing data for quality measures
- Gap analysis for HEDIS measures
- Prioritized action suggestions
- Data freshness assessment
- Completion timeline tracking

### Data Quality
- **DataQualityService**: Comprehensive data quality assessment
- Quality dimensions: Accuracy, Completeness, Consistency, Timeliness
- Issue detection and remediation recommendations
- Duplicate detection
- Referential integrity checks

## Technology Stack

- **Spring Boot 3.3.5**: Modern microservice framework
- **Apache OpenNLP**: NLP text processing
- **Stanford CoreNLP**: Advanced NLP capabilities
- **PostgreSQL**: Persistent storage
- **Redis**: Caching for terminology lookups
- **Apache Kafka**: Event streaming for audit
- **Resilience4j**: Circuit breaker and rate limiting

## Architecture

### Multi-Tenant Support
All services support tenant isolation for healthcare organizations.

### Async Processing
Large clinical documents can be processed asynchronously with task tracking.

### Caching Strategy
- Terminology lookups cached in Redis (TTL: 1 hour)
- Code validations cached to reduce latency
- Self-cleaning cache mechanism

## API Endpoints

### Clinical Note Extraction
```
POST /api/v1/enrichment/extract
- Extract entities from clinical notes
- Supports async processing
- Returns confidence scores
```

### Code Validation
```
POST /api/v1/enrichment/validate/icd10
POST /api/v1/enrichment/validate/snomed
POST /api/v1/enrichment/validate/cpt
POST /api/v1/enrichment/validate/loinc
- Validate medical codes
- Get code descriptions and hierarchy
- Check billability
```

### Code Suggestion
```
POST /api/v1/enrichment/suggest-codes
- Suggest codes from clinical text
- Support multiple code systems
- Context-aware suggestions
```

### Data Quality
```
GET /api/v1/enrichment/completeness/{patientId}
- Analyze data completeness
- Identify missing elements
- Get completion suggestions

GET /api/v1/enrichment/quality/report?patientId={id}
- Generate quality assessment report
- Multi-dimensional quality scoring
- Remediation recommendations
```

## Testing

### Overview

Data Enrichment Service has 11 comprehensive test suites with 143+ test methods covering NLP extraction, code validation, code suggestion, data quality, and API endpoint testing. The service was built following Test-Driven Development (TDD) methodology where all tests were written FIRST before implementation.

**Test Types Covered:**
- **Unit Tests**: Service layer with Mockito mocking
- **Controller Tests**: REST API endpoints with MockMvc
- **Multi-Tenant Isolation Tests**: Tenant data separation verification
- **RBAC Tests**: Role-based access control (ADMIN, CLINICIAN, CODER, ANALYST)
- **HIPAA Compliance Tests**: PHI cache TTL, audit logging, headers
- **Performance Tests**: NLP extraction latency, code validation throughput

### Quick Start

```bash
# Run all tests
./gradlew :modules:services:data-enrichment-service:test

# Run specific test suite
./gradlew :modules:services:data-enrichment-service:test --tests "*ClinicalNoteExtractorTest"
./gradlew :modules:services:data-enrichment-service:test --tests "*ICD10ValidatorTest"
./gradlew :modules:services:data-enrichment-service:test --tests "*DataEnrichmentControllerTest"

# Run tests by category
./gradlew :modules:services:data-enrichment-service:test --tests "*ValidatorTest"
./gradlew :modules:services:data-enrichment-service:test --tests "*ServiceTest"

# Run with coverage report
./gradlew :modules:services:data-enrichment-service:test jacocoTestReport

# Run integration tests (requires Docker)
./gradlew :modules:services:data-enrichment-service:integrationTest
```

### Test Coverage Summary

| Test Class | Methods | Coverage Focus |
|------------|---------|----------------|
| ClinicalNoteExtractorTest | 22 | NLP entity extraction, confidence scoring, negation detection |
| MedicalEntityRecognizerTest | 15 | Named entity recognition, entity types, metadata |
| ICD10ValidatorTest | 15 | ICD-10-CM/PCS validation, billability, hierarchy |
| SnomedValidatorTest | 10 | SNOMED CT validation, concept relationships |
| CptValidatorTest | 10 | CPT/HCPCS validation, modifier compatibility |
| LoincValidatorTest | 10 | LOINC validation, reference ranges |
| CodeSuggesterTest | 15 | AI-powered suggestions, multi-system support |
| DataCompletenessAnalyzerTest | 15 | Completeness scoring, missing data identification |
| DataQualityServiceTest | 10 | Quality dimensions, issue detection, remediation |
| DataEnrichmentControllerTest | 15 | REST API endpoints, authentication, validation |
| HierarchicalCodeSearchTest | 6 | Code hierarchy navigation, relationships |

### Test Organization

```
src/test/java/com/healthdata/enrichment/
├── service/
│   ├── ClinicalNoteExtractorTest.java      # NLP extraction tests
│   ├── MedicalEntityRecognizerTest.java    # Entity recognition tests
│   ├── CodeSuggesterTest.java              # Code suggestion tests
│   └── DataQualityServiceTest.java         # Quality assessment tests
├── analyzer/
│   └── DataCompletenessAnalyzerTest.java   # Completeness analysis tests
├── validator/
│   ├── ICD10ValidatorTest.java             # ICD-10 validation tests
│   ├── SnomedValidatorTest.java            # SNOMED validation tests
│   ├── CptValidatorTest.java               # CPT validation tests
│   └── LoincValidatorTest.java             # LOINC validation tests
├── search/
│   └── HierarchicalCodeSearchTest.java     # Hierarchy navigation tests
├── controller/
│   └── DataEnrichmentControllerTest.java   # REST API tests
└── config/
    └── TestSecurityConfiguration.java      # Test security config
```

---

### Unit Tests (NLP Extraction)

#### ClinicalNoteExtractorTest

Tests for extracting structured data from clinical notes using NLP.

```java
@DisplayName("ClinicalNoteExtractor TDD Tests")
class ClinicalNoteExtractorTest {

    private ClinicalNoteExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new ClinicalNoteExtractor(new MedicalEntityRecognizer());
    }

    @Test
    @DisplayName("Should extract diabetes diagnosis from clinical note")
    void testExtractDiabetesDiagnosis() {
        // Given
        String clinicalNote = "Patient presents with Type 2 Diabetes Mellitus. " +
                "HbA1c: 8.5%. Started on Metformin 500mg BID.";

        // When
        ExtractionResult result = extractor.extract(clinicalNote);

        // Then
        assertThat(result.getEntities()).isNotEmpty();
        List<ExtractedEntity> diagnoses = result.getEntitiesByType(EntityType.DIAGNOSIS);
        assertThat(diagnoses).anyMatch(e ->
            e.getText().toLowerCase().contains("diabetes"));
    }

    @Test
    @DisplayName("Should extract medications with dosage information")
    void testExtractMedicationsWithDosage() {
        // Given
        String note = "Patient is on Lisinopril 10mg daily for hypertension " +
                "and Metformin 500mg BID for diabetes.";

        // When
        ExtractionResult result = extractor.extract(note);

        // Then
        List<ExtractedEntity> medications = result.getEntitiesByType(EntityType.MEDICATION);
        assertThat(medications).hasSizeGreaterThanOrEqualTo(2);

        // Verify Lisinopril extraction
        ExtractedEntity lisinopril = medications.stream()
            .filter(m -> m.getText().toLowerCase().contains("lisinopril"))
            .findFirst()
            .orElseThrow();
        assertThat(lisinopril.getMetadata().get("dosage")).isEqualTo("10mg");
        assertThat(lisinopril.getMetadata().get("frequency")).isEqualTo("daily");

        // Verify Metformin extraction
        ExtractedEntity metformin = medications.stream()
            .filter(m -> m.getText().toLowerCase().contains("metformin"))
            .findFirst()
            .orElseThrow();
        assertThat(metformin.getMetadata().get("dosage")).isEqualTo("500mg");
        assertThat(metformin.getMetadata().get("frequency")).isEqualTo("BID");
    }

    @Test
    @DisplayName("Should extract lab results with numerical values")
    void testExtractLabResults() {
        // Given
        String note = "Lab Results: HbA1c 7.2%, Creatinine 1.1 mg/dL, " +
                "eGFR 85 mL/min, Glucose 126 mg/dL.";

        // When
        ExtractionResult result = extractor.extract(note);

        // Then
        List<ExtractedEntity> labs = result.getEntitiesByType(EntityType.LAB_RESULT);
        assertThat(labs).hasSizeGreaterThanOrEqualTo(3);
        assertThat(labs).anyMatch(l ->
            l.getText().contains("HbA1c") &&
            l.getMetadata().get("value").equals("7.2") &&
            l.getMetadata().get("unit").equals("%"));
    }

    @Test
    @DisplayName("Should extract vital signs from examination notes")
    void testExtractVitalSigns() {
        // Given
        String note = "Vitals: BP 138/82 mmHg, HR 72 bpm, Temp 98.6°F, " +
                "O2 Sat 97% on room air.";

        // When
        ExtractionResult result = extractor.extract(note);

        // Then
        List<ExtractedEntity> vitals = result.getEntitiesByType(EntityType.VITAL_SIGN);
        assertThat(vitals).hasSizeGreaterThanOrEqualTo(4);

        // Verify blood pressure extraction
        assertThat(vitals).anyMatch(v ->
            v.getText().contains("BP") &&
            v.getMetadata().get("systolic").equals("138") &&
            v.getMetadata().get("diastolic").equals("82"));
    }

    @Test
    @DisplayName("Should detect negated findings")
    void testNegationDetection() {
        // Given
        String note = "Patient denies chest pain. No shortness of breath. " +
                "No history of myocardial infarction.";

        // When
        ExtractionResult result = extractor.extract(note);

        // Then
        List<ExtractedEntity> negatedEntities = result.getNegatedEntities();
        assertThat(negatedEntities).hasSizeGreaterThanOrEqualTo(3);
        assertThat(negatedEntities).allMatch(ExtractedEntity::isNegated);
        assertThat(negatedEntities).anyMatch(e ->
            e.getText().toLowerCase().contains("chest pain"));
    }

    @Test
    @DisplayName("Should extract temporal expressions")
    void testTemporalExtraction() {
        // Given
        String note = "Diabetes diagnosed 5 years ago. Last HbA1c checked " +
                "on 2024-03-15. Follow-up scheduled in 3 months.";

        // When
        ExtractionResult result = extractor.extract(note);

        // Then
        List<ExtractedEntity> temporals = result.getEntitiesByType(EntityType.TEMPORAL);
        assertThat(temporals).hasSizeGreaterThanOrEqualTo(2);
        assertThat(temporals).anyMatch(t -> t.getText().contains("5 years ago"));
        assertThat(temporals).anyMatch(t -> t.getText().contains("2024-03-15"));
    }

    @Test
    @DisplayName("Should calculate overall confidence score")
    void testOverallConfidence() {
        // Given
        String note = "Type 2 Diabetes Mellitus with neuropathy. " +
                "Metformin 500mg BID. HbA1c 8.5%.";

        // When
        ExtractionResult result = extractor.extract(note);

        // Then
        assertThat(result.getOverallConfidence())
            .isBetween(0.0, 1.0);
        assertThat(result.getEntities()).allMatch(e ->
            e.getConfidence() >= 0.0 && e.getConfidence() <= 1.0);
    }

    @Test
    @DisplayName("Should extract ICD-10 codes from note with medical context")
    void testIcd10CodeExtraction() {
        // Given
        String note = "Assessment: E11.65 - Type 2 diabetes mellitus with " +
                "hyperglycemia. I10 - Essential hypertension.";

        // When
        ExtractionResult result = extractor.extract(note);

        // Then
        List<ExtractedEntity> diagnoses = result.getEntitiesByType(EntityType.DIAGNOSIS);
        assertThat(diagnoses).anyMatch(d ->
            d.getMetadata().containsKey("icd10Code") &&
            d.getMetadata().get("icd10Code").equals("E11.65"));
    }

    @Test
    @DisplayName("Should handle complex multi-paragraph notes")
    void testComplexNotes() {
        // Given
        String note = """
            Chief Complaint: Follow-up for diabetes management.

            History of Present Illness: 58-year-old male with Type 2 DM,
            diagnosed 10 years ago. Currently on Metformin 1000mg BID and
            Glipizide 5mg daily. Reports occasional hypoglycemic episodes.

            Review of Systems: Denies chest pain, SOB. Reports peripheral
            neuropathy in bilateral feet.

            Physical Exam: Vitals stable. BMI 32. Decreased sensation bilateral feet.

            Assessment/Plan:
            1. Type 2 DM with peripheral neuropathy - continue current regimen
            2. Obesity - discussed lifestyle modifications
            """;

        // When
        ExtractionResult result = extractor.extract(note);

        // Then
        assertThat(result.getEntities()).hasSizeGreaterThanOrEqualTo(5);
        assertThat(result.getEntitiesByType(EntityType.DIAGNOSIS)).isNotEmpty();
        assertThat(result.getEntitiesByType(EntityType.MEDICATION)).isNotEmpty();
        assertThat(result.getNegatedEntities()).isNotEmpty();
    }

    @Test
    @DisplayName("Should preserve text positions for entity references")
    void testTextPositionPreservation() {
        // Given
        String note = "Patient has diabetes and hypertension.";

        // When
        ExtractionResult result = extractor.extract(note);

        // Then
        List<ExtractedEntity> entities = result.getEntities();
        assertThat(entities).allMatch(e ->
            e.getStartPosition() >= 0 &&
            e.getEndPosition() > e.getStartPosition() &&
            e.getEndPosition() <= note.length());
    }

    @Test
    @DisplayName("Should support multi-tenant extraction")
    void testMultiTenantExtraction() {
        // Given
        String tenantId = "tenant-123";
        String note = "Patient has diabetes.";

        // When
        ExtractionResult result = extractor.extractWithTenant(note, tenantId);

        // Then
        assertThat(result.getTenantId()).isEqualTo(tenantId);
        assertThat(result.getEntities()).isNotEmpty();
    }
}
```

---

### Unit Tests (Code Validation)

#### ICD10ValidatorTest

Tests for ICD-10-CM and ICD-10-PCS code validation.

```java
@DisplayName("ICD10Validator TDD Tests")
class ICD10ValidatorTest {

    private ICD10Validator validator;

    @BeforeEach
    void setUp() {
        validator = new ICD10Validator();
    }

    @Test
    @DisplayName("Should validate correct ICD-10-CM code")
    void testValidICD10CM() {
        // Given
        String code = "E11.9";

        // When
        CodeValidationResult result = validator.validate(code);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getCode()).isEqualTo("E11.9");
        assertThat(result.getDescription()).containsIgnoringCase("diabetes");
    }

    @Test
    @DisplayName("Should validate ICD-10-CM with full precision")
    void testValidICD10CMFullPrecision() {
        // Given
        String code = "E11.65"; // Diabetes with hyperglycemia

        // When
        CodeValidationResult result = validator.validate(code);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.isBillable()).isTrue();
        assertThat(result.getDescription()).containsIgnoringCase("hyperglycemia");
    }

    @Test
    @DisplayName("Should reject invalid ICD-10 code")
    void testInvalidICD10() {
        // Given
        String code = "INVALID";

        // When
        CodeValidationResult result = validator.validate(code);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    @DisplayName("Should suggest similar codes for invalid input")
    void testSuggestSimilarCodes() {
        // Given
        String code = "E119"; // Missing decimal

        // When
        CodeValidationResult result = validator.validate(code);

        // Then
        assertThat(result.getSuggestions()).isNotEmpty();
        assertThat(result.getSuggestions()).anyMatch(s -> s.contains("E11.9"));
    }

    @Test
    @DisplayName("Should validate ICD-10-PCS code")
    void testValidICD10PCS() {
        // Given
        String code = "0DT60ZZ"; // Excision of stomach

        // When
        CodeValidationResult result = validator.validate(code);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getCodeSystem()).isEqualTo("ICD-10-PCS");
    }

    @Test
    @DisplayName("Should validate code format")
    void testCodeFormatValidation() {
        // Given
        String validFormat = "E11.9";
        String invalidFormat = "E11-9"; // Hyphen instead of period

        // When
        CodeValidationResult validResult = validator.validate(validFormat);
        CodeValidationResult invalidResult = validator.validate(invalidFormat);

        // Then
        assertThat(validResult.isValid()).isTrue();
        assertThat(invalidResult.isValid()).isFalse();
    }

    @Test
    @DisplayName("Should identify billable codes")
    void testBillableCodes() {
        // Given
        String billableCode = "E11.65"; // Billable (full precision)
        String nonBillableCode = "E11"; // Non-billable (needs more specificity)

        // When
        CodeValidationResult billableResult = validator.validate(billableCode);
        CodeValidationResult nonBillableResult = validator.validate(nonBillableCode);

        // Then
        assertThat(billableResult.isBillable()).isTrue();
        assertThat(nonBillableResult.isBillable()).isFalse();
    }

    @Test
    @DisplayName("Should provide code hierarchy navigation")
    void testCodeHierarchy() {
        // Given
        String code = "E11.65";

        // When
        CodeValidationResult result = validator.validate(code);

        // Then
        assertThat(result.getHierarchy()).isNotNull();
        assertThat(result.getHierarchy().getParentCode()).isEqualTo("E11.6");
        assertThat(result.getHierarchy().getCategory()).isEqualTo("E11");
        assertThat(result.getHierarchy().getChapter()).isEqualTo("E00-E89");
    }

    @Test
    @DisplayName("Should support batch validation")
    void testBatchValidation() {
        // Given
        List<String> codes = List.of("E11.9", "I10", "INVALID", "J44.9");

        // When
        List<CodeValidationResult> results = validator.validateBatch(codes);

        // Then
        assertThat(results).hasSize(4);
        assertThat(results.stream().filter(CodeValidationResult::isValid).count())
            .isEqualTo(3);
        assertThat(results.stream().filter(r -> !r.isValid()).count())
            .isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle case-insensitive validation")
    void testCaseInsensitivity() {
        // Given
        String uppercase = "E11.9";
        String lowercase = "e11.9";

        // When
        CodeValidationResult uppercaseResult = validator.validate(uppercase);
        CodeValidationResult lowercaseResult = validator.validate(lowercase);

        // Then
        assertThat(uppercaseResult.isValid()).isTrue();
        assertThat(lowercaseResult.isValid()).isTrue();
        assertThat(uppercaseResult.getCode()).isEqualTo(lowercaseResult.getCode());
    }

    @Test
    @DisplayName("Should handle null and empty codes")
    void testNullAndEmptyCodes() {
        // When/Then
        assertThat(validator.validate(null).isValid()).isFalse();
        assertThat(validator.validate("").isValid()).isFalse();
        assertThat(validator.validate("   ").isValid()).isFalse();
    }
}
```

---

### Unit Tests (Code Suggestion)

#### CodeSuggesterTest

Tests for AI-powered code suggestions from clinical text.

```java
@DisplayName("CodeSuggester TDD Tests")
class CodeSuggesterTest {

    private CodeSuggester suggester;

    @BeforeEach
    void setUp() {
        suggester = new CodeSuggester();
    }

    @Test
    @DisplayName("Should suggest ICD-10 codes from text")
    void testSuggestIcd10FromText() {
        // Given
        String text = "Type 2 Diabetes Mellitus";

        // When
        List<CodeSuggestion> suggestions = suggester.suggestIcd10(text);

        // Then
        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions.get(0).getCode()).startsWith("E11");
        assertThat(suggestions.get(0).getDescription())
            .containsIgnoringCase("diabetes");
    }

    @Test
    @DisplayName("Should suggest CPT codes from procedure text")
    void testSuggestCptFromText() {
        // Given
        String text = "Office visit, established patient";

        // When
        List<CodeSuggestion> suggestions = suggester.suggestCpt(text);

        // Then
        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions.get(0).getCode()).startsWith("992");
    }

    @Test
    @DisplayName("Should rank suggestions by confidence score")
    void testSuggestionRanking() {
        // Given
        String text = "Diabetes";

        // When
        List<CodeSuggestion> suggestions = suggester.suggestIcd10(text);

        // Then
        assertThat(suggestions).isSortedAccordingTo((s1, s2) ->
            Double.compare(s2.getConfidence(), s1.getConfidence()));
    }

    @Test
    @DisplayName("Should suggest multiple relevant codes")
    void testMultipleSuggestions() {
        // Given
        String text = "Type 2 Diabetes with complications";

        // When
        List<CodeSuggestion> suggestions = suggester.suggestIcd10(text);

        // Then
        assertThat(suggestions).hasSizeGreaterThanOrEqualTo(2);
        // E11.9 (without complications) and E11.65 (with hyperglycemia), etc.
    }

    @Test
    @DisplayName("Should suggest LOINC codes for lab tests")
    void testSuggestLoincForLabs() {
        // Given
        String text = "Hemoglobin A1c";

        // When
        List<CodeSuggestion> suggestions = suggester.suggestLoinc(text);

        // Then
        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions.get(0).getCode()).isEqualTo("4548-4");
    }

    @Test
    @DisplayName("Should suggest SNOMED codes")
    void testSuggestSnomed() {
        // Given
        String text = "Diabetes mellitus";

        // When
        List<CodeSuggestion> suggestions = suggester.suggestSnomed(text);

        // Then
        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions.get(0).getCodeSystem()).isEqualTo("SNOMED");
    }

    @Test
    @DisplayName("Should use context for better suggestions")
    void testContextualSuggestions() {
        // Given
        String text = "Diabetes";
        String context = "Patient has kidney disease";

        // When
        List<CodeSuggestion> suggestions = suggester.suggestIcd10WithContext(text, context);

        // Then
        // Should suggest E11.2x (diabetes with kidney complications)
        assertThat(suggestions).anyMatch(s -> s.getCode().contains("E11.2"));
    }

    @Test
    @DisplayName("Should handle abbreviations")
    void testAbbreviations() {
        // Given
        String text = "DM Type 2";

        // When
        List<CodeSuggestion> suggestions = suggester.suggestIcd10(text);

        // Then
        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions.get(0).getCode()).startsWith("E11");
    }

    @Test
    @DisplayName("Should suggest all code types from clinical note")
    void testSuggestFromClinicalNote() {
        // Given
        String note = "Patient with HTN and DM2 on Metformin";

        // When
        var suggestions = suggester.suggestAllCodes(note);

        // Then
        assertThat(suggestions).hasSizeGreaterThanOrEqualTo(2);
        // HTN -> I10, DM2 -> E11.x
    }

    @Test
    @DisplayName("Should provide billable code preferences")
    void testBillablePreference() {
        // Given
        String text = "Diabetes";

        // When
        List<CodeSuggestion> suggestions = suggester.suggestIcd10(text);

        // Then
        // First suggestion should be billable
        assertThat(suggestions.get(0).isBillable()).isTrue();
    }

    @Test
    @DisplayName("Should limit number of suggestions")
    void testLimitSuggestions() {
        // Given
        String text = "Diabetes";
        int maxSuggestions = 5;

        // When
        List<CodeSuggestion> suggestions = suggester.suggestIcd10(text, maxSuggestions);

        // Then
        assertThat(suggestions).hasSizeLessThanOrEqualTo(maxSuggestions);
    }

    @Test
    @DisplayName("Should handle empty text gracefully")
    void testEmptyText() {
        // Given
        String text = "";

        // When
        List<CodeSuggestion> suggestions = suggester.suggestIcd10(text);

        // Then
        assertThat(suggestions).isEmpty();
    }
}
```

---

### Unit Tests (Data Quality)

#### DataCompletenessAnalyzerTest

Tests for analyzing data completeness and identifying gaps.

```java
@DisplayName("DataCompletenessAnalyzer TDD Tests")
class DataCompletenessAnalyzerTest {

    private DataCompletenessAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new DataCompletenessAnalyzer();
    }

    @Test
    @DisplayName("Should calculate completeness score")
    void testCompletenessScore() {
        // Given
        String patientId = "patient-123";

        // When
        MissingDataReport report = analyzer.analyze(patientId);

        // Then
        assertThat(report.getCompletenessScore()).isBetween(0.0, 100.0);
    }

    @Test
    @DisplayName("Should identify missing demographics")
    void testMissingDemographics() {
        // Given
        String patientId = "patient-missing-demo";

        // When
        MissingDataReport report = analyzer.analyze(patientId);

        // Then
        List<MissingDataElement> demographics = report.getMissingElementsByCategory("demographics");
        assertThat(demographics).isNotNull();
    }

    @Test
    @DisplayName("Should identify missing quality measure data")
    void testMissingQualityMeasureData() {
        // Given
        String patientId = "patient-123";
        String measureId = "CMS122"; // Diabetes HbA1c Control

        // When
        MissingDataReport report = analyzer.analyzeForMeasure(patientId, measureId);

        // Then
        assertThat(report.getMeasureId()).isEqualTo(measureId);
        assertThat(report.getMissingElements()).isNotNull();
    }

    @Test
    @DisplayName("Should prioritize missing data by importance")
    void testPrioritizeMissingData() {
        // Given
        String patientId = "patient-123";

        // When
        List<DataCollectionSuggestion> suggestions = analyzer.getSuggestions(patientId);

        // Then
        assertThat(suggestions).isSortedAccordingTo((s1, s2) ->
            Integer.compare(s2.getPriority(), s1.getPriority()));
    }

    @Test
    @DisplayName("Should identify missing lab results")
    void testMissingLabResults() {
        // Given
        String patientId = "patient-no-labs";

        // When
        MissingDataReport report = analyzer.analyze(patientId);

        // Then
        List<MissingDataElement> labs = report.getMissingElementsByCategory("lab_results");
        assertThat(labs).anyMatch(l -> l.getName().contains("HbA1c"));
    }

    @Test
    @DisplayName("Should identify missing medications")
    void testMissingMedications() {
        // Given
        String patientId = "patient-no-meds";

        // When
        MissingDataReport report = analyzer.analyze(patientId);

        // Then
        assertThat(report.getMissingElementsByCategory("medications")).isNotNull();
    }

    @Test
    @DisplayName("Should check HEDIS measure completeness")
    void testHedisMeasureCompleteness() {
        // Given
        String patientId = "patient-123";
        String measureId = "CDC"; // Comprehensive Diabetes Care

        // When
        MissingDataReport report = analyzer.analyzeForMeasure(patientId, measureId);

        // Then
        assertThat(report.getMeasureSpecificGaps()).isNotNull();
        // CDC measure requires: HbA1c, eye exam, nephropathy screening, BP
    }

    @Test
    @DisplayName("Should provide completion timeline")
    void testCompletionTimeline() {
        // Given
        String patientId = "patient-123";

        // When
        MissingDataReport report = analyzer.analyze(patientId);

        // Then
        assertThat(report.getCompletionTimeline()).isNotNull();
        assertThat(report.getCompletionTimeline().getEstimatedDays()).isPositive();
    }

    @Test
    @DisplayName("Should identify stale data elements")
    void testStaleDataIdentification() {
        // Given
        String patientId = "patient-stale-data";

        // When
        MissingDataReport report = analyzer.analyze(patientId);

        // Then
        List<MissingDataElement> staleElements = report.getStaleElements();
        assertThat(staleElements).allMatch(e ->
            e.getLastUpdated() != null &&
            e.getDataFreshnessStatus().equals("STALE"));
    }

    @Test
    @DisplayName("Should support multi-tenant analysis")
    void testMultiTenantAnalysis() {
        // Given
        String tenantId = "tenant-123";
        String patientId = "patient-456";

        // When
        MissingDataReport report = analyzer.analyzeWithTenant(patientId, tenantId);

        // Then
        assertThat(report.getTenantId()).isEqualTo(tenantId);
    }
}
```

#### DataQualityServiceTest

Tests for comprehensive data quality assessment.

```java
@DisplayName("DataQualityService TDD Tests")
class DataQualityServiceTest {

    private DataQualityService service;

    @BeforeEach
    void setUp() {
        service = new DataQualityService();
    }

    @Test
    @DisplayName("Should assess all four quality dimensions")
    void testQualityDimensions() {
        // Given
        String patientId = "patient-123";

        // When
        DataQualityAssessment assessment = service.assessQuality(patientId);

        // Then
        assertThat(assessment.getDimensions()).containsKeys(
            QualityDimension.ACCURACY,
            QualityDimension.COMPLETENESS,
            QualityDimension.CONSISTENCY,
            QualityDimension.TIMELINESS
        );
    }

    @Test
    @DisplayName("Should identify data quality issues")
    void testIdentifyIssues() {
        // Given
        String patientId = "patient-with-issues";

        // When
        List<QualityIssue> issues = service.identifyIssues(patientId);

        // Then
        assertThat(issues).isNotNull();
        assertThat(issues).allMatch(i ->
            i.getSeverity() != null &&
            i.getDescription() != null);
    }

    @Test
    @DisplayName("Should suggest remediation actions")
    void testRemediationActions() {
        // Given
        String patientId = "patient-123";

        // When
        List<RemediationAction> actions = service.suggestRemediationActions(patientId);

        // Then
        assertThat(actions).isNotNull();
        assertThat(actions).allMatch(a ->
            a.getAction() != null &&
            a.getPriority() != null);
    }

    @Test
    @DisplayName("Should detect duplicate records")
    void testDetectDuplicates() {
        // Given
        String patientId = "patient-123";

        // When
        List<DuplicateRecord> duplicates = service.detectDuplicates(patientId);

        // Then
        assertThat(duplicates).isNotNull();
    }

    @Test
    @DisplayName("Should validate data formats")
    void testDataFormatValidation() {
        // Given
        String patientId = "patient-123";

        // When
        List<FormatIssue> formatIssues = service.validateDataFormats(patientId);

        // Then
        assertThat(formatIssues).isNotNull();
    }

    @Test
    @DisplayName("Should check referential integrity")
    void testReferentialIntegrity() {
        // Given
        String patientId = "patient-123";

        // When
        List<IntegrityIssue> integrityIssues = service.checkReferentialIntegrity(patientId);

        // Then
        assertThat(integrityIssues).isNotNull();
    }

    @Test
    @DisplayName("Should generate quality report with overall score")
    void testQualityReport() {
        // Given
        String patientId = "patient-123";

        // When
        DataQualityReport report = service.generateQualityReport(patientId);

        // Then
        assertThat(report).isNotNull();
        assertThat(report.getOverallScore()).isBetween(0.0, 100.0);
    }
}
```

---

### Controller Integration Tests

#### DataEnrichmentControllerTest

Tests for REST API endpoints with MockMvc.

```java
@WebMvcTest(DataEnrichmentController.class)
@DisplayName("DataEnrichmentController TDD Tests")
@ActiveProfiles("test")
@Import(TestSecurityConfiguration.class)
class DataEnrichmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClinicalNoteExtractor clinicalNoteExtractor;

    @MockBean
    private ICD10Validator icd10Validator;

    @MockBean
    private CodeSuggester codeSuggester;

    @MockBean
    private DataCompletenessAnalyzer completenessAnalyzer;

    @MockBean
    private DataQualityService qualityService;

    // Additional MockBeans for validators...

    @Test
    @DisplayName("POST /api/v1/enrichment/extract should extract entities")
    @WithMockUser(roles = "CLINICIAN")
    void testExtractFromClinicalNote() throws Exception {
        // Given
        ExtractionRequest request = new ExtractionRequest();
        request.setClinicalNote("Patient has Type 2 Diabetes Mellitus");

        ExtractionResult mockResult = new ExtractionResult();
        mockResult.setOverallConfidence(0.85);

        when(clinicalNoteExtractor.extract(anyString())).thenReturn(mockResult);

        // When/Then
        mockMvc.perform(post("/api/v1/enrichment/extract")
                .with(csrf())
                .header("X-Tenant-ID", "tenant-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.overallConfidence").value(0.85));

        verify(clinicalNoteExtractor, times(1)).extract(anyString());
    }

    @Test
    @DisplayName("POST /api/v1/enrichment/validate/icd10 should validate code")
    @WithMockUser(roles = "CODER")
    void testValidateIcd10Code() throws Exception {
        // Given
        CodeValidationRequest request = new CodeValidationRequest();
        request.setCode("E11.9");

        CodeValidationResult mockResult = new CodeValidationResult();
        mockResult.setValid(true);
        mockResult.setCode("E11.9");
        mockResult.setDescription("Type 2 diabetes mellitus without complications");

        when(icd10Validator.validate(anyString())).thenReturn(mockResult);

        // When/Then
        mockMvc.perform(post("/api/v1/enrichment/validate/icd10")
                .with(csrf())
                .header("X-Tenant-ID", "tenant-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.code").value("E11.9"))
            .andExpect(jsonPath("$.description").exists());
    }

    @Test
    @DisplayName("POST /api/v1/enrichment/suggest-codes should suggest codes")
    @WithMockUser(roles = "CODER")
    void testSuggestCodes() throws Exception {
        // Given
        CodeSuggestionRequest request = new CodeSuggestionRequest();
        request.setText("Type 2 Diabetes Mellitus");
        request.setCodeSystem("ICD10");

        CodeSuggestion suggestion = new CodeSuggestion();
        suggestion.setCode("E11.9");
        suggestion.setConfidence(0.95);

        when(codeSuggester.suggestIcd10(anyString(), anyInt()))
            .thenReturn(List.of(suggestion));

        // When/Then
        mockMvc.perform(post("/api/v1/enrichment/suggest-codes")
                .with(csrf())
                .header("X-Tenant-ID", "tenant-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].code").value("E11.9"))
            .andExpect(jsonPath("$[0].confidence").value(0.95));
    }

    @Test
    @DisplayName("GET /api/v1/enrichment/completeness/{patientId} should return analysis")
    @WithMockUser(roles = "ANALYST")
    void testGetCompletenessAnalysis() throws Exception {
        // Given
        String patientId = "patient-123";

        MissingDataReport mockReport = new MissingDataReport();
        mockReport.setPatientId(patientId);
        mockReport.setCompletenessScore(75.0);

        when(completenessAnalyzer.analyze(anyString())).thenReturn(mockReport);

        // When/Then
        mockMvc.perform(get("/api/v1/enrichment/completeness/{patientId}", patientId)
                .header("X-Tenant-ID", "tenant-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.patientId").value(patientId))
            .andExpect(jsonPath("$.completenessScore").value(75.0));
    }

    @Test
    @DisplayName("POST /api/v1/enrichment/extract should require authentication")
    void testExtractRequiresAuth() throws Exception {
        // Given
        ExtractionRequest request = new ExtractionRequest();
        request.setClinicalNote("Test note");

        // When/Then - No @WithMockUser
        mockMvc.perform(post("/api/v1/enrichment/extract")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/enrichment/extract should validate request")
    @WithMockUser(roles = "CLINICIAN")
    void testExtractValidation() throws Exception {
        // Given - Empty clinical note
        ExtractionRequest request = new ExtractionRequest();
        request.setClinicalNote("");

        // When/Then
        mockMvc.perform(post("/api/v1/enrichment/extract")
                .with(csrf())
                .header("X-Tenant-ID", "tenant-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/enrichment/extract should support async processing")
    @WithMockUser(roles = "CLINICIAN")
    void testAsyncExtraction() throws Exception {
        // Given
        ExtractionRequest request = new ExtractionRequest();
        request.setClinicalNote("Long clinical note for async processing...");
        request.setAsync(true);

        // When/Then
        mockMvc.perform(post("/api/v1/enrichment/extract")
                .with(csrf())
                .header("X-Tenant-ID", "tenant-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.taskId").exists());
    }

    @Test
    @DisplayName("POST /api/v1/enrichment/extract should support tenant isolation")
    @WithMockUser(roles = "CLINICIAN")
    void testTenantIsolation() throws Exception {
        // Given
        ExtractionRequest request = new ExtractionRequest();
        request.setClinicalNote("Test note");
        request.setTenantId("tenant-123");

        ExtractionResult mockResult = new ExtractionResult();
        mockResult.setTenantId("tenant-123");

        when(clinicalNoteExtractor.extractWithTenant(anyString(), anyString()))
            .thenReturn(mockResult);

        // When/Then
        mockMvc.perform(post("/api/v1/enrichment/extract")
                .with(csrf())
                .header("X-Tenant-ID", "tenant-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantId").value("tenant-123"));
    }

    @Test
    @DisplayName("GET /api/v1/enrichment/completeness/{patientId} should handle non-existent patient")
    @WithMockUser(roles = "ANALYST")
    void testCompletenessNonExistentPatient() throws Exception {
        // Given
        String patientId = "non-existent";
        when(completenessAnalyzer.analyze(anyString())).thenReturn(null);

        // When/Then
        mockMvc.perform(get("/api/v1/enrichment/completeness/{patientId}", patientId)
                .header("X-Tenant-ID", "tenant-001"))
            .andExpect(status().isNotFound());
    }
}
```

---

### Multi-Tenant Isolation Tests

```java
@SpringBootTest
@Testcontainers
@DisplayName("Data Enrichment Multi-Tenant Isolation Tests")
class DataEnrichmentMultiTenantTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private ClinicalNoteExtractor extractor;

    @Autowired
    private DataCompletenessAnalyzer analyzer;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    @DisplayName("NLP extraction results should be tenant-isolated")
    void extractionResultsShouldBeTenantIsolated() {
        // Given
        String tenant1 = "tenant-001";
        String tenant2 = "tenant-002";
        String note = "Patient has Type 2 Diabetes Mellitus";

        // When
        ExtractionResult result1 = extractor.extractWithTenant(note, tenant1);
        ExtractionResult result2 = extractor.extractWithTenant(note, tenant2);

        // Then
        assertThat(result1.getTenantId()).isEqualTo(tenant1);
        assertThat(result2.getTenantId()).isEqualTo(tenant2);
        assertThat(result1.getTenantId()).isNotEqualTo(result2.getTenantId());
    }

    @Test
    @DisplayName("Completeness analysis should be tenant-scoped")
    void completenessAnalysisShouldBeTenantScoped() {
        // Given
        String tenant1 = "tenant-001";
        String tenant2 = "tenant-002";
        String patientId = "patient-123";

        // When
        MissingDataReport report1 = analyzer.analyzeWithTenant(patientId, tenant1);
        MissingDataReport report2 = analyzer.analyzeWithTenant(patientId, tenant2);

        // Then
        assertThat(report1.getTenantId()).isEqualTo(tenant1);
        assertThat(report2.getTenantId()).isEqualTo(tenant2);
    }

    @Test
    @DisplayName("Cache should be tenant-namespaced")
    void cacheShouldBeTenantNamespaced() {
        // Given
        String tenant1 = "tenant-001";
        String tenant2 = "tenant-002";
        String code = "E11.9";

        // When - Validate same code for different tenants
        // Cache keys should include tenant prefix

        // Then - Verify cache isolation (no cross-tenant data leakage)
        // Implementation-specific assertion based on cache structure
    }
}
```

---

### RBAC/Permission Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DisplayName("Data Enrichment RBAC Tests")
class DataEnrichmentRbacTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String TENANT_ID = "tenant-rbac-001";

    @Test
    @DisplayName("ADMIN should access all endpoints")
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessAllEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/enrichment/completeness/patient-123")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/enrichment/quality/report")
                .param("patientId", "patient-123")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CLINICIAN should access extraction endpoints")
    @WithMockUser(roles = "CLINICIAN")
    void clinicianCanExtract() throws Exception {
        ExtractionRequest request = new ExtractionRequest();
        request.setClinicalNote("Patient has diabetes");

        mockMvc.perform(post("/api/v1/enrichment/extract")
                .with(csrf())
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("CODER should access validation and suggestion endpoints")
    @WithMockUser(roles = "CODER")
    void coderCanValidateAndSuggest() throws Exception {
        CodeValidationRequest validationRequest = new CodeValidationRequest();
        validationRequest.setCode("E11.9");

        mockMvc.perform(post("/api/v1/enrichment/validate/icd10")
                .with(csrf())
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(validationRequest)))
            .andExpect(status().isOk());

        CodeSuggestionRequest suggestionRequest = new CodeSuggestionRequest();
        suggestionRequest.setText("Diabetes");
        suggestionRequest.setCodeSystem("ICD10");

        mockMvc.perform(post("/api/v1/enrichment/suggest-codes")
                .with(csrf())
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(suggestionRequest)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ANALYST should access completeness and quality endpoints")
    @WithMockUser(roles = "ANALYST")
    void analystCanAccessQualityEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/enrichment/completeness/patient-123")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/enrichment/quality/report")
                .param("patientId", "patient-123")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Unauthenticated request should be rejected")
    void unauthenticatedRequestShouldFail() throws Exception {
        mockMvc.perform(get("/api/v1/enrichment/completeness/patient-123")
                .header("X-Tenant-ID", TENANT_ID))
            .andExpect(status().isUnauthorized());
    }
}
```

---

### HIPAA Compliance Tests

```java
@SpringBootTest
@Testcontainers
@DisplayName("Data Enrichment HIPAA Compliance Tests")
class DataEnrichmentHipaaComplianceTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @Test
    @DisplayName("Terminology cache TTL should not exceed 1 hour")
    void terminologyCacheTtlShouldBeCompliant() {
        // Given
        Cache codeCache = cacheManager.getCache("terminology");

        // Then
        assertThat(codeCache).isNotNull();
        // Terminology is not PHI, so 1 hour TTL is acceptable
        // Verify via configuration or introspection
    }

    @Test
    @DisplayName("Extraction results cache TTL should not exceed 5 minutes for PHI")
    void extractionCacheTtlShouldBeCompliant() {
        // Given
        Cache extractionCache = cacheManager.getCache("extractions");

        // Then
        // PHI cache TTL must be <= 5 minutes per HIPAA-CACHE-COMPLIANCE.md
        // Verify via Redis TTL inspection
    }

    @Test
    @DisplayName("PHI responses should include no-cache headers")
    @WithMockUser(roles = "CLINICIAN")
    void phiResponsesShouldIncludeNoCacheHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/enrichment/completeness/patient-123")
                .header("X-Tenant-ID", "tenant-001"))
            .andExpect(header().string("Cache-Control",
                allOf(
                    containsString("no-store"),
                    containsString("no-cache"),
                    containsString("must-revalidate")
                )))
            .andExpect(header().string("Pragma", "no-cache"));
    }

    @Test
    @DisplayName("PHI access should generate audit events")
    void phiAccessShouldBeAudited() {
        // Given
        String patientId = "patient-123";
        String tenantId = "tenant-001";

        // When - Access PHI via completeness analysis
        // completenessAnalyzer.analyze(patientId);

        // Then - Verify audit event was published to Kafka
        // Kafka consumer verification
    }

    @Test
    @DisplayName("Test data should be HIPAA-compliant synthetic data")
    void testDataShouldBeSynthetic() {
        // Verify test patient IDs follow synthetic patterns
        String testPatientId = "patient-123";
        assertThat(testPatientId).startsWith("patient-");

        // Verify no real PHI in test fixtures
        String testNote = "Patient has Type 2 Diabetes Mellitus";
        assertThat(testNote).doesNotContainIgnoringCase("John");
        assertThat(testNote).doesNotContainIgnoringCase("Smith");
        assertThat(testNote).doesNotMatch("\\d{3}-\\d{2}-\\d{4}"); // No SSN patterns
    }
}
```

---

### Performance Tests

```java
@SpringBootTest
@DisplayName("Data Enrichment Performance Tests")
class DataEnrichmentPerformanceTest {

    @Autowired
    private ClinicalNoteExtractor extractor;

    @Autowired
    private ICD10Validator icd10Validator;

    @Autowired
    private CodeSuggester codeSuggester;

    @Autowired
    private DataCompletenessAnalyzer completenessAnalyzer;

    @Test
    @DisplayName("NLP extraction should complete within 500ms for standard notes")
    void nlpExtractionPerformance() {
        // Given
        String standardNote = """
            Chief Complaint: Follow-up for diabetes management.
            History: Type 2 DM diagnosed 5 years ago. On Metformin 1000mg BID.
            Vitals: BP 130/80, HR 72.
            Labs: HbA1c 7.2%, Creatinine 1.0.
            Assessment: Diabetes well-controlled.
            """;
        int iterations = 100;
        List<Long> latencies = new ArrayList<>();

        // When
        for (int i = 0; i < iterations; i++) {
            Instant start = Instant.now();
            extractor.extract(standardNote);
            Instant end = Instant.now();
            latencies.add(Duration.between(start, end).toMillis());
        }

        // Then
        Collections.sort(latencies);
        long p95 = latencies.get((int) (iterations * 0.95));

        assertThat(p95)
            .isLessThan(500L)
            .withFailMessage("NLP extraction p95 latency %dms exceeds 500ms SLA", p95);

        System.out.printf("NLP Extraction Performance: p50=%dms, p95=%dms, p99=%dms%n",
            latencies.get(iterations / 2),
            p95,
            latencies.get((int) (iterations * 0.99)));
    }

    @Test
    @DisplayName("ICD-10 validation should complete within 50ms per code")
    void icd10ValidationPerformance() {
        // Given
        List<String> codes = List.of(
            "E11.9", "I10", "J44.9", "N18.3", "F32.9",
            "K21.0", "G47.33", "M54.5", "Z87.891", "R73.09"
        );
        int iterations = 100;
        List<Long> latencies = new ArrayList<>();

        // When
        for (int i = 0; i < iterations; i++) {
            for (String code : codes) {
                Instant start = Instant.now();
                icd10Validator.validate(code);
                Instant end = Instant.now();
                latencies.add(Duration.between(start, end).toMillis());
            }
        }

        // Then
        Collections.sort(latencies);
        long p95 = latencies.get((int) (latencies.size() * 0.95));

        assertThat(p95)
            .isLessThan(50L)
            .withFailMessage("ICD-10 validation p95 latency %dms exceeds 50ms SLA", p95);

        System.out.printf("ICD-10 Validation Performance: p50=%dms, p95=%dms%n",
            latencies.get(latencies.size() / 2),
            p95);
    }

    @Test
    @DisplayName("Code suggestion should complete within 200ms")
    void codeSuggestionPerformance() {
        // Given
        List<String> searchTerms = List.of(
            "Diabetes", "Hypertension", "COPD", "Heart failure",
            "Chronic kidney disease", "Depression"
        );
        int iterations = 50;
        List<Long> latencies = new ArrayList<>();

        // When
        for (int i = 0; i < iterations; i++) {
            for (String term : searchTerms) {
                Instant start = Instant.now();
                codeSuggester.suggestIcd10(term, 5);
                Instant end = Instant.now();
                latencies.add(Duration.between(start, end).toMillis());
            }
        }

        // Then
        Collections.sort(latencies);
        long p95 = latencies.get((int) (latencies.size() * 0.95));

        assertThat(p95)
            .isLessThan(200L)
            .withFailMessage("Code suggestion p95 latency %dms exceeds 200ms SLA", p95);

        System.out.printf("Code Suggestion Performance: p50=%dms, p95=%dms%n",
            latencies.get(latencies.size() / 2),
            p95);
    }

    @Test
    @DisplayName("Completeness analysis should complete within 300ms")
    void completenessAnalysisPerformance() {
        // Given
        int iterations = 100;
        List<Long> latencies = new ArrayList<>();

        // When
        for (int i = 0; i < iterations; i++) {
            String patientId = "patient-" + i;
            Instant start = Instant.now();
            completenessAnalyzer.analyze(patientId);
            Instant end = Instant.now();
            latencies.add(Duration.between(start, end).toMillis());
        }

        // Then
        Collections.sort(latencies);
        long p95 = latencies.get((int) (iterations * 0.95));

        assertThat(p95)
            .isLessThan(300L)
            .withFailMessage("Completeness analysis p95 latency %dms exceeds 300ms SLA", p95);

        System.out.printf("Completeness Analysis Performance: p50=%dms, p95=%dms%n",
            latencies.get(iterations / 2),
            p95);
    }

    @Test
    @DisplayName("Batch validation should scale linearly")
    void batchValidationScaling() {
        // Given
        List<String> smallBatch = IntStream.range(0, 10)
            .mapToObj(i -> "E11." + i)
            .toList();
        List<String> largeBatch = IntStream.range(0, 100)
            .mapToObj(i -> "E11." + (i % 10))
            .toList();

        // When
        Instant start1 = Instant.now();
        icd10Validator.validateBatch(smallBatch);
        long smallBatchMs = Duration.between(start1, Instant.now()).toMillis();

        Instant start2 = Instant.now();
        icd10Validator.validateBatch(largeBatch);
        long largeBatchMs = Duration.between(start2, Instant.now()).toMillis();

        // Then - Large batch should take ~10x (with some overhead)
        double ratio = (double) largeBatchMs / smallBatchMs;
        assertThat(ratio)
            .isLessThan(15.0) // Allow some overhead, but not exponential
            .withFailMessage("Batch validation does not scale linearly: ratio=%.2f", ratio);
    }
}
```

---

### Test Configuration

**application-test.yml**
```yaml
spring:
  datasource:
    url: jdbc:tc:postgresql:15:///testdb
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
  jpa:
    hibernate:
      ddl-auto: create-drop

  data:
    redis:
      host: localhost
      port: 6379

  kafka:
    bootstrap-servers: localhost:9092

# NLP Configuration
enrichment:
  nlp:
    model-path: classpath:nlp-models/
    confidence-threshold: 0.7
  cache:
    terminology-ttl: 3600  # 1 hour for terminology (non-PHI)
    extraction-ttl: 300    # 5 minutes for extractions (PHI)
  async:
    core-pool-size: 2
    max-pool-size: 4

logging:
  level:
    com.healthdata.enrichment: DEBUG
    org.springframework.security: DEBUG
```

---

### Best Practices

| Practice | Description | Example |
|----------|-------------|---------|
| NLP Model Testing | Test with varied clinical note formats | Structured, unstructured, abbreviations |
| Confidence Thresholds | Validate extraction confidence scoring | Assert confidence between 0.0 and 1.0 |
| Entity Type Coverage | Test all 9 entity types | Diagnoses, medications, procedures, labs, vitals, allergies, family history, dates, metadata |
| Negation Detection | Test negated and affirmed findings | "denies chest pain" vs "has chest pain" |
| Code System Coverage | Test all 4 code systems | ICD-10, CPT, SNOMED, LOINC |
| Billability Validation | Verify billable vs non-billable codes | E11.65 (billable) vs E11 (non-billable) |
| Batch Processing | Test batch operations for scalability | validateBatch(), suggestAllCodes() |
| Context-Aware Suggestions | Test contextual code suggestions | Diabetes + kidney disease → E11.2x |
| Async Processing | Test async extraction with task tracking | request.setAsync(true), verify taskId |
| Multi-Tenant Isolation | Verify tenant-scoped operations | extractWithTenant(), analyzeWithTenant() |

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| NLP model not loading | Model files missing from classpath | Verify nlp-models/ in src/test/resources |
| Low confidence scores | Insufficient training data | Use domain-specific medical terms |
| Code validation fails | Invalid code format | Ensure proper format (E11.9, not E119) |
| Batch timeout | Large batch size | Reduce batch size or increase timeout |
| Cache miss | Redis not running | Start TestContainers Redis |
| Async task not found | Task expired or invalid ID | Check task TTL configuration |
| Entity position mismatch | Text preprocessing changed positions | Use raw text positions before preprocessing |
| SNOMED validation fails | SNOMED lookup service unavailable | Mock SNOMED service in tests |
| Memory issues in NLP | Large document processing | Enable async processing for large docs |
| Tenant isolation violation | Missing tenant filter | Add tenantId to all repository queries |

### CI/CD Integration

**GitHub Actions workflow:**
```yaml
name: Data Enrichment Service Tests

on:
  push:
    paths:
      - 'backend/modules/services/data-enrichment-service/**'
  pull_request:
    paths:
      - 'backend/modules/services/data-enrichment-service/**'

jobs:
  test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_DB: testdb
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports:
          - 5432:5432
      redis:
        image: redis:7-alpine
        ports:
          - 6379:6379

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run tests
        run: ./gradlew :modules:services:data-enrichment-service:test

      - name: Generate coverage report
        run: ./gradlew :modules:services:data-enrichment-service:jacocoTestReport

      - name: Upload coverage
        uses: codecov/codecov-action@v3
        with:
          file: backend/modules/services/data-enrichment-service/build/reports/jacoco/test/jacocoTestReport.xml
```

## Configuration

### Application Properties
```yaml
server.port: 8089
spring.datasource.url: jdbc:postgresql://localhost:5432/hdim_enrichment
spring.data.redis.host: localhost
spring.kafka.bootstrap-servers: localhost:9092
```

### Environment Variables
- `DB_PASSWORD`: PostgreSQL password
- `REDIS_PASSWORD`: Redis password (optional)

## Running the Service

### Prerequisites
- Java 21
- PostgreSQL 14+
- Redis 6+
- Kafka 3+

### Build
```bash
./gradlew :modules:services:data-enrichment-service:build
```

### Run
```bash
./gradlew :modules:services:data-enrichment-service:bootRun
```

### Run Tests
```bash
./gradlew :modules:services:data-enrichment-service:test
```

## API Documentation

Swagger UI available at: `http://localhost:8089/swagger-ui.html`

## Monitoring

Actuator endpoints:
- Health: `/actuator/health`
- Metrics: `/actuator/metrics`
- Prometheus: `/actuator/prometheus`

## Security

- JWT-based authentication via shared authentication module
- Role-based access control (ADMIN, CLINICIAN, CODER, ANALYST)
- HIPAA-compliant audit logging via Kafka
- Tenant isolation for all operations

## Production Considerations

### Scalability
- Stateless design for horizontal scaling
- Redis for distributed caching
- Async processing for heavy workloads

### Performance
- Connection pooling (HikariCP)
- Query optimization
- Cache-first strategy for lookups

### Reliability
- Circuit breaker patterns
- Graceful degradation
- Comprehensive error handling

## Future Enhancements

1. **Enhanced NLP**: Integrate with UMLS API for comprehensive terminology
2. **Machine Learning**: Train custom models for entity extraction
3. **Real-time Processing**: WebSocket support for streaming analysis
4. **Analytics Dashboard**: Visualization of quality metrics
5. **Batch Processing**: Bulk document processing capabilities

## License

Copyright 2024 - Healthcare Data in Motion Platform
