# ADR-012: Human-in-the-Loop LinkedIn Integration

**Status:** Accepted
**Date:** 2026-03-02
**Deciders:** Aaron (founder)

---

## Context

HDIM needs a sustainable content distribution workflow for LinkedIn — the primary channel for reaching healthcare payers, ACOs, and clinical quality leaders. Manual posting is inconsistent; fully automated posting is reputationally risky for a pre-revenue startup. A middle path is needed: AI drafts, human gates, then publishes.

Two sub-problems to solve:

1. **Outbound:** How do we move from "git tag" or "topic idea" to a published LinkedIn post without losing hours to writer's block or formatting?
2. **Inbound:** How do we track engagement in a way that's useful without building a full analytics pipeline Day 1?

---

## Decision

Implement a **Claude Code command** (`/social:linkedin`) that:
- Accepts `--topic` (primary) and optional `--mode` and `--tag` arguments
- Reads recent post history to avoid thematic repetition
- Drafts one post per invocation using mode-specific prompt rules
- Presents the draft in terminal with `[A]pprove / [E]dit / [R]egenerate / [X]Reject`
- On approval: publishes via LinkedIn UGC Posts API using locally-stored OAuth tokens
- Logs each published post to `docs/outreach/linkedin-posts.md`

OAuth lifecycle is managed by `scripts/linkedin-auth.sh` (initial setup + refresh).

---

## Alternatives Considered

### Alternative 1: Standalone Python service with scheduled posting
**Rejected because:**
- Requires new infra (process manager, cron, environment isolation)
- Removes human gate — too risky for founder-voice content pre-PMF
- Adds operational burden before revenue

### Alternative 2: Zapier/Buffer integration with manual triggering
**Rejected because:**
- No AI drafting — still requires writer to produce content
- External SaaS dependency with cost and data-sharing implications
- Approval loop harder to implement in third-party tools

### Alternative 3: Claude Code command (chosen)
**Accepted because:**
- Zero new infrastructure
- Stays in existing developer workflow (terminal, git context)
- Human always gates publication — no reputational risk
- Can borrow generation patterns from Mahoosuc OS `/social/generate` command
- Audit trail via `docs/outreach/linkedin-posts.md` in source control

---

## Design Details

### `--mode` parameter

Three modes with distinct voice and structural rules:

| Mode | Voice | Key Constraint |
|------|-------|----------------|
| `thought-leadership` | Practitioner insight | No HDIM mentions, no pitch |
| `product-announce` | Founder, proof points | Technical facts, no buzzwords |
| `customer-milestone` | Relationship-aware | No customer name without explicit permission |

Default: `thought-leadership` (lowest risk, most shareable).

### Token lifecycle

LinkedIn access tokens expire every 60 days. Refresh tokens expire after 365 days.

- `scripts/linkedin-auth.sh` handles initial OAuth code flow and stores credentials in `.env.linkedin`
- `/social:linkedin` checks expiry before every publish attempt
- Auto-refresh fires if access token is expired but refresh token is valid
- Graceful exit with setup instructions if both are expired (no silent failure)

### Phase 1 / Phase 2 boundary

**Phase 1 (this ADR):** Manual engagement tracking. The `docs/outreach/linkedin-posts.md` table has Impressions and Comments columns filled manually after checking LinkedIn analytics.

**Phase 2 (future ADR):** `scripts/linkedin-engagement-sync.sh` — nightly cron pulling impressions and comments via LinkedIn Analytics API. This is explicitly deferred. The tracking table schema is forward-compatible.

---

## Consequences

### Positive
- LinkedIn posting goes from "friction-heavy manual task" to "15-second terminal workflow"
- Drafts read recent post history — reduces repetitive content without manual tracking
- Mode system enforces voice discipline (thought leadership ≠ product announcements)
- Full audit trail in source control (`docs/outreach/linkedin-posts.md`)
- Token lifecycle is explicit and documented — no surprise expiry failures

### Negative / Risks
- LinkedIn's API terms require app review for `w_member_social` scope beyond personal use
- OAuth tokens stored in `.env.linkedin` — must remain gitignored (enforced)
- If LinkedIn changes UGC Posts API (v2), command needs update
- Phase 2 engagement sync will require LinkedIn Analytics API access (separate approval)

### Mitigations
- `.env.linkedin` in `.gitignore` (committed in this ADR's PR)
- `scripts/linkedin-auth.sh --status` provides clear token health visibility
- Command fails loudly on API errors — no silent data corruption

---

## Mahoosuc OS Pattern Reuse

This implementation borrows conceptually from Mahoosuc OS:
- Generation patterns: `/social/generate` command (voice rules, platform-specific formatting)
- Approval state machine: `marketing-intelligence-engine-approval-workflow-api.md` (pending → approved/rejected)
- Scheduling patterns: `/social/schedule` command (referenced for Phase 2 design)

No code is shared (different repositories), but the design philosophy is consistent across both systems.

---

## References

- `scripts/linkedin-auth.sh` — OAuth setup and refresh
- `.claude/commands/social/linkedin.md` — Command implementation
- `docs/outreach/linkedin-posts.md` — Post tracking log
- LinkedIn UGC Posts API: https://learn.microsoft.com/en-us/linkedin/marketing/integrations/community-management/shares/ugc-post-api
- LinkedIn OAuth 2.0: https://learn.microsoft.com/en-us/linkedin/shared/authentication/authorization-code-flow
