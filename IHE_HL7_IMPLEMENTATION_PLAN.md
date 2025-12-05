# IHE/HL7 Interoperability Implementation Plan

**Role**: Software Architect
**Date**: November 27, 2025
**Status**: Strategic Architecture Plan

---

## Executive Summary

This document defines the complete architecture for implementing healthcare interoperability standards (IHE and HL7) in the HealthData-in-Motion platform. The goal is to enable seamless integration with Electronic Health Records (EHRs), Health Information Exchanges (HIEs), and other healthcare systems.

### Current State
- **FHIR R4**: Already implemented via HAPI FHIR server
- **RESTful APIs**: Custom APIs for quality measures, CQL evaluation
- **Internal Integration**: Kafka-based event streaming

### Target State
- **IHE Profiles**: XDS.b, PIX/PDQ, XCA, XDS-I.b
- **HL7 v2.x**: ADT, ORM, ORU, MDM message support
- **HL7 v3 CDA**: Clinical Document Architecture R2
- **FHIR R4**: Enhanced with IHE profiles
- **Standards Compliance**: ONC certification ready

---

## Part 1: Standards Overview

### IHE (Integrating the Healthcare Enterprise)

**Purpose**: Define how healthcare IT systems share information using existing standards (HL7, DICOM, etc.)

#### Key IHE Profiles for Implementation

1. **XDS.b (Cross-Enterprise Document Sharing)**
   - Share clinical documents across organizations
   - Document registry and repository pattern
   - Used by HIEs nationwide

2. **PIX (Patient Identifier Cross-Referencing)**
   - Map patient identifiers across systems
   - Essential for accurate patient matching
   - Foundation for data exchange

3. **PDQ (Patient Demographics Query)**
   - Query patient demographics across systems
   - Complements PIX for patient discovery
   - Reduces duplicate records

4. **XCA (Cross-Community Access)**
   - Extend XDS across multiple communities
   - Query and retrieve documents from external HIEs
   - Regional/national interoperability

5. **XDS-I.b (Cross-Enterprise Document Sharing for Imaging)**
   - Share medical imaging across organizations
   - Extends XDS.b for DICOM integration
   - Lab results, radiology reports

### HL7 v2.x Messages

**Purpose**: Real-time communication between healthcare systems

#### Priority Message Types

1. **ADT (Admit, Discharge, Transfer)**
   - A01: Admit patient
   - A03: Discharge patient
   - A04: Register patient
   - A08: Update patient information
   - A40: Merge patient

2. **ORM (Order Messages)**
   - O01: Order message
   - Lab orders, imaging orders, medication orders

3. **ORU (Observation Result)**
   - R01: Unsolicited observation report
   - Lab results, vital signs

4. **MDM (Medical Document Management)**
   - T01: Document creation
   - T02: Document replacement
   - Clinical notes, discharge summaries

5. **SIU (Scheduling)**
   - S12: Appointment notification
   - S13: Appointment rescheduling
   - S15: Appointment cancellation

### HL7 v3 CDA (Clinical Document Architecture)

**Purpose**: XML-based markup standard for clinical documents

#### CDA Document Types
- Continuity of Care Document (CCD)
- Consultation Note
- Discharge Summary
- History and Physical
- Operative Note
- Progress Note

### FHIR R4 Enhancement

**Current**: Basic FHIR R4 server (HAPI FHIR)

**Enhancements Needed**:
- IHE Mobile access to Health Documents (MHD)
- IHE Patient Demographics Query for Mobile (PDQm)
- IHE Patient Identity Cross-referencing for Mobile (PIXm)
- US Core Implementation Guide
- Bulk Data Access (Flat FHIR)
- SMART on FHIR authorization

---

## Part 2: Architecture Design

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     External Systems                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │   EHR    │  │   HIE    │  │  Lab     │  │ Imaging  │       │
│  │(HL7 v2)  │  │  (IHE)   │  │System    │  │  (DICOM) │       │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘       │
└───────┼─────────────┼─────────────┼─────────────┼──────────────┘
        │             │             │             │
        │ HL7 v2 TCP  │ SOAP/HTTP   │ FHIR/REST   │ DICOM
        │             │             │             │
┌───────┼─────────────┼─────────────┼─────────────┼──────────────┐
│       │             │             │             │               │
│   ┌───▼──────────────▼─────────────▼─────────────▼────┐        │
│   │      Interoperability Gateway Service             │        │
│   │  ┌──────────┐  ┌──────────┐  ┌──────────┐       │        │
│   │  │ HL7 v2   │  │   IHE    │  │  FHIR    │       │        │
│   │  │ Adapter  │  │ Adapter  │  │ Adapter  │       │        │
│   │  └────┬─────┘  └────┬─────┘  └────┬─────┘       │        │
│   └───────┼─────────────┼─────────────┼──────────────┘        │
│           │             │             │                         │
│   ┌───────▼─────────────▼─────────────▼──────────┐            │
│   │     Message Normalization Layer               │            │
│   │  (Transform all formats to FHIR R4)          │            │
│   └──────────────────┬────────────────────────────┘            │
│                      │                                          │
│   ┌──────────────────▼────────────────────────────┐            │
│   │          Event Bus (Kafka)                     │            │
│   └──────────────────┬────────────────────────────┘            │
│                      │                                          │
│   ┌──────────────────▼────────────────────────────┐            │
│   │      Internal Services                         │            │
│   │  - Quality Measure Service                     │            │
│   │  - Patient Service                             │            │
│   │  - CQL Engine Service                          │            │
│   └───────────────────────────────────────────────┘            │
│                                                                  │
│              HealthData-in-Motion Platform                      │
└──────────────────────────────────────────────────────────────────┘
```

### Component Architecture

#### 1. Interoperability Gateway Service (NEW)

**Purpose**: Single entry point for all external system integration

**Responsibilities**:
- Protocol handling (HL7 v2 TCP, SOAP, REST)
- Message routing and transformation
- Security and authentication
- Audit logging (ATNA compliance)
- Error handling and retry

**Technology Stack**:
- Spring Boot 3.3.5
- Apache Camel (integration framework)
- HAPI HL7v2 library
- IPF (eHealth Integration Framework)
- PostgreSQL (message store)
- Redis (caching, deduplication)

#### 2. HL7 v2 Adapter

**Purpose**: Receive, parse, and transform HL7 v2.x messages

**Key Features**:
- MLLP (Minimal Lower Layer Protocol) listener
- HL7 v2.3, v2.4, v2.5, v2.5.1, v2.6 support
- Message validation
- ACK/NAK response generation
- Transform to FHIR R4

#### 3. IHE Adapter

**Purpose**: Implement IHE profiles (XDS.b, PIX, PDQ, XCA)

**Key Features**:
- SOAP web services (ITI transactions)
- Document registry and repository
- Patient identifier cross-reference manager (PIX)
- Patient demographics query (PDQ)
- Cross-community gateway (XCA)

#### 4. FHIR Adapter

**Purpose**: Enhanced FHIR R4 server with IHE profiles

**Key Features**:
- HAPI FHIR server (already exists)
- IHE MHD (Mobile access to Health Documents)
- IHE PIXm/PDQm (mobile versions)
- US Core IG compliance
- Bulk Data Access API
- SMART on FHIR auth

#### 5. Message Normalization Layer

**Purpose**: Transform all message formats to a canonical FHIR R4 format

**Transformations**:
- HL7 v2 ADT → FHIR Patient, Encounter
- HL7 v2 ORM → FHIR ServiceRequest
- HL7 v2 ORU → FHIR Observation, DiagnosticReport
- HL7 v3 CDA → FHIR DocumentReference, Bundle
- IHE XDS → FHIR DocumentReference

---

## Part 3: Implementation Phases

### Phase 1: Interoperability Gateway Service (Weeks 1-2)

**Objective**: Create the foundation gateway service

#### Tasks

##### 1.1 Project Setup
```bash
# Create new Spring Boot module
cd backend/modules/services
mkdir interoperability-gateway-service
cd interoperability-gateway-service
```

##### 1.2 Dependencies (build.gradle.kts)
```kotlin
dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Apache Camel
    implementation("org.apache.camel.springboot:camel-spring-boot-starter:4.4.0")
    implementation("org.apache.camel.springboot:camel-hl7-starter:4.4.0")
    implementation("org.apache.camel.springboot:camel-fhir-starter:4.4.0")
    implementation("org.apache.camel.springboot:camel-cxf-starter:4.4.0")
    implementation("org.apache.camel.springboot:camel-kafka-starter:4.4.0")

    // HAPI Libraries
    implementation("ca.uhn.hapi:hapi-structures-v25:2.5.1")
    implementation("ca.uhn.hapi:hapi-structures-v26:2.5.1")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-base:7.0.2")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-structures-r4:7.0.2")
    implementation("ca.uhn.hapi.fhir:hapi-fhir-validation:7.0.2")

    // IPF (eHealth Integration Framework)
    implementation("org.openehealth.ipf.platform-camel:ipf-platform-camel-ihe-hl7v2:5.0.0")
    implementation("org.openehealth.ipf.platform-camel:ipf-platform-camel-ihe-xds:5.0.0")

    // Security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")

    // Database
    implementation("org.postgresql:postgresql")
    implementation("org.liquibase:liquibase-core")

    // Monitoring
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
}
```

##### 1.3 Application Configuration
```yaml
server:
  port: 8088

spring:
  application:
    name: interoperability-gateway-service

  datasource:
    url: jdbc:postgresql://postgres:5432/healthdata_interop
    username: healthdata
    password: ${DB_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: none
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

  kafka:
    bootstrap-servers: kafka:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

camel:
  springboot:
    name: InteroperabilityGateway

# HL7 v2 Configuration
hl7:
  listener:
    port: 2575
    bind-address: 0.0.0.0

  supported-versions:
    - 2.3
    - 2.4
    - 2.5
    - 2.5.1
    - 2.6

  timeout:
    connection: 30000
    response: 10000

# IHE Configuration
ihe:
  xds:
    registry-url: ${XDS_REGISTRY_URL:http://localhost:8089/xds-registry}
    repository-url: ${XDS_REPOSITORY_URL:http://localhost:8089/xds-repository}

  pix:
    manager-url: ${PIX_MANAGER_URL:http://localhost:8089/pix-manager}

  pdq:
    supplier-url: ${PDQ_SUPPLIER_URL:http://localhost:8089/pdq-supplier}

# FHIR Configuration
fhir:
  server-url: http://fhir-service-mock:8080/fhir
  version: R4

# Message Storage
message-store:
  retention-days: 90
  archive-enabled: true

# Security
security:
  tls:
    enabled: true
    key-store: ${TLS_KEYSTORE_PATH}
    key-store-password: ${TLS_KEYSTORE_PASSWORD}

  mutual-tls:
    enabled: false
    trust-store: ${TLS_TRUSTSTORE_PATH}
```

##### 1.4 Core Entities
```java
// Message Store Entity
@Entity
@Table(name = "interop_messages")
public class InteropMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String tenantId;
    private String messageType; // HL7v2, IHE_XDS, FHIR
    private String direction; // INBOUND, OUTBOUND

    @Column(columnDefinition = "TEXT")
    private String rawMessage;

    @Column(columnDefinition = "JSONB")
    private String normalizedMessage; // FHIR JSON

    private String sourceSystem;
    private String destinationSystem;

    private String status; // RECEIVED, PROCESSING, TRANSFORMED, DELIVERED, FAILED
    private String errorMessage;

    private LocalDateTime receivedAt;
    private LocalDateTime processedAt;
    private LocalDateTime deliveredAt;

    // Audit fields
    private String sourceIp;
    private String user;
}
```

##### 1.5 Basic Camel Routes
```java
@Component
public class InteroperabilityRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // Error handling
        onException(Exception.class)
            .handled(true)
            .log(LoggingLevel.ERROR, "Error processing message: ${exception.message}")
            .to("direct:error-handler");

        // HL7 v2 Listener
        from("mina2:tcp://0.0.0.0:{{hl7.listener.port}}?sync=true&codec=#hl7Codec")
            .routeId("hl7v2-listener")
            .log("Received HL7 v2 message from ${headers.CamelMina2RemoteAddress}")
            .to("direct:hl7v2-processor");

        // HL7 v2 Processing
        from("direct:hl7v2-processor")
            .routeId("hl7v2-processor")
            .to("bean:messageStoreService?method=saveInbound")
            .to("bean:hl7v2Transformer?method=toFhir")
            .to("direct:kafka-publisher")
            .to("bean:hl7v2AckGenerator?method=generateAck");

        // Kafka Publishing
        from("direct:kafka-publisher")
            .routeId("kafka-publisher")
            .marshal().json()
            .to("kafka:fhir-resources?brokers=kafka:9092");

        // Error Handler
        from("direct:error-handler")
            .routeId("error-handler")
            .to("bean:messageStoreService?method=markFailed")
            .to("bean:alertService?method=sendAlert");
    }
}
```

### Acceptance Criteria (Phase 1)
- [ ] Gateway service starts successfully
- [ ] HL7 v2 TCP listener accepting connections
- [ ] Messages stored in database
- [ ] Basic Camel routes operational
- [ ] Health check endpoint responding
- [ ] Docker image built and tested

**Timeline**: 2 weeks
**Effort**: 2 developers

---

### Phase 2: HL7 v2 Integration (Weeks 3-5)

**Objective**: Full HL7 v2.x message support with FHIR transformation

#### Tasks

##### 2.1 HL7 v2 Message Parser
```java
@Service
public class Hl7v2ParserService {
    private final HapiContext hapiContext;

    public Hl7v2ParserService() {
        hapiContext = new DefaultHapiContext();
        hapiContext.setValidationContext(new NoValidation());
    }

    public Message parse(String messageText) throws HL7Exception {
        Parser parser = hapiContext.getPipeParser();
        return parser.parse(messageText);
    }

    public String getMsgType(Message message) throws HL7Exception {
        return message.get("MSH-9-1").encode();
    }

    public String getTriggerEvent(Message message) throws HL7Exception {
        return message.get("MSH-9-2").encode();
    }
}
```

##### 2.2 ADT Message Handler
```java
@Service
public class AdtMessageHandler {

    public Patient handleA01(ADT_A01 adt) {
        // A01 - Admit Patient
        PID pid = adt.getPID();
        PV1 pv1 = adt.getPV1();

        Patient patient = new Patient();

        // Demographics
        patient.addName()
            .setFamily(pid.getPatientName(0).getFamilyName().getSurname().getValue())
            .addGiven(pid.getPatientName(0).getGivenName().getValue());

        // Identifiers
        patient.addIdentifier()
            .setSystem("MRN")
            .setValue(pid.getPatientIdentifierList(0).getIDNumber().getValue());

        // Birth Date
        String dob = pid.getDateTimeOfBirth().getTime().getValue();
        patient.setBirthDate(parseHL7Date(dob));

        // Gender
        String gender = pid.getAdministrativeSex().getValue();
        patient.setGender(mapGender(gender));

        return patient;
    }

    public Patient handleA08(ADT_A01 adt) {
        // A08 - Update Patient Information
        return handleA01(adt); // Same structure
    }

    public Parameters handleA40(ADT_A39 adt) {
        // A40 - Merge Patient
        Parameters params = new Parameters();

        // Source patient (to be merged from)
        PID sourcePid = adt.getPID();
        params.addParameter("source-patient-identifier",
            new Identifier()
                .setValue(sourcePid.getPatientIdentifierList(0).getIDNumber().getValue()));

        // Target patient (to be merged into)
        MRG mrg = adt.getMRG();
        params.addParameter("target-patient-identifier",
            new Identifier()
                .setValue(mrg.getPriorPatientIdentifierList(0).getIDNumber().getValue()));

        return params;
    }
}
```

##### 2.3 ORM Message Handler (Orders)
```java
@Service
public class OrmMessageHandler {

    public ServiceRequest handleO01(ORM_O01 orm) {
        // O01 - Order Message
        ORC orc = orm.getORDER().getORC();
        OBR obr = orm.getORDER().getORDER_DETAIL().getOBR();

        ServiceRequest serviceRequest = new ServiceRequest();

        // Status
        String orderStatus = orc.getOrderStatus().getValue();
        serviceRequest.setStatus(mapOrderStatus(orderStatus));

        // Intent
        serviceRequest.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);

        // Identifier
        serviceRequest.addIdentifier()
            .setSystem("ORDER_ID")
            .setValue(orc.getPlacerOrderNumber().getEntityIdentifier().getValue());

        // Code (what's being ordered)
        serviceRequest.setCode(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://loinc.org")
                .setCode(obr.getUniversalServiceIdentifier().getIdentifier().getValue())
                .setDisplay(obr.getUniversalServiceIdentifier().getText().getValue())));

        // Subject (patient)
        PID pid = orm.getPATIENT().getPID();
        serviceRequest.setSubject(new Reference()
            .setReference("Patient/" +
                pid.getPatientIdentifierList(0).getIDNumber().getValue()));

        // Priority
        String priority = orc.getOrderControl().getValue();
        serviceRequest.setPriority(mapPriority(priority));

        return serviceRequest;
    }
}
```

##### 2.4 ORU Message Handler (Results)
```java
@Service
public class OruMessageHandler {

    public DiagnosticReport handleR01(ORU_R01 oru) {
        // R01 - Observation Result
        OBR obr = oru.getPATIENT_RESULT().getORDER_OBSERVATION().getOBR();

        DiagnosticReport report = new DiagnosticReport();

        // Status
        report.setStatus(DiagnosticReport.DiagnosticReportStatus.FINAL);

        // Code
        report.setCode(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem("http://loinc.org")
                .setCode(obr.getUniversalServiceIdentifier().getIdentifier().getValue())
                .setDisplay(obr.getUniversalServiceIdentifier().getText().getValue())));

        // Observations
        int obsCount = oru.getPATIENT_RESULT().getORDER_OBSERVATION().getOBSERVATIONReps();
        for (int i = 0; i < obsCount; i++) {
            OBX obx = oru.getPATIENT_RESULT().getORDER_OBSERVATION().getOBSERVATION(i).getOBX();

            Observation obs = new Observation();
            obs.setStatus(Observation.ObservationStatus.FINAL);

            // Code
            obs.setCode(new CodeableConcept()
                .addCoding(new Coding()
                    .setSystem("http://loinc.org")
                    .setCode(obx.getObservationIdentifier().getIdentifier().getValue())
                    .setDisplay(obx.getObservationIdentifier().getText().getValue())));

            // Value
            String valueType = obx.getValueType().getValue();
            String value = obx.getObservationValue(0).encode();

            switch (valueType) {
                case "NM": // Numeric
                    obs.setValue(new Quantity()
                        .setValue(new BigDecimal(value))
                        .setUnit(obx.getUnits().getIdentifier().getValue()));
                    break;
                case "ST": // String
                    obs.setValue(new StringType(value));
                    break;
                case "CE": // Coded Element
                    obs.setValue(new CodeableConcept()
                        .addCoding(new Coding()
                            .setCode(value)));
                    break;
            }

            report.addResult(new Reference().setResource(obs));
        }

        return report;
    }
}
```

##### 2.5 ACK Generator
```java
@Service
public class Hl7v2AckGenerator {

    public String generateAck(Message originalMessage, boolean success, String errorText) throws HL7Exception {
        ACK ack = new ACK();

        // MSH Segment
        MSH msh = ack.getMSH();
        msh.getFieldSeparator().setValue("|");
        msh.getEncodingCharacters().setValue("^~\\&");
        msh.getSendingApplication().getNamespaceID().setValue("HealthDataInMotion");
        msh.getDateTimeOfMessage().getTime().setValue(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        msh.getMessageType().getMessageCode().setValue("ACK");
        msh.getMessageControlID().setValue(UUID.randomUUID().toString());
        msh.getVersionID().getVersionID().setValue("2.5");

        // MSA Segment
        MSA msa = ack.getMSA();
        msa.getAcknowledgmentCode().setValue(success ? "AA" : "AE"); // AA=Accept, AE=Error
        msa.getMessageControlID().setValue(originalMessage.get("MSH-10").encode());

        if (!success && errorText != null) {
            msa.getTextMessage().setValue(errorText);
        }

        // ERR Segment (if error)
        if (!success) {
            ERR err = ack.getERR();
            err.getErrorCodeAndLocation(0).getCodeIdentifyingError().getIdentifier().setValue("207");
            err.getErrorCodeAndLocation(0).getCodeIdentifyingError().getText().setValue(errorText);
        }

        return ack.encode();
    }
}
```

### Acceptance Criteria (Phase 2)
- [ ] All ADT message types parsed and transformed to FHIR
- [ ] ORM messages create ServiceRequest resources
- [ ] ORU messages create Observation/DiagnosticReport resources
- [ ] ACK/NAK responses generated correctly
- [ ] Integration tests with sample HL7 v2 messages
- [ ] Performance: 100 messages/second throughput
- [ ] Error handling for malformed messages

**Timeline**: 3 weeks
**Effort**: 2 developers

---

### Phase 3: IHE Profile Implementation (Weeks 6-9)

**Objective**: Implement IHE XDS.b, PIX, and PDQ profiles

#### 3.1 IHE XDS.b (Cross-Enterprise Document Sharing)

##### Document Registry
```java
@Service
public class XdsRegistryService {

    @WebMethod
    public RegistryResponse registerDocumentSet(SubmitObjectsRequest request) {
        // ITI-42: Register Document Set-b

        for (ExtrinsicObject document : request.getExtrinsicObjects()) {
            DocumentMetadata metadata = extractMetadata(document);

            // Validate metadata
            validateMetadata(metadata);

            // Store in registry
            documentRegistry.save(metadata);

            // Generate unique document ID
            String documentId = generateDocumentId();
            metadata.setUniqueId(documentId);

            // Index for search
            searchIndex.index(metadata);
        }

        return RegistryResponse.builder()
            .status("urn:oasis:names:tc:ebxml-regrep:ResponseStatusType:Success")
            .build();
    }

    @WebMethod
    public AdhocQueryResponse documentRegistryStoredQuery(AdhocQueryRequest request) {
        // ITI-18: Registry Stored Query

        String queryId = request.getAdhocQuery().getId();
        Map<String, String> params = extractQueryParameters(request);

        List<DocumentMetadata> results = switch (queryId) {
            case "urn:uuid:14d4debf-8f97-4251-9a74-a90016b0af0d" ->
                // FindDocuments query
                findDocuments(params);
            case "urn:uuid:5c4f972b-d56b-40ac-a5fc-c8ca9b40b9d4" ->
                // FindSubmissionSets query
                findSubmissionSets(params);
            default ->
                throw new UnsupportedOperationException("Query not supported: " + queryId);
        };

        return buildQueryResponse(results);
    }
}
```

##### Document Repository
```java
@Service
public class XdsRepositoryService {
    private final S3Client s3Client; // Or local storage

    @WebMethod
    public RetrieveDocumentSetResponse retrieveDocumentSet(RetrieveDocumentSetRequest request) {
        // ITI-43: Retrieve Document Set

        RetrieveDocumentSetResponse response = new RetrieveDocumentSetResponse();

        for (DocumentRequest docRequest : request.getDocumentRequests()) {
            String documentId = docRequest.getDocumentUniqueId();
            String repositoryId = docRequest.getRepositoryUniqueId();

            // Retrieve from storage
            byte[] documentContent = s3Client.getObject(
                GetObjectRequest.builder()
                    .bucket("xds-documents")
                    .key(documentId)
                    .build()
            ).readAllBytes();

            // Get metadata
            DocumentMetadata metadata = documentRegistry.findByUniqueId(documentId);

            // Build response
            DocumentResponse docResponse = DocumentResponse.builder()
                .documentUniqueId(documentId)
                .repositoryUniqueId(repositoryId)
                .mimeType(metadata.getMimeType())
                .document(documentContent)
                .build();

            response.addDocumentResponse(docResponse);
        }

        return response;
    }

    @WebMethod
    public RegistryResponse provideAndRegisterDocumentSet(ProvideAndRegisterDocumentSetRequest request) {
        // ITI-41: Provide and Register Document Set-b

        // Store documents
        for (Document document : request.getDocuments()) {
            String documentId = document.getId();
            byte[] content = document.getValue();

            // Store in repository
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket("xds-documents")
                    .key(documentId)
                    .contentType(document.getMimeType())
                    .build(),
                RequestBody.fromBytes(content)
            );
        }

        // Register metadata
        return xdsRegistry.registerDocumentSet(request.getSubmitObjectsRequest());
    }
}
```

#### 3.2 IHE PIX (Patient Identifier Cross-Referencing)

```java
@Service
public class PixManagerService {

    @WebMethod
    public PRPA_IN201310UV02 pixQuery(PRPA_IN201309UV02 request) {
        // ITI-9: PIX Query

        // Extract source patient identifier
        PatientIdentifier sourceId = extractPatientIdentifier(request);

        // Query cross-reference database
        List<PatientIdentifier> crossReferencedIds =
            pixRepository.findCrossReferencedIdentifiers(sourceId);

        // Build response
        PRPA_IN201310UV02 response = new PRPA_IN201310UV02();

        for (PatientIdentifier id : crossReferencedIds) {
            response.addPatientIdentifier(id);
        }

        return response;
    }

    @WebMethod
    public MCCI_IN000002UV01 pixFeed(PRPA_IN201301UV02 feed) {
        // ITI-8: Patient Identity Feed

        PatientIdentifier patientId = extractPatientIdentifier(feed);
        Demographics demographics = extractDemographics(feed);

        // Find matches using probabilistic matching
        List<PatientMatch> matches = patientMatchingEngine.findMatches(demographics);

        if (matches.isEmpty()) {
            // Create new cross-reference entry
            pixRepository.createNewEntry(patientId, demographics);
        } else if (matches.size() == 1 && matches.get(0).getScore() > 0.95) {
            // Link to existing patient
            pixRepository.linkIdentifier(matches.get(0).getPatientId(), patientId);
        } else {
            // Potential duplicates - flag for manual review
            duplicateReviewService.flagForReview(patientId, matches);
        }

        // Send acknowledgment
        return generateAcknowledgment(true);
    }
}
```

#### 3.3 IHE PDQ (Patient Demographics Query)

```java
@Service
public class PdqSupplierService {

    @WebMethod
    public PRPA_IN201306UV02 patientDemographicsQuery(PRPA_IN201305UV02 request) {
        // ITI-47: Patient Demographics Query

        // Extract query parameters
        QueryParameters params = extractQueryParameters(request);

        // Build search criteria
        PatientSearchCriteria criteria = PatientSearchCriteria.builder()
            .familyName(params.getFamilyName())
            .givenName(params.getGivenName())
            .birthDate(params.getBirthDate())
            .gender(params.getGender())
            .identifier(params.getIdentifier())
            .build();

        // Search patient database
        List<Patient> patients = patientRepository.search(criteria);

        // Build response
        PRPA_IN201306UV02 response = new PRPA_IN201306UV02();

        for (Patient patient : patients) {
            response.addPatient(buildPatientRecord(patient));
        }

        response.setTotalResults(patients.size());
        response.setContinuationPointer(
            patients.size() > params.getMaxResults() ?
                generateContinuationPointer(criteria, params.getMaxResults()) :
                null
        );

        return response;
    }
}
```

### Acceptance Criteria (Phase 3)
- [ ] XDS.b document registry operational
- [ ] XDS.b document repository storing/retrieving documents
- [ ] PIX manager cross-referencing patient identifiers
- [ ] PDQ supplier responding to demographic queries
- [ ] SOAP endpoints tested with SoapUI
- [ ] Integration tests with IHE Gazelle test tools
- [ ] Performance: 50 transactions/second

**Timeline**: 4 weeks
**Effort**: 2-3 developers

---

### Phase 4: FHIR Enhancement & IHE Mobile Profiles (Weeks 10-11)

**Objective**: Enhance FHIR server with IHE mobile profiles and US Core

#### 4.1 IHE MHD (Mobile access to Health Documents)

```java
@RestController
@RequestMapping("/fhir")
public class MhdController {

    @PostMapping("/DocumentManifest")
    public ResponseEntity<DocumentManifest> submitDocumentManifest(
        @RequestBody DocumentManifest manifest) {
        // ITI-105: Simplified Publish

        // Validate manifest
        ValidationResult validation = fhirValidator.validate(manifest);
        if (!validation.isSuccess()) {
            return ResponseEntity.badRequest().build();
        }

        // Store documents referenced in manifest
        for (Reference docRef : manifest.getContent()) {
            DocumentReference document = (DocumentReference) docRef.getResource();
            storeDocument(document);
        }

        // Register in XDS registry (bridge to XDS.b)
        xdsBridgeService.registerDocumentSet(manifest);

        return ResponseEntity.created(URI.create("/fhir/DocumentManifest/" + manifest.getId()))
            .body(manifest);
    }

    @GetMapping("/DocumentReference")
    public ResponseEntity<Bundle> findDocumentReferences(
        @RequestParam(required = false) String patient,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String type) {
        // ITI-67: Find Document References

        SearchParameterMap params = new SearchParameterMap();
        if (patient != null) params.add("patient", new TokenParam(patient));
        if (category != null) params.add("category", new TokenParam(category));
        if (type != null) params.add("type", new TokenParam(type));

        IBundleProvider results = fhirRepository.search(params);

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal(results.size());

        for (IBaseResource resource : results.getResources(0, results.size())) {
            bundle.addEntry()
                .setResource((DocumentReference) resource)
                .setFullUrl("/fhir/DocumentReference/" + resource.getIdElement().getIdPart());
        }

        return ResponseEntity.ok(bundle);
    }
}
```

#### 4.2 IHE PIXm/PDQm (Mobile Patient Identity Management)

```java
@RestController
@RequestMapping("/fhir")
public class PixmPdqmController {

    @GetMapping("/Patient/$ihe-pix")
    public ResponseEntity<Parameters> pixmQuery(
        @RequestParam String sourceIdentifier,
        @RequestParam(required = false) String targetSystem) {
        // ITI-83: Mobile Patient Identifier Cross-reference Query

        // Parse source identifier
        Identifier source = parseIdentifier(sourceIdentifier);

        // Query PIX manager
        List<Identifier> crossRefs = pixManager.getCrossReferences(source, targetSystem);

        // Build Parameters response
        Parameters params = new Parameters();
        params.setId(UUID.randomUUID().toString());

        for (Identifier id : crossRefs) {
            params.addParameter()
                .setName("targetIdentifier")
                .setValue(id);
        }

        return ResponseEntity.ok(params);
    }

    @GetMapping("/Patient")
    public ResponseEntity<Bundle> pdqmQuery(
        @RequestParam(required = false) String family,
        @RequestParam(required = false) String given,
        @RequestParam(required = false) String birthdate,
        @RequestParam(required = false) String identifier) {
        // ITI-78: Mobile Patient Demographics Query

        SearchParameterMap params = new SearchParameterMap();
        if (family != null) params.add("family", new StringParam(family));
        if (given != null) params.add("given", new StringParam(given));
        if (birthdate != null) params.add("birthdate", new DateParam(birthdate));
        if (identifier != null) params.add("identifier", new TokenParam(identifier));

        IBundleProvider results = patientRepository.search(params);

        return ResponseEntity.ok(buildSearchBundle(results));
    }
}
```

#### 4.3 US Core Implementation Guide

```java
@Configuration
public class UsCoreFhirConfig {

    @Bean
    public IValidatorModule usCoreValidator() {
        FhirInstanceValidator instanceValidator = new FhirInstanceValidator();

        // Load US Core profiles
        NPMPackageValidationSupport npmSupport = new NPMPackageValidationSupport(fhirContext);
        npmSupport.loadPackageFromClasspath("classpath:packages/hl7.fhir.us.core-6.1.0.tgz");

        ValidationSupportChain validationSupport = new ValidationSupportChain(
            new DefaultProfileValidationSupport(fhirContext),
            npmSupport,
            new InMemoryTerminologyServerValidationSupport(fhirContext),
            new CommonCodeSystemsTerminologyService(fhirContext)
        );

        CachingValidationSupport cachingSupport = new CachingValidationSupport(validationSupport);
        instanceValidator.setValidationSupport(cachingSupport);

        return instanceValidator;
    }
}
```

### Acceptance Criteria (Phase 4)
- [ ] IHE MHD endpoints functional
- [ ] PIXm/PDQm mobile queries working
- [ ] US Core validation enabled
- [ ] SMART on FHIR authorization (OAuth 2.0)
- [ ] Bulk Data Access API implemented
- [ ] Mobile app can query via MHD/PIXm/PDQm

**Timeline**: 2 weeks
**Effort**: 2 developers

---

## Part 4: Deployment & Operations

### Docker Configuration

```yaml
# docker-compose.yml additions
services:
  interoperability-gateway:
    image: healthdata/interoperability-gateway:latest
    container_name: healthdata-interop-gateway
    restart: unless-stopped
    ports:
      - "8088:8088"  # HTTP API
      - "2575:2575"  # HL7 v2 MLLP
      - "8089:8089"  # IHE SOAP services
    depends_on:
      - postgres
      - kafka
      - fhir-service-mock
    environment:
      SPRING_PROFILES_ACTIVE: docker
      DB_PASSWORD: dev_password
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      FHIR_SERVER_URL: http://fhir-service-mock:8080/fhir
    volumes:
      - ./logs/interop-gateway:/app/logs
    networks:
      - healthdata-network
```

### Monitoring & Alerting

```java
@Component
public class InteropMetrics {
    private final MeterRegistry registry;

    public void recordMessageReceived(String messageType) {
        registry.counter("interop.messages.received",
            "type", messageType
        ).increment();
    }

    public void recordTransformationTime(String sourceFormat, String targetFormat, long milliseconds) {
        registry.timer("interop.transformation.time",
            "source", sourceFormat,
            "target", targetFormat
        ).record(milliseconds, TimeUnit.MILLISECONDS);
    }

    public void recordError(String component, String errorType) {
        registry.counter("interop.errors",
            "component", component,
            "error", errorType
        ).increment();
    }
}
```

### Security Considerations

1. **TLS/mTLS**: All external connections use TLS 1.3
2. **Authentication**: OAuth 2.0 for FHIR, WS-Security for SOAP
3. **Authorization**: Role-based access control
4. **Audit Logging**: ATNA (Audit Trail and Node Authentication)
5. **Data Encryption**: PHI encrypted at rest (AES-256)

---

## Part 5: Testing Strategy

### Integration Testing

```java
@SpringBootTest
@AutoConfigureMockMvc
class Hl7v2IntegrationTest {

    @Test
    void testAdtA01MessageTransformation() {
        // Given: HL7 v2 ADT A01 message
        String hl7Message = "MSH|^~\\&|SENDING_APP|SENDING_FAC|RECEIVING_APP|RECEIVING_FAC|20250127120000||ADT^A01|MSG001|P|2.5\r" +
            "PID|1||12345^^^MRN||Doe^John^A||19800101|M|||123 Main St^^Springfield^IL^62701||555-1234\r" +
            "PV1|1|I|ICU^101^1|||DOC001^Smith^Jane^^^Dr|||MED||||1|||DOC001|||||||||||||||||||||||20250127080000";

        // When: Send to HL7 listener
        String ack = sendHl7Message(hl7Message);

        // Then: Verify ACK received
        assertThat(ack).contains("MSA|AA|MSG001");

        // And: Verify FHIR Patient created
        await().atMost(5, SECONDS).untilAsserted(() -> {
            Patient patient = fhirClient.read()
                .resource(Patient.class)
                .withId("12345")
                .execute();

            assertThat(patient.getNameFirstRep().getFamily()).isEqualTo("Doe");
            assertThat(patient.getNameFirstRep().getGivenAsSingleString()).isEqualTo("John");
            assertThat(patient.getGender()).isEqualTo(Enumerations.AdministrativeGender.MALE);
        });
    }
}
```

### IHE Conformance Testing

- **IHE Gazelle**: Connect to IHE Connectathon test infrastructure
- **Test Scenarios**: Pre-built test cases for each IHE profile
- **Validation**: Message validation against IHE specifications

---

## Part 6: Timeline & Resources

### Overall Timeline

| Phase | Duration | Start | End | Dependencies |
|-------|----------|-------|-----|--------------|
| Phase 1 | 2 weeks | Week 1 | Week 2 | - |
| Phase 2 | 3 weeks | Week 3 | Week 5 | Phase 1 |
| Phase 3 | 4 weeks | Week 6 | Week 9 | Phase 2 |
| Phase 4 | 2 weeks | Week 10 | Week 11 | Phase 3 |

**Total Duration**: 11 weeks

### Resource Requirements

**Development Team**:
- 2-3 Senior Backend Developers (HL7/FHIR expertise)
- 1 Integration Engineer (IHE profiles)
- 1 DevOps Engineer (deployment)
- 1 QA Engineer (testing strategy)

**Infrastructure**:
- Development: Existing + HL7 test tools
- Staging: Full integration with test EHR/HIE
- Production: HL7 interface engine (Mirth/Rhapsody) optional

**Budget**:
- Development: 11 weeks × 3 devs = 33 dev-weeks
- IHE Connectathon registration: $1,000-5,000
- Test tools (SoapUI Pro, etc.): $500-1,000/month
- Infrastructure: $200-500/month

---

## Part 7: Success Metrics

### Technical Metrics
- HL7 v2 message processing: >100 messages/second
- FHIR API response time: <200ms (p95)
- IHE transaction throughput: >50 transactions/second
- Message transformation accuracy: >99.9%
- System uptime: >99.9%

### Business Metrics
- Number of integrated systems: Track adoption
- Data exchange volume: Monitor growth
- Integration time: <2 weeks per new system
- Error rate: <0.1% for message processing
- Customer satisfaction: >4.5/5 on integration experience

---

## Part 8: Risk Mitigation

### High Risks
1. **HL7 v2 Variations**: Mitigated by flexible parsing, configuration per sender
2. **PIX Duplicate Patients**: Mitigated by probabilistic matching, manual review queue
3. **Performance Under Load**: Mitigated by load testing, horizontal scaling
4. **Legacy System Integration**: Mitigated by adapter pattern, extensive testing

### Medium Risks
1. **IHE Profile Complexity**: Use IPF framework, IHE test tools
2. **Security Vulnerabilities**: Regular audits, penetration testing
3. **Data Quality Issues**: Validation, cleansing, monitoring

---

## Conclusion

This implementation plan provides a complete roadmap for healthcare interoperability using IHE and HL7 standards. The phased approach allows for incremental delivery while building toward full HIE integration capability.

**Key Differentiators**:
- **Multi-Standard Support**: HL7 v2, v3, FHIR, IHE profiles
- **Normalization to FHIR**: Single canonical format internally
- **Scalable Architecture**: Kafka-based event streaming
- **Production-Ready**: Security, monitoring, audit logging

**Next Actions**:
1. Stakeholder approval
2. Resource allocation
3. Development environment setup
4. Phase 1 kickoff

**Status**: Ready for Implementation
**Last Updated**: November 27, 2025
**Version**: 1.0.0
