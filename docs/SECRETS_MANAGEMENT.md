# HDIM Secrets Management Guide

## Overview

This document describes the secrets management infrastructure for HealthData-in-Motion (HDIM), a HIPAA-compliant healthcare data platform. Proper secrets management is critical for:

- HIPAA Security Rule compliance (§164.312)
- Protection of PHI (Protected Health Information)
- Audit trail requirements
- Principle of least privilege

## Architecture

### Development Environment

For local development, secrets are managed via environment variables:

```bash
# Copy the template
cp .env.example .env.local

# Generate strong passwords
openssl rand -base64 32  # For each password field

# Edit .env.local with your values
```

### Production Environment

Production deployments use HashiCorp Vault for centralized secrets management.

```
┌─────────────────────────────────────────────────────────────┐
│                    HashiCorp Vault                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │  Database   │  │    Auth     │  │  External   │         │
│  │   Secrets   │  │   Secrets   │  │    APIs     │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
          │                 │                 │
          ▼                 ▼                 ▼
    ┌───────────┐    ┌───────────┐    ┌───────────┐
    │    CQL    │    │  Quality  │    │   FHIR    │
    │  Engine   │    │  Measure  │    │  Service  │
    └───────────┘    └───────────┘    └───────────┘
```

## Vault Setup

### 1. Start Vault

```bash
# Start Vault with the secrets compose file
docker-compose -f docker-compose.yml -f docker-compose.secrets.yml up -d vault
```

### 2. Initialize Vault

```bash
# Enter the Vault container
docker exec -it healthdata-vault sh

# Initialize Vault
vault operator init -key-shares=5 -key-threshold=3

# Store the unseal keys and root token SECURELY
# These are needed to unseal Vault after restarts
```

### 3. Unseal Vault

```bash
# Unseal with 3 of 5 keys
vault operator unseal <KEY_1>
vault operator unseal <KEY_2>
vault operator unseal <KEY_3>
```

### 4. Configure Secrets

```bash
# Login with root token
vault login <ROOT_TOKEN>

# Run the initialization script
./docker/vault/init-vault.sh
```

## Secret Paths

| Path | Description |
|------|-------------|
| `hdim/database/postgres` | PostgreSQL credentials |
| `hdim/cache/redis` | Redis password |
| `hdim/auth/jwt` | JWT signing secrets |
| `hdim/auth/service` | Service-to-service auth |
| `hdim/encryption/audit` | Audit log encryption key |
| `hdim/external/anthropic` | Claude AI API key |
| `hdim/monitoring/grafana` | Grafana admin credentials |

## Service Integration

### Spring Cloud Vault (Recommended)

Add to `build.gradle.kts`:

```kotlin
implementation("org.springframework.cloud:spring-cloud-starter-vault-config")
```

Add `bootstrap.yml`:

```yaml
spring:
  cloud:
    vault:
      enabled: ${VAULT_ENABLED:false}
      uri: ${VAULT_ADDR:http://localhost:8200}
      authentication: APPROLE
      app-role:
        role-id: ${VAULT_ROLE_ID}
        secret-id: ${VAULT_SECRET_ID}
      kv:
        enabled: true
        backend: hdim
        default-context: database/postgres
```

### Environment Variables Fallback

When Vault is not available, services fall back to environment variables:

```yaml
spring:
  datasource:
    password: ${SPRING_DATASOURCE_PASSWORD:}
```

## Security Policies

### Backend Services Policy

```hcl
path "hdim/data/database/*" {
  capabilities = ["read"]
}

path "hdim/data/auth/*" {
  capabilities = ["read"]
}
```

### CI/CD Policy

```hcl
path "hdim/data/*" {
  capabilities = ["read", "update"]
}
```

## Rotation Procedures

### Database Passwords

1. Generate new password in Vault
2. Update database user password
3. Restart affected services (rolling restart)

### JWT Secrets

1. Generate new secret in Vault
2. Update all services simultaneously
3. Existing tokens will be invalidated

## Audit Logging

Vault audit logging is enabled for HIPAA compliance:

```bash
# Enable file audit
vault audit enable file file_path=/vault/logs/audit.log
```

Audit logs capture:
- All secret access attempts
- Authentication events
- Policy changes
- Secret modifications

## Disaster Recovery

### Backup Keys

- Store unseal keys in separate secure locations
- Use Shamir's Secret Sharing (5 keys, 3 threshold)
- Never store all keys together

### Recovery Procedure

1. Restore Vault data from backup
2. Unseal Vault with 3 of 5 keys
3. Verify secret access
4. Restart dependent services

## Best Practices

1. **Never commit secrets** - Use `.env.example` as template only
2. **Rotate regularly** - Database passwords quarterly, JWT secrets monthly
3. **Audit access** - Review Vault audit logs weekly
4. **Least privilege** - Services only get secrets they need
5. **Encrypt at rest** - Vault data is encrypted with seal key
6. **Network isolation** - Vault only accessible from service network

## Troubleshooting

### Vault is Sealed

```bash
# Check status
vault status

# Unseal if needed (requires 3 keys)
vault operator unseal
```

### Service Can't Access Secrets

1. Check service has correct VAULT_ROLE_ID and VAULT_SECRET_ID
2. Verify AppRole is configured correctly
3. Check policy allows access to required paths

### Secret Not Found

```bash
# List secrets at path
vault kv list hdim/

# Get specific secret
vault kv get hdim/database/postgres
```
