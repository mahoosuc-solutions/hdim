# HDIM Security Questionnaire Responses

> Pre-answered responses to common security questionnaires for customer assessments.

---

## Quick Reference

| Document | Status | Last Updated |
|----------|--------|--------------|
| SIG Lite Responses | Complete | December 2025 |
| CAIQ v4 Responses | Complete | December 2025 |
| Custom Healthcare Questions | Complete | December 2025 |
| Evidence Documents | Available on request | Ongoing |

**Contact for additional questions:** security@healthdatainmotion.com

---

## Section 1: Organization & Governance

### 1.1 Company Information

| Question | Response |
|----------|----------|
| Legal company name | Health Data In Motion, Inc. |
| DBA/Trade name | HDIM |
| Year founded | 2024 |
| Company type | Delaware C-Corporation |
| Headquarters | [Address] |
| Number of employees | [Current count] |
| Primary business | Healthcare quality measurement SaaS |

### 1.2 Security Leadership

| Question | Response |
|----------|----------|
| Is there a designated CISO or security leader? | Yes - CTO currently serves as security leader |
| Does the security leader report to executive leadership? | Yes - reports directly to CEO |
| Is there a dedicated security team? | Security responsibilities are integrated into engineering; dedicated hire planned for 2025 |

### 1.3 Policies and Procedures

| Question | Response |
|----------|----------|
| Do you have documented information security policies? | Yes |
| Are policies reviewed at least annually? | Yes |
| Are policies approved by management? | Yes |
| Are policies communicated to all employees? | Yes |
| Do you have an acceptable use policy? | Yes |
| Do you have a data classification policy? | Yes |
| Do you have an incident response policy? | Yes |
| Do you have a business continuity/DR policy? | Yes |

**Evidence available:** Policy document index, policy review records

---

## Section 2: Risk Management

### 2.1 Risk Assessment

| Question | Response |
|----------|----------|
| Do you conduct risk assessments? | Yes |
| How often are risk assessments performed? | Annually, or after significant changes |
| Do risk assessments cover third-party vendors? | Yes |
| Is there a documented risk register? | Yes |
| Are risk treatment plans tracked to completion? | Yes |

### 2.2 Compliance

| Question | Response |
|----------|----------|
| Are you HIPAA compliant? | Yes |
| Do you have SOC 2 Type II certification? | In progress (target Q2 2025) |
| Do you have ISO 27001 certification? | Not currently; under evaluation |
| Do you sign Business Associate Agreements (BAAs)? | Yes, with all healthcare customers |
| Are you GDPR compliant? | Yes, for applicable data |
| Are you CCPA compliant? | Yes |

---

## Section 3: Human Resources Security

### 3.1 Background Checks

| Question | Response |
|----------|----------|
| Do you perform background checks on employees? | Yes |
| What do background checks include? | Criminal history, employment verification, education verification |
| Are background checks performed before access is granted? | Yes |
| Are contractors/vendors required to pass background checks? | Yes, for those with data access |

### 3.2 Training

| Question | Response |
|----------|----------|
| Do you provide security awareness training? | Yes |
| How often is security training conducted? | Upon hire and annually thereafter |
| Is HIPAA training included? | Yes |
| Is phishing awareness training included? | Yes |
| Is training completion tracked? | Yes |
| What is the current training completion rate? | 100% (required for system access) |

### 3.3 Termination

| Question | Response |
|----------|----------|
| Is there a documented offboarding process? | Yes |
| Is system access revoked upon termination? | Yes, immediately |
| Are access credentials disabled/deleted? | Yes |
| Is equipment retrieved? | Yes |
| How quickly is access revoked? | Within 4 hours of notification |

---

## Section 4: Access Control

### 4.1 Authentication

| Question | Response |
|----------|----------|
| Do you require unique user IDs? | Yes |
| Is multi-factor authentication (MFA) required? | Yes, for all users |
| What MFA methods are supported? | TOTP, hardware tokens, push notifications |
| Is single sign-on (SSO) available? | Yes (SAML 2.0, OIDC) |
| What is the password policy? | 12+ characters, complexity required, 90-day rotation |
| Is password history enforced? | Yes (last 12 passwords) |
| Are accounts locked after failed attempts? | Yes (5 attempts) |

### 4.2 Authorization

| Question | Response |
|----------|----------|
| Is role-based access control (RBAC) implemented? | Yes |
| Is the principle of least privilege applied? | Yes |
| Are user access rights reviewed periodically? | Yes, quarterly |
| Are privileged accounts separately managed? | Yes |
| Is just-in-time access used for privileged functions? | Yes, where applicable |

### 4.3 Session Management

| Question | Response |
|----------|----------|
| Are sessions automatically terminated after inactivity? | Yes (30 minutes) |
| Can users manually terminate sessions? | Yes |
| Are concurrent sessions limited? | Yes (configurable per customer) |
| Are session tokens securely generated? | Yes (cryptographically random) |

---

## Section 5: Data Protection

### 5.1 Encryption

| Question | Response |
|----------|----------|
| Is data encrypted at rest? | Yes |
| What encryption algorithm is used at rest? | AES-256 |
| Is data encrypted in transit? | Yes |
| What encryption is used in transit? | TLS 1.3 (minimum TLS 1.2) |
| Are encryption keys managed securely? | Yes (HashiCorp Vault) |
| How often are encryption keys rotated? | Annually or as needed |
| Is database encryption enabled? | Yes (Transparent Data Encryption) |

### 5.2 Data Classification

| Question | Response |
|----------|----------|
| Do you classify data by sensitivity? | Yes |
| What classification levels exist? | Public, Internal, Confidential, PHI |
| Is PHI/PII labeled and tracked? | Yes |
| Are handling procedures defined for each level? | Yes |

### 5.3 Data Retention and Disposal

| Question | Response |
|----------|----------|
| Is there a documented data retention policy? | Yes |
| How long is customer data retained? | Per customer agreement (default: 7 years) |
| Is data securely deleted upon request? | Yes |
| What deletion method is used? | Cryptographic erasure + overwrite verification |
| Is deletion certified? | Yes, written certification provided |

### 5.4 Backup

| Question | Response |
|----------|----------|
| Are regular backups performed? | Yes |
| How often are backups taken? | Daily full, hourly incremental |
| Are backups encrypted? | Yes (AES-256) |
| Are backups stored in a separate location? | Yes (different AWS region) |
| Are backup restorations tested? | Yes, quarterly |
| What is the backup retention period? | 30 days |

---

## Section 6: Network Security

### 6.1 Network Architecture

| Question | Response |
|----------|----------|
| Is the network segmented? | Yes |
| Are production and development environments separated? | Yes (separate VPCs) |
| Is a web application firewall (WAF) deployed? | Yes (AWS WAF) |
| Is DDoS protection in place? | Yes (AWS Shield) |
| Are security groups/firewalls configured? | Yes |

### 6.2 Intrusion Detection/Prevention

| Question | Response |
|----------|----------|
| Is IDS/IPS deployed? | Yes |
| What tools are used? | AWS GuardDuty, Datadog Security Monitoring |
| Are alerts monitored 24/7? | Yes |
| Is there an automated response capability? | Yes, for critical alerts |

### 6.3 Remote Access

| Question | Response |
|----------|----------|
| How is remote access provided? | VPN with MFA required |
| Is split tunneling prohibited? | Yes |
| Are remote access sessions logged? | Yes |

---

## Section 7: Vulnerability Management

### 7.1 Vulnerability Scanning

| Question | Response |
|----------|----------|
| Do you perform vulnerability scans? | Yes |
| How often are scans performed? | Weekly (automated), monthly (comprehensive) |
| What tools are used? | AWS Inspector, Snyk, Trivy |
| Are results reviewed and prioritized? | Yes |
| What is the remediation SLA for critical vulnerabilities? | 24 hours |
| What is the remediation SLA for high vulnerabilities? | 7 days |
| What is the remediation SLA for medium vulnerabilities? | 30 days |

### 7.2 Penetration Testing

| Question | Response |
|----------|----------|
| Do you conduct penetration tests? | Yes |
| How often are penetration tests performed? | Annually |
| Who performs penetration tests? | Independent third party |
| Are results shared with customers upon request? | Yes (executive summary) |
| Are findings remediated? | Yes, tracked to completion |

### 7.3 Patch Management

| Question | Response |
|----------|----------|
| Is there a patch management process? | Yes |
| How often are patches applied? | Critical: 24 hours, High: 7 days, Others: 30 days |
| Are patches tested before deployment? | Yes |
| Is patching automated? | Yes, where possible |

---

## Section 8: Application Security

### 8.1 Secure Development

| Question | Response |
|----------|----------|
| Is there a secure software development lifecycle (SDLC)? | Yes |
| Do developers receive secure coding training? | Yes |
| Is code reviewed before deployment? | Yes (peer review required) |
| Is static application security testing (SAST) used? | Yes (SonarQube, Snyk) |
| Is dynamic application security testing (DAST) used? | Yes (OWASP ZAP) |
| Is software composition analysis (SCA) used? | Yes (Snyk, Dependabot) |

### 8.2 Change Management

| Question | Response |
|----------|----------|
| Is there a documented change management process? | Yes |
| Are changes tested before deployment? | Yes (CI/CD pipeline) |
| Is there a rollback capability? | Yes |
| Are emergency changes documented? | Yes |
| Is separation of duties enforced? | Yes (developer cannot deploy own code) |

### 8.3 API Security

| Question | Response |
|----------|----------|
| How are APIs authenticated? | OAuth 2.0, API keys |
| Is rate limiting implemented? | Yes |
| Is input validation performed? | Yes |
| Are APIs monitored for abuse? | Yes |

---

## Section 9: Incident Response

### 9.1 Incident Response Plan

| Question | Response |
|----------|----------|
| Is there a documented incident response plan? | Yes |
| Does the plan define roles and responsibilities? | Yes |
| Does the plan include communication procedures? | Yes |
| Is the plan tested regularly? | Yes (tabletop exercises annually) |
| Is there a 24/7 incident response capability? | Yes |

### 9.2 Breach Notification

| Question | Response |
|----------|----------|
| What is the breach notification timeframe? | Within 24 hours of confirmation |
| Who is notified in case of breach? | Customer security contact, then as required by law |
| Is there a breach notification template? | Yes |
| Are breach incidents documented? | Yes |

### 9.3 Forensics

| Question | Response |
|----------|----------|
| Are logs retained for forensic analysis? | Yes (90 days minimum) |
| Can logs be provided to customers upon request? | Yes (for their data) |
| Is there a forensic investigation capability? | Yes (internal + external partner) |

---

## Section 10: Business Continuity

### 10.1 Business Continuity Planning

| Question | Response |
|----------|----------|
| Is there a documented business continuity plan? | Yes |
| Does the plan cover critical systems? | Yes |
| Is the plan tested regularly? | Yes (annually) |
| What is the Recovery Time Objective (RTO)? | 4 hours |
| What is the Recovery Point Objective (RPO)? | 1 hour |

### 10.2 Disaster Recovery

| Question | Response |
|----------|----------|
| Is there a documented disaster recovery plan? | Yes |
| Are systems replicated to a secondary site? | Yes (multi-AZ, cross-region option) |
| Is failover tested? | Yes (annually) |
| What is the target uptime SLA? | 99.9% |

---

## Section 11: Physical Security

### 11.1 Data Center Security

| Question | Response |
|----------|----------|
| Where is data hosted? | AWS (us-east-1, us-west-2) |
| Is the data center SOC 2 certified? | Yes (AWS SOC 2 Type II) |
| Is 24/7 physical security in place? | Yes (AWS manages) |
| Are biometric access controls used? | Yes (AWS manages) |
| Is video surveillance in place? | Yes (AWS manages) |

### 11.2 Office Security

| Question | Response |
|----------|----------|
| Is office access controlled? | Yes (badge access) |
| Is visitor access logged? | Yes |
| Are clean desk policies in place? | Yes |
| Is PHI stored on physical media? | No (cloud-only) |

---

## Section 12: Vendor Management

### 12.1 Third-Party Risk

| Question | Response |
|----------|----------|
| Is there a vendor management program? | Yes |
| Are vendors assessed before engagement? | Yes |
| Are vendor security requirements documented? | Yes |
| Are critical vendors reviewed annually? | Yes |
| Do vendors with data access sign appropriate agreements? | Yes (BAA, DPA as applicable) |

### 12.2 Key Vendors

| Vendor | Service | Data Access | Compliance |
|--------|---------|-------------|------------|
| AWS | Cloud hosting | Yes | SOC 2, HIPAA BAA |
| HashiCorp | Secrets management | Indirect | SOC 2 |
| Datadog | Monitoring | Logs | SOC 2, HIPAA BAA |
| GitHub | Source code | No PHI | SOC 2 |

---

## Section 13: Healthcare-Specific Questions

### 13.1 HIPAA Compliance

| Question | Response |
|----------|----------|
| Are you a Business Associate under HIPAA? | Yes |
| Do you sign Business Associate Agreements? | Yes, with all covered entity customers |
| Have you conducted a HIPAA risk assessment? | Yes |
| Is there a designated HIPAA privacy officer? | Yes |
| Is there a designated HIPAA security officer? | Yes |
| Do you provide HIPAA training to workforce? | Yes |
| Is PHI encrypted per HIPAA requirements? | Yes |
| Do you have breach notification procedures? | Yes |

### 13.2 PHI Handling

| Question | Response |
|----------|----------|
| What types of PHI do you handle? | Demographics, diagnoses, medications, labs, procedures |
| Is PHI segregated by customer (multi-tenant)? | Yes |
| Can customers access only their own data? | Yes |
| Is PHI de-identified for analytics? | Available option |
| Is PHI used for any purpose other than service delivery? | No |
| Is PHI sold to third parties? | No |

### 13.3 Healthcare Integrations

| Question | Response |
|----------|----------|
| What EHR systems do you integrate with? | Epic, Cerner, athenahealth, NextGen, and others via FHIR |
| What integration standards do you support? | FHIR R4, HL7v2 (via n8n), CSV |
| Is data transmitted securely from EHR to HDIM? | Yes (TLS 1.3) |
| Do you support Bulk FHIR export? | Yes |
| Do you support SMART on FHIR? | Yes |

---

## Section 14: Audit & Logging

### 14.1 Logging

| Question | Response |
|----------|----------|
| Are audit logs maintained? | Yes |
| What events are logged? | Authentication, access, changes, errors |
| Are logs tamper-evident? | Yes (immutable storage) |
| How long are logs retained? | 90 days online, 1 year archived |
| Are logs centralized? | Yes (SIEM) |
| Are logs encrypted? | Yes |

### 14.2 Monitoring

| Question | Response |
|----------|----------|
| Is real-time monitoring in place? | Yes |
| Are security alerts configured? | Yes |
| Is 24/7 monitoring available? | Yes |
| What SIEM is used? | Datadog Security Monitoring |

---

## Section 15: Privacy

### 15.1 Privacy Practices

| Question | Response |
|----------|----------|
| Is there a published privacy policy? | Yes |
| Is a privacy officer designated? | Yes |
| Are privacy impact assessments conducted? | Yes, for significant changes |
| Can data subjects exercise their rights? | Yes, via customer |
| What is the data subject request response time? | 15 business days |

### 15.2 Data Minimization

| Question | Response |
|----------|----------|
| Is only necessary data collected? | Yes |
| Is data purpose-limited? | Yes |
| Can customers configure data collection? | Yes |

---

## Document Request Checklist

When customers request evidence documents, we can provide:

| Document | Availability |
|----------|--------------|
| SOC 2 Type II Report | Q2 2025 (SOC 2 readiness docs available now) |
| Penetration Test Summary | Available on request (NDA) |
| Insurance Certificates | Available on request |
| Business Continuity Plan | Summary available on request |
| Incident Response Plan | Summary available on request |
| Data Flow Diagram | Available on request |
| Network Architecture Diagram | Available on request |
| BAA Template | Available immediately |
| DPA Template | Available immediately |
| Security Whitepaper | Available immediately |

---

## Annual Review Checklist

To keep questionnaire responses current:

- [ ] Review all responses quarterly
- [ ] Update after any significant security changes
- [ ] Update after policy changes
- [ ] Update after new certifications
- [ ] Version and date all updates
- [ ] Train sales/CS team on changes

---

*Last Updated: December 2025*
*Next Review: March 2026*
*Owner: Security Team*
