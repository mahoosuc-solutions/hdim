import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, forkJoin, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { CacheableService } from '../shared/cacheable.service';
import { LoggerService, ContextualLogger } from '../logger.service';
import { FhirQuestionnaireService } from '../fhir/fhir-questionnaire.service';
import {
  API_CONFIG,
  FHIR_ENDPOINTS,
  buildFhirUrl,
  HTTP_HEADERS,
} from '../../config/api.config';
import {
  FhirBundle,
  FhirObservation,
  FhirQuestionnaireResponse,
  FhirQuestionnaireResponseItem,
} from '../../models/fhir.model';
import {
  SDOHSummary,
  SDOHCategory,
  SDOHSeverity,
  SDOHQuestionnaireType,
  SDOHRiskFactor,
  SDOHNeedWithDetails,
  SDOHScreeningResult,
  ServiceReferral,
  SocialDeterminants,
  RiskLevel,
} from '../../models/patient-health.model';

/**
 * SDOH (Social Determinants of Health) Service
 *
 * Handles all SDOH data aggregation and analysis:
 * - PRAPARE and AHC-HRSN screening parsing
 * - Z-code mapping for social risk factors
 * - Service referral tracking
 * - Risk stratification for social needs
 *
 * Features:
 * - Built-in caching with 5-minute TTL
 * - Automatic Z-code generation
 * - Severity-based sorting
 * - Integration with FHIR QuestionnaireResponse and Observation
 */
@Injectable({
  providedIn: 'root',
})
export class SDOHService extends CacheableService {

  // SDOH Category to Z-Code Mapping (ICD-10)
  private readonly SDOH_CATEGORY_ZCODES: Record<SDOHCategory, string> = {
    food: 'Z59.4',
    housing: 'Z59.0',
    transportation: 'Z59.82',
    financial: 'Z59.86',
    employment: 'Z56.0',
    education: 'Z55.9',
    social: 'Z60.4',
    safety: 'Z63.0',
    'food-insecurity': 'Z59.4',
    'housing-instability': 'Z59.0',
    'utility-assistance': 'Z59.5',
    'interpersonal-safety': 'Z69.1',
    'social-isolation': 'Z60.4',
    'financial-strain': 'Z59.9',
  };

  constructor(
    private http: HttpClient,
    private fhirQuestionnaire: FhirQuestionnaireService,
    private logger: LoggerService
  ) {
    super({ ttlMs: 5 * 60 * 1000 }); // 5 minute cache
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
   * Get comprehensive SDOH summary
   */
  getSDOHSummary(patientId: string): Observable<SDOHSummary> {
    const cacheKey = `sdoh:summary:${patientId}`;
    const cached = this.getCached<SDOHSummary>(cacheKey);
    if (cached) return of(cached);

    return this.getSDOHScreeningFromFhir(patientId).pipe(
      map((screeningResult) => {
        // Convert SDOHNeedWithDetails to needs format
        const needs = screeningResult.needs.map((need) => ({
          category: need.category,
          description: `${need.questionText}: ${need.response}`,
          severity: need.severity,
          identified: screeningResult.screeningDate,
          addressed: false,
          interventions: [],
        }));

        // Calculate overall risk
        const riskMap: Record<string, RiskLevel> = {
          low: 'low',
          moderate: 'moderate',
          high: 'high',
        };
        const overallRisk = riskMap[screeningResult.overallRisk];

        // Extract Z-codes
        const zCodes = screeningResult.needs.map((n) => n.zCode);

        const summary: SDOHSummary = {
          overallRisk,
          screeningDate: screeningResult.screeningDate,
          needs,
          activeReferrals: [],
          zCodes,
        };

        this.setCache(cacheKey, summary);
        return summary;
      }),
      catchError(() => of(this.getMockSDOHSummary(patientId)))
    );
  }

  /**
   * Get complete social determinants data with FHIR integration
   */
  getSocialDeterminants(patientId: string): Observable<SocialDeterminants> {
    const cacheKey = `sdoh:determinants:${patientId}`;
    const cached = this.getCached<SocialDeterminants>(cacheKey);
    if (cached) return of(cached);

    return forkJoin({
      questionnaireResponses: this.getSDOHQuestionnaireResponses(patientId),
      observations: this.getSDOHObservations(patientId),
      serviceRequests: this.getSDOHServiceRequests(patientId),
    }).pipe(
      map(({ questionnaireResponses, observations, serviceRequests }) => {
        // Parse questionnaire data
        const questionnaireData = this.parseSDOHQuestionnaire(
          questionnaireResponses,
          patientId
        );

        // Parse risk factors from observations
        const riskFactors = this.parseSDOHRiskFactors(observations);

        // Parse service referrals
        const activeReferrals = this.parseServiceReferrals(serviceRequests);

        const determinants: SocialDeterminants = {
          patientId,
          screeningDate: questionnaireData.screeningDate,
          questionnaireType: questionnaireData.questionnaireType,
          housingStatus: questionnaireData.housingStatus,
          foodSecurity: questionnaireData.foodSecurity,
          transportation: questionnaireData.transportation,
          employment: questionnaireData.employment,
          socialSupport: questionnaireData.socialSupport,
          riskFactors,
          activeReferrals,
        };

        this.setCache(cacheKey, determinants);
        return determinants;
      }),
      catchError((error) => {
        this.logger.error('Error fetching social determinants:', error);
        return of(this.getDefaultSocialDeterminants(patientId));
      })
    );
  }

  /**
   * Get SDOH screening results from FHIR
   */
  getSDOHScreeningFromFhir(patientId: string): Observable<SDOHScreeningResult> {
    const cacheKey = `sdoh:screening:${patientId}`;
    const cached = this.getCached<SDOHScreeningResult>(cacheKey);
    if (cached) return of(cached);

    return this.fhirQuestionnaire.getSDOHScreenings(patientId).pipe(
      map((screenings) => {
        if (screenings.length === 0) {
          return this.getDefaultScreeningResult();
        }

        const mostRecent = screenings[0];

        // Determine questionnaire type
        let questionnaireType: SDOHQuestionnaireType = 'custom';
        if (mostRecent.questionnaireType === 'PRAPARE') {
          questionnaireType = 'PRAPARE';
        } else if (mostRecent.questionnaireType === 'AHC-HRSN') {
          questionnaireType = 'AHC-HRSN';
        }

        // Convert parsed items to needs
        const needs = this.convertItemsToNeeds(mostRecent.items);

        // Sort by severity
        const sortedNeeds = this.sortSDOHNeedsBySeverity(needs);

        // Calculate overall risk
        const overallRisk = this.calculateOverallRisk(sortedNeeds);

        const result: SDOHScreeningResult = {
          screeningDate: mostRecent.authoredDate,
          questionnaireType,
          questionnaireName: questionnaireType,
          needs: sortedNeeds,
          overallRisk,
          zCodes: sortedNeeds
            .map((n) => n.zCode)
            .filter((code, index, self) => self.indexOf(code) === index),
        };

        this.setCache(cacheKey, result);
        return result;
      }),
      catchError((error) => {
        this.logger.error('Error fetching SDOH screening from FHIR:', error);
        return of(this.getDefaultScreeningResult());
      })
    );
  }

  /**
   * Calculate SDOH risk score (0-100)
   */
  calculateSDOHRiskScore(patientId: string): Observable<{ score: number; level: RiskLevel }> {
    return this.getSDOHSummary(patientId).pipe(
      map((summary) => {
        let score = 0;

        // Calculate score based on needs
        for (const need of summary.needs) {
          if (need.severity === 'severe' && !need.addressed) {
            score += 25;
          } else if (need.severity === 'moderate' && !need.addressed) {
            score += 15;
          } else if (need.severity === 'mild' && !need.addressed) {
            score += 5;
          }
        }

        // Determine level
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

        return { score: Math.min(100, score), level };
      })
    );
  }

  /**
   * Identify intervention needs based on SDOH screening
   */
  identifySDOHInterventionNeeds(
    patientId: string
  ): Observable<Array<{ category: SDOHCategory; priority: string; recommendation: string }>> {
    return this.getSDOHSummary(patientId).pipe(
      map((summary) => {
        const interventions: Array<{
          category: SDOHCategory;
          priority: string;
          recommendation: string;
        }> = [];

        for (const need of summary.needs) {
          if (!need.addressed) {
            interventions.push({
              category: need.category,
              priority: need.severity === 'severe' ? 'high' : need.severity === 'moderate' ? 'medium' : 'low',
              recommendation: this.getRecommendationForCategory(need.category),
            });
          }
        }

        return interventions;
      })
    );
  }

  /**
   * Map SDOH category to Z-code
   */
  mapSDOHCategoryToZCode(category: SDOHCategory): string {
    return this.SDOH_CATEGORY_ZCODES[category] || 'Z59.9';
  }

  /**
   * Get all Z-codes for a patient's SDOH needs
   */
  getPatientZCodes(patientId: string): Observable<string[]> {
    return this.getSDOHSummary(patientId).pipe(
      map((summary) => summary.zCodes || [])
    );
  }

  /**
   * Invalidate SDOH cache for a patient
   */
  invalidatePatientSDOH(patientId: string): void {
    this.invalidatePatientCache(patientId);
    this.fhirQuestionnaire.invalidatePatientQuestionnaires(patientId);
  }

  // ===== Private Helper Methods =====

  /**
   * Get SDOH QuestionnaireResponses from FHIR
   */
  private getSDOHQuestionnaireResponses(
    patientId: string
  ): Observable<FhirBundle<FhirQuestionnaireResponse>> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.QUESTIONNAIRE_RESPONSE);
    const params = new HttpParams()
      .set('patient', patientId)
      .set('status', 'completed')
      .set('_sort', '-authored')
      .set('_count', '50');

    return this.http.get<FhirBundle<FhirQuestionnaireResponse>>(url, {
      headers: this.getHeaders(),
      params,
    });
  }

  /**
   * Get SDOH Observations from FHIR
   */
  private getSDOHObservations(
    patientId: string
  ): Observable<FhirBundle<FhirObservation>> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.OBSERVATION);
    const params = new HttpParams()
      .set('patient', patientId)
      .set('category', 'sdoh')
      .set('_sort', '-date')
      .set('_count', '100');

    return this.http
      .get<FhirBundle<FhirObservation>>(url, {
        headers: this.getHeaders(),
        params,
      })
      .pipe(catchError(() => of({ resourceType: 'Bundle' as const, type: 'searchset' as const, entry: [] })));
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

    return this.http
      .get<FhirBundle<any>>(url, {
        headers: this.getHeaders(),
        params,
      })
      .pipe(catchError(() => of({ resourceType: 'Bundle' as const, type: 'searchset' as const, entry: [] })));
  }

  /**
   * Parse SDOH questionnaire responses
   */
  private parseSDOHQuestionnaire(
    bundle: FhirBundle<FhirQuestionnaireResponse>,
    patientId: string
  ): any {
    if (!bundle.entry || bundle.entry.length === 0) {
      return this.getDefaultQuestionnaireData();
    }

    // Filter for SDOH questionnaires
    const sdohResponses = bundle.entry
      .map((e) => e.resource)
      .filter((r): r is FhirQuestionnaireResponse => r !== undefined)
      .filter(
        (r) =>
          r.questionnaire?.includes('PRAPARE') ||
          r.questionnaire?.includes('AHC-HRSN') ||
          r.questionnaire?.includes('sdoh')
      );

    if (sdohResponses.length === 0) {
      return this.getDefaultQuestionnaireData();
    }

    // Get most recent response
    const mostRecent = sdohResponses.sort(
      (a, b) => new Date(b.authored).getTime() - new Date(a.authored).getTime()
    )[0];

    // Determine questionnaire type
    let questionnaireType: SDOHQuestionnaireType = 'custom';
    if (mostRecent.questionnaire?.includes('PRAPARE')) {
      questionnaireType = 'PRAPARE';
    } else if (mostRecent.questionnaire?.includes('AHC-HRSN')) {
      questionnaireType = 'AHC-HRSN';
    }

    // Parse items
    const items = mostRecent.item || [];

    return {
      screeningDate: new Date(mostRecent.authored),
      questionnaireType,
      housingStatus: this.parseHousingStatus(items),
      foodSecurity: this.parseFoodSecurity(items),
      transportation: this.parseTransportation(items),
      employment: this.parseEmployment(items),
      socialSupport: this.parseSocialSupport(items),
    };
  }

  /**
   * Parse housing status from items
   */
  private parseHousingStatus(
    items: FhirQuestionnaireResponseItem[]
  ): { stable: boolean; details: string } {
    const housingItem = items.find(
      (item) =>
        item.linkId?.includes('housing') ||
        item.text?.toLowerCase().includes('housing')
    );

    if (!housingItem?.answer?.[0]) {
      return { stable: true, details: 'No housing data' };
    }

    const answer = housingItem.answer[0].valueString || '';
    const questionText = housingItem.text?.toLowerCase() || '';

    // Check if asking about worries
    if (questionText.includes('worried') || questionText.includes('losing')) {
      const stable = !answer.toLowerCase().includes('yes');
      return { stable, details: answer || 'Not specified' };
    }

    const stable =
      !answer.toLowerCase().includes('do not have') &&
      !answer.toLowerCase().includes('worried') &&
      !answer.toLowerCase().includes('unstable');

    return { stable, details: answer || 'Not specified' };
  }

  /**
   * Parse food security from items
   */
  private parseFoodSecurity(
    items: FhirQuestionnaireResponseItem[]
  ): { secure: boolean; details: string } {
    const foodItem = items.find(
      (item) =>
        item.linkId?.includes('food') ||
        item.text?.toLowerCase().includes('food')
    );

    if (!foodItem?.answer?.[0]) {
      return { secure: true, details: 'No food security data' };
    }

    const answer = foodItem.answer[0].valueString || '';
    const secure =
      !answer.toLowerCase().includes('often') &&
      !answer.toLowerCase().includes('sometimes') &&
      !answer.toLowerCase().includes('worry');

    return { secure, details: answer || 'Not specified' };
  }

  /**
   * Parse transportation from items
   */
  private parseTransportation(
    items: FhirQuestionnaireResponseItem[]
  ): { adequate: boolean; details: string } {
    const transportItem = items.find(
      (item) =>
        item.linkId?.includes('transport') ||
        item.text?.toLowerCase().includes('transport')
    );

    if (!transportItem?.answer?.[0]) {
      return { adequate: true, details: 'No transportation data' };
    }

    const answer = transportItem.answer[0].valueString || '';
    const adequate =
      answer.toLowerCase().includes('yes') ||
      (!answer.toLowerCase().includes('no') &&
        !answer.toLowerCase().includes('lack'));

    return { adequate, details: answer || 'Not specified' };
  }

  /**
   * Parse employment from items
   */
  private parseEmployment(
    items: FhirQuestionnaireResponseItem[]
  ): { status: string; details: string } {
    const employmentItem = items.find(
      (item) =>
        item.linkId?.includes('employment') ||
        item.text?.toLowerCase().includes('employment')
    );

    if (!employmentItem?.answer?.[0]) {
      return { status: 'Unknown', details: 'No employment data' };
    }

    const answer = employmentItem.answer[0].valueString || '';
    return { status: answer, details: answer || 'Not specified' };
  }

  /**
   * Parse social support from items
   */
  private parseSocialSupport(
    items: FhirQuestionnaireResponseItem[]
  ): { level: string; details: string } {
    const socialItem = items.find(
      (item) =>
        item.linkId?.includes('social') ||
        item.text?.toLowerCase().includes('social') ||
        item.text?.toLowerCase().includes('see or talk')
    );

    if (!socialItem?.answer?.[0]) {
      return { level: 'Unknown', details: 'No social support data' };
    }

    const answer = socialItem.answer[0].valueString || '';
    let level = 'moderate';

    if (answer.toLowerCase().includes('less than once')) {
      level = 'weak';
    } else if (
      answer.toLowerCase().includes('daily') ||
      answer.toLowerCase().includes('every day')
    ) {
      level = 'strong';
    }

    return { level, details: answer || 'Not specified' };
  }

  /**
   * Parse SDOH risk factors from observations
   */
  private parseSDOHRiskFactors(
    bundle: FhirBundle<FhirObservation>
  ): SDOHRiskFactor[] {
    if (!bundle.entry || bundle.entry.length === 0) {
      return [];
    }

    return bundle.entry
      .map((entry) => entry.resource)
      .filter((obs): obs is FhirObservation => obs !== undefined)
      .map((obs) => {
        const code = obs.code?.coding?.[0]?.code || '';
        const display =
          obs.code?.coding?.[0]?.display || obs.code?.text || '';
        const value =
          obs.valueCodeableConcept?.text ||
          obs.valueString ||
          obs.valueCodeableConcept?.coding?.[0]?.display ||
          'Unknown';

        const category = this.determineSdohCategory(code, display);
        const severity = this.determineSdohSeverity(value, display);
        const zCode = this.SDOH_CATEGORY_ZCODES[category] || '';

        return {
          category,
          code,
          display,
          value,
          severity,
          date: new Date(obs.effectiveDateTime || new Date()),
          zCode,
        };
      });
  }

  /**
   * Parse service referrals from ServiceRequest resources
   */
  private parseServiceReferrals(bundle: FhirBundle<any>): ServiceReferral[] {
    if (!bundle.entry || bundle.entry.length === 0) {
      return [];
    }

    return bundle.entry
      .map((entry) => entry.resource)
      .filter(
        (sr): sr is any =>
          sr !== undefined && sr.resourceType === 'ServiceRequest'
      )
      .map((sr) => {
        const category = this.determineSdohCategoryFromServiceRequest(sr);
        const service =
          sr.code?.text || sr.code?.coding?.[0]?.display || 'Unknown service';
        const organization =
          sr.performer?.[0]?.display || 'Unknown organization';
        const note = sr.note?.[0]?.text || '';

        return {
          id: sr.id || '',
          category,
          service,
          organization,
          status: sr.status || 'unknown',
          priority: sr.priority || 'routine',
          authoredDate: new Date(sr.authoredOn || new Date()),
          occurrenceDate: sr.occurrenceDateTime
            ? new Date(sr.occurrenceDateTime)
            : undefined,
          note,
        };
      });
  }

  /**
   * Convert parsed items to SDOH needs
   */
  private convertItemsToNeeds(items: any[]): SDOHNeedWithDetails[] {
    const needs: SDOHNeedWithDetails[] = [];

    for (const item of items) {
      const category = this.determineSdohCategoryFromLinkId(item.linkId);
      if (category) {
        const severity = this.determineSeverityFromAnswer(item.answer);
        needs.push({
          category,
          questionText: item.text,
          response: String(item.answer),
          severity,
          zCode: this.SDOH_CATEGORY_ZCODES[category] || 'Z59.9',
        });
      }
    }

    return needs;
  }

  /**
   * Determine SDOH category from link ID
   */
  private determineSdohCategoryFromLinkId(linkId: string): SDOHCategory | null {
    if (!linkId) return null;

    const text = linkId.toLowerCase();
    if (text.includes('food')) return 'food-insecurity';
    if (text.includes('housing') || text.includes('homeless'))
      return 'housing-instability';
    if (text.includes('transport')) return 'transportation';
    if (text.includes('employ')) return 'employment';
    if (text.includes('financial') || text.includes('income'))
      return 'financial-strain';
    if (text.includes('social') || text.includes('isolation'))
      return 'social-isolation';
    if (text.includes('safety') || text.includes('violence'))
      return 'interpersonal-safety';
    if (text.includes('education')) return 'education';
    if (text.includes('utility')) return 'utility-assistance';

    return null;
  }

  /**
   * Determine severity from answer
   */
  private determineSeverityFromAnswer(answer: any): SDOHSeverity {
    const text = String(answer).toLowerCase();

    if (
      text.includes('often') ||
      text.includes('always') ||
      text.includes('homeless')
    ) {
      return 'severe';
    }
    if (text.includes('sometimes') || text.includes('occasionally')) {
      return 'moderate';
    }
    if (text.includes('rarely') || text.includes('never')) {
      return 'mild';
    }

    return 'moderate';
  }

  /**
   * Determine SDOH category from code/display
   */
  private determineSdohCategory(code: string, display: string): SDOHCategory {
    const text = (code + ' ' + display).toLowerCase();

    if (text.includes('food')) return 'food-insecurity';
    if (text.includes('housing') || text.includes('homeless'))
      return 'housing-instability';
    if (text.includes('transport')) return 'transportation';
    if (text.includes('employ')) return 'employment';
    if (text.includes('financial') || text.includes('income'))
      return 'financial-strain';
    if (text.includes('social') || text.includes('isolation'))
      return 'social-isolation';
    if (text.includes('safety') || text.includes('violence'))
      return 'interpersonal-safety';
    if (text.includes('education')) return 'education';
    if (text.includes('utility')) return 'utility-assistance';

    return 'social';
  }

  /**
   * Determine SDOH severity from value/display
   */
  private determineSdohSeverity(value: string, display: string): SDOHSeverity {
    const text = (value + ' ' + display).toLowerCase();

    if (
      text.includes('severe') ||
      text.includes('critical') ||
      text.includes('homeless')
    ) {
      return 'severe';
    }
    if (text.includes('moderate') || text.includes('at risk')) {
      return 'moderate';
    }
    if (text.includes('mild') || text.includes('some concern')) {
      return 'mild';
    }

    return 'moderate';
  }

  /**
   * Determine SDOH category from ServiceRequest
   */
  private determineSdohCategoryFromServiceRequest(sr: any): SDOHCategory {
    const categoryCode = sr.category?.[0]?.coding?.[0]?.code || '';
    const categoryDisplay = sr.category?.[0]?.coding?.[0]?.display || '';
    const serviceText = sr.code?.text || '';

    const text = (
      categoryCode +
      ' ' +
      categoryDisplay +
      ' ' +
      serviceText
    ).toLowerCase();

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
   * Sort SDOH needs by severity
   */
  private sortSDOHNeedsBySeverity(needs: SDOHNeedWithDetails[]): SDOHNeedWithDetails[] {
    const severityOrder: Record<SDOHSeverity, number> = {
      severe: 0,
      moderate: 1,
      mild: 2,
      none: 3,
    };

    return [...needs].sort(
      (a, b) => severityOrder[a.severity] - severityOrder[b.severity]
    );
  }

  /**
   * Calculate overall risk from needs
   */
  private calculateOverallRisk(needs: SDOHNeedWithDetails[]): 'low' | 'moderate' | 'high' {
    const severeCount = needs.filter((n) => n.severity === 'severe').length;
    const moderateCount = needs.filter((n) => n.severity === 'moderate').length;

    if (severeCount >= 2 || (severeCount >= 1 && moderateCount >= 2)) {
      return 'high';
    } else if (severeCount >= 1 || moderateCount >= 2) {
      return 'moderate';
    } else if (moderateCount >= 1) {
      return 'moderate';
    }

    return 'low';
  }

  /**
   * Get recommendation for category
   */
  private getRecommendationForCategory(category: SDOHCategory): string {
    const recommendations: Record<SDOHCategory, string> = {
      food: 'Refer to food assistance programs (SNAP, food bank)',
      'food-insecurity': 'Refer to food assistance programs (SNAP, food bank)',
      housing: 'Connect with housing assistance services',
      'housing-instability': 'Connect with housing assistance services',
      transportation: 'Provide transportation vouchers or medical transit',
      financial: 'Refer to financial counseling and assistance programs',
      'financial-strain': 'Refer to financial counseling and assistance programs',
      employment: 'Connect with job training and employment services',
      education: 'Refer to educational support services',
      social: 'Connect with community social support groups',
      'social-isolation': 'Connect with community social support groups',
      safety: 'Refer to safety planning and victim services',
      'interpersonal-safety': 'Refer to safety planning and victim services',
      'utility-assistance': 'Connect with utility assistance programs',
    };

    return recommendations[category] || 'Assess and provide appropriate referral';
  }

  /**
   * Get default questionnaire data
   */
  private getDefaultQuestionnaireData(): any {
    return {
      screeningDate: new Date(),
      questionnaireType: 'custom' as SDOHQuestionnaireType,
      housingStatus: { stable: true, details: 'No data available' },
      foodSecurity: { secure: true, details: 'No data available' },
      transportation: { adequate: true, details: 'No data available' },
      employment: { status: 'Unknown', details: 'No data available' },
      socialSupport: { level: 'Unknown', details: 'No data available' },
    };
  }

  /**
   * Get default screening result
   */
  private getDefaultScreeningResult(): SDOHScreeningResult {
    return {
      screeningDate: new Date(),
      questionnaireType: 'custom',
      needs: [],
      overallRisk: 'low',
    };
  }

  /**
   * Get default social determinants
   */
  private getDefaultSocialDeterminants(patientId: string): SocialDeterminants {
    return {
      patientId,
      ...this.getDefaultQuestionnaireData(),
      riskFactors: [],
      activeReferrals: [],
    };
  }

  /**
   * Get mock SDOH summary for fallback
   */
  private getMockSDOHSummary(patientId: string): SDOHSummary {
    return {
      overallRisk: 'low',
      screeningDate: new Date(),
      needs: [],
      activeReferrals: [],
      zCodes: [],
    };
  }
}
