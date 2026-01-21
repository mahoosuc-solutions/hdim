# Measure Builder - Administration Manual

**Version:** 1.0
**Last Updated:** January 18, 2026
**Audience:** System Administrators, IT Operations
**Access Level:** ADMIN role required

---

## Table of Contents

1. [Overview](#overview)
2. [Configuration](#configuration)
3. [User Management](#user-management)
4. [System Maintenance](#system-maintenance)
5. [Monitoring & Alerting](#monitoring--alerting)
6. [Performance Tuning](#performance-tuning)
7. [Troubleshooting](#troubleshooting)
8. [Backup & Recovery](#backup--recovery)

---

## Overview

This manual covers administration of the Measure Builder system including configuration, user management, system maintenance, and operational procedures.

### System Requirements

**Hardware:**
- CPU: 8+ cores minimum
- RAM: 16GB+ minimum
- Disk: 50GB+ available space
- Network: 1Gbps+ bandwidth

**Software:**
- Java 21 LTS
- PostgreSQL 16+
- Redis 7+
- Kafka 3.x+
- Docker 24.0+ (if containerized)

**Browser Support:**
- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

---

## Configuration

### Environment Variables

**Critical Configuration:**

```yaml
# Enable/Disable Measure Builder
ENABLE_MEASURE_BUILDER=true

# Complexity Limits
MEASURE_BUILDER_MAX_COMPLEXITY=10  # 1-10, higher = more features
MEASURE_BUILDER_ALLOW_CUSTOM_CQL=false  # Allow manual CQL editing

# Performance Tuning
MEASURE_BUILDER_DEBOUNCE_MS=500  # Delay for real-time validation

# Data Management
MEASURE_BUILDER_VALIDATION_ENABLED=true  # Enable schema validation
MEASURE_BUILDER_DRAFT_TTL_HOURS=72  # Auto-delete old drafts

# Version Control
MEASURE_BUILDER_AUTO_VERSION=true  # Auto-increment on publish

# Access Control
MEASURE_BUILDER_ALLOW_IMPORTS=true  # Allow external measure imports
MEASURE_BUILDER_MIN_ROLE=EVALUATOR  # Minimum role to access (VIEWER, ANALYST, EVALUATOR, ADMIN, SUPER_ADMIN)
```

### Docker Compose Configuration

**Development:**
```yaml
environment:
  ENABLE_MEASURE_BUILDER: "true"
  MEASURE_BUILDER_MAX_COMPLEXITY: "10"
  MEASURE_BUILDER_ALLOW_CUSTOM_CQL: "false"
```

**Staging:**
```yaml
environment:
  ENABLE_MEASURE_BUILDER: "true"
  MEASURE_BUILDER_MAX_COMPLEXITY: "10"
  MEASURE_BUILDER_ALLOW_CUSTOM_CQL: "false"
  DEBUG: "false"
```

**Production:**
```yaml
environment:
  ENABLE_MEASURE_BUILDER: "false"  # Disabled by default, enable explicitly
  MEASURE_BUILDER_MAX_COMPLEXITY: "8"  # Lower limit
  MEASURE_BUILDER_ALLOW_CUSTOM_CQL: "false"  # Never allow in production
  MEASURE_BUILDER_MIN_ROLE: "ADMIN"  # Higher permission requirement
```

---

## User Management

### Role-Based Access Control

| Role | Can Create | Can Edit | Can Delete | Can Publish | Can Admin |
|------|-----------|----------|-----------|-----------|----------|
| VIEWER | ❌ | ❌ | ❌ | ❌ | ❌ |
| ANALYST | ✅ | ✅ | ❌ | ❌ | ❌ |
| EVALUATOR | ✅ | ✅ | ✅ (own) | ✅ | ❌ |
| ADMIN | ✅ | ✅ | ✅ | ✅ | ✅ |
| SUPER_ADMIN | ✅ | ✅ | ✅ | ✅ | ✅ |

### Assigning Measure Builder Access

```bash
# CLI command to grant access
hdim-cli users grant-role $USER_ID EVALUATOR --measure-builder

# Or via API
curl -X POST http://localhost:8087/api/v1/users/$USER_ID/roles \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"role": "EVALUATOR", "scope": "MEASURE_BUILDER"}'
```

### Tenant Management

**Multi-Tenant Configuration:**

```yaml
# Per-tenant feature flags
TENANTS:
  TENANT001:
    MEASURE_BUILDER_ENABLED: true
    MEASURE_BUILDER_MAX_COMPLEXITY: 10
    MEASURE_BUILDER_ALLOW_CUSTOM_CQL: false
  TENANT002:
    MEASURE_BUILDER_ENABLED: false
    # Inherits defaults
```

---

## System Maintenance

### Database Maintenance

**Connection Pool Monitoring:**

```sql
-- Check active connections
SELECT datname, count(*) as connections
FROM pg_stat_activity
WHERE datname = 'quality_db'
GROUP BY datname;

-- Recommended: 10-20 active, max 100
```

**Index Optimization:**

```bash
# Run weekly
docker exec hdim-postgres psql -U healthdata -d quality_db \
  -c "ANALYZE; REINDEX;"
```

**Backup Procedure:**

```bash
# Daily backup
docker exec hdim-postgres pg_dump -U healthdata quality_db | \
  gzip > backup_quality_db_$(date +%Y%m%d).sql.gz

# Retention: Keep 30 days minimum
```

### Cache Management

**Redis Monitoring:**

```bash
# Check Redis memory usage
docker exec hdim-redis redis-cli INFO memory

# Clear cache (emergency only)
docker exec hdim-redis redis-cli FLUSHALL
```

**Cache Invalidation:**

```bash
# For PHI data, TTL must be <= 5 minutes
# Automatically handled by system
# No manual invalidation needed for compliance
```

### Log Management

**Log Levels:**

```yaml
# Development
LOGGING_LEVEL_ROOT: DEBUG
LOGGING_LEVEL_COM_HEALTHDATA_MEASURE_BUILDER: DEBUG

# Staging
LOGGING_LEVEL_ROOT: INFO
LOGGING_LEVEL_COM_HEALTHDATA_MEASURE_BUILDER: DEBUG

# Production
LOGGING_LEVEL_ROOT: WARNING
LOGGING_LEVEL_COM_HEALTHDATA_MEASURE_BUILDER: INFO
```

**Log Retention:**

```bash
# Archive logs older than 30 days
find /var/log/measure-builder -name "*.log" -mtime +30 -exec gzip {} \;

# Delete archives older than 90 days
find /var/log/measure-builder -name "*.log.gz" -mtime +90 -delete
```

---

## Monitoring & Alerting

### Key Metrics to Monitor

| Metric | Target | Warning | Critical |
|--------|--------|---------|----------|
| CPU Usage | <60% | >75% | >90% |
| Memory Usage | <70% | >80% | >90% |
| Disk Space | >20% free | <15% | <5% |
| API Response P95 | <500ms | >750ms | >1000ms |
| Error Rate | <0.1% | >0.5% | >1% |
| Database Connections | <50 | >75 | >100 |

### Prometheus Alerts

**Sample Alert Rules:**

```yaml
groups:
  - name: measure-builder
    rules:
      - alert: HighCPUUsage
        expr: cpu_usage{service="measure-builder"} > 80
        for: 5m
        annotations:
          summary: "High CPU usage detected"

      - alert: SlowAPIResponse
        expr: histogram_quantile(0.95, http_request_duration_seconds) > 0.5
        for: 5m
        annotations:
          summary: "Slow API responses"

      - alert: DatabaseConnectionPoolExhausted
        expr: db_connections{status="active"} > 100
        for: 2m
        annotations:
          summary: "Database connection pool near capacity"
```

### Grafana Dashboards

**Standard Dashboards:**

1. **System Health:** CPU, Memory, Disk, Network
2. **API Metrics:** Response times, request rates, errors
3. **Database:** Connection pool, query performance, cache hits
4. **Business Metrics:** Measures created, evaluations run, users active

---

## Performance Tuning

### Optimization Tips

**1. Database Query Optimization:**

```sql
-- Add indexes for common queries
CREATE INDEX idx_measures_tenant_status
  ON measures(tenant_id, status);

CREATE INDEX idx_measures_created_date
  ON measures(created_at DESC);
```

**2. Cache Configuration:**

```yaml
SPRING_DATA_REDIS_TIMEOUT: 2000  # 2 second timeout
SPRING_DATA_REDIS_JEDIS_POOL_MAX_ACTIVE: 50
SPRING_DATA_REDIS_JEDIS_POOL_MIN_IDLE: 10
```

**3. Connection Pooling:**

```yaml
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE: 20
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE: 5
SPRING_DATASOURCE_HIKARI_CONNECTION_TIMEOUT: 20000
```

**4. JVM Tuning:**

```bash
JAVA_OPTS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200"
```

### Scaling Considerations

**Horizontal Scaling:**

- Add more application instances behind load balancer
- Ensure database supports concurrent connections
- Use Redis for distributed caching
- Monitor with Prometheus for bottlenecks

**Vertical Scaling:**

- Increase CPU cores for parallel processing
- Add RAM for larger caches
- Use faster SSD storage for database
- Adjust JVM heap allocation

---

## Troubleshooting

### Service Won't Start

**Symptoms:** Container exiting immediately or staying in restart loop

**Solution:**

```bash
# Check logs
docker logs measure-builder-service | head -50

# Common causes:
# 1. Database not responding
docker exec -it hdim-postgres pg_isready

# 2. Redis not accessible
docker exec -it hdim-redis redis-cli ping

# 3. Port already in use
lsof -i :8087  # Check if port is free
```

### Performance Degradation

**Symptoms:** Slow API responses, high CPU/memory

**Diagnosis:**

```bash
# Check active connections
SELECT count(*) FROM pg_stat_activity;

# Check slow queries
SELECT * FROM pg_stat_statements
ORDER BY mean_time DESC LIMIT 10;

# Check cache effectiveness
docker exec hdim-redis redis-cli INFO stats
```

### Database Issues

**Connection Pool Exhaustion:**

```bash
# Too many connections waiting
# Solution:
# 1. Reduce MAXIMUM_POOL_SIZE if overallocated
# 2. Restart service to clear connections
# 3. Check for connection leaks in logs

docker-compose restart measure-builder-service
```

---

## Backup & Recovery

### Backup Strategy

**Daily Backups:**

```bash
#!/bin/bash
BACKUP_DIR="/backups/measure-builder"
mkdir -p $BACKUP_DIR

# Database backup
docker exec hdim-postgres pg_dump -U healthdata quality_db | \
  gzip > $BACKUP_DIR/quality_db_$(date +%Y%m%d_%H%M%S).sql.gz

# Configuration backup
tar -czf $BACKUP_DIR/config_$(date +%Y%m%d_%H%M%S).tar.gz \
  /etc/measure-builder

echo "Backup complete"
```

**Recovery Procedure:**

```bash
# Restore database
gunzip -c /backups/measure-builder/quality_db_TIMESTAMP.sql.gz | \
  docker exec -i hdim-postgres psql -U healthdata quality_db

# Verify restore
docker exec -it hdim-postgres psql -U healthdata -d quality_db \
  -c "SELECT COUNT(*) FROM measures;"
```

### Disaster Recovery

**Steps if complete data loss:**

1. Provision new database server
2. Restore from most recent backup
3. Verify data integrity
4. Run consistency checks
5. Restart all services
6. Verify with stakeholders

---

## Security Checklist

- [ ] HTTPS enabled for all endpoints
- [ ] JWT tokens configured with strong secret
- [ ] Multi-tenant isolation verified
- [ ] HIPAA compliance headers present
- [ ] PHI cache TTL <= 5 minutes
- [ ] Audit logging enabled
- [ ] Database backups encrypted
- [ ] Access logs monitored
- [ ] Firewall rules configured
- [ ] Regular security patches applied

---

## Related Documents

- **User Guide:** `docs/user/guides/MEASURE_BUILDER_GETTING_STARTED.md`
- **Troubleshooting:** `docs/user/guides/MEASURE_BUILDER_TROUBLESHOOTING.md`
- **Deployment:** `docs/runbooks/MEASURE_BUILDER_STAGING_DEPLOYMENT.md`
- **Monitoring:** `docs/monitoring/MEASURE_BUILDER_METRICS_GUIDE.md`

---

**Status:** ✅ Complete
**Last Updated:** January 18, 2026
**Contact:** admin-support@healthdatainmotion.com
