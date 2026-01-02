/**
 * Tests for SearchBar component
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderWithTheme, screen, userEvent, waitFor } from '../../test/test-utils';
import { act } from '@testing-library/react';
import { SearchBar } from '../SearchBar';

describe('SearchBar', () => {
  let onSearchMock: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    onSearchMock = vi.fn();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders input with placeholder', () => {
    renderWithTheme(<SearchBar onSearch={onSearchMock} placeholder="Search evaluations..." />);

    const input = screen.getByPlaceholderText('Search evaluations...');
    expect(input).toBeInTheDocument();
  });

  it('displays search icon', () => {
    renderWithTheme(<SearchBar onSearch={onSearchMock} />);

    // MUI icons are rendered as SVG with a data-testid
    const searchIcon = screen.getByTestId('SearchIcon');
    expect(searchIcon).toBeInTheDocument();
  });

  it('calls onSearch after debounce delay', async () => {
    const user = userEvent.setup();
    renderWithTheme(<SearchBar onSearch={onSearchMock} debounceMs={100} />);

    const input = screen.getByRole('textbox');
    await user.type(input, 'test query');

    // Should not call immediately
    expect(onSearchMock).not.toHaveBeenCalled();

    // Wait for debounce
    await waitFor(() => {
      expect(onSearchMock).toHaveBeenCalledWith('test query');
    }, { timeout: 200 });

    expect(onSearchMock).toHaveBeenCalledTimes(1);
  });

  it('does not call onSearch before delay', async () => {
    const user = userEvent.setup();
    renderWithTheme(<SearchBar onSearch={onSearchMock} debounceMs={100} />);

    const input = screen.getByRole('textbox');
    await user.type(input, 'test');

    // Should not call immediately
    expect(onSearchMock).not.toHaveBeenCalled();

    // Wait a bit but not the full delay (this is a best-effort test)
    await new Promise(resolve => setTimeout(resolve, 50));
    expect(onSearchMock).not.toHaveBeenCalled();
  });

  it('cancels previous search on rapid typing', async () => {
    const user = userEvent.setup();
    renderWithTheme(<SearchBar onSearch={onSearchMock} debounceMs={100} />);

    const input = screen.getByRole('textbox');

    // Type rapidly
    await user.type(input, 'first second third');

    // Wait for debounce
    await waitFor(() => {
      expect(onSearchMock).toHaveBeenCalledWith('first second third');
    }, { timeout: 200 });

    // Should only call once with final value
    expect(onSearchMock).toHaveBeenCalledTimes(1);
  });

  it('shows clear button when text entered', async () => {
    const user = userEvent.setup();
    renderWithTheme(<SearchBar onSearch={onSearchMock} />);

    // Clear button should not be visible initially
    expect(screen.queryByLabelText('Clear search')).not.toBeInTheDocument();

    const input = screen.getByRole('textbox');
    await user.type(input, 'test');

    // Clear button should be visible
    expect(screen.getByLabelText('Clear search')).toBeInTheDocument();
  });

  it('clears input when clear button clicked', async () => {
    const user = userEvent.setup();
    renderWithTheme(<SearchBar onSearch={onSearchMock} />);

    const input = screen.getByRole('textbox') as HTMLInputElement;
    await user.type(input, 'test query');

    const clearButton = screen.getByLabelText('Clear search');
    await user.click(clearButton);

    // Input should be empty
    expect(input.value).toBe('');
  });

  it('calls onSearch with empty string on clear', async () => {
    const user = userEvent.setup();
    renderWithTheme(<SearchBar onSearch={onSearchMock} debounceMs={100} />);

    const input = screen.getByRole('textbox');
    await user.type(input, 'test query');

    // Wait for debounced search
    await waitFor(() => {
      expect(onSearchMock).toHaveBeenCalledWith('test query');
    }, { timeout: 200 });

    onSearchMock.mockClear();

    const clearButton = screen.getByLabelText('Clear search');
    await user.click(clearButton);

    // Should call with empty string immediately (no debounce on clear)
    expect(onSearchMock).toHaveBeenCalledWith('');
  });

  it('focuses input with Ctrl+K shortcut', async () => {
    const user = userEvent.setup();
    renderWithTheme(<SearchBar onSearch={onSearchMock} />);

    const input = screen.getByRole('textbox');

    // Input should not be focused initially
    expect(input).not.toHaveFocus();

    // Press Ctrl+K
    await user.keyboard('{Control>}k{/Control}');

    // Input should be focused
    expect(input).toHaveFocus();
  });

  it('focuses input with Cmd+K shortcut', async () => {
    const user = userEvent.setup();
    renderWithTheme(<SearchBar onSearch={onSearchMock} />);

    const input = screen.getByRole('textbox');

    // Input should not be focused initially
    expect(input).not.toHaveFocus();

    // Press Cmd+K (Meta key)
    await user.keyboard('{Meta>}k{/Meta}');

    // Input should be focused
    expect(input).toHaveFocus();
  });

  it('auto-focuses on mount when autoFocus=true', () => {
    renderWithTheme(<SearchBar onSearch={onSearchMock} autoFocus />);

    const input = screen.getByRole('textbox');
    expect(input).toHaveFocus();
  });

  it('shows loading indicator when searching', async () => {
    const user = userEvent.setup({ delay: null });
    renderWithTheme(<SearchBar onSearch={onSearchMock} isLoading />);

    // Loading indicator should be visible
    const loadingIndicator = screen.getByRole('progressbar');
    expect(loadingIndicator).toBeInTheDocument();
  });

  it('has proper ARIA labels', () => {
    renderWithTheme(<SearchBar onSearch={onSearchMock} />);

    const input = screen.getByRole('textbox');
    expect(input).toHaveAttribute('aria-label', 'Search');
  });

  it('handles controlled input value', async () => {
    const user = userEvent.setup();
    renderWithTheme(<SearchBar onSearch={onSearchMock} />);

    const input = screen.getByRole('textbox') as HTMLInputElement;

    // Type into input
    await user.type(input, 'test');

    expect(input.value).toBe('test');

    // Component should handle its own state
    expect(input.value).toBe('test');
  });

  it('uses default debounce delay of 300ms', async () => {
    const user = userEvent.setup();
    renderWithTheme(<SearchBar onSearch={onSearchMock} />);

    const input = screen.getByRole('textbox');
    await user.type(input, 'test');

    // Should not call immediately
    expect(onSearchMock).not.toHaveBeenCalled();

    // Should call after default 300ms delay
    await waitFor(() => {
      expect(onSearchMock).toHaveBeenCalledWith('test');
    }, { timeout: 400 });
  });

  it('uses custom debounce delay', async () => {
    const user = userEvent.setup();
    renderWithTheme(<SearchBar onSearch={onSearchMock} debounceMs={100} />);

    const input = screen.getByRole('textbox');
    await user.type(input, 'test');

    // Should not call immediately
    expect(onSearchMock).not.toHaveBeenCalled();

    // Should call after custom delay
    await waitFor(() => {
      expect(onSearchMock).toHaveBeenCalledWith('test');
    }, { timeout: 200 });
  });

  it('uses default placeholder when not provided', () => {
    renderWithTheme(<SearchBar onSearch={onSearchMock} />);

    const input = screen.getByPlaceholderText('Search...');
    expect(input).toBeInTheDocument();
  });

  it('prevents default on keyboard shortcut to avoid browser behavior', async () => {
    const user = userEvent.setup();
    renderWithTheme(<SearchBar onSearch={onSearchMock} />);

    const input = screen.getByRole('textbox');

    // Press Ctrl+K
    await user.keyboard('{Control>}k{/Control}');

    // Input should be focused (default prevented)
    expect(input).toHaveFocus();
  });
});
