import { chromium } from '@playwright/test';
import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';

const baseUrl = process.env.DEMO_URL || 'http://localhost:4200';
const outputDir = process.env.SCREENSHOT_DIR || path.join('screenshots', 'demo-portal');
const username = process.env.DEMO_USER || 'demo_admin';
const password = process.env.DEMO_PASS || 'demo123';
const tenantId = process.env.DEMO_TENANT_ID || 'acme-health';
const backendUrl = process.env.BACKEND_URL || 'http://127.0.0.1:18080';
const demoSeedingUrl = process.env.DEMO_SEEDING_URL || 'http://127.0.0.1:8098';
const requireResultsForCapture = process.env.CAPTURE_REQUIRE_RESULTS !== '0';
const seedResultsIfMissing = process.env.CAPTURE_SEED_RESULTS_IF_MISSING !== '0';
const resultsWaitTimeoutMs = Number(process.env.CAPTURE_RESULTS_WAIT_TIMEOUT_MS || '300000');
const patientCandidateCount = Number(process.env.CAPTURE_PATIENT_CANDIDATE_COUNT || '20');
const minPatientObservations = Number(process.env.CAPTURE_MIN_PATIENT_OBSERVATIONS || '1');
const minPatientResults = Number(process.env.CAPTURE_MIN_PATIENT_RESULTS || '0');
const patientSelectorMode = process.env.DEMO_PATIENT_SELECTOR_MODE || 'data-rich';
const stopAfterResults = process.env.CAPTURE_STOP_AFTER_RESULTS === '1';
const screenshotTimeoutMs = Number(process.env.CAPTURE_SCREENSHOT_TIMEOUT_MS || '120000');
const patientDetailStepTimeoutMs = Number(process.env.CAPTURE_PATIENT_DETAIL_STEP_TIMEOUT_MS || '45000');

const viewPort = { width: 1920, height: 1080 };
const patientId = process.env.DEMO_PATIENT_ID || '550e8400-e29b-41d4-a716-446655440001';
const forceDirectPatientDetail = process.env.FORCE_DIRECT_PATIENT_DETAIL === '1';

const captureMetadata = {
  tenantId,
  selectedPatientId: null,
  selectedPatientStats: null,
  totalResultsAtStart: null,
  seededForResults: false,
  timestamp: new Date().toISOString(),
};

const waitForAppIdle = async (page) => {
  await page.waitForTimeout(1000);
};

const waitForPatientDetailReady = async (page, timeout = 30000) => {
  try {
    await page.waitForSelector('.patient-detail-container .content, .patient-detail-container, app-patient-detail', {
      state: 'visible',
      timeout,
    });
    await page.waitForFunction(
      () => !document.querySelector('.patient-detail-container .loading-overlay'),
      null,
      { timeout: 10000 }
    ).catch(() => {});
  } catch {
    console.warn('Patient detail readiness selector timed out; continuing with best-effort capture.');
  }
  await waitForAppIdle(page);
};

const waitForRows = async (page, selector, timeout = 20000) => {
  await page.waitForFunction(
    (sel) => document.querySelectorAll(sel).length > 0,
    selector,
    { timeout }
  );
};

const authHeaders = (token, includeContentType = false) => {
  const headers = {
    'X-Tenant-ID': tenantId,
  };
  if (includeContentType) {
    headers['Content-Type'] = 'application/json';
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return headers;
};

const responseCount = (payload) => {
  if (Array.isArray(payload)) {
    return payload.length;
  }
  if (payload && typeof payload === 'object') {
    if (typeof payload.total === 'number') {
      return payload.total;
    }
    if (Array.isArray(payload.entry)) {
      return payload.entry.length;
    }
    if (Array.isArray(payload.content)) {
      return payload.content.length;
    }
  }
  return 0;
};

async function fetchJson(url, options = {}) {
  const response = await fetch(url, options);
  const text = await response.text();
  let json = null;
  try {
    json = text ? JSON.parse(text) : null;
  } catch {
    json = null;
  }
  return { response, status: response.status, ok: response.ok, text, json };
}

async function loginDemoUser() {
  const resp = await fetch(`${backendUrl}/api/v1/auth/login`, {
    method: 'POST',
    headers: authHeaders(undefined, true),
    body: JSON.stringify({ username, password }),
  });

  if (!resp.ok) {
    throw new Error(`Demo login failed (${resp.status})`);
  }

  return resp.json();
}

async function ensureResultsData(token) {
  const getResultsCount = async () => {
    const { json, status } = await fetchJson(
      `${backendUrl}/quality-measure/results?page=0&size=1`,
      { headers: authHeaders(token) }
    );
    if (status >= 400) {
      return 0;
    }
    return responseCount(json);
  };

  let resultCount = await getResultsCount();
  captureMetadata.totalResultsAtStart = resultCount;

  if (resultCount > 0 || !requireResultsForCapture) {
    return;
  }

  if (!seedResultsIfMissing) {
    throw new Error('Results dataset is empty and CAPTURE_SEED_RESULTS_IF_MISSING=0');
  }

  const seedResp = await fetch(`${demoSeedingUrl}/demo/api/v1/demo/scenarios/hedis-evaluation`, {
    method: 'POST',
    headers: authHeaders(token, true),
    body: '{}',
  });

  if (!seedResp.ok) {
    throw new Error(`Failed to trigger HEDIS seed (${seedResp.status})`);
  }

  captureMetadata.seededForResults = true;
  const start = Date.now();
  while (Date.now() - start < resultsWaitTimeoutMs) {
    await new Promise((resolve) => setTimeout(resolve, 5000));
    resultCount = await getResultsCount();
    if (resultCount > 0) {
      return;
    }
  }

  throw new Error(`Timed out waiting for non-empty results after ${resultsWaitTimeoutMs}ms`);
}

async function resolvePatientForCapture(token) {
  if (patientSelectorMode === 'fixed') {
    captureMetadata.selectedPatientId = patientId;
    return patientId;
  }

  const patientsResp = await fetchJson(
    `${backendUrl}/fhir/Patient?_count=${patientCandidateCount}`,
    { headers: authHeaders(token) }
  );

  const entries = patientsResp.json?.entry || [];
  const patientIds = entries.map((entry) => entry?.resource?.id).filter(Boolean);
  if (patientIds.length === 0) {
    throw new Error('No patients available for screenshot capture');
  }

  if (patientSelectorMode === 'first-row') {
    captureMetadata.selectedPatientId = patientIds[0];
    return patientIds[0];
  }

  let bestCandidate = { id: patientIds[0], score: -1, stats: null };
  for (const id of patientIds) {
    const [obs, cond, proc, results] = await Promise.all([
      fetchJson(`${backendUrl}/fhir/Observation?patient=${id}&_summary=count&_count=0`, { headers: authHeaders(token) }),
      fetchJson(`${backendUrl}/fhir/Condition?patient=${id}&_summary=count&_count=0`, { headers: authHeaders(token) }),
      fetchJson(`${backendUrl}/fhir/Procedure?patient=${id}&_summary=count&_count=0`, { headers: authHeaders(token) }),
      fetchJson(`${backendUrl}/quality-measure/results?patient=${id}&page=0&size=5`, { headers: authHeaders(token) }),
    ]);

    const stats = {
      observations: responseCount(obs.json),
      conditions: responseCount(cond.json),
      procedures: responseCount(proc.json),
      results: responseCount(results.json),
    };

    const meetsThresholds = stats.observations >= minPatientObservations && stats.results >= minPatientResults;
    const score = (stats.observations * 2) + stats.conditions + stats.procedures + (stats.results * 3);

    if (meetsThresholds) {
      captureMetadata.selectedPatientId = id;
      captureMetadata.selectedPatientStats = stats;
      return id;
    }
    if (score > bestCandidate.score) {
      bestCandidate = { id, score, stats };
    }
  }

  captureMetadata.selectedPatientId = bestCandidate.id;
  captureMetadata.selectedPatientStats = bestCandidate.stats;
  return bestCandidate.id;
}

async function seedAuth(context, loginData) {
  const data = loginData;
  const urlHost = new URL(baseUrl).hostname;
  await context.addCookies([
    {
      name: 'hdim_access_token',
      value: data.accessToken,
      domain: urlHost,
      path: '/api',
      httpOnly: true,
      sameSite: 'Strict',
    },
    {
      name: 'hdim_refresh_token',
      value: data.refreshToken,
      domain: urlHost,
      path: '/api/v1/auth',
      httpOnly: true,
      sameSite: 'Strict',
    },
  ]);

  const user = {
    id: '',
    username: data.username,
    email: data.email,
    firstName: data.username.split('@')[0],
    lastName: '',
    fullName: data.username,
    roles: (data.roleDetails || data.roles || []).map((role) =>
      typeof role === 'string'
        ? { id: '', name: role }
        : { id: role.id || '', name: role.name }
    ),
    tenantId: data.tenantIds?.[0] || tenantId,
    tenantIds: data.tenantIds || [],
    active: true,
  };

  await context.addInitScript(({ userProfile, tenant }) => {
    localStorage.setItem('healthdata_user', JSON.stringify(userProfile));
    localStorage.setItem('healthdata_tenant', tenant);
  }, { userProfile: user, tenant: user.tenantId || tenantId });
}

const capture = async (page, name) => {
  const filePath = path.join(outputDir, name);
  try {
    await page.screenshot({ path: filePath, fullPage: false, timeout: screenshotTimeoutMs });
  } catch (firstError) {
    if (firstError?.name !== 'TimeoutError') {
      console.warn(`Page screenshot failed for ${name}: ${String(firstError)}`);
      return;
    }
    console.warn(`Page screenshot timed out for ${name}; retrying with body locator screenshot.`);
    try {
      await page.locator('body').first().screenshot({ path: filePath, timeout: 15000 });
    } catch (secondError) {
      console.warn(`Body screenshot fallback failed for ${name}: ${String(secondError)}`);
      return;
    }
  }
  console.log(`Saved ${filePath}`);
};

const capturePatientDetailScreenshots = async (page) => {
  const detailRoot = page.locator('.patient-detail-container, app-patient-detail').first();
  if (await detailRoot.isVisible().catch(() => false)) {
    await detailRoot.screenshot({
      path: path.join(outputDir, '07-patient-detail-summary.png'),
      timeout: screenshotTimeoutMs,
    });
    console.log(`Saved ${path.join(outputDir, '07-patient-detail-summary.png')}`);
  } else {
    await capture(page, '07-patient-detail-summary.png');
  }

  const careGapsHeader = page.locator('text=Care Gaps').first();
  if (await careGapsHeader.isVisible().catch(() => false)) {
    await careGapsHeader.scrollIntoViewIfNeeded();
    await waitForAppIdle(page);
    if (await detailRoot.isVisible().catch(() => false)) {
      await detailRoot.screenshot({
        path: path.join(outputDir, '08-patient-detail-care-gaps.png'),
        timeout: screenshotTimeoutMs,
      });
      console.log(`Saved ${path.join(outputDir, '08-patient-detail-care-gaps.png')}`);
    } else {
      await capture(page, '08-patient-detail-care-gaps.png');
    }
  }
};

const tryCapturePatientDetailFromRow = async (page) => {
  const patientRow = page.locator('table.patients-table tbody tr').first();
  if (!await patientRow.isVisible().catch(() => false)) {
    return false;
  }

  await patientRow.scrollIntoViewIfNeeded();
  await patientRow.click();
  await waitForPatientDetailReady(page);
  await capturePatientDetailScreenshots(page);
  return true;
};

const capturePatientSidePanelFallback = async (page) => {
  await page.goto(`${baseUrl}/patients`, { waitUntil: 'domcontentloaded', timeout: 20000 });
  await page.waitForSelector('table.patients-table tbody tr', { timeout: 20000 });
  const quickViewButton = page.locator('button[aria-label^="Quick view for"]').first();
  if (!await quickViewButton.isVisible().catch(() => false)) {
    return false;
  }

  await quickViewButton.click();
  await page.waitForSelector('.details-panel', { timeout: 15000 });
  const panel = page.locator('.details-panel').first();

  await panel.screenshot({
    path: path.join(outputDir, '07-patient-detail-summary.png'),
    timeout: screenshotTimeoutMs,
  });
  console.log(`Saved ${path.join(outputDir, '07-patient-detail-summary.png')}`);

  const panelContent = page.locator('.details-panel .details-content').first();
  if (await panelContent.isVisible().catch(() => false)) {
    await panelContent.evaluate((el) => {
      el.scrollTo({ top: el.scrollHeight, behavior: 'auto' });
    }).catch(() => {});
    await waitForAppIdle(page);
  }

  await panel.screenshot({
    path: path.join(outputDir, '08-patient-detail-care-gaps.png'),
    timeout: screenshotTimeoutMs,
  });
  console.log(`Saved ${path.join(outputDir, '08-patient-detail-care-gaps.png')}`);

  return true;
};

const withTimeout = async (promise, timeoutMs) => {
  let timeoutHandle;
  try {
    return await Promise.race([
      promise,
      new Promise((resolve) => {
        timeoutHandle = setTimeout(() => resolve('timeout'), timeoutMs);
      }),
    ]);
  } finally {
    if (timeoutHandle) {
      clearTimeout(timeoutHandle);
    }
  }
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

  const loginData = await loginDemoUser();
  await ensureResultsData(loginData.accessToken);
  const resolvedPatientId = await resolvePatientForCapture(loginData.accessToken);
  if (!captureMetadata.selectedPatientId) {
    captureMetadata.selectedPatientId = resolvedPatientId;
  }

  const browser = await chromium.launch();
  const desktop = await browser.newContext({ viewport: viewPort });
  await seedAuth(desktop, loginData);
  let page = await desktop.newPage();

  // Login screen
  await page.goto(`${baseUrl}/login`, { waitUntil: 'domcontentloaded' });
  await page.evaluate(() => {
    localStorage.setItem('provider-dashboard-tour:completed', 'true');
    localStorage.setItem('hdim-tour-first-visit', 'false');
  });
  await page.waitForSelector('form', { timeout: 15000 });
  await waitForAppIdle(page);
  await capture(page, '01-login.png');

  // Log in only when still on login route
  const isLoginRoute = page.url().includes('/login');
  if (isLoginRoute) {
    const demoLoginButton = page.getByRole('button', { name: 'Demo Login' });
    if (await demoLoginButton.isVisible().catch(() => false)) {
      await demoLoginButton.click();
    } else {
      const usernameField = page.getByRole('textbox', { name: /username/i }).first();
      const passwordField = page.getByRole('textbox', { name: /password/i }).first();
      const signInButton = page.getByRole('button', { name: /sign in/i }).first();
      await usernameField.fill(username);
      await passwordField.fill(password);
      await signInButton.click();
    }
    await page.waitForURL((url) => !url.pathname.endsWith('/login'), { timeout: 40000 });
  }
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

  const patientDetailResult = await withTimeout((async () => {
    const viewDetailButton = page.locator('button[aria-label^="View full details"]').first();
    let patientDetailCaptured = false;

    if (!forceDirectPatientDetail && await viewDetailButton.isVisible()) {
      await viewDetailButton.click();
      await waitForPatientDetailReady(page);
      await capturePatientDetailScreenshots(page);
      patientDetailCaptured = true;
    }

    if (!patientDetailCaptured && !forceDirectPatientDetail) {
      console.warn('View detail button missing; trying to activate patient row instead.');
      patientDetailCaptured = await tryCapturePatientDetailFromRow(page);
    }

    if (!patientDetailCaptured) {
      console.warn('Unable to open patient detail via UI; navigating directly to patient detail.');
      await page.goto(`${baseUrl}/patients/${resolvedPatientId}`, { waitUntil: 'domcontentloaded' });
      await page.waitForSelector('text=Patient Detail', { timeout: 15000 }).catch(() => {});
      await waitForPatientDetailReady(page);
      await capturePatientDetailScreenshots(page);
    }

    return 'ok';
  })(), patientDetailStepTimeoutMs);

  let patientDetailDegraded = false;
  if (patientDetailResult === 'timeout') {
    patientDetailDegraded = true;
    console.warn(`Patient detail capture exceeded ${patientDetailStepTimeoutMs}ms; using fallback screenshots.`);
    const sidePanelCaptured = await capturePatientSidePanelFallback(page).catch(() => false);
    if (!sidePanelCaptured) {
      await capture(page, '07-patient-detail-summary.png');
      await capture(page, '08-patient-detail-care-gaps.png');
    }
  }

  if (patientDetailDegraded) {
    // Reset to a clean page after patient-detail timeouts to avoid navigation deadlocks.
    await page.close().catch(() => {});
    page = await desktop.newPage();
    await page.goto(`${baseUrl}/patients`, { waitUntil: 'domcontentloaded', timeout: 15000 }).catch(() => {});
  }

  // Results
  await navigateAndCapture(page, '/results', '09-results.png', 'text=Results');
  await waitForRows(page, 'table mat-row, table tbody tr').catch(() => {});
  await waitForAppIdle(page);
  await capture(page, '09-results.png');

  if (stopAfterResults) {
    const metadataPath = path.join(outputDir, 'capture-metadata.json');
    await writeFile(metadataPath, JSON.stringify(captureMetadata, null, 2));
    console.log(`Saved ${metadataPath}`);
    await browser.close();
    return;
  }

  // Evaluations
  await navigateAndCapture(page, '/evaluations', '10-evaluations.png', 'text=Evaluations');
  await waitForRows(page, 'table mat-row, table tbody tr').catch(() => {});
  await waitForAppIdle(page);
  await capture(page, '10-evaluations.png');

  // Reports
  await navigateAndCapture(page, '/reports', '11-reports.png', 'text=Reports');

  const metadataPath = path.join(outputDir, 'capture-metadata.json');
  await writeFile(metadataPath, JSON.stringify(captureMetadata, null, 2));
  console.log(`Saved ${metadataPath}`);

  await browser.close();
};

run().catch((error) => {
  console.error('Screenshot capture failed:', error);
  process.exit(1);
});
