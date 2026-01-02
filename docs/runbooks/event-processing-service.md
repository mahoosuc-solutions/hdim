# Runbook: Event Processing Service

## Overview
Kafka consumer/processor that creates alerts and care gaps.

## Dashboards & Alerts
- Grafana: `Events / Consumer Lag`
- Alerts:
  - Consumer lag > 500 messages
  - DLQ growth > 10 per minute
  - Webhook failure rate > 5%

## On-Call Checklist
1. Check `/actuator/health`.
2. Inspect Kafka consumer lag via Grafana or `kafka-consumer-groups`.
3. Check DLQ topics for recurring patterns.

## Common Tasks
### Replay DLQ
```bash
scripts/kafka/replay-dlq.sh healthdata.events.dlq
```

### Disable Webhook Temporarily
Update config map or feature flag to disable failing integrations.

## Recovery Steps
- Restart deployment: `kubectl rollout restart deploy/event-processing`.
- Scale out consumers if lag persists.
- Coordinate with integration partners to resolve webhook failures.

## Escalation
- Streaming Platform Team (Tier 2)
- Principal Architect (Tier 3)

## References
- `docs/services/event-processing-service.md`
- `docs/architecture/SYSTEM_BOOT_SEQUENCE.md`
