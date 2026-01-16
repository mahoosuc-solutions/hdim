import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { DataFlowStep } from '../components/evaluation-data-flow/evaluation-data-flow.component';

/**
 * Service for receiving real-time data flow steps via WebSocket
 */
@Injectable({
  providedIn: 'root',
})
export class EvaluationDataFlowService {
  private ws?: WebSocket;
  private dataFlowSubject = new Subject<DataFlowStep>();
  private connected = false;

  /**
   * Connect to WebSocket for evaluation data flow
   */
  connect(evaluationId: string, tenantId?: string): Observable<DataFlowStep> {
    if (this.ws && this.connected) {
      return this.dataFlowSubject.asObservable();
    }

    try {
      const wsUrl = this.buildWebSocketUrl(evaluationId, tenantId);
      this.ws = new WebSocket(wsUrl);

      this.ws.onopen = () => {
        console.log('[DataFlow] WebSocket connected');
        this.connected = true;
      };

      this.ws.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data);
          
          // Handle data flow step messages
          if (message.type === 'DATA_FLOW_STEP' && message.data) {
            const step: DataFlowStep = {
              stepNumber: message.data.stepNumber || 0,
              stepName: message.data.stepName || '',
              stepType: message.data.stepType || 'DATA_FETCH',
              timestamp: message.data.timestamp || new Date().toISOString(),
              resourcesAccessed: message.data.resourcesAccessed || [],
              inputData: message.data.inputData,
              outputData: message.data.outputData,
              decision: message.data.decision,
              reasoning: message.data.reasoning,
              durationMs: message.data.durationMs,
            };
            this.dataFlowSubject.next(step);
          }
        } catch (error) {
          console.error('[DataFlow] Error parsing WebSocket message:', error);
        }
      };

      this.ws.onerror = (error) => {
        console.error('[DataFlow] WebSocket error:', error);
        this.connected = false;
      };

      this.ws.onclose = () => {
        console.log('[DataFlow] WebSocket closed');
        this.connected = false;
      };
    } catch (error) {
      console.error('[DataFlow] Failed to connect WebSocket:', error);
    }

    return this.dataFlowSubject.asObservable();
  }

  /**
   * Disconnect WebSocket
   */
  disconnect(): void {
    if (this.ws) {
      this.ws.close();
      this.ws = undefined;
      this.connected = false;
    }
  }

  /**
   * Check if connected
   */
  isConnected(): boolean {
    return this.connected && this.ws?.readyState === WebSocket.OPEN;
  }

  /**
   * Build WebSocket URL
   */
  private buildWebSocketUrl(evaluationId: string, tenantId?: string): string {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.host;
    const baseUrl = `${protocol}//${host}`;
    
    let url = `${baseUrl}/ws/evaluation-progress?evaluationId=${evaluationId}`;
    if (tenantId) {
      url += `&tenantId=${tenantId}`;
    }
    
    return url;
  }
}
