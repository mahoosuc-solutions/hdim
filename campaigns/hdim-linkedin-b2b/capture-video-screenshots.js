/**
 * Video-Ready Screenshot Capture Script
 * Captures detailed screenshots of each service for video production
 */
const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');

const SCREENSHOTS_DIR = path.join(__dirname, 'vercel-deploy/assets/screenshots/video');
const BASE_URL = 'http://localhost:4200';

// Ensure directory exists
if (!fs.existsSync(SCREENSHOTS_DIR)) {
    fs.mkdirSync(SCREENSHOTS_DIR, { recursive: true });
}

// All routes to capture for video segments
const VIDEO_ROUTES = [
    // Core Platform
    { path: '/dashboard', name: '01-dashboard', description: 'Provider Dashboard', waitFor: '.stat-value' },
    { path: '/patients', name: '02-patients-list', description: 'Patient Registry', waitFor: 'table, .patient-list' },

    // Quality Measures
    { path: '/evaluations', name: '03-evaluations', description: 'Quality Measure Evaluation Wizard', waitFor: 'mat-stepper, .wizard' },
    { path: '/results', name: '04-results', description: 'Evaluation Results', waitFor: '.result-card, .analytics' },
    { path: '/measure-builder', name: '05-measure-builder', description: 'CQL Measure Builder', waitFor: '.measure-builder, .editor' },

    // Care Management
    { path: '/care-gaps', name: '06-care-gaps', description: 'Care Gap Management', waitFor: '.care-gap, .gap-card' },
    { path: '/care-recommendations', name: '07-care-recommendations', description: 'AI Care Recommendations', waitFor: '.recommendation' },
    { path: '/patient-health', name: '08-patient-health', description: 'Patient Health Overview', waitFor: '.health-overview' },

    // Reports & Analytics
    { path: '/reports', name: '09-reports', description: 'Quality Reports', waitFor: '.report-card' },

    // Visualizations
    { path: '/visualization/live-monitor', name: '10-live-monitor', description: 'Live Batch Processing Monitor', waitFor: '.monitor, .batch' },
    { path: '/visualization/quality-constellation', name: '11-quality-constellation', description: 'Quality Measure Constellation', waitFor: 'canvas, svg' },
    { path: '/visualization/flow-network', name: '12-flow-network', description: 'Data Flow Network', waitFor: 'canvas, svg' },
    { path: '/visualization/measure-matrix', name: '13-measure-matrix', description: 'Measure Performance Matrix', waitFor: '.matrix, table' },

    // AI & Knowledge
    { path: '/ai-assistant', name: '14-ai-assistant', description: 'AI-Powered Clinical Assistant', waitFor: '.ai-dashboard, .assistant' },
    { path: '/knowledge-base', name: '15-knowledge-base', description: 'Clinical Knowledge Base', waitFor: '.knowledge-base, .article' },

    // Developer Tools
    { path: '/agent-builder', name: '16-agent-builder', description: 'AI Agent Builder', waitFor: '.agent-builder, .workflow' },
];

/**
 * Wait for loading to complete
 */
async function waitForLoadingComplete(page, timeout = 15000) {
    const spinnerSelectors = [
        '.loading', '.spinner', 'mat-spinner', '.mat-progress-spinner',
        '[class*="loading"]', '[class*="spinner"]'
    ];

    for (const selector of spinnerSelectors) {
        try {
            await page.waitForSelector(selector, { state: 'hidden', timeout: timeout / spinnerSelectors.length });
        } catch (e) { }
    }
}

/**
 * Wait for page-specific content
 */
async function waitForContent(page, selectors, timeout = 20000) {
    if (!selectors) return;

    const selectorList = selectors.split(',').map(s => s.trim());
    for (const selector of selectorList) {
        try {
            await page.waitForSelector(selector, { timeout: timeout / selectorList.length });
            console.log(`  ✓ Found: ${selector}`);
            return true;
        } catch (e) { }
    }
    console.log(`  ⚠ Content selectors not found: ${selectors}`);
    return false;
}

async function captureVideoScreenshots() {
    console.log('═══════════════════════════════════════════════════════════');
    console.log('  VIDEO-READY SCREENSHOT CAPTURE');
    console.log('  Capturing detailed screenshots for video production');
    console.log('═══════════════════════════════════════════════════════════\n');

    const browser = await chromium.launch({
        headless: true,
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    });

    // Full HD viewport for video
    const context = await browser.newContext({
        viewport: { width: 1920, height: 1080 },
        deviceScaleFactor: 2
    });

    const page = await context.newPage();
    const results = { success: [], failed: [], skipped: [] };

    try {
        // Authenticate
        console.log('🔐 AUTHENTICATING...\n');
        await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle', timeout: 60000 });
        await page.waitForTimeout(2000);

        const demoButton = page.locator('button:has-text("Demo Login")');
        if (await demoButton.isVisible({ timeout: 5000 })) {
            await demoButton.click();
            await page.waitForTimeout(3000);
            await page.waitForURL('**/dashboard**', { timeout: 30000 }).catch(() => {});
        } else {
            // Set localStorage directly
            await page.evaluate(() => {
                const demoToken = 'demo-jwt-token-' + Date.now();
                const demoUser = {
                    id: 'demo-user-1',
                    username: 'demo',
                    email: 'demo@healthdata.com',
                    firstName: 'Demo',
                    lastName: 'User',
                    fullName: 'Demo User',
                    roles: [{
                        id: 'role-admin',
                        name: 'ADMIN',
                        description: 'Administrator',
                        permissions: [
                            { id: 'p1', name: 'VIEW_PATIENTS' },
                            { id: 'p2', name: 'EDIT_PATIENTS' },
                            { id: 'p3', name: 'VIEW_EVALUATIONS' },
                            { id: 'p4', name: 'RUN_EVALUATIONS' },
                            { id: 'p5', name: 'EXPORT_DATA' },
                            { id: 'p6', name: 'VIEW_REPORTS' },
                            { id: 'p7', name: 'VIEW_CARE_GAPS' },
                        ],
                    }],
                    tenantId: 'demo-tenant',
                    active: true,
                };
                localStorage.setItem('healthdata_auth_token', demoToken);
                localStorage.setItem('healthdata_user', JSON.stringify(demoUser));
            });
            await page.goto(`${BASE_URL}/dashboard`, { waitUntil: 'networkidle', timeout: 60000 });
        }

        console.log('✓ Authenticated successfully\n');
        console.log('═══════════════════════════════════════════════════════════');
        console.log('  CAPTURING SCREENSHOTS');
        console.log('═══════════════════════════════════════════════════════════\n');

        // Capture each route
        for (const route of VIDEO_ROUTES) {
            console.log(`📸 ${route.name}: ${route.description}`);
            console.log(`   Route: ${route.path}`);

            try {
                await page.goto(`${BASE_URL}${route.path}`, {
                    waitUntil: 'networkidle',
                    timeout: 60000
                });

                // Check for redirect
                const currentUrl = page.url();
                if (currentUrl.includes('/login')) {
                    console.log('   ⚠ Redirected to login - skipping');
                    results.skipped.push(route);
                    continue;
                }
                if (currentUrl.includes('/dashboard') && !route.path.includes('/dashboard')) {
                    console.log(`   ⚠ Redirected to dashboard - route may not exist`);
                    results.skipped.push(route);
                    continue;
                }

                // Wait for loading
                await waitForLoadingComplete(page);

                // Wait for specific content
                await waitForContent(page, route.waitFor);

                // Wait for any animations
                await page.waitForTimeout(2000);

                // Capture screenshot
                const filename = `${route.name}.png`;
                await page.screenshot({
                    path: path.join(SCREENSHOTS_DIR, filename),
                    fullPage: false
                });

                const stats = fs.statSync(path.join(SCREENSHOTS_DIR, filename));
                console.log(`   ✓ Captured: ${filename} (${Math.round(stats.size / 1024)}KB)\n`);
                results.success.push(route);

            } catch (error) {
                console.log(`   ✗ Failed: ${error.message}\n`);
                results.failed.push({ ...route, error: error.message });
            }
        }

        // Summary
        console.log('═══════════════════════════════════════════════════════════');
        console.log('  CAPTURE SUMMARY');
        console.log('═══════════════════════════════════════════════════════════');
        console.log(`  ✓ Successful: ${results.success.length}`);
        console.log(`  ⚠ Skipped:    ${results.skipped.length}`);
        console.log(`  ✗ Failed:     ${results.failed.length}`);
        console.log('');

        if (results.skipped.length > 0) {
            console.log('  Skipped routes (not available or redirected):');
            results.skipped.forEach(r => console.log(`    - ${r.path}: ${r.description}`));
        }

        console.log(`\n  Screenshots saved to: ${SCREENSHOTS_DIR}`);

        // List all captured files
        const files = fs.readdirSync(SCREENSHOTS_DIR).filter(f => f.endsWith('.png'));
        console.log(`\n  Total files: ${files.length}`);

        let totalSize = 0;
        files.forEach(f => {
            const stats = fs.statSync(path.join(SCREENSHOTS_DIR, f));
            totalSize += stats.size;
        });
        console.log(`  Total size: ${Math.round(totalSize / 1024 / 1024 * 10) / 10}MB`);

    } catch (error) {
        console.error('\n✗ Fatal error:', error.message);
    } finally {
        await browser.close();
    }
}

captureVideoScreenshots();
