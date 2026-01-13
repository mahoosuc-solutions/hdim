#!/usr/bin/env node
import { chromium } from 'playwright';
import { dirname, join } from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const BASE_URL = 'https://hdim-landing-page.vercel.app/explorer';
const OUTPUT_DIR = join(__dirname, '../landing-page-v0/public/images/dashboard');

async function captureScreenshots() {
  console.log('🚀 Starting screenshot capture from live explorer...');
  
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 },
    deviceScaleFactor: 2 // Retina display
  });
  
  const page = await context.newPage();

  try {
    // Capture main explorer dashboard
    console.log('📸 Capturing main explorer dashboard...');
    await page.goto(BASE_URL, { waitUntil: 'networkidle', timeout: 60000 });
    await page.waitForTimeout(5000); // Wait for any animations
    
    await page.screenshot({
      path: join(OUTPUT_DIR, 'main.png'),
      fullPage: false
    });

    // Capture different sections by scrolling
    console.log('📸 Capturing quality measures view...');
    await page.evaluate(() => window.scrollTo(0, 400));
    await page.waitForTimeout(2000);
    await page.screenshot({
      path: join(OUTPUT_DIR, 'measures.png'),
      fullPage: false
    });

    // Capture care gaps section
    console.log('📸 Capturing care gaps section...');
    await page.evaluate(() => window.scrollTo(0, 800));
    await page.waitForTimeout(2000);
    await page.screenshot({
      path: join(OUTPUT_DIR, 'care-gaps.png'),
      fullPage: false
    });

    // Capture mobile view
    console.log('📱 Capturing mobile view...');
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto(BASE_URL, { waitUntil: 'networkidle' });
    await page.waitForTimeout(3000);
    await page.screenshot({
      path: join(OUTPUT_DIR, 'mobile.png'),
      fullPage: true
    });

    console.log('✅ Screenshots captured successfully!');
    console.log(`📁 Saved to: ${OUTPUT_DIR}`);
  } catch (error) {
    console.error('❌ Error capturing screenshots:', error.message);
    throw error;
  } finally {
    await browser.close();
  }
}

captureScreenshots().catch(console.error);
