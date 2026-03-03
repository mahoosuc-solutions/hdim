# Claude Code Skills - HDIM Project Enhancement

**Date:** March 2, 2026
**Purpose:** Document all installed skills for enhanced HDIM product design, sales execution, and marketing video production

---

## Installation Summary

**23 skills installed** across core platform, sales, and video production:

### Core Platform Skills (14)

| Skill | Purpose | HDIM Use Case |
|-------|---------|---------------|
| `hdim-customer-scenarios` | **CUSTOM** - HDIM-specific customer persona thinking, workflow validation | Phase 2 sales positioning, product-market fit validation, feature prioritization |
| `frontend-design` | Distinctive, production-grade UI/UX design | Clinical Portal UI, Financial ROI Dashboard design |
| `brand-voice` | Data-driven, authority-building content voice | Sales collateral, marketing content, thought leadership |
| `content-optimizer` | Platform-specific content optimization (Reddit, LinkedIn, Twitter, HN) | Social media strategy, community engagement, content marketing |
| `qa-architect` | Test strategy, coverage goals, QA automation | Test planning, quality assurance strategy |
| `security-auditor` | Security audit and compliance validation | HIPAA compliance scanning, security reviews |
| `compliance-mapper` | Regulatory compliance mapping | HEDIS/FHIR/ECDS compliance validation |
| `data-governance` | Data governance and privacy controls | Multi-tenant isolation, PHI handling, data security |
| `infra-as-code` | Infrastructure automation and DevOps | Kubernetes deployment, CI/CD pipelines |
| `release-manager` | Release planning and deployment orchestration | Production deployment, release coordination |
| `vps-ops` | VPS operations and server management | On-premise deployment, infrastructure management |
| `onprem-ops` | On-premise operations and maintenance | Enterprise customer on-prem installations |
| `stripe-revenue-analyzer` | Revenue analytics and financial tracking | Financial ROI tracking (Phase 3), revenue analysis |
| `vercel-landing-page-builder` | Landing page design and development | Marketing website, product landing pages |
| `service-startup-doctor` | **NEW** - Diagnoses cascading Spring Boot startup failures in HDIM microservices | Fixes duplicate YAML keys, bean conflicts, Liquibase misconfigs, entity scan issues |

### Sales Skills (4) — Added February 2026

| Skill | Purpose | HDIM Use Case |
|-------|---------|---------------|
| `sales-manager` | Strategic advisor — validate sales strategy, analyze deals, pipeline coaching | Sanity-check sales decisions, prioritize prospects, board-level messaging |
| `sales-demo-designer` | Create persona-specific demo pathways (15/30/45-min) with feature prioritization | Build CMO, Coordinator, CFO, IT, Provider demo flows with specific talking points |
| `sales-discovery-coach` | AI-powered discovery call coach with persona roleplay and call scoring | Practice calls, analyze transcripts, score on 5 dimensions (Pain, Qualification, Objections, Next Steps, Rapport) |
| `sales-objection-handler` | Master 6 common HDIM objections with reframe techniques and competitive positioning | "Too expensive," "build vs. buy," "security concerns," "EHR integration," "wait and see" |

### Video Production Skills (5) — Added February 2026

| Skill | Purpose | HDIM Use Case |
|-------|---------|---------------|
| `video-narrative-strategist` | Plans 10-scene narrative arc for HDIM Remotion marketing videos | Start here — maps persona pain to 4-act story structure, produces story brief |
| `video-clinical-writer` | Writes production-ready text content (headlines, captions, metrics, CTAs) for video configs | Transforms story briefs into `RoleStoryConfig` text fields with brand voice |
| `video-medical-reviewer` | Validates clinical accuracy, HEDIS compliance, and HIPAA-safe marketing patterns | Reviews every text field for medical plausibility and regulatory safety before render |
| `video-visual-designer` | Optimizes overlay placement, timing, Ken Burns motion for visual impact | Designs `overlays[]` arrays — glow-highlights, metrics, badges — for each scene |
| `video-pipeline-coordinator` | Orchestrates rendering, QA, and distribution asset generation | Final step — writes config, renders MP4, generates YouTube/LinkedIn/Twitter assets |

---

## Key Skills for Phase 2 (March 2026) — Sales & Video Production NEW

### 1. `hdim-customer-scenarios` (CUSTOM BUILT) ⭐

**Purpose:** Think through HDIM features from multiple stakeholder perspectives

**Customer Personas:**
- Health Plan CMO / VP Quality (strategic value, Star ratings, ROI)
- Quality Coordinator (daily workflow, gap closure execution)
- Healthcare Provider (clinical utility, time savings, patient care)
- CFO / Finance Leader (financial ROI, board reporting)
- IT / Analytics Leader (integration, FHIR/ECDS compliance)

**Key Workflows Documented:**
1. HEDIS Reporting Season (annual cycle, Jan-Mar deadline)
2. Star Ratings Improvement (year-round quality program)
3. Pre-Visit Planning (daily provider workflow)
4. Financial ROI Tracking (executive dashboard, real-time)

**Use Cases:**
- Validate product-market fit for new features
- Design customer journeys (end-to-end workflows)
- Identify workflow gaps and pain points
- Ensure HEDIS/FHIR/ECDS alignment
- Create customer-centric sales messaging

**Example Invocation:**
```
Use the hdim-customer-scenarios skill to validate:
"How would a CMO use the predictive gap emergence feature?"
```

---

### 2. `brand-voice` (Data-Driven Authority)

**Purpose:** Ensure consistent, proof-backed brand voice across all content

**Key Principles:**
- Data-driven (every claim has a number)
- Show what didn't work (vulnerability builds trust)
- Actionable, not inspirational (reader can implement immediately)
- Authority without arrogance (let data establish expertise)
- Value-first, CTA second (90% value, 10% soft CTA)
- Honest ROI calculations (show the math, caveat assumptions)

**HDIM Applications:**
- Sales collateral (competitive comparison, ROI calculator)
- Discovery call scripts (data-driven hooks)
- Marketing content (blog posts, case studies)
- Social media (LinkedIn, Reddit, Twitter)
- Client proposals (financial proof, ROI transparency)

**Example Output:**
```markdown
❌ DON'T: "HDIM dramatically improves care gap closure"
✅ DO: "Early pilots: 35-40% gap closure improvement, $1M+ annual quality bonus capture"
```

---

### 3. `content-optimizer` (Platform-Specific Optimization)

**Purpose:** Optimize content for maximum engagement across platforms

**Platform Strategies:**
- **Reddit:** Casual, helpful, anti-marketing (data-driven hooks, soft CTA)
- **LinkedIn:** Professional thought leadership (bold statements, 3-5 hashtags)
- **Twitter:** Punchy threads (contrarian takes, numbered threads)
- **HackerNews:** Technical depth (architecture decisions, benchmarks, no emojis)
- **Discord:** Friendly, community-focused (short bursts, heavy emojis)

**HDIM Applications:**
- LinkedIn thought leadership (VP Sales, CEO positioning)
- Reddit community engagement (r/healthIT, r/productmanagement)
- Twitter threads (AI healthcare trends, quality measures)
- Content repurposing (1 blog post → 5 platform versions)

**Example Usage:**
```
Optimize the Phase 2 sales positioning for:
- LinkedIn (professional audience)
- Reddit (r/healthIT community)
- Twitter (AI healthcare thread)
```

---

### 4. `frontend-design` (Distinctive UI/UX)

**Purpose:** Create production-grade interfaces that avoid generic AI aesthetics

**Design Principles:**
- Understand context first (purpose, audience, tone, differentiation)
- Choose bold aesthetic direction (brutalist, minimalist, tech-forward, etc.)
- Typography that elevates (distinctive fonts, clear hierarchy)
- Color with purpose (cohesive palettes, atmospheric gradients)
- Motion with impact (staggered animations, scroll effects)
- Spatial composition (asymmetry, overlap, depth)

**HDIM Applications:**
- Clinical Portal UI redesign (WCAG 2.1 compliant, HIPAA-friendly)
- Financial ROI Dashboard (Phase 3 executive-level design)
- Marketing landing pages (distinctive, memorable)
- Care Gap Summaries (provider-facing UX)

**Anti-Patterns to Avoid:**
- Generic font families (Inter, Roboto, Open Sans)
- Cliché gradients (purple-to-pink everywhere)
- Predictable symmetrical layouts
- Cookie-cutter card designs

---

### 5. `security-auditor` (HIPAA Compliance Scanning)

**Purpose:** Automated security audit and compliance validation

**HDIM Critical Use:**
- PHI handling validation (cache TTL ≤ 5 minutes)
- Multi-tenant isolation verification
- HIPAA §164.312(b) audit controls
- Session timeout compliance
- No-console.log enforcement (Angular)

**Pre-Commit Hooks:**
- Scan for PHI exposure in logs
- Validate cache headers on PHI endpoints
- Check @Audited annotations on PHI methods
- Verify multi-tenant filter on all queries

---

### 6. `compliance-mapper` (Regulatory Validation)

**Purpose:** Map features to regulatory requirements

**HDIM Regulatory Landscape:**
- **HEDIS:** MY 2026 updates, ECDS transition (2030 target)
- **FHIR:** ONC HTI-1 Rule (Jan 2025), CMS Prior Auth (Jan 2026)
- **HIPAA:** §164.312(a)(2)(iii) session timeout, §164.312(b) audit controls
- **CMS Star Ratings:** Quality measure financial impact tracking

**Validation Checklist:**
- HEDIS measure alignment
- FHIR/ECDS readiness
- HIPAA PHI handling
- Multi-tenant data isolation

---

## Sales Skills — Phase 2 Execution

### `sales-manager` — Strategic Sales Advisor

**Purpose:** VP Sales coaching, deal analysis, pipeline prioritization, board-level messaging

**Use Cases:**
- Validate ICP prioritization (health plans vs. ACOs)
- Sanity-check deal strategy before a customer call
- Analyze pipeline to identify highest-probability closes
- Prepare for Series A investor questions on sales metrics

**Example Invocation:**
```
Use sales-manager skill:
"Here are my top 5 prospects. Which 2 should I focus on this week
 to hit the March LOI target? [paste prospect list]"
```

---

### `sales-demo-designer` — Persona Demo Pathways

**Purpose:** Design 15/30/45-minute demos tailored to each buyer persona with exact feature sequencing and talking points

**5 Pre-Built Demo Pathways:**
- **CMO / VP Quality** (30 min): Predictive gap detection → Coordinator dashboard → Financial tracking closer
- **Quality Coordinator** (20 min): Smart gap dashboard → Prioritization logic → Time savings calc
- **CFO / Finance Leader** (20 min): Financial model → Month-by-month tracking → Investment decision
- **IT / Analytics Leader** (30 min): Architecture → Integration → Security/compliance → Performance
- **Healthcare Provider** (10 min): Clinical alert comparison → Outcome impact

**Key Principle:** Every feature connects to a pain point. Every demo ends with financial impact.

**Example Invocation:**
```
Use sales-demo-designer skill:
"Design a 30-minute demo for a CMO at a 300K-member Blue Cross plan"
```

---

### `sales-discovery-coach` — Discovery Call Practice & Scoring

**Purpose:** Roleplay discovery calls, analyze transcripts, score on 5 dimensions

**Scoring Rubric (0-100):**
- Pain Discovery (0-20 pts)
- Qualification (0-20 pts)
- Objection Handling (0-15 pts)
- Next Steps Clarity (0-20 pts)
- Credibility & Rapport (0-25 pts)

**3 Usage Modes:**
1. **Live Roleplay** — Claude plays customer persona, gives feedback after each exchange
2. **Transcript Analysis** — Paste a real call, get scored feedback
3. **Question Bank** — "What discovery question should I ask about care gap prioritization?"

**Example Invocation:**
```
Use sales-discovery-coach skill:
"Practice a discovery call — you're a skeptical CMO at a 500K-member plan.
 I'll play the sales rep."
```

---

### `sales-objection-handler` — 6 Core Objections with Reframes

**Purpose:** Turn the 6 most common HDIM objections into buying conversations

**Covered Objections:**
1. "Too expensive / costs more than Epic add-on"
2. "We'll build this ourselves"
3. "Security and compliance concerns"
4. "Not enough EHR integration depth"
5. "We want to wait and see market adoption"
6. "We already have a quality vendor"

**Technique:** Each objection has a data-driven reframe, competitive positioning, and closing language.

**Example Invocation:**
```
Use sales-objection-handler skill:
"Customer just said: 'We'd need to rebuild our entire workflow around this.
 We don't have the bandwidth right now.'"
```

---

## Video Production Pipeline

The 5 video skills work in sequence. Always run them in this order:

```
video-narrative-strategist → video-clinical-writer → video-medical-reviewer
       → video-visual-designer → video-pipeline-coordinator
```

### Skill 1: `video-narrative-strategist` — Story Planning

**Input:** Target role (CMO, Coordinator, CFO, Provider, IT)
**Output:** 10-scene story brief with pain-to-solution arc

**4-Act Structure:**
- Act 1 (Scenes 1-2): The Pain — Reactive, overwhelmed, manual
- Act 2 (Scenes 3-5): The Moment of Change — HDIM discovery
- Act 3 (Scenes 6-8): The Solution — Workflow transformation
- Act 4 (Scenes 9-10): The Outcome — Financial impact, recognition

---

### Skill 2: `video-clinical-writer` — Script Content

**Input:** Story brief from narrative-strategist
**Output:** Complete `RoleStoryConfig` text fields — headlines, captions, metrics, CTA

**Brand Voice Enforcement:**
- Every metric has a specific number ("35-40% improvement," not "significant improvement")
- No vague claims ("HIPAA-compliant" specifics, not "secure")
- CTA is soft and value-first

---

### Skill 3: `video-medical-reviewer` — Clinical Validation

**Input:** Draft `RoleStoryConfig` from clinical-writer
**Output:** Approved config or list of corrections with clinical justification

**Validates:**
- HEDIS terminology accuracy
- CMS Star Ratings data accuracy
- HIPAA marketing compliance (no false claims)
- Statistical plausibility

---

### Skill 4: `video-visual-designer` — Overlay Design

**Input:** Clinically validated `RoleStoryConfig`
**Output:** Complete `overlays[]` arrays for all 10 scenes

**Overlay Types:** glow-highlight, metric, badge, text
**Ken Burns Motion:** Zoom/pan timing optimized for 30-second attention arc

---

### Skill 5: `video-pipeline-coordinator` — Render & Distribute

**Input:** Finalized `RoleStoryConfig` with overlays
**Output:** Rendered MP4 + YouTube/LinkedIn/Twitter distribution assets

---

## `service-startup-doctor` — Microservice Startup Failures

**Purpose:** Diagnoses cascading startup failures in HDIM Spring Boot microservices

**Common Issues Diagnosed:**
- Duplicate YAML keys in shared module configs
- Spring bean conflicts from gateway-core shared module
- Liquibase changelog misconfiguration (missing includes, wrong paths)
- JPA entity scan overreach (scanning other service's entities)
- Missing `@ConditionalOnMissingBean` optional beans

**When to Use:**
- Service crash loop or "application failed to start"
- "Could not autowire" bean errors after adding new shared module
- Liquibase rollback failures blocking startup

**Example Invocation:**
```
Use service-startup-doctor skill:
"notification-service is failing to start after the prior-auth integration.
 Here are the logs: [paste stack trace]"
```

---

## How to Use These Skills

### During Product Design
```bash
# Validate feature from customer perspective
"Use hdim-customer-scenarios skill: How would a quality coordinator use this?"

# Ensure regulatory compliance
"Use compliance-mapper skill: Does this feature align with HEDIS reporting requirements?"
```

### During Sales/Marketing
```bash
# Create platform-optimized content
"Use content-optimizer skill: Turn this case study into LinkedIn post"

# Ensure brand voice consistency
"Use brand-voice skill: Review this sales email for data-driven messaging"
```

### During Development
```bash
# Design distinctive UI
"Use frontend-design skill: Create Financial ROI Dashboard with executive-level design"

# Security audit
"Use security-auditor skill: Validate HIPAA compliance for patient data endpoints"
```

---

## Skill Activation Examples

### Example 1: Feature Validation

**User Request:**
> "We're thinking about adding automated provider SMS notifications for care gaps. Is this a good idea?"

**Skill Invocation:**
```
Use hdim-customer-scenarios skill to validate:
- Which personas benefit? (Coordinator, Provider)
- What's the workflow? (Automated vs. manual outreach)
- What's the ROI? (Time savings, gap closure improvement)
- Is this HIPAA-compliant? (PHI in SMS messages)
- What do providers actually want? (SMS fatigue, opt-in preferences)
```

**Expected Output:**
- Persona-by-persona analysis (Coordinator ✅, Provider ⚠️ "SMS fatigue")
- Workflow comparison (current manual outreach vs. automated SMS)
- Financial impact ($X time savings, Y% gap closure improvement)
- Compliance concerns (PHI in SMS = HIPAA risk, need opt-in)
- Recommendation: ✅ Build with opt-in controls, or ⚠️ Validate with providers first

---

### Example 2: Marketing Content Creation

**User Request:**
> "Create a LinkedIn post about HDIM's predictive gap feature"

**Skill Invocation:**
```
1. Use brand-voice skill: Apply data-driven, authority-building tone
2. Use content-optimizer skill: Optimize for LinkedIn (professional, 3-5 hashtags)
3. Use hdim-customer-scenarios skill: Use CMO persona pain points
```

**Expected Output:**
```markdown
# LinkedIn Post (Brand Voice + Content Optimizer Applied)

Healthcare quality teams are always playing catch-up.

I analyzed 3 months of HEDIS reporting workflows across 15 health plans.

The pattern is clear:
• Gaps identified 2-4 weeks before deadline
• Quality coordinators overwhelmed with manual outreach
• 40-60% of gaps never closed (too late to intervene)

What if you could see gaps 30-60 days ahead?

HDIM's predictive AI does exactly that:
✅ 35-40% gap closure improvement (early pilots)
✅ 40% coordinator time savings
✅ $1M+ quality bonus capture (100K member plan)

Not projections. Actual results from early pilots.

The difference: AI-first architecture built for prediction, not retrofitted onto legacy systems.

Healthcare is finally catching up on AI. HDIM is leading that wave.

#HealthcareAI #HEDIS #QualityMeasures #ValueBasedCare #HealthIT
```

---

### Example 3: UI/UX Design

**User Request:**
> "Design the Financial ROI Dashboard for CFOs and executives"

**Skill Invocation:**
```
Use frontend-design skill:
- Context: Executive dashboard, CFO/board reporting, financial focus
- Audience: C-suite, board members (non-technical)
- Tone: Professional, data-driven, authoritative
- Differentiation: Bloomberg Terminal aesthetic (financial data + healthcare context)
```

**Expected Output:**
```tsx
/**
 * Financial ROI Dashboard
 *
 * Design Direction: "Financial Terminal + Healthcare Context"
 *
 * Key Design Choices:
 * - Typography: IBM Plex Mono (financial terminal feel) + Inter (readability)
 * - Colors: Deep navy (#0A1628) + emerald accent (#10B981) for positive metrics
 * - Motion: Subtle number transitions, chart animations on scroll
 * - Layout: Grid-based, data-dense but scannable, asymmetric emphasis cards
 *
 * Target: CFO viewing on desktop during board meeting prep (30-second scan)
 */

export function FinancialROIDashboard() {
  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950">
      {/* Executive Summary Card (Hero) */}
      <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-emerald-500/10 via-emerald-600/5 to-transparent p-8 backdrop-blur-xl border border-emerald-500/20">
        {/* Real-time metrics, financial-first */}
      </div>

      {/* Quality Bonus Tracking Grid */}
      <div className="grid grid-cols-3 gap-6">
        {/* HEDIS measures with $ impact */}
      </div>
    </div>
  );
}
```

---

## Installation Verification

**Verify skills are loaded:**
```bash
ls -la /mnt/wdblack/dev/projects/hdim-master/.claude/skills/
```

**Expected output:**
```
brand-voice/
compliance-mapper/
content-optimizer/
data-governance/
frontend-design/
hdim-customer-scenarios/  ← CUSTOM BUILT
infra-as-code/
onprem-ops/
qa-architect/
release-manager/
security-auditor/
stripe-revenue-analyzer/
vercel-landing-page-builder/
vps-ops/
```

---

## Phase 2 Skill Usage Plan (March 2026)

**This Week (Mar 2-8) — Sales Motion:**
1. ✅ Sales skills installed and documented
2. ☐ Use `sales-demo-designer` to prepare CMO demo for Healthix call
3. ☐ Use `sales-discovery-coach` to practice discovery call script
4. ☐ Use `sales-objection-handler` to prepare for "we'll build it ourselves" objection

**Week 2 (Mar 9-15) — Content + Outreach:**
1. ☐ Use `content-optimizer` to optimize LinkedIn posts for thought leadership
2. ☐ Use `brand-voice` to refine client proposal templates
3. ☐ Use `video-narrative-strategist` + pipeline for first CMO role video

**Week 3-4 (Mar 16-28) — Pilot Close:**
1. ☐ Use `sales-manager` to analyze pipeline before LOI push
2. ☐ Use `hdim-customer-scenarios` for pilot customer onboarding walkthrough
3. ☐ Use `security-auditor` to validate HIPAA compliance before pilot launch

---

## Skill Maintenance

**Documentation Location:** `/mnt/wdblack/dev/projects/hdim-master/.claude/skills/`

**Update Frequency:**
- Review quarterly (March, June, September, December)
- Update after major product launches or new skill additions
- Refresh based on customer feedback

**Source Repository:** https://github.com/webemo-aaron/mahoosuc-operating-system

**Custom Skills (HDIM-specific):**
- `hdim-customer-scenarios` - Maintained in HDIM repo
- `service-startup-doctor` - Maintained in HDIM repo
- `sales-manager`, `sales-demo-designer`, `sales-discovery-coach`, `sales-objection-handler` - HDIM sales skills
- `video-*` (5 skills) - HDIM Remotion video production pipeline

---

**Document Version:** 2.0
**Last Updated:** March 2, 2026
**Status:** 23 Skills Installed — Core, Sales, Video Production
**Next Review:** June 1, 2026 (Phase 3 mid-point)
