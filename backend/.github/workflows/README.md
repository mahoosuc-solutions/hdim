# GitHub Actions Workflows

## Active Workflows

### backend-ci.yml
**Backend CI/CD Pipeline**

Automated build, test, and security scanning for all backend services.

**Triggers:**
- Push to `master` or `develop`
- Pull requests
- Manual dispatch

**Stages:**
1. **Build & Test** (45min) - Compiles 87 services, runs unit tests
2. **Security Scan** (20min) - Trivy + OWASP dependency check
3. **Code Quality** (20min) - SonarQube analysis (optional)

**Status Badge:**
```markdown
![Backend CI](https://github.com/YOUR_ORG/hdim-master/workflows/Backend%20CI%2FCD%20Pipeline/badge.svg)
```

**Documentation:** [CI/CD Setup Guide](../../docs/CI_CD_SETUP.md)

---

## Workflow Best Practices

### Security
✅ **DO:** Use environment variables for sensitive data
```yaml
env:
  DB_PASSWORD: ${{ secrets.DATABASE_PASSWORD }}
run: echo "$DB_PASSWORD"  # Safe - uses env var
```

❌ **DON'T:** Use GitHub context directly in shell commands
```yaml
run: echo "${{ secrets.DATABASE_PASSWORD }}"  # UNSAFE - command injection risk
```

### Performance
- Use `cache: 'gradle'` in setup-java action
- Set `fetch-depth: 0` only when needed (SonarQube)
- Use `continue-on-error: true` for non-critical steps

### Debugging
```bash
# View workflow run logs
gh run list --workflow=backend-ci.yml
gh run view <run-id> --log

# Re-run failed jobs
gh run rerun <run-id>
```

---

## Local Testing

Before pushing, test locally:

```bash
# Install act (GitHub Actions local runner)
brew install act  # macOS
sudo snap install act  # Ubuntu

# Run workflow locally
act -j build-and-test

# Run specific job with secrets
act -j build-and-test -s SONAR_TOKEN=your-token
```

**Note:** `act` uses Docker, so ensure Docker is running.

---

## Adding New Workflows

1. Create workflow file: `.github/workflows/my-workflow.yml`
2. Use minimal permissions:
```yaml
permissions:
  contents: read
  issues: write
```
3. Test locally with `act`
4. Document in this README

---

## Troubleshooting

**Q: Workflow not triggering on push**
A: Check `paths` filter - workflow only runs if specified files changed

**Q: Job failing with "Resource not accessible by integration"**
A: Add required permissions to workflow file

**Q: Secrets not available in PR from fork**
A: Secrets are not exposed to fork PRs (security measure)

---

For detailed documentation, see [CI/CD Setup Guide](../../docs/CI_CD_SETUP.md).
