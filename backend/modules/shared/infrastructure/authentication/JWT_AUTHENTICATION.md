# JWT Authentication Documentation

## Overview

This document describes the JWT (JSON Web Token) authentication system implemented for the HealthData in Motion platform. JWT authentication provides a stateless, scalable, and secure alternative to session-based authentication.

## Table of Contents

1. [Architecture](#architecture)
2. [How to Obtain Tokens](#how-to-obtain-tokens)
3. [How to Use Tokens](#how-to-use-tokens)
4. [Token Structure](#token-structure)
5. [Token Expiration and Refresh](#token-expiration-and-refresh)
6. [Security Considerations](#security-considerations)
7. [Migration Guide](#migration-guide-from-basic-auth)
8. [Configuration](#configuration)
9. [Troubleshooting](#troubleshooting)

---

## Architecture

### Components

1. **JwtTokenService** - Generates and validates JWT tokens
2. **JwtAuthenticationFilter** - Intercepts requests and validates JWT tokens
3. **RefreshTokenService** - Manages refresh token lifecycle
4. **AuthController** - Provides authentication endpoints
5. **TokenCleanupScheduler** - Periodic cleanup of expired tokens

### Flow Diagram

```
┌────────────┐                                    ┌────────────┐
│            │   1. POST /auth/login              │            │
│   Client   │─────────────────────────────────▶ │   Server   │
│            │      username + password           │            │
└────────────┘                                    └────────────┘
                                                         │
                                                         │ 2. Authenticate
                                                         │
                                                         ▼
┌────────────┐                                    ┌────────────┐
│            │  3. Return access + refresh token  │            │
│   Client   │◀───────────────────────────────── │   Server   │
│            │                                    │            │
└────────────┘                                    └────────────┘
     │
     │ 4. Store tokens securely
     │
     ▼
┌────────────┐                                    ┌────────────┐
│            │  5. API Request with Bearer token  │            │
│   Client   │─────────────────────────────────▶ │   Server   │
│            │     Authorization: Bearer <token>  │            │
└────────────┘                                    └────────────┘
                                                         │
                                                         │ 6. Validate token
                                                         │
                                                         ▼
┌────────────┐                                    ┌────────────┐
│            │  7. Return API response            │            │
│   Client   │◀───────────────────────────────── │   Server   │
│            │                                    │            │
└────────────┘                                    └────────────┘
```

---

## How to Obtain Tokens

### 1. Login Request

**Endpoint:** `POST /api/v1/auth/login`

**Request Body:**
```json
{
  "username": "your-username",
  "password": "your-password"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "username": "your-username",
  "email": "your-email@example.com",
  "roles": ["USER", "ADMIN"],
  "tenantIds": ["tenant-1", "tenant-2"],
  "message": "Login successful"
}
```

### 2. Error Responses

**401 Unauthorized - Invalid Credentials:**
```json
{
  "timestamp": "2025-01-01T12:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid username or password"
}
```

**401 Unauthorized - Account Locked:**
```json
{
  "timestamp": "2025-01-01T12:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Account is locked. Please try again later."
}
```

---

## How to Use Tokens

### 1. Making Authenticated Requests

Include the access token in the `Authorization` header:

```http
GET /api/v1/auth/me HTTP/1.1
Host: api.healthdata.com
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
Content-Type: application/json
```

### 2. Example with cURL

```bash
curl -X GET "https://api.healthdata.com/api/v1/auth/me" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json"
```

### 3. Example with JavaScript (Fetch API)

```javascript
const accessToken = 'eyJhbGciOiJIUzUxMiJ9...';

fetch('https://api.healthdata.com/api/v1/auth/me', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'application/json'
  }
})
.then(response => response.json())
.then(data => console.log(data));
```

### 4. Example with Python (requests)

```python
import requests

access_token = 'eyJhbGciOiJIUzUxMiJ9...'
headers = {
    'Authorization': f'Bearer {access_token}',
    'Content-Type': 'application/json'
}

response = requests.get('https://api.healthdata.com/api/v1/auth/me', headers=headers)
print(response.json())
```

---

## Token Structure

### JWT Claims

Both access and refresh tokens contain the following claims:

| Claim | Description | Example |
|-------|-------------|---------|
| `sub` | Subject (username) | "john.doe" |
| `userId` | User UUID | "123e4567-e89b-12d3-a456-426614174000" |
| `tenantIds` | Tenant IDs (comma-separated) | "tenant-1,tenant-2" |
| `roles` | User roles (comma-separated) | "USER,ADMIN" |
| `iss` | Issuer | "healthdata-in-motion" |
| `aud` | Audience | "healthdata-api" |
| `iat` | Issued at (timestamp) | 1704110400 |
| `exp` | Expiration (timestamp) | 1704111300 |
| `jti` | JWT ID (unique token ID) | "a1b2c3d4..." |

### Token Example (Decoded)

**Header:**
```json
{
  "alg": "HS512",
  "typ": "JWT"
}
```

**Payload:**
```json
{
  "sub": "john.doe",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "tenantIds": "tenant-1,tenant-2",
  "roles": "USER,ADMIN",
  "iss": "healthdata-in-motion",
  "aud": "healthdata-api",
  "iat": 1704110400,
  "exp": 1704111300,
  "jti": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

---

## Token Expiration and Refresh

### Token Lifetimes

- **Access Token:** 15 minutes (default)
- **Refresh Token:** 7 days (default)

### Refreshing Access Tokens

When an access token expires, use the refresh token to obtain a new one:

**Endpoint:** `POST /api/v1/auth/refresh`

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "username": "your-username",
  "email": "your-email@example.com",
  "roles": ["USER", "ADMIN"],
  "tenantIds": ["tenant-1", "tenant-2"],
  "message": "Token refreshed successfully"
}
```

### Refresh Token Rotation

For security, refresh tokens are rotated on each use:
1. Client sends old refresh token
2. Server validates old refresh token
3. Server generates new access + refresh tokens
4. Server revokes old refresh token
5. Server returns new tokens to client

### Automatic Token Refresh (Client Implementation)

```javascript
let accessToken = 'eyJhbGciOiJIUzUxMiJ9...';
let refreshToken = 'eyJhbGciOiJIUzUxMiJ9...';

async function fetchWithAuth(url, options = {}) {
  // Add Authorization header
  options.headers = {
    ...options.headers,
    'Authorization': `Bearer ${accessToken}`
  };

  let response = await fetch(url, options);

  // If 401, try refreshing token
  if (response.status === 401) {
    const refreshResponse = await fetch('/api/v1/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    });

    if (refreshResponse.ok) {
      const data = await refreshResponse.json();
      accessToken = data.accessToken;
      refreshToken = data.refreshToken;

      // Retry original request with new token
      options.headers['Authorization'] = `Bearer ${accessToken}`;
      response = await fetch(url, options);
    } else {
      // Refresh failed, redirect to login
      window.location.href = '/login';
    }
  }

  return response;
}
```

---

## Security Considerations

### Token Storage

**DO:**
- Store access tokens in memory (JavaScript variable)
- Store refresh tokens in httpOnly cookies (server-side)
- Store refresh tokens in secure storage (mobile apps)

**DON'T:**
- Store tokens in localStorage (XSS vulnerable)
- Store tokens in sessionStorage (XSS vulnerable)
- Include tokens in URLs (logged in server logs)
- Store tokens in plain text files

### Token Validation

The server validates tokens on every request:
1. Signature verification (HS512)
2. Expiration check
3. Issuer validation
4. Audience validation
5. Database check (refresh tokens only)

### Revocation

Revoke tokens when:
- User logs out: `POST /api/v1/auth/logout`
- User logs out from all devices: `POST /api/v1/auth/revoke`
- Password changed: Revoke all tokens
- Security breach detected: Revoke all tokens

**Logout (Single Device):**
```bash
curl -X POST "https://api.healthdata.com/api/v1/auth/logout" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "YOUR_REFRESH_TOKEN"}'
```

**Logout (All Devices):**
```bash
curl -X POST "https://api.healthdata.com/api/v1/auth/revoke" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

## Migration Guide (From Basic Auth)

### Backward Compatibility

JWT and Basic Auth can coexist:
- JWT is the primary authentication method
- Basic Auth is supported for backward compatibility
- Both work with the same endpoints

### Migration Steps

#### Phase 1: Preparation (Week 1)
1. Update client applications to support JWT
2. Test JWT authentication in development
3. Verify token refresh flow works

#### Phase 2: Gradual Rollout (Weeks 2-4)
1. Deploy JWT-enabled services
2. Monitor JWT usage vs Basic Auth
3. Encourage clients to migrate to JWT
4. Keep Basic Auth enabled

#### Phase 3: Complete Migration (Week 5+)
1. Verify all clients use JWT
2. Disable Basic Auth (configuration change)
3. Monitor for any issues
4. Update documentation

### Client Migration Example

**Before (Basic Auth):**
```javascript
fetch('https://api.healthdata.com/api/v1/data', {
  headers: {
    'Authorization': 'Basic ' + btoa(username + ':' + password)
  }
});
```

**After (JWT):**
```javascript
// 1. Login once to get tokens
const loginResponse = await fetch('https://api.healthdata.com/api/v1/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username, password })
});
const { accessToken, refreshToken } = await loginResponse.json();

// 2. Use access token for subsequent requests
fetch('https://api.healthdata.com/api/v1/data', {
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
});
```

---

## Configuration

### Environment Variables

```bash
# Required
JWT_SECRET=your-secret-key-must-be-at-least-256-bits-long

# Optional (defaults shown)
JWT_ACCESS_TOKEN_EXPIRATION=15m
JWT_REFRESH_TOKEN_EXPIRATION=7d
JWT_ISSUER=healthdata-in-motion
JWT_AUDIENCE=healthdata-api
JWT_ENABLED=true
BASIC_AUTH_ENABLED=true
```

### Application Configuration

**application.yml:**
```yaml
jwt:
  secret: ${JWT_SECRET}
  access-token-expiration: 15m
  refresh-token-expiration: 7d
  issuer: healthdata-in-motion
  audience: healthdata-api

security:
  authentication:
    jwt-enabled: true
    basic-auth-enabled: true
```

### Secret Key Generation

Generate a secure secret key (minimum 256 bits):

```bash
# Using OpenSSL
openssl rand -base64 64

# Using Python
python3 -c "import secrets; print(secrets.token_urlsafe(64))"

# Using Node.js
node -e "console.log(require('crypto').randomBytes(64).toString('base64'))"
```

---

## Troubleshooting

### Common Issues

#### 1. "JWT secret key is too short"

**Error:**
```
IllegalStateException: JWT secret key is too short. Key length: 20 bytes, minimum required: 32 bytes
```

**Solution:**
- Generate a new secret key (minimum 256 bits / 32 bytes)
- Update `JWT_SECRET` environment variable
- Restart services

#### 2. "Invalid JWT signature"

**Error:**
```
401 Unauthorized: Invalid JWT signature
```

**Causes:**
- Token was tampered with
- Wrong secret key on server
- Token signed by different service

**Solution:**
- Verify `JWT_SECRET` is correct on all services
- Regenerate tokens by logging in again
- Check for man-in-the-middle attacks

#### 3. "Token expired"

**Error:**
```
401 Unauthorized: Token expired
```

**Solution:**
- Use refresh token to get new access token
- If refresh token also expired, login again

#### 4. "Refresh token not found or revoked"

**Error:**
```
401 Unauthorized: Invalid refresh token
```

**Causes:**
- Token was revoked (logout)
- Token expired
- Token not in database

**Solution:**
- Login again to get new tokens

### Debugging Tips

**Enable Debug Logging:**
```yaml
logging:
  level:
    com.healthdata.authentication: DEBUG
    org.springframework.security: DEBUG
```

**Check Token Contents:**
Use [jwt.io](https://jwt.io) to decode tokens (don't paste production tokens!)

**Verify Token in Database:**
```sql
SELECT * FROM refresh_tokens WHERE user_id = 'YOUR_USER_ID' AND revoked_at IS NULL;
```

---

## API Reference

### Authentication Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/auth/login` | Login and get tokens | No |
| POST | `/api/v1/auth/refresh` | Refresh access token | No |
| POST | `/api/v1/auth/logout` | Logout (revoke token) | Yes |
| POST | `/api/v1/auth/revoke` | Revoke all tokens | Yes |
| GET | `/api/v1/auth/me` | Get current user info | Yes |

---

## Support

For issues or questions:
- Technical Support: support@healthdata.com
- Security Issues: security@healthdata.com
- Documentation: https://docs.healthdata.com

---

**Last Updated:** January 2025
**Version:** 1.0.0
