import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, of } from 'rxjs';
import { map, catchError, tap, shareReplay } from 'rxjs/operators';
import { ApiService } from './api.service';
import { LoggerService, ContextualLogger } from './logger.service';
import { API_CONFIG } from '../config/api.config';

/**
 * Risk tier classification
 */
export type RiskTier = 'LOW' | 'MODERATE' | 'HIGH' | 'VERY_HIGH';

/**
 * Prediction factor type
 */
export type FactorType =
  | 'HISTORICAL_PATTERN'
  | 'APPOINTMENT_ADHERENCE'
  | 'MEDICATION_REFILLS'
  | 'SIMILAR_PATIENT_BEHAVIOR';

/**
 * Prediction factor with weighted contribution
 *
 * Standard weights (Issue #157):
 * - Historical Pattern: 40%
 * - Appointment Adherence: 25%
 * - Medication Refills: 20%
 * - Similar Patient Behavior: 15%
 */
export interface PredictionFactor {
  factorType: FactorType;
  name: string;
  description: string;
  weight: number;
  rawScore: number;
  contribution: number;
  context: string;
  isConcerning: boolean;
  concernThreshold: number;
}

/**
 * Predicted Care Gap
 *
 * Represents a care gap predicted to occur within 30 days
 */
export interface PredictedCareGap {
  id: string;
  tenantId: string;
  patientId: string;
  patientName: string;

  // Measure information
  measureId: string;
  measureName: string;
  measureCategory: string;

  // Prediction details
  riskScore: number;
  riskTier: RiskTier;
  confidence: number;
  predictedGapDate: string;
  daysUntilGap: number;

  // Prediction factors
  predictionFactors: PredictionFactor[];

  // Recommended interventions
  recommendedInterventions: string[];
  priorityIntervention: string;
  interventionSuccessRate: number;

  // Historical context
  previousGapsForMeasure: number;
  lastComplianceDate: string;
  daysSinceLastCompliance: number;

  // Similar patient behavior
  similarPatientPoolSize: number;
  similarPatientGapRate: number;

  // Metadata
  predictedAt: string;
  modelVersion: string;
  metadata: Record<string, unknown>;
}

/**
 * Dashboard statistics for predicted care gaps
 */
export interface PredictedGapStats {
  totalPredictions: number;
  urgentPredictions: number;
  averageRiskScore: number;
  byRiskTier: Record<RiskTier, number>;
  byMeasureCategory: Record<string, number>;
  interventionWindowClosing: number;
  topMeasures: Array<{ measureId: string; measureName: string; count: number }>;
}

/**
 * Filter configuration for predicted care gaps
 */
export interface PredictedGapFilter {
  riskTiers?: RiskTier[];
  measureCategories?: string[];
  minRiskScore?: number;
  maxDaysUntilGap?: number;
  patientSearch?: string;
}

/**
 * Predictive Care Gap Service
 *
 * Provides access to predicted care gaps using ML-powered prediction model.
 *
 * Issue #157: Implement Predictive Care Gap Detection
 */
@Injectable({
  providedIn: 'root',
})
export class PredictiveCareGapService {
  private readonly baseUrl = API_CONFIG.PREDICTIVE_ANALYTICS_URL || `${API_CONFIG.BASE_URL}/predictive-analytics`;
  private readonly logger: ContextualLogger;

  // Cache for predicted gaps
  private predictionsCache$ = new BehaviorSubject<PredictedCareGap[]>([]);
  private statsCache$ = new BehaviorSubject<PredictedGapStats | null>(null);
  private readonly cacheTimeout = 5 * 60 * 1000; // 5 minutes
  private lastFetchTime = 0;

  constructor(
    private http: HttpClient,
    private apiService: ApiService,
    loggerService: LoggerService
  ) {
    this.logger = loggerService.withContext('PredictiveCareGapService');
  }

  /**
   * Get predicted care gaps for a provider's patient panel
   */
  getPredictedGapsForProvider(
    providerId: string,
    providerData: Record<string, unknown>,
    refresh = false
  ): Observable<PredictedCareGap[]> {
    const now = Date.now();
    if (
      !refresh &&
      this.predictionsCache$.value.length > 0 &&
      now - this.lastFetchTime < this.cacheTimeout
    ) {
      return of(this.predictionsCache$.value);
    }

    const url = `${this.baseUrl}/api/v1/analytics/providers/${providerId}/predicted-gaps`;

    return this.apiService.post<PredictedCareGap[]>(url, providerData).pipe(
      map((gaps) => this.transformPredictions(gaps)),
      tap((gaps) => {
        this.predictionsCache$.next(gaps);
        this.lastFetchTime = now;
        this.updateStats(gaps);
        this.logger.info(`Loaded ${gaps.length} predicted care gaps for provider ${providerId}`);
      }),
      shareReplay(1),
      catchError((error) => {
        this.logger.error('Error fetching predicted care gaps for provider', { providerId, error });
        return of(this.getMockPredictions());
      })
    );
  }

  /**
   * Get predicted care gaps for a specific patient
   */
  getPredictedGapsForPatient(
    patientId: string,
    patientData: Record<string, unknown>
  ): Observable<PredictedCareGap[]> {
    const url = `${this.baseUrl}/api/v1/analytics/patients/${patientId}/predicted-gaps`;

    return this.apiService.post<PredictedCareGap[]>(url, patientData).pipe(
      map((gaps) => this.transformPredictions(gaps)),
      catchError((error) => {
        this.logger.error('Error fetching predicted care gaps for patient', { patientId, error });
        return of([]);
      })
    );
  }

  /**
   * Get dashboard statistics for predicted care gaps
   */
  getPredictedGapStats(): Observable<PredictedGapStats> {
    if (this.statsCache$.value) {
      return of(this.statsCache$.value);
    }

    // Calculate stats from cached predictions
    const predictions = this.predictionsCache$.value;
    if (predictions.length > 0) {
      const stats = this.calculateStats(predictions);
      this.statsCache$.next(stats);
      return of(stats);
    }

    return of(this.getEmptyStats());
  }

  /**
   * Apply client-side filtering to predicted care gaps
   */
  applyFilters(
    predictions: PredictedCareGap[],
    filter: PredictedGapFilter
  ): PredictedCareGap[] {
    let filtered = [...predictions];

    if (filter.riskTiers?.length) {
      filtered = filtered.filter((p) => filter.riskTiers!.includes(p.riskTier));
    }

    if (filter.measureCategories?.length) {
      filtered = filtered.filter((p) =>
        filter.measureCategories!.includes(p.measureCategory)
      );
    }

    if (filter.minRiskScore != null) {
      filtered = filtered.filter((p) => p.riskScore >= filter.minRiskScore!);
    }

    if (filter.maxDaysUntilGap != null) {
      filtered = filtered.filter(
        (p) => p.daysUntilGap <= filter.maxDaysUntilGap!
      );
    }

    if (filter.patientSearch) {
      const search = filter.patientSearch.toLowerCase();
      filtered = filtered.filter(
        (p) =>
          p.patientName.toLowerCase().includes(search) ||
          p.patientId.toLowerCase().includes(search)
      );
    }

    return filtered;
  }

  /**
   * Get urgent predictions (within 7 days, high risk)
   */
  getUrgentPredictions(predictions: PredictedCareGap[]): PredictedCareGap[] {
    return predictions.filter(
      (p) => p.daysUntilGap <= 7 && p.riskScore >= 70
    );
  }

  /**
   * Get predictions where intervention window is closing (within 14 days)
   */
  getInterventionWindowClosing(predictions: PredictedCareGap[]): PredictedCareGap[] {
    return predictions.filter(
      (p) => p.daysUntilGap <= 14 && p.riskScore >= 50
    );
  }

  /**
   * Get the primary contributing factor for a prediction
   */
  getPrimaryFactor(prediction: PredictedCareGap): PredictionFactor | null {
    if (!prediction.predictionFactors?.length) {
      return null;
    }
    return prediction.predictionFactors.reduce((max, factor) =>
      factor.contribution > max.contribution ? factor : max
    );
  }

  /**
   * Get display label for risk tier
   */
  getRiskTierLabel(tier: RiskTier): string {
    const labels: Record<RiskTier, string> = {
      LOW: 'Low Risk',
      MODERATE: 'Moderate Risk',
      HIGH: 'High Risk',
      VERY_HIGH: 'Very High Risk',
    };
    return labels[tier] || tier;
  }

  /**
   * Get display color for risk tier
   */
  getRiskTierColor(tier: RiskTier): string {
    const colors: Record<RiskTier, string> = {
      LOW: '#10b981',
      MODERATE: '#f59e0b',
      HIGH: '#ef4444',
      VERY_HIGH: '#7c2d12',
    };
    return colors[tier] || '#6b7280';
  }

  /**
   * Get factor type display label
   */
  getFactorTypeLabel(type: FactorType): string {
    const labels: Record<FactorType, string> = {
      HISTORICAL_PATTERN: 'Historical Pattern',
      APPOINTMENT_ADHERENCE: 'Appointment Adherence',
      MEDICATION_REFILLS: 'Medication Refills',
      SIMILAR_PATIENT_BEHAVIOR: 'Similar Patient Behavior',
    };
    return labels[type] || type;
  }

  /**
   * Invalidate all caches
   */
  invalidateCache(): void {
    this.predictionsCache$.next([]);
    this.statsCache$.next(null);
    this.lastFetchTime = 0;
  }

  /**
   * Get cached predictions observable
   */
  getCachedPredictions$(): Observable<PredictedCareGap[]> {
    return this.predictionsCache$.asObservable();
  }

  // Private helper methods

  private transformPredictions(predictions: PredictedCareGap[]): PredictedCareGap[] {
    return predictions.map((p) => this.transformPrediction(p));
  }

  private transformPrediction(prediction: PredictedCareGap): PredictedCareGap {
    return {
      ...prediction,
      riskScore: Math.round(prediction.riskScore * 10) / 10,
      confidence: Math.round(prediction.confidence * 100) / 100,
      interventionSuccessRate: Math.round(prediction.interventionSuccessRate * 100),
    };
  }

  private calculateStats(predictions: PredictedCareGap[]): PredictedGapStats {
    const byRiskTier: Record<RiskTier, number> = {
      LOW: 0,
      MODERATE: 0,
      HIGH: 0,
      VERY_HIGH: 0,
    };

    const byMeasureCategory: Record<string, number> = {};
    const measureCounts: Record<string, { id: string; name: string; count: number }> = {};

    let totalRiskScore = 0;

    for (const p of predictions) {
      byRiskTier[p.riskTier]++;

      if (!byMeasureCategory[p.measureCategory]) {
        byMeasureCategory[p.measureCategory] = 0;
      }
      byMeasureCategory[p.measureCategory]++;

      if (!measureCounts[p.measureId]) {
        measureCounts[p.measureId] = { id: p.measureId, name: p.measureName, count: 0 };
      }
      measureCounts[p.measureId].count++;

      totalRiskScore += p.riskScore;
    }

    const topMeasures = Object.values(measureCounts)
      .sort((a, b) => b.count - a.count)
      .slice(0, 5)
      .map((m) => ({ measureId: m.id, measureName: m.name, count: m.count }));

    return {
      totalPredictions: predictions.length,
      urgentPredictions: this.getUrgentPredictions(predictions).length,
      averageRiskScore: predictions.length > 0 ? totalRiskScore / predictions.length : 0,
      byRiskTier,
      byMeasureCategory,
      interventionWindowClosing: this.getInterventionWindowClosing(predictions).length,
      topMeasures,
    };
  }

  private updateStats(predictions: PredictedCareGap[]): void {
    const stats = this.calculateStats(predictions);
    this.statsCache$.next(stats);
  }

  private getEmptyStats(): PredictedGapStats {
    return {
      totalPredictions: 0,
      urgentPredictions: 0,
      averageRiskScore: 0,
      byRiskTier: { LOW: 0, MODERATE: 0, HIGH: 0, VERY_HIGH: 0 },
      byMeasureCategory: {},
      interventionWindowClosing: 0,
      topMeasures: [],
    };
  }

  /**
   * Get mock predictions for demo/development
   */
  private getMockPredictions(): PredictedCareGap[] {
    return [
      {
        id: 'pred-001',
        tenantId: 'DEMO_TENANT',
        patientId: 'patient-001',
        patientName: 'Maria Garcia',
        measureId: 'COL',
        measureName: 'Colorectal Cancer Screening',
        measureCategory: 'HEDIS',
        riskScore: 82.5,
        riskTier: 'HIGH',
        confidence: 0.87,
        predictedGapDate: this.getFutureDate(12),
        daysUntilGap: 12,
        predictionFactors: [
          {
            factorType: 'HISTORICAL_PATTERN',
            name: 'Historical Pattern Analysis',
            description: 'Based on patient\'s historical compliance patterns',
            weight: 0.40,
            rawScore: 0.85,
            contribution: 0.34,
            context: 'Patient has 2 previous gap(s) for this measure. Last compliance was 340 days ago.',
            isConcerning: true,
            concernThreshold: 0.7,
          },
          {
            factorType: 'APPOINTMENT_ADHERENCE',
            name: 'Appointment Adherence',
            description: 'Based on appointment attendance patterns',
            weight: 0.25,
            rawScore: 0.60,
            contribution: 0.15,
            context: 'Patient has 1 no-show(s) and 2 cancellation(s) in the past 12 months.',
            isConcerning: false,
            concernThreshold: 0.7,
          },
          {
            factorType: 'MEDICATION_REFILLS',
            name: 'Medication Refill Behavior',
            description: 'Based on prescription refill patterns',
            weight: 0.20,
            rawScore: 0.45,
            contribution: 0.09,
            context: 'Medication adherence rate: 78%. 1 missed refill(s) in the past 6 months.',
            isConcerning: false,
            concernThreshold: 0.7,
          },
          {
            factorType: 'SIMILAR_PATIENT_BEHAVIOR',
            name: 'Similar Patient Behavior',
            description: 'Based on outcomes of similar patients',
            weight: 0.15,
            rawScore: 0.72,
            contribution: 0.108,
            context: 'Compared to 245 similar patients with 32% gap rate.',
            isConcerning: true,
            concernThreshold: 0.7,
          },
        ],
        recommendedInterventions: [
          'Proactive outreach - patient has history of gaps',
          'Schedule preventive care visit',
          'Send appointment reminder',
        ],
        priorityIntervention: 'Proactive outreach - patient has history of gaps',
        interventionSuccessRate: 0.67,
        previousGapsForMeasure: 2,
        lastComplianceDate: this.getPastDate(340),
        daysSinceLastCompliance: 340,
        similarPatientPoolSize: 245,
        similarPatientGapRate: 0.32,
        predictedAt: new Date().toISOString(),
        modelVersion: '1.0.0',
        metadata: {},
      },
      {
        id: 'pred-002',
        tenantId: 'DEMO_TENANT',
        patientId: 'patient-002',
        patientName: 'James Thompson',
        measureId: 'CBP',
        measureName: 'Controlling High Blood Pressure',
        measureCategory: 'HEDIS',
        riskScore: 75.2,
        riskTier: 'HIGH',
        confidence: 0.82,
        predictedGapDate: this.getFutureDate(18),
        daysUntilGap: 18,
        predictionFactors: [
          {
            factorType: 'HISTORICAL_PATTERN',
            name: 'Historical Pattern Analysis',
            description: 'Based on patient\'s historical compliance patterns',
            weight: 0.40,
            rawScore: 0.75,
            contribution: 0.30,
            context: 'Patient has 1 previous gap(s) for this measure. Last compliance was 280 days ago.',
            isConcerning: true,
            concernThreshold: 0.7,
          },
          {
            factorType: 'APPOINTMENT_ADHERENCE',
            name: 'Appointment Adherence',
            description: 'Based on appointment attendance patterns',
            weight: 0.25,
            rawScore: 0.55,
            contribution: 0.1375,
            context: 'Patient has 2 no-show(s) and 1 cancellation(s) in the past 12 months.',
            isConcerning: false,
            concernThreshold: 0.7,
          },
          {
            factorType: 'MEDICATION_REFILLS',
            name: 'Medication Refill Behavior',
            description: 'Based on prescription refill patterns',
            weight: 0.20,
            rawScore: 0.70,
            contribution: 0.14,
            context: 'Medication adherence rate: 65%. 2 missed refill(s) in the past 6 months.',
            isConcerning: true,
            concernThreshold: 0.7,
          },
          {
            factorType: 'SIMILAR_PATIENT_BEHAVIOR',
            name: 'Similar Patient Behavior',
            description: 'Based on outcomes of similar patients',
            weight: 0.15,
            rawScore: 0.58,
            contribution: 0.087,
            context: 'Compared to 312 similar patients with 28% gap rate.',
            isConcerning: false,
            concernThreshold: 0.7,
          },
        ],
        recommendedInterventions: [
          'Pharmacy coordination for medication sync',
          'Schedule chronic care follow-up',
          'Review medication adherence',
        ],
        priorityIntervention: 'Pharmacy coordination for medication sync',
        interventionSuccessRate: 0.58,
        previousGapsForMeasure: 1,
        lastComplianceDate: this.getPastDate(280),
        daysSinceLastCompliance: 280,
        similarPatientPoolSize: 312,
        similarPatientGapRate: 0.28,
        predictedAt: new Date().toISOString(),
        modelVersion: '1.0.0',
        metadata: {},
      },
      {
        id: 'pred-003',
        tenantId: 'DEMO_TENANT',
        patientId: 'patient-003',
        patientName: 'Robert Chen',
        measureId: 'CDC-E',
        measureName: 'Diabetes HbA1c Control',
        measureCategory: 'HEDIS',
        riskScore: 68.8,
        riskTier: 'MODERATE',
        confidence: 0.79,
        predictedGapDate: this.getFutureDate(25),
        daysUntilGap: 25,
        predictionFactors: [
          {
            factorType: 'HISTORICAL_PATTERN',
            name: 'Historical Pattern Analysis',
            description: 'Based on patient\'s historical compliance patterns',
            weight: 0.40,
            rawScore: 0.65,
            contribution: 0.26,
            context: 'Patient has 1 previous gap(s) for this measure. Last compliance was 320 days ago.',
            isConcerning: false,
            concernThreshold: 0.7,
          },
          {
            factorType: 'APPOINTMENT_ADHERENCE',
            name: 'Appointment Adherence',
            description: 'Based on appointment attendance patterns',
            weight: 0.25,
            rawScore: 0.72,
            contribution: 0.18,
            context: 'Patient has 3 no-show(s) and 2 cancellation(s) in the past 12 months.',
            isConcerning: true,
            concernThreshold: 0.7,
          },
          {
            factorType: 'MEDICATION_REFILLS',
            name: 'Medication Refill Behavior',
            description: 'Based on prescription refill patterns',
            weight: 0.20,
            rawScore: 0.55,
            contribution: 0.11,
            context: 'Medication adherence rate: 72%. 1 missed refill(s) in the past 6 months.',
            isConcerning: false,
            concernThreshold: 0.7,
          },
          {
            factorType: 'SIMILAR_PATIENT_BEHAVIOR',
            name: 'Similar Patient Behavior',
            description: 'Based on outcomes of similar patients',
            weight: 0.15,
            rawScore: 0.62,
            contribution: 0.093,
            context: 'Compared to 189 similar patients with 35% gap rate.',
            isConcerning: false,
            concernThreshold: 0.7,
          },
        ],
        recommendedInterventions: [
          'Implement multiple reminder strategy (call + SMS + email)',
          'Schedule chronic care follow-up',
          'Review medication adherence',
        ],
        priorityIntervention: 'Implement multiple reminder strategy (call + SMS + email)',
        interventionSuccessRate: 0.62,
        previousGapsForMeasure: 1,
        lastComplianceDate: this.getPastDate(320),
        daysSinceLastCompliance: 320,
        similarPatientPoolSize: 189,
        similarPatientGapRate: 0.35,
        predictedAt: new Date().toISOString(),
        modelVersion: '1.0.0',
        metadata: {},
      },
    ];
  }

  private getFutureDate(days: number): string {
    const date = new Date();
    date.setDate(date.getDate() + days);
    return date.toISOString().split('T')[0];
  }

  private getPastDate(days: number): string {
    const date = new Date();
    date.setDate(date.getDate() - days);
    return date.toISOString().split('T')[0];
  }
}
