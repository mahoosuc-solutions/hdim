/**
 * Test utilities for React component testing
 */

import { ReactElement } from 'react';
import { render, RenderOptions } from '@testing-library/react';
import { ThemeProvider, createTheme } from '@mui/material';
import { BatchProgressEvent, EventType } from '../types/events';

const theme = createTheme();

interface CustomRenderOptions extends Omit<RenderOptions, 'wrapper'> {
  theme?: typeof theme;
}

/**
 * Custom render function with MUI ThemeProvider
 */
export function renderWithTheme(
  ui: ReactElement,
  options?: CustomRenderOptions
) {
  const { theme: customTheme, ...renderOptions } = options || {};

  return render(ui, {
    wrapper: ({ children }) => (
      <ThemeProvider theme={customTheme || theme}>
        {children}
      </ThemeProvider>
    ),
    ...renderOptions,
  });
}

/**
 * Mock batch progress data for testing
 */
export const mockBatchProgress: BatchProgressEvent = {
  eventType: EventType.BATCH_PROGRESS,
  batchId: 'test-batch-123',
  tenantId: 'TENANT001',
  measureId: 'HEDIS-CDC',
  measureName: 'Comprehensive Diabetes Care',
  totalPatients: 100,
  completedCount: 45,
  successCount: 43,
  failedCount: 2,
  pendingCount: 55,
  percentComplete: 45.0,
  avgDurationMs: 125.5,
  currentThroughput: 3.6,
  elapsedTimeMs: 12500,
  estimatedTimeRemainingMs: 15278,
  denominatorCount: 38,
  numeratorCount: 28,
  cumulativeComplianceRate: 73.7,
  timestamp: Date.now(),
};

/**
 * Create mock batch progress with custom values
 */
export function createMockBatchProgress(overrides?: Partial<BatchProgressEvent>): BatchProgressEvent {
  return {
    ...mockBatchProgress,
    ...overrides,
  };
}

// Re-export everything from testing library
export * from '@testing-library/react';
export { default as userEvent } from '@testing-library/user-event';
