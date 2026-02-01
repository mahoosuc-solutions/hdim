# MONITORING & OBSERVABILITY GUIDE

Comprehensive monitoring, observability, and alerting standards for HDIM production environment.

**Last Updated**: January 19, 2026
**Status**: Phase 2, P1 Critical Guide
**Focus**: Prometheus, Grafana, Jaeger, OpenTelemetry, alerting, SLA monitoring

---

## Overview

**Observability** enables understanding system behavior through logs, metrics, and traces. **Monitoring** tracks these signals to detect and alert on anomalies.

### The Three Pillars of Observability

```
       Logs          Metrics         Traces
        │               │               │
        ├─ Debug info    ├─ Time series  ├─ Request paths
        ├─ Errors        ├─ Latency      ├─ Dependencies
        ├─ Warnings      ├─ Throughput   ├─ Timing breakdown
        └─ Business      └─ Resource use └─ Error causation
```

### Current HDIM Status

- ✅ **Prometheus** running (metrics collection)
- ✅ **Grafana** running (metric visualization)
- ✅ **Jaeger** running (distributed tracing)
- ✅ **OpenTelemetry** integrated in all services
- ⚠️ **Alerting rules** not configured
- ⚠️ **SLA dashboards** not created
- ⚠️ **Runbooks** not documented

---

## Metrics Collection (Prometheus)

### What Prometheus Does

```
Every 15 seconds:
1. Scrape each service's /actuator/prometheus endpoint
2. Collect metrics (JVM memory, HTTP latency, custom business metrics)
3. Store in time-series database
4. Make available for queries and alerting
```

### Prometheus Configuration

```yaml
# docker/prometheus/prometheus.yml

global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'patient-service'
    static_configs:
      - targets: ['patient-service:8084']
    metrics_path: '/actuator/prometheus'

  - job_name: 'quality-measure-service'
    static_configs:
      - targets: ['quality-measure-service:8087']

  - job_name: 'care-gap-service'
    static_configs:
      - targets: ['care-gap-service:8086']

  # ... (all 50+ services)

  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
```

### Accessing Prometheus UI

**URL**: http://localhost:9090

**Key Sections**:
- **Graph**: Interactive query builder
- **Alerts**: Active alert rules
- **Status**: Service health, targets

### Common Prometheus Queries

```promql
# HTTP Request Rate (per second)
rate(http_server_requests_seconds_count[1m])

# HTTP Response Time (p95)
histogram_quantile(0.95, http_server_requests_seconds)

# Error Rate (5xx responses)
rate(http_server_requests_seconds_count{status=~"5.."}[1m])

# JVM Memory Usage
jvm_memory_used_bytes{area="heap"}

# Database Connection Pool
hikaricp_connections_active

# Cache Hit Rate
cache_gets_hit / (cache_gets_hit + cache_gets_miss)

# Kafka Consumer Lag
kafka_consumer_lag_sum

# CPU Usage per Service
rate(process_cpu_seconds_total[1m])
```

---

## Metrics Visualization (Grafana)

### What Grafana Does

```
1. Connects to Prometheus data source
2. Creates dashboards with charts, graphs, heatmaps
3. Shows trends, patterns, anomalies
4. Enables drill-down analysis
```

### Accessing Grafana

**URL**: http://localhost:3001
**Default Login**: admin / admin (change in production!)

### Dashboard Organization

```
Grafana Dashboards/
├── System Health
│   ├── Overall Service Status
│   ├── Resource Usage (CPU, Memory, Disk)
│   └── Database Health
├── API Performance
│   ├── Patient Service
│   ├── Quality Measure Service
│   ├── Care Gap Service
│   └── FHIR Service
├── Business Metrics
│   ├── Measure Evaluations
│   ├── Care Gaps Detected
│   └── Patient Processing
├── Infrastructure
│   ├── Kubernetes Clusters
│   ├── Docker Containers
│   └── Database Performance
└── Alerts & Issues
    ├── Active Alerts
    ├── Error Rates
    └── SLA Violations
```

### Creating a Dashboard for Patient Service

```json
{
  "dashboard": {
    "title": "Patient Service Monitoring",
    "panels": [
      {
        "title": "Request Rate",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count{job='patient-service'}[1m])"
          }
        ],
        "type": "graph"
      },
      {
        "title": "Response Time (p95)",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, http_server_requests_seconds{job='patient-service'})"
          }
        ]
      },
      {
        "title": "Error Rate",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count{job='patient-service',status=~'5..'}[1m])"
          }
        ],
        "alert": {
          "message": "Error rate above 1%"
        }
      },
      {
        "title": "JVM Memory Usage",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes{job='patient-service'}"
          }
        ]
      }
    ]
  }
}
```

### Key Grafana Features

| Feature          | Use Case                              |
| ---------------- | ------------------------------------- |
| **Alerts**       | Trigger notifications when metric    |
| **Templating**   | Dynamic dashboard variables          |
| **Annotations**  | Mark deployments, incidents          |
| **Plugins**      | Extend with custom visualizations    |
| **Snapshots**    | Share dashboard states               |

---

## Distributed Tracing (Jaeger)

### What Jaeger Does

```
Every request flows through multiple services:

User Request → Gateway → Patient Service → Quality Measure Service
     │                           │                    │
     └─────────────────────────────────────────────────┘
         All linked by trace-id for end-to-end visibility
```

### Accessing Jaeger UI

**URL**: http://localhost:16686

**Key Sections**:
- **Search**: Find traces by service, operation, tag
- **Trace Detail**: View request flow, latencies, spans
- **Service Graph**: Visualize service dependencies
- **Compare**: Side-by-side trace comparison

### Trace Propagation in HDIM

HDIM uses OpenTelemetry SDK with automatic trace propagation:

```java
// Automatic (no code changes needed):
// 1. HTTP requests: Trace headers propagated automatically
// 2. Kafka messages: Trace context embedded in message headers
// 3. Database calls: Tracing interceptor records query timing

// Manual span creation for business operations:
@Service
public class QualityMeasureService {
    private final Tracer tracer;

    public EvaluationResult evaluateMeasure(String measureId, String patientId) {
        Span span = tracer.spanBuilder("evaluate_measure")
            .setAttribute("measure.id", measureId)
            .setAttribute("patient.id", patientId)
            .startSpan();

        try (Scope scope = span.makeCurrent()) {
            // Business logic
            result = performEvaluation(measureId, patientId);
            span.setAttribute("result.score", result.getScore());
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
```

### Querying Traces

**Find slow requests:**
```
Service: fhir-service
Operation: evaluateMeasure
Duration: > 5000ms  (5 seconds)
```

**Find errors:**
```
Service: patient-service
Status: ERROR
```

**Find specific patient operations:**
```
Service: quality-measure-service
Tags: patient.id=PATIENT-123
```

### Key Trace Metrics

| Metric        | Target  | Alert Threshold |
| ------------- | ------- | --------------- |
| End-to-end latency (p95) | <5s | >10s |
| Service latency (p95) | <500ms | >1s |
| Error rate | <1% | >2% |
| Span count per trace | <100 | >200 |

---

## Alerting Rules

### Alert Definition Structure

```yaml
# prometheus/alert-rules.yml

groups:
  - name: patient_service_alerts
    interval: 30s
    rules:
      - alert: PatientServiceHighErrorRate
        expr: |
          (
            rate(http_server_requests_seconds_count{
              job="patient-service",
              status=~"5.."
            }[5m])
            /
            rate(http_server_requests_seconds_count{
              job="patient-service"
            }[5m])
          ) > 0.05
        for: 2m
        annotations:
          summary: "High error rate on patient-service"
          description: "Error rate is {{ $value | humanizePercentage }}"
          runbook: "https://wiki.company.com/runbooks/patient-service-errors"
        labels:
          severity: "critical"
          team: "platform"
```

### Critical Alerts for Production

#### Alert 1: High Error Rate

```yaml
- alert: ServiceHighErrorRate
  expr: |
    (
      rate(http_server_requests_seconds_count{status=~"5.."}[5m])
      /
      rate(http_server_requests_seconds_count[5m])
    ) > 0.05
  for: 2m
  annotations:
    severity: critical
    message: "{{ $labels.service }} error rate is {{ $value | humanizePercentage }}"
```

#### Alert 2: High Latency (SLA Violation)

```yaml
- alert: HighLatency
  expr: |
    histogram_quantile(0.95, http_server_requests_seconds) > 1
  for: 5m
  annotations:
    severity: warning
    message: "{{ $labels.service }} p95 latency is {{ $value }}s"
```

#### Alert 3: High Memory Usage

```yaml
- alert: HighMemoryUsage
  expr: |
    (jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) > 0.85
  for: 5m
  annotations:
    severity: warning
    message: "{{ $labels.service }} heap memory usage is {{ $value | humanizePercentage }}"
```

#### Alert 4: Database Connection Pool Exhaustion

```yaml
- alert: ConnectionPoolExhausted
  expr: |
    hikaricp_connections_active / hikaricp_connections{state="idle"} > 0.9
  for: 2m
  annotations:
    severity: critical
    message: "Database connection pool nearly exhausted"
```

#### Alert 5: Kafka Consumer Lag

```yaml
- alert: HighKafkaConsumerLag
  expr: |
    kafka_consumer_lag_sum > 10000
  for: 5m
  annotations:
    severity: warning
    message: "Kafka consumer lag is {{ $value }} messages"
```

### Alert Routing (Alertmanager)

```yaml
# docker/prometheus/alertmanager.yml

global:
  resolve_timeout: 5m

route:
  receiver: 'default'
  group_by: ['alertname', 'cluster', 'service']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 12h

  routes:
    # Critical alerts → PagerDuty immediately
    - match:
        severity: critical
      receiver: 'pagerduty'
      continue: true
      repeat_interval: 15m

    # Warnings → Slack
    - match:
        severity: warning
      receiver: 'slack'
      repeat_interval: 1h

receivers:
  - name: 'default'
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/...'
        channel: '#monitoring'

  - name: 'pagerduty'
    pagerduty_configs:
      - service_key: '{{ .GroupLabels.service_key }}'
        description: '{{ .GroupLabels.alertname }}: {{ .Alerts.Firing | len }} firing'
```

---

## Custom Business Metrics

Beyond standard infrastructure metrics, HDIM tracks business-relevant metrics:

### Patient Service Metrics

```java
@Service
public class PatientService {
    private final MeterRegistry meterRegistry;

    public Patient createPatient(CreatePatientRequest request) {
        Patient patient = patientRepository.save(mapToEntity(request));

        // Track creation
        Counter.builder("patient.created")
            .tag("tenant", getTenantId())
            .register(meterRegistry)
            .increment();

        // Track by status
        Gauge.builder("patient.active.count", () ->
            patientRepository.countByStatus("ACTIVE"))
            .tag("tenant", getTenantId())
            .register(meterRegistry);

        return patient;
    }
}
```

### Quality Measure Metrics

```java
@Service
public class QualityMeasureService {
    private final MeterRegistry meterRegistry;
    private final Timer evaluationTimer;

    public QualityMeasureService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.evaluationTimer = Timer.builder("measure.evaluation.duration")
            .publishPercentiles(0.5, 0.95, 0.99)
            .minimumExpectedValue(Duration.ofMillis(10))
            .maximumExpectedValue(Duration.ofSeconds(60))
            .register(meterRegistry);
    }

    public EvaluationResult evaluateMeasure(String measureId, String patientId) {
        return evaluationTimer.recordCallable(() -> {
            EvaluationResult result = performEvaluation(measureId, patientId);

            // Track pass/fail
            Counter.builder("measure.evaluation.result")
                .tag("measure", measureId)
                .tag("result", result.isPassed() ? "passed" : "failed")
                .register(meterRegistry)
                .increment();

            return result;
        });
    }
}
```

### Business Dashboard (Grafana)

**Metrics to display:**
- Patients created/day
- Evaluations completed/hour
- Care gaps detected/day
- Measure pass rate by measure
- Top performing/underperforming measures

---

## Logging Standards

### Structured Logging (JSON Format)

All HDIM services use structured logging for better searchability:

```java
@Slf4j
@Service
public class PatientService {
    public Patient getPatient(String patientId, String tenantId) {
        try {
            log.info("Fetching patient", Map.of(
                "patientId", patientId,
                "tenantId", tenantId,
                "action", "PATIENT_FETCH"
            ));

            Patient patient = patientRepository.findByIdAndTenant(patientId, tenantId)
                .orElseThrow(() -> {
                    log.warn("Patient not found", Map.of(
                        "patientId", patientId,
                        "tenantId", tenantId,
                        "action", "PATIENT_NOT_FOUND"
                    ));
                    return new ResourceNotFoundException("Patient", patientId);
                });

            log.info("Patient fetched successfully", Map.of(
                "patientId", patientId,
                "tenantId", tenantId,
                "action", "PATIENT_FETCH_SUCCESS",
                "duration_ms", System.currentTimeMillis()
            ));

            return patient;
        } catch (Exception e) {
            log.error("Error fetching patient", Map.of(
                "patientId", patientId,
                "tenantId", tenantId,
                "action", "PATIENT_FETCH_ERROR",
                "error", e.getMessage()
            ), e);
            throw e;
        }
    }
}
```

### Log Levels

| Level | Use Case                    | Example                       |
| ----- | --------------------------- | ----------------------------- |
| DEBUG | Development/troubleshooting | "CQL expression evaluated to:" |
| INFO  | Business events             | "Patient created", "Evaluation started" |
| WARN  | Degraded operation          | "Cache miss", "Slow query"    |
| ERROR | Failures                    | Exceptions, data loss risks   |
| FATAL | Unrecoverable               | Service won't start           |

### Log Filtering (application.yml)

```yaml
logging:
  level:
    # Services: INFO level
    com.healthdata: INFO

    # Framework: WARN level (reduce noise)
    org.springframework: WARN
    org.hibernate: WARN
    org.apache.kafka: WARN

    # Debug specific issues:
    # com.healthdata.patient.service: DEBUG  # Uncomment to debug
```

---

## SLA Monitoring

### Service Level Agreement Definition

```yaml
Patient Service SLA:
  Availability: 99.5% uptime/month
  Latency: p95 < 100ms, p99 < 500ms
  Error Rate: < 1% of requests
  Max Recovery Time: 5 minutes

Quality Measure Service SLA:
  Availability: 99% uptime/month
  Latency: p95 < 500ms, p99 < 2s (CQL evaluation intensive)
  Error Rate: < 1% of requests
  Max Recovery Time: 10 minutes

Care Gap Service SLA:
  Availability: 99% uptime/month
  Latency: p95 < 400ms, p99 < 1s
  Error Rate: < 1% of requests
  Max Recovery Time: 10 minutes
```

### SLA Monitoring Queries

```promql
# Availability (uptime percentage)
(1 - (rate(up{job="patient-service"}==0[30d]) / rate(up{job="patient-service"}[30d]))) * 100

# Error budget consumed
rate(http_server_requests_seconds_count{job="patient-service",status=~"5.."}[30d]) /
rate(http_server_requests_seconds_count{job="patient-service"}[30d])

# Latency SLA breach
histogram_quantile(0.95, http_server_requests_seconds{job="patient-service"}) > 0.1
```

### SLA Dashboard

Display for each service:
- Monthly uptime %
- p95/p99 latency trending
- Error rate % trending
- Alert history
- Incident timeline

---

## On-Call Runbooks

### Runbook Structure

```markdown
# Service X - High Error Rate

## Alert: ServiceHighErrorRate (severity: critical)

### Immediate Actions (0-5 min)
1. Verify alert is real (not false positive)
2. Check recent deployments
3. View error logs in Jaeger/ELK
4. Check database/cache health

### Investigation (5-30 min)
1. Analyze error patterns (which endpoints, which tenants)
2. Compare with historical baselines
3. Check resource usage (CPU, memory, connections)
4. Review recent code changes

### Remediation
- **If recent deployment**: Rollback
- **If resource exhausted**: Scale service
- **If database issue**: Failover/restart
- **If deadlock**: Restart service

### Prevention
- Increase error rate alert sensitivity
- Add capacity tests to CI/CD
- Implement circuit breaker
```

---

## Health Checks and Startup Verification

### Spring Boot Actuator (Built-in Health Checks)

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus,env,loggers
  endpoint:
    health:
      show-details: always
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true
```

### Health Check Endpoints

```
GET /actuator/health/live      # Liveness probe (Is service running?)
GET /actuator/health/ready     # Readiness probe (Is service ready for traffic?)
GET /actuator/health/custom    # Custom health indicators
```

### Kubernetes Probe Configuration

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: patient-service
spec:
  containers:
    - name: patient-service
      livenessProbe:
        httpGet:
          path: /actuator/health/live
          port: 8084
        initialDelaySeconds: 30
        periodSeconds: 10

      readinessProbe:
        httpGet:
          path: /actuator/health/ready
          port: 8084
        initialDelaySeconds: 20
        periodSeconds: 5
```

---

## Best Practices Summary

### ✅ DO

- **Alert on business metrics** (measure evaluations, care gaps)
- **Monitor tail latencies** (p95, p99) not just averages
- **Correlate logs/metrics/traces** when investigating
- **Define SLAs** and actively monitor them
- **Create runbooks** for common alerts
- **Test alerts** to ensure they actually fire
- **Review alerts regularly** to eliminate noise
- **Use distributed tracing** for complex request flows

### ❌ DON'T

- **Alert on everything** (leads to alert fatigue)
- **Ignore low-level alerts** (they often precede critical ones)
- **Use only average metrics** (percentiles matter more)
- **Set static thresholds** (baselines change with load)
- **Forget alert routing** (urgent vs non-urgent)
- **Skip log aggregation** (needed for investigation)

---

## Monitoring Setup Checklist

- [ ] Prometheus scraping all services
- [ ] Grafana dashboards created (System, API, Business)
- [ ] Jaeger tracing enabled in all services
- [ ] Alert rules configured in Prometheus
- [ ] Alertmanager routing configured (Slack, PagerDuty)
- [ ] SLA definitions documented
- [ ] SLA monitoring dashboard created
- [ ] Runbooks written for critical alerts
- [ ] Health checks implemented in all services
- [ ] Logging configured with structured JSON
- [ ] Custom business metrics instrumented

---

## Accessing Monitoring Stack Locally

```bash
# Start all services with monitoring stack
docker compose up -d

# Prometheus metrics
curl http://localhost:9090

# Grafana dashboards
open http://localhost:3001  # admin/admin

# Jaeger distributed tracing
open http://localhost:16686

# Service health checks
curl http://localhost:8084/actuator/health/ready  # Patient service
curl http://localhost:8087/actuator/health/ready  # Quality measure service
```

---

## Related Documentation

- **PERFORMANCE_TESTING.md** - Load testing and baselines
- **INTEGRATION_TESTING.md** - Test health during development
- **CI_CD_GUIDE.md** - Integrating monitoring into pipeline
- **DISTRIBUTED_TRACING_GUIDE.md** - Detailed tracing configuration

---

## Next Steps

1. **Create system health dashboard** (Prometheus up status, resource usage)
2. **Create SLA dashboards** for each service tier
3. **Configure alert routing** (Slack, PagerDuty, email)
4. **Write runbooks** for top 5 alerts
5. **Test alerting** with synthetic alert triggers
6. **Establish on-call rotation** for critical alerts

---

_Last Updated: January 19, 2026_
_Version: 1.0_
