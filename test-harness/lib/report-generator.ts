/**
 * HDIM Patient Care Outcomes - Report Generator
 *
 * Generates visual HTML reports for customer demonstrations
 */

import * as fs from 'fs';
import * as path from 'path';

interface ScenarioResult {
  scenarioId: string;
  scenarioName: string;
  executedAt: string;
  patientsProcessed: number;
  aggregateOutcomes: {
    baselineComplianceRate: number;
    postInterventionComplianceRate: number;
    improvementPercentage: number;
    careGapsClosedCount: number;
  };
  clinicalImpact: {
    estimatedCostSavings: number;
    qualityScoreImprovement: number;
    patientOutcomesImproved: number;
  };
}

interface Scenario {
  id: string;
  name: string;
  description: string;
  hedis_measures: string[];
  baseline_metrics: Record<string, number>;
  post_intervention_metrics: Record<string, number>;
  outcomes_summary: Record<string, string | number>;
  clinical_stories: Array<{
    patient_id: string;
    name: string;
    age: number;
    baseline: Record<string, any>;
    hdim_actions: string[];
    post_intervention: Record<string, any>;
    outcome: string;
  }>;
}

export class ReportGenerator {
  private reportsDir: string;
  private scenariosDir: string;

  constructor(basePath: string) {
    this.reportsDir = path.join(basePath, 'reports');
    this.scenariosDir = path.join(basePath, 'scenarios');
  }

  /**
   * Generate comprehensive HTML report
   */
  generateReport(results: ScenarioResult[]): string {
    const scenarios = this.loadAllScenarios();
    const timestamp = new Date().toISOString().split('T')[0];

    const html = `
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>HDIM Patient Care Outcomes Report</title>
  <style>
    :root {
      --primary: #0066cc;
      --success: #28a745;
      --warning: #ffc107;
      --danger: #dc3545;
      --dark: #343a40;
      --light: #f8f9fa;
      --border: #dee2e6;
    }

    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
    }

    body {
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
      line-height: 1.6;
      color: var(--dark);
      background: var(--light);
    }

    .container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 2rem;
    }

    header {
      background: linear-gradient(135deg, var(--primary), #004499);
      color: white;
      padding: 3rem 2rem;
      text-align: center;
      margin-bottom: 2rem;
    }

    header h1 {
      font-size: 2.5rem;
      margin-bottom: 0.5rem;
    }

    header p {
      opacity: 0.9;
      font-size: 1.2rem;
    }

    .summary-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 1.5rem;
      margin-bottom: 3rem;
    }

    .summary-card {
      background: white;
      border-radius: 12px;
      padding: 1.5rem;
      text-align: center;
      box-shadow: 0 4px 6px rgba(0,0,0,0.1);
      border-top: 4px solid var(--primary);
    }

    .summary-card.success {
      border-top-color: var(--success);
    }

    .summary-card h3 {
      color: #666;
      font-size: 0.9rem;
      text-transform: uppercase;
      letter-spacing: 1px;
      margin-bottom: 0.5rem;
    }

    .summary-card .value {
      font-size: 2.5rem;
      font-weight: 700;
      color: var(--primary);
    }

    .summary-card.success .value {
      color: var(--success);
    }

    .scenario-section {
      background: white;
      border-radius: 12px;
      margin-bottom: 2rem;
      overflow: hidden;
      box-shadow: 0 4px 6px rgba(0,0,0,0.1);
    }

    .scenario-header {
      background: var(--dark);
      color: white;
      padding: 1.5rem;
    }

    .scenario-header h2 {
      margin-bottom: 0.5rem;
    }

    .scenario-header p {
      opacity: 0.8;
    }

    .scenario-body {
      padding: 1.5rem;
    }

    .metrics-comparison {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 2rem;
      margin-bottom: 2rem;
    }

    .metrics-column h4 {
      background: var(--light);
      padding: 0.75rem 1rem;
      border-radius: 8px;
      margin-bottom: 1rem;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .metrics-column.baseline h4 {
      border-left: 4px solid var(--danger);
    }

    .metrics-column.intervention h4 {
      border-left: 4px solid var(--success);
    }

    .metric-row {
      display: flex;
      justify-content: space-between;
      padding: 0.5rem 0;
      border-bottom: 1px solid var(--border);
    }

    .metric-row:last-child {
      border-bottom: none;
    }

    .metric-value {
      font-weight: 600;
    }

    .improvement-badge {
      display: inline-block;
      background: var(--success);
      color: white;
      padding: 0.25rem 0.75rem;
      border-radius: 20px;
      font-size: 0.85rem;
      font-weight: 600;
    }

    .patient-story {
      background: var(--light);
      border-radius: 8px;
      padding: 1.5rem;
      margin-bottom: 1rem;
      border-left: 4px solid var(--primary);
    }

    .patient-story h5 {
      color: var(--primary);
      margin-bottom: 1rem;
    }

    .story-timeline {
      position: relative;
      padding-left: 2rem;
    }

    .story-timeline::before {
      content: '';
      position: absolute;
      left: 6px;
      top: 0;
      bottom: 0;
      width: 2px;
      background: var(--border);
    }

    .timeline-item {
      position: relative;
      margin-bottom: 1rem;
      padding-left: 1rem;
    }

    .timeline-item::before {
      content: '';
      position: absolute;
      left: -2rem;
      top: 6px;
      width: 12px;
      height: 12px;
      border-radius: 50%;
      background: var(--primary);
    }

    .timeline-item.success::before {
      background: var(--success);
    }

    .outcome-box {
      background: linear-gradient(135deg, var(--success), #1e7e34);
      color: white;
      padding: 1rem;
      border-radius: 8px;
      margin-top: 1rem;
    }

    .chart-container {
      height: 300px;
      margin: 2rem 0;
    }

    footer {
      text-align: center;
      padding: 2rem;
      color: #666;
      font-size: 0.9rem;
    }

    @media (max-width: 768px) {
      .metrics-comparison {
        grid-template-columns: 1fr;
      }

      .summary-grid {
        grid-template-columns: repeat(2, 1fr);
      }
    }
  </style>
</head>
<body>
  <header>
    <h1>HDIM Patient Care Outcomes</h1>
    <p>Demonstrating Measurable Impact Through Healthcare Data Processing</p>
    <p style="margin-top: 1rem; opacity: 0.7;">Generated: ${timestamp}</p>
  </header>

  <div class="container">
    ${this.generateExecutiveSummary(results)}
    ${scenarios.map(s => this.generateScenarioSection(s)).join('\n')}
  </div>

  <footer>
    <p>HealthData-in-Motion (HDIM) | Enterprise Healthcare Quality Platform</p>
    <p>HEDIS, FHIR R4, CQL Engine | HIPAA Compliant</p>
  </footer>
</body>
</html>`;

    const outputPath = path.join(this.reportsDir, `outcomes-report-${timestamp}.html`);
    fs.writeFileSync(outputPath, html);
    console.log(`Report generated: ${outputPath}`);

    return outputPath;
  }

  /**
   * Generate executive summary section
   */
  private generateExecutiveSummary(results: ScenarioResult[]): string {
    const totalPatients = results.reduce((sum, r) => sum + r.patientsProcessed, 0);
    const totalGapsClosed = results.reduce((sum, r) => sum + r.aggregateOutcomes.careGapsClosedCount, 0);
    const totalSavings = results.reduce((sum, r) => sum + r.clinicalImpact.estimatedCostSavings, 0);
    const avgImprovement = results.reduce((sum, r) => sum + r.aggregateOutcomes.improvementPercentage, 0) / results.length;

    return `
    <div class="summary-grid">
      <div class="summary-card">
        <h3>Patient Population</h3>
        <div class="value">${totalPatients.toLocaleString()}</div>
      </div>
      <div class="summary-card success">
        <h3>Care Gaps Closed</h3>
        <div class="value">${totalGapsClosed.toLocaleString()}</div>
      </div>
      <div class="summary-card success">
        <h3>Quality Improvement</h3>
        <div class="value">+${avgImprovement.toFixed(0)}%</div>
      </div>
      <div class="summary-card success">
        <h3>Annual Savings</h3>
        <div class="value">$${(totalSavings / 1000000).toFixed(1)}M</div>
      </div>
    </div>`;
  }

  /**
   * Generate scenario section HTML
   */
  private generateScenarioSection(scenario: Scenario): string {
    const baseline = scenario.baseline_metrics;
    const intervention = scenario.post_intervention_metrics;

    return `
    <section class="scenario-section">
      <div class="scenario-header">
        <h2>${scenario.name}</h2>
        <p>${scenario.description}</p>
        <p style="margin-top: 0.5rem;"><strong>HEDIS Measures:</strong> ${scenario.hedis_measures.join(', ')}</p>
      </div>

      <div class="scenario-body">
        <div class="metrics-comparison">
          <div class="metrics-column baseline">
            <h4>
              <span>Baseline (Before HDIM)</span>
            </h4>
            ${this.generateMetricsRows(baseline)}
          </div>

          <div class="metrics-column intervention">
            <h4>
              <span>Post-Intervention</span>
              <span class="improvement-badge">Improved</span>
            </h4>
            ${this.generateMetricsRows(intervention)}
          </div>
        </div>

        <h4 style="margin-bottom: 1rem;">Patient Success Stories</h4>
        ${scenario.clinical_stories.map(story => this.generatePatientStory(story)).join('\n')}
      </div>
    </section>`;
  }

  /**
   * Generate metrics rows HTML
   */
  private generateMetricsRows(metrics: Record<string, number>): string {
    return Object.entries(metrics)
      .filter(([key]) => !['population_size'].includes(key))
      .slice(0, 6)
      .map(([key, value]) => {
        const label = key.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase());
        const formatted = key.includes('rate')
          ? `${(value * 100).toFixed(0)}%`
          : value.toLocaleString();

        return `
        <div class="metric-row">
          <span>${label}</span>
          <span class="metric-value">${formatted}</span>
        </div>`;
      })
      .join('\n');
  }

  /**
   * Generate patient story HTML
   */
  private generatePatientStory(story: any): string {
    return `
    <div class="patient-story">
      <h5>${story.name}, Age ${story.age}</h5>

      <div class="story-timeline">
        <div class="timeline-item">
          <strong>Baseline:</strong> ${story.baseline.care_gaps?.join(', ') || 'Multiple care gaps identified'}
        </div>

        ${story.hdim_actions.slice(0, 3).map((action: string) => `
        <div class="timeline-item">
          <strong>HDIM Action:</strong> ${action}
        </div>`).join('\n')}

        <div class="timeline-item success">
          <strong>Result:</strong> ${story.post_intervention.care_gaps?.length === 0 ? 'All care gaps closed' : 'Gaps addressed'}
        </div>
      </div>

      <div class="outcome-box">
        <strong>Outcome:</strong> ${story.outcome}
      </div>
    </div>`;
  }

  /**
   * Load all scenario files
   */
  private loadAllScenarios(): Scenario[] {
    const files = fs.readdirSync(this.scenariosDir).filter(f => f.endsWith('.json'));
    return files.map(f => {
      const content = fs.readFileSync(path.join(this.scenariosDir, f), 'utf-8');
      return JSON.parse(content);
    });
  }
}

export default ReportGenerator;
