# HDIM Remaining Implementation Plan

**Generated**: January 6, 2026
**Total Open Issues**: 39
**Completed Issues**: Phase 0-1 (4 issues)

---

## Executive Summary

| Phase | Issues | Status | Priority |
|-------|--------|--------|----------|
| Phase 0 | #33, #36 | ✅ Completed | - |
| Phase 1 | #64, #65 | ✅ Completed | - |
| Phase 2 | #85-93, #74, #75 | 🔴 In Progress | High |
| Phase 3 | #94-98 | 🟡 Pending | High |
| Phase 4A | #26, #27, #28 | 🟡 Pending | Critical |
| Phase 4B | #99-115 | 🟢 Pending | Medium |
| Phase 5 | #101, #102 | 🟢 Pending | Medium |

---

## Phase 2: Testing Infrastructure (11 Issues)

### Current State Assessment
- **Existing E2E Tests**: 14 spec files in `apps/clinical-portal-e2e/src/`
- **Existing Workflow**: `e2e-tests.yml` configured
- **Test Framework**: Playwright with Nx integration

### Issues to Implement

#### Issue #85 - API Connectivity E2E Tests (13 scenarios)
**File**: `apps/clinical-portal-e2e/src/api-connectivity.e2e.spec.ts`

```typescript
// Test scenarios to implement:
1. Gateway health check responds 200
2. All 28 services reachable via gateway
3. Service-to-service communication works
4. Database connectivity verified
5. Redis cache connectivity verified
6. Kafka broker connectivity verified
7. API versioning (v1 endpoints)
8. Content-Type negotiation (JSON)
9. CORS headers present
10. Rate limiting headers present
11. Request timeout handling
12. Connection retry on transient failure
13. Circuit breaker triggers on repeated failures
```

#### Issue #86 - Multi-Tenant Isolation E2E Tests (9 scenarios) - CRITICAL
**File**: `apps/clinical-portal-e2e/src/multi-tenant-isolation.e2e.spec.ts`

```typescript
// Test scenarios to implement:
1. Tenant A cannot access Tenant B's patients
2. Tenant A cannot access Tenant B's care gaps
3. Tenant A cannot access Tenant B's evaluations
4. Cross-tenant query injection prevented
5. X-Tenant-ID header required on all PHI endpoints
6. Missing tenant header returns 400
7. Invalid tenant ID returns 403
8. Tenant switch mid-session blocked
9. Audit log records tenant context
```

#### Issue #87 - JWT Token Refresh Tests (6 tests)
**File**: `apps/clinical-portal-e2e/src/jwt-token-refresh.e2e.spec.ts`

```typescript
// Test scenarios:
1. Valid token accepted
2. Expired token triggers refresh
3. Invalid token returns 401
4. Refresh token rotation works
5. Concurrent refresh requests handled
6. Token revocation prevents access
```

#### Issue #88 - Gateway Auth Filter Tests (5 tests)
**File**: `apps/clinical-portal-e2e/src/gateway-auth-filter.e2e.spec.ts`

```typescript
// Test scenarios:
1. Valid JWT passes filter
2. Missing JWT returns 401
3. Malformed JWT returns 401
4. Expired JWT returns 401
5. HMAC signature validation (when enforced)
```

#### Issue #89 - Gateway Routing Tests (31 tests)
**File**: `apps/clinical-portal-e2e/src/gateway-routing.e2e.spec.ts`

```typescript
// Test each of 28 services + 3 edge cases:
- quality-measure-service routing
- cql-engine-service routing
- fhir-service routing
- patient-service routing
- care-gap-service routing
- ... (all 28 services)
- 404 for unknown routes
- Method not allowed (405)
- Request too large (413)
```

#### Issue #90 - Service Health Check Tests (28 services)
**File**: `apps/clinical-portal-e2e/src/service-health-check.e2e.spec.ts`

```typescript
// For each service:
1. /actuator/health returns 200
2. Health details include db, redis, kafka
3. Startup probe succeeds
4. Liveness probe succeeds
5. Readiness probe succeeds
```

#### Issue #91 - Database Tenant Filtering Tests
**File**: `apps/clinical-portal-e2e/src/database-tenant-filtering.e2e.spec.ts`

```typescript
// Test scenarios:
1. Patient queries filtered by tenant
2. Care gap queries filtered by tenant
3. Evaluation queries filtered by tenant
4. Direct SQL injection blocked
5. ORM tenant filter applied automatically
```

#### Issue #92 - Cache Tenant Isolation Tests
**File**: `apps/clinical-portal-e2e/src/cache-tenant-isolation.e2e.spec.ts`

```typescript
// Test scenarios:
1. Cache keys include tenant prefix
2. PHI cache TTL <= 5 minutes
3. Cache invalidation per tenant
4. Cross-tenant cache pollution prevented
```

#### Issue #93 - CI/CD Integration Workflow
**File**: `.github/workflows/e2e-integration.yml`

```yaml
# Workflow requirements:
- Trigger on PR to main
- Start Docker services
- Wait for health checks
- Run Playwright tests
- Upload test results
- Fail PR on test failure
```

#### Issue #74 - Quality Measure E2E Tests
**File**: `apps/clinical-portal-e2e/src/quality-measures.e2e.spec.ts`

```typescript
// Test scenarios:
1. HEDIS measure list loads
2. Measure evaluation triggers successfully
3. Care gap calculation returns results
4. Quality score displays correctly
5. Measure filters work (measure type, status)
```

#### Issue #75 - CI/CD E2E Integration
**Enhancement to**: `.github/workflows/e2e-tests.yml`

---

## Phase 3: Observability Foundation (5 Issues)

### Current State Assessment
- **Existing Prometheus Config**: `prometheus.yml`, `alerts.yml`
- **Existing Grafana Dashboards**: 4 dashboards
- **No Sentry Integration**: Needs implementation

### Issues to Implement

#### Issue #94 - Frontend Sentry Integration
**Files**:
- `apps/clinical-portal/src/app/core/error-handler/sentry-error-handler.ts`
- `apps/clinical-portal/src/environments/environment.ts`
- `apps/clinical-portal/src/environments/environment.prod.ts`

```typescript
// Implementation:
1. Install @sentry/angular-ivy
2. Create SentryErrorHandler service
3. Configure DSN from environment
4. Capture unhandled errors
5. Capture HTTP errors
6. Add user context (tenant, role)
7. Add release version tracking
```

#### Issue #95 - Backend Sentry Configuration (28 services)
**Files**:
- `backend/modules/shared/infrastructure/observability/src/main/java/com/healthdata/observability/SentryConfig.java`
- Add `sentry-spring-boot-starter` dependency

```java
// Configuration for each service:
1. Add sentry-spring-boot-starter dependency
2. Configure DSN via environment
3. Add tenant context to events
4. Add user context to events
5. Configure release tracking
6. Set up performance monitoring
```

#### Issue #96 - Alertmanager Configuration
**File**: `docker/alertmanager/alertmanager.yml`

```yaml
# Configuration:
global:
  smtp_smarthost: 'smtp.example.com:587'
  slack_api_url: '${SLACK_WEBHOOK_URL}'

route:
  receiver: 'default'
  routes:
    - match:
        severity: critical
      receiver: 'pagerduty'
    - match:
        severity: warning
      receiver: 'slack'

receivers:
  - name: 'slack'
    slack_configs:
      - channel: '#hdim-alerts'
  - name: 'pagerduty'
    pagerduty_configs:
      - service_key: '${PAGERDUTY_KEY}'
```

#### Issue #97 - Prometheus Alert Rules (60+ alerts)
**File**: `docker/prometheus/alert-rules-comprehensive.yml`

```yaml
# Alert categories:
- Service Availability (28 services × 3 checks = 84)
- Response Time (P50, P95, P99)
- Error Rate (4xx, 5xx)
- Resource Utilization (CPU, Memory, Disk)
- Database (connections, query time, replication lag)
- Cache (hit rate, evictions, memory)
- Kafka (consumer lag, partition status)
- HIPAA Compliance (PHI access, audit failures)
```

#### Issue #98 - SLO Dashboards (3 dashboards)
**Files**:
- `docker/grafana/dashboards/slo-availability.json`
- `docker/grafana/dashboards/slo-latency.json`
- `docker/grafana/dashboards/slo-error-budget.json`

```json
// Dashboard panels:
- Availability SLI (99.9% target)
- Latency SLI (P99 < 500ms)
- Error Budget remaining
- Burn rate alerts
- SLO compliance history
```

---

## Phase 4A: Security Enhancement (3 Issues)

### Issues to Implement

#### Issue #26 - HMAC-SHA256 Enforcement
**Status**: Infrastructure complete, deployment pending

**Tasks**:
1. Generate production HMAC secret (32+ bytes)
2. Store in HashiCorp Vault
3. Configure `GATEWAY_AUTH_SIGNING_SECRET` in production
4. Set `GATEWAY_AUTH_DEV_MODE=false`
5. Run HMAC test suite from #33
6. Create secret rotation runbook
7. Monitor HMAC validation failures

#### Issue #27 - Security Event Monitoring
**Files**:
- `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/metrics/SecurityMetrics.java`
- `docker/grafana/dashboards/security-events.json`

```java
// Metrics to implement:
- auth_success_total (counter)
- auth_failure_total (counter, label: reason)
- tenant_violation_total (counter)
- hmac_validation_failure_total (counter)
- suspicious_activity_detected (counter)
- phi_access_total (counter, labels: resource_type, action)
```

#### Issue #28 - Mutual TLS (mTLS)
**Recommended**: Istio Service Mesh

**Tasks**:
1. Install Istio control plane
2. Enable automatic sidecar injection
3. Configure PeerAuthentication for mTLS
4. Create AuthorizationPolicy for service-to-service
5. Test internal communication encryption
6. Document certificate rotation

---

## Phase 4B: Advanced Observability (13 Issues)

### Dependency Graph
```
#104 (Correlation Engine)
  └─→ #109 (Distributed Tracing)

#105 (Cost DB) → #106 (Cost Collection) → #107 (Business KPIs)

#108 (Predictive Alerting)

#110 (Grafana Dashboards)

#111 (HIPAA Compliance) - Cross-cutting

#99 (K6 Load Testing) → #100 (Performance Baselines)

#112 (Integration Testing) - Parallel
#113 (Documentation) - Parallel
#114 (Production Deploy) - Final
```

### Key Services to Create

| Service | Issue | Port | Purpose |
|---------|-------|------|---------|
| correlation-engine-service | #104 | 8095 | Root cause analysis |
| cost-attribution-service | #105, #106 | 8096 | Cost tracking |
| kpi-framework-service | #107 | 8097 | Business metrics |

### Load Testing (#99)
**File**: `tests/load/k6-scenarios.js`

```javascript
// Scenarios:
1. Patient lookup spike test (100 → 1000 VUs)
2. Measure evaluation sustained load (50 VUs, 30min)
3. Care gap query stress test (500 concurrent)
4. Authentication flood test (rate limiting validation)
5. Mixed workload simulation (real traffic patterns)
```

### Performance Baselines (#100)
```yaml
# Targets:
- P50 latency: < 100ms
- P95 latency: < 300ms
- P99 latency: < 500ms
- Availability: 99.9%
- Error rate: < 0.1%
- Throughput: > 1000 req/s per service
```

---

## Phase 5: Operations Readiness (2 Issues)

#### Issue #101 - Deployment Test Scripts
**Directory**: `/deployment-tests/`

**Files**:
- `health-check-validator.sh` - Verify all 28 services healthy
- `migration-validator.sh` - Verify DB migrations applied
- `config-validator.sh` - Verify environment configuration
- `smoke-tests.sh` - Quick functional verification
- `rollback-test.sh` - Verify rollback capability

#### Issue #102 - Operational Runbooks
**Directory**: `/docs/runbooks/`

**Runbooks**:
- `deployment-checklist.md` - Pre/post deployment steps
- `rollback-procedure.md` - Emergency rollback
- `incident-response.md` - Incident handling workflow
- `secret-rotation.md` - HMAC, JWT, DB credential rotation
- `database-backup-restore.md` - Backup/recovery procedures
- `scaling-services.md` - Horizontal/vertical scaling
- `troubleshooting-guide.md` - Common issue resolution

---

## Implementation Priority

### Week 1-2: Phase 2 Testing
1. Create multi-tenant isolation tests (#86) - **CRITICAL**
2. Create API connectivity tests (#85)
3. Create gateway routing tests (#89)
4. Create health check tests (#90)
5. CI/CD integration (#93, #75)

### Week 2-3: Phase 3 Observability
1. Sentry frontend integration (#94)
2. Sentry backend configuration (#95)
3. Alertmanager configuration (#96)
4. Prometheus alert rules (#97)
5. SLO dashboards (#98)

### Week 3-4: Phase 4A Security
1. HMAC enforcement deployment (#26)
2. Security metrics implementation (#27)
3. mTLS evaluation and planning (#28)

### Week 4-6: Phase 4B Advanced
1. Load testing scenarios (#99)
2. Performance baselines (#100)
3. Correlation engine (#104)
4. Cost attribution (#105, #106)
5. KPI framework (#107)

### Week 6-7: Phase 5 Operations
1. Deployment scripts (#101)
2. Operational runbooks (#102)

---

## Quick Commands

```bash
# Run E2E tests
npx playwright test --project=chromium

# Run specific test file
npx playwright test src/multi-tenant-isolation.e2e.spec.ts

# Build and test cms-connector-service
cd backend && ./gradlew :modules:services:cms-connector-service:build

# Check all services health
./scripts/health-check.sh

# Deploy to staging
docker compose -f docker-compose.staging.yml up -d
```

---

## Success Metrics

- [ ] 100+ E2E tests passing
- [ ] 60+ Prometheus alert rules configured
- [ ] 10+ Grafana dashboards deployed
- [ ] 7+ operational runbooks documented
- [ ] HMAC enforcement active in production
- [ ] mTLS evaluation complete
- [ ] Performance baselines established
- [ ] All 28 services health-checked in CI/CD
