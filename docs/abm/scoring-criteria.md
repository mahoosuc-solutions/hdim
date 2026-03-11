# ABM Target Scoring Criteria

5-dimension rubric for qualifying healthcare organizations. Each dimension scores 1-5. **Threshold: 18+ out of 25 to qualify.**

---

## Scoring Rubric

| Dimension | 1 (Low) | 3 (Medium) | 5 (High) |
|-----------|---------|------------|----------|
| **VBC Exposure** | Fee-for-service only | 1-2 VBC contracts | Active MSSP / ACO REACH / MA Stars |
| **Quality Pain Signals** | No public data | Some quality reporting | Published gaps, Star drops, HEDIS shortfalls |
| **Technology Readiness** | Proprietary / legacy EHR | Partial FHIR capability | Epic / Cerner / Meditech with FHIR R4 |
| **Decision Velocity** | >$500M or 12+ month procurement | $100M-$500M | <$100M, identifiable buyer, <6 month cycle |
| **Trigger Recency** | No recent activity | 90+ day old news | Trigger within 30 days |

---

## Dimension Details

### VBC Exposure (1-5)

How deeply is the org committed to value-based care?

- **1:** Pure fee-for-service, no risk contracts
- **2:** Exploring VBC, maybe one small shared-savings arrangement
- **3:** 1-2 active VBC contracts (e.g., MSSP Track 1, small MA contract)
- **4:** Multiple VBC contracts, moving toward downside risk
- **5:** Active MSSP Track 2+, ACO REACH, significant MA Stars exposure, or capitated arrangements

### Quality Pain Signals (1-5)

Is there evidence they are struggling with quality measurement or reporting?

- **1:** No publicly available quality data, no signals
- **2:** Basic quality reporting but no visible pain
- **3:** Some quality reporting challenges mentioned in press or filings
- **4:** Known HEDIS gaps, below-average Star ratings, or accreditation issues
- **5:** Published Star rating drops, HEDIS shortfalls in public data, CMS penalties, or recent failed audits

### Technology Readiness (1-5)

Can they technically integrate with HDIM?

- **1:** Proprietary or legacy EHR with no interoperability capability
- **2:** Older EHR version, limited data exchange
- **3:** Partial FHIR capability, may need adapter work
- **4:** Modern EHR with FHIR R4 support, some integration experience
- **5:** Epic / Cerner / Meditech Expanse with FHIR R4 APIs enabled, experienced integration team

### Decision Velocity (1-5)

How fast can they move from interest to purchase?

- **1:** Large system (>$500M revenue), 12+ month procurement cycles, committee-driven
- **2:** Mid-large system, 6-12 month cycles
- **3:** Mid-size org ($100M-$500M), identifiable decision makers
- **4:** Smaller org with clear buyer, 3-6 month cycles
- **5:** <$100M revenue, identifiable single buyer or small committee, <6 month cycle, demonstrated ability to move fast on tech

### Trigger Recency (1-5)

How recently has something happened that creates urgency?

- **1:** No recent activity or news
- **2:** Some activity 6+ months ago
- **3:** Relevant news or event 90+ days ago
- **4:** Trigger event within 30-90 days
- **5:** Active trigger within 30 days (Star drop published, leadership change, contract announcement, RFP)

---

## Qualification Threshold

**18+ out of 25 = Qualified.** Proceed with archetype assignment and outreach planning.

**Below 18 = Not qualified.** Park in dormant. Re-evaluate monthly or when a new trigger surfaces.

---

## Scoring Examples

### Example A: Missouri Primary Care Association (FQHC Network) -- 19/25 QUALIFIED

| VBC Exposure | Quality Pain | Tech Readiness | Decision Velocity | Trigger Recency | Total |
|---|---|---|---|---|---|
| 3 | 5 | 3 | 5 | 3 | **19** |

**Rationale:**
- VBC (3): FQHCs participate in UDS reporting and some VBC arrangements, but not deep risk
- Pain (5): UDS quality reporting is manual, HEDIS gaps well-documented in HRSA data, known measurement burden
- Tech (3): Mix of EHRs across member clinics, partial FHIR adoption
- Velocity (5): State PCA is a single buyer for network, small org, fast decision-making
- Trigger (3): HRSA quality reporting cycle creates annual pressure but no acute trigger right now
- **Result:** 19/25 -- qualifies. Assign FQHC archetype.

### Example B: Large Academic Medical Center -- 17/25 NOT QUALIFIED

| VBC Exposure | Quality Pain | Tech Readiness | Decision Velocity | Trigger Recency | Total |
|---|---|---|---|---|---|
| 5 | 3 | 5 | 1 | 3 | **17** |

**Rationale:**
- VBC (5): Deep into ACO REACH, multiple MA contracts, capitated arrangements
- Pain (3): Has quality infrastructure but still has reporting gaps; not in crisis
- Tech (5): Epic with FHIR R4 fully enabled, experienced integration team
- Velocity (1): $2B+ revenue, 12-18 month procurement cycles, IT governance committees
- Trigger (3): Some relevant news but nothing acute
- **Result:** 17/25 -- does not qualify. Procurement cycle too long for founder-led sales. Park in dormant, revisit if trigger fires (e.g., Star rating drop, new quality VP hired).
