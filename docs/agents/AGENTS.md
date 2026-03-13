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

## Folder Hygiene (Mandatory For Agents)
- Run preflight before code edits:
  - `git status --short --branch`
  - `npm run hygiene:audit`
- Run postflight before final response/commit:
  - `npm run hygiene:audit`
  - `git status --short --branch`
- Never leave or commit generated files in forbidden paths:
  - `backend/modules/**/bin/main/**`
  - `backend/modules/**/bin/test/**`
  - `backend/platform/**/bin/main/**`
  - `backend/platform/**/bin/test/**`
  - `backend/tools/**/bin/main/**`
  - `backend/tools/**/bin/test/**`
- If forbidden generated dirtiness appears, run:
  - `npm run hygiene:clean`
- Full policy and recovery runbook: `HYGIENE.md`

## Implementation Variants & Operations
- Demo stack: `docker-compose.demo.yml` with seeding scripts; edge gateway on `http://localhost:18080`.
- Observability: `docker compose -f docker-compose.observability.yml up -d` for tracing/metrics/logs.
- External DB: `docker-compose.external-db.yml` + `.env.external-db.example` for shared storage.
- Monolith variant: see `healthdata-platform/` docs for build/runtime wiring differences.
- Tenant-aware scheduling: appointment/task vs encounter schedule sources configurable per tenant in `apps/clinical-portal/src/app/config/scheduling.config.ts`.

## Security & Configuration Tips
- Use `.env` or `portal-env.sample` for local config; do not commit secrets.
- Tenant headers are required for protected APIs; demo allowlists cover `/actuator`, `/fhir/metadata`, and auth endpoints.

## Star Ratings Feature (P1 Complete — March 2026)

The CMS Star Ratings feature lives in two modules:

### Domain Library: `backend/modules/shared/domain/star-ratings/`
- **StarRatingCalculator**: Pure Java scoring engine. 42 CMS measures across 6 domains (STAYING_HEALTHY, MANAGING_CHRONIC_CONDITIONS, MEMBER_EXPERIENCE, COMPLAINTS_AND_PERFORMANCE, DRUG_PLAN, DRUG_SAFETY).
- Supports normal and inverted measures (e.g., PCR — lower is better).
- Cut points per measure in `DEFAULT_CUT_POINTS` map; fallback `{0.50, 0.60, 0.70, 0.80, 0.90}`.
- Weighted domain scoring, overall star rating capped at 5.0, half-star rounding.
- **StarRatingMeasure** enum: 42 constants with code, name, domain, and weight.
- **StarRatingDomain** enum: 6 domains with weight field.
- **Tests**: 12 unit tests in `StarRatingCalculatorTest` covering all scoring paths and edge cases.

### Service: `backend/modules/services/care-gap-event-service/` (port 8111)
- **REST API** (`StarRatingController`): GET `/current`, GET `/trend`, POST `/simulate` — secured with `hasAnyRole('ADMIN','EVALUATOR','ANALYST')`.
- **Persistence**: `star_rating_projections` table (one row per tenant, JPA `@Version` for optimistic locking), `star_rating_snapshots` table (historical weekly/monthly).
- **Kafka**: Single `gap.events` topic for all care gap lifecycle events. `StarsGapEventListener` handles `CareGapDetectedEvent`, `GapClosedEvent`, `PatientQualifiedEvent`, `InterventionRecommendedEvent`, and Map-based fallback.
- **Event flow**: CQRS command side publishes to `gap.events` → listener triggers `StarsProjectionService.recalculateCurrentProjection()`. No synchronous recalculation in command path.
- **Scheduled snapshots**: Weekly (MON 2AM) and Monthly (1st 3AM) via `@Scheduled` in `StarsProjectionService`.
- **Liquibase**: Migrations 0004 (projections table) and 0005 (snapshots table), both with rollback directives.
- **Tests**: 9 listener tests, 32 total service tests.

### Key Design Decisions
- Stars recomputation is event-driven only (via Kafka listener), never synchronous from the command path.
- JPA `@Version` handles optimistic locking; no manual version management.
- The `gap.events` topic is the single source of truth for all gap lifecycle events.
- Orphan topics `gap.detected` and `gap.closed` were removed; only `gap.events` and `intervention.recommended` are declared.
