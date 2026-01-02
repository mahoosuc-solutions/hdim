/**
 * Browser notification service
 * Provides functions to check, request, and show browser notifications
 */

/**
 * Check if Notification API is available
 */
function isNotificationSupported(): boolean {
  return typeof window !== 'undefined' && 'Notification' in window && typeof window.Notification !== 'undefined';
}

/**
 * Check current notification permission status
 * @returns Current permission status ('default' | 'granted' | 'denied')
 */
export function checkNotificationPermission(): NotificationPermission {
  if (!isNotificationSupported()) {
    return 'default';
  }
  return Notification.permission;
}

/**
 * Request notification permission from the user
 * @returns Promise resolving to the permission status
 */
export async function requestNotificationPermission(): Promise<NotificationPermission> {
  if (!isNotificationSupported()) {
    return 'default';
  }

  try {
    const permission = await Notification.requestPermission();
    return permission;
  } catch (error) {
    console.error('Error requesting notification permission:', error);
    return 'default';
  }
}

/**
 * Show a browser notification
 * @param title - Notification title
 * @param body - Notification body text
 * @param options - Additional notification options
 */
export function showNotification(
  title: string,
  body: string,
  options?: NotificationOptions
): void {
  if (!isNotificationSupported()) {
    console.warn('Notification API is not supported in this browser');
    return;
  }

  if (Notification.permission !== 'granted') {
    console.warn('Notification permission not granted');
    return;
  }

  try {
    new Notification(title, {
      body,
      ...options,
    });
  } catch (error) {
    console.error('Error showing notification:', error);
  }
}
