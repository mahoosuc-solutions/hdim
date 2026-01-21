# Build and Test Commands - Nurse Workflow Service

## Quick Start

### Build Phase 1 Implementation
```bash
cd backend

# Build nurse-workflow-service with all tests
./gradlew :modules:services:nurse-workflow-service:build

# Or build without tests (faster initial verification)
./gradlew :modules:services:nurse-workflow-service:compileJava

# Build bootable JAR for Docker deployment
./gradlew :modules:services:nurse-workflow-service:bootJar
```

### Run Tests

#### Unit Tests Only (Fast - ~30 seconds)
```bash
./gradlew :modules:services:nurse-workflow-service:test
```

#### Integration Tests (Requires Docker - ~2 minutes)
```bash
# Ensure Docker daemon is running first
docker ps  # Verify Docker is available

# Run integration tests
./gradlew :modules:services:nurse-workflow-service:integrationTest
```

#### All Tests (Unit + Integration)
```bash
./gradlew :modules:services:nurse-workflow-service:build --tests "*Test"
```

#### Specific Test Class
```bash
# Run OutreachLogServiceTest only
./gradlew :modules:services:nurse-workflow-service:test --tests "*OutreachLogServiceTest"

# Run PatientEducationControllerTest only
./gradlew :modules:services:nurse-workflow-service:test --tests "*PatientEducationControllerTest"
```

#### Run Tests with Detailed Output
```bash
./gradlew :modules:services:nurse-workflow-service:test --info
```

## Verification Commands

### Check Compilation
```bash
# Compile without running tests
./gradlew :modules:services:nurse-workflow-service:compileJava

# Check test compilation
./gradlew :modules:services:nurse-workflow-service:compileTestJava
```

### List All Test Classes Found
```bash
# Show all tests the build system found
./gradlew :modules:services:nurse-workflow-service:test --dry-run
```

### Verify Dependencies
```bash
# Show dependency tree for the service
./gradlew :modules:services:nurse-workflow-service:dependencies
```

## Docker-Based Testing

### Start PostgreSQL for Integration Tests
```bash
# Start PostgreSQL container if needed
docker run -d \
  --name nurse-workflow-postgres \
  -e POSTGRES_DB=nurse_workflow_test \
  -e POSTGRES_USER=healthdata \
  -e POSTGRES_PASSWORD=test-password \
  -p 5432:5432 \
  postgres:16-alpine

# Verify it's running
docker logs nurse-workflow-postgres
```

### Stop Test Database
```bash
docker stop nurse-workflow-postgres
docker rm nurse-workflow-postgres
```

## Full Build Pipeline

### Complete Build with All Checks
```bash
cd backend

# Step 1: Clean previous builds
./gradlew :modules:services:nurse-workflow-service:clean

# Step 2: Verify Java version (must be 21+)
java -version

# Step 3: Compile code
./gradlew :modules:services:nurse-workflow-service:compileJava

# Step 4: Compile tests
./gradlew :modules:services:nurse-workflow-service:compileTestJava

# Step 5: Run all tests
./gradlew :modules:services:nurse-workflow-service:test

# Step 6: Create bootable JAR
./gradlew :modules:services:nurse-workflow-service:bootJar

# Step 7: Verify JAR was created
ls -lh modules/services/nurse-workflow-service/build/libs/nurse-workflow-service-*.jar
```

## Expected Test Results

### Unit Tests (30+ tests)
```
✓ OutreachLogServiceTest (8 tests)
  - Should create outreach log
  - Should retrieve by ID
  - Should get patient history
  - Should filter by outcome type
  - Should count operations
  - Should enforce multi-tenant isolation
  - Should update logs
  - Should calculate metrics

✓ MedicationReconciliationServiceTest (9 tests)
  - Should start reconciliation
  - Should complete reconciliation
  - Should retrieve by ID
  - Should get pending reconciliations
  - Should filter by trigger type
  - Should find poor understanding
  - Should count pending
  - Should verify multi-tenant isolation
  - Should update reconciliation

✓ PatientEducationServiceTest (8 tests)
  - Should log education delivery
  - Should retrieve by ID
  - Should get patient history
  - Should filter by material type
  - Should filter by delivery method
  - Should find poor understanding
  - Should calculate metrics
  - (Additional tests prepared)

✓ ReferralCoordinationServiceTest (5+ tests)
  - Should create referral
  - Should retrieve by ID
  - Should get pending referrals
  - Should find urgent referrals
  - Should calculate metrics
```

### REST Endpoint Tests (40+ tests)
```
✓ OutreachLogControllerTest (8 tests)
  - POST create (201)
  - GET single (200/404)
  - GET patient history (pagination)
  - GET filter by outcome
  - PUT update
  - DELETE
  - GET metrics
  - Missing tenant header validation

✓ MedicationReconciliationControllerTest (10 tests)
  - POST start reconciliation
  - PUT complete reconciliation
  - GET single
  - GET pending
  - GET patient history
  - GET by trigger type
  - PUT update
  - GET poor understanding
  - GET metrics
  - Missing header validation

✓ PatientEducationControllerTest (10 tests)
  - POST log education
  - GET single
  - GET patient history
  - GET by material type
  - GET by delivery method
  - GET by date range
  - GET poor understanding
  - GET interpreted sessions
  - PUT update
  - DELETE
  - GET metrics
  - Missing header validation

✓ ReferralCoordinationControllerTest (12 tests)
  - POST create referral
  - GET single
  - GET pending
  - GET patient history
  - GET by status
  - GET by specialty
  - GET awaiting scheduling
  - GET awaiting results
  - GET urgent
  - PUT update
  - GET metrics
  - Missing header validation
```

### Integration Tests (18 tests)
```
✓ NurseWorkflowServiceIntegrationTest
  - OutreachLog persistence
  - OutreachLog multi-tenant isolation
  - OutreachLog filter by outcome
  - MedicationReconciliation workflow
  - MedicationReconciliation pending
  - MedicationReconciliation poor understanding
  - PatientEducation persistence
  - PatientEducation material tracking
  - PatientEducation metrics
  - ReferralCoordination workflow
  - ReferralCoordination urgent scheduling
  - ReferralCoordination metrics
  - Cross-service complete patient workflow
```

## Troubleshooting

### Build Fails - Java Version Too Old
```bash
# Error: class file has wrong version 61.0, should be 55.0
# Solution: Upgrade to Java 21

java -version  # Check current version
# Expected: java version "21.x.x"

# On Mac with Homebrew
brew install openjdk@21
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

### Build Fails - Gradle Cache Issues
```bash
# Clear Gradle cache
./gradlew --stop
rm -rf ~/.gradle/caches
./gradlew :modules:services:nurse-workflow-service:build --refresh-dependencies
```

### Tests Fail - Docker Not Available
```bash
# Error: Cannot connect to Docker daemon

# Solution 1: Start Docker desktop (Mac/Windows)
# Solution 2: Start Docker daemon (Linux)
sudo systemctl start docker

# Solution 3: Run unit tests only (skip integration)
./gradlew :modules:services:nurse-workflow-service:test
```

### Tests Fail - Database Connection Issues
```bash
# Error: Could not create a connection to the database

# Solution: Use --info flag to see detailed logs
./gradlew :modules:services:nurse-workflow-service:integrationTest --info

# And check Docker
docker ps  # Verify TestContainers PostgreSQL started
docker logs <container_id>  # Check container logs
```

## Performance Benchmarks

### Build Times (Baseline)
```
Compilation only:        ~15 seconds
Unit tests only:         ~30 seconds
Integration tests:       ~90 seconds (includes Docker startup)
Full build with tests:   ~120 seconds
Bootable JAR:            ~20 seconds
```

### Test Execution (Expected Results)
```
OutreachLogServiceTest:                ~1.5 seconds (8 tests)
MedicationReconciliationServiceTest:   ~1.8 seconds (9 tests)
PatientEducationServiceTest:           ~1.5 seconds (8 tests)
ReferralCoordinationServiceTest:       ~1.2 seconds (5+ tests)

OutreachLogControllerTest:             ~2 seconds (8 tests)
MedicationReconciliationControllerTest: ~2.5 seconds (10 tests)
PatientEducationControllerTest:        ~2.5 seconds (10 tests)
ReferralCoordinationControllerTest:    ~3 seconds (12 tests)

NurseWorkflowServiceIntegrationTest:   ~15 seconds (18 tests, includes DB startup)

TOTAL EXECUTION TIME: ~55 seconds for all tests
```

## Deployment Verification

### Verify JAR Was Created
```bash
ls -lh backend/modules/services/nurse-workflow-service/build/libs/

# Expected output:
# -rw-r--r--  1 user  group  52M Jan 16 14:30 nurse-workflow-service-1.0.0.jar
```

### Test JAR Runs
```bash
java -jar backend/modules/services/nurse-workflow-service/build/libs/nurse-workflow-service-*.jar

# Expected output:
# Started NurseWorkflowServiceApplication in X.XXX seconds (JVM running in X.XXX)
#
# 🎯 Nurse Workflow Service (Port 8093)
# - Medication Reconciliation
# - Patient Education
# - Referral Coordination
# - Patient Outreach Logging
```

## IDE Configuration

### IntelliJ IDEA / WebStorm
```
1. Open Project Settings → Project Structure
2. Set Project SDK to Java 21
3. Set Language Level to 21
4. Open Run Configurations
5. Create JUnit test run for entire test folder
6. Click Run → Run All Tests
```

### VS Code
```
1. Install Extension Pack for Java
2. Install Gradle for Java
3. Open Terminal
4. Run: ./gradlew :modules:services:nurse-workflow-service:test
5. View results in Terminal
```

## Continuous Integration Commands

### For GitHub Actions / Jenkins
```bash
# Full test and build pipeline
./gradlew clean \
  :modules:services:nurse-workflow-service:build \
  :modules:services:nurse-workflow-service:test \
  :modules:services:nurse-workflow-service:integrationTest \
  :modules:services:nurse-workflow-service:bootJar \
  --info

# Report results
echo "Build Status: $?"  # 0 = success, non-zero = failure
```

## Docker Build and Run

### Build Docker Image
```bash
docker build \
  -f backend/modules/services/nurse-workflow-service/Dockerfile \
  -t nurse-workflow-service:latest \
  backend/modules/services/nurse-workflow-service/
```

### Run Service in Docker
```bash
docker run -d \
  --name nurse-workflow \
  -p 8093:8093 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e POSTGRES_HOST=postgres \
  -e POSTGRES_PORT=5432 \
  -e POSTGRES_DB=nurse_workflow_db \
  --network hdim-network \
  nurse-workflow-service:latest
```

### Docker Compose (Recommended)
```bash
# Using existing docker-compose.yml
docker-compose up -d nurse-workflow-service

# Check logs
docker-compose logs -f nurse-workflow-service

# Stop service
docker-compose stop nurse-workflow-service
```

## Success Criteria

✅ All commands complete without errors
✅ All 40+ tests pass (unit + integration + REST)
✅ Code compiles with Java 21+
✅ JAR file created successfully
✅ Service starts without errors
✅ All endpoints respond to HTTP requests
✅ Multi-tenant isolation enforced
✅ Database migrations executed
✅ OpenAPI documentation generated

---

**Next Step**: After successful build, proceed to Phase 2 (UI Implementation)

**Questions?** Refer to `NURSE_DASHBOARD_PHASE_1_COMPLETION.md` for detailed implementation info
