#!/bin/bash
# =============================================================================
# Vault Initialization Script for HDIM
# =============================================================================
# This script initializes Vault with the necessary secrets engines and policies
# for the HealthData-in-Motion platform.
# =============================================================================

set -e

# Wait for Vault to be ready
echo "Waiting for Vault to be ready..."
until vault status 2>/dev/null; do
    sleep 1
done

# Check if Vault is already initialized
if vault status | grep -q "Initialized.*true"; then
    echo "Vault is already initialized"
    exit 0
fi

echo "Initializing Vault..."

# Initialize Vault with 5 key shares and 3 threshold
# In production, distribute these keys securely
vault operator init -key-shares=5 -key-threshold=3 -format=json > /vault/init-keys.json

# Extract unseal keys and root token
UNSEAL_KEY_1=$(cat /vault/init-keys.json | jq -r '.unseal_keys_b64[0]')
UNSEAL_KEY_2=$(cat /vault/init-keys.json | jq -r '.unseal_keys_b64[1]')
UNSEAL_KEY_3=$(cat /vault/init-keys.json | jq -r '.unseal_keys_b64[2]')
ROOT_TOKEN=$(cat /vault/init-keys.json | jq -r '.root_token')

echo "Unsealing Vault..."
vault operator unseal $UNSEAL_KEY_1
vault operator unseal $UNSEAL_KEY_2
vault operator unseal $UNSEAL_KEY_3

# Login with root token
export VAULT_TOKEN=$ROOT_TOKEN

echo "Enabling secrets engines..."

# Enable KV secrets engine v2 for application secrets
vault secrets enable -path=hdim kv-v2

# Enable database secrets engine for dynamic credentials (optional)
# vault secrets enable database

echo "Creating application secrets..."

# Database secrets
vault kv put hdim/database/postgres \
    admin_username="healthdata_admin" \
    admin_password="CHANGE_ME_GENERATE_STRONG_PASSWORD" \
    cql_username="healthdata_cql" \
    cql_password="CHANGE_ME_GENERATE_STRONG_PASSWORD" \
    quality_username="healthdata_quality" \
    quality_password="CHANGE_ME_GENERATE_STRONG_PASSWORD" \
    fhir_username="healthdata_fhir" \
    fhir_password="CHANGE_ME_GENERATE_STRONG_PASSWORD" \
    patient_username="healthdata_patient" \
    patient_password="CHANGE_ME_GENERATE_STRONG_PASSWORD"

# Redis secrets
vault kv put hdim/cache/redis \
    password="CHANGE_ME_GENERATE_STRONG_PASSWORD"

# JWT secrets
vault kv put hdim/auth/jwt \
    secret="CHANGE_ME_GENERATE_256_BIT_SECRET" \
    access_token_expiration=900000 \
    refresh_token_expiration=604800000

# Service-to-service auth
vault kv put hdim/auth/service \
    username="service_account" \
    password="CHANGE_ME_GENERATE_STRONG_PASSWORD"

# Audit encryption key
vault kv put hdim/encryption/audit \
    key="CHANGE_ME_GENERATE_256_BIT_KEY"

# External API keys (Claude AI, etc.)
vault kv put hdim/external/anthropic \
    api_key=""

# Grafana admin
vault kv put hdim/monitoring/grafana \
    admin_user="admin" \
    admin_password="CHANGE_ME_ADMIN_PASSWORD"

echo "Creating policies..."

# Create policy for backend services
cat <<EOF | vault policy write hdim-backend -
# HDIM Backend Services Policy
# Read-only access to secrets

path "hdim/data/database/*" {
  capabilities = ["read"]
}

path "hdim/data/cache/*" {
  capabilities = ["read"]
}

path "hdim/data/auth/*" {
  capabilities = ["read"]
}

path "hdim/data/encryption/*" {
  capabilities = ["read"]
}

path "hdim/data/external/*" {
  capabilities = ["read"]
}
EOF

# Create policy for CI/CD
cat <<EOF | vault policy write hdim-cicd -
# HDIM CI/CD Policy
# Limited write access for deployments

path "hdim/data/database/*" {
  capabilities = ["read", "update"]
}

path "hdim/data/auth/*" {
  capabilities = ["read", "update"]
}
EOF

# Create policy for monitoring
cat <<EOF | vault policy write hdim-monitoring -
# HDIM Monitoring Policy

path "hdim/data/monitoring/*" {
  capabilities = ["read"]
}
EOF

echo "Enabling authentication methods..."

# Enable AppRole for service authentication (Docker Compose / local dev)
vault auth enable approle

# Create role for backend services
vault write auth/approle/role/hdim-backend \
    token_policies="hdim-backend" \
    token_ttl=1h \
    token_max_ttl=4h \
    secret_id_ttl=24h \
    secret_id_num_uses=0

# Get role ID and secret ID for configuration
ROLE_ID=$(vault read -field=role_id auth/approle/role/hdim-backend/role-id)
vault write -f -field=secret_id auth/approle/role/hdim-backend/secret-id > /vault/backend-secret-id

# Enable Kubernetes auth for pod-level secret access (Kubernetes deployments)
# Services authenticate using their K8s ServiceAccount JWT — no static credentials required
if [ "${ENABLE_K8S_AUTH:-false}" = "true" ]; then
    echo "Enabling Kubernetes authentication method..."
    vault auth enable kubernetes

    # Configure Kubernetes auth with cluster API and CA cert
    # K8s_HOST and K8S_CA_CERT are injected by the Vault agent or set as env vars
    vault write auth/kubernetes/config \
        kubernetes_host="${KUBERNETES_SERVICE_HOST:-https://kubernetes.default.svc}" \
        kubernetes_ca_cert="${KUBERNETES_CA_CERT:-@/var/run/secrets/kubernetes.io/serviceaccount/ca.crt}" \
        issuer="${K8S_TOKEN_ISSUER:-https://kubernetes.default.svc.cluster.local}"

    # Create Kubernetes role for HDIM backend services
    # Bound to the hdim-services ServiceAccount in the hdim namespace
    vault write auth/kubernetes/role/hdim-backend \
        bound_service_account_names="hdim-services" \
        bound_service_account_namespaces="hdim,hdim-production,hdim-staging" \
        token_policies="hdim-backend" \
        token_ttl=1h \
        token_max_ttl=4h

    # Create Kubernetes role for CI/CD jobs
    vault write auth/kubernetes/role/hdim-cicd \
        bound_service_account_names="hdim-cicd" \
        bound_service_account_namespaces="hdim,hdim-production,hdim-staging" \
        token_policies="hdim-cicd" \
        token_ttl=30m \
        token_max_ttl=1h

    echo "Kubernetes auth method configured."
fi

echo "==================================="
echo "Vault initialization complete!"
echo "==================================="
echo ""
echo "IMPORTANT: Store the following securely:"
echo "Root Token: $ROOT_TOKEN"
echo "Unseal Keys: See /vault/init-keys.json"
echo ""
echo "Backend AppRole Role ID: $ROLE_ID"
echo "Backend Secret ID: $(cat /vault/backend-secret-id)"
echo ""
echo "WARNING: Delete /vault/init-keys.json after storing keys securely!"
echo "==================================="
