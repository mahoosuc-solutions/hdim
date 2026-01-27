import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, forkJoin, throwError } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { ApiService } from './api.service';
import { LoggerService } from '../logger.service';
import {
  API_CONFIG,
  FHIR_ENDPOINTS,
  buildFhirUrl,
} from '../config/api.config';

/**
 * FHIR Service - Handles FHIR R4 resource operations
 *
 * Features:
 * - FHIR resource CRUD operations
 * - Resource transformation and validation
 * - Bundle import/export
 * - Code system mapping (LOINC, SNOMED, RxNorm)
 * - Search parameters support
 */
@Injectable({
  providedIn: 'root',
})
export class FhirService {
  private readonly baseUrl = API_CONFIG.FHIR_SERVER_URL;
  private readonly logger: any;

  constructor(
    private http: HttpClient,
    private apiService: ApiService,
    private loggerService: LoggerService
  ) {
    this.logger = this.loggerService.withContext(\'FhirService');}

  // ==================== Observation Operations ====================

  /**
   * Get all observations for a patient
   */
  getObservations(patientId: string, params?: ObservationSearchParams): Observable<Observation[]> {
    let httpParams = new HttpParams().set('patient', patientId);

    if (params?.category) {
      httpParams = httpParams.set('category', params.category);
    }
    if (params?.code) {
      httpParams = httpParams.set('code', params.code);
    }
    if (params?.date) {
      httpParams = httpParams.set('date', params.date);
    }
    if (params?._count) {
      httpParams = httpParams.set('_count', params._count.toString());
    }

    const url = buildFhirUrl(FHIR_ENDPOINTS.OBSERVATION);

    return this.apiService.get<FhirBundle<Observation>>(url, httpParams).pipe(
      map((bundle) => this.extractResources(bundle)),
      catchError((error) => {
        this.logger.error('Error fetching observations for patient', { patientId, error });
        return throwError(() => error);
      })
    );
  }

  /**
   * Create a new observation
   */
  createObservation(observation: Observation): Observable<Observation> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.OBSERVATION);
    return this.apiService.post<Observation>(url, observation);
  }

  // ==================== Condition Operations ====================

  /**
   * Get all conditions for a patient
   */
  getConditions(patientId: string, params?: ConditionSearchParams): Observable<Condition[]> {
    let httpParams = new HttpParams().set('patient', patientId);

    if (params?.clinicalStatus) {
      httpParams = httpParams.set('clinical-status', params.clinicalStatus);
    }
    if (params?.category) {
      httpParams = httpParams.set('category', params.category);
    }
    if (params?.code) {
      httpParams = httpParams.set('code', params.code);
    }

    const url = buildFhirUrl(FHIR_ENDPOINTS.CONDITION);

    return this.apiService.get<FhirBundle<Condition>>(url, httpParams).pipe(
      map((bundle) => this.extractResources(bundle)),
      catchError((error) => {
        this.logger.error('Error fetching conditions for patient', { patientId, error });
        return throwError(() => error);
      })
    );
  }

  /**
   * Create a new condition
   */
  createCondition(condition: Condition): Observable<Condition> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.CONDITION);
    return this.apiService.post<Condition>(url, condition);
  }

  // ==================== Medication Request Operations ====================

  /**
   * Get all medication requests for a patient
   */
  getMedicationRequests(
    patientId: string,
    params?: MedicationSearchParams
  ): Observable<MedicationRequest[]> {
    let httpParams = new HttpParams().set('patient', patientId);

    if (params?.status) {
      httpParams = httpParams.set('status', params.status);
    }
    if (params?.medication) {
      httpParams = httpParams.set('medication', params.medication);
    }

    const url = buildFhirUrl(FHIR_ENDPOINTS.MEDICATION);

    return this.apiService.get<FhirBundle<MedicationRequest>>(url, httpParams).pipe(
      map((bundle) => this.extractResources(bundle)),
      catchError((error) => {
        this.logger.error('Error fetching medication requests for patient', { patientId, error });
        return throwError(() => error);
      })
    );
  }

  /**
   * Create a new medication request
   */
  createMedicationRequest(medicationRequest: MedicationRequest): Observable<MedicationRequest> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.MEDICATION);
    return this.apiService.post<MedicationRequest>(url, medicationRequest);
  }

  // ==================== Procedure Operations ====================

  /**
   * Get all procedures for a patient
   */
  getProcedures(patientId: string, params?: ProcedureSearchParams): Observable<Procedure[]> {
    let httpParams = new HttpParams().set('patient', patientId);

    if (params?.status) {
      httpParams = httpParams.set('status', params.status);
    }
    if (params?.code) {
      httpParams = httpParams.set('code', params.code);
    }
    if (params?.date) {
      httpParams = httpParams.set('date', params.date);
    }

    const url = buildFhirUrl(FHIR_ENDPOINTS.PROCEDURE);

    return this.apiService.get<FhirBundle<Procedure>>(url, httpParams).pipe(
      map((bundle) => this.extractResources(bundle)),
      catchError((error) => {
        this.logger.error('Error fetching procedures for patient', { patientId, error });
        return throwError(() => error);
      })
    );
  }

  // ==================== Bundle Operations ====================

  /**
   * Import FHIR resources from a bundle
   */
  importFhirResources(bundle: FhirBundle): Observable<ImportResult> {
    const url = buildFhirUrl('');
    return this.apiService.post<ImportResult>(url, bundle).pipe(
      catchError((error) => {
        this.logger.error('Error importing FHIR resources', { error });
        return throwError(() => error);
      })
    );
  }

  /**
   * Export patient data as a FHIR bundle
   */
  exportPatientAsBundle(patientId: string): Observable<FhirBundle> {
    return forkJoin({
      observations: this.getObservations(patientId),
      conditions: this.getConditions(patientId),
      medications: this.getMedicationRequests(patientId),
      procedures: this.getProcedures(patientId),
    }).pipe(
      map((data) => this.createBundle([
        ...data.observations,
        ...data.conditions,
        ...data.medications,
        ...data.procedures,
      ])),
      catchError((error) => {
        this.logger.error('Error exporting patient as bundle', { patientId, error });
        return throwError(() => error);
      })
    );
  }

  // ==================== Helper Methods ====================

  /**
   * Extract resources from FHIR bundle
   */
  private extractResources<T extends FhirResource>(bundle: FhirBundle<T>): T[] {
    if (!bundle.entry || bundle.entry.length === 0) {
      return [];
    }
    return bundle.entry.map((entry) => entry.resource).filter((r) => r !== undefined) as T[];
  }

  /**
   * Create a FHIR bundle from resources
   */
  private createBundle<T extends FhirResource>(resources: T[]): FhirBundle<T> {
    return {
      resourceType: 'Bundle',
      type: 'collection',
      total: resources.length,
      entry: resources.map((resource) => ({
        resource,
        fullUrl: `${this.baseUrl}/${resource.resourceType}/${resource.id}`,
      })),
    };
  }

  /**
   * Validate a FHIR resource
   */
  validateResource<T extends FhirResource>(resource: T): ValidationResult {
    const errors: string[] = [];

    if (!resource.resourceType) {
      errors.push('resourceType is required');
    }

    if (!resource.id && resource.resourceType !== 'Bundle') {
      errors.push('id is required for non-Bundle resources');
    }

    return {
      valid: errors.length === 0,
      errors,
    };
  }

  /**
   * Map code system display name
   */
  getCodeSystemDisplay(system: string): string {
    const codeSystems: Record<string, string> = {
      'http://loinc.org': 'LOINC',
      'http://snomed.info/sct': 'SNOMED CT',
      'http://www.nlm.nih.gov/research/umls/rxnorm': 'RxNorm',
      'http://hl7.org/fhir/sid/icd-10': 'ICD-10',
      'http://hl7.org/fhir/sid/icd-9-cm': 'ICD-9-CM',
      'http://www.ama-assn.org/go/cpt': 'CPT',
    };

    return codeSystems[system] || system;
  }

  /**
   * Format coding for display
   */
  formatCoding(coding: Coding): string {
    if (!coding) return '';

    const system = this.getCodeSystemDisplay(coding.system || '');
    const code = coding.code || '';
    const display = coding.display || '';

    if (display) {
      return `${display} (${system}: ${code})`;
    }
    return `${system}: ${code}`;
  }

  /**
   * Extract primary coding from CodeableConcept
   */
  getPrimaryCoding(concept: CodeableConcept): Coding | undefined {
    if (!concept || !concept.coding || concept.coding.length === 0) {
      return undefined;
    }
    return concept.coding[0];
  }
}

// ==================== Type Definitions ====================

export interface FhirResource {
  resourceType: string;
  id?: string;
  meta?: Meta;
}

export interface Meta {
  versionId?: string;
  lastUpdated?: string;
  source?: string;
  profile?: string[];
}

export interface FhirBundle<T extends FhirResource = FhirResource> {
  resourceType: 'Bundle';
  type: 'document' | 'message' | 'transaction' | 'transaction-response' | 'batch' | 'batch-response' | 'history' | 'searchset' | 'collection';
  total?: number;
  entry?: BundleEntry<T>[];
}

export interface BundleEntry<T extends FhirResource = FhirResource> {
  fullUrl?: string;
  resource?: T;
  search?: { mode: string; score: number };
}

export interface Observation extends FhirResource {
  resourceType: 'Observation';
  status: 'registered' | 'preliminary' | 'final' | 'amended' | 'corrected' | 'cancelled' | 'entered-in-error' | 'unknown';
  category?: CodeableConcept[];
  code: CodeableConcept;
  subject: Reference;
  effectiveDateTime?: string;
  effectivePeriod?: Period;
  issued?: string;
  valueQuantity?: Quantity;
  valueCodeableConcept?: CodeableConcept;
  valueString?: string;
  valueBoolean?: boolean;
  interpretation?: CodeableConcept[];
  referenceRange?: ReferenceRange[];
}

export interface Condition extends FhirResource {
  resourceType: 'Condition';
  clinicalStatus?: CodeableConcept;
  verificationStatus?: CodeableConcept;
  category?: CodeableConcept[];
  severity?: CodeableConcept;
  code?: CodeableConcept;
  subject: Reference;
  onsetDateTime?: string;
  onsetPeriod?: Period;
  abatementDateTime?: string;
  recordedDate?: string;
}

export interface MedicationRequest extends FhirResource {
  resourceType: 'MedicationRequest';
  status: 'active' | 'on-hold' | 'cancelled' | 'completed' | 'entered-in-error' | 'stopped' | 'draft' | 'unknown';
  intent: 'proposal' | 'plan' | 'order' | 'original-order' | 'reflex-order' | 'filler-order' | 'instance-order' | 'option';
  medicationCodeableConcept?: CodeableConcept;
  medicationReference?: Reference;
  subject: Reference;
  authoredOn?: string;
  requester?: Reference;
  dosageInstruction?: Dosage[];
}

export interface Procedure extends FhirResource {
  resourceType: 'Procedure';
  status: 'preparation' | 'in-progress' | 'not-done' | 'on-hold' | 'stopped' | 'completed' | 'entered-in-error' | 'unknown';
  code?: CodeableConcept;
  subject: Reference;
  performedDateTime?: string;
  performedPeriod?: Period;
  recorder?: Reference;
  performer?: ProcedurePerformer[];
}

export interface CodeableConcept {
  coding?: Coding[];
  text?: string;
}

export interface Coding {
  system?: string;
  version?: string;
  code?: string;
  display?: string;
  userSelected?: boolean;
}

export interface Reference {
  reference?: string;
  type?: string;
  identifier?: Identifier;
  display?: string;
}

export interface Identifier {
  use?: string;
  type?: CodeableConcept;
  system?: string;
  value?: string;
}

export interface Quantity {
  value?: number;
  comparator?: '<' | '<=' | '>=' | '>';
  unit?: string;
  system?: string;
  code?: string;
}

export interface Period {
  start?: string;
  end?: string;
}

export interface ReferenceRange {
  low?: Quantity;
  high?: Quantity;
  type?: CodeableConcept;
  appliesTo?: CodeableConcept[];
  age?: Range;
  text?: string;
}

export interface Range {
  low?: Quantity;
  high?: Quantity;
}

export interface Dosage {
  sequence?: number;
  text?: string;
  timing?: Timing;
  route?: CodeableConcept;
  doseAndRate?: DoseAndRate[];
}

export interface Timing {
  repeat?: TimingRepeat;
  code?: CodeableConcept;
}

export interface TimingRepeat {
  frequency?: number;
  period?: number;
  periodUnit?: string;
}

export interface DoseAndRate {
  type?: CodeableConcept;
  doseQuantity?: Quantity;
  rateQuantity?: Quantity;
}

export interface ProcedurePerformer {
  function?: CodeableConcept;
  actor: Reference;
}

export interface ImportResult {
  success: boolean;
  resourcesCreated: number;
  resourcesUpdated: number;
  errors: string[];
}

export interface ValidationResult {
  valid: boolean;
  errors: string[];
}

export interface ObservationSearchParams {
  category?: string;
  code?: string;
  date?: string;
  _count?: number;
}

export interface ConditionSearchParams {
  clinicalStatus?: string;
  category?: string;
  code?: string;
}

export interface MedicationSearchParams {
  status?: string;
  medication?: string;
}

export interface ProcedureSearchParams {
  status?: string;
  code?: string;
  date?: string;
}
