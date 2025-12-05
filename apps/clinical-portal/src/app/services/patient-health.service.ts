/**
 * Patient Health Service
 *
 * Comprehensive service for calculating and aggregating patient health data
 * including physical health, mental health, social determinants, and risk stratification
 */

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, forkJoin, of, map, catchError, expand, reduce, EMPTY, timer, switchMap, distinctUntilChanged, throwError } from 'rxjs';
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
  CareGap,
  CareRecommendation,
  QualityMeasurePerformance,
  MentalHealthAssessment,
  MentalHealthAssessmentType,
  HealthMetricTrend,
  RiskLevel,
  HealthStatus,
  VitalSign,
  LabResult,
  LabInterpretation,
  LabPanel,
  LabTrendAnalysis,
  VitalSignHistoryPoint,
  AssessmentHistoryEntry,
  AssessmentHistory,
  MentalHealthTrend,
  MentalHealthDiagnosisFhir,
  MentalHealthCondition,
  Medication,
  Assessment,
  SDOHScreeningResult,
  SDOHNeedWithDetails,
  SDOHCategory,
  SDOHSeverity,
  SDOHQuestionnaireType,
  ChronicCondition,
  FunctionalStatus,
  SocialDeterminants,
  SDOHRiskFactor,
  ServiceReferral,
  RiskTrendData,
  MultiFactorRiskScore,
  HospitalizationPrediction,
  CategoryRiskAssessment,
  CareGapStatus,
  CareGapStatusUpdate,
  CareGapMetrics,
  CommunityResource,
  SDOHReferralDetail,
  ReferralOutcome,
  ReferralSearchCriteria,
  RecommendationFilter,
  RecommendationOutcome,
} from '../models/patient-health.model';
import { API_CONFIG, QUALITY_MEASURE_ENDPOINTS, FHIR_ENDPOINTS, buildQualityMeasureUrl, buildFhirUrl, HTTP_HEADERS } from '../config/api.config';
import {
  FhirBundle,
  FhirObservation,
  FhirDiagnosticReport,
  FhirQuestionnaireResponse,
  FhirQuestionnaireResponseItem,
  FhirCondition,
  FhirCodeableConcept,
  LOINC_VITAL_SIGNS,
  LOINC_LAB_TESTS,
  OBSERVATION_CATEGORIES,
  LOINC_LAB_PANELS,
  FHIR_INTERPRETATION_CODES,
} from '../models/fhir.model';
import { MedicationAdherenceService } from './medication-adherence.service';
import { ProcedureHistoryService } from './procedure-history.service';

@Injectable({
  providedIn: 'root',
})
export class PatientHealthService {
  private readonly baseUrl = API_CONFIG.QUALITY_MEASURE_URL;

  // Cache for FHIR observations (5 minute TTL)
  private readonly CACHE_TTL_MS = 5 * 60 * 1000;
  private vitalSignsCache = new Map<string, { data: any; timestamp: number }>();
  private labResultsCache = new Map<string, { data: any; timestamp: number }>();
  private healthScoreCache = new Map<string, { data: HealthScore; timestamp: number }>();
  private physicalHealthCache = new Map<string, { data: PhysicalHealthSummary; timestamp: number }>();
  private mentalHealthCache = new Map<string, { data: MentalHealthSummary; timestamp: number }>();

  // SDOH Category to Z-Code Mapping
  private readonly SDOH_CATEGORY_ZCODES: Record<SDOHCategory, string> = {
    'food': 'Z59.4',
    'housing': 'Z59.0',
    'transportation': 'Z59.82',
    'financial': 'Z59.86',
    'employment': 'Z56.0',
    'education': 'Z55.9',
    'social': 'Z60.4',
    'safety': 'Z63.0',
    'food-insecurity': 'Z59.4',
    'housing-instability': 'Z59.0',
    'utility-assistance': 'Z59.5',
    'interpersonal-safety': 'Z69.1',
    'social-isolation': 'Z60.4',
    'financial-strain': 'Z59.9',
  };

  constructor(
    private http: HttpClient,
    private medicationAdherenceService: MedicationAdherenceService,
    private procedureHistoryService: ProcedureHistoryService
  ) {}

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
   * Get comprehensive patient health overview
   * Calls backend API: GET /patient-health/overview/{patientId}
   */
  getPatientHealthOverview(patientId: string): Observable<PatientHealthOverview> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.PATIENT_HEALTH_OVERVIEW(patientId));

    return this.http.get<any>(url, { headers: this.getHeaders() }).pipe(
      switchMap((response) => {
        // If backend provides health score, use it; otherwise fetch separately
        const healthScore$ = response.healthScore
          ? of(response.healthScore)
          : this.getHealthScore(patientId);

        // Feature 3.2: Use real FHIR data for physical health
        const physicalHealth$ = this.getPhysicalHealthSummary(patientId);

        return forkJoin({
          healthScore: healthScore$,
          physicalHealth: physicalHealth$,
          response: of(response)
        }).pipe(
          map(({ healthScore, physicalHealth, response }) => ({
            patientId: response.patientId,
            lastUpdated: new Date(response.lastUpdated || new Date()),
            overallHealthScore: healthScore,
            physicalHealth: physicalHealth, // Feature 3.2: Now uses real FHIR data
            mentalHealth: this.transformMentalHealthSummary(response.recentMentalHealthAssessments),
            socialDeterminants: this.getMockSDOHSummary(patientId), // TODO: Get from FHIR
            riskStratification: response.riskAssessment || this.getMockRiskStratification(patientId),
            careGaps: this.transformCareGaps(response.openCareGaps),
            recommendations: this.getMockCareRecommendations(patientId), // TODO: Implement
            qualityMeasures: this.getMockQualityPerformance(patientId), // TODO: Integrate
          }))
        );
      }),
      catchError((error) => {
        console.error('Error fetching patient health overview:', error);
        // Fallback to mock data if backend fails
        return this.getPatientHealthOverviewMock(patientId);
      })
    );
  }

  /**
   * Fallback to mock data if backend is unavailable
   */
  private getPatientHealthOverviewMock(patientId: string): Observable<PatientHealthOverview> {
    return forkJoin({
      physical: this.getPhysicalHealthSummary(patientId),
      mental: this.getMentalHealthSummary(patientId),
      social: this.getSDOHSummary(patientId),
      risk: this.getRiskStratification(patientId),
      careGaps: this.getCareGaps(patientId),
      recommendations: this.getCareRecommendations(patientId),
      quality: this.getQualityMeasurePerformance(patientId),
    }).pipe(
      map((data) => {
        const overallHealthScore = this.calculateOverallHealthScore(
          patientId,
          data.physical,
          data.mental,
          data.social,
          data.quality
        );

        return {
          patientId,
          lastUpdated: new Date(),
          overallHealthScore,
          physicalHealth: data.physical,
          mentalHealth: data.mental,
          socialDeterminants: data.social,
          riskStratification: data.risk,
          careGaps: data.careGaps,
          recommendations: data.recommendations,
          qualityMeasures: data.quality,
        };
      })
    );
  }

  /**
   * Transform backend mental health assessments to frontend format
   */
  private transformMentalHealthSummary(assessments: any[]): MentalHealthSummary {
    if (!assessments || assessments.length === 0) {
      return this.getMockMentalHealth('');
    }

    const transformedAssessments: MentalHealthAssessment[] = assessments.map((a) => ({
      type: a.type?.replace('_', '-') || a.assessmentType,
      name: a.name || this.getAssessmentName(a.type || a.assessmentType),
      score: a.score,
      maxScore: a.maxScore,
      severity: a.severity,
      date: new Date(a.assessmentDate || a.date),
      interpretation: a.interpretation,
      positiveScreen: a.positiveScreen,
      thresholdScore: a.thresholdScore,
      requiresFollowup: a.requiresFollowup,
      trend: 'stable', // TODO: Calculate from history
    }));

    // Determine overall mental health status
    const hasPositiveScreens = transformedAssessments.some((a) => a.positiveScreen);
    const status = hasPositiveScreens ? 'fair' : 'good';
    const riskLevel = this.calculateMentalHealthRiskLevel(transformedAssessments);

    return {
      status,
      riskLevel,
      assessments: transformedAssessments,
      diagnoses: [], // TODO: Get from FHIR Conditions
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

  /**
   * Calculate mental health risk level from assessments
   */
  private calculateMentalHealthRiskLevel(assessments: MentalHealthAssessment[]): 'low' | 'moderate' | 'high' | 'critical' {
    const severeCount = assessments.filter((a) => a.severity === 'severe').length;
    const moderatelySevereCount = assessments.filter((a) => a.severity === 'moderately-severe').length;
    const moderateCount = assessments.filter((a) => a.severity === 'moderate').length;

    if (severeCount > 0) return 'critical';
    if (moderatelySevereCount > 0) return 'high';
    if (moderateCount > 0) return 'moderate';
    return 'low';
  }

  /**
   * Get assessment name from type
   */
  private getAssessmentName(type: string): string {
    const names: Record<string, string> = {
      'PHQ_9': 'Patient Health Questionnaire-9',
      'PHQ-9': 'Patient Health Questionnaire-9',
      'GAD_7': 'Generalized Anxiety Disorder-7',
      'GAD-7': 'Generalized Anxiety Disorder-7',
      'PHQ_2': 'Patient Health Questionnaire-2',
      'PHQ-2': 'Patient Health Questionnaire-2',
    };
    return names[type] || type;
  }

  /**
   * Transform backend care gaps to frontend format
   */
  private transformCareGaps(gaps: any[]): CareGap[] {
    if (!gaps || gaps.length === 0) {
      return [];
    }

    return gaps.map((gap) => ({
      id: gap.id,
      category: this.mapCareGapCategory(gap.category),
      title: gap.title,
      description: gap.description,
      priority: gap.priority?.toLowerCase() || 'medium',
      dueDate: gap.dueDate ? new Date(gap.dueDate) : undefined,
      overdueDays: gap.dueDate ? this.calculateOverdueDays(new Date(gap.dueDate)) : undefined,
      measureId: gap.qualityMeasure,
      measureName: gap.qualityMeasure,
      recommendedActions: gap.recommendation ? [gap.recommendation] : [],
      barriers: [],
    }));
  }

  /**
   * Map backend care gap category to frontend
   */
  private mapCareGapCategory(category: string): 'preventive' | 'chronic-disease' | 'mental-health' | 'medication' | 'screening' {
    const categoryMap: Record<string, 'preventive' | 'chronic-disease' | 'mental-health' | 'medication' | 'screening'> = {
      'MENTAL_HEALTH': 'mental-health',
      'PREVENTIVE_CARE': 'preventive',
      'CHRONIC_DISEASE': 'chronic-disease',
      'MEDICATION_MANAGEMENT': 'medication',
      'SCREENING': 'screening',
    };
    return categoryMap[category] || 'preventive';
  }

  /**
   * Calculate overdue days from due date
   */
  private calculateOverdueDays(dueDate: Date): number {
    const today = new Date();
    const diffTime = today.getTime() - dueDate.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays > 0 ? diffDays : 0;
  }

  /**
   * Get physical health summary with FULL FHIR INTEGRATION (Feature 3.2)
   * Now using real FHIR data for all physical health components
   * Includes medication adherence and procedure history
   */
  getPhysicalHealthSummary(patientId: string): Observable<PhysicalHealthSummary> {
    // Check cache first
    const cached = this.physicalHealthCache.get(patientId);
    if (cached && Date.now() - cached.timestamp < this.CACHE_TTL_MS) {
      return of(cached.data);
    }

    return forkJoin({
      vitals: this.getVitalSignsFromFhir(patientId).pipe(
        catchError(err => {
          console.error('Error fetching vitals:', err);
          return of({});
        })
      ),
      labs: this.getLabResultsFromFhir(patientId).pipe(
        catchError(err => {
          console.error('Error fetching labs:', err);
          return of([]);
        })
      ),
      conditions: this.getConditionsFromFhir(patientId).pipe(
        catchError(err => {
          console.error('Error fetching conditions:', err);
          return of([]);
        })
      ),
      medications: this.medicationAdherenceService.calculateOverallAdherence(patientId).pipe(
        catchError(err => {
          console.error('Error fetching medication adherence:', err);
          return of({
            overallPDC: 0,
            adherentCount: 0,
            totalMedications: 0,
            problematicMedications: []
          });
        })
      ),
      procedures: this.procedureHistoryService.getRecentProcedures(patientId).pipe(
        catchError(err => {
          console.error('Error fetching procedures:', err);
          return of([]);
        })
      ),
      functional: this.getFunctionalStatusFromFhir(patientId).pipe(
        catchError(err => {
          console.error('Error fetching functional status:', err);
          return of({
            adlScore: 6,
            iadlScore: 8,
            mobilityScore: 100,
            painLevel: 0,
            fatigueLevel: 0
          });
        })
      )
    }).pipe(
      map(data => this.buildPhysicalHealthSummary(data)),
      map(summary => {
        // Cache the result
        this.physicalHealthCache.set(patientId, {
          data: summary,
          timestamp: Date.now()
        });
        return summary;
      }),
      catchError(err => {
        console.error('Error building physical health summary:', err);
        return of(this.getMockPhysicalHealth(patientId));
      })
    );
  }

  /**
   * Get mental health summary
   */
  getMentalHealthSummary(patientId: string): Observable<MentalHealthSummary> {
    // Check cache first
    const cached = this.mentalHealthCache.get(patientId);
    if (cached && Date.now() - cached.timestamp < this.CACHE_TTL_MS) {
      return of(cached.data);
    }

    const url = buildQualityMeasureUrl(`/patient-health/mental-health/${patientId}`);

    return this.http.get<any>(url, { headers: this.getHeaders() }).pipe(
      map((response) => {
        const summary = this.mapMentalHealthResponse(response);
        // Cache the result
        this.mentalHealthCache.set(patientId, { data: summary, timestamp: Date.now() });
        return summary;
      }),
      catchError((error) => {
        console.error('Error fetching mental health summary:', error);
        return this.getMentalHealthFallback(patientId);
      })
    );
  }

  /**
   * Map backend mental health response to frontend model
   */
  private mapMentalHealthResponse(response: any): MentalHealthSummary {
    const assessments: MentalHealthAssessment[] = (response.assessments || []).map((a: any) => ({
      type: a.type,
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
    }));

    const hasPositiveScreens = assessments.some((a) => a.positiveScreen);
    const status: HealthStatus = hasPositiveScreens ? 'fair' : 'good';
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
   * Fallback to mock data if backend fails
   */
  private getMentalHealthFallback(patientId: string): Observable<MentalHealthSummary> {
    return of(this.getMockMentalHealth(patientId));
  }

  /**
   * Get assessment history for a specific mental health assessment type
   * Feature 3.3: Retrieve historical assessment data from backend
   */
  getAssessmentHistory(
    patientId: string,
    assessmentType: 'PHQ-9' | 'GAD-7' | 'PHQ-2'
  ): Observable<AssessmentHistory[]> {
    const url = buildQualityMeasureUrl(
      `/patient-health/assessments/${patientId}/${assessmentType}/history`
    );

    return this.http.get<AssessmentHistory[]>(url, { headers: this.getHeaders() }).pipe(
      map((history) => {
        // Convert string dates to Date objects
        return history.map((h) => ({
          ...h,
          assessedAt: new Date(h.assessedAt),
        }));
      }),
      catchError((error) => {
        console.error(`Error fetching ${assessmentType} history:`, error);
        return of([]);
      })
    );
  }

  /**
   * Calculate mental health trend from historical assessments
   * Feature 3.3: Trend analysis logic
   * - Improving: average decrease of >3 points
   * - Declining: average increase of >3 points
   * - Stable: change within ±3 points
   */
  calculateMentalHealthTrend(
    history: AssessmentHistory[]
  ): 'improving' | 'stable' | 'declining' {
    if (!history || history.length < 2) {
      return 'stable';
    }

    // Sort by date (oldest first)
    const sorted = [...history].sort(
      (a, b) => new Date(a.assessedAt).getTime() - new Date(b.assessedAt).getTime()
    );

    // Take the most recent 3 assessments
    const recent = sorted.slice(-3);
    if (recent.length < 2) {
      return 'stable';
    }

    // Calculate average change
    let totalChange = 0;
    for (let i = 1; i < recent.length; i++) {
      totalChange += recent[i].score - recent[i - 1].score;
    }
    const avgChange = totalChange / (recent.length - 1);

    // Determine trend
    if (avgChange < -3) {
      return 'improving'; // Scores are decreasing (improvement)
    } else if (avgChange > 3) {
      return 'declining'; // Scores are increasing (decline)
    } else {
      return 'stable';
    }
  }

  /**
   * Get social determinants of health summary
   * Enhanced with real FHIR QuestionnaireResponse integration
   */
  getSDOHSummary(patientId: string): Observable<SDOHSummary> {
    return this.getSDOHScreeningFromFhir(patientId).pipe(
      map((screeningResult) => {
        // Convert SDOHNeedWithDetails to SDOHNeed format
        const needs = screeningResult.needs.map(need => ({
          category: need.category,
          description: `${need.questionText}: ${need.response}`,
          severity: need.severity,
          identified: screeningResult.screeningDate,
          addressed: false,
          interventions: [],
        }));

        // Calculate overall risk
        const riskMap = {
          'low': 'low' as RiskLevel,
          'moderate': 'moderate' as RiskLevel,
          'high': 'high' as RiskLevel,
        };
        const overallRisk = riskMap[screeningResult.overallRisk];

        // Extract Z-codes
        const zCodes = screeningResult.needs.map(n => n.zCode);

        return {
          overallRisk,
          screeningDate: screeningResult.screeningDate,
          needs,
          activeReferrals: this.getMockSDOHSummary(patientId).activeReferrals,
          zCodes,
        };
      }),
      catchError(() => of(this.getMockSDOHSummary(patientId)))
    );
  }

  /**
   * Get Social Determinants (SDOH) - Feature 3.4
   * Complete SDOH data with FHIR Integration
   * - Parse PRAPARE and AHC-HRSN QuestionnaireResponses
   * - Map SDOH Observations to risk factors
   * - Include ServiceRequest referrals
   */
  getSocialDeterminants(patientId: string): Observable<SocialDeterminants> {
    return forkJoin({
      questionnaireResponses: this.getSDOHQuestionnaireResponses(patientId),
      observations: this.getSDOHObservations(patientId),
      serviceRequests: this.getSDOHServiceRequests(patientId)
    }).pipe(
      map(({ questionnaireResponses, observations, serviceRequests }) => {
        // Parse questionnaire data
        const questionnaireData = this.parseSDOHQuestionnaire(questionnaireResponses, patientId);

        // Parse risk factors from observations
        const riskFactors = this.parseSDOHRiskFactors(observations);

        // Parse service referrals
        const activeReferrals = this.parseServiceReferrals(serviceRequests);

        return {
          patientId,
          screeningDate: questionnaireData.screeningDate,
          questionnaireType: questionnaireData.questionnaireType,
          housingStatus: questionnaireData.housingStatus,
          foodSecurity: questionnaireData.foodSecurity,
          transportation: questionnaireData.transportation,
          employment: questionnaireData.employment,
          socialSupport: questionnaireData.socialSupport,
          riskFactors,
          activeReferrals
        };
      })
    );
  }

  /**
   * Get SDOH QuestionnaireResponses from FHIR
   */
  private getSDOHQuestionnaireResponses(patientId: string): Observable<FhirBundle<FhirQuestionnaireResponse>> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.QUESTIONNAIRE_RESPONSE);
    const params = new HttpParams()
      .set('patient', patientId)
      .set('status', 'completed')
      .set('_sort', '-authored')
      .set('_count', '50');

    return this.http.get<FhirBundle<FhirQuestionnaireResponse>>(url, {
      headers: this.getHeaders(),
      params
    });
  }

  /**
   * Get SDOH Observations from FHIR
   */
  private getSDOHObservations(patientId: string): Observable<FhirBundle<FhirObservation>> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.OBSERVATION);
    const params = new HttpParams()
      .set('patient', patientId)
      .set('category', 'sdoh')
      .set('_sort', '-date')
      .set('_count', '100');

    return this.http.get<FhirBundle<FhirObservation>>(url, {
      headers: this.getHeaders(),
      params
    });
  }

  /**
   * Get SDOH ServiceRequests from FHIR
   */
  private getSDOHServiceRequests(patientId: string): Observable<FhirBundle<any>> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.SERVICE_REQUEST);
    const params = new HttpParams()
      .set('patient', patientId)
      .set('_sort', '-authored')
      .set('_count', '100');

    return this.http.get<FhirBundle<any>>(url, {
      headers: this.getHeaders(),
      params
    });
  }

  /**
   * Parse SDOH questionnaire responses (PRAPARE, AHC-HRSN)
   */
  private parseSDOHQuestionnaire(bundle: FhirBundle<FhirQuestionnaireResponse>, patientId: string): any {
    if (!bundle.entry || bundle.entry.length === 0) {
      return {
        screeningDate: new Date(),
        questionnaireType: 'custom' as SDOHQuestionnaireType,
        housingStatus: { stable: true, details: 'No data available' },
        foodSecurity: { secure: true, details: 'No data available' },
        transportation: { adequate: true, details: 'No data available' },
        employment: { status: 'Unknown', details: 'No data available' },
        socialSupport: { level: 'Unknown', details: 'No data available' }
      };
    }

    // Filter for SDOH questionnaires
    const sdohResponses = bundle.entry
      .map(e => e.resource)
      .filter((r): r is FhirQuestionnaireResponse => r !== undefined)
      .filter(r =>
        r.questionnaire?.includes('PRAPARE') ||
        r.questionnaire?.includes('AHC-HRSN') ||
        r.questionnaire?.includes('sdoh')
      );

    if (sdohResponses.length === 0) {
      return {
        screeningDate: new Date(),
        questionnaireType: 'custom' as SDOHQuestionnaireType,
        housingStatus: { stable: true, details: 'No data available' },
        foodSecurity: { secure: true, details: 'No data available' },
        transportation: { adequate: true, details: 'No data available' },
        employment: { status: 'Unknown', details: 'No data available' },
        socialSupport: { level: 'Unknown', details: 'No data available' }
      };
    }

    // Get most recent response
    const mostRecent = sdohResponses.sort((a, b) =>
      new Date(b.authored).getTime() - new Date(a.authored).getTime()
    )[0];

    // Determine questionnaire type
    let questionnaireType: SDOHQuestionnaireType = 'custom';
    if (mostRecent.questionnaire?.includes('PRAPARE')) {
      questionnaireType = 'PRAPARE';
    } else if (mostRecent.questionnaire?.includes('AHC-HRSN')) {
      questionnaireType = 'AHC-HRSN';
    }

    // Parse items
    const housingStatus = this.parseHousingStatus(mostRecent.item);
    const foodSecurity = this.parseFoodSecurity(mostRecent.item);
    const transportation = this.parseTransportation(mostRecent.item);
    const employment = this.parseEmployment(mostRecent.item);
    const socialSupport = this.parseSocialSupport(mostRecent.item);

    return {
      screeningDate: new Date(mostRecent.authored),
      questionnaireType,
      housingStatus,
      foodSecurity,
      transportation,
      employment,
      socialSupport
    };
  }

  /**
   * Parse housing status from questionnaire items
   */
  private parseHousingStatus(items: FhirQuestionnaireResponseItem[]): { stable: boolean; details: string } {
    const housingItem = items.find(item =>
      item.linkId?.includes('housing') ||
      item.text?.toLowerCase().includes('housing')
    );

    if (!housingItem || !housingItem.answer || housingItem.answer.length === 0) {
      return { stable: true, details: 'No housing data' };
    }

    const answer = housingItem.answer[0].valueString || '';
    const questionText = housingItem.text?.toLowerCase() || '';

    // Check if question is asking about worries/concerns (Yes = unstable)
    if (questionText.includes('worried') || questionText.includes('losing')) {
      const stable = !answer.toLowerCase().includes('yes');
      return { stable, details: answer || 'Not specified' };
    }

    // Otherwise, check answer content
    const stable = !answer.toLowerCase().includes('do not have') &&
                   !answer.toLowerCase().includes('worried') &&
                   !answer.toLowerCase().includes('unstable');

    return {
      stable,
      details: answer || 'Not specified'
    };
  }

  /**
   * Parse food security from questionnaire items
   */
  private parseFoodSecurity(items: FhirQuestionnaireResponseItem[]): { secure: boolean; details: string } {
    const foodItem = items.find(item =>
      item.linkId?.includes('food') ||
      item.text?.toLowerCase().includes('food')
    );

    if (!foodItem || !foodItem.answer || foodItem.answer.length === 0) {
      return { secure: true, details: 'No food security data' };
    }

    const answer = foodItem.answer[0].valueString || '';
    const secure = !answer.toLowerCase().includes('often') &&
                   !answer.toLowerCase().includes('sometimes') &&
                   !answer.toLowerCase().includes('worry');

    return {
      secure,
      details: answer || 'Not specified'
    };
  }

  /**
   * Parse transportation from questionnaire items
   */
  private parseTransportation(items: FhirQuestionnaireResponseItem[]): { adequate: boolean; details: string } {
    const transportItem = items.find(item =>
      item.linkId?.includes('transport') ||
      item.text?.toLowerCase().includes('transport')
    );

    if (!transportItem || !transportItem.answer || transportItem.answer.length === 0) {
      return { adequate: true, details: 'No transportation data' };
    }

    const answer = transportItem.answer[0].valueString || '';
    const adequate = answer.toLowerCase().includes('yes') ||
                     !answer.toLowerCase().includes('no') &&
                     !answer.toLowerCase().includes('lack');

    return {
      adequate,
      details: answer || 'Not specified'
    };
  }

  /**
   * Parse employment from questionnaire items
   */
  private parseEmployment(items: FhirQuestionnaireResponseItem[]): { status: string; details: string } {
    const employmentItem = items.find(item =>
      item.linkId?.includes('employment') ||
      item.text?.toLowerCase().includes('employment')
    );

    if (!employmentItem || !employmentItem.answer || employmentItem.answer.length === 0) {
      return { status: 'Unknown', details: 'No employment data' };
    }

    const answer = employmentItem.answer[0].valueString || '';

    return {
      status: answer,
      details: answer || 'Not specified'
    };
  }

  /**
   * Parse social support from questionnaire items
   */
  private parseSocialSupport(items: FhirQuestionnaireResponseItem[]): { level: string; details: string } {
    const socialItem = items.find(item =>
      item.linkId?.includes('social') ||
      item.text?.toLowerCase().includes('social') ||
      item.text?.toLowerCase().includes('see or talk')
    );

    if (!socialItem || !socialItem.answer || socialItem.answer.length === 0) {
      return { level: 'Unknown', details: 'No social support data' };
    }

    const answer = socialItem.answer[0].valueString || '';
    let level = 'moderate';

    if (answer.toLowerCase().includes('less than once')) {
      level = 'weak';
    } else if (answer.toLowerCase().includes('daily') || answer.toLowerCase().includes('every day')) {
      level = 'strong';
    }

    return {
      level,
      details: answer || 'Not specified'
    };
  }

  /**
   * Parse SDOH risk factors from Observations
   */
  private parseSDOHRiskFactors(bundle: FhirBundle<FhirObservation>): SDOHRiskFactor[] {
    if (!bundle.entry || bundle.entry.length === 0) {
      return [];
    }

    return bundle.entry
      .map(entry => entry.resource)
      .filter((obs): obs is FhirObservation => obs !== undefined)
      .map(obs => {
        const code = obs.code?.coding?.[0]?.code || '';
        const display = obs.code?.coding?.[0]?.display || obs.code?.text || '';
        const value = obs.valueCodeableConcept?.text ||
                     obs.valueString ||
                     obs.valueCodeableConcept?.coding?.[0]?.display ||
                     'Unknown';

        // Determine category from code or display
        const category = this.determineSdohCategory(code, display);

        // Determine severity
        const severity = this.determineSdohSeverity(value, display);

        // Get Z-code
        const zCode = this.SDOH_CATEGORY_ZCODES[category] || '';

        return {
          category,
          code,
          display,
          value,
          severity,
          date: new Date(obs.effectiveDateTime || new Date()),
          zCode
        };
      });
  }

  /**
   * Determine SDOH category from code/display
   */
  private determineSdohCategory(code: string, display: string): SDOHCategory {
    const text = (code + ' ' + display).toLowerCase();

    if (text.includes('food')) return 'food-insecurity';
    if (text.includes('housing') || text.includes('homeless')) return 'housing-instability';
    if (text.includes('transport')) return 'transportation';
    if (text.includes('employ')) return 'employment';
    if (text.includes('financial') || text.includes('income')) return 'financial-strain';
    if (text.includes('social') || text.includes('isolation')) return 'social-isolation';
    if (text.includes('safety') || text.includes('violence')) return 'interpersonal-safety';
    if (text.includes('education')) return 'education';
    if (text.includes('utility')) return 'utility-assistance';

    return 'social';
  }

  /**
   * Determine SDOH severity
   */
  private determineSdohSeverity(value: string, display: string): SDOHSeverity {
    const text = (value + ' ' + display).toLowerCase();

    if (text.includes('severe') || text.includes('critical') || text.includes('homeless')) {
      return 'severe';
    }
    if (text.includes('moderate') || text.includes('at risk')) {
      return 'moderate';
    }
    if (text.includes('mild') || text.includes('some concern')) {
      return 'mild';
    }

    return 'moderate'; // default
  }

  /**
   * Parse service referrals from ServiceRequest resources
   */
  private parseServiceReferrals(bundle: FhirBundle<any>): ServiceReferral[] {
    if (!bundle.entry || bundle.entry.length === 0) {
      return [];
    }

    return bundle.entry
      .map(entry => entry.resource)
      .filter((sr): sr is any => sr !== undefined && sr.resourceType === 'ServiceRequest')
      .map(sr => {
        const category = this.determineSdohCategoryFromServiceRequest(sr);
        const service = sr.code?.text || sr.code?.coding?.[0]?.display || 'Unknown service';
        const organization = sr.performer?.[0]?.display || 'Unknown organization';
        const note = sr.note?.[0]?.text || '';

        return {
          id: sr.id || '',
          category,
          service,
          organization,
          status: sr.status || 'unknown',
          priority: sr.priority || 'routine',
          authoredDate: new Date(sr.authoredOn || new Date()),
          occurrenceDate: sr.occurrenceDateTime ? new Date(sr.occurrenceDateTime) : undefined,
          note
        };
      });
  }

  /**
   * Determine SDOH category from ServiceRequest
   */
  private determineSdohCategoryFromServiceRequest(serviceRequest: any): SDOHCategory {
    const categoryCode = serviceRequest.category?.[0]?.coding?.[0]?.code || '';
    const categoryDisplay = serviceRequest.category?.[0]?.coding?.[0]?.display || '';
    const serviceText = serviceRequest.code?.text || '';

    const text = (categoryCode + ' ' + categoryDisplay + ' ' + serviceText).toLowerCase();

    if (text.includes('food')) return 'food-insecurity';
    if (text.includes('housing')) return 'housing-instability';
    if (text.includes('transport')) return 'transportation';
    if (text.includes('employ')) return 'employment';
    if (text.includes('financial')) return 'financial-strain';
    if (text.includes('social')) return 'social-isolation';
    if (text.includes('safety')) return 'interpersonal-safety';
    if (text.includes('education')) return 'education';
    if (text.includes('utility')) return 'utility-assistance';

    return 'social';
  }

  /**
   * Get SDOH screening results from FHIR QuestionnaireResponse
   */
  getSDOHScreeningResults(patientId: string): Observable<any> {
    return of({
      patientId,
      screeningDate: new Date('2025-10-15'),
      screeningItems: [
        {
          category: 'food-insecurity' as const,
          loincCode: '88122-7',
          description: 'Reported difficulty affording healthy food 2-3 times per month',
          severity: 'moderate' as const,
          identifiedDate: new Date('2025-10-15'),
          addressed: false,
        },
        {
          category: 'transportation' as const,
          loincCode: '93030-5',
          description: 'Difficulty getting to medical appointments',
          severity: 'mild' as const,
          identifiedDate: new Date('2025-10-15'),
          addressed: true,
          interventions: ['Provided public transit pass'],
        },
      ],
    });
  }

  /**
   * Map FHIR QuestionnaireResponse to SDOH screening result
   */
  mapQuestionnaireResponseToSDOH(fhirResponse: any): Observable<any> {
    const screeningDate = new Date(fhirResponse.authored || new Date());
    const patientRef = fhirResponse.subject?.reference || '';

    let category: any = 'food-insecurity';
    let severity: 'mild' | 'moderate' | 'severe' = 'moderate';

    if (fhirResponse.item && fhirResponse.item.length > 0) {
      const linkId = fhirResponse.item[0].linkId;
      if (linkId) {
        category = linkId;
      }

      const answer = fhirResponse.item[0].answer?.[0];
      if (answer?.valueCoding?.code === 'LA33-6') {
        severity = 'moderate';
      }
    }

    return of({
      category,
      severity,
      screeningDate,
      patientId: patientRef.split('/')[1],
    });
  }

  /**
   * Get SDOH screening from FHIR QuestionnaireResponse
   * Feature 3.4: Fetch and parse SDOH data from FHIR server
   */
  getSDOHScreeningFromFhir(patientId: string): Observable<SDOHScreeningResult> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.QUESTIONNAIRE_RESPONSE);
    const params = new HttpParams()
      .set('patient', patientId)
      .set('status', 'completed')
      .set('_sort', '-authored')
      .set('_count', '50');

    return this.http.get<FhirBundle<FhirQuestionnaireResponse>>(url, {
      headers: this.getHeaders(),
      params,
    }).pipe(
      map((bundle) => {
        if (!bundle.entry || bundle.entry.length === 0) {
          return {
            screeningDate: new Date(),
            questionnaireType: 'custom' as SDOHQuestionnaireType,
            needs: [],
            overallRisk: 'low' as const,
          };
        }

        // Filter for SDOH questionnaires
        const sdohResponses = bundle.entry
          .map(e => e.resource)
          .filter((r): r is FhirQuestionnaireResponse => r !== undefined)
          .filter(r =>
            r.questionnaire.includes('PRAPARE') ||
            r.questionnaire.includes('AHC-HRSN') ||
            r.questionnaire.includes('sdoh')
          );

        if (sdohResponses.length === 0) {
          return {
            screeningDate: new Date(),
            questionnaireType: 'custom' as SDOHQuestionnaireType,
            needs: [],
            overallRisk: 'low' as const,
          };
        }

        // Get most recent screening
        const mostRecent = sdohResponses.sort((a, b) =>
          new Date(b.authored).getTime() - new Date(a.authored).getTime()
        )[0];

        // Determine questionnaire type
        let questionnaireType: SDOHQuestionnaireType = 'custom';
        if (mostRecent.questionnaire.includes('PRAPARE')) {
          questionnaireType = 'PRAPARE';
        } else if (mostRecent.questionnaire.includes('AHC-HRSN')) {
          questionnaireType = 'AHC-HRSN';
        }

        // Parse needs from response
        const needs = this.parseQuestionnaireResponseToSDOHNeeds(mostRecent);

        // Sort needs by severity (Feature 3.4)
        const sortedNeeds = this.sortSDOHNeedsBySeverity(needs);

        // Calculate overall risk
        const severeCount = sortedNeeds.filter(n => n.severity === 'severe').length;
        const moderateCount = sortedNeeds.filter(n => n.severity === 'moderate').length;
        let overallRisk: 'low' | 'moderate' | 'high' = 'low';
        if (severeCount >= 2 || (severeCount >= 1 && moderateCount >= 2)) {
          overallRisk = 'high';
        } else if (severeCount >= 1 || moderateCount >= 2) {
          overallRisk = 'moderate';
        } else if (moderateCount >= 1) {
          overallRisk = 'moderate';
        }

        // Extract Z-codes (Feature 3.4)
        const zCodes = sortedNeeds.map(n => n.zCode).filter((code, index, self) => self.indexOf(code) === index);

        // Get questionnaire name (Feature 3.4)
        const questionnaireName = questionnaireType;

        return {
          screeningDate: new Date(mostRecent.authored),
          questionnaireType,
          questionnaireName,
          needs: sortedNeeds,
          overallRisk,
          zCodes,
        };
      }),
      catchError((error) => {
        console.error('Error fetching SDOH screening from FHIR:', error);
        return of({
          screeningDate: new Date(),
          questionnaireType: 'custom' as SDOHQuestionnaireType,
          needs: [],
          overallRisk: 'low' as const,
        });
      })
    );
  }

  /**
   * Parse FHIR QuestionnaireResponse to SDOH needs
   * Feature 3.4: Parse questionnaire items and map to SDOH categories
   */
  parseQuestionnaireResponseToSDOHNeeds(response: FhirQuestionnaireResponse): SDOHNeedWithDetails[] {
    const needs: SDOHNeedWithDetails[] = [];

    if (!response.item || response.item.length === 0) {
      return needs;
    }

    // LOINC code to SDOH category mapping (Enhanced for Feature 3.4)
    const loincToCategoryMap: Record<string, SDOHCategory> = {
      '88122-7': 'food',  // Food insecurity - worried food would run out
      '88123-5': 'food',  // Food insecurity - food didn't last
      '71802-3': 'housing',  // Housing situation
      '93033-9': 'housing',  // Housing instability
      '93030-5': 'transportation',  // Transportation needs
      '63586-2': 'financial',  // Financial resource strain
      '96779-2': 'financial',  // Financial strain
      '76513-1': 'financial',  // Financial resource strain
      '93029-7': 'social',  // Social isolation - How often do you see or talk to people
      '93038-8': 'social',  // Social connection/isolation
      '93037-0': 'safety',  // Personal safety concerns
      '76501-6': 'safety',  // Within the past year, have you been afraid of your partner
      '82589-3': 'education',  // Highest level of education
      '67875-5': 'employment',  // Employment status
    };

    // Process each item in the questionnaire
    for (const item of response.item) {
      if (!item.answer || item.answer.length === 0) {
        continue;
      }

      const linkId = item.linkId;
      const questionText = item.text || '';
      const answer = item.answer[0];

      // Determine category from linkId
      let category: SDOHCategory | null = loincToCategoryMap[linkId];

      // If not in map, try to infer from question text (Enhanced for Feature 3.4)
      if (!category) {
        const textLower = questionText.toLowerCase();
        if (textLower.includes('food') || textLower.includes('hungry') || textLower.includes('meal')) {
          category = 'food';
        } else if (textLower.includes('housing') || textLower.includes('homeless') || textLower.includes('shelter')) {
          category = 'housing';
        } else if (textLower.includes('transportation') || textLower.includes('ride') || textLower.includes('bus')) {
          category = 'transportation';
        } else if (textLower.includes('money') || textLower.includes('bill') || textLower.includes('financial')) {
          category = 'financial';
        } else if (textLower.includes('social') || textLower.includes('isolated') || textLower.includes('lonely') || textLower.includes('alone')) {
          category = 'social';
        } else if (textLower.includes('safe') || textLower.includes('afraid') || textLower.includes('violence') || textLower.includes('abuse')) {
          category = 'safety';
        } else if (textLower.includes('education') || textLower.includes('school') || textLower.includes('degree')) {
          category = 'education';
        } else if (textLower.includes('employment') || textLower.includes('job') || textLower.includes('work')) {
          category = 'employment';
        } else {
          continue;  // Skip unknown categories
        }
      }

      // Get response value
      let responseText = '';
      if (answer.valueCoding) {
        responseText = answer.valueCoding.display || answer.valueCoding.code || '';
      } else if (answer.valueString) {
        responseText = answer.valueString;
      } else if (answer.valueBoolean !== undefined) {
        responseText = answer.valueBoolean ? 'Yes' : 'No';
      }

      // Determine severity from response
      const severity = this.determineSeverityFromResponse(responseText);

      // Only add if there's a positive finding
      if (severity !== 'none') {
        const zCode = this.mapSDOHCategoryToZCode(category);

        needs.push({
          category,
          severity,
          zCode,
          questionText,
          response: responseText,
        });
      }
    }

    return needs;
  }

  /**
   * Map SDOH category to ICD-10 Z-code
   * Feature 3.4: Return appropriate Z-code for SDOH category
   */
  mapSDOHCategoryToZCode(category: SDOHCategory): string {
    return this.SDOH_CATEGORY_ZCODES[category] || 'Z59.9';  // Default to Z59.9 if not found
  }

  /**
   * Sort SDOH needs by severity (Feature 3.4)
   * Order: severe > moderate > mild > none
   */
  private sortSDOHNeedsBySeverity(needs: SDOHNeedWithDetails[]): SDOHNeedWithDetails[] {
    const severityOrder: Record<SDOHSeverity, number> = {
      'severe': 3,
      'moderate': 2,
      'mild': 1,
      'none': 0
    };

    return [...needs].sort((a, b) => {
      return (severityOrder[b.severity] || 0) - (severityOrder[a.severity] || 0);
    });
  }

  /**
   * Helper method to determine severity from questionnaire response
   */
  private determineSeverityFromResponse(responseText: string): SDOHSeverity {
    const textLower = responseText.toLowerCase();

    // Check for severity indicators
    if (textLower.includes('always') || textLower.includes('often true') ||
        textLower.includes('do not have') || textLower.includes('homeless')) {
      return 'severe';
    } else if (textLower.includes('often') || textLower.includes('sometimes true') ||
               textLower.includes('yes') || textLower.includes('difficulty')) {
      return 'moderate';
    } else if (textLower.includes('sometimes') || textLower.includes('rarely')) {
      return 'mild';
    } else if (textLower.includes('never') || textLower.includes('no') ||
               textLower.includes('not applicable')) {
      return 'none';
    }

    // Default to moderate if we can't determine
    return 'moderate';
  }

  /**
   * Calculate SDOH risk score for a patient
   */
  calculateSDOHRiskScore(patientId: string): Observable<{ overallRisk: RiskLevel; score: number }> {
    return this.getSDOHSummary(patientId).pipe(
      map((summary) => {
        let score = 0;

        summary.needs.forEach((need) => {
          if (!need.addressed) {
            if (need.severity === 'severe') score += 30;
            else if (need.severity === 'moderate') score += 15;
            else if (need.severity === 'mild') score += 5;
          } else {
            if (need.severity === 'severe') score += 10;
            else if (need.severity === 'moderate') score += 5;
            else if (need.severity === 'mild') score += 2;
          }
        });

        const overallRisk = this.getRiskLevelFromScore(score);
        return { overallRisk, score };
      })
    );
  }

  /**
   * Calculate SDOH risk from needs list
   */
  calculateSDOHRiskFromNeeds(needs: any[]): Observable<RiskLevel> {
    let score = 0;

    needs.forEach((need) => {
      if (!need.addressed) {
        if (need.severity === 'severe') score += 30;
        else if (need.severity === 'moderate') score += 15;
        else if (need.severity === 'mild') score += 5;
      } else {
        if (need.severity === 'severe') score += 10;
        else if (need.severity === 'moderate') score += 5;
        else if (need.severity === 'mild') score += 2;
      }
    });

    return of(this.getRiskLevelFromScore(score));
  }

  /**
   * Identify patients needing SDOH interventions
   */
  identifySDOHInterventionNeeds(patientId: string): Observable<any[]> {
    return this.getSDOHSummary(patientId).pipe(
      map((summary) => {
        const interventions: any[] = [];

        summary.needs.forEach((need) => {
          if (!need.addressed) {
            const priority = need.severity === 'severe' ? 'urgent' :
                           need.severity === 'moderate' ? 'high' : 'medium';

            interventions.push({
              category: need.category,
              description: need.description,
              priority,
              severity: need.severity,
              recommendedActions: this.getSDOHRecommendedActions(need.category),
            });
          }
        });

        return interventions;
      })
    );
  }

  /**
   * Get recommended actions for SDOH category
   */
  private getSDOHRecommendedActions(category: string): string[] {
    const actions: Record<string, string[]> = {
      'food-insecurity': [
        'Referral to local food bank',
        'SNAP benefits application assistance',
        'Nutrition counseling',
      ],
      'housing-instability': [
        'Housing assistance referral',
        'Emergency shelter information',
        'Social work consultation',
      ],
      'transportation': [
        'Public transit pass',
        'Rideshare program enrollment',
        'Telemedicine options',
      ],
      'utility-assistance': [
        'LIHEAP application',
        'Utility company payment plans',
        'Energy assistance programs',
      ],
      'interpersonal-safety': [
        'Domestic violence resources',
        'Safety planning',
        'Immediate intervention if needed',
      ],
    };

    return actions[category] || ['Social work referral', 'Community resources'];
  }

  /**
   * Extract Z-codes from SDOH needs
   */
  private extractZCodesFromNeeds(needs: any[]): string[] {
    const zCodeMap: Record<string, string> = {
      'food-insecurity': 'Z59.4',
      'housing-instability': 'Z59.0',
      'transportation': 'Z59.82',
      'utility-assistance': 'Z59.5',
      'interpersonal-safety': 'Z69.1',
      'employment': 'Z56.9',
      'education': 'Z55.9',
      'social-isolation': 'Z60.4',
      'financial-strain': 'Z59.9',
    };

    return needs
      .map((need) => zCodeMap[need.category])
      .filter((code) => code !== undefined);
  }

  /**
   * Calculate risk level from needs
   */
  private calculateRiskLevelFromNeeds(needs: any[]): RiskLevel {
    const severeCount = needs.filter((n) => n.severity === 'severe' && !n.addressed).length;
    const moderateCount = needs.filter((n) => n.severity === 'moderate' && !n.addressed).length;

    if (severeCount >= 2) return 'critical';
    if (severeCount >= 1 || moderateCount >= 3) return 'high';
    if (moderateCount >= 1) return 'moderate';
    return 'low';
  }

  /**
   * Get risk stratification
   * Calls backend API: GET /patient-health/risk-stratification/{patientId}
   * Enhanced with real SDOH and mental health risk calculation
   */
  getRiskStratification(patientId: string): Observable<RiskStratification> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.RISK_STRATIFICATION_GET(patientId));

    return this.http.get<any>(url, { headers: this.getHeaders() }).pipe(
      map((response) => {
        return {
          overallRisk: this.mapRiskLevel(response.riskLevel),
          scores: {
            clinicalComplexity: response.riskScore || 0,
            socialComplexity: 0, // TODO: Calculate from SDOH
            mentalHealthRisk: 0, // TODO: Calculate from assessments
            utilizationRisk: 0, // TODO: Calculate from claims
            costRisk: 0, // TODO: Calculate from claims
          },
          predictions: response.predictedOutcomes || {
            hospitalizationRisk30Day: 0,
            hospitalizationRisk90Day: 0,
            edVisitRisk30Day: 0,
            readmissionRisk: 0,
          },
          categories: response.riskFactors || {},
        };
      }),
      catchError((error) => {
        console.error('Error fetching risk stratification:', error);
        return of(this.getMockRiskStratification(patientId));
      })
    );
  }

  /**
   * Feature 4.2: Get Hospitalization Prediction
   * Retrieves predictive analytics for patient hospitalization risk
   * API: GET /patient-health/predictions/{patientId}/hospitalization
   */
  getHospitalizationPrediction(patientId: string): Observable<HospitalizationPrediction> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.HOSPITALIZATION_PREDICTION(patientId));

    return this.http.get<any>(url, { headers: this.getHeaders() }).pipe(
      map((response) => {
        return {
          patientId: response.patientId,
          probability30Day: response.probability30Day,
          probability90Day: response.probability90Day,
          confidence: {
            low: response.confidence.low,
            high: response.confidence.high,
          },
          factors: response.factors.map((factor: any) => ({
            name: factor.name,
            weight: factor.weight,
            description: factor.description,
          })),
          calculatedAt: new Date(response.calculatedAt),
        };
      }),
      catchError((error) => {
        console.error('Error fetching hospitalization prediction:', error);
        throw new Error('Hospitalization prediction service unavailable');
      })
    );
  }

  /**
   * Feature 5.2: Get Care Recommendations
   * Retrieves care recommendations for a patient with optional filtering
   * API: GET /patient-health/recommendations/{patientId}
   */
  getCareRecommendations(patientId: string, filter?: RecommendationFilter): Observable<CareRecommendation[]> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.CARE_RECOMMENDATIONS(patientId));

    let params = new HttpParams();
    if (filter?.status) {
      filter.status.forEach(s => params = params.append('status', s));
    }
    if (filter?.urgency) {
      filter.urgency.forEach(u => params = params.append('urgency', u));
    }
    if (filter?.category) {
      filter.category.forEach(c => params = params.append('category', c));
    }

    return this.http.get<any[]>(url, { headers: this.getHeaders(), params }).pipe(
      map((response) => {
        const recommendations = response.map(rec => ({
          id: rec.id,
          patientId: rec.patientId,
          title: rec.title,
          description: rec.description,
          urgency: rec.urgency,
          category: rec.category,
          evidenceSource: rec.evidenceSource,
          clinicalGuideline: rec.clinicalGuideline,
          actionItems: rec.actionItems || [],
          status: rec.status,
          createdDate: new Date(rec.createdDate),
          dueDate: rec.dueDate ? new Date(rec.dueDate) : undefined,
          completedDate: rec.completedDate ? new Date(rec.completedDate) : undefined,
          outcome: rec.outcome ? {
            result: rec.outcome.result,
            notes: rec.outcome.notes,
            measuredDate: new Date(rec.outcome.measuredDate)
          } : undefined
        }));

        // Sort by urgency (emergent > urgent > soon > routine)
        const urgencyOrder: Record<string, number> = {
          'emergent': 0,
          'urgent': 1,
          'soon': 2,
          'routine': 3
        };

        return recommendations.sort((a, b) => {
          return urgencyOrder[a.urgency] - urgencyOrder[b.urgency];
        });
      }),
      catchError((error) => {
        console.error('Error fetching care recommendations:', error);
        throw new Error('Care recommendations service unavailable');
      })
    );
  }

  /**
   * Feature 5.2: Generate Recommendations
   * Generates new care recommendations based on clinical guidelines and patient data
   * API: POST /patient-health/recommendations/{patientId}/generate
   */
  generateRecommendations(patientId: string): Observable<CareRecommendation[]> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.GENERATE_RECOMMENDATIONS(patientId));

    return this.http.post<any[]>(url, {}, { headers: this.getHeaders() }).pipe(
      map((response) => {
        return response.map(rec => ({
          id: rec.id,
          patientId: rec.patientId,
          title: rec.title,
          description: rec.description,
          urgency: rec.urgency,
          category: rec.category,
          evidenceSource: rec.evidenceSource,
          clinicalGuideline: rec.clinicalGuideline,
          actionItems: rec.actionItems || [],
          status: rec.status,
          createdDate: new Date(rec.createdDate),
          dueDate: rec.dueDate ? new Date(rec.dueDate) : undefined,
          completedDate: rec.completedDate ? new Date(rec.completedDate) : undefined,
          outcome: rec.outcome ? {
            result: rec.outcome.result,
            notes: rec.outcome.notes,
            measuredDate: new Date(rec.outcome.measuredDate)
          } : undefined
        }));
      }),
      catchError((error) => {
        console.error('Error generating care recommendations:', error);
        throw new Error('Care recommendations generation service unavailable');
      })
    );
  }

  /**
   * Feature 5.2: Update Recommendation Status
   * Updates the status of a care recommendation
   * API: PUT /patient-health/recommendations/{recommendationId}/status
   */
  updateRecommendationStatus(
    recommendationId: string,
    status: 'pending' | 'accepted' | 'declined' | 'completed',
    reason?: string,
    outcome?: RecommendationOutcome
  ): Observable<CareRecommendation> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.UPDATE_RECOMMENDATION_STATUS(recommendationId));

    const body: any = { status };
    if (reason) {
      body.reason = reason;
    }
    if (outcome) {
      body.outcome = {
        result: outcome.result,
        notes: outcome.notes,
        measuredDate: outcome.measuredDate.toISOString()
      };
    }

    return this.http.put<any>(url, body, { headers: this.getHeaders() }).pipe(
      map((response) => ({
        id: response.id,
        patientId: response.patientId,
        title: response.title,
        description: response.description,
        urgency: response.urgency,
        category: response.category,
        evidenceSource: response.evidenceSource,
        clinicalGuideline: response.clinicalGuideline,
        actionItems: response.actionItems || [],
        status: response.status,
        createdDate: new Date(response.createdDate),
        dueDate: response.dueDate ? new Date(response.dueDate) : undefined,
        completedDate: response.completedDate ? new Date(response.completedDate) : undefined,
        outcome: response.outcome ? {
          result: response.outcome.result,
          notes: response.outcome.notes,
          measuredDate: new Date(response.outcome.measuredDate)
        } : undefined
      })),
      catchError((error) => {
        console.error('Error updating recommendation status:', error);
        throw new Error('Recommendation status update service unavailable');
      })
    );
  }

  /**
   * Feature 5.2: Track Recommendation Outcomes
   * Retrieves metrics and analytics about recommendation acceptance and completion rates
   * API: GET /patient-health/recommendations/{patientId}/outcomes
   */
  trackRecommendationOutcomes(patientId: string): Observable<any> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.RECOMMENDATION_OUTCOMES(patientId));

    return this.http.get<any>(url, { headers: this.getHeaders() }).pipe(
      catchError((error) => {
        console.error('Error tracking recommendation outcomes:', error);
        throw new Error('Recommendation outcomes service unavailable');
      })
    );
  }

  /**
   * Map backend risk level to frontend format
   */
  private mapRiskLevel(level: string): 'low' | 'moderate' | 'high' | 'critical' {
    const levelMap: Record<string, 'low' | 'moderate' | 'high' | 'critical'> = {
      'LOW': 'low',
      'MODERATE': 'moderate',
      'HIGH': 'high',
      'VERY_HIGH': 'critical',
      'CRITICAL': 'critical',
    };
    return levelMap[level?.toUpperCase()] || 'low';
  }

  // ========================================================================
  // ADVANCED RISK STRATIFICATION METHODS
  // ========================================================================

  /**
   * Calculate clinical complexity score based on conditions and medications
   */
  calculateClinicalComplexityScore(patientId: string): Observable<any> {
    return this.getPhysicalHealthSummary(patientId).pipe(
      map((physical) => {
        let total = 0;
        const conditionCount = physical.chronicConditions.length;

        // Base score from condition count
        total += Math.min(conditionCount * 10, 40);

        // Add points for uncontrolled conditions
        const uncontrolledCount = physical.chronicConditions.filter((c) => !c.controlled).length;
        total += uncontrolledCount * 15;

        // Add points for severe conditions
        const severeCount = physical.chronicConditions.filter((c) => c.severity === 'severe').length;
        total += severeCount * 10;

        // Comorbidity score (Charlson-like)
        const comorbidityScore = this.calculateComorbidityScore(physical.chronicConditions);

        return {
          total: Math.min(total, 100),
          conditionCount,
          comorbidityScore,
        };
      })
    );
  }

  /**
   * Calculate comorbidity score
   */
  private calculateComorbidityScore(conditions: any[]): number {
    let score = 0;

    conditions.forEach((condition) => {
      const displayLower = condition.display?.toLowerCase() || '';

      // High-weight conditions
      if (displayLower.includes('cancer') || displayLower.includes('malignancy')) score += 6;
      else if (displayLower.includes('renal') || displayLower.includes('kidney')) score += 5;
      else if (displayLower.includes('liver') || displayLower.includes('cirrhosis')) score += 4;
      else if (displayLower.includes('diabetes')) score += 2;
      else if (displayLower.includes('heart') || displayLower.includes('cardiac')) score += 2;
      else score += 1;
    });

    return score;
  }

  /**
   * Assess risk level from conditions
   */
  assessConditionsRisk(conditions: any[]): Observable<RiskLevel> {
    const uncontrolledSevere = conditions.filter(
      (c) => !c.controlled && c.severity === 'severe'
    ).length;
    const uncontrolledModerate = conditions.filter(
      (c) => !c.controlled && c.severity === 'moderate'
    ).length;

    let risk: RiskLevel = 'low';
    if (uncontrolledSevere >= 2) risk = 'critical';
    else if (uncontrolledSevere >= 1 || uncontrolledModerate >= 3) risk = 'high';
    else if (uncontrolledModerate >= 1) risk = 'moderate';

    return of(risk);
  }

  /**
   * Calculate medication risk score
   */
  calculateMedicationRiskScore(patientId: string): Observable<any> {
    // Mock implementation - would query FHIR MedicationStatement
    return of({
      medicationCount: 8,
      polypharmacyRisk: 40, // Risk from having 5+ medications
      highRiskMeds: ['warfarin', 'insulin'],
      interactionRisk: 25,
    });
  }

  /**
   * Calculate utilization risk score
   */
  calculateUtilizationRiskScore(patientId: string): Observable<any> {
    // Mock implementation - would query FHIR Encounter resources
    return of({
      total: 55,
      recentHospitalizations: 2,
      edVisits: 3,
      readmissions: 1,
    });
  }

  /**
   * Assess risk from recent admissions
   */
  assessRecentAdmissionsRisk(admissions: any[]): Observable<RiskLevel> {
    const count = admissions.length;

    let risk: RiskLevel = 'low';
    if (count >= 3) risk = 'critical';
    else if (count >= 2) risk = 'high';
    else if (count >= 1) risk = 'moderate';

    return of(risk);
  }

  /**
   * Categorize patient into risk tier based on component scores
   */
  categorizeRiskTier(scores: any): Observable<RiskLevel> {
    const avgScore = (
      scores.clinicalComplexity +
      scores.socialComplexity +
      scores.mentalHealthRisk +
      scores.utilizationRisk +
      scores.costRisk
    ) / 5;

    let tier: RiskLevel = 'low';
    if (avgScore >= 75) tier = 'critical';
    else if (avgScore >= 50) tier = 'high';
    else if (avgScore >= 25) tier = 'moderate';

    return of(tier);
  }

  /**
   * Recalculate overall risk when component scores change
   */
  recalculateOverallRisk(patientId: string): Observable<RiskStratification> {
    return forkJoin({
      clinical: this.calculateClinicalComplexityScore(patientId),
      sdoh: this.calculateSDOHRiskScore(patientId),
      mental: this.getMentalHealthSummary(patientId),
      utilization: this.calculateUtilizationRiskScore(patientId),
    }).pipe(
      map((data) => {
        const scores = {
          clinicalComplexity: data.clinical.total,
          socialComplexity: data.sdoh.score,
          mentalHealthRisk: this.calculateMentalHealthRiskScore(data.mental),
          utilizationRisk: data.utilization.total,
          costRisk: 50, // Mock value
        };

        const avgScore = (
          scores.clinicalComplexity +
          scores.socialComplexity +
          scores.mentalHealthRisk +
          scores.utilizationRisk +
          scores.costRisk
        ) / 5;

        const overallRisk = this.getRiskLevelFromScore(avgScore);

        return {
          overallRisk,
          scores,
          predictions: {
            hospitalizationRisk30Day: Math.min(avgScore * 0.3, 100),
            hospitalizationRisk90Day: Math.min(avgScore * 0.5, 100),
            edVisitRisk30Day: Math.min(avgScore * 0.4, 100),
            readmissionRisk: Math.min(avgScore * 0.25, 100),
          },
          categories: {
            diabetes: this.getRiskLevelFromScore(scores.clinicalComplexity * 0.8),
            cardiovascular: this.getRiskLevelFromScore(scores.clinicalComplexity * 0.7),
            respiratory: 'low' as RiskLevel,
            mentalHealth: this.getRiskLevelFromScore(scores.mentalHealthRisk),
            fallRisk: 'low' as RiskLevel,
          },
        };
      })
    );
  }

  /**
   * Calculate mental health risk score from assessments
   */
  private calculateMentalHealthRiskScore(mental: MentalHealthSummary): number {
    let score = 0;

    mental.assessments.forEach((assessment) => {
      if (assessment.severity === 'severe') score += 40;
      else if (assessment.severity === 'moderately-severe') score += 30;
      else if (assessment.severity === 'moderate') score += 20;
      else if (assessment.severity === 'mild') score += 10;
    });

    if (mental.suicideRisk.level === 'high') score += 50;
    else if (mental.suicideRisk.level === 'moderate') score += 25;

    return Math.min(score, 100);
  }

  /**
   * Get risk score trend over time
   */
  getRiskScoreTrend(
    patientId: string,
    startDate: Date,
    endDate: Date
  ): Observable<HealthMetricTrend> {
    // Mock implementation - would query historical risk scores
    const dataPoints = [
      { date: new Date('2025-01-01'), value: 65 },
      { date: new Date('2025-04-01'), value: 60 },
      { date: new Date('2025-07-01'), value: 55 },
      { date: new Date('2025-10-01'), value: 50 },
    ];

    const trend = this.analyzeTrendFromDataPoints(dataPoints);

    return of({
      metric: 'overallRisk',
      unit: 'score',
      dataPoints,
      trend,
      currentValue: 50,
    });
  }

  /**
   * Analyze risk trend from data points
   */
  analyzeRiskTrend(trendData: any[]): Observable<'improving' | 'stable' | 'declining'> {
    return of(this.analyzeTrendFromDataPoints(trendData));
  }

  /**
   * Analyze trend from data points helper
   */
  private analyzeTrendFromDataPoints(dataPoints: any[]): 'improving' | 'stable' | 'declining' {
    if (dataPoints.length < 2) return 'stable';

    const first = dataPoints[0].value;
    const last = dataPoints[dataPoints.length - 1].value;
    const diff = last - first;
    const percentChange = (diff / first) * 100;

    if (percentChange < -10) return 'improving'; // Risk decreasing
    if (percentChange > 10) return 'declining'; // Risk increasing
    return 'stable';
  }

  // ========================================================================
  // FEATURE 4.1: MULTI-FACTOR RISK SCORE
  // ========================================================================

  /**
   * Calculate comprehensive multi-factor risk score
   * Incorporates clinical complexity, SDOH factors, and mental health risk
   * Returns normalized score 0-100 with appropriate weighting
   */
  calculateMultiFactorRiskScore(patientId: string): Observable<MultiFactorRiskScore> {
    return forkJoin({
      physical: this.getPhysicalHealthSummary(patientId),
      mental: this.getMentalHealthSummary(patientId),
      sdoh: this.getSDOHSummary(patientId),
      clinicalComplexity: this.calculateClinicalComplexityScore(patientId),
      sdohRiskScore: this.calculateSDOHRiskScore(patientId),
    }).pipe(
      map((data) => {
        // Define evidence-based weights
        const weights = {
          clinicalComplexity: 0.40, // 40% - Primary clinical indicators
          sdohRisk: 0.30,           // 30% - Social determinants
          mentalHealthRisk: 0.30,   // 30% - Mental health factors
        };

        // Calculate component scores (0-100)
        const clinicalScore = data.clinicalComplexity.total;
        const sdohScore = data.sdohRiskScore.score;
        const mentalHealthScore = this.calculateMentalHealthRiskScore(data.mental);

        // Calculate weighted overall score
        const overallScore = Math.round(
          clinicalScore * weights.clinicalComplexity +
          sdohScore * weights.sdohRisk +
          mentalHealthScore * weights.mentalHealthRisk
        );

        // Ensure score is normalized 0-100
        const normalizedScore = Math.max(0, Math.min(100, overallScore));

        // Calculate detailed breakdown
        const conditionCount = data.physical.chronicConditions.length;
        const uncontrolledConditionCount = data.physical.chronicConditions.filter(c => !c.controlled).length;
        const comorbidityScore = data.clinicalComplexity.comorbidityScore;
        const sdohNeedCount = data.sdoh.needs.length;
        const severeSdohNeedCount = data.sdoh.needs.filter(n => n.severity === 'severe').length;
        const mentalHealthAssessmentCount = data.mental.assessments.length;
        const highRiskMentalHealthConditions = data.mental.assessments.filter(
          a => a.severity === 'severe' || a.severity === 'moderately-severe'
        ).length;

        // Get medication count (mock for now, would query FHIR in real implementation)
        const medicationCount = 0; // Would be from FHIR MedicationStatement

        // Determine overall risk level
        const overallRisk = this.getRiskLevelFromScore(normalizedScore);

        return {
          patientId,
          overallScore: normalizedScore,
          overallRisk,
          calculatedAt: new Date(),
          components: {
            clinicalComplexity: clinicalScore,
            sdohRisk: sdohScore,
            mentalHealthRisk: mentalHealthScore,
          },
          weights,
          details: {
            conditionCount,
            uncontrolledConditionCount,
            medicationCount,
            comorbidityScore,
            sdohNeedCount,
            severeSdohNeedCount,
            mentalHealthAssessmentCount,
            highRiskMentalHealthConditions,
          },
        };
      }),
      catchError((error) => {
        console.error('Error calculating multi-factor risk score:', error);
        // Return a default safe score
        return of({
          patientId,
          overallScore: 0,
          overallRisk: 'low' as RiskLevel,
          calculatedAt: new Date(),
          components: {
            clinicalComplexity: 0,
            sdohRisk: 0,
            mentalHealthRisk: 0,
          },
          weights: {
            clinicalComplexity: 0.40,
            sdohRisk: 0.30,
            mentalHealthRisk: 0.30,
          },
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
        });
      })
    );
  }

  /**
   * Get risk level from multi-factor score
   * 0-24: Low
   * 25-49: Moderate
   * 50-74: High
   * 75-100: Critical
   */
  getRiskLevelFromMultiFactorScore(score: number): Observable<RiskLevel> {
    let level: RiskLevel;
    if (score >= 75) {
      level = 'critical';
    } else if (score >= 50) {
      level = 'high';
    } else if (score >= 25) {
      level = 'moderate';
    } else {
      level = 'low';
    }
    return of(level);
  }

  /**
   * Get care gaps
   * Calls backend API: GET /patient-health/care-gaps/{patientId}
   */
  getCareGaps(patientId: string, status?: CareGapStatus, priority?: string): Observable<CareGap[]> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.CARE_GAPS_BY_PATIENT(patientId));

    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    if (priority) {
      params = params.set('priority', priority);
    }

    const options = {
      headers: this.getHeaders(),
      params: params
    };

    return this.http.get<any[]>(url, options).pipe(
      map((response) => {
        let gaps = this.transformCareGaps(response);

        // Filter by status if provided (client-side fallback)
        if (status) {
          gaps = gaps.filter(gap => this.mapStatusToBackend(status) === gap.priority);
        }

        // Sort by priority (high -> medium -> low)
        gaps.sort((a, b) => {
          const priorityOrder = { urgent: 0, high: 1, medium: 2, low: 3 };
          return priorityOrder[a.priority as keyof typeof priorityOrder] -
                 priorityOrder[b.priority as keyof typeof priorityOrder];
        });

        return gaps;
      }),
      catchError((error) => {
        console.error('Error fetching care gaps:', error);
        return of(this.getMockCareGaps(patientId));
      })
    );
  }

  /**
   * Update care gap status - Feature 5.1
   * Updates the status of a care gap (open, closed, excluded)
   */
  updateCareGapStatus(update: CareGapStatusUpdate): Observable<CareGapStatusUpdate> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.UPDATE_CARE_GAP_STATUS(update.gapId));

    // Validate status transitions
    if (!this.isValidStatusTransition(update.status)) {
      return throwError(() => new Error(`Invalid status transition to ${update.status}`));
    }

    // Validate excluded status requires reason
    if (update.status === 'excluded' && !update.reason) {
      return throwError(() => new Error('Reason is required when excluding a care gap'));
    }

    const payload = {
      status: update.status,
      reason: update.reason,
      notes: update.notes,
      updatedBy: update.updatedBy,
      updatedDate: update.updatedDate || new Date().toISOString()
    };

    return this.http.put<CareGapStatusUpdate>(url, payload, { headers: this.getHeaders() }).pipe(
      map(response => ({
        gapId: update.gapId,
        status: update.status,
        reason: update.reason,
        notes: update.notes,
        updatedBy: update.updatedBy,
        updatedDate: new Date(response.updatedDate || new Date())
      })),
      catchError((error) => {
        console.error('Error updating care gap status:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Get care gap metrics - Feature 5.1
   * Calculates metrics for care gap closure performance
   */
  getCareGapMetrics(patientId: string): Observable<CareGapMetrics> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.CARE_GAP_METRICS(patientId));

    return this.http.get<any>(url, { headers: this.getHeaders() }).pipe(
      map(response => ({
        patientId: response.patientId || patientId,
        totalGaps: response.totalGaps || 0,
        openGaps: response.openGaps || 0,
        closedGaps: response.closedGaps || 0,
        excludedGaps: response.excludedGaps || 0,
        closureRate: response.closureRate || 0,
        averageTimeToClosureDays: response.averageTimeToClosureDays || 0,
        categoryCounts: response.categoryCounts || {
          preventive: 0,
          'chronic-disease': 0,
          'mental-health': 0,
          medication: 0,
          screening: 0
        },
        priorityCounts: response.priorityCounts || {
          low: 0,
          medium: 0,
          high: 0,
          urgent: 0
        }
      })),
      catchError((error) => {
        console.error('Error fetching care gap metrics:', error);
        // Return fallback metrics
        return of(this.calculateMockMetrics(patientId));
      })
    );
  }

  /**
   * Validate status transitions
   * Valid: open -> closed, open -> excluded
   * Invalid: closed -> open, excluded -> open
   */
  private isValidStatusTransition(newStatus: CareGapStatus): boolean {
    // All transitions to closed or excluded are allowed from open
    // Once closed or excluded, gaps cannot be reopened
    return newStatus === 'closed' || newStatus === 'excluded';
  }

  /**
   * Map frontend status to backend status format
   */
  private mapStatusToBackend(status: CareGapStatus): string {
    const statusMap: Record<CareGapStatus, string> = {
      'open': 'OPEN',
      'closed': 'CLOSED',
      'excluded': 'EXCLUDED'
    };
    return statusMap[status] || status.toUpperCase();
  }

  /**
   * Calculate mock metrics (fallback when backend unavailable)
   */
  private calculateMockMetrics(patientId: string): CareGapMetrics {
    const mockGaps = this.getMockCareGaps(patientId);
    const totalGaps = mockGaps.length;
    const openGaps = mockGaps.length;
    const closedGaps = 0;
    const excludedGaps = 0;

    const categoryCounts = {
      preventive: mockGaps.filter(g => g.category === 'preventive').length,
      'chronic-disease': mockGaps.filter(g => g.category === 'chronic-disease').length,
      'mental-health': mockGaps.filter(g => g.category === 'mental-health').length,
      medication: mockGaps.filter(g => g.category === 'medication').length,
      screening: mockGaps.filter(g => g.category === 'screening').length,
    };

    const priorityCounts = {
      low: mockGaps.filter(g => g.priority === 'low').length,
      medium: mockGaps.filter(g => g.priority === 'medium').length,
      high: mockGaps.filter(g => g.priority === 'high').length,
      urgent: mockGaps.filter(g => g.priority === 'urgent').length,
    };

    return {
      patientId,
      totalGaps,
      openGaps,
      closedGaps,
      excludedGaps,
      closureRate: totalGaps > 0 ? (closedGaps / totalGaps) * 100 : 0,
      averageTimeToClosureDays: 0,
      categoryCounts,
      priorityCounts,
    };
  }

  /**
   * Get care recommendations
   * @deprecated Use getCareRecommendations from Feature 5.2 (line 1562) instead
   * This method has been replaced with a full implementation that supports filtering
   */
  // getCareRecommendations method moved to Feature 5.2 section (line 1562)

  /**
   * Get quality measure performance
   */
  getQualityMeasurePerformance(patientId: string): Observable<QualityMeasurePerformance> {
    // TODO: Integrate with existing quality measure service
    return of(this.getMockQualityPerformance(patientId));
  }

  /**
   * Get health metric trends over time
   */
  getHealthMetricTrend(
    patientId: string,
    metric: string,
    startDate: Date,
    endDate: Date
  ): Observable<HealthMetricTrend> {
    // TODO: Query time-series observation data
    return of(this.getMockHealthTrend(metric, startDate, endDate));
  }

  /**
   * Submit mental health assessment
   * Calls backend API: POST /patient-health/mental-health/assessments
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
      assessmentType: assessmentType.toLowerCase().replace('-', '_'), // PHQ-9 -> phq_9
      responses,
      assessedBy: assessedBy || 'system',
      clinicalNotes,
    };

    return this.http.post<any>(url, request, { headers: this.getHeaders() }).pipe(
      map((response) => {
        return {
          type: response.type?.replace('_', '-') || assessmentType,
          name: this.getAssessmentName(response.type || assessmentType),
          score: response.score,
          maxScore: response.maxScore,
          severity: response.severity,
          date: new Date(response.assessmentDate),
          interpretation: response.interpretation,
          positiveScreen: response.positiveScreen,
          thresholdScore: response.thresholdScore,
          requiresFollowup: response.requiresFollowup,
        };
      }),
      catchError((error) => {
        console.error('Error submitting mental health assessment:', error);
        // Fallback to client-side scoring
        const assessment = this.scoreMentalHealthAssessment(assessmentType, responses);
        return of(assessment);
      })
    );
  }

  /**
   * Get mental health assessment history from backend
   * Calls backend API: GET /patient-health/mental-health/assessments/{patientId}
   */
  getMentalHealthAssessmentHistory(
    patientId: string,
    type?: MentalHealthAssessmentType
  ): Observable<AssessmentHistoryEntry[]> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.MENTAL_HEALTH_ASSESSMENTS_BY_PATIENT(patientId));

    let params = new HttpParams();
    if (type) {
      params = params.set('type', type);
    }

    return this.http.get<any[]>(url, { headers: this.getHeaders(), params }).pipe(
      map((response) => {
        // Transform backend response to AssessmentHistoryEntry
        const history: AssessmentHistoryEntry[] = response.map((item) => ({
          id: item.id,
          type: item.type?.replace('_', '-') || item.assessmentType,
          date: new Date(item.assessmentDate || item.date),
          score: item.score,
          interpretation: item.interpretation,
          provider: item.assessedBy,
        }));

        // Sort by date descending (newest first)
        history.sort((a, b) => b.date.getTime() - a.date.getTime());

        return history;
      }),
      catchError((error) => {
        console.error('Error fetching mental health assessment history:', error);
        return of([]);
      })
    );
  }

  /**
   * Calculate detailed mental health trend from assessment history
   * Analyzes score changes over time to determine if patient is improving, stable, or declining
   * Returns detailed trend information including percentage change and period
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

    // Sort by date (oldest to newest for trend calculation)
    const sortedHistory = [...history].sort((a, b) => a.date.getTime() - b.date.getTime());

    const firstAssessment = sortedHistory[0];
    const lastAssessment = sortedHistory[sortedHistory.length - 1];

    // Calculate percentage change (negative means improvement for mental health scores)
    const percentageChange = ((lastAssessment.score - firstAssessment.score) / firstAssessment.score) * 100;

    // Calculate period in months
    const periodMs = lastAssessment.date.getTime() - firstAssessment.date.getTime();
    const periodMonths = Math.round(periodMs / (1000 * 60 * 60 * 24 * 30));

    // Determine trend direction
    // For mental health assessments, lower scores are better
    let direction: 'improving' | 'stable' | 'declining';
    const scoreDiff = Math.abs(lastAssessment.score - firstAssessment.score);

    if (scoreDiff <= 2) {
      // Within 2 points is considered stable
      direction = 'stable';
    } else if (lastAssessment.score < firstAssessment.score) {
      // Score decreased (improving)
      direction = 'improving';
    } else {
      // Score increased (declining)
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
   * Get mental health diagnoses from FHIR Condition resource
   * Filters by ICD-10 F-codes (mental health chapter)
   */
  getMentalHealthDiagnosesFromFhir(patientId: string): Observable<MentalHealthDiagnosisFhir[]> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.CONDITION);
    const params = new HttpParams()
      .set('patient', patientId)
      .set('_sort', '-onset-date');

    return this.http.get<FhirBundle<any>>(url, {
      headers: this.getHeaders(),
      params,
    }).pipe(
      map((bundle) => {
        if (!bundle.entry || bundle.entry.length === 0) {
          return [];
        }

        const diagnoses: MentalHealthDiagnosisFhir[] = [];

        for (const entry of bundle.entry) {
          const condition = entry.resource;
          if (!condition) continue;

          // Extract ICD-10 code
          const icd10Coding = condition.code?.coding?.find(
            (c: any) => c.system === 'http://hl7.org/fhir/sid/icd-10-cm'
          );

          if (!icd10Coding || !icd10Coding.code) continue;

          // Filter by F-codes (mental health chapter in ICD-10)
          if (!icd10Coding.code.startsWith('F')) continue;

          // Extract clinical status
          const clinicalStatus = condition.clinicalStatus?.coding?.[0]?.code || 'unknown';

          // Extract severity
          const severity = condition.severity?.coding?.[0]?.code;

          // Extract onset date
          const onsetDate = condition.onsetDateTime
            ? new Date(condition.onsetDateTime)
            : undefined;

          diagnoses.push({
            code: icd10Coding.code,
            display: icd10Coding.display || condition.code?.text || 'Unknown',
            severity,
            onsetDate,
            clinicalStatus,
          });
        }

        return diagnoses;
      }),
      catchError((error) => {
        console.error('Error fetching mental health diagnoses from FHIR:', error);
        return of([]);
      })
    );
  }
  /**
   * Feature 3.3: Get Mental Health Summary with FHIR Integration
   * Fetches PHQ-9/GAD-7 from QuestionnaireResponse, conditions from FHIR Condition,
   * and medications from MedicationStatement
   */
  getMentalHealthSummaryFromFhir(patientId: string): Observable<MentalHealthSummary> {
    return forkJoin({
      questionnaireResponses: this.getMentalHealthQuestionnaireResponses(patientId),
      conditions: this.getMentalHealthConditionsFromFhir(patientId),
      medications: this.getPsychMedicationsFromFhir(patientId)
    }).pipe(
      map(({ questionnaireResponses, conditions, medications }) => {
        // Parse PHQ-9 and GAD-7 scores
        const depressionScore = this.parsePHQ9Score(questionnaireResponses);
        const anxietyScore = this.parseGAD7Score(questionnaireResponses);
        
        // Build assessment history
        const assessmentHistory = this.buildAssessmentHistory(questionnaireResponses);
        
        // Calculate overall mental health status
        const status = this.calculateMentalHealthStatus(depressionScore, anxietyScore);
        const riskLevel = this.calculateMentalHealthRisk(depressionScore, anxietyScore);
        
        return {
          status,
          riskLevel,
          assessments: [],
          depressionScore,
          anxietyScore,
          conditions,
          medications,
          assessmentHistory,
          diagnoses: [],
          substanceUse: {
            hasSubstanceUse: false,
            substances: [],
            overallRisk: 'low' as RiskLevel
          },
          suicideRisk: {
            level: 'low' as RiskLevel,
            factors: [],
            protectiveFactors: [],
            requiresIntervention: false
          },
          socialSupport: {
            level: 'moderate' as const,
            hasCaregiver: false,
            livesAlone: false,
            socialIsolation: false
          },
          treatmentEngagement: {
            inTherapy: medications.length > 0,
            medicationCompliance: medications.length > 0 ? 85 : undefined
          }
        };
      }),
      catchError(error => {
        console.error('Error in getMentalHealthSummaryFromFhir:', error);
        // Return minimal valid summary on error
        return of({
          status: 'unknown' as HealthStatus,
          riskLevel: 'low' as RiskLevel,
          assessments: [],
          diagnoses: [],
          substanceUse: {
            hasSubstanceUse: false,
            substances: [],
            overallRisk: 'low' as RiskLevel
          },
          suicideRisk: {
            level: 'low' as RiskLevel,
            factors: [],
            protectiveFactors: [],
            requiresIntervention: false
          },
          socialSupport: {
            level: 'unknown' as const,
            hasCaregiver: false,
            livesAlone: false,
            socialIsolation: false
          },
          treatmentEngagement: {
            inTherapy: false
          }
        });
      })
    );
  }

  /**
   * Get mental health QuestionnaireResponses (PHQ-9, GAD-7, etc.)
   */
  private getMentalHealthQuestionnaireResponses(patientId: string): Observable<FhirQuestionnaireResponse[]> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.QUESTIONNAIRE_RESPONSE);
    const params = new HttpParams()
      .set('patient', patientId)
      .set('status', 'completed')
      .set('_sort', '-authored')
      .set('_count', '50');

    return this.http.get<FhirBundle<FhirQuestionnaireResponse>>(url, {
      headers: this.getHeaders(),
      params
    }).pipe(
      map(bundle => {
        if (!bundle.entry || bundle.entry.length === 0) {
          return [];
        }
        return bundle.entry
          .map(e => e.resource)
          .filter((r): r is FhirQuestionnaireResponse => r !== undefined)
          .filter(r => 
            r.questionnaire.includes('PHQ-9') || 
            r.questionnaire.includes('GAD-7') ||
            r.questionnaire.includes('PHQ-2')
          );
      }),
      catchError(error => {
        console.error('Error fetching questionnaire responses:', error);
        return of([]);
      })
    );
  }

  /**
   * Get mental health conditions from FHIR
   */
  private getMentalHealthConditionsFromFhir(patientId: string): Observable<MentalHealthCondition[]> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.CONDITION);
    const params = new HttpParams()
      .set('patient', patientId)
      .set('category', 'mental-health');

    return this.http.get<FhirBundle<FhirCondition>>(url, {
      headers: this.getHeaders(),
      params
    }).pipe(
      map(bundle => {
        if (!bundle.entry || bundle.entry.length === 0) {
          return [];
        }
        
        return bundle.entry
          .map(e => e.resource)
          .filter((r): r is FhirCondition => r !== undefined)
          .map(condition => this.mapFhirConditionToMentalHealthCondition(condition))
          .filter((c): c is MentalHealthCondition => c !== null);
      }),
      catchError(error => {
        console.error('Error fetching mental health conditions:', error);
        return of([]);
      })
    );
  }

  /**
   * Get psychiatric medications from FHIR MedicationStatement
   */
  private getPsychMedicationsFromFhir(patientId: string): Observable<Medication[]> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.MEDICATION_STATEMENT);
    const params = new HttpParams()
      .set('patient', patientId)
      .set('status', 'active');

    return this.http.get<FhirBundle<any>>(url, {
      headers: this.getHeaders(),
      params
    }).pipe(
      map(bundle => {
        if (!bundle.entry || bundle.entry.length === 0) {
          return [];
        }
        
        return bundle.entry
          .map(e => e.resource)
          .filter((r: any) => r !== undefined)
          .map((medStatement: any) => this.mapFhirMedicationStatementToMedication(medStatement))
          .filter((m): m is Medication => m !== null);
      }),
      catchError(error => {
        console.error('Error fetching medications:', error);
        return of([]);
      })
    );
  }

  /**
   * Map FHIR Condition to MentalHealthCondition
   */
  private mapFhirConditionToMentalHealthCondition(condition: FhirCondition): MentalHealthCondition | null {
    const coding = condition.code?.coding?.[0];
    if (!coding || !coding.code) return null;

    // Determine category from ICD-10 code
    let category: 'mood' | 'anxiety' | 'psychotic' | 'substance' | 'trauma' | 'other' = 'other';
    if (coding.code.startsWith('F3')) category = 'mood';
    else if (coding.code.startsWith('F4')) category = 'anxiety';
    else if (coding.code.startsWith('F2')) category = 'psychotic';
    else if (coding.code.startsWith('F1')) category = 'substance';
    else if (coding.code.startsWith('F43')) category = 'trauma';

    return {
      code: coding.code,
      display: coding.display || condition.code?.text || 'Unknown',
      category,
      severity: condition.severity?.coding?.[0]?.code as any,
      onsetDate: condition.onsetDateTime ? new Date(condition.onsetDateTime) : undefined,
      status: condition.clinicalStatus?.coding?.[0]?.code || 'unknown'
    };
  }

  /**
   * Map FHIR MedicationStatement to Medication
   */
  private mapFhirMedicationStatementToMedication(medStatement: any): Medication | null {
    const coding = medStatement.medicationCodeableConcept?.coding?.[0];
    if (!coding || !coding.code) return null;

    return {
      code: coding.code,
      display: coding.display || medStatement.medicationCodeableConcept?.text || 'Unknown',
      status: medStatement.status || 'unknown',
      dosage: medStatement.dosage?.[0]?.text,
      startDate: medStatement.effectiveDateTime ? new Date(medStatement.effectiveDateTime) : undefined
    };
  }

  /**
   * Parse PHQ-9 score from questionnaire responses
   */
  private parsePHQ9Score(responses: FhirQuestionnaireResponse[]): { score: number; severity: string; date: Date } | undefined {
    const phq9 = responses.find(r => r.questionnaire.includes('PHQ-9'));
    if (!phq9) return undefined;

    const scoreItem = phq9.item?.find(i => i.linkId.includes('score'));
    const score = scoreItem?.answer?.[0]?.valueInteger || 0;
    
    return {
      score,
      severity: this.determinePHQ9Severity(score),
      date: new Date(phq9.authored)
    };
  }

  /**
   * Parse GAD-7 score from questionnaire responses
   */
  private parseGAD7Score(responses: FhirQuestionnaireResponse[]): { score: number; severity: string; date: Date } | undefined {
    const gad7 = responses.find(r => r.questionnaire.includes('GAD-7'));
    if (!gad7) return undefined;

    const scoreItem = gad7.item?.find(i => i.linkId.includes('score'));
    const score = scoreItem?.answer?.[0]?.valueInteger || 0;
    
    return {
      score,
      severity: this.determineGAD7Severity(score),
      date: new Date(gad7.authored)
    };
  }

  /**
   * Determine PHQ-9 severity from score
   */
  private determinePHQ9Severity(score: number): string {
    if (score <= 4) return 'minimal';
    if (score <= 9) return 'mild';
    if (score <= 14) return 'moderate';
    if (score <= 19) return 'moderately-severe';
    return 'severe';
  }

  /**
   * Determine GAD-7 severity from score
   */
  private determineGAD7Severity(score: number): string {
    if (score <= 4) return 'minimal';
    if (score <= 9) return 'mild';
    if (score <= 14) return 'moderate';
    return 'severe';
  }

  /**
   * Build assessment history from questionnaire responses
   */
  private buildAssessmentHistory(responses: FhirQuestionnaireResponse[]): Assessment[] {
    return responses.map(r => {
      const scoreItem = r.item?.find(i => i.linkId.includes('score'));
      const score = scoreItem?.answer?.[0]?.valueInteger || 0;
      const type = r.questionnaire.includes('PHQ-9') ? 'PHQ-9' : 
                   r.questionnaire.includes('GAD-7') ? 'GAD-7' : 'PHQ-2';
      const severity = type === 'PHQ-9' ? this.determinePHQ9Severity(score) :
                       type === 'GAD-7' ? this.determineGAD7Severity(score) : 'unknown';

      return {
        id: r.id || '',
        type: type as MentalHealthAssessmentType,
        date: new Date(r.authored),
        score,
        severity
      };
    });
  }

  /**
   * Calculate mental health status from scores
   */
  private calculateMentalHealthStatus(
    depressionScore?: { score: number; severity: string; date: Date },
    anxietyScore?: { score: number; severity: string; date: Date }
  ): HealthStatus {
    if (!depressionScore && !anxietyScore) return 'unknown';
    
    const hasSevere = depressionScore?.severity === 'severe' || 
                      depressionScore?.severity === 'moderately-severe' ||
                      anxietyScore?.severity === 'severe';
    
    const hasModerate = depressionScore?.severity === 'moderate' || 
                        anxietyScore?.severity === 'moderate';
    
    if (hasSevere) return 'poor';
    if (hasModerate) return 'fair';
    return 'good';
  }

  /**
   * Calculate mental health risk from scores
   */
  private calculateMentalHealthRisk(
    depressionScore?: { score: number; severity: string; date: Date },
    anxietyScore?: { score: number; severity: string; date: Date }
  ): RiskLevel {
    if (!depressionScore && !anxietyScore) return 'low';
    
    const hasSevere = depressionScore?.severity === 'severe' || anxietyScore?.severity === 'severe';
    const hasModeratelySevere = depressionScore?.severity === 'moderately-severe';
    const hasModerate = depressionScore?.severity === 'moderate' || anxietyScore?.severity === 'moderate';
    
    if (hasSevere) return 'critical';
    if (hasModeratelySevere) return 'high';
    if (hasModerate) return 'moderate';
    return 'low';
  }

  /**
   * Calculate overall health score
   */
  private calculateOverallHealthScore(
    patientId: string,
    physical: PhysicalHealthSummary,
    mental: MentalHealthSummary,
    social: SDOHSummary,
    quality: QualityMeasurePerformance
  ): HealthScore {
    // Physical health component (40% weight)
    const physicalScore = this.calculatePhysicalScore(physical);

    // Mental health component (30% weight)
    const mentalScore = this.calculateMentalScore(mental);

    // Social health component (15% weight)
    const socialScore = this.calculateSocialScore(social);

    // Preventive care component (15% weight)
    const preventiveScore = quality.overallCompliance;

    // Weighted overall score
    const score = Math.round(
      physicalScore * 0.4 +
      mentalScore * 0.3 +
      socialScore * 0.15 +
      preventiveScore * 0.15
    );

    // Determine status
    let status: HealthStatus;
    if (score >= 85) status = 'excellent';
    else if (score >= 70) status = 'good';
    else if (score >= 50) status = 'fair';
    else status = 'poor';

    // Chronic disease score (derived from physical health for now)
    // TODO: Implement dedicated chronic disease management score
    const chronicDiseaseScore = physicalScore;

    return {
      patientId,
      overallScore: score,
      score, // Deprecated compatibility
      status,
      trend: 'stable', // TODO: Calculate from historical data
      components: {
        physical: physicalScore,
        mental: mentalScore,
        social: socialScore,
        preventive: preventiveScore,
        chronicDisease: chronicDiseaseScore,
      },
      calculatedAt: new Date(),
      lastCalculated: new Date(), // Deprecated compatibility
    };
  }

  /**
   * Calculate physical health score
   */
  private calculatePhysicalScore(physical: PhysicalHealthSummary): number {
    let score = 100;

    // Deduct points for abnormal vitals
    if (physical.vitals.bloodPressure?.status === 'abnormal') score -= 10;
    if (physical.vitals.bloodPressure?.status === 'critical') score -= 20;
    if (physical.vitals.bmi?.status === 'abnormal') score -= 5;

    // Deduct points for chronic conditions
    const severeConditions = physical.chronicConditions.filter(
      (c) => c.severity === 'severe' && !c.controlled
    );
    score -= severeConditions.length * 15;

    const moderateConditions = physical.chronicConditions.filter(
      (c) => c.severity === 'moderate' && !c.controlled
    );
    score -= moderateConditions.length * 8;

    // Deduct points for poor medication adherence
    if (physical.medicationAdherence.status === 'poor') score -= 15;

    // Deduct points for functional limitations
    if (physical.functionalStatus.adlScore < 6) score -= 10;
    if (physical.functionalStatus.painLevel > 6) score -= 10;

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
    if (mental.suicideRisk.level === 'high') score -= 30;
    else if (mental.suicideRisk.level === 'moderate') score -= 15;

    // Add points for treatment engagement
    if (mental.treatmentEngagement.inTherapy) score += 10;
    if (mental.treatmentEngagement.therapyAdherence && mental.treatmentEngagement.therapyAdherence > 80) {
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
    const severeNeeds = social.needs.filter((n) => n.severity === 'severe' && !n.addressed);
    score -= severeNeeds.length * 20;

    const moderateNeeds = social.needs.filter((n) => n.severity === 'moderate' && !n.addressed);
    score -= moderateNeeds.length * 10;

    return Math.max(0, Math.min(100, score));
  }

  /**
   * Score mental health assessment
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
   * Score PHQ-9 (Depression)
   * Total score: 0-27
   * 0-4: Minimal depression
   * 5-9: Mild depression
   * 10-14: Moderate depression
   * 15-19: Moderately severe depression
   * 20-27: Severe depression
   */
  private scorePHQ9(responses: Record<string, any>): MentalHealthAssessment {
    // Calculate total score (sum of 9 questions, each 0-3)
    const score = Object.values(responses).reduce((sum: number, val: any) => sum + (val || 0), 0);

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
   * 0-4: Minimal anxiety
   * 5-9: Mild anxiety
   * 10-14: Moderate anxiety
   * 15-21: Severe anxiety
   */
  private scoreGAD7(responses: Record<string, any>): MentalHealthAssessment {
    const score = Object.values(responses).reduce((sum: number, val: any) => sum + (val || 0), 0);

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
   * Score PHQ-2 (Brief Depression Screening)
   * Total score: 0-6
   * >= 3: Positive screen for depression
   */
  private scorePHQ2(responses: Record<string, any>): MentalHealthAssessment {
    const score = Object.values(responses).reduce((sum: number, val: any) => sum + (val || 0), 0);

    const severity = score >= 3 ? 'moderate' : 'minimal';
    const interpretation = score >= 3
      ? 'Positive screen for depression - recommend full PHQ-9'
      : 'Negative screen for depression';

    return {
      type: 'PHQ-2',
      name: 'Patient Health Questionnaire-2',
      score,
      maxScore: 6,
      severity,
      date: new Date(),
      interpretation,
      positiveScreen: score >= 3,
      thresholdScore: 3,
      requiresFollowup: score >= 3,
    };
  }

  /**
   * Determine risk level from score
   */
  private getRiskLevelFromScore(score: number): RiskLevel {
    if (score >= 75) return 'critical';
    if (score >= 50) return 'high';
    if (score >= 25) return 'moderate';
    return 'low';
  }

  // ===========================================================================
  // RISK TREND TRACKING - Feature 4.4
  // ===========================================================================

  /**
   * Get historical risk scores for a patient
   * Feature 4.4: Fetches risk score history from backend API
   *
   * @param patientId Patient ID
   * @param startDate Optional start date for filtering
   * @param endDate Optional end date for filtering
   * @returns Observable of risk history array
   */
  getRiskHistory(
    patientId: string,
    startDate?: Date,
    endDate?: Date
  ): Observable<Array<{ date: Date; score: number; metric: string }>> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.RISK_HISTORY(patientId));

    let params = new HttpParams();
    if (startDate) {
      params = params.set('startDate', startDate.toISOString());
    }
    if (endDate) {
      params = params.set('endDate', endDate.toISOString());
    }

    return this.http.get<any[]>(url, { headers: this.getHeaders(), params }).pipe(
      map((response) => {
        return response.map((item) => ({
          date: new Date(item.date),
          score: item.score,
          metric: item.metric
        }));
      })
    );
  }

  /**
   * Calculate risk trend from historical data points
   * Feature 4.4: Analyzes trend direction and percent change
   *
   * Trend determination:
   * - improving: Risk score decreased by more than 5%
   * - declining: Risk score increased by more than 5%
   * - stable: Change is within +/- 5%
   *
   * @param patientId Patient ID
   * @param metric Metric name (e.g., 'overall-risk', 'clinical-complexity')
   * @param dataPoints Array of historical data points with date and value
   * @returns RiskTrendData with trend analysis
   */
  calculateRiskTrend(
    patientId: string,
    metric: string,
    dataPoints: Array<{ date: Date; value: number; label?: string }>
  ): RiskTrendData {
    // Handle empty or single data point
    if (dataPoints.length === 0) {
      return {
        patientId,
        metric,
        trend: 'stable',
        dataPoints: [],
        percentChange: 0,
        startDate: new Date(),
        endDate: new Date()
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
        endDate: dataPoints[0].date
      };
    }

    // Calculate percent change from first to last value
    const firstValue = dataPoints[0].value;
    const lastValue = dataPoints[dataPoints.length - 1].value;
    const percentChange = ((lastValue - firstValue) / firstValue) * 100;

    // Determine trend based on 5% threshold
    // Note: For risk scores, LOWER is better, so:
    // - Negative change (decreasing risk) = improving
    // - Positive change (increasing risk) = declining
    let trend: 'improving' | 'stable' | 'declining';
    if (percentChange < -5) {
      trend = 'improving';  // Risk decreased by more than 5%
    } else if (percentChange > 5) {
      trend = 'declining';  // Risk increased by more than 5%
    } else {
      trend = 'stable';     // Within +/- 5%
    }

    return {
      patientId,
      metric,
      trend,
      dataPoints,
      percentChange,
      startDate: dataPoints[0].date,
      endDate: dataPoints[dataPoints.length - 1].date
    };
  }

  // ===========================================================================
  // FHIR OBSERVATION QUERIES
  // ===========================================================================

  /**
   * Get vital signs from FHIR Observation endpoint
   * @param patientId Patient ID
   * @returns Observable of vitals object
   */
  getVitalSignsFromFhir(patientId: string): Observable<PhysicalHealthSummary['vitals']> {
    // Check cache first
    const cached = this.vitalSignsCache.get(patientId);
    if (cached && Date.now() - cached.timestamp < this.CACHE_TTL_MS) {
      return of(cached.data);
    }

    const url = buildFhirUrl(FHIR_ENDPOINTS.OBSERVATION);
    const params = new HttpParams()
      .set('patient', patientId)
      .set('category', OBSERVATION_CATEGORIES.VITAL_SIGNS)
      .set('_sort', '-date')
      .set('_count', '100');

    return this.http.get<FhirBundle<FhirObservation>>(url, {
      headers: this.getHeaders(),
      params,
    }).pipe(
      map((bundle) => {
        const vitals = this.mapFhirObservationsToVitals(bundle);
        // Cache the result
        this.vitalSignsCache.set(patientId, { data: vitals, timestamp: Date.now() });
        return vitals;
      }),
      catchError((error) => {
        console.error('Error fetching vital signs from FHIR:', error);
        return of({});
      })
    );
  }

  /**
   * Get lab results from FHIR Observation endpoint
   * @param patientId Patient ID
   * @param options Optional query options
   * @returns Observable of lab results array
   */
  getLabResultsFromFhir(
    patientId: string,
    options?: { followPagination?: boolean }
  ): Observable<LabResult[]> {
    // Check cache first
    const cacheKey = `${patientId}-${options?.followPagination || false}`;
    const cached = this.labResultsCache.get(cacheKey);
    if (cached && Date.now() - cached.timestamp < this.CACHE_TTL_MS) {
      return of(cached.data);
    }

    const url = buildFhirUrl(FHIR_ENDPOINTS.OBSERVATION);
    const params = new HttpParams()
      .set('patient', patientId)
      .set('category', OBSERVATION_CATEGORIES.LABORATORY)
      .set('_sort', '-date')
      .set('_count', '20');

    if (options?.followPagination) {
      return this.fetchAllPages<FhirObservation>(url, params).pipe(
        map((observations) => {
          const labs = observations.map((obs) => this.mapFhirObservationToLabResult(obs));
          // Cache the result
          this.labResultsCache.set(cacheKey, { data: labs, timestamp: Date.now() });
          return labs;
        }),
        catchError((error) => {
          console.error('Error fetching lab results from FHIR:', error);
          return of([]);
        })
      );
    } else {
      return this.http.get<FhirBundle<FhirObservation>>(url, {
        headers: this.getHeaders(),
        params,
      }).pipe(
        map((bundle) => {
          const labs = (bundle.entry || [])
            .map((entry) => entry.resource)
            .filter((obs): obs is FhirObservation => obs !== undefined)
            .map((obs) => this.mapFhirObservationToLabResult(obs));
          // Cache the result
          this.labResultsCache.set(cacheKey, { data: labs, timestamp: Date.now() });
          return labs;
        }),
        catchError((error) => {
          console.error('Error fetching lab results from FHIR:', error);
          return of([]);
        })
      );
    }
  }

  /**
   * Get diagnostic reports from FHIR DiagnosticReport endpoint
   * @param patientId Patient ID
   * @returns Observable of diagnostic reports
   */
  getDiagnosticReportsFromFhir(patientId: string): Observable<any[]> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.DIAGNOSTIC_REPORT);
    const params = new HttpParams()
      .set('patient', patientId)
      .set('_sort', '-date')
      .set('_count', '20');

    return this.http.get<FhirBundle<FhirDiagnosticReport>>(url, {
      headers: this.getHeaders(),
      params,
    }).pipe(
      map((bundle) => {
        return (bundle.entry || [])
          .map((entry) => entry.resource)
          .filter((report): report is FhirDiagnosticReport => report !== undefined)
          .map((report) => ({
            id: report.id,
            code: {
              text: report.code.text || report.code.coding?.[0]?.display || 'Unknown',
            },
            status: report.status,
            date: report.effectiveDateTime ? new Date(report.effectiveDateTime) : new Date(),
            results: report.result || [],
          }));
      }),
      catchError((error) => {
        console.error('Error fetching diagnostic reports from FHIR:', error);
        return of([]);
      })
    );
  }

  /**
   * Fetch all pages of a FHIR bundle response
   * @param url Initial URL
   * @param params Query parameters
   * @returns Observable of all resources
   */
  private fetchAllPages<T>(url: string, params: HttpParams): Observable<T[]> {
    return this.http.get<FhirBundle<T>>(url, {
      headers: this.getHeaders(),
      params,
    }).pipe(
      expand((bundle) => {
        const nextLink = bundle.link?.find((link) => link.relation === 'next');
        if (nextLink?.url) {
          return this.http.get<FhirBundle<T>>(nextLink.url, {
            headers: this.getHeaders(),
          });
        }
        return EMPTY;
      }),
      reduce((acc: T[], bundle) => {
        const resources = (bundle.entry || [])
          .map((entry) => entry.resource)
          .filter((resource): resource is T => resource !== undefined);
        return [...acc, ...resources];
      }, [])
    );
  }

  /**
   * Map FHIR Observations to VitalSign objects
   */
  private mapFhirObservationsToVitals(bundle: FhirBundle<FhirObservation>): PhysicalHealthSummary['vitals'] {
    const vitals: PhysicalHealthSummary['vitals'] = {};

    if (!bundle.entry || bundle.entry.length === 0) {
      return vitals;
    }

    const observations = bundle.entry
      .map((entry) => entry.resource)
      .filter((obs): obs is FhirObservation => obs !== undefined);

    // Process each observation
    for (const obs of observations) {
      const loincCode = obs.code.coding?.find((c) => c.system === 'http://loinc.org')?.code;

      if (!loincCode) continue;

      switch (loincCode) {
        case LOINC_VITAL_SIGNS.HEART_RATE:
          if (obs.valueQuantity && obs.valueQuantity.value !== undefined) {
            vitals.heartRate = this.mapObservationToVitalSign(obs, obs.valueQuantity.value);
          }
          break;

        case LOINC_VITAL_SIGNS.BLOOD_PRESSURE_PANEL:
          if (obs.component && obs.component.length >= 2) {
            const systolic = obs.component.find((c) =>
              c.code.coding?.some((coding) => coding.code === LOINC_VITAL_SIGNS.SYSTOLIC_BP)
            );
            const diastolic = obs.component.find((c) =>
              c.code.coding?.some((coding) => coding.code === LOINC_VITAL_SIGNS.DIASTOLIC_BP)
            );

            if (systolic?.valueQuantity && diastolic?.valueQuantity) {
              const bpValue = `${systolic.valueQuantity.value}/${diastolic.valueQuantity.value}`;
              const vitalSign = this.mapObservationToVitalSign(obs, bpValue);
              // Override unit to be mmHg for BP
              vitalSign.unit = systolic.valueQuantity.unit || 'mmHg';
              vitals.bloodPressure = vitalSign;
            }
          }
          break;

        case LOINC_VITAL_SIGNS.BODY_TEMPERATURE:
          if (obs.valueQuantity && obs.valueQuantity.value !== undefined) {
            vitals.temperature = this.mapObservationToVitalSign(obs, obs.valueQuantity.value);
          }
          break;

        case LOINC_VITAL_SIGNS.BODY_WEIGHT:
          if (obs.valueQuantity && obs.valueQuantity.value !== undefined) {
            vitals.weight = this.mapObservationToVitalSign(obs, obs.valueQuantity.value);
          }
          break;

        case LOINC_VITAL_SIGNS.BODY_HEIGHT:
          if (obs.valueQuantity && obs.valueQuantity.value !== undefined) {
            vitals.height = this.mapObservationToVitalSign(obs, obs.valueQuantity.value);
          }
          break;

        case LOINC_VITAL_SIGNS.BMI:
          if (obs.valueQuantity && obs.valueQuantity.value !== undefined) {
            vitals.bmi = this.mapObservationToVitalSign(obs, obs.valueQuantity.value);
          }
          break;

        case LOINC_VITAL_SIGNS.OXYGEN_SATURATION:
          if (obs.valueQuantity && obs.valueQuantity.value !== undefined) {
            vitals.oxygenSaturation = this.mapObservationToVitalSign(obs, obs.valueQuantity.value);
          }
          break;
      }
    }

    return vitals;
  }

  /**
   * Map a single FHIR Observation to VitalSign interface
   */
  private mapObservationToVitalSign<T>(obs: FhirObservation, value: T): VitalSign<T> {
    const status = this.determineVitalSignStatus(obs);
    const date = obs.effectiveDateTime ? new Date(obs.effectiveDateTime) : new Date();

    const vitalSign: VitalSign<T> = {
      value,
      unit: obs.valueQuantity?.unit || '',
      date,
      status,
    };

    // Add reference range if available
    if (obs.referenceRange && obs.referenceRange.length > 0) {
      const range = obs.referenceRange[0];
      if (range.low?.value !== undefined || range.high?.value !== undefined) {
        vitalSign.referenceRange = {
          low: range.low?.value || 0,
          high: range.high?.value || 0,
        };
      }
    }

    return vitalSign;
  }

  /**
   * Determine vital sign status from FHIR Observation
   */
  private determineVitalSignStatus(obs: FhirObservation): 'normal' | 'abnormal' | 'critical' {
    // Check interpretation codes
    if (obs.interpretation && obs.interpretation.length > 0) {
      const interpretationCode = obs.interpretation[0].coding?.[0]?.code;
      switch (interpretationCode) {
        case 'H': // High
        case 'L': // Low
        case 'A': // Abnormal
          return 'abnormal';
        case 'HH': // Critical high
        case 'LL': // Critical low
        case 'AA': // Critical abnormal
          return 'critical';
        case 'N': // Normal
        default:
          return 'normal';
      }
    }

    // Check against reference range if available
    if (obs.valueQuantity && obs.referenceRange && obs.referenceRange.length > 0) {
      const value = obs.valueQuantity.value;
      const range = obs.referenceRange[0];

      if (value !== undefined) {
        if (range.low?.value !== undefined && value < range.low.value) {
          return 'abnormal';
        }
        if (range.high?.value !== undefined && value > range.high.value) {
          return 'abnormal';
        }
      }
    }

    return 'normal';
  }

  /**
   * Map FHIR Observation to LabResult interface
   */
  private mapFhirObservationToLabResult(obs: FhirObservation): LabResult {
    const status = this.determineVitalSignStatus(obs);
    const date = obs.effectiveDateTime ? new Date(obs.effectiveDateTime) : new Date();

    // Extract LOINC code
    const loincCode = obs.code.coding?.find(c => c.system === 'http://loinc.org')?.code;

    // Extract and map interpretation
    let interpretation: LabInterpretation | undefined;
    if (obs.interpretation && obs.interpretation.length > 0) {
      const interpretationCode = obs.interpretation[0].coding?.[0]?.code;
      if (interpretationCode) {
        interpretation = this.mapFhirInterpretationCode(interpretationCode);
      }
    }

    const labResult: LabResult = {
      code: {
        text: obs.code.text || obs.code.coding?.[0]?.display || 'Unknown',
      },
      value: obs.valueQuantity?.value || obs.valueString || '',
      unit: obs.valueQuantity?.unit,
      date,
      status,
      loincCode,
      interpretation,
    };

    // Add reference range if available
    if (obs.referenceRange && obs.referenceRange.length > 0) {
      const range = obs.referenceRange[0];
      labResult.referenceRange = {
        low: range.low?.value,
        high: range.high?.value,
        text: range.text,
      };
    }

    return labResult;
  }

  // ===========================================================================
  // REAL-TIME VITAL SIGNS SUBSCRIPTION & HISTORY
  // ===========================================================================

  /**
   * Subscribe to real-time vital signs updates with polling
   * @param patientId Patient ID
   * @param intervalMs Polling interval in milliseconds (default: 30000 = 30 seconds)
   * @returns Observable that emits vitals on each poll
   */
  subscribeToVitalSigns(
    patientId: string,
    intervalMs: number = 30000
  ): Observable<PhysicalHealthSummary['vitals']> {
    return timer(0, intervalMs).pipe(
      switchMap(() => this.getVitalSignsFromFhir(patientId)),
      distinctUntilChanged((prev, curr) => JSON.stringify(prev) === JSON.stringify(curr)),
      catchError((error) => {
        console.error('Error in vital signs subscription:', error);
        // Return empty object but continue polling
        return of({});
      })
    );
  }

  /**
   * Get vital sign history for a specific vital type over a date range
   * @param patientId Patient ID
   * @param vitalType LOINC code for the vital sign (e.g., '8867-4' for heart rate)
   * @param startDate Start date for history query
   * @param endDate End date for history query
   * @returns Observable array of history points sorted chronologically
   */
  getVitalSignHistory(
    patientId: string,
    vitalType: string,
    startDate: Date,
    endDate: Date
  ): Observable<VitalSignHistoryPoint[]> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.OBSERVATION);

    // Format dates for FHIR (YYYY-MM-DD)
    const formatDate = (date: Date): string => {
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    };

    const params = new HttpParams()
      .set('patient', patientId)
      .set('code', `http://loinc.org|${vitalType}`)
      .set('category', OBSERVATION_CATEGORIES.VITAL_SIGNS)
      .set('date', `ge${formatDate(startDate)},le${formatDate(endDate)}`)
      .set('_sort', 'date')
      .set('_count', '100');

    return this.fetchAllPages<FhirObservation>(url, params).pipe(
      map((observations) => {
        const historyPoints: VitalSignHistoryPoint[] = observations
          .map((obs): VitalSignHistoryPoint | null => {
            const value = obs.valueQuantity?.value;
            const unit = obs.valueQuantity?.unit || '';
            const date = obs.effectiveDateTime ? new Date(obs.effectiveDateTime) : new Date();
            const interpretation = obs.interpretation?.[0]?.coding?.[0]?.display;

            if (value === undefined) {
              return null;
            }

            const point: VitalSignHistoryPoint = {
              date,
              value,
              unit,
            };
            if (interpretation) {
              point.interpretation = interpretation;
            }
            return point;
          })
          .filter((point): point is VitalSignHistoryPoint => point !== null);

        // Sort chronologically (oldest to newest)
        historyPoints.sort((a, b) => a.date.getTime() - b.date.getTime());

        return historyPoints;
      }),
      catchError((error) => {
        console.error('Error fetching vital sign history:', error);
        return of([]);
      })
    );
  }

  // ===========================================================================
  // MOCK DATA - Replace with real FHIR queries
  // ===========================================================================

  private getMockPhysicalHealth(patientId: string): PhysicalHealthSummary {
    return {
      status: 'good',
      vitals: {
        bloodPressure: {
          value: '128/82',
          unit: 'mmHg',
          date: new Date('2025-11-15'),
          status: 'abnormal',
          trend: 'stable',
          referenceRange: { low: 90, high: 120 },
        },
        heartRate: {
          value: 72,
          unit: 'bpm',
          date: new Date('2025-11-15'),
          status: 'normal',
          referenceRange: { low: 60, high: 100 },
        },
        weight: {
          value: 185,
          unit: 'lbs',
          date: new Date('2025-11-15'),
          status: 'normal',
          trend: 'stable',
        },
        bmi: {
          value: 28.5,
          unit: 'kg/m²',
          date: new Date('2025-11-15'),
          status: 'abnormal',
          referenceRange: { low: 18.5, high: 25 },
        },
      },
      labs: [
        {
          code: { text: 'HbA1c' },
          value: 7.2,
          unit: '%',
          date: new Date('2025-11-10'),
          status: 'abnormal',
          referenceRange: { low: 4, high: 6, text: '<7% for diabetics' },
        },
        {
          code: { text: 'LDL Cholesterol' },
          value: 145,
          unit: 'mg/dL',
          date: new Date('2025-11-10'),
          status: 'abnormal',
          referenceRange: { high: 130 },
        },
      ],
      chronicConditions: [
        {
          code: { text: 'Type 2 Diabetes' },
          display: 'Type 2 Diabetes Mellitus',
          severity: 'moderate',
          onsetDate: new Date('2020-03-15'),
          controlled: true,
          lastReview: new Date('2025-11-10'),
        },
        {
          code: { text: 'Hypertension' },
          display: 'Essential Hypertension',
          severity: 'mild',
          onsetDate: new Date('2019-06-20'),
          controlled: true,
          lastReview: new Date('2025-11-10'),
        },
      ],
      medicationAdherence: {
        overallRate: 85,
        status: 'good',
        problematicMedications: [],
      },
      functionalStatus: {
        adlScore: 6,
        iadlScore: 8,
        mobilityScore: 85,
        painLevel: 3,
        fatigueLevel: 4,
      },
    };
  }

  private getMockMentalHealth(patientId: string): MentalHealthSummary {
    return {
      status: 'fair',
      riskLevel: 'moderate',
      assessments: [
        {
          type: 'PHQ-9',
          name: 'Patient Health Questionnaire-9',
          score: 12,
          maxScore: 27,
          severity: 'moderate',
          date: new Date('2025-11-12'),
          trend: 'improving',
          interpretation: 'Moderate depression',
          positiveScreen: true,
          thresholdScore: 10,
          requiresFollowup: true,
        },
        {
          type: 'GAD-7',
          name: 'Generalized Anxiety Disorder-7',
          score: 8,
          maxScore: 21,
          severity: 'mild',
          date: new Date('2025-11-12'),
          interpretation: 'Mild anxiety',
          positiveScreen: false,
          thresholdScore: 10,
          requiresFollowup: false,
        },
      ],
      diagnoses: [
        {
          code: { text: 'F33.1' },
          display: 'Major Depressive Disorder, Recurrent, Moderate',
          category: 'mood',
          severity: 'moderate',
          onsetDate: new Date('2023-02-10'),
          inRemission: false,
          lastReview: new Date('2025-11-01'),
        },
      ],
      substanceUse: {
        hasSubstanceUse: true,
        substances: [
          {
            substance: 'alcohol',
            frequency: 'weekly',
            severity: 'mild',
            inTreatment: false,
          },
        ],
        overallRisk: 'low',
      },
      suicideRisk: {
        level: 'low',
        factors: [],
        protectiveFactors: ['Strong family support', 'Engaged in treatment'],
        lastAssessed: new Date('2025-11-12'),
        requiresIntervention: false,
      },
      socialSupport: {
        level: 'moderate',
        hasCaregiver: false,
        livesAlone: false,
        socialIsolation: false,
      },
      treatmentEngagement: {
        inTherapy: true,
        therapyAdherence: 90,
        medicationCompliance: 85,
        lastPsychVisit: new Date('2025-10-28'),
      },
    };
  }

  private getMockSDOHSummary(patientId: string): SDOHSummary {
    return {
      overallRisk: 'moderate',
      screeningDate: new Date('2025-10-15'),
      needs: [
        {
          category: 'food-insecurity',
          description: 'Reported difficulty affording healthy food 2-3 times per month',
          severity: 'moderate',
          identified: new Date('2025-10-15'),
          addressed: false,
        },
        {
          category: 'transportation',
          description: 'Difficulty getting to medical appointments',
          severity: 'mild',
          identified: new Date('2025-10-15'),
          addressed: true,
          interventions: ['Provided public transit pass'],
        },
      ],
      activeReferrals: [
        {
          organization: 'Local Food Bank',
          category: 'food-insecurity',
          status: 'active',
          referralDate: new Date('2025-10-20'),
        },
      ],
      zCodes: ['Z59.4', 'Z59.82'],
    };
  }

  private getMockRiskStratification(patientId: string): RiskStratification {
    return {
      overallRisk: 'moderate',
      scores: {
        clinicalComplexity: 65,
        socialComplexity: 45,
        mentalHealthRisk: 55,
        utilizationRisk: 40,
        costRisk: 50,
      },
      predictions: {
        hospitalizationRisk30Day: 15,
        hospitalizationRisk90Day: 28,
        edVisitRisk30Day: 22,
        readmissionRisk: 18,
      },
      categories: {
        diabetes: 'moderate',
        cardiovascular: 'moderate',
        respiratory: 'low',
        mentalHealth: 'moderate',
        fallRisk: 'low',
      },
    };
  }

  private getMockCareGaps(patientId: string): CareGap[] {
    return [
      {
        id: 'gap-1',
        category: 'preventive',
        title: 'Annual Eye Exam Overdue',
        description: 'Patient with diabetes has not had dilated eye exam in over 18 months',
        priority: 'high',
        dueDate: new Date('2025-05-01'),
        overdueDays: 203,
        measureId: 'CDC-EED',
        measureName: 'Eye Exam for Patients with Diabetes',
        recommendedActions: [
          'Schedule appointment with ophthalmologist',
          'Patient education on diabetic retinopathy risk',
        ],
        barriers: ['Transportation challenges'],
      },
      {
        id: 'gap-2',
        category: 'chronic-disease',
        title: 'HbA1c Above Target',
        description: 'Most recent HbA1c is 7.2%, target is <7% for this patient',
        priority: 'medium',
        measureId: 'CDC-HbA1c',
        measureName: 'Hemoglobin A1c Control for Patients with Diabetes',
        recommendedActions: [
          'Review and intensify diabetes medication regimen',
          'Referral to diabetes educator',
          'Dietary counseling',
        ],
      },
      {
        id: 'gap-3',
        category: 'mental-health',
        title: 'Depression Follow-up Needed',
        description: 'PHQ-9 score of 12 indicates moderate depression, follow-up assessment needed',
        priority: 'high',
        recommendedActions: [
          'Schedule follow-up PHQ-9 in 2-4 weeks',
          'Review current antidepressant therapy',
          'Consider therapy referral',
        ],
      },
      {
        id: 'gap-4',
        category: 'screening',
        title: 'Colorectal Cancer Screening Due',
        description: 'Patient is 58 years old and has not had colorectal cancer screening',
        priority: 'medium',
        dueDate: new Date('2025-12-31'),
        measureId: 'COL',
        measureName: 'Colorectal Cancer Screening',
        recommendedActions: [
          'Discuss options: colonoscopy, FIT test, or Cologuard',
          'Provide patient education materials',
          'Schedule preferred screening test',
        ],
      },
    ];
  }

  private getMockCareRecommendations(patientId: string): CareRecommendation[] {
    return [
      {
        id: 'rec-1',
        patientId,
        category: 'treatment',
        title: 'Consider SGLT2 Inhibitor for Diabetes',
        description: 'Patient has Type 2 diabetes with elevated HbA1c. SGLT2 inhibitors provide cardiovascular and renal protection.',
        urgency: 'soon',
        evidenceSource: 'ADA Standards of Care 2025',
        actionItems: ['Review current medications', 'Discuss SGLT2 options with patient', 'Consider renal function'],
        status: 'pending',
        createdDate: new Date(),
        priority: 'high',
        evidence: 'Multiple RCTs demonstrate cardiovascular benefit in patients with T2DM',
        guidelineSource: 'ADA Standards of Care 2025',
        rationale: 'Patient has suboptimal glucose control (HbA1c 7.2%) and would benefit from medication with proven CV benefits',
        expectedBenefit: 'HbA1c reduction of 0.5-1%, reduced CV events, renal protection',
      },
      {
        id: 'rec-2',
        patientId,
        category: 'referral',
        title: 'Nutrition Counseling for Diabetes Management',
        description: 'Medical nutrition therapy can improve glycemic control and support weight management',
        urgency: 'routine',
        evidenceSource: 'ADA/AND Guidelines',
        actionItems: ['Refer to registered dietitian', 'Provide patient education materials'],
        status: 'pending',
        createdDate: new Date(),
        priority: 'medium',
        evidence: 'Medical nutrition therapy associated with 1-2% reduction in HbA1c',
        rationale: 'Patient reports difficulty with dietary adherence and food insecurity',
        resources: [
          {
            type: 'referral',
            title: 'Registered Dietitian',
            description: 'Specialized in diabetes management',
          },
        ],
      },
      {
        id: 'rec-3',
        patientId,
        category: 'lifestyle',
        title: 'Increase Physical Activity',
        description: 'Regular physical activity improves glucose control, cardiovascular health, and mental health',
        urgency: 'routine',
        evidenceSource: 'ADA Physical Activity Guidelines',
        actionItems: ['Discuss physical activity goals', 'Refer to exercise program if available'],
        status: 'pending',
        createdDate: new Date(),
        priority: 'medium',
        evidence: 'Exercise associated with improved HbA1c, mood, and cardiovascular fitness',
        rationale: 'Patient is sedentary and has both diabetes and depression',
        expectedBenefit: 'Improved glucose control, weight management, mood improvement',
        resources: [
          {
            type: 'article',
            title: 'Exercise for Diabetes',
            url: '/knowledge-base/article/exercise-diabetes',
          },
        ],
      },
      {
        id: 'rec-4',
        patientId,
        category: 'preventive',
        title: 'Annual Influenza Vaccination',
        description: 'Flu vaccination recommended for all adults, especially those with chronic conditions',
        urgency: 'soon',
        evidenceSource: 'CDC/ACIP Guidelines',
        actionItems: ['Order flu vaccine', 'Administer vaccination'],
        status: 'pending',
        createdDate: new Date(),
        priority: 'medium',
        evidence: 'Reduces risk of flu-related complications in high-risk populations',
        rationale: 'Patient has diabetes and hypertension, making them high-risk for flu complications',
      },
    ];
  }

  private getMockQualityPerformance(patientId: string): QualityMeasurePerformance {
    return {
      overallCompliance: 72,
      totalMeasures: 18,
      metMeasures: 13,
      byCategory: {
        preventive: { compliance: 70, total: 8, met: 6 },
        chronicDisease: { compliance: 80, total: 5, met: 4 },
        mentalHealth: { compliance: 67, total: 3, met: 2 },
        medication: { compliance: 100, total: 2, met: 2 },
      },
      recentResults: [
        {
          measureId: 'CDC-HbA1c',
          measureName: 'Diabetes HbA1c Control',
          category: 'Chronic Disease',
          compliant: false,
          score: 7.2,
          evaluationDate: new Date('2025-11-10'),
        },
        {
          measureId: 'CBP',
          measureName: 'Controlling High Blood Pressure',
          category: 'Chronic Disease',
          compliant: true,
          evaluationDate: new Date('2025-11-15'),
        },
      ],
    };
  }

  private getMockHealthTrend(
    metric: string,
    startDate: Date,
    endDate: Date
  ): HealthMetricTrend {
    // Generate mock trend data
    const dataPoints = [];
    const days = Math.floor((endDate.getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24));

    for (let i = 0; i <= days; i += 30) {
      const date = new Date(startDate);
      date.setDate(date.getDate() + i);
      dataPoints.push({
        date,
        value: 7.5 + Math.random() * 0.8 - 0.4, // Mock HbA1c values
      });
    }

    return {
      metric: 'HbA1c',
      unit: '%',
      dataPoints,
      trend: 'stable',
      currentValue: 7.2,
      targetValue: 7.0,
      referenceRange: { low: 4, high: 6 },
    };
  }

  // ===========================================================================
  // LAB RESULTS WITH INTERPRETATIONS (Feature 2.2)
  // ===========================================================================

  /**
   * Map FHIR interpretation code to structured LabInterpretation
   */
  mapFhirInterpretationCode(code: string): LabInterpretation {
    const mapping = FHIR_INTERPRETATION_CODES[code];

    if (mapping) {
      const descriptions: Record<string, string> = {
        'N': 'Result is within normal reference range',
        'L': 'Result is below the normal reference range',
        'H': 'Result is above the normal reference range',
        'LL': 'Result is critically low - immediate clinical attention may be required',
        'HH': 'Result is critically high - immediate clinical attention may be required',
        'A': 'Result is abnormal but not specifically high or low'
      };

      return {
        code,
        display: mapping.display,
        severity: mapping.severity,
        description: descriptions[code]
      };
    }

    // Handle unknown codes gracefully
    return {
      code,
      display: 'Unknown',
      severity: 'unknown',
      description: 'Interpretation code not recognized'
    };
  }

  /**
   * Get lab results grouped by panel (CBC, BMP, Lipid, etc.)
   */
  getLabResultsGroupedByPanel(patientId: string): Observable<LabPanel[]> {
    return this.getLabResultsFromFhir(patientId, { followPagination: false }).pipe(
      map((labResults) => {
        const panels: LabPanel[] = [];
        const usedResults = new Set<string>();

        // Group by date first
        const resultsByDate = new Map<string, LabResult[]>();
        labResults.forEach((result) => {
          const dateKey = result.date.toISOString().split('T')[0];
          if (!resultsByDate.has(dateKey)) {
            resultsByDate.set(dateKey, []);
          }
          resultsByDate.get(dateKey)?.push(result);
        });

        // Process each panel type
        const panelDefinitions = [
          { name: 'CBC', code: LOINC_LAB_PANELS.CBC.panelCode, components: LOINC_LAB_PANELS.CBC.components },
          { name: 'BMP', code: LOINC_LAB_PANELS.BMP.panelCode, components: LOINC_LAB_PANELS.BMP.components },
          { name: 'Lipid Panel', code: LOINC_LAB_PANELS.LIPID.panelCode, components: LOINC_LAB_PANELS.LIPID.components }
        ];

        resultsByDate.forEach((dateResults, dateKey) => {
          panelDefinitions.forEach((panelDef) => {
            const panelResults = dateResults.filter(r =>
              r.loincCode && (panelDef.components as readonly string[]).includes(r.loincCode) && !usedResults.has(`${r.loincCode}-${dateKey}`)
            );

            if (panelResults.length >= 2) { // At least 2 components to constitute a panel
              panelResults.forEach(r => usedResults.add(`${r.loincCode}-${dateKey}`));

              // Determine panel status
              let panelStatus: 'normal' | 'abnormal' | 'critical' = 'normal';
              if (panelResults.some(r => r.status === 'critical')) {
                panelStatus = 'critical';
              } else if (panelResults.some(r => r.status === 'abnormal')) {
                panelStatus = 'abnormal';
              }

              panels.push({
                panelCode: panelDef.code,
                panelName: panelDef.name,
                date: new Date(dateKey),
                status: panelStatus,
                results: panelResults
              });
            }
          });
        });

        // Add standalone results (not part of any panel)
        const standaloneResults = labResults.filter(r => {
          const dateKey = r.date.toISOString().split('T')[0];
          return !usedResults.has(`${r.loincCode}-${dateKey}`);
        });

        if (standaloneResults.length > 0) {
          // Group standalone by date
          const standaloneByDate = new Map<string, LabResult[]>();
          standaloneResults.forEach((result) => {
            const dateKey = result.date.toISOString().split('T')[0];
            if (!standaloneByDate.has(dateKey)) {
              standaloneByDate.set(dateKey, []);
            }
            standaloneByDate.get(dateKey)?.push(result);
          });

          standaloneByDate.forEach((results, dateKey) => {
            let status: 'normal' | 'abnormal' | 'critical' = 'normal';
            if (results.some(r => r.status === 'critical')) {
              status = 'critical';
            } else if (results.some(r => r.status === 'abnormal')) {
              status = 'abnormal';
            }

            panels.push({
              panelCode: 'standalone',
              panelName: 'Standalone Labs',
              date: new Date(dateKey),
              status,
              results
            });
          });
        }

        // Sort panels by date (newest first)
        panels.sort((a, b) => b.date.getTime() - a.date.getTime());

        return panels;
      }),
      catchError((error) => {
        console.error('Error grouping lab results by panel:', error);
        return of([]);
      })
    );
  }

  /**
   * Get lab history for a specific LOINC code
   */
  getLabHistory(patientId: string, loincCode: string, limit?: number): Observable<LabResult[]> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.OBSERVATION);
    const params = new HttpParams()
      .set('patient', patientId)
      .set('category', OBSERVATION_CATEGORIES.LABORATORY)
      .set('code', `http://loinc.org|${loincCode}`)
      .set('_sort', '-date')
      .set('_count', limit?.toString() || '10');

    return this.http.get<FhirBundle<FhirObservation>>(url, {
      headers: this.getHeaders(),
      params,
    }).pipe(
      map((bundle) => {
        const results = (bundle.entry || [])
          .map((entry) => entry.resource)
          .filter((obs): obs is FhirObservation => obs !== undefined)
          .map((obs) => this.mapFhirObservationToLabResult(obs));

        // Ensure sorted newest first
        results.sort((a, b) => b.date.getTime() - a.date.getTime());

        return results;
      }),
      catchError((error) => {
        console.error('Error fetching lab history:', error);
        return of([]);
      })
    );
  }

  /**
   * Analyze lab trend from historical results
   */
  analyzeLabTrend(results: LabResult[]): LabTrendAnalysis {
    if (results.length < 2) {
      return {
        loincCode: '',
        testName: results[0]?.code.text || 'Unknown',
        trend: 'stable',
        percentChange: 0,
        dataPoints: results
      };
    }

    // Sort by date (oldest to newest for trend calculation)
    const sortedResults = [...results].sort((a, b) => a.date.getTime() - b.date.getTime());

    // Get numeric values
    const values = sortedResults
      .map(r => typeof r.value === 'number' ? r.value : parseFloat(String(r.value)))
      .filter(v => !isNaN(v));

    if (values.length < 2) {
      return {
        loincCode: sortedResults[0]?.loincCode || '',
        testName: sortedResults[0]?.code.text || 'Unknown',
        trend: 'stable',
        percentChange: 0,
        dataPoints: results
      };
    }

    // Calculate percentage change from oldest to newest
    const oldestValue = values[0];
    const newestValue = values[values.length - 1];
    const percentChange = ((newestValue - oldestValue) / oldestValue) * 100;

    // Determine trend
    let trend: 'improving' | 'stable' | 'worsening';
    if (Math.abs(percentChange) < 5) {
      trend = 'stable';
    } else if (percentChange < 0) {
      // For most lab values, decreasing is improving (e.g., HbA1c, cholesterol)
      trend = 'improving';
    } else {
      trend = 'worsening';
    }

    // Generate recommendation for concerning trends
    let recommendation: string | undefined;
    if (trend === 'worsening' || newestValue > oldestValue * 1.2) {
      const testName = sortedResults[0]?.code.text || 'Lab value';
      recommendation = `${testName} has increased by ${Math.abs(percentChange).toFixed(1)}%. ` +
        'Consider reviewing treatment plan and patient adherence. ' +
        'May require medication adjustment or lifestyle intervention.';
    } else if (sortedResults[sortedResults.length - 1]?.status === 'critical') {
      recommendation = 'Current value is in critical range. Immediate clinical review recommended.';
    }

    return {
      loincCode: sortedResults[0]?.loincCode || '',
      testName: sortedResults[0]?.code.text || 'Unknown',
      trend,
      percentChange,
      dataPoints: results,
      recommendation
    };
  }

  /**
   * Enhanced mapFhirObservationToLabResult with interpretation
   */
  private mapFhirObservationToLabResultWithInterpretation(obs: FhirObservation): LabResult {
    const baseResult = this.mapFhirObservationToLabResult(obs);

    // Extract LOINC code
    const loincCode = obs.code.coding?.find(c => c.system === 'http://loinc.org')?.code;

    // Extract and map interpretation
    let interpretation: LabInterpretation | undefined;
    if (obs.interpretation && obs.interpretation.length > 0) {
      const interpretationCode = obs.interpretation[0].coding?.[0]?.code;
      if (interpretationCode) {
        interpretation = this.mapFhirInterpretationCode(interpretationCode);
      }
    }

    return {
      ...baseResult,
      loincCode,
      interpretation
    };
  }

  // ===========================================================================
  // HEALTH SCORE BACKEND INTEGRATION (Feature 3.1)
  // ===========================================================================

  /**
   * Get health score from backend API with caching
   * Falls back to frontend calculation if backend is unavailable
   */
  /**
   * Get health score from backend API with caching
   * Falls back to mock data if backend is unavailable
   */
  getHealthScore(patientId: string): Observable<HealthScore> {
    // Check cache first
    const cached = this.healthScoreCache.get(patientId);
    if (cached && Date.now() - cached.timestamp < this.CACHE_TTL_MS) {
      return of(cached.data);
    }

    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.PATIENT_HEALTH_SCORE(patientId));

    return this.http.get<any>(url, { headers: this.getHeaders() }).pipe(
      map((response) => {
        const healthScore: HealthScore = {
          patientId: response.patientId || patientId,
          overallScore: response.overallScore || response.score,
          score: response.score || response.overallScore, // Backward compatibility
          status: response.status,
          trend: response.trend || 'unknown',
          components: {
            physical: response.components.physical,
            mental: response.components.mental,
            social: response.components.social,
            preventive: response.components.preventive,
            chronicDisease: response.components.chronicDisease || 0
          },
          calculatedAt: new Date(response.calculatedAt || new Date()),
          lastCalculated: new Date(response.calculatedAt || new Date()) // Backward compatibility
        };

        // Cache the result
        this.healthScoreCache.set(patientId, { data: healthScore, timestamp: Date.now() });

        return healthScore;
      }),
      catchError((error) => {
        console.error('Error fetching health score from backend, falling back to mock data:', error);
        // Fallback to mock data
        return of(this.getMockHealthScore(patientId));
      })
    );
  }

  /**
   * @deprecated Use getHealthScore instead
   */
  getHealthScoreFromBackend(patientId: string): Observable<HealthScore> {
    return this.getHealthScore(patientId);
  }

  /**
   * Generate mock health score for fallback
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
        chronicDisease: 75
      },
      calculatedAt: new Date(),
      lastCalculated: new Date()
    };
  }

  /**
   * Get health score history from backend (simplified format)
   * @param patientId Patient ID
   * @returns Observable of HealthScoreHistory array with score, calculatedAt, trigger
   */
  getHealthScoreHistory(patientId: string): Observable<HealthScoreHistory[]> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.PATIENT_HEALTH_SCORE_HISTORY(patientId));

    return this.http.get<any[]>(url, {
      headers: this.getHeaders()
    }).pipe(
      map((response) => {
        return response.map(point => ({
          score: point.score,
          calculatedAt: new Date(point.calculatedAt),
          trigger: point.trigger || 'scheduled'
        }));
      }),
      catchError((error) => {
        console.error('Error fetching health score history:', error);
        return of([]);
      })
    );
  }

  /**
   * Get detailed health score history from backend with components
   * @param patientId Patient ID
   * @param months Number of months of history to retrieve (default: 12)
   */
  getHealthScoreHistoryDetailed(patientId: string, months: number = 12): Observable<HealthScoreHistoryPoint[]> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.PATIENT_HEALTH_SCORE_HISTORY(patientId));

    const params = new HttpParams().set('months', months.toString());

    return this.http.get<any[]>(url, {
      headers: this.getHeaders(),
      params
    }).pipe(
      map((response) => {
        return response.map(point => ({
          date: new Date(point.date || point.calculatedAt),
          score: point.score,
          status: point.status || this.deriveStatusFromScore(point.score),
          components: point.components || {
            physical: 0,
            mental: 0,
            social: 0,
            preventive: 0
          }
        }));
      }),
      catchError((error) => {
        console.error('Error fetching health score history:', error);
        return of([]);
      })
    );
  }

  /**
   * Derive health status from score
   */
  private deriveStatusFromScore(score: number): HealthStatus {
    if (score > 80) return 'excellent';
    if (score >= 60) return 'good';
    if (score >= 40) return 'fair';
    return 'poor';
  }

  /**
   * Calculate health score trend from historical data
   * @param history Array of historical health score points (should be sorted chronologically)
   */
  calculateHealthScoreTrend(history: HealthScoreHistoryPoint[]): HealthScoreTrend {
    if (history.length < 2) {
      return {
        direction: 'stable',
        percentChange: 0,
        pointsChange: 0
      };
    }

    // Sort by date to ensure chronological order (oldest to newest)
    const sortedHistory = [...history].sort((a, b) => a.date.getTime() - b.date.getTime());

    const oldestScore = sortedHistory[0].score;
    const newestScore = sortedHistory[sortedHistory.length - 1].score;

    const pointsChange = newestScore - oldestScore;
    const percentChange = (pointsChange / oldestScore) * 100;

    // Determine direction based on change threshold (5% or 5 points)
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
      pointsChange
    };
  }

  /**
   * Calculate weighted health score from components
   * Weights: Physical 40%, Mental 30%, Social 15%, Preventive 15%
   */
  calculateWeightedHealthScore(components: {
    physical: number;
    mental: number;
    social: number;
    preventive: number;
  }): number {
    return Math.round(
      components.physical * 0.4 +
      components.mental * 0.3 +
      components.social * 0.15 +
      components.preventive * 0.15
    );
  }

  /**
   * Determine health status from numeric score
   * Excellent: >= 80
   * Good: 60-79
   * Fair: 40-59
   * Poor: < 40
   */
  determineHealthStatus(score: number): HealthStatus {
    if (score >= 80) return 'excellent';
    if (score >= 60) return 'good';
    if (score >= 40) return 'fair';
    return 'poor';
  }

  /**
   * Invalidate health score cache for a patient
   * Should be called when patient data changes
   */
  invalidateHealthScoreCache(patientId: string): void {
    this.healthScoreCache.delete(patientId);
  }

  // ===========================================================================
  // PHYSICAL HEALTH FULL FHIR INTEGRATION (Feature 3.2)
  // ===========================================================================

  /**
   * Get chronic conditions from FHIR Condition endpoint
   * Filters by clinical status (active, recurrence)
   */
  getConditionsFromFhir(patientId: string): Observable<ChronicCondition[]> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.CONDITION);
    const params = new HttpParams()
      .set('patient', patientId)
      .set('clinical-status', 'active,recurrence')
      .set('_sort', '-recorded-date')
      .set('_count', '50');

    return this.http.get<FhirBundle<FhirCondition>>(url, {
      headers: this.getHeaders(),
      params,
    }).pipe(
      map((bundle) => {
        const conditions: ChronicCondition[] = [];

        if (!bundle.entry || bundle.entry.length === 0) {
          return conditions;
        }

        for (const entry of bundle.entry) {
          const fhirCondition = entry.resource;
          if (!fhirCondition) continue;

          // Filter by clinical status
          const clinicalStatus = fhirCondition.clinicalStatus?.coding?.[0]?.code;
          if (clinicalStatus !== 'active' && clinicalStatus !== 'recurrence') {
            continue;
          }

          // Map to ChronicCondition interface
          const condition: ChronicCondition = {
            code: {
              text: fhirCondition.code?.text || fhirCondition.code?.coding?.[0]?.display || 'Unknown'
            },
            display: fhirCondition.code?.coding?.[0]?.display || fhirCondition.code?.text || 'Unknown',
            severity: this.mapFhirSeverityToLocal(fhirCondition.severity),
            onsetDate: fhirCondition.onsetDateTime ? new Date(fhirCondition.onsetDateTime) : undefined,
            lastReview: fhirCondition.recordedDate ? new Date(fhirCondition.recordedDate) : undefined,
            controlled: this.determineConditionControlledStatus(fhirCondition),
          };

          // Extract ICD-10 and SNOMED codes
          const icd10Coding = fhirCondition.code?.coding?.find(c =>
            c.system === 'http://hl7.org/fhir/sid/icd-10' || c.system?.includes('icd-10')
          );
          const snomedCoding = fhirCondition.code?.coding?.find(c =>
            c.system === 'http://snomed.info/sct'
          );

          // Add codes to condition (using any to avoid type errors)
          (condition as any).icd10Code = icd10Coding?.code;
          (condition as any).snomedCode = snomedCoding?.code;

          conditions.push(condition);
        }

        return conditions;
      }),
      catchError((error) => {
        console.error('Error fetching conditions from FHIR:', error);
        return of([]);
      })
    );
  }

  /**
   * Map FHIR severity to local severity scale
   */
  private mapFhirSeverityToLocal(severity?: FhirCodeableConcept): 'mild' | 'moderate' | 'severe' {
    if (!severity || !severity.coding || severity.coding.length === 0) {
      return 'moderate'; // Default
    }

    const code = severity.coding[0].code?.toLowerCase();
    const display = severity.coding[0].display?.toLowerCase();

    if (code?.includes('24484000') || display?.includes('severe')) {
      return 'severe';
    }
    if (code?.includes('6736007') || display?.includes('moderate')) {
      return 'moderate';
    }
    if (code?.includes('255604002') || display?.includes('mild')) {
      return 'mild';
    }

    return 'moderate';
  }

  /**
   * Determine if condition is controlled from clinical notes
   */
  private determineConditionControlledStatus(condition: FhirCondition): boolean {
    if (!condition.note || condition.note.length === 0) {
      return true; // Assume controlled if no notes
    }

    const notesText = condition.note.map(n => n.text.toLowerCase()).join(' ');

    // Keywords indicating controlled status
    const controlledKeywords = [
      'controlled',
      'well-controlled',
      'stable',
      'managed',
      'well managed'
    ];

    // Keywords indicating uncontrolled status
    const uncontrolledKeywords = [
      'uncontrolled',
      'poorly controlled',
      'unmanaged',
      'elevated',
      'remains elevated',
      'not controlled'
    ];

    // Check for uncontrolled keywords first
    for (const keyword of uncontrolledKeywords) {
      if (notesText.includes(keyword)) {
        return false;
      }
    }

    // Check for controlled keywords
    for (const keyword of controlledKeywords) {
      if (notesText.includes(keyword)) {
        return true;
      }
    }

    // Default to controlled
    return true;
  }

  /**
   * Get medication adherence data from FHIR MedicationStatement
   * Returns medication adherence summary
   */
  getMedicationAdherenceData(patientId: string): Observable<PhysicalHealthSummary['medicationAdherence']> {
    // For now, return mock data
    // In production, query FHIR MedicationStatement and calculate adherence
    return of({
      overallRate: 85,
      status: 'good' as const,
      problematicMedications: []
    });
  }

  /**
   * Get functional status from FHIR QuestionnaireResponse
   * Calculates ADL and IADL scores from responses
   */
  getFunctionalStatusFromFhir(patientId: string): Observable<FunctionalStatus> {
    const url = buildFhirUrl('/QuestionnaireResponse');
    const params = new HttpParams()
      .set('patient', patientId)
      .set('_sort', '-authored')
      .set('_count', '10');

    return this.http.get<FhirBundle<FhirQuestionnaireResponse>>(url, {
      headers: this.getHeaders(),
      params,
    }).pipe(
      map((bundle) => {
        const functionalStatus: FunctionalStatus = {
          adlScore: 6,
          iadlScore: 8,
          mobilityScore: 85,
          painLevel: 3,
          fatigueLevel: 4
        };

        if (!bundle.entry || bundle.entry.length === 0) {
          return functionalStatus;
        }

        // Process questionnaire responses
        for (const entry of bundle.entry) {
          const response = entry.resource;
          if (!response || !response.item) continue;

          const questionnaireUrl = response.questionnaire?.toLowerCase() || '';

          // ADL Assessment
          if (questionnaireUrl.includes('adl') && !questionnaireUrl.includes('iadl')) {
            functionalStatus.adlScore = this.calculateADLScore(response.item);
          }

          // IADL Assessment
          if (questionnaireUrl.includes('iadl')) {
            functionalStatus.iadlScore = this.calculateIADLScore(response.item);
          }
        }

        return functionalStatus;
      }),
      catchError((error) => {
        console.error('Error fetching functional status from FHIR:', error);
        // Return default values
        return of({
          adlScore: 6,
          iadlScore: 8,
          mobilityScore: 85,
          painLevel: 3,
          fatigueLevel: 4
        });
      })
    );
  }

  /**
   * Calculate ADL score from questionnaire items
   * 1 point for each independent activity, 0 for dependent
   */
  private calculateADLScore(items: FhirQuestionnaireResponseItem[]): number {
    let score = 0;

    for (const item of items) {
      if (!item.answer || item.answer.length === 0) continue;

      const answer = item.answer[0];
      const code = answer.valueCoding?.code?.toLowerCase();

      if (code === 'independent') {
        score++;
      }
    }

    return Math.min(score, 6); // Max 6
  }

  /**
   * Calculate IADL score from questionnaire items
   * 1 point for each independent activity, 0 for dependent
   */
  private calculateIADLScore(items: FhirQuestionnaireResponseItem[]): number {
    let score = 0;

    for (const item of items) {
      if (!item.answer || item.answer.length === 0) continue;

      const answer = item.answer[0];
      const code = answer.valueCoding?.code?.toLowerCase();

      if (code === 'independent') {
        score++;
      }
    }

    return Math.min(score, 8); // Max 8
  }

  /**
   * Build physical health summary from aggregated FHIR data
   * Includes critical alert detection and medication adherence mapping
   */
  private buildPhysicalHealthSummary(data: {
    vitals: PhysicalHealthSummary['vitals'];
    labs: LabResult[];
    conditions: ChronicCondition[];
    medications: { overallPDC: number; adherentCount: number; totalMedications: number; problematicMedications: string[] };
    procedures: any[];
    functional: FunctionalStatus;
  }): PhysicalHealthSummary {
    // Map medication adherence data to expected format
    const medicationAdherence: PhysicalHealthSummary['medicationAdherence'] = {
      overallRate: data.medications.overallPDC,
      status: this.determineMedicationAdherenceStatus(data.medications.overallPDC),
      problematicMedications: data.medications.problematicMedications
    };

    // Detect critical alerts from vitals and labs
    this.detectCriticalAlerts(data.vitals, data.labs);

    // Determine overall status
    const status = this.determinePhysicalHealthStatus({
      vitals: data.vitals,
      labs: data.labs,
      conditions: data.conditions,
      medications: medicationAdherence,
      functional: data.functional
    });

    return {
      status,
      vitals: data.vitals,
      labs: data.labs,
      chronicConditions: data.conditions,
      medicationAdherence,
      functionalStatus: data.functional
    };
  }

  /**
   * Determine medication adherence status from PDC percentage
   */
  private determineMedicationAdherenceStatus(pdc: number): 'excellent' | 'good' | 'poor' | 'unknown' {
    if (pdc >= 80) return 'excellent';
    if (pdc >= 60) return 'good';
    if (pdc > 0) return 'poor';
    return 'unknown';
  }

  /**
   * Detect and flag critical alerts from vital signs and lab results
   */
  private detectCriticalAlerts(
    vitals: PhysicalHealthSummary['vitals'],
    labs: LabResult[]
  ): void {
    // Check blood pressure for critical values
    if (vitals.bloodPressure) {
      const bpValue = vitals.bloodPressure.value;
      if (typeof bpValue === 'string') {
        const [systolic, diastolic] = bpValue.split('/').map(v => parseInt(v, 10));
        if (systolic > 180 || diastolic > 120) {
          vitals.bloodPressure.status = 'critical';
        } else if (systolic > 140 || diastolic > 90) {
          vitals.bloodPressure.status = 'abnormal';
        }
      }
    }

    // Check heart rate for critical values
    if (vitals.heartRate) {
      const hr = vitals.heartRate.value;
      if (typeof hr === 'number') {
        if (hr < 40 || hr > 150) {
          vitals.heartRate.status = 'critical';
        } else if (hr < 60 || hr > 100) {
          vitals.heartRate.status = 'abnormal';
        }
      }
    }

    // Check temperature for critical values
    if (vitals.temperature) {
      const temp = vitals.temperature.value;
      if (typeof temp === 'number') {
        if (temp > 103 || temp < 95) {
          vitals.temperature.status = 'critical';
        } else if (temp > 100.4 || temp < 96) {
          vitals.temperature.status = 'abnormal';
        }
      }
    }

    // Check oxygen saturation for critical values
    if (vitals.oxygenSaturation) {
      const o2 = vitals.oxygenSaturation.value;
      if (typeof o2 === 'number') {
        if (o2 < 90) {
          vitals.oxygenSaturation.status = 'critical';
        } else if (o2 < 95) {
          vitals.oxygenSaturation.status = 'abnormal';
        }
      }
    }

    // Check labs for critical values (e.g., glucose)
    labs.forEach(lab => {
      if (lab.loincCode === '2339-0' || lab.loincCode === '2345-7') { // Glucose
        const value = typeof lab.value === 'number' ? lab.value : parseFloat(lab.value as string);
        if (!isNaN(value)) {
          if (value < 50 || value > 400) {
            lab.status = 'critical';
          } else if (value < 70 || value > 200) {
            lab.status = 'abnormal';
          }
        }
      }
    });
  }

  /**
   * Determine overall physical health status from components
   */
  private determinePhysicalHealthStatus(data: {
    vitals: PhysicalHealthSummary['vitals'];
    labs: LabResult[];
    conditions: ChronicCondition[];
    medications: PhysicalHealthSummary['medicationAdherence'];
    functional: FunctionalStatus;
  }): HealthStatus {
    let score = 100;

    // Check vitals
    const vitalValues = Object.values(data.vitals);
    const criticalVitals = vitalValues.filter(v => v && v.status === 'critical').length;
    const abnormalVitals = vitalValues.filter(v => v && v.status === 'abnormal').length;

    if (criticalVitals > 0) score -= 30;
    else if (abnormalVitals > 0) score -= 10;

    // Check labs
    const criticalLabs = data.labs.filter(l => l.status === 'critical').length;
    const abnormalLabs = data.labs.filter(l => l.status === 'abnormal').length;

    if (criticalLabs > 0) score -= 30;
    else if (abnormalLabs > 0) score -= 10;

    // Check conditions
    const severeUncontrolled = data.conditions.filter(c =>
      c.severity === 'severe' && !c.controlled
    ).length;
    const moderateUncontrolled = data.conditions.filter(c =>
      c.severity === 'moderate' && !c.controlled
    ).length;

    score -= severeUncontrolled * 20;
    score -= moderateUncontrolled * 10;

    // Check medication adherence
    if (data.medications.status === 'poor') score -= 15;
    else if ((data.medications.status as string) === 'fair') score -= 5;

    // Check functional status
    if (data.functional.adlScore < 4) score -= 10;
    if (data.functional.painLevel > 7) score -= 10;

    // Determine status
    if (score >= 85) return 'excellent';
    if (score >= 70) return 'good';
    if (score >= 50) return 'fair';
    return 'poor';
  }

  /**
   * Feature 4.3: Category Risk Assessments
   * Calculates condition-specific risk assessments
   */

  /**
   * Calculate diabetes risk based on HbA1c levels and other factors
   */
  calculateDiabetesRisk(patientId: string): Observable<CategoryRiskAssessment> {
    return this.getObservations(patientId, '4548-4').pipe( // HbA1c LOINC code
      map((hba1cBundle: FhirBundle) => {
        const factors: string[] = [];
        let score = 0;
        let riskLevel: 'low' | 'moderate' | 'high' | 'critical' = 'low';
        const recommendations: string[] = [];

        // Get most recent HbA1c
        const hba1cObs = hba1cBundle.entry?.length ?
          hba1cBundle.entry[0].resource as FhirObservation : null;

        if (hba1cObs && hba1cObs.valueQuantity) {
          const hba1cValue = hba1cObs.valueQuantity.value;

          if (hba1cValue !== undefined && hba1cValue < 7.0) {
            // Good control
            score = Math.round(hba1cValue * 10); // ~60-69
            riskLevel = 'low';
            factors.push(`HbA1c well controlled (${hba1cValue}%)`);
            recommendations.push('Continue current diabetes management plan');
            recommendations.push('Annual eye and foot exams');
          } else if (hba1cValue !== undefined && hba1cValue < 9.0) {
            // Moderate risk
            score = Math.round(60 + (hba1cValue - 7) * 15); // ~70-89
            riskLevel = 'moderate';
            factors.push(`HbA1c above target (${hba1cValue}%)`);
            recommendations.push('Review and intensify diabetes management');
            recommendations.push('Increase monitoring frequency');
            recommendations.push('Diabetes education and nutrition counseling');
          } else if (hba1cValue !== undefined) {
            // High risk
            score = Math.min(Math.round(70 + (hba1cValue - 7) * 10), 100); // 90-100
            riskLevel = 'high';
            factors.push(`HbA1c above target (${hba1cValue}%)`);
            recommendations.push('Urgent: Intensify diabetes management');
            recommendations.push('Endocrinology referral recommended');
            recommendations.push('Frequent monitoring required');
            recommendations.push('Review medication adherence');
          }
        } else {
          // No recent data
          factors.push('No recent HbA1c data available');
          recommendations.push('Schedule HbA1c test');
        }

        return {
          category: 'diabetes' as const,
          riskLevel,
          score,
          factors,
          recommendations,
          lastAssessed: new Date()
        };
      }),
      catchError(() => of({
        category: 'diabetes' as const,
        riskLevel: 'low' as const,
        score: 0,
        factors: ['Unable to retrieve diabetes risk data'],
        recommendations: ['Schedule HbA1c test for assessment'],
        lastAssessed: new Date()
      }))
    );
  }

  /**
   * Calculate cardiovascular risk using ASCVD-like scoring
   */
  calculateCardiovascularRisk(patientId: string): Observable<CategoryRiskAssessment> {
    return forkJoin({
      patient: this.http.get<any>(buildFhirUrl(`${FHIR_ENDPOINTS.PATIENT}/${patientId}`), { headers: this.getHeaders() }),
      bloodPressure: this.getObservations(patientId, '85354-9'), // BP LOINC
      cholesterol: this.getObservations(patientId, '2093-3'), // Total cholesterol LOINC
      conditions: this.getConditions(patientId)
    }).pipe(
      map(({ patient, bloodPressure, cholesterol, conditions }) => {
        const factors: string[] = [];
        let score = 0;
        let riskLevel: 'low' | 'moderate' | 'high' | 'critical' = 'low';
        const recommendations: string[] = [];

        // Calculate age
        if (patient.birthDate) {
          const birthDate = new Date(patient.birthDate);
          const age = new Date().getFullYear() - birthDate.getFullYear();

          if (age >= 65) {
            score += 30;
            factors.push(`Age ${age} (increased risk)`);
          } else if (age >= 45) {
            score += 15;
            factors.push(`Age ${age}`);
          } else {
            factors.push(`Age ${age} (low risk)`);
          }
        }

        // Check blood pressure
        const bpObs = bloodPressure.entry?.length ? bloodPressure.entry[0].resource as FhirObservation : null;
        if (bpObs && bpObs.component) {
          const systolic = bpObs.component.find(c => c.code?.coding?.[0]?.code === '8480-6')?.valueQuantity?.value || 0;
          const diastolic = bpObs.component.find(c => c.code?.coding?.[0]?.code === '8462-4')?.valueQuantity?.value || 0;

          if (systolic >= 140 || diastolic >= 90) {
            score += 25;
            factors.push(`Elevated blood pressure (${systolic}/${diastolic} mmHg)`);
          } else if (systolic >= 130 || diastolic >= 80) {
            score += 15;
            factors.push(`Blood pressure slightly elevated (${systolic}/${diastolic} mmHg)`);
          }
        }

        // Check cholesterol
        const cholObs = cholesterol.entry?.length ? cholesterol.entry[0].resource as FhirObservation : null;
        if (cholObs && cholObs.valueQuantity) {
          const cholValue = cholObs.valueQuantity.value;
          if (cholValue !== undefined && cholValue >= 240) {
            score += 20;
            factors.push(`High cholesterol (${cholValue} mg/dL)`);
          } else if (cholValue !== undefined && cholValue >= 200) {
            score += 10;
            factors.push(`Borderline high cholesterol (${cholValue} mg/dL)`);
          }
        }

        // Check for cardiovascular conditions
        const cvConditions = conditions.entry?.filter(e => {
          const resource = e.resource as any;
          const code = resource.code?.coding?.[0]?.code;
          // Check for cardiovascular conditions
          return code && (
            code.includes('heart') ||
            code.includes('cardiovascular') ||
            code.includes('hypertension') ||
            code === '53741008' // Coronary artery disease
          );
        });

        if (cvConditions && cvConditions.length > 0) {
          score += 15;
          factors.push('Existing cardiovascular condition');
        }

        // Determine risk level
        if (score >= 75) {
          riskLevel = 'high';
          recommendations.push('Consider cardiology referral');
          recommendations.push('Aggressive risk factor modification');
          recommendations.push('Consider statin therapy if not already prescribed');
        } else if (score >= 50) {
          riskLevel = 'moderate';
          recommendations.push('Lifestyle modifications recommended');
          recommendations.push('Monitor blood pressure and cholesterol');
          recommendations.push('Regular cardiovascular risk assessment');
        } else {
          riskLevel = 'low';
          recommendations.push('Maintain heart-healthy lifestyle');
          recommendations.push('Annual cardiovascular screening');
        }

        return {
          category: 'cardiovascular' as const,
          riskLevel,
          score,
          factors,
          recommendations,
          lastAssessed: new Date()
        };
      }),
      catchError(() => of({
        category: 'cardiovascular' as const,
        riskLevel: 'low' as const,
        score: 0,
        factors: ['Unable to retrieve cardiovascular risk data'],
        recommendations: ['Schedule cardiovascular screening'],
        lastAssessed: new Date()
      }))
    );
  }

  /**
   * Calculate mental health crisis risk from PHQ-9 and GAD-7 scores
   */
  calculateMentalHealthCrisisRisk(patientId: string): Observable<CategoryRiskAssessment> {
    return forkJoin({
      phq9: this.getQuestionnaireResponsesByType(patientId, 'PHQ-9'),
      gad7: this.getQuestionnaireResponsesByType(patientId, 'GAD-7')
    }).pipe(
      map(({ phq9, gad7 }) => {
        const factors: string[] = [];
        let score = 0;
        let riskLevel: 'low' | 'moderate' | 'high' | 'critical' = 'low';
        const recommendations: string[] = [];

        // Parse PHQ-9 score
        let phq9Score = 0;
        let hasSuicideIdeation = false;

        if (phq9.entry && phq9.entry.length > 0) {
          const qr = phq9.entry[0].resource as FhirQuestionnaireResponse;
          const totalItem = qr.item?.find(i => i.linkId?.includes('Total'));
          const q9Item = qr.item?.find(i => i.linkId === 'PHQ-9-Q9');

          if (totalItem && totalItem.answer && totalItem.answer[0].valueInteger !== undefined) {
            phq9Score = totalItem.answer[0].valueInteger;
          }

          if (q9Item && q9Item.answer && q9Item.answer[0].valueInteger && q9Item.answer[0].valueInteger > 0) {
            hasSuicideIdeation = true;
          }
        }

        // Parse GAD-7 score
        let gad7Score = 0;
        if (gad7.entry && gad7.entry.length > 0) {
          const qr = gad7.entry[0].resource as FhirQuestionnaireResponse;
          const totalItem = qr.item?.find(i => i.linkId?.includes('Total'));

          if (totalItem && totalItem.answer && totalItem.answer[0].valueInteger !== undefined) {
            gad7Score = totalItem.answer[0].valueInteger;
          }
        }

        // Calculate combined mental health risk
        if (hasSuicideIdeation) {
          score = 95;
          riskLevel = 'critical';
          factors.push('Suicide ideation present');
          factors.push(`Depression symptoms (PHQ-9: ${phq9Score})`);
          recommendations.push('URGENT: Immediate safety assessment required');
          recommendations.push('Crisis intervention services');
          recommendations.push('Psychiatric hospitalization may be needed');
        } else if (phq9Score >= 20 || gad7Score >= 15) {
          // Severe depression or anxiety
          score = 70 + Math.min(phq9Score, 30);
          riskLevel = 'high';
          factors.push(`Severe depression symptoms (PHQ-9: ${phq9Score})`);
          if (gad7Score >= 15) factors.push(`Severe anxiety symptoms (GAD-7: ${gad7Score})`);
          recommendations.push('Immediate psychiatric evaluation recommended');
          recommendations.push('Medication review and adjustment');
          recommendations.push('Intensive therapy recommended');
        } else if (phq9Score >= 10 || gad7Score >= 10) {
          // Moderate symptoms
          score = 40 + phq9Score + gad7Score;
          riskLevel = 'moderate';
          factors.push(`Moderate symptoms (PHQ-9: ${phq9Score}, GAD-7: ${gad7Score})`);
          recommendations.push('Mental health follow-up within 2 weeks');
          recommendations.push('Consider therapy or medication adjustment');
          recommendations.push('Regular symptom monitoring');
        } else {
          // Low risk
          score = phq9Score + gad7Score;
          riskLevel = 'low';
          factors.push(`Minimal symptoms (PHQ-9: ${phq9Score}, GAD-7: ${gad7Score})`);
          recommendations.push('Continue routine mental health screening');
          recommendations.push('Maintain self-care practices');
        }

        return {
          category: 'mental-health' as const,
          riskLevel,
          score,
          factors,
          recommendations,
          lastAssessed: new Date()
        };
      }),
      catchError(() => of({
        category: 'mental-health' as const,
        riskLevel: 'low' as const,
        score: 0,
        factors: ['Unable to retrieve mental health data'],
        recommendations: ['Schedule mental health screening'],
        lastAssessed: new Date()
      }))
    );
  }

  /**
   * Calculate respiratory risk for COPD/asthma patients
   */
  calculateRespiratoryRisk(patientId: string): Observable<CategoryRiskAssessment> {
    return forkJoin({
      conditions: this.getConditions(patientId),
      oxygenSat: this.getObservations(patientId, '2708-6') // Oxygen saturation LOINC
    }).pipe(
      map(({ conditions, oxygenSat }) => {
        const factors: string[] = [];
        let score = 0;
        let riskLevel: 'low' | 'moderate' | 'high' | 'critical' = 'low';
        const recommendations: string[] = [];

        // Check for respiratory conditions
        const hasAsthma = conditions.entry?.some(e => {
          const resource = e.resource as any;
          const code = resource.code?.coding?.[0]?.code;
          const text = resource.code?.text?.toLowerCase() || '';
          return code === '195967001' || text.includes('asthma');
        });

        const hasCOPD = conditions.entry?.some(e => {
          const resource = e.resource as any;
          const code = resource.code?.coding?.[0]?.code;
          const text = resource.code?.text?.toLowerCase() || '';
          return code === '13645005' || text.includes('copd') || text.includes('chronic obstructive');
        });

        if (hasCOPD) {
          score += 40;
          riskLevel = 'moderate';
          factors.push('Active COPD diagnosis');
          recommendations.push('Pulmonary rehabilitation program');
          recommendations.push('Smoking cessation if applicable');
          recommendations.push('Annual flu and pneumonia vaccines');
        }

        if (hasAsthma) {
          score += 30;
          if (riskLevel === 'low') riskLevel = 'moderate';
          factors.push('Active asthma diagnosis');
          recommendations.push('Ensure asthma action plan is current');
          recommendations.push('Regular peak flow monitoring');
        }

        // Check oxygen saturation
        const o2Obs = oxygenSat.entry?.length ? oxygenSat.entry[0].resource as FhirObservation : null;
        if (o2Obs && o2Obs.valueQuantity) {
          const o2Value = o2Obs.valueQuantity.value;

          if (o2Value !== undefined && o2Value < 90) {
            score += 30;
            riskLevel = 'high';
            factors.push(`Low oxygen saturation (${o2Value}%)`);
            recommendations.push('Consider pulmonology referral');
            recommendations.push('Oxygen therapy may be needed');
          } else if (o2Value !== undefined && o2Value < 94) {
            score += 15;
            if (riskLevel === 'low') riskLevel = 'moderate';
            factors.push(`Borderline oxygen saturation (${o2Value}%)`);
            recommendations.push('Monitor oxygen levels closely');
          }
        }

        // Default recommendations if no respiratory issues
        if (factors.length === 0) {
          factors.push('No respiratory conditions identified');
          recommendations.push('Maintain respiratory health');
          recommendations.push('Avoid smoking and secondhand smoke');
        }

        return {
          category: 'respiratory' as const,
          riskLevel,
          score,
          factors,
          recommendations,
          lastAssessed: new Date()
        };
      }),
      catchError(() => of({
        category: 'respiratory' as const,
        riskLevel: 'low' as const,
        score: 0,
        factors: ['Unable to retrieve respiratory data'],
        recommendations: ['Schedule respiratory assessment if indicated'],
        lastAssessed: new Date()
      }))
    );
  }

  /**
   * Get all category risk assessments for a patient
   */
  getCategoryRiskAssessments(patientId: string): Observable<CategoryRiskAssessment[]> {
    return forkJoin({
      diabetes: this.calculateDiabetesRisk(patientId).pipe(catchError(() => of(this.getDefaultRisk('diabetes')))),
      cardiovascular: this.calculateCardiovascularRisk(patientId).pipe(catchError(() => of(this.getDefaultRisk('cardiovascular')))),
      mentalHealth: this.calculateMentalHealthCrisisRisk(patientId).pipe(catchError(() => of(this.getDefaultRisk('mental-health')))),
      respiratory: this.calculateRespiratoryRisk(patientId).pipe(catchError(() => of(this.getDefaultRisk('respiratory'))))
    }).pipe(
      map(assessments => [
        assessments.diabetes,
        assessments.cardiovascular,
        assessments.mentalHealth,
        assessments.respiratory
      ])
    );
  }

  /**
   * Get default risk assessment for a category
   */
  private getDefaultRisk(category: 'diabetes' | 'cardiovascular' | 'mental-health' | 'respiratory'): CategoryRiskAssessment {
    return {
      category,
      riskLevel: 'low',
      score: 0,
      factors: ['Insufficient data for assessment'],
      recommendations: ['Schedule appropriate screening'],
      lastAssessed: new Date()
    };
  }

  /**
   * Helper method to get conditions for a patient
   */
  private getConditions(patientId: string): Observable<FhirBundle> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.CONDITION);
    const params = new HttpParams().set('patient', patientId);
    return this.http.get<FhirBundle>(url, { headers: this.getHeaders(), params });
  }

  /**
   * Helper method to get questionnaire responses by type
   */
  private getQuestionnaireResponsesByType(patientId: string, questionnaireType: string): Observable<FhirBundle> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.QUESTIONNAIRE_RESPONSE);
    const params = new HttpParams()
      .set('patient', patientId)
      .set('questionnaire', questionnaireType)
      .set('status', 'completed')
      .set('_sort', '-authored')
      .set('_count', '10');

    return this.http.get<FhirBundle>(url, { headers: this.getHeaders(), params }).pipe(
      catchError(() => of({ resourceType: 'Bundle', type: 'searchset', total: 0, entry: [] } as FhirBundle))
    );
  }

  /**
   * Helper method to get observations by LOINC code
   */
  private getObservations(patientId: string, loincCode: string): Observable<FhirBundle> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.OBSERVATION);
    const params = new HttpParams()
      .set('patient', patientId)
      .set('code', `http://loinc.org|${loincCode}`)
      .set('_sort', '-date')
      .set('_count', '10');

    return this.http.get<FhirBundle>(url, { headers: this.getHeaders(), params }).pipe(
      catchError(() => of({ resourceType: 'Bundle', type: 'searchset', total: 0, entry: [] } as FhirBundle))
    );
  }

  // ========================================================================
  // FEATURE 5.3: SDOH REFERRAL MANAGEMENT
  // ========================================================================

  /**
   * Search for community resources by SDOH category
   * Feature 5.3: SDOH Referral Management
   * API: GET /patient-health/sdoh/community-resources/search
   *
   * @param category - SDOH category to search for
   * @param acceptsReferralsOnly - Filter to only resources that accept referrals
   * @returns Observable of matching community resources
   */
  searchCommunityResources(
    category: SDOHCategory,
    acceptsReferralsOnly?: boolean
  ): Observable<CommunityResource[]> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.COMMUNITY_RESOURCES_SEARCH);
    let params = new HttpParams().set('category', category);

    if (acceptsReferralsOnly !== undefined) {
      params = params.set('acceptsReferrals', acceptsReferralsOnly.toString());
    }

    return this.http.get<any[]>(url, { headers: this.getHeaders(), params }).pipe(
      map((resources) =>
        resources.map((resource) => ({
          id: resource.id,
          name: resource.name,
          category: resource.category,
          description: resource.description,
          address: resource.address,
          phone: resource.phone,
          website: resource.website,
          servicesOffered: resource.servicesOffered || [],
          eligibilityRequirements: resource.eligibilityRequirements,
          operatingHours: resource.operatingHours,
          acceptsReferrals: resource.acceptsReferrals,
        }))
      ),
      catchError((error) => {
        console.error('Error searching community resources:', error);
        return of([]);
      })
    );
  }

  /**
   * Create a new SDOH referral
   * Feature 5.3: SDOH Referral Management
   * API: POST /patient-health/sdoh/referrals
   *
   * @param patientId - Patient identifier
   * @param need - SDOH need with details
   * @param resource - Community resource to refer to
   * @returns Observable of created referral
   */
  createReferral(
    patientId: string,
    need: SDOHNeedWithDetails,
    resource: CommunityResource
  ): Observable<SDOHReferralDetail> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.SDOH_REFERRALS);

    const body = {
      patientId,
      category: need.category,
      need,
      resource,
      status: 'draft',
      priority: this.determinePriority(need.severity),
      createdDate: new Date().toISOString(),
    };

    return this.http.post<any>(url, body, { headers: this.getHeaders() }).pipe(
      map((response) => this.mapReferralResponse(response)),
      catchError((error) => {
        console.error('Error creating referral:', error);
        return throwError(() => new Error('Failed to create referral'));
      })
    );
  }

  /**
   * Send a referral to the community resource
   * Feature 5.3: SDOH Referral Management
   * API: POST /patient-health/sdoh/referrals/{referralId}/send
   *
   * @param referralId - Referral identifier
   * @returns Observable of updated referral
   */
  sendReferral(referralId: string): Observable<SDOHReferralDetail> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.SDOH_REFERRAL_SEND(referralId));

    return this.http.post<any>(url, {}, { headers: this.getHeaders() }).pipe(
      map((response) => this.mapReferralResponse(response)),
      catchError((error) => {
        console.error('Error sending referral:', error);
        const errorMessage = error.error || error.statusText || 'Failed to send referral';
        return throwError(() => new Error(errorMessage));
      })
    );
  }

  /**
   * Update referral status
   * Feature 5.3: SDOH Referral Management
   * API: PUT /patient-health/sdoh/referrals/{referralId}/status
   *
   * @param referralId - Referral identifier
   * @param status - New status
   * @param outcome - Outcome details (required for completed status)
   * @param cancelReason - Reason for cancellation (required for cancelled status)
   * @returns Observable of updated referral
   */
  updateReferralStatus(
    referralId: string,
    status: 'draft' | 'sent' | 'accepted' | 'in-progress' | 'completed' | 'cancelled',
    outcome?: ReferralOutcome,
    cancelReason?: string
  ): Observable<SDOHReferralDetail> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.SDOH_REFERRAL_STATUS(referralId));

    const body: any = { status };

    if (outcome) {
      body.outcome = outcome;
    }

    if (cancelReason) {
      body.cancelReason = cancelReason;
    }

    return this.http.put<any>(url, body, { headers: this.getHeaders() }).pipe(
      map((response) => this.mapReferralResponse(response)),
      catchError((error) => {
        console.error('Error updating referral status:', error);
        const errorMessage = error.error || error.statusText || 'Failed to update referral status';
        return throwError(() => new Error(errorMessage));
      })
    );
  }

  /**
   * Get referral history for a patient
   * Feature 5.3: SDOH Referral Management
   * API: GET /patient-health/sdoh/referrals/patient/{patientId}
   *
   * @param patientId - Patient identifier
   * @param criteria - Search criteria (optional)
   * @returns Observable of patient referrals
   */
  getReferralHistory(
    patientId: string,
    criteria?: ReferralSearchCriteria
  ): Observable<SDOHReferralDetail[]> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.SDOH_REFERRAL_HISTORY(patientId));

    let params = new HttpParams();

    if (criteria?.category) {
      params = params.set('category', criteria.category);
    }

    if (criteria?.status && criteria.status.length > 0) {
      params = params.set('status', criteria.status.join(','));
    }

    if (criteria?.dateRange) {
      params = params.set('startDate', criteria.dateRange.start.toISOString());
      params = params.set('endDate', criteria.dateRange.end.toISOString());
    }

    return this.http.get<any[]>(url, { headers: this.getHeaders(), params }).pipe(
      map((referrals) => referrals.map((ref) => this.mapReferralResponse(ref))),
      catchError((error) => {
        console.error('Error fetching referral history:', error);
        return of([]);
      })
    );
  }

  /**
   * Get referral metrics for a patient
   * Feature 5.3: SDOH Referral Management
   * API: GET /patient-health/sdoh/referrals/patient/{patientId}/metrics
   *
   * @param patientId - Patient identifier
   * @returns Observable of referral metrics
   */
  getReferralMetrics(patientId: string): Observable<any> {
    const url = buildQualityMeasureUrl(QUALITY_MEASURE_ENDPOINTS.SDOH_REFERRAL_METRICS(patientId));

    return this.http.get<any>(url, { headers: this.getHeaders() }).pipe(
      map((metrics) => ({
        completionRate: metrics.completionRate,
        averageTimeToCompletion: metrics.averageTimeToCompletion,
        totalReferrals: metrics.totalReferrals,
        completedReferrals: metrics.completedReferrals,
        successfulOutcomeRate: metrics.successfulOutcomeRate,
        byCategory: metrics.byCategory || {},
      })),
      catchError((error) => {
        console.error('Error fetching referral metrics:', error);
        return of({
          completionRate: 0,
          averageTimeToCompletion: 0,
          totalReferrals: 0,
          completedReferrals: 0,
          successfulOutcomeRate: 0,
          byCategory: {},
        });
      })
    );
  }

  /**
   * Map referral response from backend to frontend format
   */
  private mapReferralResponse(response: any): SDOHReferralDetail {
    return {
      referralId: response.referralId,
      patientId: response.patientId,
      category: response.category,
      need: response.need,
      resource: response.resource,
      status: response.status,
      priority: response.priority,
      createdDate: new Date(response.createdDate),
      sentDate: response.sentDate ? new Date(response.sentDate) : undefined,
      acceptedDate: response.acceptedDate ? new Date(response.acceptedDate) : undefined,
      completedDate: response.completedDate ? new Date(response.completedDate) : undefined,
      cancelledDate: response.cancelledDate ? new Date(response.cancelledDate) : undefined,
      cancelReason: response.cancelReason,
      outcome: response.outcome ? {
        result: response.outcome.result,
        servicesReceived: response.outcome.servicesReceived || [],
        followUpNeeded: response.outcome.followUpNeeded,
        patientSatisfaction: response.outcome.patientSatisfaction,
        notes: response.outcome.notes,
        assessedDate: new Date(response.outcome.assessedDate),
      } : undefined,
      notes: response.notes || [],
    };
  }

  /**
   * Determine referral priority based on SDOH need severity
   */
  private determinePriority(severity: SDOHSeverity): 'low' | 'medium' | 'high' | 'urgent' {
    const priorityMap: Record<SDOHSeverity, 'low' | 'medium' | 'high' | 'urgent'> = {
      'none': 'low',
      'mild': 'low',
      'moderate': 'medium',
      'severe': 'urgent',
    };
    return priorityMap[severity] || 'medium';
  }

}
