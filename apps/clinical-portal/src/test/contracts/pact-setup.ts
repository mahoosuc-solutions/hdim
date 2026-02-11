/**
 * Pact Test Setup Utilities
 *
 * Provides utilities for setting up HTTP client testing with Pact mock servers.
 * Uses the native fetch API for Node.js environment compatibility.
 *
 * Note: This module is designed to work in Jest's Node test environment,
 * which is required for Pact's native bindings. For Angular integration tests,
 * use the Angular HttpClient with appropriate test setup.
 */
import { TEST_CONSTANTS } from '../../../pact/pact-config';

/**
 * HTTP methods supported by the test client.
 */
export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';

/**
 * A simplified HTTP client interface for contract tests.
 */
export interface TestHttpClient {
  /**
   * Performs a GET request.
   *
   * @param path - URL path (relative to mock server)
   * @param headers - Optional additional headers
   * @returns Promise resolving to response body
   */
  get<T>(path: string, headers?: Record<string, string>): Promise<T>;

  /**
   * Performs a POST request.
   *
   * @param path - URL path (relative to mock server)
   * @param body - Request body
   * @param headers - Optional additional headers
   * @returns Promise resolving to response body
   */
  post<T>(path: string, body: unknown, headers?: Record<string, string>): Promise<T>;

  /**
   * Performs a PUT request.
   *
   * @param path - URL path (relative to mock server)
   * @param body - Request body
   * @param headers - Optional additional headers
   * @returns Promise resolving to response body
   */
  put<T>(path: string, body: unknown, headers?: Record<string, string>): Promise<T>;

  /**
   * Performs a DELETE request.
   *
   * @param path - URL path (relative to mock server)
   * @param headers - Optional additional headers
   * @returns Promise resolving to response body
   */
  delete<T>(path: string, headers?: Record<string, string>): Promise<T>;

  /**
   * Performs a PATCH request.
   *
   * @param path - URL path (relative to mock server)
   * @param body - Request body
   * @param headers - Optional additional headers
   * @returns Promise resolving to response body
   */
  patch<T>(path: string, body: unknown, headers?: Record<string, string>): Promise<T>;
}

/**
 * Creates a test HTTP client using the fetch API.
 *
 * This is useful when Angular's HttpClient is not available or
 * when testing pure TypeScript code without Angular dependencies.
 *
 * @param mockServerUrl - URL of the Pact mock server
 * @param defaultTenantId - Default tenant ID to use
 * @returns TestHttpClient-like interface using fetch
 *
 * @example
 * ```typescript
 * const client = createFetchTestClient(mockServer.url);
 * const patient = await client.get('/api/v1/patients/123');
 * ```
 */
export function createFetchTestClient(
  mockServerUrl: string,
  defaultTenantId = TEST_CONSTANTS.TENANT_ID
): TestHttpClient {
  const buildUrl = (path: string): string => {
    const normalizedPath = path.startsWith('/') ? path : `/${path}`;
    const baseUrl = mockServerUrl.endsWith('/')
      ? mockServerUrl.slice(0, -1)
      : mockServerUrl;
    return `${baseUrl}${normalizedPath}`;
  };

  const buildHeaders = (custom?: Record<string, string>): HeadersInit => ({
    'Content-Type': 'application/json',
    'X-Tenant-ID': defaultTenantId,
    ...custom,
  });

  const handleResponse = async <T>(response: Response): Promise<T> => {
    if (!response.ok) {
      const errorBody = await response.text();
      throw new Error(`HTTP ${response.status}: ${errorBody}`);
    }
    return response.json() as Promise<T>;
  };

  return {
    get: async <T>(path: string, headers?: Record<string, string>): Promise<T> => {
      const response = await fetch(buildUrl(path), {
        method: 'GET',
        headers: buildHeaders(headers),
      });
      return handleResponse<T>(response);
    },

    post: async <T>(path: string, body: unknown, headers?: Record<string, string>): Promise<T> => {
      const response = await fetch(buildUrl(path), {
        method: 'POST',
        headers: buildHeaders(headers),
        body: JSON.stringify(body),
      });
      return handleResponse<T>(response);
    },

    put: async <T>(path: string, body: unknown, headers?: Record<string, string>): Promise<T> => {
      const response = await fetch(buildUrl(path), {
        method: 'PUT',
        headers: buildHeaders(headers),
        body: JSON.stringify(body),
      });
      return handleResponse<T>(response);
    },

    delete: async <T>(path: string, headers?: Record<string, string>): Promise<T> => {
      const response = await fetch(buildUrl(path), {
        method: 'DELETE',
        headers: buildHeaders(headers),
      });
      return handleResponse<T>(response);
    },

    patch: async <T>(path: string, body: unknown, headers?: Record<string, string>): Promise<T> => {
      const response = await fetch(buildUrl(path), {
        method: 'PATCH',
        headers: buildHeaders(headers),
        body: JSON.stringify(body),
      });
      return handleResponse<T>(response);
    },
  };
}
