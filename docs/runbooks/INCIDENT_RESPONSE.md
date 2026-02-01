# Incident Response Runbook - HDIM Platform

**Service**: All HDIM Platform Services
**Version**: 1.0.0
**Date**: 2025-12-24
**On-Call**: Platform Engineering Team

---

## Quick Reference

### Incident Severity Levels

| Severity | Definition | Response Time | Escalation |
|----------|------------|---------------|------------|
| **SEV-1** | Platform down, data loss risk, security breach | < 15 min | Immediate (PagerDuty) |
| **SEV-2** | Major feature unavailable, degraded service | < 30 min | Urgent (Slack + Page) |
| **SEV-3** | Minor feature issues, performance degradation | < 2 hr | Standard (Slack) |
| **SEV-4** | Cosmetic issues, minor bugs | Next business day | Ticket only |

### Emergency Contacts

| Role | Contact | Method |
|------|---------|--------|
| On-Call Engineer | Rotating | PagerDuty |
| Platform Lead | @platform-lead | Slack `#platform-team` |
| Security Lead | @security-lead | Slack `#security-oncall` |
| Database Admin | @dba-oncall | Slack `#dba-oncall` |
| Executive Escalation | VP Engineering | Phone (after-hours) |

### Quick Actions

```bash
# Check all service health
for svc in gateway-service fhir-service cql-engine-service patient-service; do
  echo "=== $svc ==="
  curl -s "http://localhost:808X/actuator/health" | jq '.status'
done

# Check Kubernetes pod status
kubectl get pods -n healthdata-prod -o wide

# View recent logs (all services)
kubectl logs -n healthdata-prod -l app.kubernetes.io/part-of=hdim --tail=100 --since=10m

# Emergency restart a service
kubectl rollout restart deployment/<service-name> -n healthdata-prod
```

---

## Table of Contents

1. [Incident Detection](#incident-detection)
2. [Initial Response](#initial-response)
3. [Incident Classification](#incident-classification)
4. [Response Procedures](#response-procedures)
5. [Service-Specific Runbooks](#service-specific-runbooks)
6. [Communication](#communication)
7. [Resolution & Recovery](#resolution--recovery)
8. [Post-Incident Activities](#post-incident-activities)

---

## Incident Detection

### Monitoring Sources

| Source | URL | What It Monitors |
|--------|-----|------------------|
| Grafana | `https://grafana.healthdata.com` | Metrics, dashboards |
| Prometheus Alerts | `https://prometheus.healthdata.com/alerts` | Alert rules |
| Jaeger | `https://jaeger.healthdata.com` | Distributed traces |
| PagerDuty | `https://healthdata.pagerduty.com` | Incident management |
| Loki | `https://grafana.healthdata.com/explore` | Log aggregation |

### Alert Categories

| Category | Examples | Dashboard |
|----------|----------|-----------|
| **Availability** | Service down, pods not ready, health check failures | Service Health |
| **Performance** | High latency, low throughput, timeouts | Performance Overview |
| **Resources** | CPU/memory high, disk full, connection pool exhausted | Resource Usage |
| **Errors** | 5xx rates, exception spikes, failed operations | Error Tracking |
| **Security** | Auth failures, suspicious activity, compliance issues | Security Dashboard |
| **Data** | Replication lag, backup failures, data inconsistency | Data Health |

### Health Check Endpoints

```bash
# Gateway (entry point)
curl http://gateway-service:8080/actuator/health

# Core services
curl http://fhir-service:8081/actuator/health
curl http://cql-engine-service:8082/actuator/health
curl http://patient-service:8083/actuator/health
curl http://consent-service:8084/actuator/health

# Supporting services
curl http://event-processing-service:8085/actuator/health
curl http://analytics-service:8086/actuator/health
```

---

## Initial Response

### Step 1: Acknowledge (< 5 minutes)

```markdown
1. [ ] Acknowledge alert in PagerDuty
2. [ ] Join incident Slack channel (#incidents)
3. [ ] Post initial message:
   "INCIDENT: [Brief description]
   Severity: SEV-X
   Status: Investigating
   Lead: @your-name
   Time: HH:MM UTC"
```

### Step 2: Assess Impact (5-10 minutes)

```bash
# Quick health assessment
echo "=== Platform Health Assessment ==="

# 1. Check core services
echo "Service Status:"
kubectl get pods -n healthdata-prod --sort-by='.status.phase'

# 2. Check recent events
echo -e "\nRecent Events:"
kubectl get events -n healthdata-prod --sort-by='.lastTimestamp' | tail -20

# 3. Check error rates
echo -e "\nError Rates (last 5m):"
curl -s 'http://prometheus:9090/api/v1/query' \
  --data-urlencode 'query=sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (service)'

# 4. Check response times
echo -e "\nP95 Latency (last 5m):"
curl -s 'http://prometheus:9090/api/v1/query' \
  --data-urlencode 'query=histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, service))'
```

### Step 3: Determine Scope

| Question | How to Check |
|----------|--------------|
| Which services affected? | `kubectl get pods -n healthdata-prod` |
| How many users impacted? | Grafana: Active Sessions dashboard |
| Is data at risk? | Check database health, backup status |
| Is this security-related? | Check auth logs, unusual patterns |
| Is PHI exposed? | Check audit logs for access patterns |

---

## Incident Classification

### Decision Tree

```
Is the platform completely unavailable?
├── YES → SEV-1 (Platform Outage)
└── NO → Is a critical feature unavailable?
         ├── YES → Is it affecting >50% of users?
         │         ├── YES → SEV-1 (Major Outage)
         │         └── NO → SEV-2 (Partial Outage)
         └── NO → Is there performance degradation?
                  ├── YES → Is P95 latency >2s or error rate >5%?
                  │         ├── YES → SEV-2 (Major Degradation)
                  │         └── NO → SEV-3 (Minor Degradation)
                  └── NO → SEV-4 (Minor Issue)
```

### Critical Features (SEV-1 if unavailable)

1. **Authentication/Authorization** - Users cannot log in
2. **Patient Data Access** - Cannot view/update patient records
3. **CQL Evaluation** - Quality measures cannot run
4. **Care Gap Detection** - Care gaps not being identified
5. **FHIR API** - EHR integrations broken
6. **Consent Management** - Cannot process patient consent

---

## Response Procedures

### SEV-1: Platform Outage

**Timeline**: Resolution within 1 hour

```markdown
## Immediate Actions (0-15 min)

1. [ ] Page incident commander and executives
2. [ ] Create incident bridge call
3. [ ] Identify if this is:
   - Infrastructure failure (AWS/K8s)
   - Database failure
   - Network issue
   - Application failure
   - Security incident

## Containment (15-30 min)

4. [ ] If security: Isolate affected systems
5. [ ] If database: Failover to replica
6. [ ] If application: Rollback recent deployments
7. [ ] If infrastructure: Engage cloud support

## Recovery (30-60 min)

8. [ ] Implement fix or workaround
9. [ ] Verify service restoration
10. [ ] Monitor for 15 minutes
11. [ ] Update status page
12. [ ] Stand down incident bridge
```

**Common Causes & Quick Fixes**:

| Cause | Symptoms | Quick Fix |
|-------|----------|-----------|
| Database down | Connection errors | `kubectl rollout restart statefulset/postgres` |
| Memory exhaustion | OOMKilled pods | Scale up or restart: `kubectl scale deployment/<svc> --replicas=0 && kubectl scale deployment/<svc> --replicas=3` |
| Bad deployment | Started after deploy | `kubectl rollout undo deployment/<svc>` |
| Certificate expiry | TLS errors | Check certs, apply new ones |
| DNS failure | Name resolution errors | Check CoreDNS, external DNS |

### SEV-2: Major Feature Unavailable

**Timeline**: Resolution within 2 hours

```markdown
## Immediate Actions (0-30 min)

1. [ ] Notify stakeholders in #incidents
2. [ ] Identify affected feature and services
3. [ ] Check for recent changes (deployments, configs)
4. [ ] Review logs for error patterns

## Investigation (30-60 min)

5. [ ] Trace requests through system (Jaeger)
6. [ ] Check database queries and performance
7. [ ] Verify external service connectivity
8. [ ] Test in lower environment if needed

## Resolution (60-120 min)

9. [ ] Implement fix
10. [ ] Deploy to production
11. [ ] Verify feature restored
12. [ ] Update status
```

### SEV-3: Minor Degradation

**Timeline**: Resolution within 4 hours

```markdown
## Actions

1. [ ] Log incident in tracking system
2. [ ] Investigate during business hours
3. [ ] Implement fix following normal process
4. [ ] Deploy with standard change process
```

---

## Service-Specific Runbooks

### Gateway Service

```bash
# Check gateway health
curl http://gateway-service:8080/actuator/health | jq '.'

# Check routing
curl http://gateway-service:8080/actuator/gateway/routes | jq '.[].route_id'

# View circuit breaker status
curl http://gateway-service:8080/actuator/circuitbreakers | jq '.'

# Restart gateway
kubectl rollout restart deployment/gateway-service -n healthdata-prod
```

**Common Issues**:
- Rate limiting triggered: Check `hdim.gateway.rate_limit.exceeded` metric
- Route not found: Verify service discovery and route configuration
- Authentication failures: Check JWT validation and token issuer

### FHIR Service

```bash
# Check FHIR health
curl http://fhir-service:8081/actuator/health | jq '.'

# Check FHIR metadata
curl http://fhir-service:8081/fhir/metadata | jq '.fhirVersion'

# Test patient read
curl http://fhir-service:8081/fhir/Patient/_search?_count=1

# Check database connections
curl http://fhir-service:8081/actuator/metrics/hikaricp.connections.active | jq '.'
```

**Common Issues**:
- Slow queries: Check `pg_stat_statements` for query plans
- Validation failures: Review FHIR profiles, check payload structure
- Event publishing: Verify Kafka connectivity

### CQL Engine Service

```bash
# Check CQL health
curl http://cql-engine-service:8082/actuator/health | jq '.'

# Check active evaluations
curl http://cql-engine-service:8082/actuator/metrics/cql.evaluation.active | jq '.'

# Check library cache
curl http://cql-engine-service:8082/actuator/metrics/cql.library.cache.size | jq '.'

# Clear library cache (if corrupted)
curl -X POST http://cql-engine-service:8082/admin/cache/clear
```

**Common Issues**:
- Evaluation timeout: Increase `cql.evaluation.timeout`
- Memory exhaustion: Scale up or increase heap
- Library load failures: Verify CQL library repository

### Database (PostgreSQL)

```bash
# Check PostgreSQL health
kubectl exec -it postgres-0 -n healthdata-prod -- pg_isready

# Check replication status
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "SELECT * FROM pg_stat_replication;"

# Check active connections
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "SELECT count(*) FROM pg_stat_activity;"

# Kill long-running queries
kubectl exec -it postgres-0 -n healthdata-prod -- \
  psql -U healthdata -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE state = 'active' AND query_start < now() - interval '5 minutes';"
```

### Redis Cache

```bash
# Check Redis health
kubectl exec -it redis-0 -n healthdata-prod -- redis-cli ping

# Check memory usage
kubectl exec -it redis-0 -n healthdata-prod -- redis-cli INFO memory | grep used_memory_human

# Check key count
kubectl exec -it redis-0 -n healthdata-prod -- redis-cli DBSIZE

# Flush all caches (CAUTION)
kubectl exec -it redis-0 -n healthdata-prod -- redis-cli FLUSHALL
```

### Kafka

```bash
# Check Kafka cluster health
kubectl exec -it kafka-0 -n healthdata-prod -- \
  kafka-broker-api-versions.sh --bootstrap-server localhost:9092

# Check consumer lag
kubectl exec -it kafka-0 -n healthdata-prod -- \
  kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --all-groups

# Check topic health
kubectl exec -it kafka-0 -n healthdata-prod -- \
  kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic hdim.events
```

---

## Communication

### Status Updates

**Template for Slack Updates**:
```markdown
**INCIDENT UPDATE** - [HH:MM UTC]
**Status**: Investigating | Identified | Monitoring | Resolved
**Severity**: SEV-X
**Impact**: [Brief description of user impact]
**Current Actions**: [What team is doing]
**ETA**: [Estimated resolution time]
**Next Update**: [When to expect next update]
```

### Update Frequency

| Severity | Update Interval |
|----------|-----------------|
| SEV-1 | Every 15 minutes |
| SEV-2 | Every 30 minutes |
| SEV-3 | Every 2 hours |
| SEV-4 | End of day |

### Stakeholder Notification

| Severity | Who to Notify |
|----------|---------------|
| SEV-1 | VP Engineering, Security, Compliance, Customer Success |
| SEV-2 | Engineering Manager, affected team leads |
| SEV-3 | Team lead only |
| SEV-4 | None (ticket only) |

### Status Page

URL: `https://status.healthdata.com`

Update components:
- Platform Status
- FHIR API
- Clinical Portal
- Integrations

---

## Resolution & Recovery

### Verification Checklist

```markdown
## Before Declaring Resolved

1. [ ] All health checks passing
2. [ ] Error rate back to baseline
3. [ ] Latency within SLO
4. [ ] No new errors in logs (5 min)
5. [ ] Test critical user flows
6. [ ] Database replication healthy
7. [ ] Cache hit rate normal
8. [ ] All pods healthy and ready
9. [ ] License compliance verified (docs/compliance/THIRD_PARTY_NOTICES.md)
```

### Recovery Testing

```bash
# Test critical endpoints
./scripts/smoke-test.sh

# Example smoke test
curl -s http://gateway-service:8080/health
curl -s http://fhir-service:8081/fhir/Patient?_count=1
curl -s http://cql-engine-service:8082/api/v1/evaluate \
  -H "Content-Type: application/json" \
  -d '{"libraryId":"test","patientId":"test"}'
```

### Rollback Procedures

```bash
# Kubernetes deployment rollback
kubectl rollout undo deployment/<service-name> -n healthdata-prod

# Check rollback status
kubectl rollout status deployment/<service-name> -n healthdata-prod

# View rollout history
kubectl rollout history deployment/<service-name> -n healthdata-prod
```

---

## Post-Incident Activities

### Immediate (Within 24 hours)

1. **Create post-incident ticket**
   - Summary of incident
   - Timeline of events
   - Root cause (preliminary)
   - Actions taken

2. **Schedule post-mortem**
   - Within 48 hours for SEV-1
   - Within 1 week for SEV-2

### Post-Incident Review (PIR)

**Agenda**:
1. Timeline review (5 min)
2. Root cause analysis (15 min)
3. What went well (5 min)
4. What could be improved (10 min)
5. Action items (10 min)

**PIR Document Template**:
```markdown
# Post-Incident Review: [Incident Title]

**Date**: YYYY-MM-DD
**Severity**: SEV-X
**Duration**: X hours Y minutes
**Author**: [Name]

## Summary
[1-2 sentence summary]

## Impact
- Users affected: X
- Transactions failed: Y
- SLO impact: Z% error budget consumed

## Timeline
| Time (UTC) | Event |
|------------|-------|
| HH:MM | [Event] |

## Root Cause
[Detailed explanation]

## Resolution
[What fixed it]

## Lessons Learned

### What Went Well
- [Item]

### What Could Be Improved
- [Item]

## Action Items
| # | Action | Owner | Due Date | Status |
|---|--------|-------|----------|--------|
| 1 | [Action] | [Owner] | [Date] | Open |

## References
- [Links to relevant docs, tickets, etc.]
```

### Metrics to Track

- **MTTR** (Mean Time To Resolve): Target < 1 hour for SEV-1
- **MTTA** (Mean Time To Acknowledge): Target < 5 minutes
- **MTTD** (Mean Time To Detect): Target < 2 minutes
- **Incident Count**: By severity, by service

---

## Appendix

### Useful Commands Cheat Sheet

```bash
# Pod debugging
kubectl describe pod <pod-name> -n healthdata-prod
kubectl logs <pod-name> -n healthdata-prod --previous
kubectl exec -it <pod-name> -n healthdata-prod -- /bin/sh

# Network debugging
kubectl run debug --image=busybox -it --rm -- nslookup <service>
kubectl run debug --image=curlimages/curl:8.10.1 -it --rm -- curl <url>

# Resource inspection
kubectl top pods -n healthdata-prod
kubectl top nodes

# Config verification
kubectl get configmap -n healthdata-prod
kubectl get secret -n healthdata-prod

# Force delete stuck pod
kubectl delete pod <pod-name> -n healthdata-prod --force --grace-period=0
```

### Escalation Matrix

| Time Since Incident | Escalation Action |
|---------------------|-------------------|
| 0 min | On-call engineer paged |
| 15 min (SEV-1) | Incident commander joins |
| 30 min (SEV-1) | Engineering manager notified |
| 1 hour (SEV-1) | VP Engineering notified |
| 2 hours (SEV-1) | Executive briefing |

---

**Runbook Version**: 1.0.0
**Last Updated**: 2025-12-24
**Next Review**: 2026-03-24
**Owner**: Platform Engineering Team
