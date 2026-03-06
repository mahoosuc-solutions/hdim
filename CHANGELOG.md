# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed
- Gateway now passes through non-2xx responses from upstream services instead of converting them to 500s.

## [0.2.0] - 2026-03-06

### Added - Sprint 2: Hardening
- **Prometheus `/metrics` endpoint** on all 3 MCP Edge sidecars (`prom-client` 15.x)
  - Counters: `mcp_tool_calls_total{tool,role,status}`, `mcp_rate_limit_rejections_total`
  - Histogram: `mcp_tool_duration_seconds{tool}`
  - Gauges: `mcp_active_connections`, `mcp_circuit_breaker_state{service}`
- **API key rotation** — comma-separated `MCP_EDGE_API_KEY` for zero-downtime key rotation
- **Circuit breaker** (`opossum` 8.x) on platform + clinical HTTP clients
  - CLOSED → OPEN → HALF_OPEN state machine with metrics emission
  - Fail-fast fallback (no stale PHI cache)
- **Per-key rate limiting** — SHA-256 of bearer token as rate limit key for independent windows
- **User Management Phase 4** — 4 `UserAutoRegistrationIT` integration tests (patient, care-gap, cql-engine, fhir services)

### Added - Sprint 3: Observability
- **W3C Trace Context propagation** — `traceparent` header parsing, middleware, outbound header injection
- **ECS-compatible structured logging** — `ecs.version`, `service.name`, `trace.id`, `span.id`, `parent.id`
- **Deep health probes** — `GET /health/ready` endpoint, pluggable `checks` array with degraded status
- **TLS termination config** — nginx reverse proxy template (`nginx-tls-proxy.conf`), docker-compose block
- **Connection pooling** — `keep-alive` headers on platform + clinical HTTP clients
- **Correlated request logging** — `withTraceContext` child loggers in mcp-router tool call paths

### Changed
- MCP Edge test count: 1,423 → **1,481** (+58 tests across 4 packages)
- v0.2.0 roadmap marked COMPLETE (3/3 sprints)

## [2.9.2] - 2026-03-06

### Added - Phase 1B API Documentation
- **95 new OpenAPI annotations** across 3 services: Quality Measure (32), CQL Engine (43), Analytics (20)
- Total documented endpoints: **157** (up from 62 in Phase 1A)
- Emergency runbook: `docs/runbooks/EMERGENCY_RUNBOOK.md` — severity classification, hotfix procedures, service recovery, communication templates

### Added - Backlog Closure (v2.9.1)
- **B1-B9 fully resolved:** Kafka port standardization (22 services), OAuth encryption (AES-256-GCM), JWKS mode, Liquibase bypass fix
- 68 event-replay-service unit tests (`df39ae8a5`)
- cqrs-query-service skeleton with 34 passing tests (`b81f89400`)
- Live gateway integration test framework (platform, devops, clinical sidecars)
- MCP Edge clinical sidecar PHI audit + strategy hot-swap (`5fc05d6e7`)

### Changed
- `.env.external-db` → `.env.external-db.example`, `healthdata-platform/.env` → `.env.example` with static placeholders
- 58 intelligence-layer files branched to `feature/intelligence-layer` (unblocking clean master)
- CLAUDE.md updated to v4.3

### Fixed
- Phase 1B API documentation gaps closed (Quality Measure, CQL Engine, Analytics)
- B7q and B9 commit references in KNOWN_ISSUES updated (were "pending commit", now resolved)

## [2.9.0] - 2026-03-05

### Added - MCP Edge Layer v0.1.0
- **3 MCP sidecars** for Claude Desktop/Code integration
  - `mcp-edge-platform` (:3100) — 15 platform tools (health, FHIR metadata, service catalog)
  - `mcp-edge-devops` (:3200) — 15 devops tools (Docker, logs, topology, release gates)
  - `mcp-edge-clinical` (:3300) — 68+ clinical tools across 3 strategies (composite/high-value/full-surface)
- **1,307 MCP Edge tests** with 99.35% statement coverage, 98.12% branch coverage
- RBAC exhaustive matrix: 7 roles x 108 clinical tools (352+ test cases)
- PHI leak detection and cross-sidecar isolation proofs
- Demo mode with per-tool synthetic fixtures
- stdio bridges for Claude Desktop/Code integration
- Structured pino logging with PHI scrubbing
- ESLint no-console enforcement across all 4 packages
- CI workflow with coverage thresholds and AJV validator caching
- Compliance mapping: HIPAA, SOC2, OWASP, NIST, CIS

### Added - v2.8.0 System Remediation (Tiers 1-4)
- **Tier 1 (Security):** Untrack `.env.dev`/`.pid`/`ZoneId`, fix destructive `ddl-auto`, externalize passwords, revert fhir-service demo data
- **Tier 2 (Landing Page):** metadataBase fix, skip targets, constants extraction, ARIA improvements, CookieConsent cleanup, shared types
- **Tier 3 (CI/CD):** 21 workflows normalized, 3 landing page workflows consolidated to 1, CODEOWNERS/PR template, pinned GitHub Actions, Trivy bump
- **Tier 3+ (Quick Wins):** AI audit severity filtering, patient event merge chain depth parameterized
- **Tier 4 (Docs/Hygiene):** 85 root MD files + 7 directories (362 total files) reorganized into `docs/` subdirectories

### Added - Landing Page v3
- Persona-first design with segment pages (payers, ACOs, health systems)
- Product screenshots across all pages
- SEO metadata, canonical URLs, preconnect hints
- GA4 analytics, lazy-loaded reCAPTCHA
- E2E tests for all page structures
- Deployed to Vercel

### Added - Wave-1 Revenue Infrastructure
- Payer workflows: price transparency estimate and publish APIs
- ADT payload handling and clearinghouse retry backoff
- Remittance reconciliation with negative-path validations
- State transition guards and HTTP adapter contract tests
- Performance budgets (p95) in Wave-1 assurance runner
- 360 platform assurance checklist with compliance traceability

### Added - Backend Quality
- `audit-query-service`: 10 new test files, 50 tests (commit `0f116b405`)
- `gateway-admin-service`: 8 new test files, 69 tests (commit `af397025d`)
- Cascading startup failure fixes: notification-service, prior-auth-service
- CVE remediation wave 1 with dependency overrides and NVD automation

### Changed
- Backend test suite: 259 → 709+ unit tests passing (excludes 1,307 MCP Edge tests)
- MCP Edge packages: 4 (common + 3 sidecars)
- Root markdown files reduced from 90+ to 5 (README, CLAUDE, CONTRIBUTING, CHANGELOG, VERSIONS)
- Landing page domain: healthdatainmotion.com (canonical)
- Contact emails: info@mahoosuc.solutions, sales@mahoosuc.solutions

### Fixed - Phase 21: Complete Test Stabilization (100% Pass Rate)
- Fixed 24 quality-measure-service test failures (100% pass rate: 1,577/1,577)
- RBAC auth tests: added missing X-Auth-Validated header (4 tests)
- PopulationBatch execution: database-backed job tracking, async fix (9 tests)
- PopulationCalculation: removed dangerous dummy patient fallback (4 tests)
- Controller integration: corrected 403 -> 404 for tenant isolation (2 tests)
- E2E integration: FHIR mocking via @MockBean RestTemplate (5 tests)
- All compilation errors resolved (6 fixes)

### Security
- Credentials externalized from tracked files
- `ddl-auto: create` eliminated (was in notification-service)
- GitHub Actions pinned to SHA
- Trivy scanner updated
- CODEOWNERS enforced for sensitive paths

## [2.8.1] - 2026-03-03

### Changed
- Reorganized 85 root markdown files and 7 directories (362 files total) into `docs/` subdirectories
- Root now contains only 5 markdown files: README, CLAUDE, CONTRIBUTING, CHANGELOG, VERSIONS

## [2.8.0] - 2026-03-03

### Added
- System remediation Tiers 1-4 (security, landing page, CI/CD, docs)
- AI audit severity filtering in SSE stream
- Patient event merge chain depth parameterization

### Security
- Untracked `.env.dev`, `.pid`, `ZoneId` files
- Fixed destructive `ddl-auto` configuration
- Externalized hardcoded passwords
- Reverted fhir-service demo data exposure

## [2.7.2-rc1] - 2026-02-17

### Added
- Compliance evidence gate workflow
- 360 platform assurance checklist and rubric
- Investor stability report
- CVE remediation wave 1

### Fixed
- Gateway clinical entity migration validation scoped to module entities
- Testcontainers suites gated without Docker
- Analytics test stabilization

### Added - Phase 3: Database Performance & Distributed Tracing Standardization
- **HikariCP Connection Pool Standardization (34/34 services)**
  - Production-ready configuration pattern applied to all microservices
  - Traffic tier classification: HIGH (50 connections), MEDIUM (20), LOW (10)
  - Proactive health checks with 4-minute keepalive intervals
  - Connection leak detection with 60-second threshold
  - Fast-fail connection acquisition (20-second timeout)
  - 6x safety margin for max-lifetime (30 min) vs idle-timeout (5 min)
- **Kafka Distributed Tracing (19/19 Kafka-enabled services)**
  - W3C Trace Context propagation through Kafka messages
  - KafkaProducerTraceInterceptor for trace context injection
  - KafkaConsumerTraceInterceptor for trace context extraction
  - 100% coverage across all Kafka-enabled services:
    - Core: quality-measure, fhir, care-gap, patient
    - Events: event-router, event-processing, notification
    - AI: agent-runtime, ai-assistant
    - Workflows: payer-workflows, migration-workflow
    - Integration: cdr-processor, sales-automation
    - Analytics: analytics-service (+ 6 more)

### Added - Phase 4: Distributed Tracing Infrastructure Implementation
- **OpenTelemetry Tracing Infrastructure**
  - TracingAutoConfiguration: OpenTelemetry SDK setup with OTLP export
  - RestTemplateTraceInterceptor: Auto-enabled HTTP trace propagation
  - FeignTraceInterceptor: Auto-enabled Feign client trace propagation
  - Zero-configuration trace propagation for all 34 services
  - Shared tracing module for automatic integration
- **Environment-Specific Sampling Configurations**
  - Development: 100% sampling (full debugging visibility)
  - Staging: 50% sampling (balanced visibility/performance)
  - Production: 10% sampling (cost-effective monitoring)
  - Applied to 4 core services: quality-measure, fhir, cql-engine, gateway
- **Comprehensive Documentation** (1,511 lines total)
  - `DISTRIBUTED_TRACING_GUIDE.md`: Complete OpenTelemetry architecture (775 lines)
  - `PHASE3_COMPLETION_REPORT.md`: HikariCP standardization report (234 lines)
  - `PHASE4_COMPLETION_REPORT.md`: Tracing infrastructure report (502 lines)
  - Updated CLAUDE.md with distributed tracing patterns and examples
  - Custom span creation patterns and best practices
  - Jaeger integration guide
  - Troubleshooting and debugging procedures

### Added - Quality Measure Testing
- **Comprehensive Test Suite for Measure Assignment & Override Features**
  - 6 test files with 3,371 lines of test code
  - ~85 test methods across full stack (service, repository, controller)
  - Service Layer Tests (2 files):
    - MeasureAssignmentServiceTest: 11 methods, 506 lines
    - MeasureOverrideServiceTest: 13 methods, 585 lines
  - Repository Layer Tests (2 files):
    - PatientMeasureAssignmentRepositoryTest: 412 lines (custom JPA queries)
    - PatientMeasureOverrideRepositoryTest: 398 lines (override lookups)
  - Controller Layer Tests (2 files):
    - MeasureAssignmentControllerIntegrationTest: 735 lines (E2E tests)
    - MeasureOverrideControllerIntegrationTest: 735 lines (E2E tests)
  - Coverage includes: business logic validation, multi-tenant isolation, RBAC enforcement, edge cases
- **Test Stabilization - 100% Pass Rate Achievement**
  - Systematically fixed 120 test failures (7.2% → 0% failure rate)
  - Root cause: Spring slice testing annotations incompatible with authentication config
  - Solution: Consistent `@SpringBootTest` + authentication exclusion pattern
  - 5 commits: 1e096b2c, 879fcc61, de9a0b31, 5429926f, 8728a286
  - All 1,580 tests now passing (266 new + 1,314 existing)

### Fixed - Phase 3: Critical Connection Pool Bugs
- **agent-builder-service Connection Pool**
  - Issue: max-lifetime 1200000ms (20 min) insufficient for idle-timeout 300000ms (5 min)
  - Impact: Only 4x safety margin (minimum 6x required), oversized pool (30 for MEDIUM tier)
  - Fix: max-lifetime 1200000 → 1800000ms (30 min), pool size 30 → 20
- **demo-seeding-service Connection Pool**
  - Issue: max-lifetime 600000ms (10 min) insufficient for idle-timeout 300000ms (5 min)
  - Impact: Only 2x safety margin, risk of stale connections
  - Fix: max-lifetime 600000 → 1800000ms (30 min, 6x safety margin)
- **notification-service Connection Pool**
  - Issue: max-lifetime = idle-timeout (NO safety margin)
  - Impact: Stale connections never recycled, pool exhaustion under load
  - Fix: max-lifetime set to 1800000ms (6x idle-timeout)
- **analytics-service Build Failure**
  - Issue: hypersistence-utils-hibernate-63 dependency commented out
  - Impact: 16 compilation errors (JsonBinaryType not found)
  - Fix: Uncommented dependency, build successful

### Changed - Phase 3: Configuration Standardization
- **HikariCP Configuration Pattern (34 services)**
  - connection-timeout: 20000ms (fail fast on connection acquisition)
  - idle-timeout: 300000ms (5 min, matches Docker/PostgreSQL TCP timeout)
  - max-lifetime: 1800000ms (30 min, 6x safety margin for stale connections)
  - keepalive-time: 240000ms (proactive health checks every 4 minutes)
  - leak-detection-threshold: 60000ms (detect and log connection leaks)
  - validation-timeout: 5000ms (fast validation of dead connections)
- **Kafka Tracing Configuration (19 services)**
  - Added trace interceptors to all Kafka-enabled services
  - W3C Trace Context header propagation (traceparent, tracestate)
  - End-to-end request tracing across service boundaries

### Performance - Phase 3 & 4 Impact
- **Connection Pool Reliability**
  - Services with incomplete HikariCP configs: 19/34 (56%) → 0/34 (0%)
  - Services at risk of pool exhaustion: 3 → 0
  - Services with missing timeout configurations: 19 → 0
  - Production-ready connection pool configs: 56% → 100%
- **Distributed Tracing Coverage**
  - HTTP trace propagation: 100% (34/34 services, auto-enabled)
  - Kafka trace propagation: 100% (19/19 Kafka services)
  - Environment-optimized sampling: 4 core services configured
  - Zero-configuration trace propagation for new services
- **Build Success Rate**
  - Before Phase 3: 32/33 builds successful (97%)
  - After Phase 3: 34/34 builds successful (100%)
  - Critical bugs fixed: 3 connection pool issues + 1 dependency issue

### Documentation - Phase 3 & 4
- **Developer Guides**
  - Complete OpenTelemetry architecture and configuration guide
  - HikariCP tuning guide and traffic tier classification
  - Kafka trace propagation implementation patterns
  - Custom span creation examples
  - Troubleshooting procedures for common tracing issues
- **Completion Reports**
  - Phase 3: Database performance standardization metrics
  - Phase 4: Tracing infrastructure implementation summary
  - 100% trace propagation coverage verification
- **CLAUDE.md Updates**
  - Added "Distributed Tracing" section (version 1.5)
  - Automatic trace propagation reference
  - Kafka tracing configuration examples
  - Custom span patterns and best practices

### MCP Edge Layer (v0.1.0) — 2026-03-04

- **3 sidecars:** platform (3100), devops (3200), clinical (3300)
- **3 clinical strategies:** composite (25 tools), high-value (15 tools), full-surface (68 tools)
- **123 total MCP tools** across all sidecars
- **1,273 tests** with 99.35% statement coverage
- RBAC: 7 roles x 108 clinical tools exhaustive matrix (352+ test cases)
- PHI leak detection, cross-sidecar isolation, MCP protocol contract tests
- Demo mode with per-tool fixtures for all 108 tools
- stdio bridges for Claude Desktop/Code integration
- Structured pino logging with PHI scrubbing
- ESLint no-console enforcement

### Breaking Changes
- **HikariCP Configuration Required**
  - All services now require production-ready HikariCP configuration
  - Services must define traffic tier (HIGH/MEDIUM/LOW) for pool sizing
  - Missing configurations will cause service startup warnings/failures
- **OpenTelemetry Dependency Required**
  - All services now include OpenTelemetry SDK via shared tracing module
  - OTLP endpoint configuration required (defaults to Jaeger at http://jaeger:4318/v1/traces)
  - Services will start without OTLP endpoint but traces will not be exported

## [1.2.0] - 2026-01-25

### Added
- **Patient Measure Assignment Management**
  - Manual patient-to-measure assignments with effective date ranges
  - Automatic assignment based on eligibility criteria (JSONB stored)
  - Assignment lifecycle management (create, update, deactivate)
  - Multi-tenant isolation for all assignment operations
  - Cache invalidation on assignment changes
- **Clinical Measure Override Management**
  - Patient-specific measure parameter overrides with approval workflow
  - Clinical justification requirement (HIPAA compliance)
  - Approval workflow for override requests (pending → approved/rejected)
  - Periodic review scheduling and expiration tracking
  - Multi-level override resolution (patient > profile > base measure)
  - Override conflict resolution and validation
  - Complete audit trail for all override modifications
- **Measure Configuration Profiles**
  - Reusable measure configuration templates
  - Population-specific criteria definitions
  - Priority-based profile resolution for patient assignments
  - Profile versioning and change tracking
- **Measure Audit and History**
  - Measure execution history tracking
  - Measure modification audit trail
  - Patient measure eligibility caching with TTL-based invalidation
- **OpenTelemetry Distributed Tracing**
  - Complete OTLP HTTP configuration for 11 Java microservices
  - Jaeger integration for trace visualization and analysis
  - IPv4 stack preference for Docker networking (fixes IPv6 issues)
  - Consistent OTLP endpoint configuration: `http://jaeger:4318/v1/traces`
  - Protocol standardization: `http/protobuf` across all services
  - W3C Trace Context and B3 propagation support
- **New REST API Endpoints** (13 endpoints total)
  - Measure Assignment API: 4 endpoints (get, create, delete, update dates)
  - Measure Override API: 8 endpoints (get, create, approve, review, delete, pending, due-for-review, resolve)
  - Full OpenAPI 3.0 specifications available
- **Database Tables** (7 new tables with Liquibase migrations)
  - `patient_measure_assignments` - Assignment tracking with effective dates
  - `patient_measure_overrides` - Clinical parameter overrides
  - `measure_config_profiles` - Reusable configuration templates
  - `patient_profile_assignments` - Patient-to-profile mappings
  - `measure_execution_history` - Execution audit trail
  - `measure_modification_audit` - Override change tracking
  - `patient_measure_eligibility_cache` - Performance optimization cache
- **Comprehensive Test Suite**
  - 132 new tests for measure assignment and override features
  - Service layer tests (54 tests)
  - Controller integration tests (44 tests)
  - Repository tests (34 tests)
  - Entity-migration validation tests
- **OpenAPI Documentation**
  - Automated OpenAPI specification generation script
  - Comprehensive API documentation for 6 core services
  - Postman/Insomnia import support

### Changed
- **notification-service Configuration (CRITICAL)**
  - Changed `ddl-auto` from `create` to `validate` (prevents data loss on restart)
  - Enabled Liquibase for schema management (`SPRING_LIQUIBASE_ENABLED: true`)
  - Reduced HikariCP `maxLifetime` from 30 minutes to 5 minutes (prevents stale connections)
  - Added HikariCP `keepaliveTime: 30000` (30 seconds) for connection health checks
  - Changed service port from 8089 to 8107 (resolved port conflict)
- **All Java Services Docker Configuration**
  - Added `_JAVA_OPTIONS: "-Djava.net.preferIPv4Stack=true"` to all 11 services
  - Standardized OTLP environment variables across services:
    - `OTEL_EXPORTER_OTLP_ENDPOINT`
    - `OTEL_EXPORTER_OTLP_PROTOCOL`
    - `OTEL_SERVICE_NAME`
- **Docker Compose Architecture**
  - Added Jaeger all-in-one service for distributed tracing
  - Exposed ports: 16686 (Jaeger UI), 4318 (OTLP HTTP), 14268 (Jaeger collector)
  - Added health checks for Jaeger service
- **Database Schema Management**
  - All measure-related tables now use UUIDs for primary keys
  - Added comprehensive indexing (tenant_id, patient_id, dates, JSONB)
  - GIN indexes for JSONB eligibility criteria queries
  - Database triggers for auto-calculation (assignment duration_ms)

### Fixed
- **cql-engine-service Startup Failure**
  - Removed invalid `org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration` exclusion
  - Service can now start successfully without autoconfigure errors
- **notification-service Data Loss Risk**
  - Fixed critical DDL auto configuration (was `create`, now `validate`)
  - Prevented table drops on service restart
  - Migrated schema management to Liquibase
- **notification-service Connection Pool Exhaustion**
  - Fixed HikariCP connection pool configuration
  - Reduced maxLifetime to prevent stale connections
  - Added keepalive time for connection health validation
  - Added leak detection threshold for debugging
- **notification-service Port Mismatch**
  - Changed port from 8089 to 8107 across all configuration files
  - Fixed health check endpoints and service discovery
- **IPv6 Connection Failures in Docker**
  - Added IPv4 stack preference flag to all Java services
  - Resolved connection refused errors in Docker networking
- **Deprecated Notification Methods**
  - Identified usage of deprecated `sendNotification()` methods in ClinicalAlertEventConsumer
  - Documented for future API migration (planned for v1.2.1)
- **Unchecked Type Casts**
  - Documented unchecked casts in RiskAssessmentEventConsumer
  - Added to known issues for refactoring (planned for v1.3.0)

### Security
- **HIPAA Compliance Enhancements**
  - Clinical justification requirement enforced for all patient overrides
  - Validation at service layer: blank/null justification rejected
  - Complete audit trail for all measure modifications
  - Override modification history tracked with timestamps and user attribution
  - Multi-tenant data isolation validation at all layers
  - PHI access tracking via distributed tracing (OTLP)
- **Role-Based Access Control (RBAC)**
  - Measure assignments: EVALUATOR, ADMIN, SUPER_ADMIN roles
  - Override approval: ADMIN role required (enforced via `@PreAuthorize`)
  - Tenant isolation: X-Tenant-ID header validation on all endpoints
  - Gateway trust authentication architecture (no JWT re-validation in services)

### Performance
- **Database Query Optimization**
  - Added composite indexes for common query patterns
  - GIN indexes for JSONB eligibility criteria (fast containment queries)
  - Tenant-scoped indexes for multi-tenant isolation
  - Date range indexes for effective date queries
- **Eligibility Caching**
  - Patient measure eligibility cache with 24-hour TTL
  - Cache invalidation on assignment/override changes
  - Redis-backed cache for distributed environments
- **HikariCP Connection Pool Optimization**
  - Increased maximum-pool-size for high-throughput services
  - Optimized connection timeout and lifecycle settings
  - Leak detection for troubleshooting connection issues

### Database Migrations
- **Liquibase Changesets 0034-0040** (7 migrations)
  - 0034: Create patient_measure_assignments table
  - 0035: Create patient_measure_overrides table
  - 0036: Create measure_config_profiles table
  - 0037: Create patient_profile_assignments table
  - 0038: Create measure_execution_history table
  - 0039: Create measure_modification_audit table
  - 0040: Create patient_measure_eligibility_cache table
- **Rollback Coverage**: 100% rollback SQL for all changesets

### Breaking Changes
- **Jaeger Container Required**
  - All 11 Java services now require Jaeger for OTLP trace export
  - Update `docker-compose.yml` to include Jaeger service
  - Services will start without Jaeger but traces will not be exported
  - Workaround: Disable tracing with `OTEL_TRACES_EXPORTER=none`
- **Environment Variables Added** (required for all Java services)
  - `OTEL_EXPORTER_OTLP_ENDPOINT`: Jaeger OTLP endpoint
  - `OTEL_EXPORTER_OTLP_PROTOCOL`: Protocol specification (http/protobuf)
  - `OTEL_SERVICE_NAME`: Service identifier for traces
  - `_JAVA_OPTIONS`: JVM options including IPv4 preference
- **notification-service Configuration Change**
  - DDL auto changed from `create` to `validate` (REQUIRED)
  - Liquibase must be enabled: `SPRING_LIQUIBASE_ENABLED=true`
  - Database schema must be managed via Liquibase migrations
  - **ACTION REQUIRED**: Backup database before upgrading
- **Port Change**
  - notification-service port changed from 8089 to 8107
  - Update any clients, monitoring, or load balancers using port 8089

### Documentation
- **Release Documentation** (comprehensive v1.2.0 documentation)
  - `RELEASE_NOTES_v1.2.0.md` - Complete release notes with all changes
  - `UPGRADE_GUIDE_v1.2.0.md` - Step-by-step upgrade instructions from v1.1.0
  - `KNOWN_ISSUES_v1.2.0.md` - Known issues and workarounds
  - `docs/api/README.md` - OpenAPI specification usage guide
  - `docs/api/generate-openapi-specs.sh` - Automated spec generation script
- **Architecture Documentation**
  - OTLP platform configuration summary
  - Gateway trust authentication architecture
  - Database migration runbooks
  - Entity-migration synchronization guide

## [1.1.0] - 2025-12-14

### Added
- **Agent Builder Service**: New microservice for visual AI agent workflow design
  - Agent configuration management with versioning
  - Prompt template library with variable substitution
  - Agent testing framework with mock responses
  - REST API for agent CRUD operations
- **Agent Runtime Service**: Execution engine for AI agents
- **Approval Service**: Human-in-the-loop approval workflows
- **Kubernetes Manifests**: Production-ready k8s deployments for all services
- **Docker Profiles**: Core, AI, Analytics, and Full deployment profiles
- **Grafana Dashboards**: Pre-configured monitoring dashboards
- **n8n Integration**: Workflow automation nodes and templates

### Fixed
- **Spring Security Configuration**: Removed unnecessary `spring.security.user` config from cql-engine-service and quality-measure-service that caused startup failures with empty environment variables
- **Redis Connection**: Added `application-docker.yml` for patient-service and care-gap-service to fix Redis connection in Docker (was connecting to localhost instead of redis hostname)
- **Zookeeper Health Check**: Changed from `ruok` to `srvr` command (4-letter commands disabled by default)
- **Test Configuration**: Added missing test resources for security module HIPAA compliance tests
- **Build Dependencies**: Added missing test dependencies for ehr-connector-service and data-enrichment-service

### Changed
- **Security Architecture**: All microservices now consistently use JWT authentication via Gateway service
- **Docker Compose**: Updated health checks and service dependencies
- **Multi-tenant Support**: Enhanced TenantAccessFilter configuration

### Security
- Removed hardcoded credentials from configuration files (P0 fix from 448bb22)
- Externalized all secrets to environment variables
- Re-enabled TenantAccessFilter for proper multi-tenant isolation

## [1.0.0] - 2025-12-01

### Added
- Initial release of Health Data Intelligence Platform
- Core services: Gateway, FHIR, Patient, CQL Engine, Quality Measure, Care Gap
- Support services: Consent, Event Processing, Event Router
- HIPAA-compliant audit logging
- Real-time WebSocket health score updates
- Redis caching with HIPAA-compliant TTL settings

---

[Unreleased]: https://github.com/healthdata/hdim/compare/v2.9.0...HEAD
[2.9.0]: https://github.com/healthdata/hdim/compare/v2.8.1...v2.9.0
[2.8.1]: https://github.com/healthdata/hdim/compare/v2.8.0...v2.8.1
[2.8.0]: https://github.com/healthdata/hdim/compare/v2.7.2-rc1...v2.8.0
[2.7.2-rc1]: https://github.com/healthdata/hdim/compare/v1.2.0...v2.7.2-rc1
[1.2.0]: https://github.com/healthdata/hdim/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/healthdata/hdim/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/healthdata/hdim/releases/tag/v1.0.0
