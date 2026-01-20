# ADR-007: Gateway-Trust Authentication Pattern

**Status**: Accepted
**Date**: 2026-01-19 (Decision Made: Phase 1.9, Oct 2025)
**Decision Makers**: Architecture Lead, Security Lead
**Stakeholders**: Gateway Service, All Backend Services

---

## Context

### Problem Statement

Backend services needed authentication without performing repeated JWT validation or database lookups. Validating JWT in every service creates bottleneck; trusting gateway headers is more efficient.

**Pattern**: Client → Gateway (validates JWT) → Service (trusts X-Auth-* headers)

---

## Architecture

### Flow

```
1. Client sends JWT to Gateway
   ↓
2. Gateway validates JWT (once)
   ↓
3. Gateway injects X-Auth-* headers
   ↓
4. Service trusts X-Auth-* headers (no re-validation)
   ↓
5. Response flows back
```

### Headers Injected by Gateway

| Header | Purpose |
|--------|---------|
| X-Auth-User-Id | User's UUID |
| X-Auth-Username | User's login |
| X-Auth-Tenant-Ids | Authorized tenants |
| X-Auth-Roles | User roles |
| X-Auth-Validated | HMAC signature proving gateway origin |

---

## Implementation

### Gateway Configuration

```yaml
environment:
  GATEWAY_AUTH_DEV_MODE: 'true'  # Dev: skip HMAC
  GATEWAY_AUTH_SIGNING_SECRET: ${SECRET}  # Prod: HMAC secret
```

### Service Configuration

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) {
    http.addFilterBefore(trustedHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class);
    http.addFilterAfter(trustedTenantAccessFilter, TrustedHeaderAuthFilter.class);
    return http.build();
}
```

---

## Benefits

- **Efficiency**: JWT validated once (at gateway), not repeated in all services
- **Performance**: No database lookups in services
- **Consistency**: Centralized auth logic
- **Scaling**: Services scale without auth overhead

---

## Success Criteria

- ✅ Gateway validates all JWT tokens
- ✅ Headers injected to all backend requests
- ✅ Services trust headers (no re-validation)
- ✅ Zero duplicate JWT validation
- ✅ HMAC signature prevents header spoofing

---

## References

- **[Gateway Trust Architecture Guide](../../backend/docs/GATEWAY_TRUST_ARCHITECTURE.md)**
- **[ADR-002: Gateway Modularization](ADR-002-gateway-modularization.md)**

---

## Footer

**ADR #**: 007
**Version**: 1.0
**Status**: Active and Deployed
**Last Updated**: 2026-01-19

_Decision Date: Phase 1.9 (October 2025)_
_Type: Security Pattern_
