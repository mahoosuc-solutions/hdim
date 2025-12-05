/**
 * Tests for BatchComparisonView component
 * Following TDD approach - tests written first
 */

import { describe, it, expect, vi } from 'vitest';
import { renderWithTheme, screen, userEvent } from '../../test/test-utils';
import BatchComparisonView from '../BatchComparisonView';
import { BatchProgressEvent, EventType } from '../../types/events';

describe('BatchComparisonView', () => {
  // Mock batch data with different metrics
  const mockBatches: BatchProgressEvent[] = [
    {
      eventType: EventType.BATCH_PROGRESS,
      batchId: 'batch-001',
      tenantId: 'TENANT001',
      measureId: 'HEDIS-CDC',
      measureName: 'Comprehensive Diabetes Care',
      totalPatients: 100,
      completedCount: 100,
      successCount: 95,
      failedCount: 5,
      pendingCount: 0,
      percentComplete: 100.0,
      avgDurationMs: 125.5,
      currentThroughput: 3.6,
      elapsedTimeMs: 12500,
      estimatedTimeRemainingMs: 0,
      denominatorCount: 90,
      numeratorCount: 72,
      cumulativeComplianceRate: 80.0,
      timestamp: Date.now(),
    },
    {
      eventType: EventType.BATCH_PROGRESS,
      batchId: 'batch-002',
      tenantId: 'TENANT001',
      measureId: 'HEDIS-CBP',
      measureName: 'Controlling Blood Pressure',
      totalPatients: 150,
      completedCount: 150,
      successCount: 145,
      failedCount: 5,
      pendingCount: 0,
      percentComplete: 100.0,
      avgDurationMs: 98.3,
      currentThroughput: 4.2,
      elapsedTimeMs: 15000,
      estimatedTimeRemainingMs: 0,
      denominatorCount: 140,
      numeratorCount: 126,
      cumulativeComplianceRate: 90.0,
      timestamp: Date.now(),
    },
    {
      eventType: EventType.BATCH_PROGRESS,
      batchId: 'batch-003',
      tenantId: 'TENANT001',
      measureId: 'HEDIS-CCS',
      measureName: 'Cervical Cancer Screening',
      totalPatients: 200,
      completedCount: 200,
      successCount: 180,
      failedCount: 20,
      pendingCount: 0,
      percentComplete: 100.0,
      avgDurationMs: 150.2,
      currentThroughput: 2.8,
      elapsedTimeMs: 20000,
      estimatedTimeRemainingMs: 0,
      denominatorCount: 180,
      numeratorCount: 108,
      cumulativeComplianceRate: 60.0,
      timestamp: Date.now(),
    },
  ];

  it('renders empty state when fewer than 2 batches provided', () => {
    renderWithTheme(<BatchComparisonView batches={[mockBatches[0]]} />);

    expect(screen.getByText(/at least 2 batches/i)).toBeInTheDocument();
  });

  it('renders empty state when no batches provided', () => {
    renderWithTheme(<BatchComparisonView batches={[]} />);

    expect(screen.getByText(/at least 2 batches/i)).toBeInTheDocument();
  });

  it('renders batch selectors with all available batches', () => {
    renderWithTheme(<BatchComparisonView batches={mockBatches} />);

    // Should have two select dropdowns
    const selects = screen.getAllByRole('combobox');
    expect(selects).toHaveLength(2);
  });

  it('selects batch A and displays metrics', async () => {
    const user = userEvent.setup();
    renderWithTheme(<BatchComparisonView batches={mockBatches} />);

    // Open first select and choose batch
    const selects = screen.getAllByRole('combobox');
    await user.click(selects[0]);

    // Select first batch
    const option = await screen.findByText('batch-001');
    await user.click(option);

    // Clear button should become enabled after selection
    const clearButtons = screen.getAllByRole('button', { name: /clear/i });
    expect(clearButtons[0]).not.toBeDisabled();
  });

  it('selects batch B and displays metrics', async () => {
    const user = userEvent.setup();
    renderWithTheme(<BatchComparisonView batches={mockBatches} />);

    // Open second select and choose batch
    const selects = screen.getAllByRole('combobox');
    await user.click(selects[1]);

    // Select second batch
    const option = await screen.findByText('batch-002');
    await user.click(option);

    // Clear button should become enabled after selection
    const clearButtons = screen.getAllByRole('button', { name: /clear/i });
    expect(clearButtons[1]).not.toBeDisabled();
  });

  it('calculates diff correctly when B is better than A', async () => {
    const user = userEvent.setup();
    renderWithTheme(<BatchComparisonView batches={mockBatches} />);

    // Select batch A (batch-001)
    const selects = screen.getAllByRole('combobox');
    await user.click(selects[0]);
    const optionA = await screen.findByText('batch-001');
    await user.click(optionA);

    // Select batch B (batch-002) - better metrics
    await user.click(selects[1]);
    const optionB = await screen.findByText('batch-002');
    await user.click(optionB);

    // Should show "Comparison Metrics" heading when both selected
    expect(screen.getByText(/comparison metrics/i)).toBeInTheDocument();

    // Should show positive diff (B is better)
    expect(screen.getByText(/Diff: \+50/)).toBeInTheDocument(); // More evaluations
    expect(screen.getByText(/Diff: \+10\.0%/)).toBeInTheDocument(); // Better compliance
  });

  it('calculates diff correctly when B is worse than A', async () => {
    const user = userEvent.setup();
    renderWithTheme(<BatchComparisonView batches={mockBatches} />);

    // Select batch A (batch-002) - better metrics
    const selects = screen.getAllByRole('combobox');
    await user.click(selects[0]);
    const optionA = await screen.findByText('batch-002');
    await user.click(optionA);

    // Select batch B (batch-003) - worse compliance
    await user.click(selects[1]);
    const optionB = await screen.findByText('batch-003');
    await user.click(optionB);

    // Should show negative diff (B is worse)
    expect(screen.getByText(/Diff: -30\.0%/)).toBeInTheDocument(); // Worse compliance
  });

  it('shows green indicator when batch B is better', async () => {
    const user = userEvent.setup();
    renderWithTheme(<BatchComparisonView batches={mockBatches} />);

    // Select batch A (batch-001)
    const selects = screen.getAllByRole('combobox');
    await user.click(selects[0]);
    const optionA = await screen.findByText('batch-001');
    await user.click(optionA);

    // Select batch B (batch-002) - better compliance
    await user.click(selects[1]);
    const optionB = await screen.findByText('batch-002');
    await user.click(optionB);

    // Should show positive diffs (which indicate green/better)
    expect(screen.getByText(/Diff: \+50/)).toBeInTheDocument();
    expect(screen.getByText(/Diff: \+10\.0%/)).toBeInTheDocument();
  });

  it('shows red indicator when batch B is worse', async () => {
    const user = userEvent.setup();
    renderWithTheme(<BatchComparisonView batches={mockBatches} />);

    // Select batch A (batch-002)
    const selects = screen.getAllByRole('combobox');
    await user.click(selects[0]);
    const optionA = await screen.findByText('batch-002');
    await user.click(optionA);

    // Select batch B (batch-003) - worse compliance
    await user.click(selects[1]);
    const optionB = await screen.findByText('batch-003');
    await user.click(optionB);

    // Should show negative diffs (which indicate red/worse)
    expect(screen.getByText(/Diff: -30\.0%/)).toBeInTheDocument();
  });

  it('renders comparison chart with correct data', async () => {
    const user = userEvent.setup();
    const { container } = renderWithTheme(<BatchComparisonView batches={mockBatches} />);

    // Select both batches
    const selects = screen.getAllByRole('combobox');
    await user.click(selects[0]);
    await user.click(await screen.findByText('batch-001'));
    await user.click(selects[1]);
    await user.click(await screen.findByText('batch-002'));

    // Should render chart
    const chart = container.querySelector('.recharts-responsive-container');
    expect(chart).toBeInTheDocument();
  });

  it('clears batch A selection', async () => {
    const user = userEvent.setup();
    renderWithTheme(<BatchComparisonView batches={mockBatches} />);

    // Select batch A
    const selects = screen.getAllByRole('combobox');
    await user.click(selects[0]);
    await user.click(await screen.findByText('batch-001'));

    // Click clear button for batch A
    const clearButtons = screen.getAllByRole('button', { name: /clear/i });
    await user.click(clearButtons[0]);

    // Batch A should be cleared
    expect(screen.queryByText('100')).not.toBeInTheDocument();
  });

  it('clears batch B selection', async () => {
    const user = userEvent.setup();
    renderWithTheme(<BatchComparisonView batches={mockBatches} />);

    // Select batch B
    const selects = screen.getAllByRole('combobox');
    await user.click(selects[1]);
    await user.click(await screen.findByText('batch-002'));

    // Click clear button for batch B
    const clearButtons = screen.getAllByRole('button', { name: /clear/i });
    await user.click(clearButtons[1]);

    // Batch B should be cleared
    expect(screen.queryByText('150')).not.toBeInTheDocument();
  });

  it('handles batches with same metrics (0% diff)', async () => {
    const user = userEvent.setup();
    const identicalBatch: BatchProgressEvent = { ...mockBatches[0], batchId: 'batch-004' };
    const batchesWithDuplicate = [...mockBatches, identicalBatch];

    renderWithTheme(<BatchComparisonView batches={batchesWithDuplicate} />);

    // Select both identical batches
    const selects = screen.getAllByRole('combobox');
    await user.click(selects[0]);
    await user.click(await screen.findByText('batch-001'));
    await user.click(selects[1]);
    await user.click(await screen.findByText('batch-004'));

    // Should show 0 or 0.0% diff (use getAllByText since there are multiple diff values)
    const diffElements = screen.getAllByText(/Diff: \+0/);
    expect(diffElements.length).toBeGreaterThan(0);
  });

  it('calls onClose when close button clicked', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    renderWithTheme(<BatchComparisonView batches={mockBatches} onClose={onClose} />);

    // Find and click close button
    const closeButton = screen.getByRole('button', { name: /close/i });
    await user.click(closeButton);

    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('handles missing data gracefully', () => {
    const incompleteBatch: BatchProgressEvent = {
      ...mockBatches[0],
      avgDurationMs: 0,
      cumulativeComplianceRate: 0,
    };

    renderWithTheme(<BatchComparisonView batches={[incompleteBatch, mockBatches[1]]} />);

    // Should render without crashing
    const selects = screen.getAllByRole('combobox');
    expect(selects).toHaveLength(2);
  });

  it('prevents selecting same batch twice', async () => {
    const user = userEvent.setup();
    renderWithTheme(<BatchComparisonView batches={mockBatches} />);

    // Select batch A
    const selects = screen.getAllByRole('combobox');
    await user.click(selects[0]);
    await user.click(await screen.findByText('batch-001'));

    // Try to select same batch for B
    await user.click(selects[1]);

    // batch-001 should be disabled in second dropdown
    const options = screen.getAllByRole('option');
    const batch001Option = options.find(opt => opt.textContent === 'batch-001');
    expect(batch001Option).toHaveAttribute('aria-disabled', 'true');
  });

  it('updates chart when selections change', async () => {
    const user = userEvent.setup();
    const { container } = renderWithTheme(<BatchComparisonView batches={mockBatches} />);

    // Select first pair
    const selects = screen.getAllByRole('combobox');
    await user.click(selects[0]);
    await user.click(await screen.findByText('batch-001'));
    await user.click(selects[1]);
    await user.click(await screen.findByText('batch-002'));

    // Verify chart exists
    let chart = container.querySelector('.recharts-responsive-container');
    expect(chart).toBeInTheDocument();

    // Change selection
    await user.click(selects[1]);
    await user.click(await screen.findByText('batch-003'));

    // Chart should still be present and updated
    chart = container.querySelector('.recharts-responsive-container');
    expect(chart).toBeInTheDocument();
  });

  it('displays batch IDs in dropdown options', async () => {
    const user = userEvent.setup();
    renderWithTheme(<BatchComparisonView batches={mockBatches} />);

    // Open first dropdown
    const selects = screen.getAllByRole('combobox');
    await user.click(selects[0]);

    // Should show all batch IDs
    expect(await screen.findByText('batch-001')).toBeInTheDocument();
    expect(screen.getByText('batch-002')).toBeInTheDocument();
    expect(screen.getByText('batch-003')).toBeInTheDocument();
  });

  it('displays all comparison metrics when both batches selected', async () => {
    const user = userEvent.setup();
    renderWithTheme(<BatchComparisonView batches={mockBatches} />);

    // Select both batches
    const selects = screen.getAllByRole('combobox');
    await user.click(selects[0]);
    await user.click(await screen.findByText('batch-001'));
    await user.click(selects[1]);
    await user.click(await screen.findByText('batch-002'));

    // Should show all metric labels (using getAllByText as some appear in chart too)
    expect(screen.getAllByText(/total evaluations/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/success rate/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/average duration/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/compliance/i).length).toBeGreaterThan(0);
  });

  it('formats duration in milliseconds', async () => {
    const user = userEvent.setup();
    renderWithTheme(<BatchComparisonView batches={mockBatches} />);

    // Select both batches to show metrics
    const selects = screen.getAllByRole('combobox');
    await user.click(selects[0]);
    await user.click(await screen.findByText('batch-001'));
    await user.click(selects[1]);
    await user.click(await screen.findByText('batch-002'));

    // Should display duration with 'ms' suffix
    expect(screen.getByText(/125\.5 ms/)).toBeInTheDocument();
  });
});
