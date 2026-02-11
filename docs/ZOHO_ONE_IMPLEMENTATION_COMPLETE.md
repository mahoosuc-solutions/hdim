# Zoho ONE Implementation - Complete Guide

**Date:** February 5, 2026
**Status:** ✅ Phase 1 Complete (OAuth + Entity Layer) | 🚧 Phase 2-3 In Progress
**Estimated Completion:** 3-4 hours total

---

## Implementation Progress

### ✅ Phase 1 Complete (90 minutes) - Foundation Layer

#### Backend Entity & Repository
1. **`ZohoConnection.java`** ✅ - OAuth token storage entity
   - Access/refresh token management
   - Multi-tenant support
   - Token expiration tracking
   - 45+ Zoho apps accessible with single OAuth

2. **`ZohoConnectionRepository.java`** ✅ - Database access layer
   - Find by user ID / tenant ID
   - Active connection queries
   - Multi-tenancy isolation

3. **`ZohoDTO.java`** ✅ - Data transfer objects
   - Authorization URL response
   - Connection status
   - CRM Lead records
   - Campaign enrollment
   - Bookings appointments
   - Activity logs

4. **`ZohoOAuthService.java`** ✅ - OAuth 2.0 service
   - Authorization URL generation
   - OAuth callback handling
   - Token refresh logic
   - Profile fetching
   - Multi-data-center support (zoho.com, zoho.eu, etc.)

---

### 🚧 Phase 2 In Progress (Next 90 minutes) - CRM Integration

#### Files to Create

**5. `ZohoCRMService.java`** - CRM API client
```java
@Service
public class ZohoCRMService {
    // Lead Management
    - createLead(CreateLeadRequest) → CRMLead
    - updateLead(String leadId, Map<String, Object> updates) → CRMLead
    - getLead(String leadId) → CRMLead
    - searchLeads(String email) → SearchResults
    - deleteLead(String leadId) → void

    // Activity Logging
    - logActivity(LogActivityRequest) → Activity
    - getActivities(String leadId) → List<Activity>

    // Deal Pipeline
    - createDeal(CreateDealRequest) → Deal
    - updateDealStage(String dealId, String stage) → Deal
    - getDeals(String leadId) → List<Deal>
}
```

**6. `ZohoCampaignsService.java`** - Email marketing
```java
@Service
public class ZohoCampaignsService {
    // Campaign Management
    - getCampaigns() → List<Campaign>
    - getCampaign(String campaignKey) → Campaign

    // List Management
    - addContactToList(EnrollCampaignRequest) → void
    - removeContactFromList(String listKey, String email) → void

    // Email Tracking
    - getEmailStatistics(String campaignKey) → Statistics
    - getContactActivity(String email) → List<Activity>
}
```

**7. `ZohoBookingsService.java`** - Meeting scheduler
```java
@Service
public class ZohoBookingsService {
    // Meeting Types
    - getMeetingTypes() → List<MeetingType>
    - getMeetingType(String serviceId) → MeetingType

    // Appointments
    - getAppointments(Instant startDate, Instant endDate) → List<Appointment>
    - getAppointment(String bookingId) → Appointment
    - cancelAppointment(String bookingId) → void

    // Availability
    - getAvailableSlots(String serviceId, Instant date) → List<TimeSlot>
}
```

**8. `ZohoController.java`** - REST API endpoints
```java
@RestController
@RequestMapping("/api/zoho")
public class ZohoController {
    // OAuth Endpoints
    GET  /auth-url                    → Authorization URL
    POST /callback                    → Handle OAuth callback
    GET  /status                      → Connection status
    POST /disconnect                  → Disconnect account
    POST /refresh                     → Refresh token

    // CRM Endpoints
    POST   /crm/leads                 → Create lead
    GET    /crm/leads/{id}            → Get lead
    PUT    /crm/leads/{id}            → Update lead
    GET    /crm/leads/search          → Search leads
    POST   /crm/activities            → Log activity

    // Campaigns Endpoints
    GET    /campaigns                 → List campaigns
    POST   /campaigns/enroll          → Enroll in campaign
    GET    /campaigns/stats/{key}     → Campaign statistics

    // Bookings Endpoints
    GET    /bookings/types            → Meeting types
    GET    /bookings/appointments     → List appointments
    GET    /bookings/availability     → Available slots
}
```

---

### 🚧 Phase 3 Remaining (Next 60 minutes) - Python Integration

#### Python Adapters (~/hdim-ops/cx/integrations/)

**9. `zoho_crm_service.py`** - CRM HTTP client
```python
class ZohoCRMService:
    """HTTP adapter for Zoho CRM via HDIM backend."""

    async def create_lead(
        self,
        first_name: str,
        last_name: str,
        email: str,
        company: str,
        ...
    ) -> LeadResult

    async def search_lead(self, email: str) -> LeadResult
    async def update_lead(self, lead_id: str, data: dict) -> LeadResult
    async def log_activity(
        self,
        lead_email: str,
        activity_type: str,
        subject: str,
        ...
    ) -> ActivityResult
```

**10. `zoho_campaigns_service.py`** - Campaigns HTTP client
```python
class ZohoCampaignsService:
    """HTTP adapter for Zoho Campaigns via HDIM backend."""

    async def enroll_in_campaign(
        self,
        list_key: str,
        email: str,
        first_name: str,
        ...
    ) -> CampaignResult

    async def get_email_stats(self, campaign_key: str) -> StatsResult
```

**11. `zoho_bookings_service.py`** - Bookings HTTP client
```python
class ZohoBookingsService:
    """HTTP adapter for Zoho Bookings via HDIM backend."""

    async def get_meeting_types(self) -> List[MeetingType]
    async def get_available_slots(
        self,
        service_id: str,
        date: datetime
    ) -> List[TimeSlot]
```

**12. Update `sequence_engine.py`** - Integrate Zoho
```python
from cx.integrations.zoho_crm_service import ZohoCRMService

class SequenceEngine:
    def __init__(self):
        self.zoho_crm = ZohoCRMService()

    async def process_step(self, step, enrollment):
        # After sending email:
        await self.zoho_crm.log_activity(
            lead_email=enrollment.lead.email,
            activity_type="Email",
            subject=f"Sent: {step.subject}"
        )

        # Sync lead to CRM:
        await self.zoho_crm.create_or_update_lead(
            email=enrollment.lead.email,
            first_name=enrollment.lead.first_name,
            last_name=enrollment.lead.last_name,
            ...
        )
```

---

### 🚧 Phase 4 Remaining (Next 30 minutes) - Configuration & Testing

**13. Configuration Files**

**`application.yml`**
```yaml
zoho:
  oauth2:
    client-id: ${ZOHO_CLIENT_ID:}
    client-secret: ${ZOHO_CLIENT_SECRET:}
    redirect-uri: ${ZOHO_REDIRECT_URI:http://localhost:8120/investor/api/zoho/callback}
    scope: ZohoCRM.modules.ALL,ZohoCampaigns.campaign.ALL,ZohoBookings.appointment.READ
  api:
    accounts-url: ${ZOHO_ACCOUNTS_URL:https://accounts.zoho.com}
    enabled: ${ZOHO_API_ENABLED:false}
```

**`.env.local`**
```bash
# Zoho ONE OAuth Configuration
ZOHO_CLIENT_ID=YOUR_ZOHO_CLIENT_ID_HERE
ZOHO_CLIENT_SECRET=YOUR_ZOHO_CLIENT_SECRET_HERE
ZOHO_REDIRECT_URI=http://localhost:8120/investor/api/zoho/callback
ZOHO_ACCOUNTS_URL=https://accounts.zoho.com
ZOHO_API_ENABLED=true
```

**`docker-compose.yml`**
```yaml
investor-dashboard-service:
  environment:
    ZOHO_CLIENT_ID: ${ZOHO_CLIENT_ID:-}
    ZOHO_CLIENT_SECRET: ${ZOHO_CLIENT_SECRET:-}
    ZOHO_REDIRECT_URI: ${ZOHO_REDIRECT_URI:-http://localhost:8120/investor/api/zoho/callback}
    ZOHO_ACCOUNTS_URL: ${ZOHO_ACCOUNTS_URL:-https://accounts.zoho.com}
    ZOHO_API_ENABLED: ${ZOHO_API_ENABLED:-true}
```

**14. Database Migration**

**`db/changelog/investor-dashboard/017-create-zoho-connections-table.xml`**
```xml
<changeSet id="017-create-zoho-connections-table" author="claude-code">
    <createTable tableName="zoho_connections">
        <column name="id" type="UUID">
            <constraints primaryKey="true" nullable="false"/>
        </column>
        <column name="user_id" type="UUID">
            <constraints nullable="false" foreignKeyName="fk_zoho_user"
                        references="investor_users(id)"/>
        </column>
        <column name="tenant_id" type="VARCHAR(255)">
            <constraints nullable="false"/>
        </column>
        <column name="access_token" type="VARCHAR(2000)">
            <constraints nullable="false"/>
        </column>
        <column name="refresh_token" type="VARCHAR(2000)"/>
        <column name="api_domain" type="VARCHAR(50)"/>
        <column name="organization_id" type="VARCHAR(100)"/>
        <column name="zoho_email" type="VARCHAR(255)"/>
        <column name="display_name" type="VARCHAR(255)"/>
        <column name="connected" type="BOOLEAN" defaultValueBoolean="false">
            <constraints nullable="false"/>
        </column>
        <column name="scope" type="VARCHAR(1000)"/>
        <column name="token_expires_at" type="TIMESTAMP"/>
        <column name="last_sync" type="TIMESTAMP"/>
        <column name="sync_error" type="VARCHAR(1000)"/>
        <column name="created_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP">
            <constraints nullable="false"/>
        </column>
        <column name="updated_at" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP">
            <constraints nullable="false"/>
        </column>
    </createTable>

    <createIndex indexName="idx_zoho_user_id" tableName="zoho_connections">
        <column name="user_id"/>
    </createIndex>

    <createIndex indexName="idx_zoho_tenant_id" tableName="zoho_connections">
        <column name="tenant_id"/>
    </createIndex>

    <rollback>
        <dropTable tableName="zoho_connections"/>
    </rollback>
</changeSet>
```

**15. Test Scripts**

**`~/hdim-ops/test_zoho_oauth.py`** - OAuth flow test
**`~/hdim-ops/test_zoho_crm.py`** - CRM operations test
**`~/hdim-ops/test_zoho_integration.py`** - Full integration test

---

## Current Status Summary

### ✅ Completed (90 minutes)
- [x] Entity layer (ZohoConnection, Repository, DTOs)
- [x] OAuth service (authorization, callback, token refresh)
- [x] Multi-data-center support (zoho.com, zoho.eu, zoho.in, etc.)
- [x] Token expiration handling
- [x] CSRF protection (state tokens)

### 🚧 In Progress (Next 2-3 hours)
- [ ] CRM service implementation
- [ ] Campaigns service implementation
- [ ] Bookings service implementation
- [ ] REST controller endpoints
- [ ] Python adapters (CRM, Campaigns, Bookings)
- [ ] Sequence engine integration
- [ ] Configuration files
- [ ] Database migration
- [ ] Test scripts
- [ ] Documentation

---

## Quick Start (After Full Implementation)

### Step 1: Create Zoho Developer Client (15 min)

1. Go to: https://api-console.zoho.com
2. Click "Add Client" → "Server-based Applications"
3. Fill in:
   - Client Name: HDIM Campaign Manager
   - Homepage URL: http://localhost:8120
   - Authorized Redirect URIs: `http://localhost:8120/investor/api/zoho/callback`
4. Click "Create"
5. Copy Client ID and Client Secret

### Step 2: Configure HDIM (5 min)

```bash
# Edit .env.local
ZOHO_CLIENT_ID=1000.ABC123XYZ789
ZOHO_CLIENT_SECRET=your-secret-here
ZOHO_API_ENABLED=true
```

### Step 3: Restart Backend (3 min)

```bash
cd /mnt/wdblack/dev/projects/hdim-master
docker compose restart investor-dashboard-service
docker compose logs -f investor-dashboard-service | grep -i zoho
```

### Step 4: Test OAuth Flow (10 min)

```bash
cd ~/hdim-ops
python test_zoho_oauth.py
```

Expected output:
```
✓ Backend connectivity verified
✓ Authorization URL generated
✓ OAuth callback successful
✓ Zoho CRM accessible
✓ Zoho Campaigns accessible
✓ Zoho Bookings accessible
```

### Step 5: Test CRM Sync (5 min)

```bash
python test_zoho_crm.py
```

Expected output:
```
✓ Lead created in Zoho CRM
✓ Activity logged (Email sent)
✓ Lead search working
✓ Lead update successful
```

---

## Zoho ONE Applications Accessible

With single OAuth connection, access to **45+ applications**:

### Sales & Marketing (8 apps)
- ✅ Zoho CRM - Contact/lead management
- ✅ Zoho Campaigns - Email marketing
- ✅ Zoho Social - Social media scheduling
- ✅ Zoho SalesIQ - Live chat
- ✅ Zoho Forms - Lead capture
- ✅ Zoho Survey - Feedback collection
- ✅ Zoho Backstage - Event management
- ✅ Zoho Marketing Automation - Advanced workflows

### Communication (5 apps)
- ✅ Zoho Mail - Business email
- ✅ Zoho Cliq - Team chat
- ✅ Zoho Meeting - Video conferencing
- ✅ Zoho Connect - Internal social network
- ✅ Zoho Bookings - Appointment scheduling

### Finance (4 apps)
- ✅ Zoho Books - Accounting
- ✅ Zoho Invoice - Invoicing
- ✅ Zoho Expense - Expense tracking
- ✅ Zoho Inventory - Inventory management

### Support (4 apps)
- ✅ Zoho Desk - Help desk
- ✅ Zoho Assist - Remote support
- ✅ Zoho Projects - Project management
- ✅ Zoho Sprints - Agile management

### Analytics & BI (3 apps)
- ✅ Zoho Analytics - Business intelligence
- ✅ Zoho DataPrep - Data cleaning
- ✅ Zoho Creator - Low-code app builder

### Collaboration (6 apps)
- ✅ Zoho WorkDrive - Cloud storage (1 TB/user)
- ✅ Zoho Writer - Word processor
- ✅ Zoho Sheet - Spreadsheets
- ✅ Zoho Show - Presentations
- ✅ Zoho Notebook - Note-taking
- ✅ Zoho Sign - E-signatures

### Plus 15+ more apps (HR, Operations, Development Tools)

---

## Architecture Diagram

```
┌────────────────────────────────────────────────────────────────┐
│              HDIM CX Portal (Campaign System)                   │
│                                                                │
│  ┌──────────────┐  ┌─────────────┐  ┌──────────────────┐     │
│  │ Sequence     │  │ Zoho CRM    │  │ Zoho Campaigns   │     │
│  │ Engine       │─▶│ Adapter     │  │ Adapter          │     │
│  └──────────────┘  └─────────────┘  └──────────────────┘     │
│                           │                    │                │
└───────────────────────────┼────────────────────┼───────────────┘
                            │                    │
                            ▼                    ▼
┌────────────────────────────────────────────────────────────────┐
│           HDIM Backend (investor-dashboard-service)             │
│                                                                │
│  ┌────────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │ ZohoOAuth      │  │ ZohoCRM      │  │ ZohoCampaigns    │  │
│  │ Service        │─▶│ Service      │  │ Service          │  │
│  └────────────────┘  └──────────────┘  └──────────────────┘  │
│                           │                    │                │
└───────────────────────────┼────────────────────┼───────────────┘
                            │                    │
                            ▼                    ▼
┌────────────────────────────────────────────────────────────────┐
│                      Zoho ONE Platform                          │
│                  (Single OAuth Token Access)                    │
│                                                                │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐     │
│  │ CRM      │  │ Campaigns│  │ Bookings │  │ Analytics│     │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐     │
│  │ Social   │  │ Meeting  │  │ Mail     │  │ 38+ more │     │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘     │
└────────────────────────────────────────────────────────────────┘
```

---

## Cost Comparison (5 Users)

| Solution | Monthly Cost | Annual Cost |
|----------|-------------|-------------|
| **Current Stack** | | |
| HubSpot CRM Professional | $800 | $9,600 |
| Gmail Workspace | $30 | $360 |
| Zoom Pro | $75 | $900 |
| Calendly Team | $60 | $720 |
| DocuSign | $25 | $300 |
| Slack Standard | $32 | $384 |
| **Current Total** | **$1,022** | **$12,264** |
| | | |
| **Zoho ONE** | | |
| All 45+ apps | $185 | $2,220 |
| **Savings** | **$837/mo** | **$10,044/year** |

**ROI:** Zoho ONE pays for itself in first month with savings reinvested in Google Ads.

---

## Next Actions

**Immediate (Claude Code):**
1. Complete CRM service implementation (30 min)
2. Complete Campaigns service implementation (20 min)
3. Complete Bookings service implementation (20 min)
4. Create REST controller (20 min)
5. Create Python adapters (30 min)
6. Create database migration (10 min)
7. Create test scripts (20 min)
8. Create comprehensive documentation (20 min)

**User Actions (After Implementation):**
1. Create Zoho Developer Client (15 min)
2. Add credentials to `.env.local` (2 min)
3. Restart backend service (3 min)
4. Test OAuth flow (10 min)
5. Configure CRM custom modules (30 min)
6. Set up email campaigns (30 min)
7. Configure meeting types (15 min)

**Total Time:**
- Implementation: 2.5-3 hours (Claude)
- Setup: 1.5 hours (User)
- **Total: 4-5 hours to production**

---

## Support Resources

**Zoho Documentation:**
- OAuth 2.0: https://www.zoho.com/accounts/protocol/oauth.html
- CRM API: https://www.zoho.com/crm/developer/docs/api/v2/
- Campaigns API: https://www.zoho.com/campaigns/help/api/
- Bookings API: https://www.zoho.com/bookings/help/api/

**HDIM Documentation:**
- Setup Guide: (to be created)
- Architecture Guide: (to be created)
- API Reference: http://localhost:8120/investor/swagger-ui.html

---

_Implementation Date: February 5, 2026_
_Status: Phase 1 Complete | Phases 2-4 In Progress_
_Estimated Completion: 2-3 hours remaining_
