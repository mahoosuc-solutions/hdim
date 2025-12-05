import { renderWithTheme, screen, userEvent } from '../../test/test-utils';
import { SettingsPanel } from '../SettingsPanel';
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('SettingsPanel', () => {
  const defaultProps = {
    open: true,
    onClose: vi.fn(),
  };

  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('renders when open', () => {
    renderWithTheme(<SettingsPanel {...defaultProps} />);
    expect(screen.getAllByRole('dialog')[0]).toBeInTheDocument();
    expect(screen.getByText('Settings')).toBeInTheDocument();
  });

  it('does not render when closed', () => {
    renderWithTheme(<SettingsPanel {...defaultProps} open={false} />);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('theme selector shows all options', async () => {
    const user = userEvent.setup();
    renderWithTheme(<SettingsPanel {...defaultProps} />);

    const themeSelect = screen.getByLabelText(/theme preference/i);
    expect(themeSelect).toBeInTheDocument();

    // Click to open the select
    await user.click(themeSelect);

    const options = await screen.findAllByRole('option');
    expect(options).toHaveLength(3);
    expect(options[0]).toHaveTextContent('Auto');
    expect(options[1]).toHaveTextContent('Light');
    expect(options[2]).toHaveTextContent('Dark');
  });

  it('theme selection updates local state', async () => {
    const user = userEvent.setup();
    renderWithTheme(<SettingsPanel {...defaultProps} />);

    const themeSelect = screen.getByLabelText(/theme preference/i);
    await user.click(themeSelect);

    const darkOption = screen.getByText('Dark');
    await user.click(darkOption);

    expect(themeSelect).toHaveTextContent('Dark');
  });

  it('debounce slider updates value', async () => {
    const user = userEvent.setup();
    renderWithTheme(<SettingsPanel {...defaultProps} />);

    const slider = screen.getByRole('slider', { name: /search debounce/i });
    expect(slider).toBeInTheDocument();

    // Slider should be changeable (implementation will update value)
    await user.click(slider);
    expect(slider).toHaveAttribute('aria-valuenow');
  });

  it('debounce slider shows current value in range 100-1000ms', () => {
    renderWithTheme(<SettingsPanel {...defaultProps} />);

    const slider = screen.getByRole('slider', { name: /search debounce/i });
    const currentValue = parseInt(slider.getAttribute('aria-valuenow') || '0');

    expect(currentValue).toBeGreaterThanOrEqual(100);
    expect(currentValue).toBeLessThanOrEqual(1000);
    expect(screen.getByText(/Search debounce delay: 300ms/i)).toBeInTheDocument(); // Default value display
  });

  it('notifications toggle updates state', async () => {
    const user = userEvent.setup();
    renderWithTheme(<SettingsPanel {...defaultProps} />);

    const notificationToggle = screen.getByLabelText(/enable notifications/i);
    expect(notificationToggle).not.toBeChecked();

    await user.click(notificationToggle);
    expect(notificationToggle).toBeChecked();
  });

  it('save button persists settings to localStorage', async () => {
    const user = userEvent.setup();
    renderWithTheme(<SettingsPanel {...defaultProps} />);

    // Change theme
    const themeSelect = screen.getByLabelText(/theme preference/i);
    await user.click(themeSelect);
    await user.click(screen.getByRole('option', { name: /dark/i }));

    // Enable notifications
    const notificationToggle = screen.getByLabelText(/enable notifications/i);
    await user.click(notificationToggle);

    // Save
    const saveButton = screen.getByRole('button', { name: /save/i });
    await user.click(saveButton);

    const savedSettings = JSON.parse(localStorage.getItem('userSettings') || '{}');
    expect(savedSettings.theme).toBe('dark');
    expect(savedSettings.notificationsEnabled).toBe(true);
  });

  it('cancel button reverts changes', async () => {
    const user = userEvent.setup();
    renderWithTheme(<SettingsPanel {...defaultProps} />);

    // Change theme
    const themeSelect = screen.getByLabelText(/theme preference/i);
    await user.click(themeSelect);
    await user.click(screen.getByText('Dark'));

    expect(themeSelect).toHaveTextContent('Dark');

    // Cancel
    const cancelButton = screen.getByRole('button', { name: /cancel/i });
    await user.click(cancelButton);

    // Should revert to default (Auto)
    expect(themeSelect).toHaveTextContent('Auto');
  });

  it('cancel button calls onClose', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    renderWithTheme(<SettingsPanel {...defaultProps} onClose={onClose} />);

    const cancelButton = screen.getByRole('button', { name: /cancel/i });
    await user.click(cancelButton);

    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('reset to defaults shows confirmation dialog', async () => {
    const user = userEvent.setup();
    renderWithTheme(<SettingsPanel {...defaultProps} />);

    const resetButton = screen.getByRole('button', { name: /reset to defaults/i });
    await user.click(resetButton);

    expect(screen.getByText(/are you sure/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /confirm/i })).toBeInTheDocument();
  });

  it('reset to defaults resets all settings', async () => {
    const user = userEvent.setup();

    // Set some custom settings
    localStorage.setItem('userSettings', JSON.stringify({
      theme: 'dark',
      searchDebounceMs: 1000,
      notificationsEnabled: true,
    }));

    renderWithTheme(<SettingsPanel {...defaultProps} />);

    // Verify custom settings loaded
    expect(screen.getByLabelText(/theme preference/i)).toHaveTextContent('Dark');

    // Reset
    const resetButton = screen.getByRole('button', { name: /reset to defaults/i });
    await user.click(resetButton);

    const confirmButton = screen.getByRole('button', { name: /confirm/i });
    await user.click(confirmButton);

    // Should show defaults
    expect(screen.getByLabelText(/theme preference/i)).toHaveTextContent('Auto');
    expect(screen.getByLabelText(/enable notifications/i)).not.toBeChecked();
  });

  it('unsaved changes indicator visible when modified', async () => {
    const user = userEvent.setup();
    renderWithTheme(<SettingsPanel {...defaultProps} />);

    // Initially no unsaved changes indicator
    expect(screen.queryByText(/unsaved changes/i)).not.toBeInTheDocument();

    // Change a setting
    const notificationToggle = screen.getByLabelText(/enable notifications/i);
    await user.click(notificationToggle);

    // Should show unsaved changes indicator
    expect(screen.getByText(/unsaved changes/i)).toBeInTheDocument();
  });

  it('settings load from localStorage on mount', () => {
    localStorage.setItem('userSettings', JSON.stringify({
      theme: 'light',
      searchDebounceMs: 500,
      notificationsEnabled: true,
    }));

    renderWithTheme(<SettingsPanel {...defaultProps} />);

    expect(screen.getByLabelText(/theme preference/i)).toHaveTextContent('Light');
    expect(screen.getByLabelText(/enable notifications/i)).toBeChecked();
    expect(screen.getByText(/Search debounce delay: 500ms/i)).toBeInTheDocument();
  });

  it('invalid localStorage data uses defaults', () => {
    localStorage.setItem('userSettings', 'invalid json');

    renderWithTheme(<SettingsPanel {...defaultProps} />);

    // Should use defaults
    expect(screen.getByLabelText(/theme preference/i)).toHaveTextContent('Auto');
    expect(screen.getByLabelText(/enable notifications/i)).not.toBeChecked();
  });

  it('close button calls onClose', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    renderWithTheme(<SettingsPanel {...defaultProps} onClose={onClose} />);

    const closeButton = screen.getByLabelText(/close/i);
    await user.click(closeButton);

    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it('settings sections have proper headers', () => {
    renderWithTheme(<SettingsPanel {...defaultProps} />);

    expect(screen.getByText('Appearance')).toBeInTheDocument();
    expect(screen.getByText('Search')).toBeInTheDocument();
    expect(screen.getByText('Notifications')).toBeInTheDocument();
    expect(screen.getByText('Data')).toBeInTheDocument();
  });

  it('live preview works - updates immediately in UI', async () => {
    const user = userEvent.setup();
    renderWithTheme(<SettingsPanel {...defaultProps} />);

    // Get initial theme
    const themeSelect = screen.getByLabelText(/theme preference/i);
    expect(themeSelect).toHaveTextContent('Auto');

    // Change theme
    await user.click(themeSelect);
    await user.click(screen.getByText('Light'));

    // Should update immediately (live preview)
    expect(themeSelect).toHaveTextContent('Light');

    // But not saved to localStorage yet
    expect(localStorage.getItem('userSettings')).toBeNull();
  });
});
