# HDIM Support Playbook

> Ticket triage, SLA definitions, knowledge base structure, escalation procedures, and common issue resolution.

---

## Support Mission

**Goal:** Provide responsive, expert support that helps customers achieve their quality measurement goals while maintaining high satisfaction.

**Key Metrics:**
- First Response Time: <4 hours (business hours)
- Resolution Time: <24 hours (P1/P2)
- Customer Satisfaction (CSAT): 95%+
- First Contact Resolution: 70%+

---

## Support Channels

### Available Channels

| Channel | Availability | Best For |
|---------|--------------|----------|
| Email | 24/7 (response during business hours) | Complex issues, documentation needed |
| In-App Chat | M-F 8am-8pm ET | Quick questions, real-time help |
| Help Center | 24/7 self-service | Common questions, how-tos |
| Phone | Enterprise tier only | Urgent issues, complex discussions |
| Emergency Line | 24/7 for P1 | Service outages, security incidents |

### Contact Information

| Channel | Contact |
|---------|---------|
| Email | support@healthdatainmotion.com |
| Help Center | help.healthdatainmotion.com |
| Chat | In-app widget (when logged in) |
| Emergency | (888) 555-HDIM + press 1 |

---

## Ticket Priority & SLAs

### Priority Definitions

| Priority | Description | Examples |
|----------|-------------|----------|
| **P1 - Critical** | Service completely unavailable, security incident | Platform down, data breach, cannot access any features |
| **P2 - High** | Major feature broken, significant business impact | Dashboard not loading, data sync failed, calculation errors |
| **P3 - Medium** | Feature impaired, workaround available | Slow performance, minor bugs, configuration issues |
| **P4 - Low** | Minor issues, questions, feature requests | How-to questions, cosmetic issues, enhancements |

### SLA by Priority

| Priority | First Response | Resolution Target | Update Frequency |
|----------|----------------|-------------------|------------------|
| P1 | 1 hour | 4 hours | Every 30 min |
| P2 | 4 hours | 24 hours | Every 4 hours |
| P3 | 8 hours | 72 hours | Every 24 hours |
| P4 | 24 hours | 5 business days | As needed |

### SLA by Customer Tier

| Tier | P1 Response | P2 Response | Support Hours |
|------|-------------|-------------|---------------|
| Enterprise | 30 min | 2 hours | 24/7 |
| Professional | 1 hour | 4 hours | Extended (6am-10pm ET) |
| Community | 4 hours | 8 hours | Business hours |

---

## Ticket Triage Process

### Step 1: Initial Assessment

**Upon Ticket Receipt:**
1. Read full description
2. Check customer tier (for SLA)
3. Assign priority level
4. Categorize issue type
5. Check for duplicates
6. Assign to appropriate queue

### Step 2: Priority Assignment Matrix

| Impact / Urgency | Critical Business Impact | High Impact | Medium Impact | Low Impact |
|------------------|--------------------------|-------------|---------------|------------|
| **Immediate Need** | P1 | P2 | P3 | P3 |
| **Same Day** | P2 | P2 | P3 | P4 |
| **This Week** | P2 | P3 | P4 | P4 |
| **No Rush** | P3 | P4 | P4 | P4 |

### Step 3: Issue Categorization

| Category | Description | Routing |
|----------|-------------|---------|
| Access/Login | Authentication, permissions | Tier 1 |
| Data | Import, export, sync, quality | Tier 1/2 |
| Integration | EHR connections, API, FHIR | Tier 2 |
| Performance | Slow, errors, timeouts | Tier 2 |
| Billing | Invoices, payments, plans | Billing team |
| Feature Request | New functionality | Product team |
| Security | Security concerns | Security team |

### Step 4: Auto-Response

**Template:**
```
Subject: Re: [#{{ticket.id}}] {{ticket.subject}}

Hi {{customer.first_name}},

Thank you for contacting HDIM Support. We've received your request and a team member will respond within {{sla.first_response}}.

Ticket Details:
- Ticket ID: #{{ticket.id}}
- Priority: {{ticket.priority}}
- Category: {{ticket.category}}

In the meantime, you might find these resources helpful:
- Help Center: help.healthdatainmotion.com
- Status Page: status.healthdatainmotion.com

If this is an emergency, please call (888) 555-HDIM.

Best,
HDIM Support Team
```

---

## Escalation Procedures

### Technical Escalation

| Level | Who | When | Criteria |
|-------|-----|------|----------|
| Tier 1 | Support Specialist | First contact | Standard issues, how-tos |
| Tier 2 | Senior Support | Tier 1 cannot resolve in 30 min | Complex issues, integration problems |
| Tier 3 | Engineering | Tier 2 identifies bug/code issue | Bugs, performance, architecture |
| Tier 4 | Engineering Lead | Critical bugs, P1 incidents | Major outages, data issues |

### Customer Escalation

| Trigger | Action | Who Notified |
|---------|--------|--------------|
| Customer requests manager | Assign to Support Lead | Support Lead |
| SLA breach | Automatic escalation | Support Manager |
| Repeated issues (3+ same problem) | Review and escalate | Support Lead + CSM |
| Executive complaint | Immediate escalation | VP Support + CEO |

### Escalation Template

```
ESCALATION REQUEST

Ticket ID: #{{ticket.id}}
Customer: {{customer.name}}
Tier: {{customer.tier}}
Current Priority: {{ticket.priority}}

ESCALATION REASON:
[ ] Technical complexity
[ ] Customer request
[ ] SLA at risk
[ ] Multiple occurrences
[ ] Security concern
[ ] Other: ___________

SUMMARY:
[Brief description of issue]

ATTEMPTED SOLUTIONS:
1. [What was tried]
2. [What was tried]
3. [What was tried]

REQUESTED ACTION:
[What is needed from escalation recipient]

TIMELINE:
- Issue reported: [Time]
- Current duration: [Hours/days]
- SLA deadline: [Time]

ATTACHMENTS:
- [Logs, screenshots, etc.]
```

---

## Common Issues & Resolution Guides

### Issue 1: Login/Authentication Problems

**Symptoms:**
- Cannot log in
- "Invalid credentials" error
- MFA not working
- Session expired

**Troubleshooting Steps:**

1. **Verify Credentials**
   - Confirm email address is correct
   - Check for caps lock
   - Try password reset

2. **Check Account Status**
   - Is account active? (Admin panel)
   - Is user provisioned? (User management)
   - Are there access restrictions? (IP allowlist)

3. **MFA Issues**
   - Time sync on authenticator app
   - Try backup codes
   - CSM can reset MFA if needed

4. **Browser/Cache**
   - Clear browser cache and cookies
   - Try incognito/private mode
   - Try different browser

**Resolution:**
- Password reset: Self-service or admin reset
- MFA reset: Admin or support can disable/reset
- Account unlock: Admin or support can unlock

---

### Issue 2: Data Not Syncing

**Symptoms:**
- Dashboard shows old data
- New patients not appearing
- Observations not updating

**Troubleshooting Steps:**

1. **Check Integration Status**
   - Navigate to Settings > Integrations
   - Verify connection status is "Connected"
   - Check last sync timestamp

2. **Review Sync Logs**
   - Settings > Integrations > [Connection] > Logs
   - Look for error messages
   - Note any failed records

3. **Validate Source Data**
   - Confirm data exists in source system
   - Check data format matches requirements
   - Verify patient identifiers

4. **Trigger Manual Sync**
   - Settings > Integrations > [Connection] > Sync Now
   - Wait 5-10 minutes
   - Check sync logs for results

**Common Causes:**
- API credentials expired → Re-authenticate
- Rate limiting → Wait and retry
- Data format changed → Review mapping
- Source system down → Check source status

**Resolution:**
- Credential issues: Re-establish connection
- Data mapping: Update configuration
- Rate limits: Adjust sync frequency
- Escalate to Tier 2 if: Logs show unknown errors

---

### Issue 3: Incorrect Measure Calculations

**Symptoms:**
- Quality scores seem wrong
- Patients incorrectly included/excluded
- Numerator/denominator don't match expectations

**Troubleshooting Steps:**

1. **Understand the Question**
   - Which measure(s)?
   - Which patients?
   - What is expected vs. actual?

2. **Check Measure Definition**
   - Review measure specification
   - Confirm customer understands inclusion criteria
   - Compare to NCQA/CMS specs

3. **Review Patient Data**
   - Open patient detail in HDIM
   - Check relevant clinical data
   - Verify data completeness

4. **Check Exclusions**
   - Review applicable exclusions
   - Verify exclusion criteria met
   - Check documentation

5. **Validate Calculation**
   - Use measure audit tool
   - Compare to manual calculation
   - Document discrepancy

**Common Causes:**
- Missing data → Check integration
- Incorrect coding → Review source data
- Exclusion applied → Explain to customer
- Actual bug → Escalate to Engineering

**Resolution:**
- Data issue: Fix in source system
- Understanding issue: Education
- Bug: Escalate with patient examples

---

### Issue 4: Dashboard Not Loading

**Symptoms:**
- Blank dashboard
- Spinning loader indefinitely
- Error message displayed

**Troubleshooting Steps:**

1. **Check Status Page**
   - status.healthdatainmotion.com
   - Any ongoing incidents?

2. **Browser Troubleshooting**
   - Hard refresh (Ctrl+Shift+R)
   - Clear cache
   - Try incognito mode
   - Try different browser

3. **Network Check**
   - Are other websites working?
   - Any corporate firewall/proxy?
   - VPN issues?

4. **User-Specific**
   - Does it work for other users?
   - Check user permissions
   - Try different device

**Common Causes:**
- Browser cache → Clear cache
- Network restriction → Whitelist domains
- Service issue → Check status page
- Permission issue → Verify access

**Resolution:**
- Cache: Guide customer through clearing
- Network: Provide domains to whitelist
- Service: Communicate incident status
- Permissions: Adjust user settings

---

### Issue 5: Integration Setup Failures

**Symptoms:**
- Cannot connect to EHR
- OAuth errors
- FHIR connection fails

**Troubleshooting Steps:**

1. **Verify Prerequisites**
   - EHR credentials are valid
   - Required permissions granted
   - API enabled on EHR side

2. **Check Error Messages**
   - Note exact error text
   - Check integration logs
   - Screenshot error screen

3. **Common EHR-Specific Issues**
   - Epic: App Orchard registration required
   - Cerner: Millennium version compatibility
   - athena: API key expiration

4. **Test Connectivity**
   - Can we reach the endpoint?
   - SSL certificate valid?
   - Firewall rules correct?

**Common Causes:**
- Invalid credentials → Re-enter or regenerate
- Expired tokens → Re-authenticate
- Firewall blocking → Add IP exceptions
- EHR-side config → Customer IT action needed

**Resolution:**
- Credential issues: Re-authentication
- Network issues: Provide IPs to whitelist
- EHR config: Provide customer checklist

---

### Issue 6: Slow Performance

**Symptoms:**
- Pages load slowly
- Reports take long time
- General sluggishness

**Troubleshooting Steps:**

1. **Baseline Check**
   - Is it consistently slow or intermittent?
   - Slow for all users or specific ones?
   - Which features are slow?

2. **User-Side Checks**
   - Internet speed test
   - Other applications affected?
   - Browser performance (too many tabs?)

3. **HDIM-Side Checks**
   - Check platform status
   - Review performance dashboards
   - Check for ongoing maintenance

4. **Data-Specific**
   - Large data volume?
   - Complex report?
   - Date range too broad?

**Common Causes:**
- User internet → Local issue
- Large data → Optimize query/filter
- Platform issue → Escalate to Tier 2
- Peak usage → Normal, will resolve

**Resolution:**
- User-side: Recommend fixes
- Data optimization: Suggest filters
- Platform issue: Escalate

---

## Knowledge Base Structure

### Article Categories

| Category | Description | Examples |
|----------|-------------|----------|
| Getting Started | Onboarding, basics | First login, initial setup |
| Features | Feature-specific guides | Dashboard, care gaps, reports |
| Integrations | EHR and data connections | Epic setup, FHIR config |
| Measures | Quality measure info | HEDIS specs, measure details |
| Billing | Account, payments | Upgrade, invoice access |
| Troubleshooting | Common problems | Login issues, sync errors |
| API | Developer resources | API reference, examples |

### Article Template

```markdown
# [Article Title]

## Overview
[1-2 sentence summary of what this article covers]

## Prerequisites
- [What user needs before starting]

## Steps
1. [First step with screenshot if applicable]
2. [Second step]
3. [Third step]

## Common Issues
| Issue | Solution |
|-------|----------|
| [Problem] | [Solution] |

## Related Articles
- [Link to related article]

## Need More Help?
Contact support at support@healthdatainmotion.com
```

### Top 20 Articles to Create

1. How to log in for the first time
2. Setting up your organization
3. Connecting to Epic via FHIR
4. Connecting to athenahealth
5. Uploading data via CSV
6. Understanding your quality dashboard
7. Working with care gap worklists
8. Running quality reports
9. Adding and managing users
10. Understanding HEDIS measures
11. MIPS submission guide
12. Troubleshooting data sync issues
13. Password and MFA management
14. Understanding measure calculations
15. API authentication guide
16. Webhook configuration
17. Exporting data
18. Billing and invoices
19. Account settings and preferences
20. Status page and incident communications

---

## Customer Communication Templates

### Template 1: First Response

```
Subject: Re: [#{{ticket.id}}] {{ticket.subject}}

Hi {{customer.first_name}},

Thank you for reaching out to HDIM Support. I'm {{agent.name}}, and I'll be helping you with this issue.

I understand you're experiencing [brief description of issue]. I'm sorry for any inconvenience this has caused.

[Immediate next step or question to gather more info]

[If asking for more info:]
To help me investigate further, could you please provide:
- [Specific question 1]
- [Specific question 2]

[If providing solution:]
I've looked into this and [solution/explanation].

Please let me know if you have any questions or if there's anything else I can help with.

Best regards,
{{agent.name}}
HDIM Support Team
```

### Template 2: Awaiting Customer Response

```
Subject: Re: [#{{ticket.id}}] Following up on your support request

Hi {{customer.first_name}},

I wanted to follow up on your support request regarding [issue].

To continue helping you, I need the following information:
- [Specific question]

Could you please reply with those details when you have a moment?

If you've already resolved this issue, just let me know and I'll close this ticket.

Best regards,
{{agent.name}}
HDIM Support Team
```

### Template 3: Resolution

```
Subject: Re: [#{{ticket.id}}] Issue Resolved

Hi {{customer.first_name}},

Great news! [Description of resolution].

Here's a summary of what was done:
- [Action taken]

[If applicable: Here's what to do if it happens again: [prevention tips]]

Is there anything else I can help you with? If not, I'll close this ticket in 48 hours.

Thank you for your patience, and please don't hesitate to reach out if you need anything else.

Best regards,
{{agent.name}}
HDIM Support Team
```

### Template 4: Escalation to Engineering

```
Subject: Re: [#{{ticket.id}}] Engineering Investigation in Progress

Hi {{customer.first_name}},

Thank you for your patience while we investigate this issue.

After thorough troubleshooting, I've escalated this to our engineering team for further investigation. They're looking into [brief description].

What to expect:
- Our engineering team will investigate [expected actions]
- I'll provide updates every [timeframe]
- Expected resolution: [if known]

I'll keep you informed of our progress. In the meantime, [any workaround if available].

Best regards,
{{agent.name}}
HDIM Support Team
```

### Template 5: Bug Confirmation

```
Subject: Re: [#{{ticket.id}}] Bug Confirmed - Fix in Progress

Hi {{customer.first_name}},

Thank you for your patience. Our engineering team has confirmed this is a bug in our system, and a fix is being developed.

Bug Details:
- Issue: [Description]
- Impact: [What's affected]
- Workaround: [If available]
- Expected Fix: [Timeline if known]

I'll notify you as soon as the fix is deployed. In the meantime, [workaround instructions if any].

I apologize for any inconvenience this has caused. Thank you for bringing this to our attention—your feedback helps us improve HDIM for everyone.

Best regards,
{{agent.name}}
HDIM Support Team
```

---

## Quality Assurance

### Ticket Quality Criteria

| Criterion | Standard |
|-----------|----------|
| Tone | Professional, empathetic, helpful |
| Accuracy | Information is correct |
| Completeness | All questions addressed |
| Clarity | Easy to understand |
| Grammar/Spelling | Error-free |
| Personalization | Uses customer name, context |
| Resolution | Issue fully resolved or clear next steps |

### QA Scoring

| Score | Rating | Criteria |
|-------|--------|----------|
| 90-100 | Excellent | Exceeds all standards |
| 80-89 | Good | Meets all standards |
| 70-79 | Acceptable | Minor improvements needed |
| <70 | Needs Improvement | Significant coaching needed |

### QA Review Process

- Random sample: 10% of tickets per agent per week
- Reviewed by: Support Lead or QA specialist
- Feedback: Within 48 hours of review
- Coaching: Scheduled as needed

---

## Support Metrics Dashboard

### Real-Time Metrics

| Metric | Target | Alert Threshold |
|--------|--------|-----------------|
| Open tickets | <50 | >100 |
| Tickets in breach | 0 | >0 |
| Average wait time | <30 min | >60 min |
| Chat queue | <3 | >10 |

### Daily Metrics

| Metric | Target |
|--------|--------|
| Tickets created | Trend tracking |
| Tickets resolved | > created |
| First response SLA | >95% |
| Resolution SLA | >90% |
| CSAT | >95% |

### Weekly/Monthly Metrics

| Metric | Target | Review |
|--------|--------|--------|
| First contact resolution | 70%+ | Weekly |
| Average handle time | Benchmark | Weekly |
| Ticket volume trend | Stable/declining | Monthly |
| CSAT trend | Stable/improving | Monthly |
| Top issue categories | Inform product | Monthly |

---

## Team Structure & Responsibilities

### Roles

| Role | Responsibilities |
|------|------------------|
| Support Specialist (Tier 1) | First response, common issues, triage |
| Senior Support (Tier 2) | Complex issues, integrations, escalations |
| Support Lead | Team management, QA, escalation |
| Support Manager | Strategy, metrics, hiring |

### Shift Coverage

| Shift | Hours (ET) | Coverage |
|-------|------------|----------|
| Morning | 6am-2pm | 2 agents |
| Afternoon | 2pm-10pm | 2 agents |
| On-call | 10pm-6am | 1 agent (pager) |
| Weekend | 10am-6pm | 1 agent |

### Handoff Procedures

**End of Shift:**
1. Update all open tickets with status
2. Brief incoming agent on hot issues
3. Transfer any active chats
4. Update handoff log

**Handoff Log Template:**
```
Date: [Date]
From: [Agent]
To: [Agent]

HOT ISSUES:
- Ticket #[ID]: [Brief status, next step]

WAITING ON CUSTOMER:
- Ticket #[ID]: [What we're waiting for]

ESCALATED:
- Ticket #[ID]: [Status with engineering]

NOTES:
[Any other relevant information]
```

---

*Last Updated: December 2025*
*Owner: Support Operations*
*Review Cadence: Monthly*
