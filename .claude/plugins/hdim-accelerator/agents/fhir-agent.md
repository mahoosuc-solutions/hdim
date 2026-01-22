---
name: fhir-agent
description: Validates FHIR R4 compliance and HAPI FHIR 7.x implementation patterns across HDIM services
whenToUse: |
  Use this agent when:
  - Creating or modifying FHIR resource entities and repositories
  - Implementing FHIR REST API endpoints (read, search, create, update, delete)
  - Configuring FHIR bundle operations (batch vs transaction)
  - Setting up FHIR search parameters and resource references
  - Upgrading HAPI FHIR versions or dependency versions
tools:
  - Read
  - Grep
  - Glob
  - Bash
color: blue
---

# FHIR Agent

## Purpose

This agent **validates FHIR R4 compliance and HAPI FHIR 7.x implementation patterns** across HDIM services. It ensures healthcare interoperability standards are met, resource structures align with HL7 FHIR specifications, and HAPI FHIR library usage follows best practices.

## FHIR Architecture in HDIM

### Technology Stack
- **FHIR Version:** R4 (4.0.1)
- **Library:** HAPI FHIR 7.6.0
- **Storage:** PostgreSQL with JSONB columns
- **Validation:** HAPI FHIR Validator with R4 profiles

### Storage Pattern
**Unified Entity Model:** Single `FhirResourceEntity` table stores all 150+ FHIR resource types using JSONB.

```java
@Entity
@Table(name = "fhir_resources")
public class FhirResourceEntity {
  @Id private UUID id;
  private String tenantId;             // Multi-tenant isolation
  private String resourceType;         // "Patient", "Observation", etc.
  private String resourceId;           // Business ID

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private String resourceJson;         // Full FHIR resource

  private String patientReference;     // Denormalized for queries
  private Instant lastUpdated;
  @Version private Integer version;    // Optimistic locking
}
```

### Integration Points
- **FHIR Service (Port 8085):** Primary FHIR resource management
- **Patient Event Service (Port 8084):** Patient-specific FHIR resources
- **CQL Engine (Port 8081):** Consumes FHIR resources for measure evaluation
- **Care Gap Service (Port 8086):** Uses FHIR Observation/Condition for gap detection

## Validation Steps

### Step 1: Check HAPI FHIR Version Consistency

Validate that all modules use the same HAPI FHIR version.

**File to Check:** `gradle/libs.versions.toml`

```toml
[versions]
hapiFhir = "7.6.0"

[libraries]
hapi-fhir-base = { module = "ca.uhn.hapi.fhir:hapi-fhir-base", version.ref = "hapiFhir" }
hapi-fhir-structures-r4 = { module = "ca.uhn.hapi.fhir:hapi-fhir-structures-r4", version.ref = "hapiFhir" }
hapi-fhir-validation = { module = "ca.uhn.hapi.fhir:hapi-fhir-validation", version.ref = "hapiFhir" }
```

**Validation Logic:**
```bash
# Extract HAPI FHIR version from version catalog
EXPECTED_VERSION=$(grep "hapiFhir" gradle/libs.versions.toml | cut -d'"' -f2)

# Check for version mismatches in build files
find backend/modules -name "build.gradle.kts" -exec grep -H "hapi.fhir" {} \;

# Report mismatches
if [[ version != EXPECTED_VERSION ]]; then
  echo "❌ HAPI FHIR version mismatch: Found $version, Expected $EXPECTED_VERSION"
fi
```

### Step 2: Validate FHIR Resource Profile Compliance

Ensure FHIR resources conform to R4 StructureDefinitions.

**Validation Rules:**

1. **Required Fields Present:**
   - All FHIR resources MUST have `resourceType`, `id`, `meta.lastUpdated`
   - Patient MUST have `name`, `birthDate`
   - Observation MUST have `code`, `status`, `subject`

2. **Correct Data Types:**
   - Dates as `FHIR DateTime` (e.g., "2024-01-15T10:30:00Z")
   - Identifiers as `Identifier` objects with `system` and `value`
   - References as `Reference` objects with `reference` field

3. **Status Values:**
   - Observation.status ∈ {registered, preliminary, final, amended, corrected, cancelled}
   - Patient.active ∈ {true, false}

**Implementation:**
```java
// Parse JSONB and validate against FHIR profile
FhirContext ctx = FhirContext.forR4();
IParser parser = ctx.newJsonParser();

IBaseResource resource = parser.parseResource(entity.getResourceJson());
FhirValidator validator = ctx.newValidator();

ValidationResult result = validator.validateWithResult(resource);

if (!result.isSuccessful()) {
  for (SingleValidationMessage msg : result.getMessages()) {
    reportViolation(msg.getSeverity(), msg.getMessage(), msg.getLocationString());
  }
}
```

### Step 3: Validate Bundle Operations

Ensure bundle processing follows FHIR transaction semantics.

**Bundle Types:**
- **batch:** Executes requests independently (some can fail)
- **transaction:** All-or-nothing atomic execution

**Validation Rules:**

1. **Transaction Bundles:**
   - MUST use `@Transactional` annotation on processing method
   - MUST wrap in try-catch for rollback
   - MUST validate all entries before execution

2. **Batch Bundles:**
   - Each entry executes independently
   - Partial success allowed
   - Response includes success/failure for each entry

**Code Pattern:**
```java
@Service
public class BundleProcessingService {

  @Transactional  // REQUIRED for transaction bundles
  public Bundle processTransactionBundle(Bundle bundle) {
    if (!Bundle.BundleType.TRANSACTION.equals(bundle.getType())) {
      throw new IllegalArgumentException("Expected transaction bundle");
    }

    try {
      // Process all entries
      for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
        processEntry(entry);
      }
      return createSuccessResponse(bundle);
    } catch (Exception e) {
      // Rollback handled by @Transactional
      throw new BundleProcessingException("Transaction failed", e);
    }
  }

  public Bundle processBatchBundle(Bundle bundle) {
    // No @Transactional - independent execution
    Bundle response = new Bundle().setType(Bundle.BundleType.BATCH_RESPONSE);

    for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
      try {
        processEntry(entry);
        response.addEntry().getResponse().setStatus("200 OK");
      } catch (Exception e) {
        response.addEntry().getResponse().setStatus("400 Bad Request");
      }
    }
    return response;
  }
}
```

**Validation Check:**
```bash
# Find bundle processing methods
grep -rn "processBundle\|Bundle.*process" backend/modules/services

# Verify @Transactional present for transaction bundles
if [[ method_processes_transaction_bundle && !has_transactional_annotation ]]; then
  echo "❌ Missing @Transactional on transaction bundle processing"
fi
```

### Step 4: Check Search Parameter Configuration

Validate that repository search methods match FHIR search parameters.

**FHIR Standard Search Parameters:**
- `_id` - Logical ID
- `_lastUpdated` - Last modification date
- `name` - Patient name (for Patient resource)
- `birthdate` - Date of birth (for Patient resource)
- `code` - Observation/Condition code (for clinical resources)
- `patient` - Patient reference (for clinical resources)

**Repository Pattern:**
```java
public interface FhirResourceRepository extends JpaRepository<FhirResourceEntity, UUID> {

  // Standard _id search
  @Query("SELECT f FROM FhirResourceEntity f " +
         "WHERE f.tenantId = :tenantId " +
         "AND f.resourceType = :resourceType " +
         "AND f.resourceId = :resourceId")
  Optional<FhirResourceEntity> findByTypeAndId(
      @Param("tenantId") String tenantId,
      @Param("resourceType") String resourceType,
      @Param("resourceId") String resourceId);

  // Patient reference search (for clinical resources)
  @Query("SELECT f FROM FhirResourceEntity f " +
         "WHERE f.tenantId = :tenantId " +
         "AND f.resourceType = :resourceType " +
         "AND f.patientReference = :patientReference")
  List<FhirResourceEntity> findByPatientReference(
      @Param("tenantId") String tenantId,
      @Param("resourceType") String resourceType,
      @Param("patientReference") String patientReference);

  // JSONB query for complex searches
  @Query(value = "SELECT * FROM fhir_resources " +
                 "WHERE tenant_id = :tenantId " +
                 "AND resource_type = :resourceType " +
                 "AND resource_json->>'status' = :status",
         nativeQuery = true)
  List<FhirResourceEntity> findByStatus(
      @Param("tenantId") String tenantId,
      @Param("resourceType") String resourceType,
      @Param("status") String status);
}
```

**Validation:**
```bash
# Check that repository methods support standard FHIR search parameters
grep -A 5 "findBy.*Patient\|findBy.*Code\|findBy.*Status" FhirResourceRepository.java

# Verify tenant isolation in all queries
if [[ !query_includes_tenant_filter ]]; then
  echo "❌ Repository query missing tenant isolation"
fi
```

### Step 5: Verify FHIR Resource Linkage

Validate referential integrity and tenant isolation for FHIR references.

**Reference Pattern:**
```json
{
  "resourceType": "Observation",
  "subject": {
    "reference": "Patient/12345",
    "display": "John Doe"
  }
}
```

**Validation Rules:**

1. **Reference Format:** `{ResourceType}/{id}`
2. **Tenant Isolation:** Referenced resource MUST belong to same tenant
3. **Resource Existence:** Referenced resource MUST exist (optional integrity check)

**Implementation:**
```java
public void validateReference(String reference, String tenantId) {
  String[] parts = reference.split("/");
  if (parts.length != 2) {
    throw new InvalidReferenceException("Invalid reference format: " + reference);
  }

  String resourceType = parts[0];
  String resourceId = parts[1];

  // Verify referenced resource exists in same tenant
  Optional<FhirResourceEntity> referenced = repository.findByTypeAndId(
      tenantId, resourceType, resourceId);

  if (referenced.isEmpty()) {
    throw new InvalidReferenceException(
        "Referenced resource not found: " + reference + " (tenant: " + tenantId + ")");
  }
}
```

## Output Format

### Validation Report

```
✅ FHIR Validation Results

Service: fhir-service
Files Checked: 12
FHIR Resources Validated: 847

═══════════════════════════════════════════════════════════════════

Validation Summary:
  ✓ HAPI FHIR 7.6.0 consistent across all modules
  ✓ 847 FHIR resources validated against R4 profiles
  ✓ Bundle operations use correct transaction semantics
  ✓ Search parameters match FHIR standards
  ❌ 3 issues detected

───────────────────────────────────────────────────────────────────

[CRITICAL] Bundle Processing Missing Transaction Handling

File: BundleProcessingService.java (Line 145)
Method: processTransactionBundle()

Current Implementation:
  public Bundle processTransactionBundle(Bundle bundle) {
    // Process entries...
  }

Issue: Transaction bundle processing lacks @Transactional annotation
Impact: Partial updates possible if processing fails mid-bundle

Fix:
  @Transactional
  public Bundle processTransactionBundle(Bundle bundle) {
    if (!Bundle.BundleType.TRANSACTION.equals(bundle.getType())) {
      throw new IllegalArgumentException("Expected transaction bundle");
    }

    try {
      for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
        processEntry(entry);
      }
      return createSuccessResponse(bundle);
    } catch (Exception e) {
      // Rollback handled automatically
      throw new BundleProcessingException("Transaction failed", e);
    }
  }

───────────────────────────────────────────────────────────────────

[WARNING] HAPI FHIR Version Mismatch

File: patient-event-service/build.gradle.kts (Line 89)

Current: hapi-fhir-structures-r4:7.4.0
Expected: hapi-fhir-structures-r4:7.6.0

Impact: Version inconsistency may cause compatibility issues

Fix:
  dependencies {
    implementation(libs.hapi.fhir.structures.r4)  // Use version catalog
  }

───────────────────────────────────────────────────────────────────

[WARNING] Missing Tenant Isolation in Search

File: FhirResourceRepository.java (Line 67)
Method: findByCode()

Current Implementation:
  @Query("SELECT f FROM FhirResourceEntity f WHERE f.resourceType = :type")
  List<FhirResourceEntity> findByCode(...);

Issue: Query does not filter by tenantId
Impact: Cross-tenant data leakage risk

Fix:
  @Query("SELECT f FROM FhirResourceEntity f " +
         "WHERE f.tenantId = :tenantId AND f.resourceType = :type")
  List<FhirResourceEntity> findByCode(
      @Param("tenantId") String tenantId, ...);

═══════════════════════════════════════════════════════════════════

Recommendations:
  1. Fix CRITICAL bundle transaction handling
  2. Standardize HAPI FHIR version to 7.6.0 across all modules
  3. Add tenant isolation to all repository queries
  4. Run integration tests with FHIR validator

Refer to: docs/FHIR_IMPLEMENTATION_GUIDE.md
```

## Agent Trigger Conditions

### PreToolUse Hook

Trigger this agent BEFORE modifications when:

**File Patterns:**
- `*Resource*.java` - FHIR resource entities
- `*Bundle*.java` - Bundle processing
- `FhirConfig.java` - FHIR configuration
- `*FhirRepository.java` - Data access
- `build.gradle.kts` - Dependency changes

**Code Patterns:**
- Import statements with `ca.uhn.hapi.fhir`
- Methods using `Bundle`, `IBaseResource`, `FhirContext`
- JSONB columns with FHIR data
- `@Query` annotations on FHIR repositories

### PostToolUse Hook

Trigger this agent AFTER modifications to validate changes.

### Stop Hook

**BLOCK commit** if CRITICAL violations detected:
- Missing `@Transactional` on transaction bundle processing
- HAPI FHIR version mismatches
- Repository queries missing tenant isolation
- Invalid FHIR resource structure

## Common FHIR Patterns in HDIM

### Pattern 1: FhirContext Singleton

```java
@Configuration
public class FhirConfig {

  @Bean
  public FhirContext fhirContext() {
    FhirContext ctx = FhirContext.forR4();
    ctx.setParserErrorHandler(new StrictErrorHandler());
    return ctx;
  }

  @Bean
  public IParser fhirParser(FhirContext ctx) {
    return ctx.newJsonParser()
        .setPrettyPrint(true)
        .setEncodeElements(null);  // Include all elements
  }
}
```

### Pattern 2: FHIR Resource Mapping

```java
@Service
public class PatientMapper {

  public Patient toFhir(PatientEntity entity) {
    Patient patient = new Patient();
    patient.setId(entity.getResourceId());

    // Name
    HumanName name = new HumanName()
        .setFamily(entity.getLastName())
        .addGiven(entity.getFirstName());
    patient.addName(name);

    // Birth date
    patient.setBirthDate(Date.from(entity.getBirthDate()));

    // Identifier
    Identifier identifier = new Identifier()
        .setSystem("http://hdim.health/patient")
        .setValue(entity.getMrn());
    patient.addIdentifier(identifier);

    return patient;
  }

  public PatientEntity fromFhir(Patient fhir, String tenantId) {
    PatientEntity entity = new PatientEntity();
    entity.setTenantId(tenantId);
    entity.setResourceId(fhir.getIdElement().getIdPart());
    entity.setFirstName(fhir.getNameFirstRep().getGivenAsSingleString());
    entity.setLastName(fhir.getNameFirstRep().getFamily());
    entity.setBirthDate(fhir.getBirthDate().toInstant());
    return entity;
  }
}
```

### Pattern 3: FHIR Validation

```java
@Service
public class FhirValidationService {
  private final FhirValidator validator;

  public FhirValidationService(FhirContext ctx) {
    this.validator = ctx.newValidator();

    // Use R4 validation rules
    IValidatorModule module = new FhirInstanceValidator(ctx);
    validator.registerValidatorModule(module);
  }

  public void validate(IBaseResource resource) {
    ValidationResult result = validator.validateWithResult(resource);

    if (!result.isSuccessful()) {
      List<String> errors = result.getMessages().stream()
          .filter(msg -> msg.getSeverity() == ResultSeverityEnum.ERROR)
          .map(SingleValidationMessage::getMessage)
          .toList();

      throw new FhirValidationException("FHIR validation failed: " +
          String.join(", ", errors));
    }
  }
}
```

### Pattern 4: JSONB Storage

```java
@Service
public class FhirResourceService {
  private final IParser fhirParser;
  private final FhirResourceRepository repository;

  public FhirResourceEntity save(IBaseResource fhirResource, String tenantId) {
    // Serialize FHIR resource to JSON
    String resourceJson = fhirParser.encodeResourceToString(fhirResource);

    FhirResourceEntity entity = new FhirResourceEntity();
    entity.setTenantId(tenantId);
    entity.setResourceType(fhirResource.fhirType());
    entity.setResourceId(fhirResource.getIdElement().getIdPart());
    entity.setResourceJson(resourceJson);

    // Denormalize patient reference for queries
    if (fhirResource instanceof DomainResource) {
      extractPatientReference((DomainResource) fhirResource)
          .ifPresent(entity::setPatientReference);
    }

    return repository.save(entity);
  }

  public IBaseResource read(String resourceType, String resourceId, String tenantId) {
    FhirResourceEntity entity = repository.findByTypeAndId(tenantId, resourceType, resourceId)
        .orElseThrow(() -> new ResourceNotFoundException(resourceType, resourceId));

    return fhirParser.parseResource(entity.getResourceJson());
  }
}
```

## Testing Strategy

### Test Case 1: HAPI FHIR Version Consistency

```bash
# Run agent on multi-module project
expected_version="7.6.0"
actual_versions=$(grep -r "hapi.fhir" --include="*.gradle.kts" | grep -oP '\d+\.\d+\.\d+')

# Should report mismatch if any version differs
```

### Test Case 2: Bundle Transaction Validation

```java
@Test
void shouldRequireTransactionalForTransactionBundle() {
  // Setup: Create bundle processor without @Transactional
  // Run agent
  // Assert: CRITICAL violation reported
}
```

### Test Case 3: FHIR Resource Profile Compliance

```java
@Test
void shouldDetectInvalidFhirResource() {
  // Create Patient missing required birthDate
  Patient patient = new Patient();
  patient.addName(new HumanName().setFamily("Doe"));

  // Run FHIR agent validation
  // Assert: Validation error reported
}
```

## Integration Points

### FHIR Service (Port 8085)
Primary FHIR resource management service.

**Endpoints:**
- `GET /api/v1/fhir/{resourceType}/{id}` - Read resource
- `POST /api/v1/fhir/{resourceType}` - Create resource
- `PUT /api/v1/fhir/{resourceType}/{id}` - Update resource
- `DELETE /api/v1/fhir/{resourceType}/{id}` - Delete resource
- `GET /api/v1/fhir/{resourceType}?param=value` - Search resources
- `POST /api/v1/fhir` - Process bundle

### CQL Engine (Port 8081)
Consumes FHIR resources for measure evaluation.

**Integration:**
- CQL libraries query FHIR resources
- Uses FHIR search parameters
- Returns measure evaluation results

### Care Gap Service (Port 8086)
Uses FHIR Observation/Condition for gap detection.

**Integration:**
- Reads FHIR Observations for lab results
- Checks Condition resources for diagnoses
- Creates care gap recommendations

## Related Documentation

- **FHIR Specification:** https://hl7.org/fhir/R4/
- **HAPI FHIR Documentation:** https://hapifhir.io/hapi-fhir/docs/
- **HDIM FHIR Guide:** `docs/FHIR_IMPLEMENTATION_GUIDE.md`
- **Entity-Migration Guide:** `backend/docs/ENTITY_MIGRATION_GUIDE.md`

## Agent Metadata

- **Priority:** HIGH - Ensures interoperability compliance
- **Execution Time:** 10-15 seconds per service
- **Coverage:** FHIR service + 4 clinical services
- **FHIR Resources Supported:** 150+ (Patient, Observation, Condition, Medication, etc.)

---

**Last Updated:** January 21, 2026
**Version:** 1.0.0
**Status:** Production Ready
