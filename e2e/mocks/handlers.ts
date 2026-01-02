import { http, HttpResponse } from 'msw';

/**
 * MSW API Mock Handlers for HDIM E2E Tests
 *
 * These handlers provide mock responses for API endpoints when running
 * tests without a live backend. Useful for:
 * - Isolated frontend testing
 * - Specific error scenario testing
 * - Performance testing with controlled responses
 */

// Base API URL
const API_BASE = process.env.API_BASE_URL || 'http://localhost:8001';

// Test tenant
const TEST_TENANT = 'ACME001';

// Mock data
const mockPatients = [
  {
    id: 'PAT001',
    fhirId: 'fhir-pat-001',
    tenantId: TEST_TENANT,
    firstName: 'John',
    lastName: 'Smith',
    dateOfBirth: '1965-03-15',
    gender: 'male',
    mrn: 'MRN001',
    riskScore: 85,
    riskLevel: 'HIGH',
    rafScore: 1.45,
    status: 'ACTIVE',
  },
  {
    id: 'PAT002',
    fhirId: 'fhir-pat-002',
    tenantId: TEST_TENANT,
    firstName: 'Jane',
    lastName: 'Doe',
    dateOfBirth: '1972-08-22',
    gender: 'female',
    mrn: 'MRN002',
    riskScore: 45,
    riskLevel: 'MEDIUM',
    rafScore: 1.12,
    status: 'ACTIVE',
  },
  {
    id: 'PAT003',
    fhirId: 'fhir-pat-003',
    tenantId: TEST_TENANT,
    firstName: 'Robert',
    lastName: 'Johnson',
    dateOfBirth: '1958-11-30',
    gender: 'male',
    mrn: 'MRN003',
    riskScore: 92,
    riskLevel: 'HIGH',
    rafScore: 2.34,
    status: 'ACTIVE',
  },
];

const mockCareGaps = [
  {
    id: 'GAP001',
    patientId: 'PAT001',
    measureId: 'CMS122',
    measureName: 'Diabetes: Hemoglobin A1c Control',
    status: 'OPEN',
    priority: 'HIGH',
    dueDate: '2025-03-31',
    lastEvaluated: '2024-12-01',
  },
  {
    id: 'GAP002',
    patientId: 'PAT001',
    measureId: 'CMS165',
    measureName: 'Controlling High Blood Pressure',
    status: 'OPEN',
    priority: 'MEDIUM',
    dueDate: '2025-06-30',
    lastEvaluated: '2024-12-01',
  },
  {
    id: 'GAP003',
    patientId: 'PAT002',
    measureId: 'CMS125',
    measureName: 'Breast Cancer Screening',
    status: 'CLOSED',
    priority: 'LOW',
    closedDate: '2024-11-15',
    lastEvaluated: '2024-12-01',
  },
];

const mockMeasures = [
  {
    id: 'CMS122',
    name: 'Diabetes: Hemoglobin A1c Control',
    version: '2024',
    category: 'Diabetes',
    type: 'PROCESS',
    status: 'ACTIVE',
  },
  {
    id: 'CMS165',
    name: 'Controlling High Blood Pressure',
    version: '2024',
    category: 'Cardiovascular',
    type: 'OUTCOME',
    status: 'ACTIVE',
  },
  {
    id: 'CMS125',
    name: 'Breast Cancer Screening',
    version: '2024',
    category: 'Cancer Screening',
    type: 'PROCESS',
    status: 'ACTIVE',
  },
];

const mockUsers = [
  {
    id: 'USR001',
    username: 'test_admin',
    email: 'admin@test.com',
    firstName: 'Admin',
    lastName: 'User',
    role: 'ADMIN',
    status: 'ACTIVE',
    tenantId: TEST_TENANT,
  },
  {
    id: 'USR002',
    username: 'test_evaluator',
    email: 'evaluator@test.com',
    firstName: 'Evaluator',
    lastName: 'User',
    role: 'EVALUATOR',
    status: 'ACTIVE',
    tenantId: TEST_TENANT,
  },
];

export const handlers = [
  // Health check
  http.get(`${API_BASE}/api/health`, () => {
    return HttpResponse.json({
      status: 'UP',
      timestamp: new Date().toISOString(),
    });
  }),

  http.get(`${API_BASE}/health`, () => {
    return HttpResponse.json({
      status: 'UP',
      timestamp: new Date().toISOString(),
    });
  }),

  // Authentication
  http.post(`${API_BASE}/api/v1/auth/login`, async ({ request }) => {
    const body = await request.json() as { username: string; password: string };

    if (body.password === 'password123') {
      const user = mockUsers.find(u => u.username === body.username);
      if (user) {
        return HttpResponse.json({
          token: 'mock-jwt-token-' + user.username,
          refreshToken: 'mock-refresh-token',
          user: {
            id: user.id,
            username: user.username,
            email: user.email,
            roles: [user.role],
            tenantId: user.tenantId,
          },
          expiresIn: 3600,
        });
      }
    }

    return HttpResponse.json(
      { error: 'Invalid credentials' },
      { status: 401 }
    );
  }),

  http.post(`${API_BASE}/api/v1/auth/logout`, () => {
    return HttpResponse.json({ success: true });
  }),

  http.get(`${API_BASE}/api/v1/auth/me`, ({ request }) => {
    const authHeader = request.headers.get('Authorization');
    if (authHeader?.startsWith('Bearer mock-jwt-token-')) {
      const username = authHeader.replace('Bearer mock-jwt-token-', '');
      const user = mockUsers.find(u => u.username === username);
      if (user) {
        return HttpResponse.json(user);
      }
    }
    return HttpResponse.json({ error: 'Unauthorized' }, { status: 401 });
  }),

  // Patients
  http.get(`${API_BASE}/api/v1/patients`, ({ request }) => {
    const url = new URL(request.url);
    const search = url.searchParams.get('search')?.toLowerCase();
    const status = url.searchParams.get('status');
    const riskLevel = url.searchParams.get('riskLevel');
    const page = parseInt(url.searchParams.get('page') || '0');
    const size = parseInt(url.searchParams.get('size') || '10');

    let filtered = [...mockPatients];

    if (search) {
      filtered = filtered.filter(
        p =>
          p.firstName.toLowerCase().includes(search) ||
          p.lastName.toLowerCase().includes(search) ||
          p.mrn.toLowerCase().includes(search)
      );
    }

    if (status) {
      filtered = filtered.filter(p => p.status === status);
    }

    if (riskLevel) {
      filtered = filtered.filter(p => p.riskLevel === riskLevel);
    }

    return HttpResponse.json({
      content: filtered.slice(page * size, (page + 1) * size),
      totalElements: filtered.length,
      totalPages: Math.ceil(filtered.length / size),
      page,
      size,
    });
  }),

  http.get(`${API_BASE}/api/v1/patients/:id`, ({ params }) => {
    const patient = mockPatients.find(p => p.id === params.id);
    if (patient) {
      return HttpResponse.json(patient);
    }
    return HttpResponse.json({ error: 'Patient not found' }, { status: 404 });
  }),

  // Care Gaps
  http.get(`${API_BASE}/api/v1/care-gaps`, ({ request }) => {
    const url = new URL(request.url);
    const status = url.searchParams.get('status');
    const patientId = url.searchParams.get('patientId');
    const measureId = url.searchParams.get('measureId');

    let filtered = [...mockCareGaps];

    if (status) {
      filtered = filtered.filter(g => g.status === status);
    }

    if (patientId) {
      filtered = filtered.filter(g => g.patientId === patientId);
    }

    if (measureId) {
      filtered = filtered.filter(g => g.measureId === measureId);
    }

    return HttpResponse.json({
      content: filtered,
      totalElements: filtered.length,
    });
  }),

  http.get(`${API_BASE}/api/v1/care-gaps/:id`, ({ params }) => {
    const gap = mockCareGaps.find(g => g.id === params.id);
    if (gap) {
      return HttpResponse.json(gap);
    }
    return HttpResponse.json({ error: 'Care gap not found' }, { status: 404 });
  }),

  http.put(`${API_BASE}/api/v1/care-gaps/:id/close`, ({ params }) => {
    const gap = mockCareGaps.find(g => g.id === params.id);
    if (gap) {
      return HttpResponse.json({
        ...gap,
        status: 'CLOSED',
        closedDate: new Date().toISOString(),
      });
    }
    return HttpResponse.json({ error: 'Care gap not found' }, { status: 404 });
  }),

  // Quality Measures
  http.get(`${API_BASE}/api/v1/quality-measures`, () => {
    return HttpResponse.json({
      content: mockMeasures,
      totalElements: mockMeasures.length,
    });
  }),

  http.get(`${API_BASE}/api/v1/quality-measures/:id`, ({ params }) => {
    const measure = mockMeasures.find(m => m.id === params.id);
    if (measure) {
      return HttpResponse.json(measure);
    }
    return HttpResponse.json({ error: 'Measure not found' }, { status: 404 });
  }),

  // Evaluations
  http.post(`${API_BASE}/api/v1/evaluations`, async ({ request }) => {
    const body = await request.json() as { measureId: string; patientId: string };
    return HttpResponse.json({
      id: 'EVAL-' + Date.now(),
      measureId: body.measureId,
      patientId: body.patientId,
      status: 'COMPLETED',
      result: 'NUMERATOR',
      evaluatedAt: new Date().toISOString(),
    });
  }),

  http.get(`${API_BASE}/api/v1/evaluations`, () => {
    return HttpResponse.json({
      content: [
        {
          id: 'EVAL001',
          measureId: 'CMS122',
          patientId: 'PAT001',
          status: 'COMPLETED',
          result: 'DENOMINATOR',
          evaluatedAt: '2024-12-01T10:00:00Z',
        },
      ],
      totalElements: 1,
    });
  }),

  // Dashboard
  http.get(`${API_BASE}/api/v1/dashboard/summary`, () => {
    return HttpResponse.json({
      patientCount: mockPatients.length,
      careGapCount: mockCareGaps.filter(g => g.status === 'OPEN').length,
      measureCount: mockMeasures.length,
      complianceRate: 78.5,
      highRiskCount: mockPatients.filter(p => p.riskLevel === 'HIGH').length,
      averageRafScore: 1.64,
    });
  }),

  // Risk scores
  http.get(`${API_BASE}/api/v1/risk/summary`, () => {
    return HttpResponse.json({
      highRiskCount: 2,
      mediumRiskCount: 1,
      lowRiskCount: 0,
      averageRafScore: 1.64,
    });
  }),

  http.get(`${API_BASE}/api/v1/risk/patients`, () => {
    return HttpResponse.json({
      content: mockPatients.map(p => ({
        patientId: p.id,
        riskScore: p.riskScore,
        riskLevel: p.riskLevel,
        rafScore: p.rafScore,
        hccCodes: ['HCC18', 'HCC19'],
      })),
      totalElements: mockPatients.length,
    });
  }),

  // Users (Admin)
  http.get(`${API_BASE}/api/v1/admin/users`, () => {
    return HttpResponse.json({
      content: mockUsers,
      totalElements: mockUsers.length,
    });
  }),

  // Audit logs
  http.get(`${API_BASE}/api/v1/admin/audit`, () => {
    return HttpResponse.json({
      content: [
        {
          id: 'AUD001',
          userId: 'USR001',
          action: 'LOGIN',
          resource: 'AUTH',
          timestamp: new Date().toISOString(),
          ipAddress: '127.0.0.1',
        },
        {
          id: 'AUD002',
          userId: 'USR002',
          action: 'VIEW',
          resource: 'PATIENT',
          resourceId: 'PAT001',
          timestamp: new Date().toISOString(),
          ipAddress: '127.0.0.1',
        },
      ],
      totalElements: 2,
    });
  }),

  // Reports
  http.get(`${API_BASE}/api/v1/reports`, () => {
    return HttpResponse.json({
      content: [
        {
          id: 'RPT001',
          name: 'Q4 2024 Quality Report',
          type: 'QUALITY',
          status: 'COMPLETED',
          generatedAt: '2024-12-15T10:00:00Z',
        },
        {
          id: 'RPT002',
          name: 'December Care Gap Report',
          type: 'CARE_GAP',
          status: 'COMPLETED',
          generatedAt: '2024-12-20T14:30:00Z',
        },
      ],
      totalElements: 2,
    });
  }),

  http.post(`${API_BASE}/api/v1/reports`, async ({ request }) => {
    const body = await request.json() as { type: string };
    return HttpResponse.json({
      id: 'RPT-' + Date.now(),
      name: 'New Report',
      type: body.type,
      status: 'PENDING',
      generatedAt: new Date().toISOString(),
    });
  }),

  // Integrations
  http.get(`${API_BASE}/api/v1/integrations/connectors`, () => {
    return HttpResponse.json({
      content: [
        {
          id: 'CONN001',
          name: 'Epic FHIR',
          type: 'EPIC',
          status: 'CONNECTED',
          lastSync: '2024-12-28T08:00:00Z',
        },
        {
          id: 'CONN002',
          name: 'Cerner FHIR',
          type: 'CERNER',
          status: 'DISCONNECTED',
          lastSync: '2024-12-20T12:00:00Z',
        },
      ],
      totalElements: 2,
    });
  }),

  // Fallback for unhandled requests
  http.all('*', ({ request }) => {
    console.warn(`Unhandled ${request.method} request to ${request.url}`);
    return HttpResponse.json(
      { error: 'Not found' },
      { status: 404 }
    );
  }),
];

// Error scenario handlers
export const errorHandlers = {
  serverError: http.get(`${API_BASE}/api/v1/patients`, () => {
    return HttpResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    );
  }),

  timeout: http.get(`${API_BASE}/api/v1/patients`, async () => {
    await new Promise(resolve => setTimeout(resolve, 30000));
    return HttpResponse.json({ content: [] });
  }),

  unauthorized: http.get(`${API_BASE}/api/v1/patients`, () => {
    return HttpResponse.json(
      { error: 'Unauthorized' },
      { status: 401 }
    );
  }),

  forbidden: http.get(`${API_BASE}/api/v1/patients`, () => {
    return HttpResponse.json(
      { error: 'Access denied' },
      { status: 403 }
    );
  }),

  tenantIsolation: http.get(`${API_BASE}/api/v1/patients/:id`, ({ params }) => {
    // Simulate cross-tenant access denial
    return HttpResponse.json(
      { error: 'Patient not found in tenant' },
      { status: 404 }
    );
  }),
};
