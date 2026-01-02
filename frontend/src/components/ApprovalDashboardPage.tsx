/**
 * ApprovalDashboardPage
 *
 * Page wrapper for ApprovalDashboard that provides tenant/user context
 * and integrates WebSocket notifications for real-time updates.
 * In production, this would get the context from authentication state.
 */

import { useState, useCallback } from 'react';
import ApprovalDashboard from './ApprovalDashboard';
import { ApprovalNotificationToast } from './ApprovalNotificationToast';
import { useApprovalWebSocket } from '../hooks/useApprovalWebSocket';
import { ApprovalNotification } from '../types/approval';

export function ApprovalDashboardPage() {
  // In production, these would come from authentication context
  const [tenantId] = useState('TENANT001');
  const [userId] = useState('user-001');
  const userRole = 'CLINICAL_REVIEWER';

  const [refreshTrigger, setRefreshTrigger] = useState(0);

  const handleNotification = useCallback((notification: ApprovalNotification) => {
    // Trigger dashboard refresh when we get a notification
    setRefreshTrigger(prev => prev + 1);
    console.log('Received notification:', notification);
  }, []);

  const {
    isConnected,
    pendingCount,
    notifications,
    markNotificationRead,
  } = useApprovalWebSocket({
    tenantId,
    userId,
    role: userRole,
    onNotification: handleNotification,
    onConnect: () => console.log('WebSocket connected'),
    onDisconnect: () => console.log('WebSocket disconnected'),
    onError: (error) => console.error('WebSocket error:', error),
  });

  const handleViewDetails = (requestId: string) => {
    // For now, just dismiss and let the dashboard handle details
    markNotificationRead(requestId);
  };

  return (
    <div className="relative">
      {/* Connection status indicator */}
      <div className="fixed top-4 left-4 z-50">
        <div className={`
          flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-medium
          ${isConnected ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-600'}
        `}>
          <span className={`
            w-2 h-2 rounded-full
            ${isConnected ? 'bg-green-500 animate-pulse' : 'bg-gray-400'}
          `} />
          {isConnected ? 'Live' : 'Connecting...'}
          {isConnected && pendingCount > 0 && (
            <span className="ml-1 bg-red-500 text-white px-1.5 py-0.5 rounded-full text-xs">
              {pendingCount}
            </span>
          )}
        </div>
      </div>

      {/* Real-time notification toasts */}
      <ApprovalNotificationToast
        notifications={notifications}
        onDismiss={markNotificationRead}
        onViewDetails={handleViewDetails}
        maxVisible={5}
      />

      {/* Main dashboard */}
      <ApprovalDashboard
        tenantId={tenantId}
        userId={userId}
        key={refreshTrigger}
      />
    </div>
  );
}
