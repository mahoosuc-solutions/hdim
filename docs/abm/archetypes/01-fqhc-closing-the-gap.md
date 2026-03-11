# Archetype 1: FQHC — Closing the Gap

## Target Persona
- **Title:** Quality Director / Quality & Compliance Manager
- **Org Type:** Federally Qualified Health Center or Primary Care Association, 4-12 sites
- **Reports To:** CEO, Board of Directors, HRSA
- **Measured On:** UDS clinical quality measures, HEDIS subset rates, health equity outcomes, HRSA site visit readiness
- **What Keeps Them Up at Night:** "I have 28,000 patients, two-thirds on Medicaid, and my diabetes control rate is 52%. HRSA wants UDS+ electronic reporting next year and I'm still pulling data from three different systems into spreadsheets. If our quality numbers drop, we lose our FQHC look-alike funding — and 6,000 patients lose their only source of care."

## Population Profile
- **Size Range:** 15,000-40,000 patients across 4-12 sites
- **Payer Mix:** Medicaid 65%, Uninsured/Sliding Scale 18%, Medicare 10%, Commercial 7%
- **Chronic Condition Prevalence:** Diabetes 19%, Hypertension 32%, Depression/Behavioral Health 24%, Obesity 38%, Substance Use Disorder 11%
- **Key Measures:** HbA1c Poor Control (<8%, NQF 0059), Controlling High Blood Pressure (NQF 0018), Cervical Cancer Screening (NQF 0032), Depression Screening and Follow-Up (NQF 0418), Colorectal Cancer Screening (NQF 0034), BMI Screening and Follow-Up

## Demo Seed Parameters
- **Patient Count:** 1,200 (representative sample of 28,000 population)
- **Payer Distribution:** Medicaid 65%, Uninsured 18%, Medicare 10%, Commercial 7%
- **Measures with Gap Rates:**
  | Measure | Current Rate | Target Rate | Open Gaps | Recent Closures |
  |---------|-------------|-------------|-----------|-----------------|
  | HbA1c Poor Control (<8%) | 52% | 65% | 847 | 23 (weekend) |
  | Controlling High Blood Pressure | 58% | 68% | 614 | 11 (weekend) |
  | Cervical Cancer Screening | 44% | 58% | 1,102 | 8 (weekend) |
  | Depression Screening & Follow-Up | 39% | 55% | 1,340 | 14 (weekend) |
  | Colorectal Cancer Screening | 31% | 45% | 982 | 0 |
  | BMI Screening & Follow-Up | 61% | 72% | 497 | 12 (weekend) |
- **Provider Roster:** 14 PCPs, 3 behavioral health providers, 4 care coordinators, 2 community health workers, 1 quality analyst
- **Pre-Seeded "Wins":** 3 HbA1c gaps closed via weekend urgent care labs (patients came in for other reasons, labs captured opportunistically). 14 depression screenings completed at Saturday walk-in clinic. 11 blood pressure readings documented through community health worker home visits on Friday afternoon.

## Monday Morning Scenario Script

### Opening (What They See)
Maria logs in at 7:45 AM. The dashboard shows a population health summary for all 28,000 patients across 8 sites. A green banner reads: "68 care gaps closed since Friday 5 PM." The quality trend chart shows diabetes control climbing from 48% in January to 52% today — slow but steady. A red badge on the left nav shows 2 measures flagged "At Risk" for the upcoming UDS report.

### The Good News
"Look at this — 68 gaps closed over the weekend without a single planned outreach. Twenty-three of those are HbA1c results. Your Saturday walk-in clinic is a gap-closing machine. Three patients came in for acute visits, got labs drawn, and their diabetes control results came back under 8%. That's the kind of opportunistic capture that moves your numbers. Your diabetes rate just ticked from 51.6% to 52.1% — small, but that's real patients getting better care."

### The Opportunity
"Now here's where it gets interesting. You have 847 patients overdue for HbA1c. But not all 847 are equal." Click into the prioritized outreach list. "HDIM has ranked them by closability. The top 147 patients have an appointment already scheduled in the next 30 days — they just need a lab order added. The next 312 had a visit in the last 90 days but no lab was drawn. And 203 have a transportation barrier flagged via SDOH Z-codes — calling them won't work, but your community health workers can reach them."

### The "Aha Moment"
Click into the health equity stratification view. The screen splits diabetes control rates by race, ethnicity, preferred language, and insurance status. "Your overall HbA1c rate is 52%. But look — for Spanish-speaking patients, it's 41%. For patients with no reliable phone number, it's 38%. This isn't just a quality gap. It's an equity gap. And HDIM shows you exactly where to invest your community health worker hours to close it."

### Talking Points at Each Screen
1. **[Dashboard]** — "This is your UDS report writing itself in real-time. No more 3-month year-end scramble. You know exactly where you stand today."
2. **[Care Gap List]** — "Your care coordinators aren't calling 847 patients blindly. They're calling the 147 who already have appointments. That's a 30-minute workflow, not a 3-week campaign."
3. **[Provider Breakdown]** — "Dr. Hernandez at Site 3 has a 64% diabetes control rate. Dr. Patel at Site 7 has 43%. Same patient population, same EHR. What's Dr. Hernandez doing differently? Now you can see it and spread it."
4. **[Health Equity View]** — "HRSA is asking every FQHC to report on equity stratification. This view is your answer — and your roadmap for where to deploy resources."

### Objection Pre-emption
The demo naturally answers three objections before they're raised: (1) "We can't afford another system" — the UDS reporting automation alone saves 2 FTE months, paying for itself. (2) "Our patients are too hard to reach" — the SDOH-aware prioritization shows HDIM understands FQHC populations, not just commercial ones. (3) "We already have Azara" — the real-time gap closure and equity stratification are capabilities Azara DRVS does not provide.

## Pain Hooks (By Trigger Type)

### Star Rating Drop
Not directly applicable to FQHCs, but reframe: "Your Medicaid managed care partners are tracking HEDIS rates on your attributed patients. When their rates drop, they start asking questions about your network. HDIM gives you the data to show you're closing gaps — before they start looking for other providers."

### Quality Report Published
"Your latest UDS report showed diabetes control at 48% — below the national FQHC average of 55%. That's now public on the HRSA data portal. Every funder, every board member, every competing FQHC can see it. HDIM gives you real-time visibility so next year's report tells a different story."

### Leadership Change
"New CEO wants to understand quality performance across all sites in their first 90 days. Instead of a 6-week data pull, you hand them a live dashboard. That's the difference between looking like you're struggling and looking like you have a plan."

### VBC Contract Announcement
"Your largest Medicaid MCO just announced a value-based incentive program tied to HEDIS rates. They're offering a 2% bonus on per-member-per-month for hitting diabetes and hypertension targets. That's $840K annually for your population — but only if you can measure and close gaps in real-time."

### M&A / Expansion
"You're absorbing two new sites from a closing practice. Those 6,000 patients have no quality history in your system. HDIM ingests their clinical data via FHIR, calculates gap status on day one, and your care coordinators know who needs what before the first appointment."

### Conference Presentation
"You're presenting at the NACHC Community Health Institute on health equity outcomes. Instead of showing last year's UDS data, you show a live equity stratification dashboard. Your audience is other Quality Directors — they'll ask what tool you're using."

## Overlay Fields
| Field | What Changes Per Target |
|-------|------------------------|
| Organization Name | Dashboard header, report titles, login screen welcome |
| Patient Volume | Seed data scales proportionally (1,200 seed = 28K base; adjust for 15K-40K) |
| Priority Measures | Lead with their weakest UDS measure (diabetes vs depression vs cervical screening) |
| Geography | State-specific Medicaid MCO names, state FQHC association references, regional health equity data |
| Opening Narrative | Tailored from dossier: "You told [Board/HRSA/MCO partner] you'd hit 58% diabetes control by September. Here's your path to get there." |

## Competitive Positioning
- **What They're Likely Using Today:** Azara DRVS (most common FQHC quality tool), manual EHR reports from eClinicalWorks/NextGen/athenahealth, spreadsheets for UDS compilation
- **Why HDIM Is Different:**
  - Real-time gap closure vs Azara's batch claims refresh (weekly/monthly lag)
  - SDOH Z-code integration for equity-aware outreach prioritization — Azara shows gaps but not barriers
  - UDS+ electronic reporting readiness (FHIR R4 native) — critical as HRSA transitions from manual UDS to UDS+ eCQM submission
  - Multi-site provider benchmarking with same-population normalization — see variation that isn't just case mix
- **Displacement Messaging:** "Azara tells you how many gaps you have. HDIM tells you which ones to close first, who can close them, and what barriers are in the way. When HRSA moves to UDS+ electronic quality reporting, you'll need FHIR-native infrastructure — that's not a retrofit onto Azara, that's a new foundation."
