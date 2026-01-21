# HDIM Customer Simulation Architecture

**Version**: 1.0
**Last Updated**: January 5, 2026
**Purpose**: Enable realistic customer deployment simulation using WSL PostgreSQL and external FHIR data sources

---

## Executive Summary

This architecture enables HDIM to be tested and demonstrated in a configuration that simulates a real customer deployment:

1. **External PostgreSQL** (WSL) - Simulates customer's existing database infrastructure
2. **External FHIR Server** - Simulates customer's EHR/clinical data source (Epic, Cerner, etc.)
3. **Minimal Clinical Portal** - Lightweight deployment of HDIM services
4. **Versioned Docker Images** - Production-ready, immutable deployment artifacts

**Key Benefits**:
- Validate external connectivity before customer deployments
- Test data integration workflows with realistic data
- Create reproducible demo environments
- Verify HIPAA compliance in production-like settings

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Customer Simulation Environment                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  ┌───────────────────────┐        ┌────────────────────────────────────────┐ │
│  │   Windows/WSL Host    │        │         Docker Network                 │ │
│  │                       │        │                                         │ │
│  │  ┌─────────────────┐  │        │  ┌─────────────────────────────────┐   │ │
│  │  │  PostgreSQL 15  │◄─┼────────┼──┤      Gateway Service            │   │ │
│  │  │  (WSL Native)   │  │        │  │      (Authentication)           │   │ │
│  │  │                 │  │        │  └──────────────┬──────────────────┘   │ │
│  │  │  - healthdata_db│  │        │                 │                       │ │
│  │  │  - fhir_db      │  │        │  ┌──────────────┴──────────────────┐   │ │
│  │  │  - quality_db   │  │        │  │      Core Clinical Services      │   │ │
│  │  │  - caregap_db   │  │        │  │                                  │   │ │
│  │  │  - gateway_db   │  │        │  │  ┌──────────┐  ┌──────────────┐  │   │ │
│  │  └─────────────────┘  │        │  │  │FHIR Svc  │  │Quality Meas. │  │   │ │
│  │         ▲             │        │  │  │:8085     │  │:8087         │  │   │ │
│  │         │             │        │  │  └──────────┘  └──────────────┘  │   │ │
│  │         │             │        │  │                                  │   │ │
│  │  host.docker.internal │        │  │  ┌──────────┐  ┌──────────────┐  │   │ │
│  │         or            │        │  │  │Care Gap  │  │ Patient Svc  │  │   │ │
│  │  10.255.255.254       │        │  │  │:8086     │  │ :8084        │  │   │ │
│  │  (WSL nameserver)     │        │  │  └──────────┘  └──────────────┘  │   │ │
│  │                       │        │  │                                  │   │ │
│  └───────────────────────┘        │  │  ┌──────────┐                    │   │ │
│                                   │  │  │Demo Seed │                    │   │ │
│  ┌───────────────────────┐        │  │  │:8098     │                    │   │ │
│  │   External FHIR       │        │  │  └──────────┘                    │   │ │
│  │   Server              │        │  └──────────────────────────────────┘   │ │
│  │                       │        │                                         │ │
│  │  Option A: HAPI FHIR  │        │  ┌─────────────────────────────────┐   │ │
│  │  Option B: Synthea    │◄───────┼──┤   Clinical Portal (Angular)    │   │ │
│  │  Option C: Logica     │        │  │   :4200                         │   │ │
│  │                       │        │  └─────────────────────────────────┘   │ │
│  └───────────────────────┘        │                                         │ │
│                                   │  ┌─────────────────────────────────┐   │ │
│                                   │  │      Supporting Services        │   │ │
│                                   │  │                                  │   │ │
│                                   │  │  ┌──────────┐  ┌──────────────┐  │   │ │
│                                   │  │  │ Redis    │  │ Jaeger       │  │   │ │
│                                   │  │  │ :6379    │  │ :16686       │  │   │ │
│                                   │  │  └──────────┘  └──────────────┘  │   │ │
│                                   │  └─────────────────────────────────┘   │ │
│                                   └────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Component 1: External PostgreSQL (WSL)

### Purpose
Simulate a customer's existing PostgreSQL infrastructure, demonstrating HDIM's ability to connect to external databases rather than requiring a bundled database.

### Setup Options

#### Option A: Native WSL PostgreSQL (Recommended)
```bash
# Install PostgreSQL in WSL
sudo apt update
sudo apt install postgresql postgresql-contrib

# Start PostgreSQL
sudo service postgresql start

# Configure for external connections
sudo nano /etc/postgresql/15/main/postgresql.conf
# Set: listen_addresses = '*'

sudo nano /etc/postgresql/15/main/pg_hba.conf
# Add: host all all 0.0.0.0/0 md5

# Restart
sudo service postgresql restart
```

#### Option B: Docker PostgreSQL on Host Network
```yaml
# docker-compose.external-postgres.yml
services:
  external-postgres:
    image: postgres:16-alpine
    network_mode: "host"
    environment:
      POSTGRES_USER: healthdata
      POSTGRES_PASSWORD: healthdata_password
      POSTGRES_DB: healthdata_db
    volumes:
      - external_postgres_data:/var/lib/postgresql/data
```

### Database Initialization
```bash
# Run the external database setup script
psql -U postgres -f docker/postgres/external-db-setup.sql

# Creates 30 databases:
# - healthdata_db, fhir_db, cql_db, quality_db, patient_db
# - caregap_db, consent_db, gateway_db, etc.
```

### Connection Configuration
```bash
# .env configuration for HDIM services
POSTGRES_HOST=host.docker.internal  # For Docker Desktop
# OR
POSTGRES_HOST=10.255.255.254        # For WSL (from /etc/resolv.conf)
POSTGRES_PORT=5432
POSTGRES_USER=healthdata
POSTGRES_PASSWORD=healthdata_password
```

### Persistent Testing Session
```bash
# Open persistent psql session for monitoring/testing
psql -h localhost -U healthdata -d healthdata_db

# Useful queries for testing:
\dt                                    # List tables
SELECT count(*) FROM patients;         # Patient count
SELECT * FROM audit_logs LIMIT 10;     # Audit trail
\watch 2                               # Auto-refresh every 2 seconds
```

---

## Component 2: External FHIR Server

### Purpose
Simulate customer EHR data sources (Epic, Cerner, Meditech) by providing realistic clinical FHIR R4 data.

### Option A: HAPI FHIR Server (Recommended for Development)

```yaml
# docker-compose.fhir-server.yml
services:
  hapi-fhir:
    image: hapiproject/hapi:latest
    container_name: external-fhir-server
    ports:
      - "8080:8080"
    environment:
      hapi.fhir.fhir_version: R4
      hapi.fhir.allow_external_references: "true"
      hapi.fhir.reuse_cached_search_results_millis: 60000
    volumes:
      - hapi_data:/data/hapi
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/fhir/metadata"]
      interval: 30s
      timeout: 10s
      retries: 5
```

### Option B: Logica Health Sandbox (Realistic EHR Data)
```bash
# Connect to Logica's public FHIR sandbox
EXTERNAL_FHIR_URL=https://api.logicahealth.org/hdimtest/open
```

### Option C: Synthea-Generated Data (Customizable)

```bash
# Generate synthetic patient data with Synthea
docker run --rm -v $(pwd)/synthea-output:/output \
  synthetichealth/synthea:3.1.1 \
  -p 1000 \                          # 1000 patients
  --exporter.fhir.export=true \
  --exporter.years_of_history=5 \
  Massachusetts                       # State-specific demographics
```

### FHIR Connector Configuration
```yaml
# application.yml for ehr-connector-service
fhir:
  external:
    enabled: true
    servers:
      - name: "customer-ehr"
        url: ${EXTERNAL_FHIR_URL:http://hapi-fhir:8080/fhir}
        auth:
          type: ${FHIR_AUTH_TYPE:none}  # none, basic, oauth2, smart
          # For OAuth2/SMART:
          client-id: ${FHIR_CLIENT_ID:}
          client-secret: ${FHIR_CLIENT_SECRET:}
          token-url: ${FHIR_TOKEN_URL:}
        sync:
          enabled: true
          schedule: "0 0 * * * *"  # Hourly sync
          resources:
            - Patient
            - Condition
            - Observation
            - MedicationRequest
            - Procedure
            - Encounter
            - DiagnosticReport
```

### Data Flow Architecture
```
┌─────────────────┐       ┌──────────────────┐       ┌─────────────────┐
│ External FHIR   │       │ EHR Connector    │       │ HDIM FHIR       │
│ Server          │───────│ Service          │───────│ Service         │
│                 │ Pull  │                  │ Store │                 │
│ - Epic          │       │ - Transform      │       │ - Cache         │
│ - Cerner        │       │ - Validate       │       │ - Index         │
│ - HAPI FHIR     │       │ - Enrich         │       │ - Serve         │
└─────────────────┘       └──────────────────┘       └─────────────────┘
        │                          │                          │
        │                          ▼                          │
        │                 ┌──────────────────┐                │
        │                 │ Quality Measure  │                │
        │                 │ Service          │                │
        │                 │                  │                │
        │                 │ - CQL Evaluation │                │
        │                 │ - Care Gaps      │                │
        │                 │ - Risk Scores    │                │
        │                 └──────────────────┘                │
        │                          │                          │
        └──────────────────────────┴──────────────────────────┘
```

---

## Component 3: Minimal Clinical Portal

### Service Tier Definitions

| Tier | Services | RAM | Use Case |
|------|----------|-----|----------|
| **Light** | PostgreSQL, Redis | ~1GB | CI/CD testing |
| **Minimal** | Light + Gateway, FHIR, Patient | ~3GB | Basic FHIR operations |
| **Core** | Minimal + Quality, CareGap, CQL | ~5GB | Quality measure evaluation |
| **Demo** | Core + Demo Seeding, Analytics | ~7GB | Customer demos |
| **Full** | All 28 services | ~12GB | Full platform |

### Minimal Clinical Portal (Core Tier)

```yaml
# docker-compose.minimal-clinical.yml
version: '3.8'

services:
  # Infrastructure (External PostgreSQL - not in Docker)
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s

  # Gateway (Authentication entry point)
  gateway-service:
    image: hdim/gateway-service:${VERSION:-latest}
    ports:
      - "8001:8001"
    environment:
      SPRING_PROFILES_ACTIVE: external-db
      SPRING_DATASOURCE_URL: jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/gateway_db
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      GATEWAY_AUTH_DEV_MODE: ${GATEWAY_AUTH_DEV_MODE:-true}
    extra_hosts:
      - "host.docker.internal:host-gateway"
    depends_on:
      - redis

  # FHIR Service (Clinical data)
  fhir-service:
    image: hdim/fhir-service:${VERSION:-latest}
    ports:
      - "8085:8085"
    environment:
      SPRING_PROFILES_ACTIVE: external-db
      SPRING_DATASOURCE_URL: jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/fhir_db
      EXTERNAL_FHIR_URL: ${EXTERNAL_FHIR_URL:-http://hapi-fhir:8080/fhir}
    extra_hosts:
      - "host.docker.internal:host-gateway"
    depends_on:
      - redis

  # Patient Service
  patient-service:
    image: hdim/patient-service:${VERSION:-latest}
    ports:
      - "8084:8084"
    environment:
      SPRING_PROFILES_ACTIVE: external-db
      SPRING_DATASOURCE_URL: jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/patient_db
    extra_hosts:
      - "host.docker.internal:host-gateway"
    depends_on:
      - redis

  # Quality Measure Service (CQL evaluation)
  quality-measure-service:
    image: hdim/quality-measure-service:${VERSION:-latest}
    ports:
      - "8087:8087"
    environment:
      SPRING_PROFILES_ACTIVE: external-db
      SPRING_DATASOURCE_URL: jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/quality_db
    extra_hosts:
      - "host.docker.internal:host-gateway"
    depends_on:
      - fhir-service

  # Care Gap Service
  care-gap-service:
    image: hdim/care-gap-service:${VERSION:-latest}
    ports:
      - "8086:8086"
    environment:
      SPRING_PROFILES_ACTIVE: external-db
      SPRING_DATASOURCE_URL: jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/caregap_db
    extra_hosts:
      - "host.docker.internal:host-gateway"
    depends_on:
      - quality-measure-service

  # Clinical Portal (Angular frontend)
  clinical-portal:
    image: hdim/clinical-portal:${VERSION:-latest}
    ports:
      - "4200:80"
    environment:
      API_URL: http://gateway-service:8001
    depends_on:
      - gateway-service

networks:
  default:
    name: hdim-clinical-network
```

### Required Services for Clinical Portal

| Service | Port | Purpose | Dependencies |
|---------|------|---------|--------------|
| gateway-service | 8001 | JWT auth, routing | PostgreSQL, Redis |
| fhir-service | 8085 | FHIR R4 storage | PostgreSQL, Redis |
| patient-service | 8084 | Patient management | PostgreSQL, Redis |
| quality-measure-service | 8087 | CQL evaluation | FHIR, PostgreSQL |
| care-gap-service | 8086 | Gap detection | Quality Measure |
| clinical-portal | 4200 | Angular UI | Gateway |

---

## Component 4: Docker Versioning Strategy

### Version Naming Convention

```
hdim/<service-name>:<major>.<minor>.<patch>[-<prerelease>]

Examples:
  hdim/fhir-service:1.0.0
  hdim/fhir-service:1.1.0-beta.1
  hdim/fhir-service:1.1.0-rc.1
  hdim/fhir-service:latest
```

### Semantic Versioning Rules

| Change Type | Version Bump | Example |
|-------------|--------------|---------|
| Breaking API change | Major | 1.0.0 → 2.0.0 |
| New feature (backward compatible) | Minor | 1.0.0 → 1.1.0 |
| Bug fix | Patch | 1.0.0 → 1.0.1 |
| Pre-release | Suffix | 1.1.0-alpha.1 |

### Build & Push Workflow

```bash
#!/bin/bash
# scripts/build-release.sh

VERSION=${1:-"1.0.0"}
REGISTRY=${DOCKER_REGISTRY:-"hdim"}

# Build all service images
services=(
  "gateway-service"
  "fhir-service"
  "patient-service"
  "quality-measure-service"
  "care-gap-service"
  "clinical-portal"
)

for service in "${services[@]}"; do
  echo "Building ${service}:${VERSION}..."

  # Build
  docker build \
    -t ${REGISTRY}/${service}:${VERSION} \
    -t ${REGISTRY}/${service}:latest \
    -f backend/modules/services/${service}/Dockerfile \
    backend/

  # Push to registry
  docker push ${REGISTRY}/${service}:${VERSION}
  docker push ${REGISTRY}/${service}:latest
done

# Create release manifest
cat > release-${VERSION}.yml << EOF
version: "${VERSION}"
date: "$(date -Iseconds)"
services:
$(for s in "${services[@]}"; do echo "  - ${REGISTRY}/${s}:${VERSION}"; done)
EOF
```

### Multi-Architecture Builds

```yaml
# .github/workflows/docker-build.yml
name: Build and Push Docker Images

on:
  push:
    tags:
      - 'v*'

jobs:
  build:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        service:
          - gateway-service
          - fhir-service
          - patient-service
          - quality-measure-service
          - care-gap-service
          - clinical-portal

    steps:
      - uses: actions/checkout@v4

      - name: Set up QEMU
        uses: docker/setup-qemu-action@v3

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to Registry
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: backend/
          file: backend/modules/services/${{ matrix.service }}/Dockerfile
          platforms: linux/amd64,linux/arm64
          push: true
          tags: |
            ghcr.io/${{ github.repository }}/${{ matrix.service }}:${{ github.ref_name }}
            ghcr.io/${{ github.repository }}/${{ matrix.service }}:latest
```

### Image Optimization

```dockerfile
# Dockerfile.optimized (multi-stage build)
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY modules modules

# Build only the specific service
RUN ./gradlew :modules:services:${SERVICE_NAME}:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Add non-root user
RUN addgroup -g 1001 hdim && adduser -u 1001 -G hdim -s /bin/sh -D hdim
USER hdim

COPY --from=builder /app/modules/services/${SERVICE_NAME}/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## Implementation Roadmap

### Phase 1: External Database Foundation (Day 1)

**Goal**: Establish PostgreSQL in WSL as persistent testing database

| Task | Duration | Deliverable |
|------|----------|-------------|
| Install PostgreSQL in WSL | 15 min | Running PostgreSQL |
| Run external-db-setup.sql | 10 min | 30 databases created |
| Verify connectivity from Docker | 15 min | Services connect |
| Create monitoring psql session | 10 min | Live query session |

**Verification**:
```bash
# Test connectivity
docker run --rm postgres:16 psql \
  -h host.docker.internal -U healthdata -d healthdata_db \
  -c "SELECT 1"
```

### Phase 2: External FHIR Server (Day 1-2)

**Goal**: Connect HDIM to external clinical data source

| Task | Duration | Deliverable |
|------|----------|-------------|
| Deploy HAPI FHIR server | 20 min | FHIR server at :8080 |
| Load Synthea test data | 30 min | 100+ test patients |
| Configure EHR connector | 30 min | Data sync working |
| Validate FHIR queries | 20 min | Patient search works |

**Verification**:
```bash
# Query external FHIR server
curl http://localhost:8080/fhir/Patient?_count=5
```

### Phase 3: Minimal Clinical Portal (Day 2)

**Goal**: Deploy minimal service set for clinical workflows

| Task | Duration | Deliverable |
|------|----------|-------------|
| Build versioned images | 45 min | Tagged images |
| Create docker-compose.minimal-clinical.yml | 30 min | Compose file |
| Start minimal stack | 15 min | Services running |
| Verify gateway auth | 20 min | Login works |
| Test patient search | 15 min | Search returns results |

**Verification**:
```bash
# Health check all services
curl http://localhost:8001/actuator/health
curl http://localhost:8085/fhir/actuator/health
curl http://localhost:8087/quality-measure/actuator/health
```

### Phase 4: Versioned Release (Day 3)

**Goal**: Create production-ready versioned Docker images

| Task | Duration | Deliverable |
|------|----------|-------------|
| Tag version 1.0.0 | 15 min | Git tags |
| Build multi-arch images | 60 min | amd64/arm64 images |
| Push to registry | 20 min | Images published |
| Create release notes | 30 min | CHANGELOG.md |
| Test deployment from registry | 30 min | Clean deployment |

**Verification**:
```bash
# Deploy from registry
docker compose -f docker-compose.minimal-clinical.yml pull
docker compose -f docker-compose.minimal-clinical.yml up -d
```

### Phase 5: End-to-End Validation (Day 4)

**Goal**: Validate complete customer simulation workflow

| Task | Duration | Deliverable |
|------|----------|-------------|
| Import patient from external FHIR | 30 min | Patients in HDIM |
| Run quality measure evaluation | 30 min | Care gaps identified |
| Verify audit trail | 15 min | Audit logs in PostgreSQL |
| Performance baseline | 45 min | Timing benchmarks |
| Document results | 30 min | Test report |

---

## Quick Start Commands

### Option 1: Full Setup (Recommended)

```bash
# 1. Start PostgreSQL in WSL (if not running)
sudo service postgresql start

# 2. Initialize databases
psql -U postgres -f docker/postgres/external-db-setup.sql

# 3. Set environment
cp .env.external-db .env
# Edit .env with correct POSTGRES_HOST

# 4. Start external FHIR server
docker compose -f docker-compose.fhir-server.yml up -d

# 5. Load synthetic data
docker exec external-fhir-server /load-synthea-data.sh

# 6. Start minimal clinical portal
docker compose -f docker-compose.minimal-clinical.yml up -d

# 7. Access portal
open http://localhost:4200
```

### Option 2: Demo Mode (Quick)

```bash
# Use existing Docker PostgreSQL + external FHIR only
docker compose --profile demo up -d

# Seed demo data
curl -X POST http://localhost:8098/api/v1/demo/reset
```

---

## Environment Files

### .env.customer-simulation
```bash
# Customer Simulation Environment
# Simulates connecting to customer's existing infrastructure

# External PostgreSQL (WSL)
POSTGRES_HOST=host.docker.internal
POSTGRES_PORT=5432
POSTGRES_USER=healthdata
POSTGRES_PASSWORD=healthdata_password

# External FHIR Server
EXTERNAL_FHIR_URL=http://hapi-fhir:8080/fhir
FHIR_AUTH_TYPE=none

# Service Versions
VERSION=1.0.0

# Authentication (dev mode)
GATEWAY_AUTH_DEV_MODE=true
JWT_SECRET=hdim-dev-jwt-secret-key-for-local-development-only-64chars

# Redis (Docker)
REDIS_HOST=redis
REDIS_PORT=6379

# Optional: Kafka (if needed for eventing)
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
```

---

## Monitoring & Debugging

### PostgreSQL Monitoring
```sql
-- Active connections
SELECT datname, count(*)
FROM pg_stat_activity
GROUP BY datname;

-- Slow queries
SELECT pid, now() - pg_stat_activity.query_start AS duration, query
FROM pg_stat_activity
WHERE (now() - pg_stat_activity.query_start) > interval '5 seconds';

-- Table sizes
SELECT relname, pg_size_pretty(pg_total_relation_size(relid))
FROM pg_catalog.pg_statio_user_tables
ORDER BY pg_total_relation_size(relid) DESC;
```

### Service Health Checks
```bash
#!/bin/bash
# scripts/health-check.sh

services=(
  "gateway-service:8001"
  "fhir-service:8085"
  "patient-service:8084"
  "quality-measure-service:8087"
  "care-gap-service:8086"
)

for svc in "${services[@]}"; do
  name="${svc%%:*}"
  port="${svc##*:}"
  status=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:${port}/actuator/health)
  echo "${name}: ${status}"
done
```

### Jaeger Tracing
```bash
# Access Jaeger UI
open http://localhost:16686

# Query traces for specific service
# Select service: fhir-service
# Operation: findPatients
# Time range: Last 15 minutes
```

---

## Security Considerations

### External Database Security
- Use SSL/TLS for PostgreSQL connections in production
- Rotate credentials regularly
- Limit database user permissions (least privilege)
- Enable pgAudit for database audit logging

### External FHIR Server Security
- Use OAuth2/SMART for authentication when connecting to real EHRs
- Validate FHIR resource signatures
- Implement rate limiting
- Log all data access

### Network Security
- Use Docker network isolation
- Expose only necessary ports
- Use internal DNS for service-to-service communication

---

## Troubleshooting

### PostgreSQL Connection Issues

```bash
# Check PostgreSQL is listening
sudo netstat -tlnp | grep 5432

# Test from Docker
docker run --rm postgres:16 pg_isready \
  -h host.docker.internal -p 5432 -U healthdata

# Check pg_hba.conf allows Docker network
sudo cat /etc/postgresql/15/main/pg_hba.conf | grep -v "^#"
```

### FHIR Server Issues

```bash
# Check FHIR server logs
docker logs external-fhir-server

# Validate FHIR endpoint
curl -s http://localhost:8080/fhir/metadata | jq '.fhirVersion'
```

### Service Startup Issues

```bash
# Check service logs
docker compose -f docker-compose.minimal-clinical.yml logs -f fhir-service

# Verify environment variables
docker compose -f docker-compose.minimal-clinical.yml config
```

---

## Success Criteria

| Criterion | Measurement | Target |
|-----------|-------------|--------|
| External DB connectivity | All services connect | 100% |
| FHIR data import | Patients synced from external FHIR | > 100 patients |
| Clinical portal loads | Dashboard renders | < 3 seconds |
| Quality measure evaluation | CQL evaluation completes | < 15 seconds for 1000 patients |
| Audit trail | All PHI access logged | 100% coverage |
| Docker image size | Optimized images | < 500MB per service |
| Version reproducibility | Same image produces same results | 100% |

---

## Next Steps

1. **Immediate**: Start PostgreSQL in WSL and run setup script
2. **Today**: Deploy HAPI FHIR server and load test data
3. **Tomorrow**: Build and test minimal clinical portal deployment
4. **This Week**: Create versioned release and publish images

---

**Document Owner**: Architecture Team
**Review Cadence**: Monthly
**Last Reviewed**: January 5, 2026
