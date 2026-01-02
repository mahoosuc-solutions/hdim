# Risk Assessment Policy & Annual Assessment

**HDIM - HealthData-in-Motion**

| Version | Date | Author | Status |
|---------|------|--------|--------|
| 1.0 | December 2025 | Security Team | Active |

---

## 1. Purpose

This policy establishes the process for identifying, assessing, and managing information security risks to HDIM systems and data. It ensures compliance with SOC2 CC3.2 and HIPAA Security Rule requirements.

---

## 2. Scope

This policy covers:
- All information systems processing, storing, or transmitting data
- Third-party systems with access to HDIM data
- Physical and environmental risks
- Personnel-related risks
- Business continuity risks

---

## 3. Risk Assessment Process

### 3.1 Assessment Frequency

| Assessment Type | Frequency | Trigger |
|-----------------|-----------|---------|
| Annual Comprehensive | Yearly | January |
| Significant Change | As needed | Major system changes |
| Incident-Driven | As needed | After security incidents |
| Third-Party | Per vendor | Before engagement, annually |

### 3.2 Risk Assessment Methodology

HDIM uses a qualitative risk assessment methodology based on:
- **Likelihood**: Probability of threat exploitation
- **Impact**: Consequence if threat is realized
- **Risk Score**: Likelihood x Impact

---

## 4. Risk Scoring Matrix

### 4.1 Likelihood Scale

| Rating | Score | Description | Probability |
|--------|-------|-------------|-------------|
| Very Low | 1 | Highly unlikely | <10% |
| Low | 2 | Unlikely but possible | 10-25% |
| Medium | 3 | Possible | 25-50% |
| High | 4 | Likely | 50-75% |
| Very High | 5 | Almost certain | >75% |

### 4.2 Impact Scale

| Rating | Score | Description | Business Impact |
|--------|-------|-------------|-----------------|
| Negligible | 1 | Minimal impact | <$10K, no PHI exposure |
| Low | 2 | Minor impact | $10K-$50K, limited PHI |
| Medium | 3 | Moderate impact | $50K-$250K, moderate PHI |
| High | 4 | Significant impact | $250K-$1M, significant PHI |
| Critical | 5 | Severe impact | >$1M, major PHI breach |

### 4.3 Risk Level Matrix

|  | Negligible (1) | Low (2) | Medium (3) | High (4) | Critical (5) |
|--|----------------|---------|------------|----------|--------------|
| **Very High (5)** | 5-Medium | 10-High | 15-High | 20-Critical | 25-Critical |
| **High (4)** | 4-Low | 8-Medium | 12-High | 16-Critical | 20-Critical |
| **Medium (3)** | 3-Low | 6-Medium | 9-Medium | 12-High | 15-High |
| **Low (2)** | 2-Low | 4-Low | 6-Medium | 8-Medium | 10-High |
| **Very Low (1)** | 1-Low | 2-Low | 3-Low | 4-Low | 5-Medium |

### 4.4 Risk Treatment Thresholds

| Risk Level | Score Range | Required Action |
|------------|-------------|-----------------|
| Critical | 16-25 | Immediate mitigation required |
| High | 10-15 | Mitigation within 30 days |
| Medium | 5-9 | Mitigation within 90 days |
| Low | 1-4 | Accept or address opportunistically |

---

## 5. Annual Risk Assessment - December 2025

### 5.1 Asset Inventory

| Asset Category | Assets | Data Classification | Criticality |
|----------------|--------|---------------------|-------------|
| Patient Data | FHIR Patient resources | PHI | Critical |
| Clinical Data | Observations, Conditions, Medications | PHI | Critical |
| Quality Measures | CQL libraries, HEDIS results | Confidential | High |
| User Credentials | JWT tokens, passwords (hashed) | Confidential | Critical |
| Audit Logs | Access logs, event records | Internal | High |
| Source Code | Application repositories | Confidential | High |
| Infrastructure | AWS, Docker, Kubernetes | Internal | Critical |

### 5.2 Threat Identification

| Threat ID | Threat | Category | Threat Agent |
|-----------|--------|----------|--------------|
| T-001 | Unauthorized data access | Confidentiality | External attacker |
| T-002 | SQL injection | Integrity | External attacker |
| T-003 | Ransomware | Availability | Malicious software |
| T-004 | Insider data theft | Confidentiality | Malicious insider |
| T-005 | API credential theft | Confidentiality | External attacker |
| T-006 | Cloud misconfiguration | Confidentiality | Configuration error |
| T-007 | DDoS attack | Availability | External attacker |
| T-008 | Supply chain compromise | Integrity | Compromised vendor |
| T-009 | Social engineering | Confidentiality | External attacker |
| T-010 | Data loss/corruption | Integrity | System failure |

### 5.3 Vulnerability Assessment

| Vuln ID | Vulnerability | Affected Asset | Current Control | Control Gap |
|---------|---------------|----------------|-----------------|-------------|
| V-001 | Weak access controls | All systems | RBAC, JWT | None - mitigated |
| V-002 | Unpatched software | All services | OWASP scanning | None - automated |
| V-003 | Insufficient logging | All systems | AuditService | None - mitigated |
| V-004 | No MFA for admin | Admin accounts | Password only | Gap - MFA needed |
| V-005 | Hardcoded secrets | Source code | Gitleaks scanning | None - automated |
| V-006 | Excessive permissions | User accounts | RBAC | Gap - periodic review |
| V-007 | Insecure API endpoints | Public APIs | JWT + rate limiting | None - mitigated |
| V-008 | Backup not encrypted | Database backups | GPG available | Partial - optional |

### 5.4 Risk Register

| Risk ID | Threat + Vuln | Likelihood | Impact | Risk Score | Treatment | Owner | Target Date |
|---------|---------------|------------|--------|------------|-----------|-------|-------------|
| R-001 | T-001 + V-006 | 3 | 4 | 12-High | Mitigate: Quarterly access review | Security | Q1 2026 |
| R-002 | T-004 + V-004 | 2 | 5 | 10-High | Mitigate: Implement MFA | Engineering | Q1 2026 |
| R-003 | T-003 + V-002 | 2 | 4 | 8-Medium | Mitigate: Immutable backups | Operations | Q2 2026 |
| R-004 | T-005 + V-005 | 2 | 4 | 8-Medium | Accept: Gitleaks scanning active | Security | N/A |
| R-005 | T-007 + V-007 | 3 | 3 | 9-Medium | Mitigate: WAF/DDoS protection | Infrastructure | Q1 2026 |
| R-006 | T-008 + V-002 | 2 | 4 | 8-Medium | Mitigate: SCA scanning | Engineering | Active |
| R-007 | T-010 + V-008 | 2 | 3 | 6-Medium | Mitigate: Encrypted backups | Operations | Q1 2026 |
| R-008 | T-002 + V-001 | 1 | 4 | 4-Low | Accept: Parameterized queries | Engineering | N/A |
| R-009 | T-009 + V-004 | 2 | 3 | 6-Medium | Mitigate: Security training | Security | Q1 2026 |
| R-010 | T-006 + V-001 | 2 | 3 | 6-Medium | Mitigate: IaC scanning (Checkov) | Infrastructure | Active |

---

## 6. Risk Treatment Plans

### R-001: Excessive User Permissions (Score: 12 - High)

**Current State:** RBAC implemented but no periodic access reviews
**Target State:** Quarterly access reviews with documented attestation

**Treatment Plan:**
1. Implement automated access report generation
2. Establish quarterly review schedule
3. Document manager attestation process
4. Archive review evidence for audit

**Timeline:** Q1 2026
**Owner:** Security Team
**Budget:** $0 (process change)

---

### R-002: No MFA for Admin Accounts (Score: 10 - High)

**Current State:** Password-only authentication
**Target State:** MFA required for all admin access

**Treatment Plan:**
1. Select MFA solution (TOTP or WebAuthn)
2. Configure Keycloak/Auth service for MFA
3. Enroll all admin users
4. Enforce MFA policy

**Timeline:** Q1 2026
**Owner:** Engineering Team
**Budget:** $500/month (if using external provider)

---

### R-005: DDoS Vulnerability (Score: 9 - Medium)

**Current State:** Basic rate limiting on API gateway
**Target State:** WAF and DDoS protection enabled

**Treatment Plan:**
1. Enable AWS Shield Standard (free)
2. Configure AWS WAF rules
3. Set up CloudFront for API endpoints
4. Document incident response for DDoS

**Timeline:** Q1 2026
**Owner:** Infrastructure Team
**Budget:** $500-2000/month

---

## 7. Risk Acceptance

The following risks are accepted with documented rationale:

| Risk ID | Risk Description | Residual Score | Rationale | Approved By | Date |
|---------|------------------|----------------|-----------|-------------|------|
| R-004 | Hardcoded secrets | 8-Medium | Gitleaks scanning prevents commits | Security Lead | Dec 2025 |
| R-008 | SQL Injection | 4-Low | All queries use parameterized statements | Security Lead | Dec 2025 |

---

## 8. Roles & Responsibilities

| Role | Responsibility |
|------|----------------|
| Security Lead | Conduct assessments, maintain risk register, report to management |
| Engineering Team | Implement technical controls, remediate vulnerabilities |
| Operations Team | Implement operational controls, manage infrastructure risks |
| Executive Team | Approve risk treatments, accept residual risks |

---

## 9. Assessment Approval

**Annual Risk Assessment Approval**

This risk assessment has been reviewed and approved.

| Role | Name | Signature | Date |
|------|------|-----------|------|
| Security Lead | | | |
| CTO/VP Engineering | | | |
| CEO/Executive Sponsor | | | |

---

## 10. Next Assessment

**Scheduled:** January 2026
**Triggers for interim assessment:**
- Major security incident
- Significant system changes
- New regulatory requirements
- Material business changes

---

*Document Classification: Confidential*
*Next Review Date: January 2026*
