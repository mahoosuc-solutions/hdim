# SOC2 Type I Preparation Guide

## HDIM - HealthData-in-Motion

---

## Executive Summary

This document outlines the preparation plan for achieving **SOC2 Type I** certification for HDIM. SOC2 Type I validates that security controls are properly designed and in place at a specific point in time.

**Target Timeline:** 8-12 weeks
**Estimated Cost:** $15,000-25,000 (auditor fees)
**Trust Service Criteria:** Security (required) + Confidentiality + Availability

---

## SOC2 Trust Service Criteria

### Criteria We're Pursuing

| Criteria | Required | Status | Priority |
|----------|:--------:|:------:|:--------:|
| **Security** | Yes | In Progress | P0 |
| **Confidentiality** | No | In Progress | P1 |
| **Availability** | No | In Progress | P1 |
| Processing Integrity | No | Not Started | P2 |
| Privacy | No | Not Started | P2 |

---

## Current State Assessment

### Security Controls Already Implemented

Based on our security architecture review, HDIM has the following controls in place:

#### Access Control (CC6.1-6.8)

| Control | Status | Evidence |
|---------|:------:|----------|
| Role-based access control | ✅ Done | JwtAuthenticationFilter, @PreAuthorize annotations |
| Multi-tenant isolation | ✅ Done | TenantAccessFilter, tenant-scoped queries |
| Password policies | ⚠️ Partial | Need documented policy |
| MFA support | ❌ Gap | Need to implement |
| Session management | ✅ Done | JWT tokens, 15-min expiry |
| Access logging | ✅ Done | AuditLoggingInterceptor |

#### System Operations (CC7.1-7.5)

| Control | Status | Evidence |
|---------|:------:|----------|
| Infrastructure monitoring | ⚠️ Partial | Health checks exist, need alerting |
| Incident detection | ⚠️ Partial | Logging exists, need SIEM |
| Vulnerability management | ❌ Gap | Need scanning tools |
| Change management | ⚠️ Partial | Git history, need formal process |
| Backup procedures | ⚠️ Partial | Need documented procedures |

#### Risk Management (CC3.1-3.4)

| Control | Status | Evidence |
|---------|:------:|----------|
| Risk assessment | ❌ Gap | Need formal assessment |
| Risk mitigation | ⚠️ Partial | Security controls exist |
| Vendor management | ❌ Gap | Need vendor inventory |

#### Logical & Physical Access (CC6.1-6.8)

| Control | Status | Evidence |
|---------|:------:|----------|
| Encryption at rest | ✅ Done | AES-256-GCM, PHIEncryption |
| Encryption in transit | ✅ Done | TLS 1.3, WSS |
| Key management | ⚠️ Partial | Need formal rotation policy |
| Network segmentation | ✅ Done | Docker networks, zone isolation |

---

## Gap Analysis & Remediation Plan

### Critical Gaps (Must Fix)

#### 1. Multi-Factor Authentication (MFA)
**Gap:** No MFA implementation
**Remediation:** Implement TOTP-based MFA
**Effort:** 3-5 days
**Priority:** P0

```
Files to create/modify:
- backend/modules/services/gateway-service/src/main/java/com/healthdata/gateway/auth/MfaService.java
- backend/modules/services/gateway-service/src/main/java/com/healthdata/gateway/auth/TotpVerifier.java
```

#### 2. Vulnerability Scanning
**Gap:** No automated security scanning
**Remediation:** Integrate Snyk/Trivy in CI/CD
**Effort:** 1-2 days
**Priority:** P0

```yaml
# Add to GitHub Actions workflow
- name: Run Trivy vulnerability scanner
  uses: aquasecurity/trivy-action@master
  with:
    image-ref: 'hdim/clinical-portal:latest'
    severity: 'CRITICAL,HIGH'
```

#### 3. Formal Incident Response Plan
**Gap:** No documented IR plan
**Remediation:** Create IR policy and runbooks
**Effort:** 2-3 days
**Priority:** P0

#### 4. Risk Assessment Documentation
**Gap:** No formal risk register
**Remediation:** Create risk assessment document
**Effort:** 2-3 days
**Priority:** P0

---

### High Priority Gaps

#### 5. Security Awareness Training
**Gap:** No training program
**Remediation:** Document training requirements
**Effort:** 1 day
**Priority:** P1

#### 6. Vendor Management Program
**Gap:** No vendor inventory or assessments
**Remediation:** Create vendor list with risk ratings
**Effort:** 1-2 days
**Priority:** P1

#### 7. Business Continuity Plan
**Gap:** No documented BCP
**Remediation:** Create BCP document
**Effort:** 2-3 days
**Priority:** P1

#### 8. Data Retention Policy
**Gap:** No formal retention policy
**Remediation:** Document retention requirements
**Effort:** 1 day
**Priority:** P1

---

## Required Policy Documents

### Policies to Create

| Policy | Status | Template |
|--------|:------:|:--------:|
| Information Security Policy | ❌ Needed | Below |
| Access Control Policy | ❌ Needed | Below |
| Incident Response Policy | ❌ Needed | Below |
| Change Management Policy | ❌ Needed | Below |
| Data Classification Policy | ❌ Needed | Below |
| Acceptable Use Policy | ❌ Needed | Below |
| Vendor Management Policy | ❌ Needed | Below |
| Business Continuity Policy | ❌ Needed | Below |
| Password Policy | ❌ Needed | Below |
| Encryption Policy | ❌ Needed | Below |

---

## SOC2 Evidence Requirements

### Evidence We Can Provide Today

| Evidence Type | Source | Location |
|---------------|--------|----------|
| Access control configuration | Code | JwtAuthenticationFilter.java |
| Role definitions | Code | @PreAuthorize annotations |
| Encryption implementation | Code | PHIEncryption.java |
| Audit logging | Code | AuditLoggingInterceptor.java |
| Network architecture | Docs | security/index.md |
| API authentication | Docs | api/index.md |
| Multi-tenancy isolation | Code | TenantAccessFilter.java |
| Health monitoring | Config | Docker healthchecks |

### Evidence We Need to Create

| Evidence Type | Action Required |
|---------------|-----------------|
| Security policies | Create policy documents |
| Risk assessment | Conduct formal assessment |
| Penetration test report | Hire third-party |
| Vulnerability scans | Implement scanning |
| Training records | Document training program |
| Incident response tests | Conduct tabletop exercise |
| Backup test results | Test and document |
| Vendor assessments | Assess key vendors |

---

## Implementation Roadmap

### Week 1-2: Documentation

| Task | Owner | Status |
|------|-------|:------:|
| Create Information Security Policy | - | ⬜ |
| Create Access Control Policy | - | ⬜ |
| Create Incident Response Policy | - | ⬜ |
| Create Change Management Policy | - | ⬜ |
| Create Data Classification Policy | - | ⬜ |
| Document current security controls | - | ⬜ |

### Week 3-4: Technical Controls

| Task | Owner | Status |
|------|-------|:------:|
| Implement MFA | - | ⬜ |
| Add vulnerability scanning to CI/CD | - | ⬜ |
| Configure centralized logging | - | ⬜ |
| Set up security alerting | - | ⬜ |
| Document backup procedures | - | ⬜ |

### Week 5-6: Risk & Vendor Management

| Task | Owner | Status |
|------|-------|:------:|
| Conduct risk assessment | - | ⬜ |
| Create risk register | - | ⬜ |
| Inventory vendors | - | ⬜ |
| Assess critical vendors | - | ⬜ |
| Create BCP | - | ⬜ |

### Week 7-8: Testing & Validation

| Task | Owner | Status |
|------|-------|:------:|
| Conduct penetration test | - | ⬜ |
| Test incident response | - | ⬜ |
| Test backup restoration | - | ⬜ |
| Review all evidence | - | ⬜ |
| Internal audit | - | ⬜ |

### Week 9-12: Audit

| Task | Owner | Status |
|------|-------|:------:|
| Select auditor | - | ⬜ |
| Auditor kickoff | - | ⬜ |
| Evidence submission | - | ⬜ |
| Auditor interviews | - | ⬜ |
| Remediation (if needed) | - | ⬜ |
| Final report | - | ⬜ |

---

## Auditor Selection

### Recommended SOC2 Auditors (Healthcare Experience)

| Auditor | Estimated Cost | Timeline | Notes |
|---------|----------------|----------|-------|
| Drata + Partner | $15-20K | 8-10 weeks | Automated evidence collection |
| Vanta + Partner | $15-20K | 8-10 weeks | Continuous monitoring |
| Secureframe + Partner | $12-18K | 8-10 weeks | Good for startups |
| Johanson Group | $20-30K | 10-12 weeks | Healthcare specialty |
| Schellman | $25-35K | 10-12 weeks | Large firm, thorough |

### Selection Criteria

1. Healthcare/HIPAA experience
2. Startup-friendly pricing
3. Automated evidence collection platform
4. Quick turnaround
5. Good communication

---

## Cost Estimate

### One-Time Costs

| Item | Low | High |
|------|----:|-----:|
| Auditor fees | $15,000 | $30,000 |
| Penetration test | $5,000 | $15,000 |
| Compliance platform (annual) | $5,000 | $15,000 |
| Policy templates (optional) | $0 | $2,000 |
| **Total** | **$25,000** | **$62,000** |

### Recommended Budget: $30,000

---

## Ongoing Compliance (Post-Type I)

### SOC2 Type II Requirements

After Type I, plan for Type II which requires:
- 6-12 month observation period
- Continuous evidence collection
- Quarterly access reviews
- Annual penetration testing
- Ongoing training

### Annual Costs (Type II)

| Item | Annual Cost |
|------|------------:|
| Auditor fees | $15-25K |
| Compliance platform | $5-15K |
| Penetration testing | $5-10K |
| Training | $1-2K |
| **Total** | **$26-52K** |

---

## Quick Wins (Do This Week)

### 1. Enable Security Headers
Add to nginx.conf:
```nginx
add_header X-Frame-Options "SAMEORIGIN" always;
add_header X-Content-Type-Options "nosniff" always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Content-Security-Policy "default-src 'self'" always;
```

### 2. Add Dependency Scanning
```yaml
# .github/workflows/security.yml
name: Security Scan
on: [push, pull_request]
jobs:
  scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run Snyk
        uses: snyk/actions/gradle@master
        env:
          SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
```

### 3. Document Password Requirements
Current implementation requires:
- Minimum 8 characters
- Add: uppercase, lowercase, number, special char

### 4. Enable Audit Log Retention
Configure log retention to 1 year minimum.

---

## Contact & Resources

### Compliance Platforms
- [Drata](https://drata.com) - Automated compliance
- [Vanta](https://vanta.com) - Security monitoring
- [Secureframe](https://secureframe.com) - Startup-friendly

### Auditors
- [Johanson Group](https://johansonllp.com) - Healthcare focus
- [Schellman](https://schellman.com) - Large enterprise

### Templates
- [SANS Policy Templates](https://www.sans.org/information-security-policy/)
- [CIS Controls](https://www.cisecurity.org/controls)

---

## Appendix A: Control Mapping

### CC1: Control Environment

| Control | HDIM Implementation |
|---------|---------------------|
| CC1.1 - Integrity and ethical values | Need: Code of conduct |
| CC1.2 - Board oversight | Need: Document governance |
| CC1.3 - Management structure | Need: Org chart |
| CC1.4 - Commitment to competence | Need: Job descriptions |
| CC1.5 - Accountability | Need: Document roles |

### CC2: Communication and Information

| Control | HDIM Implementation |
|---------|---------------------|
| CC2.1 - Information quality | ✅ Data validation |
| CC2.2 - Internal communication | Need: Policy distribution |
| CC2.3 - External communication | Need: Customer notification policy |

### CC3: Risk Assessment

| Control | HDIM Implementation |
|---------|---------------------|
| CC3.1 - Risk objectives | Need: Risk assessment |
| CC3.2 - Risk identification | Need: Risk register |
| CC3.3 - Fraud risk | Need: Fraud controls |
| CC3.4 - Change analysis | ⚠️ Partial: Git history |

### CC4: Monitoring Activities

| Control | HDIM Implementation |
|---------|---------------------|
| CC4.1 - Ongoing monitoring | ✅ Health checks, logging |
| CC4.2 - Deficiency evaluation | Need: Issue tracking process |

### CC5: Control Activities

| Control | HDIM Implementation |
|---------|---------------------|
| CC5.1 - Control selection | ✅ Security architecture |
| CC5.2 - Technology controls | ✅ Encryption, auth, access |
| CC5.3 - Policy deployment | Need: Policy documents |

### CC6: Logical and Physical Access

| Control | HDIM Implementation |
|---------|---------------------|
| CC6.1 - Access security | ✅ JWT, RBAC |
| CC6.2 - Access provisioning | ⚠️ Need: User provisioning process |
| CC6.3 - Access removal | ⚠️ Need: Offboarding process |
| CC6.4 - Access review | ❌ Need: Quarterly reviews |
| CC6.5 - Physical access | N/A (Cloud) |
| CC6.6 - Threat management | ⚠️ Need: Vulnerability scanning |
| CC6.7 - Transmission security | ✅ TLS, encryption |
| CC6.8 - Malware prevention | ⚠️ Need: Endpoint protection |

### CC7: System Operations

| Control | HDIM Implementation |
|---------|---------------------|
| CC7.1 - Infrastructure detection | ✅ Health checks |
| CC7.2 - Security monitoring | ⚠️ Logging exists, need SIEM |
| CC7.3 - Change evaluation | ⚠️ Need: Formal CAB |
| CC7.4 - Change management | ⚠️ Git + PR, need docs |
| CC7.5 - Incident management | ❌ Need: IR plan |

### CC8: Change Management

| Control | HDIM Implementation |
|---------|---------------------|
| CC8.1 - Change authorization | ⚠️ PR reviews, need docs |

### CC9: Risk Mitigation

| Control | HDIM Implementation |
|---------|---------------------|
| CC9.1 - Vendor risk | ❌ Need: Vendor management |
| CC9.2 - Subservice organizations | ❌ Need: Vendor inventory |

---

*Document Version: 1.0*
*Last Updated: December 2025*
*Next Review: Before audit engagement*
