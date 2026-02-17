import { describe, expect, it, beforeEach, vi } from 'vitest';
import { renderWithTheme, screen, userEvent } from '../../test/test-utils';
import { ConnectionStatus } from '../ConnectionStatus';

vi.mock('../../app/integrations/useIntegrationsHealth', () => ({
  useIntegrationsHealth: () => ({
    status: 'ok',
    sessionExpired: false,
  }),
}));

describe('ConnectionStatus', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.unstubAllEnvs();
  });

  it('shows and toggles dev Force 401 chip when integrations health is enabled', async () => {
    vi.stubEnv('VITE_ENABLE_INTEGRATIONS_HEALTH_CHECK', 'true');

    const user = userEvent.setup();
    renderWithTheme(<ConnectionStatus />);

    const chip = screen.getByText('Force 401: Off');
    expect(chip).toBeInTheDocument();

    await user.click(chip);

    expect(screen.getByText('Force 401: On')).toBeInTheDocument();
    expect(localStorage.getItem('devForceIntegrations401')).toBe('true');
  });

  it('hides dev Force 401 chip when integrations health is disabled', () => {
    vi.stubEnv('VITE_ENABLE_INTEGRATIONS_HEALTH_CHECK', 'false');

    renderWithTheme(<ConnectionStatus />);
    expect(screen.queryByText(/Force 401:/i)).not.toBeInTheDocument();
  });

  it('restores Force 401 state from localStorage', () => {
    vi.stubEnv('VITE_ENABLE_INTEGRATIONS_HEALTH_CHECK', 'true');
    localStorage.setItem('devForceIntegrations401', 'true');

    renderWithTheme(<ConnectionStatus />);
    expect(screen.getByText('Force 401: On')).toBeInTheDocument();
  });
});
