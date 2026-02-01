import { WebSocketMessage } from './websocket-message.model';

/**
 * Care gap notification message
 */
export interface CareGapNotificationMessage extends WebSocketMessage {
  type: 'CARE_GAP_NOTIFICATION';
  priority: 'medium' | 'high';
  data: {
    patientId: string;
    patientName: string;
    gapType: string;
    measureName: string;
    dueDate?: string;
    urgency: 'routine' | 'soon' | 'overdue' | 'critical';
    recommendedAction: string;
    assignedTo?: string;
  };
}

/**
 * Care gap closure notification
 */
export interface CareGapClosedMessage extends WebSocketMessage {
  type: 'CARE_GAP_CLOSED';
  data: {
    patientId: string;
    gapType: string;
    measureName: string;
    closedBy: string;
    closedAt: number;
    closureMethod: string;
  };
}
