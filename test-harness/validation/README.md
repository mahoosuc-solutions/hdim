# HDIM Deployment Validation Test Harness

## Overview

This test harness provides comprehensive validation of HDIM deployments to ensure they are fully functional, secure, and compliant with multi-tenant isolation, data integrity, API functionality, and healthcare regulatory requirements.

## Quick Start

```bash
# 1. Navigate to validation directory
cd test-harness/validation

# 2. Copy environment template
cp .env.example .env

# 3. Configure your environment
# Edit .env with your database credentials and API URL

# 4. Install dependencies
npm install

# 5. Run smoke tests (quick validation)
./run-validation.sh --tier smoke

# 6. Run all validation tiers
./run-validation.sh --tier all
```

## Test Tiers

### Tier 1: Smoke Tests (5-10 minutes)
Quick deployment health check to verify basic functionality.

**What it tests:**
- Database connectivity and health
- Required database schemas exist
- API endpoint accessibility
- Docker services status
- Tenant configuration accessibility

**When to run:**
- After every deployment
- Before running comprehensive tests
- As a quick health check

**Command:**
```bash
npm run test:smoke
# or
./run-validation.sh --tier smoke
```

### Tier 2: Functional Tests (20-30 minutes)
Comprehensive validation of core functionality.

**What it tests:**
- Multi-tenant data isolation (RLS enforcement)
- API contract compliance (FHIR R4)
- Database integrity (schema, constraints, indexes)
- Authentication and authorization
- Error handling

**When to run:**
- Before merging to main branch
- After significant code changes
- Weekly as part of QA

**Command:**
```bash
npm run test:functional
# or
./run-validation.sh --tier functional
```

### Tier 3: Integration Tests (30-60 minutes)
End-to-end workflow validation.

**What it tests:**
- Complete quality measure evaluation workflows
- Service-to-service communication
- Data pipeline validation
- External integration points

**When to run:**
- Before production deployment
- After infrastructure changes
- Monthly as comprehensive validation

**Command:**
```bash
npm run test:integration
# or
./run-validation.sh --tier integration
```

### Tier 4: Performance & Load Tests (variable duration)
Baseline performance metrics and load handling.

**What it tests:**
- Concurrent multi-tenant load
- Database query performance
- API response time benchmarks
- CQL evaluation performance

**When to run:**
- Before production deployment
- After performance-related changes
- For capacity planning

**Command:**
```bash
npm run test:performance
# or
./run-validation.sh --tier performance
```

## Directory Structure

```
validation/
├── smoke-tests/              # Tier 1: Quick deployment health checks
│   └── deployment-health.test.ts
├── multi-tenant-isolation/   # Tier 2: Security and isolation tests
│   └── (to be implemented)
├── api-contracts/            # Tier 2: API contract validation
│   └── (to be implemented)
├── database-integrity/       # Tier 2: Database schema and integrity
│   └── (to be implemented)
├── service-integration/      # Tier 3: End-to-end workflows
│   └── (to be implemented)
├── performance-load/         # Tier 4: Performance and load testing
│   └── (to be implemented)
├── lib/                      # Shared utilities and helpers
│   ├── api-client.ts
│   ├── database-helper.ts
│   ├── deployment-validator.ts
│   ├── tenant-manager.ts
│   └── test-data-generator.ts
├── config/                   # Configuration files
│   └── test-config.json
├── reports/                  # Generated test reports
├── .env.example             # Environment variable template
├── jest.config.js           # Jest configuration
├── jest.setup.js            # Jest setup file
├── package.json             # Dependencies and scripts
├── run-validation.sh        # Main test execution script
├── tsconfig.json            # TypeScript configuration
└── README.md                # This file
```

## Configuration

### Environment Variables

Copy `.env.example` to `.env` and configure:

```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=hdim
DB_USER=hdim_user
DB_PASSWORD=your_password_here

# API Configuration
HDIM_API_URL=http://localhost:3000

# Test Configuration
TEST_TIMEOUT=300000
ENABLE_SMOKE_TESTS=true
ENABLE_FUNCTIONAL_TESTS=true
ENABLE_INTEGRATION_TESTS=true
ENABLE_PERFORMANCE_TESTS=false
```

### Test Configuration

Edit `config/test-config.json` to customize:
- Test timeouts
- Load test parameters
- Reporting options

## Usage Examples

### Run Specific Test Tier

```bash
# Smoke tests only
./run-validation.sh --tier smoke

# Functional tests only
./run-validation.sh --tier functional

# Integration tests only
./run-validation.sh --tier integration

# Performance tests only
./run-validation.sh --tier performance

# All tiers
./run-validation.sh --tier all
```

### Run with npm Scripts

```bash
# Smoke tests
npm run test:smoke

# Functional tests
npm run test:functional

# Integration tests
npm run test:integration

# Performance tests
npm run test:performance

# All tests
npm test

# Watch mode (for development)
npm run test:watch
```

### Run Specific Test File

```bash
npm test -- smoke-tests/deployment-health.test.ts
```

## Success Criteria

### Tier 1 - Smoke Tests (MUST PASS)
- ✅ Database connectivity established (<1000ms)
- ✅ Required database schemas exist
- ✅ API endpoint accessible
- ✅ Docker services can be checked
- ✅ Tenant configuration accessible

### Tier 2 - Functional Tests (RECOMMENDED)
- ✅ Multi-tenant data isolation verified (100% pass rate)
- ✅ All FHIR API endpoints comply with R4 spec
- ✅ Database constraints and indexes valid
- ✅ Authentication and authorization working

### Tier 3 - Integration Tests (RECOMMENDED)
- ✅ End-to-end workflows functional
- ✅ Service-to-service communication working
- ✅ Data pipelines processing correctly

### Tier 4 - Performance Tests (BASELINE)
- ✅ API response time <500ms average under load
- ✅ Database queries meet SLA
- ✅ System handles concurrent tenants
- ✅ CQL evaluation performs within limits

## Failure Handling

### Critical Failures (Block Deployment)
- Any Tier 1 smoke test fails
- Multi-tenant isolation breach
- API returns 500 errors consistently
- Database connection failures

### Non-Critical Failures (Investigate)
- Individual functional test failures
- Performance below SLA
- Integration test warnings

## Troubleshooting

### Database Connection Refused

```bash
# Check PostgreSQL is running
docker ps | grep postgres

# Check connectivity
nc -zv localhost 5432

# Verify credentials in .env
cat .env | grep DB_
```

### API Not Accessible

```bash
# Check API service status
curl -v http://localhost:3000/health

# Check Docker services
docker ps

# Check service logs
docker logs hdim-api
```

### Tests Timing Out

```bash
# Increase timeout in jest.setup.js
# or set environment variable
export TEST_TIMEOUT=60000
```

### Permission Denied on run-validation.sh

```bash
chmod +x run-validation.sh
```

## CI/CD Integration

### GitHub Actions

Create `.github/workflows/hdim-validation.yml`:

```yaml
name: HDIM Deployment Validation

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  validate:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'

      - name: Install dependencies
        working-directory: test-harness/validation
        run: npm install

      - name: Run Smoke Tests
        working-directory: test-harness/validation
        run: npm run test:smoke
        env:
          DB_HOST: localhost
          DB_PORT: 5432
          DB_NAME: hdim_test
          DB_USER: hdim_user
          DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
          HDIM_API_URL: http://localhost:3000

      - name: Run Functional Tests
        working-directory: test-harness/validation
        run: npm run test:functional
        if: success()
```

### GitLab CI

Add to `.gitlab-ci.yml`:

```yaml
validate-deployment:
  stage: test
  image: node:18
  script:
    - cd test-harness/validation
    - npm install
    - npm run test:smoke
    - npm run test:functional
  artifacts:
    reports:
      junit: test-harness/validation/reports/junit.xml
```

## Development

### Adding New Tests

1. Create test file in appropriate tier directory
2. Import required libraries from `lib/`
3. Follow existing test patterns
4. Update this README with new test coverage

Example:

```typescript
import { describe, test, expect, beforeAll, afterAll } from '@jest/globals';
import { DatabaseHelper } from '../lib/database-helper';

describe('My New Test Suite', () => {
  let dbHelper: DatabaseHelper;

  beforeAll(async () => {
    dbHelper = new DatabaseHelper({
      host: process.env.DB_HOST!,
      port: parseInt(process.env.DB_PORT!),
      database: process.env.DB_NAME!,
      user: process.env.DB_USER!,
      password: process.env.DB_PASSWORD!,
    });
    await dbHelper.connect();
  });

  afterAll(async () => {
    await dbHelper.disconnect();
  });

  test('should validate something', async () => {
    // Your test logic here
    expect(true).toBe(true);
  });
});
```

### Running Tests in Development

```bash
# Watch mode - re-runs tests on file changes
npm run test:watch

# Run specific test file
npm test -- --testPathPattern=deployment-health

# Run with verbose output
npm test -- --verbose
```

## Advanced Usage

### Custom Test Configuration

Create custom config file:

```json
{
  "deployment": {
    "apiUrl": "https://staging.hdim.example.com",
    "database": {
      "host": "staging-db.example.com",
      "port": 5432
    }
  }
}
```

Load with:

```bash
export TEST_CONFIG=./custom-config.json
npm test
```

### Performance Profiling

```bash
# Run performance tests with detailed output
npm run test:performance -- --verbose

# Check test execution time
npm test -- --detectOpenHandles --forceExit
```

## Support

For issues or questions:

1. Check this README
2. Review test output and error messages
3. Check Docker and database logs
4. Review the comprehensive prompt documentation: `HDIM_DEPLOYMENT_VALIDATION_PROMPT.md`

## License

ISC

## Contributors

HDIM Team
