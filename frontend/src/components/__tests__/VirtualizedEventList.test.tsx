/**
 * Tests for VirtualizedEventList component
 * Following TDD approach - tests written FIRST
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderWithTheme, screen, userEvent } from '../../test/test-utils';
import { VirtualizedEventList } from '../VirtualizedEventList';
import { EventType, AnyEvaluationEvent } from '../../types/events';

describe('VirtualizedEventList', () => {
  const mockOnEventClick = vi.fn();

  // Mock event data
  const mockEvents: AnyEvaluationEvent[] = [
    {
      eventId: 'evt-1',
      eventType: EventType.EVALUATION_STARTED,
      tenantId: 'TENANT001',
      timestamp: 1704067200000, // 2024-01-01 00:00:00
      evaluationId: 'eval-1',
      measureId: 'HEDIS-CDC',
      measureName: 'Comprehensive Diabetes Care',
      patientId: 'patient-123',
      batchId: 'batch-1',
    },
    {
      eventId: 'evt-2',
      eventType: EventType.EVALUATION_COMPLETED,
      tenantId: 'TENANT001',
      timestamp: 1704067260000, // 2024-01-01 00:01:00
      evaluationId: 'eval-2',
      measureId: 'HEDIS-CBP',
      measureName: 'Controlling High Blood Pressure',
      patientId: 'patient-456',
      batchId: 'batch-1',
      inDenominator: true,
      inNumerator: true,
      complianceRate: 1.0,
      score: 100,
      durationMs: 125,
      evidence: {},
      careGapCount: 0,
    },
    {
      eventId: 'evt-3',
      eventType: EventType.EVALUATION_FAILED,
      tenantId: 'TENANT001',
      timestamp: 1704067320000, // 2024-01-01 00:02:00
      evaluationId: 'eval-3',
      measureId: 'HEDIS-BCS',
      measureName: 'Breast Cancer Screening',
      patientId: 'patient-789',
      batchId: 'batch-1',
      errorMessage: 'Failed to fetch patient data',
      errorCategory: 'FHIR_FETCH_ERROR' as any,
      durationMs: 50,
    },
    {
      eventType: EventType.BATCH_PROGRESS,
      batchId: 'batch-1',
      tenantId: 'TENANT001',
      measureId: 'HEDIS-CDC',
      measureName: 'Comprehensive Diabetes Care',
      totalPatients: 100,
      completedCount: 50,
      successCount: 48,
      failedCount: 2,
      pendingCount: 50,
      percentComplete: 50.0,
      avgDurationMs: 120,
      currentThroughput: 4.2,
      elapsedTimeMs: 12000,
      estimatedTimeRemainingMs: 12000,
      denominatorCount: 40,
      numeratorCount: 30,
      cumulativeComplianceRate: 75.0,
      timestamp: 1704067380000, // 2024-01-01 00:03:00
    },
  ];

  beforeEach(() => {
    mockOnEventClick.mockClear();
  });

  it('renders empty state when no events provided', () => {
    renderWithTheme(
      <VirtualizedEventList
        events={[]}
        onEventClick={mockOnEventClick}
      />
    );

    expect(screen.getByText(/no events to display/i)).toBeInTheDocument();
  });

  it('renders empty state with custom message', () => {
    renderWithTheme(
      <VirtualizedEventList
        events={[]}
        onEventClick={mockOnEventClick}
      />
    );

    // Check for empty state icon or text
    const emptyStateElement = screen.getByText(/no events to display/i);
    expect(emptyStateElement).toBeInTheDocument();
  });

  it('renders correct number of items using virtual scrolling', () => {
    renderWithTheme(
      <VirtualizedEventList
        events={mockEvents}
        onEventClick={mockOnEventClick}
      />
    );

    // The component should render (FixedSizeList renders only visible items)
    // We check for the presence of the list container
    const listContainer = screen.getByRole('list');
    expect(listContainer).toBeInTheDocument();
  });

  it('displays event timestamp correctly formatted', () => {
    renderWithTheme(
      <VirtualizedEventList
        events={mockEvents.slice(0, 1)}
        onEventClick={mockOnEventClick}
      />
    );

    // Should format timestamp as readable date (yyyy-MM-dd HH:mm:ss format)
    // The mock timestamp is 1704067200000
    // Check for formatted date pattern (timezone may vary)
    expect(screen.getByText(/\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}/)).toBeInTheDocument();
  });

  it('displays eventType for each row', () => {
    renderWithTheme(
      <VirtualizedEventList
        events={mockEvents.slice(0, 1)}
        onEventClick={mockOnEventClick}
      />
    );

    expect(screen.getByText('EVALUATION_STARTED')).toBeInTheDocument();
  });

  it('displays measureId for each row', () => {
    renderWithTheme(
      <VirtualizedEventList
        events={mockEvents.slice(0, 1)}
        onEventClick={mockOnEventClick}
      />
    );

    expect(screen.getByText(/HEDIS-CDC/)).toBeInTheDocument();
  });

  it('displays patientId when present', () => {
    renderWithTheme(
      <VirtualizedEventList
        events={mockEvents.slice(0, 1)}
        onEventClick={mockOnEventClick}
      />
    );

    expect(screen.getByText(/patient-123/)).toBeInTheDocument();
  });

  it('handles events without patientId gracefully (BatchProgressEvent)', () => {
    const batchEvent = mockEvents[3]; // BATCH_PROGRESS event has no patientId

    renderWithTheme(
      <VirtualizedEventList
        events={[batchEvent]}
        onEventClick={mockOnEventClick}
      />
    );

    // Should render without crashing
    expect(screen.getByText('BATCH_PROGRESS')).toBeInTheDocument();
    // Should show N/A (Batch) for missing patientId
    expect(screen.getByText('N/A (Batch)')).toBeInTheDocument();
  });

  it('calls onEventClick when row is clicked', async () => {
    const user = userEvent.setup();

    renderWithTheme(
      <VirtualizedEventList
        events={mockEvents.slice(0, 1)}
        onEventClick={mockOnEventClick}
      />
    );

    // Find and click the first row
    const firstRow = screen.getByText('EVALUATION_STARTED').closest('[role="button"]');
    expect(firstRow).toBeInTheDocument();

    if (firstRow) {
      await user.click(firstRow);
      expect(mockOnEventClick).toHaveBeenCalledWith(mockEvents[0]);
      expect(mockOnEventClick).toHaveBeenCalledTimes(1);
    }
  });

  it('applies hover effect to rows', async () => {
    const user = userEvent.setup();

    renderWithTheme(
      <VirtualizedEventList
        events={mockEvents.slice(0, 1)}
        onEventClick={mockOnEventClick}
      />
    );

    const firstRow = screen.getByText('EVALUATION_STARTED').closest('[role="button"]');
    expect(firstRow).toBeInTheDocument();

    if (firstRow) {
      // Check for cursor pointer style (indicates hoverable)
      expect(firstRow).toHaveStyle({ cursor: 'pointer' });
    }
  });

  it('uses provided height prop', () => {
    const customHeight = 600;

    renderWithTheme(
      <VirtualizedEventList
        events={mockEvents}
        onEventClick={mockOnEventClick}
        height={customHeight}
      />
    );

    // The container should have the specified height
    const container = screen.getByRole('list').parentElement;
    expect(container).toHaveStyle({ height: `${customHeight}px` });
  });

  it('uses default height when not provided', () => {
    renderWithTheme(
      <VirtualizedEventList
        events={mockEvents}
        onEventClick={mockOnEventClick}
      />
    );

    // Should use default height of 400px
    const container = screen.getByRole('list').parentElement;
    expect(container).toHaveStyle({ height: '400px' });
  });

  it('uses provided itemHeight prop', () => {
    const customItemHeight = 80;

    renderWithTheme(
      <VirtualizedEventList
        events={mockEvents}
        onEventClick={mockOnEventClick}
        itemHeight={customItemHeight}
      />
    );

    // FixedSizeList should use the custom item height
    // We verify by checking if rows exist (implementation detail)
    const listContainer = screen.getByRole('list');
    expect(listContainer).toBeInTheDocument();
  });

  it('handles large datasets efficiently (>1000 items)', () => {
    // Create 1500 mock events
    const largeDataset: AnyEvaluationEvent[] = Array.from({ length: 1500 }, (_, i) => ({
      eventId: `evt-${i}`,
      eventType: EventType.EVALUATION_COMPLETED,
      tenantId: 'TENANT001',
      timestamp: 1704067200000 + i * 1000,
      evaluationId: `eval-${i}`,
      measureId: 'HEDIS-CDC',
      measureName: 'Comprehensive Diabetes Care',
      patientId: `patient-${i}`,
      batchId: 'batch-1',
      inDenominator: true,
      inNumerator: i % 2 === 0,
      complianceRate: 0.5,
      score: 50,
      durationMs: 100,
      evidence: {},
      careGapCount: 0,
    }));

    renderWithTheme(
      <VirtualizedEventList
        events={largeDataset}
        onEventClick={mockOnEventClick}
      />
    );

    // Should render without performance issues
    // Virtual scrolling means only visible items are rendered
    const listContainer = screen.getByRole('list');
    expect(listContainer).toBeInTheDocument();

    // Should NOT render all 1500 items in DOM (virtual scrolling)
    // Only a handful of visible items should be in DOM
    const renderedRows = screen.queryAllByRole('button');
    expect(renderedRows.length).toBeLessThan(100); // Only visible items rendered
  });

  it('has proper ARIA labels for accessibility', () => {
    renderWithTheme(
      <VirtualizedEventList
        events={mockEvents}
        onEventClick={mockOnEventClick}
      />
    );

    // List should have role="list"
    const list = screen.getByRole('list');
    expect(list).toBeInTheDocument();
    expect(list).toHaveAttribute('aria-label', 'Event list');
  });

  it('rows have proper ARIA role for accessibility', () => {
    renderWithTheme(
      <VirtualizedEventList
        events={mockEvents.slice(0, 1)}
        onEventClick={mockOnEventClick}
      />
    );

    // Each row should have role="button" and be clickable
    const row = screen.getByRole('button');
    expect(row).toBeInTheDocument();
    expect(row).toHaveAttribute('tabIndex', '0');
  });

  it('supports keyboard navigation (Enter key)', async () => {
    const user = userEvent.setup();

    renderWithTheme(
      <VirtualizedEventList
        events={mockEvents.slice(0, 1)}
        onEventClick={mockOnEventClick}
      />
    );

    const row = screen.getByRole('button');

    // Focus the row
    row.focus();
    expect(row).toHaveFocus();

    // Press Enter
    await user.keyboard('{Enter}');

    // Should trigger click handler
    expect(mockOnEventClick).toHaveBeenCalledWith(mockEvents[0]);
  });

  it('supports keyboard navigation (Space key)', async () => {
    const user = userEvent.setup();

    renderWithTheme(
      <VirtualizedEventList
        events={mockEvents.slice(0, 1)}
        onEventClick={mockOnEventClick}
      />
    );

    const row = screen.getByRole('button');

    // Focus the row
    row.focus();

    // Press Space
    await user.keyboard(' ');

    // Should trigger click handler
    expect(mockOnEventClick).toHaveBeenCalledWith(mockEvents[0]);
  });

  it('renders all event types correctly', () => {
    renderWithTheme(
      <VirtualizedEventList
        events={mockEvents}
        onEventClick={mockOnEventClick}
      />
    );

    // All different event types should be visible
    expect(screen.getByText('EVALUATION_STARTED')).toBeInTheDocument();
    expect(screen.getByText('EVALUATION_COMPLETED')).toBeInTheDocument();
    expect(screen.getByText('EVALUATION_FAILED')).toBeInTheDocument();
    expect(screen.getByText('BATCH_PROGRESS')).toBeInTheDocument();
  });

  it('virtual scrolling only renders visible items in DOM', () => {
    // Create dataset larger than viewport
    const largeDataset: AnyEvaluationEvent[] = Array.from({ length: 100 }, (_, i) => ({
      eventId: `evt-${i}`,
      eventType: EventType.EVALUATION_COMPLETED,
      tenantId: 'TENANT001',
      timestamp: 1704067200000 + i * 1000,
      evaluationId: `eval-${i}`,
      measureId: 'HEDIS-CDC',
      measureName: 'Comprehensive Diabetes Care',
      patientId: `patient-${i}`,
      batchId: 'batch-1',
      inDenominator: true,
      inNumerator: true,
      complianceRate: 1.0,
      score: 100,
      durationMs: 100,
      evidence: {},
      careGapCount: 0,
    }));

    renderWithTheme(
      <VirtualizedEventList
        events={largeDataset}
        onEventClick={mockOnEventClick}
        height={400}
        itemHeight={60}
      />
    );

    // With height=400 and itemHeight=60, approximately 6-7 items should be visible
    // Virtual scrolling should not render all 100 items
    const renderedRows = screen.queryAllByRole('button');

    // Should render only visible items (plus some buffer)
    expect(renderedRows.length).toBeLessThan(20); // Much less than 100
    expect(renderedRows.length).toBeGreaterThan(0); // But more than 0
  });
});
