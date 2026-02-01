# Audit Dashboard End-to-End Testing Guide

**Date**: January 23, 2026
**Status**: Phase 3 Complete - Ready for Testing
**Services Required**: audit-query-service (port 8088), Angular dev server (port 4200)

---

## Overview

This guide provides step-by-step instructions for testing all three integrated audit dashboards:

1. **QA Audit Dashboard** - Quality analyst review of AI decisions
2. **Clinical Audit Dashboard** - Clinical staff review of AI clinical recommendations
3. **MPI Audit Dashboard** - MPI administrator review of patient merge operations

---

## Prerequisites

### 1. Backend Services Running

Verify the audit-query-service is healthy:

```bash
curl http://localhost:8088/actuator/health
# Expected: {"status":"UP"}
```

If not running, start it:

```bash
docker compose --profile core up -d audit-query-service
```

### 2. Frontend Dev Server Running

Verify the Angular dev server is running:

```bash
curl -s http://localhost:4200 | head -5
# Expected: HTML output with <!DOCTYPE html>
```

If not running, start it:

```bash
npm exec nx serve clinical-portal
```

### 3. Proxy Configuration Verified

The following proxy routes should be configured in `apps/clinical-portal/proxy.conf.json`:

```json
"/api/v1/audit": {
  "target": "http://localhost:8088",
  "secure": false,
  "changeOrigin": true,
  "logLevel": "debug"
},
"/api/v1/clinical": {
  "target": "http://localhost:8088",
  "secure": false,
  "changeOrigin": true,
  "logLevel": "debug"
},
"/api/v1/mpi": {
  "target": "http://localhost:8088",
  "secure": false,
  "changeOrigin": true,
  "logLevel": "debug"
}
```

---

## Test Scenarios

### Scenario 1: QA Audit Dashboard

**Purpose**: Quality analysts review AI agent decisions for approval, rejection, or flagging.

**Navigation**: `http://localhost:4200` → Login → QA Audit Dashboard

**Test Steps**:

1. **Load Review Queue**:
   - Verify pending AI decisions load automatically
   - Check for loading indicators
   - Expected: List of AI decisions with confidence scores

2. **Test Filters**:
   - **Agent Type Filter**: Select "Care Gap Detection", "Risk Stratification", "Measure Evaluation"
   - **Confidence Range**: Select "High (>80%)", "Medium (50-80%)", "Low (<50%)"
   - **Date Range**: Select "Today", "This Week", "This Month"
   - **Include Reviewed**: Toggle on/off
   - Expected: Review queue updates based on selected filters

3. **Test QA Metrics**:
   - Verify metrics dashboard shows:
     - Total Reviewed
     - Approved Decisions
     - Rejected Decisions
     - False Positives
     - False Negatives
     - Average Confidence Score

4. **Test Review Actions**:
   - **Approve Decision**:
     - Click "Approve" on a pending decision
     - Add review notes
     - Expected: Decision marked as approved, removed from queue, metrics updated

   - **Reject Decision**:
     - Click "Reject" on a pending decision
     - Provide rejection reason
     - Expected: Decision marked as rejected, removed from queue, metrics updated

   - **Flag for Manual Review**:
     - Click "Flag" on a pending decision
     - Provide flag reason
     - Expected: Decision flagged, sent to manual review queue

   - **Mark False Positive**:
     - Click "False Positive" on a decision
     - Provide context
     - Expected: Decision marked, used for AI model improvement

   - **Mark False Negative**:
     - Click "False Negative" on a decision
     - Provide context
     - Expected: Decision marked, used for AI model improvement

5. **Test Detail View**:
   - Click "View Details" on a decision
   - Expected: Modal/detail panel showing:
     - Full AI reasoning
     - Confidence breakdown
     - Evidence/source data
     - Review history

6. **Test Export**:
   - Click "Export QA Report"
   - Expected: Excel file downloads with name `qa-audit-report-YYYY-MM-DD.xlsx`
   - Verify file contains review queue data

7. **Test Real-Time Updates** (if SSE enabled):
   - Open browser console
   - Look for SSE connection: `EventSource` connection to `/api/v1/audit/ai/events/stream`
   - Expected: Console shows new events as they arrive (heartbeat every 5 seconds)

---

### Scenario 2: Clinical Audit Dashboard

**Purpose**: Clinical staff review AI clinical decision support recommendations.

**Navigation**: `http://localhost:4200` → Login → Clinical Audit Dashboard

**Test Steps**:

1. **Load Clinical Decisions**:
   - Verify AI clinical recommendations load automatically
   - Check for loading indicators
   - Expected: List of clinical decisions with priorities

2. **Test Filters**:
   - **Decision Type**: Select "Care Gap Alert", "Medication Alert", "Lab Value Alert"
   - **Priority**: Select "Critical", "High", "Medium", "Low"
   - **Status**: Select "Pending", "Accepted", "Rejected", "Modified"
   - **Patient Search**: Enter patient ID or name
   - **Date Range**: Select date range
   - Expected: Decisions list updates based on filters

3. **Test Clinical Metrics**:
   - Verify metrics dashboard shows:
     - Total Decisions
     - Accepted Recommendations
     - Rejected Recommendations
     - Modified Recommendations
     - Average Acceptance Rate

4. **Test Review Actions**:
   - **Accept Recommendation**:
     - Click "Accept" on a pending decision
     - Add clinical notes
     - Expected: Decision marked as accepted, clinical workflow triggered

   - **Reject Recommendation**:
     - Click "Reject" on a pending decision
     - Provide clinical rationale (required)
     - Add clinical notes
     - Expected: Decision marked as rejected, rationale recorded

   - **Modify Recommendation**:
     - Click "Modify" on a pending decision
     - Describe modifications
     - Add clinical reasoning
     - Expected: Decision modified, alternative action recorded

5. **Test Detail View**:
   - Click decision card to view details
   - Expected: Modal showing:
     - Patient context
     - AI recommendation details
     - Clinical evidence
     - Related alerts
     - Review history

6. **Test Export**:
   - Click "Export Clinical Report"
   - Expected: Excel file downloads with name `clinical-audit-report-YYYY-MM-DD.xlsx`
   - Verify file contains clinical decisions data

---

### Scenario 3: MPI Audit Dashboard

**Purpose**: MPI administrators audit patient identity merge/unmerge operations.

**Navigation**: `http://localhost:4200` → Login → MPI Audit Dashboard

**Test Steps**:

1. **Load MPI Events**:
   - Verify MPI merge events load automatically
   - Check for loading indicators
   - Expected: List of patient merge operations

2. **Test Filters**:
   - **Event Type**: Select "Patient Merge", "Patient Unmerge", "Identity Resolution", "Duplicate Detection"
   - **Date Range**: Select date range
   - **Tenant Filter**: Select tenant (multi-tenant testing)
   - **User Filter**: Enter user ID
   - Expected: Events list updates based on filters

3. **Test MPI Metrics**:
   - Verify metrics dashboard shows:
     - Total Merges
     - Total Unmerges
     - Pending Resolutions
     - Data Quality Issues
     - Duplicates Detected
     - Auto-Merged Records
     - Manual Review Required

4. **Test Match Quality Metrics**:
   - Verify match quality section shows:
     - High Confidence Matches
     - Medium Confidence Matches
     - Low Confidence Matches
     - Average Match Score

5. **Test Merge Actions**:
   - **Validate Merge**:
     - Click "Validate" on a completed merge
     - Expected: Merge marked as validated

   - **Rollback Merge**:
     - Click "Rollback" on a merge event
     - Confirm rollback action
     - Expected: Confirmation dialog, merge rolled back, patients unmerged

6. **Test Data Quality Issues**:
   - Navigate to "Data Quality" tab
   - Verify issues load
   - **Resolve Issue**:
     - Click "Resolve" on a data quality issue
     - Expected: Issue marked as resolved, removed from list

7. **Test Event Details**:
   - Click "View Details" on a merge event
   - Expected: Modal showing:
     - Source patient ID
     - Target patient ID
     - Match score
     - Merge reason
     - Validation errors (if any)
     - Merge status
     - Timestamp

8. **Test Export**:
   - Click "Export MPI Report"
   - Expected: Excel file downloads with name `mpi-audit-report-YYYY-MM-DD.xlsx`
   - Verify file contains MPI events data

---

## Network Testing (Browser DevTools)

### 1. Verify API Calls

Open browser DevTools (F12) → Network tab:

**Expected API calls for QA Dashboard**:
```
GET http://localhost:4200/api/v1/audit/ai/qa/review-queue?agentType=...
GET http://localhost:4200/api/v1/audit/ai/qa/metrics
GET http://localhost:4200/api/v1/audit/ai/qa/trends
POST http://localhost:4200/api/v1/audit/ai/qa/review/{id}/approve
POST http://localhost:4200/api/v1/audit/ai/qa/review/{id}/reject
POST http://localhost:4200/api/v1/audit/ai/qa/review/{id}/flag
```

**Expected API calls for Clinical Dashboard**:
```
GET http://localhost:4200/api/v1/audit/ai/decisions?agentType=CLINICAL_...
GET http://localhost:4200/api/v1/audit/clinical/metrics
POST http://localhost:4200/api/v1/clinical/decisions/{id}/accept
POST http://localhost:4200/api/v1/clinical/decisions/{id}/reject
POST http://localhost:4200/api/v1/clinical/decisions/{id}/modify
```

**Expected API calls for MPI Dashboard**:
```
GET http://localhost:4200/api/v1/audit/ai/user-actions?actionType=MPI_...
GET http://localhost:4200/api/v1/audit/mpi/metrics
POST http://localhost:4200/api/v1/mpi/merges/{id}/validate
POST http://localhost:4200/api/v1/mpi/merges/{id}/rollback
POST http://localhost:4200/api/v1/mpi/data-quality/{id}/resolve
```

### 2. Verify Proxy Routing

In Network tab, check:
- **Request URL**: Should be `http://localhost:4200/api/v1/audit/...` (proxied)
- **Status Code**: Should be `200 OK` (if backend is working) or `401 Unauthorized` (if authentication needed)
- **Response Headers**: Should include `X-Forwarded-For`, `X-Forwarded-Proto` from proxy

### 3. Check Console for Errors

Open browser Console tab:

**Expected**:
- ✅ No console errors (ESLint enforces no console.log)
- ✅ LoggerService messages (filtered in production)
- ✅ Audit logging messages (HIPAA-compliant)

**Red Flags**:
- ❌ CORS errors (proxy misconfigured)
- ❌ 404 errors (backend endpoints missing)
- ❌ 500 errors (backend service errors)
- ❌ Console.log statements (HIPAA violation)

---

## Authentication Testing

### 1. Login

Navigate to `http://localhost:4200`:

**Test Credentials**:
```
Username: admin@acme-health
Password: Test2026
```

**Expected**:
- Login successful
- JWT token stored in localStorage
- Redirect to dashboard

### 2. Session Timeout (HIPAA §164.312(a)(2)(iii))

**Test Steps**:
1. Login to application
2. Wait 13 minutes (idle)
3. Expected: Warning appears "Session expiring in 2 minutes"
4. Click "Stay Logged In" → Session extends
5. OR wait 2 more minutes → Automatically logged out at 15:00 mark

**Audit Logging**:
- Session timeout events logged with idle duration
- Logout events logged with reason (IDLE_TIMEOUT or EXPLICIT_LOGOUT)

---

## Error Handling Testing

### 1. Backend Service Down

**Test**:
1. Stop audit-query-service: `docker compose stop audit-query-service`
2. Navigate to QA Audit Dashboard
3. Expected: User-friendly error message, audit logged, app does NOT crash

### 2. Invalid Filters

**Test**:
1. Enter invalid date range (end date before start date)
2. Expected: Validation error, request not sent to backend

### 3. Network Errors

**Test**:
1. Disconnect network
2. Try to load dashboard
3. Expected: Timeout message, retry option

---

## Performance Testing

### 1. Large Dataset Loading

**Test**:
1. Load dashboard with 1,000+ audit events
2. Expected:
   - Loading indicator appears
   - Pagination works correctly
   - UI remains responsive
   - No memory leaks

### 2. Concurrent Actions

**Test**:
1. Open QA Audit Dashboard in 3 browser tabs
2. Approve/reject decisions in parallel
3. Expected:
   - All actions succeed
   - No race conditions
   - Metrics update correctly

---

## Accessibility Testing (WCAG 2.1 Level A)

### 1. Keyboard Navigation

**Test**:
- Tab through all dashboard elements
- Expected: Focus indicators visible, logical tab order

### 2. Screen Reader

**Test**:
- Use screen reader (NVDA, JAWS, VoiceOver)
- Expected: ARIA labels read correctly, roles announced

### 3. Color Contrast

**Test**:
- Use browser accessibility inspector
- Expected: All text meets WCAG AA contrast ratio (4.5:1)

---

## Known Issues / Expected Failures

### Backend Not Implemented (Expected 404s)

The following endpoints may return 404 until backend is fully implemented:

**QA Audit**:
- `GET /api/v1/audit/ai/qa/review-queue` - Backend controller missing
- `POST /api/v1/audit/ai/qa/review/{id}/approve` - Backend controller missing

**Clinical Audit**:
- `GET /api/v1/audit/ai/decisions` - Backend controller missing
- `POST /api/v1/clinical/decisions/{id}/accept` - Backend controller missing

**MPI Audit**:
- `GET /api/v1/audit/ai/user-actions` - Backend controller missing
- `POST /api/v1/mpi/merges/{id}/validate` - Backend controller missing

**Workaround**: Dashboards display mock data from `loadMockData()` methods until backend is implemented.

---

## Success Criteria

### Minimum Viable Testing (MVP) ✅

- [ ] All dashboards load without errors
- [ ] Filters work correctly
- [ ] Mock data displays properly
- [ ] Export functions trigger file downloads
- [ ] No console errors in browser
- [ ] No HIPAA violations (no console.log statements)

### Full Integration Testing (Production Ready)

- [ ] All API endpoints return 200 OK
- [ ] Actions (approve, reject, etc.) update backend
- [ ] Real-time SSE streaming works
- [ ] Metrics update correctly after actions
- [ ] Audit logging captures all user actions
- [ ] Session timeout works correctly
- [ ] Error handling graceful for all failure scenarios

---

## Troubleshooting

### Issue: Dashboard shows "Loading..." forever

**Cause**: Backend service not responding

**Fix**:
```bash
# Check service health
curl http://localhost:8088/actuator/health

# Restart service if needed
docker compose restart audit-query-service
```

### Issue: API calls return 401 Unauthorized

**Cause**: JWT token expired or invalid

**Fix**:
1. Logout and login again
2. Check JWT token in localStorage:
   - Open DevTools → Application → Local Storage
   - Look for `access_token` key
   - Decode JWT at https://jwt.io to verify expiration

### Issue: Proxy not routing correctly

**Cause**: Angular dev server not restarted after proxy changes

**Fix**:
```bash
# Stop dev server (Ctrl+C)
# Restart dev server
npm exec nx serve clinical-portal
```

### Issue: Browser console shows console.log statements

**Cause**: HIPAA compliance violation - code has console.log

**Fix**:
```bash
# Run linter to find violations
npm run lint

# Fix automatically where possible
npm run lint -- --fix
```

---

## Next Steps After Testing

Once testing is complete:

1. **Document Test Results**: Create test report with pass/fail status for each scenario
2. **File Backend Issues**: Create GitHub issues for missing backend endpoints
3. **Performance Optimization**: Identify slow API calls, optimize queries
4. **Security Review**: Verify HIPAA compliance, audit logging completeness
5. **User Acceptance Testing**: Share with stakeholders for feedback

---

**Testing Status**: Ready for manual testing (January 23, 2026)
**Backend Implementation**: 40% (mock data fallback active)
**Frontend Integration**: 100% (all 22 API calls wired up)
