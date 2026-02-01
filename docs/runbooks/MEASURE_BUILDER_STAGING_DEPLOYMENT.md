# Measure Builder Staging Deployment Runbook

**Status:** Production-Ready
**Version:** 1.0
**Last Updated:** January 18, 2026
**Author:** HDIM Deployment Team

---

## Executive Summary

This runbook provides step-by-step instructions for deploying the Measure Builder system to the staging environment. The Measure Builder has completed all development, testing, and validation phases and is approved for staging deployment.

**Key Facts:**
- All 225+ tests passing ✅
- Performance benchmarks met ✅
- Security validated ✅
- Accessibility compliant (WCAG 2.1 AA) ✅
- Production-ready ✅

---

## Pre-Deployment Checklist

### Infrastructure Requirements

- [ ] Staging environment running (Docker Compose or Kubernetes)
- [ ] PostgreSQL 16 with 29 databases initialized
- [ ] Redis 7 running and accessible
- [ ] Kafka 3.x cluster running
- [ ] Network connectivity validated between services
- [ ] Disk space: 50GB minimum available
- [ ] Memory: 16GB minimum available
- [ ] CPU: 8 cores minimum available

### Access & Credentials

- [ ] SSH access to staging environment
- [ ] Docker registry credentials available
- [ ] Database admin credentials available
- [ ] Staging domain/IP address documented
- [ ] SSL certificates installed (if using HTTPS)
- [ ] Environment variables prepared (.env.staging file)

### Team Preparation

- [ ] Deployment team trained and available
- [ ] On-call engineer assigned
- [ ] Communication channels open (Slack #measure-builder-deployment)
- [ ] Stakeholders notified of deployment window
- [ ] Rollback plan reviewed by team

### Code & Artifacts

- [ ] Latest source code pulled and verified
- [ ] Docker images built and tested locally
- [ ] Migrations reviewed and validated
- [ ] Configuration files prepared for staging
- [ ] Deployment scripts reviewed

### Monitoring & Alerting

- [ ] Monitoring dashboards created
- [ ] Alert thresholds configured
- [ ] Logging aggregation configured
- [ ] Error tracking (Sentry/similar) configured
- [ ] Performance baselines established

---

## Pre-Deployment Validation

Run the staging validation script to verify all systems are ready:

```bash
# Navigate to project root
cd /path/to/hdim-master

# Run comprehensive validation
./scripts/validate-measure-builder-staging.sh --verbose

# Check output for any failures
# All tests should show ✅ PASS status
```

**Expected Output:**

```
✅ Service Health Checks: All services responding
✅ Database Connectivity: PostgreSQL connected
✅ Authentication: JWT tokens generating
✅ Measure Builder APIs: All endpoints accessible
✅ Multi-tenant Isolation: X-Tenant-ID enforced
✅ Security Headers: All headers present
✅ Cache Control: HIPAA headers present
✅ Performance: Response times within budget
```

If any checks fail, **STOP** and investigate before proceeding.

---

## Deployment Steps

### Step 1: Prepare Environment (10 minutes)

**1.1 Verify staging environment status**

```bash
# SSH to staging server
ssh ubuntu@staging.hdim.local

# Check Docker status
docker ps
docker-compose ps

# Verify services are running
curl -s http://localhost:8001/health | jq .
```

**1.2 Create backup of current deployment**

```bash
# Backup database
docker exec hdim-postgres pg_dump -U healthdata quality_db | \
  gzip > backup_quality_db_$(date +%Y%m%d_%H%M%S).sql.gz

# Backup current Docker Compose
cp docker-compose.staging.yml docker-compose.staging.yml.backup
```

**1.3 Prepare environment variables**

```bash
# Copy environment template
cp .env.example .env.staging

# Edit with staging-specific values
nano .env.staging

# Key variables to set:
# ENVIRONMENT=staging
# ENABLE_MEASURE_BUILDER=true
# MEASURE_BUILDER_MAX_COMPLEXITY=10
# DEBUG=true (for staging)
# MONITORING_SAMPLING=0.5 (50% sampling)
```

### Step 2: Deploy Services (15-20 minutes)

**2.1 Pull latest code**

```bash
git fetch origin
git checkout feature/event-sourced-fhir-engine  # or master after merge
git pull origin

# Verify code is clean
git status
git log --oneline -5
```

**2.2 Build Docker images**

```bash
# Build all services
docker-compose -f docker-compose.staging.yml build

# Expected build time: 10-15 minutes
# Watch for build errors - fail fast if issues detected
```

**2.3 Stop current services (zero-downtime if possible)**

```bash
# For zero-downtime, use canary deployment:
# 1. Start new containers on different ports
# 2. Validate they work
# 3. Switch traffic
# 4. Stop old containers

# For this deployment, stop current services:
docker-compose -f docker-compose.staging.yml down --remove-orphans

# Verify stopped
docker ps | grep clinical-portal || echo "Services stopped"
```

**2.4 Start new services**

```bash
# Start services with new images
docker-compose -f docker-compose.staging.yml up -d

# Watch startup process
docker-compose -f docker-compose.staging.yml logs -f

# Expected startup time: 5-10 minutes (depends on DB migrations)
```

**2.5 Verify services are healthy**

```bash
# Wait for services to be ready
sleep 30

# Run health checks
docker-compose -f docker-compose.staging.yml ps

# Expected output: all containers in "Up" state
# Watch for any "restarting" or "exited" status

# Check service health endpoints
curl -s http://localhost:8001/health | jq .
curl -s http://localhost:8087/quality-measure/health | jq .
curl -s http://localhost:8085/fhir/health | jq .
```

### Step 3: Database Migration (5-10 minutes)

**3.1 Verify Liquibase migrations**

```bash
# Check migration logs
docker logs hdim-quality-measure-service 2>&1 | grep -i "liquibase\|migration"

# Expected: "Liquibase: executed successfully"
```

**3.2 Verify database schema**

```bash
# Connect to database
docker exec -it hdim-postgres psql -U healthdata -d quality_db

# Check tables exist
\dt

# Check for measure_builder related tables
\dt *measure*

# Exit psql
\q
```

### Step 4: Post-Deployment Validation (20-30 minutes)

**4.1 Run comprehensive validation script**

```bash
./scripts/validate-measure-builder-staging.sh --verbose --report-dir ./reports

# Wait for completion (5 minutes)
# Check report output
cat ./reports/MEASURE_BUILDER_STAGING_VALIDATION_*.md
```

**4.2 Test measure builder workflow (manual)**

```bash
# Open staging portal
open http://staging.hdim.local/pages/measure-builder
# or
curl -s http://localhost:4200/pages/measure-builder

# Test flow:
# 1. Create new measure
# 2. Add algorithm blocks
# 3. Configure sliders (range, distribution, period)
# 4. Generate CQL
# 5. Publish measure
```

**4.3 Verify multi-tenant isolation**

```bash
# Test with different tenants
TENANT1="TENANT001"
TENANT2="TENANT002"

# Create measure in TENANT1
curl -X POST http://localhost:8087/api/v1/measures \
  -H "X-Tenant-ID: $TENANT1" \
  -H "Content-Type: application/json" \
  -d '{"name": "Test Measure", "description": "Staging test"}'

# Verify TENANT2 cannot access
curl http://localhost:8087/api/v1/measures \
  -H "X-Tenant-ID: $TENANT2" | jq '.items | length'
# Should return 0
```

**4.4 Performance baseline test**

```bash
# Test response times
time curl -s http://localhost:8087/api/v1/measures?limit=10 > /dev/null

# Should complete in <500ms
# Test with different measure complexities
```

**4.5 Security header verification**

```bash
# Verify security headers
curl -i http://staging.hdim.local | grep -i \
  "cache-control\|content-security-policy\|x-frame-options"

# Should see appropriate headers
```

### Step 5: Stakeholder Sign-off (10 minutes)

**5.1 Notify stakeholders**

Send deployment report to stakeholders:

```bash
# Send report
mail -s "Measure Builder Staging Deployment Complete" \
  stakeholders@company.com < ./reports/MEASURE_BUILDER_STAGING_VALIDATION_*.md
```

**5.2 Collect sign-offs**

- [ ] QA Team: Regression testing complete
- [ ] Product Manager: Feature verification complete
- [ ] Security Team: Security review complete
- [ ] Architecture: Design review complete
- [ ] Release Manager: Approval to proceed

---

## Post-Deployment Monitoring (24-48 hours)

### Continuous Monitoring

**Every 15 minutes (first 2 hours):**

```bash
# Check service health
docker-compose ps

# Check logs for errors
docker-compose logs --since 15m | grep ERROR

# Monitor key metrics
curl -s http://localhost:9090/api/v1/query?query=up | jq .
```

**Every hour (first 24 hours):**

```bash
# Monitor performance metrics
curl -s http://localhost:9090/api/v1/query?query=http_request_duration_seconds | jq .

# Check error rates
curl -s http://localhost:9090/api/v1/query?query=http_requests_total{status="5xx"} | jq .

# Check resource usage
docker stats --no-stream
```

### Key Metrics to Monitor

| Metric | Threshold | Action |
|--------|-----------|--------|
| CPU Usage | >80% | Scale up resources |
| Memory Usage | >85% | Restart container, investigate leak |
| Error Rate | >1% | Page on-call engineer |
| API Response Time P95 | >500ms | Investigate performance regression |
| Database Connection Pool | >90% used | Increase pool size |
| Disk Usage | >85% | Expand disk, clean logs |

### Alert Configuration

Set up alerts in your monitoring system:

```yaml
# Example Prometheus alert (update for your system)
groups:
  - name: measure-builder-staging
    rules:
      - alert: HighErrorRate
        expr: rate(http_requests_total{status="5xx"}[5m]) > 0.01
        for: 5m
        annotations:
          summary: "High error rate in Measure Builder"

      - alert: SlowAPIResponse
        expr: histogram_quantile(0.95, http_request_duration_seconds) > 0.5
        for: 5m
        annotations:
          summary: "Slow API responses detected"

      - alert: ServiceDown
        expr: up{job="measure-builder"} == 0
        for: 1m
        annotations:
          summary: "Measure Builder service is down"
```

### Logging & Error Tracking

1. Monitor application logs for unusual patterns
2. Check error tracking system (Sentry/similar) for new errors
3. Review user feedback from staging testers
4. Collect performance metrics for optimization

---

## Rollback Procedure

If critical issues are discovered during or after deployment:

### Immediate Response (< 5 minutes)

1. **Alert on-call engineer**
   ```bash
   # Send alert
   echo "CRITICAL: Measure Builder deployment issue detected" | mail -s "CRITICAL ALERT" oncall@company.com
   ```

2. **Assess severity**
   - Is the service completely down?
   - Are users unable to create/save measures?
   - Is data being corrupted?

3. **Decision tree:**
   - **Continue:** Issue is minor, fix can be applied without rollback
   - **Pause:** Issue is moderate, apply fix and retest
   - **Rollback:** Issue is critical, revert to previous version

### Rollback Steps (5-10 minutes)

**4.1 Stop current services**

```bash
docker-compose -f docker-compose.staging.yml down
```

**4.2 Restore previous version**

```bash
# Use previous Docker images (tagged with version)
docker-compose -f docker-compose.staging.yml.backup up -d

# Or rollback to previous commit
git checkout HEAD~1  # Go back one commit
docker-compose -f docker-compose.staging.yml build
docker-compose -f docker-compose.staging.yml up -d
```

**4.3 Restore database backup (if needed)**

```bash
# List available backups
ls -la backup_quality_db_*.sql.gz

# Restore from backup
docker exec -i hdim-postgres psql -U healthdata quality_db < \
  backup_quality_db_TIMESTAMP.sql.gz

# Verify data integrity
docker exec -it hdim-postgres psql -U healthdata -d quality_db \
  -c "SELECT COUNT(*) FROM measures;"
```

**4.4 Verify services are operational**

```bash
docker-compose ps
curl -s http://localhost:8087/health | jq .
```

**4.5 Notify stakeholders**

```bash
# Send rollback notice
mail -s "Measure Builder Staging Rollback Completed" \
  stakeholders@company.com << EOF
Rollback completed at $(date).
Previous version restored and verified.
Impact: Users may lose recent changes.
Next steps: Root cause analysis and fix validation.
EOF
```

### Root Cause Analysis

After rollback, perform RCA:

1. Identify what went wrong
2. Fix the issue (code, configuration, or data)
3. Test thoroughly in local environment
4. Re-run validation script
5. Schedule re-deployment

---

## Verification Checklist

Use this checklist after deployment to confirm everything works:

### Functional Testing
- [ ] Create new measure successfully
- [ ] Add algorithm blocks (multiple types)
- [ ] Configure sliders (range, threshold, distribution, period)
- [ ] Generate CQL from measure
- [ ] Publish measure
- [ ] Edit published measure
- [ ] View measure details
- [ ] Filter measures by status, complexity, date
- [ ] Export measure to CQL
- [ ] Multi-user concurrent editing works

### Performance Testing
- [ ] API response time <500ms for measure list
- [ ] CQL generation <1000ms for complex measures
- [ ] SVG rendering <50ms for <150 blocks
- [ ] Canvas rendering <200ms for 150+ blocks
- [ ] Slider updates <5ms single user, <50ms 10 concurrent

### Security Testing
- [ ] X-Tenant-ID header required
- [ ] Users cannot access other tenants' measures
- [ ] PHI endpoints have no-cache headers
- [ ] CSRF tokens present on forms
- [ ] XSS protection headers present
- [ ] Rate limiting working (if configured)

### Accessibility Testing
- [ ] Keyboard navigation works
- [ ] Screen reader announces all elements
- [ ] Color contrast meets WCAG 2.1 AA
- [ ] Focus indicators visible
- [ ] Form labels associated with inputs
- [ ] Error messages accessible

### Data Integrity
- [ ] Database migrations completed successfully
- [ ] No schema validation errors in logs
- [ ] Existing data preserved (if migrating)
- [ ] Backup and restore works
- [ ] Data consistency across tenants

---

## Monitoring Dashboard Setup

### Grafana Dashboard Configuration

1. **Create new dashboard:** "Measure Builder Staging"

2. **Add panels:**

   - **Service Health Status** (Gauge)
     - Query: `up{job="measure-builder"}`
     - Threshold: 1 = healthy, 0 = down

   - **Request Rate** (Graph)
     - Query: `rate(http_requests_total[5m])`
     - Separate by status code

   - **Response Time P95** (Graph)
     - Query: `histogram_quantile(0.95, http_request_duration_seconds)`
     - Target: <500ms

   - **Error Rate** (Gauge)
     - Query: `rate(http_requests_total{status="5xx"}[5m])`
     - Alert: >1%

   - **CPU & Memory Usage** (Graph)
     - Query: `container_cpu_usage_seconds_total`, `container_memory_usage_bytes`

3. **Set up alerts:** Use thresholds defined in monitoring section

---

## Support & Escalation

### During Deployment

**Point of Contact:** Deployment Lead
**Escalation Contact:** Release Manager
**Emergency Contact:** On-Call Engineer

### Communication Channels

| Issue Type | Channel | Response Time |
|-----------|---------|---|
| Deployment blocked | Slack #measure-builder | Immediate |
| Performance issue | PagerDuty | 5 minutes |
| Data integrity | On-call + Manager | 10 minutes |
| Complete outage | Page on-call team | Immediate |

### Incident Response

1. **Detect:** Monitoring alert or user report
2. **Alert:** Page on-call engineer (< 2 minutes)
3. **Assess:** Determine scope and severity (< 5 minutes)
4. **Mitigate:** Execute fix or rollback (< 15 minutes)
5. **Communicate:** Update stakeholders (< 10 minutes)
6. **Resolve:** Get to stable state (< 30 minutes)
7. **Review:** Post-mortem within 48 hours

---

## Success Criteria

Deployment is successful when:

✅ All services start and pass health checks
✅ Database migrations complete without errors
✅ All validation script tests pass
✅ Manual workflow testing completes successfully
✅ Multi-tenant isolation verified
✅ Performance benchmarks met
✅ Security headers present
✅ No critical errors in logs
✅ Stakeholder sign-offs obtained
✅ Monitoring dashboards operational

---

## Next Steps After Successful Deployment

1. **Week 1:** Continuous monitoring (24/7 coverage)
2. **Week 2:** Begin Priority 2 - User Documentation & Training
3. **Week 3:** Begin Priority 3 - Production Monitoring Setup
4. **Week 4:** Plan production deployment

---

## Additional Resources

- **Validation Script:** `scripts/validate-measure-builder-staging.sh`
- **Validation Report:** Generated by validation script
- **Docker Compose:** `docker-compose.staging.yml`
- **Environment Template:** `.env.example`
- **Deployment Readiness Doc:** `MEASURE_BUILDER_DEPLOYMENT_READINESS.md`
- **Architecture:** `docs/architecture/SYSTEM_ARCHITECTURE.md`
- **API Specification:** `BACKEND_API_SPECIFICATION.md`

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-18 | HDIM Team | Initial creation |

---

**Status:** ✅ Ready for Staging Deployment
**Last Updated:** January 18, 2026
**Next Review:** After staging deployment (1 week)
