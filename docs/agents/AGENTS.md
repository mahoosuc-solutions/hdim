# Repository Guidelines

## Project Structure & Module Organization
- `backend/modules/services/*`: Java 21 Spring services (FHIR, quality-measure, care-gap, patient, events, gateways).
- `apps/clinical-portal/`: Angular clinical portal UI and workflows; `apps/clinical-portal-e2e/` for Playwright tests.
- `libs/`: shared TS libraries; `frontend/` for legacy UI assets where applicable.
- `demo/`, `scripts/`, root `*.sh`: demo orchestration, seeding, validation, screenshots, and reset flows.
- `docker-compose*.yml`: demo/dev/staging/prod variants plus observability and external DB profiles.
- `healthdata-platform/`: modular monolith build for alternate deployment topologies.
- `k8s/` and `kubernetes/`: cloud/on-prem manifests and overlays.

## Build, Test, and Development Commands
- `./build-backend-docker-images.sh`: build demo backend images for local compose.
- `docker compose -f docker-compose.demo.yml up -d`: start the demo stack (edge gateway at `http://localhost:18080`).
- `./scripts/seed-all-demo-data.sh`: seed baseline demo content.
- `./scripts/seed-fhir-schedule.sh`: seed schedule data (`SEED_SCHEDULE_MODE=appointment-task|encounter|both`).
- `./validate-system.sh`: validate health checks and core endpoints.
- `./demo-full-system.sh`: end-to-end build, deploy, seed, validate, and demo flow.
- `make up-core` / `make up`: start core or full profiles for development.
- `./backend/gradlew test`: backend unit/integration tests.
- `npm run e2e:clinical-portal` or `npm run e2e:clinical-portal:demo`: portal E2E tests.

## Coding Style & Naming Conventions
- Indentation: 2 spaces (see `.editorconfig`); keep Markdown line length flexible.
- TypeScript/Angular: Nx + ESLint + Prettier; feature modules under `apps/clinical-portal/src/app`.
- Java/Spring: packages under `com.healthdata.*`; tests named `*Test.java`.
- Naming: Angular specs `*.spec.ts`; REST controllers use `*Controller`.

## Testing Guidelines
- Frontend unit tests: Jest; E2E: Playwright (`apps/clinical-portal-e2e/`).
- Backend tests live in `backend/**/src/test`; run via Gradle tasks.
- Demo data changes must be reproducible via `scripts/` or `demo/` (avoid ad-hoc DB edits).

## Commit & Pull Request Guidelines
- Recent history uses short, imperative, sentence-case messages (e.g., “Fix pre-existing test failures”).
- Keep commits focused; include validation steps in PRs (commands + results).
- UI changes should include screenshots and note which demo scripts were rerun.

## Implementation Variants & Operations
- Demo stack: `docker-compose.demo.yml` with seeding scripts; edge gateway on `http://localhost:18080`.
- Observability: `docker compose -f docker-compose.observability.yml up -d` for tracing/metrics/logs.
- External DB: `docker-compose.external-db.yml` + `.env.external-db` for shared storage.
- Monolith variant: see `healthdata-platform/` docs for build/runtime wiring differences.
- Tenant-aware scheduling: appointment/task vs encounter schedule sources configurable per tenant in `apps/clinical-portal/src/app/config/scheduling.config.ts`.

## Security & Configuration Tips
- Use `.env` or `portal-env.sample` for local config; do not commit secrets.
- Tenant headers are required for protected APIs; demo allowlists cover `/actuator`, `/fhir/metadata`, and auth endpoints.
