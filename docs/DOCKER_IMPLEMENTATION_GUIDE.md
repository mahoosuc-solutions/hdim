# Docker Implementation Guide (Weeks 1-8)

## Scope
This guide covers the Docker rollout for AI sales agent services across development, staging, production, and operations hardening.

## Compose Files
- `docker-compose.dev.sales-agents.yml`: local development and fast iteration.
- `docker-compose.staging.sales-agents.yml`: production-like validation.
- `docker-compose.production.yml`: HA runtime configuration.
- `docker-compose.logging.yml`: ELK/Filebeat log aggregation add-on.

## Week 7 Security Hardening Checklist
1. Run vulnerability scans in CI (`.github/workflows/build-docker-sales-agent-images.yml`).
2. Validate runtime hardening controls:
   ```bash
   ./scripts/security/validate-hipaa-docker-security.sh docker-compose.production.yml
   ```
3. Validate secret handling:
   - Use env expansion (`${VAR}`), not inline secrets in compose.
   - Mount secret volumes read-only (`:ro`).
4. Run config scan:
   - Trivy config scan via `.github/workflows/security-hardening-validation.yml`.

## Week 8 Operations Deliverables
- Runbook: `docs/DOCKER_OPERATIONS_GUIDE.md`
- Troubleshooting: `docs/DOCKER_TROUBLESHOOTING_GUIDE.md`
- Deployment procedure: `docs/DEPLOYMENT_RUNBOOK.md`
- SLO contract language: `docs/PHASE2_PILOT_CONTRACT_SLO_LANGUAGE.md`
- Observability playbook: `docs/PHASE2_PILOT_OBSERVABILITY_DASHBOARD.md`

## Standard Deployment Flow
1. Build and scan images.
2. Deploy to staging and validate health.
3. Validate SLO dashboard traces.
4. Approve production release.
5. Deploy production compose.
6. Monitor alerts for 60 minutes post-deploy.

## Logging Stack Launch
```bash
docker compose -f docker-compose.logging.yml up -d
```

## Exit Criteria
- CI security gates pass.
- Hardening validator passes.
- Alerting and dashboards operational.
- Runbook handoff completed with on-call team.
