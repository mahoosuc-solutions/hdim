# OWASP Top 10 Security Checklist

**Application**: CMS Connector Service  
**Phase**: 5 Week 2  
**Date**: January 1, 2026  

---

## A1: Broken Authentication

Authentication failures can allow attackers to gain unauthorized access.

### Requirements
- [ ] **Passwords hashed with strong algorithm** (bcrypt, Argon2, scrypt)
  - Verify: Check password storage in database
  - Command: `grep -r "bcrypt\|Argon2" src/`

- [ ] **No hardcoded credentials** in source code
  - Verify: Search for credentials
  - Command: `grep -r "password\|secret\|token" src/ | grep -v "Password\|secret_key\|TOKEN" | head -20`

- [ ] **Session tokens are secure**
  - HttpOnly flag set (prevents JavaScript access)
  - Secure flag set (HTTPS only)
  - SameSite flag set (prevents CSRF)
  - Command: Check response headers

- [ ] **JWT signing key stored securely**
  - Verify: Key is not in source code
  - Verify: Key is in environment variables or secure vault

- [ ] **MFA/2FA implemented** (if required by threat model)
  - Status: Required for admin accounts
  - Verification method: Test admin login flow

- [ ] **Brute force protection enabled**
  - Verify: Account lockout after failed attempts
  - Verify: Rate limiting on login endpoint
  - Command: Test with multiple failed logins

- [ ] **Account lockout after N failed attempts**
  - Default: 5 attempts, 15-minute lockout
  - Verification: Test with 6 failed logins

### Status
- [ ] PASSED
- [ ] FAILED
- [ ] REMEDIATION IN PROGRESS

**Notes**:
```


```

---

## A2: Broken Access Control

Insufficient access control allows unauthorized data access or modification.

### Requirements
- [ ] **Role-based access control (RBAC) implemented**
  - Roles: Admin, Manager, User, ReadOnly
  - Verify: Check UserRole enum in code
  - Command: `find src -name "*Role*" -type f`

- [ ] **Principle of least privilege**
  - Users have minimum required permissions
  - Verify: Check role-permission mapping
  - Audit: Review user roles quarterly

- [ ] **Authorization checks on all endpoints**
  - Every protected endpoint has @PreAuthorize or @Secured
  - Command: `grep -r "@PreAuthorize\|@Secured" src/`

- [ ] **No insecure direct object references (IDOR)**
  - Cannot access other user's data by changing ID in URL
  - Test: Try accessing claim with different IDs
  - Command: `curl -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/v1/claims/OTHER_USER_ID`

- [ ] **Proper permission inheritance**
  - Child resources inherit parent permissions
  - Example: Can only access claim if user can access patient

- [ ] **Audit logging of access changes**
  - All permission changes are logged
  - Command: Check audit logs for permission changes

### Status
- [ ] PASSED
- [ ] FAILED
- [ ] REMEDIATION IN PROGRESS

**Notes**:
```


```

---

## A3: SQL Injection

Improper input handling allows attackers to inject SQL commands.

### Requirements
- [ ] **Parameterized queries everywhere**
  - Never concatenate SQL strings
  - Use JPA/Hibernate with parameterized queries
  - Command: `grep -r "SELECT.*\$\|concatenate" src/ | grep -i sql`

- [ ] **ORM validation on input**
  - Hibernate validates entity constraints
  - Custom validators for business logic
  - Command: Check @Valid annotations on controller methods

- [ ] **Web Application Firewall (WAF) rules configured**
  - AWS WAF or similar protecting application
  - SQL injection patterns blocked
  - Verification: Check WAF rules in AWS console

- [ ] **Regular expressions properly escaped**
  - Regex patterns don't allow code injection
  - Test with special characters: `*+?[]{}()^$.|\`

- [ ] **No dynamic query construction**
  - Queries are static, parameters are passed separately
  - Command: `grep -r "LIKE\|WHERE.*+" src/main/java | head -10`

### Status
- [ ] PASSED
- [ ] FAILED
- [ ] REMEDIATION IN PROGRESS

**Notes**:
```


```

---

## A4: Insecure Design

Lack of security-by-design principles.

### Requirements
- [ ] **Threat modeling completed**
  - Document: threat-model.md
  - Identify potential attack vectors
  - Define mitigation strategies

- [ ] **Security requirements documented**
  - All security features documented
  - Authentication, authorization, encryption requirements clear

- [ ] **Secure by default configuration**
  - Debug mode disabled in production
  - Verbose error messages disabled
  - Default passwords changed
  - Command: Check application properties for production

- [ ] **Rate limiting implemented**
  - API endpoints have rate limiting
  - Prevents brute force and DoS
  - Command: `grep -r "@RateLimit\|RateLimitingAspect" src/`

- [ ] **Resource limits configured**
  - Max file upload size
  - Max request body size
  - Max query results
  - Command: Check application.yml for limits

### Status
- [ ] PASSED
- [ ] FAILED
- [ ] REMEDIATION IN PROGRESS

**Notes**:
```


```

---

## A5: Security Misconfiguration

Insecure default settings or incomplete configurations.

### Requirements
- [ ] **Security headers set**
  - HSTS (HTTP Strict-Transport-Security)
  - CSP (Content-Security-Policy)
  - X-Frame-Options: DENY
  - X-Content-Type-Options: nosniff
  - X-XSS-Protection: 1; mode=block
  - Command: `curl -I http://localhost:8081 | grep -i "X-\|Strict"`

- [ ] **HTTPS enforced**
  - All traffic redirected to HTTPS
  - HSTS enabled with long max-age
  - Command: Test with curl (should redirect)

- [ ] **TLS 1.2 or higher required**
  - TLS 1.0 and 1.1 disabled
  - Only modern ciphers enabled
  - Command: `openssl s_client -connect localhost:443 -tls1_2`

- [ ] **Default credentials changed**
  - No default admin password
  - Database user has strong password
  - Verification: Manually verify

- [ ] **Unnecessary services disabled**
  - Debug endpoints disabled in production
  - Actuator endpoints restricted
  - Command: Check actuator configuration

- [ ] **Debug mode disabled in production**
  - spring.devtools.restart.enabled=false
  - logging.level.root=WARN (not DEBUG)
  - Command: Check application-prod.yml

- [ ] **Error messages don't leak information**
  - Generic error messages to users
  - Detailed errors only in logs
  - Test: Try accessing /nonexistent endpoint

### Status
- [ ] PASSED
- [ ] FAILED
- [ ] REMEDIATION IN PROGRESS

**Notes**:
```


```

---

## A6: Vulnerable & Outdated Components

Using libraries with known vulnerabilities.

### Requirements
- [ ] **Dependency scanning enabled**
  - OWASP Dependency-Check integrated in CI/CD
  - Results reviewed regularly
  - Command: `mvn org.owasp:dependency-check-maven:check`

- [ ] **Regular dependency updates**
  - Dependencies updated at least quarterly
  - Security patches applied immediately
  - Command: `mvn versions:display-dependency-updates`

- [ ] **Known CVEs patched**
  - No known CVEs in production
  - Security advisories monitored
  - Command: Check NVD (nvd.nist.gov) for dependencies

- [ ] **Supply chain security verified**
  - Dependencies from trusted sources
  - Checksums verified
  - Command: Check maven-checksum validation

- [ ] **No end-of-life (EOL) dependencies**
  - Spring Boot 3.2.0 (current LTS)
  - Java 17 (LTS, support until 2029)
  - PostgreSQL 15 (support until 2025)
  - Command: Check versions against support timelines

### Status
- [ ] PASSED
- [ ] FAILED
- [ ] REMEDIATION IN PROGRESS

**Notes**:
```


```

---

## A7: Identification & Authentication Failures

Weak authentication and session management.

### Requirements
- [ ] **Account recovery is secure**
  - Recovery tokens are random and short-lived
  - Recovery email validated
  - Command: Test account recovery flow

- [ ] **Password reset tokens expire quickly**
  - Tokens expire within 1 hour
  - Tokens are single-use
  - Command: Test password reset link expiration

- [ ] **Session management is secure**
  - Sessions have short timeout (15-30 minutes)
  - Sessions invalidated on logout
  - Sessions can't be hijacked
  - Command: Check session configuration

- [ ] **No sensitive data in URLs**
  - Tokens not in query parameters
  - Passwords never in logs or URLs
  - Command: `grep -r "password\|token" logs/ | grep "http\|url"`

- [ ] **Logout clears session completely**
  - Session is invalidated
  - Cookies are cleared
  - Token is revoked
  - Command: Test logout flow

### Status
- [ ] PASSED
- [ ] FAILED
- [ ] REMEDIATION IN PROGRESS

**Notes**:
```


```

---

## A8: Software & Data Integrity Failures

Insecure code deployment and supply chain risks.

### Requirements
- [ ] **Code integrity verification**
  - Code is signed or checksummed
  - Builds are reproducible
  - Command: Check git signatures

- [ ] **Dependencies from trusted sources**
  - Maven Central only
  - Verify checksums
  - Command: Check pom.xml for repository configuration

- [ ] **CI/CD pipeline secured**
  - Only approved changes deployed
  - Build artifacts signed
  - Deployment requires approval
  - Command: Review GitHub Actions workflows

- [ ] **Deployment integrity verified**
  - Deployments are auditable
  - Deployment logs are immutable
  - Command: Check deployment audit trail

### Status
- [ ] PASSED
- [ ] FAILED
- [ ] REMEDIATION IN PROGRESS

**Notes**:
```


```

---

## A9: Logging & Monitoring Failures

Insufficient logging and inability to detect attacks.

### Requirements
- [ ] **Security events are logged**
  - Failed login attempts
  - Authorization failures
  - Data access events
  - Configuration changes
  - Command: Check logs for security events

- [ ] **Sensitive data not logged**
  - Passwords never logged
  - API keys never logged
  - PII not logged unnecessarily
  - Command: `grep -r "password\|api.key\|credit.card" logs/`

- [ ] **Logs are protected from modification**
  - Logs stored in secure location
  - Write-once storage if possible
  - Logs are centralized
  - Command: Check log file permissions

- [ ] **Monitoring alerts configured**
  - Alerts for suspicious activity
  - Alerts for security events
  - Alerts for anomalies
  - Command: Check alert configuration

- [ ] **Incident response plan documented**
  - Procedures for security incidents
  - Contact information for security team
  - Document: incident-response-plan.md

### Status
- [ ] PASSED
- [ ] FAILED
- [ ] REMEDIATION IN PROGRESS

**Notes**:
```


```

---

## A10: Server-Side Request Forgery (SSRF)

Application makes requests to unintended locations.

### Requirements
- [ ] **Input validation on URLs**
  - URLs validated before use
  - Only expected domains allowed
  - Command: Check URL parsing code

- [ ] **Allowlist of domains**
  - Only specific domains can be accessed
  - Wildcard domains not allowed
  - Command: Check domain allowlist configuration

- [ ] **No access to internal services**
  - Cannot access internal APIs
  - Cannot access metadata services (AWS, GCP)
  - Cannot access localhost
  - Test: Try to access http://localhost:5432

- [ ] **Rate limiting on outbound requests**
  - Prevents abuse of external requests
  - Command: Check outbound rate limiting

### Status
- [ ] PASSED
- [ ] FAILED
- [ ] REMEDIATION IN PROGRESS

**Notes**:
```


```

---

## Summary

| Vulnerability | Status | Priority | Notes |
|---------------|--------|----------|-------|
| A1: Broken Authentication | ⬜ | HIGH | |
| A2: Broken Access Control | ⬜ | HIGH | |
| A3: SQL Injection | ⬜ | CRITICAL | |
| A4: Insecure Design | ⬜ | HIGH | |
| A5: Security Misconfiguration | ⬜ | HIGH | |
| A6: Vulnerable & Outdated Components | ⬜ | HIGH | |
| A7: Identification & Authentication Failures | ⬜ | HIGH | |
| A8: Software & Data Integrity Failures | ⬜ | MEDIUM | |
| A9: Logging & Monitoring Failures | ⬜ | MEDIUM | |
| A10: Server-Side Request Forgery | ⬜ | MEDIUM | |

**Overall Status**: 0/10 items verified

**Legend**:
- ⬜ Not Started
- 🟨 In Progress  
- 🟩 Passed
- 🔴 Failed
- ⚠️ Remediation in Progress

---

## Next Steps

1. **Week 1**: Complete checklist items for each vulnerability
2. **Week 2**: Execute validation tests
3. **Week 3**: Remediate any failures
4. **Week 4**: Final validation and approval

**Remediation Priority**:
1. Critical (A3: SQL Injection)
2. High (A1, A2, A4, A5, A6, A7)
3. Medium (A8, A9, A10)

---

**Reviewed By**: Security Team  
**Approval Date**: TBD  
**Next Review**: Quarterly
