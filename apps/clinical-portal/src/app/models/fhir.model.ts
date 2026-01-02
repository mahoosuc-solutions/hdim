/**
 * FHIR Resource Models
 *
 * TypeScript interfaces for FHIR R4 resources used in the clinical portal
 */

/**
 * FHIR Bundle - Container for a collection of resources
 */
export interface FhirBundle<T = any> {
  resourceType: 'Bundle';
  type: 'searchset' | 'collection' | 'batch-response' | 'transaction-response';
  total?: number;
  link?: FhirBundleLink[];
  entry?: FhirBundleEntry<T>[];
}

export interface FhirBundleLink {
  relation: 'self' | 'next' | 'previous' | 'first' | 'last';
  url: string;
}

export interface FhirBundleEntry<T = any> {
  fullUrl?: string;
  resource?: T;
  search?: {
    mode: 'match' | 'include';
    score?: number;
  };
}

/**
 * FHIR Coding - A reference to a code defined by a terminology system
 */
export interface FhirCoding {
  system?: string;
  version?: string;
  code?: string;
  display?: string;
  userSelected?: boolean;
}

/**
 * FHIR CodeableConcept - A concept that may be defined by a formal reference to a terminology
 */
export interface FhirCodeableConcept {
  coding?: FhirCoding[];
  text?: string;
}

/**
 * FHIR Quantity - A measured amount
 */
export interface FhirQuantity {
  value?: number;
  comparator?: '<' | '<=' | '>=' | '>';
  unit?: string;
  system?: string;
  code?: string;
}

/**
 * FHIR Reference - A reference from one resource to another
 */
export interface FhirReference {
  reference?: string;
  type?: string;
  identifier?: FhirIdentifier;
  display?: string;
}

/**
 * FHIR Identifier - An identifier intended for computation
 */
export interface FhirIdentifier {
  use?: 'usual' | 'official' | 'temp' | 'secondary' | 'old';
  system?: string;
  value?: string;
}

/**
 * FHIR Observation - Measurements and simple assertions
 */
export interface FhirObservation {
  resourceType: 'Observation';
  id?: string;
  status: 'registered' | 'preliminary' | 'final' | 'amended' | 'corrected' | 'cancelled' | 'entered-in-error' | 'unknown';
  category?: FhirCodeableConcept[];
  code: FhirCodeableConcept;
  subject?: FhirReference;
  effectiveDateTime?: string;
  effectivePeriod?: {
    start?: string;
    end?: string;
  };
  issued?: string;
  valueQuantity?: FhirQuantity;
  valueCodeableConcept?: FhirCodeableConcept;
  valueString?: string;
  valueBoolean?: boolean;
  valueInteger?: number;
  valueRange?: {
    low?: FhirQuantity;
    high?: FhirQuantity;
  };
  interpretation?: FhirCodeableConcept[];
  note?: Array<{
    text: string;
  }>;
  referenceRange?: FhirObservationReferenceRange[];
  component?: FhirObservationComponent[];
}

export interface FhirObservationReferenceRange {
  low?: FhirQuantity;
  high?: FhirQuantity;
  type?: FhirCodeableConcept;
  appliesTo?: FhirCodeableConcept[];
  age?: {
    low?: FhirQuantity;
    high?: FhirQuantity;
  };
  text?: string;
}

export interface FhirObservationComponent {
  code: FhirCodeableConcept;
  valueQuantity?: FhirQuantity;
  valueCodeableConcept?: FhirCodeableConcept;
  valueString?: string;
  interpretation?: FhirCodeableConcept[];
  referenceRange?: FhirObservationReferenceRange[];
}

/**
 * FHIR DiagnosticReport - A diagnostic report
 */
export interface FhirDiagnosticReport {
  resourceType: 'DiagnosticReport';
  id?: string;
  status: 'registered' | 'partial' | 'preliminary' | 'final' | 'amended' | 'corrected' | 'appended' | 'cancelled' | 'entered-in-error' | 'unknown';
  category?: FhirCodeableConcept[];
  code: FhirCodeableConcept;
  subject?: FhirReference;
  effectiveDateTime?: string;
  effectivePeriod?: {
    start?: string;
    end?: string;
  };
  issued?: string;
  result?: FhirReference[];
  conclusion?: string;
  conclusionCode?: FhirCodeableConcept[];
}

/**
 * FHIR Condition - A clinical condition, problem, diagnosis, or other event
 */
export interface FhirCondition {
  resourceType: 'Condition';
  id?: string;
  clinicalStatus?: FhirCodeableConcept;
  verificationStatus?: FhirCodeableConcept;
  category?: FhirCodeableConcept[];
  severity?: FhirCodeableConcept;
  code?: FhirCodeableConcept;
  subject: FhirReference;
  onsetDateTime?: string;
  onsetAge?: FhirQuantity;
  onsetPeriod?: {
    start?: string;
    end?: string;
  };
  recordedDate?: string;
  note?: Array<{
    text: string;
  }>;
}

/**
 * FHIR OperationOutcome - Information about the success/failure of an operation
 */
export interface FhirOperationOutcome {
  resourceType: 'OperationOutcome';
  issue: FhirOperationOutcomeIssue[];
}

export interface FhirOperationOutcomeIssue {
  severity: 'fatal' | 'error' | 'warning' | 'information';
  code: string;
  details?: FhirCodeableConcept;
  diagnostics?: string;
  location?: string[];
  expression?: string[];
}

/**
 * FHIR QuestionnaireResponse - A structured set of answers to a questionnaire
 */
export interface FhirQuestionnaireResponse {
  resourceType: 'QuestionnaireResponse';
  id?: string;
  questionnaire: string;
  status: 'completed' | 'in-progress' | 'amended' | 'entered-in-error' | 'stopped';
  subject?: FhirReference;
  authored: string;
  author?: FhirReference;
  item: FhirQuestionnaireResponseItem[];
}

export interface FhirQuestionnaireResponseItem {
  linkId: string;
  text?: string;
  answer?: FhirQuestionnaireResponseAnswer[];
  item?: FhirQuestionnaireResponseItem[];
}

export interface FhirQuestionnaireResponseAnswer {
  valueCoding?: FhirCoding;
  valueString?: string;
  valueInteger?: number;
  valueBoolean?: boolean;
  valueDate?: string;
  valueDateTime?: string;
  item?: FhirQuestionnaireResponseItem[];
}

/**
 * Common LOINC codes for vital signs
 */
export const LOINC_VITAL_SIGNS = {
  HEART_RATE: '8867-4',
  BLOOD_PRESSURE_PANEL: '85354-9',
  SYSTOLIC_BP: '8480-6',
  DIASTOLIC_BP: '8462-4',
  BODY_TEMPERATURE: '8310-5',
  BODY_WEIGHT: '29463-7',
  BODY_HEIGHT: '8302-2',
  BMI: '39156-5',
  OXYGEN_SATURATION: '2708-6',
  RESPIRATORY_RATE: '9279-1',
} as const;

/**
 * Common LOINC codes for laboratory tests
 */
export const LOINC_LAB_TESTS = {
  HBA1C: '4548-4',
  GLUCOSE: '2339-0',
  LDL_CHOLESTEROL: '18262-6',
  HDL_CHOLESTEROL: '2085-9',
  TOTAL_CHOLESTEROL: '2093-3',
  TRIGLYCERIDES: '2571-8',
  CREATININE: '2160-0',
  EGFR: '98979-8',
  ALT: '1742-6',
  AST: '1920-8',
  WBC: '6690-2',
  HEMOGLOBIN: '718-7',
  PLATELETS: '777-3',
} as const;

/**
 * FHIR Observation category codes
 */
export const OBSERVATION_CATEGORIES = {
  VITAL_SIGNS: 'vital-signs',
  LABORATORY: 'laboratory',
  SOCIAL_HISTORY: 'social-history',
  SURVEY: 'survey',
  IMAGING: 'imaging',
  PROCEDURE: 'procedure',
  ACTIVITY: 'activity',
} as const;

/**
 * Lab panel definitions with LOINC codes
 */
export const LOINC_LAB_PANELS = {
  CBC: {
    panelCode: '58410-2',
    components: ['6690-2', '789-8', '718-7', '4544-3', '777-3']
  },
  BMP: {
    panelCode: '51990-0',
    components: ['2339-0', '3094-0', '2160-0', '2951-2', '2823-3', '2075-0', '1963-8']
  },
  LIPID: {
    panelCode: '57698-3',
    components: ['2093-3', '18262-6', '2085-9', '2571-8']
  }
} as const;

/**
 * FHIR interpretation codes mapping
 */
export const FHIR_INTERPRETATION_CODES: Record<string, { display: string; severity: string }> = {
  'N': { display: 'Normal', severity: 'normal' },
  'L': { display: 'Low', severity: 'low' },
  'H': { display: 'High', severity: 'high' },
  'LL': { display: 'Critical Low', severity: 'critical-low' },
  'HH': { display: 'Critical High', severity: 'critical-high' },
  'A': { display: 'Abnormal', severity: 'abnormal' }
};

/**
 * FHIR MedicationStatement - Record of medication being taken by a patient
 */
export interface FhirMedicationStatement {
  resourceType: 'MedicationStatement';
  id?: string;
  status: 'active' | 'completed' | 'entered-in-error' | 'intended' | 'stopped' | 'on-hold' | 'unknown' | 'not-taken';
  medicationCodeableConcept?: FhirCodeableConcept;
  medicationReference?: FhirReference;
  subject?: FhirReference;
  effectiveDateTime?: string;
  effectivePeriod?: {
    start?: string;
    end?: string;
  };
  dateAsserted?: string;
  dosage?: Array<{
    text?: string;
    timing?: any;
    route?: FhirCodeableConcept;
    doseAndRate?: Array<{
      type?: FhirCodeableConcept;
      doseQuantity?: FhirQuantity;
    }>;
  }>;
}

/**
 * FHIR ServiceRequest - A request for a service to be performed
 */
export interface FhirServiceRequest {
  resourceType: 'ServiceRequest';
  id?: string;
  status: 'draft' | 'active' | 'on-hold' | 'revoked' | 'completed' | 'entered-in-error' | 'unknown';
  intent: 'proposal' | 'plan' | 'directive' | 'order' | 'original-order' | 'reflex-order' | 'filler-order' | 'instance-order' | 'option';
  category?: FhirCodeableConcept[];
  priority?: 'routine' | 'urgent' | 'asap' | 'stat';
  code?: FhirCodeableConcept;
  subject: FhirReference;
  authoredOn?: string;
  requester?: FhirReference;
  performer?: FhirReference[];
  occurrenceDateTime?: string;
  occurrencePeriod?: {
    start?: string;
    end?: string;
  };
  note?: Array<{
    text: string;
  }>;
}
