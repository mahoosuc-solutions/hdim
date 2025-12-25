import { Injectable } from '@angular/core';
import { Observable, forkJoin } from 'rxjs';
import { map } from 'rxjs/operators';
import { LoggerService, ContextualLogger } from '../logger.service';
import { PhysicalHealthService } from './physical-health.service';
import { MentalHealthService } from './mental-health.service';
import { SDOHService } from './sdoh.service';
import { RiskStratificationService } from './risk-stratification.service';
import { HealthScoringService } from './health-scoring.service';
import {
  PatientHealthOverview,
  HealthScore,
  HealthScoreHistory,
  HealthScoreHistoryPoint,
  HealthScoreTrend,
  PhysicalHealthSummary,
  MentalHealthSummary,
  SDOHSummary,
  RiskStratification,
  MultiFactorRiskScore,
  CategoryRiskAssessment,
  RiskTrendData,
  CareGap,
  CareRecommendation,
  QualityMeasurePerformance,
  MentalHealthAssessment,
  MentalHealthAssessmentType,
  MentalHealthTrend,
  AssessmentHistory,
  AssessmentHistoryEntry,
  LabResult,
  LabPanel,
  LabTrendAnalysis,
  VitalSign,
  ChronicCondition,
  SocialDeterminants,
  SDOHCategory,
  SDOHScreeningResult,
  HealthStatus,
  RiskLevel,
} from '../../models/patient-health.model';

/**
 * Patient Health Facade
 *
 * Unified API for patient health data that maintains backward compatibility
 * while delegating to specialized domain services.
 *
 * This facade provides:
 * - Single entry point for all patient health operations
 * - Aggregated overview combining all health domains
 * - Backward-compatible method signatures
 * - Simplified API for common use cases
 *
 * For new features, consider using the specialized services directly:
 * - PhysicalHealthService: Vitals, labs, conditions, medications
 * - MentalHealthService: PHQ-9, GAD-7, mental health tracking
 * - SDOHService: Social determinants, Z-codes, referrals
 * - RiskStratificationService: Multi-factor risk assessment
 * - HealthScoringService: Composite health scores
 */
@Injectable({
  providedIn: 'root',
})
export class PatientHealthFacade {
  private log: ContextualLogger;

  constructor(
    private physicalHealth: PhysicalHealthService,
    private mentalHealth: MentalHealthService,
    private sdoh: SDOHService,
    private riskStratification: RiskStratificationService,
    private healthScoring: HealthScoringService,
    private logger: LoggerService
  ) {
    this.log = this.logger.withContext('PatientHealthFacade');
  }

  // ===== Aggregated Overview =====

  /**
   * Get comprehensive patient health overview
   * Aggregates data from all health domains
   */
  getPatientHealthOverview(patientId: string): Observable<PatientHealthOverview> {
    return forkJoin({
      physical: this.physicalHealth.getPhysicalHealthSummary(patientId),
      mental: this.mentalHealth.getMentalHealthSummary(patientId),
      sdohData: this.sdoh.getSDOHSummary(patientId),
      risk: this.riskStratification.getRiskStratification(patientId),
      healthScore: this.healthScoring.getHealthScore(patientId),
    }).pipe(
      map(({ physical, mental, sdohData, risk, healthScore }) => ({
        patientId,
        lastUpdated: new Date(),
        overallHealthScore: healthScore,
        physicalHealth: physical,
        mentalHealth: mental,
        socialDeterminants: sdohData,
        riskStratification: risk,
        careGaps: [], // Would come from CareGapService
        recommendations: [], // Would come from RecommendationService
        qualityMeasures: this.getDefaultQualityMeasures(),
      }))
    );
  }

  // ===== Physical Health Delegations =====

  /**
   * Get physical health summary
   */
  getPhysicalHealthSummary(patientId: string): Observable<PhysicalHealthSummary> {
    return this.physicalHealth.getPhysicalHealthSummary(patientId);
  }

  /**
   * Get lab results grouped by panel
   */
  getLabResultsGroupedByPanel(patientId: string): Observable<LabPanel[]> {
    return this.physicalHealth.getLabResultsGroupedByPanel(patientId);
  }

  /**
   * Get lab history for a specific LOINC code
   */
  getLabHistory(
    patientId: string,
    loincCode: string,
    limit?: number
  ): Observable<LabResult[]> {
    return this.physicalHealth.getLabHistory(patientId, loincCode, limit);
  }

  /**
   * Analyze lab trend
   */
  analyzeLabTrend(results: LabResult[]): LabTrendAnalysis {
    return this.physicalHealth.analyzeLabTrend(results);
  }

  /**
   * Get chronic conditions
   */
  getChronicConditions(patientId: string): Observable<ChronicCondition[]> {
    return this.physicalHealth.getChronicConditions(patientId);
  }

  /**
   * Get vital sign history
   */
  getVitalSignHistory(
    patientId: string,
    loincCode?: string,
    limit?: number
  ): Observable<VitalSign[]> {
    return this.physicalHealth.getVitalSignHistory(patientId, loincCode, limit);
  }

  // ===== Mental Health Delegations =====

  /**
   * Get mental health summary
   */
  getMentalHealthSummary(patientId: string): Observable<MentalHealthSummary> {
    return this.mentalHealth.getMentalHealthSummary(patientId);
  }

  /**
   * Get assessment history
   */
  getAssessmentHistory(
    patientId: string,
    type: MentalHealthAssessmentType
  ): Observable<AssessmentHistory[]> {
    return this.mentalHealth.getAssessmentHistory(patientId, type);
  }

  /**
   * Submit mental health assessment
   */
  submitMentalHealthAssessment(
    patientId: string,
    type: MentalHealthAssessmentType,
    responses: Record<string, any>,
    assessedBy?: string,
    clinicalNotes?: string
  ): Observable<MentalHealthAssessment> {
    return this.mentalHealth.submitMentalHealthAssessment(
      patientId,
      type,
      responses,
      assessedBy,
      clinicalNotes
    );
  }

  /**
   * Get mental health assessment history entries
   */
  getMentalHealthAssessmentHistory(
    patientId: string,
    type?: MentalHealthAssessmentType
  ): Observable<AssessmentHistoryEntry[]> {
    return this.mentalHealth.getMentalHealthAssessmentHistory(patientId, type);
  }

  /**
   * Calculate mental health trend
   */
  calculateMentalHealthTrend(
    history: AssessmentHistory[]
  ): 'improving' | 'stable' | 'declining' {
    return this.mentalHealth.calculateMentalHealthTrend(history);
  }

  /**
   * Calculate detailed mental health trend
   */
  calculateDetailedMentalHealthTrend(
    history: AssessmentHistoryEntry[]
  ): MentalHealthTrend {
    return this.mentalHealth.calculateDetailedMentalHealthTrend(history);
  }

  /**
   * Score PHQ-9 assessment
   */
  scorePHQ9(responses: Record<string, any>): MentalHealthAssessment {
    return this.mentalHealth.scorePHQ9(responses);
  }

  /**
   * Score GAD-7 assessment
   */
  scoreGAD7(responses: Record<string, any>): MentalHealthAssessment {
    return this.mentalHealth.scoreGAD7(responses);
  }

  /**
   * Score PHQ-2 assessment
   */
  scorePHQ2(responses: Record<string, any>): MentalHealthAssessment {
    return this.mentalHealth.scorePHQ2(responses);
  }

  // ===== SDOH Delegations =====

  /**
   * Get SDOH summary
   */
  getSDOHSummary(patientId: string): Observable<SDOHSummary> {
    return this.sdoh.getSDOHSummary(patientId);
  }

  /**
   * Get social determinants
   */
  getSocialDeterminants(patientId: string): Observable<SocialDeterminants> {
    return this.sdoh.getSocialDeterminants(patientId);
  }

  /**
   * Get SDOH screening from FHIR
   */
  getSDOHScreeningFromFhir(patientId: string): Observable<SDOHScreeningResult> {
    return this.sdoh.getSDOHScreeningFromFhir(patientId);
  }

  /**
   * Calculate SDOH risk score
   */
  calculateSDOHRiskScore(
    patientId: string
  ): Observable<{ score: number; level: RiskLevel }> {
    return this.sdoh.calculateSDOHRiskScore(patientId);
  }

  /**
   * Identify SDOH intervention needs
   */
  identifySDOHInterventionNeeds(
    patientId: string
  ): Observable<Array<{ category: SDOHCategory; priority: string; recommendation: string }>> {
    return this.sdoh.identifySDOHInterventionNeeds(patientId);
  }

  /**
   * Map SDOH category to Z-code
   */
  mapSDOHCategoryToZCode(category: SDOHCategory): string {
    return this.sdoh.mapSDOHCategoryToZCode(category);
  }

  // ===== Risk Stratification Delegations =====

  /**
   * Get risk stratification
   */
  getRiskStratification(patientId: string): Observable<RiskStratification> {
    return this.riskStratification.getRiskStratification(patientId);
  }

  /**
   * Calculate multi-factor risk score
   */
  calculateMultiFactorRiskScore(patientId: string): Observable<MultiFactorRiskScore> {
    return this.riskStratification.calculateMultiFactorRiskScore(patientId);
  }

  /**
   * Calculate clinical complexity score
   */
  calculateClinicalComplexityScore(patientId: string): Observable<{
    total: number;
    comorbidityScore: number;
    medicationComplexity: number;
    functionalStatus: number;
  }> {
    return this.riskStratification.calculateClinicalComplexityScore(patientId);
  }

  /**
   * Get category risk assessments
   */
  getCategoryRiskAssessments(
    patientId: string
  ): Observable<CategoryRiskAssessment[]> {
    return this.riskStratification.getCategoryRiskAssessments(patientId);
  }

  /**
   * Calculate diabetes risk
   */
  calculateDiabetesRisk(patientId: string): Observable<CategoryRiskAssessment> {
    return this.riskStratification.calculateDiabetesRisk(patientId);
  }

  /**
   * Calculate cardiovascular risk
   */
  calculateCardiovascularRisk(
    patientId: string
  ): Observable<CategoryRiskAssessment> {
    return this.riskStratification.calculateCardiovascularRisk(patientId);
  }

  /**
   * Calculate mental health crisis risk
   */
  calculateMentalHealthCrisisRisk(
    patientId: string
  ): Observable<CategoryRiskAssessment> {
    return this.riskStratification.calculateMentalHealthCrisisRisk(patientId);
  }

  /**
   * Calculate risk trend
   */
  calculateRiskTrend(
    patientId: string,
    metric: string,
    dataPoints: Array<{ date: Date; value: number; label?: string }>
  ): RiskTrendData {
    return this.riskStratification.calculateRiskTrend(patientId, metric, dataPoints);
  }

  // ===== Health Scoring Delegations =====

  /**
   * Get health score
   */
  getHealthScore(patientId: string): Observable<HealthScore> {
    return this.healthScoring.getHealthScore(patientId);
  }

  /**
   * Get health score history
   */
  getHealthScoreHistory(patientId: string): Observable<HealthScoreHistory[]> {
    return this.healthScoring.getHealthScoreHistory(patientId);
  }

  /**
   * Get detailed health score history
   */
  getHealthScoreHistoryDetailed(
    patientId: string,
    months?: number
  ): Observable<HealthScoreHistoryPoint[]> {
    return this.healthScoring.getHealthScoreHistoryDetailed(patientId, months);
  }

  /**
   * Calculate health score trend
   */
  calculateHealthScoreTrend(history: HealthScoreHistoryPoint[]): HealthScoreTrend {
    return this.healthScoring.calculateHealthScoreTrend(history);
  }

  /**
   * Calculate weighted health score
   */
  calculateWeightedHealthScore(components: {
    physical: number;
    mental: number;
    social: number;
    preventive: number;
  }): number {
    return this.healthScoring.calculateWeightedHealthScore(components);
  }

  /**
   * Determine health status from score
   */
  determineHealthStatus(score: number): HealthStatus {
    return this.healthScoring.determineHealthStatus(score);
  }

  // ===== Cache Invalidation =====

  /**
   * Invalidate all caches for a patient
   */
  invalidatePatientCache(patientId: string): void {
    this.physicalHealth.invalidatePatientPhysicalHealth(patientId);
    this.mentalHealth.invalidatePatientMentalHealth(patientId);
    this.sdoh.invalidatePatientSDOH(patientId);
    this.riskStratification.invalidatePatientRisk(patientId);
    this.healthScoring.invalidateHealthScoreCache(patientId);
  }

  /**
   * Invalidate health score cache
   */
  invalidateHealthScoreCache(patientId: string): void {
    this.healthScoring.invalidateHealthScoreCache(patientId);
  }

  // ===== Private Helpers =====

  /**
   * Get default quality measures
   */
  private getDefaultQualityMeasures(): QualityMeasurePerformance {
    return {
      overallCompliance: 75,
      totalMeasures: 10,
      metMeasures: 7,
      byCategory: {
        preventive: { compliance: 80, total: 4, met: 3 },
        chronicDisease: { compliance: 75, total: 3, met: 2 },
        mentalHealth: { compliance: 67, total: 2, met: 1 },
        medication: { compliance: 100, total: 1, met: 1 },
      },
      recentResults: [],
    };
  }
}

/**
 * @deprecated Use PatientHealthFacade instead
 * This alias is provided for backward compatibility
 */
export { PatientHealthFacade as PatientHealthService };
