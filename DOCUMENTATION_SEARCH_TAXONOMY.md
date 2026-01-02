# Documentation Search & Navigation Taxonomy

**Document Version**: 1.0
**Date**: December 1, 2025
**Owner**: Agent 1 - Documentation Architect
**Status**: Final Specification

---

## Purpose

This document defines the **complete search and navigation taxonomy** for the documentation portals. It includes:
- Primary keywords and search terms (600+ terms)
- Synonym mapping for improved search results
- Search result ranking algorithm
- Faceted search specifications
- Navigation hierarchy
- Autocomplete and suggestion logic

---

## Table of Contents

1. [Primary Keywords by Portal](#primary-keywords-by-portal)
2. [Synonym Mapping](#synonym-mapping)
3. [Search Algorithm](#search-algorithm)
4. [Faceted Search Specification](#faceted-search-specification)
5. [Autocomplete & Suggestions](#autocomplete--suggestions)
6. [Navigation Taxonomy](#navigation-taxonomy)

---

## Primary Keywords by Portal

### Product Portal Keywords (200+ terms)

#### Architecture & Technology (50 terms)
- architecture
- microservices
- modular-monolith
- spring-boot
- java
- postgresql
- database
- api
- rest-api
- integration
- fhir
- fhir-r4
- hl7
- hl7-v2
- interoperability
- scalability
- cloud-native
- kubernetes
- docker
- containers
- load-balancing
- caching
- redis
- messaging
- kafka
- event-driven
- asynchronous
- performance
- latency
- throughput
- high-availability
- disaster-recovery
- backup
- failover
- redundancy
- monitoring
- observability
- metrics
- logging
- alerting
- tracing
- distributed-systems
- service-mesh
- gateway
- proxy
- authentication
- authorization
- oauth
- jwt
- security

#### Clinical & Healthcare (60 terms)
- quality-measures
- hedis
- hedis-measures
- cqm
- ecqm
- clinical-quality
- value-based-care
- population-health
- care-gaps
- care-gap-closure
- quality-improvement
- cms
- cms-measures
- ncqa
- joint-commission
- quality-reporting
- measure-calculation
- cql
- clinical-quality-language
- patient-outcomes
- clinical-effectiveness
- evidence-based
- risk-stratification
- risk-assessment
- chronic-disease
- diabetes-care
- hypertension
- behavioral-health
- mental-health
- depression-screening
- phq9
- preventive-care
- immunization
- cancer-screening
- mammography
- colonoscopy
- health-maintenance
- wellness
- patient-engagement
- care-coordination
- care-management
- case-management
- transitions-of-care
- hospital-readmission
- emergency-department
- medication-adherence
- medication-reconciliation
- polypharmacy
- social-determinants
- sdoh
- health-equity
- disparities
- patient-safety
- adverse-events
- clinical-decision-support
- alerts
- reminders
- documentation
- clinical-notes
- coding

#### Business & ROI (40 terms)
- roi
- return-on-investment
- cost-savings
- revenue-optimization
- financial-impact
- value-proposition
- business-case
- total-cost-ownership
- tco
- pricing
- licensing
- subscription
- saas
- implementation-cost
- operational-cost
- cost-benefit
- payback-period
- break-even
- efficiency-gains
- productivity
- time-savings
- automation
- workflow-optimization
- process-improvement
- quality-bonus
- incentive-payments
- star-ratings
- mips
- macra
- quality-payment-program
- accountable-care
- aco
- risk-based-contracts
- shared-savings
- penalty-avoidance
- compliance
- regulatory
- audit
- certification

#### Deployment & Implementation (50 terms)
- deployment
- implementation
- installation
- configuration
- setup
- onboarding
- migration
- data-migration
- go-live
- rollout
- cloud-deployment
- on-premise
- hybrid
- aws
- azure
- gcp
- google-cloud
- kubernetes-cluster
- infrastructure
- networking
- firewall
- vpn
- ssl-certificate
- tls
- encryption
- security-hardening
- ci-cd
- continuous-integration
- continuous-deployment
- devops
- infrastructure-as-code
- terraform
- ansible
- helm
- docker-compose
- environment-setup
- development
- staging
- production
- testing
- qa
- uat
- user-acceptance-testing
- performance-testing
- load-testing
- penetration-testing
- security-audit
- compliance-audit
- hipaa-compliance
- requirements

---

### User Portal Keywords (250+ terms)

#### General Navigation (30 terms)
- dashboard
- home
- menu
- navigation
- search
- filter
- sort
- export
- print
- download
- refresh
- settings
- preferences
- profile
- account
- logout
- help
- support
- tutorial
- guide
- documentation
- faq
- troubleshooting
- error
- issue
- problem
- fix
- solution
- contact

#### Patient Management (40 terms)
- patient
- patient-list
- patient-search
- patient-details
- patient-profile
- patient-record
- demographics
- medical-history
- problem-list
- diagnosis
- conditions
- medications
- allergies
- procedures
- lab-results
- vitals
- observations
- encounters
- appointments
- visit-history
- provider
- primary-care-physician
- pcp
- specialist
- referral
- care-team
- family-history
- social-history
- insurance
- coverage
- eligibility
- attribution
- panel
- patient-population
- cohort
- registry
- enrollment
- discharge
- transfer

#### Care Gap Management (50 terms)
- care-gap
- care-gap-closure
- gap-identification
- gap-assignment
- gap-prioritization
- gap-status
- open-gap
- closed-gap
- suppressed-gap
- exclusion
- numerator
- denominator
- measure-compliance
- gap-closure-workflow
- intervention
- outreach
- patient-contact
- phone-call
- letter
- email
- text-message
- reminder
- follow-up
- documentation
- evidence
- attestation
- override
- gap-reason
- barrier
- patient-refusal
- medical-contraindication
- duplicate-service
- lab-order
- referral-order
- appointment-scheduling
- due-date
- overdue
- approaching-due
- recently-closed
- closure-date
- closing-provider
- gap-owner
- responsibility
- task
- assignment
- workflow
- action-plan
- tracking
- reporting

#### Quality Measures (60 terms)
- quality-measure
- measure-evaluation
- measure-calculation
- measure-result
- measure-definition
- measure-logic
- inclusion-criteria
- exclusion-criteria
- eligible-population
- initial-population
- numerator-compliance
- denominator-exclusion
- performance-rate
- benchmark
- target
- goal
- threshold
- hedis-cdc
- hedis-cbp
- hedis-bcs
- hedis-col
- hedis-fuh
- hedis-adv
- hedis-dmr
- diabetes-care
- blood-pressure-control
- breast-cancer-screening
- colorectal-cancer-screening
- follow-up-hospitalization
- antidepressant-medication
- diabetes-monitoring
- measure-steward
- cms-measure
- mips-measure
- quality-indicator
- process-measure
- outcome-measure
- composite-measure
- clinical-measure
- administrative-claim
- medical-record
- hybrid-measure
- ecqm-reporting
- cqm-reporting
- attestation-reporting
- measure-year
- reporting-period
- measurement-period
- lookback-period
- gap-in-care
- continuous-enrollment
- data-completeness
- data-quality
- stratification
- age-stratification
- gender-stratification
- race-ethnicity

#### Reporting & Analytics (40 terms)
- report
- reporting
- analytics
- dashboard-metrics
- kpi
- key-performance-indicator
- trend
- trend-analysis
- historical-data
- time-series
- comparison
- peer-comparison
- benchmark-comparison
- drill-down
- detail-report
- summary-report
- executive-report
- operational-report
- population-report
- patient-report
- provider-report
- measure-report
- compliance-report
- gap-report
- visualization
- chart
- graph
- table
- bar-chart
- line-chart
- pie-chart
- heat-map
- scorecard
- export-excel
- export-pdf
- export-csv
- scheduled-report
- automated-report
- custom-report
- report-builder

#### System Administration (50 terms)
- administration
- admin
- system-settings
- user-management
- user-account
- add-user
- edit-user
- deactivate-user
- user-role
- permissions
- access-control
- role-assignment
- physician-role
- care-manager-role
- medical-assistant-role
- administrator-role
- data-import
- data-export
- file-upload
- bulk-import
- csv-import
- fhir-import
- integration-setup
- ehr-integration
- api-configuration
- endpoint-configuration
- authentication-setup
- single-sign-on
- sso
- saml
- ldap
- active-directory
- password-policy
- password-reset
- session-timeout
- audit-log
- activity-log
- system-log
- error-log
- monitoring
- system-health
- performance-metrics
- database-maintenance
- backup-restore
- data-retention
- archiving
- purging
- tenant-configuration
- organization-settings
- branding
- customization

#### Troubleshooting & Support (40 terms)
- error-message
- error-code
- system-error
- 400-error
- 401-error
- 403-error
- 404-error
- 500-error
- timeout
- connection-error
- authentication-failed
- permission-denied
- data-not-found
- loading-issue
- slow-performance
- page-not-loading
- blank-screen
- frozen-screen
- refresh-issue
- browser-compatibility
- cache-clearing
- cookie-clearing
- logout-issue
- login-problem
- password-forgotten
- account-locked
- session-expired
- data-sync-issue
- integration-error
- import-failed
- export-failed
- missing-data
- incorrect-data
- calculation-error
- known-issue
- bug
- workaround
- fix
- solution
- support-ticket

---

### Sales Portal Keywords (150+ terms)

#### Sales Process (40 terms)
- sales-process
- sales-methodology
- prospecting
- lead-generation
- qualification
- bant
- discovery
- needs-assessment
- demo
- demonstration
- product-presentation
- proof-of-concept
- poc
- pilot
- trial
- proposal
- rfp
- request-for-proposal
- statement-of-work
- sow
- negotiation
- pricing-discussion
- contract
- terms-conditions
- closing
- deal-closure
- win
- loss
- win-loss-analysis
- sales-cycle
- pipeline
- forecast
- quota
- territory
- account-planning
- relationship-building
- stakeholder-management
- champion
- decision-maker
- influencer

#### Competitive Intelligence (30 terms)
- competitor
- competitive-analysis
- market-position
- differentiation
- unique-value
- competitive-advantage
- battle-card
- comparison
- head-to-head
- feature-comparison
- pricing-comparison
- win-rate
- displacement
- incumbent
- alternative-solution
- market-leader
- market-share
- strengths-weaknesses
- threats
- opportunities
- positioning
- messaging
- value-statement
- elevator-pitch
- talk-track
- objection-handling
- competitive-response
- proof-point
- case-study-competitive

#### Customer Segments (40 terms)
- healthcare-system
- hospital-system
- health-network
- integrated-delivery-network
- idn
- ambulatory
- ambulatory-care
- outpatient
- clinic
- medical-group
- physician-practice
- independent-practice
- fqhc
- federally-qualified-health-center
- community-health-center
- rural-health
- critical-access-hospital
- specialty-care
- behavioral-health
- mental-health-organization
- substance-abuse
- addiction-treatment
- skilled-nursing
- long-term-care
- home-health
- hospice
- palliative-care
- accountable-care-organization
- aco
- managed-care-organization
- mco
- health-plan
- payer
- medicare-advantage
- medicaid
- commercial-insurance
- risk-bearing-entity
- value-based-organization
- capitated-arrangement

#### Use Cases (40 terms)
- quality-improvement-use-case
- care-gap-use-case
- risk-stratification-use-case
- population-health-use-case
- chronic-disease-management
- preventive-care-optimization
- readmission-reduction
- ed-utilization-reduction
- medication-management
- care-coordination-use-case
- transitions-care-use-case
- mental-health-screening-use-case
- social-determinants-use-case
- health-equity-use-case
- patient-engagement-use-case
- provider-efficiency
- workflow-optimization-use-case
- documentation-improvement
- coding-accuracy
- revenue-optimization-use-case
- compliance-reporting-use-case
- quality-reporting-use-case
- star-ratings-improvement
- mips-optimization
- hedis-improvement
- value-based-contracting
- shared-savings-use-case
- quality-bonus-use-case
- penalty-avoidance-use-case
- network-performance
- provider-attribution
- panel-management
- outreach-campaign
- patient-registry
- disease-registry
- clinical-decision-support-use-case
- point-of-care-alerts
- proactive-outreach
- patient-prioritization
- resource-allocation

---

## Synonym Mapping

### Clinical Terms

| Primary Term | Synonyms |
|--------------|----------|
| care-gap | gap, gap-in-care, quality-gap, measure-gap, compliance-gap |
| quality-measure | measure, clinical-measure, performance-measure, quality-metric, hedis-measure |
| patient | member, individual, person, beneficiary |
| provider | physician, doctor, clinician, practitioner, prescriber |
| dashboard | home, overview, summary, main-screen |
| ehr | emr, electronic-health-record, electronic-medical-record, health-record-system |
| fhir | fast-healthcare-interoperability-resources, hl7-fhir |
| cql | clinical-quality-language, measure-logic, expression-language |
| hedis | healthcare-effectiveness-data-information-set, ncqa-measures |
| cms | centers-medicare-medicaid-services, medicare, medicaid |
| population-health | pop-health, population-management, cohort-management |
| risk-stratification | risk-scoring, risk-assessment, patient-prioritization |
| care-management | case-management, care-coordination, patient-management |
| chronic-disease | chronic-condition, long-term-condition, chronic-illness |
| preventive-care | prevention, wellness, health-maintenance, screening |
| medication | drug, prescription, rx, pharmaceutical |
| diagnosis | condition, problem, disease, disorder, icd-10 |
| procedure | treatment, intervention, service, cpt-code |
| lab-result | lab, laboratory, test-result, diagnostic-test |
| vital-sign | vitals, vital-measurement, clinical-observation |

### Technical Terms

| Primary Term | Synonyms |
|--------------|----------|
| api | application-programming-interface, web-service, endpoint, rest-api, restful-api |
| integration | interface, connection, interoperability, data-exchange |
| authentication | auth, login, sign-in, credentials, identity-verification |
| authorization | access-control, permissions, privileges, entitlements |
| database | db, data-store, persistence, postgresql, postgres |
| deployment | install, installation, setup, implementation, rollout |
| configuration | config, settings, setup, customization |
| error | bug, issue, problem, exception, fault |
| performance | speed, latency, response-time, throughput |
| scalability | scaling, scale-out, horizontal-scaling, capacity |
| backup | restore, recovery, disaster-recovery, business-continuity |
| monitoring | observability, logging, metrics, alerting, tracking |
| security | infosec, information-security, cybersecurity, data-protection |
| encryption | crypto, cryptography, data-encryption, tls, ssl |

### Business Terms

| Primary Term | Synonyms |
|--------------|----------|
| roi | return-on-investment, financial-return, payback |
| implementation | deployment, rollout, go-live, launch |
| pricing | cost, price, pricing-model, subscription, license-cost |
| value-proposition | value, business-value, benefits, advantages |
| case-study | customer-story, success-story, reference, testimonial |
| demo | demonstration, product-demo, walkthrough, showcase |
| proposal | quote, rfp-response, statement-of-work, sow |
| contract | agreement, terms, license-agreement, subscription-agreement |

### Role-Based Terms

| Primary Term | Synonyms |
|--------------|----------|
| physician | doctor, provider, clinician, md, do, pcp, primary-care-physician |
| care-manager | case-manager, care-coordinator, rn, registered-nurse |
| medical-assistant | ma, clinical-assistant, healthcare-assistant |
| administrator | admin, system-admin, super-user, it-admin |
| sales-rep | account-executive, ae, sales-representative, salesperson |
| sales-engineer | se, solutions-engineer, technical-sales, presales |

---

## Search Algorithm

### Ranking Algorithm

**Score Calculation**:
```javascript
documentScore =
  (titleMatch * 10.0) +
  (summaryMatch * 5.0) +
  (tagMatch * 7.0) +
  (contentMatch * 2.0) +
  (metadataMatch * 3.0) +
  (recencyBoost) +
  (popularityBoost) +
  (authorityBoost)
```

### Scoring Components

#### 1. Title Match (Weight: 10.0)
- **Exact match**: 10.0 points
- **Partial match**: 5.0 points
- **Fuzzy match**: 2.0 points
- **Synonym match**: 7.0 points

#### 2. Summary Match (Weight: 5.0)
- **Exact phrase**: 5.0 points
- **All terms present**: 3.0 points
- **Some terms present**: 1.0 points
- **Synonym match**: 3.5 points

#### 3. Tag Match (Weight: 7.0)
- **Exact tag match**: 7.0 points
- **Partial tag match**: 3.5 points
- **Synonym tag match**: 5.0 points

#### 4. Content Match (Weight: 2.0)
- **High frequency**: 2.0 points
- **Medium frequency**: 1.0 points
- **Low frequency**: 0.5 points

#### 5. Metadata Match (Weight: 3.0)
- **Category match**: 3.0 points
- **Audience match**: 2.0 points
- **Difficulty match**: 1.0 points

#### 6. Recency Boost
```javascript
daysSinceUpdate = (today - lastUpdated) / (1000 * 60 * 60 * 24);

if (daysSinceUpdate < 30) {
  recencyBoost = 2.0;
} else if (daysSinceUpdate < 90) {
  recencyBoost = 1.0;
} else if (daysSinceUpdate < 180) {
  recencyBoost = 0.5;
} else {
  recencyBoost = 0;
}
```

#### 7. Popularity Boost
```javascript
if (viewCount > 500) {
  popularityBoost = 2.0;
} else if (viewCount > 100) {
  popularityBoost = 1.0;
} else if (viewCount > 50) {
  popularityBoost = 0.5;
} else {
  popularityBoost = 0;
}
```

#### 8. Authority Boost
```javascript
if (avgRating >= 4.5) {
  authorityBoost = 2.0;
} else if (avgRating >= 4.0) {
  authorityBoost = 1.0;
} else if (avgRating >= 3.5) {
  authorityBoost = 0.5;
} else {
  authorityBoost = 0;
}
```

---

### Search Query Processing

#### 1. Query Normalization
```javascript
function normalizeQuery(query) {
  // Convert to lowercase
  query = query.toLowerCase();

  // Remove special characters (except hyphens)
  query = query.replace(/[^a-z0-9\s\-]/g, '');

  // Trim whitespace
  query = query.trim();

  // Expand acronyms and synonyms
  query = expandSynonyms(query);

  return query;
}
```

#### 2. Synonym Expansion
```javascript
function expandSynonyms(query) {
  const words = query.split(' ');
  const expanded = [];

  words.forEach(word => {
    expanded.push(word);

    // Add synonyms
    if (synonymMap[word]) {
      expanded.push(...synonymMap[word]);
    }
  });

  return [...new Set(expanded)].join(' ');
}
```

#### 3. Fuzzy Matching
- Enable fuzzy matching for queries > 4 characters
- Levenshtein distance ≤ 2
- Examples:
  - "hedis" matches "HEDIS", "Hedis"
  - "quality" matches "qualty" (typo)
  - "dashbord" matches "dashboard" (typo)

---

## Faceted Search Specification

### Available Facets

#### 1. Portal Type
```javascript
{
  facet: 'portalType',
  label: 'Documentation Type',
  values: [
    { value: 'product', label: 'Product Documentation', count: 25 },
    { value: 'user', label: 'User Documentation', count: 50 },
    { value: 'sales', label: 'Sales Documentation', count: 40 }
  ]
}
```

#### 2. Category
```javascript
{
  facet: 'category',
  label: 'Category',
  values: [
    // Populated dynamically based on portal
    { value: 'product-overview', label: 'Product Overview', count: 4 },
    { value: 'architecture', label: 'Architecture', count: 6 },
    // ...
  ]
}
```

#### 3. Difficulty
```javascript
{
  facet: 'difficulty',
  label: 'Difficulty Level',
  values: [
    { value: 'beginner', label: 'Beginner', count: 45 },
    { value: 'intermediate', label: 'Intermediate', count: 50 },
    { value: 'advanced', label: 'Advanced', count: 20 }
  ]
}
```

#### 4. Target Audience
```javascript
{
  facet: 'targetAudience',
  label: 'For',
  values: [
    { value: 'physician', label: 'Physicians', count: 15 },
    { value: 'care-manager', label: 'Care Managers', count: 12 },
    { value: 'administrator', label: 'Administrators', count: 18 },
    { value: 'executive', label: 'Executives', count: 10 },
    { value: 'sales-rep', label: 'Sales Reps', count: 25 }
    // ...
  ]
}
```

#### 5. Last Updated
```javascript
{
  facet: 'lastUpdated',
  label: 'Last Updated',
  values: [
    { value: 'last-30-days', label: 'Last 30 days', count: 15 },
    { value: 'last-90-days', label: 'Last 90 days', count: 40 },
    { value: 'last-year', label: 'Last year', count: 60 }
  ]
}
```

#### 6. Has Video
```javascript
{
  facet: 'hasVideo',
  label: 'Content Type',
  values: [
    { value: true, label: 'With Video', count: 20 },
    { value: false, label: 'Documentation Only', count: 95 }
  ]
}
```

### Facet Interaction

**Multi-Select**: Users can select multiple values within same facet (OR logic)

**Cross-Facet**: Selections across facets use AND logic

**Example**:
- Portal Type: "user" (selected)
- Target Audience: "physician" OR "care-manager" (both selected)
- Difficulty: "beginner" (selected)

**Result**: User docs for (physicians OR care managers) AND beginner level

---

## Autocomplete & Suggestions

### Autocomplete Behavior

**Trigger**: After 3 characters typed

**Response Time**: < 100ms

**Max Suggestions**: 10

**Sources** (in priority order):
1. Document titles (exact match)
2. Popular searches
3. Tags (exact match)
4. Common terms from content

### Suggestion Algorithm

```javascript
function getAutocomplete(query, portal) {
  const normalized = normalizeQuery(query);
  const suggestions = [];

  // 1. Exact title matches (up to 3)
  const titleMatches = documents
    .filter(d => d.title.toLowerCase().includes(normalized))
    .sort((a, b) => b.viewCount - a.viewCount)
    .slice(0, 3)
    .map(d => ({
      type: 'document',
      text: d.title,
      highlight: highlightMatch(d.title, query),
      url: d.path
    }));
  suggestions.push(...titleMatches);

  // 2. Popular searches (up to 3)
  const popularSearches = getPopularSearches(portal)
    .filter(s => s.toLowerCase().includes(normalized))
    .slice(0, 3)
    .map(s => ({
      type: 'search',
      text: s,
      highlight: highlightMatch(s, query)
    }));
  suggestions.push(...popularSearches);

  // 3. Tag matches (up to 4)
  const tagMatches = getAllTags(portal)
    .filter(t => t.toLowerCase().includes(normalized))
    .slice(0, 4)
    .map(t => ({
      type: 'tag',
      text: t,
      highlight: highlightMatch(t, query)
    }));
  suggestions.push(...tagMatches);

  return suggestions.slice(0, 10);
}
```

### "Did You Mean?" Suggestions

**Trigger**: When search returns < 3 results

**Algorithm**: Levenshtein distance for query correction

**Example**:
- Query: "hedis mesures"
- Suggestion: "Did you mean: hedis measures?"

---

## Navigation Taxonomy

### Product Portal Navigation

```
Product Documentation
├── Product Overview
│   ├── Vision & Strategy
│   ├── Core Capabilities
│   ├── Value Proposition
│   └── Competitive Differentiation
├── Architecture
│   ├── System Architecture
│   ├── Integration Patterns
│   ├── Data Model
│   ├── Security Architecture
│   ├── Performance Benchmarks
│   └── Disaster Recovery
├── Implementation
│   ├── Deployment Options
│   ├── Requirements & Prerequisites
│   ├── Implementation Roadmap
│   └── Configuration Guide
├── Case Studies
│   ├── Healthcare System
│   ├── Ambulatory Network
│   └── Risk-Based Organization
└── Supporting
    ├── FHIR Integration Guide
    ├── Pricing & Licensing
    ├── Security Audit Summary
    ├── Licensing Options
    ├── Performance Testing Results
    └── Compliance Certifications
```

### User Portal Navigation

```
User Documentation
├── Getting Started
│   ├── New User Orientation
│   ├── User Roles & Permissions
│   └── First Day Checklist
├── For Physicians
│   ├── Dashboard
│   ├── Patient Search & Review
│   ├── Care Gap Identification
│   ├── Care Gap Closure
│   ├── Quality Measure Interpretation
│   ├── Clinical Alerts
│   └── FAQ for Physicians
├── For Care Managers
│   ├── Dashboard
│   ├── Gap Assignment & Prioritization
│   ├── Patient Outreach Workflows
│   ├── Risk Stratification Guide
│   ├── Outcome Documentation
│   └── FAQ for Care Managers
├── For Medical Assistants
│   ├── Workflows
│   ├── Data Entry Guide
│   ├── Patient Communication
│   └── FAQ for Medical Assistants
├── For Administrators
│   ├── System Configuration
│   ├── User Management
│   ├── Data Import & Management
│   ├── Integration Setup
│   ├── Reporting & Analytics
│   └── FAQ for Administrators
├── Feature Guides
│   ├── Dashboard Navigation
│   ├── Patient Search Best Practices
│   ├── Care Gap Management Workflows
│   ├── Evaluations & Reporting
│   ├── Quality Measures Evaluation
│   ├── Batch Operations
│   ├── Data Export Guide
│   └── Alert Management
├── Troubleshooting
│   ├── Common Issues & Solutions
│   ├── Error Codes Reference
│   ├── General FAQ
│   └── Accessibility Troubleshooting
└── Reference
    ├── Terminology Glossary
    ├── Keyboard Shortcuts
    ├── HEDIS Measures Reference
    ├── Data Standards & Formats
    ├── Security & Privacy Policies
    ├── Quick Reference Guides
    └── Accessibility Guide
```

### Sales Portal Navigation

```
Sales Documentation
├── Sales Enablement
│   ├── Sales Process Playbook
│   ├── Product Positioning & Messaging
│   ├── Objection Handling Guide
│   └── Competitive Analysis
├── Customer Segments
│   ├── Healthcare Systems Sales Kit
│   ├── Ambulatory Networks Sales Kit
│   ├── Specialty Care Sales Kit
│   ├── Risk-Based Organizations Sales Kit
│   ├── Small Practices Sales Kit
│   └── Accountable Care Organizations Sales Kit
├── Use Cases
│   ├── Quality Measure Improvement
│   ├── Care Gap Management
│   ├── Risk Stratification
│   ├── Population Health Management
│   ├── Mental Health Screening
│   └── Medication Adherence
├── Sales Tools
│   ├── Demo Script Library
│   ├── Email Template Library
│   ├── One-Pager Templates
│   ├── ROI Calculator Guide
│   ├── Proposal Templates
│   ├── Pricing Guide
│   ├── Presentation Deck Library
│   └── Discovery Question Framework
├── Case Studies
│   ├── Clinical Outcomes
│   ├── Financial Impact
│   ├── Implementation Success
│   └── Customer Testimonials
└── Supporting
    ├── Sales Training Manual
    ├── Partner Sales Playbook
    ├── Objection Response Library
    ├── Sales Resources & Tools
    ├── Sales FAQ
    └── Sales Content Index
```

---

### Breadcrumb Structure

**Format**: `Portal > Category > Subcategory > Document`

**Examples**:
- `Product Documentation > Architecture > System Architecture`
- `User Documentation > For Physicians > Care Gap Closure`
- `Sales Documentation > Sales Tools > Demo Script Library`

**Implementation**:
```javascript
function generateBreadcrumbs(document) {
  const crumbs = [];

  // Portal
  crumbs.push({
    label: getPortalLabel(document.portalType),
    url: `/${document.portalType}`
  });

  // Category
  crumbs.push({
    label: getCategoryLabel(document.category),
    url: `/${document.portalType}/${document.category}`
  });

  // Subcategory (if exists)
  if (document.subcategory) {
    crumbs.push({
      label: getSubcategoryLabel(document.subcategory),
      url: `/${document.portalType}/${document.category}/${document.subcategory}`
    });
  }

  // Current document
  crumbs.push({
    label: document.title,
    url: `/${document.path}`,
    current: true
  });

  return crumbs;
}
```

---

## Search Analytics

### Metrics to Track

1. **Search Volume**: Number of searches per day/week/month
2. **Query Distribution**: Most common search terms
3. **Zero-Result Queries**: Searches that return no results
4. **Click-Through Rate**: % of searches resulting in document click
5. **Average Results per Search**: Typical number of results
6. **Search Refinement Rate**: % of users who refine search
7. **Facet Usage**: Which facets are used most
8. **Autocomplete Usage**: % of searches using autocomplete

### Search Quality Metrics

**Good Search Quality Indicators**:
- CTR > 60%
- Zero-result rate < 10%
- Average time to click < 15 seconds
- Refinement rate < 30%

**Poor Search Quality Indicators**:
- CTR < 30%
- Zero-result rate > 20%
- High refinement rate > 50%
- High bounce rate after click

---

## Implementation Notes

### Technology Recommendations

**Search Engine**: Elasticsearch or PostgreSQL Full-Text Search

**Autocomplete**: Redis-backed cache for performance

**Analytics**: Custom tracking + Google Analytics

**UI Framework**: Angular Material or similar

### Performance Targets

- Search query response time: < 200ms (p95)
- Autocomplete response time: < 100ms (p95)
- Facet calculation time: < 150ms (p95)
- Index rebuild time: < 5 minutes
- Real-time index updates: < 1 second latency

---

## Document Control

**Version**: 1.0
**Status**: Final Specification
**Approved By**: Agent 1 - Documentation Architect
**Date**: December 1, 2025

**Change Log**:
| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-12-01 | Initial search taxonomy specification |

---

**Next Steps**:
1. Review and approve search taxonomy
2. Implement synonym mapping in search engine
3. Configure search algorithm weights
4. Build autocomplete service
5. Hand off to Agent 5 for portal implementation
