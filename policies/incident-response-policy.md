# Incident Response Policy

**HDIM - HealthData-in-Motion**

| Version | Date | Author | Status |
|---------|------|--------|--------|
| 1.0 | December 2025 | Security Team | Active |

---

## 1. Purpose

This policy establishes procedures for detecting, responding to, and recovering from security incidents. It ensures consistent handling of incidents to minimize impact and meet regulatory notification requirements.

---

## 2. Scope

This policy covers:
- All security incidents affecting HDIM systems or data
- Incidents involving customer data (including PHI)
- Incidents reported by employees, customers, or third parties
- Suspected or confirmed security events

---

## 3. Definitions

| Term | Definition |
|------|------------|
| **Security Incident** | Any event that compromises confidentiality, integrity, or availability |
| **Data Breach** | Unauthorized access, acquisition, or disclosure of protected data |
| **PHI Breach** | Unauthorized access to Protected Health Information |
| **Security Event** | Observable occurrence relevant to security (may not be an incident) |

---

## 4. Incident Classification

### 4.1 Severity Levels

| Severity | Description | Examples | Response Time |
|----------|-------------|----------|---------------|
| **Critical (P1)** | Confirmed data breach, system compromise | PHI exposure, ransomware, credential theft | Immediate |
| **High (P2)** | Likely breach, significant risk | Suspicious access, malware detection | 4 hours |
| **Medium (P3)** | Potential threat, contained | Failed attack, policy violation | 24 hours |
| **Low (P4)** | Minor issue, no impact | Spam, minor misconfiguration | 72 hours |

### 4.2 Incident Types

| Type | Description |
|------|-------------|
| Unauthorized Access | Access by unauthorized person or system |
| Malware | Virus, ransomware, trojan detection |
| Data Exposure | Unintended disclosure of sensitive data |
| Denial of Service | System unavailability due to attack |
| Phishing | Social engineering attacks |
| Insider Threat | Malicious or negligent employee actions |
| Physical | Theft, unauthorized physical access |

---

## 5. Incident Response Team

### 5.1 Team Composition

| Role | Responsibility | Contact |
|------|----------------|---------|
| **Incident Commander** | Overall coordination, decisions | [Name/Contact] |
| **Security Lead** | Technical investigation, containment | security@healthdata-in-motion.com |
| **Communications Lead** | Internal/external communications | [Name/Contact] |
| **Legal Counsel** | Regulatory, legal guidance | [Name/Contact] |
| **Executive Sponsor** | Resource approval, escalation | [Name/Contact] |

### 5.2 On-Call Rotation

- Security team maintains 24/7 on-call coverage
- On-call engineer responds within 30 minutes
- Escalation to Incident Commander for P1/P2 incidents

---

## 6. Incident Response Phases

### Phase 1: Detection & Reporting

**All personnel must report suspected incidents immediately.**

#### Reporting Channels
- Email: security@healthdata-in-motion.com
- Phone: [Emergency hotline]
- Slack: #security-incidents (internal)

#### Required Information
- Date/time of detection
- Description of the event
- Systems/data affected
- Actions already taken
- Reporter contact information

### Phase 2: Triage & Classification

Within 1 hour of report:
- [ ] Acknowledge receipt to reporter
- [ ] Assign incident number (INC-YYYY-NNNN)
- [ ] Determine severity level
- [ ] Assign incident owner
- [ ] Create incident ticket
- [ ] Notify appropriate team members

### Phase 3: Containment

**Immediate actions to limit damage:**

| Priority | Action |
|----------|--------|
| 1 | Isolate affected systems |
| 2 | Preserve evidence (logs, memory) |
| 3 | Block malicious IPs/accounts |
| 4 | Change compromised credentials |
| 5 | Enable enhanced monitoring |

**Do NOT:**
- Power off systems (preserves memory evidence)
- Delete logs or files
- Communicate externally without approval
- Make changes without documentation

### Phase 4: Investigation

- [ ] Collect and preserve evidence
- [ ] Analyze logs (access, application, system)
- [ ] Determine attack vector
- [ ] Identify scope of compromise
- [ ] Document timeline of events
- [ ] Assess data exposure

### Phase 5: Eradication

- [ ] Remove malware/backdoors
- [ ] Patch vulnerabilities
- [ ] Reset affected credentials
- [ ] Rebuild compromised systems
- [ ] Verify clean state

### Phase 6: Recovery

- [ ] Restore systems from clean backups
- [ ] Verify system functionality
- [ ] Enable monitoring
- [ ] Gradual return to production
- [ ] Confirm no reinfection

### Phase 7: Post-Incident

Within 5 business days:
- [ ] Conduct post-incident review
- [ ] Document lessons learned
- [ ] Update procedures if needed
- [ ] Implement preventive measures
- [ ] Close incident ticket

---

## 7. Communication

### 7.1 Internal Communication

| Audience | When | Channel |
|----------|------|---------|
| Incident Team | Immediately | Slack, Phone |
| Executive Team | P1/P2 within 1 hour | Email, Phone |
| All Staff | As needed | Email |
| Board | P1 breaches | Executive briefing |

### 7.2 External Communication

| Stakeholder | Requirement | Timing |
|-------------|-------------|--------|
| Affected Customers | Required for breaches | Per regulatory requirements |
| Regulators (HHS) | PHI breaches >500 individuals | 60 days |
| Regulators (State) | Varies by state | 30-60 days |
| Law Enforcement | If criminal activity | As appropriate |
| Media | Only via Communications Lead | As needed |

**All external communications require Legal and Executive approval.**

---

## 8. Evidence Handling

### 8.1 Evidence Collection

- Preserve original evidence (do not modify)
- Create forensic copies for analysis
- Document chain of custody
- Store securely with limited access
- Retain for minimum 7 years

### 8.2 Evidence Types

| Type | Collection Method |
|------|-------------------|
| Logs | Export to secure storage |
| Memory | Memory dump before shutdown |
| Disk | Forensic image |
| Network | Packet captures |
| Screenshots | Timestamped images |

---

## 9. HIPAA Breach Notification

### 9.1 Breach Determination

A breach is presumed unless low probability of compromise based on:
1. Nature and extent of PHI involved
2. Unauthorized person who used/accessed PHI
3. Whether PHI was actually acquired or viewed
4. Extent to which risk has been mitigated

### 9.2 Notification Requirements

| Affected Individuals | Notification Method | Timing |
|---------------------|---------------------|--------|
| < 500 | Individual notice | 60 days |
| >= 500 | Individual + Media + HHS | 60 days |
| Unable to contact | Substitute notice | 60 days |

### 9.3 Notification Content

- Description of what happened
- Types of information involved
- Steps individuals should take
- What we are doing in response
- Contact information

---

## 10. Incident Documentation

### 10.1 Required Documentation

- Incident report form
- Timeline of events
- Evidence inventory
- Communication log
- Remediation actions
- Post-incident review

### 10.2 Incident Report Template

```
INCIDENT REPORT

Incident ID: INC-YYYY-NNNN
Severity: P1/P2/P3/P4
Status: Open/Investigating/Contained/Resolved/Closed

SUMMARY
Date Detected:
Date Reported:
Reported By:
Description:

IMPACT
Systems Affected:
Data Affected:
Users Affected:
Business Impact:

TIMELINE
[Date/Time] - Event description

CONTAINMENT ACTIONS
1.
2.

ROOT CAUSE

REMEDIATION

LESSONS LEARNED

```

---

## 11. Metrics & Reporting

### 11.1 Key Metrics

| Metric | Target |
|--------|--------|
| Mean Time to Detect (MTTD) | < 24 hours |
| Mean Time to Contain (MTTC) | < 4 hours (P1), < 24 hours (P2) |
| Mean Time to Resolve (MTTR) | < 48 hours (P1), < 1 week (P2) |
| Incidents per month | Track trend |

### 11.2 Reporting

- Weekly: Active incident summary
- Monthly: Incident metrics report
- Quarterly: Trend analysis
- Annually: Program review

---

## 12. Training & Testing

### 12.1 Training Requirements

- Incident response training for all team members
- Annual tabletop exercises
- Technical training for responders

### 12.2 Testing

| Test Type | Frequency |
|-----------|-----------|
| Tabletop Exercise | Quarterly |
| Technical Drill | Semi-annually |
| Full Simulation | Annually |

---

## 13. Continuous Improvement

After each incident:
1. Conduct blameless post-mortem
2. Identify root causes
3. Document lessons learned
4. Update procedures
5. Implement preventive controls
6. Share learnings (appropriately)

---

## Appendix A: Contact List

| Role | Name | Phone | Email |
|------|------|-------|-------|
| Security Lead | | | |
| On-Call | | | |
| Legal | | | |
| Executive | | | |
| PR/Communications | | | |

---

## Appendix B: Escalation Matrix

```
        ┌─────────────────────────────────────────────┐
        │              ALL INCIDENTS                   │
        │         Report to Security Team              │
        └─────────────────────────────────────────────┘
                           │
           ┌───────────────┼───────────────┐
           ▼               ▼               ▼
      ┌─────────┐    ┌──────────┐    ┌──────────┐
      │   P4    │    │  P3/P2   │    │    P1    │
      │  Low    │    │ Med/High │    │ Critical │
      └─────────┘    └──────────┘    └──────────┘
           │               │               │
           ▼               ▼               ▼
      Security        Security +      Incident
       Team           On-Call        Commander +
                                     Executive
```

---

*Document Classification: Internal*
*Next Review Date: December 2026*
