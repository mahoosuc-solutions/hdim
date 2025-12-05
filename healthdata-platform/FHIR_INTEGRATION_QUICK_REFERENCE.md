# FHIR Integration Service - Quick Reference Guide

## Service Location
**Main Class**: `com.healthdata.fhir.service.FhirIntegrationService`
**File Path**: `src/main/java/com/healthdata/fhir/service/FhirIntegrationService.java`

## Core Methods Quick Lookup

### Import Operations
| Method | Purpose | Returns |
|--------|---------|---------|
| `importObservations(patientId, observations, tenantId)` | Import FHIR observations | `List<Observation>` |
| `importConditions(patientId, conditions, tenantId)` | Import FHIR conditions | `List<Condition>` |
| `importMedications(patientId, medications, tenantId)` | Import FHIR medications | `List<MedicationRequest>` |

### Export Operations
| Method | Purpose | Returns |
|--------|---------|---------|
| `exportPatientResources(patientId, tenantId)` | Export all resources as FHIR Bundle | `FhirBundle` |
| `batchImport(tenantId, bundle)` | Batch import FHIR Bundle | `BundleResult` |

### Validation Operations
| Method | Purpose | Returns |
|--------|---------|---------|
| `validateObservation(obs)` | Validate FHIR Observation | `ValidationResult` |
| `validateCondition(cond)` | Validate FHIR Condition | `ValidationResult` |
| `validateMedication(med)` | Validate FHIR MedicationRequest | `ValidationResult` |
| `validateBundle(bundle)` | Validate FHIR Bundle | `ValidationResult` |

### Code System Mappings
| Method | Purpose | Returns |
|--------|---------|---------|
| `mapLoincCode(code)` | Map LOINC observation code | `CodeSystemMapping` |
| `mapSnomedCode(code)` | Map SNOMED condition code | `CodeSystemMapping` |
| `mapRxNormCode(code)` | Map RxNorm medication code | `CodeSystemMapping` |
| `mapCustomCodeSystem(system, code)` | Map custom code system | `CodeSystemMapping` |

## Code System Constants

```java
FhirIntegrationService.LOINC_SYSTEM = "http://loinc.org"
FhirIntegrationService.SNOMED_SYSTEM = "http://snomed.info/sct"
FhirIntegrationService.RXNORM_SYSTEM = "http://www.nlm.nih.gov/research/umls/rxnorm"
FhirIntegrationService.ICD10_SYSTEM = "http://hl7.org/fhir/sid/icd-10-cm"
```

## Common LOINC Codes

| Code | Description | Category |
|------|-------------|----------|
| 85354-9 | Blood Pressure | vital-signs |
| 8867-4 | Heart Rate | vital-signs |
| 8310-5 | Body Temperature | vital-signs |
| 2345-7 | Glucose | laboratory |
| 4548-4 | Hemoglobin A1c | laboratory |

## Common SNOMED Codes

| Code | Description |
|------|-------------|
| 44054006 | Diabetes Mellitus |
| 38341003 | Hypertension |
| 13645005 | COPD |
| 49436004 | Atrial Fibrillation |
| 53741008 | Congestive Heart Failure |

## Common RxNorm Codes

| Code | Description |
|------|-------------|
| 849574 | Lisinopril 10 MG Oral Tablet |
| 1000001 | Metformin 500 MG Oral Tablet |

## Dependency Injection

```java
@Autowired
private FhirIntegrationService fhirIntegrationService;
```

## Service Auto-Configuration

The service is automatically registered as a Spring Bean due to `@Service` annotation:
```java
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class FhirIntegrationService { ... }
```

## Data Models

### FhirBundle
```java
FhirIntegrationService.FhirBundle bundle = FhirIntegrationService.FhirBundle.builder()
    .resourceType("Bundle")
    .type("batch")
    .total(count)
    .entries(entries)
    .timestamp(LocalDateTime.now())
    .build();
```

### ValidationResult
```java
ValidationResult result = ValidationResult.builder()
    .valid(true)
    .errors(new ArrayList<>())
    .build();

// Check validity
if (!result.isValid()) {
    result.getErrors().forEach(System.err::println);
}
```

### CodeSystemMapping
```java
CodeSystemMapping mapping = CodeSystemMapping.builder()
    .code("2345-7")
    .system(LOINC_SYSTEM)
    .display("Glucose")
    .category("laboratory")
    .build();
```

### BundleResult
```java
BundleResult result = BundleResult.builder()
    .bundleId(UUID.randomUUID().toString())
    .tenantId(tenantId)
    .totalEntries(count)
    .build();

result.addError("Observation", "Missing required field: code");
```

## Usage Examples

### Example 1: Import Observations
```java
// Prepare observations
List<Map<String, Object>> observations = new ArrayList<>();
Map<String, Object> obsMap = new HashMap<>();
obsMap.put("resourceType", "Observation");
obsMap.put("code", "2345-7");
obsMap.put("status", "final");
observations.add(obsMap);

// Import
List<Observation> imported = fhirIntegrationService.importObservations(
    "patient-123",
    observations,
    "tenant-456"
);

System.out.println("Imported: " + imported.size());
```

### Example 2: Validate Before Import
```java
Map<String, Object> obs = parseJson(jsonString);

ValidationResult validation = fhirIntegrationService.validateObservation(obs);
if (validation.isValid()) {
    List<Observation> imported = fhirIntegrationService.importObservations(
        patientId,
        List.of(obs),
        tenantId
    );
} else {
    validation.getErrors().forEach(err -> logger.error(err));
}
```

### Example 3: Export and Serialize Bundle
```java
FhirBundle bundle = fhirIntegrationService.exportPatientResources(
    patientId,
    tenantId
);

String bundleJson = objectMapper.writeValueAsString(bundle);
response.setBody(bundleJson);
```

### Example 4: Batch Import with Error Handling
```java
FhirBundle bundle = FhirIntegrationService.FhirBundle.builder()
    .resourceType("Bundle")
    .type("batch")
    .total(entries.size())
    .entries(entries)
    .timestamp(LocalDateTime.now())
    .build();

BundleResult result = fhirIntegrationService.batchImport(tenantId, bundle);

logger.info("Import Complete:");
logger.info("Total: {}", result.getTotalEntries());
logger.info("Success: {}", result.getSuccessCount());
logger.info("Errors: {}", result.getErrorCount());

if (!result.getErrors().isEmpty()) {
    result.getErrors().forEach(err -> logger.warn(err));
}
```

### Example 5: Code System Mapping
```java
// Get LOINC mapping
CodeSystemMapping glucose = fhirIntegrationService.mapLoincCode("2345-7");
System.out.println(glucose.getDisplay()); // "Glucose [Mass/volume]..."

// Get SNOMED mapping
CodeSystemMapping diabetes = fhirIntegrationService.mapSnomedCode("44054006");
System.out.println(diabetes.getDisplay()); // "Diabetes mellitus"

// Map custom code
CodeSystemMapping custom = fhirIntegrationService.mapCustomCodeSystem(
    "http://mycompany.org/codes",
    "MY-CODE-123"
);
```

## Transaction Management

All service methods use `@Transactional` by default:
- Create/Update operations: Full transaction with rollback on error
- Read operations: `readOnly = true` where applicable
- Batch operations: Single transaction for all entries

## Error Handling

### Validation Errors
Validation methods return `ValidationResult` with error list:
```java
if (!result.isValid()) {
    List<String> errors = result.getErrors();
    errors.forEach(err -> {
        // Handle: "Invalid resourceType - expected 'Observation'"
        // Handle: "Missing required field: code"
        // etc.
    });
}
```

### Import Errors
Invalid resources are skipped with logging:
```java
// In import methods:
if (!validation.isValid()) {
    log.warn("Skipping invalid observation: {}", validation.getErrors());
    return null; // Filtered out from result list
}
```

### Batch Errors
Batch operations collect errors:
```java
BundleResult result = fhirIntegrationService.batchImport(tenantId, bundle);
if (result.getErrorCount() > 0) {
    result.getErrors().forEach(logger::warn);
}
```

## Performance Notes

- Import methods filter invalid entries before persistence
- Batch import uses single transaction for all entries
- Export operations use read-only queries
- Code system mappings use static lookup maps
- Service is cacheable via Spring Cache annotations

## Testing

Run tests:
```bash
./gradlew test --tests "FhirIntegrationServiceTest"
```

Test count: **39 tests** (100% pass rate)

### Test Categories
- Import Operations: 12 tests
- Export Operations: 3 tests
- Batch Operations: 3 tests
- Validation Operations: 14 tests
- Code System Mappings: 8 tests

## Logging

Service uses SLF4J via Lombok `@Slf4j`:

```java
log.debug("Fetching observations for patient: {}", patientId);
log.info("Importing {} observations for patient: {}", observations.size(), patientId);
log.warn("Skipping invalid observation: {}", validation.getErrors());
log.error("Error processing bundle entry", exception);
```

## Multi-tenancy

All operations enforce tenant isolation:
```java
// All imports require tenantId
fhirIntegrationService.importObservations(patientId, observations, tenantId);

// All exports require tenantId
fhirIntegrationService.exportPatientResources(patientId, tenantId);

// All batch operations require tenantId
fhirIntegrationService.batchImport(tenantId, bundle);
```

## Integration Points

### With Repositories
Service uses:
- `ObservationRepository`
- `ConditionRepository`
- `MedicationRequestRepository`

All extending `JpaRepository` for persistence.

### With ObjectMapper
Uses Spring's `ObjectMapper` (Jackson) for:
- JSON serialization of FHIR resources
- JSON deserialization for import

### With Spring Data JPA
- `@Transactional` annotation for transaction management
- Repository abstraction for data access
- Automatic flush and commit

## API Reference

All public methods are fully documented with JavaDoc:
```java
/**
 * Import observations for a patient
 *
 * @param patientId Patient identifier
 * @param observations List of FHIR observation objects (as maps)
 * @param tenantId Tenant identifier
 * @return List of persisted Observation entities
 */
public List<Observation> importObservations(
    String patientId,
    List<Map<String, Object>> observations,
    String tenantId)
```

## Version Information

- **Version**: 1.0.0
- **Spring Boot**: 3.3.5
- **Java**: 21
- **Status**: Production Ready

---

For detailed documentation, see: `FHIR_INTEGRATION_SERVICE_SUMMARY.md`
