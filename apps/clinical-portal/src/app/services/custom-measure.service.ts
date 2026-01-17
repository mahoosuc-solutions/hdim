import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_CONFIG, QUALITY_MEASURE_ENDPOINTS } from '../config/api.config';

export interface CustomMeasure {
  id: string;
  tenantId: string;
  name: string;
  version: string;
  status: string;
  description?: string;
  category?: string;
  year?: number;
  cqlText?: string;
  valueSets?: any;
  createdBy: string;
  createdAt: string;
  updatedAt?: string;
}

export interface CreateCustomMeasureRequest {
  name: string;
  description?: string;
  category?: string;
  year?: number;
  createdBy?: string;
}

export interface UpdateCustomMeasureRequest {
  name?: string;
  description?: string;
  category?: string;
  year?: number;
}

export interface MeasureVersion {
  version: string;
  status: 'draft' | 'active' | 'retired';
  createdAt: string;
  createdBy: string;
  changelog?: string;
}

export interface VersionDiff {
  field: string;
  oldValue: any;
  newValue: any;
}

@Injectable({
  providedIn: 'root',
})
export class CustomMeasureService {
  constructor(private http: HttpClient) {}

  createDraft(req: CreateCustomMeasureRequest, tenantId: string = API_CONFIG.DEFAULT_TENANT_ID): Observable<CustomMeasure> {
    const url = `${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures`;
    const headers = new HttpHeaders({
      'X-Tenant-ID': tenantId,
    });
    return this.http.post<CustomMeasure>(url, req, { headers });
  }

  list(status?: string, tenantId: string = API_CONFIG.DEFAULT_TENANT_ID): Observable<CustomMeasure[]> {
    const url = `${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures`;
    let params = new HttpParams();
    if (status) {
      params = params.set('status', status);
    }
    const headers = new HttpHeaders({
      'X-Tenant-ID': tenantId,
    });
    return this.http.get<CustomMeasure[]>(url, { headers, params });
  }

  update(id: string, req: UpdateCustomMeasureRequest, tenantId: string = API_CONFIG.DEFAULT_TENANT_ID): Observable<CustomMeasure> {
    const url = `${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/${id}`;
    const headers = new HttpHeaders({
      'X-Tenant-ID': tenantId,
    });
    return this.http.put<CustomMeasure>(url, req, { headers });
  }

  /**
   * Delete a custom measure by ID
   */
  delete(id: string, tenantId: string = API_CONFIG.DEFAULT_TENANT_ID): Observable<void> {
    const url = `${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/${id}`;
    const headers = new HttpHeaders({
      'X-Tenant-ID': tenantId,
    });
    return this.http.delete<void>(url, { headers });
  }

  /**
   * Publish a custom measure (change status from DRAFT to PUBLISHED)
   */
  publish(id: string, tenantId: string = API_CONFIG.DEFAULT_TENANT_ID): Observable<CustomMeasure> {
    const url = `${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/${id}/publish`;
    const headers = new HttpHeaders({
      'X-Tenant-ID': tenantId,
    });
    return this.http.post<CustomMeasure>(url, {}, { headers });
  }

  /**
   * Batch delete multiple custom measures
   */
  batchDelete(ids: string[], tenantId: string = API_CONFIG.DEFAULT_TENANT_ID): Observable<{ deleted: number; failed: string[] }> {
    const url = `${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/batch-delete`;
    const headers = new HttpHeaders({
      'X-Tenant-ID': tenantId,
    });
    return this.http.post<{ deleted: number; failed: string[] }>(url, { ids }, { headers });
  }

  /**
   * Batch publish multiple custom measures
   */
  batchPublish(ids: string[], tenantId: string = API_CONFIG.DEFAULT_TENANT_ID): Observable<{ published: number; failed: string[] }> {
    const url = `${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/batch-publish`;
    const headers = new HttpHeaders({
      'X-Tenant-ID': tenantId,
    });
    return this.http.post<{ published: number; failed: string[] }>(url, { ids }, { headers });
  }

  /**
   * Update CQL text for a custom measure
   */
  updateCql(id: string, cqlText: string, tenantId: string = API_CONFIG.DEFAULT_TENANT_ID): Observable<CustomMeasure> {
    const url = `${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/${id}/cql`;
    const headers = new HttpHeaders({
      'X-Tenant-ID': tenantId,
    });
    return this.http.put<CustomMeasure>(url, { cqlText }, { headers });
  }

  /**
   * Update value sets for a custom measure
   */
  updateValueSets(id: string, valueSets: any[], tenantId: string = API_CONFIG.DEFAULT_TENANT_ID): Observable<CustomMeasure> {
    const url = `${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/${id}/value-sets`;
    const headers = new HttpHeaders({
      'X-Tenant-ID': tenantId,
    });
    return this.http.put<CustomMeasure>(url, { valueSets }, { headers });
  }

  /**
   * Get a single custom measure by ID
   */
  getById(id: string, tenantId: string = API_CONFIG.DEFAULT_TENANT_ID): Observable<CustomMeasure> {
    const url = `${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/${id}`;
    const headers = new HttpHeaders({
      'X-Tenant-ID': tenantId,
    });
    return this.http.get<CustomMeasure>(url, { headers });
  }

  /**
   * Test/evaluate a measure against sample patients
   */
  testMeasure(id: string, tenantId: string = API_CONFIG.DEFAULT_TENANT_ID): Observable<TestMeasureResult> {
    const url = `${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/${id}/test`;
    const headers = new HttpHeaders({
      'X-Tenant-ID': tenantId,
    });
    return this.http.post<TestMeasureResult>(url, {}, { headers });
  }

  /**
   * Create a new version of an existing measure
   * @param measureId The measure ID to create a new version from
   * @param versionType The type of version increment (major, minor, or patch)
   * @param tenantId The tenant ID
   * @returns The newly created measure with incremented version
   */
  createNewVersion(
    measureId: string,
    versionType: 'major' | 'minor' | 'patch',
    tenantId: string = API_CONFIG.DEFAULT_TENANT_ID
  ): Observable<CustomMeasure> {
    const url = `${API_CONFIG.QUALITY_MEASURE_URL}/api/v1/measures/${measureId}/versions`;
    const headers = new HttpHeaders({
      'X-Tenant-ID': tenantId,
      'X-Auth-User-Id': 'clinical-portal-user',
      'X-Auth-Username': 'Clinical Portal',
    });
    return this.http.post<CustomMeasure>(url, { versionType }, { headers });
  }

  /**
   * Get version history for a measure
   * @param measureId The measure ID
   * @param tenantId The tenant ID
   * @returns Array of version history entries
   */
  getVersionHistory(measureId: string, tenantId: string = API_CONFIG.DEFAULT_TENANT_ID): Observable<MeasureVersion[]> {
    const url = `${API_CONFIG.QUALITY_MEASURE_URL}/api/v1/measures/${measureId}/versions`;
    const headers = new HttpHeaders({
      'X-Tenant-ID': tenantId,
    });
    return this.http.get<MeasureVersion[]>(url, { headers });
  }

  /**
   * Compare two versions of a measure
   * @param measureId The measure ID
   * @param v1 First version to compare
   * @param v2 Second version to compare
   * @param tenantId The tenant ID
   * @returns Array of differences between the two versions
   */
  compareVersions(
    measureId: string,
    v1: string,
    v2: string,
    tenantId: string = API_CONFIG.DEFAULT_TENANT_ID
  ): Observable<VersionDiff[]> {
    const url = `${API_CONFIG.QUALITY_MEASURE_URL}/api/v1/measures/${measureId}/versions/compare`;
    const headers = new HttpHeaders({
      'X-Tenant-ID': tenantId,
    });
    const params = new HttpParams().set('v1', v1).set('v2', v2);
    return this.http.get<VersionDiff[]>(url, { headers, params });
  }

  /**
   * Publish a specific version of a measure (changes status from draft to active)
   * @param measureId The measure ID
   * @param version The version to publish
   * @param tenantId The tenant ID
   * @returns The published measure
   */
  publishVersion(
    measureId: string,
    version: string,
    tenantId: string = API_CONFIG.DEFAULT_TENANT_ID
  ): Observable<CustomMeasure> {
    const url = `${API_CONFIG.QUALITY_MEASURE_URL}/api/v1/measures/${measureId}/versions/${version}/publish`;
    const headers = new HttpHeaders({
      'X-Tenant-ID': tenantId,
      'X-Auth-User-Id': 'clinical-portal-user',
    });
    return this.http.post<CustomMeasure>(url, {}, { headers });
  }

  /**
   * Retire a specific version of a measure (changes status to retired)
   * @param measureId The measure ID
   * @param version The version to retire
   * @param tenantId The tenant ID
   * @returns The retired measure
   */
  retireVersion(
    measureId: string,
    version: string,
    tenantId: string = API_CONFIG.DEFAULT_TENANT_ID
  ): Observable<CustomMeasure> {
    const url = `${API_CONFIG.QUALITY_MEASURE_URL}/api/v1/measures/${measureId}/versions/${version}/retire`;
    const headers = new HttpHeaders({
      'X-Tenant-ID': tenantId,
    });
    return this.http.post<CustomMeasure>(url, {}, { headers });
  }

  /**
   * Clone a measure as a new draft
   * @param measureId The measure ID to clone
   * @param tenantId The tenant ID
   * @returns The newly cloned measure as a draft
   */
  cloneMeasure(measureId: string, tenantId: string = API_CONFIG.DEFAULT_TENANT_ID): Observable<CustomMeasure> {
    const url = `${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/${measureId}/clone`;
    const headers = new HttpHeaders({
      'X-Tenant-ID': tenantId,
    });
    return this.http.post<CustomMeasure>(url, {}, { headers });
  }
}

/**
 * Test measure result interface
 */
export interface TestMeasureResult {
  measureId: string;
  measureName: string;
  testDate: string;
  totalPatients: number;
  results: TestPatientResult[];
  summary: {
    passed: number;
    failed: number;
    notEligible: number;
    errors: number;
  };
}

export interface TestPatientResult {
  patientId: string;
  patientName: string;
  mrn: string;
  outcome: 'pass' | 'fail' | 'not-eligible' | 'error';
  inPopulation: boolean;
  inDenominator: boolean;
  inNumerator: boolean;
  exclusionReason?: string;
  details: string[];
  executionTimeMs?: number;
}
