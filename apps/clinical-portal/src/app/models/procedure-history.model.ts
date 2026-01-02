/**
 * Procedure History Models
 *
 * TypeScript interfaces and types for procedure history tracking and categorization
 */

import { FhirCodeableConcept, FhirReference } from './fhir.model';

/**
 * Procedure category types for clinical classification
 */
export type ProcedureCategory = 'surgical' | 'imaging' | 'lab' | 'therapeutic' | 'preventive' | 'other';

/**
 * Procedure record with extracted and normalized data from FHIR
 */
export interface ProcedureRecord {
  id: string;
  code: string;
  codeSystem: 'SNOMED' | 'CPT';
  displayName: string;
  category: ProcedureCategory;
  performedDate: Date;
  status: string;
  performerName?: string;
  performerRole?: string;
  notes?: string;
}

/**
 * Categorized procedures organized by clinical type
 */
export interface CategorizedProcedures {
  surgical: ProcedureRecord[];
  imaging: ProcedureRecord[];
  lab: ProcedureRecord[];
  therapeutic: ProcedureRecord[];
  preventive: ProcedureRecord[];
  other: ProcedureRecord[];
  totalCount: number;
}

/**
 * Query options for filtering procedures
 */
export interface ProcedureQueryOptions {
  status?: string;
  startDate?: Date;
  endDate?: Date;
  limit?: number;
}

/**
 * FHIR Procedure Resource (R4)
 */
export interface FhirProcedure {
  resourceType: 'Procedure';
  id?: string;
  status: 'preparation' | 'in-progress' | 'not-done' | 'on-hold' | 'stopped' | 'completed' | 'entered-in-error' | 'unknown';
  code?: FhirCodeableConcept;
  subject?: FhirReference;
  performedDateTime?: string;
  performedPeriod?: {
    start?: string;
    end?: string;
  };
  performer?: FhirProcedurePerformer[];
  note?: Array<{
    text: string;
  }>;
}

/**
 * FHIR Procedure Performer
 */
export interface FhirProcedurePerformer {
  function?: FhirCodeableConcept;
  actor: FhirReference;
}

/**
 * Procedure categorization mapping constants
 * Based on SNOMED CT and CPT code ranges
 */
export const PROCEDURE_CATEGORY_MAPPING = {
  surgical: {
    snomedRanges: [[387713003, 387799999]], // Surgical procedures
    cptRanges: [[10000, 69999]]
  },
  imaging: {
    snomedCodes: [168537006, 77477000, 241615005], // X-ray, CT, MRI
    cptRanges: [[70000, 79999]]
  },
  lab: {
    snomedCodes: [396550006, 86273004], // Blood draw, biopsy
    cptRanges: [[80000, 89999]]
  },
  preventive: {
    snomedCodes: [33879002, 268556000], // Vaccination, screening
    cptRanges: [[90000, 99999]]
  }
} as const;
