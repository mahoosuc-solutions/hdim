# GitHub Issues for HDIM Production Release - Incomplete Features

**Generated**: January 23, 2026
**Total Issues**: 29 issues covering 95+ incomplete features
**Priority Breakdown**: 3 CRITICAL, 10 HIGH, 12 MEDIUM, 4 LOW

---

## 🔴 CRITICAL - Audit Dashboards (3 issues)

### Issue #1: [INCOMPLETE] QA Audit Dashboard Backend APIs

**Priority**: CRITICAL
**Component**: Backend API / AI Audit System
**Affects**: QA Audit Dashboard (frontend complete, backend missing)

**Description**:
The QA Audit Dashboard frontend is fully implemented but cannot function due to missing backend API endpoints. This blocks quality assurance review of AI-generated decisions.

**Missing Endpoints** (12 total):
- `GET /api/v1/audit/ai/qa/review-queue` - Fetch AI decisions awaiting QA review
- `GET /api/v1/audit/ai/qa/metrics` - QA metrics dashboard
- `GET /api/v1/audit/ai/qa/trends` - Trend analysis over time
- `POST /api/v1/audit/ai/qa/review/{id}/approve` - Approve AI decision
- `POST /api/v1/audit/ai/qa/review/{id}/reject` - Reject AI decision with rationale
- `POST /api/v1/audit/ai/qa/review/{id}/flag` - Flag for manual escalation
- `POST /api/v1/audit/ai/qa/review/{id}/false-positive` - Mark as false positive
- `POST /api/v1/audit/ai/qa/review/{id}/false-negative` - Mark as false negative
- `GET /api/v1/audit/ai/qa/review/{id}` - Get detailed review information
- `GET /api/v1/audit/ai/qa/report/export` - Export QA report (CSV/PDF)
- `PUT /api/v1/audit/ai/qa/review/{id}` - Update review decision
- `DELETE /api/v1/audit/ai/qa/review/{id}` - Delete review (soft delete)

**Location**:
- Frontend: `apps/clinical-portal/src/app/pages/qa-audit-dashboard/qa-audit-dashboard.component.ts`
- Backend: Missing - needs controller, service, DTOs

**Acceptance Criteria**:
- [ ] All 12 endpoints implemented with proper validation
- [ ] Review decisions persist to audit database
- [ ] RBAC enforced (only QA_ADMIN and SUPER_ADMIN can approve/reject)
- [ ] Audit trail tracks all QA actions
- [ ] Integration tests for all endpoints
- [ ] Frontend TODO comments removed

**Related Issues**: #2 (Clinical Audit), #3 (MPI Audit)

---

### Issue #2: [INCOMPLETE] Clinical Audit Dashboard Backend APIs

**Priority**: CRITICAL
**Component**: Backend API / Clinical Decision Support
**Affects**: Clinical Audit Dashboard

**Description**:
Clinical audit dashboard requires backend endpoints to track AI-generated clinical recommendations and provider acceptance rates.

**Missing Endpoints** (9 total):
- `GET /api/v1/audit/ai/decisions?agentType=CLINICAL_*` - Get clinical AI decisions
- `GET /api/v1/audit/clinical/metrics` - Clinical decision metrics
- `GET /api/v1/audit/clinical/trends` - Clinical trend analysis
- `POST /api/v1/clinical/decisions/{id}/accept` - Provider accepts recommendation
- `POST /api/v1/clinical/decisions/{id}/reject` - Provider rejects with rationale
- `POST /api/v1/clinical/decisions/{id}/modify` - Provider modifies recommendation
- `GET /api/v1/audit/clinical/report/export` - Export clinical audit report
- `GET /api/v1/audit/clinical/acceptance-rate` - Calculate acceptance rates by provider
- `GET /api/v1/audit/clinical/outcomes` - Track clinical outcomes post-decision

**Location**:
- Frontend: `apps/clinical-portal/src/app/pages/clinical-audit-dashboard/clinical-audit-dashboard.component.ts`
- Backend: Missing

**Acceptance Criteria**:
- [ ] All 9 endpoints implemented
- [ ] Provider feedback captured and stored
- [ ] Acceptance rate calculation accurate
- [ ] Integration with CDS engine
- [ ] HIPAA-compliant audit logging
- [ ] Unit and integration tests

**Related Issues**: #1 (QA Audit), #3 (MPI Audit)

---

### Issue #3: [INCOMPLETE] MPI Audit Dashboard Backend APIs

**Priority**: CRITICAL
**Component**: Backend API / Master Patient Index
**Affects**: MPI Audit Dashboard

**Description**:
MPI audit dashboard tracks patient merge operations and data quality issues detected by AI.

**Missing Endpoints** (8 total):
- `GET /api/v1/audit/ai/user-actions?actionType=MPI_*` - Get MPI merge events
- `GET /api/v1/audit/mpi/metrics` - MPI data quality metrics
- `GET /api/v1/audit/mpi/merge-conflicts` - Active merge conflicts
- `POST /api/v1/mpi/merges/{id}/validate` - Validate merge correctness
- `POST /api/v1/mpi/merges/{id}/rollback` - Rollback erroneous merge
- `POST /api/v1/mpi/data-quality/{id}/resolve` - Resolve data quality issue
- `GET /api/v1/audit/mpi/report/export` - Export MPI audit report
- `GET /api/v1/mpi/duplicate-detection` - AI-detected potential duplicates

**Location**:
- Frontend: `apps/clinical-portal/src/app/pages/mpi-audit-dashboard/mpi-audit-dashboard.component.ts`
- Backend: Missing

**Acceptance Criteria**:
- [ ] All 8 endpoints implemented
- [ ] Merge rollback safely reverses changes
- [ ] Duplicate detection algorithm integrated
- [ ] Data quality scoring implemented
- [ ] Audit trail captures all MPI operations
- [ ] Integration tests for merge scenarios

**Related Issues**: #1 (QA Audit), #2 (Clinical Audit), #10 (Patient Merge Chains)

---

## 🟠 HIGH - Real-Time Features (4 issues)

### Issue #4: [INCOMPLETE] Clinical Workflow WebSocket Notifications

**Priority**: HIGH
**Component**: Backend / WebSocket
**Affects**: Real-time vital sign alerts

**Description**:
Clinical Workflow Service has stubbed WebSocket notification code for real-time vital sign alerts to providers.

**Location**:
- File: `backend/modules/services/clinical-workflow-service/src/main/java/com/healthdata/clinicalworkflow/application/VitalSignsService.java`
- Lines: 332-334

**TODO Comment**:
```java
// TODO: Implement WebSocket real-time notifications to providers
// Should push critical vital sign alerts to connected web clients
```

**Acceptance Criteria**:
- [ ] WebSocket handler configured in Spring Boot
- [ ] Client connection management (connect/disconnect)
- [ ] Alert routing by provider ID and patient assignment
- [ ] Message format defined (JSON schema)
- [ ] Frontend WebSocket client implementation
- [ ] Alert acknowledgment tracking
- [ ] Integration tests for WebSocket communication

**Technical Notes**:
- Use Spring WebSocket with STOMP protocol
- Secure WebSocket connections with JWT authentication
- Implement heartbeat/keepalive mechanism
- Handle connection failures gracefully

**Related Issues**: #5 (Demo Orchestrator WebSocket), #6 (AI Audit Dashboard SSE)

---

### Issue #5: [INCOMPLETE] Demo Orchestrator WebSocket Publishing

**Priority**: HIGH
**Component**: Backend / WebSocket
**Affects**: Demo mode real-time updates

**Description**:
Demo Orchestrator Service has TODO placeholders for WebSocket event publishing.

**Location**:
- File: `backend/modules/services/demo-orchestrator-service/src/main/java/com/healthdata/demo/orchestrator/integration/DevOpsAgentClient.java`
- Lines: 35, 40

**Missing Implementation**:
- WebSocket handler setup
- Event broadcasting to subscribed clients
- Demo scenario progress updates

**Acceptance Criteria**:
- [ ] WebSocket endpoint `/ws/demo-events` created
- [ ] Event types defined (SCENARIO_STARTED, PATIENT_CREATED, etc.)
- [ ] Broadcasting to all demo session subscribers
- [ ] Frontend integration with event display
- [ ] Unit tests for event publishing

**Related Issues**: #4 (Clinical Workflow WebSocket), #7 (Demo Seeding WebSocket Upgrade)

---

### Issue #6: [INCOMPLETE] AI Audit Dashboard Real-Time Events

**Priority**: HIGH
**Component**: Frontend / Backend
**Affects**: AI Audit Dashboard live updates

**Description**:
AI Audit Dashboard currently uses polling but should use WebSocket or Server-Sent Events for real-time audit event streaming.

**Location**:
- File: `apps/clinical-portal/src/app/components/ai-audit-dashboard/ai-audit-dashboard.component.ts`
- Line: 71

**TODO Comment**:
```typescript
// TODO: Replace polling with WebSocket/SSE for real-time events
```

**Current Behavior**: Polls every 5 seconds
**Desired Behavior**: Live event stream with <100ms latency

**Acceptance Criteria**:
- [ ] Backend SSE endpoint `/api/v1/audit/stream` created
- [ ] Event filtering by agent type, severity, date range
- [ ] Frontend EventSource integration
- [ ] Automatic reconnection on disconnect
- [ ] Backpressure handling for high event volume
- [ ] Performance testing (1000+ events/sec)

**Related Issues**: #4, #5 (WebSocket implementations)

---

### Issue #7: [ENHANCEMENT] Demo Seeding Progress WebSocket Upgrade

**Priority**: MEDIUM
**Component**: Backend / Frontend
**Affects**: Demo Seeding Data Flow visualization

**Description**:
Demo Seeding Data Flow component currently polls for progress updates. Upgrade to WebSocket for smoother real-time visualization.

**Location**:
- File: `apps/clinical-portal/src/app/demo-mode/components/demo-seeding-data-flow/demo-seeding-data-flow.component.ts`

**Current Implementation**: Polling every 1 second
**Proposed**: WebSocket with progress events

**Acceptance Criteria**:
- [ ] WebSocket endpoint `/ws/demo-seeding/{sessionId}` created
- [ ] Progress events (GENERATING, PERSISTING, etc.) streamed
- [ ] Error events with rollback capability
- [ ] Completion event with summary statistics
- [ ] Frontend animation synced with events
- [ ] Graceful degradation to polling if WebSocket fails

**Related Issues**: #5 (Demo Orchestrator WebSocket)

---

## 🟡 MEDIUM - Business Logic Stubs (15 issues)

### Issue #8: [INCOMPLETE] Patient Service Age Range Filtering

**Priority**: MEDIUM
**Component**: Backend / Patient Service
**Affects**: Patient search and filtering

**Description**:
Patient Service has stubbed age range filtering in search queries.

**Location**:
- File: `healthdata-platform/src/main/java/com/healthdata/patient/service/PatientService.java`
- Line: 322

**TODO Comment**:
```java
// TODO: Implement age range filtering (e.g., 18-65 years)
```

**Acceptance Criteria**:
- [ ] Add `minAge` and `maxAge` parameters to `PatientSearchRequest`
- [ ] Calculate age from `birthDate` field
- [ ] Filter results by age range
- [ ] Handle edge cases (unknown birth date, future dates)
- [ ] Add unit tests for age calculations
- [ ] Update API documentation

---

### Issue #9: [INCOMPLETE] Clinical Workflow Patient Name Fetching

**Priority**: MEDIUM
**Component**: Backend / Clinical Workflow Service
**Affects**: Provider dashboard displays

**Description**:
Clinical Workflow Service returns `null` for patient names instead of fetching from Patient Service.

**Location**:
- File: `backend/modules/services/clinical-workflow-service/src/main/java/com/healthdata/clinicalworkflow/application/VitalSignsService.java`
- Lines: 709-750

**Current Behavior**: Returns `null`
**Expected Behavior**: Fetch patient demographics from Patient Service via REST client

**Acceptance Criteria**:
- [ ] Create Feign client for Patient Service
- [ ] Fetch patient name by patient ID
- [ ] Cache patient demographics (5-minute TTL)
- [ ] Handle patient-not-found gracefully
- [ ] Update response DTOs with patient name
- [ ] Add integration tests

**Related Issues**: #TODO (Room Number Fetching)

---

### Issue #10: [INCOMPLETE] Patient Event Handler Merge Chain Resolution

**Priority**: HIGH
**Component**: Backend / Patient Event Handler
**Affects**: Master Patient Index (MPI)

**Description**:
Patient identifier resolution for merged patient chains is not implemented.

**Location**:
- File: `backend/modules/services/patient-event-handler-service/src/main/java/com/healthdata/patientevent/service/PatientIdentifierResolver.java`
- Line: 185

**TODO Comment**:
```java
// TODO: Query repository for full patient merge chain
// Need to traverse merge history to find canonical patient ID
```

**Current Behavior**: Only resolves direct merges
**Expected Behavior**: Traverse full merge chain (A → B → C → D)

**Acceptance Criteria**:
- [ ] Implement recursive merge chain traversal
- [ ] Detect and handle circular merge references
- [ ] Cache merge chains for performance
- [ ] Add audit logging for merge chain resolutions
- [ ] Unit tests for complex merge scenarios
- [ ] Integration tests with MPI service

**Related Issues**: #3 (MPI Audit Dashboard)

---

### Issue #11: [INCOMPLETE] Quality Measure CQL Engine Integration

**Priority**: HIGH
**Component**: Backend / Quality Measure Service
**Affects**: Custom measure evaluation

**Description**:
Custom Measure Service has stubbed CQL engine integration for evaluating custom clinical quality measures.

**Location**:
- File: `backend/modules/services/quality-measure-service/src/main/java/com/healthdata/quality/service/CustomMeasureService.java`
- Lines: 247, 302

**TODO Comments**:
```java
// TODO: Integrate with CQL Engine for custom measure evaluation
// Currently returns mock results
```

**Acceptance Criteria**:
- [ ] Create Feign client for CQL Engine Service
- [ ] Parse CQL measure definitions
- [ ] Execute CQL logic against patient data
- [ ] Return structured evaluation results
- [ ] Cache evaluation results (24-hour TTL)
- [ ] Handle CQL syntax errors gracefully
- [ ] Add integration tests with real CQL measures

**Related Issues**: #TODO (CQL Validation)

---

### Issues #12-21: [INCOMPLETE] Clinical Decision Service TODO Items

**Priority**: MEDIUM
**Component**: Backend / Clinical Decision Service
**Affects**: AI-powered clinical recommendations

**Description**:
Clinical Decision Service has 10+ TODO placeholders for incomplete business logic.

**Location**:
- File: `backend/modules/shared/infrastructure/audit/src/main/java/com/healthdata/audit/service/clinical/ClinicalDecisionService.java`
- Lines: 296, 315, 340-343, 356-359, 392-417

**Missing Implementations**:
1. **Related Alerts Counting** (Line 296)
   - Count related clinical alerts for context

2. **Review History Building** (Line 315)
   - Build provider review history for decision

3. **Care Gap Details Extraction** (Lines 340-343)
   - Extract care gap details from JSON

4. **Contributing Factors Extraction** (Lines 356-359)
   - Extract clinical factors that led to decision

5. **JSON-to-Object Transformations** (Lines 392-417)
   - 5 incomplete transformations for different data types

**Acceptance Criteria** (for each item):
- [ ] Implement missing business logic
- [ ] Add error handling for malformed data
- [ ] Add unit tests with edge cases
- [ ] Update API documentation
- [ ] Remove TODO comments

**Consolidate into separate issues**:
- Issue #12: Related Alerts Counting
- Issue #13: Review History Building
- Issue #14: Care Gap Details Extraction
- Issue #15: Contributing Factors Extraction
- Issues #16-20: JSON Transformation Methods
- Issue #21: Integration Testing for Clinical Decisions

---

### Issues #22-27: [INCOMPLETE] AI Audit Event Store Features

**Priority**: MEDIUM
**Component**: Backend / AI Audit System
**Affects**: AI operations tracking and compliance

**Description**:
AI Audit Event Store has 6 major TODO items for tracking AI configuration changes, performance, alerts, user behavior, feedback, and compliance.

**Location**:
- File: `backend/modules/shared/infrastructure/audit/src/main/java/com/healthdata/audit/service/ai/AIAuditEventStore.java`
- Lines: 260, 271, 282, 332, 343, 354

**Missing Features**:
1. **Configuration History Tracking** (Line 260)
   - Track AI model configuration changes over time

2. **Performance Impact Tracking** (Line 271)
   - Measure AI decision latency and resource usage

3. **Alerting System** (Line 282)
   - Alert on anomalous AI behavior (accuracy drop, high error rate)

4. **User Behavior Tracking** (Line 332)
   - Track how providers interact with AI recommendations

5. **Feedback Processing** (Line 343)
   - Process and learn from provider feedback

6. **Compliance Logging** (Line 354)
   - Log AI operations for regulatory compliance (21 CFR Part 11, GDPR)

**Consolidate into separate issues**:
- Issue #22: AI Configuration History Tracking
- Issue #23: AI Performance Impact Metrics
- Issue #24: AI Behavior Alerting System
- Issue #25: Provider-AI Interaction Tracking
- Issue #26: AI Feedback Learning Pipeline
- Issue #27: AI Compliance Audit Logging

---

## 🟢 LOW - Tech Debt & Enhancements (4 issues)

### Issue #28: [TECH-DEBT] Remove Deprecated Authentication & Notification Code

**Priority**: LOW
**Component**: Backend / Multiple Services
**Affects**: Code maintainability

**Description**:
8 deprecated methods across 2 services should be removed to reduce technical debt.

**Deprecated Code**:
1. **CQL Engine Service** (2 classes):
   - `JwtTokenService.java` - @Deprecated
   - `JwtAuthenticationFilter.java` - @Deprecated

2. **Quality Measure Service** (2 classes):
   - `SmsNotificationChannel.java` - @Deprecated
   - `EmailNotificationChannel.java` - @Deprecated

3. **Demo Seeding Service** (1 method):
   - `SyntheticPatientGenerator.java` - 1 deprecated method

**Acceptance Criteria**:
- [ ] Verify no references to deprecated code exist
- [ ] Remove deprecated classes and methods
- [ ] Update migration guides if needed
- [ ] Run full test suite to ensure no breaks
- [ ] Update CHANGELOG.md

**Migration Path**:
- JWT authentication → Gateway Trust Architecture (already implemented)
- SMS/Email notifications → New notification service (already implemented)

---

### Issue #29: [EPIC] Complete Frontend-Backend API Integration

**Priority**: MEDIUM
**Component**: Frontend / Backend
**Affects**: Multiple dashboards and features

**Description**:
Meta-issue tracking 65+ TODO API calls across frontend components awaiting backend implementation.

**Affected Components**:
- QA Audit Dashboard (25 TODO calls)
- Clinical Audit Dashboard (18 TODO calls)
- MPI Audit Dashboard (12 TODO calls)
- Other dashboards (10 TODO calls)

**Tracking**:
This is an EPIC that tracks completion of Issues #1-27.

**Progress**:
- [ ] 0 / 65 API integrations complete

**Related Issues**: #1-#27 (all incomplete backend features)

---

## 📋 Implementation Priority Order

### Sprint 1 (Weeks 1-2) - Critical Audit Dashboards
1. Issue #1: QA Audit Dashboard APIs
2. Issue #2: Clinical Audit Dashboard APIs
3. Issue #3: MPI Audit Dashboard APIs

### Sprint 2 (Weeks 3-4) - Real-Time Features
4. Issue #4: Clinical Workflow WebSocket
5. Issue #5: Demo Orchestrator WebSocket
6. Issue #6: AI Audit Dashboard SSE
7. Issue #10: Patient Merge Chain Resolution
8. Issue #11: CQL Engine Integration

### Sprint 3 (Weeks 5-6) - Business Logic Completion
9. Issues #8-9: Patient Service features
10. Issues #12-21: Clinical Decision Service TODOs
11. Issues #22-27: AI Audit Event Store features

### Sprint 4 (Week 7) - Tech Debt & Polish
12. Issue #7: Demo Seeding WebSocket Upgrade
13. Issue #28: Remove Deprecated Code
14. Issue #29: Frontend-Backend Integration (verification)

---

## 📊 Estimated Effort

| Priority | Issues | Estimated Hours |
|----------|--------|-----------------|
| CRITICAL | 3 | 80-120 hours |
| HIGH | 4 + 2 | 60-80 hours |
| MEDIUM | 15 | 120-160 hours |
| LOW | 4 | 20-30 hours |
| **TOTAL** | **29** | **280-390 hours** |

**Recommended Team Size**: 2-3 backend engineers, 1 frontend engineer
**Estimated Calendar Time**: 8-12 weeks (2-3 sprints)

---

## 🚀 Release Strategy

**v1.0 (MVP)** - Core clinical features (Weeks 1-2)
- Deploy audit dashboards with mock data
- Mark real-time features as "Coming Soon"
- Timeline: 2-3 weeks

**v1.1 (Audit & Analytics)** - Add audit capabilities (Weeks 3-6)
- Implement all audit dashboard endpoints
- Enable real-time event streaming
- Timeline: +4-6 weeks

**v2.0 (Full Release)** - Complete all features (Weeks 7-8)
- Finish all TODO items
- Remove deprecated code
- Full WebSocket support
- Timeline: +2 weeks

---

_Generated by: Claude Code_
_Date: January 23, 2026_
_Version: 1.0_
