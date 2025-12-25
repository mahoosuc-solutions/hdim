import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { CacheableService, conceptContainsAnyCode } from '../shared';
import { LoggerService, ContextualLogger } from '../logger.service';
import {
  API_CONFIG,
  FHIR_ENDPOINTS,
  buildFhirUrl,
  HTTP_HEADERS,
} from '../../config/api.config';
import { FhirBundle, FhirCondition } from '../../models/fhir.model';
import { ChronicCondition, RiskLevel } from '../../models/patient-health.model';

/**
 * FHIR Condition Service
 *
 * Handles all FHIR Condition resource queries including:
 * - Active conditions
 * - Chronic disease conditions
 * - Mental health conditions
 * - Condition history
 *
 * Features:
 * - Built-in caching with 5-minute TTL
 * - Condition severity mapping
 * - Clinical status filtering
 */
@Injectable({
  providedIn: 'root',
})
export class FhirConditionService extends CacheableService {
  private log: ContextualLogger;

  // SNOMED CT codes for condition categories
  private readonly MENTAL_HEALTH_SNOMED_CODES = [
    '74732009', // Mental disorder
    '35489007', // Depressive disorder
    '197480006', // Anxiety disorder
    '13746004', // Bipolar disorder
    '58214004', // Schizophrenia
    '66347000', // Panic disorder
    '47505003', // Posttraumatic stress disorder
  ];

  private readonly CHRONIC_DISEASE_SNOMED_CODES = [
    '73211009', // Diabetes mellitus
    '38341003', // Hypertensive disorder
    '13645005', // Chronic obstructive pulmonary disease
    '84114007', // Heart failure
    '709044004', // Chronic kidney disease
    '195967001', // Asthma
    '396275006', // Osteoarthritis
  ];

  constructor(
    private http: HttpClient,
    private logger: LoggerService
  ) {
    super({ ttlMs: 5 * 60 * 1000 }); // 5 minute cache
    this.log = this.logger.withContext('FhirConditionService');
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
   * Get all active conditions for a patient
   */
  getActiveConditions(patientId: string): Observable<ChronicCondition[]> {
    const cacheKey = `conditions:active:${patientId}`;
    const cached = this.getCached<ChronicCondition[]>(cacheKey);
    if (cached) return of(cached);

    const url = buildFhirUrl(FHIR_ENDPOINTS.CONDITION);
    const params = new HttpParams()
      .set('patient', patientId)
      .set('clinical-status', 'active')
      .set('_sort', '-onset-date')
      .set('_count', '100');

    return this.http
      .get<FhirBundle<FhirCondition>>(url, {
        headers: this.getHeaders(),
        params,
      })
      .pipe(
        map((bundle) => {
          const conditions = this.mapFhirConditions(bundle);
          this.setCache(cacheKey, conditions);
          return conditions;
        }),
        catchError((error) => {
          this.log.error('Error fetching conditions from FHIR:', error);
          return of([]);
        })
      );
  }

  /**
   * Get chronic disease conditions
   */
  getChronicConditions(patientId: string): Observable<ChronicCondition[]> {
    return this.getActiveConditions(patientId).pipe(
      map((conditions) =>
        conditions.filter((c) => this.isChronicCondition(c.code))
      )
    );
  }

  /**
   * Get mental health conditions
   */
  getMentalHealthConditions(patientId: string): Observable<ChronicCondition[]> {
    const cacheKey = `conditions:mental:${patientId}`;
    const cached = this.getCached<ChronicCondition[]>(cacheKey);
    if (cached) return of(cached);

    return this.getActiveConditions(patientId).pipe(
      map((conditions) => {
        const mentalHealth = conditions.filter((c) =>
          this.isMentalHealthCondition(c.code)
        );
        this.setCache(cacheKey, mentalHealth);
        return mentalHealth;
      })
    );
  }

  /**
   * Get condition history (all conditions including resolved)
   */
  getConditionHistory(patientId: string): Observable<ChronicCondition[]> {
    const cacheKey = `conditions:history:${patientId}`;
    const cached = this.getCached<ChronicCondition[]>(cacheKey);
    if (cached) return of(cached);

    const url = buildFhirUrl(FHIR_ENDPOINTS.CONDITION);
    const params = new HttpParams()
      .set('patient', patientId)
      .set('_sort', '-onset-date')
      .set('_count', '200');

    return this.http
      .get<FhirBundle<FhirCondition>>(url, {
        headers: this.getHeaders(),
        params,
      })
      .pipe(
        map((bundle) => {
          const conditions = this.mapFhirConditions(bundle);
          this.setCache(cacheKey, conditions);
          return conditions;
        }),
        catchError((error) => {
          this.log.error('Error fetching condition history from FHIR:', error);
          return of([]);
        })
      );
  }

  /**
   * Get conditions by category (SNOMED code)
   */
  getConditionsByCategory(
    patientId: string,
    snomedCodes: string[]
  ): Observable<ChronicCondition[]> {
    return this.getActiveConditions(patientId).pipe(
      map((conditions) =>
        conditions.filter((c) => conceptContainsAnyCode(c.code, snomedCodes))
      )
    );
  }

  /**
   * Invalidate condition cache for a patient
   */
  invalidatePatientConditions(patientId: string): void {
    this.invalidatePatientCache(patientId);
  }

  // ===== Private Helper Methods =====

  /**
   * Map FHIR Conditions to ChronicCondition objects
   */
  private mapFhirConditions(bundle: FhirBundle<FhirCondition>): ChronicCondition[] {
    if (!bundle.entry || bundle.entry.length === 0) {
      return [];
    }

    return bundle.entry
      .map((entry) => entry.resource)
      .filter((condition): condition is FhirCondition => condition !== undefined)
      .map((condition) => this.mapFhirCondition(condition));
  }

  /**
   * Map a single FHIR Condition to ChronicCondition
   */
  private mapFhirCondition(condition: FhirCondition): ChronicCondition {
    const coding = condition.code?.coding?.[0];

    return {
      id: condition.id || '',
      name: condition.code?.text || coding?.display || 'Unknown Condition',
      code: coding?.code || '',
      system: coding?.system || '',
      onsetDate: condition.onsetDateTime
        ? new Date(condition.onsetDateTime)
        : undefined,
      status: this.mapClinicalStatus(condition.clinicalStatus?.coding?.[0]?.code),
      severity: this.mapSeverity(condition.severity?.coding?.[0]?.code),
      riskLevel: this.calculateConditionRiskLevel(condition),
      isControlled: this.determineControlStatus(condition),
      lastAssessment: condition.recordedDate
        ? new Date(condition.recordedDate)
        : undefined,
    };
  }

  /**
   * Map clinical status from FHIR
   */
  private mapClinicalStatus(
    status?: string
  ): 'active' | 'recurrence' | 'relapse' | 'inactive' | 'remission' | 'resolved' {
    switch (status) {
      case 'active':
        return 'active';
      case 'recurrence':
        return 'recurrence';
      case 'relapse':
        return 'relapse';
      case 'inactive':
        return 'inactive';
      case 'remission':
        return 'remission';
      case 'resolved':
        return 'resolved';
      default:
        return 'active';
    }
  }

  /**
   * Map severity from FHIR
   */
  private mapSeverity(severity?: string): 'mild' | 'moderate' | 'severe' {
    switch (severity?.toLowerCase()) {
      case 'mild':
      case '255604002': // SNOMED CT mild
        return 'mild';
      case 'moderate':
      case '6736007': // SNOMED CT moderate
        return 'moderate';
      case 'severe':
      case '24484000': // SNOMED CT severe
        return 'severe';
      default:
        return 'moderate';
    }
  }

  /**
   * Calculate risk level based on condition
   */
  private calculateConditionRiskLevel(condition: FhirCondition): RiskLevel {
    const severity = condition.severity?.coding?.[0]?.code?.toLowerCase();
    const status = condition.clinicalStatus?.coding?.[0]?.code;

    if (severity === 'severe' || severity === '24484000') {
      return 'high';
    }

    if (status === 'active' && severity === 'moderate') {
      return 'moderate';
    }

    if (status === 'remission' || status === 'resolved') {
      return 'low';
    }

    return 'moderate';
  }

  /**
   * Determine if condition is controlled
   */
  private determineControlStatus(condition: FhirCondition): boolean {
    const status = condition.clinicalStatus?.coding?.[0]?.code;
    return status === 'remission' || status === 'inactive';
  }

  /**
   * Check if condition is a chronic disease
   */
  private isChronicCondition(code: string): boolean {
    if (!code) return false;
    return this.CHRONIC_DISEASE_SNOMED_CODES.some(chronicCode =>
      code === chronicCode || code.includes(chronicCode)
    );
  }

  /**
   * Check if condition is mental health related
   */
  private isMentalHealthCondition(code: string): boolean {
    if (!code) return false;
    return this.MENTAL_HEALTH_SNOMED_CODES.some(mhCode =>
      code === mhCode || code.includes(mhCode)
    );
  }
}
