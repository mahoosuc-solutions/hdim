/**
 * Clinical 360 Data Pipeline Service
 *
 * Orchestrates multi-source clinical data collection for a patient:
 * 1. Demographics (Patient Service)
 * 2. Clinical observations (FHIR Service)
 * 3. Quality measures (Quality Measure Service)
 * 4. Care gaps (Care Gap Service)
 * 5. Workflow state (Clinical Workflow Service)
 *
 * This service ensures data consistency and prevents race conditions
 * when loading the complete patient picture across multiple MFEs.
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import {
  BehaviorSubject,
  Observable,
  combineLatest,
  of,
  throwError,
} from 'rxjs';
import {
  tap,
  catchError,
  finalize,
  map,
  shareReplay,
} from 'rxjs/operators';
import { EventBusService, ClinicalEventType } from '../event-bus/event-bus.service';

/**
 * Complete clinical picture for a patient
 */
export interface Clinical360Data {
  // Demographics & identity
  patient: {
    id: string;
    firstName: string;
    lastName: string;
    dob: string;
    mrnList: Array<{ system: string; value: string }>;
    activeProblems: string[];
    demographics: {
      age: number;
      gender: string;
      zipCode?: string;
    };
  };

  // Clinical observations & findings
  clinicalFindings: {
    activeConditions: Array<{ id: string; name: string; onsetDate: string }>;
    medications: Array<{ id: string; name: string; status: string }>;
    allergies: Array<{ id: string; allergen: string; reaction: string }>;
    vitalSigns?: {
      bmi?: number;
      bloodPressure?: string;
      lastRecorded?: string;
    };
  };

  // Quality measures evaluation
  qualityMeasures: {
    measures: Array<{
      id: string;
      name: string;
      status: 'MET' | 'NOT_MET' | 'EXCLUDED' | 'NOT_APPLICABLE';
      populationCriteria: {
        population: boolean;
        denominator: boolean;
        numerator: boolean;
      };
    }>;
    totalMeasures: number;
    measuresMet: number;
    evaluatedAt: string;
  };

  // Care gaps identified
  careGaps: {
    gaps: Array<{
      id: string;
      measureId: string;
      measureName: string;
      interventionType: string;
      dueDate: string;
      priority: 'HIGH' | 'MEDIUM' | 'LOW';
    }>;
    totalGaps: number;
    criticalGaps: number;
  };

  // Active workflows
  activeWorkflows: {
    workflows: Array<{
      id: string;
      type: string;
      status: 'IN_PROGRESS' | 'COMPLETED' | 'SUSPENDED';
      startedAt: string;
      currentStep?: string;
    }>;
    totalActive: number;
  };

  // Pipeline metadata
  metadata: {
    patientId: string;
    tenantId: string;
    loadedAt: string;
    completedAt?: string;
    dataQuality: {
      demographicsComplete: boolean;
      clinicalFindingsComplete: boolean;
      measuresEvaluated: boolean;
      careGapsComputed: boolean;
      workflowsLoaded: boolean;
    };
    errors: Array<{ source: string; message: string }>;
  };
}

@Injectable({
  providedIn: 'root',
})
export class Clinical360PipelineService {
  private http = inject(HttpClient);
  private eventBus = inject(EventBusService);

  // Cache the 360 data for the current patient
  private clinical360Subject = new BehaviorSubject<Clinical360Data | null>(null);
  public clinical360$ = this.clinical360Subject.asObservable();

  // Track pipeline loading state
  private loadingSubject = new BehaviorSubject<boolean>(false);
  public loading$ = this.loadingSubject.asObservable();

  // Track pipeline errors
  private errorsSubject = new BehaviorSubject<Array<{ source: string; message: string }>>([]);
  public errors$ = this.errorsSubject.asObservable();

  /**
   * Load complete 360 clinical picture for a patient
   * Called when patient is selected in any MFE
   */
  loadClinical360(patientId: string, tenantId: string): Observable<Clinical360Data> {
    // Emit pipeline started event
    this.eventBus.emit({
      type: ClinicalEventType.DATA_PIPELINE_INITIALIZED,
      source: 'clinical-360-pipeline',
      data: { patientId, tenantId },
    } as any);

    this.loadingSubject.next(true);
    this.errorsSubject.next([]);

    return this.http
      .get<Clinical360Data>(`/api/v1/clinical-360/${patientId}`, {
        headers: {
          'X-Tenant-ID': tenantId,
        },
      })
      .pipe(
        tap((data) => {
          // Update cache
          this.clinical360Subject.next(data);

          // Emit pipeline ready event
          this.eventBus.emit({
            type: ClinicalEventType.DATA_PIPELINE_READY,
            source: 'clinical-360-pipeline',
            data: { patientId, tenantId, dataQuality: data.metadata.dataQuality },
          } as any);
        }),
        catchError((error) => {
          const errorMsg = error?.error?.message || 'Failed to load clinical 360 data';
          this.errorsSubject.next([{ source: 'clinical-360-pipeline', message: errorMsg }]);

          return throwError(() => new Error(errorMsg));
        }),
        finalize(() => {
          this.loadingSubject.next(false);
        }),
        // Cache the result for 5 minutes (HIPAA compliant)
        shareReplay({
          bufferSize: 1,
          refCount: false,
          windowTime: 5 * 60 * 1000,
        })
      );
  }

  /**
   * Get cached 360 data if available
   */
  getCached360Data(): Clinical360Data | null {
    return this.clinical360Subject.value;
  }

  /**
   * Refresh specific section of 360 data without full reload
   * Useful when a single MFE updates data (e.g., quality measures completed)
   */
  refreshSection(
    patientId: string,
    tenantId: string,
    section: keyof Clinical360Data
  ): Observable<Partial<Clinical360Data>> {
    return this.http
      .get<Partial<Clinical360Data>>(
        `/api/v1/clinical-360/${patientId}/${section}`,
        {
          headers: {
            'X-Tenant-ID': tenantId,
          },
        }
      )
      .pipe(
        tap((updates) => {
          const current = this.clinical360Subject.value;
          if (current) {
            // Merge updates into cached data
            this.clinical360Subject.next({
              ...current,
              [section]: updates[section],
              metadata: {
                ...current.metadata,
                loadedAt: new Date().toISOString(),
              },
            });
          }

          // Emit section complete event
          this.eventBus.emit({
            type: ClinicalEventType.DATA_PIPELINE_STEP_COMPLETE,
            source: 'clinical-360-pipeline',
            data: { patientId, section },
          } as any);
        }),
        catchError((error) => {
          console.warn(`Failed to refresh ${section}:`, error);
          // Don't throw - allow partial failures
          return of({});
        })
      );
  }

  /**
   * Clear cached 360 data (e.g., on patient deselection or logout)
   */
  clearCache(): void {
    this.clinical360Subject.next(null);
    this.errorsSubject.next([]);
  }

  /**
   * Get data quality indicators for the 360 data
   */
  getDataQuality(): Observable<Clinical360Data['metadata']['dataQuality']> {
    return this.clinical360$.pipe(
      map((data) => data?.metadata.dataQuality || {
        demographicsComplete: false,
        clinicalFindingsComplete: false,
        measuresEvaluated: false,
        careGapsComputed: false,
        workflowsLoaded: false,
      }),
    );
  }

  /**
   * Compute care readiness score based on 360 data
   * Ranges from 0-100 (higher = better care coordination)
   */
  getCareReadinessScore(): Observable<number> {
    return this.clinical360$.pipe(
      map((data) => {
        if (!data) return 0;

        const { dataQuality } = data.metadata;
        const { qualityMeasures, careGaps } = data;

        // Quality score: percentage of measures met
        const qualityScore =
          qualityMeasures.totalMeasures > 0
            ? (qualityMeasures.measuresMet / qualityMeasures.totalMeasures) * 40
            : 0;

        // Data completeness score
        const completenessWeight =
          (Object.values(dataQuality).filter((v) => v).length /
            Object.keys(dataQuality).length) *
          30;

        // Care gap score: fewer critical gaps = higher score
        const gapScore =
          careGaps?.totalGaps && careGaps.totalGaps > 0
            ? Math.max(0, 30 - careGaps.criticalGaps * 10)
            : 30;

        return Math.round(qualityScore + completenessWeight + gapScore);
      })
    );
  }
}
