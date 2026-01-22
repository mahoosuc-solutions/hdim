# Predictive Analytics Service Audit Integration Plan

## Summary

Implementation plan for integrating AI audit event publishing into the `predictive-analytics-service` to track all ML-powered predictions including readmission risk, cost forecasting, disease progression, and population health analytics.

## Service Overview

The predictive analytics service provides ML-based predictions for:
- **Readmission Risk**: 30/90-day hospital readmission probability
- **Cost Forecasting**: Per-patient healthcare cost predictions  
- **Disease Progression**: Chronic disease trajectory modeling
- **Population Health**: Risk stratification and cohort analysis

## Audit Integration Requirements

### 1. New Audit Integration Class

**File**: `backend/modules/services/predictive-analytics-service/src/main/java/com/healthdata/predictive/audit/PredictiveAnalyticsAuditIntegration.java`

Methods to implement:
- `publishReadmissionRiskPredictionEvent()` - Audit readmission risk predictions
- `publishCostPredictionEvent()` - Audit cost forecasting
- `publishDiseaseProgressionEvent()` - Audit disease progression modeling
- `publishPopulationRiskStratificationEvent()` - Audit population health analytics

### 2. Service Integration Points

**Files to Update**:
- `ReadmissionRiskPredictor.java` - Add audit after risk score calculation
- `CostPredictor.java` - Add audit after cost prediction
- `DiseaseProgressionPredictor.java` - Add audit after progression modeling
- `PopulationRiskStratifier.java` - Add audit after stratification

### 3. New Decision Types

Add to `AIAgentDecisionEvent.DecisionType`:
- `READMISSION_RISK_PREDICTION` - Readmission probability prediction
- `COST_PREDICTION` - Healthcare cost forecasting
- `DISEASE_PROGRESSION_PREDICTION` - Disease trajectory modeling
- `POPULATION_RISK_STRATIFICATION` - Cohort risk analysis

### 4. Agent Type

Add to `AIAgentDecisionEvent.AgentType`:
- `PREDICTIVE_ANALYTICS` - ML prediction engine

## Audit Event Schema

### Readmission Risk Event
```json
{
  "eventId": "uuid",
  "timestamp": "2026-01-14T10:30:00Z",
  "tenantId": "tenant-123",
  "agentId": "readmission-predictor",
  "agentType": "PREDICTIVE_ANALYTICS",
  "decisionType": "READMISSION_RISK_PREDICTION",
  "resourceType": "Patient",
  "resourceId": "patient-789",
  "confidenceScore": 0.75,
  "reasoning": "ML model prediction based on LACE index and clinical features",
  "inputMetrics": {
    "predictionPeriodDays": 30,
    "laceIndex": 12,
    "charlsonScore": 4,
    "priorAdmissions": 2,
    "edVisits": 3,
    "comorbidities": ["diabetes", "hypertension", "heart_failure"],
    "riskScore": 75.5,
    "riskTier": "HIGH",
    "probability": 0.755
  }
}
```

### Cost Prediction Event
```json
{
  "eventId": "uuid",
  "timestamp": "2026-01-14T10:30:00Z",
  "tenantId": "tenant-123",
  "agentId": "cost-predictor",
  "agentType": "PREDICTIVE_ANALYTICS",
  "decisionType": "COST_PREDICTION",
  "resourceType": "Patient",
  "resourceId": "patient-789",
  "confidenceScore": 0.82,
  "reasoning": "ML cost prediction based on historical utilization and risk factors",
  "inputMetrics": {
    "predictionPeriodMonths": 12,
    "predictedTotalCost": 45000.00,
    "inpatientCost": 25000.00,
    "outpatientCost": 12000.00,
    "pharmacyCost": 5000.00,
    "emergencyCost": 3000.00,
    "confidenceInterval": {"lower": 40000.00, "upper": 50000.00}
  }
}
```

### Disease Progression Event
```json
{
  "eventId": "uuid",
  "timestamp": "2026-01-14T10:30:00Z",
  "tenantId": "tenant-123",
  "agentId": "disease-progression-predictor",
  "agentType": "PREDICTIVE_ANALYTICS",
  "decisionType": "DISEASE_PROGRESSION_PREDICTION",
  "resourceType": "Patient",
  "resourceId": "patient-789",
  "confidenceScore": 0.88,
  "reasoning": "Disease trajectory model for chronic kidney disease",
  "inputMetrics": {
    "condition": "chronic-kidney-disease",
    "currentStage": "3a",
    "predictedStageIn12Months": "3b",
    "progressionProbability": 0.65,
    "timeToNextStageMonths": 18,
    "riskFactors": ["diabetes", "hypertension", "proteinuria"]
  }
}
```

### Population Risk Stratification Event
```json
{
  "eventId": "uuid",
  "timestamp": "2026-01-14T10:30:00Z",
  "tenantId": "tenant-123",
  "agentId": "population-risk-stratifier",
  "agentType": "PREDICTIVE_ANALYTICS",
  "decisionType": "POPULATION_RISK_STRATIFICATION",
  "resourceType": "Population",
  "resourceId": "cohort-456",
  "confidenceScore": 0.90,
  "reasoning": "Population-level risk analysis for care management",
  "inputMetrics": {
    "patientCount": 1500,
    "highRiskCount": 120,
    "moderateRiskCount": 350,
    "lowRiskCount": 1030,
    "averageRiskScore": 45.2,
    "stratificationDate": "2026-01-14"
  }
}
```

## Implementation Steps

1. **Add new decision types to audit model** ✅ (Already have enums)
2. **Create PredictiveAnalyticsAuditIntegration class**
3. **Update ReadmissionRiskPredictor**
   - Inject audit integration
   - Add audit publishing after predictions
4. **Update CostPredictor**
   - Inject audit integration
   - Add audit publishing after predictions
5. **Update DiseaseProgressionPredictor**
   - Inject audit integration
   - Add audit publishing after predictions
6. **Update PopulationRiskStratifier**
   - Inject audit integration
   - Add audit publishing after stratification
7. **Create lightweight unit tests**
8. **Create heavyweight integration tests with Testcontainers**

## Test Coverage

### Lightweight Tests (Unit)
- Test audit event structure for all prediction types
- Test null value handling
- Test error scenarios
- Test non-blocking behavior
- ~8-10 test cases

### Heavyweight Tests (Integration)
- Test Kafka publishing for each prediction type
- Test partition key format
- Test concurrent predictions
- Test event replay
- ~6-8 test cases with Testcontainers

## Compliance Benefits

### Clinical Decision Support
- Complete audit trail of all ML predictions
- Model versioning and reproducibility
- Feature importance tracking
- Confidence score logging

### HIPAA Compliance
- PHI access logging for predictions
- Patient-level prediction tracking
- Immutable audit trail
- 6-year retention capability

### SOC 2 Compliance
- ML model decision logging
- Risk stratification traceability
- Cost prediction audit trail
- Population health analytics tracking

## Performance Considerations

- **Non-blocking**: Audit failures don't impact predictions
- **Async Publishing**: Kafka handles backpressure
- **Minimal Overhead**: ~1-2ms per prediction
- **Scalable**: Kafka partitioning for horizontal scaling

## Dependencies

The service already has the audit module dependency:
```kotlin
implementation(project(":modules:shared:infrastructure:audit"))
```

## Configuration

```yaml
audit:
  kafka:
    enabled: true
    topic:
      ai-decisions: ai.agent.decisions
```

## Status

🔄 **IN PROGRESS** - Implementation plan complete, code implementation pending

## Next Steps

1. Create `PredictiveAnalyticsAuditIntegration.java`
2. Update predictor services to publish audit events
3. Create comprehensive test suites
4. Verify compilation and run tests
5. Document completion

## Related Services

After predictive-analytics-service, continue with:
- hcc-service (HCC coding decisions)
- quality-measure-service (Quality scoring)
- patient-service (Patient aggregation)
- fhir-service (PHI access)

---

**Plan created**: January 14, 2026
**Status**: Ready for implementation
**Estimated effort**: 2-3 hours
