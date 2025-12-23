import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map, tap, shareReplay } from 'rxjs';
import {
  CqlLibrary,
  CqlLibraryRequest,
  LibraryStatus,
  MeasureInfo,
  HedisMeasureInfo,
  HedisMeasuresResponse,
  MeasureCategory,
} from '../models/cql-library.model';
import {
  API_CONFIG,
  CQL_ENGINE_ENDPOINTS,
  buildCqlEngineUrl,
} from '../config/api.config';

/**
 * MeasureService - Primary service for CQL Library operations
 * Communicates with CQL Engine Service (port 8081)
 *
 * Implements caching with 10-minute TTL (measures rarely change)
 */
@Injectable({
  providedIn: 'root',
})
export class MeasureService {
  private readonly baseUrl = API_CONFIG.CQL_ENGINE_URL;

  // Caching infrastructure - 10 minute TTL since measures change infrequently
  private activeMeasuresCache$: Observable<CqlLibrary[]> | null = null;
  private activeMeasuresInfoCache$: Observable<MeasureInfo[]> | null = null;
  private hedisMeasuresCache$: Observable<HedisMeasureInfo[]> | null = null;
  private cacheTimestamp = 0;
  private hedisCacheTimestamp = 0;
  private readonly CACHE_TTL = 10 * 60 * 1000; // 10 minutes
  // Toggle to expose preview custom measures until backend APIs are available.
  private readonly includePreviewCustomMeasures = true;
  // Preview-only custom measures so the UI can surface custom content before backend endpoints exist.
  private readonly previewCustomMeasures: MeasureInfo[] = [
    {
      id: 'preview-custom-mrna-screening',
      name: 'CUSTOM_MRNA_SCREENING',
      displayName: 'Custom mRNA Screening v1.0',
      version: '1.0',
      description: 'Example custom measure built from FHIR observations',
      category: 'CUSTOM',
    },
  ];

  constructor(private http: HttpClient) {}

  /**
   * Get all active CQL measures
   * Endpoint: GET /api/v1/cql/libraries/active
   * Use this for populating measure dropdowns
   */
  getActiveMeasures(): Observable<CqlLibrary[]> {
    const url = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARIES_ACTIVE);
    return this.http.get<CqlLibrary[]>(url);
  }

  /**
   * Get all active measures as simplified MeasureInfo
   * Useful for dropdowns and UI display
   */
  getActiveMeasuresInfo(): Observable<MeasureInfo[]> {
    return this.getActiveMeasures().pipe(
      map((libraries) => {
        const base = libraries.map((lib) => this.toMeasureInfo(lib));
        return this.includePreviewCustomMeasures
          ? [...base, ...this.previewCustomMeasures]
          : base;
      })
    );
  }

  /**
   * Get all active CQL measures with caching (10-minute TTL)
   * Use this for dashboard and lists
   */
  getActiveMeasuresCached(): Observable<CqlLibrary[]> {
    const now = Date.now();
    if (this.activeMeasuresCache$ && (now - this.cacheTimestamp) < this.CACHE_TTL) {
      return this.activeMeasuresCache$;
    }
    this.cacheTimestamp = now;
    this.activeMeasuresCache$ = this.getActiveMeasures().pipe(shareReplay(1));
    return this.activeMeasuresCache$;
  }

  /**
   * Get all active measures as simplified MeasureInfo with caching (10-minute TTL)
   * Use this for dropdowns and dashboard - avoids repeated API calls
   */
  getActiveMeasuresInfoCached(): Observable<MeasureInfo[]> {
    const now = Date.now();
    if (this.activeMeasuresInfoCache$ && (now - this.cacheTimestamp) < this.CACHE_TTL) {
      return this.activeMeasuresInfoCache$;
    }
    this.cacheTimestamp = now;
    this.activeMeasuresInfoCache$ = this.getActiveMeasuresInfo().pipe(shareReplay(1));
    return this.activeMeasuresInfoCache$;
  }

  /**
   * Invalidate all measure caches
   * Called after create, update, delete, activate, or retire operations
   */
  invalidateCache(): void {
    this.activeMeasuresCache$ = null;
    this.activeMeasuresInfoCache$ = null;
    this.hedisMeasuresCache$ = null;
    this.cacheTimestamp = 0;
    this.hedisCacheTimestamp = 0;
  }

  // ============================================================================
  // HEDIS Measure Discovery Methods (Phase 2)
  // These methods use the new /evaluate/measures endpoints from Phase 1
  // ============================================================================

  /**
   * Get all registered HEDIS measures from the backend registry
   * Endpoint: GET /evaluate/measures
   * Returns all 56 HEDIS measures with their metadata
   */
  getHedisMeasures(): Observable<HedisMeasureInfo[]> {
    const url = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.HEDIS_MEASURES);
    return this.http.get<HedisMeasuresResponse>(url).pipe(
      map((response) => response.measures || [])
    );
  }

  /**
   * Get all HEDIS measures with caching (10-minute TTL)
   * Use this for dropdowns and lists to avoid repeated API calls
   */
  getHedisMeasuresCached(): Observable<HedisMeasureInfo[]> {
    const now = Date.now();
    if (this.hedisMeasuresCache$ && (now - this.hedisCacheTimestamp) < this.CACHE_TTL) {
      return this.hedisMeasuresCache$;
    }
    this.hedisCacheTimestamp = now;
    this.hedisMeasuresCache$ = this.getHedisMeasures().pipe(shareReplay(1));
    return this.hedisMeasuresCache$;
  }

  /**
   * Get HEDIS measures as MeasureInfo for UI dropdowns
   * Converts HedisMeasureInfo to MeasureInfo format for compatibility
   */
  getHedisMeasuresAsInfo(): Observable<MeasureInfo[]> {
    return this.getHedisMeasuresCached().pipe(
      map((measures) => measures.map((m) => this.hedisMeasureToInfo(m)))
    );
  }

  /**
   * Get a specific HEDIS measure by ID
   * Endpoint: GET /evaluate/measures/{measureId}
   */
  getHedisMeasureById(measureId: string): Observable<HedisMeasureInfo> {
    const url = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.HEDIS_MEASURE_BY_ID(measureId));
    return this.http.get<HedisMeasureInfo>(url);
  }

  /**
   * Check if a HEDIS measure is registered
   * Endpoint: GET /evaluate/measures/{measureId}/exists
   */
  hedisMeasureExists(measureId: string): Observable<boolean> {
    const url = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.HEDIS_MEASURE_EXISTS(measureId));
    return this.http.get<{ measureId: string; exists: boolean }>(url).pipe(
      map((response) => response.exists)
    );
  }

  /**
   * Get all measures (combines HEDIS + active library measures)
   * This provides a unified list of all available measures for evaluation
   */
  getAllAvailableMeasures(): Observable<MeasureInfo[]> {
    return this.getHedisMeasuresAsInfo().pipe(
      map((hedisMeasures) => {
        // Add preview custom measures if enabled
        return this.includePreviewCustomMeasures
          ? [...hedisMeasures, ...this.previewCustomMeasures]
          : hedisMeasures;
      })
    );
  }

  /**
   * Get measures filtered by category
   */
  getMeasuresByCategory(category: MeasureCategory): Observable<MeasureInfo[]> {
    return this.getAllAvailableMeasures().pipe(
      map((measures) => measures.filter((m) => m.category === category))
    );
  }

  /**
   * Helper: Convert HedisMeasureInfo to MeasureInfo
   */
  private hedisMeasureToInfo(measure: HedisMeasureInfo): MeasureInfo {
    // Determine category from measure ID pattern
    const category = this.inferCategory(measure.measureId);

    return {
      id: measure.measureId,
      name: measure.measureId,
      version: measure.version,
      description: measure.measureName,
      category,
      displayName: `${measure.measureId} - ${measure.measureName} (v${measure.version})`,
    };
  }

  /**
   * Helper: Infer category from measure ID
   * Based on HEDIS measure categories
   */
  private inferCategory(measureId: string): string {
    const categoryMap: Record<string, string[]> = {
      PREVENTIVE: ['AAP', 'ABA', 'BCS', 'CCS', 'COL', 'FVA', 'IMA', 'W15', 'LSC', 'CIS'],
      CHRONIC_DISEASE: ['CDC', 'CBP', 'SPD', 'KED', 'HBD', 'VLS', 'SSD'],
      BEHAVIORAL_HEALTH: ['ADD', 'AMM', 'FUH', 'FUM', 'FUA', 'IET', 'SSD', 'PBH', 'DSF', 'AIS'],
      MEDICATION: ['MMA', 'SFM', 'PDC', 'DRR', 'PCE', 'SAA', 'HDO', 'SPR', 'SMC', 'MRP'],
      WOMENS_HEALTH: ['PPC', 'CHL', 'NCS', 'EED', 'BPD'],
      CHILD_ADOLESCENT: ['CIS', 'IMA', 'W15', 'WCC', 'ADD', 'AMR', 'CWP'],
      SDOH: ['SDOH1', 'SDOH2'],
      UTILIZATION: ['PCR', 'MSC'],
      CARE_COORDINATION: ['COA', 'MRP', 'TSC', 'OMW', 'FUH', 'FUM'],
      OVERUSE: ['LBP', 'URI', 'CWP', 'ASF', 'CAP'],
    };

    for (const [category, measureIds] of Object.entries(categoryMap)) {
      if (measureIds.includes(measureId)) {
        return category;
      }
    }
    return 'OTHER';
  }

  /**
   * Check if cache is still valid
   */
  isCacheValid(): boolean {
    return (Date.now() - this.cacheTimestamp) < this.CACHE_TTL;
  }

  /**
   * Get all libraries (paginated)
   * Endpoint: GET /api/v1/cql/libraries
   */
  getAllLibraries(page: number = 0, size: number = 20): Observable<CqlLibrary[]> {
    const url = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARIES, {
      page: page.toString(),
      size: size.toString(),
    });
    return this.http.get<CqlLibrary[]>(url);
  }

  /**
   * Get library by ID
   * Endpoint: GET /api/v1/cql/libraries/{id}
   */
  getMeasureById(id: string): Observable<CqlLibrary> {
    const url = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARY_BY_ID(id));
    return this.http.get<CqlLibrary>(url);
  }

  /**
   * Get library by name and version
   * Endpoint: GET /api/v1/cql/libraries/by-name/{name}/version/{version}
   */
  getMeasureByName(name: string, version: string): Observable<CqlLibrary> {
    const url = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARY_BY_NAME(name, version));
    return this.http.get<CqlLibrary>(url);
  }

  /**
   * Get latest version of a library by name
   * Endpoint: GET /api/v1/cql/libraries/by-name/{name}/latest
   */
  getLatestVersion(name: string): Observable<CqlLibrary> {
    const url = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARY_LATEST(name));
    return this.http.get<CqlLibrary>(url);
  }

  /**
   * Get all versions of a library
   * Endpoint: GET /api/v1/cql/libraries/by-name/{name}/versions
   */
  getAllVersions(name: string): Observable<CqlLibrary[]> {
    const url = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARY_VERSIONS(name));
    return this.http.get<CqlLibrary[]>(url);
  }

  /**
   * Get libraries by status
   * Endpoint: GET /api/v1/cql/libraries/by-status/{status}
   */
  getMeasuresByStatus(status: LibraryStatus): Observable<CqlLibrary[]> {
    const url = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARIES_BY_STATUS(status));
    return this.http.get<CqlLibrary[]>(url);
  }

  /**
   * Search libraries
   * Endpoint: GET /api/v1/cql/libraries/search?q={searchTerm}
   */
  searchMeasures(query: string): Observable<CqlLibrary[]> {
    const url = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARIES_SEARCH, { q: query });
    return this.http.get<CqlLibrary[]>(url);
  }

  /**
   * Get library count
   * Endpoint: GET /api/v1/cql/libraries/count
   */
  getMeasureCount(): Observable<number> {
    const url = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARIES_COUNT);
    return this.http.get<number>(url);
  }

  /**
   * Get library count by status
   * Endpoint: GET /api/v1/cql/libraries/count/by-status/{status}
   */
  getMeasureCountByStatus(status: LibraryStatus): Observable<number> {
    const url = buildCqlEngineUrl(
      `${CQL_ENGINE_ENDPOINTS.LIBRARIES_COUNT}/by-status/${status}`
    );
    return this.http.get<number>(url);
  }

  /**
   * Check if library exists
   * Endpoint: GET /api/v1/cql/libraries/exists?name={name}&version={version}
   */
  measureExists(name: string, version: string): Observable<boolean> {
    const url = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARY_EXISTS, {
      name,
      version,
    });
    return this.http.get<boolean>(url);
  }

  /**
   * Create new library (ADMIN only)
   * Endpoint: POST /api/v1/cql/libraries
   * Automatically invalidates cache on success
   */
  createMeasure(request: CqlLibraryRequest): Observable<CqlLibrary> {
    const url = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARIES);
    return this.http.post<CqlLibrary>(url, request).pipe(
      tap(() => this.invalidateCache())
    );
  }

  /**
   * Update library (ADMIN only)
   * Endpoint: PUT /api/v1/cql/libraries/{id}
   * Automatically invalidates cache on success
   */
  updateMeasure(id: string, request: CqlLibraryRequest): Observable<CqlLibrary> {
    const url = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARY_BY_ID(id));
    return this.http.put<CqlLibrary>(url, request).pipe(
      tap(() => this.invalidateCache())
    );
  }

  /**
   * Activate library (ADMIN only)
   * Endpoint: POST /api/v1/cql/libraries/{id}/activate
   * Automatically invalidates cache on success
   */
  activateMeasure(id: string): Observable<CqlLibrary> {
    const url = buildCqlEngineUrl(`${CQL_ENGINE_ENDPOINTS.LIBRARY_BY_ID(id)}/activate`);
    return this.http.post<CqlLibrary>(url, {}).pipe(
      tap(() => this.invalidateCache())
    );
  }

  /**
   * Retire library (ADMIN only)
   * Endpoint: POST /api/v1/cql/libraries/{id}/retire
   * Automatically invalidates cache on success
   */
  retireMeasure(id: string): Observable<CqlLibrary> {
    const url = buildCqlEngineUrl(`${CQL_ENGINE_ENDPOINTS.LIBRARY_BY_ID(id)}/retire`);
    return this.http.post<CqlLibrary>(url, {}).pipe(
      tap(() => this.invalidateCache())
    );
  }

  /**
   * Delete library (ADMIN only)
   * Endpoint: DELETE /api/v1/cql/libraries/{id}
   * Automatically invalidates cache on success
   */
  deleteMeasure(id: string): Observable<void> {
    const url = buildCqlEngineUrl(CQL_ENGINE_ENDPOINTS.LIBRARY_BY_ID(id));
    return this.http.delete<void>(url).pipe(
      tap(() => this.invalidateCache())
    );
  }

  /**
   * Helper: Convert CqlLibrary to MeasureInfo for UI display
   */
  private toMeasureInfo(library: CqlLibrary): MeasureInfo {
    // Extract category from library name (e.g., "HEDIS-CDC" -> "HEDIS")
    const category = library.name.includes('-')
      ? library.name.split('-')[0]
      : 'CUSTOM';

    // Normalize measure ID: convert hyphens to underscores for API compatibility
    // CQL Engine uses "HEDIS-CDC", Quality Measure Service expects "HEDIS_CDC"
    const normalizedName = library.name.replace(/-/g, '_');

    return {
      id: library.id,
      name: normalizedName,  // Use normalized name for API calls
      version: library.version,
      description: library.description,
      category,
      displayName: `${library.name} v${library.version}${
        library.description ? ` - ${library.description}` : ''
      }`,
    };
  }
}
