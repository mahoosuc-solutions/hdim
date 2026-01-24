# Frontend CI/CD Pipeline Setup Guide

**Status:** ✅ Implemented (Phase 1 - Core Pipeline)
**Last Updated:** January 24, 2026

## Overview

The HDIM frontend uses GitHub Actions for continuous integration, providing automated build, test, and security scanning for all Angular applications and micro-frontends.

**Pipeline Location:** `.github/workflows/frontend-ci.yml`

---

## Frontend Applications

### Main Applications
- **clinical-portal** - Main clinical user interface (Angular 17+)
- **admin-portal** - Administrative portal

### Micro-Frontends (MFEs)
- **mfe-care-gaps** - Care gap management module
- **mfe-patients** - Patient management module  
- **mfe-quality** - Quality measures module
- **mfe-reports** - Reporting and analytics module

---

## Pipeline Stages

### Stage 1: Build & Test (30 minutes)

**Triggers:**
- Push to `master` or `develop` branches
- Pull requests to `master` or `develop`
- Manual workflow dispatch
- Filtered by frontend paths (apps/, libs/, package.json)

**Steps:**
1. **Setup**
   - Checkout code
   - Set up Node.js 20
   - Cache npm dependencies

2. **Lint**
   ```bash
   npx nx run-many --target=lint --all --parallel=3
   ```
   - ESLint checks for all apps
   - Runs in parallel (3 concurrent processes)
   - Continues on error (non-blocking)

3. **Build**
   ```bash
   npx nx build clinical-portal --configuration=production
   ```
   - Production builds with optimization
   - Angular AOT compilation
   - Output to `dist/` directory

4. **Test**
   ```bash
   npx nx run-many --target=test --all --parallel=3 --coverage
   ```
   - Jest/Vitest unit tests
   - Code coverage collection
   - Runs all test suites in parallel

5. **Artifact Upload**
   - Build artifacts (7 days retention)
   - Test coverage reports (30 days retention)

---

### Stage 2: Security Scan (15 minutes)

**Runs:** After successful build & test

**Tools:**
1. **npm audit**
   - Checks for known vulnerabilities in dependencies
   - Threshold: moderate and above
   - Non-blocking (continues on warnings)

2. **Trivy** - Filesystem vulnerability scanner
   - Scans apps/ directory
   - Detects CRITICAL and HIGH severity issues
   - Outputs SARIF format for GitHub Security

**Artifacts:**
- Security findings in GitHub Security tab
- Trivy SARIF report

---

## Nx Workspace Configuration

This project uses [Nx](https://nx.dev/) for monorepo management.

### Key Features
- **Parallel Execution:** Runs up to 3 tasks concurrently
- **Affected Detection:** Only builds/tests changed apps (not yet enabled)
- **Caching:** Nx computation caching for faster builds
- **Code Generation:** Nx schematics for scaffolding

### Common Commands

```bash
# Build specific app
npx nx build clinical-portal

# Test specific app
npx nx test clinical-portal

# Lint specific app
npx nx lint clinical-portal

# Build all apps
npx nx run-many --target=build --all

# Test all apps
npx nx run-many --target=test --all

# Affected apps only (after git changes)
npx nx affected --target=build
npx nx affected --target=test
```

---

## Local Development Testing

Test builds locally before pushing:

```bash
# Install dependencies
npm ci

# Lint all apps
npm run lint

# Build clinical portal
npx nx build clinical-portal --configuration=production

# Run tests with coverage
npx nx test clinical-portal --coverage

# Serve clinical portal locally
npx nx serve clinical-portal
```

---

## E2E Testing (Phase 2 - Future)

E2E tests are defined but not yet integrated into CI pipeline.

**Framework:** Playwright

**Test Suites:**
- `clinical-portal-e2e` - Clinical portal E2E tests
- `admin-portal-e2e` - Admin portal E2E tests
- MFE test suites for each micro-frontend

**Future Implementation:**
```yaml
- name: Run E2E tests
  run: npx nx e2e clinical-portal-e2e
```

---

## Troubleshooting

### Build Fails: "Cannot find module '@angular/...'"

**Cause:** Dependency installation issue
**Fix:**
```bash
rm -rf node_modules package-lock.json
npm install
```

---

### Tests Fail: "Heap out of memory"

**Cause:** Jest/Node.js memory limit
**Fix:** Increase Node memory:
```bash
NODE_OPTIONS=--max-old-space-size=4096 npx nx test
```

---

### Lint Errors Block Build

**Cause:** ESLint violations
**Fix:**
```bash
# Auto-fix where possible
npx nx lint clinical-portal --fix

# View specific errors
npx nx lint clinical-portal
```

---

## Performance Optimization

### Current Timings
- Build & Test: ~30 minutes
- Security Scan: ~15 minutes
- **Total:** ~45 minutes

### Future Optimizations

**1. Nx Affected (Not Implemented)**
Only build/test changed apps:
```yaml
- run: npx nx affected --target=build --parallel=3
- run: npx nx affected --target=test --parallel=3
```

**2. Nx Cloud (Optional)**
Distributed task execution and remote caching:
```yaml
env:
  NX_CLOUD_ACCESS_TOKEN: ${{ secrets.NX_CLOUD_ACCESS_TOKEN }}
```

**3. Build Matrix (Future)**
Parallel builds for each app:
```yaml
strategy:
  matrix:
    app: [clinical-portal, admin-portal, mfe-care-gaps]
```

---

## Future Enhancements

### Phase 2: E2E Testing
- [ ] Integrate Playwright E2E tests into pipeline
- [ ] Run E2E tests against deployed staging
- [ ] Visual regression testing
- [ ] Accessibility testing (axe-core)

### Phase 3: Docker Images
- [ ] Build Docker images for each app
- [ ] Push to GitHub Container Registry
- [ ] Multi-stage builds for optimization
- [ ] Nginx configuration testing

### Phase 4: Deployment
- [ ] Auto-deploy to staging (S3/CloudFront or Kubernetes)
- [ ] Manual approval for production
- [ ] Blue-green deployment strategy
- [ ] Smoke tests after deployment

---

## Related Documentation

- [Backend CI/CD Setup](./CI_CD_SETUP.md)
- [Nx Workspace Guide](https://nx.dev/getting-started/intro)
- [Angular Testing Best Practices](https://angular.io/guide/testing)

---

## Issue Reference

- **Issue:** #269 - CI/CD Pipeline - Frontend
- **Status:** Phase 1 Complete ✅
- **Implementation Date:** January 24, 2026
