import { useState, useEffect, useCallback } from 'react';
import {
  checkNotificationPermission,
  requestNotificationPermission as requestPermission,
  showNotification as showBrowserNotification,
} from '../services/notification.service';

const STORAGE_KEY = 'notificationsEnabled';

export interface UseNotificationsReturn {
  permission: NotificationPermission;
  requestPermission: () => Promise<void>;
  showNotification: (title: string, body: string, options?: NotificationOptions) => void;
  notificationsEnabled: boolean;
  toggleNotifications: () => void;
}

/**
 * React hook for managing browser notifications
 * Provides permission status, request function, and notification display
 * Persists user preference in localStorage
 */
export function useNotifications(): UseNotificationsReturn {
  const [permission, setPermission] = useState<NotificationPermission>(() =>
    checkNotificationPermission()
  );

  const [notificationsEnabled, setNotificationsEnabled] = useState<boolean>(() => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      return stored === 'true';
    } catch (error) {
      return false;
    }
  });

  // Request notification permission
  const requestNotificationPermission = useCallback(async () => {
    const newPermission = await requestPermission();
    setPermission(newPermission);
  }, []);

  // Show a notification
  const showNotification = useCallback(
    (title: string, body: string, options?: NotificationOptions) => {
      if (!notificationsEnabled || permission !== 'granted') {
        return;
      }
      showBrowserNotification(title, body, options);
    },
    [notificationsEnabled, permission]
  );

  // Toggle notifications on/off
  const toggleNotifications = useCallback(() => {
    const newValue = !notificationsEnabled;
    setNotificationsEnabled(newValue);
    try {
      localStorage.setItem(STORAGE_KEY, String(newValue));
    } catch (error) {
      console.error('Error saving notification preference:', error);
    }
  }, [notificationsEnabled]);

  // Listen for storage changes (cross-tab sync)
  useEffect(() => {
    const handleStorageChange = (event: StorageEvent) => {
      if (event.key === STORAGE_KEY) {
        try {
          const newValue = event.newValue === 'true';
          setNotificationsEnabled(newValue);
        } catch (error) {
          console.error('Error handling storage change:', error);
        }
      }
    };

    window.addEventListener('storage', handleStorageChange);
    return () => {
      window.removeEventListener('storage', handleStorageChange);
    };
  }, []);

  // Update permission status when it changes
  useEffect(() => {
    const checkPermission = () => {
      const currentPermission = checkNotificationPermission();
      // Only update if permission actually changed
      setPermission((prev) => {
        if (prev !== currentPermission) {
          return currentPermission;
        }
        return prev;
      });
    };

    // Check permission periodically in case it changes outside the app
    const interval = setInterval(checkPermission, 5000); // Check every 5 seconds instead of 1
    return () => clearInterval(interval);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // Empty deps - run once on mount, interval checks permission

  return {
    permission,
    requestPermission: requestNotificationPermission,
    showNotification,
    notificationsEnabled,
    toggleNotifications,
  };
}
