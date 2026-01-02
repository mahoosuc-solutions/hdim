# Security Checklist - Production Deployment

**Application:** HealthData-in-Motion Clinical Portal
**Version:** 1.0.0
**Checklist Date:** _____________
**Reviewer:** _____________

---

## Table of Contents

1. [OWASP Top 10 Verification](#1-owasp-top-10-verification)
2. [HIPAA Security Requirements](#2-hipaa-security-requirements)
3. [Authentication & Authorization](#3-authentication--authorization)
4. [Data Protection](#4-data-protection)
5. [Network Security](#5-network-security)
6. [Application Security](#6-application-security)
7. [Infrastructure Security](#7-infrastructure-security)
8. [Monitoring & Incident Response](#8-monitoring--incident-response)
9. [Compliance & Audit](#9-compliance--audit)
10. [Security Scanning](#10-security-scanning)

---

## 1. OWASP Top 10 Verification

### A01:2021 - Broken Access Control

- [ ] **Authorization checks on all endpoints**
  - Verify: All API endpoints require authentication
  - Verify: Role-based access control (RBAC) implemented
  - Verify: Users can only access their own data or authorized tenant data
  - Test: Attempt to access resources without authentication
  - Test: Attempt to access other users' resources

- [ ] **Tenant isolation**
  - Verify: All database queries filtered by tenant_id
  - Verify: No cross-tenant data access possible
  - Test: Attempt to query data from different tenant

- [ ] **Direct object reference protection**
  - Verify: No predictable IDs exposed (use UUIDs)
  - Verify: Authorization checks before object access
  - Test: Attempt to access objects by ID manipulation

- [ ] **CORS configuration**
  - Verify: CORS restricted to specific origins
  - Verify: Credentials only allowed for trusted origins
  - Test: Attempt CORS request from unauthorized origin

### A02:2021 - Cryptographic Failures

- [ ] **Data encryption in transit**
  - Verify: HTTPS enforced on all endpoints
  - Verify: TLS 1.2 or higher required
  - Verify: Strong cipher suites configured
  - Verify: HTTP Strict Transport Security (HSTS) enabled
  - Test: Attempt HTTP connection (should redirect to HTTPS)

- [ ] **Data encryption at rest**
  - Verify: Database encryption enabled for PHI
  - Verify: Sensitive fields encrypted (SSN, medical records)
  - Verify: Encryption keys stored securely (not in code)
  - Test: Verify encrypted data in database

- [ ] **Password storage**
  - Verify: Passwords hashed with bcrypt/scrypt/Argon2
  - Verify: Salt used for each password
  - Verify: Password history maintained (HIPAA requirement)

- [ ] **Sensitive data handling**
  - Verify: No PHI logged in plain text
  - Verify: Sensitive data masked in logs
  - Verify: Credit card data not stored (if applicable)

### A03:2021 - Injection

- [ ] **SQL Injection prevention**
  - Verify: Parameterized queries used everywhere
  - Verify: ORM (JPA/Hibernate) used properly
  - Verify: No raw SQL with user input
  - Test: Input SQL payloads in forms (e.g., `' OR '1'='1`)
  - Run: SQL injection scanner (SQLMap)

- [ ] **NoSQL Injection prevention**
  - Verify: Input validation on MongoDB/Redis queries
  - Verify: No query operators in user input

- [ ] **Command Injection prevention**
  - Verify: No system commands executed with user input
  - Verify: Input sanitized if shell commands used

- [ ] **LDAP/XPath Injection prevention**
  - Verify: Input escaped for LDAP queries (if used)

### A04:2021 - Insecure Design

- [ ] **Secure architecture**
  - Verify: Threat model documented
  - Verify: Security considered in all design decisions
  - Verify: Principle of least privilege applied
  - Verify: Defense in depth implemented

- [ ] **Business logic security**
  - Verify: Rate limiting on sensitive operations
  - Verify: Anti-automation controls (CAPTCHA, throttling)
  - Verify: Proper workflow validation

### A05:2021 - Security Misconfiguration

- [ ] **Secure defaults**
  - Verify: Default passwords changed
  - Verify: Unnecessary features disabled
  - Verify: Debug mode disabled in production
  - Verify: Verbose error messages disabled

- [ ] **Framework security**
  - Verify: Angular/Spring Boot up to date
  - Verify: Security headers configured
  - Verify: Content Security Policy (CSP) enabled

- [ ] **Dependency management**
  - Verify: All dependencies up to date
  - Verify: No known vulnerable dependencies
  - Run: `npm audit` and `./gradlew dependencyCheckAnalyze`

- [ ] **Cloud/Container configuration**
  - Verify: Docker images scanned for vulnerabilities
  - Verify: Kubernetes security policies applied
  - Verify: Secrets not hardcoded in containers

### A06:2021 - Vulnerable and Outdated Components

- [ ] **Dependency scanning**
  ```bash
  # Frontend
  npm audit
  npm audit fix

  # Backend
  ./gradlew dependencyCheckAnalyze

  # Docker images
  docker scan healthdata/clinical-portal:1.0.0
  ```

- [ ] **Component inventory**
  - Document: All third-party components used
  - Document: Version of each component
  - Document: Known vulnerabilities and mitigation

- [ ] **Update policy**
  - Policy: Regular dependency updates scheduled
  - Policy: Security patches applied within 30 days

### A07:2021 - Identification and Authentication Failures

- [ ] **Authentication implementation**
  - Verify: JWT tokens used properly
  - Verify: Token expiration enforced (1 hour max)
  - Verify: Refresh tokens rotated
  - Verify: Multi-factor authentication supported (optional)

- [ ] **Password policy**
  - Verify: Minimum 12 characters required
  - Verify: Complexity requirements enforced
  - Verify: Password history checked (last 5 passwords)
  - Verify: Common passwords blocked

- [ ] **Session management**
  - Verify: Session timeout configured (15-30 minutes idle)
  - Verify: Concurrent session limit enforced
  - Verify: Session invalidation on logout
  - Verify: Session ID regenerated after login

- [ ] **Account lockout**
  - Verify: Account locked after 5 failed attempts
  - Verify: Lockout duration 30 minutes
  - Verify: Lockout logged for audit

### A08:2021 - Software and Data Integrity Failures

- [ ] **Code integrity**
  - Verify: Code signed (if applicable)
  - Verify: Secure CI/CD pipeline
  - Verify: Dependency integrity checks (npm/gradle)

- [ ] **Update mechanism**
  - Verify: Updates signed and verified
  - Verify: Rollback capability exists

- [ ] **Deserialization**
  - Verify: User input not deserialized
  - Verify: Deserialization restricted to trusted sources

### A09:2021 - Security Logging and Monitoring Failures

- [ ] **Logging**
  - Verify: All authentication events logged
  - Verify: All authorization failures logged
  - Verify: All PHI access logged (HIPAA requirement)
  - Verify: Input validation failures logged
  - Verify: No sensitive data in logs

- [ ] **Monitoring**
  - Verify: Real-time monitoring configured
  - Verify: Alerts for suspicious activity
  - Verify: Log aggregation in place (ELK, Datadog, etc.)

- [ ] **Audit trail**
  - Verify: Immutable audit logs
  - Verify: Audit logs include: who, what, when, where
  - Verify: Audit logs retained for 6 years (HIPAA)

### A10:2021 - Server-Side Request Forgery (SSRF)

- [ ] **URL validation**
  - Verify: User-provided URLs validated
  - Verify: Whitelist of allowed domains
  - Verify: Internal network access blocked

- [ ] **SSRF prevention**
  - Verify: No arbitrary URL requests from user input
  - Test: Attempt to access internal resources via SSRF

---

## 2. HIPAA Security Requirements

### Administrative Safeguards

- [ ] **Security Management Process (164.308(a)(1))**
  - Document: Risk assessment conducted
  - Document: Risk management plan
  - Document: Sanction policy for violations
  - Document: Information system activity review

- [ ] **Workforce Security (164.308(a)(3))**
  - Policy: User authorization procedures
  - Policy: Workforce clearance procedures
  - Policy: Termination procedures

- [ ] **Information Access Management (164.308(a)(4))**
  - Verify: Access based on role and need
  - Verify: Access reviewed quarterly
  - Verify: Emergency access procedures documented

- [ ] **Security Awareness and Training (164.308(a)(5))**
  - Plan: Security reminders scheduled
  - Plan: Protection from malicious software
  - Plan: Password management training
  - Plan: Login monitoring training

- [ ] **Contingency Plan (164.308(a)(7))**
  - Document: Data backup plan
  - Document: Disaster recovery plan
  - Document: Emergency mode operation plan
  - Test: Disaster recovery annually

- [ ] **Evaluation (164.308(a)(8))**
  - Schedule: Annual security evaluation

### Physical Safeguards

- [ ] **Facility Access Controls (164.310(a)(1))**
  - Policy: Physical access restrictions
  - Policy: Visitor control procedures
  - Policy: Access control and validation

- [ ] **Workstation Use (164.310(b))**
  - Policy: Workstation security policies

- [ ] **Workstation Security (164.310(c))**
  - Verify: Screen lock after inactivity
  - Verify: Encryption on laptops
  - Verify: Physical security measures

- [ ] **Device and Media Controls (164.310(d)(1))**
  - Policy: Device disposal procedures
  - Policy: Media re-use procedures
  - Policy: Data backup procedures

### Technical Safeguards

- [ ] **Access Control (164.312(a)(1))**
  - Verify: Unique user identification (required)
  - Verify: Emergency access procedure (required)
  - Verify: Automatic logoff (addressable)
  - Verify: Encryption and decryption (addressable)

- [ ] **Audit Controls (164.312(b))**
  - Verify: Audit trail captures PHI access
  - Verify: Audit logs tamper-proof
  - Verify: Audit review process in place

- [ ] **Integrity (164.312(c)(1))**
  - Verify: Data integrity checks
  - Verify: Electronic signatures (if used)

- [ ] **Person or Entity Authentication (164.312(d))**
  - Verify: User authentication required
  - Test: Cannot access without authentication

- [ ] **Transmission Security (164.312(e)(1))**
  - Verify: PHI encrypted in transit (TLS 1.2+)
  - Verify: Integrity controls (checksums, digital signatures)

### HIPAA Breach Notification Rule

- [ ] **Breach notification procedures**
  - Document: Breach assessment procedure
  - Document: Notification timeline (60 days)
  - Document: Notification content requirements
  - Document: Escalation procedures

---

## 3. Authentication & Authorization

### JWT Implementation

- [ ] **Token generation**
  - Verify: Strong secret key (64+ bytes)
  - Verify: Secret key rotated quarterly
  - Verify: Algorithm HS256 or RS256 (not "none")
  - Test: Token cannot be modified without detection

- [ ] **Token validation**
  - Verify: Signature verified on every request
  - Verify: Expiration checked
  - Verify: Issuer validated
  - Verify: Audience validated

- [ ] **Token storage**
  - Verify: Tokens stored in httpOnly cookies (preferred)
  - Or: Tokens in memory (not localStorage)
  - Verify: Refresh tokens stored securely

### Role-Based Access Control (RBAC)

- [ ] **Roles defined**
  - ADMIN: Full system access
  - CLINICIAN: Patient data access, run evaluations
  - QUALITY_MANAGER: View reports, compliance data
  - VIEWER: Read-only access

- [ ] **Permissions matrix**
  ```
  | Resource           | ADMIN | CLINICIAN | QUALITY_MGR | VIEWER |
  |--------------------|-------|-----------|-------------|--------|
  | Patient Data       | RW    | RW        | R           | R      |
  | Run Evaluations    | RW    | RW        | R           | -      |
  | Custom Measures    | RW    | R         | R           | R      |
  | Reports            | RW    | RW        | RW          | R      |
  | User Management    | RW    | -         | -           | -      |
  | System Settings    | RW    | -         | -           | -      |
  ```

- [ ] **Authorization checks**
  - Verify: Checked on every API call
  - Verify: Checked on server side (not client only)
  - Test: Lower-privileged user cannot access higher-privileged resources

---

## 4. Data Protection

### Data Classification

- [ ] **PHI (Protected Health Information)**
  - Patient name, MRN, SSN
  - Medical record numbers
  - Health plan beneficiary numbers
  - Dates (birth, admission, discharge, death)
  - Biometric identifiers
  - Full-face photographs

- [ ] **Sensitive but not PHI**
  - User credentials
  - API keys, tokens
  - Audit logs

- [ ] **Public data**
  - Application version
  - API documentation
  - Health check endpoints

### Encryption

- [ ] **Encryption in transit**
  ```
  Protocol: TLS 1.2 minimum, TLS 1.3 preferred
  Ciphers: ECDHE-RSA-AES128-GCM-SHA256 or better
  Certificate: Valid SSL cert from trusted CA
  HSTS: Enabled with max-age=31536000
  ```

- [ ] **Encryption at rest**
  - Database: Transparent Data Encryption (TDE) enabled
  - Backups: Encrypted before storage
  - Logs: PHI redacted or encrypted

- [ ] **Key management**
  - Verify: Keys stored in HSM or key vault
  - Verify: Keys rotated annually
  - Verify: Key access audited
  - Verify: Key backup procedure documented

### Data Masking and Redaction

- [ ] **Logging**
  - Verify: PHI redacted from logs
  - Verify: Credit card numbers masked
  - Example: `SSN: ***-**-1234`

- [ ] **Display**
  - Verify: SSN partially masked in UI
  - Verify: Full data only shown to authorized users

---

## 5. Network Security

### Firewall Configuration

- [ ] **Inbound rules**
  - Allow: 80 (HTTP - redirects to HTTPS)
  - Allow: 443 (HTTPS)
  - Allow: SSH (22) from specific IPs only
  - Block: All other inbound traffic

- [ ] **Outbound rules**
  - Allow: HTTPS (443) for external API calls
  - Allow: DNS (53)
  - Allow: NTP (123)
  - Block: Unnecessary outbound traffic

- [ ] **Internal network**
  - Verify: Services communicate on internal network
  - Verify: Database not accessible from internet
  - Verify: Redis not accessible from internet

### TLS/SSL Configuration

- [ ] **Certificate**
  - Verify: Valid certificate from trusted CA
  - Verify: Certificate not expired
  - Verify: Certificate matches domain
  - Verify: Certificate chain complete

- [ ] **SSL Labs test**
  - Test: https://www.ssllabs.com/ssltest/
  - Target: A+ rating
  - Verify: No SSL/TLS vulnerabilities

### DDoS Protection

- [ ] **Rate limiting**
  - Verify: Rate limiting per IP address
  - Verify: Rate limiting per user
  - Verify: Gradual backoff for repeated violations

- [ ] **CDN/WAF**
  - Consider: Cloudflare, AWS WAF, or similar
  - Verify: DDoS protection enabled
  - Verify: Bot protection enabled

---

## 6. Application Security

### Input Validation

- [ ] **Server-side validation**
  - Verify: All inputs validated on server
  - Verify: Whitelist validation (preferred)
  - Verify: Data type validation
  - Verify: Length limits enforced
  - Verify: Format validation (email, phone, date)

- [ ] **Sanitization**
  - Verify: HTML tags stripped from user input
  - Verify: Script tags blocked
  - Verify: SQL keywords escaped

### Output Encoding

- [ ] **XSS prevention**
  - Verify: Angular automatic escaping enabled
  - Verify: DomSanitizer used for dynamic content
  - Verify: No `innerHTML` with user content
  - Test: Input XSS payloads: `<script>alert('XSS')</script>`

### CSRF Protection

- [ ] **Anti-CSRF tokens**
  - Verify: CSRF tokens on state-changing requests (POST, PUT, DELETE)
  - Verify: SameSite cookie attribute set
  - Test: Cross-site request attempt should fail

### Security Headers

- [ ] **HTTP response headers**
  ```
  Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
  X-Frame-Options: DENY
  X-Content-Type-Options: nosniff
  X-XSS-Protection: 1; mode=block
  Referrer-Policy: no-referrer-when-downgrade
  Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:
  Permissions-Policy: geolocation=(), microphone=(), camera=()
  ```

- [ ] **Verify headers**
  - Test: Use https://securityheaders.com/
  - Target: A+ rating

### File Upload Security

- [ ] **File validation**
  - Verify: File type whitelist (not blacklist)
  - Verify: File size limits (e.g., 10 MB)
  - Verify: File content validation (not just extension)
  - Verify: Files scanned for malware

- [ ] **File storage**
  - Verify: Files stored outside webroot
  - Verify: Files served with correct content-type
  - Verify: No script execution in upload directory

---

## 7. Infrastructure Security

### Container Security

- [ ] **Docker images**
  - Verify: Base images from trusted sources only
  - Verify: Images scanned for vulnerabilities
  - Verify: Multi-stage builds used
  - Verify: Non-root user runs application

- [ ] **Container runtime**
  - Verify: Containers run with minimal privileges
  - Verify: Resource limits set (CPU, memory)
  - Verify: Read-only root filesystem (where possible)

### Database Security

- [ ] **Access control**
  - Verify: Dedicated user per service (not admin)
  - Verify: Least privilege granted
  - Verify: No public access to database
  - Verify: SSL/TLS required for connections

- [ ] **Hardening**
  - Verify: Default accounts disabled
  - Verify: Unnecessary extensions disabled
  - Verify: Audit logging enabled

### Secrets Management

- [ ] **No hardcoded secrets**
  - Verify: No passwords in code
  - Verify: No API keys in code
  - Verify: No secrets in version control

- [ ] **Secrets storage**
  - Use: HashiCorp Vault, AWS Secrets Manager, or Azure Key Vault
  - Verify: Secrets encrypted at rest
  - Verify: Secrets rotated regularly
  - Verify: Access to secrets audited

---

## 8. Monitoring & Incident Response

### Security Monitoring

- [ ] **Events to monitor**
  - Failed login attempts
  - Privilege escalation attempts
  - Unauthorized access attempts
  - Unusual data access patterns
  - Configuration changes
  - Application errors (especially authentication/authorization)

- [ ] **Alerting**
  - Alert: >5 failed logins from same IP in 5 minutes
  - Alert: Unauthorized access attempt
  - Alert: Database connection failures
  - Alert: Application error rate >5%

### Incident Response

- [ ] **Incident response plan**
  - Document: Incident classification
  - Document: Response procedures
  - Document: Escalation path
  - Document: Communication plan
  - Test: Conduct tabletop exercise

- [ ] **Breach notification**
  - Document: HIPAA breach notification process
  - Document: 60-day notification requirement
  - Contact: Legal/compliance team

---

## 9. Compliance & Audit

### Documentation

- [ ] **Security policies**
  - Policy: Acceptable use policy
  - Policy: Password policy
  - Policy: Access control policy
  - Policy: Incident response policy
  - Policy: Data retention and disposal

- [ ] **Procedures**
  - Procedure: User onboarding
  - Procedure: User offboarding
  - Procedure: Access review
  - Procedure: Vulnerability management
  - Procedure: Patch management

### Audit Trail

- [ ] **Audit logging**
  - Log: User authentication (success and failure)
  - Log: User authorization failures
  - Log: PHI access (read, write, delete)
  - Log: Configuration changes
  - Log: Privileged operations

- [ ] **Audit review**
  - Schedule: Weekly audit log review
  - Document: Findings and actions taken

---

## 10. Security Scanning

### Automated Scans

- [ ] **Dependency scanning**
  ```bash
  # Frontend
  npm audit
  npm audit fix

  # Backend
  ./gradlew dependencyCheckAnalyze

  # Docker
  docker scan healthdata/clinical-portal:1.0.0
  ```

- [ ] **SAST (Static Application Security Testing)**
  - Tool: SonarQube, Checkmarx, or Veracode
  - Frequency: Every commit (CI/CD pipeline)

- [ ] **DAST (Dynamic Application Security Testing)**
  - Tool: OWASP ZAP, Burp Suite
  - Frequency: Weekly

- [ ] **Container scanning**
  - Tool: Snyk, Clair, Trivy
  - Frequency: On image build

### Penetration Testing

- [ ] **Schedule**
  - Frequency: Annually (minimum)
  - Or: After major changes

- [ ] **Scope**
  - Network penetration testing
  - Application penetration testing
  - Social engineering (if applicable)

- [ ] **Remediation**
  - Critical: Fix within 7 days
  - High: Fix within 30 days
  - Medium: Fix within 90 days
  - Low: Fix when possible

---

## Sign-Off

**Security Engineer:** _____________________ **Date:** _________

**CISO/Security Lead:** _____________________ **Date:** _________

**Compliance Officer:** _____________________ **Date:** _________

**Product Manager:** _____________________ **Date:** _________

---

## Vulnerability Severity Matrix

| Severity | CVSS Score | Response Time | Remediation Time |
|----------|------------|---------------|------------------|
| Critical | 9.0 - 10.0 | Immediate | 24 hours |
| High | 7.0 - 8.9 | 24 hours | 7 days |
| Medium | 4.0 - 6.9 | 1 week | 30 days |
| Low | 0.1 - 3.9 | 1 month | 90 days |

---

## Security Contact Information

**Security Team:** security@healthdata.example.com
**Incident Response:** incident@healthdata.example.com
**Vulnerability Reporting:** security-reports@healthdata.example.com

**On-Call Security:** +1-XXX-XXX-XXXX

---

**End of Security Checklist**
