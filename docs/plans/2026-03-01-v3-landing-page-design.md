# V3 Landing Page Design — Three Approaches for Comparison

**Date:** 2026-03-01
**Status:** Approved
**Deployment:** Three git worktrees, each with Vercel preview URL

## Problem

The current landing page (landing-page-v0/) tells a "quality measurement platform" story. Since Feb 3, 444 commits shipped: Wave-1 revenue cycle, custom measure builder, CMO onboarding, security hardening, operations orchestration. The landing page doesn't reflect this expanded platform story or organize content around customer personas.

## Strategy

Build all three approaches in parallel worktrees for side-by-side comparison:
- **Approach A:** Persona-First Homepage (segment selector → dedicated segment pages)
- **Approach B:** Feature-Story Hybrid (long-scroll homepage with inline persona callouts)
- **Approach C:** Interactive Journey Builder (wizard → personalized dynamic page)

## Shared Decisions

**Stack:** Existing Next.js 16 + Tailwind in landing-page-v0/
**Design:** v0.dev-generated components integrated into existing app
**Lead capture:** Existing reCAPTCHA + Resend pipeline (preserved)
**Analytics:** Existing GA4 + Vercel Analytics (preserved)
**Deployment:** Each worktree deploys to its own Vercel preview

**Updated metrics (March 2026):**
- 59 microservices (was 51)
- 1,171 test classes / 8,000+ methods (was 600+)
- 80+ HEDIS measures (was 52)
- Wave-1 revenue cycle shipped
- CVE-remediated, ZAP-scanned, 360-assured

**Progressive CTA hierarchy (all approaches):**
1. Download One-Pager (gated PDF, captures email)
2. Watch Platform Overview (2-min video, ungated)
3. Schedule a Demo (Calendly embed)
4. Start Your 90-Day Pilot (contact form)

**Personas served:**
- Segments: Health Plans, Health Systems, ACOs/Provider Groups
- Roles: CMO/Medical Director, Quality Director, IT/CTO, CFO/Finance

## Approach A: Persona-First Homepage

**Pages (~10):**
- / — Hero + segment selector cards + proof metrics
- /health-plans — CMO, Quality Dir, CFO, IT personas with HEDIS, care gaps, revenue cycle
- /health-systems — CMO, Quality Dir, CTO, CFO with deployment speed, EHR, operations
- /acos — Quality Lead, IT, Finance with population health, reporting
- /platform — Full capabilities (9 sections from investor PLATFORM-OVERVIEW.md)
- /security — Customer-facing security posture
- /pricing — Updated tiers
- /about — Company + founder story
- /sales — 4-step pilot journey (updated)
- /contact — Form + Calendly

**Homepage sections:**
1. Problem headline with cycling text
2. "Built for your organization" — 3 segment cards
3. Proof metrics bar
4. "What's new" highlights
5. Progressive CTA stack

**Segment page sections:**
1. Segment-specific hero
2. Persona cards (3-4 roles with pain → solution)
3. Capabilities through their lens
4. ROI calculator with segment defaults
5. 90-day pilot journey
6. Progressive CTAs

## Approach B: Feature-Story Hybrid

**Pages (~8):**
- / — Long-scroll: Problem → Solution → Capabilities → Proof → Personas → CTA
- /platform — Deep capabilities
- /security — Security posture
- /pricing — Updated tiers
- /about — Company story
- /sales — Sales process
- /performance — Benchmarks (updated)
- /contact — Form + Calendly

**Homepage sections:**
1. Hero: Problem + value prop
2. Capabilities grid (9 expandable cards)
3. Inline persona callouts per capability
4. Proof metrics with animated counters
5. "What's New" highlights
6. Security callout bar
7. Segment ROI snapshots (3 columns)
8. Progressive CTA stack

## Approach C: Interactive Journey Builder

**Pages (~8 + dynamic route):**
- / — Hero + 3-step wizard
- /your-solution — Dynamic page from wizard answers
- /platform — Full capabilities
- /security — Security posture
- /pricing — Updated tiers
- /about — Company story
- /sales — Sales process
- /contact — Form + Calendly

**Wizard steps:**
1. Organization type (Health Plan / Health System / ACO)
2. Primary role (CMO / Quality Dir / IT / CFO)
3. Biggest challenge (HEDIS / Care Gaps / Revenue / Speed / Integration / Reporting)

**Dynamic page generates:** Role-specific headline, 3-5 relevant capabilities, segment ROI, tailored CTA

## Implementation Strategy

Each approach built in its own git worktree branching from master:
- worktree-a: `feature/v3-landing-persona-first`
- worktree-b: `feature/v3-landing-feature-story`
- worktree-c: `feature/v3-landing-journey-builder`

Build order per worktree:
1. Update shared components (metrics, CTAs, messaging)
2. Build approach-specific pages
3. Verify lead capture still works
4. Deploy to Vercel preview
5. Compare side by side

## Success Criteria

- All three approaches deploy successfully
- Lead capture pipeline works on all three
- Updated metrics and messaging reflected
- Progressive CTAs functional
- Mobile responsive
- Lighthouse 90+ performance
