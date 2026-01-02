# Documentation Portal Directory Structure Specification

**Document Version**: 1.0
**Date**: December 1, 2025
**Owner**: Agent 1 - Documentation Architect
**Status**: Final Specification

---

## Purpose

This document provides the **exact, implementation-ready directory structure** for all three documentation portals. This is not a conceptual design - this is the precise folder hierarchy, file naming conventions, and organizational patterns that must be implemented.

---

## Table of Contents

1. [Complete Directory Structure](#complete-directory-structure)
2. [Naming Conventions](#naming-conventions)
3. [Directory Descriptions](#directory-descriptions)
4. [File Inventory](#file-inventory)
5. [Implementation Checklist](#implementation-checklist)

---

## Complete Directory Structure

```
/docs/
├── product/                              # Product Documentation Portal (25 docs, 45,000 words)
│   ├── 01-product-overview/              # Strategic product information (4 docs)
│   │   ├── vision-and-strategy.md
│   │   ├── core-capabilities.md
│   │   ├── value-proposition.md
│   │   └── competitive-differentiation.md
│   │
│   ├── 02-architecture/                  # Technical architecture (6 docs)
│   │   ├── system-architecture.md
│   │   ├── integration-patterns.md
│   │   ├── data-model.md
│   │   ├── security-architecture.md
│   │   ├── performance-benchmarks.md
│   │   └── disaster-recovery.md
│   │
│   ├── 03-implementation/                # Deployment and implementation (4 docs)
│   │   ├── deployment-options.md
│   │   ├── requirements-and-prerequisites.md
│   │   ├── implementation-roadmap.md
│   │   └── configuration-guide.md
│   │
│   ├── 04-case-studies/                  # Customer success stories (3 docs)
│   │   ├── healthcare-system-case-study.md
│   │   ├── ambulatory-network-case-study.md
│   │   └── risk-based-organization-case-study.md
│   │
│   └── 05-supporting/                    # Additional product materials (6 docs)
│       ├── fhir-integration-guide.md
│       ├── pricing-and-licensing.md
│       ├── security-audit-summary.md
│       ├── licensing-options.md
│       ├── performance-testing-results.md
│       └── compliance-certifications.md
│
├── users/                                # User Documentation Portal (50 docs, 37,500 words)
│   ├── 01-getting-started/              # Onboarding materials (3 docs)
│   │   ├── new-user-orientation.md
│   │   ├── user-roles-and-permissions.md
│   │   └── first-day-checklist.md
│   │
│   ├── 02-role-specific-guides/         # Guides by user role (4 subdirs, 20 docs)
│   │   ├── physician/
│   │   │   ├── physician-dashboard.md
│   │   │   ├── patient-search-and-review.md
│   │   │   ├── care-gap-identification.md
│   │   │   ├── care-gap-closure.md
│   │   │   ├── quality-measure-interpretation.md
│   │   │   ├── clinical-alerts.md
│   │   │   └── physician-faq.md
│   │   │
│   │   ├── care-manager/
│   │   │   ├── care-manager-dashboard.md
│   │   │   ├── gap-assignment-and-prioritization.md
│   │   │   ├── patient-outreach-workflows.md
│   │   │   ├── risk-stratification-guide.md
│   │   │   ├── outcome-documentation.md
│   │   │   └── care-manager-faq.md
│   │   │
│   │   ├── medical-assistant/
│   │   │   ├── medical-assistant-workflows.md
│   │   │   ├── data-entry-guide.md
│   │   │   ├── patient-communication.md
│   │   │   └── medical-assistant-faq.md
│   │   │
│   │   └── administrator/
│   │       ├── system-configuration.md
│   │       ├── user-management.md
│   │       ├── data-import-and-management.md
│   │       ├── integration-setup.md
│   │       ├── reporting-and-analytics.md
│   │       └── administrator-faq.md
│   │
│   ├── 03-feature-guides/               # Feature-specific guides (8 docs)
│   │   ├── dashboard-navigation.md
│   │   ├── patient-search-best-practices.md
│   │   ├── care-gap-management-workflows.md
│   │   ├── evaluations-and-reporting.md
│   │   ├── quality-measures-evaluation.md
│   │   ├── batch-operations.md
│   │   ├── data-export-guide.md
│   │   └── alert-management.md
│   │
│   ├── 04-troubleshooting/              # Problem resolution (4 docs)
│   │   ├── common-issues-and-solutions.md
│   │   ├── error-codes-reference.md
│   │   ├── faq-general.md
│   │   └── accessibility-troubleshooting.md
│   │
│   └── 05-reference/                    # Reference materials (7 docs)
│       ├── terminology-glossary.md
│       ├── keyboard-shortcuts.md
│       ├── hedis-measures-reference.md
│       ├── data-standards-and-formats.md
│       ├── security-and-privacy-policies.md
│       ├── quick-reference-guides.md
│       └── accessibility-guide.md
│
└── sales/                               # Sales Documentation Portal (40 docs, 30,000 words)
    ├── 01-sales-enablement/             # Sales process and strategy (4 docs)
    │   ├── sales-process-playbook.md
    │   ├── product-positioning-and-messaging.md
    │   ├── objection-handling-guide.md
    │   └── competitive-analysis.md
    │
    ├── 02-segments-and-usecases/        # Customer segments and use cases (12 docs)
    │   ├── segments/
    │   │   ├── healthcare-systems-sales-kit.md
    │   │   ├── ambulatory-networks-sales-kit.md
    │   │   ├── specialty-care-sales-kit.md
    │   │   ├── risk-based-organizations-sales-kit.md
    │   │   ├── small-practices-sales-kit.md
    │   │   └── accountable-care-organizations-sales-kit.md
    │   │
    │   └── use-cases/
    │       ├── quality-measure-improvement.md
    │       ├── care-gap-management.md
    │       ├── risk-stratification.md
    │       ├── population-health-management.md
    │       ├── mental-health-screening.md
    │       └── medication-adherence.md
    │
    ├── 03-sales-tools/                  # Sales collateral and tools (8 docs)
    │   ├── demo-script-library.md
    │   ├── email-template-library.md
    │   ├── one-pager-templates.md
    │   ├── roi-calculator-guide.md
    │   ├── proposal-templates.md
    │   ├── pricing-guide.md
    │   ├── presentation-deck-library.md
    │   └── discovery-question-framework.md
    │
    ├── 04-case-studies/                 # Customer success stories (4 docs)
    │   ├── case-study-clinical-outcomes.md
    │   ├── case-study-financial-impact.md
    │   ├── case-study-implementation-success.md
    │   └── case-study-customer-testimonials.md
    │
    └── 05-supporting/                   # Supporting sales materials (6 docs)
        ├── sales-training-manual.md
        ├── partner-sales-playbook.md
        ├── objection-response-library.md
        ├── sales-resources-and-tools.md
        ├── sales-faq.md
        └── sales-content-index.md
```

---

## Naming Conventions

### Directory Naming Rules

**Format**: `##-category-name/`

**Rules**:
1. All directory names MUST use lowercase
2. Use hyphens (`-`) to separate words, never underscores or spaces
3. Prefix with two-digit number for ordering (`01-`, `02-`, etc.)
4. Use descriptive, scannable names (no abbreviations unless industry-standard)
5. Maximum length: 40 characters

**Examples**:
- ✅ `01-product-overview/`
- ✅ `02-role-specific-guides/`
- ✅ `05-supporting/`
- ❌ `ProductOverview/` (no prefix, uppercase)
- ❌ `02_role_specific_guides/` (underscores)
- ❌ `supporting docs/` (space, no prefix)

### File Naming Rules

**Format**: `descriptive-file-name.md`

**Rules**:
1. All file names MUST use lowercase
2. Use hyphens (`-`) to separate words
3. Use `.md` extension for all documentation files
4. Be descriptive and specific
5. Use singular or plural consistently within categories
6. Maximum length: 60 characters
7. No prefixes (folder structure provides ordering)

**Examples**:
- ✅ `vision-and-strategy.md`
- ✅ `healthcare-systems-sales-kit.md`
- ✅ `new-user-orientation.md`
- ❌ `Vision_And_Strategy.md` (underscores, capitalization)
- ❌ `01-vision.md` (unnecessary prefix)
- ❌ `vis-strat.md` (unclear abbreviations)

### Special Cases

**Multi-word Concepts**: Use hyphens
- `care-gap-management.md` (not `care_gap_management.md`)

**Acronyms**: Use lowercase with hyphens if needed
- `hedis-measures-reference.md` (not `HEDIS-Measures-Reference.md`)
- `fhir-integration-guide.md` (not `FHIR_Integration_Guide.md`)

**Version Numbers**: If needed, append to filename
- `api-integration-guide-v2.md` (only if multiple versions exist)

---

## Directory Descriptions

### Product Documentation Portal (`/docs/product/`)

**Target Audience**: Healthcare executives, buyers, evaluators, C-suite
**Purpose**: Demonstrate product value, capabilities, and technical excellence
**Total**: 25 documents, ~45,000 words

#### `01-product-overview/` (4 docs)
Strategic product information for decision-makers
- Vision and roadmap
- Core capabilities and features
- Value proposition and ROI
- Competitive positioning

#### `02-architecture/` (6 docs)
Technical architecture for CIOs, CTOs, technical evaluators
- System design and components
- Integration patterns
- Data models
- Security architecture
- Performance metrics
- Disaster recovery planning

#### `03-implementation/` (4 docs)
Deployment and implementation guidance
- Cloud, on-premise, hybrid options
- System requirements
- Implementation timeline and phases
- Configuration procedures

#### `04-case-studies/` (3 docs)
Real-world customer success stories
- Healthcare system implementation
- Ambulatory network results
- Risk-based organization outcomes

#### `05-supporting/` (6 docs)
Additional technical and business materials
- FHIR integration details
- Pricing models
- Security audit summaries
- Licensing options
- Performance testing data
- Compliance certifications

---

### User Documentation Portal (`/docs/users/`)

**Target Audience**: Clinical staff, administrators, end-users
**Purpose**: Enable effective daily system usage and self-service learning
**Total**: 50 documents, ~37,500 words

#### `01-getting-started/` (3 docs)
Onboarding materials for new users
- System orientation
- Role descriptions and permissions
- First-day setup checklist

#### `02-role-specific-guides/` (20 docs in 4 subdirs)
Detailed workflow guides by user role

**`physician/` (7 docs)**
- Dashboard navigation
- Patient search and review
- Care gap workflows
- Quality measure interpretation
- Clinical alerts

**`care-manager/` (6 docs)**
- Care gap assignment
- Patient outreach
- Risk stratification
- Outcome tracking

**`medical-assistant/` (4 docs)**
- Daily workflows
- Data entry procedures
- Patient communication

**`administrator/` (6 docs)**
- System configuration
- User management
- Data import/export
- Integration setup
- Reporting

#### `03-feature-guides/` (8 docs)
Cross-role feature documentation
- Dashboard features
- Search functionality
- Care gap management
- Evaluations and reports
- Quality measures
- Batch operations
- Data export
- Alert management

#### `04-troubleshooting/` (4 docs)
Problem resolution resources
- Common issues and solutions
- Error code reference
- General FAQ
- Accessibility issues

#### `05-reference/` (7 docs)
Quick reference and lookup materials
- Terminology glossary
- Keyboard shortcuts
- HEDIS measures
- Data standards
- Security policies
- Quick reference cards
- Accessibility guide

---

### Sales Documentation Portal (`/docs/sales/`)

**Target Audience**: Sales team, sales engineers, channel partners
**Purpose**: Enable effective selling and deal closure
**Total**: 40 documents, ~30,000 words

#### `01-sales-enablement/` (4 docs)
Sales process and methodology
- Sales playbook and process
- Product positioning
- Objection handling
- Competitive analysis

#### `02-segments-and-usecases/` (12 docs in 2 subdirs)

**`segments/` (6 docs)**
Industry segment sales kits
- Healthcare systems
- Ambulatory networks
- Specialty care
- Risk-based organizations
- Small practices
- ACOs

**`use-cases/` (6 docs)**
Specific use case documentation
- Quality measure improvement
- Care gap management
- Risk stratification
- Population health
- Mental health screening
- Medication adherence

#### `03-sales-tools/` (8 docs)
Practical sales collateral
- Demo scripts
- Email templates
- One-pagers
- ROI calculator
- Proposal templates
- Pricing guide
- Presentation decks
- Discovery questions

#### `04-case-studies/` (4 docs)
Customer success stories for sales
- Clinical outcomes
- Financial impact
- Implementation success
- Customer testimonials

#### `05-supporting/` (6 docs)
Additional sales resources
- Sales training manual
- Partner playbook
- Objection responses
- Resource library
- Sales FAQ
- Content index

---

## File Inventory

### Complete Document List (115 Total)

#### Product Portal (25 docs)

**01-product-overview/** (4)
1. vision-and-strategy.md
2. core-capabilities.md
3. value-proposition.md
4. competitive-differentiation.md

**02-architecture/** (6)
5. system-architecture.md
6. integration-patterns.md
7. data-model.md
8. security-architecture.md
9. performance-benchmarks.md
10. disaster-recovery.md

**03-implementation/** (4)
11. deployment-options.md
12. requirements-and-prerequisites.md
13. implementation-roadmap.md
14. configuration-guide.md

**04-case-studies/** (3)
15. healthcare-system-case-study.md
16. ambulatory-network-case-study.md
17. risk-based-organization-case-study.md

**05-supporting/** (6)
18. fhir-integration-guide.md
19. pricing-and-licensing.md
20. security-audit-summary.md
21. licensing-options.md
22. performance-testing-results.md
23. compliance-certifications.md

**Subtotal**: 25 documents

---

#### User Portal (50 docs)

**01-getting-started/** (3)
1. new-user-orientation.md
2. user-roles-and-permissions.md
3. first-day-checklist.md

**02-role-specific-guides/physician/** (7)
4. physician-dashboard.md
5. patient-search-and-review.md
6. care-gap-identification.md
7. care-gap-closure.md
8. quality-measure-interpretation.md
9. clinical-alerts.md
10. physician-faq.md

**02-role-specific-guides/care-manager/** (6)
11. care-manager-dashboard.md
12. gap-assignment-and-prioritization.md
13. patient-outreach-workflows.md
14. risk-stratification-guide.md
15. outcome-documentation.md
16. care-manager-faq.md

**02-role-specific-guides/medical-assistant/** (4)
17. medical-assistant-workflows.md
18. data-entry-guide.md
19. patient-communication.md
20. medical-assistant-faq.md

**02-role-specific-guides/administrator/** (6)
21. system-configuration.md
22. user-management.md
23. data-import-and-management.md
24. integration-setup.md
25. reporting-and-analytics.md
26. administrator-faq.md

**03-feature-guides/** (8)
27. dashboard-navigation.md
28. patient-search-best-practices.md
29. care-gap-management-workflows.md
30. evaluations-and-reporting.md
31. quality-measures-evaluation.md
32. batch-operations.md
33. data-export-guide.md
34. alert-management.md

**04-troubleshooting/** (4)
35. common-issues-and-solutions.md
36. error-codes-reference.md
37. faq-general.md
38. accessibility-troubleshooting.md

**05-reference/** (7)
39. terminology-glossary.md
40. keyboard-shortcuts.md
41. hedis-measures-reference.md
42. data-standards-and-formats.md
43. security-and-privacy-policies.md
44. quick-reference-guides.md
45. accessibility-guide.md

**Subtotal**: 50 documents

---

#### Sales Portal (40 docs)

**01-sales-enablement/** (4)
1. sales-process-playbook.md
2. product-positioning-and-messaging.md
3. objection-handling-guide.md
4. competitive-analysis.md

**02-segments-and-usecases/segments/** (6)
5. healthcare-systems-sales-kit.md
6. ambulatory-networks-sales-kit.md
7. specialty-care-sales-kit.md
8. risk-based-organizations-sales-kit.md
9. small-practices-sales-kit.md
10. accountable-care-organizations-sales-kit.md

**02-segments-and-usecases/use-cases/** (6)
11. quality-measure-improvement.md
12. care-gap-management.md
13. risk-stratification.md
14. population-health-management.md
15. mental-health-screening.md
16. medication-adherence.md

**03-sales-tools/** (8)
17. demo-script-library.md
18. email-template-library.md
19. one-pager-templates.md
20. roi-calculator-guide.md
21. proposal-templates.md
22. pricing-guide.md
23. presentation-deck-library.md
24. discovery-question-framework.md

**04-case-studies/** (4)
25. case-study-clinical-outcomes.md
26. case-study-financial-impact.md
27. case-study-implementation-success.md
28. case-study-customer-testimonials.md

**05-supporting/** (6)
29. sales-training-manual.md
30. partner-sales-playbook.md
31. objection-response-library.md
32. sales-resources-and-tools.md
33. sales-faq.md
34. sales-content-index.md

**Subtotal**: 40 documents

---

**GRAND TOTAL**: 115 documents across 3 portals

---

## Implementation Checklist

### Phase 1: Directory Creation

```bash
# Create base structure
mkdir -p /docs/product/{01-product-overview,02-architecture,03-implementation,04-case-studies,05-supporting}
mkdir -p /docs/users/{01-getting-started,02-role-specific-guides/{physician,care-manager,medical-assistant,administrator},03-feature-guides,04-troubleshooting,05-reference}
mkdir -p /docs/sales/{01-sales-enablement,02-segments-and-usecases/{segments,use-cases},03-sales-tools,04-case-studies,05-supporting}
```

### Phase 2: File Template Creation

For each of the 115 documents:

1. ✅ Create empty .md file with correct name
2. ✅ Add front matter (metadata header)
3. ✅ Add document template structure
4. ✅ Mark as "Draft" status
5. ✅ Assign owner

### Phase 3: Validation

**Directory Structure Validation**:
- [ ] All 3 top-level portals exist
- [ ] All numbered subdirectories exist (01-, 02-, etc.)
- [ ] All nested subdirectories exist (physician/, segments/, etc.)
- [ ] No extra or missing directories

**File Validation**:
- [ ] All 115 files exist
- [ ] All filenames follow naming conventions
- [ ] All files have .md extension
- [ ] No duplicate filenames across portals
- [ ] No files outside designated structure

**Naming Convention Validation**:
- [ ] All directories lowercase with hyphens
- [ ] All files lowercase with hyphens
- [ ] All directories have 2-digit prefixes where applicable
- [ ] No spaces, underscores, or special characters

### Phase 4: Version Control

```bash
# Initialize git tracking
cd /docs
git add .
git commit -m "Initialize documentation portal directory structure"
git tag -a "v1.0-structure" -m "Initial directory structure for 3 portals (115 docs)"
```

---

## Appendix A: Directory Structure ASCII Tree

```
docs/
├── product/ (25)
│   ├── 01-product-overview/ (4)
│   ├── 02-architecture/ (6)
│   ├── 03-implementation/ (4)
│   ├── 04-case-studies/ (3)
│   └── 05-supporting/ (6)
├── users/ (50)
│   ├── 01-getting-started/ (3)
│   ├── 02-role-specific-guides/ (23)
│   │   ├── physician/ (7)
│   │   ├── care-manager/ (6)
│   │   ├── medical-assistant/ (4)
│   │   └── administrator/ (6)
│   ├── 03-feature-guides/ (8)
│   ├── 04-troubleshooting/ (4)
│   └── 05-reference/ (7)
└── sales/ (40)
    ├── 01-sales-enablement/ (4)
    ├── 02-segments-and-usecases/ (12)
    │   ├── segments/ (6)
    │   └── use-cases/ (6)
    ├── 03-sales-tools/ (8)
    ├── 04-case-studies/ (4)
    └── 05-supporting/ (6)

Total: 115 documents
```

---

## Appendix B: Migration Mapping

### Existing Files → New Structure

Many existing markdown files in the project root should be consolidated into this structure:

**Example Migrations**:
- `PRODUCTION_DEPLOYMENT_GUIDE_V2.md` → `/docs/product/03-implementation/deployment-options.md`
- `CLINICAL_USER_GUIDE.md` → `/docs/users/02-role-specific-guides/physician/physician-dashboard.md`
- `SALES_DEMO_SCRIPT.md` → `/docs/sales/03-sales-tools/demo-script-library.md`
- `CASE_STUDY_CLINICAL_IMPACT.md` → `/docs/product/04-case-studies/healthcare-system-case-study.md`

**Migration Process**:
1. Identify relevant existing content
2. Map to new file in structure
3. Extract and consolidate content
4. Update internal links
5. Archive original file (do not delete)
6. Update metadata to track migration

---

## Document Control

**Version**: 1.0
**Status**: Final Specification
**Approved By**: Agent 1 - Documentation Architect
**Date**: December 1, 2025

**Change Log**:
| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-12-01 | Initial specification |

---

**Next Steps**:
1. Review and approve directory structure
2. Execute Phase 1: Directory creation
3. Execute Phase 2: Template file creation
4. Hand off to Agent 5 for portal implementation
5. Hand off to Agents 2-4 for content writing
