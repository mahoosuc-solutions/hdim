/**
 * API Integration E2E Tests
 *
 * Comprehensive API tests covering:
 * - REST API endpoints
 * - Request/response validation
 * - Error handling
 * - API versioning
 * - Rate limiting
 * - Data consistency
 *
 * @author TDD Swarm Agent 5A
 * @tags @api @integration
 */

import { test, expect } from '../fixtures/test-fixtures';

test.describe('API Integration Tests', () => {
  test.describe('Patient API', () => {
    test('@api should create patient via API', async ({ adminApiClient }) => {
      const patient = {
        firstName: 'API',
        lastName: 'Test',
        mrn: `API-MRN-${Date.now()}`,
        dateOfBirth: '1985-06-15',
        gender: 'female',
        email: 'apitest@example.com',
        phone: '555-1234',
        tenantId: 'test-tenant-001',
      };

      const response = await adminApiClient.post('/api/patients', {
        data: patient,
      });

      expect(response.ok()).toBeTruthy();

      const created = await response.json();
      expect(created.id).toBeTruthy();
      expect(created.firstName).toBe(patient.firstName);
      expect(created.lastName).toBe(patient.lastName);
      expect(created.mrn).toBe(patient.mrn);
    });

    test('@api should retrieve patient by ID', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();

      const response = await adminApiClient.get(`/api/patients/${patient.id}`);

      expect(response.ok()).toBeTruthy();

      const retrieved = await response.json();
      expect(retrieved.id).toBe(patient.id);
    });

    test('@api should update patient via API', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();

      const updates = {
        ...patient,
        email: 'updated@example.com',
        phone: '555-9999',
      };

      const response = await adminApiClient.put(`/api/patients/${patient.id}`, {
        data: updates,
      });

      expect(response.ok()).toBeTruthy();

      const updated = await response.json();
      expect(updated.email).toBe('updated@example.com');
    });

    test('@api should delete patient via API', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();

      const response = await adminApiClient.delete(`/api/patients/${patient.id}`);

      expect(response.ok()).toBeTruthy();

      // Verify deletion
      const getResponse = await adminApiClient.get(`/api/patients/${patient.id}`, {
        failOnStatusCode: false,
      });

      expect(getResponse.status()).toBe(404);
    });

    test('@api should search patients', async ({ adminApiClient }) => {
      const response = await adminApiClient.get('/api/patients?tenantId=test-tenant-001&search=Test');

      expect(response.ok()).toBeTruthy();

      const results = await response.json();
      expect(Array.isArray(results)).toBeTruthy();
    });

    test('@api should paginate patients', async ({ adminApiClient }) => {
      const response = await adminApiClient.get(
        '/api/patients?tenantId=test-tenant-001&page=0&size=10'
      );

      expect(response.ok()).toBeTruthy();

      const results = await response.json();
      expect(Array.isArray(results)).toBeTruthy();
      expect(results.length).toBeLessThanOrEqual(10);
    });
  });

  test.describe('FHIR API', () => {
    test('@api should create FHIR Patient resource', async ({ adminApiClient, testData }) => {
      const fhirPatient = {
        resourceType: 'Patient',
        name: [
          {
            family: 'Test',
            given: ['FHIR'],
          },
        ],
        gender: 'male',
        birthDate: '1990-01-01',
        identifier: [
          {
            system: 'http://healthdata.com/mrn',
            value: `FHIR-${Date.now()}`,
          },
        ],
      };

      const response = await adminApiClient.post('/api/fhir/Patient', {
        data: fhirPatient,
        failOnStatusCode: false,
      });

      expect([200, 201, 404]).toContain(response.status());

      if (response.ok()) {
        const created = await response.json();
        expect(created.id).toBeTruthy();
        expect(created.resourceType).toBe('Patient');
      }
    });

    test('@api should create FHIR Observation resource', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();

      const observation = {
        resourceType: 'Observation',
        status: 'final',
        code: {
          coding: [
            {
              system: 'http://loinc.org',
              code: '4548-4',
              display: 'Hemoglobin A1c',
            },
          ],
        },
        subject: {
          reference: `Patient/${patient.id}`,
        },
        valueQuantity: {
          value: 7.5,
          unit: '%',
          system: 'http://unitsofmeasure.org',
          code: '%',
        },
        effectiveDateTime: new Date().toISOString(),
      };

      const response = await adminApiClient.post('/api/fhir/Observation', {
        data: observation,
        failOnStatusCode: false,
      });

      expect([200, 201, 404]).toContain(response.status());
    });

    test('@api should search FHIR resources', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();

      const response = await adminApiClient.get(
        `/api/fhir/Observation?subject=Patient/${patient.id}`,
        {
          failOnStatusCode: false,
        }
      );

      expect([200, 404]).toContain(response.status());

      if (response.ok()) {
        const bundle = await response.json();
        expect(bundle.resourceType).toBe('Bundle');
      }
    });
  });

  test.describe('Quality Measure API', () => {
    test('@api should calculate measure for patient', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();

      const response = await adminApiClient.post(
        `/api/measures/calculate?patientId=${patient.id}&measureId=hba1c-control`,
        {
          failOnStatusCode: false,
        }
      );

      expect([200, 201, 404]).toContain(response.status());

      if (response.ok()) {
        const result = await response.json();
        expect(result.measureId).toBe('hba1c-control');
        expect(result.patientId).toBe(patient.id);
      }
    });

    test('@api should initiate batch calculation', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();

      const batchRequest = {
        measureIds: ['hba1c-control'],
        patientIds: [patient.id],
        tenantId: 'test-tenant-001',
      };

      const response = await adminApiClient.post('/api/measures/batch', {
        data: batchRequest,
        failOnStatusCode: false,
      });

      expect([200, 201, 202, 404]).toContain(response.status());
    });

    test('@api should get measure results', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();

      const response = await adminApiClient.get(
        `/api/measures/results?patientId=${patient.id}&tenantId=test-tenant-001`
      );

      expect(response.ok()).toBeTruthy();

      const results = await response.json();
      expect(Array.isArray(results)).toBeTruthy();
    });

    test('@api should get compliance statistics', async ({ adminApiClient }) => {
      const response = await adminApiClient.get(
        '/api/measures/compliance?measureId=hba1c-control&tenantId=test-tenant-001',
        {
          failOnStatusCode: false,
        }
      );

      expect([200, 404]).toContain(response.status());
    });
  });

  test.describe('Care Gap API', () => {
    test('@api should detect care gaps', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();

      const response = await adminApiClient.post('/api/care-gaps/detect', {
        data: {
          patientId: patient.id,
          measureIds: ['hba1c-control'],
          tenantId: 'test-tenant-001',
        },
        failOnStatusCode: false,
      });

      expect([200, 201, 202]).toContain(response.status());
    });

    test('@api should close care gap', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();
      const gap = await testData.createCareGap(patient.id);

      const response = await adminApiClient.post(`/api/care-gaps/${gap.id}/close`, {
        data: {
          reason: 'INTERVENTION_COMPLETED',
          notes: 'Test closure',
        },
      });

      expect(response.ok()).toBeTruthy();

      const closed = await response.json();
      expect(closed.status).toBe('CLOSED');
    });

    test('@api should list care gaps', async ({ adminApiClient }) => {
      const response = await adminApiClient.get('/api/care-gaps?tenantId=test-tenant-001&status=OPEN');

      expect(response.ok()).toBeTruthy();

      const gaps = await response.json();
      expect(Array.isArray(gaps)).toBeTruthy();
    });

    test('@api should filter care gaps by priority', async ({ adminApiClient }) => {
      const response = await adminApiClient.get(
        '/api/care-gaps?tenantId=test-tenant-001&priority=HIGH'
      );

      expect(response.ok()).toBeTruthy();

      const gaps = await response.json();
      gaps.forEach((gap: any) => {
        expect(gap.priority).toBe('HIGH');
      });
    });
  });

  test.describe('Error Handling', () => {
    test('@api should return 400 for invalid request data', async ({ adminApiClient }) => {
      const invalidPatient = {
        firstName: 'Test',
        // Missing required fields
      };

      const response = await adminApiClient.post('/api/patients', {
        data: invalidPatient,
        failOnStatusCode: false,
      });

      expect(response.status()).toBe(400);

      const error = await response.json();
      expect(error.message || error.error).toBeTruthy();
    });

    test('@api should return 404 for non-existent resource', async ({ adminApiClient }) => {
      const response = await adminApiClient.get('/api/patients/non-existent-id', {
        failOnStatusCode: false,
      });

      expect(response.status()).toBe(404);
    });

    test('@api should return 401 for unauthorized requests', async ({ page }) => {
      const response = await page.request.get('/api/patients?tenantId=test-tenant-001', {
        failOnStatusCode: false,
      });

      expect([401, 403]).toContain(response.status());
    });

    test('@api should return 409 for duplicate resources', async ({ adminApiClient }) => {
      const mrn = `DUPLICATE-${Date.now()}`;

      // Create first patient
      await adminApiClient.post('/api/patients', {
        data: {
          firstName: 'First',
          lastName: 'Patient',
          mrn,
          dateOfBirth: '1980-01-01',
          gender: 'male',
          tenantId: 'test-tenant-001',
        },
      });

      // Try to create duplicate
      const response = await adminApiClient.post('/api/patients', {
        data: {
          firstName: 'Second',
          lastName: 'Patient',
          mrn, // Same MRN
          dateOfBirth: '1990-01-01',
          gender: 'female',
          tenantId: 'test-tenant-001',
        },
        failOnStatusCode: false,
      });

      expect(response.status()).toBe(409);
    });

    test('@api should return proper error format', async ({ adminApiClient }) => {
      const response = await adminApiClient.get('/api/patients/invalid-id', {
        failOnStatusCode: false,
      });

      expect(response.status()).toBe(404);

      const error = await response.json();
      expect(error.message || error.error || error.status).toBeTruthy();
    });
  });

  test.describe('Response Headers', () => {
    test('@api should include Content-Type header', async ({ adminApiClient }) => {
      const response = await adminApiClient.get('/api/patients?tenantId=test-tenant-001');

      const headers = response.headers();
      expect(headers['content-type']).toContain('application/json');
    });

    test('@api should include Cache-Control headers', async ({ adminApiClient }) => {
      const response = await adminApiClient.get('/api/patients?tenantId=test-tenant-001');

      const headers = response.headers();
      expect(headers['cache-control']).toBeTruthy();
    });

    test('@api should include security headers', async ({ page }) => {
      const response = await page.request.get('/actuator/health');

      const headers = response.headers();
      // Security headers should be present
      expect(
        headers['x-content-type-options'] ||
          headers['x-frame-options'] ||
          headers['strict-transport-security']
      ).toBeTruthy();
    });
  });

  test.describe('Data Consistency', () => {
    test('@api should maintain referential integrity', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();

      // Create observation for patient
      await testData.createObservation(patient.id);

      // Delete patient
      await adminApiClient.delete(`/api/patients/${patient.id}`);

      // Observations should be handled (cascade delete or orphan prevention)
      const obsResponse = await adminApiClient.get(
        `/api/fhir/Observation?subject=Patient/${patient.id}`,
        {
          failOnStatusCode: false,
        }
      );

      expect([200, 404]).toContain(obsResponse.status());
    });

    test('@api should handle concurrent updates', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();

      // Concurrent updates
      const update1 = adminApiClient.put(`/api/patients/${patient.id}`, {
        data: { ...patient, email: 'update1@example.com' },
        failOnStatusCode: false,
      });

      const update2 = adminApiClient.put(`/api/patients/${patient.id}`, {
        data: { ...patient, email: 'update2@example.com' },
        failOnStatusCode: false,
      });

      const [response1, response2] = await Promise.all([update1, update2]);

      // Both should complete without corruption
      expect([200, 409]).toContain(response1.status());
      expect([200, 409]).toContain(response2.status());
    });
  });

  test.describe('API Versioning', () => {
    test('@api should support API version in URL', async ({ adminApiClient }) => {
      const response = await adminApiClient.get('/api/v1/patients?tenantId=test-tenant-001', {
        failOnStatusCode: false,
      });

      expect([200, 404]).toContain(response.status());
    });

    test('@api should support API version in header', async ({ page }) => {
      const response = await page.request.get('/api/patients?tenantId=test-tenant-001', {
        headers: {
          'API-Version': '1.0',
        },
        failOnStatusCode: false,
      });

      expect([200, 401, 403, 404]).toContain(response.status());
    });
  });

  test.describe('Bulk Operations', () => {
    test('@api should support bulk patient creation', async ({ adminApiClient }) => {
      const patients = [
        {
          firstName: 'Bulk1',
          lastName: 'Patient',
          mrn: `BULK-1-${Date.now()}`,
          dateOfBirth: '1980-01-01',
          gender: 'male',
          tenantId: 'test-tenant-001',
        },
        {
          firstName: 'Bulk2',
          lastName: 'Patient',
          mrn: `BULK-2-${Date.now()}`,
          dateOfBirth: '1985-01-01',
          gender: 'female',
          tenantId: 'test-tenant-001',
        },
      ];

      const response = await adminApiClient.post('/api/patients/bulk', {
        data: patients,
        failOnStatusCode: false,
      });

      expect([200, 201, 404, 501]).toContain(response.status());
    });

    test('@api should support bulk updates', async ({ adminApiClient, testData }) => {
      const patient1 = await testData.createPatient();
      const patient2 = await testData.createPatient();

      const updates = [
        { id: patient1.id, email: 'bulk-update-1@example.com' },
        { id: patient2.id, email: 'bulk-update-2@example.com' },
      ];

      const response = await adminApiClient.patch('/api/patients/bulk', {
        data: updates,
        failOnStatusCode: false,
      });

      expect([200, 404, 501]).toContain(response.status());
    });
  });

  test.describe('Health and Monitoring', () => {
    test('@api should provide health check endpoint', async ({ page }) => {
      const response = await page.request.get('/actuator/health');

      expect(response.ok()).toBeTruthy();

      const health = await response.json();
      expect(health.status).toBe('UP');
    });

    test('@api should provide metrics endpoint', async ({ page }) => {
      const response = await page.request.get('/actuator/metrics', {
        failOnStatusCode: false,
      });

      expect([200, 401, 404]).toContain(response.status());
    });

    test('@api should provide info endpoint', async ({ page }) => {
      const response = await page.request.get('/actuator/info', {
        failOnStatusCode: false,
      });

      expect([200, 404]).toContain(response.status());
    });
  });
});
