# HDIM Platform Production Deployment Checklist - v1.3.0-test

**Target Deployment Date:** 2026-01-20
**Environment:** Production
**Deployment Window:** 02:00 AM - 06:00 AM

---

## 📋 Pre-Deployment (T-7 days)

### Documentation Review
- [ ] Review [RELEASE_NOTES_v1.3.0-test.md](./RELEASE_NOTES_v1.3.0-test.md)
- [ ] Review [UPGRADE_GUIDE_v1.3.0-test.md](./UPGRADE_GUIDE_v1.3.0-test.md)
- [ ] Review [KNOWN_ISSUES_v1.3.0-test.md](./KNOWN_ISSUES_v1.3.0-test.md)
- [ ] Review [VERSION_MATRIX_v1.3.0-test.md](./VERSION_MATRIX_v1.3.0-test.md)

### Stakeholder Communication
- [ ] Notify stakeholders of deployment date/time
- [ ] Send deployment runbook to operations team
- [ ] Schedule post-deployment review meeting
- [ ] Prepare rollback communication template

### Infrastructure Preparation
- [ ] Verify production infrastructure capacity
- [ ] Verify backup systems are operational
- [ ] Verify monitoring systems are operational
- [ ] Verify alerting rules are current

---

## 📋 Pre-Deployment (T-24 hours)

### Database Preparation
- [ ] Run database backup script
- [ ] Verify backup completion and integrity
- [ ] Test database restore procedure (on staging)
- [ ] Document current row counts for critical tables
- [ ] Reserve database maintenance window

### Environment Validation
- [ ] Verify all required secrets are in Vault/SecretManager
- [ ] Verify SSL/TLS certificates are current (>30 days validity)
- [ ] Verify DNS records are current
- [ ] Verify load balancer health checks configured

### Testing
- [ ] Run full test suite on staging environment
- [ ] Run integration tests on staging
- [ ] Run performance/load tests on staging
- [ ] Verify all validation scripts pass

---

## 📋 Pre-Deployment (T-1 hour)

### Final Checks
- [ ] Confirm no deployments in last 2 hours
- [ ] Confirm all stakeholders available during window
- [ ] Confirm rollback team on standby
- [ ] Enable deployment mode in monitoring

### Communication
- [ ] Post "Deployment Starting" notification to status page
- [ ] Notify on-call engineers
- [ ] Start deployment war room (Slack/Teams)

---

## 🚀 Deployment Steps

### Step 1: Enable Maintenance Mode
```bash
# Set maintenance page
kubectl apply -f k8s/maintenance-page.yaml

# Verify maintenance page active
curl https://hdim.example.com | grep "Maintenance"
```
- [ ] Maintenance page active
- [ ] Users see maintenance message

---

### Step 2: Stop Application Services
```bash
# Stop application pods (keep infrastructure running)
kubectl scale deployment --replicas=0 --all -n hdim-production

# Verify all application pods stopped
kubectl get pods -n hdim-production
```
- [ ] All application pods terminated
- [ ] Infrastructure pods still running (postgres, redis, kafka)

---

### Step 3: Backup Databases
```bash
# Automated backup script
./scripts/production-backup.sh v1.3.0-test

# Verify backups
ls -lh /backups/production/v1.3.0-test/
```
- [ ] All 29 databases backed up
- [ ] Backup sizes reasonable (compare to previous)
- [ ] Backup checksums generated

---

### Step 4: Run Database Migrations
```bash
# Run Liquibase migrations
cd backend
./gradlew update -Pprofile=prod

# Verify migration counts
./scripts/verify-migrations.sh
```
- [ ] All Liquibase changesets applied
- [ ] No migration errors in logs
- [ ] Migration count matches expected: 157

---

### Step 5: Deploy New Docker Images
```bash
# Pull latest images
docker pull hdim/gateway-service:v1.3.0-test
docker pull hdim/patient-service:v1.3.0-test
# ... (all 34 services)

# Update Kubernetes manifests
kubectl apply -f k8s/deployments/ -n hdim-production

# Verify image versions
kubectl get pods -n hdim-production -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.spec.containers[0].image}{"\n"}{end}'
```
- [ ] All images pulled successfully
- [ ] All deployments updated
- [ ] Image tags match v1.3.0-test

---

### Step 6: Start Application Services
```bash
# Scale up deployments
kubectl scale deployment --replicas=3 --all -n hdim-production

# Watch pods start
kubectl get pods -n hdim-production -w
```
- [ ] All pods started
- [ ] All pods report "Running"
- [ ] All pods pass readiness probes

---

### Step 7: Verify Service Health
```bash
# Check all health endpoints
for service in gateway patient fhir quality cql caregap; do
  curl https://hdim.example.com/$service/actuator/health | jq .
done
```
- [ ] All health endpoints return `UP`
- [ ] No errors in service logs
- [ ] Connection pools initialized

---

### Step 8: Run Smoke Tests
```bash
# Execute smoke test suite
./scripts/smoke-tests-production.sh
```
- [ ] Patient CRUD operations successful
- [ ] Quality measure retrieval successful
- [ ] FHIR resource queries successful
- [ ] Authentication/authorization working

---

### Step 9: Disable Maintenance Mode
```bash
# Remove maintenance page
kubectl delete -f k8s/maintenance-page.yaml

# Verify application accessible
curl https://hdim.example.com/health
```
- [ ] Maintenance page removed
- [ ] Application accessible to users
- [ ] Load balancer routing to all pods

---

### Step 10: Monitor Initial Traffic
- [ ] Monitor error rates (should be <1%)
- [ ] Monitor response times (p95 <500ms)
- [ ] Monitor database connection pools (no exhaustion)
- [ ] Monitor Kafka consumer lag (<100 messages)
- [ ] Monitor distributed tracing (Jaeger shows traces)

**Monitoring Duration:** 30 minutes

---

## ✅ Post-Deployment Validation

### Application Validation
- [ ] All 34 services reporting healthy
- [ ] API response times within SLA
- [ ] No error spikes in logs
- [ ] Database queries performing normally
- [ ] Cache hit rates normal
- [ ] Kafka consumers processing messages

### Data Validation
- [ ] Critical table row counts match pre-deployment
- [ ] No data loss detected
- [ ] Recent transactions visible
- [ ] Reports generating correctly

### Security Validation
- [ ] Authentication working (test login)
- [ ] Authorization working (test @PreAuthorize)
- [ ] HIPAA audit logging active
- [ ] SSL/TLS certificates valid

### Monitoring Validation
- [ ] Prometheus collecting metrics
- [ ] Grafana dashboards showing data
- [ ] Jaeger showing distributed traces
- [ ] Alert rules firing correctly (test alert)

---

## 🚨 Rollback Procedure

**Decision Point:** If any critical issue detected, initiate rollback

### Rollback Steps
1. [ ] Stop traffic to new version
2. [ ] Restore database backups
3. [ ] Deploy previous version images
4. [ ] Restart application services
5. [ ] Verify rollback successful
6. [ ] Document rollback reason

**Rollback Time:** 15-30 minutes

---

## 📢 Post-Deployment Communication

### Internal Communication
- [ ] Post "Deployment Complete" to status page
- [ ] Notify stakeholders of successful deployment
- [ ] Send deployment summary to operations team
- [ ] Update deployment log

### External Communication (if applicable)
- [ ] Post release announcement
- [ ] Update public documentation
- [ ] Notify customers of new features

---

## 📝 Post-Deployment Review

**Schedule:** Within 48 hours of deployment

### Review Topics
- [ ] Deployment timeline (planned vs actual)
- [ ] Issues encountered and resolutions
- [ ] Performance metrics (before/after)
- [ ] Lessons learned
- [ ] Process improvements for next deployment

---

**Deployment Lead:** {NAME}
**Database Admin:** {NAME}
**Operations Lead:** {NAME}
**On-Call Engineer:** {NAME}

**Deployment Duration:** {ACTUAL_DURATION}
**Downtime:** {ACTUAL_DOWNTIME}
**Rollback Required:** {YES/NO}
