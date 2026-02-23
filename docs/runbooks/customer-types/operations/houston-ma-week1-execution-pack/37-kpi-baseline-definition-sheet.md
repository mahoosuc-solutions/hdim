# KPI Baseline Definition Sheet

Prepared: 2026-02-23
Action: A-005 | Owner: Customer Success Lead | Due: 2026-02-26
Purpose: Define each pilot KPI, data source, baseline measurement method, and target for Phase 1.
Instructions: Customer Success Lead and customer program team to assign data owners and confirm baseline values.

---

## 1) Phase 1 Pilot KPIs

### KPI-001: 7-Day Post-Discharge Patient Engagement Rate

| Field | Value |
|---|---|
| Definition | % of discharged patients contacted and engaged via HDIM within 7 calendar days of discharge |
| Numerator | Patients with confirmed engagement event (message read / question submitted / comprehension confirmed) within 7 days |
| Denominator | Total patients discharged within reporting period |
| Target (Pilot) | ≥ 60% engagement rate within 7 days (confirm with customer program team) |
| Baseline value | **Pending — data owner to confirm** |
| Baseline method | Review last 90-day discharge cohort and current outreach contact rate |
| Data source | ADT discharge feed (SS-001) + HDIM engagement event log |
| Data owner (Customer) | **[Name / Role — TBD]** |
| Reporting cadence | Weekly steering update |
| Baseline confirmed by | | Baseline confirmation date | |

---

### KPI-002: Patient Comprehension Confirmation Rate

| Field | Value |
|---|---|
| Definition | % of engaged patients who explicitly confirm understanding of discharge instructions |
| Numerator | Patients with comprehension confirmation event recorded |
| Denominator | Total patients with active discharge instruction delivery within period |
| Target (Pilot) | ≥ 50% comprehension confirmation (early baseline; refine in Week 2 after pilot data) |
| Baseline value | **Pending — typically 0% (no prior structured measurement)** |
| Baseline method | No prior structured measure; pilot establishes new baseline |
| Data source | HDIM comprehension confirmation event log |
| Data owner (Customer) | **[Name / Role — TBD]** |
| Reporting cadence | Weekly steering update |
| Baseline confirmed by | | Baseline confirmation date | |

---

### KPI-003: Median First Response Time

| Field | Value |
|---|---|
| Definition | Median elapsed time from patient question submission to first clinically valid response delivered |
| Numerator | Sum of response times for all responded questions |
| Denominator | Total responded questions within period |
| Target (Pilot) | ≤ 4 hours median first response (business hours; confirm with clinical team) |
| Baseline value | **Pending — data owner to confirm current nurse hotline / portal response SLA** |
| Baseline method | Review current care management / nurse hotline response time data (if available) |
| Data source | HDIM message event log + nurse-workflow service timestamps |
| Data owner (Customer) | **[Name / Role — TBD]** |
| Reporting cadence | Weekly steering update |
| Baseline confirmed by | | Baseline confirmation date | |

---

### KPI-004: Escalation Closure Time

| Field | Value |
|---|---|
| Definition | Median elapsed time from escalation trigger to escalation marked resolved by clinical team |
| Numerator | Sum of escalation resolution times |
| Denominator | Total escalations within period |
| Target (Pilot) | ≤ 24 hours for non-urgent; ≤ 2 hours for urgent-flagged (confirm with clinical champion) |
| Baseline value | **Pending — data owner to confirm** |
| Baseline method | Review current escalation / care management closure data |
| Data source | HDIM nurse-workflow escalation event log |
| Data owner (Customer) | **[Name / Role — TBD]** |
| Reporting cadence | Weekly steering update |
| Baseline confirmed by | | Baseline confirmation date | |

---

## 2) Data Owner Assignment Tracking

| KPI ID | KPI Name | Customer Data Owner | HDIM Measurement Owner | Status |
|---|---|---|---|---|
| KPI-001 | 7-Day Engagement Rate | **[TBD]** | Customer Success Lead | Open |
| KPI-002 | Comprehension Confirmation | **[TBD]** | Customer Success Lead | Open |
| KPI-003 | Median First Response Time | **[TBD]** | Customer Success Lead | Open |
| KPI-004 | Escalation Closure Time | **[TBD]** | Customer Success Lead | Open |

**Action required:** Fill customer data owner column and confirm this table in `06-weekly-steering-update-week1.md`.

---

## 3) Reporting Cadence and Distribution

| Item | Schedule | Owner | Recipient |
|---|---|---|---|
| KPI snapshot | Weekly (per steering meeting) | Customer Success Lead | Executive Sponsor, Clinical Champion, HDIM SA |
| Baseline validation | End of Week 1 | Customer Success Lead + Customer Data Owner | Program leads |
| Pilot result summary | End of Phase 1 | Customer Success Lead | All stakeholders |

---

## 4) Sign-Off (Completion Criteria for A-005)

| Role | Name | Sign-Off | Date |
|---|---|---|---|
| Customer Success Lead (HDIM) | | [ ] Approved | |
| Program Lead (Customer) | | [ ] Confirmed | |
| Clinical Champion (Customer) | | [ ] Confirmed | |

When all sign-off boxes checked, update `04-action-log.md` A-005 to Closed and update `06-weekly-steering-update-week1.md` KPI Snapshot section with confirmed baselines.
