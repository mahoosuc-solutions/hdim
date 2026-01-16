# DevOps Agent - FHIR Data Validation

## Overview

The DevOps Agent Service includes comprehensive FHIR data validation to ensure demo data authenticity and completeness. This validation runs automatically during demo orchestration to verify that all demo data meets quality standards.

## Validation Checks

### 1. Resource Count Validation

Validates that minimum required resource counts are met:

- **Patient**: Minimum 50
- **Condition**: Minimum 50
- **Observation**: Minimum 200
- **MedicationRequest**: Minimum 50
- **Encounter**: Minimum 50
- **Procedure**: Minimum 20
- **Immunization**: Minimum 30
- **AllergyIntolerance**: Minimum 20

### 2. Code System Validation

Validates that required codes for quality measures are present:

- **Diabetes Mellitus Type 2** (SNOMED: 44054006)
- **Essential Hypertension** (SNOMED: 59621000)
- **HbA1c** (LOINC: 4548-4)
- **Systolic Blood Pressure** (LOINC: 8480-6)
- **PHQ-9 Depression Screening** (LOINC: 44249-1)

### 3. Data Authenticity Checks

- **Patient Name Completeness**: All patients should have family names
- **Patient Birth Date Completeness**: All patients should have birth dates
- **Observation Value Completeness**: Observations should have values

### 4. FHIR Compliance Checks

- **FHIR Version**: Validates server is R4 (4.0.1)
- **Resource Validation**: Validates sample resources against FHIR R4 profiles
- **Metadata Endpoint**: Verifies FHIR metadata endpoint is accessible

### 5. Relationship Validation

- **Patient-Observation Relationships**: Validates patients have associated observations
- **Average Resources Per Patient**: Ensures realistic data distribution

## API Endpoints

### Validate FHIR Data

```http
POST /api/v1/devops/fhir-validation/validate
```

**Response:**
```json
{
  "validationId": "uuid",
  "validationTimestamp": "2026-01-15T10:00:00Z",
  "overallStatus": "PASS",
  "totalChecks": 25,
  "passedChecks": 23,
  "failedChecks": 0,
  "warningChecks": 2,
  "resourceCountChecks": [...],
  "codeSystemChecks": [...],
  "authenticityChecks": [...],
  "complianceChecks": [...],
  "relationshipChecks": [...],
  "summary": {
    "resourceCounts": {...},
    "totalResources": 450
  }
}
```

### Get Validation Status

```http
GET /api/v1/devops/fhir-validation/status
```

Returns the most recent validation result.

## Integration with Demo Orchestrator

The FHIR validation is automatically called during the demo orchestration workflow:

1. **After Data Seeding**: Validates that seeded data meets requirements
2. **Before Scenario Execution**: Ensures data is ready for demo scenarios
3. **After Scenario Execution**: Validates data integrity after scenario runs

## Validation Status

- **PASS**: All critical checks passed
- **WARN**: Some non-critical checks failed or have warnings
- **FAIL**: Critical checks failed

## Usage in Demo Orchestration

The `DataManagerService` in the demo-orchestrator-service calls the DevOps Agent's FHIR validation:

```java
FhirValidationResultDto result = dataManager.validateFhirData();
```

This validation result is included in the demo orchestration report and displayed in the DevOps Agent UI.

## Configuration

Configure the FHIR service URL in `application.yml`:

```yaml
hdim:
  services:
    fhir:
      url: ${FHIR_SERVICE_URL:http://fhir-service:8085/fhir}
```

## Files

- **Service**: `backend/modules/services/devops-agent-service/src/main/java/com/healthdata/devops/validation/FhirDataValidationService.java`
- **Client**: `backend/modules/services/devops-agent-service/src/main/java/com/healthdata/devops/client/FhirServiceClient.java`
- **Controller**: `backend/modules/services/devops-agent-service/src/main/java/com/healthdata/devops/controller/FhirValidationController.java`
- **Model**: `backend/modules/services/devops-agent-service/src/main/java/com/healthdata/devops/model/FhirValidationResult.java`
