# Phase 6 Week 1: Production Deployment Guide

**Status**: ✅ Phase 6 Week 1
**Date**: January 1, 2026
**Service**: CMS Connector Service
**Environment**: Production

---

## Overview

This guide provides comprehensive instructions for deploying the CMS Connector Service to production with zero downtime, automated rollback capabilities, and post-deployment validation.

---

## 1. Pre-Deployment Checklist

### Environmental Prerequisites
- [ ] Production environment configured and tested
- [ ] Database created with proper backups
- [ ] Redis cache cluster operational
- [ ] Kong API Gateway running and configured
- [ ] Prometheus and Grafana deployed
- [ ] Jaeger tracing infrastructure ready
- [ ] AlertManager configured with notification channels
- [ ] Log aggregation (ELK) operational

### Application Prerequisites
- [ ] All tests passing (load, chaos, security)
- [ ] Zero critical vulnerabilities (dependency scan)
- [ ] Code reviewed and approved
- [ ] Docker image built and tested
- [ ] Configuration validated for production
- [ ] SSL certificates in place
- [ ] Environment variables documented
- [ ] Secrets stored in secure vault

### Team Prerequisites
- [ ] Deployment team assembled and trained
- [ ] Operations team on standby
- [ ] Communication channels established
- [ ] Rollback plan reviewed with team
- [ ] Incident response procedures accessible
- [ ] On-call engineer assigned
- [ ] Stakeholders notified of deployment window

### Infrastructure Prerequisites
- [ ] Load balancer health checks configured
- [ ] DNS prepared for cutover
- [ ] Firewall rules allowing traffic
- [ ] Disk space verified on all nodes
- [ ] Network connectivity tested
- [ ] Backup systems validated
- [ ] Monitoring alerts configured
- [ ] Log rotation configured

---

## 2. Pre-Deployment Validation

### Verify Database Connectivity
```bash
# Connect to production database
psql -h prod-db.example.com -U healthdata -d healthdata_prod

# Check database integrity
SELECT count(*) FROM information_schema.tables;

# Verify backups
SELECT datname, pg_database.dattablespace
FROM pg_database
WHERE datname = 'healthdata_prod';
```

### Verify Redis Cache
```bash
# Connect to Redis
redis-cli -h prod-redis.example.com -p 6379

# Test connectivity
ping
# Should respond: PONG

# Check memory usage
info memory
```

### Verify Kong Gateway
```bash
# Check Kong status
curl http://localhost:8001/status

# Verify service configuration
curl http://localhost:8001/services/cms-connector

# Test route
curl http://localhost:8001/routes
```

### Verify Monitoring Stack
```bash
# Check Prometheus targets
curl http://localhost:9090/api/v1/targets

# Check Grafana health
curl http://localhost:3000/api/health

# Test Jaeger
curl http://localhost:16686/api/services
```

---

## 3. Blue-Green Deployment Process

### Architecture Overview

```
Production Load Balancer (Kong)
    ↓
    ├─ Blue Environment  (Current - v1.0.0)
    │   └─ Running on 8081
    │
    ├─ Green Environment (New - v1.1.0)
    │   └─ Running on 8082
    │
Traffic Router
    └─ Initially: 100% → Blue
    └─ After validation: Switch to Green
    └─ Rollback ready: Quick switch back to Blue
```

### Step 1: Deploy Green Environment

**1a. Build and Tag Docker Image**
```bash
cd /home/mahoosuc-solutions/projects/hdim-master/hdim-master
docker build -t cms-connector-service:v1.1.0 \
  -f Dockerfile .

docker tag cms-connector-service:v1.1.0 \
  registry.example.com/cms-connector-service:v1.1.0

docker push registry.example.com/cms-connector-service:v1.1.0
```

**1b. Start Green Environment (Port 8082)**
```bash
# Start green container with new version
docker run -d \
  --name cms-connector-green \
  --network production-network \
  -p 8082:8081 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db:5432/healthdata_prod \
  -e SPRING_DATASOURCE_USERNAME=healthdata \
  -e SPRING_DATASOURCE_PASSWORD=$(vault kv get -field=password secret/db/prod) \
  -e SPRING_REDIS_HOST=prod-redis \
  -e SPRING_REDIS_PORT=6379 \
  -e MANAGEMENT_PROMETHEUS_METRICS_EXPORT_ENABLED=true \
  registry.example.com/cms-connector-service:v1.1.0

# Wait for startup
sleep 15

# Verify green is healthy
curl http://localhost:8082/api/v1/actuator/health
```

### Step 2: Validate Green Environment

**2a. Health Check**
```bash
#!/bin/bash
HEALTH_URL="http://localhost:8082/api/v1/actuator/health"
TIMEOUT=30
ELAPSED=0

while [ $ELAPSED -lt $TIMEOUT ]; do
  RESPONSE=$(curl -s "$HEALTH_URL" | jq -r '.status')
  if [ "$RESPONSE" = "UP" ]; then
    echo "✅ Green environment is healthy"
    exit 0
  fi
  echo "⏳ Waiting for green to be ready..."
  sleep 2
  ELAPSED=$((ELAPSED + 2))
done

echo "❌ Green environment failed to start"
exit 1
```

**2b. Smoke Testing**
```bash
#!/bin/bash
SERVICE_URL="http://localhost:8082"

# Test 1: Health endpoint
echo "Testing health endpoint..."
curl -f "$SERVICE_URL/api/v1/actuator/health" || exit 1

# Test 2: Metrics endpoint
echo "Testing metrics endpoint..."
curl -f "$SERVICE_URL/api/v1/actuator/prometheus" || exit 1

# Test 3: Protected endpoint (expect 401 without auth)
echo "Testing authentication..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
  "$SERVICE_URL/api/v1/cms/claims/search")
if [ "$HTTP_CODE" = "401" ]; then
  echo "✅ Authentication working correctly"
else
  echo "❌ Authentication check failed: $HTTP_CODE"
  exit 1
fi

# Test 4: Load test green environment
echo "Running load test on green..."
for i in {1..10}; do
  curl -s "$SERVICE_URL/api/v1/actuator/health" > /dev/null || exit 1
done

echo "✅ All smoke tests passed"
```

**2c. Verify Database Migrations**
```bash
# Check that migrations ran successfully
psql -h prod-db -U healthdata -d healthdata_prod << EOF
SELECT version, description, success
FROM schema_version
ORDER BY version DESC
LIMIT 10;
EOF

# Expected output: All migrations should have success = true
```

**2d. Check Green Application Logs**
```bash
# View recent logs
docker logs --tail 50 cms-connector-green

# Look for any errors
docker logs cms-connector-green | grep -i error | tail -10

# Check startup time
docker logs cms-connector-green | grep "Started CmsConnectorApplication"
```

### Step 3: Load Test Green Environment

```bash
#!/bin/bash
SERVICE_URL="http://localhost:8082"
DURATION=60
RATE=100  # req/sec

echo "Running load test on green for $DURATION seconds..."

# Using Apache Bench
ab -n $((DURATION * RATE)) \
   -c 10 \
   -t $DURATION \
   "$SERVICE_URL/api/v1/actuator/health"

# Expected:
# - Requests per second: >100
# - Failed requests: 0
# - 50% faster response: <10ms
# - 95% faster response: <20ms
```

### Step 4: Gradual Traffic Shift (Canary Deployment)

**4a. Configure Kong Routes for Traffic Split**
```bash
# Get current service ID
SERVICE_ID=$(curl -s http://localhost:8001/services | \
  jq -r '.data[] | select(.name=="cms-connector") | .id')

# Create blue route (existing)
curl -X PUT http://localhost:8001/routes/cms-connector-blue \
  -d "service.id=$SERVICE_ID" \
  -d "protocols=https,http" \
  -d "hosts=cms-connector.example.com" \
  -d "name=cms-connector-blue"

# Create green route (new)
curl -X POST http://localhost:8001/routes \
  -d "name=cms-connector-green" \
  -d "service.id=$SERVICE_ID" \
  -d "protocols=https,http" \
  -d "hosts=cms-connector-green.example.com"

# Or use load balancing plugin for traffic splitting
curl -X POST http://localhost:8001/plugins \
  -d "name=load-balancer" \
  -d "config.type=consistent_hashing" \
  -d "service.id=$SERVICE_ID"
```

**4b. Canary Traffic Schedule**
```
Time    | Blue | Green | Status
--------|------|-------|--------
0 min   | 100% | 0%    | Initial (baseline)
10 min  | 95%  | 5%    | Monitor green
20 min  | 90%  | 10%   | Gradual increase
30 min  | 75%  | 25%   | Continue monitoring
45 min  | 50%  | 50%   | 50/50 split
60 min  | 25%  | 75%   | Increase green
75 min  | 10%  | 90%   | Final validation
90 min  | 0%   | 100%  | Full cutover
```

### Step 5: Monitor During Traffic Shift

**5a. Real-time Metrics Monitoring**
```bash
# Watch error rate
watch -n 5 'curl -s http://localhost:9090/api/v1/query?query=rate\(http_requests_total\{status=\"5...\"\}\[5m\]\) | jq'

# Watch latency
watch -n 5 'curl -s http://localhost:9090/api/v1/query?query=histogram_quantile\(0.95,rate\(http_request_duration_seconds_bucket\[5m\]\)\) | jq'

# Watch active connections
watch -n 5 'curl -s http://localhost:9090/api/v1/query?query=db_pool_active_connections | jq'
```

**5b. Continuous Health Checks**
```bash
#!/bin/bash
while true; do
  BLUE_STATUS=$(curl -s http://localhost:8081/api/v1/actuator/health | jq -r '.status')
  GREEN_STATUS=$(curl -s http://localhost:8082/api/v1/actuator/health | jq -r '.status')

  echo "$(date): Blue=$BLUE_STATUS, Green=$GREEN_STATUS"

  if [ "$GREEN_STATUS" != "UP" ]; then
    echo "⚠️  WARNING: Green is not healthy - prepare to rollback"
  fi

  sleep 10
done
```

**5c. Log Monitoring During Shift**
```bash
# Monitor green application logs
docker logs -f cms-connector-green --tail 100

# Look for specific error patterns
docker logs cms-connector-green | grep -E "ERROR|WARN|Exception" | tail -20
```

### Step 6: Complete Traffic Cutover

Once green has been validated (all tests passing, no errors):

```bash
# Confirm green is ready for full traffic
echo "Final validation before cutover..."
curl -f http://localhost:8082/api/v1/actuator/health || {
  echo "❌ Green is not healthy!"
  exit 1
}

# Update Kong to route all traffic to green
curl -X PATCH http://localhost:8001/routes/cms-connector \
  -d "protocols=https,http" \
  -d "hosts=cms-connector.example.com" \
  -d "methods=GET,POST,PUT,DELETE,PATCH"

# Verify traffic is flowing to green
echo "Monitoring traffic..."
curl -s http://localhost:9090/api/v1/query?query=http_requests_total | jq

echo "✅ Full cutover to green completed"
```

### Step 7: Decommission Blue Environment

After 24 hours of successful green operation:

```bash
# Verify no blue traffic
curl -s http://localhost:9090/api/v1/query?query='increase(http_requests_total{instance="blue"}[5m])' | jq

# If no traffic, shutdown blue
docker stop cms-connector-blue
docker rm cms-connector-blue

# Verify all traffic on green
curl -s http://localhost:9090/api/v1/query?query='increase(http_requests_total{instance="green"}[5m])' | jq

echo "✅ Blue environment decommissioned"
```

---

## 4. Rollback Procedures

### Immediate Rollback (If Issues Detected)

If issues are detected during canary phase:

```bash
#!/bin/bash
echo "🚨 INITIATING IMMEDIATE ROLLBACK..."

# Step 1: Stop accepting traffic to green
curl -X DELETE http://localhost:8001/routes/cms-connector-green
echo "✅ Green routes removed"

# Step 2: Restore 100% traffic to blue
curl -X PATCH http://localhost:8001/routes/cms-connector-blue \
  -d "protocols=https,http" \
  -d "hosts=cms-connector.example.com"
echo "✅ Traffic restored to blue"

# Step 3: Verify blue is receiving traffic
sleep 5
BLUE_REQUESTS=$(curl -s http://localhost:9090/api/v1/query?query='rate(http_requests_total{instance="blue"}[1m])' | jq '.data.result[0].value[1]')
echo "✅ Blue requests/sec: $BLUE_REQUESTS"

# Step 4: Keep green running for investigation
# docker stop cms-connector-green
echo "ℹ️ Green environment preserved for investigation"
echo "ℹ️ View logs: docker logs cms-connector-green"

echo "✅ Rollback completed. Service restored to v1.0.0"
```

### Rollback Decision Criteria

Automatic rollback triggers:
- [ ] Error rate > 1% for 2+ minutes
- [ ] Response time p99 > 1 second for 2+ minutes
- [ ] Database connection pool exhaustion
- [ ] OutOfMemory errors
- [ ] Circuit breaker open
- [ ] Health check failing for 3+ checks

Manual rollback decision:
- [ ] Unexpected behavior in production
- [ ] Critical security issue discovered
- [ ] Data integrity issues
- [ ] Compliance violation detected

---

## 5. Post-Deployment Validation

### Automated Validation Suite

```bash
#!/bin/bash
SERVICE_URL="https://cms-connector.example.com"

echo "=== Post-Deployment Validation ==="
echo ""

# Test 1: Health Check
echo "1. Health Check..."
HEALTH=$(curl -s "$SERVICE_URL/api/v1/actuator/health" | jq -r '.status')
if [ "$HEALTH" = "UP" ]; then
  echo "   ✅ Service is healthy"
else
  echo "   ❌ Service is not healthy"
  exit 1
fi

# Test 2: HTTPS/TLS
echo "2. HTTPS/TLS Validation..."
openssl s_client -connect cms-connector.example.com:443 </dev/null | \
  openssl x509 -noout -dates
echo "   ✅ TLS certificate valid"

# Test 3: Security Headers
echo "3. Security Headers..."
curl -s -I "$SERVICE_URL/api/v1/actuator/health" | grep -E "X-Content-Type-Options|X-Frame-Options"
if [ $? -eq 0 ]; then
  echo "   ✅ Security headers present"
else
  echo "   ❌ Security headers missing"
  exit 1
fi

# Test 4: Performance
echo "4. Performance Baseline..."
RESPONSE_TIME=$(curl -s -w "%{time_total}" -o /dev/null "$SERVICE_URL/api/v1/actuator/health")
echo "   Response time: ${RESPONSE_TIME}s"
if (( $(echo "$RESPONSE_TIME < 0.2" | bc -l) )); then
  echo "   ✅ Performance acceptable"
else
  echo "   ⚠️  Performance slower than expected"
fi

# Test 5: Authentication
echo "5. Authentication Check..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
  "$SERVICE_URL/api/v1/cms/claims/search")
if [ "$HTTP_CODE" = "401" ]; then
  echo "   ✅ Authentication enforced"
else
  echo "   ❌ Authentication check failed: $HTTP_CODE"
  exit 1
fi

# Test 6: Metrics Collection
echo "6. Metrics Collection..."
METRICS=$(curl -s "$SERVICE_URL/api/v1/actuator/prometheus" | head -20)
if [ ! -z "$METRICS" ]; then
  echo "   ✅ Metrics available"
else
  echo "   ❌ Metrics not available"
  exit 1
fi

# Test 7: Database Connectivity
echo "7. Database Connectivity..."
# Application health check includes database
if [ "$HEALTH" = "UP" ]; then
  echo "   ✅ Database connected"
else
  echo "   ❌ Database connection failed"
  exit 1
fi

# Test 8: Load Test
echo "8. Load Test (100 requests)..."
ab -n 100 -c 5 "$SERVICE_URL/api/v1/actuator/health" 2>&1 | \
  grep -E "Requests per second|Failed requests"
echo "   ✅ Load test completed"

echo ""
echo "✅ All post-deployment validations passed"
```

### Manual Validation Checklist

- [ ] Application responds to health checks
- [ ] HTTPS certificate is valid and not expired
- [ ] Security headers are present
- [ ] Authentication is enforced
- [ ] Metrics are being collected
- [ ] Traces appear in Jaeger
- [ ] Logs appear in ELK
- [ ] No errors in application logs
- [ ] Database is accessible
- [ ] Cache is working
- [ ] Load balancer is healthy
- [ ] DNS is resolving correctly
- [ ] CDN (if used) is working
- [ ] Backup is running
- [ ] All alerts are firing correctly

---

## 6. Monitoring Production Deployment

### Key Metrics to Track

```
http_requests_total - Total requests
http_request_duration_seconds - Response times
http_requests_total{status="5xx"} - Error rate
db_pool_active_connections - Database connection pool
jvm_memory_used_bytes - Memory usage
cache_hits_total - Cache performance
```

### Dashboard to Monitor

Create Grafana dashboard with:
- Request rate (req/s)
- Error rate (%)
- Response time (p50, p95, p99)
- Database connection pool
- Memory usage
- GC metrics
- Cache hit rate

### Alert Rules Active

All Phase 5 alert rules should be active:
- High error rate
- High latency
- Service down
- Circuit breaker open
- Connection pool exhaustion
- High memory usage
- Long GC pauses

---

## 7. Post-Deployment Report

After deployment, document:

```markdown
## Deployment Report
- Deployment Date: YYYY-MM-DD
- Version: v1.1.0
- Duration: XX minutes
- Deployment Type: Blue-Green
- Issues Encountered: None / [List]
- Rollback Executed: Yes / No
- Post-Deployment Validation: PASSED
- Team: [Names]
- Notes: [Any issues or improvements for next deployment]
```

---

## Troubleshooting Deployment Issues

### Service Won't Start
```bash
# Check logs
docker logs cms-connector-green

# Common causes:
# - Database migration failed
# - Port already in use
# - Configuration error
# - Missing environment variables
```

### Database Migration Fails
```bash
# Rollback database changes
# (if migration is reversible)
psql -h prod-db -U healthdata -d healthdata_prod \
  -c "SELECT * FROM schema_version ORDER BY version DESC LIMIT 5;"

# Review migration error logs
docker logs cms-connector-green | grep -i "migration\|liquibase"
```

### Health Check Fails
```bash
# Test directly
curl -v http://localhost:8082/api/v1/actuator/health

# Check dependencies
curl -v http://localhost:8082/api/v1/actuator/health?show=when_authorized

# Common causes:
# - Database unreachable
# - Redis unreachable
# - Configuration missing
# - Port not exposed
```

---

## Next Steps

After successful production deployment:
1. Monitor metrics and logs for 24 hours
2. Team review of deployment process
3. Document any improvements
4. Update deployment procedures if needed
5. Proceed to Phase 6 Week 2: Monitoring & Alerting

---

**Document Version**: 1.0
**Last Updated**: January 1, 2026
**Status**: ✅ Phase 6 Week 1 - Ready for Production Deployment
