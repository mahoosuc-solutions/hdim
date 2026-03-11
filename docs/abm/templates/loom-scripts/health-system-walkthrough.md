# Loom Walkthrough Script: Health System — Integrating Post-M&A

**Duration:** 60-75 seconds
**Recording Setup:** Screen share of HDIM demo with Health System seed data loaded (3,600 patients, 280K population, 4 facilities)
**Merge Fields:** [ORG_NAME], [MEASURE_FOCUS], [POPULATION_SIZE]

---

## Pre-Recording Checklist
- [ ] Demo loaded with Health System seed profile
- [ ] System-wide dashboard showing 4-facility heat map (Facility A green, B red, C mixed, D yellow)
- [ ] MIPS composite 62/100 visible with 80 target line
- [ ] [ORG_NAME] overlay applied (if customizing for specific target)

---

## Script

### Opening Hook (0:00-0:08)
"If you've acquired facilities running different EHRs and your quality team is still spending 60% of their time reconciling reports from three or four systems, let me show you what a unified view looks like — because this is the dashboard that didn't exist until this weekend."

### Dashboard Overview (0:08-0:25)
"Here's the system-wide quality view. 280,000 patients, 4 facilities, 520 providers, all in one place. Facility A on Epic — mostly green. Facility B on Meditech — the acquisition from 14 months ago — mostly red. And notice this: the FHIR integration pipeline caught 340 duplicate patients who existed in both Facility A and Facility C — same patients, different medical record numbers, previously double-counted in your quality denominators."

### The Key Insight (0:25-0:45)
"Now drill into Facility B. Diabetes control at 43% — that's 25 points below Facility A's 68%. You probably suspected this, but this is the first time it's quantified with the same measure definition applied to both facilities. No more 'Meditech calculates it differently' debates. And look at the provider level — Dr. Liu at Facility B is actually at 61%, comparable to Facility A. Dr. Stevens is at 34%. The gap isn't the facility, it's specific providers who need support."

### Action View (0:45-0:60)
"Here's the money shot. Six commercial payer contracts with quality-tiered reimbursement. Anthem pays a 3% bonus for hitting 70% diabetes control system-wide. You're at 58%. But if you bring Facility B from 43% to the system average — just the average — your system-wide rate jumps to 62%. Bring it to Facility A's level and you're at 67%. Three points from $2.1 million. And the patients who need help are concentrated in one facility, with one set of providers."

### Closing CTA (0:60-0:75)
"I can show you a unified view across your specific systems — Epic, Meditech, Cerner, eCW, whatever you're running. Takes about 15 minutes to set up. Just reply and I'll send you a link."

---

## Personalization Notes
- If customizing for a specific target, replace [ORG_NAME] with their health system name
- Adjust [POPULATION_SIZE] to match their actual scale (100K-450K range); facility counts and gap numbers scale proportionally
- Swap in their actual EHR names if known (the multi-EHR unification is the primary hook)
- If their trigger is a MIPS penalty rather than payer contract, lead with the 62/100 composite score and $3.8M penalty risk
- For systems mid-acquisition, emphasize the "quality baseline before ink is dry" angle
- If they're planning an enterprise Epic rollout, address it: "HDIM provides unified visibility now, during the 18-24 month migration"
