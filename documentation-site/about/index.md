# About HDIM

## What is HDIM?

**HDIM (HealthData-in-Motion)** is an enterprise healthcare quality management platform that helps healthcare organizations improve patient outcomes and maximize quality incentive revenue.

---

## Platform Capabilities

### Quality Measure Evaluation
- HEDIS measure library with CQL-based evaluation
- Real-time patient quality scoring
- Batch population evaluations
- Automated care gap detection

### Care Gap Management
- Priority-based gap tracking
- Intervention documentation
- Closure workflows
- Population health analytics

### Role-Based Dashboards
- **Providers**: Patient quality metrics at point of care
- **RNs**: Outreach coordination and care management
- **MAs**: Patient preparation and scheduling support
- **Admins**: System configuration and reporting

### Integration & Interoperability
- FHIR R4 native architecture
- EHR integration ready
- Multi-tenant SaaS model
- RESTful API for custom integrations

---

## Technical Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Clinical Portal (Angular)                 │
├─────────────────────────────────────────────────────────────┤
│                      API Gateway                             │
├─────────────┬─────────────┬─────────────┬──────────────────┤
│   Patient   │   Quality   │  Care Gap   │   CQL Engine     │
│   Service   │   Measure   │   Service   │    Service       │
├─────────────┴─────────────┴─────────────┴──────────────────┤
│                   FHIR Server (R4)                          │
├─────────────────────────────────────────────────────────────┤
│                   PostgreSQL Database                        │
└─────────────────────────────────────────────────────────────┘
```

---

## Development Story

HDIM was built using AI-assisted development with Claude Code, demonstrating a new paradigm in enterprise software development.

[Read the full case study →](./development-case-study)

---

## Key Metrics

| Metric | Value |
|--------|-------|
| Backend Services | 27 microservices |
| Lines of Code | 162,752 |
| Angular Components | 82 |
| Test Files | 534 |
| Documentation Pages | 25+ |

---

## Target Users

### Healthcare Organizations
- Accountable Care Organizations (ACOs)
- Federally Qualified Health Centers (FQHCs)
- Health Systems
- Provider Groups

### Roles Supported
- Physicians, NPs, PAs
- Registered Nurses
- Medical Assistants
- Care Coordinators
- Quality Analysts
- Administrators

---

## Learn More

- [User Stories](/user-stories/) - Feature requirements by module
- [Workflows](/workflows/) - Clinical workflow documentation
- [Role Guides](/guides/) - Role-specific usage guides
- [API Reference](/api/) - Technical API documentation
