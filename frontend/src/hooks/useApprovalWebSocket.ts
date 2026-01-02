import { useEffect, useRef, useCallback, useState } from 'react';
import SockJS from 'sockjs-client';
import { Client, Message, IFrame } from '@stomp/stompjs';
import { ApprovalNotification } from '../types/approval';

interface UseApprovalWebSocketOptions {
  tenantId: string;
  userId?: string;
  role?: string;
  onNotification?: (notification: ApprovalNotification) => void;
  onConnect?: () => void;
  onDisconnect?: () => void;
  onError?: (error: Error) => void;
  autoReconnect?: boolean;
  reconnectDelay?: number;
}

interface SubscriptionResponse {
  status: string;
  tenantId: string;
  pendingCount: number;
  message: string;
}

const DEFAULT_WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8093/ws/approvals';

export function useApprovalWebSocket({
  tenantId,
  userId,
  role,
  onNotification,
  onConnect,
  onDisconnect,
  onError,
  autoReconnect = true,
  reconnectDelay = 5000,
}: UseApprovalWebSocketOptions) {
  const clientRef = useRef<Client | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [pendingCount, setPendingCount] = useState(0);
  const [notifications, setNotifications] = useState<ApprovalNotification[]>([]);
  const reconnectTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const connect = useCallback(() => {
    if (clientRef.current?.active) {
      return;
    }

    const client = new Client({
      webSocketFactory: () => new SockJS(DEFAULT_WS_URL),
      reconnectDelay: autoReconnect ? reconnectDelay : 0,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: (_frame: IFrame) => {
        setIsConnected(true);
        onConnect?.();

        // Subscribe to tenant-wide notifications
        client.subscribe(`/topic/tenant/${tenantId}/approvals`, (message: Message) => {
          handleNotification(message);
        });

        // Subscribe to user-specific notifications if userId is provided
        if (userId) {
          client.subscribe(`/queue/user/${userId}/approvals`, (message: Message) => {
            handleNotification(message);
          });
        }

        // Subscribe to role-specific notifications if role is provided
        if (role) {
          client.subscribe(`/topic/tenant/${tenantId}/role/${role}/approvals`, (message: Message) => {
            handleNotification(message);
          });
        }
      },
      onDisconnect: () => {
        setIsConnected(false);
        onDisconnect?.();
      },
      onStompError: (frame: IFrame) => {
        const error = new Error(frame.headers?.message || 'STOMP error');
        onError?.(error);
      },
      onWebSocketError: (_event: Event) => {
        const error = new Error('WebSocket error');
        onError?.(error);
      },
    });

    clientRef.current = client;
    client.activate();
  }, [tenantId, userId, role, onConnect, onDisconnect, onError, autoReconnect, reconnectDelay]);

  const handleNotification = useCallback((message: Message) => {
    try {
      const notification: ApprovalNotification = JSON.parse(message.body);
      setNotifications(prev => [notification, ...prev].slice(0, 100)); // Keep last 100
      onNotification?.(notification);

      // Update pending count based on notification type
      if (notification.type === 'CREATED') {
        setPendingCount(prev => prev + 1);
      } else if (notification.type === 'STATUS_CHANGED' &&
                 ['APPROVED', 'REJECTED', 'EXPIRED'].includes(notification.status)) {
        setPendingCount(prev => Math.max(0, prev - 1));
      }
    } catch (err) {
      console.error('Failed to parse notification:', err);
    }
  }, [onNotification]);

  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
    }
    if (clientRef.current) {
      clientRef.current.deactivate();
      clientRef.current = null;
    }
    setIsConnected(false);
  }, []);

  const sendQuickAction = useCallback(async (
    requestId: string,
    action: 'APPROVE' | 'REJECT' | 'ESCALATE',
    reason?: string,
    escalateTo?: string
  ): Promise<{ success: boolean; newStatus?: string; error?: string }> => {
    if (!clientRef.current?.active) {
      return { success: false, error: 'Not connected' };
    }

    return new Promise((resolve) => {
      const payload = {
        requestId,
        tenantId,
        action,
        actorId: userId,
        reason,
        escalateTo,
      };

      clientRef.current!.publish({
        destination: '/app/approval/quickAction',
        body: JSON.stringify(payload),
      });

      // For now, assume success - in production you'd want to wait for a response
      resolve({ success: true });
    });
  }, [tenantId, userId]);

  const clearNotifications = useCallback(() => {
    setNotifications([]);
  }, []);

  const markNotificationRead = useCallback((requestId: string) => {
    setNotifications(prev => prev.filter(n => n.requestId !== requestId));
  }, []);

  useEffect(() => {
    connect();
    return () => {
      disconnect();
    };
  }, [connect, disconnect]);

  return {
    isConnected,
    pendingCount,
    notifications,
    connect,
    disconnect,
    sendQuickAction,
    clearNotifications,
    markNotificationRead,
  };
}

export type { ApprovalNotification, SubscriptionResponse };
