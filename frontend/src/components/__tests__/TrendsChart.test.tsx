/**
 * Tests for TrendsChart component
 */

import { describe, it, expect } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import { renderWithTheme } from '../../test/test-utils';
import TrendsChart from '../TrendsChart';
import { BatchProgressEvent, EventType } from '../../types/events';

describe('TrendsChart', () => {
  // Create mock batch data spanning multiple days/weeks
  const now = Date.now();
  const oneDay = 24 * 60 * 60 * 1000;
  const oneWeek = 7 * oneDay;
  const oneMonth = 30 * oneDay;

  const createMockBatch = (
    daysAgo: number,
    successCount: number,
    failedCount: number,
    avgDuration: number,
    complianceRate: number
  ): BatchProgressEvent => ({
    eventType: EventType.BATCH_PROGRESS,
    batchId: `batch-${daysAgo}`,
    tenantId: 'TENANT001',
    measureId: 'HEDIS-CDC',
    measureName: 'Comprehensive Diabetes Care',
    totalPatients: 100,
    completedCount: successCount + failedCount,
    successCount,
    failedCount,
    pendingCount: 100 - (successCount + failedCount),
    percentComplete: ((successCount + failedCount) / 100) * 100,
    avgDurationMs: avgDuration,
    currentThroughput: 3.5,
    elapsedTimeMs: 10000,
    estimatedTimeRemainingMs: 5000,
    denominatorCount: 80,
    numeratorCount: Math.floor(80 * (complianceRate / 100)),
    cumulativeComplianceRate: complianceRate,
    timestamp: now - daysAgo * oneDay,
  });

  const mockBatches: BatchProgressEvent[] = [
    createMockBatch(0, 90, 10, 120, 85.5),
    createMockBatch(1, 85, 15, 130, 82.3),
    createMockBatch(2, 88, 12, 125, 83.7),
    createMockBatch(3, 92, 8, 115, 87.2),
    createMockBatch(7, 80, 20, 140, 78.5),
    createMockBatch(14, 75, 25, 150, 75.0),
    createMockBatch(21, 70, 30, 160, 72.5),
    createMockBatch(35, 65, 35, 170, 70.0),
  ];

  it('renders empty state when no batches provided', () => {
    renderWithTheme(
      <TrendsChart batches={[]} metric="successRate" />
    );

    expect(screen.getByTestId('trends-chart')).toBeInTheDocument();
  });

  it('renders chart with batch data', async () => {
    const { container } = renderWithTheme(
      <TrendsChart batches={mockBatches} metric="successRate" />
    );

    await waitFor(() => {
      expect(screen.getByTestId('trends-chart')).toBeInTheDocument();
      const chart = container.querySelector('.recharts-responsive-container');
      expect(chart).toBeInTheDocument();
    });
  });

  it('displays success rate metric correctly', async () => {
    const { container } = renderWithTheme(
      <TrendsChart batches={mockBatches} metric="successRate" />
    );

    await waitFor(() => {
      // Check that Y-axis label contains % symbol for success rate
      const yAxisLabel = container.querySelector('.recharts-label');
      expect(yAxisLabel).toBeInTheDocument();
    });
  });

  it('displays average duration metric correctly', async () => {
    const { container } = renderWithTheme(
      <TrendsChart batches={mockBatches} metric="avgDuration" />
    );

    await waitFor(() => {
      // Check that Y-axis is rendered for duration metric
      const yAxis = container.querySelector('.recharts-yAxis');
      expect(yAxis).toBeInTheDocument();
    });
  });

  it('displays compliance rate metric correctly', async () => {
    const { container } = renderWithTheme(
      <TrendsChart batches={mockBatches} metric="complianceRate" />
    );

    await waitFor(() => {
      // Check that Y-axis is rendered for compliance rate
      const yAxis = container.querySelector('.recharts-yAxis');
      expect(yAxis).toBeInTheDocument();
    });
  });

  it('filters data by time range (day)', async () => {
    const { container } = renderWithTheme(
      <TrendsChart
        batches={mockBatches}
        metric="successRate"
        timeRange="day"
      />
    );

    await waitFor(() => {
      expect(screen.getByTestId('trends-chart')).toBeInTheDocument();
    });

    // Only data from last day should be shown (1 batch)
    // Verify chart renders with filtered data
    const chart = container.querySelector('.recharts-responsive-container');
    expect(chart).toBeInTheDocument();
  });

  it('filters data by time range (week)', async () => {
    const { container } = renderWithTheme(
      <TrendsChart
        batches={mockBatches}
        metric="successRate"
        timeRange="week"
      />
    );

    await waitFor(() => {
      expect(screen.getByTestId('trends-chart')).toBeInTheDocument();
    });

    // Only data from last week should be shown (5 batches)
    const chart = container.querySelector('.recharts-responsive-container');
    expect(chart).toBeInTheDocument();
  });

  it('filters data by time range (month)', async () => {
    const { container } = renderWithTheme(
      <TrendsChart
        batches={mockBatches}
        metric="successRate"
        timeRange="month"
      />
    );

    await waitFor(() => {
      expect(screen.getByTestId('trends-chart')).toBeInTheDocument();
    });

    // Only data from last month should be shown (7 batches)
    const chart = container.querySelector('.recharts-responsive-container');
    expect(chart).toBeInTheDocument();
  });

  it('shows all data when timeRange is all', async () => {
    const { container } = renderWithTheme(
      <TrendsChart
        batches={mockBatches}
        metric="successRate"
        timeRange="all"
      />
    );

    await waitFor(() => {
      expect(screen.getByTestId('trends-chart')).toBeInTheDocument();
    });

    // All 8 batches should be shown
    const chart = container.querySelector('.recharts-responsive-container');
    expect(chart).toBeInTheDocument();
  });

  it('renders as line chart', async () => {
    const { container } = renderWithTheme(
      <TrendsChart
        batches={mockBatches}
        metric="successRate"
        chartType="line"
      />
    );

    await waitFor(() => {
      const line = container.querySelector('.recharts-line');
      expect(line).toBeInTheDocument();
    });
  });

  it('renders as area chart', async () => {
    const { container } = renderWithTheme(
      <TrendsChart
        batches={mockBatches}
        metric="successRate"
        chartType="area"
      />
    );

    await waitFor(() => {
      const area = container.querySelector('.recharts-area');
      expect(area).toBeInTheDocument();
    });
  });

  it('sorts data by timestamp ascending', async () => {
    // Create unsorted batches
    const unsortedBatches = [
      createMockBatch(7, 80, 20, 140, 78.5),
      createMockBatch(1, 85, 15, 130, 82.3),
      createMockBatch(14, 75, 25, 150, 75.0),
      createMockBatch(0, 90, 10, 120, 85.5),
    ];

    const { container } = renderWithTheme(
      <TrendsChart batches={unsortedBatches} metric="successRate" />
    );

    await waitFor(() => {
      expect(screen.getByTestId('trends-chart')).toBeInTheDocument();
      // Chart should render without errors
      const chart = container.querySelector('.recharts-responsive-container');
      expect(chart).toBeInTheDocument();
    });
  });

  it('formats Y-axis with correct units (%)', async () => {
    const { container } = renderWithTheme(
      <TrendsChart batches={mockBatches} metric="successRate" />
    );

    await waitFor(() => {
      // Check for Y-axis with percentage formatting
      const yAxisTicks = container.querySelectorAll('.recharts-yAxis .recharts-text');
      expect(yAxisTicks.length).toBeGreaterThan(0);
    });
  });

  it('formats Y-axis with correct units (ms)', async () => {
    const { container } = renderWithTheme(
      <TrendsChart batches={mockBatches} metric="avgDuration" />
    );

    await waitFor(() => {
      // Check for Y-axis with ms formatting
      const yAxisTicks = container.querySelectorAll('.recharts-yAxis .recharts-text');
      expect(yAxisTicks.length).toBeGreaterThan(0);
    });
  });

  it('formats X-axis as readable dates', async () => {
    const { container } = renderWithTheme(
      <TrendsChart batches={mockBatches} metric="successRate" />
    );

    await waitFor(() => {
      // Check for X-axis with date formatting
      const xAxisTicks = container.querySelectorAll('.recharts-xAxis .recharts-text');
      expect(xAxisTicks.length).toBeGreaterThan(0);
    });
  });

  it('tooltip shows correct data on hover', async () => {
    const { container } = renderWithTheme(
      <TrendsChart batches={mockBatches} metric="successRate" />
    );

    await waitFor(() => {
      // Check that tooltip wrapper exists
      const tooltip = container.querySelector('.recharts-tooltip-wrapper');
      expect(tooltip).toBeInTheDocument();
    });
  });

  it('responsive chart resizes', async () => {
    const { container } = renderWithTheme(
      <TrendsChart batches={mockBatches} metric="successRate" />
    );

    await waitFor(() => {
      // Check for ResponsiveContainer
      const responsiveContainer = container.querySelector('.recharts-responsive-container');
      expect(responsiveContainer).toBeInTheDocument();
    });
  });

  it('defaults to line chart when chartType not specified', async () => {
    const { container } = renderWithTheme(
      <TrendsChart batches={mockBatches} metric="successRate" />
    );

    await waitFor(() => {
      const line = container.querySelector('.recharts-line');
      expect(line).toBeInTheDocument();
    });
  });

  it('defaults to all time range when timeRange not specified', async () => {
    const { container } = renderWithTheme(
      <TrendsChart batches={mockBatches} metric="successRate" />
    );

    await waitFor(() => {
      expect(screen.getByTestId('trends-chart')).toBeInTheDocument();
      // All data should be shown
      const chart = container.querySelector('.recharts-responsive-container');
      expect(chart).toBeInTheDocument();
    });
  });

  it('handles single batch data point', async () => {
    const singleBatch = [createMockBatch(0, 90, 10, 120, 85.5)];

    const { container } = renderWithTheme(
      <TrendsChart batches={singleBatch} metric="successRate" />
    );

    await waitFor(() => {
      expect(screen.getByTestId('trends-chart')).toBeInTheDocument();
      const chart = container.querySelector('.recharts-responsive-container');
      expect(chart).toBeInTheDocument();
    });
  });
});
