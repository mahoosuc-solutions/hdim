# HDIM Testing Guide

**Last Updated:** January 2026
**Version:** 1.0

This guide covers the comprehensive testing infrastructure for the HealthData-in-Motion (HDIM) platform, including gateway smoke tests, demo platform validation, and end-to-end workflow testing.

---

## Table of Contents

1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [Test Scripts](#test-scripts)
4. [Gateway Smoke Tests](#gateway-smoke-tests)
5. [Demo Platform Testing](#demo-platform-testing)
6. [End-to-End Workflow](#end-to-end-workflow)
7. [System Health Runner](#system-health-runner)
8. [CI/CD Integration](#cicd-integration)
9. [Troubleshooting](#troubleshooting)
10. [Best Practices](#best-practices)

---

## Overview

The HDIM testing infrastructure provides comprehensive validation across multiple layers:

- **Gateway Testing**: Validates all 29 microservices, circuit breakers, rate limiting, authentication
- **Demo Platform**: Scenario-based patient data seeding (5K-10K patients per scenario)
- **End-to-End Workflow**: Complete FHIR → CQL Engine → Quality Measure → Care Gap flow
- **System Health**: Orchestrated testing of infrastructure, services, and workflows

### Test Coverage

| Component | Test Count | Coverage |
|-----------|------------|----------|
| Gateway Routes | 58+ tests | All 29 services × 2 paths |
| Authentication | 8 tests | JWT, cookies, header injection |
| Circuit Breakers | 29 tests | One per service |
| Rate Limiting | 4 tests | Auth, API, health, thresholds |
| Demo Scenarios | 4 strategies | 19K total patients |
| E2E Workflow | 14 steps | Full quality measure flow |

---

## Quick Start

### Prerequisites

```bash
# Verify prerequisites
docker --version          # Docker 24.0+
docker compose version    # Docker Compose 2.0+
java -version             # Java 21
./gradlew --version       # Gradle 8.11+
jq --version              # jq (JSON processor)
```

### Run Quick Validation (5 minutes)

```bash
# Start infrastructure
docker compose up -d

# Run quick system health check
./scripts/test-system-health.sh

# Expected output:
# ✓ Infrastructure Health
# ✓ Gateway Smoke Test
# ✓ Authentication Tests
# ✓ Demo Platform Validation
# ✓ End-to-End Workflow
```

### Run Full Demo Validation (10-15 minutes)

```bash
# Load full demo (19K patients)
DEMO_MODE=full ./scripts/test-system-health.sh

# This loads:
# - HEDIS Evaluation: 5,000 patients (28% care gaps)
# - Patient Journey: 1,000 patients (35% care gaps)
# - Risk Stratification: 10,000 patients (25% care gaps)
# - Multi-Tenant: 3,000 patients across 3 tenants
```

---

## Test Scripts

### Script Organization

```
scripts/
├── lib/
│   └── gateway-test-helpers.sh       # Reusable test functions
├── config/
│   └── gateway-service-routes.json   # Service configuration
├── test-gateway-smoke.sh              # Gateway smoke tests
├── test-end-to-end-workflow.sh        # E2E workflow tests
├── test-system-health.sh              # Master test orchestrator
└── test-authentication-flow.sh        # Auth validation (existing)
```

### Common Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DEMO_MODE` | `quick` | Test mode: `quick` (100 patients) or `full` (19K patients) |
| `TENANT_ID` | `acme-health` | Primary tenant ID for testing |
| `SKIP_BUILD` | `false` | Skip Gradle build step |
| `CLEANUP` | `true` | Cleanup Docker containers after tests |
| `RUN_GATEWAY_SMOKE` | `true` | Run gateway smoke test in E2E workflow |
| `SKIP_E2E` | `false` | Skip end-to-end workflow tests |
| `OUTPUT_DIR` | `/tmp/hdim-health-tests` | Output directory for test reports |

---

## Gateway Smoke Tests

### Overview

The gateway smoke test validates all 29 HDIM microservices through the API Gateway, testing routing, circuit breakers, rate limiting, and authentication.

### Usage

```bash
# Quick test (6 core services)
./scripts/test-gateway-smoke.sh --mode=quick

# Full test (all 29 services)
./scripts/test-gateway-smoke.sh --mode=full

# Generate JSON report
./scripts/test-gateway-smoke.sh --mode=full --output gateway-report.json

# Skip circuit breaker tests (faster)
./scripts/test-gateway-smoke.sh --mode=quick --skip-circuit-breakers

# Skip rate limiting tests
./scripts/test-gateway-smoke.sh --mode=full --skip-rate-limiting

# Verbose output
./scripts/test-gateway-smoke.sh --mode=full --verbose
```

### Test Categories

#### 1. Service Routing (58 tests)
Tests both API path and direct path for each service:
- `/api/{service-name}/*` - API gateway route
- `/{service-name}/*` - Direct service route

Example:
```bash
✓ Route test: /api/quality-measure/api/v1/measures
✓ Route test: /quality-measure/api/v1/measures
```

#### 2. Authentication (8 tests)
- JWT token generation via `/api/auth/login`
- Token validation via `/api/auth/me`
- Unauthenticated request rejection (401)
- Header injection prevention (malicious `X-Auth-*` headers)

#### 3. Circuit Breakers (29 tests)
Validates circuit breaker state for each service:
- `CLOSED` - Service healthy, requests pass through
- `OPEN` - Service failed, requests blocked
- `HALF_OPEN` - Testing if service recovered

#### 4. Rate Limiting (4 tests)
- Auth endpoint rate limiting (10 req/min)
- API endpoint rate limiting (100 req/sec)
- Health endpoint bypass (unlimited)
- Threshold validation

### Exit Codes

| Code | Meaning |
|------|---------|
| 0 | All tests passed |
| 1 | Critical service failure (gateway, auth, core services) |
| 2 | Partial failure (extended services) |
| 3 | Configuration error |

### Report Format

```json
{
  "timestamp": "2026-01-10T12:34:56Z",
  "summary": {
    "totalTests": 58,
    "passed": 54,
    "failed": 4,
    "skipped": 0,
    "passRate": 93.1
  },
  "tests": [
    {
      "name": "Health check: quality-measure",
      "status": "PASS",
      "message": "Service is healthy",
      "timestamp": "2026-01-10T12:34:57Z"
    }
  ]
}
```

### Service Configuration

Edit `scripts/config/gateway-service-routes.json` to modify service list:

```json
{
  "core_services": [
    {
      "name": "quality-measure",
      "port": 8087,
      "critical": true,
      "timeout": 30000,
      "healthPath": "/actuator/health",
      "testPath": "/api/v1/measures"
    }
  ]
}
```

---

## Demo Platform Testing

### Overview

The demo platform provides scenario-based patient data seeding with 4 pre-configured strategies optimized for different use cases.

### Demo Scenarios

#### 1. HEDIS Evaluation (5,000 patients)
**Purpose:** Quality measure evaluation at scale

**Characteristics:**
- 28% care gap rate (1,400 patients with gaps)
- Age distribution: 25% young (18-40), 40% middle-aged (41-65), 35% elderly (66+)
- Condition prevalence: Diabetes 25%, Hypertension 40%, COPD 8%, CHF 5%, CKD 10%
- Quality measures: BCS, COL, CBP, CDC, EED, KED

**Use Cases:**
- Star ratings simulation
- Provider performance dashboards
- Care gap identification

#### 2. Patient Journey (1,000 patients)
**Purpose:** Clinical workflow demonstrations

**Characteristics:**
- 35% care gap rate
- Rich clinical histories (5-10 encounters, 8-15 observations per patient)
- 4 named personas for demos
- Detailed medication and procedure histories

**Use Cases:**
- Training and education
- Care coordination workflows
- Pre-visit planning demonstrations

#### 3. Risk Stratification (10,000 patients)
**Purpose:** Population health management

**Characteristics:**
- 25% care gap rate
- HCC risk distribution: 60% Low, 30% Moderate, 10% High
- Condition clustering by risk tier
- Medicare-age focused (50% ages 66+)

**Use Cases:**
- Predictive analytics
- Risk stratification algorithms
- Medicare Advantage risk adjustment

#### 4. Multi-Tenant (3,000 patients)
**Purpose:** Multi-tenancy testing

**Characteristics:**
- 1,000 patients per tenant (3 tenants)
- Tenants: acme-health, blue-shield-demo, united-demo
- 30% care gap rate per tenant
- Independent data isolation

**Use Cases:**
- Data isolation validation
- Security boundary testing
- Tenant migration simulations

### Loading Demo Scenarios

#### Via Test Scripts

```bash
# Quick mode (100 patients)
./scripts/test-end-to-end-workflow.sh

# Full mode (all 4 scenarios, 19K patients)
DEMO_MODE=full ./scripts/test-end-to-end-workflow.sh
```

#### Via Java API

```java
@Autowired
private HedisEvaluationStrategy hedisStrategy;

@Autowired
private PatientJourneyStrategy journeyStrategy;

@Autowired
private RiskStratificationStrategy riskStrategy;

@Autowired
private MultiTenantStrategy multiTenantStrategy;

// Load HEDIS Evaluation scenario
SeedingResult result = hedisStrategy.seedScenario("acme-health");
System.out.println(result);
// Output: SeedingResult{scenario='hedis-evaluation', patients=5000, ...}

// Load all scenarios
multiTenantStrategy.seedScenario("acme-health");
```

#### Via REST API

```bash
# Seed 5,000 patients with 28% care gaps (HEDIS Evaluation)
curl -X POST http://localhost:8108/api/v1/demo/seed \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: acme-health" \
  -d '{"count": 5000, "careGapPercentage": 28}'

# Response:
# {
#   "patientsCreated": 5000,
#   "observationsCreated": 25000,
#   "proceduresCreated": 15000,
#   "encountersCreated": 18000,
#   "medicationsCreated": 10000,
#   "careGapsExpected": 1400
# }
```

### Demo Verification

Verify demo data quality using DemoVerificationService:

```java
@Autowired
private DemoVerificationService verificationService;

// Verify specific scenario
VerificationResult result = verificationService.verifyScenario(
    "hedis-evaluation",
    "acme-health"
);

System.out.println(result.isPassed()); // true/false
System.out.println(result.getPassedChecks() + "/" + result.getTotalChecks());

// Verify all scenarios
Map<String, VerificationResult> results =
    verificationService.verifyAllScenarios("acme-health");
```

---

## End-to-End Workflow

### Overview

The end-to-end workflow test validates the complete quality measure evaluation pipeline from FHIR data ingestion through care gap identification.

### Workflow Steps

1. **Infrastructure Startup** - PostgreSQL, Redis, Kafka
2. **Service Build** - Gradle build (optional with `SKIP_BUILD=true`)
3. **Service Startup** - FHIR, CQL Engine, Quality Measure, Demo Seeding
4. **Service Health Checks** - Wait for all services to be ready
5. **Measure Verification** - Verify HEDIS measures auto-seeded
6. **Value Set Verification** - Verify code systems loaded
7. **Gateway Smoke Test** (optional) - Validate gateway routing
8. **Patient Data Seeding** - Load demo scenarios
9. **Patient Verification** - Confirm patients in FHIR service
10. **Measure Evaluation** - Run BCS and CBP measures
11. **Result Validation** - Verify measures persisted
12. **Care Gap Detection** - Check for identified gaps
13. **Summary Report** - Aggregate results

### Usage

```bash
# Quick test (100 patients, ~5 minutes)
./scripts/test-end-to-end-workflow.sh

# Full demo (19K patients, ~15 minutes)
DEMO_MODE=full ./scripts/test-end-to-end-workflow.sh

# Skip gateway smoke test
RUN_GATEWAY_SMOKE=false ./scripts/test-end-to-end-workflow.sh

# Skip build (faster, use if services already built)
SKIP_BUILD=true ./scripts/test-end-to-end-workflow.sh

# Don't cleanup containers after test
CLEANUP=false ./scripts/test-end-to-end-workflow.sh

# Custom patient count
PATIENT_COUNT=500 CARE_GAP_PERCENTAGE=40 ./scripts/test-end-to-end-workflow.sh
```

### Service Logs

All service logs are written to `/tmp/`:

```bash
# View FHIR service logs
tail -f /tmp/fhir-service.log

# View CQL Engine logs
tail -f /tmp/cql-engine.log

# View Quality Measure service logs
tail -f /tmp/quality-measure.log

# View Demo Seeding service logs
tail -f /tmp/demo-seeding.log

# Search for errors across all logs
grep -i error /tmp/*.log
```

---

## System Health Runner

### Overview

The master test orchestrator that runs all test suites in sequence and generates a comprehensive report.

### Usage

```bash
# Quick validation (all tests, quick demo mode)
./scripts/test-system-health.sh

# Full validation (all tests, full demo mode with 19K patients)
DEMO_MODE=full ./scripts/test-system-health.sh

# Skip end-to-end tests (faster)
SKIP_E2E=true ./scripts/test-system-health.sh

# Skip gateway tests
SKIP_GATEWAY=true ./scripts/test-system-health.sh

# Skip authentication tests
SKIP_AUTH=true ./scripts/test-system-health.sh

# Custom output directory
OUTPUT_DIR=/var/log/hdim-tests ./scripts/test-system-health.sh
```

### Test Suites

1. **Infrastructure Health**
   - Docker daemon status
   - PostgreSQL connectivity
   - Redis connectivity
   - Kafka status

2. **Gateway Smoke Test**
   - All 29 services
   - Circuit breakers
   - Rate limiting
   - Authentication

3. **Authentication Flow Validation**
   - Login flow
   - JWT generation
   - Token validation

4. **Demo Platform Validation**
   - Demo Seeding Service health
   - Scenario verification (if full mode)

5. **End-to-End Workflow**
   - Complete quality measure flow
   - Care gap detection

### Report Output

Reports are saved to `$OUTPUT_DIR` (default: `/tmp/hdim-health-tests`):

```
/tmp/hdim-health-tests/
├── gateway-smoke.json           # Gateway test results
├── auth-test.log                # Authentication test log
├── e2e-workflow.log             # E2E workflow log
└── system-health-report.txt     # Comprehensive report
```

**Example Report:**

```
================================================================================
HDIM System Health Report
================================================================================
Generated: 2026-01-10 14:23:45
Demo Mode: full
Output Directory: /tmp/hdim-health-tests

TEST SUITE SUMMARY
--------------------------------------------------------------------------------
Total Test Suites: 5
Passed: 5
Failed: 0
Success Rate: 100.0%

TEST RESULTS
--------------------------------------------------------------------------------
Gateway Smoke Test:
  totalTests: 58
  passed: 54
  failed: 4
  skipped: 0
  passRate: 93.1

================================================================================
```

---

## CI/CD Integration

### GitHub Actions

The test suite integrates with GitHub Actions for automated testing on every push/PR.

See `.github/workflows/smoke-tests.yml` for configuration.

**Workflow Triggers:**
- Push to `main` or `master`
- Pull request creation/update
- Manual workflow dispatch

**Test Stages:**
1. Gateway smoke test (core services)
2. Quick demo validation (100 patients)
3. Authentication flow test

**Artifacts:**
- Gateway smoke test report (JSON)
- E2E workflow log
- System health report

### Local CI Simulation

```bash
# Simulate CI environment
export CI=true
export SKIP_BUILD=false
export DEMO_MODE=quick
export OUTPUT_DIR=./test-reports

# Run tests
./scripts/test-system-health.sh

# Check exit code
echo $?  # 0 = success, non-zero = failure
```

---

## Troubleshooting

### Common Issues

#### 1. Gateway Smoke Test Failures

**Symptom:** Multiple service health checks fail

**Diagnosis:**
```bash
# Check if services are running
docker compose ps

# Check specific service health
curl http://localhost:8087/actuator/health

# View service logs
docker compose logs quality-measure-service
```

**Solution:**
```bash
# Restart failed services
docker compose restart quality-measure-service

# Or restart all
docker compose restart
```

#### 2. Patient Seeding Timeout

**Symptom:** Demo seeding takes longer than expected

**Diagnosis:**
```bash
# Check demo seeding service logs
tail -f /tmp/demo-seeding.log

# Check database connections
docker compose exec postgres psql -U healthdata -c "SELECT count(*) FROM pg_stat_activity;"
```

**Solution:**
```bash
# Use quick mode for testing
DEMO_MODE=quick ./scripts/test-end-to-end-workflow.sh

# Reduce patient count
PATIENT_COUNT=50 ./scripts/test-end-to-end-workflow.sh
```

#### 3. jq Command Not Found

**Symptom:** Tests fail with "jq: command not found"

**Solution:**
```bash
# Install jq
# macOS
brew install jq

# Ubuntu/Debian
sudo apt-get install jq

# RHEL/CentOS
sudo yum install jq
```

#### 4. Database Connection Errors

**Symptom:** Services fail to start with database connection errors

**Diagnosis:**
```bash
# Check PostgreSQL status
docker compose exec postgres pg_isready -U healthdata

# Check database logs
docker compose logs postgres
```

**Solution:**
```bash
# Recreate database
docker compose down -v
docker compose up -d postgres
sleep 10  # Wait for PostgreSQL to initialize
```

#### 5. Port Conflicts

**Symptom:** Services fail to bind to ports

**Diagnosis:**
```bash
# Check what's using port 8087
lsof -i :8087

# Or on Linux
netstat -tlnp | grep 8087
```

**Solution:**
```bash
# Kill process using the port
kill -9 <PID>

# Or change service port in docker-compose.yml
```

### Debug Mode

Enable verbose logging:

```bash
# Gateway smoke test
./scripts/test-gateway-smoke.sh --mode=full --verbose

# End-to-end workflow
set -x  # Enable shell debug mode
./scripts/test-end-to-end-workflow.sh
set +x  # Disable debug mode
```

---

## Best Practices

### 1. Test Isolation

Always clean up between test runs:

```bash
# Clean Docker containers and volumes
docker compose down -v

# Clean test reports
rm -rf /tmp/hdim-health-tests/*
rm -f /tmp/*.log
```

### 2. Parallel Testing

Avoid running multiple test suites concurrently - they share the same infrastructure:

```bash
# ❌ DON'T DO THIS
./scripts/test-gateway-smoke.sh &
./scripts/test-end-to-end-workflow.sh &

# ✅ DO THIS
./scripts/test-system-health.sh  # Runs all tests sequentially
```

### 3. Test Data Management

Use appropriate demo modes:

```bash
# Development/debugging: quick mode
DEMO_MODE=quick ./scripts/test-end-to-end-workflow.sh

# Pre-production validation: full mode
DEMO_MODE=full ./scripts/test-system-health.sh

# Custom scenarios: direct API calls
curl -X POST http://localhost:8108/api/v1/demo/seed \
  -H "X-Tenant-ID: test-tenant" \
  -d '{"count": 1000, "careGapPercentage": 30}'
```

### 4. CI/CD Optimization

For faster CI builds:

```bash
# Skip full demo in CI
if [ "$CI" = "true" ]; then
  export DEMO_MODE=quick
  export SKIP_BUILD=false
fi

# Cache Gradle dependencies
./gradlew build --build-cache
```

### 5. Monitoring Test Results

Track test trends over time:

```bash
# Save reports with timestamps
OUTPUT_DIR="./test-reports/$(date +%Y%m%d-%H%M%S)" \
  ./scripts/test-system-health.sh

# Parse JSON reports
jq -r '.summary | "Pass Rate: \(.passRate)%"' \
  test-reports/*/gateway-smoke.json
```

---

## Appendix

### Test Metrics

Track these metrics for quality assurance:

- **Gateway Pass Rate:** Target > 95%
- **E2E Success Rate:** Target > 98%
- **Patient Seeding Time (100):** Target < 10 seconds
- **Patient Seeding Time (19K):** Target < 60 seconds
- **Total Test Suite Time:** Target < 5 minutes (quick), < 15 minutes (full)

### Further Reading

- [STARTUP_RUNBOOK.md](./STARTUP_RUNBOOK.md) - Production deployment guide
- [AUTHENTICATION_GUIDE.md](../AUTHENTICATION_GUIDE.md) - Authentication flows
- [GATEWAY_TRUST_ARCHITECTURE.md](../backend/docs/GATEWAY_TRUST_ARCHITECTURE.md) - Gateway security
- [SYSTEM_ARCHITECTURE.md](./architecture/SYSTEM_ARCHITECTURE.md) - System overview

---

**Questions or Issues?** File an issue at: https://github.com/your-org/hdim-master/issues
