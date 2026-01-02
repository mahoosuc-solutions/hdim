import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useNotifications } from '../useNotifications';

describe('useNotifications', () => {
  let mockNotification: any;
  let mockLocalStorage: any;

  beforeEach(() => {
    // Mock localStorage
    mockLocalStorage = {
      getItem: vi.fn(),
      setItem: vi.fn(),
      removeItem: vi.fn(),
      clear: vi.fn(),
    };
    vi.stubGlobal('localStorage', mockLocalStorage);

    // Create mock Notification
    mockNotification = vi.fn();
    mockNotification.permission = 'default';
    mockNotification.requestPermission = vi.fn(async () => {
      return mockNotification.permission;
    });
    vi.stubGlobal('Notification', mockNotification);

    // Mock addEventListener and removeEventListener for storage events
    vi.stubGlobal('addEventListener', vi.fn());
    vi.stubGlobal('removeEventListener', vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    mockLocalStorage.getItem.mockClear();
    mockLocalStorage.setItem.mockClear();
  });

  describe('initialization', () => {
    it('should return permission status on mount', () => {
      mockNotification.permission = 'granted';
      const { result } = renderHook(() => useNotifications());

      expect(result.current.permission).toBe('granted');
    });

    it('should check localStorage for notification preference on mount', () => {
      mockLocalStorage.getItem.mockReturnValue('true');
      const { result } = renderHook(() => useNotifications());

      expect(mockLocalStorage.getItem).toHaveBeenCalledWith('notificationsEnabled');
    });

    it('should have default state as not enabled when localStorage is empty', () => {
      mockLocalStorage.getItem.mockReturnValue(null);
      mockNotification.permission = 'default';

      const { result } = renderHook(() => useNotifications());

      expect(result.current.notificationsEnabled).toBe(false);
    });

    it('should handle invalid localStorage data gracefully', () => {
      mockLocalStorage.getItem.mockReturnValue('invalid-json');

      const { result } = renderHook(() => useNotifications());

      // Should not crash and should default to false
      expect(result.current.notificationsEnabled).toBe(false);
    });
  });

  describe('requestPermission', () => {
    it('should update permission state after request', async () => {
      mockNotification.permission = 'default';
      mockNotification.requestPermission = vi.fn(async () => {
        mockNotification.permission = 'granted';
        return 'granted';
      });

      const { result } = renderHook(() => useNotifications());

      expect(result.current.permission).toBe('default');

      await act(async () => {
        await result.current.requestPermission();
      });

      expect(result.current.permission).toBe('granted');
      expect(mockNotification.requestPermission).toHaveBeenCalled();
    });

    it('should handle permission denied', async () => {
      mockNotification.requestPermission = vi.fn(async () => {
        mockNotification.permission = 'denied';
        return 'denied';
      });

      const { result } = renderHook(() => useNotifications());

      await act(async () => {
        await result.current.requestPermission();
      });

      expect(result.current.permission).toBe('denied');
    });

    it('should handle permission already granted', async () => {
      mockNotification.permission = 'granted';
      mockNotification.requestPermission = vi.fn(async () => 'granted');

      const { result } = renderHook(() => useNotifications());

      expect(result.current.permission).toBe('granted');

      await act(async () => {
        await result.current.requestPermission();
      });

      expect(result.current.permission).toBe('granted');
    });
  });

  describe('showNotification', () => {
    it('should call service function when showing notification', () => {
      mockNotification.permission = 'granted';
      mockLocalStorage.getItem.mockReturnValue('true');

      const { result } = renderHook(() => useNotifications());

      act(() => {
        result.current.showNotification('Test', 'Test body');
      });

      expect(mockNotification).toHaveBeenCalledWith('Test', {
        body: 'Test body',
      });
    });

    it('should not show notification when disabled in localStorage', () => {
      mockNotification.permission = 'granted';
      mockLocalStorage.getItem.mockReturnValue('false');

      const { result } = renderHook(() => useNotifications());

      act(() => {
        result.current.showNotification('Test', 'Test body');
      });

      expect(mockNotification).not.toHaveBeenCalled();
    });

    it('should not show notification when permission is denied', () => {
      mockNotification.permission = 'denied';
      mockLocalStorage.getItem.mockReturnValue('true');

      const { result } = renderHook(() => useNotifications());

      act(() => {
        result.current.showNotification('Test', 'Test body');
      });

      expect(mockNotification).not.toHaveBeenCalled();
    });
  });

  describe('toggleNotifications', () => {
    it('should update localStorage when toggling', () => {
      mockLocalStorage.getItem.mockReturnValue('false');
      mockNotification.permission = 'granted';

      const { result } = renderHook(() => useNotifications());

      expect(result.current.notificationsEnabled).toBe(false);

      act(() => {
        result.current.toggleNotifications();
      });

      expect(mockLocalStorage.setItem).toHaveBeenCalledWith('notificationsEnabled', 'true');
      expect(result.current.notificationsEnabled).toBe(true);
    });

    it('should reflect permission and localStorage state', () => {
      mockLocalStorage.getItem.mockReturnValue('true');
      mockNotification.permission = 'granted';

      const { result } = renderHook(() => useNotifications());

      expect(result.current.notificationsEnabled).toBe(true);
    });
  });

  describe('cross-tab synchronization', () => {
    it('should add storage event listener on mount', () => {
      renderHook(() => useNotifications());

      expect(window.addEventListener).toHaveBeenCalledWith(
        'storage',
        expect.any(Function)
      );
    });

    it('should remove event listener on cleanup', () => {
      const { unmount } = renderHook(() => useNotifications());

      unmount();

      expect(window.removeEventListener).toHaveBeenCalledWith(
        'storage',
        expect.any(Function)
      );
    });
  });
});
