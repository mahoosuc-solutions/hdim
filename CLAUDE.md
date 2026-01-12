# CLAUDE.md - AI Coding Agent Guidelines for HDIM

## Project Overview

**HealthData-in-Motion (HDIM)** is an enterprise healthcare interoperability platform for HEDIS quality measure evaluation, FHIR R4 compliance, and clinical decision support.

**Primary Purpose**: Enable healthcare organizations to evaluate clinical quality measures (CQL/HEDIS), identify care gaps, perform risk stratification, and generate quality reports for value-based care contracts.

**Target Users**: Healthcare payers, ACOs, health systems, and clinical quality teams.

---

## Tech Stack

### Backend
- **Language**: Java 21 (LTS)
- **Framework**: Spring Boot 3.x
- **Build**: Gradle 8.11+ (Kotlin DSL)
- **FHIR**: HAPI FHIR 7.x (R4)
- **Database**: PostgreSQL 16
- **Cache**: Redis 7
- **Messaging**: Apache Kafka 3.x
- **API Gateway**: Kong
- **Security**: Spring Security + JWT

### Frontend
- **Framework**: Angular 17+
- **State**: RxJS
- **UI**: Angular Material

### Infrastructure
- **Containers**: Docker + Docker Compose
- **Orchestration**: Kubernetes (optional)
- **Monitoring**: Prometheus + Grafana
- **Secrets**: HashiCorp Vault

---

## Project Structure

```
hdim-master/
├── backend/
│   ├── modules/
│   │   ├── services/           # 28 microservices
│   │   │   ├── quality-measure-service/   # Core - HEDIS measures
│   │   │   ├── cql-engine-service/        # Core - CQL evaluation
│   │   │   ├── fhir-service/              # Core - FHIR R4 resources
│   │   │   ├── patient-service/           # Core - Patient data
│   │   │   ├── care-gap-service/          # Care gap detection
│   │   │   ├── consent-service/           # Patient consent
│   │   │   ├── gateway-service/           # API gateway
│   │   │   ├── analytics-service/         # Quality reporting
│   │   │   ├── predictive-analytics-service/
│   │   │   ├── ehr-connector-service/
│   │   │   ├── hcc-service/               # HCC risk adjustment
│   │   │   ├── prior-auth-service/
│   │   │   ├── qrda-export-service/       # QRDA I/III export
│   │   │   ├── sdoh-service/              # Social determinants
│   │   │   └── ... (20+ more services)
│   │   └── shared/             # Shared libraries
│   │       ├── domain/
│   │       ├── infrastructure/
│   │       └── api-contracts/
│   └── platform/
│       └── auth/
├── docker/                     # Docker configurations
│   ├── grafana/
│   ├── prometheus/
│   ├── postgres/
│   ├── redis/
│   └── vault/
├── docs/                       # Documentation portal
│   ├── architecture/
│   ├── operations/
│   ├── users/
│   ├── sales/
│   └── services/
└── docker-compose*.yml         # Various deployment configs
```

---

## Critical: HIPAA Compliance Requirements

**This application handles Protected Health Information (PHI). All code MUST comply with HIPAA regulations.**

### Mandatory Reading Before Modifying PHI-Related Code
- `backend/HIPAA-CACHE-COMPLIANCE.md`

### Cache Rules (DO NOT VIOLATE)
```java
// PHI cache TTL MUST be <= 5 minutes
@Cacheable(value = "patientData", key = "#patientId")
// Redis TTL configuration in application.yml must not exceed 300 seconds
```

### Required HTTP Headers for PHI Endpoints
```java
// All PHI responses MUST include:
response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
response.setHeader("Pragma", "no-cache");
```

### Audit Logging Required
```java
// Use @Audited annotation on all PHI access methods
@Audited(eventType = "PHI_ACCESS")
public Patient getPatient(String patientId) { ... }
```

### Multi-Tenant Isolation
```java
// All queries MUST filter by tenant
@Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId AND p.id = :id")
Optional<Patient> findByIdAndTenant(@Param("id") String id, @Param("tenantId") String tenantId);
```

---

## Service Ports

| Service | Port | Context Path |
|---------|------|--------------|
| Quality Measure | 8087 | /quality-measure |
| CQL Engine | 8081 | /cql-engine |
| FHIR Service | 8085 | /fhir |
| Patient Service | 8084 | /patient |
| Care Gap Service | 8086 | /care-gap |
| Gateway | 8001 | / |
| Kong API Gateway | 8000 | / |
| PostgreSQL | 5435 | - |
| Redis | 6380 | - |
| Kafka | 9094 | - |
| Prometheus | 9090 | - |
| Grafana | 3001 | - |

---

## Common Commands

### Build Commands (run from `backend/` directory)
```bash
# Build all modules
./gradlew build

# Build specific service
./gradlew :modules:services:quality-measure-service:build

# Run tests
./gradlew test

# Run specific service tests
./gradlew :modules:services:quality-measure-service:test

# Run integration tests (requires Docker)
./gradlew integrationTest

# Create bootable JAR
./gradlew :modules:services:quality-measure-service:bootJar
```

### Docker Commands (run from project root)
```bash
# Start all services
docker compose up -d

# Start core services only
docker compose --profile core up -d

# View logs
docker compose logs -f quality-measure-service

# Check health
docker compose ps

# Stop all
docker compose down

# Rebuild and start
docker compose up -d --build
```

### Database Commands
```bash
# Connect to PostgreSQL
docker exec -it hdim-postgres psql -U healthdata -d healthdata_qm

# Run migrations
./run-migrations.sh
```

---

## Coding Patterns & Conventions

### Package Structure (per service)
```
com.healthdata.{service}/
├── api/                    # REST controllers
│   └── v1/                 # API version
├── application/            # Application services
├── domain/                 # Domain models, entities
│   ├── model/
│   └── repository/
├── infrastructure/         # External integrations
│   ├── persistence/
│   ├── messaging/
│   └── external/
└── config/                 # Spring configuration
```

### Controller Pattern
```java
@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
@Validated
public class PatientController {

    private final PatientService patientService;

    @GetMapping("/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'VIEWER')")
    @Audited(eventType = "PATIENT_ACCESS")
    public ResponseEntity<PatientResponse> getPatient(
            @PathVariable String patientId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(patientService.getPatient(patientId, tenantId));
    }
}
```

### Service Pattern
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientService {

    private final PatientRepository patientRepository;
    private final AuditService auditService;

    public PatientResponse getPatient(String patientId, String tenantId) {
        return patientRepository.findByIdAndTenant(patientId, tenantId)
            .map(this::mapToResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
    }

    @Transactional
    public PatientResponse createPatient(CreatePatientRequest request, String tenantId) {
        // Implementation with audit logging
    }
}
```

### Entity Pattern
```java
@Entity
@Table(name = "patients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "fhir_id", nullable = false)
    private String fhirId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
```

### Exception Handling
```java
// Use domain-specific exceptions
throw new ResourceNotFoundException("Patient", patientId);
throw new ValidationException("Invalid PHQ-9 score: must be 0-27");
throw new TenantAccessDeniedException(tenantId);

// Global exception handler exists in each service
// See: GlobalExceptionHandler.java
```

---

## Authentication & Authorization

### JWT Token Structure
```json
{
  "sub": "user@example.com",
  "tenant_id": "TENANT001",
  "roles": ["ADMIN", "EVALUATOR"],
  "exp": 1699564800,
  "iat": 1699561200
}
```

### Role Hierarchy
| Role | Access Level |
|------|--------------|
| SUPER_ADMIN | Full system access |
| ADMIN | Tenant-level admin |
| EVALUATOR | Run evaluations, view results |
| ANALYST | View reports, analytics |
| VIEWER | Read-only access |

### Securing Endpoints
```java
@PreAuthorize("hasRole('ADMIN')")  // Admin only
@PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")  // Admin or Evaluator
@PreAuthorize("hasRole('ADMIN') or @tenantSecurity.isOwner(#patientId)")  // Complex
```

### Test Users (Development Only)
| Username | Password | Role |
|----------|----------|------|
| test_superadmin | password123 | SUPER_ADMIN |
| test_admin | password123 | ADMIN |
| test_evaluator | password123 | EVALUATOR |
| test_analyst | password123 | ANALYST |
| test_viewer | password123 | VIEWER |

---

## Gateway Trust Authentication Architecture

**IMPORTANT**: Backend services use gateway-trust authentication, NOT direct JWT validation.

### Architecture Overview
```
Client → Gateway (validates JWT) → Backend Service (trusts headers)
```

The gateway validates JWT tokens and injects trusted `X-Auth-*` headers. Backend services trust these headers rather than re-validating JWT or performing database lookups.

### Key Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `TrustedHeaderAuthFilter` | shared/authentication | Validates gateway headers, sets SecurityContext |
| `TrustedTenantAccessFilter` | shared/authentication | Validates tenant access from attributes (no DB) |
| `GatewayAuthenticationFilter` | gateway-service | Validates JWT, injects trusted headers |

### Headers Injected by Gateway

| Header | Description |
|--------|-------------|
| `X-Auth-User-Id` | User's UUID |
| `X-Auth-Username` | User's login name |
| `X-Auth-Tenant-Ids` | Comma-separated authorized tenants |
| `X-Auth-Roles` | Comma-separated roles |
| `X-Auth-Validated` | HMAC signature proving gateway origin |

### When Modifying Service Security

**DO NOT** use `JwtAuthenticationFilter` + `TenantAccessFilter` (performs DB lookup).

**DO** use `TrustedHeaderAuthFilter` + `TrustedTenantAccessFilter` (trusts gateway headers).

```java
// CORRECT - Gateway Trust Pattern
@Bean
public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        TrustedHeaderAuthFilter trustedHeaderAuthFilter,
        TrustedTenantAccessFilter trustedTenantAccessFilter) {
    http.addFilterBefore(trustedHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class);
    http.addFilterAfter(trustedTenantAccessFilter, TrustedHeaderAuthFilter.class);
    return http.build();
}
```

### Configuration

```yaml
# docker-compose.yml
environment:
  GATEWAY_AUTH_DEV_MODE: "true"          # Development: skip HMAC validation
  GATEWAY_AUTH_SIGNING_SECRET: ${SECRET} # Production: HMAC secret
```

### Documentation
- Full details: `backend/docs/GATEWAY_TRUST_ARCHITECTURE.md`

---

## FHIR R4 Guidelines

### Resource Handling
```java
// Use HAPI FHIR client
FhirContext ctx = FhirContext.forR4();
IGenericClient client = ctx.newRestfulGenericClient(fhirServerUrl);

// Parse resources
Patient patient = ctx.newJsonParser().parseResource(Patient.class, json);

// Create bundle
Bundle bundle = new Bundle();
bundle.setType(Bundle.BundleType.SEARCHSET);
bundle.addEntry().setResource(patient);
```

### Supported Resource Types
- Patient, Practitioner, Organization
- Condition, Observation, Procedure
- MedicationRequest, MedicationStatement
- Encounter, DiagnosticReport
- Immunization, AllergyIntolerance
- Consent, DocumentReference

---

## Testing Requirements

### Unit Tests
```java
@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientService patientService;

    @Test
    void shouldReturnPatient_WhenPatientExists() {
        // Given
        when(patientRepository.findByIdAndTenant(any(), any()))
            .thenReturn(Optional.of(testPatient));

        // When
        PatientResponse result = patientService.getPatient("123", "tenant1");

        // Then
        assertThat(result.getId()).isEqualTo("123");
    }
}
```

### Integration Tests
```java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PatientControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnPatient() throws Exception {
        mockMvc.perform(get("/api/v1/patients/123")
                .header("X-Tenant-ID", "tenant1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("123"));
    }
}
```

### Required Test Coverage
- Unit tests for all service methods
- Integration tests for API endpoints
- Cache behavior verification (TTL compliance)
- Multi-tenant isolation tests
- RBAC permission tests

---

## Distributed Tracing

HDIM implements **OpenTelemetry** distributed tracing across all 34 microservices for end-to-end request visibility.

### Architecture Overview

**Trace Propagation:** Automatic via interceptors (no code changes needed)

```
Client → Gateway → Service A → Service B → Kafka → Service C
         │          │           │           │        │
         └──────────┴───────────┴───────────┴────────┘
              All linked by trace-id
```

### Automatic Trace Propagation

| Transport | Interceptor | Status | Configuration |
|-----------|-------------|--------|---------------|
| **HTTP (Feign)** | FeignTraceInterceptor | ✅ Auto-enabled | No config needed |
| **HTTP (RestTemplate)** | RestTemplateTraceInterceptor | ✅ Auto-enabled | No config needed |
| **Kafka (Producer)** | KafkaProducerTraceInterceptor | ✅ Configured | Add to application.yml |
| **Kafka (Consumer)** | KafkaConsumerTraceInterceptor | ✅ Configured | Add to application.yml |

### Kafka Tracing Configuration

For services that use Kafka messaging:

```yaml
spring:
  kafka:
    producer:
      properties:
        interceptor.classes: com.healthdata.tracing.KafkaProducerTraceInterceptor
    consumer:
      properties:
        interceptor.classes: com.healthdata.tracing.KafkaConsumerTraceInterceptor
```

### Environment-Specific Sampling

Control trace volume with environment-specific sampling rates:

```yaml
---
# Development Profile - 100% Trace Sampling
spring:
  config:
    activate:
      on-profile: dev

management:
  tracing:
    sampling:
      probability: 1.0  # Capture all traces for debugging

---
# Staging Profile - 50% Trace Sampling
spring:
  config:
    activate:
      on-profile: staging

management:
  tracing:
    sampling:
      probability: 0.5  # Balance visibility and performance

---
# Production Profile - 10% Trace Sampling
spring:
  config:
    activate:
      on-profile: prod

management:
  tracing:
    sampling:
      probability: 0.1  # Cost-effective monitoring
```

### Adding Custom Spans

For business operations that need explicit tracing:

```java
@Service
public class QualityMeasureService {

    private final Tracer tracer;

    public EvaluationResult evaluateMeasure(String measureId, String patientId) {
        Span span = tracer.spanBuilder("evaluate_measure")
                .setAttribute("measure.id", measureId)
                .setAttribute("patient.id", patientId)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            EvaluationResult result = performEvaluation(measureId, patientId);
            span.setAttribute("result.score", result.getScore());
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
```

### Jaeger Integration

**Access Jaeger UI:** `http://localhost:16686`

**Query Examples:**
- Find slow requests: `service=fhir-service duration>5s`
- Find errors: `service=patient-service error=true`
- Find patient operations: `patient.id=PATIENT123`

### Documentation

- **Complete Guide:** `backend/docs/DISTRIBUTED_TRACING_GUIDE.md`
- **Shared Tracing Module:** `modules/shared/infrastructure/tracing/`

---

## Configuration Files

### Key Configuration Locations
- `src/main/resources/application.yml` - Main config
- `src/main/resources/application-dev.yml` - Development
- `src/main/resources/application-prod.yml` - Production
- `src/main/resources/db/changelog/` - Liquibase migrations

### Environment Variables
```bash
# Database
POSTGRES_HOST=localhost
POSTGRES_PORT=5435
POSTGRES_DB=healthdata_qm
POSTGRES_USER=healthdata
POSTGRES_PASSWORD=<secret>

# Redis
REDIS_HOST=localhost
REDIS_PORT=6380
REDIS_PASSWORD=<secret>

# JWT
JWT_SECRET=<64-char-hex-string>
JWT_EXPIRATION_MS=900000

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9094
```

---

## Important Files Reference

### Must-Read Before Development
1. `backend/HIPAA-CACHE-COMPLIANCE.md` - PHI handling requirements
2. `AUTHENTICATION_GUIDE.md` - Auth flow and test users
3. `DISTRIBUTION_ARCHITECTURE.md` - System architecture
4. `BACKEND_API_SPECIFICATION.md` - API design patterns

### Key Implementation Files
- `backend/modules/shared/domain/` - Shared domain models
- `backend/modules/shared/infrastructure/` - Common infrastructure
- `docker-compose.yml` - Local development setup
- `docker-compose.production.yml` - Production deployment

### Documentation
- `docs/PRODUCTION_SECURITY_GUIDE.md` - Security hardening
- `docs/DEPLOYMENT_RUNBOOK.md` - Deployment procedures
- `00_IMPLEMENTATION_OVERVIEW.md` - Documentation portal status

### Architecture Documentation
- `docs/architecture/SYSTEM_ARCHITECTURE.md` - Complete system architecture with 28 services
- `docs/architecture/decisions/` - Technology ADRs (HAPI FHIR, Kafka, PostgreSQL, Redis, Kong)
- `docs/TERMINOLOGY_GLOSSARY.md` - Single source of truth for terminology
- `backend/docs/GATEWAY_TRUST_ARCHITECTURE.md` - Authentication architecture (gold standard)

### Build & Dependencies
- `backend/gradle/libs.versions.toml` - Centralized version catalog
- `backend/DEPENDENCY_MANAGEMENT.md` - Dependency standards
- `backend/scripts/validate-dependency-versions.sh` - Validation script

---

## Build Notes

### Build Status
- ✅ All 28 services compile successfully (verified December 2025)
- ✅ agent-runtime-service: 84 tests passing
- ✅ agent-builder-service: Build successful

### Build Prerequisites
```bash
# Verify Java 21
java -version  # Should show 21.x

# Verify Gradle wrapper
./gradlew --version  # Should show 8.11+

# Verify Docker
docker --version  # Should show 24.0+
```

---

## Code Review Checklist

Before submitting code, verify:

- [ ] HIPAA compliance: Cache TTL <= 5 minutes for PHI
- [ ] Cache-Control headers on PHI endpoints
- [ ] @Audited annotation on PHI access methods
- [ ] Multi-tenant filtering in all queries
- [ ] @PreAuthorize on all API endpoints
- [ ] X-Tenant-ID header validation
- [ ] Unit tests for new functionality
- [ ] Integration tests for API changes
- [ ] No hardcoded credentials or secrets
- [ ] Proper exception handling
- [ ] Logging without PHI in messages

---

## Entity-Migration Synchronization (CRITICAL)

**This practice prevents production schema drift issues (like the RefreshToken authentication bug).**

### Quick Checklist

When **creating** a new entity:
- [ ] Create JPA entity with `@Entity`, `@Table`, `@Column` annotations
- [ ] Create Liquibase migration file (`NNNN-create-table.xml`)
- [ ] Add migration include to `db.changelog-master.xml`
- [ ] Use sequential migration numbers (no gaps, no reuse)
- [ ] Run validation test: `./gradlew test --tests "*EntityMigrationValidationTest"`

When **modifying** an entity:
- [ ] Update `@Column` annotations
- [ ] Create NEW migration (never modify existing ones)
- [ ] Use descriptive migration ID: `NNNN-add-field-to-table.xml`
- [ ] Run validation test to ensure sync

### Validation Tests

Every critical service has automated validation:
- ✅ authentication module
- ✅ patient-service
- ✅ quality-measure-service
- ✅ care-gap-service
- ✅ fhir-service
- ✅ sales-automation-service

Run locally: `./gradlew test --tests "*EntityMigrationValidationTest"`

### Hibernate Configuration

**CRITICAL**: All environments must use proper DDL auto settings:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # Use ONLY 'validate' in prod/docker/dev

  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml
```

**NEVER** use `ddl-auto: update` or `ddl-auto: create` in production.

### Column Type Mapping

| Java | PostgreSQL | Liquibase |
|------|------------|-----------|
| String (255) | VARCHAR(255) | VARCHAR(255) |
| String (large) | TEXT | TEXT |
| UUID | uuid | UUID |
| Instant | timestamp with time zone | TIMESTAMP WITH TIME ZONE |
| Boolean | boolean | BOOLEAN |
| Integer | integer | INT |
| Long | bigint | BIGINT |

### Full Guide

See `backend/docs/ENTITY_MIGRATION_GUIDE.md` for comprehensive documentation including:
- Complete type mapping reference
- Entity annotation best practices
- Migration file templates
- Troubleshooting guide
- Phase-by-phase implementation status

---

## Database Architecture & Schema Management

### Overview

HDIM uses the **Database-per-Service** pattern with **Liquibase** for all schema migrations. Each of the 29 microservices has its own logical database on a shared PostgreSQL instance, ensuring service isolation and independent schema evolution.

**Key Principles:**
- ✅ One database per service (29 databases total)
- ✅ Liquibase for ALL services (standard tool)
- ✅ `ddl-auto: validate` in all environments
- ✅ Entity-migration synchronization enforced
- ❌ Never use `ddl-auto: create` or `update` (causes data loss/drift)

### Database Inventory

**PostgreSQL Version:** 16-alpine
**Total Databases:** 29 (see `DATABASE_ARCHITECTURE_MIGRATION_PLAN.md` for complete list)

**Core Databases:**
- `fhir_db` - FHIR R4 resources (fhir-service:8085)
- `patient_db` - Patient demographics (patient-service:8084)
- `quality_db` - HEDIS measures (quality-measure-service:8087)
- `cql_db` - CQL evaluation (cql-engine-service:8081)
- `caregap_db` - Care gap detection (care-gap-service:8086)
- `gateway_db` - Authentication (gateway-*-service:8080)
- ... (23 more, see migration plan)

### Migration Standards

#### **Use Liquibase Only**

All services MUST use Liquibase for database migrations. Flyway is NOT supported.

```kotlin
// build.gradle.kts - Liquibase included via shared persistence module
dependencies {
    implementation(project(":modules:shared:infrastructure:persistence"))
    // This includes: Liquibase 4.29.2, PostgreSQL driver, HikariCP
}
```

```yaml
# docker-compose.yml or application.yml
spring:
  liquibase:
    enabled: true  # MUST be true
    change-log: classpath:db/changelog/db.changelog-master.xml
  jpa:
    hibernate:
      ddl-auto: validate  # MUST be validate (never create/update)
```

#### **Migration File Structure**

```
src/main/resources/db/changelog/
├── 0000-enable-extensions.xml           # PostgreSQL extensions (pg_trgm, etc.)
├── 0001-create-patients-table.xml       # Initial schema
├── 0002-create-insurance-table.xml      # Related tables
├── 0003-add-composite-indexes.xml       # Performance indexes
├── 0004-add-risk-score-column.xml       # Schema evolution
└── db.changelog-master.xml               # Includes all migrations
```

**Naming Convention:**
- Use 4-digit sequential numbers: `0001`, `0002`, `0003`
- Use descriptive names: `create-TABLE-table`, `add-FIELD-to-TABLE`
- Never reuse numbers or modify existing migrations
- Never skip numbers (no gaps in sequence)

#### **Master Changelog Template**

```xml
<!-- db.changelog-master.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <include file="db/changelog/0000-enable-extensions.xml"/>
    <include file="db/changelog/0001-create-patients-table.xml"/>
    <include file="db/changelog/0002-create-insurance-table.xml"/>
    <!-- Add new migrations here, never modify existing includes -->
</databaseChangeLog>
```

#### **Migration File Template**

```xml
<!-- 0001-create-patients-table.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="0001-create-patients-table" author="developer-name">
        <comment>Create patients table with tenant isolation</comment>

        <createTable tableName="patients">
            <column name="id" type="UUID" defaultValueComputed="gen_random_uuid()">
                <constraints primaryKey="true" primaryKeyName="pk_patients"/>
            </column>
            <column name="tenant_id" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="first_name" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="last_name" type="VARCHAR(100)">
                <constraints nullable="false"/>
            </column>
            <column name="date_of_birth" type="DATE">
                <constraints nullable="false"/>
            </column>
            <column name="created_at" type="TIMESTAMP WITH TIME ZONE"
                    defaultValueComputed="CURRENT_TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="updated_at" type="TIMESTAMP WITH TIME ZONE"
                    defaultValueComputed="CURRENT_TIMESTAMP">
                <constraints nullable="false"/>
            </column>
        </createTable>

        <createIndex indexName="idx_patients_tenant_id" tableName="patients">
            <column name="tenant_id"/>
        </createIndex>

        <!-- ALWAYS provide explicit rollback -->
        <rollback>
            <dropTable tableName="patients"/>
        </rollback>
    </changeSet>
</databaseChangeLog>
```

### Rollback SQL Coverage

**Status:** ✅ 100% coverage achieved (199/199 changesets)

Every Liquibase changeset in the HDIM platform includes explicit rollback SQL, ensuring safe reversion of database changes in production.

**Validation:**
- Automated via `backend/scripts/test-liquibase-rollback.sh`
- Enforced in CI/CD workflow on every PR
- Comprehensive patterns documented in `backend/docs/DATABASE_MIGRATION_RUNBOOK.md`

**Common Rollback Patterns:**

| Operation | Rollback |
|-----------|----------|
| Create table | `<dropTable tableName="..."/>` |
| Add column | `<dropColumn tableName="..." columnName="..."/>` |
| Insert data | `<delete tableName="..."><where>...</where></delete>` |
| Update data | Reverse update with original values |
| Create index | `<dropIndex tableName="..." indexName="..."/>` |
| Add comments | `COMMENT ON ... IS NULL` |
| ANALYZE | Empty rollback (statistics only) |

**Why 100% Coverage Matters:**
- Enables safe production rollbacks without data loss
- Validates migration reversibility during development
- Reduces deployment risk for database changes
- Provides disaster recovery capability

### PostgreSQL Extensions

Extensions should be managed in Liquibase migrations, not initialization scripts.

```xml
<!-- 0000-enable-extensions.xml -->
<changeSet id="0000-enable-extensions" author="hdim-platform-team">
    <comment>Enable PostgreSQL extensions for full-text search</comment>
    <sql>CREATE EXTENSION IF NOT EXISTS pg_trgm;</sql>
    <rollback>DROP EXTENSION IF EXISTS pg_trgm;</rollback>
</changeSet>
```

**Common Extensions:**
- `pg_trgm` - Trigram matching for fuzzy text search (used by fhir, cql, quality, patient services)
- `uuid-ossp` - UUID generation (optional, prefer `gen_random_uuid()`)

### Database Initialization

The `docker/postgres/init-multi-db.sh` script creates all 29 databases on PostgreSQL startup:

```bash
# Creates databases only - NO tables, NO extensions
CREATE DATABASE fhir_db;
CREATE DATABASE patient_db;
# ... (all 29 databases)

GRANT ALL PRIVILEGES ON DATABASE fhir_db TO healthdata;
# ... (all grants)
```

**What init script does:**
- ✅ Creates empty databases
- ✅ Grants privileges to database user
- ❌ Does NOT create tables (Liquibase does this)
- ❌ Does NOT create extensions (Liquibase does this)

### Migration Workflow

#### **When Creating a New Entity**

1. Create JPA entity with proper annotations:
```java
@Entity
@Table(name = "appointments")
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    // ... more fields
}
```

2. Create Liquibase migration file:
```xml
<!-- 0005-create-appointments-table.xml -->
<changeSet id="0005-create-appointments-table" author="your-name">
    <createTable tableName="appointments">
        <column name="id" type="UUID" defaultValueComputed="gen_random_uuid()">
            <constraints primaryKey="true"/>
        </column>
        <column name="tenant_id" type="VARCHAR(100)">
            <constraints nullable="false"/>
        </column>
        <column name="appointment_date" type="DATE">
            <constraints nullable="false"/>
        </column>
    </createTable>
    <rollback>
        <dropTable tableName="appointments"/>
    </rollback>
</changeSet>
```

3. Add migration to master changelog:
```xml
<!-- db.changelog-master.xml -->
<include file="db/changelog/0005-create-appointments-table.xml"/>
```

4. Run validation test:
```bash
./gradlew :modules:services:YOUR-SERVICE:test --tests "*EntityMigrationValidationTest"
```

5. Verify migration runs successfully:
```bash
docker compose up YOUR-SERVICE
# Check logs for: "Liquibase update successful"
```

#### **When Modifying an Entity**

**NEVER modify existing migrations!** Always create a NEW migration.

```java
// Add new field to entity
@Entity
@Table(name = "appointments")
public class Appointment {
    // ... existing fields

    @Column(name = "status")  // NEW FIELD
    private String status;
}
```

```xml
<!-- 0006-add-status-to-appointments.xml -->
<changeSet id="0006-add-status-to-appointments" author="your-name">
    <comment>Add status field for appointment tracking</comment>
    <addColumn tableName="appointments">
        <column name="status" type="VARCHAR(50)" defaultValue="SCHEDULED">
            <constraints nullable="true"/>  <!-- Allow null for existing rows -->
        </column>
    </addColumn>
    <rollback>
        <dropColumn tableName="appointments" columnName="status"/>
    </rollback>
</changeSet>
```

### Common Liquibase Operations

**Create Table:**
```xml
<createTable tableName="table_name">
    <column name="id" type="UUID"/>
    <!-- more columns -->
</createTable>
```

**Add Column:**
```xml
<addColumn tableName="table_name">
    <column name="new_column" type="VARCHAR(255)"/>
</addColumn>
```

**Create Index:**
```xml
<createIndex indexName="idx_table_column" tableName="table_name">
    <column name="column_name"/>
</createIndex>
```

**Add Foreign Key:**
```xml
<addForeignKeyConstraint
    constraintName="fk_appointments_patient"
    baseTableName="appointments"
    baseColumnNames="patient_id"
    referencedTableName="patients"
    referencedColumnNames="id"/>
```

**Modify Column:**
```xml
<modifyDataType tableName="table_name" columnName="column_name" newDataType="TEXT"/>
```

**Run Custom SQL:**
```xml
<sql>
    UPDATE patients SET status = 'ACTIVE' WHERE created_at > NOW() - INTERVAL '30 days';
</sql>
```

### Troubleshooting

#### **Service Won't Start - Validation Failure**

**Error:**
```
Schema-validation: missing table [appointments]
```

**Fix:** Create Liquibase migration for the missing table or remove unused @Entity

#### **Service Won't Start - Wrong Column Type**

**Error:**
```
Schema-validation: wrong column type encountered in column [appointment_date]
Expected: date, Actual: timestamp with time zone
```

**Fix:** Create migration to alter column type:
```xml
<modifyDataType tableName="appointments" columnName="appointment_date" newDataType="DATE"/>
```

#### **Migration Failed - Already Exists**

**Error:**
```
Liquibase: relation "patients" already exists
```

**Fix:**
1. Check `databasechangelog` table to see what ran:
```bash
docker exec healthdata-postgres psql -U healthdata -d SERVICE_db \
  -c "SELECT id, filename FROM databasechangelog ORDER BY orderexecuted;"
```

2. If migration already ran, remove it from master changelog
3. If table was created manually, create baseline migration with `<preConditions>`

#### **Need to Rollback Migration**

```bash
# Rollback last changeset
docker exec healthdata-postgres psql -U healthdata -d SERVICE_db \
  -c "DELETE FROM databasechangelog WHERE orderexecuted = (SELECT MAX(orderexecuted) FROM databasechangelog);"

# Manually rollback changes (Liquibase rollback command not yet integrated)
docker exec healthdata-postgres psql -U healthdata -d SERVICE_db \
  -c "DROP TABLE appointments;"  # Example rollback
```

### Migration Plan Reference

For complete details on the database architecture standardization effort:

**See:** `DATABASE_ARCHITECTURE_MIGRATION_PLAN.md`

**Current Status:** Phase 1-4 Complete (as of 2026-01-10)
- ✅ Phase 1: Fixed critical ddl-auto issues
- ✅ Phase 2: Migrated Flyway services to Liquibase
- ✅ Phase 3: Moved gateway auth tables to Liquibase
- ✅ Phase 4: Service-owned extension management
- 🔄 Phase 5: CI/CD enforcement (in progress)

**Key Achievements:**
- PostgreSQL 16 running with 29 databases
- Init script simplified to database creation only
- All schema management moved to service Liquibase migrations
- Gateway authentication schema version-controlled
- PostgreSQL extensions managed by services

### CI/CD Validation

**Entity-migration validation is enforced in CI/CD:**

**GitHub Actions:** Automated validation on PRs
```yaml
# .github/workflows/database-validation.yml
# Runs entity-migration tests when JPA entities or migrations change
```

**Pre-commit Hook:** Local validation before committing
```bash
# Install hook
ln -s ../../backend/scripts/pre-commit-db-validation.sh .git/hooks/pre-commit

# Validates entities when modified
./backend/scripts/pre-commit-db-validation.sh
```

**Manual Validation:**
```bash
cd backend
./gradlew test --tests "*EntityMigrationValidationTest"
```

---

## Getting Help

- **Database connection pooling**: See `backend/modules/shared/infrastructure/database-config/README.md` ⭐ **NEW**
- **Database-config adoption guide**: See `backend/docs/DATABASE_CONFIG_ADOPTION_GUIDE.md` ⭐ **NEW**
- **Database-config pilot validation**: See `backend/docs/DATABASE_CONFIG_PILOT_VALIDATION.md` ⭐ **NEW**
- **Distributed tracing**: See `backend/docs/DISTRIBUTED_TRACING_GUIDE.md`
- **Database migration runbook**: See `backend/docs/DATABASE_MIGRATION_RUNBOOK.md`
- **Database architecture**: See `DATABASE_ARCHITECTURE_MIGRATION_PLAN.md`
- **Database migration status**: See `backend/docs/DATABASE_MIGRATION_STATUS.md`
- **Entity-migration guide**: See `backend/docs/ENTITY_MIGRATION_GUIDE.md`
- **System architecture**: See `docs/architecture/SYSTEM_ARCHITECTURE.md`
- **Technology decisions**: See `docs/architecture/decisions/` (ADRs)
- **Terminology**: See `docs/TERMINOLOGY_GLOSSARY.md`
- **API design**: See `BACKEND_API_SPECIFICATION.md`
- **Security/HIPAA**: See `docs/PRODUCTION_SECURITY_GUIDE.md`
- **Authentication**: See `backend/docs/GATEWAY_TRUST_ARCHITECTURE.md`
- **Deployment**: See `docs/DEPLOYMENT_RUNBOOK.md`

---

*Last Updated: January 12, 2026*
*Version: 1.6* - Database-config module implemented with 3 pilot service migrations (11% complete)
