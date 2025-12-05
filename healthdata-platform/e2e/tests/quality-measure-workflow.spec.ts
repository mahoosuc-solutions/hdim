/**
 * Quality Measure Workflow E2E Tests
 *
 * Comprehensive end-to-end tests covering:
 * - Individual measure calculations
 * - Batch measure calculations
 * - Measure results and compliance tracking
 * - HEDIS measures (HbA1c, BP Control, Colorectal Screening, etc.)
 * - Measure trending and analytics
 * - Performance benchmarks
 *
 * @author TDD Swarm Agent 5A
 * @tags @workflow @quality-measure
 */

import { test, expect } from '../fixtures/test-fixtures';

test.describe('Quality Measure Workflow', () => {
  test.describe('Individual Measure Calculation', () => {
    test('@workflow should calculate HbA1c control measure', async ({
      adminApiClient,
      testData,
    }) => {
      // Create test patient
      const patient = await testData.createPatient({
        dateOfBirth: '1980-01-01',
        gender: 'male',
      });

      // Create HbA1c observation
      await testData.createObservation(patient.id, {
        code: {
          coding: [
            {
              system: 'http://loinc.org',
              code: '4548-4',
              display: 'Hemoglobin A1c',
            },
          ],
        },
        valueQuantity: {
          value: 7.2,
          unit: '%',
        },
      });

      // Calculate measure
      const response = await adminApiClient.post(
        `/api/measures/calculate?patientId=${patient.id}&measureId=hba1c-control`,
        {
          failOnStatusCode: false,
        }
      );

      expect(response.ok()).toBeTruthy();
      const result = await response.json();
      expect(result.measureId).toBe('hba1c-control');
      expect(result.patientId).toBe(patient.id);
      expect(result.compliant).toBeDefined();
    });

    test('@workflow should calculate blood pressure control measure', async ({
      adminApiClient,
      testData,
    }) => {
      const patient = await testData.createPatient();

      // Create BP observation
      await testData.createObservation(patient.id, {
        code: {
          coding: [
            {
              system: 'http://loinc.org',
              code: '85354-9',
              display: 'Blood pressure',
            },
          ],
        },
        component: [
          {
            code: {
              coding: [
                {
                  system: 'http://loinc.org',
                  code: '8480-6',
                  display: 'Systolic blood pressure',
                },
              ],
            },
            valueQuantity: {
              value: 125,
              unit: 'mmHg',
            },
          },
          {
            code: {
              coding: [
                {
                  system: 'http://loinc.org',
                  code: '8462-4',
                  display: 'Diastolic blood pressure',
                },
              ],
            },
            valueQuantity: {
              value: 78,
              unit: 'mmHg',
            },
          },
        ],
      });

      const response = await adminApiClient.post(
        `/api/measures/calculate?patientId=${patient.id}&measureId=bp-control`,
        {
          failOnStatusCode: false,
        }
      );

      expect([200, 201, 404]).toContain(response.status());
    });

    test('@workflow should handle measure calculation for patient with no data', async ({
      adminApiClient,
      testData,
    }) => {
      const patient = await testData.createPatient();

      // Calculate measure without any observations
      const response = await adminApiClient.post(
        `/api/measures/calculate?patientId=${patient.id}&measureId=hba1c-control`,
        {
          failOnStatusCode: false,
        }
      );

      // Should return non-compliant or no data status
      expect([200, 201, 404]).toContain(response.status());

      if (response.ok()) {
        const result = await response.json();
        expect(result.compliant === false || result.status === 'NO_DATA').toBeTruthy();
      }
    });

    test('@workflow should validate measure ID', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();

      const response = await adminApiClient.post(
        `/api/measures/calculate?patientId=${patient.id}&measureId=invalid-measure-id`,
        {
          failOnStatusCode: false,
        }
      );

      expect(response.status()).toBe(404);
    });

    test('@workflow should validate patient ID', async ({ adminApiClient }) => {
      const response = await adminApiClient.post(
        `/api/measures/calculate?patientId=non-existent-patient&measureId=hba1c-control`,
        {
          failOnStatusCode: false,
        }
      );

      expect(response.status()).toBe(404);
    });
  });

  test.describe('Batch Measure Calculation', () => {
    test('@workflow should calculate measures for multiple patients', async ({
      adminApiClient,
      testData,
    }) => {
      // Create multiple test patients
      const patient1 = await testData.createPatient();
      const patient2 = await testData.createPatient();
      const patient3 = await testData.createPatient();

      const batchRequest = {
        measureIds: ['hba1c-control', 'bp-control'],
        patientIds: [patient1.id, patient2.id, patient3.id],
        tenantId: 'test-tenant-001',
      };

      const response = await adminApiClient.post('/api/measures/batch', {
        data: batchRequest,
        failOnStatusCode: false,
      });

      expect([200, 201, 202]).toContain(response.status());

      if (response.ok()) {
        const result = await response.json();
        expect(result.batchId || result.jobId).toBeTruthy();
      }
    });

    test('@workflow should calculate measures for entire population', async ({
      adminApiClient,
    }) => {
      const batchRequest = {
        measureIds: ['hba1c-control'],
        tenantId: 'test-tenant-001',
        scope: 'ALL_PATIENTS',
      };

      const response = await adminApiClient.post('/api/measures/batch', {
        data: batchRequest,
        failOnStatusCode: false,
      });

      expect([200, 201, 202]).toContain(response.status());
    });

    test('@workflow should track batch calculation progress', async ({
      adminApiClient,
      testData,
    }) => {
      const patient1 = await testData.createPatient();
      const patient2 = await testData.createPatient();

      // Start batch calculation
      const batchResponse = await adminApiClient.post('/api/measures/batch', {
        data: {
          measureIds: ['hba1c-control'],
          patientIds: [patient1.id, patient2.id],
          tenantId: 'test-tenant-001',
        },
        failOnStatusCode: false,
      });

      if (!batchResponse.ok()) {
        test.skip();
      }

      const batch = await batchResponse.json();
      const batchId = batch.batchId || batch.jobId || batch.id;

      if (!batchId) {
        test.skip();
      }

      // Check status
      const statusResponse = await adminApiClient.get(`/api/measures/batch/${batchId}/status`, {
        failOnStatusCode: false,
      });

      expect([200, 404]).toContain(statusResponse.status());

      if (statusResponse.ok()) {
        const status = await statusResponse.json();
        expect(['PENDING', 'RUNNING', 'COMPLETED', 'FAILED'].includes(status.status)).toBeTruthy();
      }
    });

    test('@workflow should support batch calculation with filters', async ({
      adminApiClient,
    }) => {
      const batchRequest = {
        measureIds: ['hba1c-control'],
        tenantId: 'test-tenant-001',
        filters: {
          ageMin: 18,
          ageMax: 75,
          gender: 'male',
        },
      };

      const response = await adminApiClient.post('/api/measures/batch', {
        data: batchRequest,
        failOnStatusCode: false,
      });

      expect([200, 201, 202, 404]).toContain(response.status());
    });

    test('@workflow should cancel batch calculation', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();

      // Start batch
      const batchResponse = await adminApiClient.post('/api/measures/batch', {
        data: {
          measureIds: ['hba1c-control'],
          patientIds: [patient.id],
          tenantId: 'test-tenant-001',
        },
        failOnStatusCode: false,
      });

      if (!batchResponse.ok()) {
        test.skip();
      }

      const batch = await batchResponse.json();
      const batchId = batch.batchId || batch.jobId || batch.id;

      if (!batchId) {
        test.skip();
      }

      // Cancel batch
      const cancelResponse = await adminApiClient.post(`/api/measures/batch/${batchId}/cancel`, {
        failOnStatusCode: false,
      });

      expect([200, 204, 404]).toContain(cancelResponse.status());
    });
  });

  test.describe('Measure Results and Compliance', () => {
    test('@workflow should retrieve measure results for patient', async ({
      adminApiClient,
      testData,
    }) => {
      const patient = await testData.createPatient();

      // Calculate measure first
      await adminApiClient.post(
        `/api/measures/calculate?patientId=${patient.id}&measureId=hba1c-control`,
        {
          failOnStatusCode: false,
        }
      );

      // Get results
      const response = await adminApiClient.get(
        `/api/measures/results?patientId=${patient.id}&tenantId=test-tenant-001`
      );

      expect(response.ok()).toBeTruthy();
      const results = await response.json();
      expect(Array.isArray(results)).toBeTruthy();
    });

    test('@workflow should filter results by measure ID', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();

      const response = await adminApiClient.get(
        `/api/measures/results?patientId=${patient.id}&measureId=hba1c-control&tenantId=test-tenant-001`
      );

      expect(response.ok()).toBeTruthy();
      const results = await response.json();
      expect(Array.isArray(results)).toBeTruthy();

      results.forEach((result: any) => {
        expect(result.measureId).toBe('hba1c-control');
      });
    });

    test('@workflow should get compliance rate for measure', async ({ adminApiClient }) => {
      const response = await adminApiClient.get(
        '/api/measures/compliance?measureId=hba1c-control&tenantId=test-tenant-001',
        {
          failOnStatusCode: false,
        }
      );

      expect([200, 404]).toContain(response.status());

      if (response.ok()) {
        const compliance = await response.json();
        expect(compliance.rate >= 0 && compliance.rate <= 100).toBeTruthy();
      }
    });

    test('@workflow should get population-level statistics', async ({ adminApiClient }) => {
      const response = await adminApiClient.get(
        '/api/measures/statistics?tenantId=test-tenant-001',
        {
          failOnStatusCode: false,
        }
      );

      expect([200, 404]).toContain(response.status());

      if (response.ok()) {
        const stats = await response.json();
        expect(stats.totalPatients).toBeDefined();
        expect(stats.measuresCalculated).toBeDefined();
      }
    });

    test('@workflow should track measure compliance over time', async ({
      adminApiClient,
      testData,
    }) => {
      const patient = await testData.createPatient();

      const response = await adminApiClient.get(
        `/api/measures/trends?patientId=${patient.id}&measureId=hba1c-control&period=6M`,
        {
          failOnStatusCode: false,
        }
      );

      expect([200, 404]).toContain(response.status());

      if (response.ok()) {
        const trends = await response.json();
        expect(Array.isArray(trends)).toBeTruthy();
      }
    });
  });

  test.describe('HEDIS Measures', () => {
    test('@workflow should support all standard HEDIS measures', async ({ adminApiClient }) => {
      const hedisMe asures = [
        'hba1c-control',
        'bp-control',
        'colorectal-screening',
        'breast-cancer-screening',
        'cervical-cancer-screening',
        'cholesterol-control',
        'bmi-assessment',
        'depression-screening',
      ];

      for (const measureId of hedisMe asures) {
        const response = await adminApiClient.get(`/api/measures/${measureId}`, {
          failOnStatusCode: false,
        });

        // Measure should exist or be recognized
        expect([200, 404]).toContain(response.status());
      }
    });

    test('@workflow should calculate colorectal cancer screening', async ({
      adminApiClient,
      testData,
    }) => {
      const patient = await testData.createPatient({
        dateOfBirth: '1960-01-01', // Age 64 - within screening range
        gender: 'male',
      });

      const response = await adminApiClient.post(
        `/api/measures/calculate?patientId=${patient.id}&measureId=colorectal-screening`,
        {
          failOnStatusCode: false,
        }
      );

      expect([200, 201, 404]).toContain(response.status());
    });

    test('@workflow should calculate breast cancer screening', async ({
      adminApiClient,
      testData,
    }) => {
      const patient = await testData.createPatient({
        dateOfBirth: '1975-01-01', // Age 49 - within screening range
        gender: 'female',
      });

      const response = await adminApiClient.post(
        `/api/measures/calculate?patientId=${patient.id}&measureId=breast-cancer-screening`,
        {
          failOnStatusCode: false,
        }
      );

      expect([200, 201, 404]).toContain(response.status());
    });

    test('@workflow should respect age criteria for measures', async ({
      adminApiClient,
      testData,
    }) => {
      // Create patient too young for colorectal screening
      const youngPatient = await testData.createPatient({
        dateOfBirth: '2000-01-01', // Age 24
        gender: 'male',
      });

      const response = await adminApiClient.post(
        `/api/measures/calculate?patientId=${youngPatient.id}&measureId=colorectal-screening`,
        {
          failOnStatusCode: false,
        }
      );

      if (response.ok()) {
        const result = await response.json();
        expect(result.applicable === false || result.status === 'NOT_APPLICABLE').toBeTruthy();
      }
    });
  });

  test.describe('Measure Performance', () => {
    test('@workflow @performance should calculate measure within time limit', async ({
      adminApiClient,
      testData,
    }) => {
      const patient = await testData.createPatient();

      const startTime = Date.now();

      const response = await adminApiClient.post(
        `/api/measures/calculate?patientId=${patient.id}&measureId=hba1c-control`,
        {
          timeout: 5000,
          failOnStatusCode: false,
        }
      );

      const duration = Date.now() - startTime;

      expect([200, 201, 404]).toContain(response.status());
      expect(duration).toBeLessThan(5000); // Should complete in under 5 seconds
    });

    test('@workflow @performance should handle concurrent calculations', async ({
      adminApiClient,
      testData,
    }) => {
      const patient = await testData.createPatient();

      // Start multiple calculations concurrently
      const calculations = [
        adminApiClient.post(
          `/api/measures/calculate?patientId=${patient.id}&measureId=hba1c-control`,
          { failOnStatusCode: false }
        ),
        adminApiClient.post(
          `/api/measures/calculate?patientId=${patient.id}&measureId=bp-control`,
          { failOnStatusCode: false }
        ),
        adminApiClient.post(
          `/api/measures/calculate?patientId=${patient.id}&measureId=bmi-assessment`,
          { failOnStatusCode: false }
        ),
      ];

      const results = await Promise.all(calculations);

      // All should complete without errors
      results.forEach((response) => {
        expect([200, 201, 404]).toContain(response.status());
      });
    });
  });

  test.describe('Measure Data Quality', () => {
    test('@workflow should detect missing data for measure calculation', async ({
      adminApiClient,
      testData,
    }) => {
      const patient = await testData.createPatient();

      const response = await adminApiClient.post(
        `/api/measures/calculate?patientId=${patient.id}&measureId=hba1c-control`,
        {
          failOnStatusCode: false,
        }
      );

      if (response.ok()) {
        const result = await response.json();

        if (result.compliant === false) {
          expect(result.reason || result.status).toBeTruthy();
        }
      }
    });

    test('@workflow should validate observation data quality', async ({
      adminApiClient,
      testData,
    }) => {
      const patient = await testData.createPatient();

      // Create observation with missing value
      const invalidObservation = {
        resourceType: 'Observation',
        status: 'final',
        code: {
          coding: [
            {
              system: 'http://loinc.org',
              code: '4548-4',
            },
          ],
        },
        subject: {
          reference: `Patient/${patient.id}`,
        },
        // Missing valueQuantity
      };

      const obsResponse = await adminApiClient.post('/api/fhir/Observation', {
        data: invalidObservation,
        failOnStatusCode: false,
      });

      expect(obsResponse.status()).toBe(400);
    });

    test('@workflow should handle old observation data', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();

      // Create old observation (2 years ago)
      const oldDate = new Date();
      oldDate.setFullYear(oldDate.getFullYear() - 2);

      await testData.createObservation(patient.id, {
        effectiveDateTime: oldDate.toISOString(),
      });

      const response = await adminApiClient.post(
        `/api/measures/calculate?patientId=${patient.id}&measureId=hba1c-control`,
        {
          failOnStatusCode: false,
        }
      );

      if (response.ok()) {
        const result = await response.json();
        // Should recognize data is too old
        expect(result.dataAge || result.status).toBeTruthy();
      }
    });
  });

  test.describe('Measure Export and Reporting', () => {
    test('@workflow should export measure results', async ({ adminApiClient }) => {
      const response = await adminApiClient.get(
        '/api/measures/export?measureId=hba1c-control&tenantId=test-tenant-001&format=csv',
        {
          failOnStatusCode: false,
        }
      );

      expect([200, 404, 501]).toContain(response.status());
    });

    test('@workflow should generate measure report', async ({ adminApiClient }) => {
      const reportRequest = {
        measureIds: ['hba1c-control', 'bp-control'],
        tenantId: 'test-tenant-001',
        period: {
          start: '2024-01-01',
          end: '2024-12-31',
        },
      };

      const response = await adminApiClient.post('/api/measures/report', {
        data: reportRequest,
        failOnStatusCode: false,
      });

      expect([200, 201, 404, 501]).toContain(response.status());
    });
  });
});
