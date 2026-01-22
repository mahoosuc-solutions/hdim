# HDIM Platform v1.2.0 - Component Version Matrix

**Release Version:** v1.2.0
**Release Date:** January 25, 2026
**Last Updated:** January 11, 2026

---

## Table of Contents

1. [Core Platform](#core-platform)
2. [Java Ecosystem](#java-ecosystem)
3. [Spring Framework](#spring-framework)
4. [FHIR Stack](#fhir-stack)
5. [Databases](#databases)
6. [Caching & Messaging](#caching--messaging)
7. [Observability & Monitoring](#observability--monitoring)
8. [Container Runtime](#container-runtime)
9. [Security & Authentication](#security--authentication)
10. [Build & Development Tools](#build--development-tools)
11. [Testing Frameworks](#testing-frameworks)
12. [Microservices Version Matrix](#microservices-version-matrix)

---

## Core Platform

| Component | Version | Notes |
|-----------|---------|-------|
| **Java** | 21 LTS (21.0.1+) | Eclipse Temurin recommended, OpenJDK compatible |
| **Gradle** | 8.11.1+ | Kotlin DSL, version catalog |
| **Platform Version** | 1.2.0 | Current release |

**Java 21 Features Used:**
- Virtual threads (Project Loom) for async processing
- Pattern matching for switch expressions
- Record patterns for data extraction
- Sequenced collections
- String templates (preview)

---

## Java Ecosystem

| Library | Version | Purpose | Scope |
|---------|---------|---------|-------|
| **Lombok** | 1.18.30 | Boilerplate reduction | Compile-time |
| **Apache Commons Lang3** | 3.14.0 | Utility functions | Runtime |
| **Guava** | 33.0.0-jre | Collections, caching, utilities | Runtime |
| **Jackson Databind** | 2.16.1 | JSON serialization | Runtime |
| **Jackson JSR310** | 2.16.1 | Java 8 date/time support | Runtime |
| **SLF4J** | 2.0.11 | Logging facade | Runtime |
| **Logback** | 1.4.14 | Logging implementation | Runtime |

---

## Spring Framework

### Spring Boot

| Component | Version | Notes |
|-----------|---------|-------|
| **Spring Boot** | 3.2.2 | Base framework |
| **Spring Boot Starter Web** | 3.2.2 | REST API, MVC |
| **Spring Boot Starter Data JPA** | 3.2.2 | Database access |
| **Spring Boot Starter Data Redis** | 3.2.2 | Caching |
| **Spring Boot Starter Actuator** | 3.2.2 | Health checks, metrics |
| **Spring Boot Starter Security** | 3.2.2 | Authentication, authorization |
| **Spring Boot Starter Validation** | 3.2.2 | Bean validation |
| **Spring Boot Starter WebSocket** | 3.2.2 | Real-time communication |
| **Spring Boot Starter Mail** | 3.2.2 | Email notifications |
| **Spring Boot Starter Thymeleaf** | 3.2.2 | Template engine |
| **Spring Boot Starter Test** | 3.2.2 | Testing support |

### Spring Cloud

| Component | Version | Notes |
|-----------|---------|-------|
| **Spring Cloud** | 2023.0.0 | Cloud platform support |
| **Spring Cloud OpenFeign** | 4.1.0 | Declarative HTTP clients |
| **Spring Cloud Config** | 4.1.0 | Centralized configuration |

### Spring Data

| Component | Version | Notes |
|-----------|---------|-------|
| **Spring Data JPA** | 3.2.2 | JPA repositories |
| **Spring Data Redis** | 3.2.2 | Redis repositories, caching |
| **Hibernate Core** | 6.4.1.Final | JPA implementation |
| **Hibernate Validator** | 8.0.1.Final | Bean validation |

### Spring Security

| Component | Version | Notes |
|-----------|---------|-------|
| **Spring Security Core** | 6.2.1 | Security framework |
| **Spring Security Web** | 6.2.1 | Web security |
| **Spring Security Config** | 6.2.1 | Security configuration |
| **Spring Security Test** | 6.2.1 | Security testing |

---

## FHIR Stack

| Component | Version | FHIR Version | Notes |
|-----------|---------|--------------|-------|
| **HAPI FHIR Base** | 7.0.2 | R4 | FHIR core library |
| **HAPI FHIR Structures R4** | 7.0.2 | R4 | R4 resource definitions |
| **HAPI FHIR Client** | 7.0.2 | R4 | REST client |
| **HAPI FHIR Validation** | 7.0.2 | R4 | Resource validation |
| **FHIR Standard** | R4 (4.0.1) | R4 | HL7 FHIR R4 specification |

**Supported Resource Types:**
- Patient, Practitioner, Organization
- Condition, Observation, Procedure
- MedicationRequest, MedicationStatement
- Encounter, DiagnosticReport
- Immunization, AllergyIntolerance
- Consent, DocumentReference
- RiskAssessment, CareTeam

---

## Databases

### PostgreSQL

| Component | Version | Notes |
|-----------|---------|-------|
| **PostgreSQL** | 16.1-alpine | Primary database |
| **PostgreSQL JDBC Driver** | 42.7.1 | JDBC driver |
| **Liquibase Core** | 4.29.2 | Schema migrations |
| **HikariCP** | 5.1.0 | Connection pooling |

**PostgreSQL Extensions:**
- `pg_trgm` - Trigram matching for full-text search (used by fhir, cql, quality, patient services)
- `uuid-ossp` - UUID generation (optional, prefer `gen_random_uuid()`)

**Database Count:** 29 logical databases (one per microservice)

**Key Databases:**
- `fhir_db` - FHIR R4 resources
- `patient_db` - Patient demographics
- `quality_db` - HEDIS quality measures
- `cql_db` - CQL evaluation engine
- `caregap_db` - Care gap detection
- `gateway_db` - Authentication, users, roles

### Schema Management

| Component | Version | Purpose |
|-----------|---------|---------|
| **Liquibase** | 4.29.2 | All services (standard) |
| **Flyway** | Disabled | Not used (migrated to Liquibase) |

**Migration Status:**
- All 29 services use Liquibase exclusively
- 100% rollback SQL coverage (199/199 changesets)
- `ddl-auto: validate` enforced across all services

---

## Caching & Messaging

### Redis

| Component | Version | Notes |
|-----------|---------|-------|
| **Redis** | 7.2-alpine | In-memory cache |
| **Spring Data Redis** | 3.2.2 | Redis integration |
| **Lettuce** | 6.3.1.RELEASE | Redis client |

**HIPAA Cache TTL Settings:**
- PHI data: ≤ 5 minutes (300 seconds)
- Measure results: 2 minutes (120 seconds)
- Eligibility cache: 24 hours (86400 seconds)

### Apache Kafka

| Component | Version | Notes |
|-----------|---------|-------|
| **Apache Kafka** | 3.6.1 | Event streaming |
| **Spring Kafka** | 3.1.1 | Kafka integration |
| **Kafka Clients** | 3.6.1 | Producer/consumer clients |

**Topics:**
- `measure-evaluations` - Quality measure results
- `care-gap-events` - Care gap detection events
- `clinical-alerts` - Clinical alert notifications
- `audit-events` - HIPAA audit trail

---

## Observability & Monitoring

### Distributed Tracing (NEW in v1.2.0)

| Component | Version | Protocol | Notes |
|-----------|---------|----------|-------|
| **OpenTelemetry SDK** | 1.35.0 | OTLP HTTP | Trace instrumentation |
| **OpenTelemetry Instrumentation** | 1.35.0 | W3C, B3 | Auto-instrumentation |
| **Jaeger All-in-One** | Latest | OTLP HTTP | Trace collection, visualization |
| **OTLP Protocol** | 1.1.0 | HTTP/Protobuf | Trace export protocol |

**Configuration:**
- Endpoint: `http://jaeger:4318/v1/traces`
- Protocol: `http/protobuf`
- Propagation: W3C Trace Context + B3
- Services instrumented: 11 Java microservices

**Jaeger Ports:**
- 16686 - Jaeger UI
- 4318 - OTLP HTTP receiver
- 14268 - Jaeger collector HTTP (optional)
- 14269 - Health check endpoint

### Metrics & Monitoring

| Component | Version | Notes |
|-----------|---------|-------|
| **Micrometer Core** | 1.12.2 | Metrics facade |
| **Micrometer Registry Prometheus** | 1.12.2 | Prometheus exporter |
| **Prometheus** | 2.49.1 | Metrics collection |
| **Grafana** | 10.3.1 | Metrics visualization |

**Prometheus Exporters:**
- JVM metrics (heap, GC, threads)
- Spring Boot metrics (HTTP requests, data source)
- Custom business metrics (measure evaluations, care gaps)

**Grafana Dashboards:**
- JVM dashboard (heap, CPU, threads)
- HTTP request metrics
- Database connection pool metrics
- Cache hit/miss ratios
- Kafka consumer lag

---

## Container Runtime

| Component | Version | Notes |
|-----------|---------|-------|
| **Docker** | 24.0.7+ | Container runtime |
| **Docker Compose** | 2.24.5+ | Multi-container orchestration |
| **Docker BuildKit** | 0.12+ | Enhanced builds |

**Base Images:**
- Java services: `eclipse-temurin:21-jre-alpine`
- PostgreSQL: `postgres:16.1-alpine`
- Redis: `redis:7.2-alpine`
- Kafka: `confluentinc/cp-kafka:7.6.0`
- Jaeger: `jaegertracing/all-in-one:latest`

---

## Security & Authentication

### Security Libraries

| Component | Version | Purpose |
|-----------|---------|---------|
| **Spring Security** | 6.2.1 | Authentication, authorization |
| **jjwt-api** | 0.12.5 | JWT token creation |
| **jjwt-impl** | 0.12.5 | JWT implementation |
| **jjwt-jackson** | 0.12.5 | JWT JSON support |
| **Bouncy Castle** | 1.77 | Cryptography |

### Authentication Architecture

**Pattern:** Gateway Trust Authentication
- Gateway validates JWT tokens
- Backend services trust `X-Auth-*` headers from gateway
- No JWT re-validation in backend services
- HMAC signature validation in production

**Required Headers:**
- `X-Auth-User-Id` - User UUID
- `X-Auth-Username` - Login name
- `X-Auth-Tenant-Ids` - Authorized tenants (comma-separated)
- `X-Auth-Roles` - User roles (comma-separated)
- `X-Auth-Validated` - HMAC signature

### Secrets Management

| Component | Version | Purpose |
|-----------|---------|---------|
| **HashiCorp Vault** | 1.15.4 | Secrets storage |
| **Spring Cloud Vault** | 4.1.0 | Vault integration |

---

## Build & Development Tools

### Build Tools

| Tool | Version | Purpose |
|------|---------|---------|
| **Gradle** | 8.11.1 | Build automation |
| **Gradle Kotlin DSL** | 1.9.22 | Build configuration |
| **Gradle Version Catalog** | Built-in | Centralized dependency versions |

**Gradle Plugins:**
- `org.springframework.boot` - Spring Boot plugin
- `io.spring.dependency-management` - Dependency management
- `java` - Java compilation
- `jacoco` - Code coverage

### Development Tools

| Tool | Version | Purpose |
|------|---------|---------|
| **Git** | 2.43+ | Version control |
| **IntelliJ IDEA** | 2023.3+ | IDE (recommended) |
| **VS Code** | 1.85+ | Alternative IDE |
| **Postman** | 10.21+ | API testing |

---

## Testing Frameworks

### Unit Testing

| Framework | Version | Purpose |
|-----------|---------|---------|
| **JUnit Jupiter** | 5.10.1 | Test framework |
| **Mockito Core** | 5.8.0 | Mocking framework |
| **Mockito JUnit Jupiter** | 5.8.0 | Mockito-JUnit integration |
| **AssertJ Core** | 3.25.1 | Fluent assertions |

### Integration Testing

| Framework | Version | Purpose |
|-----------|---------|---------|
| **Spring Boot Test** | 3.2.2 | Spring integration testing |
| **Spring Security Test** | 6.2.1 | Security testing |
| **MockMvc** | 6.1.3 | Controller testing |
| **Testcontainers** | 1.19.3 | Docker containers for testing |
| **Testcontainers PostgreSQL** | 1.19.3 | PostgreSQL test containers |
| **Testcontainers Redis** | 1.19.3 | Redis test containers |
| **Testcontainers Kafka** | 1.19.3 | Kafka test containers |

### Code Coverage

| Tool | Version | Target |
|------|---------|--------|
| **JaCoCo** | 0.8.11 | ≥70% line coverage |

---

## Microservices Version Matrix

### Core Services (v1.2.0)

| Service | Version | Port | Database | Key Features |
|---------|---------|------|----------|--------------|
| **gateway-service** | 1.2.0 | 8001 | gateway_db | API gateway, JWT validation, rate limiting |
| **cql-engine-service** | 1.2.0 | 8081 | cql_db | CQL evaluation, HEDIS measure calculation |
| **fhir-service** | 1.2.0 | 8085 | fhir_db | FHIR R4 resources, resource validation |
| **patient-service** | 1.2.0 | 8084 | patient_db | Patient demographics, PHI management |
| **quality-measure-service** | 1.2.0 | 8087 | quality_db | Quality measures, assignments, overrides (NEW) |
| **care-gap-service** | 1.2.0 | 8086 | caregap_db | Care gap detection, closure tracking |

### Supporting Services (v1.2.0)

| Service | Version | Port | Database | Key Features |
|---------|---------|------|----------|--------------|
| **notification-service** | 1.2.0 | 8107 | notification_db | Email, SMS notifications (PORT CHANGED) |
| **analytics-service** | 1.2.0 | 8091 | analytics_db | Quality reporting, dashboards |
| **event-processing-service** | 1.2.0 | 8090 | event_processing_db | Event routing, transformation |
| **qrda-export-service** | 1.2.0 | 8100 | qrda_db | QRDA I/III export |
| **hcc-service** | 1.2.0 | 8103 | hcc_db | HCC risk adjustment |

### Infrastructure Services (v1.2.0)

| Service | Version | Port | Purpose |
|---------|---------|------|---------|
| **Jaeger** | Latest | 16686, 4318 | Distributed tracing (NEW) |
| **PostgreSQL** | 16.1 | 5435 | Primary database |
| **Redis** | 7.2 | 6380 | Caching layer |
| **Kafka** | 3.6.1 | 9094 | Event streaming |
| **Prometheus** | 2.49.1 | 9090 | Metrics collection |
| **Grafana** | 10.3.1 | 3001 | Metrics visualization |

---

## API Versions

### REST API Versions

| Service | API Version | Base Path |
|---------|-------------|-----------|
| gateway-service | v1 | `/api/v1` |
| cql-engine-service | v1 | `/cql-engine/api/v1` |
| fhir-service | R4 | `/fhir` (FHIR R4 standard) |
| patient-service | v1 | `/patient/api/v1` |
| quality-measure-service | v1 | `/quality-measure/api/v1` |
| care-gap-service | v1 | `/care-gap/api/v1` |

### New Endpoints (v1.2.0)

**quality-measure-service:**
- Measure Assignment API: 4 endpoints (NEW)
- Measure Override API: 8 endpoints (NEW)

---

## Dependency Version Catalog

Location: `backend/gradle/libs.versions.toml`

### Version Catalog Structure

```toml
[versions]
spring-boot = "3.2.2"
spring-cloud = "2023.0.0"
hapi-fhir = "7.0.2"
kafka = "3.6.1"
postgresql = "42.7.1"
liquibase = "4.29.2"
testcontainers = "1.19.3"
opentelemetry = "1.35.0"

[libraries]
spring-boot-starter-web = { module = "org.springframework.boot:spring-boot-starter-web", version.ref = "spring-boot" }
# ... (full catalog in libs.versions.toml)

[bundles]
spring-boot-web = ["spring-boot-starter-web", "spring-boot-starter-actuator"]
hapi-fhir-client = ["hapi-fhir-base", "hapi-fhir-structures-r4", "hapi-fhir-client"]
# ... (full bundles in libs.versions.toml)
```

---

## Breaking Changes from v1.1.0

### Version Updates

| Component | v1.1.0 | v1.2.0 | Impact |
|-----------|--------|--------|--------|
| **Java** | 21 | 21 | No change |
| **Spring Boot** | 3.2.2 | 3.2.2 | No change |
| **PostgreSQL** | 16 | 16 | No change |
| **OpenTelemetry** | N/A | 1.35.0 | **NEW** - Jaeger required |
| **notification-service port** | 8089 | 8107 | **BREAKING** - Port changed |

### New Dependencies (v1.2.0)

- OpenTelemetry SDK 1.35.0
- OpenTelemetry Instrumentation 1.35.0
- Jaeger all-in-one (latest)

### Configuration Changes

**All Java Services:**
- Added `OTEL_EXPORTER_OTLP_ENDPOINT`
- Added `OTEL_EXPORTER_OTLP_PROTOCOL`
- Added `OTEL_SERVICE_NAME`
- Added `_JAVA_OPTIONS: "-Djava.net.preferIPv4Stack=true"`

**notification-service:**
- `ddl-auto: create` → `validate` (CRITICAL)
- `SPRING_LIQUIBASE_ENABLED: false` → `true` (CRITICAL)
- `server.port: 8089` → `8107` (BREAKING)

---

## Compatibility Matrix

### Minimum Requirements

| Component | Minimum Version | Recommended Version |
|-----------|----------------|---------------------|
| Java | 21 | 21.0.1+ (Temurin) |
| Docker | 24.0 | 24.0.7+ |
| Docker Compose | 2.20 | 2.24.5+ |
| PostgreSQL | 16 | 16.1 |
| Git | 2.30 | 2.43+ |

### Operating System Compatibility

| OS | Versions | Notes |
|----|----------|-------|
| **Linux** | Ubuntu 22.04+, RHEL 9+, Debian 12+ | Primary development platform |
| **macOS** | 13 (Ventura)+, 14 (Sonoma)+ | Supported |
| **Windows** | 11 with WSL2 | WSL2 required for Docker |

### Browser Compatibility (Frontend)

| Browser | Minimum Version |
|---------|----------------|
| Chrome | 120+ |
| Firefox | 121+ |
| Safari | 17+ |
| Edge | 120+ |

---

## Upgrade Path

### From v1.1.0 to v1.2.0

**Prerequisites:**
- Backup all databases
- Review `UPGRADE_GUIDE_v1.2.0.md`
- Schedule maintenance window (5-10 minutes)

**Steps:**
1. Stop all services: `docker compose down`
2. Pull v1.2.0 code: `git checkout v1.2.0`
3. Update docker-compose.yml (add Jaeger service)
4. Rebuild services: `docker compose --profile core build`
5. Start infrastructure: `docker compose up -d postgres redis kafka jaeger`
6. Start core services: `docker compose --profile core up -d`
7. Verify migrations: Check `databasechangelog` table
8. Verify health: `docker compose ps`

**Estimated Time:** 30-60 minutes

---

## Deprecation Notices

### Deprecated in v1.2.0

| Component | Deprecation | Removal | Alternative |
|-----------|-------------|---------|-------------|
| `NotificationService.sendNotification()` | v1.2.0 | v1.3.0 | TBD (notification-service API redesign) |

### Removed in v1.2.0

| Component | Removed | Replacement |
|-----------|---------|-------------|
| Flyway | v1.2.0 | Liquibase (all services) |

---

## Support & Resources

### Documentation

- [Release Notes](RELEASE_NOTES_v1.2.0.md)
- [Upgrade Guide](UPGRADE_GUIDE_v1.2.0.md)
- [Known Issues](KNOWN_ISSUES_v1.2.0.md)
- [System Architecture](docs/architecture/SYSTEM_ARCHITECTURE.md)
- [CLAUDE.md](CLAUDE.md) - AI coding agent guidelines

### External Resources

- [Spring Boot 3.2 Reference](https://docs.spring.io/spring-boot/docs/3.2.x/reference/html/)
- [HAPI FHIR Documentation](https://hapifhir.io/hapi-fhir/docs/)
- [OpenTelemetry Java SDK](https://opentelemetry.io/docs/instrumentation/java/)
- [Jaeger Documentation](https://www.jaegertracing.io/docs/latest/)
- [PostgreSQL 16 Documentation](https://www.postgresql.org/docs/16/index.html)
- [Liquibase Documentation](https://docs.liquibase.com/)

---

## Changelog

| Version | Date | Changes |
|---------|------|---------|
| 1.2.0 | 2026-01-25 | Added OpenTelemetry, Jaeger, measure assignment/override features |
| 1.1.0 | 2025-12-14 | Added Agent Builder/Runtime services, Kubernetes manifests |
| 1.0.0 | 2025-12-01 | Initial release |

---

**Document Version:** 1.0
**Status:** Ready for Release
**Last Updated:** January 11, 2026
**Maintained By:** HDIM Platform Team
