# Risk Assessment

**HDIM - HealthData-in-Motion**

| Version | Date | Assessor | Status |
|---------|------|----------|--------|
| 1.0 | December 2025 | Security Team | Active |

---

## 1. Executive Summary

This risk assessment identifies, analyzes, and prioritizes information security risks to HDIM's systems and data. It provides the foundation for risk-based security decisions and control implementation.

**Overall Risk Level:** MODERATE (Improving)

**Key Findings:**
- ~~3~~ 1 High risk identified (requiring immediate attention)
- ~~8~~ 10 Medium risks identified (2 reduced from Critical)
- 12 Low risks identified (acceptable with monitoring)

**Recently Resolved:**
- ✅ RISK-001: MFA implemented (commit c6c2e9d)
- ✅ RISK-002: Vulnerability scanning added to CI/CD (commit 61e4388)

---

## 2. Methodology

### 2.1 Risk Calculation

```
Risk = Likelihood × Impact
```

### 2.2 Likelihood Scale

| Level | Score | Description |
|-------|:-----:|-------------|
| Very High | 5 | Almost certain (>90%) |
| High | 4 | Likely (60-90%) |
| Medium | 3 | Possible (30-60%) |
| Low | 2 | Unlikely (10-30%) |
| Very Low | 1 | Rare (<10%) |

### 2.3 Impact Scale

| Level | Score | Description |
|-------|:-----:|-------------|
| Critical | 5 | Business failure, major breach, regulatory action |
| High | 4 | Significant financial loss, data breach |
| Medium | 3 | Moderate disruption, limited exposure |
| Low | 2 | Minor disruption, minimal impact |
| Minimal | 1 | Negligible impact |

### 2.4 Risk Levels

| Score | Level | Action Required |
|:-----:|-------|-----------------|
| 15-25 | Critical | Immediate remediation |
| 10-14 | High | Remediate within 30 days |
| 5-9 | Medium | Remediate within 90 days |
| 1-4 | Low | Accept or monitor |

---

## 3. Risk Register

### 3.1 High Risks

#### RISK-001: Lack of Multi-Factor Authentication

| Attribute | Value |
|-----------|-------|
| **Category** | Access Control |
| **Threat** | Account compromise via stolen credentials |
| **Vulnerability** | Single-factor authentication |
| **Likelihood** | Low (2) |
| **Impact** | High (4) |
| **Risk Score** | **8 (Medium)** (Residual after MFA) |
| **Current Controls** | Strong passwords, account lockout, **TOTP-based MFA** |
| **Recommended Controls** | ~~Implement TOTP-based MFA~~ **IMPLEMENTED** |
| **Owner** | Engineering |
| **Target Date** | 30 days |
| **Status** | **RESOLVED** - MFA implemented (commit c6c2e9d) |

#### RISK-002: No Automated Vulnerability Scanning

| Attribute | Value |
|-----------|-------|
| **Category** | System Operations |
| **Threat** | Exploitation of known vulnerabilities |
| **Vulnerability** | No continuous vulnerability detection |
| **Likelihood** | Low (2) |
| **Impact** | High (4) |
| **Risk Score** | **8 (Medium)** (Residual after scanning) |
| **Current Controls** | **Trivy container scanning, OWASP Dependency Check, CodeQL SAST, Gitleaks secret detection, Dependabot automated updates** |
| **Recommended Controls** | ~~Implement Snyk/Trivy in CI/CD~~ **IMPLEMENTED** |
| **Owner** | Engineering |
| **Target Date** | 14 days |
| **Status** | **RESOLVED** - Security scanning added to CI/CD (commit 61e4388) |

#### RISK-003: No Formal Incident Response Testing

| Attribute | Value |
|-----------|-------|
| **Category** | Incident Response |
| **Threat** | Ineffective response to security incidents |
| **Vulnerability** | Untested IR procedures |
| **Likelihood** | Medium (3) |
| **Impact** | High (4) |
| **Risk Score** | **12 (High)** |
| **Current Controls** | IR policy documented |
| **Recommended Controls** | Conduct tabletop exercises quarterly |
| **Owner** | Security |
| **Target Date** | 45 days |
| **Status** | Open |

---

### 3.2 Medium Risks

#### RISK-004: Limited Security Monitoring/SIEM

| Attribute | Value |
|-----------|-------|
| **Category** | Detection |
| **Threat** | Delayed detection of security events |
| **Vulnerability** | No centralized security monitoring |
| **Likelihood** | Medium (3) |
| **Impact** | Medium (3) |
| **Risk Score** | **9 (Medium)** |
| **Current Controls** | Application logging, health checks |
| **Recommended Controls** | Implement log aggregation with alerting |
| **Owner** | Operations |
| **Target Date** | 60 days |
| **Status** | Open |

#### RISK-005: Incomplete Vendor Risk Management

| Attribute | Value |
|-----------|-------|
| **Category** | Third-Party |
| **Threat** | Supply chain compromise |
| **Vulnerability** | No formal vendor assessment process |
| **Likelihood** | Low (2) |
| **Impact** | High (4) |
| **Risk Score** | **8 (Medium)** |
| **Current Controls** | Ad-hoc vendor reviews |
| **Recommended Controls** | Implement vendor management program |
| **Owner** | Security |
| **Target Date** | 60 days |
| **Status** | Open |

#### RISK-006: No Formal Backup Testing

| Attribute | Value |
|-----------|-------|
| **Category** | Business Continuity |
| **Threat** | Data loss, inability to recover |
| **Vulnerability** | Backups not regularly tested |
| **Likelihood** | Low (2) |
| **Impact** | Critical (5) |
| **Risk Score** | **10 (High)** |
| **Current Controls** | Automated backups configured |
| **Recommended Controls** | Quarterly backup restoration tests |
| **Owner** | Operations |
| **Target Date** | 30 days |
| **Status** | Open |

#### RISK-007: No Formal Access Reviews

| Attribute | Value |
|-----------|-------|
| **Category** | Access Control |
| **Threat** | Excessive access accumulation |
| **Vulnerability** | Access not regularly reviewed |
| **Likelihood** | Medium (3) |
| **Impact** | Medium (3) |
| **Risk Score** | **9 (Medium)** |
| **Current Controls** | Role-based access |
| **Recommended Controls** | Quarterly access reviews |
| **Owner** | Security |
| **Target Date** | 45 days |
| **Status** | Open |

#### RISK-008: Insider Threat

| Attribute | Value |
|-----------|-------|
| **Category** | Personnel |
| **Threat** | Malicious or negligent employee actions |
| **Vulnerability** | Limited monitoring of internal access |
| **Likelihood** | Low (2) |
| **Impact** | High (4) |
| **Risk Score** | **8 (Medium)** |
| **Current Controls** | Access logging, RBAC |
| **Recommended Controls** | Enhanced monitoring, DLP |
| **Owner** | Security |
| **Target Date** | 90 days |
| **Status** | Open |

#### RISK-009: Phishing Attacks

| Attribute | Value |
|-----------|-------|
| **Category** | Social Engineering |
| **Threat** | Credential theft via phishing |
| **Vulnerability** | Human susceptibility |
| **Likelihood** | High (4) |
| **Impact** | Medium (3) |
| **Risk Score** | **12 (High)** |
| **Current Controls** | Security awareness (informal) |
| **Recommended Controls** | Formal training, phishing simulations |
| **Owner** | Security |
| **Target Date** | 45 days |
| **Status** | Open |

#### RISK-010: API Security

| Attribute | Value |
|-----------|-------|
| **Category** | Application Security |
| **Threat** | API abuse, injection attacks |
| **Vulnerability** | Potential API vulnerabilities |
| **Likelihood** | Medium (3) |
| **Impact** | Medium (3) |
| **Risk Score** | **9 (Medium)** |
| **Current Controls** | Input validation, authentication |
| **Recommended Controls** | API security testing, rate limiting |
| **Owner** | Engineering |
| **Target Date** | 60 days |
| **Status** | Open |

#### RISK-011: Key Management

| Attribute | Value |
|-----------|-------|
| **Category** | Cryptography |
| **Threat** | Key compromise or loss |
| **Vulnerability** | No formal key rotation |
| **Likelihood** | Low (2) |
| **Impact** | High (4) |
| **Risk Score** | **8 (Medium)** |
| **Current Controls** | Secure key storage |
| **Recommended Controls** | Automated key rotation, HSM |
| **Owner** | Security |
| **Target Date** | 90 days |
| **Status** | Open |

---

### 3.3 Low Risks (Summary)

| Risk ID | Description | Score | Status |
|---------|-------------|:-----:|--------|
| RISK-012 | DDoS attack | 4 | Accept (CDN protection) |
| RISK-013 | Data center failure | 4 | Accept (multi-AZ) |
| RISK-014 | Expired certificates | 3 | Monitor (auto-renewal) |
| RISK-015 | Open source vulnerabilities | 4 | Monitor (Dependabot) |
| RISK-016 | Compliance documentation gaps | 4 | In progress |
| RISK-017 | Employee device theft | 3 | Accept (encryption) |

---

## 4. Risk Treatment Summary

### 4.1 Treatment Options

| Option | Description |
|--------|-------------|
| **Mitigate** | Implement controls to reduce risk |
| **Transfer** | Transfer risk via insurance/contracts |
| **Accept** | Accept risk with documentation |
| **Avoid** | Eliminate the risk source |

### 4.2 Treatment Decisions

| Risk ID | Treatment | Rationale |
|---------|-----------|-----------|
| RISK-001 | Mitigate | Implement MFA |
| RISK-002 | Mitigate | Add vulnerability scanning |
| RISK-003 | Mitigate | Test IR procedures |
| RISK-004 | Mitigate | Implement SIEM |
| RISK-005 | Mitigate | Vendor management program |
| RISK-006 | Mitigate | Test backups |
| RISK-012 | Accept | CDN provides protection |
| RISK-013 | Accept | Multi-AZ deployment |

---

## 5. Remediation Plan

### 5.1 Priority 1 (30 Days)

| Risk | Action | Owner | Due Date |
|------|--------|-------|----------|
| RISK-001 | Implement MFA | Engineering | Week 4 |
| RISK-002 | Add Trivy to CI/CD | Engineering | Week 2 |
| RISK-006 | Test backup restoration | Operations | Week 4 |

### 5.2 Priority 2 (60 Days)

| Risk | Action | Owner | Due Date |
|------|--------|-------|----------|
| RISK-003 | Conduct tabletop exercise | Security | Week 6 |
| RISK-005 | Create vendor inventory | Security | Week 8 |
| RISK-007 | Implement access reviews | Security | Week 6 |
| RISK-009 | Launch security training | Security | Week 6 |

### 5.3 Priority 3 (90 Days)

| Risk | Action | Owner | Due Date |
|------|--------|-------|----------|
| RISK-004 | Deploy log aggregation | Operations | Week 10 |
| RISK-008 | Enhanced monitoring | Security | Week 12 |
| RISK-010 | API security testing | Engineering | Week 10 |
| RISK-011 | Key rotation automation | Security | Week 12 |

---

## 6. Risk Metrics

### 6.1 Current State

| Category | Critical | High | Medium | Low |
|----------|:--------:|:----:|:------:|:---:|
| Access Control | 1 | 0 | 1 | 0 |
| System Operations | 1 | 0 | 1 | 2 |
| Incident Response | 0 | 1 | 0 | 0 |
| Third-Party | 0 | 0 | 1 | 0 |
| Application | 0 | 0 | 1 | 1 |
| Personnel | 0 | 1 | 1 | 1 |
| **Total** | **2** | **2** | **5** | **4** |

### 6.2 Target State (90 Days)

| Category | Critical | High | Medium | Low |
|----------|:--------:|:----:|:------:|:---:|
| All | 0 | 0 | 3 | 10 |

---

## 7. Risk Acceptance

The following risks are formally accepted:

| Risk | Justification | Accepted By | Date |
|------|---------------|-------------|------|
| RISK-012 | CDN provides DDoS protection | | |
| RISK-013 | Multi-AZ deployment provides redundancy | | |
| RISK-017 | Full disk encryption mitigates device theft | | |

---

## 8. Review Schedule

| Activity | Frequency |
|----------|-----------|
| Risk assessment update | Quarterly |
| Risk register review | Monthly |
| Control effectiveness | Quarterly |
| Full reassessment | Annually |

---

## Appendix A: Asset Inventory

| Asset | Classification | Owner | Location |
|-------|---------------|-------|----------|
| Production Database | Confidential | Operations | AWS |
| Application Servers | Internal | Engineering | AWS |
| Source Code | Confidential | Engineering | GitHub |
| Customer PHI | Confidential | Operations | AWS |
| Encryption Keys | Confidential | Security | AWS KMS |

---

## Appendix B: Threat Sources

| Source | Motivation | Capability |
|--------|------------|------------|
| Cybercriminals | Financial gain | Medium-High |
| Nation States | Espionage, disruption | High |
| Hacktivists | Ideology | Low-Medium |
| Insiders | Financial, revenge | Medium |
| Competitors | Business advantage | Low |

---

*Document Classification: Confidential*
*Next Review Date: March 2026*
