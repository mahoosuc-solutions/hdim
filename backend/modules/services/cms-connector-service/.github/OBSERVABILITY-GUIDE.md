# Phase 5 Week 3: Observability Enhancement Guide

**Status**: ✅ COMPLETE
**Date**: January 1, 2026
**Phase**: Phase 5 Week 3 - CI/CD Integration & Observability Enhancement

---

## Overview

This guide provides comprehensive instructions for implementing distributed tracing, enhanced metrics collection, and alerting for the CMS Connector Service.

---

## 1. Distributed Tracing with OpenTelemetry + Jaeger

### 1.1 Architecture Overview

```
Application
    ↓ (OpenTelemetry instrumentation)
Spring Boot (with OpenTelemetry agent)
    ↓ (trace exports)
Jaeger Collector (127.0.0.1:4318)
    ↓ (stores traces)
Jaeger Backend (Elasticsearch or memory)
    ↓ (queries traces)
Jaeger UI (http://localhost:16686)
```

### 1.2 Maven Dependencies

Add to `pom.xml`:

```xml
<!-- OpenTelemetry BOM -->
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.opentelemetry</groupId>
      <artifactId>opentelemetry-bom</artifactId>
      <version>1.31.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<!-- OpenTelemetry Spring Boot Starter -->
<dependency>
  <groupId>io.opentelemetry.instrumentation</groupId>
  <artifactId>opentelemetry-spring-boot-starter</artifactId>
  <version>1.31.0-alpha</version>
</dependency>

<!-- Jaeger Exporter -->
<dependency>
  <groupId>io.opentelemetry.exporter</groupId>
  <artifactId>opentelemetry-exporter-jaeger</artifactId>
</dependency>
```

### 1.3 Spring Boot Configuration

```yaml
# application-prod.yml
spring:
  application:
    name: cms-connector-service

# OpenTelemetry Configuration
otel:
  exporter:
    otlp:
      endpoint: http://localhost:4318
  sdk:
    trace:
      sampling:
        probability: 0.1  # 10% sampling to reduce overhead
  instrumentation:
    spring-web:
      enabled: true
    jdbc:
      enabled: true
    kafka:
      enabled: true
    logback:
      enabled: true

management:
  tracing:
    sampling:
      probability: 0.1
  endpoints:
    web:
      exposure:
        include: health,metrics,traces
```

### 1.4 Docker Compose for Jaeger

```yaml
# docker-compose.observability.yml
version: '3.8'

services:
  jaeger:
    image: jaegertracing/all-in-one:latest
    ports:
      - "6831:6831/udp"  # Jaeger agent (compact Thrift)
      - "16686:16686"    # Jaeger UI
      - "4318:4318"      # OTLP receiver HTTP
      - "4317:4317"      # OTLP receiver gRPC
    environment:
      - COLLECTOR_OTLP_ENABLED=true
    volumes:
      - jaeger-storage:/badger

  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-storage:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3001:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana-storage:/var/lib/grafana

volumes:
  jaeger-storage:
  prometheus-storage:
  grafana-storage:
```

### 1.5 Accessing Jaeger UI

```
URL: http://localhost:16686

Steps:
1. Select service: cms-connector-service
2. Select operation (default: all)
3. Set time range
4. Click "Find Traces"
5. Click trace ID to view details

Key Metrics:
- Trace ID: Unique identifier for entire request flow
- Span: Individual operation (database call, HTTP request, etc.)
- Duration: How long the operation took
- Tags: Metadata (user ID, tenant ID, error status)
- Logs: Custom log events within a span
```

### 1.6 Custom Traces in Application Code

```java
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.Span;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class ClaimsService {

    @Autowired
    private Tracer tracer;

    public Claims searchClaims(String query, String tenantId) {
        // Get current span
        Span span = tracer.spanBuilder("claims.search")
            .setAttribute("tenant.id", tenantId)
            .setAttribute("query.length", query.length())
            .startSpan();

        try (Scope scope = span.makeCurrent()) {
            // Your business logic
            return performSearch(query);
        } finally {
            span.end();
        }
    }
}
```

### 1.7 Trace Sampling Strategies

| Sampling Rate | Use Case | Impact |
|---|---|---|
| 0.01 (1%) | High-traffic production | Minimal overhead, samples key issues |
| 0.1 (10%) | Standard production | Low overhead, good coverage |
| 0.5 (50%) | Staging/pre-production | Moderate overhead, excellent visibility |
| 1.0 (100%) | Development/testing | High overhead, complete visibility |

**Recommendation**: Use 10% sampling for production, 50% for staging.

---

## 2. Enhanced Metrics Collection

### 2.1 Micrometer Prometheus Integration

Already integrated in Spring Boot, no additional dependencies needed.

### 2.2 Key Metrics to Monitor

#### Application Metrics

```
# Request metrics
http_requests_total{method="GET",status="200"} 1234
http_request_duration_seconds{method="GET",quantile="0.95"} 0.156

# Database metrics
db_pool_active_connections 42
db_pool_max_connections 100
db_query_duration_seconds_bucket{le="0.1"} 890

# Cache metrics
cache_hits_total{cache="patientData"} 2345
cache_misses_total{cache="patientData"} 567

# Business metrics
claims_processed_total 5678
claims_validation_errors_total 12
care_gap_detection_duration_seconds{percentile="p95"} 0.234
```

### 2.3 Prometheus Configuration

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    cluster: 'production'

scrape_configs:
  - job_name: 'cms-connector-service'
    static_configs:
      - targets: ['localhost:8081']
    metrics_path: '/api/v1/actuator/prometheus'
    scrape_interval: 10s
```

### 2.4 Custom Metrics in Application

```java
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;

@Service
public class ClaimsService {

    private final Counter claimsProcessed;
    private final Counter claimsErrors;
    private final Timer searchDuration;

    public ClaimsService(MeterRegistry meterRegistry) {
        this.claimsProcessed = Counter.builder("claims.processed")
            .description("Total claims processed")
            .register(meterRegistry);

        this.claimsErrors = Counter.builder("claims.errors")
            .description("Total claims processing errors")
            .register(meterRegistry);

        this.searchDuration = Timer.builder("claims.search.duration")
            .description("Claims search duration")
            .register(meterRegistry);
    }

    public Claims searchClaims(String query) {
        return searchDuration.recordCallable(() -> {
            try {
                Claims result = performSearch(query);
                claimsProcessed.increment();
                return result;
            } catch (Exception e) {
                claimsErrors.increment();
                throw e;
            }
        });
    }
}
```

### 2.5 Application Metrics Endpoints

```bash
# View all metrics
curl http://localhost:8081/api/v1/actuator/metrics

# View specific metric
curl http://localhost:8081/api/v1/actuator/metrics/http.requests.total

# Export in Prometheus format
curl http://localhost:8081/api/v1/actuator/prometheus
```

---

## 3. Alert Rules Configuration

### 3.1 Prometheus Alert Rules

```yaml
# prometheus-alerts.yml
groups:
  - name: cms-connector-alerts
    interval: 30s
    rules:

      # Error Rate > 1%
      - alert: HighErrorRate
        expr: |
          (rate(http_requests_total{status=~"5.."}[5m]) /
           rate(http_requests_total[5m])) > 0.01
        for: 5m
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value | humanizePercentage }}"

      # Response Time p99 > 500ms
      - alert: HighResponseTime
        expr: |
          histogram_quantile(0.99,
            rate(http_request_duration_seconds_bucket[5m])) > 0.5
        for: 5m
        annotations:
          summary: "High response time p99"
          description: "p99 response time is {{ $value | humanizeDuration }}"

      # Service Unavailable (3 minutes)
      - alert: ServiceDown
        expr: up{job="cms-connector-service"} == 0
        for: 3m
        annotations:
          summary: "CMS Connector Service is down"
          description: "Service has been unavailable for 3 minutes"

      # Circuit Breaker Open
      - alert: CircuitBreakerOpen
        expr: circuit_breaker_state{state="open"} > 0
        for: 1m
        annotations:
          summary: "Circuit breaker is open"
          description: "Circuit breaker {{ $labels.breaker }} is open"

      # Connection Pool Exhaustion
      - alert: ConnectionPoolExhausted
        expr: |
          db_pool_active_connections / db_pool_max_connections > 0.9
        for: 5m
        annotations:
          summary: "Database connection pool near capacity"
          description: "Active connections: {{ $value | humanizePercentage }}"

      # Memory Usage > 85%
      - alert: HighMemoryUsage
        expr: |
          (jvm_memory_used_bytes{area="heap"} /
           jvm_memory_max_bytes{area="heap"}) > 0.85
        for: 5m
        annotations:
          summary: "High JVM heap memory usage"
          description: "Heap usage is {{ $value | humanizePercentage }}"

      # GC Pause Time > 200ms
      - alert: LongGCPause
        expr: |
          increase(jvm_gc_pause_seconds_sum[5m]) /
          increase(jvm_gc_pause_seconds_count[5m]) > 0.2
        for: 2m
        annotations:
          summary: "Long GC pause detected"
          description: "Average GC pause time is {{ $value | humanizeDuration }}"

      # Authentication Failures Spike
      - alert: AuthenticationFailureSpike
        expr: |
          rate(authentication_failures_total[5m]) > 10
        for: 2m
        annotations:
          summary: "Spike in authentication failures"
          description: "{{ $value | humanize }} failures per second"

      # Database Query Slow
      - alert: SlowDatabaseQuery
        expr: |
          histogram_quantile(0.95,
            rate(db_query_duration_seconds_bucket[5m])) > 1.0
        for: 5m
        annotations:
          summary: "Slow database queries detected"
          description: "p95 query time is {{ $value | humanizeDuration }}"

      # Cache Miss Rate High
      - alert: HighCacheMissRate
        expr: |
          (rate(cache_misses_total[5m]) /
           (rate(cache_hits_total[5m]) + rate(cache_misses_total[5m]))) > 0.3
        for: 10m
        annotations:
          summary: "High cache miss rate"
          description: "Cache miss rate is {{ $value | humanizePercentage }}"
```

### 3.2 Grafana Dashboard Setup

**Pre-built Dashboard**:
- Import ID: 1860 (Node Exporter Full)
- Import ID: 12856 (Spring Boot 2.1 Statistics)

**Custom Dashboard - CMS Connector Service**:

```json
{
  "dashboard": {
    "title": "CMS Connector Service - Phase 5",
    "panels": [
      {
        "title": "Request Rate (req/s)",
        "targets": [
          {
            "expr": "rate(http_requests_total[5m])"
          }
        ]
      },
      {
        "title": "Error Rate (%)",
        "targets": [
          {
            "expr": "(rate(http_requests_total{status=~\"5..\"}[5m]) / rate(http_requests_total[5m])) * 100"
          }
        ]
      },
      {
        "title": "Response Time p95 (ms)",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) * 1000"
          }
        ]
      },
      {
        "title": "Active DB Connections",
        "targets": [
          {
            "expr": "db_pool_active_connections"
          }
        ]
      },
      {
        "title": "Circuit Breaker State",
        "targets": [
          {
            "expr": "circuit_breaker_state"
          }
        ]
      },
      {
        "title": "JVM Heap Usage (%)",
        "targets": [
          {
            "expr": "(jvm_memory_used_bytes{area=\"heap\"} / jvm_memory_max_bytes{area=\"heap\"}) * 100"
          }
        ]
      }
    ]
  }
}
```

### 3.3 Alerting Channels

#### Slack Integration

```bash
# Configure Slack webhook in Grafana
1. Go to Grafana > Alerting > Notification channels
2. Add new channel: Type = Slack
3. Webhook URL: https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK
4. Channel: #alerts-cms-connector
```

#### Email Integration

```yaml
# prometheus-alertmanager.yml
global:
  resolve_timeout: 5m
  smtp_smarthost: smtp.gmail.com:587
  smtp_auth_username: alerts@example.com
  smtp_auth_password: ${SMTP_PASSWORD}
  smtp_from: prometheus@example.com

route:
  group_by: ['alertname', 'cluster']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 12h
  receiver: 'ops-team'

receivers:
  - name: 'ops-team'
    email_configs:
      - to: 'ops@example.com'
        headers:
          Subject: 'CMS Connector Alert: {{ .GroupLabels.alertname }}'
```

---

## 4. Log Aggregation

### 4.1 ELK Stack (Elasticsearch, Logstash, Kibana)

**Docker Compose**:

```yaml
elasticsearch:
  image: docker.elastic.co/elasticsearch/elasticsearch:8.0.0
  environment:
    - discovery.type=single-node
    - xpack.security.enabled=false
  ports:
    - "9200:9200"

logstash:
  image: docker.elastic.co/logstash/logstash:8.0.0
  volumes:
    - ./logstash.conf:/usr/share/logstash/pipeline/logstash.conf
  ports:
    - "5000:5000/tcp"

kibana:
  image: docker.elastic.co/kibana/kibana:8.0.0
  ports:
    - "5601:5601"
```

### 4.2 Application Log Configuration

```yaml
# application-prod.yml
logging:
  level:
    root: INFO
    com.healthdata: DEBUG
    org.springframework.security: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %logger{36} - %msg%n"
  file:
    name: logs/cms-connector.log
    max-size: 10MB
    max-history: 30
```

### 4.3 Structured Logging

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.logstash.logback.argument.StructuredArguments;

@Service
public class ClaimsService {
    private static final Logger logger = LoggerFactory.getLogger(ClaimsService.class);

    public Claims searchClaims(String query, String tenantId) {
        logger.info("Searching claims",
            StructuredArguments.kv("tenant_id", tenantId),
            StructuredArguments.kv("query_length", query.length()),
            StructuredArguments.kv("operation", "claims.search"));

        try {
            return performSearch(query);
        } catch (Exception e) {
            logger.error("Claims search failed",
                StructuredArguments.kv("tenant_id", tenantId),
                StructuredArguments.kv("error_type", e.getClass().getSimpleName()),
                e);
            throw e;
        }
    }
}
```

---

## 5. Health Checks & Readiness Probes

### 5.1 Spring Boot Actuator Health Endpoints

```bash
# Basic health
curl http://localhost:8081/api/v1/actuator/health

# Detailed health with component status
curl http://localhost:8081/api/v1/actuator/health?show=when_authorized

# Response:
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "livenessState": { "status": "UP" },
    "ping": { "status": "UP" },
    "readinessState": { "status": "UP" }
  }
}
```

### 5.2 Custom Health Indicator

```java
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class CmsConnectorHealthIndicator implements HealthIndicator {

    @Autowired
    private ClaimsService claimsService;

    @Override
    public Health health() {
        try {
            // Test critical dependency
            claimsService.validateConnectivity();
            return Health.up()
                .withDetail("service", "cms-connector")
                .withDetail("version", "1.0.0")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("reason", e.getMessage())
                .build();
        }
    }
}
```

### 5.3 Kubernetes Probes

```yaml
# deployment.yaml
spec:
  containers:
    - name: cms-connector
      livenessProbe:
        httpGet:
          path: /api/v1/actuator/health/liveness
          port: 8081
        initialDelaySeconds: 30
        periodSeconds: 10

      readinessProbe:
        httpGet:
          path: /api/v1/actuator/health/readiness
          port: 8081
        initialDelaySeconds: 10
        periodSeconds: 5
```

---

## 6. SLA Monitoring

### 6.1 Key SLIs (Service Level Indicators)

| Indicator | Target | Measurement |
|---|---|---|
| Availability | 99.9% | Uptime / Total time |
| Error Rate | < 1% | Failed requests / Total |
| Latency (p95) | < 200ms | Response time percentile |
| Latency (p99) | < 500ms | Response time percentile |

### 6.2 SLO Definition

```
SLO: 99.9% availability over 30 days
Error budget: 0.1% of requests can fail
Latency SLO: p95 < 200ms, p99 < 500ms
```

### 6.3 Error Budget Tracking

```yaml
# Prometheus alerts based on error budget
- alert: ErrorBudgetExceeded
  expr: |
    (1 - (1 - error_budget_target) / 30) <
    (count_over_time(http_requests_total{status=~"5.."}[30d]) /
     count_over_time(http_requests_total[30d]))
  for: 5m
  annotations:
    summary: "Error budget exceeded for month"
```

---

## 7. Deployment Checklist

### Pre-Production Observability Checklist

- [ ] OpenTelemetry instrumentation deployed
- [ ] Jaeger collector running and accessible
- [ ] Prometheus scraping metrics successfully
- [ ] Grafana dashboards created and tested
- [ ] Alert rules loaded into Prometheus
- [ ] Alertmanager configured with notification channels
- [ ] ELK stack (or alternative) running for logs
- [ ] Kubernetes probes configured (liveness/readiness)
- [ ] Custom health indicators working
- [ ] Log aggregation pipeline verified
- [ ] Trace sampling rate set appropriately
- [ ] Alert thresholds reviewed and approved
- [ ] Runbooks created for critical alerts
- [ ] On-call escalation paths defined

---

## 8. Cost Optimization

### 8.1 Sampling Strategies

| Strategy | Cost | Coverage |
|---|---|---|
| No sampling (100%) | High ($$$) | Complete |
| Heavy sampling (1-10%) | Low ($) | Partial |
| Adaptive sampling | Medium ($$) | Smart |

**Recommendation**: Implement adaptive sampling based on error rates.

### 8.2 Metrics Retention

```yaml
prometheus:
  retention:
    days: 30          # Balance cost vs. history
    max_bytes: 50GB   # Prevent disk exhaustion
```

### 8.3 Log Retention

```yaml
# Elasticsearch
index_lifecycle_policy:
  max_age: 30d        # Delete logs older than 30 days
  max_size: 100GB     # Per index
```

---

## 9. Next Steps

1. **Week 1**: Deploy Jaeger and enable OpenTelemetry instrumentation
2. **Week 2**: Set up Prometheus scraping and basic alerts
3. **Week 3**: Create Grafana dashboards and configure notifications
4. **Week 4**: Deploy ELK stack and integrate application logging
5. **Ongoing**: Review metrics weekly, adjust alert thresholds, optimize sampling

---

## Support & Documentation

- **OpenTelemetry**: https://opentelemetry.io/docs/
- **Jaeger**: https://www.jaegertracing.io/docs/
- **Prometheus**: https://prometheus.io/docs/
- **Grafana**: https://grafana.com/docs/grafana/latest/
- **Spring Boot Actuator**: https://spring.io/guides/gs/actuator-service/

---

**Document Version**: 1.0
**Last Updated**: January 1, 2026
**Status**: ✅ Phase 5 Week 3 - Complete
