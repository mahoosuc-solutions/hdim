# UI Testing Validation Improvements

## Overview

The UI testing validation functionality has been significantly improved to be more resilient and flexible, allowing tests to run even when some services are not fully available.

## Key Improvements

### 1. **Service Health Checker Utility**

**New File**: `e2e/utils/service-health-checker.ts`

**Features**:
- Multiple fallback URLs for each service
- Configurable timeouts and retries
- Parallel service checking
- Graceful degradation
- Detailed health reporting

**Usage**:
```typescript
const healthChecker = new ServiceHealthChecker({
  timeout: 5000,
  retries: 2,
  retryDelay: 1000,
});

const summary = await healthChecker.checkHdimServices({
  gateway: 'http://localhost:8080',
  patient: 'http://localhost:8084',
  qualityMeasure: 'http://localhost:8087',
  fhir: 'http://localhost:8085',
  portal: 'http://localhost:4200',
});
```

### 2. **Improved Global Setup**

**File**: `e2e/global.setup.ts`

**Improvements**:
- **Graceful Degradation**: Continues even if some services are unavailable
- **Multiple Fallback Strategies**: Tries multiple URLs for each service
- **Skip Option**: Can skip global setup with `SKIP_GLOBAL_SETUP=true`
- **Strict Mode**: Optional strict validation with `STRICT_VALIDATION=true`
- **Better Error Messages**: Clear reporting of which services are healthy/unhealthy
- **Flexible Authentication**: Multiple auth strategies (browser, API, mock)

**Environment Variables**:
- `SKIP_GLOBAL_SETUP=true` - Skip global setup entirely
- `STRICT_VALIDATION=true` - Fail if required services aren't healthy
- `BASE_URL` - Clinical portal URL (default: http://localhost:4200)
- `API_BASE_URL` - Gateway/API URL (default: http://localhost:8080)

### 3. **Resilient Test Implementation**

**File**: `e2e/tests/smoke/smoke.spec.ts`

**Improvements**:
- **Multiple URL Fallbacks**: Tests try multiple health check endpoints
- **Graceful Skipping**: Tests skip with informative messages if services unavailable
- **Timeout Configuration**: Shorter timeouts for faster feedback
- **Better Error Handling**: Catches and reports connection errors gracefully

### 4. **Enhanced Test Script**

**File**: `scripts/run-ui-tests.sh`

**Improvements**:
- Service health checking before tests
- Environment variable support
- Better error messages
- Multiple test execution modes

## Usage Examples

### Run Tests with Graceful Degradation

```bash
# Tests will continue even if some services are down
cd e2e
npm run test:smoke
```

### Skip Global Setup

```bash
# Skip setup entirely (useful for quick API tests)
SKIP_GLOBAL_SETUP=true npx playwright test tests/smoke/smoke.spec.ts --grep "SMOKE-070"
```

### Strict Validation Mode

```bash
# Fail if services aren't healthy (for CI/CD)
STRICT_VALIDATION=true npx playwright test
```

### Custom Service URLs

```bash
BASE_URL=http://localhost:4200 \
API_BASE_URL=http://localhost:8080 \
GATEWAY_URL=http://localhost:8080 \
npx playwright test
```

## Service Health Check Strategy

### Multiple Fallback URLs

Each service is checked against multiple possible endpoints:

**Gateway Service**:
1. `http://localhost:8080/actuator/health`
2. `http://localhost:8080/health`

**Patient Service**:
1. `http://localhost:8084/patient/actuator/health`
2. `http://localhost:8084/actuator/health`
3. `http://localhost:8084/health`

**Quality Measure Service**:
1. `http://localhost:8087/quality-measure/actuator/health`
2. `http://localhost:8087/actuator/health`
3. `http://localhost:8087/health`

**FHIR Service**:
1. `http://localhost:8085/fhir/metadata`
2. `http://localhost:8085/metadata`

**Clinical Portal**:
1. `http://localhost:4200`
2. `http://localhost:4200/index.html`

### Retry Strategy

- **Timeout**: 5 seconds per attempt
- **Retries**: 2 attempts per URL
- **Retry Delay**: 1 second between retries
- **Total Max Wait**: ~15 seconds per service

## Validation Modes

### 1. **Degraded Mode** (Default)

- Checks all services
- Reports health status
- Continues even if services are down
- Tests that require unavailable services will skip or fail gracefully

### 2. **Strict Mode**

- Enabled with `STRICT_VALIDATION=true`
- Fails setup if required services aren't healthy
- Use for CI/CD where all services must be available

### 3. **Skip Mode**

- Enabled with `SKIP_GLOBAL_SETUP=true`
- Skips all validation and setup
- Creates minimal mock auth state
- Use for quick API-only tests

## Health Check Output

Example output:

```
Service Health Summary:
  Healthy: 3/5
  ✓ Gateway (234ms)
  ✓ Patient Service (156ms)
  ✗ Quality Measure Service (Connection refused)
  ✓ FHIR Service (189ms)
  ✗ Clinical Portal (Connection refused)

⚠️  Some services are not healthy, continuing in degraded mode
Tests that require unavailable services may be skipped or fail
```

## Benefits

1. **Faster Feedback**: Tests start even if some services aren't ready
2. **Better Debugging**: Clear reporting of which services are available
3. **Flexible Execution**: Multiple modes for different scenarios
4. **Resilient Tests**: Tests handle service unavailability gracefully
5. **CI/CD Ready**: Strict mode ensures all services are available in CI

## Migration Guide

### Before

```bash
# Would fail if any service wasn't ready
npx playwright test
```

### After

```bash
# Continues with degraded mode
npx playwright test

# Or skip setup entirely
SKIP_GLOBAL_SETUP=true npx playwright test

# Or require all services
STRICT_VALIDATION=true npx playwright test
```

## Troubleshooting

### Services Not Detected

1. Check service URLs are correct
2. Verify services are actually running
3. Check firewall/network connectivity
4. Review service health check output

### Tests Skipping Unexpectedly

1. Check service health summary in setup output
2. Verify required services are marked as `required: true`
3. Review test skip conditions

### Strict Mode Failing

1. Ensure all required services are healthy
2. Check service logs for startup issues
3. Verify service endpoints are correct
4. Consider using degraded mode for development

---

**Last Updated**: January 2026  
**Version**: 2.0
