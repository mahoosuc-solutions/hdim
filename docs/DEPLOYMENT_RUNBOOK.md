# Production Deployment Runbook
**HealthData-in-Motion CQL Quality Measure Evaluation System**

**Version:** 1.0
**Last Updated:** November 5, 2025

---

## Table of Contents

1. [Pre-Deployment Checklist](#pre-deployment-checklist)
2. [Deployment Steps](#deployment-steps)
3. [Post-Deployment Validation](#post-deployment-validation)
4. [Rollback Procedures](#rollback-procedures)
5. [Backup & Restore](#backup--restore)
6. [Troubleshooting](#troubleshooting)
7. [Monitoring & Alerts](#monitoring--alerts)

---

## Pre-Deployment Checklist

### Environment Preparation

- [ ] **Server provisioned** with minimum requirements:
  - CPU: 8 cores
  - RAM: 16GB
  - Disk: 500GB SSD
  - OS: Ubuntu 22.04 LTS or CentOS 8

- [ ] **Docker installed** (version 24.0+)
  ```bash
  docker --version  # Should be 24.0.0 or higher
  docker-compose --version  # Should be 2.20.0 or higher
  ```

- [ ] **SSL certificates** generated and placed in `./ssl/`
  ```bash
  ls -l ssl/
  # Should see: healthdata.crt, healthdata.key, keystore.p12
  ```

- [ ] **.env.production** configured with secure credentials
  ```bash
  # Verify no CHANGE_ME values remain
  grep -i "CHANGE_ME" .env.production
  # Should return no results
  ```

- [ ] **Firewall configured** and tested
  ```bash
  sudo ufw status
  # Should show: 80/tcp, 443/tcp ALLOW
  ```

- [ ] **DNS records** configured
  ```bash
  # Verify DNS resolution
  nslookup api.healthdata.example.com
  nslookup grafana.healthdata.example.com
  ```

### Code & Configuration

- [ ] **Latest code** checked out from main branch
  ```bash
  git checkout main
  git pull origin main
  git log -1  # Verify latest commit
  ```

- [ ] **Docker images** built or pulled
  ```bash
  # Build services
  cd backend/modules/services/cql-engine-service
  ./gradlew bootBuildImage

  cd backend/modules/services/quality-measure-service
  ./gradlew bootBuildImage
  ```

- [ ] **Database migrations** reviewed
  ```bash
  # Check pending migrations
  ls backend/modules/services/cql-engine-service/src/main/resources/db/changelog/
  ```

- [ ] **Configuration files** reviewed
  - docker-compose.prod.yml
  - .env.production
  - prometheus-prod.yml
  - alerts.yml

### Security

- [ ] **Security audit** completed (see PRODUCTION_SECURITY_GUIDE.md)
- [ ] **Penetration testing** passed
- [ ] **Vulnerability scan** clean
- [ ] **Secrets rotated** (JWT, API keys, passwords)
- [ ] **Backup system** tested

### Team Readiness

- [ ] **Deployment team** assembled
  - Deployment Lead
  - Backend Engineer
  - DevOps Engineer
  - Security Engineer
  - QA Engineer

- [ ] **Communication channels** ready
  - Slack channel: #healthdata-deployments
  - Video conference: [Link]
  - Incident hotline: +1-555-INCIDENT

- [ ] **Rollback plan** reviewed with team
- [ ] **Maintenance window** scheduled and communicated
- [ ] **On-call rotation** updated

---

## Deployment Steps

### Phase 1: Pre-Deployment (T-30 minutes)

**1. Announce Maintenance Window**

```bash
# Post to Slack
Deployment starting in 30 minutes.
Expected duration: 1 hour
Expected downtime: 5-10 minutes
Deployment Lead: [Name]
```

**2. Verify System Health**

```bash
# Check current system status
docker-compose ps
docker-compose logs --tail=100

# Check disk space
df -h

# Check memory
free -h

# Verify backup
ls -lh /backup/latest/
```

**3. Create Pre-Deployment Backup**

```bash
# Execute backup script
./scripts/backup.sh --type=pre-deployment --tag=v1.0.17

# Verify backup completed
ls -lh /backup/pre-deployment-$(date +%Y%m%d)/
```

**4. Notify Users**

```bash
# If using maintenance mode endpoint
curl -X POST https://api.healthdata.example.com/admin/maintenance \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"enabled": true, "message": "System maintenance in progress"}'
```

### Phase 2: Stop Services (T-0)

**1. Stop Existing Services Gracefully**

```bash
# Stop services in dependency order
docker-compose down --timeout 60

# Verify all stopped
docker ps
# Should show no healthdata containers
```

**2. Verify Data Persisted**

```bash
# Check volume integrity
docker volume ls | grep healthdata
docker volume inspect healthdata_postgres_data_prod

# Verify backup volumes mounted
ls -lh /var/lib/docker/volumes/
```

### Phase 3: Deploy New Version (T+2 minutes)

**1. Pull Latest Images**

```bash
# If using registry
docker-compose -f docker-compose.prod.yml pull

# Or load from local builds
docker images | grep healthdata
```

**2. Update Configuration**

```bash
# Copy new configs if changed
cp docker-compose.prod.yml docker-compose.yml.backup
cp .env.production .env.production.backup

# Update version tags
export CQL_ENGINE_VERSION=1.0.17
export QM_SERVICE_VERSION=1.0.13
```

**3. Start Infrastructure Services First**

```bash
# Start databases and cache
docker-compose -f docker-compose.prod.yml --env-file .env.production up -d \
  postgres redis zookeeper kafka

# Wait for health checks
docker-compose ps
# Watch for (healthy) status

# Tail logs to verify startup
docker-compose logs -f postgres redis kafka &
```

**4. Run Database Migrations**

```bash
# Migrations run automatically on service start, but verify
docker-compose logs cql-engine-service | grep -i "liquibase"
docker-compose logs quality-measure-service | grep -i "liquibase"

# Or run manually if needed
docker exec healthdata-postgres-prod psql -U $POSTGRES_USER -d healthdata_cql \
  -c "SELECT * FROM databasechangelog ORDER BY dateexecuted DESC LIMIT 5;"
```

**5. Start Application Services**

```bash
# Start FHIR server
docker-compose -f docker-compose.prod.yml --env-file .env.production up -d fhir-service-mock

# Wait for FHIR to be healthy
until docker-compose ps fhir-service-mock | grep -q "healthy"; do
  echo "Waiting for FHIR service..."
  sleep 5
done

# Start CQL Engine Service
docker-compose -f docker-compose.prod.yml --env-file .env.production up -d cql-engine-service

# Wait for health check
until docker-compose ps cql-engine-service | grep -q "healthy"; do
  echo "Waiting for CQL Engine..."
  sleep 5
done

# Start Quality Measure Service
docker-compose -f docker-compose.prod.yml --env-file .env.production up -d quality-measure-service

# Wait for health check
until docker-compose ps quality-measure-service | grep -q "healthy"; do
  echo "Waiting for Quality Measure Service..."
  sleep 5
done
```

**6. Start Monitoring Services**

```bash
# Start Prometheus and Grafana
docker-compose -f docker-compose.prod.yml --env-file .env.production up -d prometheus grafana

# Verify all services running
docker-compose ps
```

### Phase 4: Post-Deployment Validation (T+10 minutes)

**1. Health Check Endpoints**

```bash
# CQL Engine health
curl -f https://api.healthdata.example.com/cql-engine/actuator/health
# Expected: {"status":"UP"}

# Quality Measure health
curl -f https://api.healthdata.example.com/quality-measure/_health
# Expected: {"status":"UP"}

# FHIR server metadata
curl -f https://fhir.healthdata.example.com/fhir/metadata
# Expected: CapabilityStatement
```

**2. Database Connectivity**

```bash
# Test database connection
docker exec healthdata-cql-engine-prod curl -f http://localhost:8081/cql-engine/actuator/health/db
# Expected: {"status":"UP"}

# Check library count
curl -u "$CQL_SERVICE_USERNAME:$CQL_SERVICE_PASSWORD" \
  -H "X-Tenant-ID: TENANT001" \
  https://api.healthdata.example.com/cql-engine/api/v1/cql/libraries | jq '.content | length'
# Expected: 6 (or your total measure count)
```

**3. Cache Connectivity**

```bash
# Test Redis connection
docker exec healthdata-cql-engine-prod curl -f http://localhost:8081/cql-engine/actuator/health/redis
# Expected: {"status":"UP"}

# Check cache hit rate
curl -u "$CQL_SERVICE_USERNAME:$CQL_SERVICE_PASSWORD" \
  https://api.healthdata.example.com/cql-engine/actuator/metrics/cache.hit.rate | jq
```

**4. Kafka Connectivity**

```bash
# List Kafka topics
docker exec healthdata-kafka-prod kafka-topics --bootstrap-server localhost:9092 --list
# Expected: batch.progress, evaluation.completed, evaluation.failed, evaluation.started

# Check consumer groups
docker exec healthdata-kafka-prod kafka-consumer-groups --bootstrap-server localhost:9092 --list
```

**5. Run Smoke Tests**

```bash
# Test evaluation endpoint
curl -X POST \
  -u "$CQL_SERVICE_USERNAME:$CQL_SERVICE_PASSWORD" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: TENANT001" \
  -d '{"libraryId": "your-library-id", "patientId": "Patient/test-123"}' \
  https://api.healthdata.example.com/cql-engine/api/v1/cql/evaluations

# Expected: HTTP 200/201 with evaluation result
```

**6. Frontend Validation**

```bash
# Check frontend deployment (if applicable)
curl -f https://dashboard.healthdata.example.com
# Expected: HTTP 200

# Test WebSocket connection
# Open browser DevTools Console:
const ws = new WebSocket('wss://api.healthdata.example.com/cql-engine/ws/evaluation-progress?tenantId=TENANT001');
ws.onopen = () => console.log('WebSocket connected');
ws.onmessage = (e) => console.log('Received:', e.data);
```

**7. Monitoring Validation**

```bash
# Check Prometheus targets
curl -f https://grafana.healthdata.example.com/api/datasources/proxy/1/api/v1/targets | jq '.data.activeTargets[] | {job: .labels.job, health: .health}'

# Verify Grafana dashboards
curl -u "$GRAFANA_ADMIN_USER:$GRAFANA_ADMIN_PASSWORD" \
  https://grafana.healthdata.example.com/api/dashboards/home | jq
```

### Phase 5: Enable Production Traffic (T+20 minutes)

**1. Disable Maintenance Mode**

```bash
# Remove maintenance mode
curl -X POST https://api.healthdata.example.com/admin/maintenance \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"enabled": false}'
```

**2. Update Load Balancer**

```bash
# If using external load balancer
# Enable backend pool
aws elbv2 modify-target-group \
  --target-group-arn $TG_ARN \
  --health-check-enabled \
  --health-check-path /actuator/health

# Verify health
aws elbv2 describe-target-health --target-group-arn $TG_ARN
```

**3. Announce Completion**

```bash
# Post to Slack
Deployment completed successfully! ✅
Version: 1.0.17
Downtime: 8 minutes
All health checks passing.
Dashboard: https://grafana.healthdata.example.com
```

---

## Post-Deployment Validation

### Automated Test Suite

**Run full integration tests:**

```bash
# Backend integration tests
cd backend
./gradlew integrationTest -Dspring.profiles.active=production

# Frontend E2E tests (if applicable)
cd frontend
npm run test:e2e:prod
```

### Manual Validation Checklist

- [ ] **All services healthy** in Grafana
- [ ] **No errors** in application logs (last 15 minutes)
- [ ] **Prometheus targets** all UP
- [ ] **Database migrations** completed successfully
- [ ] **Cache hit rate** above 80%
- [ ] **WebSocket connections** working
- [ ] **API response times** under 200ms (p95)
- [ ] **No memory leaks** (stable memory usage)
- [ ] **CPU usage** under 50%
- [ ] **Disk space** sufficient (>20% free)

### Performance Baseline

```bash
# Capture baseline metrics
curl https://grafana.healthdata.example.com/api/datasources/proxy/1/api/v1/query \
  -G --data-urlencode 'query=rate(http_server_requests_seconds_sum[5m])' \
  -u "$GRAFANA_ADMIN_USER:$GRAFANA_ADMIN_PASSWORD" | jq

# Save baseline for comparison
cat > /tmp/baseline-$(date +%Y%m%d).json <<EOF
{
  "deployment_version": "1.0.17",
  "timestamp": "$(date -Iseconds)",
  "metrics": {
    "avg_response_time_ms": $(curl -s ... | jq '.data.result[0].value[1]'),
    "requests_per_second": ...,
    "error_rate": ...,
    "cache_hit_rate": ...
  }
}
EOF
```

---

## Rollback Procedures

### When to Rollback

**Immediate Rollback Triggers:**
- Critical security vulnerability discovered
- Data corruption detected
- > 50% error rate
- Complete service outage > 5 minutes
- Database integrity compromised

**Consideration for Rollback:**
- Error rate > 10%
- Performance degradation > 50%
- Critical feature not working
- Stakeholder escalation

### Rollback Steps (< 10 minutes)

**1. Declare Rollback**

```bash
# Post to Slack
🚨 ROLLBACK INITIATED 🚨
Reason: [Brief description]
Deployment Lead: [Name]
ETA: 10 minutes
```

**2. Stop Current Services**

```bash
# Stop all services
docker-compose -f docker-compose.prod.yml down --timeout 30

# DO NOT remove volumes (preserve data)
```

**3. Restore Previous Version**

```bash
# Checkout previous version
git checkout v1.0.16  # Previous stable version

# Or pull previous images
export CQL_ENGINE_VERSION=1.0.16
export QM_SERVICE_VERSION=1.0.12

docker-compose -f docker-compose.prod.yml pull
```

**4. Rollback Database (if needed)**

```bash
# ONLY if migrations caused issues
# Restore from pre-deployment backup
./scripts/restore.sh --backup-id=pre-deployment-$(date +%Y%m%d)

# Or rollback specific migrations (use with caution)
docker exec healthdata-postgres-prod psql -U $POSTGRES_USER -d healthdata_cql \
  -c "SELECT * FROM databasechangelog ORDER BY dateexecuted DESC LIMIT 10;"

# Manual rollback (Liquibase)
docker exec healthdata-cql-engine-prod \
  java -jar liquibase.jar \
  --changeLogFile=db/changelog/db.changelog-master.xml \
  rollbackCount 1
```

**5. Start Previous Version**

```bash
# Start services
docker-compose -f docker-compose.prod.yml --env-file .env.production up -d

# Monitor logs
docker-compose logs -f --tail=100
```

**6. Validate Rollback**

```bash
# Run health checks (same as post-deployment)
./scripts/health-check.sh

# Verify version
curl https://api.healthdata.example.com/cql-engine/actuator/info | jq '.build.version'
# Should show: 1.0.16
```

**7. Announce Rollback Complete**

```bash
# Post to Slack
Rollback completed.
Current version: 1.0.16
All systems operational.
Post-mortem scheduled for: [Time]
```

---

## Backup & Restore

### Automated Backups

**Backup Schedule:**
- **Daily full backup**: 2:00 AM UTC
- **Hourly incremental**: On the hour
- **Pre/post-deployment**: Manual trigger
- **Retention**: 30 days

**Backup Script** (`scripts/backup.sh`):

```bash
#!/bin/bash
set -euo pipefail

# Configuration
BACKUP_DIR="/backup"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_TAG="${1:-scheduled}"
S3_BUCKET="${BACKUP_S3_BUCKET:-healthdata-backups}"

# Create backup directory
mkdir -p "${BACKUP_DIR}/${BACKUP_TAG}-${TIMESTAMP}"

echo "Starting backup: ${BACKUP_TAG}-${TIMESTAMP}"

# Backup PostgreSQL databases
echo "Backing up PostgreSQL..."
docker exec healthdata-postgres-prod pg_dumpall -U $POSTGRES_USER | \
  gzip > "${BACKUP_DIR}/${BACKUP_TAG}-${TIMESTAMP}/postgres_all_databases.sql.gz"

docker exec healthdata-postgres-prod pg_dump -U $POSTGRES_USER -Fc healthdata_cql > \
  "${BACKUP_DIR}/${BACKUP_TAG}-${TIMESTAMP}/healthdata_cql.dump"

docker exec healthdata-postgres-prod pg_dump -U $QM_POSTGRES_USER -Fc healthdata_quality_measure > \
  "${BACKUP_DIR}/${BACKUP_TAG}-${TIMESTAMP}/healthdata_quality_measure.dump"

# Backup Redis
echo "Backing up Redis..."
docker exec healthdata-redis-prod redis-cli --rdb /data/dump.rdb SAVE
docker cp healthdata-redis-prod:/data/dump.rdb \
  "${BACKUP_DIR}/${BACKUP_TAG}-${TIMESTAMP}/redis_dump.rdb"

# Backup Docker volumes
echo "Backing up Docker volumes..."
docker run --rm \
  -v healthdata_postgres_data_prod:/data \
  -v ${BACKUP_DIR}/${BACKUP_TAG}-${TIMESTAMP}:/backup \
  alpine tar czf /backup/postgres_volume.tar.gz -C /data .

docker run --rm \
  -v healthdata_kafka_data_prod:/data \
  -v ${BACKUP_DIR}/${BACKUP_TAG}-${TIMESTAMP}:/backup \
  alpine tar czf /backup/kafka_volume.tar.gz -C /data .

# Backup configuration files
echo "Backing up configurations..."
tar czf "${BACKUP_DIR}/${BACKUP_TAG}-${TIMESTAMP}/configs.tar.gz" \
  docker-compose.prod.yml \
  .env.production \
  docker/prometheus/ \
  docker/grafana/ \
  ssl/

# Create backup manifest
cat > "${BACKUP_DIR}/${BACKUP_TAG}-${TIMESTAMP}/manifest.json" <<EOF
{
  "backup_id": "${BACKUP_TAG}-${TIMESTAMP}",
  "timestamp": "$(date -Iseconds)",
  "type": "${BACKUP_TAG}",
  "version": "$(git describe --tags --always)",
  "files": $(ls -1 ${BACKUP_DIR}/${BACKUP_TAG}-${TIMESTAMP}/ | jq -R . | jq -s .),
  "size_bytes": $(du -sb ${BACKUP_DIR}/${BACKUP_TAG}-${TIMESTAMP}/ | cut -f1)
}
EOF

# Upload to S3
echo "Uploading to S3..."
aws s3 sync "${BACKUP_DIR}/${BACKUP_TAG}-${TIMESTAMP}/" \
  "s3://${S3_BUCKET}/${BACKUP_TAG}-${TIMESTAMP}/" \
  --storage-class STANDARD_IA

# Create latest symlink
ln -sfn "${BACKUP_DIR}/${BACKUP_TAG}-${TIMESTAMP}" "${BACKUP_DIR}/latest"

# Cleanup old backups (keep last 30 days)
find ${BACKUP_DIR} -type d -name "scheduled-*" -mtime +30 -exec rm -rf {} +

echo "Backup completed: ${BACKUP_TAG}-${TIMESTAMP}"
echo "Location: ${BACKUP_DIR}/${BACKUP_TAG}-${TIMESTAMP}"
echo "Size: $(du -sh ${BACKUP_DIR}/${BACKUP_TAG}-${TIMESTAMP}/ | cut -f1)"
```

### Restore Procedures

**Restore Script** (`scripts/restore.sh`):

```bash
#!/bin/bash
set -euo pipefail

# Configuration
BACKUP_ID="${1:?Usage: $0 <backup-id>}"
BACKUP_DIR="/backup"
RESTORE_DIR="${BACKUP_DIR}/${BACKUP_ID}"

if [ ! -d "${RESTORE_DIR}" ]; then
  echo "Backup not found: ${BACKUP_ID}"
  echo "Available backups:"
  ls -1 ${BACKUP_DIR}/ | grep -E "^(scheduled|pre-deployment|manual)-"
  exit 1
fi

echo "⚠️  WARNING: This will overwrite current data!"
echo "Backup ID: ${BACKUP_ID}"
echo "Location: ${RESTORE_DIR}"
read -p "Continue? (type 'yes' to proceed): " -r
if [ "$REPLY" != "yes" ]; then
  echo "Restore cancelled."
  exit 0
fi

# Stop services
echo "Stopping services..."
docker-compose down

# Restore PostgreSQL
echo "Restoring PostgreSQL databases..."
cat "${RESTORE_DIR}/postgres_all_databases.sql.gz" | \
  gunzip | \
  docker exec -i healthdata-postgres-prod psql -U $POSTGRES_USER

# Or restore specific databases
docker exec -i healthdata-postgres-prod pg_restore \
  -U $POSTGRES_USER -d healthdata_cql -c \
  < "${RESTORE_DIR}/healthdata_cql.dump"

docker exec -i healthdata-postgres-prod pg_restore \
  -U $QM_POSTGRES_USER -d healthdata_quality_measure -c \
  < "${RESTORE_DIR}/healthdata_quality_measure.dump"

# Restore Redis
echo "Restoring Redis..."
docker cp "${RESTORE_DIR}/redis_dump.rdb" healthdata-redis-prod:/data/dump.rdb
docker-compose restart redis

# Restore volumes
echo "Restoring Docker volumes..."
docker run --rm \
  -v healthdata_postgres_data_prod:/data \
  -v ${RESTORE_DIR}:/backup \
  alpine sh -c "cd /data && tar xzf /backup/postgres_volume.tar.gz"

docker run --rm \
  -v healthdata_kafka_data_prod:/data \
  -v ${RESTORE_DIR}:/backup \
  alpine sh -c "cd /data && tar xzf /backup/kafka_volume.tar.gz"

# Restore configurations
echo "Restoring configurations..."
tar xzf "${RESTORE_DIR}/configs.tar.gz" -C /

# Start services
echo "Starting services..."
docker-compose -f docker-compose.prod.yml --env-file .env.production up -d

# Wait for health
echo "Waiting for services to be healthy..."
sleep 30
docker-compose ps

echo "✅ Restore completed from backup: ${BACKUP_ID}"
echo "Please validate system functionality."
```

---

## Troubleshooting

### Common Issues

#### Issue: Service Won't Start

**Symptoms:**
- Container exits immediately
- Health check fails continuously

**Diagnosis:**
```bash
# Check logs
docker-compose logs --tail=100 cql-engine-service

# Check container status
docker inspect healthdata-cql-engine-prod | jq '.[0].State'

# Check resource usage
docker stats --no-stream
```

**Solutions:**
1. Check environment variables:
   ```bash
   docker exec healthdata-cql-engine-prod env | grep -i postgres
   ```

2. Verify database connectivity:
   ```bash
   docker exec healthdata-cql-engine-prod nc -zv postgres 5432
   ```

3. Check disk space:
   ```bash
   df -h
   docker system df
   ```

4. Review application logs:
   ```bash
   docker-compose logs --tail=200 cql-engine-service | grep -i error
   ```

#### Issue: High Memory Usage

**Symptoms:**
- OOMKilled errors
- Slow performance
- Containers restarting

**Diagnosis:**
```bash
# Check memory usage
docker stats --no-stream

# Check Java heap
docker exec healthdata-cql-engine-prod jstat -gc 1

# Heap dump for analysis
docker exec healthdata-cql-engine-prod \
  jcmd 1 GC.heap_dump /tmp/heap_dump.hprof
```

**Solutions:**
1. Adjust JVM settings in .env.production:
   ```
   JAVA_OPTS=-Xms2g -Xmx4g -XX:+UseG1GC
   ```

2. Increase Docker memory limits in docker-compose.prod.yml:
   ```yaml
   deploy:
     resources:
       limits:
         memory: 6G
   ```

3. Investigate memory leaks

#### Issue: Database Connection Pool Exhausted

**Symptoms:**
- "Connection timeout" errors
- Slow API responses
- HikariCP warnings in logs

**Diagnosis:**
```bash
# Check active connections
docker exec healthdata-postgres-prod psql -U $POSTGRES_USER -d healthdata_cql \
  -c "SELECT count(*) FROM pg_stat_activity WHERE datname='healthdata_cql';"

# Check pool metrics
curl http://localhost:8081/cql-engine/actuator/metrics/hikaricp.connections.active | jq
```

**Solutions:**
1. Increase pool size in application-production.yml:
   ```yaml
   spring.datasource.hikari:
     maximum-pool-size: 20
     minimum-idle: 10
   ```

2. Check for connection leaks:
   ```bash
   docker-compose logs cql-engine-service | grep -i "connection.*not.*closed"
   ```

#### Issue: Cache Miss Rate High

**Symptoms:**
- Slow evaluation performance
- High database load
- Cache hit rate < 80%

**Diagnosis:**
```bash
# Check cache stats
curl http://localhost:8081/cql-engine/actuator/metrics/cache.gets | jq

# Check Redis memory
docker exec healthdata-redis-prod redis-cli INFO memory
```

**Solutions:**
1. Increase Redis memory:
   ```bash
   # In docker-compose.prod.yml
   command: redis-server --maxmemory 2gb
   ```

2. Adjust TTL:
   ```yaml
   spring.cache.redis:
     time-to-live: 86400000  # 24 hours
   ```

3. Check eviction policy:
   ```bash
   docker exec healthdata-redis-prod redis-cli CONFIG GET maxmemory-policy
   # Should be: allkeys-lru
   ```

---

## Monitoring & Alerts

### Key Metrics to Monitor

**System Health:**
- Service uptime (target: >99.9%)
- Health check status
- Container restart count

**Performance:**
- API response time (p50, p95, p99)
- Throughput (requests/second)
- Error rate (target: <1%)
- Evaluation time (target: <200ms)

**Resources:**
- CPU usage (target: <70%)
- Memory usage (target: <80%)
- Disk space (target: >20% free)
- Network I/O

**Business Metrics:**
- Evaluations processed/hour
- Success rate (target: >95%)
- Cache hit rate (target: >80%)
- WebSocket connections

### Alert Response Procedures

**Critical Alerts (P1):**
- Response time: Immediate
- Actions:
  1. Acknowledge alert
  2. Check service status
  3. Review logs for errors
  4. Engage incident response team if needed
  5. Consider rollback if severe

**Warning Alerts (P2):**
- Response time: 15 minutes
- Actions:
  1. Acknowledge alert
  2. Investigate cause
  3. Monitor for escalation
  4. Schedule fix if needed

**Info Alerts (P3):**
- Response time: 1 hour
- Actions:
  1. Review alert
  2. Document findings
  3. Update runbooks

---

## Post-Deployment Tasks

### Immediate (Within 24 hours)

- [ ] Monitor system for 24 hours
- [ ] Review all logs for errors
- [ ] Validate all integrations
- [ ] Update documentation if needed
- [ ] Conduct team retrospective

### Short-term (Within 1 week)

- [ ] Analyze performance metrics vs. baseline
- [ ] Review and tune alert thresholds
- [ ] Update monitoring dashboards
- [ ] Plan for next deployment
- [ ] Document lessons learned

### Long-term (Within 1 month)

- [ ] Conduct security audit
- [ ] Review and update disaster recovery plan
- [ ] Capacity planning review
- [ ] Update runbooks based on issues encountered
- [ ] Schedule next penetration test

---

## Emergency Contacts

**Deployment Team:**
- Deployment Lead: [Name] - [Phone] - [Email]
- Backend Engineer: [Name] - [Phone] - [Email]
- DevOps Engineer: [Name] - [Phone] - [Email]

**Escalation:**
- Engineering Manager: [Name] - [Phone] - [Email]
- CTO: [Name] - [Phone] - [Email]

**External Support:**
- Cloud Provider Support: [Phone]
- Security Team: security-team@healthdata.example.com
- Database DBA: dba-team@healthdata.example.com

**Incident Hotline:** +1-555-INCIDENT

---

## Appendix

### Useful Commands

**View logs for all services:**
```bash
docker-compose logs -f --tail=100
```

**Restart specific service:**
```bash
docker-compose restart cql-engine-service
```

**Execute command in container:**
```bash
docker exec -it healthdata-cql-engine-prod bash
```

**Check resource usage:**
```bash
docker stats
```

**Clean up unused resources:**
```bash
docker system prune -a --volumes
```

**Export metrics:**
```bash
curl http://localhost:9090/api/v1/query?query=up | jq > metrics.json
```

---

**Document Version:** 1.0
**Last Updated:** 2025-11-05
**Next Review:** After each production deployment
