# Sales Automation Service

## Overview

The Sales Automation Service provides CRM and sales pipeline management for HDIM's go-to-market operations. It handles lead capture, account management, opportunity tracking, email sequences, and integrations with external platforms like Zoho CRM and LinkedIn.

## Responsibilities

- Capture and qualify leads from landing pages and marketing channels
- Manage accounts, contacts, and opportunities
- Track sales pipeline and forecast revenue
- Automate email sequences for lead nurturing
- Integrate with Zoho CRM for bidirectional sync
- Track email engagement (opens, clicks)
- Provide sales dashboard and analytics
- Log all sales activities for audit trail

## Technology Stack

| Component | Technology | Version | Why This Choice |
|-----------|------------|---------|-----------------|
| Runtime | Java | 21 LTS | Platform standard |
| Framework | Spring Boot | 3.x | Enterprise integrations |
| Database | PostgreSQL | 15 | Relational data model for CRM |
| Cache | Redis | 7 | Session and rate limiting |
| External | Zoho CRM API | v2 | CRM sync |
| External | LinkedIn API | v2 | Social selling integration |

## API Endpoints

### Leads

#### POST /api/v1/leads/capture
**Purpose**: Capture lead from landing page (public endpoint)
**Auth Required**: No (rate limited)
**Request Body**:
```json
{
  "email": "prospect@healthsystem.org",
  "name": "Jane Smith",
  "organization": "Regional Health System",
  "source": "LANDING_PAGE",
  "utmSource": "google",
  "utmCampaign": "hedis-2025"
}
```

#### GET /api/v1/leads
**Purpose**: List leads with filtering
**Auth Required**: Yes (roles: SALES, ADMIN)

#### PUT /api/v1/leads/{id}/qualify
**Purpose**: Qualify or disqualify lead
**Auth Required**: Yes (roles: SALES, ADMIN)

### Accounts

#### GET /api/v1/accounts
**Purpose**: List accounts
**Auth Required**: Yes (roles: SALES, ADMIN)

#### POST /api/v1/accounts
**Purpose**: Create account
**Auth Required**: Yes (roles: SALES, ADMIN)

#### GET /api/v1/accounts/{id}
**Purpose**: Get account details with contacts and opportunities
**Auth Required**: Yes (roles: SALES, ADMIN)

### Opportunities

#### GET /api/v1/opportunities
**Purpose**: List opportunities with pipeline stage filtering
**Auth Required**: Yes (roles: SALES, ADMIN)

#### POST /api/v1/opportunities
**Purpose**: Create opportunity
**Auth Required**: Yes (roles: SALES, ADMIN)

#### PUT /api/v1/opportunities/{id}/stage
**Purpose**: Move opportunity to new pipeline stage
**Auth Required**: Yes (roles: SALES, ADMIN)

### Pipeline

#### GET /api/v1/pipeline/summary
**Purpose**: Get pipeline summary by stage
**Auth Required**: Yes (roles: SALES, ADMIN)

#### GET /api/v1/pipeline/forecast
**Purpose**: Get revenue forecast
**Auth Required**: Yes (roles: ADMIN)

### Email Sequences

#### GET /api/v1/email-sequences
**Purpose**: List email sequences
**Auth Required**: Yes (roles: SALES, ADMIN)

#### POST /api/v1/email-sequences/{id}/enroll
**Purpose**: Enroll contact in email sequence
**Auth Required**: Yes (roles: SALES, ADMIN)

### Dashboard

#### GET /api/v1/dashboard/metrics
**Purpose**: Get sales dashboard metrics
**Auth Required**: Yes (roles: SALES, ADMIN)

### Webhooks

#### POST /api/v1/webhooks/zoho
**Purpose**: Receive Zoho CRM webhook events
**Auth Required**: Webhook signature validation

## Database Schema

| Table | Purpose | Key Columns |
|-------|---------|-------------|
| leads | Lead records | id, email, name, organization, status, source, created_at |
| accounts | Account/company records | id, name, industry, size, owner_id |
| contacts | Contact records | id, account_id, email, name, title |
| opportunities | Sales opportunities | id, account_id, name, stage, amount, close_date |
| activities | Sales activities | id, type, subject, related_to_type, related_to_id |
| email_sequences | Email sequence definitions | id, name, steps |
| email_sequence_enrollments | Sequence enrollments | id, sequence_id, contact_id, current_step |
| email_tracking_events | Email engagement tracking | id, email_id, event_type, timestamp |

## Pipeline Stages

| Stage | Description | Probability |
|-------|-------------|-------------|
| PROSPECTING | Initial outreach | 10% |
| QUALIFICATION | Qualifying fit | 20% |
| DEMO | Demo scheduled/completed | 40% |
| PROPOSAL | Proposal sent | 60% |
| NEGOTIATION | Contract negotiation | 80% |
| CLOSED_WON | Deal won | 100% |
| CLOSED_LOST | Deal lost | 0% |

## Kafka Topics

### Publishes

| Topic | Event Type | Payload |
|-------|------------|---------|
| sales.lead.created | LeadCreatedEvent | New lead details |
| sales.opportunity.stage-changed | OpportunityStageChangedEvent | Stage transition |
| sales.deal.closed | DealClosedEvent | Won/lost deal details |

### Consumes

| Topic | Event Type | Handler |
|-------|------------|---------|
| notification.email.opened | EmailOpenedEvent | Updates email tracking |
| notification.email.clicked | EmailClickedEvent | Updates email tracking |

## External Integrations

### Zoho CRM

- **Sync Direction**: Bidirectional
- **Entities Synced**: Leads, Accounts, Contacts, Deals
- **Sync Frequency**: Real-time via webhooks + hourly full sync
- **Configuration**: `zoho.crm.*` properties

### LinkedIn

- **Features**: Profile lookup, connection requests, InMail
- **Rate Limits**: Respected per LinkedIn API guidelines
- **Configuration**: `linkedin.*` properties

## Configuration

```yaml
# application.yml
server:
  port: 8106

spring:
  datasource:
    url: jdbc:postgresql://localhost:5435/healthdata_sales

zoho:
  crm:
    enabled: true
    client-id: ${ZOHO_CLIENT_ID}
    client-secret: ${ZOHO_CLIENT_SECRET}
    refresh-token: ${ZOHO_REFRESH_TOKEN}
    webhook-secret: ${ZOHO_WEBHOOK_SECRET}

linkedin:
  enabled: true
  client-id: ${LINKEDIN_CLIENT_ID}
  client-secret: ${LINKEDIN_CLIENT_SECRET}

email:
  tracking:
    base-url: https://track.healthdatainmotion.com
```

## Testing

```bash
# Unit tests
./gradlew :modules:services:sales-automation-service:test

# Integration tests
./gradlew :modules:services:sales-automation-service:integrationTest
```

## Monitoring

- **Health Check**: `GET /actuator/health`
- **Metrics**: `GET /actuator/metrics`
- **Prometheus**: `GET /actuator/prometheus`

### Key Metrics

| Metric | Description |
|--------|-------------|
| `leads.captured.total` | Total leads captured by source |
| `opportunities.created.total` | Opportunities created |
| `pipeline.value` | Current pipeline value by stage |
| `deals.closed.value` | Closed deal value (won/lost) |
| `email.sequences.sent` | Emails sent from sequences |

## Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Zoho sync failing | Token expired | Refresh Zoho OAuth token |
| Lead capture rate limited | High traffic | Adjust rate limit config |
| Email tracking not working | Pixel blocked | Check tracking domain DNS |

## Security Considerations

- **Lead capture rate limiting**: Prevents abuse of public endpoint
- **Zoho webhook validation**: HMAC signature verification
- **No PHI storage**: Sales data separate from clinical data
- **Audit logging**: All sales activities logged
- **Role-based access**: SALES and ADMIN roles only

## References

- [Sales Events Schema](../../shared/api-contracts/sales-events.md)
- [Zoho CRM API Docs](https://www.zoho.com/crm/developer/docs/api/v2/)
- [Gateway Trust Architecture](../../../docs/GATEWAY_TRUST_ARCHITECTURE.md)

---

*Last Updated: December 2025*
*Service Version: 1.0*
