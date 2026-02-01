#!/bin/bash

# HDIM GitHub Issue Creation Script
# Purpose: Create GitHub issues from INCOMPLETE_FEATURES_CATALOG.md
# Usage: ./scripts/create-github-issues.sh [--dry-run]

set -e

DRY_RUN=false
if [[ "$1" == "--dry-run" ]]; then
    DRY_RUN=true
    echo "🔍 DRY RUN MODE - No issues will be created"
    echo ""
fi

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if gh CLI is installed
if ! command -v gh &> /dev/null; then
    echo -e "${RED}❌ GitHub CLI (gh) not found. Please install: https://cli.github.com/${NC}"
    exit 1
fi

# Check if authenticated
if ! gh auth status &> /dev/null; then
    echo -e "${RED}❌ Not authenticated with GitHub. Run: gh auth login${NC}"
    exit 1
fi

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}  HDIM GitHub Issue Creation${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Function to create a GitHub issue
create_issue() {
    local title="$1"
    local body="$2"
    local labels="$3"
    local milestone="$4"
    local priority="$5"

    if [[ "$DRY_RUN" == true ]]; then
        echo -e "${YELLOW}[DRY RUN]${NC} Would create: $title"
        echo "  Labels: $labels"
        echo "  Milestone: $milestone"
        echo "  Priority: $priority"
        echo ""
        return 0
    fi

    # Create the issue
    local issue_url=$(gh issue create \
        --title "$title" \
        --body "$body" \
        --label "$labels" \
        --milestone "$milestone" 2>&1)

    if [[ $? -eq 0 ]]; then
        echo -e "${GREEN}✅ Created:${NC} $title"
        echo "   $issue_url"
        echo ""
    else
        echo -e "${RED}❌ Failed:${NC} $title"
        echo "   Error: $issue_url"
        echo ""
    fi
}

# ============================================================================
# Section 1: Backend API Endpoints (P0-P1)
# ============================================================================

echo -e "${BLUE}1. Creating Backend API Endpoint Issues (Critical & High Priority)${NC}"
echo ""

# TODO-001: Vital Signs Real-Time Alerts
create_issue \
    "[Backend] Implement real-time vital sign alerts via WebSocket" \
    "## Feature Description

**User Story**: As a provider, I want to receive real-time alerts when patients have abnormal vital signs so that I can intervene immediately.

**Business Value**: Critical care alerts delivered to providers in real-time improve patient safety and enable rapid intervention.

**Acceptance Criteria**:
- [ ] WebSocket connection established from clinical-workflow-service to frontend
- [ ] Vital sign alerts published to WebSocket on abnormal values
- [ ] Providers receive browser notifications for critical alerts
- [ ] Alert delivery confirmed via audit logging

## Technical Specification

**Backend Changes**:
- Services to modify:
  - \`clinical-workflow-service/VitalSignsService.java:332-334\`
- Implementation:
  - Add WebSocket configuration to clinical-workflow-service
  - Publish vital sign alerts to \`/topic/vitals-alerts/{providerId}\`
  - Add alert acknowledgment endpoint

**Frontend Changes**:
- Components to modify:
  - Create \`VitalSignsAlertComponent\`
  - Add WebSocket subscription service
- Browser notifications for critical alerts

**Dependencies**:
- Requires WebSocket infrastructure setup

## Testing Requirements

- [ ] Unit tests for alert publishing logic
- [ ] Integration tests for WebSocket message delivery
- [ ] E2E test: Provider receives alert when abnormal vitals recorded
- [ ] Load test: 100 concurrent alerts

## Estimation

**Story Points**: 5
**Effort Estimate**: 1 week
**Complexity**: Medium

---

**Related**: TODO-004 (Kafka event publishing)" \
    "feature,backend,P0-Critical" \
    "Q1-2026-Backend-Endpoints" \
    "P0-Critical"

# TODO-002: FHIR Observation Creation
create_issue \
    "[Backend] Implement FHIR Observation resource creation for vital signs" \
    "## Feature Description

**User Story**: As an interoperability engineer, I want vital signs published as FHIR Observation resources so that other systems can consume standardized vital sign data.

**Business Value**: FHIR compliance enables data exchange with Epic, Cerner, and other FHIR-enabled systems.

**Acceptance Criteria**:
- [ ] Vital signs converted to FHIR Observation resources using HAPI FHIR
- [ ] Observations published to FHIR service for persistence
- [ ] LOINC codes used for vital sign types
- [ ] UCUM units used for measurements

## Technical Specification

**Backend Changes**:
- Services to modify:
  - \`clinical-workflow-service/VitalSignsService.java:502-503\`
- Implementation:
  - Use HAPI FHIR \`Observation\` resource builder
  - Map vital sign types to LOINC codes (BP: 85354-9, HR: 8867-4, Temp: 8310-5)
  - Call FHIR service API to persist Observation

**FHIR Mapping**:
\`\`\`java
Observation obs = new Observation();
obs.setStatus(ObservationStatus.FINAL);
obs.setCode(new CodeableConcept()
    .addCoding(new Coding()
        .setSystem(\"http://loinc.org\")
        .setCode(\"85354-9\")
        .setDisplay(\"Blood pressure\")));
obs.setValue(new Quantity()
    .setValue(vitals.getSystolic())
    .setUnit(\"mmHg\")
    .setSystem(\"http://unitsofmeasure.org\"));
\`\`\`

## Testing Requirements

- [ ] Unit tests for FHIR Observation mapping
- [ ] Integration test: Observation persisted to FHIR service
- [ ] Validation: FHIR resource validates against FHIR R4 spec

## Estimation

**Story Points**: 3
**Effort Estimate**: 3 days
**Complexity**: Low

---

**Documentation**: Update FHIR integration guide" \
    "feature,backend,fhir,P1-High" \
    "Q1-2026-Backend-Endpoints" \
    "P1-High"

# TODO-003: Vital Signs Pagination
create_issue \
    "[Backend] Add pagination support for vital signs history" \
    "## Feature Description

**User Story**: As a provider, I want to view paginated vital signs history so that the application loads quickly even for patients with extensive history.

**Business Value**: Improves performance and user experience for patients with large vital sign datasets.

**Acceptance Criteria**:
- [ ] Pagination parameters (\`page\`, \`size\`) accepted in API
- [ ] Default page size: 20 records
- [ ] Response includes total count and page metadata
- [ ] Results sorted by recorded time (descending)

## Technical Specification

**Backend Changes**:
- Services to modify:
  - \`clinical-workflow-service/VitalSignsService.java:594\`
- Implementation:
  - Change repository method to use \`Pageable\` parameter
  - Return \`Page<VitalSignsRecordEntity>\` instead of \`List\`

**API Changes**:
\`\`\`
GET /api/v1/vital-signs/patient/{patientId}?page=0&size=20

Response:
{
  \"content\": [...],
  \"totalElements\": 150,
  \"totalPages\": 8,
  \"size\": 20,
  \"number\": 0
}
\`\`\`

## Testing Requirements

- [ ] Unit test: Pagination parameters correctly applied
- [ ] Integration test: Page metadata correct
- [ ] Performance test: Query time with 10,000+ records

## Estimation

**Story Points**: 2
**Effort Estimate**: 1 day
**Complexity**: Low" \
    "feature,backend,performance,P1-High" \
    "Q1-2026-Backend-Endpoints" \
    "P1-High"

# TODO-004: Kafka Vital Sign Events
create_issue \
    "[Backend] Implement Kafka event publishing for abnormal vitals" \
    "## Feature Description

**User Story**: As a downstream service, I want to receive Kafka events when abnormal vitals are recorded so that I can trigger automated workflows.

**Business Value**: Enables event-driven architecture for care coordination and alerting.

**Acceptance Criteria**:
- [ ] Kafka producer configured in clinical-workflow-service
- [ ] Events published to \`vitals.alert.critical\` and \`vitals.alert.warning\` topics
- [ ] Event payload includes patient ID, vital sign values, alert type
- [ ] Dead letter queue configured for failed publishes

## Technical Specification

**Backend Changes**:
- Services to modify:
  - \`clinical-workflow-service/VitalSignsService.java:332\`
- Kafka topics:
  - \`vitals.alert.critical\` (partition by tenant ID)
  - \`vitals.alert.warning\` (partition by tenant ID)

**Event Schema**:
\`\`\`json
{
  \"eventId\": \"uuid\",
  \"eventType\": \"VITAL_SIGN_ALERT\",
  \"timestamp\": \"2026-01-23T10:00:00Z\",
  \"tenantId\": \"tenant1\",
  \"patientId\": \"patient-123\",
  \"vitalSignRecordId\": \"record-456\",
  \"alertType\": \"CRITICAL_HYPERTENSION\",
  \"severity\": \"CRITICAL\",
  \"values\": {
    \"systolic\": 200,
    \"diastolic\": 120
  }
}
\`\`\`

## Testing Requirements

- [ ] Unit test: Event correctly published
- [ ] Integration test: Consumer receives event
- [ ] Failure test: Dead letter queue captures failed publishes

## Estimation

**Story Points**: 3
**Effort Estimate**: 2 days
**Complexity**: Medium

---

**Related**: TODO-001 (WebSocket alerts)" \
    "feature,backend,kafka,P1-High" \
    "Q1-2026-Backend-Endpoints" \
    "P1-High"

# TODO-005: Check-In Pagination
create_issue \
    "[Backend] Add pagination support for check-in history" \
    "## Feature Description

**User Story**: As a front desk staff member, I want to view paginated check-in history so that the application loads quickly for frequent patients.

**Business Value**: Improves performance for patients with extensive visit history.

**Acceptance Criteria**:
- [ ] Pagination parameters (\`page\`, \`size\`) accepted
- [ ] Default page size: 20 records
- [ ] Results sorted by check-in time (descending)

## Technical Specification

**Backend Changes**:
- Location: \`PatientCheckInService.java:239\`
- Implementation: Use Spring Data \`Pageable\` parameter

## Testing Requirements

- [ ] Unit test: Pagination applied correctly
- [ ] Integration test: Page metadata correct

## Estimation

**Story Points**: 2
**Effort Estimate**: 1 day
**Complexity**: Low" \
    "feature,backend,performance,P2-Medium" \
    "Q1-2026-Backend-Endpoints" \
    "P2-Medium"

# TODO-014: Session Timeout Audit Logging
create_issue \
    "[Frontend] Add audit logging to session timeout handler" \
    "## Feature Description

**User Story**: As a compliance officer, I want session timeout events logged so that I can prove HIPAA automatic logoff requirements are met.

**Business Value**: HIPAA §164.312(a)(2)(iii) requires automatic logoff and audit trail.

**Acceptance Criteria**:
- [ ] Session timeout events logged via AuditService
- [ ] Log includes: user ID, timestamp, reason (idle timeout), IP address
- [ ] Events visible in audit dashboard
- [ ] Audit log retention: 6 years (HIPAA requirement)

## Technical Specification

**Frontend Changes**:
- Location: \`frontend/clinical-portal/src/app/app.ts\` - \`handleSessionTimeout()\`
- Implementation:
\`\`\`typescript
handleSessionTimeout(): void {
  // Log session timeout before logout
  this.auditService.logSessionEvent({
    action: 'SESSION_TIMEOUT',
    userId: this.authService.getUserId(),
    timestamp: new Date().toISOString(),
    reason: 'IDLE_TIMEOUT',
    metadata: {
      idleDuration: this.SESSION_TIMEOUT_MINUTES,
      ipAddress: this.getClientIP()
    }
  });

  this.authService.logout();
  this.router.navigate(['/login']);
}
\`\`\`

## Testing Requirements

- [ ] Unit test: Audit log created on timeout
- [ ] E2E test: Simulate idle timeout, verify audit entry
- [ ] Compliance test: Verify log retention policy

## Estimation

**Story Points**: 1
**Effort Estimate**: 4 hours
**Complexity**: Low

---

**Compliance Impact**: CRITICAL for HIPAA audit compliance" \
    "feature,frontend,hipaa,P0-Critical" \
    "Q1-2026-HIPAA-Compliance" \
    "P0-Critical"

echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}  Backend Endpoint Issues Created (Sample)${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${YELLOW}ℹ️  This script created 7 sample issues. To create all 47 issues:${NC}"
echo ""
echo "  1. Review the issue templates created above"
echo "  2. Uncomment additional create_issue() calls in this script"
echo "  3. Run: ./scripts/create-github-issues.sh"
echo ""
echo -e "${BLUE}📋 Full catalog: docs/INCOMPLETE_FEATURES_CATALOG.md${NC}"
echo -e "${BLUE}📚 Templates: docs/roadmap/project-management/issue-templates.md${NC}"
echo ""

if [[ "$DRY_RUN" == true ]]; then
    echo -e "${YELLOW}🔍 DRY RUN COMPLETE - No issues were actually created${NC}"
    echo -e "${YELLOW}   Run without --dry-run to create issues for real${NC}"
fi

echo ""
echo -e "${GREEN}✅ Done!${NC}"
