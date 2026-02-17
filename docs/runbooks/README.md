# Incident Response Runbooks

Operational runbooks for the HealthData Intelligence Platform (HDIM).

## Runbook Index

| Runbook | Severity | Description |
|---------|----------|-------------|
| [Service Down](./service-down.md) | Critical | Backend service not responding |
| [Database Issues](./database-issues.md) | Critical | PostgreSQL connection/performance problems |
| [High Error Rate](./high-error-rate.md) | High | Elevated 5xx errors in services |
| [Performance Degradation](./performance-degradation.md) | High | Slow response times |
| [Authentication Failures](./authentication-failures.md) | High | Auth/JWT issues |
| [Kafka Issues](./kafka-issues.md) | High | Message queue problems |
| [Memory/CPU Alerts](./resource-exhaustion.md) | Medium | Resource utilization alerts |
| [Care Gap Processing](./care-gap-processing.md) | Medium | CQL evaluation issues |
| [MCP Context-Aware Release Gate](./MCP_CONTEXT_AWARE_RELEASE_GATE.md) | High | Policy-aware release gating and tenant isolation decisions |
| [MCP Implementation Reference Spec](./MCP_IMPLEMENTATION_REFERENCE_SPEC.md) | High | Central MCP architecture, tool contracts, guardrails, and testing requirements |
| [CI Branch Protection Checklist](./CI_BRANCH_PROTECTION_CHECKLIST.md) | High | Required branch settings and status checks for merge gating |
| [Session Flow PR Rehearsal Checklist](./SESSION_FLOW_PR_REHEARSAL_CHECKLIST.md) | High | Step-by-step validation for conditional session-flow CI gates |
| [Session Flow Handoff Summary](./SESSION_FLOW_HANDOFF_SUMMARY.md) | High | Consolidated commands, CI gates, and final verification sequence |
| [Session Flow Release Readiness Evidence](./SESSION_FLOW_RELEASE_READINESS_EVIDENCE.md) | High | Fill-in template for final validation evidence and Go/No-Go sign-off |
| [CDS Hooks Care Gap Validation](./CDS_HOOKS_CARE_GAP_VALIDATION.md) | High | Local validation flow for CDS Hooks care-gap integration and deterministic fixture coverage |

## Severity Levels

| Level | Response Time | Escalation |
|-------|---------------|------------|
| **Critical** | Immediate (< 15 min) | On-call engineer + team lead |
| **High** | < 30 min | On-call engineer |
| **Medium** | < 2 hours | Next business day if off-hours |
| **Low** | < 24 hours | Standard ticket queue |

## On-Call Rotation

Contact on-call via PagerDuty or Slack `#critical-alerts` channel.

## General Incident Process

1. **Acknowledge** - Acknowledge alert in PagerDuty/Slack
2. **Assess** - Determine scope and severity
3. **Communicate** - Update status page, notify stakeholders
4. **Mitigate** - Apply runbook or workaround
5. **Resolve** - Implement permanent fix
6. **Postmortem** - Document within 48 hours

## Quick Reference

### Kubernetes Commands
```bash
# Check pod status
kubectl get pods -n healthdata-prod

# Check pod logs
kubectl logs -f deployment/<service-name> -n healthdata-prod

# Restart deployment
kubectl rollout restart deployment/<service-name> -n healthdata-prod

# Scale deployment
kubectl scale deployment/<service-name> --replicas=3 -n healthdata-prod
```

### Docker Commands (Dev/Staging)
```bash
# Check service status
docker compose ps

# View logs
docker compose logs -f <service-name>

# Restart service
docker compose restart <service-name>
```

### Monitoring URLs
- **Grafana**: http://localhost:3000 (prod: grafana.healthdata-platform.io)
- **Prometheus**: http://localhost:9090
- **Alertmanager**: http://localhost:9093
