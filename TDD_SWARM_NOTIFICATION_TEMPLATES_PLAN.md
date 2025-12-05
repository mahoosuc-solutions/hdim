# TDD Swarm Implementation Plan: Notification Template System

**Date**: November 27, 2025
**Status**: READY TO EXECUTE
**Methodology**: Test-Driven Development Swarm
**Duration**: 8 days (Week 2 of Notification Engine implementation)
**Team Size**: 3 developers (can be 1 developer in 3 roles)

---

## 📋 Table of Contents

1. [Executive Summary](#executive-summary)
2. [TDD Swarm Methodology](#tdd-swarm-methodology)
3. [Phase Breakdown](#phase-breakdown)
4. [Team Structure](#team-structure)
5. [Implementation Timeline](#implementation-timeline)
6. [Detailed Task Breakdown](#detailed-task-breakdown)
7. [Test Scenarios](#test-scenarios)
8. [Success Criteria](#success-criteria)
9. [Risk Mitigation](#risk-mitigation)

---

## Executive Summary

### Objective
Implement 6 additional email/SMS notification templates using TDD Swarm methodology to complete the notification template system foundation.

### Current State
- ✅ MVP Complete: ThymeleafTemplateRenderer, critical-alert templates, preview API
- ✅ Infrastructure: Template engines configured, test framework ready
- ✅ Pattern Established: clear-alert template serves as reference implementation

### Target State
- ✅ 7 complete template sets (14 files total: 7 HTML + 7 SMS)
- ✅ 100+ unit tests (comprehensive coverage for all templates)
- ✅ Mobile-responsive email templates
- ✅ HIPAA-compliant messaging
- ✅ Production-ready preview API with all templates

### Deliverables
1. **6 New Email Templates** (HTML):
   - care-gap.html
   - health-score.html
   - appointment-reminder.html
   - medication-reminder.html
   - lab-result.html
   - digest.html

2. **6 New SMS Templates** (TXT):
   - care-gap.txt
   - health-score.txt
   - appointment-reminder.txt
   - medication-reminder.txt
   - lab-result.txt
   - digest.txt

3. **90+ Additional Unit Tests**:
   - ~15 tests per template type
   - Edge cases and error scenarios
   - Performance benchmarks
   - Mobile responsiveness validation

4. **Updated Preview API**:
   - Sample data for all 7 templates
   - Template-specific variable documentation
   - Enhanced error handling

5. **Documentation**:
   - Template usage guide
   - Variable reference for each template
   - Testing report
   - Mobile compatibility matrix

---

## TDD Swarm Methodology

### What is TDD Swarm?

A collaborative development approach where:
1. **Tests are written FIRST** (before implementation)
2. **Small, focused iterations** (Red → Green → Refactor)
3. **Parallel workstreams** (multiple templates developed simultaneously)
4. **Continuous integration** (frequent merging and testing)
5. **Peer review** (code review after each template)

### TDD Cycle for Each Template

```
┌─────────────────────────────────────────────────┐
│  RED Phase: Write Failing Tests                │
│  - Write unit test for template rendering      │
│  - Test should fail (template doesn't exist)   │
│  - Expected: Compilation error or test failure │
│  Duration: 15-20 minutes per template          │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│  GREEN Phase: Make Tests Pass                  │
│  - Create minimal template to pass tests       │
│  - Implement required variables                │
│  - Add basic styling                           │
│  - Run tests until all pass                    │
│  Duration: 30-45 minutes per template          │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│  REFACTOR Phase: Improve Implementation        │
│  - Enhance styling and UX                      │
│  - Add mobile responsiveness                   │
│  - Optimize performance                        │
│  - Add optional features                       │
│  Duration: 20-30 minutes per template          │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│  VERIFY Phase: Quality Assurance               │
│  - Run full test suite                         │
│  - Manual preview testing                      │
│  - Mobile device testing                       │
│  - Peer code review                            │
│  Duration: 15-20 minutes per template          │
└─────────────────────────────────────────────────┘
```

### Swarm Principles

1. **Test-First Always**
   - No production code without failing test
   - Tests define the contract
   - Red → Green → Refactor cycle

2. **Small Batches**
   - One template at a time
   - Frequent commits (every GREEN phase)
   - Continuous integration

3. **Parallel Development**
   - Multiple templates can be developed simultaneously
   - Each template is independent
   - Merge frequently to avoid conflicts

4. **Quality Gates**
   - All tests must pass before merge
   - Code review required for each template
   - Mobile testing required before completion

5. **Living Documentation**
   - Tests serve as documentation
   - Examples in test cases
   - Sample data in preview API

---

## Phase Breakdown

### Phase 1: Care Gap & Health Score Templates (Days 1-2)

**Templates**: care-gap, health-score

**Why Together?**: Both are informational (not urgent), similar complexity

**Test Count**: ~30 tests total (15 per template)

**Complexity**: Medium

**Duration**: 2 days

### Phase 2: Appointment & Medication Reminders (Days 3-4)

**Templates**: appointment-reminder, medication-reminder

**Why Together?**: Both are reminder-based, time-sensitive, similar structure

**Test Count**: ~30 tests total (15 per template)

**Complexity**: Low-Medium

**Duration**: 2 days

### Phase 3: Lab Results Template (Day 5)

**Templates**: lab-result

**Why Separate?**: More complex data presentation, clinical significance

**Test Count**: ~18 tests

**Complexity**: Medium-High

**Duration**: 1 day

### Phase 4: Digest Template (Day 6)

**Templates**: digest

**Why Separate?**: Most complex (aggregates multiple notification types)

**Test Count**: ~20 tests

**Complexity**: High

**Duration**: 1 day

### Phase 5: Integration & Mobile Testing (Day 7)

**Activities**:
- Cross-template integration testing
- Mobile device testing (real devices)
- Email client compatibility testing
- Performance testing (all templates)
- Load testing (concurrent rendering)

**Duration**: 1 day

### Phase 6: Documentation & Final QA (Day 8)

**Activities**:
- Complete template usage guide
- Variable reference documentation
- Testing report compilation
- Final code review
- Production readiness checklist

**Duration**: 1 day

---

## Team Structure

### Team A: Template Developer (Primary)

**Responsibilities**:
- Write unit tests (RED phase)
- Implement templates (GREEN phase)
- Refactor and optimize (REFACTOR phase)
- Update preview API sample data

**Skills Required**:
- Java/Spring Boot
- Thymeleaf template engine
- HTML/CSS (responsive design)
- JUnit 5

**Time Commitment**: Full-time (8 days)

### Team B: QA Engineer (Secondary)

**Responsibilities**:
- Review test coverage
- Perform manual testing
- Test on mobile devices
- Verify email client compatibility
- Document test results

**Skills Required**:
- Manual testing
- Mobile device testing
- Email client testing tools
- Bug reporting

**Time Commitment**: Part-time (Days 5-8, 4 days)

### Team C: Reviewer (Tertiary)

**Responsibilities**:
- Code review after each template
- Architecture validation
- Security review (XSS, HIPAA)
- Performance review
- Approve merge requests

**Skills Required**:
- Java/Spring Boot expertise
- Security best practices
- Healthcare compliance (HIPAA)
- Code review experience

**Time Commitment**: Part-time (1-2 hours per day)

**Note**: In a solo development scenario, one person can fulfill all three roles sequentially.

---

## Implementation Timeline

### Day 1: Care Gap Template

**Morning (4 hours)**:
- ✅ Write care-gap unit tests (RED)
- ✅ Implement care-gap.html template (GREEN)
- ✅ Implement care-gap.txt template (GREEN)
- ✅ Refactor and optimize (REFACTOR)

**Afternoon (4 hours)**:
- ✅ Add sample data to preview API
- ✅ Manual testing and preview
- ✅ Mobile responsiveness testing
- ✅ Code review and merge

**Deliverables**:
- care-gap.html (mobile-responsive)
- care-gap.txt (concise SMS)
- CareGapTemplateTest.java (~15 tests)
- Updated TemplatePreviewController with care-gap sample data

### Day 2: Health Score Template

**Morning (4 hours)**:
- ✅ Write health-score unit tests (RED)
- ✅ Implement health-score.html template (GREEN)
- ✅ Implement health-score.txt template (GREEN)
- ✅ Refactor and optimize (REFACTOR)

**Afternoon (4 hours)**:
- ✅ Add sample data to preview API
- ✅ Manual testing and preview
- ✅ Mobile responsiveness testing
- ✅ Code review and merge

**Deliverables**:
- health-score.html (with score visualization)
- health-score.txt (brief score update)
- HealthScoreTemplateTest.java (~15 tests)
- Updated TemplatePreviewController with health-score sample data

### Day 3: Appointment Reminder Template

**Morning (4 hours)**:
- ✅ Write appointment-reminder unit tests (RED)
- ✅ Implement appointment-reminder.html template (GREEN)
- ✅ Implement appointment-reminder.txt template (GREEN)
- ✅ Refactor and optimize (REFACTOR)

**Afternoon (4 hours)**:
- ✅ Add sample data to preview API
- ✅ Add calendar event (.ics) generation support
- ✅ Manual testing and preview
- ✅ Code review and merge

**Deliverables**:
- appointment-reminder.html (with calendar integration)
- appointment-reminder.txt (brief reminder)
- AppointmentReminderTemplateTest.java (~15 tests)
- Updated TemplatePreviewController with appointment sample data

### Day 4: Medication Reminder Template

**Morning (4 hours)**:
- ✅ Write medication-reminder unit tests (RED)
- ✅ Implement medication-reminder.html template (GREEN)
- ✅ Implement medication-reminder.txt template (GREEN)
- ✅ Refactor and optimize (REFACTOR)

**Afternoon (4 hours)**:
- ✅ Add sample data to preview API
- ✅ Manual testing and preview
- ✅ Mobile responsiveness testing
- ✅ Code review and merge

**Deliverables**:
- medication-reminder.html (with dosage information)
- medication-reminder.txt (brief medication reminder)
- MedicationReminderTemplateTest.java (~15 tests)
- Updated TemplatePreviewController with medication sample data

### Day 5: Lab Result Template

**Morning (4 hours)**:
- ✅ Write lab-result unit tests (RED)
- ✅ Implement lab-result.html template (GREEN)
- ✅ Implement lab-result.txt template (GREEN)
- ✅ Refactor and optimize (REFACTOR)

**Afternoon (4 hours)**:
- ✅ Add sample data to preview API
- ✅ Add result visualization (trend charts)
- ✅ Manual testing and preview
- ✅ Code review and merge

**Deliverables**:
- lab-result.html (with result interpretation)
- lab-result.txt (brief result summary)
- LabResultTemplateTest.java (~18 tests)
- Updated TemplatePreviewController with lab-result sample data

### Day 6: Digest Template

**Morning (4 hours)**:
- ✅ Write digest unit tests (RED)
- ✅ Implement digest.html template (GREEN)
- ✅ Implement digest.txt template (GREEN)
- ✅ Refactor and optimize (REFACTOR)

**Afternoon (4 hours)**:
- ✅ Add sample data to preview API
- ✅ Test with multiple notification types
- ✅ Manual testing and preview
- ✅ Code review and merge

**Deliverables**:
- digest.html (aggregated notifications)
- digest.txt (daily summary)
- DigestTemplateTest.java (~20 tests)
- Updated TemplatePreviewController with digest sample data

### Day 7: Integration & Mobile Testing

**Morning (4 hours)**:
- ✅ Run full test suite (all 100+ tests)
- ✅ Integration testing (all templates together)
- ✅ Mobile device testing (iOS, Android)
- ✅ Email client compatibility testing

**Afternoon (4 hours)**:
- ✅ Performance testing (rendering benchmarks)
- ✅ Load testing (concurrent template rendering)
- ✅ Fix any issues discovered
- ✅ Final mobile optimization

**Deliverables**:
- Mobile compatibility matrix
- Email client compatibility report
- Performance benchmarks
- Integration test results

### Day 8: Documentation & Final QA

**Morning (4 hours)**:
- ✅ Write template usage guide
- ✅ Create variable reference documentation
- ✅ Compile testing report
- ✅ Create mobile testing checklist

**Afternoon (4 hours)**:
- ✅ Final code review
- ✅ Production readiness checklist
- ✅ Deploy to staging environment
- ✅ Smoke tests on staging

**Deliverables**:
- TEMPLATE_USAGE_GUIDE.md
- TEMPLATE_VARIABLE_REFERENCE.md
- TESTING_REPORT.md
- Production deployment plan

---

## Detailed Task Breakdown

### Template 1: Care Gap (care-gap)

#### RED Phase: Write Tests First

**File**: `CareGapTemplateTest.java`

**Test Cases** (15 tests):
1. Should render care-gap HTML template with all required variables
2. Should render care-gap SMS template with minimal content
3. Should display gap type and description
4. Should display measure name and details
5. Should display due date with formatting
6. Should display patient information (name, MRN)
7. Should include recommended actions list
8. Should include action button with URL
9. Should handle missing optional fields gracefully
10. Should display gap priority/severity
11. Should be mobile-responsive (viewport meta tag)
12. Should include HIPAA disclaimer
13. Should escape HTML in user content (XSS prevention)
14. Should render in reasonable time (<100ms)
15. Should handle multiple recommended actions

**Test Example**:
```java
@Test
@DisplayName("Should render care-gap HTML template with all required variables")
void shouldRenderCareGapHtmlTemplate() {
    // Given
    Map<String, Object> variables = new HashMap<>();
    variables.put("channel", "EMAIL");
    variables.put("patientName", "John Smith");
    variables.put("mrn", "MRN-123456");
    variables.put("gapType", "Preventive Care Gap");
    variables.put("gapMessage", "Patient is due for annual diabetic eye exam");
    variables.put("measure", "CDC-H: Comprehensive Diabetes Care - Eye Exam");
    variables.put("dueDate", "2025-12-01");
    variables.put("actionUrl", "https://example.com/care-gaps/456");

    // When
    String result = renderer.render("care-gap", variables);

    // Then
    assertNotNull(result);
    assertTrue(result.contains("Care Gap"));
    assertTrue(result.contains("John Smith"));
    assertTrue(result.contains("Preventive Care Gap"));
    assertTrue(result.contains("diabetic eye exam"));
    assertTrue(result.contains("CDC-H"));
}
```

#### GREEN Phase: Implement Template

**File**: `care-gap.html`

**Required Variables**:
- `patientName` (String) - Patient full name
- `mrn` (String, optional) - Medical Record Number
- `gapType` (String) - Type of care gap (e.g., "Preventive Care Gap")
- `gapMessage` (String) - Detailed description of the gap
- `measure` (String) - Quality measure name (e.g., "CDC-H")
- `dueDate` (String) - When the gap should be addressed
- `actionUrl` (String) - Link to take action
- `recommendedActions` (List<String>, optional) - Steps to close gap
- `priority` (String, optional) - GAP priority: HIGH, MEDIUM, LOW
- `facilityName` (String, optional) - Healthcare facility name

**Template Structure**:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Care Gap Notification</title>
    <style>
        /* Mobile-responsive CSS */
        /* Color scheme: Blue theme for informational */
        /* Priority badges: HIGH=orange, MEDIUM=yellow, LOW=blue */
    </style>
</head>
<body>
    <!-- Header: Care Gap icon and title -->
    <!-- Patient info panel -->
    <!-- Gap type badge (with priority color) -->
    <!-- Gap description -->
    <!-- Measure details -->
    <!-- Due date (with urgency indicator if overdue) -->
    <!-- Recommended actions list -->
    <!-- Action button -->
    <!-- Footer with HIPAA disclaimer -->
</body>
</html>
```

**File**: `care-gap.txt`

```
📋 CARE GAP

Patient: [(${patientName})]
MRN: [(${mrn})]

Gap: [(${gapType})]
[(${gapMessage})]

Measure: [(${measure})]
Due: [(${dueDate})]

Action: [(${actionUrl})]

- HealthData-in-Motion
```

#### REFACTOR Phase: Optimize

**Enhancements**:
- Add urgency indicator (days until due date)
- Color-code priority levels
- Add measure description tooltip
- Optimize CSS for faster rendering
- Add accessibility (ARIA labels)
- Ensure consistent spacing

#### VERIFY Phase: QA

**Manual Testing**:
- Preview in browser
- Test on mobile (iPhone, Android)
- Test in email clients (Gmail, Outlook, Apple Mail)
- Verify all links work
- Check HIPAA compliance

**Performance Testing**:
- Measure rendering time (should be <100ms)
- Test with long text (truncation handling)
- Test with missing optional fields

---

### Template 2: Health Score (health-score)

#### RED Phase: Write Tests First

**File**: `HealthScoreTemplateTest.java`

**Test Cases** (15 tests):
1. Should render health-score HTML template with current and previous scores
2. Should render health-score SMS template
3. Should display score change indicator (+/- with color)
4. Should display current score prominently
5. Should display previous score for comparison
6. Should include score interpretation (Excellent, Good, Fair, Poor)
7. Should display contributing factors list
8. Should display improvement recommendations
9. Should handle score increase (green indicator)
10. Should handle score decrease (red indicator)
11. Should handle no change (gray indicator)
12. Should be mobile-responsive
13. Should include HIPAA disclaimer
14. Should escape HTML in user content
15. Should render in reasonable time (<100ms)

**Test Example**:
```java
@Test
@DisplayName("Should display score change with correct color indicator")
void shouldDisplayScoreChangeWithCorrectColor() {
    // Test increase (green)
    Map<String, Object> variables = new HashMap<>();
    variables.put("channel", "EMAIL");
    variables.put("patientName", "John Smith");
    variables.put("currentScore", 75);
    variables.put("previousScore", 70);
    variables.put("scoreChange", "+5");
    variables.put("actionUrl", "https://example.com");

    String result = renderer.render("health-score", variables);
    assertTrue(result.contains("score-increase") || result.contains("green"));

    // Test decrease (red)
    variables.put("currentScore", 65);
    variables.put("previousScore", 70);
    variables.put("scoreChange", "-5");

    result = renderer.render("health-score", variables);
    assertTrue(result.contains("score-decrease") || result.contains("red"));
}
```

#### GREEN Phase: Implement Template

**File**: `health-score.html`

**Required Variables**:
- `patientName` (String)
- `mrn` (String, optional)
- `currentScore` (Integer) - 0-100
- `previousScore` (Integer) - 0-100
- `scoreChange` (String) - e.g., "+5", "-3", "0"
- `scoreMessage` (String) - Interpretation message
- `contributingFactors` (List<String>, optional) - What affected score
- `improvementRecommendations` (List<String>, optional) - How to improve
- `actionUrl` (String)
- `scoreDate` (String, optional) - When score was calculated

**Design Features**:
- Large score display (visual prominence)
- Score change indicator (arrow up/down with color)
- Score interpretation label
- Bar chart or gauge visualization (CSS-only)
- Color coding: 80-100=green, 60-79=yellow, 40-59=orange, <40=red

**File**: `health-score.txt`

```
📊 HEALTH SCORE UPDATE

Patient: [(${patientName})]

Current Score: [(${currentScore})]/100
Previous: [(${previousScore})]
Change: [(${scoreChange})]

[(${scoreMessage})]

View details: [(${actionUrl})]

- HealthData-in-Motion
```

---

### Template 3: Appointment Reminder (appointment-reminder)

#### RED Phase: Write Tests First

**File**: `AppointmentReminderTemplateTest.java`

**Test Cases** (15 tests):
1. Should render appointment-reminder HTML template
2. Should render appointment-reminder SMS template
3. Should display appointment date and time prominently
4. Should display provider name and credentials
5. Should display location/address
6. Should display appointment type
7. Should include preparation instructions
8. Should include confirm/cancel action buttons
9. Should include add-to-calendar link
10. Should handle virtual appointments (Zoom link)
11. Should display parking/directions if provided
12. Should be mobile-responsive
13. Should include HIPAA disclaimer
14. Should escape HTML in user content
15. Should render in reasonable time (<100ms)

#### GREEN Phase: Implement Template

**File**: `appointment-reminder.html`

**Required Variables**:
- `patientName` (String)
- `appointmentDate` (String) - "December 15, 2025"
- `appointmentTime` (String) - "10:30 AM"
- `providerName` (String) - "Dr. Sarah Johnson"
- `location` (String) - "Main Clinic - Building A, Room 205"
- `appointmentType` (String) - "Follow-up Visit", "Annual Physical"
- `confirmUrl` (String) - Link to confirm
- `cancelUrl` (String) - Link to cancel/reschedule
- `preparationInstructions` (List<String>, optional)
- `parkingDirections` (String, optional)
- `isVirtual` (Boolean, optional)
- `virtualMeetingUrl` (String, optional) - For telehealth

**Design Features**:
- Calendar icon with date/time
- Provider photo placeholder
- Map/location visualization
- Confirm/Cancel buttons side-by-side
- Add to calendar button (.ics file)
- Preparation checklist

**File**: `appointment-reminder.txt`

```
📅 APPOINTMENT REMINDER

Patient: [(${patientName})]

Date: [(${appointmentDate})] at [(${appointmentTime})]
Provider: [(${providerName})]
Location: [(${location})]

Confirm: [(${confirmUrl})]
Cancel: [(${cancelUrl})]

- HealthData-in-Motion
```

---

### Template 4: Medication Reminder (medication-reminder)

#### RED Phase: Write Tests First

**File**: `MedicationReminderTemplateTest.java`

**Test Cases** (15 tests):
1. Should render medication-reminder HTML template
2. Should render medication-reminder SMS template
3. Should display medication name and dosage
4. Should display dosing instructions
5. Should display refill date
6. Should display pharmacy information
7. Should display prescriber name
8. Should include refill request button
9. Should display remaining refills count
10. Should display medication warnings/precautions
11. Should handle multiple medications (list)
12. Should be mobile-responsive
13. Should include HIPAA disclaimer
14. Should escape HTML in user content
15. Should render in reasonable time (<100ms)

#### GREEN Phase: Implement Template

**File**: `medication-reminder.html`

**Required Variables**:
- `patientName` (String)
- `medicationName` (String) - "Metformin 500mg"
- `dosage` (String) - "Take 1 tablet twice daily with meals"
- `refillDate` (String) - "December 20, 2025"
- `remainingRefills` (Integer) - Number of refills left
- `prescriber` (String) - "Dr. Sarah Johnson"
- `pharmacy` (String, optional) - "CVS Pharmacy - Main St"
- `refillUrl` (String) - Link to request refill
- `warnings` (List<String>, optional) - Important warnings
- `sideEffects` (List<String>, optional) - Common side effects

**Design Features**:
- Pill icon
- Dosage instructions (prominent)
- Refill countdown ("3 days until refill needed")
- Request refill button
- Warnings in highlighted box (if present)
- Pharmacy contact information

**File**: `medication-reminder.txt`

```
💊 MEDICATION REMINDER

Patient: [(${patientName})]

Medication: [(${medicationName})]
Dosage: [(${dosage})]

Refill needed: [(${refillDate})]
Refills remaining: [(${remainingRefills})]

Request refill: [(${refillUrl})]

- HealthData-in-Motion
```

---

### Template 5: Lab Result (lab-result)

#### RED Phase: Write Tests First

**File**: `LabResultTemplateTest.java`

**Test Cases** (18 tests):
1. Should render lab-result HTML template
2. Should render lab-result SMS template
3. Should display test name prominently
4. Should display result value
5. Should display normal range
6. Should display abnormal flag (HIGH/LOW) with color
7. Should display test date
8. Should display ordering provider
9. Should include result interpretation
10. Should include follow-up recommendations
11. Should display previous results (trend)
12. Should highlight abnormal results in red
13. Should show normal results in green
14. Should handle multiple lab results (panel)
15. Should be mobile-responsive
16. Should include HIPAA disclaimer
17. Should escape HTML in user content
18. Should render in reasonable time (<100ms)

#### GREEN Phase: Implement Template

**File**: `lab-result.html`

**Required Variables**:
- `patientName` (String)
- `mrn` (String, optional)
- `testName` (String) - "Hemoglobin A1C"
- `resultValue` (String) - "7.2%"
- `normalRange` (String) - "<7.0% for diabetic patients"
- `abnormalFlag` (String, optional) - "HIGH", "LOW", "CRITICAL"
- `testDate` (String) - "November 25, 2025"
- `orderingProvider` (String) - "Dr. Sarah Johnson"
- `interpretation` (String, optional) - Clinical significance
- `followUpRecommendations` (List<String>, optional)
- `previousResults` (List<Map<String,String>>, optional) - Historical data
- `actionUrl` (String)

**Design Features**:
- Test result card with color coding
- Abnormal flag badge (red for HIGH, blue for LOW)
- Normal range comparison bar
- Trend chart (if previous results available)
- Interpretation section
- Follow-up recommendations

**File**: `lab-result.txt`

```
🔬 LAB RESULT

Patient: [(${patientName})]

Test: [(${testName})]
Result: [(${resultValue})]
Normal Range: [(${normalRange})]
[# th:if="${abnormalFlag}"]Flag: [(${abnormalFlag})][/]

Test Date: [(${testDate})]
Provider: [(${orderingProvider})]

View details: [(${actionUrl})]

- HealthData-in-Motion
```

---

### Template 6: Digest (digest)

#### RED Phase: Write Tests First

**File**: `DigestTemplateTest.java`

**Test Cases** (20 tests):
1. Should render digest HTML template
2. Should render digest SMS template
3. Should display digest date prominently
4. Should group notifications by type
5. Should display critical alerts section
6. Should display care gaps section
7. Should display appointments section
8. Should display lab results section
9. Should display count for each section
10. Should handle empty sections gracefully
11. Should display priority indicators
12. Should include quick action links
13. Should display unread notification count
14. Should handle single notification type
15. Should handle all notification types
16. Should be mobile-responsive
17. Should include HIPAA disclaimer
18. Should escape HTML in user content
19. Should render in reasonable time (<150ms) (complex template)
20. Should handle large notification counts (50+)

#### GREEN Phase: Implement Template

**File**: `digest.html`

**Required Variables**:
- `patientName` (String, optional) - If single patient
- `digestDate` (String) - "December 1, 2025"
- `patientCount` (Integer, optional) - For provider digest
- `criticalAlerts` (List<Map<String,Object>>, optional)
- `criticalAlertCount` (Integer)
- `careGaps` (List<Map<String,Object>>, optional)
- `careGapCount` (Integer)
- `appointments` (List<Map<String,Object>>, optional)
- `appointmentCount` (Integer)
- `labResults` (List<Map<String,Object>>, optional)
- `labResultCount` (Integer)
- `unreadCount` (Integer) - Total unread notifications
- `actionUrl` (String) - Link to full dashboard

**Design Features**:
- Header with date and total notification count
- Sections for each notification type
- Collapsible sections (CSS-only accordion)
- Count badges for each section
- Priority indicators (red dots for critical)
- Quick action buttons for each item
- Summary footer with totals

**File**: `digest.txt`

```
📬 DAILY DIGEST - [(${digestDate})]

[# th:if="${patientName}"]Patient: [(${patientName})][/]

Summary:
- Critical Alerts: [(${criticalAlertCount})]
- Care Gaps: [(${careGapCount})]
- Appointments: [(${appointmentCount})]
- Lab Results: [(${labResultCount})]

Total: [(${unreadCount})] notifications

View dashboard: [(${actionUrl})]

- HealthData-in-Motion
```

---

## Test Scenarios

### Unit Test Categories (Per Template)

#### 1. Basic Rendering Tests
- Template renders without errors
- All required variables are substituted
- HTML structure is valid

#### 2. Variable Handling Tests
- Required variables present and displayed
- Optional variables work when present
- Optional variables don't break when absent
- Default values applied correctly

#### 3. Formatting Tests
- Dates formatted correctly
- Numbers formatted with proper precision
- Currency formatted with $ symbol
- Phone numbers formatted

#### 4. Conditional Logic Tests
- Sections show/hide based on variables
- Color coding based on values (severity, scores)
- Icons/badges based on status

#### 5. List/Collection Tests
- Lists render correctly (iteration)
- Empty lists don't break template
- Single item lists render
- Multiple item lists render
- Large lists (10+ items) render

#### 6. Mobile Responsiveness Tests
- Viewport meta tag present
- Responsive CSS classes applied
- Font sizes appropriate for mobile
- Touch-friendly button sizes (44x44px minimum)

#### 7. Security Tests
- HTML escaping (XSS prevention)
- No script execution in templates
- Safe URL handling

#### 8. Compliance Tests
- HIPAA disclaimer present
- PHI handling notice present
- Appropriate footer content

#### 9. Performance Tests
- Rendering time <100ms (standard templates)
- Rendering time <150ms (complex digest template)
- Memory usage reasonable
- Concurrent rendering capability

#### 10. Edge Case Tests
- Very long text (truncation/wrapping)
- Special characters in content
- Null values handled gracefully
- Empty strings handled gracefully

### Integration Test Scenarios

#### Scenario 1: All Templates Render Successfully
```java
@Test
@DisplayName("Should render all 7 templates without errors")
void shouldRenderAllTemplates() {
    List<String> templates = List.of(
        "critical-alert", "care-gap", "health-score",
        "appointment-reminder", "medication-reminder",
        "lab-result", "digest"
    );

    for (String templateId : templates) {
        Map<String, Object> variables = getSampleData(templateId);
        String result = renderer.render(templateId, variables);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }
}
```

#### Scenario 2: Preview API Returns All Templates
```java
@Test
@DisplayName("Preview API should list all available templates")
void previewApiShouldListAllTemplates() {
    ResponseEntity<Map<String, Object>> response =
        previewController.listTemplates();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    Map<String, Object> body = response.getBody();

    List<String> templates = (List<String>) body.get("templates");
    assertEquals(7, templates.size());
    assertTrue(templates.contains("critical-alert"));
    assertTrue(templates.contains("care-gap"));
    assertTrue(templates.contains("health-score"));
    // ... etc
}
```

#### Scenario 3: Concurrent Template Rendering
```java
@Test
@DisplayName("Should handle concurrent rendering of multiple templates")
void shouldHandleConcurrentRendering() throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(10);
    List<Future<String>> futures = new ArrayList<>();

    // Render 100 templates concurrently
    for (int i = 0; i < 100; i++) {
        futures.add(executor.submit(() ->
            renderer.render("critical-alert", getSampleData("critical-alert"))
        ));
    }

    // All should complete successfully
    for (Future<String> future : futures) {
        String result = future.get();
        assertNotNull(result);
    }

    executor.shutdown();
}
```

#### Scenario 4: Template Caching Performance
```java
@Test
@DisplayName("Cached templates should render faster than cold renders")
void cachedTemplatesShouldRenderFaster() {
    Map<String, Object> variables = getSampleData("critical-alert");

    // Cold render (first time)
    long startCold = System.currentTimeMillis();
    renderer.render("critical-alert", variables);
    long coldDuration = System.currentTimeMillis() - startCold;

    // Warm render (cached)
    long startWarm = System.currentTimeMillis();
    renderer.render("critical-alert", variables);
    long warmDuration = System.currentTimeMillis() - startWarm;

    assertTrue(warmDuration < coldDuration,
        "Cached render should be faster (cold: " + coldDuration +
        "ms, warm: " + warmDuration + "ms)");
}
```

---

## Success Criteria

### Quantitative Metrics

1. **Test Coverage**: ≥95% code coverage
   - All template files covered
   - All rendering paths tested
   - Edge cases included

2. **Test Count**: ≥100 unit tests total
   - Critical-alert: 18 tests ✅ (already implemented)
   - Care-gap: 15 tests
   - Health-score: 15 tests
   - Appointment-reminder: 15 tests
   - Medication-reminder: 15 tests
   - Lab-result: 18 tests
   - Digest: 20 tests
   - Integration tests: 10+ tests

3. **Performance Benchmarks**:
   - Standard templates: <100ms rendering time
   - Complex templates (digest): <150ms rendering time
   - Concurrent rendering: 100 templates in <5 seconds
   - Memory usage: <50MB for 1000 rendered templates

4. **Mobile Compatibility**: 100% pass rate
   - iOS Safari: ✅
   - Android Chrome: ✅
   - Mobile viewport: 320px - 414px

5. **Email Client Compatibility**: ≥95% pass rate
   - Gmail (Web, iOS, Android): ✅
   - Outlook (Desktop, Web): ✅
   - Apple Mail (macOS, iOS): ✅
   - Yahoo Mail: ✅
   - Thunderbird: ✅

### Qualitative Criteria

1. **Code Quality**:
   - All templates follow consistent patterns
   - CSS is optimized and DRY
   - No code duplication
   - Clear variable naming
   - Comprehensive comments

2. **User Experience**:
   - Professional appearance
   - Consistent branding across all templates
   - Clear call-to-action buttons
   - Readable fonts (minimum 14px body text)
   - Good color contrast (WCAG AA compliance)

3. **Security**:
   - All user input is escaped (XSS prevention)
   - No inline JavaScript
   - Safe URL handling
   - HIPAA compliance messaging

4. **Documentation**:
   - Every template has usage guide
   - All variables documented
   - Sample data provided
   - Testing procedures documented

5. **Maintainability**:
   - Easy to add new templates
   - Clear patterns to follow
   - Good test coverage for confidence in changes
   - Version control best practices

---

## Risk Mitigation

### Risk 1: Timeline Slippage

**Probability**: Medium
**Impact**: Medium

**Mitigation Strategies**:
1. **Build buffer time** into schedule (Day 8 is buffer/documentation)
2. **Prioritize templates** by business value (critical-alert > digest)
3. **Parallel development** where possible (2 developers = faster)
4. **Daily standups** to identify blockers early
5. **MVP mindset**: Ship working templates, enhance later

**Contingency Plan**:
- If behind schedule on Day 5, reduce digest template complexity
- Skip optional features (trend charts, visualizations)
- Focus on core functionality first
- Documentation can be completed post-implementation

### Risk 2: Email Client Compatibility Issues

**Probability**: High
**Impact**: Medium

**Mitigation Strategies**:
1. **Use proven patterns** from critical-alert template (already tested)
2. **Table-based layouts** for maximum compatibility
3. **Inline CSS** (no external stylesheets)
4. **Test early** (Day 2-3, not Day 7)
5. **Use compatibility tools** (Email on Acid, Litmus)

**Contingency Plan**:
- Maintain fallback styles for problematic clients
- Document known issues and workarounds
- Provide plain-text alternative (already have SMS templates)
- Focus on top 3 clients: Gmail, Outlook, Apple Mail

### Risk 3: Mobile Responsiveness Issues

**Probability**: Medium
**Impact**: High (60% of emails opened on mobile)

**Mitigation Strategies**:
1. **Mobile-first design** approach
2. **Test on real devices** (not just emulators)
3. **Responsive breakpoints** at 600px, 480px, 320px
4. **Touch-friendly buttons** (minimum 44x44px)
5. **Readable fonts** (minimum 14px on mobile)

**Contingency Plan**:
- Simplify layout for mobile (single column)
- Increase font sizes if needed
- Use larger buttons and spacing
- Test on borrowed devices if needed

### Risk 4: Performance Degradation

**Probability**: Low
**Impact**: High

**Mitigation Strategies**:
1. **Performance testing** from Day 1
2. **Template caching** enabled
3. **Optimize CSS** (remove unused styles)
4. **Benchmark against targets** (<100ms)
5. **Profile rendering** to find bottlenecks

**Contingency Plan**:
- Reduce template complexity if slow
- Remove non-essential features (animations, complex charts)
- Increase cache TTL
- Pre-compile templates

### Risk 5: Security Vulnerabilities (XSS)

**Probability**: Low
**Impact**: Critical

**Mitigation Strategies**:
1. **Thymeleaf auto-escaping** (enabled by default)
2. **Security testing** in every template test
3. **Code review** by security-aware reviewer
4. **No dynamic template compilation**
5. **Test with malicious input**

**Contingency Plan**:
- Additional security review before production
- Penetration testing if needed
- Security audit by external expert
- Emergency rollback plan

### Risk 6: HIPAA Compliance Issues

**Probability**: Low
**Impact**: Critical

**Mitigation Strategies**:
1. **HIPAA disclaimer** in every template footer
2. **PHI handling warnings** prominently displayed
3. **Compliance review** by healthcare compliance expert
4. **Audit trail** for template usage
5. **Encryption** in transit (HTTPS only)

**Contingency Plan**:
- Legal review before production deployment
- Additional disclaimers if needed
- Opt-out mechanism for sensitive notifications
- Compliance certification if required

---

## Daily Standup Format

### Daily Check-in Questions

1. **What did you complete yesterday?**
   - Templates implemented
   - Tests written
   - Issues resolved

2. **What are you working on today?**
   - Current template
   - Test scenarios
   - Blockers to address

3. **Any blockers or concerns?**
   - Technical challenges
   - Resource needs
   - Timeline concerns

### Sample Day 3 Standup

**Developer**:
- ✅ Yesterday: Completed health-score template, 15 tests passing
- 🔨 Today: Appointment-reminder template, add calendar integration
- ⚠️ Blockers: Need clarification on .ics file generation

**QA Engineer**:
- ✅ Yesterday: Tested care-gap and health-score on mobile devices
- 🔨 Today: Email client testing for new templates
- ⚠️ Blockers: None

**Reviewer**:
- ✅ Yesterday: Reviewed and approved care-gap template
- 🔨 Today: Review health-score template, security audit
- ⚠️ Blockers: None

---

## Deployment Checklist

### Pre-Deployment (Day 8)

- [ ] All 100+ unit tests passing
- [ ] Integration tests passing
- [ ] Mobile testing complete (iOS, Android)
- [ ] Email client testing complete (Gmail, Outlook, Apple Mail)
- [ ] Performance benchmarks met (<100ms rendering)
- [ ] Security review complete (no XSS vulnerabilities)
- [ ] HIPAA compliance verified
- [ ] Code review approved
- [ ] Documentation complete
- [ ] Sample data verified in preview API

### Deployment to Staging

- [ ] Build Docker image with new templates
- [ ] Deploy to staging environment
- [ ] Smoke test: Preview all 7 templates
- [ ] Verify template rendering performance
- [ ] Test preview API endpoints
- [ ] Verify mobile responsiveness
- [ ] Check logs for errors

### Deployment to Production

- [ ] Staging tests passed
- [ ] Stakeholder approval received
- [ ] Deployment window scheduled
- [ ] Rollback plan documented
- [ ] Build production Docker image
- [ ] Deploy to production
- [ ] Smoke test: Preview all 7 templates
- [ ] Monitor logs for errors (first 24 hours)
- [ ] Verify performance metrics
- [ ] User acceptance testing

### Post-Deployment

- [ ] Monitor email delivery rates
- [ ] Monitor template rendering performance
- [ ] Collect user feedback
- [ ] Track email open rates (by template)
- [ ] Track click-through rates (by template)
- [ ] Identify improvement opportunities
- [ ] Plan enhancements for next iteration

---

## Appendix A: Template Variable Reference

### Common Variables (All Templates)

| Variable | Type | Required | Description |
|----------|------|----------|-------------|
| `channel` | String | Yes | EMAIL or SMS |
| `patientName` | String | Yes | Patient full name |
| `mrn` | String | No | Medical Record Number |
| `dob` | String | No | Date of birth |
| `age` | String | No | Patient age |
| `facilityName` | String | No | Healthcare facility |
| `timestamp` | String | No | Notification timestamp |
| `actionUrl` | String | Yes | Link to take action |

### Template-Specific Variables

See detailed task breakdown above for each template's specific variables.

---

## Appendix B: CSS Best Practices for Email Templates

### 1. Use Inline Styles
```html
<!-- ❌ Bad: External stylesheet -->
<link rel="stylesheet" href="styles.css">

<!-- ✅ Good: Inline styles -->
<td style="padding: 20px; background-color: #f5f5f5;">
```

### 2. Use Tables for Layout
```html
<!-- ✅ Good: Table-based layout for compatibility -->
<table width="100%" cellpadding="0" cellspacing="0">
    <tr>
        <td>Content here</td>
    </tr>
</table>
```

### 3. Set Explicit Widths
```html
<!-- ✅ Good: Explicit width for email clients -->
<table width="600" style="max-width: 600px;">
```

### 4. Use Web-Safe Fonts
```html
<!-- ✅ Good: Fallback fonts -->
<td style="font-family: Arial, Helvetica, sans-serif;">
```

### 5. Avoid Floats and Positioning
```html
<!-- ❌ Bad: Floats don't work in many email clients -->
<div style="float: left;">

<!-- ✅ Good: Use tables -->
<table><tr><td>Column 1</td><td>Column 2</td></tr></table>
```

### 6. Use Absolute URLs
```html
<!-- ❌ Bad: Relative URL -->
<img src="/images/logo.png">

<!-- ✅ Good: Absolute URL -->
<img src="https://example.com/images/logo.png">
```

---

## Appendix C: Mobile Testing Checklist

### Device Testing Matrix

| Device | Screen Size | OS | Browser | Status |
|--------|-------------|----|---------| -------|
| iPhone 14 Pro | 393 x 852 | iOS 17 | Safari | ⬜ |
| iPhone SE | 375 x 667 | iOS 17 | Safari | ⬜ |
| Samsung Galaxy S23 | 360 x 800 | Android 13 | Chrome | ⬜ |
| Google Pixel 7 | 412 x 915 | Android 13 | Chrome | ⬜ |
| iPad Air | 820 x 1180 | iOS 17 | Safari | ⬜ |

### Mobile Checks (Per Template)

- [ ] Text is readable (minimum 14px)
- [ ] Buttons are tappable (minimum 44x44px)
- [ ] Layout doesn't break on small screens
- [ ] Images scale properly
- [ ] Links are clickable
- [ ] No horizontal scrolling required
- [ ] Colors are visible (not too light/dark)
- [ ] Viewport meta tag present
- [ ] Loading time acceptable (<3 seconds)

---

## Appendix D: TDD Command Reference

### Run All Tests
```bash
./gradlew :modules:services:quality-measure-service:test
```

### Run Specific Test Class
```bash
./gradlew :modules:services:quality-measure-service:test --tests CareGapTemplateTest
```

### Run Specific Test Method
```bash
./gradlew :modules:services:quality-measure-service:test --tests CareGapTemplateTest.shouldRenderCareGapHtmlTemplate
```

### Run Tests with Coverage
```bash
./gradlew :modules:services:quality-measure-service:jacocoTestReport
```

### Continuous Testing (Watch Mode)
```bash
./gradlew :modules:services:quality-measure-service:test --continuous
```

### Fast Feedback Loop
```bash
# Compile, test, report (repeat)
./gradlew :modules:services:quality-measure-service:compileJava test --no-daemon
```

---

## Summary

This TDD Swarm plan provides a **comprehensive, test-driven roadmap** for implementing the remaining 6 notification templates over 8 days.

**Key Success Factors**:
1. ✅ Test-First approach (RED → GREEN → REFACTOR)
2. ✅ Clear task breakdown with time estimates
3. ✅ Comprehensive test scenarios (100+ tests)
4. ✅ Risk mitigation strategies
5. ✅ Mobile and email client compatibility focus
6. ✅ Security and HIPAA compliance built-in
7. ✅ Daily progress tracking and standup format
8. ✅ Production deployment checklist

**Expected Outcomes**:
- 7 complete template sets (14 files: 7 HTML + 7 SMS)
- 100+ passing unit tests
- Mobile-responsive, email-compatible templates
- HIPAA-compliant messaging
- Production-ready preview API
- Complete documentation

**Ready to Execute**: This plan can be followed immediately by a development team (or solo developer) to complete the notification template system implementation.

---

**Status**: ✅ **PLAN COMPLETE - READY FOR EXECUTION**
**Prepared By**: Claude Code (Software Architect)
**Date**: November 27, 2025
**Version**: 1.0.0

**Next Action**: Begin Day 1 - Care Gap Template Implementation
