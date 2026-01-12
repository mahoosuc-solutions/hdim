# OpenAPI Specifications - HDIM Platform v1.2.0

## Overview

This directory contains OpenAPI 3.0.1 specifications for HDIM platform services.

## Generated Specifications

### quality-measure-service
- **File:** `openapi-quality-measure-v1.2.0.json`
- **Generated:** January 11, 2026
- **OpenAPI Version:** 3.0.1
- **Service Version:** 1.0.7
- **Total Endpoints:** 84

**Contents:**
- ✅ Quality measure evaluation endpoints
- ✅ Population calculation endpoints
- ✅ Risk assessment endpoints
- ✅ Care gap detection endpoints
- ✅ Clinical decision support endpoints
- ✅ Mental health assessment endpoints
- ⚠️  **NOT YET INCLUDED:** New v1.2.0 endpoints (require service rebuild)
  - Measure Assignment API (5 endpoints)
  - Measure Override API (8 endpoints)

**Why v1.2.0 endpoints missing:**
The running service was built before the new controllers were added. To include new endpoints:
1. Rebuild service: `./gradlew :modules:services:quality-measure-service:build`
2. Restart service: `docker compose up -d --build quality-measure-service`
3. Regenerate spec: `curl http://localhost:8087/quality-measure/v3/api-docs > docs/api/openapi-quality-measure-v1.2.0-complete.json`

## Viewing Specifications

### Option 1: Swagger UI (Online)
1. Go to https://editor.swagger.io
2. File → Import File
3. Select `openapi-quality-measure-v1.2.0.json`

### Option 2: Local Swagger UI
```bash
# Install swagger-ui (if not installed)
npm install -g swagger-ui-watcher

# View spec
swagger-ui-watcher docs/api/openapi-quality-measure-v1.2.0.json
# Opens browser at http://localhost:8080
```

### Option 3: VS Code Extension
1. Install "OpenAPI (Swagger) Editor" extension
2. Open `openapi-quality-measure-v1.2.0.json`
3. Right-click → "Preview Swagger"

## Generating Specifications

### Manual Generation (Current Running Services)
```bash
# quality-measure-service
curl http://localhost:8087/quality-measure/v3/api-docs > docs/api/openapi-quality-measure-v1.2.0.json

# Other services
curl http://localhost:8085/fhir/v3/api-docs > docs/api/openapi-fhir-v1.2.0.json
curl http://localhost:8084/patient/v3/api-docs > docs/api/openapi-patient-v1.2.0.json
curl http://localhost:8080/gateway/v3/api-docs > docs/api/openapi-gateway-v1.2.0.json
curl http://localhost:8081/cql-engine/v3/api-docs > docs/api/openapi-cql-engine-v1.2.0.json
```

### Automated Generation Script
```bash
# Use provided script (requires all services running)
./docs/api/generate-openapi-specs.sh

# Or with pretty-printing
./docs/api/generate-openapi-specs.sh --pretty
```

## Using Specifications

### Client Code Generation

**TypeScript/JavaScript:**
```bash
npx @openapitools/openapi-generator-cli generate \
  -i docs/api/openapi-quality-measure-v1.2.0.json \
  -g typescript-axios \
  -o client/typescript/quality-measure

# Install generated client
cd client/typescript/quality-measure
npm install
```

**Java:**
```bash
openapi-generator-cli generate \
  -i docs/api/openapi-quality-measure-v1.2.0.json \
  -g java \
  --library okhttp-gson \
  -o client/java/quality-measure
```

**Python:**
```bash
openapi-generator-cli generate \
  -i docs/api/openapi-quality-measure-v1.2.0.json \
  -g python \
  -o client/python/quality-measure
```

### API Contract Testing
```bash
# Install Dredd API testing tool
npm install -g dredd

# Run contract tests
dredd docs/api/openapi-quality-measure-v1.2.0.json http://localhost:8087
```

### Postman Collection Import
1. Open Postman
2. File → Import
3. Select `openapi-quality-measure-v1.2.0.json`
4. All endpoints will be imported as a collection

## Known Issues

### HDIM-1205: Missing v1.2.0 Endpoints
**Status:** Partial - Baseline spec generated, new endpoints require service rebuild

**Current State:**
- ✅ Baseline endpoints documented (84 endpoints)
- ⚠️  New v1.2.0 endpoints not yet in spec (13 endpoints)

**Resolution:** v1.2.1 (February 2026)
- Will rebuild all services
- Regenerate complete specifications
- Include all new v1.2.0 endpoints

### SpringDoc Configuration
**Note:** Services have SpringDoc dependency but configuration is not yet enabled in `application.yml`.

To enable:
```yaml
# Add to application.yml
springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
  packages-to-scan: com.healthdata
```

## Specification Validation

### Validate OpenAPI Spec
```bash
# Install OpenAPI CLI
npm install -g @apidevtools/swagger-cli

# Validate spec
swagger-cli validate docs/api/openapi-quality-measure-v1.2.0.json
# Returns: openapi-quality-measure-v1.2.0.json is valid
```

### Check for Breaking Changes
```bash
# Install openapi-diff
npm install -g openapi-diff

# Compare with previous version
openapi-diff docs/api/openapi-quality-measure-v1.1.0.json \
             docs/api/openapi-quality-measure-v1.2.0.json
```

## Contributing

When adding new endpoints:
1. Ensure controller has `@Operation` annotation
2. Add `@Schema` annotations to DTOs
3. Rebuild service
4. Regenerate OpenAPI spec
5. Validate spec
6. Commit to docs/api/

Example controller annotation:
```java
@Operation(
    summary = "Create measure assignment",
    description = "Manually assign a quality measure to a patient",
    tags = {"Measure Assignment"}
)
@ApiResponses({
    @ApiResponse(responseCode = "201", description = "Assignment created"),
    @ApiResponse(responseCode = "409", description = "Duplicate assignment")
})
@PostMapping("/patients/{patientId}/measure-assignments")
public ResponseEntity<AssignmentResponse> createAssignment(...) {
    // ...
}
```

## Related Documentation
- [API Design Guide](../../BACKEND_API_SPECIFICATION.md)
- [Release Notes](../../RELEASE_NOTES_v1.2.0.md)
- [Known Issues](../../KNOWN_ISSUES_v1.2.0.md)
- [System Architecture](../architecture/SYSTEM_ARCHITECTURE.md)

---

**Last Updated:** January 11, 2026
**Document Version:** 1.0
