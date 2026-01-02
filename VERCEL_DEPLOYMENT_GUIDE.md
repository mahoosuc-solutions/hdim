# Vercel Deployment Guide - HealthData In Motion

**Date**: November 20, 2025
**Version**: 1.0
**Status**: Ready for Production Deployment

---

## Overview

This guide covers deploying the HealthData In Motion Clinical Portal (Angular frontend) to Vercel, while the backend services (Spring Boot) are deployed separately to a cloud provider.

### Architecture

```
┌─────────────────────────────────────────────┐
│         Vercel (Frontend Only)              │
│  ┌───────────────────────────────────────┐  │
│  │   Clinical Portal (Angular SPA)       │  │
│  │   - Patient Health Overview           │  │
│  │   - Quality Measures Dashboard        │  │
│  │   - Mental Health Assessments         │  │
│  └───────────────────────────────────────┘  │
└────────────────┬────────────────────────────┘
                 │ HTTPS API Calls
                 ↓
┌─────────────────────────────────────────────┐
│   API Gateway (Kong) - AWS/Railway/Render   │
│  ┌───────────────────────────────────────┐  │
│  │  Route /api/quality → Quality Service │  │
│  │  Route /api/cql → CQL Engine Service  │  │
│  │  Route /api/fhir → FHIR Server        │  │
│  └───────────────────────────────────────┘  │
└────────────────┬────────────────────────────┘
                 │
                 ↓
┌─────────────────────────────────────────────┐
│     Backend Services (Spring Boot)          │
│  ┌──────────────────┬──────────────────┐    │
│  │ Quality Measure  │  CQL Engine      │    │
│  │ Service          │  Service         │    │
│  └──────────────────┴──────────────────┘    │
│  ┌──────────────────┬──────────────────┐    │
│  │ FHIR Server      │  PostgreSQL DB   │    │
│  └──────────────────┴──────────────────┘    │
└─────────────────────────────────────────────┘
```

---

## Prerequisites

### 1. Vercel Account Setup
- [ ] Create account at [vercel.com](https://vercel.com)
- [ ] Install Vercel CLI: `npm install -g vercel`
- [ ] Login to Vercel: `vercel login`

### 2. Backend Deployment (Required First)
- [ ] Deploy backend services (see [BACKEND_DEPLOYMENT_OPTIONS.md](#backend-deployment-options))
- [ ] Note your API Gateway URL (e.g., `https://api.healthdata-in-motion.com`)
- [ ] Verify backend health checks are passing

### 3. Domain Setup (Optional)
- [ ] Purchase domain (e.g., `healthdata-in-motion.com`)
- [ ] Configure DNS to point to Vercel

---

## Quick Start Deployment

### Option 1: Deploy via Vercel CLI (Recommended for First Deploy)

1. **Build locally to verify**:
```bash
# From project root
npx nx build clinical-portal --configuration=production

# Verify build output
ls -la dist/apps/clinical-portal/browser/
```

2. **Deploy to Vercel**:
```bash
# First deployment (interactive)
vercel

# Follow prompts:
# - Set up and deploy: Y
# - Which scope: [Your account/team]
# - Link to existing project: N
# - Project name: healthdata-in-motion
# - Directory with code: ./ (project root)
# - Override settings: Y
#   - Build Command: npx nx build clinical-portal --configuration=production
#   - Output Directory: dist/apps/clinical-portal/browser
#   - Install Command: npm install
```

3. **Set environment variables**:
```bash
# Production API URLs
vercel env add API_GATEWAY_URL production
# Enter: https://api.healthdata-in-motion.com

vercel env add QUALITY_MEASURE_URL production
# Enter: https://quality-api.healthdata-in-motion.com

vercel env add CQL_ENGINE_URL production
# Enter: https://cql-api.healthdata-in-motion.com

vercel env add FHIR_SERVER_URL production
# Enter: https://fhir-api.healthdata-in-motion.com

vercel env add DEFAULT_TENANT_ID production
# Enter: default
```

4. **Deploy to production**:
```bash
vercel --prod
```

### Option 2: Deploy via GitHub Integration (Recommended for CI/CD)

1. **Push code to GitHub**:
```bash
git add .
git commit -m "Add Vercel deployment configuration"
git push origin main
```

2. **Connect to Vercel**:
   - Go to [vercel.com/new](https://vercel.com/new)
   - Click "Import Git Repository"
   - Select your GitHub repository
   - Configure project:
     - **Framework Preset**: Other
     - **Root Directory**: `./`
     - **Build Command**: `npx nx build clinical-portal --configuration=production`
     - **Output Directory**: `dist/apps/clinical-portal/browser`
     - **Install Command**: `npm install`

3. **Add Environment Variables** in Vercel Dashboard:
   - Go to Project Settings → Environment Variables
   - Add the following:

   | Variable | Value | Environment |
   |----------|-------|-------------|
   | `API_GATEWAY_URL` | `https://api.healthdata-in-motion.com` | Production |
   | `QUALITY_MEASURE_URL` | `https://quality-api.healthdata-in-motion.com` | Production |
   | `CQL_ENGINE_URL` | `https://cql-api.healthdata-in-motion.com` | Production |
   | `FHIR_SERVER_URL` | `https://fhir-api.healthdata-in-motion.com` | Production |
   | `DEFAULT_TENANT_ID` | `default` | Production |

4. **Deploy**:
   - Vercel will automatically deploy on every push to `main`
   - Or click "Deploy" in Vercel dashboard

---

## Environment Variables Reference

### Required Variables (Production)

```bash
# API Gateway URL (Kong or direct backend)
API_GATEWAY_URL=https://api.healthdata-in-motion.com

# Individual service URLs (if not using gateway)
QUALITY_MEASURE_URL=https://quality-api.healthdata-in-motion.com
CQL_ENGINE_URL=https://cql-api.healthdata-in-motion.com
FHIR_SERVER_URL=https://fhir-api.healthdata-in-motion.com

# Tenant configuration
DEFAULT_TENANT_ID=default
```

### Optional Variables

```bash
# Analytics
GA_MEASUREMENT_ID=G-XXXXXXXXXX

# Error reporting (Sentry)
SENTRY_DSN=https://xxxxx@xxxxx.ingest.sentry.io/xxxxx

# Feature flags
ENABLE_ANALYTICS=true
ENABLE_ERROR_REPORTING=true
ENABLE_DEBUG_MODE=false
```

### Preview/Staging Environment Variables

For preview deployments (e.g., `staging` branch):

```bash
# Staging backend URLs
API_GATEWAY_URL=https://staging-api.healthdata-in-motion.com
QUALITY_MEASURE_URL=https://staging-quality-api.healthdata-in-motion.com
CQL_ENGINE_URL=https://staging-cql-api.healthdata-in-motion.com
FHIR_SERVER_URL=https://staging-fhir-api.healthdata-in-motion.com
```

---

## Vercel Configuration Files

### 1. `vercel.json` (Root Directory)

Already created with:
- Build command configuration
- Output directory
- SPA routing (all routes → index.html)
- Security headers (CSP, X-Frame-Options, etc.)
- Asset caching (1 year for static assets)

### 2. `.vercelignore` (Root Directory)

Already created to exclude:
- `node_modules`
- `backend/` (not deployed to Vercel)
- Build artifacts
- Development files

### 3. Environment Files

Created two environment files:
- `apps/clinical-portal/src/environments/environment.ts` (development)
- `apps/clinical-portal/src/environments/environment.prod.ts` (production)

---

## Custom Domain Setup

### 1. Add Domain in Vercel

```bash
# Via CLI
vercel domains add healthdata-in-motion.com

# Or via Dashboard:
# Project Settings → Domains → Add Domain
```

### 2. Configure DNS

**Option A: Vercel Nameservers (Recommended)**
- Point your domain's nameservers to Vercel
- Vercel will manage all DNS records

**Option B: CNAME Record**
- Add CNAME record: `www.healthdata-in-motion.com` → `cname.vercel-dns.com`
- Add A record for apex: `healthdata-in-motion.com` → `76.76.21.21`

### 3. Enable HTTPS

- Vercel automatically provisions SSL certificates via Let's Encrypt
- Force HTTPS redirect (enabled by default)

### 4. Configure Subdomains

```bash
# Add subdomain
vercel domains add clinical.healthdata-in-motion.com

# Configure for different environments
clinical.healthdata-in-motion.com → Production
staging.healthdata-in-motion.com → Staging branch
```

---

## Deployment Workflow

### Automatic Deployments (GitHub Integration)

**Production Deployment** (triggered by push to `main`):
```bash
git checkout main
git pull origin main
# Make changes
git add .
git commit -m "Feature: Add patient detail view"
git push origin main
# Vercel automatically deploys to production
```

**Preview Deployment** (triggered by pull request):
```bash
git checkout -b feature/new-dashboard
# Make changes
git add .
git commit -m "WIP: New dashboard design"
git push origin feature/new-dashboard
# Create pull request on GitHub
# Vercel automatically creates preview deployment
```

**Staging Deployment** (triggered by push to `staging`):
```bash
git checkout staging
git merge main
git push origin staging
# Vercel deploys to staging environment
```

### Manual Deployments (Vercel CLI)

**Deploy to preview**:
```bash
vercel
```

**Deploy to production**:
```bash
vercel --prod
```

**Deploy specific branch**:
```bash
git checkout staging
vercel --prod --scope staging
```

---

## Post-Deployment Verification

### 1. Health Check

Visit your deployed URL and verify:
```bash
# Open in browser
open https://healthdata-in-motion.vercel.app

# Or your custom domain
open https://clinical.healthdata-in-motion.com
```

**Checklist**:
- [ ] Application loads without errors
- [ ] All routes work (patient list, dashboard, reports)
- [ ] Assets load correctly (images, fonts, icons)
- [ ] No console errors
- [ ] API calls succeed (check Network tab)

### 2. API Connectivity Test

Open browser DevTools → Network tab and verify:
- [ ] API calls go to correct backend URL
- [ ] CORS headers are present
- [ ] Authentication tokens are included
- [ ] Responses are successful (200 OK)

Example API call to verify:
```bash
# From deployed frontend, check console:
fetch('https://api.healthdata-in-motion.com/quality-measure/actuator/health')
  .then(r => r.json())
  .then(console.log)
```

### 3. Performance Check

Use Vercel Analytics or Lighthouse:
```bash
# Run Lighthouse
npx lighthouse https://clinical.healthdata-in-motion.com --view

# Check Vercel Analytics
# Dashboard → Analytics → Speed Insights
```

**Target Metrics**:
- Performance: 90+
- Accessibility: 95+
- Best Practices: 90+
- SEO: 80+
- First Contentful Paint: <1.5s
- Time to Interactive: <3.5s

### 4. Security Headers Check

```bash
# Check security headers
curl -I https://clinical.healthdata-in-motion.com

# Should see:
# X-Content-Type-Options: nosniff
# X-Frame-Options: DENY
# X-XSS-Protection: 1; mode=block
# Referrer-Policy: strict-origin-when-cross-origin
```

---

## Troubleshooting

### Issue 1: Build Fails on Vercel

**Error**: `Cannot find module '@angular/core'`

**Solution**:
```bash
# Verify package.json has all dependencies
# Ensure @angular/core is in "dependencies" not "devDependencies"

# Test build locally
npx nx build clinical-portal --configuration=production

# If local build works but Vercel fails, check Node version
# In vercel.json or dashboard, set:
"engines": {
  "node": "20.x"
}
```

### Issue 2: API Calls Fail (CORS)

**Error**: `Access to fetch at 'https://api...' from origin 'https://clinical...' has been blocked by CORS`

**Solution**:
```bash
# Backend must allow your Vercel domain in CORS configuration

# In Spring Boot (QualityMeasureSecurityConfig.java):
CorsConfiguration configuration = new CorsConfiguration();
configuration.setAllowedOrigins(Arrays.asList(
    "https://clinical.healthdata-in-motion.com",
    "https://*.vercel.app"  // Allow all Vercel preview deployments
));
```

### Issue 3: Environment Variables Not Working

**Error**: API calls go to `localhost` instead of production URL

**Solution**:
```bash
# Verify environment variables are set in Vercel
vercel env ls

# Pull environment variables to test locally
vercel env pull

# Redeploy to pick up new env vars
vercel --prod
```

### Issue 4: 404 on Refresh

**Error**: Refreshing `/patients` returns 404

**Solution**:
```bash
# Verify vercel.json has rewrites configuration:
"rewrites": [
  {
    "source": "/(.*)",
    "destination": "/index.html"
  }
]

# This ensures all routes go to index.html (SPA routing)
```

### Issue 5: Build Output Directory Not Found

**Error**: `Error: No Output Directory named "dist/apps/clinical-portal/browser" found`

**Solution**:
```bash
# Check Angular build configuration
# In apps/clinical-portal/project.json:
"options": {
  "outputPath": "dist/apps/clinical-portal"
}

# Vercel output directory should be:
dist/apps/clinical-portal/browser

# Verify build locally:
npx nx build clinical-portal --configuration=production
ls -la dist/apps/clinical-portal/browser/
```

---

## Monitoring & Analytics

### 1. Vercel Analytics (Built-in)

Enable in Vercel Dashboard:
- Project Settings → Analytics → Enable

**Metrics Available**:
- Page views and unique visitors
- Top pages and referrers
- Device and browser breakdown
- Core Web Vitals (LCP, FID, CLS)
- Real User Monitoring (RUM)

### 2. Google Analytics (Optional)

Add GA4 tracking:

1. Create GA4 property at [analytics.google.com](https://analytics.google.com)
2. Get Measurement ID (e.g., `G-XXXXXXXXXX`)
3. Add to Vercel environment variables:
```bash
vercel env add GA_MEASUREMENT_ID production
# Enter: G-XXXXXXXXXX
```

4. Add tracking code to `index.html` or use Angular Google Analytics package

### 3. Error Monitoring with Sentry (Optional)

1. Create account at [sentry.io](https://sentry.io)
2. Create new Angular project
3. Get DSN (e.g., `https://xxxxx@xxxxx.ingest.sentry.io/xxxxx`)
4. Add to environment variables:
```bash
vercel env add SENTRY_DSN production
# Enter: https://xxxxx@xxxxx.ingest.sentry.io/xxxxx
```

5. Install Sentry SDK:
```bash
npm install --save @sentry/angular
```

6. Configure in `main.ts`:
```typescript
import * as Sentry from "@sentry/angular";

Sentry.init({
  dsn: environment.errorReporting.dsn,
  environment: environment.errorReporting.environment,
  tracesSampleRate: 1.0,
});
```

---

## Performance Optimization

### 1. Enable Edge Caching

Already configured in `vercel.json`:
```json
{
  "headers": [
    {
      "source": "/assets/(.*)",
      "headers": [
        {
          "key": "Cache-Control",
          "value": "public, max-age=31536000, immutable"
        }
      ]
    }
  ]
}
```

### 2. Image Optimization

Use Vercel Image Optimization:
```typescript
// Instead of:
<img src="/assets/logo.png" />

// Use:
<img src="/_next/image?url=/assets/logo.png&w=640&q=75" />
```

Or install `@vercel/image-loader`:
```bash
npm install @vercel/image-loader
```

### 3. Code Splitting

Already enabled by Angular build:
```bash
# Verify in build output:
npx nx build clinical-portal --configuration=production

# Look for:
# - main.xxx.js (main bundle)
# - polyfills.xxx.js
# - Lazy-loaded route chunks
```

### 4. Prerender Static Routes (Optional)

For frequently accessed routes:
```typescript
// In project.json, add:
"prerender": {
  "executor": "@angular/build:prerender",
  "options": {
    "routes": [
      "/",
      "/dashboard",
      "/patients"
    ]
  }
}
```

---

## CI/CD Best Practices

### 1. Branch Strategy

**Recommended Setup**:
- `main` → Production deployment
- `staging` → Staging deployment
- `feature/*` → Preview deployments

**Vercel Configuration**:
```bash
# In Vercel Dashboard → Git → Configure:
Production Branch: main
Preview Deployments: All branches
Ignored Build Step: None
```

### 2. Deployment Checks

Add `pre-deploy` script to `package.json`:
```json
{
  "scripts": {
    "pre-deploy": "npm run lint && npm run test && npm run build"
  }
}
```

Run before deploying:
```bash
npm run pre-deploy
```

### 3. Rollback Strategy

**If deployment fails**:
```bash
# Via CLI - redeploy previous version
vercel rollback

# Or via Dashboard:
# Deployments → [Previous successful deployment] → Promote to Production
```

### 4. Deployment Protection

Enable in Vercel Dashboard:
- Settings → Deployment Protection → Enable
- Require approval for production deployments
- Set up Slack/Discord notifications

---

## Security Checklist

### Pre-Deployment
- [ ] All API keys in environment variables (not hardcoded)
- [ ] Sensitive data not in git repository
- [ ] `.env` files in `.gitignore`
- [ ] Security headers configured in `vercel.json`
- [ ] HTTPS enforced (automatic in Vercel)
- [ ] CORS properly configured on backend

### Post-Deployment
- [ ] Test authentication flows
- [ ] Verify HTTPS certificate
- [ ] Check security headers (securityheaders.com)
- [ ] Test CORS from deployed domain
- [ ] Review Vercel access logs
- [ ] Enable DDoS protection (Vercel Pro plan)

### Ongoing
- [ ] Regular dependency updates (`npm audit fix`)
- [ ] Monitor error logs for security issues
- [ ] Review access logs weekly
- [ ] Rotate API keys quarterly

---

## Cost Estimation

### Vercel Pricing Tiers

**Hobby (Free)**:
- Unlimited deployments
- 100 GB bandwidth/month
- Automatic HTTPS
- **Limitations**: Commercial use not allowed

**Pro ($20/month)**:
- Unlimited deployments
- 1 TB bandwidth/month
- Password protection
- Analytics
- Team collaboration
- **Best for**: Small teams, production apps

**Enterprise (Custom)**:
- Custom bandwidth
- SLA guarantees
- Advanced security
- Dedicated support
- **Best for**: Large healthcare organizations

### Estimated Costs (Pro Plan)

**Assumptions**:
- 1,000 active users/month
- 100 page views per user = 100,000 views/month
- Average page size: 2 MB
- Total bandwidth: 200 GB/month

**Monthly Costs**:
- Vercel Pro: $20
- Additional bandwidth: $0 (under 1 TB limit)
- **Total: $20/month**

**Plus Backend Costs** (see [BACKEND_DEPLOYMENT_OPTIONS.md](#)):
- AWS/Railway/Render: $50-200/month
- Database: $20-50/month
- **Total System Cost: $90-270/month**

---

## Backend Deployment Options

### Option 1: Railway (Recommended for Quick Start)

**Pros**:
- Simple setup
- Git integration
- Automatic HTTPS
- PostgreSQL included
- $5/month starter plan

**Steps**:
1. Sign up at [railway.app](https://railway.app)
2. Click "New Project" → "Deploy from GitHub"
3. Select backend repository
4. Railway auto-detects Docker configuration
5. Add PostgreSQL service
6. Deploy

**Configuration**:
```bash
# Railway will set these automatically:
DATABASE_URL=postgresql://...
SPRING_PROFILES_ACTIVE=docker
```

### Option 2: AWS ECS (Production-Grade)

**Pros**:
- Highly scalable
- Full control
- HIPAA compliant options
- Integrated with AWS services

**Cons**:
- More complex setup
- Higher cost ($100-500/month)

**Steps**:
1. Create ECS cluster
2. Build Docker images → Push to ECR
3. Create task definitions
4. Create RDS PostgreSQL instance
5. Deploy services
6. Configure load balancer
7. Set up Route 53 for DNS

### Option 3: Render (Middle Ground)

**Pros**:
- Easier than AWS
- More features than Railway
- Automatic HTTPS
- $7/month starter

**Steps**:
1. Sign up at [render.com](https://render.com)
2. Connect GitHub repository
3. Create Web Service for each backend service
4. Create PostgreSQL database
5. Deploy

---

## Next Steps

### Immediate (This Week)
1. [ ] Deploy backend services to Railway/Render/AWS
2. [ ] Note backend API URLs
3. [ ] Deploy frontend to Vercel (preview)
4. [ ] Test end-to-end connectivity
5. [ ] Configure custom domain (optional)

### Short-Term (Next 2 Weeks)
1. [ ] Set up staging environment
2. [ ] Configure CI/CD pipeline
3. [ ] Enable monitoring and analytics
4. [ ] Load testing
5. [ ] Security audit

### Long-Term (Next Month)
1. [ ] Production deployment
2. [ ] User acceptance testing
3. [ ] Performance optimization
4. [ ] Documentation for operations team
5. [ ] Disaster recovery plan

---

## Support Resources

### Vercel Documentation
- Getting Started: https://vercel.com/docs
- Build Configuration: https://vercel.com/docs/build-step
- Environment Variables: https://vercel.com/docs/environment-variables
- Custom Domains: https://vercel.com/docs/custom-domains

### Community Support
- Vercel Discord: https://vercel.com/discord
- GitHub Discussions: https://github.com/vercel/vercel/discussions
- Stack Overflow: Tag `vercel`

### Contact
- Vercel Support: support@vercel.com (Pro/Enterprise only)
- Status Page: https://www.vercel-status.com

---

## Appendix: Complete Deployment Commands

### First-Time Setup
```bash
# 1. Install Vercel CLI
npm install -g vercel

# 2. Login to Vercel
vercel login

# 3. Build locally to verify
npx nx build clinical-portal --configuration=production

# 4. Deploy to preview
vercel

# 5. Set production environment variables
vercel env add API_GATEWAY_URL production
vercel env add QUALITY_MEASURE_URL production
vercel env add CQL_ENGINE_URL production
vercel env add FHIR_SERVER_URL production

# 6. Deploy to production
vercel --prod
```

### Subsequent Deployments (via Git)
```bash
# Just push to GitHub
git add .
git commit -m "Update: Feature XYZ"
git push origin main

# Vercel automatically deploys
```

### Quick Rollback
```bash
# List deployments
vercel ls

# Rollback to previous
vercel rollback
```

---

**Deployment Checklist Complete** ✅

Your HealthData In Motion Clinical Portal is now ready for production deployment on Vercel!

**Next Action**: Run `vercel` command to deploy your first preview.

---

**Document Version**: 1.0
**Last Updated**: November 20, 2025
**Maintained By**: DevOps Team
