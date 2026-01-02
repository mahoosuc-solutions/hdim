# FHIR Resource Controller Test - Quick Reference

## File Location
`src/test/java/com/healthdata/api/FhirResourceControllerTest.java`

## Quick Stats
| Metric | Value |
|--------|-------|
| Total Lines | 1,026 |
| File Size | 44 KB |
| Total Test Methods | 38 |
| Test Classes (Nested) | 5 |
| Compilation Status | SUCCESS |
| Framework | JUnit 5 |

## Test Method Count by Category

| Category | Count | Methods |
|----------|-------|---------|
| Observations | 9 | GET /api/fhir/observations/{patientId} tests |
| Conditions | 8 | GET /api/fhir/conditions/{patientId} tests |
| Medications | 10 | GET /api/fhir/medications/{patientId} tests |
| Error Handling | 6 | Exception and edge case handling |
| Response Format | 5 | Content type and format validation |

## Medical Codes Reference

### Observations (LOINC)
```
2345-7   → Glucose in Serum or Plasma (laboratory)
4548-4   → Hemoglobin A1c in Blood (laboratory)
85354-9  → Blood Pressure Panel (vital-signs)
8867-4   → Heart Rate (vital-signs)
```

### Conditions (SNOMED)
```
44054006 → Type 2 Diabetes Mellitus (active)
38341003 → Hypertension (active)
13645005 → COPD (active)
```

### Medications (RxNorm)
```
6809 → Metformin (500mg BID)
9471 → Lisinopril (10mg daily)
435  → Albuterol (90mcg as-needed)
```

## Test Endpoints

| Method | Endpoint | Tests |
|--------|----------|-------|
| GET | /api/fhir/observations/{patientId} | 9 |
| GET | /api/fhir/conditions/{patientId} | 8 |
| GET | /api/fhir/medications/{patientId} | 10 |

## Sample Test IDs

```
validPatientId: "patient-diabetes-123"
nonExistentPatientId: "patient-nonexistent-999"
validTenantId: "tenant-001"
```

## Test Data Available

### Observation Objects
- `glucoseObservation` - 185 mg/dL
- `hba1cObservation` - 8.5%
- `bloodPressureObservation` - 140/90
- `heartRateObservation` - 78 bpm

### Condition Objects
- `diabetesCondition` - Type 2 Diabetes (active, confirmed)
- `hypertensionCondition` - Hypertension (active, confirmed)
- `copd_condition` - COPD (active, confirmed)

### Medication Objects
- `metforminRequest` - 500mg BID, 10 refills
- `lisinoprilRequest` - 10mg daily, 11 refills
- `albuterolRequest` - 90mcg PRN, 5 refills

## Key Assertions Used

```java
// Status assertions
assertOkStatus(result);              // HTTP 200
assertServerErrorResponse(result);   // HTTP 5xx
assertClientErrorResponse(result);   // HTTP 4xx

// Content assertions
assertJsonContentType(result);       // Validates JSON
assertJsonFieldExists(result, "field");      // Field present
assertJsonFieldValue(result, "field", "value"); // Field value

// Service mocking
when(fhirService.getObservationsForPatient(patientId))
    .thenReturn(observationList);
verify(fhirService, times(1)).getObservationsForPatient(patientId);
```

## Test Nested Classes

### 1. GetObservationsTests (9)
- ✓ Successful retrieval
- ✓ Empty list handling
- ✓ Patient not found
- ✓ LOINC code validation
- ✓ Vital signs category
- ✓ Laboratory category
- ✓ Values and units
- ✓ Effective dates
- ✓ Status field

### 2. GetConditionsTests (8)
- ✓ Successful retrieval
- ✓ Empty list handling
- ✓ SNOMED code validation
- ✓ Clinical status
- ✓ Verification status
- ✓ Severity information
- ✓ Date fields (onset, recorded)
- ✓ Multiple chronic conditions

### 3. GetMedicationsTests (10)
- ✓ Successful retrieval
- ✓ Empty list handling
- ✓ RxNorm code validation
- ✓ Medication status
- ✓ Dosage information
- ✓ Timing information
- ✓ Refill counts
- ✓ Valid period dates
- ✓ Multiple active medications
- ✓ Complete medication data

### 4. ErrorHandlingTests (6)
- ✓ Service exceptions
- ✓ Null patient IDs
- ✓ Error responses

### 5. ResponseFormatTests (5)
- ✓ JSON content type
- ✓ Complete data objects
- ✓ All endpoints

## How to Run Tests

```bash
# Compile test code
./gradlew compileTestJava

# Run all FHIR tests
./gradlew test --tests FhirResourceControllerTest

# Run specific nested class
./gradlew test --tests FhirResourceControllerTest\$GetObservationsTests

# Run with detailed output
./gradlew test --tests FhirResourceControllerTest -i

# Run specific test method
./gradlew test --tests FhirResourceControllerTest.testGetObservationsSuccess
```

## Spring Test Base Class

Extends: `BaseWebControllerTest`

Provides:
- `performGet(path)` - GET request
- `performPost(path, body)` - POST request
- `performPut(path, body)` - PUT request
- `getResponseContent(result)` - Extract response
- `getStatusCode(result)` - HTTP status
- `toJson(object)` - Serialize to JSON

## Medical Data Characteristics

**Patient Profile:**
- Age: ~38 years old (DOB pattern)
- Diagnoses: Type 2 Diabetes, Hypertension, COPD
- Recent Lab Work: Glucose elevated (185), HbA1c 8.5% (suboptimal control)
- Vital Signs: Elevated BP (140/90), normal HR (78)
- Active Medications: 3 (diabetes, hypertension, respiratory)
- Multi-tenant: Isolated to tenant-001

## Test Utilities

Helper methods for test support:
```java
countConditionCodesInResponse(String content)  // Count SNOMED codes
countMedicationCodesInResponse(String content) // Count RxNorm codes
```

## Import Statements Included

```java
// Core Testing
import com.healthdata.BaseWebControllerTest;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MvcResult;

// FHIR Domain Models
import com.healthdata.fhir.domain.*;
import com.healthdata.fhir.service.FhirService;

// Mocking & Assertions
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Java Utilities
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
```

## Next Steps for Implementation

1. Create `FhirResourceController` class
2. Implement the three endpoints:
   - `GET /api/fhir/observations/{patientId}`
   - `GET /api/fhir/conditions/{patientId}`
   - `GET /api/fhir/medications/{patientId}`
3. Wire FhirService into controller
4. Run tests to verify implementation
5. Add error handling as needed

## Documentation References

- LOINC: https://loinc.org/
- SNOMED CT: https://www.snomed.org/
- RxNorm: https://www.nlm.nih.gov/research/umls/rxnorm/

## Test Philosophy

- **Arrange-Act-Assert**: Clear test structure
- **Realistic Data**: Uses actual medical codes and values
- **Comprehensive Coverage**: Tests happy paths and error cases
- **Isolation**: Mocks external dependencies
- **Maintainability**: Clear naming and documentation
