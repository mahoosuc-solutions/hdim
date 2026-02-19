# Landing Page & GTM Validation Plan

> Note: Historical planning snapshot. For current operational commands and workflow paths, use `landing-page-v0/README.md`, `landing-page-v0/tests/README.md`, and `.github/workflows/landing-page-validation.yml`.

## Executive Summary

This plan provides a comprehensive, fully-automated validation strategy for the HDIM landing page deployment on Vercel. It covers functionality, content completeness, marketing materials, integrations, SEO, performance, accessibility, and mobile responsiveness.

**Validation Scope**: Complete pre-launch and post-deployment validation
**Platform**: Vercel with Next.js
**Automation Level**: Fully automated with CI/CD integration
**Target Timeline**: 2-3 days for implementation, ongoing monitoring

---

## Phase 1: Pre-Deployment Validation

### 1.1 Content Completeness Audit

**Automated Checks** (using custom validation script):

```bash
# Create content-validator.js
npm install --save-dev glob cheerio markdown-it
```

**Content validation checklist**:
- [ ] All planned pages exist in `/landing-page-v0/app/` or `/pages/`
- [ ] All images have alt text
- [ ] All copy matches brand voice guidelines
- [ ] All CTAs are present and functional
- [ ] Legal pages (Privacy, Terms, Cookie Policy) complete
- [ ] All placeholder text replaced with final copy

**Script**: `scripts/validate-content.js`
```javascript
// Validates:
// - All pages exist
// - No "TODO" or "Lorem ipsum" in content
// - All images have alt attributes
// - All links have href values
// - Required sections present on each page
```

### 1.2 Marketing Materials Validation

**Assets to validate**:

| Asset Type | Location | Validation |
|------------|----------|------------|
| Product videos | `/public/videos/` | File exists, <50MB, proper format |
| Demo screenshots | `/public/images/demo/` | Optimized, WebP format, responsive |
| PDF resources | `/public/downloads/` | Accessible, <5MB, proper metadata |
| Brand assets | `/public/brand/` | SVG/PNG logos, favicon set |
| ROI calculator | Component/page | Fully functional, accurate calculations |

**Automated validation**:
```bash
# scripts/validate-assets.sh
#!/bin/bash

# Check video files
find public/videos -name "*.mp4" -size +50M -exec echo "WARNING: {} exceeds 50MB" \;

# Check images are optimized
find public/images -name "*.jpg" -o -name "*.png" | while read img; do
  size=$(stat -f%z "$img")
  if [ $size -gt 500000 ]; then
    echo "WARNING: $img is ${size} bytes (should be <500KB)"
  fi
done

# Check PDFs exist and are accessible
test -f public/downloads/hdim-product-overview.pdf || echo "ERROR: Product overview PDF missing"
```

### 1.3 Link Validation (Broken Link Check)

**Automated tool**: `broken-link-checker`

```bash
npm install --save-dev broken-link-checker

# Create check-links.js
const blc = require('broken-link-checker');

const siteChecker = new blc.SiteChecker({
  excludeExternalLinks: false,
  filterLevel: 3
}, {
  link: (result) => {
    if (result.broken) {
      console.error(`❌ BROKEN: ${result.url.original} (${result.brokenReason})`);
    }
  },
  end: () => {
    console.log('✅ Link validation complete');
  }
});

siteChecker.enqueue('http://localhost:3000');
```

**Manual verification for**:
- External partner links (integrations, case studies)
- Social media links
- Documentation links
- Support/contact links

### 1.4 Form Validation

**Forms to test**:
1. **Demo request form**
   - [ ] All fields validate correctly
   - [ ] Required fields enforced
   - [ ] Email validation works
   - [ ] Phone number formatting
   - [ ] Submission triggers analytics event
   - [ ] Success message displays
   - [ ] Error handling works

2. **Contact form**
   - [ ] Same validations as demo form
   - [ ] File upload (if applicable) works
   - [ ] CAPTCHA/bot protection active

3. **Newsletter signup**
   - [ ] Email validation
   - [ ] Double opt-in confirmation sent
   - [ ] GDPR consent checkbox

**Automated test** (Playwright):
```javascript
// tests/forms.spec.ts
import { test, expect } from '@playwright/test';

test('demo request form submits successfully', async ({ page }) => {
  await page.goto('/request-demo');

  await page.fill('[name="firstName"]', 'John');
  await page.fill('[name="lastName"]', 'Doe');
  await page.fill('[name="email"]', 'john.doe@example.com');
  await page.fill('[name="company"]', 'ACME Health');
  await page.fill('[name="phone"]', '555-123-4567');

  await page.click('button[type="submit"]');

  await expect(page.locator('.success-message')).toBeVisible();
});

test('form validates required fields', async ({ page }) => {
  await page.goto('/request-demo');
  await page.click('button[type="submit"]');

  await expect(page.locator('.error-message')).toBeVisible();
});
```

---

## Phase 2: Integration Testing

### 2.1 Analytics Validation

**Tools to integrate**:
- Google Analytics 4
- Google Tag Manager
- Segment (optional)
- HubSpot/Zoho tracking

**Validation checklist**:
- [ ] GA4 tracking ID configured
- [ ] Page view events firing
- [ ] Custom events tracked:
  - [ ] Demo request submitted
  - [ ] Video played
  - [ ] PDF downloaded
  - [ ] ROI calculator used
  - [ ] Pricing viewed
  - [ ] Contact clicked
- [ ] UTM parameters captured
- [ ] Conversion goals set up
- [ ] Real-time reports working

**Automated validation** (using `@analytics/google-analytics`):
```javascript
// __tests__/analytics.test.js
test('analytics fires page view on navigation', async () => {
  const spy = jest.spyOn(window.gtag, 'event');

  render(<HomePage />);

  expect(spy).toHaveBeenCalledWith('page_view', expect.any(Object));
});
```

### 2.2 CRM Integration

**Integration**: Zoho CRM / HubSpot

**Test cases**:
- [ ] Form submission creates lead in CRM
- [ ] Lead data maps correctly:
  - Name, email, phone
  - Company, title
  - Source/campaign tracking
  - Custom fields populated
- [ ] Duplicate detection works
- [ ] Lead scoring triggered
- [ ] Workflow automation fires
- [ ] Notification emails sent

**Validation script**:
```javascript
// scripts/test-crm-integration.js
const axios = require('axios');

async function testCRMIntegration() {
  const testLead = {
    firstName: 'Test',
    lastName: 'User',
    email: `test+${Date.now()}@example.com`,
    company: 'Test Company',
    source: 'landing_page_test'
  };

  // Submit via API
  const response = await axios.post('/api/submit-demo-request', testLead);

  console.log('✅ Form submission successful');

  // Wait 5 seconds for CRM sync
  await new Promise(resolve => setTimeout(resolve, 5000));

  // Check CRM API for lead creation
  // (Implementation depends on CRM)

  console.log('✅ CRM integration validated');
}
```

### 2.3 Email Integration

**Email service**: SendGrid / AWS SES / Zoho Mail

**Emails to test**:
1. **Demo request confirmation** (to user)
   - [ ] Sends immediately
   - [ ] Correct template/branding
   - [ ] Personalization works
   - [ ] CTA links work
   - [ ] Unsubscribe link present

2. **Demo request notification** (to sales)
   - [ ] Sends to correct team
   - [ ] Contains all form data
   - [ ] Links to CRM record

3. **Newsletter welcome** (if applicable)
   - [ ] Double opt-in confirmation
   - [ ] Welcome email after confirmation

**Automated test**:
```javascript
// Use MailHog or similar for testing
// tests/email.spec.ts
test('demo confirmation email sent', async ({ request }) => {
  await request.post('/api/submit-demo-request', {
    data: { /* test data */ }
  });

  // Check MailHog API for email
  const emails = await request.get('http://localhost:8025/api/v2/messages');
  const json = await emails.json();

  expect(json.items).toHaveLength(1);
  expect(json.items[0].Content.Headers.Subject).toContain('Demo Request Confirmation');
});
```

---

## Phase 3: SEO & Performance Validation

### 3.1 SEO Validation

**Automated tool**: `next-seo` + custom validators

**Checklist per page**:
- [ ] Title tag present (50-60 characters)
- [ ] Meta description present (150-160 characters)
- [ ] Open Graph tags configured
- [ ] Twitter Card tags configured
- [ ] Canonical URL set
- [ ] Structured data (Schema.org):
  - [ ] Organization
  - [ ] WebSite
  - [ ] BreadcrumbList
  - [ ] Product (if applicable)
- [ ] Robots meta tag appropriate
- [ ] XML sitemap generated
- [ ] robots.txt configured

**Validation script**:
```javascript
// scripts/validate-seo.js
const { JSDOM } = require('jsdom');
const fs = require('fs');
const glob = require('glob');

function validateSEO(html, pagePath) {
  const dom = new JSDOM(html);
  const doc = dom.window.document;

  const errors = [];

  // Check title
  const title = doc.querySelector('title');
  if (!title || title.textContent.length < 10 || title.textContent.length > 60) {
    errors.push(`Title issue: ${title?.textContent || 'missing'}`);
  }

  // Check meta description
  const description = doc.querySelector('meta[name="description"]');
  if (!description || description.content.length < 50 || description.content.length > 160) {
    errors.push(`Description issue: ${description?.content || 'missing'}`);
  }

  // Check OG tags
  const ogTitle = doc.querySelector('meta[property="og:title"]');
  const ogDesc = doc.querySelector('meta[property="og:description"]');
  const ogImage = doc.querySelector('meta[property="og:image"]');

  if (!ogTitle) errors.push('Missing og:title');
  if (!ogDesc) errors.push('Missing og:description');
  if (!ogImage) errors.push('Missing og:image');

  // Check structured data
  const jsonLd = doc.querySelector('script[type="application/ld+json"]');
  if (!jsonLd) errors.push('Missing structured data');

  return { page: pagePath, errors };
}

// Run on all built pages
glob('.next/server/pages/**/*.html', (err, files) => {
  files.forEach(file => {
    const html = fs.readFileSync(file, 'utf8');
    const result = validateSEO(html, file);
    if (result.errors.length > 0) {
      console.error(`❌ ${result.page}:`, result.errors);
    } else {
      console.log(`✅ ${result.page}`);
    }
  });
});
```

**Tools to use**:
- Google Search Console (submit sitemap)
- Google Rich Results Test
- Schema.org validator
- SEMrush Site Audit
- Screaming Frog SEO Spider

### 3.2 Performance Validation

**Tool**: Lighthouse CI + Web Vitals

**Targets** (minimum scores):
- Performance: 90+
- Accessibility: 95+
- Best Practices: 95+
- SEO: 100

**Core Web Vitals targets**:
- LCP (Largest Contentful Paint): <2.5s
- FID (First Input Delay): <100ms
- CLS (Cumulative Layout Shift): <0.1

**Lighthouse CI setup**:
```bash
npm install --save-dev @lhci/cli

# lighthouserc.js
module.exports = {
  ci: {
    collect: {
      startServerCommand: 'npm run start',
      url: ['http://localhost:3000/', 'http://localhost:3000/pricing', 'http://localhost:3000/request-demo'],
      numberOfRuns: 3
    },
    assert: {
      preset: 'lighthouse:recommended',
      assertions: {
        'categories:performance': ['error', {minScore: 0.9}],
        'categories:accessibility': ['error', {minScore: 0.95}],
        'categories:seo': ['error', {minScore: 1.0}],
        'first-contentful-paint': ['error', {maxNumericValue: 2000}],
        'largest-contentful-paint': ['error', {maxNumericValue: 2500}],
        'cumulative-layout-shift': ['error', {maxNumericValue: 0.1}]
      }
    },
    upload: {
      target: 'temporary-public-storage'
    }
  }
};
```

**Performance optimization checklist**:
- [ ] Images optimized and using next/image
- [ ] Fonts optimized (font-display: swap)
- [ ] Code splitting implemented
- [ ] Dynamic imports for heavy components
- [ ] CDN configured for static assets
- [ ] Compression enabled (gzip/brotli)
- [ ] Critical CSS inlined
- [ ] Unused JavaScript removed

### 3.3 Accessibility Validation

**Tools**:
- axe DevTools
- WAVE
- Lighthouse accessibility audit
- Manual keyboard navigation testing

**WCAG 2.1 AA Checklist**:
- [ ] All images have alt text
- [ ] Color contrast ratio ≥4.5:1 for normal text
- [ ] Color contrast ratio ≥3:1 for large text
- [ ] All interactive elements keyboard accessible
- [ ] Focus indicators visible
- [ ] Skip to main content link
- [ ] Form labels associated with inputs
- [ ] ARIA labels where appropriate
- [ ] No flashing content >3 times/second
- [ ] Semantic HTML used correctly
- [ ] Heading hierarchy logical

**Automated test** (axe-core):
```javascript
// tests/accessibility.spec.ts
import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

test('homepage passes accessibility scan', async ({ page }) => {
  await page.goto('/');

  const accessibilityScanResults = await new AxeBuilder({ page }).analyze();

  expect(accessibilityScanResults.violations).toEqual([]);
});
```

### 3.4 Mobile Responsiveness

**Devices to test**:
- iPhone 12/13/14 (iOS Safari)
- Samsung Galaxy S21/S22 (Chrome Android)
- iPad Pro (Safari)
- Google Pixel (Chrome)

**Responsive breakpoints**:
- Mobile: 320px - 767px
- Tablet: 768px - 1023px
- Desktop: 1024px+

**Automated test** (Playwright):
```javascript
// tests/responsive.spec.ts
const devices = [
  { name: 'iPhone 12', viewport: { width: 390, height: 844 } },
  { name: 'iPad Pro', viewport: { width: 1024, height: 1366 } },
  { name: 'Desktop', viewport: { width: 1920, height: 1080 } }
];

devices.forEach(device => {
  test(`renders correctly on ${device.name}`, async ({ page }) => {
    await page.setViewportSize(device.viewport);
    await page.goto('/');

    // Check critical elements visible
    await expect(page.locator('nav')).toBeVisible();
    await expect(page.locator('h1')).toBeVisible();
    await expect(page.locator('.cta-button')).toBeVisible();

    // Take screenshot for visual regression
    await page.screenshot({ path: `screenshots/${device.name}.png`, fullPage: true });
  });
});
```

**Manual testing checklist**:
- [ ] Navigation menu works on mobile
- [ ] Forms usable on small screens
- [ ] Touch targets ≥44x44px
- [ ] No horizontal scrolling
- [ ] Text readable without zooming
- [ ] Videos/images scale properly
- [ ] Modals/popups work on mobile

---

## Phase 4: Deployment & Post-Launch Validation

### 4.1 Vercel Deployment Setup

**Project configuration**:

```bash
# Install Vercel CLI
npm i -g vercel

# Link project
cd landing-page-v0
vercel link

# Configure environment variables
vercel env add NEXT_PUBLIC_GA_ID production
vercel env add ZOHO_API_KEY production
vercel env add SENDGRID_API_KEY production
```

**`vercel.json` configuration**:
```json
{
  "buildCommand": "npm run build",
  "framework": "nextjs",
  "redirects": [
    {
      "source": "/demo",
      "destination": "/request-demo",
      "permanent": true
    }
  ],
  "headers": [
    {
      "source": "/(.*)",
      "headers": [
        {
          "key": "X-Frame-Options",
          "value": "DENY"
        },
        {
          "key": "X-Content-Type-Options",
          "value": "nosniff"
        },
        {
          "key": "Strict-Transport-Security",
          "value": "max-age=31536000; includeSubDomains"
        }
      ]
    }
  ]
}
```

**Deployment checklist**:
- [ ] Production environment variables set
- [ ] Custom domain configured
- [ ] SSL certificate active
- [ ] DNS records propagated
- [ ] Analytics connected
- [ ] Error tracking configured (Sentry)
- [ ] Preview deployments enabled

### 4.2 Post-Deployment Smoke Tests

**Automated smoke test suite**:
```javascript
// tests/smoke.spec.ts
import { test, expect } from '@playwright/test';

const PRODUCTION_URL = 'https://hdim.example.com';

test.describe('Production Smoke Tests', () => {
  test('homepage loads successfully', async ({ page }) => {
    const response = await page.goto(PRODUCTION_URL);
    expect(response?.status()).toBe(200);

    await expect(page.locator('h1')).toBeVisible();
    await expect(page.locator('.cta-button')).toBeVisible();
  });

  test('all critical pages load', async ({ page }) => {
    const pages = ['/', '/pricing', '/request-demo', '/about', '/contact'];

    for (const path of pages) {
      const response = await page.goto(`${PRODUCTION_URL}${path}`);
      expect(response?.status()).toBe(200);
    }
  });

  test('form submission works', async ({ page }) => {
    await page.goto(`${PRODUCTION_URL}/request-demo`);

    // Fill form with test data
    await page.fill('[name="email"]', `test+${Date.now()}@example.com`);
    // ... fill other fields

    await page.click('button[type="submit"]');

    await expect(page.locator('.success-message')).toBeVisible({ timeout: 10000 });
  });

  test('analytics fires', async ({ page }) => {
    let analyticsRequest = false;

    page.on('request', request => {
      if (request.url().includes('google-analytics.com')) {
        analyticsRequest = true;
      }
    });

    await page.goto(PRODUCTION_URL);

    expect(analyticsRequest).toBe(true);
  });
});
```

**Run smoke tests**:
```bash
# Run against production
PLAYWRIGHT_BASE_URL=https://hdim.example.com npx playwright test smoke
```

### 4.3 Monitoring Setup

**Tools to configure**:

1. **Uptime monitoring** (UptimeRobot / Pingdom)
   - Check URL: https://hdim.example.com
   - Interval: 5 minutes
   - Alert contacts configured

2. **Error tracking** (Sentry)
   ```javascript
   // _app.tsx or layout.tsx
   import * as Sentry from '@sentry/nextjs';

   Sentry.init({
     dsn: process.env.NEXT_PUBLIC_SENTRY_DSN,
     environment: process.env.NODE_ENV,
     tracesSampleRate: 1.0
   });
   ```

3. **Performance monitoring** (Vercel Analytics)
   ```javascript
   // app/layout.tsx
   import { Analytics } from '@vercel/analytics/react';

   export default function RootLayout({ children }) {
     return (
       <html>
         <body>
           {children}
           <Analytics />
         </body>
       </html>
     );
   }
   ```

4. **Real User Monitoring** (Google Analytics + Web Vitals)
   ```javascript
   // lib/vitals.js
   import { getCLS, getFID, getFCP, getLCP, getTTFB } from 'web-vitals';

   function sendToAnalytics(metric) {
     window.gtag('event', metric.name, {
       value: Math.round(metric.value),
       event_label: metric.id,
       non_interaction: true
     });
   }

   getCLS(sendToAnalytics);
   getFID(sendToAnalytics);
   getFCP(sendToAnalytics);
   getLCP(sendToAnalytics);
   getTTFB(sendToAnalytics);
   ```

---

## Phase 5: Continuous Validation (CI/CD)

### 5.1 GitHub Actions Workflow

**File**: `.github/workflows/landing-page-ci.yml`

```yaml
name: Landing Page CI/CD

on:
  push:
    branches: [main, staging]
    paths:
      - 'landing-page-v0/**'
  pull_request:
    branches: [main]
    paths:
      - 'landing-page-v0/**'

jobs:
  validate:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
          cache: 'npm'
          cache-dependency-path: landing-page-v0/package-lock.json

      - name: Install dependencies
        working-directory: landing-page-v0
        run: npm ci

      - name: Run linter
        working-directory: landing-page-v0
        run: npm run lint

      - name: Run type check
        working-directory: landing-page-v0
        run: npm run type-check

      - name: Build
        working-directory: landing-page-v0
        run: npm run build

      - name: Validate content
        working-directory: landing-page-v0
        run: npm run validate:content

      - name: Validate assets
        working-directory: landing-page-v0
        run: npm run validate:assets

      - name: Run unit tests
        working-directory: landing-page-v0
        run: npm run test

      - name: Install Playwright
        working-directory: landing-page-v0
        run: npx playwright install --with-deps

      - name: Run E2E tests
        working-directory: landing-page-v0
        run: npm run test:e2e

      - name: Run Lighthouse CI
        working-directory: landing-page-v0
        run: |
          npm install -g @lhci/cli
          lhci autorun

      - name: Check broken links
        working-directory: landing-page-v0
        run: npm run check:links

      - name: Validate SEO
        working-directory: landing-page-v0
        run: npm run validate:seo

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-results
          path: |
            landing-page-v0/playwright-report/
            landing-page-v0/lighthouse-report/
          retention-days: 30

  deploy-preview:
    needs: validate
    if: github.event_name == 'pull_request'
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Deploy to Vercel Preview
        uses: amondnet/vercel-action@v20
        with:
          vercel-token: ${{ secrets.VERCEL_TOKEN }}
          vercel-org-id: ${{ secrets.VERCEL_ORG_ID }}
          vercel-project-id: ${{ secrets.VERCEL_PROJECT_ID }}
          working-directory: landing-page-v0

      - name: Comment PR with preview URL
        uses: actions/github-script@v6
        with:
          script: |
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: `✅ Preview deployment ready!\n\n🔗 ${process.env.VERCEL_URL}`
            })

  deploy-production:
    needs: validate
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Deploy to Vercel Production
        uses: amondnet/vercel-action@v20
        with:
          vercel-token: ${{ secrets.VERCEL_TOKEN }}
          vercel-org-id: ${{ secrets.VERCEL_ORG_ID }}
          vercel-project-id: ${{ secrets.VERCEL_PROJECT_ID }}
          vercel-args: '--prod'
          working-directory: landing-page-v0

      - name: Run production smoke tests
        working-directory: landing-page-v0
        env:
          PLAYWRIGHT_BASE_URL: https://hdim.example.com
        run: npx playwright test smoke

      - name: Notify team on Slack
        uses: 8398a7/action-slack@v3
        with:
          status: ${{ job.status }}
          text: '🚀 Landing page deployed to production!'
          webhook_url: ${{ secrets.SLACK_WEBHOOK }}
        if: always()
```

### 5.2 NPM Scripts Setup

**Add to `landing-page-v0/package.json`**:

```json
{
  "scripts": {
    "dev": "next dev",
    "build": "next build",
    "start": "next start",
    "lint": "next lint",
    "type-check": "tsc --noEmit",
    "test": "jest",
    "test:e2e": "playwright test",
    "test:smoke": "playwright test smoke",
    "validate:content": "node scripts/validate-content.js",
    "validate:assets": "bash scripts/validate-assets.sh",
    "validate:seo": "node scripts/validate-seo.js",
    "check:links": "node scripts/check-links.js",
    "lighthouse": "lhci autorun",
    "validate:all": "npm run validate:content && npm run validate:assets && npm run validate:seo && npm run check:links"
  }
}
```

---

## Phase 6: Final Pre-Launch Checklist

### 6.1 Technical Validation

- [ ] All pages load successfully (200 status)
- [ ] No broken links (internal or external)
- [ ] All forms submit correctly
- [ ] Analytics tracking verified
- [ ] CRM integration working
- [ ] Email notifications sending
- [ ] SEO meta tags present on all pages
- [ ] Structured data validates
- [ ] Sitemap generated and submitted
- [ ] Robots.txt configured
- [ ] Lighthouse scores meet targets (90/95/95/100)
- [ ] Core Web Vitals pass
- [ ] Accessibility audit passes (WCAG 2.1 AA)
- [ ] Mobile responsive on all devices
- [ ] SSL certificate active
- [ ] Custom domain configured
- [ ] Error tracking configured
- [ ] Uptime monitoring active
- [ ] License compliance verified (docs/compliance/THIRD_PARTY_NOTICES.md)

### 6.2 Content Validation

- [ ] All copy reviewed and approved
- [ ] No placeholder text ("Lorem ipsum", "TODO")
- [ ] Brand voice consistent
- [ ] Legal pages complete (Privacy, Terms, Cookies)
- [ ] Product videos uploaded and playing
- [ ] Demo screenshots optimized
- [ ] PDFs accessible and downloadable
- [ ] ROI calculator functional
- [ ] Testimonials/case studies present
- [ ] Pricing page accurate
- [ ] Contact information correct
- [ ] License compliance verified (docs/compliance/THIRD_PARTY_NOTICES.md)

### 6.3 Marketing Validation

- [ ] UTM parameters configured
- [ ] Conversion goals set up in GA4
- [ ] Lead scoring configured in CRM
- [ ] Email sequences tested
- [ ] Social share images (OG/Twitter) correct
- [ ] Demo booking flow works end-to-end
- [ ] Download gating (if applicable) works
- [ ] Newsletter signup functional

### 6.4 Security Validation

- [ ] HTTPS enforced
- [ ] Security headers configured
- [ ] No sensitive data in client-side code
- [ ] API keys secured (environment variables)
- [ ] CORS configured correctly
- [ ] Rate limiting on forms (prevent spam)
- [ ] CAPTCHA/bot protection active
- [ ] CSP (Content Security Policy) configured

---

## Validation Schedule

### Implementation Timeline

**Day 1**: Setup & Automation
- Set up validation scripts
- Configure Playwright tests
- Set up Lighthouse CI
- Configure GitHub Actions workflow

**Day 2**: Testing & Fixes
- Run full validation suite
- Fix identified issues
- Re-validate fixes
- Manual testing on devices

**Day 3**: Deployment & Monitoring
- Deploy to Vercel production
- Run smoke tests
- Configure monitoring tools
- Final sign-off

### Ongoing Monitoring

**Daily**:
- Uptime monitoring alerts
- Error tracking review (Sentry)
- Form submission monitoring

**Weekly**:
- Analytics review (traffic, conversions)
- Performance metrics (Web Vitals)
- Broken link check
- SEO ranking check

**Monthly**:
- Full Lighthouse audit
- Accessibility audit
- Content freshness review
- Security audit

---

## Tools & Technologies

### Required Tools

| Tool | Purpose | Cost |
|------|---------|------|
| Playwright | E2E testing | Free |
| Lighthouse CI | Performance monitoring | Free |
| broken-link-checker | Link validation | Free |
| axe-core | Accessibility testing | Free |
| Google Analytics 4 | Web analytics | Free |
| Google Search Console | SEO monitoring | Free |
| Vercel | Hosting & deployment | Free tier available |
| Sentry | Error tracking | Free tier available |

### Optional Tools

| Tool | Purpose | Cost |
|------|---------|------|
| UptimeRobot | Uptime monitoring | Free tier available |
| SEMrush | SEO audit | Paid |
| Screaming Frog | SEO crawling | Free up to 500 URLs |
| Hotjar | User behavior | Free tier available |

---

## Success Metrics

### Technical Metrics

- **Uptime**: 99.9%+
- **Page Load Time**: <2.5s (LCP)
- **Lighthouse Performance**: 90+
- **Broken Links**: 0
- **Form Success Rate**: >95%
- **Mobile Traffic**: No errors

### Business Metrics

- **Demo Requests**: Track conversion rate
- **Newsletter Signups**: Track growth
- **Bounce Rate**: <60%
- **Time on Page**: >2 minutes
- **Pages per Session**: >2
- **Lead Quality**: Track CRM scoring

---

## Emergency Response Plan

### Issue Categories

**P0 - Critical** (site down, forms broken)
- Response time: <1 hour
- Rollback immediately if needed
- Notify team via Slack/PagerDuty

**P1 - High** (broken links, analytics down)
- Response time: <4 hours
- Fix within same day

**P2 - Medium** (styling issues, slow loading)
- Response time: <24 hours
- Fix in next deployment

**P3 - Low** (minor copy changes, optimizations)
- Response time: <1 week
- Bundle with other updates

### Rollback Procedure

Vercel makes rollback easy:

```bash
# List recent deployments
vercel ls

# Rollback to previous deployment
vercel rollback <deployment-url>
```

Or via Vercel dashboard:
1. Go to Deployments
2. Find working deployment
3. Click "Promote to Production"

---

## Appendix

### A. Validation Scripts

All validation scripts referenced in this plan should be created in:
```
landing-page-v0/
├── scripts/
│   ├── validate-content.js
│   ├── validate-assets.sh
│   ├── validate-seo.js
│   └── check-links.js
├── tests/
│   ├── forms.spec.ts
│   ├── responsive.spec.ts
│   ├── accessibility.spec.ts
│   ├── analytics.spec.ts
│   └── smoke.spec.ts
└── lighthouserc.js
```

### B. Browser & Device Testing Matrix

| Browser | Version | Desktop | Mobile | Tablet |
|---------|---------|---------|--------|--------|
| Chrome | Latest | ✓ | ✓ | ✓ |
| Firefox | Latest | ✓ | - | - |
| Safari | Latest | ✓ | ✓ (iOS) | ✓ (iPadOS) |
| Edge | Latest | ✓ | - | - |
| Samsung Internet | Latest | - | ✓ | - |

### C. Page Inventory

Ensure all pages are validated:

**Public Pages**:
- [ ] Homepage (/)
- [ ] About (/about)
- [ ] Pricing (/pricing)
- [ ] Request Demo (/request-demo)
- [ ] Contact (/contact)
- [ ] Blog (/blog) - if applicable
- [ ] Case Studies (/case-studies) - if applicable
- [ ] Resources (/resources) - if applicable

**Legal Pages**:
- [ ] Privacy Policy (/privacy)
- [ ] Terms of Service (/terms)
- [ ] Cookie Policy (/cookies)
- [ ] GDPR Compliance (/gdpr) - if EU targeting

**Utility Pages**:
- [ ] 404 Error
- [ ] 500 Error
- [ ] Thank You (/thank-you)
- [ ] Confirmation (/confirmed)

---

## Implementation Command

To implement this validation plan:

```bash
# 1. Navigate to landing page directory
cd landing-page-v0

# 2. Install validation dependencies
npm install --save-dev \
  @playwright/test \
  @lhci/cli \
  broken-link-checker \
  @axe-core/playwright \
  cheerio \
  glob \
  markdown-it \
  web-vitals

# 3. Create scripts directory
mkdir -p scripts tests

# 4. Copy validation scripts (see Appendix A)
# Scripts will be provided separately

# 5. Configure Playwright
npx playwright install

# 6. Run initial validation
npm run validate:ci

# 7. Fix any issues found

# 8. Set up GitHub Actions
# Use .github/workflows/landing-page-validation.yml

# 9. Deploy to Vercel
vercel --prod

# 10. Run smoke tests
npm run test:smoke
```

---

**Document Version**: 1.0
**Last Updated**: December 30, 2025
**Owner**: Engineering & Marketing Teams
**Next Review**: Post-deployment (within 7 days)
