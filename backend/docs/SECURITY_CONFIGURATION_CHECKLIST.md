# Security Configuration Checklist

**GitHub Issue:** https://github.com/webemo-aaron/hdim/issues/132

Use this checklist when creating new services or reviewing security configurations for existing services.

## Pre-Deployment Security Checklist

### Required Annotations

- [ ] `@Configuration` on security config class
- [ ] `@EnableMethodSecurity(prePostEnabled = true)` for `@PreAuthorize` support
- [ ] `@Profile("!test")` on production security beans
- [ ] `@Order` on security filter chains (test = 1, production = 2)

### Security Filter Configuration

- [ ] `TrustedHeaderAuthFilter` bean defined (with `@Profile("!test")`)
- [ ] `TrustedTenantAccessFilter` bean defined (with `@Profile("!test")`)
- [ ] Filters added in correct order:
  1. `TrustedHeaderAuthFilter` before `UsernamePasswordAuthenticationFilter`
  2. `TrustedTenantAccessFilter` after `TrustedHeaderAuthFilter`

### Configuration Properties

- [ ] `gateway.auth.signing-secret` configured (production)
- [ ] `gateway.auth.dev-mode` set to `false` in production
- [ ] Service URLs use correct ports (see Port Reference below)

### Feign Client Configuration

- [ ] Authentication module dependency included
- [ ] `@EnableFeignClients` on application class
- [ ] Feign client URLs configured in `application-docker.yml`

---

## Security Config Template

```java
package com.healthdata.{service}.config;

import com.healthdata.authentication.filter.TrustedHeaderAuthFilter;
import com.healthdata.authentication.security.TrustedTenantAccessFilter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)  // REQUIRED for @PreAuthorize
public class {Service}SecurityConfig {

    @Value("${gateway.auth.signing-secret:}")
    private String signingSecret;

    @Value("${gateway.auth.dev-mode:true}")
    private boolean devMode;

    @Bean
    @Profile("!test")
    public TrustedHeaderAuthFilter trustedHeaderAuthFilter(MeterRegistry meterRegistry) {
        TrustedHeaderAuthFilter.TrustedHeaderAuthConfig config;
        if (devMode) {
            config = TrustedHeaderAuthFilter.TrustedHeaderAuthConfig.development();
        } else {
            config = TrustedHeaderAuthFilter.TrustedHeaderAuthConfig.production(signingSecret);
        }
        return new TrustedHeaderAuthFilter(config, meterRegistry);
    }

    @Bean
    @Profile("!test")
    public TrustedTenantAccessFilter trustedTenantAccessFilter(MeterRegistry meterRegistry) {
        return new TrustedTenantAccessFilter(meterRegistry);
    }

    @Bean
    @Profile("test")
    @Order(1)
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Profile("!test")
    @Order(2)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            TrustedHeaderAuthFilter trustedHeaderAuthFilter,
            TrustedTenantAccessFilter trustedTenantAccessFilter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/health/**",
                    "/actuator/info",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(trustedHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // CRITICAL: Add tenant filter AFTER auth filter
        http.addFilterAfter(trustedTenantAccessFilter, TrustedHeaderAuthFilter.class);

        return http.build();
    }
}
```

---

## Port Reference

| Service | Port | Context Path |
|---------|------|--------------|
| Gateway | 8080 | / |
| CQL Engine | 8081 | /cql-engine |
| Consent | 8082 | /consent |
| Event Processing | 8083 | /events |
| Patient | 8084 | /patient |
| FHIR | 8085 | /fhir |
| Care Gap | 8086 | /care-gap |
| Quality Measure | 8087 | /quality-measure |

---

## Verification Steps

### 1. Verify @EnableMethodSecurity

```bash
# Check if annotation is present
grep -r "@EnableMethodSecurity" backend/modules/services/*/src/main/java/
```

Expected: All services with `@PreAuthorize` annotations should have `@EnableMethodSecurity`

### 2. Verify Filter Chain Order

```bash
# Check filter configuration
grep -r "addFilterBefore\|addFilterAfter" backend/modules/services/*/src/main/java/
```

Expected:
- `TrustedHeaderAuthFilter` added before `UsernamePasswordAuthenticationFilter`
- `TrustedTenantAccessFilter` added after `TrustedHeaderAuthFilter`

### 3. Verify Port Configuration

```bash
# Check for wrong FHIR port (should be 8085, not 8083)
grep -r "fhir-service:8083" backend/
```

Expected: No results (FHIR should use port 8085)

### 4. Test Authentication Flow

```bash
# Test with demo headers
curl -X GET http://localhost:8080/api/patients \
  -H "X-Auth-User-Id: demo-user-id" \
  -H "X-Auth-Username: demo_user" \
  -H "X-Auth-Tenant-Ids: acme-health" \
  -H "X-Auth-Roles: ADMIN" \
  -H "X-Auth-Validated: gateway-test" \
  -H "X-Tenant-ID: acme-health"
```

Expected: 200 OK (not 403)

### 5. Verify Feign Header Forwarding

Enable debug logging and check for:
```
Auth headers forwarded to downstream service: patient-service
```

---

## Common Issues

### Issue: 403 Forbidden on Protected Endpoints

**Symptoms:**
- API returns 403 even with valid auth headers
- `@PreAuthorize` annotations not working

**Solution:**
1. Add `@EnableMethodSecurity(prePostEnabled = true)` to security config
2. Rebuild and redeploy the service

### Issue: Service-to-Service Calls Fail with 403

**Symptoms:**
- Service A can call Service B directly
- When called through gateway, Service B returns 403

**Solution:**
1. Ensure `AuthHeaderForwardingInterceptor` is on classpath
2. Add authentication module dependency
3. Verify Feign client configuration

### Issue: Connection Refused to FHIR Service

**Symptoms:**
- Feign client throws `Connection refused`
- Other services can reach FHIR

**Solution:**
1. Check FHIR URL in application-docker.yml
2. Update port from 8083 to 8085
3. Rebuild and redeploy the calling service

### Issue: Tenant Mismatch (403 "Access denied to tenant")

**Symptoms:**
- Auth passes but tenant validation fails
- X-Tenant-ID doesn't match X-Auth-Tenant-Ids

**Solution:**
1. Ensure gateway sets X-Tenant-ID from X-Auth-Tenant-Ids
2. Frontend should not send X-Tenant-ID (let gateway set it)

---

## Security Config Status by Service

| Service | @EnableMethodSecurity | TrustedHeaderAuthFilter | TrustedTenantAccessFilter | Status |
|---------|----------------------|-------------------------|---------------------------|--------|
| patient-service | ✅ | ✅ | ✅ | Complete |
| fhir-service | ✅ | ✅ | ✅ | Complete |
| quality-measure-service | ✅ | ✅ | ✅ | Complete |
| consent-service | ✅ | ✅ | ✅ | Complete |
| care-gap-service | ✅ | ✅ | ✅ | Complete |
| agent-builder-service | ✅ | Pattern 3 (No Auth) | Pattern 3 | Complete |
| event-processing-service | ✅ | ✅ | ✅ | Complete |

---

## Related Documentation

- [Service-to-Service Authentication](./SERVICE_TO_SERVICE_AUTHENTICATION.md)
- [Gateway Trust Architecture](./GATEWAY_TRUST_ARCHITECTURE.md)
- [HIPAA Cache Compliance](../HIPAA-CACHE-COMPLIANCE.md)

---

*Last Updated: January 2026*
*GitHub Issue: https://github.com/webemo-aaron/hdim/issues/132*
