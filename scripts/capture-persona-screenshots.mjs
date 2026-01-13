#!/usr/bin/env node
import { chromium } from 'playwright';
import { dirname, join } from 'path';
import { fileURLToPath } from 'url';
import { mkdir } from 'fs/promises';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const BASE_URL = 'https://hdim-landing-page.vercel.app/explorer';
const OUTPUT_DIR = join(__dirname, '../landing-page-v0/public/images/personas');

// Persona-specific screenshot configurations
const personaConfigurations = [
  {
    folder: 'medical-assistant',
    title: 'Medical Assistant',
    screenshots: [
      { name: 'patient-scheduling', scroll: 0, description: 'Patient scheduling and care coordination dashboard' },
      { name: 'care-gap-tracking', scroll: 800, description: 'Care gap identification and tracking' },
      { name: 'task-management', scroll: 1200, description: 'Daily task management and patient outreach' },
      { name: 'appointment-prep', scroll: 1600, description: 'Appointment preparation and chart review' }
    ]
  },
  {
    folder: 'nurse-practitioner',
    title: 'Nurse Practitioner',
    screenshots: [
      { name: 'clinical-dashboard', scroll: 0, description: 'Clinical quality metrics dashboard' },
      { name: 'measure-performance', scroll: 600, description: 'HEDIS measure performance tracking' },
      { name: 'patient-interventions', scroll: 1000, description: 'Patient intervention workflows' },
      { name: 'quality-reporting', scroll: 1400, description: 'Quality measure reporting and analytics' }
    ]
  },
  {
    folder: 'provider',
    title: 'Provider/Physician',
    screenshots: [
      { name: 'patient-overview', scroll: 200, description: 'Comprehensive patient care overview' },
      { name: 'clinical-decision-support', scroll: 700, description: 'Clinical decision support and recommendations' },
      { name: 'measure-compliance', scroll: 1100, description: 'Quality measure compliance tracking' },
      { name: 'population-analytics', scroll: 1500, description: 'Population health analytics' }
    ]
  },
  {
    folder: 'admin',
    title: 'Administrator',
    screenshots: [
      { name: 'system-dashboard', scroll: 0, description: 'System performance and metrics dashboard' },
      { name: 'reporting-analytics', scroll: 500, description: 'Executive reporting and analytics' },
      { name: 'compliance-monitoring', scroll: 900, description: 'Compliance and audit monitoring' },
      { name: 'configuration-management', scroll: 1300, description: 'System configuration and user management' }
    ]
  }
];

async function capturePersonaScreenshots() {
  console.log('🚀 Starting persona screenshot capture from live explorer...\n');
  
  // Create output directory
  await mkdir(OUTPUT_DIR, { recursive: true });
  
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 },
    deviceScaleFactor: 2
  });
  
  const page = await context.newPage();

  try {
    console.log('⏳ Loading explorer page...');
    await page.goto(BASE_URL, { waitUntil: 'networkidle', timeout: 60000 });
    await page.waitForTimeout(5000);

    for (const persona of personaConfigurations) {
      console.log(`\n📸 Capturing screenshots for: ${persona.title}`);
      console.log(`   Folder: ${persona.folder}\n`);
      
      // Create persona directory
      const personaDir = join(OUTPUT_DIR, persona.folder);
      await mkdir(personaDir, { recursive: true });
      
      for (const screenshot of persona.screenshots) {
        console.log(`  → ${screenshot.name}: ${screenshot.description}`);
        
        // Scroll to position
        await page.evaluate((scrollY) => window.scrollTo(0, scrollY), screenshot.scroll);
        await page.waitForTimeout(2000);
        
        // Capture screenshot
        await page.screenshot({
          path: join(personaDir, `${screenshot.name}.png`),
          fullPage: false
        });
      }
      
      // Reset scroll position for next persona
      await page.evaluate(() => window.scrollTo(0, 0));
      await page.waitForTimeout(1000);
    }

    console.log('\n✅ All persona screenshots captured successfully!');
    console.log(`📁 Saved to: ${OUTPUT_DIR}\n`);
    
    // Summary
    console.log('📊 Summary:');
    for (const persona of personaConfigurations) {
      console.log(`   ${persona.title}: ${persona.screenshots.length} screenshots`);
    }
    
  } catch (error) {
    console.error('\n❌ Error capturing screenshots:', error.message);
    throw error;
  } finally {
    await browser.close();
  }
}

capturePersonaScreenshots().catch(console.error);
