# CLAUDE.md - HDIM Development Quick Reference

## Project Overview

**HealthData-in-Motion (HDIM)** is an enterprise healthcare interoperability platform for HEDIS quality measure evaluation, FHIR R4 compliance, and clinical decision support.

**Purpose:** Enable healthcare organizations to evaluate clinical quality measures (CQL/HEDIS), identify care gaps, perform risk stratification, and generate quality reports for value-based care contracts.

**Target Users:** Healthcare payers, ACOs, health systems, and clinical quality teams.

---

## 📚 Documentation Navigation

### Quick Start (5-10 minutes)
- **[Project Overview](./docs/README.md)** - What is HDIM?
- **[Quick Start Docker](./README.md#-quick-start-docker)** - Deploy in 3 minutes
- **[Service Catalog](./docs/services/SERVICE_CATALOG.md)** - All 50+ services & ports

### Common Tasks
- **Building & Deployment:** [Build Management Guide](./backend/docs/BUILD_MANAGEMENT_GUIDE.md)
- **Database Work:** [Database Architecture Guide](./backend/docs/DATABASE_ARCHITECTURE_GUIDE.md)
- **Writing Code:** [Coding Standards](./backend/docs/CODING_STANDARDS.md)
- **Running Commands:** [Command Reference](./backend/docs/COMMAND_REFERENCE.md)
- **API Documentation:** [OpenAPI/Swagger Guide](./docs/Q1_2026_API_DOCUMENTATION_PHASE_1A_COMPLETE.md) ✨ NEW - Interactive API docs

### Deep Dive Guides (Comprehensive)
- **[Liquibase Workflow](./backend/docs/LIQUIBASE_DEVELOPMENT_WORKFLOW.md)** ⭐ CRITICAL - Database migrations
- **[Event Sourcing Architecture](./docs/architecture/EVENT_SOURCING_ARCHITECTURE.md)** ✨ NEW - CQRS pattern, event services, projections
- **[Gateway Architecture](./docs/architecture/GATEWAY_ARCHITECTURE.md)** ✨ NEW - Modularized 4-gateway design, authentication flows
- **[Gateway Trust Authentication](./backend/docs/GATEWAY_TRUST_ARCHITECTURE.md)** - Gateway-service trust pattern
- **[Distributed Tracing](./backend/docs/DISTRIBUTED_TRACING_GUIDE.md)** - OpenTelemetry observability
- **[HIPAA Compliance](./backend/HIPAA-CACHE-COMPLIANCE.md)** - PHI handling requirements
- **[All Backend Guides](./backend/docs/README.md)** - Complete technical documentation index

### Reference Portals
- **[Main Documentation Portal](./docs/README.md)** - Central hub for 1,411+ docs with role-based navigation
- **[Troubleshooting Guide](./docs/troubleshooting/README.md)** - Problem resolution decision trees

---

## 🤖 Claude Code Plugins

**31 plugins installed** for enhanced HDIM development workflow.

### Quick Commands

```bash
/commit                    # AI-generated commit messages
/feature-dev               # Guided feature development
/review-pr [PR_NUMBER]     # Comprehensive PR review
/hookify                   # Create custom automation hooks
/help                      # List all available commands
```

### Key Plugins for HDIM

| Plugin | Purpose | Priority |
|--------|---------|----------|
| `jdtls-lsp` | Java IntelliSense (requires JDT.LS) | ⭐ Critical |
| `security-guidance` | HIPAA compliance scanning | ⭐ Critical |
| `commit-commands` | Enhanced git workflow | ⭐ Critical |
| `feature-dev` | Guided feature development | ⭐ Critical |
| `pr-review-toolkit` | Multi-agent PR review | ⭐ Critical |
| `code-review` | Automated code review | High |
| `typescript-lsp` | TypeScript support (Angular) | Medium |
| `hookify` | Custom automation hooks | Medium |

### Documentation

- **[Complete Plugin Reference](./docs/CLAUDE_CODE_PLUGINS.md)** - All 31 plugins with usage examples
- **[Activation Checklist](./docs/PLUGIN_ACTIVATION_CHECKLIST.md)** - Step-by-step setup guide
- **[Installation Status](./docs/PLUGIN_ACTIVATION_STATUS.md)** - Current installation state

### Setup Requirements

**JDT.LS (Java Language Server):**
```bash
# Install for Java IntelliSense
brew install jdtls  # macOS

# Or use automated script for WSL2/Ubuntu
./scripts/install-jdtls-wsl.sh
```

**Verify installation:**
```bash
which jdtls
jdtls --version
```

### Recommended Hooks for HDIM

Use `/hookify` to create custom automation:

1. **Prevent direct schema changes** - Enforce Liquibase migrations
2. **Auto-validate entity-migrations** - Run validation before Docker builds
3. **HIPAA compliance check** - Scan for PHI in logs/cache configurations

---

## Tech Stack

### Backend
- **Language:** Java 21 (LTS)
- **Framework:** Spring Boot 3.x
- **Build:** Gradle 8.11+ (Kotlin DSL)
- **FHIR:** HAPI FHIR 7.x (R4)
- **Database:** PostgreSQL 16
- **Cache:** Redis 7
- **Messaging:** Apache Kafka 3.x
- **API Gateway:** Kong
- **Security:** Spring Security + JWT

### Frontend
- **Framework:** Angular 17+
- **State:** RxJS
- **UI:** Angular Material

### Infrastructure
- **Containers:** Docker + Docker Compose
- **Orchestration:** Kubernetes (optional)
- **Monitoring:** Prometheus + Grafana
- **Secrets:** HashiCorp Vault

---

## Service Ports

| Service | Port | Purpose |
|---------|------|---------|
| Gateway | 8001 | API entry point |
| CQL Engine | 8081 | CQL evaluation |
| Patient Service | 8084 | Patient data |
| FHIR Service | 8085 | FHIR R4 resources |
| Care Gap Service | 8086 | Care gap detection |
| Quality Measure | 8087 | HEDIS measures |
| PostgreSQL | 5435 | Primary database |
| Redis | 6380 | Cache layer |
| Kafka | 9094 | Message broker |
| Prometheus | 9090 | Metrics collection |
| Grafana | 3001 | Dashboard UI |

[Complete Service List →](./docs/services/SERVICE_CATALOG.md)

---

## ⚠️ Critical: HIPAA Compliance Requirements

**This application handles Protected Health Information (PHI). All code MUST comply with HIPAA regulations.**

### Essential Rules

```java
// Cache TTL MUST be ≤ 5 minutes for PHI
@Cacheable(value = "patientData", key = "#patientId")

// All PHI responses MUST include no-cache headers
response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");

// Use @Audited annotation on all PHI access methods
@Audited(eventType = "PHI_ACCESS")
public Patient getPatient(String patientId) { ... }

// All queries MUST filter by tenant
@Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId AND p.id = :id")
Optional<Patient> findByIdAndTenant(@Param("id") String id, @Param("tenantId") String tenantId);
```

**Must Read Before Development:** [HIPAA Compliance Guide](./backend/HIPAA-CACHE-COMPLIANCE.md)

---

## 🎨 Frontend Development & HIPAA Compliance

### Angular Clinical Portal (CRITICAL)

The Clinical Portal handles sensitive PHI and MUST follow HIPAA-compliant coding practices.

#### ✅ Production-Ready Infrastructure (January 2026)

**HIPAA §164.312(b) Compliance - Audit Controls:**
- ✅ **HTTP Audit Interceptor** - Automatic logging of ALL API calls (100% coverage)
  - Tracks patient access, care gaps, evaluations, reports, FHIR resources
  - Resource type extraction, duration tracking, success/failure outcomes
  - Fire-and-forget batching, offline resilience
- ✅ **Session Timeout Audit Logging** - HIPAA §164.312(a)(2)(iii) compliant (PR #294)
  - Logs all session timeouts (automatic idle timeout + explicit logout)
  - Captures idle duration, timeout reason, warning status
  - Differentiates automatic vs explicit logout
  - Full audit trail for compliance verification
- ✅ **Global Error Handler** - Prevents application crashes, logs security incidents
  - Catches all unhandled exceptions
  - PHI filtering via LoggerService
  - Audit trail for security events
- ✅ **ESLint no-console enforcement** - Prevents PHI exposure via browser console
  - Build fails if console statements detected
  - Enforces LoggerService usage

**Implementation Status:** [UI Validation Implementation Summary](./docs/UI_VALIDATION_IMPLEMENTATION_SUMMARY.md)

---

#### Mandatory Coding Patterns (Angular)

**1. Logging - Use LoggerService, NEVER console.log**

```typescript
import { LoggerService } from '../../services/logger.service';

export class PatientComponent {
  private logger = this.loggerService.withContext('PatientComponent');

  constructor(private loggerService: LoggerService) {}

  loadPatient(patientId: string): void {
    this.logger.info('Loading patient', patientId);  // ✅ CORRECT
    // console.log('Loading patient:', patientId);   // ❌ HIPAA VIOLATION
  }

  handleError(error: Error): void {
    this.logger.error('Failed to load patient', error);  // ✅ CORRECT
    // console.error('Error:', error);                    // ❌ HIPAA VIOLATION
  }
}
```

**Why:** LoggerService automatically filters PHI in production. Console.log exposes PHI to browser DevTools.

**ESLint Enforcement:** Build fails if console statements detected.

---

**2. Audit Logging - Automatic via HTTP Interceptor**

```typescript
// NO CODE REQUIRED - Audit interceptor logs automatically

// Example: Patient access
this.http.get(`/patient/${patientId}`).subscribe(...);
// → Automatically logged: { action: 'READ', resourceType: 'Patient', resourceId: '123', ... }

// Example: Care gap closure
this.http.post(`/care-gap/${gapId}/close`, data).subscribe(...);
// → Automatically logged: { action: 'UPDATE', resourceType: 'CareGap', resourceId: '456', ... }
```

**Coverage:** 100% of backend API calls automatically audited.

**Optional - Explicit Business Logic Audit:**
```typescript
import { AuditService } from '../../services/audit.service';

export class CareGapService {
  constructor(private auditService: AuditService) {}

  closeCareGap(gapId: string, closureData: any): Observable<void> {
    this.auditService.logCareGapAction({
      gapId,
      patientId: closureData.patientId,
      action: 'address',
      success: true,
    });
    return this.http.post(`/care-gap/${gapId}/close`, closureData);
  }
}
```

---

**3. Error Handling - Global Error Handler Active**

```typescript
// Application-level errors automatically caught and logged
// NO CODE REQUIRED - Global error handler is active

// Example: Uncaught exception
throw new Error('Unexpected error');
// → Automatically logged, audited, user shown friendly message, app does NOT crash
```

**Manual Error Handling (Optional):**
```typescript
this.patientService.getPatient(patientId).subscribe({
  next: (patient) => this.patient = patient,
  error: (err) => {
    this.logger.error('Failed to load patient', err);  // Logged with PHI filtering
    this.showError('Unable to load patient data');     // User-friendly message
  }
});
```

---

**4. Session Timeout - HIPAA Compliant (Complete) ✅**

```typescript
// Session timeout active in app.ts:
// - 15-minute idle timeout with HIPAA §164.312(a)(2)(iii) compliance
// - 2-minute warning before logout
// - Activity listeners (click, keypress, mousemove, scroll)
// - "Stay Logged In" button
// - ✅ Audit logging on timeout and logout (PR #294)

// Audit logging implementation:
private onSessionTimeout(): void {
  this.auditService.logSessionTimeout({
    reason: 'IDLE_TIMEOUT',
    idleDurationMinutes: 15,
    warningShown: true,
  });
  this.authService.logout();
}

logout(): void {
  this.auditService.logSessionTimeout({
    reason: 'EXPLICIT_LOGOUT',
    warningShown: false,
  });
  this.authService.logout();
}
```

**HIPAA Compliance:** Fully compliant with §164.312(a)(2)(iii) - Automatic Logoff requirement. All session timeouts are audited with user ID, timestamp, reason, and idle duration.

---

**5. Accessibility (WCAG 2.1 Level A) - In Progress**

**Current Status:** 343 ARIA attributes across 53 files (50% coverage)

**Pending Items:**
- Skip-to-content link
- ARIA labels on table action buttons
- Focus indicators for keyboard navigation

**Pattern for Table Actions:**
```html
<!-- BEFORE -->
<button mat-icon-button (click)="viewPatient(patient)">
  <mat-icon>visibility</mat-icon>
</button>

<!-- AFTER -->
<button mat-icon-button
        (click)="viewPatient(patient)"
        [attr.aria-label]="'View patient ' + patient.name">
  <mat-icon aria-hidden="true">visibility</mat-icon>
</button>
```

---

### Frontend Code Review Checklist

Before committing Angular code:

- [ ] **NO console.log/error/warn/debug** - Use LoggerService
- [ ] **HTTP calls automatically audited** - Verify interceptor is registered
- [ ] **Errors handled gracefully** - Use try/catch or RxJS catchError
- [ ] **ARIA attributes on interactive elements** - Buttons, links, form fields
- [ ] **No hardcoded PHI in templates** - Use data binding, never literal patient data
- [ ] **Session timeout respected** - No operations that bypass idle detection

**Verification:**
```bash
# Verify no console statements
npm run lint

# Verify production build
npm run build:prod
grep -r 'console\.' dist/  # Should return nothing
```

---

## Build & Deployment

### Building Services

**Golden Rule:** Build ONE service at a time to avoid system overload.

```bash
# Pre-cache dependencies locally first (prevents Docker TLS issues)
cd backend
./gradlew downloadDependencies --no-daemon

# Build single service
./gradlew :modules:services:SERVICENAME:bootJar -x test

# Build Docker image
docker compose build SERVICENAME

# Start and verify
docker compose up -d SERVICENAME
docker compose logs -f SERVICENAME | head -50
```

**Troubleshooting:** [Build Management Guide](./backend/docs/BUILD_MANAGEMENT_GUIDE.md)

### Docker Commands

```bash
# Start all services
docker compose up -d

# View logs
docker compose logs -f SERVICE

# Check health
docker compose ps

# Stop all
docker compose down
```

[All Docker Commands →](./backend/docs/COMMAND_REFERENCE.md#docker-compose-commands)

---

## Database Schema Management

### Key Principles

- ✅ Use **Liquibase for ALL migrations** (never Flyway)
- ✅ Set `ddl-auto: validate` in all environments
- ✅ Always include rollback directives in migrations
- ❌ Never use `ddl-auto: create` or `update` (causes data drift)

### Quick Entity Migration

```java
// 1. Create entity with @Entity, @Table, @Column
@Entity
@Table(name = "appointments")
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
}

// 2. Create migration file (000N-create-appointments-table.xml)
// 3. Add to db.changelog-master.xml
// 4. Run: ./gradlew test --tests "*EntityMigrationValidationTest"
// 5. Verify: docker compose up SERVICE
```

[Complete Guide →](./backend/docs/DATABASE_ARCHITECTURE_GUIDE.md)

---

## Coding Patterns

### Service Layer (Business Logic)

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientService {
    private final PatientRepository patientRepository;

    // READ operations (no transaction)
    public PatientResponse getPatient(String patientId, String tenantId) {
        return patientRepository.findByIdAndTenant(patientId, tenantId)
            .map(this::mapToResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
    }

    // WRITE operations (@Transactional required)
    @Transactional
    public PatientResponse createPatient(CreatePatientRequest request, String tenantId) {
        Patient patient = patientRepository.save(toEntity(request, tenantId));
        return mapToResponse(patient);
    }
}
```

### Controller Layer (REST API)

```java
@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {
    private final PatientService patientService;

    @GetMapping("/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    @Audited(eventType = "PATIENT_ACCESS")
    public ResponseEntity<PatientResponse> getPatient(
            @PathVariable String patientId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(patientService.getPatient(patientId, tenantId));
    }
}
```

### Entity Pattern

```java
@Entity
@Table(name = "patients")
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
```

[Complete Patterns →](./backend/docs/CODING_STANDARDS.md)

---

## Authentication & Authorization

### Architecture

```
Client → Gateway (validates JWT) → Service (trusts X-Auth-* headers)
```

The gateway validates JWT tokens and injects trusted headers. Services trust these headers.

### Key Components

| Component | Purpose |
|-----------|---------|
| `TrustedHeaderAuthFilter` | Validates gateway headers |
| `TrustedTenantAccessFilter` | Validates tenant access |
| `GatewayAuthenticationFilter` | Validates JWT in gateway |

### Role Hierarchy

| Role | Access |
|------|--------|
| SUPER_ADMIN | Full system access |
| ADMIN | Tenant-level admin |
| EVALUATOR | Run evaluations, view results |
| ANALYST | View reports |
| VIEWER | Read-only |

### Securing Endpoints

```java
@PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
public ResponseEntity<PatientResponse> getPatient(...) { ... }
```

[Complete Auth Guide →](./backend/docs/GATEWAY_TRUST_ARCHITECTURE.md)

---

## Common Commands

### Gradle (Backend Directory)

```bash
# Build specific service
./gradlew :modules:services:SERVICENAME:build

# Run tests
./gradlew test

# Run specific test
./gradlew test --tests PatientServiceTest.shouldCreatePatient

# Download dependencies
./gradlew downloadDependencies --no-daemon
```

[All Gradle Commands →](./backend/docs/COMMAND_REFERENCE.md#gradle-commands-backend)

### Docker Compose

```bash
# Start all services
docker compose up -d

# View specific logs
docker compose logs -f patient-event-service

# Execute command in container
docker compose exec patient-event-service sh
```

[All Docker Commands →](./backend/docs/COMMAND_REFERENCE.md#docker-compose-commands)

### PostgreSQL

```bash
# Connect to database
docker exec -it hdim-postgres psql -U healthdata -d patient_db

# List tables
docker exec -it hdim-postgres psql -U healthdata -d patient_db -c "\dt"
```

[All Database Commands →](./backend/docs/COMMAND_REFERENCE.md#postgresql-commands)

---

## Testing Requirements

### ⚡ Test Classification & Selective Execution (Phase 3)

**NEW:** Run only the tests you need with tagged test categories.

```bash
# Unit tests only (fast feedback - ~30-60 seconds)
./gradlew testUnit

# Integration tests only (~3-5 minutes)
./gradlew testIntegration

# All tests - comprehensive validation (~4-6 minutes)
./gradlew testAll
./gradlew test  # Backward compatible
```

**Why this matters:**
- ⚡ **30-60 second feedback loop** during development (vs 4-6 minutes for all tests)
- 🎯 **Selective testing** based on what changed
- 📚 **Clear organization** - every test is classified
- 🚀 **Faster commits** - validate logic before pushing

**Complete Guide:** [TEST_CLASSIFICATION_GUIDE.md](./backend/docs/TEST_CLASSIFICATION_GUIDE.md)

---

### Unit Tests

```java
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class PatientServiceTest {
    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientService patientService;

    @Test
    void shouldReturnPatient_WhenExists() {
        when(patientRepository.findByIdAndTenant("123", "tenant1"))
            .thenReturn(Optional.of(testPatient));

        PatientResponse result = patientService.getPatient("123", "tenant1");

        assertThat(result.getId()).isEqualTo("123");
    }
}
```

### Integration Tests

```java
@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
class PatientControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnPatient() throws Exception {
        mockMvc.perform(get("/api/v1/patients/123")
                .header("X-Tenant-ID", "tenant1"))
            .andExpect(status().isOk());
    }
}
```

### Test Coverage Requirements
- ✅ Unit tests for all service methods
- ✅ Integration tests for API endpoints
- ✅ Multi-tenant isolation tests
- ✅ RBAC permission tests

---

## API Documentation (OpenAPI/Swagger) ✨ NEW

HDIM provides **interactive API documentation** via OpenAPI 3.0 and Swagger UI for all REST endpoints.

### Phase 1A Complete (January 2026)

**62 production-ready endpoints** documented across 4 services with comprehensive annotations, examples, and clinical context.

| Service | Endpoints | Swagger UI | Status |
|---------|-----------|------------|--------|
| **Patient Service** | 19 | http://localhost:8084/patient/swagger-ui/index.html | ✅ 100% |
| **Care Gap Service** | 17 | http://localhost:8086/care-gap/swagger-ui/index.html | ✅ 100% |
| **FHIR Service** | 26 | http://localhost:8085/fhir/swagger-ui/index.html | ✅ Core |
| **Quality Measure** | 5 | http://localhost:8087/quality-measure/swagger-ui/index.html | ⏳ Partial |

### Key Features

**Every documented endpoint includes:**
- ✅ Interactive testing via Swagger UI
- ✅ JWT Bearer authentication integration
- ✅ Request/response examples with FHIR R4 format
- ✅ Clinical use cases (HEDIS, quality measures, care gaps)
- ✅ Multi-tenancy patterns (X-Tenant-ID header)
- ✅ HIPAA compliance notes
- ✅ Error response scenarios

### Quick Start

**View API Documentation:**
```bash
# Start service
docker compose up -d patient-service

# Open Swagger UI in browser
http://localhost:8084/patient/swagger-ui/index.html

# Or download OpenAPI spec
curl http://localhost:8084/patient/v3/api-docs > patient-api.json
```

**Test API Endpoints:**
1. Click "Authorize" in Swagger UI
2. Enter JWT token: `Bearer {your-token}`
3. Select endpoint (e.g., GET /patient/health-record)
4. Enter required parameters
5. Click "Execute" to test

### Documentation Reference

- **[Phase 1A Complete Summary](./docs/Q1_2026_API_DOCUMENTATION_PHASE_1A_COMPLETE.md)** - Complete milestone documentation
- **[Pattern Guide](./docs/API_DOCUMENTATION_PATTERNS.md)** - Reusable OpenAPI annotation patterns
- **[Service Reports](./docs/)** - Individual service completion summaries

### Business Value

- **Developer Onboarding:** Reduced from days to hours
- **Self-Service Discovery:** No code reading required
- **Integration Testing:** Complete API contracts with examples
- **Compliance:** HIPAA patterns explicitly documented

---

## Distributed Tracing

HDIM uses **OpenTelemetry** for end-to-end request visibility across all microservices.

### Automatic Trace Propagation

| Transport | Status |
|-----------|--------|
| HTTP (Feign) | ✅ Auto-enabled |
| HTTP (RestTemplate) | ✅ Auto-enabled |
| Kafka Producer | ✅ Configured |
| Kafka Consumer | ✅ Configured |

### Custom Spans

```java
@Service
public class QualityMeasureService {
    private final Tracer tracer;

    public EvaluationResult evaluateMeasure(String measureId, String patientId) {
        Span span = tracer.spanBuilder("evaluate_measure")
            .setAttribute("measure.id", measureId)
            .startSpan();

        try (Scope scope = span.makeCurrent()) {
            return performEvaluation(measureId, patientId);
        }
    }
}
```

[Complete Guide →](./backend/docs/DISTRIBUTED_TRACING_GUIDE.md)

---

## Entity-Migration Synchronization (CRITICAL)

**This practice prevents production schema drift issues (like database validation failures at runtime).**

HDIM uses **Liquibase** for all database migrations with **Hibernate validation** to ensure JPA entities match the actual database schema. Mismatches cause runtime failures and production outages.

### Quick Checklist

When **creating** a new entity:

- [ ] Create JPA entity with `@Entity`, `@Table`, `@Column` annotations
- [ ] Create Liquibase migration file (`NNNN-create-table.xml`)
- [ ] Add migration include to `db.changelog-master.xml`
- [ ] Run validation test: `./gradlew test --tests "*EntityMigrationValidationTest"`

When **modifying** an entity:

- [ ] Update `@Column` annotations
- [ ] Create NEW migration (never modify existing migrations)
- [ ] Use descriptive migration ID: `NNNN-add-field-to-table.xml`
- [ ] Run validation test before committing

### How Validation Works

**Entity-migration validation tests run in every service:**

1. Enable Liquibase: Run actual database migrations
2. Set Hibernate to `validate` mode: Compare entity definitions to actual schema
3. Detect mismatches: Test fails if entity columns don't match migrated schema
4. Fail fast: Catch schema drift at test time, not at runtime

```bash
# Run validation locally
./gradlew test --tests "*EntityMigrationValidationTest"

# Run specific service
./gradlew :modules:services:SERVICENAME:test --tests "*EntityMigrationValidationTest"
```

### Pre-Build Validation

Validate before building to catch issues early:

```bash
# Comprehensive pre-Docker validation
./scripts/validate-before-docker-build.sh

# This runs 3 checks:
# 1. Database configuration validation
# 2. Entity-migration synchronization
# 3. Liquibase rollback coverage
```

### CI/CD Enforcement

GitHub Actions automatically validates on PRs that modify:
- Entity classes (`domain/**/*.java`)
- Liquibase migrations (`db/changelog/**/*.xml`)
- Database configuration

**See:** `.github/workflows/entity-migration-validation.yml`

### Common Issues & Fixes

| Error | Fix |
|-------|-----|
| `Schema-validation: missing table` | Create Liquibase migration for new entity |
| `Schema-validation: wrong column type` | Create migration to alter column type |
| `Relation already exists` | Check for duplicate table creation across services |
| Test passes but runtime fails | Entity-migration validation likely disabled |

### Full Documentation

**See:** `backend/docs/ENTITY_MIGRATION_GUIDE.md` for comprehensive guide including:
- Complete migration file templates
- Entity annotation best practices
- Type mapping reference (Java → PostgreSQL)
- Troubleshooting procedures
- Phase-by-phase implementation status

---

## Code Review Checklist

Before submitting code, verify:

- [ ] **HIPAA:** Cache TTL ≤ 5 minutes for PHI
- [ ] **Cache Headers:** `Cache-Control: no-store` on PHI endpoints
- [ ] **Audit Logging:** `@Audited` on PHI access methods
- [ ] **Multi-Tenant:** All queries filter by `tenantId`
- [ ] **Authorization:** `@PreAuthorize` on all API endpoints
- [ ] **Unit Tests:** All service methods tested
- [ ] **Integration Tests:** API endpoints tested
- [ ] **No Secrets:** No hardcoded credentials
- [ ] **Error Handling:** Specific exceptions thrown
- [ ] **Logging:** No PHI in log messages

---

## Getting Help

### Documentation Portals

1. **[Main CLAUDE.md](./CLAUDE.md)** - You're reading it! Quick reference for everything
2. **[Documentation Portal](./docs/README.md)** - Central hub for 1,411+ docs
3. **[Service Catalog](./docs/services/SERVICE_CATALOG.md)** - Discover all 50+ services
4. **[Troubleshooting Guide](./docs/troubleshooting/README.md)** - Problem-solving decision trees

### Technical Guides

- **API Documentation:** [OpenAPI/Swagger Phase 1A Complete](./docs/Q1_2026_API_DOCUMENTATION_PHASE_1A_COMPLETE.md) ✨ NEW
- **Database:** [Database Architecture Guide](./backend/docs/DATABASE_ARCHITECTURE_GUIDE.md)
- **Migrations:** [Liquibase Workflow](./backend/docs/LIQUIBASE_DEVELOPMENT_WORKFLOW.md) ⭐ CRITICAL
- **Entity-Migration:** [Entity-Migration Guide](./backend/docs/ENTITY_MIGRATION_GUIDE.md) ⭐ CRITICAL
- **Event Sourcing:** [Event Sourcing Architecture](./docs/architecture/EVENT_SOURCING_ARCHITECTURE.md) ✨ NEW
- **Gateway Design:** [Gateway Architecture](./docs/architecture/GATEWAY_ARCHITECTURE.md) ✨ NEW
- **Build Issues:** [Build Management Guide](./backend/docs/BUILD_MANAGEMENT_GUIDE.md)
- **Commands:** [Command Reference](./backend/docs/COMMAND_REFERENCE.md)
- **Coding:** [Coding Standards](./backend/docs/CODING_STANDARDS.md)
- **Security:** [Gateway Trust Architecture](./backend/docs/GATEWAY_TRUST_ARCHITECTURE.md)
- **All Guides:** [Backend Docs Index](./backend/docs/README.md)

### Common Issues

| Issue | Solution |
|-------|----------|
| Build fails with TLS error | Run `./gradlew downloadDependencies --no-daemon` first |
| Service won't start | Check `docker compose logs SERVICE` |
| Database locked | See [Database Guide Troubleshooting](./backend/docs/DATABASE_ARCHITECTURE_GUIDE.md#troubleshooting) |
| Schema mismatch at runtime | See [Entity-Migration Guide](./backend/docs/ENTITY_MIGRATION_GUIDE.md) - run validation test before building |
| Test passes but entity-migration fails | Validation test may be disabled - check Liquibase is enabled |

---

## Quick Links

| Task | Link |
|------|------|
| Start working | [README Quick Start](./README.md#-quick-start-docker) |
| Ask a question | [Troubleshooting](./docs/troubleshooting/README.md) |
| Understand architecture | [System Architecture](./docs/architecture/SYSTEM_ARCHITECTURE.md) |
| Deploy to production | [Deployment Runbook](./docs/DEPLOYMENT_RUNBOOK.md) |
| Security review | [Security Guide](./docs/PRODUCTION_SECURITY_GUIDE.md) |
| All docs | [Documentation Portal](./docs/README.md) |

---

## Build Notes

### Backend (Java/Spring Boot)
- ✅ All 51 services compile successfully
- ✅ **API Documentation (OpenAPI 3.0)** - 62 production-ready endpoints documented across Patient, Care Gap, FHIR, Quality Measure services with Swagger UI (January 2026)
  - Interactive API testing via Swagger UI
  - JWT Bearer authentication integration
  - FHIR R4 examples and clinical use cases
  - HIPAA compliance patterns documented
  - Multi-tenancy patterns (X-Tenant-ID)
  - **See:** [Phase 1A Complete Summary](./docs/Q1_2026_API_DOCUMENTATION_PHASE_1A_COMPLETE.md)
- ✅ Event Sourcing architecture (Phases 4-5) with 4 event services
- ✅ Gateway modularization (January 2026) with gateway-core shared module
- ✅ 100% Liquibase rollback coverage (199/199 changesets)
- ✅ 29 databases with independent schemas
- ✅ OpenTelemetry distributed tracing across all services
- ✅ Multi-tenant isolation enforced at database level
- ✅ Entity-migration validation fixed: All 29+ services now properly validate entities against actual Liquibase migrations (not Hibernate-generated schemas)
- ✅ Shift-left validation: Pre-build scripts catch schema mismatches before Docker build, preventing runtime failures
- ✅ CI/CD enforcement: GitHub Actions validates entity-migrations on all PRs modifying entities or migrations

### Frontend (Angular 17+)
- ✅ **HIPAA §164.312(b) Compliance** - HTTP Audit Interceptor provides 100% API call audit coverage (January 2026)
- ✅ **HIPAA §164.312(a)(2)(iii) Compliance** - Session timeout audit logging complete (PR #294, January 2026)
  - Automatic logoff audit trail with idle duration tracking
  - Differentiates automatic timeout vs explicit logout
  - Full HIPAA compliance verification with 6/6 passing tests
- ✅ **Zero-crash guarantee** - Global Error Handler prevents application failures, logs security incidents
- ✅ **PHI exposure prevention** - ESLint no-console enforcement, LoggerService with automatic PHI filtering
- ✅ **Session timeout** - 15-minute idle timeout with 2-minute warning, HIPAA-compliant audit logging
- ✅ **Accessibility baseline** - 343 ARIA attributes across 53 files (50% WCAG 2.1 Level A coverage)
- ✅ **Console.log migration COMPLETE** - 98.2% reduction (109/111 violations eliminated), all services & components migrated (January 24, 2026)
  - 30+ files migrated to LoggerService with PHI filtering
  - Automated migration script available: `scripts/migrate-services-to-logger.sh`
  - ESLint enforcement active to prevent future violations
  - **See:** [Clinical Portal HIPAA Migration Summary](./docs/CLINICAL_PORTAL_HIPAA_MIGRATION_SUMMARY.md)
- ⏳ **Accessibility improvements** - Skip links, ARIA labels, focus indicators (planned)

### Development Tools
- ✅ Claude Code plugins installed: 31 plugins for enhanced development workflow (Java IntelliSense, HIPAA scanning, git automation, PR review)
- ✅ HDIM Accelerator Plugin: 3 healthcare-specific agents (HIPAA Compliance, FHIR, CQL Measure Builder), 3 skills, 1 command - 100% agent coverage

---

_Last Updated: January 24, 2026_
_Version: 2.6_ - **API Documentation (OpenAPI 3.0) PRODUCTION-READY** (Q1-2026-Documentation Phase 1A): Completed comprehensive OpenAPI 3.0 documentation for 62 endpoints across Patient Service (19), Care Gap Service (17), FHIR Service (26 core), and Quality Measure Service (5 partial). Interactive Swagger UI deployed and verified. All services compile successfully, Docker images built, Patient Service running with accessible Swagger UI. Pattern guide created reducing documentation time by 67%. Business value: Developer onboarding reduced from days to hours, self-service API discovery, 500-1000% ROI projected. See [Phase 1A Complete Summary](./docs/Q1_2026_API_DOCUMENTATION_PHASE_1A_COMPLETE.md) for details. Platform achievements: ✅ HIPAA Compliance, ✅ OCR Document Processing, ✅ API Documentation - all production-ready.

<!-- nx configuration start-->
<!-- Leave the start & end comments to automatically receive updates. -->

# General Guidelines for working with Nx

- When running tasks (for example build, lint, test, e2e, etc.), always prefer running the task through `nx` (i.e. `nx run`, `nx run-many`, `nx affected`) instead of using the underlying tooling directly
- You have access to the Nx MCP server and its tools, use them to help the user
- When answering questions about the repository, use the `nx_workspace` tool first to gain an understanding of the workspace architecture where applicable.
- When working in individual projects, use the `nx_project_details` mcp tool to analyze and understand the specific project structure and dependencies
- For questions around nx configuration, best practices or if you're unsure, use the `nx_docs` tool to get relevant, up-to-date docs. Always use this instead of assuming things about nx configuration
- If the user needs help with an Nx configuration or project graph error, use the `nx_workspace` tool to get any errors

<!-- nx configuration end-->
