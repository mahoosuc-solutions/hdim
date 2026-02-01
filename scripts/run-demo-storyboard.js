#!/usr/bin/env node
/**
 * Automated demo walkthrough with storyboard overlays.
 * Uses Playwright to log in, navigate key pages, and capture screenshots.
 */

const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const BASE_URL = process.env.DEMO_BASE_URL || 'http://localhost:4200';
const USERNAME = process.env.DEMO_USER || 'demo_admin';
const PASSWORD = process.env.DEMO_PASSWORD || 'demo123';
const OUTPUT_DIR =
  process.env.DEMO_STORYBOARD_DIR ||
  path.join(__dirname, '..', 'docs', 'screenshots', 'storyboard');

const VIEWPORT = { width: 1920, height: 1080 };
const PAUSE_MS = 1200;

const steps = [
  {
    id: 'dashboard',
    title: 'Provider Dashboard',
    body:
      'A real-time view of scheduled patients, priority gaps, and care actions.',
    path: '/dashboard',
    waitFor: '.metrics-grid, .statistics-grid, .stats-grid',
  },
  {
    id: 'care-gaps',
    title: 'Care Gaps',
    body:
      'Identify care opportunities, prioritize outreach, and track closure status.',
    path: '/care-gaps',
    waitFor: '.care-gaps-list, .care-gap-item, table',
  },
  {
    id: 'results',
    title: 'Quality Results',
    body:
      'Measure performance, validate improvements, and surface impact by cohort.',
    path: '/results',
    waitFor: 'table, .results-table, mat-table',
  },
  {
    id: 'quality-measures',
    title: 'Quality Measures',
    body:
      'Active measures and evaluation status keep your quality program on track.',
    path: '/quality-measures',
    waitFor: 'table, .measure-card, mat-table',
  },
  {
    id: 'live-monitor',
    title: 'Live Monitor',
    body:
      'Watch evaluation activity in real time to prove the system is working end-to-end.',
    path: '/visualization/live-monitor',
    waitFor: 'canvas, svg, .monitor, .chart',
  },
  {
    id: 'ai-assistant',
    title: 'AI Solutioning',
    body:
      'AI-guided workflows highlight gaps, recommend actions, and explain impact.',
    path: '/ai-assistant',
    waitFor: 'app-ai-dashboard, .ai-dashboard, .assistant-panel',
  },
];

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

async function injectStoryCard(page, title, body, stepIndex, totalSteps) {
  await page.evaluate(
    ({ title, body, stepIndex, totalSteps }) => {
      const existing = document.getElementById('demo-story-card');
      if (existing) existing.remove();

      const card = document.createElement('div');
      card.id = 'demo-story-card';
      card.style.position = 'fixed';
      card.style.right = '32px';
      card.style.bottom = '32px';
      card.style.width = '360px';
      card.style.padding = '18px 20px';
      card.style.background = 'rgba(15, 23, 42, 0.92)';
      card.style.color = '#f8fafc';
      card.style.border = '1px solid rgba(148, 163, 184, 0.2)';
      card.style.borderRadius = '16px';
      card.style.boxShadow = '0 18px 40px rgba(15, 23, 42, 0.45)';
      card.style.zIndex = '99999';
      card.style.fontFamily =
        "'Space Grotesk', 'Segoe UI', Arial, sans-serif";

      const label = document.createElement('div');
      label.textContent = `Step ${stepIndex + 1} of ${totalSteps}`;
      label.style.fontSize = '12px';
      label.style.letterSpacing = '0.08em';
      label.style.textTransform = 'uppercase';
      label.style.color = '#94a3b8';

      const headline = document.createElement('div');
      headline.textContent = title;
      headline.style.fontSize = '18px';
      headline.style.fontWeight = '600';
      headline.style.margin = '8px 0 10px';

      const text = document.createElement('div');
      text.textContent = body;
      text.style.fontSize = '14px';
      text.style.lineHeight = '1.5';
      text.style.color = '#e2e8f0';

      card.appendChild(label);
      card.appendChild(headline);
      card.appendChild(text);
      document.body.appendChild(card);
    },
    { title, body, stepIndex, totalSteps }
  );
}

async function clearStoryCard(page) {
  await page.evaluate(() => {
    const existing = document.getElementById('demo-story-card');
    if (existing) existing.remove();
  });
}

async function waitForAny(page, selectorList, timeoutMs = 30000) {
  const selectors = selectorList
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
  for (const selector of selectors) {
    try {
      await page.waitForSelector(selector, { timeout: timeoutMs });
      return selector;
    } catch (err) {
      // Try next selector
    }
  }
  return null;
}

async function run() {
  ensureDir(OUTPUT_DIR);

  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: VIEWPORT });

  try {
    await page.goto(`${BASE_URL}/login?demo=true&storyboard=1`, { waitUntil: 'domcontentloaded' });

    const demoButton = page.locator('.demo-button');
    if (await demoButton.count()) {
      await demoButton.first().click();
    } else {
      await page.fill('input[formcontrolname="username"]', USERNAME);
      await page.fill('input[formcontrolname="password"]', PASSWORD);
      await page.click('button[type="submit"]');
    }

    await page.waitForURL('**/dashboard', { timeout: 60000 });
    await page.waitForTimeout(PAUSE_MS);

    await page.screenshot({
      path: path.join(OUTPUT_DIR, '00-login-success.png'),
      fullPage: true,
    });

    const tourButton = page.locator('button[mattooltip="Take a Tour"]');
    if (await tourButton.count()) {
      await tourButton.first().click();
      await page.waitForSelector('.tour-overlay', { timeout: 15000 });

      let stepIndex = 0;
      while (true) {
        await page.waitForTimeout(700);
        await page.screenshot({
          path: path.join(OUTPUT_DIR, `tour-step-${stepIndex + 1}.png`),
          fullPage: true,
        });

        const nextButton = page.getByRole('button', { name: /next/i });
        if (await nextButton.count()) {
          await nextButton.click();
          stepIndex += 1;
          continue;
        }

        const doneButton = page.getByRole('button', { name: /done/i });
        if (await doneButton.count()) {
          await doneButton.click();
        }
        break;
      }
    }

    for (let i = 0; i < steps.length; i += 1) {
      const step = steps[i];
      await page.goto(`${BASE_URL}${step.path}`, { waitUntil: 'domcontentloaded' });
      await waitForAny(page, step.waitFor);
      await page.waitForTimeout(PAUSE_MS);
      await injectStoryCard(page, step.title, step.body, i, steps.length);
      await page.waitForTimeout(700);
      await page.screenshot({
        path: path.join(OUTPUT_DIR, `${String(i + 1).padStart(2, '0')}-${step.id}.png`),
        fullPage: true,
      });
      await clearStoryCard(page);
    }
  } finally {
    await browser.close();
  }

  console.log(`Storyboard captured in: ${OUTPUT_DIR}`);
}

run().catch((error) => {
  console.error(error);
  process.exit(1);
});
