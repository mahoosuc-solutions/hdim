# Vendor Management Policy

**HDIM - HealthData-in-Motion**

| Version | Date | Author | Status |
|---------|------|--------|--------|
| 1.0 | December 2025 | Security Team | Active |

---

## 1. Purpose

This policy establishes procedures for assessing, managing, and monitoring third-party vendors who have access to HDIM systems or data. It ensures compliance with SOC2 CC9.1/CC9.2, HIPAA Business Associate requirements, and industry best practices.

---

## 2. Scope

This policy applies to:
- All third-party vendors with access to HDIM systems
- Cloud infrastructure providers
- Software-as-a-Service (SaaS) providers
- Managed service providers
- Consultants and contractors with system access
- Partners with data integration capabilities

---

## 3. Definitions

| Term | Definition |
|------|------------|
| **Vendor** | Any external party providing services or products |
| **Critical Vendor** | Vendors with access to PHI or essential to operations |
| **Business Associate** | HIPAA-defined entity handling PHI on our behalf |
| **BAA** | Business Associate Agreement (HIPAA requirement) |
| **SLA** | Service Level Agreement |

---

## 4. Vendor Risk Classification

### 4.1 Risk Tiers

| Tier | Criteria | Examples | Review Frequency |
|------|----------|----------|------------------|
| **Tier 1 - Critical** | PHI access, essential service | AWS, PostgreSQL (managed) | Quarterly |
| **Tier 2 - High** | System access, no PHI | GitHub, Slack | Semi-annually |
| **Tier 3 - Moderate** | Limited access | Analytics tools, monitoring | Annually |
| **Tier 4 - Low** | No system/data access | Office supplies, legal | As needed |

### 4.2 Classification Criteria

| Factor | Critical (3pts) | High (2pts) | Moderate (1pt) | Low (0pts) |
|--------|-----------------|-------------|----------------|------------|
| PHI Access | Direct access | Encrypted access | Metadata only | None |
| System Access | Admin/root | Read/write | Read-only | None |
| Data Volume | All patient data | Subset | Aggregate | None |
| Availability Impact | Service outage | Degraded service | Minor impact | No impact |
| Replaceability | 6+ months | 1-6 months | 1-4 weeks | < 1 week |

**Total Score Mapping:**
- 10-15 pts: Tier 1 (Critical)
- 6-9 pts: Tier 2 (High)
- 3-5 pts: Tier 3 (Moderate)
- 0-2 pts: Tier 4 (Low)

---

## 5. Vendor Inventory

### 5.1 Current Vendor Register

| Vendor | Service | Tier | PHI Access | BAA | Last Review | Next Review |
|--------|---------|------|------------|-----|-------------|-------------|
| AWS | Cloud infrastructure | 1 - Critical | Yes (encrypted) | Yes | Dec 2025 | Mar 2026 |
| GitHub | Source code hosting | 2 - High | No | N/A | Dec 2025 | Jun 2026 |
| Slack | Team communication | 3 - Moderate | No | N/A | Dec 2025 | Dec 2026 |
| Datadog | Monitoring | 2 - High | No (metrics only) | N/A | Dec 2025 | Jun 2026 |
| Docker Hub | Container registry | 3 - Moderate | No | N/A | Dec 2025 | Dec 2026 |
| Confluent | Kafka (optional) | 1 - Critical | Possible | Yes | Dec 2025 | Mar 2026 |
| Redis Labs | Cache (optional) | 1 - Critical | Yes (encrypted) | Yes | Dec 2025 | Mar 2026 |
| HashiCorp | Vault secrets | 1 - Critical | Yes (keys) | Yes | Dec 2025 | Mar 2026 |

### 5.2 Inventory Maintenance

- New vendors added within 5 business days of engagement
- Terminated vendors removed within 5 business days
- Full inventory review: Quarterly

---

## 6. Vendor Assessment Process

### 6.1 Pre-Engagement Assessment

Before engaging any Tier 1-3 vendor:

**Step 1: Business Need**
- [ ] Define service requirements
- [ ] Identify data access needs
- [ ] Determine risk tier

**Step 2: Due Diligence**
- [ ] Security questionnaire (Tier 1-2)
- [ ] SOC2 report review (if available)
- [ ] HIPAA compliance verification (if PHI access)
- [ ] Reference checks (Tier 1)

**Step 3: Risk Assessment**
- [ ] Complete vendor risk assessment form
- [ ] Identify compensating controls
- [ ] Document residual risk

**Step 4: Contractual Requirements**
- [ ] BAA executed (if PHI access)
- [ ] Security addendum signed
- [ ] SLA defined
- [ ] Right to audit clause
- [ ] Incident notification requirements

**Step 5: Approval**
- [ ] Security Lead approval (Tier 1-2)
- [ ] Legal approval (contracts)
- [ ] Executive approval (Tier 1)

### 6.2 Security Questionnaire

For Tier 1-2 vendors, require responses to:

| Category | Questions |
|----------|-----------|
| Security Governance | SOC2, ISO27001, HITRUST certifications? |
| Access Control | MFA required? RBAC implemented? |
| Data Protection | Encryption at rest and in transit? |
| Incident Response | Breach notification timeframe? |
| Business Continuity | RTO/RPO for their service? |
| Compliance | HIPAA compliance? BAA available? |
| Subprocessors | Who else has access to our data? |

---

## 7. Contractual Requirements

### 7.1 Required Terms (All Vendors with Data Access)

| Term | Requirement |
|------|-------------|
| Confidentiality | Non-disclosure of all data |
| Data Use | Only for contracted purposes |
| Security Controls | Industry-standard protections |
| Incident Notification | 24-hour breach notification |
| Audit Rights | Annual audit capability |
| Termination | Data return/destruction |
| Insurance | Cyber liability coverage |

### 7.2 HIPAA Business Associate Agreement

Required for all Tier 1 vendors with PHI access:

- Permitted uses and disclosures
- Safeguards required
- Reporting obligations
- Subcontractor requirements
- Breach notification (24 hours)
- Return/destruction of PHI
- HHS access for audit

### 7.3 Security Addendum

Standard security requirements:

```markdown
SECURITY ADDENDUM

Vendor agrees to:
1. Maintain SOC2 Type II certification or equivalent
2. Encrypt all data at rest (AES-256) and in transit (TLS 1.2+)
3. Implement MFA for all administrative access
4. Conduct annual penetration testing
5. Notify Customer within 24 hours of any security incident
6. Restrict data access to authorized personnel only
7. Conduct background checks on personnel with data access
8. Maintain audit logs for minimum 1 year
9. Allow Customer audit upon 30 days notice
10. Return or destroy all data upon termination
```

---

## 8. Ongoing Monitoring

### 8.1 Periodic Review Schedule

| Tier | Review Frequency | Review Scope |
|------|------------------|--------------|
| Critical | Quarterly | Full review |
| High | Semi-annually | Security review |
| Moderate | Annually | Compliance check |
| Low | As needed | Contract review |

### 8.2 Review Checklist

**Quarterly Review (Tier 1):**
- [ ] SOC2 report updated?
- [ ] BAA still current?
- [ ] Any security incidents?
- [ ] SLA compliance?
- [ ] Subprocessor changes?
- [ ] Continued business need?

**Semi-Annual Review (Tier 2):**
- [ ] Security posture changes?
- [ ] Compliance status?
- [ ] Performance issues?

### 8.3 Continuous Monitoring

For Critical vendors, monitor:
- Uptime/availability
- Security bulletins
- News for breaches/incidents
- Regulatory actions
- Financial stability

---

## 9. Vendor Incidents

### 9.1 Notification Requirements

Vendors must notify HDIM:
- Security breach: 24 hours
- Data exposure: 24 hours
- Service outage: 1 hour
- Compliance change: 30 days
- Subprocessor change: 30 days

### 9.2 Incident Response

Upon vendor notification:
1. Log incident in tracking system
2. Assess impact to HDIM systems/data
3. Coordinate with vendor on remediation
4. Notify affected parties if required
5. Document lessons learned
6. Re-assess vendor risk rating

---

## 10. Vendor Termination

### 10.1 Offboarding Process

- [ ] Revoke all system access
- [ ] Request data return/destruction certificate
- [ ] Archive vendor documentation
- [ ] Update vendor inventory
- [ ] Notify internal stakeholders
- [ ] Collect any HDIM equipment/credentials

### 10.2 Data Handling

| Data Type | Requirement |
|-----------|-------------|
| PHI | Secure destruction with certificate |
| Confidential | Return or destruction |
| Configuration | Update to remove dependencies |
| Credentials | Rotate all shared secrets |

---

## 11. Roles & Responsibilities

| Role | Responsibility |
|------|----------------|
| Security Lead | Assessment, risk review, BAA approval |
| Procurement | Contract negotiation, inventory |
| Legal | Contract review, BAA execution |
| IT Operations | Access provisioning, monitoring |
| Business Owner | Vendor selection, performance |

---

## 12. Exceptions

Exceptions require:
1. Business justification
2. Compensating controls documented
3. Security Lead approval
4. Time-limited exception period
5. Executive approval (Tier 1 exceptions)

---

## 13. Metrics

| Metric | Target |
|--------|--------|
| Vendor assessments completed on time | 100% |
| Critical vendors with current BAA | 100% |
| Vendor incidents per year | <5 |
| Time to vendor access revocation | <24 hours |

---

## Appendix A: Vendor Risk Assessment Form

```markdown
VENDOR RISK ASSESSMENT

Vendor Name: ________________
Service: ____________________
Date: ______________________
Assessor: __________________

CLASSIFICATION
[ ] Tier 1 - Critical
[ ] Tier 2 - High
[ ] Tier 3 - Moderate
[ ] Tier 4 - Low

DATA ACCESS
PHI Access: [ ] Yes [ ] No
Data Types: __________________
Access Method: _______________
Encryption: [ ] At Rest [ ] In Transit

SECURITY REVIEW
SOC2 Report: [ ] Yes [ ] No  Type: I / II  Date: ______
ISO 27001: [ ] Yes [ ] No  Date: ______
HIPAA Compliance: [ ] Yes [ ] No [ ] N/A
Penetration Test: [ ] Yes [ ] No  Date: ______

RISK FACTORS
| Factor | Score (0-3) |
|--------|-------------|
| PHI Access | |
| System Access | |
| Data Volume | |
| Availability Impact | |
| Replaceability | |
| TOTAL | |

IDENTIFIED RISKS
1.
2.
3.

COMPENSATING CONTROLS
1.
2.
3.

RESIDUAL RISK: [ ] Low [ ] Medium [ ] High

RECOMMENDATION
[ ] Approve engagement
[ ] Approve with conditions
[ ] Reject

APPROVALS
Security Lead: _______________ Date: _______
Legal: ______________________ Date: _______
Executive (Tier 1): __________ Date: _______
```

---

*Document Classification: Internal*
*Next Review Date: December 2026*
