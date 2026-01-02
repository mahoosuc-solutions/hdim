# Event Processing Service - Metrics Quick Reference

## Endpoints

### Local Development
- **Prometheus:** http://localhost:8083/events/actuator/prometheus
- **Health:** http://localhost:8083/events/actuator/health
- **Metrics:** http://localhost:8083/events/actuator/metrics

### Production
- **Prometheus:** https://events.healthdata.io/actuator/prometheus
- **Health:** https://events.healthdata.io/actuator/health

---

## Available Metrics

### DLQ Metrics

#### `dlq.failures.total` (Counter)
Total number of events that failed and were sent to DLQ

**Tags:**
- `topic` - Kafka topic name
- `event_type` - Type of event

**Example Query:**
```promql
# Total DLQ failures
sum(dlq_failures_total)

# Failure rate by topic
rate(dlq_failures_total{topic="patient.health.events"}[5m])

# Failures by event type
sum by (event_type) (dlq_failures_total)
```

#### `dlq.retries.total` (Counter)
Total number of DLQ retry attempts

**Example Query:**
```promql
# Total retries
dlq_retries_total

# Retry rate
rate(dlq_retries_total[5m])
```

#### `dlq.exhausted.total` (Gauge)
Current number of events that exhausted all retry attempts

**Example Query:**
```promql
# Current exhausted events
dlq_exhausted_total

# Alert on exhausted events
dlq_exhausted_total > 10
```

#### `dlq.failed.total` (Gauge)
Current number of failed events in DLQ

**Example Query:**
```promql
# Current failed events
dlq_failed_total

# Alert on high failed count
dlq_failed_total >= 100
```

#### `dlq.retrying.total` (Gauge)
Current number of events being retried

**Example Query:**
```promql
# Current retrying events
dlq_retrying_total
```

### Event Processing Metrics

#### `event.processing.duration` (Timer/Histogram)
Duration of event processing

**Tags:**
- `event_type` - Type of event

**Percentiles:** p50, p95, p99
**SLA Buckets:** 10ms, 50ms, 100ms, 500ms, 1000ms

**Example Query:**
```promql
# P95 processing duration
histogram_quantile(0.95, rate(event_processing_duration_seconds_bucket[5m]))

# Average duration by event type
avg by (event_type) (rate(event_processing_duration_seconds_sum[5m]) / rate(event_processing_duration_seconds_count[5m]))

# Events exceeding 100ms SLA
rate(event_processing_duration_seconds_bucket{le="0.1"}[5m])
```

#### `event.processing.success` (Counter)
Total number of successfully processed events

**Tags:**
- `event_type` - Type of event

**Example Query:**
```promql
# Total successful events
sum(event_processing_success_total)

# Success rate by event type
rate(event_processing_success_total{event_type="CARE_GAP_DETECTED"}[5m])
```

#### `event.processing.failure` (Counter)
Total number of failed event processing attempts

**Tags:**
- `event_type` - Type of event

**Example Query:**
```promql
# Total failed events
sum(event_processing_failure_total)

# Failure rate
rate(event_processing_failure_total[5m])

# Success ratio
sum(rate(event_processing_success_total[5m])) / (sum(rate(event_processing_success_total[5m])) + sum(rate(event_processing_failure_total[5m])))
```

---

## Health Check

### Status Levels

**UP** - System is healthy
- Failed events < 100
- No critical issues

**WARNING** - System needs attention
- Failed events >= 100 and < 500
- OR exhausted events >= 100

**DOWN** - System requires immediate intervention
- Failed events >= 500
- OR unable to check DLQ status

### Health Response Example

```json
{
  "status": "WARNING",
  "components": {
    "dlq": {
      "status": "WARNING",
      "details": {
        "failed": 150,
        "exhausted": 10,
        "retrying": 5,
        "total": 165,
        "warningThreshold": 100,
        "criticalThreshold": 500,
        "message": "High number of failed events in DLQ"
      }
    }
  }
}
```

---

## Grafana Dashboard Panels

### Panel 1: DLQ Overview
```promql
# Queries:
1. Failed Events: dlq_failed_total
2. Exhausted Events: dlq_exhausted_total
3. Retrying Events: dlq_retrying_total
```

### Panel 2: DLQ Failure Rate
```promql
rate(dlq_failures_total[5m])
```

### Panel 3: Event Processing Success Rate
```promql
sum(rate(event_processing_success_total[5m])) /
(sum(rate(event_processing_success_total[5m])) + sum(rate(event_processing_failure_total[5m]))) * 100
```

### Panel 4: Processing Duration Percentiles
```promql
# P50
histogram_quantile(0.50, rate(event_processing_duration_seconds_bucket[5m]))

# P95
histogram_quantile(0.95, rate(event_processing_duration_seconds_bucket[5m]))

# P99
histogram_quantile(0.99, rate(event_processing_duration_seconds_bucket[5m]))
```

### Panel 5: Failures by Event Type
```promql
sum by (event_type) (rate(dlq_failures_total[5m]))
```

### Panel 6: Processing Throughput
```promql
sum(rate(event_processing_success_total[5m]))
```

---

## Alert Rules

### Critical Alerts

#### High DLQ Size
```yaml
- alert: CriticalDLQSize
  expr: dlq_failed_total >= 500
  for: 1m
  labels:
    severity: critical
    team: event-processing
  annotations:
    summary: "DLQ has reached critical threshold"
    description: "DLQ has {{ $value }} failed events (critical threshold: 500)"
    runbook_url: "https://wiki.company.com/runbooks/dlq-critical"
```

#### Service Down
```yaml
- alert: EventProcessingServiceDown
  expr: up{job="event-processing-service"} == 0
  for: 2m
  labels:
    severity: critical
    team: event-processing
  annotations:
    summary: "Event processing service is down"
```

### Warning Alerts

#### High DLQ Failure Rate
```yaml
- alert: HighDLQFailureRate
  expr: rate(dlq_failures_total[5m]) > 10
  for: 5m
  labels:
    severity: warning
    team: event-processing
  annotations:
    summary: "High DLQ failure rate detected"
    description: "DLQ failure rate is {{ $value | humanize }} events/sec"
```

#### High Event Processing Errors
```yaml
- alert: HighEventProcessingErrors
  expr: rate(event_processing_failure_total[5m]) > 5
  for: 5m
  labels:
    severity: warning
    team: event-processing
  annotations:
    summary: "High event processing error rate"
    description: "Error rate is {{ $value | humanize }} events/sec"
```

#### Slow Event Processing
```yaml
- alert: SlowEventProcessing
  expr: histogram_quantile(0.95, rate(event_processing_duration_seconds_bucket[5m])) > 1.0
  for: 10m
  labels:
    severity: warning
    team: event-processing
  annotations:
    summary: "Event processing P95 latency is high"
    description: "P95 latency is {{ $value | humanize }}s (threshold: 1s)"
```

#### Many Exhausted Events
```yaml
- alert: ManyExhaustedEvents
  expr: dlq_exhausted_total >= 50
  for: 5m
  labels:
    severity: warning
    team: event-processing
  annotations:
    summary: "Many events have exhausted retries"
    description: "{{ $value }} events need manual intervention"
```

---

## Common Queries

### Performance Analysis

```promql
# Average processing time by event type
avg by (event_type) (
  rate(event_processing_duration_seconds_sum[5m]) /
  rate(event_processing_duration_seconds_count[5m])
)

# Events processed per second
sum(rate(event_processing_success_total[5m]))

# Error rate percentage
(
  sum(rate(event_processing_failure_total[5m])) /
  (sum(rate(event_processing_success_total[5m])) + sum(rate(event_processing_failure_total[5m])))
) * 100
```

### Capacity Planning

```promql
# Peak processing rate (last 24h)
max_over_time(
  sum(rate(event_processing_success_total[5m]))[24h:]
)

# Total events processed today
sum(increase(event_processing_success_total[24h]))

# DLQ growth rate
deriv(dlq_failed_total[1h])
```

### Troubleshooting

```promql
# Recent DLQ failures by topic
topk(5, sum by (topic) (increase(dlq_failures_total[1h])))

# Events stuck in retry
dlq_retrying_total

# Failed events requiring attention
dlq_failed_total + dlq_exhausted_total
```

---

## Testing Metrics Locally

### 1. Start the Service
```bash
cd backend/modules/services/event-processing-service
./gradlew bootRun
```

### 2. Check Prometheus Endpoint
```bash
curl http://localhost:8083/events/actuator/prometheus
```

### 3. Check Health Endpoint
```bash
curl http://localhost:8083/events/actuator/health | jq
```

### 4. List Available Metrics
```bash
curl http://localhost:8083/events/actuator/metrics | jq
```

### 5. Get Specific Metric
```bash
curl http://localhost:8083/events/actuator/metrics/dlq.failures.total | jq
```

---

## Integration with Code

### Record Event Processing Metrics
```java
@Service
@RequiredArgsConstructor
public class EventHandler {

    private final EventProcessingMetrics metrics;

    public void handleEvent(Event event) {
        Timer.Sample sample = metrics.startTimer();

        try {
            // Process event
            processEvent(event);

            // Record success
            metrics.stopTimer(sample, event.getType());
            metrics.recordSuccess(event.getType());

        } catch (Exception e) {
            // Record failure
            metrics.stopTimer(sample, event.getType());
            metrics.recordFailure(event.getType());

            // Send to DLQ
            dlqService.recordFailure(
                event.getTopic(),
                event.getType(),
                event.getTenantId(),
                event.getPatientId(),
                event,
                e
            );

            throw e;
        }
    }
}
```

### DLQ Metrics Are Automatic
DLQ metrics are automatically recorded by `DeadLetterQueueService`:
- `recordFailure()` → increments `dlq.failures.total`
- `markForRetry()` → increments `dlq.retries.total`
- Gauges update automatically based on database state

---

## Best Practices

1. **Monitor Trends:** Look at rates, not absolute values
2. **Set Appropriate SLOs:** Define acceptable latency and error rates
3. **Alert on Symptoms:** Alert on user-impacting issues, not causes
4. **Use Labels Wisely:** Don't create high-cardinality labels
5. **Regular Review:** Review and adjust thresholds quarterly
6. **Document Runbooks:** Link alerts to actionable runbooks

---

## Support

For issues or questions:
- **Slack:** #event-processing-alerts
- **PagerDuty:** event-processing-oncall
- **Wiki:** https://wiki.company.com/event-processing/metrics
