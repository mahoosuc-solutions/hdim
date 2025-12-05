# SDOH Integration Service

Social Determinants of Health (SDOH) integration service for the HDIM platform, implementing Gravity Project FHIR Implementation Guide standards for comprehensive SDOH screening, Z-code mapping, community resource integration, and health equity analytics.

## Features

### 1. Gravity Standard Implementation
- **AHC-HRSN Screening**: Accountable Health Communities Health-Related Social Needs screening tool
- **PRAPARE Screening**: Protocol for Responding to and Assessing Patients' Assets, Risks, and Experiences
- **Standardized SDOH Categories**: Food Insecurity, Housing Instability, Transportation, Financial Strain, Education, Employment, Utilities, Social Isolation, Interpersonal Violence, Health Literacy, Substance Use, Mental Health, Disability, Immigration
- **LOINC Code Mapping**: All screening questions mapped to standard LOINC codes

### 2. Z-Code Mapping (ICD-10-CM)
- **Automated Z-Code Assignment**: Maps SDOH findings to ICD-10-CM Z-codes (Z55-Z65)
- **Category-specific Codes**:
  - Food Insecurity: Z59.4, Z59.41, Z59.48
  - Housing Instability: Z59.0, Z59.00, Z59.01, Z59.02
  - Transportation: Z59.82
  - Financial Strain: Z59.5, Z59.6, Z59.7
  - Education: Z55.x series
  - Employment: Z56.x series
  - And more...
- **FHIR Condition Export**: Export Z-coded diagnoses as FHIR Condition resources

### 3. Community Resource Directory
- **Resource Search**: Search by category, location, zip code, or proximity
- **Resource Categories**: Food, Housing, Transportation, Utilities, Employment, Education, Financial Services, Healthcare, Mental Health, Legal Services, Childcare, Eldercare
- **Geolocation Support**: Find resources within specified radius
- **Referral Management**: Create, track, and manage patient referrals to community resources
- **Referral Status Tracking**: PENDING, CONTACTED, SCHEDULED, COMPLETED, DECLINED, CANCELLED

### 4. Health Equity Analytics
- **Disparity Measurement**: Calculate health disparities across multiple stratifications
- **Stratification Types**: Race, Ethnicity, Language, Geography, Insurance Status, Income Level, Age Group, Gender
- **Trend Analysis**: Track disparity trends over time
- **Automated Reports**: Generate comprehensive health equity reports
- **Key Findings**: AI-generated insights and recommendations

### 5. SDOH Risk Scoring
- **Composite Risk Score**: 0-100 scale risk assessment
- **Risk Levels**: LOW (0-25), MODERATE (26-50), HIGH (51-75), CRITICAL (76-100)
- **Category-specific Weights**: Different SDOH categories weighted by impact
- **Predictive Analytics**:
  - Hospitalization risk prediction
  - Emergency visit risk prediction
  - Medication adherence impact prediction
- **Risk History Tracking**: Monitor changes over time

### 6. Multi-tenant Support
- Full tenant isolation for all SDOH data
- Tenant-specific screening statistics
- Tenant-level equity reporting

## API Endpoints

### SDOH Screening
- `POST /api/v1/sdoh/screening/{patientId}` - Submit SDOH screening
- `GET /api/v1/sdoh/assessment/{patientId}` - Get patient SDOH assessment
- `GET /api/v1/sdoh/screening/questions` - Get screening questionnaire

### Z-Code Management
- `GET /api/v1/sdoh/z-codes/{patientId}` - Get SDOH Z-codes for patient

### Community Resources
- `GET /api/v1/sdoh/resources` - Search community resources
- `POST /api/v1/sdoh/referral` - Create resource referral
- `GET /api/v1/sdoh/referrals/{patientId}` - Get patient referrals
- `PUT /api/v1/sdoh/referral/{referralId}/status` - Update referral status

### Health Equity
- `GET /api/v1/sdoh/equity/report` - Generate health equity report

### Risk Scoring
- `GET /api/v1/sdoh/risk/{patientId}` - Get patient risk score

### Health Check
- `GET /api/v1/sdoh/_health` - Service health check

## Technology Stack

- **Spring Boot 3.3.5**: Core framework
- **Spring Data JPA**: Data persistence
- **PostgreSQL**: Primary database
- **Redis**: Caching layer
- **HAPI FHIR 7.6.0**: FHIR resource handling
- **Jackson**: JSON processing
- **Lombok**: Boilerplate reduction
- **JUnit 5**: Testing framework
- **Mockito**: Mocking framework

## TDD Test Coverage

The service was developed using Test-Driven Development (TDD) methodology with 90+ comprehensive tests:

### Test Suites
1. **GravityScreeningServiceTest**: 22 tests
   - AHC-HRSN questionnaire creation
   - PRAPARE questionnaire creation
   - Screening submission and validation
   - Assessment management
   - FHIR mapping

2. **ZCodeMapperTest**: 15 tests
   - Category to Z-code mapping
   - Z-code validation
   - Diagnosis creation and management
   - FHIR Condition export

3. **CommunityResourceServiceTest**: 15 tests
   - Resource search by category, location, zip code
   - Proximity search with geolocation
   - Referral creation and management
   - Distance calculations

4. **HealthEquityAnalyzerTest**: 15 tests
   - Equity report generation
   - Disparity metrics calculation
   - Stratification analysis
   - Trend tracking

5. **SdohRiskCalculatorTest**: 11 tests
   - Risk score calculation
   - Risk level assignment
   - Predictive analytics
   - Risk history tracking

6. **SdohControllerTest**: 15 tests
   - All REST API endpoints
   - Security and authorization
   - Request/response validation

### Running Tests

```bash
# Run all tests
./gradlew :modules:services:sdoh-service:test

# Run specific test class
./gradlew :modules:services:sdoh-service:test --tests GravityScreeningServiceTest

# Run with coverage
./gradlew :modules:services:sdoh-service:test jacocoTestReport
```

## Database Schema

### Tables
- `sdoh_assessments` - SDOH screening assessments
- `sdoh_diagnoses` - Z-coded SDOH diagnoses
- `community_resources` - Community resource directory
- `resource_referrals` - Patient referrals to resources
- `sdoh_risk_scores` - SDOH risk scores

## Configuration

### Database
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/hdim_sdoh
    username: hdim_user
    password: hdim_pass
```

### Redis Cache
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

### Server
```yaml
server:
  port: 8089
```

## Security

- JWT-based authentication
- Role-based access control (RBAC)
- Required roles: ANALYST, EVALUATOR, ADMIN, SUPER_ADMIN
- Tenant isolation via X-Tenant-ID header

## Standards Compliance

### Gravity Project FHIR IG
- Implements Gravity Project FHIR Implementation Guide
- SDOH Clinical Care FHIR profiles
- Standardized terminology and value sets

### ICD-10-CM
- Complete Z55-Z65 code range support
- Accurate code descriptions
- Category-to-code mappings

### LOINC
- Screening questions coded with LOINC
- AHC-HRSN and PRAPARE LOINC codes

### CMS Health Equity
- Supports CMS health equity measures
- Stratification reporting
- Disparity tracking

## Development

### Build
```bash
./gradlew :modules:services:sdoh-service:build
```

### Run
```bash
./gradlew :modules:services:sdoh-service:bootRun
```

### Docker
```bash
docker build -t hdim-sdoh-service .
docker run -p 8089:8089 hdim-sdoh-service
```

## API Documentation

Interactive API documentation available at:
- Swagger UI: http://localhost:8089/swagger-ui.html
- OpenAPI Spec: http://localhost:8089/v3/api-docs

## Example Usage

### Submit SDOH Screening
```bash
curl -X POST http://localhost:8089/api/v1/sdoh/screening/patient-001 \
  -H "X-Tenant-ID: tenant-001" \
  -H "Content-Type: application/json" \
  -d '{
    "screeningTool": "AHC-HRSN",
    "responses": [
      {
        "questionId": "ahc-food-1",
        "answer": "Often true"
      }
    ]
  }'
```

### Search Community Resources
```bash
curl http://localhost:8089/api/v1/sdoh/resources?category=FOOD&city=Boston&state=MA \
  -H "X-Tenant-ID: tenant-001"
```

### Generate Health Equity Report
```bash
curl "http://localhost:8089/api/v1/sdoh/equity/report?startDate=2024-01-01&endDate=2024-12-31" \
  -H "X-Tenant-ID: tenant-001"
```

## License

Copyright © 2024 Mahoosuc Solutions. All rights reserved.

## Support

For support and questions, contact the HDIM development team.
