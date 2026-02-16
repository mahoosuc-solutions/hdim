# Clinical Portal User Guide

**Audience**: Clinicians, Nurses, Care Managers  
**Last Updated**: February 2026

## 1. Getting Started

### Login with SSO
1. Open the Clinical Portal URL for your environment.
2. Select your organization SSO option or use assigned credentials.
3. Complete MFA if prompted.
4. Confirm the active tenant in the top navigation after login.

Related references:
- `docs/user/guides/getting-started/first-day.md`
- `docs/user/guides/reference/security-privacy.md`

### Dashboard overview
After login, the dashboard presents:
- Open care gap counts
- Patient risk and outreach indicators
- Quality measure performance highlights
- Task and alert summaries

Screenshot:
- `docs/screenshots/care-manager/care-manager-dashboard-overview.png`

Related reference:
- `docs/user/guides/features/core/dashboard.md`

## 2. Patient Search

### Search by name, MRN, DOB
1. Navigate to **Patients**.
2. Use quick search or advanced filters.
3. Search by:
   - Patient name
   - MRN
   - Date of birth

Screenshot:
- `docs/screenshots/care-manager/care-manager-patient-list.png`

Related references:
- `docs/user/guides/features/core/search.md`
- `docs/user/guides/getting-started/platform-navigation.md`

### Viewing patient details
From search results:
1. Open the patient chart.
2. Review demographics, conditions, medications, care gaps, and risk profile.
3. Use timeline and encounter history for clinical context.

Screenshot:
- `docs/screenshots/care-manager/care-manager-patient-detail.png`

## 3. Care Gap Management

### Finding open gaps
1. Open **Care Gaps**.
2. Filter by priority, measure type, risk, or days open.
3. Prioritize high-risk patients and urgent due dates.

Screenshot:
- `docs/screenshots/care-manager/care-manager-care-gaps-overview.png`

### Closing gaps
1. Open gap detail.
2. Select closure type (service completed, refusal, not clinically appropriate, etc.).
3. Add notes and supporting evidence.
4. Submit closure and confirm updated status.

Screenshot:
- `docs/screenshots/care-manager/care-manager-care-gap-detail.png`

### Assigning gaps
1. Use bulk or single-gap actions.
2. Assign to clinician, nurse, or care coordinator.
3. Track ownership and due dates in work queues.

Related references:
- `docs/user-guides/care-manager-guide.md`
- `docs/user/guides/workflows/physician/responding-care-gaps.md`

## 4. Quality Measures

### Viewing patient measures
1. Navigate to **Quality Measures**.
2. Review numerator/denominator performance and benchmark status.
3. Drill into patient-level evidence and exclusions.

Screenshot:
- `docs/screenshots/care-manager/care-manager-quality-measures.png`

### Understanding measure logic
- Measure definitions and performance logic are available in the measure detail and documentation links.
- For governance and troubleshooting, reference measure builder and workflow docs.

Related references:
- `docs/user/guides/workflows/physician/quality-metrics.md`
- `docs/user/guides/MEASURE_BUILDER_GETTING_STARTED.md`

## 5. AI Assistant

### Asking questions
1. Open the assistant panel/workspace.
2. Ask clinical or operational questions using clear context.
3. Use suggested prompts for patient review, care gap planning, and prioritization.

Screenshots:
- `docs/screenshots/ai-user/ai-user-ai-chat-interface.png`
- `docs/screenshots/ai-user/ai-user-agent-selection.png`

### Understanding responses
- Validate recommendations against patient chart context and policy requirements.
- Treat assistant output as decision support, not autonomous final authority.
- Escalate uncertain recommendations to clinical leadership.

Screenshots:
- `docs/screenshots/ai-user/ai-user-conversation-history.png`
- `docs/screenshots/ai-user/ai-user-tool-library.png`

## Additional Role Guides

- Care Manager guide: `docs/user-guides/care-manager-guide.md`
- Physician workflows: `docs/user/guides/workflows/physician/daily-workflow.md`
- Admin and user management: `docs/user/guides/admin/user-management.md`

