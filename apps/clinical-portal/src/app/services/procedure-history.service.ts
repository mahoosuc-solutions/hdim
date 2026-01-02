/**
 * Procedure History Service
 *
 * Feature 2.4: Comprehensive service for fetching, categorizing, and displaying
 * FHIR Procedure resources with provider information
 */

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  ProcedureRecord,
  CategorizedProcedures,
  ProcedureQueryOptions,
  FhirProcedure,
  ProcedureCategory,
  PROCEDURE_CATEGORY_MAPPING,
} from '../models/procedure-history.model';
import { FhirBundle } from '../models/fhir.model';
import { API_CONFIG, FHIR_ENDPOINTS, buildFhirUrl, HTTP_HEADERS } from '../config/api.config';

@Injectable({
  providedIn: 'root',
})
export class ProcedureHistoryService {
  constructor(private http: HttpClient) {}

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
   * Get procedures for a patient with optional filtering
   * @param patientId Patient ID
   * @param options Query options for filtering
   * @returns Observable of procedure records
   */
  getProcedures(patientId: string, options?: ProcedureQueryOptions): Observable<ProcedureRecord[]> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.PROCEDURE);
    let params = new HttpParams().set('patient', patientId);

    // Apply filters
    if (options?.status) {
      params = params.set('status', options.status);
    }

    if (options?.startDate && options?.endDate) {
      const startDateStr = options.startDate.toISOString().split('T')[0];
      const endDateStr = options.endDate.toISOString().split('T')[0];
      params = params.set('date', `ge${startDateStr},le${endDateStr}`);
    }

    if (options?.limit) {
      params = params.set('_count', options.limit.toString());
    }

    return this.http
      .get<FhirBundle<FhirProcedure>>(url, {
        headers: this.getHeaders(),
        params,
      })
      .pipe(
        map((bundle) => {
          const procedures = (bundle.entry || [])
            .map((entry) => entry.resource)
            .filter((procedure): procedure is FhirProcedure => procedure !== undefined)
            .map((procedure) => this.mapFhirProcedureToProcedureRecord(procedure));

          return procedures;
        })
      );
  }

  /**
   * Get procedures organized by category
   * @param patientId Patient ID
   * @returns Observable of categorized procedures
   */
  getProceduresByCategory(patientId: string): Observable<CategorizedProcedures> {
    return this.getProcedures(patientId).pipe(
      map((procedures) => {
        const categorized: CategorizedProcedures = {
          surgical: [],
          imaging: [],
          lab: [],
          therapeutic: [],
          preventive: [],
          other: [],
          totalCount: procedures.length,
        };

        procedures.forEach((procedure) => {
          categorized[procedure.category].push(procedure);
        });

        return categorized;
      })
    );
  }

  /**
   * Get recent procedures (last 12 months)
   * @param patientId Patient ID
   * @returns Observable of recent procedure records
   */
  getRecentProcedures(patientId: string): Observable<ProcedureRecord[]> {
    const today = new Date();
    const twelveMonthsAgo = new Date(today);
    twelveMonthsAgo.setMonth(today.getMonth() - 12);

    const options: ProcedureQueryOptions = {
      startDate: twelveMonthsAgo,
      endDate: today,
    };

    return this.getProcedures(patientId, options).pipe(
      map((procedures) => {
        // Client-side filtering to ensure only procedures from last 12 months
        const filtered = procedures.filter((proc) => {
          const procDate = new Date(proc.performedDate);
          return procDate >= twelveMonthsAgo && procDate <= today;
        });

        // Sort by date descending (most recent first)
        return filtered.sort((a, b) => b.performedDate.getTime() - a.performedDate.getTime());
      })
    );
  }

  /**
   * Get scheduled procedures (status=preparation with future dates)
   * @param patientId Patient ID
   * @returns Observable of scheduled procedure records
   */
  getScheduledProcedures(patientId: string): Observable<ProcedureRecord[]> {
    const options: ProcedureQueryOptions = {
      status: 'preparation',
    };

    return this.getProcedures(patientId, options).pipe(
      map((procedures) => {
        const today = new Date();

        // Filter for future dates only
        const scheduled = procedures.filter((proc) => {
          const procDate = new Date(proc.performedDate);
          return procDate > today;
        });

        // Sort by date ascending (soonest first)
        return scheduled.sort((a, b) => a.performedDate.getTime() - b.performedDate.getTime());
      })
    );
  }

  /**
   * Categorize a procedure based on its code
   * @param procedure FHIR Procedure resource
   * @returns Procedure category
   */
  categorizeProcedure(procedure: FhirProcedure): ProcedureCategory {
    if (!procedure.code?.coding || procedure.code.coding.length === 0) {
      return 'other';
    }

    const coding = procedure.code.coding[0];
    const system = coding.system;
    const code = coding.code;

    if (!code) {
      return 'other';
    }

    // Check SNOMED codes
    if (system === 'http://snomed.info/sct') {
      const codeNum = parseInt(code, 10);

      // Check specific SNOMED codes for categories
      // Cast to readonly number[] to allow includes() with parsed number
      if ((PROCEDURE_CATEGORY_MAPPING.imaging.snomedCodes as readonly number[]).includes(codeNum)) {
        return 'imaging';
      }

      if ((PROCEDURE_CATEGORY_MAPPING.lab.snomedCodes as readonly number[]).includes(codeNum)) {
        return 'lab';
      }

      if ((PROCEDURE_CATEGORY_MAPPING.preventive.snomedCodes as readonly number[]).includes(codeNum)) {
        return 'preventive';
      }

      // Check SNOMED code ranges for surgical procedures
      for (const [start, end] of PROCEDURE_CATEGORY_MAPPING.surgical.snomedRanges) {
        if (codeNum >= start && codeNum <= end) {
          return 'surgical';
        }
      }

      // Default for other SNOMED codes - check if it looks therapeutic
      // For this implementation, we'll categorize remaining as therapeutic or other
      // based on code range (simplified logic)
      if (codeNum >= 1000000 && codeNum < 300000000) {
        return 'therapeutic';
      }
    }

    // Check CPT codes
    if (system === 'http://www.ama-assn.org/go/cpt') {
      const codeNum = parseInt(code, 10);

      // Check CPT code ranges
      for (const [start, end] of PROCEDURE_CATEGORY_MAPPING.surgical.cptRanges) {
        if (codeNum >= start && codeNum <= end) {
          return 'surgical';
        }
      }

      for (const [start, end] of PROCEDURE_CATEGORY_MAPPING.imaging.cptRanges) {
        if (codeNum >= start && codeNum <= end) {
          return 'imaging';
        }
      }

      for (const [start, end] of PROCEDURE_CATEGORY_MAPPING.lab.cptRanges) {
        if (codeNum >= start && codeNum <= end) {
          return 'lab';
        }
      }

      for (const [start, end] of PROCEDURE_CATEGORY_MAPPING.preventive.cptRanges) {
        if (codeNum >= start && codeNum <= end) {
          return 'preventive';
        }
      }
    }

    return 'other';
  }

  /**
   * Map FHIR Procedure resource to ProcedureRecord
   * @param fhirProcedure FHIR Procedure resource
   * @returns ProcedureRecord
   */
  private mapFhirProcedureToProcedureRecord(fhirProcedure: FhirProcedure): ProcedureRecord {
    const id = fhirProcedure.id || 'unknown';
    const status = fhirProcedure.status;

    // Extract code and code system
    let code = '';
    let codeSystem: 'SNOMED' | 'CPT' = 'SNOMED';
    let displayName = '';

    if (fhirProcedure.code?.coding && fhirProcedure.code.coding.length > 0) {
      const coding = fhirProcedure.code.coding[0];
      code = coding.code || '';

      if (coding.system === 'http://snomed.info/sct') {
        codeSystem = 'SNOMED';
      } else if (coding.system === 'http://www.ama-assn.org/go/cpt') {
        codeSystem = 'CPT';
      }

      // Prefer text over display
      displayName = fhirProcedure.code.text || coding.display || '';
    }

    // Extract performed date
    let performedDate = new Date();
    if (fhirProcedure.performedDateTime) {
      performedDate = new Date(fhirProcedure.performedDateTime);
    } else if (fhirProcedure.performedPeriod?.start) {
      performedDate = new Date(fhirProcedure.performedPeriod.start);
    }

    // Extract performer information
    let performerName: string | undefined;
    let performerRole: string | undefined;

    if (fhirProcedure.performer && fhirProcedure.performer.length > 0) {
      const performer = fhirProcedure.performer[0];
      performerName = performer.actor?.display;

      if (performer.function?.coding && performer.function.coding.length > 0) {
        performerRole = performer.function.coding[0].display;
      }
    }

    // Extract notes
    let notes: string | undefined;
    if (fhirProcedure.note && fhirProcedure.note.length > 0) {
      notes = fhirProcedure.note.map((n) => n.text).join('; ');
    }

    // Categorize procedure
    const category = this.categorizeProcedure(fhirProcedure);

    return {
      id,
      code,
      codeSystem,
      displayName,
      category,
      performedDate,
      status,
      performerName,
      performerRole,
      notes,
    };
  }
}
