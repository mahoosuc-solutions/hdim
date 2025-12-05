# Phase 6: Production Deployment & Monitoring Plan

**Date:** December 2, 2025
**Status:** Planning & Implementation
**Focus:** Production deployment, monitoring infrastructure, operational readiness

---

## Executive Summary

Phase 6 focuses on preparing the healthdata-in-motion platform for production deployment. This includes setting up comprehensive monitoring infrastructure, establishing operational procedures, and executing the production deployment with validation and rollback procedures in place.

**Key Objectives:**
1. Deploy Prometheus metrics collection
2. Configure Grafana dashboards
3. Implement centralized logging (ELK stack)
4. Set up production alerting rules
5. Deploy Quality-Measure service
6. Create operational runbooks
7. Document deployment procedures
8. Validate production readiness

---

## Phase 6 Architecture Overview

### Monitoring Stack Components

```
┌─────────────────────────────────────────────┐
│         Production Monitoring Stack          │
├─────────────────────────────────────────────┤
│                                             │
│  ┌─────────────┐   ┌──────────────────┐   │
│  │ Prometheus  │   │  Grafana         │   │
│  │ (Metrics)   │─→ │  (Dashboards)    │   │
│  └────┬────────┘   └──────────────────┘   │
│       │                                    │
│  ┌────▼───────────────────────────────┐   │
│  │  Application Metrics Export        │   │
│  │  - JVM metrics                     │   │
│  │  - HTTP endpoints                 │   │
│  │  - Database connection pool       │   │
│  │  - Kafka consumer lag             │   │
│  └────────────────────────────────────┘   │
│                                             │
│  ┌──────────────────────────────────────┐  │
│  │  Elasticsearch / Logstash / Kibana   │  │
│  │  Centralized Logging                │  │
│  │  - Application logs                 │  │
│  │  - Service logs                     │  │
│  │  - Database logs                    │  │
│  │  - Infrastructure logs              │  │
│  └──────────────────────────────────────┘  │
│                                             │
│  ┌──────────────────────────────────────┐  │
│  │  Alerting System                     │  │
│  │  - Alert rules (Prometheus)          │  │
│  │  - Notification channels             │  │
│  │  - On-call routing                   │  │
│  └──────────────────────────────────────┘  │
│                                             │
└─────────────────────────────────────────────┘
```

---

## 1. Prometheus Setup

### 1.1 Prometheus Configuration

**Installation & Configuration:**
```yaml
# prometheus-docker-compose.yml additions
prometheus:
  image: prom/prometheus:latest
  container_name: healthdata-prometheus
  ports:
    - "9090:9090"
  volumes:
    - ./prometheus.yml:/etc/prometheus/prometheus.yml
    - ./alert-rules.yml:/etc/prometheus/alert-rules.yml
    - prometheus-data:/prometheus
  command:
    - '--config.file=/etc/prometheus/prometheus.yml'
    - '--storage.tsdb.path=/prometheus'
    - '--web.console.libraries=/usr/share/prometheus/console_libraries'
    - '--web.console.templates=/usr/share/prometheus/consoles'
  networks:
    - healthdata-network
```

### 1.2 Scrape Configuration

**prometheus.yml:**
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  # Quality-Measure Service
  - job_name: 'quality-measure-service'
    static_configs:
      - targets: ['localhost:8087']
    metrics_path: '/quality-measure/actuator/prometheus'

  # CQL Engine Service
  - job_name: 'cql-engine-service'
    static_configs:
      - targets: ['localhost:8088']
    metrics_path: '/cql/actuator/prometheus'

  # FHIR Service
  - job_name: 'fhir-service'
    static_configs:
      - targets: ['localhost:8089']
    metrics_path: '/fhir/actuator/prometheus'

  # Kafka Metrics (via JMX exporter)
  - job_name: 'kafka'
    static_configs:
      - targets: ['localhost:5556']

  # PostgreSQL (via postgres_exporter)
  - job_name: 'postgresql'
    static_configs:
      - targets: ['localhost:9187']

  # Redis (via redis_exporter)
  - job_name: 'redis'
    static_configs:
      - targets: ['localhost:9121']

alerting:
  alertmanagers:
    - static_configs:
        - targets: ['localhost:9093']

rule_files:
  - '/etc/prometheus/alert-rules.yml'
```

### 1.3 Key Metrics to Monitor

```
JVM Metrics:
  - jvm_memory_used_bytes
  - jvm_threads_live
  - process_cpu_usage
  - process_memory_rss_bytes

HTTP/API Metrics:
  - http_requests_total
  - http_request_duration_seconds
  - http_request_size_bytes
  - http_response_status

Database Metrics:
  - hikaricp_connections
  - hikaricp_connections_idle
  - database_query_duration
  - sql_errors_total

Kafka Metrics:
  - kafka_consumer_lag
  - kafka_messages_in
  - kafka_broker_leader_election_latency
  - kafka_fetch_latency_avg

Business Metrics:
  - care_gaps_created
  - care_gaps_closed
  - health_scores_calculated
  - alerts_triggered
  - notifications_sent
```

---

## 2. Grafana Dashboards

### 2.1 Dashboard Configuration

**Installation:**
```yaml
grafana:
  image: grafana/grafana:latest
  container_name: healthdata-grafana
  ports:
    - "3000:3000"
  environment:
    - GF_SECURITY_ADMIN_PASSWORD=admin
    - GF_INSTALL_PLUGINS=grafana-piechart-panel
  volumes:
    - grafana-data:/var/lib/grafana
    - ./grafana/dashboards:/etc/grafana/provisioning/dashboards
    - ./grafana/datasources:/etc/grafana/provisioning/datasources
  networks:
    - healthdata-network
```

### 2.2 Dashboard Templates

**Dashboard 1: System Overview**
```
Panels:
  - Service Health Status (single stat)
  - Request Rate (graph)
  - Error Rate (graph)
  - Response Time (graph)
  - CPU Usage (gauge)
  - Memory Usage (gauge)
  - Disk I/O (graph)
  - Network I/O (graph)
```

**Dashboard 2: Application Performance**
```
Panels:
  - HTTP Request Rate (graph)
  - HTTP Response Time (histogram)
  - Database Query Count (graph)
  - Database Query Duration (graph)
  - Cache Hit Rate (gauge)
  - Kafka Consumer Lag (table)
  - JVM Heap Usage (gauge)
  - Thread Count (graph)
```

**Dashboard 3: Business Metrics**
```
Panels:
  - Care Gaps Created (counter)
  - Care Gaps Closed (counter)
  - Health Scores Calculated (rate)
  - Clinical Alerts Triggered (rate)
  - Notifications Sent (counter)
  - Care Gap Distribution by Status (pie chart)
  - Health Score Distribution (histogram)
  - Alert Severity Distribution (pie chart)
```

**Dashboard 4: Infrastructure Health**
```
Panels:
  - PostgreSQL Connection Pool (gauge)
  - Redis Memory Usage (gauge)
  - Kafka Broker Status (single stat)
  - Zookeeper Status (single stat)
  - Disk Space Usage (gauge)
  - Database Size Growth (graph)
  - Service Availability (graph)
  - Consumer Group Lag (table)
```

---

## 3. Centralized Logging (ELK Stack)

### 3.1 ELK Stack Setup

```yaml
# ELK Services
elasticsearch:
  image: docker.elastic.co/elasticsearch/elasticsearch:8.0.0
  environment:
    - discovery.type=single-node
    - xpack.security.enabled=false
  ports:
    - "9200:9200"
  volumes:
    - elasticsearch-data:/usr/share/elasticsearch/data

logstash:
  image: docker.elastic.co/logstash/logstash:8.0.0
  volumes:
    - ./logstash.conf:/usr/share/logstash/pipeline/logstash.conf
  ports:
    - "5000:5000/udp"
    - "9600:9600"

kibana:
  image: docker.elastic.co/kibana/kibana:8.0.0
  ports:
    - "5601:5601"
  environment:
    - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
```

### 3.2 Logstash Configuration

```
# logstash.conf
input {
  tcp {
    port => 5000
    codec => json
  }
}

filter {
  if [fields][service] {
    mutate {
      add_field => { "[@metadata][index_prefix]" => "%{[fields][service]}" }
    }
  }

  if [exception] {
    mutate {
      add_tag => [ "error" ]
    }
  }

  date {
    match => [ "timestamp", "ISO8601" ]
    target => "@timestamp"
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "%{[@metadata][index_prefix]}-%{+YYYY.MM.dd}"
  }
}
```

### 3.3 Application Logging Configuration

**Spring Boot Configuration (application-prod.yml):**
```yaml
logging:
  level:
    root: INFO
    com.healthdata: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: /var/log/healthdata/application.log
  logback:
    rollingpolicy:
      max-file-size: 100MB
      max-history: 30

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

---

## 4. Alerting Rules

### 4.1 Alert Rules Configuration

**alert-rules.yml:**
```yaml
groups:
  - name: application_alerts
    interval: 30s
    rules:
      # Service Availability
      - alert: ServiceDown
        expr: up{job="quality-measure-service"} == 0
        for: 1m
        annotations:
          summary: "{{ $labels.job }} is down"

      # High Error Rate
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.05
        for: 5m
        annotations:
          summary: "High error rate detected"

      # High Response Time
      - alert: HighResponseTime
        expr: histogram_quantile(0.95, http_request_duration_seconds) > 1
        for: 5m
        annotations:
          summary: "High response time detected"

      # Database Connection Pool
      - alert: LowDatabaseConnections
        expr: hikaricp_connections_idle < 5
        for: 5m
        annotations:
          summary: "Low database connection availability"

      # Kafka Consumer Lag
      - alert: HighKafkaConsumerLag
        expr: kafka_consumer_lag > 10000
        for: 5m
        annotations:
          summary: "High Kafka consumer lag"

      # Disk Space
      - alert: LowDiskSpace
        expr: (node_filesystem_avail_bytes / node_filesystem_size_bytes) < 0.1
        for: 5m
        annotations:
          summary: "Low disk space available"

      # Memory Usage
      - alert: HighMemoryUsage
        expr: (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) > 0.85
        for: 5m
        annotations:
          summary: "High memory usage"
```

### 4.2 Alert Notification Channels

**Email Configuration:**
```yaml
# AlertManager configuration
global:
  resolve_timeout: 5m

route:
  receiver: 'default'
  group_by: ['alertname', 'cluster']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 12h
  routes:
    - match:
        severity: critical
      receiver: 'critical'
      repeat_interval: 5m

receivers:
  - name: 'default'
    email_configs:
      - to: 'ops-team@healthdata.com'
        from: 'alerts@healthdata.com'
        smarthost: 'smtp.gmail.com:587'
        auth_username: 'alerts@healthdata.com'
        auth_password: 'password'

  - name: 'critical'
    email_configs:
      - to: 'oncall@healthdata.com'
        send_resolved: true
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
        channel: '#critical-alerts'
        title: 'Critical Alert'
        text: '{{ .GroupLabels.alertname }}'
```

---

## 5. Quality-Measure Service Deployment

### 5.1 Deployment Strategy

**Blue-Green Deployment:**
```
Blue Environment (Current):
  - Quality-Measure Service v1.0
  - PostgreSQL v16
  - Redis v7.4.6
  - Kafka cluster

Green Environment (New):
  - Quality-Measure Service v2.0
  - Same data stores (shared DB)
  - Same Kafka cluster

Deployment Flow:
  1. Spin up Green environment
  2. Run smoke tests
  3. Gradual traffic shift (10% → 50% → 100%)
  4. Monitor metrics
  5. Keep Blue as rollback
  6. Delete Blue after 24 hours
```

### 5.2 Deployment Configuration

```yaml
# production-docker-compose.yml
version: '3.8'

services:
  quality-measure-service:
    image: healthdata/quality-measure-service:latest
    container_name: quality-measure-prod
    ports:
      - "8087:8087"
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/quality_db
      - SPRING_DATASOURCE_USERNAME=${DB_USER}
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
      - SPRING_REDIS_HOST=redis
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092
      - MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=health,metrics,prometheus
    depends_on:
      - postgres
      - redis
      - kafka
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8087/quality-measure/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    networks:
      - healthdata-network
    restart: unless-stopped
```

### 5.3 Pre-Deployment Validation Checklist

```
Infrastructure:
  ☐ PostgreSQL version verified (16+)
  ☐ Redis version verified (7.4.6)
  ☐ Kafka topics verified (19 operational)
  ☐ Network connectivity tested
  ☐ Storage capacity verified (>500GB free)
  ☐ Memory available (>8GB)
  ☐ CPU capacity (>4 cores)

Database:
  ☐ Liquibase migrations tested
  ☐ Backup taken
  ☐ Connection pool size verified (20)
  ☐ Query performance validated
  ☐ Indexes created and optimized
  ☐ VACUUM/ANALYZE run

Application:
  ☐ Build artifact created
  ☐ Unit tests passed
  ☐ Integration tests passed
  ☐ Load test results acceptable
  ☐ Security scan passed
  ☐ Configuration validated

Monitoring:
  ☐ Prometheus scraping verified
  ☐ Grafana dashboards ready
  ☐ Alerting rules loaded
  ☐ Log aggregation ready
  ☐ Error tracking configured

Operational:
  ☐ Runbooks created
  ☐ On-call rotation ready
  ☐ Rollback procedure tested
  ☐ Communication plan ready
  ☐ Change log documented
```

---

## 6. Production Validation Tests

### 6.1 Smoke Tests

**Test Suite:**
```bash
# Service Availability
curl -f http://localhost:8087/quality-measure/actuator/health

# API Endpoints
curl -f http://localhost:8087/quality-measure/api/care-gaps

# Database Connectivity
curl -f http://localhost:8087/quality-measure/actuator/health | grep -q '"db":"UP"'

# Kafka Integration
curl -f http://localhost:8087/quality-measure/actuator/health | grep -q '"kafka":"UP"'

# Cache Integration
curl -f http://localhost:8087/quality-measure/actuator/health | grep -q '"redis":"UP"'
```

### 6.2 Integration Tests

```bash
# Care Gap CRUD Operations
# Health Score Calculations
# Notification Event Flow
# Multi-Service Communication
# Database Persistence
# Cache Effectiveness
```

### 6.3 Load Test

```
Test Scenarios:
  - 100 concurrent users
  - 10 requests per second
  - 15-minute duration
  - Monitor: Response times, error rates, resource usage

Expected Results:
  - P95 response time: < 200ms
  - Error rate: < 0.1%
  - CPU usage: < 70%
  - Memory usage: < 70%
  - Database connections: < 15 (of 20)
```

---

## 7. Operational Runbooks

### 7.1 Service Startup Procedure

```
Prerequisites:
  1. Verify PostgreSQL is running
  2. Verify Redis is running
  3. Verify Kafka is running
  4. Verify network connectivity

Steps:
  1. Start Quality-Measure service
  2. Monitor startup logs (should complete in 30-40 seconds)
  3. Verify health endpoint returns 200 OK
  4. Verify all components show UP status
  5. Check Grafana dashboard for baseline metrics
  6. Verify no errors in logs

Rollback:
  1. Stop Quality-Measure service
  2. Revert to previous version
  3. Restart service
  4. Verify health
  5. Investigate issue
```

### 7.2 High CPU Usage Response

```
Symptoms:
  - CPU usage > 80% sustained
  - Response times increasing
  - Queue depth increasing

Diagnosis:
  1. Check which process is using CPU (jps, top)
  2. Review Grafana JVM metrics
  3. Check application logs for errors
  4. Analyze Kafka consumer lag

Resolution Options:
  1. Scale horizontally (add more instances)
  2. Tune GC settings
  3. Optimize database queries
  4. Increase thread pool size
  5. Check for memory leaks
```

### 7.3 Database Connection Pool Exhaustion

```
Symptoms:
  - "Unable to acquire JDBC Connection" errors
  - API responses with 503 status
  - Connections stuck in IDLE state

Resolution:
  1. Monitor HikariCP metrics
  2. Kill idle connections if safe
  3. Restart service to reset pool
  4. Increase pool size (max_pool_size)
  5. Review query timeout settings
  6. Check for connection leaks in code
```

### 7.4 Kafka Consumer Lag Growing

```
Symptoms:
  - Consumer lag increasing over time
  - Notifications delayed
  - Health scores not calculating

Resolution:
  1. Check consumer group status
  2. Verify message processing rate
  3. Review service logs for errors
  4. Check CPU/memory of service
  5. Scale consumer instances if needed
  6. Optimize message processing logic
```

---

## 8. Rollback Procedures

### 8.1 Deployment Rollback

```
Scenario: Production deployment has critical issues

Steps:
  1. Detect issue via alerts or monitoring
  2. Notify ops team and stakeholders
  3. Switch traffic back to Blue environment
  4. Stop Green environment
  5. Investigate root cause
  6. Create fix
  7. Schedule new deployment
  8. Document incident
```

### 8.2 Database Rollback

```
Scenario: Liquibase migration has issues

Steps:
  1. Verify backup exists
  2. Restore from backup
  3. Stop application
  4. Run rollback SQL
  5. Restart application
  6. Verify functionality
```

### 8.3 Configuration Rollback

```
Scenario: Configuration change caused issues

Steps:
  1. Revert configuration files
  2. Restart service
  3. Verify metrics return to baseline
  4. Investigate what changed
  5. Test configuration in staging
  6. Re-apply with corrections
```

---

## 9. Deployment Timeline

### Day 1: Preparation
- [ ] 09:00 - Final code review
- [ ] 10:00 - Build artifact creation
- [ ] 11:00 - Staging validation
- [ ] 14:00 - Team sync (confirm go/no-go)
- [ ] 16:00 - Final backup

### Day 2: Deployment Window (1 hour)
- [ ] 14:00 - Backup current state
- [ ] 14:05 - Spin up Green environment
- [ ] 14:10 - Run smoke tests
- [ ] 14:15 - Shift 10% traffic
- [ ] 14:20 - Monitor for 5 minutes
- [ ] 14:25 - Shift 50% traffic
- [ ] 14:30 - Monitor for 5 minutes
- [ ] 14:35 - Shift 100% traffic
- [ ] 14:45 - Final validation
- [ ] 15:00 - Deployment complete

### Post-Deployment
- [ ] Monitor for 24 hours
- [ ] Review metrics and logs
- [ ] Verify all features working
- [ ] Clean up Blue environment
- [ ] Document lessons learned

---

## 10. Success Criteria

### Deployment Success
- ✅ Service starts successfully
- ✅ Health endpoint returns 200 OK
- ✅ All components show UP status
- ✅ Smoke tests pass
- ✅ Zero critical errors in logs
- ✅ Response times within targets
- ✅ Error rate < 0.1%
- ✅ No consumer lag growth

### Operational Readiness
- ✅ Monitoring dashboards populated
- ✅ Alerting rules firing as expected
- ✅ Logs aggregated and searchable
- ✅ Runbooks tested
- ✅ On-call procedures confirmed
- ✅ Team trained on procedures
- ✅ Rollback tested
- ✅ Communication channels ready

---

## Next Steps

1. **Week 1:**
   - Set up Prometheus and Grafana
   - Configure ELK logging
   - Create alert rules
   - Deploy monitoring infrastructure

2. **Week 2:**
   - Execute deployment validation tests
   - Load test in production environment
   - Train ops team
   - Final readiness review

3. **Week 3:**
   - Execute production deployment
   - Monitor for 24 hours
   - Validate all features
   - Document procedures

---

**Generated:** December 2, 2025
**Status:** Comprehensive deployment plan ready
**Next Phase:** Execution of Phase 6 tasks

🤖 Generated with Claude Code
