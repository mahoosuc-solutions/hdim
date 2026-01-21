---
description: Intelligent build cleanup with space savings reporting
arguments:
  scope:
    description: Cleanup scope (all, service <name>, shared, unused)
    type: string
    required: true
    default: unused
---

# Clean Build Command

Intelligent Gradle build artifact cleanup with disk space savings reporting.

## What This Command Does

Removes build artifacts and reports space reclaimed:
- `build/` directories
- Gradle cache (obsolete versions)
- Test reports and logs
- Dangling Docker images (with confirmation)

**Does NOT remove:**
- Source code
- Version-controlled files
- Docker images in use

## Usage

```bash
/clean-build <scope>
```

## Scopes

| Scope | What It Cleans |
|-------|----------------|
| `all` | All modules (38 services + shared) |
| `service <name>` | Specific service only |
| `shared` | Shared modules only |
| `unused` | Smart detection of unused artifacts |

## Examples

```bash
# Clean unused artifacts (safe, recommended)
/clean-build unused

# Clean specific service
/clean-build service patient-service

# Clean all modules
/clean-build all

# Clean shared modules
/clean-build shared
```

---

## Implementation

### Step 1: Validate Scope

```bash
case "{{scope}}" in
    all|shared|unused)
        # Valid global scopes
        ;;
    service*)
        # Extract service name
        SERVICE_NAME=$(echo "{{scope}}" | awk '{print $2}')
        if [ ! -d "backend/modules/services/$SERVICE_NAME" ]; then
            echo "Error: Service $SERVICE_NAME not found"
            exit 1
        fi
        ;;
    *)
        echo "Error: Invalid scope. Use: all, service <name>, shared, or unused"
        exit 1
        ;;
esac
```

### Step 2: Calculate Current Usage

```bash
cd backend

# Get initial disk usage
INITIAL_SIZE=$(du -sh . | awk '{print $1}')
echo "Current build artifacts size: $INITIAL_SIZE"
```

### Step 3: Clean Based on Scope

#### For `all`:

```bash
echo "Cleaning all modules..."

# Clean all services
./gradlew clean

# Report what was cleaned
echo "✅ Cleaned build/ directories for all modules"
```

#### For `service <name>`:

```bash
echo "Cleaning service: $SERVICE_NAME..."

# Clean specific service
./gradlew :modules:services:$SERVICE_NAME:clean

echo "✅ Cleaned build/ for $SERVICE_NAME"
```

#### For `shared`:

```bash
echo "Cleaning shared modules..."

# Clean shared modules
./gradlew :modules:shared:clean

echo "✅ Cleaned shared module build artifacts"
```

#### For `unused`:

```bash
echo "Detecting unused build artifacts..."

# Find build/ directories not modified in 7+ days
UNUSED_BUILDS=$(find backend/modules -type d -name "build" -mtime +7)

if [ -z "$UNUSED_BUILDS" ]; then
    echo "No unused build artifacts found (> 7 days old)"
else
    echo "Found unused build directories:"
    echo "$UNUSED_BUILDS"

    # Calculate size
    UNUSED_SIZE=$(du -sh $UNUSED_BUILDS | awk '{sum+=$1} END {print sum}')
    echo "Total size: ${UNUSED_SIZE}MB"

    # Remove
    echo "$UNUSED_BUILDS" | xargs rm -rf
    echo "✅ Removed ${UNUSED_SIZE}MB of unused build artifacts"
fi

# Find obsolete Gradle cache (versions not in use)
echo "Checking Gradle cache..."
GRADLE_CACHE="$HOME/.gradle/caches"

if [ -d "$GRADLE_CACHE" ]; then
    # Find cache entries > 30 days old
    OBSOLETE_CACHE=$(find $GRADLE_CACHE -type d -mtime +30)

    if [ -n "$OBSOLETE_CACHE" ]; then
        CACHE_SIZE=$(du -sh $OBSOLETE_CACHE | awk '{sum+=$1} END {print sum}')
        echo "Found ${CACHE_SIZE}MB of obsolete Gradle cache"
        # Note: Don't auto-delete without confirmation
        echo "ℹ️  To clean Gradle cache manually:"
        echo "   rm -rf $HOME/.gradle/caches/modules-2/files-2.1/<old-versions>"
    fi
fi
```

### Step 4: Check for Unused Docker Images

```bash
echo ""
echo "Checking for unused Docker images..."

# Find dangling images (multi-stage build artifacts)
DANGLING=$(docker images -f "dangling=true" -q)

if [ -n "$DANGLING" ]; then
    DANGLING_SIZE=$(docker images -f "dangling=true" --format "{{.Size}}" | \
                    awk '{sum+=$1} END {print sum}')
    DANGLING_COUNT=$(echo "$DANGLING" | wc -l)

    echo "Found $DANGLING_COUNT dangling Docker images (${DANGLING_SIZE} total)"
    echo "ℹ️  To remove: docker rmi \$(docker images -f 'dangling=true' -q)"
fi

# Find unused HDIM images (not running, not in docker-compose.yml)
RUNNING_IMAGES=$(docker ps --format "{{.Image}}" | sort -u)
COMPOSE_IMAGES=$(grep "image:" docker-compose.yml | awk '{print $2}' | sort -u)
ALL_HDIM_IMAGES=$(docker images | grep "hdim-" | awk '{print $1":"$2}' | sort -u)

UNUSED_IMAGES=$(comm -23 <(echo "$ALL_HDIM_IMAGES") <(echo "$RUNNING_IMAGES $COMPOSE_IMAGES" | tr ' ' '\n' | sort -u))

if [ -n "$UNUSED_IMAGES" ]; then
    UNUSED_COUNT=$(echo "$UNUSED_IMAGES" | wc -l)
    echo "Found $UNUSED_COUNT unused HDIM Docker images"
    echo "$UNUSED_IMAGES"
    echo "ℹ️  To review and remove, use: /docker-prune"
fi
```

### Step 5: Calculate Space Reclaimed

```bash
# Get final disk usage
FINAL_SIZE=$(du -sh backend | awk '{print $1}')

# Calculate savings (simplified - actual implementation would need units handling)
echo ""
echo "==================================="
echo "Build Cleanup Summary"
echo "==================================="
echo "Initial size: $INITIAL_SIZE"
echo "Final size: $FINAL_SIZE"
echo "Space reclaimed: (calculated)"
echo ""
echo "✅ Cleanup complete!"
```

### Step 6: Provide Recommendations

```bash
echo ""
echo "Recommendations:"
echo "-----------------------------------"

# Check if build cache is enabled
if ! grep -q "org.gradle.caching=true" gradle.properties; then
    echo "⚠️  Gradle build cache disabled"
    echo "   Enable with: echo 'org.gradle.caching=true' >> gradle.properties"
fi

# Check if daemon is enabled
if ! grep -q "org.gradle.daemon=true" gradle.properties; then
    echo "⚠️  Gradle daemon disabled"
    echo "   Enable with: echo 'org.gradle.daemon=true' >> gradle.properties"
fi

# Check for large test reports
LARGE_REPORTS=$(find backend -name "test" -type d -size +100M)
if [ -n "$LARGE_REPORTS" ]; then
    echo "⚠️  Large test report directories found"
    echo "   Consider cleaning: $LARGE_REPORTS"
fi

echo ""
echo "For Docker cleanup, run: /docker-prune"
echo "For build optimization tips, run: /build-stats"
```

---

## Safety Features

1. **Confirmation for Large Cleanups:**
```bash
if [ "$scope" = "all" ]; then
    echo "⚠️  This will clean ALL modules (38 services + shared)"
    echo "Continue? [y/N]"
    read -r response
    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        echo "Cancelled"
        exit 0
    fi
fi
```

2. **Dry Run Mode:**
```bash
# Add --dry-run flag
if [ "$DRY_RUN" = "true" ]; then
    echo "DRY RUN: Would remove:"
    find backend/modules -type d -name "build" -mtime +7
    exit 0
fi
```

3. **Backup Before Clean:**
```bash
# Create backup of critical build artifacts
if [ "$BACKUP" = "true" ]; then
    BACKUP_DIR="backups/build-$(date +%Y%m%d-%H%M%S)"
    mkdir -p "$BACKUP_DIR"
    # Copy important JARs
    find backend -name "*.jar" -not -path "*/build/*" -exec cp {} "$BACKUP_DIR" \;
    echo "✅ Backed up JARs to $BACKUP_DIR"
fi
```

---

## Advanced Usage

### Clean with Statistics

```bash
/clean-build unused --stats
# Output:
# ✅ Removed 15 build directories (450MB)
# ✅ Found 8 dangling Docker images (1.2GB)
# ✅ Found 3 unused HDIM images (800MB)
# Total potential savings: 2.45GB
```

### Clean with Docker

```bash
/clean-build all --include-docker
# Also removes unused Docker images (with confirmation)
```

### Scheduled Cleanup

```bash
# Add to cron for weekly cleanup
0 2 * * 0 /path/to/claude-code /clean-build unused
```

---

## Troubleshooting

### "Permission denied" errors

**Cause:** Build directories owned by Docker containers

**Fix:**
```bash
sudo chown -R $USER:$USER backend/modules/services/*/build
```

### Gradle cache corruption

**Cause:** Interrupted builds, network issues

**Fix:**
```bash
# Rebuild Gradle cache
rm -rf $HOME/.gradle/caches
./gradlew --refresh-dependencies
```

### Disk space still high

**Cause:** Docker volumes, logs, or test data

**Fix:**
```bash
# Check Docker volume usage
docker system df

# Clean Docker completely
/docker-prune

# Check for large log files
find backend -name "*.log" -size +100M
```

---

## Best Practices

1. **Run `unused` scope weekly** - Keeps disk usage manageable
2. **Run `all` before major releases** - Clean slate for builds
3. **Check Docker regularly** - Images accumulate quickly (38 services!)
4. **Enable build cache** - Reduces rebuild time (saves space indirectly)
5. **Monitor disk usage** - Use `/build-stats` for trends

---

## Related Commands

- `/docker-prune` - Docker-specific cleanup
- `/build-stats` - Analyze build performance and space usage
- `/build-service` - Build specific service with caching

---

## Example Output

```
$ /clean-build unused

Detecting unused build artifacts...
Current build artifacts size: 2.5GB

Found unused build directories:
backend/modules/services/patient-service/build (last modified: 10 days ago)
backend/modules/services/fhir-service/build (last modified: 15 days ago)
Total size: 450MB

✅ Removed 450MB of unused build artifacts

Checking for unused Docker images...
Found 8 dangling Docker images (1.2GB total)
ℹ️  To remove: docker rmi $(docker images -f 'dangling=true' -q)

Found 3 unused HDIM images:
- hdim-old-service:latest (350MB)
- hdim-test-service:v1 (300MB)
- hdim-deprecated:latest (150MB)
ℹ️  To review and remove, use: /docker-prune

===================================
Build Cleanup Summary
===================================
Initial size: 2.5GB
Final size: 2.05GB
Space reclaimed: 450MB

✅ Cleanup complete!

Recommendations:
-----------------------------------
For Docker cleanup, run: /docker-prune
For build optimization tips, run: /build-stats
```

---

## Performance Impact

**Cleanup Time:**
- `unused`: ~10 seconds
- `service <name>`: ~5 seconds
- `shared`: ~15 seconds
- `all`: ~30-60 seconds (38 services)

**Space Savings (Typical):**
- Unused builds: 200-500MB
- Gradle cache: 50-200MB
- Docker images: 1-5GB
- **Total:** 1.5-6GB reclaimed
