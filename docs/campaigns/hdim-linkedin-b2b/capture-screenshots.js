const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');

const SCREENSHOTS_DIR = path.join(__dirname, 'vercel-deploy/assets/screenshots');
const BASE_URL = 'http://localhost:4200';

// Ensure directory exists
if (!fs.existsSync(SCREENSHOTS_DIR)) {
    fs.mkdirSync(SCREENSHOTS_DIR, { recursive: true });
}

/**
 * Helper: Wait for loading spinners to disappear
 */
async function waitForLoadingComplete(page, timeout = 10000) {
    const spinnerSelectors = [
        '.loading',
        '.spinner',
        '.mat-spinner',
        '.mat-progress-spinner',
        '[class*="loading"]',
        '[class*="spinner"]',
        'mat-spinner',
        'mat-progress-spinner',
        '.cdk-overlay-container mat-spinner'
    ];

    for (const selector of spinnerSelectors) {
        try {
            await page.waitForSelector(selector, { state: 'hidden', timeout: timeout / spinnerSelectors.length });
        } catch (e) {
            // Selector might not exist, that's ok
        }
    }
}

/**
 * Helper: Wait for specific element with non-zero/non-loading text
 */
async function waitForDataElement(page, selector, options = {}) {
    const { timeout = 15000, excludeText = ['0', 'Loading', 'loading...'] } = options;

    try {
        await page.waitForSelector(selector, { timeout });

        // Poll for non-zero/non-loading content
        const startTime = Date.now();
        while (Date.now() - startTime < timeout) {
            const text = await page.locator(selector).first().textContent().catch(() => '');
            const trimmed = text?.trim() || '';

            if (trimmed && !excludeText.some(ex => trimmed === ex || trimmed.includes(ex))) {
                console.log(`  ✓ Found data: "${trimmed.substring(0, 50)}..."`);
                return true;
            }
            await page.waitForTimeout(500);
        }
    } catch (e) {
        console.log(`  ⚠ Element ${selector} not found or timed out`);
    }
    return false;
}

/**
 * Helper: Validate page has loaded correctly (not on login)
 */
async function validateNotLoginPage(page) {
    const url = page.url();
    if (url.includes('/login')) {
        console.log('  ⚠ Redirected to login, re-authenticating...');
        return false;
    }
    return true;
}

/**
 * Helper: Capture screenshot with validation
 */
async function captureWithValidation(page, name, validationFn) {
    console.log(`\nCapturing ${name}...`);

    // Wait for network to settle
    await page.waitForLoadState('networkidle', { timeout: 30000 }).catch(() => {});

    // Wait for spinners to disappear
    await waitForLoadingComplete(page);

    // Run custom validation if provided
    if (validationFn) {
        await validationFn(page);
    }

    // Additional settle time for Angular animations
    await page.waitForTimeout(1500);

    // Take screenshot
    await page.screenshot({
        path: path.join(SCREENSHOTS_DIR, `${name}.png`),
        fullPage: false
    });

    console.log(`✓ ${name} captured!`);
}

async function captureScreenshots() {
    console.log('Launching browser...');
    const browser = await chromium.launch({
        headless: true,
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    });

    const context = await browser.newContext({
        viewport: { width: 1440, height: 900 },
        deviceScaleFactor: 2 // High-res for retina
    });

    const page = await context.newPage();

    try {
        // Step 1: Login via Demo Mode
        console.log('\n=== AUTHENTICATION ===');
        console.log('Navigating to login page...');
        await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle', timeout: 60000 });
        await page.waitForTimeout(2000);

        // Look for demo login button
        console.log('Looking for Demo Login button...');
        const demoButton = page.locator('button:has-text("Demo Login")');

        if (await demoButton.isVisible({ timeout: 5000 })) {
            console.log('Clicking Demo Login...');
            await demoButton.click();
            await page.waitForTimeout(3000);

            // Wait for navigation to complete
            await page.waitForURL('**/dashboard**', { timeout: 30000 }).catch(() => {
                console.log('Did not redirect to dashboard, checking current URL...');
            });
            console.log('Current URL:', page.url());
        } else {
            console.log('Demo Login button not found, setting localStorage directly...');

            // Set demo auth directly in localStorage
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
                            { id: 'perm-1', name: 'VIEW_PATIENTS' },
                            { id: 'perm-2', name: 'EDIT_PATIENTS' },
                            { id: 'perm-3', name: 'VIEW_EVALUATIONS' },
                            { id: 'perm-4', name: 'RUN_EVALUATIONS' },
                            { id: 'perm-5', name: 'EXPORT_DATA' },
                        ],
                    }],
                    tenantId: 'demo-tenant',
                    active: true,
                };
                localStorage.setItem('healthdata_auth_token', demoToken);
                localStorage.setItem('healthdata_user', JSON.stringify(demoUser));
            });

            // Reload to pick up auth
            await page.goto(`${BASE_URL}/dashboard`, { waitUntil: 'networkidle', timeout: 60000 });
        }

        await page.waitForTimeout(2000);
        console.log('✓ Authenticated. Current URL:', page.url());

        console.log('\n=== CAPTURING SCREENSHOTS ===');

        // 1. Dashboard Screenshot - Grade A in previous run
        await page.goto(`${BASE_URL}/dashboard`, { waitUntil: 'networkidle', timeout: 60000 });
        await captureWithValidation(page, 'dashboard', async (p) => {
            // Wait for quality score or patient count to show non-zero
            await waitForDataElement(p, '.stat-value, .metric-value, [class*="score"]', {
                excludeText: ['0', '0%', 'Loading']
            });
        });

        // 2. Patients List Screenshot - Grade D, needs improvement
        await page.goto(`${BASE_URL}/patients`, { waitUntil: 'networkidle', timeout: 60000 });
        await captureWithValidation(page, 'patients', async (p) => {
            // Wait for patient table rows or data to appear
            console.log('  Waiting for patient data to load...');

            // Wait for either table rows or stat cards with data
            const hasData = await Promise.race([
                waitForDataElement(p, 'table tbody tr', { timeout: 15000, excludeText: [] }),
                waitForDataElement(p, '.patient-card', { timeout: 15000, excludeText: [] }),
                waitForDataElement(p, '[class*="total-patients"]', { timeout: 15000, excludeText: ['0'] })
            ]);

            // Extra wait for Angular to render
            if (!hasData) {
                console.log('  ⚠ Patient data may not have loaded, waiting extra time...');
                await p.waitForTimeout(5000);
            }
        });

        // 3. Quality Measures / Evaluations Screenshot - Grade B
        await page.goto(`${BASE_URL}/evaluations`, { waitUntil: 'networkidle', timeout: 60000 });
        await captureWithValidation(page, 'evaluations', async (p) => {
            // Wait for measure cards or wizard to be visible
            await waitForDataElement(p, '.measure-card, .wizard-step, mat-stepper', {
                excludeText: ['Loading']
            });
            await p.waitForTimeout(2000); // Extra time for measure data
        });

        // 4. Results/Analytics Screenshot - Grade F, needs major fix
        await page.goto(`${BASE_URL}/results`, { waitUntil: 'networkidle', timeout: 60000 });
        await captureWithValidation(page, 'results', async (p) => {
            console.log('  Waiting for results data (longer timeout)...');

            // Wait explicitly for loading text to disappear
            try {
                await p.waitForFunction(() => {
                    const body = document.body.innerText;
                    return !body.includes('Loading results') && !body.includes('Loading...');
                }, { timeout: 20000 });
                console.log('  ✓ Loading complete');
            } catch (e) {
                console.log('  ⚠ Results may still be loading');
            }

            // Wait for actual data
            await waitForDataElement(p, '.result-card, .analytics-card, [class*="measure-result"]', {
                timeout: 15000,
                excludeText: ['0', '0%', 'Loading', 'No results']
            });

            await p.waitForTimeout(3000); // Extra settle time
        });

        // 5. Care Gaps Screenshot - Grade A in previous run
        await page.goto(`${BASE_URL}/care-gaps`, { waitUntil: 'networkidle', timeout: 60000 });
        await captureWithValidation(page, 'care-gaps', async (p) => {
            await waitForDataElement(p, '.gap-card, .care-gap-row, [class*="gap"]', {
                excludeText: ['Loading']
            });
        });

        // 6. Reports Screenshot - Grade B-
        await page.goto(`${BASE_URL}/reports`, { waitUntil: 'networkidle', timeout: 60000 });
        await captureWithValidation(page, 'reports', async (p) => {
            // Wait for report cards to be fully rendered
            await waitForDataElement(p, '.report-card, .report-type, [class*="report"]', {
                excludeText: ['Loading']
            });
            await p.waitForTimeout(2000); // Extra render time
        });

        // 7. Live Monitor Screenshot - Grade F, wrong page captured
        console.log('\nCapturing live monitor...');
        try {
            await page.goto(`${BASE_URL}/live-monitor`, { waitUntil: 'networkidle', timeout: 30000 });

            // Check if we got redirected
            const currentUrl = page.url();
            console.log(`  Current URL: ${currentUrl}`);

            if (currentUrl.includes('/dashboard') || !currentUrl.includes('live-monitor')) {
                console.log('  ⚠ Live monitor redirected to dashboard - route may not exist');
                console.log('  Skipping live-monitor screenshot (not a valid route)');
            } else {
                await captureWithValidation(page, 'live-monitor', async (p) => {
                    await waitForDataElement(p, '.monitor-panel, .live-data, [class*="monitor"]', {
                        excludeText: ['Loading']
                    });
                });
            }
        } catch (e) {
            console.log('  ✗ Live monitor page not available, skipping...');
        }

        // 8. AI Assistant Screenshot - Grade C (empty state expected)
        console.log('\nCapturing AI assistant...');
        try {
            await page.goto(`${BASE_URL}/ai-assistant`, { waitUntil: 'networkidle', timeout: 30000 });

            const currentUrl = page.url();
            if (currentUrl.includes('/login') || currentUrl.includes('/dashboard')) {
                console.log('  ⚠ AI assistant redirected - route may not exist');
                console.log('  Skipping ai-assistant screenshot');
            } else {
                await captureWithValidation(page, 'ai-assistant', async (p) => {
                    // Wait for assistant UI to render (empty state is ok)
                    await p.waitForSelector('[class*="assistant"], [class*="ai"], .chat-container', { timeout: 10000 }).catch(() => {});
                    await p.waitForTimeout(2000);
                });
            }
        } catch (e) {
            console.log('  ✗ AI assistant page not available, skipping...');
        }

        console.log('\n=== SCREENSHOT CAPTURE COMPLETE ===');
        console.log(`Screenshots saved to: ${SCREENSHOTS_DIR}`);

        // List captured files
        const files = fs.readdirSync(SCREENSHOTS_DIR);
        console.log('\nCaptured files:');
        files.filter(f => f.endsWith('.png')).forEach(f => {
            const stats = fs.statSync(path.join(SCREENSHOTS_DIR, f));
            console.log(`  - ${f} (${Math.round(stats.size / 1024)}KB)`);
        });

    } catch (error) {
        console.error('\n✗ Error capturing screenshots:', error.message);
        console.error('Full error:', error);

        // Save error state screenshot
        try {
            await page.screenshot({
                path: path.join(SCREENSHOTS_DIR, 'error-state.png'),
                fullPage: true
            });
            console.log('Error state screenshot saved');
        } catch (e) {
            console.error('Could not save error screenshot');
        }
    } finally {
        await browser.close();
    }
}

captureScreenshots();
