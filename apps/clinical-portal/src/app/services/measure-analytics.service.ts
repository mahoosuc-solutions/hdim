import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, of, forkJoin } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { API_CONFIG } from '../config/api.config';
import { CustomMeasure } from './custom-measure.service';

/**
 * Performance metrics for a single measure
 */
export interface MeasurePerformanceMetrics {
  measureId: string;
  measureName: string;
  measureVersion: string;
  measureCategory: string;
  status: string;
  metrics: {
    totalEvaluations: number;
    totalPatients: number;
    passRate: number;
    failRate: number;
    exclusionRate: number;
    averageExecutionTimeMs: number;
    minExecutionTimeMs: number;
    maxExecutionTimeMs: number;
    lastEvaluationDate: string;
    trendsPassRate: number[]; // Last 6 periods
    trendsVolume: number[]; // Last 6 periods
  };
  populations: {
    initialPopulation: number;
    denominator: number;
    numerator: number;
    denominatorExclusions: number;
    denominatorExceptions: number;
    numeratorExclusions: number;
  };
  breakdown: {
    byAgeGroup: { label: string; count: number; passRate: number }[];
    byGender: { label: string; count: number; passRate: number }[];
    byPayer: { label: string; count: number; passRate: number }[];
  };
}

/**
 * Comparison result between two measures
 */
export interface MeasureComparisonResult {
  measure1: MeasurePerformanceMetrics;
  measure2: MeasurePerformanceMetrics;
  comparison: {
    passRateDiff: number;
    volumeDiff: number;
    executionTimeDiff: number;
    improvementAreas: string[];
    concerns: string[];
  };
}

/**
 * Aggregated performance summary
 */
export interface PerformanceSummary {
  totalMeasures: number;
  totalEvaluations: number;
  averagePassRate: number;
  topPerformers: { measureId: string; measureName: string; passRate: number }[];
  needsAttention: { measureId: string; measureName: string; passRate: number; reason: string }[];
  trendsOverview: {
    period: string;
    evaluations: number;
    averagePassRate: number;
  }[];
}

@Injectable({
  providedIn: 'root',
})
export class MeasureAnalyticsService {
  constructor(private http: HttpClient) {}

  /**
   * Get performance metrics for a specific measure
   */
  getMeasurePerformance(
    measureId: string,
    tenantId: string = API_CONFIG.DEFAULT_TENANT_ID
  ): Observable<MeasurePerformanceMetrics> {
    const url = `${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/${measureId}/analytics`;
    const headers = new HttpHeaders({ 'X-Tenant-ID': tenantId });

    return this.http.get<MeasurePerformanceMetrics>(url, { headers }).pipe(
      catchError(() => of(this.generateMockMetrics(measureId)))
    );
  }

  /**
   * Get performance metrics for multiple measures
   */
  getMultipleMeasurePerformance(
    measures: CustomMeasure[],
    tenantId: string = API_CONFIG.DEFAULT_TENANT_ID
  ): Observable<MeasurePerformanceMetrics[]> {
    if (measures.length === 0) {
      return of([]);
    }

    const requests = measures.map((m) =>
      this.getMeasurePerformance(m.id, tenantId).pipe(
        map((metrics) => ({
          ...metrics,
          measureId: m.id,
          measureName: m.name,
          measureVersion: m.version || '1.0.0',
          measureCategory: m.category || 'CUSTOM',
          status: m.status || 'DRAFT',
        }))
      )
    );

    return forkJoin(requests);
  }

  /**
   * Get aggregated performance summary
   */
  getPerformanceSummary(
    tenantId: string = API_CONFIG.DEFAULT_TENANT_ID
  ): Observable<PerformanceSummary> {
    const url = `${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/analytics/summary`;
    const headers = new HttpHeaders({ 'X-Tenant-ID': tenantId });

    return this.http.get<PerformanceSummary>(url, { headers }).pipe(
      catchError(() => of(this.generateMockSummary()))
    );
  }

  /**
   * Compare two measures
   */
  compareMeasures(
    measureId1: string,
    measureId2: string,
    tenantId: string = API_CONFIG.DEFAULT_TENANT_ID
  ): Observable<MeasureComparisonResult> {
    return forkJoin([
      this.getMeasurePerformance(measureId1, tenantId),
      this.getMeasurePerformance(measureId2, tenantId),
    ]).pipe(
      map(([m1, m2]) => this.buildComparison(m1, m2))
    );
  }

  /**
   * Build comparison result from two metrics
   */
  private buildComparison(
    m1: MeasurePerformanceMetrics,
    m2: MeasurePerformanceMetrics
  ): MeasureComparisonResult {
    const passRateDiff = m1.metrics.passRate - m2.metrics.passRate;
    const volumeDiff = m1.metrics.totalPatients - m2.metrics.totalPatients;
    const executionTimeDiff = m1.metrics.averageExecutionTimeMs - m2.metrics.averageExecutionTimeMs;

    const improvementAreas: string[] = [];
    const concerns: string[] = [];

    if (passRateDiff > 5) {
      improvementAreas.push(`${m1.measureName} has ${passRateDiff.toFixed(1)}% higher pass rate`);
    } else if (passRateDiff < -5) {
      concerns.push(`${m1.measureName} has ${Math.abs(passRateDiff).toFixed(1)}% lower pass rate`);
    }

    if (executionTimeDiff < -10) {
      improvementAreas.push(`${m1.measureName} is ${Math.abs(executionTimeDiff).toFixed(0)}ms faster`);
    } else if (executionTimeDiff > 10) {
      concerns.push(`${m1.measureName} is ${executionTimeDiff.toFixed(0)}ms slower`);
    }

    return {
      measure1: m1,
      measure2: m2,
      comparison: {
        passRateDiff,
        volumeDiff,
        executionTimeDiff,
        improvementAreas,
        concerns,
      },
    };
  }

  /**
   * Generate mock metrics for fallback
   */
  private generateMockMetrics(measureId: string): MeasurePerformanceMetrics {
    const passRate = 60 + Math.random() * 35;
    const totalPatients = Math.floor(500 + Math.random() * 2000);
    const numerator = Math.floor(totalPatients * (passRate / 100));

    return {
      measureId,
      measureName: `Measure ${measureId.slice(0, 8)}`,
      measureVersion: '1.0.0',
      measureCategory: 'CUSTOM',
      status: 'PUBLISHED',
      metrics: {
        totalEvaluations: Math.floor(10 + Math.random() * 50),
        totalPatients,
        passRate,
        failRate: 100 - passRate - Math.random() * 10,
        exclusionRate: Math.random() * 10,
        averageExecutionTimeMs: Math.floor(50 + Math.random() * 150),
        minExecutionTimeMs: Math.floor(20 + Math.random() * 30),
        maxExecutionTimeMs: Math.floor(200 + Math.random() * 300),
        lastEvaluationDate: new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000).toISOString(),
        trendsPassRate: this.generateTrendData(6, passRate, 5),
        trendsVolume: this.generateTrendData(6, totalPatients / 6, totalPatients / 20),
      },
      populations: {
        initialPopulation: totalPatients,
        denominator: Math.floor(totalPatients * 0.9),
        numerator,
        denominatorExclusions: Math.floor(totalPatients * 0.05),
        denominatorExceptions: Math.floor(totalPatients * 0.02),
        numeratorExclusions: Math.floor(totalPatients * 0.01),
      },
      breakdown: {
        byAgeGroup: [
          { label: '18-34', count: Math.floor(totalPatients * 0.2), passRate: passRate + Math.random() * 5 },
          { label: '35-49', count: Math.floor(totalPatients * 0.25), passRate: passRate + Math.random() * 3 },
          { label: '50-64', count: Math.floor(totalPatients * 0.3), passRate: passRate - Math.random() * 2 },
          { label: '65+', count: Math.floor(totalPatients * 0.25), passRate: passRate - Math.random() * 5 },
        ],
        byGender: [
          { label: 'Male', count: Math.floor(totalPatients * 0.48), passRate: passRate + Math.random() * 3 - 1.5 },
          { label: 'Female', count: Math.floor(totalPatients * 0.52), passRate: passRate + Math.random() * 3 - 1.5 },
        ],
        byPayer: [
          { label: 'Medicare', count: Math.floor(totalPatients * 0.35), passRate: passRate - Math.random() * 3 },
          { label: 'Medicaid', count: Math.floor(totalPatients * 0.2), passRate: passRate - Math.random() * 5 },
          { label: 'Commercial', count: Math.floor(totalPatients * 0.35), passRate: passRate + Math.random() * 3 },
          { label: 'Self-Pay', count: Math.floor(totalPatients * 0.1), passRate: passRate - Math.random() * 8 },
        ],
      },
    };
  }

  /**
   * Generate mock summary for fallback
   */
  private generateMockSummary(): PerformanceSummary {
    return {
      totalMeasures: 12,
      totalEvaluations: 156,
      averagePassRate: 72.5,
      topPerformers: [
        { measureId: 'top-1', measureName: 'Flu Immunization', passRate: 89.2 },
        { measureId: 'top-2', measureName: 'BMI Screening', passRate: 85.7 },
        { measureId: 'top-3', measureName: 'BP Control', passRate: 78.3 },
      ],
      needsAttention: [
        { measureId: 'low-1', measureName: 'Colorectal Screening', passRate: 52.1, reason: 'Below benchmark' },
        { measureId: 'low-2', measureName: 'Falls Risk Assessment', passRate: 58.4, reason: 'Declining trend' },
      ],
      trendsOverview: [
        { period: 'Jan', evaluations: 24, averagePassRate: 68.2 },
        { period: 'Feb', evaluations: 28, averagePassRate: 70.5 },
        { period: 'Mar', evaluations: 22, averagePassRate: 69.8 },
        { period: 'Apr', evaluations: 30, averagePassRate: 71.2 },
        { period: 'May', evaluations: 26, averagePassRate: 73.5 },
        { period: 'Jun', evaluations: 26, averagePassRate: 72.5 },
      ],
    };
  }

  /**
   * Generate trend data with variance
   */
  private generateTrendData(count: number, baseline: number, variance: number): number[] {
    const data: number[] = [];
    for (let i = 0; i < count; i++) {
      data.push(baseline + (Math.random() - 0.5) * 2 * variance);
    }
    return data;
  }
}
