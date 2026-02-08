# LinkedIn OAuth Setup Guide

**Date:** February 5, 2026
**Version:** 1.0
**Purpose:** Configure LinkedIn OAuth 2.0 integration for HDIM Campaign Management System

---

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Phase 1: LinkedIn Developer App Setup](#phase-1-linkedin-developer-app-setup)
4. [Phase 2: Configure HDIM Backend](#phase-2-configure-hdim-backend)
5. [Phase 3: Test OAuth Flow](#phase-3-test-oauth-flow)
6. [Phase 4: Campaign System Integration](#phase-4-campaign-system-integration)
7. [Troubleshooting](#troubleshooting)
8. [Known Limitations](#known-limitations)
9. [Security Considerations](#security-considerations)

---

## Overview

This guide walks you through configuring LinkedIn OAuth 2.0 authentication for the HDIM platform. The integration enables:

- ✅ OAuth 2.0 authentication flow
- ✅ Access token management with automatic refresh
- ✅ LinkedIn profile retrieval
- ✅ Connection status tracking
- ⚠️ Direct messaging (requires LinkedIn API approval - not available by default)

**Architecture:**

```
Campaign System (Python)
    ↓
Real LinkedIn Adapter (linkedin_service.py)
    ↓
HDIM Backend (investor-dashboard-service:8120)
    ↓
LinkedIn OAuth 2.0 API
```

---

## Prerequisites

### Required

- ✅ LinkedIn account (personal or company)
- ✅ LinkedIn Company Page (required for app creation - must be admin)
- ✅ HDIM investor-dashboard-service running (port 8120)
- ✅ Business email for app verification

### Optional

- LinkedIn Marketing Developer Platform partnership (for messaging API)
- Production domain for OAuth callbacks

---

## Phase 1: LinkedIn Developer App Setup

### Step 1.1: Create LinkedIn Developer App

1. **Navigate to LinkedIn Developers:**
   - Go to: https://www.linkedin.com/developers/apps
   - Log in with your LinkedIn account

2. **Create New App:**
   - Click "Create app" button
   - Fill in required information:
     ```
     App name:         HDIM Campaign Manager
     LinkedIn Page:    [Select your company page]
     Privacy policy:   https://healthdata.com/privacy
     App logo:         [Upload HDIM logo]
     Legal agreement:  ✓ Accept terms
     ```

3. **Submit for Verification:**
   - Click "Create app"
   - LinkedIn may take 1-2 business days to verify your app
   - You'll receive an email when approved

### Step 1.2: Configure OAuth Settings

**After app approval:**

1. **Navigate to "Auth" Tab:**
   - Click on your app in the developer dashboard
   - Go to "Auth" section

2. **Add Redirect URLs:**
   ```
   Development:  http://localhost:8120/investor/api/linkedin/callback
   Production:   https://your-domain.com/investor/api/linkedin/callback
   ```

   **⚠️ Important:** Redirect URLs must match exactly (including http/https and trailing slashes)

3. **Request OAuth 2.0 Scopes:**

   | Scope | Purpose | Status |
   |-------|---------|--------|
   | `r_liteprofile` | Basic profile (name, photo) | ✅ Available by default |
   | `r_emailaddress` | Email address | ✅ Available by default |
   | `w_member_social` | Post updates | ⚠️ May require review |
   | `w_organization_social` | Company posts | ❌ Requires partnership |

   **Note:** `w_member_social` may require LinkedIn review. Apply for access if needed.

4. **Copy Credentials:**
   - **Client ID:** Copy this value (e.g., `78abc123def456`)
   - **Client Secret:** Copy this value (keep secure!)
   - **Primary OAuth 2.0 redirect URL:** Verify it matches your backend

### Step 1.3: Verify App Status

Before proceeding, ensure:

- ✅ App status shows "Verified"
- ✅ At least 2 OAuth scopes granted (`r_liteprofile`, `r_emailaddress`)
- ✅ Redirect URL registered correctly
- ✅ Credentials copied securely

---

## Phase 2: Configure HDIM Backend

### Step 2.1: Update .env.local

**File:** `/mnt/wdblack/dev/projects/hdim-master/.env.local`

Add LinkedIn OAuth credentials:

```bash
# ============================================
# LinkedIn OAuth Configuration
# ============================================
LINKEDIN_CLIENT_ID=78abc123def456
LINKEDIN_CLIENT_SECRET=YOUR_SECRET_HERE
LINKEDIN_REDIRECT_URI=http://localhost:8120/investor/api/linkedin/callback
LINKEDIN_API_ENABLED=true
```

**⚠️ Security:**
- Never commit `.env.local` to version control (already in `.gitignore`)
- Use environment-specific credentials (dev vs production)
- Store production secrets in secure vault (e.g., HashiCorp Vault)

### Step 2.2: Verify Configuration Files

**These files should already be configured (verify):**

**File:** `backend/modules/services/investor-dashboard-service/src/main/resources/application.yml`

```yaml
linkedin:
  oauth2:
    client-id: ${LINKEDIN_CLIENT_ID:}
    client-secret: ${LINKEDIN_CLIENT_SECRET:}
    redirect-uri: ${LINKEDIN_REDIRECT_URI:http://localhost:8120/investor/api/linkedin/callback}
    scope: r_liteprofile,r_emailaddress,w_member_social
  api:
    base-url: https://api.linkedin.com/v2
    oauth-url: https://www.linkedin.com/oauth/v2
    enabled: ${LINKEDIN_API_ENABLED:false}
```

**File:** `docker-compose.yml` (investor-dashboard-service section)

```yaml
environment:
  LINKEDIN_CLIENT_ID: ${LINKEDIN_CLIENT_ID:-}
  LINKEDIN_CLIENT_SECRET: ${LINKEDIN_CLIENT_SECRET:-}
  LINKEDIN_REDIRECT_URI: ${LINKEDIN_REDIRECT_URI:-http://localhost:8120/investor/api/linkedin/callback}
  LINKEDIN_API_ENABLED: ${LINKEDIN_API_ENABLED:-true}
```

### Step 2.3: Restart Backend Service

```bash
cd /mnt/wdblack/dev/projects/hdim-master

# Stop service
docker compose down investor-dashboard-service

# Rebuild with new environment
docker compose build investor-dashboard-service

# Start service
docker compose up -d investor-dashboard-service

# Verify startup (check for LinkedIn config in logs)
docker compose logs -f investor-dashboard-service | head -50
```

**Expected in logs:**
```
LinkedInService initialized
  client-id: 78****** (redacted)
  redirect-uri: http://localhost:8120/investor/api/linkedin/callback
  enabled: true
```

### Step 2.4: Health Check

```bash
# Test service is running
curl http://localhost:8120/investor/actuator/health

# Expected response:
# {"status":"UP"}
```

---

## Phase 3: Test OAuth Flow

### Step 3.1: Test Authorization URL Generation

```bash
cd ~/hdim-ops

# Create test script
cat > test_oauth_url.py << 'EOF'
import asyncio
from cx.integrations.linkedin_service import LinkedInService

async def test():
    service = LinkedInService()
    result = await service.get_auth_url(state="test-123")
    if result.success:
        print("Authorization URL:")
        print(result.data.get('authorizationUrl'))
    else:
        print(f"Error: {result.error}")
    await service.close()

asyncio.run(test())
EOF

# Run test
python test_oauth_url.py
```

**Expected output:**
```
Authorization URL:
https://www.linkedin.com/oauth/v2/authorization?response_type=code&client_id=78abc...&redirect_uri=http://localhost:8120/...
```

### Step 3.2: Manual OAuth Flow Test

1. **Copy the authorization URL from Step 3.1**

2. **Open URL in browser:**
   - You'll be redirected to LinkedIn login
   - Log in if not already authenticated
   - Review permissions requested by the app
   - Click "Allow" to authorize

3. **LinkedIn redirects to callback URL:**
   ```
   http://localhost:8120/investor/api/linkedin/callback?code=AQT...&state=test-123
   ```

4. **Backend automatically:**
   - Validates state parameter (CSRF protection)
   - Exchanges authorization code for access token
   - Fetches LinkedIn profile
   - Stores token in `linkedin_connections` table
   - Returns success response

### Step 3.3: Verify Connection Status

```bash
# Check connection status
curl -X GET "http://localhost:8120/investor/api/linkedin/status?userId=test-user" \
  -H "X-Auth-User-ID: cx-portal-system" \
  -H "X-Auth-Tenant-ID: default-tenant" \
  -H "X-Auth-Roles: ADMIN"
```

**Expected after successful OAuth:**
```json
{
  "connected": true,
  "linkedInMemberId": "abc123xyz",
  "profileUrl": "https://www.linkedin.com/in/abc123xyz",
  "expiresAt": "2026-04-06T12:00:00Z"
}
```

### Step 3.4: Verify Database Storage

```bash
# Check database
docker exec -it hdim-postgres psql -U healthdata -d investor_dashboard_db -c "
SELECT id, user_id, linked_in_member_id, connected, token_expires_at
FROM linkedin_connections
ORDER BY created_at DESC
LIMIT 5;
"
```

---

## Phase 4: Campaign System Integration

### Step 4.1: Verify Real Adapters Active

**File:** `~/hdim-ops/cx/workflows/sequence_engine.py` (lines 21-23)

Should show:
```python
# Real service adapters - switched from mock to real implementations (Feb 5, 2026)
from cx.integrations.email_service import EmailService
from cx.integrations.linkedin_service import LinkedInService
```

### Step 4.2: Test Real Adapter Connectivity

```bash
cd ~/hdim-ops

# Run comprehensive connectivity test
python test_linkedin_real.py
```

**Expected output:**
```
======================================================================
LinkedIn Real Adapter Connectivity Test
======================================================================

Test 1: Backend connectivity check
----------------------------------------------------------------------
✓ Backend is reachable
  Auth URL: https://www.linkedin.com/oauth/v2/authorization?...
  State: test-state-123

Test 2: Check connection status
----------------------------------------------------------------------
✓ Connection status retrieved
  Connected: true
  Status: ACTIVE

Test 3: Test send_message (expect 'not implemented' response)
----------------------------------------------------------------------
✓ Expected 'not implemented' response received
  Error message: LinkedIn messaging API not yet implemented - requires LinkedIn API approval
  → This is correct behavior (messaging requires LinkedIn API approval)

======================================================================
Test Summary
======================================================================
✓ Real adapter connectivity verified
```

### Step 4.3: Run Campaign Integration Test

```bash
cd ~/hdim-ops

# Run full integration test
python test_full_integration.py
```

**Expected behavior:**
- ✅ Email sends successfully (Gmail SMTP)
- ✅ LinkedIn OAuth operations work (get auth URL, check status)
- ⚠️ LinkedIn messaging returns "not implemented" (expected)
- ✅ Campaign continues despite LinkedIn messaging limitation

---

## Troubleshooting

### Issue: "Client ID is empty"

**Symptom:**
```
Error: LinkedIn client ID not configured
```

**Solution:**
1. Verify `.env.local` contains `LINKEDIN_CLIENT_ID=...`
2. Restart investor-dashboard-service: `docker compose restart investor-dashboard-service`
3. Check logs: `docker compose logs investor-dashboard-service | grep -i linkedin`

---

### Issue: "Invalid redirect URI"

**Symptom:**
```
Error: redirect_uri_mismatch
```

**Solution:**
1. Check LinkedIn app settings → Auth tab → Redirect URLs
2. Ensure exact match: `http://localhost:8120/investor/api/linkedin/callback`
3. No trailing slashes, correct protocol (http vs https)
4. Re-save in LinkedIn Developer Portal

---

### Issue: "Insufficient permissions"

**Symptom:**
```
Error: insufficient_permissions
```

**Solution:**
1. Go to LinkedIn app → Products tab
2. Request additional OAuth scopes if needed
3. May require LinkedIn review (1-2 weeks)
4. Alternative: Use manual workflow for messaging

---

### Issue: "Connection test fails"

**Symptom:**
```
Failed to connect to backend: Connection refused
```

**Solution:**
1. Verify service is running: `docker compose ps | grep investor`
2. Check port 8120 is accessible: `curl http://localhost:8120/investor/actuator/health`
3. Check firewall rules if running on remote server
4. Verify service logs: `docker compose logs investor-dashboard-service`

---

## Known Limitations

### LinkedIn Messaging API

**Issue:** LinkedIn Official API does not support direct messaging or connection requests without Marketing Developer Platform partnership.

**Workaround Options:**

1. **Manual Workflow (Recommended for MVP):**
   - OAuth connects successfully ✅
   - Campaign creates pending action in CX Portal ✅
   - User manually sends message in LinkedIn
   - User marks action as completed in CX Portal

2. **Third-Party Services:**
   - Phantombuster (automation platform)
   - Expandi.io (LinkedIn automation)
   - MeetAlfred (sales automation)
   - ⚠️ These may violate LinkedIn TOS - use with caution

3. **Apply for LinkedIn Marketing Developer Platform:**
   - Requires partnership application
   - May take several weeks to months
   - Provides access to messaging and connection APIs

### State Token Storage (Production Issue)

**Issue:** Current implementation stores CSRF state tokens in-memory (will be lost on service restart or with load balancing).

**Solution for Production:**

Migrate to Redis for state storage:

```java
// In LinkedInService.java
@Autowired
private RedisTemplate<String, String> redisTemplate;

private void storeState(String state) {
    redisTemplate.opsForValue().set(
        "linkedin:state:" + state,
        state,
        10,
        TimeUnit.MINUTES
    );
}
```

---

## Security Considerations

### Access Token Storage

**Current Implementation:**
- Access tokens stored in PostgreSQL `linkedin_connections` table
- TODO: Line 85 in `LinkedInConnection.java` mentions adding encryption

**Production Recommendation:**

Add token encryption:

```java
@Convert(converter = SensitiveDataConverter.class)
@Column(name = "access_token", nullable = false, length = 2000)
private String accessToken;
```

### OAuth State Parameter

- ✅ Random UUID generated for each auth request
- ✅ Validated on callback to prevent CSRF attacks
- ⚠️ Currently stored in-memory (production should use Redis)

### Rate Limiting

Configure LinkedIn API rate limits in `application.yml`:

```yaml
linkedin:
  outreach:
    daily-connection-limit: 50
    daily-inmail-limit: 25
    max-concurrent-requests: 5
    rate-limit-per-minute: 10
```

---

## Production Deployment

### Environment Configuration

Create `.env.production`:

```bash
# LinkedIn OAuth (Production)
LINKEDIN_CLIENT_ID=production-client-id
LINKEDIN_CLIENT_SECRET=production-client-secret
LINKEDIN_REDIRECT_URI=https://app.healthdata.com/investor/api/linkedin/callback
LINKEDIN_API_ENABLED=true

# Backend URLs (Production)
INVESTOR_DASHBOARD_URL=https://api.healthdata.com/investor
SALES_AUTOMATION_URL=https://api.healthdata.com/sales-automation
```

### Pre-Deployment Checklist

- [ ] Production LinkedIn app created and verified
- [ ] Production redirect URI registered in LinkedIn app
- [ ] Production credentials stored in secure vault
- [ ] Token encryption enabled (see Security Considerations)
- [ ] State storage migrated to Redis (see Known Limitations)
- [ ] Rate limiting configured
- [ ] SSL certificates configured for HTTPS
- [ ] Monitoring and alerting configured
- [ ] Backup strategy for `linkedin_connections` table

---

## Support & Resources

### HDIM Documentation

- **Main Guide:** [CLAUDE.md](../CLAUDE.md)
- **Service Catalog:** [docs/services/SERVICE_CATALOG.md](../docs/services/SERVICE_CATALOG.md)
- **API Documentation:** [Swagger UI](http://localhost:8120/investor/swagger-ui.html)

### LinkedIn Resources

- **Developer Portal:** https://www.linkedin.com/developers/apps
- **OAuth 2.0 Docs:** https://docs.microsoft.com/en-us/linkedin/shared/authentication/authentication
- **API Reference:** https://docs.microsoft.com/en-us/linkedin/shared/api-guide/concepts
- **Marketing Developer Platform:** https://www.linkedin.com/help/linkedin/answer/a545111

### Campaign System Files

- **Real LinkedIn Adapter:** `~/hdim-ops/cx/integrations/linkedin_service.py`
- **Sequence Engine:** `~/hdim-ops/cx/workflows/sequence_engine.py`
- **Test Scripts:** `~/hdim-ops/test_linkedin_real.py`

---

## Changelog

| Date | Version | Changes |
|------|---------|---------|
| 2026-02-05 | 1.0 | Initial release - LinkedIn OAuth configuration guide |

---

_Last Updated: February 5, 2026_
_Maintained by: HDIM Development Team_
