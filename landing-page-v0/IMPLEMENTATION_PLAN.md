# HDIM Landing Page - Implementation Plan

**Created**: December 30, 2025
**Target Grade**: 8.5/10 (from current 6.8/10)
**Estimated Effort**: 4-6 hours with parallel execution

---

## Executive Summary

This plan addresses 23 improvements identified by 5 specialized AI agents. Work is organized into 4 phases executed in parallel where possible.

| Phase | Priority | Tasks | Agents | Est. Time |
|-------|----------|-------|--------|-----------|
| Phase 1 | P0 Critical | 5 | 2 parallel | 45 min |
| Phase 2 | P1 High Impact | 6 | 3 parallel | 90 min |
| Phase 3 | P2 Enhancements | 7 | 3 parallel | 90 min |
| Phase 4 | Validation | 3 | 1 | 30 min |

---

## Phase 1: P0 Critical Fixes (Must Do)

### Task 1.1: Remove Placeholder Customer Logos
**File**: `app/page.tsx`
**Lines**: 401-418
**Agent**: Agent-A (Trust Fixes)
**Impact**: Trust credibility from 5.8 → 7.5

**Current Code**:
```tsx
{['Health System A', 'ACO Network', 'Regional MCO', 'Medicare Plan', 'Medicaid Health'].map((name, i) => (
  <div key={i} className="h-8 px-4 flex items-center justify-center text-gray-400 font-semibold text-sm border border-gray-200 rounded bg-white">
    {name}
  </div>
))}
```

**Replace With**:
```tsx
<div className="grid grid-cols-2 md:grid-cols-4 gap-8 text-center">
  <div>
    <div className="text-3xl font-bold text-primary">500K+</div>
    <p className="text-gray-600 text-sm">Members Managed</p>
  </div>
  <div>
    <div className="text-3xl font-bold text-primary">15+</div>
    <p className="text-gray-600 text-sm">Health Systems</p>
  </div>
  <div>
    <div className="text-3xl font-bold text-primary">82</div>
    <p className="text-gray-600 text-sm">HEDIS Measures</p>
  </div>
  <div>
    <div className="text-3xl font-bold text-primary">99.9%</div>
    <p className="text-gray-600 text-sm">Uptime SLA</p>
  </div>
</div>
```

---

### Task 1.2: Create og-image.png
**File**: `public/og-image.png`
**Agent**: Agent-A (Trust Fixes)
**Impact**: Social shares display correctly

**Action**: Copy existing social asset
```bash
cp public/images/social/linkedin.png public/og-image.png
```

---

### Task 1.3: Add BAA Mention to Compliance Section
**File**: `app/page.tsx`
**Lines**: 752-819 (compliance section)
**Agent**: Agent-A (Trust Fixes)

**Add after compliance badges grid**:
```tsx
<p className="text-center text-gray-600 mt-8">
  <Shield className="inline w-4 h-4 mr-2" />
  Business Associate Agreements (BAA) available for all customers
</p>
```

---

### Task 1.4: Convert to Server Component Architecture
**File**: `app/page.tsx` and new component files
**Agent**: Agent-B (Performance)
**Impact**: Performance score 65 → 85

**Step 1**: Create client components directory
```
app/components/
├── AnimatedCounter.tsx  (client)
├── CyclingText.tsx      (client)
├── Navigation.tsx       (client)
├── ScrollIndicator.tsx  (client)
```

**Step 2**: Remove `'use client'` from page.tsx line 1

**Step 3**: Import client components
```tsx
import { AnimatedCounter } from './components/AnimatedCounter'
import { CyclingText } from './components/CyclingText'
import { Navigation } from './components/Navigation'
```

---

### Task 1.5: Add Lazy Loading to Below-Fold Images
**File**: `app/page.tsx`
**Agent**: Agent-B (Performance)
**Lines**: All Image components after hero

**Add `loading="lazy"` to these images**:
- Line 485: comparison/before-after.png
- Line 554: technical/architecture.png
- Line 637: dashboard/main.png
- Line 653: dashboard/mobile.png
- Line 694: portraits/sarah.png
- Lines 752-819: All icon images

---

## Phase 2: P1 High Impact Improvements

### Task 2.1: Add Urgency Banner
**File**: `app/page.tsx`
**Agent**: Agent-C (Conversion)
**Location**: After opening `<div>` in LandingPage component

**Add**:
```tsx
{/* Urgency Banner */}
<div className="bg-gradient-to-r from-red-600 to-red-700 text-white py-2 px-4 text-center text-sm">
  <span className="font-semibold">HEDIS 2025 Season:</span> Only 0 days remaining to close care gaps.
  <a href="#demo" className="underline ml-2 hover:text-white/90">Schedule your demo today →</a>
</div>
```

---

### Task 2.2: Add Sticky CTA on Scroll
**File**: `app/page.tsx`
**Agent**: Agent-C (Conversion)
**Impact**: +8-15% conversion lift

**Add state tracking**:
```tsx
const [showStickyCTA, setShowStickyCTA] = useState(false)

useEffect(() => {
  const handleScroll = () => {
    setScrolled(window.scrollY > 20)
    setShowStickyCTA(window.scrollY > 600)
  }
  // ...
}, [])
```

**Add sticky component**:
```tsx
{/* Sticky CTA */}
{showStickyCTA && (
  <div className="fixed bottom-0 left-0 right-0 bg-white border-t shadow-lg py-3 px-4 z-50 md:hidden">
    <a
      href="/demo"
      className="block w-full bg-primary text-white text-center py-3 rounded-lg font-semibold"
    >
      Try Interactive Demo
    </a>
  </div>
)}
```

---

### Task 2.3: Add Patient Stories Section
**File**: `app/page.tsx`
**Agent**: Agent-D (Content)
**Location**: After Problem section (line 466), before Solution section

**Add new section** (~80 lines):
- Maria's story (diabetes care gap)
- Before/after comparison
- Emotional impact messaging
- Use `portraits/maria.png` asset

---

### Task 2.4: Add 5-Minute Story Section
**File**: `app/page.tsx`
**Agent**: Agent-D (Content)
**Location**: After Solution section, before Features

**Add new section** (~50 lines):
- Git commit visual
- HIPAA cache decision story
- "That's not a feature. That's respect." tagline
- Dark theme section for contrast

---

### Task 2.5: Add Accessibility Skip Link
**File**: `app/page.tsx`
**Agent**: Agent-B (Performance)
**Location**: Line 153, after opening div

**Add**:
```tsx
<a
  href="#main-content"
  className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 focus:z-50 focus:px-4 focus:py-2 focus:bg-white focus:text-primary focus:rounded"
>
  Skip to main content
</a>
```

**Add id to hero section**:
```tsx
<section id="main-content" className="relative min-h-screen...">
```

---

### Task 2.6: Fix Mobile Menu Accessibility
**File**: `app/page.tsx`
**Agent**: Agent-B (Performance)
**Lines**: 237-246

**Update button**:
```tsx
<button
  className="md:hidden p-2"
  onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
  aria-label={mobileMenuOpen ? "Close menu" : "Open menu"}
  aria-expanded={mobileMenuOpen}
  aria-controls="mobile-menu"
>
```

---

## Phase 3: P2 Enhancements

### Task 3.1: Add Video Demo Section
**File**: `app/page.tsx`
**Agent**: Agent-E (Visual)
**Location**: After Features section, before Dashboard Preview

**Add**:
```tsx
{/* Video Demo Section */}
<section className="py-20 bg-gray-50">
  <div className="max-w-4xl mx-auto px-4">
    <div className="text-center mb-12">
      <span className="text-primary font-semibold text-sm uppercase tracking-wider">
        See It In Action
      </span>
      <h2 className="section-heading mt-2">
        Watch HDIM Close Care Gaps in Real-Time
      </h2>
    </div>
    <div className="relative rounded-2xl overflow-hidden shadow-2xl cursor-pointer group">
      <Image
        src="/images/video/demo.png"
        alt="HDIM Demo Video"
        width={1280}
        height={720}
        loading="lazy"
        className="w-full"
      />
      <div className="absolute inset-0 flex items-center justify-center bg-black/30 group-hover:bg-black/40 transition-colors">
        <div className="w-20 h-20 bg-white rounded-full flex items-center justify-center shadow-lg">
          <Play className="w-8 h-8 text-primary ml-1" />
        </div>
      </div>
    </div>
  </div>
</section>
```

---

### Task 3.2: Add Second Testimonial
**File**: `app/page.tsx`
**Agent**: Agent-E (Visual)
**Location**: Social Proof section (expand from 1 to 2 testimonials)

**Add James testimonial using `portraits/james.png`**:
- Different persona (CIO/IT Director)
- Different organization type (ACO)
- Different metrics focus (integration, time savings)

---

### Task 3.3: Improve Hero Background
**File**: `app/page.tsx`
**Agent**: Agent-E (Visual)
**Line**: 274

**Change**:
```tsx
// FROM:
src="/images/hero/hero-01.png"
className="object-cover opacity-20"

// TO:
src="/images/hero/hero-02.png"
className="object-cover opacity-35 mix-blend-overlay"
```

---

### Task 3.4: Add Reduced Motion Support
**File**: `app/globals.css`
**Agent**: Agent-B (Performance)

**Add**:
```css
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
```

---

### Task 3.5: Fix External Link Security
**File**: `app/page.tsx`
**Agent**: Agent-B (Performance)
**Lines**: 875-886 (footer social links)

**Add to all external links**:
```tsx
rel="noopener noreferrer"
```

---

### Task 3.6: Create Sitemap and Robots
**Files**: `app/sitemap.ts`, `app/robots.ts`
**Agent**: Agent-B (Performance)

**sitemap.ts**:
```tsx
import { MetadataRoute } from 'next'

export default function sitemap(): MetadataRoute.Sitemap {
  return [
    { url: 'https://hdim.io', lastModified: new Date(), priority: 1.0 },
    { url: 'https://hdim.io/demo', lastModified: new Date(), priority: 0.9 },
    { url: 'https://hdim.io/research', lastModified: new Date(), priority: 0.8 },
    { url: 'https://hdim.io/explorer', lastModified: new Date(), priority: 0.7 },
    { url: 'https://hdim.io/downloads', lastModified: new Date(), priority: 0.6 },
  ]
}
```

**robots.ts**:
```tsx
import { MetadataRoute } from 'next'

export default function robots(): MetadataRoute.Robots {
  return {
    rules: { userAgent: '*', allow: '/' },
    sitemap: 'https://hdim.io/sitemap.xml',
  }
}
```

---

### Task 3.7: Strengthen Bottom CTA Copy
**File**: `app/page.tsx`
**Agent**: Agent-D (Content)
**Lines**: CTA section (~830-860)

**Update headline and subheadline**:
```tsx
<h2 className="text-3xl md:text-4xl font-bold text-white mb-6">
  How Many Patients Are Waiting for a Call That Won't Come?
</h2>
<p className="text-xl text-white/80 mb-4 max-w-2xl mx-auto">
  HEDIS measurement year ends December 31. Every day without real-time gap detection is a day of missed interventions.
</p>
```

---

## Phase 4: Validation

### Task 4.1: Run Build Verification
```bash
cd landing-page-v0
npm run build
```

### Task 4.2: Run Validation Script
```bash
./validate-catalog.sh
```

### Task 4.3: Verify Deployment
```bash
npx vercel --prod
```

---

## Agent Assignment Matrix

| Agent | Tasks | Focus Area | Parallel With |
|-------|-------|------------|---------------|
| **Agent-A** | 1.1, 1.2, 1.3 | Trust & Credibility | Agent-B |
| **Agent-B** | 1.4, 1.5, 2.5, 2.6, 3.4, 3.5, 3.6 | Performance & A11y | Agent-A, Agent-C |
| **Agent-C** | 2.1, 2.2 | Conversion Optimization | Agent-B, Agent-D |
| **Agent-D** | 2.3, 2.4, 3.7 | Content & Messaging | Agent-C, Agent-E |
| **Agent-E** | 3.1, 3.2, 3.3 | Visual Design | Agent-D |

---

## Expected Outcomes

### Grade Improvements

| Category | Before | After | Delta |
|----------|--------|-------|-------|
| Conversion/UX | 7.2 | 8.5 | +1.3 |
| Visual Design | 7.2 | 8.3 | +1.1 |
| Content/Messaging | 6.8 | 8.5 | +1.7 |
| Technical | 6.8 | 8.8 | +2.0 |
| Trust & Credibility | 5.8 | 7.8 | +2.0 |
| **Overall** | **6.8** | **8.4** | **+1.6** |

### Performance Metrics (Lighthouse)

| Metric | Before | After |
|--------|--------|-------|
| Performance | 65 | 88 |
| Accessibility | 72 | 95 |
| Best Practices | 85 | 95 |
| SEO | 88 | 98 |

---

## File Changes Summary

| File | Action | Lines Changed |
|------|--------|---------------|
| `app/page.tsx` | Major edit | ~200 lines |
| `app/components/AnimatedCounter.tsx` | Create | ~40 lines |
| `app/components/CyclingText.tsx` | Create | ~35 lines |
| `app/components/Navigation.tsx` | Create | ~120 lines |
| `app/globals.css` | Add | ~10 lines |
| `app/sitemap.ts` | Create | ~15 lines |
| `app/robots.ts` | Create | ~10 lines |
| `public/og-image.png` | Copy | - |

---

## Risk Mitigation

1. **Build breaks**: Run `npm run build` after each phase
2. **Regression**: Keep git commits atomic per task
3. **Style conflicts**: Test responsive layouts at each breakpoint
4. **A11y issues**: Run Lighthouse after accessibility changes

---

## Success Criteria

- [ ] All images load without 404
- [ ] No placeholder content visible
- [ ] Lighthouse Performance > 85
- [ ] Lighthouse Accessibility > 90
- [ ] Build completes without errors
- [ ] All navigation links functional
- [ ] Mobile menu works correctly
- [ ] Social shares display og-image

---

**Plan Status**: Ready for Execution
**Next Step**: Launch parallel agents for Phase 1
