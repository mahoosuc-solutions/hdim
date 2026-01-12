# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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

[Unreleased]: https://github.com/healthdata/hdim/compare/v1.2.0...HEAD
[1.2.0]: https://github.com/healthdata/hdim/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/healthdata/hdim/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/healthdata/hdim/releases/tag/v1.0.0
