/**
 * Workflow Video Recording Script
 * Records 7 marketing videos based on the storyboard specifications
 * Uses Playwright to capture user interactions as MP4 files
 */
const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');

const VIDEOS_DIR = path.join(__dirname, 'vercel-deploy/assets/videos');
const BASE_URL = 'http://localhost:4200';

// Ensure directory exists
if (!fs.existsSync(VIDEOS_DIR)) {
    fs.mkdirSync(VIDEOS_DIR, { recursive: true });
}

/**
 * Helper: Wait for loading spinners to disappear
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
 * Helper: Smooth scroll to element
 */
async function smoothScrollTo(page, selector) {
    await page.evaluate((sel) => {
        const element = document.querySelector(sel);
        if (element) {
            element.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
    }, selector);
    await page.waitForTimeout(800);
}

/**
 * Helper: Highlight element with visual indicator
 */
async function highlightElement(page, selector, duration = 1500) {
    await page.evaluate((sel) => {
        const element = document.querySelector(sel);
        if (element) {
            const originalOutline = element.style.outline;
            const originalTransition = element.style.transition;
            element.style.transition = 'outline 0.3s ease';
            element.style.outline = '3px solid #00B4A0';
            setTimeout(() => {
                element.style.outline = originalOutline;
                element.style.transition = originalTransition;
            }, 1500);
        }
    }, selector);
    await page.waitForTimeout(duration);
}

/**
 * Authenticate using demo mode
 */
async function authenticate(page) {
    console.log('  Authenticating...');
    await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle', timeout: 60000 });
    await page.waitForTimeout(2000);

    const demoButton = page.locator('button:has-text("Demo Login")');
    if (await demoButton.isVisible({ timeout: 5000 })) {
        await demoButton.click();
        await page.waitForTimeout(3000);
        await page.waitForURL('**/dashboard**', { timeout: 30000 }).catch(() => {});
    } else {
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
    await page.waitForTimeout(2000);
    console.log('  ✓ Authenticated');
}

// ============================================================================
// VIDEO 1: Morning Dashboard Review (45 seconds)
// ============================================================================
async function recordVideo1_DashboardReview(context) {
    console.log('\n📹 Recording Video 1: Morning Dashboard Review');

    const page = await context.newPage();
    await authenticate(page);

    // Scene 1.2: Dashboard Load
    console.log('  Scene: Dashboard loading...');
    await page.goto(`${BASE_URL}/dashboard`, { waitUntil: 'networkidle' });
    await waitForLoadingComplete(page);
    await page.waitForTimeout(3000);

    // Scene 1.3: Review Quality Measures
    console.log('  Scene: Reviewing quality measures...');
    await smoothScrollTo(page, '.quality-measures, .measures-section, [class*="quality"]');
    await page.waitForTimeout(2000);

    // Hover over a measure
    const measureElement = page.locator('.measure-card, .metric-card, [class*="measure"]').first();
    if (await measureElement.isVisible({ timeout: 3000 })) {
        await measureElement.hover();
        await page.waitForTimeout(1500);
    }

    // Scene 1.4: Click on Care Gaps
    console.log('  Scene: Navigating to care gaps...');
    const careGapsLink = page.locator('a:has-text("Care Gaps"), button:has-text("Care Gaps"), [routerLink*="care-gaps"]').first();
    if (await careGapsLink.isVisible({ timeout: 3000 })) {
        await careGapsLink.click();
        await page.waitForTimeout(3000);
        await waitForLoadingComplete(page);
    } else {
        await page.goto(`${BASE_URL}/care-gaps`, { waitUntil: 'networkidle' });
    }
    await page.waitForTimeout(3000);

    // Scene 1.5: View patient detail
    console.log('  Scene: Viewing patient details...');
    const patientRow = page.locator('tr, .patient-card, .gap-row').first();
    if (await patientRow.isVisible({ timeout: 3000 })) {
        await patientRow.click();
        await page.waitForTimeout(2000);
    }

    await page.waitForTimeout(2000);
    await page.close();
    console.log('  ✓ Video 1 complete');
}

// ============================================================================
// VIDEO 2: Care Gap Prioritization (45 seconds)
// ============================================================================
async function recordVideo2_CareGapPrioritization(context) {
    console.log('\n📹 Recording Video 2: Care Gap Prioritization');

    const page = await context.newPage();
    await authenticate(page);

    // Navigate to care gaps
    console.log('  Scene: Opening care gap dashboard...');
    await page.goto(`${BASE_URL}/care-gaps`, { waitUntil: 'networkidle' });
    await waitForLoadingComplete(page);
    await page.waitForTimeout(3000);

    // Scene: View priority sections
    console.log('  Scene: Reviewing priority levels...');
    const highPriority = page.locator('[class*="high"], .priority-high, .urgent').first();
    if (await highPriority.isVisible({ timeout: 3000 })) {
        await highPriority.hover();
        await page.waitForTimeout(1500);
    }

    // Scene: Click on a patient
    console.log('  Scene: Selecting patient...');
    const patientCard = page.locator('.patient-card, .gap-card, tr').first();
    if (await patientCard.isVisible({ timeout: 3000 })) {
        await patientCard.click();
        await page.waitForTimeout(3000);
    }

    // Navigate to patient health overview
    console.log('  Scene: Viewing patient health overview...');
    await page.goto(`${BASE_URL}/patient-health`, { waitUntil: 'networkidle' });
    await waitForLoadingComplete(page);
    await page.waitForTimeout(3000);

    await page.close();
    console.log('  ✓ Video 2 complete');
}

// ============================================================================
// VIDEO 3: HEDIS Batch Evaluation (60 seconds)
// ============================================================================
async function recordVideo3_BatchEvaluation(context) {
    console.log('\n📹 Recording Video 3: HEDIS Batch Evaluation');

    const page = await context.newPage();
    await authenticate(page);

    // Navigate to evaluations
    console.log('  Scene: Opening evaluations...');
    await page.goto(`${BASE_URL}/evaluations`, { waitUntil: 'networkidle' });
    await waitForLoadingComplete(page);
    await page.waitForTimeout(3000);

    // Scene: Select measure dropdown
    console.log('  Scene: Selecting measure...');
    const measureDropdown = page.locator('mat-select, select, .measure-select').first();
    if (await measureDropdown.isVisible({ timeout: 3000 })) {
        await measureDropdown.click();
        await page.waitForTimeout(1500);
        // Select first option
        const option = page.locator('mat-option, option').first();
        if (await option.isVisible({ timeout: 2000 })) {
            await option.click();
            await page.waitForTimeout(1000);
        }
    }

    // Navigate to live monitor
    console.log('  Scene: Opening live monitor...');
    await page.goto(`${BASE_URL}/visualization/live-monitor`, { waitUntil: 'networkidle' });
    await waitForLoadingComplete(page);
    await page.waitForTimeout(4000);

    // Navigate to results
    console.log('  Scene: Viewing results...');
    await page.goto(`${BASE_URL}/results`, { waitUntil: 'networkidle' });
    await waitForLoadingComplete(page);
    await page.waitForTimeout(3000);

    // Navigate to reports for export
    console.log('  Scene: Viewing reports...');
    await page.goto(`${BASE_URL}/reports`, { waitUntil: 'networkidle' });
    await waitForLoadingComplete(page);
    await page.waitForTimeout(3000);

    await page.close();
    console.log('  ✓ Video 3 complete');
}

// ============================================================================
// VIDEO 4: Post-Discharge Readmission Prevention (55 seconds)
// ============================================================================
async function recordVideo4_ReadmissionPrevention(context) {
    console.log('\n📹 Recording Video 4: Post-Discharge Readmission Prevention');

    const page = await context.newPage();
    await authenticate(page);

    // Start at dashboard
    console.log('  Scene: Dashboard with alerts...');
    await page.goto(`${BASE_URL}/dashboard`, { waitUntil: 'networkidle' });
    await waitForLoadingComplete(page);
    await page.waitForTimeout(3000);

    // Navigate to care gaps (simulating alert click)
    console.log('  Scene: Viewing care gaps...');
    await page.goto(`${BASE_URL}/care-gaps`, { waitUntil: 'networkidle' });
    await waitForLoadingComplete(page);
    await page.waitForTimeout(3000);

    // View patient detail
    console.log('  Scene: Patient discharge details...');
    await page.goto(`${BASE_URL}/patient-health`, { waitUntil: 'networkidle' });
    await waitForLoadingComplete(page);
    await page.waitForTimeout(3000);

    // View AI recommendations
    console.log('  Scene: AI recommendations...');
    await page.goto(`${BASE_URL}/care-recommendations`, { waitUntil: 'networkidle' });
    await waitForLoadingComplete(page);
    await page.waitForTimeout(3000);

    await page.close();
    console.log('  ✓ Video 4 complete');
}

// ============================================================================
// VIDEO 5: Efficient Diabetes Visit (60 seconds)
// ============================================================================
async function recordVideo5_DiabetesVisit(context) {
    console.log('\n📹 Recording Video 5: Efficient Diabetes Visit');

    const page = await context.newPage();
    await authenticate(page);

    // Start at patient health overview (pre-visit summary)
    console.log('  Scene: Pre-visit summary...');
    await page.goto(`${BASE_URL}/patient-health`, { waitUntil: 'networkidle' });
    await waitForLoadingComplete(page);
    await page.waitForTimeout(3000);

    // View care gaps
    console.log('  Scene: Viewing care gaps...');
    await page.goto(`${BASE_URL}/care-gaps`, { waitUntil: 'networkidle' });
    await waitForLoadingComplete(page);
    await page.waitForTimeout(3000);

    // View AI assistant for recommendations
    console.log('  Scene: AI clinical assistant...');
    await page.goto(`${BASE_URL}/ai-assistant`, { waitUntil: 'networkidle' });
    await waitForLoadingComplete(page);
    await page.waitForTimeout(3000);

    // View knowledge base
    console.log('  Scene: Knowledge base...');
    await page.goto(`${BASE_URL}/knowledge-base`, { waitUntil: 'networkidle' });
    await waitForLoadingComplete(page);
    await page.waitForTimeout(3000);

    // Return to dashboard showing closed gaps
    console.log('  Scene: Dashboard with updated metrics...');
    await page.goto(`${BASE_URL}/dashboard`, { waitUntil: 'networkidle' });
    await waitForLoadingComplete(page);
    await page.waitForTimeout(3000);

    await page.close();
    console.log('  ✓ Video 5 complete');
}

// ============================================================================
// VIDEO 6: MIPS Performance Alert (60 seconds)
// ============================================================================
async function recordVideo6_MIPSAlert(context) {
    console.log('\n📹 Recording Video 6: MIPS Performance Alert');

    const page = await context.newPage();
    await authenticate(page);

    // Dashboard showing metrics
    console.log('  Scene: Dashboard with quality scores...');
    await page.goto(`${BASE_URL}/dashboard`, { waitUntil: 'networkidle' });
    await waitForLoadingComplete(page);
    await page.waitForTimeout(3000);

    // Scroll to quality measures
    console.log('  Scene: Quality measures performance...');
    await smoothScrollTo(page, '.quality-measures, .measures-section');
    await page.waitForTimeout(2000);

    // Navigate to results for detailed analysis
    console.log('  Scene: Detailed results analysis...');
    await page.goto(`${BASE_URL}/results`, { waitUntil: 'networkidle' });
    await waitForLoadingComplete(page);
    await page.waitForTimeout(3000);

    // View reports
    console.log('  Scene: Reports for intervention planning...');
    await page.goto(`${BASE_URL}/reports`, { waitUntil: 'networkidle' });
    await waitForLoadingComplete(page);
    await page.waitForTimeout(3000);

    // View care gaps for patient list
    console.log('  Scene: Patient list for outreach...');
    await page.goto(`${BASE_URL}/care-gaps`, { waitUntil: 'networkidle' });
    await waitForLoadingComplete(page);
    await page.waitForTimeout(3000);

    await page.close();
    console.log('  ✓ Video 6 complete');
}

// ============================================================================
// VIDEO 7: Real-Time Data Integration (45 seconds)
// ============================================================================
async function recordVideo7_DataIntegration(context) {
    console.log('\n📹 Recording Video 7: Real-Time Data Integration');

    const page = await context.newPage();
    await authenticate(page);

    // Live monitor showing data flow
    console.log('  Scene: Live batch monitor...');
    await page.goto(`${BASE_URL}/visualization/live-monitor`, { waitUntil: 'networkidle' });
    await waitForLoadingComplete(page);
    await page.waitForTimeout(4000);

    // Data flow network visualization
    console.log('  Scene: Data flow network...');
    await page.goto(`${BASE_URL}/visualization/flow-network`, { waitUntil: 'networkidle' });
    await waitForLoadingComplete(page);
    await page.waitForTimeout(4000);

    // Quality constellation
    console.log('  Scene: Quality constellation...');
    await page.goto(`${BASE_URL}/visualization/quality-constellation`, { waitUntil: 'networkidle' });
    await waitForLoadingComplete(page);
    await page.waitForTimeout(4000);

    // Measure matrix
    console.log('  Scene: Measure matrix...');
    await page.goto(`${BASE_URL}/visualization/measure-matrix`, { waitUntil: 'networkidle' });
    await waitForLoadingComplete(page);
    await page.waitForTimeout(4000);

    await page.close();
    console.log('  ✓ Video 7 complete');
}

// ============================================================================
// MAIN EXECUTION
// ============================================================================
async function recordAllVideos() {
    console.log('═══════════════════════════════════════════════════════════');
    console.log('  WORKFLOW VIDEO RECORDING');
    console.log('  Recording 7 marketing videos based on storyboard');
    console.log('═══════════════════════════════════════════════════════════\n');

    const browser = await chromium.launch({
        headless: true,
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    });

    // Create context with video recording enabled
    const context = await browser.newContext({
        viewport: { width: 1920, height: 1080 },
        deviceScaleFactor: 1,
        recordVideo: {
            dir: VIDEOS_DIR,
            size: { width: 1920, height: 1080 }
        }
    });

    const results = { success: [], failed: [] };

    const videos = [
        { name: 'Video 1: Dashboard Review', fn: recordVideo1_DashboardReview },
        { name: 'Video 2: Care Gap Prioritization', fn: recordVideo2_CareGapPrioritization },
        { name: 'Video 3: Batch Evaluation', fn: recordVideo3_BatchEvaluation },
        { name: 'Video 4: Readmission Prevention', fn: recordVideo4_ReadmissionPrevention },
        { name: 'Video 5: Diabetes Visit', fn: recordVideo5_DiabetesVisit },
        { name: 'Video 6: MIPS Alert', fn: recordVideo6_MIPSAlert },
        { name: 'Video 7: Data Integration', fn: recordVideo7_DataIntegration },
    ];

    for (const video of videos) {
        try {
            await video.fn(context);
            results.success.push(video.name);
        } catch (error) {
            console.error(`  ✗ Failed: ${error.message}`);
            results.failed.push({ name: video.name, error: error.message });
        }
    }

    await context.close();
    await browser.close();

    // Summary
    console.log('\n═══════════════════════════════════════════════════════════');
    console.log('  RECORDING SUMMARY');
    console.log('═══════════════════════════════════════════════════════════');
    console.log(`  ✓ Successful: ${results.success.length}`);
    console.log(`  ✗ Failed:     ${results.failed.length}`);
    console.log(`\n  Videos saved to: ${VIDEOS_DIR}`);

    // List video files
    if (fs.existsSync(VIDEOS_DIR)) {
        const files = fs.readdirSync(VIDEOS_DIR).filter(f => f.endsWith('.webm'));
        console.log(`\n  Video files (${files.length}):`);
        files.forEach(f => {
            const stats = fs.statSync(path.join(VIDEOS_DIR, f));
            console.log(`    - ${f} (${Math.round(stats.size / 1024 / 1024 * 10) / 10}MB)`);
        });
    }

    console.log('\n  Note: Videos are in WebM format. Convert to MP4 with:');
    console.log('  ffmpeg -i input.webm -c:v libx264 -crf 20 output.mp4');
}

// Run if executed directly
recordAllVideos().catch(console.error);
