# ADR-[NUMBER]: [Title]

**Date**: YYYY-MM-DD
**Status**: [Proposed | Accepted | Deprecated | Superseded by ADR-XXXX]
**Deciders**: [Names or roles who made this decision]
**Technical Story**: [Link to issue/epic or brief description]

---

## Context and Problem Statement

[Describe the context and problem statement in 2-4 sentences. Articulate the problem as a question if helpful. Include constraints and requirements that influenced the decision.]

**Example:**
> HDIM requires a FHIR R4 compliant server for storing and serving clinical resources. The implementation must support multi-tenant data isolation, high performance (100+ req/sec), and HIPAA compliance with encryption and audit logging.

---

## Decision Drivers

* [Driver 1 - e.g., "HIPAA compliance requirements"]
* [Driver 2 - e.g., "Performance at scale (1M+ patients)"]
* [Driver 3 - e.g., "Integration with existing Spring Boot stack"]
* [Driver 4 - e.g., "Open source licensing preferred"]
* [Driver 5 - e.g., "Community support and documentation"]

---

## Considered Options

1. **[Option 1]** - [Brief description]
2. **[Option 2]** - [Brief description]
3. **[Option 3]** - [Brief description]

*Include at least 3 options to demonstrate thorough evaluation.*

---

## Decision Outcome

**Chosen option**: "[Option X]"

**Rationale**: [Explain why this option was selected, referencing specific decision drivers]

**Example:**
> Chosen option: "HAPI FHIR 7.x", because it is the industry-standard open-source FHIR implementation with native Spring Boot integration, proven in production healthcare environments, and provides the multi-tenant and audit logging capabilities required for HIPAA compliance.

---

## Consequences

### Positive

* [Benefit 1 - e.g., "Faster development with pre-built FHIR operations"]
* [Benefit 2 - e.g., "Standards compliance guaranteed"]
* [Benefit 3 - e.g., "Strong community support"]

### Negative

* [Drawback 1 - e.g., "Learning curve for HAPI-specific patterns"]
* [Drawback 2 - e.g., "Large dependency footprint (~50MB)"]
* [Mitigation for drawbacks if applicable]

### Neutral

* [Observation 1 - e.g., "Requires Java 17+, which aligns with our Java 21 stack"]

---

## Pros and Cons of Options

### Option 1: [Name]

[Brief description or link to more information]

| Aspect | Assessment |
|--------|------------|
| [Criterion 1] | Good/Bad/Neutral - [Reason] |
| [Criterion 2] | Good/Bad/Neutral - [Reason] |
| [Criterion 3] | Good/Bad/Neutral - [Reason] |

**Summary**: [1-2 sentence summary of this option's fit]

---

### Option 2: [Name]

[Brief description or link to more information]

| Aspect | Assessment |
|--------|------------|
| [Criterion 1] | Good/Bad/Neutral - [Reason] |
| [Criterion 2] | Good/Bad/Neutral - [Reason] |
| [Criterion 3] | Good/Bad/Neutral - [Reason] |

**Summary**: [1-2 sentence summary of this option's fit]

---

### Option 3: [Name]

[Brief description or link to more information]

| Aspect | Assessment |
|--------|------------|
| [Criterion 1] | Good/Bad/Neutral - [Reason] |
| [Criterion 2] | Good/Bad/Neutral - [Reason] |
| [Criterion 3] | Good/Bad/Neutral - [Reason] |

**Summary**: [1-2 sentence summary of this option's fit]

---

## Implementation Notes

### Configuration

```yaml
# Example configuration for the chosen option
[config-key]: [value]
```

### Migration Steps (if replacing existing solution)

1. [Step 1]
2. [Step 2]
3. [Step 3]

### Verification

- [ ] [How to verify the implementation is correct]
- [ ] [Performance benchmark to run]
- [ ] [Security check to perform]

---

## Links

* [Documentation for chosen option]
* [Related ADR - ADR-XXXX if applicable]
* [Implementation reference - file path or PR]
* [Issue/Epic link]

---

## Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | YYYY-MM-DD | [Author] | Initial decision |

---

*This ADR follows the template in `/docs/templates/ADR_TEMPLATE.md`*
