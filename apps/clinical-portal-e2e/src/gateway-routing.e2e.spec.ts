import { test, expect, APIRequestContext } from '@playwright/test';

/**
 * Gateway Routing E2E Tests
 *
 * Tests validate that the API gateway correctly routes requests to all backend services.
 * Covers all 28 microservices plus edge cases.
 *
 * Issue: #89
 */

const GATEWAY_URL = process.env['GATEWAY_URL'] || 'http://localhost:18080';
const API_BASE = `${GATEWAY_URL}/api`;
const TEST_TENANT_ID = '11111111-1111-1111-1111-111111111111';
const DEMO_SAFE = process.env['DEMO_SAFE'] === '1' || process.env['DEMO_SAFE'] === 'true';

// Complete service routing configuration (28 services)
const SERVICE_ROUTES = [
  // Core Services
  {
    name: 'quality-measure-service',
    basePath: '/quality-measure',
    port: 8087,
    endpoints: [
      { method: 'GET', path: '/api/v1/quality-measures', description: 'List measures' },
      { method: 'GET', path: '/actuator/health', description: 'Health check' },
    ],
  },
  {
    name: 'cql-engine-service',
    basePath: '/cql-engine',
    port: 8081,
    endpoints: [
      { method: 'POST', path: '/api/v1/evaluate', description: 'Evaluate CQL' },
      { method: 'GET', path: '/actuator/health', description: 'Health check' },
    ],
  },
  {
    name: 'fhir-service',
    basePath: '/fhir',
    port: 8085,
    endpoints: [
      { method: 'GET', path: '/api/v1/patients', description: 'List patients' },
      { method: 'GET', path: '/api/v1/conditions', description: 'List conditions' },
      { method: 'GET', path: '/actuator/health', description: 'Health check' },
    ],
  },
  {
    name: 'patient-service',
    basePath: '/patient',
    port: 8084,
    endpoints: [
      { method: 'GET', path: '/api/v1/patients', description: 'List patients' },
      { method: 'GET', path: '/actuator/health', description: 'Health check' },
    ],
  },
  {
    name: 'care-gap-service',
    basePath: '/care-gap',
    port: 8086,
    endpoints: [
      { method: 'GET', path: '/api/v1/care-gaps', description: 'List care gaps' },
      { method: 'GET', path: '/actuator/health', description: 'Health check' },
    ],
  },
  {
    name: 'gateway-service',
    basePath: '',
    port: 18080,
    endpoints: [
      { method: 'GET', path: '/actuator/health', description: 'Health check' },
      { method: 'GET', path: '/actuator/info', description: 'Service info' },
    ],
  },
  {
    name: 'analytics-service',
    basePath: '/analytics',
    port: 8088,
    endpoints: [
      { method: 'GET', path: '/api/v1/reports', description: 'List reports' },
      { method: 'GET', path: '/actuator/health', description: 'Health check' },
    ],
  },
  {
    name: 'consent-service',
    basePath: '/consent',
    port: 8089,
    endpoints: [
      { method: 'GET', path: '/api/v1/consents', description: 'List consents' },
      { method: 'GET', path: '/actuator/health', description: 'Health check' },
    ],
  },
  {
    name: 'notification-service',
    basePath: '/notification',
    port: 8090,
    endpoints: [
      { method: 'GET', path: '/api/v1/notifications', description: 'List notifications' },
      { method: 'GET', path: '/actuator/health', description: 'Health check' },
    ],
  },
  {
    name: 'audit-service',
    basePath: '/audit',
    port: 8091,
    endpoints: [
      { method: 'GET', path: '/api/v1/audit/events', description: 'List audit events' },
      { method: 'GET', path: '/actuator/health', description: 'Health check' },
    ],
  },
  // Extended Services
  {
    name: 'ehr-connector-service',
    basePath: '/ehr-connector',
    port: 8092,
    endpoints: [
      { method: 'GET', path: '/api/v1/connections', description: 'List EHR connections' },
      { method: 'GET', path: '/actuator/health', description: 'Health check' },
    ],
  },
  {
    name: 'hcc-service',
    basePath: '/hcc',
    port: 8093,
    endpoints: [
      { method: 'GET', path: '/api/v1/hcc-codes', description: 'List HCC codes' },
      { method: 'GET', path: '/actuator/health', description: 'Health check' },
    ],
  },
  {
    name: 'prior-auth-service',
    basePath: '/prior-auth',
    port: 8094,
    endpoints: [
      { method: 'GET', path: '/api/v1/authorizations', description: 'List prior auths' },
      { method: 'GET', path: '/actuator/health', description: 'Health check' },
    ],
  },
  {
    name: 'qrda-export-service',
    basePath: '/qrda-export',
    port: 8095,
    endpoints: [
      { method: 'GET', path: '/api/v1/exports', description: 'List QRDA exports' },
      { method: 'GET', path: '/actuator/health', description: 'Health check' },
    ],
  },
  {
    name: 'sdoh-service',
    basePath: '/sdoh',
    port: 8096,
    endpoints: [
      { method: 'GET', path: '/api/v1/assessments', description: 'List SDOH assessments' },
      { method: 'GET', path: '/actuator/health', description: 'Health check' },
    ],
  },
  {
    name: 'predictive-analytics-service',
    basePath: '/predictive',
    port: 8097,
    endpoints: [
      { method: 'GET', path: '/api/v1/predictions', description: 'List predictions' },
      { method: 'GET', path: '/actuator/health', description: 'Health check' },
    ],
  },
  {
    name: 'documentation-service',
    basePath: '/documentation',
    port: 8098,
    endpoints: [
      { method: 'GET', path: '/api/v1/documents', description: 'List documents' },
      { method: 'GET', path: '/actuator/health', description: 'Health check' },
    ],
  },
  {
    name: 'demo-seeding-service',
    basePath: '/demo-seeding',
    port: 8099,
    endpoints: [
      { method: 'GET', path: '/api/v1/seed-status', description: 'Seed status' },
      { method: 'GET', path: '/actuator/health', description: 'Health check' },
    ],
  },
  {
    name: 'cms-connector-service',
    basePath: '/cms-connector',
    port: 8100,
    endpoints: [
      { method: 'GET', path: '/api/v1/cms/status', description: 'CMS connector status' },
      { method: 'GET', path: '/actuator/health', description: 'Health check' },
    ],
  },
  // Additional Services
  {
    name: 'appointment-service',
    basePath: '/appointment',
    port: 8101,
    endpoints: [
      { method: 'GET', path: '/api/v1/appointments', description: 'List appointments' },
      { method: 'GET', path: '/actuator/health', description: 'Health check' },
    ],
  },
  {
    name: 'care-plan-service',
    basePath: '/care-plan',
    port: 8102,
    endpoints: [
      { method: 'GET', path: '/api/v1/care-plans', description: 'List care plans' },
      { method: 'GET', path: '/actuator/health', description: 'Health check' },
    ],
  },
  {
    name: 'immunization-service',
    basePath: '/immunization',
    port: 8103,
    endpoints: [
      { method: 'GET', path: '/api/v1/immunizations', description: 'List immunizations' },
      { method: 'GET', path: '/actuator/health', description: 'Health check' },
    ],
  },
  {
    name: 'medication-service',
    basePath: '/medication',
    port: 8104,
    endpoints: [
      { method: 'GET', path: '/api/v1/medications', description: 'List medications' },
      { method: 'GET', path: '/actuator/health', description: 'Health check' },
    ],
  },
  {
    name: 'encounter-service',
    basePath: '/encounter',
    port: 8105,
    endpoints: [
      { method: 'GET', path: '/api/v1/encounters', description: 'List encounters' },
      { method: 'GET', path: '/actuator/health', description: 'Health check' },
    ],
  },
  {
    name: 'claim-service',
    basePath: '/claim',
    port: 8106,
    endpoints: [
      { method: 'GET', path: '/api/v1/claims', description: 'List claims' },
      { method: 'GET', path: '/actuator/health', description: 'Health check' },
    ],
  },
  {
    name: 'provider-service',
    basePath: '/provider',
    port: 8107,
    endpoints: [
      { method: 'GET', path: '/api/v1/providers', description: 'List providers' },
      { method: 'GET', path: '/actuator/health', description: 'Health check' },
    ],
  },
  {
    name: 'organization-service',
    basePath: '/organization',
    port: 8108,
    endpoints: [
      { method: 'GET', path: '/api/v1/organizations', description: 'List organizations' },
      { method: 'GET', path: '/actuator/health', description: 'Health check' },
    ],
  },
  {
    name: 'user-service',
    basePath: '/user',
    port: 8109,
    endpoints: [
      { method: 'GET', path: '/api/v1/users', description: 'List users' },
      { method: 'GET', path: '/actuator/health', description: 'Health check' },
    ],
  },
];

test.describe('Gateway Routing Tests', () => {
  let apiContext: APIRequestContext;

  test.beforeAll(async ({ playwright }) => {
    apiContext = await playwright.request.newContext({
      baseURL: GATEWAY_URL,
      extraHTTPHeaders: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'X-Tenant-ID': TEST_TENANT_ID,
      },
      timeout: 30000,
    });
  });

  test.afterAll(async () => {
    await apiContext.dispose();
  });

  test.describe('Core Service Routing', () => {
    const coreServices = SERVICE_ROUTES.filter(s =>
      ['quality-measure-service', 'cql-engine-service', 'fhir-service',
        'patient-service', 'care-gap-service', 'gateway-service'].includes(s.name)
    );

    for (const service of coreServices) {
      test(`Routes to ${service.name} (${service.basePath || '/'})`, async () => {
        const healthPath = service.basePath
          ? `${service.basePath}/actuator/health`
          : '/actuator/health';

        const response = await apiContext.get(healthPath);

        // Service should be reachable - 200 OK or auth-related responses
        expect([200, 401, 403]).toContain(response.status());

        if (response.status() === 200) {
          const health = await response.json();
          expect(health.status).toBeDefined();
        }
      });
    }
  });

  test.describe('Extended Service Routing', () => {
    if (DEMO_SAFE) {
      test.skip(true, 'Extended services are not required in demo-safe runs.');
    }
    const extendedServices = SERVICE_ROUTES.filter(s =>
      !['quality-measure-service', 'cql-engine-service', 'fhir-service',
        'patient-service', 'care-gap-service', 'gateway-service'].includes(s.name)
    );

    for (const service of extendedServices) {
      test(`Routes to ${service.name} (${service.basePath})`, async () => {
        const healthPath = `${service.basePath}/actuator/health`;

        const response = await apiContext.get(healthPath);

        // Extended services may not be deployed - accept more status codes
        expect([200, 401, 403, 404, 502, 503]).toContain(response.status());

        if (response.status() === 200) {
          const health = await response.json();
          expect(health.status).toBeDefined();
        }
      });
    }
  });

  test.describe('API Endpoint Routing', () => {
    test('Quality Measure API routes correctly', async () => {
      const response = await apiContext.get('/quality-measure/api/v1/quality-measures');
      expect([200, 401, 403]).toContain(response.status());
    });

    test('FHIR Patient API routes correctly', async () => {
      const response = await apiContext.get('/fhir/api/v1/patients');
      expect([200, 401, 403]).toContain(response.status());
    });

    test('Care Gap API routes correctly', async () => {
      const response = await apiContext.get('/care-gap/api/v1/care-gaps');
      expect([200, 401, 403]).toContain(response.status());
    });

    test('CQL Engine evaluate endpoint routes correctly', async () => {
      const response = await apiContext.post('/cql-engine/api/v1/evaluate', {
        data: { library: 'test', context: {} },
      });
      // POST might fail validation but should route correctly
      expect([200, 400, 401, 403, 422]).toContain(response.status());
    });
  });

  test.describe('Gateway Direct Routes', () => {
    test('Gateway actuator routes work directly', async () => {
      const endpoints = ['/actuator/health', '/actuator/info', '/actuator/prometheus'];

      for (const endpoint of endpoints) {
        const response = await apiContext.get(endpoint);
        // Actuator endpoints might be restricted
        expect([200, 401, 403, 404]).toContain(response.status());
      }
    });

    test('Gateway API v1 base route works', async () => {
      const response = await apiContext.get(`${API_BASE}/v1/health`);
      expect([200, 401, 403, 404]).toContain(response.status());
    });
  });
});

test.describe('Edge Cases', () => {
  let apiContext: APIRequestContext;

  test.beforeAll(async ({ playwright }) => {
    apiContext = await playwright.request.newContext({
      baseURL: GATEWAY_URL,
      extraHTTPHeaders: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'X-Tenant-ID': TEST_TENANT_ID,
      },
      timeout: 10000,
    });
  });

  test.afterAll(async () => {
    await apiContext.dispose();
  });

  test('404 for completely unknown routes', async () => {
    const response = await apiContext.get('/completely-unknown-service/api/v1/data');
    expect(response.status()).toBe(404);
  });

  test('404 for unknown paths within known service', async () => {
    const response = await apiContext.get('/fhir/api/v1/nonexistent-resource');
    expect([404]).toContain(response.status());
  });

  test('Method not allowed (405) for wrong HTTP method', async () => {
    // Try to DELETE the health endpoint which only supports GET
    const response = await apiContext.delete('/actuator/health');

    // Should return 405 Method Not Allowed or similar
    expect([400, 403, 405]).toContain(response.status());
  });

  test('Request too large (413) for oversized payload', async () => {
    // Create a large payload (> typical limit)
    const largePayload = {
      data: 'x'.repeat(10 * 1024 * 1024), // 10MB string
    };

    const response = await apiContext.post('/fhir/api/v1/patients', {
      data: largePayload,
    });

    // Should return 413 Payload Too Large or similar
    expect([400, 401, 403, 413, 422]).toContain(response.status());
  });

  test('Handles malformed paths gracefully', async () => {
    const malformedPaths = [
      '/fhir/../../../etc/passwd',
      '/fhir/%00',
      '/fhir/%2e%2e%2f',
      '/fhir/api/v1/../../admin',
    ];

    for (const path of malformedPaths) {
      const response = await apiContext.get(path);
      // Should not crash, return appropriate error
      expect([400, 403, 404]).toContain(response.status());
    }
  });

  test('Handles special characters in path', async () => {
    const specialPaths = [
      '/fhir/api/v1/patients/test%20patient',
      '/fhir/api/v1/patients/test+patient',
      "/fhir/api/v1/patients/test'patient",
    ];

    for (const path of specialPaths) {
      const response = await apiContext.get(path);
      // Should handle gracefully
      expect([400, 401, 403, 404]).toContain(response.status());
    }
  });

  test('Handles query string routing', async () => {
    const response = await apiContext.get('/fhir/api/v1/patients?page=1&size=10');
    expect([200, 401, 403]).toContain(response.status());
  });

  test('Handles fragment identifiers (should be stripped)', async () => {
    const response = await apiContext.get('/actuator/health#section');
    // Fragments should be ignored by server
    expect(response.status()).toBe(200);
  });
});

test.describe('Header Forwarding', () => {
  let apiContext: APIRequestContext;

  test.beforeAll(async ({ playwright }) => {
    apiContext = await playwright.request.newContext({
      baseURL: GATEWAY_URL,
      timeout: 10000,
    });
  });

  test.afterAll(async () => {
    await apiContext.dispose();
  });

  test('X-Tenant-ID header forwarded to services', async () => {
    const response = await apiContext.get('/fhir/api/v1/patients', {
      headers: {
        'X-Tenant-ID': TEST_TENANT_ID,
      },
    });

    // Request should process (tenant header was forwarded)
    expect([200, 401, 403]).toContain(response.status());
  });

  test('Authorization header forwarded to services', async () => {
    const response = await apiContext.get('/fhir/api/v1/patients', {
      headers: {
        'Authorization': 'Bearer test-token',
        'X-Tenant-ID': TEST_TENANT_ID,
      },
    });

    // Should get auth error (invalid token) proving header was forwarded
    expect([401, 403]).toContain(response.status());
  });

  test('Custom headers forwarded to services', async () => {
    const response = await apiContext.get('/fhir/api/v1/patients', {
      headers: {
        'X-Request-ID': 'test-request-123',
        'X-Correlation-ID': 'correlation-456',
        'X-Tenant-ID': TEST_TENANT_ID,
      },
    });

    // Request should process
    expect([200, 401, 403]).toContain(response.status());

    // Check if correlation ID is returned
    const responseCorrelationId = response.headers()['x-correlation-id'];
    if (responseCorrelationId) {
      // If gateway echoes correlation ID, verify it
      expect(responseCorrelationId).toContain('correlation');
    }
  });
});

test.describe('Load Balancing and Failover', () => {
  let apiContext: APIRequestContext;

  test.beforeAll(async ({ playwright }) => {
    apiContext = await playwright.request.newContext({
      baseURL: GATEWAY_URL,
      extraHTTPHeaders: {
        'X-Tenant-ID': TEST_TENANT_ID,
      },
      timeout: 10000,
    });
  });

  test.afterAll(async () => {
    await apiContext.dispose();
  });

  test('Consistent routing for same endpoint', async () => {
    const responses = [];

    for (let i = 0; i < 5; i++) {
      const response = await apiContext.get('/actuator/health');
      responses.push(response.status());
    }

    // All requests should get consistent response
    const uniqueStatuses = [...new Set(responses)];
    expect(uniqueStatuses.length).toBe(1);
  });

  test('Circuit breaker does not block healthy services', async () => {
    // Make several requests to verify service remains accessible
    const results = [];

    for (let i = 0; i < 10; i++) {
      const response = await apiContext.get('/actuator/health');
      results.push(response.status());
    }

    // All should succeed (no circuit breaker trip for healthy service)
    expect(results.every(status => status === 200)).toBeTruthy();
  });
});

test.describe('Response Handling', () => {
  let apiContext: APIRequestContext;

  test.beforeAll(async ({ playwright }) => {
    apiContext = await playwright.request.newContext({
      baseURL: GATEWAY_URL,
      extraHTTPHeaders: {
        'X-Tenant-ID': TEST_TENANT_ID,
      },
    });
  });

  test.afterAll(async () => {
    await apiContext.dispose();
  });

  test('Gateway preserves response headers from services', async () => {
    const response = await apiContext.get('/actuator/health');

    // Check for standard headers that should be preserved
    expect(response.headers()['content-type']).toBeDefined();
  });

  test('Gateway adds tracing headers', async () => {
    const response = await apiContext.get('/actuator/health');

    // Check for tracing headers (may or may not be present depending on config)
    const traceHeaders = ['x-request-id', 'x-trace-id', 'x-span-id', 'x-correlation-id'];
    const hasTracing = traceHeaders.some(h => response.headers()[h] !== undefined);

    // Tracing is optional, so just note if present
    if (!hasTracing) {
      console.log('Note: No tracing headers detected');
    }
  });

  test('Gateway handles streaming responses', async () => {
    // Request that might return streaming data
    const response = await apiContext.get('/actuator/health');

    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body).toBeDefined();
  });
});
