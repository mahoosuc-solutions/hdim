# ServiceHive.ai — Design Document

**Date:** March 3, 2026
**Author:** Aaron (Mahoosuc Solutions)
**Status:** Approved

---

## Overview

ServiceHive.ai is a multi-product GTM portfolio manager built on AgentMesh. It is the first public reference implementation of the AgentMesh platform, proving that event-driven AI agents can manage complex, multi-channel business operations.

Four AI agents coordinate across a product portfolio (HDIM, AgentMesh, landing page, investor pipeline). They draft LinkedIn content, compose outreach emails, track engagement metrics, and generate cross-product status reports. The user approves agent work from a dashboard. Agents execute.

### Three Purposes

1. **Operational need** — managing GTM across 4+ products manually doesn't scale
2. **AgentMesh showcase** — proves the platform works for a real, non-healthcare use case
3. **Open-source reference app** — ships alongside AgentMesh, showing developers how to build multi-agent apps

### Architecture Dependency

```
AgentMesh v0.1 "Spark" (Event Core + basic Agent Runtime)
    └── ServiceHive.ai v0.1 (4 agents + dashboard + scheduler)
```

ServiceHive ships when AgentMesh v0.1 ships. Developed in parallel.

---

## Agent Definitions

Four agents, each a first-class AgentMesh participant with typed event subscriptions and productions.

### Agent 1: LinkedIn Content Agent

**Purpose:** Manages the full LinkedIn content lifecycle — drafts, schedules, publishes, and tracks posts.

**Subscribes to:** `ContentCalendarDue`, `PostApproved`, `PostScheduled`, `EngagementThresholdReached`
**Produces:** `PostDrafted`, `PostScheduled`, `PostPublished`, `PostFailed`, `CommentSuggested`

**Capabilities:**
- Drafts posts using thought-leadership voice guidelines
- Pulls from content calendar to know what's due
- Schedules via Node.js cron service (calls LinkedIn Posts API at the target time)
- After publishing, monitors early engagement and suggests hashtag comments
- Scans Tier 2 target profiles and suggests comments from the comment bank, adapted to their actual recent post

**Tools:** `LinkedInPostsAPI`, `ContentCalendarRead`, `CommentBankSearch`, `ProfileScraper`

### Agent 2: Email Outreach Agent

**Purpose:** Drafts and manages outreach sequences for pilot prospects, investors, and partners.

**Subscribes to:** `OutreachSequenceDue`, `EmailDraftApproved`, `EmailBounced`, `ReplyReceived`
**Produces:** `EmailDrafted`, `EmailSent`, `FollowUpScheduled`, `SequenceCompleted`, `WarmLeadIdentified`

**Capabilities:**
- Drafts personalized emails based on prospect profile and product context
- Manages multi-step sequences (intro → follow-up → last touch)
- Tracks opens/replies via webhook from email provider
- Suggests follow-up timing based on engagement signals
- Flags warm leads (opened 2+ times, clicked link, replied)

**Tools:** `EmailSend` (SMTP/API), `ProspectProfileRead`, `SequenceManager`, `EngagementWebhook`

### Agent 3: Engagement Tracker Agent

**Purpose:** Monitors engagement metrics across LinkedIn and email, surfaces trends, and suggests next actions.

**Subscribes to:** `PostPublished`, `EmailSent`, `DailyMetricsPoll`, `WeeklyReportDue`
**Produces:** `MetricsUpdated`, `TrendDetected`, `ActionSuggested`, `WeeklyReportGenerated`

**Capabilities:**
- Polls LinkedIn analytics (profile views, post impressions, connection requests)
- Aggregates email engagement (open rates, reply rates, sequence completion)
- Detects trends (e.g., "Profile views up 3x after Post #2, driven by HEDIS group shares")
- Suggests next actions ("Courtney Breece liked your comment — send DM with post link")
- Generates weekly engagement report for the dashboard

**Tools:** `LinkedInAnalytics`, `EmailMetricsRead`, `TrendAnalyzer`, `ReportGenerator`

### Agent 4: Portfolio Coordinator Agent

**Purpose:** Cross-product awareness and strategic coordination. The "chief of staff" agent.

**Subscribes to:** `PostPublished`, `EmailSent`, `MetricsUpdated`, `MilestoneCompleted`, `WeeklyReportDue`
**Produces:** `WeeklyStatusReport`, `ConflictDetected`, `PriorityRecommendation`, `MilestoneReminder`

**Capabilities:**
- Knows the state of all products: HDIM (dev status, test results), AgentMesh (extraction progress), landing page (traffic), investor pipeline (stage, last touch), customer pilots (status)
- Generates weekly portfolio status report
- Detects conflicts ("You have an investor demo Tuesday but Post #3 draft isn't approved yet")
- Recommends priority shifts based on engagement signals and deadlines
- Tracks milestones and sends reminders

**Tools:** `GitStatusRead`, `ProjectConfigRead`, `CalendarRead`, `MilestoneTracker`

### Agent Coordination Pattern

Agents communicate through events, not direct calls. Example flow:

```
ContentCalendarDue (daily check)
  → LinkedIn Agent drafts post → PostDrafted
    → Dashboard shows draft for approval
      → User approves → PostApproved
        → LinkedIn Agent schedules → PostScheduled
          → Cron fires at target time → PostPublished
            → Engagement Tracker starts monitoring → MetricsUpdated
              → Portfolio Coordinator includes in weekly report
```

---

## System Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   ServiceHive Dashboard                      │
│  Product Overview │ Agent Queue │ Calendar │ Metrics │ Reports│
├──────────────────────────────┬──────────────────────────────┤
│       WebSocket Event Bridge │  REST API (approval, config) │
├──────────────────────────────┴──────────────────────────────┤
│                    AgentMesh Runtime                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐   │
│  │ LinkedIn  │ │  Email   │ │Engagement│ │  Portfolio    │   │
│  │  Agent    │ │  Agent   │ │ Tracker  │ │ Coordinator  │   │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └──────┬───────┘   │
│       │             │            │               │           │
│  ┌────┴─────────────┴────────────┴───────────────┴────┐     │
│  │              AgentMesh Event Core                    │     │
│  │  Event Store │ Router │ Projections │ DLQ │ Replay  │     │
│  └─────────────────────┬──────────────────────────────┘     │
├─────────────────────────┼───────────────────────────────────┤
│                   Infrastructure                             │
│  PostgreSQL (events + projections)  │  Redis (agent memory) │
│  Node.js Cron Scheduler            │  SMTP/Email API        │
│  LinkedIn Posts API                 │  Docker Compose        │
└─────────────────────────────────────────────────────────────┘
```

### Key Design Decisions

**No Kafka.** AgentMesh v0.1 uses PostgreSQL as both event store and transport (LISTEN/NOTIFY for real-time, polling for replay). Kafka is overkill for a single-user GTM tool. This is a deliberate simplification from HDIM — one of the things AgentMesh proves is that event sourcing doesn't require Kafka to start.

**Single database.** One PostgreSQL instance with schemas:
- `events` — append-only event store (all agent events)
- `projections` — read models (content calendar, metrics, pipeline state)
- `config` — product definitions, prospect lists, comment banks, sequence templates

**Node.js throughout.** Dashboard (React), agents (TypeScript), cron scheduler (Node.js), CLI — all JavaScript/TypeScript. No Java. This is the AgentMesh TypeScript stack.

**Human-in-the-loop by default.** Every agent action that's externally visible (publish post, send email, DM someone) requires approval. Internal actions (draft content, update metrics, generate report) execute autonomously. The approval queue is the primary interaction point on the dashboard.

### Data Flow: Post Lifecycle

```
1. Cron: daily 6am check → emit ContentCalendarDue
2. LinkedIn Agent: receives event, checks calendar projection
   → finds Post #4 due today
   → drafts post using LLM + voice guidelines + dedup check
   → emit PostDrafted { content, wordCount, hashtags, targetTime }
3. Event Store: persists PostDrafted
4. Dashboard projection: updates approval queue (new item)
5. WebSocket: pushes notification to dashboard
6. User: reviews draft, clicks Approve
   → REST API emits PostApproved { postId, scheduledTime: "7:30 AM ET" }
7. LinkedIn Agent: receives PostApproved
   → registers job in cron scheduler for 7:30 AM
   → emit PostScheduled { postId, scheduledTime }
8. Cron: fires at 7:30 AM
   → calls LinkedIn Posts API (/rest/posts)
   → on 201: emit PostPublished { postId, urn, url }
   → on error: emit PostFailed { postId, error, retryAt }
9. Engagement Tracker: receives PostPublished
   → starts polling LinkedIn analytics every 2 hours
   → emit MetricsUpdated { postId, impressions, engagement }
```

### Cron Scheduler

Lightweight Node.js service using BullMQ + Redis for reliable delayed job execution:

```typescript
// Scheduler subscribes to PostScheduled events
// Registers a one-time job at the target time
scheduler.on('PostScheduled', async (event) => {
  await queue.add('publish-post', {
    postId: event.postId,
    content: event.content,
  }, {
    delay: event.scheduledTime - Date.now(),
  });
});

// Worker processes the job at scheduled time
worker.on('publish-post', async (job) => {
  const result = await linkedInClient.publish(job.data.content);
  await eventStore.emit(result.success
    ? { type: 'PostPublished', urn: result.urn }
    : { type: 'PostFailed', error: result.error });
});
```

If the process restarts, pending jobs survive in Redis.

### Dashboard

React SPA with four views:

| View | Purpose |
|------|---------|
| **Overview** | Product cards (HDIM, AgentMesh, landing page, investors, pilots) with status badges and key metrics |
| **Approval Queue** | Pending agent actions — post drafts, email drafts, suggested comments. Approve/edit/reject each |
| **Content Calendar** | Week/month view of scheduled posts, emails, engagement milestones across all products |
| **Metrics** | LinkedIn analytics (impressions, profile views, connections), email metrics (opens, replies), trend charts |

Real-time updates via WebSocket event bridge.

---

## Data Model

### Event Schema

All events follow a standard AgentMesh envelope:

```typescript
interface AgentMeshEvent {
  id: string;                    // UUID
  type: string;                  // e.g. "PostDrafted"
  aggregateId: string;           // e.g. post ID, sequence ID
  aggregateType: string;         // e.g. "LinkedInPost", "EmailSequence"
  producedBy: string;            // agent name or "user"
  timestamp: string;             // ISO 8601
  version: number;               // event version for schema evolution
  payload: Record<string, any>;  // event-specific data
  metadata: {
    correlationId: string;       // traces related events across agents
    tenantId?: string;           // for future multi-user support
  };
}
```

### Event Types

| Agent | Event | Payload |
|-------|-------|---------|
| LinkedIn | `PostDrafted` | `{ content, wordCount, hashtags, targetDate, mode }` |
| LinkedIn | `PostApproved` | `{ scheduledTime }` |
| LinkedIn | `PostPublished` | `{ urn, url }` |
| LinkedIn | `PostFailed` | `{ error, retryAt }` |
| LinkedIn | `CommentSuggested` | `{ targetPerson, targetPostUrl, commentText, rationale }` |
| Email | `EmailDrafted` | `{ to, subject, body, sequenceId, stepNumber }` |
| Email | `EmailSent` | `{ messageId, provider }` |
| Email | `ReplyReceived` | `{ from, subject, sentiment }` |
| Email | `WarmLeadIdentified` | `{ prospectId, signals[] }` |
| Tracker | `MetricsUpdated` | `{ source, metrics{} }` |
| Tracker | `TrendDetected` | `{ metric, direction, magnitude, insight }` |
| Tracker | `ActionSuggested` | `{ action, target, rationale, priority }` |
| Coordinator | `WeeklyStatusReport` | `{ products[], highlights[], risks[], nextWeek[] }` |
| Coordinator | `ConflictDetected` | `{ items[], recommendation }` |
| System | `ContentCalendarDue` | `{ date, dueItems[] }` |
| System | `DailyMetricsPoll` | `{ date }` |
| System | `WeeklyReportDue` | `{ weekOf }` |

### Projections (Read Models)

| Projection | Source Events | Purpose |
|------------|--------------|---------|
| `content_calendar` | PostDrafted, PostApproved, PostScheduled, PostPublished | Calendar view of all content |
| `approval_queue` | PostDrafted, EmailDrafted, CommentSuggested | Pending items for user review |
| `linkedin_metrics` | MetricsUpdated, PostPublished | Post performance over time |
| `email_pipeline` | EmailDrafted, EmailSent, ReplyReceived, WarmLeadIdentified | Outreach sequence status |
| `product_status` | WeeklyStatusReport, MilestoneCompleted | Portfolio overview cards |
| `engagement_timeline` | All events | Chronological activity feed |

### Configuration Tables (not event-sourced)

```
products           — { id, name, description, status, repoUrl, dashboardUrl }
prospects          — { id, name, title, company, linkedinUrl, email, tier, notes }
comment_bank       — { id, targetPersonId, topic, commentText, lastUsedAt }
sequence_templates — { id, name, steps[], defaultDelays[] }
voice_guidelines   — { id, mode, rules, examples }
agent_config       — { agentName, llmProvider, model, temperature, maxTokens }
```

---

## Repository Structure

```
servicehive/
├── packages/
│   ├── dashboard/                    # React SPA
│   │   ├── src/
│   │   │   ├── views/
│   │   │   │   ├── Overview.tsx
│   │   │   │   ├── ApprovalQueue.tsx
│   │   │   │   ├── ContentCalendar.tsx
│   │   │   │   └── Metrics.tsx
│   │   │   ├── components/
│   │   │   ├── hooks/
│   │   │   └── services/
│   │   │       └── eventBridge.ts     # WebSocket client
│   │   └── package.json
│   ├── agents/
│   │   ├── linkedin-agent/            # LinkedIn content agent
│   │   │   ├── src/
│   │   │   │   ├── agent.ts           # defineAgent() config
│   │   │   │   ├── handlers/
│   │   │   │   │   ├── draftPost.ts
│   │   │   │   │   ├── schedulePost.ts
│   │   │   │   │   └── suggestComment.ts
│   │   │   │   └── tools/
│   │   │   │       ├── linkedinApi.ts
│   │   │   │       └── profileScraper.ts
│   │   │   └── package.json
│   │   ├── email-agent/               # Email outreach agent
│   │   │   ├── src/
│   │   │   │   ├── agent.ts
│   │   │   │   ├── handlers/
│   │   │   │   │   ├── draftEmail.ts
│   │   │   │   │   ├── sendEmail.ts
│   │   │   │   │   └── processReply.ts
│   │   │   │   └── tools/
│   │   │   │       └── emailClient.ts
│   │   │   └── package.json
│   │   ├── engagement-tracker/        # Metrics & trends agent
│   │   │   ├── src/
│   │   │   │   ├── agent.ts
│   │   │   │   ├── handlers/
│   │   │   │   │   ├── pollMetrics.ts
│   │   │   │   │   ├── detectTrends.ts
│   │   │   │   │   └── suggestAction.ts
│   │   │   │   └── tools/
│   │   │   │       └── linkedinAnalytics.ts
│   │   │   └── package.json
│   │   └── portfolio-coordinator/     # Cross-product agent
│   │       ├── src/
│   │       │   ├── agent.ts
│   │       │   ├── handlers/
│   │       │   │   ├── generateReport.ts
│   │       │   │   ├── detectConflicts.ts
│   │       │   │   └── trackMilestones.ts
│   │       │   └── tools/
│   │       │       └── gitStatus.ts
│   │       └── package.json
│   ├── scheduler/                     # Node.js cron + BullMQ
│   │   ├── src/
│   │   │   ├── scheduler.ts
│   │   │   ├── workers/
│   │   │   │   ├── publishPost.ts
│   │   │   │   └── sendEmail.ts
│   │   │   └── queue.ts
│   │   └── package.json
│   └── server/                        # API server + event bridge
│       ├── src/
│       │   ├── api/
│       │   │   ├── approval.ts
│       │   │   ├── calendar.ts
│       │   │   ├── config.ts
│       │   │   └── metrics.ts
│       │   ├── eventBridge.ts         # WebSocket server
│       │   ├── projections/           # Event → read model updaters
│       │   └── seed/                  # Import existing content pipeline
│       │       ├── importCommentBank.ts
│       │       ├── importCalendar.ts
│       │       └── importProspects.ts
│       └── package.json
├── docker/
│   ├── docker-compose.yml             # Postgres + Redis + all services
│   └── init.sql                       # Schema creation
├── seed-data/                         # Existing content pipeline
│   ├── comment-bank.json              # From tier2-comment-bank.md
│   ├── content-calendar.json          # From content-calendar.md
│   ├── prospects.json                 # From distribution doc
│   └── products.json                  # HDIM, AgentMesh, landing page, etc.
├── nx.json
├── package.json
├── tsconfig.base.json
├── .env.example
├── LICENSE                            # Apache 2.0
└── README.md
```

### Seed Data Migration

Existing content pipeline (`docs/outreach/`) becomes initial seed data:

| Source File | Imports Into |
|---|---|
| `content-calendar.md` | `content_calendar` projection + scheduled events |
| `tier2-comment-bank.md` | `comment_bank` table (42 comments, 18 people) |
| `hedis-post-distribution.md` | `prospects` table (25+ people with tiers) |
| `stars-post-draft.md` | `PostDrafted` event (approved, scheduled Thu Mar 6) |
| `mips-post-draft.md` | `PostDrafted` event (draft, pending approval) |
| `product-intro-draft.md` | `PostDrafted` event (draft, held for Week 3) |
| `linkedin-posts.md` | `PostPublished` events (historical, Post #1) |

---

## Tech Stack

| Component | Technology | Rationale |
|-----------|-----------|-----------|
| Monorepo | Nx | Task caching, dependency graph, affected commands |
| Language | TypeScript throughout | AgentMesh TypeScript stack |
| Dashboard | React + Vite | Fast builds, largest ecosystem |
| Agents | AgentMesh `defineAgent()` API | First-party showcase |
| Event Store | PostgreSQL + `@agentmesh/event-store` | LISTEN/NOTIFY for real-time, polling for replay |
| Job Queue | BullMQ + Redis | Reliable delayed job execution, survives restarts |
| LLM | Claude via `@agentmesh/runtime` adapter | Best reasoning for content generation |
| LinkedIn API | Posts API (`/rest/posts`) | `LinkedIn-Version: 202401` header required |
| Email | Resend or SendGrid API | Transactional email with open/click tracking |
| Deployment | Docker Compose (local) → VPS (production) | Simple, no Kubernetes needed |
| Real-time | WebSocket | Event bridge to dashboard |

---

## Release Plan

### v0.1 "Hive Mind" (Weeks 2-4) — MVP

**Ships:** LinkedIn Agent + Dashboard (approval queue + calendar) + Scheduler + seed import

**What works:**
- Import existing content pipeline as seed data
- LinkedIn Agent drafts posts from content calendar
- Approval queue shows drafts, user approves/edits/rejects
- Scheduler publishes at target time via Posts API
- Comment suggestions for Tier 2 targets
- Basic metrics display (manual entry initially)

**What doesn't yet:**
- Email agent, engagement tracker, portfolio coordinator
- Real-time LinkedIn analytics polling

### v0.2 "Swarm" (Weeks 5-7)

**Adds:** Email Agent + Engagement Tracker

**What works:**
- Email drafting and sequence management
- Automated LinkedIn analytics polling
- Trend detection ("Post #2 outperformed Post #1 by 3x — Stars angle resonated")
- Action suggestions in approval queue
- Metrics dashboard with charts

### v0.3 "Colony" (Weeks 8-10)

**Adds:** Portfolio Coordinator + full dashboard

**What works:**
- Cross-product status reports
- Conflict detection
- Milestone tracking
- Weekly generated reports
- All four agents coordinating through events

### Success Metrics

| Phase | Metric | Target |
|-------|--------|--------|
| v0.1 | Posts published via ServiceHive | 4+ (Week 2-4 calendar items) |
| v0.1 | Time from draft to publish | <5 min (vs manual copy-paste) |
| v0.2 | Email sequences running | 2+ active sequences |
| v0.2 | Automated engagement insights | 3+ actionable suggestions/week |
| v0.3 | Weekly report generation | Fully automated, <30s |
| v0.3 | GitHub stars (as AgentMesh showcase) | Referenced in AgentMesh README |

---

## Competitive Positioning

ServiceHive.ai demonstrates AgentMesh's value proposition by contrast:

| Capability | ServiceHive (AgentMesh) | Buffer/Hootsuite | HubSpot | Manual (current) |
|---|---|---|---|---|
| Multi-product portfolio | Native | No | Partial | Spreadsheets |
| AI agent drafting | LLM-powered, voice-aware | Template-based | AI assist | Manual writing |
| Event-sourced audit trail | Complete history, replayable | None | Activity log | None |
| LinkedIn personal profile scheduling | BullMQ cron (API limitation workaround) | Paid feature | No | Manual |
| Cross-channel coordination | Agent events | Separate tools | Integrated but rigid | Mental model |
| Custom agent logic | `defineAgent()` — unlimited | None | Workflows | N/A |
| Self-hosted / data ownership | Full control | SaaS only | SaaS only | Local files |
| Cost | Infrastructure only (~$20/mo VPS) | $15-100/mo | $800+/mo | Time |

---

## LinkedIn API Constraints

The LinkedIn Posts API has specific limitations that shape the architecture:

- **No scheduled publishing for personal profiles** — the `scheduledPublishAt` field does not exist. Organization pages support it, personal profiles do not. This is why ServiceHive needs its own scheduler (BullMQ).
- **Rate limits:** 100 API calls per day per member for posting. More than sufficient for 2 posts/week.
- **Analytics access:** Requires `r_organization_social` for org pages. Personal profile analytics are limited to what's available through the profile API. Engagement tracking will supplement with polling.
- **Token refresh:** LinkedIn access tokens expire every 60 days. Refresh tokens last 365 days. The scheduler must handle token refresh automatically.

---

## References

- [AgentMesh Design Document](./2026-03-03-agentmesh-design.md) — Platform architecture
- [LinkedIn Content Pipeline](../outreach/2026-03-03-content-calendar.md) — 4-week content calendar (seed data source)
- [Tier 2 Comment Bank](../outreach/2026-03-03-tier2-comment-bank.md) — 42 comments, 18 people (seed data source)
- [Distribution Plan](../outreach/2026-03-03-hedis-post-distribution.md) — Prospects and engagement targets
- [LinkedIn Posts API](https://learn.microsoft.com/en-us/linkedin/marketing/community-management/shares/posts-api) — Microsoft Learn documentation
- [HDIM Agent Runtime](../../backend/modules/services/agent-runtime-service/) — Existing agent infrastructure (extraction source for AgentMesh)
