# HDIM Demo Quick Start Guide

## Prerequisites

- Docker Desktop (or Docker Engine + Docker Compose)
- 8GB RAM minimum (16GB recommended)
- 10GB disk space
- Java 21 (for building - pre-built JARs included)

## Quick Start (5 minutes)

### 1. Start Infrastructure Only

```bash
# Start just the databases and message broker
docker compose up -d postgres redis zookeeper kafka

# Wait for services to be healthy (about 30 seconds)
docker compose ps
```

### 2. Build and Start Backend Services

```bash
# Build all service JARs (if not already built)
cd backend
export JAVA_HOME=/path/to/jdk-21
./gradlew bootJar -x test

# Return to project root and start all services
cd ..
docker compose up -d --build
```

### 3. Verify Services Are Running

```bash
# Check all services are healthy
docker compose ps

# Expected output: all services should show "healthy" or "running"
```

### 4. Access the Application

| Service | URL | Description |
|---------|-----|-------------|
| Clinical Portal | http://localhost:4200 | Main user interface |
| Gateway API | http://localhost:8080 | API gateway |
| FHIR Service | http://localhost:8081 | FHIR R4 API |
| Quality Measure | http://localhost:8087 | Quality measures |
| CQL Engine | http://localhost:8086 | Measure evaluation |

## Demo Walkthrough

### Step 1: Create a Test Patient

```bash
curl -X POST http://localhost:8081/fhir/Patient \
  -H "Content-Type: application/fhir+json" \
  -H "X-Tenant-ID: demo-tenant" \
  -d '{
    "resourceType": "Patient",
    "id": "demo-patient-1",
    "name": [{"family": "Garcia", "given": ["Maria"]}],
    "gender": "female",
    "birthDate": "1965-03-15"
  }'
```

### Step 2: Add Clinical Data

```bash
# Add a condition (Diabetes)
curl -X POST http://localhost:8081/fhir/Condition \
  -H "Content-Type: application/fhir+json" \
  -H "X-Tenant-ID: demo-tenant" \
  -d '{
    "resourceType": "Condition",
    "subject": {"reference": "Patient/demo-patient-1"},
    "code": {"coding": [{"system": "http://snomed.info/sct", "code": "44054006", "display": "Diabetes mellitus type 2"}]},
    "clinicalStatus": {"coding": [{"code": "active"}]}
  }'

# Add an old A1C observation (creates care gap)
curl -X POST http://localhost:8081/fhir/Observation \
  -H "Content-Type: application/fhir+json" \
  -H "X-Tenant-ID: demo-tenant" \
  -d '{
    "resourceType": "Observation",
    "status": "final",
    "subject": {"reference": "Patient/demo-patient-1"},
    "code": {"coding": [{"system": "http://loinc.org", "code": "4548-4", "display": "Hemoglobin A1c"}]},
    "effectiveDateTime": "2024-01-15",
    "valueQuantity": {"value": 7.2, "unit": "%"}
  }'
```

### Step 3: Evaluate Quality Measures

```bash
# Calculate CDC (Comprehensive Diabetes Care) measure
curl -X POST http://localhost:8086/api/cql/evaluations \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: demo-tenant" \
  -d '{
    "patientId": "demo-patient-1",
    "measureId": "HEDIS-CDC",
    "measurementPeriod": {
      "start": "2025-01-01",
      "end": "2025-12-31"
    }
  }'
```

### Step 4: View Care Gaps

```bash
# Get care gaps for the patient
curl http://localhost:8083/api/care-gaps/demo-patient-1 \
  -H "X-Tenant-ID: demo-tenant"
```

## Sample Data Load

For a full demo with multiple patients:

```bash
# Load sample patient population (if available)
./scripts/load-demo-data.sh
```

## Troubleshooting

### Services Not Starting

```bash
# Check logs for a specific service
docker compose logs fhir-service

# Restart a specific service
docker compose restart quality-measure-service
```

### Database Connection Issues

```bash
# Verify PostgreSQL is running
docker compose exec postgres pg_isready -U healthdata

# Check databases were created
docker compose exec postgres psql -U healthdata -c "\l"
```

### Port Conflicts

If ports are already in use, edit `docker-compose.yml`:

```yaml
ports:
  - "8081:8081"  # Change left side to available port
```

## Stopping the Demo

```bash
# Stop all services but keep data
docker compose stop

# Stop and remove all containers (keeps data volumes)
docker compose down

# Stop and remove everything including data
docker compose down -v
```

## Performance Notes

- First measure calculation may take 2-3 seconds (cold start)
- Subsequent calculations: <200ms
- Batch processing: 10-30ms per patient
- Memory usage: ~4GB for full stack

## Next Steps

1. Explore the Clinical Portal UI at http://localhost:4200
2. Review API documentation at http://localhost:8080/swagger-ui.html
3. Load your own FHIR data for testing
4. Contact us for a production deployment discussion
