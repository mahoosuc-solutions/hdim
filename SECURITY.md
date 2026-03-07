# Security Policy

## Reporting a Vulnerability

HealthData-in-Motion (HDIM) takes security seriously, especially given our role in healthcare data processing. We appreciate responsible disclosure of security vulnerabilities.

### How to Report

**Email:** security@gratefulhouse.com

**GitHub:** Use [GitHub Security Advisories](https://github.com/grateful-house/hdim/security/advisories/new) to report vulnerabilities privately.

**Do NOT** file public GitHub issues for security vulnerabilities.

### What to Include

- Description of the vulnerability
- Steps to reproduce
- Affected version(s) and component(s)
- Potential impact assessment
- Any suggested remediation (optional)

### Response Timeline

| Stage | SLA |
|-------|-----|
| Acknowledgment | 48 hours |
| Triage and severity assessment | 7 calendar days |
| Fix for Critical/High severity | 30 calendar days |
| Fix for Medium/Low severity | 90 calendar days |
| Public disclosure (coordinated) | After fix is released |

### Scope

The following components are in scope for security reports:

- All HDIM backend services (Java/Spring Boot)
- MCP Edge sidecars (Node.js)
- Clinical Portal (Angular)
- API Gateway configurations
- Docker Compose and deployment configurations
- Authentication and authorization flows
- PHI handling and encryption

### Out of Scope

- Third-party dependencies (report to upstream maintainers)
- Social engineering attacks
- Denial of service attacks
- Issues in development/test configurations only

### Safe Harbor

We support safe harbor for security researchers who:

- Make a good faith effort to avoid privacy violations, data destruction, and service disruption
- Only interact with accounts you own or with explicit permission
- Do not exploit a vulnerability beyond what is necessary to confirm it exists
- Report vulnerabilities promptly and do not disclose publicly before a fix is available

We will not pursue legal action against researchers who follow this policy, subject
to compliance with applicable law and this policy.

This safe harbor does not apply to bad-faith conduct, extortion, privacy
violations, data destruction, service disruption, or unauthorized access beyond
what is necessary to validate a reported issue.

### Severity Classification

We use the Common Vulnerability Scoring System (CVSS v3.1) to assess severity:

| CVSS Score | Severity | Examples |
|------------|----------|----------|
| 9.0 - 10.0 | Critical | PHI data breach, authentication bypass, remote code execution |
| 7.0 - 8.9 | High | Privilege escalation, tenant isolation bypass |
| 4.0 - 6.9 | Medium | Information disclosure (non-PHI), CSRF |
| 0.1 - 3.9 | Low | Minor information leak, verbose error messages |

### Healthcare-Specific Considerations

Given HDIM processes Protected Health Information (PHI) under HIPAA:

- Any vulnerability that could expose PHI is treated as **Critical** regardless of CVSS score
- Tenant isolation bypasses are treated as **Critical**
- Audit log tampering is treated as **High**
- We coordinate with affected healthcare organizations before public disclosure

### Recognition

We maintain a security acknowledgments page for researchers who responsibly disclose vulnerabilities. If you would like to be credited, please let us know in your report.

### Contact

- **Security reports:** security@gratefulhouse.com
- **General inquiries:** info@gratefulhouse.com
- **PGP key:** Available upon request

---

*This policy is based on recommendations from CERT/CC and GitHub's security advisory workflow.*
*Last updated: March 7, 2026*
