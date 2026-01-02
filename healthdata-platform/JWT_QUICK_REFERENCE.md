# JWT Security Quick Reference

## Quick Start

### 1. Environment Setup

```bash
# Set JWT secret (production)
export JWT_SECRET="your-256-bit-secure-random-secret-key"
```

### 2. Generate Token

```java
// In your service
@Autowired
private JwtTokenProvider tokenProvider;

// Generate access token
String token = tokenProvider.generateToken("username", List.of("ADMIN", "PROVIDER"));

// Generate token pair
Map<String, String> tokens = tokenProvider.generateTokenPair("username", List.of("ADMIN"));
```

### 3. Validate Token

```java
if (tokenProvider.validateToken(token)) {
    String username = tokenProvider.getUsernameFromToken(token);
    List<String> roles = tokenProvider.getRolesFromToken(token);
}
```

### 4. Use in Request

```bash
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/patients
```

## Common Operations

### Get Current User

```java
// Method 1: Using SecurityContext
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String username = auth.getName();

// Method 2: Using JwtUtils
String username = JwtUtils.getCurrentUsername();
```

### Check User Roles

```java
// Method 1: Using UserPrincipal
if (principal.hasRole("ADMIN")) { ... }
if (principal.hasAnyRole("ADMIN", "PROVIDER")) { ... }
if (principal.hasAllRoles("ADMIN", "PROVIDER")) { ... }

// Method 2: Using JwtUtils
if (JwtUtils.hasRole("ADMIN")) { ... }
if (JwtUtils.hasAnyRole("ADMIN", "PROVIDER")) { ... }
```

### Secure Endpoint

```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin/users")
public List<User> getAllUsers() { ... }

@PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER')")
@GetMapping("/reports")
public List<Report> getReports() { ... }
```

## API Endpoints

| Endpoint | Method | Description | Auth |
|----------|--------|-------------|------|
| `/api/auth/login` | POST | Authenticate user | No |
| `/api/auth/refresh` | POST | Refresh access token | No |
| `/api/auth/me` | POST | Get current user | Yes |

## Token Claims

```json
{
  "sub": "username",
  "roles": ["ADMIN", "PROVIDER"],
  "iat": 1701432000,
  "exp": 1701433800
}
```

## Status Codes

| Code | Meaning | Cause |
|------|---------|-------|
| 200 | OK | Request successful |
| 401 | Unauthorized | Missing/invalid token |
| 403 | Forbidden | Insufficient permissions |
| 400 | Bad Request | Invalid credentials |

## Configuration Properties

```yaml
spring.security.jwt.secret: JWT signing secret (minimum 256 bits)
spring.security.jwt.expiration: Access token expiration (ms)
spring.security.jwt.refresh-expiration: Refresh token expiration (ms)
```

## User Roles

- `ADMIN` - Full access
- `PROVIDER` - Provider access
- `CARE_MANAGER` - Care management
- `PATIENT` - Patient self-service

## Default Expiration Times

- Access Token: 15 minutes (900,000 ms)
- Refresh Token: 7 days (604,800,000 ms)

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Token invalid | Verify JWT_SECRET matches |
| Token expired | Use refresh endpoint |
| 401 error | Ensure "Authorization: Bearer <token>" format |
| 403 error | User lacks required role |
| CORS error | Check allowed origins in config |

## File Locations

```
src/main/java/com/healthdata/shared/security/
├── jwt/JwtTokenProvider.java
├── jwt/JwtAuthenticationFilter.java
├── jwt/JwtConstants.java
├── config/SecurityConfig.java
├── model/UserPrincipal.java
├── util/JwtUtils.java
└── api/AuthenticationController.java
```

## Security Best Practices

1. Always use HTTPS in production
2. Store JWT_SECRET securely (never in code)
3. Use strong random secret (256+ bits)
4. Keep expiration times reasonable
5. Validate tokens on every protected request
6. Log authentication failures
7. Implement token rotation
8. Monitor for suspicious patterns

## Performance Metrics

- Token generation: ~1ms
- Token validation: ~1-5ms
- No database queries for stateless validation
- Horizontal scaling: No session sync needed

---

For detailed documentation, see `JWT_SECURITY_IMPLEMENTATION.md`
