# HealthData In Motion - Backend Services

Healthcare platform backend services built with Spring Boot and FHIR R4.

## ⚠️ CRITICAL: HIPAA Compliance

**This application handles Protected Health Information (PHI) and must comply with HIPAA regulations.**

### MANDATORY Reading Before Making Changes:

📋 **[HIPAA Cache Compliance Documentation](HIPAA-CACHE-COMPLIANCE.md)**

This document describes critical security controls that **MUST NOT be disabled or modified**:

1. **HTTP Cache-Control Headers** - Prevents browser/proxy caching of PHI
2. **Redis Cache TTL Limits** - Maximum 5-minute retention for PHI
3. **Frontend RxJS Caching** - Automatic cleanup on component destruction

**⚠️ DO NOT:**
- Increase cache TTL values above documented limits
- Remove or disable cache-control interceptors
- Remove `refCount: true` from RxJS shareReplay operators
- Modify cache settings without compliance review

**📚 Required Reading:**
- [HIPAA-CACHE-COMPLIANCE.md](HIPAA-CACHE-COMPLIANCE.md) - Full compliance documentation
- Inline comments in code marked with ⚠️ warnings

---

## Architecture

```
backend/
├── modules/
│   ├── services/              # Microservices
│   │   ├── patient-service/
│   │   ├── fhir-service/
│   │   ├── cql-engine-service/
│   │   ├── quality-measure-service/
│   │   ├── care-gap-service/
│   │   └── consent-service/
│   └── shared/                # Shared libraries
│       ├── domain/
│       ├── infrastructure/
│       └── api-contracts/
└── platform/                  # Platform services
    └── auth/
```

## Quick Start

### Prerequisites

- Java 21
- Docker & Docker Compose (for PostgreSQL, Redis, Kafka)
- Gradle 8.11+

### Build

> Run backend build commands from the `backend/` directory to use the included Gradle wrapper (`./gradlew`) and avoid host tool version drift.

```bash
# Build all modules
./gradlew build

# Build specific service
./gradlew :modules:services:patient-service:build
```

### Run Services

```bash
# Start infrastructure (PostgreSQL, Redis, Kafka)
docker compose up -d

# Run a service
./gradlew :modules:services:patient-service:bootRun
```

### Run Tests

```bash
# Run all tests
./gradlew test

# Run tests for specific service
./gradlew :modules:services:patient-service:test
```

### Tooling stability (avoid missing Maven/Java)

- **Use the wrapper:** Always call `./gradlew ...` from `backend/`—no system Gradle/Maven needed.
- **Pin JDK:** Install a single JDK (e.g., Temurin 21) via sdkman/asdf and keep it on PATH; Gradle toolchains will pick it up.
- **Container option:** For fully hermetic builds, run tests/builds inside a JDK container (e.g., `eclipse-temurin:21-jdk`) mounting the repo and invoking `./gradlew`.

## Service Ports

| Service | Port | Context Path |
|---------|------|--------------|
| Patient Service | 8084 | /patient |
| FHIR Service | 8085 | /fhir |
| CQL Engine | 8081 | /cql-engine |
| Quality Measure | 8087 | /quality-measure |
| Care Gap | 8086 | /care-gap |

## Configuration

Each service has configuration in `src/main/resources/application.yml`:

- **Database**: PostgreSQL connection settings
- **Cache**: Redis TTL settings ⚠️ **DO NOT INCREASE** (HIPAA compliance)
- **Kafka**: Event streaming configuration
- **Security**: JWT and audit settings

**⚠️ CRITICAL:** Cache TTL settings are HIPAA-compliance requirements. See [HIPAA-CACHE-COMPLIANCE.md](HIPAA-CACHE-COMPLIANCE.md).

## Development Guidelines

### Adding New Services

1. Follow existing service structure in `modules/services/`
2. Use shared domain models from `modules/shared/domain/`
3. Include HIPAA audit logging for PHI operations
4. Configure cache TTLs ≤ 5 minutes for PHI data
5. Add Cache-Control header interceptor for PHI endpoints

### Working with PHI

**All code that handles PHI must:**

1. ✅ Use cache TTL ≤ 5 minutes
2. ✅ Apply `Cache-Control: no-store` headers
3. ✅ Include audit logging (@Audited annotation)
4. ✅ Validate multi-tenant isolation (X-Tenant-ID)
5. ✅ Use encryption for sensitive fields

### Testing

```bash
# Unit tests
./gradlew :modules:services:patient-service:test

# Integration tests
./gradlew :modules:services:patient-service:integrationTest

# HIPAA compliance verification
curl -v http://localhost:8084/patient/api/patients \
  -H "X-Tenant-ID: tenant1" | grep -i cache-control
```

## Security

### Multi-Tenancy

All services enforce tenant isolation via `X-Tenant-ID` header:

```bash
curl http://localhost:8084/patient/api/patients \
  -H "X-Tenant-ID: your-tenant-id" \
  -H "Authorization: Bearer <token>"
```

### Authentication

JWT tokens required for all endpoints (except health checks).

### Audit Logging

All PHI operations are logged to audit database with:
- User ID
- Tenant ID
- Operation type
- Timestamp
- IP address

Retention: 7 years (HIPAA requirement)

## Technology Stack

- **Framework**: Spring Boot 3.x
- **Java**: 21 (LTS)
- **Database**: PostgreSQL 15
- **Cache**: Redis 7
- **Messaging**: Kafka 3.x
- **FHIR**: HAPI FHIR 7.x (R4)
- **Security**: Spring Security, JWT
- **Build**: Gradle 8.11 (Kotlin DSL)

## Resources

- [HIPAA Cache Compliance](HIPAA-CACHE-COMPLIANCE.md) - **REQUIRED READING**
- [FHIR R4 Specification](https://hl7.org/fhir/R4/)
- [HIPAA Security Rule](https://www.hhs.gov/hipaa/for-professionals/security/index.html)
- [CQL Specification](https://cql.hl7.org/)

## Support

For questions about:
- **HIPAA compliance**: Review HIPAA-CACHE-COMPLIANCE.md first
- **Architecture**: Check service-specific README files
- **Development**: See inline code documentation
