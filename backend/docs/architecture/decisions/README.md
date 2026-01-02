# Architecture Decision Records (ADRs)

This directory contains Architecture Decision Records for the HDIM AI Agent Platform.

## What is an ADR?

An Architecture Decision Record captures an important architectural decision made along with its context and consequences. ADRs are immutable once accepted - if a decision changes, a new ADR is created that supersedes the old one.

## ADR Index

| ADR | Title | Status | Date |
|-----|-------|--------|------|
| [0000](0000-adr-template.md) | ADR Template | Template | - |
| [0001](0001-multi-provider-llm-architecture.md) | Multi-Provider LLM Architecture | Accepted | 2024-12-06 |
| [0002](0002-no-code-agent-builder.md) | No-Code Agent Builder Design | Accepted | 2024-12-06 |
| [0003](0003-phi-safe-memory-management.md) | PHI-Safe Memory Management | Accepted | 2024-12-06 |
| [0004](0004-agent-versioning-strategy.md) | Agent Versioning and Rollback Strategy | Accepted | 2024-12-06 |

## ADR Status Definitions

- **Proposed**: Under discussion, not yet accepted
- **Accepted**: Decision has been made and is in effect
- **Deprecated**: Decision is no longer relevant
- **Superseded**: Decision has been replaced by a newer ADR

## Creating a New ADR

1. Copy `0000-adr-template.md` to a new file with the next number
2. Fill in the Context, Decision, and Consequences sections
3. Submit for review
4. Update this README index once accepted

## Related Documentation

- [HDIM AI Agent Platform Plan](../../../.claude/plans/agile-launching-church.md)
- [Agent Builder Service](../../modules/services/agent-builder-service/)
- [Agent Runtime Service](../../modules/services/agent-runtime-service/)
