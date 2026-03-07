# Security Auth/Tenant/RBAC Rerun Report

**Date:** 2026-03-06  
**Scope:** Authentication provider wiring, tenant isolation, RBAC coverage, and regression tests for privilege escalation / tenant tampering.  
**Related Todo:** `todos/004-pending-p1-remediate-critical-auth-and-tenant-controls.md`

## Rerun Command Set

```bash
cd backend
./gradlew \
  :modules:shared:infrastructure:gateway-core:test --tests "*CustomUserDetailsServiceTest" \
  :modules:shared:infrastructure:authentication:test --tests "*TrustedHeaderAuthFilterTest" \
  :modules:services:event-processing-service:test --tests "*DeadLetterQueueControllerSecurityTest" --tests "*DeadLetterQueueServiceTest" \
  --no-daemon
```

## Result Summary

- `CustomUserDetailsServiceTest`: **3 passed, 0 failed**
- `TrustedHeaderAuthFilterTest`: **19 passed, 0 failed**
- `DeadLetterQueueControllerSecurityTest`: **3 passed, 0 failed**
- `DeadLetterQueueServiceTest` (tenant-scoped + mutation regression): **44 passed, 0 failed**
- Overall targeted rerun: **SUCCESS**

## Control Closure Mapping

1. **UserDetailsService-backed authentication implemented and tested**
- Implementation present in `gateway-core` (`CustomUserDetailsService` + `GatewaySecurityConfig` DAO provider wiring).
- Direct regression coverage added and passing (`CustomUserDetailsServiceTest`).

2. **Tenant access checks enforce user-tenant membership on protected APIs**
- Runtime enforcement provided by `TrustedTenantAccessFilter` (gateway-trust tenant validation).
- Event-processing DLQ endpoints now enforce tenant scoping end-to-end:
  - controller requires `X-Tenant-ID`
  - service methods use tenant-scoped repository lookups for object mutations and reads
  - cross-tenant mutation attempts return not-found semantics via tenant-scoped lookup failure.

3. **RBAC on privileged endpoints**
- Event-processing DLQ endpoints now use explicit `@PreAuthorize`.
- Mutation endpoints (`retry`, `resolve`, `exhaust`) require elevated roles and exclude read-only/audit-only role usage.
- Contract-level regression test validates annotation presence and constraints.

4. **Security regressions: auth bypass, privilege escalation, tenant tampering**
- Auth bypass regression: `TrustedHeaderAuthFilterTest` (missing headers, invalid signature, error handling, path filtering).
- Privilege escalation regression: `DeadLetterQueueControllerSecurityTest` (mutation role constraints).
- Tenant tampering regression: `DeadLetterQueueServiceTest` tenant-scoped mutation rejection tests.

## Audit Disposition

- For the scoped controls above, findings are now **closed with evidence**.
- Residual findings outside this scope should remain tracked in the master security backlog until independently remediated or formally accepted with compensating controls.
