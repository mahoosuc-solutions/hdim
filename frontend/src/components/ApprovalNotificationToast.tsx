import { useState, useEffect } from 'react';
import { ApprovalNotification } from '../types/approval';

interface ApprovalNotificationToastProps {
  notifications: ApprovalNotification[];
  onDismiss: (requestId: string) => void;
  onViewDetails: (requestId: string) => void;
  maxVisible?: number;
}

const riskLevelColors: Record<string, { bg: string; border: string; icon: string }> = {
  CRITICAL: { bg: 'bg-red-50', border: 'border-red-400', icon: 'text-red-600' },
  HIGH: { bg: 'bg-orange-50', border: 'border-orange-400', icon: 'text-orange-600' },
  MEDIUM: { bg: 'bg-yellow-50', border: 'border-yellow-400', icon: 'text-yellow-600' },
  LOW: { bg: 'bg-blue-50', border: 'border-blue-400', icon: 'text-blue-600' },
};

const notificationTypeIcons: Record<string, string> = {
  CREATED: 'M12 4v16m8-8H4',
  ASSIGNED: 'M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z',
  STATUS_CHANGED: 'M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z',
  EXPIRING_SOON: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z',
};

export function ApprovalNotificationToast({
  notifications,
  onDismiss,
  onViewDetails,
  maxVisible = 5,
}: ApprovalNotificationToastProps) {
  const [visibleNotifications, setVisibleNotifications] = useState<ApprovalNotification[]>([]);
  const [dismissing, setDismissing] = useState<Set<string>>(new Set());

  useEffect(() => {
    // Only show the most recent notifications
    setVisibleNotifications(notifications.slice(0, maxVisible));
  }, [notifications, maxVisible]);

  const handleDismiss = (requestId: string) => {
    setDismissing(prev => new Set(prev).add(requestId));
    setTimeout(() => {
      onDismiss(requestId);
      setDismissing(prev => {
        const next = new Set(prev);
        next.delete(requestId);
        return next;
      });
    }, 300);
  };

  if (visibleNotifications.length === 0) {
    return null;
  }

  return (
    <div className="fixed top-4 right-4 z-50 space-y-2 max-w-md">
      {visibleNotifications.map((notification) => {
        const colors = riskLevelColors[notification.riskLevel] || riskLevelColors.LOW;
        const isDismissing = dismissing.has(notification.requestId);

        return (
          <div
            key={notification.requestId}
            className={`
              ${colors.bg} ${colors.border}
              border-l-4 rounded-lg shadow-lg p-4
              transform transition-all duration-300 ease-out
              ${isDismissing ? 'opacity-0 translate-x-full' : 'opacity-100 translate-x-0'}
            `}
          >
            <div className="flex items-start">
              <div className={`flex-shrink-0 ${colors.icon}`}>
                <svg
                  className="h-5 w-5"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d={notificationTypeIcons[notification.type] || notificationTypeIcons.STATUS_CHANGED}
                  />
                </svg>
              </div>
              <div className="ml-3 flex-1">
                <div className="flex justify-between items-start">
                  <p className="text-sm font-medium text-gray-900">
                    {notification.entityType}
                  </p>
                  <span className={`
                    inline-flex items-center px-2 py-0.5 rounded text-xs font-medium
                    ${notification.riskLevel === 'CRITICAL' ? 'bg-red-100 text-red-800' :
                      notification.riskLevel === 'HIGH' ? 'bg-orange-100 text-orange-800' :
                      notification.riskLevel === 'MEDIUM' ? 'bg-yellow-100 text-yellow-800' :
                      'bg-blue-100 text-blue-800'}
                  `}>
                    {notification.riskLevel}
                  </span>
                </div>
                <p className="mt-1 text-sm text-gray-600">
                  {notification.message}
                </p>
                <div className="mt-2 flex justify-between items-center">
                  <span className="text-xs text-gray-500">
                    {formatTimestamp(notification.timestamp)}
                  </span>
                  <div className="space-x-2">
                    <button
                      onClick={() => onViewDetails(notification.requestId)}
                      className="text-xs text-indigo-600 hover:text-indigo-800 font-medium"
                    >
                      View Details
                    </button>
                    <button
                      onClick={() => handleDismiss(notification.requestId)}
                      className="text-xs text-gray-500 hover:text-gray-700"
                    >
                      Dismiss
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        );
      })}

      {notifications.length > maxVisible && (
        <div className="text-center">
          <span className="text-sm text-gray-500">
            +{notifications.length - maxVisible} more notifications
          </span>
        </div>
      )}
    </div>
  );
}

function formatTimestamp(timestamp: string): string {
  const date = new Date(timestamp);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffSecs = Math.floor(diffMs / 1000);
  const diffMins = Math.floor(diffSecs / 60);
  const diffHours = Math.floor(diffMins / 60);

  if (diffSecs < 60) {
    return 'Just now';
  } else if (diffMins < 60) {
    return `${diffMins}m ago`;
  } else if (diffHours < 24) {
    return `${diffHours}h ago`;
  } else {
    return date.toLocaleDateString();
  }
}

export default ApprovalNotificationToast;
