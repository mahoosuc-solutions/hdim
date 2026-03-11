# Archetype 5: Specialty Network — Proving Value

## Target Persona
- **Title:** Network Director / VP of Quality and Network Performance
- **Org Type:** Specialty care network (cardiology, oncology, behavioral health, nephrology, or orthopedics), 15-80 providers across 5-25 locations, often an IPA or clinically integrated network structure
- **Reports To:** Network Board, Managing Partners, participating practice leadership
- **Measured On:** Payer contract retention, quality-based incentive capture, referral volume, referral loop closure rate, network-level outcome metrics, cost-per-episode benchmarks
- **What Keeps Them Up at Night:** "We have 40 payer contracts with quality language buried in each one. Every payer defines 'quality' differently. My cardiologists are measured on statin therapy by one payer, blood pressure by another, and readmissions by a third. I can't even produce a unified report card for my own network. And our referring PCPs are starting to send patients to the hospital-owned group because they publish outcomes data and we can't."

## Population Profile
- **Size Range:** 5,000-30,000 patients in a focused clinical domain (e.g., 18,000 cardiology patients across 12 practices)
- **Payer Mix:** Commercial 38%, Medicare (FFS + MA) 35%, Medicaid 20%, Self-Pay/Other 7% (varies significantly by specialty — cardiology skews Medicare, behavioral health skews Medicaid)
- **Chronic Condition Prevalence (Cardiology example):** Hypertension 72%, Hyperlipidemia 64%, CAD 38%, CHF 22%, Atrial Fibrillation 18%, Diabetes (co-morbid) 34%, CKD (co-morbid) 19%
- **Key Measures:** Statin Therapy for CVD (SPC), Controlling High Blood Pressure (CBP), Cardiac Rehab Referral, Beta-Blocker Persistence Post-MI, ACE/ARB for LVSD, 30-Day Post-Discharge Follow-Up, Referral Loop Closure Rate, Cost per Episode (PCI, CABG, CHF admission), specialty-specific MIPS measures

## Demo Seed Parameters
- **Patient Count:** 900 (representative sample of 18,000 cardiology patients)
- **Payer Distribution:** Commercial 38%, Medicare 35%, Medicaid 20%, Self-Pay 7%
- **Measures with Gap Rates:**
  | Measure | Current Rate | Target Rate | Open Gaps | Recent Closures |
  |---------|-------------|-------------|-----------|-----------------|
  | Statin Therapy for CVD (SPC) | 71% | 82% | 1,620 | 28 (weekend refills) |
  | Controlling High Blood Pressure | 64% | 75% | 2,340 | 19 (Friday clinic) |
  | Beta-Blocker Post-MI (12 months) | 76% | 85% | 410 | 7 (weekend refills) |
  | ACE/ARB for LVSD | 79% | 88% | 380 | 5 (weekend refills) |
  | Cardiac Rehab Referral Post-Event | 34% | 60% | 890 | 0 |
  | 30-Day Post-Discharge Follow-Up | 62% | 80% | 520 | 12 (weekend calls) |
  | Referral Loop Closure | 58% | 80% | 312 open loops | 8 (reports received) |
  | Cost per CHF Admission | $18,400 | $15,500 | N/A | N/A |
- **Provider Roster:** 24 cardiologists across 8 practices, 6 NPs/PAs, 3 care coordinators, 1 quality manager, 180 referring PCPs in the network
- **Pre-Seeded "Wins":** 28 statin therapy gaps closed via weekend pharmacy refill data. 12 post-discharge follow-up calls completed Saturday (care coordinator weekend coverage). 8 referral loops closed — reports from outside labs and imaging centers received and matched to original referral orders. Payer contract dashboard loaded with all 40 contracts normalized to common measure definitions.

## Monday Morning Scenario Script

### Opening (What They See)
Karen logs in at 8:30 AM. The Network Performance Dashboard shows a summary view: "18,000 patients | 24 cardiologists | 40 payer contracts | 5 common quality measures." Below, a contract alignment matrix shows each payer contract as a row, each quality measure as a column, with color-coded performance indicators. A banner reads: "5 measures appear in 80% of your contracts. Master these 5, satisfy 32 payers at once."

### The Good News
"This is the view that didn't exist before today. You've been managing 40 payer contracts as 40 separate quality programs. HDIM just normalized them. Look — Anthem, Aetna, UnitedHealthcare, Humana, and your largest Medicaid MCO all measure statin therapy for cardiovascular disease. Different contract language, same underlying HEDIS measure. Your network-wide rate is 71%. That's one number, calculated once, applicable to 32 of your 40 contracts."

"And the weekend brought good news: 28 statin refill gaps closed automatically from PBM data. Your 30-day post-discharge follow-up rate ticked up 0.4% from Saturday care coordinator calls — 12 patients reached, 12 follow-up appointments confirmed. And 8 referral loops that were open for 20+ days just closed because lab and imaging reports finally came in and were auto-matched."

### The Opportunity
"Now here's where it gets strategic. Your cardiac rehab referral rate is 34%. The target across your payer contracts is 60%. That's the worst-performing measure in your network, and it's a measure that 28 of your 40 payers include in their quality incentive calculations." Click into the cardiac rehab drill-down. "HDIM identified 890 patients who had a qualifying cardiac event (MI, PCI, CABG, or CHF admission) in the last 12 months and have no cardiac rehab referral on file. 340 of those patients have a follow-up appointment scheduled in the next 30 days. If your cardiologists add a rehab referral at those visits, your rate jumps from 34% to 53% — with zero additional patient outreach."

Click into the referral loop tracker. "This one is urgent. You have 312 open referral loops — patients referred to your cardiologists by PCPs where no report has been sent back. Average loop age: 34 days. Dr. Patel's practice has 89 open loops; Dr. Chen's has 12. Same patient volume. The difference is workflow — Dr. Chen's office auto-faxes visit summaries; Dr. Patel's relies on manual follow-up."

### The "Aha Moment"
Click into the Network Value Dashboard. The screen shows a side-by-side comparison: "Your Network vs Hospital-Owned Cardiology Group." Quality measures, referral response times, patient satisfaction, and cost-per-episode are lined up. "Your referring PCPs are choosing between you and the hospital group. Right now, the hospital group publishes outcomes data on their website. You don't. But look at this — your outcomes are actually better on 3 of 5 measures. Your statin therapy rate is 71% vs their 68%. Your cost per CHF admission is $18,400 vs their $21,200. You're winning on quality and cost. You just can't prove it. Until now."

"This is the PDF you send to every referring PCP in your network. 'Our cardiology network outperforms the hospital group on statin adherence, blood pressure control, and cost per episode. Here's the data.' That's how you protect referral volume."

### Talking Points at Each Screen
1. **[Contract Alignment Matrix]** — "40 contracts, 5 common measures. Instead of 40 quality improvement projects, you have 5. That's manageable for a quality manager and 3 care coordinators."
2. **[Care Gap List]** — "890 patients need cardiac rehab referrals. 340 have appointments in the next 30 days. Print the list. Hand it to the MAs. Referral happens at check-in. No extra work for the cardiologist."
3. **[Referral Loop Tracker]** — "312 open loops is 312 PCPs who are wondering if their patient ever got seen. Close the loop and you keep the referral. Leave it open and they send the next patient somewhere else."
4. **[Network Value Dashboard]** — "This is your sales deck to referring physicians. Not marketing claims — actual outcomes data. 'Send your patients to us because we're better and we can prove it.'"

### Objection Pre-emption
The demo naturally answers: (1) "We're too small for an enterprise platform" — HDIM is priced for networks, not health systems; the 900-patient seed shows it works at specialty scale. (2) "Our payer contracts are all different" — the contract alignment matrix normalizes them, reducing 40 programs to 5. (3) "Cardiologists won't change their workflow" — the referral loop tracker and cardiac rehab list are operational tools that save their staff time, not add work. (4) "We can't afford to lose referrals" — the Network Value Dashboard is specifically designed to protect and grow referral volume by proving your network's value with data.

## Pain Hooks (By Trigger Type)

### Star Rating Drop
Reframe for specialty networks: "Your MA plan partners' Star Ratings dropped, and they're scrutinizing specialty network performance. HEDIS measures in your domain — statin therapy, blood pressure control, readmissions — directly affect their Stars. HDIM gives you a proactive quality report to send to every MA plan you contract with: 'Here's what we're doing, here are our rates, here's our improvement trajectory.' That turns a defensive conversation into a strategic one."

### Quality Report Published
"Your largest payer just published their specialty network quality report card. Your cardiology network scored Tier 2 out of 3. The hospital-owned group scored Tier 1. Tier 1 gets a 4% reimbursement bonus. Tier 2 gets standard rates. HDIM shows you exactly which measures put you in Tier 2 and what it takes to move to Tier 1 before the next measurement period."

### Leadership Change
"New managing partner wants to understand network performance across all 8 practices before their first board meeting. Instead of asking each practice to self-report, hand them a unified dashboard. They see quality rates, referral patterns, cost per episode, and payer contract performance across the entire network. That's leadership intelligence, delivered on day one."

### VBC Contract Announcement
"Your largest payer just shifted from fee-for-service to episode-based bundled payment for PCI and CHF. Your cost per CHF admission is $18,400 — the bundle target is $16,000. HDIM shows you where the cost variation lives: which practices have higher readmission rates, which patients are missing follow-up, where post-acute coordination is breaking down. You need to cut $2,400 per episode, and the first step is knowing where the waste is."

### M&A / Expansion
"A 4-practice nephrology group wants to join your clinically integrated network. They bring 3,200 patients and 3 payer contracts with quality incentives you don't currently track. HDIM ingests their clinical data, maps their quality measures to your existing contract alignment matrix, and shows the combined network performance before the affiliation agreement is signed."

### Conference Presentation
"You're presenting at ACC (American College of Cardiology) on specialty network quality measurement. Your poster shows how a 24-cardiologist network unified quality tracking across 40 payer contracts using FHIR-based CQL execution. The audience is cardiology practice administrators who are drowning in the same contract-by-contract quality reporting. You'll have a line at your poster."

## Overlay Fields
| Field | What Changes Per Target |
|-------|------------------------|
| Organization Name | Network dashboard header, value comparison reports, referring physician communications |
| Patient Volume | Seed data scales (900 seed = 18K base; adjust for 5K-30K). Gap counts scale proportionally |
| Priority Measures | Lead with the measures common across their largest payer contracts. If cardiac rehab is not relevant (behavioral health network), substitute with follow-up visit measures |
| Geography | Regional payer names, competing specialty groups (hospital-owned vs independent), state-specific referral patterns |
| Opening Narrative | Tailored from dossier: "You have 40 payer contracts and no unified quality report. Your largest referrer just asked for outcomes data you can't produce. Here's how to change that in 30 days." |

## Competitive Positioning
- **What They're Likely Using Today:** Specialty-specific registries (ACC NCDR for cardiology, ASCO CancerLinQ for oncology), individual practice EHR reports, manual payer contract tracking in spreadsheets, no unified cross-practice quality view
- **Why HDIM Is Different:**
  - Payer contract normalization: maps 40 contracts to common measure definitions — registries track clinical measures but don't connect to payer contract quality incentives
  - Cross-practice quality benchmarking: same CQL logic across all practices regardless of EHR — registries require manual data submission, HDIM pulls directly from clinical systems
  - Referral loop tracking: monitors referral-to-report cycle time and closes loops automatically — no registry or EHR does this across a multi-practice network
  - Network value documentation: generates outcomes comparisons vs competing groups for referring physician retention — this capability simply doesn't exist in specialty registries
- **Displacement Messaging:** "Your ACC NCDR registry tells you how your outcomes compare to national benchmarks. It doesn't tell you how you compare to the hospital-owned group that's competing for your referrals. It doesn't normalize your 40 payer contracts into 5 actionable measures. And it doesn't track whether Dr. Patel's office sent the visit summary back to the referring PCP. HDIM isn't a registry replacement — it's the operational layer that turns registry-quality data into payer contract performance, referral retention, and network growth. The specialty networks that survive the shift to value-based care won't be the ones with the best clinical data — they'll be the ones that can prove their value to every payer and every referring physician, in real-time."
