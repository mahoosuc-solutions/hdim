# SOC 2 Type II Readiness Checklist

> Comprehensive assessment of HDIM's readiness for SOC 2 Type II certification.

---

## Executive Summary

**Target Certification:** SOC 2 Type II
**Target Date:** Q2 2025
**Auditor:** [To be selected - recommendations below]
**Estimated Cost:** $25,000-$40,000

### Current Status Overview

| Trust Service Criteria | Status | Gap Level |
|------------------------|--------|-----------|
| Security (CC) | Mostly Ready | Low |
| Availability (A) | Mostly Ready | Low |
| Confidentiality (C) | Ready | Minimal |
| Processing Integrity (PI) | Mostly Ready | Low |
| Privacy (P) | Partially Ready | Medium |

---

## Trust Service Criteria: Security (CC)

### CC1 - Control Environment

#### CC1.1 - Management Philosophy and Operating Style

| Control | Description | Current State | Action Needed |
|---------|-------------|---------------|---------------|
| CC1.1.1 | Defined organizational structure | ✅ Implemented | None |
| CC1.1.2 | Board oversight (or equivalent) | ⚠️ Partial | Document governance structure |
| CC1.1.3 | Management commitment to integrity | ✅ Implemented | Document in policy |
| CC1.1.4 | Defined authorities and responsibilities | ⚠️ Partial | Create RACI matrix |

**Evidence Required:**
- [ ] Organization chart with reporting lines
- [ ] Board/leadership meeting minutes showing security oversight
- [ ] Written statement of management philosophy
- [ ] Role descriptions with security responsibilities

---

#### CC1.2 - Commitment to Competence

| Control | Description | Current State | Action Needed |
|---------|-------------|---------------|---------------|
| CC1.2.1 | Competency requirements defined | ⚠️ Partial | Document for security roles |
| CC1.2.2 | Training and development programs | ⚠️ Partial | Implement security training |
| CC1.2.3 | Security awareness program | ❌ Not Started | Develop and implement |

**Evidence Required:**
- [ ] Job descriptions with competency requirements
- [ ] Training records for security team
- [ ] Security awareness training completion records
- [ ] Annual training calendar

**Action Items:**
1. [ ] Implement security awareness training platform (e.g., KnowBe4)
2. [ ] Develop annual training curriculum
3. [ ] Document role-specific competency requirements

---

#### CC1.3 - Personnel Security

| Control | Description | Current State | Action Needed |
|---------|-------------|---------------|---------------|
| CC1.3.1 | Background checks | ✅ Implemented | Document process |
| CC1.3.2 | Employment agreements with security terms | ⚠️ Partial | Add security clauses |
| CC1.3.3 | Termination procedures | ⚠️ Partial | Document access revocation |

**Evidence Required:**
- [ ] Background check policy and records
- [ ] Employment agreement templates with confidentiality clauses
- [ ] Termination checklist including access revocation
- [ ] Exit interview documentation

---

### CC2 - Communication and Information

#### CC2.1 - Information Quality

| Control | Description | Current State | Action Needed |
|---------|-------------|---------------|---------------|
| CC2.1.1 | Relevant information identified | ✅ Implemented | None |
| CC2.1.2 | Internal communication channels | ✅ Implemented | Document |
| CC2.1.3 | External communication protocols | ⚠️ Partial | Document escalation paths |

**Evidence Required:**
- [ ] Data classification policy
- [ ] Internal communication matrix
- [ ] Customer communication templates
- [ ] Security bulletin process

---

### CC3 - Risk Assessment

#### CC3.1 - Risk Identification and Analysis

| Control | Description | Current State | Action Needed |
|---------|-------------|---------------|---------------|
| CC3.1.1 | Risk assessment methodology | ⚠️ Partial | Formalize process |
| CC3.1.2 | Risk register | ❌ Not Started | Create and maintain |
| CC3.1.3 | Annual risk assessment | ❌ Not Started | Conduct initial assessment |
| CC3.1.4 | Third-party risk assessment | ⚠️ Partial | Document vendor assessment |

**Evidence Required:**
- [ ] Risk assessment methodology document
- [ ] Risk register with ratings and owners
- [ ] Annual risk assessment reports
- [ ] Third-party vendor risk assessments

**Action Items:**
1. [ ] Develop risk assessment framework
2. [ ] Create risk register template
3. [ ] Conduct initial risk assessment
4. [ ] Schedule annual risk review

---

### CC4 - Monitoring Activities

#### CC4.1 - Ongoing Monitoring

| Control | Description | Current State | Action Needed |
|---------|-------------|---------------|---------------|
| CC4.1.1 | Continuous monitoring | ✅ Implemented | Document dashboards |
| CC4.1.2 | Security metrics tracking | ⚠️ Partial | Define KPIs |
| CC4.1.3 | Management review of controls | ❌ Not Started | Establish review cadence |

**Evidence Required:**
- [ ] Monitoring dashboard screenshots
- [ ] Security metrics report template
- [ ] Management review meeting minutes
- [ ] Exception/deviation reports

---

### CC5 - Control Activities

#### CC5.1 - Logical and Physical Access Controls

| Control | Description | Current State | Action Needed |
|---------|-------------|---------------|---------------|
| CC5.1.1 | Access provisioning/de-provisioning | ✅ Implemented | Document process |
| CC5.1.2 | Role-based access control | ✅ Implemented | Evidence collection |
| CC5.1.3 | Unique user IDs | ✅ Implemented | None |
| CC5.1.4 | Multi-factor authentication | ✅ Implemented | Document coverage |
| CC5.1.5 | Password requirements | ✅ Implemented | Document policy |
| CC5.1.6 | Access reviews | ⚠️ Partial | Quarterly reviews |

**Evidence Required:**
- [ ] Access provisioning procedure
- [ ] Access de-provisioning procedure
- [ ] RBAC matrix
- [ ] MFA configuration evidence
- [ ] Password policy document
- [ ] Access review reports (quarterly)

---

#### CC5.2 - Data Protection

| Control | Description | Current State | Action Needed |
|---------|-------------|---------------|---------------|
| CC5.2.1 | Encryption at rest | ✅ Implemented (AES-256) | Document |
| CC5.2.2 | Encryption in transit | ✅ Implemented (TLS 1.3) | Document |
| CC5.2.3 | Key management | ✅ Implemented (HashiCorp Vault) | Document |
| CC5.2.4 | Data classification | ⚠️ Partial | Complete classification |

**Evidence Required:**
- [ ] Encryption standards document
- [ ] Key management procedure
- [ ] SSL/TLS certificate inventory
- [ ] Data classification policy with examples

---

#### CC5.3 - Change Management

| Control | Description | Current State | Action Needed |
|---------|-------------|---------------|---------------|
| CC5.3.1 | Change management process | ✅ Implemented | Document procedure |
| CC5.3.2 | Change approval | ✅ Implemented (PR reviews) | Evidence collection |
| CC5.3.3 | Testing before deployment | ✅ Implemented (CI/CD) | Evidence collection |
| CC5.3.4 | Rollback procedures | ✅ Implemented | Document |
| CC5.3.5 | Emergency change process | ⚠️ Partial | Document procedure |

**Evidence Required:**
- [ ] Change management policy
- [ ] PR review history (sample)
- [ ] CI/CD pipeline documentation
- [ ] Deployment logs
- [ ] Emergency change procedure
- [ ] Rollback procedure

---

#### CC5.4 - Vulnerability Management

| Control | Description | Current State | Action Needed |
|---------|-------------|---------------|---------------|
| CC5.4.1 | Vulnerability scanning | ✅ Implemented | Document frequency |
| CC5.4.2 | Penetration testing | ⚠️ Partial | Schedule annual pentest |
| CC5.4.3 | Remediation tracking | ✅ Implemented | Document SLAs |
| CC5.4.4 | Patch management | ✅ Implemented | Document policy |

**Evidence Required:**
- [ ] Vulnerability scanning reports (monthly)
- [ ] Penetration test report (annual)
- [ ] Remediation tracking log
- [ ] Patch management policy and records

**Action Items:**
1. [ ] Schedule annual penetration test
2. [ ] Define remediation SLAs by severity
3. [ ] Document patch management cadence

---

### CC6 - System Operations

#### CC6.1 - Incident Management

| Control | Description | Current State | Action Needed |
|---------|-------------|---------------|---------------|
| CC6.1.1 | Incident response plan | ✅ Implemented | Review and update |
| CC6.1.2 | Incident detection | ✅ Implemented | Document tools |
| CC6.1.3 | Incident response procedures | ✅ Implemented | Test with tabletop |
| CC6.1.4 | Post-incident review | ⚠️ Partial | Formalize PIR process |
| CC6.1.5 | Incident reporting | ⚠️ Partial | Document to customers |

**Evidence Required:**
- [ ] Incident response plan document
- [ ] Detection tool configurations (SIEM, alerts)
- [ ] Incident log and resolutions
- [ ] Post-incident review reports
- [ ] Customer notification templates

---

#### CC6.2 - Business Continuity and Disaster Recovery

| Control | Description | Current State | Action Needed |
|---------|-------------|---------------|---------------|
| CC6.2.1 | BCP/DR plan | ⚠️ Partial | Complete and document |
| CC6.2.2 | Backup procedures | ✅ Implemented | Document |
| CC6.2.3 | DR testing | ❌ Not Started | Schedule annual test |
| CC6.2.4 | RTO/RPO definitions | ⚠️ Partial | Document by system |

**Evidence Required:**
- [ ] Business Continuity Plan
- [ ] Disaster Recovery Plan
- [ ] Backup logs and verification reports
- [ ] DR test results (annual)
- [ ] RTO/RPO matrix

**Action Items:**
1. [ ] Complete BCP/DR documentation
2. [ ] Define RTO/RPO for each critical system
3. [ ] Schedule DR test
4. [ ] Document backup verification process

---

## Trust Service Criteria: Availability (A)

### A1 - System Availability

| Control | Description | Current State | Action Needed |
|---------|-------------|---------------|---------------|
| A1.1 | Availability SLAs defined | ⚠️ Partial | Document in customer agreements |
| A1.2 | Capacity planning | ✅ Implemented (Kubernetes HPA) | Document |
| A1.3 | Performance monitoring | ✅ Implemented | Document tools/thresholds |
| A1.4 | Availability metrics tracking | ✅ Implemented | Create reports |

**Evidence Required:**
- [ ] SLA documentation
- [ ] Capacity planning records
- [ ] Monitoring dashboard access
- [ ] Availability reports (monthly)
- [ ] Uptime metrics (target: 99.9%)

---

## Trust Service Criteria: Confidentiality (C)

### C1 - Confidentiality Commitments

| Control | Description | Current State | Action Needed |
|---------|-------------|---------------|---------------|
| C1.1 | Confidentiality policies | ✅ Implemented | Review and update |
| C1.2 | Data handling procedures | ✅ Implemented | Document by data type |
| C1.3 | NDA/confidentiality agreements | ✅ Implemented | Track inventory |
| C1.4 | Customer data segregation | ✅ Implemented (multi-tenant) | Document |

**Evidence Required:**
- [ ] Confidentiality policy
- [ ] Data handling procedures
- [ ] NDA templates and tracking
- [ ] Multi-tenancy architecture documentation
- [ ] Data isolation testing results

---

## Trust Service Criteria: Processing Integrity (PI)

### PI1 - Processing Accuracy

| Control | Description | Current State | Action Needed |
|---------|-------------|---------------|---------------|
| PI1.1 | Processing completeness checks | ✅ Implemented | Document |
| PI1.2 | Error detection and correction | ✅ Implemented | Document |
| PI1.3 | Data validation | ✅ Implemented (FHIR validation) | Document |
| PI1.4 | Processing accuracy verification | ⚠️ Partial | Formal verification procedures |

**Evidence Required:**
- [ ] Processing controls documentation
- [ ] Error handling procedures
- [ ] FHIR validation rules
- [ ] Quality assurance/testing procedures
- [ ] Data reconciliation reports

---

## Trust Service Criteria: Privacy (P)

### P1 - Privacy Practices

| Control | Description | Current State | Action Needed |
|---------|-------------|---------------|---------------|
| P1.1 | Privacy policy published | ✅ Implemented | Review annually |
| P1.2 | Consent management | ⚠️ Partial | Formalize for PHI |
| P1.3 | Data subject rights procedures | ⚠️ Partial | Document GDPR/CCPA response |
| P1.4 | Privacy impact assessments | ❌ Not Started | Develop template |
| P1.5 | Data retention policy | ⚠️ Partial | Document by data type |
| P1.6 | Data deletion procedures | ⚠️ Partial | Formalize and test |

**Evidence Required:**
- [ ] Privacy policy (public)
- [ ] Consent records/management system
- [ ] Data subject access request procedures
- [ ] Privacy impact assessment template
- [ ] Data retention schedule
- [ ] Data deletion verification records

**Action Items:**
1. [ ] Develop Privacy Impact Assessment template
2. [ ] Document data subject rights procedures
3. [ ] Create data retention schedule
4. [ ] Test and document deletion procedures

---

## Gap Summary and Remediation Plan

### Critical Gaps (Must Fix Before Audit)

| Gap | Description | Owner | Target Date |
|-----|-------------|-------|-------------|
| Risk Assessment | No formal annual risk assessment | [TBD] | Month 1 |
| Security Training | No security awareness program | [TBD] | Month 2 |
| DR Testing | No documented DR test | [TBD] | Month 3 |
| Penetration Test | No recent pentest | [TBD] | Month 2 |

### Medium Gaps (Fix Before Audit)

| Gap | Description | Owner | Target Date |
|-----|-------------|-------|-------------|
| Access Reviews | Quarterly access reviews not documented | [TBD] | Month 1 |
| BCP/DR Plan | Documentation incomplete | [TBD] | Month 2 |
| Privacy Impact | No PIA template | [TBD] | Month 3 |
| Data Retention | Incomplete retention schedule | [TBD] | Month 2 |

### Low Gaps (Document/Formalize)

| Gap | Description | Owner | Target Date |
|-----|-------------|-------|-------------|
| Policy Documentation | Several policies need formalization | [TBD] | Ongoing |
| Evidence Collection | Need systematic evidence gathering | [TBD] | Month 1 |
| Procedure Updates | Some procedures outdated | [TBD] | Month 2 |

---

## Audit Readiness Timeline

### Month 1: Foundation
- [ ] Hire/designate compliance lead
- [ ] Select auditor
- [ ] Complete risk assessment
- [ ] Begin policy documentation
- [ ] Start evidence collection system

### Month 2: Controls Implementation
- [ ] Implement security awareness training
- [ ] Schedule penetration test
- [ ] Complete BCP/DR documentation
- [ ] Implement access review process
- [ ] Document all procedures

### Month 3: Testing and Remediation
- [ ] Conduct internal audit/readiness assessment
- [ ] Complete penetration test
- [ ] Conduct DR test
- [ ] Remediate any findings
- [ ] Complete evidence collection

### Month 4: Type I Audit (Optional)
- [ ] Type I audit (point-in-time assessment)
- [ ] Remediate any Type I findings
- [ ] Begin observation period

### Months 5-10: Observation Period
- [ ] Operate controls consistently
- [ ] Collect evidence throughout period
- [ ] Conduct quarterly access reviews
- [ ] Maintain incident/change logs

### Month 11: Type II Audit
- [ ] Type II audit fieldwork
- [ ] Respond to auditor requests
- [ ] Address any findings

### Month 12: Report
- [ ] Receive SOC 2 Type II report
- [ ] Address any management letter items
- [ ] Publish/share with customers

---

## Auditor Recommendations

### Tier 1 (Big 4 - Large Organizations)
- **Deloitte, EY, KPMG, PwC**
- Cost: $100K-$300K
- Best for: Large enterprises, public companies
- Not recommended for HDIM (overkill)

### Tier 2 (Mid-Market - Recommended)
- **A-LIGN**
  - Healthcare experience
  - Cost: $30K-$50K
  - Fast turnaround
  - Strong for startups

- **Coalfire**
  - Cloud security focus
  - Cost: $40K-$60K
  - FedRAMP experience

- **Schellman**
  - Privacy expertise
  - Cost: $35K-$55K
  - HITRUST experience

### Tier 3 (Boutique - Cost-Effective)
- **Johanson Group**
  - Startup-friendly
  - Cost: $20K-$35K
  - Flexible engagement

- **AssuranceLab**
  - Automated platform
  - Cost: $15K-$30K
  - Faster process

### Recommendation for HDIM
**A-LIGN or AssuranceLab** - Balance of cost, speed, and healthcare experience.

---

## Evidence Collection System

### Required Evidence Repository

| Category | Tool/Location | Responsible |
|----------|---------------|-------------|
| Policies | Google Drive / Notion | Compliance Lead |
| Access Reviews | Identity Provider Logs | IT |
| Change Management | GitHub PRs/Commits | Engineering |
| Vulnerability Scans | Security Tool Exports | Security |
| Incident Logs | Ticketing System | Security |
| Training Records | Training Platform | HR |
| Backup Logs | Cloud Provider Console | DevOps |
| Monitoring | Dashboard Screenshots | DevOps |

### Recommended Tools
- **Vanta** - SOC 2 automation platform
- **Drata** - Compliance automation
- **Secureframe** - Continuous monitoring
- **Tugboat Logic** - Policy management

**Cost:** $10K-$20K/year for automation platform
**ROI:** Reduces audit prep time by 50-70%

---

## Budget Estimate

| Item | Low Estimate | High Estimate |
|------|--------------|---------------|
| Auditor Fees | $25,000 | $50,000 |
| Penetration Test | $10,000 | $25,000 |
| Compliance Platform | $10,000 | $20,000 |
| Training Platform | $2,000 | $5,000 |
| Staff Time (internal) | $15,000 | $30,000 |
| Policy Consultant (optional) | $0 | $15,000 |
| **Total** | **$62,000** | **$145,000** |

**Recommendation:** Target $75K-$100K budget for first SOC 2 Type II.

---

## Appendices

### A. Policy Templates Needed
1. Information Security Policy
2. Access Control Policy
3. Change Management Policy
4. Incident Response Policy
5. Business Continuity/DR Policy
6. Data Classification Policy
7. Encryption Policy
8. Acceptable Use Policy
9. Vendor Management Policy
10. Privacy Policy

### B. Key Contacts
| Role | Name | Email |
|------|------|-------|
| Executive Sponsor | [TBD] | |
| Compliance Lead | [TBD] | |
| IT Contact | [TBD] | |
| Engineering Contact | [TBD] | |
| HR Contact | [TBD] | |
| Auditor | [TBD] | |

### C. Useful Resources
- AICPA Trust Services Criteria: [aicpa.org](https://aicpa.org)
- SOC 2 Guide: [socreports.com](https://socreports.com)
- Control Mapping Spreadsheet: [Internal link]

---

*Last Updated: December 2025*
*Review Date: Quarterly*
