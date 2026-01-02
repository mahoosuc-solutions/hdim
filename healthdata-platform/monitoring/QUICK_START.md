# Monitoring Stack - Quick Start Guide

## 🚀 Start Monitoring in 3 Minutes

### 1. Set Environment Variables (30 seconds)

```bash
# Create .env file
cat > .env << EOF
# Grafana
GRAFANA_USER=admin
GRAFANA_PASSWORD=admin

# Elasticsearch
ELASTIC_USER=elastic
ELASTIC_PASSWORD=changeme

# Alerting (optional - can be configured later)
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/YOUR/WEBHOOK
PAGERDUTY_SERVICE_KEY=your-key-here
SMTP_USER=your-email@gmail.com
SMTP_PASSWORD=your-app-password
EOF
```

### 2. Start the Stack (1 minute)

```bash
# Start all monitoring services
docker-compose -f docker-compose.monitoring.yml up -d

# Wait for services to be healthy
sleep 30
```

### 3. Verify Health (30 seconds)

```bash
# Run health check
./monitoring/health-check.sh
```

## 📊 Access Dashboards

| Service | URL | Login |
|---------|-----|-------|
| **Grafana** | http://localhost:3000 | admin / admin |
| **Prometheus** | http://localhost:9090 | None |
| **Kibana** | http://localhost:5601 | elastic / changeme |
| **Alertmanager** | http://localhost:9093 | None |

## 🎯 First Steps After Startup

### View Pre-built Dashboards
1. Open Grafana: http://localhost:3000
2. Login (admin/admin)
3. Go to **Dashboards** → **Browse**
4. Open:
   - **Application Metrics** - Service health, latency, errors
   - **Infrastructure** - CPU, memory, disk, network
   - **Business Metrics** - Quality measures, care gaps, patients

### Check Active Alerts
1. Open Prometheus: http://localhost:9090
2. Click **Alerts** tab
3. View firing and pending alerts

### Search Logs
1. Open Kibana: http://localhost:5601
2. Create index pattern: `healthdata-*`
3. Go to **Discover**
4. Start searching logs

## 📈 Key Metrics to Watch

### Application Health
```
Request Rate:     sum(rate(http_server_requests_seconds_count[5m]))
Error Rate:       sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
Response Time:    histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))
```

### JVM Health
```
Heap Usage:       jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}
GC Pause Time:    rate(jvm_gc_pause_seconds_sum[5m])
Thread Count:     jvm_threads_live_threads
```

### Business Metrics
```
Measures/Hour:    sum(rate(measure_calculations_total[1h]))
Care Gaps/Hour:   sum(rate(care_gaps_detected_total[1h]))
Patients/Hour:    sum(rate(patients_processed_total[1h]))
```

## 🔔 Alert Configuration

### Test Alert (Slack)
```bash
# Fire a test alert
curl -X POST http://localhost:9093/api/v2/alerts \
  -H 'Content-Type: application/json' \
  -d '[{
    "labels": {"alertname": "TestAlert", "severity": "warning"},
    "annotations": {"summary": "Test alert from monitoring stack"}
  }]'
```

### View Alert Routes
```bash
# Check alert routing configuration
curl http://localhost:9093/api/v2/status
```

## 🔍 Common Queries

### Prometheus Queries

**Top 5 Slowest Endpoints:**
```promql
topk(5, histogram_quantile(0.95,
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri)
))
```

**Services with High Error Rate:**
```promql
(sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (job)
/
sum(rate(http_server_requests_seconds_count[5m])) by (job)) > 0.01
```

**Database Connection Pool Usage:**
```promql
hikaricp_connections_active / hikaricp_connections_max
```

### Kibana Queries (KQL)

**All Errors:**
```
level: ERROR
```

**Errors in Last Hour:**
```
level: ERROR AND @timestamp >= now-1h
```

**Slow Requests:**
```
response_time > 1000
```

**Specific Service Logs:**
```
service: "quality-measure-service" AND level: (ERROR OR WARN)
```

## 🛠️ Troubleshooting

### Service Not Starting
```bash
# View logs
docker-compose -f docker-compose.monitoring.yml logs -f [service-name]

# Restart service
docker-compose -f docker-compose.monitoring.yml restart [service-name]
```

### No Metrics in Grafana
```bash
# Check Prometheus targets
curl http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | {job: .labels.job, health: .health}'

# Verify application is exposing metrics
curl http://localhost:8080/actuator/prometheus
```

### No Logs in Kibana
```bash
# Check Elasticsearch indices
curl http://localhost:9200/_cat/indices?v

# Check Logstash processing
docker logs healthdata-logstash | tail -50
```

## 📚 Documentation

- **Complete Guide:** `/monitoring/README.md`
- **Alert Rules:** `/monitoring/prometheus/prometheus-rules.yml`
- **Dashboard Config:** `/monitoring/grafana/provisioning/dashboards/`
- **Implementation Details:** `/MONITORING_IMPLEMENTATION_COMPLETE.md`

## 🔐 Security Notes

### Change Default Passwords
```bash
# Grafana
# Login to Grafana → User Icon → Change Password

# Elasticsearch
docker exec healthdata-elasticsearch \
  /usr/share/elasticsearch/bin/elasticsearch-reset-password -u elastic
```

### Enable HTTPS (Production)
```yaml
# Add to docker-compose.monitoring.yml
grafana:
  environment:
    - GF_SERVER_PROTOCOL=https
    - GF_SERVER_CERT_FILE=/etc/grafana/cert.pem
    - GF_SERVER_CERT_KEY=/etc/grafana/key.pem
```

## 💾 Backup

### Quick Backup
```bash
# Backup all monitoring data
docker-compose -f docker-compose.monitoring.yml stop
tar -czf monitoring-backup-$(date +%Y%m%d).tar.gz \
  /var/lib/docker/volumes/healthdata-platform_prometheus-data \
  /var/lib/docker/volumes/healthdata-platform_grafana-data \
  /var/lib/docker/volumes/healthdata-platform_elasticsearch-data
docker-compose -f docker-compose.monitoring.yml start
```

## 🔄 Stop/Restart

### Stop All Services
```bash
docker-compose -f docker-compose.monitoring.yml down
```

### Stop (Keep Data)
```bash
docker-compose -f docker-compose.monitoring.yml stop
```

### Restart
```bash
docker-compose -f docker-compose.monitoring.yml restart
```

### Reset All Data
```bash
# WARNING: This deletes all monitoring data!
docker-compose -f docker-compose.monitoring.yml down -v
```

## 📞 Support

- **Health Check Issues:** Run `./monitoring/health-check.sh -v`
- **Configuration Issues:** Check `/monitoring/README.md`
- **Alert Issues:** Review `/monitoring/alertmanager/alertmanager.yml`
- **Dashboard Issues:** Check Grafana logs: `docker logs healthdata-grafana`

---

**Ready to monitor!** 🎉

Start here: http://localhost:3000 (Grafana)
