/**
 * Pact Configuration for Clinical Portal Consumer Contract Tests
 *
 * This module provides centralized configuration for Pact consumer contract
 * testing, including provider names, matchers, and factory functions for
 * creating Pact instances.
 *
 * @see https://docs.pact.io/implementation_guides/javascript
 */
import { PactV4, MatchersV3 } from '@pact-foundation/pact';
import * as path from 'path';

/**
 * Core Pact configuration for the Clinical Portal consumer.
 */
export const PACT_CONFIG = {
  /** Consumer application name - must match what provider tests expect */
  consumer: 'ClinicalPortal',

  /** Directory where Pact contract files will be written */
  pactDir: path.resolve(__dirname, '../pacts'),

  /** Logging level for Pact operations */
  logLevel: 'warn' as const,
};

/**
 * Provider names that Clinical Portal consumes.
 *
 * These names MUST match the @Provider annotations in the backend
 * provider verification tests exactly.
 */
export const PROVIDER_NAMES = {
  /** Patient Service - Patient demographics and health data */
  PATIENT_SERVICE: 'PatientService',

  /** Care Gap Service - Care gap detection and management */
  CARE_GAP_SERVICE: 'CareGapService',

  /** Quality Measure Service - HEDIS quality measure evaluation */
  QUALITY_MEASURE_SERVICE: 'QualityMeasureService',
};

/**
 * Test constants that match the provider state setup.
 *
 * These values are defined in PatientContractStateSetup.java and
 * CareGapContractStateSetup.java and must remain synchronized.
 */
export const TEST_CONSTANTS = {
  /** Test tenant ID for contract tests */
  TENANT_ID: 'test-tenant-contracts',

  /** John Doe patient UUID - FHIR-compliant string format */
  PATIENT_JOHN_DOE_ID: 'f47ac10b-58cc-4372-a567-0e02b2c3d479',

  /** Jane Smith patient UUID */
  PATIENT_JANE_SMITH_ID: 'a1b2c3d4-e5f6-7890-abcd-ef1234567890',

  /** Non-existent patient UUID for 404 tests */
  NON_EXISTENT_PATIENT_ID: '00000000-0000-0000-0000-000000000000',

  /** Test MRN for John Doe */
  MRN_JOHN_DOE: 'MRN-12345',

  /** Test MRN for Jane Smith */
  MRN_JANE_SMITH: 'MRN-67890',
};

/**
 * FHIR-compliant matchers for contract verification.
 *
 * These matchers ensure that contract tests validate FHIR-compliant
 * data formats while allowing flexibility in actual values.
 */
export const Matchers = {
  /**
   * Matches a FHIR-compliant UUID string.
   *
   * Format: 8-4-4-4-12 hexadecimal characters (lowercase)
   *
   * @param example - Example UUID to use in contract
   * @returns Pact regex matcher
   */
  uuid: (example = 'f47ac10b-58cc-4372-a567-0e02b2c3d479') =>
    MatchersV3.regex(
      /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/,
      example
    ),

  /**
   * Matches a FHIR date (YYYY-MM-DD format).
   *
   * @param example - Example date to use in contract
   * @returns Pact regex matcher
   */
  fhirDate: (example = '1980-05-15') =>
    MatchersV3.regex(/^\d{4}-\d{2}-\d{2}$/, example),

  /**
   * Matches an ISO 8601 datetime string.
   *
   * @param example - Example datetime to use in contract
   * @returns Pact datetime matcher
   */
  isoDateTime: (example = '2024-01-15T10:30:00Z') =>
    MatchersV3.datetime("yyyy-MM-dd'T'HH:mm:ss'Z'", example),

  /**
   * Matches the test tenant ID.
   *
   * @returns Pact string matcher
   */
  tenantId: () => MatchersV3.string(TEST_CONSTANTS.TENANT_ID),

  /**
   * Matches any non-empty string.
   *
   * @param example - Example string to use in contract
   * @returns Pact string matcher
   */
  nonEmptyString: (example = 'example') => MatchersV3.string(example),

  /**
   * Matches any integer value.
   *
   * @param example - Example integer to use in contract
   * @returns Pact integer matcher
   */
  integer: (example = 0) => MatchersV3.integer(example),

  /**
   * Matches any boolean value.
   *
   * @param example - Example boolean to use in contract
   * @returns Pact boolean matcher
   */
  boolean: (example = true) => MatchersV3.boolean(example),

  /**
   * Matches FHIR gender values.
   *
   * @param example - Example gender to use in contract
   * @returns Pact regex matcher
   */
  fhirGender: (example = 'male') =>
    MatchersV3.regex(/^(male|female|other|unknown)$/, example),
};

/**
 * Creates a new Pact V4 provider instance.
 *
 * @param providerName - Name of the provider service
 * @returns Configured PactV4 instance
 *
 * @example
 * ```typescript
 * const provider = createPactProvider(PROVIDER_NAMES.PATIENT_SERVICE);
 *
 * await provider
 *   .addInteraction()
 *   .given('patient exists')
 *   .uponReceiving('a request for patient data')
 *   .withRequest('GET', '/api/v1/patients/123')
 *   .willRespondWith(200, {})
 *   .executeTest(async (mockServer) => {
 *     // Test code using mockServer.url
 *   });
 * ```
 */
export function createPactProvider(providerName: string): PactV4 {
  return new PactV4({
    consumer: PACT_CONFIG.consumer,
    provider: providerName,
    dir: PACT_CONFIG.pactDir,
    logLevel: PACT_CONFIG.logLevel,
  });
}

/**
 * Common HTTP headers for contract tests.
 */
export const COMMON_HEADERS = {
  /** Content-Type header for JSON requests */
  contentType: { 'Content-Type': 'application/json' },

  /** Accept header for JSON responses */
  accept: { Accept: 'application/json' },

  /**
   * Creates headers with tenant ID.
   *
   * @param tenantId - Tenant ID to include
   * @returns Headers object with X-Tenant-ID
   */
  withTenant: (tenantId = TEST_CONSTANTS.TENANT_ID) => ({
    'Content-Type': 'application/json',
    'X-Tenant-ID': tenantId,
  }),
};
