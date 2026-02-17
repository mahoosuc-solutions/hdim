import { describe, expect, it, beforeEach } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { renderWithTheme, screen, waitFor } from '../../test/test-utils';
import { AppShell } from '../AppShell';

describe('AppShell session guard', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('redirects to /login when session expired marker exists without token', async () => {
    localStorage.setItem('sessionExpiredAt', new Date().toISOString());

    renderWithTheme(
      <MemoryRouter initialEntries={['/evaluations']}>
        <Routes>
          <Route path="/login" element={<div>Login Route</div>} />
          <Route path="/" element={<AppShell />}>
            <Route path="evaluations" element={<div>Evaluations Route</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => expect(screen.getByText('Login Route')).toBeInTheDocument());
    expect(screen.queryByText('Evaluations Route')).not.toBeInTheDocument();
  });
});
