/**
 * Mock LoggerService for unit tests
 *
 * Provides a consistent mock implementation of LoggerService across all test suites.
 * This prevents issues where components/services depend on LoggerService but tests
 * fail to provide the mock.
 *
 * Usage in TestBed:
 * ```typescript
 * const mockLoggerService = createMockLoggerService();
 * TestBed.configureTestingModule({
 *   providers: [
 *     { provide: LoggerService, useValue: mockLoggerService },
 *   ],
 * });
 * ```
 */

export function createMockLoggerService() {
  return {
    withContext: jest.fn().mockReturnValue({
      debug: jest.fn(),
      info: jest.fn(),
      warn: jest.fn(),
      error: jest.fn(),
    }),
    debug: jest.fn(),
    info: jest.fn(),
    warn: jest.fn(),
    error: jest.fn(),
  };
}

/**
 * Convenience export for direct use as mock value
 */
export const MOCK_LOGGER_SERVICE = createMockLoggerService();
