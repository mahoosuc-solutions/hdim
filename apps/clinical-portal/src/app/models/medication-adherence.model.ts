/**
 * Medication Adherence Models
 *
 * Models for tracking medication adherence using PDC (Proportion of Days Covered)
 * and identifying adherence gaps.
 */

/**
 * Date Range for calculating adherence over a period
 */
export interface DateRange {
  startDate: Date;
  endDate: Date;
}

/**
 * PDC (Proportion of Days Covered) Result
 * Measures medication adherence as a percentage of days covered
 */
export interface PDCResult {
  medicationCode: string;
  medicationName: string;
  pdc: number;
  status: 'excellent' | 'good' | 'poor';
  daysCovered: number;
  totalDays: number;
  gaps: AdherenceGap[];
}

/**
 * Gap in medication adherence
 */
export interface AdherenceGap {
  startDate: Date;
  endDate: Date;
  daysWithout: number;
}

/**
 * Overall medication adherence score across all medications
 */
export interface MedicationAdherenceScore {
  overallPDC: number;
  adherentCount: number;
  totalMedications: number;
  problematicMedications: string[];
}

/**
 * Medication with poor adherence requiring intervention
 */
export interface ProblematicMedication {
  medicationCode: string;
  medicationName: string;
  pdc: number;
  daysOverdue: number;
  recommendation: string;
}

/**
 * FHIR MedicationRequest resource (simplified)
 */
export interface MedicationRequest {
  id?: string;
  status: string;
  medicationCodeableConcept?: {
    coding?: Array<{
      system?: string;
      code?: string;
      display?: string;
    }>;
    text?: string;
  };
  dosageInstruction?: Array<{
    text?: string;
    timing?: any;
  }>;
  dispenseRequest?: {
    expectedSupplyDuration?: {
      value?: number;
      unit?: string;
    };
    quantity?: {
      value?: number;
    };
  };
  authoredOn?: string;
}
