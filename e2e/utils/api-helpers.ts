/**
 * API Helpers for E2E Testing
 *
 * Provides direct backend API access for test setup, teardown,
 * and verification of state changes.
 */

export interface ApiHelpersOptions {
  baseUrl: string;
  tenantId: string;
  timeout?: number;
}

export interface AuthResponse {
  token: string;
  refreshToken?: string;
  expiresIn: number;
  tokenType: string;
}

export interface ApiResponse<T = any> {
  data: T;
  status: number;
  headers: Headers;
}

export class ApiHelpers {
  private options: Required<ApiHelpersOptions>;
  private authToken: string | null = null;

  constructor(options: ApiHelpersOptions) {
    this.options = {
      baseUrl: options.baseUrl,
      tenantId: options.tenantId,
      timeout: options.timeout || 30000,
    };
  }

  /**
   * Authenticate and store token
   */
  async authenticate(username: string, password: string): Promise<AuthResponse> {
    const response = await fetch(`${this.options.baseUrl}/api/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Tenant-ID': this.options.tenantId,
      },
      body: JSON.stringify({ username, password }),
    });

    if (!response.ok) {
      throw new Error(`Authentication failed: ${response.status} ${response.statusText}`);
    }

    const authResponse: AuthResponse = await response.json();
    this.authToken = authResponse.token;

    return authResponse;
  }

  /**
   * Make an authenticated API request
   */
  async request<T = any>(
    method: string,
    path: string,
    body?: object,
    options?: {
      headers?: Record<string, string>;
      timeout?: number;
    }
  ): Promise<T> {
    const controller = new AbortController();
    const timeoutId = setTimeout(
      () => controller.abort(),
      options?.timeout || this.options.timeout
    );

    try {
      const response = await fetch(`${this.options.baseUrl}${path}`, {
        method,
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
          'X-Tenant-ID': this.options.tenantId,
          ...(this.authToken && { 'Authorization': `Bearer ${this.authToken}` }),
          ...options?.headers,
        },
        body: body ? JSON.stringify(body) : undefined,
        signal: controller.signal,
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`API request failed: ${response.status} ${response.statusText} - ${errorText}`);
      }

      // Handle empty responses
      const text = await response.text();
      if (!text) {
        return null as T;
      }

      return JSON.parse(text);
    } finally {
      clearTimeout(timeoutId);
    }
  }

  /**
   * GET request shorthand
   */
  async get<T = any>(path: string, options?: { headers?: Record<string, string> }): Promise<T> {
    return this.request<T>('GET', path, undefined, options);
  }

  /**
   * POST request shorthand
   */
  async post<T = any>(path: string, body: object, options?: { headers?: Record<string, string> }): Promise<T> {
    return this.request<T>('POST', path, body, options);
  }

  /**
   * PUT request shorthand
   */
  async put<T = any>(path: string, body: object, options?: { headers?: Record<string, string> }): Promise<T> {
    return this.request<T>('PUT', path, body, options);
  }

  /**
   * DELETE request shorthand
   */
  async delete<T = any>(path: string, options?: { headers?: Record<string, string> }): Promise<T> {
    return this.request<T>('DELETE', path, undefined, options);
  }

  /**
   * PATCH request shorthand
   */
  async patch<T = any>(path: string, body: object, options?: { headers?: Record<string, string> }): Promise<T> {
    return this.request<T>('PATCH', path, body, options);
  }

  // ==================== Patient Operations ====================

  /**
   * Get patient by ID
   */
  async getPatient(patientId: string): Promise<any> {
    return this.get(`/api/v1/patients/${patientId}`);
  }

  /**
   * Search patients
   */
  async searchPatients(query: string, limit: number = 10): Promise<any[]> {
    return this.get(`/api/v1/patients?search=${encodeURIComponent(query)}&limit=${limit}`);
  }

  /**
   * Create patient
   */
  async createPatient(patient: object): Promise<any> {
    return this.post('/api/v1/patients', patient);
  }

  /**
   * Delete patient
   */
  async deletePatient(patientId: string): Promise<void> {
    await this.delete(`/api/v1/patients/${patientId}`);
  }

  // ==================== Quality Measure Operations ====================

  /**
   * Get quality measures
   */
  async getQualityMeasures(options?: { category?: string; status?: string }): Promise<any[]> {
    const params = new URLSearchParams();
    if (options?.category) params.append('category', options.category);
    if (options?.status) params.append('status', options.status);
    const queryString = params.toString() ? `?${params.toString()}` : '';
    return this.get(`/api/v1/quality-measures${queryString}`);
  }

  /**
   * Get single quality measure
   */
  async getQualityMeasure(measureId: string): Promise<any> {
    return this.get(`/api/v1/quality-measures/${measureId}`);
  }

  /**
   * Run quality evaluation
   */
  async runEvaluation(patientId: string, measureCode: string): Promise<any> {
    return this.post('/api/v1/evaluations', { patientId, measureCode });
  }

  /**
   * Get evaluation result
   */
  async getEvaluation(evaluationId: string): Promise<any> {
    return this.get(`/api/v1/evaluations/${evaluationId}`);
  }

  // ==================== Care Gap Operations ====================

  /**
   * Get care gaps
   */
  async getCareGaps(options?: {
    patientId?: string;
    status?: string;
    urgency?: string;
  }): Promise<any[]> {
    const params = new URLSearchParams();
    if (options?.patientId) params.append('patientId', options.patientId);
    if (options?.status) params.append('status', options.status);
    if (options?.urgency) params.append('urgency', options.urgency);
    const queryString = params.toString() ? `?${params.toString()}` : '';
    return this.get(`/api/v1/care-gaps${queryString}`);
  }

  /**
   * Get single care gap
   */
  async getCareGap(gapId: string): Promise<any> {
    return this.get(`/api/v1/care-gaps/${gapId}`);
  }

  /**
   * Close care gap
   */
  async closeCareGap(gapId: string, reason: string, notes?: string): Promise<any> {
    return this.post(`/api/v1/care-gaps/${gapId}/close`, { reason, notes });
  }

  /**
   * Record intervention
   */
  async recordIntervention(gapId: string, intervention: {
    type: string;
    outcome: string;
    notes?: string;
  }): Promise<any> {
    return this.post(`/api/v1/care-gaps/${gapId}/interventions`, intervention);
  }

  // ==================== Health Check Operations ====================

  /**
   * Check service health
   */
  async healthCheck(): Promise<{ status: string; components?: Record<string, any> }> {
    return this.get('/actuator/health');
  }

  /**
   * Wait for service to be healthy
   */
  async waitForHealth(timeoutMs: number = 60000): Promise<void> {
    const startTime = Date.now();

    while (Date.now() - startTime < timeoutMs) {
      try {
        const health = await this.healthCheck();
        if (health.status === 'UP') {
          return;
        }
      } catch {
        // Service not ready
      }

      await new Promise(resolve => setTimeout(resolve, 1000));
    }

    throw new Error(`Service not healthy after ${timeoutMs}ms`);
  }

  // ==================== Batch Operations ====================

  /**
   * Run batch evaluation
   */
  async runBatchEvaluation(patientIds: string[], measureCodes: string[]): Promise<any> {
    return this.post('/api/v1/evaluations/batch', { patientIds, measureCodes });
  }

  /**
   * Get batch status
   */
  async getBatchStatus(batchId: string): Promise<any> {
    return this.get(`/api/v1/evaluations/batch/${batchId}`);
  }

  /**
   * Bulk close care gaps
   */
  async bulkCloseCareGaps(gapIds: string[], reason: string): Promise<any> {
    return this.post('/api/v1/care-gaps/bulk-close', { gapIds, reason });
  }
}
