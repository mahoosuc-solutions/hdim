# E2E Testing Quick Start Guide

Get started with E2E testing in 5 minutes!

## 🚀 Quick Setup

### 1. One-Command Setup

```bash
# From project root
cd e2e
npm install && npm run test:install
```

### 2. Start Backend

```bash
# Terminal 1 - Start backend
cd ..
./start.sh

# Wait for backend to be ready
curl http://localhost:8080/actuator/health
```

### 3. Run Tests

```bash
# Terminal 2 - Run all tests
cd e2e
npm test

# Or run specific suite
npm test -- patient-workflow.spec.ts
```

## 📋 Common Commands

### Run Tests

```bash
# All tests
npm test

# Specific test file
npm test -- patient-workflow.spec.ts

# Tests with tag
npm test -- --grep @workflow

# Specific browser
npm run test:chromium
npm run test:firefox
npm run test:webkit

# Mobile devices
npm run test:mobile

# With UI (interactive mode)
npm run test:ui

# Debug mode
npm run test:debug
```

### View Reports

```bash
# HTML report
npm run test:report

# Or manually
npx playwright show-report
```

## 🎯 Test by Category

```bash
# Patient workflows
npm test -- patient-workflow.spec.ts

# Quality measures
npm test -- quality-measure-workflow.spec.ts

# Care gaps
npm test -- care-gap-workflow.spec.ts

# Security
npm test -- security.spec.ts

# Performance
npm test -- performance.spec.ts

# Accessibility
npm test -- accessibility.spec.ts

# API integration
npm test -- api-integration.spec.ts
```

## 🔧 Configuration

### Environment File

Copy `.env.example` to `.env` and customize:

```bash
cp .env.example .env
```

Edit `.env`:

```env
BASE_URL=http://localhost:8080
HEADLESS=false
WORKERS=4
TIMEOUT=30000
```

### Quick Config Changes

```bash
# Run in headed mode (see browser)
HEADLESS=false npm test

# Increase timeout
TIMEOUT=60000 npm test

# Limit workers
WORKERS=1 npm test

# Skip retries
RETRIES=0 npm test
```

## 🐛 Troubleshooting

### Backend Not Running

```bash
# Check backend health
curl http://localhost:8080/actuator/health

# Start backend
./start.sh

# Or with Docker
docker compose up -d
```

### Authentication Failures

```bash
# Delete auth cache
rm -rf .auth/

# Re-run tests (will re-authenticate)
npm test
```

### Browser Installation

```bash
# Reinstall browsers
npx playwright install --with-deps

# Or specific browser
npx playwright install chromium
```

### Tests Timing Out

```bash
# Increase timeout
TIMEOUT=60000 npm test

# Or run in headed mode to see what's happening
npm run test:headed
```

### Port Already in Use

```bash
# Check what's using port 8080
lsof -i :8080

# Kill process
kill -9 <PID>
```

## 📊 Understand Results

### Test Output

```
Running 60 tests using 4 workers

✓ patient-workflow.spec.ts:10:5 › Patient Management Workflow › should create patient (1.2s)
✓ patient-workflow.spec.ts:25:5 › Patient Management Workflow › should search patients (890ms)
✗ patient-workflow.spec.ts:40:5 › Patient Management Workflow › should update patient (2.1s)

60 passed (2m 15s)
1 failed
```

### View Detailed Report

```bash
npm run test:report
```

Opens HTML report in browser with:
- Test results
- Screenshots of failures
- Traces
- Error messages

### Check Specific Test

```bash
# Run single test
npm test -- patient-workflow.spec.ts -g "should create patient"
```

## 💡 Pro Tips

### 1. Run Failed Tests Only

```bash
npm test -- --last-failed
```

### 2. Interactive Mode

```bash
npm run test:ui
```

- Pick tests to run
- See results in real-time
- Debug failures instantly

### 3. Generate Code

```bash
npm run test:codegen
```

Opens browser to record interactions and generate test code.

### 4. Parallel Execution

```bash
# Run with more workers
WORKERS=8 npm test

# Or sequential
WORKERS=1 npm test
```

### 5. Filter Tests

```bash
# Run only workflow tests
npm test -- --grep @workflow

# Skip slow tests
npm test -- --grep-invert @slow

# Performance tests only
npm run test:performance
```

## 📝 Writing Your First Test

Create `tests/my-test.spec.ts`:

```typescript
import { test, expect } from '../fixtures/test-fixtures';

test.describe('My Feature', () => {
  test('should do something', async ({ adminApiClient }) => {
    const response = await adminApiClient.get('/api/endpoint');

    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data).toBeTruthy();
  });
});
```

Run it:

```bash
npm test -- my-test.spec.ts
```

## 🎓 Next Steps

1. **Read the full README**: `cat README.md`
2. **Explore test files**: `ls tests/`
3. **Check fixtures**: `cat fixtures/test-fixtures.ts`
4. **Customize config**: `cat playwright.config.ts`

## 📚 Resources

- [Playwright Docs](https://playwright.dev/docs/intro)
- [Best Practices](https://playwright.dev/docs/best-practices)
- [API Reference](https://playwright.dev/docs/api/class-test)

## ❓ Need Help?

- Check `README.md` for detailed documentation
- Run `npm test -- --help` for CLI options
- View `playwright.config.ts` for configuration
- Contact the development team

---

**Ready to test! 🚀**

Start with:

```bash
npm test
```
