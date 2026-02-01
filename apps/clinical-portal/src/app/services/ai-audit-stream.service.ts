import { Injectable, OnDestroy } from '@angular/core';
import { Observable, Subject, BehaviorSubject } from 'rxjs';
import { LoggerService } from './logger.service';

/**
 * Service for managing Server-Sent Events (SSE) connection to AI Audit stream.
 *
 * Features:
 * - Automatic reconnection on connection loss
 * - Connection status tracking
 * - Event filtering by agent type and severity
 * - Observable stream of AI audit events
 *
 * Usage:
 * ```typescript
 * constructor(private aiAuditStream: AiAuditStreamService) {}
 *
 * ngOnInit() {
 *   this.aiAuditStream.connect();
 *   this.aiAuditStream.events$.subscribe(event => {
 *     this.logger.info('New AI decision:', event);
 *   });
 * }
 * ```
 */
@Injectable({
  providedIn: 'root'
})
export class AiAuditStreamService implements OnDestroy {
  private eventSource: EventSource | null = null;
  private eventsSubject = new Subject<any>();
  private connectionStatusSubject = new BehaviorSubject<'connected' | 'disconnected' | 'connecting'>('disconnected');
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 5000; // 5 seconds
  private reconnectTimer: any = null;


  /**
   * Observable stream of AI audit events.
   */
  public events$ = this.eventsSubject.asObservable();

  /**
   * Observable for connection status.
   */
  public connectionStatus$ = this.connectionStatusSubject.asObservable();

  constructor(private logger: LoggerService) {
  }

  /**
   * Connect to the SSE stream.
   *
   * @param agentType Optional filter by agent type (e.g., 'CLINICAL_DECISION')
   * @param severity Optional filter by severity ('HIGH', 'MEDIUM', 'LOW')
   */
  connect(agentType?: string, severity?: string): void {
    if (this.eventSource) {
      this.logger.warn('Already connected to SSE stream');
      return;
    }

    this.connectionStatusSubject.next('connecting');
    this.logger.info('Connecting to AI audit stream', { agentType, severity });

    // Build URL with query parameters
    let url = '/api/v1/audit/ai/stream';
    const params: string[] = [];
    if (agentType) {
      params.push(`agentType=${encodeURIComponent(agentType)}`);
    }
    if (severity) {
      params.push(`severity=${encodeURIComponent(severity)}`);
    }
    if (params.length > 0) {
      url += `?${params.join('&')}`;
    }

    try {
      this.eventSource = new EventSource(url);

      // Handle AI_DECISION events
      this.eventSource.addEventListener('AI_DECISION', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data);
          this.logger.debug('Received AI decision event', data.eventId);
          this.eventsSubject.next(data);
          this.reconnectAttempts = 0; // Reset reconnect counter on successful event
        } catch (error) {
          this.logger.error('Failed to parse SSE event data', error);
        }
      });

      // Handle connection open
      this.eventSource.onopen = () => {
        this.logger.info('SSE connection established');
        this.connectionStatusSubject.next('connected');
        this.reconnectAttempts = 0;
      };

      // Handle errors and auto-reconnect
      this.eventSource.onerror = (error) => {
        this.logger.error('SSE connection error', error);
        this.connectionStatusSubject.next('disconnected');
        this.closeConnection();
        this.attemptReconnect(agentType, severity);
      };

    } catch (error) {
      this.logger.error('Failed to create EventSource', error);
      this.connectionStatusSubject.next('disconnected');
      this.attemptReconnect(agentType, severity);
    }
  }

  /**
   * Disconnect from the SSE stream.
   */
  disconnect(): void {
    this.logger.info('Disconnecting from AI audit stream');
    this.clearReconnectTimer();
    this.closeConnection();
    this.connectionStatusSubject.next('disconnected');
    this.reconnectAttempts = 0;
  }

  /**
   * Close the EventSource connection.
   */
  private closeConnection(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }

  /**
   * Attempt to reconnect to the SSE stream.
   */
  private attemptReconnect(agentType?: string, severity?: string): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      this.logger.error('Max reconnection attempts reached. Giving up.');
      return;
    }

    this.reconnectAttempts++;
    const delay = this.reconnectDelay * this.reconnectAttempts; // Exponential backoff

    this.logger.info(
      `Attempting to reconnect (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts}) in ${delay}ms`
    );

    this.clearReconnectTimer();
    this.reconnectTimer = setTimeout(() => {
      this.connect(agentType, severity);
    }, delay);
  }

  /**
   * Clear the reconnect timer.
   */
  private clearReconnectTimer(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }

  /**
   * Cleanup on service destroy.
   */
  ngOnDestroy(): void {
    this.disconnect();
    this.eventsSubject.complete();
    this.connectionStatusSubject.complete();
  }
}
