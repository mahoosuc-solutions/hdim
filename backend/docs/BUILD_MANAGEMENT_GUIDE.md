# Build Management Guide

---
**Navigation:** [CLAUDE.md](../../CLAUDE.md#build-management-best-practices) | [Documentation Portal](../../docs/README.md) | [Backend Docs Index](./README.md)
---

## Overview

This guide covers best practices for building microservices in HDIM. The platform's 28+ services and complex dependency chain require careful build management to avoid system overload and timeouts.

**Key Principle:** Build ONE service at a time in a controlled, sequential manner.

---

## Golden Rules for Building Services

**IMPORTANT**: Follow these rules to avoid chaotic builds and system overload.

### Rule 1: Build ONE Service at a Time

❌ **DON'T** (creates chaos):
```bash
# This spawns 5 parallel builds - system overload
docker compose build patient-event-service quality-measure-event-service \
  care-gap-event-service clinical-workflow-event-service fhir-service
```

✅ **DO** (sequential, controlled):
```bash
# Build one service, wait for completion, then next
docker compose build patient-event-service
docker compose build quality-measure-event-service
docker compose build care-gap-event-service
```

### Rule 2: Cache Dependencies Locally First

Before Docker builds:
```bash
# Pre-cache all Gradle dependencies
cd backend
./gradlew --no-daemon downloadDependencies

# This prevents TLS timeout issues in Docker containers
# Docker will reuse the cached ~/.gradle/ directory
```

### Rule 3: Monitor Builds Actively

```bash
# Watch a single build
docker compose build patient-event-service --progress=plain

# Check status while building
docker ps --format "table {{.Names}}\t{{.Status}}"

# Monitor service startup after build
docker compose logs -f patient-event-service | grep -E "ERROR|error|Started|Successfully"
```

### Rule 4: Use Clear Build Strategies

| Scenario | Command | Why |
|----------|---------|-----|
| First build | `docker compose build --no-cache SERVICE` | Force fresh build, no issues from stale layers |
| Dependency update | `./gradlew clean build` locally, then `docker compose build` | Cache local deps, avoid Docker TLS issues |
| Debug failing build | `docker compose build --progress=plain SERVICE` | See step-by-step output, not truncated summary |
| Production build | `docker compose build --pull SERVICE` | Fresh base images, no cached security issues |

---

## Common Build Issues & Solutions

| Problem | Cause | Fix |
|---------|-------|-----|
| **TLS Handshake Error** | Docker can't reach Maven Central | `./gradlew downloadDependencies` locally first, or use `--no-cache` and wait |
| **"Relation already exists"** | `ddl-auto: create` runs before Liquibase | Verify `ddl-auto: validate` in docker-compose.yml |
| **Build times out** | Too many parallel builds | Build services sequentially, one at a time |
| **Out of memory** | Docker daemon running too many containers | Stop background containers: `docker compose down` |
| **Docker layer cache issues** | Stale cached layer preventing code updates | Use `--no-cache` flag: `docker compose build --no-cache SERVICE` |

---

## Network Configuration

The following settings have been pre-configured to handle Docker network issues:

### Gradle Configuration

**File**: `backend/gradle.properties`
```properties
# Network timeouts (2 minutes) - prevents Maven Central TLS failures
systemProp.org.gradle.internal.http.connectionTimeout=120000
systemProp.org.gradle.internal.http.socketTimeout=120000

# Enable Maven mirror support
org.gradle.internal.repository.maven.disabled=false
```

### Repository Mirrors

**File**: `backend/build.gradle.kts`
```kotlin
repositories {
    mavenCentral()  // Primary
    maven { url = uri("https://maven.aliyun.com/repository/public") }  // Fallback
    maven { url = uri("https://build.fhir.org/ig/") }  // FHIR
}
```

The configuration includes:
- **Primary:** Maven Central (standard repository)
- **Fallback:** Alibaba Maven mirror (faster in some regions)
- **FHIR:** Build FHIR registry (HL7 FHIR specifications)

---

## Recommended Build Workflow

Follow this step-by-step workflow for reliable builds:

### 1. Clean Local Cache (if issues persist)

```bash
cd backend
./gradlew clean --no-daemon
```

Use this when experiencing:
- Stale dependency caches
- Repeated compilation failures
- Class loading issues

### 2. Download Dependencies Locally

```bash
./gradlew downloadDependencies --no-daemon
```

**Why:** Pre-caches all Maven dependencies locally, preventing TLS timeout issues in Docker containers.

**Result:** Docker build reuses cached `~/.gradle/` directory, making Docker builds fast.

### 3. Build JAR Locally

```bash
./gradlew :modules:services:SERVICENAME:bootJar -x test --no-daemon
```

**Why:** Verifies code compiles and dependencies resolve before Docker build.

**Flags:**
- `:modules:services:SERVICENAME:bootJar` - Create bootable JAR for specific service
- `-x test` - Skip tests (build only, verify tests separately)
- `--no-daemon` - Don't use Gradle daemon (prevents hanging builds)

### 4. Build Docker Image

```bash
docker compose build SERVICENAME
```

**Why:** Should be fast now - all dependencies already cached locally.

### 5. Start and Verify

```bash
docker compose up -d SERVICENAME
docker compose logs -f SERVICENAME | head -50
```

**Check for:**
- "Started [ServiceName]" message (successful startup)
- No ERROR or EXCEPTION lines
- Database migrations completed (Liquibase messages)

---

## Build Strategies by Scenario

### First-Time Setup

```bash
# Clean start, force fresh dependencies
cd backend
./gradlew clean --no-daemon
./gradlew downloadDependencies --no-daemon

# Build all services sequentially
for service in patient-event-service quality-measure-event-service \
  care-gap-event-service clinical-workflow-event-service fhir-service; do
  echo "Building $service..."
  docker compose build "$service"
  sleep 10  # Give Docker daemon time to reset
done

# Start core services only
docker compose --profile core up -d
```

### Dependency Update

When updating dependencies (e.g., Spring Boot version):

```bash
# 1. Update build.gradle.kts or libs.versions.toml
# 2. Verify locally first
cd backend
./gradlew clean --no-daemon
./gradlew :modules:services:SERVICENAME:test

# 3. Cache new dependencies
./gradlew downloadDependencies --no-daemon

# 4. Build Docker image
docker compose build --no-cache SERVICENAME

# 5. Test in Docker
docker compose up -d SERVICENAME
docker compose logs -f SERVICENAME
```

### Debug Failing Build

```bash
# See step-by-step output (not truncated summary)
docker compose build --progress=plain SERVICENAME 2>&1 | tee build.log

# If it hangs, check daemon status
docker ps
docker stats  # Monitor resource usage

# Common fixes
docker compose down  # Free memory
./gradlew --stop   # Stop Gradle daemon
docker compose build --no-cache SERVICENAME  # Clean rebuild
```

### Production Build

```bash
# Fresh base images, security-critical
docker compose build --pull SERVICENAME

# Verify no vulnerabilities
docker inspect SERVICENAME:latest
```

---

## Build Performance Optimization

### Parallel Builds (HDIM Limitation)

❌ **Do NOT attempt parallel builds** with `docker compose build service1 service2 service3`

**Why:**
- Each build requires 2GB+ RAM
- Docker daemon becomes bottleneck
- Network timeouts increase with parallel builds
- No performance gain vs sequential (build time dominated by Java compilation)

**Sequential is faster** because:
1. Each build gets full resources
2. No contention for network/disk I/O
3. Easier to diagnose failures

### Memory Management

Monitor Docker daemon memory:

```bash
# Check current usage
docker stats --no-stream

# If building stalls, free memory
docker compose down
docker system prune -a  # Remove unused images/containers/networks
```

Set Docker desktop limits:
- **Memory:** 8GB minimum (more if available)
- **CPU:** 4+ cores
- **Disk:** 50GB free space minimum

### Build Times

**Typical build times (sequential):**
- First build: 5-10 minutes (downloading all dependencies)
- Subsequent builds: 30 seconds - 2 minutes (cached dependencies)
- With code changes: 1-3 minutes (recompilation only)
- Production build: 10-15 minutes (fresh images, security scans)

### Optimize Gradle Builds

```bash
# Enable build cache
export GRADLE_OPTS="-Dorg.gradle.caching=true"

# Limit parallel tasks (if using gradle parallel)
./gradlew --max-workers=2 build

# Use daemon for faster repeated builds
./gradlew build  # Uses daemon by default
```

---

## Docker Compose Build Commands

### Build Single Service

```bash
docker compose build patient-event-service
```

### Build Multiple (sequentially)

```bash
docker compose build patient-event-service
docker compose build quality-measure-event-service
docker compose build care-gap-event-service
```

### View Build Output

```bash
# Verbose output (step-by-step)
docker compose build --progress=plain SERVICENAME

# Standard output (summary)
docker compose build SERVICENAME

# Watch logs while building
docker compose logs -f SERVICENAME
```

### Rebuild Without Cache

```bash
# Force rebuild, ignore all cached layers
docker compose build --no-cache SERVICENAME
```

### Pull Fresh Base Images

```bash
# Useful for security updates to base image
docker compose build --pull SERVICENAME
```

---

## Troubleshooting

### "TLS Handshake Failed"

**Error:**
```
javax.net.ssl.SSLHandshakeException: Remote host closed connection during handshake
```

**Cause:** Docker can't reach Maven Central

**Fix:**
```bash
# Option 1: Pre-cache locally
cd backend
./gradlew downloadDependencies --no-daemon

# Option 2: Use --no-cache (slow but works)
docker compose build --no-cache SERVICE

# Option 3: Check network/firewall
curl https://repo.maven.apache.org/maven2/
```

### "Relation Already Exists"

**Error:**
```
org.postgresql.util.PSQLException: ERROR: relation "patients" already exists
```

**Cause:** `ddl-auto: create` ran before Liquibase migrations

**Fix:**
1. Check `docker-compose.yml` or `application.yml`
2. Verify `spring.jpa.hibernate.ddl-auto: validate`
3. Ensure `spring.liquibase.enabled: true`

See [Database Architecture Guide](./DATABASE_ARCHITECTURE_GUIDE.md) for complete database setup.

### Build Hangs / Times Out

**Symptoms:** Build stuck for >30 minutes

**Cause:** Usually network timeout or system resource exhaustion

**Fix:**
```bash
# Stop hanging build
docker compose down
docker system prune -af  # Remove everything

# Check system resources
docker stats
free -h
df -h

# Retry with clean slate
./gradlew --stop
./gradlew downloadDependencies --no-daemon
docker compose build --no-cache SERVICENAME
```

### Out of Memory

**Error:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Fix:**
```bash
# Increase Gradle heap
export GRADLE_OPTS="-Xmx4g"

# Stop other containers
docker compose down

# Retry build
docker compose build SERVICENAME
```

---

## Related Guides

- [Database Architecture Guide](./DATABASE_ARCHITECTURE_GUIDE.md) - Liquibase and schema management
- [Liquibase Development Workflow](./LIQUIBASE_DEVELOPMENT_WORKFLOW.md) - Database migration procedures
- [Command Reference](./COMMAND_REFERENCE.md) - Complete list of build and gradle commands

---

_Last Updated: January 19, 2026_
_Version: 1.0_