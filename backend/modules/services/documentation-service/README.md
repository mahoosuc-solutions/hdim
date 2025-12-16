# Documentation Service

Clinical documentation management service for storing, versioning, and retrieving healthcare documents including clinical notes, CDA documents, and attachments.

## Overview

The Documentation Service provides comprehensive document management capabilities for clinical documentation. It supports structured clinical documents (HL7 CDA), unstructured clinical notes, version control, attachments, and full-text search across document content.

## Key Features

### Clinical Document Management
- Create, read, update, delete (CRUD) operations
- Patient-specific document retrieval
- Document type categorization
- Multi-tenant document isolation
- Document status tracking (draft, final, amended)

### Version Control
- Automatic versioning on updates
- Version history tracking
- Compare versions
- Restore previous versions
- Version-specific metadata

### Document Attachments
- Associate files with clinical documents
- Support for images, PDFs, and other file types
- Attachment metadata (file name, size, mime type)
- Secure attachment storage
- Attachment-level access control

### Full-Text Search
- Search across document content
- Filter by patient, type, status
- Pagination support for large result sets
- Relevance-based ranking
- Multi-field search capabilities

### CDA (Clinical Document Architecture) Support
- HL7 CDA R2 document storage
- CDA section parsing
- Structured data extraction
- CDA validation and conformance
- Transform CDA to FHIR documents

### Document Templates
- Reusable document templates
- Template versioning
- Organization-specific templates
- Template preview and validation

## Technology Stack

- **Spring Boot 3.x**: Core framework
- **PostgreSQL**: Document storage and metadata
- **Redis**: Document caching
- **Liquibase**: Database migrations
- **Resilience4j**: Circuit breakers and retry logic

## API Endpoints

### Clinical Documents
```
GET    /api/documents/clinical
       - Get all documents for tenant

GET    /api/documents/clinical/paginated
       - Get paginated document list

GET    /api/documents/clinical/{id}
       - Get specific document

GET    /api/documents/clinical/patient/{patientId}
       - Get all documents for a patient

GET    /api/documents/clinical/patient/{patientId}/paginated
       - Get paginated patient documents

GET    /api/documents/clinical/search?query={query}
       - Search documents by content

POST   /api/documents/clinical
       - Create new clinical document

PUT    /api/documents/clinical/{id}
       - Update document (creates new version)

DELETE /api/documents/clinical/{id}
       - Delete document (soft delete)
```

### Document Attachments
```
POST   /api/documents/clinical/{id}/attachments
       - Add attachment to document

GET    /api/documents/clinical/attachments/{attachmentId}
       - Get specific attachment

DELETE /api/documents/clinical/attachments/{attachmentId}
       - Remove attachment
```

### Product Documentation
```
GET    /api/documents/product
       - Get product documentation

POST   /api/documents/product
       - Create product documentation

PUT    /api/documents/product/{id}
       - Update product documentation

DELETE /api/documents/product/{id}
       - Delete product documentation
```

## Configuration

### Application Properties
```yaml
server.port: 8091
spring.datasource.url: jdbc:postgresql://localhost:5432/healthdata_docs
spring.cache.type: redis
spring.liquibase.enabled: true
```

### Storage Configuration
- Document content stored in PostgreSQL TEXT fields
- Binary attachments stored in BYTEA columns
- Optional S3/object storage integration for large files

### Resilience
```yaml
resilience4j:
  circuitbreaker:
    instances:
      documentationDefault:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
```

## Running Locally

### Prerequisites
- Java 21
- PostgreSQL 14+
- Redis 6+

### Build
```bash
./gradlew :modules:services:documentation-service:build
```

### Run
```bash
./gradlew :modules:services:documentation-service:bootRun
```

### Run Tests
```bash
./gradlew :modules:services:documentation-service:test
```

## Document Types

### Supported Document Types
- Clinical Notes (Progress Notes, Discharge Summaries)
- CDA Documents (Continuity of Care Document - CCD)
- Lab Reports
- Imaging Reports
- Consultation Notes
- Operative Reports
- Patient Instructions
- Care Plans

### Document Status
- DRAFT: Document in progress
- FINAL: Completed and signed
- AMENDED: Modified after finalization
- ENTERED_IN_ERROR: Marked as incorrect
- DEPRECATED: Superseded by newer version

## Version Control

### Versioning Strategy
- Automatic version increment on updates
- Major version: Significant changes
- Minor version: Minor corrections
- Version metadata includes author, timestamp, change reason

### Version History
```java
// Example: Get document versions
GET /api/documents/clinical/{id}/versions
    Returns: List of all versions with metadata
```

## Security

### Access Control
- JWT-based authentication
- Role-based access (USER, CLINICIAN, ADMIN)
- Tenant isolation via X-Tenant-ID header
- Document-level permissions
- Audit trail for all operations

### HIPAA Compliance
- Encrypted data at rest
- TLS encryption in transit
- Comprehensive audit logging
- Automatic PHI detection
- Retention policy enforcement

## Search Capabilities

### Full-Text Search
- PostgreSQL full-text search (tsvector)
- Weighted search across fields
- Fuzzy matching support
- Stop word filtering
- Stemming and normalization

### Search Filters
- Patient ID
- Document type
- Date range
- Author/Provider
- Document status

## Integration

This service integrates with:
- **FHIR Service**: Convert documents to FHIR DocumentReference
- **Authentication Service**: User and tenant validation
- **Audit Service**: Document access logging
- **AI Assistant Service**: Document summarization

## CDA Support

### CDA Document Handling
- Parse HL7 CDA XML
- Extract structured sections
- Validate against CDA schema
- Transform to FHIR Composition
- Support for CDA templates (CCD, Discharge Summary)

## API Documentation

Swagger UI available at:
```
http://localhost:8091/swagger-ui.html
```

## Monitoring

Actuator endpoints:
- Health: `/actuator/health`
- Metrics: `/actuator/metrics`
- Prometheus: `/actuator/prometheus`

## Performance Optimization

### Caching Strategy
- Redis cache for frequently accessed documents
- Cache invalidation on updates
- Patient-level cache prefetching

### Database Optimization
- Indexed fields: patient_id, document_type, created_at
- Partitioning by tenant for large deployments
- Connection pooling with HikariCP

## License

Copyright (c) 2024 Mahoosuc Solutions
