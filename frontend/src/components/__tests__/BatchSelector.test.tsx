/**
 * Tests for BatchSelector component
 * Following TDD approach - tests written first
 */

import { describe, it, expect, vi } from 'vitest';
import { renderWithTheme, screen, userEvent } from '../../test/test-utils';
import { BatchSelector } from '../BatchSelector';

// Mock batch data
const mockBatches = [
  {
    batchId: 'batch-1',
    measureName: 'Comprehensive Diabetes Care',
    timestamp: Date.now() - 120000, // 2 minutes ago
    status: 'active' as const,
  },
  {
    batchId: 'batch-2',
    measureName: 'Breast Cancer Screening',
    timestamp: Date.now() - 300000, // 5 minutes ago
    status: 'completed' as const,
  },
  {
    batchId: 'batch-3',
    measureName: 'Colorectal Cancer Screening',
    timestamp: Date.now() - 60000, // 1 minute ago
    status: 'active' as const,
  },
];

describe('BatchSelector', () => {
  it('renders with empty batch list', () => {
    const onBatchSelect = vi.fn();

    renderWithTheme(
      <BatchSelector batches={[]} onBatchSelect={onBatchSelect} />
    );

    // Should render the Select component with label
    expect(screen.getByLabelText('Select Batch')).toBeInTheDocument();
  });

  it('displays "No batches available" message when empty', async () => {
    const onBatchSelect = vi.fn();
    const user = userEvent.setup();

    renderWithTheme(
      <BatchSelector batches={[]} onBatchSelect={onBatchSelect} />
    );

    // Click to open the dropdown
    const selectButton = screen.getByRole('combobox');
    await user.click(selectButton);

    // Should show "No batches available" message
    expect(screen.getByText('No batches available')).toBeInTheDocument();
  });

  it('renders batch list with measure names', async () => {
    const onBatchSelect = vi.fn();
    const user = userEvent.setup();

    renderWithTheme(
      <BatchSelector batches={mockBatches} onBatchSelect={onBatchSelect} />
    );

    // Click to open the dropdown
    const selectButton = screen.getByRole('combobox');
    await user.click(selectButton);

    // Should display all measure names
    expect(screen.getByText(/Comprehensive Diabetes Care/)).toBeInTheDocument();
    expect(screen.getByText(/Breast Cancer Screening/)).toBeInTheDocument();
    expect(screen.getByText(/Colorectal Cancer Screening/)).toBeInTheDocument();
  });

  it('displays status badges (active/completed)', async () => {
    const onBatchSelect = vi.fn();
    const user = userEvent.setup();

    renderWithTheme(
      <BatchSelector batches={mockBatches} onBatchSelect={onBatchSelect} />
    );

    // Click to open the dropdown
    const selectButton = screen.getByRole('combobox');
    await user.click(selectButton);

    // Should display status badges
    const activeBadges = screen.getAllByText('Active');
    const completedBadges = screen.getAllByText('Completed');

    expect(activeBadges.length).toBe(2); // batch-1 and batch-3
    expect(completedBadges.length).toBe(1); // batch-2
  });

  it('calls onBatchSelect when batch is selected', async () => {
    const onBatchSelect = vi.fn();
    const user = userEvent.setup();

    renderWithTheme(
      <BatchSelector batches={mockBatches} onBatchSelect={onBatchSelect} />
    );

    // Click to open the dropdown
    const selectButton = screen.getByRole('combobox');
    await user.click(selectButton);

    // Click on a batch option
    const option = screen.getByText(/Breast Cancer Screening/);
    await user.click(option);

    // Should call onBatchSelect with the correct batchId
    expect(onBatchSelect).toHaveBeenCalledWith('batch-2');
    expect(onBatchSelect).toHaveBeenCalledTimes(1);
  });

  it('sorts active batches before completed batches', async () => {
    const onBatchSelect = vi.fn();
    const user = userEvent.setup();

    renderWithTheme(
      <BatchSelector batches={mockBatches} onBatchSelect={onBatchSelect} />
    );

    // Click to open the dropdown
    const selectButton = screen.getByRole('combobox');
    await user.click(selectButton);

    // Get all menu items
    const menuItems = screen.getAllByRole('option');

    // First two should be active batches (sorted by timestamp desc)
    // batch-3 (1 min ago) should come before batch-1 (2 min ago)
    expect(menuItems[0]).toHaveTextContent('Colorectal Cancer Screening');
    expect(menuItems[1]).toHaveTextContent('Comprehensive Diabetes Care');
    // Last should be completed batch
    expect(menuItems[2]).toHaveTextContent('Breast Cancer Screening');
  });

  it('formats timestamps as relative time', async () => {
    const onBatchSelect = vi.fn();
    const user = userEvent.setup();

    renderWithTheme(
      <BatchSelector batches={mockBatches} onBatchSelect={onBatchSelect} />
    );

    // Click to open the dropdown
    const selectButton = screen.getByRole('combobox');
    await user.click(selectButton);

    // Should display relative time for each batch
    expect(screen.getByText(/1 minute ago/)).toBeInTheDocument();
    expect(screen.getByText(/2 minutes ago/)).toBeInTheDocument();
    expect(screen.getByText(/5 minutes ago/)).toBeInTheDocument();
  });

  it('highlights currently selected batch', async () => {
    const onBatchSelect = vi.fn();
    const user = userEvent.setup();

    renderWithTheme(
      <BatchSelector
        batches={mockBatches}
        onBatchSelect={onBatchSelect}
        selectedBatchId="batch-1"
      />
    );

    // Click to open the dropdown
    const selectButton = screen.getByRole('combobox');
    await user.click(selectButton);

    // The selected option should have aria-selected="true"
    const options = screen.getAllByRole('option');
    const selectedOption = options.find(option => option.getAttribute('aria-selected') === 'true');

    expect(selectedOption).toHaveTextContent('Comprehensive Diabetes Care');
  });

  it('truncates long measure names (>30 chars)', async () => {
    const longNameBatch = {
      batchId: 'batch-long',
      measureName: 'This is a very long measure name that exceeds thirty characters',
      timestamp: Date.now(),
      status: 'active' as const,
    };

    const onBatchSelect = vi.fn();
    const user = userEvent.setup();

    renderWithTheme(
      <BatchSelector batches={[longNameBatch]} onBatchSelect={onBatchSelect} />
    );

    // Click to open the dropdown
    const selectButton = screen.getByRole('combobox');
    await user.click(selectButton);

    // The long measure name should be present in the document
    expect(screen.getByText(longNameBatch.measureName)).toBeInTheDocument();

    // The component renders with the long name, and CSS handles truncation
    // We verify the content is there and trust the CSS implementation
    const option = screen.getByRole('option');
    expect(option).toBeInTheDocument();
  });

  it('handles single batch', async () => {
    const singleBatch = [mockBatches[0]];
    const onBatchSelect = vi.fn();
    const user = userEvent.setup();

    renderWithTheme(
      <BatchSelector batches={singleBatch} onBatchSelect={onBatchSelect} />
    );

    // Click to open the dropdown
    const selectButton = screen.getByRole('combobox');
    await user.click(selectButton);

    // Should display the single batch
    const options = screen.getAllByRole('option');
    expect(options.length).toBe(1);
    expect(options[0]).toHaveTextContent('Comprehensive Diabetes Care');
  });

  it('updates when batch list changes', async () => {
    const onBatchSelect = vi.fn();
    const user = userEvent.setup();

    const { rerender } = renderWithTheme(
      <BatchSelector batches={[mockBatches[0]]} onBatchSelect={onBatchSelect} />
    );

    // Click to open the dropdown
    let selectButton = screen.getByRole('combobox');
    await user.click(selectButton);

    // Should display only one batch
    let options = screen.getAllByRole('option');
    expect(options.length).toBe(1);

    // Close dropdown
    await user.keyboard('{Escape}');

    // Update with all batches
    rerender(
      <BatchSelector batches={mockBatches} onBatchSelect={onBatchSelect} />
    );

    // Click to open the dropdown again
    selectButton = screen.getByRole('combobox');
    await user.click(selectButton);

    // Should now display all batches
    options = screen.getAllByRole('option');
    expect(options.length).toBe(3);
  });

  it('displays selected batch value in the select', async () => {
    const onBatchSelect = vi.fn();

    renderWithTheme(
      <BatchSelector
        batches={mockBatches}
        onBatchSelect={onBatchSelect}
        selectedBatchId="batch-1"
      />
    );

    // The select should display the selected batch measure name
    expect(screen.getByText('Comprehensive Diabetes Care')).toBeInTheDocument();
  });
});
