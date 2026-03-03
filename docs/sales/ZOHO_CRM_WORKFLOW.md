# HDIM Zoho CRM Workflow Specification

**Version**: 1.0
**Last Updated**: December 28, 2025
**Service**: sales-automation-service (Port 8106)
**Integration**: Zoho CRM API v3

---

## Table of Contents

1. [Overview](#overview)
2. [Lead Scoring Model](#lead-scoring-model)
3. [Lead Stages and Automation](#lead-stages-and-automation)
4. [Email Sequences](#email-sequences)
5. [Zoho CRM Custom Fields](#zoho-crm-custom-fields)
6. [Integration Webhooks](#integration-webhooks)
7. [API Endpoints](#api-endpoints)
8. [Implementation Notes](#implementation-notes)

---

## Overview

This document defines the complete Zoho CRM integration workflow for HDIM's sales automation. The workflow covers lead qualification, stage progression, email automation, and bi-directional data synchronization.

### Architecture

```
+------------------+      Webhooks       +------------------------+
|                  | <------------------ |                        |
|   Zoho CRM       |                     |  sales-automation-     |
|   (Cloud)        | ------------------> |  service               |
|                  |      API Sync       |  (Port 8106)           |
+------------------+                     +------------------------+
                                                    |
                                                    v
                                         +------------------------+
                                         |  PostgreSQL            |
                                         |  (sales_automation_db) |
                                         +------------------------+
```

### Integration Points

| Component | Zoho Module | HDIM Entity | Sync Direction |
|-----------|-------------|-------------|----------------|
| Leads | Leads | Lead | Bi-directional |
| Accounts | Accounts | Account | Bi-directional |
| Contacts | Contacts | Contact | Bi-directional |
| Opportunities | Deals | Opportunity | Bi-directional |
| Activities | Tasks/Events | Activity | Bi-directional |

---

## Lead Scoring Model

### Scoring Criteria Matrix

The lead scoring model evaluates prospects based on five key dimensions aligned with HDIM's ideal customer profile (ICP) for healthcare quality measurement solutions.

| Criteria | Weight | Description | Score Range |
|----------|--------|-------------|-------------|
| **Organization Size** | 30% | Based on bed count (hospitals) or member count (payers/ACOs) | 0-10 |
| **Value-Based Care Contracts** | 25% | Participation in MSSP, ACO REACH, MA Stars, or commercial VBC | 0-10 |
| **Technology Stack** | 20% | EHR system and integration readiness | 0-10 |
| **Timeline to Decision** | 15% | Urgency and buying timeline | 0-10 |
| **Budget Authority** | 10% | Decision-making power and budget control | 0-10 |

### Detailed Scoring Rubrics

#### 1. Organization Size (30% Weight)

**For Health Systems/Hospitals (Bed Count):**

| Bed Count | Raw Score | Weighted Score |
|-----------|-----------|----------------|
| 500+ beds | 10 | 30 |
| 300-499 beds | 8 | 24 |
| 150-299 beds | 6 | 18 |
| 50-149 beds | 4 | 12 |
| 25-49 beds | 2 | 6 |
| < 25 beds | 1 | 3 |

**For Payers/ACOs/Health Plans (Member Count):**

| Member Count | Raw Score | Weighted Score |
|--------------|-----------|----------------|
| 500,000+ members | 10 | 30 |
| 250,000-499,999 members | 8 | 24 |
| 100,000-249,999 members | 6 | 18 |
| 50,000-99,999 members | 5 | 15 |
| 25,000-49,999 members | 4 | 12 |
| 10,000-24,999 members | 3 | 9 |
| < 10,000 members | 2 | 6 |

#### 2. Value-Based Care Contracts (25% Weight)

| VBC Participation | Raw Score | Weighted Score |
|-------------------|-----------|----------------|
| Multiple VBC contracts (3+) | 10 | 25 |
| 2 VBC contracts | 8 | 20 |
| 1 VBC contract (ACO REACH/MSSP) | 7 | 17.5 |
| 1 VBC contract (MA Stars) | 6 | 15 |
| 1 Commercial VBC contract | 5 | 12.5 |
| Planning VBC participation | 3 | 7.5 |
| No VBC participation | 0 | 0 |

**VBC Contract Types:**
- MSSP (Medicare Shared Savings Program)
- ACO REACH (Realizing Equity, Access, and Community Health)
- Medicare Advantage Stars
- Commercial VBC/Alternative Payment Models
- Medicaid VBC Programs
- Bundled Payments

#### 3. Technology Stack (20% Weight)

**EHR System Scoring:**

| EHR System | Integration Readiness | Raw Score | Weighted Score |
|------------|----------------------|-----------|----------------|
| Epic | Native FHIR R4 | 10 | 20 |
| Cerner (Oracle Health) | Native FHIR R4 | 9 | 18 |
| Meditech Expanse | FHIR R4 Available | 7 | 14 |
| AllScripts | Variable FHIR | 6 | 12 |
| athenahealth | API Available | 6 | 12 |
| eClinicalWorks | Limited Integration | 4 | 8 |
| NextGen | Limited Integration | 4 | 8 |
| Multiple EHRs | Complex but high value | 8 | 16 |
| Custom/Legacy | Requires HL7v2/custom | 2 | 4 |
| Unknown | Assessment needed | 3 | 6 |

**Technology Modifiers:**
- +1 point: Has dedicated IT integration team
- +1 point: Previous FHIR implementation experience
- +1 point: Using cloud-based EHR
- -1 point: On-premise only with no cloud plans

#### 4. Timeline to Decision (15% Weight)

| Timeline | Urgency Level | Raw Score | Weighted Score |
|----------|---------------|-----------|----------------|
| Immediate (< 30 days) | Critical | 10 | 15 |
| Short-term (1-3 months) | High | 8 | 12 |
| Medium-term (3-6 months) | Moderate | 6 | 9 |
| Long-term (6-12 months) | Planning | 4 | 6 |
| 12+ months | Exploratory | 2 | 3 |
| No timeline | Unknown | 1 | 1.5 |

**Timeline Indicators:**
- Active RFP process = +2 points
- CMS reporting deadline approaching = +2 points
- Contract renewal imminent = +1 point
- Budget already allocated = +1 point

#### 5. Budget Authority (10% Weight)

| Decision Maker Level | Raw Score | Weighted Score |
|---------------------|-----------|----------------|
| C-Suite (CEO, CFO, CMO, CIO) | 10 | 10 |
| VP Level (VP Quality, VP IT, VP Operations) | 8 | 8 |
| Director Level | 6 | 6 |
| Manager Level | 4 | 4 |
| Individual Contributor | 2 | 2 |
| Unknown | 3 | 3 |

**Budget Authority Modifiers:**
- +2 points: Direct budget control confirmed
- +1 point: Part of buying committee
- +1 point: Has vendor selection authority
- -1 point: Requires board approval for all purchases

### Score Thresholds

| Score Range | Classification | Next Action |
|-------------|----------------|-------------|
| 0-29 | Cold Lead | Add to nurture sequence, no immediate outreach |
| 30-49 | Warm Lead | SDR qualification call, add to engagement sequence |
| **50-69** | **MQL (Marketing Qualified Lead)** | **Immediate SDR outreach, discovery call** |
| **70-100** | **SQL (Sales Qualified Lead)** | **Assign to AE, create opportunity** |

### Lead Score Formula

```javascript
// Lead Score Calculation
function calculateLeadScore(lead) {
    let score = 0;

    // Organization Size (30%)
    score += (getOrganizationSizeScore(lead) * 0.30);

    // VBC Contracts (25%)
    score += (getVBCScore(lead) * 0.25);

    // Technology Stack (20%)
    score += (getTechnologyScore(lead) * 0.20);

    // Timeline (15%)
    score += (getTimelineScore(lead) * 0.15);

    // Budget Authority (10%)
    score += (getBudgetAuthorityScore(lead) * 0.10);

    // Normalize to 0-100 scale
    return Math.min(Math.round(score * 10), 100);
}
```

---

## Lead Stages and Automation

### Stage Pipeline

```
+-------------+     +-------------+     +-------------+     +------------------+
|  Stage 1    | --> |  Stage 2    | --> |  Stage 3    | --> |    Stage 4       |
|  NEW LEAD   |     |     MQL     |     |     SQL     |     | DEMO SCHEDULED   |
| Score: Any  |     | Score >= 50 |     | Score >= 70 |     | Demo confirmed   |
+-------------+     +-------------+     +-------------+     +------------------+
                                                                    |
                                                                    v
+-------------+     +--------------+     +-----------------+     +------------------+
|  Stage 7    | <-- |   Stage 6    | <-- |    Stage 5      | <-- |    Stage 4       |
| CLOSED      |     | NEGOTIATION  |     | PROPOSAL SENT   |     | DEMO SCHEDULED   |
| Won/Lost    |     | >$500K alert |     | Track opens     |     | Send prep        |
+-------------+     +--------------+     +-----------------+     +------------------+
```

### Stage 1: New Lead

**Entry Triggers:**
- Form submission (website, landing pages)
- Trade show badge scan import
- Referral entry (partner portal, customer referral)
- Content download (whitepaper, ROI calculator)
- Webinar registration
- LinkedIn Lead Gen form
- Cold outreach response

**Automated Actions:**

| Action | Timing | Owner |
|--------|--------|-------|
| Create lead record in HDIM | Immediate | System |
| Sync to Zoho CRM Leads module | < 1 minute | Integration |
| Calculate initial lead score | Immediate | System |
| Auto-assign to SDR (round-robin) | Immediate | System |
| Send welcome email (Sequence A, Email 1) | Immediate | System |
| Create initial follow-up task | +2 hours | SDR |
| Enrich data (Clearbit/ZoomInfo) | +5 minutes | Integration |
| Add to lead nurture campaign | Immediate | System |
| Log activity: "Lead Created" | Immediate | System |

**Assignment Rules:**

```yaml
lead_assignment:
  strategy: round_robin
  pools:
    - name: enterprise_sdr
      criteria:
        - organization_size >= 250000  # members
        - or organization_type in [PAYER, HEALTH_SYSTEM]
      members: [sdr1, sdr2, sdr3]

    - name: mid_market_sdr
      criteria:
        - organization_size >= 50000
        - organization_size < 250000
      members: [sdr4, sdr5]

    - name: smb_sdr
      criteria:
        - default
      members: [sdr6, sdr7, sdr8]

  fallback: sales_manager
```

**Zoho Workflow Rule:**
```
Trigger: On Lead Create
Conditions: None (all new leads)
Actions:
  1. Field Update: Lead_Stage = "New Lead"
  2. Field Update: Lead_Source_Date = Current Date
  3. Task: "Initial outreach" assigned to Lead Owner, Due: +2 hours
  4. Email Alert: New Lead Notification to SDR
  5. Webhook: POST to sales-automation-service/api/sales/zoho/webhook
```

---

### Stage 2: MQL (Marketing Qualified Lead)

**Entry Triggers:**
- Lead score reaches >= 50
- SDR manually qualifies lead
- High-intent activity (pricing page visit, demo request)

**Exit Criteria to Stage 3 (SQL):**
- Lead score >= 70
- Discovery call completed
- Budget, Authority, Need, Timeline (BANT) confirmed

**Automated Actions:**

| Action | Timing | Owner |
|--------|--------|-------|
| Alert SDR via Slack/Email | Immediate | System |
| Update Zoho Lead Stage | Immediate | Integration |
| Schedule discovery call task | Immediate | System |
| Add to MQL nurture sequence | Immediate | System |
| Track all email opens/clicks | Ongoing | System |
| Update lead score (if activity) | Real-time | System |
| Create "Discovery Call" activity | Immediate | System |

**SDR Tasks:**
1. Review lead score breakdown
2. Research organization (10 min prep)
3. Attempt phone contact (3 attempts)
4. Send personalized email if no phone answer
5. Complete discovery call questionnaire
6. Update qualification notes in Zoho

**Discovery Call Questionnaire:**
- Current HEDIS/quality reporting process
- Pain points with existing solution
- Decision-making process and timeline
- Budget range and fiscal year
- Technical requirements (EHR, integrations)
- Competitive solutions being evaluated

**Zoho Workflow Rule:**
```
Trigger: On Field Update (Lead_Score >= 50)
Conditions: Lead_Stage = "New Lead"
Actions:
  1. Field Update: Lead_Stage = "MQL"
  2. Field Update: MQL_Date = Current Date
  3. Task: "Schedule Discovery Call" assigned to Lead Owner, Due: +24 hours
  4. Email Alert: MQL Alert to SDR Manager
  5. Add to Blueprint: MQL Qualification Process
```

---

### Stage 3: SQL (Sales Qualified Lead)

**Entry Triggers:**
- Lead score >= 70
- Discovery call completed with positive outcome
- BANT criteria confirmed
- SDR qualifies and converts lead

**Exit Criteria to Stage 4 (Demo Scheduled):**
- Demo meeting confirmed on calendar
- Key stakeholders identified
- Demo requirements documented

**Automated Actions:**

| Action | Timing | Owner |
|--------|--------|-------|
| Assign to Account Executive (AE) | Immediate | System |
| Convert Lead to Contact + Account + Opportunity | Immediate | Integration |
| Create Opportunity in Zoho Deals | Immediate | Integration |
| Notify AE via Slack/Email | Immediate | System |
| Schedule demo task | Immediate | System |
| Remove from lead nurture sequence | Immediate | System |
| Add to opportunity pipeline tracking | Immediate | System |
| Log SDR handoff notes | Immediate | SDR |

**Lead Conversion Mapping:**

| Lead Field | Contact Field | Account Field | Opportunity Field |
|------------|---------------|---------------|-------------------|
| First Name | First Name | - | - |
| Last Name | Last Name | - | - |
| Email | Email | - | - |
| Phone | Phone | - | - |
| Company | - | Account Name | - |
| Organization Type | - | Account Type | - |
| Lead Score | - | - | Lead Score |
| - | - | - | Name = "HDIM - {Account Name}" |
| - | - | - | Stage = "Discovery" |
| - | - | - | Amount = Estimated ACV |

**AE Assignment Rules:**

```yaml
ae_assignment:
  strategy: territory_based
  territories:
    - name: enterprise_west
      criteria:
        - organization_size >= 250000
        - state in [CA, WA, OR, NV, AZ, CO, UT]
      ae: ae_enterprise_1

    - name: enterprise_east
      criteria:
        - organization_size >= 250000
        - state in [NY, NJ, MA, CT, PA, FL, GA]
      ae: ae_enterprise_2

    - name: mid_market
      criteria:
        - organization_size >= 50000
        - organization_size < 250000
      ae: ae_midmarket_pool  # round-robin

    - name: smb
      criteria:
        - default
      ae: ae_smb_pool  # round-robin
```

**Zoho Workflow Rule:**
```
Trigger: On Lead Conversion
Conditions: Lead_Score >= 70
Actions:
  1. Create Deal: Stage = "Discovery"
  2. Set Deal Amount: Estimated_ACV from Lead
  3. Task: "Schedule Demo" assigned to Deal Owner, Due: +48 hours
  4. Email Alert: SQL Notification to AE
  5. Webhook: POST to sales-automation-service for opportunity creation
```

---

### Stage 4: Demo Scheduled

**Entry Triggers:**
- Demo meeting confirmed on calendar
- Calendar invite accepted by prospect

**Exit Criteria to Stage 5 (Proposal Sent):**
- Demo completed
- Stakeholder feedback positive
- Proposal requested

**Automated Actions:**

| Action | Timing | Owner |
|--------|--------|-------|
| Send demo prep materials | Immediately on confirmation | System |
| Create calendar event in Zoho | Immediate | Integration |
| Notify demo team (SE, Product) | Immediate | System |
| Send demo reminder to prospect | -24 hours before demo | System |
| Create follow-up task | Demo date + 1 day | System |
| Add to demo follow-up sequence (Sequence B) | Post-demo | System |
| Log demo completion activity | Post-demo | AE |

**Demo Prep Materials Sent:**
- HDIM Platform Overview (PDF)
- Relevant case study based on organization type
- Pre-demo questionnaire (optional)
- Zoom/Teams meeting link

**Demo Types:**

| Demo Type | Duration | Attendees | Focus |
|-----------|----------|-----------|-------|
| Discovery Demo | 30 min | 1-2 stakeholders | High-level overview |
| Technical Demo | 60 min | IT + Clinical | Integration, architecture |
| Executive Demo | 30 min | C-suite | ROI, strategic value |
| Full Platform Demo | 90 min | Full buying committee | Comprehensive walkthrough |
| POC Planning | 45 min | Technical team | Implementation approach |

**Zoho Workflow Rule:**
```
Trigger: On Deal Stage Change to "Demo"
Conditions: Demo_Date is not empty
Actions:
  1. Task: "Prepare Demo Materials" assigned to SE, Due: Demo_Date - 2 days
  2. Task: "Post-Demo Follow-up" assigned to Deal Owner, Due: Demo_Date + 1 day
  3. Email Template: "Demo Confirmation" to Primary Contact
  4. Scheduled Email: "Demo Reminder" to Primary Contact, Send: Demo_Date - 1 day
  5. Webhook: POST to sales-automation-service for demo prep
```

---

### Stage 5: Proposal Sent

**Entry Triggers:**
- Proposal document sent to prospect
- Deal stage updated to "Proposal"

**Exit Criteria to Stage 6 (Negotiation):**
- Proposal reviewed by prospect
- Pricing discussion initiated
- Contract terms requested

**Automated Actions:**

| Action | Timing | Owner |
|--------|--------|-------|
| Track proposal opens/views | Real-time | System |
| Alert AE on first open | Immediate | System |
| Schedule follow-up call | +3 days | System |
| Add to proposal follow-up sequence (Sequence C) | Immediate | System |
| Alert manager if no activity after 7 days | +7 days | System |
| Create proposal revision task if requested | On request | System |

**Proposal Tracking:**
```yaml
proposal_tracking:
  tool: DocuSign / PandaDoc / Zoho Sign
  events_tracked:
    - document_viewed
    - document_forwarded
    - page_viewed (with duration)
    - document_downloaded
    - comment_added

  alerts:
    - type: first_view
      notify: [deal_owner]
      channel: [slack, email]

    - type: no_activity
      threshold: 7 days
      notify: [deal_owner, sales_manager]
      channel: [email]

    - type: forwarded
      notify: [deal_owner]
      channel: [slack]
      action: update_contacts
```

**Zoho Workflow Rule:**
```
Trigger: On Deal Stage Change to "Proposal"
Conditions: Proposal_Sent_Date is not empty
Actions:
  1. Field Update: Proposal_Sent_Date = Current Date
  2. Task: "Proposal Follow-up Call" assigned to Deal Owner, Due: +3 days
  3. Scheduled Action: Alert if no stage change in 7 days
  4. Email Template: "Proposal Sent Confirmation" to Deal Owner
  5. Webhook: POST to sales-automation-service for tracking setup
```

---

### Stage 6: Negotiation

**Entry Triggers:**
- Pricing discussion started
- Contract redlines received
- Legal review requested

**Exit Criteria to Stage 7:**
- Contract signed (Closed Won)
- Deal lost (Closed Lost)

**Automated Actions:**

| Action | Timing | Owner |
|--------|--------|-------|
| Involve VP Sales if deal > $500K | Immediate | System |
| Track contract redlines | Ongoing | System |
| Alert legal if redlines received | On receipt | System |
| Create negotiation summary task | +2 days | System |
| Escalate if stalled > 14 days | +14 days | System |
| Update forecast probability to 70% | Immediate | System |

**Escalation Matrix:**

| Deal Size | Negotiation Authority | Discount Authority | Executive Involvement |
|-----------|----------------------|-------------------|----------------------|
| < $50K | AE | Up to 10% | None |
| $50K - $150K | AE | Up to 15% (manager approval) | VP Sales for > 15% |
| $150K - $500K | AE + Sales Manager | Up to 20% (VP approval) | VP Sales on all calls |
| > $500K | Enterprise Team | VP approval required | CEO involvement optional |

**Contract Negotiation Tracking:**

```yaml
negotiation_tracking:
  fields:
    - original_price
    - proposed_price
    - discount_percentage
    - discount_reason
    - contract_term_changes
    - legal_review_status
    - redline_count
    - days_in_negotiation

  alerts:
    - threshold: 5 redlines
      action: escalate_to_legal

    - threshold: 14 days_in_stage
      action: escalate_to_manager

    - threshold: 25% discount
      action: require_vp_approval
```

**Zoho Workflow Rule:**
```
Trigger: On Deal Stage Change to "Negotiation"
Conditions: Amount >= 500000
Actions:
  1. Email Alert: "High Value Deal Alert" to VP Sales
  2. Add Participant: VP Sales as Deal Participant
  3. Task: "Executive Strategy Call" assigned to VP Sales, Due: +1 day
  4. Field Update: Probability = 70
  5. Webhook: POST to sales-automation-service for leadership notification
```

---

### Stage 7: Closed (Won/Lost)

#### Closed Won

**Entry Triggers:**
- Contract signed
- PO received
- Deal marked as Closed Won

**Automated Actions:**

| Action | Timing | Owner |
|--------|--------|-------|
| Update CRM deal status | Immediate | Integration |
| Convert Account stage to "Customer" | Immediate | System |
| Trigger customer onboarding workflow | Immediate | System |
| Notify Customer Success team | Immediate | System |
| Create onboarding kickoff task | +1 day | System |
| Send welcome email to customer | Immediate | System |
| Remove from all sales sequences | Immediate | System |
| Log revenue recognition date | Immediate | Finance |
| Create implementation project | +1 day | System |

**Customer Handoff Package:**
- Signed contract/SOW
- Discovery call notes
- Demo recordings
- Technical requirements document
- Key stakeholder contact list
- Implementation timeline
- Success criteria defined

**Zoho Workflow Rule:**
```
Trigger: On Deal Stage Change to "Closed Won"
Conditions: None
Actions:
  1. Field Update: Closed_Date = Current Date
  2. Field Update: Account Stage = "Customer"
  3. Email Template: "Welcome to HDIM" to Primary Contact
  4. Task: "Onboarding Kickoff" assigned to CSM, Due: +3 days
  5. Email Alert: "Deal Won!" to Sales Team
  6. Webhook: POST to sales-automation-service for onboarding trigger
  7. Blueprint: Start Customer Onboarding Process
```

#### Closed Lost

**Entry Triggers:**
- Deal lost to competitor
- Prospect went silent
- Budget cut
- Project cancelled
- No decision

**Automated Actions:**

| Action | Timing | Owner |
|--------|--------|-------|
| Update CRM deal status | Immediate | Integration |
| Capture lost reason | Immediate | AE |
| Schedule post-mortem review | +7 days | System |
| Add to re-engagement sequence (Sequence D) | +30 days | System |
| Notify sales management | Immediate | System |
| Update competitive intelligence | +7 days | AE |
| Archive opportunity data | +30 days | System |

**Lost Reason Categories:**

```yaml
lost_reasons:
  - COMPETITOR_CHOSEN:
      competitors: [Inovalon, Cotiviti, Arcadia, Change Healthcare, Other]
      follow_up: 90 days
      re_engage: true

  - BUDGET_CUT:
      follow_up: 180 days
      re_engage: true
      trigger: fiscal_year_start

  - NO_DECISION:
      follow_up: 120 days
      re_engage: true

  - PROJECT_CANCELLED:
      follow_up: 365 days
      re_engage: true

  - TIMING_NOT_RIGHT:
      follow_up: 90 days
      re_engage: true

  - WENT_SILENT:
      follow_up: 60 days
      re_engage: true
      sequence: re_engagement

  - NOT_A_FIT:
      follow_up: null
      re_engage: false
      disqualify: true
```

**Zoho Workflow Rule:**
```
Trigger: On Deal Stage Change to "Closed Lost"
Conditions: None
Actions:
  1. Field Update: Closed_Date = Current Date
  2. Mandatory Field: Lost_Reason (required)
  3. Task: "Loss Analysis" assigned to Deal Owner, Due: +7 days
  4. Email Alert: "Deal Lost" to Sales Manager
  5. Scheduled Action: Add to Re-engagement after 30 days
  6. Webhook: POST to sales-automation-service for analytics
```

---

## Email Sequences

### Sequence A: Inbound Lead Nurture

**Purpose:** Nurture new inbound leads who are not yet sales-ready
**Target:** Leads with score < 50
**Duration:** 21 days (7 emails)
**Enrollment Trigger:** New lead created from inbound source

| Day | Email | Subject Line | Content Focus |
|-----|-------|--------------|---------------|
| 0 | Email 1 | Welcome to HDIM - Your Quality Measurement Journey | Welcome, value proposition overview |
| 3 | Email 2 | How {Organization} Can Improve HEDIS Scores by 15% | Pain point: Quality scores |
| 7 | Email 3 | Case Study: {Similar Org} Achieved 98% Gap Closure | Social proof, case study |
| 10 | Email 4 | The True Cost of Manual HEDIS Reporting | Pain point: Operational burden |
| 14 | Email 5 | See HDIM in Action - Watch Our 3-Minute Demo | Low-commitment engagement |
| 18 | Email 6 | CMS Reporting Deadlines Are Approaching | Urgency, regulatory compliance |
| 21 | Email 7 | Ready to Talk? Let's Schedule 15 Minutes | Direct CTA, meeting request |

**Exit Conditions:**
- Lead score reaches >= 50 (transition to MQL)
- Lead replies to any email
- Lead schedules a meeting
- Lead unsubscribes

**Performance Targets:**

| Metric | Target |
|--------|--------|
| Open Rate | > 25% |
| Click Rate | > 3% |
| Reply Rate | > 2% |
| Meeting Booked | > 5% |

---

### Sequence B: Demo Follow-up

**Purpose:** Maintain engagement after demo, drive to proposal
**Target:** Opportunities in Demo stage (post-demo)
**Duration:** 7 days (3 emails)
**Enrollment Trigger:** Demo completed

| Day | Email | Subject Line | Content Focus |
|-----|-------|--------------|---------------|
| 1 | Email 1 | Great Meeting! Here's Your Personalized HDIM Summary | Demo recap, key points discussed |
| 3 | Email 2 | Your Custom ROI Analysis for {Organization} | Personalized ROI calculator results |
| 7 | Email 3 | Quick Question: What's Your Next Step? | Soft ask for feedback, next steps |

**Personalization Variables:**
- `{{first_name}}` - Contact first name
- `{{organization}}` - Account name
- `{{demo_date}}` - Date of demo
- `{{ae_name}}` - Account Executive name
- `{{key_pain_point}}` - Primary pain point from discovery
- `{{roi_estimate}}` - Calculated ROI figure
- `{{similar_customer}}` - Reference customer in same vertical

**Exit Conditions:**
- Opportunity moves to Proposal stage
- Contact replies
- Contact schedules follow-up call
- 7 days elapsed with no response (escalate to AE)

---

### Sequence C: Proposal Follow-up

**Purpose:** Drive proposal review and advance to negotiation
**Target:** Opportunities in Proposal stage
**Duration:** 14 days (5 emails)
**Enrollment Trigger:** Proposal sent

| Day | Email | Subject Line | Content Focus |
|-----|-------|--------------|---------------|
| 0 | Email 1 | Your HDIM Proposal - What to Expect | Proposal overview, timeline, next steps |
| 3 | Email 2 | Quick Question About Your Proposal | Check-in, offer to address questions |
| 6 | Email 3 | What Our Customers Say About HDIM | Customer testimonials, references |
| 10 | Email 4 | Have Questions? Here Are Common Ones | FAQ, objection handling |
| 14 | Email 5 | Final Check-in: How Can We Help? | Urgency, executive escalation offer |

**Document Tracking Integration:**
- Alert AE when proposal is first opened
- Track time spent on each section
- Alert AE when proposal is forwarded to new recipients
- Auto-update Zoho with engagement metrics

**Exit Conditions:**
- Opportunity moves to Negotiation stage
- Contact replies with questions/feedback
- Proposal signed
- 14 days elapsed (escalate to Sales Manager)

---

### Sequence D: Re-engagement

**Purpose:** Re-engage lost or dormant opportunities
**Target:** Closed Lost opportunities (30+ days), Dormant leads (60+ days)
**Duration:** 30 days (4 emails)
**Enrollment Trigger:** Lost reason not "Not a Fit", 30 days post-close

| Day | Email | Subject Line | Content Focus |
|-----|-------|--------------|---------------|
| 0 | Email 1 | {First_Name}, Things Have Changed at HDIM | New features, updates since last contact |
| 10 | Email 2 | New: {Relevant Feature} Now Available | Feature announcement relevant to their needs |
| 20 | Email 3 | {Similar Customer} Just Joined HDIM | Social proof, peer company success |
| 30 | Email 4 | Worth Another Look? 15 Minutes, No Pressure | Low-commitment re-engagement offer |

**Personalization Based on Lost Reason:**

| Lost Reason | Email Angle |
|-------------|-------------|
| Competitor Chosen | Highlight differentiators, new features |
| Budget Cut | ROI focus, flexible pricing options |
| No Decision | Urgency, regulatory deadlines |
| Timing Not Right | "Is now a better time?" |
| Went Silent | Value reminder, no-pressure check-in |

**Exit Conditions:**
- Contact replies
- Contact books meeting
- Contact unsubscribes
- 30 days elapsed (move to long-term nurture or archive)

---

## Zoho CRM Custom Fields

### Leads Module Custom Fields

| Field API Name | Display Name | Type | Picklist Values / Formula |
|----------------|--------------|------|---------------------------|
| `Organization_Size__c` | Organization Size | Picklist | `<25 beds`, `25-49 beds`, `50-149 beds`, `150-299 beds`, `300-499 beds`, `500+ beds`, `<10K members`, `10-25K members`, `25-50K members`, `50-100K members`, `100-250K members`, `250-500K members`, `500K+ members` |
| `VBC_Contracts__c` | VBC Contracts | Multi-Select Checkbox | `MSSP`, `ACO_REACH`, `MA_Stars`, `Commercial_VBC`, `Medicaid_VBC`, `Bundled_Payments`, `None`, `Planning` |
| `EHR_System__c` | EHR System | Picklist | `Epic`, `Cerner`, `Meditech`, `AllScripts`, `athenahealth`, `eClinicalWorks`, `NextGen`, `Multiple`, `Custom_Legacy`, `Unknown` |
| `Current_HEDIS_Process__c` | Current HEDIS Process | Multi-Line Text | Free text (500 char) |
| `Lead_Score__c` | Lead Score | Formula (Number) | `(Organization_Size_Score * 0.30) + (VBC_Score * 0.25) + (EHR_Score * 0.20) + (Timeline_Score * 0.15) + (Budget_Score * 0.10)` |
| `Estimated_Members__c` | Estimated Members | Number | Integer |
| `Estimated_Beds__c` | Estimated Beds | Number | Integer |
| `Timeline_To_Decision__c` | Timeline to Decision | Picklist | `Immediate (<30 days)`, `Short-term (1-3 months)`, `Medium-term (3-6 months)`, `Long-term (6-12 months)`, `12+ months`, `Unknown` |
| `Budget_Authority_Level__c` | Budget Authority Level | Picklist | `C-Suite`, `VP Level`, `Director`, `Manager`, `Individual Contributor`, `Unknown` |
| `HDIM_Lead_ID` | HDIM Lead ID | Text (100) | UUID from HDIM system |
| `MQL_Date__c` | MQL Date | Date | Auto-set when Lead_Score >= 50 |
| `SQL_Date__c` | SQL Date | Date | Auto-set on conversion |
| `Lead_Stage__c` | Lead Stage | Picklist | `New Lead`, `MQL`, `SQL`, `Converted`, `Disqualified` |
| `FHIR_Readiness__c` | FHIR Readiness | Picklist | `Native FHIR R4`, `FHIR Available`, `Limited Integration`, `HL7v2 Only`, `Unknown` |
| `Active_RFP__c` | Active RFP | Checkbox | Boolean |
| `Competitor_Evaluation__c` | Competitors Being Evaluated | Multi-Select | `Inovalon`, `Cotiviti`, `Arcadia`, `Change Healthcare`, `Innovaccer`, `Health Catalyst`, `None`, `Unknown` |

### Accounts Module Custom Fields

| Field API Name | Display Name | Type | Notes |
|----------------|--------------|------|-------|
| `HDIM_Account_ID` | HDIM Account ID | Text (100) | UUID from HDIM |
| `Account_Type__c` | Account Type | Picklist | `ACO`, `Health System`, `Payer`, `HIE`, `FQHC`, `Clinic`, `Hospital`, `Other` |
| `Total_Members__c` | Total Members | Number | Rollup/manual |
| `Total_Beds__c` | Total Beds | Number | Rollup/manual |
| `Primary_EHR__c` | Primary EHR | Picklist | Same as Lead EHR |
| `VBC_Contracts__c` | VBC Contracts | Multi-Select | Same as Lead |
| `Contract_Value__c` | Contract Value (ACV) | Currency | Annual contract value |
| `Contract_Start_Date__c` | Contract Start Date | Date | - |
| `Contract_End_Date__c` | Contract End Date | Date | - |
| `Health_Score__c` | Customer Health Score | Number | 0-100 (for customers) |
| `NPS_Score__c` | NPS Score | Number | -100 to 100 |

### Contacts Module Custom Fields

| Field API Name | Display Name | Type | Notes |
|----------------|--------------|------|-------|
| `HDIM_Contact_ID` | HDIM Contact ID | Text (100) | UUID from HDIM |
| `Contact_Type__c` | Contact Type | Picklist | `Economic Buyer`, `Technical Buyer`, `End User`, `Champion`, `Influencer`, `Blocker` |
| `Buying_Role__c` | Buying Role | Picklist | `Decision Maker`, `Evaluator`, `Recommender`, `Gatekeeper` |
| `Engagement_Score__c` | Engagement Score | Number | Email/activity engagement |
| `Last_Activity_Date__c` | Last Activity Date | Date | Auto-updated |
| `Preferred_Contact_Method__c` | Preferred Contact Method | Picklist | `Email`, `Phone`, `LinkedIn`, `Text` |

### Deals Module Custom Fields

| Field API Name | Display Name | Type | Notes |
|----------------|--------------|------|-------|
| `HDIM_Opportunity_ID` | HDIM Opportunity ID | Text (100) | UUID from HDIM |
| `Deal_Type__c` | Deal Type | Picklist | `New Business`, `Expansion`, `Renewal`, `Upsell` |
| `Lead_Score_At_Conversion__c` | Lead Score at Conversion | Number | Captured at lead conversion |
| `Demo_Date__c` | Demo Date | Date | Scheduled demo date |
| `Demo_Completed__c` | Demo Completed | Checkbox | Boolean |
| `Demo_Attendees__c` | Demo Attendees | Multi-Select Lookup | Contact lookup |
| `Proposal_Sent_Date__c` | Proposal Sent Date | Date | - |
| `Proposal_Document_Link__c` | Proposal Document Link | URL | Link to proposal doc |
| `Proposal_Views__c` | Proposal Views | Number | From tracking |
| `Contract_Term_Months__c` | Contract Term (Months) | Number | 12, 24, 36, etc. |
| `Discount_Percentage__c` | Discount Percentage | Percent | - |
| `Discount_Reason__c` | Discount Reason | Text | - |
| `Competitor_Lost_To__c` | Competitor Lost To | Picklist | For Closed Lost |
| `Lost_Reason__c` | Lost Reason | Picklist | `Competitor`, `Budget`, `No Decision`, `Timing`, `Not a Fit`, `Other` |
| `Lost_Reason_Details__c` | Lost Reason Details | Multi-Line Text | - |
| `Implementation_Start__c` | Implementation Start Date | Date | For Closed Won |
| `Go_Live_Target__c` | Go-Live Target Date | Date | For Closed Won |
| `Success_Criteria__c` | Success Criteria | Multi-Line Text | Defined outcomes |

---

## Integration Webhooks

### Webhook Configuration

**HDIM Webhook Endpoint:**
```
POST https://api.hdim.health/sales-automation/api/sales/zoho/webhook
```

**Zoho Webhook URL:**
```
https://api.hdim.health/sales-automation/api/sales/zoho/webhook
```

**Authentication:**
```yaml
authentication:
  type: HMAC-SHA256
  header: X-Zoho-Signature
  secret: ${ZOHO_WEBHOOK_SECRET}
```

### Webhook Events

#### 1. New Lead Created

**Trigger:** Lead record created in Zoho CRM
**Direction:** Zoho -> HDIM

```json
{
  "event": "lead.created",
  "module": "Leads",
  "timestamp": "2025-12-28T10:30:00Z",
  "data": {
    "id": "123456789012345678",
    "First_Name": "John",
    "Last_Name": "Smith",
    "Email": "john.smith@hospital.org",
    "Company": "Regional Medical Center",
    "Organization_Size__c": "300-499 beds",
    "EHR_System__c": "Epic",
    "Lead_Source": "Website"
  }
}
```

**HDIM Actions:**
1. Create Lead record in PostgreSQL
2. Calculate lead score
3. Assign to SDR (round-robin)
4. Enrich data via Clearbit/ZoomInfo
5. Add to email sequence if score < 50
6. Sync HDIM_Lead_ID back to Zoho

---

#### 2. Lead Score Changed

**Trigger:** Lead_Score__c field updated
**Direction:** Zoho -> HDIM (or HDIM -> Zoho)

```json
{
  "event": "lead.score_changed",
  "module": "Leads",
  "timestamp": "2025-12-28T11:00:00Z",
  "data": {
    "id": "123456789012345678",
    "Lead_Score__c": 55,
    "Previous_Score": 45,
    "HDIM_Lead_ID": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

**HDIM Actions:**
1. Update Lead score in HDIM database
2. If score >= 50 (MQL threshold):
   - Update Lead_Stage__c to "MQL"
   - Trigger MQL workflow
   - Create SDR task
   - Send alert notification
3. If score >= 70 (SQL threshold):
   - Trigger SQL workflow
   - Prepare for conversion

---

#### 3. Lead Converted

**Trigger:** Lead converted to Contact + Account + Deal
**Direction:** Zoho -> HDIM

```json
{
  "event": "lead.converted",
  "module": "Leads",
  "timestamp": "2025-12-28T12:00:00Z",
  "data": {
    "lead_id": "123456789012345678",
    "contact_id": "223456789012345678",
    "account_id": "323456789012345678",
    "deal_id": "423456789012345678",
    "HDIM_Lead_ID": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

**HDIM Actions:**
1. Create Contact record linked to Lead
2. Create Account record
3. Create Opportunity record
4. Update Lead status to "Converted"
5. Assign Opportunity to AE
6. Remove from lead sequences
7. Sync new IDs back to Zoho

---

#### 4. Demo Scheduled

**Trigger:** Demo_Date__c populated on Deal
**Direction:** Zoho -> HDIM

```json
{
  "event": "deal.demo_scheduled",
  "module": "Deals",
  "timestamp": "2025-12-28T14:00:00Z",
  "data": {
    "id": "423456789012345678",
    "Deal_Name": "HDIM - Regional Medical Center",
    "Demo_Date__c": "2025-01-05T14:00:00Z",
    "Demo_Attendees__c": ["Contact1_ID", "Contact2_ID"],
    "HDIM_Opportunity_ID": "660e8400-e29b-41d4-a716-446655440001"
  }
}
```

**HDIM Actions:**
1. Create/update calendar event
2. Send demo prep materials
3. Create SE prep task
4. Schedule reminder emails
5. Update Opportunity stage to "Demo"

---

#### 5. Proposal Sent

**Trigger:** Proposal_Sent_Date__c populated on Deal
**Direction:** Zoho -> HDIM

```json
{
  "event": "deal.proposal_sent",
  "module": "Deals",
  "timestamp": "2025-12-28T16:00:00Z",
  "data": {
    "id": "423456789012345678",
    "Proposal_Sent_Date__c": "2025-12-28",
    "Proposal_Document_Link__c": "https://app.pandadoc.com/doc/abc123",
    "Amount": 150000,
    "HDIM_Opportunity_ID": "660e8400-e29b-41d4-a716-446655440001"
  }
}
```

**HDIM Actions:**
1. Update Opportunity stage to "Proposal"
2. Initialize proposal tracking
3. Add to Proposal follow-up sequence
4. Create follow-up tasks

---

#### 6. Deal Stage Changed

**Trigger:** Stage field updated on Deal
**Direction:** Zoho -> HDIM

```json
{
  "event": "deal.stage_changed",
  "module": "Deals",
  "timestamp": "2025-12-28T17:00:00Z",
  "data": {
    "id": "423456789012345678",
    "Stage": "Negotiation",
    "Previous_Stage": "Proposal",
    "Amount": 150000,
    "HDIM_Opportunity_ID": "660e8400-e29b-41d4-a716-446655440001"
  }
}
```

**HDIM Actions:**
1. Update Opportunity stage in HDIM
2. Execute stage-specific workflow
3. If Negotiation + Amount > $500K:
   - Alert VP Sales
   - Add VP as participant
4. Update forecast probabilities

---

#### 7. Deal Closed Won

**Trigger:** Deal Stage changed to "Closed Won"
**Direction:** Zoho -> HDIM

```json
{
  "event": "deal.closed_won",
  "module": "Deals",
  "timestamp": "2025-12-28T18:00:00Z",
  "data": {
    "id": "423456789012345678",
    "Amount": 150000,
    "Contract_Term_Months__c": 24,
    "Implementation_Start__c": "2025-01-15",
    "Go_Live_Target__c": "2025-04-01",
    "HDIM_Opportunity_ID": "660e8400-e29b-41d4-a716-446655440001"
  }
}
```

**HDIM Actions:**
1. Update Opportunity to Closed Won
2. Update Account stage to "Customer"
3. Trigger customer onboarding workflow:
   - Create onboarding project
   - Assign Customer Success Manager
   - Schedule kickoff call
   - Send welcome package
4. Remove from all sales sequences
5. Log revenue recognition
6. Send internal celebration notification

---

#### 8. Deal Closed Lost

**Trigger:** Deal Stage changed to "Closed Lost"
**Direction:** Zoho -> HDIM

```json
{
  "event": "deal.closed_lost",
  "module": "Deals",
  "timestamp": "2025-12-28T18:30:00Z",
  "data": {
    "id": "523456789012345678",
    "Lost_Reason__c": "Competitor",
    "Competitor_Lost_To__c": "Inovalon",
    "Lost_Reason_Details__c": "Existing contract with Inovalon, not willing to switch",
    "HDIM_Opportunity_ID": "770e8400-e29b-41d4-a716-446655440002"
  }
}
```

**HDIM Actions:**
1. Update Opportunity to Closed Lost
2. Capture and store lost reason
3. Create loss analysis task
4. Update competitive intelligence
5. Schedule re-engagement (if applicable):
   - Add to Sequence D after 30 days
   - Set re-engagement trigger based on lost reason
6. Send notification to Sales Manager

---

### HDIM -> Zoho Sync Events

#### Push Updates to Zoho

**Endpoint:** Zoho CRM API v3
**Base URL:** `https://www.zohoapis.com/crm/v3`

**Sync Operations:**

| Operation | Endpoint | Method |
|-----------|----------|--------|
| Create Lead | `/Leads` | POST |
| Update Lead | `/Leads/{id}` | PUT |
| Create Contact | `/Contacts` | POST |
| Update Account | `/Accounts/{id}` | PUT |
| Create Deal | `/Deals` | POST |
| Update Deal | `/Deals/{id}` | PUT |
| Create Task | `/Tasks` | POST |
| Create Note | `/Notes` | POST |

**Sync Frequency:**

| Event Type | Sync Timing |
|------------|-------------|
| Score changes | Real-time |
| Stage changes | Real-time |
| Field updates | Real-time |
| Activity logs | Real-time |
| Bulk enrichment | Every 15 minutes |
| Full reconciliation | Daily at 2 AM |

---

## API Endpoints

### HDIM Sales Automation API

**Base URL:** `http://localhost:8106/sales-automation/api/sales`

#### Lead Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/leads` | List all leads |
| GET | `/leads/{id}` | Get lead by ID |
| POST | `/leads` | Create new lead |
| PUT | `/leads/{id}` | Update lead |
| DELETE | `/leads/{id}` | Delete lead |
| POST | `/leads/{id}/score` | Recalculate lead score |
| POST | `/leads/{id}/convert` | Convert lead to opportunity |
| GET | `/leads/by-score` | Get leads by score range |
| GET | `/leads/by-status` | Get leads by status |

#### Opportunity Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/opportunities` | List opportunities |
| GET | `/opportunities/{id}` | Get opportunity by ID |
| POST | `/opportunities` | Create opportunity |
| PUT | `/opportunities/{id}` | Update opportunity |
| PUT | `/opportunities/{id}/stage` | Update stage |
| GET | `/opportunities/pipeline` | Get pipeline view |
| GET | `/opportunities/forecast` | Get forecast data |

#### Email Sequence Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/sequences` | List email sequences |
| GET | `/sequences/{id}` | Get sequence details |
| POST | `/sequences` | Create sequence |
| PUT | `/sequences/{id}` | Update sequence |
| POST | `/sequences/{id}/enroll` | Enroll contact |
| DELETE | `/sequences/{id}/enroll/{enrollmentId}` | Unenroll contact |
| GET | `/sequences/{id}/stats` | Get sequence performance |

#### Zoho Integration Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/zoho/webhook` | Receive Zoho webhook |
| GET | `/zoho/webhook` | Verify webhook endpoint |
| POST | `/zoho/webhook/sync` | Trigger manual sync |
| GET | `/zoho/status` | Get sync status |

---

## Implementation Notes

### Environment Variables

```bash
# Zoho CRM Integration
ZOHO_CLIENT_ID=your_client_id
ZOHO_CLIENT_SECRET=your_client_secret
ZOHO_REFRESH_TOKEN=your_refresh_token
ZOHO_SYNC_ENABLED=true
ZOHO_WEBHOOK_ENABLED=true
ZOHO_WEBHOOK_SECRET=your_webhook_secret

# Email Configuration (Zoho Mail)
MAIL_HOST=smtp.zoho.com
MAIL_PORT=587
MAIL_USERNAME=noreply@hdim.health
MAIL_PASSWORD=your_mail_password

# Database
POSTGRES_HOST=localhost
POSTGRES_PORT=5435
POSTGRES_DB=sales_automation_db
POSTGRES_USER=healthdata
POSTGRES_PASSWORD=your_password
```

### Zoho CRM Setup Steps

1. **Create Custom Fields**
   - Navigate to Setup > Customization > Modules > Leads/Accounts/Contacts/Deals
   - Add all custom fields listed in [Zoho CRM Custom Fields](#zoho-crm-custom-fields)

2. **Create Workflow Rules**
   - Navigate to Setup > Automation > Workflow Rules
   - Create workflow rules for each stage transition

3. **Configure Webhooks**
   - Navigate to Setup > Developer Space > Webhooks
   - Add HDIM webhook URL with authentication

4. **Set Up Blueprints**
   - Navigate to Setup > Automation > Blueprint
   - Create blueprints for Lead Qualification and Deal Pipeline

5. **Configure OAuth**
   - Navigate to Setup > Developer Space > APIs
   - Generate OAuth credentials
   - Configure refresh token

### Testing Checklist

- [ ] Lead creation webhook received and processed
- [ ] Lead score calculation accurate
- [ ] MQL threshold triggers correct workflow
- [ ] SQL threshold triggers lead conversion
- [ ] Demo scheduling creates calendar event
- [ ] Proposal tracking initialized correctly
- [ ] Closed Won triggers onboarding
- [ ] Closed Lost triggers re-engagement
- [ ] Email sequences send on schedule
- [ ] Bi-directional sync maintains data integrity

### Monitoring & Alerts

| Metric | Alert Threshold | Action |
|--------|-----------------|--------|
| Webhook failures | > 3 in 1 hour | Page on-call |
| Sync latency | > 5 minutes | Email ops team |
| Unprocessed leads | > 10 in queue | Alert SDR manager |
| Sequence bounce rate | > 10% | Review email hygiene |
| Conversion rate | < 5% (MQL to SQL) | Review scoring model |

---

## Appendix

### Lead Score Calculation Example

**Scenario:** Regional Health System with 300 beds

| Criteria | Raw Score | Weight | Weighted Score |
|----------|-----------|--------|----------------|
| Organization Size: 300-499 beds | 8 | 30% | 24 |
| VBC Contracts: MSSP + MA Stars | 8 | 25% | 20 |
| EHR System: Epic | 10 | 20% | 20 |
| Timeline: 3-6 months | 6 | 15% | 9 |
| Budget Authority: VP Level | 8 | 10% | 8 |
| **Total** | | | **81** |

**Result:** SQL (Score >= 70) - Assign to AE immediately

### Related Documentation

- [Sales Automation Service README](/backend/modules/services/sales-automation-service/README.md)
- [HDIM API Specification](/docs/BACKEND_API_SPECIFICATION.md)
- [Authentication Guide](/AUTHENTICATION_GUIDE.md)
- [Product Sheet](/sales/PRODUCT_SHEET.md)

---

*Document Owner: Sales Operations*
*Last Review: December 28, 2025*
*Next Review: March 28, 2025*
