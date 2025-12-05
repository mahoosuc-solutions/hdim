/**
 * Custom React hook for WebSocket integration with Zustand store
 *
 * Automatically connects/disconnects WebSocket based on component lifecycle
 * and synchronizes events with the evaluation store.
 */

import { useEffect, useRef } from 'react';
import { getWebSocketService, ConnectionStatus } from '../services/websocket.service';
import { useEvaluationStore } from '../store/evaluationStore';
import type { AnyEvaluationEvent } from '../types/events';

interface UseWebSocketOptions {
  tenantId?: string;
  autoConnect?: boolean;
  baseUrl?: string;
  authToken?: string;
}

export const useWebSocket = (options: UseWebSocketOptions = {}) => {
  const {
    tenantId,
    autoConnect = true,
    baseUrl = 'ws://localhost:8081/cql-engine',
    authToken
  } = options;

  const wsRef = useRef(getWebSocketService(baseUrl, tenantId, authToken));

  // Subscribe to WebSocket events once on mount
  useEffect(() => {
    const ws = wsRef.current;

    // Subscribe to events
    const unsubscribeEvent = ws.onEvent((event: AnyEvaluationEvent) => {
      useEvaluationStore.getState().addEvent(event);
    });

    // Subscribe to status changes
    const unsubscribeStatus = ws.onStatusChange((status: ConnectionStatus) => {
      useEvaluationStore.getState().setConnectionStatus(status);
    });

    // Subscribe to errors
    const unsubscribeError = ws.onError((error: Error) => {
      useEvaluationStore.getState().setError(error);
    });

    // Cleanup subscriptions on unmount
    return () => {
      unsubscribeEvent();
      unsubscribeStatus();
      unsubscribeError();
    };
  }, []); // Empty deps - subscribe once on mount

  // Handle connection/disconnection separately
  useEffect(() => {
    const ws = wsRef.current;

    if (autoConnect) {
      ws.connect();
    }

    // Disconnect on unmount
    return () => {
      ws.disconnect();
    };
  }, [autoConnect]); // Only reconnect if autoConnect changes

  // Handle tenant ID changes
  useEffect(() => {
    if (tenantId) {
      wsRef.current.setTenantId(tenantId);
    }
  }, [tenantId]);

  // Handle auth token changes
  useEffect(() => {
    if (authToken) {
      wsRef.current.setAuthToken(authToken);
    }
  }, [authToken]);

  return {
    connect: () => wsRef.current.connect(),
    disconnect: () => wsRef.current.disconnect(),
    setTenantId: (newTenantId: string) => wsRef.current.setTenantId(newTenantId),
    setAuthToken: (newAuthToken: string) => wsRef.current.setAuthToken(newAuthToken),
    getStatus: () => wsRef.current.getConnectionStatus()
  };
};
