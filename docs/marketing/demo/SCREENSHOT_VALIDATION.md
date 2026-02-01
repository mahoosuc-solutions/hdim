# Screenshot Validation Guide

## Overview

This guide explains how to validate that screenshots contain proper data and all services are operational before capturing screenshots.

## Validation Process

### Step 1: Environment Validation

Before capturing screenshots, validate that all services are healthy:

```bash
node scripts/validate-demo-environment.js
```

**What it checks:**
- ✅ Infrastructure services (PostgreSQL, Redis, Kafka, Jaeger)
- ✅ Backend services (FHIR, CQL, Patient, Quality, Care Gap, Events)
- ✅ Gateway services (Admin, FHIR, Clinical, Edge)
- ✅ Frontend (Clinical Portal)
- ✅ Demo seeding service

**Expected output:**
```
✓ PostgreSQL
✓ Redis
✓ Kafka
✓ Jaeger
✓ FHIR Service
✓ CQL Engine
✓ Patient Service
✓ Quality Measure
✓ Care Gap Service
✓ Event Processing
✓ Gateway Edge
✓ Gateway Admin
✓ Clinical Portal
✓ Demo Seeding Service
```

### Step 2: Demo Data Validation

The validation script also checks that demo data is present:

```bash
node scripts/validate-demo-environment.js
```

**What it checks:**
- ✅ Patients exist in Patient Service
- ✅ Care gaps exist in Care Gap Service
- ✅ Quality measures exist in Quality Measure Service
- ✅ FHIR resources exist in FHIR Service

**Expected output:**
```
✓ Patients - Data present
✓ Care Gaps - Data present
✓ Quality Measures - Data present
✓ FHIR Resources - Data present
```

### Step 3: Data Seeding

If data is not present, seed it:

```bash
curl -X POST http://localhost:8098/demo/api/v1/demo/scenarios/hedis-evaluation
```

**Expected response:**
```json
{
  "status": "success",
  "message": "Demo scenario loaded successfully",
  "scenario": "hedis-evaluation",
  "patientsCreated": 100,
  "careGapsCreated": 250,
  "qualityMeasuresEvaluated": 15
}
```

### Step 4: Screenshot Capture with Validation

The enhanced screenshot script automatically validates data:

```bash
node scripts/capture-screenshots.js
```

**Validation features:**
- ✅ Checks for tables with data rows
- ✅ Checks for lists with items
- ✅ Checks for cards or data containers
- ✅ Checks for charts or visualizations
- ✅ Validates text content (word count > 50)
- ✅ Verifies screenshot file size (> 10KB)

**What you'll see:**
```
[INFO] Navigating to: http://localhost:4200/dashboard
[SUCCESS] Captured: care-manager-dashboard-overview.png (245.32KB)
```

**Warnings (if data is missing):**
```
[WARNING] Warning: dashboard-overview may not have data
[WARNING] Warning: dashboard-overview.png is very small (8.45KB) - may be empty
```

## Complete Workflow

### Automated (Recommended)

Use the complete script that does everything:

```bash
./scripts/run-demo-screenshots.sh
```

This script:
1. Starts all services
2. Waits for initialization
3. Validates environment
4. Seeds demo data (if needed)
5. Validates data presence
6. Captures screenshots with validation
7. Generates index

### Manual Steps

If you prefer manual control:

```bash
# 1. Start services
docker compose -f docker-compose.demo.yml up -d
sleep 120

# 2. Validate environment
node scripts/validate-demo-environment.js

# 3. Seed data (if needed)
curl -X POST http://localhost:8098/demo/api/v1/demo/scenarios/hedis-evaluation

# 4. Wait for data propagation
sleep 10

# 5. Capture screenshots
node scripts/capture-screenshots.js
```

## Screenshot Validation Criteria

### Data Presence Checks

The screenshot script validates pages contain:

1. **Tables with rows** - Data tables should have at least 1 row
2. **Lists with items** - Lists should have at least 3 items
3. **Cards/containers** - Data cards or containers should be present
4. **Charts/visualizations** - Charts (canvas, svg) should be present
5. **Text content** - Page should have at least 50 words of content

### File Size Validation

Screenshots are validated for reasonable size:
- **Minimum:** 10KB (smaller may indicate empty page)
- **Typical:** 50-500KB (normal pages with data)
- **Large:** 500KB+ (complex pages with many elements)

### Visual Validation

After capture, manually verify:
- ✅ Screenshots show actual data (not loading spinners)
- ✅ Tables contain rows with data
- ✅ Charts display properly
- ✅ Navigation is visible
- ✅ No error messages visible

## Troubleshooting

### Issue: Services Not Healthy

**Symptoms:**
- Validation script shows ✗ for services
- Health checks fail

**Solutions:**
1. Check service logs: `docker logs <container-name>`
2. Wait longer for services to start (some take 60-90 seconds)
3. Verify dependencies are healthy first
4. Check for port conflicts

### Issue: No Demo Data

**Symptoms:**
- Validation shows "No data found"
- Screenshots show empty pages

**Solutions:**
1. Seed demo data: `curl -X POST http://localhost:8098/demo/api/v1/demo/scenarios/hedis-evaluation`
2. Wait 10-15 seconds for data to propagate
3. Verify seeding service is healthy
4. Check seeding service logs

### Issue: Screenshots Are Empty

**Symptoms:**
- Screenshot files are very small (< 10KB)
- Pages show loading spinners
- No data visible

**Solutions:**
1. Increase wait times in `capture-screenshots.js`
2. Verify demo data is seeded
3. Check portal is accessible: `curl http://localhost:4200`
4. Verify login credentials are correct
5. Check browser console for errors (run in non-headless mode temporarily)

### Issue: Login Fails

**Symptoms:**
- Login page captured but authentication fails
- Error messages in screenshots

**Solutions:**
1. Verify credentials match demo users in database
2. Check gateway authentication is working
3. Verify JWT secret is configured correctly
4. Check gateway logs for authentication errors

## Validation Checklist

Before capturing screenshots, verify:

- [ ] All infrastructure services are healthy
- [ ] All backend services are healthy
- [ ] All gateway services are healthy
- [ ] Clinical portal is accessible
- [ ] Demo seeding service is healthy
- [ ] Demo data is seeded (patients, care gaps, quality measures)
- [ ] Portal login works with demo credentials
- [ ] Pages load with actual data (not empty)
- [ ] Screenshot script has correct base URL (localhost:4200)
- [ ] Playwright is installed: `npx playwright install chromium`

## Expected Screenshot Count

Based on configured scenarios:

| User Type | Pages | Total Screenshots |
|-----------|-------|------------------|
| Care Manager | 10 | 10 |
| Physician | 8 | 8 |
| Admin | 9 | 9 |
| AI User | 6 | 6 |
| Patient | 6 | 6 |
| Quality Manager | 5 | 5 |
| Data Analyst | 6 | 6 |
| **Total** | **50** | **50** |

## Output Structure

```
docs/screenshots/
├── INDEX.md                    # Screenshot index
├── care-manager/
│   ├── care-manager-login.png
│   ├── care-manager-dashboard-overview.png
│   └── ...
├── physician/
│   └── ...
└── ...
```

## Next Steps

After successful screenshot capture:

1. **Review INDEX.md** - Check all screenshots are listed
2. **Spot check screenshots** - Verify quality and data presence
3. **Select best shots** - Choose highest quality for marketing
4. **Update marketing materials** - Use screenshots in presentations
5. **Archive** - Store in version control or asset management
