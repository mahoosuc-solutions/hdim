# Service-to-Service Authentication Guide

**GitHub Issue:** https://github.com/webemo-aaron/hdim/issues/132

This document describes how authentication context is propagated when services communicate with each other via Feign clients.

## Architecture Overview

```
┌─────────────┐     JWT      ┌─────────────┐   X-Auth-*    ┌─────────────┐   X-Auth-*    ┌─────────────┐
│   Client    │─────────────▶│   Gateway   │──────────────▶│  Service A  │──────────────▶│  Service B  │
└─────────────┘              └─────────────┘               └─────────────┘               └─────────────┘
                                   │                              │                             │
                                   │ Validates JWT                │ TrustedHeaderAuthFilter     │ TrustedHeaderAuthFilter
                                   │ Injects X-Auth-* headers     │ Extracts auth context       │ Validates headers
                                   │ Signs with HMAC              │ Feign forwards headers      │ Sets SecurityContext
                                   ▼                              ▼                             ▼
```

## Key Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `GatewayAuthenticationFilter` | gateway-service | Validates JWT, injects X-Auth-* headers |
| `TrustedHeaderAuthFilter` | shared/authentication | Validates gateway signature, sets SecurityContext |
| `TrustedTenantAccessFilter` | shared/authentication | Enforces tenant isolation |
| `AuthHeaderForwardingInterceptor` | shared/authentication | Forwards auth headers in Feign calls |

## Headers Propagated

| Header | Description | Example |
|--------|-------------|---------|
| `X-Auth-User-Id` | User's unique identifier (UUID) | `550e8400-e29b-41d4-a716-446655440000` |
| `X-Auth-Username` | User's login name | `john.doe` |
| `X-Auth-Tenant-Ids` | Comma-separated authorized tenant IDs | `acme-health,beta-clinic` |
| `X-Auth-Roles` | Comma-separated user roles | `ADMIN,EVALUATOR` |
| `X-Auth-Validated` | Gateway validation signature | `gateway-1703000000-BASE64HMAC` |
| `X-Auth-Token-Id` | Original JWT token ID (jti) | `a1b2c3d4-e5f6-7890` |
| `X-Auth-Token-Expires` | Token expiration timestamp | `1703260800` |
| `X-Tenant-ID` | Current tenant context | `acme-health` |

## AuthHeaderForwardingInterceptor

The `AuthHeaderForwardingInterceptor` is automatically registered when the authentication module is included and Feign is on the classpath.

### How It Works

1. **Request Context**: Uses `RequestContextHolder` to access the current HTTP request
2. **Header Extraction**: Reads all `X-Auth-*` headers from the incoming request
3. **Header Forwarding**: Adds headers to the outgoing Feign request template
4. **Logging**: Logs header forwarding (with sensitive values masked)

### Location

```
backend/modules/shared/infrastructure/authentication/
└── src/main/java/com/healthdata/authentication/feign/
    └── AuthHeaderForwardingInterceptor.java
```

### Auto-Registration

The interceptor uses `@ConditionalOnClass(name = "feign.RequestInterceptor")` to auto-register when Feign is available:

```java
@Slf4j
@Component
@ConditionalOnClass(name = "feign.RequestInterceptor")
public class AuthHeaderForwardingInterceptor implements RequestInterceptor {
    // ...
}
```

## Using Feign Clients

### Basic Feign Client

No additional configuration needed - the interceptor is auto-registered:

```java
@FeignClient(name = "patient-service", url = "${patient.service.url}")
public interface PatientServiceClient {

    @GetMapping("/api/v1/patients/{id}")
    PatientResponse getPatient(@PathVariable String id);

    @PostMapping("/api/v1/patients")
    PatientResponse createPatient(@RequestBody CreatePatientRequest request);
}
```

### With Custom Configuration

If you need additional interceptors (e.g., FHIR content types):

```java
@FeignClient(
    name = "fhir-service",
    url = "${fhir.server.url}",
    configuration = FhirServiceClientConfiguration.class
)
public interface FhirServiceClient {
    @GetMapping("/Patient/{id}")
    String getPatient(@PathVariable String id);
}
```

```java
@Configuration
public class FhirServiceClientConfiguration {

    @Bean
    public RequestInterceptor fhirAcceptHeaderInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("Accept", "application/fhir+json");
            requestTemplate.header("Content-Type", "application/fhir+json");
        };
    }

    // AuthHeaderForwardingInterceptor is auto-added globally
}
```

## Testing Service-to-Service Calls

### Demo Mode

In demo mode (`GATEWAY_AUTH_ENFORCED=false`), the gateway injects demo user headers:

```yaml
# Gateway injects these headers when auth is not enforced
X-Auth-User-Id: demo-user-id
X-Auth-Username: demo_user
X-Auth-Tenant-Ids: acme-health
X-Auth-Roles: ADMIN,EVALUATOR,ANALYST
X-Auth-Validated: gateway-{timestamp}-DEMO
```

### Integration Tests

For testing Feign client behavior with auth headers:

```java
@SpringBootTest
@AutoConfigureMockMvc
class PatientServiceClientIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldForwardAuthHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/patients/123")
                .header("X-Auth-User-Id", "test-user-id")
                .header("X-Auth-Username", "test_user")
                .header("X-Auth-Tenant-Ids", "test-tenant")
                .header("X-Auth-Roles", "ADMIN")
                .header("X-Auth-Validated", "gateway-test"))
            .andExpect(status().isOk());

        // Verify Feign client received the headers
        // (use WireMock or similar for downstream service mocking)
    }
}
```

## Troubleshooting

### 403 Forbidden Errors

1. **Check if headers are present**: Enable debug logging
   ```yaml
   logging:
     level:
       com.healthdata.authentication.feign: DEBUG
   ```

2. **Verify tenant access**: Ensure `X-Auth-Tenant-Ids` includes the tenant in `X-Tenant-ID`

3. **Check @EnableMethodSecurity**: Verify the service has `@EnableMethodSecurity(prePostEnabled = true)`

### Headers Not Being Forwarded

1. **Verify interceptor registration**: Check logs for `Auth headers forwarded to downstream service`

2. **Check request context**: Ensure the call is within an HTTP request context

3. **Verify Feign is on classpath**: Check that `spring-cloud-starter-openfeign` is a dependency

### Connection Refused

1. **Check service URLs in configuration**:
   ```yaml
   fhir:
     server:
       url: http://fhir-service:8085/fhir  # Correct port!
   ```

2. **Verify Docker network**: Ensure services are on the same network

## Configuration Reference

### Service Configuration

```yaml
# application-docker.yml
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 10000
        loggerLevel: basic
```

### Authentication Module Dependency

Add to your service's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":modules:shared:infrastructure:authentication"))
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
}
```

## Security Considerations

1. **Trust Chain**: Backend services MUST validate `X-Auth-Validated` signature
2. **Tenant Isolation**: Always include `TrustedTenantAccessFilter` after `TrustedHeaderAuthFilter`
3. **No Direct Access**: Backend services should not be directly accessible from outside the cluster
4. **HMAC Validation**: In production, enable strict HMAC validation with a shared secret

## Related Documentation

- [Gateway Trust Architecture](./GATEWAY_TRUST_ARCHITECTURE.md)
- [Security Configuration Checklist](./SECURITY_CONFIGURATION_CHECKLIST.md)
- [HIPAA Cache Compliance](../HIPAA-CACHE-COMPLIANCE.md)

---

*Last Updated: January 2026*
*GitHub Issue: https://github.com/webemo-aaron/hdim/issues/132*
