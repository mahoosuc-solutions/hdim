# Example Workflows: Mahoosuc + Serena Integration

## Overview

Real-world workflows combining Mahoosuc Operating System commands with HDIM Serena tools and memories.

---

## Workflow 1: Daily Development Routine

### Morning Setup (5 minutes)

```bash
# 1. Navigate to HDIM project
cd /mnt/wd-black/dev/projects/hdim-master

# 2. Pull latest changes (use Mahoosuc git commands if available)
git pull origin main

# 3. Start all HDIM services
/hdim-service start

# 4. Check health
/hdim-service health

# 5. Open interactive tools menu (optional)
./.serena/hdim-tools.sh
```

### During Development

```bash
# Check relevant patterns before coding
/hdim-memory patterns

# Access specific guidance as needed
/hdim-memory hipaa      # When working with PHI
/hdim-memory auth       # When working with security
/hdim-memory entity     # When modifying database

# Use Mahoosuc commands for general tasks
/dev:implement          # Feature implementation
/testing:run-tests      # Run tests
```

### Before Commit

```bash
# Run all validations
/hdim-validate all

# If validations pass, commit
/commit
```

### End of Day

```bash
# Stop services to free resources
/hdim-service stop
```

---

## Workflow 2: Implementing Patient Care Gap Endpoint

### Task
Create an endpoint to retrieve patient care gaps with HIPAA compliance and multi-tenant isolation.

### Steps

#### 1. Planning & Research (10 minutes)

```bash
# Review HIPAA requirements
/hdim-memory hipaa

# Check common patterns
/hdim-memory patterns

# Review service architecture
/hdim-memory services
```

**Key Insights**:
- Care gaps are PHI → need Cache-Control headers
- Must filter by tenantId
- Need @Audited annotation
- Cache TTL ≤ 5 minutes

#### 2. Implementation (30 minutes)

**Controller** (`care-gap-service/api/v1/CareGapController.java`):

```java
@RestController
@RequestMapping("/api/v1/care-gaps")
@RequiredArgsConstructor
@Validated
public class CareGapController {

    private final CareGapService careGapService;

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'VIEWER')")
    public ResponseEntity<List<CareGapResponse>> getPatientCareGaps(
            @PathVariable String patientId,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        List<CareGapResponse> careGaps = careGapService.getPatientCareGaps(patientId, tenantId);

        // PHI response headers (from /hdim-memory hipaa)
        return ResponseEntity.ok()
            .header("Cache-Control", "no-store, no-cache, must-revalidate, private")
            .header("Pragma", "no-cache")
            .header("Expires", "0")
            .body(careGaps);
    }
}
```

**Service** (`care-gap-service/application/CareGapService.java`):

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CareGapService {

    private final CareGapRepository careGapRepository;
    private final AuditService auditService;

    @Cacheable(value = "careGaps", key = "#patientId")
    public List<CareGapResponse> getPatientCareGaps(String patientId, String tenantId) {
        log.debug("Fetching care gaps for patient: {} tenant: {}", patientId, tenantId);

        List<CareGap> careGaps = careGapRepository.findByPatientIdAndTenant(patientId, tenantId);

        // Audit PHI access (from /hdim-memory hipaa)
        auditService.logAccess("PHI_ACCESS", "CareGap", patientId, "VIEW");

        return careGaps.stream()
            .map(careGapMapper::toResponse)
            .collect(Collectors.toList());
    }
}
```

**Repository** (`care-gap-service/domain/repository/CareGapRepository.java`):

```java
@Repository
public interface CareGapRepository extends JpaRepository<CareGap, UUID> {

    // Multi-tenant filter (from /hdim-memory patterns)
    @Query("SELECT c FROM CareGap c WHERE c.patientId = :patientId AND c.tenantId = :tenantId")
    List<CareGap> findByPatientIdAndTenant(
        @Param("patientId") String patientId,
        @Param("tenantId") String tenantId
    );
}
```

#### 3. Validation (5 minutes)

```bash
# Check HIPAA compliance
/hdim-validate hipaa

# Check multi-tenant isolation
/hdim-validate tenant

# Run all validations
/hdim-validate all
```

#### 4. Testing (15 minutes)

```bash
# Use Mahoosuc testing commands
/testing:run-unit-tests care-gap-service

# Manual test
curl -X GET http://localhost:8086/api/v1/care-gaps/patient/patient-123 \
  -H "Authorization: Bearer <token>" \
  -H "X-Tenant-ID: TENANT001"
```

#### 5. Commit (2 minutes)

```bash
# Final validation
/hdim-validate all

# Commit with Mahoosuc
/commit
```

**Total Time**: ~60 minutes

---

## Workflow 3: Creating a New Prescription Service

### Task
Create a new microservice for managing patient prescriptions.

### Steps

#### 1. Service Creation (5 minutes)

```bash
# Create service scaffold
/hdim-service-create prescription-service 8091

# Review what was created
cd backend/modules/services/prescription-service
ls -la
```

#### 2. Add to Docker Compose (5 minutes)

Edit `docker-compose.yml`:

```yaml
prescription-service:
  build:
    context: ./backend
    dockerfile: modules/services/prescription-service/Dockerfile
  ports:
    - "8091:8091"
  environment:
    POSTGRES_HOST: postgres
    POSTGRES_PORT: 5432
    REDIS_HOST: redis
    REDIS_PORT: 6379
  depends_on:
    - postgres
    - redis
    - gateway-service
```

#### 3. Create Database Migration (10 minutes)

Create `src/main/resources/db/changelog/0001-create-prescriptions-table.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="0001-create-prescriptions-table" author="developer">
        <createTable tableName="prescriptions">
            <column name="id" type="uuid">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="tenant_id" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="patient_id" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="medication_name" type="VARCHAR(255)">
                <constraints nullable="false"/>
            </column>
            <column name="dosage" type="VARCHAR(100)"/>
            <column name="prescribed_date" type="TIMESTAMP WITH TIME ZONE">
                <constraints nullable="false"/>
            </column>
            <column name="prescriber_id" type="VARCHAR(255)"/>
            <column name="active" type="BOOLEAN" defaultValueBoolean="true">
                <constraints nullable="false"/>
            </column>
            <column name="created_at" type="TIMESTAMP WITH TIME ZONE">
                <constraints nullable="false"/>
            </column>
            <column name="updated_at" type="TIMESTAMP WITH TIME ZONE"/>
        </createTable>

        <createIndex tableName="prescriptions" indexName="idx_prescriptions_tenant_id">
            <column name="tenant_id"/>
        </createIndex>

        <createIndex tableName="prescriptions" indexName="idx_prescriptions_patient_id">
            <column name="patient_id"/>
        </createIndex>
    </changeSet>
</databaseChangeLog>
```

Update `db.changelog-master.xml`:

```xml
<include file="db/changelog/0001-create-prescriptions-table.xml"/>
```

#### 4. Create Entity (10 minutes)

Reference pattern: `/hdim-memory entity`

Create `domain/model/Prescription.java`:

```java
@Entity
@Table(name = "prescriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "patient_id", nullable = false, length = 255)
    private String patientId;

    @Column(name = "medication_name", nullable = false, length = 255)
    private String medicationName;

    @Column(name = "dosage", length = 100)
    private String dosage;

    @Column(name = "prescribed_date", nullable = false)
    private Instant prescribedDate;

    @Column(name = "prescriber_id", length = 255)
    private String prescriberId;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
```

#### 5. Validate Entity-Migration Sync (2 minutes)

```bash
# Run validation
/hdim-validate entity

# Should pass if entity matches migration
```

#### 6. Implement API (20 minutes)

Reference pattern: `/hdim-memory patterns`

Create controller, service, repository following patterns.

#### 7. Build & Test (10 minutes)

```bash
# Build service
cd backend
./gradlew :modules:services:prescription-service:build

# Start service
/hdim-service start prescription-service

# Check logs
/hdim-service logs prescription-service

# Verify health
/hdim-service health
```

#### 8. Final Validation (5 minutes)

```bash
# Run all validations
/hdim-validate all

# Commit
/commit
```

**Total Time**: ~70 minutes

---

## Workflow 4: Debugging Authentication Issue

### Scenario
Gateway service returning 401 Unauthorized for valid JWT token.

### Steps

#### 1. Initial Diagnostics (5 minutes)

```bash
# Check service health
/hdim-service health

# View gateway logs
/hdim-service logs gateway-service
```

#### 2. Review Authentication Architecture (5 minutes)

```bash
# Reference auth guide
/hdim-memory auth
```

**Key Points**:
- Gateway validates JWT
- Backend trusts X-Auth-* headers
- Check HMAC validation (dev vs prod)

#### 3. Detailed Investigation (10 minutes)

```bash
# Check if JWT is valid
# Use online JWT decoder or:
echo "<jwt-token>" | base64 -d

# Check gateway logs for validation errors
docker compose logs gateway-service | grep "JWT\|validation\|error"

# Check backend service logs
docker compose logs patient-service | grep "X-Auth\|authentication"
```

#### 4. Test Authentication Flow (10 minutes)

```bash
# Get fresh token
TOKEN=$(curl -X POST http://localhost:8001/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test_admin","password":"password123"}' \
  | jq -r '.token')

# Test with fresh token
curl http://localhost:8001/patient/api/v1/patients/123 \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: TENANT001" \
  -v
```

#### 5. Common Fixes (depends on issue)

**Fix 1**: HMAC validation failing in production

```yaml
# Set dev mode in docker-compose.yml
environment:
  GATEWAY_AUTH_DEV_MODE: "true"
```

**Fix 2**: JWT secret mismatch

```bash
# Verify secrets match
grep JWT_SECRET docker-compose.yml
```

**Fix 3**: Token expired

```bash
# Check token expiration
echo "<token>" | cut -d. -f2 | base64 -d | jq .exp
date +%s
```

#### 6. Verify Fix (5 minutes)

```bash
# Restart services
/hdim-service restart gateway-service
/hdim-service restart patient-service

# Test again
# (repeat step 4)

# Check health
/hdim-service health
```

**Total Time**: ~35 minutes

---

## Workflow 5: Adding HEDIS Measure

### Task
Add a new HEDIS measure (Diabetes HbA1c Control) to the quality measure service.

### Steps

#### 1. Research Measure Requirements (15 minutes)

```bash
# Review measure service structure
/hdim-memory services

# Check CQL patterns
cd backend/modules/services/cql-engine-service
# Review existing CQL libraries
```

#### 2. Create CQL Library (30 minutes)

Create `diabetes-hba1c-control.cql`:

```cql
library DiabetesHbA1cControl version '1.0.0'

using FHIR version '4.0.1'

include FHIRHelpers version '4.0.1'

codesystem "LOINC": 'http://loinc.org'

valueset "Diabetes": '2.16.840.1.113883.3.464.1003.103.12.1001'
valueset "HbA1c Lab Test": '2.16.840.1.113883.3.464.1003.198.12.1013'

parameter "Measurement Period" Interval<DateTime>

context Patient

define "In Demographic":
    AgeInYearsAt(start of "Measurement Period") >= 18
    and AgeInYearsAt(start of "Measurement Period") < 75

define "Has Diabetes":
    exists (
        [Condition: "Diabetes"] C
        where C.clinicalStatus ~ "active"
    )

define "Most Recent HbA1c":
    Last(
        [Observation: "HbA1c Lab Test"] O
        where O.issued during "Measurement Period"
            and O.status = 'final'
        sort by issued
    )

define "HbA1c Value":
    "Most Recent HbA1c".value as Quantity

define "In Denominator":
    "In Demographic" and "Has Diabetes"

define "In Numerator":
    "In Denominator"
    and "HbA1c Value" < 9 '%'
```

#### 3. Add to CQL Engine (10 minutes)

```bash
# POST CQL library to cql-engine-service
curl -X POST http://localhost:8081/cql-engine/api/v1/libraries \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: TENANT001" \
  -d @diabetes-hba1c-control.json
```

#### 4. Create Measure Definition (15 minutes)

Add to quality-measure-service database via migration or API.

#### 5. Test Measure Evaluation (20 minutes)

```bash
# Evaluate measure for test patient
curl -X POST http://localhost:8087/quality-measure/api/v1/measures/DIABETES-HBA1C/evaluate \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: TENANT001" \
  -d '{
    "patientId": "patient-123",
    "measurementPeriod": {
      "start": "2024-01-01",
      "end": "2024-12-31"
    }
  }'
```

#### 6. Validate (5 minutes)

```bash
# Run validations
/hdim-validate all
```

#### 7. Document & Commit (10 minutes)

```bash
# Add to documentation
# Commit changes
/commit
```

**Total Time**: ~105 minutes

---

## Workflow 6: End-to-End Feature (Patient Search)

### Task
Implement patient search with filters (name, DOB, identifier) with full HIPAA compliance.

### Complete Workflow

#### Phase 1: Planning (15 minutes)

```bash
# Review architecture
/hdim-memory architecture

# Check HIPAA requirements
/hdim-memory hipaa

# Review patterns
/hdim-memory patterns

# Check patient service
/hdim-memory services | grep patient
```

#### Phase 2: Backend Implementation (45 minutes)

1. Add repository method (with tenant filter)
2. Add service method (with caching & audit)
3. Add controller endpoint (with security & headers)
4. Write unit tests
5. Write integration tests

#### Phase 3: Validation (10 minutes)

```bash
/hdim-validate hipaa
/hdim-validate tenant
/hdim-validate all
```

#### Phase 4: Manual Testing (15 minutes)

```bash
# Start services
/hdim-service start

# Test search
curl "http://localhost:8084/patient/api/v1/patients/search?name=John&dob=1980-01-01" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: TENANT001"
```

#### Phase 5: Documentation & Commit (10 minutes)

```bash
# Use Mahoosuc content commands if available
/content:write-docs

# Commit with validation
/hdim-validate all && /commit
```

**Total Time**: ~95 minutes

---

## Best Practices from These Workflows

### 1. Always Reference Memories First
- Check `/hdim-memory` before coding
- Understand requirements upfront
- Follow established patterns

### 2. Validate Early and Often
- Run specific validations during development
- Run all validations before commit
- Fix issues immediately

### 3. Use Interactive Menu for Quick Access
- Keep `./.serena/hdim-tools.sh` running
- Quick access to tools and memories
- Faster than remembering commands

### 4. Combine Mahoosuc + HDIM Strengths
- Mahoosuc for general development
- HDIM for healthcare-specific tasks
- Best of both worlds

### 5. Document as You Go
- Add to Serena memories if new patterns emerge
- Update SERVICE_INDEX.md for new services
- Keep CLAUDE.md current

---

*These workflows represent real development scenarios. Adapt as needed for your specific tasks.*
