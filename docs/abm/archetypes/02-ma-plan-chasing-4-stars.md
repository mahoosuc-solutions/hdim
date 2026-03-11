# Archetype 2: MA Plan — Chasing 4 Stars

## Target Persona
- **Title:** VP of Quality / Senior Director of Stars Performance
- **Org Type:** Regional Medicare Advantage plan, 50K-200K members
- **Reports To:** CMO and CFO (dual reporting — quality is both clinical and financial)
- **Measured On:** Star Rating (current + projected), quality bonus capture, gap closure rate, member retention, HEDIS measure performance
- **What Keeps Them Up at Night:** "We're at 3.5 Stars. The 4.0 threshold is worth $12 million in quality bonus. Six of our measures are within 2 percentage points of the next cut point. I'm making decisions with data that's 60-90 days old. By the time I know a gap closed, measurement year is almost over. And CMS just changed 3 HEDIS measures to ECDS — my team doesn't even know how to calculate those yet."

## Population Profile
- **Size Range:** 50,000-200,000 attributed Medicare Advantage lives
- **Payer Mix:** 100% Medicare Advantage (but dual-eligible subset 18-25%, driving SDOH complexity)
- **Chronic Condition Prevalence:** Diabetes 28%, Hypertension 54%, CHF 12%, COPD 14%, Depression 19%, CKD 16%
- **Key Measures:** Medication Adherence — Diabetes (PDC-DR), Medication Adherence — Hypertension (PDC-RASA), Medication Adherence — Cholesterol (PDC-STA), Breast Cancer Screening (BCS), Colorectal Cancer Screening (COL), Diabetes Eye Exam (EED), HbA1c Control (<8%), Controlling High Blood Pressure (CBP), Plan All-Cause Readmissions (PCR), Statin Therapy for CVD (SPC)

## Demo Seed Parameters
- **Patient Count:** 2,400 (representative sample of 120,000 member plan)
- **Payer Distribution:** MA-PD 72%, MA-only 10%, D-SNP (dual eligible) 18%
- **Measures with Gap Rates:**
  | Measure | Current Rate | Target Rate | Open Gaps | Recent Closures |
  |---------|-------------|-------------|-----------|-----------------|
  | Medication Adherence — Diabetes (PDC-DR) | 78% | 80% | 3,200 | 89 (weekend refills) |
  | Medication Adherence — RAS Antagonists | 79% | 81% | 2,870 | 74 (weekend refills) |
  | Medication Adherence — Statins (PDC-STA) | 77% | 80% | 3,640 | 82 (weekend refills) |
  | Breast Cancer Screening (BCS) | 71% | 74% | 4,120 | 0 |
  | Colorectal Cancer Screening (COL) | 68% | 72% | 5,340 | 0 |
  | HbA1c Control (<8%) | 62% | 67% | 6,080 | 31 (weekend labs) |
  | Controlling High Blood Pressure (CBP) | 66% | 70% | 5,100 | 18 (weekend clinic) |
  | Diabetic Eye Exam (EED) | 59% | 65% | 7,200 | 0 |
  | Statin Therapy for CVD (SPC) | 74% | 78% | 2,950 | 45 (weekend refills) |
  | Plan All-Cause Readmissions (PCR) | 14.8% | <13.5% | 412 (at-risk) | 3 (avoided) |
- **Provider Roster:** 480 contracted PCPs, 1,200 specialists, 28 care managers, 6 pharmacy outreach specialists, 4 quality analysts
- **Pre-Seeded "Wins":** 245 medication adherence gaps closed via weekend pharmacy refills (auto-captured from PBM feed). 31 HbA1c results returned from Friday lab draws. 18 blood pressure readings from Saturday urgent care visits. 3 readmissions avoided — patients flagged as high-risk on Friday were contacted by care managers before discharge.

## Monday Morning Scenario Script

### Opening (What They See)
Rachel logs in at 8:00 AM. The Stars Projection Dashboard dominates the screen: a large "3.5" with an arrow trending toward "3.72" based on current trajectory. A waterfall chart shows each measure's contribution to the overall rating. Six measures are highlighted in amber — within 2 percentage points of the next cut point. A ticker at the top reads: "297 gaps closed since Friday 5 PM. 142 days remaining in measurement year."

### The Good News
"Good news first — your pharmacy team's weekend refill capture just moved the needle. 245 medication adherence gaps closed automatically from PBM data flowing in over the weekend. Your PDC-DR just went from 77.8% to 78.1%. That doesn't sound like much, but you're 1.9 points from the 4-star cut point on that measure. Every tenth of a point matters. And look — 3 readmissions avoided. Your Friday discharge follow-up calls caught Mrs. Chen, Mr. Ramirez, and Mr. Thompson before they bounced back. That's $45,000 in avoided costs and 3 members who didn't have a terrible weekend."

### The Opportunity
"Now here's the play. You have 6 measures within 2 percentage points of the next cut point. Let me rank them by closability." Click into the Stars Impact Simulator. "Medication adherence is your highest-leverage move. You need 2 points on PDC-DR, 2 points on PDC-RASA, and 3 points on PDC-STA. That's 3,200 + 2,870 + 3,640 = 9,710 total open gaps. But 3,200 of those members have 90-day refills due in the next 45 days. If your pharmacy outreach team contacts even half of them and converts 60%, that's 960 gap closures — enough to push all three measures past the cut point."

Click into the member prioritization list. "HDIM has ranked all 3,200 PDC-DR gaps by closability. The top tier: members who filled 2 of 3 required fills and have a refill due within 30 days. One pharmacy call closes the gap. The second tier: members who missed their last fill but have an active prescription. The third tier: members who need a provider visit to renew. Your care managers work the list top-down, highest impact first."

### The "Aha Moment"
Click into the Stars Impact Simulator. Drag the PDC-DR rate from 78% to 80%. The projected Star Rating updates in real-time: 3.5 becomes 3.62. Drag PDC-RASA to 81%. Now it's 3.71. Add CBP to 70%. It hits 3.84. "Three measures. That's the difference between 3.5 and 3.84 Stars. At your membership size, that's the difference between zero quality bonus and $12 million. And I just showed you which 3,200 members to call first."

### Talking Points at Each Screen
1. **[Stars Dashboard]** — "This updates daily. Not quarterly. You know exactly where you stand every morning, with 142 days left to act."
2. **[Care Gap List]** — "Your care managers aren't working a list of 40,000 open gaps. They're working the 3,200 that move your Star Rating. Prioritized. Ranked. With the member's pharmacy, last fill date, and preferred contact method."
3. **[Provider Breakdown]** — "Dr. Singh's panel has a 72% PDC-DR rate. Dr. Okafor's has 84%. Same formulary, same population mix. HDIM shows you the provider-level variation so you can target your academic detailing where it matters."
4. **[Stars Impact Simulator]** — "This is the screen your CFO wants to see. Drag the sliders, see the bonus move. It turns quality strategy into a financial planning exercise."

### Objection Pre-emption
The demo naturally answers: (1) "We already have Cotiviti/Inovalon" — the real-time pharmacy refill capture and daily Stars projection are capabilities batch-based platforms cannot replicate. (2) "Our providers won't engage" — HDIM's provider-level scorecards show them their own performance vs peers, which drives behavior change without mandates. (3) "ECDS transition is overwhelming" — show the ECDS-ready measures calculating from clinical data, not just claims. (4) "The ROI timeline is too long" — the Stars Impact Simulator quantifies the bonus in the current measurement year.

## Pain Hooks (By Trigger Type)

### Star Rating Drop
"You dropped from 4.0 to 3.5 Stars. That's $12M in quality bonus — gone. Your board is asking what happened and what's the recovery plan. HDIM gives you the recovery plan: exactly which measures are closest to the cut point, which members to prioritize, and a daily projection so the board can track progress in real-time. The 2025 Stars nadir hit plans across the industry — the ones that recover fastest will be the ones with real-time visibility."

### Quality Report Published
"CMS just released the Star Ratings preview. You're at 3.5 on six measures that were 3.0 or below. Your competitors in the same county are at 4.0. Members have a choice during AEP. HDIM shows you the gap between your rates and the 4-star cut points — and exactly how many member interventions close that gap before the next measurement year ends."

### Leadership Change
"New CMO wants a quality strategy briefing in their first 30 days. Instead of a 60-page PowerPoint with stale data, you pull up the Stars Dashboard. Live numbers. Projected trajectory. Financial impact. They see the plan, not the problem. That's how you build trust with new leadership."

### VBC Contract Announcement
"Your parent company just signed a new employer group that adds 30,000 MA members. Your quality infrastructure was built for 90,000. Can it handle 120,000 with the same care management team? HDIM's prioritization means your 28 care managers work smarter, not harder — the ranked list adjusts automatically as the population grows."

### M&A / Expansion
"You're acquiring a smaller MA plan with 25,000 members and a 3.0 Star Rating. Their quality data is in a different format, different vendor, different measurement logic. HDIM ingests their claims and clinical data via FHIR, calculates gap status on day one, and gives you a combined Stars projection before the ink is dry on the acquisition."

### Conference Presentation
"You're presenting at AHIP on Stars recovery strategy. Your slide deck shows a live Stars Impact Simulator with real (de-identified) data. The audience sees measures moving in real-time as you drag sliders. Every VP of Quality in the room asks what platform you're using."

## Overlay Fields
| Field | What Changes Per Target |
|-------|------------------------|
| Organization Name | Stars Dashboard header, member communications, provider scorecards |
| Patient Volume | Seed data scales (2,400 seed = 120K base; adjust for 50K-200K). Gap counts and bonus dollars scale proportionally |
| Priority Measures | Lead with their weakest Stars-weighted measures. If medication adherence is strong, pivot to screening measures |
| Geography | County-level Stars comparison data, state-specific pharmacy regulations, regional provider network density |
| Opening Narrative | Tailored from dossier: "Your 2025 Star Rating dropped to 3.5. You told the board you'd recover to 4.0 by 2027. Here's the 142-day plan to get there." |

## Competitive Positioning
- **What They're Likely Using Today:** Cotiviti (quality analytics + HEDIS abstraction), Inovalon (claims-based quality), Optum (if UHG-affiliated), internal actuarial team with SAS/SQL, legacy HEDIS vendor for annual audit
- **Why HDIM Is Different:**
  - Real-time gap closure from pharmacy, lab, and clinical data vs Cotiviti/Inovalon's 30-60 day claims lag
  - Daily Stars Rating projection vs quarterly estimates — decisions made with today's data, not last quarter's
  - ECDS-ready: 3 measures transitioning + 6 new electronic measures in MY 2026 — HDIM calculates from clinical data natively, no claims-to-clinical translation required
  - Stars Impact Simulator quantifies the financial impact of closing specific gaps — turns quality meetings into financial planning sessions
- **Displacement Messaging:** "Cotiviti tells you where you were 60 days ago. HDIM tells you where you are today and where you'll be in December. HEDIS measures represent roughly 40% of your Part C Star Rating — and with ECDS expanding from 3 to 9 measures in MY 2026, you need a platform that calculates from clinical data, not one that retrofits claims. The plans that hit 4 Stars next year will be the ones that switched from rearview mirror analytics to real-time intelligence this year."
