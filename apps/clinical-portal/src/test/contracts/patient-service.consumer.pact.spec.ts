/**
 * Patient Service Consumer Contract Tests
 *
 * Defines the contract expectations for the Clinical Portal's interactions
 * with the Patient Service. These contracts are verified by the Patient
 * Service provider tests.
 *
 * Provider States:
 * - "patient exists with id f47ac10b-58cc-4372-a567-0e02b2c3d479"
 * - "no patient exists with id 00000000-0000-0000-0000-000000000000"
 * - "patient exists with MRN MRN-12345"
 * - "multiple patients exist"
 *
 * @see PatientServiceProviderTest (backend)
 * @see PatientContractStateSetup (backend)
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

describe('Patient Service Consumer Contract', () => {
  // Create Pact provider for Patient Service
  const provider = createPactProvider(PROVIDER_NAMES.PATIENT_SERVICE);

  describe('GET /api/v1/patients/:id', () => {
    describe('when patient exists', () => {
      it('should return the patient data', async () => {
        // Define the expected response structure
        const expectedPatient = {
          id: Matchers.uuid(TEST_CONSTANTS.PATIENT_JOHN_DOE_ID),
          tenantId: Matchers.nonEmptyString(TEST_CONSTANTS.TENANT_ID),
          fhirPatientId: Matchers.uuid(TEST_CONSTANTS.PATIENT_JOHN_DOE_ID),
          mrn: Matchers.nonEmptyString(TEST_CONSTANTS.MRN_JOHN_DOE),
          firstName: Matchers.nonEmptyString('John'),
          lastName: Matchers.nonEmptyString('Doe'),
          dateOfBirth: Matchers.fhirDate('1980-05-15'),
          gender: Matchers.fhirGender('male'),
          active: Matchers.boolean(true),
          email: MatchersV3.string('john.doe@example.com'),
          phone: MatchersV3.string('555-123-4567'),
          addressLine1: MatchersV3.string('123 Main Street'),
          city: MatchersV3.string('Springfield'),
          state: MatchersV3.string('IL'),
          zipCode: MatchersV3.string('62701'),
        };

        await provider
          .addInteraction()
          .given(`patient exists with id ${TEST_CONSTANTS.PATIENT_JOHN_DOE_ID}`)
          .uponReceiving('a request to get patient by ID')
          .withRequest('GET', `/api/v1/patients/${TEST_CONSTANTS.PATIENT_JOHN_DOE_ID}`, (builder) => {
            builder.headers(COMMON_HEADERS.withTenant());
          })
          .willRespondWith(200, (builder) => {
            builder
              .headers({ 'Content-Type': 'application/json' })
              .jsonBody(expectedPatient);
          })
          .executeTest(async (mockServer) => {
            const client = createFetchTestClient(mockServer.url);
            const response = await client.get<Record<string, unknown>>(
              `/api/v1/patients/${TEST_CONSTANTS.PATIENT_JOHN_DOE_ID}`
            );

            // Verify essential fields
            expect(response).toBeDefined();
            expect(response['id']).toBe(TEST_CONSTANTS.PATIENT_JOHN_DOE_ID);
            expect(response['firstName']).toBe('John');
            expect(response['lastName']).toBe('Doe');
            expect(response['active']).toBe(true);
          });
      });
    });

    describe('when patient does not exist', () => {
      it('should return 404 Not Found', async () => {
        const nonExistentId = TEST_CONSTANTS.NON_EXISTENT_PATIENT_ID;

        // Define error response structure
        const errorResponse = {
          status: MatchersV3.integer(404),
          error: MatchersV3.string('Not Found'),
          message: MatchersV3.string(`Patient not found with id: ${nonExistentId}`),
          path: MatchersV3.string(`/api/v1/patients/${nonExistentId}`),
        };

        await provider
          .addInteraction()
          .given(`no patient exists with id ${nonExistentId}`)
          .uponReceiving('a request to get a non-existent patient')
          .withRequest('GET', `/api/v1/patients/${nonExistentId}`, (builder) => {
            builder.headers(COMMON_HEADERS.withTenant());
          })
          .willRespondWith(404, (builder) => {
            builder
              .headers({ 'Content-Type': 'application/json' })
              .jsonBody(errorResponse);
          })
          .executeTest(async (mockServer) => {
            const client = createFetchTestClient(mockServer.url);

            try {
              await client.get<unknown>(`/api/v1/patients/${nonExistentId}`);
              fail('Expected 404 error to be thrown');
            } catch (error) {
              expect(error).toBeDefined();
              expect((error as Error).message).toContain('404');
            }
          });
      });
    });
  });

  describe('GET /api/v1/patients/search', () => {
    describe('when searching by MRN', () => {
      it('should return patients matching the MRN', async () => {
        // Expected patient in search results
        const patientInResults = {
          id: Matchers.uuid(TEST_CONSTANTS.PATIENT_JOHN_DOE_ID),
          tenantId: Matchers.nonEmptyString(TEST_CONSTANTS.TENANT_ID),
          mrn: Matchers.nonEmptyString(TEST_CONSTANTS.MRN_JOHN_DOE),
          firstName: Matchers.nonEmptyString('John'),
          lastName: Matchers.nonEmptyString('Doe'),
          dateOfBirth: Matchers.fhirDate('1980-05-15'),
          gender: Matchers.fhirGender('male'),
          active: Matchers.boolean(true),
        };

        // Paginated response structure
        const searchResponse = {
          content: MatchersV3.eachLike(patientInResults, 1),
          totalElements: Matchers.integer(1),
          totalPages: Matchers.integer(1),
          number: Matchers.integer(0),
          size: Matchers.integer(20),
          first: Matchers.boolean(true),
          last: Matchers.boolean(true),
        };

        await provider
          .addInteraction()
          .given(`patient exists with MRN ${TEST_CONSTANTS.MRN_JOHN_DOE}`)
          .uponReceiving('a request to search patients by MRN')
          .withRequest('GET', '/api/v1/patients/search', (builder) => {
            builder
              .headers(COMMON_HEADERS.withTenant())
              .query({ mrn: TEST_CONSTANTS.MRN_JOHN_DOE });
          })
          .willRespondWith(200, (builder) => {
            builder
              .headers({ 'Content-Type': 'application/json' })
              .jsonBody(searchResponse);
          })
          .executeTest(async (mockServer) => {
            const client = createFetchTestClient(mockServer.url);
            const response = await client.get<Record<string, unknown>>(
              `/api/v1/patients/search?mrn=${TEST_CONSTANTS.MRN_JOHN_DOE}`
            );

            // Verify search results structure
            expect(response).toBeDefined();
            expect(response['content']).toBeDefined();
            expect(Array.isArray(response['content'])).toBe(true);
            expect((response['content'] as unknown[]).length).toBeGreaterThan(0);

            const firstPatient = (response['content'] as Record<string, unknown>[])[0];
            expect(firstPatient['mrn']).toBe(TEST_CONSTANTS.MRN_JOHN_DOE);
          });
      });
    });

    describe('when searching returns no results', () => {
      it('should return empty content array', async () => {
        const emptySearchResponse = {
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
          .given(`no patient exists with id ${TEST_CONSTANTS.NON_EXISTENT_PATIENT_ID}`)
          .uponReceiving('a request to search patients with no matches')
          .withRequest('GET', '/api/v1/patients/search', (builder) => {
            builder
              .headers(COMMON_HEADERS.withTenant())
              .query({ mrn: 'NON-EXISTENT-MRN' });
          })
          .willRespondWith(200, (builder) => {
            builder
              .headers({ 'Content-Type': 'application/json' })
              .jsonBody(emptySearchResponse);
          })
          .executeTest(async (mockServer) => {
            const client = createFetchTestClient(mockServer.url);
            const response = await client.get<Record<string, unknown>>(
              '/api/v1/patients/search?mrn=NON-EXISTENT-MRN'
            );

            // Verify empty results
            expect(response).toBeDefined();
            expect(response['content']).toEqual([]);
            expect(response['totalElements']).toBe(0);
          });
      });
    });
  });

  describe('GET /api/v1/patients', () => {
    describe('when listing active patients', () => {
      it('should return paginated list of patients', async () => {
        // Patient structure for list response
        const patientListItem = {
          id: Matchers.uuid(),
          tenantId: Matchers.nonEmptyString(TEST_CONSTANTS.TENANT_ID),
          mrn: Matchers.nonEmptyString(),
          firstName: Matchers.nonEmptyString(),
          lastName: Matchers.nonEmptyString(),
          dateOfBirth: Matchers.fhirDate(),
          gender: Matchers.fhirGender(),
          active: Matchers.boolean(true),
        };

        // Paginated response with at least 2 patients
        const listResponse = {
          content: MatchersV3.eachLike(patientListItem, 2),
          totalElements: Matchers.integer(2),
          totalPages: Matchers.integer(1),
          number: Matchers.integer(0),
          size: Matchers.integer(20),
        };

        await provider
          .addInteraction()
          .given('multiple patients exist')
          .uponReceiving('a request to list all patients')
          .withRequest('GET', '/api/v1/patients', (builder) => {
            builder
              .headers(COMMON_HEADERS.withTenant())
              .query({ page: '0', size: '20' });
          })
          .willRespondWith(200, (builder) => {
            builder
              .headers({ 'Content-Type': 'application/json' })
              .jsonBody(listResponse);
          })
          .executeTest(async (mockServer) => {
            const client = createFetchTestClient(mockServer.url);
            const response = await client.get<Record<string, unknown>>(
              '/api/v1/patients?page=0&size=20'
            );

            // Verify list response
            expect(response).toBeDefined();
            expect(response['content']).toBeDefined();
            expect(Array.isArray(response['content'])).toBe(true);
            expect((response['content'] as unknown[]).length).toBeGreaterThanOrEqual(2);
          });
      });
    });
  });

  describe('GET /patient/health-record/:patientId', () => {
    describe('when patient health record exists', () => {
      it('should return comprehensive health record data', async () => {
        // Health record response structure
        const healthRecordResponse = {
          patientId: Matchers.uuid(TEST_CONSTANTS.PATIENT_JOHN_DOE_ID),
          demographics: {
            firstName: Matchers.nonEmptyString('John'),
            lastName: Matchers.nonEmptyString('Doe'),
            dateOfBirth: Matchers.fhirDate('1980-05-15'),
            gender: Matchers.fhirGender('male'),
          },
          tenantId: Matchers.nonEmptyString(TEST_CONSTANTS.TENANT_ID),
        };

        await provider
          .addInteraction()
          .given(`patient exists with id ${TEST_CONSTANTS.PATIENT_JOHN_DOE_ID}`)
          .uponReceiving('a request for patient health record')
          .withRequest('GET', `/patient/health-record/${TEST_CONSTANTS.PATIENT_JOHN_DOE_ID}`, (builder) => {
            builder.headers(COMMON_HEADERS.withTenant());
          })
          .willRespondWith(200, (builder) => {
            builder
              .headers({ 'Content-Type': 'application/json' })
              .jsonBody(healthRecordResponse);
          })
          .executeTest(async (mockServer) => {
            const client = createFetchTestClient(mockServer.url);
            const response = await client.get<Record<string, unknown>>(
              `/patient/health-record/${TEST_CONSTANTS.PATIENT_JOHN_DOE_ID}`
            );

            // Verify health record structure
            expect(response).toBeDefined();
            expect(response['patientId']).toBe(TEST_CONSTANTS.PATIENT_JOHN_DOE_ID);
            expect(response['demographics']).toBeDefined();
          });
      });
    });
  });
});
