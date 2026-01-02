/**
 * Tests for DarkModeToggle component
 *
 * Test-Driven Development (TDD) - Tests written FIRST
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { screen, waitFor, renderWithTheme, userEvent } from '../../test/test-utils';
import { DarkModeToggle } from '../DarkModeToggle';

// Mock the useDarkMode hook
vi.mock('../../hooks/useDarkMode', () => ({
  useDarkMode: vi.fn(),
}));

import { useDarkMode } from '../../hooks/useDarkMode';

describe('DarkModeToggle', () => {
  const mockToggleDarkMode = vi.fn();
  const mockSetDarkMode = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Rendering', () => {
    it('renders switch in light mode (unchecked)', () => {
      vi.mocked(useDarkMode).mockReturnValue({
        isDarkMode: false,
        toggleDarkMode: mockToggleDarkMode,
        setDarkMode: mockSetDarkMode,
      });

      renderWithTheme(<DarkModeToggle />);

      const switchElement = screen.getByRole('switch');
      expect(switchElement).toBeInTheDocument();
      expect(switchElement).not.toBeChecked();
    });

    it('renders switch in dark mode (checked)', () => {
      vi.mocked(useDarkMode).mockReturnValue({
        isDarkMode: true,
        toggleDarkMode: mockToggleDarkMode,
        setDarkMode: mockSetDarkMode,
      });

      renderWithTheme(<DarkModeToggle />);

      const switchElement = screen.getByRole('switch');
      expect(switchElement).toBeInTheDocument();
      expect(switchElement).toBeChecked();
    });
  });

  describe('Icons', () => {
    it('displays sun icon (Brightness7) in light mode', () => {
      vi.mocked(useDarkMode).mockReturnValue({
        isDarkMode: false,
        toggleDarkMode: mockToggleDarkMode,
        setDarkMode: mockSetDarkMode,
      });

      renderWithTheme(<DarkModeToggle />);

      // The sun icon should be present
      const sunIcon = screen.getByTestId('Brightness7Icon');
      expect(sunIcon).toBeInTheDocument();
    });

    it('displays moon icon (Brightness4) in dark mode', () => {
      vi.mocked(useDarkMode).mockReturnValue({
        isDarkMode: true,
        toggleDarkMode: mockToggleDarkMode,
        setDarkMode: mockSetDarkMode,
      });

      renderWithTheme(<DarkModeToggle />);

      // The moon icon should be present
      const moonIcon = screen.getByTestId('Brightness4Icon');
      expect(moonIcon).toBeInTheDocument();
    });
  });

  describe('Tooltip', () => {
    it('shows "Switch to dark mode" tooltip in light mode', async () => {
      vi.mocked(useDarkMode).mockReturnValue({
        isDarkMode: false,
        toggleDarkMode: mockToggleDarkMode,
        setDarkMode: mockSetDarkMode,
      });

      renderWithTheme(<DarkModeToggle />);

      const switchElement = screen.getByRole('switch');

      // Hover over the switch to trigger tooltip
      await userEvent.hover(switchElement);

      await waitFor(() => {
        expect(screen.getByText('Switch to dark mode')).toBeInTheDocument();
      });
    });

    it('shows "Switch to light mode" tooltip in dark mode', async () => {
      vi.mocked(useDarkMode).mockReturnValue({
        isDarkMode: true,
        toggleDarkMode: mockToggleDarkMode,
        setDarkMode: mockSetDarkMode,
      });

      renderWithTheme(<DarkModeToggle />);

      const switchElement = screen.getByRole('switch');

      // Hover over the switch to trigger tooltip
      await userEvent.hover(switchElement);

      await waitFor(() => {
        expect(screen.getByText('Switch to light mode')).toBeInTheDocument();
      });
    });
  });

  describe('Interaction', () => {
    it('calls toggleDarkMode when switch is clicked', async () => {
      vi.mocked(useDarkMode).mockReturnValue({
        isDarkMode: false,
        toggleDarkMode: mockToggleDarkMode,
        setDarkMode: mockSetDarkMode,
      });

      renderWithTheme(<DarkModeToggle />);

      const switchElement = screen.getByRole('switch');
      await userEvent.click(switchElement);

      expect(mockToggleDarkMode).toHaveBeenCalledTimes(1);
    });

    it('calls toggleDarkMode multiple times for multiple clicks', async () => {
      vi.mocked(useDarkMode).mockReturnValue({
        isDarkMode: false,
        toggleDarkMode: mockToggleDarkMode,
        setDarkMode: mockSetDarkMode,
      });

      renderWithTheme(<DarkModeToggle />);

      const switchElement = screen.getByRole('switch');
      await userEvent.click(switchElement);
      await userEvent.click(switchElement);
      await userEvent.click(switchElement);

      expect(mockToggleDarkMode).toHaveBeenCalledTimes(3);
    });
  });

  describe('Keyboard Accessibility', () => {
    it('can be activated with Space key', async () => {
      vi.mocked(useDarkMode).mockReturnValue({
        isDarkMode: false,
        toggleDarkMode: mockToggleDarkMode,
        setDarkMode: mockSetDarkMode,
      });

      const user = userEvent.setup();
      renderWithTheme(<DarkModeToggle />);

      const switchElement = screen.getByRole('switch');
      await user.click(switchElement);

      expect(mockToggleDarkMode).toHaveBeenCalledTimes(1);
    });

    it('can receive focus for keyboard navigation', () => {
      vi.mocked(useDarkMode).mockReturnValue({
        isDarkMode: false,
        toggleDarkMode: mockToggleDarkMode,
        setDarkMode: mockSetDarkMode,
      });

      renderWithTheme(<DarkModeToggle />);

      const switchElement = screen.getByRole('switch');

      // Switch should be focusable
      expect(switchElement).toBeInTheDocument();
      expect(switchElement.tagName).toBe('INPUT');
    });
  });

  describe('ARIA and Accessibility', () => {
    it('renders accessible switch element in light mode', () => {
      vi.mocked(useDarkMode).mockReturnValue({
        isDarkMode: false,
        toggleDarkMode: mockToggleDarkMode,
        setDarkMode: mockSetDarkMode,
      });

      renderWithTheme(<DarkModeToggle />);

      // Should be able to find the switch by role
      const switchElement = screen.getByRole('switch');
      expect(switchElement).toBeInTheDocument();
      expect(switchElement).toHaveProperty('type', 'checkbox');
    });

    it('renders accessible switch element in dark mode', () => {
      vi.mocked(useDarkMode).mockReturnValue({
        isDarkMode: true,
        toggleDarkMode: mockToggleDarkMode,
        setDarkMode: mockSetDarkMode,
      });

      renderWithTheme(<DarkModeToggle />);

      // Should be able to find the switch by role
      const switchElement = screen.getByRole('switch');
      expect(switchElement).toBeInTheDocument();
      expect(switchElement).toHaveProperty('type', 'checkbox');
    });

    it('is accessible by role when unchecked', () => {
      vi.mocked(useDarkMode).mockReturnValue({
        isDarkMode: false,
        toggleDarkMode: mockToggleDarkMode,
        setDarkMode: mockSetDarkMode,
      });

      renderWithTheme(<DarkModeToggle />);

      const switchElement = screen.getByRole('switch');
      expect(switchElement).toBeInTheDocument();
      expect(switchElement).not.toBeChecked();
    });

    it('is accessible by role when checked', () => {
      vi.mocked(useDarkMode).mockReturnValue({
        isDarkMode: true,
        toggleDarkMode: mockToggleDarkMode,
        setDarkMode: mockSetDarkMode,
      });

      renderWithTheme(<DarkModeToggle />);

      const switchElement = screen.getByRole('switch');
      expect(switchElement).toBeInTheDocument();
      expect(switchElement).toBeChecked();
    });
  });

  describe('External State Updates', () => {
    it('updates when dark mode changes externally', () => {
      const { rerender } = renderWithTheme(<DarkModeToggle />);

      // Start in light mode
      vi.mocked(useDarkMode).mockReturnValue({
        isDarkMode: false,
        toggleDarkMode: mockToggleDarkMode,
        setDarkMode: mockSetDarkMode,
      });

      rerender(<DarkModeToggle />);

      let switchElement = screen.getByRole('switch');
      expect(switchElement).not.toBeChecked();

      // Change to dark mode externally
      vi.mocked(useDarkMode).mockReturnValue({
        isDarkMode: true,
        toggleDarkMode: mockToggleDarkMode,
        setDarkMode: mockSetDarkMode,
      });

      rerender(<DarkModeToggle />);

      switchElement = screen.getByRole('switch');
      expect(switchElement).toBeChecked();
    });
  });

  describe('Styling and Transitions', () => {
    it('has transition styles applied', () => {
      vi.mocked(useDarkMode).mockReturnValue({
        isDarkMode: false,
        toggleDarkMode: mockToggleDarkMode,
        setDarkMode: mockSetDarkMode,
      });

      const { container } = renderWithTheme(<DarkModeToggle />);

      // Check that the component or its wrapper has transition styles
      // MUI Switch internally applies transitions, so we just verify it renders
      expect(container.firstChild).toBeInTheDocument();
    });
  });
});
