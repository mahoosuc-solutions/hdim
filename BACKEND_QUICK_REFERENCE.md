# Backend Implementation Quick Reference

## File Locations

### CQL Engine Service (Evaluations)
```
backend/modules/services/cql-engine-service/src/main/java/com/healthdata/cql/
├── controller/
│   └── CqlEvaluationController.java              [20+ REST endpoints]
├── service/
│   └── CqlEvaluationService.java                 [Full evaluation logic]
├── entity/
│   └── CqlEvaluation.java                        [JPA entity, cql_evaluations table]
├── repository/
│   └── CqlEvaluationRepository.java              [30+ query methods]
└── measure/
    └── MeasureTemplateEngine.java                [CQL execution engine]
```

### Quality Measure Service (Results & Reports)
```
backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/
├── controller/
│   └── QualityMeasureController.java             [6 REST endpoints]
├── service/
│   ├── MeasureCalculationService.java            [Result calculation]
│   └── QualityReportService.java                 [Report generation (in-memory)]
├── persistence/
│   ├── QualityMeasureResultEntity.java           [JPA entity]
│   └── QualityMeasureResultRepository.java       [6 query methods]
├── dto/
│   ├── QualityMeasureResultDTO.java              [API response DTO]
│   └── QualityMeasureResultMapper.java           [Mapper]
└── client/
    ├── CqlEngineServiceClient.java
    ├── CareGapServiceClient.java
    └── PatientServiceClient.java
```

### Care Gap Service (Results & Reports)
```
backend/modules/services/care-gap-service/src/main/java/com/healthdata/caregap/
├── controller/
│   └── CareGapController.java                    [15+ REST endpoints]
├── service/
│   ├── CareGapIdentificationService.java         [Gap identification]
│   └── CareGapReportService.java                 [Report generation (in-memory)]
├── persistence/
│   ├── CareGapEntity.java                        [JPA entity, care_gaps table]
│   └── CareGapRepository.java                    [67+ query methods]
└── client/
    ├── CqlEngineServiceClient.java
    └── PatientServiceClient.java
```

---

## Key Endpoints at a Glance

### Evaluations
```
POST   /api/v1/cql/evaluations
POST   /api/v1/cql/evaluations/{id}/execute
GET    /api/v1/cql/evaluations
GET    /api/v1/cql/evaluations/{id}
GET    /api/v1/cql/evaluations/patient/{patientId}
GET    /api/v1/cql/evaluations/library/{libraryId}
GET    /api/v1/cql/evaluations/by-status/{status}
POST   /api/v1/cql/evaluations/batch
POST   /api/v1/cql/evaluations/{id}/retry
DELETE /api/v1/cql/evaluations/old
[15+ more endpoints in controller]
```

### Quality Measure Results
```
POST   /quality-measure/calculate
GET    /quality-measure/results
GET    /quality-measure/score
GET    /quality-measure/report/patient
GET    /quality-measure/report/population
GET    /quality-measure/_health
```

### Care Gaps
```
POST   /care-gap/identify
POST   /care-gap/identify/{library}
POST   /care-gap/refresh
POST   /care-gap/close
GET    /care-gap/open
GET    /care-gap/high-priority
GET    /care-gap/overdue
GET    /care-gap/upcoming
GET    /care-gap/summary
GET    /care-gap/stats
GET    /care-gap/by-category
GET    /care-gap/by-priority
GET    /care-gap/population-report
[2+ more endpoints in controller]
```

---

## Database Tables

### cql_evaluations
```
id | tenant_id | library_id | patient_id | context_data | evaluation_result | 
status | error_message | duration_ms | evaluation_date | created_at
```

### quality_measure_results
```
id | tenant_id | patient_id | measure_id | measure_name | measure_category | 
measure_year | numerator_compliant | denominator_elligible | compliance_rate | 
score | calculation_date | cql_library | cql_result | created_at | created_by | version
```

### care_gaps
```
id | tenant_id | patient_id | measure_id | measure_name | measure_category | 
gap_type | gap_status | gap_description | priority | risk_score | 
identified_date | due_date | closed_date | recommendation | cql_library | 
cql_result | created_at | created_by | updated_at | updated_by | version
```

---

## Service Method Examples

### Execute CQL Evaluation
```java
// From CqlEvaluationService
CqlEvaluation evaluation = evaluationService.createEvaluation(
    tenantId, libraryId, patientId
);
CqlEvaluation executed = evaluationService.executeEvaluation(
    evaluation.getId(), tenantId
);
```

### Calculate Quality Measure
```java
// From MeasureCalculationService
QualityMeasureResultEntity result = calculationService.calculateMeasure(
    tenantId, patientId, measureId, createdBy
);
MeasureCalculationService.QualityScore score = 
    calculationService.getQualityScore(tenantId, patientId);
```

### Get Quality Report
```java
// From QualityReportService
QualityReportService.QualityReport report = 
    reportService.getPatientQualityReport(tenantId, patientId);
    
QualityReportService.PopulationQualityReport popReport = 
    reportService.getPopulationQualityReport(tenantId, year);
```

### Identify Care Gaps
```java
// From CareGapIdentificationService
List<CareGapEntity> gaps = identificationService.identifyAllCareGaps(
    tenantId, patientId, createdBy
);
```

### Get Care Gap Report
```java
// From CareGapReportService
CareGapReportService.CareGapSummary summary = 
    reportService.getCareGapSummary(tenantId, patientId);
    
CareGapReportService.PopulationGapReport popReport = 
    reportService.getPopulationGapReport(tenantId);
```

---

## What's Fully Implemented
1. CQL Evaluations - Complete CRUD + batch operations
2. Quality Measure Results - Calculation, retrieval, scoring
3. Care Gap Results - Identification, status tracking, closure
4. Quality Measure Reports (in-memory) - Patient and population level
5. Care Gap Reports (in-memory) - Patient and population level
6. Multi-tenancy - All services tenant-aware
7. Security - Role-based access control on all endpoints
8. Caching - Spring Cache on key queries

---

## What's Missing/Partial
1. Persistent Report Storage - All reports in-memory only
2. Report Scheduling - No scheduled/recurring reports
3. Report Export - No PDF/CSV/Excel export
4. Advanced Analytics - No trending, cohort analysis, benchmarking
5. Evaluation DTOs - CQL evals returned as entities
6. Care Gap DTOs - No explicit DTO mapping layer
7. Data Retention - Only CQL evals have retention policy
8. Report Pagination - No pagination for large reports
9. Report History - No versioning/change tracking
10. Anomaly Detection - No data quality monitoring

---

## Integration Points

### Quality Measure Service integrates with:
- PatientServiceClient → Patient demographics
- CareGapServiceClient → Care gap integration
- CqlEngineServiceClient → CQL evaluation
- Kafka → measure-calculated topic

### Care Gap Service integrates with:
- PatientServiceClient → Patient demographics
- CqlEngineServiceClient → CQL evaluation
- Database → FHIR entity references (Encounter, Condition, Procedure)

### CQL Engine Service:
- Self-contained (no client integrations)
- Uses MeasureTemplateEngine (internal CQL execution)
- Stores evaluation results in cql_evaluations table

---

## Common Patterns

### Multi-tenancy
```java
@RequestHeader("X-Tenant-ID") String tenantId
// All repository queries filter by tenantId
```

### Role-based Access
```java
@PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
```

### Caching
```java
@Cacheable(value = "measureResults", key = "#tenantId + ':' + #patientId")
public List<QualityMeasureResultEntity> getPatientMeasureResults(...)
```

### Result Records (instead of entities for reports)
```java
public record QualityReport(
    String patientId,
    long totalMeasures,
    long compliantMeasures,
    double qualityScore,
    Map<String, Long> measuresByCategory,
    String careGapSummary
) {}
```

---

## Testing
- Location: `src/test/java/com/healthdata/{service}/integration/`
- Quality Measure Service has good integration test coverage
- CQL Engine Service tests not visible in codebase
- Missing: Report export tests, analytics tests, data retention tests

---

## Performance Considerations
1. CQL evaluations indexed on: library_id, patient_id, tenant_id
2. Results indexed on: tenant_id, patient_id, measure_id
3. Care gaps indexed on: various combinations for common queries
4. Caching highly used queries (evaluation results, reports)
5. Batch evaluation available for concurrent processing
6. Paginated queries for large datasets

---

## Deployment Checklist
- [ ] Cache configuration (recommend Redis for distributed)
- [ ] Kafka configuration (measure-calculated topic)
- [ ] Database migrations (Liquibase)
- [ ] Feign client URLs configured
- [ ] Multi-tenant context propagation
- [ ] Security configuration (auth provider)
- [ ] Data retention policies configured
