import { WebSocketMessage } from './websocket-message.model';

/**
 * Health score update message from quality-measure-service
 */
export interface HealthScoreUpdateMessage extends WebSocketMessage {
  type: 'HEALTH_SCORE_UPDATE';
  data: {
    patientId: string;
    score: number;
    previousScore?: number;
    category: 'excellent' | 'good' | 'fair' | 'poor' | 'critical';
    factors: {
      name: string;
      impact: number;
    }[];
    calculatedAt: number;
  };
}

/**
 * Significant health score change alert
 */
export interface HealthScoreChangeMessage extends WebSocketMessage {
  type: 'SIGNIFICANT_CHANGE';
  priority: 'high' | 'critical';
  data: {
    patientId: string;
    currentScore: number;
    previousScore: number;
    changePercent: number;
    trend: 'improving' | 'declining';
    recommendation?: string;
  };
}

/**
 * Quality measure evaluation progress
 */
export interface EvaluationProgressMessage extends WebSocketMessage {
  type: 'EVALUATION_PROGRESS';
  data: {
    evaluationId: string;
    measureId: string;
    patientCount: number;
    processed: number;
    percentComplete: number;
    status: 'running' | 'completed' | 'failed';
    estimatedTimeRemaining?: number;
  };
}
