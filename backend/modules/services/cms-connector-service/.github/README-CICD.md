# CI/CD Pipeline Documentation

## Overview

The CMS Connector Service uses a comprehensive CI/CD pipeline built with GitHub Actions that automates building, testing, scanning, and deploying the application across multiple environments.

## Pipeline Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Git Event Trigger                        │
│        (Push to master/develop or Pull Request)                 │
└────────────┬────────────────────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Build & Test Job                              │
│  - Checkout code                                                 │
│  - Set up Java 17 (with Maven cache)                             │
│  - Maven build (skip tests in fast path)                         │
│  - Run unit tests                                                │
│  - Generate coverage report (Codecov)                            │
└────────────┬────────────────────────────────────────────────────┘
             │ (success)
             ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Docker Build Job                                │
│  - Set up Docker Buildx (multi-platform support)                 │
│  - Login to Docker Hub (if push event)                           │
│  - Build Docker image (with layer caching)                       │
│  - Push to Docker Hub (only on push events)                      │
│  - Publish to Docker Hub as:                                     │
│    - username/cms-connector-service:latest                       │
│    - username/cms-connector-service:{commit-sha}                 │
└────────────┬────────────────────────────────────────────────────┘
             │ (success on push, skip on PR)
             ▼
┌─────────────────────────────────────────────────────────────────┐
│                 Security Scan Job                                │
│  - Run Trivy filesystem scanner                                  │
│  - Check for vulnerabilities (CRITICAL, HIGH severity)           │
│  - Generate SARIF report                                         │
│  - Upload to GitHub Security tab                                 │
└────────────┬────────────────────────────────────────────────────┘
             │ (success or warnings allowed)
             ├─────────────────┬──────────────────┐
             │ (develop)       │ (master)         │
             ▼                 ▼                  
    Deploy Dev Job      Deploy Staging Job      
    - SSH to dev        - SSH to staging         
    - Pull image        - Pull image             
    - Restart services  - Restart services       
    - Health check      - Health check           
```

## Pipeline Workflows

### 1. Pull Request Workflow

When a PR is opened against `master` or `develop`:

1. **Build & Test** - Compiles code and runs tests
2. **Docker Build** - Builds image (doesn't push)
3. **Security Scan** - Scans for vulnerabilities

**Outcome**: ✅ or ❌ status shown on PR, blocks merge if any job fails

### 2. Development Push Workflow

When code is pushed to `develop` branch:

1. **Build & Test** - Full Maven build and test suite
2. **Docker Build** - Builds and pushes to Docker Hub
3. **Security Scan** - Trivy vulnerability assessment
4. **Deploy Dev** - Automatically deploys to development environment

**Outcome**: Application available at development server within 5-10 minutes

### 3. Staging/Release Push Workflow

When code is pushed to `master` branch:

1. **Build & Test** - Full Maven build and test suite
2. **Docker Build** - Builds and pushes to Docker Hub (tagged as latest)
3. **Security Scan** - Trivy vulnerability assessment
4. **Deploy Staging** - Automatically deploys to staging environment

**Outcome**: Release candidate deployed to staging for final validation

## Trigger Conditions

The pipeline is triggered when:

- **Push event** on `master` or `develop` branch
- **Pull request** against `master` or `develop` branch

Path filters (only if these paths changed):
- `src/**` - Source code
- `pom.xml` - Maven dependencies
- `Dockerfile` - Container definition
- `docker-compose*.yml` - Service orchestration
- `.github/workflows/docker-ci-cd.yml` - Pipeline definition itself

This means pushing to feature branches doesn't trigger CI/CD until a PR is opened.

## Jobs Detailed

### Build & Test Job

**Runs on**: Ubuntu Latest
**Duration**: ~5-10 minutes

Steps:
1. **Checkout** - Gets the code
2. **Setup Java 17** - Installs JDK with Maven cache
3. **Maven Build** - `mvn clean package -DskipTests`
4. **Unit Tests** - `mvn test` (includes integration tests)
5. **Coverage Report** - Generates `target/site/jacoco/jacoco.xml`
6. **Upload to Codecov** - Tracks code coverage trends over time

**Passes if**: Tests pass and no compilation errors
**Fails if**: Tests fail or code doesn't compile

### Docker Build Job

**Runs on**: Ubuntu Latest
**Duration**: ~3-5 minutes (varies with cache hits)
**Depends on**: Build & Test job success

Steps:
1. **Setup Buildx** - Enables multi-platform builds (amd64, arm64)
2. **Docker Hub Login** - Uses `DOCKER_USERNAME` and `DOCKER_PASSWORD` secrets
3. **Build & Push** - Multi-stage build with layer caching
4. **Image Tags**:
   - `latest` tag (always points to latest master/develop)
   - Commit SHA tag (immutable reference)

**On Pull Requests**: Only builds, doesn't push (no Docker Hub login)

**On Push Events**: Builds and pushes both images

**Image Size**: ~490MB for production (multi-stage optimized)

### Security Scan Job

**Runs on**: Ubuntu Latest
**Duration**: ~2-3 minutes
**Depends on**: Docker Build job

Steps:
1. **Trivy Scan** - Filesystem vulnerability scanner
2. **Format** - Generates SARIF output for GitHub integration
3. **Upload** - Posts results to GitHub Security tab

**Severity Filter**: Reports CRITICAL and HIGH vulnerabilities

**Outcome**: 
- ✅ If no critical/high vulns
- ⚠️ If medium/low vulns (job succeeds, warning)
- ❌ If critical/high vulns (job fails)

### Deploy Dev Job

**Runs on**: Ubuntu Latest
**Duration**: ~2-3 minutes
**Depends on**: Security Scan job success
**Only on**: Push to `develop` branch
**Environment**: Development

Steps:
1. **Setup SSH** - Configures private key from secret
2. **Scan host keys** - Adds deployment server to known_hosts
3. **Execute deployment**:
   - `cd /opt/cms-connector`
   - `docker-compose -f docker-compose.dev.yml pull`
   - `docker-compose -f docker-compose.dev.yml up -d`
   - Health check via curl to `http://localhost:8080/actuator/health`

**Environment Variables Used**:
- `DEV_DEPLOY_HOST` - Target server hostname
- `DEV_DEPLOY_USER` - SSH username
- `DEV_DEPLOY_KEY` - Private SSH key (stored securely)

**Outcome**: 
- ✅ Application running and healthy on dev server
- ❌ SSH error, health check failed, or deployment error

### Deploy Staging Job

**Runs on**: Ubuntu Latest
**Duration**: ~2-3 minutes
**Depends on**: Security Scan job success
**Only on**: Push to `master` branch
**Environment**: Staging

Steps:
1. **Setup SSH** - Configures private key from secret
2. **Scan host keys** - Adds deployment server to known_hosts
3. **Execute deployment**:
   - `cd /opt/cms-connector`
   - `docker-compose -f docker-compose.prod.yml pull`
   - `docker-compose -f docker-compose.prod.yml up -d`
   - Health check via curl

**Environment Variables Used**:
- `STAGING_DEPLOY_HOST` - Target server hostname
- `STAGING_DEPLOY_USER` - SSH username
- `STAGING_DEPLOY_KEY` - Private SSH key (stored securely)

**Outcome**: 
- ✅ Application running and healthy on staging server
- ❌ SSH error, health check failed, or deployment error

### Notify Job

**Runs on**: Ubuntu Latest
**Depends on**: All other jobs (always runs)
**Duration**: <1 minute

Provides summary:
```
Pipeline Summary:
==================
Build & Test: success
Docker Build: success
Security Scan: success
Dev Deployment: skipped
Staging Deployment: success
```

## Secret Configuration

See [.github/SECRETS_SETUP.md](./.SECRETS_SETUP.md) for detailed secret configuration instructions.

### Required Secrets Summary

**Repository Secrets** (all environments):
- `DOCKER_USERNAME` - Docker Hub username
- `DOCKER_PASSWORD` - Docker Hub access token

**Development Environment Secrets**:
- `DEV_DEPLOY_HOST` - Dev server hostname/IP
- `DEV_DEPLOY_USER` - Dev SSH username
- `DEV_DEPLOY_KEY` - Dev SSH private key

**Staging Environment Secrets**:
- `STAGING_DEPLOY_HOST` - Staging server hostname/IP
- `STAGING_DEPLOY_USER` - Staging SSH username
- `STAGING_DEPLOY_KEY` - Staging SSH private key

## Local Testing & Development

### Testing Locally Without Pushing

You can test your changes locally before committing:

```bash
# Build locally
mvn clean package

# Build Docker image locally
docker build -t cms-connector-service:test .

# Test with docker-compose dev setup
./docker-run.sh dev up
./docker-run.sh dev logs

# Clean up
./docker-run.sh dev down
```

### Using GitHub CLI Locally

Install GitHub Actions runner locally to test workflows:

```bash
# Install act (local GitHub Actions runner)
# https://github.com/nektos/act

act -j build-and-test -l  # Run build job locally
act push -j docker-build  # Simulate push event
```

## Monitoring Pipeline Execution

### Viewing Workflow Runs

1. Go to repository on GitHub
2. Click **Actions** tab
3. Select workflow run to view:
   - Overall status (✅/❌)
   - Individual job results
   - Detailed logs for each step
   - Annotations for errors/warnings

### Common Dashboard Views

**Recent Runs**: Lists latest workflow executions
**Workflow Status**: Overall health of CI/CD
**Security Tab**: Vulnerability scan results from Trivy

### Debugging Failed Jobs

1. Click on failed job
2. Expand relevant step to see full output
3. Common issues:
   - **Build fails**: Check Maven error messages, dependency versions
   - **Tests fail**: Review test output, database connectivity
   - **Docker build fails**: Check Dockerfile syntax, file paths
   - **Deploy fails**: Verify SSH keys, server connectivity, disk space
   - **Health check fails**: Application didn't start, check logs

## Performance Optimization

### Caching Strategy

1. **Maven Dependency Cache**
   - Automatically cached between runs
   - Invalidated if `pom.xml` changes
   - Speeds up builds by ~50%

2. **Docker Layer Caching**
   - Each Dockerfile instruction cached
   - Reused across builds if unchanged
   - Build cache stored in GitHub Actions
   - Multi-stage build optimizes final image size

3. **GitHub Actions Cache**
   - Maven cache: `~/.m2/repository`
   - Docker buildx cache: `type=gha` backend

### Build Time Breakdown

Typical build times:
- **Build & Test**: 5-10 minutes
  - Maven download (first run): +3-5 minutes
  - Unit tests: ~3-5 minutes
- **Docker Build**: 3-5 minutes
  - Base image pull: ~1 minute
  - Maven build inside Docker: ~2-3 minutes (if no cache)
  - Final layer creation: ~30s
- **Security Scan**: 2-3 minutes
- **Deployment**: 2-3 minutes

**Total pipeline time**: ~15-25 minutes (varies with caching)

## Troubleshooting

### SSH Deployment Failures

```
ERROR: Permission denied (publickey)
```

Solution:
1. Verify public key is in `~/.ssh/authorized_keys` on deployment server
2. Check file permissions: `authorized_keys` must be 600
3. Verify SSH daemon is running: `systemctl status sshd`
4. Test manually: `ssh -i ~/.ssh/github_dev deploy@hostname`

### Docker Push Failures

```
ERROR: unauthorized: authentication required
```

Solution:
1. Verify Docker Hub credentials are correct
2. Use access token, not password
3. Check token hasn't expired
4. Regenerate token if needed

### Health Check Failures

```
ERROR: Failed to check application health
```

Solution:
1. Check application started: `docker-compose ps`
2. Check logs: `docker-compose logs cms-connector`
3. Verify database connectivity
4. Check Spring Boot health endpoint: `curl http://localhost:8080/actuator/health`

### Trivy Scan Vulnerabilities

If security scan fails due to vulnerabilities:

1. View detailed report in **Security** tab
2. Check base image (eclipse-temurin) for newer versions
3. Update dependencies in `pom.xml`
4. Rebuild image

## Best Practices

1. **Commit Frequently**: Small, focused commits help identify failing changes
2. **Write Tests**: Coverage reports track quality over time
3. **Review PR Checks**: Don't merge if CI/CD pipeline shows ❌
4. **Monitor Deployments**: Check application health after deployment
5. **Rotate Secrets**: Update SSH keys and access tokens periodically
6. **Keep Dependencies Updated**: Run `mvn versions:display-dependency-updates`

## Advanced Configuration

### Adding Approval Gates

Require manual approval before deploying to production:

1. Go to **Settings > Environments > staging**
2. Enable "Required reviewers"
3. Add team members who must approve

### Custom Notifications

Add notification step to workflows:

```yaml
- name: Notify Slack
  if: always()
  uses: slackapi/slack-github-action@v1
  with:
    webhook-url: ${{ secrets.SLACK_WEBHOOK }}
    payload: |
      {
        "text": "Pipeline ${{ job.status }}"
      }
```

### Matrix Builds

Test against multiple Java versions:

```yaml
strategy:
  matrix:
    java-version: [17, 21]
```

## Support & Documentation

- **GitHub Actions Docs**: https://docs.github.com/en/actions
- **Trivy Scanner**: https://aquasecurity.github.io/trivy/
- **Docker Buildx**: https://docs.docker.com/build/buildx/
- **Maven Guide**: https://maven.apache.org/guides/

## File Structure

```
.github/
├── workflows/
│   └── docker-ci-cd.yml          # Main CI/CD workflow
├── SECRETS_SETUP.md              # Secret configuration guide
├── setup-secrets.sh              # Script to generate SSH keys
└── README-CICD.md                # This file
```
