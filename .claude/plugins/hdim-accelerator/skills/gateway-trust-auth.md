---
description: Gateway trust authentication patterns for HDIM microservices
tags:
  - security
  - authentication
  - gateway
  - multi-tenant
---

# Gateway Trust Authentication Skill

Comprehensive guidance for implementing gateway trust authentication in HDIM microservices.

<skill_instructions>

## Overview

HDIM uses **gateway trust authentication** instead of distributed JWT validation. The gateway validates JWT tokens once and injects trusted headers that backend services trust.

**Architecture:**
```
Client → Gateway (validates JWT) → Backend Service (trusts headers)
```

**Benefits:**
- ✅ Single point of JWT validation (gateway only)
- ✅ No JWT libraries in backend services
- ✅ No database lookups for user/tenant data
- ✅ Faster request processing
- ✅ Simpler service implementation

---

## Key Components

### 1. Gateway Authentication Filter

**Location:** `gateway-service/GatewayAuthenticationFilter.java`

**Responsibilities:**
- Validates JWT token
- Extracts user/tenant/role information
- Injects trusted X-Auth-* headers
- Adds HMAC signature (production) or dev-mode marker

### 2. Trusted Header Auth Filter

**Location:** `modules/shared/authentication/TrustedHeaderAuthFilter.java`

**Responsibilities:**
- Validates X-Auth-Validated header (HMAC or dev-mode)
- Extracts authentication from trusted headers
- Sets Spring SecurityContext

### 3. Trusted Tenant Access Filter

**Location:** `modules/shared/authentication/TrustedTenantAccessFilter.java`

**Responsibilities:**
- Validates X-Tenant-ID header
- Ensures user has access to requested tenant
- Returns 403 if tenant access denied

---

## Injected Headers

The gateway injects these headers on every authenticated request:

| Header | Example | Description |
|--------|---------|-------------|
| `X-Auth-User-Id` | `550e8400-e29b-41d4-a716-446655440000` | User's UUID |
| `X-Auth-Username` | `test_admin` | User's login name |
| `X-Auth-Tenant-Ids` | `tenant-001,tenant-002` | Comma-separated authorized tenants |
| `X-Auth-Roles` | `ADMIN,EVALUATOR` | Comma-separated roles |
| `X-Auth-Validated` | `dev-mode` or HMAC signature | Proof of gateway origin |

---

## SecurityConfig Pattern

**Standard SecurityConfig for all services:**

```java
package com.healthdata.SERVICE.config;

import com.healthdata.shared.authentication.filter.TrustedHeaderAuthFilter;
import com.healthdata.shared.authentication.filter.TrustedTenantAccessFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final TrustedHeaderAuthFilter trustedHeaderAuthFilter;
    private final TrustedTenantAccessFilter trustedTenantAccessFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/actuator/prometheus").permitAll()
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            // Gateway trust filter chain (ORDER MATTERS!)
            .addFilterBefore(trustedHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(trustedTenantAccessFilter, TrustedHeaderAuthFilter.class);

        return http.build();
    }
}
```

**Critical:**
- TrustedHeaderAuthFilter BEFORE UsernamePasswordAuthenticationFilter
- TrustedTenantAccessFilter AFTER TrustedHeaderAuthFilter

---

## Controller Pattern

**Extracting authentication from headers:**

```java
@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
@Validated
public class ResourceController {

    private final ResourceService service;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'VIEWER')")
    public ResponseEntity<ResourceResponse> getById(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") String tenantId,  // Required for tenant isolation
            @RequestHeader("X-Auth-User-Id") String userId) {  // Optional: for audit logging

        ResourceResponse response = service.getById(id, tenantId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<ResourceResponse> create(
            @Valid @RequestBody ResourceRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader("X-Auth-User-Id") String userId) {

        ResourceResponse response = service.create(request, tenantId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
```

**Best Practices:**
- Always extract X-Tenant-ID header
- Use @PreAuthorize for role-based access
- Pass userId for audit logging (createdBy, updatedBy)

---

## Role Hierarchy

**HDIM roles (highest to lowest):**

| Role | Access Level | Common Use |
|------|--------------|------------|
| SUPER_ADMIN | Full system access | Platform administrators |
| ADMIN | Tenant-level admin | Tenant administrators |
| EVALUATOR | Run evaluations, create data | Clinical quality teams |
| ANALYST | View reports, analytics | Business analysts |
| VIEWER | Read-only access | Auditors, reviewers |

**@PreAuthorize patterns:**
```java
@PreAuthorize("hasRole('SUPER_ADMIN')")  // SUPER_ADMIN only
@PreAuthorize("hasRole('ADMIN')")  // ADMIN + SUPER_ADMIN
@PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")  // ADMIN, EVALUATOR, or SUPER_ADMIN
@PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'ANALYST', 'VIEWER')")  // Any authenticated user
```

**Note:** Spring Security's role hierarchy automatically grants SUPER_ADMIN all lower roles.

---

## Testing with Gateway Trust Headers

**GatewayTrustTestHeaders builder:**

```java
import com.healthdata.shared.authentication.test.GatewayTrustTestHeaders;

// In test setup
GatewayTrustTestHeaders testHeaders = GatewayTrustTestHeaders.builder()
        .userId(UUID.randomUUID())
        .username("test_admin")
        .tenantIds("tenant-001,tenant-002")
        .roles("ADMIN,EVALUATOR")
        .build();

// Use in MockMvc tests
mockMvc.perform(get("/api/v1/resources/123")
                .headers(testHeaders.toHttpHeaders()))
        .andExpect(status().isOk());
```

**Phase 21 Pattern (from 100% test pass rate achievement):**
```java
@BeforeEach
void setUp() {
    testHeaders = GatewayTrustTestHeaders.builder()
            .userId(UUID.fromString(USER_ID))
            .username("test_admin")
            .tenantIds(TENANT_ID)
            .roles("ADMIN,EVALUATOR")
            .build();
}

@Test
void shouldReturnResourceForAuthorizedTenant() throws Exception {
    mockMvc.perform(get("/api/v1/resources/{id}", resourceId)
                    .headers(testHeaders.toHttpHeaders()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tenantId").value(TENANT_ID));
}

@Test
void shouldReturn404ForUnauthorizedTenant() throws Exception {
    // Create resource for different tenant
    Resource resource = createResourceForTenant("different-tenant");

    // Request with original test headers (tenant-001)
    mockMvc.perform(get("/api/v1/resources/{id}", resource.getId())
                    .headers(testHeaders.toHttpHeaders()))
            .andExpect(status().isNotFound());  // 404, not 403 (prevents info disclosure)
}

@Test
void shouldReturn401WhenAuthHeaderMissing() throws Exception {
    mockMvc.perform(get("/api/v1/resources/123"))
            .andExpect(status().isUnauthorized());
}

@Test
void shouldReturn403WhenUserLacksRole() throws Exception {
    GatewayTrustTestHeaders viewerHeaders = GatewayTrustTestHeaders.builder()
            .userId(UUID.randomUUID())
            .username("test_viewer")
            .tenantIds(TENANT_ID)
            .roles("VIEWER")  // VIEWER cannot DELETE
            .build();

    mockMvc.perform(delete("/api/v1/resources/123")
                    .headers(viewerHeaders.toHttpHeaders()))
            .andExpect(status().isForbidden());
}
```

---

## Environment Configuration

**Development (docker-compose.yml):**
```yaml
environment:
  GATEWAY_AUTH_DEV_MODE: "true"  # Skip HMAC validation
```

**Production:**
```yaml
environment:
  GATEWAY_AUTH_SIGNING_SECRET: ${GATEWAY_AUTH_SECRET}  # 64-char hex string
```

**application.yml:**
```yaml
gateway:
  auth:
    dev-mode: ${GATEWAY_AUTH_DEV_MODE:false}
    signing-secret: ${GATEWAY_AUTH_SIGNING_SECRET:}
```

---

## Multi-Tenant Security

**Tenant Isolation Pattern:**

```java
@Repository
public interface ResourceRepository extends JpaRepository<Resource, UUID> {

    /**
     * Find by ID with tenant isolation.
     * Returns empty Optional if not found OR tenant mismatch (404, not 403).
     */
    @Query("SELECT r FROM Resource r WHERE r.id = :id AND r.tenantId = :tenantId")
    Optional<Resource> findByIdAndTenant(
            @Param("id") UUID id,
            @Param("tenantId") String tenantId
    );

    /**
     * Find all for tenant.
     */
    @Query("SELECT r FROM Resource r WHERE r.tenantId = :tenantId ORDER BY r.createdAt DESC")
    List<Resource> findAllByTenant(@Param("tenantId") String tenantId);

    /**
     * Count for tenant.
     */
    @Query("SELECT COUNT(r) FROM Resource r WHERE r.tenantId = :tenantId")
    long countByTenant(@Param("tenantId") String tenantId);
}
```

**Critical:** EVERY query MUST filter by tenantId.

---

## Common Pitfalls

### ❌ Missing X-Auth-Validated Header

**Error:** 401 Unauthorized

**Cause:** TrustedHeaderAuthFilter requires X-Auth-Validated header.

**Fix (Tests):**
```java
// Use GatewayTrustTestHeaders builder (includes X-Auth-Validated)
mockMvc.perform(get("/api/v1/resources/123")
        .headers(testHeaders.toHttpHeaders()))
```

### ❌ Wrong Filter Order

**Error:** Authentication fails intermittently

**Cause:** TrustedHeaderAuthFilter added after instead of before UsernamePasswordAuthenticationFilter.

**Fix:**
```java
.addFilterBefore(trustedHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class)
// NOT .addFilterAfter(...)
```

### ❌ Missing @PreAuthorize

**Error:** Endpoints accessible without proper roles

**Cause:** Forgot @PreAuthorize annotation on controller method.

**Fix:**
```java
@GetMapping("/{id}")
@PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'VIEWER')")  // Add this
public ResponseEntity<ResourceResponse> getById(...) {
```

### ❌ Returning 403 Instead of 404

**Error:** Information disclosure (reveals resource exists)

**Cause:** Repository returns resource, service checks tenant and throws 403.

**Fix:**
```java
// Use tenant-filtered query that returns Optional.empty() for wrong tenant
return repository.findByIdAndTenant(id, tenantId)
        .map(this::toResponse)
        .orElseThrow(() -> new ResourceNotFoundException("Resource", id));  // 404
```

---

## Best Practices

**DO:**
- ✅ Use TrustedHeaderAuthFilter + TrustedTenantAccessFilter
- ✅ Add filters in correct order
- ✅ Use @PreAuthorize on all endpoints
- ✅ Extract X-Tenant-ID header in controllers
- ✅ Use tenant-filtered queries in repositories
- ✅ Return 404 (not 403) for tenant isolation violations
- ✅ Use GatewayTrustTestHeaders in tests

**DON'T:**
- ❌ Validate JWT in backend services (gateway handles this)
- ❌ Query database for user/tenant data (trust headers)
- ❌ Skip @PreAuthorize (insecure)
- ❌ Return 403 for wrong tenant (information disclosure)
- ❌ Create custom authentication filters (use shared)

---

## Quick Reference

**Filter Chain:**
```
Request → TrustedHeaderAuthFilter → TrustedTenantAccessFilter → Controller
```

**Headers:**
```
X-Auth-User-Id: UUID
X-Auth-Username: string
X-Auth-Tenant-Ids: comma-separated
X-Auth-Roles: comma-separated
X-Auth-Validated: HMAC or "dev-mode"
X-Tenant-ID: single tenant (from X-Auth-Tenant-Ids)
```

**Roles (high to low):**
```
SUPER_ADMIN > ADMIN > EVALUATOR > ANALYST > VIEWER
```

**Test Headers:**
```java
GatewayTrustTestHeaders.builder()
    .userId(UUID)
    .username(String)
    .tenantIds(String)
    .roles(String)
    .build()
    .toHttpHeaders()
```

---

## Documentation

- Complete architecture: `backend/docs/GATEWAY_TRUST_ARCHITECTURE.md` (gold standard)
- Authentication guide: `AUTHENTICATION_GUIDE.md`
- Test users: See AUTHENTICATION_GUIDE.md

</skill_instructions>
