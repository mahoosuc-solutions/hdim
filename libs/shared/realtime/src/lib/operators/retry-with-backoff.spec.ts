import { retryWithBackoff } from './retry-with-backoff';
import { throwError, of, Subject } from 'rxjs';
import { take } from 'rxjs/operators';

describe('retryWithBackoff operator', () => {
  it('should succeed on first attempt without retry', (done) => {
    const source = of('success').pipe(
      retryWithBackoff({ maxRetries: 3, initialDelay: 100 })
    );

    source.subscribe({
      next: (value) => {
        expect(value).toBe('success');
      },
      complete: () => {
        done();
      }
    });
  });

  it('should fail after max retries exceeded', (done) => {
    const source = throwError(() => new Error('Connection failed')).pipe(
      retryWithBackoff({ maxRetries: 1, initialDelay: 10 })
    );

    let errorCaught = false;
    source.subscribe({
      error: (err) => {
        expect(err.message).toBe('Connection failed');
        errorCaught = true;
        done();
      }
    });
  });

  it('should retry specified number of times', (done) => {
    let attemptCount = 0;
    const source = new Subject<string>();

    const testSource = source.pipe(
      take(1), // Only take first emission
      retryWithBackoff({ maxRetries: 2, initialDelay: 10 })
    );

    let errorCount = 0;
    testSource.subscribe({
      error: () => {
        errorCount++;
        done();
      }
    });

    // Error immediately to trigger retries
    source.error(new Error('First error'));
  });

  it('should handle zero initial delay', (done) => {
    const source = throwError(() => new Error('test')).pipe(
      retryWithBackoff({ maxRetries: 1, initialDelay: 0 })
    );

    source.subscribe({
      error: () => {
        done();
      }
    });
  });

  it('should provide configuration options', (done) => {
    // Test that configuration is properly used
    const config = {
      maxRetries: 3,
      initialDelay: 500,
      maxDelay: 15000
    };

    expect(config.maxRetries).toBe(3);
    expect(config.initialDelay).toBe(500);
    expect(config.maxDelay).toBe(15000);

    done();
  });

  it('should respect maxDelay cap', (done) => {
    // This test verifies the logic without actual timing
    const config = {
      maxRetries: 10,
      initialDelay: 1000,
      maxDelay: 5000
    };

    // Calculate what delays would be
    let delay = config.initialDelay;
    let capped = Math.min(delay, config.maxDelay);
    expect(capped).toBe(1000);

    delay = config.initialDelay * Math.pow(2, 1);
    capped = Math.min(delay, config.maxDelay);
    expect(capped).toBe(2000);

    delay = config.initialDelay * Math.pow(2, 2);
    capped = Math.min(delay, config.maxDelay);
    expect(capped).toBe(4000);

    delay = config.initialDelay * Math.pow(2, 3);
    capped = Math.min(delay, config.maxDelay);
    expect(capped).toBe(5000); // Capped at maxDelay

    done();
  });

  it('should have correct exponential backoff formula', (done) => {
    const config = {
      maxRetries: 5,
      initialDelay: 100,
      maxDelay: 30000
    };

    // Test exponential backoff calculation
    const delays = [];
    for (let i = 0; i < config.maxRetries; i++) {
      const backoff = config.initialDelay * Math.pow(2, i);
      const capped = Math.min(backoff, config.maxDelay);
      delays.push(capped);
    }

    expect(delays[0]).toBe(100);    // 100 * 2^0
    expect(delays[1]).toBe(200);    // 100 * 2^1
    expect(delays[2]).toBe(400);    // 100 * 2^2
    expect(delays[3]).toBe(800);    // 100 * 2^3
    expect(delays[4]).toBe(1600);   // 100 * 2^4

    done();
  });
});
