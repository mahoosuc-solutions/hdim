# Authentication Guide - HealthData In Motion
**Version**: 1.0
**Last Updated**: November 20, 2025

---

## Overview

HealthData In Motion uses JWT (JSON Web Token) based authentication with role-based access control (RBAC). This guide covers test users, authentication flow, and API usage.

---

## Test Users

All test users have been pre-created with the password: **`password123`**

### Available Test Users

| Username | Email | Role(s) | Access Level |
|----------|-------|---------|--------------|
| `test_superadmin` | superadmin@test.com | SUPER_ADMIN | Full system access |
| `test_admin` | admin@test.com | ADMIN | Administrative functions |
| `test_evaluator` | evaluator@test.com | EVALUATOR | CQL evaluation, measure calculation |
| `test_analyst` | analyst@test.com | ANALYST | Quality reports, analytics |
| `test_viewer` | viewer@test.com | VIEWER | Read-only access |
| `test_multiuser` | multi@test.com | ADMIN, ANALYST, EVALUATOR | Multiple roles for testing |

---

## Authentication Flow

### 1. Login and Get JWT Token

**Endpoint**: `POST /api/v1/auth/login`

```bash
curl -X POST http://localhost:8087/quality-measure/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "test_admin",
    "password": "password123"
  }'
```

**Response**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "uuid",
    "username": "test_admin",
    "email": "admin@test.com",
    "firstName": "Test",
    "lastName": "Admin",
    "roles": ["ADMIN"],
    "tenants": ["default"]
  }
}
```

### 2. Use Access Token in API Calls

Include the access token in the `Authorization` header:

```bash
TOKEN="your-access-token-here"

curl http://localhost:8087/quality-measure/patient-health/overview/patient123 \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: default"
```

### 3. Refresh Token (when access token expires)

**Endpoint**: `POST /api/v1/auth/refresh`

```bash
curl -X POST http://localhost:8087/quality-measure/api/v1/auth/refresh \
  -H 'Content-Type: application/json' \
  -d '{
    "refreshToken": "your-refresh-token"
  }'
```

### 4. Logout

**Endpoint**: `POST /api/v1/auth/logout`

```bash
curl -X POST http://localhost:8087/quality-measure/api/v1/auth/logout \
  -H "Authorization: Bearer $TOKEN"
```

---

## Quick Start Examples

### Example 1: Login as Admin and Access Patient Health API

```bash
#!/bin/bash

# 1. Login
RESPONSE=$(curl -s -X POST http://localhost:8087/quality-measure/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "test_admin",
    "password": "password123"
  }')

# 2. Extract access token
TOKEN=$(echo $RESPONSE | jq -r '.accessToken')

echo "Token: $TOKEN"

# 3. Submit PHQ-9 assessment
curl -X POST http://localhost:8087/quality-measure/patient-health/mental-health/assessments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: default" \
  -d '{
    "patientId": "patient123",
    "assessmentType": "phq-9",
    "responses": {"q1":2,"q2":2,"q3":1,"q4":1,"q5":1,"q6":2,"q7":1,"q8":1,"q9":1},
    "assessedBy": "test_admin"
  }'

# 4. Get patient health overview
curl http://localhost:8087/quality-measure/patient-health/overview/patient123 \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: default"
```

### Example 2: Test Role-Based Access Control

```bash
# Login as viewer (read-only)
VIEWER_TOKEN=$(curl -s -X POST http://localhost:8087/quality-measure/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"test_viewer","password":"password123"}' \
  | jq -r '.accessToken')

# Try to submit assessment (should fail with 403 Forbidden)
curl -X POST http://localhost:8087/quality-measure/patient-health/mental-health/assessments \
  -H "Authorization: Bearer $VIEWER_TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: default" \
  -d '{"patientId":"test","assessmentType":"phq-9","responses":{}}'

# Read operations should work
curl http://localhost:8087/quality-measure/patient-health/overview/patient123 \
  -H "Authorization: Bearer $VIEWER_TOKEN" \
  -H "X-Tenant-ID: default"
```

---

## Role-Based Access Control (RBAC)

### Patient Health Overview API Permissions

| Endpoint | SUPER_ADMIN | ADMIN | EVALUATOR | ANALYST | VIEWER |
|----------|-------------|-------|-----------|---------|--------|
| `GET /overview/{patientId}` | ✓ | ✓ | ✓ | ✓ | ✓ |
| `POST /mental-health/assessments` | ✓ | ✓ | ✓ | ✗ | ✗ |
| `GET /mental-health/assessments/{patientId}` | ✓ | ✓ | ✓ | ✓ | ✓ |
| `GET /care-gaps/{patientId}` | ✓ | ✓ | ✓ | ✓ | ✓ |
| `PUT /care-gaps/{gapId}/address` | ✓ | ✓ | ✓ | ✗ | ✗ |
| `POST /risk-stratification/{patientId}/calculate` | ✓ | ✓ | ✓ | ✗ | ✗ |
| `GET /risk-stratification/{patientId}` | ✓ | ✓ | ✓ | ✓ | ✓ |
| `GET /health-score/{patientId}` | ✓ | ✓ | ✓ | ✓ | ✓ |

**Note**: Actual permissions are defined by `@PreAuthorize` annotations in the controller. Update as needed for your organization's access control policies.

---

## Token Configuration

### Access Token
- **Expiration**: 15 minutes (900 seconds)
- **Algorithm**: HS256 (HMAC-SHA256)
- **Issuer**: healthdata-in-motion
- **Audience**: healthdata-api

### Refresh Token
- **Expiration**: 7 days (168 hours)
- **Purpose**: Obtain new access tokens without re-login
- **Storage**: Database-backed for revocation support

---

## Frontend Integration

### Angular AuthService Example

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';

interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = 'http://localhost:8087/quality-measure/api/v1/auth';
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {
    // Load user from localStorage on init
    const storedUser = localStorage.getItem('currentUser');
    if (storedUser) {
      this.currentUserSubject.next(JSON.parse(storedUser));
    }
  }

  login(username: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.API_URL}/login`, {
      username,
      password
    }).pipe(
      tap(response => {
        // Store tokens and user
        localStorage.setItem('accessToken', response.accessToken);
        localStorage.setItem('refreshToken', response.refreshToken);
        localStorage.setItem('currentUser', JSON.stringify(response.user));
        this.currentUserSubject.next(response.user);
      })
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/logout`, {}).pipe(
      tap(() => {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('currentUser');
        this.currentUserSubject.next(null);
      })
    );
  }

  getAccessToken(): string | null {
    return localStorage.getItem('accessToken');
  }

  isAuthenticated(): boolean {
    return !!this.getAccessToken();
  }

  hasRole(role: string): boolean {
    const user = this.currentUserSubject.value;
    return user?.roles?.includes(role) || false;
  }
}
```

### HTTP Interceptor for Adding Token

```typescript
import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler } from '@angular/common/http';
import { AuthService } from './auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private authService: AuthService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler) {
    const token = this.authService.getAccessToken();

    if (token) {
      req = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }

    return next.handle(req);
  }
}
```

---

## Security Notes

### Development vs Production

**Current (Development)**:
- Simple BCrypt password hash
- Test users with known passwords
- Tokens stored in localStorage

**Production Requirements**:
1. **Password Policy**:
   - Minimum 12 characters
   - Require uppercase, lowercase, numbers, special characters
   - Password expiration (90 days)
   - No password reuse (last 5 passwords)

2. **Token Security**:
   - Store tokens in HttpOnly cookies (not localStorage)
   - Implement CSRF protection
   - Use secure flag for cookies (HTTPS only)
   - Short access token expiration (5-15 minutes)

3. **Additional Security**:
   - Multi-factor authentication (MFA)
   - Account lockout after failed attempts
   - IP whitelisting for sensitive roles
   - Audit logging for all authentication events

---

## Troubleshooting

### Issue: "Invalid credentials"

**Check**:
1. Verify username and password are correct
2. Check if account is locked: `SELECT * FROM users WHERE username = 'test_admin'`
3. Verify user is active: `active = true`

### Issue: "403 Forbidden"

**Check**:
1. Verify token is included in Authorization header
2. Check user has required role for endpoint
3. Verify token has not expired (15 minutes)

### Issue: "Token expired"

**Solution**: Use refresh token to get new access token

```bash
curl -X POST http://localhost:8087/quality-measure/api/v1/auth/refresh \
  -H 'Content-Type: application/json' \
  -d '{"refreshToken":"your-refresh-token"}'
```

---

## Testing Authentication

### Test Script: `test-authentication.sh`

```bash
#!/bin/bash

echo "==================================="
echo "Authentication Test Suite"
echo "==================================="

# Test 1: Login with valid credentials
echo "Test 1: Login with valid credentials"
RESPONSE=$(curl -s -X POST http://localhost:8087/quality-measure/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"test_admin","password":"password123"}')

TOKEN=$(echo $RESPONSE | jq -r '.accessToken')

if [ "$TOKEN" != "null" ] && [ -n "$TOKEN" ]; then
  echo "✓ Login successful"
else
  echo "✗ Login failed"
  exit 1
fi

# Test 2: Access protected endpoint with token
echo "Test 2: Access protected endpoint"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
  http://localhost:8087/quality-measure/patient-health/overview/test123 \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: default")

if [ "$HTTP_CODE" = "200" ]; then
  echo "✓ Authenticated access successful"
else
  echo "✗ Authenticated access failed (HTTP $HTTP_CODE)"
fi

# Test 3: Access without token (should fail)
echo "Test 3: Access without token"
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
  http://localhost:8087/quality-measure/patient-health/overview/test123 \
  -H "X-Tenant-ID: default")

if [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "403" ]; then
  echo "✓ Unauthorized access correctly blocked"
else
  echo "✗ Unauthorized access not blocked (HTTP $HTTP_CODE)"
fi

echo "==================================="
echo "All tests completed"
echo "==================================="
```

---

## Database Queries

### View All Users and Roles

```sql
SELECT
    u.username,
    u.email,
    u.first_name || ' ' || u.last_name as full_name,
    u.active,
    string_agg(DISTINCT ur.role, ', ') as roles,
    string_agg(DISTINCT ut.tenant_id, ', ') as tenants
FROM users u
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN user_tenants ut ON u.id = ut.user_id
GROUP BY u.username, u.email, u.first_name, u.last_name, u.active
ORDER BY u.username;
```

### Check User Authentication Status

```sql
SELECT
    username,
    active,
    email_verified,
    account_locked_until,
    failed_login_attempts,
    last_login_at
FROM users
WHERE username = 'test_admin';
```

---

**For More Information**:
- JWT Specification: https://jwt.io/
- Spring Security: https://spring.io/projects/spring-security
- OWASP Authentication Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html

---

*Last Updated: November 20, 2025*
*Version: 1.0*
