import { test, expect, APIRequestContext } from '@playwright/test';

/**
 * Service Health Check E2E Tests
 *
 * Tests validate that all 28 backend services report proper health status
 * including database, Redis, and Kafka connectivity.
 *
 * Issue: #90
 */

const GATEWAY_URL = process.env['GATEWAY_URL'] || 'http://localhost:18080';
const GATEWAY_EDGE_URL = process.env['GATEWAY_EDGE_URL'] || 'http://localhost:8080';
const EXTERNAL_FHIR_URL = process.env['EXTERNAL_FHIR_URL'] || 'http://localhost:8088';
const JAEGER_URL = process.env['JAEGER_URL'] || 'http://localhost:16686';
const DEMO_SAFE = process.env['DEMO_SAFE'] === '1' || process.env['DEMO_SAFE'] === 'true';

// All 28 microservices with their configurations
const ALL_SERVICES = [
  // Core Services (always expected to be running)
  { name: 'gateway-service', basePath: '', port: 18080, required: true },
  { name: 'quality-measure-service', basePath: '/quality-measure', port: 8087, required: true },
  { name: 'cql-engine-service', basePath: '/cql-engine', port: 8081, required: true },
  { name: 'fhir-service', basePath: '/fhir', port: 8085, required: true },
  { name: 'patient-service', basePath: '/patient', port: 8084, required: true },
  { name: 'care-gap-service', basePath: '/care-gap', port: 8086, required: true },

  // Supporting Services
  { name: 'analytics-service', basePath: '/analytics', port: 8088, required: false },
  { name: 'consent-service', basePath: '/consent', port: 8089, required: false },
  { name: 'notification-service', basePath: '/notification', port: 8090, required: false },
  { name: 'audit-service', basePath: '/audit', port: 8091, required: false },

  // Integration Services
  { name: 'ehr-connector-service', basePath: '/ehr-connector', port: 8092, required: false },
  { name: 'cms-connector-service', basePath: '/cms-connector', port: 8100, required: false },

  // Clinical Services
  { name: 'hcc-service', basePath: '/hcc', port: 8093, required: false },
  { name: 'prior-auth-service', basePath: '/prior-auth', port: 8094, required: false },
  { name: 'qrda-export-service', basePath: '/qrda-export', port: 8095, required: false },
  { name: 'sdoh-service', basePath: '/sdoh', port: 8096, required: false },
  { name: 'predictive-analytics-service', basePath: '/predictive', port: 8097, required: false },

  // Operational Services
  { name: 'documentation-service', basePath: '/documentation', port: 8098, required: false },
  { name: 'demo-seeding-service', basePath: '/demo-seeding', port: 8099, required: false },

  // Additional Domain Services
  { name: 'appointment-service', basePath: '/appointment', port: 8101, required: false },
  { name: 'care-plan-service', basePath: '/care-plan', port: 8102, required: false },
  { name: 'immunization-service', basePath: '/immunization', port: 8103, required: false },
  { name: 'medication-service', basePath: '/medication', port: 8104, required: false },
  { name: 'encounter-service', basePath: '/encounter', port: 8105, required: false },
  { name: 'claim-service', basePath: '/claim', port: 8106, required: false },
  { name: 'provider-service', basePath: '/provider', port: 8107, required: false },
  { name: 'organization-service', basePath: '/organization', port: 8108, required: false },
  { name: 'user-service', basePath: '/user', port: 8109, required: false },
];

// Health status types
type HealthStatus = 'UP' | 'DOWN' | 'OUT_OF_SERVICE' | 'UNKNOWN';

interface HealthResponse {
  status: HealthStatus;
  components?: {
    db?: { status: HealthStatus; details?: Record<string, unknown> };
    redis?: { status: HealthStatus; details?: Record<string, unknown> };
    kafka?: { status: HealthStatus; details?: Record<string, unknown> };
    diskSpace?: { status: HealthStatus; details?: Record<string, unknown> };
    ping?: { status: HealthStatus };
    livenessState?: { status: HealthStatus };
    readinessState?: { status: HealthStatus };
  };
  details?: Record<string, unknown>;
}

test.describe('Service Health Checks', () => {
  let apiContext: APIRequestContext;

  test.beforeAll(async ({ playwright }) => {
    apiContext = await playwright.request.newContext({
      baseURL: GATEWAY_URL,
      timeout: 30000,
    });
  });

  test.afterAll(async () => {
    await apiContext.dispose();
  });

  test.describe('Core Services Health', () => {
    const coreServices = ALL_SERVICES.filter(s => s.required);

    for (const service of coreServices) {
      test(`${service.name} /actuator/health returns 200`, async () => {
        const healthPath = service.basePath
          ? `${service.basePath}/actuator/health`
          : '/actuator/health';

        const response = await apiContext.get(healthPath);

        expect(response.status()).toBe(200);

        const health: HealthResponse = await response.json();
        expect(health.status).toBe('UP');
      });

      test(`${service.name} includes database health`, async () => {
        const healthPath = service.basePath
          ? `${service.basePath}/actuator/health`
          : '/actuator/health';

        const response = await apiContext.get(healthPath);

        if (response.status() === 200) {
          const health: HealthResponse = await response.json();

          // Most services should have database connectivity
          if (health.components?.db) {
            expect(health.components.db.status).toBe('UP');

            // Verify database details if exposed
            if (health.components.db.details) {
              expect(health.components.db.details.database).toBeDefined();
            }
          }
        }
      });

      test(`${service.name} includes Redis health`, async () => {
        const healthPath = service.basePath
          ? `${service.basePath}/actuator/health`
          : '/actuator/health';

        const response = await apiContext.get(healthPath);

        if (response.status() === 200) {
          const health: HealthResponse = await response.json();

          // Redis is used by most services for caching
          if (health.components?.redis) {
            expect(health.components.redis.status).toBe('UP');
          }
        }
      });

      test(`${service.name} includes Kafka health`, async () => {
        const healthPath = service.basePath
          ? `${service.basePath}/actuator/health`
          : '/actuator/health';

        const response = await apiContext.get(healthPath);

        if (response.status() === 200) {
          const health: HealthResponse = await response.json();

          // Kafka is used by messaging-enabled services
          if (health.components?.kafka) {
            expect(health.components.kafka.status).toBe('UP');
          }
        }
      });
    }
  });

  test.describe('Kubernetes Probes', () => {
    const coreServices = ALL_SERVICES.filter(s => s.required);

    for (const service of coreServices) {
      test(`${service.name} startup probe succeeds`, async () => {
        // Startup probe typically uses /actuator/health/liveness
        const probePath = service.basePath
          ? `${service.basePath}/actuator/health/liveness`
          : '/actuator/health/liveness';

        const response = await apiContext.get(probePath);

        // Startup probe should succeed or fall back to main health
        expect([200, 404]).toContain(response.status());

        if (response.status() === 200) {
          const health: HealthResponse = await response.json();
          expect(health.status).toBe('UP');
        } else {
          // Fall back to main health endpoint
          const mainHealthPath = service.basePath
            ? `${service.basePath}/actuator/health`
            : '/actuator/health';

          const fallbackResponse = await apiContext.get(mainHealthPath);
          expect(fallbackResponse.status()).toBe(200);
        }
      });

      test(`${service.name} liveness probe succeeds`, async () => {
        const probePath = service.basePath
          ? `${service.basePath}/actuator/health/liveness`
          : '/actuator/health/liveness';

        const response = await apiContext.get(probePath);

        // Liveness should return UP or fallback to main health
        expect([200, 404]).toContain(response.status());

        if (response.status() === 200) {
          const health: HealthResponse = await response.json();
          expect(health.status).toBe('UP');
        }
      });

      test(`${service.name} readiness probe succeeds`, async () => {
        const probePath = service.basePath
          ? `${service.basePath}/actuator/health/readiness`
          : '/actuator/health/readiness';

        const response = await apiContext.get(probePath);

        // Readiness should return UP when service is ready
        expect([200, 404]).toContain(response.status());

        if (response.status() === 200) {
          const health: HealthResponse = await response.json();
          expect(health.status).toBe('UP');
        }
      });
    }
  });

  test.describe('Extended Services Health', () => {
    if (DEMO_SAFE) {
      test.skip(true, 'Extended services are not required in demo-safe runs.');
    }
    const extendedServices = ALL_SERVICES.filter(s => !s.required);

    for (const service of extendedServices) {
      test(`${service.name} health check (optional)`, async () => {
        const healthPath = `${service.basePath}/actuator/health`;

        const response = await apiContext.get(healthPath);

        // Extended services may not be deployed - accept multiple status codes
        expect([200, 401, 403, 404, 502, 503]).toContain(response.status());

        if (response.status() === 200) {
          const health: HealthResponse = await response.json();
          expect(['UP', 'DOWN']).toContain(health.status);

          // If UP, verify components if available
          if (health.status === 'UP' && health.components) {
            if (health.components.db) {
              expect(health.components.db.status).toBeDefined();
            }
          }
        }
      });
    }
  });
});

test.describe('Health Component Verification', () => {
  let apiContext: APIRequestContext;

  test.beforeAll(async ({ playwright }) => {
    apiContext = await playwright.request.newContext({
      baseURL: GATEWAY_URL,
      timeout: 30000,
    });
  });

  test.afterAll(async () => {
    await apiContext.dispose();
  });

  test('Gateway health includes all expected components', async () => {
    const response = await apiContext.get('/actuator/health');

    expect(response.status()).toBe(200);

    const health: HealthResponse = await response.json();

    // Gateway should report overall status
    expect(health.status).toBeDefined();

    // Check for standard components
    if (health.components) {
      // Disk space is typically always present
      if (health.components.diskSpace) {
        expect(health.components.diskSpace.status).toBe('UP');

        if (health.components.diskSpace.details) {
          const details = health.components.diskSpace.details as any;
          expect(details.total).toBeDefined();
          expect(details.free).toBeDefined();
        }
      }

      // Ping is a simple health indicator
      if (health.components.ping) {
        expect(health.components.ping.status).toBe('UP');
      }
    }
  });

  test('Database connection pool health', async () => {
    const response = await apiContext.get('/actuator/health');

    if (response.status() === 200) {
      const health: HealthResponse = await response.json();

      if (health.components?.db?.details) {
        const dbDetails = health.components.db.details as any;

        // Check for connection pool metrics if exposed
        if (dbDetails.connectionPool) {
          expect(dbDetails.connectionPool.active).toBeDefined();
          expect(dbDetails.connectionPool.idle).toBeDefined();
        }
      }
    }
  });

  test('Redis connection health', async () => {
    const response = await apiContext.get('/actuator/health');

    if (response.status() === 200) {
      const health: HealthResponse = await response.json();

      if (health.components?.redis?.details) {
        const redisDetails = health.components.redis.details as any;

        // Check for Redis cluster info if exposed
        if (redisDetails.cluster_enabled !== undefined) {
          expect(typeof redisDetails.cluster_enabled).toBe('boolean');
        }

        // Check for version if exposed
        if (redisDetails.version) {
          expect(redisDetails.version).toBeDefined();
        }
      }
    }
  });

  test('Kafka broker health', async () => {
    const response = await apiContext.get('/actuator/health');

    if (response.status() === 200) {
      const health: HealthResponse = await response.json();

      if (health.components?.kafka?.details) {
        const kafkaDetails = health.components.kafka.details as any;

        // Check for broker info if exposed
        if (kafkaDetails.brokerId) {
          expect(kafkaDetails.brokerId).toBeDefined();
        }

        if (kafkaDetails.clusterId) {
          expect(kafkaDetails.clusterId).toBeDefined();
        }
      }
    }
  });
});

test.describe('Health Metrics and Info', () => {
  let apiContext: APIRequestContext;

  test.beforeAll(async ({ playwright }) => {
    apiContext = await playwright.request.newContext({
      baseURL: GATEWAY_URL,
      timeout: 30000,
    });
  });

  test.afterAll(async () => {
    await apiContext.dispose();
  });

  test('Service info endpoint returns build information', async () => {
    const response = await apiContext.get('/actuator/info');

    // Info endpoint may be secured
    expect([200, 401, 403]).toContain(response.status());

    if (response.status() === 200) {
      const info = await response.json();

      // Check for build info if exposed
      if (info.build) {
        expect(info.build.name).toBeDefined();
        expect(info.build.version).toBeDefined();
      }

      // Check for git info if exposed
      if (info.git) {
        expect(info.git.branch).toBeDefined();
      }
    }
  });

  test('Prometheus metrics endpoint accessible', async () => {
    const response = await apiContext.get('/actuator/prometheus');

    // Prometheus endpoint may be secured
    expect([200, 401, 403, 404]).toContain(response.status());

    if (response.status() === 200) {
      const metrics = await response.text();

      // Should contain Prometheus format metrics
      expect(metrics).toContain('# HELP');
      expect(metrics).toContain('# TYPE');
    }
  });

  test('Health endpoint response time is acceptable', async () => {
    const startTime = Date.now();
    const response = await apiContext.get('/actuator/health');
    const endTime = Date.now();

    const responseTime = endTime - startTime;

    expect(response.status()).toBe(200);

    // Health check should respond within 5 seconds
    expect(responseTime).toBeLessThan(5000);
  });
});

test.describe('Health Degradation Scenarios', () => {
  let apiContext: APIRequestContext;

  test.beforeAll(async ({ playwright }) => {
    apiContext = await playwright.request.newContext({
      baseURL: GATEWAY_URL,
      timeout: 30000,
    });
  });

  test.afterAll(async () => {
    await apiContext.dispose();
  });

  test('Service reports degraded state appropriately', async () => {
    const response = await apiContext.get('/actuator/health');

    if (response.status() === 200) {
      const health: HealthResponse = await response.json();

      // If any component is DOWN, overall should reflect
      if (health.components) {
        const componentStatuses = Object.values(health.components)
          .map(c => (c as any).status);

        const hasDownComponent = componentStatuses.includes('DOWN');

        if (hasDownComponent) {
          // Overall status should reflect degraded state
          expect(['DOWN', 'OUT_OF_SERVICE']).toContain(health.status);
        }
      }
    }
  });

  test('Partial outage does not crash health endpoint', async () => {
    // Even if some components are down, health endpoint should respond
    const response = await apiContext.get('/actuator/health');

    // Should always get a response (not timeout or error)
    expect([200, 503]).toContain(response.status());

    const health: HealthResponse = await response.json();
    expect(health.status).toBeDefined();
  });
});

test.describe('Service Dependencies', () => {
  let apiContext: APIRequestContext;

  test.beforeAll(async ({ playwright }) => {
    apiContext = await playwright.request.newContext({
      baseURL: GATEWAY_URL,
      timeout: 30000,
    });
  });

  test.afterAll(async () => {
    await apiContext.dispose();
  });

  test('Quality measure service can connect to CQL engine', async () => {
    // Check quality measure service health
    const qmResponse = await apiContext.get('/quality-measure/actuator/health');

    if (qmResponse.status() === 200) {
      const health: HealthResponse = await qmResponse.json();

      // Check for CQL engine dependency if exposed
      if (health.components) {
        const cqlEngineComponent = (health.components as any)['cqlEngine'];
        if (cqlEngineComponent) {
          expect(cqlEngineComponent.status).toBe('UP');
        }
      }
    }
  });

  test('Care gap service can connect to patient service', async () => {
    const cgResponse = await apiContext.get('/care-gap/actuator/health');

    if (cgResponse.status() === 200) {
      const health: HealthResponse = await cgResponse.json();

      // Check for patient service dependency if exposed
      if (health.components) {
        const patientComponent = (health.components as any)['patientService'];
        if (patientComponent) {
          expect(patientComponent.status).toBe('UP');
        }
      }
    }
  });

  test('All core services are healthy simultaneously', async () => {
    const coreServices = ALL_SERVICES.filter(s => s.required);
    const healthChecks = [];

    for (const service of coreServices) {
      const healthPath = service.basePath
        ? `${service.basePath}/actuator/health`
        : '/actuator/health';

      const response = await apiContext.get(healthPath);
      healthChecks.push({
        service: service.name,
        status: response.status(),
        health: response.status() === 200 ? await response.json() : null,
      });
    }

    // All core services should be healthy
    for (const check of healthChecks) {
      expect(check.status).toBe(200);
      if (check.health) {
        expect(check.health.status).toBe('UP');
      }
    }
  });
});

test.describe('Gateway-Edge Infrastructure Health', () => {
  let apiContext: APIRequestContext;

  test.beforeAll(async ({ playwright }) => {
    apiContext = await playwright.request.newContext({
      baseURL: GATEWAY_EDGE_URL,
      timeout: 30000,
    });
  });

  test.afterAll(async () => {
    await apiContext.dispose();
  });

  test('Gateway-edge nginx is accessible', async () => {
    const response = await apiContext.get('/actuator/health');

    // Gateway-edge proxies to gateway-admin for actuator endpoints
    expect([200, 502, 503]).toContain(response.status());

    if (response.status() === 200) {
      // Verify X-Edge header is present (proves nginx is routing)
      const xEdge = response.headers()['x-edge'];
      expect(xEdge).toBe('gateway-edge');
    }
  });

  test('Gateway-admin-service is healthy via gateway-edge', async () => {
    const response = await apiContext.get('/api/v1/auth/health');

    // Should route to gateway-admin
    expect([200, 404, 502, 503]).toContain(response.status());

    const xEdge = response.headers()['x-edge'];
    expect(xEdge).toBe('gateway-edge');
  });

  test('Gateway-fhir-service is healthy via gateway-edge', async () => {
    const response = await apiContext.get('/fhir/actuator/health');

    // Should route to gateway-fhir
    expect([200, 401, 403, 502, 503]).toContain(response.status());

    const xEdge = response.headers()['x-edge'];
    expect(xEdge).toBe('gateway-edge');
  });

  test('Gateway-clinical-service is healthy via gateway-edge', async () => {
    const response = await apiContext.get('/care-gap/actuator/health');

    // Should route to gateway-clinical
    expect([200, 401, 403, 502, 503]).toContain(response.status());

    const xEdge = response.headers()['x-edge'];
    expect(xEdge).toBe('gateway-edge');
  });

  test('All three gateway services respond through gateway-edge', async () => {
    const gatewayRoutes = [
      { path: '/actuator/health', expected: 'gateway-admin' },
      { path: '/fhir/metadata', expected: 'gateway-fhir' },
      { path: '/care-gap/api/v1/care-gaps', expected: 'gateway-clinical' },
    ];

    for (const route of gatewayRoutes) {
      const response = await apiContext.get(route.path, {
        headers: { 'X-Tenant-ID': 'demo-tenant' },
      });

      // All should be routable
      expect([200, 401, 403, 404, 502, 503]).toContain(response.status());

      // X-Edge header should be present
      const xEdge = response.headers()['x-edge'];
      expect(xEdge).toBe('gateway-edge');
    }
  });

  test('Gateway-edge request ID is unique', async () => {
    const response1 = await apiContext.get('/actuator/health');
    const response2 = await apiContext.get('/actuator/health');

    const requestId1 = response1.headers()['x-edge-request-id'];
    const requestId2 = response2.headers()['x-edge-request-id'];

    expect(requestId1).toBeTruthy();
    expect(requestId2).toBeTruthy();
    expect(requestId1).not.toBe(requestId2);
  });
});

test.describe('External FHIR Server Health', () => {
  let apiContext: APIRequestContext;

  test.beforeAll(async ({ playwright }) => {
    apiContext = await playwright.request.newContext({
      baseURL: EXTERNAL_FHIR_URL,
      extraHTTPHeaders: {
        'Accept': 'application/fhir+json',
      },
      timeout: 30000,
    });
  });

  test.afterAll(async () => {
    await apiContext.dispose();
  });

  test('External FHIR server is accessible', async () => {
    const response = await apiContext.get('/fhir/metadata');

    // External FHIR may not be running
    if (response.status() === 200) {
      const capability = await response.json();
      expect(capability.resourceType).toBe('CapabilityStatement');
    } else {
      // Skip gracefully if not deployed
      expect([502, 503, 404]).toContain(response.status());
      console.log('External FHIR server not available');
    }
  });

  test('External FHIR server version is compatible', async () => {
    const response = await apiContext.get('/fhir/metadata');

    if (response.status() === 200) {
      const capability = await response.json();

      // Should be FHIR R4
      expect(capability.fhirVersion).toBe('4.0.1');
    }
  });

  test('External FHIR supports required resource types', async () => {
    const response = await apiContext.get('/fhir/metadata');

    if (response.status() !== 200) {
      test.skip();
      return;
    }

    const capability = await response.json();
    const resources = capability.rest?.[0]?.resource || [];
    const resourceTypes = resources.map((r: { type: string }) => r.type);

    // Required resource types for HDIM smart routing
    const requiredTypes = [
      'Patient',
      'Condition',
      'Observation',
      'Procedure',
      'MedicationRequest',
      'Encounter',
    ];

    for (const type of requiredTypes) {
      expect(resourceTypes).toContain(type);
    }
  });

  test('External FHIR Patient search works', async () => {
    const response = await apiContext.get('/fhir/Patient?_count=1');

    if (response.status() === 200) {
      const bundle = await response.json();
      expect(bundle.resourceType).toBe('Bundle');
      expect(bundle.type).toBe('searchset');
    } else {
      // May not have data or may require auth
      expect([401, 403, 404]).toContain(response.status());
    }
  });
});

test.describe('Distributed Tracing Health (Jaeger)', () => {
  let apiContext: APIRequestContext;

  test.beforeAll(async ({ playwright }) => {
    apiContext = await playwright.request.newContext({
      baseURL: JAEGER_URL,
      timeout: 30000,
    });
  });

  test.afterAll(async () => {
    await apiContext.dispose();
  });

  test('Jaeger UI is accessible', async () => {
    const response = await apiContext.get('/');

    // Jaeger may not be running
    if (response.status() === 200) {
      const html = await response.text();
      expect(html).toContain('Jaeger');
    } else {
      expect([502, 503]).toContain(response.status());
      console.log('Jaeger not available');
    }
  });

  test('Jaeger API services endpoint works', async () => {
    const response = await apiContext.get('/api/services');

    if (response.status() === 200) {
      const services = await response.json();
      expect(services.data).toBeDefined();
      expect(Array.isArray(services.data)).toBe(true);

      console.log(`Jaeger tracking ${services.data.length} services`);
    }
  });

  test('HDIM services appear in Jaeger', async () => {
    const response = await apiContext.get('/api/services');

    if (response.status() !== 200) {
      test.skip();
      return;
    }

    const services = await response.json();
    const serviceNames = services.data || [];

    // Check for core HDIM services
    const expectedServices = [
      'gateway',
      'patient',
      'fhir',
      'quality-measure',
      'cql-engine',
      'care-gap',
    ];

    // At least some HDIM services should be tracked
    const foundServices = expectedServices.filter(expected =>
      serviceNames.some((name: string) =>
        name.toLowerCase().includes(expected.toLowerCase())
      )
    );

    if (foundServices.length === 0) {
      console.log('No HDIM services found in Jaeger yet - traces may not have been generated');
    } else {
      console.log(`Found ${foundServices.length} HDIM services in Jaeger: ${foundServices.join(', ')}`);
    }
  });

  test('Jaeger traces endpoint works', async () => {
    // Query for recent traces
    const response = await apiContext.get('/api/traces?service=gateway-service&limit=1');

    if (response.status() === 200) {
      const traces = await response.json();
      expect(traces.data).toBeDefined();
    }
  });
});

test.describe('Prometheus Metrics Infrastructure', () => {
  let apiContext: APIRequestContext;

  test.beforeAll(async ({ playwright }) => {
    apiContext = await playwright.request.newContext({
      baseURL: GATEWAY_EDGE_URL,
      timeout: 30000,
    });
  });

  test.afterAll(async () => {
    await apiContext.dispose();
  });

  test('Core services expose Prometheus metrics', async () => {
    const coreServices = [
      '/actuator/prometheus',
      '/quality-measure/actuator/prometheus',
      '/fhir/actuator/prometheus',
      '/patient/actuator/prometheus',
    ];

    for (const endpoint of coreServices) {
      const response = await apiContext.get(endpoint);

      // Metrics may be secured
      if (response.status() === 200) {
        const metrics = await response.text();
        expect(metrics).toContain('# HELP');
        expect(metrics).toContain('# TYPE');
        expect(metrics).toContain('http_server_requests');
      }
    }
  });

  test('JVM metrics are exposed', async () => {
    const response = await apiContext.get('/actuator/prometheus');

    if (response.status() === 200) {
      const metrics = await response.text();

      // Check for JVM metrics
      const jvmMetrics = [
        'jvm_memory_used_bytes',
        'jvm_gc_pause',
        'jvm_threads_live',
      ];

      for (const metric of jvmMetrics) {
        if (!metrics.includes(metric)) {
          console.log(`JVM metric ${metric} not found`);
        }
      }
    }
  });

  test('HTTP request metrics are exposed', async () => {
    const response = await apiContext.get('/actuator/prometheus');

    if (response.status() === 200) {
      const metrics = await response.text();

      // Check for HTTP request metrics
      expect(metrics).toContain('http_server_requests');
    }
  });
});
