import { describe, expect, it, vi } from 'vitest';
import { createSessionExpiryHandler, shouldRequireSessionReauth } from '../sessionExpiry';

describe('createSessionExpiryHandler', () => {
  it('clears auth tokens, marks session expiry, and redirects once', () => {
    const storage: Storage = {
      getItem: vi.fn(),
      setItem: vi.fn(),
      removeItem: vi.fn(),
      clear: vi.fn(),
      key: vi.fn(),
      get length() {
        return 0;
      },
    };

    const redirect = vi.fn();
    const onExpired = vi.fn();
    const handler = createSessionExpiryHandler({
      storage,
      redirect,
      redirectUrl: '/login',
      onExpired,
    });

    expect(handler()).toBe(true);
    expect(handler()).toBe(false);

    expect(storage.removeItem).toHaveBeenCalledWith('authToken');
    expect(storage.removeItem).toHaveBeenCalledWith('refreshToken');
    expect(storage.setItem).toHaveBeenCalledWith('sessionExpiredAt', expect.any(String));
    expect(onExpired).toHaveBeenCalledTimes(1);
    expect(redirect).toHaveBeenCalledTimes(1);
    expect(redirect).toHaveBeenCalledWith('/login');
  });
});

describe('shouldRequireSessionReauth', () => {
  it('returns true when expiry marker exists and token missing', () => {
    localStorage.clear();
    localStorage.setItem('sessionExpiredAt', new Date().toISOString());

    expect(shouldRequireSessionReauth()).toBe(true);
  });

  it('returns false when token exists', () => {
    localStorage.clear();
    localStorage.setItem('sessionExpiredAt', new Date().toISOString());
    localStorage.setItem('authToken', 'token-1');

    expect(shouldRequireSessionReauth()).toBe(false);
  });
});
