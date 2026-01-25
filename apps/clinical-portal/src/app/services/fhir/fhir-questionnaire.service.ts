import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { CacheableService } from '../shared/cacheable.service';
import { LoggerService, ContextualLogger } from '../logger.service';
import {
  API_CONFIG,
  FHIR_ENDPOINTS,
  buildFhirUrl,
  HTTP_HEADERS,
} from '../../config/api.config';
import {
  FhirBundle,
  FhirQuestionnaireResponse,
  FhirQuestionnaireResponseItem,
} from '../../models/fhir.model';
import {
  MentalHealthAssessmentType,
  SDOHQuestionnaireType,
} from '../../models/patient-health.model';

/**
 * Parsed questionnaire response
 */
export interface ParsedQuestionnaireResponse {
  id: string;
  questionnaireType: string;
  authoredDate: Date;
  status: string;
  items: ParsedQuestionnaireItem[];
  totalScore?: number;
}

/**
 * Parsed questionnaire item
 */
export interface ParsedQuestionnaireItem {
  linkId: string;
  text: string;
  answer: string | number | boolean;
  answerDisplay?: string;
}

/**
 * FHIR Questionnaire Response Service
 *
 * Handles all FHIR QuestionnaireResponse resource queries including:
 * - Mental health assessments (PHQ-9, GAD-7, PHQ-2)
 * - SDOH screenings (PRAPARE, AHC-HRSN)
 * - Functional status assessments
 *
 * Features:
 * - Built-in caching with 5-minute TTL
 * - Questionnaire type detection
 * - Score calculation for standardized assessments
 */
@Injectable({
  providedIn: 'root',
})
export class FhirQuestionnaireService extends CacheableService {
  private log: ContextualLogger;

  // Questionnaire identifiers
  private readonly QUESTIONNAIRE_TYPES = {
    PHQ9: ['phq-9', 'phq9', 'patient-health-questionnaire-9'],
    PHQ2: ['phq-2', 'phq2', 'patient-health-questionnaire-2'],
    GAD7: ['gad-7', 'gad7', 'generalized-anxiety-disorder-7'],
    PRAPARE: ['prapare', 'protocol-for-responding-to-assessing-patients-assets'],
    AHC_HRSN: ['ahc-hrsn', 'accountable-health-communities'],
    FUNCTIONAL: ['functional-status', 'adl', 'iadl'],
  };

  constructor(
    private http: HttpClient,
    private logger: LoggerService
  ) {
    super({ ttlMs: 5 * 60 * 1000 }); // 5 minute cache
    this.log = this.logger.withContext('FhirQuestionnaireService');
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
   * Get all questionnaire responses for a patient
   */
  getQuestionnaireResponses(
    patientId: string,
    limit = 50
  ): Observable<FhirQuestionnaireResponse[]> {
    const cacheKey = `questionnaire:all:${patientId}:${limit}`;
    const cached = this.getCached<FhirQuestionnaireResponse[]>(cacheKey);
    if (cached) return of(cached);

    const url = buildFhirUrl(FHIR_ENDPOINTS.QUESTIONNAIRE_RESPONSE);
    const params = new HttpParams()
      .set('patient', patientId)
      .set('_sort', '-authored')
      .set('_count', String(limit));

    return this.http
      .get<FhirBundle<FhirQuestionnaireResponse>>(url, {
        headers: this.getHeaders(),
        params,
      })
      .pipe(
        map((bundle) => {
          const responses = (bundle.entry || [])
            .map((entry) => entry.resource)
            .filter(
              (response): response is FhirQuestionnaireResponse =>
                response !== undefined
            );
          this.setCache(cacheKey, responses);
          return responses;
        }),
        catchError((error) => {
          this.log.error('Error fetching questionnaire responses from FHIR:', error);
          return of([]);
        })
      );
  }

  /**
   * Get mental health assessment responses (PHQ-9, GAD-7, PHQ-2)
   */
  getMentalHealthAssessments(
    patientId: string,
    type?: MentalHealthAssessmentType
  ): Observable<ParsedQuestionnaireResponse[]> {
    const cacheKey = `questionnaire:mental:${patientId}:${type || 'all'}`;
    const cached = this.getCached<ParsedQuestionnaireResponse[]>(cacheKey);
    if (cached) return of(cached);

    return this.getQuestionnaireResponses(patientId).pipe(
      map((responses) => {
        const mentalHealthTypes = [
          ...this.QUESTIONNAIRE_TYPES.PHQ9,
          ...this.QUESTIONNAIRE_TYPES.PHQ2,
          ...this.QUESTIONNAIRE_TYPES.GAD7,
        ];

        const filtered = responses
          .filter((r) => this.matchesQuestionnaireType(r, mentalHealthTypes))
          .filter((r) => !type || this.matchesSpecificType(r, type))
          .map((r) => this.parseQuestionnaireResponse(r));

        this.setCache(cacheKey, filtered);
        return filtered;
      })
    );
  }

  /**
   * Get latest mental health assessment of a specific type
   */
  getLatestMentalHealthAssessment(
    patientId: string,
    type: MentalHealthAssessmentType
  ): Observable<ParsedQuestionnaireResponse | null> {
    return this.getMentalHealthAssessments(patientId, type).pipe(
      map((assessments) => assessments[0] || null)
    );
  }

  /**
   * Get SDOH screening responses
   */
  getSDOHScreenings(
    patientId: string,
    type?: SDOHQuestionnaireType
  ): Observable<ParsedQuestionnaireResponse[]> {
    const cacheKey = `questionnaire:sdoh:${patientId}:${type || 'all'}`;
    const cached = this.getCached<ParsedQuestionnaireResponse[]>(cacheKey);
    if (cached) return of(cached);

    return this.getQuestionnaireResponses(patientId).pipe(
      map((responses) => {
        const sdohTypes = [
          ...this.QUESTIONNAIRE_TYPES.PRAPARE,
          ...this.QUESTIONNAIRE_TYPES.AHC_HRSN,
        ];

        const filtered = responses
          .filter((r) => this.matchesQuestionnaireType(r, sdohTypes))
          .map((r) => this.parseQuestionnaireResponse(r));

        this.setCache(cacheKey, filtered);
        return filtered;
      })
    );
  }

  /**
   * Get latest SDOH screening
   */
  getLatestSDOHScreening(
    patientId: string
  ): Observable<ParsedQuestionnaireResponse | null> {
    return this.getSDOHScreenings(patientId).pipe(
      map((screenings) => screenings[0] || null)
    );
  }

  /**
   * Get functional status assessments
   */
  getFunctionalStatusAssessments(
    patientId: string
  ): Observable<ParsedQuestionnaireResponse[]> {
    const cacheKey = `questionnaire:functional:${patientId}`;
    const cached = this.getCached<ParsedQuestionnaireResponse[]>(cacheKey);
    if (cached) return of(cached);

    return this.getQuestionnaireResponses(patientId).pipe(
      map((responses) => {
        const filtered = responses
          .filter((r) =>
            this.matchesQuestionnaireType(r, this.QUESTIONNAIRE_TYPES.FUNCTIONAL)
          )
          .map((r) => this.parseQuestionnaireResponse(r));

        this.setCache(cacheKey, filtered);
        return filtered;
      })
    );
  }

  /**
   * Calculate PHQ-9 score from questionnaire response
   */
  calculatePHQ9Score(response: ParsedQuestionnaireResponse): number {
    // PHQ-9 has 9 questions, each scored 0-3
    return response.items
      .filter((item) => typeof item.answer === 'number')
      .reduce((sum, item) => sum + (item.answer as number), 0);
  }

  /**
   * Calculate GAD-7 score from questionnaire response
   */
  calculateGAD7Score(response: ParsedQuestionnaireResponse): number {
    // GAD-7 has 7 questions, each scored 0-3
    return response.items
      .filter((item) => typeof item.answer === 'number')
      .reduce((sum, item) => sum + (item.answer as number), 0);
  }

  /**
   * Get PHQ-9 severity level
   */
  getPHQ9Severity(score: number): 'minimal' | 'mild' | 'moderate' | 'moderately-severe' | 'severe' {
    if (score <= 4) return 'minimal';
    if (score <= 9) return 'mild';
    if (score <= 14) return 'moderate';
    if (score <= 19) return 'moderately-severe';
    return 'severe';
  }

  /**
   * Get GAD-7 severity level
   */
  getGAD7Severity(score: number): 'minimal' | 'mild' | 'moderate' | 'severe' {
    if (score <= 4) return 'minimal';
    if (score <= 9) return 'mild';
    if (score <= 14) return 'moderate';
    return 'severe';
  }

  /**
   * Invalidate questionnaire cache for a patient
   */
  invalidatePatientQuestionnaires(patientId: string): void {
    this.invalidatePatientCache(patientId);
  }

  // ===== Private Helper Methods =====

  /**
   * Check if questionnaire matches any of the given types
   */
  private matchesQuestionnaireType(
    response: FhirQuestionnaireResponse,
    types: string[]
  ): boolean {
    const questionnaire = response.questionnaire?.toLowerCase() || '';
    return types.some((type) => questionnaire.includes(type));
  }

  /**
   * Check if questionnaire matches a specific assessment type
   */
  private matchesSpecificType(
    response: FhirQuestionnaireResponse,
    type: MentalHealthAssessmentType
  ): boolean {
    const questionnaire = response.questionnaire?.toLowerCase() || '';

    switch (type) {
      case 'PHQ-9':
        return this.QUESTIONNAIRE_TYPES.PHQ9.some((t) =>
          questionnaire.includes(t)
        );
      case 'PHQ-2':
        return this.QUESTIONNAIRE_TYPES.PHQ2.some((t) =>
          questionnaire.includes(t)
        );
      case 'GAD-7':
        return this.QUESTIONNAIRE_TYPES.GAD7.some((t) =>
          questionnaire.includes(t)
        );
      default:
        return false;
    }
  }

  /**
   * Parse FHIR QuestionnaireResponse to simplified structure
   */
  private parseQuestionnaireResponse(
    response: FhirQuestionnaireResponse
  ): ParsedQuestionnaireResponse {
    const items = this.parseQuestionnaireItems(response.item || []);
    const questionnaireType = this.detectQuestionnaireType(response);

    const parsed: ParsedQuestionnaireResponse = {
      id: response.id || '',
      questionnaireType,
      authoredDate: response.authored
        ? new Date(response.authored)
        : new Date(),
      status: response.status || 'completed',
      items,
    };

    // Calculate score for known questionnaire types
    if (this.matchesQuestionnaireType(response, this.QUESTIONNAIRE_TYPES.PHQ9)) {
      parsed.totalScore = this.calculatePHQ9Score(parsed);
    } else if (
      this.matchesQuestionnaireType(response, this.QUESTIONNAIRE_TYPES.GAD7)
    ) {
      parsed.totalScore = this.calculateGAD7Score(parsed);
    }

    return parsed;
  }

  /**
   * Parse questionnaire items recursively
   */
  private parseQuestionnaireItems(
    items: FhirQuestionnaireResponseItem[]
  ): ParsedQuestionnaireItem[] {
    const parsed: ParsedQuestionnaireItem[] = [];

    for (const item of items) {
      if (item.answer && item.answer.length > 0) {
        const answer = item.answer[0];
        parsed.push({
          linkId: item.linkId,
          text: item.text || '',
          answer: this.extractAnswer(answer),
          answerDisplay: this.extractAnswerDisplay(answer),
        });
      }

      // Recursively parse nested items
      if (item.item) {
        parsed.push(...this.parseQuestionnaireItems(item.item));
      }
    }

    return parsed;
  }

  /**
   * Extract answer value from FHIR answer
   */
  private extractAnswer(answer: any): string | number | boolean {
    if (answer.valueInteger !== undefined) return answer.valueInteger;
    if (answer.valueDecimal !== undefined) return answer.valueDecimal;
    if (answer.valueBoolean !== undefined) return answer.valueBoolean;
    if (answer.valueString !== undefined) return answer.valueString;
    if (answer.valueCoding) return answer.valueCoding.code || '';
    if (answer.valueQuantity) return answer.valueQuantity.value || 0;
    return '';
  }

  /**
   * Extract display text from FHIR answer
   */
  private extractAnswerDisplay(answer: any): string | undefined {
    if (answer.valueCoding?.display) return answer.valueCoding.display;
    return undefined;
  }

  /**
   * Detect questionnaire type from response
   */
  private detectQuestionnaireType(response: FhirQuestionnaireResponse): string {
    const questionnaire = response.questionnaire?.toLowerCase() || '';

    if (this.QUESTIONNAIRE_TYPES.PHQ9.some((t) => questionnaire.includes(t))) {
      return 'PHQ-9';
    }
    if (this.QUESTIONNAIRE_TYPES.PHQ2.some((t) => questionnaire.includes(t))) {
      return 'PHQ-2';
    }
    if (this.QUESTIONNAIRE_TYPES.GAD7.some((t) => questionnaire.includes(t))) {
      return 'GAD-7';
    }
    if (this.QUESTIONNAIRE_TYPES.PRAPARE.some((t) => questionnaire.includes(t))) {
      return 'PRAPARE';
    }
    if (this.QUESTIONNAIRE_TYPES.AHC_HRSN.some((t) => questionnaire.includes(t))) {
      return 'AHC-HRSN';
    }
    if (this.QUESTIONNAIRE_TYPES.FUNCTIONAL.some((t) => questionnaire.includes(t))) {
      return 'Functional Status';
    }

    return 'Unknown';
  }
}
