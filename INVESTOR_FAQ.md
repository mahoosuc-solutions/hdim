# Investor FAQ – Health Data In Motion
**Version:** November 20, 2025  
**Audience:** VC and strategic investors evaluating HDIM

---

## 1. What problem does HDIM solve?
Health systems earn or lose millions based on quality measures, but the underlying workflows are manual, retrospective, and siloed. Coordinators spend up to 16 hours per week compiling spreadsheets, mental health screening is inconsistent, and gaps are discovered months after visits. HDIM automates this end-to-end: real-time screening, automatic gap creation, prioritized routing, and auditable closure.

## 2. How is HDIM different from existing quality vendors?
- **FHIR-native architecture:** We integrate directly with modern APIs; competitors bolt legacy HL7 feeds onto proprietary warehouses, leading to 6-12 month implementations.  
- **Mental health + quality convergence:** We are the only platform that treats depression screening as both a clinical and financial lever, not a standalone behavioral workflow.  
- **Actionable automation:** Instead of dashboards, we deliver tasks, scripts, and documentation inside care team workflows.  
- **Time-to-value:** 8-12 weeks to go live with measurable ROI inside one quarter.

## 3. What does the implementation require from customers?
- 20-25 hours of IT time for FHIR endpoint validation and SSO configuration.  
- 2 clinical champions for workflow design.  
- 6-week phased rollout (pilot clinic → scale).  
HDIM provides implementation PMs, clinical trainers, and a playbook that reuses 80% of configuration work between sites.

## 4. How do you price the product?
Annual subscription indexed to attributed patient volume (tiers starting at $150K for 15K lives). Implementation fee covers data validation, workflow configuration, and training; typically $35K-$45K. Add-on modules—risk stratification, payer connectors, analytics—are priced per module ($25K-$60K each). We target >75% blended gross margin.

## 5. What proof points exist today?
- 3 paying health systems, 2 active pilots.  
- 23 percentage point average improvement in gap closure within 90 days.  
- +0.6 Star rating lift documented by customer QA teams.  
- $265K median net ROI inside five months (quality bonuses + labor savings).  
- 0% logo churn; renewal intent survey at 92%.

## 6. What is the regulatory/compliance posture?
- HIPAA compliant architecture with encryption in transit and at rest.  
- SOC 2 Type I completed (Aug 2025); Type II in progress.  
- HITRUST assessment underway (expected Feb 2026).  
- Business Associate Agreements (BAAs) signed with every customer; data never leaves the customer’s cloud tenancy without encryption.
- **Data consistency assurance:** An AI-driven integration layer reads schemas from Epic Clarity, Cerner HealtheEDW, and other clinical data repositories, then auto-generates deterministic SQL mapping templates into our canonical FHIR store. Large language models flag schema drift, normalize code sets, and require human approval before promotion, creating an auditable chain that keeps quality measures consistent across sites.

## 7. Who are the competitors and how do you win?
- **EHR-native modules (Epic, Cerner):** Slow to configure, limited mental health workflows, heavy IT lift. HDIM wins on speed, specialization, and ROI.  
- **Population health analytics vendors:** Great dashboards, poor workflow integration. We complement data warehouses by triggering action.  
- **Point solutions (behavioral health, risk strat only):** Narrow scope; customers do not want multiple vendors managing similar workflows. HDIM offers a unified quality + behavioral automation layer.

## 8. What is the go-to-market motion?
- Direct sales to mid-market health systems with clinical and quality buyers.  
- Marketing engine anchored by webinar series, ROI calculators, and case studies.  
- Channel partners (RevCycle consultants, behavioral health integrators) to source 30% of pipeline by mid-2026.  
- Land-and-expand: start with depression screening + quality automation; upsell risk stratification, payer connectors, and analytics packs.

## 9. How will new capital be used?
- Scale GTM team (2 AEs, 1 partner manager, 1 demand gen lead).  
- Accelerate product roadmap (measure builder, payer connectors, AI-based care navigation).  
- Expand customer success & clinical ops to maintain sub-12-week implementations.  
- Complete security certifications (SOC 2 Type II, HITRUST) and enhance data residency options.  
- Build investor relations infrastructure (reporting, analyst coverage, customer advisory board).

## 10. What are the biggest risks?
1. **Lengthy health system procurement cycles:** Mitigated by pilot-to-contract playbooks and ROI guarantees.  
2. **Data integration variability:** FHIR maturity differs by EHR; we partner with Redox/Avaneer to abstract differences.  
	- Mitigation: AI-based ingestion continuously inspects new tables, regenerates SQL views, and back-tests measures before release so investors and customers see identical gap calculations regardless of data source.
3. **Behavioral health capacity constraints:** Addressed through tele-psychiatry partnerships and configurable routing that throttles demand.  
4. **Regulatory shifts:** Diversifying into payer connectors ensures we ride changes rather than react.

## 11. What milestones will you hit before the Series A?
- Two new ARR contracts >$500K combined.  
- Publish AHIP co-branded benchmark report.  
- Release self-serve Measure Builder MVP.  
- HITRUST certification complete.  
- Expand ARR to >$2M run rate with <5% monthly cash burn growth.

## 12. Who should investors contact?
Email investors@healthdatainmotion.com to request the data room, schedule demos, or speak with customer references. Monthly metrics briefings occur on the second Thursday (invite distributed upon NDA).
