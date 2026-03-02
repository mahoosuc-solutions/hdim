# V3 Landing Page — Three-Approach Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build three v3 landing page variants in parallel worktrees for side-by-side comparison: Persona-First (A), Feature-Story (B), and Journey Builder (C).

**Architecture:** Each approach modifies the existing Next.js 16 app at `landing-page-v0/`. Shared updates (metrics, CTAs, messaging) are built first in each worktree independently, then approach-specific pages are added. Each worktree gets a Vercel preview deployment.

**Tech Stack:** Next.js 16.1.4, React 19, Tailwind CSS 3.4, TypeScript 5, Lucide React, existing Resend + reCAPTCHA lead pipeline

**Base path:** `/mnt/wdblack/dev/projects/hdim-master/landing-page-v0/`

---

## Phase 0: Create Three Worktrees

### Task 0: Create worktrees from master

**Step 1: Create three worktrees**

```bash
cd /mnt/wdblack/dev/projects/hdim-master
git worktree add .claude/worktrees/v3-landing-a -b feature/v3-landing-persona-first
git worktree add .claude/worktrees/v3-landing-b -b feature/v3-landing-feature-story
git worktree add .claude/worktrees/v3-landing-c -b feature/v3-landing-journey-builder
```

**Step 2: Install dependencies in each**

```bash
cd .claude/worktrees/v3-landing-a/landing-page-v0 && npm install
cd /mnt/wdblack/dev/projects/hdim-master/.claude/worktrees/v3-landing-b/landing-page-v0 && npm install
cd /mnt/wdblack/dev/projects/hdim-master/.claude/worktrees/v3-landing-c/landing-page-v0 && npm install
```

**Step 3: Verify each builds**

```bash
cd <worktree>/landing-page-v0 && npm run build
```

Expected: Build succeeds in all three.

---

## Phase 1: Shared Foundation (apply to each worktree)

Each worktree needs these shared updates before approach-specific work begins.

### Task 1: Update site-wide metrics and constants

**Files:**
- Create: `landing-page-v0/lib/constants.ts`

```typescript
// Site-wide metrics — March 2026
export const METRICS = {
  services: 59,
  testClasses: 1171,
  testMethods: '8,000+',
  apiEndpoints: 62,
  hedisMeasures: '80+',
  evaluationSpeed: '<2 seconds',
  deploymentTime: '90 days',
  databases: 29,
  liquibaseChangesets: 199,
  annualSavings: '$722K',
  roi: '1,720%',
  paybackDays: 22,
} as const

export const PERSONAS = {
  healthPlans: {
    label: 'Health Plans',
    slug: 'health-plans',
    description: 'Medicare Advantage, Commercial, Medicaid managed care',
    roles: ['CMO / Medical Director', 'Quality Director', 'CFO / Finance', 'IT / CTO'],
    painPoints: [
      'HEDIS compliance gaps costing $500K-2M per 1% shortfall',
      'Care gaps identified months after intervention window closes',
      'Manual measure processes consuming 40-60% of quality team time',
      'No visibility into custom quality measure performance',
    ],
  },
  healthSystems: {
    label: 'Health Systems',
    slug: 'health-systems',
    description: 'Hospitals, IDNs, academic medical centers',
    roles: ['CMO / Medical Director', 'Quality Director', 'CTO / IT Director', 'CFO'],
    painPoints: [
      'EHR integrations taking 18-24 months with incumbent vendors',
      'Quality data siloed across clinical systems',
      'Revenue cycle and quality measurement on separate platforms',
      'Security and compliance burden growing without automation',
    ],
  },
  acos: {
    label: 'ACOs & Provider Groups',
    slug: 'acos',
    description: 'Accountable Care Organizations, physician groups, IPAs',
    roles: ['Quality Lead', 'IT Director', 'Finance / Operations', 'Medical Director'],
    painPoints: [
      'Population health visibility limited to retrospective reports',
      'Care gap closure rates below targets for shared savings',
      'Reporting burden consuming clinical staff time',
      'Data integration across multiple EHR systems',
    ],
  },
} as const

export const CAPABILITIES = [
  {
    id: 'quality-engine',
    title: 'Quality Measure Engine',
    description: '80+ HEDIS measures with real-time CQL evaluation',
    detail: 'Direct CQL execution on FHIR R4 resources. Under 2-second evaluation per patient. Population-level batch processing with 10 concurrent threads.',
    isNew: false,
  },
  {
    id: 'care-gaps',
    title: 'Care Gap Detection',
    description: 'Automated identification and closure workflows',
    detail: 'Real-time detection as clinical events occur. Closure workflows for clinical teams. Patient engagement integration for outreach.',
    isNew: false,
  },
  {
    id: 'revenue-cycle',
    title: 'Revenue Cycle',
    description: 'Claims, remittance, price transparency',
    detail: 'Wave-1 complete: claims processing with clearinghouse integration, remittance reconciliation (ERA/835), price transparency APIs, ADT event handling.',
    isNew: true,
    badge: 'Wave-1',
  },
  {
    id: 'fhir',
    title: 'FHIR R4 Interoperability',
    description: 'Native integration with Epic, Cerner, Athena',
    detail: '100% FHIR attribute preservation (vs. competitors 30-50%). No ETL translation loss. 62 documented API endpoints with Swagger UI.',
    isNew: false,
  },
  {
    id: 'measure-builder',
    title: 'Custom Measure Builder',
    description: 'Create and deploy custom quality measures via UI',
    detail: 'Health plans can build proprietary measures without engineering involvement. 7 configurable metadata fields. End-to-end tested.',
    isNew: true,
    badge: 'New',
  },
  {
    id: 'cmo-onboarding',
    title: 'CMO Onboarding',
    description: 'Dashboard workflows and acceptance playbooks',
    detail: 'Structured path from evaluation to adoption for health plan CMOs. Validation hooks at each onboarding milestone.',
    isNew: true,
    badge: 'New',
  },
  {
    id: 'clinical-portal',
    title: 'Clinical Portal',
    description: 'Angular-based clinical workspace with operations dashboards',
    detail: 'HIPAA-compliant frontend with PHI filtering, 15-minute session timeout, 100% API audit coverage, 343 ARIA accessibility attributes.',
    isNew: false,
  },
  {
    id: 'operations',
    title: 'Operations Orchestration',
    description: '16-class gateway framework for enterprise operations',
    detail: 'Standardized security, rate limiting, and multi-tenancy across 4 modularized API gateways. New services inherit patterns automatically.',
    isNew: true,
    badge: 'New',
  },
  {
    id: 'security',
    title: 'Security & Compliance',
    description: 'HIPAA engineered, CVE remediated, ZAP scanned',
    detail: 'HIPAA §164.312 compliance at every layer. CVE remediation with burn-down tracking. ZAP scanning on every PR. 360 platform assurance with evidence sign-off.',
    isNew: false,
  },
] as const

export const PROGRESSIVE_CTAS = [
  {
    level: 'low',
    label: 'Download One-Pager',
    description: 'Get the executive summary',
    action: 'download', // gated PDF
  },
  {
    level: 'medium',
    label: 'Watch Platform Overview',
    description: '2-minute walkthrough',
    action: 'video', // ungated
  },
  {
    level: 'high',
    label: 'Schedule a Demo',
    description: '60-minute deep-dive',
    action: 'calendly',
  },
  {
    level: 'highest',
    label: 'Start Your 90-Day Pilot',
    description: 'Observable SLOs from day one',
    action: 'contact',
  },
] as const
```

**Step 1: Write the file**
**Step 2: Verify TypeScript compiles:** `npx tsc --noEmit lib/constants.ts`
**Step 3: Commit**

```bash
git add lib/constants.ts
git commit -m "feat: add shared constants for v3 landing page"
```

---

### Task 2: Create ProgressiveCTA component

**Files:**
- Create: `landing-page-v0/app/components/ProgressiveCTA.tsx`

This component renders the 4-tier CTA hierarchy. Used on every page.

```typescript
'use client'

import { ArrowRight, Download, Play, Calendar, Rocket } from 'lucide-react'

interface ProgressiveCTAProps {
  variant?: 'full' | 'compact' | 'inline'
  highlight?: 'low' | 'medium' | 'high' | 'highest'
}

export function ProgressiveCTA({ variant = 'full', highlight }: ProgressiveCTAProps) {
  const ctas = [
    {
      level: 'low' as const,
      label: 'Download One-Pager',
      sub: 'Get the executive summary',
      icon: Download,
      href: '/downloads/hdim-one-pager.pdf',
      style: 'border border-gray-300 hover:border-primary-400 text-gray-700',
    },
    {
      level: 'medium' as const,
      label: 'Watch Overview',
      sub: '2-minute walkthrough',
      icon: Play,
      href: '#video',
      style: 'border border-primary-200 bg-primary-50 text-primary-700 hover:bg-primary-100',
    },
    {
      level: 'high' as const,
      label: 'Schedule a Demo',
      sub: '60-minute deep-dive',
      icon: Calendar,
      href: '/schedule',
      style: 'bg-primary-600 text-white hover:bg-primary-700',
    },
    {
      level: 'highest' as const,
      label: 'Start Your 90-Day Pilot',
      sub: 'Observable SLOs from day one',
      icon: Rocket,
      href: '/contact?intent=pilot',
      style: 'bg-accent-600 text-white hover:bg-accent-700',
    },
  ]

  if (variant === 'inline') {
    // Single highlighted CTA for inline use
    const cta = ctas.find(c => c.level === (highlight || 'high'))!
    return (
      <a href={cta.href} className={`inline-flex items-center gap-2 px-6 py-3 rounded-lg font-semibold transition-all ${cta.style}`}>
        <cta.icon className="w-5 h-5" />
        {cta.label}
        <ArrowRight className="w-4 h-4" />
      </a>
    )
  }

  if (variant === 'compact') {
    return (
      <div className="flex flex-wrap gap-3">
        {ctas.slice(2).map(cta => (
          <a key={cta.level} href={cta.href} className={`inline-flex items-center gap-2 px-5 py-2.5 rounded-lg font-medium transition-all ${cta.style}`}>
            <cta.icon className="w-4 h-4" />
            {cta.label}
          </a>
        ))}
      </div>
    )
  }

  // Full variant: all 4 CTAs in a grid
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {ctas.map(cta => (
        <a key={cta.level} href={cta.href} className={`flex flex-col items-center gap-2 p-6 rounded-xl text-center transition-all ${cta.style} ${highlight === cta.level ? 'ring-2 ring-primary-500 scale-105' : ''}`}>
          <cta.icon className="w-8 h-8" />
          <span className="font-semibold">{cta.label}</span>
          <span className="text-sm opacity-75">{cta.sub}</span>
        </a>
      ))}
    </div>
  )
}
```

**Step 1: Write the file**
**Step 2: Verify build:** `npm run build`
**Step 3: Commit**

```bash
git add app/components/ProgressiveCTA.tsx
git commit -m "feat: add progressive CTA component with 4-tier hierarchy"
```

---

### Task 3: Create MetricsBar component

**Files:**
- Create: `landing-page-v0/app/components/MetricsBar.tsx`

Proof metrics strip with animated counters. Shows the key numbers.

```typescript
import { AnimatedCounter } from './AnimatedCounter'

export function MetricsBar() {
  const metrics = [
    { value: 59, label: 'Microservices', suffix: '' },
    { value: 1171, label: 'Test Classes', suffix: '' },
    { value: 80, label: 'HEDIS Measures', suffix: '+' },
    { value: 62, label: 'API Endpoints', suffix: '' },
    { value: 2, label: 'Second Evaluation', prefix: '<' },
    { value: 90, label: 'Day Deployment', suffix: '' },
  ]

  return (
    <section className="bg-gray-50 border-y border-gray-200 py-8">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-6 text-center">
          {metrics.map((m) => (
            <div key={m.label}>
              <div className="text-3xl font-bold text-primary-700">
                {m.prefix || ''}<AnimatedCounter target={m.value} />{m.suffix || ''}
              </div>
              <div className="text-sm text-gray-500 mt-1">{m.label}</div>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
```

**Step 1: Write the file**
**Step 2: Verify build**
**Step 3: Commit**

```bash
git add app/components/MetricsBar.tsx
git commit -m "feat: add metrics bar with updated March 2026 numbers"
```

---

### Task 4: Create WhatsNew component

**Files:**
- Create: `landing-page-v0/app/components/WhatsNew.tsx`

Highlights the February 2026 capabilities that weren't on the old site.

```typescript
import { Sparkles, DollarSign, Wrench, Shield, Users } from 'lucide-react'

const newCapabilities = [
  {
    icon: DollarSign,
    title: 'Revenue Cycle (Wave-1)',
    description: 'Claims processing, remittance reconciliation, and price transparency — integrated on the same event-driven backbone.',
    badge: 'Wave-1',
  },
  {
    icon: Wrench,
    title: 'Custom Measure Builder',
    description: 'Create and deploy custom quality measures via UI. No engineering involvement required.',
    badge: 'New',
  },
  {
    icon: Users,
    title: 'CMO Onboarding',
    description: 'Dashboard workflows and acceptance playbooks for structured evaluation-to-adoption.',
    badge: 'New',
  },
  {
    icon: Shield,
    title: 'Security Hardening',
    description: 'CVE remediation, ZAP scanning, 360 platform assurance — evidence-based, not checkbox compliance.',
    badge: 'Hardened',
  },
]

export function WhatsNew() {
  return (
    <section className="py-16 bg-white">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center mb-12">
          <div className="inline-flex items-center gap-2 text-accent-600 font-semibold text-sm mb-3">
            <Sparkles className="w-4 h-4" />
            Shipped February 2026
          </div>
          <h2 className="text-3xl font-bold text-gray-900">What&apos;s New</h2>
          <p className="mt-3 text-gray-500 max-w-2xl mx-auto">
            444 commits shipped in February. Here are the highlights.
          </p>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          {newCapabilities.map((cap) => (
            <div key={cap.title} className="flex gap-4 p-6 rounded-xl border border-gray-200 hover:shadow-lg transition-shadow">
              <div className="flex-shrink-0 w-12 h-12 rounded-lg bg-primary-50 flex items-center justify-center">
                <cap.icon className="w-6 h-6 text-primary-600" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="font-semibold text-gray-900">{cap.title}</h3>
                  <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-accent-100 text-accent-700">{cap.badge}</span>
                </div>
                <p className="mt-1 text-sm text-gray-500">{cap.description}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
```

**Step 1: Write the file**
**Step 2: Verify build**
**Step 3: Commit**

```bash
git add app/components/WhatsNew.tsx
git commit -m "feat: add What's New section for February 2026 capabilities"
```

---

## Phase 2A: Approach A — Persona-First Homepage

**Worktree:** `.claude/worktrees/v3-landing-a/landing-page-v0/`

### Task 5A: Build the homepage with segment selector

**Files:**
- Modify: `app/page.tsx` — Replace current homepage with segment-selector design

**Homepage structure:**
1. Hero with cycling text and problem statement
2. "Built for your organization" — 3 segment cards (Health Plans, Health Systems, ACOs) that link to `/health-plans`, `/health-systems`, `/acos`
3. MetricsBar component
4. WhatsNew component
5. ProgressiveCTA (full variant)

**Step 1: Rewrite app/page.tsx with segment cards as the primary navigation pattern**
**Step 2: Verify build:** `npm run build`
**Step 3: Commit**

### Task 6A: Build /health-plans segment page

**Files:**
- Create: `app/health-plans/page.tsx`

**Structure:**
1. Segment hero: "For Health Plans: Close care gaps before they cost you"
2. Persona cards — 4 roles (CMO, Quality Director, CFO, IT) each with:
   - Role title
   - Pain point specific to that role
   - How HDIM solves it
   - 2-3 relevant capabilities
3. Capabilities section — filtered to most relevant for health plans: Quality Engine, Care Gaps, Custom Measure Builder, CMO Onboarding, Revenue Cycle
4. ROI section with health plan-specific numbers ($500K-2M per 1% gap)
5. "How we work together" — 90-day pilot journey
6. ProgressiveCTA

**Step 1: Write the page**
**Step 2: Verify build**
**Step 3: Commit**

### Task 7A: Build /health-systems segment page

**Files:**
- Create: `app/health-systems/page.tsx`

Same structure as health-plans but with health system-specific:
- Pain points: EHR integration speed, siloed quality data, separate platforms
- Capabilities emphasis: FHIR R4, Operations Orchestration, Security, Clinical Portal
- ROI: deployment speed (90 days vs 18-24 months), EHR integration

**Step 1: Write the page**
**Step 2: Verify build**
**Step 3: Commit**

### Task 8A: Build /acos segment page

**Files:**
- Create: `app/acos/page.tsx`

Same structure with ACO-specific:
- Pain points: population health visibility, care gap closure rates, multi-EHR integration
- Capabilities emphasis: Quality Engine, Care Gaps, FHIR R4, Clinical Portal
- ROI: shared savings improvement, reporting automation

**Step 1: Write the page**
**Step 2: Verify build**
**Step 3: Commit**

### Task 9A: Build /platform page

**Files:**
- Create: `app/platform/page.tsx`

Full capabilities overview showing all 9 capabilities from CAPABILITIES constant. Each with expandable detail. "New" badges on Wave-1, Measure Builder, CMO Onboarding, Operations.

**Step 1: Write the page**
**Step 2: Verify build**
**Step 3: Commit**

### Task 10A: Build /security page

**Files:**
- Create: `app/security/page.tsx`

Customer-facing security posture page. Sections: HIPAA compliance, CVE remediation, security scanning, multi-tenant isolation, audit trails. Based on investor SECURITY-COMPLIANCE.md but in customer-facing tone.

**Step 1: Write the page**
**Step 2: Verify build**
**Step 3: Commit**

### Task 11A: Update navigation and sitemap

**Files:**
- Modify: `app/components/LandingPageClient.tsx` — Update nav links
- Modify: `app/sitemap.ts` — Add new routes

Replace current nav with: Home, Health Plans, Health Systems, ACOs, Platform, Security, Pricing, About, Contact

**Step 1: Update files**
**Step 2: Verify build and all links work**
**Step 3: Commit**

### Task 12A: Remove deprecated pages

**Files:**
- Delete: `app/explorer/` (fold into segment pages)
- Delete: `app/downloads/` (fold into one-pager CTA)
- Delete: `app/research/` (fold into platform page)
- Keep: `app/features/` as redirect to `/platform`
- Keep: `app/demo/`, `app/performance/` as secondary pages

**Step 1: Remove directories, add redirects in next.config.js**
**Step 2: Verify build**
**Step 3: Final commit**

```bash
git add -A
git commit -m "feat: complete Approach A — persona-first homepage with segment pages"
```

---

## Phase 2B: Approach B — Feature-Story Hybrid

**Worktree:** `.claude/worktrees/v3-landing-b/landing-page-v0/`

### Task 5B: Build long-scroll homepage

**Files:**
- Modify: `app/page.tsx`

**Homepage sections (linear scroll):**
1. Hero with problem statement + value prop
2. Capabilities grid — 9 cards from CAPABILITIES, each expandable
3. Inline persona callouts — after each capability group, a "What this means for [role]" sidebar
4. MetricsBar component
5. WhatsNew component
6. Security callout bar (dark bg, key security proof points)
7. Segment ROI snapshots — 3 columns (Health Plans, Health Systems, ACOs) with key ROI numbers
8. ProgressiveCTA (full variant)

**Step 1: Write the full homepage**
**Step 2: Verify build**
**Step 3: Commit**

### Task 6B: Create PersonaCallout component

**Files:**
- Create: `app/components/PersonaCallout.tsx`

Inline sidebar component showing "What this means for [role]" with icon, role name, and 2-sentence value prop. Used between capability sections on the homepage.

**Step 1: Write component**
**Step 2: Verify build**
**Step 3: Commit**

### Task 7B: Create SecurityBar component

**Files:**
- Create: `app/components/SecurityBar.tsx`

Dark background callout bar with key security stats: HIPAA compliant, CVE-remediated, ZAP-scanned, 100% audit coverage, multi-tenant isolation. Links to /security page.

**Step 1: Write component**
**Step 2: Verify build**
**Step 3: Commit**

### Task 8B: Create SegmentROI component

**Files:**
- Create: `app/components/SegmentROI.tsx`

3-column section with segment-specific ROI snapshots. Each column: segment name, key metric, and 2-3 bullet points.

**Step 1: Write component**
**Step 2: Verify build**
**Step 3: Commit**

### Task 9B: Build /platform page (same as 9A)
### Task 10B: Build /security page (same as 10A)

### Task 11B: Update navigation

Nav: Home, Platform, Security, Pricing, About, Sales, Contact

**Step 1: Update nav and sitemap**
**Step 2: Remove deprecated pages (same as 12A)**
**Step 3: Final commit**

```bash
git add -A
git commit -m "feat: complete Approach B — feature-story hybrid homepage"
```

---

## Phase 2C: Approach C — Interactive Journey Builder

**Worktree:** `.claude/worktrees/v3-landing-c/landing-page-v0/`

### Task 5C: Build homepage with wizard

**Files:**
- Modify: `app/page.tsx`

**Homepage sections:**
1. Hero with problem statement
2. JourneyWizard component (the interactive 3-step flow)
3. MetricsBar component (below wizard for visitors who scroll past)
4. Brief capabilities overview (3 cards: Quality, Revenue, Operations)
5. ProgressiveCTA (compact variant)

**Step 1: Write the homepage**
**Step 2: Verify build**
**Step 3: Commit**

### Task 6C: Create JourneyWizard component

**Files:**
- Create: `app/components/JourneyWizard.tsx`

Interactive 3-step wizard:

```
Step 1: "What type of organization are you?"
  → 3 cards: Health Plan, Health System, ACO/Provider Group

Step 2: "What's your primary role?"
  → 4 cards (dynamically populated based on step 1 selection from PERSONAS constant)

Step 3: "What's your biggest challenge?"
  → 4-6 cards: HEDIS compliance, Care gap identification, Revenue cycle,
    Implementation speed, Data integration, Reporting

→ Button: "See Your Solution" → navigates to /your-solution?segment=X&role=Y&challenge=Z
```

State managed with useState. Animated transitions between steps. Back button on steps 2+3.

**Step 1: Write the component**
**Step 2: Verify build**
**Step 3: Commit**

### Task 7C: Build /your-solution dynamic page

**Files:**
- Create: `app/your-solution/page.tsx`

Reads query params: segment, role, challenge. Generates personalized content:

1. Role-specific headline: "For [Role] at [Segment Type]s"
2. Challenge-specific lead: "You told us [challenge] is your biggest pain point. Here's how HDIM solves it."
3. 3-5 most relevant capabilities (filtered from CAPABILITIES based on segment + challenge)
4. Segment-specific ROI numbers
5. "What [similar role]s are saying" — testimonial placeholder
6. ProgressiveCTA (highlight the 'high' tier — Schedule a Demo)

Fallback: if no params, show all capabilities with segment selector.

**Step 1: Write the page**
**Step 2: Verify build**
**Step 3: Commit**

### Task 8C: Build content mapping logic

**Files:**
- Create: `landing-page-v0/lib/journey-mapper.ts`

Maps wizard selections to content:

```typescript
// Maps (segment, role, challenge) → relevant capabilities, headline, ROI data
export function mapJourneyToContent(
  segment: string,
  role: string,
  challenge: string
): {
  headline: string
  subheadline: string
  capabilities: typeof CAPABILITIES[number][]
  roi: { metric: string; value: string }[]
}
```

Contains the content mapping logic. Each combination of segment + challenge maps to 3-5 capabilities from the CAPABILITIES array.

**Step 1: Write the mapper**
**Step 2: Verify TypeScript compiles**
**Step 3: Commit**

### Task 9C: Build /platform page (same as 9A)
### Task 10C: Build /security page (same as 10A)

### Task 11C: Update navigation and cleanup

Nav: Home, Platform, Security, Pricing, About, Sales, Contact

**Step 1: Update nav, sitemap, remove deprecated pages**
**Step 2: Final commit**

```bash
git add -A
git commit -m "feat: complete Approach C — interactive journey builder"
```

---

## Phase 3: Verify and Preview

### Task 13: Build and verify all three

**For each worktree:**

```bash
cd <worktree>/landing-page-v0
npm run build          # Must succeed
npm run lint           # Zero warnings
```

Verify:
- [ ] Lead capture form still works (reCAPTCHA + Resend)
- [ ] Analytics scripts present (GA4 + Vercel)
- [ ] All internal links resolve
- [ ] Mobile responsive (check at 375px, 768px, 1024px widths)
- [ ] Progressive CTAs render on every page

### Task 14: Deploy previews

Each branch gets a Vercel preview:

```bash
# From each worktree
cd <worktree>/landing-page-v0
npx vercel --yes  # Deploys preview
```

Or push branches and let Vercel auto-deploy:

```bash
git push origin feature/v3-landing-persona-first
git push origin feature/v3-landing-feature-story
git push origin feature/v3-landing-journey-builder
```

Collect three preview URLs for side-by-side comparison.

---

## Comparison Criteria

After all three are deployed, compare on:

| Criterion | A (Persona-First) | B (Feature-Story) | C (Journey Builder) |
|-----------|--------------------|--------------------|---------------------|
| Time to relevance | ? | ? | ? |
| Mobile experience | ? | ? | ? |
| Conversion clarity | ? | ? | ? |
| Content depth | ? | ? | ? |
| Build complexity | Low-Medium | Low | Medium-High |
| Maintenance burden | Medium (3 segment pages) | Low (1 homepage) | Medium (content mapping) |
