# HDIM v3.0.0-rc1 Migration Guide

## Overview

HDIM v3.0.0-rc1 ("Shield") introduces **external integration adapters** and an **IHE Gateway** for health information exchange. All changes in this release are **additive** -- existing services, databases, and configurations remain unaffected. No modifications to running services are required unless you wish to enable the new integration capabilities.

New services introduced:

| Service | Port | Purpose |
|---------|------|---------|
| CoreHive Adapter | 8120 | AI engine integration (PHI Level: NONE) |
| Healthix Adapter | 8121 | HIE/PDR integration (PHI Level: FULL) |
| HEDIS Adapter | 8122 | HEDIS/MIPS/STAR measures (PHI Level: LIMITED) |
| IHE Gateway | 8125 | IHE XDS.b/PIXv3/XCA transactions (PHI Level: FULL) |

---

## Prerequisites

Before deploying v3.0.0-rc1, ensure the following are available:

| Component | Minimum Version | Notes |
|-----------|----------------|-------|
| Docker Compose | v2.20+ | Required for `--profile` support and service dependencies |
| PostgreSQL | 16 | Existing HDIM database server |
| Apache Kafka | 3.x | Existing HDIM Kafka cluster |
| Kong | 3.x | API gateway (format version 3.0) |
| Java | 21 (LTS) | Runtime for all adapter services |

---

## Database Setup

Four new databases must be created in the PostgreSQL instance. If you use the HDIM `init-multi-db.sh` script, these are added automatically. Otherwise, create them manually:

```sql
CREATE DATABASE corehive_adapter_db OWNER healthdata;
CREATE DATABASE healthix_adapter_db OWNER healthdata;
CREATE DATABASE hedis_adapter_db    OWNER healthdata;
CREATE DATABASE ihe_gateway_db      OWNER healthdata;
```

Each service uses Liquibase migrations with `ddl-auto: validate`. Schema creation happens automatically on first startup via Liquibase changelogs -- no manual DDL is required.

---

## Required Environment Variables

The following variables **must** be set in your `.env` file or environment before starting the new services:

| Variable | Description | Example |
|----------|-------------|---------|
| `POSTGRES_PASSWORD` | Password for the `healthdata` database user | `your-secure-password` |
| `JWT_SECRET` | Shared JWT signing secret for adapter authentication | `your-jwt-secret-key` |

---

## Feature Toggles

Each adapter can be independently enabled or disabled via environment variables. All default to `false` (disabled):

| Variable | Default | Description |
|----------|---------|-------------|
| `COREHIVE_ENABLED` | `false` | Enable CoreHive AI engine adapter |
| `HEALTHIX_ENABLED` | `false` | Enable Healthix HIE/PDR adapter |
| `HEDIS_ENABLED` | `false` | Enable HEDIS/MIPS/STAR adapter |

Set toggles to `true` in your `.env` file to activate the corresponding adapter:

```bash
# .env
COREHIVE_ENABLED=true
HEALTHIX_ENABLED=true
HEDIS_ENABLED=true
```

When a toggle is `false`, the adapter service starts but does not process events or connect to the external system.

---

## External Service URLs

Configure the URLs for each external system your deployment connects to. Defaults point to `host.docker.internal` for local development:

### CoreHive Adapter

| Variable | Default | Description |
|----------|---------|-------------|
| `COREHIVE_BASE_URL` | `http://host.docker.internal:3067` | CoreHive API base URL |
| `COREHIVE_API_KEY` | _(empty)_ | API key for CoreHive authentication |
| `COREHIVE_TIMEOUT_MS` | `5000` | Request timeout in milliseconds |

### Healthix Adapter

| Variable | Default | Description |
|----------|---------|-------------|
| `HEALTHIX_GATEWAY_URL` | `http://host.docker.internal:3000` | Healthix gateway URL |
| `HEALTHIX_FHIR_URL` | `http://host.docker.internal:8080` | Healthix FHIR server URL |
| `HEALTHIX_MPI_URL` | `http://host.docker.internal:8000` | Healthix MPI server URL |
| `HEALTHIX_DOCUMENT_URL` | `http://host.docker.internal:3010` | Healthix document repository URL |
| `HEALTHIX_HL7_URL` | `http://host.docker.internal:3020` | Healthix HL7 v2 endpoint URL |
| `HEALTHIX_MTLS_ENABLED` | `false` | Enable mutual TLS for Healthix |

### HEDIS Adapter

| Variable | Default | Description |
|----------|---------|-------------|
| `HEDIS_BASE_URL` | `http://host.docker.internal:3333` | HEDIS engine base URL |
| `HEDIS_CQL_URL` | `http://host.docker.internal:8090` | CQL evaluation engine URL |
| `HEDIS_API_KEY` | _(empty)_ | API key for HEDIS authentication |

### IHE Gateway

| Variable | Default | Description |
|----------|---------|-------------|
| `HEALTHIX_FHIR_URL` | `http://healthix-fhir:8080` | FHIR server for IHE transactions |
| `HEALTHIX_MPI_URL` | `http://healthix-mpi:8000` | MPI for PIXv3 patient identity |
| `HEALTHIX_DOCUMENT_URL` | `http://healthix-document:3010` | Document repository for XDS.b |

---

## Deployment

### Start All External Services

```bash
docker compose \
  -f docker-compose.yml \
  -f docker-compose.external-integrations.yml \
  --profile external \
  up -d
```

### Start Individual Adapters

```bash
# CoreHive only
docker compose -f docker-compose.yml -f docker-compose.external-integrations.yml \
  --profile external-corehive up -d

# Healthix only
docker compose -f docker-compose.yml -f docker-compose.external-integrations.yml \
  --profile external-healthix up -d

# HEDIS only
docker compose -f docker-compose.yml -f docker-compose.external-integrations.yml \
  --profile external-hedis up -d

# IHE Gateway only
docker compose -f docker-compose.yml -f docker-compose.external-integrations.yml \
  --profile external-ihe up -d
```

### Build Before Deploying (if needed)

```bash
# Build all adapter images
docker compose \
  -f docker-compose.yml \
  -f docker-compose.external-integrations.yml \
  --profile external \
  build
```

---

## Health Check Verification

After deployment, verify each service is running and healthy:

```bash
# CoreHive Adapter (port 8120)
curl -s http://localhost:8120/corehive-adapter/actuator/health | jq .
# Expected: {"status":"UP"}

# Healthix Adapter (port 8121)
curl -s http://localhost:8121/healthix-adapter/actuator/health | jq .
# Expected: {"status":"UP"}

# HEDIS Adapter (port 8122)
curl -s http://localhost:8122/hedis-adapter/actuator/health | jq .
# Expected: {"status":"UP"}

# IHE Gateway (port 8125)
curl -s http://localhost:8125/ihe-gateway/health | jq .
# Expected: {"status":"UP","service":"ihe-gateway-service","timestamp":"..."}
```

All four services should return a status of `UP`. If any service reports `DOWN`, check the container logs:

```bash
docker compose -f docker-compose.yml -f docker-compose.external-integrations.yml \
  logs -f <service-name>
```

---

## Kong Gateway Route Verification

After the adapters are running, verify that Kong is routing traffic to the new services:

```bash
# List all configured services
curl -s http://localhost:8001/services | jq '.data[].name'

# List all configured routes
curl -s http://localhost:8001/routes | jq '.data[].paths'

# Test routing through Kong (adjust port to your Kong proxy port)
curl -s http://localhost:8000/corehive-adapter/actuator/health | jq .
curl -s http://localhost:8000/healthix-adapter/actuator/health | jq .
curl -s http://localhost:8000/hedis-adapter/actuator/health | jq .
curl -s http://localhost:8000/ihe-gateway/health | jq .
```

---

## Grafana Dashboard Import

Import the pre-built monitoring dashboards for the new services:

1. Open Grafana at `http://localhost:3001`
2. Navigate to **Dashboards > Import**
3. Import each dashboard JSON from `monitoring/grafana/dashboards/`:
   - `external-adapters-overview.json` -- Combined adapter metrics
   - `ihe-gateway-transactions.json` -- IHE transaction monitoring
4. Select the **Prometheus** data source when prompted
5. Verify metrics are flowing by checking the dashboard panels

---

## Rollback Procedure

If issues arise, the external integration services can be disabled without affecting existing HDIM services.

### Step 1: Disable Feature Toggles

Set the relevant toggle(s) to `false` in your `.env` file:

```bash
COREHIVE_ENABLED=false
HEALTHIX_ENABLED=false
HEDIS_ENABLED=false
```

### Step 2: Stop the Adapter Containers

```bash
# Stop all external services
docker compose \
  -f docker-compose.yml \
  -f docker-compose.external-integrations.yml \
  --profile external \
  stop corehive-adapter-service healthix-adapter-service hedis-adapter-service ihe-gateway-service

# Or remove them entirely
docker compose \
  -f docker-compose.yml \
  -f docker-compose.external-integrations.yml \
  --profile external \
  rm -f corehive-adapter-service healthix-adapter-service hedis-adapter-service ihe-gateway-service
```

### Step 3: Verify Core Services Are Unaffected

```bash
# Confirm existing services are still healthy
docker compose ps --format "table {{.Name}}\t{{.Status}}"
```

The four new databases (`corehive_adapter_db`, `healthix_adapter_db`, `hedis_adapter_db`, `ihe_gateway_db`) can remain in place -- they consume minimal storage and do not affect other databases. Remove them only if you intend to fully uninstall the external integration capability.

---

## Support

For issues related to this release, consult:

- **Operator Runbook:** `docs/releases/v3.0.0-rc1/OPERATOR_RUNBOOK.md`
- **IHE Conformance Statement:** `docs/releases/v3.0.0-rc1/IHE_CONFORMANCE_STATEMENT.md`
- **Troubleshooting Guide:** `docs/troubleshooting/README.md`
