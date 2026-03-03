# HDIM Healthix HIE Deployment — 16M Patient Scale Guide

> **Customer:** Healthix (NYC metro, largest US public HIE)
> **Scale:** 16 million patients, 500+ participating organizations
> **Related docs:** [RHEL 7 Deployment Guide](./RHEL7-DEPLOYMENT-GUIDE.md) | [Architecture Reference](./RHEL7-ARCHITECTURE.md)

---

## Table of Contents

1. [Scale Context](#1-scale-context)
2. [Hardware Requirements](#2-hardware-requirements)
3. [What Changes at Healthix Scale](#3-what-changes-at-healthix-scale)
4. [Installation](#4-installation)
5. [Configuration Deep Dive](#5-configuration-deep-dive)
6. [Capacity Planning](#6-capacity-planning)
7. [Performance Tuning](#7-performance-tuning)
8. [Monitoring at Scale](#8-monitoring-at-scale)

---

## 1. Scale Context

### Healthix by the Numbers

| Metric | Value | HDIM Impact |
|--------|-------|-------------|
| Patient records | 16,000,000 | Database sizing, index strategy |
| Participating organizations | 500+ | Multi-tenancy, connection pooling |
| Concurrent clinical users (peak) | ~5,000 | Gateway threads, session cache |
| HEDIS measures evaluated | 50+ per patient | CQL engine CPU, memory |
| Care gaps generated | ~50M-100M annually | Care gap service, Kafka throughput |
| Audit events (HIPAA) | ~500K/hour peak | Kafka partitions, audit DB writes |
| API requests | ~5,000/minute peak | Gateway capacity, rate limiting |

### Why the Default Config Won't Work

The default `docker-compose.resources.yml` is sized for a pilot (~50K patients):

| Resource | Default (Pilot) | Healthix Scale | Multiplier |
|----------|----------------|----------------|------------|
| PostgreSQL shared_buffers | 256 MB | 8 GB | 32x |
| PostgreSQL max_connections | 300 | 500 (+ PgBouncer 2000) | ~7x effective |
| Redis maxmemory | 384 MB | 6 GB | 16x |
| Kafka heap | 768 MB | 3 GB | 4x |
| CQL Engine JVM heap | 512 MB | 6 GB | 12x |
| Patient Service JVM heap | 384 MB | 3 GB | 8x |
| FHIR Service JVM heap | 512 MB | 3 GB | 6x |
| Hikari pool (per service) | 10 connections | 50 connections | 5x |
| Gateway Tomcat threads | default (200) | 400 | 2x |
| Total RAM | 16 GB | 128 GB | 8x |

---

## 2. Hardware Requirements

### Single-Server Deployment (Minimum)

| Resource | Requirement | Notes |
|----------|-------------|-------|
| **CPU** | 32 cores (64 threads) | Intel Xeon Gold or AMD EPYC |
| **RAM** | 128 GB DDR4 ECC | 64 GB minimum with reduced cache |
| **Boot disk** | 500 GB SSD | OS + Docker images |
| **Data disk** | 2 TB NVMe SSD | PostgreSQL data, WAL, Kafka logs |
| **Network** | 10 Gbps | Internal Docker bridge throughput |
| **RAID** | RAID-10 recommended | Data protection for NVMe array |

### Multi-Server Deployment (Recommended for Production)

| Server | CPU | RAM | Disk | Purpose |
|--------|-----|-----|------|---------|
| **App Server 1** | 16 cores | 64 GB | 500 GB SSD | Services (gateways, patient, FHIR, care-gap) |
| **App Server 2** | 16 cores | 64 GB | 500 GB SSD | Services (CQL, quality, audit, analytics, AI) |
| **DB Server** | 16 cores | 64 GB | 2 TB NVMe | PostgreSQL primary + PgBouncer |
| **DB Replica** | 8 cores | 32 GB | 2 TB NVMe | PostgreSQL read replica |
| **Cache/Broker** | 8 cores | 32 GB | 500 GB SSD | Redis Sentinel + Kafka |

For multi-server deployment, use the Kubernetes manifests in `k8s/` instead of Docker Compose.

### Kernel Tuning (RHEL 7)

Apply before installation — add to `/etc/sysctl.conf`:

```bash
# PostgreSQL shared memory (8 GB shared_buffers)
kernel.shmmax = 8589934592
kernel.shmall = 2097152

# Network tuning for high connection count
net.core.somaxconn = 65535
net.ipv4.tcp_max_syn_backlog = 65535
net.core.netdev_max_backlog = 65535

# File descriptors
fs.file-max = 1000000

# Virtual memory (PostgreSQL benefits)
vm.swappiness = 1
vm.dirty_ratio = 10
vm.dirty_background_ratio = 3
vm.overcommit_memory = 2
vm.overcommit_ratio = 90

# Transparent Huge Pages (disable for PostgreSQL)
# Also add to /etc/rc.local:
# echo never > /sys/kernel/mm/transparent_hugepage/enabled
```

Apply and persist:
```bash
sysctl -p
echo never > /sys/kernel/mm/transparent_hugepage/enabled
```

---

## 3. What Changes at Healthix Scale

### Compose File Layering

The Healthix profile automatically layers these compose files:

```bash
docker compose \
  -f docker-compose.yml              # Base services
  -f docker-compose.prod.yml         # Security hardening (127.0.0.1 binds)
  -f docker-compose.resources.yml    # Base resource limits
  -f docker-compose.ha.yml           # PostgreSQL primary-replica
  -f docker-compose.redis-ha.yml     # Redis Sentinel (master + 2 replicas + 3 sentinels)
  -f docker-compose.healthix.yml     # Healthix-scale overrides (THIS FILE)
  --profile full up -d
```

### docker-compose.healthix.yml Overview

This overlay overrides resource limits from `docker-compose.resources.yml`:

**Infrastructure tier:**
- PostgreSQL: 8 GB shared_buffers, 16 GB container, 500 connections, parallel query workers
- PgBouncer: 2000 client connections pooled to 100 server connections
- Redis: 6 GB maxmemory, active defragmentation
- Kafka: 3 GB heap, 12 default partitions, 7-day retention

**Hot-path services (handle most queries):**
- Patient, FHIR, care-gap, quality-measure: 3 GB JVM heap, 50 Hikari connections
- CQL engine: 6 GB JVM heap, 128-thread evaluation pool (HEDIS bulk runs)
- Audit service: 3 GB heap, 100 Hikari connections (HIPAA — every API call)

**Gateway tier (all inbound traffic):**
- 400 Tomcat threads, 20K max connections, 5K requests/minute rate limit

---

## 4. Installation

### Quick Start

```bash
# 1. Apply kernel tuning (see section 2)
sudo sysctl -p

# 2. Run installer with healthix profile
sudo ./installer/rhel7/hdim-install.sh install --profile healthix

# 3. Verify
sudo ./installer/rhel7/hdim-install.sh status
```

The installer handles:
- Pre-flight validation (128 GB RAM, 2 TB disk, 32+ cores)
- Layering HA + Redis Sentinel + Healthix compose files
- Building all 51+ services
- Starting with scaled resource limits
- Health verification

### Post-Install: Verify Scaled Configuration

```bash
# Verify PostgreSQL shared_buffers
docker exec $(docker ps -q --filter 'name=postgres') \
  psql -U healthdata -c "SHOW shared_buffers;"
# Expected: 8GB

# Verify Redis maxmemory
docker exec $(docker ps -q --filter 'name=redis') \
  redis-cli INFO memory | grep maxmemory_human
# Expected: 6.00G

# Verify CQL engine heap
docker exec $(docker ps -q --filter 'name=cql-engine') \
  java -XX:+PrintFlagsFinal -version 2>&1 | grep MaxHeapSize
# Expected: ~6GB

# Verify PgBouncer pool size
docker exec $(docker ps -q --filter 'name=pgbouncer') \
  psql -p 6432 -U healthdata pgbouncer -c "SHOW POOLS;"
```

---

## 5. Configuration Deep Dive

### PostgreSQL at 16M Patients

| Parameter | Value | Rationale |
|-----------|-------|-----------|
| `shared_buffers` | 8 GB | ~50% of 16 GB container RAM (PostgreSQL recommendation) |
| `effective_cache_size` | 24 GB | Informs query planner about OS cache (container + host) |
| `work_mem` | 64 MB | Per-sort/hash operation — allows in-memory sorts for 16M patient queries |
| `maintenance_work_mem` | 1 GB | VACUUM, CREATE INDEX on large tables |
| `max_connections` | 500 | Server-side (PgBouncer handles 2000 client-side) |
| `max_parallel_workers` | 16 | Parallel query execution for large scans |
| `max_wal_size` | 8 GB | Prevents excessive checkpointing during bulk loads |
| `default_statistics_target` | 500 | Better query plans for skewed distributions |
| `shm_size` | 4 GB | Docker shared memory for PostgreSQL semaphores |
| `timezone` | America/New_York | Healthix operates in Eastern Time |

### Why PgBouncer is Critical

Without PgBouncer, 50+ services × 50 connections = 2,500 server connections — PostgreSQL can't handle that efficiently. PgBouncer in `transaction` mode:
- Accepts 2,000 client connections
- Multiplexes to 100 server connections
- Returns connections to pool between transactions
- Reduces PostgreSQL process overhead by ~20x

### CQL Engine Sizing

The CQL engine evaluates HEDIS measures. At 16M patients × 50+ measures:
- **Bulk evaluation:** ~800M measure-patient combinations
- **Thread pool:** 128 threads for parallel evaluation
- **6 GB heap:** CQL evaluation loads patient bundles into memory
- **G1GC with 32 MB regions:** Optimized for large heap with short pauses

### Kafka Topic Strategy

| Topic Category | Partitions | Retention | Volume |
|----------------|------------|-----------|--------|
| Patient events | 12 | 7 days | ~50K/hour |
| Care gap events | 12 | 7 days | ~50K/hour |
| Audit events | 12 | 30 days | ~500K/hour |
| Evaluation results | 12 | 3 days | ~100K/hour |

---

## 6. Capacity Planning

### Database Growth Projections

| Data Type | Size per Patient | 16M Patients | Annual Growth |
|-----------|-----------------|--------------|---------------|
| Patient demographics | ~400 bytes | ~6 GB | ~5% |
| Clinical observations | ~2 KB | ~32 GB | ~20% |
| Care gaps | ~500 bytes/gap × 5 | ~40 GB | ~30% |
| Audit events | ~200 bytes/event | ~100 GB/year | ~100 GB/year |
| HEDIS results | ~1 KB/result | ~80 GB | ~50% |
| Indexes | ~30% of data | ~80 GB | proportional |
| WAL/temp | ~20 GB | ~20 GB | stable |
| **Total Year 1** | | **~360 GB** | |
| **Total Year 3** | | **~800 GB** | |

### When to Consider Kubernetes

Move from Docker Compose to Kubernetes when:

| Signal | Threshold | Action |
|--------|-----------|--------|
| Patient count | > 25M | Horizontal pod scaling |
| Concurrent users | > 10K | Service mesh, auto-scaling |
| Database size | > 1 TB | Citus/sharding, dedicated DB servers |
| Availability SLA | > 99.9% | Multi-node K8s, PDB, pod anti-affinity |
| API latency P99 | > 2s | Service replicas, read replicas |

---

## 7. Performance Tuning

### HEDIS Season Preparation (January-March)

During HEDIS reporting season, CQL evaluation load peaks. Tune for throughput:

```bash
# Increase CQL engine evaluation threads
docker compose exec cql-engine-service sh -c \
  'export MEASURE_EVALUATION_MAX_POOL_SIZE=256 && kill -USR1 1'

# Or restart with updated env
# In .env, add:
MEASURE_EVALUATION_CORE_POOL_SIZE=64
MEASURE_EVALUATION_MAX_POOL_SIZE=256
MEASURE_EVALUATION_QUEUE_CAPACITY=5000
```

### Batch Data Ingestion

When loading 16M patient records initially:

```bash
# Increase PostgreSQL work_mem for bulk operations
docker exec $(docker ps -q --filter 'name=postgres') \
  psql -U healthdata -c "ALTER SYSTEM SET work_mem = '256MB';"
docker exec $(docker ps -q --filter 'name=postgres') \
  psql -U healthdata -c "SELECT pg_reload_conf();"

# Reset after ingestion
docker exec $(docker ps -q --filter 'name=postgres') \
  psql -U healthdata -c "ALTER SYSTEM RESET work_mem;"
docker exec $(docker ps -q --filter 'name=postgres') \
  psql -U healthdata -c "SELECT pg_reload_conf();"
```

### Query Performance Monitoring

```bash
# Enable pg_stat_statements for slow query analysis
docker exec $(docker ps -q --filter 'name=postgres') \
  psql -U healthdata -c "CREATE EXTENSION IF NOT EXISTS pg_stat_statements;"

# Top 10 slowest queries
docker exec $(docker ps -q --filter 'name=postgres') \
  psql -U healthdata -c "
    SELECT query, calls, mean_exec_time::numeric(10,2) as avg_ms,
           total_exec_time::numeric(10,2) as total_ms
    FROM pg_stat_statements
    ORDER BY mean_exec_time DESC
    LIMIT 10;"
```

---

## 8. Monitoring at Scale

### Key Metrics to Watch

| Metric | Warning | Critical | Where |
|--------|---------|----------|-------|
| PostgreSQL connections | > 400 | > 480 | Grafana → PostgreSQL dashboard |
| PgBouncer wait time | > 100ms | > 500ms | `SHOW POOLS` |
| Redis memory usage | > 5 GB | > 5.5 GB | Grafana → Redis dashboard |
| Kafka consumer lag | > 10K | > 100K | Grafana → Kafka dashboard |
| CQL evaluation queue | > 1000 | > 1800 | Service metrics |
| JVM heap usage | > 80% | > 90% | Grafana → JVM dashboard |
| API P99 latency | > 1s | > 3s | Grafana → Gateway dashboard |
| Disk usage (/opt) | > 70% | > 85% | Host monitoring |

### Grafana Dashboards

Access at `http://<server>:3001` (credentials in `.env`):

1. **HDIM Overview** — Service health, request rates, error rates
2. **PostgreSQL** — Connections, query rates, replication lag, cache hit ratio
3. **Redis** — Memory, hit/miss ratio, connected clients
4. **Kafka** — Broker throughput, consumer lag, partition distribution
5. **JVM** — Heap usage, GC pauses, thread counts (per service)

### Alerting (Recommended)

Configure Grafana alerts for:
- PostgreSQL replication lag > 30 seconds
- Any service unhealthy for > 2 minutes
- Disk usage > 80%
- API error rate > 1%
- Kafka consumer lag growing for > 5 minutes

---

## Resource Allocation Summary

### Total Server Resources (Single-Server Healthix)

| Component | CPU | RAM | Notes |
|-----------|-----|-----|-------|
| **PostgreSQL primary** | 8 cores | 16 GB | + 4 GB shm_size |
| **PostgreSQL replica** | 4 cores | 8 GB | Read queries |
| **PgBouncer** | 2 cores | 1 GB | Connection pooling |
| **Redis master** | 4 cores | 8 GB | 6 GB maxmemory |
| **Redis replicas (2)** | 2 cores ea | 4 GB ea | Read replicas |
| **Redis sentinels (3)** | 0.5 cores ea | 256 MB ea | Failover |
| **Kafka** | 4 cores | 4 GB | 3 GB heap |
| **Zookeeper** | 2 cores | 1 GB | Kafka coordination |
| **CQL engine** | 8 cores | 8 GB | 6 GB heap |
| **Patient + FHIR + care-gap + quality** | 4 cores ea | 4 GB ea | 3 GB heap each |
| **Gateways (4)** | 2-4 cores ea | 1-2 GB ea | 400 threads each |
| **Audit + Auth** | 4+2 cores | 4+2 GB | Heavy write path |
| **Analytics + AI (3)** | 4 cores ea | 4 GB ea | 3 GB heap each |
| **Observability** | 5 cores | 7 GB | Jaeger, Prometheus, Grafana |
| **OS + Docker overhead** | 4 cores | 8 GB | |
| **TOTAL** | ~80 cores | ~120 GB | Fits 128 GB server |

*Note: CPU overcommit is expected and acceptable. RAM must not overcommit — all reservations must fit in physical RAM.*
