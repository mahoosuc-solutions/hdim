# HDIM Implementation Timeline Planner

A CLI tool to calculate demo-to-production timelines based on customer profiles and integration complexity.

## Quick Start

```bash
# Run demo with example profiles
npx ts-node timeline-calculator.ts --demo small-practice

# Run interactive mode
npx ts-node interactive-planner.ts
```

## Features

- **Complexity Scoring**: Automatically assesses implementation complexity based on:
  - Organization type (solo practice through large health system)
  - EHR system and FHIR capability
  - Integration method
  - IT capability level
  - Data volume and sources

- **Phase-Based Planning**: Generates detailed timelines with:
  - Discovery & Planning
  - Environment Setup
  - Integration Configuration
  - Data Sync & Validation
  - User Training
  - Pilot (optional)
  - Go-Live
  - Post-Implementation Review

- **Risk Assessment**: Identifies potential risks and mitigation strategies

- **Actionable Recommendations**: Provides tailored guidance based on customer profile

## Usage

### Demo Mode

See example output for different customer types:

```bash
# Solo practice (CSV upload, ~1-3 days)
npx ts-node timeline-calculator.ts --demo solo-practice

# Small practice (FHIR API, ~7-14 days)
npx ts-node timeline-calculator.ts --demo small-practice

# FQHC (n8n workflow, ~14-28 days)
npx ts-node timeline-calculator.ts --demo fqhc

# Mid-size ACO (SMART on FHIR, ~21-42 days)
npx ts-node timeline-calculator.ts --demo midsize-aco

# Large health system (Private Cloud, ~42-84 days)
npx ts-node timeline-calculator.ts --demo large-health-system
```

### Interactive Mode

Run the interactive planner to build a custom profile:

```bash
npx ts-node interactive-planner.ts
```

### Programmatic Usage

```typescript
import { calculateTimeline, formatTimelineMarkdown } from './timeline-calculator';

const profile = {
  organizationType: 'small-practice',
  organizationName: 'Example Medical Group',
  ehrSystem: 'athenahealth',
  integrationMethod: 'fhir-api',
  itCapability: 'basic',
  hdimTier: 'professional',
  patientCount: 5000,
  providerCount: 6,
  siteCount: 2,
  dataSourceCount: 2,
  qualityPrograms: ['MIPS'],
  hasExistingFHIR: true,
  hasDedicatedIT: false,
  requiresSOC2: false,
  requiresBAA: true,
  pilotFirst: false,
};

const result = calculateTimeline(profile, new Date());
console.log(formatTimelineMarkdown(result));
```

## Complexity Factors

### Organization Type Weight
| Type | Complexity Score |
|------|-----------------|
| Solo Practice | 1 |
| Small Practice | 2 |
| Rural Hospital | 3 |
| FQHC | 4 |
| Small ACO | 5 |
| Mid-size ACO | 7 |
| IPA | 8 |
| Large Health System | 10 |

### EHR System Weight
| System | Complexity Score |
|--------|-----------------|
| None (CSV) | 1 |
| athenahealth | 2 |
| eClinicalWorks, NextGen, Allscripts | 3 |
| Epic, Cerner | 4 |
| MEDITECH | 5 |
| Legacy Systems | 6 |

### Integration Method Weight
| Method | Complexity Score | Typical Duration |
|--------|-----------------|------------------|
| CSV Upload | 1 | Same day |
| FHIR API | 3 | 2-5 days |
| SMART on FHIR | 4 | 3-7 days |
| n8n Workflow | 5 | 5-10 days |
| Private Cloud | 7 | 10-21 days |
| On-Premise | 10 | 21-35 days |

## Output Formats

- **Text**: Human-readable console output with ASCII formatting
- **Markdown**: Copy-paste ready for documentation
- **JSON**: Machine-readable for integration with other tools

## Example Output

```
═══════════════════════════════════════════════════════════════════════════════
HDIM IMPLEMENTATION TIMELINE
═══════════════════════════════════════════════════════════════════════════════

CUSTOMER PROFILE
────────────────────────────────────────
Organization: Riverside Primary Care
Type: small-practice
EHR System: athenahealth
Integration Method: fhir-api
HDIM Tier: professional

COMPLEXITY ASSESSMENT
────────────────────────────────────────
Score: 28/100 [█████░░░░░░░░░░░░░░░]
Level: MEDIUM

TIMELINE SUMMARY
────────────────────────────────────────
Estimated Duration: 12-21 business days
                    (~3-5 weeks)

PHASE BREAKDOWN
────────────────────────────────────────

▶ DISCOVERY & PLANNING
  Duration: 1-2 days (Day 1 → Day 1-2)
  Initial customer discovery, requirements gathering...

▶ ENVIRONMENT SETUP
  Duration: 1-2 days (Day 2 → Day 2-4)
  Provision HDIM environment, configure tenant...

... [additional phases]
```

## Integration with Sales Process

1. **During Demo**: Run quick estimate based on customer profile
2. **Post-Demo**: Generate detailed proposal with timeline markdown
3. **SOW Development**: Export JSON for integration with proposal tools
4. **Implementation Kickoff**: Use as basis for project plan

## Related Documentation

- [Customer Integration Examples](../../yc-application-v2/customer-integrations/)
- [Implementation Checklist](../../yc-application-v2/customer-integrations/_templates/IMPLEMENTATION_CHECKLIST.md)
- [EHR Integration Roadmap](../../yc-application-v2/EHR_INTEGRATION_ROADMAP.md)
- [Deployment Options](../../yc-application-v2/DEPLOYMENT_OPTIONS.md)

---

*Part of the HDIM Implementation Toolkit*
