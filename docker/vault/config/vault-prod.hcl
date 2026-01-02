# =============================================================================
# HashiCorp Vault Configuration for HDIM - PRODUCTION
# =============================================================================
# TLS-enabled configuration for production deployments
# HIPAA-compliant secrets management
# =============================================================================

# Storage backend - PostgreSQL for HA in production
storage "postgresql" {
  connection_url = "postgres://vault:vault_password@postgres:5432/vault_db?sslmode=require"
  table          = "vault_kv_store"
  ha_enabled     = true
}

# Listener configuration with TLS
listener "tcp" {
  address     = "0.0.0.0:8200"
  
  # TLS Configuration - REQUIRED for production
  tls_disable     = 0
  tls_cert_file   = "/vault/ssl/tls.crt"
  tls_key_file    = "/vault/ssl/tls.key"
  tls_min_version = "tls12"
  
  # Client certificate authentication (optional, for mTLS)
  # tls_client_ca_file = "/vault/ssl/ca.crt"
  # tls_require_and_verify_client_cert = true
}

# API address for client configuration
api_addr     = "https://vault:8200"
cluster_addr = "https://vault:8201"

# UI enabled for administration (consider disabling in high-security environments)
ui = true

# Audit logging for HIPAA compliance - REQUIRED
audit {
  type = "file"
  path = "file"
  options {
    file_path = "/vault/logs/audit.log"
  }
}

# Disable mlock for containerized environments
disable_mlock = true

# Telemetry for monitoring
telemetry {
  prometheus_retention_time = "60s"
  disable_hostname = true
}

# Token TTL settings
max_lease_ttl     = "768h"   # 32 days max
default_lease_ttl = "168h"   # 7 days default

# =============================================================================
# Production Checklist:
# =============================================================================
# 1. Generate TLS certificates and place in /vault/ssl/
# 2. Update PostgreSQL connection string with production credentials
# 3. Initialize Vault and store unseal keys securely
# 4. Enable audit logging
# 5. Configure auto-unseal (AWS KMS, GCP KMS, or Azure Key Vault)
# 6. Set up backup schedule for PostgreSQL storage backend
# =============================================================================
