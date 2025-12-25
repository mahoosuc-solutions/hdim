import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, forkJoin, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { CacheableService } from '../shared/cacheable.service';
import { LoggerService, ContextualLogger } from '../logger.service';
import { PhysicalHealthService } from './physical-health.service';
import { MentalHealthService } from './mental-health.service';
import { SDOHService } from './sdoh.service';
import {
  API_CONFIG,
  QUALITY_MEASURE_ENDPOINTS,
  buildQualityMeasureUrl,
  HTTP_HEADERS,
} from '../../config/api.config';
import {
  HealthScore,
  HealthScoreHistory,
  HealthScoreHistoryPoint,
  HealthScoreTrend,
  HealthStatus,
  PhysicalHealthSummary,
  MentalHealthSummary,
  SDOHSummary,
  QualityMeasurePerformance,
} from '../../models/patient-health.model';

/**
 * Health Scoring Service
 *
 * Calculates composite health scores from multiple domains:
 * - Physical health (40% weight)
 * - Mental health (30% weight)
 * - Social health (15% weight)
 * - Preventive care (15% weight)
 *
 * Features:
 * - Built-in caching with 5-minute TTL
 * - Historical trend analysis
 * - Status determination based on score thresholds
 * - Backend integration with fallback calculation
 */
@Injectable({
  providedIn: 'root',
})
export class HealthScoringService extends CacheableService {
  private log: ContextualLogger;

  // Component weights for overall health score
  private readonly WEIGHTS = {
    physical: 0.4,
    mental: 0.3,
    social: 0.15,
    preventive: 0.15,
  };

  constructor(
    private http: HttpClient,
    private physicalHealth: PhysicalHealthService,
    private mentalHealth: MentalHealthService,
    private sdoh: SDOHService,
    private logger: LoggerService
  ) {
    super({ ttlMs: 5 * 60 * 1000 }); // 5 minute cache
    this.log = this.logger.withContext('HealthScoringService');
  }

  /**
   * Get HTTP headers with tenant ID
   */
  private getHeaders(): HttpHeaders {
    return new HttpHeaders({
      [HTTP_HEADERS.TENANT_ID]: API_CONFIG.DEFAULT_TENANT_ID,
      [HTTP_HEADERS.CONTENT_TYPE]: 'application/json',
    });
  }

  /**
   * Get health score from backend with caching
   * Falls back to local calculation if backend unavailable
   */
  getHealthScore(patientId: string): Observable<HealthScore> {
    const cacheKey = `health:score:${patientId}`;
    const cached = this.getCached<HealthScore>(cacheKey);
    if (cached) return of(cached);

    const url = buildQualityMeasureUrl(
      QUALITY_MEASURE_ENDPOINTS.PATIENT_HEALTH_SCORE(patientId)
    );

    return this.http.get<any>(url, { headers: this.getHeaders() }).pipe(
      map((response) => {
        const healthScore: HealthScore = {
          patientId: response.patientId || patientId,
          overallScore: response.overallScore || response.score,
          score: response.score || response.overallScore,
          status: response.status,
          trend: response.trend || 'unknown',
          components: {
            physical: response.components.physical,
            mental: response.components.mental,
            social: response.components.social,
            preventive: response.components.preventive,
            chronicDisease: response.components.chronicDisease || 0,
          },
          calculatedAt: new Date(response.calculatedAt || new Date()),
          lastCalculated: new Date(response.calculatedAt || new Date()),
        };

        this.setCache(cacheKey, healthScore);
        return healthScore;
      }),
      catchError((error) => {
        this.log.error('Error fetching health score from backend:', error);
        return this.calculateHealthScore(patientId);
      })
    );
  }

  /**
   * Calculate health score locally from component services
   */
  calculateHealthScore(patientId: string): Observable<HealthScore> {
    return forkJoin({
      physical: this.physicalHealth.getPhysicalHealthSummary(patientId),
      mental: this.mentalHealth.getMentalHealthSummary(patientId),
      sdohData: this.sdoh.getSDOHSummary(patientId),
    }).pipe(
      map(({ physical, mental, sdohData }) => {
        // Calculate component scores
        const physicalScore = this.calculatePhysicalScore(physical);
        const mentalScore = this.calculateMentalScore(mental);
        const socialScore = this.calculateSocialScore(sdohData);
        const preventiveScore = 75; // Default - would come from quality measures

        // Calculate weighted overall score
        const overallScore = this.calculateWeightedHealthScore({
          physical: physicalScore,
          mental: mentalScore,
          social: socialScore,
          preventive: preventiveScore,
        });

        // Determine status
        const status = this.determineHealthStatus(overallScore);

        const healthScore: HealthScore = {
          patientId,
          overallScore,
          score: overallScore,
          status,
          trend: 'stable',
          components: {
            physical: physicalScore,
            mental: mentalScore,
            social: socialScore,
            preventive: preventiveScore,
            chronicDisease: physicalScore, // Derived from physical for now
          },
          calculatedAt: new Date(),
          lastCalculated: new Date(),
        };

        return healthScore;
      }),
      catchError((error) => {
        this.log.error('Error calculating health score:', error);
        return of(this.getMockHealthScore(patientId));
      })
    );
  }

  /**
   * Get health score history from backend
   */
  getHealthScoreHistory(patientId: string): Observable<HealthScoreHistory[]> {
    const url = buildQualityMeasureUrl(
      QUALITY_MEASURE_ENDPOINTS.PATIENT_HEALTH_SCORE_HISTORY(patientId)
    );

    return this.http
      .get<any[]>(url, {
        headers: this.getHeaders(),
      })
      .pipe(
        map((response) =>
          response.map((point) => ({
            score: point.score,
            calculatedAt: new Date(point.calculatedAt),
            trigger: point.trigger || 'scheduled',
          }))
        ),
        catchError((error) => {
          this.log.error('Error fetching health score history:', error);
          return of([]);
        })
      );
  }

  /**
   * Get detailed health score history with components
   */
  getHealthScoreHistoryDetailed(
    patientId: string,
    months: number = 12
  ): Observable<HealthScoreHistoryPoint[]> {
    const url = buildQualityMeasureUrl(
      QUALITY_MEASURE_ENDPOINTS.PATIENT_HEALTH_SCORE_HISTORY(patientId)
    );

    const params = new HttpParams().set('months', months.toString());

    return this.http
      .get<any[]>(url, {
        headers: this.getHeaders(),
        params,
      })
      .pipe(
        map((response) =>
          response.map((point) => ({
            date: new Date(point.date || point.calculatedAt),
            score: point.score,
            status: point.status || this.determineHealthStatus(point.score),
            components: point.components || {
              physical: 0,
              mental: 0,
              social: 0,
              preventive: 0,
            },
          }))
        ),
        catchError((error) => {
          this.log.error('Error fetching health score history:', error);
          return of([]);
        })
      );
  }

  /**
   * Calculate health score trend from historical data
   */
  calculateHealthScoreTrend(history: HealthScoreHistoryPoint[]): HealthScoreTrend {
    if (history.length < 2) {
      return {
        direction: 'stable',
        percentChange: 0,
        pointsChange: 0,
      };
    }

    // Sort by date (oldest to newest)
    const sortedHistory = [...history].sort(
      (a, b) => a.date.getTime() - b.date.getTime()
    );

    const oldestScore = sortedHistory[0].score;
    const newestScore = sortedHistory[sortedHistory.length - 1].score;

    const pointsChange = newestScore - oldestScore;
    const percentChange = (pointsChange / oldestScore) * 100;

    // Determine direction based on threshold
    let direction: 'improving' | 'stable' | 'declining';

    if (Math.abs(percentChange) < 5 && Math.abs(pointsChange) < 5) {
      direction = 'stable';
    } else if (pointsChange > 0) {
      direction = 'improving';
    } else {
      direction = 'declining';
    }

    return {
      direction,
      percentChange,
      pointsChange,
    };
  }

  /**
   * Calculate weighted health score from components
   */
  calculateWeightedHealthScore(components: {
    physical: number;
    mental: number;
    social: number;
    preventive: number;
  }): number {
    return Math.round(
      components.physical * this.WEIGHTS.physical +
        components.mental * this.WEIGHTS.mental +
        components.social * this.WEIGHTS.social +
        components.preventive * this.WEIGHTS.preventive
    );
  }

  /**
   * Determine health status from numeric score
   */
  determineHealthStatus(score: number): HealthStatus {
    if (score >= 80) return 'excellent';
    if (score >= 60) return 'good';
    if (score >= 40) return 'fair';
    return 'poor';
  }

  /**
   * Invalidate health score cache for a patient
   */
  invalidateHealthScoreCache(patientId: string): void {
    this.invalidatePatientCache(patientId);
  }

  // ===== Private Helper Methods =====

  /**
   * Calculate physical health score
   */
  private calculatePhysicalScore(physical: PhysicalHealthSummary): number {
    let score = 100;

    // Deduct points for abnormal vitals
    if (physical.vitals?.bloodPressure?.status === 'abnormal' ||
        physical.vitals?.bloodPressure?.status === 'warning') {
      score -= 10;
    }
    if (physical.vitals?.bloodPressure?.status === 'critical') {
      score -= 20;
    }
    if (physical.vitals?.bmi?.status === 'abnormal' ||
        physical.vitals?.bmi?.status === 'warning') {
      score -= 5;
    }

    // Deduct points for chronic conditions
    const severeConditions = physical.chronicConditions.filter(
      (c) => c.severity === 'severe' && !c.isControlled
    );
    score -= severeConditions.length * 15;

    const moderateConditions = physical.chronicConditions.filter(
      (c) => c.severity === 'moderate' && !c.isControlled
    );
    score -= moderateConditions.length * 8;

    // Deduct points for poor medication adherence
    if (physical.medicationAdherence?.status === 'poor') {
      score -= 15;
    }

    // Deduct points for functional limitations
    if (physical.functionalStatus?.adlScore < 6) {
      score -= 10;
    }
    if (physical.functionalStatus?.painLevel > 6) {
      score -= 10;
    }

    return Math.max(0, Math.min(100, score));
  }

  /**
   * Calculate mental health score
   */
  private calculateMentalScore(mental: MentalHealthSummary): number {
    let score = 100;

    // Deduct points based on mental health assessments
    for (const assessment of mental.assessments) {
      if (assessment.severity === 'severe') score -= 30;
      else if (assessment.severity === 'moderately-severe') score -= 25;
      else if (assessment.severity === 'moderate') score -= 15;
      else if (assessment.severity === 'mild') score -= 10;
    }

    // Deduct points for substance use
    if (mental.substanceUse.overallRisk === 'high') score -= 20;
    else if (mental.substanceUse.overallRisk === 'moderate') score -= 10;

    // Deduct points for suicide risk
    if (mental.suicideRisk.level === 'high' || mental.suicideRisk.level === 'critical') {
      score -= 30;
    } else if (mental.suicideRisk.level === 'moderate') {
      score -= 15;
    }

    // Add points for treatment engagement
    if (mental.treatmentEngagement.inTherapy) score += 10;
    if (
      mental.treatmentEngagement.therapyAdherence &&
      mental.treatmentEngagement.therapyAdherence > 80
    ) {
      score += 5;
    }

    return Math.max(0, Math.min(100, score));
  }

  /**
   * Calculate social health score
   */
  private calculateSocialScore(social: SDOHSummary): number {
    let score = 100;

    // Deduct points for SDOH needs
    const severeNeeds = social.needs.filter(
      (n) => n.severity === 'severe' && !n.addressed
    );
    score -= severeNeeds.length * 20;

    const moderateNeeds = social.needs.filter(
      (n) => n.severity === 'moderate' && !n.addressed
    );
    score -= moderateNeeds.length * 10;

    return Math.max(0, Math.min(100, score));
  }

  /**
   * Get mock health score for fallback
   */
  private getMockHealthScore(patientId: string): HealthScore {
    return {
      patientId,
      overallScore: 75,
      score: 75,
      status: 'good',
      trend: 'stable',
      components: {
        physical: 80,
        mental: 75,
        social: 70,
        preventive: 75,
        chronicDisease: 75,
      },
      calculatedAt: new Date(),
      lastCalculated: new Date(),
    };
  }
}
