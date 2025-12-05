# Predictive Analytics Service

ML-powered predictive analytics service for healthcare risk prediction, cost forecasting, and disease progression modeling.

## Overview

The Predictive Analytics Service provides machine learning-based predictions for:
- **Readmission Risk**: 30/90-day hospital readmission probability
- **Cost Forecasting**: Per-patient healthcare cost predictions
- **Disease Progression**: Chronic disease trajectory modeling
- **Population Health**: Risk stratification and cohort analysis

## Features

### 1. Readmission Risk Prediction
- LACE index calculation (Length, Acuity, Comorbidities, ED visits)
- Charlson Comorbidity Index integration
- 30-day and 90-day prediction periods
- Risk tier classification (LOW, MODERATE, HIGH, VERY_HIGH)
- Feature importance analysis

### 2. Cost Prediction
- Multi-category cost breakdown:
  - Inpatient care
  - Outpatient care
  - Pharmacy/medications
  - Emergency department
  - Lab and diagnostics
  - Imaging
  - Other ancillary services
- Configurable prediction periods (1-24 months)
- Confidence intervals

### 3. Disease Progression
- Supported conditions:
  - Diabetes
  - Chronic Kidney Disease (CKD)
  - Heart Failure
  - And more...
- Time-to-event predictions
- Stage transition modeling
- Risk factor identification

### 4. Population Risk Stratification
- Automated cohort generation
- Risk tier distribution
- High-risk patient identification
- Batch processing support

## Technology Stack

- **Framework**: Spring Boot 3.3.5
- **Language**: Java 21
- **ML Library**: Smile ML 3.1.1
- **Statistics**: Apache Commons Math 3.6.1
- **Database**: PostgreSQL
- **Cache**: Redis
- **Testing**: JUnit 5, Mockito, Spring Test

## API Endpoints

### Readmission Risk
```
POST /api/v1/analytics/readmission-risk/{patientId}
Headers: X-Tenant-ID
Query: predictionPeriod (30 or 90 days)
Body: Patient clinical data
```

### Cost Prediction
```
POST /api/v1/analytics/cost-prediction/{patientId}
Headers: X-Tenant-ID
Query: predictionPeriodMonths (default: 12)
Body: Patient clinical data
```

### Disease Progression
```
POST /api/v1/analytics/disease-progression/{patientId}
Headers: X-Tenant-ID
Query: condition (e.g., "diabetes", "chronic-kidney-disease")
Body: Patient clinical data
```

### Population Risk Stratification
```
GET /api/v1/analytics/population/risk-stratification
Headers: X-Tenant-ID
Query: patientIds (comma-separated)
```

### High-Risk Patients
```
GET /api/v1/analytics/population/high-risk
Headers: X-Tenant-ID
Query: patientIds (comma-separated)
```

## Domain Models

### Core Models
- **PatientFeatures**: ML feature extraction from clinical data
- **ReadmissionRiskScore**: Risk score with LACE index and risk tier
- **CostBreakdown**: Multi-category cost predictions
- **ProgressionRisk**: Disease trajectory and time-to-event
- **RiskCohort**: Population segments by risk tier
- **RiskTier**: LOW, MODERATE, HIGH, VERY_HIGH

### Supporting Models
- **ReadmissionRiskFactors**: Contributing factors with importance scores
- **TimeToEvent**: Predicted event timing with confidence intervals

## Machine Learning Features

### Feature Extraction
The service extracts 17 clinical features:
1. Age
2. Gender
3. Charlson Comorbidity Index
4. Active condition count
5. Hospitalizations (past year)
6. ED visits (past 6 months)
7. Outpatient visits (past 6 months)
8. Active medications
9. Medication changes (30 days)
10. HbA1c
11. Serum creatinine
12. eGFR
13. Blood pressure (systolic)
14. Blood pressure (diastolic)
15. BMI
16. Social risk score
17. Length of stay

### Feature Normalization
- Min-max normalization
- Clinical value ranges
- Handles missing values

### Model Explainability
- Feature importance scores
- Contributing factor breakdown
- Risk factor identification

## Test Coverage

The service includes 95+ comprehensive tests following TDD methodology:

- **FeatureExtractorTest**: 15+ tests
- **ReadmissionRiskPredictorTest**: 25+ tests
- **CostPredictorTest**: 15+ tests
- **DiseaseProgressionPredictorTest**: 15+ tests
- **PopulationRiskStratifierTest**: 10+ tests
- **PredictiveAnalyticsControllerTest**: 15+ tests

### Test Categories
- Basic functionality tests
- Edge case handling
- Validation tests
- Integration tests
- Error handling tests

## Clinical Algorithms

### LACE Index Calculation
- **L**ength of stay: 0-7 points
- **A**cuity of admission: 0-3 points
- **C**omorbidities (Charlson): 0-5 points
- **E**D visits (6 months): 0-4 points
- Total: 0-19 points

### Charlson Comorbidity Index
Weighted scoring for 17 comorbid conditions with age adjustment.

### Risk Tier Classification
- **LOW**: 0-25 (routine monitoring)
- **MODERATE**: 25-50 (enhanced monitoring)
- **HIGH**: 50-75 (proactive intervention)
- **VERY_HIGH**: 75-100 (intensive case management)

## Multi-Tenancy Support

All operations require tenant ID for data isolation:
- Header: `X-Tenant-ID`
- Ensures data separation
- Tenant-specific caching

## Security

- Spring Security integration
- Role-based access control (RBAC)
- Required roles: ANALYST, EVALUATOR, ADMIN, SUPER_ADMIN
- CSRF protection
- Secure API endpoints

## Configuration

### Application Properties
```yaml
spring:
  application:
    name: predictive-analytics-service

server:
  port: 8089

logging:
  level:
    com.healthdata.predictive: DEBUG
```

### Environment Variables
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password

## Building and Running

### Build
```bash
./gradlew :modules:services:predictive-analytics-service:build
```

### Run Tests
```bash
./gradlew :modules:services:predictive-analytics-service:test
```

### Run Service
```bash
./gradlew :modules:services:predictive-analytics-service:bootRun
```

## Dependencies

- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Security
- Spring Boot Starter Validation
- Spring Boot Starter Cache (Redis)
- Spring Cloud OpenFeign
- HAPI FHIR Client
- Smile ML 3.1.1
- Apache Commons Math 3.6.1
- Lombok
- JUnit 5
- Mockito

## Future Enhancements

### Planned Features
1. **Advanced ML Models**
   - Neural networks for complex patterns
   - Ensemble methods (Random Forest, Gradient Boosting)
   - Deep learning for temporal data

2. **Model Training Pipeline**
   - Automated model retraining
   - A/B testing framework
   - Model performance monitoring

3. **Additional Predictions**
   - Sepsis risk prediction
   - Fall risk assessment
   - Medication adherence prediction
   - No-show probability

4. **Enhanced Explainability**
   - SHAP values
   - LIME explanations
   - Counterfactual analysis

5. **Real-time Predictions**
   - Streaming analytics
   - Event-driven predictions
   - Alert generation

## API Examples

### Readmission Risk Prediction
```bash
curl -X POST http://localhost:8089/api/v1/analytics/readmission-risk/patient-123 \
  -H "X-Tenant-ID: tenant-1" \
  -H "Content-Type: application/json" \
  -d '{
    "age": 65,
    "charlsonComorbidityIndex": 4,
    "hospitalizationsPastYear": 2,
    "edVisitsPast6Months": 3,
    "lengthOfStay": 7,
    "lastAdmissionAcuity": "emergency"
  }'
```

### Cost Prediction
```bash
curl -X POST http://localhost:8089/api/v1/analytics/cost-prediction/patient-123?predictionPeriodMonths=12 \
  -H "X-Tenant-ID: tenant-1" \
  -H "Content-Type: application/json" \
  -d '{
    "age": 65,
    "charlsonComorbidityIndex": 4,
    "activeMedicationCount": 8,
    "hospitalizationsPastYear": 1
  }'
```

### Disease Progression
```bash
curl -X POST http://localhost:8089/api/v1/analytics/disease-progression/patient-123?condition=diabetes \
  -H "X-Tenant-ID: tenant-1" \
  -H "Content-Type: application/json" \
  -d '{
    "age": 55,
    "hemoglobinA1c": 8.5,
    "charlsonComorbidityIndex": 3
  }'
```

## License

Copyright 2025 HealthData in Motion. All rights reserved.
