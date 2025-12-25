import { CodeableConcept } from '../../models/patient.model';

/**
 * FHIR Utility Functions
 *
 * Helper functions for working with FHIR data types
 */

/**
 * Extract the primary code string from a CodeableConcept or string
 * Returns the first coding's code, or the string itself, or empty string if not found
 */
export function getCodeFromConcept(concept: CodeableConcept | string | undefined | null): string {
  if (!concept) return '';
  if (typeof concept === 'string') return concept;
  if (!concept.coding?.length) return '';
  return concept.coding[0]?.code || '';
}

/**
 * Extract all code strings from a CodeableConcept or string
 */
export function getAllCodesFromConcept(concept: CodeableConcept | string | undefined | null): string[] {
  if (!concept) return [];
  if (typeof concept === 'string') return [concept];
  if (!concept.coding) return [];
  return concept.coding
    .map(coding => coding.code)
    .filter((code): code is string => !!code);
}

/**
 * Check if a CodeableConcept or string contains a specific code
 */
export function conceptContainsCode(concept: CodeableConcept | string | undefined | null, code: string): boolean {
  return getAllCodesFromConcept(concept).includes(code);
}

/**
 * Check if a CodeableConcept or string contains any of the specified codes
 */
export function conceptContainsAnyCode(concept: CodeableConcept | string | undefined | null, codes: string[]): boolean {
  const conceptCodes = getAllCodesFromConcept(concept);
  return codes.some(code => conceptCodes.includes(code));
}

/**
 * Check if a CodeableConcept's or string's code starts with a prefix
 */
export function conceptCodeStartsWith(concept: CodeableConcept | string | undefined | null, prefix: string): boolean {
  return getAllCodesFromConcept(concept).some(code => code.startsWith(prefix));
}

/**
 * Check if a CodeableConcept's or string's code includes a substring
 */
export function conceptCodeIncludes(concept: CodeableConcept | string | undefined | null, substring: string): boolean {
  return getAllCodesFromConcept(concept).some(code => code.includes(substring));
}

/**
 * Get display text from a CodeableConcept or string
 * Prefers text, then first coding display, or returns string directly
 */
export function getDisplayFromConcept(concept: CodeableConcept | string | undefined | null): string {
  if (!concept) return '';
  if (typeof concept === 'string') return concept;
  if (concept.text) return concept.text;
  if (concept.coding?.length && concept.coding[0]?.display) {
    return concept.coding[0].display;
  }
  return '';
}
