# Command Reference

---
**Navigation:** [CLAUDE.md](../../CLAUDE.md#common-commands) | [Documentation Portal](../../docs/README.md) | [Backend Docs Index](./README.md)
---

## Overview

This is a comprehensive reference of commonly used commands for development, testing, building, and operations in HDIM.

**Key Locations:**
- **Build commands:** Run from `backend/` directory
- **Docker commands:** Run from project root
- **Database commands:** Use `docker exec` from anywhere

---

## Gradle Commands (Backend)

### Building

Run all from `backend/` directory.

#### Full Build

```bash
# Build all modules (compile + test + package)
./gradlew build

# Skip tests for faster build
./gradlew build -x test

# Build with clean first (useful for stale artifacts)
./gradlew clean build

# Build single service
./gradlew :modules:services:patient-event-service:build

# Build multiple services
./gradlew :modules:services:patient-event-service:build \
  :modules:services:quality-measure-event-service:build
```

#### Creating JARs

```bash
# Create bootable JAR for service
./gradlew :modules:services:SERVICENAME:bootJar

# Create JARs for all services
./gradlew bootJar

# Output location: modules/services/SERVICENAME/build/libs/servicename-version.jar
```

### Testing

```bash
# Run all tests
./gradlew test

# Run tests for specific service
./gradlew :modules:services:patient-event-service:test

# Run specific test class
./gradlew :modules:services:patient-event-service:test --tests PatientServiceTest

# Run specific test method
./gradlew :modules:services:patient-event-service:test --tests PatientServiceTest.shouldCreatePatient

# Run tests matching pattern
./gradlew test --tests "*Migration*"

# Run integration tests (requires Docker)
./gradlew integrationTest

# Skip tests (for CI/CD when tests already passed)
./gradlew build -x test
```

### Dependency Management

```bash
# Download all dependencies locally
./gradlew downloadDependencies

# Download and verify checksum
./gradlew downloadDependencies --refresh-dependencies

# View dependency tree
./gradlew dependencies

# View dependency tree for specific project
./gradlew :modules:services:patient-event-service:dependencies

# Check for dependency updates available
./gradlew dependencyUpdates

# Report dependency conflicts
./gradlew dependencyInsight --dependency spring-boot-starter
```

### Troubleshooting

```bash
# Clean local cache
./gradlew clean

# Clean and reset daemon
./gradlew --stop
./gradlew clean

# Run with more verbose output
./gradlew build --info

# Run with debug output
./gradlew build --debug

# Force rebuild without cache
./gradlew clean build --no-build-cache

# Run with maximum parallel jobs
./gradlew build --parallel --max-workers=8

# Run with single thread (for debugging)
./gradlew build --max-workers=1
```

### Performance & Optimization

```bash
# Enable Gradle build cache
export GRADLE_OPTS="-Dorg.gradle.caching=true"
./gradlew build

# Use Gradle daemon for faster builds
./gradlew build  # Uses daemon by default

# Increase heap size for large projects
export GRADLE_OPTS="-Xmx4g"
./gradlew build

# Run in offline mode (uses cached deps only)
./gradlew build --offline
```

---

## Docker Compose Commands

### Container Management

Run all from project root (where `docker-compose.yml` exists).

#### Starting Services

```bash
# Start all services
docker compose up -d

# Start core services only (use profiles)
docker compose --profile core up -d

# Start specific services
docker compose up -d patient-event-service quality-measure-event-service

# Start and show logs
docker compose up -d
docker compose logs -f

# Start with fresh builds
docker compose up -d --build

# Start with fresh builds and no cache
docker compose up -d --build --no-cache
```

#### Stopping Services

```bash
# Stop all services (keeps containers)
docker compose stop

# Stop specific service
docker compose stop patient-event-service

# Remove all containers (but not volumes)
docker compose down

# Remove containers and volumes (clean slate)
docker compose down -v

# Remove containers, volumes, and networks
docker compose down -v --remove-orphans
```

#### Viewing Status

```bash
# Show running containers
docker compose ps

# Show all containers (including stopped)
docker compose ps -a

# Show container stats (memory, CPU)
docker stats --no-stream hdim-postgres

# Watch containers in real-time
watch -n 1 docker compose ps
```

### Building

```bash
# Build single service image
docker compose build patient-event-service

# Build multiple services sequentially
docker compose build patient-event-service
docker compose build quality-measure-event-service

# Build all images
docker compose build

# Build with fresh base images
docker compose build --pull patient-event-service

# Build without using cache layers
docker compose build --no-cache patient-event-service

# View build progress
docker compose build --progress=plain patient-event-service
```

### Logging

```bash
# View logs for service
docker compose logs patient-event-service

# Follow logs (tail -f behavior)
docker compose logs -f patient-event-service

# Show last 100 lines
docker compose logs --tail=100 patient-event-service

# Follow logs with timestamps
docker compose logs -f -t patient-event-service

# View logs for multiple services
docker compose logs -f patient-event-service quality-measure-event-service

# Search logs for specific pattern
docker compose logs patient-event-service | grep -i error

# Stream logs from all services
docker compose logs -f
```

### Debugging

```bash
# Execute command in running container
docker compose exec patient-event-service sh

# Execute as root
docker compose exec -u root patient-event-service sh

# Run command in container (without interactive shell)
docker compose exec patient-event-service ls -la /app

# Check environment variables in container
docker compose exec patient-event-service env | sort

# View container configuration
docker inspect hdim-postgres
```

### System Management

```bash
# Remove unused images
docker image prune -a

# Remove unused volumes
docker volume prune

# Remove unused networks
docker network prune

# Full cleanup (images, containers, volumes, networks)
docker system prune -a --volumes

# Show disk usage
docker system df

# Restart all services
docker compose restart

# Restart specific service
docker compose restart patient-event-service
```

---

## PostgreSQL Commands

### Connecting

```bash
# Connect to default database
docker exec -it hdim-postgres psql -U healthdata -d healthdata_qm

# Connect to specific database
docker exec -it hdim-postgres psql -U healthdata -d patient_db

# Connect with password prompt
docker exec -it hdim-postgres psql -U healthdata -W -d patient_db

# Connect as superuser
docker exec -it hdim-postgres psql -U postgres -d postgres
```

### Database Operations

```bash
# List all databases
docker exec -it hdim-postgres psql -U healthdata -c "\l"

# Create new database
docker exec -it hdim-postgres psql -U healthdata -c "CREATE DATABASE test_db;"

# Drop database
docker exec -it hdim-postgres psql -U healthdata -c "DROP DATABASE test_db;"

# View current database
docker exec -it hdim-postgres psql -U healthdata -c "\c patient_db"

# Show database size
docker exec -it hdim-postgres psql -U healthdata -c "SELECT pg_size_pretty(pg_database_size('patient_db'));"
```

### Schema Operations

```bash
# List all tables
docker exec -it hdim-postgres psql -U healthdata -d patient_db -c "\dt"

# Describe table structure
docker exec -it hdim-postgres psql -U healthdata -d patient_db -c "\d patients"

# List all indexes
docker exec -it hdim-postgres psql -U healthdata -d patient_db -c "\di"

# Show table size
docker exec -it hdim-postgres psql -U healthdata -d patient_db \
  -c "SELECT relname, pg_size_pretty(pg_total_relation_size(relid)) AS size FROM pg_catalog.pg_statio_user_tables ORDER BY size DESC;"

# View table row count
docker exec -it hdim-postgres psql -U healthdata -d patient_db \
  -c "SELECT schemaname, tablename, n_live_tup FROM pg_stat_user_tables;"
```

### Data Operations

```bash
# Run query from command line
docker exec -it hdim-postgres psql -U healthdata -d patient_db \
  -c "SELECT COUNT(*) FROM patients;"

# Run SQL script from file
docker exec -i hdim-postgres psql -U healthdata -d patient_db < script.sql

# Export data as CSV
docker exec -it hdim-postgres psql -U healthdata -d patient_db \
  -c "\COPY (SELECT * FROM patients) TO STDOUT CSV HEADER" > patients.csv

# Import data from CSV
docker exec -i hdim-postgres psql -U healthdata -d patient_db \
  -c "\COPY patients FROM STDIN CSV HEADER" < patients.csv

# Backup database
docker exec hdim-postgres pg_dump -U healthdata patient_db > patient_db_backup.sql

# Restore database
docker exec -i hdim-postgres psql -U healthdata patient_db < patient_db_backup.sql
```

### Liquibase Operations

```bash
# Check Liquibase status
docker exec -it hdim-postgres psql -U healthdata -d patient_db \
  -c "SELECT id, filename, orderexecuted FROM databasechangelog ORDER BY orderexecuted DESC LIMIT 10;"

# Clear Liquibase lock (emergency)
docker exec -it hdim-postgres psql -U healthdata -d patient_db \
  -c "DELETE FROM databasechangeloglock WHERE ID=1;"

# View pending migrations
docker exec -it hdim-postgres psql -U healthdata -d patient_db \
  -c "SELECT * FROM databasechangelog WHERE id NOT IN (SELECT DISTINCT id FROM databasechangelog WHERE LOCKED = false);"

# Manual migration registry update
docker exec -it hdim-postgres psql -U healthdata -d patient_db \
  -c "INSERT INTO databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum, description, comments, tag, liquibase, contexts, labels, deployment_id) VALUES ('0999-manual', 'admin', 'db/changelog/manual.xml', NOW(), (SELECT MAX(orderexecuted)+1 FROM databasechangelog), 'EXECUTED', 'manual', 'Manual entry', '', NULL, '4.27.0', NULL, NULL, NULL);"
```

---

## System Commands

### Local Development

```bash
# View all running containers
docker ps

# View all containers (including stopped)
docker ps -a

# Check disk usage
df -h

# Check available memory
free -h

# Check CPU usage
top

# Monitor processes real-time
htop
```

### Git Commands

```bash
# View current branch and status
git status

# View recent commits
git log --oneline -10

# Create new branch
git checkout -b feature/new-feature

# Push to remote
git push origin feature/new-feature

# Create pull request
gh pr create --title "Feature: New Feature" --body "Description"

# View PR status
gh pr view

# Check for uncommitted changes
git diff

# Stage changes
git add .

# Commit changes
git commit -m "feat: add new feature"

# View commit history
git log --oneline --graph --all
```

### Network Diagnostics

```bash
# Check if service is accessible
curl http://localhost:8084/patient/health

# Check all services
for port in 8080 8081 8084 8085 8086 8087; do
  echo "Port $port: $(curl -s http://localhost:$port/health || echo 'DOWN')"
done

# DNS resolution
nslookup hdim-postgres

# Check port availability
netstat -tlnp | grep 8084

# Trace network path to host
traceroute google.com
```

---

## Useful Aliases

Add to `.bashrc` or `.zshrc` for convenience:

```bash
# Docker shortcuts
alias dc='docker compose'
alias dcl='docker compose logs -f'
alias dcp='docker compose ps'
alias dcup='docker compose up -d'
alias dcdown='docker compose down'

# Gradle shortcuts (when in backend/)
alias gw='./gradlew'
alias gwb='./gradlew build'
alias gwt='./gradlew test'
alias gwc='./gradlew clean'

# PostgreSQL shortcuts
alias psql-hdim='docker exec -it hdim-postgres psql -U healthdata'
alias psql-patient='docker exec -it hdim-postgres psql -U healthdata -d patient_db'

# Git shortcuts
alias gs='git status'
alias gc='git commit -m'
alias gp='git push'
alias gl='git log --oneline -10'
```

---

## Quick Reference Cheat Sheet

### When Building Fails

```bash
# 1. Clean everything
cd backend
./gradlew clean --no-daemon

# 2. Download dependencies
./gradlew downloadDependencies --no-daemon

# 3. Rebuild
./gradlew build -x test

# 4. If Docker build fails
docker compose down -v
docker compose build --no-cache SERVICENAME
docker compose up -d SERVICENAME
```

### When Services Won't Start

```bash
# 1. Check logs
docker compose logs SERVICE

# 2. Check database connectivity
docker compose logs hdim-postgres | tail -20

# 3. Restart everything clean
docker compose down -v
docker compose up -d

# 4. Check if migrations are stuck
docker exec -it hdim-postgres psql -U healthdata -d SERVICE_db \
  -c "SELECT * FROM databasechangeloglock;"
```

### When Performance is Slow

```bash
# 1. Check container resources
docker stats --no-stream

# 2. Stop unnecessary containers
docker compose down

# 3. Prune unused resources
docker system prune -a --volumes

# 4. Increase Docker resource limits
# Edit Docker Desktop settings: Preferences → Resources
```

### When Database is Corrupted

```bash
# 1. Backup current data (if possible)
docker exec hdim-postgres pg_dump -U healthdata patient_db > backup.sql

# 2. Reset completely
docker compose down -v

# 3. Restart services (will re-initialize)
docker compose up -d

# 4. Verify migrations ran
docker compose logs SERVICE | grep -i liquibase
```

---

## Related Documentation

- [Build Management Guide](./BUILD_MANAGEMENT_GUIDE.md) - Detailed build troubleshooting
- [Database Architecture Guide](./DATABASE_ARCHITECTURE_GUIDE.md) - Database operations
- [Liquibase Development Workflow](./LIQUIBASE_DEVELOPMENT_WORKFLOW.md) - Advanced database procedures

---

_Last Updated: January 19, 2026_
_Version: 1.0_