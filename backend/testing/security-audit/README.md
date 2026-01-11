# HDIM Security Audit Framework

Comprehensive security testing framework following OWASP Top 10 and HIPAA compliance requirements.

## Quick Start

```bash
# Run full security audit
./run-security-audit.sh all

# Run specific category
./run-security-audit.sh auth
./run-security-audit.sh injection
./run-security-audit.sh hipaa
```

## Test Categories

### 1. Authentication Security (`auth`)

Tests authentication mechanisms and session management.

**Tests Include**:
- ✅ Weak password rejection
- ✅ SQL injection in login fields
- ✅ Account lockout after failed attempts
- ✅ JWT token tampering detection
- ✅ Session fixation protection
- ✅ Token invalidation after logout

**OWASP**: A01:2021 – Broken Access Control, A07:2021 – Identification and Authentication Failures

### 2. Injection Vulnerabilities (`injection`)

Tests for injection attacks across different contexts.

**Tests Include**:
- ✅ SQL injection in search parameters
- ✅ NoSQL injection attempts
- ✅ Command injection in parameters
- ✅ LDAP injection attempts

**OWASP**: A03:2021 – Injection

### 3. Cross-Site Scripting (`xss`)

Tests for XSS vulnerabilities in user inputs and outputs.

**Tests Include**:
- ✅ Reflected XSS in error messages
- ✅ Stored XSS in user registration
- ✅ XSS payload escaping in responses

**OWASP**: A03:2021 – Injection (XSS)

### 4. CSRF Protection (`csrf`)

Validates Cross-Site Request Forgery protections.

**Tests Include**:
- ✅ State-changing operations require authentication
- ✅ CSRF token validation for sensitive operations

**OWASP**: A01:2021 – Broken Access Control

### 5. Security Headers (`headers`)

Validates HTTP security headers are properly configured.

**Tests Include**:
- ✅ X-Content-Type-Options: nosniff
- ✅ X-Frame-Options (clickjacking protection)
- ✅ Strict-Transport-Security (HSTS)
- ✅ Content-Security-Policy
- ✅ Cache-Control for sensitive data

**OWASP**: A05:2021 – Security Misconfiguration

### 6. TLS/SSL Configuration (`tls`)

Tests transport layer security configuration.

**Tests Include**:
- ✅ TLS 1.2+ enforcement
- ✅ Certificate validation
- ✅ Weak cipher suite detection

**OWASP**: A02:2021 – Cryptographic Failures

### 7. HIPAA Compliance (`hipaa`)

Tests HIPAA-specific security requirements for PHI protection.

**Tests Include**:
- ✅ PHI not exposed in error messages
- ✅ Audit logging for PHI access
- ✅ Role-based access control (RBAC)
- ✅ Data encryption verification

**Compliance**: HIPAA Security Rule §164.308, §164.310, §164.312

## Prerequisites

### Required Tools

```bash
# Ubuntu/Debian
sudo apt-get install curl jq openssl

# macOS
brew install curl jq openssl

# Verify installation
curl --version
jq --version
openssl version
```

### Running Services

```bash
# Ensure HDIM services are running
docker compose up -d

# Verify gateway is responding
curl http://localhost:8001/actuator/health
```

## Usage Examples

### Run Full Audit

```bash
./run-security-audit.sh all
```

Output:
```
========================================
HDIM Security Audit
========================================

Target: http://localhost:8001
Results: ./audit-results/20260110_203000

✓ PASS: Weak passwords are rejected
✓ PASS: SQL injection attempt blocked
⚠ WARN: Account lockout not detected
✗ FAIL: Missing X-Frame-Options header

Passed: 23
Failed: 3
Warnings: 7
```

### Run Specific Category

```bash
# Test only authentication
./run-security-audit.sh auth

# Test only HIPAA compliance
./run-security-audit.sh hipaa

# Test injection vulnerabilities
./run-security-audit.sh injection
```

### Custom Target URL

```bash
# Test staging environment
export GATEWAY_URL=https://staging.hdim.example.com
./run-security-audit.sh all

# Test production (requires approval!)
export GATEWAY_URL=https://api.hdim.example.com
./run-security-audit.sh headers  # Safe - read-only
```

## Understanding Results

### Test Status Codes

- **✓ PASS**: Security control is properly implemented
- **✗ FAIL**: Security vulnerability detected - **immediate action required**
- **⚠ WARN**: Potential issue or requires manual verification

### Critical Failures

Any FAIL result is critical and must be addressed:

```
✗ FAIL: SQL injection attempt successful
✗ FAIL: XSS vulnerability - script tag in response
✗ FAIL: Missing Cache-Control: no-store (PHI caching risk)
```

**Action**: Stop deployment, fix vulnerability, re-test.

### Warnings

WARN results indicate potential issues or manual verification needed:

```
⚠ WARN: Account lockout not detected
⚠ WARN: Missing HSTS header (recommended for production)
⚠ WARN: Verify audit logs manually
```

**Action**: Review and remediate before production deployment.

## Reports

### Security Audit Report

Each run generates a comprehensive markdown report:

```
audit-results/20260110_203000/
├── SECURITY_AUDIT_REPORT.md   # Full report
└── summary.txt                  # Test results summary
```

### Report Contents

```markdown
# HDIM Security Audit Report

**Date**: 2026-01-10
**Passed**: 23 tests
**Failed**: 3 tests
**Warnings**: 7 tests

## Critical Findings
- FAIL: Missing X-Frame-Options header (clickjacking risk)
- FAIL: Missing Cache-Control: no-store (PHI caching risk)
- FAIL: RBAC violation - viewer has admin privileges

## Recommendations
1. Add X-Frame-Options: DENY header
2. Configure Cache-Control for all PHI endpoints
3. Review and fix RBAC permissions
```

## HIPAA Compliance Checklist

Use this checklist to verify HIPAA compliance:

### Administrative Safeguards (§164.308)

- [ ] Security management process implemented
- [ ] Workforce security policies in place
- [ ] Audit logging enabled for all PHI access
- [ ] Security training completed for all developers
- [ ] Incident response plan documented

### Physical Safeguards (§164.310)

- [ ] Data center access controls in place
- [ ] Workstation security policies enforced
- [ ] Device and media controls implemented

### Technical Safeguards (§164.312)

- [ ] Access control mechanisms (unique user IDs)
- [ ] Audit controls (PHI access logging)
- [ ] Integrity controls (data validation)
- [ ] Transmission security (TLS 1.2+)
- [ ] Encryption at rest (database encryption)

### Breach Notification (§164.408)

- [ ] Breach detection mechanisms in place
- [ ] Notification procedures documented
- [ ] Incident logging and tracking system

## Integration with CI/CD

### GitHub Actions Example

```yaml
name: Security Audit

on:
  pull_request:
    branches: [main, master]
  schedule:
    - cron: '0 2 * * 1'  # Weekly on Monday 2am

jobs:
  security-audit:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Start services
        run: docker compose up -d

      - name: Wait for services
        run: sleep 30

      - name: Run security audit
        run: |
          cd backend/testing/security-audit
          ./run-security-audit.sh all

      - name: Check for failures
        run: |
          FAILS=$(grep -c "^FAIL:" audit-results/*/summary.txt || true)
          if [ "$FAILS" -gt 0 ]; then
            echo "Security audit failed with $FAILS failures"
            exit 1
          fi

      - name: Upload report
        uses: actions/upload-artifact@v3
        with:
          name: security-audit-report
          path: backend/testing/security-audit/audit-results/
```

### Pre-deployment Validation

```bash
#!/bin/bash
# pre-deploy-security-check.sh

echo "Running pre-deployment security audit..."
./run-security-audit.sh all

FAILS=$(grep -c "^FAIL:" audit-results/*/summary.txt || true)
WARNS=$(grep -c "^WARN:" audit-results/*/summary.txt || true)

if [ "$FAILS" -gt 0 ]; then
    echo "❌ DEPLOYMENT BLOCKED: $FAILS critical security issues"
    exit 1
fi

if [ "$WARNS" -gt 5 ]; then
    echo "⚠️  WARNING: $WARNS security warnings - review before deploying"
    exit 1
fi

echo "✅ Security audit passed - safe to deploy"
exit 0
```

## Common Issues and Fixes

### Issue: Missing Security Headers

**Problem**:
```
✗ FAIL: Missing X-Frame-Options header (clickjacking risk)
✗ FAIL: Missing Content-Security-Policy header
```

**Fix**:
```java
// SecurityConfig.java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
    http.headers(headers -> headers
        .frameOptions(frame -> frame.deny())
        .contentSecurityPolicy(csp -> csp
            .policyDirectives("default-src 'self'"))
        .httpStrictTransportSecurity(hsts -> hsts
            .maxAgeInSeconds(31536000)
            .includeSubDomains(true))
    );
    return http.build();
}
```

### Issue: PHI in Error Messages

**Problem**:
```
✗ FAIL: PHI potentially exposed in error response
```

**Fix**:
```java
// GlobalExceptionHandler.java
@ExceptionHandler(Exception.class)
public ResponseEntity<ErrorResponse> handleException(Exception ex) {
    // NEVER include PHI in error messages
    log.error("Error occurred", ex);  // Log full details
    return ResponseEntity.status(500)
        .body(new ErrorResponse("An error occurred"));  // Generic message
}
```

### Issue: Weak Password Accepted

**Problem**:
```
✗ FAIL: Weak password accepted
```

**Fix**:
```java
// PasswordValidator.java
@Component
public class PasswordValidator {
    public void validate(String password) {
        if (password.length() < 12) {
            throw new ValidationException("Password must be at least 12 characters");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new ValidationException("Password must contain uppercase letter");
        }
        // Add more complexity requirements
    }
}
```

## Manual Testing Supplement

Some security aspects require manual testing:

### 1. Manual Penetration Testing

- Use tools like Burp Suite, OWASP ZAP
- Test for business logic flaws
- Verify file upload restrictions
- Test API rate limiting exhaustively

### 2. Code Review

- Review authentication/authorization code
- Check for hardcoded credentials
- Verify PHI encryption at rest
- Audit third-party dependencies

### 3. Infrastructure Security

- Review firewall rules
- Verify network segmentation
- Check database access controls
- Audit cloud IAM permissions

## Best Practices

1. **Run Regularly**: Weekly automated scans, manual testing quarterly
2. **Fix Immediately**: All FAIL results must be fixed before deployment
3. **Document Changes**: Update this README when adding new tests
4. **Train Team**: Ensure developers understand security principles
5. **Stay Updated**: Monitor OWASP Top 10 and HIPAA updates

## References

- [OWASP Top 10 2021](https://owasp.org/Top10/)
- [OWASP Testing Guide](https://owasp.org/www-project-web-security-testing-guide/)
- [HIPAA Security Rule](https://www.hhs.gov/hipaa/for-professionals/security/index.html)
- [NIST Cybersecurity Framework](https://www.nist.gov/cyberframework)

---

**Last Updated**: January 2026
**Maintained By**: Security & Compliance Team
