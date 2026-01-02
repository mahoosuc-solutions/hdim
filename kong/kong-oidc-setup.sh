#!/bin/bash
set -e

#
# Kong OIDC Plugin Setup for HealthData In Motion HIE Platform
#
# This script configures OpenID Connect authentication via Kong
#
# Prerequisites:
#   1. OIDC provider configured (Okta, Auth0, Keycloak, Azure AD, etc.)
#   2. Client ID and Client Secret obtained
#   3. Discovery endpoint URL available
#
# Example OIDC Providers:
#   - Okta: https://{yourOktaDomain}/oauth2/default/.well-known/openid-configuration
#   - Auth0: https://{yourAuth0Domain}/.well-known/openid-configuration
#   - Keycloak: https://{yourKeycloakDomain}/realms/{realm}/.well-known/openid-configuration
#   - Azure AD: https://login.microsoftonline.com/{tenant}/v2.0/.well-known/openid-configuration
#

KONG_ADMIN_URL="${KONG_ADMIN_URL:-http://localhost:8001}"

# OIDC Configuration - Set these environment variables or update values below
OIDC_ISSUER="${OIDC_ISSUER:-}"
OIDC_CLIENT_ID="${OIDC_CLIENT_ID:-}"
OIDC_CLIENT_SECRET="${OIDC_CLIENT_SECRET:-}"
OIDC_DISCOVERY="${OIDC_DISCOVERY:-}"

echo "=========================================="
echo "Kong OIDC Plugin Setup"
echo "=========================================="
echo ""

# Validate environment variables
if [ -z "$OIDC_ISSUER" ] || [ -z "$OIDC_CLIENT_ID" ] || [ -z "$OIDC_CLIENT_SECRET" ] || [ -z "$OIDC_DISCOVERY" ]; then
    echo "ERROR: OIDC configuration not provided"
    echo ""
    echo "Please set the following environment variables:"
    echo "  OIDC_ISSUER         - OIDC provider issuer URL"
    echo "  OIDC_CLIENT_ID      - OAuth2 client ID"
    echo "  OIDC_CLIENT_SECRET  - OAuth2 client secret"
    echo "  OIDC_DISCOVERY      - OIDC discovery endpoint"
    echo ""
    echo "Example for Keycloak:"
    echo "  export OIDC_ISSUER=https://keycloak.example.com/realms/healthdata"
    echo "  export OIDC_CLIENT_ID=healthdata-api-gateway"
    echo "  export OIDC_CLIENT_SECRET=your-client-secret-here"
    echo "  export OIDC_DISCOVERY=https://keycloak.example.com/realms/healthdata/.well-known/openid-configuration"
    echo ""
    echo "Example for Okta:"
    echo "  export OIDC_ISSUER=https://dev-12345.okta.com/oauth2/default"
    echo "  export OIDC_CLIENT_ID=0oa1a2b3c4d5e6f7g8h9"
    echo "  export OIDC_CLIENT_SECRET=your-client-secret-here"
    echo "  export OIDC_DISCOVERY=https://dev-12345.okta.com/oauth2/default/.well-known/openid-configuration"
    echo ""
    exit 1
fi

# Check if Kong is running
echo "Checking Kong connectivity..."
if ! curl -sf "${KONG_ADMIN_URL}" > /dev/null; then
    echo "ERROR: Cannot connect to Kong Admin API at ${KONG_ADMIN_URL}"
    exit 1
fi
echo "✓ Kong is running"
echo ""

#
# Install OIDC Plugin (if using custom plugin)
#
echo "Note: Kong OIDC plugin requires Kong Enterprise or custom plugin installation"
echo "For Kong OSS, use lua-resty-openidc plugin or Kong Enterprise trial"
echo ""

#
# Option 1: Kong Enterprise OIDC Plugin (Recommended for Production)
#
echo "Configuring OpenID Connect authentication..."
echo ""
echo "Provider: $OIDC_ISSUER"
echo "Client ID: $OIDC_CLIENT_ID"
echo ""

# Apply OIDC to CQL Engine Service
echo "Applying OIDC to CQL Engine Service..."
curl -sf -X POST "${KONG_ADMIN_URL}/services/cql-engine-service/plugins" \
  -H "Content-Type: application/json" \
  -d @- << EOF
{
  "name": "openid-connect",
  "config": {
    "issuer": "${OIDC_ISSUER}",
    "client_id": ["${OIDC_CLIENT_ID}"],
    "client_secret": ["${OIDC_CLIENT_SECRET}"],
    "discovery": "${OIDC_DISCOVERY}",
    "auth_methods": ["bearer", "session"],
    "redirect_uri": "http://localhost:8000/api/cql/auth/callback",
    "logout_uri": "http://localhost:8000/api/cql/logout",
    "scopes": ["openid", "profile", "email"],
    "token_endpoint_auth_method": "client_secret_post",
    "ssl_verify": true,
    "session_secret": "$(openssl rand -base64 32)",
    "recovery_page_path": "/auth/error",
    "bearer_only": false,
    "bearer_jwt_auth_enable": true,
    "bearer_jwt_auth_allowed_auds": ["${OIDC_CLIENT_ID}"],
    "consumer_claim": "sub",
    "consumer_by": ["username", "custom_id"],
    "leeway": 60
  }
}
EOF

echo ""
echo "✓ OIDC configured for CQL Engine Service"
echo ""

# Apply OIDC to Quality Measure Service
echo "Applying OIDC to Quality Measure Service..."
curl -sf -X POST "${KONG_ADMIN_URL}/services/quality-measure-service/plugins" \
  -H "Content-Type: application/json" \
  -d @- << EOF
{
  "name": "openid-connect",
  "config": {
    "issuer": "${OIDC_ISSUER}",
    "client_id": ["${OIDC_CLIENT_ID}"],
    "client_secret": ["${OIDC_CLIENT_SECRET}"],
    "discovery": "${OIDC_DISCOVERY}",
    "auth_methods": ["bearer", "session"],
    "redirect_uri": "http://localhost:8000/api/quality/auth/callback",
    "logout_uri": "http://localhost:8000/api/quality/logout",
    "scopes": ["openid", "profile", "email"],
    "token_endpoint_auth_method": "client_secret_post",
    "ssl_verify": true,
    "session_secret": "$(openssl rand -base64 32)",
    "recovery_page_path": "/auth/error",
    "bearer_only": false,
    "bearer_jwt_auth_enable": true,
    "bearer_jwt_auth_allowed_auds": ["${OIDC_CLIENT_ID}"],
    "consumer_claim": "sub",
    "consumer_by": ["username", "custom_id"],
    "leeway": 60
  }
}
EOF

echo ""
echo "✓ OIDC configured for Quality Measure Service"
echo ""

#
# Option 2: JWT Plugin (Alternative for Bearer Token Validation Only)
#
echo "=========================================="
echo "Alternative: JWT Plugin Configuration"
echo "=========================================="
echo ""
echo "If you prefer simple JWT validation without full OIDC flow,"
echo "you can use the Kong JWT plugin instead."
echo ""
echo "To configure JWT plugin, run:"
echo "  ./kong-jwt-setup.sh"
echo ""

#
# Summary
#
echo "=========================================="
echo "OIDC Configuration Complete!"
echo "=========================================="
echo ""
echo "Authentication Flow:"
echo "  1. User accesses API endpoint via Kong"
echo "  2. Kong redirects to OIDC provider for authentication"
echo "  3. User authenticates with OIDC provider"
echo "  4. OIDC provider redirects back with authorization code"
echo "  5. Kong exchanges code for access token"
echo "  6. Kong validates token and proxies request to backend service"
echo ""
echo "Bearer Token Authentication:"
echo "  - Frontend obtains JWT token from OIDC provider"
echo "  - Includes token in Authorization header: 'Bearer <token>'"
echo "  - Kong validates token signature and claims"
echo "  - Kong proxies authenticated request to backend"
echo ""
echo "Testing:"
echo "  # Get token from OIDC provider"
echo "  TOKEN=\$(curl -X POST '${OIDC_ISSUER}/token' \\"
echo "    -d 'grant_type=client_credentials' \\"
echo "    -d 'client_id=${OIDC_CLIENT_ID}' \\"
echo "    -d 'client_secret=${OIDC_CLIENT_SECRET}' | jq -r '.access_token')"
echo ""
echo "  # Use token to access API"
echo "  curl -H \"Authorization: Bearer \$TOKEN\" \\"
echo "       -H \"X-Tenant-ID: default\" \\"
echo "       http://localhost:8000/api/cql/evaluations"
echo ""
