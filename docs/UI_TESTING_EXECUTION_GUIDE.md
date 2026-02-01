# HDIM UI Testing Execution Guide

## Overview

This guide explains how to run automated UI tests to exercise the HDIM demo platform. The test suite uses Playwright to test both the Angular Clinical Portal and backend services.

## Quick Start

### Prerequisites

1. **Services Running**: Demo services should be started
   ```bash
   docker compose -f demo/docker-compose.demo.yml up -d
   ```

2. **Node.js**: Version 18+ required
   ```bash
   node --version  # Should be 18+
   ```

3. **Dependencies Installed**: In the `e2e` directory
   ```bash
   cd e2e
   npm install
   npx playwright install chromium
   ```

### Running Tests

#### Option 1: Using the Test Script (Recommended)

```bash
# Run smoke tests (quick validation)
./scripts/run-ui-tests.sh smoke

# Run API health check tests only
./scripts/run-ui-tests.sh api

# Run all workflow tests
./scripts/run-ui-tests.sh workflows

# Run all tests
./scripts/run-ui-tests.sh all
```

#### Option 2: Direct Playwright Commands

```bash
cd e2e

# Run smoke tests
npm run test:smoke

# Run specific test file
npx playwright test tests/smoke/smoke.spec.ts

# Run with visible browser
npx playwright test --headed

# Run specific test by name
npx playwright test --grep "SMOKE-001"
```

## Test Categories

### 1. Smoke Tests (@smoke)

**Purpose**: Quick validation of critical paths  
**Duration**: ~5 minutes  
**When to Run**: Every commit, before deployment

```bash
npm run test:smoke
# or
npx playwright test --grep @smoke
```

**Tests Include**:
- Authentication (login/logout)
- Dashboard loading
- Patient list display
- Care gap access
- Navigation
- API health checks

### 2. Workflow Tests

**Purpose**: Full feature coverage  
**Duration**: ~30-45 minutes  
**When to Run**: Nightly, pre-release

**Available Workflows**:
- `test:auth` - Authentication workflows
- `test:patient` - Patient management
- `test:care-gap` - Care gap workflows
- `test:evaluation` - Quality measure evaluation
- `test:reports` - Reporting functionality
- `test:admin` - Administration features
- `test:risk` - Risk stratification

### 3. API Health Tests

**Purpose**: Verify backend services are responding  
**Duration**: ~1 minute  
**When to Run**: Before other tests, service health checks

```bash
npx playwright test --grep "SMOKE-070|SMOKE-071"
```

## Service Requirements

### Required Services

For full test execution, these services should be running:

| Service | Port | Health Check |
|---------|------|--------------|
| Clinical Portal | 4200 | `http://localhost:4200` |
| Gateway Service | 8080 | `http://localhost:8080/actuator/health` |
| Patient Service | 8084 | `http://localhost:8084/patient/actuator/health` |
| Quality Measure | 8087 | `http://localhost:8087/quality-measure/actuator/health` |
| FHIR Service | 8085 | `http://localhost:8085/fhir/metadata` |

### Checking Service Health

```bash
# Check all services
docker compose -f demo/docker-compose.demo.yml ps

# Check specific service
curl http://localhost:4200
curl http://localhost:8080/actuator/health
```

## Test Configuration

### Environment Variables

Set these in your environment or `.env` file:

```bash
BASE_URL=http://localhost:4200
API_BASE_URL=http://localhost:8080
HEADLESS=true
WORKERS=4
```

### Playwright Configuration

Key settings in `e2e/playwright.config.ts`:

- **Timeout**: 30 seconds per test
- **Retries**: 2 retries on failure (CI mode)
- **Workers**: 4 parallel workers
- **Browsers**: Chromium (default), Firefox, WebKit
- **Viewport**: 1280x720

## Common Issues and Solutions

### Issue: Global Setup Fails (Services Not Ready)

**Symptom**: 
```
Error: Service at http://localhost:8080/quality-measure/actuator/health not available
```

**Solution**:
1. Wait for services to fully start (60-90 seconds)
2. Check service health: `docker compose ps`
3. Run API-only tests that don't require full setup:
   ```bash
   npx playwright test --grep "SMOKE-070|SMOKE-071"
   ```

### Issue: Clinical Portal Not Accessible

**Symptom**: Tests fail with "Navigation timeout" or "Connection refused"

**Solution**:
1. Check portal is running: `docker compose ps clinical-portal`
2. Check logs: `docker compose logs clinical-portal`
3. Verify port 4200 is not in use: `netstat -tulpn | grep 4200`
4. Restart portal: `docker compose restart clinical-portal`

### Issue: Browser Installation Fails

**Symptom**: 
```
Failed to install browsers
sudo: a password is required
```

**Solution**:
```bash
# Install without system dependencies (may work)
npx playwright install chromium

# Or install with dependencies (requires sudo)
sudo npx playwright install --with-deps
```

### Issue: Tests Timeout

**Symptom**: Tests fail with timeout errors

**Solution**:
1. Increase timeout in test:
   ```typescript
   test.setTimeout(60000); // 60 seconds
   ```
2. Check if services are slow to respond
3. Run tests with `--headed` to see what's happening

## Test Reports

### HTML Report

After running tests, view the HTML report:

```bash
cd e2e
npx playwright show-report
```

Report location: `e2e/playwright-report/index.html`

### JUnit XML

For CI/CD integration:

```bash
# Report location
e2e/test-results/junit.xml
```

## Best Practices

### 1. Run Smoke Tests First

Always run smoke tests before full test suite:

```bash
npm run test:smoke
```

### 2. Check Service Health

Verify services are healthy before running tests:

```bash
docker compose -f demo/docker-compose.demo.yml ps
```

### 3. Use Appropriate Test Type

- **Development**: Run smoke tests frequently
- **Pre-commit**: Run smoke tests
- **Pre-release**: Run full test suite
- **CI/CD**: Run all tests with retries

### 4. Monitor Test Results

Review test reports regularly:
- Check for flaky tests
- Monitor test duration
- Track failure patterns

## Test Execution Workflow

```
1. Start Services
   ↓
2. Wait for Health (60-90 seconds)
   ↓
3. Run Smoke Tests
   ↓
4. If Pass: Run Full Suite
   ↓
5. Review Reports
   ↓
6. Fix Issues
```

## Integration with Demo Platform

The UI tests are designed to work with the demo platform:

- **Demo Mode**: Tests use demo login credentials
- **Test Data**: Tests can seed their own data if needed
- **Service Discovery**: Tests discover services via environment variables
- **Health Checks**: Tests verify service health before execution

## Next Steps

1. **Review Test Results**: Check HTML report
2. **Fix Failures**: Address any test failures
3. **Add Tests**: Add tests for new features
4. **Optimize**: Improve test performance and reliability

## Additional Resources

- **Test Runbook**: `e2e/TEST-RUNBOOK.md`
- **Playwright Docs**: https://playwright.dev
- **Test Structure**: `e2e/tests/` directory
- **Page Objects**: `e2e/pages/` directory

---

**Last Updated**: January 2026  
**Version**: 1.0
