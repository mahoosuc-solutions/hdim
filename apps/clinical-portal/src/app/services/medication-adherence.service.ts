/**
 * Medication Adherence Service
 *
 * Service for calculating medication adherence using PDC (Proportion of Days Covered)
 * and identifying adherence gaps from FHIR MedicationRequest resources.
 */

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, forkJoin, of } from 'rxjs';
import { map, catchError, switchMap } from 'rxjs/operators';
import { LoggerService } from '../logger.service';
import {
  PDCResult,
  MedicationAdherenceScore,
  AdherenceGap,
  ProblematicMedication,
  DateRange,
  MedicationRequest,
} from '../models/medication-adherence.model';
import {
  API_CONFIG,
  FHIR_ENDPOINTS,
  buildFhirUrl,
  HTTP_HEADERS,
} from '../config/api.config';

/**
 * Interface for medication dispense records
 */
interface MedicationDispense {
  id?: string;
  whenHandedOver?: string;
  daysSupply?: {
    value?: number;
  };
}

/**
 * Interface for covered period
 */
interface CoveredPeriod {
  startDate: Date;
  endDate: Date;
}

@Injectable({
  providedIn: 'root',
})
export class MedicationAdherenceService {
  private readonly logger: any;

  constructor(
    private http: HttpClient,
    private loggerService: LoggerService
  ) {
    this.logger = this.loggerService.withContext(\'MedicationAdherenceService');}

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
   * Get active medications for a patient
   * @param patientId Patient ID
   * @returns Observable of MedicationRequest array
   */
  getActiveMedications(patientId: string): Observable<MedicationRequest[]> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.MEDICATION);
    const params = new HttpParams()
      .set('patient', patientId)
      .set('status', 'active');

    return this.http
      .get<any>(url, {
        headers: this.getHeaders(),
        params,
      })
      .pipe(
        map((bundle) => {
          if (!bundle.entry || bundle.entry.length === 0) {
            return [];
          }

          return bundle.entry.map((entry: any) => entry.resource as MedicationRequest);
        }),
        catchError((error) => {
          this.logger.error('Error fetching active medications', { error });
          return of([]);
        })
      );
  }

  /**
   * Calculate PDC (Proportion of Days Covered) for a medication
   * @param patientId Patient ID
   * @param medicationCode Medication code (e.g., RxNorm)
   * @param period Date range for calculation
   * @returns Observable of PDCResult
   */
  calculatePDC(
    patientId: string,
    medicationCode: string,
    period: DateRange
  ): Observable<PDCResult> {
    return this.getMedicationDispenses(patientId, medicationCode, period).pipe(
      map((dispenses) => {
        const totalDays = this.calculateTotalDays(period.startDate, period.endDate);
        const coveredPeriods = this.buildCoveredPeriods(dispenses, period);
        const daysCovered = this.calculateDaysCovered(coveredPeriods);
        const pdc = Math.round((daysCovered / totalDays) * 100);
        const status = this.determinePDCStatus(pdc);
        const gaps = this.calculateGapsFromCoveredPeriods(coveredPeriods, period);

        return {
          medicationCode,
          medicationName: this.getMedicationNameFromDispenses(dispenses) || medicationCode,
          pdc,
          status,
          daysCovered,
          totalDays,
          gaps,
        };
      })
    );
  }

  /**
   * Calculate overall adherence across all active medications
   * @param patientId Patient ID
   * @returns Observable of MedicationAdherenceScore
   */
  calculateOverallAdherence(patientId: string): Observable<MedicationAdherenceScore> {
    const period: DateRange = {
      startDate: new Date(new Date().setDate(new Date().getDate() - 30)),
      endDate: new Date(),
    };

    return this.getActiveMedications(patientId).pipe(
      switchMap((medications) => {
        if (medications.length === 0) {
          return of({
            overallPDC: 0,
            adherentCount: 0,
            totalMedications: 0,
            problematicMedications: [],
          });
        }

        // Calculate PDC for each medication
        const pdcCalculations = medications.map((med) => {
          const medicationCode = this.extractMedicationCode(med);
          return this.calculatePDC(patientId, medicationCode, period);
        });

        // Wait for all PDC calculations
        return forkJoin(pdcCalculations);
      }),
      map((pdcResults) => {
        if (!Array.isArray(pdcResults) || pdcResults.length === 0) {
          return {
            overallPDC: 0,
            adherentCount: 0,
            totalMedications: 0,
            problematicMedications: [],
          };
        }

        const totalMedications = pdcResults.length;
        const totalPDC = pdcResults.reduce((sum: number, result: PDCResult) => sum + result.pdc, 0);
        const overallPDC = Math.round(totalPDC / totalMedications);
        const adherentCount = pdcResults.filter((result: PDCResult) => result.pdc >= 80).length;
        const problematicMedications = pdcResults
          .filter((result: PDCResult) => result.pdc < 80)
          .map((result: PDCResult) => result.medicationName);

        return {
          overallPDC,
          adherentCount,
          totalMedications,
          problematicMedications,
        };
      }),
      catchError((error) => {
        this.logger.error('Error calculating overall adherence', { error });
        return of({
          overallPDC: 0,
          adherentCount: 0,
          totalMedications: 0,
          problematicMedications: [],
        });
      })
    );
  }

  /**
   * Identify adherence gaps for a medication
   * @param patientId Patient ID
   * @param medicationCode Medication code
   * @param period Date range for analysis
   * @returns Observable of AdherenceGap array
   */
  identifyAdherenceGaps(
    patientId: string,
    medicationCode: string,
    period: DateRange
  ): Observable<AdherenceGap[]> {
    return this.getMedicationDispenses(patientId, medicationCode, period).pipe(
      map((dispenses) => {
        const coveredPeriods = this.buildCoveredPeriods(dispenses, period);
        return this.calculateGapsFromCoveredPeriods(coveredPeriods, period);
      })
    );
  }

  /**
   * Get medications with poor adherence (PDC < 80%)
   * @param patientId Patient ID
   * @returns Observable of ProblematicMedication array
   */
  getProblematicMedications(patientId: string): Observable<ProblematicMedication[]> {
    const period: DateRange = {
      startDate: new Date(new Date().setDate(new Date().getDate() - 30)),
      endDate: new Date(),
    };

    return this.getActiveMedications(patientId).pipe(
      switchMap((medications) => {
        if (medications.length === 0) {
          return of([]);
        }

        // Calculate PDC for each medication
        const pdcCalculations = medications.map((med) => {
          const medicationCode = this.extractMedicationCode(med);
          const medicationName = this.extractMedicationName(med);
          return this.calculatePDC(patientId, medicationCode, period).pipe(
            map((pdcResult) => ({
              medicationCode,
              medicationName,
              pdcResult,
            }))
          );
        });

        return forkJoin(pdcCalculations);
      }),
      map((results) => {
        if (!Array.isArray(results) || results.length === 0) {
          return [];
        }

        const problematic = results
          .filter((result) => result.pdcResult.pdc < 80)
          .map((result) => {
            const daysOverdue = this.calculateDaysOverdue(result.pdcResult);
            const recommendation = this.generateRecommendation(result.pdcResult.pdc, daysOverdue);

            return {
              medicationCode: result.medicationCode,
              medicationName: result.medicationName,
              pdc: result.pdcResult.pdc,
              daysOverdue,
              recommendation,
            };
          });

        // Sort by lowest PDC first
        problematic.sort((a, b) => a.pdc - b.pdc);

        return problematic;
      }),
      catchError((error) => {
        this.logger.error('Error getting problematic medications', { error });
        return of([]);
      })
    );
  }

  // ==========================================================================
  // Private Helper Methods
  // ==========================================================================

  /**
   * Get medication dispenses from FHIR server
   */
  private getMedicationDispenses(
    patientId: string,
    medicationCode: string,
    period: DateRange
  ): Observable<MedicationDispense[]> {
    const url = buildFhirUrl(FHIR_ENDPOINTS.MEDICATION);
    const params = new HttpParams()
      .set('patient', patientId)
      .set('medication', medicationCode);

    return this.http
      .get<any>(url, {
        headers: this.getHeaders(),
        params,
      })
      .pipe(
        map((bundle) => {
          if (!bundle.entry || bundle.entry.length === 0) {
            return [];
          }

          return bundle.entry.map((entry: any) => entry.resource as MedicationDispense);
        }),
        catchError((error) => {
          this.logger.error('Error fetching medication dispenses', { error });
          return of([]);
        })
      );
  }

  /**
   * Extract medication code from MedicationRequest
   */
  private extractMedicationCode(medication: MedicationRequest): string {
    if (medication.medicationCodeableConcept?.coding && medication.medicationCodeableConcept.coding.length > 0) {
      return medication.medicationCodeableConcept.coding[0].code || 'unknown';
    }
    return medication.id || 'unknown';
  }

  /**
   * Extract medication name from MedicationRequest
   */
  private extractMedicationName(medication: MedicationRequest): string {
    if (medication.medicationCodeableConcept?.coding && medication.medicationCodeableConcept.coding.length > 0) {
      return medication.medicationCodeableConcept.coding[0].display || 'Unknown Medication';
    }
    if (medication.medicationCodeableConcept?.text) {
      return medication.medicationCodeableConcept.text;
    }
    return 'Unknown Medication';
  }

  /**
   * Get medication name from dispense records
   */
  private getMedicationNameFromDispenses(dispenses: MedicationDispense[]): string | null {
    // In real implementation, would extract from dispense record
    return null;
  }

  /**
   * Calculate total days in period
   */
  private calculateTotalDays(startDate: Date, endDate: Date): number {
    const diffTime = endDate.getTime() - startDate.getTime();
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
  }

  /**
   * Build covered periods from dispense records
   * Handles overlapping periods correctly
   */
  private buildCoveredPeriods(dispenses: MedicationDispense[], period: DateRange): CoveredPeriod[] {
    const periods: CoveredPeriod[] = [];

    for (const dispense of dispenses) {
      if (!dispense.whenHandedOver || !dispense.daysSupply?.value) {
        continue;
      }

      const fillDate = new Date(dispense.whenHandedOver);
      const daysSupply = dispense.daysSupply.value;
      const endDate = new Date(fillDate);
      endDate.setDate(endDate.getDate() + daysSupply - 1);

      // Constrain to analysis period
      const constrainedStart = fillDate < period.startDate ? period.startDate : fillDate;
      const constrainedEnd = endDate > period.endDate ? period.endDate : endDate;

      if (constrainedStart <= constrainedEnd) {
        periods.push({
          startDate: constrainedStart,
          endDate: constrainedEnd,
        });
      }
    }

    // Sort by start date
    periods.sort((a, b) => a.startDate.getTime() - b.startDate.getTime());

    // Merge overlapping periods
    return this.mergeOverlappingPeriods(periods);
  }

  /**
   * Merge overlapping periods to avoid double-counting
   */
  private mergeOverlappingPeriods(periods: CoveredPeriod[]): CoveredPeriod[] {
    if (periods.length === 0) {
      return [];
    }

    const merged: CoveredPeriod[] = [periods[0]];

    for (let i = 1; i < periods.length; i++) {
      const current = periods[i];
      const last = merged[merged.length - 1];

      // Check if current period overlaps with last merged period
      if (current.startDate <= last.endDate) {
        // Extend the last period if current extends beyond it
        if (current.endDate > last.endDate) {
          last.endDate = current.endDate;
        }
      } else {
        // No overlap, add as new period
        merged.push(current);
      }
    }

    return merged;
  }

  /**
   * Calculate total days covered from covered periods
   */
  private calculateDaysCovered(coveredPeriods: CoveredPeriod[]): number {
    let totalDays = 0;

    for (const period of coveredPeriods) {
      const days = this.calculateTotalDays(period.startDate, period.endDate);
      totalDays += days;
    }

    return totalDays;
  }

  /**
   * Calculate gaps from covered periods
   */
  private calculateGapsFromCoveredPeriods(
    coveredPeriods: CoveredPeriod[],
    analysisPeriod: DateRange
  ): AdherenceGap[] {
    const gaps: AdherenceGap[] = [];

    if (coveredPeriods.length === 0) {
      // Entire period is a gap
      gaps.push({
        startDate: analysisPeriod.startDate,
        endDate: analysisPeriod.endDate,
        daysWithout: this.calculateTotalDays(analysisPeriod.startDate, analysisPeriod.endDate),
      });
      return gaps;
    }

    // Check for gap at start
    const firstPeriod = coveredPeriods[0];
    if (firstPeriod.startDate > analysisPeriod.startDate) {
      const gapEnd = new Date(firstPeriod.startDate);
      gapEnd.setDate(gapEnd.getDate() - 1);
      gaps.push({
        startDate: analysisPeriod.startDate,
        endDate: gapEnd,
        daysWithout: this.calculateTotalDays(analysisPeriod.startDate, gapEnd),
      });
    }

    // Check for gaps between periods
    for (let i = 0; i < coveredPeriods.length - 1; i++) {
      const currentPeriod = coveredPeriods[i];
      const nextPeriod = coveredPeriods[i + 1];

      const gapStart = new Date(currentPeriod.endDate);
      gapStart.setDate(gapStart.getDate() + 1);

      const gapEnd = new Date(nextPeriod.startDate);
      gapEnd.setDate(gapEnd.getDate() - 1);

      if (gapStart <= gapEnd) {
        gaps.push({
          startDate: gapStart,
          endDate: gapEnd,
          daysWithout: this.calculateTotalDays(gapStart, gapEnd),
        });
      }
    }

    // Check for gap at end
    const lastPeriod = coveredPeriods[coveredPeriods.length - 1];
    if (lastPeriod.endDate < analysisPeriod.endDate) {
      const gapStart = new Date(lastPeriod.endDate);
      gapStart.setDate(gapStart.getDate() + 1);
      gaps.push({
        startDate: gapStart,
        endDate: analysisPeriod.endDate,
        daysWithout: this.calculateTotalDays(gapStart, analysisPeriod.endDate),
      });
    }

    return gaps;
  }

  /**
   * Determine PDC status based on percentage
   */
  private determinePDCStatus(pdc: number): 'excellent' | 'good' | 'poor' {
    if (pdc >= 80) {
      return 'excellent';
    } else if (pdc >= 60) {
      return 'good';
    } else {
      return 'poor';
    }
  }

  /**
   * Calculate days overdue from last fill
   */
  private calculateDaysOverdue(pdcResult: PDCResult): number {
    if (pdcResult.gaps.length === 0) {
      return 0;
    }

    // Find the most recent gap
    const mostRecentGap = pdcResult.gaps[pdcResult.gaps.length - 1];
    const today = new Date();

    // If the most recent gap extends to today or beyond
    if (mostRecentGap.endDate >= today) {
      return mostRecentGap.daysWithout;
    }

    return 0;
  }

  /**
   * Generate recommendation based on PDC and days overdue
   */
  private generateRecommendation(pdc: number, daysOverdue: number): string {
    if (pdc < 50) {
      return 'Critical adherence issue. Schedule urgent follow-up appointment to discuss barriers and provide adherence counseling.';
    } else if (pdc < 60) {
      return 'Poor adherence detected. Contact patient to identify barriers and provide support. Consider adherence aids or simplified regimen.';
    } else if (daysOverdue > 7) {
      return `Medication overdue by ${daysOverdue} days. Contact patient immediately to arrange refill and assess barriers.`;
    } else {
      return 'Moderate adherence concern. Provide patient education and monitor closely. Consider reminder system.';
    }
  }
}
