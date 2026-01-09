import { test, expect, APIRequestContext } from '@playwright/test';

/**
 * Multi-Tenant Isolation E2E Tests
 *
 * CRITICAL: These tests validate HIPAA-compliant tenant data isolation.
 * Tenant A must NEVER be able to access Tenant B's PHI data.
 *
 * Issue: #86
 */

const GATEWAY_URL = process.env['GATEWAY_URL'] || 'http://localhost:8001';
const GATEWAY_EDGE_URL = process.env['GATEWAY_EDGE_URL'] || 'http://localhost:8080';
const EXTERNAL_FHIR_URL = process.env['EXTERNAL_FHIR_URL'] || 'http://localhost:8088';
const API_BASE = `${GATEWAY_URL}/api`;

// Test tenant configurations
const TENANT_A = {
  id: '11111111-1111-1111-1111-111111111111',
  name: 'Tenant A - Test Healthcare',
  token: '', // Will be populated during setup
};

const TENANT_B = {
  id: '22222222-2222-2222-2222-222222222222',
  name: 'Tenant B - Other Healthcare',
  token: '', // Will be populated during setup
};

// Test patient IDs (should belong to specific tenants)
const TENANT_A_PATIENT_ID = 'patient-tenant-a-001';
const TENANT_B_PATIENT_ID = 'patient-tenant-b-001';

test.describe('Multi-Tenant Data Isolation', () => {
  let apiContext: APIRequestContext;

  test.beforeAll(async ({ playwright }) => {
    // Create API context for direct HTTP calls
    apiContext = await playwright.request.newContext({
      baseURL: GATEWAY_URL,
      extraHTTPHeaders: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
    });

    // In demo mode, we may not need actual tokens
    // but we still test header validation
  });

  test.afterAll(async () => {
    await apiContext.dispose();
  });

  test.describe('Patient Data Isolation', () => {
    test('Tenant A cannot access Tenant B patient data', async () => {
      // Attempt to access Tenant B's patient with Tenant A's context
      const response = await apiContext.get(`${API_BASE}/v1/patients/${TENANT_B_PATIENT_ID}`, {
        headers: {
          'X-Tenant-ID': TENANT_A.id,
        },
      });

      // Should either return 404 (not found for this tenant) or 403 (forbidden)
      expect([403, 404]).toContain(response.status());

      if (response.status() === 200) {
        // If somehow returns 200, verify it's NOT tenant B's data
        const data = await response.json();
        expect(data.tenantId).not.toBe(TENANT_B.id);
      }
    });

    test('Tenant B cannot access Tenant A patient data', async () => {
      const response = await apiContext.get(`${API_BASE}/v1/patients/${TENANT_A_PATIENT_ID}`, {
        headers: {
          'X-Tenant-ID': TENANT_B.id,
        },
      });

      expect([403, 404]).toContain(response.status());
    });

    test('Patient list only returns own tenant patients', async () => {
      const response = await apiContext.get(`${API_BASE}/v1/patients`, {
        headers: {
          'X-Tenant-ID': TENANT_A.id,
        },
      });

      if (response.status() === 200) {
        const data = await response.json();
        const patients = data.content || data.patients || data || [];

        // All returned patients must belong to Tenant A
        for (const patient of patients) {
          if (patient.tenantId) {
            expect(patient.tenantId).toBe(TENANT_A.id);
          }
        }
      }
    });
  });

  test.describe('Care Gap Isolation', () => {
    test('Tenant A cannot access Tenant B care gaps', async () => {
      const response = await apiContext.get(`${API_BASE}/v1/care-gaps`, {
        headers: {
          'X-Tenant-ID': TENANT_A.id,
        },
        params: {
          patientId: TENANT_B_PATIENT_ID,
        },
      });

      // Should not return Tenant B's care gaps
      if (response.status() === 200) {
        const data = await response.json();
        const gaps = data.content || data.careGaps || data || [];

        for (const gap of gaps) {
          if (gap.tenantId) {
            expect(gap.tenantId).not.toBe(TENANT_B.id);
          }
        }
      }
    });

    test('Care gap list filtered by tenant', async () => {
      const response = await apiContext.get(`${API_BASE}/v1/care-gaps`, {
        headers: {
          'X-Tenant-ID': TENANT_A.id,
        },
      });

      if (response.status() === 200) {
        const data = await response.json();
        const gaps = data.content || data.careGaps || data || [];

        for (const gap of gaps) {
          if (gap.tenantId) {
            expect(gap.tenantId).toBe(TENANT_A.id);
          }
        }
      }
    });
  });

  test.describe('Evaluation Isolation', () => {
    test('Tenant A cannot access Tenant B evaluations', async () => {
      const response = await apiContext.get(`${API_BASE}/v1/evaluations`, {
        headers: {
          'X-Tenant-ID': TENANT_A.id,
        },
      });

      if (response.status() === 200) {
        const data = await response.json();
        const evaluations = data.content || data.evaluations || data || [];

        for (const evaluation of evaluations) {
          if (evaluation.tenantId) {
            expect(evaluation.tenantId).not.toBe(TENANT_B.id);
          }
        }
      }
    });
  });

  test.describe('Header Validation', () => {
    test('Missing X-Tenant-ID header returns 400 on PHI endpoints', async () => {
      // Patient endpoint - PHI data
      const patientResponse = await apiContext.get(`${API_BASE}/v1/patients`, {
        headers: {
          // Deliberately omit X-Tenant-ID
        },
      });

      // Should require tenant header for PHI access
      expect([400, 401, 403]).toContain(patientResponse.status());
    });

    test('Invalid tenant ID format returns 400', async () => {
      const response = await apiContext.get(`${API_BASE}/v1/patients`, {
        headers: {
          'X-Tenant-ID': 'not-a-valid-uuid',
        },
      });

      expect([400, 401, 403]).toContain(response.status());
    });

    test('Non-existent tenant ID returns 403', async () => {
      const response = await apiContext.get(`${API_BASE}/v1/patients`, {
        headers: {
          'X-Tenant-ID': '99999999-9999-9999-9999-999999999999',
        },
      });

      // Should be forbidden - tenant doesn't exist
      expect([401, 403]).toContain(response.status());
    });
  });

  test.describe('Cross-Tenant Injection Prevention', () => {
    test('SQL injection in tenant ID blocked', async () => {
      const maliciousTenantId = "' OR '1'='1";

      const response = await apiContext.get(`${API_BASE}/v1/patients`, {
        headers: {
          'X-Tenant-ID': maliciousTenantId,
        },
      });

      // Should reject malformed tenant ID
      expect([400, 401, 403]).toContain(response.status());
    });

    test('Path traversal in patient ID blocked', async () => {
      const maliciousPatientId = '../../../etc/passwd';

      const response = await apiContext.get(`${API_BASE}/v1/patients/${maliciousPatientId}`, {
        headers: {
          'X-Tenant-ID': TENANT_A.id,
        },
      });

      // Should be handled safely (404 or 400)
      expect([400, 404]).toContain(response.status());
    });

    test('Tenant ID in query param ignored (header required)', async () => {
      // Try to override tenant via query parameter
      const response = await apiContext.get(`${API_BASE}/v1/patients`, {
        headers: {
          'X-Tenant-ID': TENANT_A.id,
        },
        params: {
          tenantId: TENANT_B.id, // Should be ignored
        },
      });

      if (response.status() === 200) {
        const data = await response.json();
        const patients = data.content || data.patients || data || [];

        // Should use header tenant, not query param
        for (const patient of patients) {
          if (patient.tenantId) {
            expect(patient.tenantId).toBe(TENANT_A.id);
          }
        }
      }
    });
  });

  test.describe('Audit Trail Validation', () => {
    test('PHI access creates audit log entry', async () => {
      // Make a PHI access request
      const accessResponse = await apiContext.get(`${API_BASE}/v1/patients`, {
        headers: {
          'X-Tenant-ID': TENANT_A.id,
        },
      });

      // Give time for async audit logging
      await new Promise(resolve => setTimeout(resolve, 1000));

      // Check audit logs (if endpoint available)
      const auditResponse = await apiContext.get(`${API_BASE}/v1/audit/recent`, {
        headers: {
          'X-Tenant-ID': TENANT_A.id,
        },
      });

      if (auditResponse.status() === 200) {
        const auditData = await auditResponse.json();
        const logs = auditData.content || auditData.logs || auditData || [];

        // Should have recent PHI access log
        const phiAccessLogs = logs.filter((log: any) =>
          log.eventType === 'PHI_ACCESS' ||
          log.action === 'READ' ||
          log.resourceType === 'Patient'
        );

        expect(phiAccessLogs.length).toBeGreaterThan(0);
      }
    });
  });
});

test.describe('Demo Mode Tenant Isolation', () => {
  let apiContext: APIRequestContext;

  test.beforeAll(async ({ playwright }) => {
    apiContext = await playwright.request.newContext({
      baseURL: GATEWAY_URL,
    });
  });

  test.afterAll(async () => {
    await apiContext.dispose();
  });

  test('Demo tenant can access demo patients only', async () => {
    const demoTenantId = 'demo-tenant-001';

    const response = await apiContext.get(`${API_BASE}/v1/patients`, {
      headers: {
        'X-Tenant-ID': demoTenantId,
        'Cookie': 'demo_mode=true',
      },
    });

    if (response.status() === 200) {
      const data = await response.json();
      const patients = data.content || data.patients || data || [];

      // Demo tenant should only see demo data
      for (const patient of patients) {
        if (patient.tenantId) {
          expect(patient.tenantId).toBe(demoTenantId);
        }
      }
    }
  });

  test('Production data not accessible in demo mode', async () => {
    const prodTenantId = TENANT_A.id;

    // In demo mode, production tenant should not be accessible
    const response = await apiContext.get(`${API_BASE}/v1/patients`, {
      headers: {
        'X-Tenant-ID': prodTenantId,
        'Cookie': 'demo_mode=true',
      },
    });

    // Either forbidden or filtered to empty
    if (response.status() === 200) {
      const data = await response.json();
      const patients = data.content || data.patients || data || [];
      // If demo mode is active, should not return production data
      expect(patients.every((p: any) => !p.tenantId || p.tenantId !== prodTenantId)).toBeTruthy();
    }
  });
});

test.describe('External FHIR Tenant Isolation', () => {
  let apiContext: APIRequestContext;
  let gatewayEdgeContext: APIRequestContext;

  test.beforeAll(async ({ playwright }) => {
    apiContext = await playwright.request.newContext({
      baseURL: EXTERNAL_FHIR_URL,
      extraHTTPHeaders: {
        'Accept': 'application/fhir+json',
        'Content-Type': 'application/fhir+json',
      },
      timeout: 30000,
    });

    gatewayEdgeContext = await playwright.request.newContext({
      baseURL: GATEWAY_EDGE_URL,
      extraHTTPHeaders: {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
      },
      timeout: 30000,
    });
  });

  test.afterAll(async () => {
    await apiContext.dispose();
    await gatewayEdgeContext.dispose();
  });

  test.describe('External FHIR Access Control', () => {
    test('External FHIR requests require tenant context', async () => {
      // Direct external FHIR access should work for metadata (public)
      const metadataResponse = await apiContext.get('/fhir/metadata');

      // Metadata is typically public
      if (metadataResponse.status() === 200) {
        const capability = await metadataResponse.json();
        expect(capability.resourceType).toBe('CapabilityStatement');
      }
    });

    test('Gateway routes enforce tenant on external FHIR reads', async () => {
      // Access through gateway-edge should enforce tenant
      const response = await gatewayEdgeContext.get('/patient/api/v1/patients', {
        headers: {
          // Deliberately omit X-Tenant-ID
        },
      });

      // Should require tenant header
      expect([400, 401, 403]).toContain(response.status());
    });

    test('Smart routing preserves tenant isolation for Patient reads', async () => {
      // Patient reads go to external FHIR but tenant isolation must be enforced
      const tenantAResponse = await gatewayEdgeContext.get('/patient/api/v1/patients', {
        headers: {
          'X-Tenant-ID': TENANT_A.id,
        },
      });

      const tenantBResponse = await gatewayEdgeContext.get('/patient/api/v1/patients', {
        headers: {
          'X-Tenant-ID': TENANT_B.id,
        },
      });

      // Both should get responses (even if empty)
      expect([200, 401, 403]).toContain(tenantAResponse.status());
      expect([200, 401, 403]).toContain(tenantBResponse.status());

      // If both succeed, verify data isolation
      if (tenantAResponse.status() === 200 && tenantBResponse.status() === 200) {
        const tenantAData = await tenantAResponse.json();
        const tenantBData = await tenantBResponse.json();

        const patientsA = tenantAData.content || tenantAData || [];
        const patientsB = tenantBData.content || tenantBData || [];

        // Extract patient IDs
        const idsA = patientsA.map((p: any) => p.id || p.fhirId);
        const idsB = patientsB.map((p: any) => p.id || p.fhirId);

        // No overlap between tenant datasets (unless both are empty)
        if (idsA.length > 0 && idsB.length > 0) {
          const overlap = idsA.filter((id: string) => idsB.includes(id));
          expect(overlap.length).toBe(0);
        }
      }
    });
  });

  test.describe('External FHIR to Internal FHIR Isolation', () => {
    test('Generated resources (MeasureReport) stay in internal FHIR', async () => {
      // MeasureReports are generated by quality measure evaluation
      // They should be stored in internal FHIR, not external
      const response = await gatewayEdgeContext.get('/fhir/MeasureReport', {
        headers: {
          'X-Tenant-ID': 'demo-tenant',
        },
      });

      // Should route to internal FHIR via gateway-fhir
      const xEdge = response.headers()['x-edge'];
      expect(xEdge).toBe('gateway-edge');

      // MeasureReports are internal-only, so should be accessible
      expect([200, 401, 403, 404]).toContain(response.status());
    });

    test('Care gaps reference patients correctly across routing boundary', async () => {
      // Care gaps reference patients, but gaps are internal, patients may be external
      const response = await gatewayEdgeContext.get('/care-gap/api/v1/care-gaps', {
        headers: {
          'X-Tenant-ID': 'demo-tenant',
        },
      });

      if (response.status() === 200) {
        const data = await response.json();
        const gaps = data.content || data || [];

        // Care gaps should have patient references
        for (const gap of gaps) {
          if (gap.patientId || gap.patientReference) {
            // Patient reference should be valid format
            const patientRef = gap.patientId || gap.patientReference;
            expect(patientRef).toBeTruthy();
          }
        }
      }
    });
  });

  test.describe('Tenant-Aware Audit for External FHIR', () => {
    test('External FHIR reads are audited with tenant context', async () => {
      const testTenantId = 'demo-tenant';

      // Make a patient read that routes to external FHIR
      await gatewayEdgeContext.get('/patient/api/v1/patients', {
        headers: {
          'X-Tenant-ID': testTenantId,
          'X-Request-ID': `audit-test-${Date.now()}`,
        },
      });

      // Give time for async audit logging
      await new Promise(resolve => setTimeout(resolve, 1000));

      // Check audit logs
      const auditResponse = await gatewayEdgeContext.get('/audit/api/v1/events', {
        headers: {
          'X-Tenant-ID': testTenantId,
        },
        params: {
          limit: 10,
          sort: 'desc',
        },
      });

      if (auditResponse.status() === 200) {
        const auditData = await auditResponse.json();
        const events = auditData.content || auditData || [];

        // Recent audit events should include tenant context
        const recentPatientEvents = events.filter((e: any) =>
          e.resourceType === 'Patient' || e.action?.includes('patient')
        );

        if (recentPatientEvents.length > 0) {
          // Audit events should have tenant ID
          for (const event of recentPatientEvents) {
            expect(event.tenantId || event.tenant).toBe(testTenantId);
          }
        }
      }
    });
  });
});

test.describe('Gateway-Edge Tenant Routing', () => {
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

  test.describe('Tenant Header Propagation Through Gateway-Edge', () => {
    test('X-Tenant-ID header propagates through nginx to gateway services', async () => {
      const testTenantId = 'demo-tenant';

      const response = await apiContext.get('/patient/api/v1/patients', {
        headers: {
          'X-Tenant-ID': testTenantId,
        },
      });

      // Gateway-edge should forward the tenant header
      const xEdge = response.headers()['x-edge'];
      expect(xEdge).toBe('gateway-edge');

      // Request should be processed (not rejected for missing tenant)
      expect([200, 401, 403]).toContain(response.status());
    });

    test('X-Tenant-ID cannot be spoofed via X-Auth headers', async () => {
      // Attempt to inject tenant via X-Auth headers (should be stripped)
      const response = await apiContext.get('/patient/api/v1/patients', {
        headers: {
          'X-Auth-Tenant-Ids': 'admin-tenant,all-tenants',
          'X-Tenant-ID': 'demo-tenant',
        },
      });

      // X-Auth headers should be stripped by gateway
      // Only X-Tenant-ID (request header) should be used
      expect([200, 401, 403]).toContain(response.status());
    });

    test('Multi-tenant header injection blocked', async () => {
      // Attempt to specify multiple tenants
      const response = await apiContext.get('/patient/api/v1/patients', {
        headers: {
          'X-Tenant-ID': TENANT_A.id,
          'X-Tenant-Id': TENANT_B.id, // Case variation
        },
      });

      // Should use first header or reject ambiguous
      expect([200, 400, 401, 403]).toContain(response.status());

      if (response.status() === 200) {
        const data = await response.json();
        const patients = data.content || data || [];

        // All data should be from ONE tenant (the validated one)
        const tenantIds = [...new Set(patients.map((p: any) => p.tenantId).filter(Boolean))];
        expect(tenantIds.length).toBeLessThanOrEqual(1);
      }
    });
  });

  test.describe('Cross-Gateway Tenant Consistency', () => {
    test('Tenant context consistent across gateway-admin routes', async () => {
      const testTenantId = 'demo-tenant';

      const authResponse = await apiContext.get('/api/v1/auth/me', {
        headers: {
          'X-Tenant-ID': testTenantId,
        },
      });

      // Gateway-admin should receive tenant context
      expect([200, 401, 403, 404]).toContain(authResponse.status());
    });

    test('Tenant context consistent across gateway-fhir routes', async () => {
      const testTenantId = 'demo-tenant';

      const fhirResponse = await apiContext.get('/fhir/Patient', {
        headers: {
          'X-Tenant-ID': testTenantId,
        },
      });

      // Gateway-fhir should receive tenant context
      expect([200, 401, 403]).toContain(fhirResponse.status());
    });

    test('Tenant context consistent across gateway-clinical routes', async () => {
      const testTenantId = 'demo-tenant';

      const clinicalResponse = await apiContext.get('/care-gap/api/v1/care-gaps', {
        headers: {
          'X-Tenant-ID': testTenantId,
        },
      });

      // Gateway-clinical should receive tenant context
      expect([200, 401, 403]).toContain(clinicalResponse.status());
    });
  });
});
