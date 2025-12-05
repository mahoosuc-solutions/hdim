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

## TDD Development

This service was built following Test-Driven Development (TDD) methodology:

### Test Suite: 143+ Tests
- **ClinicalNoteExtractorTest**: 22 tests
- **MedicalEntityRecognizerTest**: 15 tests
- **ICD10ValidatorTest**: 15 tests
- **SnomedValidatorTest**: 10 tests
- **CptValidatorTest**: 10 tests
- **LoincValidatorTest**: 10 tests
- **CodeSuggesterTest**: 15 tests
- **DataCompletenessAnalyzerTest**: 15 tests
- **DataQualityServiceTest**: 10 tests
- **DataEnrichmentControllerTest**: 15 tests
- **HierarchicalCodeSearchTest**: 6 tests

All tests were written FIRST before implementation, ensuring production-ready code quality.

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
