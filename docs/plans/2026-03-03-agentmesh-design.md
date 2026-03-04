# AgentMesh — Design Document

**Date:** March 3, 2026
**Author:** Aaron (Mahoosuc Solutions)
**Status:** Approved

---

## Overview

AgentMesh is an open-source event mesh platform where AI agents are first-class citizens, with micro-frontends as the human interface layer. Scaffolded via `npx create-agentmesh-app`. Core event infrastructure extracted from the battle-tested HDIM healthcare platform (316 Java files, production-proven CQRS + Event Sourcing).

**Tagline:** *The open-source event mesh for AI-agent powered businesses*

**License:** Apache 2.0

## Problem Statement

1. **Event sourcing is too hard to start.** The boilerplate is massive — event store, projections, DLQ, replay, multi-tenancy. Developers want it but can't justify 6 months of infrastructure work.

2. **No opinionated ES framework for the AI era.** Axon exists but is complex/commercial with no AI agent story. Temporal is workflow orchestration, not event sourcing.

3. **AI agent frameworks lack durable state.** LangChain, CrewAI, and others have no event backbone, no persistence, no audit trails. Agents forget everything between runs.

4. **No platform unifies events + agents + frontends.** These three concerns are always solved separately, creating integration nightmares.

AgentMesh fills the gap between all of these.

## Strategic Context

AgentMesh serves as the open-source engine that positions Mahoosuc Solutions as the go-to partner for building event-driven AI businesses.

**Business model:** Open core
- **Free:** Core framework, CLI, starters
- **Paid (Mahoosuc Solutions):**
  - Implementation services ($150-300K per engagement)
  - Managed AgentMesh Cloud ($5-50K/month)
  - Domain accelerators — Healthcare (from HDIM), FinTech, InsurTech ($25-100K)
  - Enterprise support contracts

**Revenue targets:**
- Year 1: $500K-1.5M
- Year 2: $2-5M
- Year 3: $8-15M

**HDIM relationship:** HDIM becomes "AgentMesh Healthcare Accelerator" — the commercial healthcare distribution of the open source platform.

## Architecture

### Three Layers

```
┌─────────────────────────────────────────────────────────────┐
│                    MFE Shell (Layer 3)                       │
│  Module Federation │ Event Bridge │ Agent Dashboards │ HITL  │
├─────────────────────────────────────────────────────────────┤
│                  Agent Runtime (Layer 2)                     │
│  Agent Lifecycle │ Event Subscriptions │ Memory │ Tools      │
├─────────────────────────────────────────────────────────────┤
│                   Event Core (Layer 1)                       │
│  Event Store │ CQRS │ Projections │ DLQ │ Replay │ Kafka    │
├─────────────────────────────────────────────────────────────┤
│                   Infrastructure                            │
│  PostgreSQL │ Kafka │ Redis │ Docker Compose                │
└─────────────────────────────────────────────────────────────┘
```

### Layer 1: Event Core (Extracted from HDIM)

| Component | HDIM Origin | AgentMesh Package |
|-----------|------------|-------------------|
| Event Store | `event-store-service` | `@agentmesh/event-store` |
| CQRS Engine | `event-sourcing` shared module | `@agentmesh/cqrs` |
| Event Router | `event-router-service` | `@agentmesh/router` |
| Dead Letter Queue | `DeadLetterQueueService` | `@agentmesh/dlq` |
| Event Replay | `event-replay-service` | `@agentmesh/replay` |
| Kafka Transport | `UserContextKafkaInterceptor` | `@agentmesh/transport` |
| Multi-Tenancy | Tenant filters throughout | `@agentmesh/tenant` |

**Extraction rules:** Strip all healthcare references (FHIR, HEDIS, PHI markers). Generalize `EventUserContext` to generic audit context. Remove HIPAA-specific sensitivity markers. Preserve all architectural patterns.

### Layer 2: Agent Runtime (New Build)

AI agents as first-class event mesh participants:

- **Agent Definition:** `defineAgent()` API with typed subscriptions, productions, memory, approval policies
- **Agent Lifecycle:** register → activate → run → pause → deactivate
- **Agent Memory:** Event replay as context window — agents "remember" via event history
- **LLM Adapters:** Pluggable — Anthropic, OpenAI, Ollama, custom. Agent runtime is LLM-agnostic.
- **Tools:** Tool registry — agents call functions (HTTP, DB, custom)
- **Human-in-the-Loop:** Approval engine — agents request human approval with timeout + escalation
- **Multi-Agent Coordination:** Agents subscribe to each other's events, saga patterns

```typescript
const agent = defineAgent({
  name: 'order-fraud-detector',
  subscriptions: ['OrderPlaced', 'PaymentProcessed'],
  produces: ['FraudFlagged', 'OrderApproved'],
  memory: { replayWindow: '30d', projections: ['CustomerProfile'] },
  approval: { when: (d) => d.confidence < 0.85, timeout: '15m' },
  async handler(event, context) {
    const history = await context.replay('OrderPlaced', { customerId: event.customerId });
    const decision = await context.llm.evaluate({ prompt: '...', context: history });
    return decision.isFraudulent
      ? context.emit('FraudFlagged', { orderId: event.orderId, reason: decision.reason })
      : context.emit('OrderApproved', { orderId: event.orderId });
  },
});
```

### Layer 3: MFE Shell (New Build)

Three roles:
1. **App Shell:** React + Module Federation container, dynamic module loading
2. **Event Bridge:** WebSocket real-time event stream to browser
3. **Agent Control Plane:** Dashboard, approval queue, event inspector, decision audit trail

## Tech Stack

| Layer | Technology | Rationale |
|-------|-----------|-----------|
| Event Store | PostgreSQL + JSONB | Proven at scale in HDIM |
| Transport | Kafka | Industry standard, HDIM battle-tested |
| Agent Runtime | TypeScript (Node.js) | Largest dev ecosystem, async-native, LLM SDK availability |
| MFE Shell | React + Module Federation | Largest MFE ecosystem |
| CLI | Node.js | npm ecosystem, `npx` support |
| Java SDK | Spring Boot starter | Enterprise companion |
| Monorepo | Nx | Task caching, dependency graph, affected commands |

## Repository Structure

```
agentmesh/
├── packages/
│   ├── core/                      # Event Core (Layer 1)
│   │   ├── event-store/
│   │   ├── cqrs/
│   │   ├── router/
│   │   ├── dlq/
│   │   ├── replay/
│   │   ├── transport/
│   │   └── tenant/
│   ├── agent-runtime/             # Agent Runtime (Layer 2)
│   │   ├── runtime/
│   │   ├── memory/
│   │   ├── tools/
│   │   ├── approval/
│   │   └── llm-adapters/
│   ├── mfe-shell/                 # MFE Shell (Layer 3)
│   │   ├── shell/
│   │   ├── event-bridge/
│   │   ├── agent-dashboard/
│   │   ├── approval-ui/
│   │   └── event-inspector/
│   ├── sdk/
│   │   ├── typescript/
│   │   ├── java/
│   │   └── python/
│   └── cli/
│       ├── create-agentmesh-app/
│       └── agentmesh-cli/
├── starters/
│   ├── hello-agent/
│   ├── multi-agent/
│   └── saas-template/
├── docker/
├── docs/
├── website/
├── nx.json
├── package.json
├── LICENSE
└── README.md
```

## Release Strategy

### v0.1 "Spark" (Weeks 1-4)

Event Core + basic Agent Runtime + CLI scaffold + `hello-agent` starter + Docker Compose.

**Goal:** `npx create-agentmesh-app` → working event-driven app with one agent in 5 minutes.

**Ships:** Event Store, CQRS, Router, DLQ, Transport, basic `defineAgent()`, CLI, Docker, docs, website landing.

**Success metric:** 500 GitHub stars in 2 weeks.

### v0.5 "Ignite" (Weeks 5-10)

Agent Memory + LLM Adapters + Human-in-the-Loop + Multi-Agent Coordination + Event Replay + Java Spring Boot Starter + `multi-agent` starter.

**Success metric:** 2,000 stars, 5 external contributors.

### v1.0 "Blaze" (Weeks 11-16)

MFE Shell + Event Bridge + Agent Dashboard + Approval Queue UI + Event Inspector + `saas-template` starter + Auth/RBAC + Observability + Python SDK + Managed Cloud beta.

**Success metric:** 5,000+ stars, first paying cloud customer.

## HDIM Extraction Map

| Component | Effort | Notes |
|-----------|--------|-------|
| Event Store | Low | Strip healthcare, generalize |
| CQRS Engine | Low | Remove FHIR/HEDIS refs |
| Event Router | Medium | Generalize routing rules |
| DLQ | Low | Already generic |
| Kafka Transport | Low | Rename headers |
| Multi-Tenancy | Low | Already a pattern |
| Event Replay | Low | Already generic |
| TestEventWaiter → TS | Low | Port to TypeScript |
| Agent Runtime | High | New code, inspired by HDIM patterns |
| MFE Shell | High | New code |
| CLI | Medium | New code |

~40% extracted from HDIM, ~60% new code.

## Competitive Positioning

| | AgentMesh | Axon | Temporal | LangGraph | Single-SPA |
|---|---|---|---|---|---|
| Event Sourcing | Core | Core | No | No | No |
| CQRS + Projections | Yes | Yes | No | No | No |
| AI Agent Runtime | Native | No | No | Core | No |
| Agent-Event Integration | Mesh | No | No | No | No |
| Micro-Frontends | Built-in | No | No | No | Core |
| Multi-Tenancy | Built-in | No | No | No | No |
| One-Command Setup | Yes | No | Yes | No | No |
| Open Source | Apache 2.0 | Partial | Yes | Yes | Yes |

## Value Estimation

| Scenario | 2-Year Value | Assumptions |
|----------|-------------|-------------|
| Conservative | $5-10M | 3K stars, $500K ARR |
| Moderate | $25-75M | 10K stars, $3M ARR |
| Aggressive | $100-200M+ | 25K+ stars, $10M+ ARR |
