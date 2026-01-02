# Reports Feature - Comprehensive Testing Guide

## Test Strategy Overview

This guide covers all testing approaches for the Reports feature, from unit tests to manual testing procedures.

## Test Pyramid

```
        /\
       /E2E\          11 smoke tests (UI validation)
      /------\
     /  Int.  \       API contract tests (pending backend)
    /----------\
   /    Unit    \     18 tests (100% service coverage)
  /--------------\
```

## 1. Unit Tests (✅ Complete - 18/18 Passing)

### Location
`apps/clinical-portal/src/app/services/evaluation.service.reports.spec.ts`

### Coverage Summary
- **Save Operations:** 5 tests (patient/population report generation)
- **Retrieve Operations:** 5 tests (get all, get by ID, filter by type)
- **Delete Operations:** 2 tests (success/error scenarios)
- **Export Operations:** 4 tests (CSV/Excel export)
- **Download Operations:** 2 tests (trigger browser download)

### Running Unit Tests
```bash
# Run all reports tests
npm run test -- --testPathPattern=evaluation.service.reports

# Run with coverage
npm run test:coverage -- --testPathPattern=evaluation.service.reports

# Watch mode for development
npm run test -- --watch --testPathPattern=evaluation.service.reports
```

### Expected Results
```
Test Suites: 1 passed
Tests:       18 passed
Time:        ~1 second
Coverage:    100% for EvaluationService report methods
```

### What's Tested
✅ HTTP request parameters are correct
✅ Response data is properly typed
✅ Error scenarios return appropriate errors
✅ Optional parameters have sensible defaults
✅ Blob responses handled correctly for exports
✅ File downloads triggered with correct filenames

### What's NOT Tested (Integration Layer)
❌ Actual backend API responses
❌ Database persistence
❌ Authentication/authorization
❌ Network timeouts/retries
❌ CORS configuration
❌ Multi-tenancy isolation

## 2. E2E Smoke Tests (✅ Complete - 11 tests)

### Location
`apps/clinical-portal-e2e/src/reports.e2e.spec.ts`

### Test Scenarios
1. **Page Load** (2 tests)
   - Page header displays
   - Both tabs visible

2. **Generate Reports Tab** (3 tests)
   - Patient report card visible
   - Population report card visible
   - Feature descriptions visible

3. **Saved Reports Tab** (4 tests)
   - Tab navigation works
   - Filter buttons visible
   - Empty state displays
   - Filter buttons clickable

4. **Responsive Design** (2 tests)
   - Mobile viewport (375x667)
   - Tablet viewport (768x1024)

### Running E2E Tests
```bash
# Ensure dev server is running first
npm run start &

# Run E2E tests
npx playwright test apps/clinical-portal-e2e/src/reports.e2e.spec.ts

# Run with UI (debugging)
npx playwright test --ui

# Run specific browser
npx playwright test --project=chromium
```

### Prerequisites
- Dev server running on port 4202
- No backend services required (UI-only validation)

### What's Tested
✅ Page renders without errors
✅ All UI elements are visible
✅ Tab navigation functions
✅ Buttons are clickable
✅ Responsive layouts work

### What's NOT Tested (Requires Backend)
❌ Report generation flow
❌ Data display in tables
❌ Export functionality
❌ Delete operations
❌ Error messages
❌ Loading states

## 3. Integration Tests (📋 Documented - Pending Backend)

### Test Specifications

Detailed test specifications are documented in `reports.e2e.spec.ts` under the `PENDING IMPLEMENTATION` section.

### Required Setup
1. **Backend Services Running:**
   - Quality Measure Service (port 8087)
   - Patient Service (port 8084)
   - FHIR Service (port 8085)
   - CQL Engine Service (port 8081)

2. **Test Data:**
   - Test user with credentials
   - Test patients in FHIR server
   - Sample quality measures

3. **Authentication:**
   - JWT token generation
   - Session management
   - Tenant context

### Integration Test Scenarios

#### Scenario 1: Generate Patient Report
**Steps:**
1. Click "Generate Patient Report"
2. Patient selection dialog opens
3. Select "John Doe" from list
4. Enter report name "Test Report 2024"
5. Click Generate
6. Wait for API call to complete
7. Navigate to Saved Reports
8. Verify new report in list

**API Calls:**
```
POST /quality-measure/report/patient/save
GET /quality-measure/reports?type=PATIENT
```

**Success Criteria:**
- ✅ Dialog opens within 500ms
- ✅ API call returns 200
- ✅ Report appears in list within 3s
- ✅ Toast notification shows "Success"

#### Scenario 2: Generate Population Report
**Steps:**
1. Click "Generate Population Report"
2. Year selection dialog opens
3. Select year "2024"
4. Enter report name "Population 2024"
5. Click Generate
6. Wait for processing
7. Navigate to Saved Reports
8. Verify new report in list

**API Calls:**
```
POST /quality-measure/report/population/save
GET /quality-measure/reports?type=POPULATION
```

#### Scenario 3: View Report Details
**Steps:**
1. Navigate to Saved Reports
2. Find report "Test Report 2024"
3. Click "View" button
4. Verify dialog displays:
   - Report name
   - Report type
   - Created date
   - Created by user
   - Evaluation results
5. Click close

**API Calls:**
```
GET /quality-measure/reports/{id}
```

#### Scenario 4: Export to CSV
**Steps:**
1. Navigate to Saved Reports
2. Click "CSV" button on a report
3. Verify file download starts
4. Verify filename format: `{reportName}.csv`
5. Verify file contains valid CSV data

**API Calls:**
```
GET /quality-measure/reports/{id}/export/csv
```

**File Validation:**
- CSV headers present
- Data rows match expected format
- UTF-8 encoding
- No malformed data

#### Scenario 5: Export to Excel
**Steps:**
1. Navigate to Saved Reports
2. Click "Excel" button on a report
3. Verify file download starts
4. Verify filename format: `{reportName}.xlsx`

**API Calls:**
```
GET /quality-measure/reports/{id}/export/excel
```

#### Scenario 6: Delete Report
**Steps:**
1. Navigate to Saved Reports
2. Click "Delete" button
3. Confirmation dialog appears
4. Click "Confirm"
5. Report removed from list
6. Success toast appears

**API Calls:**
```
DELETE /quality-measure/reports/{id}
GET /quality-measure/reports (refresh)
```

#### Scenario 7: Filter Reports
**Steps:**
1. Navigate to Saved Reports
2. Click "Patient" filter
3. Verify only patient reports shown
4. Click "Population" filter
5. Verify only population reports shown
6. Click "All Reports"
7. Verify all reports shown

**API Calls:**
```
GET /quality-measure/reports?type=PATIENT
GET /quality-measure/reports?type=POPULATION
GET /quality-measure/reports
```

### Error Scenarios

#### Network Failure
**Test:** Disconnect network during API call
**Expected:** Error toast with retry option

#### 401 Unauthorized
**Test:** Use expired JWT token
**Expected:** Redirect to login page

#### 404 Not Found
**Test:** Request non-existent report
**Expected:** Error toast "Report not found"

#### 500 Server Error
**Test:** Backend service unavailable
**Expected:** Error toast with user-friendly message

## 4. Manual Testing Checklist

### Pre-Test Setup
- [ ] All services running (Docker Compose)
- [ ] Test user created with credentials
- [ ] Test data loaded (patients, measures)
- [ ] Browser dev tools open (Network tab)

### Functional Testing

#### Report Generation
- [ ] Patient report dialog opens
- [ ] Patient search works
- [ ] Patient selection works
- [ ] Report name field accepts input
- [ ] Generate button enabled when valid
- [ ] Loading spinner shows during generation
- [ ] Success message appears
- [ ] New report appears in list

#### Report Viewing
- [ ] Saved Reports tab loads data
- [ ] Report cards display correctly
- [ ] Report type badge shows (Patient/Population)
- [ ] Created date formatted correctly
- [ ] Created by shows username
- [ ] View button opens details dialog
- [ ] Details display all fields

#### Export Functionality
- [ ] CSV export downloads file
- [ ] Excel export downloads file
- [ ] Files have correct names
- [ ] Files contain valid data
- [ ] Downloads don't block UI

#### Delete Functionality
- [ ] Delete button shows confirmation
- [ ] Cancel keeps report
- [ ] Confirm removes report
- [ ] List refreshes after delete
- [ ] Success message shows

#### Filtering
- [ ] Filter buttons toggle correctly
- [ ] Patient filter shows only patient reports
- [ ] Population filter shows only population reports
- [ ] All Reports shows everything
- [ ] Filter state persists on refresh

### UI/UX Testing

#### Visual Design
- [ ] Cards have proper spacing
- [ ] Buttons are properly styled
- [ ] Icons display correctly
- [ ] Colors match design system
- [ ] Typography is consistent

#### Responsive Design
- [ ] Mobile layout (< 768px)
- [ ] Tablet layout (768-1024px)
- [ ] Desktop layout (> 1024px)
- [ ] Cards stack on mobile
- [ ] Buttons remain accessible

#### Accessibility
- [ ] Keyboard navigation works
- [ ] Tab order is logical
- [ ] Focus indicators visible
- [ ] Screen reader labels present
- [ ] Color contrast sufficient
- [ ] Error messages readable

### Performance Testing

#### Load Times
- [ ] Initial page load < 2s
- [ ] Report list load < 1s
- [ ] Export download starts < 3s
- [ ] Delete operation < 500ms

#### Large Data Sets
- [ ] 100+ reports render smoothly
- [ ] Filtering is instant
- [ ] Scrolling is smooth
- [ ] No memory leaks on prolonged use

### Security Testing

#### Authentication
- [ ] Unauthenticated redirects to login
- [ ] Expired token triggers re-auth
- [ ] JWT includes tenant context

#### Authorization
- [ ] Users only see their tenant's reports
- [ ] Can't access other tenant reports by ID
- [ ] RBAC enforced on operations

#### Data Protection
- [ ] No PHI in console logs
- [ ] No PHI in error messages
- [ ] HTTPS for all API calls (production)

## 5. Test Data Management

### Creating Test Reports via API

```bash
# Generate patient report
curl -X POST http://localhost:8087/quality-measure/report/patient/save \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": "patient-123",
    "measureId": "CMS68v11",
    "tenantId": "TENANT001",
    "reportName": "Test Patient Report",
    "createdBy": "testuser"
  }'

# Generate population report
curl -X POST http://localhost:8087/quality-measure/report/population/save \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "year": 2024,
    "measureId": "CMS68v11",
    "tenantId": "TENANT001",
    "reportName": "Test Population Report",
    "createdBy": "testuser"
  }'
```

### Database Test Data

```sql
-- Create test reports directly
INSERT INTO saved_reports (
  id, report_name, report_type, measure_id, 
  patient_id, year, tenant_id, created_by, created_at
) VALUES
  (gen_random_uuid(), 'Test Report 1', 'PATIENT', 'CMS68v11', 
   'patient-123', NULL, 'TENANT001', 'testuser', NOW()),
  (gen_random_uuid(), 'Test Report 2', 'POPULATION', 'CMS68v11', 
   NULL, 2024, 'TENANT001', 'testuser', NOW());

-- Verify
SELECT * FROM saved_reports WHERE tenant_id = 'TENANT001';
```

## 6. CI/CD Integration

### GitHub Actions Workflow

```yaml
name: Reports Feature Tests

on: [push, pull_request]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
      - run: npm ci
      - run: npm run test -- --testPathPattern=evaluation.service.reports
      - uses: codecov/codecov-action@v3

  e2e-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
      - run: npm ci
      - run: npx playwright install
      - run: npm run start & npx wait-on http://localhost:4202
      - run: npx playwright test reports.e2e.spec.ts
      - uses: actions/upload-artifact@v3
        if: always()
        with:
          name: playwright-report
          path: playwright-report/
```

## 7. Test Metrics & Reporting

### Current Status
- **Unit Tests:** 18/18 passing (100% coverage for report methods)
- **E2E Smoke Tests:** 11/11 passing (UI validation only)
- **Integration Tests:** 0/24 pending (requires backend)

### Coverage Goals
- Unit Test Coverage: **✅ 100%** for EvaluationService reports
- E2E Coverage: **⚠️ 45%** (11 smoke tests of 24 planned scenarios)
- Integration Coverage: **❌ 0%** (pending backend setup)

### Known Gaps
1. Backend integration tests not implemented
2. Performance tests not automated
3. Security tests manual only
4. Load testing not configured
5. Accessibility audits manual

## 8. Troubleshooting Test Failures

### Unit Test Failures

**Issue:** "Cannot find module"
```bash
# Solution: Clear Jest cache
npm run test -- --clearCache
```

**Issue:** "Timeout exceeded"
```bash
# Solution: Increase timeout in jest.config
testTimeout: 10000
```

### E2E Test Failures

**Issue:** "page.goto: Protocol error"
```bash
# Solution: Check if dev server is running
lsof -i :4202
npm run start
```

**Issue:** "Element not visible"
```bash
# Solution: Add wait conditions
await page.waitForSelector('mat-card');
```

### Integration Test Issues

**Issue:** "Connection refused"
```bash
# Solution: Check backend services
docker-compose ps
curl http://localhost:8087/quality-measure/actuator/health
```

**Issue:** "401 Unauthorized"
```bash
# Solution: Regenerate JWT token
curl -X POST http://localhost:8084/auth/login \
  -d '{"username":"testuser","password":"password123"}'
```

## 9. Next Steps

### Short Term
1. ✅ Complete unit test coverage
2. ✅ Create E2E smoke tests
3. ✅ Document integration test specs
4. ⏳ Set up Docker Compose for full stack
5. ⏳ Configure JWT for quality-measure-service

### Medium Term
1. Implement full integration tests
2. Add performance benchmarks
3. Automate accessibility testing
4. Set up CI/CD pipeline
5. Create test data factories

### Long Term
1. Visual regression testing
2. Load/stress testing
3. Security penetration testing
4. Cross-browser compatibility
5. Mobile device testing

## Success Criteria

✅ **Code Quality**
- All unit tests passing
- No linting errors
- Type safety enforced
- Code coverage > 80%

✅ **Functionality**
- All user stories covered
- Error handling robust
- Loading states clear
- Success feedback immediate

✅ **Performance**
- Page load < 2s
- API calls < 1s
- Exports < 5s
- UI responsive

✅ **Accessibility**
- WCAG 2.1 AA compliant
- Keyboard navigable
- Screen reader compatible
- Color contrast adequate

✅ **Security**
- Authentication required
- Authorization enforced
- No PHI exposure
- Audit logging active
