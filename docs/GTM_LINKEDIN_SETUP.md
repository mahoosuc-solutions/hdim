# LinkedIn OAuth Setup Guide

**Purpose:** Enable automated LinkedIn outreach tracking for HDIM GTM launch.

## Prerequisites

- LinkedIn personal account (or company page admin access)
- HDIM services running locally or in production

## Step 1: Create LinkedIn Developer Application

1. Go to [LinkedIn Developer Portal](https://www.linkedin.com/developers/apps)
2. Click "Create app"
3. Fill in application details:
   - **App name:** HDIM Sales Automation
   - **LinkedIn Page:** Select or create a company page
   - **App logo:** Upload HDIM logo
   - **Legal agreement:** Accept terms

4. After creation, note your:
   - **Client ID:** Found in Auth tab
   - **Client Secret:** Found in Auth tab (click "Show")

## Step 2: Configure OAuth Settings

In the LinkedIn app settings:

1. Go to **Auth** tab
2. Add **Authorized redirect URLs:**

   **For Sales Automation Service:**
   ```
   http://localhost:8106/sales-automation/api/linkedin/oauth/callback
   https://api.hdim.io/sales-automation/api/linkedin/oauth/callback
   ```

   **For Investor Dashboard Service:**
   ```
   http://localhost:4200/investor-launch/linkedin/callback
   https://admin.hdim.ai/investor-launch/linkedin/callback
   ```

3. Request **Products:**
   - **Share on LinkedIn** - Required for posting
   - **Sign In with LinkedIn using OpenID Connect** - Required for auth
   - **Marketing Developer Platform** (if available) - For campaign management

## Step 3: Set Environment Variables

### Local Development (.env file)

Create or update `.env` in the project root:

```bash
# LinkedIn OAuth
LINKEDIN_CLIENT_ID=your_client_id_here
LINKEDIN_CLIENT_SECRET=your_client_secret_here

# Override redirect URIs if needed
LINKEDIN_REDIRECT_URI=http://localhost:8106/sales-automation/api/linkedin/oauth/callback
```

### Docker Compose

Add to `docker-compose.yml` environment section:

```yaml
services:
  sales-automation-service:
    environment:
      - LINKEDIN_CLIENT_ID=${LINKEDIN_CLIENT_ID}
      - LINKEDIN_CLIENT_SECRET=${LINKEDIN_CLIENT_SECRET}
      - LINKEDIN_REDIRECT_URI=http://localhost:8106/sales-automation/api/linkedin/oauth/callback
```

### Production

Set environment variables in your deployment platform (AWS, GCP, etc.):

```bash
export LINKEDIN_CLIENT_ID="your_production_client_id"
export LINKEDIN_CLIENT_SECRET="your_production_client_secret"
export LINKEDIN_REDIRECT_URI="https://api.hdim.io/sales-automation/api/linkedin/oauth/callback"
```

## Step 4: Test OAuth Flow

### Sales Automation Service

1. Start the service:
   ```bash
   docker compose up -d sales-automation-service
   ```

2. Navigate to the LinkedIn OAuth endpoint:
   ```
   http://localhost:8106/sales-automation/api/linkedin/oauth/authorize
   ```

3. Authorize the application on LinkedIn

4. You should be redirected back with a success message

### Investor Dashboard Service

1. Start the service:
   ```bash
   docker compose up -d investor-dashboard-service
   ```

2. Log into admin-portal at `http://localhost:4200`

3. Navigate to Investor Launch page

4. Click "Connect LinkedIn" button

5. Authorize and verify the connection shows as active

## Configuration Reference

### Sales Automation Service (`application.yml`)

```yaml
linkedin:
  api:
    base-url: https://api.linkedin.com/v2
    oauth-url: https://www.linkedin.com/oauth/v2
  oauth:
    client-id: ${LINKEDIN_CLIENT_ID:}
    client-secret: ${LINKEDIN_CLIENT_SECRET:}
    redirect-uri: ${LINKEDIN_REDIRECT_URI:http://localhost:8106/sales-automation/api/linkedin/oauth/callback}
  outreach:
    daily-connection-limit: ${LINKEDIN_DAILY_CONNECTION_LIMIT:50}
    daily-inmail-limit: ${LINKEDIN_DAILY_INMAIL_LIMIT:25}
    min-delay-between-actions-ms: ${LINKEDIN_MIN_DELAY_MS:30000}
    max-delay-between-actions-ms: ${LINKEDIN_MAX_DELAY_MS:120000}
```

### Investor Dashboard Service (`application.yml`)

```yaml
linkedin:
  oauth2:
    client-id: ${LINKEDIN_CLIENT_ID:}
    client-secret: ${LINKEDIN_CLIENT_SECRET:}
    redirect-uri: ${LINKEDIN_REDIRECT_URI:http://localhost:4200/investor-launch/linkedin/callback}
    scope: r_liteprofile,r_emailaddress,w_member_social
  api:
    base-url: https://api.linkedin.com/v2
```

## Rate Limits

LinkedIn API has strict rate limits. HDIM enforces conservative defaults:

| Action | Daily Limit | Min Delay |
|--------|-------------|-----------|
| Connection requests | 50/day | 30 seconds |
| InMail messages | 25/day | 30 seconds |
| Profile views | 100/day | 5 seconds |

**Important:** Exceeding limits can result in account restrictions.

## Troubleshooting

### "Invalid redirect_uri" Error

- Verify the redirect URI in LinkedIn app matches exactly (including trailing slashes)
- Check that the environment variable is set correctly

### "Access Denied" Error

- Ensure required products are approved in LinkedIn Developer Portal
- Check that OAuth scopes match what's configured

### Token Expiration

- LinkedIn access tokens expire after 60 days
- Refresh tokens last 365 days
- The application handles refresh automatically

## Security Notes

- **Never commit** `LINKEDIN_CLIENT_SECRET` to version control
- Use environment variables or secrets management in production
- Rotate credentials periodically
- Monitor for unusual API usage patterns

---

**Last Updated:** February 2026
**Related Docs:** [Sales Automation Service](../backend/modules/services/sales-automation-service/README.md)
