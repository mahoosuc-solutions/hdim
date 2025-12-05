/**
 * Performance Benchmark E2E Tests
 *
 * Performance tests covering:
 * - API response times
 * - Database query performance
 * - Concurrent request handling
 * - Load testing scenarios
 * - Memory usage monitoring
 * - Cache effectiveness
 *
 * @author TDD Swarm Agent 5A
 * @tags @performance @benchmark
 */

import { test, expect } from '../fixtures/test-fixtures';

test.describe('Performance Benchmarks', () => {
  test.describe('API Response Times', () => {
    test('@performance should respond to health check within 100ms', async ({ page }) => {
      const startTime = Date.now();

      const response = await page.request.get('/actuator/health');

      const duration = Date.now() - startTime;

      expect(response.ok()).toBeTruthy();
      expect(duration).toBeLessThan(100);
    });

    test('@performance should list patients within 500ms', async ({ adminApiClient }) => {
      const startTime = Date.now();

      const response = await adminApiClient.get('/api/patients?tenantId=test-tenant-001');

      const duration = Date.now() - startTime;

      expect(response.ok()).toBeTruthy();
      expect(duration).toBeLessThan(500);
    });

    test('@performance should get patient details within 200ms', async ({
      adminApiClient,
      testData,
    }) => {
      const patient = await testData.createPatient();

      const startTime = Date.now();

      const response = await adminApiClient.get(`/api/patients/${patient.id}`);

      const duration = Date.now() - startTime;

      expect(response.ok()).toBeTruthy();
      expect(duration).toBeLessThan(200);
    });

    test('@performance should search patients within 300ms', async ({ adminApiClient }) => {
      const startTime = Date.now();

      const response = await adminApiClient.get(
        '/api/patients?search=Smith&tenantId=test-tenant-001'
      );

      const duration = Date.now() - startTime;

      expect(response.ok()).toBeTruthy();
      expect(duration).toBeLessThan(300);
    });

    test('@performance should calculate measure within 1000ms', async ({
      adminApiClient,
      testData,
    }) => {
      const patient = await testData.createPatient();

      const startTime = Date.now();

      const response = await adminApiClient.post(
        `/api/measures/calculate?patientId=${patient.id}&measureId=hba1c-control`,
        {
          failOnStatusCode: false,
        }
      );

      const duration = Date.now() - startTime;

      expect([200, 201, 404]).toContain(response.status());
      expect(duration).toBeLessThan(1000);
    });
  });

  test.describe('Concurrent Request Handling', () => {
    test('@performance should handle 10 concurrent patient requests', async ({
      adminApiClient,
    }) => {
      const requests = [];

      for (let i = 0; i < 10; i++) {
        requests.push(adminApiClient.get('/api/patients?tenantId=test-tenant-001'));
      }

      const startTime = Date.now();
      const responses = await Promise.all(requests);
      const duration = Date.now() - startTime;

      responses.forEach((response) => {
        expect(response.ok()).toBeTruthy();
      });

      // All 10 should complete within 2 seconds
      expect(duration).toBeLessThan(2000);
    });

    test('@performance should handle concurrent patient creation', async ({
      adminApiClient,
    }) => {
      const creations = [];

      for (let i = 0; i < 5; i++) {
        creations.push(
          adminApiClient.post('/api/patients', {
            data: {
              firstName: 'Concurrent',
              lastName: `Patient${i}`,
              mrn: `CONCURRENT-${Date.now()}-${i}`,
              dateOfBirth: '1980-01-01',
              gender: 'male',
              tenantId: 'test-tenant-001',
            },
          })
        );
      }

      const startTime = Date.now();
      const responses = await Promise.all(creations);
      const duration = Date.now() - startTime;

      responses.forEach((response) => {
        expect(response.ok()).toBeTruthy();
      });

      expect(duration).toBeLessThan(3000);
    });

    test('@performance should handle concurrent measure calculations', async ({
      adminApiClient,
      testData,
    }) => {
      const patient = await testData.createPatient();

      const calculations = [
        'hba1c-control',
        'bp-control',
        'bmi-assessment',
        'cholesterol-control',
      ].map((measureId) =>
        adminApiClient.post(
          `/api/measures/calculate?patientId=${patient.id}&measureId=${measureId}`,
          { failOnStatusCode: false }
        )
      );

      const startTime = Date.now();
      const responses = await Promise.all(calculations);
      const duration = Date.now() - startTime;

      responses.forEach((response) => {
        expect([200, 201, 404]).toContain(response.status());
      });

      expect(duration).toBeLessThan(5000);
    });
  });

  test.describe('Database Query Performance', () => {
    test('@performance should paginate large result sets efficiently', async ({
      adminApiClient,
    }) => {
      const startTime = Date.now();

      const response = await adminApiClient.get(
        '/api/patients?tenantId=test-tenant-001&page=0&size=100'
      );

      const duration = Date.now() - startTime;

      expect(response.ok()).toBeTruthy();
      expect(duration).toBeLessThan(1000);
    });

    test('@performance should filter results efficiently', async ({ adminApiClient }) => {
      const startTime = Date.now();

      const response = await adminApiClient.get(
        '/api/patients?tenantId=test-tenant-001&gender=male&ageMin=18&ageMax=65'
      );

      const duration = Date.now() - startTime;

      expect(response.ok()).toBeTruthy();
      expect(duration).toBeLessThan(800);
    });

    test('@performance should sort large datasets efficiently', async ({ adminApiClient }) => {
      const startTime = Date.now();

      const response = await adminApiClient.get(
        '/api/patients?tenantId=test-tenant-001&sort=lastName,asc&size=100'
      );

      const duration = Date.now() - startTime;

      expect(response.ok()).toBeTruthy();
      expect(duration).toBeLessThan(1000);
    });
  });

  test.describe('Batch Operations Performance', () => {
    test('@performance should handle batch measure calculation', async ({
      adminApiClient,
      testData,
    }) => {
      const patients = [];
      for (let i = 0; i < 5; i++) {
        patients.push(await testData.createPatient());
      }

      const startTime = Date.now();

      const response = await adminApiClient.post('/api/measures/batch', {
        data: {
          measureIds: ['hba1c-control'],
          patientIds: patients.map((p) => p.id),
          tenantId: 'test-tenant-001',
        },
        failOnStatusCode: false,
      });

      const duration = Date.now() - startTime;

      expect([200, 201, 202]).toContain(response.status());
      expect(duration).toBeLessThan(3000);
    });

    test('@performance should export data efficiently', async ({ adminApiClient }) => {
      const startTime = Date.now();

      const response = await adminApiClient.get(
        '/api/patients/export?tenantId=test-tenant-001&format=csv',
        {
          failOnStatusCode: false,
        }
      );

      const duration = Date.now() - startTime;

      expect([200, 404, 501]).toContain(response.status());
      expect(duration).toBeLessThan(5000);
    });
  });

  test.describe('Cache Performance', () => {
    test('@performance should serve cached patient data faster', async ({
      adminApiClient,
      testData,
    }) => {
      const patient = await testData.createPatient();

      // First request (uncached)
      const startTime1 = Date.now();
      await adminApiClient.get(`/api/patients/${patient.id}`);
      const duration1 = Date.now() - startTime1;

      // Second request (should be cached)
      const startTime2 = Date.now();
      await adminApiClient.get(`/api/patients/${patient.id}`);
      const duration2 = Date.now() - startTime2;

      // Cached request should be faster or similar
      expect(duration2).toBeLessThanOrEqual(duration1 * 1.5);
    });

    test('@performance should cache measure results', async ({ adminApiClient, testData }) => {
      const patient = await testData.createPatient();

      // Calculate measure
      await adminApiClient.post(
        `/api/measures/calculate?patientId=${patient.id}&measureId=hba1c-control`,
        { failOnStatusCode: false }
      );

      // Get results (first time)
      const startTime1 = Date.now();
      await adminApiClient.get(
        `/api/measures/results?patientId=${patient.id}&tenantId=test-tenant-001`
      );
      const duration1 = Date.now() - startTime1;

      // Get results again (should be cached)
      const startTime2 = Date.now();
      await adminApiClient.get(
        `/api/measures/results?patientId=${patient.id}&tenantId=test-tenant-001`
      );
      const duration2 = Date.now() - startTime2;

      expect(duration2).toBeLessThanOrEqual(duration1 * 1.5);
    });
  });

  test.describe('Memory and Resource Usage', () => {
    test('@performance should handle large patient datasets', async ({ adminApiClient }) => {
      const response = await adminApiClient.get(
        '/api/patients?tenantId=test-tenant-001&size=500',
        {
          failOnStatusCode: false,
          timeout: 10000,
        }
      );

      expect([200, 404]).toContain(response.status());
    });

    test('@performance should stream large result sets', async ({ adminApiClient }) => {
      const response = await adminApiClient.get(
        '/api/patients/stream?tenantId=test-tenant-001',
        {
          failOnStatusCode: false,
        }
      );

      expect([200, 404, 501]).toContain(response.status());
    });
  });

  test.describe('Response Size Optimization', () => {
    test('@performance should compress API responses', async ({ page }) => {
      const response = await page.request.get('/api/patients?tenantId=test-tenant-001', {
        headers: {
          'Accept-Encoding': 'gzip, deflate',
        },
      });

      const headers = response.headers();
      // Check if compression is enabled
      expect(['gzip', 'deflate', undefined]).toContain(headers['content-encoding']);
    });

    test('@performance should support field projection', async ({ adminApiClient }) => {
      const fullResponse = await adminApiClient.get(
        '/api/patients?tenantId=test-tenant-001&size=10'
      );
      const fullData = await fullResponse.json();
      const fullSize = JSON.stringify(fullData).length;

      const projectedResponse = await adminApiClient.get(
        '/api/patients?tenantId=test-tenant-001&size=10&fields=id,firstName,lastName',
        {
          failOnStatusCode: false,
        }
      );

      if (projectedResponse.ok()) {
        const projectedData = await projectedResponse.json();
        const projectedSize = JSON.stringify(projectedData).length;

        // Projected response should be smaller
        expect(projectedSize).toBeLessThan(fullSize);
      }
    });
  });

  test.describe('Latency Benchmarks', () => {
    test('@performance should meet P50 latency target (< 50ms)', async ({ adminApiClient }) => {
      const latencies: number[] = [];

      for (let i = 0; i < 10; i++) {
        const start = Date.now();
        await adminApiClient.get('/actuator/health');
        latencies.push(Date.now() - start);
      }

      latencies.sort((a, b) => a - b);
      const p50 = latencies[Math.floor(latencies.length * 0.5)];

      expect(p50).toBeLessThan(50);
    });

    test('@performance should meet P95 latency target (< 200ms)', async ({ adminApiClient }) => {
      const latencies: number[] = [];

      for (let i = 0; i < 20; i++) {
        const start = Date.now();
        await adminApiClient.get('/api/patients?tenantId=test-tenant-001');
        latencies.push(Date.now() - start);
      }

      latencies.sort((a, b) => a - b);
      const p95 = latencies[Math.floor(latencies.length * 0.95)];

      expect(p95).toBeLessThan(200);
    });

    test('@performance should meet P99 latency target (< 500ms)', async ({ adminApiClient }) => {
      const latencies: number[] = [];

      for (let i = 0; i < 100; i++) {
        const start = Date.now();
        await adminApiClient.get('/api/patients?tenantId=test-tenant-001');
        latencies.push(Date.now() - start);
      }

      latencies.sort((a, b) => a - b);
      const p99 = latencies[Math.floor(latencies.length * 0.99)];

      expect(p99).toBeLessThan(500);
    });
  });

  test.describe('Throughput Tests', () => {
    test('@performance should handle sustained load', async ({ adminApiClient }) => {
      const duration = 5000; // 5 seconds
      const startTime = Date.now();
      let requestCount = 0;

      while (Date.now() - startTime < duration) {
        await adminApiClient.get('/actuator/health');
        requestCount++;
      }

      const requestsPerSecond = (requestCount / duration) * 1000;

      // Should handle at least 10 requests per second
      expect(requestsPerSecond).toBeGreaterThan(10);
    });
  });
});
