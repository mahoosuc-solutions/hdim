# Test Harness Deployment Guide

## Overview

This guide covers building and deploying the HDIM Test Harness, which includes:
1. **Testing Dashboard** - Angular web application with comprehensive testing tools
2. **Validation Scripts** - Node.js/TypeScript scripts for automated testing

## Prerequisites

- Node.js 20.x or higher
- npm 10.x or higher
- Docker (optional, for containerized deployment)
- Access to HDIM backend services

## Quick Start

### Automated Build

```bash
# Run the build script
./scripts/build-test-harness.sh
```

This will:
1. Build the Angular testing dashboard
2. Prepare validation scripts
3. Create deployment package
4. Generate Docker configuration

### Manual Build

#### Step 1: Build Angular Application

```bash
# From project root
npm run nx -- build clinical-portal --configuration=production
```

Output: `dist/apps/clinical-portal/browser`

#### Step 2: Prepare Validation Scripts

```bash
cd test-harness/validation
npm install
npx tsc --noEmit  # Verify compilation
```

## Deployment Options

### Option 1: Docker Deployment (Recommended)

```bash
# Build deployment package first
./scripts/build-test-harness.sh

# Start test harness web server
docker-compose -f docker-compose.test-harness.yml up -d

# Check status
docker ps | grep test-harness

# View logs
docker logs hdim-test-harness-web

# Access dashboard
open http://localhost:8080/testing
```

**Stop the service:**
```bash
docker-compose -f docker-compose.test-harness.yml down
```

### Option 2: Nginx Server

```bash
# Copy files to nginx directory
sudo cp -r dist/test-harness/clinical-portal/* /var/www/html/

# Or use custom location
sudo cp -r dist/test-harness/clinical-portal/* /var/www/test-harness/
sudo chown -R www-data:www-data /var/www/test-harness

# Configure nginx (see docker/nginx/test-harness.conf for reference)
sudo systemctl reload nginx
```

### Option 3: Node.js http-server

```bash
cd dist/test-harness/clinical-portal
npx http-server -p 8080 -c-1
```

### Option 4: Static Hosting Services

#### AWS S3 + CloudFront

```bash
# Upload to S3
aws s3 sync dist/test-harness/clinical-portal/ s3://your-bucket/test-harness/ \
  --delete \
  --cache-control "public, max-age=31536000, immutable"

# Invalidate CloudFront
aws cloudfront create-invalidation \
  --distribution-id YOUR_DIST_ID \
  --paths "/*"
```

#### Netlify

```bash
# Install Netlify CLI
npm install -g netlify-cli

# Deploy
cd dist/test-harness/clinical-portal
netlify deploy --prod --dir=.
```

#### Vercel

```bash
# Install Vercel CLI
npm install -g vercel

# Deploy
cd dist/test-harness/clinical-portal
vercel --prod
```

## Accessing the Testing Dashboard

### URL
- Local: `http://localhost:8080/testing`
- Production: `https://your-domain.com/testing`

### Authentication
- Requires: `DEVELOPER` or `ADMIN` role
- Login with appropriate credentials
- Or use demo mode: `?demo=true` (if enabled)

### Features Available
1. **Demo Scenarios** - Load test scenarios
2. **API Testing** - Test backend endpoints
3. **Data Management** - Seed/validate/reset test data
4. **Service Health** - Monitor backend services
5. **Test Results** - View and export test execution history

## Running Validation Scripts

The validation scripts can run independently of the web dashboard:

```bash
cd test-harness/validation

# Install dependencies (if not already done)
npm install

# Run smoke tests
./run-validation.sh --tier smoke

# Run all validation tiers
./run-validation.sh --tier all

# Run specific test suite
npm run test:smoke
npm run test:functional
npm run test:integration
npm run test:performance
```

## Configuration

### Environment Variables

For validation scripts, create `.env` file in `test-harness/validation/`:

```env
# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=hdim
DB_USER=hdim_user
DB_PASSWORD=your_password

# API Configuration
API_BASE_URL=http://localhost:18080
TENANT_ID=acme-health

# Test Configuration
TEST_TIMEOUT_MS=30000
MAX_RETRIES=3
```

### API Configuration

The testing dashboard uses the same API configuration as the main clinical portal:
- Gateway URL: Configured in `apps/clinical-portal/src/app/config/api.config.ts`
- Tenant ID: Defaults to `acme-health` (can be changed in TestingService)

## Troubleshooting

### Build Failures

**Issue:** Angular build fails
```bash
# Check Node.js version
node --version  # Should be 20.x or higher

# Clear cache and rebuild
rm -rf node_modules dist .nx
npm install
npm run nx -- build clinical-portal --configuration=production
```

**Issue:** TypeScript compilation errors
```bash
# Check TypeScript version
npx tsc --version

# Fix linting issues
npm run nx -- lint clinical-portal
```

### Deployment Issues

**Issue:** Dashboard not accessible
- Check nginx/Docker logs
- Verify port 8080 is not in use
- Check firewall rules
- Verify files were copied correctly

**Issue:** API calls failing
- Verify backend services are running
- Check API gateway URL configuration
- Verify CORS settings
- Check network connectivity

**Issue:** Authentication fails
- Verify user has DEVELOPER or ADMIN role
- Check JWT token validity
- Verify auth service is running

### Validation Script Issues

**Issue:** Database connection fails
- Verify PostgreSQL is running
- Check database credentials in `.env`
- Verify network connectivity
- Check database user permissions

**Issue:** API tests fail
- Verify backend services are healthy
- Check API gateway is accessible
- Verify tenant ID is correct
- Check service logs for errors

## Monitoring

### Health Checks

**Web Dashboard:**
```bash
curl http://localhost:8080/health
```

**Docker Container:**
```bash
docker inspect hdim-test-harness-web | grep Health -A 10
```

### Logs

**Docker:**
```bash
docker logs -f hdim-test-harness-web
```

**Nginx:**
```bash
tail -f /var/log/nginx/access.log
tail -f /var/log/nginx/error.log
```

## Security Considerations

1. **Access Control**
   - Testing dashboard requires DEVELOPER/ADMIN role
   - Do not expose to public internet without authentication
   - Use HTTPS in production

2. **Data Protection**
   - Test data may contain PHI - handle with care
   - Use separate test databases
   - Follow HIPAA guidelines for test data

3. **Network Security**
   - Restrict access to testing dashboard
   - Use VPN or private network
   - Implement rate limiting

## Maintenance

### Updating the Dashboard

```bash
# Rebuild after code changes
./scripts/build-test-harness.sh

# Restart Docker container
docker-compose -f docker-compose.test-harness.yml restart
```

### Cleaning Up

```bash
# Remove deployment package
rm -rf dist/test-harness

# Stop and remove Docker containers
docker-compose -f docker-compose.test-harness.yml down -v

# Clean build artifacts
rm -rf dist/apps/clinical-portal
```

## Support

For issues or questions:
1. Check logs (see Monitoring section)
2. Review validation reports in `test-harness/validation/reports/`
3. Check test results in dashboard (Test Results section)
4. Review documentation in `docs/testing/`

## Next Steps

After deployment:
1. Access the testing dashboard
2. Run smoke tests to verify deployment
3. Load a demo scenario
4. Test API endpoints
5. Monitor service health
6. Export test results for analysis

---

**Last Updated:** December 30, 2025
