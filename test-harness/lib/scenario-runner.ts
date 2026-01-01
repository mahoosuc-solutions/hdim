/**
 * HDIM Patient Care Outcomes - Scenario Runner
 *
 * Executes patient care scenarios to demonstrate data processing
 * impact on healthcare outcomes.
 */

import * as fs from 'fs';
import * as path from 'path';

// Types
interface ScenarioMetrics {
  [key: string]: number;
}

interface ClinicalStory {
  patient_id: string;
  name: string;
  age: number;
  baseline: Record<string, any>;
  hdim_actions: string[];
  post_intervention: Record<string, any>;
  outcome: string;
}

interface Scenario {
  id: string;
  name: string;
  description: string;
  hedis_measures: string[];
  baseline_metrics: ScenarioMetrics;
  post_intervention_metrics: ScenarioMetrics;
  outcomes_summary: Record<string, string | number>;
  clinical_stories: ClinicalStory[];
  intervention_timeline: Array<{ month: number; action: string; description: string }>;
}

interface QualityMeasureResult {
  patientId: string;
  measureId: string;
  baseline: {
    inDenominator: boolean;
    inNumerator: boolean;
    complianceStatus: 'MET' | 'NOT_MET' | 'EXCLUDED';
  };
  postIntervention: {
    inDenominator: boolean;
    inNumerator: boolean;
    complianceStatus: 'MET' | 'NOT_MET' | 'EXCLUDED';
  };
  improvement: boolean;
}

interface ScenarioResult {
  scenarioId: string;
  scenarioName: string;
  executedAt: string;
  patientsProcessed: number;
  measureResults: QualityMeasureResult[];
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

// Scenario Runner Class
export class ScenarioRunner {
  private scenariosDir: string;
  private patientsDir: string;
  private resultsDir: string;
  private apiBaseUrl: string;

  constructor(basePath: string, apiBaseUrl: string = 'http://localhost:8087') {
    this.scenariosDir = path.join(basePath, 'scenarios');
    this.patientsDir = path.join(basePath, 'patients');
    this.resultsDir = path.join(basePath, 'results');
    this.apiBaseUrl = apiBaseUrl;
  }

  /**
   * Load a scenario definition
   */
  loadScenario(scenarioId: string): Scenario {
    const scenarioPath = path.join(this.scenariosDir, `${scenarioId}.json`);
    const content = fs.readFileSync(scenarioPath, 'utf-8');
    return JSON.parse(content);
  }

  /**
   * Load all available scenarios
   */
  listScenarios(): string[] {
    return fs.readdirSync(this.scenariosDir)
      .filter(f => f.endsWith('.json'))
      .map(f => f.replace('.json', ''));
  }

  /**
   * Execute a single scenario
   */
  async runScenario(scenarioId: string, options: {
    verbose?: boolean;
    simulateOnly?: boolean
  } = {}): Promise<ScenarioResult> {
    const { verbose = false, simulateOnly = true } = options;
    const scenario = this.loadScenario(scenarioId);

    if (verbose) {
      console.log(`\n${'='.repeat(60)}`);
      console.log(`SCENARIO: ${scenario.name}`);
      console.log(`${'='.repeat(60)}`);
      console.log(`Description: ${scenario.description}`);
      console.log(`HEDIS Measures: ${scenario.hedis_measures.join(', ')}`);
    }

    // Simulate processing based on scenario data
    const measureResults = this.simulateMeasureEvaluations(scenario);
    const aggregateOutcomes = this.calculateAggregateOutcomes(scenario);
    const clinicalImpact = this.calculateClinicalImpact(scenario);

    const result: ScenarioResult = {
      scenarioId: scenario.id,
      scenarioName: scenario.name,
      executedAt: new Date().toISOString(),
      patientsProcessed: scenario.baseline_metrics.population_size || 100,
      measureResults,
      aggregateOutcomes,
      clinicalImpact
    };

    if (verbose) {
      this.printResultsSummary(result, scenario);
    }

    // Save results
    this.saveResults(result);

    return result;
  }

  /**
   * Simulate measure evaluations for demo purposes
   */
  private simulateMeasureEvaluations(scenario: Scenario): QualityMeasureResult[] {
    const results: QualityMeasureResult[] = [];

    for (const story of scenario.clinical_stories) {
      for (const measure of scenario.hedis_measures) {
        const hasBaselineGap = story.baseline.care_gaps?.length > 0;
        const hasPostInterventionGap = story.post_intervention.care_gaps?.length > 0;

        results.push({
          patientId: story.patient_id,
          measureId: measure,
          baseline: {
            inDenominator: true,
            inNumerator: !hasBaselineGap,
            complianceStatus: hasBaselineGap ? 'NOT_MET' : 'MET'
          },
          postIntervention: {
            inDenominator: true,
            inNumerator: !hasPostInterventionGap,
            complianceStatus: hasPostInterventionGap ? 'NOT_MET' : 'MET'
          },
          improvement: hasBaselineGap && !hasPostInterventionGap
        });
      }
    }

    return results;
  }

  /**
   * Calculate aggregate outcomes from scenario data
   */
  private calculateAggregateOutcomes(scenario: Scenario): ScenarioResult['aggregateOutcomes'] {
    const baseline = scenario.baseline_metrics;
    const postIntervention = scenario.post_intervention_metrics;

    // Calculate average compliance rates from available metrics
    const baselineRates = Object.entries(baseline)
      .filter(([k]) => k.includes('rate'))
      .map(([, v]) => v);
    const postRates = Object.entries(postIntervention)
      .filter(([k]) => k.includes('rate'))
      .map(([, v]) => v);

    const avgBaseline = baselineRates.length > 0
      ? baselineRates.reduce((a, b) => a + b, 0) / baselineRates.length
      : 0.5;
    const avgPost = postRates.length > 0
      ? postRates.reduce((a, b) => a + b, 0) / postRates.length
      : 0.75;

    const baselineGaps = baseline.care_gaps_per_patient || 2;
    const postGaps = postIntervention.care_gaps_per_patient || 0.5;
    const population = baseline.population_size || 100;

    return {
      baselineComplianceRate: Math.round(avgBaseline * 100),
      postInterventionComplianceRate: Math.round(avgPost * 100),
      improvementPercentage: Math.round((avgPost - avgBaseline) * 100),
      careGapsClosedCount: Math.round((baselineGaps - postGaps) * population)
    };
  }

  /**
   * Calculate clinical impact metrics
   */
  private calculateClinicalImpact(scenario: Scenario): ScenarioResult['clinicalImpact'] {
    const summary = scenario.outcomes_summary;
    const population = scenario.baseline_metrics.population_size || 100;

    const costSavingsPerMember = typeof summary.estimated_cost_savings_per_member === 'number'
      ? summary.estimated_cost_savings_per_member
      : 1500;

    return {
      estimatedCostSavings: costSavingsPerMember * population,
      qualityScoreImprovement: this.extractPercentage(summary.care_gaps_reduction as string) || 25,
      patientOutcomesImproved: Math.round(population * 0.65)
    };
  }

  /**
   * Extract percentage from string like "+23%" or "-31%"
   */
  private extractPercentage(str: string): number {
    if (!str) return 0;
    const match = str.match(/([+-]?\d+)/);
    return match ? Math.abs(parseInt(match[1], 10)) : 0;
  }

  /**
   * Print formatted results summary
   */
  private printResultsSummary(result: ScenarioResult, scenario: Scenario): void {
    console.log(`\n${'─'.repeat(60)}`);
    console.log('BASELINE STATE (Before HDIM)');
    console.log(`${'─'.repeat(60)}`);

    for (const [key, value] of Object.entries(scenario.baseline_metrics)) {
      const label = this.formatMetricLabel(key);
      const formatted = this.formatMetricValue(key, value);
      console.log(`  ${label}: ${formatted}`);
    }

    console.log(`\n${'─'.repeat(60)}`);
    console.log('POST-INTERVENTION STATE (After HDIM)');
    console.log(`${'─'.repeat(60)}`);

    for (const [key, value] of Object.entries(scenario.post_intervention_metrics)) {
      const label = this.formatMetricLabel(key);
      const formatted = this.formatMetricValue(key, value);
      console.log(`  ${label}: ${formatted}`);
    }

    console.log(`\n${'─'.repeat(60)}`);
    console.log('OUTCOMES IMPACT');
    console.log(`${'─'.repeat(60)}`);

    for (const [key, value] of Object.entries(scenario.outcomes_summary)) {
      const label = this.formatMetricLabel(key);
      console.log(`  ${label}: ${value}`);
    }

    console.log(`\n${'─'.repeat(60)}`);
    console.log('PATIENT STORIES');
    console.log(`${'─'.repeat(60)}`);

    for (const story of scenario.clinical_stories) {
      console.log(`\n  Patient: ${story.name} (Age ${story.age})`);
      console.log(`  Baseline Care Gaps: ${story.baseline.care_gaps?.join(', ') || 'None'}`);
      console.log(`  HDIM Actions Taken:`);
      for (const action of story.hdim_actions.slice(0, 3)) {
        console.log(`    • ${action}`);
      }
      console.log(`  Post-Intervention: ${story.post_intervention.care_gaps?.length === 0 ? 'All gaps closed' : story.post_intervention.care_gaps?.join(', ')}`);
      console.log(`  Outcome: ${story.outcome}`);
    }

    console.log(`\n${'='.repeat(60)}`);
    console.log('SUMMARY');
    console.log(`${'='.repeat(60)}`);
    console.log(`  Compliance Rate: ${result.aggregateOutcomes.baselineComplianceRate}% → ${result.aggregateOutcomes.postInterventionComplianceRate}%`);
    console.log(`  Improvement: +${result.aggregateOutcomes.improvementPercentage}%`);
    console.log(`  Care Gaps Closed: ${result.aggregateOutcomes.careGapsClosedCount}`);
    console.log(`  Estimated Cost Savings: $${result.clinicalImpact.estimatedCostSavings.toLocaleString()}`);
    console.log(`  Patients with Improved Outcomes: ${result.clinicalImpact.patientOutcomesImproved}`);
  }

  /**
   * Format metric label for display
   */
  private formatMetricLabel(key: string): string {
    return key
      .replace(/_/g, ' ')
      .replace(/\b\w/g, l => l.toUpperCase());
  }

  /**
   * Format metric value for display
   */
  private formatMetricValue(key: string, value: number): string {
    if (key.includes('rate')) {
      return `${(value * 100).toFixed(1)}%`;
    }
    if (key.includes('per_1000')) {
      return `${value} per 1,000`;
    }
    if (key.includes('per_patient')) {
      return `${value.toFixed(1)} per patient`;
    }
    return value.toLocaleString();
  }

  /**
   * Save results to file
   */
  private saveResults(result: ScenarioResult): void {
    const filename = `${result.scenarioId}-${Date.now()}.json`;
    const filepath = path.join(this.resultsDir, filename);
    fs.writeFileSync(filepath, JSON.stringify(result, null, 2));
  }

  /**
   * Run all scenarios
   */
  async runAllScenarios(options: { verbose?: boolean } = {}): Promise<ScenarioResult[]> {
    const scenarios = this.listScenarios();
    const results: ScenarioResult[] = [];

    console.log('\n' + '═'.repeat(70));
    console.log('  HDIM PATIENT CARE OUTCOMES DEMONSTRATION');
    console.log('  Showing Real-World Impact of Healthcare Data Processing');
    console.log('═'.repeat(70));

    for (const scenarioId of scenarios) {
      const result = await this.runScenario(scenarioId, options);
      results.push(result);
    }

    // Print aggregate summary
    this.printAggregateSummary(results);

    return results;
  }

  /**
   * Print aggregate summary across all scenarios
   */
  private printAggregateSummary(results: ScenarioResult[]): void {
    const totalPatients = results.reduce((sum, r) => sum + r.patientsProcessed, 0);
    const totalGapsClosed = results.reduce((sum, r) => sum + r.aggregateOutcomes.careGapsClosedCount, 0);
    const totalSavings = results.reduce((sum, r) => sum + r.clinicalImpact.estimatedCostSavings, 0);
    const avgImprovement = results.reduce((sum, r) => sum + r.aggregateOutcomes.improvementPercentage, 0) / results.length;

    console.log('\n' + '═'.repeat(70));
    console.log('  AGGREGATE IMPACT ACROSS ALL SCENARIOS');
    console.log('═'.repeat(70));
    console.log(`  Scenarios Evaluated:       ${results.length}`);
    console.log(`  Total Patients Impacted:   ${totalPatients.toLocaleString()}`);
    console.log(`  Care Gaps Closed:          ${totalGapsClosed.toLocaleString()}`);
    console.log(`  Avg Quality Improvement:   +${avgImprovement.toFixed(1)}%`);
    console.log(`  Estimated Annual Savings:  $${totalSavings.toLocaleString()}`);
    console.log('═'.repeat(70) + '\n');
  }
}

// CLI Entry Point
if (require.main === module) {
  const args = process.argv.slice(2);
  const basePath = path.resolve(__dirname, '..');
  const runner = new ScenarioRunner(basePath);

  const scenarioArg = args.find(a => a.startsWith('--scenario='));
  const verbose = args.includes('--verbose') || args.includes('-v');

  if (scenarioArg) {
    const scenarioId = scenarioArg.split('=')[1];
    runner.runScenario(scenarioId, { verbose: true })
      .then(() => process.exit(0))
      .catch(err => {
        console.error('Error:', err);
        process.exit(1);
      });
  } else {
    runner.runAllScenarios({ verbose })
      .then(() => process.exit(0))
      .catch(err => {
        console.error('Error:', err);
        process.exit(1);
      });
  }
}

export default ScenarioRunner;
