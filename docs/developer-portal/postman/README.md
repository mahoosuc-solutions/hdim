# HDIM Platform - Postman Collections

**Status:** ✅ Production Ready
**Last Updated:** January 24, 2026
**Version:** 1.0

## Overview

Pre-built Postman collections for testing HDIM Platform APIs. Includes authentication, patient management, quality measures, care gaps, and prior authorization workflows.

---

## Quick Start

### 1. Import Environment

1. Open Postman
2. Click "Import" button
3. Select `HDIM-Environment.postman_environment.json`
4. Environment is now available in dropdown

### 2. Import Collections

Import all collections at once:

1. Click "Import" button
2. Select all `.postman_collection.json` files
3. Collections appear in left sidebar

### 3. Select Environment

1. Click environment dropdown (top-right)
2. Select "HDIM - Development"

### 4. Authenticate

1. Open "01 Authentication" collection
2. Run "Login" request
3. JWT token is automatically saved to environment

### 5. Start Testing!

All subsequent requests will use the saved JWT token automatically.

---

## Collections

| Collection | Description | Requests |
|------------|-------------|----------|
| **01 Authentication** | JWT login, token refresh, logout | 4 |
| **02 Patient Management** | Search, create, update patients | 5 |
| **03 Quality Measures** | HEDIS measure evaluation | 6 |
| **04 Care Gaps** | Care gap detection and closure | 3 |
| **05 Prior Authorization** | Prior auth requests | 3 |

---

## Environment Variables

The environment includes pre-configured variables:

### Service URLs

```
base_url: http://localhost:8001
gateway_url: http://localhost:8001
quality_measure_url: http://localhost:8087/quality-measure
patient_url: http://localhost:8084/patient
fhir_url: http://localhost:8085/fhir
care_gap_url: http://localhost:8086/care-gap
cql_engine_url: http://localhost:8081/cql-engine
prior_auth_url: http://localhost:8102/prior-auth
```

### Authentication

```
username: test_evaluator
password: password123
tenant_id: TENANT-001
jwt_token: (auto-populated after login)
refresh_token: (auto-populated after login)
```

### Dynamic Variables

These are auto-populated by test scripts:

```
patient_id: (from search results)
measure_id: (from measure list)
evaluation_id: (from evaluation)
care_gap_id: (from care gap list)
```

---

## Usage Examples

### Example 1: Search for a Patient

1. **Open:** "02 Patient Management > Search Patients"
2. **Edit Query Params:**
   - `lastName`: Smith
   - `dateOfBirth`: 1980-05-15
3. **Send Request**
4. **Result:** First patient ID is auto-saved to `{{patient_id}}`

### Example 2: Run Quality Measure Evaluation

1. **Prerequisites:** Have a valid `{{patient_id}}` (from patient search)
2. **Open:** "03 Quality Measures > List Measures"
3. **Send Request** - First measure ID is auto-saved
4. **Open:** "03 Quality Measures > Evaluate Measure"
5. **Send Request** - Evaluation runs automatically
6. **View Results:** Check "Get Evaluation Result"

### Example 3: Close a Care Gap

1. **Open:** "04 Care Gaps > List Patient Care Gaps"
2. **Send Request** - First gap ID is auto-saved
3. **Open:** "04 Care Gaps > Close Care Gap"
4. **Edit Body:**
   ```json
   {
     "closureReason": "LAB_COMPLETED",
     "closureDate": "2026-01-25T00:00:00Z",
     "notes": "HbA1c completed, value: 6.2%"
   }
   ```
5. **Send Request**

---

## Auto-Save Scripts

Collections include test scripts that automatically save response data to environment variables:

**Authentication:**
```javascript
// Saves JWT token after login
pm.environment.set("jwt_token", jsonData.token);
pm.environment.set("refresh_token", jsonData.refreshToken);
```

**Patient Search:**
```javascript
// Saves first patient ID
pm.environment.set("patient_id", jsonData.patients[0].id);
```

**Quality Measures:**
```javascript
// Saves first measure ID
pm.environment.set("measure_id", jsonData.measures[0].id);
```

**Evaluations:**
```javascript
// Saves evaluation ID
pm.environment.set("evaluation_id", jsonData.evaluationId);
```

---

## Authentication Flow

### Login Workflow

```
1. Run "Login" request
   ↓
2. JWT token auto-saved to environment
   ↓
3. All subsequent requests use {{jwt_token}}
   ↓
4. Token expires after 60 minutes
   ↓
5. Run "Refresh Token" to get new token
```

### Manual Token Refresh

If you see `401 Unauthorized` errors:

1. **Open:** "01 Authentication > Refresh Token"
2. **Send Request**
3. **Result:** New JWT token is saved, continue testing

### Re-authentication

If refresh token has expired (7 days):

1. **Open:** "01 Authentication > Login"
2. **Send Request**
3. **Result:** Fresh tokens saved

---

## Test Credentials

### Development Environment

| Username | Password | Role | Tenant ID |
|----------|----------|------|-----------|
| `test_evaluator` | `password123` | EVALUATOR | TENANT-001 |
| `test_admin` | `password123` | ADMIN | TENANT-001 |
| `test_analyst` | `password123` | ANALYST | TENANT-001 |
| `test_viewer` | `password123` | VIEWER | TENANT-001 |

### Role Permissions

| Role | Permissions |
|------|-------------|
| **EVALUATOR** | Run evaluations, view results, create assignments |
| **ADMIN** | All EVALUATOR permissions + manage users, override results |
| **ANALYST** | View reports, download QRDA exports |
| **VIEWER** | Read-only access to dashboards |

---

## Customizing for Production

### 1. Duplicate Environment

1. Right-click "HDIM - Development"
2. Select "Duplicate"
3. Rename to "HDIM - Production"

### 2. Update URLs

Change all URLs to production:

```
base_url: https://api.hdim.example.com
gateway_url: https://api.hdim.example.com
quality_measure_url: https://api.hdim.example.com/quality-measure
patient_url: https://api.hdim.example.com/patient
...
```

### 3. Update Credentials

Use production credentials:

```
username: your_production_username
password: your_production_password
tenant_id: your_tenant_id
```

### 4. Select Production Environment

Switch environment dropdown to "HDIM - Production"

---

## Tips & Best Practices

### 1. Use Environment Variables

Always use variables instead of hardcoding values:

**Good:**
```
GET {{patient_url}}/api/v1/patients/{{patient_id}}
```

**Bad:**
```
GET http://localhost:8084/patient/api/v1/patients/PAT-12345
```

### 2. Save Response Data

Use test scripts to save IDs for subsequent requests:

```javascript
pm.test("Save patient ID", function() {
    const jsonData = pm.response.json();
    pm.environment.set("patient_id", jsonData.id);
});
```

### 3. Check Response Status

Verify responses are successful:

```javascript
pm.test("Status code is 200", function() {
    pm.response.to.have.status(200);
});
```

### 4. Validate Response Schema

Ensure response format is correct:

```javascript
pm.test("Response has required fields", function() {
    pm.expect(jsonData).to.have.property("patientId");
    pm.expect(jsonData).to.have.property("measureId");
});
```

### 5. Use Collection Runner

Run entire collections in sequence:

1. Click "..." next to collection name
2. Select "Run collection"
3. Select environment
4. Click "Run HDIM - [Collection Name]"

---

## Troubleshooting

### "401 Unauthorized" Error

**Problem:** JWT token expired or invalid

**Solution:**
1. Run "Authentication > Refresh Token"
2. If that fails, run "Authentication > Login"

### "403 Forbidden" Error

**Problem:** User lacks required role

**Solution:**
1. Verify user has correct role (EVALUATOR, ADMIN, etc.)
2. Contact administrator to assign appropriate role

### "404 Not Found" Error

**Problem:** Resource doesn't exist or wrong URL

**Solution:**
1. Verify resource ID is correct (check environment variables)
2. Ensure services are running: `docker compose ps`
3. Check service URLs in environment

### "Connection Refused" Error

**Problem:** Services are not running

**Solution:**
```bash
# Start all services
docker compose up -d

# Check service status
docker compose ps

# View logs
docker compose logs -f quality-measure-service
```

### Missing Environment Variables

**Problem:** `{{patient_id}}` or other variables are empty

**Solution:**
1. Run prerequisite requests first (e.g., "Search Patients" before "Get Patient by ID")
2. Check test scripts saved data correctly (view Console tab)

---

## Advanced Usage

### Creating Custom Requests

1. **Duplicate Existing Request**
   - Right-click request
   - Select "Duplicate"
   - Modify URL, body, headers as needed

2. **Add to Collection**
   - Click "..." next to collection
   - Select "Add Request"
   - Configure request details

### Exporting Collections

**Share with team:**

1. Right-click collection
2. Select "Export"
3. Choose "Collection v2.1 (recommended)"
4. Share JSON file

### Importing OpenAPI Specs

**Generate collections from OpenAPI:**

1. Click "Import"
2. Select `docs/api/openapi-*.json` files
3. Postman auto-generates requests

---

## Related Documentation

- **[API Getting Started Guide](../api/GETTING_STARTED.md)** - Comprehensive API overview
- **[OpenAPI Specifications](../api/README.md)** - Complete API reference
- **[Service Catalog](../services/SERVICE_CATALOG.md)** - All 50+ services
- **[CLAUDE.md](../../CLAUDE.md)** - HDIM development quick reference

---

## Support

**Issues:**
- Check [Troubleshooting](#troubleshooting) section
- View service logs: `docker compose logs -f [SERVICE_NAME]`
- Check [Monitoring Dashboard](http://localhost:3000) (Grafana)

**Contact:**
- Email: support@hdim-platform.com
- Slack: #hdim-api-support

---

**Last Updated:** January 24, 2026
**Document Version:** 1.0
**Status:** ✅ Production Ready
