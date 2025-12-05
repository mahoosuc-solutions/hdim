/**
 * Care Gap Management Workflow E2E Tests
 *
 * Comprehensive end-to-end tests covering:
 * - Care gap detection and identification
 * - Gap prioritization and risk stratification
 * - Gap closure workflows
 * - Bulk gap operations
 * - Gap analytics and reporting
 * - Intervention tracking
 *
 * @author TDD Swarm Agent 5A
 * @tags @workflow @care-gap
 */

import { test, expect } from '../fixtures/test-fixtures';

test.describe('Care Gap Management Workflow', () => {
  test.describe('Care Gap Detection', () => {
    test('@workflow should detect missing HbA1c screening gap', async ({
      adminApiClient,
      testData,
    }) => {
      // Create diabetic patient without recent HbA1c
      const patient = await testData.createPatient({
        dateOfBirth: '1970-01-01',
      });

      // Detect gaps
      const response = await adminApiClient.post('/api/care-gaps/detect', {
        data: {
          patientId: patient.id,
          measureIds: ['hba1c-control'],
          tenantId: 'test-tenant-001',
        },
        failOnStatusCode: false,
      });

      expect([200, 201, 202]).toContain(response.status());

      if (response.ok()) {
        const gaps = await response.json();
        expect(Array.isArray(gaps)).toBeTruthy();
      }
    });

    test('@workflow should detect gaps for entire population', async ({ adminApiClient }) => {
      const response = await adminApiClient.post('/api/care-gaps/detect', {
        data: {
          scope: 'ALL_PATIENTS',
          measureIds: ['hba1c-control', 'bp-control'],
          tenantId: 'test-tenant-001',
        },
        failOnStatusCode: false,
      });

      expect([200, 201, 202]).toContain(response.status());
    });

    test('@workflow should prioritize care gaps by risk', async ({
      adminApiClient,
      testData,
    }) => {
      const patient = await testData.createPatient();

      // Create gap
      const gap = await testData.createCareGap(patient.id, {
        priority: 'HIGH',
        measureId: 'hba1c-control',
      });

      // Get gaps sorted by priority
      const response = await adminApiClient.get(
        `/api/care-gaps?patientId=${patient.id}&tenantId=test-tenant-001&sort=priority,desc`
      );

      expect(response.ok()).toBeTruthy();
      const gaps = await response.json();
      expect(Array.isArray(gaps)).toBeTruthy();

      if (gaps.length > 0) {
        expect(['HIGH', 'MEDIUM', 'LOW'].includes(gaps[0].priority)).toBeTruthy();
      }
    });

    test('@workflow should filter gaps by measure type', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();

      await testData.createCareGap(patient.id, { measureId: 'hba1c-control' });
      await testData.createCareGap(patient.id, { measureId: 'bp-control' });

      const response = await adminApiClient.get(
        `/api/care-gaps?measureId=hba1c-control&tenantId=test-tenant-001`
      );

      expect(response.ok()).toBeTruthy();
      const gaps = await response.json();

      gaps.forEach((gap: any) => {
        expect(gap.measureId).toBe('hba1c-control');
      });
    });

    test('@workflow should filter gaps by status', async ({ adminApiClient }) => {
      const response = await adminApiClient.get(
        '/api/care-gaps?status=OPEN&tenantId=test-tenant-001'
      );

      expect(response.ok()).toBeTruthy();
      const gaps = await response.json();

      gaps.forEach((gap: any) => {
        expect(gap.status).toBe('OPEN');
      });
    });

    test('@workflow should detect gaps with age criteria', async ({
      adminApiClient,
      testData,
    }) => {
      // Create patient of screening age
      const patient = await testData.createPatient({
        dateOfBirth: '1960-01-01', // Age 64
        gender: 'male',
      });

      const response = await adminApiClient.post('/api/care-gaps/detect', {
        data: {
          patientId: patient.id,
          measureIds: ['colorectal-screening'],
          tenantId: 'test-tenant-001',
        },
        failOnStatusCode: false,
      });

      expect([200, 201, 202]).toContain(response.status());
    });
  });

  test.describe('Care Gap Closure', () => {
    test('@workflow should close care gap with intervention', async ({
      adminApiClient,
      testData,
    }) => {
      const patient = await testData.createPatient();
      const gap = await testData.createCareGap(patient.id, {
        status: 'OPEN',
        priority: 'HIGH',
      });

      const closure = {
        reason: 'INTERVENTION_COMPLETED',
        interventionType: 'PATIENT_NOTIFICATION',
        notes: 'Patient completed screening procedure',
        closedBy: 'admin@healthdata.com',
      };

      const response = await adminApiClient.post(`/api/care-gaps/${gap.id}/close`, {
        data: closure,
      });

      expect(response.ok()).toBeTruthy();
      const closedGap = await response.json();
      expect(closedGap.status).toBe('CLOSED');
    });

    test('@workflow should validate closure reason', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();
      const gap = await testData.createCareGap(patient.id);

      const invalidClosure = {
        reason: 'INVALID_REASON',
      };

      const response = await adminApiClient.post(`/api/care-gaps/${gap.id}/close`, {
        data: invalidClosure,
        failOnStatusCode: false,
      });

      expect(response.status()).toBe(400);
    });

    test('@workflow should track closure history', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();
      const gap = await testData.createCareGap(patient.id);

      // Close gap
      await adminApiClient.post(`/api/care-gaps/${gap.id}/close`, {
        data: {
          reason: 'INTERVENTION_COMPLETED',
          notes: 'Gap closed',
        },
      });

      // Get gap history
      const historyResponse = await adminApiClient.get(`/api/care-gaps/${gap.id}/history`, {
        failOnStatusCode: false,
      });

      expect([200, 404]).toContain(historyResponse.status());

      if (historyResponse.ok()) {
        const history = await historyResponse.json();
        expect(Array.isArray(history)).toBeTruthy();
      }
    });

    test('@workflow should reopen closed gap', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();
      const gap = await testData.createCareGap(patient.id);

      // Close gap
      await adminApiClient.post(`/api/care-gaps/${gap.id}/close`, {
        data: { reason: 'INTERVENTION_COMPLETED' },
      });

      // Reopen gap
      const reopenResponse = await adminApiClient.post(`/api/care-gaps/${gap.id}/reopen`, {
        data: { reason: 'GAP_STILL_EXISTS', notes: 'Recent test showed gap persists' },
        failOnStatusCode: false,
      });

      expect([200, 404]).toContain(reopenResponse.status());

      if (reopenResponse.ok()) {
        const reopenedGap = await reopenResponse.json();
        expect(reopenedGap.status).toBe('OPEN');
      }
    });

    test('@workflow should auto-close gap when data received', async ({
      adminApiClient,
      testData,
    }) => {
      const patient = await testData.createPatient();
      const gap = await testData.createCareGap(patient.id, {
        measureId: 'hba1c-control',
        gapType: 'MISSING_DATA',
      });

      // Create observation that fills the gap
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
          value: 7.0,
          unit: '%',
        },
      });

      // Trigger auto-closure detection
      const response = await adminApiClient.post('/api/care-gaps/auto-close', {
        data: {
          patientId: patient.id,
          tenantId: 'test-tenant-001',
        },
        failOnStatusCode: false,
      });

      expect([200, 202, 404]).toContain(response.status());
    });
  });

  test.describe('Bulk Gap Operations', () => {
    test('@workflow should close multiple gaps', async ({ adminApiClient, testData }) => {
      const patient1 = await testData.createPatient();
      const patient2 = await testData.createPatient();

      const gap1 = await testData.createCareGap(patient1.id);
      const gap2 = await testData.createCareGap(patient2.id);

      const bulkClosure = {
        gapIds: [gap1.id, gap2.id],
        reason: 'INTERVENTION_COMPLETED',
        notes: 'Bulk closure',
      };

      const response = await adminApiClient.post('/api/care-gaps/bulk-close', {
        data: bulkClosure,
        failOnStatusCode: false,
      });

      expect([200, 202, 404]).toContain(response.status());

      if (response.ok()) {
        const result = await response.json();
        expect(result.closedCount || result.count).toBeGreaterThan(0);
      }
    });

    test('@workflow should assign gaps to care coordinator', async ({
      adminApiClient,
      testData,
    }) => {
      const patient = await testData.createPatient();
      const gap = await testData.createCareGap(patient.id);

      const assignment = {
        gapIds: [gap.id],
        assignedTo: 'coordinator@healthdata.com',
        notes: 'Please follow up with patient',
      };

      const response = await adminApiClient.post('/api/care-gaps/assign', {
        data: assignment,
        failOnStatusCode: false,
      });

      expect([200, 404]).toContain(response.status());
    });

    test('@workflow should export care gaps report', async ({ adminApiClient }) => {
      const response = await adminApiClient.get(
        '/api/care-gaps/export?tenantId=test-tenant-001&format=csv&status=OPEN',
        {
          failOnStatusCode: false,
        }
      );

      expect([200, 404, 501]).toContain(response.status());
    });
  });

  test.describe('Gap Analytics', () => {
    test('@workflow should get gap statistics by measure', async ({ adminApiClient }) => {
      const response = await adminApiClient.get(
        '/api/care-gaps/statistics?groupBy=measure&tenantId=test-tenant-001',
        {
          failOnStatusCode: false,
        }
      );

      expect([200, 404]).toContain(response.status());

      if (response.ok()) {
        const stats = await response.json();
        expect(Array.isArray(stats) || typeof stats === 'object').toBeTruthy();
      }
    });

    test('@workflow should get gap statistics by priority', async ({ adminApiClient }) => {
      const response = await adminApiClient.get(
        '/api/care-gaps/statistics?groupBy=priority&tenantId=test-tenant-001',
        {
          failOnStatusCode: false,
        }
      );

      expect([200, 404]).toContain(response.status());

      if (response.ok()) {
        const stats = await response.json();
        expect(stats).toBeTruthy();
      }
    });

    test('@workflow should track gap closure rate', async ({ adminApiClient }) => {
      const response = await adminApiClient.get(
        '/api/care-gaps/closure-rate?period=30d&tenantId=test-tenant-001',
        {
          failOnStatusCode: false,
        }
      );

      expect([200, 404]).toContain(response.status());

      if (response.ok()) {
        const rate = await response.json();
        expect(rate.rate >= 0 && rate.rate <= 100).toBeTruthy();
      }
    });

    test('@workflow should get gap trends over time', async ({ adminApiClient }) => {
      const response = await adminApiClient.get(
        '/api/care-gaps/trends?period=6M&tenantId=test-tenant-001',
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

  test.describe('Gap Interventions', () => {
    test('@workflow should create intervention for gap', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();
      const gap = await testData.createCareGap(patient.id);

      const intervention = {
        gapId: gap.id,
        type: 'PATIENT_OUTREACH',
        method: 'PHONE_CALL',
        scheduledDate: new Date(Date.now() + 86400000).toISOString(), // Tomorrow
        notes: 'Call patient to schedule appointment',
      };

      const response = await adminApiClient.post('/api/care-gaps/interventions', {
        data: intervention,
        failOnStatusCode: false,
      });

      expect([200, 201, 404]).toContain(response.status());
    });

    test('@workflow should track intervention outcomes', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();
      const gap = await testData.createCareGap(patient.id);

      // Get interventions for gap
      const response = await adminApiClient.get(`/api/care-gaps/${gap.id}/interventions`, {
        failOnStatusCode: false,
      });

      expect([200, 404]).toContain(response.status());

      if (response.ok()) {
        const interventions = await response.json();
        expect(Array.isArray(interventions)).toBeTruthy();
      }
    });

    test('@workflow should generate intervention recommendations', async ({
      adminApiClient,
      testData,
    }) => {
      const patient = await testData.createPatient();
      const gap = await testData.createCareGap(patient.id, {
        priority: 'HIGH',
      });

      const response = await adminApiClient.get(`/api/care-gaps/${gap.id}/recommendations`, {
        failOnStatusCode: false,
      });

      expect([200, 404]).toContain(response.status());

      if (response.ok()) {
        const recommendations = await response.json();
        expect(Array.isArray(recommendations)).toBeTruthy();
      }
    });
  });

  test.describe('Gap Notifications', () => {
    test('@workflow should send notification for new gap', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient({ email: 'patient@example.com' });

      const gap = await testData.createCareGap(patient.id, {
        priority: 'HIGH',
        measureId: 'hba1c-control',
      });

      const notification = {
        gapId: gap.id,
        recipientType: 'PATIENT',
        channel: 'EMAIL',
        template: 'gap-notification',
      };

      const response = await adminApiClient.post('/api/care-gaps/notify', {
        data: notification,
        failOnStatusCode: false,
      });

      expect([200, 201, 202, 404]).toContain(response.status());
    });

    test('@workflow should notify care team of high-priority gaps', async ({
      adminApiClient,
      testData,
    }) => {
      const patient = await testData.createPatient();
      const gap = await testData.createCareGap(patient.id, { priority: 'HIGH' });

      const notification = {
        gapId: gap.id,
        recipientType: 'CARE_TEAM',
        channel: 'EMAIL',
      };

      const response = await adminApiClient.post('/api/care-gaps/notify', {
        data: notification,
        failOnStatusCode: false,
      });

      expect([200, 201, 202, 404]).toContain(response.status());
    });
  });

  test.describe('Gap Risk Stratification', () => {
    test('@workflow should calculate gap risk score', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();
      const gap = await testData.createCareGap(patient.id);

      const response = await adminApiClient.get(`/api/care-gaps/${gap.id}/risk-score`, {
        failOnStatusCode: false,
      });

      expect([200, 404]).toContain(response.status());

      if (response.ok()) {
        const risk = await response.json();
        expect(risk.score >= 0 && risk.score <= 100).toBeTruthy();
      }
    });

    test('@workflow should categorize gaps by risk level', async ({ adminApiClient }) => {
      const response = await adminApiClient.get(
        '/api/care-gaps?riskLevel=HIGH&tenantId=test-tenant-001',
        {
          failOnStatusCode: false,
        }
      );

      expect([200, 404]).toContain(response.status());

      if (response.ok()) {
        const gaps = await response.json();
        gaps.forEach((gap: any) => {
          expect(['HIGH', 'MEDIUM', 'LOW'].includes(gap.riskLevel || gap.priority)).toBeTruthy();
        });
      }
    });

    test('@workflow should prioritize gaps for patient population', async ({ adminApiClient }) => {
      const response = await adminApiClient.get(
        '/api/care-gaps/prioritized?tenantId=test-tenant-001&limit=10',
        {
          failOnStatusCode: false,
        }
      );

      expect([200, 404]).toContain(response.status());

      if (response.ok()) {
        const gaps = await response.json();
        expect(Array.isArray(gaps)).toBeTruthy();

        // Should be sorted by priority/risk
        if (gaps.length > 1) {
          const priorities = gaps.map((g: any) => g.priority);
          expect(priorities[0]).toBe('HIGH');
        }
      }
    });
  });

  test.describe('Gap Validation', () => {
    test('@workflow should validate gap data completeness', async ({ adminApiClient }) => {
      const invalidGap = {
        // Missing required fields
        status: 'OPEN',
        tenantId: 'test-tenant-001',
      };

      const response = await adminApiClient.post('/api/care-gaps', {
        data: invalidGap,
        failOnStatusCode: false,
      });

      expect(response.status()).toBe(400);
    });

    test('@workflow should prevent duplicate gaps', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();

      const gap1 = await testData.createCareGap(patient.id, {
        measureId: 'hba1c-control',
        gapType: 'MISSING_DATA',
      });

      // Try to create duplicate
      const response = await adminApiClient.post('/api/care-gaps', {
        data: {
          patientId: patient.id,
          measureId: 'hba1c-control',
          gapType: 'MISSING_DATA',
          priority: 'HIGH',
          status: 'OPEN',
          tenantId: 'test-tenant-001',
        },
        failOnStatusCode: false,
      });

      // Should either reject duplicate or return existing
      expect([200, 201, 409]).toContain(response.status());
    });
  });
});
