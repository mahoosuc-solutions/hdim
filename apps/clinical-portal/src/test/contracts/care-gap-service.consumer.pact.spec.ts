/**
 * Care Gap Service Consumer Contract Tests
 *
 * Defines the contract expectations for the Clinical Portal's interactions
 * with the Care Gap Service. These contracts are verified by the Care Gap
 * Service provider tests.
 *
 * Provider States:
 * - "open care gaps exist for patient 550e8400-e29b-41d4-a716-446655440000"
 * - "care gap HBA1C exists"
 * - "care gap exists with id 550e8400-e29b-41d4-a716-446655440001"
 * - "care gap BCS exists"
 * - "no care gaps exist for patient 00000000-0000-0000-0000-000000000000"
 * - "care gap can be closed"
 *
 * @see CareGapServiceProviderTest (backend)
 * @see CareGapContractStateSetup (backend)
 */
import { MatchersV3 } from '@pact-foundation/pact';
import {
  createPactProvider,
  PROVIDER_NAMES,
  TEST_CONSTANTS,
  Matchers,
  COMMON_HEADERS,
} from '../../../pact/pact-config';
import { createFetchTestClient } from './pact-setup';

/**
 * Care Gap test constants matching CareGapContractStateSetup.java
 */
const CARE_GAP_CONSTANTS = {
  /** Patient ID for patient with open care gaps */
  PATIENT_WITH_GAPS_ID: '550e8400-e29b-41d4-a716-446655440000',

  /** Patient ID for patient with no care gaps */
  PATIENT_NO_GAPS_ID: '00000000-0000-0000-0000-000000000000',

  /** HBA1C care gap ID */
  CARE_GAP_HBA1C_ID: '550e8400-e29b-41d4-a716-446655440001',

  /** BCS care gap ID */
  CARE_GAP_BCS_ID: '550e8400-e29b-41d4-a716-446655440002',

  /** COL (closable) care gap ID */
  CARE_GAP_COL_ID: '550e8400-e29b-41d4-a716-446655440003',

  /** HEDIS measure IDs */
  MEASURE_HBA1C: 'CDC-HBA1C',
  MEASURE_BCS: 'BCS',
  MEASURE_COL: 'COL',
};

describe('Care Gap Service Consumer Contract', () => {
  // Create Pact provider for Care Gap Service
  const provider = createPactProvider(PROVIDER_NAMES.CARE_GAP_SERVICE);

  describe('GET /api/v1/care-gaps (by patient and status)', () => {
    describe('when open care gaps exist for patient', () => {
      it('should return list of open care gaps', async () => {
        // Define the expected care gap structure
        const expectedCareGap = {
          id: Matchers.uuid(CARE_GAP_CONSTANTS.CARE_GAP_HBA1C_ID),
          tenantId: Matchers.nonEmptyString(TEST_CONSTANTS.TENANT_ID),
          patientId: Matchers.uuid(CARE_GAP_CONSTANTS.PATIENT_WITH_GAPS_ID),
          measureId: Matchers.nonEmptyString(CARE_GAP_CONSTANTS.MEASURE_HBA1C),
          measureName: Matchers.nonEmptyString('Hemoglobin A1c Testing for Diabetics'),
          gapStatus: MatchersV3.regex(/^(OPEN|CLOSED)$/, 'OPEN'),
          gapCategory: Matchers.nonEmptyString('HEDIS'),
          priority: MatchersV3.regex(/^(high|medium|low)$/, 'high'),
          identifiedDate: MatchersV3.timestamp("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", '2024-01-15T10:30:00.000Z'),
          dueDate: Matchers.fhirDate('2024-02-15'),
        };

        // Paginated response structure
        const listResponse = {
          content: MatchersV3.eachLike(expectedCareGap, 1),
          totalElements: Matchers.integer(2),
          totalPages: Matchers.integer(1),
          number: Matchers.integer(0),
          size: Matchers.integer(20),
          first: Matchers.boolean(true),
          last: Matchers.boolean(true),
        };

        await provider
          .addInteraction()
          .given(`open care gaps exist for patient ${CARE_GAP_CONSTANTS.PATIENT_WITH_GAPS_ID}`)
          .uponReceiving('a request to get open care gaps for patient')
          .withRequest('GET', '/api/v1/care-gaps', (builder) => {
            builder
              .headers(COMMON_HEADERS.withTenant())
              .query({
                patientId: CARE_GAP_CONSTANTS.PATIENT_WITH_GAPS_ID,
                status: 'OPEN',
              });
          })
          .willRespondWith(200, (builder) => {
            builder
              .headers({ 'Content-Type': 'application/json' })
              .jsonBody(listResponse);
          })
          .executeTest(async (mockServer) => {
            const client = createFetchTestClient(mockServer.url);
            const response = await client.get<Record<string, unknown>>(
              `/api/v1/care-gaps?patientId=${CARE_GAP_CONSTANTS.PATIENT_WITH_GAPS_ID}&status=OPEN`
            );

            // Verify response structure
            expect(response).toBeDefined();
            expect(response['content']).toBeDefined();
            expect(Array.isArray(response['content'])).toBe(true);
            expect((response['content'] as unknown[]).length).toBeGreaterThan(0);

            // Verify care gap properties
            const firstGap = (response['content'] as Record<string, unknown>[])[0];
            expect(firstGap['patientId']).toBe(CARE_GAP_CONSTANTS.PATIENT_WITH_GAPS_ID);
            expect(firstGap['gapStatus']).toBe('OPEN');
          });
      });
    });

    describe('when no care gaps exist for patient', () => {
      it('should return empty content array', async () => {
        const emptyResponse = {
          content: [],
          totalElements: Matchers.integer(0),
          totalPages: Matchers.integer(0),
          number: Matchers.integer(0),
          size: Matchers.integer(20),
          first: Matchers.boolean(true),
          last: Matchers.boolean(true),
        };

        await provider
          .addInteraction()
          .given(`no care gaps exist for patient ${CARE_GAP_CONSTANTS.PATIENT_NO_GAPS_ID}`)
          .uponReceiving('a request to get care gaps for patient with no gaps')
          .withRequest('GET', '/api/v1/care-gaps', (builder) => {
            builder
              .headers(COMMON_HEADERS.withTenant())
              .query({
                patientId: CARE_GAP_CONSTANTS.PATIENT_NO_GAPS_ID,
                status: 'OPEN',
              });
          })
          .willRespondWith(200, (builder) => {
            builder
              .headers({ 'Content-Type': 'application/json' })
              .jsonBody(emptyResponse);
          })
          .executeTest(async (mockServer) => {
            const client = createFetchTestClient(mockServer.url);
            const response = await client.get<Record<string, unknown>>(
              `/api/v1/care-gaps?patientId=${CARE_GAP_CONSTANTS.PATIENT_NO_GAPS_ID}&status=OPEN`
            );

            // Verify empty results
            expect(response).toBeDefined();
            expect(response['content']).toEqual([]);
            expect(response['totalElements']).toBe(0);
          });
      });
    });
  });

  describe('GET /api/v1/care-gaps/:id', () => {
    describe('when care gap HBA1C exists', () => {
      it('should return the HBA1C care gap details', async () => {
        // Full care gap response structure
        const expectedCareGap = {
          id: Matchers.uuid(CARE_GAP_CONSTANTS.CARE_GAP_HBA1C_ID),
          tenantId: Matchers.nonEmptyString(TEST_CONSTANTS.TENANT_ID),
          patientId: Matchers.uuid(CARE_GAP_CONSTANTS.PATIENT_WITH_GAPS_ID),
          measureId: Matchers.nonEmptyString(CARE_GAP_CONSTANTS.MEASURE_HBA1C),
          measureName: Matchers.nonEmptyString('Hemoglobin A1c Testing for Diabetics'),
          gapCategory: Matchers.nonEmptyString('HEDIS'),
          measureYear: Matchers.integer(2024),
          gapType: Matchers.nonEmptyString('preventive-care'),
          gapStatus: MatchersV3.regex(/^(OPEN|CLOSED)$/, 'OPEN'),
          gapDescription: Matchers.nonEmptyString('Patient has not had HbA1c test in the measurement period'),
          gapReason: Matchers.nonEmptyString('No HbA1c test result found in the last 12 months'),
          priority: MatchersV3.regex(/^(high|medium|low)$/, 'high'),
          severity: MatchersV3.regex(/^(high|medium|low)$/, 'high'),
          identifiedDate: MatchersV3.timestamp("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", '2024-01-15T10:30:00.000Z'),
          dueDate: Matchers.fhirDate('2024-02-15'),
          recommendation: Matchers.nonEmptyString('Schedule HbA1c blood test'),
          recommendationType: Matchers.nonEmptyString('screening'),
          recommendedAction: Matchers.nonEmptyString('Order HbA1c test and schedule follow-up appointment'),
        };

        await provider
          .addInteraction()
          .given('care gap HBA1C exists')
          .uponReceiving('a request to get HBA1C care gap by ID')
          .withRequest('GET', `/api/v1/care-gaps/${CARE_GAP_CONSTANTS.CARE_GAP_HBA1C_ID}`, (builder) => {
            builder.headers(COMMON_HEADERS.withTenant());
          })
          .willRespondWith(200, (builder) => {
            builder
              .headers({ 'Content-Type': 'application/json' })
              .jsonBody(expectedCareGap);
          })
          .executeTest(async (mockServer) => {
            const client = createFetchTestClient(mockServer.url);
            const response = await client.get<Record<string, unknown>>(
              `/api/v1/care-gaps/${CARE_GAP_CONSTANTS.CARE_GAP_HBA1C_ID}`
            );

            // Verify care gap details
            expect(response).toBeDefined();
            expect(response['id']).toBe(CARE_GAP_CONSTANTS.CARE_GAP_HBA1C_ID);
            expect(response['measureId']).toBe(CARE_GAP_CONSTANTS.MEASURE_HBA1C);
            expect(response['gapStatus']).toBe('OPEN');
            expect(response['priority']).toBe('high');
          });
      });
    });

    describe('when care gap BCS exists', () => {
      it('should return the BCS care gap details', async () => {
        // BCS care gap response structure
        const expectedCareGap = {
          id: Matchers.uuid(CARE_GAP_CONSTANTS.CARE_GAP_BCS_ID),
          tenantId: Matchers.nonEmptyString(TEST_CONSTANTS.TENANT_ID),
          patientId: Matchers.uuid(CARE_GAP_CONSTANTS.PATIENT_WITH_GAPS_ID),
          measureId: Matchers.nonEmptyString(CARE_GAP_CONSTANTS.MEASURE_BCS),
          measureName: Matchers.nonEmptyString('Breast Cancer Screening'),
          gapCategory: Matchers.nonEmptyString('HEDIS'),
          measureYear: Matchers.integer(2024),
          gapType: Matchers.nonEmptyString('preventive-care'),
          gapStatus: MatchersV3.regex(/^(OPEN|CLOSED)$/, 'OPEN'),
          gapDescription: Matchers.nonEmptyString('Patient is overdue for mammogram screening'),
          gapReason: Matchers.nonEmptyString('No mammogram found in the last 2 years'),
          priority: MatchersV3.regex(/^(high|medium|low)$/, 'medium'),
          severity: MatchersV3.regex(/^(high|medium|low)$/, 'medium'),
          identifiedDate: MatchersV3.timestamp("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", '2024-01-15T10:30:00.000Z'),
          dueDate: Matchers.fhirDate('2024-03-15'),
          recommendation: Matchers.nonEmptyString('Schedule mammogram screening'),
          recommendationType: Matchers.nonEmptyString('screening'),
          recommendedAction: Matchers.nonEmptyString('Order screening mammogram and coordinate with imaging center'),
        };

        await provider
          .addInteraction()
          .given('care gap BCS exists')
          .uponReceiving('a request to get BCS care gap by ID')
          .withRequest('GET', `/api/v1/care-gaps/${CARE_GAP_CONSTANTS.CARE_GAP_BCS_ID}`, (builder) => {
            builder.headers(COMMON_HEADERS.withTenant());
          })
          .willRespondWith(200, (builder) => {
            builder
              .headers({ 'Content-Type': 'application/json' })
              .jsonBody(expectedCareGap);
          })
          .executeTest(async (mockServer) => {
            const client = createFetchTestClient(mockServer.url);
            const response = await client.get<Record<string, unknown>>(
              `/api/v1/care-gaps/${CARE_GAP_CONSTANTS.CARE_GAP_BCS_ID}`
            );

            // Verify care gap details
            expect(response).toBeDefined();
            expect(response['id']).toBe(CARE_GAP_CONSTANTS.CARE_GAP_BCS_ID);
            expect(response['measureId']).toBe(CARE_GAP_CONSTANTS.MEASURE_BCS);
            expect(response['gapStatus']).toBe('OPEN');
            expect(response['priority']).toBe('medium');
          });
      });
    });
  });

  describe('POST /api/v1/care-gaps/:id/close', () => {
    describe('when care gap can be closed', () => {
      it('should close the care gap and return updated status', async () => {
        // Close care gap request
        const closeRequest = {
          closureReason: 'Screening completed',
          closureDate: '2024-01-20',
          closedBy: 'test-user',
          interventionType: 'completed-screening',
          interventionNotes: 'Patient completed HbA1c test',
        };

        // Expected closed care gap response
        const closedCareGap = {
          id: Matchers.uuid(CARE_GAP_CONSTANTS.CARE_GAP_COL_ID),
          tenantId: Matchers.nonEmptyString(TEST_CONSTANTS.TENANT_ID),
          patientId: Matchers.uuid(CARE_GAP_CONSTANTS.PATIENT_WITH_GAPS_ID),
          measureId: Matchers.nonEmptyString(CARE_GAP_CONSTANTS.MEASURE_COL),
          measureName: Matchers.nonEmptyString('Colorectal Cancer Screening'),
          gapStatus: MatchersV3.string('CLOSED'),
          closureReason: Matchers.nonEmptyString('Screening completed'),
          closureDate: Matchers.fhirDate('2024-01-20'),
          closedBy: Matchers.nonEmptyString('test-user'),
        };

        await provider
          .addInteraction()
          .given('care gap can be closed')
          .uponReceiving('a request to close a care gap')
          .withRequest('POST', `/api/v1/care-gaps/${CARE_GAP_CONSTANTS.CARE_GAP_COL_ID}/close`, (builder) => {
            builder
              .headers(COMMON_HEADERS.withTenant())
              .jsonBody(closeRequest);
          })
          .willRespondWith(200, (builder) => {
            builder
              .headers({ 'Content-Type': 'application/json' })
              .jsonBody(closedCareGap);
          })
          .executeTest(async (mockServer) => {
            const client = createFetchTestClient(mockServer.url);
            const response = await client.post<Record<string, unknown>>(
              `/api/v1/care-gaps/${CARE_GAP_CONSTANTS.CARE_GAP_COL_ID}/close`,
              closeRequest
            );

            // Verify closed status
            expect(response).toBeDefined();
            expect(response['id']).toBe(CARE_GAP_CONSTANTS.CARE_GAP_COL_ID);
            expect(response['gapStatus']).toBe('CLOSED');
            expect(response['closureReason']).toBe('Screening completed');
          });
      });
    });
  });
});
