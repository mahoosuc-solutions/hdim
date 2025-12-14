# HDIM Docker Desktop - Local Testing Guide

## Overview

This guide covers testing the HDIM Healthcare Platform locally using Docker Desktop before deploying to production registries.

## Quick Start

### 1. Build All Services

```bash
# Build all 22 services
docker compose build

# Build specific profile
docker compose build gateway-service fhir-service patient-service

# Build with verbose output
docker compose build --progress=plain
```

### 2. Start Services Locally

```bash
# Start infrastructure only
docker compose up -d postgres redis kafka zookeeper

# Start core clinical services (10 services)
docker compose --profile core up -d

# Start AI services
docker compose --profile ai up -d

# Start all services (22 total)
docker compose --profile full up -d
```

### 3. Verify Services

```bash
# Check running containers
docker compose ps

# Check service health
curl http://localhost:8080/actuator/health  # Gateway
curl http://localhost:8085/actuator/health  # FHIR
curl http://localhost:8084/actuator/health  # Patient

# View logs
docker compose logs -f gateway-service
docker compose logs -f fhir-service

# Check all service health
make health
```

## Service Ports

### Infrastructure
- **PostgreSQL**: 5435
- **Redis**: 6379
- **Zookeeper**: 2181
- **Kafka**: 9092
- **Prometheus**: 9090
- **Grafana**: 3000

### Core Clinical Services (Profile: core)
- **Gateway**: 8080
- **CQL Engine**: 8081
- **Consent**: 8082
- **Event Processing**: 8083
- **Patient**: 8084
- **FHIR**: 8085
- **Care Gap**: 8086
- **Quality Measure**: 8087
- **Event Router**: 8095

### AI Services (Profile: ai)
- **Agent Runtime**: 8088
- **AI Assistant**: 8090
- **Agent Builder**: 8096

### Analytics Services (Profile: analytics)
- **Analytics**: 8092
- **Predictive Analytics**: 8093
- **SDOH**: 8094

### Full Deployment Services (Profile: full)
- **Data Enrichment**: 8089
- **Documentation**: 8091
- **Approval**: 8097
- **Payer Workflows**: 8098
- **CDR Processor**: 8099
- **EHR Connector**: 8100

## Testing Workflows

### Test 1: Basic Health Check

```bash
#!/bin/bash
# test-health.sh

services=(
    "8080:Gateway"
    "8085:FHIR"
    "8084:Patient"
    "8086:CareGap"
    "8087:QualityMeasure"
)

for service in "${services[@]}"; do
    port="${service%%:*}"
    name="${service##*:}"

    status=$(curl -s "http://localhost:$port/actuator/health" | jq -r '.status' 2>/dev/null || echo "DOWN")

    if [ "$status" = "UP" ]; then
        echo "✅ $name (port $port): $status"
    else
        echo "❌ $name (port $port): $status"
    fi
done
```

### Test 2: FHIR API Workflow

```bash
# Create a test patient
curl -X POST http://localhost:8085/fhir/Patient \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "Patient",
    "identifier": [{"system": "http://test.org", "value": "test-001"}],
    "name": [{"family": "Test", "given": ["Patient"]}],
    "gender": "male",
    "birthDate": "1980-01-01"
  }'

# Search for patients
curl http://localhost:8085/fhir/Patient?family=Test

# Get patient by ID
curl http://localhost:8085/fhir/Patient/[id]
```

### Test 3: Quality Measure Evaluation

```bash
# Evaluate quality measures for a patient
curl -X POST http://localhost:8087/api/quality-measures/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "patient-123",
    "measureIds": ["CMS122", "CMS165"],
    "periodStart": "2024-01-01",
    "periodEnd": "2024-12-31"
  }'
```

### Test 4: Care Gap Analysis

```bash
# Get care gaps for a patient
curl http://localhost:8086/api/care-gaps/patient/patient-123

# Close a care gap
curl -X PUT http://localhost:8086/api/care-gaps/gap-456/close \
  -H "Content-Type: application/json" \
  -d '{
    "closedDate": "2024-12-08",
    "closedBy": "provider-789",
    "notes": "Patient completed recommended screening"
  }'
```

### Test 5: Agent Interaction

```bash
# Create an agent session
curl -X POST http://localhost:8088/api/agents/sessions \
  -H "Content-Type: application/json" \
  -d '{
    "agentType": "clinical-decision",
    "patientId": "patient-123",
    "userId": "user-456"
  }'

# Send message to agent
curl -X POST http://localhost:8088/api/agents/sessions/[session-id]/messages \
  -H "Content-Type: application/json" \
  -d '{
    "content": "What are the care gaps for this patient?",
    "role": "user"
  }'
```

## Monitoring and Debugging

### View Logs

```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f gateway-service

# Follow last 100 lines
docker compose logs -f --tail=100 fhir-service

# Search logs
docker compose logs | grep ERROR
docker compose logs | grep "patient-123"
```

### Resource Usage

```bash
# Container stats
docker stats

# Specific service stats
docker stats hdim-master-gateway-service-1

# System-wide resource usage
docker system df
```

### Database Access

```bash
# Connect to PostgreSQL
docker compose exec postgres psql -U healthdata -d healthdata_db

# Run SQL query
docker compose exec postgres psql -U healthdata -d healthdata_db -c "SELECT COUNT(*) FROM patients"

# View tables
docker compose exec postgres psql -U healthdata -d healthdata_db -c "\dt"
```

### Redis Access

```bash
# Connect to Redis CLI
docker compose exec redis redis-cli

# Check keys
docker compose exec redis redis-cli KEYS "*"

# Get value
docker compose exec redis redis-cli GET session:12345
```

### Kafka Inspection

```bash
# List topics
docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# Consume messages
docker compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic fhir-events \
  --from-beginning
```

## Performance Testing

### Load Test with Artillery

```bash
# Install Artillery
npm install -g artillery

# Run FHIR API load test
artillery run load-tests/fhir-api-load-test.yml

# Run full platform load test
artillery run load-tests/full-platform-load-test.yml

# Custom quick test
artillery quick --count 100 --num 10 http://localhost:8085/fhir/Patient
```

### JMeter Test Plan

```bash
# Run JMeter in GUI mode
jmeter -t load-tests/hdim-load-test.jmx

# Run in headless mode
jmeter -n -t load-tests/hdim-load-test.jmx -l results.jtl -e -o report/
```

## Troubleshooting

### Service Won't Start

**Check logs:**
```bash
docker compose logs gateway-service
```

**Common issues:**
1. Port already in use → Change port in docker-compose.yml
2. Database not ready → Wait for postgres to be healthy
3. Memory issues → Increase Docker Desktop memory allocation

**Restart service:**
```bash
docker compose restart gateway-service
docker compose up -d --force-recreate gateway-service
```

### Database Connection Errors

```bash
# Check PostgreSQL is running
docker compose ps postgres

# Verify connection
docker compose exec gateway-service curl http://localhost:5435 || echo "Cannot connect"

# Check database logs
docker compose logs postgres
```

### Out of Memory

```bash
# Check memory usage
docker stats --no-stream

# Increase memory in Docker Desktop settings:
# Settings → Resources → Memory → 8GB+

# Restart Docker Desktop
```

### Kafka Not Working

```bash
# Check Kafka and Zookeeper
docker compose ps zookeeper kafka

# Restart Kafka stack
docker compose restart zookeeper kafka

# Verify Kafka connectivity
docker compose exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092
```

## Image Management

### List Built Images

```bash
# All HDIM images
docker images | grep hdim-master

# Image sizes
docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}" | grep hdim
```

### Clean Up

```bash
# Stop all services
docker compose down

# Remove all containers and volumes
docker compose down -v

# Remove all HDIM images
docker images | grep hdim-master | awk '{print $3}' | xargs docker rmi

# Clean up unused images
docker image prune -a

# Full system cleanup
docker system prune -a --volumes
```

### Export/Import Images

```bash
# Save images to tar
docker save hdim-master-gateway-service:latest | gzip > gateway-service.tar.gz

# Load images from tar
gunzip -c gateway-service.tar.gz | docker load

# Save all HDIM images
docker images | grep hdim-master | awk '{print $1":"$2}' | \
  xargs -I {} docker save {} | gzip > hdim-all-images.tar.gz
```

## Grafana Dashboards

### Access Grafana

1. Open http://localhost:3000
2. Login: `admin` / `admin`
3. Navigate to: Dashboards → HDIM

**Available Dashboards:**
- **System Health**: Service status, JVM metrics, database connections
- **Event Processing**: Event throughput, DLQ activity, circuit breakers
- **Clinical Operations**: Care gaps, quality measures, patient risk

### Custom Queries

**Prometheus queries:**
```promql
# Event processing rate
rate(events_processed_total[5m])

# Service health
up{job=~".*-service"}

# JVM memory usage
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100

# HTTP request rate
rate(http_server_requests_seconds_count[5m])
```

## Development Workflow

### Make Code Changes

```bash
# 1. Make code changes in your IDE

# 2. Rebuild specific service
docker compose build gateway-service

# 3. Restart service with new code
docker compose up -d --force-recreate gateway-service

# 4. View logs
docker compose logs -f gateway-service
```

### Hot Reload (Development Mode)

```bash
# Run service locally with hot reload
cd backend
./gradlew :modules:services:gateway-service:bootRun

# Or use docker compose override for dev
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d
```

## CI/CD Integration

### Test Before Push

```bash
#!/bin/bash
# pre-commit-hook.sh

echo "🏗️  Building all services..."
docker compose build

echo "🚀 Starting services..."
docker compose --profile core up -d

echo "⏳ Waiting for services to be ready..."
sleep 30

echo "🧪 Running health checks..."
./docker/test-health.sh

echo "🧹 Cleaning up..."
docker compose down

echo "✅ All tests passed!"
```

### GitHub Actions Local Testing

```bash
# Install act
brew install act  # macOS
# or download from https://github.com/nektos/act

# Run GitHub Actions locally
act -j build-and-test

# Run specific workflow
act push -W .github/workflows/deploy-docker.yml
```

## Best Practices

### 1. Resource Limits

Set reasonable limits in docker-compose.yml:
```yaml
services:
  gateway-service:
    deploy:
      resources:
        limits:
          cpus: '1'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

### 2. Health Checks

Ensure all services have health checks:
```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 40s
```

### 3. Network Isolation

Use custom networks for security:
```yaml
networks:
  frontend:
    driver: bridge
  backend:
    driver: bridge
    internal: true
```

### 4. Volume Persistence

Use named volumes for data:
```yaml
volumes:
  postgres-data:
  kafka-data:
  redis-data:
```

## Performance Optimization

### Build Cache

```bash
# Build with cache
docker compose build

# Build without cache (clean build)
docker compose build --no-cache

# Parallel builds
docker compose build --parallel
```

### Layer Optimization

Check Dockerfile layer sizes:
```bash
docker history hdim-master-gateway-service:latest
```

### Multi-Stage Optimization

Our Dockerfiles use multi-stage builds for smaller images:
- **Build stage**: Full JDK with Gradle
- **Runtime stage**: JRE only
- **Size reduction**: ~40-60%

## Security Testing

### Vulnerability Scanning

```bash
# Scan image with Trivy
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
  aquasec/trivy image hdim-master-gateway-service:latest

# Scan all images
docker images | grep hdim-master | awk '{print $1":"$2}' | \
  xargs -I {} docker run --rm aquasec/trivy image {}

# Generate report
docker run --rm -v $(pwd):/output \
  aquasec/trivy image --format json \
  --output /output/scan-report.json \
  hdim-master-gateway-service:latest
```

### Container Security

```bash
# Check container privileges
docker inspect hdim-master-gateway-service-1 | jq '.[].HostConfig.Privileged'

# Verify non-root user
docker compose exec gateway-service whoami
# Should output: appuser

# Check SELinux/AppArmor
docker inspect hdim-master-gateway-service-1 | jq '.[].AppArmorProfile'
```

## Next Steps

After local testing succeeds:

1. **Tag for Registry**:
   ```bash
   docker tag hdim-master-gateway-service:latest myregistry/hdim/gateway-service:v1.0.0
   ```

2. **Push to Registry**:
   ```bash
   docker push myregistry/hdim/gateway-service:v1.0.0
   ```

3. **Deploy to Staging**:
   ```bash
   kubectl set image deployment/gateway app=myregistry/hdim/gateway-service:v1.0.0
   ```

4. **Run Integration Tests**:
   ```bash
   cd backend
   ./gradlew integrationTest
   ```

5. **Deploy to Production**:
   ```bash
   ./docker/deploy.sh v1.0.0
   ```

---

**Status**: 🟢 Ready for Local Testing
**Services**: 22 microservices
**Profiles**: core (10), ai (3), analytics (3), full (22)
**Annual ROI**: $60,000 from deployment automation
