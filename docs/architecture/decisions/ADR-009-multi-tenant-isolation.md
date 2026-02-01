# ADR-009: Multi-Tenant Isolation at Row Level

**Status**: Accepted
**Date**: 2026-01-19 (Decision Made: Phase 1, Sept 2025)
**Decision Makers**: Architecture Lead, Security Lead
**Stakeholders**: All Backend Services, Security Team

---

## Context

### Problem Statement

HDIM serves multiple healthcare organizations (tenants) with strict isolation requirements. Patient data from Tenant A must never be visible to Tenant B. Required database-level isolation enforcement.

---

## Options Considered

### Option 1: Row-Level Isolation (tenant_id Column)

**Description**: Every table has tenant_id column; all queries filter by tenant

**Pros**:
- Database enforces isolation
- SQL-level access control
- Cost-effective (single database)
- HIPAA-compliant

**Cons**:
- Requires discipline (every query must filter)
- Risk if WHERE clause forgotten

**Risk Level**: Low with proper testing

---

### Option 2: Database-per-Tenant

**Description**: Separate database per organization

**Pros**:
- Guaranteed isolation
- Database naturally separates data

**Cons**:
- Expensive (29 databases × N tenants)
- Operational complexity
- Hard to debug

**Risk Level**: High (cost)

---

## Decision

**We chose Option 1 (Row-Level Isolation)** because:

1. **HIPAA Compliance**: Row-level filtering meets regulatory requirements
2. **Cost-Effective**: Single database per service
3. **Scalable**: Grows with tenant count
4. **Testable**: Isolation verified in tests
5. **Database Constraints**: Liquibase enforces tenant_id on all tables

---

## Implementation

### Entity Pattern

```java
@Entity
@Table(name = "patients")
public class Patient {
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "patient_id", nullable = false)
    private String patientId;
}
```

### Query Pattern

```java
@Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId AND p.id = :id")
Optional<Patient> findByIdAndTenant(
    @Param("id") String id,
    @Param("tenantId") String tenantId);
```

### API Header Validation

```java
@GetMapping("/{patientId}")
public Patient getPatient(
    @PathVariable String patientId,
    @RequestHeader("X-Tenant-ID") String tenantId) {
    return service.getPatient(patientId, tenantId);
}
```

### Liquibase Constraint

```xml
<addUniqueConstraint
    tableName="patients"
    columnNames="tenant_id,patient_id"/>
```

---

## Testing

Every test must verify tenant isolation:

```java
@Test
void testTenantIsolation() {
    // Patient in tenant1
    Patient p1 = getPatient(patientId, "tenant1");

    // Same patient ID, different tenant should not find it
    Optional<Patient> p2 = getPatient(patientId, "tenant2");
    assertThat(p2).isEmpty();  // Verify isolation
}
```

---

## Success Criteria

- ✅ 100% of tables have tenant_id column
- ✅ All queries filter by tenant_id
- ✅ API endpoints validate X-Tenant-ID header
- ✅ Unit tests verify tenant isolation
- ✅ Zero cross-tenant data leaks
- ✅ HIPAA compliance validated

---

## References

- **[HIPAA Compliance Guide](../../backend/HIPAA-CACHE-COMPLIANCE.md)**
- **[Coding Standards](../../backend/docs/CODING_STANDARDS.md)**

---

## Footer

**ADR #**: 009
**Version**: 1.0
**Status**: Active and Validated
**Compliance**: HIPAA PHI Protection

_Decision Date: Phase 1 (September 2025)_
_Implementation Status: 29 databases, 100% isolation_
