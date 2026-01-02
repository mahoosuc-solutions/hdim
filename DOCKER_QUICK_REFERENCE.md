# HealthData-in-Motion - Docker Quick Reference

**Version**: 1.0.0 | **Date**: 2025-10-30

---

## ⚡ Quick Start

```bash
# 1. Setup
make setup

# 2. Start services
make up

# 3. Check health
make health

# 4. View logs
make logs

# 5. Stop services
make down
```

---

## 🌐 Service URLs

| Service | URL | Port |
|---------|-----|------|
| **CQL Engine API** | http://localhost:8081 | 8081 |
| **Swagger UI** | http://localhost:8081/swagger-ui.html | 8081 |
| **FHIR Server** | http://localhost:8080/fhir | 8080 |
| **PostgreSQL** | localhost:5435 | 5435 |
| **Redis** | localhost:6379 | 6379 |
| **Kafka** | localhost:9092 | 9092 |
| **Prometheus** | http://localhost:9090 | 9090 |
| **Grafana** | http://localhost:3000 | 3000 |

---

## 🔧 Essential Commands

### Service Management
```bash
make up                  # Start all services
make up-monitoring       # Start with Prometheus + Grafana
make down                # Stop all services
make restart             # Restart all services
make restart-cql         # Restart CQL Engine only
make rebuild             # Rebuild and restart
```

### Monitoring
```bash
make logs                # View all logs
make logs-cql            # View CQL Engine logs
make ps                  # List containers
make status              # Show service status
make health              # Check health of all services
```

### Database
```bash
make db-connect          # Connect to PostgreSQL
make db-backup           # Backup database
make db-restore FILE=x   # Restore from backup
make shell-db            # Shell into PostgreSQL container
```

### Testing
```bash
make test-api            # Test CQL Engine API
make test-fhir           # Test FHIR Server
```

### Development
```bash
make shell-cql           # Shell into CQL Engine container
make shell-redis         # Redis CLI
make rebuild-cql         # Rebuild CQL Engine only
```

### Cleanup
```bash
make clean               # Remove containers and volumes
make clean-logs          # Remove log files
make down-volumes        # Stop and remove volumes (DESTRUCTIVE!)
```

---

## 📋 Common Tasks

### Check if Services are Running
```bash
make ps
# or
docker-compose ps
```

### View Real-time Logs
```bash
# All services
make logs

# Specific service
docker-compose logs -f cql-engine-service
docker-compose logs -f postgres
docker-compose logs -f kafka
```

### Test API Endpoints
```bash
# Health check
curl http://localhost:8081/actuator/health | jq '.'

# List all 52 HEDIS measures
curl http://localhost:8081/api/v1/measures | jq '. | length'

# Get measure metadata
curl http://localhost:8081/api/v1/measures/BCS | jq '.'

# Evaluate measure (requires FHIR data)
curl -H "X-Tenant-ID: tenant-1" \
     http://localhost:8081/api/v1/measures/BCS/evaluate/patient-123 | jq '.'
```

### Database Operations
```bash
# Connect to database
make db-connect
# Or manually:
docker-compose exec postgres psql -U healthdata -d healthdata_cql

# Run SQL query
docker-compose exec postgres psql -U healthdata -d healthdata_cql \
    -c "SELECT COUNT(*) FROM cql_library;"

# Backup
make db-backup

# Restore
make db-restore FILE=backups/backup.sql
```

### Check Resource Usage
```bash
# All containers
docker stats

# Specific container
docker stats healthdata-cql-engine
```

---

## 🚨 Troubleshooting

### Service Won't Start
```bash
# Check logs
make logs-cql

# Check health
make health

# Restart service
docker-compose restart cql-engine-service
```

### Port Already in Use
```bash
# Find process using port 8081
lsof -i :8081

# Kill process
kill -9 <PID>

# Or change port in docker-compose.yml
```

### Database Connection Failed
```bash
# Check PostgreSQL is running
docker-compose ps postgres

# Check health
docker-compose exec postgres pg_isready -U healthdata

# View logs
make logs-db
```

### Out of Memory
```bash
# Check Docker memory limit
docker system info | grep Memory

# Increase in Docker Desktop:
# Settings → Resources → Memory → 8GB+

# Check container memory
docker stats
```

### Clean Restart
```bash
# Stop everything
make down

# Remove volumes (DESTRUCTIVE!)
make down-volumes

# Rebuild and start
make rebuild
```

---

## 📊 Health Checks

### All Services
```bash
make health
```

### Individual Services
```bash
# CQL Engine
curl http://localhost:8081/actuator/health

# PostgreSQL
docker-compose exec postgres pg_isready -U healthdata

# Redis
docker-compose exec redis redis-cli ping

# Kafka
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

---

## 🔐 Default Credentials

| Service | Username | Password | Notes |
|---------|----------|----------|-------|
| PostgreSQL | healthdata | dev_password | Change in .env for production |
| Grafana | admin | admin | Change on first login |
| Redis | - | (none) | Set REDIS_PASSWORD in .env |

---

## 📦 Docker Compose Profiles

```bash
# Core services only (default)
docker-compose up -d

# With monitoring
docker-compose --profile monitoring up -d

# Or set in .env:
# COMPOSE_PROFILES=monitoring
```

---

## 🎯 API Endpoints

### CQL Engine Service

```bash
# List all measures (52)
GET http://localhost:8081/api/v1/measures

# Get measure metadata
GET http://localhost:8081/api/v1/measures/{measureId}

# Evaluate single measure
GET http://localhost:8081/api/v1/measures/{measureId}/evaluate/{patientId}
Headers: X-Tenant-ID: {tenantId}

# Evaluate multiple measures
GET http://localhost:8081/api/v1/measures/evaluate/{patientId}?measureIds=BCS,CDC,AMM
Headers: X-Tenant-ID: {tenantId}

# Patient quality dashboard (all 52 measures)
GET http://localhost:8081/api/v1/measures/dashboard/{patientId}
Headers: X-Tenant-ID: {tenantId}

# Get patient care gaps
GET http://localhost:8081/api/v1/measures/care-gaps/{patientId}
Headers: X-Tenant-ID: {tenantId}
```

### Swagger UI
```
http://localhost:8081/swagger-ui.html
```

---

## 🔢 Service Versions

| Service | Version | Image |
|---------|---------|-------|
| CQL Engine | 1.0.0 | healthdata/cql-engine-service:1.0.0 |
| PostgreSQL | 16 | postgres:16-alpine |
| Redis | 7 | redis:7-alpine |
| Kafka | 7.5.0 | confluentinc/cp-kafka:7.5.0 |
| Zookeeper | 7.5.0 | confluentinc/cp-zookeeper:7.5.0 |
| HAPI FHIR | latest | hapiproject/hapi:latest |
| Prometheus | latest | prom/prometheus:latest |
| Grafana | latest | grafana/grafana:latest |

---

## 💾 Data Persistence

### Volumes
```bash
# List volumes
docker volume ls | grep healthdata

# Inspect volume
docker volume inspect healthdata-in-motion_postgres_data

# Remove all volumes (DESTRUCTIVE!)
docker-compose down -v
```

### Backup Locations
- Database backups: `./backups/`
- Application logs: `./logs/cql-engine/`

---

## 🎨 Monitoring Dashboards

### Prometheus
- URL: http://localhost:9090
- Targets: http://localhost:9090/targets
- Graph: http://localhost:9090/graph

### Grafana
- URL: http://localhost:3000
- Default: admin / admin
- Add data source: http://prometheus:9090

**Key Metrics:**
```promql
# Measure evaluation rate
rate(measure_evaluation_total[5m])

# Response time (P95)
histogram_quantile(0.95, measure_evaluation_duration_seconds)

# Cache hit rate
measure_cache_hit_rate

# Error rate
rate(measure_error_total[5m])
```

---

## 📝 Environment Variables

Edit `.env` file:

```bash
# PostgreSQL
POSTGRES_PASSWORD=your-password

# CQL Engine
MEASURE_EVALUATION_MAX_POOL_SIZE=50
LOGGING_LEVEL_COM_HEALTHDATA_CQL=DEBUG

# FHIR
FHIR_SERVICE_URL=http://fhir-service-mock:8080/fhir

# JVM
JAVA_OPTS=-XX:MaxRAMPercentage=75.0
```

---

## 🔄 Update Services

```bash
# Pull latest images
docker-compose pull

# Rebuild specific service
docker-compose build cql-engine-service

# Update and restart
docker-compose up -d --build
```

---

## 📚 Additional Documentation

- **Full Guide**: [DOCKER_README.md](./DOCKER_README.md)
- **Deployment Summary**: [DOCKER_DEPLOYMENT_SUMMARY.md](./DOCKER_DEPLOYMENT_SUMMARY.md)
- **CQL Engine Docs**: [CQL_ENGINE_SERVICE_COMPLETE.md](./docs/CQL_ENGINE_SERVICE_COMPLETE.md)
- **Integration Tests**: [INTEGRATION_TESTS_SUMMARY.md](./backend/modules/services/cql-engine-service/INTEGRATION_TESTS_SUMMARY.md)

---

## 🆘 Getting Help

```bash
# Makefile help
make help

# Docker Compose help
docker-compose help

# Service logs
make logs-cql

# Health check
make health

# Resource usage
docker stats
```

---

**Quick Start**: `make setup && make deploy && make health`

**Status**: ✅ Production-Ready | **Version**: 1.0.0

