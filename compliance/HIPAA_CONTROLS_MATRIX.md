# HIPAA Security Rule Controls Matrix

## Document Purpose

This document maps HIPAA Security Rule requirements (45 CFR Part 164 Subpart C) to HDIM's implemented technical controls. It serves as supporting documentation for Business Associate Agreements (BAAs) and demonstrates compliance with the HIPAA Security Rule.

**Document Version:** 1.0
**Last Updated:** December 2024
**Review Schedule:** Quarterly

---

## Executive Summary

HDIM implements comprehensive technical safeguards to protect electronic Protected Health Information (ePHI) in compliance with the HIPAA Security Rule. Key security capabilities include:

- **JWT-based Authentication** - All API endpoints require valid authentication tokens
- **Multi-Tenant Isolation** - Row-level security ensures tenant data separation
- **Field-Level Encryption** - AES-256-GCM encryption for sensitive PHI fields
- **WebSocket Security** - Authenticated real-time connections with audit logging
- **Comprehensive Audit Logging** - All PHI access events logged for 6+ years
- **Transport Security** - TLS 1.2+ for all communications

---

## Administrative Safeguards (§ 164.308)

### § 164.308(a)(1) - Security Management Process

| Requirement | HDIM Implementation | Evidence |
|-------------|---------------------|----------|
| Risk Analysis | Annual security assessments documented | `/compliance/risk-assessments/` |
| Risk Management | Remediation tracking via security backlog | GitHub Security Issues |
| Sanction Policy | Employee security policy enforcement | HR Policy Documentation |
| Information System Activity Review | Automated audit log analysis | Prometheus/Grafana dashboards |

### § 164.308(a)(3) - Workforce Security

| Requirement | HDIM Implementation | Evidence |
|-------------|---------------------|----------|
| Authorization/Supervision | Role-based access control (RBAC) | `TenantAccessFilter.java` |
| Workforce Clearance | Background check requirements | HR Onboarding Process |
| Termination Procedures | Account deactivation on termination | User Management API |

### § 164.308(a)(4) - Information Access Management

| Requirement | HDIM Implementation | Evidence |
|-------------|---------------------|----------|
| Access Authorization | JWT tokens with role claims | `JwtTokenService.java` |
| Access Establishment | Tenant-scoped user provisioning | Admin Portal |
| Access Modification | Real-time permission updates | User Management API |

### § 164.308(a)(5) - Security Awareness and Training

| Requirement | HDIM Implementation | Evidence |
|-------------|---------------------|----------|
| Security Reminders | Quarterly security updates | Email Announcements |
| Protection from Malware | Secure code review process | GitHub PR Reviews |
| Log-in Monitoring | Failed login attempt tracking | `AuditService.java` |
| Password Management | Strong password policy enforcement | `PasswordValidator.java` |

### § 164.308(a)(6) - Security Incident Procedures

| Requirement | HDIM Implementation | Evidence |
|-------------|---------------------|----------|
| Response and Reporting | Incident response plan | `/compliance/INCIDENT_RESPONSE_PLAN.md` |

### § 164.308(a)(7) - Contingency Plan

| Requirement | HDIM Implementation | Evidence |
|-------------|---------------------|----------|
| Data Backup Plan | Automated encrypted backups | `/docker/postgres/backup/` |
| Disaster Recovery | Multi-region deployment support | Kubernetes manifests |
| Emergency Mode Operation | Graceful degradation patterns | Circuit breaker configs |
| Testing and Revision | Quarterly DR testing | DR Test Reports |
| Applications Criticality Analysis | Service dependency mapping | Architecture docs |

---

## Physical Safeguards (§ 164.310)

### § 164.310(a)(1) - Facility Access Controls

| Requirement | HDIM Implementation | Evidence |
|-------------|---------------------|----------|
| Contingency Operations | Cloud provider facility controls | AWS/GCP SOC 2 Reports |
| Facility Security Plan | Cloud provider physical security | AWS/GCP Compliance |
| Access Control | Network segmentation | Docker network configs |
| Maintenance Records | Cloud provider maintenance logs | AWS/GCP Compliance |

### § 164.310(d)(1) - Device and Media Controls

| Requirement | HDIM Implementation | Evidence |
|-------------|---------------------|----------|
| Disposal | Secure data destruction procedures | Data Retention Policy |
| Media Re-use | Volume encryption | Database encryption |
| Accountability | Asset inventory management | Infrastructure-as-Code |
| Data Backup/Storage | Encrypted backup storage | S3/GCS with encryption |

---

## Technical Safeguards (§ 164.312)

### § 164.312(a)(1) - Access Control

| Requirement | HDIM Implementation | Evidence |
|-------------|---------------------|----------|
| Unique User Identification | UUID-based user identifiers | `User.java` entity |
| Emergency Access Procedure | Break-glass admin access | Admin override capability |
| Automatic Logoff | 15-minute session timeout | `SessionTimeoutManager.java` |
| Encryption and Decryption | AES-256-GCM field encryption | `FieldEncryptionService.java` |

**Implementation Details:**

```
JWT Authentication Flow:
1. User authenticates via Gateway Service
2. Gateway issues signed JWT with user/tenant claims
3. All downstream services validate JWT via JwtAuthenticationFilter
4. TenantAccessFilter enforces tenant isolation on every request
5. Session timeout tracked via SessionTimeoutManager (WebSocket)
```

**Code References:**
- `JwtAuthenticationFilter.java` - JWT validation on all requests
- `TenantAccessFilter.java` - Multi-tenant access enforcement
- `QualityMeasureSecurityConfig.java` - Security filter chain configuration
- `PatientSecurityConfig.java` - Patient service security
- `FhirSecurityConfig.java` - FHIR service security

### § 164.312(a)(2)(iii) - Automatic Logoff

| Component | Timeout | Implementation |
|-----------|---------|----------------|
| WebSocket Sessions | 15 minutes inactivity | `SessionTimeoutManager.java` |
| JWT Tokens | 24 hours | Configurable via `jwt.expiration` |
| HTTP Sessions | Stateless (no timeout) | JWT-based, no server sessions |

### § 164.312(b) - Audit Controls

| Requirement | HDIM Implementation | Evidence |
|-------------|---------------------|----------|
| Audit Logging | Comprehensive event logging | `AuditService.java` |
| Audit Storage | PostgreSQL with 6-year retention | Audit schema |
| Audit Review | Prometheus/Grafana dashboards | Monitoring stack |
| Log Integrity | Immutable audit records | Database constraints |

**Audit Events Captured:**

| Event Type | Data Logged | Retention |
|------------|-------------|-----------|
| Authentication | User ID, IP, timestamp, success/failure | 6 years |
| PHI Access | User, patient ID, resource type, action | 6 years |
| Data Modification | Before/after values, user, timestamp | 6 years |
| WebSocket Connect/Disconnect | Session ID, user, tenant, duration | 6 years |
| Export/Download | User, data scope, timestamp, format | 6 years |

**Code References:**
- `AuditService.java` - Core audit logging service
- `AuditLoggingInterceptor.java` - WebSocket audit logging
- `AuditEventEntity.java` - Audit record structure

### § 164.312(c)(1) - Integrity Controls

| Requirement | HDIM Implementation | Evidence |
|-------------|---------------------|----------|
| Data Integrity | Database constraints, validation | Entity validation annotations |
| Transmission Integrity | TLS 1.2+ for all communications | nginx TLS configuration |
| Checksum Verification | FHIR resource versioning | ETag headers |

### § 164.312(d) - Person or Entity Authentication

| Requirement | HDIM Implementation | Evidence |
|-------------|---------------------|----------|
| User Authentication | JWT token validation | `JwtTokenService.java` |
| Service Authentication | mTLS between services (optional) | Service mesh config |
| MFA Support | TOTP-based MFA | `MfaTokenService.java` |

**Authentication Security:**
- All API endpoints require valid JWT (removed DEMO MODE bypasses)
- WebSocket connections validated via `JwtWebSocketHandshakeInterceptor`
- Rate limiting prevents brute-force attacks

### § 164.312(e)(1) - Transmission Security

| Requirement | HDIM Implementation | Evidence |
|-------------|---------------------|----------|
| Integrity Controls | TLS 1.2+ with strong ciphers | nginx configuration |
| Encryption | HTTPS/WSS for all traffic | SSL certificates |

**TLS Configuration:**

```nginx
# Minimum TLS version
ssl_protocols TLSv1.2 TLSv1.3;

# Strong cipher suites only
ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:...

# HSTS enabled
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains";
```

---

## Data Encryption Summary

### Encryption at Rest

| Data Type | Encryption Method | Key Management |
|-----------|-------------------|----------------|
| Database (PostgreSQL) | TDE or volume encryption | Cloud KMS / LUKS |
| SSN Field | AES-256-GCM | HashiCorp Vault |
| PHI Fields (names, addresses) | AES-256-GCM | HashiCorp Vault |
| Backup Files | GPG encryption | Vault-managed keys |
| Redis Cache | TLS + RDB encryption | Application keys |

### Encryption in Transit

| Communication Path | Protocol | Minimum Version |
|--------------------|----------|-----------------|
| Client → Gateway | HTTPS | TLS 1.2 |
| Gateway → Services | HTTPS | TLS 1.2 |
| WebSocket | WSS | TLS 1.2 |
| Database Connections | TLS | TLS 1.2 |
| Service-to-Service | HTTPS/mTLS | TLS 1.2 |

---

## Multi-Tenant Isolation

### Row-Level Security

All database tables include tenant isolation:

```sql
-- Example: Patient table RLS
ALTER TABLE patients ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON patients
    USING (tenant_id = current_setting('app.tenant_id')::uuid);
```

### API-Level Isolation

- `TenantAccessFilter` validates tenant claims on every request
- `JwtWebSocketHandshakeInterceptor` enforces tenant on WebSocket connections
- `TenantAccessInterceptor` validates WebSocket tenant access

### Audit Isolation

All audit logs include tenant context for proper scoping of compliance reports.

---

## Security Incident Response

See: `/compliance/INCIDENT_RESPONSE_PLAN.md`

Key response timelines:
- **Breach Detection:** Automated monitoring with immediate alerts
- **Initial Assessment:** Within 24 hours
- **Notification:** Within 60 days per HIPAA requirements
- **Remediation:** Based on severity assessment

---

## Compliance Verification

### Automated Testing

| Test Type | Frequency | Coverage |
|-----------|-----------|----------|
| Unit Tests | Every commit | Security filters, encryption |
| Integration Tests | Every PR | Authentication flows |
| Security Scans | Weekly | OWASP dependency check |
| Penetration Testing | Annual | Full application scope |

### Manual Review

| Review Type | Frequency | Reviewer |
|-------------|-----------|----------|
| Code Review | Every PR | Senior developer |
| Security Review | Quarterly | Security team |
| Compliance Audit | Annual | External auditor |

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | Dec 2024 | HDIM Security Team | Initial creation |

---

## Appendix A: File Reference Index

| Security Control | Primary Files |
|-----------------|---------------|
| JWT Authentication | `JwtAuthenticationFilter.java`, `JwtTokenService.java` |
| Tenant Isolation | `TenantAccessFilter.java`, `TenantAccessInterceptor.java` |
| Field Encryption | `FieldEncryptionService.java`, `@Encrypted` annotation |
| Audit Logging | `AuditService.java`, `AuditLoggingInterceptor.java` |
| WebSocket Security | `JwtWebSocketHandshakeInterceptor.java`, `WebSocketConfig.java` |
| Session Timeout | `SessionTimeoutManager.java` |
| Rate Limiting | `RateLimitingInterceptor.java` |
| Security Configs | `*SecurityConfig.java` in each service |

---

## Appendix B: BAA Technical Exhibit

This document serves as a technical exhibit for Business Associate Agreements, demonstrating HDIM's technical safeguards implementation in accordance with 45 CFR § 164.314(a)(2)(i).

**Covered Controls:**
- Access controls (§ 164.312(a))
- Audit controls (§ 164.312(b))
- Integrity controls (§ 164.312(c))
- Authentication (§ 164.312(d))
- Transmission security (§ 164.312(e))

For questions regarding this controls matrix, contact: security@hdim.health
