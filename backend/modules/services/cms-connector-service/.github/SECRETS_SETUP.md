# GitHub Actions Secrets Setup Guide

This guide explains how to configure the required secrets for the CI/CD pipeline.

## Required Secrets

### 1. Docker Registry Credentials
Set these in your GitHub repository settings under **Settings > Secrets and variables > Actions**.

#### DOCKER_USERNAME
- **Type**: Repository Secret
- **Value**: Your Docker Hub username
- **Example**: `myusername`
- **Used for**: Authenticating with Docker Hub to push images

#### DOCKER_PASSWORD
- **Type**: Repository Secret (encrypted)
- **Value**: Docker Hub access token (not your password)
- **Steps to create**:
  1. Go to https://hub.docker.com/settings/security
  2. Click "New Access Token"
  3. Give it a meaningful name (e.g., "GitHub Actions")
  4. Select "Read, Write, Delete" permissions
  5. Copy the token and store it securely
- **Used for**: Authenticating with Docker Hub to push images

### 2. Development Environment Secrets
Set these in your GitHub repository settings for the **development** environment.

#### DEV_DEPLOY_HOST
- **Type**: Environment Secret
- **Value**: Hostname or IP address of development server
- **Example**: `dev.cms.example.com` or `192.168.1.100`
- **Used for**: SSH connection to dev deployment target

#### DEV_DEPLOY_USER
- **Type**: Environment Secret
- **Value**: SSH username for development server
- **Example**: `ec2-user` or `ubuntu`
- **Used for**: SSH authentication to dev server

#### DEV_DEPLOY_KEY
- **Type**: Environment Secret (encrypted)
- **Value**: Private SSH key for development server (multiline)
- **Steps to generate**:
  ```bash
  ssh-keygen -t ed25519 -f ~/.ssh/github_dev -C "github-actions-dev"
  ```
- **Setup on dev server**:
  ```bash
  mkdir -p ~/.ssh
  echo "PUBLIC_KEY_HERE" >> ~/.ssh/authorized_keys
  chmod 600 ~/.ssh/authorized_keys
  ```
- **Used for**: SSH key-based authentication to deploy to dev

### 3. Staging Environment Secrets
Set these in your GitHub repository settings for the **staging** environment.

#### STAGING_DEPLOY_HOST
- **Type**: Environment Secret
- **Value**: Hostname or IP address of staging server
- **Example**: `staging.cms.example.com`
- **Used for**: SSH connection to staging deployment target

#### STAGING_DEPLOY_USER
- **Type**: Environment Secret
- **Value**: SSH username for staging server
- **Example**: `deploy` or `ubuntu`
- **Used for**: SSH authentication to staging server

#### STAGING_DEPLOY_KEY
- **Type**: Environment Secret (encrypted)
- **Value**: Private SSH key for staging server (multiline)
- **Steps to generate**:
  ```bash
  ssh-keygen -t ed25519 -f ~/.ssh/github_staging -C "github-actions-staging"
  ```
- **Setup on staging server**:
  ```bash
  mkdir -p ~/.ssh
  echo "PUBLIC_KEY_HERE" >> ~/.ssh/authorized_keys
  chmod 600 ~/.ssh/authorized_keys
  ```
- **Used for**: SSH key-based authentication to deploy to staging

## How to Set Secrets in GitHub

### Via Web UI
1. Navigate to your repository on GitHub
2. Click **Settings** tab
3. In the left sidebar, click **Secrets and variables > Actions**
4. Click **New repository secret** for repository-wide secrets
5. For environment-specific secrets:
   - Click **Environments**
   - Create/select an environment (development, staging)
   - Click **Add secret** under that environment
6. Enter the secret name and value
7. Click **Add secret**

### Via GitHub CLI
```bash
# Repository-wide secrets
gh secret set DOCKER_USERNAME --body "your-username"
gh secret set DOCKER_PASSWORD --body "$(cat ~/path/to/token)"

# Environment-specific secrets
gh secret set DEV_DEPLOY_HOST --env development --body "dev.example.com"
gh secret set DEV_DEPLOY_USER --env development --body "ubuntu"
gh secret set DEV_DEPLOY_KEY --env development --body "$(cat ~/.ssh/github_dev)"

gh secret set STAGING_DEPLOY_HOST --env staging --body "staging.example.com"
gh secret set STAGING_DEPLOY_USER --env staging --body "deploy"
gh secret set STAGING_DEPLOY_KEY --env staging --body "$(cat ~/.ssh/github_staging)"
```

## Creating GitHub Environments

1. Go to **Settings > Environments**
2. Click **New environment**
3. Create "development" and "staging" environments
4. Optionally set deployment protection rules:
   - Click the environment
   - Enable "Require reviewers" for manual approval before deployment
   - Add required reviewers (team members)

## Security Best Practices

1. **Use tokens, not passwords**: For Docker credentials, use access tokens instead of account passwords
2. **Minimize permissions**: 
   - Docker token: Only needs "Read, Write, Delete" for the specific repositories
   - SSH keys: Create specific keys for each purpose
   - SSH users: Use dedicated deployment users with limited permissions
3. **Rotate regularly**: Update secrets every 90 days or when team members change
4. **Audit access**: Regularly check GitHub Actions logs for unauthorized attempts
5. **Never commit secrets**: Ensure `.env` files and private keys are in `.gitignore`
6. **Use environment protection**: Require approvals before deploying to production

## Deployment Server Setup

On your dev and staging servers, ensure:

1. **Docker is installed and running**:
   ```bash
   docker --version
   docker-compose --version
   ```

2. **Deployment directory exists**:
   ```bash
   sudo mkdir -p /opt/cms-connector
   sudo chown deploy:deploy /opt/cms-connector  # Use your deploy user
   ```

3. **Docker compose files are in place**:
   ```bash
   cd /opt/cms-connector
   # Copy docker-compose.dev.yml or docker-compose.prod.yml
   # Copy .env.example as .env and configure
   ```

4. **SSH key-based login is enabled**:
   ```bash
   # On deployment server
   mkdir -p ~/.ssh
   chmod 700 ~/.ssh
   # Add GitHub Actions public key to authorized_keys
   chmod 600 ~/.ssh/authorized_keys
   ```

5. **Firewall allows docker-compose exec**:
   - Ensure SSH tunneling allows Docker command execution
   - May need to add deploy user to docker group:
     ```bash
     sudo usermod -aG docker deploy
     sudo systemctl restart docker
     ```

## Workflow Execution

The CI/CD pipeline triggers on:

1. **Pull Requests**: On master/develop branches
   - Runs: build-and-test, docker-build, security-scan
   - **Does NOT deploy** to any environment

2. **Push to develop**: Automatic deployment to **development**
   - Runs: All jobs
   - Deploys to: Development environment after security scan passes

3. **Push to master**: Automatic deployment to **staging**
   - Runs: All jobs
   - Deploys to: Staging environment after security scan passes

## Monitoring Pipeline Execution

1. Go to **Actions** tab in GitHub repository
2. Click on the workflow run to see detailed logs
3. Each job shows:
   - Build & Test: Maven compilation, unit tests, coverage
   - Docker Build: Multi-platform Docker image build
   - Security Scan: Trivy vulnerability scanning results
   - Deploy Dev/Staging: Deployment logs and health checks
   - Notify: Summary of all job statuses

## Troubleshooting

### "Authentication failed" on Docker login
- Verify DOCKER_USERNAME and DOCKER_PASSWORD secrets are set correctly
- Docker password must be an access token, not your account password
- Check token hasn't expired

### "Permission denied" on SSH deployment
- Verify SSH key is correctly added to authorized_keys on deployment server
- Check file permissions: `authorized_keys` should be 600, `.ssh` should be 700
- Verify deploy user exists and SSH daemon is running

### "Health check failed" after deployment
- Check application logs: `docker-compose logs cms-connector`
- Verify environment variables are set in .env file
- Ensure database is running: `docker-compose ps`
- Check database connectivity from application container

### Trivy scan fails or shows vulnerabilities
- Review scan output in GitHub Security tab
- For high/critical vulnerabilities:
  1. Check if base image has been updated
  2. Update dependencies in pom.xml
  3. Run scan again after updates

## Next Steps

1. Create SSH keys and add to deployment servers
2. Set all required secrets in GitHub
3. Create development and staging environments if not already present
4. Make a test commit to develop branch to trigger the pipeline
5. Monitor the workflow execution in the Actions tab
