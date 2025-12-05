# FHIR Resource Controller Test - Code Samples

## Sample Test Method 1: Basic Observation Retrieval

```java
@Test
@DisplayName("Should retrieve all observations for patient successfully")
void testGetObservationsSuccess() throws Exception {
    // Arrange
    List<Observation> observations = Arrays.asList(
            glucoseObservation,
            hba1cObservation,
            bloodPressureObservation,
            heartRateObservation
    );
    when(fhirService.getObservationsForPatient(validPatientId))
            .thenReturn(observations);

    // Act
    MvcResult result = performGet("/api/fhir/observations/" + validPatientId);

    // Assert
    assertOkStatus(result);
    assertJsonContentType(result);
    String content = getResponseContent(result);
    assertNotNull(content);
    assertTrue(content.contains("2345-7") || content.contains("glucose"));
    verify(fhirService, times(1)).getObservationsForPatient(validPatientId);
}
```

**Purpose**: Verifies successful retrieval of multiple observations with proper HTTP status and JSON content type.

---

## Sample Test Method 2: Condition Validation with SNOMED Codes

```java
@Test
@DisplayName("Should retrieve conditions with correct SNOMED codes")
void testGetConditionsWithSnomedCodes() throws Exception {
    // Arrange
    List<Condition> conditions = Arrays.asList(
            diabetesCondition,      // SNOMED: 44054006
            hypertensionCondition,  // SNOMED: 38341003
            copd_condition          // SNOMED: 13645005
    );
    when(fhirService.getConditionsForPatient(validPatientId))
            .thenReturn(conditions);

    // Act
    MvcResult result = performGet("/api/fhir/conditions/" + validPatientId);

    // Assert
    assertOkStatus(result);
    String content = getResponseContent(result);
    assertTrue(content.contains("44054006") || content.contains("38341003") || content.contains("13645005"));
    verify(fhirService).getConditionsForPatient(validPatientId);
}
```

**Purpose**: Tests that SNOMED CT condition codes are properly included in the response.

---

## Sample Test Method 3: Medication Dosage Validation

```java
@Test
@DisplayName("Should include dosage information")
void testGetMedicationsIncludesDosage() throws Exception {
    // Arrange
    List<MedicationRequest> medications = Arrays.asList(metforminRequest);
    when(fhirService.getMedicationsForPatient(validPatientId))
            .thenReturn(medications);

    // Act
    MvcResult result = performGet("/api/fhir/medications/" + validPatientId);

    // Assert
    assertOkStatus(result);
    String content = getResponseContent(result);
    assertTrue(content.contains("500") || content.contains("mg") || content.contains("dosage"));
    verify(fhirService).getMedicationsForPatient(validPatientId);
}
```

**Purpose**: Validates that medication dosage information is properly returned.

---

## Sample Test Method 4: Error Handling with Service Exception

```java
@Test
@DisplayName("Should handle service exception for observations")
void testGetObservationsServiceException() throws Exception {
    // Arrange
    when(fhirService.getObservationsForPatient(validPatientId))
            .thenThrow(new RuntimeException("Database connection failed"));

    // Act
    MvcResult result = performGet("/api/fhir/observations/" + validPatientId);

    // Assert
    assertServerErrorResponse(result);
}
```

**Purpose**: Verifies that service exceptions are properly handled and return 500 errors.

---

## Sample Test Method 5: Empty Results Handling

```java
@Test
@DisplayName("Should return empty list when patient has no observations")
void testGetObservationsEmptyList() throws Exception {
    // Arrange
    when(fhirService.getObservationsForPatient(validPatientId))
            .thenReturn(List.of());

    // Act
    MvcResult result = performGet("/api/fhir/observations/" + validPatientId);

    // Assert
    assertOkStatus(result);
    assertJsonContentType(result);
    verify(fhirService, times(1)).getObservationsForPatient(validPatientId);
}
```

**Purpose**: Tests that empty result sets are handled correctly (HTTP 200 with empty content).

---

## Sample Test Data Setup

```java
@BeforeEach
public void setUp() {
    super.setUp();
    initializeTestData();
}

private void initializeTestData() {
    validPatientId = "patient-diabetes-123";
    nonExistentPatientId = "patient-nonexistent-999";
    validTenantId = "tenant-001";

    // Glucose observation (LOINC: 2345-7)
    glucoseObservation = Observation.builder()
            .id("obs-glucose-001")
            .patientId(validPatientId)
            .code("2345-7")
            .system("http://loinc.org")
            .display("Glucose [Mass/volume] in Serum or Plasma")
            .valueQuantity(new BigDecimal("185"))
            .valueUnit("mg/dL")
            .status("final")
            .category("laboratory")
            .effectiveDate(LocalDateTime.now().minusDays(2))
            .tenantId(validTenantId)
            .build();

    // Type 2 Diabetes condition (SNOMED: 44054006)
    diabetesCondition = Condition.builder()
            .id("cond-diabetes-001")
            .patientId(validPatientId)
            .code("44054006")
            .display("Type 2 diabetes mellitus")
            .clinicalStatus("active")
            .verificationStatus("confirmed")
            .category("problem-list-item")
            .severity("moderate")
            .onsetDate(LocalDateTime.now().minusYears(5))
            .recordedDate(LocalDateTime.now().minusYears(5))
            .tenantId(validTenantId)
            .build();

    // Metformin medication (RxNorm: 6809)
    metforminRequest = MedicationRequest.builder()
            .id("med-metformin-001")
            .patientId(validPatientId)
            .medicationCode("6809")
            .medicationDisplay("Metformin")
            .status("active")
            .intent("order")
            .priority("routine")
            .dosageQuantity(500.0)
            .dosageUnit("mg")
            .dosageTiming("BID")
            .refillsRemaining(10)
            .tenantId(validTenantId)
            .build();
}
```

---

## Sample Test: Multiple Chronic Conditions

```java
@Test
@DisplayName("Should handle patient with multiple chronic conditions")
void testGetConditionsMultipleChronic() throws Exception {
    // Arrange
    List<Condition> conditions = Arrays.asList(
            diabetesCondition,
            hypertensionCondition,
            copd_condition
    );
    when(fhirService.getConditionsForPatient(validPatientId))
            .thenReturn(conditions);

    // Act
    MvcResult result = performGet("/api/fhir/conditions/" + validPatientId);

    // Assert
    assertOkStatus(result);
    String content = getResponseContent(result);
    assertEquals(3, countConditionCodesInResponse(content));
    verify(fhirService).getConditionsForPatient(validPatientId);
}
```

---

## Sample Test: Response Content Validation

```java
@Test
@DisplayName("Should return observations with complete FHIR resource data")
void testObservationsCompleteData() throws Exception {
    // Arrange
    when(fhirService.getObservationsForPatient(validPatientId))
            .thenReturn(Arrays.asList(glucoseObservation));

    // Act
    MvcResult result = performGet("/api/fhir/observations/" + validPatientId);

    // Assert
    assertOkStatus(result);
    assertJsonFieldExists(result, "code");
    assertJsonFieldExists(result, "valueQuantity");
    assertJsonFieldExists(result, "status");
    assertJsonFieldExists(result, "effectiveDate");
}
```

---

## Medical Code Constants Reference

### LOINC Codes Used in Tests
```java
private static final String LOINC_GLUCOSE = "2345-7";           // mg/dL
private static final String LOINC_HBA1C = "4548-4";              // %
private static final String LOINC_BLOOD_PRESSURE = "85354-9";    // mmHg
private static final String LOINC_HEART_RATE = "8867-4";         // beats/min
```

### SNOMED Codes Used in Tests
```java
private static final String SNOMED_DIABETES = "44054006";   // Type 2 DM
private static final String SNOMED_HYPERTENSION = "38341003"; // HTN
private static final String SNOMED_COPD = "13645005";        // COPD
```

### RxNorm Codes Used in Tests
```java
private static final String RXNORM_METFORMIN = "6809";      // 500mg BID
private static final String RXNORM_LISINOPRIL = "9471";      // 10mg daily
private static final String RXNORM_ALBUTEROL = "435";        // 90mcg PRN
```

---

## Helper Methods

```java
/**
 * Count the number of condition codes found in the response
 */
private int countConditionCodesInResponse(String responseContent) {
    int count = 0;
    if (responseContent.contains("44054006")) count++;
    if (responseContent.contains("38341003")) count++;
    if (responseContent.contains("13645005")) count++;
    return count;
}

/**
 * Count the number of medication codes found in the response
 */
private int countMedicationCodesInResponse(String responseContent) {
    int count = 0;
    if (responseContent.contains("6809")) count++;
    if (responseContent.contains("9471")) count++;
    if (responseContent.contains("435")) count++;
    return count;
}
```

---

## Key Testing Patterns

### Pattern 1: Testing with Mocked Service
```java
// Setup mock
when(fhirService.getObservationsForPatient(validPatientId))
    .thenReturn(testData);

// Execute
MvcResult result = performGet("/api/fhir/observations/" + validPatientId);

// Verify
verify(fhirService, times(1)).getObservationsForPatient(validPatientId);
```

### Pattern 2: Testing Error Conditions
```java
// Setup exception
when(fhirService.getObservationsForPatient(validPatientId))
    .thenThrow(new RuntimeException("Error"));

// Execute
MvcResult result = performGet("/api/fhir/observations/" + validPatientId);

// Assert error response
assertServerErrorResponse(result);
```

### Pattern 3: Testing Response Content
```java
// Setup
when(fhirService.getObservationsForPatient(validPatientId))
    .thenReturn(testData);

// Execute
MvcResult result = performGet("/api/fhir/observations/" + validPatientId);

// Assert response structure
assertJsonFieldExists(result, "code");
assertJsonFieldExists(result, "valueQuantity");
```

---

## Assertion Quick Reference

| Assertion | Purpose |
|-----------|---------|
| `assertOkStatus(result)` | Verify HTTP 200 |
| `assertJsonContentType(result)` | Verify JSON content type |
| `assertJsonFieldExists(result, field)` | Field must exist |
| `assertJsonFieldValue(result, field, value)` | Field value must match |
| `verify(service).method()` | Service method was called |
| `verify(service, times(1)).method()` | Called exactly once |

