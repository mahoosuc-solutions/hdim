# [Skill Name] - TEMPLATE

> **This is a template for all skill guides in the HDIM Skills Center.**
> **Copy this file and customize for each skill.**

---

## Overview

### What is This Skill?

Clear, concise explanation of what the skill is. 2-3 sentences maximum.

**Example:** "CQRS (Command Query Responsibility Segregation) is an architectural pattern that separates read and write operations into different models, enabling independent optimization of each path for better performance and scalability."

### Why is This Important for HDIM?

Explain business and technical impact. Why do we care? What problems does it solve?

**Example:** "HDIM processes quality measures for 1000+ patients across dozens of measures simultaneously. CQRS allows us to optimize write performance for measure evaluation while simultaneously optimizing reads for reporting and analytics."

### Business Impact

Concrete metrics or examples showing value.

**Example:**
- Enables 10,000+ patients per tenant
- <200ms p95 measure evaluation latency
- 99.9% uptime SLA for production

### Key Services Using This Skill

Which HDIM services implement this pattern?

**Example:**
- patient-event-service (8084)
- quality-measure-event-service (8087)
- care-gap-event-service (8086)
- clinical-workflow-event-service

### Estimated Learning Time

How long to understand and be productive with this skill?

**Example:** 1-2 weeks

---

## Key Concepts

Break down the skill into 3-5 core concepts that build on each other.

### Concept 1: [Name]

**Definition:** Clear definition of this concept.

**Why it matters:** How does this support the overall skill?

**Real-world example:** Concrete example from HDIM or general software engineering.

### Concept 2: [Name]

**Definition:** ...

### Concept 3: [Name]

**Definition:** ...

---

## Architecture Pattern

### How It Works

Describe the pattern at a high level. This section should make sense to someone who's never encountered this pattern before.

### Diagram

```
ASCII diagram showing flow/architecture

Example:

Request
  ↓
┌────────────────┐
│ Write Model    │
│ (Commands)     │
└────────┬───────┘
         ↓
    Event Store
         ↓
┌────────────────┐
│ Read Model     │
│ (Projections)  │
└────────┬───────┘
         ↓
    Response
```

### Design Decisions

Why did we make specific choices?

**Decision 1: Why [Choice A] instead of [Choice B]?**
- **Trade-off:** What do we gain? What do we lose?
- **Rationale:** Why is this trade-off acceptable for HDIM?
- **Alternative:** What could we have done instead?

**Example:**
- **Decision:** Separate read and write models (CQRS)
- **Trade-off:** Gain independent scalability, lose immediate consistency
- **Rationale:** HDIM needs to evaluate 10,000+ measures concurrently while serving reports simultaneously
- **Alternative:** Single model (simpler but doesn't scale)

### Trade-offs

| Aspect | Pro | Con |
|--------|-----|-----|
| Performance | Optimized reads and writes separately | Network latency between models |
| Scalability | Can scale read and write independently | More complex infrastructure |
| Consistency | Strong consistency within each model | Eventually consistent between models |
| Cost | Fewer servers needed for same performance | Need multiple models (storage cost) |
| Development | Clear separation of concerns | Requires different thinking (CQRS mindset) |

---

## Implementation Guide

### Step-by-Step

Walk through implementation in logical steps. Each step should be actionable.

#### Step 1: [Initial Setup]

Description of what to do.

```java
// Code example
@Entity
@Table(name = "thing")
public class Thing {
    @Id
    private UUID id;
}
```

#### Step 2: [Second Phase]

Description.

```java
// Next code example
```

#### Step 3: [etc]

### Best Practices

What should engineers always do when implementing this?

- ✅ **DO:** Specific best practice
  - Why: Because...
  - Example: Code snippet

- ❌ **DON'T:** Anti-pattern
  - Why: Because...
  - Example: Bad code snippet

### Common Patterns

Recurring patterns that emerge when using this skill.

**Pattern 1: [Name]**
- When to use: ...
- How to implement: ...
- Example: ...

---

## Real-World Examples from HDIM

### Example 1: [Service Name]

**Where:** `backend/modules/services/[service]/...`

**What it does:** Brief description.

**Key file:** `[exact path to file]`

**Relevant code:**
```java
// Actual code from HDIM
```

**Why this example matters:** How does it illustrate the skill?

### Example 2: [Another Service]

**Where:** ...

### Example 3: [Third Example]

**Where:** ...

---

## Testing Strategies

### Unit Testing

How to unit test this skill.

```java
@ExtendWith(MockitoExtension.class)
class ExampleServiceTest {
    @Test
    void shouldDoSomething() {
        // ARRANGE
        // SETUP

        // ACT
        // EXECUTE

        // ASSERT
        // VERIFY
    }
}
```

### Integration Testing

How to integration test this skill.

```java
@SpringBootTest
@AutoConfigureMockMvc
class ExampleControllerIntegrationTest {
    @Test
    void shouldReturnSuccess() {
        // Test full request/response cycle
    }
}
```

### Test Checklist

- [ ] Happy path (success case)
- [ ] Exception cases
- [ ] Edge cases (null, empty, boundary values)
- [ ] Mock interactions verified
- [ ] Multi-tenant isolation
- [ ] RBAC permissions
- [ ] Performance characteristics

---

## Troubleshooting

### Common Issues

#### Issue 1: [Specific Problem]

**Symptoms:** How do you know something is wrong?
- Error message: `specific error`
- Behavior: What goes wrong

**Root cause:** Why does this happen?

**Solution:**
```bash
# Steps to fix
1. Check X
2. Verify Y
3. Do Z
```

**Prevention:** How to avoid this in the future.

#### Issue 2: [Another Problem]

**Symptoms:** ...

### Debug Techniques

How to debug when things go wrong.

```bash
# Enable debug logging
./gradlew run --debug

# Check database state
docker exec -it hdim-postgres psql -U healthdata -d db_name

# View logs
docker compose logs -f service-name | grep ERROR
```

---

## References & Resources

### HDIM Documentation

- [Related guide 1](#) - Brief description
- [Related guide 2](#) - Brief description
- [Service catalog](#) - List of services using this skill

### External Resources

- **[Official documentation](https://example.com)** - What it covers
- **[Tutorial](https://example.com)** - What you'll learn
- **[Specification](https://example.com)** - Official standard

### Related Skills

These skills build on this one or complement it:

- **Prerequisite:** [Skill A](#) - Learn this first
- **Complement:** [Skill B](#) - Learn this alongside
- **Advanced:** [Skill C](#) - Learn after mastering this

---

## Quick Reference Checklist

### Before You Start
- [ ] Understand the problem this skill solves
- [ ] Know why it's important for HDIM
- [ ] Have reviewed the key concepts

### While Implementing
- [ ] Following best practices (DO's and DON'Ts)
- [ ] Using established patterns from HDIM
- [ ] Writing tests alongside code
- [ ] Checking edge cases
- [ ] Verifying multi-tenant isolation
- [ ] Ensuring HIPAA compliance (if PHI involved)

### After Implementation
- [ ] All tests passing (unit + integration)
- [ ] Code reviewed by peer
- [ ] Performance targets met
- [ ] Documentation updated
- [ ] Ready for production

---

## Key Takeaways

Summarize the most important concepts from this guide:

1. **Core Concept:** Brief summary of the main idea
2. **Implementation:** How you typically implement this
3. **Common Pitfall:** Most frequent mistake people make
4. **Why It Matters:** How this impacts HDIM

---

## FAQ

**Q: When should I use this skill?**
A: Answer specific guidance on when to apply this pattern.

**Q: What's the performance impact?**
A: Concrete metrics from HDIM (latency, throughput, memory, etc.)

**Q: Can I do [thing] without this skill?**
A: Explain dependencies and prerequisites.

**Q: How does this relate to [other skill]?**
A: Explain relationship to complementary skills.

---

## Next Steps

After completing this guide:

1. **Practice:** Write code implementing this skill
2. **Review:** Have peer review your implementation
3. **Test:** Write comprehensive tests
4. **Learn:** Move to next skill in your learning path
5. **Contribute:** Help others learn this skill

**Your Next Guide:** [Next skill in learning path](#)

---

## Feedback

Found an issue with this guide? Have suggestions?

1. Check if another guide already covers this
2. Open an issue with specific suggestions
3. Submit a PR with improvements
4. Help newer team members understand this skill

---

**Last Updated:** [Date]
**Version:** 1.0
**Difficulty Level:** ⭐⭐⭐ (1-5 stars)
**Time Investment:** [X] weeks
**Prerequisite Skills:** [List]
**Related Skills:** [List]

---

**← Previous Guide** | [Skills Hub](./README.md) | **Next Guide →**
