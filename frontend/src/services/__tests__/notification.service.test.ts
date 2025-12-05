import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import {
  checkNotificationPermission,
  requestNotificationPermission,
  showNotification,
} from '../notification.service';

describe('notification.service', () => {
  let mockNotification: any;
  let notificationInstances: any[] = [];
  let MockNotificationConstructor: any;

  beforeEach(() => {
    // Reset notification instances
    notificationInstances = [];

    // Create a mock Notification constructor function
    MockNotificationConstructor = function(this: any, title: string, options?: NotificationOptions) {
      this.title = title;
      this.options = options;
      this.close = vi.fn();
      this.onclick = null;
      this.onclose = null;
      this.onerror = null;
      this.onshow = null;
      notificationInstances.push(this);
    };

    // Add static properties
    MockNotificationConstructor.permission = 'default';
    MockNotificationConstructor.requestPermission = vi.fn(async () => {
      return MockNotificationConstructor.permission;
    });

    mockNotification = vi.fn(MockNotificationConstructor);
    mockNotification.permission = 'default';
    mockNotification.requestPermission = vi.fn(async () => {
      return mockNotification.permission;
    });

    // Stub the global Notification
    vi.stubGlobal('Notification', mockNotification);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    notificationInstances = [];
  });

  describe('checkNotificationPermission', () => {
    it('should return current permission status', () => {
      mockNotification.permission = 'granted';
      const permission = checkNotificationPermission();
      expect(permission).toBe('granted');
    });

    it('should return default permission when not set', () => {
      mockNotification.permission = 'default';
      const permission = checkNotificationPermission();
      expect(permission).toBe('default');
    });

    it('should return denied when permission is denied', () => {
      mockNotification.permission = 'denied';
      const permission = checkNotificationPermission();
      expect(permission).toBe('denied');
    });

    it('should return default when Notification API is not available', () => {
      vi.stubGlobal('Notification', undefined);
      const permission = checkNotificationPermission();
      expect(permission).toBe('default');
    });
  });

  describe('requestNotificationPermission', () => {
    it('should return a promise that resolves to permission status', async () => {
      mockNotification.permission = 'granted';
      mockNotification.requestPermission = vi.fn(async () => 'granted');

      const permission = await requestNotificationPermission();
      expect(permission).toBe('granted');
      expect(mockNotification.requestPermission).toHaveBeenCalled();
    });

    it('should handle permission request rejection', async () => {
      mockNotification.requestPermission = vi.fn(async () => 'denied');

      const permission = await requestNotificationPermission();
      expect(permission).toBe('denied');
    });

    it('should return default when Notification API is not available', async () => {
      vi.stubGlobal('Notification', undefined);
      const permission = await requestNotificationPermission();
      expect(permission).toBe('default');
    });
  });

  describe('showNotification', () => {
    it('should do nothing if permission is denied', () => {
      mockNotification.permission = 'denied';
      showNotification('Test', 'Test body');
      expect(mockNotification).not.toHaveBeenCalled();
    });

    it('should create notification if permission is granted', () => {
      mockNotification.permission = 'granted';
      showNotification('Test Title', 'Test Body');

      expect(mockNotification).toHaveBeenCalledWith('Test Title', {
        body: 'Test Body',
      });
      expect(notificationInstances).toHaveLength(1);
    });

    it('should pass options to Notification constructor', () => {
      mockNotification.permission = 'granted';
      const options = {
        icon: '/icon.png',
        tag: 'test-tag',
        requireInteraction: true,
      };

      showNotification('Test', 'Body', options);

      expect(mockNotification).toHaveBeenCalledWith('Test', {
        body: 'Body',
        ...options,
      });
    });

    it('should handle Notification API not available (SSR/old browsers)', () => {
      vi.stubGlobal('Notification', undefined);

      // Should not throw
      expect(() => {
        showNotification('Test', 'Body');
      }).not.toThrow();
    });

    it('should allow multiple notifications to be shown', () => {
      mockNotification.permission = 'granted';

      showNotification('First', 'First body');
      showNotification('Second', 'Second body');
      showNotification('Third', 'Third body');

      expect(mockNotification).toHaveBeenCalledTimes(3);
      expect(notificationInstances).toHaveLength(3);
      expect(notificationInstances[0].title).toBe('First');
      expect(notificationInstances[1].title).toBe('Second');
      expect(notificationInstances[2].title).toBe('Third');
    });
  });
});
