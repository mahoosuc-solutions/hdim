import { setupServer } from 'msw/node';
import { handlers, errorHandlers } from './handlers';

/**
 * MSW Server Setup for Node.js (Playwright tests)
 *
 * Use this server to mock API responses in E2E tests when
 * running without a live backend.
 *
 * Usage in tests:
 * ```typescript
 * import { server } from '../mocks/server';
 *
 * test.beforeAll(() => server.listen());
 * test.afterEach(() => server.resetHandlers());
 * test.afterAll(() => server.close());
 *
 * // Override handler for specific test
 * test('handles server error', async () => {
 *   server.use(errorHandlers.serverError);
 *   // ... test error handling
 * });
 * ```
 */

// Create the MSW server with default handlers
export const server = setupServer(...handlers);

// Re-export error handlers for use in tests
export { errorHandlers };

// Export types
export type MockServer = typeof server;
