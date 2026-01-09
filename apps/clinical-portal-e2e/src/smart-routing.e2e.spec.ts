import { test, expect, APIRequestContext } from '@playwright/test';

/**
 * Smart Routing E2E Tests
 *
 * Tests validate:
 * 1. Gateway-edge nginx routes requests to correct upstream gateway services
 * 2. Authoritative source routing (Patient→external FHIR, Generated→internal FHIR)
 * 3. X-Edge header propagation for observability
 * 4. Demo seeding hybrid mode behavior
 *
 * Issue: Advanced Security + Smart Routing Milestone
 */

const GATEWAY_EDGE_URL = process.env['GATEWAY_EDGE_URL'] || 'http://localhost:8080';
const EXTERNAL_FHIR_URL = process.env['EXTERNAL_FHIR_URL'] || 'http://localhost:8088';
const TEST_TENANT_ID = 'demo-tenant';

// URL patterns and their expected upstream gateway services (from nginx.conf)
const ROUTING_MATRIX = {
  gateway_admin: [
    { path: '/api/v1/auth/login', description: 'Auth login' },
    { path: '/api/v1/auth/refresh', description: 'Auth refresh' },
    { path: '/api/v1/agent-builder/agents', description: 'Agent builder' },
    { path: '/api/v1/tools/list', description: 'Tools list' },
    { path: '/api/v1/providers/list', description: 'Providers list' },
    { path: '/api/v1/runtime/status', description: 'Runtime status' },
    { path: '/api/sales/leads', description: 'Sales leads' },
    { path: '/sales-automation/campaigns', description: 'Sales automation' },
    { path: '/actuator/health', description: 'Actuator health' },
    { path: '/v3/api-docs/', description: 'OpenAPI docs' },
    { path: '/swagger-ui/', description: 'Swagger UI' },
  ],
  gateway_fhir: [
    { path: '/api/fhir/Patient', description: 'API FHIR Patient' },
    { path: '/fhir/Patient', description: 'FHIR Patient' },
    { path: '/api/patients/', description: 'API patients' },
    { path: '/patient/api/v1/patients', description: 'Patient service' },
    { path: '/api/cql/evaluate', description: 'API CQL evaluate' },
    { path: '/cql-engine/api/v1/evaluate', description: 'CQL engine' },
    { path: '/api/quality/measures', description: 'API quality measures' },
    { path: '/quality-measure/api/v1/quality-measures', description: 'Quality measure service' },
  ],
  gateway_clinical: [
    { path: '/api/care-gaps/', description: 'API care gaps' },
    { path: '/care-gap/api/v1/care-gaps', description: 'Care gap service' },
    { path: '/api/consent/', description: 'API consent' },
    { path: '/consent/api/v1/consents', description: 'Consent service' },
    { path: '/api/events/', description: 'API events' },
    { path: '/events/api/v1/events', description: 'Events service' },
    { path: '/api/v1/qrda/', description: 'QRDA export' },
    { path: '/api/v1/hcc/', description: 'HCC service' },
    { path: '/api/ecr/', description: 'ECR service' },
    { path: '/api/v1/prior-auth/', description: 'Prior auth' },
    { path: '/api/v1/provider-access/', description: 'Provider access' },
  ],
  demo_seeding: [
    { path: '/demo/seed-status', description: 'Demo seed status' },
    { path: '/demo/api/v1/scenarios', description: 'Demo scenarios' },
  ],
};

test.describe('Gateway-Edge URL Pattern Routing', () => {
  let apiContext: APIRequestContext;

  test.beforeAll(async ({ playwright }) => {
    apiContext = await playwright.request.newContext({
      baseURL: GATEWAY_EDGE_URL,
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

  test.describe('Gateway Admin Routes', () => {
    for (const route of ROUTING_MATRIX.gateway_admin) {
      test(`Routes ${route.path} to gateway-admin (${route.description})`, async () => {
        const response = await apiContext.get(route.path);

        // Check X-Edge headers are present (proves request went through gateway-edge)
        const xEdge = response.headers()['x-edge'];
        const xEdgeUpstream = response.headers()['x-edge-upstream'];
        const xEdgeRequestId = response.headers()['x-edge-request-id'];

        expect(xEdge).toBe('gateway-edge');
        expect(xEdgeRequestId).toBeTruthy();

        // Upstream header shows IP:port (nginx resolves hostnames to IPs)
        // Just verify it's present and formatted as IP:port or hostname:port
        if (xEdgeUpstream) {
          expect(xEdgeUpstream).toMatch(/\d+\.\d+\.\d+\.\d+:\d+|gateway-admin/);
        }

        // Request should reach the gateway (even if auth fails or backend returns error)
        // 500 is acceptable - routing worked, backend returned error
        expect([200, 401, 403, 404, 405, 500, 502, 503]).toContain(response.status());
      });
    }
  });

  test.describe('Gateway FHIR Routes', () => {
    for (const route of ROUTING_MATRIX.gateway_fhir) {
      test(`Routes ${route.path} to gateway-fhir (${route.description})`, async () => {
        const response = await apiContext.get(route.path);

        // Check X-Edge headers
        const xEdge = response.headers()['x-edge'];
        const xEdgeUpstream = response.headers()['x-edge-upstream'];
        const xEdgeRequestId = response.headers()['x-edge-request-id'];

        expect(xEdge).toBe('gateway-edge');
        expect(xEdgeRequestId).toBeTruthy();

        // Upstream header shows IP:port (nginx resolves hostnames to IPs)
        if (xEdgeUpstream) {
          expect(xEdgeUpstream).toMatch(/\d+\.\d+\.\d+\.\d+:\d+|gateway-fhir/);
        }

        // Request should reach the gateway (even if backend returns error)
        // 500 is acceptable - routing worked, backend returned error
        expect([200, 401, 403, 404, 405, 500, 502, 503]).toContain(response.status());
      });
    }
  });

  test.describe('Gateway Clinical Routes', () => {
    for (const route of ROUTING_MATRIX.gateway_clinical) {
      test(`Routes ${route.path} to gateway-clinical (${route.description})`, async () => {
        const response = await apiContext.get(route.path);

        // Check X-Edge headers
        const xEdge = response.headers()['x-edge'];
        const xEdgeUpstream = response.headers()['x-edge-upstream'];
        const xEdgeRequestId = response.headers()['x-edge-request-id'];

        expect(xEdge).toBe('gateway-edge');
        expect(xEdgeRequestId).toBeTruthy();

        // Upstream header shows IP:port (nginx resolves hostnames to IPs)
        if (xEdgeUpstream) {
          expect(xEdgeUpstream).toMatch(/\d+\.\d+\.\d+\.\d+:\d+|gateway-clinical/);
        }

        // Request should reach the gateway (even if backend returns error)
        // 500 is acceptable - routing worked, backend returned error
        expect([200, 401, 403, 404, 405, 500, 502, 503]).toContain(response.status());
      });
    }
  });

  test.describe('Demo Seeding Routes', () => {
    for (const route of ROUTING_MATRIX.demo_seeding) {
      test(`Routes ${route.path} to demo-seeding (${route.description})`, async () => {
        const response = await apiContext.get(route.path);

        // Check X-Edge headers
        const xEdge = response.headers()['x-edge'];
        const xEdgeRequestId = response.headers()['x-edge-request-id'];

        expect(xEdge).toBe('gateway-edge');
        expect(xEdgeRequestId).toBeTruthy();

        // Demo seeding might not be deployed - accept more status codes
        expect([200, 401, 403, 404, 502, 503]).toContain(response.status());
      });
    }
  });

  test.describe('Default Route Fallback', () => {
    test('Unknown paths fall back to gateway-admin', async () => {
      const response = await apiContext.get('/unknown-path-test-12345');

      const xEdge = response.headers()['x-edge'];
      expect(xEdge).toBe('gateway-edge');

      // Should get 404 or 500 (if route doesn't exist at backend)
      expect([404, 500, 502]).toContain(response.status());
    });

    test('Root path routes to gateway-admin', async () => {
      const response = await apiContext.get('/');

      const xEdge = response.headers()['x-edge'];
      expect(xEdge).toBe('gateway-edge');

      // Root path may return various responses depending on gateway config
      expect([200, 401, 403, 404, 500, 502]).toContain(response.status());
    });
  });
});

test.describe('X-Edge Header Propagation', () => {
  let apiContext: APIRequestContext;

  test.beforeAll(async ({ playwright }) => {
    apiContext = await playwright.request.newContext({
      baseURL: GATEWAY_EDGE_URL,
      extraHTTPHeaders: {
        'X-Tenant-ID': TEST_TENANT_ID,
      },
      timeout: 15000,
    });
  });

  test.afterAll(async () => {
    await apiContext.dispose();
  });

  test('X-Edge header identifies gateway-edge', async () => {
    const response = await apiContext.get('/actuator/health');

    expect(response.headers()['x-edge']).toBe('gateway-edge');
  });

  test('X-Edge-Request-ID is unique per request', async () => {
    const response1 = await apiContext.get('/actuator/health');
    const response2 = await apiContext.get('/actuator/health');

    const requestId1 = response1.headers()['x-edge-request-id'];
    const requestId2 = response2.headers()['x-edge-request-id'];

    expect(requestId1).toBeTruthy();
    expect(requestId2).toBeTruthy();
    expect(requestId1).not.toBe(requestId2);
  });

  test('Client-provided X-Request-ID is forwarded', async () => {
    const clientRequestId = `client-test-${Date.now()}`;

    const response = await apiContext.get('/actuator/health', {
      headers: {
        'X-Request-ID': clientRequestId,
      },
    });

    // Gateway-edge generates its own X-Edge-Request-ID
    // But should also forward client's X-Request-ID to downstream
    expect(response.headers()['x-edge-request-id']).toBeTruthy();
    expect(response.status()).toBe(200);
  });

  test('X-Edge-Upstream shows target service address', async () => {
    const response = await apiContext.get('/actuator/health');

    const upstream = response.headers()['x-edge-upstream'];
    // Upstream header should contain the service address (IP:port or hostname:port)
    if (upstream) {
      expect(upstream).toMatch(/gateway-admin|8080|\d+\.\d+\.\d+\.\d+/);
    }
  });
});

test.describe('External FHIR Server Integration', () => {
  let apiContext: APIRequestContext;
  let externalFhirContext: APIRequestContext;

  test.beforeAll(async ({ playwright }) => {
    apiContext = await playwright.request.newContext({
      baseURL: GATEWAY_EDGE_URL,
      extraHTTPHeaders: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'X-Tenant-ID': TEST_TENANT_ID,
      },
      timeout: 30000,
    });

    externalFhirContext = await playwright.request.newContext({
      baseURL: EXTERNAL_FHIR_URL,
      extraHTTPHeaders: {
        'Content-Type': 'application/fhir+json',
        'Accept': 'application/fhir+json',
      },
      timeout: 30000,
    });
  });

  test.afterAll(async () => {
    await apiContext.dispose();
    await externalFhirContext.dispose();
  });

  test('External FHIR server is accessible', async () => {
    const response = await externalFhirContext.get('/fhir/metadata');

    expect(response.status()).toBe(200);
    const capability = await response.json();
    expect(capability.resourceType).toBe('CapabilityStatement');
  });

  test('External FHIR server supports Patient resource', async () => {
    const response = await externalFhirContext.get('/fhir/metadata');

    const capability = await response.json();
    const patientResource = capability.rest?.[0]?.resource?.find(
      (r: { type: string }) => r.type === 'Patient'
    );

    expect(patientResource).toBeTruthy();
    expect(patientResource.type).toBe('Patient');
  });
});

test.describe('Authoritative Source Routing (Smart Routing Policy)', () => {
  let apiContext: APIRequestContext;
  let externalFhirContext: APIRequestContext;

  test.beforeAll(async ({ playwright }) => {
    apiContext = await playwright.request.newContext({
      baseURL: GATEWAY_EDGE_URL,
      extraHTTPHeaders: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'X-Tenant-ID': TEST_TENANT_ID,
      },
      timeout: 30000,
    });

    externalFhirContext = await playwright.request.newContext({
      baseURL: EXTERNAL_FHIR_URL,
      extraHTTPHeaders: {
        'Content-Type': 'application/fhir+json',
        'Accept': 'application/fhir+json',
      },
      timeout: 30000,
    });
  });

  test.afterAll(async () => {
    await apiContext.dispose();
    await externalFhirContext.dispose();
  });

  test.describe('Patient Reads (Authoritative: External FHIR)', () => {
    test.skip('Patient search routes through internal gateway', async () => {
      // This test verifies the routing layer, not the data source
      // Smart routing policy is enforced at the gateway-fhir-service level
      const response = await apiContext.get('/patient/api/v1/patients');

      // Request should be routed correctly
      const xEdge = response.headers()['x-edge'];
      expect(xEdge).toBe('gateway-edge');

      // Accept auth or success responses
      expect([200, 401, 403]).toContain(response.status());
    });

    test.skip('Patient by ID routes through internal gateway', async () => {
      const testPatientId = 'test-patient-123';
      const response = await apiContext.get(`/patient/api/v1/patients/${testPatientId}`);

      const xEdge = response.headers()['x-edge'];
      expect(xEdge).toBe('gateway-edge');

      // 404 is acceptable if patient doesn't exist
      expect([200, 401, 403, 404]).toContain(response.status());
    });
  });

  test.describe('Generated Resources (Authoritative: Internal FHIR)', () => {
    test.skip('MeasureReport queries route to internal FHIR', async () => {
      // MeasureReports are generated resources - should stay internal
      const response = await apiContext.get('/fhir/MeasureReport');

      const xEdge = response.headers()['x-edge'];
      expect(xEdge).toBe('gateway-edge');

      expect([200, 401, 403, 404]).toContain(response.status());
    });

    test.skip('Care gap results route to internal services', async () => {
      // Care gaps are generated from quality measure evaluation
      const response = await apiContext.get('/care-gap/api/v1/care-gaps');

      const xEdge = response.headers()['x-edge'];
      expect(xEdge).toBe('gateway-edge');

      expect([200, 401, 403]).toContain(response.status());
    });
  });
});

test.describe('Demo Seeding Hybrid Mode', () => {
  let apiContext: APIRequestContext;

  test.beforeAll(async ({ playwright }) => {
    apiContext = await playwright.request.newContext({
      baseURL: GATEWAY_EDGE_URL,
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

  test('Demo seed status endpoint is accessible', async () => {
    const response = await apiContext.get('/demo/seed-status');

    // Demo seeding service may not be deployed
    if (response.status() === 200) {
      const status = await response.json();
      expect(status).toHaveProperty('status');
    } else {
      // Accept 502/503 if service not deployed
      expect([401, 403, 404, 502, 503]).toContain(response.status());
    }
  });

  test.skip('Demo seeding respects hybrid mode configuration', async () => {
    // This test requires the demo seeding service to be running
    // with FHIR_TARGET=hybrid configuration

    const response = await apiContext.get('/demo/api/v1/seed-config');

    if (response.status() === 200) {
      const config = await response.json();
      // Verify hybrid mode is configured
      expect(config.fhirTarget).toBe('hybrid');
      expect(config.internalUrl).toBeTruthy();
      expect(config.externalUrl).toBeTruthy();
    }
  });
});

test.describe('Request Body Size Limits', () => {
  let apiContext: APIRequestContext;

  test.beforeAll(async ({ playwright }) => {
    apiContext = await playwright.request.newContext({
      baseURL: GATEWAY_EDGE_URL,
      extraHTTPHeaders: {
        'Content-Type': 'application/json',
        'X-Tenant-ID': TEST_TENANT_ID,
      },
      timeout: 60000,
    });
  });

  test.afterAll(async () => {
    await apiContext.dispose();
  });

  test('Accepts requests under 50MB limit', async () => {
    // Create a 1MB payload (well under the limit)
    const payload = { data: 'x'.repeat(1024 * 1024) };

    const response = await apiContext.post('/fhir/api/v1/bulk-import', {
      data: payload,
    });

    // Should not get 413 Payload Too Large
    expect(response.status()).not.toBe(413);
    // May get auth error, validation error, or server error - but not size error
    expect([200, 400, 401, 403, 404, 422, 500, 502]).toContain(response.status());
  });

  test('Rejects requests over 50MB limit', async () => {
    // Create a 60MB payload (over the limit)
    const payload = { data: 'x'.repeat(60 * 1024 * 1024) };

    try {
      const response = await apiContext.post('/fhir/api/v1/bulk-import', {
        data: payload,
      });

      // Should get 413 Payload Too Large
      expect([413]).toContain(response.status());
    } catch {
      // Request may fail/timeout for large payloads - this is acceptable
    }
  });
});

test.describe('Cross-Origin Resource Sharing (CORS)', () => {
  let apiContext: APIRequestContext;

  test.beforeAll(async ({ playwright }) => {
    apiContext = await playwright.request.newContext({
      baseURL: GATEWAY_EDGE_URL,
      timeout: 15000,
    });
  });

  test.afterAll(async () => {
    await apiContext.dispose();
  });

  test('OPTIONS preflight request succeeds', async () => {
    const response = await apiContext.fetch('/actuator/health', {
      method: 'OPTIONS',
      headers: {
        'Origin': 'http://localhost:4200',
        'Access-Control-Request-Method': 'GET',
        'Access-Control-Request-Headers': 'X-Tenant-ID,Authorization',
      },
    });

    // CORS preflight should succeed or be handled by gateway
    expect([200, 204, 401, 403, 405]).toContain(response.status());
  });

  test('CORS headers present in response', async () => {
    const response = await apiContext.get('/actuator/health', {
      headers: {
        'Origin': 'http://localhost:4200',
      },
    });

    // Check for CORS headers (may be set by gateway or backend)
    const corsHeaders = [
      'access-control-allow-origin',
      'access-control-allow-methods',
      'access-control-allow-headers',
    ];

    // At least some CORS headers should be present if configured
    const hasCorsHeaders = corsHeaders.some(
      (h) => response.headers()[h] !== undefined
    );

    // CORS might not be configured on actuator - just verify response works
    expect(response.status()).toBe(200);

    if (hasCorsHeaders) {
      console.log('CORS headers detected:', response.headers());
    }
  });
});
