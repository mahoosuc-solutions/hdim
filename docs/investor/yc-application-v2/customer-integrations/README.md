# HDIM Customer Integration Examples

> Real-world integration scenarios demonstrating how HDIM connects to healthcare organizations of all types and sizes.

## Overview

This directory contains modular, detailed integration examples for each customer segment. Each scenario includes:

- Customer profile and context
- Architecture diagram
- Integration configuration
- Sample data payloads
- Step-by-step implementation
- Expected outcomes and ROI

## Directory Structure

```
customer-integrations/
├── README.md                 # This file - index and overview
├── _templates/               # Reusable templates for new scenarios
│   ├── SCENARIO_TEMPLATE.md
│   └── IMPLEMENTATION_CHECKLIST.md
├── _shared/                  # Shared components referenced by scenarios
│   ├── FHIR_PAYLOADS.md      # Sample FHIR bundles
│   ├── CSV_TEMPLATES.md      # CSV format specifications
│   ├── N8N_WORKFLOWS.md      # n8n workflow examples
│   └── MEASURE_SETS.md       # Common measure sets by segment
├── 01-solo-practice/         # Solo physician practices
├── 02-small-practice/        # 2-10 physician practices
├── 03-fqhc/                  # Federally Qualified Health Centers
├── 04-rural-hospital/        # Critical Access Hospitals, rural facilities
├── 05-small-aco/             # ACOs with <15,000 attributed lives
├── 06-midsize-aco/           # ACOs with 15,000-100,000 attributed lives
├── 07-large-health-system/   # Enterprise health systems
└── 08-ipa/                   # Independent Physician Associations
```

## Quick Reference

| Scenario | Organization | Size | Integration | Tier | Monthly Cost |
|----------|--------------|------|-------------|------|--------------|
| [Solo Practice](01-solo-practice/dr-martinez-family-medicine.md) | Dr. Martinez Family Medicine | 1,200 patients | CSV Upload | Community | $49 |
| [Small Practice](02-small-practice/riverside-primary-care.md) | Riverside Primary Care | 4,500 patients | athenahealth API | Professional | $299 |
| [FQHC](03-fqhc/community-health-partners.md) | Community Health Partners | 22,000 patients | FHIR + n8n | Enterprise | $799* |
| [Rural Hospital](04-rural-hospital/mountain-view-cah.md) | Mountain View CAH | 8,000 patients | n8n Workflow | Professional | $254* |
| [Small ACO](05-small-aco/coastal-care-partners.md) | Coastal Care Partners | 8,500 lives | Multi-FHIR | Enterprise | $999 |
| [Mid-size ACO](06-midsize-aco/metro-health-alliance.md) | Metro Health Alliance | 42,000 lives | SMART on FHIR | Enterprise Plus | $2,499 |
| [Large System](07-large-health-system/regional-medical-center.md) | Regional Medical Center | 180,000 patients | Private Cloud | Health System | ~$15,000 |
| [IPA](08-ipa/valley-physicians-network.md) | Valley Physicians Network | 85 practices | n8n Hub | Enterprise Plus | $2,499 |

*Includes applicable discounts (FQHC 20%, Rural 15%)

## Integration Methods Summary

### CSV Upload (Simplest)
- **Best for:** Solo practices, small practices without API access
- **Effort:** Self-service, no IT required
- **Time to value:** Same day
- **See:** [Solo Practice Example](01-solo-practice/dr-martinez-family-medicine.md)

### FHIR API (Standard)
- **Best for:** Practices with modern EHRs (Epic, Cerner, athenahealth)
- **Effort:** OAuth authorization, one-time setup
- **Time to value:** 1-3 days
- **See:** [Small Practice Example](02-small-practice/riverside-primary-care.md)

### n8n Workflow (Flexible)
- **Best for:** Legacy systems, custom data sources, multi-system environments
- **Effort:** Custom workflow development
- **Time to value:** 3-7 days
- **See:** [Rural Hospital Example](04-rural-hospital/mountain-view-cah.md), [IPA Example](08-ipa/valley-physicians-network.md)

### Private Cloud (Enterprise)
- **Best for:** Large health systems with data residency requirements
- **Effort:** Dedicated deployment
- **Time to value:** 2-4 weeks
- **See:** [Large Health System Example](07-large-health-system/regional-medical-center.md)

## Key Differentiators

| Factor | HDIM | Traditional Vendors |
|--------|------|---------------------|
| **Implementation Time** | 1-14 days | 12-18 months |
| **Cost** | $49-$15,000/mo | $50,000-$500,000/mo |
| **Quality Evaluation** | <200ms real-time | 24-48 hour batch |
| **Integration Flexibility** | Any source (FHIR, n8n, CSV) | Limited EHR support |
| **Contract Term** | Month-to-month available | 3-5 year minimum |

## Using These Examples

### For Sales Teams
1. Identify customer segment from the table above
2. Use the corresponding scenario as a reference during demos
3. Customize ROI calculations for customer-specific volumes

### For Implementation Teams
1. Start with the [Implementation Checklist](_templates/IMPLEMENTATION_CHECKLIST.md)
2. Reference the appropriate scenario for architecture patterns
3. Use [Shared Components](_shared/) for sample payloads and workflows

### For Product Development
1. Reference scenarios when designing new features
2. Ensure new integrations follow established patterns
3. Update scenarios when integration methods change

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | December 2025 | Initial release with 8 scenarios |

---

*For questions about these integration examples, contact: integrations@healthdatainmotion.com*
