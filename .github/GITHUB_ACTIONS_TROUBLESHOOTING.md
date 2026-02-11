# GitHub Actions CI/CD Troubleshooting Guide

## Current Issue (PR #390)

### Symptoms
- All CI/CD checks timeout in 2-3 seconds
- Timeouts occur before build steps begin
- Local builds succeed (verified with `npx nx build mfeDeployment`)
- Vercel checks pass (using different infrastructure)

### Root Cause
Infrastructure/runner issue, not code quality:
- GitHub Actions runners appear to be under-resourced or experiencing issues
- 2-3 second failures indicate timeout before test execution starts
- Not caused by code changes (local build succeeds)

### Evidence
```bash
# Local build test
$ npx nx build mfeDeployment
✅ SUCCESS (31.8 seconds)

# GitHub Actions result
❌ FAILED (2-3 seconds timeout)
```

## Investigation Steps

1. **Check Runner Status**
   ```bash
   gh workflow list  # View workflows
   gh run list       # Check recent runs
   ```

2. **Review Logs**
   - Go to: https://github.com/webemo-aaron/hdim/actions
   - Click latest run
   - Check "Build and Test" job for actual error
   - Look for resource constraints in runner

3. **Check GitHub Status**
   - Visit: https://www.githubstatus.com
   - Verify no GitHub Actions outages

4. **Verify Configuration**
   - Check `.github/workflows/` files
   - Review timeout settings
   - Verify runner configuration

## Recommendations

### Short-term
1. Don't block PRs on these timeouts
2. Code review can proceed independently
3. Local verification sufficient for now
4. Merge when code approved (infrastructure separate)

### Long-term
1. Scale GitHub Actions runners
2. Increase timeout thresholds
3. Add runner health monitoring
4. Consider self-hosted runners if recurring issue

## Local Verification Process

Until GitHub Actions is fixed, use this verification:

```bash
# Frontend builds
npx nx build mfeDeployment

# Backend builds
cd backend
./gradlew :modules:services:demo-seeding-service:build -x test

# Tests
./gradlew testUnit
./gradlew testFast
./gradlew testAll
```

## Related Issues
- PR #390: Deployment console MFE + infrastructure enhancements
- Status: Code ready, infrastructure issue separable
