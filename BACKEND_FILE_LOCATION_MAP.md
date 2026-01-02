# Backend File Location Map

## Complete Directory Structure for Evaluations, Results & Reports

### CQL Engine Service - Evaluations

**Base Path**: `backend/modules/services/cql-engine-service/src/main/java/com/healthdata/cql/`

#### Controllers (REST Endpoints)
```
controller/
├── CqlEvaluationController.java
│   ├── 20+ endpoints for CRUD and querying
│   ├── POST /api/v1/cql/evaluations
│   ├── GET /api/v1/cql/evaluations
│   ├── POST /api/v1/cql/evaluations/batch
│   └── DELETE /api/v1/cql/evaluations/old
├── CqlLibraryController.java
├── ValueSetController.java
└── [other controllers...]
```

#### Services (Business Logic)
```
service/
├── CqlEvaluationService.java
│   ├── createEvaluation()
│   ├── executeEvaluation()
│   ├── getEvaluationsForPatient()
│   ├── getEvaluationsForLibrary()
│   ├── batchEvaluate()
│   ├── retryEvaluation()
│   └── deleteOldEvaluations()
├── CqlLibraryService.java
├── ValueSetService.java
├── TemplateCacheService.java
└── [other services...]
```

#### Entities (Database Models)
```
entity/
├── CqlEvaluation.java
│   ├── id: UUID
│   ├── tenant_id: String
│   ├── library_id: UUID (FK)
│   ├── patient_id: String
│   ├── evaluation_result: JSON
│   ├── status: PENDING|SUCCESS|FAILED
│   └── [other fields...]
├── CqlLibrary.java
└── ValueSet.java
```

#### Repositories (Data Access)
```
repository/
├── CqlEvaluationRepository.java
│   ├── findByTenantId()
│   ├── findByTenantIdAndPatientId()
│   ├── findLatestByPatientAndLibrary()
│   ├── countByTenantIdAndStatus()
│   ├── getAverageDurationForLibrary()
│   └── [27+ other query methods...]
├── CqlLibraryRepository.java
└── ValueSetRepository.java
```

#### Engine
```
engine/
├── MeasureTemplateEngine.java
│   ├── evaluateMeasure()
│   ├── evaluateBatch()
│   └── [concurrent evaluation logic...]
└── TemplateCacheService.java
```

---

### Quality Measure Service - Results & Reports

**Base Path**: `backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/`

#### Controllers (REST Endpoints)
```
controller/
└── QualityMeasureController.java
    ├── POST /quality-measure/calculate
    ├── GET /quality-measure/results
    ├── GET /quality-measure/score
    ├── GET /quality-measure/report/patient
    ├── GET /quality-measure/report/population
    └── GET /quality-measure/_health
```

#### Services (Business Logic)
```
service/
├── MeasureCalculationService.java
│   ├── calculateMeasure()
│   ├── getPatientMeasureResults()
│   ├── getAllMeasureResults()
│   ├── getQualityScore()
│   ├── publishCalculationEvent() [Kafka]
│   └── [extraction helper methods...]
└── QualityReportService.java
    ├── getPatientQualityReport()
    │   └── Returns: QualityReport record
    ├── getPopulationQualityReport()
    │   └── Returns: PopulationQualityReport record
    └── [in-memory report generation...]
```

#### Entities (Database Models)
```
persistence/
├── QualityMeasureResultEntity.java
│   ├── id: UUID
│   ├── tenant_id: String
│   ├── patient_id: UUID
│   ├── measure_id: String
│   ├── measure_category: HEDIS|CMS|custom
│   ├── numerator_compliant: Boolean
│   ├── denominator_elligible: Boolean
│   ├── score: Double
│   ├── cql_result: JSON
│   └── [audit fields...]
└── [no entity for in-memory reports]
```

#### Repositories (Data Access)
```
persistence/
└── QualityMeasureResultRepository.java
    ├── findByTenantIdAndPatientId()
    ├── findByPatientAndMeasure()
    ├── findByMeasureYear()
    ├── countCompliantMeasures()
    ├── findByTenantIdWithPagination()
    └── [other query methods...]
```

#### DTOs (API Response Objects)
```
dto/
├── QualityMeasureResultDTO.java
│   ├── Excludes: cql_result (JSONB)
│   ├── Includes: OpenAPI annotations
│   └── [all result fields...]
├── QualityMeasureResultMapper.java
│   ├── toDTO()
│   └── toDTOList()
└── MeasureCalculationRequest.java
```

#### Clients (Feign Integrations)
```
client/
├── CqlEngineServiceClient.java
│   └── evaluateCql()
├── CareGapServiceClient.java
│   └── getCareGapSummary()
└── PatientServiceClient.java
    └── [patient data retrieval...]
```

#### Models & Measures
```
model/
├── MeasureResult.java
├── SubMeasureResult.java
├── CareGap.java
├── Recommendation.java
└── PatientData.java

measure/
├── MeasureCalculator.java
├── DiabetesCareCalculator.java
└── MeasureRegistry.java
```

---

### Care Gap Service - Results & Reports

**Base Path**: `backend/modules/services/care-gap-service/src/main/java/com/healthdata/caregap/`

#### Controllers (REST Endpoints)
```
controller/
└── CareGapController.java
    ├── POST /care-gap/identify
    ├── POST /care-gap/identify/{library}
    ├── POST /care-gap/refresh
    ├── POST /care-gap/close
    ├── GET /care-gap/open
    ├── GET /care-gap/high-priority
    ├── GET /care-gap/overdue
    ├── GET /care-gap/upcoming
    ├── GET /care-gap/summary
    ├── GET /care-gap/stats
    ├── GET /care-gap/by-category
    ├── GET /care-gap/by-priority
    ├── GET /care-gap/population-report
    └── GET /care-gap/_health
```

#### Services (Business Logic)
```
service/
├── CareGapIdentificationService.java
│   ├── identifyAllCareGaps()
│   ├── identifyCareGapsForLibrary()
│   ├── refreshCareGaps()
│   ├── closeCareGap()
│   ├── getOpenCareGaps()
│   ├── getHighPriorityCareGaps()
│   ├── getCareGapStats()
│   └── [gap management logic...]
└── CareGapReportService.java
    ├── getCareGapSummary()
    │   └── Returns: CareGapSummary record
    ├── getGapsByMeasureCategory()
    ├── getGapsByPriority()
    ├── getOverdueGaps()
    ├── getUpcomingGaps()
    ├── getPopulationGapReport()
    │   └── Returns: PopulationGapReport record
    └── [in-memory report generation...]
```

#### Entities (Database Models)
```
persistence/
├── CareGapEntity.java
│   ├── id: UUID
│   ├── tenant_id: String
│   ├── patient_id: UUID
│   ├── measure_id: String
│   ├── measure_category: HEDIS|CMS|custom
│   ├── gap_type: String
│   ├── gap_status: open|in-progress|closed|cancelled
│   ├── priority: high|medium|low
│   ├── risk_score: Double
│   ├── identified_date: LocalDate
│   ├── due_date: LocalDate
│   ├── closed_date: LocalDate
│   ├── recommendation: TEXT
│   ├── cql_result: JSON
│   ├── related_encounter_id: UUID (FHIR)
│   ├── related_condition_id: UUID (FHIR)
│   ├── related_procedure_id: UUID (FHIR)
│   └── [audit fields...]
└── [no entity for in-memory reports]
```

#### Repositories (Data Access)
```
persistence/
└── CareGapRepository.java
    ├── findByTenantIdAndPatientId()
    ├── findOpenGapsByPatient()
    ├── findHighPriorityOpenGaps()
    ├── findClosedGapsByPatient()
    ├── findByMeasure()
    ├── findByMeasureCategory()
    ├── findOverdueGaps()
    ├── findGapsDueInRange()
    ├── countOpenGaps()
    ├── countByStatus()
    ├── countByPriority()
    ├── countByMeasureCategory()
    ├── hasOpenGapForMeasure()
    └── [67+ total query methods...]
```

#### Clients (Feign Integrations)
```
client/
├── CqlEngineServiceClient.java
│   └── evaluateCql()
└── PatientServiceClient.java
    └── [patient data retrieval...]
```

#### Configuration
```
config/
├── CareGapSecurityConfig.java
└── [other configs...]
```

---

## Test Files Location

### Quality Measure Service Tests
```
backend/modules/services/quality-measure-service/src/test/java/com/healthdata/quality/

integration/
├── PopulationReportApiIntegrationTest.java
├── PatientReportApiIntegrationTest.java
├── ResultsApiIntegrationTest.java
├── CachingBehaviorIntegrationTest.java
├── ErrorHandlingIntegrationTest.java
└── [other tests...]
```

---

## Configuration Files

### Application Properties
```
backend/modules/services/{service}/src/main/resources/
├── application.yml
├── application-dev.yml
├── application-prod.yml
└── [service-specific configs...]
```

### Security Configurations
```
{service}/config/
├── [ServiceName]SecurityConfig.java
└── [other security...]

Examples:
- CqlSecurityCustomizer.java
- FhirSecurityConfig.java
- QualityMeasureSecurityConfig.java
- CareGapSecurityConfig.java
```

### Database Migrations (Liquibase)
```
backend/modules/services/{service}/src/main/resources/db/changelog/
├── [Service-specific changelogs...]
```

---

## Key Integration Points

### Between Services
```
Quality Measure Service
  ├→ CqlEngineServiceClient
  │   └→ POST /api/v1/cql/evaluations/{id}/execute
  ├→ CareGapServiceClient
  │   └→ GET /care-gap/summary?patient=...
  └→ PatientServiceClient
      └→ GET /patient/{id}

Care Gap Service
  ├→ CqlEngineServiceClient
  │   └→ POST /api/v1/cql/evaluations/{id}/execute
  └→ PatientServiceClient
      └→ GET /patient/{id}
```

### Event Publishing
```
Quality Measure Service
  └→ Kafka Topic: measure-calculated
      ├── Event Format: {tenantId, patientId, measureId, timestamp}
      └── Consumed by: [Analytics service, reporting service, etc.]
```

---

## Report Data Flow (In-Memory)

### Quality Report Generation
```
QualityMeasureController.getPatientQualityReport()
  └→ QualityReportService.getPatientQualityReport()
      ├→ MeasureCalculationService.getPatientMeasureResults()
      ├→ MeasureCalculationService.getQualityScore()
      ├→ CareGapServiceClient.getCareGapSummary()
      └→ Returns: QualityReport record (in-memory)
```

### Care Gap Report Generation
```
CareGapController.getCareGapSummary()
  └→ CareGapReportService.getCareGapSummary()
      ├→ CareGapRepository.findOpenGapsByPatient()
      ├→ CareGapRepository.findClosedGapsByPatient()
      ├→ CareGapRepository.countOverdueGaps()
      └→ Returns: CareGapSummary record (in-memory)
```

---

## Database Schema Locations

### Schema Definitions
```
Liquibase Migrations (per service):
- CQL Engine Service: cql_evaluations, cql_libraries, value_sets tables
- Quality Measure Service: quality_measure_results table
- Care Gap Service: care_gaps table
```

### Key Tables
```
1. cql_evaluations
   - Stores CQL expression evaluation results
   - Indexed on: tenant_id, library_id, patient_id, evaluation_date

2. quality_measure_results
   - Stores calculated quality measure results
   - Indexed on: tenant_id, patient_id, measure_id

3. care_gaps
   - Stores identified care gaps
   - Indexed on: tenant_id, patient_id, gap_status, priority, due_date

4. cql_libraries
   - Referenced by cql_evaluations
   - Stores CQL library definitions

5. value_sets
   - Referenced by CQL evaluations
   - Stores medical terminology value sets
```

---

## Build & Deployment

### Maven Structure
```
backend/pom.xml (root)
└── modules/pom.xml
    └── services/pom.xml
        ├── cql-engine-service/pom.xml
        ├── quality-measure-service/pom.xml
        ├── care-gap-service/pom.xml
        ├── patient-service/pom.xml
        ├── fhir-service/pom.xml
        ├── consent-service/pom.xml
        ├── event-processing-service/pom.xml
        └── analytics-service/pom.xml
```

### Docker
```
Each service has:
- Dockerfile
- docker-compose.yml (for local development)
```

---

## Access Patterns by Role

### ANALYST (Read-only)
```
GET /api/v1/cql/evaluations
GET /api/v1/cql/evaluations/{id}
GET /quality-measure/results
GET /quality-measure/score
GET /quality-measure/report/*
GET /care-gap/open
GET /care-gap/summary
GET /care-gap/population-report
[No POST, PUT, DELETE]
```

### EVALUATOR (Read + Execute)
```
[All ANALYST endpoints]
POST /api/v1/cql/evaluations
POST /api/v1/cql/evaluations/{id}/execute
POST /api/v1/cql/evaluations/batch
POST /quality-measure/calculate
POST /care-gap/identify
POST /care-gap/close
[No DELETE]
```

### ADMIN (All operations)
```
[All EVALUATOR endpoints]
DELETE /api/v1/cql/evaluations/old
[All modifications for their tenant]
```

### SUPER_ADMIN (All operations)
```
[All operations across all tenants]
```

---

## Summary of File Counts

### CQL Engine Service
- Controllers: 4
- Services: 4+
- Entities: 3
- Repositories: 3
- Total Custom Classes: 15+

### Quality Measure Service
- Controllers: 1
- Services: 2
- Entities: 1
- Repositories: 1
- DTOs/Mappers: 3
- Clients: 3
- Total Custom Classes: 11+

### Care Gap Service
- Controllers: 1
- Services: 2
- Entities: 1
- Repositories: 1
- Clients: 2
- Total Custom Classes: 7+

**Total Backend Classes**: 30+

---

