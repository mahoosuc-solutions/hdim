# ADR-0004: HashiCorp Vault for Secrets Management

**Status**: Accepted
**Date**: 2025-12-06
**Decision Makers**: Architecture Team, Security Team
**Consulted**: DevOps, Platform Engineering
**Informed**: All Development Teams

## Context

**Problem Statement**:
After externalizing secrets to environment variables (ADR-0001), we needed a production-grade secrets management solution:

1. Environment variables alone don't provide secret rotation
2. No audit trail for secret access
3. No centralized management for 16+ microservices
4. Kubernetes secrets are base64-encoded, not encrypted at rest by default

**Business Context**:
- HIPAA requires audit controls for PHI access
- Need to rotate credentials without service restarts
- Multi-environment deployment (dev, staging, prod)
- SOC 2 compliance requires secrets management audit trail

**Technical Context**:
- Spring Boot 3.x microservices
- Docker Compose for local development
- Kubernetes for production
- PostgreSQL, Redis, external FHIR servers requiring credentials

## Decision

**We will implement HashiCorp Vault as the centralized secrets management solution with Spring Cloud Vault integration.**

**Specific Implementation**:

1. **Vault Architecture**:
   - Key-Value v2 secrets engine for application secrets
   - Database secrets engine for dynamic PostgreSQL credentials
   - PKI secrets engine for TLS certificate management
   - Transit engine for encryption as a service

2. **Secret Organization**:
   ```
   secret/hdim/
   ├── common/          # Shared across all services
   │   ├── jwt
   │   └── encryption
   ├── services/
   │   ├── gateway-service/
   │   ├── patient-service/
   │   ├── care-gap-service/
   │   └── ...
   └── integrations/
       ├── fhir-server/
       └── external-apis/
   ```

3. **Authentication Methods**:
   - Kubernetes auth for production (pod identity)
   - AppRole for CI/CD pipelines
   - Token auth for local development

## Alternatives Considered

### Alternative 1: AWS Secrets Manager
**Description**: AWS-managed secrets service
**Pros**:
- Fully managed, no operational overhead
- Native AWS integration
- Automatic rotation for RDS

**Cons**:
- AWS lock-in
- Cost per secret/API call
- Limited to AWS environments

**Why Not Chosen**: Need cloud-agnostic solution; may deploy to multiple clouds

### Alternative 2: Kubernetes Secrets with Sealed Secrets
**Description**: Encrypted Kubernetes secrets using Bitnami Sealed Secrets
**Pros**:
- GitOps-friendly (encrypted secrets in repo)
- No additional infrastructure
- Kubernetes-native

**Cons**:
- Kubernetes-only
- No dynamic secrets
- Limited audit capabilities

**Why Not Chosen**: Need dynamic credential rotation and comprehensive audit trail

### Alternative 3: CyberArk Conjur
**Description**: Enterprise secrets management
**Pros**:
- Enterprise features
- Strong compliance certifications
- Role-based access control

**Cons**:
- High licensing cost
- Complex setup
- Overkill for current scale

**Why Not Chosen**: Cost prohibitive; Vault provides similar features open-source

## Consequences

### Positive Consequences
- **Security**: Secrets never stored in code or environment variables
- **Audit Trail**: Every secret access logged and auditable
- **Dynamic Secrets**: Database credentials rotated automatically
- **Encryption**: Transit engine provides encryption as a service
- **Centralized**: Single source of truth for all secrets

### Negative Consequences
- **Infrastructure**: Additional service to operate and maintain
- **Complexity**: Learning curve for development teams
- **Dependency**: Services fail if Vault unavailable
- **Latency**: Network call required for each secret fetch

### Mitigation
- High availability Vault cluster (3-node minimum)
- Secret caching in Spring Cloud Vault
- Fallback to environment variables for local development
- Comprehensive runbooks and documentation

## Configuration

### Vault Server (docker/vault/config/vault.hcl)
```hcl
storage "file" {
  path = "/vault/data"
}

listener "tcp" {
  address     = "0.0.0.0:8200"
  tls_disable = 0
  tls_cert_file = "/vault/certs/vault.crt"
  tls_key_file  = "/vault/certs/vault.key"
}

api_addr = "https://vault:8200"
cluster_addr = "https://vault:8201"
ui = true
```

### Spring Cloud Vault Integration
```yaml
spring:
  cloud:
    vault:
      uri: ${VAULT_ADDR:http://localhost:8200}
      authentication: TOKEN
      token: ${VAULT_TOKEN:}
      kv:
        enabled: true
        backend: secret
        default-context: hdim/services/${spring.application.name}
```

### Secret Paths
| Secret Type | Path | Rotation |
|-------------|------|----------|
| JWT Secret | `secret/hdim/common/jwt` | Manual (quarterly) |
| Database Creds | `database/creds/hdim-readonly` | Dynamic (1 hour TTL) |
| API Keys | `secret/hdim/integrations/*` | Manual |
| TLS Certs | `pki/issue/hdim` | Automatic (30 days) |

## Implementation Plan

1. **Phase 1 (Completed)**: Docker Compose Vault setup for development
2. **Phase 2 (Completed)**: Spring Cloud Vault integration in services
3. **Phase 3**: Kubernetes Vault deployment with HA
4. **Phase 4**: Database secrets engine for dynamic credentials
5. **Phase 5**: PKI secrets engine for internal TLS

## Files Created

**Infrastructure**:
- `docker/vault/config/vault.hcl`
- `docker/vault/init-vault.sh`
- `docker-compose.secrets.yml`

**Documentation**:
- `docs/SECRETS_MANAGEMENT.md`

**Dependencies**:
- Added `spring-cloud-vault` to `gradle/libs.versions.toml`

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Secret rotation frequency | Monthly | Vault audit logs |
| Mean time to rotate | <5 minutes | Automation metrics |
| Hardcoded credentials | 0 | Static analysis |
| Vault availability | 99.9% | Monitoring |

## Related Decisions

- **Depends on**: [ADR-0001](0001-externalize-secrets-and-credentials.md) - Secrets externalization
- **Related to**: [ADR-0002](0002-implement-tenant-isolation-security.md) - Security architecture

## References

- [HashiCorp Vault Documentation](https://www.vaultproject.io/docs)
- [Spring Cloud Vault](https://spring.io/projects/spring-cloud-vault)
- [HIPAA Security Rule - Access Controls](https://www.hhs.gov/hipaa/for-professionals/security/index.html)
