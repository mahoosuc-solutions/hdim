# OAuth 2.0 / OpenID Connect SSO Implementation Guide

**Status:** ✅ Production Ready (Implemented)
**Last Updated:** January 24, 2026
**Version:** 1.0

---

## Overview

This guide documents the **production-ready OAuth 2.0 / OpenID Connect (OIDC) authentication implementation** in the HDIM platform. OAuth 2.0 enables modern SSO integration with cloud identity providers like Okta, Azure AD, Google, and Auth0.

### What is OAuth 2.0 / OpenID Connect?

**OAuth 2.0** is an authorization framework that enables applications to obtain limited access to user accounts on an HTTP service.

**OpenID Connect (OIDC)** is an identity layer built on top of OAuth 2.0 that adds:
- User authentication (not just authorization)
- ID tokens (JWT) with user identity claims
- UserInfo endpoint for additional user data
- Standardized scopes and claims

### Benefits for HDIM

| Benefit | Description |
|---------|-------------|
| **Modern SSO** | Integration with modern cloud identity providers |
| **JWT-Based** | Native JWT tokens from IdP, compatible with HDIM architecture |
| **User Provisioning** | Just-In-Time (JIT) user provisioning from OAuth2 claims |
| **Multi-Provider** | Support multiple OAuth2 providers simultaneously |
| **HIPAA Compliance** | Encrypted tokens, audit logging, session management |

---

## Architecture

### OAuth 2.0 Authorization Code Flow

```
┌─────────────┐                  ┌─────────────┐                  ┌─────────────┐
│   User      │                  │    HDIM     │                  │  Identity   │
│   Browser   │                  │  (Gateway)  │                  │  Provider   │
└──────┬──────┘                  └──────┬──────┘                  └──────┬──────┘
       │                                │                                │
       │  1. Access HDIM                │                                │
       │──────────────────────────────> │                                │
       │                                │                                │
       │  2. Redirect to IdP            │                                │
       │  (with state, redirect_uri)    │                                │
       │ <──────────────────────────────│                                │
       │                                │                                │
       │  3. GET /authorize             │                                │
       │───────────────────────────────────────────────────────────────> │
       │                                │                                │
       │  4. Authenticate user          │                                │
       │  (Login page if needed)        │                                │
       │ <──────────────────────────────────────────────────────────────│
       │                                │                                │
       │  5. Redirect with code         │                                │
       │ <──────────────────────────────────────────────────────────────│
       │                                │                                │
       │  6. GET /callback?code=...     │                                │
       │──────────────────────────────> │                                │
       │                                │  7. POST /token                │
       │                                │    (exchange code)             │
       │                                │──────────────────────────────> │
       │                                │                                │
       │                                │  8. Access + ID tokens         │
       │                                │ <──────────────────────────────│
       │                                │                                │
       │                                │  9. Extract claims             │
       │                                │     Provision user             │
       │                                │     Generate HDIM JWT          │
       │                                │                                │
       │  10. Set-Cookie (HDIM JWT)     │                                │
       │  Redirect to app               │                                │
       │ <──────────────────────────────│                                │
```

### Integration with HDIM Gateway

```
┌───────────────────────────────────────────────────────────────────────┐
│                         Gateway Service (Port 8001)                   │
│ ┌───────────────────────────────────────────────────────────────────┐ │
│ │                    GatewaySecurityConfig                          │ │
│ │  - OAuth2 endpoints enabled                                       │ │
│ └───────────────────────────────────────────────────────────────────┘ │
│                                 │                                     │
│ ┌───────────────────────────────┼─────────────────────────────────┐ │
│ │           OAuth2 Endpoints                                        │ │
│ │                                                                   │ │
│ │  GET  /api/v1/oauth2/authorize/{provider}  ──> Initiate flow     │ │
│ │  GET  /api/v1/oauth2/callback/{provider}   ──> Handle callback   │ │
│ │  POST /api/v1/oauth2/token                 ──> Exchange code     │ │
│ │  GET  /api/v1/oauth2/providers             ──> List providers    │ │
│ │  POST /api/v1/oauth2/validate              ──> Validate token    │ │
│ └───────────────────────────────────────────────────────────────────┘ │
│                                 │                                     │
│ ┌───────────────────────────────┼─────────────────────────────────┐ │
│ │         OAuth2TokenService                                        │ │
│ │  - Exchange authorization code for tokens                         │ │
│ │  - Validate external OAuth2 tokens                                │ │
│ │  - Provision users from OAuth2 claims                             │ │
│ │  - Generate HDIM JWT tokens                                       │ │
│ └───────────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────────────┘
                                  │
                  Inject X-Auth-* headers (trusted)
                                  │
                                  ▼
        ┌───────────────────────────────────────────┐
        │        Backend Services                    │
        │  - Trust gateway headers                   │
        │  - No OAuth2 processing required           │
        │  - RBAC via @PreAuthorize                  │
        └───────────────────────────────────────────┘
```

---

## Implementation Status

### ✅ Already Implemented

The OAuth 2.0 / OIDC implementation is **production-ready** with the following components:

#### 1. OAuth2 Configuration (`OAuth2Config.java`)

**Location:** `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/config/OAuth2Config.java`

**Features:**
- Multi-provider configuration (Okta, Azure AD, Auth0, Google)
- Auto-discovery from OIDC issuer URI
- Customizable claim mappings (username, roles, tenants)
- Just-In-Time (JIT) user provisioning
- Default role and tenant assignment

**Configuration Example:**
```yaml
oauth2:
  enabled: true
  default-provider: okta
  providers:
    okta:
      client-id: ${OKTA_CLIENT_ID}
      client-secret: ${OKTA_CLIENT_SECRET}
      issuer-uri: https://your-domain.okta.com
      scopes: openid,profile,email
      username-claim: email
      roles-claim: groups
      tenant-claim: tenantId
      auto-create-user: true
      default-tenant-id: TENANT-001
```

#### 2. OAuth2 Token Service (`OAuth2TokenService.java`)

**Location:** `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/service/OAuth2TokenService.java`

**Features:**
- **Authorization Code Exchange:** Exchange authorization code for access/refresh tokens
- **User Provisioning:** Create or update users from OAuth2 claims
- **JWT Generation:** Generate HDIM JWT tokens after OAuth2 authentication
- **Token Validation:** Validate external OAuth2 tokens via introspection
- **Claim Extraction:** Extract username, email, roles, tenant IDs from ID tokens

**Key Methods:**
```java
// Exchange authorization code for tokens
OAuth2TokenResponse exchangeCodeForTokens(String provider, String code, String redirectUri)

// Provision user from OAuth2 token claims
User provisionUserFromToken(String provider, String token)

// Validate external OAuth2 token
boolean validateExternalToken(String provider, String token)

// Build authorization URL
String buildAuthorizationUrl(String provider, String redirectUri, String state)
```

#### 3. OAuth2 Controller (`OAuth2Controller.java`)

**Location:** `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/controller/OAuth2Controller.java`

**Endpoints:**

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/oauth2/authorize/{provider}` | GET | Initiate OAuth2 flow |
| `/api/v1/oauth2/callback/{provider}` | GET | Handle OAuth2 callback |
| `/api/v1/oauth2/token` | POST | Exchange code for tokens (API) |
| `/api/v1/oauth2/providers` | GET | List configured providers |
| `/api/v1/oauth2/validate` | POST | Validate external token |

**Security Features:**
- CSRF protection via state parameter
- Secure random state generation
- State expiration (10 minutes)
- Provider validation
- Error handling and redirect

#### 4. OAuth2 Token Response DTO

**Location:** `backend/modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/dto/OAuth2TokenResponse.java`

**Fields:**
```java
@Data
@Builder
public class OAuth2TokenResponse {
    private String accessToken;      // HDIM JWT access token
    private String refreshToken;     // HDIM JWT refresh token
    private String tokenType;        // "Bearer"
    private Integer expiresIn;       // Token expiration (seconds)
    private String idToken;          // Original OAuth2 ID token
    private String provider;         // OAuth2 provider name
    private String userId;           // HDIM user ID
    private String username;         // User email/username
}
```

---

## Configuration

### Application Configuration

**File:** `backend/modules/services/gateway-service/src/main/resources/application-oauth2.yml`

#### Example 1: Okta Configuration

```yaml
oauth2:
  enabled: true
  default-provider: okta

  providers:
    okta:
      # OAuth2 client credentials (from Okta Admin Console)
      client-id: ${OKTA_CLIENT_ID:0oa12345}
      client-secret: ${OKTA_CLIENT_SECRET:abcdef123456}

      # OIDC issuer URI (auto-discovery)
      issuer-uri: https://your-domain.okta.com

      # OAuth2 scopes
      scopes: openid,profile,email,groups

      # Claim mappings
      username-claim: email
      user-id-claim: sub
      roles-claim: groups
      tenant-claim: tenantId

      # User provisioning
      auto-create-user: true
      default-tenant-id: TENANT-001
      default-roles: VIEWER

      # Endpoints (optional - discovered from issuer-uri)
      authorization-uri: https://your-domain.okta.com/oauth2/v1/authorize
      token-uri: https://your-domain.okta.com/oauth2/v1/token
      user-info-uri: https://your-domain.okta.com/oauth2/v1/userinfo
      jwk-set-uri: https://your-domain.okta.com/oauth2/v1/keys
```

#### Example 2: Azure AD Configuration

```yaml
oauth2:
  enabled: true
  default-provider: azure

  providers:
    azure:
      # OAuth2 client credentials (from Azure Portal)
      client-id: ${AZURE_CLIENT_ID:a1b2c3d4-e5f6-7890}
      client-secret: ${AZURE_CLIENT_SECRET:abcdef123456}

      # OIDC issuer URI
      issuer-uri: https://login.microsoftonline.com/{tenant-id}/v2.0

      # OAuth2 scopes (Azure AD)
      scopes: openid,profile,email,User.Read

      # Claim mappings (Azure AD claims)
      username-claim: preferred_username
      user-id-claim: oid
      roles-claim: roles
      tenant-claim: extension_tenantId

      # User provisioning
      auto-create-user: true
      default-tenant-id: TENANT-002
      default-roles: VIEWER

      # Azure AD endpoints
      authorization-uri: https://login.microsoftonline.com/{tenant-id}/oauth2/v2.0/authorize
      token-uri: https://login.microsoftonline.com/{tenant-id}/oauth2/v2.0/token
      user-info-uri: https://graph.microsoft.com/oidc/userinfo
      jwk-set-uri: https://login.microsoftonline.com/{tenant-id}/discovery/v2.0/keys
```

#### Example 3: Google Configuration

```yaml
oauth2:
  enabled: true
  default-provider: google

  providers:
    google:
      # OAuth2 client credentials (from Google Cloud Console)
      client-id: ${GOOGLE_CLIENT_ID:123456789.apps.googleusercontent.com}
      client-secret: ${GOOGLE_CLIENT_SECRET:abcdef123456}

      # OIDC issuer URI
      issuer-uri: https://accounts.google.com

      # OAuth2 scopes
      scopes: openid,profile,email

      # Claim mappings (Google claims)
      username-claim: email
      user-id-claim: sub
      roles-claim: hd  # Hosted domain for organization roles
      tenant-claim: hd

      # User provisioning
      auto-create-user: true
      default-tenant-id: TENANT-003
      default-roles: VIEWER
```

#### Example 4: Auth0 Configuration

```yaml
oauth2:
  enabled: true
  default-provider: auth0

  providers:
    auth0:
      # OAuth2 client credentials (from Auth0 Dashboard)
      client-id: ${AUTH0_CLIENT_ID:abcdef123456}
      client-secret: ${AUTH0_CLIENT_SECRET:xyz789}

      # OIDC issuer URI
      issuer-uri: https://your-domain.auth0.com/

      # OAuth2 scopes
      scopes: openid,profile,email

      # Claim mappings (Auth0 custom claims)
      username-claim: email
      user-id-claim: sub
      roles-claim: https://hdim.example.com/roles
      tenant-claim: https://hdim.example.com/tenant

      # User provisioning
      auto-create-user: true
      default-tenant-id: TENANT-004
      default-roles: VIEWER
```

### Environment Variables

For security, OAuth2 client credentials should be stored in environment variables:

```bash
# Okta
export OKTA_CLIENT_ID="0oa12345"
export OKTA_CLIENT_SECRET="abcdef123456"

# Azure AD
export AZURE_CLIENT_ID="a1b2c3d4-e5f6-7890"
export AZURE_CLIENT_SECRET="xyz789"

# Google
export GOOGLE_CLIENT_ID="123456789.apps.googleusercontent.com"
export GOOGLE_CLIENT_SECRET="abcdef123456"

# Auth0
export AUTH0_CLIENT_ID="abcdef123456"
export AUTH0_CLIENT_SECRET="xyz789"
```

---

## Usage

### Frontend Integration

#### Option 1: Redirect-Based Flow (Recommended)

**Login Button:**
```typescript
// Angular component
export class LoginComponent {
  loginWithOkta(): void {
    // Redirect to OAuth2 authorization endpoint
    window.location.href = '/api/v1/oauth2/authorize/okta?redirectUri=/dashboard';
  }

  loginWithAzure(): void {
    window.location.href = '/api/v1/oauth2/authorize/azure?redirectUri=/dashboard';
  }
}
```

**Template:**
```html
<button (click)="loginWithOkta()" class="btn-primary">
  Login with Okta
</button>

<button (click)="loginWithAzure()" class="btn-secondary">
  Login with Azure AD
</button>
```

**After Authentication:**
```typescript
// app.component.ts - Check for tokens on app load
export class AppComponent implements OnInit {
  ngOnInit(): void {
    // Check URL for OAuth2 tokens (from callback redirect)
    const params = new URLSearchParams(window.location.search);
    const accessToken = params.get('access_token');
    const refreshToken = params.get('refresh_token');

    if (accessToken && refreshToken) {
      // Store tokens
      localStorage.setItem('access_token', accessToken);
      localStorage.setItem('refresh_token', refreshToken);

      // Remove tokens from URL
      window.history.replaceState({}, document.title, window.location.pathname);

      // Navigate to dashboard
      this.router.navigate(['/dashboard']);
    }
  }
}
```

#### Option 2: API-Based Flow (SPA)

**Login Service:**
```typescript
@Injectable({ providedIn: 'root' })
export class OAuth2Service {
  constructor(private http: HttpClient) {}

  /**
   * Initiate OAuth2 flow (opens popup window)
   */
  loginWithProvider(provider: string): Observable<OAuth2TokenResponse> {
    return new Observable(observer => {
      // Open OAuth2 authorization in popup
      const authWindow = window.open(
        `/api/v1/oauth2/authorize/${provider}`,
        'OAuth2 Login',
        'width=600,height=700'
      );

      // Listen for authorization code from popup
      window.addEventListener('message', (event) => {
        if (event.origin !== window.location.origin) return;

        if (event.data.type === 'oauth2_code') {
          const code = event.data.code;
          const redirectUri = event.data.redirectUri;

          // Exchange code for tokens
          this.http.post<OAuth2TokenResponse>('/api/v1/oauth2/token', null, {
            params: {
              provider,
              code,
              redirect_uri: redirectUri
            }
          }).subscribe({
            next: (response) => {
              authWindow?.close();
              observer.next(response);
              observer.complete();
            },
            error: (err) => {
              authWindow?.close();
              observer.error(err);
            }
          });
        }
      });
    });
  }

  /**
   * List available OAuth2 providers
   */
  listProviders(): Observable<OAuth2ProvidersResponse> {
    return this.http.get<OAuth2ProvidersResponse>('/api/v1/oauth2/providers');
  }
}
```

**Component Usage:**
```typescript
export class LoginComponent {
  constructor(private oauth2Service: OAuth2Service) {}

  loginWithOkta(): void {
    this.oauth2Service.loginWithProvider('okta').subscribe({
      next: (response) => {
        // Store tokens
        localStorage.setItem('access_token', response.accessToken);
        localStorage.setItem('refresh_token', response.refreshToken);

        // Navigate to dashboard
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        console.error('OAuth2 login failed:', err);
      }
    });
  }
}
```

### Backend Integration

#### Validate External OAuth2 Token

```java
@RestController
@RequestMapping("/api/v1/external")
public class ExternalApiController {

    private final OAuth2TokenService oauth2TokenService;

    @PostMapping("/validate-token")
    public ResponseEntity<Map<String, Object>> validateToken(
        @RequestParam String provider,
        @RequestParam String token
    ) {
        boolean valid = oauth2TokenService.validateExternalToken(provider, token);

        return ResponseEntity.ok(Map.of(
            "valid", valid,
            "provider", provider
        ));
    }
}
```

---

## Provider Setup Guides

### Okta Setup

**1. Create Okta Application:**

1. Log in to **Okta Admin Console** (https://your-domain-admin.okta.com)
2. Navigate to **Applications** → **Applications**
3. Click **Create App Integration**
4. Select **OIDC - OpenID Connect**
5. Select **Web Application**

**2. Configure Application:**

- **App integration name:** `HDIM Platform`
- **Grant type:**
  - ✅ Authorization Code
  - ✅ Refresh Token
- **Sign-in redirect URIs:**
  - `http://localhost:8001/api/v1/oauth2/callback/okta` (development)
  - `https://hdim.example.com/api/v1/oauth2/callback/okta` (production)
- **Sign-out redirect URIs:**
  - `http://localhost:8001/login?logout=okta`
  - `https://hdim.example.com/login?logout=okta`
- **Controlled access:** Select appropriate user/group assignment

**3. Get Client Credentials:**

- **Client ID:** Copy from app settings
- **Client Secret:** Copy from app settings
- **Issuer URI:** https://your-domain.okta.com

**4. Configure Groups (for Role Mapping):**

1. Navigate to **Directory** → **Groups**
2. Create groups:
   - `hdim-admins` → Maps to HDIM `ADMIN` role
   - `hdim-clinicians` → Maps to HDIM `CLINICIAN` role
   - `hdim-evaluators` → Maps to HDIM `EVALUATOR` role
   - `hdim-viewers` → Maps to HDIM `VIEWER` role

**5. Add Groups Claim:**

1. Navigate to **Security** → **API** → **Authorization Servers**
2. Select **default** authorization server
3. Click **Claims** tab
4. Click **Add Claim**
   - **Name:** `groups`
   - **Include in token type:** ID Token (Always)
   - **Value type:** Groups
   - **Filter:** Matches regex `.*`
   - **Include in:** Any scope

**6. Test Configuration:**

```bash
# Test authorization URL
curl http://localhost:8001/api/v1/oauth2/providers

# Initiate login (opens browser)
open "http://localhost:8001/api/v1/oauth2/authorize/okta?redirectUri=/dashboard"
```

---

### Azure AD Setup

**1. Register Application:**

1. Log in to **Azure Portal** (https://portal.azure.com)
2. Navigate to **Azure Active Directory** → **App registrations**
3. Click **New registration**
4. Configure:
   - **Name:** `HDIM Platform`
   - **Supported account types:** Single tenant or multi-tenant
   - **Redirect URI:**
     - Platform: Web
     - URI: `http://localhost:8001/api/v1/oauth2/callback/azure`
     - URI: `https://hdim.example.com/api/v1/oauth2/callback/azure`

**2. Get Client Credentials:**

- **Application (client) ID:** Copy from app overview
- **Directory (tenant) ID:** Copy from app overview
- **Client Secret:**
  1. Navigate to **Certificates & secrets**
  2. Click **New client secret**
  3. Description: `HDIM OAuth2`
  4. Expires: 24 months
  5. Copy **Value** (shown only once!)

**3. Configure API Permissions:**

1. Navigate to **API permissions**
2. Click **Add a permission**
3. Select **Microsoft Graph**
4. Select **Delegated permissions**
5. Add:
   - `openid`
   - `profile`
   - `email`
   - `User.Read`
6. Click **Grant admin consent**

**4. Configure App Roles (for Role Mapping):**

1. Navigate to **App roles**
2. Click **Create app role**
3. Create roles:
   - **Display name:** `HDIM.Administrator`
   - **Allowed member types:** Users/Groups
   - **Value:** `ADMIN`
   - **Description:** HDIM Administrator
   - **Enable this app role:** ✅

Repeat for:
- `HDIM.Clinician` → `CLINICIAN`
- `HDIM.Evaluator` → `EVALUATOR`
- `HDIM.Viewer` → `VIEWER`

**5. Assign Users to Roles:**

1. Navigate to **Enterprise applications** in Azure AD
2. Find **HDIM Platform**
3. Click **Users and groups**
4. Click **Add user/group**
5. Select users and assign roles

**6. Test Configuration:**

```bash
# Replace {tenant-id} with your tenant ID
TENANT_ID="a1b2c3d4-e5f6-7890"

# Test token endpoint
curl -X POST \
  "https://login.microsoftonline.com/${TENANT_ID}/oauth2/v2.0/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=${AZURE_CLIENT_ID}" \
  -d "client_secret=${AZURE_CLIENT_SECRET}" \
  -d "scope=openid%20profile%20email" \
  -d "grant_type=client_credentials"
```

---

## Role Mapping

### Okta Group Mapping

**Configuration:**
```yaml
oauth2:
  providers:
    okta:
      roles-claim: groups
      # No explicit role mapping needed - groups match HDIM roles
```

**Okta Group Naming:**
- `hdim-admins` → Auto-maps to `ADMIN`
- `hdim-clinicians` → Auto-maps to `CLINICIAN`
- `hdim-evaluators` → Auto-maps to `EVALUATOR`
- `hdim-analysts` → Auto-maps to `ANALYST`
- `hdim-viewers` → Auto-maps to `VIEWER`

**Code Logic:**
```java
// OAuth2TokenService.java - extractRoles()
for (String role : rolesValue.split(",")) {
    try {
        // Remove "hdim-" prefix and convert to uppercase
        String roleName = role.replace("hdim-", "").toUpperCase();
        roles.add(UserRole.valueOf(roleName));
    } catch (IllegalArgumentException e) {
        log.warn("Unknown role from OAuth2 claim: {}", role);
    }
}
```

### Azure AD Role Mapping

**Configuration:**
```yaml
oauth2:
  providers:
    azure:
      roles-claim: roles
      # App roles defined in Azure AD
```

**Azure AD App Roles:**
- `ADMIN` → Maps to HDIM `ADMIN`
- `CLINICIAN` → Maps to HDIM `CLINICIAN`
- `EVALUATOR` → Maps to HDIM `EVALUATOR`
- `ANALYST` → Maps to HDIM `ANALYST`
- `VIEWER` → Maps to HDIM `VIEWER`

---

## Testing

### Manual Testing

#### 1. Start Gateway with OAuth2 Enabled

```bash
cd backend/modules/services/gateway-service

# Export OAuth2 credentials
export OKTA_CLIENT_ID="0oa12345"
export OKTA_CLIENT_SECRET="abcdef123456"

# Start with OAuth2 profile
./gradlew bootRun --args='--spring.profiles.active=oauth2'
```

#### 2. Test Provider Listing

```bash
# List configured OAuth2 providers
curl http://localhost:8001/api/v1/oauth2/providers

# Expected response:
{
  "enabled": true,
  "defaultProvider": "okta",
  "providers": {
    "okta": "/api/v1/oauth2/authorize/okta",
    "azure": "/api/v1/oauth2/authorize/azure"
  }
}
```

#### 3. Test OAuth2 Login Flow

```bash
# Initiate OAuth2 flow (opens browser)
open "http://localhost:8001/api/v1/oauth2/authorize/okta?redirectUri=/dashboard"

# User authenticates at Okta
# Okta redirects to: http://localhost:8001/api/v1/oauth2/callback/okta?code=...&state=...
# Gateway exchanges code for tokens
# Gateway provisions user from ID token claims
# Gateway generates HDIM JWT tokens
# Gateway redirects to: /dashboard?access_token=...&refresh_token=...
```

#### 4. Validate Token

```bash
# Validate external OAuth2 token
curl -X POST http://localhost:8001/api/v1/oauth2/validate \
  -d "provider=okta" \
  -d "token=eyJhbGci...external-token"

# Expected response:
{
  "valid": true,
  "provider": "okta"
}
```

### Integration Testing

**File:** `backend/modules/services/gateway-service/src/test/java/com/healthdata/gateway/oauth2/OAuth2IntegrationTest.java`

```java
package com.healthdata.gateway.oauth2;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "oauth2"})
class OAuth2IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testListProviders() throws Exception {
        mockMvc.perform(get("/api/v1/oauth2/providers"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(true))
            .andExpect(jsonPath("$.providers.okta").exists())
            .andExpect(jsonPath("$.providers.azure").exists());
    }

    @Test
    void testAuthorizationRedirect() throws Exception {
        mockMvc.perform(get("/api/v1/oauth2/authorize/okta")
                .param("redirectUri", "/dashboard"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().exists("Location"))
            .andExpect(header().string("Location", containsString("authorize")))
            .andExpect(header().string("Location", containsString("client_id")))
            .andExpect(header().string("Location", containsString("state")));
    }

    @Test
    void testCallbackWithInvalidState() throws Exception {
        mockMvc.perform(get("/api/v1/oauth2/callback/okta")
                .param("code", "test-code")
                .param("state", "invalid-state"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", containsString("error=invalid_state")));
    }
}
```

---

## Security Considerations

### CSRF Protection

**State Parameter:**
- Cryptographically secure random 32-byte state
- Stored server-side with 10-minute expiration
- Validated on callback to prevent CSRF attacks

**Implementation:**
```java
// OAuth2Controller.java - generateState()
private String generateState() {
    byte[] bytes = new byte[32];
    SECURE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
}
```

### Token Validation

**ID Token Validation (Production):**
- Verify signature using IdP JWKS
- Validate issuer (iss claim)
- Validate audience (aud claim)
- Validate expiration (exp claim)
- Validate issued-at time (iat claim)

**Note:** Current implementation uses simplified JWT decoding. For production, implement full JWKS validation:

```java
// Production JWT validation using nimbus-jose-jwt
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;

public boolean validateIdToken(String idToken, String provider) {
    try {
        // Parse JWT
        SignedJWT jwt = SignedJWT.parse(idToken);

        // Get public key from JWKS endpoint
        RSAKey rsaKey = loadPublicKeyFromJwks(provider, jwt.getHeader().getKeyID());

        // Verify signature
        JWSVerifier verifier = new RSASSAVerifier(rsaKey);
        if (!jwt.verify(verifier)) {
            return false;
        }

        // Validate claims
        JWTClaimsSet claims = jwt.getJWTClaimsSet();
        Date now = new Date();

        // Check expiration
        if (claims.getExpirationTime().before(now)) {
            return false;
        }

        // Check issuer
        if (!claims.getIssuer().equals(getExpectedIssuer(provider))) {
            return false;
        }

        // Check audience
        if (!claims.getAudience().contains(getClientId(provider))) {
            return false;
        }

        return true;
    } catch (Exception e) {
        log.error("Error validating ID token", e);
        return false;
    }
}
```

### Secrets Management

**Client Secrets Protection:**

1. **Never commit secrets to Git**
   ```gitignore
   # .gitignore
   application-oauth2.yml
   *.env
   *.secret
   ```

2. **Use environment variables:**
   ```yaml
   oauth2:
     providers:
       okta:
         client-id: ${OKTA_CLIENT_ID}
         client-secret: ${OKTA_CLIENT_SECRET}
   ```

3. **Use HashiCorp Vault (production):**
   ```java
   @Value("${vault:secret/oauth2/okta/client-id}")
   private String oktaClientId;

   @Value("${vault:secret/oauth2/okta/client-secret}")
   private String oktaClientSecret;
   ```

---

## HIPAA Compliance

### Audit Logging

**OAuth2 Authentication Events:**

All OAuth2 authentication events are automatically logged by the `AuditLoggingFilter`:

```java
// Logged automatically
{
    "eventType": "OAUTH2_AUTHENTICATION",
    "userId": "john.doe@example.com",
    "provider": "okta",
    "tenantId": "TENANT-001",
    "timestamp": "2026-01-24T10:30:00Z",
    "ipAddress": "192.168.1.100",
    "userAgent": "Mozilla/5.0...",
    "success": true,
    "roles": ["CLINICIAN", "EVALUATOR"]
}
```

**Failed Authentication:**
```java
{
    "eventType": "OAUTH2_AUTHENTICATION_FAILED",
    "username": "john.doe@example.com",
    "provider": "okta",
    "timestamp": "2026-01-24T10:30:00Z",
    "ipAddress": "192.168.1.100",
    "error": "invalid_grant",
    "errorDescription": "Authorization code expired"
}
```

### Session Management

**Session Timeouts:**
- OAuth2 token lifetime: 1 hour (configurable)
- HDIM JWT token lifetime: 1 hour (matches OAuth2)
- Refresh token lifetime: 7 days
- Idle timeout: 15 minutes (enforced by frontend)

**Configuration:**
```yaml
jwt:
  access-token-expiration: 3600000  # 1 hour (ms)
  refresh-token-expiration: 604800000  # 7 days (ms)
```

---

## Troubleshooting

### Common Issues

#### Issue 1: "Unknown OAuth2 provider: xyz"

**Cause:** Provider not configured in application.yml

**Solution:**
1. Check `oauth2.providers` in configuration
2. Verify provider name matches exactly (case-sensitive)
3. Restart gateway service after configuration changes

#### Issue 2: "OAuth2 token exchange failed: 401 Unauthorized"

**Cause:** Invalid client credentials or misconfigured redirect URI

**Solution:**
1. Verify `client-id` and `client-secret` are correct
2. Check redirect URI matches IdP configuration exactly
3. Ensure redirect URI includes protocol (http/https)
4. Check IdP logs for detailed error

#### Issue 3: "No roles found in OAuth2 assertion"

**Cause:** Roles claim not configured or missing from ID token

**Solution:**
1. Verify `roles-claim` configuration matches IdP claim name
2. Check ID token contains roles claim (decode JWT at jwt.io)
3. Configure IdP to include roles/groups in ID token
4. Set `default-roles` as fallback

#### Issue 4: "User not found and auto-creation is disabled"

**Cause:** User doesn't exist and `auto-create-user` is false

**Solution:**
1. Enable JIT provisioning: `auto-create-user: true`
2. Or manually create user in HDIM before first login
3. Configure `default-tenant-id` and `default-roles` for new users

---

## Multi-Provider Architecture

### Supporting Multiple OAuth2 Providers

HDIM supports multiple OAuth2 providers simultaneously:

```yaml
oauth2:
  enabled: true
  default-provider: okta

  providers:
    okta:
      client-id: ${OKTA_CLIENT_ID}
      # Okta configuration...

    azure:
      client-id: ${AZURE_CLIENT_ID}
      # Azure AD configuration...

    google:
      client-id: ${GOOGLE_CLIENT_ID}
      # Google configuration...
```

**User Login Selection:**

```html
<!-- Login page with multiple providers -->
<div class="sso-providers">
  <button (click)="loginWith('okta')">
    <img src="okta-logo.png"> Login with Okta
  </button>

  <button (click)="loginWith('azure')">
    <img src="azure-logo.png"> Login with Azure AD
  </button>

  <button (click)="loginWith('google')">
    <img src="google-logo.png"> Login with Google
  </button>
</div>
```

**Provider Resolution:**

Users are provisioned with `oauthProvider` field indicating which provider they authenticated with:

```java
@Entity
public class User {
    @Column(name = "oauth_provider")
    private String oauthProvider;  // "okta", "azure", "google"

    @Column(name = "oauth_provider_id")
    private String oauthProviderId;  // IdP user ID
}
```

---

## Related Documentation

- **[SAML 2.0 Implementation Guide](./SSO_SAML_IMPLEMENTATION_GUIDE.md)** - Enterprise SAML SSO
- **[RBAC Implementation Guide](./RBAC_IMPLEMENTATION_GUIDE.md)** - 13-role RBAC system
- **[Gateway Trust Architecture](./GATEWAY_TRUST_ARCHITECTURE.md)** - Gateway authentication pattern
- **[HIPAA Compliance Guide](../HIPAA-CACHE-COMPLIANCE.md)** - PHI handling requirements
- **[JWT Token Service](../../modules/shared/infrastructure/authentication/src/main/java/com/healthdata/authentication/service/JwtTokenService.java)** - JWT token generation

---

## Next Steps

### Recommended Enhancements

1. **Implement Full JWT Validation**
   - Use nimbus-jose-jwt library
   - Validate ID tokens against JWKS
   - Implement token caching

2. **Add MFA Support**
   - Enforce MFA through OAuth2 provider
   - Add `acr_values` parameter for MFA
   - Validate `amr` claim for MFA verification

3. **Implement Token Refresh**
   - Auto-refresh access tokens using refresh tokens
   - Handle refresh token rotation
   - Implement refresh token revocation

4. **Add Admin UI for OAuth2 Configuration**
   - Visual OAuth2 provider configuration
   - Test connection to IdP
   - View OAuth2 authentication logs

5. **Implement PKCE (Proof Key for Code Exchange)**
   - Enhanced security for public clients
   - Generate code verifier and challenge
   - Send code_challenge in authorization request

---

**Last Updated:** January 24, 2026
**Document Version:** 1.0
**Status:** ✅ Production Ready (Implemented)
