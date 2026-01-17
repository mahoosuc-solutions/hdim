import { Observable, throwError, timer } from 'rxjs';
import { retryWhen, scan, mergeMap } from 'rxjs/operators';

/**
 * Configuration for retry-with-backoff operator
 */
export interface RetryWithBackoffConfig {
  /**
   * Maximum number of retry attempts after initial failure
   */
  maxRetries: number;

  /**
   * Initial delay in milliseconds before first retry
   */
  initialDelay: number;

  /**
   * Maximum delay in milliseconds (to cap exponential backoff)
   * Default: 30000 (30 seconds)
   */
  maxDelay?: number;
}

/**
 * Retry operator with exponential backoff
 *
 * Automatically retries failed observables with exponential backoff delay.
 * Useful for handling temporary network failures in WebSocket connections.
 *
 * Example:
 * ```typescript
 * websocketService.messages$.pipe(
 *   retryWithBackoff({ maxRetries: 5, initialDelay: 1000 })
 * ).subscribe(msg => console.log(msg));
 * ```
 *
 * @param config - Retry configuration
 * @returns Operator function for piping
 */
export function retryWithBackoff(config: RetryWithBackoffConfig) {
  return <T>(source: Observable<T>) => {
    const maxDelay = config.maxDelay ?? 30000;

    return source.pipe(
      retryWhen(errors =>
        errors.pipe(
          scan((retryCount, error) => {
            if (retryCount >= config.maxRetries) {
              throw error;
            }
            return retryCount + 1;
          }, 0),
          mergeMap((retryCount) => {
            const backoffDelay = config.initialDelay * Math.pow(2, retryCount - 1);
            const cappedDelay = Math.min(backoffDelay, maxDelay);
            return timer(cappedDelay);
          })
        )
      )
    );
  };
}
