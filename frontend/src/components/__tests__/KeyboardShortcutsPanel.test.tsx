import { describe, it, expect, beforeEach, vi } from 'vitest';
import { renderWithTheme, screen, userEvent, waitFor } from '../../test/test-utils';
import { KeyboardShortcutsPanel } from '../KeyboardShortcutsPanel';

describe('KeyboardShortcutsPanel', () => {
  const mockOnClose = vi.fn();

  beforeEach(() => {
    mockOnClose.mockClear();
  });

  it('renders when open is true', () => {
    renderWithTheme(<KeyboardShortcutsPanel open={true} onClose={mockOnClose} />);

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByText('Keyboard Shortcuts')).toBeInTheDocument();
  });

  it('does not render when open is false', () => {
    renderWithTheme(<KeyboardShortcutsPanel open={false} onClose={mockOnClose} />);

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('displays all shortcut categories', () => {
    renderWithTheme(<KeyboardShortcutsPanel open={true} onClose={mockOnClose} />);

    expect(screen.getByText('Search & Navigation')).toBeInTheDocument();
    expect(screen.getByText('Theme')).toBeInTheDocument();
    expect(screen.getByText('Help')).toBeInTheDocument();
    expect(screen.getByText('Modal')).toBeInTheDocument();
  });

  it('displays Ctrl+K shortcut with description', () => {
    renderWithTheme(<KeyboardShortcutsPanel open={true} onClose={mockOnClose} />);

    // Multiple Ctrl keys exist, so use getAllByText
    const ctrlKeys = screen.getAllByText('Ctrl');
    expect(ctrlKeys.length).toBeGreaterThan(0);
    expect(screen.getByText('K')).toBeInTheDocument();
    expect(screen.getByText('Focus search')).toBeInTheDocument();
  });

  it('displays Ctrl+? shortcut with description', () => {
    renderWithTheme(<KeyboardShortcutsPanel open={true} onClose={mockOnClose} />);

    expect(screen.getByText('?')).toBeInTheDocument();
    expect(screen.getByText('Open keyboard shortcuts')).toBeInTheDocument();
  });

  it('displays ESC shortcut with description', () => {
    renderWithTheme(<KeyboardShortcutsPanel open={true} onClose={mockOnClose} />);

    expect(screen.getByText('ESC')).toBeInTheDocument();
    expect(screen.getByText('Close any modal')).toBeInTheDocument();
  });

  it('renders keyboard keys as visual chips', () => {
    renderWithTheme(<KeyboardShortcutsPanel open={true} onClose={mockOnClose} />);

    // Check that chips exist for keyboard keys by verifying their presence
    const ctrlKeys = screen.getAllByText('Ctrl');
    const kKey = screen.getByText('K');
    const escKey = screen.getByText('ESC');

    // All keys should be in the document
    expect(ctrlKeys.length).toBeGreaterThan(0);
    expect(kKey).toBeInTheDocument();
    expect(escKey).toBeInTheDocument();
  });

  it('calls onClose when close button is clicked', async () => {
    const user = userEvent.setup();
    renderWithTheme(<KeyboardShortcutsPanel open={true} onClose={mockOnClose} />);

    const closeButton = screen.getByLabelText(/close/i);
    await user.click(closeButton);

    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });

  it('calls onClose when ESC key is pressed', async () => {
    renderWithTheme(<KeyboardShortcutsPanel open={true} onClose={mockOnClose} />);

    const dialog = screen.getByRole('dialog');
    await userEvent.keyboard('{Escape}');

    await waitFor(() => {
      expect(mockOnClose).toHaveBeenCalledTimes(1);
    });
  });

  it('filters shortcuts based on search input', async () => {
    const user = userEvent.setup();
    renderWithTheme(<KeyboardShortcutsPanel open={true} onClose={mockOnClose} />);

    const searchInput = screen.getByPlaceholderText(/search shortcuts/i);
    await user.type(searchInput, 'search');

    // Should show "Focus search" shortcut
    expect(screen.getByText('Focus search')).toBeInTheDocument();

    // Should not show "Close any modal"
    expect(screen.queryByText('Close any modal')).not.toBeInTheDocument();
  });

  it('shows all shortcuts when search is empty', async () => {
    const user = userEvent.setup();
    renderWithTheme(<KeyboardShortcutsPanel open={true} onClose={mockOnClose} />);

    const searchInput = screen.getByPlaceholderText(/search shortcuts/i);
    await user.type(searchInput, 'test');
    await user.clear(searchInput);

    // All shortcuts should be visible
    expect(screen.getByText('Focus search')).toBeInTheDocument();
    expect(screen.getByText('Close any modal')).toBeInTheDocument();
    expect(screen.getByText('Open keyboard shortcuts')).toBeInTheDocument();
  });

  it('displays both Ctrl and Cmd variations for Mac compatibility', () => {
    renderWithTheme(<KeyboardShortcutsPanel open={true} onClose={mockOnClose} />);

    // Should show Ctrl (or Cmd on Mac)
    const ctrlOrCmd = screen.getAllByText(/Ctrl|Cmd/);
    expect(ctrlOrCmd.length).toBeGreaterThan(0);
  });

  it('displays category headers for organization', () => {
    renderWithTheme(<KeyboardShortcutsPanel open={true} onClose={mockOnClose} />);

    // Category headers should be present
    const searchNav = screen.getByText('Search & Navigation');
    const theme = screen.getByText('Theme');
    const help = screen.getByText('Help');
    const modal = screen.getByText('Modal');

    expect(searchNav).toBeInTheDocument();
    expect(theme).toBeInTheDocument();
    expect(help).toBeInTheDocument();
    expect(modal).toBeInTheDocument();
  });

  it('clears search when clear button is clicked', async () => {
    const user = userEvent.setup();
    renderWithTheme(<KeyboardShortcutsPanel open={true} onClose={mockOnClose} />);

    const searchInput = screen.getByPlaceholderText(/search shortcuts/i) as HTMLInputElement;
    await user.type(searchInput, 'search');

    expect(searchInput.value).toBe('search');

    // Find and click clear button (InputAdornment icon button)
    const clearButton = screen.getByLabelText(/clear search/i);
    await user.click(clearButton);

    expect(searchInput.value).toBe('');
  });
});
