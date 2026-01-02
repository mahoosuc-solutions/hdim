# CDR Processor Service - Test Suite Index

## Quick Links

- [TEST_REPORT.md](TEST_REPORT.md) - Comprehensive test documentation and analysis
- [QUICK_TEST_REFERENCE.md](QUICK_TEST_REFERENCE.md) - Quick command reference
- [src/test/TEST_SUMMARY.md](src/test/TEST_SUMMARY.md) - Detailed test summary
- [src/test/resources/sample-messages.txt](src/test/resources/sample-messages.txt) - Sample HL7 messages

## Test Files

### 1. Parser Tests
**File:** [src/test/java/com/healthdata/cdr/parser/Hl7v2ParserServiceTest.java](src/test/java/com/healthdata/cdr/parser/Hl7v2ParserServiceTest.java)
- **Tests:** 23
- **Focus:** HL7 v2 message parsing and segment extraction
- **Key Tests:**
  - Parse ADT messages (A01, A02, A03, A08)
  - Parse ORU messages (R01)
  - Parse ORM messages (O01)
  - Extract PID, PV1, OBX segments
  - Handle errors and edge cases

### 2. ADT Handler Tests
**File:** [src/test/java/com/healthdata/cdr/handler/AdtMessageHandlerTest.java](src/test/java/com/healthdata/cdr/handler/AdtMessageHandlerTest.java)
- **Tests:** 16
- **Focus:** ADT message processing and FHIR resource creation
- **Key Tests:**
  - Create Patient from ADT
  - Create/update Encounter
  - Handle admit/transfer/discharge
  - Extract insurance (IN1)
  - Handle emergency admits

### 3. ORU Handler Tests
**File:** [src/test/java/com/healthdata/cdr/handler/OruMessageHandlerTest.java](src/test/java/com/healthdata/cdr/handler/OruMessageHandlerTest.java)
- **Tests:** 17
- **Focus:** Lab result processing and Observation creation
- **Key Tests:**
  - Create Observations from OBX
  - Handle multiple observations
  - Parse numeric/text results
  - Handle status codes
  - Create DiagnosticReport

### 4. HL7 to FHIR Converter Tests
**File:** [src/test/java/com/healthdata/cdr/converter/Hl7ToFhirConverterTest.java](src/test/java/com/healthdata/cdr/converter/Hl7ToFhirConverterTest.java)
- **Tests:** 26
- **Focus:** HL7 segment to FHIR resource conversion
- **Key Tests:**
  - PID → Patient
  - PV1 → Encounter
  - OBX → Observation
  - OBR → DiagnosticReport
  - ORC → ServiceRequest
  - IN1 → Coverage
  - Date/time conversions
  - Code system mappings

## Test Statistics

| Category | Count | Percentage |
|----------|-------|------------|
| Parser Tests | 23 | 28% |
| ADT Handler Tests | 16 | 20% |
| ORU Handler Tests | 17 | 21% |
| Converter Tests | 26 | 31% |
| **TOTAL** | **82** | **100%** |

## Coverage Matrix

| HL7 Message Type | Tests | FHIR Resources |
|------------------|-------|----------------|
| ADT^A01 (Admit) | 18 | Patient, Encounter, Coverage |
| ADT^A02 (Transfer) | 4 | Encounter (location) |
| ADT^A03 (Discharge) | 5 | Encounter (status) |
| ADT^A08 (Update) | 6 | Patient (demographics) |
| ORU^R01 (Lab) | 25 | Observation, DiagnosticReport |
| ORM^O01 (Order) | 3 | ServiceRequest |

## HL7 Segments Tested

| Segment | Purpose | Tests |
|---------|---------|-------|
| MSH | Message Header | All 82 |
| EVN | Event Type | 23 |
| PID | Patient Identification | 45 |
| PV1 | Patient Visit | 28 |
| IN1 | Insurance | 4 |
| OBR | Observation Request | 12 |
| OBX | Observation Result | 20 |
| ORC | Order Common | 3 |

## FHIR Resources Created

| Resource | From Segment | Tests |
|----------|--------------|-------|
| Patient | PID | 18 |
| Encounter | PV1 | 16 |
| Observation | OBX | 20 |
| DiagnosticReport | OBR | 8 |
| ServiceRequest | ORC | 3 |
| Coverage | IN1 | 4 |

## Test Execution

### Quick Start
```bash
cd /home/mahoosuc-solutions/projects/hdim-master/hdim-master/backend
./gradlew :modules:services:cdr-processor-service:test
```

### Detailed Commands
See [QUICK_TEST_REFERENCE.md](QUICK_TEST_REFERENCE.md)

## Documentation Structure

```
cdr-processor-service/
├── TEST_INDEX.md (this file)
├── TEST_REPORT.md (comprehensive report)
├── QUICK_TEST_REFERENCE.md (quick commands)
└── src/
    └── test/
        ├── TEST_SUMMARY.md (test summary)
        ├── resources/
        │   └── sample-messages.txt (sample data)
        └── java/com/healthdata/cdr/
            ├── parser/
            │   └── Hl7v2ParserServiceTest.java (23 tests)
            ├── handler/
            │   ├── AdtMessageHandlerTest.java (16 tests)
            │   └── OruMessageHandlerTest.java (17 tests)
            └── converter/
                └── Hl7ToFhirConverterTest.java (26 tests)
```

## Key Features Tested

### Message Parsing
- [x] Parse HL7 v2.5 messages
- [x] Extract all standard segments
- [x] Handle malformed messages
- [x] Validate message structure
- [x] Extract metadata (control ID, timestamp, version)

### Patient Data
- [x] Patient identifiers (MRN)
- [x] Patient name (all components)
- [x] Gender (M/F/U)
- [x] Date of birth
- [x] Address (full)
- [x] Phone number
- [x] Multiple identifiers

### Visit Data
- [x] Patient class (I/O/E)
- [x] Location (facility, room, bed)
- [x] Admission type
- [x] Visit number
- [x] Timestamps (admit/discharge)

### Lab Results
- [x] Numeric observations
- [x] Text observations
- [x] Multiple observations
- [x] Status codes (F/P/C)
- [x] Abnormal flags (H/L/N)
- [x] Reference ranges
- [x] LOINC codes

### Error Handling
- [x] Null inputs
- [x] Empty inputs
- [x] Malformed messages
- [x] Invalid dates
- [x] Missing segments
- [x] Wrong message types

### FHIR Compliance
- [x] FHIR R4 resources
- [x] Code system mappings
- [x] Date/time conversions
- [x] Resource relationships
- [x] Status codes

## Dependencies

### HL7 Processing
- HAPI HL7v2 (ca.uhn.hapi)
- HAPI FHIR R4 (ca.uhn.hapi.fhir)

### Testing
- JUnit 5
- Mockito

## Test Quality Metrics

| Metric | Target | Status |
|--------|--------|--------|
| Test Count | 30+ | ✓ 82 tests |
| Line Coverage | >80% | To be verified |
| Branch Coverage | >75% | To be verified |
| Method Coverage | >90% | To be verified |

## Next Steps

1. **Execute Tests**
   ```bash
   ./gradlew :modules:services:cdr-processor-service:test
   ```

2. **Generate Coverage**
   ```bash
   ./gradlew :modules:services:cdr-processor-service:test jacocoTestReport
   ```

3. **Review Results**
   - Check test output
   - Review coverage report
   - Identify gaps

4. **Iterate**
   - Fix failing tests
   - Add missing tests
   - Improve coverage

## Support

For questions or issues:
1. Review [TEST_REPORT.md](TEST_REPORT.md) for detailed documentation
2. Check [QUICK_TEST_REFERENCE.md](QUICK_TEST_REFERENCE.md) for commands
3. Examine sample messages in [src/test/resources/sample-messages.txt](src/test/resources/sample-messages.txt)

---

**Test Suite Version:** 1.0
**Created:** December 5, 2025
**Total Tests:** 82
**Status:** ✓ Ready to Run
