import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, map, timeout } from 'rxjs/operators';
import { API_CONFIG, CQL_ENGINE_ENDPOINTS } from '../config/api.config';

// Timeout constants for CQL operations
const CQL_VALIDATION_TIMEOUT_MS = 30000; // 30 seconds for validation
const CQL_EVALUATION_TIMEOUT_MS = 120000; // 2 minutes for single evaluation
const CQL_BATCH_EVALUATION_TIMEOUT_MS = 300000; // 5 minutes for batch evaluation

/**
 * Value Set interface matching backend ValueSet entity
 */
export interface ValueSet {
  id: string;
  tenantId: string;
  oid: string;
  name: string;
  version: string;
  codeSystem: string;
  codes: string; // JSON array of codes
  description?: string;
  publisher?: string;
  status: 'DRAFT' | 'ACTIVE' | 'RETIRED';
  active: boolean;
  createdAt: string;
  updatedAt?: string;
}

/**
 * Value set with parsed codes for UI display
 */
export interface ValueSetDisplay {
  id: string;
  name: string;
  oid: string;
  category: string;
  version: string;
  codeCount: number;
  codeSystem: string;
  selected?: boolean;
}

/**
 * CQL Validation result
 */
export interface CqlValidationResult {
  valid: boolean;
  errors: CqlValidationError[];
  warnings: CqlValidationWarning[];
  compiledElm?: string;
}

export interface CqlValidationError {
  line: number;
  column: number;
  message: string;
  severity: 'error' | 'critical';
}

export interface CqlValidationWarning {
  line: number;
  column: number;
  message: string;
}

/**
 * CQL Evaluation request
 */
export interface CqlEvaluationRequest {
  libraryId?: string;
  cqlContent?: string;
  patientId: string;
  parameters?: Record<string, any>;
}

/**
 * CQL Evaluation result
 */
export interface CqlEvaluationResult {
  evaluationId: string;
  libraryId: string;
  patientId: string;
  status: 'SUCCESS' | 'FAILED' | 'PENDING';
  evaluationResult?: any;
  errorMessage?: string;
  durationMs: number;
  evaluationDate: string;
}

@Injectable({
  providedIn: 'root',
})
export class CqlEngineService {
  constructor(private http: HttpClient) {}

  /**
   * Get all value sets
   */
  listValueSets(
    tenantId: string = API_CONFIG.DEFAULT_TENANT_ID
  ): Observable<ValueSetDisplay[]> {
    const url = `${API_CONFIG.CQL_ENGINE_URL}/api/v1/value-sets`;
    const headers = new HttpHeaders({
      'X-Tenant-ID': tenantId,
    });

    return this.http.get<ValueSet[]>(url, { headers }).pipe(
      map((valueSets) =>
        valueSets.map((vs) => this.mapToDisplay(vs))
      ),
      catchError(() => {
        // Return sample data if backend unavailable
        return of(this.getSampleValueSets());
      })
    );
  }

  /**
   * Search value sets by name or OID
   */
  searchValueSets(
    query: string,
    category?: string,
    tenantId: string = API_CONFIG.DEFAULT_TENANT_ID
  ): Observable<ValueSetDisplay[]> {
    const url = `${API_CONFIG.CQL_ENGINE_URL}/api/v1/value-sets/search`;
    const headers = new HttpHeaders({
      'X-Tenant-ID': tenantId,
    });
    let params = new HttpParams().set('query', query);
    if (category) {
      params = params.set('category', category);
    }

    return this.http.get<ValueSet[]>(url, { headers, params }).pipe(
      map((valueSets) =>
        valueSets.map((vs) => this.mapToDisplay(vs))
      ),
      catchError(() => {
        // Filter sample data if backend unavailable
        const samples = this.getSampleValueSets();
        return of(
          samples.filter(
            (vs) =>
              vs.name.toLowerCase().includes(query.toLowerCase()) ||
              vs.oid.toLowerCase().includes(query.toLowerCase())
          )
        );
      })
    );
  }

  /**
   * Get value set by OID
   */
  getValueSetByOid(
    oid: string,
    tenantId: string = API_CONFIG.DEFAULT_TENANT_ID
  ): Observable<ValueSetDisplay | null> {
    const url = `${API_CONFIG.CQL_ENGINE_URL}/api/v1/value-sets/by-oid/${encodeURIComponent(oid)}`;
    const headers = new HttpHeaders({
      'X-Tenant-ID': tenantId,
    });

    return this.http.get<ValueSet>(url, { headers }).pipe(
      map((vs) => this.mapToDisplay(vs)),
      catchError(() => of(null))
    );
  }

  /**
   * Validate CQL syntax and compile to ELM
   * Includes timeout protection to prevent hanging requests
   */
  validateCql(
    cqlContent: string,
    tenantId: string = API_CONFIG.DEFAULT_TENANT_ID
  ): Observable<CqlValidationResult> {
    const url = `${API_CONFIG.CQL_ENGINE_URL}/api/v1/cql/validate`;
    const headers = new HttpHeaders({
      'X-Tenant-ID': tenantId,
    });

    return this.http.post<CqlValidationResult>(url, { cqlContent }, { headers }).pipe(
      timeout(CQL_VALIDATION_TIMEOUT_MS),
      catchError((err) => {
        // Handle timeout specifically
        const errorMessage = err.name === 'TimeoutError'
          ? `Validation timed out after ${CQL_VALIDATION_TIMEOUT_MS / 1000} seconds`
          : `Validation service unavailable: ${err.message || 'Unknown error'}`;
        return of({
          valid: false,
          errors: [
            {
              line: 1,
              column: 1,
              message: errorMessage,
              severity: 'error' as const,
            },
          ],
          warnings: [],
        });
      })
    );
  }

  /**
   * Evaluate CQL against a patient
   * Includes timeout protection to prevent hanging requests
   */
  evaluateCql(
    request: CqlEvaluationRequest,
    tenantId: string = API_CONFIG.DEFAULT_TENANT_ID
  ): Observable<CqlEvaluationResult> {
    const url = `${API_CONFIG.CQL_ENGINE_URL}${CQL_ENGINE_ENDPOINTS.EVALUATIONS}`;
    const headers = new HttpHeaders({
      'X-Tenant-ID': tenantId,
    });

    return this.http.post<CqlEvaluationResult>(url, request, { headers }).pipe(
      timeout(CQL_EVALUATION_TIMEOUT_MS),
      catchError((err) => {
        if (err.name === 'TimeoutError') {
          return throwError(() => new Error(
            `CQL evaluation timed out after ${CQL_EVALUATION_TIMEOUT_MS / 1000} seconds`
          ));
        }
        return throwError(() => err);
      })
    );
  }

  /**
   * Batch evaluate CQL against multiple patients
   * Includes timeout protection to prevent hanging requests
   */
  batchEvaluateCql(
    libraryId: string,
    patientIds: string[],
    tenantId: string = API_CONFIG.DEFAULT_TENANT_ID
  ): Observable<CqlEvaluationResult[]> {
    const url = `${API_CONFIG.CQL_ENGINE_URL}${CQL_ENGINE_ENDPOINTS.EVALUATIONS_BATCH}`;
    const headers = new HttpHeaders({
      'X-Tenant-ID': tenantId,
    });

    return this.http.post<CqlEvaluationResult[]>(
      url,
      { libraryId, patientIds },
      { headers }
    ).pipe(
      timeout(CQL_BATCH_EVALUATION_TIMEOUT_MS),
      catchError((err) => {
        if (err.name === 'TimeoutError') {
          return throwError(() => new Error(
            `Batch CQL evaluation timed out after ${CQL_BATCH_EVALUATION_TIMEOUT_MS / 1000} seconds`
          ));
        }
        return throwError(() => err);
      })
    );
  }

  /**
   * Map backend ValueSet to display format
   */
  private mapToDisplay(vs: ValueSet): ValueSetDisplay {
    let codeCount = 0;
    try {
      const codes = JSON.parse(vs.codes || '[]');
      codeCount = Array.isArray(codes) ? codes.length : 0;
    } catch {
      codeCount = 0;
    }

    // Map code system to category
    const categoryMap: Record<string, string> = {
      'http://snomed.info/sct': 'Diagnoses',
      'http://loinc.org': 'Laboratory',
      'http://www.nlm.nih.gov/research/umls/rxnorm': 'Medications',
      'http://www.ama-assn.org/go/cpt': 'Procedures',
      'http://hl7.org/fhir/sid/icd-10-cm': 'Diagnoses',
    };

    return {
      id: vs.id,
      name: vs.name,
      oid: vs.oid,
      category: categoryMap[vs.codeSystem] || 'Other',
      version: vs.version,
      codeCount,
      codeSystem: vs.codeSystem,
    };
  }

  /**
   * Sample value sets for fallback when backend unavailable
   */
  private getSampleValueSets(): ValueSetDisplay[] {
    return [
      { id: '1', name: 'Diabetes Diagnosis Codes', oid: '2.16.840.1.113883.3.464.1003.103.12.1001', category: 'Diagnoses', version: '2024', codeCount: 45, codeSystem: 'SNOMED' },
      { id: '2', name: 'HbA1c Laboratory Test', oid: '2.16.840.1.113883.3.464.1003.198.12.1013', category: 'Laboratory', version: '2024', codeCount: 12, codeSystem: 'LOINC' },
      { id: '3', name: 'Annual Wellness Visit', oid: '2.16.840.1.113883.3.526.3.1240', category: 'Procedures', version: '2024', codeCount: 8, codeSystem: 'CPT' },
      { id: '4', name: 'Blood Pressure Measurement', oid: '2.16.840.1.113883.3.526.3.1032', category: 'Vitals', version: '2024', codeCount: 6, codeSystem: 'LOINC' },
      { id: '5', name: 'Statin Medications', oid: '2.16.840.1.113883.3.526.3.1572', category: 'Medications', version: '2024', codeCount: 87, codeSystem: 'RxNorm' },
      { id: '6', name: 'Hypertension Diagnosis', oid: '2.16.840.1.113883.3.526.3.1003', category: 'Diagnoses', version: '2024', codeCount: 34, codeSystem: 'SNOMED' },
      { id: '7', name: 'BMI Measurement', oid: '2.16.840.1.113883.3.600.1.681', category: 'Vitals', version: '2024', codeCount: 4, codeSystem: 'LOINC' },
      { id: '8', name: 'Colonoscopy Procedure', oid: '2.16.840.1.113883.3.464.1003.108.12.1020', category: 'Procedures', version: '2024', codeCount: 18, codeSystem: 'CPT' },
      { id: '9', name: 'Influenza Vaccine', oid: '2.16.840.1.113883.3.526.3.1254', category: 'Medications', version: '2024', codeCount: 15, codeSystem: 'CVX' },
      { id: '10', name: 'Mammography Screening', oid: '2.16.840.1.113883.3.464.1003.108.12.1018', category: 'Procedures', version: '2024', codeCount: 9, codeSystem: 'CPT' },
      { id: '11', name: 'Breast Cancer Diagnosis', oid: '2.16.840.1.113883.3.464.1003.108.12.1001', category: 'Diagnoses', version: '2024', codeCount: 28, codeSystem: 'SNOMED' },
      { id: '12', name: 'Colorectal Cancer Diagnosis', oid: '2.16.840.1.113883.3.464.1003.108.12.1002', category: 'Diagnoses', version: '2024', codeCount: 32, codeSystem: 'SNOMED' },
      { id: '13', name: 'Lipid Panel', oid: '2.16.840.1.113883.3.464.1003.198.12.1016', category: 'Laboratory', version: '2024', codeCount: 8, codeSystem: 'LOINC' },
      { id: '14', name: 'Depression Screening', oid: '2.16.840.1.113883.3.600.145.10', category: 'Procedures', version: '2024', codeCount: 12, codeSystem: 'LOINC' },
      { id: '15', name: 'ACE Inhibitors', oid: '2.16.840.1.113883.3.526.3.1573', category: 'Medications', version: '2024', codeCount: 42, codeSystem: 'RxNorm' },
    ];
  }
}
