# Docker Compose Usage Guide

**Status**: Production ✅
**Last Updated**: January 19, 2026
**Total Files**: 17 docker-compose configurations

---

## Overview

HDIM provides 18 specialized docker-compose files for different deployment scenarios, development modes, and testing strategies. Each file is optimized for specific use cases rather than having a single "one-size-fits-all" configuration.

### Quick Start (Choose One)

```bash
# Development - Everything locally with all services
docker compose up -d

# Minimal - Just infrastructure (Postgres, Redis)
docker compose --profile light up -d

# Core clinical services only
docker compose --profile core up -d

# Demo - Phase 5 event services
docker compose --profile demo up -d

# Production-like with security hardening
docker compose -f docker-compose.dev-hardened.yml up -d
```

---

## File Matrix: When to Use What

| File | Purpose | Services | Use Case | RAM |
|------|---------|----------|----------|-----|
| **docker-compose.yml** | Full stack with all profiles | 28+ services | Local development, testing all features | 8-16GB |
| **docker-compose.local.yml** | Minimal infrastructure override | postgres, redis only | CI/CD, GitHub Actions act runner | <1GB |
| **docker-compose.dev-hardened.yml** | Security hardening for dev | Core services with security | Security testing, hardened dev environment | 6-8GB |
| **docker-compose.demo.yml** | Full demo with all features | All 28+ services + demo data | Product demos, evaluation | 8GB |
| **docker-compose.demo-core.yml** | Demo with core services only | Core + demo data (no AI/analytics) | Lightweight demo | 4-6GB |
| **docker-compose.minimal-clinical.yml** | Clinical workflows only | Clinical core services | Clinical feature development | 4-6GB |
| **docker-compose.prod.yml** | Production template | Simplified for prod deployment | Reference for production | N/A |
| **docker-compose.production.yml** | Full production setup | All services production-ready | Production deployment base | N/A |
| **docker-compose.staging.yml** | Staging environment | All services in staging config | Pre-production testing | 8GB |
| **docker-compose.test.yml** | Testing environment | Services for integration testing | Integration tests, test suites | 4GB |
| **docker-compose.external-db.yml** | External database config | Services with external Postgres | Testing with external DB | 6-8GB |
| **docker-compose.fhir-server.yml** | FHIR-only setup | FHIR service + dependencies | FHIR compliance testing | 2-3GB |
| **docker-compose.ha.yml** | High availability setup | HA-configured services | HA testing, redundancy | 10-12GB |
| **docker-compose.redis-ha.yml** | Redis high availability | Redis cluster setup | Redis HA testing | 3-4GB |
| **docker-compose.observability.yml** | Observability stack | Prometheus, Grafana, logging | Monitoring setup testing | 2-3GB |
| **docker-compose.resources.yml** | Resource constraints | Services with limits/requests | Resource testing, performance | Variable |
| **docker-compose.secrets.yml** | Secrets management | Services with HashiCorp Vault | Secrets handling testing | 3-4GB |

---

## Profiles: Quick Reference

The main `docker-compose.yml` supports these profiles:

```bash
# Profile Usage:
docker compose --profile PROFILE up -d

# Or combine multiple profiles:
docker compose --profile core --profile analytics up -d
```

### Available Profiles

| Profile | Services | Use Case | RAM |
|---------|----------|----------|-----|
| **light** | Postgres, Redis, Kafka | Infrastructure only | <1GB |
| **core** | light + 10 clinical services | Core clinical functionality | 4-6GB |
| **ai** | core + 3 AI services | With AI/ML capabilities | 6-8GB |
| **analytics** | core + 3 analytics services | With analytics suite | 6-8GB |
| **demo** | core + 4 Event services (Phase 5) | Modern event-driven architecture | 6-8GB |
| **full** | all of the above | Everything | 8-16GB |

**Profile Details**:

- **light**: Postgres, Redis, Kafka, Prometheus, Grafana, Jaeger (observability)
- **core**: light + Patient Service, Quality Measure, FHIR, Care Gap, CQL Engine, Gateway services
- **ai**: core + Predictive Analytics, Risk Stratification, Clinical Decision Support
- **analytics**: core + Analytics Service, Reporting Engine, Data Warehouse
- **demo**: core + Patient Event Service, Quality Measure Event Service, Care Gap Event Service, Clinical Workflow Event Service (Event Sourcing architecture)
- **full**: All profiles combined

---

## Decision Tree: Which File to Use?

```
Start
  │
  ├─ What's your goal?
  │
  ├─ "Run everything locally"
  │  └─→ docker compose up -d
  │      (main docker-compose.yml with all profiles)
  │
  ├─ "Just testing infrastructure/CI"
  │  └─→ docker compose -f docker-compose.local.yml up -d
  │      (Fast startup, minimal RAM)
  │
  ├─ "Want to see Event Sourcing demo"
  │  └─→ docker compose --profile demo up -d
  │      (Event services + core clinical)
  │
  ├─ "Need demo with all features"
  │  └─→ docker compose -f docker-compose.demo.yml up -d
  │      (Full feature set + demo data)
  │
  ├─ "Testing clinical workflows only"
  │  └─→ docker compose -f docker-compose.minimal-clinical.yml up -d
  │      (Just clinical services)
  │
  ├─ "Security hardening test"
  │  └─→ docker compose -f docker-compose.dev-hardened.yml up -d
  │      (Enhanced security config)
  │
  ├─ "Integration testing"
  │  └─→ docker compose -f docker-compose.test.yml up -d
  │      (Test environment setup)
  │
  ├─ "HA/Redundancy testing"
  │  └─→ docker compose -f docker-compose.ha.yml up -d
  │      (HA-configured services)
  │
  ├─ "FHIR compliance only"
  │  └─→ docker compose -f docker-compose.fhir-server.yml up -d
  │      (FHIR service + dependencies)
  │
  └─ "Production preparation"
     └─→ docker compose -f docker-compose.production.yml up -d
         (Production-ready reference)
```

---

## Common Scenarios

### Scenario 1: Local Development (Full Stack)

```bash
# Start everything with default profile
docker compose up -d

# Verify all services running
docker compose ps

# View logs
docker compose logs -f

# Access services:
# - Clinical Portal: http://localhost:4200
# - Admin Portal: http://localhost:4201
# - Patient Service: http://localhost:8084
# - Quality Measure: http://localhost:8087
# - FHIR Service: http://localhost:8085
# - Care Gap Service: http://localhost:8086
# - Grafana: http://localhost:3000
# - Jaeger: http://localhost:16686
```

### Scenario 2: CI/CD with GitHub Actions (Minimal)

```bash
# Minimal infrastructure for act runner
docker compose -f docker-compose.local.yml up -d

# Run GitHub Actions locally
act -j build

# The workflow will have access to:
# - Postgres (connection string: localhost:5435)
# - Redis (connection string: localhost:6380)
# - Kafka (bootstrap servers: localhost:9094)

# Stop when done
docker compose -f docker-compose.local.yml down
```

### Scenario 3: Event Sourcing Demo (Phase 5)

```bash
# Start core services + Event services
docker compose --profile demo up -d

# Services include:
# - patient-event-service (8110) - REST API for patient events
# - patient-event-handler-service - Business logic library
# - quality-measure-event-service (8091) - Quality measure events
# - quality-measure-event-handler-service - Business logic library
# - care-gap-event-service (8111) - Care gap events
# - care-gap-event-handler-service - Business logic library
# - clinical-workflow-event-service (8093) - Workflow events
# - clinical-workflow-event-handler-service - Business logic library

# View event processing
docker compose logs -f patient-event-service
docker compose logs -f patient-event-handler-service

# Test event API
curl -X POST http://localhost:8110/api/v1/patients/events \
  -H "Content-Type: application/json" \
  -d '{"patientId": "123", "eventType": "PATIENT_CREATED"}'
```

### Scenario 4: Clinical Workflows Only

```bash
# Minimal clinical setup for feature development
docker compose -f docker-compose.minimal-clinical.yml up -d

# Services: Patient, Quality Measure, FHIR, Care Gap, CQL Engine

# Faster startup, less resource usage
# Perfect for focused feature work
```

### Scenario 5: Security Testing (Hardened Dev)

```bash
# Development with enhanced security
docker compose -f docker-compose.dev-hardened.yml up -d

# Includes:
# - JWT token validation
# - RBAC enforcement
# - Rate limiting
# - Multi-tenant isolation
# - Audit logging

# Test with strict authentication
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8001/api/v1/patients
```

### Scenario 6: Production Reference

```bash
# Review production configuration (don't start in dev!)
cat docker-compose.production.yml

# Key differences from dev:
# - No port mappings (behind reverse proxy)
# - Health checks configured
# - Resource limits set
# - Logging configured
# - Monitoring/metrics enabled
# - Secrets from vault

# For actual production deployment:
# 1. Adjust image versions to specific tags (not latest)
# 2. Configure environment variables from Vault
# 3. Set resource limits for your infrastructure
# 4. Configure persistent storage
# 5. Set up load balancing/reverse proxy
# 6. Enable monitoring and alerting
```

---

## Advanced Usage

### Combining Multiple Compose Files

```bash
# Dev environment with observability
docker compose \
  -f docker-compose.yml \
  -f docker-compose.observability.yml \
  up -d

# This merges configurations:
# - All services from docker-compose.yml
# - Observability stacks (Prometheus, Grafana, etc.) from second file

# Services are combined, not replaced
```

### Using Override Files

```bash
# Create custom override for local environment
cat > docker-compose.override.yml <<EOF
version: '3.8'
services:
  patient-service:
    environment:
      LOG_LEVEL: DEBUG
      DATABASE_POOL_SIZE: 5
  quality-measure-service:
    build:
      context: ./backend
      dockerfile: modules/services/quality-measure-service/Dockerfile
    # Override port mapping
    ports:
      - "8087:8080"
EOF

# This automatically applies when you run docker compose
docker compose up -d
# Override file is automatically loaded
```

### Environment-Specific Configuration

```bash
# Development with custom settings
cat > docker-compose.override.dev.yml <<EOF
services:
  postgres:
    command: postgres -c log_statement=all -c log_min_duration_statement=1000
  redis:
    command: redis-server --loglevel debug
EOF

docker compose -f docker-compose.yml -f docker-compose.override.dev.yml up -d
```

### Service-Specific Profiles

```bash
# Run specific services with dependencies
docker compose --profile light --profile analytics up -d

# This includes:
# - light profile (infrastructure)
# - analytics profile services
```

---

## Troubleshooting

### Port Already in Use

```bash
# Check which services are running
docker compose ps

# Kill process on port
# macOS/Linux
lsof -ti :8001 | xargs kill -9

# Or stop and remove containers
docker compose down

# Remove all containers (careful!)
docker compose down -v  # Also removes volumes
```

### Out of Memory

```bash
# Check RAM usage
docker stats --no-stream

# Solution 1: Use lighter profile
docker compose -f docker-compose.local.yml up -d  # <1GB

# Solution 2: Stop non-essential services
docker compose stop quality-measure-service care-gap-service

# Solution 3: Use minimal clinical setup
docker compose -f docker-compose.minimal-clinical.yml up -d
```

### Services Won't Start

```bash
# Check logs
docker compose logs service-name

# Check database connectivity
docker compose exec postgres psql -U healthdata -c "\l"

# Check migrations
docker compose logs patient-service | grep -i liquibase

# Restart everything cleanly
docker compose down -v
docker compose up -d
```

### Slow Performance

```bash
# Check CPU/Memory
docker stats

# Solutions:
1. Reduce services (use a lighter profile or specific file)
2. Increase Docker resources (Preferences → Resources)
3. Use docker-compose.resources.yml with constraints
4. Run health checks: docker compose ps
5. Check logs for errors: docker compose logs -f
```

### Database Schema Issues

```bash
# Check if migrations ran
docker exec healthdata-postgres psql -U healthdata -d patient_db \
  -c "SELECT * FROM databasechangelog LIMIT 5;"

# Clear Liquibase lock if stuck
docker exec healthdata-postgres psql -U healthdata -d patient_db \
  -c "DELETE FROM databasechangeloglock WHERE ID=1;"

# Restart service to retry migrations
docker compose restart patient-service
```

---

## File Organization

```
hdim-master/
├── docker-compose.yml                 # ← Start here (main, has profiles)
├── docker-compose.local.yml            # ← For CI/testing
├── docker-compose.dev-hardened.yml    # ← For security testing
├── docker-compose.demo*.yml            # ← For product demos
├── docker-compose.minimal-clinical.yml # ← For clinical dev
├── docker-compose.prod.yml             # ← Production reference
├── docker-compose.production.yml       # ← Full production setup
├── docker-compose.staging.yml          # ← Staging env
├── docker-compose.test.yml             # ← For testing
├── docker-compose.external-db.yml      # ← With external DB
├── docker-compose.fhir-server.yml      # ← FHIR-only
├── docker-compose.ha.yml               # ← HA setup
├── docker-compose.redis-ha.yml         # ← Redis HA
├── docker-compose.observability.yml    # ← Monitoring
├── docker-compose.resources.yml        # ← Resource limits
├── docker-compose.secrets.yml          # ← Vault integration
├── docker/
│   ├── README.md                       # ← You are here
│   ├── DOCKER_DEPLOY_QUICK_REFERENCE.md
│   ├── LOCAL_TESTING_GUIDE.md
│   ├── deploy.sh
│   ├── postgres/
│   ├── redis/
│   ├── grafana/
│   ├── prometheus/
│   ├── otel/
│   ├── vault/
│   ├── nginx/
│   ├── gateway-edge/
│   └── ...
```

---

## Quick Commands Reference

```bash
# Start (choose one method)
docker compose up -d                                    # Full stack
docker compose --profile light up -d                    # Infrastructure only
docker compose --profile demo up -d                     # Event services demo
docker compose -f docker-compose.local.yml up -d        # CI/testing

# Status
docker compose ps                                       # List all services
docker compose ps --filter "status=running"             # Running only
docker stats --no-stream                                # Resource usage

# Logs
docker compose logs                                     # All logs
docker compose logs -f service-name                     # Follow specific service
docker compose logs --tail=50 service-name              # Last 50 lines

# Management
docker compose restart service-name                     # Restart one
docker compose restart                                  # Restart all
docker compose down                                     # Stop all (keep volumes)
docker compose down -v                                  # Stop all (remove volumes)
docker compose pull                                     # Update images

# Debugging
docker compose exec service-name bash                   # Shell into container
docker compose exec postgres psql -U healthdata         # PostgreSQL shell
docker compose logs service-name | grep ERROR           # Find errors

# Cleanup
docker system prune -a                                  # Remove unused resources
docker volume prune                                     # Remove unused volumes
docker compose down -v --remove-orphans                 # Complete cleanup
```

---

## Environment Variables

Set in `.env` file or export before running:

```bash
# Database
POSTGRES_USER=healthdata
POSTGRES_PASSWORD=healthdata_password
POSTGRES_DB=healthdata_db

# Redis
REDIS_PASSWORD=redis_password

# JWT
JWT_SECRET=<64-character-hex-string>
JWT_EXPIRATION_MS=900000

# Kafka
KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# Service ports (if overriding defaults)
PATIENT_SERVICE_PORT=8084
QUALITY_MEASURE_PORT=8087
FHIR_SERVICE_PORT=8085
CARE_GAP_SERVICE_PORT=8086

# Environment
ENVIRONMENT=development
LOG_LEVEL=INFO

# Gateway
GATEWAY_ENABLE_AUTH=true
GATEWAY_AUTH_DEV_MODE=true  # Set to false in production
```

---

## Related Documentation

- **Docker Deployment Reference**: `docker/DOCKER_DEPLOY_QUICK_REFERENCE.md`
- **Local Testing Guide**: `docker/LOCAL_TESTING_GUIDE.md`
- **Production Security**: `docs/PRODUCTION_SECURITY_GUIDE.md`
- **Deployment Runbook**: `docs/DEPLOYMENT_RUNBOOK.md`
- **Command Reference**: `backend/docs/COMMAND_REFERENCE.md`

---

## Performance Optimization

### For Development (Fastest Iteration)

```bash
# Use minimal setup
docker compose --profile light up -d

# Build services locally
cd backend
./gradlew :modules:services:patient-service:bootJar

# Run service locally (faster than Docker)
java -jar modules/services/patient-service/build/libs/patient-service-*.jar
```

### For CI/CD (Minimal Overhead)

```bash
# Use local.yml (no services, just infrastructure)
docker compose -f docker-compose.local.yml up -d

# Services connect to infrastructure
# No container overhead for application code
```

### For Demos (All Features, Acceptable Startup)

```bash
# Use demo profile
docker compose --profile demo up -d

# Wait for health checks
while ! docker compose ps | grep -q "healthy"; do sleep 2; done
```

---

## Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| Port already in use | Another service on port | Kill process or map to different port |
| Out of memory | Too many services | Use lighter profile or docker-compose.local.yml |
| Service won't start | Migration failure | Check logs, reset with `docker compose down -v` |
| Database locked | Liquibase lock | DELETE FROM databasechangeloglock |
| Slow performance | Docker resource limit | Increase Docker memory in settings |
| Network issues | Container can't reach other | Ensure all on same network (healthdata-network) |

---

_Last Updated: January 19, 2026_
_Version: 1.0 - Initial Docker Compose Usage Guide_
