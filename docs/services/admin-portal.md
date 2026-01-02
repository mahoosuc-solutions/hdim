# Admin Portal

## Purpose
Operator-focused Angular application offering dashboards, service catalog, system health, and API playground tooling.

## Features
- Platform dashboard (metrics, service health, alerts).
- Service catalog with SLAs, owners, compliance tags.
- System health view for dependencies (Postgres, Redis, Kafka).
- API playground with presets, header/query management, live responses.

## Dependencies
- Gateway URLs (configured via `NG_APP_ADMIN_API_URL`).
- Backend API endpoints described in OpenAPI docs.

## Observability
- Served as static assets via Spring Boot or CDN.
- Telemetry instrumented with OpenTelemetry (JS) to Prometheus/Grafana.

## Maintenance
- Built with Nx/Nx Cloud; see `frontend/` for source.
- Update UI documentation in `docs/product/overview.md` as features evolve.
