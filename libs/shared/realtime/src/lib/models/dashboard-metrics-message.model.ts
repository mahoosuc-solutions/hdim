import { WebSocketMessage } from './websocket-message.model';

/**
 * Real-time dashboard metrics update
 */
export interface DashboardMetricsMessage extends WebSocketMessage {
  type: 'DASHBOARD_METRICS';
  data: {
    metricType: 'quality_score' | 'patient_count' | 'care_gaps' | 'evaluations' | 'alerts';
    value: number;
    change?: number;
    changePercent?: number;
    trend?: 'up' | 'down' | 'stable';
    timeWindow: string;
  };
}

/**
 * Live analytics update
 */
export interface AnalyticsUpdateMessage extends WebSocketMessage {
  type: 'ANALYTICS_UPDATE';
  data: {
    reportId?: string;
    dataset: string;
    metrics: {
      [key: string]: number;
    };
    timestamp: number;
  };
}
