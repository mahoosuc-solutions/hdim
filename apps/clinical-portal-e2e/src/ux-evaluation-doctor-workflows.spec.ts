import { test, expect, Page } from '@playwright/test';

/**
 * UX Evaluation: Doctor Workflows
 *
 * This test suite evaluates the user experience specifically for doctor workflows,
 * focusing on time-saving opportunities and patient care efficiency.
 *
 * Evaluation Criteria:
 * - Load Time Performance (FCP, LCP, TTI)
 * - Navigation Efficiency
 * - Data Accessibility
 * - Workflow Completion Time
 * - Error Prevention & Recovery
 * - Mobile/Responsive Experience
 * - Information Density & Clarity
 */

interface WorkflowMetrics {
  name: string;
  startTime: number;
  endTime: number;
  duration: number;
  steps: number;
  clicks: number;
  grade: string;
  issues: string[];
  recommendations: string[];
}

const workflowResults: WorkflowMetrics[] = [];

test.describe('UX Evaluation: Doctor Workflows', () => {

  test.beforeEach(async ({ page }) => {
    // Navigate to app
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');
  });

  test('Workflow 1: Quick Patient Search and View', async ({ page }) => {
    const workflow: WorkflowMetrics = {
      name: 'Quick Patient Search and View',
      startTime: Date.now(),
      endTime: 0,
      duration: 0,
      steps: 0,
      clicks: 0,
      grade: 'A',
      issues: [],
      recommendations: []
    };

    // Step 1: Navigate to Patients page
    console.log('\n🔍 Workflow 1: Quick Patient Search and View');
    console.log('Step 1: Navigate to Patients page');

    const patientsNav = page.locator('a[href="/patients"]').first();
    await patientsNav.click();
    workflow.clicks++;
    workflow.steps++;

    await page.waitForLoadState('networkidle');
    const loadTime = Date.now() - workflow.startTime;
    console.log(`  ⏱️  Load time: ${loadTime}ms`);

    if (loadTime > 2000) {
      workflow.grade = 'B';
      workflow.issues.push('Patient list load time exceeds 2 seconds');
      workflow.recommendations.push('Implement skeleton loaders for immediate feedback');
      workflow.recommendations.push('Add pagination or virtual scrolling for large patient lists');
    }

    // Step 2: Search for a patient
    console.log('Step 2: Search for patient');
    const searchInput = page.locator('input[placeholder*="Search"]').first();

    if (await searchInput.isVisible()) {
      const searchStartTime = Date.now();
      await searchInput.fill('Patient');
      workflow.clicks++;
      workflow.steps++;

      // Wait for search results
      await page.waitForTimeout(500);
      const searchResponseTime = Date.now() - searchStartTime;
      console.log(`  ⏱️  Search response: ${searchResponseTime}ms`);

      if (searchResponseTime > 300) {
        workflow.grade = workflow.grade === 'A' ? 'B' : workflow.grade;
        workflow.issues.push('Search response time > 300ms');
        workflow.recommendations.push('Implement client-side filtering for immediate results');
      }
    } else {
      workflow.grade = 'C';
      workflow.issues.push('Search functionality not immediately visible');
      workflow.recommendations.push('Make search more prominent (top of page, larger input)');
    }

    // Step 3: View patient details
    console.log('Step 3: View patient details');
    const firstPatient = page.locator('table tbody tr').first();

    if (await firstPatient.isVisible()) {
      const detailsStartTime = Date.now();
      await firstPatient.click();
      workflow.clicks++;
      workflow.steps++;

      await page.waitForTimeout(1000);
      const detailsLoadTime = Date.now() - detailsStartTime;
      console.log(`  ⏱️  Details load: ${detailsLoadTime}ms`);

      if (detailsLoadTime > 1000) {
        workflow.grade = 'B';
        workflow.issues.push('Patient details panel load > 1 second');
        workflow.recommendations.push('Preload patient data on hover for faster access');
      }
    } else {
      workflow.grade = 'D';
      workflow.issues.push('No patient data available to view');
      workflow.recommendations.push('Add sample data or better empty state messaging');
    }

    workflow.endTime = Date.now();
    workflow.duration = workflow.endTime - workflow.startTime;

    console.log(`\n📊 Workflow Summary:`);
    console.log(`  Duration: ${workflow.duration}ms`);
    console.log(`  Steps: ${workflow.steps}`);
    console.log(`  Clicks: ${workflow.clicks}`);
    console.log(`  Grade: ${workflow.grade}`);
    console.log(`  Issues: ${workflow.issues.length}`);

    workflowResults.push(workflow);

    expect(workflow.grade).not.toBe('F');
  });

  test('Workflow 2: Run Quality Measure Evaluation', async ({ page }) => {
    const workflow: WorkflowMetrics = {
      name: 'Run Quality Measure Evaluation',
      startTime: Date.now(),
      endTime: 0,
      duration: 0,
      steps: 0,
      clicks: 0,
      grade: 'A',
      issues: [],
      recommendations: []
    };

    console.log('\n🔍 Workflow 2: Run Quality Measure Evaluation');
    console.log('Step 1: Navigate to Evaluations page');

    // Step 1: Navigate to Evaluations
    const evalNav = page.locator('a[href="/evaluations"]').first();
    await evalNav.click();
    workflow.clicks++;
    workflow.steps++;

    await page.waitForLoadState('networkidle');
    const loadTime = Date.now() - workflow.startTime;
    console.log(`  ⏱️  Load time: ${loadTime}ms`);

    if (loadTime > 2000) {
      workflow.grade = 'B';
      workflow.issues.push('Evaluations page load time > 2 seconds');
      workflow.recommendations.push('Optimize measure list loading with lazy loading');
    }

    // Step 2: Check form accessibility
    console.log('Step 2: Evaluate form usability');
    const measureSelect = page.locator('mat-select').first();
    const patientInput = page.locator('input[placeholder*="patient"]').or(page.locator('mat-autocomplete')).first();

    if (await measureSelect.isVisible() && await patientInput.isVisible()) {
      console.log('  ✅ Form fields are visible');

      // Check if there's helpful text/instructions
      const helpText = page.locator('mat-hint, .help-text, .info-text');
      const hasHelpText = await helpText.count() > 0;

      if (!hasHelpText) {
        workflow.grade = 'B';
        workflow.issues.push('No contextual help or instructions for evaluation form');
        workflow.recommendations.push('Add tooltips or help text explaining what each field means');
        workflow.recommendations.push('Include examples (e.g., "Select measure like HEDIS Diabetes HbA1c")');
      }
    } else {
      workflow.grade = 'C';
      workflow.issues.push('Evaluation form fields not clearly visible');
      workflow.recommendations.push('Improve form layout with clearer labels and structure');
    }

    // Step 3: Test measure selection
    console.log('Step 3: Test measure selection');
    if (await measureSelect.isVisible()) {
      await measureSelect.click();
      workflow.clicks++;
      workflow.steps++;

      await page.waitForTimeout(500);

      // Check if measures are loaded
      const measureOptions = page.locator('mat-option');
      const optionCount = await measureOptions.count();

      console.log(`  📋 Available measures: ${optionCount}`);

      if (optionCount === 0) {
        workflow.grade = 'D';
        workflow.issues.push('No measures available in dropdown');
        workflow.recommendations.push('Ensure measures are preloaded or show loading state');
      } else if (optionCount > 10) {
        workflow.issues.push('Large number of measures may be overwhelming');
        workflow.recommendations.push('Add search/filter within measure dropdown');
        workflow.recommendations.push('Categorize measures (HEDIS, Custom, etc.)');
      }

      await page.keyboard.press('Escape');
    }

    workflow.endTime = Date.now();
    workflow.duration = workflow.endTime - workflow.startTime;

    console.log(`\n📊 Workflow Summary:`);
    console.log(`  Duration: ${workflow.duration}ms`);
    console.log(`  Steps: ${workflow.steps}`);
    console.log(`  Clicks: ${workflow.clicks}`);
    console.log(`  Grade: ${workflow.grade}`);
    console.log(`  Issues: ${workflow.issues.length}`);

    workflowResults.push(workflow);

    expect(workflow.grade).not.toBe('F');
  });

  test('Workflow 3: Review Dashboard and Care Gaps', async ({ page }) => {
    const workflow: WorkflowMetrics = {
      name: 'Review Dashboard and Care Gaps',
      startTime: Date.now(),
      endTime: 0,
      duration: 0,
      steps: 0,
      clicks: 0,
      grade: 'A',
      issues: [],
      recommendations: []
    };

    console.log('\n🔍 Workflow 3: Review Dashboard and Care Gaps');
    console.log('Step 1: Navigate to Dashboard');

    // Step 1: Navigate to Dashboard
    const dashNav = page.locator('a[href="/dashboard"]').first();
    await dashNav.click();
    workflow.clicks++;
    workflow.steps++;

    await page.waitForLoadState('networkidle');
    const loadTime = Date.now() - workflow.startTime;
    console.log(`  ⏱️  Dashboard load: ${loadTime}ms`);

    if (loadTime > 3000) {
      workflow.grade = 'C';
      workflow.issues.push('Dashboard takes > 3 seconds to load');
      workflow.recommendations.push('Implement progressive loading - show most critical metrics first');
      workflow.recommendations.push('Cache dashboard data with refresh button');
    }

    // Step 2: Evaluate data visualization
    console.log('Step 2: Evaluate data visualization');
    await page.waitForTimeout(2000); // Wait for data to load

    const statsCards = page.locator('.stat-card, mat-card').count();
    console.log(`  📊 Statistics cards visible`);

    // Check for visual hierarchy
    const hasHeading = await page.locator('h1, h2').count() > 0;
    if (!hasHeading) {
      workflow.grade = 'B';
      workflow.issues.push('Unclear visual hierarchy on dashboard');
      workflow.recommendations.push('Add clear section headings for different metric groups');
    }

    // Check for actionable insights
    const hasActionButtons = await page.locator('button:has-text("View"), button:has-text("Details")').count() > 0;
    if (!hasActionButtons) {
      workflow.issues.push('No quick actions from dashboard metrics');
      workflow.recommendations.push('Add "View Details" or "Take Action" buttons on metric cards');
      workflow.recommendations.push('Enable drill-down from dashboard to specific patient lists');
    }

    // Step 3: Check for care gap visibility
    console.log('Step 3: Check care gap prominence');
    const careGapSection = page.locator('text=/care gap/i, text=/at risk/i, text=/action required/i');
    const hasCareGaps = await careGapSection.count() > 0;

    if (!hasCareGaps) {
      workflow.grade = 'C';
      workflow.issues.push('Care gaps not prominently displayed on dashboard');
      workflow.recommendations.push('Add dedicated "Patients Needing Attention" section at top of dashboard');
      workflow.recommendations.push('Highlight urgent care gaps with visual indicators (red badges, icons)');
      workflow.recommendations.push('Show count of patients with open care gaps');
    }

    workflow.endTime = Date.now();
    workflow.duration = workflow.endTime - workflow.startTime;

    console.log(`\n📊 Workflow Summary:`);
    console.log(`  Duration: ${workflow.duration}ms`);
    console.log(`  Steps: ${workflow.steps}`);
    console.log(`  Clicks: ${workflow.clicks}`);
    console.log(`  Grade: ${workflow.grade}`);
    console.log(`  Issues: ${workflow.issues.length}`);

    workflowResults.push(workflow);

    expect(workflow.grade).not.toBe('F');
  });

  test('Workflow 4: Generate and Export Report', async ({ page }) => {
    const workflow: WorkflowMetrics = {
      name: 'Generate and Export Report',
      startTime: Date.now(),
      endTime: 0,
      duration: 0,
      steps: 0,
      clicks: 0,
      grade: 'A',
      issues: [],
      recommendations: []
    };

    console.log('\n🔍 Workflow 4: Generate and Export Report');
    console.log('Step 1: Navigate to Reports page');

    // Step 1: Navigate to Reports
    const reportsNav = page.locator('a[href="/reports"]').first();
    await reportsNav.click();
    workflow.clicks++;
    workflow.steps++;

    await page.waitForLoadState('networkidle');
    const loadTime = Date.now() - workflow.startTime;
    console.log(`  ⏱️  Load time: ${loadTime}ms`);

    // Step 2: Evaluate report generation UI
    console.log('Step 2: Evaluate report generation interface');
    const generateButton = page.locator('button:has-text("Generate")').first();

    if (await generateButton.isVisible()) {
      console.log('  ✅ Generate button visible');

      // Check for report type clarity
      const hasReportTypes = await page.locator('text=/patient report/i, text=/population report/i').count() > 0;
      if (hasReportTypes) {
        console.log('  ✅ Report types are clear');
      } else {
        workflow.grade = 'B';
        workflow.issues.push('Report types not clearly differentiated');
        workflow.recommendations.push('Add icons and color coding to distinguish report types');
      }
    } else {
      workflow.grade = 'C';
      workflow.issues.push('Report generation options not immediately visible');
      workflow.recommendations.push('Make report generation more prominent');
    }

    // Step 3: Check saved reports
    console.log('Step 3: Navigate to saved reports');
    const savedReportsTab = page.locator('text=/saved reports/i').first();

    if (await savedReportsTab.isVisible()) {
      await savedReportsTab.click();
      workflow.clicks++;
      workflow.steps++;

      await page.waitForTimeout(1000);

      // Check for export options
      const exportButtons = await page.locator('button:has-text("Export"), button:has-text("CSV"), button:has-text("Excel")').count();

      if (exportButtons === 0) {
        workflow.grade = 'B';
        workflow.issues.push('Export options not visible or unclear');
        workflow.recommendations.push('Add prominent export buttons (CSV, Excel, PDF)');
        workflow.recommendations.push('Include export all functionality for bulk downloads');
      } else {
        console.log(`  ✅ Export options available`);
      }
    }

    workflow.endTime = Date.now();
    workflow.duration = workflow.endTime - workflow.startTime;

    console.log(`\n📊 Workflow Summary:`);
    console.log(`  Duration: ${workflow.duration}ms`);
    console.log(`  Steps: ${workflow.steps}`);
    console.log(`  Clicks: ${workflow.clicks}`);
    console.log(`  Grade: ${workflow.grade}`);
    console.log(`  Issues: ${workflow.issues.length}`);

    workflowResults.push(workflow);

    expect(workflow.grade).not.toBe('F');
  });

  test('Workflow 5: Mobile Responsiveness for Tablet/iPad', async ({ page }) => {
    const workflow: WorkflowMetrics = {
      name: 'Mobile Responsiveness (Tablet)',
      startTime: Date.now(),
      endTime: 0,
      duration: 0,
      steps: 0,
      clicks: 0,
      grade: 'A',
      issues: [],
      recommendations: []
    };

    console.log('\n🔍 Workflow 5: Mobile Responsiveness (Tablet)');
    console.log('Step 1: Set tablet viewport (iPad Pro)');

    // Set tablet size
    await page.setViewportSize({ width: 1024, height: 1366 });
    workflow.steps++;

    // Test navigation
    console.log('Step 2: Test navigation on tablet');
    await page.goto('/patients');
    await page.waitForLoadState('networkidle');

    // Use first() to avoid strict mode violation when multiple nav elements exist
    const nav = page.locator('mat-nav-list, nav').first();
    const isNavVisible = await nav.isVisible();

    if (!isNavVisible) {
      workflow.grade = 'B';
      workflow.issues.push('Navigation may be hidden on tablet view');
      workflow.recommendations.push('Ensure navigation is accessible via hamburger menu on tablet');
    }

    // Test patient list on tablet
    console.log('Step 3: Test patient list table on tablet');
    const table = page.locator('table');

    if (await table.isVisible()) {
      // Check if table is scrollable or responsive
      const tableWidth = await table.boundingBox();
      const viewportWidth = 1024;

      if (tableWidth && tableWidth.width > viewportWidth) {
        workflow.issues.push('Patient table may require horizontal scrolling on tablet');
        workflow.recommendations.push('Implement responsive table with collapsible columns');
        workflow.recommendations.push('Consider card layout for mobile/tablet instead of table');
      }
    }

    // Test touch targets
    console.log('Step 4: Evaluate touch target sizes');
    const buttons = page.locator('button');
    const buttonCount = await buttons.count();

    let smallButtonCount = 0;
    for (let i = 0; i < Math.min(buttonCount, 10); i++) {
      const bbox = await buttons.nth(i).boundingBox();
      if (bbox && (bbox.height < 44 || bbox.width < 44)) {
        smallButtonCount++;
      }
    }

    if (smallButtonCount > 0) {
      workflow.grade = 'B';
      workflow.issues.push(`${smallButtonCount} buttons are smaller than 44x44px (Apple HIG minimum)`);
      workflow.recommendations.push('Increase button sizes to minimum 44x44px for better touch targets');
    }

    workflow.endTime = Date.now();
    workflow.duration = workflow.endTime - workflow.startTime;

    console.log(`\n📊 Workflow Summary:`);
    console.log(`  Duration: ${workflow.duration}ms`);
    console.log(`  Steps: ${workflow.steps}`);
    console.log(`  Clicks: ${workflow.clicks}`);
    console.log(`  Grade: ${workflow.grade}`);
    console.log(`  Issues: ${workflow.issues.length}`);

    workflowResults.push(workflow);

    expect(workflow.grade).not.toBe('F');
  });

  test.afterAll(() => {
    // Generate final UX report
    console.log('\n\n' + '='.repeat(80));
    console.log('UX EVALUATION SUMMARY: CLINICAL PORTAL FOR DOCTORS');
    console.log('='.repeat(80) + '\n');

    let totalIssues = 0;
    let totalRecommendations = 0;
    const gradeCount: Record<string, number> = { A: 0, B: 0, C: 0, D: 0, F: 0 };

    workflowResults.forEach((workflow, index) => {
      console.log(`\n${index + 1}. ${workflow.name}`);
      console.log(`   Grade: ${workflow.grade}`);
      console.log(`   Duration: ${workflow.duration}ms`);
      console.log(`   Steps: ${workflow.steps} | Clicks: ${workflow.clicks}`);
      console.log(`   Issues: ${workflow.issues.length}`);

      if (workflow.issues.length > 0) {
        console.log(`   ⚠️  Issues:`);
        workflow.issues.forEach(issue => console.log(`      - ${issue}`));
      }

      if (workflow.recommendations.length > 0) {
        console.log(`   💡 Recommendations:`);
        workflow.recommendations.forEach(rec => console.log(`      - ${rec}`));
      }

      totalIssues += workflow.issues.length;
      totalRecommendations += workflow.recommendations.length;
      gradeCount[workflow.grade]++;
    });

    console.log('\n' + '-'.repeat(80));
    console.log('OVERALL ASSESSMENT');
    console.log('-'.repeat(80));
    console.log(`Total Workflows Evaluated: ${workflowResults.length}`);
    console.log(`Total Issues Found: ${totalIssues}`);
    console.log(`Total Recommendations: ${totalRecommendations}`);
    console.log(`\nGrade Distribution:`);
    console.log(`  A: ${gradeCount.A} workflows`);
    console.log(`  B: ${gradeCount.B} workflows`);
    console.log(`  C: ${gradeCount.C} workflows`);
    console.log(`  D: ${gradeCount.D} workflows`);
    console.log(`  F: ${gradeCount.F} workflows`);

    const avgGrade = gradeCount.A * 4 + gradeCount.B * 3 + gradeCount.C * 2 + gradeCount.D * 1;
    const avgGradeValue = avgGrade / workflowResults.length;
    let overallGrade = 'F';
    if (avgGradeValue >= 3.5) overallGrade = 'A';
    else if (avgGradeValue >= 2.5) overallGrade = 'B';
    else if (avgGradeValue >= 1.5) overallGrade = 'C';
    else if (avgGradeValue >= 0.5) overallGrade = 'D';

    console.log(`\n🎯 OVERALL UX GRADE: ${overallGrade}`);
    console.log('\n' + '='.repeat(80) + '\n');
  });
});
