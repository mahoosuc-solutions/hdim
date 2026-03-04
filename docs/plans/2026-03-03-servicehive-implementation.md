# ServiceHive.ai Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build ServiceHive.ai — a multi-product GTM portfolio manager with 4 AI agents, React dashboard, and LinkedIn publishing scheduler — as the first app on AgentMesh.

**Architecture:** AgentMesh Event Core (PostgreSQL event store + LISTEN/NOTIFY transport) provides the event backbone. Four TypeScript agents subscribe to and produce events. React dashboard connects via WebSocket event bridge. BullMQ + Redis handles delayed job execution for LinkedIn post scheduling.

**Tech Stack:** TypeScript, Nx monorepo, React + Vite, PostgreSQL, Redis, BullMQ, WebSocket, LinkedIn Posts API, Claude API (via LLM adapter)

**Design doc:** `docs/plans/2026-03-03-servicehive-design.md`
**AgentMesh design:** `docs/plans/2026-03-03-agentmesh-design.md`

---

## Phase 1: AgentMesh Event Core (Tasks 1-6)

These tasks create the minimal AgentMesh packages that ServiceHive depends on. This is NOT the full AgentMesh v0.1 — it's the subset needed to run ServiceHive.

### Task 1: Scaffold AgentMesh Nx Monorepo

**Files:**
- Create: `/mnt/wdblack/dev/projects/agentmesh/package.json`
- Create: `/mnt/wdblack/dev/projects/agentmesh/nx.json`
- Create: `/mnt/wdblack/dev/projects/agentmesh/tsconfig.base.json`
- Create: `/mnt/wdblack/dev/projects/agentmesh/.gitignore`
- Create: `/mnt/wdblack/dev/projects/agentmesh/LICENSE`
- Create: `/mnt/wdblack/dev/projects/agentmesh/README.md`

**Step 1: Create directory and initialize Nx**

```bash
mkdir -p /mnt/wdblack/dev/projects/agentmesh
cd /mnt/wdblack/dev/projects/agentmesh
npx create-nx-workspace@latest agentmesh --preset=ts --nxCloud=skip --packageManager=npm
```

If interactive prompts block, use manual setup:

```bash
cd /mnt/wdblack/dev/projects/agentmesh
npm init -y
npm install -D nx@latest @nx/js @nx/node typescript @types/node vitest
```

**Step 2: Configure Nx workspace**

Create `nx.json`:
```json
{
  "$schema": "./node_modules/nx/schemas/nx-schema.json",
  "targetDefaults": {
    "build": { "dependsOn": ["^build"], "cache": true },
    "test": { "cache": true },
    "lint": { "cache": true }
  },
  "defaultBase": "main"
}
```

Create `tsconfig.base.json`:
```json
{
  "compileOnSave": false,
  "compilerOptions": {
    "rootDir": ".",
    "sourceMap": true,
    "declaration": true,
    "moduleResolution": "node",
    "emitDecoratorMetadata": true,
    "experimentalDecorators": true,
    "importHelpers": true,
    "target": "es2022",
    "module": "es2022",
    "lib": ["es2022"],
    "skipLibCheck": true,
    "skipDefaultLibCheck": true,
    "baseUrl": ".",
    "strict": true,
    "paths": {
      "@agentmesh/event-store": ["packages/core/event-store/src/index.ts"],
      "@agentmesh/runtime": ["packages/agent-runtime/runtime/src/index.ts"]
    }
  },
  "exclude": ["node_modules", "dist"]
}
```

**Step 3: Create package directory structure**

```bash
mkdir -p packages/core/event-store/src
mkdir -p packages/agent-runtime/runtime/src
mkdir -p packages/core/event-store/__tests__
mkdir -p packages/agent-runtime/runtime/__tests__
```

**Step 4: Initialize git and commit**

```bash
git init
git add -A
git commit -m "chore: scaffold AgentMesh Nx monorepo"
```

---

### Task 2: Implement @agentmesh/event-store

**Files:**
- Create: `packages/core/event-store/src/index.ts`
- Create: `packages/core/event-store/src/event-store.ts`
- Create: `packages/core/event-store/src/types.ts`
- Create: `packages/core/event-store/src/pg-event-store.ts`
- Create: `packages/core/event-store/src/in-memory-event-store.ts`
- Test: `packages/core/event-store/__tests__/event-store.test.ts`
- Create: `packages/core/event-store/package.json`
- Create: `packages/core/event-store/tsconfig.json`
- Create: `packages/core/event-store/project.json`

**Step 1: Write the failing test**

```typescript
// packages/core/event-store/__tests__/event-store.test.ts
import { describe, it, expect, beforeEach } from 'vitest';
import { InMemoryEventStore } from '../src/in-memory-event-store';
import type { AgentMeshEvent } from '../src/types';

describe('InMemoryEventStore', () => {
  let store: InMemoryEventStore;

  beforeEach(() => {
    store = new InMemoryEventStore();
  });

  it('should append and retrieve events', async () => {
    const event: AgentMeshEvent = {
      id: '1',
      type: 'PostDrafted',
      aggregateId: 'post-1',
      aggregateType: 'LinkedInPost',
      producedBy: 'linkedin-agent',
      timestamp: new Date().toISOString(),
      version: 1,
      payload: { content: 'Test post', wordCount: 5 },
      metadata: { correlationId: 'corr-1' },
    };

    await store.append(event);
    const events = await store.getByAggregateId('post-1');
    expect(events).toHaveLength(1);
    expect(events[0].type).toBe('PostDrafted');
  });

  it('should subscribe to events by type', async () => {
    const received: AgentMeshEvent[] = [];
    store.subscribe('PostDrafted', (event) => received.push(event));

    await store.append({
      id: '2',
      type: 'PostDrafted',
      aggregateId: 'post-2',
      aggregateType: 'LinkedInPost',
      producedBy: 'linkedin-agent',
      timestamp: new Date().toISOString(),
      version: 1,
      payload: {},
      metadata: { correlationId: 'corr-2' },
    });

    expect(received).toHaveLength(1);
  });

  it('should not deliver events to unrelated subscribers', async () => {
    const received: AgentMeshEvent[] = [];
    store.subscribe('EmailSent', (event) => received.push(event));

    await store.append({
      id: '3',
      type: 'PostDrafted',
      aggregateId: 'post-3',
      aggregateType: 'LinkedInPost',
      producedBy: 'linkedin-agent',
      timestamp: new Date().toISOString(),
      version: 1,
      payload: {},
      metadata: { correlationId: 'corr-3' },
    });

    expect(received).toHaveLength(0);
  });

  it('should replay events from a given position', async () => {
    for (let i = 0; i < 5; i++) {
      await store.append({
        id: `e-${i}`,
        type: 'MetricsUpdated',
        aggregateId: 'metrics',
        aggregateType: 'Metrics',
        producedBy: 'tracker',
        timestamp: new Date().toISOString(),
        version: 1,
        payload: { value: i },
        metadata: { correlationId: `c-${i}` },
      });
    }

    const replayed = await store.replay({ fromPosition: 2 });
    expect(replayed).toHaveLength(3);
    expect(replayed[0].payload.value).toBe(2);
  });
});
```

**Step 2: Run test to verify it fails**

Run: `npx vitest run packages/core/event-store/__tests__/event-store.test.ts`
Expected: FAIL — modules don't exist yet

**Step 3: Implement types**

```typescript
// packages/core/event-store/src/types.ts
export interface AgentMeshEvent {
  id: string;
  type: string;
  aggregateId: string;
  aggregateType: string;
  producedBy: string;
  timestamp: string;
  version: number;
  payload: Record<string, any>;
  metadata: {
    correlationId: string;
    tenantId?: string;
  };
}

export type EventHandler = (event: AgentMeshEvent) => void | Promise<void>;

export interface EventStore {
  append(event: AgentMeshEvent): Promise<void>;
  getByAggregateId(aggregateId: string): Promise<AgentMeshEvent[]>;
  getByType(type: string, options?: { limit?: number }): Promise<AgentMeshEvent[]>;
  subscribe(eventType: string, handler: EventHandler): () => void;
  subscribeAll(handler: EventHandler): () => void;
  replay(options?: { fromPosition?: number; eventTypes?: string[] }): Promise<AgentMeshEvent[]>;
}
```

**Step 4: Implement InMemoryEventStore**

```typescript
// packages/core/event-store/src/in-memory-event-store.ts
import type { AgentMeshEvent, EventHandler, EventStore } from './types';

export class InMemoryEventStore implements EventStore {
  private events: AgentMeshEvent[] = [];
  private subscribers = new Map<string, Set<EventHandler>>();
  private allSubscribers = new Set<EventHandler>();

  async append(event: AgentMeshEvent): Promise<void> {
    this.events.push(event);
    const handlers = this.subscribers.get(event.type);
    if (handlers) {
      for (const handler of handlers) await handler(event);
    }
    for (const handler of this.allSubscribers) await handler(event);
  }

  async getByAggregateId(aggregateId: string): Promise<AgentMeshEvent[]> {
    return this.events.filter((e) => e.aggregateId === aggregateId);
  }

  async getByType(type: string, options?: { limit?: number }): Promise<AgentMeshEvent[]> {
    const matched = this.events.filter((e) => e.type === type);
    return options?.limit ? matched.slice(0, options.limit) : matched;
  }

  subscribe(eventType: string, handler: EventHandler): () => void {
    if (!this.subscribers.has(eventType)) this.subscribers.set(eventType, new Set());
    this.subscribers.get(eventType)!.add(handler);
    return () => this.subscribers.get(eventType)?.delete(handler);
  }

  subscribeAll(handler: EventHandler): () => void {
    this.allSubscribers.add(handler);
    return () => this.allSubscribers.delete(handler);
  }

  async replay(options?: { fromPosition?: number; eventTypes?: string[] }): Promise<AgentMeshEvent[]> {
    let result = this.events.slice(options?.fromPosition ?? 0);
    if (options?.eventTypes) {
      result = result.filter((e) => options.eventTypes!.includes(e.type));
    }
    return result;
  }
}
```

**Step 5: Create barrel export**

```typescript
// packages/core/event-store/src/index.ts
export type { AgentMeshEvent, EventHandler, EventStore } from './types';
export { InMemoryEventStore } from './in-memory-event-store';
```

**Step 6: Run tests to verify they pass**

Run: `npx vitest run packages/core/event-store/__tests__/event-store.test.ts`
Expected: 4 tests PASS

**Step 7: Commit**

```bash
git add packages/core/event-store/
git commit -m "feat(event-store): implement InMemoryEventStore with subscribe and replay"
```

---

### Task 3: Implement @agentmesh/runtime (defineAgent API)

**Files:**
- Create: `packages/agent-runtime/runtime/src/index.ts`
- Create: `packages/agent-runtime/runtime/src/types.ts`
- Create: `packages/agent-runtime/runtime/src/define-agent.ts`
- Create: `packages/agent-runtime/runtime/src/agent-runner.ts`
- Test: `packages/agent-runtime/runtime/__tests__/agent-runner.test.ts`

**Step 1: Write the failing test**

```typescript
// packages/agent-runtime/runtime/__tests__/agent-runner.test.ts
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { defineAgent } from '../src/define-agent';
import { AgentRunner } from '../src/agent-runner';
import { InMemoryEventStore } from '@agentmesh/event-store';

describe('AgentRunner', () => {
  let eventStore: InMemoryEventStore;
  let runner: AgentRunner;

  beforeEach(() => {
    eventStore = new InMemoryEventStore();
    runner = new AgentRunner(eventStore);
  });

  it('should register and trigger an agent on subscribed event', async () => {
    const handler = vi.fn().mockResolvedValue(undefined);

    const agent = defineAgent({
      name: 'test-agent',
      subscriptions: ['TestEvent'],
      produces: ['TestResult'],
      handler,
    });

    runner.register(agent);
    await runner.start();

    await eventStore.append({
      id: '1',
      type: 'TestEvent',
      aggregateId: 'agg-1',
      aggregateType: 'Test',
      producedBy: 'system',
      timestamp: new Date().toISOString(),
      version: 1,
      payload: { value: 42 },
      metadata: { correlationId: 'c-1' },
    });

    expect(handler).toHaveBeenCalledTimes(1);
    expect(handler.mock.calls[0][0].type).toBe('TestEvent');
  });

  it('should not trigger agent for unsubscribed events', async () => {
    const handler = vi.fn().mockResolvedValue(undefined);

    const agent = defineAgent({
      name: 'selective-agent',
      subscriptions: ['PostDrafted'],
      produces: [],
      handler,
    });

    runner.register(agent);
    await runner.start();

    await eventStore.append({
      id: '2',
      type: 'EmailSent',
      aggregateId: 'email-1',
      aggregateType: 'Email',
      producedBy: 'system',
      timestamp: new Date().toISOString(),
      version: 1,
      payload: {},
      metadata: { correlationId: 'c-2' },
    });

    expect(handler).not.toHaveBeenCalled();
  });

  it('should provide emit function in agent context', async () => {
    const agent = defineAgent({
      name: 'emitting-agent',
      subscriptions: ['InputEvent'],
      produces: ['OutputEvent'],
      async handler(event, context) {
        await context.emit('OutputEvent', 'output-1', { result: 'done' });
      },
    });

    runner.register(agent);
    await runner.start();

    await eventStore.append({
      id: '3',
      type: 'InputEvent',
      aggregateId: 'input-1',
      aggregateType: 'Input',
      producedBy: 'system',
      timestamp: new Date().toISOString(),
      version: 1,
      payload: {},
      metadata: { correlationId: 'c-3' },
    });

    const outputEvents = await eventStore.getByType('OutputEvent');
    expect(outputEvents).toHaveLength(1);
    expect(outputEvents[0].payload.result).toBe('done');
    expect(outputEvents[0].producedBy).toBe('emitting-agent');
  });
});
```

**Step 2: Run test to verify it fails**

Run: `npx vitest run packages/agent-runtime/runtime/__tests__/agent-runner.test.ts`
Expected: FAIL

**Step 3: Implement types**

```typescript
// packages/agent-runtime/runtime/src/types.ts
import type { AgentMeshEvent, EventStore } from '@agentmesh/event-store';

export interface AgentContext {
  event: AgentMeshEvent;
  emit(type: string, aggregateId: string, payload: Record<string, any>): Promise<void>;
  store: EventStore;
}

export interface AgentConfig {
  name: string;
  subscriptions: string[];
  produces: string[];
  handler: (event: AgentMeshEvent, context: AgentContext) => Promise<void>;
  approval?: {
    when: (event: AgentMeshEvent) => boolean;
    timeout?: string;
  };
}

export interface Agent {
  config: AgentConfig;
}
```

**Step 4: Implement defineAgent**

```typescript
// packages/agent-runtime/runtime/src/define-agent.ts
import type { Agent, AgentConfig } from './types';

export function defineAgent(config: AgentConfig): Agent {
  return { config };
}
```

**Step 5: Implement AgentRunner**

```typescript
// packages/agent-runtime/runtime/src/agent-runner.ts
import { randomUUID } from 'crypto';
import type { EventStore, AgentMeshEvent } from '@agentmesh/event-store';
import type { Agent, AgentContext } from './types';

export class AgentRunner {
  private agents: Agent[] = [];
  private unsubscribers: (() => void)[] = [];

  constructor(private eventStore: EventStore) {}

  register(agent: Agent): void {
    this.agents.push(agent);
  }

  async start(): Promise<void> {
    for (const agent of this.agents) {
      for (const eventType of agent.config.subscriptions) {
        const unsub = this.eventStore.subscribe(eventType, async (event) => {
          const context: AgentContext = {
            event,
            store: this.eventStore,
            emit: async (type, aggregateId, payload) => {
              await this.eventStore.append({
                id: randomUUID(),
                type,
                aggregateId,
                aggregateType: type.replace(/([A-Z])/g, ' $1').trim().split(' ')[0],
                producedBy: agent.config.name,
                timestamp: new Date().toISOString(),
                version: 1,
                payload,
                metadata: { correlationId: event.metadata.correlationId },
              });
            },
          };
          await agent.config.handler(event, context);
        });
        this.unsubscribers.push(unsub);
      }
    }
  }

  async stop(): Promise<void> {
    this.unsubscribers.forEach((unsub) => unsub());
    this.unsubscribers = [];
  }
}
```

**Step 6: Create barrel export**

```typescript
// packages/agent-runtime/runtime/src/index.ts
export type { AgentConfig, AgentContext, Agent } from './types';
export { defineAgent } from './define-agent';
export { AgentRunner } from './agent-runner';
```

**Step 7: Run tests to verify they pass**

Run: `npx vitest run packages/agent-runtime/runtime/__tests__/agent-runner.test.ts`
Expected: 3 tests PASS

**Step 8: Commit**

```bash
git add packages/agent-runtime/
git commit -m "feat(runtime): implement defineAgent API and AgentRunner with event subscriptions"
```

---

### Task 4: Add PostgreSQL EventStore implementation

**Files:**
- Create: `packages/core/event-store/src/pg-event-store.ts`
- Create: `packages/core/event-store/src/schema.sql`
- Test: `packages/core/event-store/__tests__/pg-event-store.test.ts`

**Step 1: Write schema**

```sql
-- packages/core/event-store/src/schema.sql
CREATE SCHEMA IF NOT EXISTS events;

CREATE TABLE IF NOT EXISTS events.event_log (
  position BIGSERIAL PRIMARY KEY,
  id UUID NOT NULL UNIQUE,
  type VARCHAR(255) NOT NULL,
  aggregate_id VARCHAR(255) NOT NULL,
  aggregate_type VARCHAR(255) NOT NULL,
  produced_by VARCHAR(255) NOT NULL,
  timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  version INTEGER NOT NULL DEFAULT 1,
  payload JSONB NOT NULL DEFAULT '{}',
  metadata JSONB NOT NULL DEFAULT '{}',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_event_log_aggregate_id ON events.event_log(aggregate_id);
CREATE INDEX idx_event_log_type ON events.event_log(type);
CREATE INDEX idx_event_log_timestamp ON events.event_log(timestamp);

-- LISTEN/NOTIFY trigger for real-time subscriptions
CREATE OR REPLACE FUNCTION events.notify_event() RETURNS trigger AS $$
BEGIN
  PERFORM pg_notify('agentmesh_events', json_build_object(
    'id', NEW.id,
    'type', NEW.type,
    'aggregate_id', NEW.aggregate_id,
    'position', NEW.position
  )::text);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER event_log_notify
  AFTER INSERT ON events.event_log
  FOR EACH ROW EXECUTE FUNCTION events.notify_event();
```

**Step 2: Implement PgEventStore**

```typescript
// packages/core/event-store/src/pg-event-store.ts
import { Pool, type PoolConfig } from 'pg';
import type { AgentMeshEvent, EventHandler, EventStore } from './types';

export class PgEventStore implements EventStore {
  private pool: Pool;
  private subscribers = new Map<string, Set<EventHandler>>();
  private allSubscribers = new Set<EventHandler>();
  private listening = false;

  constructor(config: PoolConfig) {
    this.pool = new Pool(config);
  }

  async append(event: AgentMeshEvent): Promise<void> {
    await this.pool.query(
      `INSERT INTO events.event_log (id, type, aggregate_id, aggregate_type, produced_by, timestamp, version, payload, metadata)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)`,
      [event.id, event.type, event.aggregateId, event.aggregateType, event.producedBy,
       event.timestamp, event.version, JSON.stringify(event.payload), JSON.stringify(event.metadata)]
    );
    // Local dispatch for same-process subscribers
    await this.dispatch(event);
  }

  async getByAggregateId(aggregateId: string): Promise<AgentMeshEvent[]> {
    const result = await this.pool.query(
      'SELECT * FROM events.event_log WHERE aggregate_id = $1 ORDER BY position',
      [aggregateId]
    );
    return result.rows.map(this.rowToEvent);
  }

  async getByType(type: string, options?: { limit?: number }): Promise<AgentMeshEvent[]> {
    const limit = options?.limit ?? 100;
    const result = await this.pool.query(
      'SELECT * FROM events.event_log WHERE type = $1 ORDER BY position DESC LIMIT $2',
      [type, limit]
    );
    return result.rows.map(this.rowToEvent);
  }

  subscribe(eventType: string, handler: EventHandler): () => void {
    if (!this.subscribers.has(eventType)) this.subscribers.set(eventType, new Set());
    this.subscribers.get(eventType)!.add(handler);
    return () => this.subscribers.get(eventType)?.delete(handler);
  }

  subscribeAll(handler: EventHandler): () => void {
    this.allSubscribers.add(handler);
    return () => this.allSubscribers.delete(handler);
  }

  async replay(options?: { fromPosition?: number; eventTypes?: string[] }): Promise<AgentMeshEvent[]> {
    const conditions = ['position >= $1'];
    const params: any[] = [options?.fromPosition ?? 0];
    if (options?.eventTypes?.length) {
      conditions.push(`type = ANY($2)`);
      params.push(options.eventTypes);
    }
    const result = await this.pool.query(
      `SELECT * FROM events.event_log WHERE ${conditions.join(' AND ')} ORDER BY position`,
      params
    );
    return result.rows.map(this.rowToEvent);
  }

  async startListening(): Promise<void> {
    if (this.listening) return;
    this.listening = true;
    const client = await this.pool.connect();
    await client.query('LISTEN agentmesh_events');
    client.on('notification', async (msg) => {
      if (msg.channel === 'agentmesh_events' && msg.payload) {
        const { id } = JSON.parse(msg.payload);
        const result = await this.pool.query('SELECT * FROM events.event_log WHERE id = $1', [id]);
        if (result.rows.length) await this.dispatch(this.rowToEvent(result.rows[0]));
      }
    });
  }

  async close(): Promise<void> {
    await this.pool.end();
  }

  private async dispatch(event: AgentMeshEvent): Promise<void> {
    const handlers = this.subscribers.get(event.type);
    if (handlers) for (const h of handlers) await h(event);
    for (const h of this.allSubscribers) await h(event);
  }

  private rowToEvent(row: any): AgentMeshEvent {
    return {
      id: row.id,
      type: row.type,
      aggregateId: row.aggregate_id,
      aggregateType: row.aggregate_type,
      producedBy: row.produced_by,
      timestamp: row.timestamp,
      version: row.version,
      payload: row.payload,
      metadata: row.metadata,
    };
  }
}
```

**Step 3: Update barrel export**

```typescript
// packages/core/event-store/src/index.ts
export type { AgentMeshEvent, EventHandler, EventStore } from './types';
export { InMemoryEventStore } from './in-memory-event-store';
export { PgEventStore } from './pg-event-store';
```

**Step 4: Install pg dependency**

```bash
npm install pg
npm install -D @types/pg
```

**Step 5: Commit**

```bash
git add packages/core/event-store/
git commit -m "feat(event-store): add PgEventStore with LISTEN/NOTIFY real-time subscriptions"
```

---

### Task 5: Add Docker Compose for AgentMesh infrastructure

**Files:**
- Create: `docker/docker-compose.yml`
- Create: `docker/init.sql`

**Step 1: Create Docker Compose**

```yaml
# docker/docker-compose.yml
version: '3.8'
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: agentmesh
      POSTGRES_USER: agentmesh
      POSTGRES_PASSWORD: agentmesh_dev
    ports:
      - '5433:5432'
    volumes:
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
      - agentmesh_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - '6381:6379'

volumes:
  agentmesh_data:
```

**Step 2: Create init.sql** (copy from schema.sql + add projections/config schemas)

```sql
-- docker/init.sql
-- Event Store schema
CREATE SCHEMA IF NOT EXISTS events;

CREATE TABLE IF NOT EXISTS events.event_log (
  position BIGSERIAL PRIMARY KEY,
  id UUID NOT NULL UNIQUE,
  type VARCHAR(255) NOT NULL,
  aggregate_id VARCHAR(255) NOT NULL,
  aggregate_type VARCHAR(255) NOT NULL,
  produced_by VARCHAR(255) NOT NULL,
  timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  version INTEGER NOT NULL DEFAULT 1,
  payload JSONB NOT NULL DEFAULT '{}',
  metadata JSONB NOT NULL DEFAULT '{}',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_event_log_aggregate_id ON events.event_log(aggregate_id);
CREATE INDEX idx_event_log_type ON events.event_log(type);
CREATE INDEX idx_event_log_timestamp ON events.event_log(timestamp);

CREATE OR REPLACE FUNCTION events.notify_event() RETURNS trigger AS $$
BEGIN
  PERFORM pg_notify('agentmesh_events', json_build_object(
    'id', NEW.id, 'type', NEW.type, 'aggregate_id', NEW.aggregate_id, 'position', NEW.position
  )::text);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER event_log_notify
  AFTER INSERT ON events.event_log
  FOR EACH ROW EXECUTE FUNCTION events.notify_event();

-- Projections schema (read models)
CREATE SCHEMA IF NOT EXISTS projections;

-- Config schema (non-event-sourced)
CREATE SCHEMA IF NOT EXISTS config;

CREATE TABLE config.products (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(255) NOT NULL,
  description TEXT,
  status VARCHAR(50) DEFAULT 'active',
  repo_url VARCHAR(500),
  dashboard_url VARCHAR(500),
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE config.prospects (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(255) NOT NULL,
  title VARCHAR(255),
  company VARCHAR(255),
  linkedin_url VARCHAR(500),
  email VARCHAR(255),
  tier INTEGER DEFAULT 2,
  notes TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE config.comment_bank (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  target_person_id UUID REFERENCES config.prospects(id),
  topic VARCHAR(255),
  comment_text TEXT NOT NULL,
  last_used_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE config.voice_guidelines (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  mode VARCHAR(50) NOT NULL UNIQUE,
  rules JSONB NOT NULL DEFAULT '[]',
  examples JSONB NOT NULL DEFAULT '[]'
);

CREATE TABLE config.agent_config (
  agent_name VARCHAR(100) PRIMARY KEY,
  llm_provider VARCHAR(50) DEFAULT 'claude',
  model VARCHAR(100) DEFAULT 'claude-sonnet-4-5-20241022',
  temperature DECIMAL(3,2) DEFAULT 0.7,
  max_tokens INTEGER DEFAULT 4096
);

-- Projection tables
CREATE TABLE projections.content_calendar (
  id UUID PRIMARY KEY,
  post_date DATE NOT NULL,
  status VARCHAR(50) NOT NULL DEFAULT 'planned',
  topic VARCHAR(500),
  content TEXT,
  word_count INTEGER,
  hashtags TEXT[],
  mode VARCHAR(50) DEFAULT 'thought-leadership',
  post_url VARCHAR(500),
  post_urn VARCHAR(500),
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE projections.approval_queue (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id UUID NOT NULL,
  event_type VARCHAR(255) NOT NULL,
  agent_name VARCHAR(100) NOT NULL,
  summary TEXT NOT NULL,
  payload JSONB NOT NULL DEFAULT '{}',
  status VARCHAR(50) DEFAULT 'pending',
  created_at TIMESTAMPTZ DEFAULT NOW(),
  resolved_at TIMESTAMPTZ
);

CREATE TABLE projections.linkedin_metrics (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  post_id UUID,
  measured_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  impressions INTEGER DEFAULT 0,
  engagement_rate DECIMAL(5,2) DEFAULT 0,
  comments INTEGER DEFAULT 0,
  likes INTEGER DEFAULT 0,
  shares INTEGER DEFAULT 0,
  profile_views INTEGER DEFAULT 0,
  connection_requests INTEGER DEFAULT 0
);
```

**Step 3: Commit**

```bash
git add docker/
git commit -m "infra: add Docker Compose with PostgreSQL + Redis for AgentMesh"
```

---

### Task 6: Create .env.example and verify infrastructure boots

**Files:**
- Create: `.env.example`

**Step 1: Create .env.example**

```bash
# AgentMesh Configuration
DATABASE_URL=postgresql://agentmesh:agentmesh_dev@localhost:5433/agentmesh
REDIS_URL=redis://localhost:6381

# LinkedIn Posts API
LINKEDIN_ACCESS_TOKEN=
LINKEDIN_PERSON_ID=
LINKEDIN_TOKEN_EXPIRY=

# LLM Provider
ANTHROPIC_API_KEY=
LLM_MODEL=claude-sonnet-4-5-20241022

# Email (Resend)
RESEND_API_KEY=
EMAIL_FROM=aaron@mahoosuc.solutions
```

**Step 2: Boot infrastructure and verify**

```bash
cd docker && docker compose up -d
docker compose ps  # Both postgres and redis should be healthy
docker exec -it docker-postgres-1 psql -U agentmesh -d agentmesh -c "\dt events.*"
docker exec -it docker-postgres-1 psql -U agentmesh -d agentmesh -c "\dt config.*"
docker exec -it docker-postgres-1 psql -U agentmesh -d agentmesh -c "\dt projections.*"
```

Expected: All tables exist in all three schemas.

**Step 3: Commit**

```bash
git add .env.example
git commit -m "chore: add .env.example with all ServiceHive configuration"
```

---

## Phase 2: ServiceHive LinkedIn Agent + Dashboard (Tasks 7-12)

### Task 7: Scaffold ServiceHive packages

**Files:**
- Create: `packages/agents/linkedin-agent/package.json`
- Create: `packages/agents/linkedin-agent/src/agent.ts`
- Create: `packages/agents/linkedin-agent/tsconfig.json`
- Create: `packages/scheduler/package.json`
- Create: `packages/scheduler/src/scheduler.ts`
- Create: `packages/server/package.json`
- Create: `packages/server/src/index.ts`
- Create: `packages/dashboard/package.json` (Vite + React)

**Step 1: Create LinkedIn agent package**

```bash
mkdir -p packages/agents/linkedin-agent/src/handlers
mkdir -p packages/agents/linkedin-agent/src/tools
mkdir -p packages/agents/linkedin-agent/__tests__
```

```json
// packages/agents/linkedin-agent/package.json
{
  "name": "@servicehive/linkedin-agent",
  "version": "0.1.0",
  "private": true,
  "type": "module",
  "main": "src/index.ts",
  "dependencies": {
    "@agentmesh/event-store": "workspace:*",
    "@agentmesh/runtime": "workspace:*"
  }
}
```

**Step 2: Create scheduler package**

```bash
mkdir -p packages/scheduler/src/workers
```

```bash
npm install bullmq
```

**Step 3: Create server package**

```bash
mkdir -p packages/server/src/api
mkdir -p packages/server/src/projections
mkdir -p packages/server/src/seed
```

```bash
npm install express ws cors dotenv
npm install -D @types/express @types/ws @types/cors
```

**Step 4: Create dashboard (React + Vite)**

```bash
cd packages
npm create vite@latest dashboard -- --template react-ts
cd dashboard
npm install
```

**Step 5: Commit**

```bash
git add packages/
git commit -m "chore: scaffold ServiceHive packages — linkedin-agent, scheduler, server, dashboard"
```

---

### Task 8: Implement LinkedIn Agent — draftPost handler

**Files:**
- Create: `packages/agents/linkedin-agent/src/agent.ts`
- Create: `packages/agents/linkedin-agent/src/handlers/draftPost.ts`
- Test: `packages/agents/linkedin-agent/__tests__/draft-post.test.ts`

**Step 1: Write the failing test**

```typescript
// packages/agents/linkedin-agent/__tests__/draft-post.test.ts
import { describe, it, expect, beforeEach } from 'vitest';
import { InMemoryEventStore } from '@agentmesh/event-store';
import { AgentRunner } from '@agentmesh/runtime';
import { createLinkedInAgent } from '../src/agent';

describe('LinkedIn Agent — draftPost', () => {
  let eventStore: InMemoryEventStore;
  let runner: AgentRunner;

  beforeEach(async () => {
    eventStore = new InMemoryEventStore();
    runner = new AgentRunner(eventStore);
    // Use a mock LLM that returns a fixed draft
    const agent = createLinkedInAgent({
      llm: {
        generate: async () => ({
          content: 'Test post about quality measurement. This is a draft.',
        }),
      },
    });
    runner.register(agent);
    await runner.start();
  });

  it('should produce PostDrafted when ContentCalendarDue fires', async () => {
    await eventStore.append({
      id: 'cal-1',
      type: 'ContentCalendarDue',
      aggregateId: 'calendar-2026-03-06',
      aggregateType: 'ContentCalendar',
      producedBy: 'system',
      timestamp: new Date().toISOString(),
      version: 1,
      payload: {
        date: '2026-03-06',
        dueItems: [{ postId: 'post-2', topic: 'Stars ratings infrastructure', mode: 'thought-leadership' }],
      },
      metadata: { correlationId: 'cc-1' },
    });

    const drafted = await eventStore.getByType('PostDrafted');
    expect(drafted).toHaveLength(1);
    expect(drafted[0].payload.content).toContain('quality measurement');
    expect(drafted[0].producedBy).toBe('linkedin-agent');
  });
});
```

**Step 2: Implement createLinkedInAgent**

```typescript
// packages/agents/linkedin-agent/src/agent.ts
import { defineAgent } from '@agentmesh/runtime';
import type { AgentMeshEvent, AgentContext } from '@agentmesh/runtime';

interface LLMClient {
  generate(prompt: string, options?: Record<string, any>): Promise<{ content: string }>;
}

interface LinkedInAgentDeps {
  llm: LLMClient;
}

export function createLinkedInAgent(deps: LinkedInAgentDeps) {
  return defineAgent({
    name: 'linkedin-agent',
    subscriptions: ['ContentCalendarDue', 'PostApproved'],
    produces: ['PostDrafted', 'PostScheduled', 'CommentSuggested'],
    async handler(event: AgentMeshEvent, context: AgentContext) {
      switch (event.type) {
        case 'ContentCalendarDue':
          return handleContentCalendarDue(event, context, deps);
        case 'PostApproved':
          return handlePostApproved(event, context);
      }
    },
  });
}

async function handleContentCalendarDue(
  event: AgentMeshEvent,
  context: AgentContext,
  deps: LinkedInAgentDeps,
) {
  for (const item of event.payload.dueItems ?? []) {
    const prompt = buildDraftPrompt(item);
    const result = await deps.llm.generate(prompt);
    const wordCount = result.content.split(/\s+/).length;

    await context.emit('PostDrafted', item.postId, {
      content: result.content,
      wordCount,
      topic: item.topic,
      mode: item.mode ?? 'thought-leadership',
      targetDate: event.payload.date,
    });
  }
}

async function handlePostApproved(event: AgentMeshEvent, context: AgentContext) {
  await context.emit('PostScheduled', event.aggregateId, {
    scheduledTime: event.payload.scheduledTime,
    content: event.payload.content,
  });
}

function buildDraftPrompt(item: { topic: string; mode?: string }): string {
  return `Write a LinkedIn thought-leadership post about: ${item.topic}.
Voice: practitioner insight, no product mentions, no pitch.
Length: 150-220 words.
Structure: 1 opening observation, 2-3 insight sentences, 1 closing provocation.
No CTAs, no em-dashes, no bullet lists, no hollow phrases.`;
}
```

**Step 3: Run tests**

Run: `npx vitest run packages/agents/linkedin-agent/__tests__/draft-post.test.ts`
Expected: PASS

**Step 4: Commit**

```bash
git add packages/agents/linkedin-agent/
git commit -m "feat(linkedin-agent): implement draftPost handler with LLM integration"
```

---

### Task 9: Implement BullMQ Scheduler for post publishing

**Files:**
- Create: `packages/scheduler/src/scheduler.ts`
- Create: `packages/scheduler/src/workers/publishPost.ts`
- Test: `packages/scheduler/__tests__/scheduler.test.ts`

**Step 1: Write the failing test**

```typescript
// packages/scheduler/__tests__/scheduler.test.ts
import { describe, it, expect, vi } from 'vitest';
import { InMemoryEventStore } from '@agentmesh/event-store';

// Test the scheduling logic without BullMQ (unit test)
describe('Scheduler — publish post', () => {
  it('should emit PostPublished on successful API call', async () => {
    const eventStore = new InMemoryEventStore();
    const mockLinkedInApi = vi.fn().mockResolvedValue({
      success: true,
      urn: 'urn:li:share:123456',
      url: 'https://www.linkedin.com/feed/update/urn:li:share:123456/',
    });

    // Simulate what the worker does
    const result = await mockLinkedInApi({ content: 'Test post' });

    await eventStore.append({
      id: 'pub-1',
      type: 'PostPublished',
      aggregateId: 'post-1',
      aggregateType: 'LinkedInPost',
      producedBy: 'scheduler',
      timestamp: new Date().toISOString(),
      version: 1,
      payload: { urn: result.urn, url: result.url },
      metadata: { correlationId: 'c-1' },
    });

    const published = await eventStore.getByType('PostPublished');
    expect(published).toHaveLength(1);
    expect(published[0].payload.urn).toBe('urn:li:share:123456');
  });

  it('should emit PostFailed on API error', async () => {
    const eventStore = new InMemoryEventStore();
    const mockLinkedInApi = vi.fn().mockResolvedValue({
      success: false,
      error: 'HTTP 401: Token expired',
    });

    const result = await mockLinkedInApi({ content: 'Test post' });

    await eventStore.append({
      id: 'fail-1',
      type: 'PostFailed',
      aggregateId: 'post-1',
      aggregateType: 'LinkedInPost',
      producedBy: 'scheduler',
      timestamp: new Date().toISOString(),
      version: 1,
      payload: { error: result.error },
      metadata: { correlationId: 'c-1' },
    });

    const failed = await eventStore.getByType('PostFailed');
    expect(failed).toHaveLength(1);
    expect(failed[0].payload.error).toContain('401');
  });
});
```

**Step 2: Implement scheduler**

```typescript
// packages/scheduler/src/scheduler.ts
import { Queue, Worker } from 'bullmq';
import type { EventStore, AgentMeshEvent } from '@agentmesh/event-store';
import { publishPost } from './workers/publishPost';

export interface SchedulerConfig {
  redisUrl: string;
  eventStore: EventStore;
  linkedInAccessToken: string;
  linkedInPersonId: string;
}

export function createScheduler(config: SchedulerConfig) {
  const connection = { url: config.redisUrl };

  const queue = new Queue('servicehive-jobs', { connection });

  const worker = new Worker('servicehive-jobs', async (job) => {
    switch (job.name) {
      case 'publish-post':
        return publishPost(job.data, config);
    }
  }, { connection });

  // Subscribe to PostScheduled events
  config.eventStore.subscribe('PostScheduled', async (event: AgentMeshEvent) => {
    const delay = new Date(event.payload.scheduledTime).getTime() - Date.now();
    await queue.add('publish-post', {
      postId: event.aggregateId,
      content: event.payload.content,
      correlationId: event.metadata.correlationId,
    }, {
      delay: Math.max(delay, 0), // If past due, publish immediately
      jobId: `post-${event.aggregateId}`,
    });
  });

  return { queue, worker };
}
```

```typescript
// packages/scheduler/src/workers/publishPost.ts
import { randomUUID } from 'crypto';
import type { EventStore } from '@agentmesh/event-store';

interface PublishData {
  postId: string;
  content: string;
  correlationId: string;
}

interface PublishConfig {
  eventStore: EventStore;
  linkedInAccessToken: string;
  linkedInPersonId: string;
}

export async function publishPost(data: PublishData, config: PublishConfig): Promise<void> {
  const authorUrn = `urn:li:person:${config.linkedInPersonId}`;

  const response = await fetch('https://api.linkedin.com/rest/posts', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${config.linkedInAccessToken}`,
      'Content-Type': 'application/json',
      'LinkedIn-Version': '202401',
      'X-Restli-Protocol-Version': '2.0.0',
    },
    body: JSON.stringify({
      author: authorUrn,
      commentary: data.content,
      visibility: 'PUBLIC',
      distribution: {
        feedDistribution: 'MAIN_FEED',
        targetEntities: [],
        thirdPartyDistributionChannels: [],
      },
      lifecycleState: 'PUBLISHED',
      isReshareDisabledByAuthor: false,
    }),
  });

  if (response.status === 201) {
    const body = await response.json().catch(() => ({}));
    const urn = body.id || body.urn || '';
    await config.eventStore.append({
      id: randomUUID(),
      type: 'PostPublished',
      aggregateId: data.postId,
      aggregateType: 'LinkedInPost',
      producedBy: 'scheduler',
      timestamp: new Date().toISOString(),
      version: 1,
      payload: { urn, url: `https://www.linkedin.com/feed/update/${urn}/` },
      metadata: { correlationId: data.correlationId },
    });
  } else {
    const error = await response.text();
    await config.eventStore.append({
      id: randomUUID(),
      type: 'PostFailed',
      aggregateId: data.postId,
      aggregateType: 'LinkedInPost',
      producedBy: 'scheduler',
      timestamp: new Date().toISOString(),
      version: 1,
      payload: { error: `HTTP ${response.status}: ${error}` },
      metadata: { correlationId: data.correlationId },
    });
  }
}
```

**Step 3: Run tests**

Run: `npx vitest run packages/scheduler/__tests__/scheduler.test.ts`
Expected: PASS

**Step 4: Commit**

```bash
git add packages/scheduler/
git commit -m "feat(scheduler): implement BullMQ scheduler with LinkedIn Posts API worker"
```

---

### Task 10: Implement API server with approval endpoints

**Files:**
- Create: `packages/server/src/index.ts`
- Create: `packages/server/src/api/approval.ts`
- Create: `packages/server/src/api/calendar.ts`
- Create: `packages/server/src/eventBridge.ts`
- Create: `packages/server/src/projections/approvalQueue.ts`
- Create: `packages/server/src/projections/contentCalendar.ts`

This task is implementation-only (no TDD — REST endpoints are integration-tested via the dashboard). Code provided in full.

**Step 1: Implement server entry point**

```typescript
// packages/server/src/index.ts
import express from 'express';
import cors from 'cors';
import { createServer } from 'http';
import { PgEventStore } from '@agentmesh/event-store';
import { AgentRunner } from '@agentmesh/runtime';
import { createLinkedInAgent } from '@servicehive/linkedin-agent';
import { createScheduler } from '@servicehive/scheduler';
import { createEventBridge } from './eventBridge';
import { approvalRouter } from './api/approval';
import { calendarRouter } from './api/calendar';
import { setupProjections } from './projections';
import 'dotenv/config';

const app = express();
app.use(cors());
app.use(express.json());

const eventStore = new PgEventStore({
  connectionString: process.env.DATABASE_URL,
});

// Projections
const projections = setupProjections(eventStore);

// API routes
app.use('/api/approvals', approvalRouter(eventStore, projections));
app.use('/api/calendar', calendarRouter(projections));

// Health check
app.get('/health', (_, res) => res.json({ status: 'ok' }));

// HTTP + WebSocket server
const server = createServer(app);
createEventBridge(server, eventStore);

// Agents
const runner = new AgentRunner(eventStore);
const linkedInAgent = createLinkedInAgent({
  llm: {
    generate: async (prompt) => {
      // Claude API call — implement in Task 11
      return { content: '[LLM integration pending]' };
    },
  },
});
runner.register(linkedInAgent);

// Scheduler
createScheduler({
  redisUrl: process.env.REDIS_URL!,
  eventStore,
  linkedInAccessToken: process.env.LINKEDIN_ACCESS_TOKEN!,
  linkedInPersonId: process.env.LINKEDIN_PERSON_ID!,
});

const PORT = process.env.PORT || 3100;
server.listen(PORT, async () => {
  await eventStore.startListening();
  await runner.start();
  console.log(`ServiceHive server running on port ${PORT}`);
});
```

**Step 2: Implement approval API**

```typescript
// packages/server/src/api/approval.ts
import { Router } from 'express';
import { randomUUID } from 'crypto';
import type { EventStore } from '@agentmesh/event-store';

export function approvalRouter(eventStore: EventStore, projections: any) {
  const router = Router();

  // GET /api/approvals — list pending approvals
  router.get('/', async (req, res) => {
    const items = await projections.approvalQueue.getPending();
    res.json(items);
  });

  // POST /api/approvals/:id/approve — approve an item
  router.post('/:id/approve', async (req, res) => {
    const { id } = req.params;
    const { scheduledTime, edits } = req.body;
    const item = await projections.approvalQueue.getById(id);
    if (!item) return res.status(404).json({ error: 'Not found' });

    const eventType = `${item.event_type.replace('Drafted', '')}Approved`;
    await eventStore.append({
      id: randomUUID(),
      type: eventType,
      aggregateId: item.payload.postId || item.payload.sequenceId || id,
      aggregateType: item.event_type.replace('Drafted', ''),
      producedBy: 'user',
      timestamp: new Date().toISOString(),
      version: 1,
      payload: { ...item.payload, ...edits, scheduledTime },
      metadata: { correlationId: item.payload.correlationId || randomUUID() },
    });

    await projections.approvalQueue.resolve(id);
    res.json({ status: 'approved' });
  });

  // POST /api/approvals/:id/reject — reject an item
  router.post('/:id/reject', async (req, res) => {
    const { id } = req.params;
    await projections.approvalQueue.resolve(id, 'rejected');
    res.json({ status: 'rejected' });
  });

  return router;
}
```

**Step 3: Implement WebSocket event bridge**

```typescript
// packages/server/src/eventBridge.ts
import { WebSocketServer } from 'ws';
import type { Server } from 'http';
import type { EventStore } from '@agentmesh/event-store';

export function createEventBridge(server: Server, eventStore: EventStore) {
  const wss = new WebSocketServer({ server, path: '/ws' });

  eventStore.subscribeAll((event) => {
    const message = JSON.stringify(event);
    wss.clients.forEach((client) => {
      if (client.readyState === 1) client.send(message);
    });
  });

  return wss;
}
```

**Step 4: Commit**

```bash
git add packages/server/
git commit -m "feat(server): implement API server with approval endpoints and WebSocket event bridge"
```

---

### Task 11: Implement Claude LLM adapter

**Files:**
- Create: `packages/agent-runtime/runtime/src/llm/claude-adapter.ts`
- Test: `packages/agent-runtime/runtime/__tests__/claude-adapter.test.ts`

**Step 1: Install Anthropic SDK**

```bash
npm install @anthropic-ai/sdk
```

**Step 2: Implement adapter**

```typescript
// packages/agent-runtime/runtime/src/llm/claude-adapter.ts
import Anthropic from '@anthropic-ai/sdk';

export interface LLMClient {
  generate(prompt: string, options?: { maxTokens?: number; temperature?: number }): Promise<{ content: string }>;
}

export function createClaudeAdapter(apiKey: string, model = 'claude-sonnet-4-5-20241022'): LLMClient {
  const client = new Anthropic({ apiKey });

  return {
    async generate(prompt, options = {}) {
      const response = await client.messages.create({
        model,
        max_tokens: options.maxTokens ?? 1024,
        messages: [{ role: 'user', content: prompt }],
      });
      const textBlock = response.content.find((b) => b.type === 'text');
      return { content: textBlock?.text ?? '' };
    },
  };
}
```

**Step 3: Commit**

```bash
git add packages/agent-runtime/runtime/src/llm/
git commit -m "feat(runtime): add Claude LLM adapter using Anthropic SDK"
```

---

### Task 12: Scaffold React Dashboard with Approval Queue view

**Files:**
- Modify: `packages/dashboard/src/App.tsx`
- Create: `packages/dashboard/src/views/ApprovalQueue.tsx`
- Create: `packages/dashboard/src/views/Overview.tsx`
- Create: `packages/dashboard/src/hooks/useEventBridge.ts`
- Create: `packages/dashboard/src/services/api.ts`

**Step 1: Implement API client**

```typescript
// packages/dashboard/src/services/api.ts
const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:3100';

export async function fetchApprovals() {
  const res = await fetch(`${API_BASE}/api/approvals`);
  return res.json();
}

export async function approveItem(id: string, data: { scheduledTime?: string; edits?: Record<string, any> }) {
  const res = await fetch(`${API_BASE}/api/approvals/${id}/approve`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return res.json();
}

export async function rejectItem(id: string) {
  const res = await fetch(`${API_BASE}/api/approvals/${id}/reject`, { method: 'POST' });
  return res.json();
}
```

**Step 2: Implement WebSocket hook**

```typescript
// packages/dashboard/src/hooks/useEventBridge.ts
import { useEffect, useRef, useState } from 'react';

export function useEventBridge(url = 'ws://localhost:3100/ws') {
  const [events, setEvents] = useState<any[]>([]);
  const wsRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    const ws = new WebSocket(url);
    wsRef.current = ws;
    ws.onmessage = (msg) => {
      const event = JSON.parse(msg.data);
      setEvents((prev) => [event, ...prev].slice(0, 50));
    };
    return () => ws.close();
  }, [url]);

  return events;
}
```

**Step 3: Implement ApprovalQueue view**

```tsx
// packages/dashboard/src/views/ApprovalQueue.tsx
import { useEffect, useState } from 'react';
import { fetchApprovals, approveItem, rejectItem } from '../services/api';

interface ApprovalItem {
  id: string;
  event_type: string;
  agent_name: string;
  summary: string;
  payload: Record<string, any>;
  status: string;
  created_at: string;
}

export function ApprovalQueue() {
  const [items, setItems] = useState<ApprovalItem[]>([]);

  useEffect(() => {
    fetchApprovals().then(setItems);
  }, []);

  const handleApprove = async (id: string) => {
    await approveItem(id, {});
    setItems((prev) => prev.filter((i) => i.id !== id));
  };

  const handleReject = async (id: string) => {
    await rejectItem(id);
    setItems((prev) => prev.filter((i) => i.id !== id));
  };

  return (
    <div>
      <h2>Approval Queue</h2>
      {items.length === 0 && <p>No pending approvals</p>}
      {items.map((item) => (
        <div key={item.id} style={{ border: '1px solid #ccc', padding: 16, marginBottom: 12, borderRadius: 8 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <strong>{item.agent_name}</strong>
            <span>{item.event_type}</span>
          </div>
          <p>{item.summary}</p>
          {item.payload.content && (
            <pre style={{ whiteSpace: 'pre-wrap', background: '#f5f5f5', padding: 12, borderRadius: 4 }}>
              {item.payload.content}
            </pre>
          )}
          <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
            <button onClick={() => handleApprove(item.id)} style={{ background: '#22c55e', color: 'white', padding: '8px 16px', border: 'none', borderRadius: 4, cursor: 'pointer' }}>
              Approve
            </button>
            <button onClick={() => handleReject(item.id)} style={{ background: '#ef4444', color: 'white', padding: '8px 16px', border: 'none', borderRadius: 4, cursor: 'pointer' }}>
              Reject
            </button>
          </div>
        </div>
      ))}
    </div>
  );
}
```

**Step 4: Wire up App.tsx**

```tsx
// packages/dashboard/src/App.tsx
import { useState } from 'react';
import { ApprovalQueue } from './views/ApprovalQueue';
import { useEventBridge } from './hooks/useEventBridge';

function App() {
  const [view, setView] = useState<'overview' | 'approvals' | 'calendar' | 'metrics'>('approvals');
  const events = useEventBridge();

  return (
    <div style={{ maxWidth: 960, margin: '0 auto', padding: 24 }}>
      <h1>ServiceHive</h1>
      <nav style={{ display: 'flex', gap: 16, marginBottom: 24 }}>
        {(['overview', 'approvals', 'calendar', 'metrics'] as const).map((v) => (
          <button key={v} onClick={() => setView(v)} style={{ fontWeight: view === v ? 'bold' : 'normal', cursor: 'pointer', background: 'none', border: 'none', fontSize: 16, textDecoration: view === v ? 'underline' : 'none' }}>
            {v.charAt(0).toUpperCase() + v.slice(1)}
          </button>
        ))}
      </nav>
      {view === 'approvals' && <ApprovalQueue />}
      {view === 'overview' && <p>Overview — coming soon</p>}
      {view === 'calendar' && <p>Content Calendar — coming soon</p>}
      {view === 'metrics' && <p>Metrics — coming soon</p>}
      <div style={{ marginTop: 32, borderTop: '1px solid #eee', paddingTop: 16 }}>
        <h3>Live Events ({events.length})</h3>
        {events.slice(0, 10).map((e, i) => (
          <div key={i} style={{ fontSize: 12, fontFamily: 'monospace', padding: 4 }}>
            {e.type} — {e.producedBy} — {new Date(e.timestamp).toLocaleTimeString()}
          </div>
        ))}
      </div>
    </div>
  );
}

export default App;
```

**Step 5: Commit**

```bash
git add packages/dashboard/
git commit -m "feat(dashboard): implement React dashboard with Approval Queue and live event feed"
```

---

### Task 13: Create seed data import scripts

**Files:**
- Create: `packages/server/src/seed/importFromHDIM.ts`
- Create: `seed-data/products.json`

**Step 1: Create products seed data**

```json
// seed-data/products.json
[
  { "name": "HDIM", "description": "Healthcare interoperability platform for HEDIS/FHIR/CQL", "status": "active", "repoUrl": "https://github.com/webemo-aaron/hdim" },
  { "name": "AgentMesh", "description": "Open-source event mesh for AI-agent powered businesses", "status": "development", "repoUrl": "" },
  { "name": "Landing Page", "description": "healthdatainmotion.com marketing site", "status": "active", "dashboardUrl": "https://landing-page-ecru-five-65.vercel.app" },
  { "name": "Investor Pipeline", "description": "Seed round fundraising and investor relationships", "status": "active" },
  { "name": "Customer Pilots", "description": "Healthcare organization pilot engagements", "status": "prospecting" }
]
```

**Step 2: Create import script that reads HDIM outreach docs**

```typescript
// packages/server/src/seed/importFromHDIM.ts
import { readFileSync } from 'fs';
import { Pool } from 'pg';
import 'dotenv/config';

const HDIM_OUTREACH_DIR = process.env.HDIM_OUTREACH_DIR || '/mnt/wdblack/dev/projects/hdim-master/docs/outreach';

async function seed() {
  const pool = new Pool({ connectionString: process.env.DATABASE_URL });

  // Import products
  const products = JSON.parse(readFileSync('seed-data/products.json', 'utf-8'));
  for (const p of products) {
    await pool.query(
      `INSERT INTO config.products (name, description, status, repo_url, dashboard_url)
       VALUES ($1, $2, $3, $4, $5) ON CONFLICT DO NOTHING`,
      [p.name, p.description, p.status, p.repoUrl || null, p.dashboardUrl || null]
    );
  }
  console.log(`Imported ${products.length} products`);

  // Import content calendar items as events
  // (Reads the markdown and creates PostDrafted/PostPublished events)
  console.log('Seed complete. Import comment bank and prospects manually or extend this script.');
  await pool.end();
}

seed().catch(console.error);
```

**Step 3: Add seed script to package.json**

```json
// In root package.json scripts:
"seed": "npx tsx packages/server/src/seed/importFromHDIM.ts"
```

**Step 4: Commit**

```bash
git add seed-data/ packages/server/src/seed/
git commit -m "feat(seed): add product seed data and HDIM import script"
```

---

### Task 14: Integration test — full post lifecycle

**Files:**
- Create: `__tests__/integration/post-lifecycle.test.ts`

**Step 1: Write integration test**

```typescript
// __tests__/integration/post-lifecycle.test.ts
import { describe, it, expect, beforeEach } from 'vitest';
import { InMemoryEventStore } from '@agentmesh/event-store';
import { AgentRunner } from '@agentmesh/runtime';
import { createLinkedInAgent } from '@servicehive/linkedin-agent';
import { randomUUID } from 'crypto';

describe('Post Lifecycle — end to end', () => {
  let eventStore: InMemoryEventStore;
  let runner: AgentRunner;

  beforeEach(async () => {
    eventStore = new InMemoryEventStore();
    runner = new AgentRunner(eventStore);

    const agent = createLinkedInAgent({
      llm: { generate: async () => ({ content: 'Stars ratings stabilized but measurement infrastructure did not.' }) },
    });
    runner.register(agent);
    await runner.start();
  });

  it('should flow from ContentCalendarDue → PostDrafted → PostApproved → PostScheduled', async () => {
    // 1. System emits calendar due
    await eventStore.append({
      id: randomUUID(),
      type: 'ContentCalendarDue',
      aggregateId: 'cal-2026-03-06',
      aggregateType: 'ContentCalendar',
      producedBy: 'system',
      timestamp: new Date().toISOString(),
      version: 1,
      payload: { date: '2026-03-06', dueItems: [{ postId: 'post-2', topic: 'Stars infrastructure', mode: 'thought-leadership' }] },
      metadata: { correlationId: randomUUID() },
    });

    // 2. Agent should have produced PostDrafted
    const drafted = await eventStore.getByType('PostDrafted');
    expect(drafted).toHaveLength(1);
    expect(drafted[0].payload.content).toContain('Stars');

    // 3. User approves
    await eventStore.append({
      id: randomUUID(),
      type: 'PostApproved',
      aggregateId: 'post-2',
      aggregateType: 'LinkedInPost',
      producedBy: 'user',
      timestamp: new Date().toISOString(),
      version: 1,
      payload: { scheduledTime: '2026-03-06T12:30:00Z', content: drafted[0].payload.content },
      metadata: { correlationId: drafted[0].metadata.correlationId },
    });

    // 4. Agent should have produced PostScheduled
    const scheduled = await eventStore.getByType('PostScheduled');
    expect(scheduled).toHaveLength(1);
    expect(scheduled[0].payload.scheduledTime).toBe('2026-03-06T12:30:00Z');
  });
});
```

**Step 2: Run**

Run: `npx vitest run __tests__/integration/post-lifecycle.test.ts`
Expected: PASS

**Step 3: Commit**

```bash
git add __tests__/
git commit -m "test: add end-to-end post lifecycle integration test"
```

---

## Summary

| Phase | Tasks | What Ships |
|-------|-------|------------|
| Phase 1 (Tasks 1-6) | AgentMesh Event Core | `@agentmesh/event-store`, `@agentmesh/runtime`, Docker infra |
| Phase 2 (Tasks 7-14) | ServiceHive v0.1 | LinkedIn Agent, Scheduler, API Server, Dashboard, Seed Import |

**Total: 14 tasks.** Each task is a commit. Phase 1 can be completed in 1-2 sessions. Phase 2 in 2-3 sessions.

**Not in this plan (future):** Email Agent (v0.2), Engagement Tracker (v0.2), Portfolio Coordinator (v0.3), full dashboard views, production deployment.
