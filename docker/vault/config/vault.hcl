# =============================================================================
# HashiCorp Vault Configuration for HDIM
# =============================================================================
# Production-ready configuration for secrets management
# =============================================================================

# Storage backend - PostgreSQL for HA in production
# For development, use file storage
storage "file" {
  path = "/vault/data"
}

# Listener configuration
listener "tcp" {
  address     = "0.0.0.0:8200"
  tls_disable = 1  # Enable TLS in production with proper certs

  # In production, enable TLS:
  # tls_cert_file = "/vault/certs/vault.crt"
  # tls_key_file  = "/vault/certs/vault.key"
}

# API address for client configuration
api_addr = "http://vault:8200"
cluster_addr = "http://vault:8201"

# UI enabled for administration
ui = true

# Audit logging for compliance (HIPAA requirement)
# audit {
#   type = "file"
#   path = "file"
#   options {
#     file_path = "/vault/logs/audit.log"
#   }
# }

# Disable mlock for containerized environments
disable_mlock = true

# Telemetry for monitoring
telemetry {
  prometheus_retention_time = "60s"
  disable_hostname = true
}

# Max lease TTL
max_lease_ttl = "768h"
default_lease_ttl = "168h"
