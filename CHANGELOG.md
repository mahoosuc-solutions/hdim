# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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

[Unreleased]: https://github.com/healthdata/hdim/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/healthdata/hdim/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/healthdata/hdim/releases/tag/v1.0.0
