# ABM Scenario Engine

Account-Based Marketing system for HealthData-in-Motion (HDIM). Research-driven targeting of 10-15 healthcare organizations with archetype-based demos and event-triggered outreach.

**Operated by:** One person (founder-led sales)
**Goal:** Convert qualified healthcare orgs into HDIM pilots via personalized, trigger-driven outreach sequences

---

## Quick Start

1. **Copy** `targets/_template.md` and rename to `targets/[org-name].md`
2. **Research** the organization using public data (CMS, NCQA, LinkedIn, news)
3. **Score** against the [scoring rubric](./scoring-criteria.md) across 5 dimensions (25 points max)
4. **Qualify** -- 18+ out of 25 to proceed; below 18, park in dormant
5. **Assign archetype** from `archetypes/` that best matches the org's profile
6. **Monitor** for trigger events during weekly scans
7. **Execute outreach** using the matching sequence from `templates/` when a trigger fires

---

## Cadences

### Weekly (15 minutes)

Run the [weekly scan checklist](./monitoring/weekly-scan-checklist.md):

- Review Google Alerts digest for each active target
- Check CMS.gov for new quality data releases
- Scan LinkedIn profiles of decision makers for posts, job changes, announcements
- Search Google News for "[Org Name]" + quality / acquisition / value-based / contract
- Check NCQA for accreditation changes or new measure year announcements

Log any findings in [monitoring/trigger-log.md](./monitoring/trigger-log.md). If a trigger fires, initiate outreach within the response window.

### Monthly (30 minutes)

- Re-score all active targets against the rubric
- Rotate out dormant targets (no trigger in 90+ days, score dropped below 18)
- Add 1-2 new candidates from pipeline research
- Refresh dossiers with any new intelligence

---

## File Layout

```
docs/abm/
  README.md                          # This file -- system overview
  scoring-criteria.md                # 5-dimension scoring rubric (18+/25 to qualify)
  archetypes/                        # Org archetype profiles (FQHC, MA Plan, ACO, etc.)
  targets/
    _template.md                     # Blank dossier template
    _tracker.md                      # Master outreach tracker across all targets
    [org-name].md                    # One dossier per qualified target
  templates/
    email-sequences/                 # Multi-touch email templates by archetype
    linkedin-comment-banks/          # Curated comments for LinkedIn engagement
    loom-scripts/                    # Video demo scripts by archetype
  monitoring/
    weekly-scan-checklist.md         # 15-minute weekly scan procedure
    trigger-log.md                   # Log of detected trigger events
  seed-profiles/                     # FHIR seed data profiles for archetype demos
```

---

## Key References

- [Scoring Criteria](./scoring-criteria.md) -- how to score and qualify targets
- [Target Tracker](./targets/_tracker.md) -- master status of all targets
- [Weekly Scan Checklist](./monitoring/weekly-scan-checklist.md) -- trigger monitoring procedure
- [Trigger Log](./monitoring/trigger-log.md) -- detected events and response actions
