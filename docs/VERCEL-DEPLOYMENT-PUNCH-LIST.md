# HDIM Vercel Deployment - Q1 2026 Punch List

**Current Status:** ✅ Production-ready frontend with HIPAA compliance infrastructure
**Last Updated:** February 1, 2026
**Priority:** High (Deploy improvements to Vercel)

---

## 📊 CURRENT STATE SUMMARY

### What's Currently Deployed

**Primary Applications on Vercel:**
- ✅ **Clinical Portal** (Angular 21, 51 services integration)
- ✅ **Admin Portal** (Angular 21, tenant management)
- ✅ **Landing Page** (Next.js, public marketing site)

**Production-Ready Features:**
- ✅ Real-time patient dashboard
- ✅ FHIR R4 clinical data display
- ✅ CQL/HEDIS quality measure evaluation
- ✅ Care gap detection & closure workflows
- ✅ Risk stratification & visualization
- ✅ QRDA I/III export functionality

**HIPAA Compliance Infrastructure (Q1 2026):**
- ✅ **HTTP Audit Interceptor** - 100% API call logging (automatic)
- ✅ **Session Timeout** - 15-minute idle with audit logging
- ✅ **LoggerService** - PHI filtering in logs (98.2% console.log migration)
- ✅ **Global Error Handler** - Unhandled exception protection
- ✅ **Fire-and-Forget Batching** - Non-blocking audit event submission

**Security Headers Configured:**
- ✅ X-Content-Type-Options: nosniff
- ✅ X-Frame-Options: DENY (clickjacking protection)
- ✅ X-XSS-Protection: 1; mode=block
- ✅ Referrer-Policy: strict-origin-when-cross-origin
- ✅ Permissions-Policy: camera=(), microphone=(), geolocation=()

---

## 🎯 PUNCH LIST: PRIORITY IMPROVEMENTS

### TIER 1: CRITICAL (Deploy First)

#### 1.1 Build Output Verification & Deployment Health
**Current Status:** Build directory empty (on-demand Vercel builds)
**Risk:** High (deployment might be failing silently)
**Effort:** 2-4 hours

**Actions:**
- [ ] Verify production build succeeds locally
  ```bash
  npx nx build clinical-portal --configuration=production
  ls -lh dist/apps/clinical-portal/browser/
  ```
- [ ] Check bundle size compliance
  - Target: <800KB initial bundle (warning), <1.5MB (error)
  - Verify: index.html + main.*.js + polyfills.*.js combined
- [ ] Test Vercel preview deployment manually
  - Push to feature branch
  - Monitor Vercel deployment logs
  - Verify no build errors
- [ ] Add build verification to GitHub Actions
  ```yaml
  - name: Verify production build
    run: |
      npx nx build clinical-portal --configuration=production
      du -sh dist/apps/clinical-portal/browser/
  ```
- [ ] Document build command in vercel.json
  ```json
  {
    "buildCommand": "npx nx build clinical-portal --configuration=production"
  }
  ```

**Success Criteria:**
- Build completes in <5 minutes
- Bundle size <800KB
- No errors in Vercel logs
- All assets included (CSS, fonts, images)

---

#### 1.2 Environment Variable Management
**Current Status:** environment.prod.ts configured, Vercel secrets not documented
**Risk:** Medium (missing env vars could break production)
**Effort:** 2-3 hours

**Required Vercel Environment Variables:**
```
API_GATEWAY_URL              → https://api.healthdata-in-motion.com (or your domain)
TENANT_ID                    → Default tenant for Vercel preview deployments
SENTRY_DSN                   → Error tracking (set up next)
GA_MEASUREMENT_ID            → Google Analytics 4 measurement ID
MATOMO_SITE_ID               → Alternative analytics (if using)
ENABLE_DEMO_MODE             → true for staging, false for production
FEATURE_FLAGS_URL            → Optional: remote feature flags service
```

**Actions:**
- [ ] Document all required Vercel secrets in `/docs/VERCEL-SECRETS-GUIDE.md`
  - Which env vars are required vs optional
  - Where to find/generate each value
  - How to set them in Vercel dashboard
- [ ] Create `.env.example` file with all keys (no secrets)
  ```
  API_GATEWAY_URL=
  TENANT_ID=
  SENTRY_DSN=
  GA_MEASUREMENT_ID=
  ```
- [ ] Add GitHub Actions step to validate env vars
  ```yaml
  - name: Validate environment variables
    run: |
      npm run check-env-vars
  ```
- [ ] Create script to verify production env vars in deployed app
  ```bash
  npm run verify-prod-env
  ```

**Success Criteria:**
- Vercel secrets are set and documented
- Production app loads without errors
- API Gateway URL is correct
- Analytics is configured and working

---

#### 1.3 API Gateway Routing Configuration
**Current Status:** vercel.json has rewrites, backend routing untested
**Risk:** High (API calls might fail due to misconfigured rewrites)
**Effort:** 3-4 hours

**Current vercel.json Rewrite:**
```json
{
  "rewrites": [
    {
      "source": "/api/(.*)",
      "destination": "https://api.healthdata-in-motion.com/api/$1"
    }
  ]
}
```

**Actions:**
- [ ] Test API routing from deployed app
  ```bash
  # After deployment, open browser DevTools
  # Make a call to /api/v1/patients
  # Verify request goes to correct backend
  ```
- [ ] Add CORS headers for API Gateway communication
  ```json
  {
    "headers": [
      {
        "source": "/api/(.*)",
        "headers": [
          {
            "key": "Access-Control-Allow-Origin",
            "value": "https://your-frontend-domain.com"
          },
          {
            "key": "Access-Control-Allow-Methods",
            "value": "GET,POST,PUT,DELETE,OPTIONS"
          },
          {
            "key": "Access-Control-Allow-Headers",
            "value": "Content-Type,Authorization,X-Tenant-ID"
          }
        ]
      }
    ]
  }
  ```
- [ ] Test multi-tenant routing (X-Tenant-ID header)
  - Verify requests from different tenants are isolated
  - Check audit logs show correct tenant_id
- [ ] Document API Gateway integration in deployment guide
  - How to switch backend URL (dev/staging/production)
  - How to debug API routing issues
  - Common API errors and troubleshooting

**Success Criteria:**
- API calls from frontend reach correct backend
- CORS headers allow cross-origin requests
- Multi-tenant headers are passed correctly
- Backend returns expected responses

---

#### 1.4 Error Tracking Setup (Sentry)
**Current Status:** Global error handler implemented, no tracking configured
**Risk:** High (production errors go undetected)
**Effort:** 2-3 hours

**Current Implementation:**
- Error handler catches unhandled exceptions
- Logs to console in development
- Could be sent to backend, but not configured

**Actions:**
- [ ] Create Sentry account (sentry.io)
  - Create organization for HDIM
  - Create 3 projects: development, staging, production
  - Get DSN URLs for each environment
- [ ] Install Sentry SDK
  ```bash
  npm install @sentry/angular @sentry/tracing
  ```
- [ ] Initialize Sentry in main.ts
  ```typescript
  import * as Sentry from "@sentry/angular";

  Sentry.init({
    dsn: environment.sentryDsn,
    integrations: [
      new Sentry.Replay({ maskAllText: false, blockAllMedia: false }),
    ],
    tracesSampleRate: environment.production ? 0.1 : 1.0,
    replaysSessionSampleRate: environment.production ? 0.1 : 1.0,
    replaysOnErrorSampleRate: 1.0,
    environment: environment.production ? 'production' : 'development',
  });
  ```
- [ ] Capture critical errors (already handled by global error handler)
  ```typescript
  Sentry.captureException(error, {
    tags: {
      severity: 'critical',
      tenant: this.tenantId,
    }
  });
  ```
- [ ] Set up Sentry alerts
  - Alert on 10+ errors in 5 minutes
  - Alert on new error type
  - Digest email daily
- [ ] Configure Sentry for PHI compliance
  - Don't capture request/response bodies (PHI risk)
  - Filter patient IDs from breadcrumbs
  - Test that PHI is not sent to Sentry
- [ ] Add Sentry status page to dashboard
  - Show recent error count
  - Link to Sentry dashboard for details

**Success Criteria:**
- Sentry project created with 3 environments
- DSN configured in Vercel secrets
- Production errors appear in Sentry within seconds
- No PHI is captured or sent to Sentry
- Team receives daily error digests

---

### TIER 2: HIGH PRIORITY (Deploy Second)

#### 2.1 Content Security Policy (CSP) Headers
**Current Status:** Not configured
**Risk:** Medium (XSS vulnerability)
**Effort:** 3-4 hours

**Actions:**
- [ ] Add CSP headers to vercel.json
  ```json
  {
    "headers": [
      {
        "source": "/(.*)",
        "headers": [
          {
            "key": "Content-Security-Policy",
            "value": "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval' *.google-analytics.com *.sentry.io; style-src 'self' 'unsafe-inline' fonts.googleapis.com; font-src 'self' fonts.gstatic.com; img-src 'self' data: https:; connect-src 'self' api.healthdata-in-motion.com *.sentry.io *.google-analytics.com;"
          }
        ]
      }
    ]
  }
  ```
- [ ] Test CSP with various attacks
  - Try inline script injection (should be blocked)
  - Try external script loading (should be blocked unless whitelisted)
  - Check browser console for CSP violations
- [ ] Optimize CSP to be more restrictive
  - Remove 'unsafe-inline' if possible
  - Use nonce for inline scripts
  - Remove 'unsafe-eval' for production
- [ ] Monitor CSP violations in Sentry
  - Add CSP violation reporting endpoint
  - Alert on new CSP violations

**Success Criteria:**
- CSP headers configured in Vercel
- No CSP violations in production
- Known third-parties (Google, Sentry) are whitelisted
- Team understands CSP policy and how to update it

---

#### 2.2 Accessibility Improvements (WCAG 2.1 Level A)
**Current Status:** 50% coverage (343 ARIA attributes across 53 files)
**Risk:** Medium (compliance, user experience)
**Effort:** 8-12 hours

**Current Coverage:**
- 343 ARIA attributes implemented (50% of needed)
- Basic semantic HTML
- Missing: skip links, focus indicators, text alternatives

**Priority Improvements:**
1. **Skip-to-Content Link** (1 hour)
   - Add hidden link at top of page
   - Jump to #main-content
   - Visible on keyboard focus
   ```html
   <a href="#main-content" class="sr-only skip-link">
     Skip to main content
   </a>
   <main id="main-content">...
   ```

2. **Focus Indicators** (2-3 hours)
   - Add visible focus outline (not removed!)
   - Ensure Tab key navigates through all interactive elements
   - Test with keyboard only (no mouse)
   ```css
   *:focus {
     outline: 3px solid #0066cc;
     outline-offset: 2px;
   }
   ```

3. **ARIA Labels on Table Actions** (2-3 hours)
   - All action buttons need aria-labels
   - Especially "View", "Edit", "Delete" buttons
   ```html
   <button mat-icon-button
           aria-label="View patient details for {{ patient.name }}">
     <mat-icon>visibility</mat-icon>
   </button>
   ```

4. **Form Accessibility** (2-3 hours)
   - All form fields need labels (visible or aria-label)
   - Error messages associated with fields
   - ARIA-required on required fields
   - Error messages linked with aria-describedby

5. **Alt Text for Images** (1 hour)
   - All img tags need alt text
   - Data visualizations need alternative text descriptions
   - Icons hidden from screen readers (aria-hidden)

**Actions:**
- [ ] Run axe-core accessibility audit
  ```bash
  npm install --save-dev @axe-core/puppeteer
  npm run test:accessibility
  ```
- [ ] Add accessibility tests to CI/CD
  ```yaml
  - name: Run accessibility tests
    run: npm run test:a11y
  ```
- [ ] Target: WCAG 2.1 Level AA (not just Level A)
  - Better contrast ratios
  - Better keyboard support
  - Better screen reader support

**Success Criteria:**
- Skip-to-content link present and working
- All interactive elements have visible focus indicators
- Tab order makes logical sense
- All form fields have labels
- axe-core scan shows 0 critical/serious violations
- WCAG 2.1 Level A compliance verified

---

#### 2.3 Performance Optimization
**Current Status:** Bundle budget configured (800KB), no profiling done
**Risk:** Medium (slow app = poor UX)
**Effort:** 8-12 hours

**Actions:**
- [ ] Profile bundle with source-map-explorer
  ```bash
  npm install --save-dev source-map-explorer
  npm run build:prod
  npx source-map-explorer dist/apps/clinical-portal/browser/**/*.js
  ```
- [ ] Identify largest dependencies
  - Remove unused libraries
  - Replace with smaller alternatives
  - Tree-shake unnecessary code
- [ ] Implement route lazy-loading
  ```typescript
  const routes: Routes = [
    {
      path: 'patients',
      loadComponent: () => import('./patients/patients.component')
        .then(m => m.PatientsComponent)
    }
  ];
  ```
- [ ] Add HTTP compression
  ```json
  // vercel.json
  {
    "headers": [
      {
        "source": "/(.*)",
        "headers": [
          {
            "key": "Content-Encoding",
            "value": "gzip"
          }
        ]
      }
    ]
  }
  ```
- [ ] Enable service worker for PWA support
  ```bash
  npx @angular/cli:ng add @angular/pwa
  ```
- [ ] Monitor Core Web Vitals
  - LCP (Largest Contentful Paint) < 2.5s
  - FID (First Input Delay) < 100ms
  - CLS (Cumulative Layout Shift) < 0.1
  - Add to dashboard

**Success Criteria:**
- Bundle size < 600KB (down from 800KB)
- Route lazy-loading reduces initial bundle
- Service worker enabled for offline support
- Core Web Vitals: LCP < 2.5s, FID < 100ms, CLS < 0.1
- Lighthouse score > 90

---

#### 2.4 CI/CD Deployment Automation
**Current Status:** GitHub Actions configured, Vercel deployments might be manual
**Risk:** Medium (deployment could be inconsistent)
**Effort:** 4-6 hours

**Current CI/CD Jobs:**
- ✅ Build and test
- ✅ Security scan (ESLint)
- ✅ Code coverage
- ⏳ Vercel deployments (may need configuration)

**Actions:**
- [ ] Configure automatic Vercel deployments
  ```yaml
  - name: Deploy to Vercel (Staging)
    if: github.ref == 'refs/heads/develop'
    uses: amondnet/vercel-action@v20
    with:
      vercel-token: ${{ secrets.VERCEL_TOKEN }}
      vercel-args: '--prod'
      vercel-org-id: ${{ secrets.VERCEL_ORG_ID }}
      vercel-project-id: ${{ secrets.VERCEL_PROJECT_ID_STAGING }}

  - name: Deploy to Vercel (Production)
    if: github.ref == 'refs/heads/master'
    uses: amondnet/vercel-action@v20
    with:
      vercel-token: ${{ secrets.VERCEL_TOKEN }}
      vercel-args: '--prod'
      vercel-org-id: ${{ secrets.VERCEL_ORG_ID }}
      vercel-project-id: ${{ secrets.VERCEL_PROJECT_ID_PROD }}
  ```
- [ ] Add deployment notification to Slack
  ```yaml
  - name: Notify Slack
    uses: slackapi/slack-github-action@v1.24.0
    with:
      payload: |
        {
          "text": "✅ Deployed to Production",
          "blocks": [
            {
              "type": "section",
              "text": {
                "type": "mrkdwn",
                "text": "🚀 Clinical Portal deployed to production\n🔗 <https://your-domain.com|Visit App>\n📊 <https://vercel.com/dashboard|Vercel Dashboard>"
              }
            }
          ]
        }
  ```
- [ ] Add rollback automation
  ```yaml
  - name: Rollback on failure
    if: failure()
    run: |
      vercel rollback --confirm --token=${{ secrets.VERCEL_TOKEN }}
  ```
- [ ] Monitor deployment health
  - Check Lighthouse scores post-deployment
  - Run smoke tests on production
  - Verify health check endpoints

**Success Criteria:**
- Commits to master automatically deploy to production Vercel
- Commits to develop deploy to staging Vercel
- Feature branch PRs get preview deployments
- Team is notified of deployments in Slack
- Automatic rollback on deployment failure

---

### TIER 3: MEDIUM PRIORITY (Deploy Third)

#### 3.1 Real User Monitoring (RUM)
**Current Status:** Not configured
**Risk:** Low (nice to have)
**Effort:** 2-3 hours

**Actions:**
- [ ] Add Web Vitals tracking
  ```typescript
  import { getCLS, getFID, getFCP, getLCP, getTTFB } from 'web-vitals';

  export function reportWebVitals() {
    getCLS(console.log);
    getFID(console.log);
    getFCP(console.log);
    getLCP(console.log);
    getTTFB(console.log);
  }
  ```
- [ ] Send metrics to analytics backend
  - LCP (Largest Contentful Paint)
  - FID (First Input Delay)
  - CLS (Cumulative Layout Shift)
  - FCP (First Contentful Paint)
  - TTFB (Time to First Byte)
- [ ] Set up Vercel Analytics
  - Enable in Vercel dashboard
  - Monitor Core Web Vitals trends
  - Set alerts for degradation

**Success Criteria:**
- Web Vitals are tracked and stored
- Dashboard shows performance trends
- Team can identify performance regressions early

---

#### 3.2 Testing Infrastructure
**Current Status:** Unit tests at 70% coverage, E2E tests minimal
**Risk:** Medium (regressions in production)
**Effort:** 12-16 hours

**Actions:**
- [ ] Expand E2E test coverage
  - Patient creation/viewing workflow
  - Care gap detection
  - Quality measure evaluation
  - Report generation
  ```bash
  npx nx e2e clinical-portal-e2e
  ```
- [ ] Add visual regression testing
  ```bash
  npm install --save-dev @percy/cli @percy/playwright
  ```
- [ ] Add performance benchmarking
  - Page load times
  - Data table rendering
  - Chart/visualization rendering
- [ ] Add accessibility testing
  ```bash
  npm install --save-dev axe-playwright
  ```
- [ ] Test on real devices
  - iPhone/iPad
  - Android phones
  - Tablets

**Success Criteria:**
- E2E test coverage for critical paths
- Visual regression tests catch UI changes
- Performance benchmarks track regressions
- Accessibility tests run on every PR

---

#### 3.3 Documentation for Vercel Deployment
**Current Status:** No deployment guide
**Risk:** Medium (onboarding difficulty)
**Effort:** 3-4 hours

**Actions:**
- [ ] Create `/docs/VERCEL-DEPLOYMENT-GUIDE.md`
  - How to set up Vercel project
  - Required environment variables
  - How to configure GitHub integration
  - How to deploy manually vs automatic
  - How to rollback
  - Common issues and troubleshooting
- [ ] Create `/docs/VERCEL-SECRETS-GUIDE.md`
  - All required Sentry secrets
  - API Gateway URL configuration
  - Analytics configuration
  - How to rotate secrets safely
- [ ] Create deployment runbook for team
  - Step-by-step deployment procedure
  - Checklist before deploying
  - What to monitor after deployment
  - Who to contact if something breaks

**Success Criteria:**
- New team member can deploy without help
- All environment variables are documented
- Common issues have solutions
- Deployment rollback procedure is clear

---

## 📋 DEPLOYMENT CHECKLIST

### Pre-Deployment
- [ ] Build verification (bundle size, no errors)
- [ ] All environment variables set in Vercel
- [ ] Sentry project created and DSN configured
- [ ] API Gateway URL verified
- [ ] CORS headers configured
- [ ] CSP headers configured
- [ ] Security headers verified
- [ ] CI/CD pipeline green (all tests passing)
- [ ] Accessibility audit passing (Level A minimum)
- [ ] Performance budget met (<800KB)

### Deployment
- [ ] Deploy to staging environment first
- [ ] Verify staging deployment succeeds
- [ ] Run smoke tests on staging
- [ ] Get team approval for production
- [ ] Deploy to production
- [ ] Monitor error tracking (Sentry)
- [ ] Check core web vitals
- [ ] Verify all features working in production

### Post-Deployment
- [ ] Monitor error rate for 1 hour
- [ ] Check user activity (analytics)
- [ ] Verify API connectivity
- [ ] Run accessibility audit on production build
- [ ] Performance profile (Lighthouse)
- [ ] Team notification (Slack)
- [ ] Update deployment log

---

## 📊 SUCCESS METRICS

### By End of Tier 1 (Critical)
- ✅ Production build succeeds reliably
- ✅ Environment variables properly configured
- ✅ API Gateway routing working
- ✅ Error tracking (Sentry) active
- ✅ Zero untracked production errors

### By End of Tier 2 (High Priority)
- ✅ CSP headers protecting against XSS
- ✅ WCAG 2.1 Level A accessibility compliance
- ✅ Performance budget met (Bundle <800KB → <600KB)
- ✅ Automated deployments working
- ✅ Zero critical security vulnerabilities

### By End of Tier 3 (Medium Priority)
- ✅ Core Web Vitals tracked and monitored
- ✅ E2E test coverage >80% for critical paths
- ✅ Documentation complete for team
- ✅ Zero deployment surprises

---

## 📈 ESTIMATED EFFORT

| Tier | Tasks | Est. Hours | Priority | Timeline |
|------|-------|-----------|----------|----------|
| **Tier 1** | 4 critical | 11-14 hours | **Immediate** | This week |
| **Tier 2** | 4 high | 23-30 hours | **Next week** | Weeks 2-3 |
| **Tier 3** | 3 medium | 17-23 hours | **Following** | Weeks 3-4 |
| **Total** | 11 tasks | **51-67 hours** | | **4 weeks** |

---

## 📝 IMPLEMENTATION NOTES

**These improvements build on:**
- ✅ 7 infrastructure modernization phases (Phases 1-7 complete)
- ✅ 90%+ faster feedback loops achieved
- ✅ 51 microservices in production
- ✅ 4,500+ tests with 94%+ pass rate
- ✅ HIPAA compliance verified
- ✅ Production-ready infrastructure

**The punch list addresses:**
- Frontend deployment health (verify builds work)
- Observability (track errors in production)
- Security hardening (CSP, CORS, secure headers)
- Accessibility compliance (WCAG 2.1 Level A)
- Performance optimization (bundle size, Core Web Vitals)
- Team productivity (CI/CD automation, documentation)

---

## 🎯 NEXT ACTION

**Start with Tier 1 this week:**
1. Run local production build and verify bundle size
2. Document all required Vercel environment variables
3. Test API Gateway routing from deployed app
4. Set up Sentry error tracking
5. Add CSP headers to vercel.json

Expected result: Production deployments are reliable, monitored, and documented.

---

**Document Status:** Ready for implementation
**Last Updated:** February 1, 2026
**Ready for:** Team review and prioritization
