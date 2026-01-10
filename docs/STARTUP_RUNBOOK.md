# HDIM Platform Startup Runbook

**Version:** 1.0
**Last Updated:** January 10, 2026
**Applies To:** HDIM Core Services (FHIR, CQL Engine, Quality Measure)

---

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Quick Start (Development)](#quick-start-development)
4. [Production Startup Sequence](#production-startup-sequence)
5. [Initialization & Seeding](#initialization--seeding)
6. [Health Checks & Verification](#health-checks--verification)
7. [Troubleshooting](#troubleshooting)
8. [Configuration Reference](#configuration-reference)

---

## Overview

This runbook provides step-by-step procedures for starting the HDIM platform services in the correct order to ensure all dependencies are met and the system functions end-to-end.

### Service Dependency Chain

```
┌─────────────────────────────────────────────────────────────┐
│  Infrastructure Layer                                       │
│  PostgreSQL → Redis → Kafka                                 │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│  Core Services Layer                                        │
│  FHIR Service → CQL Engine Service → Quality Measure Service│
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│  Support Services Layer                                     │
│  Demo Seeding Service, Analytics Service, etc.             │
└─────────────────────────────────────────────────────────────┘
```

### Critical Initialization Requirements

| Component | Auto-Initialized | Manual Action Required |
|-----------|------------------|------------------------|
| **Database Schema** | ✅ Liquibase migrations run automatically | None |
| **Value Sets** | ✅ Migration 0014 seeds essential codes | None (as of v1.1) |
| **HEDIS Measures** | ✅ Auto-seeded on QM Service startup | None (as of v1.1) |
| **Patient Data** | ❌ Not auto-seeded | Must seed via API or demo-seeding-service |

---

## Prerequisites

### System Requirements

- **Operating System:** Linux, macOS, or Windows (WSL2)
- **Java:** OpenJDK 21 (LTS)
- **Gradle:** 8.11+ (wrapper provided)
- **Docker:** 24.0+ with Docker Compose
- **Memory:** Minimum 8 GB RAM (16 GB recommended)
- **Disk:** 20 GB free space

### Verify Prerequisites

```bash
# Check Java version
java -version
# Expected: openjdk version "21.x.x"

# Check Gradle version
./gradlew --version
# Expected: Gradle 8.11+

# Check Docker version
docker --version
# Expected: Docker version 24.0+

docker compose version
# Expected: Docker Compose version 2.x
```

---

## Quick Start (Development)

### Automated Test Script

The fastest way to validate the complete workflow:

```bash
# Run end-to-end test (starts all services, seeds data, runs tests)
./scripts/test-end-to-end-workflow.sh

# Configuration via environment variables
TENANT_ID=acme-health \
PATIENT_COUNT=50 \
CARE_GAP_PERCENTAGE=30 \
CLEANUP=true \
./scripts/test-end-to-end-workflow.sh
```

**What the script does:**
1. Starts PostgreSQL, Redis, Kafka
2. Starts FHIR, CQL Engine, Quality Measure services
3. Waits for health checks
4. Seeds demo patient data
5. Runs HEDIS measure evaluations
6. Validates results
7. Cleans up (optional)

### Manual Development Startup

For iterative development with hot reloading:

```bash
# Terminal 1: Start infrastructure
docker compose up -d postgres redis kafka

# Terminal 2: Start FHIR Service
cd backend
./gradlew :modules:services:fhir-service:bootRun

# Terminal 3: Start CQL Engine Service
cd backend
./gradlew :modules:services:cql-engine-service:bootRun

# Terminal 4: Start Quality Measure Service
cd backend
./gradlew :modules:services:quality-measure-service:bootRun

# Terminal 5: (Optional) Start Demo Seeding Service
cd backend
./gradlew :modules:services:demo-seeding-service:bootRun
```

---

## Production Startup Sequence

### Phase 1: Infrastructure Services

**Objective:** Start PostgreSQL, Redis, and Kafka with production configurations.

#### Step 1.1: Start PostgreSQL

```bash
docker compose -f docker-compose.production.yml up -d postgres

# Wait for PostgreSQL to be ready
docker compose exec postgres pg_isready -U healthdata
```

**Verification:**
```bash
# Check PostgreSQL logs
docker compose logs -f postgres

# Expected: "database system is ready to accept connections"
```

**Troubleshooting:**
- If PostgreSQL fails to start, check disk space: `df -h`
- Verify data directory permissions: `ls -la /var/lib/postgresql/data`
- Check Docker logs: `docker compose logs postgres`

#### Step 1.2: Start Redis

```bash
docker compose -f docker-compose.production.yml up -d redis

# Verify Redis is running
docker compose exec redis redis-cli ping
# Expected: PONG
```

**Verification:**
```bash
# Check Redis info
docker compose exec redis redis-cli INFO server

# Expected: redis_version:7.x
```

#### Step 1.3: Start Kafka

```bash
docker compose -f docker-compose.production.yml up -d kafka

# Wait for Kafka to be ready (30 seconds typical)
sleep 30

# Verify Kafka is running
docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

**Verification:**
```bash
# Check Kafka logs
docker compose logs kafka | grep "started (kafka.server.KafkaServer)"

# Expected: "Kafka Server started"
```

**Create Required Topics (if not auto-created):**
```bash
docker compose exec kafka kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic healthdata.fhir.resources \
  --partitions 12 \
  --replication-factor 1

docker compose exec kafka kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic healthdata.quality.measures \
  --partitions 8 \
  --replication-factor 1
```

---

### Phase 2: Core Application Services

**Objective:** Start services in dependency order, waiting for each to become healthy before proceeding.

#### Step 2.1: Start FHIR Service

```bash
# Option A: Docker (recommended for production)
docker compose -f docker-compose.production.yml up -d fhir-service

# Option B: Direct Java (development)
cd backend
./gradlew :modules:services:fhir-service:bootRun
```

**Wait for Health Check:**
```bash
# Wait for health endpoint (up to 90 seconds)
until curl -sf http://localhost:8085/actuator/health > /dev/null; do
  echo "Waiting for FHIR Service..."
  sleep 5
done

echo "FHIR Service is healthy"
```

**Verify Database Migrations:**
```bash
# Check Liquibase changelog table
docker compose exec postgres psql -U healthdata -d healthdata_fhir \
  -c "SELECT COUNT(*) FROM databasechangelog;"

# Expected: 17 migrations applied
```

**Verify FHIR Resources Accessible:**
```bash
curl -H "X-Tenant-ID: acme-health" \
     http://localhost:8085/api/v1/Patient?_count=1

# Expected: {"resourceType": "Bundle", "type": "searchset", ...}
```

#### Step 2.2: Start CQL Engine Service

```bash
# Option A: Docker (recommended for production)
docker compose -f docker-compose.production.yml up -d cql-engine-service

# Option B: Direct Java (development)
cd backend
./gradlew :modules:services:cql-engine-service:bootRun
```

**Wait for Health Check:**
```bash
# Wait for health endpoint (up to 90 seconds)
until curl -sf http://localhost:8081/actuator/health > /dev/null; do
  echo "Waiting for CQL Engine Service..."
  sleep 5
done

echo "CQL Engine Service is healthy"
```

**Verify HedisMeasureRegistry Initialized:**
```bash
# Check logs for registry initialization
docker compose logs cql-engine-service | grep "HedisMeasureRegistry"

# Expected: "Registered 56 HEDIS measures"
```

**Verify CQL Libraries Seeded:**
```bash
# Check database for CQL library count
docker compose exec postgres psql -U healthdata -d healthdata_cql \
  -c "SELECT COUNT(*) FROM cql_libraries WHERE status = 'ACTIVE';"

# Expected: 56 measures
```

**Verify Value Sets Seeded:**
```bash
# Check value sets table
docker compose exec postgres psql -U healthdata -d healthdata_cql \
  -c "SELECT COUNT(*) FROM value_sets WHERE tenant_id = 'acme-health';"

# Expected: 20+ value sets (from migration 0014)
```

**List Available Measures:**
```bash
curl -H "X-Tenant-ID: acme-health" \
     http://localhost:8081/api/v1/libraries

# Expected: JSON array with 56 measures (BCS, CBP, CDC, COL, CCS, etc.)
```

#### Step 2.3: Start Quality Measure Service

```bash
# Option A: Docker (recommended for production)
docker compose -f docker-compose.production.yml up -d quality-measure-service

# Option B: Direct Java (development)
cd backend
./gradlew :modules:services:quality-measure-service:bootRun
```

**Wait for Health Check:**
```bash
# Wait for health endpoint (up to 90 seconds)
until curl -sf http://localhost:8087/actuator/health > /dev/null; do
  echo "Waiting for Quality Measure Service..."
  sleep 5
done

echo "Quality Measure Service is healthy"
```

**Verify Auto-Seeding of Measure Definitions:**
```bash
# Check logs for auto-seeding
docker compose logs quality-measure-service | grep "Auto-seeding HEDIS"

# Expected: "HEDIS measure seeding complete: X new measures seeded, Y total measures"
```

**Verify Measure Definitions Available:**
```bash
curl -H "X-Tenant-ID: acme-health" \
     "http://localhost:8087/api/v1/measures?measureSet=HEDIS"

# Expected: JSON array with 7+ measures (BCS, COL, CBP, CDC, CCS, EED, SPC)
```

---

### Phase 3: Support Services

#### Step 3.1: Start Demo Seeding Service (Optional - Development Only)

```bash
docker compose up -d demo-seeding-service

# Wait for health check
until curl -sf http://localhost:8103/actuator/health > /dev/null; do
  echo "Waiting for Demo Seeding Service..."
  sleep 5
done
```

#### Step 3.2: Start Analytics Service (Optional)

```bash
docker compose -f docker-compose.production.yml up -d analytics-service
```

#### Step 3.3: Start Gateway Service (Production)

```bash
docker compose -f docker-compose.production.yml up -d gateway-service

# Verify gateway routing
curl http://localhost:8001/health
```

---

## Initialization & Seeding

### Automatic Initialization (No Action Required)

The following are **automatically initialized** on service startup:

1. **Database Schemas** - Liquibase migrations run automatically
2. **CQL Libraries** - Migration `0012-seed-hedis-measures.xml` seeds 56 measures
3. **Value Sets** - Migration `0014-seed-essential-value-sets.xml` seeds 20 code systems
4. **HEDIS Measure Definitions** - Quality Measure Service auto-seeds on startup

### Manual Data Seeding (Required for Testing)

Patient data **must be manually seeded** via one of these methods:

#### Method 1: Demo Seeding Service API

```bash
# Seed 100 synthetic patients with 30% care gaps
curl -X POST \
  -H "X-Tenant-ID: acme-health" \
  -H "Content-Type: application/json" \
  -d '{"count": 100, "careGapPercentage": 30}' \
  http://localhost:8103/api/v1/demo/seed
```

**Response:**
```json
{
  "status": "success",
  "patientsCreated": 100,
  "tenantId": "acme-health",
  "careGapsGenerated": 30
}
```

#### Method 2: FHIR Bundle Upload

```bash
# Upload FHIR Bundle with patient resources
curl -X POST \
  -H "X-Tenant-ID: acme-health" \
  -H "Content-Type: application/fhir+json" \
  -d @sample-patient-bundle.json \
  http://localhost:8085/api/v1/
```

#### Method 3: EHR Connector (Production)

For production, use the EHR connector service to sync real patient data:

```bash
# Configure EHR connector
curl -X POST \
  -H "X-Tenant-ID: acme-health" \
  -d '{"ehrSystem": "epic", "endpoint": "https://fhir.epic.com/interconnect-fhir-oauth"}' \
  http://localhost:8090/api/v1/ehr-connector/config

# Trigger sync
curl -X POST \
  -H "X-Tenant-ID: acme-health" \
  http://localhost:8090/api/v1/ehr-connector/sync
```

---

## Health Checks & Verification

### Service Health Endpoints

| Service | Health Endpoint | Port |
|---------|----------------|------|
| FHIR Service | http://localhost:8085/actuator/health | 8085 |
| CQL Engine | http://localhost:8081/actuator/health | 8081 |
| Quality Measure | http://localhost:8087/actuator/health | 8087 |
| Demo Seeding | http://localhost:8103/actuator/health | 8103 |
| Gateway | http://localhost:8001/health | 8001 |

### Comprehensive Health Check Script

```bash
#!/bin/bash
# Check all core services

services=(
  "FHIR:http://localhost:8085/actuator/health"
  "CQL:http://localhost:8081/actuator/health"
  "Quality:http://localhost:8087/actuator/health"
)

for service in "${services[@]}"; do
  name="${service%%:*}"
  url="${service#*:}"

  if curl -sf "$url" > /dev/null; then
    echo "✓ $name Service: HEALTHY"
  else
    echo "✗ $name Service: DOWN"
  fi
done
```

### End-to-End Workflow Validation

```bash
# 1. Verify infrastructure
docker compose ps

# 2. Verify services
curl http://localhost:8085/actuator/health  # FHIR
curl http://localhost:8081/actuator/health  # CQL Engine
curl http://localhost:8087/actuator/health  # Quality Measure

# 3. Verify measure definitions
curl -H "X-Tenant-ID: acme-health" \
     http://localhost:8087/api/v1/measures | jq '. | length'

# Expected: 7+ measures

# 4. Verify value sets
curl -H "X-Tenant-ID: acme-health" \
     http://localhost:8081/api/v1/valuesets | jq '. | length'

# Expected: 20+ value sets

# 5. Create test patient
patient_response=$(curl -sf -X POST \
  -H "X-Tenant-ID: acme-health" \
  -H "Content-Type: application/json" \
  -d '{
    "resourceType": "Patient",
    "gender": "female",
    "birthDate": "1970-01-01"
  }' \
  http://localhost:8085/api/v1/Patient)

patient_id=$(echo "$patient_response" | jq -r '.id')
echo "Created patient: $patient_id"

# 6. Evaluate measure
curl -X POST \
  -H "X-Tenant-ID: acme-health" \
  -H "Content-Type: application/json" \
  -d "{\"patientId\": \"$patient_id\", \"measureId\": \"BCS\"}" \
  http://localhost:8087/api/v1/evaluate | jq '.'

# Expected: Measure result with numerator/denominator compliance
```

---

## Troubleshooting

### Common Issues

#### Issue 1: PostgreSQL Connection Refused

**Symptoms:**
```
org.postgresql.util.PSQLException: Connection to localhost:5435 refused
```

**Solution:**
```bash
# Check if PostgreSQL is running
docker compose ps postgres

# Check PostgreSQL logs
docker compose logs postgres

# Restart PostgreSQL
docker compose restart postgres

# Wait for ready state
docker compose exec postgres pg_isready -U healthdata
```

#### Issue 2: CQL Engine Measure Registry Empty

**Symptoms:**
```
HedisMeasureRegistry initialized with 0 measures
```

**Root Cause:** Java measure classes not on classpath or not annotated with `@Component`

**Solution:**
```bash
# Verify JAR contains measure classes
cd backend/modules/services/cql-engine-service
./gradlew bootJar
jar tf build/libs/cql-engine-service-*.jar | grep BCSMeasure

# Expected: com/healthdata/cql/measure/BCSMeasure.class

# Rebuild and restart
./gradlew clean :modules:services:cql-engine-service:build
./gradlew :modules:services:cql-engine-service:bootRun
```

#### Issue 3: Quality Measure Service - No Measures Seeded

**Symptoms:**
```
GET /api/v1/measures returns empty array []
```

**Root Cause:** Auto-seeding disabled or failed

**Solution:**
```bash
# Check application.yml
cat backend/modules/services/quality-measure-service/src/main/resources/application.yml | grep auto-seed

# Expected:
# healthdata:
#   quality:
#     auto-seed-measures: true
#     default-tenant: acme-health

# Check logs for auto-seeding
docker compose logs quality-measure-service | grep "Auto-seeding"

# If disabled, manually seed
curl -X POST \
  -H "X-Tenant-ID: acme-health" \
  http://localhost:8087/api/v1/measures/seed
```

#### Issue 4: Value Sets Not Found

**Symptoms:**
```
Measure evaluation fails with "Value set not found: 2.16.840.1.113883.3.464.1003.108.12.1018"
```

**Root Cause:** Migration 0014 not applied

**Solution:**
```bash
# Check Liquibase changelog
docker compose exec postgres psql -U healthdata -d healthdata_cql \
  -c "SELECT id FROM databasechangelog WHERE id = '0014-seed-essential-value-sets';"

# If not found, manually run migration
cd backend/modules/services/cql-engine-service
./gradlew liquibaseUpdate

# Restart CQL Engine Service
```

#### Issue 5: Measure Evaluation Returns "Patient Not Found"

**Symptoms:**
```json
{
  "error": "Patient not found",
  "patientId": "abc-123"
}
```

**Solution:**
```bash
# Verify patient exists in FHIR Service
curl -H "X-Tenant-ID: acme-health" \
     http://localhost:8085/api/v1/Patient/abc-123

# If 404, seed patient data
curl -X POST \
  -H "X-Tenant-ID: acme-health" \
  -d '{"count": 10}' \
  http://localhost:8103/api/v1/demo/seed
```

---

## Configuration Reference

### Environment Variables

#### Global Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `TENANT_ID` | `acme-health` | Default tenant for demo data |
| `POSTGRES_HOST` | `localhost` | PostgreSQL hostname |
| `POSTGRES_PORT` | `5435` | PostgreSQL port |
| `REDIS_HOST` | `localhost` | Redis hostname |
| `REDIS_PORT` | `6380` | Redis port |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9094` | Kafka bootstrap servers |

#### FHIR Service

| Variable | Default | Description |
|----------|---------|-------------|
| `FHIR_SERVER_PORT` | `8085` | Service port |
| `FHIR_CACHE_TTL_SECONDS` | `300` | PHI cache TTL (5 minutes max) |
| `FHIR_BULK_EXPORT_ENABLED` | `true` | Enable bulk export API |

#### CQL Engine Service

| Variable | Default | Description |
|----------|---------|-------------|
| `CQL_ENGINE_PORT` | `8081` | Service port |
| `CQL_THREAD_POOL_SIZE` | `auto` | Thread pool size (default: 2x CPU cores) |
| `CQL_CACHE_ENABLED` | `true` | Enable measure result caching |

#### Quality Measure Service

| Variable | Default | Description |
|----------|---------|-------------|
| `QUALITY_MEASURE_PORT` | `8087` | Service port |
| `HEALTHDATA_QUALITY_AUTO_SEED_MEASURES` | `true` | Auto-seed measures on startup |
| `HEALTHDATA_QUALITY_DEFAULT_TENANT` | `acme-health` | Default tenant for seeding |

### Application Properties

#### application.yml (Quality Measure Service)

```yaml
healthdata:
  quality:
    auto-seed-measures: true        # Enable automatic measure seeding
    default-tenant: acme-health     # Tenant to seed for
    measure-cache-ttl: 300          # Cache TTL in seconds

spring:
  datasource:
    url: jdbc:postgresql://localhost:5435/healthdata_qm
    username: healthdata
    password: ${POSTGRES_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: validate            # CRITICAL: Use validate in production

  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml
```

---

## Startup Sequence Summary

### Development (Automated)

```bash
./scripts/test-end-to-end-workflow.sh
```

### Development (Manual)

```bash
# 1. Infrastructure
docker compose up -d postgres redis kafka

# 2. Services (in separate terminals)
./gradlew :modules:services:fhir-service:bootRun
./gradlew :modules:services:cql-engine-service:bootRun
./gradlew :modules:services:quality-measure-service:bootRun

# 3. Seed data
./gradlew :modules:services:demo-seeding-service:bootRun
curl -X POST -H "X-Tenant-ID: acme-health" \
  -d '{"count": 100}' \
  http://localhost:8103/api/v1/demo/seed
```

### Production

```bash
# 1. Infrastructure
docker compose -f docker-compose.production.yml up -d postgres redis kafka

# 2. Core services (wait for each health check)
docker compose -f docker-compose.production.yml up -d fhir-service
docker compose -f docker-compose.production.yml up -d cql-engine-service
docker compose -f docker-compose.production.yml up -d quality-measure-service

# 3. Gateway
docker compose -f docker-compose.production.yml up -d gateway-service

# 4. Verify
./scripts/health-check.sh
```

---

## Support & Documentation

- **Architecture:** `/docs/architecture/SYSTEM_ARCHITECTURE.md`
- **API Reference:** `/BACKEND_API_SPECIFICATION.md`
- **Authentication:** `/backend/docs/GATEWAY_TRUST_ARCHITECTURE.md`
- **Deployment:** `/docs/DEPLOYMENT_RUNBOOK.md`
- **HIPAA Compliance:** `/backend/HIPAA-CACHE-COMPLIANCE.md`

---

**Document Version:** 1.0
**Maintained By:** DevOps Team
**Last Review:** 2026-01-10
