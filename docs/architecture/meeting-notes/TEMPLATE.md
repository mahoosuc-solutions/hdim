# Architecture Review Meeting Notes - TEMPLATE

Use this template to document outcomes from weekly architecture review meetings.

---

# Meeting Notes: [DATE - e.g., January 23, 2026]

**Date**: YYYY-MM-DD
**Time**: 10:00 AM - 11:00 AM PT
**Attendees**: [List names and roles]
**Facilitator**: [Architecture Lead name]
**Scribe**: [Note-taker]

---

## Agenda

1. Opening & Status Update (5 min)
2. [ADR Title #1] (25 min)
3. [ADR Title #2] (25 min)
4. Closing & Action Items (5 min)

---

## 1. Opening & Status Update (5 min)

### Active Decisions Summary

- Total active decisions: [#]
- Decisions approved this month: [#]
- Decisions deferred: [#]
- Decisions needing follow-up: [list]

### Key Metrics

| Metric | Current | Target | Status |
|--------|---------|--------|--------|
| ADRs created Q1 2026 | [#] | 4-6 | [Status] |
| Time to implementation | [#] weeks | <4 weeks | [Status] |
| Team satisfaction | [score]/5 | >4/5 | [Status] |

### Status of Previous Decisions

- ADR-XXX [Title]: Implementation [% complete] (On track / At risk)
- ADR-YYY [Title]: 1-month review scheduled [DATE]

---

## 2. ADR #[NUMBER]: [TITLE]

**Presenter**: [Name]
**Decision Status**: [Proposed / Accepted / Conditional / Rejected / Deferred]

### Presentation Summary

**Problem**: [1-2 sentence summary of the problem]

**Options Considered**:
1. [Option 1 name]
2. [Option 2 name]
3. [Option 3 name - if applicable]

**Selected Option**: [Option # - Rationale in 1-2 sentences]

**Key Consequences**:
- Positive: [Impact]
- Negative: [Impact]
- Neutral: [Impact]

### Discussion

**Questions Asked**:
- Q: [Question from reviewer]
  A: [Answer from presenter]

- Q: [Question]
  A: [Answer]

**Concerns Raised**:
- Concern: [Issue raised]
  Response: [How will it be addressed?]

**Comments/Suggestions**:
- Suggestion: [Recommendation]
  - Presenter response: [Will this be incorporated?]

### Review Criteria Assessment

| Criteria | Pass/Fail | Notes |
|----------|-----------|-------|
| Problem Clarity | ✅ / ❌ | [Comments] |
| Options Analysis | ✅ / ❌ | [Comments] |
| Decision Justification | ✅ / ❌ | [Comments] |
| Impact Assessment | ✅ / ❌ | [Comments] |
| Implementation Plan | ✅ / ❌ | [Comments] |
| Alignment | ✅ / ❌ | [Comments] |
| Team Alignment | ✅ / ❌ | [Comments] |

### Decision

**DECISION**: ✅ **ACCEPTED** / 🔄 **CONDITIONAL** / ❌ **REJECTED** / ⏸️ **DEFERRED**

**Approvers**:
- [ ] Architecture Lead: [Name] - ✅ Approves
- [ ] Tech Lead [Domain]: [Name] - ✅ Approves
- [ ] [Other required role]: [Name] - ✅ Approves

**If Conditional**:
- Condition 1: [What must be verified?]
  - Owner: [Name]
  - Deadline: [Date]
  - How verified: [Method]
- Condition 2: [What must be verified?]
  - Owner: [Name]
  - Deadline: [Date]
  - How verified: [Method]

**If Rejected/Deferred**:
- Reason: [Why not accepted?]
- Feedback: [Specific feedback for revision]
- Next steps: [When to resubmit?]

### Implementation Details (If Accepted)

**Implementation Owner**: [Name/Team]
**Target Start Date**: [Date]
**Target Completion Date**: [Date]
**Success Criteria**:
- [ ] [Criterion 1]
- [ ] [Criterion 2]
- [ ] [Criterion 3]

**Dependencies**:
- Depends on: [ADR-###] (status: [status])
- Blocks: [ADR-###] (waiting since [date])

**1-Month Review Scheduled**: [Date]
**3-Month Review Scheduled**: [Date]

---

## 3. ADR #[NUMBER]: [TITLE]

[Same format as ADR #2 above]

---

## 4. Closing & Action Items

### Action Items

| Action | Owner | Due Date | Status |
|--------|-------|----------|--------|
| Implement ADR-XXX | [Team] | [Date] | Assigned |
| Prepare ADR-YYY for next meeting | [Name] | [Date] | Assigned |
| Schedule condition verification for ADR-ZZZ | [Name] | [Date] | Assigned |
| [Other follow-up] | [Name] | [Date] | Assigned |

### Next Meeting

**Date**: Thursday, [DATE]
**Time**: 10:00 AM PT
**Expected ADRs**:
- ADR-[#]: [Title] (Ready / Still in drafting)
- ADR-[#]: [Title] (Ready / Still in drafting)

### Key Decisions/Reminders

- **ADR-XXX accepted**: Implementation begins [date]
- **ADR-YYY deferred**: Resubmit when [condition is met]
- **ADR-ZZZ conditions**: Must verify by [date]

### Communication

Meeting recording: [Link to Zoom recording if available]
Announcements to post in #architecture-discussion:
- [Decision 1 outcome]
- [Decision 2 outcome]
- [Action items]

---

## Attendee Feedback (Optional)

**What went well**:
- [Feedback]
- [Feedback]

**What could be improved**:
- [Suggestion]
- [Suggestion]

**Suggestions for next meeting**:
- [Idea]

---

## Appendix: Architecture Principles (For Reference)

When evaluating decisions, ensure alignment with:

1. **Microservices**: Independent, loosely-coupled services
2. **Multi-Tenant**: Row-level isolation, strict data boundaries
3. **Event-Driven**: Async communication via Kafka, immutable event log
4. **HIPAA-Ready**: Security, audit trail, data protection
5. **Cloud-Native**: Container-based, horizontally scalable
6. **Open Source**: Avoid vendor lock-in, use proven technologies

---

## Appendix: Architecture Decision Log

Current status of all active decisions:

| ADR | Title | Status | Date | Impact |
|-----|-------|--------|------|--------|
| [ADR#] | [Title] | [Status] | [Date] | [Impact] |

[Link to full decision log: docs/architecture/ARCHITECTURE_REVIEW_PROCESS.md#active-decisions]

---

_Minutes recorded by: [Scribe name]_
_Reviewed by: [Architecture Lead name]_
_Date approved: [Date]_
_Next review: [Date]_

---

## How to Use This Template

1. **Copy this template** to `/docs/architecture/meeting-notes/YYYY-MM-DD.md`
2. **Fill in sections** as meeting progresses
3. **Complete decisions** before meeting ends
4. **Share recording link** after meeting
5. **Post summary** in #architecture-discussion within 1 hour
6. **File archive** for record-keeping

---

_Template Version: 1.0_
_Created: January 19, 2026_
_For questions: See MEETING_SCHEDULE.md_
