# HIPAA Security Incident Response Plan

## Document Purpose

This document outlines HDIM's procedures for detecting, responding to, and recovering from security incidents involving electronic Protected Health Information (ePHI), in compliance with HIPAA Security Rule § 164.308(a)(6).

**Document Version:** 1.0
**Last Updated:** December 2024
**Plan Owner:** Security Team
**Review Schedule:** Annual (or after any incident)

---

## 1. Incident Classification

### Severity Levels

| Level | Definition | Response Time | Example |
|-------|------------|---------------|---------|
| **Critical** | Active breach with confirmed PHI exposure | Immediate (< 1 hour) | Unauthorized database access, ransomware |
| **High** | Potential breach, unconfirmed exposure | < 4 hours | Suspicious access patterns, credential theft |
| **Medium** | Security violation, no apparent exposure | < 24 hours | Failed attack attempt, policy violation |
| **Low** | Security anomaly, investigation needed | < 72 hours | Unusual login patterns, false positive |

### Incident Categories

1. **Unauthorized Access** - Access by unauthorized individuals
2. **Unauthorized Disclosure** - PHI shared with unauthorized parties
3. **Unauthorized Modification** - PHI altered without authorization
4. **Denial of Service** - System unavailability affecting PHI access
5. **Malware/Ransomware** - Malicious software affecting ePHI systems
6. **Physical Security** - Physical breach affecting ePHI infrastructure
7. **Insider Threat** - Malicious activity by authorized personnel

---

## 2. Incident Response Team

### Core Team Members

| Role | Responsibility | Contact |
|------|----------------|---------|
| **Incident Commander** | Overall incident management | On-call rotation |
| **Security Lead** | Technical investigation | Security team |
| **Engineering Lead** | System remediation | Engineering team |
| **Legal/Compliance** | HIPAA notification requirements | Legal counsel |
| **Communications** | Internal/external communications | PR/Marketing |
| **Privacy Officer** | PHI impact assessment | Compliance team |

### Escalation Path

```
Detection → Security Lead → Incident Commander
                    ↓
            Legal/Compliance (if PHI involved)
                    ↓
            Executive Leadership (Critical/High severity)
```

---

## 3. Detection and Alerting

### Automated Detection

| Detection Method | Covered Threats | Alert Destination |
|-----------------|-----------------|-------------------|
| Prometheus Alerts | System anomalies, performance | PagerDuty |
| Audit Log Analysis | Unusual access patterns | Security dashboard |
| Failed Auth Monitoring | Brute-force attempts | Security team |
| SIEM Integration | Correlated security events | SOC team |
| Dependency Scanning | Vulnerable dependencies | GitHub Security |

### Manual Reporting

All employees must report suspected incidents to:
- Email: security@hdim.health
- Slack: #security-incidents
- Phone: Security hotline (24/7)

---

## 4. Response Procedures

### Phase 1: Initial Assessment (0-1 hours)

**Objectives:**
- Confirm incident is genuine (not false positive)
- Classify severity level
- Activate appropriate response team

**Actions:**
1. [ ] Receive and log incident report
2. [ ] Assign Incident Commander
3. [ ] Gather initial evidence (logs, screenshots)
4. [ ] Classify severity and category
5. [ ] Notify core response team
6. [ ] Create incident ticket with timeline

**Documentation Required:**
- Incident ID
- Detection timestamp
- Reporter details
- Initial classification
- Systems potentially affected

### Phase 2: Containment (1-4 hours)

**Objectives:**
- Prevent further damage
- Preserve evidence
- Minimize impact to operations

**Actions:**
1. [ ] Isolate affected systems (if necessary)
2. [ ] Revoke compromised credentials
3. [ ] Block malicious IPs/accounts
4. [ ] Preserve audit logs and evidence
5. [ ] Notify affected tenants (if multi-tenant impact)

**Containment Strategies:**

| Scenario | Containment Action |
|----------|-------------------|
| Compromised credentials | Immediate password reset, session invalidation |
| Malware detected | Network isolation, endpoint quarantine |
| Unauthorized access | Block source IP, disable account |
| Data exfiltration | Block outbound traffic, preserve logs |

### Phase 3: Eradication (4-24 hours)

**Objectives:**
- Remove threat from environment
- Patch vulnerabilities
- Restore secure state

**Actions:**
1. [ ] Identify root cause
2. [ ] Remove malware/backdoors
3. [ ] Patch vulnerabilities
4. [ ] Reset all potentially compromised credentials
5. [ ] Verify eradication with security scans

### Phase 4: Recovery (24-72 hours)

**Objectives:**
- Restore normal operations
- Implement additional safeguards
- Verify system integrity

**Actions:**
1. [ ] Restore systems from clean backups (if needed)
2. [ ] Verify data integrity
3. [ ] Gradually restore services
4. [ ] Implement additional monitoring
5. [ ] Validate security controls

### Phase 5: Post-Incident (1-2 weeks)

**Objectives:**
- Document lessons learned
- Improve detection/response
- Complete regulatory notifications

**Actions:**
1. [ ] Complete incident timeline
2. [ ] Conduct post-mortem meeting
3. [ ] Update incident response procedures
4. [ ] Implement preventive measures
5. [ ] Complete HIPAA breach notifications (if required)

---

## 5. HIPAA Breach Notification Requirements

### Breach Determination

A breach is presumed unless the organization demonstrates a low probability of compromise based on:
1. Nature and extent of PHI involved
2. Unauthorized person who used/accessed PHI
3. Whether PHI was actually acquired or viewed
4. Extent to which risk has been mitigated

### Notification Timelines

| Notification | Timeline | Trigger |
|--------------|----------|---------|
| **Individual Notice** | Within 60 days of discovery | Any breach of unsecured PHI |
| **HHS Notice** | Within 60 days (or annually for <500) | Any breach of unsecured PHI |
| **Media Notice** | Within 60 days | Breach affecting >500 in a state |
| **Business Associate** | Without unreasonable delay | BA discovers breach |

### Notification Content

Required elements for individual notification:
- Description of what happened
- Types of PHI involved
- Steps individuals should take
- What we are doing to investigate/mitigate
- Contact procedures for questions

---

## 6. Communication Templates

### Internal Stakeholder Notification

```
SUBJECT: [SEVERITY] Security Incident - [INCIDENT-ID]

CLASSIFICATION: [Critical/High/Medium/Low]
STATUS: [Investigating/Contained/Resolved]

SUMMARY:
[Brief description of incident]

AFFECTED SYSTEMS:
[List of affected systems/services]

CURRENT ACTIONS:
[List of response actions in progress]

NEXT UPDATE: [Date/Time]

FOR QUESTIONS: Contact [Incident Commander]
```

### Customer Notification (Potential Breach)

```
SUBJECT: Security Notice - Action May Be Required

Dear [Customer Name],

We are writing to inform you of a security incident that may have
affected your organization's data.

WHAT HAPPENED:
[Description]

WHAT INFORMATION WAS INVOLVED:
[Types of data potentially affected]

WHAT WE ARE DOING:
[Response actions taken]

WHAT YOU CAN DO:
[Recommended actions]

FOR MORE INFORMATION:
Contact: security@hdim.health
Phone: [Security hotline]

We apologize for any concern this may cause and are committed to
protecting your data.

Sincerely,
HDIM Security Team
```

---

## 7. Evidence Preservation

### Evidence Collection Checklist

| Evidence Type | Collection Method | Storage |
|---------------|-------------------|---------|
| Audit Logs | Export from database | Encrypted S3 bucket |
| System Logs | Copy from servers | Encrypted S3 bucket |
| Network Logs | Export from firewall/proxy | Encrypted S3 bucket |
| Memory Dumps | Forensic imaging tools | Encrypted storage |
| Screenshots | Secure capture | Incident ticket |
| Emails/Communications | Preserve in place | Legal hold |

### Chain of Custody

All evidence must be:
- Timestamped at collection
- Hash-verified for integrity
- Stored in tamper-evident storage
- Accessed only by authorized personnel
- Documented in chain of custody log

---

## 8. Post-Incident Review

### Post-Mortem Template

```
INCIDENT POST-MORTEM: [INCIDENT-ID]

Date of Incident: [Date]
Date of Resolution: [Date]
Severity: [Level]

TIMELINE:
[Detailed timeline of events]

ROOT CAUSE:
[Technical and process root causes]

IMPACT:
- Systems affected: [List]
- Data affected: [Scope]
- Duration: [Time]
- Business impact: [Description]

WHAT WENT WELL:
[Effective response elements]

WHAT COULD BE IMPROVED:
[Areas for improvement]

ACTION ITEMS:
[ ] [Action 1] - Owner: [Name] - Due: [Date]
[ ] [Action 2] - Owner: [Name] - Due: [Date]

LESSONS LEARNED:
[Key takeaways]
```

---

## 9. Testing and Exercises

### Tabletop Exercises

- **Frequency:** Quarterly
- **Participants:** Incident response team
- **Scenarios:** Rotate through incident categories
- **Documentation:** Exercise report with findings

### Technical Drills

- **Frequency:** Semi-annually
- **Scope:** Full incident simulation
- **Objectives:** Test detection, response, communication

---

## 10. Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | Dec 2024 | Security Team | Initial creation |

---

## Appendix: Quick Reference Card

### Immediate Actions for Any Incident

1. **DON'T PANIC** - Follow this plan
2. **DOCUMENT** - Note timestamps and actions
3. **REPORT** - Contact security@hdim.health
4. **PRESERVE** - Don't modify/delete evidence
5. **CONTAIN** - Isolate if directed by security

### Key Contacts

| Role | Contact |
|------|---------|
| Security Hotline | [Phone number] |
| Incident Email | security@hdim.health |
| Privacy Officer | [Contact] |
| Legal Counsel | [Contact] |

### Severity Quick Reference

- **CRITICAL:** Active breach, immediate escalation
- **HIGH:** Potential breach, 4-hour response
- **MEDIUM:** Security violation, 24-hour response
- **LOW:** Anomaly investigation, 72-hour response
