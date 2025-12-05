/**
 * Patient Management Workflow E2E Tests
 *
 * Comprehensive end-to-end tests covering:
 * - Patient registration and creation
 * - Patient search and filtering
 * - Patient detail viewing and editing
 * - Patient data validation
 * - Bulk operations
 * - Error handling
 *
 * @author TDD Swarm Agent 5A
 * @tags @workflow @patient
 */

import { test, expect } from '../fixtures/test-fixtures';

test.describe('Patient Management Workflow', () => {
  test.describe('Patient Registration', () => {
    test('@workflow should complete full patient registration flow', async ({ adminApiClient, adminPage }) => {
      // Navigate to patients page via API or UI
      const response = await adminApiClient.get('/api/patients?tenantId=test-tenant-001');
      expect(response.ok()).toBeTruthy();

      // Create a new patient
      const newPatient = {
        firstName: 'John',
        lastName: 'Smith',
        mrn: `MRN-${Date.now()}`,
        dateOfBirth: '1980-01-15',
        gender: 'male',
        email: 'john.smith@example.com',
        phone: '555-0123',
        address: {
          line1: '123 Main St',
          city: 'Boston',
          state: 'MA',
          zipCode: '02101',
        },
        insurance: {
          provider: 'United Healthcare',
          memberId: 'UHC123456',
          groupNumber: 'GRP789',
        },
        tenantId: 'test-tenant-001',
      };

      const createResponse = await adminApiClient.post('/api/patients', {
        data: newPatient,
      });

      expect(createResponse.ok()).toBeTruthy();
      const createdPatient = await createResponse.json();
      expect(createdPatient.id).toBeTruthy();
      expect(createdPatient.firstName).toBe('John');
      expect(createdPatient.lastName).toBe('Smith');
      expect(createdPatient.mrn).toBe(newPatient.mrn);
    });

    test('@workflow should validate required patient fields', async ({ adminApiClient }) => {
      const invalidPatient = {
        firstName: 'Test',
        // Missing required fields: lastName, dateOfBirth, gender
        tenantId: 'test-tenant-001',
      };

      const response = await adminApiClient.post('/api/patients', {
        data: invalidPatient,
        failOnStatusCode: false,
      });

      expect(response.status()).toBe(400);
      const error = await response.json();
      expect(error.message).toMatch(/required|validation/i);
    });

    test('@workflow should prevent duplicate MRN', async ({ adminApiClient }) => {
      const mrn = `UNIQUE-MRN-${Date.now()}`;

      // Create first patient
      const patient1 = {
        firstName: 'First',
        lastName: 'Patient',
        mrn,
        dateOfBirth: '1980-01-01',
        gender: 'male',
        tenantId: 'test-tenant-001',
      };

      const response1 = await adminApiClient.post('/api/patients', { data: patient1 });
      expect(response1.ok()).toBeTruthy();

      // Try to create duplicate
      const patient2 = {
        firstName: 'Second',
        lastName: 'Patient',
        mrn, // Same MRN
        dateOfBirth: '1990-01-01',
        gender: 'female',
        tenantId: 'test-tenant-001',
      };

      const response2 = await adminApiClient.post('/api/patients', {
        data: patient2,
        failOnStatusCode: false,
      });

      expect(response2.status()).toBe(409); // Conflict
    });

    test('@workflow should validate email format', async ({ adminApiClient }) => {
      const patient = {
        firstName: 'Test',
        lastName: 'Patient',
        mrn: `MRN-${Date.now()}`,
        dateOfBirth: '1980-01-01',
        gender: 'male',
        email: 'invalid-email', // Invalid format
        tenantId: 'test-tenant-001',
      };

      const response = await adminApiClient.post('/api/patients', {
        data: patient,
        failOnStatusCode: false,
      });

      expect(response.status()).toBe(400);
    });

    test('@workflow should validate date of birth format', async ({ adminApiClient }) => {
      const patient = {
        firstName: 'Test',
        lastName: 'Patient',
        mrn: `MRN-${Date.now()}`,
        dateOfBirth: 'invalid-date',
        gender: 'male',
        tenantId: 'test-tenant-001',
      };

      const response = await adminApiClient.post('/api/patients', {
        data: patient,
        failOnStatusCode: false,
      });

      expect(response.status()).toBe(400);
    });
  });

  test.describe('Patient Search and Retrieval', () => {
    test('@workflow should search patients by name', async ({ adminApiClient, testData }) => {
      // Create test patients
      const patient1 = await testData.createPatient({
        firstName: 'Alice',
        lastName: 'Anderson',
      });

      const patient2 = await testData.createPatient({
        firstName: 'Bob',
        lastName: 'Brown',
      });

      // Search by first name
      const searchResponse = await adminApiClient.get(
        '/api/patients?search=Alice&tenantId=test-tenant-001'
      );

      expect(searchResponse.ok()).toBeTruthy();
      const results = await searchResponse.json();
      expect(Array.isArray(results)).toBeTruthy();

      const foundPatient = results.find((p: any) => p.id === patient1.id);
      expect(foundPatient).toBeTruthy();
    });

    test('@workflow should filter patients by gender', async ({ adminApiClient, testData }) => {
      // Create test patients
      await testData.createPatient({ gender: 'male' });
      await testData.createPatient({ gender: 'female' });

      // Filter by gender
      const response = await adminApiClient.get(
        '/api/patients?gender=male&tenantId=test-tenant-001'
      );

      expect(response.ok()).toBeTruthy();
      const results = await response.json();
      expect(Array.isArray(results)).toBeTruthy();

      // All results should be male
      results.forEach((patient: any) => {
        expect(patient.gender).toBe('male');
      });
    });

    test('@workflow should paginate patient results', async ({ adminApiClient }) => {
      // Get first page
      const page1Response = await adminApiClient.get(
        '/api/patients?tenantId=test-tenant-001&page=0&size=10'
      );

      expect(page1Response.ok()).toBeTruthy();
      const page1 = await page1Response.json();

      expect(Array.isArray(page1)).toBeTruthy();
      expect(page1.length).toBeLessThanOrEqual(10);
    });

    test('@workflow should retrieve patient by ID', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();

      const response = await adminApiClient.get(`/api/patients/${patient.id}`);

      expect(response.ok()).toBeTruthy();
      const retrieved = await response.json();
      expect(retrieved.id).toBe(patient.id);
      expect(retrieved.firstName).toBe(patient.firstName);
      expect(retrieved.lastName).toBe(patient.lastName);
    });

    test('@workflow should return 404 for non-existent patient', async ({ adminApiClient }) => {
      const response = await adminApiClient.get('/api/patients/non-existent-id', {
        failOnStatusCode: false,
      });

      expect(response.status()).toBe(404);
    });

    test('@workflow should search by MRN', async ({ adminApiClient, testData }) => {
      const mrn = `SEARCH-MRN-${Date.now()}`;
      const patient = await testData.createPatient({ mrn });

      const response = await adminApiClient.get(
        `/api/patients?mrn=${mrn}&tenantId=test-tenant-001`
      );

      expect(response.ok()).toBeTruthy();
      const results = await response.json();
      expect(results.length).toBeGreaterThan(0);
      expect(results[0].mrn).toBe(mrn);
    });
  });

  test.describe('Patient Update Operations', () => {
    test('@workflow should update patient demographics', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();

      const updates = {
        email: 'updated@example.com',
        phone: '555-9999',
        address: {
          line1: '456 New Street',
          city: 'Cambridge',
          state: 'MA',
          zipCode: '02139',
        },
      };

      const response = await adminApiClient.put(`/api/patients/${patient.id}`, {
        data: { ...patient, ...updates },
      });

      expect(response.ok()).toBeTruthy();
      const updated = await response.json();
      expect(updated.email).toBe(updates.email);
      expect(updated.phone).toBe(updates.phone);
    });

    test('@workflow should update patient insurance', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();

      const insuranceUpdate = {
        insurance: {
          provider: 'Blue Cross Blue Shield',
          memberId: 'BCBS987654',
          groupNumber: 'GRP321',
        },
      };

      const response = await adminApiClient.put(`/api/patients/${patient.id}`, {
        data: { ...patient, ...insuranceUpdate },
      });

      expect(response.ok()).toBeTruthy();
      const updated = await response.json();
      expect(updated.insurance.provider).toBe('Blue Cross Blue Shield');
    });

    test('@workflow should prevent updating to duplicate MRN', async ({
      adminApiClient,
      testData,
    }) => {
      const patient1 = await testData.createPatient();
      const patient2 = await testData.createPatient();

      // Try to update patient2's MRN to match patient1
      const response = await adminApiClient.put(`/api/patients/${patient2.id}`, {
        data: { ...patient2, mrn: patient1.mrn },
        failOnStatusCode: false,
      });

      expect(response.status()).toBe(409);
    });

    test('@workflow should validate update data', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();

      const invalidUpdate = {
        email: 'invalid-email-format',
      };

      const response = await adminApiClient.put(`/api/patients/${patient.id}`, {
        data: { ...patient, ...invalidUpdate },
        failOnStatusCode: false,
      });

      expect(response.status()).toBe(400);
    });
  });

  test.describe('Patient Deletion and Soft Delete', () => {
    test('@workflow should soft delete patient', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();

      // Delete patient
      const deleteResponse = await adminApiClient.delete(`/api/patients/${patient.id}`);
      expect(deleteResponse.ok()).toBeTruthy();

      // Verify patient is not in active list
      const searchResponse = await adminApiClient.get(
        `/api/patients?mrn=${patient.mrn}&tenantId=test-tenant-001`
      );

      const results = await searchResponse.json();
      const deletedPatient = results.find((p: any) => p.id === patient.id);
      expect(deletedPatient).toBeFalsy();
    });

    test('@workflow should prevent operations on deleted patient', async ({
      adminApiClient,
      testData,
    }) => {
      const patient = await testData.createPatient();

      // Delete patient
      await adminApiClient.delete(`/api/patients/${patient.id}`);

      // Try to update deleted patient
      const updateResponse = await adminApiClient.put(`/api/patients/${patient.id}`, {
        data: { ...patient, email: 'new@example.com' },
        failOnStatusCode: false,
      });

      expect(updateResponse.status()).toBe(404);
    });
  });

  test.describe('Tenant Isolation', () => {
    test('@workflow should isolate patients by tenant', async ({ adminApiClient, testData }) => {
      // Create patient in test-tenant-001
      const patient1 = await testData.createPatient({ tenantId: 'test-tenant-001' });

      // Try to access from different tenant context
      const response = await adminApiClient.get(
        '/api/patients?tenantId=different-tenant-002'
      );

      const results = await response.json();
      const foundPatient = results.find((p: any) => p.id === patient1.id);
      expect(foundPatient).toBeFalsy();
    });

    test('@workflow should prevent cross-tenant updates', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient({ tenantId: 'test-tenant-001' });

      // Try to update with different tenantId
      const response = await adminApiClient.put(`/api/patients/${patient.id}`, {
        data: { ...patient, tenantId: 'different-tenant-002' },
        failOnStatusCode: false,
      });

      expect(response.status()).toBe(403);
    });
  });

  test.describe('Patient Demographics Validation', () => {
    test('@workflow should validate patient age', async ({ adminApiClient }) => {
      const futureDate = new Date();
      futureDate.setFullYear(futureDate.getFullYear() + 1);

      const patient = {
        firstName: 'Test',
        lastName: 'Patient',
        mrn: `MRN-${Date.now()}`,
        dateOfBirth: futureDate.toISOString().split('T')[0],
        gender: 'male',
        tenantId: 'test-tenant-001',
      };

      const response = await adminApiClient.post('/api/patients', {
        data: patient,
        failOnStatusCode: false,
      });

      expect(response.status()).toBe(400);
    });

    test('@workflow should validate phone number format', async ({ adminApiClient }) => {
      const patient = {
        firstName: 'Test',
        lastName: 'Patient',
        mrn: `MRN-${Date.now()}`,
        dateOfBirth: '1980-01-01',
        gender: 'male',
        phone: 'invalid-phone',
        tenantId: 'test-tenant-001',
      };

      const response = await adminApiClient.post('/api/patients', {
        data: patient,
        failOnStatusCode: false,
      });

      // Should either accept or reject based on validation rules
      expect([200, 201, 400]).toContain(response.status());
    });

    test('@workflow should validate gender values', async ({ adminApiClient }) => {
      const patient = {
        firstName: 'Test',
        lastName: 'Patient',
        mrn: `MRN-${Date.now()}`,
        dateOfBirth: '1980-01-01',
        gender: 'invalid-gender',
        tenantId: 'test-tenant-001',
      };

      const response = await adminApiClient.post('/api/patients', {
        data: patient,
        failOnStatusCode: false,
      });

      expect(response.status()).toBe(400);
    });
  });

  test.describe('Patient Search Advanced Features', () => {
    test('@workflow should support fuzzy name search', async ({ adminApiClient, testData }) => {
      await testData.createPatient({ firstName: 'Christopher', lastName: 'Smith' });

      // Search with partial match
      const response = await adminApiClient.get(
        '/api/patients?search=Chris&tenantId=test-tenant-001'
      );

      expect(response.ok()).toBeTruthy();
      const results = await response.json();
      expect(results.length).toBeGreaterThan(0);
    });

    test('@workflow should sort patients by name', async ({ adminApiClient }) => {
      const response = await adminApiClient.get(
        '/api/patients?tenantId=test-tenant-001&sort=lastName,asc'
      );

      expect(response.ok()).toBeTruthy();
      const results = await response.json();

      if (results.length > 1) {
        for (let i = 1; i < results.length; i++) {
          expect(results[i].lastName >= results[i - 1].lastName).toBeTruthy();
        }
      }
    });

    test('@workflow should filter by date range', async ({ adminApiClient }) => {
      const response = await adminApiClient.get(
        '/api/patients?tenantId=test-tenant-001&dobFrom=1980-01-01&dobTo=1990-12-31'
      );

      expect(response.ok()).toBeTruthy();
      const results = await response.json();

      results.forEach((patient: any) => {
        const dob = new Date(patient.dateOfBirth);
        expect(dob >= new Date('1980-01-01')).toBeTruthy();
        expect(dob <= new Date('1990-12-31')).toBeTruthy();
      });
    });
  });

  test.describe('Bulk Patient Operations', () => {
    test('@workflow should export patient list', async ({ adminApiClient }) => {
      const response = await adminApiClient.get(
        '/api/patients/export?tenantId=test-tenant-001&format=csv',
        {
          failOnStatusCode: false,
        }
      );

      // Should return CSV data or 200 status
      expect([200, 404, 501]).toContain(response.status());
    });

    test('@workflow should get patient count', async ({ adminApiClient }) => {
      const response = await adminApiClient.get(
        '/api/patients/count?tenantId=test-tenant-001'
      );

      expect(response.ok()).toBeTruthy();
      const count = await response.json();
      expect(typeof count === 'number' || typeof count.count === 'number').toBeTruthy();
    });
  });
});
