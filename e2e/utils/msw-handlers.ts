/**
 * Mock Service Worker (MSW) Handlers
 *
 * Provides mock API handlers for offline testing and controlled test scenarios.
 * These handlers simulate backend responses without requiring actual services.
 *
 * Note: MSW needs to be integrated with the frontend build for full support.
 * This file provides the handler definitions for when MSW is configured.
 */

/**
 * API Response Types
 */
export interface MockPatient {
  id: string;
  fhirId: string;
  firstName: string;
  lastName: string;
  birthDate: string;
  gender: 'male' | 'female' | 'other';
  mrn: string;
}

export interface MockQualityMeasure {
  id: string;
  code: string;
  name: string;
  category: string;
  version: string;
  status: 'active' | 'draft' | 'retired';
}

export interface MockCareGap {
  id: string;
  patientId: string;
  measureCode: string;
  gapType: string;
  urgency: 'HIGH' | 'MEDIUM' | 'LOW';
  status: 'OPEN' | 'CLOSED' | 'IN_PROGRESS';
  identifiedDate: string;
  dueDate: string;
}

export interface MockEvaluationResult {
  id: string;
  patientId: string;
  measureCode: string;
  result: 'COMPLIANT' | 'NON_COMPLIANT' | 'NOT_ELIGIBLE';
  numerator: boolean;
  denominator: boolean;
  evaluationDate: string;
}

/**
 * Generate mock data
 */
function generateMockPatients(count: number): MockPatient[] {
  const firstNames = ['Test_Alice', 'Test_Bob', 'Test_Carol', 'Test_David', 'Test_Emma'];
  const lastNames = ['Synthetic', 'TestPatient', 'FakeData', 'DemoUser'];

  return Array.from({ length: count }, (_, i) => ({
    id: `PATIENT_${i + 1}`,
    fhirId: `FHIR_PATIENT_${i + 1}`,
    firstName: firstNames[i % firstNames.length],
    lastName: lastNames[i % lastNames.length],
    birthDate: `19${60 + (i % 40)}-${String((i % 12) + 1).padStart(2, '0')}-${String((i % 28) + 1).padStart(2, '0')}`,
    gender: i % 3 === 0 ? 'male' : i % 3 === 1 ? 'female' : 'other',
    mrn: `TEST_MRN_${10000 + i}`,
  }));
}

function generateMockMeasures(): MockQualityMeasure[] {
  return [
    { id: 'M1', code: 'CMS122v12', name: 'Diabetes: Hemoglobin A1c Poor Control', category: 'Chronic Disease', version: '2024', status: 'active' },
    { id: 'M2', code: 'CMS130v12', name: 'Colorectal Cancer Screening', category: 'Preventive', version: '2024', status: 'active' },
    { id: 'M3', code: 'CMS165v12', name: 'Controlling High Blood Pressure', category: 'Chronic Disease', version: '2024', status: 'active' },
    { id: 'M4', code: 'CMS138v12', name: 'Tobacco Use Screening and Cessation', category: 'Preventive', version: '2024', status: 'active' },
    { id: 'M5', code: 'CMS125v12', name: 'Breast Cancer Screening', category: 'Women\'s Health', version: '2024', status: 'active' },
  ];
}

function generateMockCareGaps(patientCount: number): MockCareGap[] {
  const gaps: MockCareGap[] = [];
  const measures = generateMockMeasures();
  const urgencies: ('HIGH' | 'MEDIUM' | 'LOW')[] = ['HIGH', 'MEDIUM', 'LOW'];

  for (let i = 0; i < patientCount; i++) {
    if (i % 3 !== 0) { // Not all patients have gaps
      gaps.push({
        id: `GAP_${i + 1}`,
        patientId: `PATIENT_${i + 1}`,
        measureCode: measures[i % measures.length].code,
        gapType: 'SCREENING',
        urgency: urgencies[i % 3],
        status: 'OPEN',
        identifiedDate: new Date(Date.now() - (i * 24 * 60 * 60 * 1000)).toISOString(),
        dueDate: new Date(Date.now() + (90 * 24 * 60 * 60 * 1000)).toISOString(),
      });
    }
  }

  return gaps;
}

/**
 * MSW Handler Definitions
 *
 * These are the REST handlers that MSW will use to mock API responses.
 * To use these, MSW must be installed and configured in the frontend.
 *
 * Installation:
 * npm install msw --save-dev
 *
 * Setup:
 * npx msw init public/ --save
 */
export const mswHandlerDefinitions = {
  // Authentication
  login: {
    method: 'POST',
    path: '/api/auth/login',
    handler: (body: { username: string; password: string }) => {
      // Validate credentials
      const validUsers = ['test_superadmin', 'test_admin', 'test_evaluator', 'test_analyst', 'test_viewer'];
      if (validUsers.includes(body.username) && body.password === 'password123') {
        return {
          status: 200,
          body: {
            token: 'mock_jwt_token_' + body.username,
            refreshToken: 'mock_refresh_token',
            expiresIn: 900,
            tokenType: 'Bearer',
          },
        };
      }
      return {
        status: 401,
        body: { error: 'Invalid credentials' },
      };
    },
  },

  // Patients
  getPatients: {
    method: 'GET',
    path: '/api/v1/patients',
    handler: (params: { search?: string; limit?: number }) => {
      let patients = generateMockPatients(50);

      if (params.search) {
        patients = patients.filter(p =>
          p.firstName.toLowerCase().includes(params.search!.toLowerCase()) ||
          p.lastName.toLowerCase().includes(params.search!.toLowerCase()) ||
          p.mrn.toLowerCase().includes(params.search!.toLowerCase())
        );
      }

      if (params.limit) {
        patients = patients.slice(0, params.limit);
      }

      return { status: 200, body: patients };
    },
  },

  getPatient: {
    method: 'GET',
    path: '/api/v1/patients/:id',
    handler: (params: { id: string }) => {
      const patients = generateMockPatients(50);
      const patient = patients.find(p => p.id === params.id);
      if (patient) {
        return { status: 200, body: patient };
      }
      return { status: 404, body: { error: 'Patient not found' } };
    },
  },

  // Quality Measures
  getMeasures: {
    method: 'GET',
    path: '/api/v1/quality-measures',
    handler: () => {
      return { status: 200, body: generateMockMeasures() };
    },
  },

  // Evaluations
  runEvaluation: {
    method: 'POST',
    path: '/api/v1/evaluations',
    handler: (body: { patientId: string; measureCode: string }) => {
      // Simulate evaluation result
      const results: ('COMPLIANT' | 'NON_COMPLIANT' | 'NOT_ELIGIBLE')[] = [
        'COMPLIANT', 'NON_COMPLIANT', 'NOT_ELIGIBLE'
      ];
      const result = results[Math.floor(Math.random() * 3)];

      return {
        status: 200,
        body: {
          id: `EVAL_${Date.now()}`,
          patientId: body.patientId,
          measureCode: body.measureCode,
          result,
          numerator: result === 'COMPLIANT',
          denominator: result !== 'NOT_ELIGIBLE',
          evaluationDate: new Date().toISOString(),
        },
      };
    },
  },

  // Care Gaps
  getCareGaps: {
    method: 'GET',
    path: '/api/v1/care-gaps',
    handler: (params: { status?: string; urgency?: string }) => {
      let gaps = generateMockCareGaps(50);

      if (params.status && params.status !== 'ALL') {
        gaps = gaps.filter(g => g.status === params.status);
      }
      if (params.urgency && params.urgency !== 'ALL') {
        gaps = gaps.filter(g => g.urgency === params.urgency);
      }

      return { status: 200, body: gaps };
    },
  },

  closeCareGap: {
    method: 'POST',
    path: '/api/v1/care-gaps/:id/close',
    handler: (params: { id: string }, body: { reason: string; notes?: string }) => {
      return {
        status: 200,
        body: {
          id: params.id,
          status: 'CLOSED',
          closedDate: new Date().toISOString(),
          closedReason: body.reason,
          closedNotes: body.notes,
        },
      };
    },
  },

  // Dashboard
  getDashboardMetrics: {
    method: 'GET',
    path: '/api/v1/dashboard/metrics',
    handler: () => {
      const patients = generateMockPatients(50);
      const gaps = generateMockCareGaps(50);

      return {
        status: 200,
        body: {
          patientCount: patients.length,
          careGapsCount: gaps.filter(g => g.status === 'OPEN').length,
          evaluationsCount: 150,
          complianceRate: 72.5,
        },
      };
    },
  },

  // Health Check
  healthCheck: {
    method: 'GET',
    path: '/actuator/health',
    handler: () => {
      return {
        status: 200,
        body: { status: 'UP' },
      };
    },
  },
};

/**
 * Playwright Route Handler Factory
 *
 * Creates Playwright route handlers from MSW definitions.
 * Use this when MSW is not available but you need to mock API responses.
 */
export function createPlaywrightMockRoutes(page: any): void {
  const handlers = mswHandlerDefinitions;

  // Auth
  page.route('**/api/auth/login', async (route: any) => {
    const body = route.request().postDataJSON();
    const result = handlers.login.handler(body);
    await route.fulfill({
      status: result.status,
      contentType: 'application/json',
      body: JSON.stringify(result.body),
    });
  });

  // Patients
  page.route('**/api/v1/patients', async (route: any) => {
    if (route.request().method() === 'GET') {
      const url = new URL(route.request().url());
      const result = handlers.getPatients.handler({
        search: url.searchParams.get('search') || undefined,
        limit: url.searchParams.get('limit') ? parseInt(url.searchParams.get('limit')!) : undefined,
      });
      await route.fulfill({
        status: result.status,
        contentType: 'application/json',
        body: JSON.stringify(result.body),
      });
    }
  });

  // Quality Measures
  page.route('**/api/v1/quality-measures', async (route: any) => {
    const result = handlers.getMeasures.handler();
    await route.fulfill({
      status: result.status,
      contentType: 'application/json',
      body: JSON.stringify(result.body),
    });
  });

  // Evaluations
  page.route('**/api/v1/evaluations', async (route: any) => {
    if (route.request().method() === 'POST') {
      const body = route.request().postDataJSON();
      const result = handlers.runEvaluation.handler(body);
      await route.fulfill({
        status: result.status,
        contentType: 'application/json',
        body: JSON.stringify(result.body),
      });
    }
  });

  // Care Gaps
  page.route('**/api/v1/care-gaps', async (route: any) => {
    if (route.request().method() === 'GET') {
      const url = new URL(route.request().url());
      const result = handlers.getCareGaps.handler({
        status: url.searchParams.get('status') || undefined,
        urgency: url.searchParams.get('urgency') || undefined,
      });
      await route.fulfill({
        status: result.status,
        contentType: 'application/json',
        body: JSON.stringify(result.body),
      });
    }
  });

  // Dashboard
  page.route('**/api/v1/dashboard/metrics', async (route: any) => {
    const result = handlers.getDashboardMetrics.handler();
    await route.fulfill({
      status: result.status,
      contentType: 'application/json',
      body: JSON.stringify(result.body),
    });
  });

  // Health Check
  page.route('**/actuator/health', async (route: any) => {
    const result = handlers.healthCheck.handler();
    await route.fulfill({
      status: result.status,
      contentType: 'application/json',
      body: JSON.stringify(result.body),
    });
  });
}

/**
 * Offline Test Configuration
 *
 * Configuration for running tests with mocked backend.
 */
export const offlineTestConfig = {
  // Use mocked responses
  useMocks: true,

  // Delay to simulate network latency
  latencyMs: 100,

  // Probability of simulated failure (for chaos testing)
  failureRate: 0,

  // Enable verbose logging of mocked requests
  logRequests: false,
};

/**
 * WebSocket Mock Server for E2E Testing
 *
 * Simulates WebSocket events for testing real-time features
 * without a live backend connection.
 */
export class WebSocketMockServer {
  private events: any[] = [];
  private subscribers: ((event: any) => void)[] = [];
  private autoPlay = false;
  private playbackInterval: ReturnType<typeof setInterval> | null = null;

  /**
   * Queue an event to be sent
   */
  queueEvent(event: any): void {
    this.events.push(event);
  }

  /**
   * Queue multiple events
   */
  queueEvents(events: any[]): void {
    this.events.push(...events);
  }

  /**
   * Subscribe to receive events
   */
  subscribe(callback: (event: any) => void): () => void {
    this.subscribers.push(callback);
    return () => {
      this.subscribers = this.subscribers.filter(s => s !== callback);
    };
  }

  /**
   * Send next queued event to all subscribers
   */
  sendNext(): any | undefined {
    const event = this.events.shift();
    if (event) {
      this.subscribers.forEach(cb => cb(event));
    }
    return event;
  }

  /**
   * Send all queued events
   */
  sendAll(): void {
    while (this.events.length > 0) {
      this.sendNext();
    }
  }

  /**
   * Start auto-playing events at interval
   */
  startAutoPlay(intervalMs: number = 1000): void {
    this.autoPlay = true;
    this.playbackInterval = setInterval(() => {
      if (this.events.length > 0) {
        this.sendNext();
      } else {
        this.stopAutoPlay();
      }
    }, intervalMs);
  }

  /**
   * Stop auto-playing events
   */
  stopAutoPlay(): void {
    this.autoPlay = false;
    if (this.playbackInterval) {
      clearInterval(this.playbackInterval);
      this.playbackInterval = null;
    }
  }

  /**
   * Clear all queued events
   */
  clear(): void {
    this.events = [];
  }

  /**
   * Get count of queued events
   */
  get queuedCount(): number {
    return this.events.length;
  }
}

/**
 * Create a WebSocket mock that integrates with Playwright page
 */
export function createPlaywrightWebSocketMock(page: any): WebSocketMockServer {
  const mockServer = new WebSocketMockServer();

  // Inject mock WebSocket into page
  page.addInitScript(() => {
    (window as any).__mockWebSocket = {
      listeners: [] as ((event: any) => void)[],
      emit: (event: any) => {
        (window as any).__mockWebSocket.listeners.forEach((cb: any) => cb(event));
      },
    };

    // Intercept WebSocket connections
    const OriginalWebSocket = window.WebSocket;
    (window as any).WebSocket = class MockWebSocket {
      onopen: (() => void) | null = null;
      onmessage: ((event: MessageEvent) => void) | null = null;
      onerror: ((error: Event) => void) | null = null;
      onclose: (() => void) | null = null;
      readyState = 1; // OPEN

      constructor(url: string) {
        console.log('[MockWebSocket] Connection to:', url);

        // Simulate connection
        setTimeout(() => {
          if (this.onopen) this.onopen();
        }, 100);

        // Register for mock events
        (window as any).__mockWebSocket.listeners.push((event: any) => {
          if (this.onmessage) {
            this.onmessage(new MessageEvent('message', {
              data: JSON.stringify(event),
            }));
          }
        });
      }

      send(data: string): void {
        console.log('[MockWebSocket] Send:', data);
      }

      close(): void {
        this.readyState = 3; // CLOSED
        if (this.onclose) this.onclose();
      }
    };
  });

  // Bridge mock server events to page
  mockServer.subscribe(async (event) => {
    await page.evaluate((evt: any) => {
      (window as any).__mockWebSocket?.emit(evt);
    }, event);
  });

  return mockServer;
}
