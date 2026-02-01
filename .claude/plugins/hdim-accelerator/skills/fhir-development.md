---
name: fhir-development
description: Comprehensive guide to implementing FHIR R4 resources in HDIM using HAPI FHIR 7.x, JSONB storage, and event-driven architecture
---

# FHIR Development Skill

## Overview

This skill provides comprehensive guidance on implementing **FHIR R4 (Fast Healthcare Interoperability Resources)** in the HDIM platform. HDIM uses **HAPI FHIR 7.6.0** for healthcare data interoperability, supporting 150+ FHIR resource types with a unified storage model.

**Key Technologies:**
- FHIR R4 (4.0.1) - HL7 standard for healthcare data exchange
- HAPI FHIR 7.6.0 - Java library for FHIR implementation
- PostgreSQL JSONB - Flexible storage for 150+ resource types
- Event-driven architecture - Kafka-based FHIR resource lifecycle events

## FHIR Architecture in HDIM

### Unified Storage Model

HDIM uses a **single table** (`fhir_resources`) to store all FHIR resource types using PostgreSQL JSONB:

```sql
CREATE TABLE fhir_resources (
  id UUID PRIMARY KEY,
  tenant_id VARCHAR(100) NOT NULL,
  resource_type VARCHAR(50) NOT NULL,    -- "Patient", "Observation", etc.
  resource_id VARCHAR(100) NOT NULL,     -- Business ID
  resource_json JSONB NOT NULL,          -- Full FHIR resource
  patient_reference VARCHAR(200),        -- Denormalized for queries
  last_updated TIMESTAMP NOT NULL,
  version INTEGER NOT NULL,              -- Optimistic locking
  created_at TIMESTAMP NOT NULL,
  deleted_at TIMESTAMP,                  -- Soft delete (HIPAA)
  UNIQUE (tenant_id, resource_type, resource_id)
);

-- Performance indexes
CREATE INDEX idx_fhir_tenant_type ON fhir_resources(tenant_id, resource_type);
CREATE INDEX idx_fhir_patient_ref ON fhir_resources(tenant_id, patient_reference);
CREATE INDEX idx_fhir_last_updated ON fhir_resources(tenant_id, resource_type, last_updated);

-- JSONB query optimization
CREATE INDEX idx_fhir_resource_json_gin ON fhir_resources USING GIN (resource_json);
```

**Advantages:**
- ✅ Single schema supports 150+ FHIR resource types
- ✅ No schema changes when FHIR adds new resource types
- ✅ Fast JSONB queries with GIN indexes
- ✅ Multi-tenant isolation at row level
- ✅ Simplifies backup/restore operations

## HAPI FHIR Setup

### FhirContext Configuration (Singleton)

```java
@Configuration
public class FhirConfig {

  @Bean
  public FhirContext fhirContext() {
    FhirContext ctx = FhirContext.forR4();

    // Performance optimization: disable schema validation
    ctx.getParserOptions().setStripVersionsFromReferences(false);
    ctx.getParserOptions().setOverrideResourceIdWithBundleEntryFullUrl(false);

    // Strict error handling
    ctx.setParserErrorHandler(new StrictErrorHandler());

    return ctx;
  }

  @Bean
  public IParser jsonParser(FhirContext ctx) {
    return ctx.newJsonParser()
        .setPrettyPrint(false)        // Compact JSON for storage
        .setEncodeElements(null)       // Include all elements
        .setDontEncodeElements(null);  // Don't exclude any elements
  }

  @Bean
  public FhirValidator fhirValidator(FhirContext ctx) {
    FhirValidator validator = ctx.newValidator();

    // Use R4 validation module
    IValidatorModule module = new FhirInstanceValidator(ctx);
    validator.registerValidatorModule(module);

    return validator;
  }
}
```

**Performance Tips:**
- Create **one FhirContext per application** (thread-safe singleton)
- Reuse `IParser` instances (thread-safe)
- Cache `FhirValidator` (expensive to create)
- Disable validation in production (validate at API boundary only)

### Gradle Dependencies

```kotlin
// build.gradle.kts
dependencies {
  // HAPI FHIR core libraries
  implementation(libs.hapi.fhir.base)
  implementation(libs.hapi.fhir.structures.r4)
  implementation(libs.hapi.fhir.validation)
  implementation(libs.hapi.fhir.validation.resources.r4)

  // JSONB support
  implementation("org.postgresql:postgresql")
  implementation("com.vladmihalcea:hibernate-types-60")
}
```

## FHIR Resource Entity Pattern

### Unified Entity (All Resource Types)

```java
@Entity
@Table(name = "fhir_resources", indexes = {
  @Index(name = "idx_fhir_tenant_type_id",
         columnList = "tenant_id,resource_type,resource_id", unique = true),
  @Index(name = "idx_fhir_tenant_type_updated",
         columnList = "tenant_id,resource_type,last_updated"),
  @Index(name = "idx_fhir_patient_ref",
         columnList = "tenant_id,patient_reference")
})
@SQLDelete(sql = "UPDATE fhir_resources SET deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class FhirResourceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "tenant_id", nullable = false, updatable = false)
  private String tenantId;

  @Column(name = "resource_type", nullable = false)
  private String resourceType;  // "Patient", "Observation", "Condition"

  @Column(name = "resource_id", nullable = false)
  private String resourceId;    // Business ID (e.g., "patient-12345")

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "resource_json", columnDefinition = "jsonb", nullable = false)
  private String resourceJson;

  @Column(name = "patient_reference")
  private String patientReference;  // "Patient/12345" (denormalized)

  @Column(name = "last_updated", nullable = false)
  private Instant lastUpdated;

  @Version
  @Column(name = "version")
  private Integer version;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
    if (lastUpdated == null) {
      lastUpdated = Instant.now();
    }
  }

  @PreUpdate
  protected void onUpdate() {
    lastUpdated = Instant.now();
  }
}
```

### Repository with FHIR-Specific Queries

```java
public interface FhirResourceRepository extends JpaRepository<FhirResourceEntity, UUID> {

  // Standard FHIR read: GET /fhir/{type}/{id}
  @Query("SELECT f FROM FhirResourceEntity f " +
         "WHERE f.tenantId = :tenantId " +
         "AND f.resourceType = :resourceType " +
         "AND f.resourceId = :resourceId")
  Optional<FhirResourceEntity> findByTypeAndId(
      @Param("tenantId") String tenantId,
      @Param("resourceType") String resourceType,
      @Param("resourceId") String resourceId);

  // Search by patient reference
  @Query("SELECT f FROM FhirResourceEntity f " +
         "WHERE f.tenantId = :tenantId " +
         "AND f.resourceType = :resourceType " +
         "AND f.patientReference = :patientReference " +
         "ORDER BY f.lastUpdated DESC")
  List<FhirResourceEntity> findByPatientReference(
      @Param("tenantId") String tenantId,
      @Param("resourceType") String resourceType,
      @Param("patientReference") String patientReference,
      Pageable pageable);

  // Search by JSONB field (status example)
  @Query(value = "SELECT * FROM fhir_resources " +
                 "WHERE tenant_id = :tenantId " +
                 "AND resource_type = :resourceType " +
                 "AND resource_json->>'status' = :status " +
                 "ORDER BY last_updated DESC",
         nativeQuery = true)
  List<FhirResourceEntity> findByStatus(
      @Param("tenantId") String tenantId,
      @Param("resourceType") String resourceType,
      @Param("status") String status,
      Pageable pageable);

  // Search by date range
  @Query("SELECT f FROM FhirResourceEntity f " +
         "WHERE f.tenantId = :tenantId " +
         "AND f.resourceType = :resourceType " +
         "AND f.lastUpdated BETWEEN :startDate AND :endDate")
  List<FhirResourceEntity> findByDateRange(
      @Param("tenantId") String tenantId,
      @Param("resourceType") String resourceType,
      @Param("startDate") Instant startDate,
      @Param("endDate") Instant endDate);
}
```

## FHIR Service Implementation

### Core FHIR Operations (CRUD)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class FhirResourceService {

  private final FhirResourceRepository repository;
  private final IParser fhirParser;
  private final FhirValidator fhirValidator;
  private final ApplicationEventPublisher eventPublisher;

  // READ: GET /fhir/{type}/{id}
  @Transactional(readOnly = true)
  public IBaseResource read(String resourceType, String resourceId, String tenantId) {
    FhirResourceEntity entity = repository
        .findByTypeAndId(tenantId, resourceType, resourceId)
        .orElseThrow(() -> new ResourceNotFoundException(resourceType, resourceId));

    return fhirParser.parseResource(entity.getResourceJson());
  }

  // CREATE: POST /fhir/{type}
  @Transactional
  public IBaseResource create(IBaseResource resource, String tenantId) {
    // Validate FHIR resource
    ValidationResult validationResult = fhirValidator.validateWithResult(resource);
    if (!validationResult.isSuccessful()) {
      throw new FhirValidationException(validationResult.getMessages());
    }

    // Generate resource ID if missing
    if (resource.getIdElement().isEmpty()) {
      resource.setId(IdType.newRandomUuid());
    }

    // Serialize to JSON
    String resourceJson = fhirParser.encodeResourceToString(resource);

    // Save to database
    FhirResourceEntity entity = new FhirResourceEntity();
    entity.setTenantId(tenantId);
    entity.setResourceType(resource.fhirType());
    entity.setResourceId(resource.getIdElement().getIdPart());
    entity.setResourceJson(resourceJson);

    // Extract patient reference (denormalize for queries)
    extractPatientReference(resource).ifPresent(entity::setPatientReference);

    entity = repository.save(entity);

    // Publish event
    eventPublisher.publishEvent(new FhirResourceCreatedEvent(entity));

    return resource;
  }

  // UPDATE: PUT /fhir/{type}/{id}
  @Transactional
  public IBaseResource update(
      String resourceType, String resourceId, IBaseResource resource, String tenantId) {

    FhirResourceEntity entity = repository
        .findByTypeAndId(tenantId, resourceType, resourceId)
        .orElseThrow(() -> new ResourceNotFoundException(resourceType, resourceId));

    // Validate FHIR resource
    ValidationResult validationResult = fhirValidator.validateWithResult(resource);
    if (!validationResult.isSuccessful()) {
      throw new FhirValidationException(validationResult.getMessages());
    }

    // Update JSON
    String resourceJson = fhirParser.encodeResourceToString(resource);
    entity.setResourceJson(resourceJson);

    // Update patient reference
    extractPatientReference(resource).ifPresent(entity::setPatientReference);

    entity = repository.save(entity);

    // Publish event
    eventPublisher.publishEvent(new FhirResourceUpdatedEvent(entity));

    return resource;
  }

  // DELETE: DELETE /fhir/{type}/{id} (Soft delete for HIPAA)
  @Transactional
  public void delete(String resourceType, String resourceId, String tenantId) {
    FhirResourceEntity entity = repository
        .findByTypeAndId(tenantId, resourceType, resourceId)
        .orElseThrow(() -> new ResourceNotFoundException(resourceType, resourceId));

    repository.delete(entity);  // Soft delete via @SQLDelete

    // Publish event
    eventPublisher.publishEvent(new FhirResourceDeletedEvent(entity));
  }

  // SEARCH: GET /fhir/{type}?param=value
  @Transactional(readOnly = true)
  public Bundle search(String resourceType, Map<String, String> parameters, String tenantId) {
    // Extract standard FHIR search parameters
    String patientId = parameters.get("patient");
    String status = parameters.get("status");
    String code = parameters.get("code");
    Instant lastUpdated = parseInstant(parameters.get("_lastUpdated"));

    List<FhirResourceEntity> results;

    if (patientId != null) {
      results = repository.findByPatientReference(
          tenantId, resourceType, "Patient/" + patientId, Pageable.ofSize(50));
    } else if (status != null) {
      results = repository.findByStatus(tenantId, resourceType, status, Pageable.ofSize(50));
    } else {
      results = repository.findByDateRange(
          tenantId, resourceType, lastUpdated, Instant.now());
    }

    return createSearchResultBundle(results);
  }

  // Helper: Extract patient reference from resource
  private Optional<String> extractPatientReference(IBaseResource resource) {
    if (resource instanceof Patient) {
      return Optional.of("Patient/" + resource.getIdElement().getIdPart());
    }

    if (resource instanceof DomainResource) {
      DomainResource domainResource = (DomainResource) resource;

      // Check subject field (Observation, Condition, etc.)
      if (domainResource.hasSubject()) {
        Reference subject = domainResource.getSubject();
        if (subject.getReference().startsWith("Patient/")) {
          return Optional.of(subject.getReference());
        }
      }
    }

    return Optional.empty();
  }

  // Helper: Create Bundle from search results
  private Bundle createSearchResultBundle(List<FhirResourceEntity> results) {
    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.SEARCHSET);
    bundle.setTotal(results.size());

    for (FhirResourceEntity entity : results) {
      IBaseResource resource = fhirParser.parseResource(entity.getResourceJson());

      Bundle.BundleEntryComponent entry = new Bundle.BundleEntryComponent();
      entry.setResource((Resource) resource);
      entry.setFullUrl("https://hdim.health/fhir/" +
          entity.getResourceType() + "/" + entity.getResourceId());

      bundle.addEntry(entry);
    }

    return bundle;
  }
}
```

## FHIR REST Controller

### Standard FHIR Endpoints

```java
@RestController
@RequestMapping("/api/v1/fhir")
@RequiredArgsConstructor
@Slf4j
public class FhirResourceController {

  private final FhirResourceService fhirService;
  private final IParser fhirParser;

  // READ: GET /fhir/{type}/{id}
  @GetMapping("/{resourceType}/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
  @Audited(action = AuditAction.READ, resourceType = "FHIR", encryptPayload = false)
  public ResponseEntity<String> read(
      @PathVariable String resourceType,
      @PathVariable String id,
      @RequestHeader("X-Tenant-ID") String tenantId) {

    IBaseResource resource = fhirService.read(resourceType, id, tenantId);
    String json = fhirParser.encodeResourceToString(resource);

    return ResponseEntity.ok()
        .header("Content-Type", "application/fhir+json")
        .header("Cache-Control", "no-store, no-cache, must-revalidate")
        .body(json);
  }

  // CREATE: POST /fhir/{type}
  @PostMapping("/{resourceType}")
  @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
  @Audited(action = AuditAction.CREATE, resourceType = "FHIR", encryptPayload = true)
  public ResponseEntity<String> create(
      @PathVariable String resourceType,
      @RequestBody String resourceJson,
      @RequestHeader("X-Tenant-ID") String tenantId) {

    IBaseResource resource = fhirParser.parseResource(resourceType + ".class", resourceJson);
    IBaseResource created = fhirService.create(resource, tenantId);

    String responseJson = fhirParser.encodeResourceToString(created);
    String location = "/api/v1/fhir/" + resourceType + "/" +
        created.getIdElement().getIdPart();

    return ResponseEntity.created(URI.create(location))
        .header("Content-Type", "application/fhir+json")
        .body(responseJson);
  }

  // UPDATE: PUT /fhir/{type}/{id}
  @PutMapping("/{resourceType}/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
  @Audited(action = AuditAction.UPDATE, resourceType = "FHIR", encryptPayload = true)
  public ResponseEntity<String> update(
      @PathVariable String resourceType,
      @PathVariable String id,
      @RequestBody String resourceJson,
      @RequestHeader("X-Tenant-ID") String tenantId) {

    IBaseResource resource = fhirParser.parseResource(resourceType + ".class", resourceJson);
    IBaseResource updated = fhirService.update(resourceType, id, resource, tenantId);

    String responseJson = fhirParser.encodeResourceToString(updated);

    return ResponseEntity.ok()
        .header("Content-Type", "application/fhir+json")
        .body(responseJson);
  }

  // DELETE: DELETE /fhir/{type}/{id}
  @DeleteMapping("/{resourceType}/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  @Audited(action = AuditAction.DELETE, resourceType = "FHIR", encryptPayload = false)
  public ResponseEntity<Void> delete(
      @PathVariable String resourceType,
      @PathVariable String id,
      @RequestHeader("X-Tenant-ID") String tenantId) {

    fhirService.delete(resourceType, id, tenantId);

    return ResponseEntity.noContent().build();
  }

  // SEARCH: GET /fhir/{type}?param=value
  @GetMapping("/{resourceType}")
  @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
  @Audited(action = AuditAction.SEARCH, resourceType = "FHIR", encryptPayload = false)
  public ResponseEntity<String> search(
      @PathVariable String resourceType,
      @RequestParam Map<String, String> parameters,
      @RequestHeader("X-Tenant-ID") String tenantId) {

    Bundle bundle = fhirService.search(resourceType, parameters, tenantId);
    String json = fhirParser.encodeResourceToString(bundle);

    return ResponseEntity.ok()
        .header("Content-Type", "application/fhir+json")
        .body(json);
  }

  // BUNDLE: POST /fhir
  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
  @Audited(action = AuditAction.BATCH, resourceType = "FHIR", encryptPayload = true)
  public ResponseEntity<String> processBundle(
      @RequestBody String bundleJson,
      @RequestHeader("X-Tenant-ID") String tenantId) {

    Bundle bundle = (Bundle) fhirParser.parseResource(bundleJson);
    Bundle response = fhirService.processBundle(bundle, tenantId);

    String responseJson = fhirParser.encodeResourceToString(response);

    return ResponseEntity.ok()
        .header("Content-Type", "application/fhir+json")
        .body(responseJson);
  }
}
```

## Bundle Operations (Batch vs Transaction)

### Transaction Bundle (Atomic)

```java
@Service
public class FhirBundleService {

  @Transactional  // CRITICAL: Enables rollback on failure
  public Bundle processTransactionBundle(Bundle bundle, String tenantId) {
    if (!Bundle.BundleType.TRANSACTION.equals(bundle.getType())) {
      throw new InvalidRequestException("Expected transaction bundle");
    }

    Bundle response = new Bundle();
    response.setType(Bundle.BundleType.TRANSACTIONRESPONSE);

    try {
      for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
        Bundle.BundleEntryComponent responseEntry = processEntry(entry, tenantId);
        response.addEntry(responseEntry);
      }

      return response;
    } catch (Exception e) {
      // Rollback handled automatically by @Transactional
      throw new BundleProcessingException("Transaction bundle failed: " + e.getMessage(), e);
    }
  }

  // Batch Bundle (Independent execution)
  public Bundle processBatchBundle(Bundle bundle, String tenantId) {
    Bundle response = new Bundle();
    response.setType(Bundle.BundleType.BATCHRESPONSE);

    for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
      try {
        Bundle.BundleEntryComponent responseEntry = processEntry(entry, tenantId);
        response.addEntry(responseEntry);
      } catch (Exception e) {
        // Log error but continue processing other entries
        Bundle.BundleEntryComponent errorEntry = new Bundle.BundleEntryComponent();
        errorEntry.getResponse()
            .setStatus("400 Bad Request")
            .setOutcome(createOperationOutcome(e.getMessage()));
        response.addEntry(errorEntry);
      }
    }

    return response;
  }
}
```

## FHIR Events (Kafka Integration)

### Event Schema

```java
public record FhirResourceCreatedEvent(
    UUID eventId,
    String tenantId,
    String resourceType,
    String resourceId,
    String resourceJson,
    String patientReference,
    Instant occurredAt
) {}

public record FhirResourceUpdatedEvent(
    UUID eventId,
    String tenantId,
    String resourceType,
    String resourceId,
    String resourceJson,
    String previousVersion,
    Integer newVersion,
    Instant occurredAt
) {}

public record FhirResourceDeletedEvent(
    UUID eventId,
    String tenantId,
    String resourceType,
    String resourceId,
    Instant occurredAt
) {}
```

### Event Publisher

```java
@Component
@RequiredArgsConstructor
public class FhirEventPublisher {

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;

  @EventListener
  public void handleFhirResourceCreated(FhirResourceCreatedEvent event) {
    String topic = "fhir-resource-events";
    String key = event.tenantId() + ":" + event.resourceType() + ":" + event.resourceId();

    try {
      String payload = objectMapper.writeValueAsString(event);
      kafkaTemplate.send(topic, key, payload);
    } catch (JsonProcessingException e) {
      log.error("Failed to publish FHIR resource created event", e);
    }
  }
}
```

## Common FHIR Patterns

### Pattern: Patient Resource Mapping

```java
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

    // Gender
    if (entity.getGender() != null) {
      patient.setGender(Enumerations.AdministrativeGender.valueOf(entity.getGender()));
    }

    // Identifier (MRN)
    Identifier identifier = new Identifier()
        .setSystem("http://hdim.health/mrn")
        .setValue(entity.getMrn());
    patient.addIdentifier(identifier);

    // Active status
    patient.setActive(entity.isActive());

    return patient;
  }
}
```

### Pattern: Observation Resource

```java
public class ObservationMapper {

  public Observation toFhir(ObservationEntity entity) {
    Observation observation = new Observation();
    observation.setId(entity.getResourceId());

    // Status (required)
    observation.setStatus(Observation.ObservationStatus.FINAL);

    // Code (LOINC)
    CodeableConcept code = new CodeableConcept();
    code.addCoding()
        .setSystem("http://loinc.org")
        .setCode(entity.getLoincCode())
        .setDisplay(entity.getDisplayName());
    observation.setCode(code);

    // Subject (patient reference)
    Reference subject = new Reference("Patient/" + entity.getPatientId());
    observation.setSubject(subject);

    // Effective date
    observation.setEffective(new DateTimeType(Date.from(entity.getEffectiveDate())));

    // Value (Quantity)
    Quantity value = new Quantity()
        .setValue(entity.getValue())
        .setUnit(entity.getUnit())
        .setSystem("http://unitsofmeasure.org")
        .setCode(entity.getUnitCode());
    observation.setValue(value);

    return observation;
  }
}
```

## Testing FHIR Implementation

### Unit Test Example

```java
@SpringBootTest
class FhirResourceServiceTest {

  @Autowired
  private FhirResourceService fhirService;

  @Autowired
  private IParser fhirParser;

  @Test
  void shouldCreatePatientResource() {
    // Given: Valid FHIR Patient
    Patient patient = new Patient();
    patient.addName(new HumanName().setFamily("Doe").addGiven("John"));
    patient.setBirthDate(Date.from(Instant.parse("1980-01-15T00:00:00Z")));

    // When: Create resource
    IBaseResource created = fhirService.create(patient, "tenant-001");

    // Then: Resource persisted
    assertThat(created).isNotNull();
    assertThat(created.getIdElement().getIdPart()).isNotEmpty();
  }

  @Test
  void shouldValidateFhirResource() {
    // Given: Invalid Patient (missing required field)
    Patient patient = new Patient();
    // Missing name (required)

    // When/Then: Validation fails
    assertThatThrownBy(() -> fhirService.create(patient, "tenant-001"))
        .isInstanceOf(FhirValidationException.class);
  }
}
```

## Related Documentation

- **FHIR R4 Specification:** https://hl7.org/fhir/R4/
- **HAPI FHIR Documentation:** https://hapifhir.io/hapi-fhir/docs/
- **FHIR Agent:** `.claude/plugins/hdim-accelerator/agents/fhir-agent.md`
- **Entity-Migration Guide:** `backend/docs/ENTITY_MIGRATION_GUIDE.md`

---

**Last Updated:** January 21, 2026
**Version:** 1.0.0
**FHIR Compliance:** R4 (4.0.1)
