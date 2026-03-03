# Integration Test Execution Guide

**Date**: January 15, 2026  
**Status**: Ready to Execute  
**Tests**: Decision Replay & QA Per-Agent Statistics

---

## Prerequisites

### 1. Docker Setup

**Check Docker Status**:
```bash
docker ps
```

**If Docker is not running**, start it:

**Linux (WSL2)**:
```bash
# Check if Docker service is running
sudo service docker status

# Start Docker service
sudo service docker start

# Verify Docker is running
docker ps
```

**Windows (Docker Desktop)**:
- Open Docker Desktop application
- Wait for Docker to start
- Verify in terminal: `docker ps`

**Mac (Docker Desktop)**:
- Open Docker Desktop application
- Wait for Docker to start
- Verify in terminal: `docker ps`

### 2. Verify Testcontainers Access

Testcontainers needs access to Docker. Verify:
```bash
# Should return Docker version
docker version

# Should list containers (may be empty)
docker ps
```

---

## Test Execution

### Step 1: Navigate to Backend Directory
```bash
cd /home/webemo-aaron/projects/hdim-master/backend
```

### Step 2: Execute Integration Tests

**Run All Integration Tests**:
```bash
./gradlew :modules:shared:infrastructure:audit:test --tests "*IntegrationTest" --no-daemon
```

**Run Specific Test Class**:
```bash
# QA Review Service Integration Tests
./gradlew :modules:shared:infrastructure:audit:test --tests "QAReviewServicePerAgentIntegrationTest" --no-daemon

# Decision Replay Service Integration Tests
./gradlew :modules:shared:infrastructure:audit:test --tests "DecisionReplayServiceIntegrationTest" --no-daemon
```

### Step 3: Review Test Results

**Expected Output**:
- All tests should pass
- Testcontainers will start PostgreSQL container automatically
- Tests will run against real database
- Container will be cleaned up after tests

**If Tests Fail**:
1. Check Docker is running
2. Check test logs for errors
3. Verify database connection
4. Review test output for specific failures

---

## Test Files

### 1. QAReviewServicePerAgentIntegrationTest ✅
**Location**: `backend/modules/shared/infrastructure/audit/src/test/java/com/healthdata/audit/integration/QAReviewServicePerAgentIntegrationTest.java`

**Tests** (4 tests):
- `testGetMetrics_PerAgentStatistics_FromDatabase`
- `testGetAccuracyTrends_PerAgentTrends_FromDatabase`
- `testGetMetrics_PerAgentStatistics_Filtered_FromDatabase`
- `testPerformance_WithLargeDataset`

### 2. DecisionReplayServiceIntegrationTest ✅
**Location**: `backend/modules/shared/infrastructure/audit/src/test/java/com/healthdata/audit/integration/DecisionReplayServiceIntegrationTest.java`

**Tests** (6 tests):
- `testReplayDecision_FromDatabase_WithAgentService`
- `testReplayDecision_FromDatabase_ValidationFallback`
- `testReplayDecisionBatch_FromDatabase`
- `testReplayDecisionChain_FromDatabase`
- `testDecisionPersistence_AndRetrieval`
- `testPerformance_WithMultipleDecisions`

---

## Troubleshooting

### Issue: Docker Not Available

**Error**: `Could not find a valid Docker environment`

**Solution**:
1. Start Docker service (see Prerequisites)
2. Verify Docker is accessible: `docker ps`
3. Re-run tests

### Issue: Testcontainers Cannot Pull Image

**Error**: `Failed to pull image postgres:16-alpine`

**Solution**:
1. Check internet connection
2. Manually pull image: `docker pull postgres:16-alpine`
3. Re-run tests

### Issue: Port Already in Use

**Error**: `Port already in use`

**Solution**:
1. Check if PostgreSQL container is already running: `docker ps`
2. Stop existing container: `docker stop <container-id>`
3. Re-run tests

### Issue: Test Failures

**Check**:
1. Review test output for specific error messages
2. Verify database schema is created correctly
3. Check entity mappings
4. Review test data setup

---

## Expected Test Results

### Success Criteria
- ✅ All 10 integration tests pass
- ✅ Testcontainers starts PostgreSQL successfully
- ✅ Database operations complete successfully
- ✅ All assertions pass
- ✅ Performance tests meet targets

### Test Execution Time
- **QA Review Tests**: ~5-10 seconds
- **Decision Replay Tests**: ~10-15 seconds
- **Total**: ~15-25 seconds

---

## Next Steps After Tests Pass

1. **Document Results**: Update test execution report
2. **Update Release Readiness**: Mark integration tests as complete
3. **Proceed to Day 2**: Staging environment setup

---

**Status**: Ready to Execute  
**Blocking**: Docker availability  
**Next Action**: Start Docker and execute tests
