/**
 * Tests for ExportButton component
 * Following TDD approach - tests written first
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderWithTheme, screen, userEvent, waitFor } from '../../test/test-utils';
import { ExportButton } from '../ExportButton';
import * as exportService from '../../services/export.service';

// Mock the export service
vi.mock('../../services/export.service', () => ({
  exportToCSV: vi.fn(),
  exportToJSON: vi.fn(),
}));

describe('ExportButton', () => {
  const mockData = [
    { id: 1, name: 'John', age: 30 },
    { id: 2, name: 'Jane', age: 25 },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders with CSV and JSON options', async () => {
    const user = userEvent.setup();
    renderWithTheme(<ExportButton data={mockData} />);

    // Click the export button to open menu
    const exportButton = screen.getByRole('button', { name: /export/i });
    expect(exportButton).toBeInTheDocument();

    await user.click(exportButton);

    // Menu should show CSV and JSON options
    expect(screen.getByRole('menuitem', { name: /csv/i })).toBeInTheDocument();
    expect(screen.getByRole('menuitem', { name: /json/i })).toBeInTheDocument();
  });

  it('opens menu on button click', async () => {
    const user = userEvent.setup();
    renderWithTheme(<ExportButton data={mockData} />);

    const exportButton = screen.getByRole('button', { name: /export/i });

    // Menu should not be visible initially
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();

    // Click button
    await user.click(exportButton);

    // Menu should be visible
    expect(screen.getByRole('menu')).toBeInTheDocument();
  });

  it('calls exportToCSV when CSV option clicked', async () => {
    const user = userEvent.setup();
    renderWithTheme(<ExportButton data={mockData} filename="test-export" />);

    // Open menu
    const exportButton = screen.getByRole('button', { name: /export/i });
    await user.click(exportButton);

    // Click CSV option
    const csvOption = screen.getByRole('menuitem', { name: /csv/i });
    await user.click(csvOption);

    // Verify exportToCSV was called with correct arguments
    await waitFor(() => {
      expect(exportService.exportToCSV).toHaveBeenCalledWith(mockData, 'test-export');
    });
  });

  it('calls exportToJSON when JSON option clicked', async () => {
    const user = userEvent.setup();
    renderWithTheme(<ExportButton data={mockData} filename="test-export" />);

    // Open menu
    const exportButton = screen.getByRole('button', { name: /export/i });
    await user.click(exportButton);

    // Click JSON option
    const jsonOption = screen.getByRole('menuitem', { name: /json/i });
    await user.click(jsonOption);

    // Verify exportToJSON was called with correct arguments
    await waitFor(() => {
      expect(exportService.exportToJSON).toHaveBeenCalledWith(mockData, 'test-export');
    });
  });

  it('shows loading state during export', async () => {
    const user = userEvent.setup();

    // Make exportToCSV async with delay
    vi.mocked(exportService.exportToCSV).mockImplementation(() => {
      return new Promise(resolve => setTimeout(resolve, 100)) as any;
    });

    renderWithTheme(<ExportButton data={mockData} />);

    // Open menu and click CSV
    const exportButton = screen.getByRole('button', { name: /export/i });
    await user.click(exportButton);

    const csvOption = screen.getByRole('menuitem', { name: /csv/i });
    await user.click(csvOption);

    // Should show loading indicator
    await waitFor(() => {
      expect(screen.getByRole('progressbar')).toBeInTheDocument();
    });
  });

  it('shows success notification after export', async () => {
    const user = userEvent.setup();
    renderWithTheme(<ExportButton data={mockData} />);

    // Open menu and click CSV
    const exportButton = screen.getByRole('button', { name: /export/i });
    await user.click(exportButton);

    const csvOption = screen.getByRole('menuitem', { name: /csv/i });
    await user.click(csvOption);

    // Should show success message
    await waitFor(() => {
      expect(screen.getByText(/export successful/i)).toBeInTheDocument();
    });
  });

  it('handles errors with error notification', async () => {
    const user = userEvent.setup();

    // Make exportToCSV throw error
    vi.mocked(exportService.exportToCSV).mockImplementation(() => {
      throw new Error('Export failed');
    });

    renderWithTheme(<ExportButton data={mockData} />);

    // Open menu and click CSV
    const exportButton = screen.getByRole('button', { name: /export/i });
    await user.click(exportButton);

    const csvOption = screen.getByRole('menuitem', { name: /csv/i });
    await user.click(csvOption);

    // Should show error message
    await waitFor(() => {
      expect(screen.getByText(/export failed/i)).toBeInTheDocument();
    });
  });

  it('disabled state prevents export', () => {
    renderWithTheme(<ExportButton data={mockData} disabled />);

    const exportButton = screen.getByRole('button', { name: /export/i });

    // Button should be disabled
    expect(exportButton).toBeDisabled();

    // Menu should not open (can't interact with disabled button)
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();

    // Export should not be called
    expect(exportService.exportToCSV).not.toHaveBeenCalled();
    expect(exportService.exportToJSON).not.toHaveBeenCalled();
  });

  it('uses custom filename when provided', async () => {
    const user = userEvent.setup();
    renderWithTheme(<ExportButton data={mockData} filename="custom-name" />);

    // Open menu and click CSV
    const exportButton = screen.getByRole('button', { name: /export/i });
    await user.click(exportButton);

    const csvOption = screen.getByRole('menuitem', { name: /csv/i });
    await user.click(csvOption);

    // Verify custom filename was used
    await waitFor(() => {
      expect(exportService.exportToCSV).toHaveBeenCalledWith(mockData, 'custom-name');
    });
  });

  it('uses default filename when not provided', async () => {
    const user = userEvent.setup();
    renderWithTheme(<ExportButton data={mockData} />);

    // Open menu and click JSON
    const exportButton = screen.getByRole('button', { name: /export/i });
    await user.click(exportButton);

    const jsonOption = screen.getByRole('menuitem', { name: /json/i });
    await user.click(jsonOption);

    // Verify default filename was used
    await waitFor(() => {
      expect(exportService.exportToJSON).toHaveBeenCalledWith(mockData, 'evaluation-export');
    });
  });

  it('displays download icon', () => {
    renderWithTheme(<ExportButton data={mockData} />);

    const exportButton = screen.getByRole('button', { name: /export/i });

    // Button should contain download icon (check for svg with data-testid or class)
    const svg = exportButton.querySelector('svg');
    expect(svg).toBeInTheDocument();
  });

  it('closes menu after successful export', async () => {
    const user = userEvent.setup();
    renderWithTheme(<ExportButton data={mockData} />);

    // Open menu
    const exportButton = screen.getByRole('button', { name: /export/i });
    await user.click(exportButton);

    expect(screen.getByRole('menu')).toBeInTheDocument();

    // Click CSV option
    const csvOption = screen.getByRole('menuitem', { name: /csv/i });
    await user.click(csvOption);

    // Menu should close after export
    await waitFor(() => {
      expect(screen.queryByRole('menu')).not.toBeInTheDocument();
    });
  });

  it('handles empty data array', async () => {
    const user = userEvent.setup();
    renderWithTheme(<ExportButton data={[]} />);

    // Open menu and click CSV
    const exportButton = screen.getByRole('button', { name: /export/i });
    await user.click(exportButton);

    const csvOption = screen.getByRole('menuitem', { name: /csv/i });
    await user.click(csvOption);

    // Should still call export with empty array
    await waitFor(() => {
      expect(exportService.exportToCSV).toHaveBeenCalledWith([], 'evaluation-export');
    });
  });

  it('closes snackbar when user clicks away', async () => {
    const user = userEvent.setup();

    // Reset mocks to ensure clean state
    vi.mocked(exportService.exportToCSV).mockImplementation(() => {});
    vi.mocked(exportService.exportToJSON).mockImplementation(() => {});

    renderWithTheme(<ExportButton data={mockData} />);

    // Trigger success notification
    const exportButton = screen.getByRole('button', { name: /export/i });
    await user.click(exportButton);

    const csvOption = screen.getByRole('menuitem', { name: /csv/i });
    await user.click(csvOption);

    // Wait for success message
    await waitFor(() => {
      expect(screen.getByText(/export successful/i)).toBeInTheDocument();
    });

    // Find and click close button in snackbar
    const closeButton = screen.getByRole('button', { name: /close/i });
    await user.click(closeButton);

    // Snackbar should close
    await waitFor(() => {
      expect(screen.queryByText(/export successful/i)).not.toBeInTheDocument();
    });
  });
});
