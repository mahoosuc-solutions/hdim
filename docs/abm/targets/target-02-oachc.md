# Ohio Association of Community Health Centers (OACHC)

## Score: 21/25
| VBC Exposure | Quality Pain | Tech Readiness | Decision Velocity | Trigger Recency |
|---|---|---|---|---|
| 3 | 5 | 3 | 5 | 5 |

## Organization Profile
- **Type:** State PCA + HCCN, FQHC Network
- **Size:** 58 FQHCs, ~500 sites, 76 of 88 Ohio counties, ~1M patients
- **Geography:** Ohio statewide, headquarters in Columbus
- **EHR:** Mixed across 58 member centers; Ohio Data Integration Platform (ODIP) via Azara Healthcare unifies reporting
- **Payer Mix:** Medicaid ~50% / Medicare ~15% / Uninsured/Sliding Fee ~25% / Commercial ~10%
- **Revenue:** ~$65M (PCA operations); member centers collectively >$1B

## Decision Makers
| Name | Title | LinkedIn | Recent Activity |
|------|-------|----------|-----------------|
| Julie DiRossi-King | President & CEO | [LinkedIn](https://www.linkedin.com/in/julie-dirossi-king-1276795/) | Appointed Aug 2023; previously COO since 2014, Policy Director since 2006 |
| Dr. Heidi Gullett | Chief Medical Officer (CMO) | — | Clinical quality strategy, HEDIS race/ethnicity stratification leadership |
| Teresa Rios-Bishop | Chief Operating Officer (COO) | — | Operational oversight of 58-center network |

## VBC Contracts
- 8 Ohio Medicaid MCOs requiring quality reporting (CareSource, Molina, Anthem, UnitedHealthcare, Aetna, Buckeye, AmeriHealth Caritas, Paramount)
- UDS reporting required for all 58 FQHCs (federal compliance)
- HRSA HCCN grant supporting ODIP data integration platform
- Ohio Medicaid Provider Incentive Program participation
- PCMH recognition across member centers

## Quality Performance
- 62% of UDS clinical quality measures in top national quartiles (2022 data)
- ODIP launched May 2018 with Azara Healthcare — statewide data reporting and analytics platform
- SDOH data integration initiative active through ODIP
- UDS+ FHIR testing completed March 2024 — early adopter status
- HEDIS stratification now requires race/ethnicity data for 22 measures (NCQA MY 2024+)
- Ohio Medicaid requires MCO self-reported and audited HEDIS results with member-level detail files

## Recent Intelligence
| Date | Source | Summary | URL |
|------|--------|---------|-----|
| 2024-03 | OACHC | UDS+ FHIR testing completed — OACHC among early PCA testers | https://www.ohiochc.org/ |
| 2025-Q1 | Ohio Dept of Medicaid | CY 2025 MCO HEDIS submission specs released — member-level detail (MLD) now required for all measures | https://medicaid.ohio.gov/ |
| 2025-Q2 | NCQA | HEDIS MY 2026 adds 7 new measures, transitions 4 to ECDS — impacts Ohio CHC reporting | https://www.ncqa.org/ |
| 2023-08 | OACHC | Julie DiRossi-King appointed President & CEO after 17 years at OACHC | https://www.ohiochc.org/news/ |
| 2025-Q3 | Azara Healthcare | OACHC featured as statewide health transformation success story via ODIP | https://www.azarahealthcare.com/ |

## Pain Hypothesis
OACHC faces the most complex multi-payer quality reporting challenge of any state PCA: 58 FQHCs must report to 8 different Medicaid MCOs (each with slightly different quality measure specifications), plus federal UDS/UDS+ requirements, plus HEDIS stratification by race/ethnicity across 22 measures. Their ODIP platform (Azara DRVS) handles UDS aggregation but was not designed for multi-MCO HEDIS reconciliation or FHIR R4-native data exchange. HDIM solves this by providing: (1) automated HEDIS measure evaluation via CQL that maps to each MCO's specific reporting requirements, (2) race/ethnicity stratification engine built natively into the measure evaluation pipeline, (3) FHIR R4 data transformation that bridges ODIP's aggregated data to MCO-specific submission formats, (4) a care gap identification layer that works across all 8 MCO contracts simultaneously. The scale — 1M patients across 58 centers — means even small improvements in quality measure performance translate to significant Medicaid incentive revenue.

## Trigger Events
| Date | Trigger Type | Description | Urgency |
|------|-------------|-------------|---------|
| 2025-Q1 | Regulatory | Ohio Medicaid CY 2025 HEDIS specs require member-level detail for all measures | Critical |
| 2025-Q2 | Regulatory | HEDIS MY 2026 adds 7 measures + 4 ECDS transitions — expands reporting burden | Critical |
| 2024-03 | Technology | UDS+ FHIR testing completed — OACHC ready for FHIR-native infrastructure | High |
| 2025-Q3 | Strategic | NCQA race/ethnicity stratification for 22 HEDIS measures — new data collection and reporting requirements | High |
| 2026-Q3 | Conference | OACHC Annual Conference (date TBD) — leadership gathering of 58 CHCs | Medium |

## Archetype Match
- **Primary:** 01-FQHC
- **Overlay Notes:** Largest state PCA in the Midwest by site count (~500 sites). The 8-MCO reporting complexity is a unique differentiator — no other target has this level of multi-payer quality reporting burden. New CEO (DiRossi-King, 2023) may be open to strategic technology investments. ODIP/Azara DRVS is deeply embedded, so HDIM positioning should emphasize complementary capabilities (FHIR R4, CQL evaluation, multi-MCO reconciliation) rather than platform replacement. Early UDS+ FHIR testing signals tech-forward leadership.

## Outreach Status
| Touch | Date | Channel | Content | Response |
|-------|------|---------|---------|----------|
| T1 | — | — | — | — |
| T2 | — | — | — | — |
| T3 | — | — | — | — |

## Next Action
Engage Julie DiRossi-King (CEO) with outreach focused on the 8-MCO quality reporting complexity and HEDIS race/ethnicity stratification challenge. Reference their early UDS+ FHIR testing as evidence of tech readiness. Position HDIM as the CQL evaluation and multi-payer reconciliation layer that sits on top of their existing ODIP/Azara investment. Timeline: initiate T1 outreach by April 2026.
