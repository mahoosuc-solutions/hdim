# HCC Service

Hierarchical Condition Category (HCC) risk adjustment service for Medicare Advantage RAF score calculation and documentation gap management.

## Purpose

The HCC Service calculates Risk Adjustment Factor (RAF) scores using CMS HCC models V24 and V28 for Medicare Advantage plans. It provides ICD-10 to HCC crosswalk mapping, blended RAF calculations during transition years, documentation gap identification, and high-value opportunity reporting to maximize accurate risk capture and reimbursement.

## Key Features

- **Dual Model Support**: CMS HCC V24 and V28 model implementations
- **Blended RAF Calculation**: 2024 blending (67% V28 / 33% V24) per CMS guidelines
- **ICD-10 Crosswalk**: Complete ICD-10-CM to HCC mapping for both models
- **RAF Score Calculation**: Patient-level and population-level RAF scoring
- **Demographic Factors**: Age, sex, dual eligibility, institutional status adjustments
- **HCC Hierarchies**: Automatic hierarchy suppression rules
- **Disease Interactions**: Capture HCC interaction terms (diabetes + CHF, etc.)
- **Documentation Gaps**: Identify missing HCC documentation from prior year
- **High-Value Opportunities**: Rank patients by potential RAF uplift
- **Patient HCC Profiles**: Historical tracking of captured HCCs by year
- **Multi-tenant Support**: Complete tenant isolation and security

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/hcc/api/v1/hcc/patient/{patientId}/calculate` | Calculate RAF score for patient |
| GET | `/hcc/api/v1/hcc/patient/{patientId}/profile` | Get patient HCC profile and history |
| GET | `/hcc/api/v1/hcc/crosswalk` | Get ICD-10 to HCC mappings (batch) |
| GET | `/hcc/api/v1/hcc/patient/{patientId}/documentation-gaps` | Get documentation gaps |
| GET | `/hcc/api/v1/hcc/opportunities` | Get high-value RAF uplift opportunities |

## Configuration

### Application Properties

```yaml
server:
  port: 8105
  servlet:
    context-path: /hcc

hcc:
  model:
    year: 2024
    v24-weight: 0.33
    v28-weight: 0.67
  crosswalk:
    data-path: classpath:data/hcc-crosswalk/
```

### Service Integration

```yaml
fhir:
  server:
    url: http://localhost:8085/fhir

patient:
  service:
    url: http://localhost:8084/patient

care-gap:
  service:
    url: http://localhost:8086/care-gap
```

### Database

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5435/healthdata_cql
    username: healthdata
    password: ${DB_PASSWORD}
```

### Cache

```yaml
spring.cache:
  type: redis
  redis:
    time-to-live: 3600000  # 1 hour
```

## HCC Models

### CMS-HCC V24 (Legacy)
- 79 HCC categories
- Community and institutional models
- Used through 2023
- 33% weight in 2024 blending

### CMS-HCC V28 (Current)
- 115 HCC categories (expanded from V24)
- Improved clinical granularity
- Enhanced disease capture
- 67% weight in 2024 blending
- 100% weight starting 2025

## RAF Score Components

### Demographic Factors
- Age (categorical: 0-34, 35-44, 45-54, 55-59, 60-64, 65-69, 70-74, 75-79, 80-84, 85-89, 90-94, 95+)
- Sex (Male/Female)
- Dual eligibility (Medicaid eligible)
- Institutional status (nursing home resident)
- Originally disabled status

### Disease Factors
- Captured HCCs from diagnosis codes
- HCC hierarchies (higher severity suppresses lower)
- Disease interactions (e.g., Diabetes + CHF)

### Blended Score (2024)
```
Blended RAF = (V24 RAF × 0.33) + (V28 RAF × 0.67)
```

## Documentation Gaps

Documentation gaps are identified when:
- HCC was captured in prior year but not current year
- Chronic condition expected to persist
- Significant RAF impact if recaptured
- Annual documentation requirement not met

### Gap Types
- **Suspected Recapture**: Prior year HCC not documented in current year
- **High Impact**: HCC with coefficient > 0.5
- **Care Opportunity**: Clinical intervention recommended
- **Chart Review**: Medical record review needed

## Running Locally

### Start the Service

```bash
./gradlew :modules:services:hcc-service:bootRun
```

### Running Tests

```bash
./gradlew :modules:services:hcc-service:test
```

### Building

```bash
./gradlew :modules:services:hcc-service:build
```

## Example Usage

### Calculate RAF Score

```bash
curl -X POST http://localhost:8105/hcc/api/v1/hcc/patient/patient-123/calculate \
  -H "X-Tenant-ID: tenant-001" \
  -H "Content-Type: application/json" \
  -d '{
    "diagnosisCodes": ["E11.9", "I50.9", "J44.9"],
    "age": 72,
    "sex": "M",
    "dualEligible": true,
    "institutionalized": false
  }'
```

Response:
```json
{
  "patientId": "patient-123",
  "v24Raf": 1.523,
  "v28Raf": 1.678,
  "blendedRaf": 1.634,
  "capturedHccs": [
    {"model": "V28", "hccCode": "HCC19", "description": "Diabetes without Complication", "coefficient": 0.318},
    {"model": "V28", "hccCode": "HCC85", "description": "Congestive Heart Failure", "coefficient": 0.323},
    {"model": "V28", "hccCode": "HCC111", "description": "Chronic Obstructive Pulmonary Disease", "coefficient": 0.328}
  ],
  "interactions": [
    {"code": "D1_CHF", "coefficient": 0.154}
  ]
}
```

### Get Documentation Gaps

```bash
curl http://localhost:8105/hcc/api/v1/hcc/patient/patient-123/documentation-gaps?year=2024 \
  -H "X-Tenant-ID: tenant-001"
```

### Get High-Value Opportunities

```bash
curl "http://localhost:8105/hcc/api/v1/hcc/opportunities?year=2024&minUplift=0.1" \
  -H "X-Tenant-ID: tenant-001"
```

## Common HCC Categories

### High-Impact HCCs (Coefficient > 0.5)
- HCC8: Metastatic Cancer (2.659)
- HCC9: Lung and Other Severe Cancers (1.395)
- HCC17: Diabetes with Chronic Complications (0.318 + interactions)
- HCC18: Diabetes with Acute Complications (0.318 + interactions)
- HCC85: Congestive Heart Failure (0.323)
- HCC86: Acute Myocardial Infarction (0.184)

### Common Chronic Conditions
- HCC19: Diabetes without Complication (0.318)
- HCC108: Vascular Disease (0.288)
- HCC111: COPD (0.328)
- HCC112: Fibrosis of Lung (0.247)

## Hierarchies

HCC hierarchies ensure only the most severe condition is counted:
- HCC17 (Diabetes with chronic complications) suppresses HCC18, HCC19
- HCC80 (Acute Stroke) suppresses HCC81, HCC82
- HCC85 (CHF) suppresses HCC86 (AMI)

## Integration

The HCC Service integrates with:
- **FHIR Service**: Retrieves patient conditions/diagnoses
- **Patient Service**: Gets patient demographics
- **Care Gap Service**: Creates documentation gap tasks
- **Event Router**: Publishes HCC calculation events

## Standards Compliance

- CMS HCC Risk Adjustment Model V24/V28
- ICD-10-CM Official Guidelines
- Medicare Managed Care Manual Chapter 7
- CMS Rate Announcement methodology

## Authors

HDIM Development Team

## License

Proprietary - Mahoosuc Solutions
