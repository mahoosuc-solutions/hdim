# CMS & Public Health Tech Requirements 2024-2025
**Research Date:** December 14, 2024
**Purpose:** Identify feature opportunities for HDIM roadmap and YC application

---

## Executive Summary

Federal regulations are driving healthcare towards digital-first, FHIR-based, real-time data exchange. HDIM's architecture is perfectly positioned for these requirements. Key opportunities:

1. **Digital Quality Measures (dQM)** - CMS wants all-digital by 2025, NCQA by 2030
2. **SDOH Screening & Reporting** - Mandatory as of 2024, expanding to outpatient
3. **Prior Authorization APIs** - Required by 2027, massive pain point
4. **HCC Risk Adjustment V28** - Full implementation 2026, coding accuracy critical
5. **TEFCA/Interoperability** - National health data exchange network live
6. **Electronic Case Reporting (eCR)** - Real-time public health reporting

---

## 1. Quality Reporting Requirements (APP Plus)

### Timeline
- **2024:** Final year for CMS Web Interface reporting
- **2025:** Must report eCQMs, CQMs, or Medicare CQMs (no more Web Interface)
- **2025:** 100% CEHRT compliance required for ACOs
- **2028:** APP Plus expands to 11 measures

### New APP Plus Measures (Rolling Out)
| Year | New Measure |
|------|-------------|
| 2025 | Breast cancer screening |
| 2026 | Colon cancer screening |
| 2027 | Treatment of substance use disorder |
| 2028 | Screening for SDOH, Adult immunization |

### HDIM Feature Opportunities
- [ ] **APP Plus measure set** - Pre-built measures for 2025-2028 roadmap
- [ ] **eCQM export** - Generate compliant QRDA files for submission
- [ ] **Medicare CQM support** - Transitional step before full eCQM
- [ ] **CEHRT compliance dashboard** - Help ACOs track their 100% requirement

**Sources:**
- [MDinteractive - 2025 QPP Final Rule](https://mdinteractive.com/mips-blog/cms-releases-2025-quality-payment-program-final-rule-what-it-means-mips-reporting)
- [Medisolv - 2024 APP Requirements](https://blog.medisolv.com/articles/2024-app-reporting-requirements-acos)

---

## 2. Digital Quality Measures (dQM)

### CMS Vision
- **Goal:** All quality measures digital by 2025
- **Reality:** Phased transition through 2030
- **Standard:** CQL + FHIR (exactly what HDIM uses)

### NCQA HEDIS Digital Transition
- Fully digital by 2030
- Digital Content Services (DCS) launched for 2024 measure year
- All organizations using NCQA measures must be certified

### Technical Requirements
- CQL for measure logic
- FHIR R4 for data model
- Automated data capture from EHRs, claims, registries, HIEs

### HDIM Feature Opportunities
- [x] **CQL-native engine** - Already built
- [x] **FHIR R4 data model** - Already built
- [ ] **NCQA DCS integration** - Direct submission to NCQA
- [ ] **dQM certification** - Get measures certified by NCQA
- [ ] **Real-time calculation** - Key differentiator vs batch

**Sources:**
- [NCQA - Digital Quality Measures](https://www.ncqa.org/hedis/the-future-of-hedis/digital-measures/)
- [eCQI Resource Center - dQM Roadmap](https://ecqi.healthit.gov/dqm)

---

## 3. SDOH Screening & Reporting (Mandatory 2024)

### Requirements
- **SDOH-1:** Screening rate measure
- **SDOH-2:** Screen positive rate measure
- **Domains (HRSN):** Food insecurity, housing instability, transportation, utilities, interpersonal safety
- **Timeline:** Voluntary 2023, Required 2024, Submission May 2025

### Z Code Documentation
- ICD-10-CM Chapter 21 codes for social needs
- Example: Z59.82 (Transportation insecurity)
- HCPCS G0136 for SDOH risk assessment

### 2025 Expansion
- Expanding to outpatient settings
- Integration into payment models
- Homelessness now a CC (complication/comorbidity) for inpatient

### HDIM Feature Opportunities
- [ ] **SDOH screening measure templates** - SDOH-1 and SDOH-2 out of box
- [ ] **Z code tracking** - Auto-identify SDOH gaps from Z code presence/absence
- [ ] **HRSN dashboard** - Aggregate view of social needs in population
- [ ] **SDOH-to-intervention workflow** - Connect gaps to community resources
- [ ] **G0136 billing support** - Track eligible AWV + SDOH combinations

**Sources:**
- [CMSA - SDOH Requirements 2024-2025](https://cmsatoday.com/2024/12/30/2024-sdoh-social-determinants-of-health-sdoh-requirements-and-what-lies-ahead-in-2025/)
- [Medisolv - CMS SDOH Measures](https://blog.medisolv.com/articles/intro-cms-sdoh-measures)

---

## 4. CMS Interoperability & Prior Authorization APIs

### Timeline
- **Jan 1, 2026:** Patient Access API, Provider Access API, Payer-to-Payer API
- **Jan 1, 2027:** Prior Authorization API (FHIR-based)

### Prior Authorization Requirements
- 72 hours for expedited (urgent) requests
- 7 calendar days for standard requests
- Must provide: covered items list, documentation requirements, approval/denial/request more info

### Technical Standards
- HL7 FHIR R4.0.1
- Provider Access API for claims, USCDI data, prior auth info

### Impacted Payers
- Medicare Advantage
- Medicaid/CHIP FFS and managed care
- QHPs on federal exchanges

### HDIM Feature Opportunities
- [ ] **Prior auth status tracking** - Integrate with payer APIs
- [ ] **Prior auth prediction** - Flag likely denials based on gaps
- [ ] **Provider Access API consumer** - Pull payer data for complete picture
- [ ] **Documentation automation** - Generate required clinical documentation

**Sources:**
- [CMS - Interoperability Final Rule](https://www.cms.gov/priorities/burden-reduction/overview/interoperability/policies-and-regulations/cms-interoperability-and-prior-authorization-final-rule-cms-0057-f)
- [Federal Register - Prior Auth Rule](https://www.federalregister.gov/documents/2024/02/08/2024-00895/medicare-and-medicaid-programs-patient-protection-and-affordable-care-act-advancing-interoperability)

---

## 5. TEFCA (National Health Data Exchange)

### Current State (2024)
- Common Agreement v2.1 released October 2024
- FHIR-based transactions required
- 7 QHINs live: CommonWell, eHealth Exchange, Epic Nexus, Health Gorilla, Kno2, KONZA, MedAllies

### Exchange Purposes Supported
- Treatment
- Payment
- Healthcare operations
- Public health
- Government benefits determination
- Individual access services

### Public Health Updates (August 2024)
- New SOPs for electronic case reporting (eCR)
- Electronic lab reporting (ELR) use cases
- Enhanced public health data exchange

### HDIM Feature Opportunities
- [ ] **QHIN connectivity** - Query patient data via TEFCA
- [ ] **Cross-organizational quality** - Calculate measures across fragmented care
- [ ] **Attribution reconciliation** - Use TEFCA to find patients attributed elsewhere
- [ ] **Care coordination alerts** - Notify when patient seen at other facilities

**Sources:**
- [HealthIT.gov - TEFCA](https://www.healthit.gov/topic/interoperability/policy/trusted-exchange-framework-and-common-agreement-tefca)
- [Sequoia Project - TEFCA RCE](https://rce.sequoiaproject.org/tefca/)

---

## 6. HCC Risk Adjustment V28 (Full Implementation 2026)

### Transition Timeline
- **2024:** 67% V24 / 33% V28
- **2025:** 33% V24 / 67% V28
- **2026:** 100% V28

### Key Changes
- 115 HCCs (up from 86 in V24)
- 2,264 fewer diagnosis codes map to HCCs
- Focus on severity over quantity
- "Constraining" - related HCCs get same coefficients
- Diabetes example: uncomplicated vs. with complications now equal weight

### Documentation Requirements
- Greater precision in code assignment
- Comprehensive clinical evidence required
- Cross-mapping to both V24 and V28 during transition

### Financial Impact
- 2.16% decrease in average risk scores (2024)
- 2.45% decrease (2025)
- $11B savings to Medicare Trust Fund

### HDIM Feature Opportunities
- [ ] **HCC gap identification** - Identify undercoded conditions
- [ ] **V28 impact analysis** - Show organizations their transition risk
- [ ] **Documentation sufficiency** - Flag records needing more specificity
- [ ] **Dual-model scoring** - Calculate both V24 and V28 during transition
- [ ] **Condition crosswalk** - Map diagnoses that changed HCC assignment

**Sources:**
- [Chess Health - CMS-HCC V28 Transition](https://www.chesshealthsolutions.com/2025/04/17/the-future-of-risk-adjustment-transitioning-to-cms-hcc-v28/)
- [Forvis Mazars - V28 Implications](https://www.forvismazars.us/forsights/2024/10/what-s-new-cms-hcc-version-28-risk-adjustment-implications)

---

## 7. Electronic Case Reporting (eCR)

### Requirements
- Required since January 1, 2022 (Promoting Interoperability Program)
- 56,500+ facilities now live
- HL7 FHIR eCR IG 2.1.0 required by December 31, 2025
- USCDI v3 required by December 31, 2025

### Technical Standards
- FHIR-based eICR (electronic Initial Case Report)
- Transmission via APHL AIMS platform
- Automated triggers from EHR data

### HDIM Feature Opportunities
- [ ] **Reportable condition detection** - Flag eCR-triggering diagnoses
- [ ] **eICR generation** - Create compliant case reports
- [ ] **AIMS integration** - Transmit to public health via AIMS
- [ ] **Syndromic surveillance** - Aggregate patterns for public health

**Sources:**
- [CDC - What is eCR](https://www.cdc.gov/ecr/php/about/index.html)
- [HealthIT.gov - eCR Transmission](https://www.healthit.gov/test-method/transmission-public-health-agencies-electronic-case-reporting)

---

## 8. USCDI v4 Data Elements (August 2024)

### New Data Class
- **Facility Information:** Identifier, Type, Name

### New Data Elements
- Alcohol Use Assessment
- Substance Use Assessment
- Physical Activity Assessment
- Treatment Intervention Preference
- Care Experience Preference
- Medication Instructions
- Medication Adherence
- Substance (non-medication) allergies
- Encounter Identifier
- 6 new Laboratory elements
- Performance Time (Procedures)

### Timeline
- v3 currently required (Cures Act)
- v4 proposed required January 1, 2028 (HTI-2)
- Available for voluntary adoption since August 2024

### HDIM Feature Opportunities
- [ ] **USCDI v4 data model** - Support all new elements
- [ ] **Assessment tracking** - Alcohol, substance, physical activity
- [ ] **Preference documentation** - Treatment and care experience
- [ ] **Medication adherence gaps** - Identify non-adherence patterns

**Sources:**
- [HealthIT.gov - USCDI](https://www.healthit.gov/isp/united-states-core-data-interoperability-uscdi)
- [Healthcare IT News - USCDI v4](https://www.healthcareitnews.com/news/onc-publishes-new-uscdi-v4-standards)

---

## Feature Prioritization Matrix

### High Priority (Address 2024-2025 Requirements)
| Feature | Regulatory Driver | Timeline |
|---------|------------------|----------|
| eCQM/QRDA export | Web Interface sunset | 2025 |
| SDOH-1/SDOH-2 measures | IQR mandate | Now |
| APP Plus measures | MSSP ACO requirement | 2025+ |
| dQM certification | NCQA digital transition | 2025-2030 |

### Medium Priority (2026-2027 Requirements)
| Feature | Regulatory Driver | Timeline |
|---------|------------------|----------|
| HCC V28 gap analysis | Risk adjustment transition | 2026 |
| Prior auth API integration | CMS interoperability | 2027 |
| Provider Access API | CMS interoperability | 2026 |
| eCR generation | Promoting Interoperability | Dec 2025 |

### Strategic (Competitive Differentiation)
| Feature | Value Proposition |
|---------|-------------------|
| TEFCA/QHIN connectivity | Cross-organizational quality |
| Real-time SDOH interventions | Beyond just screening |
| V28 documentation AI | Coding accuracy improvement |
| Care coordination alerts | Reduce fragmentation |

---

## Key Messages for YC Application

### Regulatory Tailwinds
1. **CMS Web Interface sunset (2025)** forces ACOs to modernize
2. **100% CEHRT requirement (2025)** eliminates paper holdouts
3. **dQM transition (2025-2030)** validates our CQL/FHIR architecture
4. **Prior auth APIs (2027)** create new integration opportunities
5. **TEFCA live** enables national data exchange we can leverage

### HDIM Positioning
> "We're not just building what healthcare needs today - we're building what regulations will require tomorrow. CMS's all-digital quality mandate, NCQA's 2030 HEDIS vision, and ONC's FHIR requirements all validate our architectural decisions. Legacy platforms are technical debt; we're the upgrade path."

---

## Sources Summary

- [CMS - Interoperability Final Rule](https://www.cms.gov/priorities/burden-reduction/overview/interoperability/policies-and-regulations/cms-interoperability-and-prior-authorization-final-rule-cms-0057-f)
- [MDinteractive - 2025 MIPS Rules](https://mdinteractive.com/2025-mips-rules)
- [NCQA - Digital Quality Measures](https://www.ncqa.org/hedis/the-future-of-hedis/digital-measures/)
- [eCQI Resource Center - dQM Roadmap](https://ecqi.healthit.gov/dqm)
- [HealthIT.gov - TEFCA](https://www.healthit.gov/topic/interoperability/policy/trusted-exchange-framework-and-common-agreement-tefca)
- [CDC - Electronic Case Reporting](https://www.cdc.gov/ecr/php/about/index.html)
- [CMSA - SDOH Requirements](https://cmsatoday.com/2024/12/30/2024-sdoh-social-determinants-of-health-sdoh-requirements-and-what-lies-ahead-in-2025/)
- [Chess Health - HCC V28](https://www.chesshealthsolutions.com/2025/04/17/the-future-of-risk-adjustment-transitioning-to-cms-hcc-v28/)
