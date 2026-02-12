# Claude Code Skills - HDIM Project Enhancement

**Date:** February 11, 2026
**Purpose:** Document newly installed skills from mahoosuc-os for enhanced HDIM product design and customer scenario thinking

---

## Installation Summary

**13 skills installed** from webemo-aaron/mahoosuc-operating-system repository:

### Core Skills (All Installed)

| Skill | Purpose | HDIM Use Case |
|-------|---------|---------------|
| `hdim-customer-scenarios` | **NEW** - HDIM-specific customer persona thinking, workflow validation | Phase 2 sales positioning, product-market fit validation, feature prioritization |
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

---

## Key Skills for Phase 2 (March 2026)

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

## Next Steps: Using Skills for Phase 2

**Week of Feb 11-15:**
1. ✅ Skills installed and documented
2. ☐ Use `hdim-customer-scenarios` to validate Phase 2 positioning
3. ☐ Use `brand-voice` to review discovery call script
4. ☐ Use `content-optimizer` to create LinkedIn thought leadership posts

**Week of Feb 17-21:**
1. ☐ Use `frontend-design` to design marketing landing page
2. ☐ Use `security-auditor` to validate HIPAA compliance before pilot
3. ☐ Use `compliance-mapper` to verify HEDIS/FHIR alignment

**Week of Feb 24-28:**
1. ☐ Use `content-optimizer` for multi-platform content (Reddit, Twitter, LinkedIn)
2. ☐ Use `brand-voice` for client proposal templates
3. ☐ Use `hdim-customer-scenarios` for VP Sales training (persona walkthroughs)

---

## Skill Maintenance

**Documentation Location:** `/mnt/wdblack/dev/projects/hdim-master/.claude/skills/`

**Update Frequency:**
- Review quarterly (March, June, September, December)
- Update after major product launches
- Refresh based on customer feedback

**Source Repository:** https://github.com/webemo-aaron/mahoosuc-operating-system

**Custom Skills:**
- `hdim-customer-scenarios` - Maintained in HDIM repo (HDIM-specific)
- All others synced from mahoosuc-os

---

**Document Version:** 1.0
**Last Updated:** February 11, 2026
**Status:** Skills Installed & Ready for Phase 2 Execution
**Next Review:** March 15, 2026 (Phase 2 mid-point)
