/**
 * Analytics Service
 *
 * Handles event tracking, batching, and backend integration for analytics collection.
 *
 * Features:
 * - Event recording with automatic metadata (timestamp, userId, sessionId, tenantId)
 * - Event batching (configurable batch size, default 10)
 * - Automatic batch flushing on interval (default 5 seconds)
 * - Manual batch flushing
 * - Backend integration (POST to /api/analytics/events)
 * - Observable streams for event tracking
 * - Multiple event categories (connection, notification, performance, engagement)
 *
 * Usage:
 * ```typescript
 * constructor(private analytics: AnalyticsService) {}
 *
 * onWebSocketConnected() {
 *   this.analytics.recordEvent('websocket_connected', {
 *     connectionDuration: 150,  // ms
 *     latency: 75               // ms
 *   });
 * }
 *
 * onNotificationShown(notificationId: string) {
 *   this.analytics.recordEvent('notification_shown', {
 *     notificationId,
 *     type: 'toast'
 *   });
 * }
 *
 * // Observables
 * this.analytics.events$.subscribe(event => {
 *   console.log('Tracked:', event.eventName);
 * });
 *
 * this.analytics.batches$.subscribe(batch => {
 *   console.log('Flushed', batch.events.length, 'events');
 * });
 * ```
 */

import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Subject, Observable, interval, Subscription } from 'rxjs';
import { takeUntil, filter } from 'rxjs/operators';
import { v4 as uuidv4 } from 'uuid';

export interface AnalyticsEvent {
  id: string;
  eventName: string;
  category: 'connection' | 'notification' | 'performance' | 'engagement' | 'error' | 'business';
  userId: string;
  tenantId: string;
  sessionId: string;
  timestamp: number;
  metadata: Record<string, any>;
  correlationId?: string;
}

export interface AnalyticsBatch {
  id: string;
  events: AnalyticsEvent[];
  createdAt: number;
  flushedAt?: number;
}

export interface AnalyticsConfig {
  batchSize: number;
  flushInterval: number; // milliseconds
  apiEndpoint: string;
  enabled: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AnalyticsService implements OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private readonly eventSubject = new Subject<AnalyticsEvent>();
  private readonly batchSubject = new Subject<AnalyticsBatch>();

  public readonly events$: Observable<AnalyticsEvent> = this.eventSubject.asObservable();
  public readonly batches$: Observable<AnalyticsBatch> = this.batchSubject.asObservable();

  private currentBatch: AnalyticsEvent[] = [];
  private readonly batchQueue: AnalyticsBatch[] = [];
  private batchFlushTimer: Subscription | null = null;

  private config: AnalyticsConfig = {
    batchSize: 10,
    flushInterval: 5000, // 5 seconds
    apiEndpoint: '/api/analytics/events',
    enabled: true
  };

  private userId: string;
  private tenantId: string;
  private sessionId: string;

  constructor(private http: HttpClient) {
    this.userId = this.generateUserId();
    this.tenantId = this.generateTenantId();
    this.sessionId = this.generateSessionId();

    this.initializeAutoFlush();
  }

  /**
   * Record an analytics event
   *
   * @param eventName - Name of the event (e.g., 'websocket_connected')
   * @param metadata - Event metadata (will be merged with standard fields)
   * @param category - Event category (default: 'engagement')
   */
  recordEvent(
    eventName: string,
    metadata: Record<string, any> = {},
    category: AnalyticsEvent['category'] = 'engagement',
    correlationId?: string
  ): void {
    if (!this.config.enabled) {
      return;
    }

    const event: AnalyticsEvent = {
      id: uuidv4(),
      eventName,
      category,
      userId: this.userId,
      tenantId: this.tenantId,
      sessionId: this.sessionId,
      timestamp: Date.now(),
      metadata,
      correlationId
    };

    this.currentBatch.push(event);
    this.eventSubject.next(event);

    // Auto-flush if batch is full
    if (this.currentBatch.length >= this.config.batchSize) {
      this.flushBatch();
    }
  }

  /**
   * Flush current batch to backend
   */
  flushBatch(): void {
    if (this.currentBatch.length === 0) {
      return;
    }

    const batch: AnalyticsBatch = {
      id: uuidv4(),
      events: [...this.currentBatch],
      createdAt: Date.now()
    };

    this.currentBatch = [];
    this.batchQueue.push(batch);

    // Send to backend
    this.sendBatchToBackend(batch)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          batch.flushedAt = Date.now();
          this.batchSubject.next(batch);
        },
        error: (error) => {
          console.error('Failed to flush analytics batch:', error);
          // Re-add to queue for retry
          this.currentBatch.push(...batch.events);
        }
      });
  }

  /**
   * Get current batch
   */
  getCurrentBatch(): AnalyticsEvent[] {
    return [...this.currentBatch];
  }

  /**
   * Get batch queue
   */
  getBatchQueue(): AnalyticsBatch[] {
    return [...this.batchQueue];
  }

  /**
   * Set user ID for tracking
   */
  setUserId(userId: string): void {
    this.userId = userId;
  }

  /**
   * Get current user ID
   */
  getUserId(): string {
    return this.userId;
  }

  /**
   * Set tenant ID for tracking
   */
  setTenantId(tenantId: string): void {
    this.tenantId = tenantId;
  }

  /**
   * Get current tenant ID
   */
  getTenantId(): string {
    return this.tenantId;
  }

  /**
   * Get current session ID
   */
  getSessionId(): string {
    return this.sessionId;
  }

  /**
   * Update analytics configuration
   */
  setConfig(config: Partial<AnalyticsConfig>): void {
    this.config = { ...this.config, ...config };

    // Restart auto-flush with new interval
    if (this.batchFlushTimer) {
      this.batchFlushTimer.unsubscribe();
    }
    this.initializeAutoFlush();
  }

  /**
   * Get current configuration
   */
  getConfig(): AnalyticsConfig {
    return { ...this.config };
  }

  /**
   * Clear all events and batches (for testing)
   */
  clear(): void {
    this.currentBatch = [];
    this.batchQueue.length = 0;
  }

  private initializeAutoFlush(): void {
    if (this.batchFlushTimer) {
      this.batchFlushTimer.unsubscribe();
    }

    this.batchFlushTimer = interval(this.config.flushInterval)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        if (this.currentBatch.length > 0) {
          this.flushBatch();
        }
      });
  }

  private sendBatchToBackend(batch: AnalyticsBatch): Observable<any> {
    return this.http.post(this.config.apiEndpoint, {
      batch: batch.events,
      timestamp: batch.createdAt
    });
  }

  private generateUserId(): string {
    const stored = sessionStorage.getItem('analytics_user_id');
    if (stored) {
      return stored;
    }
    const userId = uuidv4();
    sessionStorage.setItem('analytics_user_id', userId);
    return userId;
  }

  private generateTenantId(): string {
    const stored = sessionStorage.getItem('analytics_tenant_id');
    return stored || 'default-tenant';
  }

  private generateSessionId(): string {
    const stored = sessionStorage.getItem('analytics_session_id');
    if (stored) {
      return stored;
    }
    const sessionId = uuidv4();
    sessionStorage.setItem('analytics_session_id', sessionId);
    return sessionId;
  }

  ngOnDestroy(): void {
    // Flush remaining events
    this.flushBatch();

    // Clean up subscriptions
    if (this.batchFlushTimer) {
      this.batchFlushTimer.unsubscribe();
    }
    this.destroy$.next();
    this.destroy$.complete();
  }
}
