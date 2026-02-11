/**
 * NgRx Store Mock Implementation for Testing
 *
 * Provides a mock Store implementation for testing components that depend on NgRx.
 * Returns empty observables by default, allowing components to initialize without errors.
 */

import { of, EMPTY } from 'rxjs';

/**
 * Creates a mock NgRx Store instance
 * All select operations return empty observables to prevent initialization errors
 */
export function createMockStore() {
  return {
    select: jest.fn(() => of({})),
    dispatch: jest.fn(),
    subscribe: jest.fn(() => ({ unsubscribe: jest.fn() })),
    pipe: jest.fn(() => of({})),
    asObservable: jest.fn(() => of({})),
  };
}

export const MOCK_STORE = createMockStore();
