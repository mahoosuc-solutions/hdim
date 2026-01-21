import { Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import {
  WebSocketMessage,
  ConnectionState,
  ConnectionStatus,
  WebSocketConfig,
  isWebSocketMessage
} from '../models/websocket-message.model';
import { WebSocketMessageQueue } from './websocket-message-queue';

/**
 * Shared WebSocket Service
 *
 * Provides real-time communication with backend WebSocket endpoints.
 *
 * Features:
 * - Automatic reconnection with exponential backoff
 * - JWT authentication integration
 * - Multi-tenant support
 * - Message queuing during disconnection
 * - Type-safe message streams
 * - Connection state management
 *
 * Usage:
 * ```typescript
 * constructor(private websocketService: WebSocketService) {}
 *
 * // Connect to quality measure endpoint
 * this.websocketService.connect('quality-measure');
 *
 * // Subscribe to health score updates
 * this.websocketService.ofType<HealthScoreUpdateMessage>('HEALTH_SCORE_UPDATE')
 *   .subscribe(msg => console.log('Health score:', msg.data.score));
 * ```
 */
@Injectable({
  providedIn: 'root'
})
export class WebSocketService implements OnDestroy {
  private messageSubject = new Subject<WebSocketMessage>();
  private connectionStatusSubject = new BehaviorSubject<ConnectionStatus>({
    state: ConnectionState.DISCONNECTED,
    retryCount: 0
  });

  private socket: WebSocket | null = null;
  private reconnectAttempts = 0;
  private heartbeatTimer: any;
  private reconnectTimer: any;
  private messageQueue: WebSocketMessageQueue;
  private config: WebSocketConfig | null = null;
  private authToken: string | null = null;

  /**
   * Observable stream of all incoming WebSocket messages
   * Filter by type using the ofType operator
   */
  public readonly messages$ = this.messageSubject.asObservable();

  /**
   * Observable stream of connection status changes
   */
  public readonly connectionStatus$ = this.connectionStatusSubject.asObservable();

  /**
   * Get current connection state synchronously
   */
  public get isConnected(): boolean {
    return this.connectionStatusSubject.value.state === ConnectionState.CONNECTED;
  }

  constructor() {
    this.messageQueue = new WebSocketMessageQueue(100);
  }

  /**
   * Connect to a WebSocket endpoint
   *
   * @param endpoint - Backend service endpoint ('quality-measure', 'fhir', 'migration', etc.)
   * @param token - JWT auth token
   * @param config - Optional WebSocket configuration
   */
  connect(endpoint: string, token: string, config?: Partial<WebSocketConfig>): void {
    if (this.socket) {
      console.warn('WebSocket already connected. Disconnect first.');
      return;
    }

    if (!token) {
      console.error('Cannot connect to WebSocket: No auth token provided');
      this.updateConnectionStatus({
        state: ConnectionState.ERROR,
        lastError: 'Authentication required',
        retryCount: 0
      });
      return;
    }

    this.authToken = token;
    this.config = this.buildConfig(endpoint, config);

    this.performConnect();
  }

  /**
   * Disconnect from WebSocket
   */
  disconnect(): void {
    this.stopHeartbeat();
    this.stopReconnect();

    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }

    this.updateConnectionStatus({
      state: ConnectionState.DISCONNECTED,
      retryCount: 0
    });
  }

  /**
   * Send a message to the WebSocket server
   *
   * @param message - Message to send
   */
  send<T extends WebSocketMessage>(message: T): void {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify(message));
    } else {
      // Queue message for later delivery
      this.messageQueue.enqueue(message);
    }
  }

  /**
   * Get messages of a specific type
   *
   * @param type - Message type to filter
   */
  ofType<T extends WebSocketMessage>(type: string): Observable<T> {
    return this.messages$.pipe(
      filter(msg => msg.type === type),
      map(msg => msg as T)
    );
  }

  /**
   * Get messages for a specific tenant
   *
   * @param tenantId - Tenant ID to filter
   */
  forTenant(tenantId: string): Observable<WebSocketMessage> {
    return this.messages$.pipe(
      filter(msg => !msg.tenantId || msg.tenantId === tenantId)
    );
  }

  ngOnDestroy(): void {
    this.disconnect();
    this.messageSubject.complete();
    this.connectionStatusSubject.complete();
  }

  // Private helper methods

  private buildConfig(endpoint: string, overrides?: Partial<WebSocketConfig>): WebSocketConfig {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.hostname;
    const port = window.location.port || (protocol === 'wss:' ? '443' : '80');

    // Map endpoint to WebSocket path
    const wsPathMap: { [key: string]: string } = {
      'quality-measure': '/quality-measure/ws/health-scores',
      'fhir': '/fhir/ws/subscriptions',
      'migration': '/migration/ws/progress',
      'clinical-workflow': '/clinical-workflow/ws/updates'
    };

    const wsPath = wsPathMap[endpoint] || `/ws/${endpoint}`;
    const url = `${protocol}//${host}:${port}${wsPath}`;

    return {
      url,
      reconnectInterval: overrides?.reconnectInterval ?? 1000,
      maxReconnectAttempts: overrides?.maxReconnectAttempts ?? 10,
      heartbeatInterval: overrides?.heartbeatInterval ?? 30000,
      messageQueueSize: overrides?.messageQueueSize ?? 100
    };
  }

  private performConnect(): void {
    if (!this.config || !this.authToken) {
      return;
    }

    this.updateConnectionStatus({ state: ConnectionState.CONNECTING });

    try {
      // Add token as query parameter
      const urlWithToken = `${this.config.url}?token=${encodeURIComponent(this.authToken)}`;
      this.socket = new WebSocket(urlWithToken);

      this.socket.onopen = () => this.handleOpen();
      this.socket.onmessage = (event) => this.handleMessage(event);
      this.socket.onerror = (error) => this.handleError(error);
      this.socket.onclose = () => this.handleClose();
    } catch (error) {
      console.error('WebSocket connection error:', error);
      this.scheduleReconnect();
    }
  }

  private handleOpen(): void {
    console.log('WebSocket connected');
    this.reconnectAttempts = 0;
    this.updateConnectionStatus({
      state: ConnectionState.CONNECTED,
      lastConnected: Date.now(),
      retryCount: 0
    });

    this.startHeartbeat();
    this.flushMessageQueue();
  }

  private handleMessage(event: MessageEvent): void {
    try {
      const message: WebSocketMessage = JSON.parse(event.data);
      if (isWebSocketMessage(message)) {
        this.messageSubject.next(message);
      }
    } catch (error) {
      console.error('Failed to parse WebSocket message:', error);
    }
  }

  private handleError(error: Event): void {
    console.error('WebSocket error:', error);
    this.updateConnectionStatus({
      state: ConnectionState.ERROR,
      lastError: 'Connection error'
    });
  }

  private handleClose(): void {
    console.log('WebSocket closed');
    this.stopHeartbeat();

    this.updateConnectionStatus({
      state: ConnectionState.DISCONNECTED
    });

    // Attempt reconnection if not max attempts
    if (
      this.reconnectAttempts < ((this.config?.maxReconnectAttempts) ?? 10)
    ) {
      this.scheduleReconnect();
    }
  }

  private scheduleReconnect(): void {
    if (!this.config) {
      return;
    }

    const baseDelay = this.config.reconnectInterval ?? 1000;
    const delay = baseDelay * Math.pow(2, this.reconnectAttempts); // Exponential backoff
    const maxDelay = 30000; // Max 30 seconds
    const actualDelay = Math.min(delay, maxDelay);

    console.log(`Reconnecting in ${actualDelay}ms (attempt ${this.reconnectAttempts + 1})`);

    this.updateConnectionStatus({
      state: ConnectionState.RECONNECTING,
      retryCount: this.reconnectAttempts + 1
    });

    this.reconnectTimer = setTimeout(() => {
      this.reconnectAttempts++;
      this.performConnect();
    }, actualDelay);
  }

  private startHeartbeat(): void {
    if (!this.config) {
      return;
    }

    const interval = this.config.heartbeatInterval ?? 30000;

    this.heartbeatTimer = setInterval(() => {
      if (this.socket?.readyState === WebSocket.OPEN) {
        // Send ping message
        this.send({
          type: 'PING',
          timestamp: Date.now()
        } as WebSocketMessage);
      }
    }, interval);
  }

  private stopHeartbeat(): void {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }

  private stopReconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }

  private flushMessageQueue(): void {
    if (this.socket?.readyState === WebSocket.OPEN) {
      const queuedMessages = this.messageQueue.dequeueAll();
      queuedMessages.forEach(msg => this.send(msg));
    }
  }

  private updateConnectionStatus(status: Partial<ConnectionStatus>): void {
    const currentStatus = this.connectionStatusSubject.value;
    this.connectionStatusSubject.next({
      ...currentStatus,
      ...status
    });
  }
}
