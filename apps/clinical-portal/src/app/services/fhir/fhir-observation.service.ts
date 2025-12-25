import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, of, EMPTY } from 'rxjs';
import { map, catchError, expand, reduce } from 'rxjs/operators';
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
  FhirObservation,
  FhirDiagnosticReport,
  LOINC_VITAL_SIGNS,
  OBSERVATION_CATEGORIES,
  FHIR_INTERPRETATION_CODES,
} from '../../models/fhir.model';
import {
  VitalSign,
  LabResult,
  LabInterpretation,
  PhysicalHealthSummary,
} from '../../models/patient-health.model';

/**
 * FHIR Observation Service
 *
 * Handles all FHIR Observation resource queries including:
 * - Vital signs (blood pressure, heart rate, weight, etc.)
 * - Laboratory results (HbA1c, lipid panel, etc.)
 * - Diagnostic reports
 *
 * Features:
 * - Built-in caching with 5-minute TTL
 * - Automatic pagination handling
 * - LOINC code mapping
 * - Interpretation code mapping
 */
@Injectable({
  providedIn: 'root',
})
export class FhirObservationService extends CacheableService {
  private log: ContextualLogger;

  constructor(
    private http: HttpClient,
    private logger: LoggerService
  ) {
    super({ ttlMs: 5 * 60 * 1000 }); // 5 minute cache
    this.log = this.logger.withContext('FhirObservationService');
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
   * Get vital signs from FHIR Observation endpoint
   */
  getVitalSigns(patientId: string): Observable<PhysicalHealthSummary['vitals']> {
    const cacheKey = `vitals:${patientId}`;
    const cached = this.getCached<PhysicalHealthSummary['vitals']>(cacheKey);
    if (cached) return of(cached);

    const url = buildFhirUrl(FHIR_ENDPOINTS.OBSERVATION);
    const params = new HttpParams()
      .set('patient', patientId)
      .set('category', OBSERVATION_CATEGORIES.VITAL_SIGNS)
      .set('_sort', '-date')
      .set('_count', '100');

    return this.http
      .get<FhirBundle<FhirObservation>>(url, {
        headers: this.getHeaders(),
        params,
      })
      .pipe(
        map((bundle) => {
          const vitals = this.mapFhirObservationsToVitals(bundle);
          this.setCache(cacheKey, vitals);
          return vitals;
        }),
        catchError((error) => {
          this.log.error('Error fetching vital signs from FHIR:', error);
          return of({});
        })
      );
  }

  /**
   * Get lab results from FHIR Observation endpoint
   */
  getLabResults(
    patientId: string,
    options?: { followPagination?: boolean; count?: number }
  ): Observable<LabResult[]> {
    const cacheKey = `labs:${patientId}:${options?.followPagination || false}`;
    const cached = this.getCached<LabResult[]>(cacheKey);
    if (cached) return of(cached);

    const url = buildFhirUrl(FHIR_ENDPOINTS.OBSERVATION);
    const params = new HttpParams()
      .set('patient', patientId)
      .set('category', OBSERVATION_CATEGORIES.LABORATORY)
      .set('_sort', '-date')
      .set('_count', String(options?.count || 20));

    if (options?.followPagination) {
      return this.fetchAllPages<FhirObservation>(url, params).pipe(
        map((observations) => {
          const labs = observations.map((obs) => this.mapFhirObservationToLabResult(obs));
          this.setCache(cacheKey, labs);
          return labs;
        }),
        catchError((error) => {
          this.log.error('Error fetching lab results from FHIR:', error);
          return of([]);
        })
      );
    }

    return this.http
      .get<FhirBundle<FhirObservation>>(url, {
        headers: this.getHeaders(),
        params,
      })
      .pipe(
        map((bundle) => {
          const labs = (bundle.entry || [])
            .map((entry) => entry.resource)
            .filter((obs): obs is FhirObservation => obs !== undefined)
            .map((obs) => this.mapFhirObservationToLabResult(obs));
          this.setCache(cacheKey, labs);
          return labs;
        }),
        catchError((error) => {
          this.log.error('Error fetching lab results from FHIR:', error);
          return of([]);
        })
      );
  }

  /**
   * Get diagnostic reports from FHIR DiagnosticReport endpoint
   */
  getDiagnosticReports(patientId: string): Observable<any[]> {
    const cacheKey = `diagnostic:${patientId}`;
    const cached = this.getCached<any[]>(cacheKey);
    if (cached) return of(cached);

    const url = buildFhirUrl(FHIR_ENDPOINTS.DIAGNOSTIC_REPORT);
    const params = new HttpParams()
      .set('patient', patientId)
      .set('_sort', '-date')
      .set('_count', '20');

    return this.http
      .get<FhirBundle<FhirDiagnosticReport>>(url, {
        headers: this.getHeaders(),
        params,
      })
      .pipe(
        map((bundle) => {
          const reports = (bundle.entry || [])
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
          this.setCache(cacheKey, reports);
          return reports;
        }),
        catchError((error) => {
          this.log.error('Error fetching diagnostic reports from FHIR:', error);
          return of([]);
        })
      );
  }

  /**
   * Get vital sign history for a specific LOINC code
   */
  getVitalSignHistory(
    patientId: string,
    loincCode?: string,
    limit: number = 30
  ): Observable<VitalSign[]> {
    const cacheKey = `vital-history:${patientId}:${loincCode || 'all'}:${limit}`;
    const cached = this.getCached<VitalSign[]>(cacheKey);
    if (cached) return of(cached);

    const url = buildFhirUrl(FHIR_ENDPOINTS.OBSERVATION);
    let params = new HttpParams()
      .set('patient', patientId)
      .set('category', OBSERVATION_CATEGORIES.VITAL_SIGNS)
      .set('_sort', '-date')
      .set('_count', String(limit));

    if (loincCode) {
      params = params.set('code', `http://loinc.org|${loincCode}`);
    }

    return this.http
      .get<FhirBundle<FhirObservation>>(url, {
        headers: this.getHeaders(),
        params,
      })
      .pipe(
        map((bundle) => {
          const vitals = (bundle.entry || [])
            .map((entry) => entry.resource)
            .filter((obs): obs is FhirObservation => obs !== undefined)
            .map((obs) => this.mapObservationToVitalSign(obs));
          this.setCache(cacheKey, vitals);
          return vitals;
        }),
        catchError((error) => {
          this.log.error('Error fetching vital sign history:', error);
          return of([]);
        })
      );
  }

  /**
   * Get lab history for a specific LOINC code
   */
  getLabHistory(
    patientId: string,
    loincCode: string,
    limit: number = 10
  ): Observable<LabResult[]> {
    const cacheKey = `lab-history:${patientId}:${loincCode}:${limit}`;
    const cached = this.getCached<LabResult[]>(cacheKey);
    if (cached) return of(cached);

    const url = buildFhirUrl(FHIR_ENDPOINTS.OBSERVATION);
    const params = new HttpParams()
      .set('patient', patientId)
      .set('code', `http://loinc.org|${loincCode}`)
      .set('_sort', '-date')
      .set('_count', String(limit));

    return this.http
      .get<FhirBundle<FhirObservation>>(url, {
        headers: this.getHeaders(),
        params,
      })
      .pipe(
        map((bundle) => {
          const labs = (bundle.entry || [])
            .map((entry) => entry.resource)
            .filter((obs): obs is FhirObservation => obs !== undefined)
            .map((obs) => this.mapFhirObservationToLabResult(obs));
          this.setCache(cacheKey, labs);
          return labs;
        }),
        catchError((error) => {
          this.log.error('Error fetching lab history:', error);
          return of([]);
        })
      );
  }

  /**
   * Invalidate observation cache for a patient
   */
  invalidatePatientObservations(patientId: string): void {
    this.invalidatePatientCache(patientId);
  }

  // ===== Private Helper Methods =====

  /**
   * Fetch all pages of a FHIR bundle response
   */
  private fetchAllPages<T>(url: string, params: HttpParams): Observable<T[]> {
    return this.http
      .get<FhirBundle<T>>(url, {
        headers: this.getHeaders(),
        params,
      })
      .pipe(
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
  private mapFhirObservationsToVitals(
    bundle: FhirBundle<FhirObservation>
  ): PhysicalHealthSummary['vitals'] {
    const vitals: PhysicalHealthSummary['vitals'] = {};

    if (!bundle.entry || bundle.entry.length === 0) {
      return vitals;
    }

    const observations = bundle.entry
      .map((entry) => entry.resource)
      .filter((obs): obs is FhirObservation => obs !== undefined);

    for (const obs of observations) {
      const loincCode = obs.code.coding?.find(
        (c) => c.system === 'http://loinc.org'
      )?.code;

      if (!loincCode) continue;

      switch (loincCode) {
        case LOINC_VITAL_SIGNS.HEART_RATE:
          if (obs.valueQuantity?.value !== undefined) {
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
              vitals.bloodPressure = this.mapObservationToVitalSign(obs, bpValue);
              vitals.bloodPressure.unit = 'mmHg';
            }
          }
          break;

        case LOINC_VITAL_SIGNS.BODY_WEIGHT:
          if (obs.valueQuantity?.value !== undefined) {
            vitals.weight = this.mapObservationToVitalSign(obs, obs.valueQuantity.value);
          }
          break;

        case LOINC_VITAL_SIGNS.BMI:
          if (obs.valueQuantity?.value !== undefined) {
            vitals.bmi = this.mapObservationToVitalSign(obs, obs.valueQuantity.value);
          }
          break;

        case LOINC_VITAL_SIGNS.RESPIRATORY_RATE:
          if (obs.valueQuantity?.value !== undefined) {
            vitals.respiratoryRate = this.mapObservationToVitalSign(
              obs,
              obs.valueQuantity.value
            );
          }
          break;

        case LOINC_VITAL_SIGNS.OXYGEN_SATURATION:
          if (obs.valueQuantity?.value !== undefined) {
            vitals.oxygenSaturation = this.mapObservationToVitalSign(
              obs,
              obs.valueQuantity.value
            );
          }
          break;

        case LOINC_VITAL_SIGNS.BODY_TEMPERATURE:
          if (obs.valueQuantity?.value !== undefined) {
            vitals.temperature = this.mapObservationToVitalSign(
              obs,
              obs.valueQuantity.value
            );
          }
          break;
      }
    }

    return vitals;
  }

  /**
   * Map a single FHIR Observation to VitalSign
   */
  private mapObservationToVitalSign(obs: FhirObservation, value?: any): VitalSign {
    return {
      name: obs.code.text || obs.code.coding?.[0]?.display || 'Unknown',
      value: value ?? obs.valueQuantity?.value ?? 0,
      unit: obs.valueQuantity?.unit || '',
      date: obs.effectiveDateTime ? new Date(obs.effectiveDateTime) : new Date(),
      status: this.determineVitalStatus(obs),
      trend: 'stable',
    };
  }

  /**
   * Map FHIR Observation to LabResult
   */
  private mapFhirObservationToLabResult(obs: FhirObservation): LabResult {
    const interpretation = this.mapFhirInterpretationCode(
      obs.interpretation?.[0]?.coding?.[0]?.code
    );

    return {
      id: obs.id || '',
      name: obs.code.text || obs.code.coding?.[0]?.display || 'Unknown',
      code: obs.code.coding?.[0]?.code || '',
      value: obs.valueQuantity?.value ?? 0,
      unit: obs.valueQuantity?.unit || '',
      referenceRange: this.formatReferenceRange(obs.referenceRange),
      date: obs.effectiveDateTime ? new Date(obs.effectiveDateTime) : new Date(),
      interpretation,
      status: obs.status || 'final',
    };
  }

  /**
   * Format reference range from FHIR
   */
  private formatReferenceRange(range?: FhirObservation['referenceRange']): string {
    if (!range || range.length === 0) return '';

    const first = range[0];
    if (first.low && first.high) {
      return `${first.low.value}-${first.high.value} ${first.low.unit || ''}`.trim();
    }
    if (first.low) {
      return `>${first.low.value} ${first.low.unit || ''}`.trim();
    }
    if (first.high) {
      return `<${first.high.value} ${first.high.unit || ''}`.trim();
    }
    return first.text || '';
  }

  /**
   * Determine vital sign status based on observation
   */
  private determineVitalStatus(obs: FhirObservation): 'normal' | 'warning' | 'critical' {
    const interpretation = obs.interpretation?.[0]?.coding?.[0]?.code;

    if (!interpretation) return 'normal';

    switch (interpretation) {
      case FHIR_INTERPRETATION_CODES.NORMAL:
        return 'normal';
      case FHIR_INTERPRETATION_CODES.HIGH:
      case FHIR_INTERPRETATION_CODES.LOW:
        return 'warning';
      case FHIR_INTERPRETATION_CODES.CRITICAL_HIGH:
      case FHIR_INTERPRETATION_CODES.CRITICAL_LOW:
        return 'critical';
      default:
        return 'normal';
    }
  }

  /**
   * Map FHIR interpretation code to LabInterpretation
   */
  private mapFhirInterpretationCode(code?: string): LabInterpretation {
    if (!code) return 'normal';

    switch (code) {
      case FHIR_INTERPRETATION_CODES.NORMAL:
        return 'normal';
      case FHIR_INTERPRETATION_CODES.HIGH:
        return 'high';
      case FHIR_INTERPRETATION_CODES.LOW:
        return 'low';
      case FHIR_INTERPRETATION_CODES.CRITICAL_HIGH:
        return 'critical-high';
      case FHIR_INTERPRETATION_CODES.CRITICAL_LOW:
        return 'critical-low';
      case FHIR_INTERPRETATION_CODES.ABNORMAL:
        return 'abnormal';
      default:
        return 'normal';
    }
  }
}
