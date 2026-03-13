# UI Experience Design — Operational Views for Pilot Delivery

**Date:** March 13, 2026
**Author:** Aaron + Claude Code
**Status:** Design Complete — Ready for Implementation
**Context:** Post-validation of star-ratings backend (49/49 tests, seed script ready). Five development tasks require UI/UX design before implementation.

---

## Table of Contents

1. [Star-Ratings Angular UI Component](#1-star-ratings-angular-ui-component)
2. [Docker Stack Smoke Test with Seed Data](#2-docker-stack-smoke-test-with-seed-data)
3. [Houston Response Package Finalization](#3-houston-response-package-finalization)
4. [LinkedIn Content Pipeline](#4-linkedin-content-pipeline)
5. [Custom Domain (healthdatainmotion.com)](#5-custom-domain-healthdatainmotioncom)
6. [Implementation Priority Matrix](#6-implementation-priority-matrix)
7. [LinkedIn Content Update](#7-linkedin-content-update)

---

## 1. Star-Ratings Angular UI Component

### 1.1 User Types & Personas

| Persona | Role | Primary Goal | Key Decisions |
|---------|------|-------------|---------------|
| **Sarah Chen** — Quality Director | ADMIN | Monitor overall star rating trajectory, report to board | Budget allocation, staffing, strategic direction |
| **Marcus Williams** — Care Manager | EVALUATOR | Identify which gaps to close for maximum star impact | Daily outreach prioritization, care coordination |
| **Priya Patel** — Quality Analyst | ANALYST | Build reports, track domain trends, forecast ratings | Data exports, measure benchmarking, simulation modeling |
| **David Kim** — System Administrator | ADMIN | Ensure data freshness, manage tenant configurations | Seed data, recalculation triggers, access control |

### 1.2 User Stories

#### Epic 1: Star Rating Dashboard (Quality Director)

**US-1.1** As a Quality Director, I want to see our current overall star rating prominently displayed so I can assess our plan's competitive position at a glance.

**Acceptance Criteria:**
- Large gauge visualization showing overall rating (0.0–5.0) with half-star precision
- Color coding: Red (< 3.0), Yellow (3.0–3.49), Green (3.5–3.99), Gold (4.0+)
- Quality Bonus indicator (>= 4.0 stars = 5% revenue bonus)
- Last calculated timestamp with freshness indicator
- Rounded rating shown alongside precise rating

**US-1.2** As a Quality Director, I want to see domain-level breakdowns so I can identify which clinical areas are dragging down our overall rating.

**Acceptance Criteria:**
- 7 domain cards: Staying Healthy, Managing Chronic Conditions, Member Experience, Complaints & Performance, Drug Plan, Drug Safety, (Overall weighted)
- Each domain shows: domain stars, measure count, average performance rate
- Sort by lowest-performing domain (improvement opportunity first)
- Click-through to domain detail with constituent measures

**US-1.3** As a Quality Director, I want to see a 12-week star rating trend so I can verify our quality improvement initiatives are working.

**Acceptance Criteria:**
- Line chart with weekly data points (default 12 weeks)
- Granularity toggle: WEEKLY / MONTHLY
- Configurable time window: 4, 8, 12, 26, 52 weeks
- Annotation markers for key events (e.g., "Outreach campaign launched")
- Quality Bonus threshold line at 4.0
- Trend arrow with delta from period start

#### Epic 2: Measure-Level Performance (Quality Analyst)

**US-2.1** As a Quality Analyst, I want to see per-measure star performance rates so I can identify which specific HEDIS measures need intervention.

**Acceptance Criteria:**
- Data table with columns: Measure Code, Measure Name, Domain, Numerator, Denominator, Performance Rate, Stars (1-5), Gap Count
- Sortable by any column
- Color-coded performance rate cells (red/yellow/green relative to CMS cut points)
- Filter by domain
- Search by measure code or name
- Export to CSV

**US-2.2** As a Quality Analyst, I want to compare our performance rates against CMS cut points so I can see exactly how close we are to the next star level.

**Acceptance Criteria:**
- Per-measure: show current rate, cut point for current star, cut point for next star
- "Gap to next star" calculation: how many additional closures needed
- Highlight measures where small improvements yield star increases
- Sorted by "lowest effort to next star" (highest ROI)

#### Epic 3: What-If Simulation (Care Manager + Quality Director)

**US-3.1** As a Care Manager, I want to simulate the effect of closing specific care gaps so I can prioritize my outreach efforts for maximum star impact.

**Acceptance Criteria:**
- Select measure(s) from a dropdown
- Enter number of additional gap closures per measure
- "Simulate" button calls POST /simulate endpoint
- Side-by-side comparison: current vs. simulated rating
- Delta visualization: +0.25 stars, moved from 3.5 → 3.75
- Impact on quality bonus eligibility highlighted
- Multiple scenarios can be compared

**US-3.2** As a Quality Director, I want to see the top 5 highest-impact gap closure opportunities so I can direct resources to the measures that move the needle most.

**Acceptance Criteria:**
- Auto-calculated recommendations: "Close 15 more COL gaps to move from 3 → 4 stars"
- Ranked by ROI (fewest closures needed per star improvement)
- Linked to care-gap manager for action
- Refreshes on each recalculation

#### Epic 4: Multi-Tenant Isolation (System Administrator)

**US-4.1** As a System Administrator, I want tenant-isolated star ratings so each health plan sees only their own data.

**Acceptance Criteria:**
- All API calls include X-Tenant-ID header (already enforced)
- Tenant name shown in page header
- No cross-tenant data leakage (backend-enforced, UI reflects)

### 1.3 User Journeys

#### Journey 1: Morning Star Rating Check (Quality Director, 2 min)

```
Login → Dashboard (sees overall compliance stat-card with star mention)
  → Click "Star Ratings" nav item
  → Star Rating Dashboard loads
  → Scan: Overall = 3.75 ★ (up from 3.5 last month), Quality Bonus = YES
  → Glance at domain breakdown → "Drug Safety" is weakest (2.5 ★)
  → Glance at 12-week trend → consistent upward trajectory
  → Satisfied, moves to next task
```

#### Journey 2: Outreach Prioritization (Care Manager, 5 min)

```
Login → Star Ratings → Measures tab
  → Sort by "Gap to Next Star" ascending
  → See: COL needs 12 more closures to jump from 3★ to 4★
  → See: CBP needs 8 more closures for same jump
  → Click "Simulate" → Enter COL:12, CBP:8
  → Result: Overall jumps from 3.75 → 4.1 ★ (+0.35), Quality Bonus stays YES
  → Click "View COL Gaps" → navigates to Care Gap Manager filtered by COL
  → Begins outreach workflow
```

#### Journey 3: Board Report Preparation (Quality Analyst, 10 min)

```
Login → Star Ratings → Trend tab
  → Set granularity: MONTHLY, window: 52 weeks
  → Screenshot chart for board deck
  → Switch to Measures tab → Export CSV for detailed appendix
  → Run 3 simulation scenarios:
    1. "Status quo" (no additional closures)
    2. "Moderate effort" (25 closures across 5 measures)
    3. "Aggressive" (50 closures across 8 measures)
  → Document: Scenario 2 reaches 4.0★ (Quality Bonus), Scenario 3 reaches 4.3★
  → Export simulation results
```

#### Journey 4: Demo Walkthrough (Sales Engineer, 5 min)

```
Navigate to /star-ratings → Sees pre-seeded demo data
  → "Here's your overall star rating with real-time quality bonus tracking"
  → Show domain breakdown → "Identify exactly where to focus"
  → Click trend → "12 weeks of improvement data, weekly and monthly"
  → Open simulator → "What if you close 20 COL gaps?"
  → Simulated result shows 0.5 star improvement
  → "This is the kind of actionable intelligence that drives contract negotiations"
```

### 1.4 Component Architecture

```
star-ratings/
├── star-ratings.component.ts          ← Main page (route: /star-ratings)
├── star-ratings.component.html
├── star-ratings.component.scss
├── star-ratings.service.ts            ← HTTP client for /api/v1/star-ratings/*
├── star-ratings.model.ts              ← TypeScript interfaces mirroring DTOs
├── components/
│   ├── star-gauge/                    ← Large circular gauge (0-5 stars)
│   │   ├── star-gauge.component.ts
│   │   ├── star-gauge.component.html
│   │   └── star-gauge.component.scss
│   ├── domain-breakdown/              ← 7 domain summary cards
│   │   ├── domain-breakdown.component.ts
│   │   └── domain-breakdown.component.html
│   ├── measure-table/                 ← Sortable measure performance table
│   │   ├── measure-table.component.ts
│   │   └── measure-table.component.html
│   ├── trend-chart/                   ← Line chart with Chart.js
│   │   ├── trend-chart.component.ts
│   │   └── trend-chart.component.html
│   └── simulation-panel/             ← What-if simulator
│       ├── simulation-panel.component.ts
│       └── simulation-panel.component.html
└── star-ratings.module.ts             ← (standalone components, no module needed)
```

### 1.5 Integration with Existing Infrastructure

**Route Registration** (`app.routes.ts`):
```typescript
{
  path: 'star-ratings',
  loadComponent: () =>
    import('./pages/star-ratings/star-ratings.component').then(
      (m) => m.StarRatingsComponent
    ),
  canActivate: [AuthGuard],
  data: { permissions: ['VIEW_EVALUATIONS'] },
},
```

**Navigation** — Add to sidebar after "Care Gaps":
```
Dashboard → Patients → Quality Measures → Care Gaps → ★ Star Ratings → Reports
```

**Shared Components Reused:**
- `StatCardComponent` — for overall rating, quality bonus, gap counts
- `chart-line` — for 12-week trend (Chart.js already installed)
- `chart-gauge` — for star rating gauge visualization
- `data-table` — for measure performance table
- `filter-panel` — for domain/time filters
- `page-header` — consistent page header with breadcrumb
- `status-badge` — for quality bonus YES/NO indicator
- `LoggerService` — HIPAA-compliant logging (no console.log)

**API Integration:**
```typescript
// star-ratings.service.ts
@Injectable({ providedIn: 'root' })
export class StarRatingsService {
  private baseUrl = '/care-gap-event/api/v1/star-ratings';

  getCurrentRating(): Observable<StarRatingResponse> {
    return this.http.get<StarRatingResponse>(`${this.baseUrl}/current`);
    // X-Tenant-ID injected by existing HTTP interceptor
  }

  getTrend(weeks = 12, granularity = 'WEEKLY'): Observable<StarRatingTrendResponse> {
    return this.http.get<StarRatingTrendResponse>(`${this.baseUrl}/trend`, {
      params: { weeks: weeks.toString(), granularity }
    });
  }

  simulate(request: StarRatingSimulationRequest): Observable<StarRatingResponse> {
    return this.http.post<StarRatingResponse>(`${this.baseUrl}/simulate`, request);
  }
}
```

**Dashboard Integration** — Add star rating stat-card to main dashboard:
```html
<!-- In dashboard.component.html, alongside existing stat-cards -->
<app-stat-card
  title="Star Rating"
  [value]="currentStarRating"
  suffix="★"
  icon="stars"
  [trend]="starRatingTrend"
  [targetValue]="4.0"
  [higherIsBetter]="true"
  tooltip="CMS Medicare Advantage Star Rating (Quality Bonus threshold: 4.0★)"
  (primaryActionClick)="navigateToStarRatings()">
</app-stat-card>
```

### 1.6 Visual Design Specifications

**Color Palette (Star Rating Tiers):**
| Stars | Color | Hex | Meaning |
|-------|-------|-----|---------|
| < 2.0 | Deep Red | #D32F2F | Critical — plan at risk |
| 2.0–2.99 | Orange | #F57C00 | Below average — needs improvement |
| 3.0–3.49 | Yellow | #FBC02D | Average — competitive but not bonus-eligible |
| 3.5–3.99 | Light Green | #66BB6A | Above average — approaching bonus |
| 4.0–4.49 | Green | #388E3C | Quality Bonus eligible (5% revenue) |
| 4.5–5.0 | Gold | #FFD700 | Exceptional — top-tier plan |

**Layout (Desktop — 1200px+):**
```
┌─────────────────────────────────────────────────────────────────┐
│ ★ Star Ratings — demo-tenant                    Last calc: 2m ago │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │ Overall  │  │ Quality  │  │ Open     │  │ Closed   │        │
│  │ 3.75 ★   │  │ Bonus:YES│  │ Gaps: 180│  │ Gaps: 917│        │
│  │ (4.0 rnd)│  │ +5% rev  │  │ ↓12 wk   │  │ ↑42 wk   │        │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │
│                                                                   │
│  ┌────────────────────────────┐  ┌────────────────────────────┐  │
│  │     12-Week Trend          │  │   Domain Breakdown         │  │
│  │   ╱‾‾‾‾‾‾‾‾╲              │  │   ★★★★☆ Eff. of Care 3.8  │  │
│  │  ╱           ╲─────────── │  │   ★★★☆☆ Chronic     3.2  │  │
│  │ ╱  ────────── 4.0 bonus  │  │   ★★★★☆ Member Exp  4.1  │  │
│  │╱                          │  │   ★★☆☆☆ Drug Safety  2.5  │  │
│  │ Jan  Feb  Mar             │  │   ★★★☆☆ Drug Plan    3.0  │  │
│  └────────────────────────────┘  └────────────────────────────┘  │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────────┐│
│  │ Measure Performance                               [Export]  ││
│  │ Code │ Name                  │ Domain │ Rate  │ Stars │ Gap ││
│  │ COL  │ Colorectal Screening  │ Eff.   │ 66.7% │ ★★★   │ 35 ││
│  │ BCS  │ Breast Cancer Screen  │ Eff.   │ 72.0% │ ★★★★  │ 28 ││
│  │ CBP  │ Blood Pressure Ctrl   │ Chron. │ 58.0% │ ★★★   │ 42 ││
│  └──────────────────────────────────────────────────────────────┘│
│                                                                   │
│  ┌──────────────────────────────────────────────────────────────┐│
│  │ What-If Simulator                                           ││
│  │ ┌─────────┐ ┌─────────┐  [+ Add Measure]                   ││
│  │ │COL  ▼   │ │ 15      │                                     ││
│  │ └─────────┘ └─────────┘                                     ││
│  │                              [Simulate]                     ││
│  │ Current: 3.75 ★  ──→  Simulated: 4.10 ★  (+0.35)          ││
│  │ Quality Bonus: YES → YES  Revenue Impact: +$2.4M est.      ││
│  └──────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
```

### 1.7 Success Criteria

| Metric | Target | Measurement |
|--------|--------|-------------|
| Time to first meaningful view | < 2 seconds | Lighthouse performance audit |
| Dashboard comprehension | < 30 seconds to answer "What's our star rating?" | User testing |
| Simulation round-trip | < 3 seconds | POST /simulate response time |
| Accessibility | WCAG 2.1 Level A | axe-core audit, 0 critical violations |
| Test coverage | > 80% | Jest unit tests for service + components |
| Pilot demo success | Prospect says "I want this" | Sales feedback |

### 1.8 Estimated Effort

| Component | Hours | Priority |
|-----------|-------|----------|
| StarRatingsService + models | 1 | P0 |
| Star gauge component | 2 | P0 |
| Domain breakdown | 1.5 | P0 |
| Trend chart | 1.5 | P0 |
| Measure table | 2 | P0 |
| Simulation panel | 3 | P1 |
| Dashboard stat-card integration | 0.5 | P0 |
| Route + navigation | 0.5 | P0 |
| Unit tests | 2 | P0 |
| **Total** | **14 hours** | |

---

## 2. Docker Stack Smoke Test with Seed Data

### 2.1 User Types & Personas

| Persona | Role | Primary Goal |
|---------|------|-------------|
| **Aaron** — Founder/Developer | DevOps | Verify full stack health before demos |
| **Future Sales Engineer** | Demo | Spin up a complete demo environment in < 5 min |
| **Future QA Engineer** | QA | Validate all services pass health checks after deployment |

### 2.2 User Stories

**US-S1** As a developer, I want a single command that starts all services, seeds demo data, and validates health so I can verify the system works end-to-end.

**Acceptance Criteria:**
- `./scripts/demo-up.sh` starts core services, waits for health, seeds data
- Clear progress output: "Starting PostgreSQL... ✓", "Seeding star ratings... ✓"
- Health check summary table at end showing all services green/red
- Total time < 5 minutes on a warm Docker cache
- Exits with code 0 only if all critical services are healthy

**US-S2** As a sales engineer, I want pre-configured demo tenants so I can walk through the product without manual setup.

**Acceptance Criteria:**
- `demo-tenant` pre-populated with 14 HEDIS measures, 1400 care gaps, 12-week trend
- Star rating: ~3.5 baseline trending to ~3.75 (realistic mid-performing MA plan)
- At least 2 tenants seeded for multi-tenant demo
- Demo user credentials documented

**US-S3** As a developer, I want the smoke test to validate API responses, not just container health, so I can catch regressions in business logic.

**Acceptance Criteria:**
- After seed: GET /star-ratings/current returns 200 with rating > 0
- After seed: GET /star-ratings/trend returns at least 10 data points
- Care gap counts match seed expectations (±5%)
- Report generation produces valid output

### 2.3 User Journey: Demo Environment Setup

```
Developer runs: ./scripts/demo-up.sh
  → Docker Compose starts core services (postgres, redis, kafka, gateway, care-gap-event-service)
  → Waits for health endpoints (max 120s timeout per service)
  → Runs seed-star-ratings-demo.sh for demo-tenant
  → Runs seed-star-ratings-demo.sh for acme-health (second tenant)
  → Validates API responses:
    ✓ GET /care-gap-event/api/v1/star-ratings/current → 200, overallRating=3.50
    ✓ GET /care-gap-event/api/v1/star-ratings/trend → 200, 13 points
    ✓ GET /care-gap-event/actuator/health → UP
  → Prints summary:
    ╔══════════════════════════════════════════╗
    ║  HDIM Demo Environment Ready            ║
    ║  Star Ratings: http://localhost:8111/... ║
    ║  Portal:       http://localhost:4200     ║
    ║  Tenant: demo-tenant / acme-health      ║
    ║  Total time: 3m 42s                     ║
    ╚══════════════════════════════════════════╝
```

### 2.4 Component Architecture

```
scripts/
├── demo-up.sh                    ← Orchestrator: up + seed + validate
├── demo-down.sh                  ← Clean shutdown
├── seed-star-ratings-demo.sh     ← Star ratings seed (ALREADY EXISTS)
├── smoke-test.sh                 ← API-level validation
└── demo-health-check.sh          ← Container health polling
```

### 2.5 Success Criteria

| Metric | Target |
|--------|--------|
| Cold start to demo-ready | < 5 minutes |
| Warm start (images cached) | < 2 minutes |
| All health checks pass | 100% of core services |
| API smoke tests pass | All 3 star-rating endpoints return expected data |
| Idempotent | Can run repeatedly without errors |

### 2.6 Estimated Effort: 3 hours

---

## 3. Houston Response Package Finalization

### 3.1 User Types & Personas

| Persona | Role | Primary Goal |
|---------|------|-------------|
| **Houston MA Transitions Buyer** | VP of Quality / CTO | Evaluate HDIM for MA transitions compliance |
| **Aaron** — Founder | Sales | Win the first pilot contract ($50-100K) |
| **Technical Evaluator** | IT Director | Assess security, integration, scalability |

### 3.2 User Stories

**US-H1** As a health plan VP of Quality, I want to see how HDIM tracks Star Ratings in real-time so I can understand the value proposition for our MA transitions book.

**Acceptance Criteria:**
- Live demo URL or recorded walkthrough showing star ratings dashboard
- Before/after simulation: "Closing 50 gaps in COL → 0.5 star improvement"
- Competitive differentiation: manual Excel tracking vs. real-time HDIM
- ROI calculation: Quality Bonus at 4.0★ = 5% of Part C/D revenue

**US-H2** As a technical evaluator, I want evidence of HIPAA compliance and multi-tenant isolation so I can approve HDIM for our security review.

**Acceptance Criteria:**
- HIPAA compliance documentation with specific controls
- Multi-tenant architecture diagram showing data isolation
- Audit logging evidence (HTTP interceptor, session timeout logging)
- DR test results and access review documentation

**US-H3** As the buyer, I want a clear implementation timeline and pricing so I can make a procurement decision.

**Acceptance Criteria:**
- 30-day pilot proposal with defined success criteria
- Pricing tiers (already defined: Starter $2K/mo, Growth $5K/mo, Enterprise $12K/mo)
- Integration requirements: data format, connectivity, timeline
- Support SLA during pilot period

### 3.3 User Journey: Houston Evaluation

```
Week 0: Aaron sends response package
  → VP of Quality reads executive summary (2 pages)
  → Forwards technical appendix to IT Director
  → IT Director reviews: HIPAA checklist ✓, architecture diagram ✓, security controls ✓

Week 1: Technical deep-dive call
  → Aaron demos live star ratings dashboard with Houston's seed data
  → Shows simulation: "What if your BCS screening rate improves 5%?"
  → IT Director asks about data ingestion — show FHIR R4 endpoint docs
  → Agree on 30-day pilot scope

Week 2-5: Pilot execution
  → Houston feeds HEDIS data via FHIR API
  → HDIM processes and generates star ratings
  → Weekly check-in calls reviewing rating trajectory

Week 6: Pilot review
  → Compare HDIM projections to Houston's manual calculations
  → Demonstrate: identified 3 measures where 50 closures = 0.5 star improvement
  → Sign Growth tier contract ($5K/mo)
```

### 3.4 Deliverables Checklist

| Deliverable | Status | Location |
|-------------|--------|----------|
| Executive summary | ✅ Exists | `docs/sales/houston/` |
| Technical architecture | ✅ Exists | `docs/architecture/` |
| HIPAA compliance pack | ✅ Exists | `docs/compliance/` |
| Live demo environment | ⏳ Needs demo-up.sh | See Section 2 |
| Star ratings walkthrough | ⏳ Needs UI | See Section 1 |
| Pricing proposal | ✅ Exists | `docs/sales/` |
| Implementation timeline | ⏳ Draft needed | 1-page document |
| API documentation | ✅ Exists | Swagger UI (157 endpoints) |

### 3.5 Success Criteria

| Metric | Target |
|--------|--------|
| Response time to prospect | < 48 hours from inquiry |
| Technical evaluation pass | Security review approved |
| Pilot agreement signed | Within 2 weeks of demo |
| Pilot success | Star rating projections within 0.1★ of actual |

### 3.6 Estimated Effort: 2 hours (implementation timeline doc + demo script)

---

## 4. LinkedIn Content Pipeline

### 4.1 User Types & Personas

| Persona | Role | Primary Goal |
|---------|------|-------------|
| **Aaron** — Founder | Thought Leader | Build credibility in healthcare quality space |
| **Target: Quality Directors** | Buyer | Discover solutions to Stars challenges |
| **Target: Health IT Leaders** | Influencer | Evaluate vendor technical capability |
| **Target: Investors/Advisors** | Stakeholder | Track Grateful House progress |

### 4.2 User Stories

**US-L1** As a founder, I want a consistent LinkedIn posting cadence so I build thought leadership visibility with my target buyer personas.

**Acceptance Criteria:**
- 2 posts per week minimum
- Each post ties to a real HDIM capability or market insight
- Engagement targets: 500+ impressions, 10+ reactions, 3+ comments per post
- Content themes rotate: market insight → product capability → industry news → case study

**US-L2** As a target buyer, I want to see posts that demonstrate deep domain expertise so I trust this vendor understands my challenges.

**Acceptance Criteria:**
- Posts reference specific CMS regulations (Star Ratings methodology, HEDIS measures)
- Technical accuracy verified (CMS cut points, measure weights, bonus thresholds)
- Posts include actionable insights, not just product promotion
- Mix of educational content (70%) and product mentions (30%)

### 4.3 Content Journey: Post Discovery to Pilot

```
Quality Director sees LinkedIn post about Stars methodology changes
  → Reads: "3 HEDIS measures moving to ECDS in MY 2026 — here's what changes"
  → Thinks: "This person understands our world"
  → Follows Aaron
  → Sees next post: "Real-time star rating simulation — close 50 gaps, see 0.5★ improvement"
  → Clicks link to blog/demo
  → Requests demo
  → Enters Houston-style evaluation pipeline
```

### 4.4 Content Calendar (Updated)

| # | Date | Theme | Title/Hook | HDIM Feature Tie-In |
|---|------|-------|-----------|---------------------|
| 1 | ✅ Mar 3 | Market insight | HEDIS quality gaps at scale | Care gap detection engine |
| 2 | ⏳ Mar 13 | Stars methodology | Stars ratings infrastructure gap | **Star ratings dashboard (NEW)** |
| 3 | Mar 17 | MIPS/Stars convergence | APP Plus expansion 2025-2028 | Multi-framework support |
| 4 | Mar 20 | Technical capability | Real-time star simulation demo | What-if simulator |
| 5 | Mar 24 | Industry news | CMS 2027 Star Ratings advance notice | Stars trend tracking |
| 6 | Mar 27 | Case study teaser | "What a 0.5★ improvement means for revenue" | Quality bonus calculator |
| 7 | Mar 31 | BSL launch announcement | Open-source healthcare quality platform | BSL 1.1 / Grateful House |
| 8 | Apr 3 | Technical deep-dive | Event sourcing for real-time quality metrics | Event processing architecture |

### 4.5 Success Criteria

| Metric | Target | Timeframe |
|--------|--------|-----------|
| Posting cadence | 2/week | Ongoing |
| Post impressions | 500+ average | By post #5 |
| Profile views | 100+/week | By week 3 |
| Connection requests from target buyers | 5+/month | By month 2 |
| Inbound demo requests via LinkedIn | 1+/month | By month 2 |

### 4.6 Estimated Effort: 1 hour per post (8 hours total for pipeline)

---

## 5. Custom Domain (healthdatainmotion.com)

### 5.1 User Types & Personas

| Persona | Role | Primary Goal |
|---------|------|-------------|
| **Prospect** | Buyer | Access professional-looking product landing page |
| **Aaron** — Founder | Brand | Present credible, enterprise-grade web presence |
| **Google/LinkedIn** | SEO | Index and rank content for healthcare quality searches |

### 5.2 User Stories

**US-D1** As a prospect visiting healthdatainmotion.com, I want a professional landing page so I trust this is a legitimate enterprise vendor.

**Acceptance Criteria:**
- healthdatainmotion.com resolves to landing page (currently on Vercel subdomain)
- HTTPS with valid certificate (Vercel handles this automatically)
- Page load < 3 seconds
- Mobile responsive
- Contact forms work (info@mahoosuc.solutions, sales@mahoosuc.solutions)

**US-D2** As the founder, I want proper DNS configuration so SEO works and emails are deliverable.

**Acceptance Criteria:**
- A/CNAME records pointing to Vercel
- MX records for email (if using custom email)
- robots.txt and sitemap.xml with canonical domain
- Google Search Console verified
- LinkedIn company page linked to custom domain

### 5.3 User Journey: Prospect Web Visit

```
Quality Director gets LinkedIn post from Aaron
  → Post links to healthdatainmotion.com/star-ratings
  → Landing page loads with professional branding
  → Sees: Hero section, feature overview, star ratings screenshot, pricing
  → Clicks "Request Demo" → form sends to sales@mahoosuc.solutions
  → Aaron receives notification, responds within 24 hours
  → Demo scheduled → Houston-style evaluation pipeline begins
```

### 5.4 DNS Configuration Plan

```
# Vercel custom domain setup
healthdatainmotion.com        → A record → 76.76.21.21 (Vercel)
www.healthdatainmotion.com    → CNAME  → cname.vercel-dns.com
```

**Current state:** Domain purchased, not yet configured. Landing page deployed at `landing-page-ecru-five-65.vercel.app`.

### 5.5 Success Criteria

| Metric | Target |
|--------|--------|
| Domain resolves correctly | HTTPS, < 100ms DNS lookup |
| SSL certificate active | Valid, auto-renewed |
| Page load time | < 3 seconds (already meeting this on Vercel) |
| SEO indexing | Indexed within 7 days of DNS change |
| Email deliverability | SPF/DKIM pass for outbound |

### 5.6 Estimated Effort: 1 hour (DNS config + Vercel setup + verification)

---

## 6. Implementation Priority Matrix

| Priority | Task | Revenue Impact | Effort | Dependencies |
|----------|------|---------------|--------|-------------|
| **P0** | Star Ratings UI (Section 1) | Direct — demo differentiator | 14 hrs | Backend API complete ✅ |
| **P0** | Docker Smoke Test (Section 2) | Enables all demos | 3 hrs | Seed script complete ✅ |
| **P1** | Custom Domain (Section 5) | Brand credibility | 1 hr | DNS access |
| **P1** | Houston Response (Section 3) | First pilot close | 2 hrs | Star Ratings UI |
| **P2** | LinkedIn Pipeline (Section 4) | Lead generation | 8 hrs | Star Ratings screenshots |

### Recommended Execution Order

```
Day 1 (6 hrs):
  Morning:  Docker smoke test script (3 hrs) → validates demo environment
  Afternoon: Star Ratings UI — service + models + gauge + route (3 hrs)

Day 2 (6 hrs):
  Morning:  Star Ratings UI — domain breakdown + trend chart (3 hrs)
  Afternoon: Star Ratings UI — measure table + dashboard integration (3 hrs)

Day 3 (5 hrs):
  Morning:  Star Ratings UI — simulation panel + tests (3 hrs)
  Afternoon: Custom domain setup (1 hr) + Houston response update (1 hr)

Day 4 (2 hrs):
  LinkedIn post #2 publish + post #3 draft with star ratings screenshots
```

**Total: 20 hours across 4 days → Demo-ready for pilot prospects**

---

## 7. LinkedIn Content Update

### Posts to Generate (Post Star-Ratings UI Completion)

**Post #2 (Ready Now — Publish Today):** "Stars ratings infrastructure gap"
- Theme: Most health plans track Stars in Excel. That's a $3.2M blind spot.
- Hook: "Your 2026 Star Rating is being calculated right now. Are you watching?"
- CTA: Link to healthdatainmotion.com (once custom domain is live)
- Assets needed: None (conceptual post, already drafted)

**Post #3 (Draft by Mar 17):** "MIPS/Stars convergence"
- Theme: APP Plus expansion means ACOs and MA plans face the same quality measurement challenge
- Hook: "By 2028, 11 MIPS measures will be in APP Plus. Sound familiar, Stars people?"
- Assets needed: None (regulatory analysis post)

**Post #4 (New — Mar 20):** "Real-time star simulation demo" ← **Requires star ratings UI**
- Theme: Show a 30-second screen recording of the what-if simulator
- Hook: "What if you could see exactly how closing 50 care gaps affects your Star Rating?"
- Assets needed: Screen recording of simulation panel
- Format: Video/GIF (30 seconds)

**Post #5 (New — Mar 24):** "CMS 2027 advance notice analysis"
- Theme: Break down anticipated CMS methodology changes for MY 2027
- Hook: "CMS just gave you a 12-month heads up. Here's what's changing."
- Assets needed: None (regulatory analysis)

**Post #6 (New — Mar 27):** "Quality bonus revenue calculator"
- Theme: Quantify what a 0.5★ improvement means in dollars
- Hook: "For a 50,000-member MA plan, the difference between 3.5★ and 4.0★ is $12M annually."
- Assets needed: Revenue calculation table (can be text-based)

**Post #7 (New — Mar 31):** "BSL launch announcement"
- Theme: Grateful House open-sources HDIM under BSL 1.1
- Hook: "Healthcare quality infrastructure shouldn't be locked behind 7-figure contracts."
- Assets needed: GitHub repo link (already public)

**Post #8 (New — Apr 3):** "Event sourcing for healthcare quality"
- Theme: Technical deep-dive on how event-driven architecture enables real-time quality tracking
- Hook: "Your HEDIS data is a stream of events. Why are you treating it like a batch report?"
- Assets needed: Architecture diagram

### Content Generation Priorities

1. **Publish Post #2 today** (already drafted, just needs final review)
2. **Finalize Post #3** by Mar 15 (already drafted)
3. **Posts #4-8** drafted sequentially as features ship
4. **Post #4 is the money post** — requires star ratings UI to be live for screen recording

---

## Appendix A: Existing Infrastructure Summary

### Clinical Portal Architecture (for Star Ratings integration)

- **Framework:** Angular 17+ standalone components
- **UI Library:** Angular Material 21.2.0
- **Charts:** Chart.js 4.5.1 + ng2-charts 8.0.0 (already installed)
- **State:** RxJS (no NgRx needed for star ratings — simple service-based state)
- **Auth:** AuthGuard + RoleGuard with X-Tenant-ID header injection
- **Logging:** LoggerService (HIPAA-compliant, no console.log)
- **Audit:** HTTP Audit Interceptor (automatic, no code needed)
- **Session:** 15-minute idle timeout with HIPAA audit logging
- **Shared components:** 38 reusable components (stat-card, chart-line, chart-gauge, data-table, etc.)
- **Routes:** 40+ lazy-loaded routes with role-based access
- **Existing star references:** Quality measures page already displays `starRating` per measure with `getStarArray()` helper

### Backend API (Already Complete)

| Endpoint | Method | Auth | Response |
|----------|--------|------|----------|
| `/api/v1/star-ratings/current` | GET | ADMIN, EVALUATOR, ANALYST | StarRatingResponse |
| `/api/v1/star-ratings/trend` | GET | ADMIN, EVALUATOR, ANALYST | StarRatingTrendResponse |
| `/api/v1/star-ratings/simulate` | POST | ADMIN, EVALUATOR | StarRatingResponse |

### HEDIS Measures Supported (52 total)

7 domains, 52 measures defined in `StarRatingMeasure` enum. 14 measures seeded in demo script with realistic numerator/denominator patterns for a mid-performing MA plan (~3.5 stars baseline).
