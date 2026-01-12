# HDIM Platform API Documentation

This directory contains OpenAPI 3.0 specifications for HDIM Platform microservices.

## Available Specifications (v1.2.0)

| Service | Specification | Description |
|---------|---------------|-------------|
| gateway-service | `openapi-gateway-service-v1.2.0.json` | API Gateway and authentication |
| cql-engine-service | `openapi-cql-engine-service-v1.2.0.json` | CQL evaluation engine for HEDIS measures |
| fhir-service | `openapi-fhir-service-v1.2.0.json` | FHIR R4 resource management |
| patient-service | `openapi-patient-service-v1.2.0.json` | Patient demographics and PHI |
| quality-measure-service | `openapi-quality-measure-service-v1.2.0.json` | Quality measure evaluation and reporting |
| care-gap-service | `openapi-care-gap-service-v1.2.0.json` | Care gap detection and management |

## Generating OpenAPI Specifications

### Prerequisites

1. **Services Running**: All target services must be running via Docker Compose
2. **jq Installed** (optional, for JSON formatting):
   ```bash
   # Ubuntu/Debian
   sudo apt install jq

   # macOS
   brew install jq
   ```

### Generate All Specifications

```bash
# From project root
cd docs/api
./generate-openapi-specs.sh
```

### Generate Single Service Specification

```bash
# Generate spec for quality-measure-service only
./generate-openapi-specs.sh --service quality-measure-service
```

### Example Output

```
======================================
HDIM Platform OpenAPI Generator
Version: v1.2.0
======================================

✓ jq detected - will format JSON output

Mode: Generating OpenAPI specs for all 6 core services
Output Directory: /mnt/wd-black/dev/projects/hdim-master/docs/api

Processing: quality-measure-service
  Port: 8087
  Context Path: /quality-measure
  → Fetching: http://localhost:8087/quality-measure/v3/api-docs
  ✓ SUCCESS - JSON saved: openapi-quality-measure-service-v1.2.0.json
  → Size: 45K, Lines: 1234

======================================
Generation Summary
======================================
✓ Successful: 6
✗ Failed: 0

Generated OpenAPI specifications:
  openapi-gateway-service-v1.2.0.json (32K)
  openapi-cql-engine-service-v1.2.0.json (28K)
  openapi-fhir-service-v1.2.0.json (67K)
  openapi-patient-service-v1.2.0.json (38K)
  openapi-quality-measure-service-v1.2.0.json (45K)
  openapi-care-gap-service-v1.2.0.json (41K)
```

## Using OpenAPI Specifications

### 1. API Testing with Postman

Import OpenAPI specs into Postman:

1. Open Postman
2. Click "Import" button
3. Select "File" tab
4. Upload `openapi-<service>-v1.2.0.json`
5. Postman will create a collection with all endpoints

**Benefits:**
- Pre-configured request examples
- Automatic request validation
- Response schema validation
- Environment variable support

### 2. API Testing with Insomnia

Import OpenAPI specs into Insomnia:

1. Open Insomnia
2. Click "Create" → "Import From" → "File"
3. Select `openapi-<service>-v1.2.0.json`
4. Insomnia will create a request collection

### 3. Generate API Client Libraries

Use OpenAPI Generator to create client libraries:

```bash
# Install OpenAPI Generator
npm install -g @openapitools/openapi-generator-cli

# Generate TypeScript client
openapi-generator-cli generate \
  -i openapi-quality-measure-service-v1.2.0.json \
  -g typescript-axios \
  -o ../clients/typescript/quality-measure-client

# Generate Java client
openapi-generator-cli generate \
  -i openapi-quality-measure-service-v1.2.0.json \
  -g java \
  -o ../clients/java/quality-measure-client \
  --additional-properties=library=resttemplate

# Generate Python client
openapi-generator-cli generate \
  -i openapi-quality-measure-service-v1.2.0.json \
  -g python \
  -o ../clients/python/quality-measure-client
```

**Supported Languages:**
- TypeScript (axios, angular, fetch)
- Java (RestTemplate, WebClient, OkHttp)
- Python (urllib3, requests)
- C# (.NET Core)
- Go
- Ruby
- PHP
- Kotlin
- Swift

### 4. API Documentation with Swagger UI

View interactive API documentation:

```bash
# Start services
docker compose up -d quality-measure-service

# Access Swagger UI
open http://localhost:8087/quality-measure/swagger-ui.html
```

**Swagger UI Features:**
- Interactive API exploration
- Try-it-out functionality
- Request/response examples
- Schema definitions
- Authentication testing

### 5. API Contract Testing

Use OpenAPI specs for contract testing:

```javascript
// Example: Pact contract testing with OpenAPI
const { PactV3, MatchersV3 } = require('@pact-foundation/pact');
const openapi = require('./openapi-quality-measure-service-v1.2.0.json');

describe('Quality Measure Service Contract', () => {
  it('should match OpenAPI specification', async () => {
    // Validate responses against OpenAPI schema
    const response = await fetch('http://localhost:8087/quality-measure/api/v1/measures');
    const schema = openapi.paths['/api/v1/measures'].get.responses['200'].content['application/json'].schema;

    // Assert response matches schema
    expect(validateSchema(response, schema)).toBe(true);
  });
});
```

## OpenAPI Specification Structure

### Example: quality-measure-service Endpoints

**Measure Assignment API** (New in v1.2.0):
- `GET /quality-measure/patients/{patientId}/measure-assignments`
- `POST /quality-measure/patients/{patientId}/measure-assignments`
- `DELETE /quality-measure/measure-assignments/{assignmentId}`
- `PUT /quality-measure/measure-assignments/{assignmentId}/dates`

**Measure Override API** (New in v1.2.0):
- `GET /quality-measure/patients/{patientId}/measure-overrides`
- `POST /quality-measure/patients/{patientId}/measure-overrides`
- `POST /quality-measure/measure-overrides/{overrideId}/approve`
- `POST /quality-measure/measure-overrides/{overrideId}/review`
- `DELETE /quality-measure/measure-overrides/{overrideId}`
- `GET /quality-measure/measure-overrides/pending-approval`
- `GET /quality-measure/measure-overrides/due-for-review`
- `POST /quality-measure/patients/{patientId}/measures/{measureId}/resolve-overrides`

**Measure Evaluation API** (Existing):
- `POST /quality-measure/evaluations`
- `GET /quality-measure/evaluations/{evaluationId}`
- `GET /quality-measure/patients/{patientId}/evaluations`

### Authentication

All endpoints require JWT authentication:

```bash
# Obtain JWT token
curl -X POST http://localhost:8001/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test_evaluator",
    "password": "password123"
  }'

# Use token in subsequent requests
curl -X GET http://localhost:8087/quality-measure/patients/{patientId}/measure-assignments \
  -H "Authorization: Bearer <jwt-token>" \
  -H "X-Tenant-ID: TENANT-001"
```

### Required Headers

| Header | Description | Required |
|--------|-------------|----------|
| `Authorization` | Bearer JWT token | Yes |
| `X-Tenant-ID` | Tenant identifier for multi-tenancy | Yes |
| `Content-Type` | Request body type (usually `application/json`) | For POST/PUT |
| `Accept` | Response type (usually `application/json`) | Optional |

## Validation and Linting

### Validate OpenAPI Specifications

```bash
# Install OpenAPI Validator
npm install -g @ibm/openapi-validator

# Validate specification
lint-openapi openapi-quality-measure-service-v1.2.0.json

# Expected output:
# ✓ No errors or warnings found
```

### Common Validation Issues

**1. Missing Required Fields**
```json
{
  "error": "Missing required field 'description' in operation GET /api/v1/measures",
  "fix": "Add description to operation in @Operation annotation"
}
```

**2. Inconsistent Response Schemas**
```json
{
  "error": "Response schema mismatch for 200 status code",
  "fix": "Ensure @ApiResponse schema matches actual DTO class"
}
```

## Continuous Integration

### Auto-Generate on Build

Add to `build.gradle.kts`:

```kotlin
plugins {
    id("org.springdoc.openapi-gradle-plugin") version "1.8.0"
}

openApi {
    apiDocsUrl.set("http://localhost:8087/quality-measure/v3/api-docs")
    outputFileName.set("openapi-quality-measure-service.json")
    outputDir.set(file("$rootDir/docs/api"))
}
```

### GitHub Actions Workflow

```yaml
name: Generate OpenAPI Specs

on:
  push:
    branches: [main, develop]

jobs:
  generate-specs:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Start services
        run: docker compose up -d

      - name: Wait for services
        run: sleep 30

      - name: Generate OpenAPI specs
        run: |
          cd docs/api
          ./generate-openapi-specs.sh

      - name: Commit specs
        run: |
          git config --local user.email "ci@hdim-platform.com"
          git config --local user.name "CI Bot"
          git add docs/api/*.json
          git commit -m "chore: Update OpenAPI specifications [skip ci]" || true
          git push
```

## Troubleshooting

### Service Not Responding

**Problem:** `curl: (7) Failed to connect to localhost port 8087`

**Solution:**
```bash
# Check if service is running
docker compose ps quality-measure-service

# If not running, start it
docker compose up -d quality-measure-service

# Wait for service to be healthy
docker compose ps
# Should show "healthy" status
```

### OpenAPI Endpoint Not Found

**Problem:** `curl: (22) The requested URL returned error: 404`

**Solution:**
```bash
# Verify SpringDoc dependency in build.gradle.kts
grep -r "springdoc" backend/modules/services/quality-measure-service/build.gradle.kts

# Should show:
# implementation(libs.springdoc.openapi.starter.webmvc.ui)

# If missing, add to dependencies:
implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
```

### Invalid JSON Output

**Problem:** JSON is malformed or truncated

**Solution:**
```bash
# Install jq for validation
sudo apt install jq

# Validate JSON
jq '.' openapi-quality-measure-service-v1.2.0.json

# If error, regenerate spec
./generate-openapi-specs.sh --service quality-measure-service
```

## Version History

| Version | Date | Changes |
|---------|------|---------|
| v1.2.0 | 2026-01-25 | Added measure assignment and override endpoints, OTLP tracing metadata |
| v1.1.0 | 2025-12-14 | Added care gap detection endpoints |
| v1.0.0 | 2025-11-01 | Initial OpenAPI specification release |

## Contributing

When adding new API endpoints:

1. **Add SpringDoc Annotations** in controller:
   ```java
   @Operation(summary = "Get measure assignments", description = "Retrieves all measure assignments for a patient")
   @ApiResponses(value = {
       @ApiResponse(responseCode = "200", description = "Assignments retrieved successfully"),
       @ApiResponse(responseCode = "404", description = "Patient not found"),
       @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
   })
   @GetMapping("/patients/{patientId}/measure-assignments")
   public ResponseEntity<List<MeasureAssignmentDTO>> getAssignments(...) { }
   ```

2. **Regenerate Specification**:
   ```bash
   docker compose up -d quality-measure-service
   cd docs/api
   ./generate-openapi-specs.sh --service quality-measure-service
   ```

3. **Validate Specification**:
   ```bash
   lint-openapi openapi-quality-measure-service-v1.2.0.json
   ```

4. **Commit to Repository**:
   ```bash
   git add docs/api/openapi-quality-measure-service-v1.2.0.json
   git commit -m "docs: Update OpenAPI spec for quality-measure-service"
   ```

## Resources

- [OpenAPI Specification 3.0](https://swagger.io/specification/)
- [SpringDoc OpenAPI Documentation](https://springdoc.org/)
- [OpenAPI Generator](https://openapi-generator.tech/)
- [Swagger UI](https://swagger.io/tools/swagger-ui/)
- [Postman Documentation](https://learning.postman.com/docs/getting-started/importing-and-exporting-data/)

---

**Last Updated:** January 11, 2026
**Version:** 1.0 (for v1.2.0 release)
**Status:** OpenAPI generation script ready, specifications to be generated on service startup
