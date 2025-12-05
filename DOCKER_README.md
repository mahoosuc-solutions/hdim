# HealthData-in-Motion - Docker Deployment Guide

**Version**: 1.0.0
**Date**: 2025-10-30
**Status**: ✅ Production-Ready

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Quick Start](#quick-start)
4. [Architecture](#architecture)
5. [Services](#services)
6. [Configuration](#configuration)
7. [Usage](#usage)
8. [Monitoring](#monitoring)
9. [Performance](#performance)
10. [Troubleshooting](#troubleshooting)
11. [Production Deployment](#production-deployment)

---

## 🎯 Overview

Complete Docker Compose setup for the HealthData-in-Motion platform, including:
- **CQL Engine Service** - HEDIS quality measure evaluation (52 measures)
- **PostgreSQL** - Database for CQL data and FHIR resources
- **Redis** - Caching layer for performance optimization
- **Kafka + Zookeeper** - Event streaming for audit trails
- **HAPI FHIR Server** - Mock FHIR server for development
- **Prometheus** - Metrics collection (optional)
- **Grafana** - Monitoring dashboards (optional)

---

## ✅ Prerequisites

### Required Software:
- **Docker**: 20.10+ ([Install Docker](https://docs.docker.com/get-docker/))
- **Docker Compose**: 2.0+ ([Install Docker Compose](https://docs.docker.com/compose/install/))
- **Git**: For cloning the repository

### System Requirements:
- **CPU**: 4+ cores recommended
- **RAM**: 8GB minimum, 16GB recommended
- **Disk**: 20GB free space
- **OS**: Linux, macOS, Windows (with WSL2)

### Verify Installation:
```bash
docker --version
# Docker version 20.10.0 or higher

docker-compose --version
# Docker Compose version v2.0.0 or higher
```

---

## 🚀 Quick Start

### 1. Clone the Repository
```bash
git clone https://github.com/your-org/healthdata-in-motion.git
cd healthdata-in-motion
```

### 2. Configure Environment
```bash
# Copy example environment file
cp .env.example .env

# Edit .env with your settings (optional)
nano .env
```

### 3. Start the Stack
```bash
# Start all core services
docker-compose up -d

# View logs
docker-compose logs -f
```

### 4. Verify Services
```bash
# Check service health
docker-compose ps

# All services should show "healthy" status
```

### 5. Access Services
| Service | URL | Credentials |
|---------|-----|-------------|
| **CQL Engine API** | http://localhost:8081 | N/A |
| **Swagger UI** | http://localhost:8081/swagger-ui.html | N/A |
| **FHIR Server** | http://localhost:8080/fhir | N/A |
| **PostgreSQL** | localhost:5435 | healthdata / dev_password |
| **Redis** | localhost:6379 | N/A |
| **Kafka** | localhost:9092 | N/A |

---

## 🏗️ Architecture

### Service Dependencies

```
┌─────────────────────────────────────────────────────────────┐
│                   HealthData-in-Motion Stack                │
└─────────────────────────────────────────────────────────────┘
                              │
                ┌─────────────┴─────────────┐
                │                           │
        ┌───────▼───────┐          ┌───────▼────────┐
        │ CQL Engine    │          │  FHIR Service  │
        │   Service     │◄─────────┤   (HAPI)       │
        │   Port: 8081  │  queries │  Port: 8080    │
        └───┬───┬───┬───┘          └────────┬───────┘
            │   │   │                       │
    ┌───────┘   │   └───────┐              │
    │           │           │               │
┌───▼───┐   ┌──▼───┐   ┌───▼────┐     ┌───▼─────┐
│ Postgre│   │Redis │   │ Kafka  │     │Postgres │
│SQL     │   │Cache │   │Events  │     │(FHIR DB)│
│Port:   │   │Port: │   │Port:   │     │         │
│5435    │   │6379  │   │9092    │     │         │
└────────┘   └──────┘   └────────┘     └─────────┘
                           │
                      ┌────▼─────┐
                      │Zookeeper │
                      │Port: 2181│
                      └──────────┘
```

### Network Configuration
- **Network**: `healthdata-network` (bridge)
- **Subnet**: 172.20.0.0/16
- **Internal DNS**: Service names resolve automatically

---

## 📦 Services

### 1. PostgreSQL Database
**Image**: `postgres:16-alpine`
**Port**: 5435 → 5432
**Purpose**: Primary database for CQL Engine and FHIR data

**Features**:
- Auto-initialization with schemas
- UUID and full-text search extensions
- Persistent volume storage
- Health checks enabled

**Databases Created**:
- `healthdata_cql` - CQL Engine data
- `healthdata_fhir` - FHIR resources

### 2. Redis Cache
**Image**: `redis:7-alpine`
**Port**: 6379
**Purpose**: Caching layer for measure evaluation results

**Configuration**:
- **Max Memory**: 512MB
- **Eviction Policy**: allkeys-lru
- **Persistence**: AOF + RDB snapshots
- **TTL**: 24 hours (configurable)

### 3. Apache Kafka
**Image**: `confluentinc/cp-kafka:7.5.0`
**Ports**: 9092 (internal), 9093 (external)
**Purpose**: Event streaming for audit trail

**Topics Auto-Created**:
- `measure-evaluations` - Evaluation events
- `audit-events` - Audit trail
- `care-gaps-identified` - Care gap notifications

### 4. Zookeeper
**Image**: `confluentinc/cp-zookeeper:7.5.0`
**Port**: 2181
**Purpose**: Kafka cluster coordination

### 5. CQL Engine Service
**Image**: `healthdata/cql-engine-service:1.0.0`
**Port**: 8081
**Purpose**: HEDIS quality measure evaluation

**Features**:
- 52 HEDIS measures (100% coverage)
- REST API with 6 endpoints
- Swagger/OpenAPI documentation
- Async/parallel evaluation
- Multi-tenant support
- Redis caching

**Resource Limits**:
- CPU: 2.0 cores (max)
- Memory: 2GB (max)
- JVM: 75% of container memory

### 6. HAPI FHIR Server (Mock)
**Image**: `hapiproject/hapi:latest`
**Port**: 8080
**Purpose**: Mock FHIR server for development

**Configuration**:
- FHIR Version: R4
- REST Hook subscriptions enabled
- CORS enabled
- Stores data in PostgreSQL

### 7. Prometheus (Optional)
**Image**: `prom/prometheus:latest`
**Port**: 9090
**Purpose**: Metrics collection and alerting

**Profile**: `monitoring`

### 8. Grafana (Optional)
**Image**: `grafana/grafana:latest`
**Port**: 3000
**Purpose**: Monitoring dashboards

**Profile**: `monitoring`
**Default Credentials**: admin / admin

---

## ⚙️ Configuration

### Environment Variables

Create a `.env` file from `.env.example`:

```bash
cp .env.example .env
```

**Key Variables**:

```bash
# PostgreSQL
POSTGRES_DB=healthdata_cql
POSTGRES_USER=healthdata
POSTGRES_PASSWORD=your-secure-password

# CQL Engine
CQL_ENGINE_PORT=8081
MEASURE_EVALUATION_MAX_POOL_SIZE=50

# FHIR Service
FHIR_SERVICE_URL=http://fhir-service-mock:8080/fhir

# Logging
LOGGING_LEVEL_COM_HEALTHDATA_CQL=DEBUG
```

### Spring Profiles

The CQL Engine Service uses the `docker` profile by default:
- Configuration: `application-docker.yml`
- Auto-configured for container networking
- Optimized for Docker environment

---

## 💻 Usage

### Starting Services

```bash
# Start all core services
docker-compose up -d

# Start with monitoring (Prometheus + Grafana)
docker-compose --profile monitoring up -d

# Start specific service
docker-compose up -d cql-engine-service

# Rebuild and start
docker-compose up -d --build
```

### Stopping Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: Deletes all data)
docker-compose down -v

# Stop specific service
docker-compose stop cql-engine-service
```

### Viewing Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f cql-engine-service

# Last 100 lines
docker-compose logs --tail=100 cql-engine-service

# Since 10 minutes ago
docker-compose logs --since 10m cql-engine-service
```

### Scaling Services

```bash
# Scale CQL Engine to 3 instances
docker-compose up -d --scale cql-engine-service=3

# Note: You'll need a load balancer for production scaling
```

### Accessing Containers

```bash
# Execute command in container
docker-compose exec cql-engine-service bash

# Access PostgreSQL
docker-compose exec postgres psql -U healthdata -d healthdata_cql

# Access Redis CLI
docker-compose exec redis redis-cli
```

---

## 📊 Monitoring

### Health Checks

All services have health checks configured. Check status:

```bash
# View service health
docker-compose ps

# Check CQL Engine health endpoint
curl http://localhost:8081/actuator/health

# Expected response:
{
  "status": "UP",
  "components": {
    "redis": {"status": "UP"},
    "db": {"status": "UP"},
    "kafka": {"status": "UP"}
  }
}
```

### Metrics (with Monitoring Profile)

1. **Prometheus**: http://localhost:9090
   - Query: `http_server_requests_seconds_count`
   - Targets: http://localhost:9090/targets

2. **Grafana**: http://localhost:3000
   - Username: `admin`
   - Password: `admin`
   - Add Prometheus data source: `http://prometheus:9090`

### Key Metrics to Monitor

```promql
# Measure evaluation duration
measure_evaluation_duration_seconds

# Total evaluations
measure_evaluation_total

# Cache hit rate
measure_cache_hit_rate

# Error count
measure_error_total

# HTTP response times
http_server_requests_seconds
```

---

## ⚡ Performance

### Performance Metrics

**Single Instance** (2 cores, 2GB RAM):
- **Throughput**: 200-400 requests/second (with caching)
- **Latency (P95)**: <300ms for single measure evaluation
- **Cache Hit Rate**: 85-95% (steady state)
- **Concurrent Users**: 100-500 per instance

**Full Dashboard** (52 HEDIS Measures):
- **Parallel Evaluation**: ~2 seconds with caching
- **Sequential**: ~18 seconds without parallelization
- **Memory per Patient**: ~50KB (all measures cached)

📊 **For detailed performance documentation, see**: [docs/PERFORMANCE_GUIDE.md](./docs/PERFORMANCE_GUIDE.md)

### Performance Optimization

#### 1. Enable Redis Caching

Redis caching provides **3-5x performance improvement**:

```bash
# Verify Redis is running
docker-compose ps redis

# Check cache hit rate
docker-compose exec redis redis-cli INFO stats | grep hit_rate

# Monitor cache keys
docker-compose exec redis redis-cli DBSIZE
```

**Cache Configuration** (`.env`):
```bash
SPRING_CACHE_TYPE=redis
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379
SPRING_CACHE_REDIS_TIME_TO_LIVE=86400  # 24 hours
```

#### 2. Thread Pool Tuning

Adjust parallel evaluation capacity:

```bash
# Light workload (< 50 req/s)
MEASURE_EVALUATION_CORE_POOL_SIZE=5
MEASURE_EVALUATION_MAX_POOL_SIZE=20

# Medium workload (50-200 req/s) - DEFAULT
MEASURE_EVALUATION_CORE_POOL_SIZE=10
MEASURE_EVALUATION_MAX_POOL_SIZE=50

# Heavy workload (200-500 req/s)
MEASURE_EVALUATION_CORE_POOL_SIZE=20
MEASURE_EVALUATION_MAX_POOL_SIZE=100
```

Apply changes:
```bash
echo "MEASURE_EVALUATION_MAX_POOL_SIZE=100" >> .env
docker-compose restart cql-engine-service
```

#### 3. JVM Optimization

**Production JVM Settings** (`.env`):
```bash
JAVA_OPTS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:InitialRAMPercentage=50.0 \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+AlwaysPreTouch \
  -XX:+DisableExplicitGC"
```

**Memory Recommendations**:
| Container Memory | Heap Size | Use Case |
|------------------|-----------|----------|
| 1GB | 750MB | Development |
| 2GB | 1.5GB | Production |
| 4GB | 3GB | High Load |

#### 4. Database Connection Pool

**HikariCP Settings** (`.env`):
```bash
# Light load
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=10
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=2

# Medium load - DEFAULT
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=20
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=5

# Heavy load
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=40
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=10
```

#### 5. Horizontal Scaling

Scale to multiple instances for higher throughput:

```bash
# Docker Compose
docker-compose up -d --scale cql-engine-service=3

# Verify scaling
docker-compose ps | grep cql-engine

# With load balancer
docker-compose up -d traefik
```

**Scaling Guidelines**:
| Target Load | Instances | Total Throughput |
|-------------|-----------|------------------|
| < 200 req/s | 1 | 400 req/s |
| 200-800 req/s | 2-3 | 1,200 req/s |
| 800-2000 req/s | 4-6 | 2,400 req/s |
| 2000+ req/s | 8-10 | 4,000 req/s |

### Performance Monitoring

#### View Real-Time Metrics

```bash
# Application metrics
curl http://localhost:8081/actuator/metrics | jq '.names'

# Request latency
curl http://localhost:8081/actuator/metrics/http.server.requests | jq '.'

# Thread pool utilization
curl http://localhost:8081/actuator/metrics/executor.active | jq '.'

# Cache statistics
curl http://localhost:8081/actuator/metrics/cache.gets | jq '.'

# JVM memory
curl http://localhost:8081/actuator/metrics/jvm.memory.used | jq '.'
```

#### Prometheus Queries

```promql
# Request rate (req/s)
rate(http_server_requests_seconds_count{job="cql-engine"}[5m])

# P95 Latency
histogram_quantile(0.95,
  rate(http_server_requests_seconds_bucket{job="cql-engine"}[5m]))

# Cache hit rate
rate(cache_gets_total{result="hit"}[5m]) /
rate(cache_gets_total[5m])

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m]) /
rate(http_server_requests_seconds_count[5m])
```

#### Grafana Dashboards

Access Grafana at `http://localhost:3000` (if monitoring enabled):
- **Service Overview**: Request rate, latency, errors
- **Resource Usage**: CPU, memory, GC metrics
- **Cache Performance**: Hit rate, misses, evictions
- **Business Metrics**: Measure evaluations, care gaps

### Performance Testing

#### Run Load Tests

```bash
# Install Apache JMeter
wget https://downloads.apache.org//jmeter/binaries/apache-jmeter-5.6.tgz
tar -xzf apache-jmeter-5.6.tgz

# Run performance test
cd performance-tests
jmeter -n -t measure-evaluation.jmx -l results.jtl -e -o report/

# View report
open report/index.html
```

#### Stress Test

```bash
# Using Apache Bench (quick test)
ab -n 1000 -c 50 \
  -H "X-Tenant-ID: tenant-1" \
  http://localhost:8081/api/v1/measures/BCS/evaluate/patient-123

# Using hey (modern alternative)
hey -n 1000 -c 50 \
  -H "X-Tenant-ID: tenant-1" \
  http://localhost:8081/api/v1/measures/BCS/evaluate/patient-123
```

### Performance Troubleshooting

#### Issue: High Latency

**Symptoms**: P95 > 500ms, slow responses

**Quick Checks**:
```bash
# Check cache hit rate
docker-compose exec redis redis-cli INFO stats | grep hit_rate

# Check active threads
curl http://localhost:8081/actuator/metrics/executor.active

# Check database connections
curl http://localhost:8081/actuator/metrics/hikaricp.connections.active
```

**Solutions**:
1. **Warm cache**: `./scripts/warm-cache.sh`
2. **Increase thread pool**: Set `MEASURE_EVALUATION_MAX_POOL_SIZE=100`
3. **Scale horizontally**: `docker-compose up -d --scale cql-engine-service=3`

#### Issue: Low Throughput

**Symptoms**: < 150 req/s, high queue depth

**Quick Checks**:
```bash
# Check thread pool queue
curl http://localhost:8081/actuator/metrics/executor.queue.size

# Check resource usage
docker stats cql-engine-service --no-stream
```

**Solutions**:
1. **Increase thread pool**: `MEASURE_EVALUATION_MAX_POOL_SIZE=100`
2. **Increase connection pool**: `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=40`
3. **Add more instances**: Scale horizontally

#### Issue: Memory Issues

**Symptoms**: OOM errors, high GC time

**Quick Checks**:
```bash
# Check memory usage
curl http://localhost:8081/actuator/metrics/jvm.memory.used

# Check GC activity
curl http://localhost:8081/actuator/metrics/jvm.gc.pause
```

**Solutions**:
1. **Increase container memory**: Edit `docker-compose.yml`, set `memory: 4G`
2. **Optimize cache size**: Set Redis `maxmemory 1gb` and `maxmemory-policy allkeys-lru`
3. **Enable heap dumps**: `JAVA_OPTS=-XX:+HeapDumpOnOutOfMemoryError`

### Performance Resources

- **Complete Guide**: [docs/PERFORMANCE_GUIDE.md](./docs/PERFORMANCE_GUIDE.md)
- **Operations Runbook**: [docs/runbooks/PERFORMANCE_RUNBOOK.md](./docs/runbooks/PERFORMANCE_RUNBOOK.md)
- **Troubleshooting**: [docs/DOCKER_TROUBLESHOOTING.md](./docs/DOCKER_TROUBLESHOOTING.md)
- **Service Docs**: [docs/CQL_ENGINE_SERVICE_COMPLETE.md](./docs/CQL_ENGINE_SERVICE_COMPLETE.md)

---

## 🔧 Troubleshooting

### Common Issues

#### 1. Service Won't Start
```bash
# Check logs
docker-compose logs cql-engine-service

# Common causes:
# - Database not ready → Wait 30s for PostgreSQL to initialize
# - Port already in use → Change port in docker-compose.yml
# - Out of memory → Increase Docker memory limit
```

#### 2. Cannot Connect to Database
```bash
# Verify PostgreSQL is healthy
docker-compose ps postgres

# Check database logs
docker-compose logs postgres

# Test connection
docker-compose exec postgres psql -U healthdata -d healthdata_cql -c "\l"
```

#### 3. Redis Connection Failed
```bash
# Check Redis status
docker-compose exec redis redis-cli ping
# Expected: PONG

# Restart Redis
docker-compose restart redis
```

#### 4. Kafka Not Ready
```bash
# Kafka takes 40-60 seconds to start
# Check health
docker-compose logs kafka

# List topics
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

#### 5. Out of Memory
```bash
# Increase Docker memory
# Docker Desktop → Settings → Resources → Memory → 8GB+

# Or reduce service limits in docker-compose.yml
```

### Clean Restart

```bash
# Stop all services
docker-compose down

# Remove volumes (WARNING: Deletes data)
docker-compose down -v

# Remove images
docker-compose down --rmi all

# Rebuild and start
docker-compose up -d --build
```

### View Resource Usage

```bash
# All containers
docker stats

# Specific container
docker stats healthdata-cql-engine
```

---

## 🚀 Production Deployment

### Security Hardening

1. **Change Default Passwords**:
```bash
# Update .env file
POSTGRES_PASSWORD=<strong-random-password>
REDIS_PASSWORD=<strong-random-password>
```

2. **Enable TLS/SSL**:
- Configure PostgreSQL SSL
- Use HTTPS for API endpoints
- Enable Kafka SASL authentication

3. **Restrict Network Access**:
```yaml
# In docker-compose.yml, remove port mappings for internal services
# Only expose API gateway publicly
```

4. **Use Secrets Management**:
```bash
# Docker Swarm secrets
echo "my-password" | docker secret create db_password -

# Or use external secret manager (AWS Secrets Manager, Vault)
```

### High Availability

1. **Database Replication**:
- Configure PostgreSQL streaming replication
- Use pgpool-II for load balancing

2. **Redis Cluster**:
- Deploy Redis Sentinel for failover
- Or use Redis Cluster mode

3. **Kafka Cluster**:
- Deploy 3+ Kafka brokers
- Set replication factor to 3

4. **Service Scaling**:
```bash
# Deploy with Docker Swarm
docker stack deploy -c docker-compose.yml healthdata

# Or use Kubernetes (recommended for production)
```

### Kubernetes Deployment

See [CQL_ENGINE_SERVICE_COMPLETE.md](./docs/CQL_ENGINE_SERVICE_COMPLETE.md) for Kubernetes manifests.

### Backup Strategy

```bash
# PostgreSQL backup
docker-compose exec postgres pg_dump -U healthdata healthdata_cql > backup.sql

# Restore
docker-compose exec -T postgres psql -U healthdata -d healthdata_cql < backup.sql

# Automated backups with cron
0 2 * * * docker-compose exec postgres pg_dump -U healthdata healthdata_cql | gzip > /backups/db-$(date +\%Y\%m\%d).sql.gz
```

---

## 📚 Additional Resources

- [CQL Engine Service Documentation](./docs/CQL_ENGINE_SERVICE_COMPLETE.md)
- [Integration Tests](./backend/modules/services/cql-engine-service/INTEGRATION_TESTS_SUMMARY.md)
- [HEDIS Measures Guide](./docs/PHASE_3_HEDIS_52_MEASURES_100PCT_COMPLETE.md)
- [Docker Compose Reference](https://docs.docker.com/compose/)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)

---

## 🤝 Support

For issues or questions:
1. Check [Troubleshooting](#troubleshooting) section
2. Review service logs: `docker-compose logs`
3. Open an issue on GitHub
4. Contact: support@healthdata-in-motion.com

---

**Generated**: 2025-10-30
**Version**: 1.0.0
**Docker Compose Version**: 3.9
**Status**: ✅ Production-Ready
