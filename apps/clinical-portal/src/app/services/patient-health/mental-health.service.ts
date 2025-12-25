import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, forkJoin, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { CacheableService } from '../shared/cacheable.service';
import { LoggerService, ContextualLogger } from '../logger.service';
import { FhirQuestionnaireService, ParsedQuestionnaireResponse } from '../fhir/fhir-questionnaire.service';
import { FhirConditionService } from '../fhir/fhir-condition.service';
import {
  API_CONFIG,
  QUALITY_MEASURE_ENDPOINTS,
  buildQualityMeasureUrl,
  HTTP_HEADERS,
} from '../../config/api.config';
import {
  MentalHealthSummary,
  MentalHealthAssessment,
  MentalHealthAssessmentType,
  MentalHealthTrend,
  MentalHealthDiagnosisFhir,
  MentalHealthCondition,
  AssessmentHistory,
  AssessmentHistoryEntry,
  HealthStatus,
  RiskLevel,
} from '../../models/patient-health.model';

/**
 * Mental Health Service
 *
 * Handles all mental health data aggregation and analysis:
 * - PHQ-9, GAD-7, PHQ-2 assessment scoring and tracking
 * - Mental health diagnosis management (ICD-10 F-codes)
 * - Treatment engagement monitoring
 * - Suicide risk assessment
 * - Substance use screening
 *
 * Features:
 * - Built-in caching with 5-minute TTL
 * - Assessment scoring with severity mapping
 * - Trend analysis over time
 * - Integration with FHIR QuestionnaireResponse and Condition
 */
@Injectable({
  providedIn: 'root',
})
export class MentalHealthService extends CacheableService {
  private log: ContextualLogger;

  // Assessment name mappings
  private readonly ASSESSMENT_NAMES: Record<string, string> = {
    'PHQ_9': 'Patient Health Questionnaire-9',
    'PHQ-9': 'Patient Health Questionnaire-9',
    'GAD_7': 'Generalized Anxiety Disorder-7',
    'GAD-7': 'Generalized Anxiety Disorder-7',
    'PHQ_2': 'Patient Health Questionnaire-2',
    'PHQ-2': 'Patient Health Questionnaire-2',
  };

  constructor(
    private http: HttpClient,
    private fhirQuestionnaire: FhirQuestionnaireService,
    private fhirCondition: FhirConditionService,
    private logger: LoggerService
  ) {
    super({ ttlMs: 5 * 60 * 1000 }); // 5 minute cache
    this.log = this.logger.withContext('MentalHealthService');
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
   * Get comprehensive mental health summary
   */
  getMentalHealthSummary(patientId: string): Observable<MentalHealthSummary> {
    const cacheKey = `mental:summary:${patientId}`;
    const cached = this.getCached<MentalHealthSummary>(cacheKey);
    if (cached) return of(cached);

    const url = buildQualityMeasureUrl(`/patient-health/mental-health/${patientId}`);

    return this.http.get<any>(url, { headers: this.getHeaders() }).pipe(
      map((response) => {
        const summary = this.mapMentalHealthResponse(response);
        this.setCache(cacheKey, summary);
        return summary;
      }),
      catchError((error) => {
        this.log.error('Error fetching mental health summary:', error);
        return this.getMentalHealthFromFhir(patientId);
      })
    );
  }

  /**
   * Get mental health summary from FHIR data
   */
  getMentalHealthFromFhir(patientId: string): Observable<MentalHealthSummary> {
    const cacheKey = `mental:fhir:${patientId}`;
    const cached = this.getCached<MentalHealthSummary>(cacheKey);
    if (cached) return of(cached);

    return forkJoin({
      phq9: this.fhirQuestionnaire.getLatestMentalHealthAssessment(patientId, 'PHQ-9'),
      gad7: this.fhirQuestionnaire.getLatestMentalHealthAssessment(patientId, 'GAD-7'),
      conditions: this.fhirCondition.getMentalHealthConditions(patientId),
    }).pipe(
      map(({ phq9, gad7, conditions }) => {
        const assessments = this.buildAssessmentsFromFhir(phq9, gad7);
        const status = this.calculateMentalHealthStatus(assessments);
        const riskLevel = this.calculateMentalHealthRiskLevel(assessments);

        const summary: MentalHealthSummary = {
          status,
          riskLevel,
          assessments,
          diagnoses: this.mapConditionsToDiagnoses(conditions),
          substanceUse: {
            hasSubstanceUse: false,
            substances: [],
            overallRisk: 'low',
          },
          suicideRisk: {
            level: 'low',
            factors: [],
            protectiveFactors: [],
            lastAssessed: new Date(),
            requiresIntervention: false,
          },
          socialSupport: {
            level: 'moderate',
            hasCaregiver: false,
            livesAlone: false,
            socialIsolation: false,
          },
          treatmentEngagement: {
            inTherapy: false,
            lastPsychVisit: undefined,
          },
        };

        this.setCache(cacheKey, summary);
        return summary;
      }),
      catchError((error) => {
        this.log.error('Error fetching mental health from FHIR:', error);
        return of(this.getDefaultMentalHealthSummary());
      })
    );
  }

  /**
   * Get assessment history for a specific type
   */
  getAssessmentHistory(
    patientId: string,
    assessmentType: MentalHealthAssessmentType
  ): Observable<AssessmentHistory[]> {
    const url = buildQualityMeasureUrl(
      `/patient-health/assessments/${patientId}/${assessmentType}/history`
    );

    return this.http.get<AssessmentHistory[]>(url, { headers: this.getHeaders() }).pipe(
      map((history) =>
        history.map((h) => ({
          ...h,
          assessedAt: new Date(h.assessedAt),
        }))
      ),
      catchError((error) => {
        this.log.error(`Error fetching ${assessmentType} history:`, error);
        return this.getAssessmentHistoryFromFhir(patientId, assessmentType);
      })
    );
  }

  /**
   * Get assessment history from FHIR
   */
  private getAssessmentHistoryFromFhir(
    patientId: string,
    type: MentalHealthAssessmentType
  ): Observable<AssessmentHistory[]> {
    return this.fhirQuestionnaire.getMentalHealthAssessments(patientId, type).pipe(
      map((assessments) =>
        assessments.map((a) => ({
          assessedAt: a.authoredDate,
          score: a.totalScore || 0,
          severity: this.getSeverityFromScore(type, a.totalScore || 0),
        }))
      )
    );
  }

  /**
   * Submit mental health assessment
   */
  submitMentalHealthAssessment(
    patientId: string,
    assessmentType: MentalHealthAssessmentType,
    responses: Record<string, any>,
    assessedBy?: string,
    clinicalNotes?: string
  ): Observable<MentalHealthAssessment> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.MENTAL_HEALTH_ASSESSMENTS);

    const request = {
      patientId,
      assessmentType: assessmentType.toLowerCase().replace('-', '_'),
      responses,
      assessedBy: assessedBy || 'system',
      clinicalNotes,
    };

    return this.http.post<any>(url, request, { headers: this.getHeaders() }).pipe(
      map((response) => ({
        type: (response.type?.replace('_', '-') || assessmentType) as MentalHealthAssessmentType,
        name: this.getAssessmentName(response.type || assessmentType),
        score: response.score,
        maxScore: response.maxScore,
        severity: response.severity,
        date: new Date(response.assessmentDate),
        interpretation: response.interpretation,
        positiveScreen: response.positiveScreen,
        thresholdScore: response.thresholdScore,
        requiresFollowup: response.requiresFollowup,
      })),
      catchError((error) => {
        this.log.error('Error submitting mental health assessment:', error);
        // Fallback to client-side scoring
        return of(this.scoreMentalHealthAssessment(assessmentType, responses));
      })
    );
  }

  /**
   * Get mental health assessment history entries
   */
  getMentalHealthAssessmentHistory(
    patientId: string,
    type?: MentalHealthAssessmentType
  ): Observable<AssessmentHistoryEntry[]> {
    const url = buildQualityMeasureUrl(
      QUALITY_MEASURE_ENDPOINTS.MENTAL_HEALTH_ASSESSMENTS_BY_PATIENT(patientId)
    );

    let params = new HttpParams();
    if (type) {
      params = params.set('type', type);
    }

    return this.http.get<any[]>(url, { headers: this.getHeaders(), params }).pipe(
      map((response) => {
        const history: AssessmentHistoryEntry[] = response.map((item) => ({
          id: item.id,
          type: item.type?.replace('_', '-') || item.assessmentType,
          date: new Date(item.assessmentDate || item.date),
          score: item.score,
          interpretation: item.interpretation,
          provider: item.assessedBy,
        }));

        history.sort((a, b) => b.date.getTime() - a.date.getTime());
        return history;
      }),
      catchError((error) => {
        this.log.error('Error fetching mental health assessment history:', error);
        return of([]);
      })
    );
  }

  /**
   * Calculate detailed mental health trend from assessment history
   */
  calculateDetailedMentalHealthTrend(history: AssessmentHistoryEntry[]): MentalHealthTrend {
    if (history.length < 2) {
      return {
        direction: 'stable',
        percentageChange: 0,
        periodMonths: 0,
        dataPoints: history.length,
      };
    }

    // Sort by date (oldest to newest)
    const sortedHistory = [...history].sort(
      (a, b) => a.date.getTime() - b.date.getTime()
    );

    const firstAssessment = sortedHistory[0];
    const lastAssessment = sortedHistory[sortedHistory.length - 1];

    // Calculate percentage change (negative means improvement)
    const percentageChange =
      ((lastAssessment.score - firstAssessment.score) / firstAssessment.score) * 100;

    // Calculate period in months
    const periodMs = lastAssessment.date.getTime() - firstAssessment.date.getTime();
    const periodMonths = Math.round(periodMs / (1000 * 60 * 60 * 24 * 30));

    // Determine trend direction
    let direction: 'improving' | 'stable' | 'declining';
    const scoreDiff = Math.abs(lastAssessment.score - firstAssessment.score);

    if (scoreDiff <= 2) {
      direction = 'stable';
    } else if (lastAssessment.score < firstAssessment.score) {
      direction = 'improving';
    } else {
      direction = 'declining';
    }

    return {
      direction,
      percentageChange,
      periodMonths,
      dataPoints: history.length,
    };
  }

  /**
   * Calculate mental health trend from simple history
   */
  calculateMentalHealthTrend(history: AssessmentHistory[]): 'improving' | 'stable' | 'declining' {
    if (!history || history.length < 2) {
      return 'stable';
    }

    const sorted = [...history].sort(
      (a, b) => new Date(a.assessedAt).getTime() - new Date(b.assessedAt).getTime()
    );

    const recent = sorted.slice(-3);
    if (recent.length < 2) {
      return 'stable';
    }

    let totalChange = 0;
    for (let i = 1; i < recent.length; i++) {
      totalChange += recent[i].score - recent[i - 1].score;
    }
    const avgChange = totalChange / (recent.length - 1);

    if (avgChange < -3) return 'improving';
    if (avgChange > 3) return 'declining';
    return 'stable';
  }

  /**
   * Get mental health diagnoses from FHIR (ICD-10 F-codes)
   */
  getMentalHealthDiagnoses(patientId: string): Observable<MentalHealthDiagnosisFhir[]> {
    return this.fhirCondition.getMentalHealthConditions(patientId).pipe(
      map((conditions) =>
        conditions
          .filter((c) => c.code.startsWith('F'))
          .map((condition) => ({
            code: condition.code,
            display: condition.name,
            severity: condition.severity,
            onsetDate: condition.onsetDate,
            clinicalStatus: condition.status,
          }))
      ),
      catchError((error) => {
        this.log.error('Error fetching mental health diagnoses:', error);
        return of([]);
      })
    );
  }

  /**
   * Calculate mental health risk score (0-100)
   */
  calculateMentalHealthRiskScore(summary: MentalHealthSummary): number {
    let score = 0;

    // Assessment scores contribute to risk
    for (const assessment of summary.assessments) {
      if (assessment.severity === 'severe') score += 30;
      else if (assessment.severity === 'moderately-severe') score += 25;
      else if (assessment.severity === 'moderate') score += 15;
      else if (assessment.severity === 'mild') score += 8;
    }

    // Suicide risk
    if (summary.suicideRisk.level === 'critical') score += 30;
    else if (summary.suicideRisk.level === 'high') score += 20;
    else if (summary.suicideRisk.level === 'moderate') score += 10;

    // Substance use
    if (summary.substanceUse.overallRisk === 'high') score += 15;
    else if (summary.substanceUse.overallRisk === 'moderate') score += 8;

    // Treatment engagement reduces risk
    if (summary.treatmentEngagement.inTherapy) score -= 10;
    if (summary.socialSupport.level === 'strong') score -= 5;

    return Math.max(0, Math.min(100, score));
  }

  /**
   * Invalidate mental health cache for a patient
   */
  invalidatePatientMentalHealth(patientId: string): void {
    this.invalidatePatientCache(patientId);
    this.fhirQuestionnaire.invalidatePatientQuestionnaires(patientId);
    this.fhirCondition.invalidatePatientConditions(patientId);
  }

  // ===== Scoring Methods =====

  /**
   * Score PHQ-9 (Depression)
   * Total score: 0-27
   */
  scorePHQ9(responses: Record<string, any>): MentalHealthAssessment {
    const score = Object.values(responses).reduce(
      (sum: number, val: any) => sum + (val || 0),
      0
    );

    let severity: MentalHealthAssessment['severity'];
    let interpretation: string;

    if (score <= 4) {
      severity = 'minimal';
      interpretation = 'Minimal or no depression';
    } else if (score <= 9) {
      severity = 'mild';
      interpretation = 'Mild depression';
    } else if (score <= 14) {
      severity = 'moderate';
      interpretation = 'Moderate depression';
    } else if (score <= 19) {
      severity = 'moderately-severe';
      interpretation = 'Moderately severe depression';
    } else {
      severity = 'severe';
      interpretation = 'Severe depression';
    }

    return {
      type: 'PHQ-9',
      name: 'Patient Health Questionnaire-9',
      score,
      maxScore: 27,
      severity,
      date: new Date(),
      interpretation,
      positiveScreen: score >= 10,
      thresholdScore: 10,
      requiresFollowup: score >= 10,
    };
  }

  /**
   * Score GAD-7 (Anxiety)
   * Total score: 0-21
   */
  scoreGAD7(responses: Record<string, any>): MentalHealthAssessment {
    const score = Object.values(responses).reduce(
      (sum: number, val: any) => sum + (val || 0),
      0
    );

    let severity: MentalHealthAssessment['severity'];
    let interpretation: string;

    if (score <= 4) {
      severity = 'minimal';
      interpretation = 'Minimal anxiety';
    } else if (score <= 9) {
      severity = 'mild';
      interpretation = 'Mild anxiety';
    } else if (score <= 14) {
      severity = 'moderate';
      interpretation = 'Moderate anxiety';
    } else {
      severity = 'severe';
      interpretation = 'Severe anxiety';
    }

    return {
      type: 'GAD-7',
      name: 'Generalized Anxiety Disorder-7',
      score,
      maxScore: 21,
      severity,
      date: new Date(),
      interpretation,
      positiveScreen: score >= 10,
      thresholdScore: 10,
      requiresFollowup: score >= 10,
    };
  }

  /**
   * Score PHQ-2 (Depression Screening)
   * Total score: 0-6
   */
  scorePHQ2(responses: Record<string, any>): MentalHealthAssessment {
    const score = Object.values(responses).reduce(
      (sum: number, val: any) => sum + (val || 0),
      0
    );

    const positiveScreen = score >= 3;
    const severity: MentalHealthAssessment['severity'] = positiveScreen ? 'moderate' : 'minimal';

    return {
      type: 'PHQ-2',
      name: 'Patient Health Questionnaire-2',
      score,
      maxScore: 6,
      severity,
      date: new Date(),
      interpretation: positiveScreen
        ? 'Positive screen - PHQ-9 recommended'
        : 'Negative screen',
      positiveScreen,
      thresholdScore: 3,
      requiresFollowup: positiveScreen,
    };
  }

  // ===== Private Helper Methods =====

  /**
   * Map backend response to MentalHealthSummary
   */
  private mapMentalHealthResponse(response: any): MentalHealthSummary {
    const assessments: MentalHealthAssessment[] = (response.assessments || []).map(
      (a: any) => ({
        type: a.type as MentalHealthAssessmentType,
        name: a.name || this.getAssessmentName(a.type),
        score: a.score,
        maxScore: a.maxScore,
        severity: a.severity,
        date: new Date(a.date),
        interpretation: a.interpretation,
        positiveScreen: a.positiveScreen,
        thresholdScore: a.thresholdScore,
        requiresFollowup: a.requiresFollowup,
        trend: a.trend || 'stable',
      })
    );

    const status = this.calculateMentalHealthStatus(assessments);
    const riskLevel = this.calculateMentalHealthRiskLevel(assessments);

    return {
      status,
      riskLevel,
      assessments,
      diagnoses: response.diagnoses || [],
      substanceUse: response.substanceUse || {
        hasSubstanceUse: false,
        substances: [],
        overallRisk: 'low',
      },
      suicideRisk: response.suicideRisk || {
        level: 'low',
        factors: [],
        protectiveFactors: [],
        lastAssessed: new Date(),
        requiresIntervention: false,
      },
      socialSupport: response.socialSupport || {
        level: 'moderate',
        hasCaregiver: false,
        livesAlone: false,
        socialIsolation: false,
      },
      treatmentEngagement: response.treatmentEngagement || {
        inTherapy: false,
        lastPsychVisit: undefined,
      },
    };
  }

  /**
   * Build assessments from FHIR questionnaire responses
   */
  private buildAssessmentsFromFhir(
    phq9: ParsedQuestionnaireResponse | null,
    gad7: ParsedQuestionnaireResponse | null
  ): MentalHealthAssessment[] {
    const assessments: MentalHealthAssessment[] = [];

    if (phq9) {
      const score = phq9.totalScore || 0;
      assessments.push({
        type: 'PHQ-9',
        name: 'Patient Health Questionnaire-9',
        score,
        maxScore: 27,
        severity: this.getSeverityFromScore('PHQ-9', score),
        date: phq9.authoredDate,
        interpretation: this.getInterpretation('PHQ-9', score),
        positiveScreen: score >= 10,
        thresholdScore: 10,
        requiresFollowup: score >= 10,
      });
    }

    if (gad7) {
      const score = gad7.totalScore || 0;
      assessments.push({
        type: 'GAD-7',
        name: 'Generalized Anxiety Disorder-7',
        score,
        maxScore: 21,
        severity: this.getSeverityFromScore('GAD-7', score),
        date: gad7.authoredDate,
        interpretation: this.getInterpretation('GAD-7', score),
        positiveScreen: score >= 10,
        thresholdScore: 10,
        requiresFollowup: score >= 10,
      });
    }

    return assessments;
  }

  /**
   * Get severity from score
   */
  private getSeverityFromScore(
    type: MentalHealthAssessmentType,
    score: number
  ): MentalHealthAssessment['severity'] {
    if (type === 'PHQ-9') {
      if (score <= 4) return 'minimal';
      if (score <= 9) return 'mild';
      if (score <= 14) return 'moderate';
      if (score <= 19) return 'moderately-severe';
      return 'severe';
    } else if (type === 'GAD-7') {
      if (score <= 4) return 'minimal';
      if (score <= 9) return 'mild';
      if (score <= 14) return 'moderate';
      return 'severe';
    }
    return 'moderate';
  }

  /**
   * Get interpretation text
   */
  private getInterpretation(type: MentalHealthAssessmentType, score: number): string {
    if (type === 'PHQ-9') {
      if (score <= 4) return 'Minimal or no depression';
      if (score <= 9) return 'Mild depression';
      if (score <= 14) return 'Moderate depression';
      if (score <= 19) return 'Moderately severe depression';
      return 'Severe depression';
    } else if (type === 'GAD-7') {
      if (score <= 4) return 'Minimal anxiety';
      if (score <= 9) return 'Mild anxiety';
      if (score <= 14) return 'Moderate anxiety';
      return 'Severe anxiety';
    }
    return 'Unknown';
  }

  /**
   * Map conditions to diagnoses
   */
  private mapConditionsToDiagnoses(conditions: any[]): MentalHealthDiagnosisFhir[] {
    return conditions
      .filter((c) => c.code && c.code.startsWith('F'))
      .map((c) => ({
        code: c.code,
        display: c.name,
        severity: c.severity,
        onsetDate: c.onsetDate,
        clinicalStatus: c.status,
      }));
  }

  /**
   * Calculate mental health status
   */
  private calculateMentalHealthStatus(assessments: MentalHealthAssessment[]): HealthStatus {
    const hasPositiveScreens = assessments.some((a) => a.positiveScreen);
    const hasSevere = assessments.some(
      (a) => a.severity === 'severe' || a.severity === 'moderately-severe'
    );

    if (hasSevere) return 'poor';
    if (hasPositiveScreens) return 'fair';
    return 'good';
  }

  /**
   * Calculate mental health risk level
   */
  private calculateMentalHealthRiskLevel(
    assessments: MentalHealthAssessment[]
  ): 'low' | 'moderate' | 'high' | 'critical' {
    const severeCount = assessments.filter((a) => a.severity === 'severe').length;
    const moderatelySevereCount = assessments.filter(
      (a) => a.severity === 'moderately-severe'
    ).length;
    const moderateCount = assessments.filter((a) => a.severity === 'moderate').length;

    if (severeCount > 0) return 'critical';
    if (moderatelySevereCount > 0) return 'high';
    if (moderateCount > 0) return 'moderate';
    return 'low';
  }

  /**
   * Score mental health assessment based on type
   */
  private scoreMentalHealthAssessment(
    type: MentalHealthAssessmentType,
    responses: Record<string, any>
  ): MentalHealthAssessment {
    switch (type) {
      case 'PHQ-9':
        return this.scorePHQ9(responses);
      case 'GAD-7':
        return this.scoreGAD7(responses);
      case 'PHQ-2':
        return this.scorePHQ2(responses);
      default:
        throw new Error(`Unsupported assessment type: ${type}`);
    }
  }

  /**
   * Get assessment name from type
   */
  private getAssessmentName(type: string): string {
    return this.ASSESSMENT_NAMES[type] || type;
  }

  /**
   * Get default mental health summary
   */
  private getDefaultMentalHealthSummary(): MentalHealthSummary {
    return {
      status: 'good',
      riskLevel: 'low',
      assessments: [],
      diagnoses: [],
      substanceUse: {
        hasSubstanceUse: false,
        substances: [],
        overallRisk: 'low',
      },
      suicideRisk: {
        level: 'low',
        factors: [],
        protectiveFactors: [],
        lastAssessed: new Date(),
        requiresIntervention: false,
      },
      socialSupport: {
        level: 'moderate',
        hasCaregiver: false,
        livesAlone: false,
        socialIsolation: false,
      },
      treatmentEngagement: {
        inTherapy: false,
        lastPsychVisit: undefined,
      },
    };
  }
}
