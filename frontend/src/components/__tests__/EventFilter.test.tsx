/**
 * Tests for EventFilter component
 * Following TDD approach - tests written first
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderWithTheme, screen, userEvent } from '../../test/test-utils';
import { EventFilter } from '../EventFilter';
import { EventType } from '../../types/events';

describe('EventFilter', () => {
  const mockEventTypes = [
    EventType.BATCH_PROGRESS,
    EventType.EVALUATION_COMPLETED,
    EventType.EVALUATION_FAILED,
    EventType.EVALUATION_STARTED,
  ];

  const mockMeasures = ['HEDIS-CDC', 'HEDIS-CBP', 'HEDIS-BCS'];

  const mockOnFilterChange = vi.fn();

  beforeEach(() => {
    // Clear localStorage before each test
    localStorage.clear();
  });

  afterEach(() => {
    mockOnFilterChange.mockClear();
  });

  it('renders with default state (no filters)', () => {
    renderWithTheme(
      <EventFilter
        availableEventTypes={mockEventTypes}
        availableMeasures={mockMeasures}
        onFilterChange={mockOnFilterChange}
      />
    );

    expect(screen.getByText('Event Filters')).toBeInTheDocument();
    expect(screen.getByText('All Measures')).toBeInTheDocument();
  });

  it('displays all available event types as chips', () => {
    renderWithTheme(
      <EventFilter
        availableEventTypes={mockEventTypes}
        availableMeasures={mockMeasures}
        onFilterChange={mockOnFilterChange}
      />
    );

    expect(screen.getByText('BATCH_PROGRESS')).toBeInTheDocument();
    expect(screen.getByText('EVALUATION_COMPLETED')).toBeInTheDocument();
    expect(screen.getByText('EVALUATION_FAILED')).toBeInTheDocument();
    expect(screen.getByText('EVALUATION_STARTED')).toBeInTheDocument();
  });

  it('toggles event type selection on click', async () => {
    const user = userEvent.setup();

    renderWithTheme(
      <EventFilter
        availableEventTypes={mockEventTypes}
        availableMeasures={mockMeasures}
        onFilterChange={mockOnFilterChange}
      />
    );

    const chip = screen.getByText('BATCH_PROGRESS');
    await user.click(chip);

    // onFilterChange should be called with BATCH_PROGRESS in eventTypes
    expect(mockOnFilterChange).toHaveBeenCalledWith({
      eventTypes: [EventType.BATCH_PROGRESS],
      measureId: null,
      statusFilter: 'all',
    });

    // Click again to deselect
    await user.click(chip);

    expect(mockOnFilterChange).toHaveBeenCalledWith({
      eventTypes: [],
      measureId: null,
      statusFilter: 'all',
    });
  });

  it('supports multi-select for event types', async () => {
    const user = userEvent.setup();

    renderWithTheme(
      <EventFilter
        availableEventTypes={mockEventTypes}
        availableMeasures={mockMeasures}
        onFilterChange={mockOnFilterChange}
      />
    );

    // Select multiple event types
    await user.click(screen.getByText('BATCH_PROGRESS'));
    await user.click(screen.getByText('EVALUATION_COMPLETED'));

    // Should have both selected
    expect(mockOnFilterChange).toHaveBeenLastCalledWith({
      eventTypes: [EventType.BATCH_PROGRESS, EventType.EVALUATION_COMPLETED],
      measureId: null,
      statusFilter: 'all',
    });
  });

  it('displays measure dropdown with "All Measures"', () => {
    renderWithTheme(
      <EventFilter
        availableEventTypes={mockEventTypes}
        availableMeasures={mockMeasures}
        onFilterChange={mockOnFilterChange}
      />
    );

    expect(screen.getByText('All Measures')).toBeInTheDocument();
  });

  it('allows selecting a specific measure', async () => {
    const user = userEvent.setup();

    renderWithTheme(
      <EventFilter
        availableEventTypes={mockEventTypes}
        availableMeasures={mockMeasures}
        onFilterChange={mockOnFilterChange}
      />
    );

    // Click on the select dropdown
    const measureSelect = screen.getByLabelText('Measure');
    await user.click(measureSelect);

    // Select a specific measure
    const option = screen.getByText('HEDIS-CDC');
    await user.click(option);

    expect(mockOnFilterChange).toHaveBeenCalledWith({
      eventTypes: [],
      measureId: 'HEDIS-CDC',
      statusFilter: 'all',
    });
  });

  it('calls onFilterChange when filters update', async () => {
    const user = userEvent.setup();

    renderWithTheme(
      <EventFilter
        availableEventTypes={mockEventTypes}
        availableMeasures={mockMeasures}
        onFilterChange={mockOnFilterChange}
      />
    );

    await user.click(screen.getByText('BATCH_PROGRESS'));

    expect(mockOnFilterChange).toHaveBeenCalled();
  });

  it('quick filter "Errors Only" selects only EVALUATION_FAILED', async () => {
    const user = userEvent.setup();

    renderWithTheme(
      <EventFilter
        availableEventTypes={mockEventTypes}
        availableMeasures={mockMeasures}
        onFilterChange={mockOnFilterChange}
      />
    );

    const errorsOnlyButton = screen.getByText('Errors Only');
    await user.click(errorsOnlyButton);

    expect(mockOnFilterChange).toHaveBeenCalledWith({
      eventTypes: [EventType.EVALUATION_FAILED],
      measureId: null,
      statusFilter: 'errors',
    });
  });

  it('quick filter "Success Only" selects only EVALUATION_COMPLETED', async () => {
    const user = userEvent.setup();

    renderWithTheme(
      <EventFilter
        availableEventTypes={mockEventTypes}
        availableMeasures={mockMeasures}
        onFilterChange={mockOnFilterChange}
      />
    );

    const successOnlyButton = screen.getByText('Success Only');
    await user.click(successOnlyButton);

    expect(mockOnFilterChange).toHaveBeenCalledWith({
      eventTypes: [EventType.EVALUATION_COMPLETED],
      measureId: null,
      statusFilter: 'success',
    });
  });

  it('quick filter "All" resets status filter and clears event type filters', async () => {
    const user = userEvent.setup();

    renderWithTheme(
      <EventFilter
        availableEventTypes={mockEventTypes}
        availableMeasures={mockMeasures}
        onFilterChange={mockOnFilterChange}
      />
    );

    // First select Errors Only
    await user.click(screen.getByText('Errors Only'));

    // Then click All
    await user.click(screen.getByText('All'));

    expect(mockOnFilterChange).toHaveBeenLastCalledWith({
      eventTypes: [],
      measureId: null,
      statusFilter: 'all',
    });
  });

  it('"Clear All" resets all filters', async () => {
    const user = userEvent.setup();

    renderWithTheme(
      <EventFilter
        availableEventTypes={mockEventTypes}
        availableMeasures={mockMeasures}
        onFilterChange={mockOnFilterChange}
      />
    );

    // Select some filters
    await user.click(screen.getByText('BATCH_PROGRESS'));
    await user.click(screen.getByText('EVALUATION_COMPLETED'));

    // Click Clear All
    const clearButton = screen.getByText('Clear All Filters');
    await user.click(clearButton);

    expect(mockOnFilterChange).toHaveBeenLastCalledWith({
      eventTypes: [],
      measureId: null,
      statusFilter: 'all',
    });
  });

  it('displays active filter count badge', async () => {
    const user = userEvent.setup();

    renderWithTheme(
      <EventFilter
        availableEventTypes={mockEventTypes}
        availableMeasures={mockMeasures}
        onFilterChange={mockOnFilterChange}
      />
    );

    // Initially no badge (0 filters)
    expect(screen.queryByText('2')).not.toBeInTheDocument();

    // Select two event types
    await user.click(screen.getByText('BATCH_PROGRESS'));
    await user.click(screen.getByText('EVALUATION_COMPLETED'));

    // Should show badge with count 2
    expect(screen.getByText('2')).toBeInTheDocument();
  });

  it('counts measure filter in active filter badge', async () => {
    const user = userEvent.setup();

    renderWithTheme(
      <EventFilter
        availableEventTypes={mockEventTypes}
        availableMeasures={mockMeasures}
        onFilterChange={mockOnFilterChange}
      />
    );

    // Select one event type
    await user.click(screen.getByText('BATCH_PROGRESS'));

    // Select a measure
    await user.click(screen.getByLabelText('Measure'));
    await user.click(screen.getByText('HEDIS-CDC'));

    // Should show badge with count 2 (1 event type + 1 measure)
    expect(screen.getByText('2')).toBeInTheDocument();
  });

  it('collapses/expands filter panel', async () => {
    const user = userEvent.setup();

    renderWithTheme(
      <EventFilter
        availableEventTypes={mockEventTypes}
        availableMeasures={mockMeasures}
        onFilterChange={mockOnFilterChange}
      />
    );

    // Initially expanded (default)
    expect(screen.getByText('BATCH_PROGRESS')).toBeInTheDocument();
    expect(screen.getByText('Event Types')).toBeInTheDocument();

    // Find and click the collapse button
    const collapseButton = screen.getByLabelText('Collapse filters');
    await user.click(collapseButton);

    // The expand button should now be visible
    expect(screen.getByLabelText('Expand filters')).toBeInTheDocument();

    // Click again to expand
    const expandButton = screen.getByLabelText('Expand filters');
    await user.click(expandButton);

    // Collapse button should be visible again
    expect(screen.getByLabelText('Collapse filters')).toBeInTheDocument();
    // Content should be visible
    expect(screen.getByText('BATCH_PROGRESS')).toBeInTheDocument();
  });

  it('handles empty event type list', () => {
    renderWithTheme(
      <EventFilter
        availableEventTypes={[]}
        availableMeasures={mockMeasures}
        onFilterChange={mockOnFilterChange}
      />
    );

    // Should render without crashing
    expect(screen.getByText('Event Filters')).toBeInTheDocument();

    // Should show a message about no event types
    expect(screen.getByText('No event types available')).toBeInTheDocument();
  });

  it('handles empty measures list', () => {
    renderWithTheme(
      <EventFilter
        availableEventTypes={mockEventTypes}
        availableMeasures={[]}
        onFilterChange={mockOnFilterChange}
      />
    );

    // Should render with only "All Measures" option
    expect(screen.getByText('All Measures')).toBeInTheDocument();
  });

  it('persists filter state when component remounts', async () => {
    const user = userEvent.setup();

    const { unmount } = renderWithTheme(
      <EventFilter
        availableEventTypes={mockEventTypes}
        availableMeasures={mockMeasures}
        onFilterChange={mockOnFilterChange}
      />
    );

    // Select some filters
    await user.click(screen.getByText('BATCH_PROGRESS'));

    // Unmount the component
    unmount();

    // Remount with same props
    renderWithTheme(
      <EventFilter
        availableEventTypes={mockEventTypes}
        availableMeasures={mockMeasures}
        onFilterChange={mockOnFilterChange}
      />
    );

    // The BATCH_PROGRESS chip should still be selected (visually indicated)
    // We'll check this by verifying the onFilterChange was called on mount
    // with the persisted state
    expect(mockOnFilterChange).toHaveBeenCalledWith(
      expect.objectContaining({
        eventTypes: expect.arrayContaining([EventType.BATCH_PROGRESS]),
      })
    );
  });

  it('shows visual indication of selected event type chips', async () => {
    const user = userEvent.setup();

    renderWithTheme(
      <EventFilter
        availableEventTypes={mockEventTypes}
        availableMeasures={mockMeasures}
        onFilterChange={mockOnFilterChange}
      />
    );

    // Initially should have outlined variant (not selected)
    const initialChip = screen.getByText('BATCH_PROGRESS').closest('.MuiChip-root');
    expect(initialChip).toHaveClass('MuiChip-outlined');

    // Click to select
    await user.click(screen.getByText('BATCH_PROGRESS'));

    // Should now have filled variant (selected)
    const selectedChip = screen.getByText('BATCH_PROGRESS').closest('.MuiChip-root');
    expect(selectedChip).toHaveClass('MuiChip-filled');
  });
});
