import {
  CqlEvaluation,
  BatchEvaluationResponse,
  EvaluationResponse,
  EvaluationStatus,
} from '../../app/models/evaluation.model';
import {
  QualityMeasureResult,
  QualityScore,
  QualityReport,
  PopulationQualityReport,
  MeasureSummary,
  GapInCare,
  MeasureCategory,
} from '../../app/models/quality-result.model';

/**
 * Factory for creating mock Evaluation and Quality Result objects for testing
 */
export class EvaluationFactory {
  private static counter = 1;

  /**
   * Create a CQL Evaluation result
   */
  static createCqlEvaluation(overrides?: Partial<CqlEvaluation>): CqlEvaluation {
    const id = `eval-${this.counter++}`;
    return {
      id: overrides?.id || id,
      tenantId: overrides?.tenantId || 'tenant-1',
      library: overrides?.library || {
        id: 'lib-1',
        name: 'HEDIS-CDC',
        version: '1.0.0',
      },
      patientId: overrides?.patientId || 'patient-001',
      contextData: overrides?.contextData,
      evaluationResult: overrides?.evaluationResult || {
        InDenominator: true,
        InNumerator: true,
      },
      status: overrides?.status || 'SUCCESS',
      errorMessage: overrides?.errorMessage,
      durationMs: overrides?.durationMs || 123,
      evaluationDate: overrides?.evaluationDate || '2024-01-15T10:00:00Z',
      createdAt: overrides?.createdAt || '2024-01-15T10:00:00Z',
    };
  }

  /**
   * Create successful evaluation
   */
  static createSuccessfulEvaluation(): CqlEvaluation {
    return this.createCqlEvaluation({
      status: 'SUCCESS',
      evaluationResult: { InDenominator: true, InNumerator: true },
    });
  }

  /**
   * Create failed evaluation
   */
  static createFailedEvaluation(): CqlEvaluation {
    return this.createCqlEvaluation({
      status: 'FAILED',
      errorMessage: 'Evaluation failed: Invalid patient data',
      evaluationResult: {},
    });
  }

  /**
   * Create evaluation response
   */
  static createEvaluationResponse(
    overrides?: Partial<EvaluationResponse>
  ): EvaluationResponse {
    return {
      evaluationId: overrides?.evaluationId || `eval-${this.counter++}`,
      status: overrides?.status || 'SUCCESS',
      result: overrides?.result || { InDenominator: true, InNumerator: true },
      durationMs: overrides?.durationMs || 123,
    };
  }

  /**
   * Create batch evaluation response (array of CqlEvaluation)
   * Note: Backend returns List<CqlEvaluation> directly, not wrapped object
   */
  static createBatchResponse(count = 3): BatchEvaluationResponse {
    return Array.from({ length: count }, () => this.createCqlEvaluation());
  }

  /**
   * Create QualityMeasureResult - compliant patient
   */
  static createCompliantResult(): QualityMeasureResult {
    return {
      id: `result-${this.counter++}`,
      tenantId: 'tenant-1',
      patientId: 'patient-001',
      measureId: 'CDC-A1C9',
      measureName: 'HEDIS-CDC',
      measureCategory: 'HEDIS',
      measureYear: 2024,
      numeratorCompliant: true,
      denominatorEligible: true,
      complianceRate: 100.0,
      score: 100,
      calculationDate: '2024-01-15T10:00:00Z',
      cqlLibrary: 'HEDIS-CDC-1.0.0',
      createdAt: '2024-01-15T10:00:00Z',
      createdBy: 'system',
      version: 1,
    };
  }

  /**
   * Create QualityMeasureResult - non-compliant patient
   */
  static createNonCompliantResult(): QualityMeasureResult {
    return {
      id: `result-${this.counter++}`,
      tenantId: 'tenant-1',
      patientId: 'patient-002',
      measureId: 'CDC-A1C9',
      measureName: 'HEDIS-CDC',
      measureCategory: 'HEDIS',
      measureYear: 2024,
      numeratorCompliant: false,
      denominatorEligible: true,
      complianceRate: 0.0,
      score: 0,
      calculationDate: '2024-01-15T10:00:00Z',
      cqlLibrary: 'HEDIS-CDC-1.0.0',
      createdAt: '2024-01-15T10:00:00Z',
      createdBy: 'system',
      version: 1,
    };
  }

  /**
   * Create QualityMeasureResult - not eligible
   */
  static createNotEligibleResult(): QualityMeasureResult {
    return {
      id: `result-${this.counter++}`,
      tenantId: 'tenant-1',
      patientId: 'patient-003',
      measureId: 'CDC-A1C9',
      measureName: 'HEDIS-CDC',
      measureCategory: 'HEDIS',
      measureYear: 2024,
      numeratorCompliant: false,
      denominatorEligible: false,
      complianceRate: 0.0,
      score: 0,
      calculationDate: '2024-01-15T10:00:00Z',
      cqlLibrary: 'HEDIS-CDC-1.0.0',
      createdAt: '2024-01-15T10:00:00Z',
      createdBy: 'system',
      version: 1,
    };
  }

  /**
   * Create QualityScore for a patient
   */
  static createQualityScore(overrides?: Partial<QualityScore>): QualityScore {
    return {
      totalMeasures: overrides?.totalMeasures || 5,
      compliantMeasures: overrides?.compliantMeasures || 4,
      scorePercentage: overrides?.scorePercentage || 80.0,
    };
  }

  /**
   * Create QualityReport for a patient
   */
  static createPatientReport(): QualityReport {
    return {
      patientId: 'patient-001',
      patientName: 'John Doe',
      reportDate: '2024-01-15T10:00:00Z',
      overallScore: {
        totalMeasures: 5,
        compliantMeasures: 4,
        scorePercentage: 80.0,
      },
      measureResults: [
        this.createCompliantResult(),
        this.createNonCompliantResult(),
      ],
      gapsInCare: [
        {
          measureId: 'CDC-A1C9',
          measureName: 'HEDIS-CDC',
          description: 'HbA1c test not completed',
          recommendation: 'Schedule HbA1c test',
        },
      ],
    };
  }

  /**
   * Create PopulationQualityReport
   */
  static createPopulationReport(): PopulationQualityReport {
    return {
      year: 2024,
      totalPatients: 1000,
      reportDate: '2024-01-15T10:00:00Z',
      overallCompliance: 75.0,
      measureSummaries: [
        {
          measureId: 'CDC-A1C9',
          measureName: 'HEDIS-CDC',
          totalEligible: 1000,
          totalCompliant: 800,
          complianceRate: 80.0,
        },
        {
          measureId: 'CBP-BP',
          measureName: 'HEDIS-CBP',
          totalEligible: 1000,
          totalCompliant: 750,
          complianceRate: 75.0,
        },
      ],
    };
  }

  /**
   * Create multiple quality measure results
   */
  static createMany(count: number, overrides?: Partial<QualityMeasureResult>): QualityMeasureResult[] {
    return Array.from({ length: count }, () => {
      // Randomize compliance for variety
      const isCompliant = Math.random() > 0.5;
      return isCompliant ? this.createCompliantResult() : this.createNonCompliantResult();
    });
  }

  /**
   * Reset counter for tests
   */
  static reset(): void {
    this.counter = 1;
  }
}
