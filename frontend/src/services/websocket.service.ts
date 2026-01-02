/**
 * WebSocket service for real-time evaluation progress streaming
 *
 * Features:
 * - Automatic reconnection with exponential backoff
 * - Event-based message routing
 * - Connection state management
 * - Tenant-based filtering
 */

import type { WebSocketMessage, AnyEvaluationEvent } from '../types/events';

export enum ConnectionStatus {
  CONNECTING = 'CONNECTING',
  CONNECTED = 'CONNECTED',
  DISCONNECTED = 'DISCONNECTED',
  RECONNECTING = 'RECONNECTING',
  ERROR = 'ERROR'
}

type EventHandler = (event: AnyEvaluationEvent) => void;
type StatusHandler = (status: ConnectionStatus) => void;
type ErrorHandler = (error: Error) => void;

export class WebSocketService {
  private ws: WebSocket | null = null;
  private url: string;
  private tenantId?: string;
  private authToken?: string;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 10;
  private reconnectDelay = 1000; // Start at 1 second
  private maxReconnectDelay = 30000; // Max 30 seconds
  private reconnectTimer: number | null = null;
  private pingInterval: number | null = null;
  private isManualClose = false;
  private currentStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED;

  // Event handlers
  private eventHandlers: Set<EventHandler> = new Set();
  private statusHandlers: Set<StatusHandler> = new Set();
  private errorHandlers: Set<ErrorHandler> = new Set();

  constructor(
    baseUrl: string = 'ws://localhost:8081/cql-engine',
    tenantId?: string,
    authToken?: string
  ) {
    // Only append /ws/evaluation-progress, baseUrl should include context path if needed
    this.url = `${baseUrl}/ws/evaluation-progress`;
    this.tenantId = tenantId;
    this.authToken = authToken;
  }

  /**
   * Connect to WebSocket endpoint
   */
  connect(): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      return;
    }

    this.isManualClose = false;
    this.updateStatus(ConnectionStatus.CONNECTING);

    try {
      // Build query parameters
      const params = new URLSearchParams();
      if (this.tenantId) {
        params.append('tenantId', this.tenantId);
      }
      if (this.authToken) {
        params.append('token', this.authToken);
      }

      const url = params.toString() ? `${this.url}?${params.toString()}` : this.url;

      this.ws = new WebSocket(url);

      this.ws.onopen = this.handleOpen.bind(this);
      this.ws.onmessage = this.handleMessage.bind(this);
      this.ws.onerror = this.handleError.bind(this);
      this.ws.onclose = this.handleClose.bind(this);
    } catch (error) {
      this.handleConnectionError(error as Error);
    }
  }

  /**
   * Manually disconnect from WebSocket
   */
  disconnect(): void {
    this.isManualClose = true;
    this.clearReconnectTimer();
    this.clearPingInterval();

    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }

    this.updateStatus(ConnectionStatus.DISCONNECTED);
  }

  /**
   * Subscribe to evaluation events
   */
  onEvent(handler: EventHandler): () => void {
    this.eventHandlers.add(handler);
    return () => this.eventHandlers.delete(handler);
  }

  /**
   * Subscribe to connection status changes
   */
  onStatusChange(handler: StatusHandler): () => void {
    this.statusHandlers.add(handler);
    return () => this.statusHandlers.delete(handler);
  }

  /**
   * Subscribe to errors
   */
  onError(handler: ErrorHandler): () => void {
    this.errorHandlers.add(handler);
    return () => this.errorHandlers.delete(handler);
  }

  /**
   * Get current connection status
   */
  getConnectionStatus(): ConnectionStatus {
    if (!this.ws) return ConnectionStatus.DISCONNECTED;

    switch (this.ws.readyState) {
      case WebSocket.CONNECTING:
        return this.reconnectAttempts > 0
          ? ConnectionStatus.RECONNECTING
          : ConnectionStatus.CONNECTING;
      case WebSocket.OPEN:
        return ConnectionStatus.CONNECTED;
      case WebSocket.CLOSING:
      case WebSocket.CLOSED:
        return ConnectionStatus.DISCONNECTED;
      default:
        return ConnectionStatus.ERROR;
    }
  }

  /**
   * Handle WebSocket open event
   */
  private handleOpen(): void {
    this.reconnectAttempts = 0;
    this.reconnectDelay = 1000;
    this.updateStatus(ConnectionStatus.CONNECTED);
    this.startPingInterval();
  }

  /**
   * Handle incoming WebSocket messages
   */
  private handleMessage(event: MessageEvent): void {
    try {
      const message: WebSocketMessage = JSON.parse(event.data);

      switch (message.type) {
        case 'WELCOME':
        case 'CONNECTION_ESTABLISHED':
          // Welcome message received
          break;

        case 'EVALUATION_EVENT':
          if (message.data) {
            this.notifyEventHandlers(message.data);
          }
          break;

        case 'ERROR':
          this.notifyErrorHandlers(new Error(message.message || 'Server error'));
          break;

        default:
          // Unknown message type
          break;
      }
    } catch (error) {
      this.notifyErrorHandlers(error as Error);
    }
  }

  /**
   * Handle WebSocket error
   */
  private handleError(error: Event): void {
    this.updateStatus(ConnectionStatus.ERROR);
    this.notifyErrorHandlers(new Error('WebSocket connection error'));
  }

  /**
   * Handle WebSocket close event
   */
  private handleClose(event: CloseEvent): void {
    this.clearPingInterval();
    this.updateStatus(ConnectionStatus.DISCONNECTED);

    // Attempt reconnection if not manually closed
    if (!this.isManualClose) {
      this.scheduleReconnect();
    }
  }

  /**
   * Schedule reconnection with exponential backoff
   */
  private scheduleReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      this.notifyErrorHandlers(new Error('Max reconnection attempts reached'));
      return;
    }

    this.reconnectAttempts++;
    const delay = Math.min(
      this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1),
      this.maxReconnectDelay
    );

    this.updateStatus(ConnectionStatus.RECONNECTING);

    this.reconnectTimer = window.setTimeout(() => {
      this.connect();
    }, delay);
  }

  /**
   * Start ping interval to keep connection alive
   */
  private startPingInterval(): void {
    this.clearPingInterval();

    // Send ping every 30 seconds
    this.pingInterval = window.setInterval(() => {
      // WebSocket has built-in ping/pong, no need to send custom message
    }, 30000);
  }

  /**
   * Clear reconnect timer
   */
  private clearReconnectTimer(): void {
    if (this.reconnectTimer !== null) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
  }

  /**
   * Clear ping interval
   */
  private clearPingInterval(): void {
    if (this.pingInterval !== null) {
      clearInterval(this.pingInterval);
      this.pingInterval = null;
    }
  }

  /**
   * Handle connection errors
   */
  private handleConnectionError(error: Error): void {
    this.updateStatus(ConnectionStatus.ERROR);
    this.notifyErrorHandlers(error);
    this.scheduleReconnect();
  }

  /**
   * Update connection status and notify handlers
   */
  private updateStatus(status: ConnectionStatus): void {
    // Only notify if status actually changed
    if (this.currentStatus === status) {
      return;
    }

    this.currentStatus = status;
    this.statusHandlers.forEach(handler => {
      try {
        handler(status);
      } catch (error) {
        // Handler error - silently ignore
      }
    });
  }

  /**
   * Notify event handlers
   */
  private notifyEventHandlers(event: AnyEvaluationEvent): void {
    this.eventHandlers.forEach(handler => {
      try {
        handler(event);
      } catch (error) {
        // Handler error - silently ignore
      }
    });
  }

  /**
   * Notify error handlers
   */
  private notifyErrorHandlers(error: Error): void {
    this.errorHandlers.forEach(handler => {
      try {
        handler(error);
      } catch (error) {
        // Handler error - silently ignore
      }
    });
  }

  /**
   * Update tenant ID and reconnect
   */
  setTenantId(tenantId: string): void {
    if (this.tenantId !== tenantId) {
      this.tenantId = tenantId;
      // Reconnect with new tenant ID
      if (this.ws?.readyState === WebSocket.OPEN) {
        this.disconnect();
        this.connect();
      }
    }
  }

  /**
   * Update authentication token and reconnect
   */
  setAuthToken(authToken: string): void {
    if (this.authToken !== authToken) {
      this.authToken = authToken;
      // Reconnect with new auth token
      if (this.ws?.readyState === WebSocket.OPEN) {
        this.disconnect();
        this.connect();
      }
    }
  }
}

// Singleton instance
let instance: WebSocketService | null = null;

/**
 * Get or create WebSocket service singleton
 */
export const getWebSocketService = (
  baseUrl?: string,
  tenantId?: string,
  authToken?: string
): WebSocketService => {
  if (!instance) {
    instance = new WebSocketService(baseUrl, tenantId, authToken);
  }
  return instance;
};

/**
 * Reset WebSocket service singleton (useful for testing)
 */
export const resetWebSocketService = (): void => {
  if (instance) {
    instance.disconnect();
    instance = null;
  }
};
