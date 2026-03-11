# Loom Walkthrough Script: ACO — Under the Gun

**Duration:** 60-75 seconds
**Recording Setup:** Screen share of HDIM demo with ACO seed data loaded (1,800 patients, 52K attributed lives, 34 practice sites)
**Merge Fields:** [ORG_NAME], [MEASURE_FOCUS], [POPULATION_SIZE]

---

## Pre-Recording Checklist
- [ ] Demo loaded with ACO seed profile
- [ ] Quality Gate Scorecard showing 8/10 passing, 2 red (Flu Vaccination, Falls Risk)
- [ ] 60-day countdown timer visible
- [ ] [ORG_NAME] overlay applied (if customizing for specific target)

---

## Script

### Opening Hook (0:00-0:08)
"If you're managing quality across a network of independent practices with a reporting deadline 60 days out, you already know the spreadsheet-and-EHR-report approach is breaking. Let me show you what it looks like when all four of your EHR systems speak the same language."

### Dashboard Overview (0:08-0:25)
"Here's the Quality Gate Scorecard. Ten measures, eight passing, two failing — flu vaccination at 61%, need 70%, and falls risk screening stuck at 55%, need 65%. The countdown says 60 days. But the good news: 87 flu shots were captured over the weekend from CVS and Walgreens pharmacy data flowing in automatically. Flu rate just moved from 60.4 to 61.1. At this pace, you'll cross 70% with 15 days of cushion."

### The Key Insight (0:25-0:45)
"Falls risk is the problem — it's flat. 1,890 open gaps. But look at the provider-level breakdown. Dr. Morrison's practice on Epic has a 74% falls screening rate — she built a standing order set. Dr. Keller's practice on eClinicalWorks is at 31%. Same Medicare population. And here's why your current approach can't show you this — four EHRs calculate falls screening differently. Epic counts the screening tool. eCW counts a diagnosis code. HDIM runs the official CQL logic against FHIR-normalized data from all four systems. One number. One truth."

### Action View (0:45-0:60)
"Now watch this. Toggle flu to 70% — one gate turns green. Toggle falls risk to 65% — both gates green. Shared savings unlocked: $4.2 million distributed to 120 participating providers. Two measures. 60 days. And here's the outreach list for each, broken down by practice site, with patients who have appointments in the next two weeks right at the top."

### Closing CTA (0:60-0:75)
"Want to see what this looks like across your provider network? I can map [ORG_NAME]'s measures and EHR systems into this view. Takes about 15 minutes to set up. Just reply and we'll find a time."

---

## Personalization Notes
- If customizing for a specific target, replace [ORG_NAME] with their ACO name
- Adjust [POPULATION_SIZE] to match their attributed lives (30K-75K range); savings dollars scale proportionally
- If their failing gates are different (e.g., diabetes + depression instead of flu + falls), reorder the narrative
- Reference their specific EHR mix if known — the cross-EHR unification story is the strongest hook for ACOs
- For ACO REACH participants, mention the APP Plus expansion (6 to 8 to 11 measures by 2028) as a scaling concern
