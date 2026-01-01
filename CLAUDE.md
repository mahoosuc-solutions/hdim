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
- **Database**: PostgreSQL 15
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

## Getting Help

- **System architecture**: See `docs/architecture/SYSTEM_ARCHITECTURE.md`
- **Technology decisions**: See `docs/architecture/decisions/` (ADRs)
- **Terminology**: See `docs/TERMINOLOGY_GLOSSARY.md`
- **API design**: See `BACKEND_API_SPECIFICATION.md`
- **Security/HIPAA**: See `docs/PRODUCTION_SECURITY_GUIDE.md`
- **Authentication**: See `backend/docs/GATEWAY_TRUST_ARCHITECTURE.md`
- **Deployment**: See `docs/DEPLOYMENT_RUNBOOK.md`

---

*Last Updated: January 1, 2026*
*Version: 1.2* - Added Entity-Migration Synchronization section with validation framework
