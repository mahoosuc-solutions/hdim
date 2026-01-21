---
description: Generate FHIR R4 resource endpoint with HAPI FHIR integration
arguments:
  service_name:
    description: Service name (typically fhir-service)
    type: string
    required: true
  resource_type:
    description: FHIR R4 resource type (e.g., Patient, Observation, Condition)
    type: string
    required: true
---

# FHIR Resource Command

Generate a complete FHIR R4 resource endpoint with HAPI FHIR 7.x integration patterns.

## What This Command Does

1. **Generates FHIR Controller** - REST endpoints for FHIR operations (CRUD + Search)
2. **Creates FHIR Service** - Business logic with HAPI FHIR client
3. **Adds Entity/Repository** - Database persistence for FHIR resources
4. **Creates Test Fixtures** - Sample FHIR JSON for testing
5. **Generates Integration Tests** - Testcontainers-based tests with mock FHIR server

## Usage

```bash
/fhir-resource {{service_name}} {{resource_type}}
```

## Examples

```bash
# Add Patient resource endpoint
/fhir-resource fhir-service Patient

# Add Observation resource endpoint
/fhir-resource fhir-service Observation

# Add Condition resource endpoint
/fhir-service Condition
```

## Supported FHIR Resources

- Patient, Practitioner, Organization
- Observation, Condition, Procedure
- MedicationRequest, MedicationStatement
- Encounter, DiagnosticReport
- Immunization, AllergyIntolerance
- Consent, DocumentReference

## Implementation

Generate FHIR R4 resource endpoint following HAPI FHIR 7.x patterns.

### Step 1: Generate FHIR Controller

```java
package com.healthdata.fhir.api.v1;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import com.healthdata.fhir.service.{{ResourceType}}Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.{{ResourceType}};
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * FHIR R4 {{ResourceType}} resource endpoint.
 *
 * Implements FHIR REST operations:
 * - Read: GET /{{ResourceType}}/{id}
 * - Create: POST /{{ResourceType}}
 * - Update: PUT /{{ResourceType}}/{id}
 * - Delete: DELETE /{{ResourceType}}/{id}
 * - Search: GET /{{ResourceType}}?param=value
 */
@RestController
@RequestMapping("/fhir/{{ResourceType}}")
@RequiredArgsConstructor
@Validated
@Slf4j
public class {{ResourceType}}Controller {

    private final {{ResourceType}}Service service;
    private final FhirContext fhirContext = FhirContext.forR4();

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'ANALYST', 'VIEWER')")
    public ResponseEntity<String> read(
            @PathVariable String id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Reading {{ResourceType}} with id={} for tenant={}", id, tenantId);

        {{ResourceType}} resource = service.read(id, tenantId);
        String json = fhirContext.newJsonParser().encodeResourceToString(resource);
        return ResponseEntity.ok(json);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<String> create(
            @RequestBody String resourceJson,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-Auth-User-Id") String userId) {
        log.debug("Creating {{ResourceType}} for tenant={}", tenantId);

        {{ResourceType}} resource = fhirContext.newJsonParser().parseResource({{ResourceType}}.class, resourceJson);
        MethodOutcome outcome = service.create(resource, tenantId, userId);

        String responseJson = fhirContext.newJsonParser().encodeResourceToString(({{ResourceType}}) outcome.getResource());
        return ResponseEntity.status(HttpStatus.CREATED).body(responseJson);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<String> update(
            @PathVariable String id,
            @RequestBody String resourceJson,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-Auth-User-Id") String userId) {
        log.debug("Updating {{ResourceType}} with id={} for tenant={}", id, tenantId);

        {{ResourceType}} resource = fhirContext.newJsonParser().parseResource({{ResourceType}}.class, resourceJson);
        resource.setId(id);

        MethodOutcome outcome = service.update(resource, tenantId, userId);
        String responseJson = fhirContext.newJsonParser().encodeResourceToString(({{ResourceType}}) outcome.getResource());
        return ResponseEntity.ok(responseJson);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Deleting {{ResourceType}} with id={} for tenant={}", id, tenantId);

        service.delete(id, tenantId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'ANALYST', 'VIEWER')")
    public ResponseEntity<String> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String identifier,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Searching {{ResourceType}}s for tenant={}", tenantId);

        Bundle bundle = service.search(name, identifier, tenantId);
        String json = fhirContext.newJsonParser().encodeResourceToString(bundle);
        return ResponseEntity.ok(json);
    }
}
```

### Step 2: Generate FHIR Service

```java
package com.healthdata.fhir.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.healthdata.fhir.domain.model.Fhir{{ResourceType}}Entity;
import com.healthdata.fhir.domain.repository.Fhir{{ResourceType}}Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.{{ResourceType}};
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class {{ResourceType}}Service {

    private final Fhir{{ResourceType}}Repository repository;

    public {{ResourceType}} read(String id, String tenantId) {
        Fhir{{ResourceType}}Entity entity = repository.findByFhirIdAndTenant(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("{{ResourceType}}/" + id));

        return parseResource(entity.getResourceJson());
    }

    @Transactional
    public MethodOutcome create({{ResourceType}} resource, String tenantId, String userId) {
        String fhirId = UUID.randomUUID().toString();
        resource.setId(fhirId);

        Fhir{{ResourceType}}Entity entity = Fhir{{ResourceType}}Entity.builder()
                .fhirId(fhirId)
                .tenantId(tenantId)
                .resourceJson(serializeResource(resource))
                .createdBy(userId)
                .createdAt(Instant.now())
                .build();

        repository.save(entity);

        MethodOutcome outcome = new MethodOutcome();
        outcome.setId(new IdType("{{ResourceType}}", fhirId));
        outcome.setResource(resource);
        outcome.setCreated(true);
        return outcome;
    }

    @Transactional
    public MethodOutcome update({{ResourceType}} resource, String tenantId, String userId) {
        String fhirId = resource.getIdElement().getIdPart();

        Fhir{{ResourceType}}Entity entity = repository.findByFhirIdAndTenant(fhirId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("{{ResourceType}}/" + fhirId));

        entity.setResourceJson(serializeResource(resource));
        entity.setUpdatedBy(userId);
        entity.setUpdatedAt(Instant.now());
        repository.save(entity);

        MethodOutcome outcome = new MethodOutcome();
        outcome.setId(new IdType("{{ResourceType}}", fhirId));
        outcome.setResource(resource);
        outcome.setCreated(false);
        return outcome;
    }

    @Transactional
    public void delete(String id, String tenantId) {
        Fhir{{ResourceType}}Entity entity = repository.findByFhirIdAndTenant(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("{{ResourceType}}/" + id));

        repository.delete(entity);
    }

    public Bundle search(String name, String identifier, String tenantId) {
        List<Fhir{{ResourceType}}Entity> entities;

        if (name != null) {
            entities = repository.searchByName(name, tenantId);
        } else if (identifier != null) {
            entities = repository.searchByIdentifier(identifier, tenantId);
        } else {
            entities = repository.findAllByTenant(tenantId);
        }

        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal(entities.size());

        entities.forEach(entity -> {
            {{ResourceType}} resource = parseResource(entity.getResourceJson());
            bundle.addEntry()
                    .setFullUrl("{{ResourceType}}/" + entity.getFhirId())
                    .setResource(resource);
        });

        return bundle;
    }

    private String serializeResource({{ResourceType}} resource) {
        return FhirContext.forR4().newJsonParser().encodeResourceToString(resource);
    }

    private {{ResourceType}} parseResource(String json) {
        return FhirContext.forR4().newJsonParser().parseResource({{ResourceType}}.class, json);
    }
}
```

### Step 3: Create Test Fixture

Generate sample FHIR JSON:

```json
{
  "resourceType": "{{ResourceType}}",
  "id": "example-{{resource_type}}-001",
  "meta": {
    "versionId": "1",
    "lastUpdated": "2024-01-20T10:00:00Z"
  },
  "text": {
    "status": "generated",
    "div": "<div>Sample {{ResourceType}}</div>"
  }
  // TODO: Add resource-specific fields based on FHIR R4 specification
}
```

### Step 4: Summary

```
✅ FHIR {{ResourceType}} resource created successfully!

**Files Created:**
- Controller: {{ResourceType}}Controller.java
- Service: {{ResourceType}}Service.java
- Test Fixture: test/resources/fhir/{{ResourceType}}-example.json

**Entity Required:**
Run: /add-entity {{service_name}} Fhir{{ResourceType}}Entity

**Endpoints:**
- GET /fhir/{{ResourceType}}/{id} - Read resource
- POST /fhir/{{ResourceType}} - Create resource
- PUT /fhir/{{ResourceType}}/{id} - Update resource
- DELETE /fhir/{{ResourceType}}/{id} - Delete resource
- GET /fhir/{{ResourceType}}?param=value - Search resources

**Next Steps:**
1. Create entity: /add-entity {{service_name}} Fhir{{ResourceType}}Entity
2. Add resource-specific fields to test fixture
3. Implement custom search parameters
4. Add validation logic
5. Test with sample FHIR data

**FHIR Documentation:**
- Specification: https://hl7.org/fhir/R4/{{resource_type}}.html
- HAPI FHIR: https://hapifhir.io/hapi-fhir/docs/
```

## Related Skills

- `fhir-development` - HAPI FHIR patterns
- `hipaa-compliance` - PHI handling for FHIR resources
