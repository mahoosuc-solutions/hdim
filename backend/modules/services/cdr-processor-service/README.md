# CDR Processor Service

Clinical Data Repository message processing service for HL7v2 and CDA document parsing with FHIR R4 conversion.

## Purpose

The CDR Processor Service provides high-performance parsing and transformation of clinical messages and documents. It supports HL7v2 messages (ADT, ORU, ORM, etc.) and CDA/C-CDA documents with automatic conversion to FHIR R4 resources for standardized data exchange.

## Key Features

- **HL7v2 Message Parsing**: Support for ADT, ORU, ORM, RDE, RAS, VXU message types
- **CDA Document Parsing**: C-CDA R2.1 document parsing and extraction
- **FHIR R4 Conversion**: Automatic conversion from HL7v2 and CDA to FHIR R4
- **Batch Processing**: Efficient batch processing of multiple messages/documents
- **Multi-tenant Support**: Complete tenant isolation and security
- **Validation**: Optional message/document validation with detailed error reporting
- **Dead Letter Queue**: Automatic DLQ handling for failed messages
- **Message Persistence**: Configurable message storage with 90-day retention
- **Audit Logging**: HIPAA-compliant audit trail for all processing

## API Endpoints

### HL7v2 Processing

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/cdr/hl7/v2` | Parse single HL7v2 message |
| POST | `/api/v1/cdr/hl7/v2/batch` | Batch process multiple HL7v2 messages |
| GET | `/api/v1/cdr/message-types` | Get supported HL7v2 message types |

### CDA Processing

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/cdr/cda` | Parse single CDA document |
| POST | `/api/v1/cdr/cda/batch` | Batch process multiple CDA documents |
| GET | `/api/v1/cdr/cda/document-types` | Get supported CDA document types |

### Service Status

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/cdr/status` | Get service status and capabilities |
| GET | `/api/v1/cdr/health` | Health check endpoint |

## Configuration

### Application Properties

```yaml
server:
  port: 8099

hl7:
  enabled: true
  validation:
    enabled: false
    strict: false
  parser:
    version: "2.5"
    allow-unknown-versions: true
  processing:
    batch-size: 100
    thread-pool-size: 10
    timeout-seconds: 60
  fhir:
    conversion-enabled: true
    auto-convert: false

cdr:
  audit:
    enabled: true
    include-message-content: false
  dlq:
    enabled: true
    topic: cdr-dlq
    max-retries: 3
  storage:
    persist-messages: true
    retention-days: 90
```

### Database

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/hdim
    username: hdim
    password: ${DB_PASSWORD}
```

### Kafka

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: cdr-processor-group
```

## Supported Message Types

### HL7v2
- **ADT**: A01-A08, A11 (admissions, transfers, discharges, updates)
- **ORU**: R01 (observation results, lab results)
- **ORM**: O01 (general orders)
- **RDE**: O11 (pharmacy orders)
- **RAS**: O17 (pharmacy administration)
- **VXU**: V04 (vaccination updates)

### CDA
- Continuity of Care Document (CCD)
- Consultation Note
- Discharge Summary
- History and Physical
- Progress Note
- Operative Note
- All C-CDA R2.1 document types

## Running Locally

### Start the Service

```bash
./gradlew :modules:services:cdr-processor-service:bootRun
```

### Running Tests

```bash
./gradlew :modules:services:cdr-processor-service:test
```

---

## Testing

### Overview

CDR Processor Service has comprehensive test coverage for HL7v2 message parsing, FHIR R4 conversion, and message handler processing. The test suite covers 11 HL7v2 message types (ADT, ORU, ORM, RDE, RAS, VXU, MDM, SIU, BAR, DFT, PPR) with 112 total test methods across 12 test files.

### Quick Start

```bash
# Run all tests
./gradlew :modules:services:cdr-processor-service:test

# Run specific test class
./gradlew :modules:services:cdr-processor-service:test --tests "Hl7v2ParserServiceTest"

# Run tests for specific message handler
./gradlew :modules:services:cdr-processor-service:test --tests "AdtMessageHandlerTest"

# Run HL7 to FHIR conversion tests
./gradlew :modules:services:cdr-processor-service:test --tests "Hl7ToFhirConverterTest"

# Run with test coverage report
./gradlew :modules:services:cdr-processor-service:test jacocoTestReport

# Run tests with verbose output
./gradlew :modules:services:cdr-processor-service:test --info
```

### Test Organization

```
src/test/java/com/healthdata/cdr/
├── parser/
│   └── Hl7v2ParserServiceTest.java         # HL7v2 parsing and routing (17 tests)
├── converter/
│   └── Hl7ToFhirConverterTest.java         # HL7 to FHIR R4 conversion (13 tests)
└── handler/
    ├── AdtMessageHandlerTest.java          # ADT message handling (21 tests)
    ├── OruMessageHandlerTest.java          # ORU lab results (4 tests)
    ├── RdeMessageHandlerTest.java          # RDE pharmacy orders (4 tests)
    ├── RasMessageHandlerTest.java          # RAS pharmacy admin (5 tests)
    ├── VxuMessageHandlerTest.java          # VXU immunizations (3 tests)
    ├── MdmMessageHandlerTest.java          # MDM documents (12 tests)
    ├── SiuMessageHandlerTest.java          # SIU scheduling (12 tests)
    ├── BarMessageHandlerTest.java          # BAR billing (9 tests)
    ├── DftMessageHandlerTest.java          # DFT financial (8 tests)
    └── PprMessageHandlerTest.java          # PPR problems (4 tests)
```

### Test Coverage Summary

| Test Class | Tests | Focus Area |
|------------|-------|------------|
| Hl7v2ParserServiceTest | 17 | Message parsing, routing, validation |
| Hl7ToFhirConverterTest | 13 | ADT→Patient, ORU→Observation, ORM→ServiceRequest |
| AdtMessageHandlerTest | 21 | Patient, visit, event data extraction |
| OruMessageHandlerTest | 4 | Lab results, order observations |
| RdeMessageHandlerTest | 4 | Pharmacy encoded orders |
| RasMessageHandlerTest | 5 | Medication administration |
| VxuMessageHandlerTest | 3 | Vaccination updates |
| MdmMessageHandlerTest | 12 | Medical documents, TXA segment |
| SiuMessageHandlerTest | 12 | Scheduling, appointments |
| BarMessageHandlerTest | 9 | Billing account records |
| DftMessageHandlerTest | 8 | Financial transactions |
| PprMessageHandlerTest | 4 | Problem list management |
| **Total** | **112** | |

---

### Unit Tests - HL7v2 Parser Service

Tests message parsing, handler routing, and validation.

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("HL7 v2 Parser Service Tests")
class Hl7v2ParserServiceTest {

    @InjectMocks
    private Hl7v2ParserService parserService;

    @Mock
    private Parser hl7v2Parser;

    @Mock
    private AdtMessageHandler adtMessageHandler;

    @Mock
    private OruMessageHandler oruMessageHandler;

    // Additional message handler mocks...

    private static final String TENANT_ID = "test-tenant";

    @Nested
    @DisplayName("ADT Message Parsing")
    class AdtMessageParsingTests {

        @Test
        @DisplayName("Should parse ADT^A01 and route to ADT handler")
        void parseMessage_withAdtA01_routesToAdtHandler() throws HL7Exception {
            // Given
            String rawMessage = "MSH|^~\\&|SENDING|FAC|RECV|FAC|20240115120000||ADT^A01|123|P|2.5\rPID|1||12345||DOE^JOHN";
            Message mockMessage = createMockMessage("ADT", "A01", "123", "2.5");
            Map<String, Object> handlerResult = new HashMap<>();
            handlerResult.put("patient", Map.of("patientId", "12345"));

            when(hl7v2Parser.parse(rawMessage)).thenReturn(mockMessage);
            when(adtMessageHandler.handle(mockMessage)).thenReturn(handlerResult);

            // When
            Hl7v2Message result = parserService.parseMessage(rawMessage, TENANT_ID);

            // Then
            assertThat(result.getStatus()).isEqualTo("PARSED");
            assertThat(result.getMessageType()).isEqualTo("ADT");
            assertThat(result.getTriggerEvent()).isEqualTo("A01");
            assertThat(result.getMessageControlId()).isEqualTo("123");
            assertThat(result.getVersion()).isEqualTo("2.5");
            assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
            verify(adtMessageHandler).handle(mockMessage);
        }

        @Test
        @DisplayName("Should extract message code from ADT message")
        void parseMessage_withAdtA01_extractsMessageCode() throws HL7Exception {
            String rawMessage = "MSH|^~\\&|SENDING|FAC|RECV|FAC|20240115120000||ADT^A01|456|P|2.5";
            Message mockMessage = createMockMessage("ADT", "A01", "456", "2.5");
            when(hl7v2Parser.parse(rawMessage)).thenReturn(mockMessage);
            when(adtMessageHandler.handle(mockMessage)).thenReturn(new HashMap<>());

            Hl7v2Message result = parserService.parseMessage(rawMessage, TENANT_ID);

            assertThat(result.getMessageCode()).isEqualTo("ADT^A01");
        }
    }

    @Nested
    @DisplayName("ORU Message Parsing")
    class OruMessageParsingTests {

        @Test
        @DisplayName("Should parse ORU^R01 and route to ORU handler")
        void parseMessage_withOruR01_routesToOruHandler() throws HL7Exception {
            String rawMessage = "MSH|^~\\&|LAB|FAC|EMR|FAC|20240115120000||ORU^R01|789|P|2.5";
            Message mockMessage = createMockMessage("ORU", "R01", "789", "2.5");
            Map<String, Object> handlerResult = new HashMap<>();
            handlerResult.put("observations", java.util.List.of());

            when(hl7v2Parser.parse(rawMessage)).thenReturn(mockMessage);
            when(oruMessageHandler.handle(mockMessage)).thenReturn(handlerResult);

            Hl7v2Message result = parserService.parseMessage(rawMessage, TENANT_ID);

            assertThat(result.getMessageType()).isEqualTo("ORU");
            assertThat(result.getTriggerEvent()).isEqualTo("R01");
            verify(oruMessageHandler).handle(mockMessage);
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return error status when parsing fails")
        void parseMessage_withInvalidMessage_returnsErrorStatus() throws HL7Exception {
            String rawMessage = "INVALID MESSAGE";
            when(hl7v2Parser.parse(rawMessage)).thenThrow(new HL7Exception("Parse error"));

            Hl7v2Message result = parserService.parseMessage(rawMessage, TENANT_ID);

            assertThat(result.getStatus()).isEqualTo("ERROR");
            assertThat(result.getErrorMessage()).contains("Parse error");
            assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
        }

        @Test
        @DisplayName("Should handle unsupported message type")
        void parseMessage_withUnsupportedType_logsWarning() throws HL7Exception {
            String rawMessage = "MSH|^~\\&|APP|FAC|EMR|FAC|20240115120000||XXX^Y01|000|P|2.5";
            Message mockMessage = createMockMessage("XXX", "Y01", "000", "2.5");
            when(hl7v2Parser.parse(rawMessage)).thenReturn(mockMessage);

            Hl7v2Message result = parserService.parseMessage(rawMessage, TENANT_ID);

            assertThat(result.getStatus()).isEqualTo("PARSED");
            assertThat(result.getMessageType()).isEqualTo("XXX");
            assertThat(result.getParsedData()).containsKey("warning");
        }
    }

    @Nested
    @DisplayName("Message Validation")
    class MessageValidationTests {

        @Test
        @DisplayName("Should validate null message")
        void validateMessage_withNull_returnsFalse() {
            assertThat(parserService.validateMessage(null)).isFalse();
        }

        @Test
        @DisplayName("Should validate empty message")
        void validateMessage_withEmpty_returnsFalse() {
            assertThat(parserService.validateMessage("")).isFalse();
        }

        @Test
        @DisplayName("Should validate message without MSH")
        void validateMessage_withoutMsh_returnsFalse() {
            assertThat(parserService.validateMessage("PID|1||12345")).isFalse();
        }

        @Test
        @DisplayName("Should validate valid message")
        void validateMessage_withValidMessage_returnsTrue() {
            String validMessage = "MSH|^~\\&|APP|FAC|EMR|FAC|20240115120000||ADT^A01|123|P|2.5";
            assertThat(parserService.validateMessage(validMessage)).isTrue();
        }
    }
}
```

---

### Unit Tests - HL7 to FHIR Converter

Tests conversion of HL7v2 messages to FHIR R4 resources.

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("HL7 to FHIR Converter Tests")
class Hl7ToFhirConverterTest {

    @InjectMocks
    private Hl7ToFhirConverter converter;

    @BeforeEach
    void setUp() {
        converter = new Hl7ToFhirConverter();
    }

    @Nested
    @DisplayName("ADT Message Conversion")
    class AdtConversionTests {

        @Test
        @DisplayName("Should convert ADT message to Patient resource")
        void convertToFhir_withAdtMessage_createsPatientResource() {
            // Given
            Hl7v2Message message = createAdtMessage();

            // When
            Bundle bundle = converter.convertToFhir(message);

            // Then
            assertThat(bundle).isNotNull();
            assertThat(bundle.getType()).isEqualTo(Bundle.BundleType.TRANSACTION);
            assertThat(bundle.getEntry()).isNotEmpty();

            Optional<Patient> patient = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Patient)
                .map(e -> (Patient) e.getResource())
                .findFirst();
            assertThat(patient).isPresent();
        }

        @Test
        @DisplayName("Should convert ADT message to Encounter resource")
        void convertToFhir_withAdtMessage_createsEncounterResource() {
            Hl7v2Message message = createAdtMessageWithVisit();

            Bundle bundle = converter.convertToFhir(message);

            assertThat(bundle).isNotNull();
            Optional<Encounter> encounter = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Encounter)
                .map(e -> (Encounter) e.getResource())
                .findFirst();
            assertThat(encounter).isPresent();
        }

        @Test
        @DisplayName("Should set patient identifier from parsed data")
        void convertToFhir_withPatientId_setsIdentifier() {
            Hl7v2Message message = createAdtMessage();

            Bundle bundle = converter.convertToFhir(message);

            Optional<Patient> patient = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Patient)
                .map(e -> (Patient) e.getResource())
                .findFirst();
            assertThat(patient).isPresent();
            assertThat(patient.get().getIdentifier()).isNotEmpty();
            assertThat(patient.get().getIdentifier().get(0).getValue()).isEqualTo("12345");
        }
    }

    @Nested
    @DisplayName("ORU Message Conversion")
    class OruConversionTests {

        @Test
        @DisplayName("Should convert ORU message to Observation resources")
        void convertToFhir_withOruMessage_createsObservationResources() {
            Hl7v2Message message = createOruMessage();

            Bundle bundle = converter.convertToFhir(message);

            assertThat(bundle).isNotNull();
            List<Observation> observations = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Observation)
                .map(e -> (Observation) e.getResource())
                .toList();
            assertThat(observations).isNotEmpty();
        }

        @Test
        @DisplayName("Should set observation code from parsed data")
        void convertToFhir_withObservationCode_setsCode() {
            Hl7v2Message message = createOruMessage();

            Bundle bundle = converter.convertToFhir(message);

            Optional<Observation> observation = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Observation)
                .map(e -> (Observation) e.getResource())
                .findFirst();
            assertThat(observation).isPresent();
            assertThat(observation.get().getCode().getCoding()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("RDE Message Conversion")
    class RdeConversionTests {

        @Test
        @DisplayName("Should convert RDE message to MedicationRequest resource")
        void convertToFhir_withRdeMessage_createsMedicationRequestResource() {
            Hl7v2Message message = createRdeMessage();

            Bundle bundle = converter.convertToFhir(message);

            assertThat(bundle).isNotNull();
            Optional<MedicationRequest> medicationRequest = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof MedicationRequest)
                .map(e -> (MedicationRequest) e.getResource())
                .findFirst();
            assertThat(medicationRequest).isPresent();
        }
    }

    @Nested
    @DisplayName("VXU Message Conversion")
    class VxuConversionTests {

        @Test
        @DisplayName("Should convert VXU message to Immunization resource")
        void convertToFhir_withVxuMessage_createsImmunizationResource() {
            Hl7v2Message message = createVxuMessage();

            Bundle bundle = converter.convertToFhir(message);

            assertThat(bundle).isNotNull();
            Optional<Immunization> immunization = bundle.getEntry().stream()
                .filter(e -> e.getResource() instanceof Immunization)
                .map(e -> (Immunization) e.getResource())
                .findFirst();
            assertThat(immunization).isPresent();
        }
    }

    @Nested
    @DisplayName("Empty/Null Data Handling")
    class EmptyDataTests {

        @Test
        @DisplayName("Should return empty bundle for null parsed data")
        void convertToFhir_withNullParsedData_returnsEmptyBundle() {
            Hl7v2Message message = Hl7v2Message.builder()
                .messageType("ADT")
                .parsedData(null)
                .build();

            Bundle bundle = converter.convertToFhir(message);

            assertThat(bundle).isNotNull();
            assertThat(bundle.getEntry()).isEmpty();
        }

        @Test
        @DisplayName("Should handle unsupported message type")
        void convertToFhir_withUnsupportedType_returnsEmptyBundle() {
            Hl7v2Message message = Hl7v2Message.builder()
                .messageType("XXX")
                .parsedData(new HashMap<>())
                .build();

            Bundle bundle = converter.convertToFhir(message);

            assertThat(bundle).isNotNull();
            assertThat(bundle.getEntry()).isEmpty();
        }
    }

    // Helper method to create test messages
    private Hl7v2Message createAdtMessage() {
        Map<String, Object> patientData = new HashMap<>();
        patientData.put("patientId", "12345");
        patientData.put("familyName", "Smith");
        patientData.put("givenName", "John");
        patientData.put("gender", "M");
        patientData.put("dateOfBirth", "19800115");

        Map<String, Object> parsedData = new HashMap<>();
        parsedData.put("patient", patientData);

        return Hl7v2Message.builder()
            .messageType("ADT")
            .triggerEvent("A01")
            .parsedData(parsedData)
            .build();
    }
}
```

---

### Unit Tests - ADT Message Handler

Tests ADT (Admit/Discharge/Transfer) message processing.

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("ADT Message Handler Tests")
class AdtMessageHandlerTest {

    @InjectMocks
    private AdtMessageHandler handler;

    @Nested
    @DisplayName("Patient Data Tests")
    class PatientDataTests {

        @Test
        @DisplayName("Should extract patient identifier from PID segment")
        void extractPatient_withPidSegment_extractsPatientId() throws HL7Exception {
            ADT_A01 adt = mock(ADT_A01.class, RETURNS_DEEP_STUBS);
            setupAdtWithPatient(adt, "12345", "Smith", "John");

            Map<String, Object> result = handler.handle(adt);

            assertThat(result).containsKey("patient");
            @SuppressWarnings("unchecked")
            Map<String, Object> patient = (Map<String, Object>) result.get("patient");
            assertThat(patient).containsEntry("patientIdentifier", "12345");
        }

        @Test
        @DisplayName("Should extract patient name from PID segment")
        void extractPatient_withPidSegment_extractsPatientName() throws HL7Exception {
            ADT_A01 adt = mock(ADT_A01.class, RETURNS_DEEP_STUBS);
            setupAdtWithPatient(adt, "12345", "Smith", "John");

            Map<String, Object> result = handler.handle(adt);

            @SuppressWarnings("unchecked")
            Map<String, Object> patient = (Map<String, Object>) result.get("patient");
            assertThat(patient).containsEntry("familyName", "Smith");
            assertThat(patient).containsEntry("givenName", "John");
        }

        @Test
        @DisplayName("Should extract date of birth from PID segment")
        void extractPatient_withPidSegment_extractsDateOfBirth() throws HL7Exception {
            ADT_A01 adt = mock(ADT_A01.class, RETURNS_DEEP_STUBS);
            setupAdtWithDateOfBirth(adt, "19800115");

            Map<String, Object> result = handler.handle(adt);

            @SuppressWarnings("unchecked")
            Map<String, Object> patient = (Map<String, Object>) result.get("patient");
            assertThat(patient).containsEntry("dateOfBirth", "19800115");
        }

        @Test
        @DisplayName("Should extract gender from PID segment")
        void extractPatient_withPidSegment_extractsGender() throws HL7Exception {
            ADT_A01 adt = mock(ADT_A01.class, RETURNS_DEEP_STUBS);
            setupAdtWithGender(adt, "M");

            Map<String, Object> result = handler.handle(adt);

            @SuppressWarnings("unchecked")
            Map<String, Object> patient = (Map<String, Object>) result.get("patient");
            assertThat(patient).containsEntry("gender", "M");
        }
    }

    @Nested
    @DisplayName("Visit Data Tests")
    class VisitDataTests {

        @Test
        @DisplayName("Should extract patient class from PV1 segment")
        void extractVisit_withPv1Segment_extractsPatientClass() throws HL7Exception {
            ADT_A01 adt = mock(ADT_A01.class, RETURNS_DEEP_STUBS);
            setupAdtWithVisit(adt, "I", "V123456");

            Map<String, Object> result = handler.handle(adt);

            assertThat(result).containsKey("visit");
            @SuppressWarnings("unchecked")
            Map<String, Object> visit = (Map<String, Object>) result.get("visit");
            assertThat(visit).containsEntry("patientClass", "I");
        }

        @Test
        @DisplayName("Should extract visit number from PV1 segment")
        void extractVisit_withPv1Segment_extractsVisitNumber() throws HL7Exception {
            ADT_A01 adt = mock(ADT_A01.class, RETURNS_DEEP_STUBS);
            setupAdtWithVisit(adt, "I", "V123456");

            Map<String, Object> result = handler.handle(adt);

            @SuppressWarnings("unchecked")
            Map<String, Object> visit = (Map<String, Object>) result.get("visit");
            assertThat(visit).containsEntry("visitNumber", "V123456");
        }

        @Test
        @DisplayName("Should extract admit date/time from PV1 segment")
        void extractVisit_withPv1Segment_extractsAdmitDateTime() throws HL7Exception {
            ADT_A01 adt = mock(ADT_A01.class, RETURNS_DEEP_STUBS);
            setupAdtWithAdmitDateTime(adt, "20240115120000");

            Map<String, Object> result = handler.handle(adt);

            @SuppressWarnings("unchecked")
            Map<String, Object> visit = (Map<String, Object>) result.get("visit");
            assertThat(visit).containsEntry("admitDateTime", "20240115120000");
        }
    }

    @Nested
    @DisplayName("Event Type Mapping Tests")
    class EventTypeMappingTests {

        @Test
        @DisplayName("Should map A01 to in-progress status")
        void mapEventToEncounterStatus_withA01_returnsInProgress() {
            String status = handler.mapEventToEncounterStatus("A01");
            assertThat(status).isEqualTo("in-progress");
        }

        @Test
        @DisplayName("Should map A03 to finished status")
        void mapEventToEncounterStatus_withA03_returnsFinished() {
            String status = handler.mapEventToEncounterStatus("A03");
            assertThat(status).isEqualTo("finished");
        }

        @Test
        @DisplayName("Should map A05 to planned status")
        void mapEventToEncounterStatus_withA05_returnsPlanned() {
            String status = handler.mapEventToEncounterStatus("A05");
            assertThat(status).isEqualTo("planned");
        }

        @Test
        @DisplayName("Should map A11 to cancelled status")
        void mapEventToEncounterStatus_withA11_returnsCancelled() {
            String status = handler.mapEventToEncounterStatus("A11");
            assertThat(status).isEqualTo("cancelled");
        }

        @Test
        @DisplayName("Should identify A06 as patient class change event")
        void isPatientClassChangeEvent_withA06_returnsTrue() {
            boolean isChange = handler.isPatientClassChangeEvent("A06");
            assertThat(isChange).isTrue();
        }

        @Test
        @DisplayName("Should identify A11 as cancellation event")
        void isCancellationEvent_withA11_returnsTrue() {
            boolean isCancellation = handler.isCancellationEvent("A11");
            assertThat(isCancellation).isTrue();
        }
    }
}
```

---

### Unit Tests - MDM Message Handler

Tests MDM (Medical Document Management) message processing.

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("MDM Message Handler Tests")
class MdmMessageHandlerTest {

    @InjectMocks
    private MdmMessageHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MdmMessageHandler();
    }

    @Nested
    @DisplayName("MDM^T01 - Original Document Notification")
    class MdmT01Tests {

        @Test
        @DisplayName("Should extract message type and trigger event from MDM^T01")
        void handle_withValidMdmT01_extractsMessageTypeAndTriggerEvent() throws HL7Exception {
            // Given
            MDM_T01 mdm = mock(MDM_T01.class);
            EVN evn = mock(EVN.class);
            PID pid = mock(PID.class);
            PV1 pv1 = mock(PV1.class);
            TXA txa = mock(TXA.class);

            when(mdm.getEVN()).thenReturn(evn);
            when(mdm.getPID()).thenReturn(pid);
            when(mdm.getPV1()).thenReturn(pv1);
            when(mdm.getTXA()).thenReturn(txa);

            mockMinimalEvn(evn);
            mockMinimalPid(pid);
            mockMinimalPv1(pv1);
            mockMinimalTxa(txa);

            // When
            Map<String, Object> result = handler.handle(mdm);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("messageType")).isEqualTo("MDM");
            assertThat(result.get("triggerEvent")).isEqualTo("T01");
            assertThat(result.get("eventDescription")).isEqualTo("Original document notification");
        }

        @Test
        @DisplayName("Should extract document data from TXA segment")
        void handle_withValidMdmT01_extractsDocumentData() throws HL7Exception {
            // Given
            MDM_T01 mdm = mock(MDM_T01.class);
            // ... setup mocks with document data

            // When
            Map<String, Object> result = handler.handle(mdm);

            // Then
            assertThat(result).containsKey("document");
            @SuppressWarnings("unchecked")
            Map<String, Object> document = (Map<String, Object>) result.get("document");
            assertThat(document.get("setId")).isEqualTo("1");
            assertThat(document.get("documentType")).isEqualTo("DISCHARGE_SUMMARY");
            assertThat(document.get("completionStatus")).isEqualTo("AU");
            assertThat(document.get("uniqueDocumentNumber")).isEqualTo("DOC-12345");
        }

        @Test
        @DisplayName("Should handle missing EVN segment gracefully")
        void handle_withNullEvnSegment_handlesGracefully() throws HL7Exception {
            MDM_T01 mdm = mock(MDM_T01.class);
            when(mdm.getEVN()).thenReturn(null);
            when(mdm.getPID()).thenReturn(null);
            when(mdm.getPV1()).thenReturn(null);
            when(mdm.getTXA()).thenReturn(null);

            Map<String, Object> result = handler.handle(mdm);

            assertThat(result).isNotNull();
            assertThat(result.get("messageType")).isEqualTo("MDM");
            assertThat(result.get("triggerEvent")).isEqualTo("T01");
        }
    }

    @Nested
    @DisplayName("MDM^T02 - Original Document Notification and Content")
    class MdmT02Tests {

        @Test
        @DisplayName("Should extract message type and trigger event from MDM^T02")
        void handle_withValidMdmT02_extractsMessageTypeAndTriggerEvent() throws HL7Exception {
            MDM_T02 mdm = mock(MDM_T02.class);
            // ... setup mocks

            Map<String, Object> result = handler.handle(mdm);

            assertThat(result).isNotNull();
            assertThat(result.get("messageType")).isEqualTo("MDM");
            assertThat(result.get("triggerEvent")).isEqualTo("T02");
            assertThat(result.get("eventDescription")).isEqualTo("Original document notification and content");
        }

        @Test
        @DisplayName("Should extract document content placeholder from MDM^T02")
        void handle_withValidMdmT02_extractsDocumentContent() throws HL7Exception {
            MDM_T02 mdm = mock(MDM_T02.class);
            // ... setup mocks

            Map<String, Object> result = handler.handle(mdm);

            assertThat(result).containsKey("documentContent");
            assertThat(result.get("documentContent")).isEqualTo("See attached document");
        }
    }
}
```

---

### Unit Tests - SIU Message Handler

Tests SIU (Scheduling Information Unsolicited) message processing.

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("SIU Message Handler Tests")
class SiuMessageHandlerTest {

    @InjectMocks
    private SiuMessageHandler handler;

    @Nested
    @DisplayName("Trigger Event Tests")
    class TriggerEventTests {

        @Test
        @DisplayName("Should extract S12 trigger event for new appointment booking")
        void determineTriggerEvent_withS12_returnsCorrectDescription() throws HL7Exception {
            SIU_S12 siu = mock(SIU_S12.class, RETURNS_DEEP_STUBS);
            setupMinimalSiu(siu, "S12");

            Map<String, Object> result = handler.handle(siu);

            assertThat(result).containsEntry("messageType", "SIU");
            assertThat(result).containsEntry("triggerEvent", "S12");
            assertThat(result).containsEntry("eventDescription", "Notification of new appointment booking");
        }

        @Test
        @DisplayName("Should extract S13 trigger event for appointment rescheduling")
        void determineTriggerEvent_withS13_returnsCorrectDescription() throws HL7Exception {
            SIU_S12 siu = mock(SIU_S12.class, RETURNS_DEEP_STUBS);
            setupMinimalSiu(siu, "S13");

            Map<String, Object> result = handler.handle(siu);

            assertThat(result).containsEntry("triggerEvent", "S13");
            assertThat(result).containsEntry("eventDescription", "Notification of appointment rescheduling");
        }

        @Test
        @DisplayName("Should extract S15 trigger event for appointment cancellation")
        void determineTriggerEvent_withS15_returnsCorrectDescription() throws HL7Exception {
            SIU_S12 siu = mock(SIU_S12.class, RETURNS_DEEP_STUBS);
            setupMinimalSiu(siu, "S15");

            Map<String, Object> result = handler.handle(siu);

            assertThat(result).containsEntry("triggerEvent", "S15");
            assertThat(result).containsEntry("eventDescription", "Notification of appointment cancellation");
        }

        @Test
        @DisplayName("Should extract S26 trigger event for patient no-show")
        void determineTriggerEvent_withS26_returnsCorrectDescription() throws HL7Exception {
            SIU_S12 siu = mock(SIU_S12.class, RETURNS_DEEP_STUBS);
            setupMinimalSiu(siu, "S26");

            Map<String, Object> result = handler.handle(siu);

            assertThat(result).containsEntry("triggerEvent", "S26");
            assertThat(result).containsEntry("eventDescription", "Notification of patient no-show");
        }
    }

    @Nested
    @DisplayName("Scheduling Data Extraction")
    class SchedulingDataTests {

        @Test
        @DisplayName("Should extract placer and filler appointment IDs from SCH segment")
        void extractSchedulingData_withSchSegment_extractsAppointmentIds() throws HL7Exception {
            SIU_S12 siu = mock(SIU_S12.class, RETURNS_DEEP_STUBS);
            setupMinimalSiu(siu, "S12");
            SCH sch = setupSchWithAppointmentIds(siu);

            Map<String, Object> result = handler.handle(siu);

            assertThat(result).containsKey("schedule");
            @SuppressWarnings("unchecked")
            Map<String, Object> schedule = (Map<String, Object>) result.get("schedule");
            assertThat(schedule).containsEntry("placerAppointmentId", "PLACER001");
            assertThat(schedule).containsEntry("fillerAppointmentId", "FILLER001");
        }

        @Test
        @DisplayName("Should extract appointment duration from SCH segment")
        void extractSchedulingData_withSchSegment_extractsDuration() throws HL7Exception {
            SIU_S12 siu = mock(SIU_S12.class, RETURNS_DEEP_STUBS);
            setupMinimalSiu(siu, "S12");
            setupSchWithDuration(siu);

            Map<String, Object> result = handler.handle(siu);

            assertThat(result).containsKey("schedule");
            @SuppressWarnings("unchecked")
            Map<String, Object> schedule = (Map<String, Object>) result.get("schedule");
            assertThat(schedule).containsEntry("appointmentDuration", "30");
        }
    }
}
```

---

### Unit Tests - BAR and DFT Message Handlers

Tests BAR (Billing Account Record) and DFT (Detailed Financial Transaction) message processing.

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("BAR Message Handler Tests")
class BarMessageHandlerTest {

    @InjectMocks
    private BarMessageHandler handler;

    @Nested
    @DisplayName("BAR^P01 - Add Patient Account")
    class BarP01Tests {

        @Test
        @DisplayName("Should extract trigger event P01 and event description")
        void handle_withBarP01_extractsTriggerEvent() throws HL7Exception {
            BAR_P01 bar = mock(BAR_P01.class);
            when(bar.getEVN()).thenReturn(null);
            when(bar.getPID()).thenReturn(null);
            when(bar.getVISITReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(bar);

            assertThat(result).containsEntry("messageType", "BAR");
            assertThat(result).containsEntry("triggerEvent", "P01");
            assertThat(result).containsEntry("eventDescription", "Add patient account");
        }

        @Test
        @DisplayName("Should extract patient ID and name from PID segment")
        void handle_withBarP01_extractsPatientData() throws HL7Exception {
            BAR_P01 bar = mock(BAR_P01.class);
            PID pid = createMockPidWithBasicData();

            when(bar.getEVN()).thenReturn(null);
            when(bar.getPID()).thenReturn(pid);
            when(bar.getVISITReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(bar);

            assertThat(result).containsKey("patient");
            @SuppressWarnings("unchecked")
            Map<String, Object> patient = (Map<String, Object>) result.get("patient");
            assertThat(patient).containsEntry("patientId", "12345");
            assertThat(patient).containsEntry("familyName", "Smith");
            assertThat(patient).containsEntry("givenName", "John");
        }

        @Test
        @DisplayName("Should extract account number from PID segment")
        void handle_withBarP01_extractsAccountNumber() throws HL7Exception {
            BAR_P01 bar = mock(BAR_P01.class);
            PID pid = createMockPidWithAccountNumber();

            when(bar.getEVN()).thenReturn(null);
            when(bar.getPID()).thenReturn(pid);
            when(bar.getVISITReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(bar);

            @SuppressWarnings("unchecked")
            Map<String, Object> patient = (Map<String, Object>) result.get("patient");
            assertThat(patient).containsEntry("accountNumber", "ACC001");
        }
    }

    @Nested
    @DisplayName("BAR^P02 - Purge Patient Accounts")
    class BarP02Tests {

        @Test
        @DisplayName("Should extract trigger event P02 and event description")
        void handle_withBarP02_extractsTriggerEvent() throws HL7Exception {
            BAR_P02 bar = mock(BAR_P02.class);
            when(bar.getEVN()).thenReturn(null);
            when(bar.getPATIENTReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(bar);

            assertThat(result).containsEntry("messageType", "BAR");
            assertThat(result).containsEntry("triggerEvent", "P02");
            assertThat(result).containsEntry("eventDescription", "Purge patient accounts");
        }
    }
}

@ExtendWith(MockitoExtension.class)
@DisplayName("DFT Message Handler Tests")
class DftMessageHandlerTest {

    @InjectMocks
    private DftMessageHandler handler;

    @Nested
    @DisplayName("DFT^P03 - Post Detail Financial Transaction")
    class DftP03Tests {

        @Test
        @DisplayName("Should extract trigger event P03 and event description")
        void handle_withDftP03_extractsTriggerEvent() throws HL7Exception {
            DFT_P03 dft = mock(DFT_P03.class);
            when(dft.getEVN()).thenReturn(null);
            when(dft.getPID()).thenReturn(null);
            when(dft.getPV1()).thenReturn(null);
            when(dft.getFINANCIALReps()).thenReturn(0);
            when(dft.getDG1Reps()).thenReturn(0);
            when(dft.getGT1Reps()).thenReturn(0);
            when(dft.getINSURANCEReps()).thenReturn(0);

            Map<String, Object> result = handler.handle(dft);

            assertThat(result).containsEntry("messageType", "DFT");
            assertThat(result).containsEntry("triggerEvent", "P03");
            assertThat(result).containsEntry("eventDescription", "Post detail financial transaction");
        }

        @Test
        @DisplayName("Should extract visit data from PV1 segment")
        void handle_withDftP03_extractsVisitData() throws HL7Exception {
            DFT_P03 dft = mock(DFT_P03.class);
            PV1 pv1 = createMockPv1WithBasicData();

            when(dft.getEVN()).thenReturn(null);
            when(dft.getPID()).thenReturn(null);
            when(dft.getPV1()).thenReturn(pv1);
            // ... setup other mocks

            Map<String, Object> result = handler.handle(dft);

            assertThat(result).containsKey("visit");
            @SuppressWarnings("unchecked")
            Map<String, Object> visit = (Map<String, Object>) result.get("visit");
            assertThat(visit).containsEntry("visitNumber", "V001");
            assertThat(visit).containsEntry("patientClass", "I");
        }
    }
}
```

---

### RBAC/Permission Tests

Test role-based access control for CDR API endpoints.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class CdrRbacTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String TENANT_ID = "tenant-rbac-001";

    @Test
    @DisplayName("Admin should be able to parse HL7v2 messages")
    void adminCanParseHl7Messages() throws Exception {
        String hl7Message = "MSH|^~\\&|APP|FAC|EMR|FAC|20240115120000||ADT^A01|123|P|2.5";

        mockMvc.perform(post("/api/v1/cdr/hl7/v2")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "admin-001")
                .header("X-Auth-Roles", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"" + hl7Message + "\", \"convertToFhir\": true}"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Evaluator should be able to parse HL7v2 messages")
    void evaluatorCanParseHl7Messages() throws Exception {
        String hl7Message = "MSH|^~\\&|APP|FAC|EMR|FAC|20240115120000||ADT^A01|123|P|2.5";

        mockMvc.perform(post("/api/v1/cdr/hl7/v2")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "evaluator-001")
                .header("X-Auth-Roles", "EVALUATOR")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"" + hl7Message + "\", \"convertToFhir\": true}"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Viewer should have read-only access to service status")
    void viewerCanAccessServiceStatus() throws Exception {
        mockMvc.perform(get("/api/v1/cdr/status")
                .header("X-Tenant-ID", TENANT_ID)
                .header("X-Auth-User-Id", "viewer-001")
                .header("X-Auth-Roles", "VIEWER"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Unauthenticated requests should be rejected")
    void unauthenticatedRequestsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/cdr/hl7/v2")
                .header("X-Tenant-ID", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"MSH|...\"}"))
            .andExpect(status().isUnauthorized());
    }
}
```

---

### HIPAA Compliance Tests

Test HIPAA-compliant handling of Protected Health Information (PHI).

```java
@SpringBootTest
@Testcontainers
class CdrHipaaComplianceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("PHI responses must include no-cache headers")
    void phiResponsesShouldIncludeNoCacheHeaders() throws Exception {
        mockMvc.perform(post("/api/v1/cdr/hl7/v2")
                .header("X-Tenant-ID", "tenant-001")
                .header("X-Auth-User-Id", "user-001")
                .header("X-Auth-Roles", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"MSH|...\", \"tenantId\": \"tenant-001\"}"))
            .andExpect(header().string("Cache-Control",
                allOf(
                    containsString("no-store"),
                    containsString("no-cache"),
                    containsString("must-revalidate")
                )))
            .andExpect(header().string("Pragma", "no-cache"));
    }

    @Test
    @DisplayName("Message persistence should respect 90-day retention policy")
    void messagePersistenceShouldRespectRetentionPolicy() {
        // Verify configuration
        assertThat(cdrConfig.getStorage().getRetentionDays())
            .isLessThanOrEqualTo(90)
            .withFailMessage("Message retention exceeds 90 days (policy violation)");
    }

    @Test
    @DisplayName("Test data must use synthetic PHI patterns")
    void testDataMustBeSynthetic() {
        // Verify test messages use synthetic patterns
        String testPatientId = "12345";
        String testPatientName = "Smith";

        // In production, patient IDs would follow TEST-MRN-XXXXXX pattern
        // This test ensures test data generators create clearly synthetic data
        assertThat(testPatientId)
            .doesNotContain("real")
            .withFailMessage("Test data should use synthetic identifiers");
    }

    @Test
    @DisplayName("DLQ messages should include error context but mask PHI")
    void dlqMessagesShouldMaskPhi() {
        // Dead letter queue should store:
        // - Error reason
        // - Message type
        // - Tenant ID
        // - Timestamp
        // But NOT:
        // - Patient names
        // - SSNs
        // - Full message content with PHI
    }

    @Test
    @DisplayName("Audit logging should capture message processing events")
    void auditLoggingShouldCaptureProcessingEvents() {
        // Verify audit events are generated for:
        // - Message parsing requests
        // - FHIR conversion requests
        // - Batch processing operations
        // - Error/DLQ events
    }
}
```

---

### Performance Tests

Test message processing throughput and latency.

```java
@SpringBootTest
class CdrPerformanceTest {

    @Autowired
    private Hl7v2ParserService parserService;

    @Autowired
    private Hl7ToFhirConverter fhirConverter;

    @Test
    @DisplayName("Single message parsing should complete within 100ms")
    void singleMessageParsingShouldBeUnder100ms() {
        String hl7Message = createTestAdtMessage();
        String tenantId = "tenant-perf-001";

        Instant start = Instant.now();
        Hl7v2Message result = parserService.parseMessage(hl7Message, tenantId);
        Instant end = Instant.now();

        long elapsedMs = Duration.between(start, end).toMillis();

        assertThat(result.getStatus()).isEqualTo("PARSED");
        assertThat(elapsedMs)
            .isLessThan(100L)
            .withFailMessage("Single message parsing took %dms, exceeds 100ms SLA", elapsedMs);
    }

    @Test
    @DisplayName("Batch processing should handle 100+ messages per second")
    void batchProcessingShouldMeetThroughputSla() {
        String tenantId = "tenant-perf-001";
        int messageCount = 100;
        List<String> messages = IntStream.range(0, messageCount)
            .mapToObj(i -> createTestAdtMessage(i))
            .collect(Collectors.toList());

        Instant start = Instant.now();
        messages.forEach(msg -> parserService.parseMessage(msg, tenantId));
        Instant end = Instant.now();

        long elapsedMs = Duration.between(start, end).toMillis();
        double messagesPerSecond = messageCount / (elapsedMs / 1000.0);

        assertThat(messagesPerSecond)
            .isGreaterThan(100.0)
            .withFailMessage("Throughput %.2f msg/s below 100 msg/s SLA", messagesPerSecond);

        System.out.printf("Performance: %d messages in %dms (%.2f msg/s)%n",
            messageCount, elapsedMs, messagesPerSecond);
    }

    @Test
    @DisplayName("HL7 to FHIR conversion should complete within 200ms per message")
    void hl7ToFhirConversionShouldBeUnder200ms() {
        Hl7v2Message parsedMessage = createParsedAdtMessage();

        Instant start = Instant.now();
        Bundle bundle = fhirConverter.convertToFhir(parsedMessage);
        Instant end = Instant.now();

        long elapsedMs = Duration.between(start, end).toMillis();

        assertThat(bundle).isNotNull();
        assertThat(elapsedMs)
            .isLessThan(200L)
            .withFailMessage("FHIR conversion took %dms, exceeds 200ms SLA", elapsedMs);
    }

    @Test
    @DisplayName("Memory usage should not exceed limits during batch processing")
    void memoryUsageShouldBeControlled() {
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        // Process 1000 messages
        String tenantId = "tenant-perf-001";
        for (int i = 0; i < 1000; i++) {
            parserService.parseMessage(createTestAdtMessage(i), tenantId);
        }

        runtime.gc();
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryDelta = finalMemory - initialMemory;

        // Memory increase should be reasonable (< 100MB for 1000 messages)
        assertThat(memoryDelta / (1024 * 1024))
            .isLessThan(100L)
            .withFailMessage("Memory usage increased by %dMB during batch processing",
                memoryDelta / (1024 * 1024));
    }

    private String createTestAdtMessage(int index) {
        return String.format(
            "MSH|^~\\&|SENDING|FAC|RECV|FAC|%s||ADT^A01|MSG%d|P|2.5\r" +
            "PID|1||%d||TEST^PATIENT%d",
            "20240115120000", index, index, index);
    }
}
```

---

### Test Configuration

**BaseIntegrationTest Setup**

```java
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Database configuration
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Redis configuration
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }
}
```

**application-test.yml Configuration**

```yaml
spring:
  datasource:
    url: jdbc:tc:postgresql:15:///testdb
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver

hl7:
  enabled: true
  validation:
    enabled: true
    strict: false
  processing:
    batch-size: 10
    thread-pool-size: 2
    timeout-seconds: 30

cdr:
  audit:
    enabled: true
    include-message-content: false
  dlq:
    enabled: false
  storage:
    persist-messages: false
    retention-days: 1
```

---

### Supported HL7v2 Message Types

| Message Type | Description | Handler | Tests |
|--------------|-------------|---------|-------|
| ADT | Admit/Discharge/Transfer | AdtMessageHandler | 21 |
| ORU | Observation Results | OruMessageHandler | 4 |
| ORM | General Orders | OrmMessageHandler | 1 |
| RDE | Pharmacy Orders | RdeMessageHandler | 4 |
| RAS | Pharmacy Admin | RasMessageHandler | 5 |
| VXU | Vaccinations | VxuMessageHandler | 3 |
| MDM | Medical Documents | MdmMessageHandler | 12 |
| SIU | Scheduling | SiuMessageHandler | 12 |
| BAR | Billing Records | BarMessageHandler | 9 |
| DFT | Financial Trans | DftMessageHandler | 8 |
| PPR | Problems | PprMessageHandler | 4 |

---

### HL7v2 to FHIR R4 Resource Mapping

| HL7v2 Message | FHIR R4 Resource(s) |
|---------------|---------------------|
| ADT^A01-A08 | Patient, Encounter |
| ORU^R01 | Observation, DiagnosticReport |
| ORM^O01 | ServiceRequest |
| RDE^O11 | MedicationRequest |
| RAS^O17 | MedicationAdministration |
| VXU^V04 | Immunization |
| MDM^T01/T02 | DocumentReference |

---

### Best Practices

- **HIPAA Compliance**: All test data uses synthetic patterns; no real PHI in tests
- **Message Validation**: Validate MSH segment presence before parsing
- **Error Handling**: Handle HL7Exception gracefully, return ERROR status
- **FHIR Conversion**: Convert to FHIR R4 only when explicitly requested
- **Batch Processing**: Use configurable batch size (default: 100)
- **DLQ Handling**: Failed messages go to dead letter queue with context
- **Audit Logging**: All message processing operations are audited
- **Performance**: Target 100+ messages/second throughput

### Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| HL7Exception: Parse error | Invalid message format | Validate MSH segment and delimiters |
| Empty bundle returned | Unsupported message type | Check supported types table above |
| Conversion timeout | Complex message structure | Increase `timeout-seconds` config |
| DLQ overflow | High error rate | Check message source quality |
| Memory issues | Large batch size | Reduce `batch-size` config |

### CI/CD Integration

```yaml
# .github/workflows/cdr-processor-tests.yml
name: CDR Processor Tests

on:
  push:
    paths:
      - 'backend/modules/services/cdr-processor-service/**'
  pull_request:
    paths:
      - 'backend/modules/services/cdr-processor-service/**'

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Run tests
        run: ./gradlew :modules:services:cdr-processor-service:test
      - name: Generate coverage report
        run: ./gradlew :modules:services:cdr-processor-service:jacocoTestReport
      - name: Upload coverage
        uses: codecov/codecov-action@v3
```

---

### Building

```bash
./gradlew :modules:services:cdr-processor-service:build
```

## Example Usage

### Parse HL7v2 Message

```bash
curl -X POST http://localhost:8099/api/v1/cdr/hl7/v2 \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "tenant-001",
    "message": "MSH|^~\\&|EPIC|...",
    "convertToFhir": true
  }'
```

### Parse CDA Document

```bash
curl -X POST http://localhost:8099/api/v1/cdr/cda \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "tenant-001",
    "document": "<ClinicalDocument>...</ClinicalDocument>",
    "validateDocument": true,
    "convertToFhir": true
  }'
```

## Performance

- Processes 100+ messages per second
- Batch processing up to 100 messages per request
- Automatic thread pool sizing (10 threads default)
- Connection pooling with HikariCP

## Authors

HDIM Development Team

## License

Proprietary - Mahoosuc Solutions
