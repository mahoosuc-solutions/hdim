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
} from '../models/risk-assessment.model';

@Injectable({
  providedIn: 'root',
})
export class RiskAssessmentService {
  constructor(private apiService: ApiService) {}

  /**
   * Get the current risk assessment for a patient
   * @param patientId Patient FHIR ID
   * @returns Observable of RiskAssessment or null if not found
   */
  getRiskAssessment(patientId: string): Observable<RiskAssessment | null> {
    if (!patientId || patientId.trim() === '') {
      return throwError(() => new Error('Patient ID is required'));
    }

    const url = `/api/patients/${patientId}/risk-assessment`;

    return this.apiService.get<RiskAssessment>(url).pipe(
      map((assessment) => this.transformAssessment(assessment)),
      catchError((error) => {
        console.error(
          `Error fetching risk assessment for patient ${patientId}:`,
          error
        );
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
        console.error(
          `Error fetching risk history for patient ${patientId}:`,
          error
        );
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
        console.error(
          `Error fetching ${category} risk assessment for patient ${patientId}:`,
          error
        );
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
        console.error(
          `Error recalculating risk for patient ${patientId}:`,
          error
        );
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
        console.error('Error fetching population statistics:', error);
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
