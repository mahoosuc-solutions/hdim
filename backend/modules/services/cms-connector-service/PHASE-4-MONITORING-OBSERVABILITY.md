# Phase 4: Monitoring & Observability

**Status**: Not Started  
**Duration**: 4 weeks  
**Priority**: 🟠 HIGH (can start in parallel with Phase 3)  
**Team**: DevOps, SRE, Backend Engineers  

## Overview

Phase 4 establishes comprehensive monitoring, observability, and alerting infrastructure. A production system is only as good as your visibility into it. This phase enables rapid problem detection and resolution.

## Key Principles

1. **Three Pillars of Observability**
   - **Metrics**: Quantitative measurements (latency, throughput, errors)
   - **Logs**: Detailed event information (application logs, access logs)
   - **Traces**: Request flow through system (distributed tracing)

2. **Observability vs Monitoring**
   - Monitoring: Checking if system is up
   - Observability: Understanding why system is failing

## Learning Outcomes

After Phase 4, you will:
- Collect and visualize metrics in real-time
- Aggregate and search logs across services
- Trace requests through distributed system
- Set up intelligent alerting
- Create on-call runbooks
- Measure SLAs and reliability

---

## Week 1: Logging Infrastructure

### Objectives
- Centralized log aggregation
- Structured logging format
- Log retention and archival
- Log search and analysis

### Option A: ELK Stack (Open Source)

```bash
# Stack: Elasticsearch + Logstash + Kibana
# Cost: Self-hosted (~$500-2000/month infrastructure)
# Effort: 3-4 days

# docker-compose-elk.yml
version: '3.8'
services:
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.10.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
    ports:
      - "9200:9200"
    volumes:
      - elasticsearch-data:/usr/share/elasticsearch/data

  logstash:
    image: docker.elastic.co/logstash/logstash:8.10.0
    volumes:
      - ./logstash.conf:/usr/share/logstash/pipeline/logstash.conf
    ports:
      - "5000:5000"
    depends_on:
      - elasticsearch

  kibana:
    image: docker.elastic.co/kibana/kibana:8.10.0
    ports:
      - "5601:5601"
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    depends_on:
      - elasticsearch

volumes:
  elasticsearch-data:
```

### Option B: Datadog (SaaS)

```bash
# Cost: $300-1000+/month depending on retention
# Effort: 1-2 days
# Pros: Fully managed, better UX
# Cons: Vendor lock-in, expensive at scale
```

### Option C: CloudWatch (AWS-native)

```bash
# Cost: $0.50 per GB ingested
# Effort: 2-3 days
# Pros: Integrated with AWS, simple
# Cons: Limited querying, expensive at scale
```

### Implementation Steps

1. **Update Spring Boot Logging Config** (`logback-spring.xml`)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <springProperty name="LOG_FILE" source="logging.file.name"/>
  <springProperty name="LOG_LEVEL" source="logging.level.root"/>
  
  <!-- JSON logging for structured logs -->
  <appender name="JSON_FILE" class="ch.qos.logback.core.FileAppender">
    <file>${LOG_FILE}</file>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
      <includeContext>true</includeContext>
      <includeMdcData>true</includeMdcData>
      <includeCallerData>true</includeCallerData>
      <customFields>{"service":"cms-connector","environment":"production"}</customFields>
    </encoder>
  </appender>

  <!-- Console appender -->
  <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
  </appender>

  <!-- Async appender for performance -->
  <appender name="ASYNC_JSON" class="ch.qos.logback.classic.AsyncAppender">
    <queueSize>512</queueSize>
    <discardingThreshold>0</discardingThreshold>
    <appender-ref ref="JSON_FILE"/>
  </appender>

  <logger name="com.healthdata.cms" level="${LOG_LEVEL}"/>
  
  <root level="WARN">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="ASYNC_JSON"/>
  </root>
</configuration>
```

2. **Add Logback Dependencies** (`pom.xml`)
```xml
<dependency>
  <groupId>net.logstash.logback</groupId>
  <artifactId>logstash-logback-encoder</artifactId>
  <version>7.4</version>
</dependency>

<dependency>
  <groupId>ch.qos.logback</groupId>
  <artifactId>logback-classic</artifactId>
</dependency>
```

3. **Create Logstash Pipeline Config** (`logstash.conf`)
```
input {
  tcp {
    port => 5000
    codec => json
  }
}

filter {
  if [type] == "java-logs" {
    json {
      source => "message"
    }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "cms-logs-%{+YYYY.MM.dd}"
  }
}
```

### Success Criteria
- [ ] All application logs flowing to centralized system
- [ ] Structured JSON logging enabled
- [ ] Search/filter working in dashboard
- [ ] Log retention policy set (30 days minimum)
- [ ] Log archival to S3 (for long-term storage)

---

## Week 2: Metrics & Dashboards

### Objectives
- Real-time metrics collection
- Visualization dashboards
- Performance baselines
- Custom business metrics

### Technology Stack

**Prometheus** (metrics collection)
- Scrapes metrics from applications
- Time-series database
- Powerful query language (PromQL)

**Grafana** (visualization)
- Dashboards
- Alerting
- Multiple data sources

### Implementation

1. **Add Prometheus Spring Boot Starter** (`pom.xml`)
```xml
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

2. **Prometheus Configuration** (`prometheus.yml`)
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'cms-connector'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 5s

  - job_name: 'postgres'
    static_configs:
      - targets: ['localhost:9187']

  - job_name: 'redis'
    static_configs:
      - targets: ['localhost:9121']

# Alerting rules
rule_files:
  - 'alerts.yml'

alerting:
  alertmanagers:
    - static_configs:
        - targets: ['localhost:9093']
```

3. **Custom Application Metrics**
```java
@Component
public class CmsMetrics {
  private final MeterRegistry meterRegistry;

  public CmsMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public void recordApiCall(String endpoint, long duration) {
    Timer.builder("cms.api.call")
      .tag("endpoint", endpoint)
      .register(meterRegistry)
      .record(duration, TimeUnit.MILLISECONDS);
  }

  public void recordDataSyncSuccess(String sourceSystem) {
    Counter.builder("cms.sync.success")
      .tag("source", sourceSystem)
      .register(meterRegistry)
      .increment();
  }

  public void recordDataSyncFailure(String sourceSystem) {
    Counter.builder("cms.sync.failure")
      .tag("source", sourceSystem)
      .register(meterRegistry)
      .increment();
  }
}
```

4. **Grafana Dashboard JSON** (`grafana-dashboards.json`)
```json
{
  "dashboard": {
    "title": "CMS Connector Service",
    "panels": [
      {
        "title": "Request Rate",
        "targets": [
          {
            "expr": "rate(http_requests_total[5m])"
          }
        ]
      },
      {
        "title": "P95 Latency",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, http_request_duration_seconds)"
          }
        ]
      },
      {
        "title": "Error Rate",
        "targets": [
          {
            "expr": "rate(http_requests_total{status=~\"5..\"}[5m])"
          }
        ]
      },
      {
        "title": "Database Connections",
        "targets": [
          {
            "expr": "db_client_connections_usage{}"
          }
        ]
      }
    ]
  }
}
```

### Success Criteria
- [ ] Prometheus scraping metrics successfully
- [ ] Grafana dashboards created
- [ ] System metrics visible (CPU, memory, disk)
- [ ] Application metrics visible (requests, latency, errors)
- [ ] Database metrics visible (connections, queries)
- [ ] Custom business metrics visible

---

## Week 3: Distributed Tracing

### Objectives
- Request tracing across services
- Performance profiling
- Dependency visualization
- Bottleneck identification

### Technology: Jaeger

1. **Add Jaeger Dependencies** (`pom.xml`)
```xml
<dependency>
  <groupId>io.opentelemetry</groupId>
  <artifactId>opentelemetry-api</artifactId>
</dependency>

<dependency>
  <groupId>io.opentelemetry</groupId>
  <artifactId>opentelemetry-sdk</artifactId>
</dependency>

<dependency>
  <groupId>io.opentelemetry.exporter</groupId>
  <artifactId>opentelemetry-exporter-jaeger</artifactId>
</dependency>

<dependency>
  <groupId>io.opentelemetry.instrumentation</groupId>
  <artifactId>opentelemetry-spring-boot-starter</artifactId>
</dependency>
```

2. **Application Configuration** (`application-prod.yml`)
```yaml
otel:
  sdk:
    disabled: false
  exporter:
    jaeger:
      endpoint: http://jaeger:14250
  metrics:
    enabled: true
  logs:
    enabled: true
  traces:
    exporter: jaeger
    sampling:
      probability: 0.1  # Sample 10% of requests
```

3. **Custom Span Instrumentation**
```java
@Service
public class DataSyncService {
  private final Tracer tracer;

  public DataSyncService(Tracer tracer) {
    this.tracer = tracer;
  }

  public void syncData(String source) {
    Span span = tracer.spanBuilder("data_sync")
      .setAttribute("source", source)
      .startSpan();

    try (Scope scope = span.makeCurrent()) {
      // Sync logic
      span.addEvent("sync_started");
      performSync(source);
      span.addEvent("sync_completed");
    } finally {
      span.end();
    }
  }
}
```

### Success Criteria
- [ ] Jaeger running and receiving traces
- [ ] Requests traced end-to-end
- [ ] Service dependencies visible
- [ ] Latency breakdown visible per service
- [ ] Sampling configured appropriately

---

## Week 4: Alerting & Incident Response

### Objectives
- Intelligent alerting rules
- Incident response procedures
- On-call rotations
- Post-incident reviews

### Alert Rules (`alerts.yml`)
```yaml
groups:
  - name: cms-connector
    interval: 30s
    rules:
      # Application health
      - alert: ApplicationDown
        expr: up{job="cms-connector"} == 0
        for: 2m
        annotations:
          summary: "CMS Connector is down"
          description: "CMS Connector application is not responding"

      # Error rate
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.05
        for: 5m
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value | humanizePercentage }}"

      # Latency
      - alert: HighLatency
        expr: histogram_quantile(0.95, http_request_duration_seconds) > 1
        for: 5m
        annotations:
          summary: "High request latency"
          description: "P95 latency is {{ $value }}s"

      # Database
      - alert: DatabaseConnectionPoolExhausted
        expr: db_client_connections_usage > 0.9
        for: 2m
        annotations:
          summary: "Database connection pool near limit"

      # Cache
      - alert: RedisCacheDown
        expr: up{job="redis"} == 0
        for: 1m
        annotations:
          summary: "Redis cache is down"

      # Disk space
      - alert: DiskSpaceLow
        expr: node_filesystem_avail_bytes / node_filesystem_size_bytes < 0.1
        for: 5m
        annotations:
          summary: "Disk space running low"
          description: "Less than 10% disk space available"
```

### Incident Response Runbooks

**Format**: `incident-response/runbooks/[ALERT_NAME].md`

**Example: `ApplicationDown.md`**
```markdown
# Alert: Application Down

## Severity: Critical
**SEV-1**: Application is completely unavailable

## Detection
- Alert fires when application is down for 2+ minutes
- Monitored via `/actuator/health` endpoint

## Immediate Actions (0-5 min)
1. [ ] Check application logs for errors
2. [ ] Check if container/pod is running
3. [ ] Verify network connectivity to application
4. [ ] Check if database is accessible
5. [ ] Check if dependencies (Redis, etc.) are accessible

## Diagnosis (5-15 min)
1. [ ] Review recent deployment changes
2. [ ] Check resource utilization (CPU, memory)
3. [ ] Review error logs in detail
4. [ ] Check external service status

## Resolution Options

### Option 1: Restart Application
```bash
kubectl rollout restart deployment/cms-connector -n production
# or
docker-compose restart cms-connector
```

### Option 2: Rollback Recent Deployment
```bash
kubectl rollout undo deployment/cms-connector -n production
# or
./scripts/rollback.sh
```

### Option 3: Scale Up Resources
```bash
kubectl scale deployment/cms-connector --replicas=3 -n production
```

## Escalation
If application doesn't recover in 15 minutes:
- [ ] Page on-call engineer
- [ ] Engage database team
- [ ] Check infrastructure status

## Post-Incident
- [ ] Root cause analysis
- [ ] Document findings
- [ ] Create tickets to prevent recurrence
- [ ] Schedule blameless post-mortem
```

### Success Criteria
- [ ] Alerts configured for critical issues
- [ ] Alert routing set up (Slack, email, PagerDuty)
- [ ] Runbooks written for top 10 alerts
- [ ] On-call rotation documented
- [ ] Escalation procedures defined

---

## Deliverables Summary

### Week 1: Logging
- [ ] Centralized log aggregation
- [ ] Structured JSON logging
- [ ] Log search working
- [ ] 30-day retention configured

### Week 2: Metrics
- [ ] Prometheus collecting metrics
- [ ] Grafana dashboards created
- [ ] Custom metrics implemented
- [ ] Performance baselines established

### Week 3: Tracing
- [ ] Jaeger receiving traces
- [ ] End-to-end tracing working
- [ ] Service dependencies visible
- [ ] Latency profiling available

### Week 4: Alerting
- [ ] Alert rules configured
- [ ] Notifications working
- [ ] Runbooks created
- [ ] Incident response procedures defined

---

## Key Files to Create

```
monitoring/
├── prometheus/
│   ├── prometheus.yml
│   ├── alerts.yml
│   └── docker-compose.yml
├── grafana/
│   ├── grafana-dashboards.json
│   └── datasources.yml
├── jaeger/
│   └── docker-compose.yml
├── logstash/
│   └── logstash.conf
├── elk/
│   └── docker-compose.yml
└── docs/
    ├── logging-guide.md
    ├── metrics-guide.md
    ├── tracing-guide.md
    └── alerting-guide.md

incident-response/
├── playbook.md
├── runbooks/
│   ├── ApplicationDown.md
│   ├── HighErrorRate.md
│   ├── HighLatency.md
│   ├── DatabaseDown.md
│   └── CacheDown.md
└── post-incident-template.md
```

---

## Success Metrics

- **MTTD** (Mean Time To Detect): < 5 minutes
- **MTTR** (Mean Time To Resolve): < 30 minutes
- **Alert Accuracy**: > 95% (low false positives)
- **Dashboard Coverage**: 100% of critical metrics
- **Runbook Accuracy**: 100% of procedures tested
- **Incident Response Time**: < 15 minutes to first action

---

## Budget Estimate
- Self-hosted ELK: $500-2000/month infrastructure
- Datadog: $300-1000+/month SaaS
- PagerDuty: $200-500/month for on-call management
- Development effort: 60-80 hours
