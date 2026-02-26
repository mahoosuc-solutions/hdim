# HDIM Product Documentation

> **HealthData-in-Motion** — Real-time healthcare quality measurement, care gap management, and clinical decision support.

This folder contains the complete product documentation suite for technical and business audiences evaluating HDIM.

---

## Document Guide

| # | Document | Audience | Purpose |
|---|----------|----------|---------|
| 01 | [Executive Overview](01-executive-overview.md) | Executives, VPs, partners | What HDIM does, who it serves, why it matters |
| 02 | [Platform Architecture](02-platform-architecture.md) | Solution architects, CTOs | System design, service topology, data flows |
| 03 | [Integration Guide](03-integration-guide.md) | Integration engineers, partners | APIs, FHIR endpoints, CDS Hooks, data requirements |
| 04 | [Use Cases & ROI](04-use-cases-and-roi.md) | CFOs, VPs of Quality, business leaders | Real-world scenarios, financial models, workflow improvements |
| 05 | [Security & Compliance](05-security-and-compliance.md) | CISOs, compliance officers, auditors | HIPAA controls, RBAC, encryption, audit infrastructure |
| 06 | [Technical Specifications](06-technical-specifications.md) | Engineering teams, technical evaluators | Service inventory, databases, Kafka topics, FHIR conformance |
| 07 | [FAQ & Positioning](07-faq-and-positioning.md) | Sales, partners, evaluators | Common questions, competitive comparison, objection handling |

---

## Reading Paths

**If you're a CTO or VP Engineering:**
Start with [01 Executive Overview](01-executive-overview.md) → [02 Platform Architecture](02-platform-architecture.md) → [03 Integration Guide](03-integration-guide.md)

**If you're a CFO or VP Quality:**
Start with [01 Executive Overview](01-executive-overview.md) → [04 Use Cases & ROI](04-use-cases-and-roi.md)

**If you're a CISO or Compliance Officer:**
Start with [05 Security & Compliance](05-security-and-compliance.md) → [06 Technical Specifications](06-technical-specifications.md)

**If you're a Solution Architect doing due diligence:**
Start with [02 Platform Architecture](02-platform-architecture.md) → [06 Technical Specifications](06-technical-specifications.md) → [03 Integration Guide](03-integration-guide.md)

**If you're evaluating HDIM against alternatives:**
Start with [07 FAQ & Positioning](07-faq-and-positioning.md) → [04 Use Cases & ROI](04-use-cases-and-roi.md)

---

## Platform at a Glance

| Dimension | Detail |
|-----------|--------|
| **Core function** | HEDIS quality measure evaluation (CQL-based), care gap detection, clinical decision support |
| **Data standard** | FHIR R4 native (HAPI FHIR 7.x) |
| **Architecture** | 58 microservices, event-sourced (CQRS), multi-tenant |
| **HEDIS measures** | 10 measures (6 with full calculators: CDC, BCS, CCS, COL, CBP, SPC) |
| **Evaluation speed** | <2 seconds per patient |
| **Deployment** | SaaS, private cloud, on-premises, or OEM/white-label |
| **Compliance** | HIPAA built-in (PHI caching, audit logging, session timeout, tenant isolation) |

---

*HealthData-in-Motion | https://healthdatainmotion.com | February 2026*
