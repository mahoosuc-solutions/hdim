# HealthData Platform - Monitoring & Observability

## Overview

Complete production-grade monitoring infrastructure for the HealthData Platform, providing comprehensive observability through metrics, logs, traces, and alerts.

## Architecture

### Components

1. **Prometheus** - Time-series metrics collection and storage
2. **Grafana** - Visualization and dashboards
3. **Alertmanager** - Alert routing and notification management
4. **Elasticsearch** - Log storage and full-text search
5. **Logstash** - Log processing and enrichment pipeline
6. **Kibana** - Log visualization and analysis
7. **Loki** - Log aggregation (lightweight alternative)
8. **Exporters** - Specialized metric collectors (Node, PostgreSQL, Redis, Kafka)

## Quick Start

### 1. Start Monitoring Stack

```bash
# Start all monitoring services
docker-compose -f docker-compose.monitoring.yml up -d

# Check service health
docker-compose -f docker-compose.monitoring.yml ps

# View logs
docker-compose -f docker-compose.monitoring.yml logs -f
```

### 2. Access Dashboards

| Service | URL | Default Credentials |
|---------|-----|---------------------|
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | - |
| Alertmanager | http://localhost:9093 | - |
| Kibana | http://localhost:5601 | elastic / changeme |
| Elasticsearch | http://localhost:9200 | elastic / changeme |

### 3. Configure Environment Variables

Create `.env` file:

```bash
# Grafana
GRAFANA_USER=admin
GRAFANA_PASSWORD=secure-password

# Elasticsearch
ELASTIC_USER=elastic
ELASTIC_PASSWORD=secure-password

# Alerting
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/YOUR/WEBHOOK/URL
PAGERDUTY_SERVICE_KEY=your-pagerduty-key

# Email Alerts
SMTP_USER=your-email@gmail.com
SMTP_PASSWORD=your-app-password
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587

# Database
DB_USER=postgres
DB_PASSWORD=postgres
DB_NAME=healthdata
```

## Metrics Collection

### Application Metrics

Spring Boot applications expose metrics via `/actuator/prometheus`:

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: prometheus,health,info,metrics
  metrics:
    export:
      prometheus:
        enabled: true
```

### Custom Business Metrics

```java
@Component
public class MeasureMetrics {
    private final MeterRegistry registry;

    public void recordCalculation(String measureType) {
        registry.counter("measure_calculations_total",
                         "measure_type", measureType).increment();
    }

    public void recordFailure(String measureType) {
        registry.counter("measure_calculations_failed_total",
                         "measure_type", measureType).increment();
    }
}
```

## Logging Configuration

### Application Logging

Add to your `application.yml`:

```yaml
logging:
  level:
    root: INFO
    com.healthdata: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  logstash:
    host: logstash
    port: 5000
```

### JSON Logging

Copy `monitoring/logback-spring.xml` to `src/main/resources/`:

```bash
cp monitoring/logback-spring.xml src/main/resources/
```

### Log Levels

- **ERROR** - Critical issues requiring immediate attention
- **WARN** - Potential problems or degraded performance
- **INFO** - Normal operational messages
- **DEBUG** - Detailed diagnostic information
- **TRACE** - Very detailed tracing information

## Alert Configuration

### Alert Severity Levels

1. **Critical** - Service down, data loss, security breach
   - Immediate notification via PagerDuty + Slack
   - 5-minute repeat interval

2. **Warning** - Performance degradation, approaching limits
   - Slack notification
   - 1-hour repeat interval

### Alert Routing

Alerts are routed based on:
- Component (application, database, cache, messaging)
- Severity (critical, warning)
- Business domain (quality, caregaps, patients)

### Customizing Alerts

Edit `monitoring/prometheus/prometheus-rules.yml`:

```yaml
- alert: CustomAlert
  expr: your_metric > threshold
  for: 5m
  labels:
    severity: warning
    component: custom
  annotations:
    summary: "Custom alert fired"
    description: "Metric exceeded threshold"
```

## Dashboards

### Pre-configured Dashboards

1. **Application Metrics** (`application-metrics.json`)
   - Request rate and latency
   - Error rates by endpoint
   - JVM metrics (heap, GC, threads)
   - Database connection pool
   - Cache hit rates

2. **Infrastructure** (`infrastructure.json`)
   - Node CPU, memory, disk
   - Network I/O
   - Database performance
   - Redis metrics
   - Kafka metrics

3. **Business Metrics** (`business-metrics.json`)
   - Quality measures calculated
   - Care gaps detected and closed
   - Patient processing rates
   - Compliance rates by measure
   - FHIR resource creation

### Creating Custom Dashboards

1. Access Grafana (http://localhost:3000)
2. Click "+" → "Dashboard"
3. Add panels with PromQL queries
4. Save dashboard
5. Export JSON and commit to repository

## ELK Stack Usage

### Searching Logs in Kibana

1. Access Kibana (http://localhost:5601)
2. Create index pattern: `healthdata-*`
3. Go to Discover
4. Use KQL (Kibana Query Language):

```
# Find all errors
level: ERROR

# Find errors in specific service
level: ERROR AND service: "quality-measure-service"

# Find slow requests
response_time > 1000

# Search by timestamp
@timestamp > "2025-01-01" AND level: ERROR
```

### Log Retention

- Application logs: 30 days
- Audit logs: 365 days
- Metrics: 30 days in Prometheus

### Index Lifecycle Management

Elasticsearch automatically:
- Rotates indices daily
- Compresses old indices
- Deletes indices older than retention period

## Monitoring Best Practices

### 1. Metrics

- Use histograms for latency measurements
- Use counters for event counts
- Use gauges for current values
- Tag metrics with relevant labels

### 2. Logging

- Include correlation IDs for request tracing
- Log at appropriate levels
- Avoid logging sensitive data (PHI/PII)
- Use structured logging (JSON)

### 3. Alerting

- Set meaningful thresholds
- Avoid alert fatigue
- Include runbook links in alerts
- Test alert routing regularly

### 4. Dashboards

- Start with high-level overview
- Drill down to details
- Use consistent color schemes
- Add annotations for deployments

## Performance Tuning

### Prometheus

```yaml
# Adjust scrape interval for high-volume metrics
scrape_interval: 15s  # Default
scrape_timeout: 10s

# Retention
storage:
  tsdb:
    retention:
      time: 30d
      size: 50GB
```

### Elasticsearch

```yaml
# Heap size (50% of available RAM, max 32GB)
ES_JAVA_OPTS: "-Xms2g -Xmx2g"

# Index refresh interval
index.refresh_interval: 30s
```

### Logstash

```yaml
# Pipeline workers
pipeline.workers: 4
pipeline.batch.size: 125
```

## Troubleshooting

### Prometheus Not Scraping Targets

```bash
# Check target health
curl http://localhost:9090/api/v1/targets

# Verify service is exposing metrics
curl http://application:8080/actuator/prometheus

# Check Prometheus logs
docker logs healthdata-prometheus
```

### Elasticsearch Connection Issues

```bash
# Check cluster health
curl http://localhost:9200/_cluster/health

# Check indices
curl http://localhost:9200/_cat/indices?v

# View logs
docker logs healthdata-elasticsearch
```

### Alerts Not Firing

```bash
# Check alert rules
curl http://localhost:9090/api/v1/rules

# Check Alertmanager status
curl http://localhost:9093/api/v2/status

# Test alert routing
amtool config routes test --config.file=alertmanager.yml
```

## Backup and Recovery

### Prometheus Data

```bash
# Backup
tar -czf prometheus-backup-$(date +%Y%m%d).tar.gz /var/lib/docker/volumes/prometheus-data

# Restore
tar -xzf prometheus-backup-20250101.tar.gz -C /var/lib/docker/volumes/prometheus-data
```

### Elasticsearch Snapshots

```bash
# Create snapshot repository
curl -X PUT "localhost:9200/_snapshot/healthdata_backup" -H 'Content-Type: application/json' -d'
{
  "type": "fs",
  "settings": {
    "location": "/usr/share/elasticsearch/backups"
  }
}'

# Create snapshot
curl -X PUT "localhost:9200/_snapshot/healthdata_backup/snapshot_$(date +%Y%m%d)"

# Restore snapshot
curl -X POST "localhost:9200/_snapshot/healthdata_backup/snapshot_20250101/_restore"
```

## Security

### Authentication

- Grafana: Username/password authentication
- Elasticsearch: Basic authentication enabled
- Prometheus: Use reverse proxy for authentication

### Network Security

- All services on isolated network
- Expose only necessary ports
- Use TLS for external access

### Data Protection

- Encrypt data at rest
- Encrypt data in transit
- Regular security audits

## Maintenance

### Regular Tasks

- Review and update alert thresholds
- Clean up old dashboards
- Verify backup integrity
- Update monitoring stack versions
- Review log retention policies

### Health Checks

```bash
# Check all services
docker-compose -f docker-compose.monitoring.yml ps

# Health check script
./monitoring/health-check.sh
```

## Integration with CI/CD

### Deployment Annotations

```bash
# Add deployment annotation to Grafana
curl -X POST http://localhost:3000/api/annotations \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Deployment v1.2.3",
    "tags": ["deployment"],
    "time": '$(date +%s000)'
  }'
```

### Metrics in CI/CD

Monitor deployment success:
- Error rate after deployment
- Response time changes
- Resource utilization

## Support

For issues or questions:
- Check logs: `docker-compose -f docker-compose.monitoring.yml logs`
- Review documentation: `/docs/monitoring`
- Contact DevOps team: devops@healthdata-platform.io

## References

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Elasticsearch Documentation](https://www.elastic.co/guide/)
- [Alertmanager Documentation](https://prometheus.io/docs/alerting/latest/alertmanager/)
