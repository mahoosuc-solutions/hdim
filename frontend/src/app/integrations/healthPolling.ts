import { SessionExpiredError } from './client';

export const DEFAULT_HEALTH_REFETCH_INTERVAL_MS = 5000;
export const DEFAULT_HEALTH_MAX_RETRIES = 2;

function isUnauthorizedError(error: unknown): boolean {
  if (error instanceof SessionExpiredError) {
    return true;
  }

  if (typeof error === 'object' && error !== null && 'status' in error) {
    return (error as { status?: number }).status === 401;
  }

  if (error instanceof Error) {
    return /unauthorized|session expired/i.test(error.message);
  }

  return false;
}

export const defaultHealthQueryPolicy = {
  shouldRetry: (error: unknown, failureCount = 0): boolean => {
    if (isUnauthorizedError(error)) {
      return false;
    }

    return failureCount < DEFAULT_HEALTH_MAX_RETRIES;
  },
  nextRefetchInterval: (
    error?: unknown,
    intervalMs = DEFAULT_HEALTH_REFETCH_INTERVAL_MS
  ): number | false => {
    if (isUnauthorizedError(error)) {
      return false;
    }

    return intervalMs;
  },
};
