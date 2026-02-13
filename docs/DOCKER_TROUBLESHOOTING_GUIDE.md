# Docker Troubleshooting Guide

## 1. Service Will Not Start
```bash
docker compose -f docker-compose.production.yml ps
docker compose -f docker-compose.production.yml logs --tail=200 <service>
```

Common causes:
- Missing env var in `.env.production`
- Invalid image tag
- Port collisions on host

## 2. Health Check Failures
```bash
docker inspect --format='{{json .State.Health}}' <container>
```
Actions:
- Verify dependency services are healthy.
- Confirm service listens on expected port.
- Check TLS/env config mismatch.

## 3. DB/Cache Connectivity Errors
- Verify `POSTGRES_HOST`, `POSTGRES_PORT`, `REDIS_HOST`, `REDIS_PORT`.
- Confirm network membership:
```bash
docker network inspect production-network
```

## 4. High Latency / SLO Risk
- Check Prometheus alerts (`HighLatencyP95`, `CriticalLatencyP99`).
- Review Jaeger traces for slow spans.
- Scale app replicas and verify DB saturation.

## 5. Security Hardening Drift
```bash
./scripts/security/validate-hipaa-docker-security.sh docker-compose.production.yml
```
If failure occurs:
- Re-add `no-new-privileges`
- Re-add `cap_drop: [ALL]`
- Re-add `read_only: true` + tmpfs mounts

## 6. Logging Pipeline Not Ingesting
```bash
docker compose -f docker-compose.logging.yml ps
docker compose -f docker-compose.logging.yml logs --tail=200 filebeat
curl -s http://localhost:9200/_cat/indices?v
```
Actions:
- Verify docker socket mount in Filebeat.
- Validate `monitoring/filebeat.yml` syntax.
- Restart Filebeat after config updates.
