# CORS Configuration Guide

## Overview

Cross-Origin Resource Sharing (CORS) is centralized at the **Gateway Service** level. Individual backend services should NOT implement their own CORS handling.

## Architecture

```
Frontend (localhost:4200)
        │
        ▼
   API Gateway (8001)  ◄── CORS handled here
        │
        ▼
   Backend Services (8081-8099)  ◄── No CORS needed
```

## Gateway CORS Configuration

**Location**: `gateway-service/src/main/resources/application.yml`

```yaml
gateway:
  auth:
    cors:
      allowed-origins:
        - http://localhost:4200    # Clinical Portal
        - http://localhost:4201    # Admin Portal
        - http://localhost:4202    # Development
      allowed-methods:
        - GET
        - POST
        - PUT
        - PATCH
        - DELETE
        - OPTIONS
      allowed-headers:
        - "*"
      allow-credentials: true
      max-age: 3600  # Preflight cache: 1 hour
```

## Production Configuration

For production, update `application-prod.yml`:

```yaml
gateway:
  auth:
    cors:
      allowed-origins:
        - https://clinical.hdim.health
        - https://admin.hdim.health
      allowed-methods:
        - GET
        - POST
        - PUT
        - PATCH
        - DELETE
        - OPTIONS
      allowed-headers:
        - Authorization
        - Content-Type
        - X-Tenant-ID
        - X-Request-ID
      allow-credentials: true
      max-age: 86400  # Preflight cache: 24 hours
```

## Why Gateway-Only CORS?

1. **Single Point of Configuration**: All CORS rules in one place
2. **Security**: Prevents inconsistent CORS policies across services
3. **Simplicity**: Backend services remain CORS-agnostic
4. **Performance**: Gateway handles preflight requests efficiently

## Backend Service Guidelines

### DO NOT Add CORS to Backend Services

```java
// ❌ WRONG - Do not add @CrossOrigin to controllers
@CrossOrigin(origins = "*")
@RestController
public class PatientController { }

// ❌ WRONG - Do not add CorsConfiguration beans
@Bean
public CorsConfigurationSource corsConfigurationSource() { }
```

### DO Allow Gateway Headers

Backend services should trust headers from the gateway:

```java
// ✅ CORRECT - Trust gateway headers, no CORS handling
@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {
    // Gateway handles CORS, just process requests
}
```

## Troubleshooting

### Common Issues

| Symptom | Cause | Solution |
|---------|-------|----------|
| `Access-Control-Allow-Origin` missing | Request bypassing gateway | Ensure frontend calls gateway URL |
| Preflight (OPTIONS) fails | Gateway not handling OPTIONS | Check `allowed-methods` includes OPTIONS |
| Credentials not sent | `allow-credentials: false` | Set to `true` and specify origins |
| "Wildcard with credentials" error | Using `*` with credentials | List specific origins instead |

### Debugging CORS Issues

1. **Check browser console** for CORS errors
2. **Verify request URL** points to gateway (port 8001)
3. **Inspect preflight** response headers:
   ```bash
   curl -X OPTIONS http://localhost:8001/api/patients \
     -H "Origin: http://localhost:4200" \
     -H "Access-Control-Request-Method: GET" \
     -v
   ```

## Security Considerations

1. **Never use `*` for origins in production** - Always list specific domains
2. **Limit allowed methods** - Only include methods your API uses
3. **Restrict headers** - Instead of `*`, list specific headers needed
4. **Use HTTPS in production** - CORS doesn't encrypt data

## Related Documentation

- [Gateway Trust Architecture](GATEWAY_TRUST_ARCHITECTURE.md)
- [Authentication Guide](../AUTHENTICATION_GUIDE.md)
- [Production Security Guide](../docs/PRODUCTION_SECURITY_GUIDE.md)
