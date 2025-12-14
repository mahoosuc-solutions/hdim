# AI Agent Platform - Performance Optimization Documentation

**Comprehensive performance optimization strategy for the HDIM healthcare platform's AI Agent services**

---

## Overview

This directory contains performance optimization documentation for:
- **Agent Builder Service** (port 8096) - No-code agent configuration and management
- **Agent Runtime Service** (port 8088) - Agent execution and LLM orchestration

**Target:** Achieve sub-3-second P95 latency for all agent operations while supporting 500+ concurrent users per service.

---

## Documents

### 1. [optimization-plan-agent-platform.md](./optimization-plan-agent-platform.md)
**Main optimization roadmap** - 33KB comprehensive plan

**Contents:**
- Critical optimizations (do this week)
  - GIN indexes for JSONB columns
  - N+1 query fixes
  - Redis caching implementation
  - Snapshot serialization optimization
- High priority optimizations (next sprint)
  - Async processing
  - Connection pool tuning
  - Cache warming
  - Additional indexes
- Medium priority improvements
  - Feign client optimization
  - Pagination improvements
  - Response compression
  - Redis memory optimization
- Performance testing strategy
- Rollback procedures
- Monitoring setup
- 3-week implementation timeline

**When to use:** Complete reference for all optimizations, detailed analysis, and implementation strategies.

---

### 2. [QUICK_START_OPTIMIZATION.md](./QUICK_START_OPTIMIZATION.md)
**Quick implementation guide** - 8.4KB rapid reference

**Contents:**
- Critical path optimizations only
- Copy-paste code snippets
- Verification commands
- Expected results table
- Rollback instructions

**When to use:** For immediate implementation of critical fixes. Start here if you need to deploy optimizations quickly.

---

### 3. [METRICS_TRACKING.md](./METRICS_TRACKING.md)
**Performance metrics tracking template** - 9.2KB measurement framework

**Contents:**
- Baseline metrics collection
- Post-optimization tracking tables
- Week-by-week progress monitoring
- Success criteria checklist
- Alerting thresholds
- Grafana dashboard queries
- Metric collection scripts

**When to use:** Before starting optimizations (baseline), during implementation (tracking), and ongoing (monitoring).

---

## Quick Start (5 Minutes)

If you're new to this optimization effort, follow these steps:

### Step 1: Understand Current State
```bash
# Run baseline metrics
cd backend/docs/performance
psql -d healthdata_db -c "SELECT mean_exec_time FROM pg_stat_statements WHERE query LIKE '%agent_configurations%' ORDER BY mean_exec_time DESC LIMIT 5;"

# Record results in METRICS_TRACKING.md
```

### Step 2: Implement Critical Fixes
```bash
# Follow QUICK_START_OPTIMIZATION.md
# Implement in this order:
1. Database indexes (30 min)
2. Fix N+1 queries (15 min)
3. Add Redis caching (1 hour)
4. Connection pool tuning (5 min)
```

### Step 3: Validate Improvements
```bash
# Run load test
k6 run load-test.js

# Compare against baseline in METRICS_TRACKING.md
```

---

## Key Findings Summary

### Critical Performance Issues Identified

1. **JSONB Query Performance** (CRITICAL)
   - No GIN indexes on JSONB columns
   - Full table scans on tool_configuration, guardrail_configuration
   - Impact: 500ms query time → Target: 25ms (95% improvement)

2. **N+1 Query Pattern** (CRITICAL)
   - AgentVersion entity lazy-loads AgentConfiguration
   - 21 queries for version history with 20 versions
   - Impact: 800ms → Target: 50ms (94% improvement)

3. **Missing Cache Layer** (HIGH)
   - Prompt templates queried repeatedly from DB
   - Active agents list not cached
   - Impact: 200-300ms per lookup → Target: 5-10ms (97% improvement)

4. **Connection Pool Undersized** (HIGH)
   - Agent Builder: max-pool-size=10 (too small)
   - Agent Runtime: max-pool-size=20 (needs tuning)
   - Impact: Connection starvation at >100 concurrent users

5. **Synchronous Operations** (MEDIUM)
   - Agent testing blocks HTTP threads for 60+ seconds
   - No async processing for long-running operations
   - Impact: Thread pool exhaustion under load

---

## Implementation Priority

### Week 1: Critical (Do Immediately)
```
Priority: CRITICAL
Impact: 90%+ of performance gains
Effort: 8 hours
Risk: Low

Tasks:
✓ Add GIN indexes for JSONB
✓ Fix N+1 query patterns
✓ Implement Redis caching
✓ Tune connection pools
```

### Week 2: High (Next Sprint)
```
Priority: HIGH
Impact: Scalability and concurrency
Effort: 13 hours
Risk: Medium (requires testing)

Tasks:
✓ Async processing
✓ Cache warming
✓ Additional indexes
✓ Load testing
```

### Week 3: Medium (Nice to Have)
```
Priority: MEDIUM
Impact: Incremental improvements
Effort: 7 hours
Risk: Low

Tasks:
✓ Feign optimization
✓ Pagination improvements
✓ Response compression
```

---

## Expected Results

### Performance Improvements

| Operation | Current P95 | Target P95 | Expected Improvement |
|-----------|-------------|------------|---------------------|
| List agents | 500ms | 50ms | 90% faster |
| Get agent config | 300ms | 25ms | 92% faster |
| Create agent | 800ms | 200ms | 75% faster |
| Update agent | 600ms | 150ms | 75% faster |
| Version history | 800ms | 50ms | 94% faster |
| Template lookup | 200ms | 10ms | 95% faster |
| Test message | 5000ms | 2000ms | 60% faster |
| Agent execution | 8000ms | 3000ms | 63% faster |

### Scalability Improvements

| Metric | Current | Target | Improvement |
|--------|---------|--------|-------------|
| Concurrent users | 100 | 500 | 5x capacity |
| Requests/second | 50 | 250 | 5x throughput |
| Database connections | 10 | 30 | 3x capacity |
| Cache hit rate | 0% | 80%+ | New capability |

---

## Architecture Context

### Agent Builder Service (8096)
```
Components:
- AgentConfigurationService: Agent CRUD operations
- AgentVersionService: Version history and rollback
- PromptTemplateService: Template management
- AgentTestService: Testing sandbox

Database:
- agent_configurations (JSONB: tool_config, guardrail_config)
- agent_versions (JSONB: configuration_snapshot)
- prompt_templates (JSONB: variables)
- agent_test_sessions (JSONB: messages, metrics)

Key Bottlenecks:
- JSONB queries without GIN indexes
- N+1 queries in version retrieval
- No caching layer
```

### Agent Runtime Service (8088)
```
Components:
- AgentOrchestrator: Agent execution loop
- LLMProviderFactory: LLM provider selection
- ToolRegistry: Tool management
- RedisConversationMemory: Session memory

Infrastructure:
- Redis: Conversation memory (15 min TTL)
- PostgreSQL: Task execution tracking
- Feign Client: Inter-service communication

Key Bottlenecks:
- Synchronous LLM calls (120s timeout)
- Connection pool sizing
- Redis memory management
```

---

## Monitoring & Alerts

### Critical Metrics to Track

1. **API Latency**
   - P95 latency < 3s (target)
   - P99 latency < 5s (threshold)

2. **Database Performance**
   - Query time < 100ms (95% of queries)
   - Connection pool usage < 80%
   - Cache hit ratio > 80%

3. **Redis Performance**
   - Cache hit rate > 80%
   - Memory usage < 80%
   - GET latency < 5ms

4. **Application Health**
   - Error rate < 1%
   - Thread pool queue depth < 50
   - GC pause time < 100ms

### Prometheus Queries

```promql
# API P95 latency
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Database connection pool usage
hikari_connections_active / hikari_connections_max

# Cache hit rate
rate(cache_gets_hits_total[5m]) / rate(cache_gets_total[5m])

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])
```

---

## Code References

### Key Files to Modify

**Agent Builder Service:**
```
modules/services/agent-builder-service/
├── src/main/java/com/healthdata/agentbuilder/
│   ├── repository/
│   │   ├── AgentConfigurationRepository.java  (add fetch joins)
│   │   ├── AgentVersionRepository.java        (fix N+1 queries)
│   │   └── PromptTemplateRepository.java      (add caching)
│   ├── service/
│   │   ├── AgentConfigurationService.java     (add @Cacheable)
│   │   ├── AgentVersionService.java           (optimize queries)
│   │   └── PromptTemplateService.java         (add @Cacheable)
│   └── config/
│       └── CacheConfig.java                   (NEW - Redis cache)
└── src/main/resources/
    ├── application.yml                         (tune connection pool)
    └── db/migration/
        └── V2__add_jsonb_indexes.sql          (NEW - GIN indexes)
```

**Agent Runtime Service:**
```
modules/services/agent-runtime-service/
├── src/main/java/com/healthdata/agent/
│   ├── core/AgentOrchestrator.java            (async processing)
│   ├── memory/RedisConversationMemory.java    (optimize serialization)
│   └── config/AsyncConfig.java                (NEW - async executors)
└── src/main/resources/
    └── application.yml                         (tune pools, Redis)
```

---

## Support & Questions

**For implementation help:**
- Review QUICK_START_OPTIMIZATION.md for code snippets
- Check optimization-plan-agent-platform.md for detailed explanations
- Use METRICS_TRACKING.md to validate improvements

**For performance issues:**
- Collect metrics using scripts in METRICS_TRACKING.md
- Check Grafana dashboards
- Review slow query log in PostgreSQL
- Inspect Redis INFO output

**For rollback assistance:**
- Each optimization has rollback steps in optimization-plan-agent-platform.md
- Database migrations can be reverted with Flyway
- Application changes can be rolled back via git/kubectl

---

## Change Log

| Date | Version | Changes | Author |
|------|---------|---------|--------|
| 2025-12-06 | 1.0 | Initial optimization plan created | Backend Team |
| | | Identified critical performance bottlenecks | |
| | | Created implementation roadmap | |
| | | Established success criteria | |

---

## Next Review

**Scheduled:** After Week 1 implementation (2025-12-13)

**Agenda:**
- Review Week 1 optimization results
- Validate performance improvements
- Adjust Week 2 priorities if needed
- Update metrics and documentation

---

**Document Owner:** Backend Engineering Team
**Status:** Active Development
**Phase:** Implementation (Week 1)
