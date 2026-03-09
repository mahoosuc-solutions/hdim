# HDIM Story Punch List: From Node.js Prototype to Purpose-Built Java Platform

Date: 2026-03-09
Owner: GTM + Product + Architecture
Scope: `landing-page/src/app/resources/*` and `docs/marketing/web/*`

## Objective
Create one coherent buyer-facing narrative that explains why HDIM moved from a working Node.js prototype to a purpose-built Java platform, what changed, what was preserved, and why the result is commercially safer and faster to deploy.

## Current State Summary
- The transformation story exists, but it is fragmented across `origin-story`, `executive-summary`, `java-rebuild`, and legacy HTML pages.
- Technical rationale is strong, but buyer-facing risk reduction language is under-emphasized.
- Evidence links exist, but story-to-proof mapping is not consistently visible on each page.
- Navigation does not currently force a clear reading order for non-technical buyers.

## Priority 0 (Do First): Narrative Spine and Message Consistency

1. Add a shared "Transformation Narrative" block to top-level story pages.
- Insert into:
  - `landing-page/src/app/resources/origin-story/page.tsx`
  - `landing-page/src/app/resources/executive-summary/page.tsx`
  - `landing-page/src/app/resources/java-rebuild/page.tsx`
- Content requirements:
  - One sentence each for: problem, decision date, why Java, measurable outcome.
  - Use absolute date: `November 24, 2024` for rebuild decision.
  - Keep terminology consistent: "Node.js prototype" and "purpose-built Java platform".
- Acceptance criteria:
  - The same 4-line narrative appears consistently on all three pages.

2. Create a single before/after table for business and technical readers.
- Insert into:
  - `landing-page/src/app/resources/executive-summary/page.tsx`
  - `docs/marketing/web/sales-narrative.html`
- Content requirements:
  - Columns: `Node.js Prototype`, `Java Platform`, `Business Impact`.
  - Rows include: ecosystem fit, compliance posture, testing maturity, onboarding/maintainability, scale profile.
- Acceptance criteria:
  - A non-technical executive can explain why the rewrite lowered delivery risk in under 60 seconds.

3. Add a fixed reading path CTA cluster.
- Insert into:
  - `landing-page/src/app/resources/ResourcesHub.tsx`
  - `landing-page/src/app/resources/technical/TechnicalHub.tsx`
  - `landing-page/src/app/resources/executive/ExecutiveHub.tsx`
- Content requirements:
  - Ordered path: `Origin Story -> Executive Summary -> Java Rebuild -> Architecture Evolution -> Evidence Room`.
- Acceptance criteria:
  - Every persona hub has at least one CTA into this exact path.

## Priority 1: Buyer Confidence and Diligence Readiness

4. Add "What we kept vs what we replaced" visual section.
- Insert into:
  - `landing-page/src/app/resources/java-rebuild/page.tsx`
  - `docs/marketing/web/java-rebuild-deep-dive.html`
- Content requirements:
  - Two-column card grid:
    - Kept: domain boundaries, FHIR canonical model, tenant isolation intent, REST contracts.
    - Replaced: migration strategy, auth trust pattern, event backbone, observability stack.
- Acceptance criteria:
  - Page clearly shows this was disciplined evolution, not a restart from scratch.

5. Add explicit risk-reduction claims with proof links.
- Insert into:
  - `landing-page/src/app/resources/executive-summary/page.tsx`
  - `landing-page/src/app/resources/trust-center/page.tsx` (or equivalent trust surface)
- Content requirements:
  - Claims: reduced integration risk, reduced compliance drift risk, reduced ops scaling risk.
  - Each claim includes a proof link to relevant evidence/report pages.
- Acceptance criteria:
  - No major claim appears without adjacent proof path.

6. Add "decision log highlights" for architecture credibility.
- Insert into:
  - `landing-page/src/app/resources/architecture-evolution/page.tsx`
- Content requirements:
  - 5 key decision points with date, trigger, decision, outcome.
  - Include rebuild decision (`November 24, 2024`) and Liquibase standardization milestone.
- Acceptance criteria:
  - Timeline reads as governed engineering decisions, not anecdotal narrative.

## Priority 2: Sales Enablement Content

7. Expand sales narrative page from outline to field-ready talk track.
- Update:
  - `docs/marketing/web/sales-narrative.html`
- Content requirements:
  - Add sections:
    - 30-second opener
    - 2-minute story
    - objection handling ("Why rebuild?", "Single architect risk?", "Can my team run this?")
    - proof pack links by buyer type (CIO, CISO, clinical leader, technical evaluator)
- Acceptance criteria:
  - A salesperson can run a first-call narrative without additional prep docs.

8. Add "customer outcomes" section with clinical and operational framing.
- Insert into:
  - `landing-page/src/app/resources/origin-story/page.tsx`
  - `landing-page/src/app/resources/executive-summary/page.tsx`
- Content requirements:
  - Translate platform features into outcomes:
    - faster care-gap closure cycles
    - clearer audit traceability
    - reduced implementation coordination overhead
- Acceptance criteria:
  - Story is outcome-led, not only engineering-led.

9. Standardize terminology glossary.
- Add page or section in:
  - `landing-page/src/app/resources/technical/page.tsx` or dedicated glossary page.
- Terms to normalize:
  - spec-driven development, gateway trust, entity-migration validation, evidence freshness, purpose-built.
- Acceptance criteria:
  - Same term definitions appear across all major narrative pages.

## Priority 3: Editorial and Trust Hygiene

10. Copy-edit all transformation pages for consistency and grammar.
- Review files:
  - `landing-page/src/app/resources/origin-story/page.tsx`
  - `landing-page/src/app/resources/executive-summary/page.tsx`
  - `landing-page/src/app/resources/java-rebuild/page.tsx`
  - `docs/marketing/web/ai-solutioning-journey.html`
  - `docs/marketing/web/java-rebuild-deep-dive.html`
- Focus:
  - consistent tense, no duplicate claims, no conflicting metrics.
- Acceptance criteria:
  - One canonical metric set across all pages.

11. Add "last validated" stamp on key evidence-linked pages.
- Insert into:
  - executive, trust, technical surfaces.
- Content requirements:
  - ISO date + timezone + validator source.
- Acceptance criteria:
  - Buyers can see evidence recency at a glance.

12. Add lightweight story QA checklist in repo.
- Create:
  - `docs/marketing/web/STORY_QA_CHECKLIST.md`
- Include checks:
  - claim-to-proof link present
  - date specificity present
  - persona CTA present
  - no metric conflicts
- Acceptance criteria:
  - Content updates can be validated before deployment.

## Suggested Implementation Sequence (2-Day Sprint)

Day 1
1. Priority 0 items (narrative spine, before/after table, CTA path).
2. Priority 1 item 4 and 5 (kept/replaced + risk-to-proof mapping).

Day 2
1. Priority 1 item 6 and Priority 2 item 7 (decision logs + field-ready sales narrative).
2. Priority 2 item 8/9 and Priority 3 hygiene pass.
3. Final QA using checklist and link audit.

## Definition of Done
- A first-time buyer can understand the Node.js-to-Java decision in under 2 minutes.
- Every major claim has a nearby evidence path.
- Technical and executive pages use the same dates, metrics, and terms.
- Persona hubs route users through a clear transformation narrative journey.
