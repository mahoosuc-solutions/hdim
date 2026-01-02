# Phase 4: Continuous Risk Assessment - Quick Reference

## Files Created (9 files)

### Tests (TDD - Written First)
1. `RiskCalculationServiceTest.java` - 7 test scenarios, 354 lines
2. `ChronicDiseaseMonitoringServiceTest.java` - 7 test scenarios, 438 lines

### Implementation
3. `RiskCalculationService.java` - Core risk calculation, 498 lines ✅
4. `DiseaseDeteriorationDetector.java` - Clinical thresholds, 329 lines ✅
5. `ChronicDiseaseMonitoringService.java` - Monitoring orchestration, 261 lines ✅
6. `RiskAssessmentEventConsumer.java` - Kafka listener, 243 lines ✅

### Data
7. `ChronicDiseaseMonitoringEntity.java` - JPA entity, 118 lines ✅
8. `ChronicDiseaseMonitoringRepository.java` - Spring Data JPA, 77 lines ✅
9. `0010-create-chronic-disease-monitoring-table.xml` - Liquibase migration ✅

**Status:** All files compile successfully ✅

---

## Clinical Thresholds Cheat Sheet

### HbA1c (Diabetes)
- **Target:** <7.0%
- **Deteriorating:** >9.0%
- **Alert:** >9.0% OR increase >1%
- **Monitoring:** 90 days (stable), 60 days (deteriorating)

### Blood Pressure
- **Target:** <130 mmHg systolic
- **Deteriorating:** >140 mmHg
- **Alert:** >160 mmHg OR increase >20 mmHg
- **Monitoring:** 30 days (stable), 14 days (deteriorating)

### LDL Cholesterol
- **Target:** <100 mg/dL
- **Deteriorating:** >190 mg/dL
- **Alert:** >220 mg/dL OR increase >40 mg/dL
- **Monitoring:** 180 days (stable), 90 days (deteriorating)

---

## Risk Levels

| Score   | Level      | Hospital Admit (90d) | ED Visit (90d) |
|---------|-----------|---------------------|----------------|
| 0-24    | LOW       | 2%                  | 5%             |
| 25-49   | MODERATE  | 10%                 | 20%            |
| 50-74   | HIGH      | 25%                 | 40%            |
| 75-100  | VERY_HIGH | 45%                 | 65%            |

---

## Kafka Topics

### Consumed (Input)
- `fhir.conditions.created` → New chronic disease diagnoses
- `fhir.conditions.updated` → Condition status changes
- `fhir.observations.created` → Lab results (HbA1c, BP, LDL)

### Produced (Output)
- `risk-assessment.updated` → Every risk recalculation
- `risk-level.changed` → When risk level changes (e.g., LOW → HIGH)
- `chronic-disease.deterioration` → When disease deteriorates

---

## LOINC Code Mappings

| LOINC Code | Metric        | Disease (SNOMED)          |
|-----------|--------------|---------------------------|
| 4548-4    | HbA1c        | 44054006 (Diabetes)       |
| 8480-6    | BP Systolic  | 38341003 (Hypertension)   |
| 18262-6   | LDL Chol     | 13644009 (Hyperlipidemia) |

---

## Key API Methods

### RiskCalculationService
```java
RiskAssessmentDTO recalculateRiskOnCondition(String tenantId, String patientId, Map<String, Object> conditionData)
RiskAssessmentDTO recalculateRiskOnObservation(String tenantId, String patientId, Map<String, Object> observationData)
```

### ChronicDiseaseMonitoringService
```java
ChronicDiseaseMonitoringEntity processLabResult(String tenantId, String patientId, Map<String, Object> observationData)
List<ChronicDiseaseMonitoringEntity> getDeterioratingPatients(String tenantId)
List<ChronicDiseaseMonitoringEntity> getPatientsWithAlerts(String tenantId)
List<ChronicDiseaseMonitoringEntity> getPatientsDueForMonitoring(String tenantId)
```

### DiseaseDeteriorationDetector
```java
Trend analyzeTrend(String metric, Double previousValue, Double currentValue)
boolean shouldTriggerAlert(String metric, Double currentValue, Double previousValue)
String getDeteriorationSeverity(String metric, Double value)
```

---

## Database Tables

### `risk_assessments` (Existing)
- Primary key: `id` (UUID)
- Key fields: `risk_score`, `risk_level`, `chronic_condition_count`
- JSONB fields: `risk_factors`, `predicted_outcomes`, `recommendations`
- Indexes: patient_date, risk_level, tenant isolation

### `chronic_disease_monitoring` (New)
- Primary key: `id` (UUID)
- Key fields: `disease_code`, `latest_value`, `previous_value`, `trend`, `alert_triggered`
- Unique constraint: (tenant_id, patient_id, disease_code)
- Indexes: tenant, patient, alerts, trend, next_monitoring

---

## Deployment Checklist

- [ ] Run database migration (automatic via Liquibase)
- [ ] Create Kafka topics (or verify auto-creation)
- [ ] Configure `application.yml` (Kafka consumer group, thresholds)
- [ ] Deploy quality-measure-service
- [ ] Verify Kafka connectivity
- [ ] Monitor Grafana dashboards
- [ ] Train clinical staff on alerts

---

## Testing Summary

**Total Tests:** 14 scenarios
**Coverage:**
- ✅ Risk recalculation on new conditions
- ✅ Risk recalculation on lab results
- ✅ Risk level change detection
- ✅ FHIR data extraction
- ✅ Predicted outcomes
- ✅ Multi-tenant isolation
- ✅ Event publishing
- ✅ HbA1c trend detection
- ✅ BP trend detection
- ✅ LDL trend detection
- ✅ Deterioration alerts
- ✅ Improvement detection
- ✅ Threshold validation

**Compilation:** ✅ All production code compiles successfully

---

## Example Event Payloads

### risk-level.changed Event
```json
{
  "eventType": "risk-level.changed",
  "tenantId": "tenant-123",
  "patientId": "patient-456",
  "previousLevel": "LOW",
  "newLevel": "HIGH",
  "timestamp": "2025-11-25T20:30:00Z"
}
```

### chronic-disease.deterioration Event
```json
{
  "eventType": "chronic-disease.deterioration",
  "tenantId": "tenant-123",
  "patientId": "patient-456",
  "diseaseCode": "44054006",
  "diseaseName": "Type 2 Diabetes Mellitus",
  "metric": "HbA1c",
  "previousValue": 7.5,
  "newValue": 9.2,
  "trend": "DETERIORATING",
  "alertLevel": "HIGH",
  "monitoringId": "uuid-here",
  "timestamp": "2025-11-25T20:30:00Z"
}
```

---

## Troubleshooting

### No events being consumed
- Check Kafka broker connectivity
- Verify consumer group ID in application.yml
- Check topic names match exactly
- Review Kafka consumer logs

### Risk not updating
- Verify FHIR event has correct category ("encounter-diagnosis")
- Check condition clinicalStatus is "active"
- Review RiskAssessmentEventConsumer logs

### Alerts not triggering
- Verify LOINC codes match exactly (4548-4, 8480-6, 18262-6)
- Check threshold values in DiseaseDeteriorationDetector
- Ensure valueQuantity.value is numeric

---

**Quick Reference Version:** 1.0
**Last Updated:** November 25, 2025
