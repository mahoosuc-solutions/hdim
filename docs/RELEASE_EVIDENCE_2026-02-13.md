# Release Evidence - Docker Weeks 7-8

- Date (UTC): 2026-02-13T21:44:00Z
- Branch: `master`
- Commit at start: `0f04ddf61`

## Summary
- Week 7 security + logging assets: validated and running.
- Week 8 docs/runbooks: already committed to `master`.
- Staging full bring-up: blocked by `ai-sales-agent` Docker build failure.

## Checks Executed

### 1. Compose Config Validation
- `docker compose -f docker-compose.staging.sales-agents.yml config`: PASS
  - Note: warning for deprecated `version` key in staging compose.
- `docker compose -f docker-compose.logging.yml config`: PASS

### 2. Staging Bring-up Attempt
- Command: `docker compose -f docker-compose.staging.sales-agents.yml up -d`
- Result: FAIL (build blocker in `ai-sales-agent`).

Blocking error excerpt:
- `error: package directory 'src/venv' does not exist`
- Fails at `Dockerfile.optimized:24` during `pip install -e .`

Repro command:
- `docker compose -f docker-compose.staging.sales-agents.yml build ai-sales-agent`
- Result: same failure reproduced.

### 3. Existing Staging Service Health (running containers)
- `curl -fsS http://localhost:8095/health`: PASS
  - `{"status":"healthy","service":"live-call-sales-agent","version":"1.0.0"}`
- `curl -fsS http://localhost:8098/actuator/health`: PASS
  - `{"status":"UP", ...}`
- `curl -fsS http://localhost:16686/`: PASS (HTML served)

### 4. Logging Stack Validation
- `docker compose -f docker-compose.logging.yml up -d`: PASS
- `docker compose -f docker-compose.logging.yml ps`: PASS
  - `elasticsearch` up/healthy
  - `kibana` up
  - `filebeat` up
- `curl -fsS http://localhost:9200/_cluster/health`: PASS
  - status: `yellow` (single-node expected)
- `curl -fsS http://localhost:5601/api/status`: PASS
  - overall level: `available`
- `docker logs --tail=30 hdim-filebeat`: PASS
  - active event ingestion and output acknowledgements observed.

## Go/No-Go Assessment
- Security/observability controls: GO
- Full staging deployment readiness: NO-GO until `ai-sales-agent` Docker build is fixed.

## Required Follow-up
1. Fix packaging config in `backend/modules/services/ai-sales-agent` so editable install does not reference `src/venv`.
2. Re-run:
   - `docker compose -f docker-compose.staging.sales-agents.yml build ai-sales-agent`
   - `docker compose -f docker-compose.staging.sales-agents.yml up -d`
3. Re-capture this evidence section with full-stack health green.
