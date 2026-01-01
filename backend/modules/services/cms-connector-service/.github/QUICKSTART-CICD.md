# CI/CD Pipeline Quick Start Guide

## 5-Minute Setup

Get your CI/CD pipeline running in just 5 minutes.

### Step 1: Create Docker Hub Access Token (2 minutes)

1. Go to https://hub.docker.com/settings/security
2. Click "New Access Token"
3. Name it: `github-actions`
4. Set permissions: Read, Write, Delete
5. Generate and **save the token** (copy it somewhere)

### Step 2: Add GitHub Secrets (1 minute)

Go to your repository on GitHub:

1. **Settings > Secrets and variables > Actions**
2. Create `DOCKER_USERNAME`:
   - Value: Your Docker Hub username
3. Create `DOCKER_PASSWORD`:
   - Value: The access token from Step 1

### Step 3: Push a Commit (2 minutes)

```bash
# Make any small change (e.g., README update)
echo "# Updated" >> README.md

# Commit and push
git add .
git commit -m "Trigger CI/CD pipeline"
git push origin develop
```

### Step 4: Watch Workflow Execute

Go to **Actions** tab on GitHub and watch your pipeline run:
- ✅ Build & Test (5-10 min)
- ✅ Docker Build (3-5 min)  
- ✅ Security Scan (2-3 min)
- ✅ Deploy Dev (optional, requires secrets)

**Done!** Your CI/CD pipeline is working.

---

## Testing Locally Without Pushing

### Option 1: Simple Local Build

Test without committing:

```bash
# Build locally
mvn clean package

# Build Docker image
docker build -t cms-connector-service:test .

# Test with docker-compose
./docker-run.sh dev up
./docker-run.sh dev logs
./docker-run.sh dev down
```

**Time**: ~15 minutes

### Option 2: Using Act (Local GitHub Actions Runner)

Run workflows locally before pushing:

```bash
# Install act
# https://github.com/nektos/act

# Run build job locally
act -j build-and-test

# Run docker build job
act -j docker-build

# Simulate push event (all jobs)
act push -e workflow-trigger.json
```

**Requires**: 
- Docker installed
- act installed
- ~5 GB disk space

**Time**: ~20 minutes

---

## Understanding Pipeline Flow

### Pull Request (No Deploy)

When you open a PR to master/develop:

```
PR Created
    ↓
GitHub Actions triggered
    ├─ Build & Test ...................... 5-10 min
    ├─ Docker Build (no push) ............ 3-5 min
    └─ Security Scan ..................... 2-3 min
    
Result: PR shows ✅ or ❌ check
Deployment: NONE (tests only)
```

### Push to Develop (Auto Deploy)

When you push to develop branch:

```
Push to develop
    ↓
GitHub Actions triggered
    ├─ Build & Test ...................... 5-10 min
    ├─ Docker Build + Push ............... 3-5 min
    ├─ Security Scan ..................... 2-3 min
    ├─ Deploy to Development ............. 2-3 min
    └─ Notify (summary) .................. <1 min
    
Result: ✅ Success
Deployment: To development server (if secrets configured)
New image: docker.io/username/cms-connector-service:latest
```

### Push to Master (Release)

When you push to master branch (release):

```
Push to master
    ↓
GitHub Actions triggered
    ├─ Build & Test ...................... 5-10 min
    ├─ Docker Build + Push ............... 3-5 min
    ├─ Security Scan ..................... 2-3 min
    ├─ Deploy to Staging ................. 2-3 min
    └─ Notify (summary) .................. <1 min
    
Result: ✅ Success
Deployment: To staging server (if secrets configured)
New image: docker.io/username/cms-connector-service:latest
```

---

## Common Tasks

### View Workflow Logs

1. Go to **Actions** tab
2. Click on workflow run name
3. Click on job name
4. Expand steps to see output

### Re-run Failed Job

1. Go to workflow run
2. Click "Re-run failed jobs"
3. Workflow re-executes

### Debug Build Failure

```bash
# Build locally to reproduce
mvn clean package

# Check for errors
mvn compile
mvn test

# View specific test results
cat target/surefire-reports/TEST-*.xml
```

### Check Image Was Pushed

```bash
# List images on Docker Hub
docker pull your-username/cms-connector-service:latest

# Verify image works
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  your-username/cms-connector-service:latest

# Check health
curl http://localhost:8080/actuator/health
```

### View Security Scan Results

1. Go to **Security** tab on GitHub
2. Click "Code scanning alerts"
3. View detailed vulnerability report from Trivy

---

## Troubleshooting

### "Build failed" Error

**Check**:
1. Does code compile locally? `mvn compile`
2. Do tests pass locally? `mvn test`
3. View full error in GitHub Actions logs

**Common causes**:
- Missing dependencies (check pom.xml)
- Test failures (run `mvn test` locally)
- Java version mismatch (should be 17)

### "Docker push failed" Error

**Check**:
1. Are secrets set correctly?
2. Is Docker Hub token expired?

**Fix**:
1. Generate new token: https://hub.docker.com/settings/security
2. Update `DOCKER_PASSWORD` secret in GitHub

### "Health check failed" Error

**Check**:
1. Is application starting? Check logs
2. Is database accessible?
3. Is network connectivity okay?

**Fix**:
- Review application logs: `docker-compose logs cms-connector`
- Verify database is running: `docker-compose ps`
- Test endpoint: `curl http://localhost:8080/actuator/health`

### "Permission denied" SSH Error

**Check**:
1. Is SSH key configured correctly?
2. Can you SSH to deployment server manually?

**Fix**:
1. Verify public key in deployment server's `~/.ssh/authorized_keys`
2. Check file permissions: `chmod 600 ~/.ssh/authorized_keys`
3. Test: `ssh -i ~/.ssh/key deploy@host`

---

## Next Steps

### After First Successful Run:

1. **Create Development Environment** (2 minutes)
   - Go to **Settings > Environments**
   - Create "development" environment
   - Add secrets for dev server deployment

2. **Create Staging Environment** (2 minutes)
   - Create "staging" environment
   - Add secrets for staging server deployment

3. **Configure Deployment Servers** (15 minutes)
   - Add SSH public key to each server
   - Create `/opt/cms-connector` directory
   - Copy docker-compose files

4. **Test Full Pipeline** (5 minutes)
   - Push to develop (triggers dev deployment)
   - Verify app running on dev server
   - Push to master (triggers staging deployment)
   - Verify app running on staging server

### Advanced Configuration:

- Add Slack notifications
- Enable branch protection rules
- Set up approval gates for production
- Configure cost monitoring
- Set up incident alerts

---

## Files Overview

### Main Files

```
.github/
├── workflows/
│   └── docker-ci-cd.yml              ← Main workflow file
├── QUICKSTART-CICD.md               ← This file
├── README-CICD.md                   ← Full documentation
├── SECRETS_SETUP.md                 ← Secrets configuration
├── DOCKER-REGISTRY-SETUP.md         ← Docker Hub setup
└── setup-secrets.sh                 ← Helper script
```

### What Each File Does

| File | Purpose |
|------|---------|
| `docker-ci-cd.yml` | GitHub Actions workflow definition - defines all jobs |
| `README-CICD.md` | Complete CI/CD documentation with architecture details |
| `SECRETS_SETUP.md` | Guide for configuring secrets and environments |
| `DOCKER-REGISTRY-SETUP.md` | Guide for Docker Hub and image management |
| `setup-secrets.sh` | Bash script to generate SSH keys and set secrets |
| `QUICKSTART-CICD.md` | This quick start guide |

---

## Monitoring Dashboard

### Real-time Monitoring

**GitHub Actions Dashboard**: https://github.com/webemo-aaron/hdim/actions

Shows:
- Recent workflow runs
- Pass/fail status
- Execution time
- Detailed logs per job

### Alerts to Enable

1. Email notifications for failures
2. GitHub Issues for critical vulnerabilities
3. Slack integration (advanced)

---

## Cost Considerations

### GitHub Actions Free Tier

- **Includes**: 2,000 minutes/month free for private repos
- **Pipeline time**: ~20 min per run
- **Daily builds**: ~50 free builds per month
- **Cost**: $0.008 per minute over quota

**Estimate**: For 5 commits/day = ~100 minutes/month = FREE

### Docker Hub

- **Free Tier**: Unlimited pulls, 1 private repo, rate limits apply
- **Docker Pro**: $5/month - unlimited, higher rate limits
- **Registry**: Free Container Registry on GitHub (ghcr.io)

---

## Quick Commands

```bash
# View recent commits
git log --oneline -5

# Check current branch
git branch -v

# Stage changes
git add .

# Commit with message
git commit -m "Your message"

# Push to develop (triggers CI)
git push origin develop

# Push to master (releases)
git push origin master

# View GitHub Actions
# Open: https://github.com/webemo-aaron/hdim/actions

# View logs locally
docker-compose logs cms-connector

# Check health
curl http://localhost:8080/actuator/health
```

---

## Need Help?

1. **Check logs**: Actions tab → Click run → Expand failed step
2. **Read full docs**: See `README-CICD.md`
3. **Setup help**: See `SECRETS_SETUP.md`
4. **Docker issues**: See `DOCKER-REGISTRY-SETUP.md`
5. **Common errors**: See "Troubleshooting" section above

**Most issues resolved by**:
1. Reading the error message in workflow logs
2. Fixing the issue locally
3. Pushing again to re-run pipeline

---

## Success Checklist

- ✅ Docker Hub access token created
- ✅ GitHub secrets configured
- ✅ Pushed to develop branch
- ✅ Workflow executed (all green checkmarks)
- ✅ Image pushed to Docker Hub
- ✅ Can pull and run image locally

**If all checked**: CI/CD pipeline is ready! 🎉

**Next**: Configure deployment servers for auto-deployment.
