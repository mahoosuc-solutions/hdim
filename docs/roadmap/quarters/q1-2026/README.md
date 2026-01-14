# Q1 2026: Foundation & MVP Launch

**Period**: January 1 - March 31, 2026  
**Theme**: "Viable Product, Visible Value"  
**Goal**: Make the platform production-ready for early adopters with complete clinical workflows, working AI agents, and polished UX.

---

## Overview

Q1 2026 focuses on building the core user-facing applications and essential infrastructure to launch an MVP that delivers immediate value to healthcare providers. By the end of Q1, we will have:

- ✅ **5 functional portals** (Clinical, Admin, Agent Studio, Developer Portal, Analytics)
- ✅ **Complete authentication** (SSO, MFA, RBAC)
- ✅ **Core clinical workflows** (Care gaps, quality measures, patient search)
- ✅ **Working AI agents** (Clinical assistant, care gap analysis)
- ✅ **API documentation** (OpenAPI/Swagger, Postman collections)

**Success Criteria**:
- 5+ pilot customers onboarded
- Sub-5 second clinical queries
- 80%+ user satisfaction score
- Zero critical security vulnerabilities

---

## Feature Breakdown

### 1. Clinical User Portal 🏥

**Target Users**: Physicians, Nurses, Care Managers  
**Priority**: P0  
**Total Effort**: 12 weeks  
**Team Size**: 3 frontend engineers

#### 1.1 Patient Search & 360° View
**Spec**: [patient-search.md](./specs/patient-search.md)  
**Effort**: 3 weeks  
**Dependencies**: FHIR Service, Patient Service

**Features**:
- Search patients by name, MRN, DOB, phone
- Auto-complete with fuzzy matching
- Advanced filters (age range, condition, payer, provider)
- Patient demographics card
- Timeline view of clinical events
- Problem list, medication list, allergy list
- Recent encounters, lab results, procedures
- Care team members

**Technical Stack**:
- React 18 + TypeScript
- Material-UI components
- React Query for data fetching
- Zustand for local state
- Recharts for vitals visualization

**API Endpoints**:
```typescript
GET /api/v1/patients/search?query={query}&filters={filters}
GET /api/v1/patients/{id}/comprehensive
GET /api/v1/patients/{id}/timeline
GET /api/v1/patients/{id}/care-team
```

**UI Mockup**:
```
┌─────────────────────────────────────────────────────────┐
│ 🔍 Search Patients                            [+ New]   │
│ ┌───────────────────────────────────────────────────┐   │
│ │ Search by name, MRN, DOB...                      │   │
│ └───────────────────────────────────────────────────┘   │
│ [📋 Filters: Age: All | Condition: All | Payer: All]   │
├─────────────────────────────────────────────────────────┤
│ 👤 John Doe, M, 65 years | MRN: 123456 | Active      │
│    Last Visit: 2026-01-10 | Provider: Dr. Smith      │
│    Open Care Gaps: 3 | STAR Rating: 4.2              │
├─────────────────────────────────────────────────────────┤
│ 👤 Jane Smith, F, 42 years | MRN: 789012 | Active    │
│    Last Visit: 2026-01-08 | Provider: Dr. Johnson    │
│    Open Care Gaps: 1 | STAR Rating: 4.8              │
└─────────────────────────────────────────────────────────┘
```

#### 1.2 Care Gap Dashboard
**Spec**: [care-gap-dashboard.md](./specs/care-gap-dashboard.md)  
**Effort**: 2 weeks  
**Dependencies**: Care Gap Service, Quality Measure Service

**Features**:
- Visual dashboard showing all open care gaps
- Priority ranking (High, Medium, Low)
- Filter by measure, due date, patient
- Bulk actions (assign, close, snooze)
- Gap closure rate trend chart
- Export to Excel

**Technical Details**:
```typescript
interface CareGap {
  id: string;
  patientId: string;
  patientName: string;
  measureId: string;
  measureName: string;
  description: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  status: 'OPEN' | 'IN_PROGRESS' | 'CLOSED';
  dueDate: string;
  assignedTo?: string;
  recommendedActions: string[];
}
```

**API Endpoints**:
```typescript
GET /api/v1/care-gaps?status=OPEN&sort=priority
POST /api/v1/care-gaps/{id}/assign
POST /api/v1/care-gaps/{id}/close
GET /api/v1/care-gaps/statistics
```

#### 1.3 Quality Measure Viewer
**Spec**: [quality-measure-viewer.md](./specs/quality-measure-viewer.md)  
**Effort**: 2 weeks  
**Dependencies**: CQL Engine, Quality Measure Service

**Features**:
- Display HEDIS/CMS measure compliance per patient
- Drill-down into measure details
- Show numerator/denominator logic
- Display evidence (observations, conditions)
- Real-time measure calculation
- Historical trend

**Measure Categories**:
- **Diabetes**: HbA1c control, eye exams, BP control
- **Cardiovascular**: BP control, statin therapy, ASA use
- **Preventive**: Mammography, colorectal cancer screening
- **Behavioral Health**: Depression screening, PHQ-9

#### 1.4 AI Clinical Assistant Chat
**Spec**: [ai-assistant-chat.md](./specs/ai-assistant-chat.md)  
**Effort**: 2 weeks  
**Dependencies**: AI Assistant Service, Agent Runtime Service

**Features**:
- Embedded chat interface
- Natural language queries
- Context-aware responses
- Tool calling (FHIR query, CQL evaluation)
- Conversation history
- Copy/paste responses

**Example Queries**:
- "What are the care gaps for patient John Doe?"
- "Show me all diabetic patients with HbA1c > 9"
- "Calculate HEDIS measures for this patient"
- "What preventive screenings are due this month?"

**Technical Implementation**:
```typescript
const handleChatMessage = async (message: string) => {
  const response = await fetch('/api/v1/ai/chat', {
    method: 'POST',
    body: JSON.stringify({
      message,
      context: { patientId, tenantId },
      conversationId,
    }),
  });
  
  const data = await response.json();
  setMessages([...messages, { role: 'assistant', content: data.response }]);
};
```

#### 1.5 Document Upload & OCR
**Spec**: [document-upload.md](./specs/document-upload.md)  
**Effort**: 3 weeks  
**Dependencies**: Document Service (NEW), S3, Textract/Tesseract

**Features**:
- Drag-and-drop file upload
- Support PDF, JPG, PNG, TIFF
- OCR text extraction
- Link documents to patients
- Document viewer
- Search within documents

---

### 2. Enhanced Admin Portal ⚙️

**Target Users**: System Administrators, DevOps, Support  
**Priority**: P0  
**Total Effort**: 10 weeks  
**Team Size**: 2 frontend engineers

#### 2.1 Unified Service Dashboard
**Spec**: [service-dashboard.md](./specs/service-dashboard.md)  
**Effort**: 1 week

**Features**:
- Single-page overview of all 30+ services
- Health status indicators (UP, DOWN, DEGRADED)
- Last deployment time
- Version numbers
- Quick links to logs, metrics
- Restart service button (with confirmation)

**Dashboard Layout**:
```
┌─────────────────────────────────────────────────────────┐
│ System Health: ✅ All Systems Operational               │
│ Last Updated: 2 seconds ago                    [Refresh]│
├─────────────────────────────────────────────────────────┤
│ Core Services (8/8 UP)                                  │
│ ✅ FHIR Service      v2.3.1   Up 15 days   [Logs][Metrics]│
│ ✅ CQL Engine        v1.8.2   Up 20 days   [Logs][Metrics]│
│ ✅ Care Gap Service  v1.5.0   Up 10 days   [Logs][Metrics]│
│ ...                                                      │
├─────────────────────────────────────────────────────────┤
│ AI Services (5/5 UP)                                    │
│ ✅ AI Assistant      v0.9.0   Up 5 days    [Logs][Metrics]│
│ ✅ Agent Runtime     v1.2.0   Up 8 days    [Logs][Metrics]│
│ ...                                                      │
└─────────────────────────────────────────────────────────┘
```

#### 2.2 Real-Time Monitoring
**Spec**: [monitoring-dashboard.md](./specs/monitoring-dashboard.md)  
**Effort**: 2 weeks

**Metrics Displayed**:
- **CPU Usage**: Per service, color-coded
- **Memory Usage**: With heap usage for Java services
- **Request Rate**: Requests per second
- **Error Rate**: Errors per second
- **Latency**: p50, p95, p99
- **Active Connections**: Database, Redis, Kafka

**Integrations**:
- Prometheus for metrics collection
- Grafana dashboards embedded in iframe
- WebSocket for live updates

#### 2.3 Audit Log Viewer
**Spec**: [audit-log-viewer.md](./specs/audit-log-viewer.md)  
**Effort**: 2 weeks

**Features**:
- Search audit events by user, resource, action, date
- Advanced filters (event type, outcome, IP address)
- Export to CSV/JSON
- Event details modal
- Real-time log streaming
- Compliance reports (HIPAA access logs)

#### 2.4 User Management
**Spec**: [user-management.md](./specs/user-management.md)  
**Effort**: 2 weeks

**Features**:
- CRUD operations for users
- Assign roles (ADMIN, CLINICIAN, NURSE, etc.)
- Set permissions (31 granular permissions)
- Enable/disable users
- Password reset
- MFA setup
- User activity log

#### 2.5 Tenant Management
**Spec**: [tenant-management.md](./specs/tenant-management.md)  
**Effort**: 2 weeks

**Features**:
- Create/edit/deactivate tenants
- Set tenant configuration (feature flags, limits)
- View usage statistics (API calls, storage, users)
- Billing information
- Custom branding (logo, colors)
- Data isolation verification

#### 2.6 Configuration Editor
**Spec**: [config-editor.md](./specs/config-editor.md)  
**Effort**: 3 weeks

**Features**:
- Edit service configurations without code deploy
- Feature flag management
- Rate limit configuration
- API key management
- Webhook configuration
- Change history with rollback
- Validation before save

---

### 3. AI Agent Studio 🤖

**Target Users**: Clinical Informaticists, Quality Managers  
**Priority**: P0  
**Total Effort**: 9 weeks  
**Team Size**: 2 frontend engineers

#### 3.1 Visual Agent Designer
**Spec**: [agent-designer.md](./specs/agent-designer.md)  
**Effort**: 4 weeks

**Features**:
- Drag-and-drop interface for agent configuration
- Configure agent persona (name, role, avatar)
- Select tools (FHIR query, CQL evaluation, etc.)
- Configure guardrails (clinical safety, PHI protection)
- Set LLM parameters (temperature, max tokens, model)
- Preview system prompt

**UI Sections**:
1. **Agent Identity**: Name, description, avatar
2. **Persona**: Role, capabilities, constraints
3. **Tools**: Enable/disable tools with permissions
4. **Guardrails**: Safety rules, content filters
5. **LLM Settings**: Provider, model, temperature
6. **Advanced**: Memory settings, timeout, max iterations

#### 3.2 Prompt Template Library
**Spec**: [prompt-templates.md](./specs/prompt-templates.md)  
**Effort**: 2 weeks

**Features**:
- Browse template library
- Search templates by category
- Create custom templates
- Variable substitution ({{variable}})
- Template validation
- Preview rendered template
- Clone system templates

**Template Categories**:
- System Prompts
- Capabilities
- Constraints
- Clinical Safety Rules
- Welcome Messages
- Error Messages

#### 3.3 Interactive Testing
**Spec**: [agent-testing.md](./specs/agent-testing.md)  
**Effort**: 2 weeks

**Features**:
- Sandbox test environment
- Send messages to agent
- View tool invocations
- See guardrail triggers
- Performance metrics (latency, tokens)
- Save test sessions
- Export conversation

#### 3.4 Version Control UI
**Spec**: [agent-versioning.md](./specs/agent-versioning.md)  
**Effort**: 2 weeks

**Features**:
- View version history
- Compare versions (diff view)
- Rollback to previous version
- Publish version
- Version tags and notes

#### 3.5 Performance Metrics
**Spec**: [agent-metrics.md](./specs/agent-metrics.md)  
**Effort**: 1 week

**Metrics**:
- Token usage (input, output, total)
- Average latency
- Guardrail trigger rate
- Tool invocation frequency
- User satisfaction ratings
- Cost per conversation

---

### 4. Developer Portal 👨‍💻

**Target Users**: Developers, Integration Partners  
**Priority**: P0  
**Total Effort**: 8 weeks  
**Team Size**: 2 frontend + 1 backend engineer

#### 4.1 Interactive API Documentation
**Spec**: [api-docs.md](./specs/api-docs.md)  
**Effort**: 2 weeks

**Features**:
- OpenAPI/Swagger UI for all 30+ services
- Try it out feature with test credentials
- Request/response examples
- Error code documentation
- Rate limit information
- Authentication guide

#### 4.2 Postman Collections
**Spec**: [postman-collections.md](./specs/postman-collections.md)  
**Effort**: 1 week

**Collections**:
- Patient Management
- Care Gaps & Quality Measures
- Prior Authorization
- AI Agents
- Webhooks
- Admin Operations

#### 4.3 Code Examples
**Spec**: [code-examples.md](./specs/code-examples.md)  
**Effort**: 2 weeks

**Languages**:
- Python
- Java
- JavaScript/TypeScript
- cURL

**Examples**:
- Authentication (JWT)
- Search patients
- Create prior auth request
- Evaluate quality measures
- Subscribe to webhooks

#### 4.4 Sandbox Environment
**Spec**: [sandbox.md](./specs/sandbox.md)  
**Effort**: 3 weeks

**Features**:
- Test API keys (sandbox mode)
- Synthetic patient data
- No PHI
- Reset data button
- Webhook testing endpoint
- Rate limits (100 req/min)

#### 4.5 Webhook Configuration
**Spec**: [webhook-config.md](./specs/webhook-config.md)  
**Effort**: 2 weeks

**Features**:
- Register webhook URLs
- Select events to subscribe to
- Secret key for signature validation
- Test webhook delivery
- View delivery history
- Retry failed deliveries

---

### 5. Authentication & Authorization 🔒

**Priority**: P0  
**Total Effort**: 8 weeks  
**Team Size**: 2 backend engineers + 1 security engineer

#### 5.1 SSO Integration
**Spec**: [sso-integration.md](./specs/sso-integration.md)  
**Effort**: 3 weeks

**Supported Providers**:
- Okta (SAML 2.0 & OAuth 2.0)
- Azure AD (SAML 2.0 & OAuth 2.0)
- Auth0 (OAuth 2.0)
- Google Workspace (OAuth 2.0)
- Generic SAML 2.0

**Configuration UI**:
- Upload IDP metadata XML
- Configure SSO endpoint URLs
- Map user attributes
- Test SSO flow
- Enable/disable per tenant

#### 5.2 MFA Support
**Spec**: [mfa.md](./specs/mfa.md)  
**Effort**: 2 weeks

**Supported Methods**:
- TOTP (Time-based One-Time Password) - Google Authenticator, Authy
- SMS (via Twilio)
- Email
- Backup codes

**Features**:
- Enforce MFA per tenant
- Enforce MFA for specific roles (ADMIN, BILLING)
- User can enroll multiple methods
- Remember device for 30 days

#### 5.3 Role-Based Access Control (RBAC)
**Spec**: [rbac.md](./specs/rbac.md)  
**Effort**: 2 weeks

**13 Roles**:
1. SUPER_ADMIN
2. TENANT_ADMIN
3. CLINICIAN
4. NURSE
5. CARE_MANAGER
6. QUALITY_ANALYST
7. BILLING_ADMIN
8. DEVELOPER
9. PATIENT
10. VIEWER
11. SUPPORT
12. SECURITY_ADMIN
13. COMPLIANCE_OFFICER

**31 Permissions** (examples):
- `read:patient`
- `write:patient`
- `read:care_gap`
- `write:care_gap`
- `evaluate:cql`
- `manage:users`
- `manage:tenants`
- `access:api`
- etc.

#### 5.4 Session Management
**Spec**: [session-management.md](./specs/session-management.md)  
**Effort**: 1 week

**Features**:
- 4-hour idle timeout
- Secure session storage in Redis
- Concurrent session limit (5 per user)
- Session revocation
- "Kick out user" feature for admins
- Activity tracking

---

## Infrastructure & DevOps

### CI/CD Enhancements

**GitHub Actions Workflows**:
```yaml
- Frontend Build & Test (all portals)
- Backend Build & Test (all services)
- Docker Image Build & Push
- Deploy to Staging
- Deploy to Production (manual approval)
- Security Scanning (Snyk, Trivy)
- Dependency Updates (Dependabot)
```

### Monitoring Setup

**Tools**:
- Prometheus for metrics
- Grafana for dashboards
- Jaeger for distributed tracing
- ELK Stack for logs

**Dashboards to Create**:
1. System Overview
2. Service Health
3. API Performance
4. Database Performance
5. Cache Performance
6. Kafka Topics
7. User Activity

---

## Testing Strategy

### Test Coverage Targets

| Type | Coverage | Responsibility |
|------|----------|----------------|
| **Unit Tests** | 80%+ | Engineers |
| **Integration Tests** | 70%+ | Engineers |
| **E2E Tests** | Critical paths | QA Team |
| **Performance Tests** | All APIs | QA + DevOps |
| **Security Tests** | OWASP Top 10 | Security Team |

### Test Automation

**Frontend**:
- Vitest for unit tests
- React Testing Library for component tests
- Playwright for E2E tests

**Backend**:
- JUnit 5 for unit tests
- Spring Boot Test for integration tests
- K6 for load tests

---

## Release Plan

### Sprint Structure

- **Sprint Duration**: 2 weeks
- **Total Sprints**: 6 sprints in Q1
- **Sprint Planning**: Monday (2 hours)
- **Sprint Review**: Friday (1 hour)
- **Sprint Retrospective**: Friday (1 hour)

### Sprint Breakdown

**Sprint 1 (Jan 1-14)**: Project setup, authentication foundation  
**Sprint 2 (Jan 15-28)**: Clinical Portal - Patient Search  
**Sprint 3 (Jan 29-Feb 11)**: Clinical Portal - Care Gap Dashboard  
**Sprint 4 (Feb 12-25)**: Admin Portal + Agent Studio foundation  
**Sprint 5 (Feb 26-Mar 11)**: Agent Studio + Developer Portal  
**Sprint 6 (Mar 12-25)**: Bug fixes, testing, documentation  

**Release Date**: **March 28, 2026** (MVP Launch)

---

## Dependencies & Risks

### Critical Dependencies

1. **Claude API Access**: Need production API keys
2. **SSO Provider**: Customer needs Okta/Azure AD configured
3. **Infrastructure**: AWS accounts set up
4. **Legal**: BAAs with all subprocessors signed

### Key Risks

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Claude API downtime | Low | High | Implement fallback to Azure OpenAI |
| SSO integration complexity | Medium | Medium | Start with OAuth 2.0, add SAML later |
| Performance issues | Medium | High | Load testing early in sprint 3 |
| Security vulnerabilities | Low | Critical | Weekly security scans |

---

## Success Metrics (KPIs)

### Product Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Pilot Customers** | 5+ | Sign LOIs |
| **Clinical Query Time** | <5 sec | Measure p95 latency |
| **User Satisfaction** | 80%+ | NPS survey |
| **Uptime** | 99.5%+ | Uptime robot |
| **API Error Rate** | <1% | Prometheus |

### Engineering Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Test Coverage** | 75%+ | Jacoco/NYC |
| **Build Time** | <10 min | GitHub Actions |
| **Deploy Frequency** | Daily | Git log |
| **MTTR** | <1 hour | PagerDuty |

---

## Team Allocation

### Frontend Team (6 engineers)
- **Clinical Portal**: 3 engineers (full-time)
- **Admin Portal**: 1 engineer (full-time)
- **Agent Studio**: 1 engineer (full-time)
- **Developer Portal**: 1 engineer (50%), Shared (50%)

### Backend Team (6 engineers)
- **Core Services**: 3 engineers (enhancements)
- **AI Services**: 2 engineers (agent builder backend)
- **Infrastructure**: 1 engineer (auth, monitoring)

### QA Team (2 engineers)
- **Test Automation**: 1 engineer
- **Manual Testing**: 1 engineer

### DevOps (2 engineers)
- **CI/CD**: 1 engineer
- **Infrastructure**: 1 engineer

### Design (1 designer)
- **UI/UX**: All portals

**Total**: 17 FTEs

---

## Budget Estimate

| Category | Cost |
|----------|------|
| **Engineering** (17 FTEs × 3 months × $15K/month) | $765K |
| **Infrastructure** (AWS, staging + prod) | $30K |
| **Third-Party Services** (Claude API, Twilio, etc.) | $10K |
| **Tools** (GitHub, Figma, etc.) | $5K |
| **Total Q1 Budget** | **$810K** |

---

## Next Steps

1. Review and approve Q1 plan
2. Set up GitHub milestones and issues
3. Assign engineers to teams
4. Kick off Sprint 1 (Jan 1)
5. Weekly stakeholder updates

---

**Document Owner**: Product Manager  
**Last Updated**: January 14, 2026  
**Status**: Draft - Pending Approval
