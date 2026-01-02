# Authentication Architecture

## Overview

The HDIM platform uses a **Two-Tier Authentication Architecture** that separates JWT validation (available to all microservices) from full authentication operations (available only to the Gateway service).

This design ensures:
- Microservices remain lightweight without database dependencies for auth
- Authentication operations are centralized in the Gateway
- HIPAA-compliant session management and cache eviction
- Clean separation of concerns

## Two-Tier Model

### Tier 1: JWT Validation (All Microservices)

Microservices only need to **validate** incoming JWTs. They do not perform:
- User login/logout
- Token generation
- MFA verification
- API key management

**Components available to all microservices:**

| Component | Purpose | Database Required |
|-----------|---------|-------------------|
| `JwtTokenService` | Parse and validate JWT tokens | No |
| `JwtAuthenticationFilter` | HTTP filter for auth header extraction | No |
| `JwtConfig` | JWT configuration (secret, expiration) | No |

These components are auto-configured via `AuthenticationAutoConfiguration`.

### Tier 2: Full Auth Stack (Gateway Service Only)

The Gateway service handles all authentication operations that require database access:

| Component | Purpose | Database Required |
|-----------|---------|-------------------|
| `AuthController` | Login/logout REST endpoints | Yes (via services) |
| `LogoutService` | Session termination + PHI cache eviction | Yes (UserRepository) |
| `MfaService` | TOTP setup, verification, recovery codes | Yes (UserRepository) |
| `RefreshTokenService` | Token refresh with revocation support | Yes (RefreshTokenRepository) |
| `ApiKeyService` | API key creation, validation, rotation | Yes (ApiKeyRepository) |

These components are **NOT** auto-scanned. They must be explicitly configured as beans in the Gateway service.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Client Request                               │
└─────────────────────────────────┬───────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      GATEWAY SERVICE (Tier 2)                        │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │                    Full Authentication Stack                   │  │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────────────┐  │  │
│  │  │AuthController│ │ LogoutService│ │ RefreshTokenService  │  │  │
│  │  └──────────────┘ └──────────────┘ └──────────────────────┘  │  │
│  │  ┌──────────────┐ ┌──────────────┐                           │  │
│  │  │  MfaService  │ │ ApiKeyService│                           │  │
│  │  └──────────────┘ └──────────────┘                           │  │
│  └───────────────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │              JWT Validation (Tier 1 components)                │  │
│  │  ┌──────────────────┐ ┌────────────────────────────────────┐  │  │
│  │  │  JwtTokenService │ │    JwtAuthenticationFilter         │  │  │
│  │  └──────────────────┘ └────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │                    Database Repositories                     │    │
│  │  UserRepository │ RefreshTokenRepository │ ApiKeyRepository  │    │
│  └─────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼ (Proxied requests with validated JWT)
┌─────────────────────────────────────────────────────────────────────┐
│                      MICROSERVICES (Tier 1 Only)                     │
│  ┌─────────────────────────────────────────────────────────────┐    │
│  │              JWT Validation Components ONLY                  │    │
│  │  ┌──────────────────┐ ┌────────────────────────────────────┐│    │
│  │  │  JwtTokenService │ │    JwtAuthenticationFilter         ││    │
│  │  └──────────────────┘ └────────────────────────────────────┘│    │
│  └─────────────────────────────────────────────────────────────┘    │
│                                                                      │
│  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐        │
│  │ quality-measure │ │   analytics     │ │     fhir        │ ...    │
│  │    -service     │ │    -service     │ │   -service      │        │
│  └─────────────────┘ └─────────────────┘ └─────────────────┘        │
└─────────────────────────────────────────────────────────────────────┘
```

## Configuration

### Microservices (Default)

No special configuration needed. The `AuthenticationAutoConfiguration` provides JWT validation automatically:

```yaml
# application.yml for any microservice
jwt:
  secret: ${JWT_SECRET}
  expiration-ms: 3600000
```

### Gateway Service

The Gateway requires explicit configuration to enable the full auth stack:

**1. Enable auth controller in `application.yml`:**
```yaml
authentication:
  controller:
    enabled: true
```

**2. Gateway-specific bean configuration (`GatewayAuthenticationConfig.java`):**

```java
@Configuration
@ConditionalOnProperty(
    name = "authentication.controller.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@EnableJpaRepositories(basePackages = {
    "com.healthdata.authentication.repository"
})
@EntityScan(basePackages = {
    "com.healthdata.authentication.domain",
    "com.healthdata.authentication.entity"
})
public class GatewayAuthenticationConfig {

    @Bean
    public LogoutService logoutService(
            UserRepository userRepository,
            @Autowired(required = false) CacheEvictionService cacheEvictionService) {
        return new LogoutService(userRepository, cacheEvictionService);
    }

    @Bean
    public MfaService mfaService(UserRepository userRepository) {
        return new MfaService(userRepository);
    }

    @Bean
    public RefreshTokenService refreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            JwtTokenService jwtTokenService,
            JwtConfig jwtConfig) {
        return new RefreshTokenService(refreshTokenRepository, userRepository, jwtTokenService, jwtConfig);
    }

    @Bean
    public ApiKeyService apiKeyService(ApiKeyRepository apiKeyRepository) {
        return new ApiKeyService(apiKeyRepository);
    }
}
```

## Adding New Microservices

When creating a new microservice:

1. **Include the authentication module dependency:**
   ```groovy
   implementation project(':modules:shared:infrastructure:authentication')
   ```

2. **No additional configuration needed** - JWT validation is auto-configured

3. **Do NOT** include `authentication.controller.enabled: true` - this would attempt to load the full auth stack

4. **Verify startup** - the service should start without any `UserRepository` or database-related auth errors

## Adding New Gateway-Only Services

When adding a new authentication service that requires database access:

1. **Create the service class WITHOUT `@Service` annotation:**
   ```java
   // NOTE: No @Service annotation - this bean must be explicitly configured in Gateway service
   @Slf4j
   @RequiredArgsConstructor
   public class NewAuthService {
       private final UserRepository userRepository;
       // ...
   }
   ```

2. **Add explicit bean configuration in `GatewayAuthenticationConfig.java`:**
   ```java
   @Bean
   public NewAuthService newAuthService(UserRepository userRepository) {
       return new NewAuthService(userRepository);
   }
   ```

3. **Update this documentation** to include the new service in the Tier 2 table

## Troubleshooting

### "Bean of type 'UserRepository' not found" in microservice

**Cause:** A Tier 2 service is being loaded in a microservice context.

**Solution:**
1. Verify the service class does NOT have `@Service` annotation
2. Verify `authentication.controller.enabled` is NOT set to `true` in the microservice
3. Verify `AuthenticationAutoConfiguration` does NOT scan the service package

### Gateway auth endpoints not working

**Cause:** Tier 2 services not properly configured.

**Solution:**
1. Verify `authentication.controller.enabled: true` in Gateway's application.yml
2. Verify `GatewayAuthenticationConfig` is in a scanned package
3. Check that all required repositories are available

### MFA or refresh tokens not persisting

**Cause:** Database repositories not properly configured.

**Solution:**
1. Verify `@EnableJpaRepositories` includes `com.healthdata.authentication.repository`
2. Verify `@EntityScan` includes `com.healthdata.authentication.domain` and `.entity`
3. Check database connectivity and migrations

## Security Considerations

### HIPAA Compliance

- `LogoutService` performs PHI cache eviction on logout (HIPAA 45 CFR 164.312(a)(2)(i))
- All auth operations are logged for audit trail
- Token revocation is immediate and tracked

### Token Security

- JWTs are signed with HS256 algorithm
- Refresh tokens stored in database for revocation
- API keys hashed with SHA-256 before storage

### MFA Security

- TOTP using HMAC-SHA1 (Google Authenticator compatible)
- 8 recovery codes generated per user
- Recovery codes are single-use

## Related Documentation

- `/backend/HIPAA-CACHE-COMPLIANCE.md` - PHI cache eviction requirements
- `/backend/modules/shared/infrastructure/authentication/README.md` - Module documentation
