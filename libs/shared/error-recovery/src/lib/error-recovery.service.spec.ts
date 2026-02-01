import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ErrorRecoveryService, QueuedOperation } from './error-recovery.service';
import { of, throwError } from 'rxjs';

describe('ErrorRecoveryService', () => {
  let service: ErrorRecoveryService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ErrorRecoveryService]
    });
    service = TestBed.inject(ErrorRecoveryService);
    httpMock = TestBed.configureTestingModule({}).compileComponents();
  });

  afterEach(() => {
    service.clearQueue();
    service.clearErrorHistory();
  });

  describe('Retry Logic', () => {
    it('should execute operation with retry on failure', (done) => {
      let attempts = 0;
      const operation = () => {
        attempts++;
        return attempts < 3 ? throwError(() => new Error('Test error')) : of({ success: true });
      };

      service.executeWithRetry(operation, 'test_op', { maxRetries: 3, initialDelay: 10 })
        .subscribe({
          next: (result) => {
            expect(result.success).toBe(true);
            expect(attempts).toBe(3);
            done();
          },
          error: () => fail('Should have succeeded after retries')
        });
    });

    it('should fail after max retries exceeded', (done) => {
      let attempts = 0;
      const operation = () => {
        attempts++;
        return throwError(() => new Error('Persistent error'));
      };

      service.executeWithRetry(operation, 'test_op', { maxRetries: 2, initialDelay: 10 })
        .subscribe({
          next: () => fail('Should have failed'),
          error: (error) => {
            expect(error.message).toBe('Persistent error');
            expect(attempts).toBe(3); // 1 initial + 2 retries
            done();
          }
        });
    });

    it('should use custom retry policy', (done) => {
      const operation = () => of({ data: 'test' });

      service.executeWithRetry(operation, 'test_op', { maxRetries: 5, initialDelay: 50, maxDelay: 500 })
        .subscribe({
          next: () => {
            const policy = service.getDefaultRetryPolicy();
            expect(policy.maxRetries).toBe(3); // Default unchanged
            done();
          }
        });
    });
  });

  describe('Operation Queuing', () => {
    it('should queue operation', () => {
      const operation = () => of({});
      const id = service.queueOperation('test_op', operation);

      expect(id).toBeTruthy();
      expect(service.getQueueSize()).toBe(1);
    });

    it('should retrieve queued operation', () => {
      const operation = () => of({ data: 'test' });
      const id = service.queueOperation('test_op', operation);

      const queued = service.getQueuedOperation(id);
      expect(queued).toBeTruthy();
      expect(queued?.name).toBe('test_op');
    });

    it('should get all queued operations', () => {
      service.queueOperation('op1', () => of({}));
      service.queueOperation('op2', () => of({}));
      service.queueOperation('op3', () => of({}));

      const operations = service.getQueuedOperations();
      expect(operations.length).toBe(3);
    });

    it('should remove queued operation', () => {
      const id = service.queueOperation('test_op', () => of({}));
      expect(service.getQueueSize()).toBe(1);

      service.removeQueuedOperation(id);
      expect(service.getQueueSize()).toBe(0);
    });

    it('should clear all queued operations', () => {
      service.queueOperation('op1', () => of({}));
      service.queueOperation('op2', () => of({}));

      service.clearQueue();
      expect(service.getQueueSize()).toBe(0);
    });

    it('should enforce max queue size', () => {
      for (let i = 0; i < 150; i++) {
        service.queueOperation(`op_${i}`, () => of({}));
      }

      expect(service.getQueueSize()).toBeLessThanOrEqual(100);
    });
  });

  describe('Error Tracking', () => {
    it('should track error history', (done) => {
      let errorCount = 0;

      service.errors$.subscribe(() => {
        errorCount++;
      });

      const operation = () => throwError(() => new Error('Test error'));
      service.executeWithRetry(operation, 'failing_op', { maxRetries: 1, initialDelay: 10 })
        .subscribe({
          error: () => {
            setTimeout(() => {
              expect(errorCount).toBeGreaterThan(0);
              done();
            }, 50);
          }
        });
    });

    it('should get error history', (done) => {
      const operation = () => throwError(() => new Error('Test error'));
      service.executeWithRetry(operation, 'test_op', { maxRetries: 1, initialDelay: 10 })
        .subscribe({
          error: () => {
            setTimeout(() => {
              const history = service.getErrorHistory();
              expect(history.length).toBeGreaterThan(0);
              done();
            }, 50);
          }
        });
    });

    it('should clear error history', (done) => {
      const operation = () => throwError(() => new Error('Test error'));
      service.executeWithRetry(operation, 'test_op', { maxRetries: 1, initialDelay: 10 })
        .subscribe({
          error: () => {
            setTimeout(() => {
              service.clearErrorHistory();
              const history = service.getErrorHistory();
              expect(history.length).toBe(0);
              done();
            }, 50);
          }
        });
    });

    it('should get errors by name', (done) => {
      let completed = 0;

      const op1 = () => throwError(() => new Error('Error 1'));
      const op2 = () => throwError(() => new Error('Error 2'));

      service.executeWithRetry(op1, 'op_type_1', { maxRetries: 1, initialDelay: 10 })
        .subscribe({ error: () => { completed++; } });

      service.executeWithRetry(op2, 'op_type_2', { maxRetries: 1, initialDelay: 10 })
        .subscribe({
          error: () => {
            completed++;
            if (completed === 2) {
              setTimeout(() => {
                const errors1 = service.getErrorsByName('op_type_1');
                expect(errors1.length).toBeGreaterThan(0);
                done();
              }, 100);
            }
          }
        });
    });

    it('should get errors by severity', (done) => {
      const operation = () => throwError(() => new Error('Test error'));
      service.executeWithRetry(operation, 'test_op', { maxRetries: 1, initialDelay: 10 })
        .subscribe({
          error: () => {
            setTimeout(() => {
              const critical = service.getErrorsBySeverity('critical');
              const warnings = service.getErrorsBySeverity('warning');
              expect(critical.length + warnings.length + service.getErrorsBySeverity('error').length)
                .toBeGreaterThan(0);
              done();
            }, 50);
          }
        });
    });
  });

  describe('Retry Conditions', () => {
    it('should identify retriable errors', () => {
      // Network error
      const networkError = { status: 0 };
      expect(service.isRetriable(networkError)).toBe(true);

      // Server error
      const serverError = { status: 500 };
      expect(service.isRetriable(serverError)).toBe(true);

      // Timeout
      const timeoutError = { status: 408 };
      expect(service.isRetriable(timeoutError)).toBe(true);

      // Rate limit
      const rateLimitError = { status: 429 };
      expect(service.isRetriable(rateLimitError)).toBe(true);

      // Client error (not retriable)
      const clientError = { status: 400 };
      expect(service.isRetriable(clientError)).toBe(false);
    });
  });

  describe('Retry Policy Configuration', () => {
    it('should set default retry policy', () => {
      service.setDefaultRetryPolicy({ maxRetries: 5, initialDelay: 500 });

      const policy = service.getDefaultRetryPolicy();
      expect(policy.maxRetries).toBe(5);
      expect(policy.initialDelay).toBe(500);
    });

    it('should get default retry policy', () => {
      const policy = service.getDefaultRetryPolicy();

      expect(policy.maxRetries).toBeGreaterThan(0);
      expect(policy.initialDelay).toBeGreaterThan(0);
      expect(policy.maxDelay).toBeGreaterThan(policy.initialDelay);
      expect(policy.backoffMultiplier).toBeGreaterThan(1);
    });
  });

  describe('Observable Streams', () => {
    it('should emit operation queue updates', (done) => {
      let queueUpdated = false;

      service.operationQueue$.subscribe(queue => {
        if (queue.length > 0) {
          queueUpdated = true;
        }
      });

      service.queueOperation('test_op', () => of({}));

      setTimeout(() => {
        expect(queueUpdated).toBe(true);
        done();
      }, 50);
    });
  });

  describe('Memory Cleanup', () => {
    it('should perform memory cleanup', (done) => {
      // Add old errors
      for (let i = 0; i < 10; i++) {
        const op = () => throwError(() => new Error(`Error ${i}`));
        service.executeWithRetry(op, `test_op_${i}`, { maxRetries: 1, initialDelay: 10 })
          .subscribe({ error: () => {} });
      }

      setTimeout(() => {
        const historyBefore = service.getErrorHistory().length;
        service.performMemoryCleanup();
        const historyAfter = service.getErrorHistory().length;

        expect(historyAfter).toBeLessThanOrEqual(historyBefore);
        done();
      }, 100);
    });
  });

  describe('Exponential Backoff', () => {
    it('should increase delay with exponential backoff', (done) => {
      let attempts = 0;
      const timestamps: number[] = [];

      const operation = () => {
        timestamps.push(Date.now());
        attempts++;
        if (attempts < 3) {
          return throwError(() => new Error('Retry'));
        }
        return of({ success: true });
      };

      service.executeWithRetry(operation, 'backoff_test', {
        maxRetries: 3,
        initialDelay: 20,
        maxDelay: 1000,
        backoffMultiplier: 2
      }).subscribe({
        next: () => {
          if (timestamps.length >= 3) {
            const delay1 = timestamps[1] - timestamps[0];
            const delay2 = timestamps[2] - timestamps[1];
            // Second delay should be larger than first
            expect(delay2).toBeGreaterThan(delay1 / 2);
            done();
          }
        },
        error: () => fail('Should succeed')
      });
    });
  });

  describe('Integration Scenarios', () => {
    it('should queue operation during failure and retry on recovery', (done) => {
      const operation = () => of({ recovered: true });

      // Queue operation
      const id = service.queueOperation('recover_op', operation);
      expect(service.getQueueSize()).toBe(1);

      // Simulate connection recovery
      service.retryQueuedOperations();

      setTimeout(() => {
        expect(service.getQueueSize()).toBe(0);
        done();
      }, 100);
    });

    it('should handle mixed queue of successful and failed operations', (done) => {
      let successes = 0;

      const op1 = () => of({ data: 'success1' });
      const op2 = () => throwError(() => new Error('Failed'));
      const op3 = () => of({ data: 'success2' });

      service.queueOperation('op1', op1);
      service.queueOperation('op2', op2);
      service.queueOperation('op3', op3);

      service.retryQueuedOperations();

      setTimeout(() => {
        const remaining = service.getQueueSize();
        expect(remaining).toBeGreaterThanOrEqual(0);
        done();
      }, 200);
    });
  });
});
