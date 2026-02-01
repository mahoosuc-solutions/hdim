# Runbook: CQL Engine Service

## Overview
Evaluates CQL expressions and HEDIS measures.

## Dashboards & Alerts
- Grafana: `CQL / Evaluation Performance`
- Alerts:
  - Evaluation latency > 750 ms
  - Cache miss rate < 40%
  - Kafka publish failure for metrics topic

## On-Call Checklist
1. Check `/actuator/health`.
2. Inspect measure logs for failing calculations.
3. Validate connectivity to FHIR service (internal ping endpoint).
4. License compliance verified (docs/compliance/THIRD_PARTY_NOTICES.md).

## Common Tasks
### Flush Library Cache
```bash
curl -X POST https://<host>/cql/internal/cache/flush -H "Authorization: Bearer <token>"
```

### Re-run Measure
Use CLI `scripts/cql/run-measure.sh <measureId> <patientId>`.

## Recovery Steps
- Restart pods with `kubectl rollout restart deploy/cql-engine`.
- Ensure Redis is reachable; failover if necessary.
- If measure definitions corrupted, re-run Flyway migrations and redeploy.

## Escalation
- Clinical Analytics Team (Tier 2)
- Chief Medical Information Officer (Tier 3)

## References
- `docs/services/cql-engine-service.md`
- `docs/architecture/SYSTEM_BOOT_SEQUENCE.md`
