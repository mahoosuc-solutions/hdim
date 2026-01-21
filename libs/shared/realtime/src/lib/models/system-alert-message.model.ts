import { WebSocketMessage } from './websocket-message.model';

/**
 * Clinical alert message
 */
export interface ClinicalAlertMessage extends WebSocketMessage {
  type: 'CLINICAL_ALERT';
  priority: 'high' | 'critical';
  data: {
    patientId?: string;
    alertType: 'medication' | 'lab_result' | 'vital_sign' | 'diagnosis' | 'other';
    severity: 'warning' | 'urgent' | 'emergency';
    title: string;
    message: string;
    actionRequired: boolean;
    actionUrl?: string;
    expiresAt?: number;
  };
}

/**
 * System status notification
 */
export interface SystemStatusMessage extends WebSocketMessage {
  type: 'SYSTEM_STATUS';
  priority: 'low' | 'medium';
  data: {
    status: 'operational' | 'degraded' | 'maintenance' | 'outage';
    service?: string;
    message: string;
    estimatedResolution?: number;
  };
}

/**
 * Connection established welcome message
 */
export interface ConnectionEstablishedMessage extends WebSocketMessage {
  type: 'CONNECTION_ESTABLISHED';
  sessionId: string;
  username: string;
  sessionTimeoutMinutes: number;
  authenticated: boolean;
  message: string;
}

/**
 * Heartbeat/ping message
 */
export interface HeartbeatMessage extends WebSocketMessage {
  type: 'PING' | 'PONG';
}
