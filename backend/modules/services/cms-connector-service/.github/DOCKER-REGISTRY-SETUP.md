# Docker Registry Integration Setup

## Overview

The CI/CD pipeline pushes Docker images to Docker Hub after successful builds. This guide covers setting up Docker registry credentials and configuring your CI/CD pipeline for automated image publishing.

## Docker Hub Account Setup

### 1. Create Docker Hub Account (if needed)

1. Visit https://hub.docker.com/
2. Click "Sign Up"
3. Create account with username and email
4. Verify email address

### 2. Create Access Token

Access tokens are more secure than passwords for automated tools.

1. Log in to Docker Hub: https://hub.docker.com/login
2. Navigate to **Account Settings > Security**
3. Click **New Access Token**
4. **Token name**: Enter "github-actions-cms" (or similar descriptive name)
5. **Access permissions**: Select "Read, Write, Delete"
   - Read: Pull images
   - Write: Push images
   - Delete: Clean up old images
6. Click **Generate**
7. **Save the token immediately** - you won't be able to see it again
8. Copy the token to a secure location

### 3. Configure GitHub Secrets

Store Docker credentials as GitHub secrets so they're not visible in logs.

#### Using Web UI:

1. Go to your repository on GitHub
2. Click **Settings** tab
3. In left sidebar: **Secrets and variables > Actions**
4. Click **New repository secret**
5. Create secret `DOCKER_USERNAME`:
   - **Name**: `DOCKER_USERNAME`
   - **Value**: Your Docker Hub username
   - Click **Add secret**
6. Create secret `DOCKER_PASSWORD`:
   - **Name**: `DOCKER_PASSWORD`
   - **Value**: The access token from step 2 (not your password)
   - Click **Add secret**

#### Using GitHub CLI:

```bash
# Log in to GitHub if needed
gh auth login

# Set Docker secrets
gh secret set DOCKER_USERNAME --body "your-docker-username"
gh secret set DOCKER_PASSWORD --body "your-access-token"

# Verify secrets are set
gh secret list
```

## Image Naming and Tagging

### Image Repository URL

Images are pushed to:
```
docker.io/your-docker-username/cms-connector-service:tag
```

Example for username "acmedev":
```
docker.io/acmedev/cms-connector-service:latest
docker.io/acmedev/cms-connector-service:abc123def456
```

### Tagging Strategy

The CI/CD pipeline automatically tags images:

1. **Latest Tag** (`latest`)
   - Points to most recent successful master/develop build
   - Updated on every push to master/develop
   - **Use case**: Quick reference to latest release

2. **Commit SHA Tag** (e.g., `abc123def456`)
   - Immutable reference to specific commit
   - Never changes
   - **Use case**: Production deployments, disaster recovery, debugging

### Example Workflow

```
Push to develop (commit: abc123)
    ↓
Build & Test ✓
    ↓
Docker Build:
    - Builds image
    - Tags as: `latest` and `abc123`
    - Pushes both tags
    ↓
Image available:
    - docker.io/username/cms-connector-service:latest
    - docker.io/username/cms-connector-service:abc123

Later, push to develop (commit: def789)
    ↓
    [Same process]
    ↓
Image tags updated:
    - docker.io/username/cms-connector-service:latest  (now points to def789)
    - docker.io/username/cms-connector-service:def789  (new image)
    - docker.io/username/cms-connector-service:abc123  (still available)
```

## Using Images in Production

### Pull Images from Registry

Once images are pushed to Docker Hub, you can pull them:

```bash
# Pull latest image
docker pull your-docker-username/cms-connector-service:latest

# Pull specific commit version
docker pull your-docker-username/cms-connector-service:abc123def456

# Run container
docker run -p 8080:8080 your-docker-username/cms-connector-service:latest
```

### With Docker Compose

Update your `docker-compose.prod.yml`:

```yaml
services:
  cms-connector:
    image: your-docker-username/cms-connector-service:latest
    # ... rest of configuration
```

Or specify exact version:

```yaml
cms-connector:
  image: your-docker-username/cms-connector-service:abc123def456
```

## Private Registry (Optional)

If using a private Docker registry instead of Docker Hub:

### 1. Update Workflow

Edit `.github/workflows/docker-ci-cd.yml`:

```yaml
env:
  REGISTRY: your-registry.com  # Change this
  IMAGE_NAME: cms-connector-service
```

### 2. Update Login Action

```yaml
- name: Login to Private Registry
  uses: docker/login-action@v3
  with:
    registry: ${{ env.REGISTRY }}
    username: ${{ secrets.PRIVATE_REGISTRY_USERNAME }}
    password: ${{ secrets.PRIVATE_REGISTRY_PASSWORD }}
```

### 3. Add Registry Credentials

```bash
gh secret set PRIVATE_REGISTRY_USERNAME --body "registry-user"
gh secret set PRIVATE_REGISTRY_PASSWORD --body "registry-token"
```

## Image Size Optimization

### Current Size

CMS Connector Service Docker image:
- **Base**: eclipse-temurin:17-jre-alpine (~200MB)
- **Application JAR**: ~80MB
- **Total**: ~290-320MB uncompressed, ~490MB with all layers

### Size Breakdown

```
eclipse-temurin:17-jre-alpine    200MB (base OS + Java runtime)
cms-connector-service JAR         80MB  (compiled application)
Spring Boot embedded Tomcat       ~15MB (web server)
PostgreSQL JDBC driver            ~3MB
Redis client library              ~2MB
Other dependencies                ~10MB
```

### Optimization Techniques

1. **Use Alpine Linux Base** ✓
   - Already using alpine (not full Ubuntu/Debian)
   - Saves ~300MB vs full OS

2. **Multi-stage Build** ✓
   - Already implemented
   - Only runtime environment in final image
   - Builder stage discarded after compilation

3. **Remove Non-Essential Dependencies**
   - Review `pom.xml` for unused libraries
   - Example: Exclude logging frameworks if using single provider
   ```xml
   <exclusions>
     <exclusion>
       <groupId>commons-logging</groupId>
       <artifactId>commons-logging</artifactId>
     </exclusion>
   </exclusions>
   ```

4. **Compress JAR** (Advanced)
   - Use ProGuard or Yguard to minimize JAR
   - Can reduce JAR by 20-30%
   - Trade-off: stack traces less readable

## Troubleshooting

### "Unauthorized: authentication required"

**Problem**: Docker push fails during workflow

**Solution**:
1. Verify `DOCKER_USERNAME` secret is set correctly
2. Verify `DOCKER_PASSWORD` is an access token (not your password)
3. Regenerate access token:
   - Go to https://hub.docker.com/settings/security
   - Delete old token
   - Create new token with same permissions
   - Update `DOCKER_PASSWORD` secret in GitHub

### "No space left on device"

**Problem**: Docker build fails with disk space error

**Solution**:
1. GitHub Actions runners have ~14GB disk space
2. Image too large
3. Clean up old images: 
   ```bash
   docker image prune -a --filter "until=72h"
   ```
4. Optimize Dockerfile to reduce final image size

### "Image not found when pulling"

**Problem**: Can pull `latest` but not specific SHA tag

**Solution**:
1. Verify both tags were pushed:
   ```bash
   docker pull your-user/cms-connector-service:latest
   docker pull your-user/cms-connector-service:SHA
   ```
2. Check Docker Hub web UI under "Tags" tab
3. Verify build job actually pushed (check workflow logs)

### "Registry returned status code 429: Too Many Requests"

**Problem**: Rate limit exceeded on Docker Hub

**Solution**:
- Docker Hub free tier has pull rate limits
- Options:
  1. Upgrade to Docker Pro ($5/month)
  2. Use GitHub Container Registry (ghcr.io) - free
  3. Wait 6 hours for rate limit to reset

## Alternative: GitHub Container Registry

GitHub provides free container registry for public repositories.

### Update Workflow for GHCR:

```yaml
env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}/cms-connector-service

- name: Login to GitHub Container Registry
  uses: docker/login-action@v3
  with:
    registry: ghcr.io
    username: ${{ github.actor }}
    password: ${{ secrets.GITHUB_TOKEN }}
```

No additional secrets needed - `GITHUB_TOKEN` is built-in.

### Images Available At:

```
ghcr.io/webemo-aaron/hdim/cms-connector-service:latest
ghcr.io/webemo-aaron/hdim/cms-connector-service:abc123
```

## Best Practices

1. **Always use access tokens, not passwords**
   - More secure
   - Can be rotated independently
   - Can be revoked if compromised

2. **Tag images with commit SHA**
   - Provides audit trail
   - Enables rollback to any previous version
   - Enables reproducible deployments

3. **Use immutable image references in production**
   - Don't use `latest` in production deployments
   - Use specific commit SHA tag instead
   - Ensures predictable behavior

4. **Clean up old images**
   - Docker Hub free tier has storage limits
   - Delete images older than 90 days
   - Keep only last 10 tagged versions

5. **Scan images for vulnerabilities**
   - Trivy already integrated in workflow
   - Review SARIF results regularly
   - Update base image when security patches available

6. **Monitor image pulls**
   - Check Docker Hub activity logs
   - Verify only authorized systems are pulling images
   - Alert on suspicious pull patterns

## Monitoring Image Usage

### Docker Hub Dashboard

1. Go to https://hub.docker.com/
2. Click on repository: `cms-connector-service`
3. View:
   - **Tags**: All available versions
   - **Activity**: Recent pulls and pushes
   - **Collaborators**: Who has access
   - **Webhooks**: Integration settings

### Checking Image Details

```bash
# Inspect pulled image
docker image inspect cms-connector-service:latest

# View image history
docker history cms-connector-service:latest

# Check image size
docker images cms-connector-service
```

## Security Considerations

### Prevent Unauthorized Pushes

1. **Make repository public** (recommended)
   - Allows anyone to pull images
   - Only authorized CI/CD pushes via token
   - Mirrors source code on GitHub (already public)

2. **Use minimal access tokens**
   - Don't reuse tokens across services
   - Create separate token for each purpose
   - Rotate tokens quarterly

3. **Monitor token usage**
   - Review activity logs in Docker Hub
   - Delete unused tokens immediately
   - Audit GitHub repository access logs

### Image Signing (Advanced)

For critical production deployments, sign images:

```bash
# Enable Docker Content Trust
export DOCKER_CONTENT_TRUST=1
docker push your-user/cms-connector-service:latest
```

Requires additional setup with Notary keys.

## Integration with Deployment

### Development Deployment

Dev server pulls latest:
```yaml
# docker-compose.dev.yml
image: your-user/cms-connector-service:latest
```

Pulls latest tagged version on each deployment.

### Staging/Production Deployment

Staging server pins to specific version:
```yaml
# docker-compose.prod.yml
image: your-user/cms-connector-service:abc123def456
```

Always deploys exact commit, never auto-upgrades.

## Workflow Summary

```
Commit code → Push to GitHub
              ↓
         GitHub Actions triggered
              ↓
         Build & Test ✓
              ↓
         Build Docker Image
              ↓
         Tag: latest + SHA
              ↓
         Push to Docker Registry
              ↓
         Development:
         docker-compose pulls :latest
         
         Staging:
         docker-compose pulls :abc123 (specific version)
```

## Next Steps

1. Create Docker Hub account and access token
2. Add `DOCKER_USERNAME` and `DOCKER_PASSWORD` secrets to GitHub
3. Trigger workflow by pushing to develop branch
4. Verify image appears in Docker Hub
5. Test pulling and running image locally
6. Update docker-compose files to use your registry credentials

## Support Resources

- Docker Hub Docs: https://docs.docker.com/docker-hub/
- Access Tokens: https://docs.docker.com/docker-hub/access-tokens/
- GitHub Actions Docker: https://github.com/docker/login-action
- Private Registries: https://docs.docker.com/registry/
