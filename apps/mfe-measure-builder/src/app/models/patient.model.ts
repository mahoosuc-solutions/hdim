/**
 * FHIR Patient Resource (Simplified)
 */
export interface Patient {
  resourceType: 'Patient';
  id: string;
  identifier?: Identifier[];
  name?: HumanName[];
  gender?: 'male' | 'female' | 'other' | 'unknown';
  birthDate?: string; // YYYY-MM-DD
  active?: boolean;
  telecom?: ContactPoint[];
  address?: Address[];
}

/**
 * FHIR Identifier
 */
export interface Identifier {
  system?: string; // e.g., "MRN"
  value: string; // e.g., "12345"
  type?: CodeableConcept;
}

/**
 * FHIR Human Name
 */
export interface HumanName {
  use?: 'official' | 'usual' | 'temp' | 'nickname' | 'anonymous' | 'old' | 'maiden';
  family?: string; // Last name
  given?: string[]; // First name(s)
  prefix?: string[];
  suffix?: string[];
  text?: string; // Full name as text
}

/**
 * FHIR Contact Point
 */
export interface ContactPoint {
  system?: 'phone' | 'fax' | 'email' | 'pager' | 'url' | 'sms' | 'other';
  value?: string;
  use?: 'home' | 'work' | 'temp' | 'old' | 'mobile';
}

/**
 * FHIR Address
 */
export interface Address {
  use?: 'home' | 'work' | 'temp' | 'old' | 'billing';
  type?: 'postal' | 'physical' | 'both';
  line?: string[];
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
}

/**
 * FHIR Codeable Concept
 */
export interface CodeableConcept {
  coding?: Coding[];
  text?: string;
}

/**
 * FHIR Coding
 */
export interface Coding {
  system?: string;
  code?: string;
  display?: string;
}

/**
 * Bundle response for multiple resources
 */
export interface Bundle<T> {
  resourceType: 'Bundle';
  type: 'searchset' | 'collection';
  total?: number;
  entry?: BundleEntry<T>[];
}

/**
 * Bundle Entry
 */
export interface BundleEntry<T> {
  fullUrl?: string;
  resource: T;
}

/**
 * Simplified Patient for UI display
 */
export interface PatientSummary {
  id: string;
  mrn?: string; // Medical Record Number
  mrnAssigningAuthority?: string; // Assigning authority (system)
  fullName: string;
  firstName?: string;
  lastName?: string;
  dateOfBirth?: string;
  age?: number;
  gender?: string;
  status: 'Active' | 'Inactive';
  lastEvaluationDate?: string;
}
