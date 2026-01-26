/**
 * Mock implementations for testing
 *
 * Centralized mocks for common services and utilities used across test suites.
 * Provides a consistent pattern for all service dependencies in tests.
 */

// Logging
export { createMockLoggerService, MOCK_LOGGER_SERVICE } from './logger.mock';

// State management (NgRx)
export { createMockStore, MOCK_STORE } from './store.mock';

// Routing
export { createMockRouter, MOCK_ROUTER } from './router.mock';

// Dialog
export {
  createMockMatDialogRef,
  createMockMatDialog,
  MOCK_MAT_DIALOG_REF,
  MOCK_MAT_DIALOG,
} from './dialog.mock';

// HTTP
export {
  createMockHttpClient,
  createMockApiService,
  MOCK_HTTP_CLIENT,
  MOCK_API_SERVICE,
} from './http.mock';
