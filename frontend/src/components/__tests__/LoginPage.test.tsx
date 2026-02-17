import { describe, expect, it, beforeEach, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { renderWithTheme, screen, userEvent } from '../../test/test-utils';
import { LoginPage } from '../LoginPage';

describe('LoginPage', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.unstubAllEnvs();
  });

  it('shows session-expired warning when marker exists', () => {
    localStorage.setItem('sessionExpiredAt', new Date().toISOString());

    renderWithTheme(
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText(/session expired/i)).toBeInTheDocument();
  });

  it('sets auth token and navigates to evaluations', async () => {
    const user = userEvent.setup();
    localStorage.setItem('sessionExpiredAt', new Date().toISOString());

    renderWithTheme(
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/evaluations" element={<div>Evaluations Page</div>} />
        </Routes>
      </MemoryRouter>
    );

    await user.click(screen.getByRole('button', { name: /sign in/i }));

    expect(localStorage.getItem('sessionExpiredAt')).toBeNull();
    expect(localStorage.getItem('authToken')).toMatch(/^dev-session-/);
    expect(screen.getByText('Evaluations Page')).toBeInTheDocument();
  });

  it('redirects to external auth login when configured', async () => {
    const user = userEvent.setup();
    vi.stubEnv('VITE_AUTH_LOGIN_URL', 'https://auth.example.com/login');
    const redirectToAuth = vi.fn();

    renderWithTheme(
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<LoginPage redirectToAuth={redirectToAuth} />} />
        </Routes>
      </MemoryRouter>
    );

    await user.click(screen.getByRole('button', { name: /sign in/i }));

    expect(redirectToAuth).toHaveBeenCalledWith('https://auth.example.com/login');
    expect(localStorage.getItem('authToken')).toBeNull();
  });
});
