#!/bin/bash

# HDIM GitHub Issue Creation - Batch 2
# Creates 15 high-impact issues from INCOMPLETE_FEATURES_CATALOG.md
# 7 Backend + 3 Frontend + 5 Strategic Integration issues

set -e

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  HDIM Batch 2 Issue Creation (15 issues)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Check gh CLI
if ! command -v gh &> /dev/null; then
    echo "❌ GitHub CLI not found. Install: https://cli.github.com/"
    exit 1
fi

# Verify milestones exist
echo "🔍 Verifying milestones..."
gh milestone list | grep -q "Q1-2026-Backend-Endpoints" || {
    echo "❌ Milestone 'Q1-2026-Backend-Endpoints' not found"
    exit 1
}

echo "✅ Milestones verified"
echo ""

# Counter for created issues
CREATED=0

# ============================================================
# BATCH 1: Backend Endpoints (7 issues)
# ============================================================

echo "📦 Creating Backend Endpoint Issues (7 issues)..."
echo ""

# Issue 1: TODO-006 - OUT_OF_SERVICE room status
gh issue create \
  --title "[Backend] Implement OUT_OF_SERVICE room status workflow" \
  --label "feature,backend,P1-High" \
  --milestone "Q1-2026-Backend-Endpoints" \
  --body "## Feature Description

**User Story**: As a facility manager, I want to mark rooms as OUT_OF_SERVICE so they are excluded from patient assignment during maintenance.

**Business Value**: Proper room availability tracking prevents assigning patients to unavailable rooms.

**Acceptance Criteria**:
- [ ] Add OUT_OF_SERVICE status to RoomStatus enum
- [ ] Implement status change workflow with audit trail
- [ ] Exclude OUT_OF_SERVICE rooms from assignment queries
- [ ] Add restoration workflow when maintenance complete

## Technical Specification
**Location**: \`clinical-workflow-service/RoomManagementService.java:408\`

## Estimation
**Story Points**: 2 | **Effort**: 2 days | **Complexity**: Medium

**Business Impact**: Prevents operational errors in patient room assignments"

CREATED=$((CREATED + 1))
echo "✅ Created issue #$CREATED: OUT_OF_SERVICE room status"

# Issue 2: TODO-007 - Demo data seeding
gh issue create \
  --title "[Backend] Implement demo data seeding for demo environments" \
  --label "feature,backend,P2-Medium" \
  --milestone "Q1-2026-Backend-Endpoints" \
  --body "## Feature Description

**User Story**: As a sales engineer, I want automated demo data seeding so I can quickly set up realistic demo environments.

**Business Value**: Reduces demo setup time from hours to minutes, enables consistent sales demonstrations.

**Acceptance Criteria**:
- [ ] Seed realistic patient data (50-100 patients)
- [ ] Generate care gaps for demo scenarios
- [ ] Create evaluation results with realistic metrics
- [ ] Support multiple demo scenarios (diabetes, hypertension, preventive care)

## Technical Specification
**Location**: \`demo-orchestrator-service/DataManagerService.java:18\`

## Estimation
**Story Points**: 3 | **Effort**: 2 days | **Complexity**: Medium

**Business Impact**: Accelerates sales cycle with ready-to-use demo environments"

CREATED=$((CREATED + 1))
echo "✅ Created issue #$CREATED: Demo data seeding"

# Issue 3: TODO-008 - Demo data clearing
gh issue create \
  --title "[Backend] Implement demo data clearing for environment resets" \
  --label "feature,backend,P2-Medium" \
  --milestone "Q1-2026-Backend-Endpoints" \
  --body "## Feature Description

**User Story**: As a sales engineer, I want to reset demo environments so I can start clean demonstrations.

**Business Value**: Enables reusing demo environments without manual cleanup.

**Acceptance Criteria**:
- [ ] Clear all demo tenant data safely (preserve configuration)
- [ ] Cascade delete related entities (care gaps, evaluations, results)
- [ ] Maintain audit trail of data clearing events
- [ ] Add confirmation dialog to prevent accidental deletion

## Technical Specification
**Location**: \`demo-orchestrator-service/DataManagerService.java:25\`

## Estimation
**Story Points**: 2 | **Effort**: 1 day | **Complexity**: Low

**Business Impact**: Simplifies demo environment management"

CREATED=$((CREATED + 1))
echo "✅ Created issue #$CREATED: Demo data clearing"

# Issue 4: TODO-009 - WebSocket DevOps logs
gh issue create \
  --title "[Backend] Implement WebSocket publishing for DevOps agent logs" \
  --label "feature,backend,P2-Medium" \
  --milestone "Q1-2026-Backend-Endpoints" \
  --body "## Feature Description

**User Story**: As a DevOps engineer, I want real-time deployment logs so I can monitor deployments without polling.

**Business Value**: Real-time visibility into deployment progress and failures.

**Acceptance Criteria**:
- [ ] Implement WebSocket endpoint for log streaming
- [ ] Publish deployment agent logs in real-time
- [ ] Add log level filtering (INFO, WARN, ERROR)
- [ ] Implement reconnection logic for dropped connections

## Technical Specification
**Location**: \`demo-orchestrator-service/DevOpsAgentClient.java:35, 40\`

## Estimation
**Story Points**: 3 | **Effort**: 1 day | **Complexity**: Medium

**Business Impact**: Faster identification of deployment issues"

CREATED=$((CREATED + 1))
echo "✅ Created issue #$CREATED: WebSocket DevOps logs"

# Issue 5: TODO-010 - FHIR identifier serialization
gh issue create \
  --title "[Backend] Implement proper FHIR identifier serialization for merged patients" \
  --label "feature,backend,fhir,P1-High" \
  --milestone "Q1-2026-Backend-Endpoints" \
  --body "## Feature Description

**User Story**: As a FHIR integrator, I want proper identifier serialization for merged patients so FHIR consumers receive valid identifiers.

**Business Value**: FHIR R4 compliance for patient merge events.

**Acceptance Criteria**:
- [ ] Serialize FHIR Identifier objects correctly (not toString())
- [ ] Preserve identifier system, value, and use fields
- [ ] Add unit tests for merged patient FHIR serialization
- [ ] Verify against FHIR R4 specification

## Technical Specification
**Location**: \`patient-event-handler-service/PatientMergedEventHandler.java:139\`

## Estimation
**Story Points**: 1 | **Effort**: 1 day | **Complexity**: Low

**Business Impact**: Prevents FHIR validation failures for merged patients"

CREATED=$((CREATED + 1))
echo "✅ Created issue #$CREATED: FHIR identifier serialization"

# Issue 6: TODO-012 - Patient name resolution
gh issue create \
  --title "[Backend] Implement patient name resolution in vital signs alerts" \
  --label "feature,backend,P1-High" \
  --milestone "Q1-2026-Backend-Endpoints" \
  --body "## Feature Description

**User Story**: As a nurse, I want to see patient names in vital sign alerts so I know which patient needs attention.

**Business Value**: Faster response to critical alerts by showing human-readable names.

**Acceptance Criteria**:
- [ ] Call Patient Service to resolve patient ID to name
- [ ] Handle patient not found gracefully (fallback to ID)
- [ ] Cache patient names (5-minute TTL per HIPAA requirements)
- [ ] Add circuit breaker for Patient Service calls

## Technical Specification
**Location**: \`clinical-workflow-service/VitalSignsService.java:709, 750\`

## Estimation
**Story Points**: 2 | **Effort**: 2 days | **Complexity**: Medium

**Business Impact**: Improves alert usability and response time"

CREATED=$((CREATED + 1))
echo "✅ Created issue #$CREATED: Patient name resolution"

# Issue 7: TODO-013 - Room number resolution
gh issue create \
  --title "[Backend] Implement room number resolution for vital sign alerts" \
  --label "feature,backend,P1-High" \
  --milestone "Q1-2026-Backend-Endpoints" \
  --body "## Feature Description

**User Story**: As a nurse, I want to see room numbers in vital sign alerts so I know where to respond.

**Business Value**: Reduces alert response time by showing exact room location.

**Acceptance Criteria**:
- [ ] Call Room Management Service to resolve room ID to room number
- [ ] Handle room not found gracefully (fallback to room ID)
- [ ] Cache room mappings (long TTL, rooms change infrequently)
- [ ] Add circuit breaker for Room Management Service calls

## Technical Specification
**Location**: \`clinical-workflow-service/VitalSignsService.java:751\`

## Estimation
**Story Points**: 2 | **Effort**: 1 day | **Complexity**: Low

**Business Impact**: Critical for emergency response workflows"

CREATED=$((CREATED + 1))
echo "✅ Created issue #$CREATED: Room number resolution"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# ============================================================
# BATCH 2: Frontend Accessibility (3 issues)
# ============================================================

echo "🎨 Creating Frontend Accessibility Issues (3 issues)..."
echo ""

# Issue 8: TODO-015 - Skip-to-content link
gh issue create \
  --title "[Frontend] Add skip-to-content link for keyboard navigation" \
  --label "feature,frontend,accessibility,P1-High" \
  --milestone "Q1-2026-Backend-Endpoints" \
  --body "## Feature Description

**User Story**: As a keyboard user, I want a skip-to-content link so I can bypass navigation menus.

**Business Value**: WCAG 2.4.1 compliance - improves accessibility for keyboard-only users.

**Acceptance Criteria**:
- [ ] Add invisible skip link at top of page
- [ ] Make link visible on focus (keyboard Tab key)
- [ ] Link jumps to main content area (#main)
- [ ] Verify with screen reader (NVDA/JAWS)

## Technical Specification
**Location**: \`frontend/clinical-portal/src/app/app.html\` (top of template)

**Implementation Pattern**:
\`\`\`html
<a href=\"#main\" class=\"skip-link\">Skip to main content</a>
\`\`\`

\`\`\`css
.skip-link {
  position: absolute;
  top: -40px;
  left: 0;
  background: #000;
  color: white;
  padding: 8px;
  z-index: 100;
}

.skip-link:focus {
  top: 0;
}
\`\`\`

## Estimation
**Story Points**: 1 | **Effort**: 4 hours | **Complexity**: Low

**WCAG Compliance**: WCAG 2.4.1 - Bypass Blocks (Level A)"

CREATED=$((CREATED + 1))
echo "✅ Created issue #$CREATED: Skip-to-content link"

# Issue 9: TODO-016 - ARIA labels for table buttons
gh issue create \
  --title "[Frontend] Add ARIA labels to table action buttons" \
  --label "feature,frontend,accessibility,P1-High" \
  --milestone "Q1-2026-Backend-Endpoints" \
  --body "## Feature Description

**User Story**: As a screen reader user, I want descriptive button labels so I understand what each action does.

**Business Value**: WCAG 4.1.2 compliance - 50% → 100% ARIA attribute coverage.

**Acceptance Criteria**:
- [ ] Add \`[attr.aria-label]\` to all icon-only buttons in tables
- [ ] Use patient context in labels (\"View patient John Doe\")
- [ ] Mark icons as \`aria-hidden=\"true\"\`
- [ ] Verify with screen reader testing

## Technical Specification
**Location**: All table templates (patients, evaluations, care gaps, results)

**Pattern**:
\`\`\`html
<button mat-icon-button
        (click)=\"viewPatient(patient)\"
        [attr.aria-label]=\"'View patient ' + patient.name\">
  <mat-icon aria-hidden=\"true\">visibility</mat-icon>
</button>
\`\`\`

## Estimation
**Story Points**: 2 | **Effort**: 1 day | **Complexity**: Low

**WCAG Compliance**: WCAG 4.1.2 - Name, Role, Value (Level A)"

CREATED=$((CREATED + 1))
echo "✅ Created issue #$CREATED: ARIA labels for tables"

# Issue 10: TODO-017 - Focus indicators
gh issue create \
  --title "[Frontend] Enhance focus indicators for keyboard navigation" \
  --label "feature,frontend,accessibility,P1-High" \
  --milestone "Q1-2026-Backend-Endpoints" \
  --body "## Feature Description

**User Story**: As a keyboard user, I want visible focus indicators so I know where I am on the page.

**Business Value**: WCAG 2.4.7 compliance - improves keyboard navigation experience.

**Acceptance Criteria**:
- [ ] Add high-contrast focus outline to all interactive elements
- [ ] Ensure 3:1 contrast ratio against background
- [ ] Test with keyboard-only navigation
- [ ] Verify focus is never trapped in modals

## Technical Specification
**Location**: \`frontend/clinical-portal/src/styles.scss\` (global styles)

**Implementation**:
\`\`\`scss
*:focus {
  outline: 2px solid #005fcc; // High contrast blue
  outline-offset: 2px;
}

button:focus, a:focus, input:focus, select:focus {
  outline: 2px solid #005fcc;
  outline-offset: 2px;
}
\`\`\`

## Estimation
**Story Points**: 1 | **Effort**: 4 hours | **Complexity**: Low

**WCAG Compliance**: WCAG 2.4.7 - Focus Visible (Level AA)"

CREATED=$((CREATED + 1))
echo "✅ Created issue #$CREATED: Focus indicators"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# ============================================================
# BATCH 3: Strategic Integrations (5 issues)
# ============================================================

echo "🚀 Creating Strategic Integration Issues (5 issues)..."
echo ""

# Create Q2 milestone for strategic features
echo "🔍 Creating Q2-2026-Strategic-Integrations milestone..."
gh api repos/webemo-aaron/hdim/milestones -f title="Q2-2026-Strategic-Integrations" -f due_on="2026-06-30T23:59:59Z" -f description="Strategic EHR integrations, patient engagement, and SDOH tools" 2>/dev/null || echo "  (Milestone may already exist)"

# Issue 11: TODO-019 - SMART on FHIR
gh issue create \
  --title "[Strategic] Implement SMART on FHIR compliance for Epic/Cerner embedding" \
  --label "feature,backend,frontend,fhir,P1-High" \
  --milestone "Q2-2026-Strategic-Integrations" \
  --body "## Feature Description

**User Story**: As a clinician, I want HDIM to launch from within Epic/Cerner so I don't need to switch applications.

**Business Value**: Seamless EHR integration enables HDIM to embed directly in Epic/Cerner patient charts.

**Acceptance Criteria**:
- [ ] Implement SMART App Launch Framework 1.0
- [ ] Support standalone and EHR launch contexts
- [ ] Handle OAuth 2.0 authorization with EHR
- [ ] Extract patient context from launch parameters
- [ ] Support patient-level and user-level scopes

## Technical Specification
**Components**: Gateway Service (OAuth), Frontend (SMART launcher)

**SMART Launch Sequence**:
1. EHR initiates launch with \`iss\` and \`launch\` parameters
2. App redirects to EHR authorization endpoint
3. EHR returns authorization code
4. App exchanges code for access token + patient context
5. App displays patient-specific content

**Epic Endpoints**: Use Epic FHIR R4 sandbox for testing
- Authorization: \`https://fhir.epic.com/interconnect-fhir-oauth/oauth2/authorize\`
- Token: \`https://fhir.epic.com/interconnect-fhir-oauth/oauth2/token\`

## Estimation
**Story Points**: 13 | **Effort**: 8-12 weeks | **Complexity**: High

**Business Impact**: Access to Epic customer marketplace (500M+ patients)"

CREATED=$((CREATED + 1))
echo "✅ Created issue #$CREATED: SMART on FHIR"

# Issue 12: TODO-020 - CDS Hooks
gh issue create \
  --title "[Strategic] Implement CDS Hooks for clinical decision support" \
  --label "feature,backend,fhir,P1-High" \
  --milestone "Q2-2026-Strategic-Integrations" \
  --body "## Feature Description

**User Story**: As a clinician, I want real-time care gap alerts when viewing patients in Epic/Cerner.

**Business Value**: Point-of-care alerts improve care gap closure rates by 40-60%.

**Acceptance Criteria**:
- [ ] Implement CDS Hooks 1.0 specification
- [ ] Support \`patient-view\` hook (fires when patient chart opened)
- [ ] Return care gap alerts as CDS Hooks cards
- [ ] Add \`order-select\` hook for care gap closure suggestions
- [ ] Implement card actions (\"Close Gap\", \"Dismiss\", \"Snooze\")

## Technical Specification
**Components**: CDS Hooks Service (new), Care Gap Service (integration)

**Supported Hooks**:
1. \`patient-view\`: Show care gaps when patient chart opened
2. \`order-select\`: Suggest orders to close care gaps
3. \`order-sign\`: Warn if order doesn't address open care gaps
4. \`appointment-book\`: Suggest preventive care appointments

**Response Format**:
\`\`\`json
{
  \"cards\": [{
    \"summary\": \"Open Care Gap: Diabetes HbA1c\",
    \"indicator\": \"warning\",
    \"detail\": \"Patient overdue for HbA1c test (last: 14 months ago)\",
    \"source\": { \"label\": \"HDIM Care Gap Detection\" },
    \"links\": [{ \"label\": \"View Care Gap\", \"url\": \"...\" }]
  }]
}
\`\`\`

## Estimation
**Story Points**: 10 | **Effort**: 6-8 weeks | **Complexity**: High

**Business Impact**: Real-time point-of-care clinical decision support"

CREATED=$((CREATED + 1))
echo "✅ Created issue #$CREATED: CDS Hooks"

# Issue 13: TODO-026 - Twilio SMS reminders
gh issue create \
  --title "[Strategic] Implement Twilio integration for SMS appointment reminders" \
  --label "feature,backend,P2-Medium" \
  --milestone "Q2-2026-Strategic-Integrations" \
  --body "## Feature Description

**User Story**: As a patient, I want SMS appointment reminders so I don't forget my appointments.

**Business Value**: Reduce no-show rates by 30-40%, improve patient satisfaction.

**Acceptance Criteria**:
- [ ] Integrate Twilio SMS API
- [ ] Send reminders 24 hours before appointment
- [ ] Support opt-in/opt-out workflows
- [ ] Add HIPAA-compliant message templates
- [ ] Track delivery status and failures

## Technical Specification
**Components**: Patient Engagement Service (new), Twilio SDK

**SMS Templates**:
- Reminder: \"Your appointment with Dr. Smith is tomorrow at 2:00 PM. Reply CONFIRM to confirm or CANCEL to reschedule.\"
- Confirmation: \"Thank you for confirming your appointment tomorrow at 2:00 PM.\"
- Cancellation: \"Your appointment has been cancelled. Call 555-1234 to reschedule.\"

**HIPAA Considerations**:
- Do NOT include diagnosis or specific medical information
- Use generic appointment language
- Implement delivery confirmation tracking
- Add opt-out mechanism (\"Reply STOP to unsubscribe\")

## Estimation
**Story Points**: 5 | **Effort**: 2 weeks | **Complexity**: Medium

**Business Impact**: 30-40% reduction in no-show rates ($100-200K annual savings for 1000-patient practice)"

CREATED=$((CREATED + 1))
echo "✅ Created issue #$CREATED: Twilio SMS reminders"

# Issue 14: TODO-029 - NowPow SDOH integration
gh issue create \
  --title "[Strategic] Implement NowPow community resource directory integration" \
  --label "feature,backend,sdoh,P1-High" \
  --milestone "Q2-2026-Strategic-Integrations" \
  --body "## Feature Description

**User Story**: As a care coordinator, I want to refer patients to community resources (food banks, housing assistance) to address social determinants of health.

**Business Value**: Automated SDOH referrals improve health outcomes and reduce hospital readmissions.

**Acceptance Criteria**:
- [ ] Integrate NowPow API for community resource search
- [ ] Filter resources by patient ZIP code and needs
- [ ] Send electronic referrals to community organizations
- [ ] Track referral status and outcomes
- [ ] Add SDOH screening questions (housing, food, transportation)

## Technical Specification
**Components**: SDOH Service (new), NowPow API integration

**NowPow API Endpoints**:
- Search: \`GET /api/v1/resources?zipcode=60601&category=food\`
- Referral: \`POST /api/v1/referrals\`
- Status: \`GET /api/v1/referrals/{id}/status\`

**SDOH Categories**:
- Food insecurity
- Housing instability
- Transportation barriers
- Utility assistance
- Legal aid
- Employment assistance

**Workflow**:
1. Care coordinator completes SDOH screening with patient
2. System searches NowPow for relevant resources
3. Coordinator selects resources and creates electronic referral
4. Patient receives referral information (SMS/email)
5. System tracks referral status and follow-up

## Estimation
**Story Points**: 8 | **Effort**: 3 weeks | **Complexity**: High

**Business Impact**: Address social determinants of health, reduce readmissions by 20-30%"

CREATED=$((CREATED + 1))
echo "✅ Created issue #$CREATED: NowPow SDOH integration"

# Issue 15: TODO-035 - Validic RPM integration
gh issue create \
  --title "[Strategic] Integrate Validic for multi-device RPM data" \
  --label "feature,backend,rpm,P2-Medium" \
  --milestone "Q2-2026-Strategic-Integrations" \
  --body "## Feature Description

**User Story**: As a patient, I want my wearable device data automatically sent to my care team so they can monitor my health remotely.

**Business Value**: Remote patient monitoring enables proactive care and reduces hospital admissions.

**Acceptance Criteria**:
- [ ] Integrate Validic API for device data ingestion
- [ ] Support 300+ devices (Fitbit, Apple Watch, Withings, etc.)
- [ ] Ingest vital signs (heart rate, blood pressure, weight, glucose)
- [ ] Create FHIR Observations from device data
- [ ] Implement alert thresholds for abnormal readings

## Technical Specification
**Components**: RPM Service (new), Validic API integration

**Supported Devices** (via Validic):
- Fitness trackers: Fitbit, Garmin, Apple Watch
- Blood pressure: Omron, Withings
- Weight scales: Withings, Fitbit Aria
- Glucose meters: Dexcom, Freestyle Libre
- Pulse oximeters: Nonin, Masimo

**Validic API**:
- Webhook: Validic pushes device data to HDIM
- Organization: \`GET /v1/organizations/{id}\`
- Users: \`GET /v1/organizations/{id}/users\`
- Data: Real-time webhook delivery

**Data Flow**:
1. Patient connects device to Validic
2. Validic webhook sends data to HDIM RPM Service
3. RPM Service creates FHIR Observation
4. Observation stored in FHIR Service
5. Alert Service checks thresholds and triggers alerts

## Estimation
**Story Points**: 8 | **Effort**: 3 weeks | **Complexity**: High

**Business Impact**: Enable remote patient monitoring for chronic disease management (300+ supported devices)"

CREATED=$((CREATED + 1))
echo "✅ Created issue #$CREATED: Validic RPM integration"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "✅ Successfully created $CREATED issues!"
echo ""
echo "📊 Summary:"
echo "  - Backend Endpoints: 7 issues"
echo "  - Frontend Accessibility: 3 issues"
echo "  - Strategic Integrations: 5 issues"
echo ""
echo "🔗 View all issues:"
echo "   gh issue list --milestone Q1-2026-Backend-Endpoints"
echo "   gh issue list --milestone Q2-2026-Strategic-Integrations"
echo ""
echo "🎯 Next steps:"
echo "   1. Review issues in GitHub"
echo "   2. Assign issues to team members"
echo "   3. Create project board for Q1/Q2 2026"
echo "   4. Begin implementation"
echo ""
