/**
 * Angular Router Mock Implementation for Testing
 *
 * Provides a mock Router instance for testing components that depend on routing.
 * Supports navigation calls without actual route changes.
 */

import { of } from 'rxjs';

/**
 * Creates a mock Router instance
 * navigate() returns a resolved promise
 * url property is safely accessible
 */
export function createMockRouter() {
  return {
    navigate: jest.fn(() => Promise.resolve(true)),
    navigateByUrl: jest.fn(() => Promise.resolve(true)),
    createUrlTree: jest.fn(),
    parseUrl: jest.fn(),
    url: '/mock-url',
    events: of({}),
    routerState: { root: { firstChild: null } },
    config: [],
    outlet: null,
    params: {},
    queryParams: {},
    fragment: null,
  };
}

export const MOCK_ROUTER = createMockRouter();
