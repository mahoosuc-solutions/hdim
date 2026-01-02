# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.4.x   | :white_check_mark: |
| 1.3.x   | :white_check_mark: |
| < 1.3   | :x:                |

## Reporting a Vulnerability

We take security seriously at HDIM. If you discover a security vulnerability, please report it responsibly.

### How to Report

**DO NOT** open a public GitHub issue for security vulnerabilities.

Instead, please report vulnerabilities via one of these methods:

1. **Email:** security@healthdata-in-motion.com
2. **GitHub Security Advisories:** [Report a vulnerability](../../security/advisories/new)

### What to Include

Please provide:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)
- Your contact information

### Response Timeline

| Phase | Timeframe |
|-------|-----------|
| Initial Response | 24-48 hours |
| Triage & Assessment | 3-5 business days |
| Fix Development | Varies by severity |
| Disclosure | Coordinated with reporter |

### Severity Ratings

| Severity | Response Time | Examples |
|----------|---------------|----------|
| Critical | 24 hours | RCE, auth bypass, data breach |
| High | 72 hours | Privilege escalation, SQL injection |
| Medium | 1 week | XSS, CSRF, information disclosure |
| Low | 2 weeks | Minor issues, hardening |

### Safe Harbor

We support responsible disclosure. If you:
- Make a good faith effort to avoid privacy violations
- Give us reasonable time to fix issues
- Don't exploit the vulnerability beyond testing

We will:
- Not pursue legal action
- Work with you to understand and resolve the issue
- Credit you in our security acknowledgments (if desired)

## Security Measures

### Current Security Controls

- **Authentication:** JWT-based with HS512 signing
- **Authorization:** Role-based access control (RBAC)
- **Encryption:** AES-256-GCM for data at rest, TLS 1.3 in transit
- **Multi-Tenancy:** Tenant isolation at application and database levels
- **Audit Logging:** Comprehensive security event logging
- **Rate Limiting:** Protection against brute force attacks

### Automated Security Scanning

We use the following tools in our CI/CD pipeline:
- **Trivy:** Container vulnerability scanning
- **OWASP Dependency Check:** Dependency vulnerability scanning
- **CodeQL:** Static application security testing
- **Gitleaks:** Secret detection
- **Checkov:** Infrastructure as code scanning
- **Dependabot:** Automated dependency updates

### Compliance

HDIM is designed for healthcare environments and aims to comply with:
- HIPAA Security Rule
- SOC2 Type I/II
- OWASP Top 10

## Security Updates

Security updates are released as:
- **Patch releases:** For security fixes (e.g., 1.4.1)
- **Security advisories:** Published in GitHub Security Advisories

Subscribe to releases to be notified of security updates.

## Contact

- **Security Team:** security@healthdata-in-motion.com
- **General:** support@healthdata-in-motion.com
