# ABM Execution Plan — March 12-18, 2026

Purpose: convert the five currently `triggered` accounts into real week-one outbound activity with immediate logging.

This plan is anchored to:
- [Target Tracker](../targets/_tracker.md)
- [Trigger Log](./trigger-log.md)
- [Activation — March 11, 2026](./activation-2026-03-11.md)

## Current State

As of March 12, 2026:

- 5 targets have active triggers: Privia Health, Fallon Health, ChristianaCare, IPHCA, Gundersen/Emplify
- all 5 remain `triggered` in the tracker
- none of the 5 dossiers show a logged T1, T2, or T3 touch
- email sequence assets already exist for the required trigger types

Conclusion: the bottleneck is manual execution, not content creation.

## Operating Rules

### Status Transition Rule

Use the tracker and dossier touch tables as the source of truth for execution.

- Keep status `triggered` until a real T1 touch is sent
- Move status to `in-sequence` only after:
  - T1 date is entered in the target dossier outreach table
  - T1 date and channel are entered in [../targets/_tracker.md](../targets/_tracker.md)
  - `Next Action` is updated to the dated T2 follow-up

Do not mark a target `in-sequence` for research completion alone.

### Logging Rule

After every real touch, update all three places in the same session:

1. target dossier outreach table
2. [../targets/_tracker.md](../targets/_tracker.md)
3. [./trigger-log.md](./trigger-log.md) action text if execution meaning changed

### Cadence Rule

- Maximum 1 email send per target per touch
- Maximum 1-2 thoughtful LinkedIn engagements per day total
- Sequence the five triggered accounts over the week rather than batching all channels at once

## Priority Order

1. Privia Health
2. Fallon Health
3. ChristianaCare
4. IPHCA / CINI
5. Gundersen / Emplify

Rationale:

- Privia and Fallon have the freshest M&A triggers
- ChristianaCare has a high-value technology trigger with a named buyer path
- IPHCA has the clearest greenfield quality-infrastructure case
- Gundersen is strong but less time-sensitive than the first four

## Blocker-Clearing Research

Only do research that is required to send T1 this week.

### Research Required Before T1

| Target | Blocking Question | Completion Standard |
|---|---|---|
| Privia Health | Who owns quality reporting across the ACO portfolio? | 1 named primary contact and 1 backup contact |
| Fallon Health | Is the first buyer Fallon-side or MGB integration-side? | 1 named primary contact and 1 backup contact |
| Gundersen / Emplify | Who owns post-Epic quality standardization? | 1 named primary contact and 1 backup contact |

### Research Already Sufficient To Start

| Target | Primary Contact Path |
|---|---|
| ChristianaCare | Randy Gaboriault or Christine Donohue-Henry |
| IPHCA / CINI | Angela Boyer or Ben Harvey |

## Seven-Day Plan

## Day 1 — Thursday, March 12, 2026

Goal: clear contact blockers for the first two sends and complete first LinkedIn engagement.

- Finalize Privia primary contact and backup
- Finalize Fallon primary contact and backup
- Review recent LinkedIn activity for Privia and Fallon
- Make 1 thoughtful LinkedIn engagement on Privia or Fallon content
- Draft final T1 copy for Privia using `merger-expansion`

End-of-day standard:

- Privia ready to send Friday
- Fallon ready to send Monday

## Day 2 — Friday, March 13, 2026

Goal: send first real T1.

- Send T1 email to Privia Health
- Log T1 immediately in Privia dossier and tracker
- Move Privia from `triggered` to `in-sequence`
- Set Privia `Next Action` to `T2 email due 2026-03-19`
- Make 1 thoughtful LinkedIn engagement for ChristianaCare or Fallon

## Day 3 — Monday, March 16, 2026

Goal: send second T1 and clear the next blocker.

- Send T1 email to Fallon Health
- Log T1 immediately in Fallon dossier and tracker
- Move Fallon from `triggered` to `in-sequence`
- Set Fallon `Next Action` to `T2 email due 2026-03-22`
- Confirm ChristianaCare final contact path: Randy first, Christine backup

## Day 4 — Tuesday, March 17, 2026

Goal: send the technology-trigger outreach.

- Send T1 email to ChristianaCare
- Log T1 immediately in ChristianaCare dossier and tracker
- Move ChristianaCare from `triggered` to `in-sequence`
- Set ChristianaCare `Next Action` to `T2 email due 2026-03-23`
- Make 1 thoughtful LinkedIn engagement on ChristianaCare leadership content

## Day 5 — Wednesday, March 18, 2026

Goal: convert the greenfield FQHC/CIN target.

- Send T1 email to IPHCA / CINI
- Log T1 immediately in IPHCA dossier and tracker
- Move IPHCA from `triggered` to `in-sequence`
- Set IPHCA `Next Action` to `T2 email due 2026-03-24`
- Finalize Gundersen primary contact and backup if still open

## Day 6 — Thursday, March 19, 2026

Goal: send the fifth triggered account and start follow-up rhythm.

- Send T1 email to Gundersen / Emplify
- Log T1 immediately in Gundersen dossier and tracker
- Move Gundersen from `triggered` to `in-sequence`
- Set Gundersen `Next Action` to `T2 email due 2026-03-25`
- If no Privia response, prepare Privia T2

## Day 7 — Friday, March 20, 2026

Goal: close week one with clean records and next-touch readiness.

- Audit all five dossier outreach tables for completeness
- Audit [../targets/_tracker.md](../targets/_tracker.md) for matching T1 dates and channels
- Reconcile [./trigger-log.md](./trigger-log.md) wording so only truly sent outreach is described as initiated
- Prepare Privia T2 for send on March 19 or next business day if not already sent
- Queue Fallon T2 and ChristianaCare T2 drafts

## Per-Target Execution Notes

### Privia Health

- Sequence: `merger-expansion`
- First send target: March 13, 2026
- Primary objective: get into the quality-reporting owner conversation around Evolent integration and APP Plus complexity
- Research blocker: final named quality owner

T1 success condition:

- one personalized email sent to named contact
- dossier T1 row filled
- tracker row shows `T1 Date = 2026-03-13`, `T1 Channel = email`, `Status = in-sequence`

### Fallon Health

- Sequence: `merger-expansion`
- First send target: March 16, 2026
- Primary objective: frame HDIM as integration-planning support, not post-close disruption
- Research blocker: Fallon-side vs MGB-side first buyer

T1 success condition:

- one personalized email sent to named contact
- dossier T1 row filled
- tracker row shows `T1 Date = 2026-03-16`, `T1 Channel = email`, `Status = in-sequence`

### ChristianaCare

- Sequence: `leadership-change` adapted for Epic go-live
- First send target: March 17, 2026
- Primary objective: tie Epic migration to quality-measurement continuity
- Contact path: Randy Gaboriault primary, Christine Donohue-Henry backup

T1 success condition:

- one personalized email sent to Randy or Christine
- dossier T1 row filled
- tracker row shows `T1 Date = 2026-03-17`, `T1 Channel = email`, `Status = in-sequence`

### IPHCA / CINI

- Sequence: `vbc-contract` adapted for greenfield infrastructure build
- First send target: March 18, 2026
- Primary objective: position HDIM as the quality-measurement backbone for the 5-MCO network
- Contact path: Angela Boyer primary, Ben Harvey backup

T1 success condition:

- one personalized email sent to Angela or Ben
- dossier T1 row filled
- tracker row shows `T1 Date = 2026-03-18`, `T1 Channel = email`, `Status = in-sequence`

### Gundersen / Emplify

- Sequence: `merger-expansion`
- First send target: March 19, 2026
- Primary objective: tie unified Epic go-live to post-merger quality normalization
- Research blocker: named quality or strategy owner

T1 success condition:

- one personalized email sent to named contact
- dossier T1 row filled
- tracker row shows `T1 Date = 2026-03-19`, `T1 Channel = email`, `Status = in-sequence`

## Exact Tracker Updates After Each T1 Send

Update the relevant row in [../targets/_tracker.md](../targets/_tracker.md):

- `T1 Date` = actual send date in `YYYY-MM-DD`
- `T1 Channel` = `email`
- `Status` = `in-sequence`
- `Next Action` = `T2 email due YYYY-MM-DD`

Leave `T2 Date`, `T2 Channel`, `T3 Date`, `T3 Channel`, and `Response` blank until they actually happen.

## Exact Dossier Updates After Each T1 Send

In the target dossier `Outreach Status` table:

- fill the `T1` row with actual date
- set `Channel` to `Email`
- summarize the angle in `Content`
- leave `Response` blank unless a reply exists

Example content summary:

- `M&A integration pain + APP Plus reporting complexity`
- `Epic go-live quality continuity angle`
- `Greenfield CIN infrastructure build across 5 MCOs`

## Week-One Success Criteria

- 5 of 5 triggered targets have either:
  - a logged T1 send, or
  - a documented blocker with named next research action
- tracker status reflects real execution, not intent
- no dossier has a blank outreach table once T1 is sent
- Privia, Fallon, and ChristianaCare are all in `in-sequence` by March 17, 2026

## Not In Scope This Week

- writing new sequence templates
- building automation for LinkedIn or email sending
- adding new targets
- re-scoring the full list unless new intelligence materially changes priority
