import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { ApiClient, SessionExpiredError } from './client';
import { defaultHealthQueryPolicy, DEFAULT_HEALTH_REFETCH_INTERVAL_MS } from './healthPolling';
import { getHealth, type IntegrationsHealthResponse } from './integrations';

interface UseIntegrationsHealthOptions {
  client: ApiClient;
  enabled?: boolean;
  intervalMs?: number;
  onSessionExpired?: () => void;
}

interface UseIntegrationsHealthResult {
  data?: IntegrationsHealthResponse;
  status: 'idle' | 'loading' | 'ok' | 'error';
  error?: Error;
  sessionExpired: boolean;
}

export function useIntegrationsHealth(
  options: UseIntegrationsHealthOptions
): UseIntegrationsHealthResult {
  const { client, enabled = true, intervalMs = DEFAULT_HEALTH_REFETCH_INTERVAL_MS, onSessionExpired } = options;
  const [data, setData] = useState<IntegrationsHealthResponse | undefined>(undefined);
  const [status, setStatus] = useState<'idle' | 'loading' | 'ok' | 'error'>(enabled ? 'loading' : 'idle');
  const [error, setError] = useState<Error | undefined>(undefined);
  const [sessionExpired, setSessionExpired] = useState(false);
  const failureCountRef = useRef(0);

  const pollOnce = useCallback(async (): Promise<{ shouldContinue: boolean; encounteredError?: Error }> => {
    try {
      const next = await getHealth(client);
      setData(next);
      setStatus('ok');
      setError(undefined);
      failureCountRef.current = 0;
      return { shouldContinue: true };
    } catch (err) {
      const normalizedError = err instanceof Error ? err : new Error(String(err));
      setError(normalizedError);
      setStatus('error');
      failureCountRef.current += 1;

      if (normalizedError instanceof SessionExpiredError) {
        setSessionExpired(true);
        onSessionExpired?.();
      }

      return {
        shouldContinue: defaultHealthQueryPolicy.shouldRetry(normalizedError, failureCountRef.current),
        encounteredError: normalizedError,
      };
    }
  }, [client, onSessionExpired]);

  useEffect(() => {
    if (!enabled) {
      setStatus('idle');
      return;
    }

    let active = true;
    let timeoutId: ReturnType<typeof setTimeout> | undefined;

    const tick = async () => {
      if (!active) return;

      const { shouldContinue, encounteredError } = await pollOnce();
      if (!active) return;

      const nextInterval = defaultHealthQueryPolicy.nextRefetchInterval(
        encounteredError,
        intervalMs
      );

      if (shouldContinue && nextInterval !== false) {
        timeoutId = setTimeout(tick, nextInterval);
      }
    };

    tick();

    return () => {
      active = false;
      if (timeoutId) {
        clearTimeout(timeoutId);
      }
    };
  }, [enabled, intervalMs, pollOnce]);

  return useMemo(
    () => ({
      data,
      status,
      error,
      sessionExpired,
    }),
    [data, status, error, sessionExpired]
  );
}
