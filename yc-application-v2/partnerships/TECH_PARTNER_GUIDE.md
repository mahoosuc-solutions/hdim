# HDIM Technology Partner Integration Guide

> Integration certification process, API access, sandbox environment, and go-to-market collaboration.

---

## Welcome Technology Partners

This guide provides everything you need to build, certify, and go-to-market with an HDIM integration. Our technology partner program enables you to extend your platform's value through healthcare quality measurement capabilities.

**What You'll Get:**
- Full REST API access
- Dedicated sandbox environment
- Technical integration support
- Joint go-to-market opportunities
- Revenue share on referred customers

---

## Integration Overview

### What Can You Build?

| Integration Type | Description | Common Use Cases |
|------------------|-------------|------------------|
| **Data Source** | Send clinical data to HDIM | EHR, lab systems, claims |
| **Data Consumer** | Receive quality data from HDIM | Analytics, BI, reporting |
| **Bidirectional** | Both send and receive | EHR with embedded quality |
| **Embedded** | Display HDIM in your UI | SMART on FHIR apps |

### Integration Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Partner Platform                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  Your App   │  │   Your UI   │  │  Your Data Store    │  │
│  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘  │
└─────────┼────────────────┼─────────────────────┼────────────┘
          │                │                     │
          ▼                ▼                     ▼
    ┌───────────────────────────────────────────────────┐
    │                   HDIM API Layer                   │
    │  ┌────────────┐  ┌────────────┐  ┌────────────┐   │
    │  │    REST    │  │   FHIR     │  │  Webhooks  │   │
    │  │    API     │  │   Server   │  │            │   │
    │  └────────────┘  └────────────┘  └────────────┘   │
    └───────────────────────────────────────────────────┘
                          │
                          ▼
    ┌───────────────────────────────────────────────────┐
    │                HDIM Core Platform                  │
    │  ┌────────────┐  ┌────────────┐  ┌────────────┐   │
    │  │   CQL      │  │   Quality  │  │   Care     │   │
    │  │  Engine    │  │  Measures  │  │   Gaps     │   │
    │  └────────────┘  └────────────┘  └────────────┘   │
    └───────────────────────────────────────────────────┘
```

---

## Certification Levels

### Level 1: Compatible

**Description:** Your integration has been tested and validated to work with HDIM.

**Requirements:**
- [ ] Complete integration proposal
- [ ] Demonstrate working data exchange
- [ ] Pass basic validation tests
- [ ] Sign technology partner agreement

**Benefits:**
- Listed in HDIM partner directory
- "Compatible with HDIM" badge
- Partner portal access
- Basic technical support

**Timeline:** 2-4 weeks

---

### Level 2: Certified

**Description:** Your integration has been fully tested, documented, and is actively maintained.

**Requirements:**
- [ ] All Level 1 requirements
- [ ] Complete integration testing suite
- [ ] Quarterly validation testing
- [ ] Integration documentation
- [ ] Joint support procedures

**Benefits:**
- Featured listing in partner directory
- "HDIM Certified" badge
- Co-marketing opportunities
- Priority technical support
- Revenue share on referrals (10%)

**Timeline:** 4-8 weeks

---

### Level 3: Premier

**Description:** Deep strategic partnership with roadmap alignment and go-to-market investment.

**Requirements:**
- [ ] All Level 2 requirements
- [ ] Joint product roadmap
- [ ] Dedicated integration support
- [ ] Minimum referral commitments
- [ ] Executive sponsorship

**Benefits:**
- Featured partnership on website
- Joint press release
- Co-marketing fund
- Dedicated partner manager
- Enhanced revenue share (15%)
- Roadmap input

**Timeline:** Negotiated

---

## Getting Started

### Step 1: Apply for Partnership

Submit your interest via partners@healthdatainmotion.com or the partner portal.

**Include:**
- Company overview
- Integration concept
- Target use cases
- Timeline expectations
- Technical contact information

### Step 2: Discovery Call

We'll schedule a 30-minute call to discuss:
- Your platform and customer base
- Integration scope and approach
- Technical requirements
- Partnership terms
- Timeline and next steps

### Step 3: Agreement Signing

Upon mutual agreement, we'll formalize with:
- Technology Partner Agreement
- Data Processing Agreement (DPA)
- API Terms of Use

### Step 4: Technical Onboarding

- Sandbox environment provisioned
- API credentials issued
- Technical documentation access
- Integration support assigned

### Step 5: Development & Testing

- Build your integration
- Test in sandbox environment
- HDIM validation and certification
- Documentation completion

### Step 6: Go-to-Market

- Joint announcement
- Marketing materials
- Sales enablement
- Launch!

---

## API Access

### Authentication

HDIM supports two authentication methods:

**OAuth 2.0 (Recommended)**
```
POST /oauth/token
Content-Type: application/x-www-form-urlencoded

grant_type=client_credentials
&client_id={your_client_id}
&client_secret={your_client_secret}
&scope=read write
```

**API Key**
```
GET /api/v1/patients
Authorization: Bearer {your_api_key}
X-Organization-ID: {org_id}
```

### Rate Limits

| Partner Level | Requests/Minute | Requests/Day |
|---------------|-----------------|--------------|
| Compatible | 60 | 10,000 |
| Certified | 300 | 100,000 |
| Premier | 1,000 | Unlimited |

### Key Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/patients` | GET, POST | Patient demographics |
| `/api/v1/patients/{id}/observations` | GET, POST | Clinical observations |
| `/api/v1/patients/{id}/conditions` | GET, POST | Diagnoses/conditions |
| `/api/v1/patients/{id}/procedures` | GET, POST | Procedures performed |
| `/api/v1/measures` | GET | Available quality measures |
| `/api/v1/patients/{id}/measure-results` | GET | Patient measure results |
| `/api/v1/care-gaps` | GET | Open care gaps |
| `/api/v1/webhooks` | POST, DELETE | Webhook subscriptions |

### FHIR R4 Server

For partners preferring FHIR:
- Base URL: `https://fhir.healthdatainmotion.com/r4/{tenant}`
- Supported resources: Patient, Observation, Condition, Procedure, MeasureReport
- Bulk FHIR export supported

### Webhooks

Subscribe to real-time events:

| Event | Description |
|-------|-------------|
| `care_gap.opened` | New care gap identified |
| `care_gap.closed` | Care gap resolved |
| `measure.calculated` | Measure result updated |
| `patient.updated` | Patient data changed |

---

## Sandbox Environment

### Access

**Sandbox URL:** `https://sandbox.api.healthdatainmotion.com`
**FHIR Sandbox:** `https://sandbox.fhir.healthdatainmotion.com/r4`

### Test Data

Your sandbox includes:
- 1,000 synthetic patients
- Realistic clinical data patterns
- Pre-calculated quality measures
- Open and closed care gaps
- Multiple organization structures

### Sandbox Limits

| Resource | Limit |
|----------|-------|
| API requests | 10,000/day |
| Patients | 10,000 |
| Organizations | 10 |
| Data retention | 90 days |
| Concurrent users | 5 |

### Reset Procedures

**Reset test data:**
```
POST /sandbox/reset
Authorization: Bearer {sandbox_key}
```

**Request additional data:**
Contact partners@healthdatainmotion.com

---

## Integration Patterns

### Pattern 1: Send Clinical Data

**Use Case:** EHR or data source sends patient data to HDIM

**Flow:**
1. Patient encounter occurs in your system
2. Your system packages clinical data (FHIR or JSON)
3. POST to HDIM API
4. HDIM calculates measures in real-time
5. Quality results available via API or webhook

**Example:**
```json
POST /api/v1/patients/{id}/observations
Content-Type: application/json
Authorization: Bearer {token}

{
  "resourceType": "Observation",
  "code": {
    "coding": [{
      "system": "http://loinc.org",
      "code": "4548-4",
      "display": "Hemoglobin A1c"
    }]
  },
  "valueQuantity": {
    "value": 7.2,
    "unit": "%"
  },
  "effectiveDateTime": "2025-01-15"
}
```

---

### Pattern 2: Retrieve Quality Data

**Use Case:** Analytics platform retrieves quality metrics

**Flow:**
1. Request measure results for patient or population
2. HDIM returns current quality status
3. Your system displays or processes results

**Example:**
```json
GET /api/v1/organizations/{org_id}/measure-summary
Authorization: Bearer {token}

Response:
{
  "organizationId": "org_123",
  "period": "2025",
  "measures": [
    {
      "measureId": "DM-HbA1c",
      "measureName": "Diabetes HbA1c Control",
      "numerator": 847,
      "denominator": 1023,
      "rate": 82.8,
      "benchmark": 75.0
    }
  ]
}
```

---

### Pattern 3: Embed Care Gaps

**Use Case:** Display care gaps within your application

**Flow:**
1. Request open care gaps for patient
2. Display in your UI (chart, dashboard, worklist)
3. User closes gap in your system
4. Send update to HDIM
5. Gap marked as closed

**Example:**
```json
GET /api/v1/patients/{patient_id}/care-gaps
Authorization: Bearer {token}

Response:
{
  "patientId": "pt_456",
  "careGaps": [
    {
      "gapId": "cg_789",
      "measureId": "DM-HbA1c",
      "measureName": "Diabetes HbA1c Control",
      "status": "open",
      "dueDate": "2025-03-31",
      "priority": "high",
      "action": "Order HbA1c test"
    }
  ]
}
```

---

### Pattern 4: SMART on FHIR App

**Use Case:** Embed HDIM quality view in EHR workflow

**Flow:**
1. User launches SMART app from EHR
2. App authenticates via SMART on FHIR
3. App displays quality measures and care gaps
4. User takes action within EHR
5. EHR data flows to HDIM, gap closes

**Launch Parameters:**
```
https://app.healthdatainmotion.com/smart/launch
?iss=https://fhir.ehr.com/r4
&launch={launch_token}
```

---

## Testing & Certification

### Test Suite

Before certification, your integration must pass:

**Data Exchange Tests**
- [ ] Successful authentication
- [ ] Patient data submission
- [ ] Observation data submission
- [ ] Error handling for invalid data
- [ ] Rate limit handling

**Quality Retrieval Tests**
- [ ] Measure results retrieval
- [ ] Care gap retrieval
- [ ] Population summary retrieval
- [ ] Pagination handling

**Webhook Tests** (if applicable)
- [ ] Webhook subscription
- [ ] Event receipt and acknowledgment
- [ ] Retry handling
- [ ] Unsubscription

### Validation Process

1. **Self-Testing:** Run tests in sandbox (provided test scripts)
2. **Documentation Review:** Submit integration documentation
3. **HDIM Validation:** We run certification test suite
4. **Feedback:** Address any issues identified
5. **Certification:** Receive certification upon passing

### Quarterly Validation

Certified partners must:
- Run validation tests quarterly
- Report any breaking changes
- Maintain documentation accuracy
- Respond to validation requests within 10 business days

---

## Documentation Requirements

### For Compatible Level

| Document | Description |
|----------|-------------|
| Integration overview | High-level description |
| Authentication method | How you authenticate |
| Data flows | What data goes where |

### For Certified Level

| Document | Description |
|----------|-------------|
| Technical specification | Detailed integration design |
| Setup guide | How customers enable integration |
| User guide | How users interact with integration |
| Support runbook | Troubleshooting procedures |
| Error codes | Integration-specific errors |

### Template Available

Download our integration documentation template from the partner portal.

---

## Support Procedures

### Technical Support Channels

| Level | Channel | Response Time |
|-------|---------|---------------|
| Compatible | Email | 2 business days |
| Certified | Email + Slack | 1 business day |
| Premier | Dedicated contact | 4 hours |

**Email:** integrations@healthdatainmotion.com
**Slack:** Provided upon Certified status

### Joint Customer Support

For issues affecting mutual customers:

1. **Triage:** Determine if issue is on HDIM or partner side
2. **Escalation:** Both parties engage if unclear
3. **Resolution:** Owning party fixes, notifies other
4. **Communication:** Joint update to customer if needed

### Escalation Path

1. Partner Portal ticket
2. Integration support email
3. Integration team lead
4. Partner engineering manager
5. VP Engineering (critical issues only)

---

## Go-to-Market Collaboration

### Joint Marketing Activities

| Activity | HDIM Contribution | Partner Contribution |
|----------|-------------------|----------------------|
| Press release | Writing, distribution | Review, amplification |
| Case study | Customer interview, writing | Customer introduction |
| Webinar | Platform, promotion (50%) | Content, promotion (50%) |
| Blog post | Publication, promotion | Writing or review |
| Conference | Booth mention, materials | Booth mention, materials |

### Sales Enablement

**What We Provide:**
- Integration one-pager
- Joint solution brief
- Demo environment with integration
- Sales training (30 min)
- FAQ document

**What We Need From You:**
- Customer referral process
- Sales team briefing
- Pricing alignment
- Support handoff process

### Lead Sharing

**From HDIM to Partner:**
- Customers requesting your product category
- Prospects mentioning your platform
- RFPs requiring your capabilities

**From Partner to HDIM:**
- Customers needing quality measurement
- Prospects mentioning quality challenges
- RFPs requiring HDIM capabilities

**Revenue Share:**
- 10% for Certified partners
- 15% for Premier partners
- On first-year contract value
- For referred and closed customers

---

## Security & Compliance

### Security Requirements

All partners must:
- [ ] Use TLS 1.2+ for all API calls
- [ ] Store credentials securely (no plaintext)
- [ ] Implement OAuth token refresh
- [ ] Log API access for audit
- [ ] Report security incidents within 24 hours

### Compliance

**HIPAA:**
- BAA required before accessing PHI
- PHI handling per BAA terms
- Incident notification per BAA

**SOC 2:**
- Partners encouraged to be SOC 2 certified
- Required for Premier partnerships

### Data Handling

| Data Type | Our Responsibility | Your Responsibility |
|-----------|-------------------|---------------------|
| Data in transit | Encrypted via TLS | Use TLS 1.2+ |
| Data at rest | Encrypted (AES-256) | Follow your policies |
| Access logs | Maintain for 7 years | Maintain per your policy |
| PHI | Per BAA | Per BAA |

---

## Partner Agreement Terms

### Key Terms Summary

| Term | Standard | Notes |
|------|----------|-------|
| Duration | 24 months | Auto-renew |
| Termination notice | 90 days | Either party |
| IP rights | Each retains own | No transfer |
| Data ownership | Customer owns | No exceptions |
| Revenue share | 10-15% | Per tier |
| Exclusivity | Non-exclusive | Negotiate if needed |
| Liability | Standard | Per agreement |

### API Terms

- Fair use policy applies
- No reselling API access
- Attribution required in UI
- Subject to rate limits
- May be terminated for abuse

---

## Resources

### Documentation

| Resource | Location |
|----------|----------|
| API Reference | developers.healthdatainmotion.com |
| FHIR Documentation | developers.healthdatainmotion.com/fhir |
| Integration Templates | Partner Portal → Resources |
| Test Scripts | Partner Portal → Testing |

### Contacts

| Role | Email |
|------|-------|
| Partner Manager | [Assigned upon agreement] |
| Integration Support | integrations@healthdatainmotion.com |
| Partnership Inquiries | partners@healthdatainmotion.com |

### Partner Portal

**URL:** partners.healthdatainmotion.com

**Features:**
- API credentials management
- Sandbox environment access
- Integration status tracking
- Lead submission and tracking
- Revenue share reporting
- Resource downloads
- Support ticket management

---

## Appendix: Sample Integration Checklist

### Pre-Development

- [ ] Partnership agreement signed
- [ ] Sandbox credentials received
- [ ] API documentation reviewed
- [ ] Integration scope defined
- [ ] Technical contact assigned

### Development

- [ ] Authentication implemented
- [ ] Core data exchange working
- [ ] Error handling implemented
- [ ] Logging implemented
- [ ] Rate limiting handled
- [ ] Unit tests written

### Testing

- [ ] Sandbox testing complete
- [ ] Edge cases tested
- [ ] Performance validated
- [ ] Security review complete
- [ ] Documentation drafted

### Certification

- [ ] Test suite passed
- [ ] Documentation submitted
- [ ] HDIM validation complete
- [ ] Certification received
- [ ] Partner portal updated

### Launch

- [ ] Go-to-market materials ready
- [ ] Sales teams briefed
- [ ] Support runbooks published
- [ ] Announcement made
- [ ] Monitoring in place

---

*Last Updated: December 2025*
*Technical Questions: integrations@healthdatainmotion.com*
