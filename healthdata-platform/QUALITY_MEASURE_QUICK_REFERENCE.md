# Quality Measure Calculation Engine - Quick Reference

## Service Usage Examples

### 1. Calculate Single Measure for a Patient

```java
@Autowired
private QualityMeasureCalculationService calculationService;

// Calculate diabetes HbA1c control
MeasureResult result = calculationService.calculateMeasure(
    "patient-123",
    QualityMeasureCalculationService.HEDIS_DIABETES_HBA1C
);

// Check results
boolean isCompliant = result.isCompliant();
double score = result.getScore();
int numerator = result.getNumerator();
int denominator = result.getDenominator();
double percentage = result.getPercentage();
```

### 2. Calculate All Measures for a Patient

```java
// Get all 5 HEDIS measures for a patient
List<MeasureResult> allResults = calculationService.calculateAllMeasuresForPatient("patient-123");

for (MeasureResult result : allResults) {
    System.out.println("Measure: " + result.getMeasureId());
    System.out.println("Score: " + result.getScore());
    System.out.println("Compliant: " + result.isCompliant());
}
```

### 3. Batch Calculate Measure for Multiple Patients

```java
List<String> patientIds = Arrays.asList("patient-1", "patient-2", "patient-3");

List<MeasureResult> batchResults = calculationService.batchCalculate(
    patientIds,
    QualityMeasureCalculationService.HEDIS_DIABETES_HBA1C
);

// Process results
batchResults.forEach(result -> {
    if (result.isCompliant()) {
        // Handle compliant patient
    } else {
        // Identify gap and create care plan
    }
});
```

### 4. Calculate Measures for Population

```java
Pageable pageable = PageRequest.of(0, 100); // First page, 100 patients

List<MeasureResult> populationResults = calculationService.calculateMeasuresForPopulation(
    "tenant-1",
    QualityMeasureCalculationService.HEDIS_HYPERTENSION_BP,
    pageable
);

// Get statistics
Map<String, Object> stats = calculationService.getPopulationMeasureStatistics(
    "tenant-1",
    QualityMeasureCalculationService.HEDIS_HYPERTENSION_BP
);

double complianceRate = (double) stats.get("compliance_rate");
System.out.println("Population Compliance Rate: " + complianceRate + "%");
```

### 5. Access Cached Results

```java
// Get previously calculated results from cache
List<MeasureResult> cachedResults = calculationService.getCachedMeasureResults(
    "patient-123",
    QualityMeasureCalculationService.HEDIS_DIABETES_HBA1C
);

// Invalidate cache when new data arrives
calculationService.invalidateMeasureCache(
    "patient-123",
    QualityMeasureCalculationService.HEDIS_DIABETES_HBA1C
);
```

## Measure Constants

```java
// All HEDIS measure IDs
QualityMeasureCalculationService.HEDIS_DIABETES_HBA1C           // "HEDIS-DC"
QualityMeasureCalculationService.HEDIS_HYPERTENSION_BP          // "HEDIS-BPC"
QualityMeasureCalculationService.HEDIS_MEDICATION_ADHERENCE     // "HEDIS-MA"
QualityMeasureCalculationService.HEDIS_BREAST_CANCER_SCREENING  // "HEDIS-BCS"
QualityMeasureCalculationService.HEDIS_COLORECTAL_CANCER_SCREENING // "HEDIS-CCS"
```

## Result Object Properties

```java
MeasureResult result = ...;

// Core properties
String patientId = result.getPatientId();
String measureId = result.getMeasureId();
boolean compliant = result.isCompliant();
String status = result.getComplianceStatus(); // "COMPLIANT" or "NON_COMPLIANT"

// Numerator/Denominator
int numerator = result.getNumerator();
int denominator = result.getDenominator();
double percentage = result.getPercentage(); // (numerator / denominator) * 100

// Score
Double score = result.getScore();

// Measurement periods
LocalDate periodStart = result.getPeriodStart();
LocalDate periodEnd = result.getPeriodEnd();

// Metadata
LocalDateTime calculationDate = result.getCalculationDate();
String tenantId = result.getTenantId();
Map<String, String> details = result.getDetails();
```

## HEDIS Measure Specifications

### 1. Diabetes HbA1c Control (HEDIS-DC)

**Clinical Goal:** Patients with diabetes have adequate glycemic control

```
Denominator: Patients 18-75 with diabetes diagnosis (ICD-10: E10, E11)
Numerator:   Patients with HbA1c <= 7.0% (LOINC: 4548-4)
Threshold:   7.0% or lower
Compliance:  Numerator = 1, Denominator = 1
```

### 2. Blood Pressure Control (HEDIS-BPC)

**Clinical Goal:** Patients with hypertension achieve control

```
Denominator: Patients 18-85 with hypertension (ICD-10: I10)
Numerator:   Systolic <140 AND Diastolic <90 mmHg
Thresholds:  Systolic < 140 (LOINC: 8480-6)
             Diastolic < 90 (LOINC: 8462-4)
Compliance:  Both thresholds met
```

### 3. Medication Adherence (HEDIS-MA)

**Clinical Goal:** Patients adhere to chronic disease medications

```
Denominator: Patients with chronic disease diagnoses
Numerator:   Patients with >= 80% medication adherence rate
Measure:     Based on observation frequency over past year
Compliance:  Adherence >= 80%
```

### 4. Breast Cancer Screening (HEDIS-BCS)

**Clinical Goal:** Eligible women receive mammography screening

```
Denominator: Women 40-74 without breast cancer history (no ICD-10: C50)
Numerator:   Women with mammography within 24 months (LOINC: 44892-0)
Interval:    2 years (24 months)
Compliance:  Screening within interval
```

### 5. Colorectal Cancer Screening (HEDIS-CCS)

**Clinical Goal:** Eligible adults receive appropriate colorectal screening

```
Denominator: Patients 50-75 without colorectal cancer history (no C18, C19)
Numerator:   Any of the following within specified interval:
  - Colonoscopy within 10 years (LOINC: 73761-1)
  - FOBT within 1 year (LOINC: 2335-8)
  - FIT within 1 year (LOINC: 38253-1)
Compliance:  At least one screening completed
```

## Population Statistics Response

```json
{
  "total_patients": 500,
  "compliant_patients": 425,
  "compliance_rate": 85.0,
  "average_score": 82.5,
  "min_score": 45.0,
  "max_score": 100.0,
  "measure_id": "HEDIS-DC",
  "tenant_id": "tenant-1",
  "calculation_date": "2025-12-01T10:30:00"
}
```

## Integration Checklist

- [ ] Add QualityMeasureCalculationService to Spring context
- [ ] Ensure PatientRepository, ObservationRepository, ConditionRepository are available
- [ ] Configure Spring Cache (e.g., @EnableCaching in config)
- [ ] Set up database schema for measure_results table
- [ ] Configure tenant context for multi-tenancy
- [ ] Add appropriate permissions/roles for measure calculation
- [ ] Set up monitoring/logging for batch operations
- [ ] Configure cache eviction policies for production

## Performance Considerations

### Caching Strategy
- Individual measure results cached by patientId + measureId
- Cache key format: `patientId:measureId`
- Automatic invalidation on new calculations
- Configurable TTL for cache entries

### Batch Processing
- Parallel stream processing for multiple patients
- Graceful error handling (skips failed patients)
- Null filtering to exclude errors from results
- Recommended batch size: 50-100 patients

### Database Queries
- Uses JPA lazy loading for relationships
- Efficient repository queries with proper indexing
- Pagination support for large result sets
- Aggregation queries for population statistics

## Troubleshooting

### Patient Not in Denominator
- Check age requirements for measure
- Verify diagnostic condition codes (ICD-10) exist
- Ensure conditions have active clinical status

### Missing Observations
- Verify observation codes (LOINC) match system
- Check observation effective dates
- Ensure observations have final status

### Incorrect Compliance Status
- Validate threshold values match HEDIS specifications
- Check calculation date ranges
- Verify data types (BigDecimal vs Double)

### Cache Issues
- Clear cache if measure definition changes
- Monitor cache hit rates
- Adjust TTL for volatile data

## Database Schema

```sql
-- MeasureResult table in quality schema
CREATE TABLE quality.measure_results (
    id UUID PRIMARY KEY,
    patient_id VARCHAR NOT NULL,
    measure_id VARCHAR NOT NULL,
    score DOUBLE PRECISION,
    numerator INTEGER,
    denominator INTEGER,
    compliant BOOLEAN NOT NULL,
    calculation_date TIMESTAMP NOT NULL,
    period_start DATE,
    period_end DATE,
    tenant_id VARCHAR NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (patient_id) REFERENCES patient.patients(id)
);

-- Details element collection table
CREATE TABLE quality.measure_result_details (
    result_id UUID NOT NULL,
    detail_key VARCHAR NOT NULL,
    detail_value VARCHAR,
    PRIMARY KEY (result_id, detail_key),
    FOREIGN KEY (result_id) REFERENCES quality.measure_results(id)
);

-- Recommended indexes
CREATE INDEX idx_patient_measure ON quality.measure_results(patient_id, measure_id);
CREATE INDEX idx_tenant_measure ON quality.measure_results(tenant_id, measure_id);
CREATE INDEX idx_calculation_date ON quality.measure_results(calculation_date DESC);
CREATE INDEX idx_compliance ON quality.measure_results(tenant_id, compliant);
```

## Related Documentation

- HEDIS Measures: See QUALITY_MEASURE_IMPLEMENTATION_SUMMARY.md
- Test Coverage: See QualityMeasureCalculationServiceTest.java
- Domain Models: See MeasureResult.java
- Repository Patterns: See QualityMeasureResultRepository.java
