import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { filter, tap } from 'rxjs/operators';

/**
 * Native WebSocket service for coaching UI following HDIM WebSocket pattern.
 *
 * **HDIM Pattern:**
 * - JWT authentication via URL parameter or Authorization header
 * - Native WebSocket (not STOMP/SockJS)
 * - Topic-based message routing
 * - Automatic reconnection with exponential backoff
 * - Heartbeat/ping to keep connection alive
 * - HIPAA-compliant message structure
 */
@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private ws: WebSocket | null = null;
  private messageSubject = new Subject<any>();
  private connectionStatusSubject = new BehaviorSubject<boolean>(false);
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private heartbeatInterval: any;

  messages$ = this.messageSubject.asObservable();
  connectionStatus$ = this.connectionStatusSubject.asObservable();

  constructor() {}

  /**
   * Connect to WebSocket endpoint with JWT authentication.
   *
   * **Pattern:** JWT token passed in URL parameter as per HDIM Quality Measure Service.
   * This allows stateless authentication without session cookies.
   */
  connect(topic: string, jwtToken?: string): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      console.warn('WebSocket already connected');
      return;
    }

    // Get JWT token from storage if not provided
    const token = jwtToken || this.getJwtToken();
    if (!token) {
      console.error('❌ No JWT token available for WebSocket connection');
      return;
    }

    // Construct WebSocket URL with JWT token
    const wsUrl = this.buildWebSocketUrl(topic, token);

    try {
      this.ws = new WebSocket(wsUrl);

      this.ws.onopen = () => this.onConnectionOpen();
      this.ws.onmessage = (event) => this.onMessage(event);
      this.ws.onerror = (error) => this.onError(error);
      this.ws.onclose = () => this.onConnectionClose();
    } catch (error) {
      console.error('❌ Failed to create WebSocket:', error);
      this.connectionStatusSubject.next(false);
    }
  }

  /**
   * Send message through WebSocket with HDIM envelope format.
   *
   * Message structure:
   * ```
   * {
   *   type: 'coaching|transcript|status',
   *   timestamp: number,
   *   tenantId: string,
   *   userId: string,
   *   payload: {...}
   * }
   * ```
   */
  send(type: string, payload: any): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.warn('⚠️ WebSocket not connected, message not sent');
      return;
    }

    const message = {
      type,
      timestamp: Date.now(),
      tenantId: this.getTenantId(),
      userId: this.getUserId(),
      payload,
    };

    try {
      this.ws.send(JSON.stringify(message));
    } catch (error) {
      console.error('❌ Failed to send message:', error);
    }
  }

  /**
   * Filter messages by type.
   */
  getMessagesByType(type: string): Observable<any> {
    return this.messages$.pipe(
      filter(msg => msg.type === type),
      tap(msg => console.debug(`📨 Received ${type} message:`, msg))
    );
  }

  /**
   * Disconnect from WebSocket and cleanup.
   */
  disconnect(): void {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
    }

    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }

    this.connectionStatusSubject.next(false);
  }

  // Private methods

  private onConnectionOpen(): void {
    console.log('✅ WebSocket connected');
    this.connectionStatusSubject.next(true);
    this.reconnectAttempts = 0;

    // Start heartbeat
    this.startHeartbeat();
  }

  private onMessage(event: MessageEvent): void {
    try {
      const message = JSON.parse(event.data);
      this.messageSubject.next(message);
    } catch (error) {
      console.error('❌ Failed to parse WebSocket message:', error);
    }
  }

  private onError(error: Event): void {
    console.error('❌ WebSocket error:', error);
    this.connectionStatusSubject.next(false);
  }

  private onConnectionClose(): void {
    console.log('🔌 WebSocket disconnected');
    this.connectionStatusSubject.next(false);

    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
    }

    // Attempt to reconnect
    this.attemptReconnect();
  }

  private startHeartbeat(): void {
    // Send PING every 25 seconds (HDIM pattern)
    this.heartbeatInterval = setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        const ping = {
          type: 'PING',
          timestamp: Date.now(),
          tenantId: this.getTenantId(),
        };
        this.ws.send(JSON.stringify(ping));
      }
    }, 25000);
  }

  private attemptReconnect(): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      const backoffTime = Math.pow(2, this.reconnectAttempts) * 1000;
      console.log(`⏳ Reconnecting in ${backoffTime / 1000}s (attempt ${this.reconnectAttempts + 1}/${this.maxReconnectAttempts})`);

      setTimeout(() => {
        this.reconnectAttempts++;
        this.connect(''); // Will use stored topic
      }, backoffTime);
    } else {
      console.error('❌ Failed to reconnect after max attempts');
    }
  }

  private buildWebSocketUrl(topic: string, token: string): string {
    // Get WebSocket host from environment or window.location
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.hostname;
    const port = window.location.port ? `:${window.location.port}` : '';

    // HDIM pattern: /topic/sales-coaching/{userId}?token={jwt}
    return `${protocol}//${host}${port}/topic/sales-coaching/${this.getUserId()}?token=${token}`;
  }

  private getJwtToken(): string {
    // Try to get from localStorage or sessionStorage
    return (
      localStorage.getItem('jwt_token') ||
      sessionStorage.getItem('jwt_token') ||
      ''
    );
  }

  private getTenantId(): string {
    return localStorage.getItem('tenant_id') || 'default-tenant';
  }

  private getUserId(): string {
    // Get from URL param or localStorage
    const params = new URLSearchParams(window.location.search);
    return params.get('userId') || localStorage.getItem('user_id') || 'unknown-user';
  }
}
