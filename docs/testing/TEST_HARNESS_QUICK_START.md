# Test Harness Quick Start Guide

## 🚀 Quick Start (30 seconds)

```bash
# Start development server
./scripts/deploy-test-harness-dev.sh

# Or manually
npm run nx -- serve clinical-portal
```

Then open: **http://localhost:4200/testing**

## 📋 Prerequisites

- Node.js 20.x
- npm 10.x
- Backend services running (for API testing)

## 🔐 Authentication

The testing dashboard requires:
- **Role**: DEVELOPER or ADMIN
- **Login**: Use your existing credentials

## ✨ Features

### 1. Demo Scenarios
Load test scenarios to populate data:
- HEDIS Evaluation
- Patient Journey
- Risk Stratification
- Multi-Tenant

### 2. API Testing
Test backend endpoints:
- Patient Service
- Care Gap Service
- Quality Measure Service
- FHIR Service

### 3. Data Management
- **Seed**: Load test data
- **Validate**: Check data integrity
- **Reset**: Clear test data

### 4. Service Health
Monitor all backend services:
- Real-time health checks
- Status indicators
- Response times

### 5. Test Results
- View execution history
- Export as JSON or CSV
- Persistent storage (localStorage)

## 🧪 Running Tests

### Via Dashboard
1. Navigate to http://localhost:4200/testing
2. Use the UI to run tests
3. View results in real-time

### Via Validation Scripts
```bash
cd test-harness/validation
npm install
./run-validation.sh --tier smoke
```

## 📊 Test IDs for Automation

All interactive elements have `data-testid` attributes:

- `test-scenario-{scenario-name}`
- `test-api-{service}-{endpoint}`
- `test-seed-data`
- `test-validate-data`
- `test-reset-data`
- `test-service-health-{service-name}`

## 🐛 Troubleshooting

### Server won't start
```bash
# Check if port is in use
lsof -i :4200

# Kill existing process
pkill -f "nx serve clinical-portal"
```

### Can't access dashboard
- Verify you're logged in
- Check you have DEVELOPER or ADMIN role
- Try: http://localhost:4200/testing?demo=true

### API tests failing
- Verify backend services are running
- Check API gateway URL in config
- Review service health status

## 📚 More Information

- **Full Deployment Guide**: `docs/testing/TEST_HARNESS_DEPLOYMENT.md`
- **Build Status**: `docs/testing/TEST_HARNESS_BUILD_STATUS.md`
- **Validation Report**: `docs/testing/TESTING_DASHBOARD_VALIDATION_REPORT.md`

---

**Ready to test!** 🎉
