---
name: hdim-dev
description: HDIM development workflow with Serena memory integration
category: hdim
when_to_use: |
  Use this skill when working on HDIM development tasks that require:
  - HIPAA compliance guidance
  - Gateway trust authentication patterns
  - Multi-tenant isolation implementation
  - Entity-migration synchronization
  - Service-specific development
---

# HDIM Development Skill

Comprehensive development workflow for HDIM with integrated Serena memories and validation.

## Skill Overview

This skill provides context-aware development assistance for HDIM, automatically referencing Serena memories for patterns, compliance requirements, and best practices.

## When to Use

Invoke this skill when:
- Implementing new HDIM features
- Working with PHI data
- Creating new services or controllers
- Modifying database entities
- Debugging authentication issues
- Implementing multi-tenant features

## Skill Context

### Available Serena Memories

The skill has access to:

1. **HIPAA Compliance Checklist** (9.5K)
   - Cache TTL requirements
   - HTTP headers for PHI
   - Audit logging patterns
   - Pre-commit checklist

2. **Gateway Trust Authentication** (13K)
   - Authentication flow
   - Header injection
   - Filter configuration
   - HMAC signing

3. **Entity-Migration Sync** (15K)
   - Column type mapping
   - Entity patterns
   - Migration templates
   - Validation testing

4. **Common Patterns** (21K)
   - Controller patterns
   - Service patterns
   - Repository patterns
   - Testing patterns

5. **Service Registry** (13K)
   - All 28 services
   - Ports and dependencies
   - Build commands

6. **Architecture Overview** (5.2K)
   - Tech stack
   - Request flow
   - Core services

7. **Troubleshooting Guide** (13K)
   - Common issues
   - Debug procedures
   - Performance tips

### Validation Tools

The skill can run:
- HIPAA compliance checker
- Multi-tenant query validator
- Entity-migration validator
- Service health checker

## Workflow Steps

### 1. Understand the Task

**Ask**:
- What feature are you implementing?
- Does it involve PHI data?
- Which service(s) are affected?
- Are database changes required?

### 2. Reference Relevant Memories

**For PHI-related work**:
```
Read: .serena/memories/hipaa-compliance-checklist.md
- Verify cache TTL requirements
- Check header requirements
- Review audit logging needs
```

**For authentication**:
```
Read: .serena/memories/gateway-trust-auth.md
- Understand Gateway Trust pattern
- Check filter configuration
- Verify header usage
```

**For database changes**:
```
Read: .serena/memories/entity-migration-sync.md
- Review column type mapping
- Follow entity pattern
- Plan migration creation
```

**For implementation**:
```
Read: .serena/memories/common-patterns.md
- Follow controller pattern
- Use service pattern
- Apply repository pattern
```

### 3. Implement with Patterns

Apply the appropriate pattern from common-patterns.md:

**Controller Example**:
```java
@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
@Validated
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'VIEWER')")
    public ResponseEntity<ResourceResponse> getResource(
            @PathVariable String id,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        ResourceResponse resource = resourceService.getResource(id, tenantId);

        // PHI response headers
        return ResponseEntity.ok()
            .header("Cache-Control", "no-store, no-cache, must-revalidate, private")
            .header("Pragma", "no-cache")
            .header("Expires", "0")
            .body(resource);
    }
}
```

**Service Example**:
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final AuditService auditService;

    @Cacheable(value = "resourceData", key = "#resourceId")
    public ResourceResponse getResource(String resourceId, String tenantId) {
        log.debug("Fetching resource: {} for tenant: {}", resourceId, tenantId);

        Resource resource = resourceRepository.findByIdAndTenant(resourceId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Resource", resourceId));

        auditService.logAccess("PHI_ACCESS", "Resource", resourceId, "VIEW");

        return resourceMapper.toResponse(resource);
    }
}
```

**Repository Example**:
```java
@Repository
public interface ResourceRepository extends JpaRepository<Resource, UUID> {

    @Query("SELECT r FROM Resource r WHERE r.id = :id AND r.tenantId = :tenantId")
    Optional<Resource> findByIdAndTenant(
        @Param("id") String id,
        @Param("tenantId") String tenantId
    );
}
```

### 4. Validate Implementation

Run validation tools:

```bash
# HIPAA compliance
/hdim-validate hipaa

# Multi-tenant queries
/hdim-validate tenant

# Entity-migration sync
/hdim-validate entity
```

### 5. Pre-Commit Check

Before committing:

```bash
# Run all validations
/hdim-validate all

# Or use pre-commit workflow
bash .serena/workflows/pre-commit-check.sh
```

## Example Workflows

### Workflow 1: Add Patient Endpoint

**Task**: Add endpoint to retrieve patient care gaps

**Steps**:
1. Read HIPAA compliance checklist (PHI involved)
2. Read common patterns (controller/service pattern)
3. Implement controller with proper annotations
4. Add cache-control headers
5. Implement service with tenant filtering
6. Add audit logging
7. Run HIPAA validation
8. Run multi-tenant validation

### Workflow 2: Create New Entity

**Task**: Create new Prescription entity

**Steps**:
1. Read entity-migration sync guide
2. Create JPA entity with proper annotations
3. Create Liquibase migration
4. Add to changelog-master.xml
5. Run entity-migration validation
6. Verify ddl-auto: validate

### Workflow 3: Implement Authentication

**Task**: Add authentication to new service

**Steps**:
1. Read gateway-trust-auth guide
2. Configure TrustedHeaderAuthFilter
3. Configure TrustedTenantAccessFilter
4. Add SecurityFilterChain
5. Test with X-Auth-* headers
6. Verify no JWT re-validation

## Validation Checklist

Before marking task complete:

- [ ] HIPAA: Cache TTL ≤ 5 minutes for PHI
- [ ] HIPAA: Cache-Control headers on PHI endpoints
- [ ] HIPAA: @Audited annotation on PHI access
- [ ] Multi-tenant: ALL queries filter by tenantId
- [ ] Auth: @PreAuthorize on all endpoints
- [ ] Auth: Using TrustedHeaderAuthFilter
- [ ] Database: Entity matches migration
- [ ] Database: ddl-auto is 'validate'
- [ ] Tests: EntityMigrationValidationTest passes
- [ ] Logging: No PHI in log statements

## Integration with Mahoosuc Commands

This skill integrates with:

- `/hdim-validate` - Run validation checks
- `/hdim-memory` - Access specific memories
- `/hdim-service` - Manage services
- `/dev:implement` - General development
- `/testing:*` - Testing automation
- `/commit` - Git commit with validation

## Quick Reference

### Common Tasks

| Task | Commands |
|------|----------|
| Start development | `/hdim-service start` → `/hdim-service health` |
| Check HIPAA | `/hdim-validate hipaa` |
| View patterns | `/hdim-memory patterns` |
| Create service | `/hdim-service-create <name> <port>` |
| Pre-commit | `/hdim-validate all` |

### Memory Quick Access

| Need | Memory |
|------|--------|
| HIPAA guidance | `/hdim-memory hipaa` |
| Auth patterns | `/hdim-memory auth` |
| Database changes | `/hdim-memory entity` |
| Code patterns | `/hdim-memory patterns` |
| Service info | `/hdim-memory services` |
| Debugging | `/hdim-memory troubleshooting` |

## Success Criteria

Development task is complete when:

1. ✅ Implementation follows common patterns
2. ✅ HIPAA compliance validated
3. ✅ Multi-tenant isolation verified
4. ✅ Entity-migration sync confirmed
5. ✅ All validation checks pass
6. ✅ Tests written and passing
7. ✅ Documentation updated

## Resources

- Serena Memories: `.serena/memories/`
- Validation Tools: `.serena/tools/`
- Workflows: `.serena/workflows/`
- Service Configs: `backend/modules/services/[service]/.serena/`
- CLAUDE.md: Complete coding guidelines
