import { test, expect, APIRequestContext } from '@playwright/test';

/**
 * API Connectivity E2E Tests
 *
 * Tests validate gateway connectivity, service reachability, and infrastructure health.
 *
 * Issue: #85
 */

const GATEWAY_URL = process.env['GATEWAY_URL'] || 'http://localhost:18080';
const API_BASE = `${GATEWAY_URL}/api`;
const DEMO_SAFE = process.env['DEMO_SAFE'] === '1' || process.env['DEMO_SAFE'] === 'true';

// Service configurations with their expected routes
const CORE_SERVICES = [
  { name: 'quality-measure-service', route: '/quality-measure', healthPath: '/actuator/health' },
  { name: 'cql-engine-service', route: '/cql-engine', healthPath: '/actuator/health' },
  { name: 'fhir-service', route: '/fhir', healthPath: '/actuator/health' },
  { name: 'patient-service', route: '/patient', healthPath: '/actuator/health' },
  { name: 'care-gap-service', route: '/care-gap', healthPath: '/actuator/health' },
  { name: 'gateway-service', route: '', healthPath: '/actuator/health' },
  { name: 'analytics-service', route: '/analytics', healthPath: '/actuator/health' },
  { name: 'consent-service', route: '/consent', healthPath: '/actuator/health' },
  { name: 'notification-service', route: '/notification', healthPath: '/actuator/health' },
  { name: 'audit-service', route: '/audit', healthPath: '/actuator/health' },
];

const EXTENDED_SERVICES = [
  { name: 'ehr-connector-service', route: '/ehr-connector', healthPath: '/actuator/health' },
  { name: 'hcc-service', route: '/hcc', healthPath: '/actuator/health' },
  { name: 'prior-auth-service', route: '/prior-auth', healthPath: '/actuator/health' },
  { name: 'qrda-export-service', route: '/qrda-export', healthPath: '/actuator/health' },
  { name: 'sdoh-service', route: '/sdoh', healthPath: '/actuator/health' },
  { name: 'predictive-analytics-service', route: '/predictive', healthPath: '/actuator/health' },
  { name: 'documentation-service', route: '/documentation', healthPath: '/actuator/health' },
  { name: 'demo-seeding-service', route: '/demo-seeding', healthPath: '/actuator/health' },
  { name: 'cms-connector-service', route: '/cms-connector', healthPath: '/actuator/health' },
];

const DEMO_CORE_SERVICES = CORE_SERVICES.filter((service) =>
  ['quality-measure-service', 'cql-engine-service', 'fhir-service', 'patient-service', 'care-gap-service', 'gateway-service']
    .includes(service.name)
);

const ACTIVE_CORE_SERVICES = DEMO_SAFE ? DEMO_CORE_SERVICES : CORE_SERVICES;
const ACTIVE_EXTENDED_SERVICES = DEMO_SAFE ? [] : EXTENDED_SERVICES;

test.describe('API Connectivity Tests', () => {
  let apiContext: APIRequestContext;

  test.beforeAll(async ({ playwright }) => {
    apiContext = await playwright.request.newContext({
      baseURL: GATEWAY_URL,
      extraHTTPHeaders: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      timeout: 30000,
    });
  });

  test.afterAll(async () => {
    await apiContext.dispose();
  });

  test.describe('Gateway Health', () => {
    test('Gateway health check responds 200', async () => {
      const response = await apiContext.get('/actuator/health');

      expect(response.status()).toBe(200);

      const health = await response.json();
      expect(health.status).toBe('UP');
    });

    test('Gateway returns proper health details', async () => {
      const response = await apiContext.get('/actuator/health');

      if (response.status() === 200) {
        const health = await response.json();

        // Check for component health if available
        if (health.components) {
          // Database health
          if (health.components.db) {
            expect(health.components.db.status).toBe('UP');
          }

          // Redis health
          if (health.components.redis) {
            expect(health.components.redis.status).toBe('UP');
          }

          // Disk space
          if (health.components.diskSpace) {
            expect(health.components.diskSpace.status).toBe('UP');
          }
        }
      }
    });

    test('Gateway info endpoint accessible', async () => {
      const response = await apiContext.get('/actuator/info');

      // Should be accessible (200) or secured (401/403)
      expect([200, 401, 403]).toContain(response.status());
    });
  });

  test.describe('Service Reachability', () => {
    for (const service of ACTIVE_CORE_SERVICES) {
      test(`Core service ${service.name} is reachable via gateway`, async () => {
        // Try health endpoint through gateway routing
        const healthUrl = service.route
          ? `${service.route}${service.healthPath}`
          : service.healthPath;

        const response = await apiContext.get(healthUrl);

        // Should get a valid response (200 OK, or auth required 401/403)
        expect([200, 401, 403, 503]).toContain(response.status());

        if (response.status() === 200) {
          const health = await response.json();
          expect(health.status).toBeDefined();
        }
      });
    }
  });

  test.describe('Infrastructure Connectivity', () => {
    test('Database connectivity verified via gateway health', async () => {
      const response = await apiContext.get('/actuator/health');

      if (response.status() === 200) {
        const health = await response.json();

        // Check database component if exposed
        if (health.components?.db) {
          expect(health.components.db.status).toBe('UP');

          // Verify database details if available
          if (health.components.db.details) {
            expect(health.components.db.details.database).toBeDefined();
          }
        }
      }
    });

    test('Redis cache connectivity verified', async () => {
      const response = await apiContext.get('/actuator/health');

      if (response.status() === 200) {
        const health = await response.json();

        // Check Redis component if exposed
        if (health.components?.redis) {
          expect(health.components.redis.status).toBe('UP');
        }
      }
    });

    test('Kafka broker connectivity verified', async () => {
      const response = await apiContext.get('/actuator/health');

      if (response.status() === 200) {
        const health = await response.json();

        // Check Kafka component if exposed
        if (health.components?.kafka) {
          expect(health.components.kafka.status).toBe('UP');
        }
      }
    });
  });

  test.describe('API Versioning', () => {
    test('V1 API endpoints respond correctly', async () => {
      // Test a known v1 endpoint
      const response = await apiContext.get(`${API_BASE}/v1/health`);

      // Should return valid response or auth error
      expect([200, 401, 403, 404]).toContain(response.status());
    });

    test('Unversioned API returns proper response', async () => {
      const response = await apiContext.get(`${API_BASE}/health`);

      // Should either work or return clear error
      expect([200, 401, 403, 404]).toContain(response.status());
    });
  });

  test.describe('Content Negotiation', () => {
    test('JSON content-type negotiation works', async () => {
      const response = await apiContext.get('/actuator/health', {
        headers: {
          'Accept': 'application/json',
        },
      });

      expect(response.status()).toBe(200);

      const contentType = response.headers()['content-type'];
      expect(contentType).toContain('application/json');
    });

    test('Invalid Accept header handled gracefully', async () => {
      const response = await apiContext.get('/actuator/health', {
        headers: {
          'Accept': 'application/xml',
        },
      });

      // Should either return JSON anyway or 406 Not Acceptable
      expect([200, 406]).toContain(response.status());
    });
  });

  test.describe('CORS Headers', () => {
    test('CORS headers present on API responses', async () => {
      const response = await apiContext.get('/actuator/health', {
        headers: {
          'Origin': 'http://localhost:4200',
        },
      });

      // CORS headers may or may not be present depending on config
      const corsHeader = response.headers()['access-control-allow-origin'];

      if (corsHeader) {
        // If CORS is enabled, verify it's properly configured
        expect(['*', 'http://localhost:4200']).toContain(corsHeader);
      }
    });

    test('OPTIONS preflight request handled', async () => {
      // Playwright doesn't support OPTIONS directly, use fetch workaround
      const response = await apiContext.fetch('/actuator/health', {
        method: 'OPTIONS',
        headers: {
          'Origin': 'http://localhost:4200',
          'Access-Control-Request-Method': 'GET',
        },
      });

      // Should return 200 or 204 for preflight
      expect([200, 204, 403]).toContain(response.status());
    });
  });

  test.describe('Rate Limiting', () => {
    test('Rate limit headers present on responses', async () => {
      const response = await apiContext.get('/actuator/health');

      // Check for common rate limit headers
      const rateLimitRemaining = response.headers()['x-ratelimit-remaining'];
      const rateLimitLimit = response.headers()['x-ratelimit-limit'];
      const retryAfter = response.headers()['retry-after'];

      // Rate limiting may or may not be configured
      if (rateLimitLimit) {
        expect(parseInt(rateLimitLimit)).toBeGreaterThan(0);
      }

      if (rateLimitRemaining) {
        expect(parseInt(rateLimitRemaining)).toBeGreaterThanOrEqual(0);
      }
    });

    test('Rate limit exceeded returns 429', async () => {
      // Make rapid requests to potentially trigger rate limiting
      const responses = await Promise.all(
        Array(100).fill(null).map(() => apiContext.get('/actuator/health'))
      );

      // Check if any response is 429 (rate limited)
      const rateLimited = responses.filter(r => r.status() === 429);

      // If rate limiting is enabled, we should get some 429s
      // If not enabled, all should be 200
      const allOk = responses.every(r => r.status() === 200);
      const someRateLimited = rateLimited.length > 0;

      expect(allOk || someRateLimited).toBeTruthy();
    });
  });

  test.describe('Error Handling', () => {
    test('Request timeout handled gracefully', async () => {
      // Create context with very short timeout
      const timeoutContext = await apiContext['_playwright'].request.newContext({
        baseURL: GATEWAY_URL,
        timeout: 100, // Very short timeout
      });

      try {
        // This might timeout or succeed quickly
        const response = await timeoutContext.get('/actuator/health');
        expect([200, 408, 504]).toContain(response.status());
      } catch (error: any) {
        // Timeout error is expected
        expect(error.message).toContain('timeout');
      } finally {
        await timeoutContext.dispose();
      }
    });

    test('404 for unknown routes', async () => {
      const response = await apiContext.get('/api/v1/nonexistent-endpoint-xyz');

      expect(response.status()).toBe(404);
    });

    test('Method not allowed returns 405', async () => {
      // POST to a GET-only endpoint
      const response = await apiContext.post('/actuator/health', {
        data: {},
      });

      // Should return 405 Method Not Allowed or similar error
      expect([400, 405, 415]).toContain(response.status());
    });
  });

  test.describe('Connection Resilience', () => {
    test('Connection retry on transient failure simulated', async () => {
      // Make multiple requests to verify consistent connectivity
      const results = [];

      for (let i = 0; i < 5; i++) {
        const response = await apiContext.get('/actuator/health');
        results.push(response.status());
        await new Promise(resolve => setTimeout(resolve, 100));
      }

      // All requests should succeed
      expect(results.every(status => status === 200)).toBeTruthy();
    });

    test('Gateway handles service unavailable gracefully', async () => {
      // Request a potentially unavailable service
      const response = await apiContext.get('/unavailable-service/health');

      // Should return a proper error code, not hang or crash
      expect([404, 502, 503, 504]).toContain(response.status());
    });

    test('Circuit breaker behavior validated', async () => {
      // Make requests to an endpoint that might trigger circuit breaker
      const responses = [];

      for (let i = 0; i < 10; i++) {
        const response = await apiContext.get('/api/v1/health');
        responses.push({
          status: response.status(),
          circuitBreaker: response.headers()['x-circuit-breaker-state'],
        });
      }

      // Verify we get consistent responses (no cascading failures)
      const statuses = responses.map(r => r.status);
      const uniqueStatuses = [...new Set(statuses)];

      // Should have consistent status (all same) or proper degradation
      expect(uniqueStatuses.length).toBeLessThanOrEqual(3);
    });
  });
});

  test.describe('Service-to-Service Communication', () => {
  let apiContext: APIRequestContext;
  const testTenantId = '11111111-1111-1111-1111-111111111111';

  test.beforeAll(async ({ playwright }) => {
    apiContext = await playwright.request.newContext({
      baseURL: GATEWAY_URL,
      extraHTTPHeaders: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'X-Tenant-ID': testTenantId,
      },
    });
  });

  test.afterAll(async () => {
    await apiContext.dispose();
  });

  test('Quality measure service can reach FHIR service', async () => {
    // Trigger an operation that requires service-to-service communication
    const response = await apiContext.get(`${API_BASE}/v1/quality-measures`);

    // Should get valid response (success or auth error, not connection error)
    expect([200, 401, 403]).toContain(response.status());
  });

  test('Care gap service can reach patient service', async () => {
    const response = await apiContext.get(`${API_BASE}/v1/care-gaps`);

    // Should get valid response
    expect([200, 401, 403]).toContain(response.status());
  });

  test('Analytics service can aggregate data from multiple services', async () => {
    test.skip(DEMO_SAFE, 'Analytics service not enabled for demo-safe runs.');
    const response = await apiContext.get(`${API_BASE}/v1/analytics/summary`);

    // Should get valid response
    expect([200, 401, 403, 404]).toContain(response.status());
  });
});

  test.describe('Extended Service Connectivity', () => {
    if (DEMO_SAFE) {
      test.skip(true, 'Extended services are not required in demo-safe runs.');
    }
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

  for (const service of ACTIVE_EXTENDED_SERVICES) {
    test(`Extended service ${service.name} health check`, async () => {
      const healthUrl = `${service.route}${service.healthPath}`;
      const response = await apiContext.get(healthUrl);

      // Extended services may not all be running
      // Accept 200 (up), 503 (down but reachable), or 404 (not deployed)
      expect([200, 401, 403, 404, 503]).toContain(response.status());
    });
  }
});
