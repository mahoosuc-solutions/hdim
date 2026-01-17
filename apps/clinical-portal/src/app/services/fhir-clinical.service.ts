import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import {
  API_CONFIG,
  FHIR_ENDPOINTS,
  buildFhirUrl,
} from '../config/api.config';

/**
 * FHIR Clinical Data - Observation Resource
 */
export interface Observation {
  resourceType: 'Observation';
  id?: string;
  status: string;
  code: {
    coding?: Array<{
      system?: string;
      code?: string;
      display?: string;
    }>;
    text?: string;
  };
  subject: {
    reference: string;
  };
  effectiveDateTime?: string;
  valueQuantity?: {
    value: number;
    unit: string;
    system?: string;
  };
  valueString?: string;
  valueCodeableConcept?: {
    coding?: Array<{
      system?: string;
      code?: string;
      display?: string;
    }>;
    text?: string;
  };
}

/**
 * FHIR Clinical Data - Condition Resource
 */
export interface Condition {
  resourceType: 'Condition';
  id?: string;
  clinicalStatus?: {
    coding?: Array<{
      system?: string;
      code?: string;
      display?: string;
    }>;
  };
  code: {
    coding?: Array<{
      system?: string;
      code?: string;
      display?: string;
    }>;
    text?: string;
  };
  subject: {
    reference: string;
  };
  onsetDateTime?: string;
  recordedDate?: string;
}

/**
 * FHIR Clinical Data - Procedure Resource
 */
export interface Procedure {
  resourceType: 'Procedure';
  id?: string;
  status: string;
  code: {
    coding?: Array<{
      system?: string;
      code?: string;
      display?: string;
    }>;
    text?: string;
  };
  subject: {
    reference: string;
  };
  performedDateTime?: string;
}

/**
 * FHIR Bundle
 */
export interface FhirBundle<T> {
  resourceType: 'Bundle';
  type: string;
  total?: number;
  entry?: Array<{
    resource: T;
  }>;
}

/**
 * Patient Clinical Summary
 */
export interface PatientClinicalData {
  observations: Observation[];
  conditions: Condition[];
  procedures: Procedure[];
}

/**
 * FhirClinicalService - Handles FHIR clinical data resources
 * Fetches Observations, Conditions, and Procedures for patients
 */
@Injectable({
  providedIn: 'root',
})
export class FhirClinicalService {
  private readonly baseUrl = API_CONFIG.FHIR_SERVER_URL;

  constructor(private http: HttpClient) {}

  /**
   * Get all clinical data for a patient
   */
  getPatientClinicalData(patientId: string): Observable<PatientClinicalData> {
    return forkJoin({
      observations: this.getObservations(patientId),
      conditions: this.getConditions(patientId),
      procedures: this.getProcedures(patientId),
    });
  }

  /**
   * Get observations for a patient
   * Endpoint: GET /Observation?patient={id}
   */
  getObservations(patientId: string, count: number = 100): Observable<Observation[]> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.OBSERVATION, {
      patient: patientId,
      _count: count.toString(),
      _sort: '-date',
    });
    return this.http.get<FhirBundle<Observation>>(url).pipe(
      map((bundle) => this.extractResourcesFromBundle(bundle)),
      catchError(() => of([]))
    );
  }

  /**
   * Get conditions for a patient
   * Endpoint: GET /Condition?patient={id}
   */
  getConditions(patientId: string, count: number = 100): Observable<Condition[]> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.CONDITION, {
      patient: patientId,
      _count: count.toString(),
      _sort: '-recorded-date',
    });
    return this.http.get<FhirBundle<Condition>>(url).pipe(
      map((bundle) => this.extractResourcesFromBundle(bundle)),
      catchError(() => of([]))
    );
  }

  /**
   * Get procedures for a patient
   * Endpoint: GET /Procedure?patient={id}
   */
  getProcedures(patientId: string, count: number = 100): Observable<Procedure[]> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.PROCEDURE, {
      patient: patientId,
      _count: count.toString(),
      _sort: '-date',
    });
    return this.http.get<FhirBundle<Procedure>>(url).pipe(
      map((bundle) => this.extractResourcesFromBundle(bundle)),
      catchError(() => of([]))
    );
  }

  /**
   * Get observations by code (e.g., HbA1c, Blood Pressure)
   */
  getObservationsByCode(patientId: string, code: string): Observable<Observation[]> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.OBSERVATION, {
      patient: patientId,
      code,
      _sort: '-date',
    });
    return this.http.get<FhirBundle<Observation>>(url).pipe(
      map((bundle) => this.extractResourcesFromBundle(bundle)),
      catchError(() => of([]))
    );
  }

  /**
   * Helper: Extract resources from FHIR Bundle
   */
  private extractResourcesFromBundle<T>(bundle: FhirBundle<T>): T[] {
    if (!bundle.entry || bundle.entry.length === 0) {
      return [];
    }
    return bundle.entry.map((entry) => entry.resource);
  }

  /**
   * Helper: Format observation value for display
   */
  formatObservationValue(observation: Observation): string {
    if (observation.valueQuantity) {
      return `${observation.valueQuantity.value} ${observation.valueQuantity.unit}`;
    }
    if (observation.valueString) {
      return observation.valueString;
    }
    if (observation.valueCodeableConcept?.text) {
      return observation.valueCodeableConcept.text;
    }
    return 'N/A';
  }

  /**
   * Helper: Get observation code display name
   */
  getObservationCodeDisplay(observation: Observation): string {
    if (observation.code.text) {
      return observation.code.text;
    }
    if (observation.code.coding && observation.code.coding.length > 0) {
      return observation.code.coding[0].display || observation.code.coding[0].code || 'Unknown';
    }
    return 'Unknown';
  }

  /**
   * Helper: Get condition code display name
   */
  getConditionCodeDisplay(condition: Condition): string {
    if (condition.code.text) {
      return condition.code.text;
    }
    if (condition.code.coding && condition.code.coding.length > 0) {
      return condition.code.coding[0].display || condition.code.coding[0].code || 'Unknown';
    }
    return 'Unknown';
  }

  /**
   * Helper: Get procedure code display name
   */
  getProcedureCodeDisplay(procedure: Procedure): string {
    if (procedure.code.text) {
      return procedure.code.text;
    }
    if (procedure.code.coding && procedure.code.coding.length > 0) {
      return procedure.code.coding[0].display || procedure.code.coding[0].code || 'Unknown';
    }
    return 'Unknown';
  }

  /**
   * Helper: Get condition status
   */
  getConditionStatus(condition: Condition): string {
    if (condition.clinicalStatus?.coding && condition.clinicalStatus.coding.length > 0) {
      return condition.clinicalStatus.coding[0].code || 'unknown';
    }
    return 'unknown';
  }
}
