const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');

const SCREENSHOTS_DIR = path.join(__dirname, 'vercel-deploy/assets/screenshots');
const BASE_URL = 'http://localhost:4200';

// Ensure directory exists
if (!fs.existsSync(SCREENSHOTS_DIR)) {
    fs.mkdirSync(SCREENSHOTS_DIR, { recursive: true });
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
        console.log('Navigating to login page...');
        await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle', timeout: 60000 });
        await page.waitForTimeout(2000);

        // Look for demo login button
        console.log('Looking for Demo Login button...');
        const demoButton = page.locator('button:has-text("Demo Login")');

        if (await demoButton.isVisible({ timeout: 5000 })) {
            console.log('Clicking Demo Login...');
            await demoButton.click();
            await page.waitForTimeout(3000); // Wait for redirect

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
        console.log('Authenticated. Current URL:', page.url());

        // 1. Dashboard Screenshot
        console.log('Capturing dashboard...');
        await page.goto(`${BASE_URL}/dashboard`, { waitUntil: 'networkidle', timeout: 60000 });
        await page.waitForTimeout(3000); // Wait for data to load
        await page.screenshot({
            path: path.join(SCREENSHOTS_DIR, 'dashboard.png'),
            fullPage: false
        });
        console.log('Dashboard captured!');

        // 2. Patients List Screenshot
        console.log('Capturing patients...');
        await page.goto(`${BASE_URL}/patients`, { waitUntil: 'networkidle', timeout: 60000 });
        await page.waitForTimeout(3000);
        await page.screenshot({
            path: path.join(SCREENSHOTS_DIR, 'patients.png'),
            fullPage: false
        });
        console.log('Patients captured!');

        // 3. Quality Measures / Evaluations Screenshot
        console.log('Capturing evaluations...');
        await page.goto(`${BASE_URL}/evaluations`, { waitUntil: 'networkidle', timeout: 60000 });
        await page.waitForTimeout(4000); // Extra time for measure loading
        await page.screenshot({
            path: path.join(SCREENSHOTS_DIR, 'evaluations.png'),
            fullPage: false
        });
        console.log('Evaluations captured!');

        // 4. Results/Analytics Screenshot
        console.log('Capturing results...');
        await page.goto(`${BASE_URL}/results`, { waitUntil: 'networkidle', timeout: 60000 });
        await page.waitForTimeout(3000);
        await page.screenshot({
            path: path.join(SCREENSHOTS_DIR, 'results.png'),
            fullPage: false
        });
        console.log('Results captured!');

        // 5. Care Gaps Screenshot
        console.log('Capturing care gaps...');
        await page.goto(`${BASE_URL}/care-gaps`, { waitUntil: 'networkidle', timeout: 60000 });
        await page.waitForTimeout(3000);
        await page.screenshot({
            path: path.join(SCREENSHOTS_DIR, 'care-gaps.png'),
            fullPage: false
        });
        console.log('Care gaps captured!');

        // 6. Reports Screenshot
        console.log('Capturing reports...');
        await page.goto(`${BASE_URL}/reports`, { waitUntil: 'networkidle', timeout: 60000 });
        await page.waitForTimeout(3000);
        await page.screenshot({
            path: path.join(SCREENSHOTS_DIR, 'reports.png'),
            fullPage: false
        });
        console.log('Reports captured!');

        // 7. Live Monitor Screenshot (if exists)
        console.log('Capturing live monitor...');
        try {
            await page.goto(`${BASE_URL}/live-monitor`, { waitUntil: 'networkidle', timeout: 30000 });
            await page.waitForTimeout(3000);
            await page.screenshot({
                path: path.join(SCREENSHOTS_DIR, 'live-monitor.png'),
                fullPage: false
            });
            console.log('Live monitor captured!');
        } catch (e) {
            console.log('Live monitor page not available, skipping...');
        }

        // 8. AI Assistant Screenshot (if exists)
        console.log('Capturing AI assistant...');
        try {
            await page.goto(`${BASE_URL}/ai-assistant`, { waitUntil: 'networkidle', timeout: 30000 });
            await page.waitForTimeout(3000);
            await page.screenshot({
                path: path.join(SCREENSHOTS_DIR, 'ai-assistant.png'),
                fullPage: false
            });
            console.log('AI assistant captured!');
        } catch (e) {
            console.log('AI assistant page not available, skipping...');
        }

        console.log('\n=== Screenshot capture complete ===');
        console.log(`Screenshots saved to: ${SCREENSHOTS_DIR}`);

    } catch (error) {
        console.error('Error capturing screenshots:', error.message);
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
