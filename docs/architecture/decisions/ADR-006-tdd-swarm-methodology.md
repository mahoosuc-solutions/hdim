# ADR-006: TDD Swarm Development Methodology

**Status**: Accepted
**Date**: 2026-01-19 (Decision Made: Phase 2, Oct 2025)
**Decision Makers**: Architecture Lead, Engineering Leads
**Stakeholders**: All Development Teams

---

## Context

### Problem Statement

Traditional sequential development (one team per week) was too slow for healthcare delivery timelines. HDIM needed concurrent team execution (RED-GREEN-REFACTOR cycles) to accelerate feature delivery while maintaining quality.

**Problem**: Phase 5 (4 event services) would take 8 weeks sequentially—unacceptable for healthcare innovation cycles

---

## Options Considered

### Option 1: TDD Swarm (Test-Driven Development + Parallel Execution)

**Description**: RED phase (all teams write tests in parallel) → GREEN phase (teams implement independently) → REFACTOR phase (consolidate and optimize)

**Results (Phase 5)**:
- Delivery time: 8 weeks → 2 weeks (4x faster)
- Test coverage: 60% → 90%+ (from start)
- Post-launch bugs: 15-20 → 2-3 (85% fewer)
- Code review time: 1 week → 1 day

**Pros**:
- 4x faster delivery (2 weeks vs 8 weeks)
- 90%+ test coverage from day 1
- 85% fewer bugs
- Tests as specification (no ambiguity)
- Team parallelization

**Cons**:
- Requires test-first mindset
- Developer training needed
- More complex test infrastructure
- Initial setup overhead

**Estimated Effort**: 2-week per feature
**Risk Level**: Low (proven in Phase 5)

---

### Option 2: Traditional Sequential Development

**Description**: Team 1 → Team 2 → Team 3 → Team 4 (one after another)

**Results**:
- 8 weeks total
- 60% test coverage (added later)
- 15-20 bugs post-launch

**Pros**:
- Familiar to teams
- No coordination needed
- Clear hand-offs

**Cons**:
- 4x slower
- Low test coverage early
- More bugs
- Late discovery of integration issues

**Risk Level**: High (slow delivery)

---

## Decision

**We chose Option 1 (TDD Swarm)** because:

1. **Speed**: 4x faster delivery directly improves healthcare outcomes
2. **Quality**: 90%+ coverage from start prevents bugs
3. **Proven**: Phase 5 demonstrated 85% fewer post-launch bugs
4. **Team Morale**: Faster delivery improves morale
5. **Cost**: Fewer bugs = lower support costs

---

## Consequences

### Positive

- Phase 5 delivered in 2 weeks (vs 8 weeks estimate)
- 90%+ test coverage achieved from day 1
- Only 2-3 bugs found post-launch (vs 15-20)
- 1-day code reviews (vs 1-week)
- Developers confident refactoring

### Negative

- Requires test-first mindset (not all developers comfortable)
- Higher initial overhead
- More test code to maintain
- Mock management complexity

---

## Implementation

### RED Phase (Test Specification)

All teams write failing tests defining expected behavior:

```java
@Test
void shouldCreatePatientEvent_WithValidData() {
    // Arrange
    PatientCreatedEvent event = new PatientCreatedEvent(...);

    // Act
    eventStore.append(event);

    // Assert
    assertThat(eventStore.getEvents()).contains(event);
}
```

### GREEN Phase (Implementation)

Teams implement code to pass tests:

```java
public void append(Event event) {
    eventStore.insert(event);
}
```

### REFACTOR Phase (Optimization)

Teams consolidate, optimize, and integrate:

```java
public void append(Event event) {
    // Add caching, compression, validation
    validateEvent(event);
    compressEvent(event);
    eventStore.insert(event);
}
```

### Success Criteria

- ✅ All teams deliver on schedule
- ✅ 90%+ test coverage achieved
- ✅ <3 bugs found post-launch
- ✅ 1-day code review turnaround
- ✅ Team satisfaction >4/5

---

## References

- **[TDD Swarm Detailed Guide](../../development/TDD_SWARM.md)**
- **[Service Creation Template](../../development/SERVICE_CREATION_TEMPLATE.md)**

---

## Footer

**ADR #**: 006
**Version**: 1.0
**Status**: Active and Validated
**Phase 5 Results**: 2 weeks (vs 8 weeks), 90% coverage, 85% fewer bugs

_Decision Date: Phase 2 (October 2025)_
_Proven in Phase 5 (Oct 2025 - Jan 2026)_
