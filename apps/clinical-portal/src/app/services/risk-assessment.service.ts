/**
 * Risk Assessment Service
 *
 * Service for managing patient risk assessments including:
 * - Fetching current risk assessment
 * - Viewing risk history
 * - Getting category-specific assessments
 * - Triggering risk recalculation
 * - Viewing population-level statistics
 */

import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { ApiService } from './api.service';
import {
  RiskAssessment,
  PopulationStats,
  HccRiskAssessment,
} from '../models/risk-assessment.model';
import { API_CONFIG } from '../config/api.config';
import { LoggerService } from './logger.service';

@Injectable({
  providedIn: 'root',
})
export class RiskAssessmentService {
  private get logger() {
    return this.loggerService.withContext('RiskAssessmentService');
  }

  constructor(
    private apiService: ApiService,
    private loggerService: LoggerService
  ) {}

  /**
   * Get HCC-based risk assessment for a patient from patient-service.
   *
   * This is the primary method for risk stratification, combining:
   * - CMS-HCC RAF scores (V24, V28, blended)
   * - Care gap counts
   * - Documentation gap opportunities
   *
   * @param patientId Patient UUID
   * @returns Observable of HccRiskAssessment or null if not found
   */
  getHccRiskAssessment(patientId: string): Observable<HccRiskAssessment | null> {
    if (!patientId || patientId.trim() === '') {
      return throwError(() => new Error('Patient ID is required'));
    }

    const url = `${API_CONFIG.PATIENT_URL}/risk-assessment?patient=${encodeURIComponent(patientId)}`;

    return this.apiService.get<HccRiskAssessment>(url).pipe(
      map((assessment) => this.transformHccAssessment(assessment)),
      catchError((error) => {
        this.logger.warn(`HCC risk assessment not available for patient ${patientId}`, error);
        return of(null);
      })
    );
  }

  /**
   * Get the current risk assessment for a patient (legacy method)
   * @param patientId Patient FHIR ID
   * @returns Observable of RiskAssessment or null if not found
   * @deprecated Use getHccRiskAssessment() for HCC-based assessments
   */
  getRiskAssessment(patientId: string): Observable<RiskAssessment | null> {
    if (!patientId || patientId.trim() === '') {
      return throwError(() => new Error('Patient ID is required'));
    }

    const url = `/api/patients/${patientId}/risk-assessment`;

    return this.apiService.get<RiskAssessment>(url).pipe(
      map((assessment) => this.transformAssessment(assessment)),
      catchError((error) => {
        this.loggerService.error(`Error fetching risk assessment for patient ${patientId}`, error);
        return of(null);
      })
    );
  }

  /**
   * Get risk assessment history for a patient
   * @param patientId Patient FHIR ID
   * @returns Observable of RiskAssessment array (ordered by date, most recent first)
   */
  getRiskHistory(patientId: string): Observable<RiskAssessment[]> {
    if (!patientId || patientId.trim() === '') {
      return throwError(() => new Error('Patient ID is required'));
    }

    const url = `/api/patients/${patientId}/risk-history`;

    return this.apiService.get<RiskAssessment[]>(url).pipe(
      map((history) => history.map((assessment) => this.transformAssessment(assessment))),
      catchError((error) => {
        this.loggerService.error(`Error fetching risk history for patient ${patientId}`, error);
        return of([]);
      })
    );
  }

  /**
   * Get risk assessment for a specific category
   * @param patientId Patient FHIR ID
   * @param category Risk category (e.g., CARDIOVASCULAR, DIABETES)
   * @returns Observable of RiskAssessment or null if not found
   */
  getRiskByCategory(
    patientId: string,
    category: string
  ): Observable<RiskAssessment | null> {
    if (!patientId || patientId.trim() === '' || !category || category.trim() === '') {
      return throwError(() => new Error('Patient ID and category are required'));
    }

    const url = `/api/patients/${patientId}/risk-by-category/${category}`;

    return this.apiService.get<RiskAssessment>(url).pipe(
      map((assessment) => this.transformAssessment(assessment)),
      catchError((error) => {
        this.loggerService.error(`Error fetching ${category} risk assessment for patient ${patientId}`, error);
        return of(null);
      })
    );
  }

  /**
   * Trigger risk recalculation for a patient
   * @param patientId Patient FHIR ID
   * @returns Observable of updated RiskAssessment
   */
  recalculateRisk(patientId: string): Observable<RiskAssessment> {
    if (!patientId || patientId.trim() === '') {
      return throwError(() => new Error('Patient ID is required'));
    }

    const url = `/api/patients/${patientId}/recalculate-risk`;

    return this.apiService.post<RiskAssessment>(url, {}).pipe(
      map((assessment) => this.transformAssessment(assessment)),
      catchError((error) => {
        this.loggerService.error(`Error recalculating risk for patient ${patientId}`, error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Get population-level risk statistics
   * @returns Observable of PopulationStats
   */
  getPopulationStats(): Observable<PopulationStats> {
    const url = '/api/risk/population-stats';

    return this.apiService.get<PopulationStats>(url).pipe(
      catchError((error) => {
        this.loggerService.error('Error fetching population statistics', error);
        return of(this.getEmptyStats());
      })
    );
  }

  // ==========================================================================
  // Private Helper Methods
  // ==========================================================================

  /**
   * Transform assessment data (convert date strings to Date objects)
   */
  private transformAssessment(assessment: any): RiskAssessment {
    return {
      ...assessment,
      assessmentDate: assessment.assessmentDate
        ? new Date(assessment.assessmentDate)
        : new Date(),
      createdAt: assessment.createdAt
        ? new Date(assessment.createdAt)
        : new Date(),
    };
  }

  /**
   * Transform HCC assessment data
   */
  private transformHccAssessment(assessment: any): HccRiskAssessment {
    return {
      ...assessment,
      // Ensure arrays are initialized
      topHccs: assessment.topHccs || [],
      chronicConditions: assessment.chronicConditions || [],
      // Ensure data availability is present
      dataAvailability: assessment.dataAvailability || {
        hccDataAvailable: false,
        careGapDataAvailable: false,
        documentationGapDataAvailable: false,
      },
    };
  }

  /**
   * Get empty population stats (for error handling)
   */
  private getEmptyStats(): PopulationStats {
    return {
      totalPatients: 0,
      riskLevelDistribution: {
        low: 0,
        moderate: 0,
        high: 0,
        'very-high': 0,
      },
    };
  }
}
