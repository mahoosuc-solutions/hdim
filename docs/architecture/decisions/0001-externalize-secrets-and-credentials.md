# ADR-0001: Externalize Secrets and Credentials

**Status**: Accepted
**Date**: 2025-12-06
**Decision Makers**: Architecture Team
**Consulted**: Security Team, DevOps
**Informed**: All Development Teams

## Context

**Problem Statement**:
The HDIM healthcare platform had hardcoded credentials throughout the codebase, creating critical security vulnerabilities:

1. Frontend `auth.interceptor.ts` contained hardcoded Basic Auth credentials
2. Backend `application.yml` files had default passwords like `dev_password`, `healthdata_password`
3. JWT secrets had insecure fallback values like `changeme_in_production`
4. `.env` files with real credentials were at risk of being committed

**Business Context**:
- HIPAA Security Rule (§164.312) requires access controls and audit capabilities
- Hardcoded credentials represent a critical security vulnerability
- Any credential exposure could lead to PHI breach and regulatory penalties

**Technical Context**:
- 16+ microservices using Spring Boot
- Angular frontends with HTTP interceptors
- Docker Compose for local development
- No centralized secrets management

## Decision

**We will externalize all secrets and credentials to environment variables with no insecure defaults.**

**Specific Implementation**:

1. **Frontend Changes**:
   - Remove hardcoded Basic Auth from `auth.interceptor.ts`
   - Use JWT Bearer tokens from AuthService
   - Never store credentials in frontend code

2. **Backend Changes**:
   - All passwords use `${ENV_VAR:}` pattern (empty default)
   - JWT secrets require explicit configuration
   - Service-to-service auth via environment variables

3. **Configuration Files**:
   - `.env.example` as template with `CHANGE_ME` placeholders
   - `.gitignore` excludes all `.env*` files except `.env.example`
   - Security warnings in YAML comments

## Alternatives Considered

### Alternative 1: Continue with Development Defaults
**Description**: Keep `dev_password` defaults for developer convenience
**Pros**:
- Easy local development setup
- No configuration required

**Cons**:
- Critical security vulnerability
- Defaults often leak to production
- Violates HIPAA requirements

**Why Not Chosen**: Unacceptable security risk for healthcare platform

### Alternative 2: Encrypted Properties Files
**Description**: Use Jasypt or similar to encrypt properties
**Pros**:
- Secrets encrypted at rest
- No external dependencies

**Cons**:
- Encryption key still needs secure storage
- Doesn't solve the root problem
- Complex key rotation

**Why Not Chosen**: Shifts problem to encryption key management

### Alternative 3: Spring Cloud Config Server
**Description**: Centralized configuration server
**Pros**:
- Centralized management
- Version control for configs
- Profile-based configuration

**Cons**:
- Additional infrastructure
- Single point of failure
- Still needs secrets backend

**Why Not Chosen**: Environment variables simpler for initial fix; Vault added in ADR-0004

## Consequences

### Positive Consequences
- **Security**: No credentials in source code or version control
- **Compliance**: Meets HIPAA access control requirements
- **Flexibility**: Easy to change credentials per environment
- **Audit**: Clear separation of code and configuration

### Negative Consequences
- **Setup Complexity**: Developers must configure local environment
- **Documentation**: Need clear setup instructions
- **Risk of Misconfiguration**: Empty defaults may cause startup failures

### Mitigation
- Created `.env.example` with all required variables
- Added `openssl rand` commands in comments for generating secrets
- Application startup fails fast if required secrets missing

## Compliance & Security

- **HIPAA §164.312(d)**: Person or entity authentication
- **HIPAA §164.312(a)(1)**: Access control
- **OWASP A07:2021**: Identification and Authentication Failures

## Implementation Plan

1. **Phase 1 (Completed)**: Frontend credential removal
2. **Phase 2 (Completed)**: Backend YAML externalization (16+ services)
3. **Phase 3 (Completed)**: `.gitignore` and `.env.example` updates
4. **Phase 4**: Team communication and documentation

## Files Modified

- `apps/clinical-portal/src/app/interceptors/auth.interceptor.ts`
- `backend/modules/services/*/src/main/resources/application.yml` (16+ files)
- `.gitignore`
- `.env.example`

## Related Decisions

- **Related to**: [ADR-0004](0004-hashicorp-vault-secrets-management.md) - Production secrets management

## References

- [OWASP Secrets Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)
- [12-Factor App - Config](https://12factor.net/config)
