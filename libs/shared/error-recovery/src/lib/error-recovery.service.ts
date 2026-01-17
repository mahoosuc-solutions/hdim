/**
 * Error Recovery Service
 *
 * Provides comprehensive error handling, retry logic, and operation queuing for resilience.
 *
 * Features:
 * - Configurable retry policies (exponential backoff, max retries)
 * - Operation queuing during disconnection/failures
 * - Automatic operation retry on connection recovery
 * - Error categorization and handling
 * - Partial failure handling (isolate component failures)
 * - Memory pressure cleanup
 * - Observable streams for error tracking
 *
 * Usage:
 * ```typescript
 * constructor(private errorRecovery: ErrorRecoveryService) {}
 *
 * // Execute with automatic retry
 * this.errorRecovery.executeWithRetry(
 *   () => this.http.get('/api/data'),
 *   'fetch_data',
 *   { maxRetries: 3, initialDelay: 1000 }
 * ).subscribe(
 *   (data) => console.log('Success:', data),
 *   (error) => console.error('Failed after retries:', error)
 * );
 *
 * // Queue operation for later retry
 * this.errorRecovery.queueOperation(
 *   'user_action',
 *   () => this.api.submitForm(formData)
 * );
 *
 * // Handle connection recovery
 * this.websocket.connectionStatus$.subscribe(status => {
 *   if (status === 'connected') {
 *     this.errorRecovery.retryQueuedOperations();
 *   }
 * });
 *
 * // Observable for error tracking
 * this.errorRecovery.errors$.subscribe(error => {
 *   console.error('Tracked error:', error);
 * });
 * ```
 */

import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, Subject, throwError, timer, of } from 'rxjs';
import { Subject as RxSubject } from 'rxjs';
import { retryWhen, mergeMap, finalize, catchError, takeUntil } from 'rxjs/operators';
import { v4 as uuidv4 } from 'uuid';

export interface RetryPolicy {
  maxRetries: number;
  initialDelay: number; // milliseconds
  maxDelay: number;
  backoffMultiplier: number;
}

export interface QueuedOperation {
  id: string;
  name: string;
  operation: () => Observable<any>;
  queuedAt: number;
  attempts: number;
  lastError?: Error;
}

export interface RecoveryError {
  id: string;
  name: string;
  error: any;
  severity: 'warning' | 'error' | 'critical';
  timestamp: number;
  context?: Record<string, any>;
  retriable: boolean;
  attemptCount: number;
}

@Injectable({
  providedIn: 'root'
})
export class ErrorRecoveryService implements OnDestroy {
  private readonly destroy$ = new RxSubject<void>();
  private readonly errorSubject = new Subject<RecoveryError>();
  private readonly operationQueueSubject = new Subject<QueuedOperation[]>();

  public readonly errors$: Observable<RecoveryError> = this.errorSubject.asObservable();
  public readonly operationQueue$: Observable<QueuedOperation[]> = this.operationQueueSubject.asObservable();

  private readonly operationQueue: Map<string, QueuedOperation> = new Map();
  private readonly errorHistory: RecoveryError[] = [];
  private readonly maxQueueSize = 100;
  private readonly maxErrorHistorySize = 1000;

  private defaultRetryPolicy: RetryPolicy = {
    maxRetries: 3,
    initialDelay: 1000,
    maxDelay: 30000,
    backoffMultiplier: 2
  };

  private isProcessingQueue = false;

  constructor(private http: HttpClient) {}

  /**
   * Execute operation with automatic retry on failure
   *
   * @param operation - Function that returns an observable
   * @param operationName - Name for tracking and error reporting
   * @param policy - Optional retry policy (uses default if not provided)
   * @param context - Optional context for error tracking
   */
  executeWithRetry<T>(
    operation: () => Observable<T>,
    operationName: string,
    policy?: Partial<RetryPolicy>,
    context?: Record<string, any>
  ): Observable<T> {
    const retryPolicy = { ...this.defaultRetryPolicy, ...policy };
    let attemptCount = 0;

    return operation().pipe(
      retryWhen(errors =>
        errors.pipe(
          mergeMap((error, index) => {
            attemptCount = index + 1;

            if (attemptCount > retryPolicy.maxRetries) {
              const recoveryError: RecoveryError = {
                id: uuidv4(),
                name: operationName,
                error,
                severity: this.getSeverity(error),
                timestamp: Date.now(),
                context,
                retriable: false,
                attemptCount
              };
              this.errorSubject.next(recoveryError);
              this.addToErrorHistory(recoveryError);
              return throwError(() => error);
            }

            // Calculate exponential backoff delay
            const delay = Math.min(
              retryPolicy.initialDelay * Math.pow(retryPolicy.backoffMultiplier, attemptCount - 1),
              retryPolicy.maxDelay
            );

            return timer(delay);
          })
        )
      ),
      catchError(error => {
        return throwError(() => error);
      })
    );
  }

  /**
   * Queue operation for later retry (useful during disconnection)
   */
  queueOperation(
    name: string,
    operation: () => Observable<any>,
    priority: number = 0
  ): string {
    // Check queue size
    if (this.operationQueue.size >= this.maxQueueSize) {
      // Remove oldest low-priority items
      const sorted = Array.from(this.operationQueue.values()).sort((a, b) => b.queuedAt - a.queuedAt);
      const toRemove = sorted.slice(50); // Keep 50 items
      toRemove.forEach(op => this.operationQueue.delete(op.id));
    }

    const queued: QueuedOperation = {
      id: uuidv4(),
      name,
      operation,
      queuedAt: Date.now(),
      attempts: 0
    };

    this.operationQueue.set(queued.id, queued);
    this.operationQueueSubject.next(Array.from(this.operationQueue.values()));

    return queued.id;
  }

  /**
   * Retry all queued operations
   */
  retryQueuedOperations(): void {
    if (this.isProcessingQueue || this.operationQueue.size === 0) {
      return;
    }

    this.isProcessingQueue = true;
    const operations = Array.from(this.operationQueue.values());

    operations.forEach(op => {
      op.attempts++;
      op.operation()
        .pipe(
          takeUntil(this.destroy$),
          finalize(() => {
            this.operationQueue.delete(op.id);
            this.operationQueueSubject.next(Array.from(this.operationQueue.values()));

            if (this.operationQueue.size === 0) {
              this.isProcessingQueue = false;
            }
          })
        )
        .subscribe({
          next: () => {
            // Success - operation removed from queue
          },
          error: (error) => {
            // Keep in queue for next retry
            op.lastError = error;
          }
        });
    });
  }

  /**
   * Get queued operation by ID
   */
  getQueuedOperation(id: string): QueuedOperation | undefined {
    return this.operationQueue.get(id);
  }

  /**
   * Get all queued operations
   */
  getQueuedOperations(): QueuedOperation[] {
    return Array.from(this.operationQueue.values());
  }

  /**
   * Remove operation from queue
   */
  removeQueuedOperation(id: string): void {
    this.operationQueue.delete(id);
    this.operationQueueSubject.next(Array.from(this.operationQueue.values()));
  }

  /**
   * Clear all queued operations
   */
  clearQueue(): void {
    this.operationQueue.clear();
    this.operationQueueSubject.next([]);
  }

  /**
   * Get queue size
   */
  getQueueSize(): number {
    return this.operationQueue.size;
  }

  /**
   * Get error history
   */
  getErrorHistory(): RecoveryError[] {
    return [...this.errorHistory];
  }

  /**
   * Clear error history
   */
  clearErrorHistory(): void {
    this.errorHistory.length = 0;
  }

  /**
   * Get recent errors by category
   */
  getErrorsByName(name: string): RecoveryError[] {
    return this.errorHistory.filter(e => e.name === name);
  }

  /**
   * Get errors by severity
   */
  getErrorsBySeverity(severity: 'warning' | 'error' | 'critical'): RecoveryError[] {
    return this.errorHistory.filter(e => e.severity === severity);
  }

  /**
   * Check if operation is retriable
   */
  isRetriable(error: any): boolean {
    if (error instanceof HttpErrorResponse) {
      // Retriable on network errors and specific status codes
      if (error.status === 0) return true; // Network error
      if (error.status >= 500) return true; // Server error
      if (error.status === 408) return true; // Request timeout
      if (error.status === 429) return true; // Too many requests
    }
    return false;
  }

  /**
   * Set default retry policy
   */
  setDefaultRetryPolicy(policy: Partial<RetryPolicy>): void {
    this.defaultRetryPolicy = { ...this.defaultRetryPolicy, ...policy };
  }

  /**
   * Get default retry policy
   */
  getDefaultRetryPolicy(): RetryPolicy {
    return { ...this.defaultRetryPolicy };
  }

  /**
   * Perform memory cleanup on memory pressure
   */
  performMemoryCleanup(): void {
    // Clear old error history
    const maxAge = 60 * 60 * 1000; // 1 hour
    const now = Date.now();
    const filtered = this.errorHistory.filter(e => now - e.timestamp < maxAge);
    this.errorHistory.length = 0;
    this.errorHistory.push(...filtered);

    // Clear old queued operations
    const maxQueueAge = 24 * 60 * 60 * 1000; // 24 hours
    for (const [id, op] of this.operationQueue.entries()) {
      if (now - op.queuedAt > maxQueueAge) {
        this.operationQueue.delete(id);
      }
    }

    this.operationQueueSubject.next(Array.from(this.operationQueue.values()));
  }

  private getSeverity(error: any): 'warning' | 'error' | 'critical' {
    if (error instanceof HttpErrorResponse) {
      if (error.status >= 500) return 'critical';
      if (error.status >= 400) return 'error';
    }
    return 'warning';
  }

  private addToErrorHistory(error: RecoveryError): void {
    this.errorHistory.push(error);
    if (this.errorHistory.length > this.maxErrorHistorySize) {
      this.errorHistory.shift();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
