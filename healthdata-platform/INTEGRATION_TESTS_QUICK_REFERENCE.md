# Integration Tests Quick Reference Guide

## 📁 Test File Locations

```
healthdata-platform/
└── src/test/java/com/healthdata/
    ├── contracts/
    │   └── PatientApiContractTest.java         (721 lines, 34 tests)
    ├── integration/
    │   ├── QualityMeasureIntegrationTest.java  (753 lines, 28 tests)
    │   ├── CareGapDetectionIntegrationTest.java (778 lines, 25 tests)
    │   ├── FhirResourceIntegrationTest.java    (469 lines, 21 tests)
    │   └── DatabaseIntegrationTest.java        (520 lines, 18 tests)
    └── test/
        ├── TestDataBuilder.java                (546 lines)
        └── ExternalServiceMocks.java           (356 lines)
```

**Total**: 4,143 lines of test code, 126 test methods, 32 nested test classes

---

## 🚀 Quick Start

### Run All Tests
```bash
./gradlew test
```

### Run Specific Test Suite
```bash
# API Contract Tests
./gradlew test --tests "PatientApiContractTest"

# Quality Measure Tests
./gradlew test --tests "QualityMeasureIntegrationTest"

# Care Gap Tests
./gradlew test --tests "CareGapDetectionIntegrationTest"

# FHIR Tests
./gradlew test --tests "FhirResourceIntegrationTest"

# Database Tests
./gradlew test --tests "DatabaseIntegrationTest"
```

### Run Integration Tests Only
```bash
./gradlew test --tests "*IntegrationTest"
```

### Run Contract Tests Only
```bash
./gradlew test --tests "*ContractTest"
```

---

## 🧪 Test Data Builder Usage

### Create a Healthy Patient
```java
Patient patient = testDataBuilder
    .aHealthyPatient()
    .firstName("John")
    .lastName("Doe")
    .age(50)
    .build();
```

### Create a Diabetic Patient with Lab Results
```java
Patient patient = testDataBuilder
    .aPatientWithDiabetes()
    .age(60)
    .withHbA1c(6.8)
    .withBloodPressure(130, 80)
    .withCholesterol(200)
    .build();
```

### Create a High-Risk Patient
```java
Patient patient = testDataBuilder
    .aHighRiskPatient()
    .age(75)
    .withHbA1c(8.2)
    .withBloodPressure(155, 95)
    .withMetformin()
    .withLisinopril()
    .build();
```

### Create a Patient Needing Screening
```java
Patient patient = testDataBuilder
    .aFemalePatientForScreening()
    .age(55)
    .withMammogram()      // Recent screening
    .withColonoscopy()    // Recent screening
    .build();
```

---

## 📊 Test Coverage Summary

### API Contract Tests (34 tests)
- ✅ Create Patient (9 tests)
- ✅ Get Patient (5 tests)
- ✅ Update Patient (5 tests)
- ✅ List Patients (9 tests)
- ✅ Delete Patient (3 tests)
- ✅ Error Handling (3 tests)

### Quality Measure Tests (28 tests)
- ✅ HbA1c Control (6 tests)
- ✅ Blood Pressure Control (5 tests)
- ✅ Medication Adherence (3 tests)
- ✅ Preventive Screening (6 tests)
- ✅ Batch Calculation (3 tests)
- ✅ Measure Accuracy (3 tests)
- ✅ Performance Benchmarks (2 tests)

### Care Gap Detection Tests (25 tests)
- ✅ Preventive Care Gaps (5 tests)
- ✅ Chronic Disease Gaps (6 tests)
- ✅ Medication Adherence Gaps (2 tests)
- ✅ Gap Prioritization (4 tests)
- ✅ Batch Gap Detection (3 tests)
- ✅ Multi-Tenant Isolation (1 test)
- ✅ Gap Closure Tracking (2 tests)
- ✅ Performance Benchmarks (2 tests)

### FHIR Integration Tests (21 tests)
- ✅ Resource Validation (4 tests)
- ✅ Code System Mapping (6 tests)
- ✅ Resource Transformation (3 tests)
- ✅ Bundle Processing (2 tests)
- ✅ Terminology Validation (4 tests)
- ✅ Performance Tests (2 tests)

### Database Integration Tests (18 tests)
- ✅ Patient Repository (6 tests)
- ✅ Observation Repository (5 tests)
- ✅ Condition Repository (3 tests)
- ✅ Quality Measure Result Repository (3 tests)
- ✅ Transaction Management (1 test)

---

## 🎯 Common Test Patterns

### API Contract Testing
```java
@Test
@DisplayName("Valid request returns 201 Created")
void createPatient_ValidRequest_Returns201() throws Exception {
    CreatePatientRequest request = CreatePatientRequest.builder()
        .firstName("John")
        .lastName("Doe")
        .mrn("MRN001")
        .build();

    mockMvc.perform(post("/api/patients")
        .contentType(APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists());
}
```

### Quality Measure Testing
```java
@Test
@DisplayName("Compliant patient passes measure")
void calculateMeasure_CompliantPatient_ReturnsPass() {
    Patient patient = createPatientWithDiabetes("John", "Doe");
    createHbA1cObservation(patient.getId(), 6.5, LocalDate.now().minusDays(30));

    MeasureResult result = measureService.calculateMeasure(patient.getId(), "hba1c-control");

    assertTrue(result.isCompliant());
    assertThat(result.getValue()).isEqualTo(6.5);
}
```

### Care Gap Detection Testing
```java
@Test
@DisplayName("Detects screening gap for overdue patient")
void detectGaps_OverdueScreening_DetectsGap() {
    Patient patient = createFemalePatient("Jane", "Doe", 55);

    List<CareGap> gaps = gapEngine.detectGapsForPatient(patient.getId());

    Optional<CareGap> screeningGap = gaps.stream()
        .filter(g -> g.getGapType() == GapType.CANCER_SCREENING)
        .findFirst();

    assertTrue(screeningGap.isPresent());
    assertThat(screeningGap.get().getPriority()).isEqualTo("HIGH");
}
```

### Database Testing
```java
@Test
@DisplayName("Batch insert performs efficiently")
void batchInsert_100Patients_EfficientPerformance() {
    List<Patient> patients = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
        patients.add(createPatientBuilder().build());
    }

    long startTime = System.currentTimeMillis();
    patientRepository.saveAll(patients);
    entityManager.flush();
    long duration = System.currentTimeMillis() - startTime;

    assertThat(duration).isLessThan(3000); // < 3 seconds
}
```

---

## 📈 Performance Benchmarks

| Operation | Target | Status |
|-----------|--------|--------|
| Single patient measure calculation | < 100ms | ✅ Pass |
| Batch 100 patients calculation | < 10s | ✅ Pass |
| Batch 500 patients calculation | < 60s | ✅ Pass |
| Single patient gap detection | < 200ms | ✅ Pass |
| Batch 100 patients gap detection | < 30s | ✅ Pass |
| Batch 500 patients gap detection | < 60s | ✅ Pass |
| Database batch 100 patients | < 3s | ✅ Pass |
| Database batch 1000 observations | < 10s | ✅ Pass |
| FHIR transform 100 resources | < 1s | ✅ Pass |
| FHIR validate 100 resources | < 2s | ✅ Pass |

---

## 🔧 Helper Methods Reference

### Patient Helpers
```java
// Create basic patient
Patient createPatient(String firstName, String lastName, String mrn, String tenantId)

// Create patient with diabetes
Patient createPatientWithDiabetes(String firstName, String lastName, int age)

// Create patient with hypertension
Patient createPatientWithHypertension(String firstName, String lastName, int age)

// Create patient with multiple conditions
Patient createPatientWithMultipleConditions(String firstName, String lastName, int age)
```

### Observation Helpers
```java
// Create HbA1c observation
void createHbA1cObservation(String patientId, double value, LocalDate effectiveDate)

// Create blood pressure observation
void createBloodPressureObservation(String patientId, int systolic, int diastolic, LocalDate effectiveDate)

// Create screening observation
void createMammogramObservation(String patientId, LocalDate effectiveDate)
void createColonoscopyObservation(String patientId, LocalDate effectiveDate)
```

### Condition Helpers
```java
// Create condition
Condition createCondition(String patientId, String code, String display, String status)
```

---

## 🧩 Code System Reference

### LOINC Codes (Observations)
- `4548-4` - Hemoglobin A1c
- `8480-6` - Systolic Blood Pressure
- `8462-4` - Diastolic Blood Pressure
- `2093-3` - Total Cholesterol
- `39156-5` - Body Mass Index (BMI)
- `24606-6` - Mammogram
- `73761-0` - Colonoscopy

### ICD-10 Codes (Conditions)
- `E11` - Type 2 Diabetes Mellitus
- `I10` - Essential Hypertension
- `I50` - Heart Failure
- `J44` - Chronic Obstructive Pulmonary Disease

### RxNorm Codes (Medications)
- `6809` - Metformin
- `29046` - Lisinopril
- `1191` - Aspirin

---

## 🏗️ Test Structure

### Recommended Test Organization
```java
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class MyIntegrationTest extends BaseIntegrationTest {

    @Nested
    @DisplayName("Feature Category")
    class FeatureTests {

        @Test
        @DisplayName("Specific scenario description")
        void testMethod_Scenario_ExpectedOutcome() {
            // Arrange
            // ... setup test data

            // Act
            // ... execute the operation

            // Assert
            // ... verify the results
        }
    }
}
```

---

## 🎓 Best Practices

### 1. Use Descriptive Test Names
```java
// ✅ Good
void createPatient_DuplicateMrn_Returns409()

// ❌ Bad
void testCreatePatient()
```

### 2. Use Test Data Builder
```java
// ✅ Good - Readable and maintainable
Patient patient = testDataBuilder
    .aPatientWithDiabetes()
    .age(60)
    .withHbA1c(6.8)
    .build();

// ❌ Bad - Verbose and hard to read
Patient patient = new Patient();
patient.setFirstName("John");
patient.setLastName("Doe");
// ... 20 more lines
```

### 3. Use @Nested for Organization
```java
@Nested
@DisplayName("Create Patient Tests")
class CreatePatientTests {
    // All create-related tests here
}
```

### 4. Use @DisplayName for Clarity
```java
@DisplayName("Valid request returns 201 Created with patient object")
```

### 5. Follow AAA Pattern
```java
// Arrange
Patient patient = createTestPatient();

// Act
MeasureResult result = measureService.calculate(patient.getId());

// Assert
assertTrue(result.isCompliant());
```

---

## 🔍 Debugging Tests

### Enable SQL Logging
```yaml
# application-test.yml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

### Run Single Test
```bash
./gradlew test --tests "PatientApiContractTest.CreatePatientContract.createPatient_ValidRequest_Returns201"
```

### Debug Mode
```bash
./gradlew test --debug-jvm --tests "PatientApiContractTest"
```

---

## 📝 Maintenance

### Adding New Tests
1. Follow existing naming conventions
2. Use @Nested for organization
3. Add @DisplayName for readability
4. Include in appropriate test suite
5. Update this documentation

### Updating Test Data
- Modify `TestDataBuilder.java` for new scenarios
- Update helper methods for new data types
- Keep LOINC/ICD-10 codes current

### Performance Regression
- Run benchmarks regularly
- Alert on degradation > 10%
- Document performance changes

---

## 📚 Additional Resources

### Documentation
- Full documentation: `TDD_AGENT_5B_INTEGRATION_TESTS_COMPLETE.md`
- Test implementation: `TEST_IMPLEMENTATION_SUMMARY.md`

### Standards
- FHIR R4: https://www.hl7.org/fhir/
- LOINC: https://loinc.org/
- SNOMED CT: https://www.snomed.org/
- ICD-10: https://www.who.int/classifications/icd/

### Testing Frameworks
- JUnit 5: https://junit.org/junit5/
- AssertJ: https://assertj.github.io/doc/
- Spring Boot Test: https://spring.io/guides/gs/testing-web/

---

**Last Updated**: 2025-12-01
**Version**: 1.0
**Maintained by**: TDD Swarm Agent 5B
