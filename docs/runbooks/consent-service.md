# Runbook: Consent Service

## Overview
Enforces consent and privacy rules for data access.

## Dashboards & Alerts
- Grafana: `Consent / Decision Latency`
- Alerts:
  - Decision latency > 200 ms
  - Denial ratio spike > 30%
  - Kafka consumer lag on `healthdata.consents`

## On-Call Checklist
1. Hit `/actuator/health` for readiness.
2. Inspect consent decision logs for exceptions.
3. Validate Kafka consumption; look for DLQ messages.
4. License compliance verified (docs/compliance/THIRD_PARTY_NOTICES.md).

## Common Tasks
### Flush Policy Cache
```bash
curl -X POST https://<host>/consent/internal/cache/flush -H "Authorization: Bearer <token>"
```

### Reprocess Consent Event
Use script `scripts/kafka/replay-consent.sh` with event ID.

## Recovery Steps
- Rollout restart deployment.
- Rebuild policy cache by pulling latest records from Postgres.
- Coordinate with compliance if denial ratio persists >1h.

## Escalation
- Privacy Engineering (Tier 2)
- Compliance Officer on-call

## References
- `docs/services/consent-service.md`
- `docs/architecture/SYSTEM_BOOT_SEQUENCE.md`
