# Gateway Trust Authentication Architecture

## Overview

HDIM uses a **gateway-trust authentication pattern** where the API Gateway is the single point of JWT validation. Backend services trust gateway-injected headers rather than re-validating JWT tokens or performing database lookups.

This architecture provides:
- **Performance**: No duplicate JWT validation or database lookups per request
- **Scalability**: Backend services are stateless and horizontally scalable
- **Security**: Single point of authentication with HMAC-signed headers
- **Simplicity**: Backend services don't need access to user databases

---

## Architecture Diagram

```
                              EXTERNAL NETWORK
    ┌──────────────────────────────────────────────────────────────┐
    │                                                              │
    │   ┌────────┐                      ┌─────────────────────┐   │
    │   │ Client │ ─────────────────────▶│   API Gateway      │   │
    │   └────────┘   Authorization:      │   (Port 8080)      │   │
    │                Bearer <JWT>        │                     │   │
    │                                    │  1. Validates JWT   │   │
    │                                    │  2. Strips X-Auth-* │   │
    │                                    │  3. Injects headers │   │
    │                                    │  4. Signs with HMAC │   │
    │                                    └─────────┬───────────┘   │
    │                                              │               │
    └──────────────────────────────────────────────┼───────────────┘
                                                   │
                              INTERNAL NETWORK     │ X-Auth-* Headers
    ┌──────────────────────────────────────────────┼───────────────┐
    │                                              ▼               │
    │   ┌──────────────────────┐    ┌──────────────────────┐      │
    │   │ Quality Measure Svc  │    │   Care Gap Service   │      │
    │   │    (Port 8087)       │    │    (Port 8086)       │      │
    │   │                      │    │                      │      │
    │   │ TrustedHeaderAuth    │    │ TrustedHeaderAuth    │      │
    │   │ TrustedTenantAccess  │    │ TrustedTenantAccess  │      │
    │   └──────────────────────┘    └──────────────────────┘      │
    │                                                              │
    │   ┌──────────────────────┐    ┌──────────────────────┐      │
    │   │   FHIR Service       │    │   Patient Service    │      │
    │   │    (Port 8085)       │    │    (Port 8084)       │      │
    │   └──────────────────────┘    └──────────────────────┘      │
    │                                                              │
    └──────────────────────────────────────────────────────────────┘
```

---

## Header Flow

### 1. Client Request (External)
```http
GET /api/v1/quality-measure/custom-measures HTTP/1.1
Host: localhost:8080
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
X-Tenant-ID: tenant-001
```

### 2. Gateway Processing
The gateway:
1. **Strips external X-Auth-* headers** (prevents spoofing)
2. **Validates JWT token** (signature, expiry, issuer, audience)
3. **Extracts claims** from JWT
4. **Injects trusted headers** with HMAC signature

### 3. Backend Request (Internal)
```http
GET /quality-measure/custom-measures HTTP/1.1
Host: quality-measure-service:8087
X-Tenant-ID: tenant-001
X-Auth-User-Id: 550e8400-e29b-41d4-a716-446655440000
X-Auth-Username: test_admin
X-Auth-Tenant-Ids: tenant-001,tenant-002
X-Auth-Roles: ADMIN,EVALUATOR
X-Auth-Validated: gateway-1703952000-k8dF2xYz...
X-Auth-Token-Id: abc123-def456
X-Auth-Token-Expires: 1703952900
```

---

## Header Specifications

| Header | Description | Format | Example |
|--------|-------------|--------|---------|
| `X-Auth-User-Id` | User's UUID | UUID string | `550e8400-e29b-41d4-a716-446655440000` |
| `X-Auth-Username` | User's login name | String | `test_admin` |
| `X-Auth-Tenant-Ids` | Authorized tenants | Comma-separated | `tenant-001,tenant-002` |
| `X-Auth-Roles` | User's roles | Comma-separated | `ADMIN,EVALUATOR` |
| `X-Auth-Validated` | Gateway signature | `gateway-{ts}-{hmac}` | `gateway-1703952000-k8dF2x...` |
| `X-Auth-Token-Id` | Original JWT ID | String | `abc123-def456` |
| `X-Auth-Token-Expires` | Token expiry | Unix timestamp | `1703952900` |

---

## Security Model

### Trust Boundary
```
┌─────────────────────────────────────────────────────────────┐
│                    UNTRUSTED (Internet)                     │
│                                                             │
│    Clients can send ANY headers, including fake X-Auth-*    │
│                                                             │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    GATEWAY (Trust Boundary)                 │
│                                                             │
│    • Validates JWT tokens                                   │
│    • STRIPS all incoming X-Auth-* headers                   │
│    • Injects trusted headers with HMAC signature            │
│    • Only authenticated requests pass through               │
│                                                             │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    TRUSTED (Internal Network)               │
│                                                             │
│    Backend services trust X-Auth-* headers because:         │
│    • They are NOT directly accessible from internet         │
│    • Headers are signed with HMAC (production mode)         │
│    • Gateway strips external X-Auth-* headers               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### HMAC Signature Validation

**Format**: `gateway-{timestamp}-{base64-hmac}`

**Algorithm**:
```
HMAC-SHA256(userId + ":" + timestamp, sharedSecret)
```

**Validation**:
1. Check signature starts with `gateway-`
2. Parse timestamp from signature
3. Verify timestamp is within validity window (default: 5 minutes)
4. Compute expected HMAC using shared secret
5. Compare signatures using constant-time comparison

---

## Configuration

### Development Mode (Demo)

In development mode, HMAC validation is bypassed. Any signature starting with `gateway-` is accepted.

```yaml
# docker-compose.yml
environment:
  GATEWAY_AUTH_DEV_MODE: "true"
```

**WARNING**: Never use development mode in production!

### Production Mode

In production, full HMAC validation is enforced.

```yaml
# docker-compose.yml
environment:
  GATEWAY_AUTH_DEV_MODE: "false"
  GATEWAY_AUTH_SIGNING_SECRET: ${GATEWAY_AUTH_SIGNING_SECRET}
```

Generate a secure secret:
```bash
openssl rand -base64 64
```

The same secret must be configured on:
- Gateway service (for signing)
- All backend services (for validation)

---

## Filter Chain

### Backend Service Filter Order

```
Request
    │
    ▼
┌─────────────────────────────────┐
│ TrustedHeaderAuthFilter         │  Order: Before UsernamePasswordAuthFilter
│                                 │
│ • Validates X-Auth-Validated    │
│ • Extracts user context         │
│ • Sets SecurityContext          │
│ • Stores tenant IDs in attrs    │
└─────────────────┬───────────────┘
                  │
                  ▼
┌─────────────────────────────────┐
│ TrustedTenantAccessFilter       │  Order: After TrustedHeaderAuthFilter
│                                 │
│ • Reads tenant IDs from attrs   │
│ • Validates X-Tenant-ID header  │
│ • Blocks unauthorized access    │
│ • NO DATABASE LOOKUP            │
└─────────────────┬───────────────┘
                  │
                  ▼
┌─────────────────────────────────┐
│ Controller                      │
│                                 │
│ • Receives authenticated request│
│ • X-Tenant-ID validated         │
│ • User context in SecurityCtx   │
└─────────────────────────────────┘
```

---

## Implementation Files

| Component | File | Purpose |
|-----------|------|---------|
| Gateway Auth Filter | `gateway-service/.../GatewayAuthenticationFilter.java` | Validates JWT, injects headers |
| Trusted Header Filter | `shared/.../TrustedHeaderAuthFilter.java` | Validates HMAC, sets context |
| Trusted Tenant Filter | `shared/.../TrustedTenantAccessFilter.java` | Validates tenant access |
| Header Constants | `shared/.../AuthHeaderConstants.java` | Header name definitions |
| Quality Measure Config | `quality-measure-service/.../QualityMeasureSecurityConfig.java` | Service security config |
| Care Gap Config | `care-gap-service/.../CareGapSecurityConfig.java` | Service security config |

---

## Migrating Services to Gateway Trust

To migrate a backend service from JWT validation to gateway trust:

### 1. Update Security Config

Replace:
```java
import com.healthdata.authentication.filter.JwtAuthenticationFilter;
import com.healthdata.authentication.security.TenantAccessFilter;
```

With:
```java
import com.healthdata.authentication.filter.TrustedHeaderAuthFilter;
import com.healthdata.authentication.security.TrustedTenantAccessFilter;
```

### 2. Add Configuration Properties

```java
@Value("${gateway.auth.signing-secret:}")
private String signingSecret;

@Value("${gateway.auth.dev-mode:true}")
private boolean devMode;
```

### 3. Create Filter Beans

```java
@Bean
@Profile("!test")
public TrustedHeaderAuthFilter trustedHeaderAuthFilter() {
    TrustedHeaderAuthFilter.TrustedHeaderAuthConfig config;
    if (devMode) {
        config = TrustedHeaderAuthFilter.TrustedHeaderAuthConfig.development();
    } else {
        config = TrustedHeaderAuthFilter.TrustedHeaderAuthConfig.production(signingSecret);
    }
    return new TrustedHeaderAuthFilter(config);
}

@Bean
@Profile("!test")
public TrustedTenantAccessFilter trustedTenantAccessFilter() {
    return new TrustedTenantAccessFilter();
}
```

### 4. Update Filter Chain

```java
@Bean
@Profile("!test")
public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        TrustedHeaderAuthFilter trustedHeaderAuthFilter,
        TrustedTenantAccessFilter trustedTenantAccessFilter) throws Exception {

    http.addFilterBefore(trustedHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class);
    http.addFilterAfter(trustedTenantAccessFilter, TrustedHeaderAuthFilter.class);

    return http.build();
}
```

### 5. Update docker-compose.yml

```yaml
environment:
  GATEWAY_AUTH_DEV_MODE: "true"
  GATEWAY_AUTH_SIGNING_SECRET: ${GATEWAY_AUTH_SIGNING_SECRET:-}
```

---

## Troubleshooting

### 403 Forbidden - "No tenant access configured"

**Cause**: TrustedTenantAccessFilter couldn't find tenant IDs in request attributes.

**Solution**: Ensure TrustedHeaderAuthFilter runs before TrustedTenantAccessFilter and that the gateway is injecting X-Auth-Tenant-Ids header.

### 401 Unauthorized - "Invalid gateway signature"

**Cause**: HMAC signature validation failed.

**Solutions**:
1. Check that `GATEWAY_AUTH_SIGNING_SECRET` matches between gateway and service
2. Check that system clocks are synchronized (signature has 5-minute validity)
3. In development, ensure `GATEWAY_AUTH_DEV_MODE: "true"`

### No Authentication Set

**Cause**: TrustedHeaderAuthFilter didn't find X-Auth-Validated header.

**Solutions**:
1. Verify request is going through gateway (not direct to service)
2. Check gateway logs for JWT validation errors
3. Ensure gateway is running and healthy

---

## Service-to-Service Communication

When backend services call each other via Feign clients, authentication headers must be forwarded to maintain the trust chain.

### AuthHeaderForwardingInterceptor

The `AuthHeaderForwardingInterceptor` automatically forwards X-Auth-* headers from the incoming request to outgoing Feign calls.

**Location**: `shared/infrastructure/authentication/.../feign/AuthHeaderForwardingInterceptor.java`

**Auto-Registration**: The interceptor is auto-registered when:
- The authentication module is on the classpath
- Feign is on the classpath (`@ConditionalOnClass(name = "feign.RequestInterceptor")`)

### Service-to-Service Flow

```
┌─────────────┐   X-Auth-*    ┌─────────────┐   X-Auth-*    ┌─────────────┐
│  Gateway    │──────────────▶│  Service A  │──────────────▶│  Service B  │
└─────────────┘               └─────────────┘               └─────────────┘
                                     │                             │
                                     │ AuthHeaderForwarding        │ TrustedHeaderAuthFilter
                                     │ Interceptor                 │ Validates headers
                                     ▼                             ▼
```

### Feign Client Example

```java
@FeignClient(name = "patient-service", url = "${patient.service.url}")
public interface PatientServiceClient {

    @GetMapping("/api/v1/patients/{id}")
    PatientResponse getPatient(@PathVariable String id);
}
```

No additional configuration needed - headers are forwarded automatically.

For more details, see [Service-to-Service Authentication](./SERVICE_TO_SERVICE_AUTHENTICATION.md).

---

## @EnableMethodSecurity Requirement

**CRITICAL**: All services using `@PreAuthorize` annotations MUST have `@EnableMethodSecurity(prePostEnabled = true)` on their security configuration class.

### Why This Is Required

In Spring Security 6.x, method security is NOT enabled by default. Without this annotation:
- `@PreAuthorize` annotations are ignored
- All requests pass authorization checks
- Security is effectively bypassed

### Correct Configuration

```java
@Configuration
@EnableMethodSecurity(prePostEnabled = true)  // REQUIRED!
public class ServiceSecurityConfig {
    // ...
}
```

### Verification

```bash
# Check if all services have @EnableMethodSecurity
grep -r "@EnableMethodSecurity" backend/modules/services/*/src/main/java/
```

For a complete checklist, see [Security Configuration Checklist](./SECURITY_CONFIGURATION_CHECKLIST.md).

---

## Security Checklist

- [ ] Backend services are NOT publicly accessible (only via gateway)
- [ ] Gateway strips external X-Auth-* headers
- [ ] Production uses HMAC signature validation (dev mode disabled)
- [ ] Signing secret is at least 32 characters
- [ ] Signing secret is stored securely (not in git)
- [ ] System clocks are synchronized (NTP)
- [ ] Rate limiting is enabled on gateway

---

## References

- `AuthHeaderConstants.java` - Header name constants
- `GatewayAuthenticationFilter.java` - Gateway header injection
- `TrustedHeaderAuthFilter.java` - Service header validation
- `TrustedTenantAccessFilter.java` - Tenant isolation
- `AuthHeaderForwardingInterceptor.java` - Feign header forwarding
- [Service-to-Service Authentication](./SERVICE_TO_SERVICE_AUTHENTICATION.md)
- [Security Configuration Checklist](./SECURITY_CONFIGURATION_CHECKLIST.md)
- `AUTHENTICATION_GUIDE.md` - General authentication guide
- `HIPAA-CACHE-COMPLIANCE.md` - PHI handling requirements

---

*Last Updated: January 2026*
*GitHub Issue: https://github.com/webemo-aaron/hdim/issues/132*
