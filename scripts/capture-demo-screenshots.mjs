import { chromium } from '@playwright/test';
import { mkdir } from 'node:fs/promises';
import path from 'node:path';

const baseUrl = process.env.DEMO_URL || 'http://localhost:4200';
const outputDir = process.env.SCREENSHOT_DIR || path.join('screenshots', 'demo-portal');
const username = process.env.DEMO_USER || 'demo_admin';
const password = process.env.DEMO_PASS || 'demo123';

const viewPort = { width: 1920, height: 1080 };
const patientId = process.env.DEMO_PATIENT_ID || '550e8400-e29b-41d4-a716-446655440001';

const waitForAppIdle = async (page) => {
  await page.waitForTimeout(1000);
};

const waitForRows = async (page, selector, timeout = 20000) => {
  await page.waitForFunction(
    (sel) => document.querySelectorAll(sel).length > 0,
    selector,
    { timeout }
  );
};

const capture = async (page, name) => {
  const filePath = path.join(outputDir, name);
  await page.screenshot({ path: filePath, fullPage: false });
  console.log(`Saved ${filePath}`);
};

const navigateAndCapture = async (page, route, name, waitForSelector) => {
  await page.goto(`${baseUrl}${route}`, { waitUntil: 'domcontentloaded' });
  if (waitForSelector) {
    await page.waitForSelector(waitForSelector, { timeout: 15000 });
  }
  await waitForAppIdle(page);
  await capture(page, name);
};

const run = async () => {
  await mkdir(outputDir, { recursive: true });

  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: viewPort });

  // Login screen
  await page.goto(`${baseUrl}/login`, { waitUntil: 'domcontentloaded' });
  await page.evaluate(() => {
    localStorage.setItem('provider-dashboard-tour:completed', 'true');
    localStorage.setItem('hdim-tour-first-visit', 'false');
  });
  await page.waitForSelector('form', { timeout: 15000 });
  await waitForAppIdle(page);
  await capture(page, '01-login.png');

  // Log in
  await page.fill('input[formcontrolname="username"]', username);
  await page.fill('input[formcontrolname="password"]', password);
  await page.click('button[type="submit"]');
  await page.waitForURL((url) => !url.pathname.endsWith('/login'), { timeout: 40000 });
  await waitForAppIdle(page);

  // Provider dashboard overview (top)
  await page.evaluate(() => window.scrollTo(0, 0));
  await waitForAppIdle(page);
  await capture(page, '02-dashboard-overview.png');

  // Provider dashboard care gap actions
  const careGapsHeader = page.locator('text=High Priority Care Gaps').first();
  if (await careGapsHeader.isVisible()) {
    await waitForRows(page, 'table.care-gaps-table tbody tr').catch(() => {});
    await careGapsHeader.scrollIntoViewIfNeeded();
    await waitForAppIdle(page);
  }
  await capture(page, '03-dashboard-care-gaps.png');

  // Risk stratification section (if present)
  const riskHeader = page.locator('text=Risk Stratification').first();
  if (await riskHeader.isVisible()) {
    await riskHeader.scrollIntoViewIfNeeded();
    await waitForAppIdle(page);
    await capture(page, '04-dashboard-risk-stratification.png');
  }

  // Care Gap Manager
  await navigateAndCapture(page, '/care-gaps', '05-care-gaps.png', 'text=Care Gaps');

  // Patients list
  await navigateAndCapture(page, '/patients', '06-patients-list.png', 'text=Patient Management');
  await waitForRows(page, 'table.patients-table tbody tr').catch(() => {});
  await waitForAppIdle(page);
  await capture(page, '06-patients-list.png');

  // Patient detail summary
  await page.waitForSelector('table.patients-table tbody tr', { timeout: 20000 }).catch(() => {});
  const patientTable = page.locator('table.patients-table').first();
  if (await patientTable.isVisible()) {
    await patientTable.scrollIntoViewIfNeeded();
    await waitForAppIdle(page);
  }

  const firstPatientRow = page.locator('table.patients-table tbody tr').first();
  let patientDetailCaptured = false;
  if (await firstPatientRow.isVisible()) {
    await firstPatientRow.click();
    await page.waitForURL('**/patients/**', { timeout: 15000 });
    await waitForAppIdle(page);
    await capture(page, '07-patient-detail-summary.png');
    patientDetailCaptured = true;

    // Patient detail care gaps section (scroll)
    const patientCareGapsHeader = page.locator('text=Care Gaps').first();
    if (await patientCareGapsHeader.isVisible()) {
      await patientCareGapsHeader.scrollIntoViewIfNeeded();
      await waitForAppIdle(page);
      await capture(page, '08-patient-detail-care-gaps.png');
    }
  }

  if (!patientDetailCaptured) {
    console.warn('No patient rows visible; navigating directly to patient detail.');
    await navigateAndCapture(page, `/patients/${patientId}`, '07-patient-detail-summary.png', 'text=Patient Detail');
    const patientCareGapsHeader = page.locator('text=Care Gaps').first();
    if (await patientCareGapsHeader.isVisible()) {
      await patientCareGapsHeader.scrollIntoViewIfNeeded();
      await waitForAppIdle(page);
      await capture(page, '08-patient-detail-care-gaps.png');
    }
  }

  // Results
  await navigateAndCapture(page, '/results', '09-results.png', 'text=Results');
  await waitForRows(page, 'table mat-row, table tbody tr').catch(() => {});
  await waitForAppIdle(page);
  await capture(page, '09-results.png');

  // Evaluations
  await navigateAndCapture(page, '/evaluations', '10-evaluations.png', 'text=Evaluations');
  await waitForRows(page, 'table mat-row, table tbody tr').catch(() => {});
  await waitForAppIdle(page);
  await capture(page, '10-evaluations.png');

  // Reports
  await navigateAndCapture(page, '/reports', '11-reports.png', 'text=Reports');

  await browser.close();
};

run().catch((error) => {
  console.error('Screenshot capture failed:', error);
  process.exit(1);
});
