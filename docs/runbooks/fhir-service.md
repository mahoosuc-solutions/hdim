# Runbook: FHIR Service

## Overview
Spring Boot service handling FHIR R4 REST APIs.

## Dashboards & Alerts
- Grafana: `FHIR / API Performance`
- Alerts:
  - High latency (>500 ms p95)
  - Validation failures > 5/min
  - Kafka publish failures

## On-Call Checklist
1. Check service health endpoint (`/actuator/health`).
2. Inspect Grafana dashboards for latency/spikes.
3. Verify Kafka producer metrics; check DLQ for events.
4. Review logs (ELK/Loki) for error stack traces.

## Common Tasks
### Clear Cache Entry
```bash
redis-cli -h <redis_host> DEL fhir:resource:<resourceId>
```

### Replay Failed Events
Consume from DLQ topic and replay via CLI script (see `scripts/kafka/replay.sh`).

## Recovery Steps
- Restart pod/deployment via `kubectl rollout restart deploy/fhir-service`.
- If DB connection errors persist, failover to replica or contact DBA.
- For schema issues, run Flyway migrations and redeploy.

## Escalation
- Tier 2: Platform Engineering
- Tier 3: Principal Architect

## References
- `docs/services/fhir-service.md`
- `docs/architecture/SYSTEM_BOOT_SEQUENCE.md`
