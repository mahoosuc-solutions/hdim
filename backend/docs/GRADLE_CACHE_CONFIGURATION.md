# Gradle Build Cache Configuration Guide

**Purpose:** Comprehensive guide for configuring and optimizing Gradle's build cache in HDIM
**Audience:** Developers, DevOps engineers, build automation engineers
**Status:** Production-Ready
**Version:** 1.0

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Configuration](#configuration)
3. [How Build Cache Works](#how-build-cache-works)
4. [Performance Tuning](#performance-tuning)
5. [Troubleshooting](#troubleshooting)
6. [Advanced Techniques](#advanced-techniques)
7. [Team Guidelines](#team-guidelines)

---

## Quick Start

### Enable Gradle Build Cache (5 minutes)

**Step 1: Verify gradle.properties**

```bash
cd backend
grep "org.gradle.caching" gradle.properties
```

**Expected Output:**
```
org.gradle.caching=true
```

If not present, add to `gradle.properties`:

```gradle
# Enable Gradle Build Cache
org.gradle.caching=true
```

**Step 2: Test with a build**

```bash
cd backend

# Clean build (establishes cache baseline)
./gradlew clean build -x test --build-cache

# Second build (should use cache)
./gradlew clean build -x test --build-cache
```

**Expected Result:**
Second build should show "UP-TO-DATE" or "FROM-CACHE" for most tasks.

**Step 3: Verify cache directory**

```bash
# Check cache exists
ls -la ~/.gradle/build-cache/

# View cache size
du -sh ~/.gradle/

# Expected: 100-500 MB depending on project size
```

---

## Configuration

### File: backend/gradle.properties

```gradle
# Gradle Build Cache Configuration
# ============================================================================

# Enable build cache (stores task outputs for reuse)
org.gradle.caching=true

# Optional: Enable remote build cache (requires server)
# org.gradle.cache.remote=true
# org.gradle.cache.remote.url=https://cache.healthdata.example.com
# org.gradle.cache.remote.push=true

# JVM Arguments for optimal caching
# ============================================================================

# Heap size: 4GB sufficient for HDIM (29+ services)
# -XX:+UseStringDeduplication: Reduces memory by 5-10% for large builds
# -XX:+UnlockExperimentalVMOptions: Enable experimental features (optional)
org.gradle.jvmargs=-Xmx4g -XX:+UseStringDeduplication

# Daemon JVM arguments (for --daemon mode)
# Note: Currently disabled in CI (--no-daemon) but useful locally
org.gradle.daemon.jvmargs=-Xmx2g

# Parallel Execution
# ============================================================================

# Enable parallel task execution
org.gradle.parallel=true

# Number of workers (default: number of CPU cores)
# Set to 4 for consistent performance across environments
org.gradle.workers.max=4

# Network Configuration
# ============================================================================

# HTTP connection timeout (ms) - 2 minutes
systemProp.org.gradle.internal.http.connectionTimeout=120000

# Socket timeout (ms) - 2 minutes
systemProp.org.gradle.internal.http.socketTimeout=120000

# Optional: Repository mirrors for better TLS support
org.gradle.internal.repository.maven.disabled=false

# Java Installation (optional)
# ============================================================================

# Point to locally downloaded JDK (if not using system default)
# org.gradle.java.installations.paths=/usr/lib/jvm/java-21-openjdk
# org.gradle.java.installations.auto-detect=true
# org.gradle.java.installations.auto-download=false
```

### Environment Variables (Workflow)

**File: `.github/workflows/backend-ci-v2-parallel.yml`**

```yaml
env:
  JAVA_VERSION: '21'
  # Enable build cache and parallel execution
  GRADLE_OPTS: |
    -Dorg.gradle.daemon=false
    -Dorg.gradle.parallel=true
    -Dorg.gradle.workers.max=4
```

### GitHub Actions Workflow Step

**Complete configuration:**

```yaml
build:
  runs-on: ubuntu-latest
  steps:
    # Setup Java with automatic Gradle cache
    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: 'gradle'  # Automatic setup of ~/.gradle

    # Additional cache configuration (optional but recommended)
    - name: Cache Gradle dependencies
      uses: actions/cache@v4
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
          backend/.gradle
        key: ${{ runner.os }}-gradle-${{ hashFiles('backend/**/*.gradle.kts', 'backend/**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-

    # Download dependencies (populates cache)
    - name: Download Gradle dependencies
      working-directory: backend
      run: |
        ./gradlew downloadDependencies \
          --no-daemon \
          --build-cache \
          --console=plain

    # Build with cache enabled
    - name: Build with cache
      working-directory: backend
      run: |
        ./gradlew build -x test \
          --no-daemon \
          --build-cache \
          --parallel \
          --max-workers=4 \
          --console=plain
```

---

## How Build Cache Works

### Cache Storage Structure

```
~/.gradle/
├── caches/
│   ├── modules-2/              (Dependency cache)
│   │   ├── files-2.1/
│   │   └── metadata-2.x/
│   ├── build-cache-1/          (Task output cache)
│   │   ├── 12/                 (Cache entries by hash)
│   │   ├── 34/
│   │   └── ...
│   └── transformers-3/         (Artifact transforms)
├── wrapper/                    (Gradle distribution)
│   └── dists/
└── build-cache/                (Project-specific, if enabled)
```

### Cache Key Generation

Gradle determines cache validity based on:

```
Cache Key = hash(
  - Task type (e.g., compileJava)
  - Task inputs (source files, configuration)
  - Gradle version
  - Plugin versions
  - Java version
)
```

**Example:**

```gradle
// This task would generate a cache key based on:
tasks.register<JavaCompile>("compileJava") {
  // Inputs that affect cache
  source = sourceSets.main.java.srcDirs
  classpath = configurations.compileClasspath

  // Output that gets cached
  destinationDir = file("build/classes/java/main")
}
```

### Cache Invalidation Triggers

Cache is automatically invalidated when:

1. **Source file changes**
   ```bash
   # Any .java file change → compileJava task re-executes
   echo "// comment" >> src/main/java/Example.java
   ```

2. **Configuration changes**
   ```gradle
   # Change in build.gradle.kts → affected tasks re-execute
   dependencies {
     implementation("com.example:lib:1.0.0")  // version change
   }
   ```

3. **Gradle version upgrade**
   ```bash
   # Gradle version change → all tasks may re-execute
   ./gradlew wrapper --gradle-version=8.12
   ```

4. **Plugin version changes**
   ```gradle
   plugins {
     id("java") version "1.0.0"  // change in version
   }
   ```

### Cache Hit vs Miss

**Cache Miss (Task Re-executes):**
```
Task: compileJava
Inputs: Java source files (modified)
         Gradle configuration (unchanged)
         JDK version (unchanged)

Cache Key: hash(all inputs)
→ No matching cache entry
→ Task executes and generates output
→ Output stored in cache with new key
→ Result: EXECUTED
```

**Cache Hit (Task Skipped):**
```
Task: compileJava
Inputs: Java source files (unchanged)
         Gradle configuration (unchanged)
         JDK version (unchanged)

Cache Key: hash(all inputs)
→ Matching cache entry found
→ Cached output restored
→ Task skipped
→ Result: UP-TO-DATE or FROM-CACHE
```

### Performance Breakdown

**First Build (No Cache):**
```
Initial setup:      2-3 min
  └ Download dependencies
  └ Setup build environment

Compilation:        4-5 min
  └ Compile all Java source files
  └ Generate resources

Testing:            1-2 min
  └ Unit test execution

Processing:         1-2 min
  └ Plugin execution
  └ Annotation processing

Total:              8-12 minutes
```

**Second Build (Full Cache Hit):**
```
Setup:              1-2 min
  └ Validate inputs (fast)
  └ Check cache keys

Cache restoration:  3-4 min
  └ Load compiled classes from cache
  └ Load generated resources from cache

Unchanged tasks:    1-2 min
  └ Configuration processing (not cacheable)
  └ Final validation

Total:              5-8 minutes (33-50% faster!)
```

---

## Performance Tuning

### 1. Optimize JVM Settings

**For CI/CD Environments:**

```gradle
# gradle.properties
org.gradle.jvmargs=-Xmx4g \
  -XX:+UseStringDeduplication \
  -XX:StringDeduplicationAgeThreshold=3 \
  -XX:+ParallelRefProcEnabled
```

**Why each setting:**

| Setting | Purpose | Impact |
|---------|---------|--------|
| `-Xmx4g` | Heap size | Allows parallel compilation of 29+ services |
| `UseStringDeduplication` | Memory optimization | 5-10% memory reduction |
| `StringDeduplicationAgeThreshold=3` | GC tuning | Faster deduplication |
| `ParallelRefProcEnabled` | Parallel GC | Better GC performance with parallel builds |

### 2. Configure Parallel Workers

```gradle
# gradle.properties
org.gradle.parallel=true
org.gradle.workers.max=4
```

**Worker Count Tuning:**

| Environment | CPU Cores | Recommended Workers | Reasoning |
|-------------|-----------|-------------------|-----------|
| Developer Laptop | 4-8 | 2-4 | Balance build speed with responsiveness |
| CI/CD Runner | 4-8 | 4-8 | Maximize resource utilization |
| Docker | 2-4 | 2 | Prevent resource exhaustion |

**Test locally:**
```bash
# Monitor with different worker counts
time ./gradlew build -x test --gradle.workers.max=2
time ./gradlew build -x test --gradle.workers.max=4
time ./gradlew build -x test --gradle.workers.max=8
```

### 3. Cache Directory Optimization

**Monitor cache size:**

```bash
# Check current size
du -sh ~/.gradle/caches

# Breakdown by module
du -sh ~/.gradle/caches/modules-2

# Find large entries
find ~/.gradle -type f -size +10M | head -10
```

**Cache rotation (when > 4 GB):**

```bash
# Option 1: Clear dependencies only (keep build cache)
rm -rf ~/.gradle/caches/modules-2

# Option 2: Full cache reset
rm -rf ~/.gradle/caches ~/.gradle/wrapper

# Rebuild to repopulate
./gradlew clean build --build-cache -x test
```

### 4. GitHub Actions Cache Strategy

**Optimize cache key:**

```yaml
cache:
  # Include only essential files in key hash
  path: |
    ~/.gradle/caches
    ~/.gradle/wrapper
  key: ${{ runner.os }}-gradle-${{ hashFiles(
    'backend/gradle.properties',
    'backend/gradle/wrapper/gradle-wrapper.properties',
    'backend/**/*.gradle.kts'
  ) }}
  restore-keys: |
    ${{ runner.os }}-gradle-
    ${{ runner.os }}-
```

**Why this works:**

```
Build 1: Hash = ABC123
  ├─ gradle.properties unchanged
  ├─ gradle-wrapper.properties unchanged
  └─ build.gradle.kts unchanged
  → Cache key = ABC123

Build 2: Hash = ABC123 (exact match!)
  → Use cached artifacts from Build 1
  → 80% faster restore

Build 3: gradle.properties changed
  ├─ Hash = XYZ789 (different!)
  └─ Fallback to ABC123 prefix (restore-keys)
  → Partial cache restored
  → 50% faster than cold start
```

---

## Troubleshooting

### Issue 1: Cache Not Working

**Symptom:** Build times don't improve on second run

**Diagnosis:**

```bash
# Run build with diagnostic output
./gradlew build --build-cache --info 2>&1 | grep -i "cache"

# Expected output:
# "Task ... FROM-CACHE"
# "Task ... UP-TO-DATE"

# If you see "EXECUTED" for all tasks, cache isn't working
```

**Solution Steps:**

```bash
# 1. Verify caching is enabled
grep "org.gradle.caching=true" gradle.properties

# 2. Check cache directory exists
ls -la ~/.gradle/build-cache/

# 3. Clear and rebuild
rm -rf ~/.gradle/build-cache
./gradlew build --build-cache -x test

# 4. Run second build (should use cache)
./gradlew build --build-cache -x test

# 5. Check output for cache hits
./gradlew build --build-cache --info 2>&1 | tail -20
```

### Issue 2: Slow Build Despite Caching

**Symptom:** Build still takes 8-10 minutes even with cache

**Diagnosis:**

```bash
# Check which tasks are not cached
./gradlew build --build-cache --info 2>&1 | \
  grep "EXECUTED" | head -20

# Check for non-deterministic tasks
./gradlew build --build-cache --info 2>&1 | \
  grep -E "(EXECUTED|FAILED)"
```

**Solutions:**

1. **Check for task outputs that change every build:**
   ```gradle
   // build.gradle.kts
   tasks.test {
     outputs.upToDateWhen { false }  // ← Always runs, not cacheable
   }
   ```

2. **Verify no system clock dependencies:**
   ```java
   // BAD: Includes timestamp
   class BuildInfo {
     String timestamp = System.currentTimeMillis(); // ← Cache buster
   }

   // GOOD: Use build time instead
   class BuildInfo {
     String buildTime = System.getenv("BUILD_TIME"); // ← Stable
   }
   ```

3. **Check file permissions haven't changed:**
   ```bash
   # Permissions changes invalidate cache
   find . -type f -newer ~/.gradle/build-cache -ls | head -20
   ```

### Issue 3: Cache Grows Too Large

**Symptom:** `~/.gradle` directory > 4 GB

**Diagnosis:**

```bash
# Find largest cache directories
du -sh ~/.gradle/caches/* | sort -h | tail -5

# Identify which dependencies are cached
du -sh ~/.gradle/caches/modules-2/files-2.1/*/
```

**Solutions:**

```bash
# Option 1: Clean old cache entries (keeps wrapper)
rm -rf ~/.gradle/caches/build-cache-1

# Option 2: Rotate all caches (starts fresh)
rm -rf ~/.gradle/caches ~/.gradle/wrapper

# Option 3: Gradle cache cleanup (Gradle 5.6+)
./gradlew cacheCleanup
```

### Issue 4: GitHub Actions Cache Full

**Symptom:** "Cache upload failed due to quota restrictions"

**Investigation:**

```bash
# Estimate cache size before push
cd backend
tar -czf /tmp/cache-estimate.tar.gz ~/.gradle
du -sh /tmp/cache-estimate.tar.gz
```

**Solutions:**

1. **Reduce cache paths in workflow:**
   ```yaml
   path: |
     ~/.gradle/caches/modules-2  # Only modules, not wrapper
     ~/.gradle/wrapper
   ```

2. **Clear cache via GitHub UI:**
   - Settings → Actions → Caches
   - Delete oldest caches first

3. **Exclude non-essential files:**
   ```yaml
   path: |
     ~/.gradle/caches
   exclude: |
     ~/.gradle/caches/modules-2/files-2.1/**/sources
     ~/.gradle/caches/modules-2/files-2.1/**/javadoc
   ```

---

## Advanced Techniques

### 1. Remote Build Cache Server

For teams with many developers:

```gradle
// gradle.properties
org.gradle.caching=true
org.gradle.cache.remote.url=https://cache.healthdata.example.com
org.gradle.cache.remote.push=true
org.gradle.cache.remote.credentials.username=ci-user
org.gradle.cache.remote.credentials.password=${GRADLE_CACHE_PASSWORD}
```

**Benefits:**
- First PR build: 12 min → 8-9 min
- Team's subsequent builds: 10 min → 4-5 min
- 40-50% team-wide speedup

**Setup:**
- Use Gradle Enterprise (commercial) or similar
- Configure authentication in CI/CD secrets

### 2. Selective Task Caching

Some tasks shouldn't be cached (non-deterministic):

```gradle
// build.gradle.kts
tasks.test {
  outputs.cacheIf {
    // Only cache if specific conditions met
    System.getenv("CACHE_TESTS")?.toBoolean() ?: false
  }
}

// Or: never cache
tasks.integrationTest {
  outputs.upToDateWhen { false }
}
```

### 3. Custom Cache Tasks

Define cache-aware custom tasks:

```gradle
abstract class MyTask : DefaultTask() {
  @get:InputFile
  abstract val inputFile: RegularFileProperty

  @get:OutputFile
  abstract val outputFile: RegularFileProperty

  @TaskAction
  fun execute() {
    val input = inputFile.get().asFile
    val output = outputFile.get().asFile
    output.writeText(input.readText().toUpperCase())
  }
}

tasks.register<MyTask>("processData") {
  inputFile = layout.projectDirectory.file("input.txt")
  outputFile = layout.buildDirectory.file("output.txt")
}
```

### 4. Cache Debugging

Enable detailed cache diagnostics:

```bash
# Detailed cache info
./gradlew build --build-cache -Dorg.gradle.caching.debug=true

# Export cache metadata
./gradlew build --build-cache -Dorg.gradle.cache.debug=true 2>&1 | tee cache-debug.log

# Analyze cache entries
find ~/.gradle/build-cache-1 -type f | head -20
```

---

## Team Guidelines

### For Developers

1. **Run with cache locally:**
   ```bash
   cd backend
   ./gradlew build -x test --build-cache
   ```

2. **Don't disable caching for "full" builds:**
   ```bash
   # ✗ Avoid
   ./gradlew clean build

   # ✓ Use cache-aware
   ./gradlew build --build-cache
   ```

3. **Report slow builds:**
   ```bash
   # If build > 10 min, investigate
   ./gradlew build --build-cache --info > build-report.txt
   grep "EXECUTED" build-report.txt
   ```

### For CI/CD Operators

1. **Monitor cache health weekly:**
   ```bash
   # Check cache hit rate
   grep "FROM-CACHE" .github/workflows/logs | wc -l

   # Check cache miss rate
   grep "EXECUTED" .github/workflows/logs | wc -l

   # Target: 70% hit rate
   ```

2. **Review cache growth monthly:**
   ```bash
   # Check if cache exceeds 4 GB
   du -sh ~/.gradle/caches

   # If yes, rotate
   rm -rf ~/.gradle/caches/modules-2
   ```

3. **Document cache strategy:**
   - Cache retention: 7 days (GitHub default)
   - Cache rotation: Monthly
   - Hit rate target: >70%

### For Build Engineers

1. **Optimize hot paths:**
   - Identify tasks taking >1 min
   - Add cache hints if non-deterministic
   - Profile build execution

2. **Keep gradle.properties up-to-date:**
   - Review quarterly for new Gradle versions
   - Test new JVM flags (StringDeduplication, etc.)
   - Document any changes

3. **Integrate with monitoring:**
   - Export cache hit rates to dashboard
   - Alert on cache misses > 3 consecutive builds
   - Track build time trends

---

## Reference

### Gradle Cache Documentation

- [Gradle Build Cache](https://docs.gradle.org/current/userguide/build_cache.html)
- [Gradle Build Cache Types](https://docs.gradle.org/current/userguide/build_cache_concepts.html)

### Related HDIM Documentation

- [PHASE-7-CACHING-STRATEGY.md](../../PHASE-7-CACHING-STRATEGY.md) - Overall caching strategy
- [BUILD_MANAGEMENT_GUIDE.md](./BUILD_MANAGEMENT_GUIDE.md) - Build system overview
- [COMMAND_REFERENCE.md](./COMMAND_REFERENCE.md) - Common build commands

---

## Appendix: Configuration Templates

### Complete gradle.properties

```gradle
# Gradle Build Cache (Phase 7 Optimization)
org.gradle.caching=true

# JVM Configuration
org.gradle.jvmargs=-Xmx4g \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:+UseStringDeduplication \
  -Dapi.version=1.52 \
  -Ddocker.api.version=1.52

# Java Installation
org.gradle.java.installations.auto-detect=true
org.gradle.java.installations.auto-download=false

# Network Timeouts
systemProp.org.gradle.internal.http.connectionTimeout=120000
systemProp.org.gradle.internal.http.socketTimeout=120000

# Repository Configuration
org.gradle.internal.repository.maven.disabled=false

# Parallel Execution
org.gradle.parallel=true
org.gradle.workers.max=4
```

### Complete Workflow Step

```yaml
- name: Build with optimized Gradle cache
  working-directory: backend
  run: |
    # Download dependencies
    ./gradlew downloadDependencies \
      --no-daemon \
      --build-cache \
      --console=plain

    # Build with cache and parallel execution
    ./gradlew build -x test \
      --no-daemon \
      --build-cache \
      --parallel \
      --max-workers=4 \
      --console=plain
```

---

**Version:** 1.0
**Last Updated:** February 2026
**Maintained By:** HDIM DevOps Team
**Status:** Production-Ready
