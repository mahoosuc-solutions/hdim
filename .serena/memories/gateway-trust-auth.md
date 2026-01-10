# Gateway Trust Authentication Architecture

## ⚠️ CRITICAL: This is the GOLD STANDARD Authentication Pattern

Backend services use **gateway-trust authentication**, NOT direct JWT validation.

**Full Documentation**: `backend/docs/GATEWAY_TRUST_ARCHITECTURE.md`

---

## Architecture Flow

```
Client
  → Gateway (validates JWT)
    → Injects X-Auth-* headers
      → Backend Service (trusts headers)
        → No JWT validation
        → No database lookup
        → Uses header values directly
```

---

## Key Principle

**The gateway is the ONLY component that validates JWT tokens.**

Backend services:
- ✅ Trust headers injected by gateway
- ❌ Do NOT re-validate JWT
- ❌ Do NOT perform database lookups for user/tenant info

---

## Headers Injected by Gateway

| Header | Description | Example |
|--------|-------------|---------|
| `X-Auth-User-Id` | User's UUID | `550e8400-e29b-41d4-a716-446655440000` |
| `X-Auth-Username` | User's login name | `test_admin` |
| `X-Auth-Tenant-Ids` | Comma-separated authorized tenants | `TENANT001,TENANT002` |
| `X-Auth-Roles` | Comma-separated roles | `ADMIN,EVALUATOR` |
| `X-Auth-Validated` | HMAC signature proving gateway origin | `hmac-sha256-signature` |

---

## Component Reference

### 1. Gateway Filter (JWT Validation)

**Location**: `gateway-service/src/main/java/.../GatewayAuthenticationFilter.java`

```java
@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) {

        // 1. Extract JWT from Authorization header
        String jwt = extractJwt(request);

        // 2. Validate JWT signature & expiration
        Claims claims = jwtService.validateToken(jwt);

        // 3. Extract user info from JWT claims
        String userId = claims.get("sub", String.class);
        String username = claims.get("username", String.class);
        List<String> tenantIds = claims.get("tenant_ids", List.class);
        List<String> roles = claims.get("roles", List.class);

        // 4. Inject trusted headers
        request.setAttribute("X-Auth-User-Id", userId);
        request.setAttribute("X-Auth-Username", username);
        request.setAttribute("X-Auth-Tenant-Ids", String.join(",", tenantIds));
        request.setAttribute("X-Auth-Roles", String.join(",", roles));

        // 5. Generate HMAC signature (production only)
        String signature = hmacService.sign(userId, tenantIds, roles);
        request.setAttribute("X-Auth-Validated", signature);

        filterChain.doFilter(request, response);
    }
}
```

### 2. Backend Service Filter (Header Trust)

**Location**: `shared/infrastructure/authentication/src/main/java/.../TrustedHeaderAuthFilter.java`

```java
@Component
public class TrustedHeaderAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) {

        // 1. Extract headers (NO JWT validation)
        String userId = request.getHeader("X-Auth-User-Id");
        String username = request.getHeader("X-Auth-Username");
        String tenantIds = request.getHeader("X-Auth-Tenant-Ids");
        String roles = request.getHeader("X-Auth-Roles");
        String validated = request.getHeader("X-Auth-Validated");

        // 2. Verify HMAC signature (production only)
        if (!devMode) {
            hmacService.verify(validated, userId, tenantIds, roles);
        }

        // 3. Create Spring Security authentication (NO DB lookup)
        List<GrantedAuthority> authorities = parseRoles(roles);
        Authentication auth = new TrustedHeaderAuthentication(
            userId,
            username,
            parseTenantIds(tenantIds),
            authorities
        );

        // 4. Set SecurityContext
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }
}
```

### 3. Tenant Access Filter

**Location**: `shared/infrastructure/authentication/src/main/java/.../TrustedTenantAccessFilter.java`

```java
@Component
public class TrustedTenantAccessFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) {

        // Extract tenant from request header
        String requestedTenant = request.getHeader("X-Tenant-ID");

        // Get authorized tenants from authentication (already set by TrustedHeaderAuthFilter)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        List<String> authorizedTenants = ((TrustedHeaderAuthentication) auth).getTenantIds();

        // Validate access (NO database lookup)
        if (!authorizedTenants.contains(requestedTenant)) {
            response.sendError(403, "Access denied to tenant: " + requestedTenant);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
```

---

## Security Configuration Pattern

### ✅ CORRECT - Gateway Trust Pattern

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ServiceSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            TrustedHeaderAuthFilter trustedHeaderAuthFilter,
            TrustedTenantAccessFilter trustedTenantAccessFilter) {

        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated())
            // CRITICAL: Use gateway trust filters
            .addFilterBefore(trustedHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(trustedTenantAccessFilter, TrustedHeaderAuthFilter.class);

        return http.build();
    }
}
```

### ❌ WRONG - JWT Re-Validation Pattern

```java
// DO NOT USE THIS PATTERN
@Configuration
public class BadSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthFilter,  // ❌ NO!
            TenantAccessFilter tenantAccessFilter) { // ❌ NO! (DB lookup)

        http
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)  // ❌
            .addFilterAfter(tenantAccessFilter, JwtAuthenticationFilter.class);  // ❌

        return http.build();
    }
}
```

---

## Configuration

### Development Mode

```yaml
# docker-compose.yml or application.yml
environment:
  GATEWAY_AUTH_DEV_MODE: "true"  # Skip HMAC validation
```

### Production Mode

```yaml
environment:
  GATEWAY_AUTH_DEV_MODE: "false"
  GATEWAY_AUTH_SIGNING_SECRET: ${VAULT_SECRET}  # 64-char hex string
```

---

## Testing

### Unit Test Example

```java
@SpringBootTest
@AutoConfigureMockMvc
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAuthenticateWithTrustedHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/patients/123")
                .header("X-Auth-User-Id", "user-123")
                .header("X-Auth-Username", "test_admin")
                .header("X-Auth-Tenant-Ids", "TENANT001")
                .header("X-Auth-Roles", "ADMIN,EVALUATOR")
                .header("X-Auth-Validated", "dev-mode-skip")
                .header("X-Tenant-ID", "TENANT001"))
            .andExpect(status().isOk());
    }

    @Test
    void shouldRejectUnauthorizedTenant() throws Exception {
        mockMvc.perform(get("/api/v1/patients/123")
                .header("X-Auth-User-Id", "user-123")
                .header("X-Auth-Tenant-Ids", "TENANT001")  // Authorized for TENANT001
                .header("X-Tenant-ID", "TENANT002"))        // Requesting TENANT002
            .andExpect(status().isForbidden());
    }
}
```

---

## Common Mistakes to Avoid

### ❌ Mistake 1: JWT Validation in Backend Service

```java
// DO NOT DO THIS in backend services
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(...) {
        String jwt = request.getHeader("Authorization");
        Claims claims = jwtService.validateToken(jwt);  // ❌ Gateway already did this!
        // ...
    }
}
```

### ❌ Mistake 2: Database Lookup for User/Tenant

```java
// DO NOT DO THIS
@Component
public class TenantAccessFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(...) {
        String userId = request.getHeader("X-Auth-User-Id");
        User user = userRepository.findById(userId);  // ❌ Unnecessary DB call!
        List<String> tenants = user.getTenantIds();    // ❌ Already in X-Auth-Tenant-Ids!
        // ...
    }
}
```

### ❌ Mistake 3: Missing X-Tenant-ID Header Validation

```java
// DO NOT SKIP tenant header validation
@GetMapping("/patients/{id}")
public ResponseEntity<Patient> getPatient(@PathVariable String id) {
    // ❌ Missing @RequestHeader("X-Tenant-ID")
    // ❌ No tenant isolation!
    return ResponseEntity.ok(patientService.getPatient(id));
}
```

---

## Performance Benefits

| Approach | JWT Decode | DB Lookup | Latency |
|----------|------------|-----------|---------|
| ❌ JWT Re-Validation | ✅ Yes | ✅ Yes | ~50-100ms |
| ✅ Gateway Trust | ❌ No | ❌ No | ~1-5ms |

**Result**: 10-50x faster authentication in backend services.

---

## Security Considerations

### Production Deployment

1. **HMAC Signature**: MUST be enabled in production
2. **Secret Rotation**: Rotate `GATEWAY_AUTH_SIGNING_SECRET` quarterly
3. **Network Isolation**: Backend services should ONLY accept traffic from gateway
4. **Header Validation**: Validate all required headers are present

### Network Security

```yaml
# docker-compose.production.yml
services:
  patient-service:
    networks:
      - internal  # NOT exposed to external network
    environment:
      GATEWAY_AUTH_DEV_MODE: "false"

  gateway-service:
    networks:
      - external  # Exposed to internet
      - internal  # Communicates with backend
    ports:
      - "8001:8001"
```

---

## Migration Checklist

When updating a service to gateway trust:

- [ ] Remove `JwtAuthenticationFilter`
- [ ] Remove `UserDetailsService` / `UserRepository` lookups
- [ ] Add `TrustedHeaderAuthFilter` dependency
- [ ] Add `TrustedTenantAccessFilter` dependency
- [ ] Update `SecurityFilterChain` configuration
- [ ] Update tests to use `X-Auth-*` headers
- [ ] Verify `X-Tenant-ID` header validation in controllers
- [ ] Remove JWT secret configuration (only in gateway)
- [ ] Test with `GATEWAY_AUTH_DEV_MODE=true`
- [ ] Configure `GATEWAY_AUTH_SIGNING_SECRET` for production

---

## Quick Reference

### Request Headers (Client → Gateway)

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
X-Tenant-ID: TENANT001
```

### Injected Headers (Gateway → Backend)

```http
X-Auth-User-Id: 550e8400-e29b-41d4-a716-446655440000
X-Auth-Username: test_admin
X-Auth-Tenant-Ids: TENANT001,TENANT002
X-Auth-Roles: ADMIN,EVALUATOR
X-Auth-Validated: hmac-sha256-signature
X-Tenant-ID: TENANT001
```

### Service Code

```java
@GetMapping("/patients/{id}")
@PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'VIEWER')")
public ResponseEntity<Patient> getPatient(
        @PathVariable String id,
        @RequestHeader("X-Tenant-ID") String tenantId) {

    // SecurityContext already populated by TrustedHeaderAuthFilter
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String userId = auth.getName();  // From X-Auth-User-Id
    List<String> roles = auth.getAuthorities();  // From X-Auth-Roles

    // Tenant already validated by TrustedTenantAccessFilter
    return ResponseEntity.ok(patientService.getPatient(id, tenantId));
}
```

---

## Troubleshooting

### Problem: 401 Unauthorized

```
Caused by: Missing required header: X-Auth-User-Id
```

**Solution**: Ensure request passes through gateway first. Backend services should NOT be accessed directly.

### Problem: 403 Forbidden

```
Access denied to tenant: TENANT002
```

**Solution**: User's `X-Auth-Tenant-Ids` header doesn't include requested tenant. Check JWT claims.

### Problem: HMAC validation failed

```
Invalid X-Auth-Validated signature
```

**Solution**: Verify `GATEWAY_AUTH_SIGNING_SECRET` matches between gateway and services.

---

## Resources

- **Full Architecture**: `backend/docs/GATEWAY_TRUST_ARCHITECTURE.md`
- **Authentication Guide**: `AUTHENTICATION_GUIDE.md`
- **Security Config Examples**: `backend/modules/shared/infrastructure/authentication/`
