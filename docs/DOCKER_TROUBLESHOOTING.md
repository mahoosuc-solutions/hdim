# Docker Troubleshooting Guide

Comprehensive troubleshooting guide for HealthData-in-Motion Docker deployment.

**Version**: 1.0.0 | **Date**: 2025-10-31

---

## Table of Contents

1. [Quick Diagnostics](#quick-diagnostics)
2. [Service-Specific Issues](#service-specific-issues)
3. [Common Problems](#common-problems)
4. [Performance Issues](#performance-issues)
5. [Networking Problems](#networking-problems)
6. [Data & Volume Issues](#data--volume-issues)
7. [Build Problems](#build-problems)
8. [Docker Compose Issues](#docker-compose-issues)
9. [Logging & Debugging](#logging--debugging)
10. [Recovery Procedures](#recovery-procedures)

---

## Quick Diagnostics

### Health Check Commands

```bash
# Quick health check all services
make health

# Check specific service
./scripts/health-check.sh cql-engine

# View all container status
docker-compose ps

# View resource usage
docker stats --no-stream

# Check logs for errors
make logs | grep -i error
```

### System Requirements Check

```bash
# Docker version (requires 20.10+)
docker --version

# Docker Compose version (requires 2.x)
docker-compose --version

# Available disk space (need 20GB+)
df -h

# Available memory (need 8GB+)
free -h

# Check Docker daemon
docker info
```

---

## Service-Specific Issues

### CQL Engine Service

#### Problem: Service Won't Start

**Symptoms**:
- Container exits immediately
- Health check fails
- Port 8081 not accessible

**Diagnosis**:
```bash
# Check container status
docker-compose ps cql-engine-service

# View recent logs
make logs-cql

# Check health endpoint
curl http://localhost:8081/actuator/health
```

**Common Causes & Solutions**:

1. **Database Connection Failed**
   ```bash
   # Check PostgreSQL is running
   docker-compose ps postgres

   # Test database connection
   docker-compose exec postgres pg_isready -U healthdata

   # Check database logs
   make logs-db

   # Restart PostgreSQL
   docker-compose restart postgres
   ```

2. **Port Already in Use**
   ```bash
   # Find what's using port 8081
   lsof -i :8081

   # Kill the process
   kill -9 <PID>

   # Or change port in .env
   echo "CQL_ENGINE_PORT=8082" >> .env
   docker-compose up -d cql-engine-service
   ```

3. **Out of Memory**
   ```bash
   # Check container memory
   docker stats healthdata-cql-engine --no-stream

   # Increase memory in docker-compose.yml
   # Under cql-engine-service.deploy.resources.limits.memory

   # Or adjust JVM memory in .env
   echo "JAVA_OPTS=-XX:MaxRAMPercentage=50.0" >> .env
   ```

4. **Missing Environment Variables**
   ```bash
   # Check environment variables
   docker-compose exec cql-engine-service env | grep SPRING

   # Validate .env file
   cat .env

   # Recreate with defaults
   cp .env.example .env
   docker-compose up -d cql-engine-service
   ```

#### Problem: Slow Performance

**Solutions**:
```bash
# Check thread pool settings
curl http://localhost:8081/actuator/metrics/executor.active | jq '.'

# Increase thread pool in .env
echo "MEASURE_EVALUATION_MAX_POOL_SIZE=100" >> .env
docker-compose restart cql-engine-service

# Check database connection pool
curl http://localhost:8081/actuator/metrics/hikaricp.connections | jq '.'

# Enable caching
# Verify Redis is running
make logs-redis
```

#### Problem: Measure Evaluation Failures

**Diagnosis**:
```bash
# Check FHIR server connectivity
curl http://localhost:8080/fhir/metadata

# View evaluation errors
make logs-cql | grep "MeasureEvaluation"

# Check cache hit rate
curl http://localhost:8081/actuator/prometheus | grep cache
```

**Solutions**:
```bash
# Restart FHIR service
docker-compose restart fhir-service-mock

# Clear cache and restart
docker-compose exec redis redis-cli FLUSHALL
docker-compose restart cql-engine-service

# Check FHIR service URL
docker-compose exec cql-engine-service env | grep FHIR_SERVICE_URL
```

---

### PostgreSQL

#### Problem: Database Won't Start

**Diagnosis**:
```bash
# Check container status
docker-compose ps postgres

# View logs
make logs-db

# Check data volume
docker volume inspect healthdata-in-motion_postgres_data
```

**Solutions**:

1. **Corrupted Data**
   ```bash
   # Stop all services
   make down

   # Backup existing data
   make db-backup

   # Remove volume and recreate
   docker volume rm healthdata-in-motion_postgres_data
   make up
   ```

2. **Permission Issues**
   ```bash
   # Check volume permissions
   docker-compose exec postgres ls -la /var/lib/postgresql/data

   # Fix permissions
   docker-compose exec -u root postgres chown -R postgres:postgres /var/lib/postgresql/data
   docker-compose restart postgres
   ```

3. **Port Conflict**
   ```bash
   # Find process using port 5435
   lsof -i :5435

   # Change port in docker-compose.yml
   # ports: - "5436:5432"
   ```

#### Problem: Connection Timeouts

**Solutions**:
```bash
# Increase connection limits in docker-compose.yml
# Under postgres.command: add -c max_connections=200

# Restart PostgreSQL
docker-compose restart postgres

# Check active connections
docker-compose exec postgres psql -U healthdata -d healthdata_cql \
  -c "SELECT COUNT(*) FROM pg_stat_activity;"
```

#### Problem: Disk Space Full

**Diagnosis**:
```bash
# Check database size
docker-compose exec postgres psql -U healthdata -d healthdata_cql \
  -c "SELECT pg_database_size('healthdata_cql')/1024/1024 AS size_mb;"

# Check disk usage
df -h

# Check Docker volumes
docker system df -v
```

**Solutions**:
```bash
# Vacuum database
docker-compose exec postgres psql -U healthdata -d healthdata_cql \
  -c "VACUUM FULL ANALYZE;"

# Clean old Docker data
docker system prune -a --volumes

# Increase disk space (cloud/VM)
# Resize disk and restart Docker
```

---

### Redis

#### Problem: Redis Not Responding

**Diagnosis**:
```bash
# Check Redis status
docker-compose ps redis

# Test connection
docker-compose exec redis redis-cli ping

# Check memory usage
docker-compose exec redis redis-cli INFO memory
```

**Solutions**:

1. **Out of Memory**
   ```bash
   # Check memory limit
   docker-compose exec redis redis-cli CONFIG GET maxmemory

   # Flush unnecessary keys
   docker-compose exec redis redis-cli FLUSHDB

   # Increase memory limit in docker-compose.yml
   # command: redis-server --maxmemory 1gb
   ```

2. **Too Many Connections**
   ```bash
   # Check connected clients
   docker-compose exec redis redis-cli CLIENT LIST | wc -l

   # Restart Redis
   docker-compose restart redis
   ```

---

### Kafka

#### Problem: Kafka Won't Start

**Common Causes**:

1. **Zookeeper Not Ready**
   ```bash
   # Check Zookeeper
   docker-compose ps zookeeper
   make logs-zk

   # Restart in order
   docker-compose restart zookeeper
   sleep 10
   docker-compose restart kafka
   ```

2. **Port Conflicts**
   ```bash
   # Check port 9092
   lsof -i :9092

   # Change Kafka port in docker-compose.yml
   ```

3. **Broker ID Conflict**
   ```bash
   # Stop Kafka and Zookeeper
   docker-compose stop kafka zookeeper

   # Remove volumes
   docker volume rm healthdata-in-motion_kafka_data
   docker volume rm healthdata-in-motion_zookeeper_data

   # Restart
   docker-compose up -d zookeeper
   sleep 10
   docker-compose up -d kafka
   ```

#### Problem: Topic Not Created

**Diagnosis**:
```bash
# List topics
docker-compose exec kafka kafka-topics \
  --bootstrap-server localhost:9092 --list

# Describe topic
docker-compose exec kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --describe --topic cql-audit-events
```

**Solutions**:
```bash
# Create topic manually
docker-compose exec kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --create --topic cql-audit-events \
  --partitions 3 --replication-factor 1

# Restart CQL Engine to recreate topics
docker-compose restart cql-engine-service
```

---

### FHIR Server

#### Problem: FHIR Server Returns 500 Errors

**Diagnosis**:
```bash
# Check FHIR logs
make logs-fhir

# Test metadata endpoint
curl -v http://localhost:8080/fhir/metadata

# Check database connection
docker-compose exec fhir-service-mock env | grep DB
```

**Solutions**:
```bash
# Restart FHIR server
docker-compose restart fhir-service-mock

# Clear FHIR database (if needed)
docker-compose exec postgres psql -U healthdata -d healthdata_fhir \
  -c "TRUNCATE TABLE hfj_resource CASCADE;"

# Reload test data
./docker/fhir-test-data/load-test-data.sh
```

---

## Common Problems

### Problem: All Services Fail to Start

**Quick Fix**:
```bash
# Complete restart
make down
make clean
make setup
make up

# Wait for health
make health
```

### Problem: "Container Name Already in Use"

**Solution**:
```bash
# Remove all stopped containers
docker-compose down

# Force remove
docker-compose down --remove-orphans

# Clean up
docker system prune
```

### Problem: "Network Already Exists"

**Solution**:
```bash
# List networks
docker network ls | grep healthdata

# Remove network
docker network rm healthdata-network

# Recreate
docker-compose up -d
```

### Problem: "Volume in Use"

**Solution**:
```bash
# Stop all containers using volume
docker-compose down

# List containers using volume
docker ps -a --filter volume=healthdata-in-motion_postgres_data

# Force remove container
docker rm -f <container-id>

# Remove volume
docker volume rm healthdata-in-motion_postgres_data
```

---

## Performance Issues

### Slow Startup Times

**Diagnosis**:
```bash
# Check startup time
docker-compose logs cql-engine-service | grep "Started"

# Check health probe timing
docker inspect healthdata-cql-engine | jq '.[0].State.Health'
```

**Optimization**:
```bash
# Increase startup probe timeout in docker-compose.yml
# healthcheck.start_period: 120s

# Pre-warm JVM
# Add to .env:
echo "JAVA_OPTS=-XX:TieredStopAtLevel=1 -Xverify:none" >> .env

# Use Docker build cache
docker-compose build --pull
```

### High CPU Usage

**Diagnosis**:
```bash
# Monitor CPU
docker stats

# Check thread usage
curl http://localhost:8081/actuator/metrics/jvm.threads.live | jq '.'

# Profile application
docker-compose exec cql-engine-service jstack 1
```

**Solutions**:
```bash
# Limit CPU in docker-compose.yml
# cpus: '2.0'

# Adjust thread pool
echo "MEASURE_EVALUATION_CORE_POOL_SIZE=5" >> .env
echo "MEASURE_EVALUATION_MAX_POOL_SIZE=20" >> .env

# Enable G1GC
echo "JAVA_OPTS=-XX:+UseG1GC -XX:MaxGCPauseMillis=200" >> .env
```

### High Memory Usage

**Diagnosis**:
```bash
# Check memory
docker stats healthdata-cql-engine --no-stream

# JVM memory
curl http://localhost:8081/actuator/metrics/jvm.memory.used | jq '.'

# Heap dump
docker-compose exec cql-engine-service jcmd 1 GC.heap_dump /tmp/heap.hprof
```

**Solutions**:
```bash
# Reduce JVM heap
echo "JAVA_OPTS=-XX:MaxRAMPercentage=50.0" >> .env

# Increase Docker memory limit
# In Docker Desktop: Settings → Resources → Memory → 8GB

# Enable heap dump on OOM
echo "JAVA_OPTS=-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs" >> .env
```

---

## Networking Problems

### Cannot Access Services from Host

**Diagnosis**:
```bash
# Check port bindings
docker-compose ps

# Test connectivity
curl http://localhost:8081/actuator/health
nc -zv localhost 8081

# Check firewall
sudo iptables -L -n | grep 8081
```

**Solutions**:
```bash
# Verify docker-compose.yml ports section
# ports: - "8081:8081"

# Restart Docker daemon
sudo systemctl restart docker

# Check Docker network
docker network inspect healthdata-network
```

### Services Cannot Communicate

**Diagnosis**:
```bash
# Test from CQL Engine to PostgreSQL
docker-compose exec cql-engine-service \
  nc -zv postgres 5432

# Test DNS resolution
docker-compose exec cql-engine-service \
  nslookup postgres

# Check network
docker network ls
docker network inspect healthdata-network
```

**Solutions**:
```bash
# Recreate network
docker-compose down
docker network rm healthdata-network
docker-compose up -d

# Use explicit links in docker-compose.yml
# links: - postgres - redis - kafka
```

---

## Data & Volume Issues

### Lost Data After Restart

**Prevention**:
```bash
# Verify volumes are defined
docker volume ls | grep healthdata

# Check volume mounts
docker-compose config | grep volumes

# Always backup before major changes
make db-backup
```

**Recovery**:
```bash
# List backups
ls -lh backups/

# Restore from backup
make db-restore FILE=backups/backup_2024-10-31.sql

# Verify data
make db-connect
\dt
SELECT COUNT(*) FROM cql_library;
```

### Volume Permission Denied

**Solution**:
```bash
# Fix PostgreSQL volume permissions
docker-compose down
docker volume rm healthdata-in-motion_postgres_data
docker-compose up -d postgres

# For existing volume
docker-compose exec -u root postgres \
  chown -R postgres:postgres /var/lib/postgresql/data
```

---

## Build Problems

### Docker Build Fails

**Common Issues**:

1. **Gradle Download Timeout**
   ```bash
   # Increase timeout in backend/gradle/wrapper/gradle-wrapper.properties
   # networkTimeout=60000

   # Use cached Gradle
   docker build --build-arg GRADLE_USER_HOME=/cache/.gradle
   ```

2. **Out of Disk Space**
   ```bash
   # Clean Docker cache
   docker system prune -a

   # Check space
   df -h

   # Remove unused images
   docker image prune -a
   ```

3. **Network Issues During Build**
   ```bash
   # Use host network
   docker build --network=host -t healthdata/cql-engine-service:1.0.0 \
     --build-arg SERVICE_NAME=cql-engine-service \
     -f backend/Dockerfile backend

   # Configure proxy if needed
   docker build --build-arg HTTP_PROXY=http://proxy:8080
   ```

### Image Size Too Large

**Optimization**:
```bash
# Check image size
docker images | grep healthdata

# Use multi-stage builds (already implemented)
# Remove build cache
docker build --no-cache

# Analyze layers
docker history healthdata/cql-engine-service:1.0.0
```

---

## Docker Compose Issues

### Services Start in Wrong Order

**Solution**:
```bash
# Use depends_on with condition
# Already configured in docker-compose.yml

# Manual startup order
docker-compose up -d postgres redis zookeeper
sleep 10
docker-compose up -d kafka
sleep 5
docker-compose up -d cql-engine-service fhir-service-mock
```

### Environment Variables Not Applied

**Diagnosis**:
```bash
# Check loaded variables
docker-compose config

# Verify .env file
cat .env

# Check container env
docker-compose exec cql-engine-service env
```

**Solutions**:
```bash
# Recreate containers
docker-compose up -d --force-recreate

# Explicitly load .env
docker-compose --env-file .env up -d

# Use docker-compose.override.yml for local changes
```

---

## Logging & Debugging

### Enable Debug Logging

**CQL Engine**:
```bash
# Add to .env
echo "LOGGING_LEVEL_COM_HEALTHDATA_CQL=DEBUG" >> .env
docker-compose restart cql-engine-service

# View debug logs
make logs-cql | grep DEBUG
```

**Spring Boot**:
```bash
echo "LOGGING_LEVEL_ORG_SPRINGFRAMEWORK=DEBUG" >> .env
docker-compose restart cql-engine-service
```

### Capture All Logs

```bash
# All services to file
docker-compose logs --no-color > logs/all-services.log

# Specific service with timestamp
docker-compose logs -t cql-engine-service > logs/cql-engine.log

# Follow logs with grep
docker-compose logs -f | grep -i error
```

### Access Container Shell

```bash
# CQL Engine
make shell-cql

# PostgreSQL
make shell-db

# Redis
docker-compose exec redis sh

# Check files
docker-compose exec cql-engine-service ls -la /app
```

---

## Recovery Procedures

### Complete System Reset

```bash
# WARNING: Destroys all data

# 1. Stop everything
docker-compose down -v

# 2. Remove all related containers
docker ps -a | grep healthdata | awk '{print $1}' | xargs docker rm -f

# 3. Remove all volumes
docker volume ls | grep healthdata | awk '{print $2}' | xargs docker volume rm

# 4. Remove networks
docker network ls | grep healthdata | awk '{print $2}' | xargs docker network rm

# 5. Clean system
docker system prune -a --volumes

# 6. Rebuild from scratch
make setup
make deploy
```

### Restore from Backup

```bash
# 1. Stop services
make down

# 2. Start only database
docker-compose up -d postgres
sleep 10

# 3. Restore database
make db-restore FILE=backups/backup_2024-10-31.sql

# 4. Start all services
make up

# 5. Verify
make health
```

### Rollback to Previous Version

```bash
# 1. Stop current version
docker-compose down

# 2. Change image tag in docker-compose.yml
# image: healthdata/cql-engine-service:1.0.0-previous

# 3. Pull old image
docker-compose pull

# 4. Start with old version
docker-compose up -d

# 5. Verify
make health
```

---

## Getting Help

### Collect Diagnostic Information

```bash
# Run diagnostic script
./scripts/collect-diagnostics.sh

# Or manually:
docker-compose ps > diagnostics/ps.txt
docker-compose logs --tail=1000 > diagnostics/logs.txt
docker stats --no-stream > diagnostics/stats.txt
docker network inspect healthdata-network > diagnostics/network.json
docker volume inspect healthdata-in-motion_postgres_data > diagnostics/volume.json
```

### Useful Commands

```bash
# System overview
make status

# Health check
make health

# Full logs
make logs

# Database status
make db-connect
\l
\dt
\q

# Redis status
docker-compose exec redis redis-cli INFO

# Kafka topics
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

---

## Additional Resources

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Docker Guide](https://spring.io/guides/gs/spring-boot-docker/)
- [PostgreSQL Docker Guide](https://hub.docker.com/_/postgres)

---

**Last Updated**: 2025-10-31
**Version**: 1.0.0
**Maintainer**: HealthData-in-Motion Team
