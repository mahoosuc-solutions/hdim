# AI Audit RBAC Integration - Integration Testing Guide

## Table of Contents
1. [Overview](#overview)
2. [Test Environment Setup](#test-environment-setup)
3. [API Endpoint Testing](#api-endpoint-testing)
4. [UI Workflow Testing](#ui-workflow-testing)
5. [Security Testing](#security-testing)
6. [Performance Benchmarks](#performance-benchmarks)
7. [Automated Test Scripts](#automated-test-scripts)
8. [Common Issues & Troubleshooting](#common-issues--troubleshooting)

---

## Overview

This guide provides comprehensive testing procedures for the AI Audit RBAC Integration, covering all 7 phases of implementation:
- Phase 1: QA Review API
- Phase 2: MPI Audit API
- Phase 3: Clinical Decision API
- Phase 4: Angular HTTP Services
- Phase 5: Clinical Dashboard Templates
- Phase 6: Angular Routing with Guards
- Phase 7: Integration Documentation (this document)

### Testing Scope
- **21 REST API endpoints** across 3 audit domains
- **4 Angular services** with HTTP client integration
- **2 Angular dashboard components** with comprehensive UI
- **10 RBAC roles** with permission testing
- **Database operations** (4 tables, 17 indexes, 8 JSONB columns)

---

## Test Environment Setup

### Prerequisites
```bash
# Backend Requirements
- Java 17 or higher
- PostgreSQL 14+ with JSONB support
- Gradle 8+
- Spring Boot 3.3.5

# Frontend Requirements
- Node.js 18+
- Angular 16+
- npm 9+

# Testing Tools
- Postman or curl for API testing
- Chrome DevTools for frontend debugging
- pgAdmin or psql for database verification
```

### Database Setup

1. **Create Test Database**
```sql
CREATE DATABASE hdim_test;
\c hdim_test;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
```

2. **Run Migrations**
```bash
cd backend
./gradlew flywayMigrate -Dflyway.url=jdbc:postgresql://localhost:5432/hdim_test
```

3. **Verify Tables Created**
```sql
-- Should show 4 audit tables
SELECT tablename FROM pg_tables 
WHERE schemaname = 'public' 
AND tablename IN ('qa_reviews', 'mpi_merges', 'data_quality_issues', 'clinical_decisions');

-- Verify indexes (should show 17 total)
SELECT indexname FROM pg_indexes 
WHERE schemaname = 'public' 
AND tablename IN ('qa_reviews', 'mpi_merges', 'data_quality_issues', 'clinical_decisions');
```

### Test Data Generation

**Create Test Data Script** (`backend/src/test/resources/test-data.sql`):
```sql
-- Insert QA Review test data
INSERT INTO qa_reviews (id, tenant_id, ai_decision_type, ai_recommendation, confidence_score, 
                        review_status, priority, created_at, updated_at)
VALUES 
  (uuid_generate_v4(), 'test-tenant', 'MEDICATION_RECOMMENDATION', 
   '{"drug": "Lisinopril", "dosage": "10mg"}', 0.92, 'PENDING', 'HIGH', NOW(), NOW()),
  (uuid_generate_v4(), 'test-tenant', 'CARE_GAP_DETECTION', 
   '{"service": "Diabetes screening", "overdue_days": 30}', 0.85, 'APPROVED', 'MEDIUM', NOW(), NOW());

-- Insert MPI Merge test data
INSERT INTO mpi_merges (id, tenant_id, source_patient_id, target_patient_id, merge_type, 
                        confidence_score, merge_status, validation_status, merge_timestamp, 
                        source_patient_snapshot, target_patient_snapshot, created_at, updated_at)
VALUES
  (uuid_generate_v4(), 'test-tenant', 'P001', 'P002', 'AUTOMATIC', 0.88, 'COMPLETED', 
   'PENDING_VALIDATION', NOW(), 
   '{"firstName": "John", "lastName": "Doe", "dob": "1980-01-15"}'::jsonb,
   '{"firstName": "John", "lastName": "Doe", "dob": "1980-01-15"}'::jsonb,
   NOW(), NOW());

-- Insert Data Quality Issue test data
INSERT INTO data_quality_issues (id, tenant_id, patient_id, issue_type, severity, status, 
                                 affected_field, current_value, detected_at, created_at, updated_at)
VALUES
  (uuid_generate_v4(), 'test-tenant', 'P001', 'DUPLICATE', 'HIGH', 'OPEN', 
   'ssn', '***-**-1234', NOW(), NOW(), NOW());

-- Insert Clinical Decision test data
INSERT INTO clinical_decisions (id, tenant_id, patient_id, decision_type, alert_severity, 
                                decision_timestamp, review_status, evidence_grade, 
                                confidence_score, specialty_area, created_at, updated_at)
VALUES
  (uuid_generate_v4(), 'test-tenant', 'P003', 'MEDICATION_ALERT', 'CRITICAL', NOW(), 
   'PENDING', 'A', 0.95, 'CARDIOLOGY', NOW(), NOW()),
  (uuid_generate_v4(), 'test-tenant', 'P004', 'CARE_GAP', 'MODERATE', NOW(), 
   'APPROVED', 'B', 0.82, 'ENDOCRINOLOGY', NOW(), NOW());
```

**Load Test Data**:
```bash
psql -d hdim_test -f backend/src/test/resources/test-data.sql
```

### Start Backend Server
```bash
cd backend
./gradlew bootRun

# Verify server started
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

### Start Frontend Server
```bash
cd frontend
npm install
npm start

# Verify Angular dev server
curl http://localhost:4200
# Expected: HTML content
```

---

## API Endpoint Testing

### Phase 1: QA Review API Testing

#### Test 1.1: Get QA Review History
```bash
# Basic request
curl -X GET "http://localhost:8080/api/v1/qa-review/reviews?page=0&size=20" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"

# Expected Response:
{
  "content": [
    {
      "reviewId": "uuid-here",
      "aiDecisionType": "MEDICATION_RECOMMENDATION",
      "aiRecommendation": "...",
      "confidenceScore": 0.92,
      "reviewStatus": "PENDING",
      "priority": "HIGH",
      "createdAt": "2026-01-13T10:00:00Z",
      "hasDiscrepancy": false
    }
  ],
  "totalElements": 2,
  "totalPages": 1,
  "size": 20,
  "number": 0
}

# Test filtering
curl -X GET "http://localhost:8080/api/v1/qa-review/reviews?reviewStatus=PENDING&priority=HIGH" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Test pagination
curl -X GET "http://localhost:8080/api/v1/qa-review/reviews?page=1&size=10&sort=createdAt,desc" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Validation Checklist:**
- ✅ Returns paginated results
- ✅ Filters work correctly
- ✅ Sorting is applied
- ✅ Tenant isolation (only test-tenant data returned)
- ✅ HTTP 200 status code
- ✅ Valid JSON response

#### Test 1.2: Get QA Review Detail
```bash
curl -X GET "http://localhost:8080/api/v1/qa-review/reviews/{reviewId}" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Expected Response:
{
  "reviewId": "uuid-here",
  "aiDecisionType": "MEDICATION_RECOMMENDATION",
  "reviewStatus": "PENDING",
  "createdAt": "2026-01-13T10:00:00Z",
  "aiDecisionContext": {...},
  "reviewerAssessment": null,
  "discrepancyAnalysis": null,
  "reviewHistory": []
}
```

**Validation Checklist:**
- ✅ Returns detailed review information
- ✅ JSONB fields properly deserialized
- ✅ 404 if review not found
- ✅ 403 if wrong tenant

#### Test 1.3: Submit QA Review
```bash
curl -X POST "http://localhost:8080/api/v1/qa-review/reviews/{reviewId}/submit" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reviewOutcome": "APPROVED",
    "reviewNotes": "Decision aligns with clinical guidelines",
    "hasDiscrepancy": false
  }'

# Expected Response:
{
  "reviewId": "uuid-here",
  "reviewOutcome": "APPROVED",
  "reviewedBy": "test.user@example.com",
  "reviewedAt": "2026-01-13T15:30:00Z",
  "success": true,
  "message": "Review submitted successfully"
}
```

**Validation Checklist:**
- ✅ Review status updated in database
- ✅ reviewedBy set to authenticated user
- ✅ reviewedAt timestamp recorded
- ✅ reviewNotes saved
- ✅ 400 if validation fails (missing required fields)
- ✅ 404 if review not found

#### Test 1.4: Get QA Metrics
```bash
curl -X GET "http://localhost:8080/api/v1/qa-review/metrics?startDate=2026-01-01&endDate=2026-01-31" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Expected Response:
{
  "totalReviews": 50,
  "approvedReviews": 42,
  "rejectedReviews": 5,
  "pendingReviews": 3,
  "approvalRate": 84.0,
  "discrepancyRate": 8.0,
  "averageConfidenceScore": 0.87,
  "averageReviewTimeHours": 12,
  "decisionTypeDistribution": {...},
  "priorityDistribution": {...}
}
```

**Validation Checklist:**
- ✅ Metrics calculated correctly
- ✅ Date range filtering works
- ✅ Distribution objects populated
- ✅ Rates calculated as percentages

#### Test 1.5: Get QA Accuracy Trends
```bash
curl -X GET "http://localhost:8080/api/v1/qa-review/trends?days=30" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Expected Response:
{
  "dailyTrends": [
    {
      "date": "2026-01-13",
      "totalReviews": 5,
      "approved": 4,
      "rejected": 1,
      "approvalRate": 80.0,
      "discrepancyRate": 20.0
    }
  ],
  "averageApprovalRate": 84.5,
  "averageDiscrepancyRate": 8.2
}
```

**Validation Checklist:**
- ✅ Daily trends grouped by date
- ✅ Rates calculated correctly
- ✅ Sorted by date ascending
- ✅ Days parameter respected

### Phase 2: MPI Audit API Testing

#### Test 2.1: Get MPI Merge History
```bash
curl -X GET "http://localhost:8080/api/v1/mpi/merges?mergeStatus=COMPLETED&minConfidenceScore=0.8" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Expected Response:
{
  "content": [
    {
      "mergeId": "uuid-here",
      "sourcePatientId": "P001",
      "targetPatientId": "P002",
      "confidenceScore": 0.88,
      "mergeStatus": "COMPLETED",
      "validationStatus": "PENDING_VALIDATION",
      "mergeTimestamp": "2026-01-13T10:00:00Z",
      "dataQualityIssueCount": 1,
      "priority": "MEDIUM"
    }
  ],
  "totalElements": 1,
  "totalPages": 1
}
```

**Validation Checklist:**
- ✅ 8-parameter filtering works
- ✅ Confidence score range filtering
- ✅ Priority calculated correctly (< 0.7 = CRITICAL, < 0.8 = HIGH, < 0.9 = MEDIUM)
- ✅ Data quality issue count accurate

#### Test 2.2: Get Merge Detail
```bash
curl -X GET "http://localhost:8080/api/v1/mpi/merges/{mergeId}" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Expected Response:
{
  "mergeId": "uuid-here",
  "mergeStatus": "COMPLETED",
  "validationStatus": "PENDING_VALIDATION",
  "confidenceScore": 0.88,
  "sourcePatientSnapshot": {
    "firstName": "John",
    "lastName": "Doe",
    "dob": "1980-01-15"
  },
  "targetPatientSnapshot": {...},
  "mergedPatientSnapshot": {...},
  "matchingAlgorithmDetails": {...},
  "attributeMatches": [...],
  "attributeConflicts": [...],
  "dataQualityIssues": [...],
  "validationHistory": []
}
```

**Validation Checklist:**
- ✅ JSONB patient snapshots deserialized
- ✅ Related data quality issues loaded
- ✅ Matching details parsed correctly
- ✅ 404 if merge not found

#### Test 2.3: Validate Merge
```bash
curl -X POST "http://localhost:8080/api/v1/mpi/merges/{mergeId}/validate" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "validationOutcome": "VALID",
    "validationNotes": "Merge appears correct after manual review",
    "hasMergeErrors": false,
    "hasDataQualityIssues": false
  }'

# Expected Response:
{
  "mergeId": "uuid-here",
  "validationOutcome": "VALID",
  "validatedBy": "mpi.analyst@example.com",
  "validatedAt": "2026-01-13T15:45:00Z",
  "success": true,
  "message": "Merge validated successfully"
}
```

**Validation Checklist:**
- ✅ Merge validationStatus updated to VALIDATED
- ✅ validatedBy, validatedAt, validationNotes saved
- ✅ Transaction committed
- ✅ 400 if validation fails

#### Test 2.4: Rollback Merge
```bash
curl -X POST "http://localhost:8080/api/v1/mpi/merges/{mergeId}/rollback" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "rollbackReason": "Merge was incorrect - patients are not the same person",
    "recreateSourcePatient": true,
    "preserveTargetPatient": true,
    "rollbackStrategy": "FULL_ROLLBACK"
  }'

# Expected Response:
{
  "mergeId": "uuid-here",
  "rollbackStatus": "ROLLED_BACK",
  "rolledBackBy": "mpi.analyst@example.com",
  "rolledBackAt": "2026-01-13T16:00:00Z",
  "success": true,
  "message": "Merge rolled back successfully",
  "restoredSourcePatientId": "P001",
  "updatedTargetPatientId": "P002"
}
```

**Validation Checklist:**
- ✅ Merge status changed to ROLLED_BACK
- ✅ Rollback metadata saved
- ✅ Patient IDs returned
- ✅ Transaction integrity maintained

#### Test 2.5: Get Data Quality Issues
```bash
curl -X GET "http://localhost:8080/api/v1/mpi/data-quality/issues?severity=HIGH&status=OPEN" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Expected Response:
{
  "content": [
    {
      "issueId": "uuid-here",
      "patientId": "P001",
      "issueType": "DUPLICATE",
      "severity": "HIGH",
      "status": "OPEN",
      "affectedField": "ssn",
      "currentValue": "***-**-1234",
      "suggestedValue": null,
      "recommendation": "Verify SSN with patient",
      "detectedAt": "2026-01-13T10:00:00Z"
    }
  ]
}
```

**Validation Checklist:**
- ✅ Filtering by severity and status works
- ✅ Patient-specific issues returned
- ✅ Recommendations present

#### Test 2.6: Resolve Data Quality Issue
```bash
curl -X POST "http://localhost:8080/api/v1/mpi/data-quality/issues/{issueId}/resolve" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "resolutionAction": "MANUAL_FIX",
    "correctedValue": "123-45-6789",
    "resolutionNotes": "Verified SSN with patient records"
  }'

# Expected Response:
{
  "issueId": "uuid-here",
  "status": "RESOLVED",
  "resolvedBy": "data.steward@example.com",
  "resolvedAt": "2026-01-13T16:15:00Z",
  "correctedValue": "123-45-6789"
}
```

**Validation Checklist:**
- ✅ Issue status updated to RESOLVED
- ✅ Resolution metadata saved
- ✅ Corrected value recorded

### Phase 3: Clinical Decision API Testing

#### Test 3.1: Get Clinical Decision History
```bash
curl -X GET "http://localhost:8080/api/v1/clinical/decisions?decisionType=MEDICATION_ALERT&alertSeverity=CRITICAL" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Expected Response:
{
  "content": [
    {
      "decisionId": "uuid-here",
      "decisionType": "MEDICATION_ALERT",
      "patientId": "P003",
      "patientName": "Jane Smith",
      "alertSeverity": "CRITICAL",
      "reviewStatus": "PENDING",
      "decisionTimestamp": "2026-01-13T10:00:00Z",
      "evidenceGrade": "A",
      "confidenceScore": 0.95,
      "specialtyArea": "CARDIOLOGY",
      "clinicalRecommendation": "Avoid combining medications",
      "priority": "CRITICAL",
      "hasOverride": false,
      "relatedAlertsCount": 0
    }
  ]
}
```

**Validation Checklist:**
- ✅ 8-parameter filtering works
- ✅ Evidence grade filtering
- ✅ Specialty area filtering
- ✅ Priority calculated from severity and confidence

#### Test 3.2: Review Clinical Decision
```bash
curl -X POST "http://localhost:8080/api/v1/clinical/decisions/{decisionId}/review" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reviewOutcome": "APPROVED",
    "reviewNotes": "Alert is clinically appropriate",
    "applyOverride": false
  }'

# Expected Response:
{
  "decisionId": "uuid-here",
  "reviewOutcome": "APPROVED",
  "reviewedBy": "clinician@example.com",
  "reviewedAt": "2026-01-13T16:30:00Z",
  "success": true,
  "message": "Decision reviewed successfully",
  "overrideApplied": false
}
```

**Validation Checklist:**
- ✅ Review status updated
- ✅ Override logic works when applyOverride=true
- ✅ Override metadata saved

#### Test 3.3: Get Medication Alerts
```bash
curl -X GET "http://localhost:8080/api/v1/clinical/medication-alerts?severity=CRITICAL" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Expected Response:
{
  "content": [
    {
      "alertId": "uuid-here",
      "patientId": "P003",
      "alertType": "INTERACTION",
      "severity": "CRITICAL",
      "involvedMedications": ["Warfarin", "Aspirin"],
      "alertMessage": "Drug interaction detected",
      "clinicalRecommendation": "Consider alternative anticoagulant",
      "evidenceGrade": "A",
      "acknowledged": false
    }
  ]
}
```

**Validation Checklist:**
- ✅ Only MEDICATION_ALERT decisions returned
- ✅ Severity filtering works
- ✅ Medications array populated

#### Test 3.4: Get Care Gaps
```bash
curl -X GET "http://localhost:8080/api/v1/clinical/care-gaps?status=OPEN" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Expected Response:
{
  "content": [
    {
      "gapId": "uuid-here",
      "patientId": "P004",
      "gapType": "SCREENING_OVERDUE",
      "serviceDescription": "HbA1c screening for diabetes management",
      "dueDate": "2025-12-15",
      "daysPastDue": 29,
      "priority": "HIGH",
      "guidelineReference": "ADA Diabetes Care Guidelines 2024",
      "status": "OPEN",
      "evidenceGrade": "B"
    }
  ]
}
```

**Validation Checklist:**
- ✅ Only CARE_GAP decisions returned
- ✅ Days past due calculated correctly
- ✅ Guideline references present

#### Test 3.5: Get Risk Stratifications
```bash
curl -X GET "http://localhost:8080/api/v1/clinical/risk-stratifications?riskLevel=HIGH" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Expected Response:
{
  "content": [
    {
      "stratificationId": "uuid-here",
      "patientId": "P005",
      "riskCategory": "CARDIOVASCULAR",
      "overallRiskLevel": "HIGH",
      "riskScore": 0.82,
      "contributingFactors": [
        {
          "factorName": "Hypertension",
          "factorValue": "Stage 2",
          "contribution": "HIGH",
          "modifiable": true
        }
      ],
      "assessmentModel": "AI-BASED",
      "evidenceGrade": "A",
      "recommendedInterventions": ["Lifestyle modification", "Medication adjustment"]
    }
  ]
}
```

**Validation Checklist:**
- ✅ Only RISK_STRATIFICATION decisions returned
- ✅ Risk factors properly structured
- ✅ Interventions array populated

#### Test 3.6: Get Clinical Metrics
```bash
curl -X GET "http://localhost:8080/api/v1/clinical/metrics" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Expected Response:
{
  "totalDecisions": 150,
  "approvedDecisions": 120,
  "rejectedDecisions": 10,
  "pendingReview": 20,
  "approvalRate": 80.0,
  "overrideRate": 5.0,
  "averageConfidenceScore": 0.89,
  "averageReviewTimeHours": 18,
  "decisionTypeDistribution": {
    "medicationAlerts": 60,
    "careGaps": 50,
    "riskStratifications": 30,
    "clinicalPathways": 10
  },
  "severityDistribution": {
    "critical": 15,
    "high": 45,
    "moderate": 60,
    "low": 30
  },
  "evidenceGradeDistribution": {
    "gradeA": 80,
    "gradeB": 50,
    "gradeC": 15,
    "gradeD": 5
  }
}
```

**Validation Checklist:**
- ✅ All metrics calculated
- ✅ Distribution objects complete
- ✅ Rates calculated correctly

---

## UI Workflow Testing

### Clinical Audit Dashboard Testing

#### Test UI-1: Dashboard Load
1. Navigate to `http://localhost:4200/audit/clinical`
2. **Expected:**
   - Dashboard loads without errors
   - 5 metric cards display (Total Decisions, Approval Rate, Avg Confidence, Pending Review, Override Rate)
   - Tabs visible: Decisions, Medication Alerts, Care Gaps, Risk Stratification, Metrics & Trends
   - Default tab is "Decisions"

#### Test UI-2: Filter Decisions
1. Select "Medication Alert" from Decision Type dropdown
2. Select "Critical" from Severity dropdown
3. Click filter or press Enter
4. **Expected:**
   - Table updates with filtered results
   - Only MEDICATION_ALERT decisions with CRITICAL severity shown
   - Badge counts update

#### Test UI-3: View Decision Detail
1. Click "View" button on any decision
2. **Expected:**
   - Modal opens with decision details
   - Patient context visible
   - Clinical recommendation displayed
   - Evidence grade shown
   - Review history visible (if any)

#### Test UI-4: Quick Approve Decision
1. Find a decision with status "PENDING"
2. Click the green checkmark button
3. **Expected:**
   - Confirmation dialog appears
   - After confirmation, status changes to "APPROVED"
   - Table refreshes
   - Metric cards update

#### Test UI-5: Medication Alerts Tab
1. Click "Medication Alerts" tab
2. **Expected:**
   - Tab switches
   - Medication alerts loaded
   - Severity filter visible
   - Alert cards show: alert type, severity, involved medications, recommendation
   - Evidence grade badge visible

#### Test UI-6: Care Gaps Tab
1. Click "Care Gaps" tab
2. **Expected:**
   - Care gaps loaded
   - Status filter works (OPEN, IN_PROGRESS, CLOSED, DISMISSED)
   - Due dates displayed
   - Days overdue calculated and shown in red if > 0
   - Guideline references visible

#### Test UI-7: Risk Stratification Tab
1. Click "Risk Stratification" tab
2. **Expected:**
   - Risk assessments loaded
   - Risk level filter works (HIGH, MODERATE, LOW)
   - Contributing factors list displayed
   - Modifiable badge shown where applicable
   - Recommended interventions list visible

#### Test UI-8: Metrics & Trends Tab
1. Click "Metrics & Trends" tab
2. **Expected:**
   - Distribution charts visible
   - Decision Type Distribution shows counts
   - Severity Distribution shows counts
   - Evidence Grade Distribution shows counts
   - Performance metrics displayed (approval rate, rejection rate, override rate, avg review time)
   - Trend summary shows 30-day averages

#### Test UI-9: Pagination
1. On Decisions tab, click "Next" button
2. **Expected:**
   - Page increments
   - New set of decisions loaded
   - "Previous" button enabled
   - Page indicator updates (e.g., "Page 2 of 5")

#### Test UI-10: Clear Filters
1. Apply multiple filters
2. Click "Clear Filters" button
3. **Expected:**
   - All filter fields reset
   - Table reloads with unfiltered data
   - All decisions visible again

---

## Security Testing

### RBAC Permission Testing

#### Test SEC-1: QA Analyst Role
```bash
# Login as QA_ANALYST user
TOKEN=$(curl -X POST http://localhost:8080/auth/login \
  -d '{"username":"qa.analyst@test.com","password":"test123"}' | jq -r '.token')

# Should succeed: View QA reviews
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/qa-review/reviews
# Expected: HTTP 200

# Should succeed: Submit QA review
curl -X POST -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/qa-review/reviews/{id}/submit \
  -d '{"reviewOutcome":"APPROVED"}'
# Expected: HTTP 200

# Should fail: Access MPI endpoints
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/mpi/merges
# Expected: HTTP 403 Forbidden
```

**Validation:**
- ✅ Can access QA endpoints
- ✅ Cannot access MPI endpoints
- ✅ Cannot access Clinical endpoints

#### Test SEC-2: MPI Analyst Role
```bash
# Login as MPI_ANALYST user
TOKEN=$(curl -X POST http://localhost:8080/auth/login \
  -d '{"username":"mpi.analyst@test.com","password":"test123"}' | jq -r '.token')

# Should succeed: View MPI merges
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/mpi/merges
# Expected: HTTP 200

# Should succeed: Validate merge
curl -X POST -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/mpi/merges/{id}/validate
# Expected: HTTP 200

# Should succeed: Rollback merge
curl -X POST -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/mpi/merges/{id}/rollback
# Expected: HTTP 200

# Should fail: Access QA endpoints
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/qa-review/reviews
# Expected: HTTP 403 Forbidden
```

**Validation:**
- ✅ Can access MPI endpoints
- ✅ Cannot access QA endpoints (unless also AUDITOR role)
- ✅ Can manage data quality issues

#### Test SEC-3: Clinician Role
```bash
# Login as CLINICIAN user
TOKEN=$(curl -X POST http://localhost:8080/auth/login \
  -d '{"username":"clinician@test.com","password":"test123"}' | jq -r '.token')

# Should succeed: View clinical decisions
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/clinical/decisions
# Expected: HTTP 200

# Should succeed: Review decision
curl -X POST -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/clinical/decisions/{id}/review
# Expected: HTTP 200

# Should succeed: View medication alerts
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/clinical/medication-alerts
# Expected: HTTP 200

# Should fail: Access MPI endpoints
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/mpi/merges
# Expected: HTTP 403 Forbidden
```

**Validation:**
- ✅ Can access Clinical endpoints
- ✅ Can review decisions
- ✅ Cannot access QA or MPI endpoints

#### Test SEC-4: Auditor Role (Read-Only)
```bash
# Login as AUDITOR user
TOKEN=$(curl -X POST http://localhost:8080/auth/login \
  -d '{"username":"auditor@test.com","password":"test123"}' | jq -r '.token')

# Should succeed: View all endpoints
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/qa-review/reviews
# Expected: HTTP 200

curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/mpi/merges
# Expected: HTTP 200

curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/clinical/decisions
# Expected: HTTP 200

# Should fail: Submit review (write operation)
curl -X POST -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/qa-review/reviews/{id}/submit
# Expected: HTTP 403 Forbidden (AUDITOR has read-only access)
```

**Validation:**
- ✅ Can view all audit data
- ✅ Cannot perform write operations
- ✅ Read-only access enforced

#### Test SEC-5: Tenant Isolation
```bash
# Login as user from tenant-1
TOKEN_1=$(curl -X POST http://localhost:8080/auth/login \
  -d '{"username":"user1@tenant1.com","password":"test123"}' | jq -r '.token')

# Create review for tenant-1
REVIEW_ID=$(curl -X POST -H "Authorization: Bearer $TOKEN_1" \
  http://localhost:8080/api/v1/qa-review/reviews \
  -d '{...}' | jq -r '.reviewId')

# Login as user from tenant-2
TOKEN_2=$(curl -X POST http://localhost:8080/auth/login \
  -d '{"username":"user2@tenant2.com","password":"test123"}' | jq -r '.token')

# Try to access tenant-1's review
curl -H "Authorization: Bearer $TOKEN_2" \
  http://localhost:8080/api/v1/qa-review/reviews/$REVIEW_ID
# Expected: HTTP 404 Not Found (tenant isolation working)
```

**Validation:**
- ✅ Users cannot access other tenants' data
- ✅ All queries filtered by tenantId
- ✅ 404 returned instead of 403 (security through obscurity)

### Authentication Testing

#### Test SEC-6: JWT Token Validation
```bash
# Test with expired token
curl -H "Authorization: Bearer EXPIRED_TOKEN" \
  http://localhost:8080/api/v1/qa-review/reviews
# Expected: HTTP 401 Unauthorized

# Test with invalid token
curl -H "Authorization: Bearer INVALID_TOKEN" \
  http://localhost:8080/api/v1/qa-review/reviews
# Expected: HTTP 401 Unauthorized

# Test with no token
curl http://localhost:8080/api/v1/qa-review/reviews
# Expected: HTTP 401 Unauthorized
```

**Validation:**
- ✅ Expired tokens rejected
- ✅ Invalid tokens rejected
- ✅ Missing tokens rejected
- ✅ Proper error messages returned

---

## Performance Benchmarks

### API Response Time Benchmarks

**Acceptable Limits:**
- GET endpoints (list): < 200ms
- GET endpoints (detail): < 100ms
- POST endpoints (write): < 300ms
- Metrics calculation: < 500ms
- Trend analysis: < 1000ms

#### Test PERF-1: List Endpoints Performance
```bash
# QA Review History (20 items)
time curl -w "\nTime: %{time_total}s\n" \
  http://localhost:8080/api/v1/qa-review/reviews?size=20
# Expected: < 200ms

# MPI Merge History (20 items)
time curl -w "\nTime: %{time_total}s\n" \
  http://localhost:8080/api/v1/mpi/merges?size=20
# Expected: < 200ms

# Clinical Decisions (20 items)
time curl -w "\nTime: %{time_total}s\n" \
  http://localhost:8080/api/v1/clinical/decisions?size=20
# Expected: < 200ms
```

#### Test PERF-2: Detail Endpoints Performance
```bash
time curl -w "\nTime: %{time_total}s\n" \
  http://localhost:8080/api/v1/qa-review/reviews/{id}
# Expected: < 100ms

time curl -w "\nTime: %{time_total}s\n" \
  http://localhost:8080/api/v1/mpi/merges/{id}
# Expected: < 100ms (includes JSONB deserialization)

time curl -w "\nTime: %{time_total}s\n" \
  http://localhost:8080/api/v1/clinical/decisions/{id}
# Expected: < 100ms
```

#### Test PERF-3: Metrics Calculation Performance
```bash
time curl -w "\nTime: %{time_total}s\n" \
  http://localhost:8080/api/v1/qa-review/metrics
# Expected: < 500ms

time curl -w "\nTime: %{time_total}s\n" \
  http://localhost:8080/api/v1/mpi/metrics
# Expected: < 500ms (includes COUNT, AVG queries)

time curl -w "\nTime: %{time_total}s\n" \
  http://localhost:8080/api/v1/clinical/metrics
# Expected: < 500ms
```

#### Test PERF-4: Trend Analysis Performance
```bash
time curl -w "\nTime: %{time_total}s\n" \
  "http://localhost:8080/api/v1/qa-review/trends?days=30"
# Expected: < 1000ms

time curl -w "\nTime: %{time_total}s\n" \
  "http://localhost:8080/api/v1/mpi/trends?days=30"
# Expected: < 1000ms (includes grouping by date)

time curl -w "\nTime: %{time_total}s\n" \
  "http://localhost:8080/api/v1/clinical/trends?days=30"
# Expected: < 1000ms
```

### Database Performance Testing

#### Test PERF-5: Index Effectiveness
```sql
-- Check index usage for QA reviews
EXPLAIN ANALYZE
SELECT * FROM qa_reviews 
WHERE tenant_id = 'test-tenant' 
AND review_status = 'PENDING'
ORDER BY created_at DESC
LIMIT 20;
-- Expected: Index Scan using idx_qa_tenant_status

-- Check index usage for MPI merges
EXPLAIN ANALYZE
SELECT * FROM mpi_merges
WHERE tenant_id = 'test-tenant'
AND validation_status = 'PENDING_VALIDATION'
LIMIT 20;
-- Expected: Index Scan using idx_mpi_validation_status

-- Check JSONB query performance
EXPLAIN ANALYZE
SELECT * FROM mpi_merges
WHERE source_patient_snapshot->>'firstName' = 'John'
LIMIT 20;
-- Expected: Should complete in < 100ms even without index
```

**Validation:**
- ✅ Indexes are being used
- ✅ Query plans are optimal
- ✅ No sequential scans on large tables
- ✅ JSONB queries acceptable performance

#### Test PERF-6: Concurrent Users Load Test
```bash
# Install Apache Bench
sudo apt-get install apache2-utils

# Test with 100 concurrent requests
ab -n 1000 -c 100 -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/qa-review/reviews

# Expected:
# - Requests per second: > 100
# - Mean response time: < 1000ms
# - 99th percentile: < 2000ms
# - No failed requests
```

---

## Automated Test Scripts

### Postman Collection

**Create Postman Collection** (`AI-Audit-RBAC-Integration.postman_collection.json`):

```json
{
  "info": {
    "name": "AI Audit RBAC Integration",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080"
    },
    {
      "key": "token",
      "value": ""
    }
  ],
  "item": [
    {
      "name": "Authentication",
      "item": [
        {
          "name": "Login",
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "pm.test(\"Login successful\", function() {",
                  "  pm.response.to.have.status(200);",
                  "  var jsonData = pm.response.json();",
                  "  pm.environment.set(\"token\", jsonData.token);",
                  "});"
                ]
              }
            }
          ],
          "request": {
            "method": "POST",
            "header": [],
            "body": {
              "mode": "raw",
              "raw": "{\"username\":\"test@example.com\",\"password\":\"test123\"}",
              "options": {
                "raw": {
                  "language": "json"
                }
              }
            },
            "url": {
              "raw": "{{baseUrl}}/auth/login",
              "host": ["{{baseUrl}}"],
              "path": ["auth", "login"]
            }
          }
        }
      ]
    },
    {
      "name": "QA Review API",
      "item": [
        {
          "name": "Get QA Reviews",
          "event": [
            {
              "listen": "test",
              "script": {
                "exec": [
                  "pm.test(\"Status code is 200\", function() {",
                  "  pm.response.to.have.status(200);",
                  "});",
                  "pm.test(\"Response has pagination\", function() {",
                  "  var jsonData = pm.response.json();",
                  "  pm.expect(jsonData).to.have.property('content');",
                  "  pm.expect(jsonData).to.have.property('totalElements');",
                  "});"
                ]
              }
            }
          ],
          "request": {
            "method": "GET",
            "header": [
              {
                "key": "Authorization",
                "value": "Bearer {{token}}"
              }
            ],
            "url": {
              "raw": "{{baseUrl}}/api/v1/qa-review/reviews?page=0&size=20",
              "host": ["{{baseUrl}}"],
              "path": ["api", "v1", "qa-review", "reviews"],
              "query": [
                {"key": "page", "value": "0"},
                {"key": "size", "value": "20"}
              ]
            }
          }
        }
      ]
    }
  ]
}
```

### Bash Test Script

**Create** `backend/test-integration.sh`:

```bash
#!/bin/bash

# AI Audit RBAC Integration Test Script
# Usage: ./test-integration.sh

set -e

BASE_URL="http://localhost:8080"
TOKEN=""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Test counter
PASSED=0
FAILED=0

# Helper function to run test
run_test() {
  local test_name=$1
  local endpoint=$2
  local expected_status=$3
  local method=${4:-GET}
  
  echo -n "Testing: $test_name... "
  
  if [ "$method" = "GET" ]; then
    response=$(curl -s -w "\n%{http_code}" -H "Authorization: Bearer $TOKEN" "$BASE_URL$endpoint")
  else
    response=$(curl -s -w "\n%{http_code}" -X "$method" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" "$BASE_URL$endpoint")
  fi
  
  status=$(echo "$response" | tail -n1)
  
  if [ "$status" = "$expected_status" ]; then
    echo -e "${GREEN}PASSED${NC}"
    ((PASSED++))
  else
    echo -e "${RED}FAILED${NC} (Expected: $expected_status, Got: $status)"
    ((FAILED++))
  fi
}

# Login first
echo "Logging in..."
TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"test@example.com","password":"test123"}' | jq -r '.token')

if [ -z "$TOKEN" ]; then
  echo "Login failed! Exiting."
  exit 1
fi

echo "Token acquired. Starting tests..."
echo ""

# QA Review API Tests
echo "=== QA Review API Tests ==="
run_test "Get QA Reviews" "/api/v1/qa-review/reviews" "200"
run_test "Get QA Metrics" "/api/v1/qa-review/metrics" "200"
run_test "Get QA Trends" "/api/v1/qa-review/trends?days=30" "200"
echo ""

# MPI Audit API Tests
echo "=== MPI Audit API Tests ==="
run_test "Get MPI Merges" "/api/v1/mpi/merges" "200"
run_test "Get Data Quality Issues" "/api/v1/mpi/data-quality/issues" "200"
run_test "Get MPI Metrics" "/api/v1/mpi/metrics" "200"
run_test "Get MPI Trends" "/api/v1/mpi/trends?days=30" "200"
echo ""

# Clinical Decision API Tests
echo "=== Clinical Decision API Tests ==="
run_test "Get Clinical Decisions" "/api/v1/clinical/decisions" "200"
run_test "Get Medication Alerts" "/api/v1/clinical/medication-alerts" "200"
run_test "Get Care Gaps" "/api/v1/clinical/care-gaps" "200"
run_test "Get Risk Stratifications" "/api/v1/clinical/risk-stratifications" "200"
run_test "Get Clinical Metrics" "/api/v1/clinical/metrics" "200"
run_test "Get Clinical Trends" "/api/v1/clinical/trends?days=30" "200"
echo ""

# Security Tests
echo "=== Security Tests ==="
run_test "Invalid Token Returns 401" "/api/v1/qa-review/reviews" "401"
echo ""

# Summary
echo "================================"
echo "Test Summary:"
echo -e "${GREEN}Passed: $PASSED${NC}"
echo -e "${RED}Failed: $FAILED${NC}"
echo "================================"

if [ $FAILED -eq 0 ]; then
  echo "All tests passed!"
  exit 0
else
  echo "Some tests failed!"
  exit 1
fi
```

**Make executable and run:**
```bash
chmod +x backend/test-integration.sh
./backend/test-integration.sh
```

---

## Common Issues & Troubleshooting

### Issue 1: 403 Forbidden on API Calls

**Symptoms:**
- API returns HTTP 403
- Error message: "Access Denied"

**Possible Causes:**
1. User doesn't have required role
2. JWT token expired
3. Tenant ID mismatch

**Solutions:**
```bash
# Check JWT token expiration
echo $TOKEN | jq -R 'split(".") | .[1] | @base64d | fromjson'

# Verify user roles
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/user/me

# Re-authenticate
TOKEN=$(curl -X POST http://localhost:8080/auth/login \
  -d '{"username":"user@test.com","password":"pass"}' | jq -r '.token')
```

### Issue 2: Empty Result Sets

**Symptoms:**
- API returns 200 but empty content array
- totalElements: 0

**Possible Causes:**
1. No test data in database
2. Tenant isolation filtering out data
3. Date filters too restrictive

**Solutions:**
```bash
# Check database has data
psql -d hdim_test -c "SELECT COUNT(*) FROM qa_reviews;"

# Check tenant ID in token matches data
psql -d hdim_test -c "SELECT DISTINCT tenant_id FROM qa_reviews;"

# Remove date filters
curl http://localhost:8080/api/v1/qa-review/reviews
# Instead of:
curl http://localhost:8080/api/v1/qa-review/reviews?startDate=2026-01-01
```

### Issue 3: Slow Query Performance

**Symptoms:**
- API calls take > 2 seconds
- Database CPU usage high

**Possible Causes:**
1. Missing indexes
2. Large dataset without pagination
3. Inefficient JPQL query

**Solutions:**
```sql
-- Check if indexes exist
SELECT indexname FROM pg_indexes 
WHERE tablename = 'qa_reviews';

-- Analyze query plan
EXPLAIN ANALYZE
SELECT * FROM qa_reviews WHERE tenant_id = 'test' LIMIT 20;

-- Rebuild indexes if needed
REINDEX TABLE qa_reviews;

-- Update statistics
ANALYZE qa_reviews;
```

### Issue 4: JSONB Deserialization Errors

**Symptoms:**
- 500 Internal Server Error
- Stack trace shows JSON parsing exception

**Possible Causes:**
1. Invalid JSON in JSONB column
2. Missing Hibernate type configuration
3. Null JSONB fields

**Solutions:**
```sql
-- Validate JSONB data
SELECT id, source_patient_snapshot::text 
FROM mpi_merges 
WHERE source_patient_snapshot IS NOT NULL 
LIMIT 5;

-- Check for NULL values
SELECT COUNT(*) FROM mpi_merges WHERE source_patient_snapshot IS NULL;
```

**Java fix:**
```java
// Ensure @Type annotation present
@Type(JsonBinaryType.class)
@Column(name = "source_patient_snapshot", columnDefinition = "jsonb")
private Map<String, Object> sourcePatientSnapshot;

// Add null check in service
if (entity.getSourcePatientSnapshot() != null) {
  detail.setSourcePatientSnapshot(entity.getSourcePatientSnapshot());
}
```

### Issue 5: Angular Service 404 Errors

**Symptoms:**
- Angular service calls return 404
- Console shows "GET http://localhost:4200/api/v1/... 404"

**Possible Causes:**
1. Wrong API base URL
2. Backend not running
3. CORS issue

**Solutions:**
```typescript
// Check environment.ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080'  // Not localhost:4200!
};

// Verify backend is running
curl http://localhost:8080/actuator/health
```

### Issue 6: CORS Blocked Requests

**Symptoms:**
- Console error: "has been blocked by CORS policy"
- Preflight OPTIONS request fails

**Possible Causes:**
1. CORS not configured in Spring Boot
2. Wrong allowed origins

**Solutions:**
```java
// Add CORS configuration
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:4200")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}
```

### Issue 7: Database Connection Errors

**Symptoms:**
- Application startup fails
- Error: "Connection to localhost:5432 refused"

**Solutions:**
```bash
# Check PostgreSQL is running
sudo systemctl status postgresql

# Start PostgreSQL
sudo systemctl start postgresql

# Verify connection
psql -h localhost -U postgres -d hdim_test

# Check application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/hdim_test
spring.datasource.username=postgres
spring.datasource.password=yourpassword
```

---

## Test Data Cleanup

**Clean up test data after testing:**
```sql
-- Delete test data
DELETE FROM qa_reviews WHERE tenant_id = 'test-tenant';
DELETE FROM mpi_merges WHERE tenant_id = 'test-tenant';
DELETE FROM data_quality_issues WHERE tenant_id = 'test-tenant';
DELETE FROM clinical_decisions WHERE tenant_id = 'test-tenant';

-- Reset sequences if needed
ALTER SEQUENCE qa_reviews_id_seq RESTART WITH 1;

-- Vacuum tables
VACUUM ANALYZE qa_reviews;
VACUUM ANALYZE mpi_merges;
VACUUM ANALYZE data_quality_issues;
VACUUM ANALYZE clinical_decisions;
```

---

## Continuous Integration Setup

**GitHub Actions Workflow** (`.github/workflows/integration-tests.yml`):

```yaml
name: Integration Tests

on:
  push:
    branches: [ master, develop ]
  pull_request:
    branches: [ master, develop ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:14
        env:
          POSTGRES_DB: hdim_test
          POSTGRES_USER: postgres
          POSTGRES_PASSWORD: postgres
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Run Backend Tests
      run: |
        cd backend
        ./gradlew test integrationTest
    
    - name: Set up Node.js
      uses: actions/setup-node@v3
      with:
        node-version: '18'
    
    - name: Run Frontend Tests
      run: |
        cd frontend
        npm ci
        npm run test:ci
    
    - name: Run Integration Tests
      run: |
        cd backend
        ./gradlew bootRun &
        sleep 30
        ./test-integration.sh
```

---

## Conclusion

This integration testing guide provides comprehensive coverage for all phases of the AI Audit RBAC Integration. Following these test procedures ensures:

✅ All 21 API endpoints function correctly  
✅ RBAC security is properly enforced  
✅ Tenant isolation works as expected  
✅ UI workflows operate smoothly  
✅ Performance benchmarks are met  
✅ Common issues can be quickly diagnosed and resolved  

**Next Steps:**
1. Run all tests in this guide
2. Document any failures
3. Fix issues and re-test
4. Set up CI/CD pipeline
5. Perform user acceptance testing
6. Deploy to staging environment

---

**Document Version:** 1.0  
**Last Updated:** 2026-01-13  
**Maintainer:** Development Team  
**Review Cycle:** After each sprint
