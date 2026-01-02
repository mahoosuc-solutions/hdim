# Phase 6: Operations Runbook

**Status**: ✅ Phase 6 Operations
**Date**: January 1, 2026
**Service**: CMS Connector Service

---

## Quick Reference

### Health Status
```bash
# Check service health
curl https://cms-connector.example.com/api/v1/actuator/health

# View all component health
curl https://cms-connector.example.com/api/v1/actuator/health?show=when_authorized
```

### Common Commands
```bash
# View logs
docker logs -f cms-connector-service --tail 100

# Check metrics
curl https://cms-connector.example.com/api/v1/actuator/prometheus | head -50

# Service stats
docker stats cms-connector-service
```

---

## 1. Daily Checks

### Morning Health Verification
```bash
#!/bin/bash
echo "=== Daily Health Check ==="

# 1. Service Health
HEALTH=$(curl -s https://cms-connector.example.com/api/v1/actuator/health | jq -r '.status')
echo "Service Status: $HEALTH"

# 2. Error Rate Last Hour
ERROR_RATE=$(curl -s http://prometheus:9090/api/v1/query?query='rate(http_requests_total{status="5xx"}[1h])' | jq '.data.result[0].value[1]')
echo "Error Rate: $ERROR_RATE/sec"

# 3. Response Time p95
P95=$(curl -s http://prometheus:9090/api/v1/query?query='histogram_quantile(0.95,rate(http_request_duration_seconds_bucket[1h]))' | jq '.data.result[0].value[1]')
echo "Response Time p95: ${P95}s"

# 4. Database Connections
DB_CONN=$(curl -s http://prometheus:9090/api/v1/query?query='db_pool_active_connections' | jq '.data.result[0].value[1]')
echo "Active DB Connections: $DB_CONN"

# 5. Memory Usage
MEMORY=$(curl -s http://prometheus:9090/api/v1/query?query='jvm_memory_used_bytes{area="heap"}' | jq '.data.result[0].value[1]')
echo "Heap Memory: $((MEMORY / 1024 / 1024)) MB"

# 6. Recent Alerts
echo "Recent Alerts:"
curl -s http://alertmanager:9093/api/v1/alerts | jq '.data[] | {status: .status, labels: {alertname: .labels.alertname}}'
```

### Resolve Issues
- [ ] If any component DOWN, check logs and troubleshoot
- [ ] If error rate high, identify root cause
- [ ] If response time slow, analyze database/cache
- [ ] If memory high, check for leaks
- [ ] If alerts firing, review and resolve

---

## 2. Scaling Operations

### Horizontal Scaling (Add More Instances)

```bash
#!/bin/bash
echo "Scaling CMS Connector Service to 3 instances..."

# Get current instance count
CURRENT=$(docker ps -f "name=cms-connector" --format table | tail -n +2 | wc -l)
echo "Current instances: $CURRENT"

# Start new instance on port 8083
docker run -d \
  --name cms-connector-service-3 \
  --network production-network \
  -p 8083:8081 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db:5432/healthdata_prod \
  -e SPRING_DATASOURCE_USERNAME=healthdata \
  -e SPRING_DATASOURCE_PASSWORD=$(vault kv get -field=password secret/db/prod) \
  registry.example.com/cms-connector-service:latest

# Register with Kong load balancer
curl -X POST http://localhost:8001/upstreams/cms-connector/targets \
  -d "target=localhost:8083" \
  -d "weight=100"

echo "✅ New instance registered"
```

### Vertical Scaling (Increase Resources)

```bash
# Update memory/CPU limits
docker update --memory 2g --cpus 2 cms-connector-service-1

# Or in Kubernetes
kubectl set resources deployment cms-connector \
  --limits=cpu=2,memory=2Gi \
  --requests=cpu=1,memory=1Gi
```

### Downscaling (Remove Instances)

```bash
# Drain connections from instance
curl -X DELETE http://localhost:8001/upstreams/cms-connector/targets/localhost:8083

# Wait for connections to close
sleep 30

# Stop instance
docker stop cms-connector-service-3
docker rm cms-connector-service-3

echo "✅ Instance removed from rotation"
```

---

## 3. Database Administration

### Database Backup
```bash
# Immediate backup
pg_dump -h prod-db -U healthdata healthdata_prod | \
  gzip > /backups/cms-connector-$(date +%Y%m%d_%H%M%S).sql.gz

# Verify backup
gunzip -c /backups/cms-connector-*.sql.gz | head -20
```

### Database Size Check
```bash
psql -h prod-db -U healthdata -d healthdata_prod << EOF
SELECT
  pg_database.datname,
  pg_size_pretty(pg_database_size(pg_database.datname)) as size
FROM pg_database;
EOF
```

### Clear Cache
```bash
# Connect to Redis
redis-cli -h prod-redis -p 6379

# Clear all cache
FLUSHALL

# Or selective clear
DEL patient:*
DEL claims:*
```

### Connection Pool Check
```bash
# View current connections
psql -h prod-db -U healthdata -d healthdata_prod << EOF
SELECT
  datname,
  usename,
  application_name,
  state,
  count(*) as conn_count
FROM pg_stat_activity
GROUP BY datname, usename, application_name, state;
EOF

# Kill hung connections if needed
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = 'healthdata_prod' AND state = 'idle';
```

---

## 4. Log Analysis & Debugging

### View Recent Errors
```bash
# Application logs
docker logs cms-connector-service --since 1h | grep ERROR | tail -20

# Elasticsearch query
curl -s 'http://elasticsearch:9200/logs-*/_search?q=level:ERROR' | \
  jq '.hits.hits[] | {timestamp: ._source.timestamp, message: ._source.message}'
```

### Trace Requests in Jaeger
```bash
# Query Jaeger API
curl 'http://jaeger:16686/api/traces?service=cms-connector-service&limit=20' | jq

# Find slow requests (>500ms)
curl 'http://jaeger:16686/api/traces?service=cms-connector-service&minDuration=500ms' | jq
```

### Performance Investigation
```bash
# Check for slow queries
psql -h prod-db -U healthdata -d healthdata_prod << EOF
SELECT
  query,
  calls,
  total_time,
  mean_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;
EOF
```

### Memory Leak Detection
```bash
# Monitor memory trend
docker stats cms-connector-service --no-stream --format '{{.MemUsage}}'

# If increasing, check for:
# 1. Connection leaks
# 2. Cache growth
# 3. Thread creation

# Enable heap dump
jmap -dump:live,format=b,file=heap.bin $(pgrep -f java)
```

---

## 5. Certificate Management

### Certificate Expiration Check
```bash
# Check certificate expiration
openssl s_client -connect cms-connector.example.com:443 </dev/null | \
  openssl x509 -noout -dates

# Expected output:
# notBefore=Jan  1 00:00:00 2024 GMT
# notAfter=Jan  1 23:59:59 2025 GMT

# Alert if expiring within 30 days
EXPIRY=$(openssl s_client -connect cms-connector.example.com:443 </dev/null | \
  openssl x509 -noout -dates | grep notAfter | cut -d= -f2)
DAYS_LEFT=$(( ($(date -d "$EXPIRY" +%s) - $(date +%s)) / 86400 ))
echo "Days until expiration: $DAYS_LEFT"
```

### Renew Certificate (Let's Encrypt)
```bash
# Renew certificate
certbot renew --force-renewal -d cms-connector.example.com

# If renewal successful, reload Kong
# Kong will automatically pick up the updated certificate
curl -X POST http://localhost:8001/config \
  -d '{"_format_version":"3"}'

# Verify new certificate
openssl s_client -connect cms-connector.example.com:443 </dev/null | \
  openssl x509 -noout -dates
```

---

## 6. Dependency Updates

### Check for Updates
```bash
# Maven dependency updates
mvn versions:display-dependency-updates

# Docker base image updates
docker pull eclipse-temurin:21-jdk-focal
docker run eclipse-temurin:21-jdk-focal java -version
```

### Apply Updates Safely
```bash
# 1. Update dependencies
mvn versions:update-dependency-versions

# 2. Test locally
./gradlew test

# 3. Test with Docker
docker build -t cms-connector-service:test .

# 4. Deploy to staging first
docker-compose -f docker-compose.staging.yml up -d

# 5. Run test suite on staging
bash tests/security/run-security-tests.sh http://localhost:8081

# 6. If all tests pass, deploy to production
# See PRODUCTION-DEPLOYMENT.md
```

---

## 7. Common Issues & Solutions

### Issue: High Memory Usage
```bash
# Symptoms: Memory growing over time
# Solution:

# 1. Check for connection leaks
curl https://cms-connector.example.com/api/v1/actuator/metrics/db.pool.active | jq

# 2. Force garbage collection (Java)
jcmd $(pgrep -f java) GC.run

# 3. Monitor after restart
docker restart cms-connector-service
docker stats cms-connector-service

# 4. If still high, analyze heap dump
# See "Memory Leak Detection" above
```

### Issue: Slow Response Times
```bash
# Symptoms: p95 > 200ms, p99 > 500ms
# Solution:

# 1. Check database connection pool
curl https://cms-connector.example.com/api/v1/actuator/metrics/db.pool.active | jq

# 2. Check for slow queries
# See "Performance Investigation" above

# 3. Check cache hit rate
curl https://cms-connector.example.com/api/v1/actuator/metrics/cache.hits | jq

# 4. Increase cache or connection pool
docker update cms-connector-service --env SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE=20

# 5. Monitor and validate
curl https://cms-connector.example.com/api/v1/actuator/metrics/http.request.duration | jq
```

### Issue: High Error Rate
```bash
# Symptoms: >1% errors for 5+ minutes
# Solution:

# 1. Check application logs
docker logs cms-connector-service --tail 100 | grep ERROR

# 2. Check database connectivity
curl https://cms-connector.example.com/api/v1/actuator/health/db | jq

# 3. Check external service connectivity
curl https://cms-connector.example.com/api/v1/actuator/health | jq '.components'

# 4. If transient, monitor
watch -n 5 'curl -s http://prometheus:9090/api/v1/query?query=rate\(http_requests_total\{status=\"5xx\"\}\[5m\]\) | jq'

# 5. If persistent, consider rollback
# See PRODUCTION-DEPLOYMENT.md Rollback Procedures
```

### Issue: Database Connection Pool Exhausted
```bash
# Symptoms: "Connection pool exhausted" errors
# Solution:

# 1. Check current connections
curl https://cms-connector.example.com/api/v1/actuator/metrics/db.pool.active | jq '.measurements[0].value'

# 2. Check max pool size
# Default: 10, Recommended: 20

# 3. Increase pool size (requires restart)
docker update cms-connector-service \
  --env SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE=30

docker restart cms-connector-service

# 4. Monitor connection count
watch -n 2 'curl -s https://cms-connector.example.com/api/v1/actuator/metrics/db.pool.active | jq'
```

### Issue: Service Won't Start
```bash
# Symptoms: Container exits immediately
# Solution:

# 1. Check logs
docker logs cms-connector-service

# 2. Common causes and fixes:
#    a. Database migration failed
#       → Check database connectivity
#       → Review migration files
#    b. Port already in use
#       → Kill process: lsof -i :8081 | kill -9
#    c. Out of memory
#       → Increase JVM memory: -Xmx1g
#    d. Configuration error
#       → Verify environment variables
#       → Check vault secrets access

# 3. Start in foreground for debugging
docker run -it cms-connector-service
```

---

## 8. Performance Tuning

### Database Query Optimization
```bash
# Enable query stats
psql -h prod-db -U healthdata -d healthdata_prod << EOF
CREATE EXTENSION pg_stat_statements;
EOF

# Find slow queries
SELECT
  query,
  calls,
  total_time,
  mean_time
FROM pg_stat_statements
WHERE mean_time > 100
ORDER BY mean_time DESC;
```

### Cache Optimization
```bash
# Monitor cache hit rate
curl https://cms-connector.example.com/api/v1/actuator/metrics/cache.hit | jq

# If hit rate < 80%:
# 1. Increase cache TTL
# 2. Increase cache size
# 3. Review cache key strategy

# Check cache statistics
redis-cli -h prod-redis INFO stats
```

### Connection Pool Tuning
```bash
# Current settings
echo "Max connections: $(curl -s https://cms-connector.example.com/api/v1/actuator/env | jq '.propertySources[] | select(.name | contains("datasource")) | .properties.maximum_pool_size')"

# Optimal settings:
# Core = CPU_CORES
# Max = CPU_CORES * 2 + 1
# Recommended: 8-20 for production
```

---

## 9. On-Call Procedures

### Upon Alert
1. [ ] Acknowledge alert in AlertManager
2. [ ] Check service health: `curl https://cms-connector.example.com/api/v1/actuator/health`
3. [ ] View recent logs: `docker logs -f cms-connector-service --tail 50`
4. [ ] Check metrics in Grafana
5. [ ] Determine if rollback needed
6. [ ] If unable to resolve, page the on-call manager

### During Outage
1. [ ] Declare incident in #incidents channel
2. [ ] Establish incident commander
3. [ ] Update status page
4. [ ] Begin investigation
5. [ ] Implement fix or rollback
6. [ ] Monitor for stability
7. [ ] Close incident
8. [ ] Schedule post-mortem

---

## 10. Maintenance Windows

### Scheduled Maintenance
```bash
# 1. Announce maintenance (24 hour notice)
# 2. Set status page to "Maintenance"
# 3. Drain connections
# 4. Perform maintenance
# 5. Validate service
# 6. Update status page
# 7. Monitor for issues
```

### Backup Maintenance
```bash
# Daily: Full backup
pg_dump -h prod-db -U healthdata healthdata_prod | gzip > /backups/daily.sql.gz

# Weekly: Test restore
gunzip -c /backups/daily.sql.gz | psql -U healthdata -d restore_test_db

# Monthly: Archive old backups
find /backups -mtime +90 -delete
```

---

## Quick Links

- **Grafana Dashboards**: http://grafana.example.com:3000
- **Prometheus Queries**: http://prometheus.example.com:9090
- **Jaeger Traces**: http://jaeger.example.com:16686
- **Kibana Logs**: http://kibana.example.com:5601
- **Kong Admin**: http://kong-admin.example.com:8001
- **AlertManager**: http://alertmanager.example.com:9093

---

**Document Version**: 1.0
**Last Updated**: January 1, 2026
**Status**: ✅ Phase 6 Operations - Ready for Production
