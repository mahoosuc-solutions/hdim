# Container & Service Validation Guide

**Purpose:** Comprehensive guide for validating all containers and services before testing  
**Last Updated:** January 2025

---

## Quick Start

### Basic Validation
```bash
# Run comprehensive validation
./scripts/validate-containers.sh

# Generate detailed report
./scripts/test-readiness-report.sh --output test-readiness.md
```

### Quick Health Check
```bash
# Check specific service
./scripts/health-check.sh cql-engine

# Check all services
./scripts/health-check.sh
```

---

## Validation Scripts

### 1. `validate-containers.sh` - Comprehensive Validation

**Purpose:** Validates all containers, services, ports, databases, and connectivity

**Usage:**
```bash
# Basic validation
./scripts/validate-containers.sh

# Verbose output
./scripts/validate-containers.sh --verbose

# Attempt automatic fixes
./scripts/validate-containers.sh --fix
```

**What it checks:**
- ✅ Docker environment (Docker daemon, Docker Compose)
- ✅ Infrastructure containers (PostgreSQL, Redis, Kafka, Zookeeper, Jaeger)
- ✅ Database connectivity (PostgreSQL queries)
- ✅ Redis connectivity (PING test)
- ✅ Kafka connectivity (broker API)
- ✅ Backend services (HTTP health endpoints)
- ✅ Frontend services (Clinical Portal)
- ✅ Port availability
- ✅ Service dependencies

**Output:**
- ✓ Green checkmarks for passing checks
- ⚠ Yellow warnings for optional/unavailable services
- ✗ Red X for failed critical checks
- Summary with pass/fail counts

### 2. `test-readiness-report.sh` - Detailed Report

**Purpose:** Generates a comprehensive markdown report of system status

**Usage:**
```bash
# Generate report to console
./scripts/test-readiness-report.sh

# Save report to file
./scripts/test-readiness-report.sh --output reports/test-readiness-$(date +%Y%m%d).md
```

**Report includes:**
- Executive summary with status
- Detailed validation results
- Container status table
- Service health endpoint status
- Next steps and recommendations

### 3. `health-check.sh` - Quick Health Check

**Purpose:** Fast health check for individual services or all services

**Usage:**
```bash
# Check all services
./scripts/health-check.sh

# Check specific service
./scripts/health-check.sh cql-engine
./scripts/health-check.sh postgres
./scripts/health-check.sh kafka
```

**Available services:**
- `clinical-portal` or `frontend`
- `gateway`
- `cql-engine` or `cql`
- `quality-measure` or `quality`
- `postgres` or `db`
- `redis`
- `kafka`
- `fhir`
- `consent`
- `event-processing` or `events`
- `patient`
- `care-gap`
- `prometheus`
- `grafana`

---

## Validation Checklist

### Pre-Testing Checklist

Before running tests, ensure:

- [ ] **Docker Environment**
  - [ ] Docker daemon is running
  - [ ] Docker Compose is available
  - [ ] Sufficient resources (RAM, disk space)

- [ ] **Infrastructure Services**
  - [ ] PostgreSQL container is healthy
  - [ ] Redis container is healthy
  - [ ] Kafka container is healthy
  - [ ] Zookeeper container is healthy

- [ ] **Database Connectivity**
  - [ ] PostgreSQL accepts connections
  - [ ] Database queries work
  - [ ] Migrations are applied

- [ ] **Backend Services**
  - [ ] Gateway Service (port 8080)
  - [ ] CQL Engine (port 8081)
  - [ ] Consent Service (port 8082)
  - [ ] Event Processing (port 8083)
  - [ ] Patient Service (port 8084)
  - [ ] FHIR Service (port 8085)
  - [ ] Care Gap Service (port 8086)
  - [ ] Quality Measure (port 8087)

- [ ] **Frontend Services**
  - [ ] Clinical Portal (port 4200)

- [ ] **Ports Available**
  - [ ] No port conflicts
  - [ ] All required ports are accessible

---

## Common Issues & Solutions

### Issue: Docker Daemon Not Running

**Symptoms:**
```
✗ Docker Daemon: Not running
```

**Solutions:**
```bash
# Linux
sudo systemctl start docker

# WSL
sudo service docker start

# Mac
# Open Docker Desktop application
```

### Issue: Container Not Found

**Symptoms:**
```
✗ PostgreSQL: Container not found
```

**Solutions:**
```bash
# Start containers
docker compose up -d

# Check which containers are running
docker ps

# Check all containers (including stopped)
docker ps -a
```

### Issue: Service Unhealthy

**Symptoms:**
```
✗ CQL Engine: Unhealthy (HTTP 503)
```

**Solutions:**
```bash
# Check service logs
docker logs healthdata-cql-engine-service

# Restart service
docker compose restart cql-engine-service

# Check dependencies
docker compose ps
```

### Issue: Port Already in Use

**Symptoms:**
```
✗ Gateway (Port 8080): Not in use
⚠ Port 8080 is in use
```

**Solutions:**
```bash
# Find process using port
lsof -i :8080
# or
netstat -tuln | grep 8080

# Stop conflicting service or change port in docker-compose.yml
```

### Issue: Database Connection Failed

**Symptoms:**
```
✗ PostgreSQL (healthdata_db): Not accepting connections
```

**Solutions:**
```bash
# Check PostgreSQL logs
docker logs healthdata-postgres

# Verify database exists
docker exec healthdata-postgres psql -U healthdata -l

# Restart PostgreSQL
docker compose restart postgres
```

### Issue: Kafka Not Accessible

**Symptoms:**
```
✗ Kafka: Broker not accessible
```

**Solutions:**
```bash
# Check Kafka logs
docker logs healthdata-kafka

# Verify Zookeeper is running
docker ps | grep zookeeper

# Restart Kafka
docker compose restart kafka zookeeper
```

---

## Validation Workflow

### 1. Initial Setup Validation
```bash
# Check Docker environment
./scripts/validate-containers.sh

# If issues found, review logs
docker compose logs
```

### 2. Pre-Test Validation
```bash
# Run comprehensive validation
./scripts/validate-containers.sh --verbose

# Generate report
./scripts/test-readiness-report.sh --output test-readiness.md
```

### 3. Continuous Validation
```bash
# Quick health check before each test run
./scripts/health-check.sh

# Monitor specific service
watch -n 5 './scripts/health-check.sh cql-engine'
```

### 4. Post-Test Validation
```bash
# Verify services still healthy after tests
./scripts/validate-containers.sh

# Check for resource leaks
docker stats --no-stream
```

---

## Service Health Endpoints

All backend services expose health endpoints via Spring Boot Actuator:

| Service | Health Endpoint |
|---------|----------------|
| Gateway | `http://localhost:8080/actuator/health` |
| CQL Engine | `http://localhost:8081/cql-engine/actuator/health` |
| Consent | `http://localhost:8082/consent/actuator/health` |
| Event Processing | `http://localhost:8083/events/actuator/health` |
| Patient | `http://localhost:8084/patient/actuator/health` |
| FHIR | `http://localhost:8085/fhir/actuator/health` |
| Care Gap | `http://localhost:8086/care-gap/actuator/health` |
| Quality Measure | `http://localhost:8087/quality-measure/actuator/health` |

**Expected Response:**
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

---

## Container Health Checks

Docker Compose includes health checks for all services:

```yaml
healthcheck:
  test: ["CMD", "wget", "--spider", "-q", "http://localhost:8080/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 5
  start_period: 90s
```

**Check health status:**
```bash
# View health status
docker inspect --format='{{.State.Health.Status}}' healthdata-cql-engine-service

# View health check history
docker inspect --format='{{json .State.Health}}' healthdata-cql-engine-service | jq
```

---

## Performance Validation

### Resource Usage
```bash
# Check container resource usage
docker stats --no-stream

# Check disk usage
docker system df

# Check network usage
docker network inspect healthdata-network
```

### Load Testing
```bash
# Run load tests (if available)
npm run test:load

# Monitor during load test
watch -n 1 './scripts/health-check.sh'
```

---

## Integration with CI/CD

### GitHub Actions Example
```yaml
- name: Validate Containers
  run: |
    ./scripts/validate-containers.sh
    if [ $? -ne 0 ]; then
      echo "Validation failed"
      exit 1
    fi

- name: Generate Test Readiness Report
  run: |
    ./scripts/test-readiness-report.sh --output test-readiness.md
    cat test-readiness.md
```

---

## Troubleshooting

### View Service Logs
```bash
# All services
docker compose logs

# Specific service
docker compose logs cql-engine-service

# Follow logs
docker compose logs -f cql-engine-service

# Last 100 lines
docker compose logs --tail=100 cql-engine-service
```

### Restart Services
```bash
# Restart specific service
docker compose restart cql-engine-service

# Restart all services
docker compose restart

# Restart with rebuild
docker compose up -d --build
```

### Clean Start
```bash
# Stop all containers
docker compose down

# Remove volumes (⚠️ deletes data)
docker compose down -v

# Start fresh
docker compose up -d
```

---

## Related Documentation

- **[System Architecture](../../architecture/SYSTEM_ARCHITECTURE.md)**: Overall system design
- **[Deployment Guide](../../deployment/)**: Deployment procedures
- **[Service Documentation](../../services/)**: Individual service specs
- **[Testing Guide](./TESTING_GUIDE.md)**: Test execution guide

---

**Last Updated:** January 2025  
**Maintained By:** Platform Engineering Team
