import { describe, expect, it, vi } from 'vitest';
import { ApiClient, SessionExpiredError } from '../client';

describe('ApiClient', () => {
  it('throws SessionExpiredError and triggers unauthorized callback once', async () => {
    const onUnauthorized = vi.fn();
    const fetchImpl = vi
      .fn()
      .mockResolvedValueOnce(new Response('expired', { status: 401, statusText: 'Unauthorized' }))
      .mockResolvedValueOnce(new Response('still expired', { status: 401, statusText: 'Unauthorized' }));

    const client = new ApiClient({
      baseUrl: 'http://localhost:5173',
      onUnauthorized,
      fetchImpl,
    });

    await expect(client.get('/health/integrations')).rejects.toBeInstanceOf(SessionExpiredError);
    await expect(client.get('/health/integrations')).rejects.toBeInstanceOf(SessionExpiredError);
    expect(onUnauthorized).toHaveBeenCalledTimes(1);
  });

  it('adds bearer token when tokenProvider returns one', async () => {
    const fetchImpl = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: { 'content-type': 'application/json' },
      })
    );

    const client = new ApiClient({
      baseUrl: 'http://localhost:5173',
      tokenProvider: () => 'abc123',
      fetchImpl,
    });

    await client.get('/health/integrations');

    const [, init] = fetchImpl.mock.calls[0] as [string, RequestInit];
    const headers = new Headers(init.headers);
    expect(headers.get('authorization')).toBe('Bearer abc123');
  });

  it('supports forced unauthorized mode for live dev testing', async () => {
    const onUnauthorized = vi.fn();
    const fetchImpl = vi.fn();
    const client = new ApiClient({
      baseUrl: 'http://localhost:5173',
      onUnauthorized,
      fetchImpl,
      shouldForceUnauthorized: (path) => path === '/health/integrations',
    });

    await expect(client.get('/health/integrations')).rejects.toBeInstanceOf(SessionExpiredError);
    await expect(client.get('/health/integrations')).rejects.toBeInstanceOf(SessionExpiredError);

    expect(onUnauthorized).toHaveBeenCalledTimes(1);
    expect(fetchImpl).not.toHaveBeenCalled();
  });
});
