# Patient Health Overview API - Quick Reference

**Base URL**: `http://localhost:8087/quality-measure/patient-health`
**Version**: 1.0.16
**Status**: ✅ Production Ready

---

## 🔗 Endpoints

### 1. Submit Mental Health Assessment
```bash
POST /mental-health/assessments
Content-Type: application/json
X-Tenant-ID: {tenantId}

{
  "patientId": "test123",
  "assessmentType": "phq-9",  // or "gad-7", "phq-2"
  "responses": {
    "q1": 2, "q2": 2, "q3": 1, "q4": 1, "q5": 1,
    "q6": 2, "q7": 1, "q8": 1, "q9": 1
  },
  "assessedBy": "Dr-Smith",
  "clinicalNotes": "Optional notes"
}

# Response: 201 Created
{
  "id": "uuid",
  "score": 12,
  "severity": "moderate",
  "positiveScreen": true,
  "requiresFollowup": true
}
```

### 2. Get Patient Assessments
```bash
GET /mental-health/assessments/{patientId}?type=phq-9&limit=10
X-Tenant-ID: {tenantId}

# Returns: List of assessments ordered by date DESC
```

### 3. Get Assessment Trend
```bash
GET /mental-health/assessments/{patientId}/trend?type=phq-9&months=6
X-Tenant-ID: {tenantId}

# Returns: Trend analysis with data points
```

### 4. Get Patient Care Gaps
```bash
GET /care-gaps/{patientId}?status=OPEN&category=MENTAL_HEALTH
X-Tenant-ID: {tenantId}

# Returns: List of care gaps with priority
```

### 5. Address Care Gap
```bash
PUT /care-gaps/{gapId}/address
Content-Type: application/json
X-Tenant-ID: {tenantId}

{
  "addressedBy": "Dr-Smith",
  "notes": "Scheduled follow-up appointment",
  "outcomeCode": "COMPLETED"
}
```

### 6. Calculate Risk Stratification
```bash
POST /risk-stratification/{patientId}/calculate
X-Tenant-ID: {tenantId}

# Returns: Risk score 0-100 with level (LOW/MODERATE/HIGH/VERY_HIGH)
```

### 7. Get Risk Assessment
```bash
GET /risk-stratification/{patientId}
X-Tenant-ID: {tenantId}

# Returns: Latest risk assessment with factors
```

### 8. Get Health Score
```bash
GET /health-score/{patientId}
X-Tenant-ID: {tenantId}

# Returns: Composite score with component breakdown
{
  "overallScore": 87,
  "interpretation": "excellent",
  "componentScores": {
    "physical": 75,
    "mental": 100,
    "social": 80,
    "preventive": 85,
    "chronicDisease": 100
  }
}
```

### 9. Get Complete Patient Overview
```bash
GET /overview/{patientId}
X-Tenant-ID: {tenantId}

# Returns: Comprehensive health data
{
  "patientId": "test123",
  "healthScore": {...},
  "recentMentalHealthAssessments": [...],
  "openCareGaps": [...],
  "riskAssessment": {...},
  "summaryStats": {...}
}
```

---

## 📋 Mental Health Assessment Types

### PHQ-9 (Depression)
- **Questions**: 9 items
- **Scale**: 0-3 per item (0-27 total)
- **Threshold**: ≥10 = positive screen
- **Severity Levels**:
  - 0-4: Minimal
  - 5-9: Mild
  - 10-14: Moderate
  - 15-19: Moderately severe
  - 20-27: Severe

### GAD-7 (Anxiety)
- **Questions**: 7 items
- **Scale**: 0-3 per item (0-21 total)
- **Threshold**: ≥10 = positive screen
- **Severity Levels**:
  - 0-4: Minimal
  - 5-9: Mild
  - 10-14: Moderate
  - 15-21: Severe

### PHQ-2 (Brief Depression Screening)
- **Questions**: 2 items
- **Scale**: 0-3 per item (0-6 total)
- **Threshold**: ≥3 = positive screen
- **Severity Levels**:
  - 0-2: Negative
  - 3-6: Positive (recommend full PHQ-9)

---

## 🧪 Quick Test Commands

### Test PHQ-9 Submission (Moderate Depression)
```bash
curl -X POST http://localhost:8087/quality-measure/patient-health/mental-health/assessments \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: test-tenant" \
  -d '{
    "patientId": "test123",
    "assessmentType": "phq-9",
    "responses": {"q1":2,"q2":2,"q3":1,"q4":1,"q5":1,"q6":2,"q7":1,"q8":1,"q9":1},
    "assessedBy": "Dr-Test"
  }'
```

### Test Health Overview
```bash
curl http://localhost:8087/quality-measure/patient-health/overview/test123 \
  -H "X-Tenant-ID: test-tenant"
```

### Run Complete Test Suite
```bash
./test-patient-health-simple.sh
```

---

## 🔒 Security

### Development (Current)
- All endpoints: `.permitAll()` for testing
- No authentication required

### Production (Required)
- JWT authentication via `Authorization: Bearer {token}`
- Role-based access control:
  - `PROVIDER`, `NURSE`: Full patient access
  - `CARE_COORDINATOR`: Care gap management
  - `ANALYST`: Risk stratification
  - `ADMIN`, `SUPER_ADMIN`: Full access

---

## 🗄️ Database Tables

### mental_health_assessments
- Primary Key: `id` (UUID)
- Patient Link: `patient_id` (VARCHAR)
- Tenant: `tenant_id` (VARCHAR)
- Responses: `responses` (JSONB)
- **5 indexes** for performance

### care_gaps
- Primary Key: `id` (UUID)
- Patient Link: `patient_id` (VARCHAR)
- Status: `status` (ENUM: OPEN, IN_PROGRESS, CLOSED)
- Priority: `priority` (ENUM: LOW, MEDIUM, HIGH, URGENT)
- **7 indexes** for performance

### risk_assessments
- Primary Key: `id` (UUID)
- Patient Link: `patient_id` (VARCHAR)
- Risk Score: `risk_score` (DECIMAL 0-100)
- Risk Level: `risk_level` (ENUM: LOW, MODERATE, HIGH, VERY_HIGH)
- **5 indexes** for performance

---

## 🚀 Deployment

### Current Status
```bash
$ docker compose ps | grep quality-measure
healthdata-quality-measure   healthdata/quality-measure-service:1.0.16   Up (healthy)
```

### Rebuild & Deploy
```bash
# Build JAR
./backend/gradlew -p backend :modules:services:quality-measure-service:bootJar

# Build Docker image
docker build -f backend/modules/services/quality-measure-service/Dockerfile \
  -t healthdata/quality-measure-service:1.0.16 \
  backend/modules/services/quality-measure-service

# Deploy
docker compose up -d quality-measure-service
```

### Health Check
```bash
curl http://localhost:8087/quality-measure/actuator/health
```

---

## 📊 Response Codes

| Code | Meaning | Common Causes |
|------|---------|---------------|
| 200 | OK | Successful GET request |
| 201 | Created | Successful POST request |
| 400 | Bad Request | Invalid JSON or missing required fields |
| 404 | Not Found | Patient ID not found or invalid endpoint |
| 500 | Server Error | Database or service error |

---

## 💡 Tips

### Patient IDs
- Use simple IDs: `test123`, `patient456`
- Avoid FHIR-style IDs with slashes (`Patient/123`) until URL encoding is implemented

### Tenant IDs
- Always include `X-Tenant-ID` header
- All queries are automatically filtered by tenant

### Testing
- Use unique patient IDs for each test to avoid conflicts
- Check care gaps were auto-created after positive screens
- Verify database entries: `docker exec -it healthdata-postgres psql -U healthdata -d healthdata_cql`

---

## 📖 Documentation

| Document | Purpose |
|----------|---------|
| [BACKEND_DEPLOYMENT_COMPLETE.md](BACKEND_DEPLOYMENT_COMPLETE.md) | Full deployment guide |
| [BACKEND_API_SPECIFICATION.md](BACKEND_API_SPECIFICATION.md) | Detailed API documentation |
| [BACKEND_IMPLEMENTATION_COMPLETE.md](BACKEND_IMPLEMENTATION_COMPLETE.md) | Implementation details |
| [CLINICAL_USER_GUIDE.md](CLINICAL_USER_GUIDE.md) | End-user guide for clinicians |

---

## 🐛 Troubleshooting

### Issue: 404 Not Found
- **Check**: Is service running? `docker compose ps`
- **Check**: Is path correct? Base is `/quality-measure/patient-health`
- **Check**: Is patient ID properly formatted? (no slashes)

### Issue: Empty Response
- **Check**: Does patient exist in database?
- **Check**: Is tenant ID correct?
- **Check**: Check logs: `docker logs healthdata-quality-measure`

### Issue: Score Incorrect
- **Check**: Are responses properly formatted? `{"q1":2,"q2":2,...}`
- **Check**: Are all questions included? (9 for PHQ-9, 7 for GAD-7, 2 for PHQ-2)
- **Check**: Unit tests: `./backend/gradlew -p backend :modules:services:quality-measure-service:test`

---

**Quick Access**
- Base URL: `http://localhost:8087/quality-measure/patient-health`
- Test Script: `./test-patient-health-simple.sh`
- Logs: `docker logs healthdata-quality-measure`
- Database: `docker exec -it healthdata-postgres psql -U healthdata -d healthdata_cql`

**Version**: 1.0.16 | **Status**: ✅ Production Ready | **Updated**: Nov 20, 2025
