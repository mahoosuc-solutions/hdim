# HDIM Monitoring Setup Guide

**Status:** ✅ Production Ready
**Last Updated:** January 24, 2026

## Overview

HDIM uses a comprehensive monitoring stack based on Prometheus, Grafana, and Alertmanager for metrics collection, visualization, and alerting across 30+ microservices.

**Stack Components:**
- **Prometheus** - Metrics collection and storage (30-day retention)
- **Grafana** - Dashboards and visualization
- **Alertmanager** - Alert routing and notification
- **Jaeger** - Distributed tracing
- **Node Exporter** - Host-level metrics

---

## Quick Start

### Start Monitoring Stack

```bash
cd backend/docker/monitoring
docker compose -f docker-compose.monitoring.yml up -d
```

### Access UIs

| Service | URL | Credentials |
|---------|-----|-------------|
| **Prometheus** | http://localhost:9090 | None |
| **Grafana** | http://localhost:3000 | admin/admin (change on first login) |
| **Alertmanager** | http://localhost:9093 | None |
| **Jaeger** | http://localhost:16686 | None |

---

## Prometheus Configuration

### Metrics Collection

**Scrape Interval:** 15 seconds
**Evaluation Interval:** 15 seconds
**Data Retention:** 30 days

**Monitored Services:** 30+ backend services organized by tier:
- **Gateway:** gateway-service
- **Core:** cql-engine-service, fhir-service, patient-service
- **Analytics:** quality-measure-service, care-gap-service, analytics-service, predictive-analytics-service
- **Clinical:** consent-service, documentation-service, sdoh-service
- **Integration:** ehr-connector-service, cdr-processor-service, migration-workflow-service
- **Payer:** prior-auth-service, payer-workflows-service, hcc-service
- **Reporting:** qrda-export-service, ecr-service
- **Events:** event-router-service, event-processing-service
- **AI:** agent-runtime-service, agent-builder-service, ai-assistant-service
- **Workflow:** approval-service

### Service Discovery

**Static Configuration:** Services are manually configured in `prometheus.yml`

**Endpoint:** All Spring Boot services expose metrics at `/actuator/prometheus`

**Labels:** Each service is tagged with:
- `service` - Service name
- `tier` - Service tier (gateway, core, analytics, etc.)

### Verify Metrics Exposure

```bash
# Check if service exposes Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# Example metrics:
# jvm_memory_used_bytes
# http_server_requests_seconds
# system_cpu_usage
# hikaricp_connections_active
```

---

## Alert Rules

### Alert Categories

**17 configured alert rules** across 5 categories:

#### 1. Availability
- **ServiceDown** - Service is unreachable for 2+ minutes (critical)

#### 2. Resource Utilization
- **HighCPUUsage** - CPU >85% for 5 minutes (warning)
- **HighMemoryUsage** - Heap >85% for 5 minutes (warning)
- **CriticalMemoryUsage** - Heap >95% for 2 minutes (critical)
- **DiskSpaceLow** - <15% free for 5 minutes (warning)
- **CriticalDiskSpace** - <5% free for 1 minute (critical)

#### 3. Application Performance
- **HighErrorRate** - 5xx errors >5% for 5 minutes (warning)
- **CriticalErrorRate** - 5xx errors >20% for 2 minutes (critical)
- **SlowResponseTime** - P95 >2 seconds for 5 minutes (warning)

#### 4. Infrastructure
- **DatabaseConnectionPoolLow** - >85% utilization for 5 minutes (warning)
- **JVMThreadDeadlock** - Deadlock detected (critical)
- **KafkaConsumerLag** - Lag >10,000 messages for 10 minutes (warning)

#### 5. Business/Compliance
- **AuditLogFailure** - HIPAA audit logging failure (critical)
- **PHIAccessSpike** - PHI access 3x normal rate (warning)
- **LowCacheHitRate** - <50% for 10 minutes (info)

### Alert Severity Levels

| Severity | Response Time | Notification |
|----------|---------------|--------------|
| **critical** | Immediate | Slack + Email + PagerDuty |
| **warning** | 5-30 minutes | Slack (batched) |
| **info** | Best effort | Slack (daily digest) |

### Testing Alerts

```bash
# Trigger test alert
curl -X POST http://localhost:9093/api/v1/alerts \
  -H 'Content-Type: application/json' \
  -d '[{
    "labels": {
      "alertname": "TestAlert",
      "severity": "warning",
      "service": "test-service"
    },
    "annotations": {
      "summary": "This is a test alert",
      "description": "Testing alert routing"
    }
  }]'
```

---

## Alertmanager Configuration

### Alert Routing

**Grouping:** Alerts grouped by `alertname`, `service`, and `severity`

**Routing Rules:**
1. **Critical** → Immediate notification, repeat every 30 minutes
2. **Compliance** → Immediate notification, repeat every hour
3. **Security** → 30-second wait, repeat every 2 hours
4. **Warning** → 5-minute batching, repeat every 4 hours
5. **Info** → 10-minute batching, repeat daily

### Notification Channels

**Configured Receivers:**
1. **default** - Webhook to localhost:5001
2. **critical-alerts** - Slack #hdim-alerts-critical
3. **compliance-alerts** - Slack #hdim-compliance
4. **security-alerts** - Slack #hdim-security
5. **warning-alerts** - Slack #hdim-alerts
6. **info-alerts** - Slack #hdim-alerts (no resolution)

### Configure Slack Notifications

```bash
# Set Slack webhook URL
export SLACK_WEBHOOK_URL='https://hooks.slack.com/services/YOUR/WEBHOOK/URL'

# Restart Alertmanager
docker compose -f docker-compose.monitoring.yml restart alertmanager
```

### Silence Alerts

```bash
# Silence alert via UI
http://localhost:9093/#/silences

# Silence via API
curl -X POST http://localhost:9093/api/v2/silences \
  -H 'Content-Type: application/json' \
  -d '{
    "matchers": [
      {
        "name": "alertname",
        "value": "HighCPUUsage",
        "isRegex": false
      }
    ],
    "startsAt": "2026-01-24T00:00:00Z",
    "endsAt": "2026-01-24T23:59:59Z",
    "createdBy": "admin",
    "comment": "Scheduled maintenance window"
  }'
```

---

## Grafana Dashboards

### Provisioned Dashboards

Grafana automatically loads dashboards from:
```
backend/docker/monitoring/grafana/dashboards/
```

**Recommended Dashboards:**
1. **System Overview** - All services health at a glance
2. **Service Detail** - Individual service deep dive
3. **API Performance** - Request rates, latency, errors
4. **Database Metrics** - Connection pools, query performance
5. **Cache Metrics** - Hit rates, evictions
6. **Kafka Metrics** - Producer/consumer throughput, lag
7. **User Activity** - PHI access, user sessions

### Import Community Dashboards

```bash
# Import JVM dashboard
Dashboard ID: 4701 (JVM Micrometer)

# Import Spring Boot dashboard
Dashboard ID: 12900 (Spring Boot Statistics)

# Import PostgreSQL dashboard
Dashboard ID: 9628 (PostgreSQL Database)
```

---

## Distributed Tracing (Jaeger)

### Access Jaeger UI

http://localhost:16686

### Trace a Request

1. Make an API request
2. Open Jaeger UI
3. Select service from dropdown
4. View trace timeline

### Example Trace Query

```bash
# Find traces for patient-service with errors
Service: patient-service
Tags: error=true
Min Duration: 1s
```

---

## Troubleshooting

### Prometheus Not Scraping Services

**Symptom:** `up{service="..."}` shows 0

**Check:**
```bash
# Verify service is running
docker compose ps patient-service

# Check if metrics endpoint is accessible
curl http://patient-service:8080/actuator/prometheus

# Check Prometheus targets
http://localhost:9090/targets
```

**Fix:** Ensure service is on `hdim-network` and exposes port 8080

---

### Alertmanager Not Sending Notifications

**Symptom:** Alerts firing but no notifications

**Check:**
```bash
# View Alertmanager status
http://localhost:9093/#/status

# Check configuration
docker compose -f docker-compose.monitoring.yml \
  exec alertmanager cat /etc/alertmanager/alertmanager.yml
```

**Fix:** Verify `SLACK_WEBHOOK_URL` is set correctly

---

### Grafana Cannot Connect to Prometheus

**Symptom:** "Data source is not working"

**Check:**
```bash
# Test Prometheus from Grafana container
docker compose -f docker-compose.monitoring.yml \
  exec grafana curl http://prometheus:9090/api/v1/query?query=up
```

**Fix:** Verify both are on same Docker network (`hdim-network`)

---

### High Disk Usage

**Symptom:** Prometheus volume filling up

**Check:**
```bash
# Check Prometheus storage size
docker volume inspect monitoring_prometheus_data

# Check retention settings
docker compose -f docker-compose.monitoring.yml \
  exec prometheus cat /etc/prometheus/prometheus.yml
```

**Fix:** Adjust retention period or increase volume size

---

## Production Deployment

### Prerequisites

- [ ] Slack webhook URL configured
- [ ] Email SMTP configured (optional)
- [ ] PagerDuty integration configured (optional)
- [ ] All services exposing `/actuator/prometheus`
- [ ] Network configured (`hdim-network`)

### Deployment Steps

1. **Configure Secrets**
   ```bash
   export GRAFANA_ADMIN_PASSWORD='secure-password'
   export SLACK_WEBHOOK_URL='https://hooks.slack.com/...'
   ```

2. **Start Stack**
   ```bash
   docker compose -f docker-compose.monitoring.yml up -d
   ```

3. **Verify Services**
   ```bash
   docker compose -f docker-compose.monitoring.yml ps
   ```

4. **Check Targets**
   - Open http://localhost:9090/targets
   - Verify all services show "UP" status

5. **Import Dashboards**
   - Open Grafana
   - Import recommended dashboards
   - Configure alerts

6. **Test Alerting**
   - Trigger test alert
   - Verify notification received
   - Adjust routing as needed

---

## Metrics Reference

### Spring Boot Actuator Metrics

**JVM:**
- `jvm_memory_used_bytes` - Heap/non-heap memory
- `jvm_threads_live` - Active threads
- `jvm_gc_pause_seconds` - GC pause time

**HTTP:**
- `http_server_requests_seconds` - Request duration
- `http_server_requests_seconds_count` - Request count

**Database:**
- `hikaricp_connections_active` - Active DB connections
- `hikaricp_connections_max` - Max pool size

**Custom:**
- `phi_access_total` - PHI access count
- `audit_log_failures_total` - Audit failures
- `cache_gets_total` - Cache operations

---

## Related Documentation

- [Distributed Tracing Guide](./DISTRIBUTED_TRACING_GUIDE.md)
- [HIPAA Compliance](../HIPAA-CACHE-COMPLIANCE.md)
- [Production Security](../docs/PRODUCTION_SECURITY_GUIDE.md)

---

## Issue Reference

- **Issue:** #271 - Monitoring Setup - Prometheus
- **Issue:** #272 - Monitoring Setup - Grafana
- **Status:** Production Ready ✅
- **Implementation Date:** January 24, 2026
