# Local Development Environment Setup Guide

Complete guide for setting up HDIM for local development. Follow this guide to get productive in **1 day** instead of 3-5 days.

**Last Updated**: January 19, 2026
**Status**: Complete setup guide for all platforms

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Initial Setup](#initial-setup)
3. [Running Services Locally](#running-services-locally)
4. [Database Management](#database-management)
5. [Debugging & Troubleshooting](#debugging--troubleshooting)
6. [IDE Configuration](#ide-configuration)
7. [Development Workflows](#development-workflows)

---

## Prerequisites

### System Requirements

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| **RAM** | 8 GB | 16 GB+ |
| **CPU Cores** | 2 | 4+ |
| **Disk Space** | 50 GB | 100 GB+ |
| **OS** | macOS 11+, Ubuntu 20.04+, Windows 10+ | macOS 12+, Ubuntu 22.04+, Windows 11 |

### Required Software

**Java 21 (LTS)**
```bash
# macOS (using Homebrew)
brew install openjdk@21

# Ubuntu/Debian
sudo apt-get install openjdk-21-jdk

# Windows (using Chocolatey)
choco install openjdk21

# Verify installation
java -version
# Should show: openjdk version "21.x.x"

# Set JAVA_HOME environment variable
export JAVA_HOME=$(/usr/libexec/java_home -v 21)  # macOS
# OR add to ~/.bashrc / ~/.zshrc / System Environment Variables
```

**Gradle 8.11+**
```bash
# macOS
brew install gradle

# Ubuntu/Debian
sudo apt-get install gradle

# Windows (using Chocolatey)
choco install gradle

# Verify installation
gradle --version
# Should show: Gradle 8.11+
```

**Docker Desktop 24.0+**
```bash
# Download from https://www.docker.com/products/docker-desktop

# Verify installation
docker --version
# Should show: Docker version 24.0+

docker run hello-world
# Should print: Hello from Docker!
```

**Docker Desktop Memory Configuration** (IMPORTANT!)
```bash
# Open Docker Desktop Settings → Resources → Memory
# Set to at least 8 GB (preferably 10-12 GB)
# This prevents OOM errors when running Kafka, Redis, PostgreSQL, and services

# Verify Docker can access memory
docker run --rm -m 8g ubuntu echo "Docker has access to 8GB RAM"
```

**Git 2.30+**
```bash
# macOS
brew install git

# Ubuntu/Debian
sudo apt-get install git

# Windows
# Download from https://git-scm.com/download/win

# Verify installation
git --version
# Should show: git version 2.30+
```

**Node.js 20+ (for Frontend)**
```bash
# macOS
brew install node@20

# Ubuntu/Debian
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs

# Windows (using Chocolatey)
choco install nodejs

# Verify installation
node --version
# Should show: v20.x.x

npm --version
# Should show: 9.x.x+
```

### Optional but Recommended Tools

- **IDE**: IntelliJ IDEA 2024+ (Community or Ultimate) or VS Code
- **Database Tools**: DBeaver Community or pgAdmin 4
- **API Testing**: Postman or Insomnia
- **Container Inspection**: Docker Desktop Dashboard (included)
- **Git Client**: GitKraken or GitHub Desktop (optional, CLI works fine)

---

## Initial Setup

### Step 1: Clone the Repository

```bash
# Clone HDIM repository
git clone https://github.com/your-org/healthdata-in-motion.git
cd healthdata-in-motion

# Set default branch to master (if needed)
git checkout master
git branch -u origin/master master
```

### Step 2: Create Environment Configuration

Create `.env.local` in project root:

```bash
# Create file
cp .env.example .env.local  # If example exists
# OR create manually
cat > .env.local << 'EOF'
# === PostgreSQL Configuration ===
POSTGRES_HOST=localhost
POSTGRES_PORT=5435
POSTGRES_DB=healthdata
POSTGRES_USER=healthdata
POSTGRES_PASSWORD=healthdata_password

# === Redis Configuration ===
REDIS_HOST=localhost
REDIS_PORT=6380
REDIS_PASSWORD=redis_password

# === Kafka Configuration ===
KAFKA_BOOTSTRAP_SERVERS=localhost:9094
KAFKA_ZOOKEEPER_CONNECT=localhost:2182

# === Application Configuration ===
ENVIRONMENT=development
LOG_LEVEL=INFO

# === JWT Configuration (Development) ===
JWT_SECRET=dev_secret_key_change_in_production_minimum_64_characters_long_here_xxx
JWT_EXPIRATION_MS=900000

# === Jaeger Tracing (Development) ===
JAEGER_ENABLED=true
JAEGER_AGENT_HOST=localhost
JAEGER_AGENT_PORT=6831

# === Spring Profiles ===
SPRING_PROFILES_ACTIVE=dev

# === Development Flags ===
DEBUG=false
GATEWAY_AUTH_DEV_MODE=true
EOF
```

### Step 3: Verify Java & Gradle Setup

```bash
# Verify Java 21
java -version

# Verify JAVA_HOME is set
echo $JAVA_HOME
# Should output: /Library/Java/JavaVirtualMachines/openjdk-21.jdk/Contents/Home (macOS example)

# If not set, add to shell profile (~/.bashrc, ~/.zshrc, etc.)
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# Verify Gradle
./gradlew --version

# Test Gradle build (from backend/ directory)
cd backend
./gradlew build -x test
```

### Step 4: Install Frontend Dependencies

```bash
# From project root
npm install

# Verify installation
npm list | head -20
```

---

## Running Services Locally

### Option A: Full Stack with Docker Compose (Recommended for Beginners)

```bash
# Start all services (core profile)
docker compose --profile core up -d

# OR start extended profile (includes analytics, predictive services)
docker compose --profile extended up -d

# Verify services are running
docker compose ps

# Expected output:
# NAME                     STATUS              PORTS
# healthdata-postgres      Up 2 minutes        5435->5432/tcp
# healthdata-redis         Up 2 minutes        6380->6379/tcp
# healthdata-kafka         Up 2 minutes        9094->9093/tcp
# healthdata-zookeeper     Up 2 minutes        2182->2181/tcp
# healthdata-grafana       Up 2 minutes        3001->3000/tcp
# healthdata-prometheus    Up 2 minutes        9090->9090/tcp
# healthdata-jaeger        Up 2 minutes        16686->16686/tcp
# gateway-service          Up 1 minute         8001->8001/tcp
# quality-measure-service  Up 1 minute         8087->8087/tcp
# ... (more services)

# Check service logs
docker compose logs -f gateway-service
docker compose logs -f quality-measure-service
```

**Docker Compose Profiles:**
- `core`: Gateway, CQL, Quality Measure, FHIR, Patient, Care Gap, Consent services
- `extended`: All core + Analytics, Predictive, SDOH, Data Enrichment
- `full`: Everything including event-driven services

---

### Option B: Running Services from IDE (Advanced)

#### IntelliJ IDEA 2024+

1. **Open Project**
   - File → Open → Select `healthdata-in-motion` directory
   - Accept "Load project configuration from Gradle"

2. **Configure JDK**
   - File → Project Structure → Project
   - Set SDK to Java 21
   - Build language level: Java 21

3. **Run Individual Service**
   - Expand: `backend/modules/services/quality-measure-service`
   - Find: `src/main/java/.../QualityMeasureServiceApplication.java`
   - Right-click → Run 'QualityMeasureServiceApplication'
   - Service starts on port 8087

4. **Create Run Configuration**
   - Run → Edit Configurations
   - Click `+` → Spring Boot
   - Name: "Quality Measure Service"
   - Main class: `com.healthdata.quality.QualityMeasureServiceApplication`
   - VM options: `-Dspring.profiles.active=dev`
   - Environment variables: Paste contents from `.env.local`
   - Click OK → Click Run

#### VS Code

1. **Install Extensions**
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - REST Client (for API testing)

2. **Open Project**
   - File → Open Folder → Select `healthdata-in-motion`

3. **Run Service**
   - Terminal → New Terminal
   - Navigate: `cd backend/modules/services/quality-measure-service`
   - Run: `../../gradlew bootRun`
   - Service starts on port 8087

---

### Option C: Hybrid Approach (Recommended for Experienced Devs)

Run infrastructure (database, cache, messaging) in Docker, services from IDE:

```bash
# Terminal 1: Start only infrastructure services
docker compose --profile infrastructure up -d
# Runs: postgres, redis, kafka, zookeeper, jaeger, prometheus, grafana

# Terminal 2: Run service from IDE or command line
cd backend/modules/services/quality-measure-service
../../gradlew bootRun

# Terminal 3: Run another service
cd backend/modules/services/cql-engine-service
../../gradlew bootRun

# This approach provides:
# ✅ Fast code reload with Spring DevTools
# ✅ Easy debugging
# ❌ Manual service startup
```

---

## Database Management

### Initial Database Setup

**Liquibase Migrations Auto-Run**
```bash
# Services automatically run Liquibase migrations on startup
# No manual action needed!

# Check migration status
docker compose exec postgres psql -U healthdata -d quality_db << EOF
SELECT * FROM databasechangelog ORDER BY orderexecuted DESC LIMIT 10;
EOF
```

### Database Access

**Connect with CLI**
```bash
# PostgreSQL CLI
docker compose exec postgres psql -U healthdata -d quality_db
# Useful commands:
# \dt               - List tables
# \d table_name     - Describe table
# SELECT COUNT(*) FROM patients;  - Count records
# \q               - Quit

# List all databases
docker compose exec postgres psql -U healthdata -l
```

**Connect with GUI Tools**
```
DBeaver:
- Host: localhost
- Port: 5435
- Database: quality_db (or any service database)
- Username: healthdata
- Password: healthdata_password

pgAdmin:
- URL: http://localhost:5050
- Email: admin@example.com
- Password: admin
```

### Database Seeding with Demo Data

```bash
# Some services include demo data seeders
# They automatically run on startup if conditions are met

# Manually trigger seeding (if needed)
curl -X POST http://localhost:8084/patient/api/v1/seed-demo-data \
  -H "Authorization: Bearer $(get_dev_token)" \
  -H "X-Tenant-ID: demo-tenant"

# Check if demo patients were created
docker compose exec postgres psql -U healthdata -d patient_db << EOF
SELECT COUNT(*) as patient_count FROM patients;
EOF
```

### Resetting Database

```bash
# Drop and recreate a specific database
docker compose exec postgres psql -U healthdata << EOF
DROP DATABASE IF EXISTS quality_db;
CREATE DATABASE quality_db;
GRANT ALL PRIVILEGES ON DATABASE quality_db TO healthdata;
EOF

# Restart the service to trigger Liquibase migrations
docker compose restart quality-measure-service

# Wait for service to become healthy
docker compose exec quality-measure-service curl -s http://localhost:8087/actuator/health | jq .
```

### Clearing Liquibase Locks (If Migration Stuck)

```bash
# Use provided recovery script
./backend/scripts/clear-liquibase-locks.sh

# OR manually clear for specific database
docker compose exec postgres psql -U healthdata -d quality_db << EOF
UPDATE databasechangeloglock SET LOCKED=false WHERE ID=1;
EOF
```

---

## Debugging & Troubleshooting

### Enable Debug Logging

**Option 1: Environment Variable**
```bash
# In .env.local
LOG_LEVEL=DEBUG

# Restart services
docker compose restart quality-measure-service
```

**Option 2: Application Configuration**
```yaml
# In application-dev.yml
logging:
  level:
    root: INFO
    com.healthdata: DEBUG
    org.springframework: WARN
    org.springframework.security: DEBUG
```

### Remote Debugging from IDE

```bash
# Option A: Docker Compose with Debug Port
# Modify docker-compose.yml for service:
services:
  quality-measure-service:
    ports:
      - "8087:8087"
      - "5005:5005"  # Add debug port
    environment:
      - JAVA_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005

# Then in IDE:
# Run → Edit Configurations
# Click + → Remote JVM Debug
# Name: "Quality Measure Debug"
# Host: localhost
# Port: 5005
# Click OK → Run
```

### View Logs

```bash
# Docker Compose logs
docker compose logs -f quality-measure-service

# Last 100 lines of logs
docker compose logs --tail=100 quality-measure-service

# Logs from past 10 minutes
docker compose logs --since 10m quality-measure-service

# Search logs for errors
docker compose logs quality-measure-service | grep -i error

# Stream logs from multiple services
docker compose logs -f gateway-service quality-measure-service
```

### Jaeger Distributed Tracing

```bash
# Access Jaeger UI
# http://localhost:16686

# Query examples in Jaeger:
# - Service: quality-measure-service
# - Operation: GET /api/v1/measures
# - Tags: span.kind=server error=true
# - Min Duration: 500ms

# This shows complete request traces across all services
```

### Common Issues & Solutions

**Issue: "Port 5435 already in use"**
```bash
# Find process using port
lsof -i :5435

# Kill process
kill -9 <PID>

# OR change port in docker-compose.override.yml
# And restart: docker compose restart postgres
```

**Issue: "Cannot connect to Docker daemon"**
```bash
# Ensure Docker Desktop is running
# macOS: Start Docker Desktop.app

# Verify Docker daemon
docker ps

# If still fails, restart daemon
# macOS: docker-machine restart default
# Linux: sudo systemctl restart docker
```

**Issue: "Out of memory" errors**
```bash
# Increase Docker Desktop memory
# Docker Desktop → Settings → Resources → Memory: 10-12 GB

# Check current memory
docker stats --no-stream
```

**Issue: "Service won't start - validation error"**
```bash
# Check service logs
docker compose logs quality-measure-service

# If: "Schema-validation: missing table"
# → Database migration didn't run
# → Check Liquibase lock: ./backend/scripts/clear-liquibase-locks.sh
# → Restart service: docker compose restart quality-measure-service

# If: "Connection refused"
# → PostgreSQL might not be ready
# → Wait 10 seconds and retry
# → Check: docker compose logs postgres
```

**Issue: "gradle build hangs"**
```bash
# Kill hung Gradle daemon
./gradlew --stop

# Clear Gradle cache
rm -rf ~/.gradle/caches

# Rebuild
./gradlew build
```

---

## IDE Configuration

### IntelliJ IDEA Setup (Recommended)

**Code Inspection & Formatting**
```
Settings → Editor → Code Style → Java
- Tab size: 4 spaces
- Indent: 4 spaces
- Continuation indent: 8 spaces

Settings → Editor → Inspections
- Enable: Spring Boot
- Enable: Spring Data
- Enable: Security issues
```

**Run/Debug Configuration**
```
Run → Edit Configurations → Templates → Spring Boot
- VM options: -Dspring.profiles.active=dev
- Environment variables: <paste .env.local content>
- Working directory: <project root>
```

**Maven/Gradle Configuration**
```
Settings → Build, Execution, Deployment → Gradle
- Gradle JVM: Java 21
- Build and run using: Gradle
- Run tests using: Gradle
```

### VS Code Setup

**Extensions to Install**
```
- Extension Pack for Java (Microsoft)
- Spring Boot Extension Pack (VMware)
- Spring Boot Dashboard (VMware)
- REST Client (Huachao Mao)
- Docker (Microsoft)
- GitLens (Eric Amodio)
```

**Workspace Settings (.vscode/settings.json)**
```json
{
  "java.home": "/path/to/java21",
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-21",
      "path": "/path/to/java21",
      "default": true
    }
  ],
  "[java]": {
    "editor.defaultFormatter": "redhat.java",
    "editor.formatOnSave": true
  }
}
```

---

## Development Workflows

### Typical Development Day

**Morning: Start Services**
```bash
# Terminal 1: Start infrastructure
docker compose --profile core up -d

# Wait for services to be healthy
docker compose exec postgres pg_isready
```

**Develop: Edit Code & Run Tests**
```bash
# Terminal 2: Watch and rebuild on changes
cd backend/modules/services/quality-measure-service
../../gradlew build --continuous

# Run specific test
../../gradlew test --tests "MeasureServiceTest"

# Run tests with coverage
../../gradlew test jacocoTestReport
```

**Test API: Make Requests**
```bash
# Get development token
export TOKEN=$(curl -s -X POST http://localhost:8001/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test_admin","password":"password123"}' | jq -r .token)

# Make API request
curl -X GET http://localhost:8087/quality-measure/api/v1/measures \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: demo-tenant" | jq

# View in Postman collection (see docs/api/)
```

**Evening: Clean Up**
```bash
# Stop services
docker compose down

# Cleanup volumes (optional, removes data)
docker compose down -v

# Check what's running
docker ps -a
```

### Git Workflow

```bash
# Create feature branch
git checkout -b feature/my-feature

# Make changes, commit
git add .
git commit -m "Add feature: description"

# Run tests before pushing
./gradlew test

# Push to remote
git push origin feature/my-feature

# Create PR in GitHub
# Include: description, testing notes, related issues
```

### Common Dev Commands Cheatsheet

```bash
# Build all backend services
cd backend && ./gradlew build -x test

# Run all tests
cd backend && ./gradlew test

# Run specific service tests
cd backend && ./gradlew :modules:services:quality-measure-service:test

# Start one service locally
cd backend/modules/services/quality-measure-service
../../gradlew bootRun

# Build and push Docker image
docker build -t hdim/quality-measure-service:latest .
docker push hdim/quality-measure-service:latest

# View logs
docker compose logs -f <service-name>

# Check service health
curl http://localhost:8087/actuator/health

# Access database
docker compose exec postgres psql -U healthdata -d quality_db

# Clear Liquibase locks
./backend/scripts/clear-liquibase-locks.sh

# Run specific test in IDE
# IntelliJ: Right-click test → Run
# VS Code: Click "Run Test" above test method
```

---

## Quick Reference: Service Ports

| Service | Port | URL |
|---------|------|-----|
| **Gateway** | 8001 | http://localhost:8001 |
| **CQL Engine** | 8081 | http://localhost:8081 |
| **Consent** | 8082 | http://localhost:8082 |
| **Patient** | 8084 | http://localhost:8084 |
| **FHIR** | 8085 | http://localhost:8085 |
| **Care Gap** | 8086 | http://localhost:8086 |
| **Quality Measure** | 8087 | http://localhost:8087 |
| **PostgreSQL** | 5435 | localhost:5435 |
| **Redis** | 6380 | localhost:6380 |
| **Kafka** | 9094 | localhost:9094 |
| **Jaeger UI** | 16686 | http://localhost:16686 |
| **Prometheus** | 9090 | http://localhost:9090 |
| **Grafana** | 3001 | http://localhost:3001 |

---

## Next Steps

- ✅ **Environment Setup Complete!** You should now be able to run services.
- 📖 **See [TESTING_STRATEGY.md](TESTING_STRATEGY.md)** for how to write and run tests
- 🔍 **See [CLAUDE.md](../../CLAUDE.md)** for coding patterns and best practices
- 🐛 **See [docs/troubleshooting/README.md](../troubleshooting/README.md)** if you hit issues
- 🔧 **See [CI_CD_GUIDE.md](CI_CD_GUIDE.md)** to understand the build pipeline

---

**Last Updated**: January 19, 2026
**Maintained by**: HDIM Platform Team
**Status**: Complete - Tested for macOS, Ubuntu, Windows
