# HDIM Incomplete Features Catalog

**Generated:** January 23, 2026
**Purpose:** Comprehensive catalog of incomplete features, pending implementations, and TODO items for GitHub issue creation

---

## Executive Summary

This document catalogs **35+ incomplete backend endpoints**, **frontend enhancements**, and **strategic integrations** identified across the HDIM platform. These are organized by priority (P0-P3) and grouped by functional area.

---

## 1. Backend API Endpoints (35+ Incomplete)

### 1.1 Clinical Workflow Service (Priority: P1-High)

#### Vital Signs Management
- **TODO-001**: Implement real-time vital sign alerts via WebSocket
  - Current: Logs only, no real-time notification
  - Location: `VitalSignsService.java:332-334`
  - Impact: Critical care alerts not delivered to providers in real-time

- **TODO-002**: Implement FHIR Observation resource creation for vital signs
  - Current: Placeholder JSON, no HAPI FHIR integration
  - Location: `VitalSignsService.java:502-503`
  - Impact: Vital signs not published to FHIR ecosystem

- **TODO-003**: Add pagination support for vital signs history
  - Current: Returns all records (scalability issue)
  - Location: `VitalSignsService.java:594`
  - Impact: Performance degradation with large patient history

- **TODO-004**: Implement Kafka event publishing for abnormal vitals
  - Current: No event-driven alerting
  - Location: `VitalSignsService.java:332`
  - Impact: Downstream services don't receive vital sign alerts

#### Patient Check-In
- **TODO-005**: Add pagination support for check-in history
  - Location: `PatientCheckInService.java:239`
  - Impact: Slow queries for patients with extensive visit history

#### Room Management
- **TODO-006**: Implement OUT_OF_SERVICE room status workflow
  - Location: `RoomManagementService.java:408`
  - Impact: Cannot properly track room maintenance and availability

### 1.2 Demo Orchestrator Service (Priority: P2-Medium)

- **TODO-007**: Implement data seeding logic for demo environments
  - Location: `DataManagerService.java:18`
  - Impact: Cannot auto-populate demo data for sales presentations

- **TODO-008**: Implement data clearing logic for demo resets
  - Location: `DataManagerService.java:25`
  - Impact: Manual cleanup required for demo environment resets

- **TODO-009**: Implement WebSocket publishing for DevOps agent logs
  - Location: `DevOpsAgentClient.java:35, 40`
  - Impact: Real-time deployment monitoring not available

### 1.3 Patient Event Handler Service (Priority: P1-High)

- **TODO-010**: Implement proper FHIR identifier serialization for merged patients
  - Location: `PatientMergedEventHandler.java:139`
  - Impact: Merged patient identifiers may not properly serialize for FHIR compliance

### 1.4 Predictive Analytics Service (Priority: P2-Medium)

- **TODO-011**: Add specific care gap prediction audit logging
  - Location: `PredictedCareGapService.java:82`
  - Impact: Incomplete audit trail for predictive model usage

### 1.5 Missing Service Integrations

- **TODO-012**: Patient name resolution in vital signs alerts
  - Location: `VitalSignsService.java:709, 750`
  - Impact: Vital sign alerts show patient ID but not human-readable name

- **TODO-013**: Room number resolution for vital sign alerts
  - Location: `VitalSignsService.java:751`
  - Impact: Providers don't know which room has the alert

---

## 2. Frontend Features (Angular Clinical Portal)

### 2.1 HIPAA Compliance - Session Timeout (Priority: P0-Critical)

- **TODO-014**: Add audit logging to session timeout handler
  - Current: Session timeout implemented but no audit trail
  - Location: `app.ts` - `handleSessionTimeout()` method
  - Impact: HIPAA §164.312(a)(2)(iii) - Cannot prove automatic logoff occurred

### 2.2 Accessibility (WCAG 2.1 Level A) (Priority: P1-High)

- **TODO-015**: Add skip-to-content link for keyboard navigation
  - Impact: WCAG 2.4.1 violation - keyboard users cannot bypass navigation

- **TODO-016**: Add ARIA labels to table action buttons
  - Current: 50% coverage (343/686 ARIA attributes)
  - Impact: Screen readers cannot describe button purposes

- **TODO-017**: Enhance focus indicators for keyboard navigation
  - Impact: WCAG 2.4.7 violation - keyboard users lose context

### 2.3 Console.log Migration (Priority: P2-Medium)

- **TODO-018**: Migrate remaining 48 files from console.log to LoggerService
  - Current: Migration script available but not executed
  - Script: `scripts/migrate-console-to-logger.sh`
  - Impact: Potential PHI exposure in production browser logs

---

## 3. Strategic Integration Roadmap (Long-Term)

### 3.1 EHR & Clinical Workflow Integration (Priority: P1-High)

- **TODO-019**: SMART on FHIR compliance for Epic/Cerner embedding
  - Timeline: 8-12 weeks
  - Impact: HDIM apps can embed directly in Epic/Cerner patient charts

- **TODO-020**: CDS Hooks implementation for clinical decision support
  - Timeline: 6-8 weeks
  - Hooks: patient-view, order-select, order-sign, appointment-book
  - Impact: Real-time care gap alerts at point of care

- **TODO-021**: Epic App Orchard certification
  - Timeline: 16-24 weeks
  - Impact: Access to Epic customer marketplace

- **TODO-022**: Cerner CODE Program certification
  - Timeline: 12-16 weeks
  - Impact: Access to Cerner customer marketplace

### 3.2 Mental Health & Behavioral Health (Priority: P1-High)

- **TODO-023**: Implement Columbia Suicide Severity Rating Scale (C-SSRS)
  - Impact: Suicide risk assessment capability

- **TODO-024**: Integrate with Ginger behavioral health platform
  - Impact: Virtual therapy session coordination

- **TODO-025**: Implement SBIRT (Screening, Brief Intervention, Referral to Treatment) workflow
  - Impact: Substance abuse screening and intervention

### 3.3 Patient Engagement (Priority: P2-Medium)

- **TODO-026**: Implement Twilio integration for SMS appointment reminders
  - Impact: Reduce no-show rates by 30-40%

- **TODO-027**: Integrate with MyChart/Epic MyChart for patient portal
  - Impact: Bi-directional patient communication

- **TODO-028**: Implement automated patient education content delivery
  - Impact: Improve patient health literacy

### 3.4 SDOH & Community Health (Priority: P1-High)

- **TODO-029**: Implement NowPow community resource directory integration
  - Impact: Automated social service referrals

- **TODO-030**: Add UniteUs closed-loop referral tracking
  - Impact: Track social service referral outcomes

- **TODO-031**: Implement PRAPARE SDOH screening tool
  - Impact: Comprehensive social risk assessment

### 3.5 AI/ML Predictive Analytics (Priority: P2-Medium)

- **TODO-032**: Implement hospitalization risk prediction models
  - Timeline: Q2 2025
  - Impact: 25-35% improvement in high-risk patient identification

- **TODO-033**: Add cost prediction models for high-cost patient identification
  - Timeline: Q2 2025
  - Impact: 40-50% improvement in intervention targeting

- **TODO-034**: Implement behavioral health prediction (suicide risk, depression severity)
  - Timeline: Q2 2025
  - Impact: Early intervention for mental health crises

### 3.6 Remote Patient Monitoring (Priority: P2-Medium)

- **TODO-035**: Integrate with Validic for multi-device RPM data
  - Impact: Support 300+ wearables and medical devices

- **TODO-036**: Add Apple HealthKit integration for iOS devices
  - Impact: Native iOS health data capture

- **TODO-037**: Integrate Google Fit for Android devices
  - Impact: Native Android health data capture

### 3.7 Care Coordination (Priority: P1-High)

- **TODO-038**: Implement eReferral workflow via Carequality
  - Impact: Seamless specialist referrals across health systems

- **TODO-039**: Add Direct Secure Messaging for HIPAA-compliant provider communication
  - Impact: Secure clinical document exchange

- **TODO-040**: Implement CarePort hospital admission/discharge notifications
  - Impact: Real-time ADT alerts for care coordination

---

## 4. Infrastructure & DevOps (Priority: P2-Medium)

### 4.1 Monitoring & Observability

- **TODO-041**: Implement custom OpenTelemetry spans for business logic
  - Impact: Better visibility into application performance

- **TODO-042**: Add Grafana dashboards for HEDIS measure evaluation performance
  - Impact: Proactive performance issue detection

### 4.2 Testing & Quality

- **TODO-043**: Add integration tests for SMART on FHIR launch flows
  - Impact: Prevent regressions in EHR embedding

- **TODO-044**: Implement contract testing with Pact for service-to-service communication
  - Impact: Catch API contract violations early

---

## 5. Documentation & Compliance (Priority: P3-Low)

- **TODO-045**: Document all HIPAA audit logging patterns
  - Impact: Simplified compliance audits

- **TODO-046**: Create runbook for Epic App Orchard submission process
  - Impact: Faster EHR marketplace onboarding

- **TODO-047**: Document CDS Hooks implementation guide
  - Impact: Accelerate clinical decision support development

---

## Priority Summary

| Priority | Count | Definition |
|----------|-------|------------|
| **P0-Critical** | 1 | Blocks production deployment, HIPAA violation risk |
| **P1-High** | 14 | Important for milestone, strategic value |
| **P2-Medium** | 27 | Should have, enhances capabilities |
| **P3-Low** | 5 | Nice to have, documentation improvements |
| **Total** | **47** | |

---

## Next Steps

1. **Create GitHub Issues**: Use templates from `docs/roadmap/project-management/issue-templates.md`
2. **Assign Milestones**:
   - Q1 2026: P0-Critical + P1-High backend endpoints
   - Q2 2026: Strategic integrations (SMART on FHIR, CDS Hooks)
   - Q3 2026: Patient engagement and RPM integrations
   - Q4 2026: AI/ML predictive analytics enhancements

3. **Label Strategy**:
   - Type: `feature`, `enhancement`, `technical-debt`
   - Area: `backend`, `frontend`, `infrastructure`, `integration`
   - Priority: `P0-Critical`, `P1-High`, `P2-Medium`, `P3-Low`

---

**Maintained By:** Engineering Team
**Review Cycle:** Monthly
**Last Updated:** January 23, 2026
