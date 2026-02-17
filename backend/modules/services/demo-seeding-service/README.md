# Demo Seeding Service

A service for generating synthetic patient data and managing demo scenarios for video recordings and live demonstrations.

## Quick Start

### Prerequisites
- PostgreSQL running on port 5435 with database `healthdata_demo`
- Docker network `hdim-master_healthdata-network`

### Running the Service

**Option 1: Docker (Recommended)**
```bash
# Build the JAR
cd backend
./gradlew :modules:services:demo-seeding-service:bootJar

# Run in Docker with PostgreSQL access
docker run --rm -d \
  --name demo-seeding-service \
  --network hdim-master_healthdata-network \
  -p 8098:8098 \
  -e SPRING_DATASOURCE_URL='jdbc:postgresql://healthdata-postgres:5432/healthdata_demo?sslmode=disable' \
  -e SPRING_DATASOURCE_USERNAME=healthdata \
  -e SPRING_DATASOURCE_PASSWORD=healthdata_password \
  -v $(pwd)/modules/services/demo-seeding-service/build/libs/demo-seeding-service-1.0.0.jar:/app.jar \
  eclipse-temurin:21-jre \
  java -jar /app.jar
```

**Option 2: Local Development**
```bash
# Requires PostgreSQL accessible on localhost:5435
./gradlew :modules:services:demo-seeding-service:bootRun
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/demo/api/v1/demo/status` | Get demo environment status |
| GET | `/demo/api/v1/demo/scenarios` | List available scenarios |
| POST | `/demo/api/v1/demo/scenarios/{name}` | Load a scenario |
| POST | `/demo/api/v1/demo/webhooks/test` | Webhook testing endpoint (echo payload + metadata) |
| GET | `/demo/actuator/health` | Health check |

### Load a Scenario

```bash
curl -X POST http://localhost:8098/demo/api/v1/demo/scenarios/hedis-evaluation
```

Response:
```json
{
  "scenarioName": "hedis-evaluation",
  "sessionId": "uuid",
  "patientCount": 9033,
  "careGapCount": 1400,
  "loadTimeMs": 8209,
  "success": true
}
```

### Webhook Testing (Sandbox)

```bash
curl -X POST http://localhost:8098/demo/api/v1/demo/webhooks/test \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: sandbox-tenant" \
  -H "X-Webhook-Event: integration.test" \
  -d '{"source":"postman","status":"ok"}'
```

## Developer Portal Sandbox Workflow

The platform already includes the core pieces required for a developer sandbox lane:

1. Generate sandbox API key (Gateway API key management):
```bash
curl -X POST http://localhost:8080/api/v1/api-keys \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Sandbox Key",
    "description": "Developer portal sandbox access",
    "tenantId": "sandbox-tenant",
    "scopes": ["fhir:read","fhir:write"],
    "rateLimitPerMinute": 100
  }'
```
2. Seed synthetic data (non-PHI demo patients):
```bash
NON_INTERACTIVE=1 SEED_PROFILE=smoke ./scripts/seed-all-demo-data.sh
```
3. Reset data between validation runs:
```bash
curl -X POST http://localhost:8098/demo/api/v1/demo/reset/current-tenant
```
4. Validate webhook integrations:
```bash
curl -X POST http://localhost:8098/demo/api/v1/demo/webhooks/test -d '{}'
```

## Available Scenarios

| Scenario | Patients | Description |
|----------|----------|-------------|
| `hedis-evaluation` | 5,000 | HEDIS quality measure evaluation with care gaps |
| `patient-journey` | 1,000 | 360-degree patient view with SDOH factors |
| `risk-stratification` | 10,000 | Population risk stratification and HCC scoring |
| `multi-tenant` | 25,000 | Multi-tenant SaaS demonstration (3 tenants) |

## Configuration

Key configuration in `application.yml`:

```yaml
server:
  port: 8098
  servlet:
    context-path: /demo

spring:
  datasource:
    url: jdbc:postgresql://localhost:5435/healthdata_demo?sslmode=disable

demo:
  generation:
    default-care-gap-percentage: 28
    default-patient-count: 5000
```

## Database Setup

The service uses Liquibase for schema management. Tables are created automatically:
- `demo_scenarios` - Pre-configured scenario definitions
- `demo_sessions` - Active demo session tracking
- `demo_snapshots` - Database snapshots for quick reset
- `synthetic_patient_templates` - Patient persona templates

## Troubleshooting

### JSONB Type Errors
Ensure all JSONB columns in entities have the annotation:
```java
@JdbcTypeCode(SqlTypes.JSON)
@Column(name = "field_name", columnDefinition = "JSONB")
private String fieldName;
```

### Table Does Not Exist Errors
The service gracefully handles missing FHIR resource tables (patients, conditions, etc.) that exist in other services but not in the demo database. This is expected behavior.

### Connection Issues
- Verify PostgreSQL is running: `docker ps | grep postgres`
- Check network connectivity: `docker network inspect hdim-master_healthdata-network`
- Ensure database exists: `docker exec healthdata-postgres psql -U healthdata -c '\l'`
