import { describe, expect, it, beforeEach, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { renderWithTheme, screen, waitFor } from '../../test/test-utils';
import { AuthCallbackPage } from '../AuthCallbackPage';

describe('AuthCallbackPage', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.unstubAllEnvs();
  });

  it('stores token from query param and redirects to evaluations', async () => {
    renderWithTheme(
      <MemoryRouter initialEntries={['/auth/callback?access_token=test-token-123']}>
        <Routes>
          <Route path="/auth/callback" element={<AuthCallbackPage />} />
          <Route path="/evaluations" element={<div>Evaluations Page</div>} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => expect(screen.getByText('Evaluations Page')).toBeInTheDocument());
    expect(localStorage.getItem('authToken')).toBe('test-token-123');
  });

  it('renders error message when token is missing', async () => {
    renderWithTheme(
      <MemoryRouter initialEntries={['/auth/callback']}>
        <Routes>
          <Route path="/auth/callback" element={<AuthCallbackPage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => expect(screen.getByText(/missing token/i)).toBeInTheDocument());
  });
});
