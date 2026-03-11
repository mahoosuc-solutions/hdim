# Fallon Health

## Score: 21/25
| VBC Exposure | Quality Pain | Tech Readiness | Decision Velocity | Trigger Recency |
|---|---|---|---|---|
| 5 | 5 | 3 | 3 | 5 |

## Organization Profile
- **Type:** Community health plan, not-for-profit (being acquired by Mass General Brigham Health Plan)
- **Size:** ~148K total members (Fallon); combined entity ~553K members (3rd largest insurer in Massachusetts)
- **Geography:** Massachusetts — headquartered in Worcester; statewide coverage
- **EHR:** Plan-side systems (claims/quality); provider network uses mixed EHRs; MGB Health Plan operates on Epic (MGB system-wide)
- **Payer Mix:** Medicare Advantage ~30% / Medicaid ~35% / Commercial ~35%
- **Revenue:** Not publicly disclosed (not-for-profit); net losses of $15.25M (2023) and $13.4M (2024) prompted the acquisition

## Decision Makers
| Name | Title | LinkedIn | Recent Activity |
|------|-------|----------|-----------------|
| Manny Lopes | President & CEO, Fallon Health (since July 2024) | [LinkedIn](https://www.linkedin.com/in/manny-lopes/) | Leading MGB acquisition integration; former BCBS MA exec, Fenway Health interim CEO |
| (CMO — not publicly named) | Chief Medical Officer, Fallon Health | — | Quality measure oversight during M&A transition |
| (MGB Health Plan leadership TBD) | Combined entity leadership | — | Post-acquisition organizational structure not yet announced |

## VBC Contracts
- Medicare Advantage plans: 4.0 CMS Stars (2026 ratings, down from 4.5 historically)
- NCQA top-rated Medicaid plan in Massachusetts (4.5/5 NCQA 2025 Medicaid rating)
- ACHP (Alliance of Community Health Plans) member — community health plan model
- Medicaid managed care contracts with MassHealth
- Commercial value-based arrangements with Massachusetts provider networks
- Post-acquisition: combined MA + Medicaid + commercial portfolio with MGB Health Plan (~405K MGB + ~148K Fallon)

## Quality Performance
- Medicare Advantage: 4.0 CMS Stars (2026) — declined from historical 4.5 Stars
- Medicaid: 4.5/5 NCQA rating (2025) — top-rated Medicaid plan in Massachusetts, no other MA Medicaid plan rated higher
- ACHP member — among 16 ACHP Medicaid contracts earning 4+ Stars in NCQA 2025 ratings
- Founded 1977 — 49 years of continuous operation in Massachusetts market
- Financial deterioration: $22.5M net margin (2022) to -$15.25M (2023) to -$13.4M (2024)
- Star rating decline from 4.5 to 4.0 coincides with financial stress — quality investment likely deferred

## Recent Intelligence
| Date | Source | Summary | URL |
|------|--------|---------|-----|
| 2026-01-08 | Boston Globe | Mass General Brigham to acquire Fallon Health — combined 553K members, 3rd largest insurer in MA | https://www.bostonglobe.com/2026/01/08/business/mass-general-brigham-acquire-insurer-fallon-health/ |
| 2026-01 | Fallon Health | Joint statement: no immediate changes for members; regulatory review process underway | https://fallonhealth.org/en/About/newsroom/2026/FHMBGHP |
| 2026-02 | WBJ | Fallon CEO Manny Lopes: company not looking to downsize after merger | https://wbjournal.com/article/fallon-ceo-company-not-looking-to-downsize-after-mass-general-brigham-merger/ |
| 2025-09 | NCQA | Fallon earns top national recognition: 4.5/5 NCQA Medicaid rating — highest in Massachusetts | https://fallonhealth.org/en/About/newsroom/2025/ncqa |
| 2024-07 | WBJ | Manny Lopes named permanent CEO, replacing retired Richard Burke | https://www.wbjournal.com/article/fallon-health-names-permanent-ceo |

## Pain Hypothesis
Fallon Health faces a dual quality crisis driven by the MGB acquisition: (1) their Medicare Advantage Stars rating has declined from 4.5 to 4.0 — a half-star drop that costs millions in quality bonus revenue and signals quality infrastructure underinvestment during the financial losses of 2023-2024, and (2) the acquisition by Mass General Brigham Health Plan creates a massive integration challenge — merging two health plan populations (~553K combined members) with different quality measurement systems, provider networks, and clinical data sources while maintaining quality ratings for both populations during the transition. The regulatory review process (Division of Insurance, Health Policy Commission, AG) adds compliance pressure. HDIM solves this by providing: (1) CQL-native HEDIS measure evaluation that can run consistently across both Fallon and MGB Health Plan populations from day one of integration, (2) FHIR R4 data normalization that bridges the different provider network data sources (MGB Epic systems + Fallon's community provider network), (3) real-time care gap identification that helps recover the lost half-star in MA while maintaining the 4.5 Medicaid rating, and (4) a unified quality dashboard that gives the combined entity a single view of quality performance across all lines of business. The financial case is compelling: recovering from 4.0 to 4.5 Stars across the combined MA population would drive significant quality bonus revenue that directly addresses the financial losses that prompted the acquisition.

## Trigger Events
| Date | Trigger Type | Description | Urgency |
|------|-------------|-------------|---------|
| 2026-01 | M&A | MGB acquisition announced — regulatory review underway, expected to close 2026 | Critical |
| 2026-Q2 | Regulatory | Division of Insurance, Health Policy Commission, AG review — quality maintenance under scrutiny | High |
| 2025-Q3 | Quality | MA Stars decline from 4.5 to 4.0 — quality bonus revenue loss, competitive positioning weakened | Critical |
| 2025-Q2 | Regulatory | HEDIS MY 2026: 7 new measures + 4 ECDS transitions — reporting complexity during M&A integration | High |
| 2024-Q3 | Leadership | New CEO Manny Lopes (July 2024) — fresh leadership, new strategic priorities, open to technology investments | Medium |

## Archetype Match
- **Primary:** 02-MA Plan
- **Overlay Notes:** The M&A trigger makes this also fit 04-Health System archetype, since MGB is a major academic health system. The acquisition creates a unique buying window: the combined entity will need to rationalize quality measurement infrastructure, and HDIM could be positioned as the platform of choice for the merged organization. However, the acquisition also creates procurement uncertainty — who is the buyer (Fallon leadership vs. MGB Health Plan leadership)? Decision velocity is constrained (score: 3) by both the regulatory review timeline and the organizational ambiguity of the transition. The not-for-profit status and financial losses mean budget is tight, but MGB's resources change the equation post-close. Tech readiness is moderate (score: 3) because Fallon's current systems are aging, but MGB's Epic infrastructure provides a strong FHIR R4 foundation post-integration.

## Outreach Status
| Touch | Date | Channel | Content | Response |
|-------|------|---------|---------|----------|
| T1 | — | — | — | — |
| T2 | — | — | — | — |
| T3 | — | — | — | — |

## Next Action
Monitor the regulatory review timeline — the optimal engagement window is after regulatory approval but before systems integration begins. Identify the quality leadership at both Fallon and MGB Health Plan who will lead the integration. Position HDIM as the unified quality measurement platform for the combined entity. Lead with the Star rating recovery story: the decline from 4.5 to 4.0 on the MA side is a quantifiable pain point that HDIM directly addresses. Reference the NCQA 4.5 Medicaid rating as proof that Fallon's quality capability exists — the challenge is extending it consistently across the combined population with new infrastructure. Timeline: initiate T1 outreach by May 2026 (post-regulatory clarity). The MA geography (Worcester/Boston) aligns well with in-person engagement.
