# Docker Operations Guide

## Daily Operations
1. Confirm core services are healthy.
2. Confirm alertmanager has no unresolved critical alerts.
3. Review overnight SLO compliance.
4. Confirm log ingestion pipeline status.

## Operational Commands
```bash
docker compose -f docker-compose.production.yml ps
docker compose -f docker-compose.production.yml logs --tail=100 ai-sales-agent-1
docker compose -f docker-compose.production.yml logs --tail=100 live-call-sales-agent-1
```

## Weekly Security Review
1. Run hardening validation script.
2. Review latest Trivy findings from CI.
3. Confirm no secret leaks from gitleaks report.
4. Verify HIPAA audit log failure alert has zero incidents.

## Team Training Plan (Week 8)
- Session 1: Deployment and rollback drill.
- Session 2: Incident response and alert triage.
- Session 3: SLO review and customer reporting workflow.

## Incident Severity Model
- `P1`: Service unavailable or compliance breach.
- `P2`: Latency/error SLO breach risk.
- `P3`: Degraded non-critical observability component.

## Escalation
- P1: On-call + security lead + compliance lead immediately.
- P2: On-call + service owner within 15 minutes.
- P3: Service owner in next business window.

## SLO Contract Alignment
Use these documents for customer-facing commitments and verification:
- `docs/PHASE2_PILOT_CONTRACT_SLO_LANGUAGE.md`
- `docs/PHASE2_PILOT_OBSERVABILITY_DASHBOARD.md`

## Pre-Release Go/No-Go
1. Security workflow green.
2. Staging deploy validated.
3. SLO dashboard baseline healthy.
4. Rollback plan rehearsed.
