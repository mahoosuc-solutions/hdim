# HDIM Resource Limits Configuration

## Overview

Resource limits have been added to prevent services from consuming excessive memory and causing system instability. All services now have defined memory caps and JVM tuning for optimal performance.

**Implementation:** `docker-compose.resources.yml` (overlay file)

**Auto-applied by:** `docker-quick.sh` and `docker-start.sh`

---

## Resource Allocation Summary

### Infrastructure Services

| Service | Memory Limit | Reserved | CPU Limit | Notes |
|---------|-------------|----------|-----------|-------|
| PostgreSQL | 2GB | 512MB | 2.0 | Optimized for 300 connections |
| Redis | 512MB | 128MB | 1.0 | LRU eviction at 384MB |
| Kafka | 1GB | 512MB | 1.0 | Heap: 768MB max |
| Zookeeper | 512MB | 256MB | 0.5 | Kafka coordinator |
| Jaeger | 256MB | 64MB | 0.5 | Distributed tracing |

**Total Infrastructure:** 4.25GB max

---

### Core Java Services (High Load)

| Service | Memory Limit | Reserved | JVM Heap | CPU | Notes |
|---------|-------------|----------|----------|-----|-------|
| FHIR Service | 768MB | 256MB | 256-512MB | 2.0 | FHIR R4 resources |
| Quality Measure | 768MB | 384MB | 384-512MB | 2.0 | CQL evaluation (CPU-intensive) |
| CQL Engine | 768MB | 384MB | 384-512MB | 2.0 | Clinical query language |

**Total Core (High):** 2.3GB max

---

### Core Services (Medium Load)

| Service | Memory Limit | Reserved | JVM Heap | CPU |
|---------|-------------|----------|----------|-----|
| Patient Service | 512MB | 256MB | 256-384MB | 1.0 |
| Care Gap Service | 512MB | 256MB | 256-384MB | 1.0 |
| Gateway Admin | 512MB | 256MB | 256-384MB | 1.0 |
| Gateway Patient | 512MB | 256MB | 256-384MB | 1.0 |
| Gateway Provider | 512MB | 256MB | 256-384MB | 1.0 |

**Total Core (Medium):** 2.5GB max

---

### Lightweight Services

| Service | Memory Limit | Reserved | JVM Heap | CPU |
|---------|-------------|----------|----------|-----|
| Consent Service | 384MB | 128MB | 128-256MB | 0.5 |
| Attribution Service | 384MB | 128MB | 128-256MB | 0.5 |
| Value Set Service | 384MB | 128MB | 128-256MB | 0.5 |
| Measure Repository | 384MB | 128MB | 128-256MB | 0.5 |

**Total Lightweight:** 1.5GB max

---

### Support Services

| Service | Memory Limit | Reserved | CPU | Notes |
|---------|-------------|----------|-----|-------|
| Demo Seeding | 512MB | 256MB | 1.0 | Only in demo profile |
| Clinical Portal | 256MB | 64MB | 0.5 | Angular frontend |
| Postgres Backup | 256MB | 64MB | 0.5 | Scheduled backups |

---

## Profile Memory Budgets

### Light Profile
```
PostgreSQL:     2GB limit (512MB reserved)
Redis:        512MB limit (128MB reserved)
────────────────────────────────────────
Total:       2.5GB max (640MB reserved)
Actual:      ~100MB typical usage
```

### Core Profile
```
Infrastructure:  4.25GB
Core Services:   4.8GB (high + medium)
Support:         0.5GB
────────────────────────────────────────
Total:          9.5GB max
Typical:        ~6GB active usage
```

### Demo Profile
```
Core Profile:    9.5GB
Demo Seeding:    0.5GB
────────────────────────────────────────
Total:          10GB max
Typical:        ~6.5GB active usage
```

### Full Profile
```
Core Profile:      9.5GB
AI Services:       2GB
Analytics:         2GB
Remaining:         3GB
────────────────────────────────────────
Total:            16.5GB max
Typical:          ~12GB active usage
```

---

## JVM Configuration Applied

### Standard JVM Options (All Services)
```
-XX:+UseContainerSupport          # Detect container memory limits
-XX:MaxRAMPercentage=75.0         # Use 75% of container memory
-XX:+UseG1GC                      # Low-latency garbage collector
-XX:+ExitOnOutOfMemoryError       # Exit cleanly on OOM
-XX:+HeapDumpOnOutOfMemoryError   # Create heap dump for debugging
-XX:HeapDumpPath=/app/logs/heap-dump.hprof
```

### High-Load Services (Additional)
```
-XX:MaxGCPauseMillis=200          # Target 200ms GC pause
-XX:+UseStringDeduplication       # Reduce memory for duplicate strings
```

### Service-Specific Heap Sizes

| Service Type | Min Heap | Max Heap | Container Limit |
|-------------|----------|----------|-----------------|
| High Load | 256-384MB | 512MB | 768MB |
| Medium Load | 256MB | 384MB | 512MB |
| Lightweight | 128MB | 256MB | 384MB |

**Rationale:** Heap uses ~75% of container memory, leaving room for:
- Off-heap memory (NIO buffers, thread stacks)
- JVM metadata
- Operating system overhead

---

## PostgreSQL Tuning

### Memory Configuration
```sql
shared_buffers=256MB            # Shared memory for caching
effective_cache_size=1GB        # OS + PG cache estimate
work_mem=16MB                   # Per-query working memory
maintenance_work_mem=128MB      # For VACUUM, CREATE INDEX
wal_buffers=16MB                # Write-ahead log buffers
```

### Connection Pooling
```sql
max_connections=300             # Support 28 services × ~10 connections
```

**Per-Service HikariCP Settings:**
- Default pool size: 10 connections
- High-load services: 20 connections
- Total: ~280 connections (80% of max)

---

## Redis Configuration

### Memory Management
```
maxmemory 384mb                 # 75% of 512MB container limit
maxmemory-policy allkeys-lru    # Evict least recently used keys
```

### Persistence
```
save 60 1000                    # Save if 1000 keys changed in 60s
appendonly yes                  # AOF persistence enabled
```

---

## Benefits of Resource Limits

### Before Limits
- ❌ Services could consume unlimited memory
- ❌ OOM kills unpredictable
- ❌ One service could starve others
- ❌ No guarantee of available resources

### After Limits
- ✅ Predictable memory usage
- ✅ Services stay within bounds
- ✅ Fair resource allocation
- ✅ Early OOM detection with heap dumps
- ✅ Prevents system-wide crashes

---

## Monitoring Resource Usage

### Real-time Stats
```bash
# All containers
docker stats

# Specific service
docker stats healthdata-fhir-service

# Quick status
docker-quick.sh status
```

### Check for OOM Kills
```bash
# Check container events
docker events --filter 'event=oom'

# Check if container was OOM killed
docker inspect <container> | grep OOMKilled
```

### Find Heap Dumps
```bash
# After OOM, check logs directory
docker exec <container> ls -lh /app/logs/heap-dump.hprof
```

---

## Adjusting Limits

### Temporary Override (Testing)
```bash
# Start without resource limits
cd /mnt/wd-black/dev/projects/hdim-master
docker compose --profile core up -d
```

### Permanent Changes
Edit `docker-compose.resources.yml`:

```yaml
fhir-service:
  deploy:
    resources:
      limits:
        memory: 1G  # Increase from 768MB
  environment:
    JAVA_OPTS: >-
      -Xms384m
      -Xmx768m     # Increase heap accordingly
```

Then restart:
```bash
docker-quick.sh stop
docker-quick.sh core
```

---

## Troubleshooting

### Service Keeps Crashing
```bash
# Check if OOM killed
docker inspect <container> | grep OOMKilled

# Check logs for heap dump
docker logs <container> | grep HeapDump

# Increase memory limit in docker-compose.resources.yml
```

### High Memory Usage
```bash
# Identify culprit
docker stats | sort -k4 -h

# Check JVM heap usage
docker exec <container> jcmd 1 GC.heap_info

# Consider reducing max heap or connection pool size
```

### Performance Issues
```bash
# Check GC activity
docker exec <container> jcmd 1 VM.flags

# If frequent GC, consider increasing heap
# Edit JAVA_OPTS in docker-compose.resources.yml
```

---

## Testing Resource Limits

### Stress Test
```bash
# Start core profile with limits
docker-quick.sh core

# Monitor during load
watch -n 2 'docker stats --no-stream'

# Check that services stay within limits
```

### Verify Heap Configuration
```bash
# Check actual heap settings
docker exec hdim-fhir bash -c 'java -XX:+PrintFlagsFinal -version | grep -i heap'
```

---

## Rollback Plan

If resource limits cause issues:

### Temporary Disable
```bash
# Edit docker-quick.sh
# Remove "-f docker-compose.resources.yml" from commands

# Restart
docker-quick.sh stop
docker-quick.sh core
```

### Permanent Disable
```bash
# Rename file
mv docker-compose.resources.yml docker-compose.resources.yml.disabled

# Scripts will auto-skip missing file
```

---

*Last updated: 2026-01-10*
*Resource limits enforced: Yes*
*Auto-applied by startup scripts: Yes*
