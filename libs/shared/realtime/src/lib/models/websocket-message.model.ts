/**
 * WebSocket Message Models
 * Type-safe message contracts for real-time HDIM communication
 */

/**
 * Base WebSocket message interface
 * All WebSocket messages must extend this interface
 */
export interface WebSocketMessage {
  type: string;
  timestamp: number;
  tenantId?: string;
  priority?: 'low' | 'medium' | 'high' | 'critical';
}

/**
 * Connection state enum
 */
export enum ConnectionState {
  DISCONNECTED = 'DISCONNECTED',
  CONNECTING = 'CONNECTING',
  CONNECTED = 'CONNECTED',
  RECONNECTING = 'RECONNECTING',
  ERROR = 'ERROR'
}

/**
 * Connection status interface
 */
export interface ConnectionStatus {
  state: ConnectionState;
  lastConnected?: number;
  lastError?: string;
  retryCount: number;
  sessionId?: string;
}

/**
 * WebSocket configuration options
 */
export interface WebSocketConfig {
  url: string;
  reconnectInterval?: number;    // Default: 1000ms
  maxReconnectAttempts?: number; // Default: 10
  heartbeatInterval?: number;    // Default: 30000ms (30s)
  messageQueueSize?: number;     // Default: 100
}

/**
 * Type guard for WebSocketMessage
 */
export function isWebSocketMessage(obj: any): obj is WebSocketMessage {
  return (
    obj &&
    typeof obj === 'object' &&
    typeof obj.type === 'string' &&
    typeof obj.timestamp === 'number'
  );
}
