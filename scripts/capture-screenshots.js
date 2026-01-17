/**
 * Automated Screenshot Capture Script
 * Uses Playwright to capture screenshots for all user types and scenarios
 */

const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

// Parse command-line arguments
function getArg(flag, defaultValue) {
  const args = process.argv.slice(2);
  const index = args.indexOf(flag);
  return index !== -1 && args[index + 1] 
    ? args[index + 1] 
    : defaultValue;
}

const PHASE = getArg('--phase', 'AFTER'); // BEFORE, DURING, AFTER
const SCENARIO_ID = getArg('--scenario', null);
const USER_TYPE = getArg('--user-type', null);
const CUSTOM_OUTPUT_DIR = getArg('--output-dir', null);
const SKIP_ANALYTICS = process.env.SKIP_ANALYTICS === '1';
const API_DEFAULT_TENANT = 'acme-health';

// Configuration
const CONFIG = {
  screenshotDir: CUSTOM_OUTPUT_DIR || path.join(__dirname, '..', 'docs', 'screenshots'),
  viewport: { width: 1920, height: 1080 },
  timeout: 60000, // Increased timeout for data loading
  waitForNetworkIdle: true,
  baseUrl: 'http://localhost:4200', // Clinical Portal URL from docker-compose
  validateData: true, // Validate that pages contain data before capturing
  // Data loading detection
  waitForDataLoad: true,
  dataLoadTimeout: 30000, // 30 seconds max wait for data
  monitorNetworkRequests: true,
  monitorDOMState: true,
  // Region-based capture
  captureRegions: true, // Capture region screenshots in addition to full-page
  // Phase-based capture
  phase: PHASE,
  scenarioId: SCENARIO_ID,
  userType: USER_TYPE,
};

// API endpoints to monitor for data loading
const DATA_API_PATTERNS = {
  dashboard: [
    '/patient/api/v1/patients/summary',
    '/quality-measure/api/v1/measures/active',
    '/cql-engine/api/v1/evaluations'
  ],
  patients: ['/patient/api/v1/patients'],
  'care-gaps': ['/care-gap/api/v1/care-gaps'],
  'quality-measures': ['/quality-measure/api/v1/measures'],
  results: ['/quality-measure/api/v1/results'],
  analytics: [
    '/patient/api/v1/patients/summary',
    '/quality-measure/api/v1/results'
  ],
  generic: [] // No specific patterns for generic pages
};

// Page regions to capture and validate
const PAGE_REGIONS = {
  dashboard: [
    {
      name: 'top-stats',
      selector: '.statistics-grid, app-stat-card, .metrics-grid, .stats-grid',
      priority: 'high',
      validation: {
        minElements: 4,
        dataCheck: async (elements) => {
          // Check stat cards have numeric values > 0
          for (const el of elements.slice(0, 10)) {
            try {
              const text = await el.textContent();
              const numbers = text.match(/\d+/g);
              if (numbers && numbers.some(n => parseInt(n) > 0)) {
                return true;
              }
            } catch (e) {
              // Continue to next element
            }
          }
          return false;
        }
      }
    },
    {
      name: 'care-gaps',
      selector: '.care-gaps-section, .care-gaps-card, .care-gaps-list, .care-gap-item',
      priority: 'high',
      validation: {
        minElements: 1,
        dataCheck: async (elements) => {
          // Check for care gap items or empty state message
          if (elements.length === 0) return false;
          // Check at least one element has meaningful content
          for (const el of elements.slice(0, 5)) {
            try {
              const text = await el.textContent();
              if (text.trim().length > 10) return true;
            } catch (e) {
              // Continue
            }
          }
          return false;
        }
      }
    },
    {
      name: 'charts',
      selector: 'canvas, svg, [class*="chart"], .compliance-trend-chart',
      priority: 'medium',
      validation: {
        minElements: 1,
        dataCheck: async (elements) => {
          // Charts should be visible and have dimensions
          for (const el of elements.slice(0, 5)) {
            try {
              const box = await el.boundingBox();
              if (box && box.width > 50 && box.height > 50) {
                return true;
              }
            } catch (e) {
              // Continue
            }
          }
          return false;
        }
      }
    },
    {
      name: 'tables',
      selector: 'table tbody tr, mat-table tbody tr, .recent-activity-table tbody tr',
      priority: 'medium',
      validation: {
        minElements: 1,
        dataCheck: async (elements) => {
          // At least one table row with content
          for (const el of elements.slice(0, 10)) {
            try {
              const text = await el.textContent();
              if (text.trim().length > 10) return true;
            } catch (e) {
              // Continue
            }
          }
          return false;
        }
      }
    }
  ],
  patients: [
    {
      name: 'top-stats',
      selector: '.statistics-row, .stat-card, .page-header .stat-card',
      priority: 'high',
      validation: {
        minElements: 4,
        dataCheck: async (elements) => {
          for (const el of elements.slice(0, 10)) {
            try {
              const text = await el.textContent();
              if (/\d+/.test(text)) return true;
            } catch (e) {
              // Continue
            }
          }
          return false;
        }
      }
    },
    {
      name: 'patient-table',
      selector: 'table tbody tr, mat-table tbody tr, .patient-list tbody tr',
      priority: 'high',
      validation: {
        minElements: 5,
        dataCheck: async (elements) => {
          // Check rows have patient data (name, MRN, etc.)
          for (const el of elements.slice(0, 10)) {
            try {
              const text = await el.textContent();
              if (text.length > 20 && (text.includes('MRN') || /\d{3,}/.test(text))) {
                return true;
              }
            } catch (e) {
              // Continue
            }
          }
          return false;
        }
      }
    }
  ],
  'care-gaps': [
    {
      name: 'summary-stats',
      selector: '.care-gap-summary, .statistics-row, app-stat-card, .header-stats',
      priority: 'high',
      validation: {
        minElements: 2,
        dataCheck: async (elements) => {
          for (const el of elements.slice(0, 10)) {
            try {
              const text = await el.textContent();
              if (/\d+/.test(text)) return true;
            } catch (e) {
              // Continue
            }
          }
          return false;
        }
      }
    },
    {
      name: 'care-gap-list',
      selector: '.care-gaps-list, .care-gap-item, table tbody tr, mat-table tbody tr',
      priority: 'high',
      validation: {
        minElements: 3,
        dataCheck: async (elements) => {
          // Check for gap items or empty state
          if (elements.length === 0) return false;
          for (const el of elements.slice(0, 10)) {
            try {
              const text = await el.textContent();
              if (text.length > 10) return true;
            } catch (e) {
              // Continue
            }
          }
          return false;
        }
      }
    }
  ],
  'quality-measures': [
    {
      name: 'header-stats',
      selector: '.header-stats, .stat-card, .page-header .stat-card',
      priority: 'high',
      validation: {
        minElements: 2,
        dataCheck: async (elements) => {
          for (const el of elements.slice(0, 10)) {
            try {
              const text = await el.textContent();
              if (/\d+/.test(text)) return true;
            } catch (e) {
              // Continue
            }
          }
          return false;
        }
      }
    },
    {
      name: 'measures-grid',
      selector: '.measures-grid, .measure-card, table tbody tr, mat-table tbody tr',
      priority: 'high',
      validation: {
        minElements: 5,
        dataCheck: async (elements) => {
          // Check for measure items with content
          if (elements.length < 5) return false;
          let hasContent = 0;
          for (const el of elements.slice(0, 10)) {
            try {
              const text = await el.textContent();
              if (text.trim().length > 10) hasContent++;
            } catch (e) {
              // Continue
            }
          }
          return hasContent >= 5;
        }
      }
    }
  ],
  results: [
    {
      name: 'results-table',
      selector: 'table tbody tr, mat-table tbody tr, .results-list tbody tr',
      priority: 'high',
      validation: {
        minElements: 5,
        dataCheck: async (elements) => {
          // Check rows have result data
          for (const el of elements.slice(0, 10)) {
            try {
              const text = await el.textContent();
              if (text.length > 15) return true;
            } catch (e) {
              // Continue
            }
          }
          return false;
        }
      }
    }
  ],
  generic: [] // No specific regions for generic pages
};

// User scenarios
const SCENARIOS = [
  {
    userType: 'care-manager',
    credentials: {
      email: 'demo_admin@hdim.ai',
      password: 'demo123',
    },
    baseUrl: CONFIG.baseUrl,
    pages: [
      { path: '/', name: 'login', preAuth: true },
      { path: '/dashboard', name: 'dashboard-overview', wait: 2000 },
      { path: '/patients', name: 'patient-list', wait: 2000 },
      { path: '/care-gaps', name: 'care-gaps-overview', wait: 2000 },
      { path: '/care-gaps/pat-001', name: 'care-gap-detail', wait: 2000 },
      { path: '/patients/pat-001', name: 'patient-detail', wait: 2000 },
      { path: '/quality-measures', name: 'quality-measures', wait: 2000 },
      { path: '/results', name: 'results', wait: 2000 },
      { path: '/analytics', name: 'analytics-dashboard', wait: 2000 },
      { path: '/reports', name: 'reports', wait: 2000 },
      { path: '/settings', name: 'settings', wait: 1000 },
    ],
  },
  {
    userType: 'physician',
    credentials: {
      email: 'demo_analyst@hdim.ai',
      password: 'demo123',
    },
    baseUrl: CONFIG.baseUrl,
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
      email: 'demo_admin@hdim.ai',
      password: 'demo123',
    },
    baseUrl: CONFIG.baseUrl,
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
      email: 'demo_analyst@hdim.ai',
      password: 'demo123',
    },
    baseUrl: CONFIG.baseUrl,
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
      email: 'demo_viewer@hdim.ai',
      password: 'demo123',
    },
    baseUrl: CONFIG.baseUrl,
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
      email: 'demo_analyst@hdim.ai',
      password: 'demo123',
    },
    baseUrl: CONFIG.baseUrl,
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
      email: 'demo_analyst@hdim.ai',
      password: 'demo123',
    },
    baseUrl: CONFIG.baseUrl,
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

async function fetchPatientIds() {
  try {
    const response = await fetch(`${CONFIG.baseUrl}/fhir/Patient?_count=2`, {
      headers: { 'X-Tenant-ID': API_DEFAULT_TENANT },
    });
    if (!response.ok) {
      throw new Error(`FHIR patient request failed: ${response.status}`);
    }
    const data = await response.json();
    const entries = data.entry || [];
    const ids = entries.map((entry) => entry.resource?.id).filter(Boolean);
    if (ids.length === 0) {
      throw new Error('No patient IDs found');
    }
    return ids;
  } catch (error) {
    log(`Patient ID lookup failed, using fallback IDs: ${error.message}`, 'warning');
    return ['pat-001', 'pat-002'];
  }
}

function applyPatientIdsToScenarios(scenarios, patientIds) {
  const primaryId = patientIds[0] || 'pat-001';
  const secondaryId = patientIds[1] || primaryId;

  for (const scenario of scenarios) {
    scenario.pages = scenario.pages.map((page) => {
      let updatedPath = page.path;
      updatedPath = updatedPath.replace('pat-001', primaryId);
      updatedPath = updatedPath.replace('pat-002', secondaryId);
      return { ...page, path: updatedPath };
    });
  }
}

function filterAnalyticsPages(scenarios) {
  if (!SKIP_ANALYTICS) return;
  for (const scenario of scenarios) {
    scenario.pages = scenario.pages.filter((page) => !page.path.startsWith('/analytics'));
  }
  log('Skipping analytics pages (SKIP_ANALYTICS=1)', 'info');
}

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

/**
 * Detect page type from URL path
 */
function detectPageType(path) {
  if (path.includes('dashboard')) return 'dashboard';
  if (path.includes('patients')) return 'patients';
  if (path.includes('care-gaps')) return 'care-gaps';
  if (path.includes('quality-measures') || path.includes('quality-measure')) return 'quality-measures';
  if (path.includes('results') || path.includes('cql-results')) return 'results';
  if (path.includes('analytics')) return 'analytics';
  return 'generic';
}

/**
 * Set up network request monitoring for a page type
 * Returns handlers that can be attached before navigation
 */
function setupNetworkMonitoring(page, pageType) {
  if (!CONFIG.monitorNetworkRequests) return null;
  
  const patterns = DATA_API_PATTERNS[pageType] || [];
  if (patterns.length === 0) return null;
  
  const pendingRequests = new Map();
  const completedRequests = new Set();
  
  // Intercept requests
  const requestHandler = (request) => {
    const url = request.url();
    if (patterns.some(pattern => url.includes(pattern))) {
      pendingRequests.set(url, request);
      log(`  Monitoring API request: ${url.substring(0, 80)}...`, 'info');
    }
  };
  
  // Track completions
  const responseHandler = (response) => {
    const url = response.url();
    if (patterns.some(pattern => url.includes(pattern))) {
      pendingRequests.delete(url);
      completedRequests.add(url);
      const status = response.status();
      if (status >= 200 && status < 300) {
        log(`  ✓ API request completed: ${url.substring(0, 80)}... (${status})`, 'info');
      } else {
        log(`  ⚠ API request failed: ${url.substring(0, 80)}... (${status})`, 'warning');
      }
    }
  };
  
  // Set up listeners
  page.on('request', requestHandler);
  page.on('response', responseHandler);
  
  return {
    handlers: { requestHandler, responseHandler },
    pendingRequests,
    completedRequests,
    waitForFirstResponse: async () => {
      // Wait for first API response
      const maxWait = CONFIG.dataLoadTimeout;
      const startTime = Date.now();
      const checkInterval = 100;
      
      // Give a moment for requests to start after navigation
      await page.waitForTimeout(500);
      
      while (completedRequests.size === 0 && (Date.now() - startTime) < maxWait) {
        await page.waitForTimeout(checkInterval);
      }
      
      return completedRequests.size > 0;
    },
    waitForCompletion: async () => {
      // Wait for all requests to complete
      const maxWait = CONFIG.dataLoadTimeout;
      const startTime = Date.now();
      const checkInterval = 100;
      
      // Give a moment for requests to start after navigation
      await page.waitForTimeout(500);
      
      while (pendingRequests.size > 0 && (Date.now() - startTime) < maxWait) {
        await page.waitForTimeout(checkInterval);
      }
      
      const allComplete = pendingRequests.size === 0;
      if (!allComplete) {
        log(`Warning: ${pendingRequests.size} API request(s) may not have completed`, 'warning');
        pendingRequests.forEach((request, url) => {
          log(`  Pending: ${url.substring(0, 80)}...`, 'warning');
        });
      } else if (completedRequests.size > 0) {
        log(`  All ${completedRequests.size} API request(s) completed`, 'success');
      }
      
      return allComplete;
    },
    cleanup: () => {
      page.off('request', requestHandler);
      page.off('response', responseHandler);
    }
  };
}

/**
 * Get data selectors for specific page types
 */
function getDataSelectorsForPage(pageType) {
  const selectors = {
    dashboard: [
      '[class*="stat-card"]',
      '[class*="dashboard-stat"]',
      'app-stat-card',
      'table tbody tr',
      '[class*="chart"]',
      'mat-card mat-card-content'
    ],
    patients: [
      'table tbody tr',
      '[class*="patient-list"]',
      '[class*="patient-card"]',
      'mat-table tbody tr'
    ],
    'care-gaps': [
      'table tbody tr',
      '[class*="care-gap"]',
      '[class*="gap-card"]',
      'mat-table tbody tr'
    ],
    'quality-measures': [
      'table tbody tr',
      '[class*="measure"]',
      '[class*="quality-card"]',
      'mat-table tbody tr'
    ],
    results: [
      'table tbody tr',
      '[class*="result"]',
      'mat-table tbody tr'
    ],
    analytics: [
      '[class*="chart"]',
      'canvas',
      'svg',
      'table tbody tr'
    ]
  };
  
  return selectors[pageType] || ['table tbody tr', '[class*="card"]'];
}

/**
 * Wait for data to load by monitoring DOM state
 */
async function waitForDataToLoad(page, pageType) {
  if (!CONFIG.monitorDOMState) return;
  
  const checks = [
    // 1. Wait for loading overlays to disappear
    async () => {
      try {
        await page.waitForSelector('app-loading-overlay', { 
          state: 'hidden', 
          timeout: 5000 
        });
      } catch (e) {
        // Overlay may not exist, continue
      }
    },
    
    // 2. Wait for spinners to disappear
    async () => {
      try {
        await page.waitForFunction(() => {
          const spinners = document.querySelectorAll('mat-spinner, .mat-spinner, [class*="spinner"], [class*="loading"]');
          return Array.from(spinners).every(spinner => {
            const style = window.getComputedStyle(spinner);
            const parent = spinner.closest('[class*="loading"]');
            const parentStyle = parent ? window.getComputedStyle(parent) : null;
            return (style.display === 'none' || style.visibility === 'hidden') &&
                   (!parentStyle || parentStyle.display === 'none' || parentStyle.visibility === 'hidden');
          });
        }, { timeout: 10000 });
      } catch (e) {
        // Spinners may not exist, continue
      }
    },
    
    // 3. Wait for data elements to appear (page-specific)
    async () => {
      const dataSelectors = getDataSelectorsForPage(pageType);
      let foundAny = false;
      
      for (const selector of dataSelectors) {
        try {
          await page.waitForSelector(selector, { timeout: 5000 });
          foundAny = true;
          break; // Found at least one data element
        } catch (e) {
          // Some selectors may not exist on all pages
        }
      }
      
      if (!foundAny && dataSelectors.length > 0) {
        // Try waiting a bit more for data to appear
        await page.waitForTimeout(2000);
      }
    }
  ];
  
  // Run all checks in parallel with timeout
  await Promise.allSettled(checks.map(check => check()));
}

async function login(page, credentials, baseUrl) {
  try {
    await page.goto(`${baseUrl}/`, { waitUntil: 'networkidle', timeout: CONFIG.timeout });
    
    // Wait for Angular Material inputs to be ready
    await page.waitForSelector('input[type="text"], input[type="password"]', { timeout: 10000 });
    await page.waitForTimeout(2000); // Wait for Angular to fully initialize
    
    // Try Material Design selectors first (Angular Material)
    const usernameInput = await page.$('#mat-input-0, input[placeholder*="username" i], input[placeholder*="email" i]');
    const passwordInput = await page.$('#mat-input-1, input[type="password"]');
    
    if (usernameInput && passwordInput) {
      // Fill credentials using Material Design inputs
      await usernameInput.fill(credentials.email);
      await passwordInput.fill(credentials.password);
      
      // Try to find and click Demo Login button (preferred for demo mode)
      const demoButton = await page.$('button.demo-button, button:has-text("Demo Login")');
      if (demoButton) {
        await demoButton.click();
        log(`Clicked Demo Login button`, 'info');
      } else {
        // Fallback to regular login button
        const loginButton = await page.$('button.login-button, button:has-text("Sign In"), button:has-text("Login")');
        if (loginButton) {
          // Wait for button to be enabled (not disabled)
          await page.waitForFunction(
            (btn) => !btn.disabled,
            await loginButton.evaluateHandle(el => el),
            { timeout: 5000 }
          ).catch(() => {});
          await loginButton.click();
        } else {
          // Last resort: press Enter
          await page.keyboard.press('Enter');
        }
      }
    } else {
      // Fallback: try generic selectors
      const inputs = await page.$$('input[type="text"], input[type="email"]');
      const passwordInputs = await page.$$('input[type="password"]');
      
      if (inputs.length > 0 && passwordInputs.length > 0) {
        await inputs[0].fill(credentials.email);
        await passwordInputs[0].fill(credentials.password);
        
        // Try Demo Login button
        const demoButton = await page.$('button:has-text("Demo Login"), button.demo-button');
        if (demoButton) {
          await demoButton.click();
        } else {
          await page.keyboard.press('Enter');
        }
      } else {
        log(`Could not find login form inputs`, 'error');
        return false;
      }
    }
    
    // Wait for navigation or dashboard to appear
    await Promise.race([
      page.waitForNavigation({ waitUntil: 'networkidle', timeout: 20000 }),
      page.waitForSelector('[class*="dashboard"], [class*="main"], nav, header, [class*="mat-sidenav"]', { timeout: 20000 }),
      page.waitForFunction(() => !window.location.href.includes('/login') && window.location.href !== 'http://localhost:4200/', { timeout: 20000 })
    ]).catch(() => {});
    
    // Additional wait for Angular to render
    await page.waitForTimeout(3000);
    
    // Verify we're logged in (not still on login page)
    const currentUrl = page.url();
    const pageContent = await page.content();
    const hasLoginForm = pageContent.includes('mat-input-0') || pageContent.includes('Enter your username');
    
    if (hasLoginForm && (currentUrl.includes('/login') || currentUrl.endsWith('/'))) {
      // Still on login page - check for error messages
      const errorMsg = await page.$('[class*="error"], [class*="alert"], [class*="mat-error"]');
      if (errorMsg) {
        const errorText = await errorMsg.textContent();
        log(`Login error: ${errorText}`, 'error');
      } else {
        log(`Still on login page - may need different credentials or demo mode`, 'warning');
      }
      return false;
    }
    
    log(`Logged in as ${credentials.email}`, 'success');
    return true;
  } catch (error) {
    log(`Login failed for ${credentials.email}: ${error.message}`, 'error');
    // Try to capture login page for debugging
    try {
      await page.screenshot({ path: path.join(CONFIG.screenshotDir, 'login-error.png'), fullPage: true });
    } catch (e) {
      // Ignore screenshot errors
    }
    return false;
  }
}

/**
 * Validate that a region contains data
 */
async function validateRegionData(page, region) {
  try {
    const elements = await page.$$(region.selector);
    
    if (elements.length < region.validation.minElements) {
      return false;
    }
    
    // Run custom data check if provided
    if (region.validation.dataCheck) {
      // Pass all elements to the data check function
      const result = await region.validation.dataCheck(elements);
      return result;
    }
    
    // Default: check element has text content
    const hasContent = await Promise.all(
      elements.slice(0, 10).map(async el => {
        const text = await el.textContent();
        return text.trim().length > 0;
      })
    );
    
    return hasContent.some(has => has);
  } catch (e) {
    return false;
  }
}

/**
 * Capture screenshot of a specific region
 */
async function captureRegionScreenshot(page, region, pageInfo, outputDir) {
  try {
    // Wait for region selector to be visible
    await page.waitForSelector(region.selector, { timeout: 10000 }).catch(() => {
      // Region may not exist on this page, that's okay
    });
    
    // Get all matching elements
    const elements = await page.$$(region.selector);
    if (elements.length === 0) {
      log(`  Region ${region.name}: Not found on page`, 'warning');
      return false;
    }
    
    // Validate region has data
    const hasData = await validateRegionData(page, region);
    if (!hasData && region.priority === 'high') {
      log(`  Region ${region.name}: High-priority region may not have data`, 'warning');
    } else if (hasData) {
      log(`  Region ${region.name}: Data validated`, 'info');
    }
    
    // Ensure regions directory exists
    const regionsDir = path.join(outputDir, 'regions');
    ensureDirectoryExists(regionsDir);
    
    // Capture region screenshot
    const filename = `${pageInfo.name}-${region.name}.png`;
    const filepath = path.join(regionsDir, filename);
    const target = elements[0];
    await target.scrollIntoViewIfNeeded();
    await target.screenshot({ path: filepath });
    
    // Verify screenshot size
    const stats = fs.statSync(filepath);
    const fileSizeKB = stats.size / 1024;
    
    log(`  ✓ Captured region: ${region.name} (${fileSizeKB.toFixed(2)}KB)`, 'success');
    return true;
  } catch (error) {
    log(`  ✗ Failed to capture region ${region.name}: ${error.message}`, 'error');
    return false;
  }
}

/**
 * Capture all regions for a page type
 */
async function captureAllRegions(page, pageType, pageInfo, outputDir) {
  const regions = PAGE_REGIONS[pageType] || [];
  if (regions.length === 0) {
    return { captured: 0, validated: 0 };
  }
  
  log(`Capturing ${regions.length} region(s) for ${pageType}...`, 'info');
  
  let captured = 0;
  let validated = 0;
  
  for (const region of regions) {
    const hasData = await validateRegionData(page, region);
    if (hasData) validated++;
    
    if (await captureRegionScreenshot(page, region, pageInfo, outputDir)) {
      captured++;
    }
  }
  
  return { captured, validated };
}

async function validatePageHasData(page, pageInfo) {
  if (!CONFIG.validateData) return true;
  
  try {
    // Wait for content to load
    await page.waitForTimeout(2000);
    
    // Check for common data indicators
    const hasData = await page.evaluate(() => {
      // Check for tables with rows
      const tables = document.querySelectorAll('table tbody tr');
      if (tables.length > 0) return true;
      
      // Check for lists with items
      const listItems = document.querySelectorAll('ul li, ol li');
      if (listItems.length > 3) return true;
      
      // Check for cards or data containers
      const cards = document.querySelectorAll('[class*="card"], [class*="item"], [class*="row"]');
      if (cards.length > 0) return true;
      
      // Check for charts or visualizations
      const charts = document.querySelectorAll('canvas, svg, [class*="chart"]');
      if (charts.length > 0) return true;
      
      // Check for text content (not just headers)
      const textContent = document.body.innerText || '';
      const wordCount = textContent.split(/\s+/).length;
      if (wordCount > 50) return true; // Reasonable content threshold
      
      return false;
    });
    
    return hasData;
  } catch (e) {
    // If validation fails, still capture (might be a loading page)
    return true;
  }
}

async function captureScreenshot(page, scenario, pageInfo, outputDir, phase = null) {
  const capturePhase = phase || CONFIG.phase;
  let networkMonitor = null;
  
  try {
    const url = `${scenario.baseUrl}${pageInfo.path}`;
    
    log(`Navigating to: ${url} (Phase: ${capturePhase})`, 'info');
    
    // Determine page type for data loading detection
    const pageType = detectPageType(pageInfo.path);
    
    // Navigate to page
    await page.goto(url, { waitUntil: 'networkidle', timeout: CONFIG.timeout });
    
    if (capturePhase === 'BEFORE') {
      // Capture immediately - before any data loads
      await page.waitForTimeout(1000); // Just wait for page render
      const filename = `${scenario.userType}-${pageInfo.name}-before.png`;
      const filepath = path.join(outputDir, filename);
      
      await page.screenshot({
        path: filepath,
        fullPage: true,
      });
      
      const stats = fs.statSync(filepath);
      const fileSizeKB = stats.size / 1024;
      log(`Captured BEFORE: ${filename} (${fileSizeKB.toFixed(2)}KB)`, 'success');
      return true;
    }
    
    if (capturePhase === 'DURING') {
      // Set up network monitoring
      if (CONFIG.monitorNetworkRequests) {
        networkMonitor = setupNetworkMonitoring(page, pageType);
      }
      
      // Capture during initial loading
      await page.waitForTimeout(500);
      const filenameLoading = `${scenario.userType}-${pageInfo.name}-during-loading.png`;
      const filepathLoading = path.join(outputDir, filenameLoading);
      await page.screenshot({
        path: filepathLoading,
        fullPage: true,
      });
      const statsLoading = fs.statSync(filepathLoading);
      log(`Captured DURING (loading): ${filenameLoading} (${(statsLoading.size / 1024).toFixed(2)}KB)`, 'info');
      
      // Wait for first API response and capture partial data
      if (networkMonitor) {
        await networkMonitor.waitForFirstResponse();
        await page.waitForTimeout(500);
        const filenamePartial = `${scenario.userType}-${pageInfo.name}-during-partial.png`;
        const filepathPartial = path.join(outputDir, filenamePartial);
        await page.screenshot({
          path: filepathPartial,
          fullPage: true,
        });
        const statsPartial = fs.statSync(filepathPartial);
        log(`Captured DURING (partial): ${filenamePartial} (${(statsPartial.size / 1024).toFixed(2)}KB)`, 'info');
        
        // Continue monitoring until complete
        await networkMonitor.waitForCompletion();
        await page.waitForTimeout(500);
        const filenameComplete = `${scenario.userType}-${pageInfo.name}-during-complete.png`;
        const filepathComplete = path.join(outputDir, filenameComplete);
        await page.screenshot({
          path: filepathComplete,
          fullPage: true,
        });
        const statsComplete = fs.statSync(filepathComplete);
        log(`Captured DURING (complete): ${filenameComplete} (${(statsComplete.size / 1024).toFixed(2)}KB)`, 'success');
      }
      
      if (networkMonitor) {
        networkMonitor.cleanup();
      }
      return true;
    }
    
    if (capturePhase === 'AFTER') {
      // Set up network monitoring BEFORE navigation if enabled
      if (CONFIG.waitForDataLoad && CONFIG.monitorNetworkRequests) {
        log(`Setting up network request monitoring for ${pageType}...`, 'info');
        networkMonitor = setupNetworkMonitoring(page, pageType);
      }
      
      // Wait for network requests to complete (after navigation)
      if (networkMonitor) {
        log(`Waiting for data API requests...`, 'info');
        const apiComplete = await networkMonitor.waitForCompletion();
        if (!apiComplete) {
          log(`Warning: Some API requests may not have completed`, 'warning');
        }
      }
      
      // Wait for DOM state changes (loading spinners, data elements) if enabled
      if (CONFIG.waitForDataLoad && CONFIG.monitorDOMState) {
        log(`Waiting for data to render...`, 'info');
        await waitForDataToLoad(page, pageType);
      }
      
      // Additional wait if specified
      if (pageInfo.wait) {
        await page.waitForTimeout(pageInfo.wait);
      }
      
      // Final validation: check data is present
      const hasData = await validatePageHasData(page, pageInfo);
      if (!hasData && CONFIG.validateData) {
        log(`Warning: ${pageInfo.name} may not have data`, 'warning');
      }
      
      // Capture region screenshots if enabled
      let regionResults = { captured: 0, validated: 0 };
      if (CONFIG.captureRegions) {
        regionResults = await captureAllRegions(page, pageType, pageInfo, outputDir);
        log(`Region capture: ${regionResults.captured} captured, ${regionResults.validated} validated`, 
          regionResults.validated === regionResults.captured ? 'success' : 'warning');
      }
      
      // Generate filename
      const filename = `${scenario.userType}-${pageInfo.name}-after.png`;
      const filepath = path.join(outputDir, filename);
      
      // Capture full-page screenshot
      await page.screenshot({
        path: filepath,
        fullPage: true,
      });
      
      // Verify screenshot was created and has reasonable size
      const stats = fs.statSync(filepath);
      const fileSizeKB = stats.size / 1024;
      
      if (fileSizeKB < 10) {
        log(`Warning: ${filename} is very small (${fileSizeKB.toFixed(2)}KB) - may be empty`, 'warning');
      }
      
      log(`Captured AFTER: ${filename} (${fileSizeKB.toFixed(2)}KB)`, 'success');
      
      if (networkMonitor) {
        networkMonitor.cleanup();
      }
      return true;
    }
    
    // Default behavior (backward compatibility)
    return await captureScreenshot(page, scenario, pageInfo, outputDir, 'AFTER');
  } catch (error) {
    log(`Failed to capture ${pageInfo.name}: ${error.message}`, 'error');
    return false;
  } finally {
    // Clean up network monitoring listeners
    if (networkMonitor) {
      networkMonitor.cleanup();
    }
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
      if (await captureScreenshot(page, scenario, pageInfo, outputDir, CONFIG.phase)) {
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
  log(`Phase: ${CONFIG.phase}`, 'info');
  if (CONFIG.scenarioId) {
    log(`Scenario: ${CONFIG.scenarioId}`, 'info');
  }
  if (CONFIG.userType) {
    log(`User Type: ${CONFIG.userType}`, 'info');
  }
  log('========================================', 'info');
  
  // Ensure screenshot directory exists
  ensureDirectoryExists(CONFIG.screenshotDir);
  
  // Filter scenarios if scenarioId or userType provided
  let scenariosToCapture = SCENARIOS;
  if (CONFIG.scenarioId) {
    // Filter by scenarioId if scenarios have that property
    scenariosToCapture = scenariosToCapture.filter(s => 
      s.scenarioId === CONFIG.scenarioId || s.userType === CONFIG.scenarioId
    );
  }
  if (CONFIG.userType) {
    scenariosToCapture = scenariosToCapture.filter(s => s.userType === CONFIG.userType);
  }
  
  if (scenariosToCapture.length === 0) {
    log(`No scenarios found matching criteria`, 'error');
    process.exit(1);
  }

  const patientIds = await fetchPatientIds();
  applyPatientIdsToScenarios(scenariosToCapture, patientIds);
  filterAnalyticsPages(scenariosToCapture);

  log(`Capturing ${CONFIG.phase} screenshots for ${scenariosToCapture.length} scenario(s)`, 'info');
  
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
    for (const scenario of scenariosToCapture) {
      const result = await captureScenario(browser, scenario);
      stats.totalCaptured += result.captured;
      stats.totalFailed += result.failed;
    }
    
    // Generate index only if not phase-specific
    if (CONFIG.phase === 'AFTER' && !CONFIG.scenarioId) {
      await generateIndex(scenariosToCapture, stats);
    }
    
    log('\n========================================', 'info');
    log(`${CONFIG.phase} Screenshot Capture Complete`, 'success');
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
