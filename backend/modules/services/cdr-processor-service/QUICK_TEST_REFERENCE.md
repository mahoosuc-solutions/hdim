# Quick Test Reference - CDR Processor Service

## Test Execution Commands

### Run All Tests
```bash
cd /home/mahoosuc-solutions/projects/hdim-master/hdim-master/backend
./gradlew :modules:services:cdr-processor-service:test
```

### Run Individual Test Classes
```bash
# Parser tests (23 tests)
./gradlew test --tests "Hl7v2ParserServiceTest"

# ADT handler tests (16 tests)
./gradlew test --tests "AdtMessageHandlerTest"

# ORU handler tests (17 tests)
./gradlew test --tests "OruMessageHandlerTest"

# Converter tests (26 tests)
./gradlew test --tests "Hl7ToFhirConverterTest"
```

### Run Specific Test Methods
```bash
# Example: Run just the ADT^A01 parsing test
./gradlew test --tests "Hl7v2ParserServiceTest.testParseAdtA01AdmitMessage"

# Example: Run just patient creation test
./gradlew test --tests "AdtMessageHandlerTest.testCreatePatientFromAdtA01"
```

### Run with Coverage
```bash
./gradlew :modules:services:cdr-processor-service:test jacocoTestReport
```

## Test File Locations

| Test Class | Location | Tests |
|------------|----------|-------|
| Hl7v2ParserServiceTest | `src/test/java/com/healthdata/cdr/parser/` | 23 |
| AdtMessageHandlerTest | `src/test/java/com/healthdata/cdr/handler/` | 16 |
| OruMessageHandlerTest | `src/test/java/com/healthdata/cdr/handler/` | 17 |
| Hl7ToFhirConverterTest | `src/test/java/com/healthdata/cdr/converter/` | 26 |

## Test Categories Quick Reference

### Parser Tests (23)
- Message type parsing (ADT, ORU, ORM)
- Segment extraction (PID, PV1, OBX)
- Error handling (null, empty, malformed)
- Metadata extraction (control ID, timestamp, version)

### ADT Handler Tests (16)
- Patient resource creation
- Encounter creation/updates
- Transfer handling
- Discharge processing
- Insurance extraction
- Emergency admits

### ORU Handler Tests (17)
- Observation creation
- Multiple observations
- Numeric vs text results
- Result status codes
- DiagnosticReport creation
- Abnormal flags

### Converter Tests (26)
- PID → Patient
- PV1 → Encounter
- OBX → Observation
- OBR → DiagnosticReport
- ORC → ServiceRequest
- IN1 → Coverage
- Date/time conversions
- Code system mappings

## Sample Messages

See: `src/test/resources/sample-messages.txt`

Quick samples:
```
ADT^A01: Patient admit with insurance
ADT^A02: Patient transfer
ADT^A03: Patient discharge
ADT^A08: Update patient info
ORU^R01: Lab results (BMP)
ORM^O01: Lab order
```

## Expected Test Results

### If Implementation is Complete:
- All 82 tests should PASS
- Coverage should be >80%

### If Implementation is Incomplete:
- Tests will FAIL with specific error messages
- Use failures to guide implementation

## Common Test Patterns

### All tests follow AAA pattern:
```java
@Test
void testSomething() {
    // Arrange (Given)
    // ... setup test data

    // Act (When)
    // ... call method under test

    // Assert (Then)
    // ... verify results
}
```

### Mocking example:
```java
@Mock
private PID mockPid;

when(mockPid.getPatientName(0).getFamilyName().getValue())
    .thenReturn("DOE");
```

## Verification Checklist

After running tests, verify:
- [ ] All 82 tests pass
- [ ] No compilation errors
- [ ] Coverage reports generated
- [ ] No warnings in logs
- [ ] All message types handled
- [ ] All segments extracted correctly
- [ ] FHIR resources created properly

## Troubleshooting

### Tests won't compile?
- Check HAPI HL7v2 dependency
- Check HAPI FHIR dependency
- Check JUnit 5 and Mockito dependencies

### Tests fail with NullPointerException?
- Check if implementation classes exist
- Check method signatures match test expectations
- Check mocking configuration

### Tests fail with wrong values?
- Review HL7 segment parsing logic
- Check FHIR resource mapping
- Verify date/time conversion

## Dependencies Required

```kotlin
// HAPI HL7v2
implementation("ca.uhn.hapi:hapi-structures-v25:2.5.1")
implementation("ca.uhn.hapi:hapi-base:2.5.1")

// HAPI FHIR
implementation("ca.uhn.hapi.fhir:hapi-fhir-structures-r4:6.8.0")
implementation("ca.uhn.hapi.fhir:hapi-fhir-base:6.8.0")

// Testing
testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
testImplementation("org.mockito:mockito-core:5.5.0")
testImplementation("org.mockito:mockito-junit-jupiter:5.5.0")
```

## Quick Stats

- **Total Tests:** 82
- **Test Files:** 4
- **Message Types Covered:** 6 (A01, A02, A03, A08, R01, O01)
- **FHIR Resources Tested:** 6 (Patient, Encounter, Observation, DiagnosticReport, ServiceRequest, Coverage)
- **Error Scenarios:** 15+
- **Edge Cases:** 10+

---

**Quick Reference Card - CDR Processor TDD Tests**
**Total: 82 Tests | 4 Files | 100% Ready**
