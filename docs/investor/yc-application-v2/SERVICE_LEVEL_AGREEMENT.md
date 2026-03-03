# HDIM Service Level Agreement

**Commitment to Reliability and Support**

*Version 1.0 | December 2025*

---

## 1. Overview

This Service Level Agreement ("SLA") describes the service levels Health Data In Motion ("HDIM", "we", "us") commits to providing for our healthcare quality measurement platform. This SLA applies to all customers with an active subscription.

**Our Commitment:**
> We understand that healthcare organizations depend on HDIM for quality measurement that affects patient care and reimbursement. We commit to providing reliable, responsive, and secure service.

---

## 2. Service Availability

### 2.1 Uptime Commitment

| Tier | Monthly Uptime | Annual Downtime | Measurement |
|------|----------------|-----------------|-------------|
| **Community** | 99.5% | ~3.6 hours/month | Best effort |
| **Professional** | 99.9% | ~43 min/month | Guaranteed |
| **Enterprise** | 99.9% | ~43 min/month | Guaranteed |
| **Enterprise Plus** | 99.95% | ~22 min/month | Guaranteed |
| **Dedicated SaaS** | 99.95% | ~22 min/month | Guaranteed |

### 2.2 Uptime Calculation

```
                    Total Minutes - Downtime Minutes
Uptime Percentage = ─────────────────────────────────── × 100
                          Total Minutes

Monthly Calculation Period: Calendar month (first day 00:00 UTC to last day 23:59 UTC)
```

**What Counts as Downtime:**
- ✅ HDIM platform unavailable (HTTP 5xx errors)
- ✅ API response time >30 seconds
- ✅ Login/authentication failures (HDIM-caused)
- ✅ Data not accessible or corrupted

**What Does NOT Count as Downtime:**
- ❌ Scheduled maintenance (with notice)
- ❌ Customer-caused issues (misconfiguration, exceeded limits)
- ❌ Third-party service failures (customer's EHR, ISP)
- ❌ Force majeure events
- ❌ Beta/preview features

### 2.3 Availability by Component

| Component | Target | Measurement |
|-----------|--------|-------------|
| **Web Portal** | 99.9% | Synthetic monitoring |
| **API Gateway** | 99.9% | Health checks |
| **CQL Engine** | 99.9% | Transaction success rate |
| **Database** | 99.99% | AWS RDS metrics |
| **Authentication** | 99.9% | Login success rate |

---

## 3. Performance Standards

### 3.1 Response Time Targets

| Operation | Target | P95 | P99 |
|-----------|--------|-----|-----|
| **Page Load** | <2 seconds | <3s | <5s |
| **API Response** | <500ms | <1s | <2s |
| **CQL Evaluation** | <200ms | <500ms | <1s |
| **Report Generation** | <10 seconds | <20s | <30s |
| **Data Export (1K records)** | <5 seconds | <10s | <15s |
| **Search** | <1 second | <2s | <3s |

### 3.2 Throughput Guarantees

| Tier | API Requests/min | Concurrent Users | Data Ingestion |
|------|------------------|------------------|----------------|
| Community | 100 | 5 | 1,000 records/hr |
| Professional | 500 | 25 | 10,000 records/hr |
| Enterprise | 2,000 | 100 | 50,000 records/hr |
| Enterprise Plus | 5,000 | Unlimited | 200,000 records/hr |

### 3.3 Data Durability

| Metric | Commitment |
|--------|------------|
| **Data Durability** | 99.999999999% (11 9's) |
| **Backup Frequency** | Daily automated |
| **Backup Retention** | 30 days (90 days Enterprise+) |
| **Point-in-Time Recovery** | Yes (Enterprise+) |
| **Cross-Region Replication** | Yes (Enterprise+) |

---

## 4. Support Services

### 4.1 Support Channels

| Channel | Community | Professional | Enterprise | Enterprise Plus |
|---------|-----------|--------------|------------|-----------------|
| **Documentation** | ✅ | ✅ | ✅ | ✅ |
| **Community Forum** | ✅ | ✅ | ✅ | ✅ |
| **Email Support** | ✅ | ✅ | ✅ | ✅ |
| **Chat Support** | ❌ | ✅ | ✅ | ✅ |
| **Phone Support** | ❌ | ✅ | ✅ | ✅ |
| **Dedicated CSM** | ❌ | ❌ | ✅ | ✅ |
| **24/7 Emergency** | ❌ | ❌ | ❌ | ✅ |
| **On-Site Support** | ❌ | ❌ | ❌ | Available |

### 4.2 Support Hours

| Tier | Hours | Days | Timezone |
|------|-------|------|----------|
| Community | 9am-5pm | Mon-Fri | US Eastern |
| Professional | 8am-8pm | Mon-Fri | US Eastern |
| Enterprise | 7am-9pm | Mon-Fri | US Eastern |
| Enterprise Plus | 24/7 | Every day | Global |

### 4.3 Holiday Schedule

Support is reduced on the following US holidays:
- New Year's Day
- Memorial Day
- Independence Day
- Labor Day
- Thanksgiving Day
- Christmas Day

*Enterprise Plus customers receive 24/7 coverage including holidays.*

---

## 5. Incident Management

### 5.1 Incident Severity Levels

| Severity | Definition | Examples |
|----------|------------|----------|
| **P1 - Critical** | Complete service outage or data breach | Platform down, security incident |
| **P2 - High** | Major feature unavailable, significant performance degradation | API failures, login issues |
| **P3 - Medium** | Feature partially impaired, workaround available | Report errors, slow performance |
| **P4 - Low** | Minor issue, cosmetic defect | UI bugs, documentation errors |

### 5.2 Response Time Commitments

| Severity | Community | Professional | Enterprise | Enterprise Plus |
|----------|-----------|--------------|------------|-----------------|
| **P1 - Critical** | 8 hours | 2 hours | 1 hour | 15 minutes |
| **P2 - High** | 24 hours | 8 hours | 4 hours | 1 hour |
| **P3 - Medium** | 72 hours | 24 hours | 8 hours | 4 hours |
| **P4 - Low** | 5 days | 72 hours | 48 hours | 24 hours |

*Response time = time from ticket creation to first substantive response from HDIM support.*

### 5.3 Resolution Time Targets

| Severity | Target Resolution | Maximum |
|----------|-------------------|---------|
| **P1 - Critical** | 4 hours | 8 hours |
| **P2 - High** | 8 hours | 24 hours |
| **P3 - Medium** | 3 business days | 5 business days |
| **P4 - Low** | 10 business days | 20 business days |

*Resolution = issue fixed OR workaround provided OR root cause identified with timeline.*

### 5.4 Escalation Process

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         INCIDENT ESCALATION                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   TIER 1: Support Engineer                                                  │
│   ├── Initial response                                                      │
│   ├── Known issue resolution                                                │
│   └── Escalate if: unresolved after 30 min (P1) or 2 hours (P2)            │
│                                                                             │
│   TIER 2: Senior Engineer                                                   │
│   ├── Complex technical issues                                              │
│   ├── Performance investigations                                            │
│   └── Escalate if: unresolved after 2 hours (P1) or 4 hours (P2)           │
│                                                                             │
│   TIER 3: Engineering Lead                                                  │
│   ├── Architecture-level issues                                             │
│   ├── Code-level fixes                                                      │
│   └── Escalate if: unresolved after 4 hours (P1)                           │
│                                                                             │
│   EXECUTIVE: CTO / CEO                                                      │
│   ├── Extended outages (>4 hours)                                           │
│   ├── Security incidents                                                    │
│   └── Customer communication                                                │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.5 Incident Communication

| Event | Channel | Timing |
|-------|---------|--------|
| **Incident Detected** | Status page, email | Within 15 minutes |
| **Updates During Incident** | Status page | Every 30 minutes |
| **Resolution** | Status page, email | Within 1 hour of fix |
| **Post-Mortem (P1/P2)** | Email to affected customers | Within 5 business days |

**Status Page:** status.healthdatainmotion.com

---

## 6. Maintenance Windows

### 6.1 Scheduled Maintenance

| Type | Frequency | Duration | Notice |
|------|-----------|----------|--------|
| **Standard Updates** | Weekly | <15 min | 48 hours |
| **Major Updates** | Monthly | <1 hour | 7 days |
| **Infrastructure** | Quarterly | <4 hours | 14 days |
| **Emergency** | As needed | Varies | ASAP |

### 6.2 Maintenance Schedule

**Standard Maintenance Window:**
- **Day:** Sunday
- **Time:** 2:00 AM - 6:00 AM US Eastern
- **Typical Duration:** 15-30 minutes

**Maintenance Notification:**
- Email to admin contacts
- In-app banner (24 hours prior)
- Status page announcement

### 6.3 Zero-Downtime Deployments

Most updates are deployed with zero downtime using:
- Rolling deployments
- Blue-green infrastructure
- Database migrations during low traffic

**Downtime Required For:**
- Major database schema changes
- Infrastructure upgrades
- Security patches requiring restart

---

## 7. Service Credits

### 7.1 Credit Calculation

If HDIM fails to meet uptime commitments, customers receive service credits:

| Uptime Achieved | Credit (% of Monthly Fee) |
|-----------------|---------------------------|
| 99.9% - 99.5% | 10% |
| 99.5% - 99.0% | 25% |
| 99.0% - 95.0% | 50% |
| Below 95.0% | 100% |

**Example:**
- Enterprise customer pays $999/month
- Uptime was 99.7% (below 99.9% commitment)
- Credit = $999 × 10% = $99.90

### 7.2 Credit Request Process

1. **Submit Request:** Email support@healthdatainmotion.com within 30 days of incident
2. **Include:** Account name, incident date/time, description
3. **Review:** HDIM validates against monitoring data
4. **Credit Applied:** Within 30 days, applied to next invoice

### 7.3 Credit Limitations

- Maximum credit: 100% of monthly fee
- Credits do not accumulate month-to-month
- Credits cannot be redeemed for cash
- Credits void if customer is in breach of terms
- Excludes scheduled maintenance and excluded events

---

## 8. Data Protection SLA

### 8.1 Backup Commitments

| Commitment | Standard | Enterprise+ |
|------------|----------|-------------|
| **Backup Frequency** | Daily | Continuous (PITR) |
| **Retention** | 30 days | 90 days |
| **Recovery Point Objective (RPO)** | 24 hours | 1 hour |
| **Recovery Time Objective (RTO)** | 4 hours | 1 hour |
| **Geographic Redundancy** | Single region | Multi-region |

### 8.2 Data Recovery

| Scenario | Recovery Time | Process |
|----------|---------------|---------|
| **Accidental Deletion (user data)** | <4 hours | Support ticket, restore from backup |
| **Accidental Deletion (config)** | <1 hour | Self-service or support |
| **Data Corruption** | <4 hours | Support ticket, point-in-time restore |
| **Complete Disaster** | <8 hours | Failover to DR region |

### 8.3 Data Export

Customers can export their data at any time:
- **Format:** FHIR R4, CSV, JSON
- **Frequency:** Unlimited
- **Cost:** Included
- **Timeline:** Immediate (small), up to 24 hours (large exports)

---

## 9. Security SLA

### 9.1 Security Response Times

| Event Type | Response Time | Resolution Target |
|------------|---------------|-------------------|
| **Critical Vulnerability (CVE 9.0+)** | 4 hours | 24 hours |
| **High Vulnerability (CVE 7.0-8.9)** | 24 hours | 7 days |
| **Medium Vulnerability (CVE 4.0-6.9)** | 72 hours | 30 days |
| **Low Vulnerability (CVE <4.0)** | 7 days | 90 days |

### 9.2 Security Incident Response

| Phase | Timeline |
|-------|----------|
| **Detection & Triage** | <1 hour |
| **Initial Containment** | <2 hours |
| **Customer Notification (if affected)** | <24 hours |
| **Full Investigation** | <72 hours |
| **Remediation Complete** | Varies by severity |
| **Post-Incident Report** | <7 days |

### 9.3 Compliance Commitments

| Compliance | Status | Audit Frequency |
|------------|--------|-----------------|
| **HIPAA Technical Safeguards** | ✅ Compliant | Continuous |
| **SOC 2 Type I** | 🔄 Q2 2025 | Annual |
| **SOC 2 Type II** | 📋 Planned | Annual |
| **Penetration Testing** | ✅ Annual | Annual |
| **Vulnerability Scanning** | ✅ Weekly | Weekly |

---

## 10. Customer Responsibilities

To receive full SLA benefits, customers must:

### 10.1 Technical Requirements

- [ ] Use supported browsers (Chrome, Firefox, Safari, Edge - latest 2 versions)
- [ ] Maintain stable internet connection (>10 Mbps recommended)
- [ ] Keep API integrations updated to supported versions
- [ ] Not exceed documented rate limits

### 10.2 Account Requirements

- [ ] Maintain accurate contact information
- [ ] Designate at least one admin contact
- [ ] Respond to critical security notifications within 24 hours
- [ ] Keep credentials secure (enable MFA)

### 10.3 Reporting Requirements

- [ ] Report issues through official support channels
- [ ] Provide accurate and complete information about issues
- [ ] Cooperate with troubleshooting requests
- [ ] Test issues in supported configuration before reporting

---

## 11. Exclusions

This SLA does not apply to:

### 11.1 Excluded Events

- Force majeure (natural disasters, war, government action)
- Internet backbone failures
- Customer's network or equipment failures
- Customer misconfiguration
- Exceeding usage limits
- Beta, trial, or preview features
- Third-party service outages
- Scheduled maintenance (with proper notice)
- Customer-requested changes
- Suspension due to non-payment or ToS violation

### 11.2 Excluded Services

- Free trial accounts
- Development/sandbox environments
- Beta features (marked as "Beta" in UI)
- API versions beyond end-of-life
- On-premise deployments (separate SLA)

---

## 12. SLA Reporting

### 12.1 Monthly Reports

Enterprise and Enterprise Plus customers receive monthly SLA reports including:

| Metric | Included |
|--------|----------|
| Uptime percentage | ✅ |
| Incident summary | ✅ |
| Response time metrics | ✅ |
| Support ticket summary | ✅ |
| Performance trends | ✅ |

### 12.2 Real-Time Monitoring

All customers can access:
- **Status Page:** status.healthdatainmotion.com
- **Incident History:** Past 90 days
- **Maintenance Calendar:** Upcoming scheduled maintenance
- **Subscribe:** Email/SMS notifications

### 12.3 Quarterly Reviews

Enterprise Plus customers receive quarterly business reviews:
- SLA performance review
- Usage analytics
- Roadmap preview
- Success planning

---

## 13. SLA Tiers Summary

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            SLA COMPARISON                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│                    Community   Professional   Enterprise   Enterprise Plus │
│                    ─────────   ────────────   ──────────   ───────────────  │
│                                                                             │
│   Uptime SLA         99.5%        99.9%         99.9%          99.95%       │
│                                                                             │
│   P1 Response        8 hrs        2 hrs         1 hr           15 min       │
│                                                                             │
│   Support Hours      9-5 ET       8am-8pm       7am-9pm        24/7         │
│                                                                             │
│   Phone Support        ❌           ✅             ✅              ✅         │
│                                                                             │
│   Dedicated CSM        ❌           ❌             ✅              ✅         │
│                                                                             │
│   24/7 Emergency       ❌           ❌             ❌              ✅         │
│                                                                             │
│   Monthly Reports      ❌           ❌             ✅              ✅         │
│                                                                             │
│   Quarterly Reviews    ❌           ❌             ❌              ✅         │
│                                                                             │
│   Service Credits      ❌           ✅             ✅              ✅         │
│                                                                             │
│   Price             $49/mo      $299/mo       $999/mo       $2,499/mo      │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 14. Contact Information

### 14.1 Support Contacts

| Purpose | Contact |
|---------|---------|
| **General Support** | support@healthdatainmotion.com |
| **Emergency (P1)** | +1-XXX-XXX-XXXX (Enterprise Plus) |
| **Security Issues** | security@healthdatainmotion.com |
| **Billing Questions** | billing@healthdatainmotion.com |
| **SLA Credit Requests** | support@healthdatainmotion.com |

### 14.2 Escalation Contacts

| Level | Contact | When to Use |
|-------|---------|-------------|
| **Support Manager** | support-manager@healthdatainmotion.com | Unresolved after 24 hours |
| **Customer Success** | cs@healthdatainmotion.com | Relationship issues |
| **Executive** | executive@healthdatainmotion.com | Critical unresolved issues |

---

## 15. SLA Amendments

### 15.1 Changes to SLA

- HDIM may update this SLA with 30 days notice
- Material changes require 60 days notice
- Customers notified via email and in-app
- Continued use constitutes acceptance

### 15.2 Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | December 2025 | Initial release |

---

## Appendix A: Definitions

| Term | Definition |
|------|------------|
| **Downtime** | Period when service is unavailable (>5% error rate or >30s response) |
| **Response Time** | Time from ticket creation to first substantive response |
| **Resolution Time** | Time from ticket creation to issue resolved or workaround provided |
| **Scheduled Maintenance** | Planned downtime with advance notice |
| **Emergency Maintenance** | Unplanned maintenance for security or stability |
| **Business Day** | Monday-Friday, excluding US federal holidays |
| **Business Hours** | As defined per tier in Section 4.2 |

## Appendix B: Service Level Objectives (Internal)

*These are internal targets that exceed customer SLAs:*

| Metric | Internal Target | Customer SLA |
|--------|-----------------|--------------|
| Uptime | 99.99% | 99.9-99.95% |
| P1 Response | 5 minutes | 15 min - 8 hrs |
| API Latency (P95) | <200ms | <500ms |
| Page Load | <1 second | <2 seconds |

---

*Service Level Agreement Version: 1.0*
*Effective Date: January 1, 2025*
*Last Updated: December 2025*

**Acceptance:** By using HDIM services, you agree to this SLA. For questions, contact support@healthdatainmotion.com.
