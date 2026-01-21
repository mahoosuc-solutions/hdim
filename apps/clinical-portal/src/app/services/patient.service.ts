import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, tap, shareReplay } from 'rxjs';
import {
  Patient,
  PatientSummary,
  Bundle,
  BundleEntry,
} from '../models/patient.model';
import {
  API_CONFIG,
  FHIR_ENDPOINTS,
  buildFhirUrl,
} from '../config/api.config';

/**
 * PatientService - Handles FHIR Patient resource operations
 * Communicates with FHIR Service (port 8085)
 *
 * Implements caching with 5-minute TTL and automatic invalidation on mutations
 */
@Injectable({
  providedIn: 'root',
})
export class PatientService {
  private readonly baseUrl = API_CONFIG.FHIR_SERVER_URL;

  // Caching infrastructure
  private patientSummaryCache$: Observable<PatientSummary[]> | null = null;
  private patientsCache$: Observable<Patient[]> | null = null;
  private cacheTimestamp = 0;
  private readonly CACHE_TTL = 5 * 60 * 1000; // 5 minutes

  constructor(private http: HttpClient) {}

  /**
   * Get all patients
   * Endpoint: GET /Patient
   */
  getPatients(count: number = 100): Observable<Patient[]> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.PATIENT, {
      _count: count.toString(),
    });
    return this.http.get<Bundle<Patient>>(url).pipe(
      map((bundle) => this.extractResourcesFromBundle(bundle))
    );
  }

  /**
   * Get patient by ID
   * Endpoint: GET /Patient/{id}
   */
  getPatient(id: string): Observable<Patient> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.PATIENT_BY_ID(id));
    return this.http.get<Patient>(url);
  }

  /**
   * Search patients by name
   * Endpoint: GET /Patient?name={name}
   */
  searchPatientsByName(name: string): Observable<Patient[]> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.PATIENT, {
      name,
    });
    return this.http.get<Bundle<Patient>>(url).pipe(
      map((bundle) => this.extractResourcesFromBundle(bundle))
    );
  }

  /**
   * Search patients by identifier (e.g., MRN)
   * Endpoint: GET /Patient?identifier={value}
   */
  searchPatientsByIdentifier(value: string): Observable<Patient[]> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.PATIENT, {
      identifier: value,
    });
    return this.http.get<Bundle<Patient>>(url).pipe(
      map((bundle) => this.extractResourcesFromBundle(bundle))
    );
  }

  /**
   * Search patients by birthdate
   * Endpoint: GET /Patient?birthdate={date}
   */
  searchPatientsByBirthdate(birthdate: string): Observable<Patient[]> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.PATIENT, {
      birthdate,
    });
    return this.http.get<Bundle<Patient>>(url).pipe(
      map((bundle) => this.extractResourcesFromBundle(bundle))
    );
  }

  /**
   * Search patients with multiple criteria
   */
  searchPatients(params: {
    name?: string;
    identifier?: string;
    birthdate?: string;
    gender?: string;
  }): Observable<Patient[]> {
    const queryParams: Record<string, string> = {};

    if (params.name) queryParams['name'] = params.name;
    if (params.identifier) queryParams['identifier'] = params.identifier;
    if (params.birthdate) queryParams['birthdate'] = params.birthdate;
    if (params.gender) queryParams['gender'] = params.gender;

    const url = buildFhirUrl(FHIR_ENDPOINTS.PATIENT, queryParams);
    return this.http.get<Bundle<Patient>>(url).pipe(
      map((bundle) => this.extractResourcesFromBundle(bundle))
    );
  }

  /**
   * Create a new patient
   * Endpoint: POST /Patient
   * Automatically invalidates cache on success
   */
  createPatient(patient: Patient): Observable<Patient> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.PATIENT);
    return this.http.post<Patient>(url, patient).pipe(
      tap(() => this.invalidateCache())
    );
  }

  /**
   * Update an existing patient
   * Endpoint: PUT /Patient/{id}
   * Automatically invalidates cache on success
   */
  updatePatient(id: string, patient: Patient): Observable<Patient> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.PATIENT_BY_ID(id));
    return this.http.put<Patient>(url, patient).pipe(
      tap(() => this.invalidateCache())
    );
  }

  /**
   * Delete a patient (soft delete)
   * Endpoint: DELETE /Patient/{id}
   * Automatically invalidates cache on success
   */
  deletePatient(id: string): Observable<void> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.PATIENT_BY_ID(id));
    return this.http.delete<void>(url).pipe(
      tap(() => this.invalidateCache())
    );
  }

  /**
   * Get all patients as simplified PatientSummary objects
   * Useful for UI display
   */
  getPatientsSummary(): Observable<PatientSummary[]> {
    return this.getPatients().pipe(
      map((patients) => patients.map((p) => this.toPatientSummary(p)))
    );
  }

  /**
   * Get all patients with caching (5-minute TTL)
   * Use this for dashboard and lists that don't need real-time data
   */
  getPatientsCached(count: number = 100): Observable<Patient[]> {
    const now = Date.now();
    if (this.patientsCache$ && (now - this.cacheTimestamp) < this.CACHE_TTL) {
      return this.patientsCache$;
    }
    this.cacheTimestamp = now;
    this.patientsCache$ = this.getPatients(count).pipe(shareReplay(1));
    return this.patientsCache$;
  }

  /**
   * Get all patients as simplified PatientSummary objects with caching (5-minute TTL)
   * Use this for dashboard statistics and patient lists
   */
  getPatientsSummaryCached(): Observable<PatientSummary[]> {
    const now = Date.now();
    if (this.patientSummaryCache$ && (now - this.cacheTimestamp) < this.CACHE_TTL) {
      return this.patientSummaryCache$;
    }
    this.cacheTimestamp = now;
    this.patientSummaryCache$ = this.getPatientsSummary().pipe(shareReplay(1));
    return this.patientSummaryCache$;
  }

  /**
   * Invalidate all patient caches
   * Called automatically after create, update, or delete operations
   */
  invalidateCache(): void {
    this.patientSummaryCache$ = null;
    this.patientsCache$ = null;
    this.cacheTimestamp = 0;
  }

  /**
   * Check if cache is still valid
   */
  isCacheValid(): boolean {
    return (Date.now() - this.cacheTimestamp) < this.CACHE_TTL;
  }

  /**
   * Helper: Extract resources from FHIR Bundle
   */
  private extractResourcesFromBundle<T>(bundle: Bundle<T>): T[] {
    if (!bundle.entry || bundle.entry.length === 0) {
      return [];
    }
    return bundle.entry.map((entry) => entry.resource);
  }

  /**
   * Helper: Convert FHIR Patient to PatientSummary for UI display
   */
  toPatientSummary(patient: Patient): PatientSummary {
    const name = patient.name && patient.name.length > 0 ? patient.name[0] : undefined;
    const fullName = name
      ? `${name.given?.join(' ') || ''} ${name.family || ''}`.trim()
      : 'Unknown';

    const firstName = name?.given?.[0];
    const lastName = name?.family;

    // Extract MRN from identifiers - prefer type.text, fallback to first identifier
    const mrnIdentifier = patient.identifier?.find(
      (id) => id.type?.text === 'Medical Record Number'
    ) || patient.identifier?.[0];
    const mrn = mrnIdentifier?.value;
    const mrnAssigningAuthority = mrnIdentifier?.system;

    // Calculate age from birthDate
    const age = patient.birthDate ? this.calculateAge(patient.birthDate) : undefined;

    return {
      id: patient.id,
      mrn,
      mrnAssigningAuthority,
      fullName,
      firstName,
      lastName,
      dateOfBirth: patient.birthDate,
      age,
      gender: patient.gender,
      status: patient.active !== false ? 'Active' : 'Inactive',
    };
  }

  /**
   * Helper: Calculate age from birthdate
   */
  private calculateAge(birthDate: string): number {
    const today = new Date();
    const birth = new Date(birthDate);
    let age = today.getFullYear() - birth.getFullYear();
    const monthDiff = today.getMonth() - birth.getMonth();

    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
      age--;
    }

    return age;
  }

  /**
   * Helper: Format patient name for display
   */
  formatPatientName(patient: Patient): string {
    if (!patient.name || patient.name.length === 0) {
      return 'Unknown';
    }

    const name = patient.name[0];
    return `${name.given?.join(' ') || ''} ${name.family || ''}`.trim();
  }

  /**
   * Helper: Get MRN from patient
   */
  getPatientMRN(patient: Patient): string | undefined {
    return patient.identifier?.find(
      (id) => id.type?.text === 'Medical Record Number'
    )?.value;
  }

  /**
   * Helper: Get MRN assigning authority from patient
   */
  getPatientMRNAuthority(patient: Patient): string | undefined {
    return patient.identifier?.find(
      (id) => id.type?.text === 'Medical Record Number'
    )?.system;
  }
}
