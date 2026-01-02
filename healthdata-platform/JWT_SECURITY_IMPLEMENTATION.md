# JWT Security Implementation for HealthData Platform

## Overview

This document describes the comprehensive JWT (JSON Web Token) security implementation for the HealthData Platform. The implementation uses industry-standard JWT authentication with HS256 (HMAC SHA-256) algorithm for token signing.

## Architecture

### Components

1. **JwtTokenProvider** - Generates and validates JWT tokens
2. **JwtAuthenticationFilter** - Validates tokens and sets SecurityContext
3. **SecurityConfig** - Configures Spring Security with JWT authentication
4. **UserPrincipal** - Represents authenticated user in SecurityContext
5. **JwtAuthenticationEntryPoint** - Handles authentication errors (401)
6. **JwtAccessDeniedHandler** - Handles authorization errors (403)
7. **JwtConstants** - Centralized JWT constants
8. **JwtUtils** - Utility methods for authentication operations

### File Structure

```
src/main/java/com/healthdata/shared/security/
├── jwt/
│   ├── JwtTokenProvider.java          # Token generation and validation
│   ├── JwtAuthenticationFilter.java    # Token extraction and validation filter
│   ├── JwtConstants.java              # JWT constants
│   └── ...
├── config/
│   ├── SecurityConfig.java            # Spring Security configuration
│   ├── JwtAuthenticationEntryPoint.java
│   └── JwtAccessDeniedHandler.java
├── model/
│   └── UserPrincipal.java            # UserDetails implementation
├── util/
│   └── JwtUtils.java                 # Utility methods
├── api/
│   └── AuthenticationController.java  # Authentication endpoints
└── exception/
    ├── JwtException.java
    ├── InvalidTokenException.java
    └── TokenExpiredException.java
```

## Key Features

### 1. Token Generation

JwtTokenProvider generates two types of tokens:

```java
// Access Token (15 minutes expiration by default)
String accessToken = tokenProvider.generateToken(username, roles);

// Refresh Token (7 days expiration by default)
String refreshToken = tokenProvider.generateRefreshToken(username, roles);

// Both tokens
Map<String, String> tokens = tokenProvider.generateTokenPair(username, roles);
```

### 2. Token Claims

Each JWT token contains:
- `sub` - Username (subject)
- `exp` - Expiration time
- `iat` - Issued at time
- `roles` - List of user roles

Example decoded token payload:
```json
{
  "sub": "john.doe",
  "roles": ["PROVIDER", "ADMIN"],
  "iat": 1701432000,
  "exp": 1701433800
}
```

### 3. Token Validation

The JwtAuthenticationFilter validates tokens on each request:

```
Request → Extract token from Authorization header
         → Validate signature and expiration
         → Extract username and roles
         → Create UsernamePasswordAuthenticationToken
         → Set in SecurityContext
```

### 4. Stateless Authentication

- No server-side session storage
- Tokens are self-contained and cryptographically signed
- Scales horizontally without session synchronization
- Each service can verify tokens independently

## Configuration

### Application.yml

```yaml
spring:
  security:
    jwt:
      secret: ${JWT_SECRET:mySecretKeyForJWTShouldBeAtLeast256BitsLongForHS256Algorithm123456}
      expiration: 900000          # 15 minutes in milliseconds
      refresh-expiration: 604800000  # 7 days in milliseconds
```

### Environment Variables

For production, set the JWT secret via environment variable:

```bash
export JWT_SECRET="your-very-long-and-secure-secret-key-at-least-256-bits"
```

### Security Best Practices

1. **Secret Key**
   - Minimum 256 bits (32 bytes) for HS256
   - Use strong random generation
   - Store securely (never in version control)
   - Rotate regularly in production

2. **Token Expiration**
   - Short expiration for access tokens (15 minutes)
   - Longer expiration for refresh tokens (7 days)
   - Implement refresh token rotation

3. **HTTPS**
   - Always use HTTPS in production
   - Prevents token interception

4. **CORS**
   - Configured for specific origins
   - Prevents cross-origin token theft

## Usage Examples

### 1. Authentication Endpoint

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -d "username=john.doe&password=secure_password" \
  -H "Content-Type: application/x-www-form-urlencoded"

# Response
{
  "status": "success",
  "message": "User authenticated successfully",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "john.doe",
  "roles": ["PROVIDER"],
  "expiresIn": 900
}
```

### 2. Using Access Token

```bash
# Request with JWT token
curl -X GET http://localhost:8080/api/patients \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

### 3. Refresh Token

```bash
# Get new access token
curl -X POST http://localhost:8080/api/auth/refresh \
  -d "refreshToken=eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/x-www-form-urlencoded"

# Response
{
  "status": "success",
  "message": "Token refreshed successfully",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 900
}
```

### 4. Get Current User

```bash
curl -X POST http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."

# Response
{
  "status": "success",
  "username": "john.doe",
  "roles": ["PROVIDER"]
}
```

## Endpoint Authorization

### Public Endpoints (No authentication required)

- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `GET /actuator/health`
- `GET /swagger-ui/**`
- `GET /v3/api-docs/**`

### Protected Endpoints (Authentication required)

**Patient Endpoints:**
- `GET /api/patients/**` - PATIENT, PROVIDER, ADMIN, CARE_MANAGER
- `POST /api/patients/**` - PROVIDER, ADMIN
- `PUT /api/patients/**` - PROVIDER, ADMIN
- `DELETE /api/patients/**` - ADMIN only

**FHIR Endpoints:**
- `GET|POST|PUT /api/fhir/**` - PROVIDER, ADMIN, CARE_MANAGER

**Quality Measure Endpoints:**
- `GET /api/measures/**` - PROVIDER, ADMIN, CARE_MANAGER, PATIENT
- `POST|PUT /api/measures/**` - ADMIN only

**Care Gap Endpoints:**
- `GET|POST|PUT /api/care-gaps/**` - PROVIDER, ADMIN, CARE_MANAGER

**Admin Endpoints:**
- `GET|POST|PUT|DELETE /api/admin/**` - ADMIN only

## Using UserPrincipal

### In Controllers

```java
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.healthdata.shared.security.model.UserPrincipal;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    @GetMapping("/{id}")
    public ResponseEntity<Patient> getPatient(@PathVariable String id) {
        // Get current authentication
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        // Get current user principal
        UserPrincipal user = (UserPrincipal) auth.getPrincipal();

        // Check roles
        if (user.hasRole("ADMIN")) {
            // Admin logic
        }

        return ResponseEntity.ok(patientService.getPatient(id));
    }
}
```

### With JwtUtils

```java
import com.healthdata.shared.security.util.JwtUtils;

@RestController
@RequestMapping("/api/measures")
public class MeasureController {

    @GetMapping
    public ResponseEntity<List<Measure>> getMeasures() {
        String currentUser = JwtUtils.getCurrentUsername();

        if (JwtUtils.hasAnyRole("ADMIN", "PROVIDER")) {
            // Authorized logic
        }

        return ResponseEntity.ok(measureService.getMeasures());
    }
}
```

### Method Security

```java
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER')")
    @GetMapping("/reports")
    public ResponseEntity<List<Report>> getReports() {
        return ResponseEntity.ok(reportService.getReports());
    }
}
```

## Error Handling

### 401 Unauthorized

Returned when:
- Token is missing
- Token is invalid or malformed
- Token signature verification fails
- Token is expired

Response:
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource",
  "path": "/api/patients",
  "timestamp": 1701432000000
}
```

### 403 Forbidden

Returned when:
- User is authenticated but lacks required role

Response:
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "You do not have permission to access this resource",
  "detail": "Access Denied",
  "path": "/api/admin/users",
  "timestamp": 1701432000000
}
```

## Testing

### Integration Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthenticationFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Test
    void testValidToken() throws Exception {
        String token = tokenProvider.generateToken("testuser", List.of("PROVIDER"));

        mockMvc.perform(get("/api/patients")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void testInvalidToken() throws Exception {
        mockMvc.perform(get("/api/patients")
                .header("Authorization", "Bearer invalid_token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testNoToken() throws Exception {
        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isUnauthorized());
    }
}
```

## CORS Configuration

Configured for:

**Development:**
- http://localhost:3000 (React)
- http://localhost:4200 (Angular)
- http://localhost:8080 (Backend same-origin)

**Production:**
- https://healthdata-platform.vercel.app
- https://healthdata-platform.com
- https://*.healthdata.com

**Allowed Methods:**
- GET, POST, PUT, DELETE, PATCH, OPTIONS

**Exposed Headers:**
- Authorization
- X-Total-Count
- X-Page-Number
- X-Page-Size

## User Roles

The system supports the following roles:

| Role | Description |
|------|-------------|
| ADMIN | Full system access, user management |
| PROVIDER | Healthcare provider, patient management |
| CARE_MANAGER | Care coordination, gap management |
| PATIENT | Patient self-service access |
| SYSTEM | Service-to-service authentication |

## Security Checklist

- [x] JWT tokens signed with HS256
- [x] Token expiration implemented
- [x] Refresh token support
- [x] HTTPS enforcement (production)
- [x] CORS properly configured
- [x] CSRF disabled for API
- [x] Stateless session management
- [x] Role-based access control
- [x] Method-level security
- [x] Comprehensive error handling
- [x] Audit logging
- [x] Token claim validation

## Performance Considerations

- **Token Size:** ~200-400 bytes per token
- **Validation Time:** <5ms per request
- **No Database Hits:** Tokens are self-contained
- **Horizontal Scaling:** No session synchronization needed

## References

- [JJWT Library](https://github.com/jwtk/jjwt)
- [JWT.io](https://jwt.io)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [RFC 7519 - JSON Web Token](https://tools.ietf.org/html/rfc7519)

## Troubleshooting

### Token Validation Failures

1. **Check secret key** - Ensure JWT_SECRET matches across instances
2. **Check expiration** - Token may be expired
3. **Check header format** - Must be "Authorization: Bearer <token>"
4. **Check signature** - Token may have been modified

### CORS Issues

1. **Check allowed origins** - Verify frontend origin is in whitelist
2. **Check preflight request** - OPTIONS request should succeed
3. **Check credentials** - allowCredentials must be true

### Performance Issues

1. **Check token size** - Reduce claims if necessary
2. **Check filter chain** - Ensure JWT filter is optimized
3. **Check database queries** - Token validation shouldn't query DB

## Future Enhancements

1. **OAuth 2.0/OIDC** - Support OpenID Connect providers
2. **Token Introspection** - Query token metadata without parsing
3. **Token Revocation** - Blacklist/revoke tokens
4. **Asymmetric Signing** - Use RS256 instead of HS256
5. **MFA Integration** - Multi-factor authentication support
6. **Token Audit** - Track token creation and usage

---

Last Updated: 2024
Version: 1.0
