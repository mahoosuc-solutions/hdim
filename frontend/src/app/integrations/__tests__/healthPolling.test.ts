import { describe, expect, it } from 'vitest';
import { SessionExpiredError } from '../client';
import { defaultHealthQueryPolicy } from '../healthPolling';

describe('defaultHealthQueryPolicy', () => {
  it('disables retry for session expiry', () => {
    const error = new SessionExpiredError('/health/integrations');
    expect(defaultHealthQueryPolicy.shouldRetry(error)).toBe(false);
  });

  it('allows limited retry for non-auth errors', () => {
    expect(defaultHealthQueryPolicy.shouldRetry(new Error('network down'), 0)).toBe(true);
    expect(defaultHealthQueryPolicy.shouldRetry(new Error('network down'), 2)).toBe(false);
  });

  it('stops polling after session expiry', () => {
    const error = new SessionExpiredError('/health/integrations');
    expect(defaultHealthQueryPolicy.nextRefetchInterval(error)).toBe(false);
  });

  it('keeps polling when no auth error', () => {
    expect(defaultHealthQueryPolicy.nextRefetchInterval(undefined, 5000)).toBe(5000);
  });
});
