/**
 * HTTP Client Mock Implementation for Testing
 *
 * Provides mock implementations for HTTP operations in component and service tests.
 */

import { of } from 'rxjs';

/**
 * Creates a mock HttpClient instance
 * All HTTP methods return empty observables to allow components to initialize
 */
export function createMockHttpClient() {
  return {
    get: jest.fn(() => of({})),
    post: jest.fn(() => of({})),
    put: jest.fn(() => of({})),
    delete: jest.fn(() => of({})),
    patch: jest.fn(() => of({})),
    head: jest.fn(() => of({})),
    request: jest.fn(() => of({})),
    options: jest.fn(() => of({})),
  };
}

/**
 * Creates a mock API service
 * Commonly injected service for data fetching
 */
export function createMockApiService() {
  return {
    get: jest.fn(() => of({})),
    post: jest.fn(() => of({})),
    put: jest.fn(() => of({})),
    delete: jest.fn(() => of({})),
    getPatients: jest.fn(() => of([])),
    getCareGaps: jest.fn(() => of([])),
    getMeasures: jest.fn(() => of([])),
    evaluateMeasure: jest.fn(() => of({})),
  };
}

export const MOCK_HTTP_CLIENT = createMockHttpClient();
export const MOCK_API_SERVICE = createMockApiService();
