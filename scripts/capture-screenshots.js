/**
 * Automated Screenshot Capture Script
 * Uses Playwright to capture screenshots for all user types and scenarios
 */

const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

// Configuration
const CONFIG = {
  screenshotDir: path.join(__dirname, '..', 'docs', 'screenshots'),
  viewport: { width: 1920, height: 1080 },
  timeout: 30000,
  waitForNetworkIdle: true,
};

// User scenarios
const SCENARIOS = [
  {
    userType: 'care-manager',
    credentials: {
      email: 'care.manager@demo.com',
      password: 'Demo2026!',
    },
    baseUrl: 'http://localhost:3000',
    pages: [
      { path: '/', name: 'login', preAuth: true },
      { path: '/dashboard', name: 'dashboard-overview', wait: 2000 },
      { path: '/patients', name: 'patient-list', wait: 2000 },
      { path: '/care-gaps', name: 'care-gaps-overview', wait: 2000 },
      { path: '/care-gaps/pat-001', name: 'care-gap-detail', wait: 2000 },
      { path: '/patients/pat-001', name: 'patient-detail', wait: 2000 },
      { path: '/quality-measures', name: 'quality-measures', wait: 2000 },
      { path: '/analytics', name: 'analytics-dashboard', wait: 2000 },
      { path: '/reports', name: 'reports', wait: 2000 },
      { path: '/settings', name: 'settings', wait: 1000 },
    ],
  },
  {
    userType: 'physician',
    credentials: {
      email: 'dr.smith@demo.com',
      password: 'Demo2026!',
    },
    baseUrl: 'http://localhost:3000',
    pages: [
      { path: '/', name: 'login', preAuth: true },
      { path: '/dashboard', name: 'clinical-dashboard', wait: 2000 },
      { path: '/patients', name: 'patient-search', wait: 2000 },
      { path: '/patients/pat-002', name: 'patient-clinical-summary', wait: 2000 },
      { path: '/patients/pat-002/vitals', name: 'patient-vitals', wait: 2000 },
      { path: '/patients/pat-002/medications', name: 'patient-medications', wait: 2000 },
      { path: '/ai-assistant', name: 'ai-assistant-panel', wait: 2000 },
      { path: '/cql-results', name: 'cql-evaluation-results', wait: 2000 },
    ],
  },
  {
    userType: 'admin',
    credentials: {
      email: 'admin@demo.com',
      password: 'Demo2026!',
    },
    baseUrl: 'http://localhost:3001',
    pages: [
      { path: '/', name: 'login', preAuth: true },
      { path: '/dashboard', name: 'admin-dashboard', wait: 2000 },
      { path: '/users', name: 'user-management', wait: 2000 },
      { path: '/users/create', name: 'create-user', wait: 1000 },
      { path: '/roles', name: 'role-management', wait: 2000 },
      { path: '/audit-logs', name: 'audit-log-viewer', wait: 2000 },
      { path: '/integrations', name: 'integration-status', wait: 2000 },
      { path: '/system-health', name: 'system-health-monitor', wait: 2000 },
      { path: '/configuration', name: 'system-configuration', wait: 2000 },
      { path: '/tenants', name: 'tenant-management', wait: 2000 },
    ],
  },
  {
    userType: 'ai-user',
    credentials: {
      email: 'ai.user@demo.com',
      password: 'Demo2026!',
    },
    baseUrl: 'http://localhost:3002',
    pages: [
      { path: '/', name: 'login', preAuth: true },
      { path: '/chat', name: 'ai-chat-interface', wait: 2000 },
      { path: '/agents', name: 'agent-selection', wait: 2000 },
      { path: '/tools', name: 'tool-library', wait: 2000 },
      { path: '/history', name: 'conversation-history', wait: 2000 },
      { path: '/audit', name: 'decision-audit-trail', wait: 2000 },
    ],
  },
  {
    userType: 'patient',
    credentials: {
      email: 'patient@demo.com',
      password: 'Demo2026!',
    },
    baseUrl: 'http://localhost:3003',
    pages: [
      { path: '/', name: 'login', preAuth: true },
      { path: '/home', name: 'patient-home', wait: 2000 },
      { path: '/health-summary', name: 'health-summary', wait: 2000 },
      { path: '/care-gaps', name: 'my-care-gaps', wait: 2000 },
      { path: '/appointments', name: 'appointments', wait: 2000 },
      { path: '/messages', name: 'secure-messaging', wait: 2000 },
      { path: '/documents', name: 'my-documents', wait: 2000 },
    ],
  },
  {
    userType: 'quality-manager',
    credentials: {
      email: 'quality.manager@demo.com',
      password: 'Demo2026!',
    },
    baseUrl: 'http://localhost:3000',
    pages: [
      { path: '/', name: 'login', preAuth: true },
      { path: '/dashboard', name: 'quality-dashboard', wait: 2000 },
      { path: '/quality-measures', name: 'hedis-measures', wait: 2000 },
      { path: '/reports/quality', name: 'quality-reports', wait: 2000 },
      { path: '/gap-closure-tracking', name: 'gap-closure-tracking', wait: 2000 },
      { path: '/performance-trends', name: 'performance-trends', wait: 2000 },
    ],
  },
  {
    userType: 'data-analyst',
    credentials: {
      email: 'admin@demo.com', // Using admin for now
      password: 'Demo2026!',
    },
    baseUrl: 'http://localhost:3004',
    pages: [
      { path: '/', name: 'login', preAuth: true },
      { path: '/overview', name: 'analytics-overview', wait: 2000 },
      { path: '/population-health', name: 'population-analytics', wait: 2000 },
      { path: '/quality-metrics', name: 'quality-dashboards', wait: 2000 },
      { path: '/financial', name: 'financial-analytics', wait: 2000 },
      { path: '/reports', name: 'report-library', wait: 2000 },
      { path: '/custom-reports', name: 'report-builder', wait: 2000 },
    ],
  },
];

// Utility functions
function ensureDirectoryExists(dir) {
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }
}

function log(message, type = 'info') {
  const colors = {
    info: '\x1b[34m',    // Blue
    success: '\x1b[32m', // Green
    error: '\x1b[31m',   // Red
    warning: '\x1b[33m', // Yellow
  };
  const reset = '\x1b[0m';
  console.log(`${colors[type]}[${type.toUpperCase()}]${reset} ${message}`);
}

async function login(page, credentials, baseUrl) {
  try {
    await page.goto(`${baseUrl}/`);
    
    // Wait for login form
    await page.waitForSelector('[name="email"], input[type="email"]', { timeout: 5000 });
    
    // Fill credentials
    await page.fill('[name="email"], input[type="email"]', credentials.email);
    await page.fill('[name="password"], input[type="password"]', credentials.password);
    
    // Submit form
    await page.click('button[type="submit"]');
    
    // Wait for navigation
    await page.waitForNavigation({ waitUntil: 'networkidle', timeout: 10000 });
    
    log(`Logged in as ${credentials.email}`, 'success');
    return true;
  } catch (error) {
    log(`Login failed for ${credentials.email}: ${error.message}`, 'error');
    return false;
  }
}

async function captureScreenshot(page, scenario, pageInfo, outputDir) {
  try {
    const url = `${scenario.baseUrl}${pageInfo.path}`;
    
    // Navigate to page
    await page.goto(url, { waitUntil: 'networkidle', timeout: CONFIG.timeout });
    
    // Additional wait if specified
    if (pageInfo.wait) {
      await page.waitForTimeout(pageInfo.wait);
    }
    
    // Generate filename
    const filename = `${scenario.userType}-${pageInfo.name}.png`;
    const filepath = path.join(outputDir, filename);
    
    // Capture screenshot
    await page.screenshot({
      path: filepath,
      fullPage: true,
    });
    
    log(`Captured: ${filename}`, 'success');
    return true;
  } catch (error) {
    log(`Failed to capture ${pageInfo.name}: ${error.message}`, 'error');
    return false;
  }
}

async function captureScenario(browser, scenario) {
  log(`\n========================================`, 'info');
  log(`Capturing screenshots for: ${scenario.userType}`, 'info');
  log(`========================================`, 'info');
  
  const context = await browser.newContext({
    viewport: CONFIG.viewport,
    userAgent: 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
  });
  
  const page = await context.newPage();
  
  // Create output directory for this user type
  const outputDir = path.join(CONFIG.screenshotDir, scenario.userType);
  ensureDirectoryExists(outputDir);
  
  let captured = 0;
  let failed = 0;
  
  for (const pageInfo of scenario.pages) {
    // If this is the login page or pre-auth, capture before login
    if (pageInfo.preAuth) {
      if (await captureScreenshot(page, scenario, pageInfo, outputDir)) {
        captured++;
      } else {
        failed++;
      }
      
      // Now perform login
      if (!await login(page, scenario.credentials, scenario.baseUrl)) {
        log(`Cannot proceed with ${scenario.userType} - login failed`, 'error');
        break;
      }
    } else {
      // Regular page capture (already authenticated)
      if (await captureScreenshot(page, scenario, pageInfo, outputDir)) {
        captured++;
      } else {
        failed++;
      }
    }
  }
  
  await context.close();
  
  log(`\nCompleted ${scenario.userType}: ${captured} captured, ${failed} failed`, 
    failed === 0 ? 'success' : 'warning');
  
  return { captured, failed };
}

async function generateIndex(scenarios, stats) {
  const indexPath = path.join(CONFIG.screenshotDir, 'INDEX.md');
  
  let content = '# Screenshot Index\n\n';
  content += `Generated: ${new Date().toISOString()}\n\n`;
  content += `Total Screenshots: ${stats.totalCaptured}\n`;
  content += `Failed: ${stats.totalFailed}\n\n`;
  
  content += '## User Types\n\n';
  
  for (const scenario of scenarios) {
    content += `### ${scenario.userType}\n\n`;
    content += `**Base URL**: ${scenario.baseUrl}\n\n`;
    content += `**Pages**:\n\n`;
    
    for (const page of scenario.pages) {
      const filename = `${scenario.userType}/${scenario.userType}-${page.name}.png`;
      content += `- [${page.name}](${filename}) - ${page.path}\n`;
    }
    
    content += '\n';
  }
  
  fs.writeFileSync(indexPath, content);
  log(`Generated index at ${indexPath}`, 'success');
}

async function main() {
  log('========================================', 'info');
  log('Automated Screenshot Capture', 'info');
  log('========================================', 'info');
  
  // Ensure screenshot directory exists
  ensureDirectoryExists(CONFIG.screenshotDir);
  
  // Launch browser
  const browser = await chromium.launch({
    headless: true,
    args: ['--disable-dev-shm-usage', '--no-sandbox'],
  });
  
  const stats = {
    totalCaptured: 0,
    totalFailed: 0,
  };
  
  try {
    // Capture screenshots for each scenario
    for (const scenario of SCENARIOS) {
      const result = await captureScenario(browser, scenario);
      stats.totalCaptured += result.captured;
      stats.totalFailed += result.failed;
    }
    
    // Generate index
    await generateIndex(SCENARIOS, stats);
    
    log('\n========================================', 'info');
    log('Screenshot Capture Complete', 'success');
    log('========================================', 'info');
    log(`Total Captured: ${stats.totalCaptured}`, 'success');
    log(`Total Failed: ${stats.totalFailed}`, stats.totalFailed === 0 ? 'success' : 'error');
    log(`Output Directory: ${CONFIG.screenshotDir}`, 'info');
    
  } catch (error) {
    log(`Fatal error: ${error.message}`, 'error');
    process.exit(1);
  } finally {
    await browser.close();
  }
}

// Run main function
main().catch(error => {
  log(`Unhandled error: ${error.message}`, 'error');
  process.exit(1);
});
