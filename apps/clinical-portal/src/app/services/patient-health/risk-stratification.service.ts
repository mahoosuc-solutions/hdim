import { Injectable } from '@angular/core';
import { Observable, forkJoin, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { CacheableService, getCodeFromConcept, conceptCodeIncludes } from '../shared';
import { LoggerService, ContextualLogger } from '../logger.service';
import { PhysicalHealthService } from './physical-health.service';
import { MentalHealthService } from './mental-health.service';
import { SDOHService } from './sdoh.service';
import {
  RiskStratification,
  RiskLevel,
  MultiFactorRiskScore,
  CategoryRiskAssessment,
  RiskTrendData,
  HospitalizationPrediction,
  PhysicalHealthSummary,
  MentalHealthSummary,
  SDOHSummary,
} from '../../models/patient-health.model';

/**
 * Risk Stratification Service
 *
 * Handles multi-factor risk assessment including:
 * - Clinical complexity scoring
 * - SDOH risk factors
 * - Mental health risk integration
 * - Category-specific risk assessments (diabetes, cardiovascular, etc.)
 * - Hospitalization risk prediction
 *
 * Features:
 * - Built-in caching with 5-minute TTL
 * - Evidence-based weighting
 * - Trend analysis over time
 */
@Injectable({
  providedIn: 'root',
})
export class RiskStratificationService extends CacheableService {

  // Evidence-based weights for multi-factor risk
  private readonly RISK_WEIGHTS = {
    clinicalComplexity: 0.4, // 40% - Primary clinical indicators
    sdohRisk: 0.3, // 30% - Social determinants
    mentalHealthRisk: 0.3, // 30% - Mental health factors
  };

  constructor(
    private physicalHealth: PhysicalHealthService,
    private mentalHealth: MentalHealthService,
    private sdoh: SDOHService,
    private logger: LoggerService
  ) {
    super({ ttlMs: 5 * 60 * 1000 }); // 5 minute cache  }

  /**
   * Get overall risk stratification for a patient
   */
  getRiskStratification(patientId: string): Observable<RiskStratification> {
    const cacheKey = `risk:stratification:${patientId}`;
    const cached = this.getCached<RiskStratification>(cacheKey);
    if (cached) return of(cached);

    return this.calculateMultiFactorRiskScore(patientId).pipe(
      map((multiFactorScore) => {
        const stratification: RiskStratification = {
          overallRisk: multiFactorScore.overallRisk,
          scores: {
            clinicalComplexity: multiFactorScore.components.clinicalComplexity,
            socialComplexity: multiFactorScore.components.sdohRisk,
            mentalHealthRisk: multiFactorScore.components.mentalHealthRisk,
            utilizationRisk: 0,
            costRisk: 0,
          },
          predictions: {
            hospitalizationRisk30Day: this.estimateHospitalizationRisk(multiFactorScore),
            hospitalizationRisk90Day: this.estimateHospitalizationRisk(multiFactorScore) * 0.7,
            edVisitRisk30Day: this.estimateEDVisitRisk(multiFactorScore),
            readmissionRisk: this.estimateReadmissionRisk(multiFactorScore),
          },
          categories: {
            diabetes: this.getRiskLevelFromScore(multiFactorScore.components.clinicalComplexity),
            cardiovascular: this.getRiskLevelFromScore(multiFactorScore.components.clinicalComplexity),
            respiratory: 'low',
            mentalHealth: this.getRiskLevelFromScore(multiFactorScore.components.mentalHealthRisk),
            fallRisk: 'low',
          },
        };

        this.setCache(cacheKey, stratification);
        return stratification;
      }),
      catchError((error) => {
        this.logger.error('Error getting risk stratification:', error);
        return of(this.getDefaultRiskStratification());
      })
    );
  }

  /**
   * Calculate comprehensive multi-factor risk score
   */
  calculateMultiFactorRiskScore(patientId: string): Observable<MultiFactorRiskScore> {
    const cacheKey = `risk:multifactor:${patientId}`;
    const cached = this.getCached<MultiFactorRiskScore>(cacheKey);
    if (cached) return of(cached);

    return forkJoin({
      physical: this.physicalHealth.getPhysicalHealthSummary(patientId),
      mental: this.mentalHealth.getMentalHealthSummary(patientId),
      sdohData: this.sdoh.getSDOHSummary(patientId),
    }).pipe(
      map(({ physical, mental, sdohData }) => {
        // Calculate component scores (0-100)
        const clinicalScore = this.calculateClinicalComplexityFromData(physical);
        const sdohScore = this.calculateSDOHScore(sdohData);
        const mentalHealthScore = this.mentalHealth.calculateMentalHealthRiskScore(mental);

        // Calculate weighted overall score
        const overallScore = Math.round(
          clinicalScore * this.RISK_WEIGHTS.clinicalComplexity +
            sdohScore * this.RISK_WEIGHTS.sdohRisk +
            mentalHealthScore * this.RISK_WEIGHTS.mentalHealthRisk
        );

        // Ensure score is normalized 0-100
        const normalizedScore = Math.max(0, Math.min(100, overallScore));

        // Calculate detailed breakdown
        const conditionCount = physical.chronicConditions.length;
        const uncontrolledConditionCount = physical.chronicConditions.filter(
          (c) => !c.isControlled
        ).length;
        const comorbidityScore = this.calculateComorbidityScore(physical);
        const sdohNeedCount = sdohData.needs.length;
        const severeSdohNeedCount = sdohData.needs.filter(
          (n) => n.severity === 'severe'
        ).length;
        const mentalHealthAssessmentCount = mental.assessments.length;
        const highRiskMentalHealthConditions = mental.assessments.filter(
          (a) => a.severity === 'severe' || a.severity === 'moderately-severe'
        ).length;

        const overallRisk = this.getRiskLevelFromScore(normalizedScore);

        const result: MultiFactorRiskScore = {
          patientId,
          overallScore: normalizedScore,
          overallRisk,
          calculatedAt: new Date(),
          components: {
            clinicalComplexity: clinicalScore,
            sdohRisk: sdohScore,
            mentalHealthRisk: mentalHealthScore,
          },
          weights: this.RISK_WEIGHTS,
          details: {
            conditionCount,
            uncontrolledConditionCount,
            medicationCount: 0,
            comorbidityScore,
            sdohNeedCount,
            severeSdohNeedCount,
            mentalHealthAssessmentCount,
            highRiskMentalHealthConditions,
          },
        };

        this.setCache(cacheKey, result);
        return result;
      }),
      catchError((error) => {
        this.logger.error('Error calculating multi-factor risk score:', error);
        return of(this.getDefaultMultiFactorScore(patientId));
      })
    );
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
    return this.physicalHealth.getPhysicalHealthSummary(patientId).pipe(
      map((physical) => {
        const comorbidityScore = this.calculateComorbidityScore(physical);
        const medicationComplexity = this.calculateMedicationComplexity(physical);
        const functionalStatus = this.calculateFunctionalStatusScore(physical);

        const total = Math.round(
          comorbidityScore * 0.5 +
            medicationComplexity * 0.3 +
            functionalStatus * 0.2
        );

        return {
          total,
          comorbidityScore,
          medicationComplexity,
          functionalStatus,
        };
      })
    );
  }

  /**
   * Get category-specific risk assessments
   */
  getCategoryRiskAssessments(
    patientId: string
  ): Observable<CategoryRiskAssessment[]> {
    const cacheKey = `risk:categories:${patientId}`;
    const cached = this.getCached<CategoryRiskAssessment[]>(cacheKey);
    if (cached) return of(cached);

    return forkJoin({
      diabetes: this.calculateDiabetesRisk(patientId),
      cardiovascular: this.calculateCardiovascularRisk(patientId),
      mentalHealthCrisis: this.calculateMentalHealthCrisisRisk(patientId),
    }).pipe(
      map(({ diabetes, cardiovascular, mentalHealthCrisis }) => {
        const assessments = [diabetes, cardiovascular, mentalHealthCrisis];
        this.setCache(cacheKey, assessments);
        return assessments;
      }),
      catchError((error) => {
        this.logger.error('Error getting category risk assessments:', error);
        return of([]);
      })
    );
  }

  /**
   * Calculate diabetes risk assessment
   */
  calculateDiabetesRisk(patientId: string): Observable<CategoryRiskAssessment> {
    return this.physicalHealth.getPhysicalHealthSummary(patientId).pipe(
      map((physical) => {
        const factors: string[] = [];
        let score = 0;
        let riskLevel: RiskLevel = 'low';
        const recommendations: string[] = [];

        // Check for diabetes conditions
        const diabetesConditions = physical.chronicConditions.filter(
          (c) =>
            c.name?.toLowerCase().includes('diabetes') ||
            conceptCodeIncludes(c.code, 'E11') ||
            conceptCodeIncludes(c.code, 'E10')
        );

        if (diabetesConditions.length > 0) {
          const controlled = diabetesConditions.some((c) => c.isControlled);
          if (controlled) {
            score = 40;
            riskLevel = 'moderate';
            factors.push('Diabetes - controlled');
            recommendations.push('Continue current diabetes management plan');
          } else {
            score = 70;
            riskLevel = 'high';
            factors.push('Diabetes - uncontrolled');
            recommendations.push('Intensify diabetes management');
            recommendations.push('Endocrinology referral recommended');
          }
        }

        // Check HbA1c from labs
        const hba1cLab = physical.labs?.find(
          (l) => getCodeFromConcept(l.code) === '4548-4' || l.name?.toLowerCase().includes('hba1c')
        );
        if (hba1cLab && typeof hba1cLab.value === 'number') {
          if (hba1cLab.value >= 9) {
            score = Math.max(score, 80);
            riskLevel = 'high';
            factors.push(`HbA1c critically elevated (${hba1cLab.value}%)`);
            recommendations.push('Urgent: Intensify diabetes management');
          } else if (hba1cLab.value >= 7) {
            score = Math.max(score, 60);
            riskLevel = riskLevel === 'low' ? 'moderate' : riskLevel;
            factors.push(`HbA1c above target (${hba1cLab.value}%)`);
            recommendations.push('Review and optimize diabetes treatment');
          }
        }

        return {
          category: 'diabetes' as const,
          riskLevel,
          score,
          factors,
          recommendations,
          lastAssessed: new Date(),
        };
      }),
      catchError(() =>
        of({
          category: 'diabetes' as const,
          riskLevel: 'low' as const,
          score: 0,
          factors: ['Unable to retrieve diabetes risk data'],
          recommendations: ['Schedule HbA1c test for assessment'],
          lastAssessed: new Date(),
        })
      )
    );
  }

  /**
   * Calculate cardiovascular risk assessment
   */
  calculateCardiovascularRisk(
    patientId: string
  ): Observable<CategoryRiskAssessment> {
    return this.physicalHealth.getPhysicalHealthSummary(patientId).pipe(
      map((physical) => {
        const factors: string[] = [];
        let score = 0;
        let riskLevel: RiskLevel = 'low';
        const recommendations: string[] = [];

        // Check blood pressure
        const bp = physical.vitals?.bloodPressure;
        if (bp && typeof bp.value === 'string') {
          const [systolic, diastolic] = bp.value.split('/').map((v) => parseInt(v, 10));
          if (systolic >= 180 || diastolic >= 120) {
            score += 40;
            factors.push(`Blood pressure critically elevated (${bp.value})`);
            recommendations.push('Urgent: Evaluate hypertensive crisis');
          } else if (systolic >= 140 || diastolic >= 90) {
            score += 25;
            factors.push(`Blood pressure elevated (${bp.value})`);
            recommendations.push('Optimize blood pressure management');
          }
        }

        // Check for hypertension conditions
        const htConditions = physical.chronicConditions.filter(
          (c) =>
            c.name?.toLowerCase().includes('hypertension') ||
            conceptCodeIncludes(c.code, 'I10')
        );
        if (htConditions.length > 0) {
          const uncontrolled = htConditions.some((c) => !c.isControlled);
          if (uncontrolled) {
            score += 20;
            factors.push('Hypertension - uncontrolled');
          }
        }

        // Check cholesterol
        const ldlLab = physical.labs?.find(
          (l) => getCodeFromConcept(l.code) === '2089-1' || l.name?.toLowerCase().includes('ldl')
        );
        if (ldlLab && typeof ldlLab.value === 'number' && ldlLab.value > 160) {
          score += 15;
          factors.push(`LDL cholesterol elevated (${ldlLab.value})`);
          recommendations.push('Consider statin therapy or lifestyle modifications');
        }

        // Determine overall risk level
        if (score >= 60) {
          riskLevel = 'high';
        } else if (score >= 30) {
          riskLevel = 'moderate';
        }

        if (recommendations.length === 0) {
          recommendations.push('Continue cardiovascular health monitoring');
        }

        return {
          category: 'cardiovascular' as const,
          riskLevel,
          score: Math.min(100, score),
          factors,
          recommendations,
          lastAssessed: new Date(),
        };
      }),
      catchError(() =>
        of({
          category: 'cardiovascular' as const,
          riskLevel: 'low' as const,
          score: 0,
          factors: ['Unable to retrieve cardiovascular risk data'],
          recommendations: ['Schedule cardiovascular assessment'],
          lastAssessed: new Date(),
        })
      )
    );
  }

  /**
   * Calculate mental health crisis risk
   */
  calculateMentalHealthCrisisRisk(
    patientId: string
  ): Observable<CategoryRiskAssessment> {
    return this.mentalHealth.getMentalHealthSummary(patientId).pipe(
      map((mental) => {
        const factors: string[] = [];
        const score = this.mentalHealth.calculateMentalHealthRiskScore(mental);
        const recommendations: string[] = [];

        // Add factors based on assessments
        for (const assessment of mental.assessments) {
          if (assessment.severity === 'severe') {
            factors.push(`${assessment.name}: Severe (${assessment.score}/${assessment.maxScore})`);
          } else if (assessment.severity === 'moderately-severe') {
            factors.push(`${assessment.name}: Moderately severe (${assessment.score}/${assessment.maxScore})`);
          }
        }

        // Check suicide risk
        if (mental.suicideRisk.level === 'high' || mental.suicideRisk.level === 'critical') {
          factors.push('Elevated suicide risk');
          recommendations.push('Urgent: Safety assessment and crisis intervention');
        }

        // Check treatment engagement
        if (!mental.treatmentEngagement.inTherapy && score > 50) {
          recommendations.push('Recommend mental health treatment engagement');
        }

        // Determine risk level
        let riskLevel: RiskLevel;
        if (score >= 75) {
          riskLevel = 'critical';
        } else if (score >= 50) {
          riskLevel = 'high';
        } else if (score >= 25) {
          riskLevel = 'moderate';
        } else {
          riskLevel = 'low';
        }

        if (recommendations.length === 0) {
          recommendations.push('Continue mental health monitoring');
        }

        return {
          category: 'mental-health' as const,
          riskLevel,
          score,
          factors,
          recommendations,
          lastAssessed: new Date(),
        };
      }),
      catchError(() =>
        of({
          category: 'mental-health' as const,
          riskLevel: 'low' as const,
          score: 0,
          factors: ['Unable to retrieve mental health data'],
          recommendations: ['Schedule mental health screening'],
          lastAssessed: new Date(),
        })
      )
    );
  }

  /**
   * Calculate risk trend from historical data
   */
  calculateRiskTrend(
    patientId: string,
    metric: string,
    dataPoints: Array<{ date: Date; value: number; label?: string }>
  ): RiskTrendData {
    if (dataPoints.length === 0) {
      return {
        patientId,
        metric,
        trend: 'stable',
        dataPoints: [],
        percentChange: 0,
        startDate: new Date(),
        endDate: new Date(),
      };
    }

    if (dataPoints.length === 1) {
      return {
        patientId,
        metric,
        trend: 'stable',
        dataPoints,
        percentChange: 0,
        startDate: dataPoints[0].date,
        endDate: dataPoints[0].date,
      };
    }

    const firstValue = dataPoints[0].value;
    const lastValue = dataPoints[dataPoints.length - 1].value;
    const percentChange = ((lastValue - firstValue) / firstValue) * 100;

    // For risk scores, LOWER is better
    let trend: 'improving' | 'stable' | 'declining';
    if (percentChange < -5) {
      trend = 'improving';
    } else if (percentChange > 5) {
      trend = 'declining';
    } else {
      trend = 'stable';
    }

    return {
      patientId,
      metric,
      trend,
      dataPoints,
      percentChange,
      startDate: dataPoints[0].date,
      endDate: dataPoints[dataPoints.length - 1].date,
    };
  }

  /**
   * Invalidate risk stratification cache for a patient
   */
  invalidatePatientRisk(patientId: string): void {
    this.invalidatePatientCache(patientId);
  }

  // ===== Private Helper Methods =====

  /**
   * Get risk level from score
   */
  private getRiskLevelFromScore(score: number): RiskLevel {
    if (score >= 75) return 'critical';
    if (score >= 50) return 'high';
    if (score >= 25) return 'moderate';
    return 'low';
  }

  /**
   * Calculate clinical complexity from physical health data
   */
  private calculateClinicalComplexityFromData(physical: PhysicalHealthSummary): number {
    let score = 0;

    // Chronic conditions
    const severeConditions = physical.chronicConditions.filter(
      (c) => c.severity === 'severe'
    ).length;
    const uncontrolledConditions = physical.chronicConditions.filter(
      (c) => !c.isControlled
    ).length;

    score += severeConditions * 15;
    score += uncontrolledConditions * 10;

    // Medication adherence
    if (physical.medicationAdherence?.status === 'poor') {
      score += 20;
    }

    // Critical vitals
    const criticalVitals = Object.values(physical.vitals).filter(
      (v) => v && v.status === 'critical'
    ).length;
    score += criticalVitals * 15;

    // Critical labs
    const criticalLabs = (physical.labs || []).filter(
      (l) => l.status === 'critical'
    ).length;
    score += criticalLabs * 10;

    return Math.min(100, score);
  }

  /**
   * Calculate SDOH score
   */
  private calculateSDOHScore(sdoh: SDOHSummary): number {
    let score = 0;

    for (const need of sdoh.needs) {
      if (need.severity === 'severe' && !need.addressed) {
        score += 25;
      } else if (need.severity === 'moderate' && !need.addressed) {
        score += 15;
      } else if (need.severity === 'mild' && !need.addressed) {
        score += 5;
      }
    }

    return Math.min(100, score);
  }

  /**
   * Calculate comorbidity score
   */
  private calculateComorbidityScore(physical: PhysicalHealthSummary): number {
    const conditionCount = physical.chronicConditions.length;
    const severeCount = physical.chronicConditions.filter(
      (c) => c.severity === 'severe'
    ).length;
    const uncontrolledCount = physical.chronicConditions.filter(
      (c) => !c.isControlled
    ).length;

    return Math.min(
      100,
      conditionCount * 8 + severeCount * 15 + uncontrolledCount * 12
    );
  }

  /**
   * Calculate medication complexity
   */
  private calculateMedicationComplexity(physical: PhysicalHealthSummary): number {
    // Based on adherence - poor adherence = higher complexity/risk
    if (physical.medicationAdherence?.status === 'poor') {
      return 70;
    } else if (physical.medicationAdherence?.status === 'good') {
      return 40;
    } else if (physical.medicationAdherence?.status === 'excellent') {
      return 20;
    }
    return 50;
  }

  /**
   * Calculate functional status score
   */
  private calculateFunctionalStatusScore(physical: PhysicalHealthSummary): number {
    const functional = physical.functionalStatus;
    if (!functional) return 30;

    let score = 0;

    // ADL limitations
    if (functional.adlScore < 4) score += 30;
    else if (functional.adlScore < 6) score += 15;

    // Pain level
    if (functional.painLevel > 7) score += 25;
    else if (functional.painLevel > 4) score += 15;

    // Fatigue
    if (functional.fatigueLevel > 7) score += 15;
    else if (functional.fatigueLevel > 4) score += 8;

    return score;
  }

  /**
   * Extract risk factors from multi-factor score
   */
  private extractRiskFactors(score: MultiFactorRiskScore): string[] {
    const factors: string[] = [];

    if (score.components.clinicalComplexity > 60) {
      factors.push('High clinical complexity');
    }
    if (score.components.sdohRisk > 50) {
      factors.push('Significant social determinant needs');
    }
    if (score.components.mentalHealthRisk > 50) {
      factors.push('Elevated mental health risk');
    }
    if (score.details.uncontrolledConditionCount > 0) {
      factors.push(`${score.details.uncontrolledConditionCount} uncontrolled condition(s)`);
    }
    if (score.details.severeSdohNeedCount > 0) {
      factors.push(`${score.details.severeSdohNeedCount} severe SDOH need(s)`);
    }

    return factors;
  }

  /**
   * Estimate hospitalization risk
   */
  private estimateHospitalizationRisk(score: MultiFactorRiskScore): number {
    // Simple estimation based on overall score
    return Math.round(score.overallScore * 0.4);
  }

  /**
   * Estimate ED visit risk
   */
  private estimateEDVisitRisk(score: MultiFactorRiskScore): number {
    return Math.round(score.overallScore * 0.5);
  }

  /**
   * Estimate readmission risk
   */
  private estimateReadmissionRisk(score: MultiFactorRiskScore): number {
    return Math.round(score.overallScore * 0.35);
  }

  /**
   * Get default risk stratification
   */
  private getDefaultRiskStratification(): RiskStratification {
    return {
      overallRisk: 'low',
      scores: {
        clinicalComplexity: 0,
        socialComplexity: 0,
        mentalHealthRisk: 0,
        utilizationRisk: 0,
        costRisk: 0,
      },
      predictions: {
        hospitalizationRisk30Day: 0,
        hospitalizationRisk90Day: 0,
        edVisitRisk30Day: 0,
        readmissionRisk: 0,
      },
      categories: {
        diabetes: 'low',
        cardiovascular: 'low',
        respiratory: 'low',
        mentalHealth: 'low',
        fallRisk: 'low',
      },
    };
  }

  /**
   * Get default multi-factor score
   */
  private getDefaultMultiFactorScore(patientId: string): MultiFactorRiskScore {
    return {
      patientId,
      overallScore: 0,
      overallRisk: 'low',
      calculatedAt: new Date(),
      components: {
        clinicalComplexity: 0,
        sdohRisk: 0,
        mentalHealthRisk: 0,
      },
      weights: this.RISK_WEIGHTS,
      details: {
        conditionCount: 0,
        uncontrolledConditionCount: 0,
        medicationCount: 0,
        comorbidityScore: 0,
        sdohNeedCount: 0,
        severeSdohNeedCount: 0,
        mentalHealthAssessmentCount: 0,
        highRiskMentalHealthConditions: 0,
      },
    };
  }
}
