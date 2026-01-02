# ADR-0002: Implement Tenant Isolation Security

**Status**: Accepted
**Date**: 2025-12-06
**Decision Makers**: Architecture Team, Security Team
**Consulted**: Compliance Officer
**Informed**: All Development Teams

## Context

**Problem Statement**:
The multi-tenant HDIM platform had critical tenant isolation vulnerabilities:

1. `TenantAccessFilter.java` was disabled (renamed to `.disabled`)
2. Security configurations in multiple services were missing tenant validation
3. `CqlSecurityCustomizer.java` allowed ALL requests without authentication
4. Any authenticated user could access any tenant's data

**Business Context**:
- Healthcare SaaS serving multiple healthcare organizations
- Each tenant's PHI must be strictly isolated
- Cross-tenant data access would be a reportable HIPAA breach
- Potential penalties: $100K-$1.5M per violation category

**Technical Context**:
- Multi-tenant architecture with `X-Tenant-ID` header
- JWT authentication with tenant claims
- Spring Security filter chain
- Services: patient-service, care-gap-service, quality-measure-service, fhir-service, cql-engine-service

## Decision

**We will re-enable and enforce tenant isolation through a security filter that validates user access to requested tenants.**

**Specific Implementation**:

1. **TenantAccessFilter**: Re-enabled to validate `X-Tenant-ID` against user's authorized tenants
2. **Security Configs**: Updated all services to include TenantAccessFilter after JWT authentication
3. **CQL Engine**: Fixed security bypass - re-enabled authentication requirement

**Filter Chain Order**:
```
Request → RateLimitFilter → JwtAuthenticationFilter → TenantAccessFilter → Controller
```

## Alternatives Considered

### Alternative 1: Database-Level Row Security (RLS)
**Description**: Implement PostgreSQL Row-Level Security policies
**Pros**:
- Defense in depth at database layer
- Works regardless of application bugs
- Query-level enforcement

**Cons**:
- Complex to implement across all tables
- Performance overhead
- Doesn't prevent API-level exposure

**Why Not Chosen**: Application-level filter provides clearer audit trail; RLS can be added later as defense-in-depth

### Alternative 2: Separate Databases Per Tenant
**Description**: Each tenant gets isolated database
**Pros**:
- Complete data isolation
- Easy to audit and backup per tenant
- No risk of cross-tenant queries

**Cons**:
- Significant infrastructure cost
- Complex connection management
- Difficult to scale beyond ~100 tenants

**Why Not Chosen**: Current architecture uses shared database; would require major refactor

### Alternative 3: API Gateway-Only Enforcement
**Description**: Validate tenant access only at gateway
**Pros**:
- Single point of enforcement
- Simpler service code

**Cons**:
- Services vulnerable if accessed directly
- No defense in depth
- Gateway bypass = complete tenant isolation failure

**Why Not Chosen**: Services should be self-protecting; gateway is additional layer

## Consequences

### Positive Consequences
- **Security**: Users can only access their authorized tenants
- **Compliance**: Meets HIPAA access control requirements
- **Auditability**: Filter logs all access attempts and denials
- **Defense in Depth**: Each service validates independently

### Negative Consequences
- **Performance**: Additional database lookup per request to verify tenant access
- **Complexity**: All services must be updated and tested
- **False Positives**: Misconfigured tenant assignments cause access denials

### Mitigation
- Cache user-tenant mappings in Redis (TTL: 5 minutes)
- Clear error messages for tenant access denials
- Comprehensive integration tests for tenant isolation

## Compliance & Security

- **HIPAA §164.312(a)(1)**: Access Control - Technical safeguards
- **HIPAA §164.308(a)(4)**: Information Access Management
- **Internal CVE**: CVE-INTERNAL-2025-001 (Complete Bypass of Tenant Isolation)

## Implementation Plan

1. **Phase 1 (Completed)**: Re-enable `TenantAccessFilter.java`
2. **Phase 2 (Completed)**: Update security configs in all services
3. **Phase 3 (Completed)**: Fix CQL Engine security bypass
4. **Phase 4**: Integration testing with multi-tenant scenarios

## Files Modified

- `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/security/TenantAccessFilter.java`
- `backend/modules/services/care-gap-service/src/main/java/com/healthdata/caregap/config/CareGapSecurityConfig.java`
- `backend/modules/services/patient-service/src/main/java/com/healthdata/patient/config/PatientSecurityConfig.java`
- `backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/config/QualityMeasureSecurityConfig.java`
- `backend/modules/services/fhir-service/src/main/java/com/healthdata/fhir/config/FhirSecurityConfig.java`
- `backend/modules/services/cql-engine-service/src/main/java/com/healthdata/cql/config/CqlSecurityCustomizer.java`

## Success Metrics

- **Zero cross-tenant access incidents**: Target 0 violations
- **Access denial rate**: <0.1% for legitimate requests
- **Latency impact**: <5ms additional per request

## Related Decisions

- **Related to**: [ADR-0001](0001-externalize-secrets-and-credentials.md) - Credentials management

## References

- [OWASP Multi-Tenant Security](https://cheatsheetseries.owasp.org/cheatsheets/Multi-Tenancy_Security_Cheat_Sheet.html)
- [HIPAA Security Rule](https://www.hhs.gov/hipaa/for-professionals/security/index.html)
