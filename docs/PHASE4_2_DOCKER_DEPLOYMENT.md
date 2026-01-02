# Phase 4.2: Docker Deployment Plan - Monitoring & Alerting

**Status**: 📋 Planning Phase
**Target**: Staging Environment
**Timeline**: Parallel with Phase 4.1

---

## Docker Infrastructure Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     Docker Compose Network                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │            Backend Services (18 total)                   │   │
│  │  Quality-Measure, FHIR, Patient, CQL-Engine,            │   │
│  │  Care-Gap, Prior-Auth, Sales-Auto, SDOH, ECR, HCC, etc  │   │
│  │  ├─ :8087, :8085, :8084, :8081, :8086, :8102, :8106,   │   │
│  │  │  :8094, :8101, :8105, ...                             │   │
│  │  └─ /actuator/prometheus (metrics endpoint)             │   │
│  └──────────────┬───────────────────────────────────────────┘   │
│                 │ metrics                                        │
│                 ▼                                                │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Prometheus Server                                       │   │
│  │  ├─ Port: 9090                                           │   │
│  │  ├─ Config: prometheus.yml                              │   │
│  │  ├─ Rules: rules/security-alerts.yml                    │   │
│  │  └─ Volume: prometheus-data (persistent)                │   │
│  └──────────────┬───────────────────────────────────────────┘   │
│                 │ time series data                               │
│                 ▼                                                │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  Grafana Dashboard                                       │   │
│  │  ├─ Port: 3001                                           │   │
│  │  ├─ Data Source: Prometheus                             │   │
│  │  ├─ Dashboard: hdim-authentication-security.json        │   │
│  │  └─ Volume: grafana-data (persistent)                   │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  AlertManager (Optional - Phase 4.2B)                    │   │
│  │  ├─ Port: 9093                                           │   │
│  │  └─ Handles: Alert routing, notifications               │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Phase 4.2A: Metrics Collection Setup

### Task 1: Update Prometheus Configuration

**File**: `docker/prometheus/prometheus.yml`

**Changes Required**:

1. **Add Global Settings**
   ```yaml
   global:
     scrape_interval: 15s
     evaluation_interval: 15s
     external_labels:
       environment: 'docker'
       cluster: 'hdim'
   ```

2. **Add Scrape Configs for All Services**
   ```yaml
   scrape_configs:
     - job_name: 'quality-measure-service'
       static_configs:
         - targets: ['quality-measure-service:8087']
       metrics_path: '/actuator/prometheus'
       scrape_interval: 30s

     - job_name: 'prior-auth-service'
       static_configs:
         - targets: ['prior-auth-service:8102']
       metrics_path: '/actuator/prometheus'
       scrape_interval: 30s

     # ... (continue for all 18 backend services)
     # See: docs/PHASE4_MONITORING_GUIDE.md for complete list
   ```

3. **Add Rule File Locations**
   ```yaml
   rule_files:
     - '/etc/prometheus/rules/*.yml'
   ```

### Task 2: Load Prometheus Rules

**File**: `docker/prometheus/rules/security-alerts.yml`

**Status**: ✅ Already created in Phase 4.2

**Deployment**:
```bash
# Copy rules file to Prometheus config directory
docker cp docker/prometheus/rules/security-alerts.yml healthdata-prometheus:/etc/prometheus/rules/

# Reload Prometheus configuration
docker exec healthdata-prometheus promtool check rules /etc/prometheus/rules/security-alerts.yml
docker exec healthdata-prometheus kill -HUP 1  # Reload config
```

### Task 3: Verify Prometheus Metrics Collection

**Steps**:
1. Start Prometheus
2. Navigate to `http://localhost:9090`
3. Search for metrics:
   - `auth_success_total`
   - `auth_failure_total`
   - `tenant_violations_total`
4. Verify scrape targets are healthy:
   - Go to Status → Targets
   - Confirm all 18 backend services show "UP"
   - Check if metrics are being collected

**Success Criteria**:
```
✅ All backend services: UP (should have 18)
✅ Metrics available: auth_success_total, auth_failure_total, etc.
✅ No "DOWN" targets
✅ Scrape time < 2 seconds per target
```

---

## Phase 4.2B: Grafana Dashboard Setup

### Task 4: Configure Grafana Data Source

**File**: `docker/grafana/provisioning/datasources/prometheus.yml`

**Content**:
```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: true
    jsonData:
      timeInterval: 15s
```

### Task 5: Import Grafana Dashboard

**File**: `docker/grafana/dashboards/hdim-authentication-security.json`

**Status**: ✅ Already created in Phase 4.2

**Deployment Options**:

#### Option A: Manual Import (Web UI)
```
1. Navigate to http://localhost:3001
2. Login (admin/password)
3. Go to Dashboards → Import
4. Upload: docker/grafana/dashboards/hdim-authentication-security.json
5. Select: Prometheus data source
6. Click: Import
```

#### Option B: Automatic Provisioning (Recommended)
```bash
# Create provisioning directory if needed
mkdir -p docker/grafana/provisioning/dashboards

# Copy dashboard to provisioning directory
cp docker/grafana/dashboards/hdim-authentication-security.json \
   docker/grafana/provisioning/dashboards/

# Add provisioning config
cat > docker/grafana/provisioning/dashboards/dashboards.yml << 'EOF'
apiVersion: 1

providers:
  - name: 'HDIM Dashboards'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /etc/grafana/provisioning/dashboards
EOF
```

### Task 6: Update docker-compose.yml

**Changes Required**:

```yaml
services:
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./docker/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - ./docker/prometheus/rules:/etc/prometheus/rules
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--storage.tsdb.retention.time=15d'  # 15-day retention
    networks:
      - hdim-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9090/-/healthy"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 30s

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3001:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_SECURITY_ADMIN_USER=admin
      - GF_INSTALL_PLUGINS=grafana-piechart-panel
    volumes:
      - ./docker/grafana/provisioning/datasources:/etc/grafana/provisioning/datasources
      - ./docker/grafana/provisioning/dashboards:/etc/grafana/provisioning/dashboards
      - grafana-data:/var/lib/grafana
    networks:
      - hdim-network
    depends_on:
      - prometheus
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:3000/api/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

volumes:
  prometheus-data:
    driver: local
  grafana-data:
    driver: local
```

---

## Phase 4.2C: Deployment Verification

### Task 7: Health Check Verification

**Script**: `docker/health-checks.sh`

```bash
#!/bin/bash

echo "=== Phase 4.2 Docker Deployment Health Checks ==="
echo

# Check Prometheus
echo "1. Prometheus Health"
curl -f http://localhost:9090/-/healthy && echo "✅ Prometheus UP" || echo "❌ Prometheus DOWN"

# Check Grafana
echo "2. Grafana Health"
curl -f http://localhost:3000/api/health && echo "✅ Grafana UP" || echo "❌ Grafana DOWN"

# Check metrics collection
echo "3. Metrics Collection"
METRICS=$(curl -s http://localhost:9090/api/v1/query?query=auth_success_total | jq '.data.result | length')
echo "   Found $METRICS metrics sources"
[ "$METRICS" -gt 0 ] && echo "✅ Metrics Collected" || echo "❌ No Metrics Found"

# Check alert rules
echo "4. Alert Rules"
RULES=$(curl -s http://localhost:9090/api/v1/rules | jq '.data.groups | length')
echo "   Found $RULES rule groups"
[ "$RULES" -gt 0 ] && echo "✅ Rules Loaded" || echo "❌ No Rules Loaded"

# Check individual services
echo "5. Backend Services Metrics"
for service in quality-measure-service prior-auth-service sales-automation-service fhir-service; do
    HEALTH=$(curl -s http://$service:$(docker inspect -f '{{.Config.ExposedPorts | keys}}' healthdata-$service | grep -oE '[0-9]+' | head -1)/actuator/prometheus | grep -c "auth_success_total")
    if [ "$HEALTH" -gt 0 ]; then
        echo "✅ $service: Metrics available"
    else
        echo "❌ $service: No metrics found"
    fi
done

echo
echo "=== Health Check Complete ==="
```

**Expected Output**:
```
✅ Prometheus UP
✅ Grafana UP
✅ Metrics Collected (Found 18 metrics sources)
✅ Rules Loaded (Found 1 rule groups)
✅ quality-measure-service: Metrics available
✅ prior-auth-service: Metrics available
✅ sales-automation-service: Metrics available
✅ fhir-service: Metrics available
```

### Task 8: Dashboard Validation

**Steps**:
1. Open `http://localhost:3001`
2. Login: admin / admin
3. Navigate to "HDIM - Authentication & Security Metrics"
4. Verify all 6 panels loading:
   - [ ] Authentication Success Rate (should show line graph)
   - [ ] Authentication Failures Rate (should show gauge)
   - [ ] Authentication Latency (should show P50/P95 lines)
   - [ ] HMAC Validation Failures (should show gauge, likely 0)
   - [ ] Tenant Isolation Violations (should show line graph, likely 0)
   - [ ] Missing Tenant Context (should show line graph)

**Expected Visualizations**:
- All graphs have data (not "No data" or gray panels)
- Latency graph shows values < 100ms
- Success rate > 95%
- HMAC failures = 0 (in dev mode, expected)

---

## Phase 4.2D: Alert Testing

### Task 9: Test Alert Firing

**Test Case 1: High Failure Rate Alert**

```bash
# Generate failures by making unauthenticated requests
for i in {1..100}; do
  curl -X GET http://localhost:8102/api/test 2>/dev/null
done

# Check if alert fires
curl -s http://localhost:9090/api/v1/rules | jq '.data.groups[0].rules[] | select(.name=="HighAuthenticationFailureRate")'

# Expected: Alert should be FIRING after ~5 minutes
```

**Test Case 2: HMAC Validation Alert**

```bash
# Currently won't fire in dev mode (expected)
# This will fire when Phase 4.1 (HMAC enforcement) is enabled

# To test, temporarily disable dev mode:
# 1. Set GATEWAY_AUTH_DEV_MODE=false in docker-compose.yml
# 2. Send request with invalid signature
# 3. Check if HMACValidationFailures alert fires
# 4. Re-enable dev mode
```

**Test Case 3: Tenant Isolation Alert**

```bash
# Simulate cross-tenant access attempt (requires code to allow this for testing)
# Check tenant_violations_total metric increases
curl -s http://localhost:9090/api/v1/query?query=increase(tenant_violations_total%5B5m%5D)

# Expected: Should show 0 in normal operation
```

### Task 10: Notifications Setup (Optional - Phase 4.2B)

**AlertManager Configuration** (if implementing Phase 4.2B):

```yaml
# docker/alertmanager/alertmanager.yml

global:
  resolve_timeout: 5m
  slack_api_url: 'YOUR_SLACK_WEBHOOK_URL'

route:
  receiver: 'default'
  group_by: ['alertname', 'cluster']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 6h
  routes:
    - match:
        severity: critical
      receiver: 'critical-pagerduty'
      continue: true
    - match:
        severity: warning
      receiver: 'warning-slack'

receivers:
  - name: 'default'
    slack_configs:
      - channel: '#alerts-general'
        title: 'HDIM Alert: {{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'

  - name: 'critical-pagerduty'
    pagerduty_configs:
      - service_key: 'YOUR_PAGERDUTY_KEY'

  - name: 'warning-slack'
    slack_configs:
      - channel: '#alerts-warning'
        title: 'HDIM Warning: {{ .GroupLabels.alertname }}'
```

---

## Deployment Checklist

### Pre-Deployment
- [ ] All 18 backend services compiled with metrics
- [ ] prometheus.yml contains all service scrape configs
- [ ] security-alerts.yml created
- [ ] hdim-authentication-security.json created
- [ ] docker-compose.yml updated with Prometheus and Grafana

### Deployment (Staging)
- [ ] Stop existing containers: `docker-compose down`
- [ ] Pull latest code: `git pull`
- [ ] Rebuild services: `docker-compose build --no-cache`
- [ ] Start services: `docker-compose up -d`
- [ ] Verify containers running: `docker-compose ps`

### Post-Deployment Validation
- [ ] Prometheus running: `curl http://localhost:9090/-/healthy`
- [ ] Grafana running: `curl http://localhost:3000/api/health`
- [ ] Metrics collected: Check target status in Prometheus
- [ ] Dashboard loaded: Verify all 6 panels in Grafana
- [ ] Alert rules loaded: Check rules in Prometheus
- [ ] Generate test traffic: Run 100+ requests to backend services
- [ ] Verify dashboard updates: Graphs should show data
- [ ] Test alert scenarios: Try triggering warnings/criticals

### Production Deployment
- [ ] Complete all staging validation
- [ ] Backup current Prometheus data
- [ ] Plan maintenance window (minimal disruption)
- [ ] Set up notification channels (Slack, PagerDuty)
- [ ] Deploy with blue-green strategy (old + new running, then switch)
- [ ] Monitor for 24 hours post-deployment
- [ ] Document metrics baseline for future comparison

---

## Rollback Plan

If issues occur during deployment:

```bash
# 1. Stop new deployment
docker-compose down

# 2. Restore previous docker-compose.yml
git checkout HEAD~1 -- docker-compose.yml

# 3. Restart previous version
docker-compose up -d

# 4. Verify services are running
docker-compose ps
```

---

## Monitoring the Monitoring

**Key Metrics to Watch Post-Deployment**:

1. **Prometheus Uptime**: Should be 99.9%+
2. **Scrape Success Rate**: Should be 100% for all targets
3. **Disk Usage**: Monitor Prometheus data growth
   - ~2GB per day typical (15-day retention)
4. **Memory Usage**: Prometheus typically uses 500MB-1GB
5. **Grafana Load Time**: Should be < 2 seconds per dashboard

**Troubleshooting**:

| Problem | Solution |
|---------|----------|
| Metrics not appearing | Check service scrape endpoint: `/actuator/prometheus` |
| High disk usage | Reduce retention time in Prometheus config |
| Dashboard slow | Check Prometheus query performance, adjust refresh rate |
| Alerts not firing | Verify rules syntax, check alert query in Prometheus |

---

## Next Steps

### Immediate (Phase 4.2 Deployment)
1. Update Prometheus configuration
2. Deploy to staging
3. Validate metrics collection
4. Test alerts

### Short-term (Phase 4.1 Integration)
- HMAC metrics will start showing failures when enforcement enabled
- Monitor `hmac_validation_failures_total` closely

### Medium-term (Phase 4.3 Integration)
- Add TLS metrics for certificate monitoring
- Track mTLS handshake performance

---

*Phase 4.2 Docker Deployment Plan*
*HDIM - HealthData-in-Motion*
*December 31, 2025*
