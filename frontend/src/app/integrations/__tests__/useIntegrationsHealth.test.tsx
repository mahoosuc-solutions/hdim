import { renderHook, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { ApiClient } from '../client';
import { useIntegrationsHealth } from '../useIntegrationsHealth';

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

describe('useIntegrationsHealth', () => {
  it('polls health endpoint at interval', async () => {
    const fetchImpl = vi.fn().mockImplementation(async () =>
      new Response(JSON.stringify({ status: 'ok' }), {
        status: 200,
        headers: { 'content-type': 'application/json' },
      })
    );
    const client = new ApiClient({ baseUrl: 'http://localhost:5173', fetchImpl });
    const { result } = renderHook(() =>
      useIntegrationsHealth({
        client,
        intervalMs: 20,
      })
    );

    await waitFor(() => expect(result.current.status).toBe('ok'));
    expect(fetchImpl.mock.calls.length).toBeGreaterThanOrEqual(1);

    await waitFor(() => expect(fetchImpl.mock.calls.length).toBeGreaterThanOrEqual(3));
  });

  it('stops polling after unauthorized response', async () => {
    const onSessionExpired = vi.fn();
    const fetchImpl = vi
      .fn()
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ status: 'ok' }), {
          status: 200,
          headers: { 'content-type': 'application/json' },
        })
      )
      .mockResolvedValueOnce(new Response('expired', { status: 401, statusText: 'Unauthorized' }))
      .mockImplementation(async () =>
        new Response(JSON.stringify({ status: 'ok' }), {
          status: 200,
          headers: { 'content-type': 'application/json' },
        })
      );

    const client = new ApiClient({ baseUrl: 'http://localhost:5173', fetchImpl });
    const { result } = renderHook(() =>
      useIntegrationsHealth({
        client,
        intervalMs: 20,
        onSessionExpired,
      })
    );

    await waitFor(() => expect(result.current.data?.status).toBe('ok'));

    await waitFor(() => expect(result.current.sessionExpired).toBe(true));
    expect(onSessionExpired).toHaveBeenCalledTimes(1);

    const callCountAtExpiry = fetchImpl.mock.calls.length;

    await sleep(80);

    expect(fetchImpl).toHaveBeenCalledTimes(callCountAtExpiry);
  });
});
