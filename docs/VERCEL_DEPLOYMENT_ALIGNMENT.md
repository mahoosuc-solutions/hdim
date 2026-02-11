# HDIM Vercel Deployment Alignment Guide

**Created:** February 2, 2026
**Purpose:** Document all Vercel deployments, their purposes, and alignment strategy

---

## Executive Summary

HDIM has **4 active Vercel projects** serving different purposes. This document maps each deployment to its use case and provides recommendations for alignment.

| Project | Purpose | Primary URL | Status |
|---------|---------|-------------|--------|
| `hdim-master` | Clinical Portal (Angular) | hdim-master.vercel.app | Active |
| `hdim-landing-page` | Marketing Landing Page | hdim-landing-page.vercel.app | Active |
| `technical-product-site` | Technical Documentation Site | technical-product-site.vercel.app | Active |
| `vercel-deploy` | B2B Campaign Pages | (campaign-specific) | Active |

---

## Deployment Inventory

### 1. hdim-master (Clinical Portal)

**Location:** `/` (repository root)
**Project ID:** `prj_hMCcxnnuSnzYJ1BmAs2vdEY8kc4E`
**Framework:** Angular 17+ (custom build)

**Purpose:**
- Main clinical portal application
- Patient data visualization
- Care gap management
- Quality measure dashboards
- HIPAA-compliant PHI handling

**Configuration:**
```json
{
  "buildCommand": "npm run build:clinical-portal",
  "outputDirectory": "dist/apps/clinical-portal/browser",
  "framework": null
}
```

**Use Cases:**
- Customer demos (connects to demo backend)
- Development preview deployments
- Production clinical portal (with backend)

**Recommended Domain:**
- `app.healthdatainmotion.com` (production)
- `demo.healthdatainmotion.com` (demo)

---

### 2. hdim-landing-page (Marketing)

**Location:** `/landing-page-v0/`
**Project ID:** `prj_zgvGbQ8KLRILBwwAyz76yNay2PHD`
**Framework:** Next.js

**Purpose:**
- Public marketing website
- Lead generation
- ROI calculator
- Demo request forms
- SEO-optimized content

**Configuration:**
```json
{
  "name": "hdim-landing-page",
  "framework": "nextjs",
  "outputDirectory": ".next",
  "regions": ["iad1"]
}
```

**Key Routes:**
| Route | Purpose |
|-------|---------|
| `/` | Homepage |
| `/demo` | Demo request (redirects to #demo) |
| `/calculator` | ROI calculator (redirects to #calculator) |

**Use Cases:**
- Organic traffic landing
- LinkedIn ad campaigns
- Google Ads landing
- Investor preview

**Recommended Domain:**
- `www.healthdatainmotion.com` (primary)
- `healthdatainmotion.com` (redirect to www)

---

### 3. technical-product-site (Technical Docs)

**Location:** `/hdim-master/technical-product-site/`
**Project ID:** `prj_HgRAIWBR5Iu2w2ATL0dToGp7InTW`
**Framework:** Next.js

**Purpose:**
- Technical product documentation
- A/B testing variant pages
- GA4 analytics integration
- Detailed feature documentation

**Configuration:**
```json
{
  "framework": "nextjs",
  "env": {
    "NEXT_PUBLIC_SITE_URL": "https://hdim-technical-product-site.vercel.app"
  }
}
```

**Key Features:**
- A/B testing implementation
- GA4 conversion tracking
- Mobile optimization
- Variant testing for marketing messages

**Use Cases:**
- Technical buyer evaluation
- Developer documentation
- Integration guides
- API documentation preview

**Recommended Domain:**
- `docs.healthdatainmotion.com` (technical docs)
- `features.healthdatainmotion.com` (feature showcase)

---

### 4. vercel-deploy (B2B Campaigns)

**Location:** `/campaigns/hdim-linkedin-b2b/vercel-deploy/`
**Project ID:** `prj_yZP6X2W2hEocdznDBq1Cqg5Dzck8`
**Framework:** Static HTML

**Purpose:**
- LinkedIn B2B campaign landing pages
- Investor materials (one-pager, pitch deck)
- Holiday campaigns
- Sales enablement materials

**Configuration:**
```json
{
  "rewrites": [
    { "source": "/demo", "destination": "/landing-page-a.html" },
    { "source": "/calculator", "destination": "/landing-page-b.html" },
    { "source": "/investors", "destination": "/one-pager.html" },
    { "source": "/deck", "destination": "/pitch-deck.html" },
    { "source": "/holiday", "destination": "/holiday-card.html" },
    { "source": "/gallery", "destination": "/gallery.html" }
  ]
}
```

**Key Routes:**
| Route | Page | Use Case |
|-------|------|----------|
| `/demo` | landing-page-a.html | LinkedIn ad variant A |
| `/calculator` | landing-page-b.html | LinkedIn ad variant B |
| `/investors` | one-pager.html | Investor outreach |
| `/deck` | pitch-deck.html | Pitch deck viewer |
| `/holiday` | holiday-card.html | Seasonal campaign |
| `/gallery` | gallery.html | Product screenshots |

**Use Cases:**
- LinkedIn InMail campaigns
- Email campaign landing pages
- Investor link sharing
- Sales team quick links

**Recommended Domain:**
- `go.healthdatainmotion.com` (short links)
- Or use Vercel preview URLs for tracking

---

### 5. docs/marketing/web (AI Solutioning)

**Location:** `/docs/marketing/web/`
**Framework:** Static HTML
**Status:** Nested within main repo (no separate Vercel project)

**Purpose:**
- AI solutioning methodology documentation
- Platform architecture visualization
- Vision deck for investors
- Technical comparison pages

**Key Files:**
| File | Purpose |
|------|---------|
| `ai-solutioning-index.html` | Main AI methodology page |
| `platform-architecture.html` | Architecture diagrams |
| `vision-deck.html` | Investor vision presentation |
| `executive-summary.html` | Executive overview |
| `cms-vision.html` | CMS integration vision |

**Recommendation:**
Deploy as part of `technical-product-site` or create dedicated subdomain.

---

## Duplicate/Nested Configurations (Cleanup Needed)

The following are **duplicate configurations** in nested directories that should be cleaned up:

| Path | Issue | Recommendation |
|------|-------|----------------|
| `/hdim-master/vercel.json` | Duplicate of root | Delete or merge |
| `/hdim-master/landing-page-v0/` | Duplicate of `/landing-page-v0/` | Delete |
| `/hdim-master/campaigns/` | Duplicate of `/campaigns/` | Delete |
| `/hdim-backend-tests/*/vercel.json` | Test artifacts | Delete |
| `/hdim-admin-portal/*/vercel.json` | Nested duplicates | Delete |
| `/hdim-predictive-analytics/*/vercel.json` | Nested duplicates | Delete |

**Root Cause:** Repository was cloned/copied into itself at some point, creating nested `hdim-master/` directory with duplicate configs.

---

## Recommended Domain Structure

### Production Domains

| Domain | Vercel Project | Purpose |
|--------|---------------|---------|
| `healthdatainmotion.com` | hdim-landing-page | Redirect to www |
| `www.healthdatainmotion.com` | hdim-landing-page | Marketing site |
| `app.healthdatainmotion.com` | hdim-master | Clinical portal |
| `docs.healthdatainmotion.com` | technical-product-site | Technical docs |
| `go.healthdatainmotion.com` | vercel-deploy | Campaign links |

### Development/Preview

| Domain Pattern | Purpose |
|----------------|---------|
| `*.vercel.app` | Preview deployments |
| `preview-*.healthdatainmotion.com` | Staging previews |

---

## Deployment Use Cases by Audience

### For Customers (Health Systems)

| Audience Need | Deployment | URL |
|---------------|------------|-----|
| Learn about product | hdim-landing-page | www.healthdatainmotion.com |
| Request demo | hdim-landing-page | www.healthdatainmotion.com/demo |
| See ROI | hdim-landing-page | www.healthdatainmotion.com/calculator |
| Technical evaluation | technical-product-site | docs.healthdatainmotion.com |
| Access portal | hdim-master | app.healthdatainmotion.com |

### For Investors

| Audience Need | Deployment | URL |
|---------------|------------|-----|
| Quick overview | vercel-deploy | go.healthdatainmotion.com/investors |
| Full pitch deck | vercel-deploy | go.healthdatainmotion.com/deck |
| Product demo | hdim-master | demo.healthdatainmotion.com |
| Technical depth | technical-product-site | docs.healthdatainmotion.com |

### For LinkedIn Campaigns

| Campaign Type | Deployment | URL |
|---------------|------------|-----|
| Demo variant A | vercel-deploy | go.healthdatainmotion.com/demo |
| Calculator variant B | vercel-deploy | go.healthdatainmotion.com/calculator |
| General landing | hdim-landing-page | www.healthdatainmotion.com |

---

## Action Items

### Immediate (This Week)

- [ ] **Configure custom domains** in Vercel dashboard
- [ ] **Set up DNS records** for healthdatainmotion.com
- [ ] **Delete duplicate nested configs** in `/hdim-master/` subdirectory
- [ ] **Update investor materials** to use go.healthdatainmotion.com links

### Short-term (This Month)

- [ ] **Consolidate AI solutioning pages** into technical-product-site
- [ ] **Set up redirects** from old Vercel URLs to custom domains
- [ ] **Configure SSL certificates** for all custom domains
- [ ] **Set up analytics** (GA4) across all deployments

### Long-term

- [ ] **Implement CDN caching** for static assets
- [ ] **Set up monitoring** for all deployments
- [ ] **Create deployment pipeline** for automated promotions
- [ ] **Document deployment procedures** for team

---

## Vercel CLI Quick Reference

```bash
# List all projects
vercel project ls

# List deployments for current project
vercel ls

# Deploy to production
vercel --prod

# Deploy with specific project
vercel --prod --scope=team_np0XIw160vaTgG18AaYpi6XT

# Link local directory to project
vercel link

# Check deployment logs
vercel logs <deployment-url>

# Add custom domain
vercel domains add healthdatainmotion.com

# Set environment variable
vercel env add VARIABLE_NAME
```

---

## Environment Variables by Project

### hdim-master (Clinical Portal)
```
NODE_ENV=production
API_URL=https://api.healthdatainmotion.com
```

### hdim-landing-page
```
NODE_ENV=production
NEXT_PUBLIC_GA_ID=G-XXXXXXXXXX
NEXT_PUBLIC_CALENDLY_URL=https://calendly.com/hdim
```

### technical-product-site
```
NEXT_PUBLIC_SITE_URL=https://docs.healthdatainmotion.com
NEXT_PUBLIC_GA_ID=G-XXXXXXXXXX
```

### vercel-deploy (campaigns)
```
(No environment variables - static HTML)
```

---

## Monitoring & Analytics

### Recommended Setup

| Deployment | Analytics | Monitoring |
|------------|-----------|------------|
| hdim-master | GA4 + Vercel Analytics | Vercel Speed Insights |
| hdim-landing-page | GA4 + conversion tracking | Vercel Analytics |
| technical-product-site | GA4 + A/B testing | Vercel Analytics |
| vercel-deploy | UTM tracking → GA4 | Basic uptime |

### Key Metrics to Track

| Metric | Deployment | Target |
|--------|------------|--------|
| Page load time | All | < 2s |
| Core Web Vitals | Landing page | All green |
| Conversion rate | Landing page | > 3% |
| Demo requests | Landing page | 10+/week |
| Bounce rate | All | < 50% |

---

*Last updated: February 2, 2026*
