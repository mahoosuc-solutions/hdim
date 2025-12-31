# Documentation Quality Checklist

**Purpose**: Use this checklist before committing documentation to ensure consistency and quality.
**Version**: 1.0
**Last Updated**: December 30, 2025

---

## Quick Reference

Before committing any documentation, verify:

- [ ] Microservices count is **28** (not 27 or 29)
- [ ] HEDIS measures count is **56** (not 82)
- [ ] FHIR specification is **FHIR R4** (not just "FHIR")
- [ ] PHI cache TTL is **5 minutes** (not "short-term")
- [ ] All links work (no 404s)
- [ ] "Last Updated" date is current

---

## General Requirements

### Document Structure

- [ ] Document follows appropriate template (Service README, ADR, etc.)
- [ ] Table of contents present for documents > 100 lines
- [ ] Version history at bottom of document
- [ ] "Last Updated" date in header or footer
- [ ] Clear section headings (use ## and ### consistently)

### Terminology Compliance

- [ ] All terminology matches [TERMINOLOGY_GLOSSARY.md](/docs/TERMINOLOGY_GLOSSARY.md)
- [ ] Service names use official names (not abbreviations)
- [ ] Port numbers match glossary
- [ ] Technology versions correct

### Content Quality

- [ ] No placeholder content ("[TBD]", "[TODO]", "lorem ipsum")
- [ ] Technical claims include proof points or references
- [ ] No unsubstantiated buzzwords ("AI-powered", "industry-leading")
- [ ] Code examples compile/run
- [ ] Links verified (internal and external)

---

## Technical Accuracy Checks

### Microservices and Services

| Check | Correct Value | Common Mistakes |
|-------|---------------|-----------------|
| Total microservices | **28** | 27 (outdated), 29 (includes build dir) |
| HEDIS measures | **56** | 82 (outdated historical claim) |
| Java version | **21 LTS** | 17, 11 |
| Spring Boot | **3.x** | 2.x |
| HAPI FHIR | **7.x** | 6.x |
| PostgreSQL | **15** | 14, 13 |

### FHIR Specification

- [ ] Always write "FHIR R4" (not "FHIR" or "FHIR compliant")
- [ ] Reference specific resources (Patient, Observation, etc.)
- [ ] Mention HAPI FHIR 7.x when discussing implementation

### Database and Ports

| Component | Dev Port | Prod Port |
|-----------|----------|-----------|
| PostgreSQL | 5435 | 5432 |
| Redis | 6380 | 6379 |
| Kafka | 9094 | 9092 |
| Kong | 8000 | 8000 |
| Gateway Service | 8001 | 8001 |

---

## HIPAA Compliance Language

### Required Mentions

When documenting PHI-related features:

- [ ] PHI cache TTL mentioned as "5 minutes" (or "<=5 min")
- [ ] Encryption specified: "AES-256 at rest, TLS 1.3 in transit"
- [ ] Multi-tenant isolation explicitly mentioned
- [ ] Audit logging requirements documented

### Prohibited Content

- [ ] No actual PHI in examples (use synthetic data only)
- [ ] No credentials or secrets in documentation
- [ ] No internal IP addresses or hostnames
- [ ] No customer-specific data

### Example Compliance Check

**Good**:
> PHI is cached in Redis with a maximum TTL of 5 minutes, complying with HIPAA minimum necessary requirements (45 CFR 164.502).

**Bad**:
> We cache patient data for performance.

---

## Architecture Documentation Checks

### Service Documentation

- [ ] Service dependencies listed with port numbers
- [ ] API endpoints documented with auth requirements
- [ ] Database schema outlined (key tables and columns)
- [ ] Kafka topics documented (producers and consumers)
- [ ] Links to related ADRs included

### Diagram Requirements

- [ ] Include service names AND port numbers
- [ ] Show data flow direction (arrows)
- [ ] Include connection type (REST, Kafka, Redis)
- [ ] Use consistent styling (ASCII art or Mermaid)

---

## GTM/Marketing Content Checks

### Claims Verification

- [ ] ROI claims include calculation methodology
- [ ] Performance benchmarks cite source/date
- [ ] "Faster" claims quantified (e.g., "40% faster", not "much faster")
- [ ] Competitive claims are defensible

### Prohibited Language

| Avoid | Replace With |
|-------|--------------|
| "AI-powered" | Specific tech (CQL execution, HCC v28 models) |
| "Real-time" | Specific latency (<200ms, 5-minute cache) |
| "Industry-leading" | Specific benchmark with source |
| "Cutting-edge" | Specific technology or approach |
| "Best-in-class" | Comparative metrics with sources |

### Technical Differentiators

- [ ] CQL-native execution mentioned (not proprietary interpretation)
- [ ] FHIR R4 native architecture explained (no translation layer)
- [ ] Gateway Trust authentication referenced
- [ ] 5-minute PHI cache compliance highlighted

---

## Code Example Checks

### Compilation and Style

- [ ] Code examples compile/run without errors
- [ ] Code follows project conventions (see CLAUDE.md)
- [ ] Imports are complete (no missing imports)
- [ ] Error handling is appropriate

### Security in Examples

- [ ] Secrets/credentials replaced with placeholders (`${SECRET}`)
- [ ] Multi-tenant filtering shown in queries (`tenantId` WHERE clause)
- [ ] `@PreAuthorize` annotations included on endpoints
- [ ] `@Audited` annotation shown for PHI access

### Example Quality Check

**Good**:
```java
@GetMapping("/{patientId}")
@PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
@Audited(eventType = "PATIENT_ACCESS")
public ResponseEntity<Patient> getPatient(
        @PathVariable String patientId,
        @RequestHeader("X-Tenant-ID") String tenantId) {
    // Multi-tenant isolation: filter by tenantId
    return patientRepository.findByIdAndTenant(patientId, tenantId)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
}
```

**Bad**:
```java
@GetMapping("/{patientId}")
public Patient getPatient(@PathVariable String patientId) {
    return patientRepository.findById(patientId).orElse(null);
}
```

---

## Links and References

### Internal Links

- [ ] All internal links use relative paths from project root
- [ ] Links tested (no 404s)
- [ ] ADRs cross-referenced where applicable
- [ ] Related service READMEs linked

### External Links

- [ ] External links valid and accessible
- [ ] Links to official documentation (FHIR, HIPAA, NCQA)
- [ ] No links to competitor documentation

### Required References

For technical documents, consider including:
- [ ] [TERMINOLOGY_GLOSSARY.md](/docs/TERMINOLOGY_GLOSSARY.md)
- [ ] [HIPAA Cache Compliance](/backend/HIPAA-CACHE-COMPLIANCE.md)
- [ ] [Gateway Trust Architecture](/backend/docs/GATEWAY_TRUST_ARCHITECTURE.md)
- [ ] [CLAUDE.md](/CLAUDE.md) for project conventions

---

## Pre-Commit Final Check

Before `git commit`:

1. [ ] Run spell check on document
2. [ ] Verify all links work
3. [ ] Check terminology against TERMINOLOGY_GLOSSARY.md
4. [ ] Ensure "Last Updated" date is current
5. [ ] Review diff for placeholder content
6. [ ] Confirm no sensitive data in examples
7. [ ] Verify document renders correctly in Markdown preview

---

## Document Type Specific Checks

### Service README

Follow [SERVICE_README_TEMPLATE.md](/docs/templates/SERVICE_README_TEMPLATE.md)

Required sections:
- [ ] Overview
- [ ] Technology Stack
- [ ] API Endpoints
- [ ] Database Schema
- [ ] Kafka Topics
- [ ] Configuration
- [ ] Testing
- [ ] Monitoring
- [ ] Security

### ADR (Architecture Decision Record)

Follow [ADR_TEMPLATE.md](/docs/templates/ADR_TEMPLATE.md)

Required sections:
- [ ] Context and Problem Statement
- [ ] Decision Drivers (minimum 5)
- [ ] Considered Options (minimum 3)
- [ ] Decision Outcome
- [ ] Consequences (positive, negative, neutral)
- [ ] Pros/Cons of ALL options
- [ ] Links

### GTM Materials

Required elements:
- [ ] Technical proof points for all claims
- [ ] ROI calculation methodology
- [ ] Links to technical documentation
- [ ] Competitive differentiation with specifics
- [ ] No unsubstantiated buzzwords

---

## Quality Metrics

### Documentation Health Score

Calculate your document's health score:

| Criterion | Weight | Score (0-10) |
|-----------|--------|--------------|
| Terminology compliance | 20% | |
| Technical accuracy | 25% | |
| Completeness | 20% | |
| Code examples quality | 15% | |
| Links validity | 10% | |
| HIPAA compliance | 10% | |
| **Total** | 100% | |

**Passing score**: 8.0+

---

## Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-12-30 | Documentation Team | Initial creation |

---

*This checklist is the authoritative quality standard for HDIM documentation.*
