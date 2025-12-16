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
