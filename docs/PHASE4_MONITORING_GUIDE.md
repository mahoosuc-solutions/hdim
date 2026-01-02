# Phase 4.2: Enhanced Monitoring & Alerting Implementation Guide

**Status**: 🔄 In Progress
**Date**: December 31, 2025
**Component**: Gateway-Trust Authentication Security Monitoring

---

## Overview

This document describes the comprehensive monitoring and alerting infrastructure for HDIM's gateway-trust authentication system. This enables real-time detection of security incidents, performance degradation, and compliance violations.

---

## Metrics Collected

### 1. Authentication Metrics (TrustedHeaderAuthFilter)

#### Counter: `auth_success_total`
- **Type**: Counter
- **Unit**: requests
- **Tags**: `filter=trusted_header_auth`, `instance`
- **Description**: Total number of successful authentications
- **When Incremented**: When X-Auth-* headers are validated and SecurityContext is set
- **Use Case**: Baseline traffic measurement, success rate calculation
- **Query Example**:
  ```promql
  rate(auth_success_total[5m])  # Requests/second
  ```

#### Counter: `auth_failure_total`
- **Type**: Counter
- **Unit**: requests
- **Tags**: `filter=trusted_header_auth`, `instance`
- **Description**: Total number of failed authentications
- **When Incremented**: When authentication fails (missing headers, invalid format, exceptions)
- **Use Case**: Failure rate detection, anomaly detection
- **Query Example**:
  ```promql
  rate(auth_failure_total[5m])  # Failures/second
  ```

#### Counter: `hmac_validation_failures_total`
- **Type**: Counter
- **Unit**: requests
- **Tags**: `filter=trusted_header_auth`, `instance`
- **Description**: Total number of HMAC signature validation failures
- **When Incremented**: When X-Auth-Validated header signature is invalid or expired
- **Critical For**: Detecting header injection attacks, signing secret mismatches
- **Query Example**:
  ```promql
  rate(hmac_validation_failures_total[1m])  # Critical: should be 0
  ```

#### Timer: `auth_latency`
- **Type**: Histogram/Timer
- **Unit**: seconds
- **Tags**: `filter=trusted_header_auth`, `instance`
- **Description**: Authentication processing latency
- **Buckets**: Automatically generated (0.001s, 0.01s, 0.1s, 1s, etc.)
- **Use Case**: Performance monitoring, latency SLA tracking
- **Query Example**:
  ```promql
  # P95 latency (95th percentile)
  histogram_quantile(0.95, rate(auth_latency_bucket[5m]))

  # P50 latency (median)
  histogram_quantile(0.50, rate(auth_latency_bucket[5m]))

  # Average latency
  rate(auth_latency_sum[5m]) / rate(auth_latency_count[5m])
  ```

---

### 2. Tenant Isolation Metrics (TrustedTenantAccessFilter)

#### Counter: `tenant_violations_total`
- **Type**: Counter
- **Unit**: requests
- **Tags**: `filter=trusted_tenant_access`, `instance`
- **Description**: Total number of tenant isolation violations (cross-tenant access attempts)
- **When Incremented**: When user attempts to access unauthorized tenant in X-Tenant-ID header
- **Critical For**: Security detection, HIPAA compliance
- **Query Example**:
  ```promql
  rate(tenant_violations_total[1m])  # Critical: should be 0
  increase(tenant_violations_total[5m])  # Total violations in 5min
  ```

#### Counter: `missing_tenant_context_total`
- **Type**: Counter
- **Unit**: requests
- **Tags**: `filter=trusted_tenant_access`, `instance`
- **Description**: Total number of requests with missing tenant context
- **When Incremented**: When tenant IDs not found in request attributes (filter chain issue)
- **Critical For**: Configuration validation, filter chain integrity
- **Query Example**:
  ```promql
  rate(missing_tenant_context_total[5m])  # Warning: should be < 5 req/min
  ```

---

## Calculated Metrics

### Authentication Success Rate
```promql
rate(auth_success_total[5m]) / (rate(auth_success_total[5m]) + rate(auth_failure_total[5m]))

# Expected: > 99%
# Warning: 95% - 99%
# Critical: < 95%
```

### HMAC Validation Failure Rate
```promql
rate(hmac_validation_failures_total[1m])

# Expected: 0
# Critical: > 0 failures per second
```

### Average Authentication Latency
```promql
rate(auth_latency_sum[5m]) / rate(auth_latency_count[5m])

# Expected: < 10ms
# Warning: 10-50ms
# Critical: > 50ms
```

---

## Alerting Rules

### Critical Alerts (Immediate Response Required)

#### 1. HMAC Validation Failures
```yaml
Alert: HMACValidationFailures
Condition: rate(hmac_validation_failures_total[1m]) > 0
Duration: 1 minute
Severity: CRITICAL
Reason: Indicates header injection attempts or signing secret mismatch
Action:
  1. Check gateway logs for signature generation errors
  2. Verify GATEWAY_AUTH_SIGNING_SECRET matches all services
  3. If services have dev mode enabled, disable immediately
  4. Review recent deployments for configuration changes
```

#### 2. Tenant Isolation Violation
```yaml
Alert: TenantIsolationViolation
Condition: rate(tenant_violations_total[1m]) > 0
Duration: 1 minute
Severity: CRITICAL
Reason: Users attempting to access unauthorized tenants (security breach)
Action:
  1. Immediately investigate user accounts with violations
  2. Check audit logs for suspicious activity
  3. Verify TrustedTenantAccessFilter is active in filter chain
  4. Review X-Tenant-ID header validation logic
  5. Consider account lockout for repeat offenders
```

#### 3. Authentication SLA Violation
```yaml
Alert: AuthenticationSLAViolation
Condition: Success rate < 99.9% for >30 minutes
Duration: 5 minutes
Severity: CRITICAL
Reason: Violates HIPAA availability requirements (§164.308(a)(7))
Action:
  1. Escalate to on-call ops engineer
  2. Check gateway and backend service health
  3. Review authentication service logs
  4. Consider failover to backup systems
  5. Document incident and impact assessment
```

### Warning Alerts (Investigation Required)

#### 4. High Authentication Failure Rate
```yaml
Alert: HighAuthenticationFailureRate
Condition: rate(auth_failure_total[5m]) > 10 req/sec
Duration: 5 minutes
Severity: WARNING
Reason: May indicate brute force attacks or misconfigured clients
Action:
  1. Review authentication logs for patterns
  2. Check client configuration in error logs
  3. Monitor user accounts for lock-out patterns
  4. Consider rate limiting if attack pattern detected
```

#### 5. Authentication Latency High
```yaml
Alert: AuthenticationLatencyHigh
Condition: histogram_quantile(0.95, rate(auth_latency_bucket[5m])) > 0.5s
Duration: 5 minutes
Severity: WARNING
Reason: Authentication processing is degraded
Action:
  1. Check gateway resource utilization (CPU, memory, network)
  2. Review backend service response times
  3. Check database connection pool status
  4. Consider service scaling if under load
```

#### 6. Missing Tenant Context
```yaml
Alert: MissingTenantContext
Condition: rate(missing_tenant_context_total[5m]) > 5 req/min
Duration: 5 minutes
Severity: WARNING
Reason: Filter chain misconfiguration or race condition
Action:
  1. Verify TrustedHeaderAuthFilter is registered before TrustedTenantAccessFilter
  2. Check filter chain configuration in SecurityConfig
  3. Review application logs for filter initialization errors
  4. Consider service restart if filter chain is corrupted
```

---

## Prometheus Configuration

### scrape_configs

Add to `docker/prometheus/prometheus.yml`:

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    environment: 'docker'
    cluster: 'hdim'

scrape_configs:
  # Backend services authentication metrics
  - job_name: 'quality-measure-service'
    static_configs:
      - targets: ['quality-measure-service:8087']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 30s

  - job_name: 'cql-engine-service'
    static_configs:
      - targets: ['cql-engine-service:8081']
    metrics_path: '/actuator/prometheus'

  - job_name: 'fhir-service'
    static_configs:
      - targets: ['fhir-service:8085']
    metrics_path: '/actuator/prometheus'

  # ... (similar for all 18 backend services)

  - job_name: 'gateway-service'
    static_configs:
      - targets: ['gateway-service:8001']
    metrics_path: '/actuator/prometheus'

  # Prometheus itself
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
```

### Recording Rules

Add to `docker/prometheus/rules/security-recording-rules.yml`:

```yaml
groups:
  - name: authentication_recording_rules
    interval: 30s
    rules:
      - record: auth:success_rate:5m
        expr: rate(auth_success_total[5m]) / (rate(auth_success_total[5m]) + rate(auth_failure_total[5m]))

      - record: auth:latency_p95:5m
        expr: histogram_quantile(0.95, rate(auth_latency_bucket[5m]))

      - record: auth:latency_p50:5m
        expr: histogram_quantile(0.50, rate(auth_latency_bucket[5m]))

      - record: tenant:violations_rate:1m
        expr: rate(tenant_violations_total[1m])

      - record: tenant:missing_context_rate:5m
        expr: rate(missing_tenant_context_total[5m])
```

---

## Grafana Dashboard

### Dashboard: `hdim-authentication-security.json`

**Location**: `docker/grafana/dashboards/hdim-authentication-security.json`

**Panels**:
1. **Authentication Success Rate (5min)**
   - Type: Time series
   - Query: Success rate calculation
   - Threshold: Green (>99%), Yellow (95-99%), Red (<95%)

2. **Authentication Failures Rate (5min)**
   - Type: Gauge
   - Query: `rate(auth_failure_total[5m])`
   - Threshold: Green (<5), Yellow (5-10), Red (>10)

3. **Authentication Latency (5min)**
   - Type: Time series
   - Queries: P50, P95, P99 latencies
   - Threshold: Green (<10ms), Yellow (10-50ms), Red (>50ms)

4. **HMAC Validation Failures (5min)**
   - Type: Gauge
   - Query: `increase(hmac_validation_failures_total[5m])`
   - Threshold: Green (0), Red (>0)

5. **Tenant Isolation Violations (1min)**
   - Type: Time series
   - Query: `rate(tenant_violations_total[1m])`
   - Critical if > 0

6. **Missing Tenant Context (1min)**
   - Type: Time series
   - Query: `rate(missing_tenant_context_total[1m])`
   - Threshold: Yellow (>5), Red (>10)

### Accessing the Dashboard

1. Open Grafana: `http://localhost:3001`
2. Login with credentials
3. Navigate to Dashboards → General → HDIM Authentication Security
4. Adjust time range and refresh rate as needed

---

## Monitoring Runbook

### Daily Checks

```bash
# 1. Verify metrics are being collected
curl http://prometheus:9090/api/v1/query?query=auth_success_total

# 2. Check authentication success rate
curl http://prometheus:9090/api/v1/query?query=\
  'rate(auth_success_total%5B5m%5D)%20%2F%20(rate(auth_success_total%5B5m%5D)%20%2B%20rate(auth_failure_total%5B5m%5D))'

# 3. Check for HMAC failures
curl http://prometheus:9090/api/v1/query?query=rate(hmac_validation_failures_total%5B1m%5D)

# 4. Check for tenant violations
curl http://prometheus:9090/api/v1/query?query=rate(tenant_violations_total%5B1m%5D)

# 5. Check authentication latency
curl http://prometheus:9090/api/v1/query?query=\
  'histogram_quantile(0.95,%20rate(auth_latency_bucket%5B5m%5D))'
```

### Weekly Report

Generate weekly security metrics report:

```bash
#!/bin/bash
# Weekly metrics summary

echo "=== HDIM Authentication Metrics - Weekly Report ==="
echo "Period: Last 7 days"
echo

# Success rate
echo "Auth Success Rate (avg):"
curl -s 'http://prometheus:9090/api/v1/query?query=avg(auth:success_rate:5m)' | jq '.data.result[0].value[1]'

# Total authentications
echo "Total Auth Requests:"
curl -s 'http://prometheus:9090/api/v1/query?query=increase(auth_success_total%5B7d%5D)' | jq '.data.result[] | {instance: .metric.instance, count: .value[1]}'

# Security incidents
echo "Tenant Violations:"
curl -s 'http://prometheus:9090/api/v1/query?query=increase(tenant_violations_total%5B7d%5D)' | jq '.data.result[] | {instance: .metric.instance, violations: .value[1]}'

echo "HMAC Failures:"
curl -s 'http://prometheus:9090/api/v1/query?query=increase(hmac_validation_failures_total%5B7d%5D)' | jq '.data.result[] | {instance: .metric.instance, failures: .value[1]}'
```

---

## Alert Response Procedures

### Playbook: HMAC Validation Failures

**Severity**: 🔴 CRITICAL

1. **Immediate (< 5 min)**
   - Page on-call security engineer
   - Disable external API access (if critical)
   - Document incident start time

2. **Triage (< 15 min)**
   ```bash
   # Check gateway logs
   docker logs healthdata-gateway-service | grep -i "hmac\|signature"

   # Check for recent deployments
   git log --oneline -20

   # Verify signing secret configuration
   docker exec healthdata-gateway-service env | grep GATEWAY_AUTH
   ```

3. **Investigation (< 30 min)**
   - Compare `GATEWAY_AUTH_SIGNING_SECRET` on gateway vs all backend services
   - Verify no recent Dockerfile or config changes
   - Check if any services are in development mode
   - Review authentication filter logs for patterns

4. **Resolution**
   - If secret mismatch: Update docker-compose.yml and redeploy
   - If dev mode enabled: Disable and restart service
   - If signature generation error: Check gateway logs and rollback if necessary

5. **Post-Incident**
   - Document root cause
   - Review configuration management procedures
   - Update monitoring thresholds if needed
   - Schedule post-mortem within 24 hours

---

### Playbook: Tenant Isolation Violation

**Severity**: 🔴 CRITICAL

1. **Immediate (< 2 min)**
   - Page on-call security and ops
   - Prepare for account suspension
   - Alert security team

2. **Triage (< 5 min)**
   ```bash
   # Check which user/tenant combination violated
   kubectl logs -l app=backend-service --all-namespaces | grep -i "tenant.*violation"

   # Check user account activity
   docker exec healthdata-postgres psql -U healthdata -d healthdata_users \
     -c "SELECT * FROM audit_log WHERE action='UNAUTHORIZED_TENANT_ACCESS' ORDER BY created_at DESC LIMIT 10;"
   ```

3. **Investigation**
   - Identify user account responsible
   - Review all their recent API calls
   - Check for data access logs
   - Determine if data was actually accessed (query the backend)

4. **Action**
   - If data was accessed: Initiate data breach protocol
   - Suspend user account
   - Reset user credentials
   - Notify user of security incident

5. **Post-Incident**
   - Forensic analysis of user behavior
   - Determine if intentional or misconfiguration
   - Review access control policies
   - Document incident in HIPAA audit log

---

## HIPAA Compliance

### §164.312(a)(2)(i) - Access Controls

**Metric**: `auth_success_total`, `auth_failure_total`

- **Requirement**: User authentication with unique identifier
- **Validation**: All requests must pass `TrustedHeaderAuthFilter`
- **Audit**: `auth_success_total` tracks all authenticated access

### §164.312(a)(2)(iii) - Encryption & Decryption

**Metric**: `hmac_validation_failures_total`

- **Requirement**: Authentication credentials must be validated
- **Validation**: HMAC signature validates gateway origin
- **Audit**: Every validation failure is logged and counted

### §164.312(a)(3) - Audit Controls

**Metrics**: All authentication and tenant isolation metrics

- **Requirement**: Audit trail of user access to ePHI
- **Validation**: Prometheus metrics provide continuous audit trail
- **Retention**: Prometheus data retention = 15 days (configurable)

### §164.312(a)(1) - Information Access Management

**Metric**: `tenant_violations_total`

- **Requirement**: Access to ePHI limited to authorized users/roles
- **Validation**: `TrustedTenantAccessFilter` enforces tenant isolation
- **Audit**: Violation attempts are logged and counted

---

## Troubleshooting

### Metrics Not Appearing in Prometheus

**Problem**: No data in Prometheus for authentication metrics

**Solution**:
1. Verify Prometheus scrape targets are healthy:
   ```bash
   curl http://prometheus:9090/api/v1/targets
   ```

2. Check service health endpoints:
   ```bash
   curl http://localhost:8087/actuator/health
   curl http://localhost:8087/actuator/prometheus
   ```

3. Verify Micrometer is enabled:
   - Check `pom.xml` has `spring-boot-starter-actuator`
   - Check `application.yml` has `management.endpoints.web.include=prometheus`

4. Restart the service:
   ```bash
   docker-compose restart quality-measure-service
   ```

### Alerts Firing Constantly

**Problem**: Alert keeps firing even though issue is resolved

**Solution**:
1. Adjust alert duration threshold:
   ```yaml
   for: 10m  # Increase from 5m to give time for recovery
   ```

2. Check Prometheus evaluation:
   ```bash
   # Verify query returns expected results
   curl 'http://prometheus:9090/api/v1/query?query=ALERT_CONDITION'
   ```

3. Clear Prometheus cache if needed:
   ```bash
   docker-compose exec prometheus promtool check rules /etc/prometheus/rules/*.yml
   docker-compose restart prometheus
   ```

### High False-Positive Rate

**Problem**: Alerting on normal variations (e.g., brief latency spikes)

**Solution**:
1. Increase averaging window: `[5m]` → `[10m]`
2. Increase threshold: `> 50ms` → `> 100ms`
3. Increase alert duration: `for: 5m` → `for: 10m`
4. Use percentile instead of peak: `P95 > 50ms` instead of `MAX > 100ms`

---

## Next Steps

1. **Deploy to Staging**
   - Apply Prometheus rules and Grafana dashboard
   - Verify metrics collection for 24 hours
   - Test alert firing and notifications

2. **Set Up Alerting Channels**
   - Configure email notifications
   - Set up Slack/PagerDuty integration
   - Test alert routing

3. **Phase 4.1: HMAC Enforcement**
   - Monitor for signature validation issues
   - Use metrics to validate HMAC implementation

4. **Phase 4.3: mTLS Implementation**
   - Add TLS handshake metrics
   - Monitor certificate expiry

---

*Phase 4.2 Monitoring & Alerting Implementation Guide*
*HDIM - HealthData-in-Motion*
*December 31, 2025*
