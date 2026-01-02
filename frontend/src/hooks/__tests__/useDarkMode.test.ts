/**
 * Tests for useDarkMode hook
 *
 * Test-Driven Development (TDD) - Tests written FIRST
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { useDarkMode } from '../useDarkMode';

describe('useDarkMode', () => {
  beforeEach(() => {
    // Clear localStorage before each test
    localStorage.removeItem('darkMode');
    // Clear all mocks
    vi.clearAllMocks();
  });

  afterEach(() => {
    localStorage.removeItem('darkMode');
  });

  describe('Basic Functionality', () => {
    it('returns isDarkMode boolean', () => {
      const { result } = renderHook(() => useDarkMode());

      expect(typeof result.current.isDarkMode).toBe('boolean');
    });

    it('returns toggleDarkMode function', () => {
      const { result } = renderHook(() => useDarkMode());

      expect(typeof result.current.toggleDarkMode).toBe('function');
    });

    it('returns setDarkMode function', () => {
      const { result } = renderHook(() => useDarkMode());

      expect(typeof result.current.setDarkMode).toBe('function');
    });

    it('toggleDarkMode switches value from false to true', () => {
      const { result } = renderHook(() => useDarkMode());
      const initialValue = result.current.isDarkMode;

      act(() => {
        result.current.toggleDarkMode();
      });

      expect(result.current.isDarkMode).toBe(!initialValue);
    });

    it('toggleDarkMode switches value from true to false', () => {
      const { result } = renderHook(() => useDarkMode());

      // First toggle to ensure we're in dark mode
      act(() => {
        result.current.setDarkMode(true);
      });

      expect(result.current.isDarkMode).toBe(true);

      // Toggle back
      act(() => {
        result.current.toggleDarkMode();
      });

      expect(result.current.isDarkMode).toBe(false);
    });

    it('setDarkMode sets specific value to true', () => {
      const { result } = renderHook(() => useDarkMode());

      act(() => {
        result.current.setDarkMode(true);
      });

      expect(result.current.isDarkMode).toBe(true);
    });

    it('setDarkMode sets specific value to false', () => {
      const { result } = renderHook(() => useDarkMode());

      act(() => {
        result.current.setDarkMode(false);
      });

      expect(result.current.isDarkMode).toBe(false);
    });
  });

  describe('LocalStorage Persistence', () => {
    it('reads from localStorage on mount when value is true', () => {
      localStorage.setItem('darkMode', 'true');

      const { result } = renderHook(() => useDarkMode());

      expect(result.current.isDarkMode).toBe(true);
    });

    it('reads from localStorage on mount when value is false', () => {
      localStorage.setItem('darkMode', 'false');

      const { result } = renderHook(() => useDarkMode());

      expect(result.current.isDarkMode).toBe(false);
    });

    it('writes to localStorage when dark mode is enabled', () => {
      const { result } = renderHook(() => useDarkMode());

      act(() => {
        result.current.setDarkMode(true);
      });

      expect(localStorage.getItem('darkMode')).toBe('true');
    });

    it('writes to localStorage when dark mode is disabled', () => {
      const { result } = renderHook(() => useDarkMode());

      act(() => {
        result.current.setDarkMode(false);
      });

      expect(localStorage.getItem('darkMode')).toBe('false');
    });

    it('writes to localStorage on toggle', () => {
      const { result } = renderHook(() => useDarkMode());
      const initialValue = result.current.isDarkMode;

      act(() => {
        result.current.toggleDarkMode();
      });

      const expectedValue = !initialValue;
      expect(localStorage.getItem('darkMode')).toBe(String(expectedValue));
    });
  });

  describe('System Preference Detection', () => {
    it('defaults to system preference when no localStorage value', () => {
      // Mock matchMedia to return dark mode preference
      const mockMatchMedia = vi.fn().mockImplementation((query) => ({
        matches: query === '(prefers-color-scheme: dark)',
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      }));

      vi.stubGlobal('matchMedia', mockMatchMedia);

      const { result } = renderHook(() => useDarkMode());

      expect(result.current.isDarkMode).toBe(true);

      vi.unstubAllGlobals();
    });

    it('defaults to light mode when no localStorage and no system preference', () => {
      // Mock matchMedia to return light mode preference
      const mockMatchMedia = vi.fn().mockImplementation((query) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      }));

      vi.stubGlobal('matchMedia', mockMatchMedia);

      const { result } = renderHook(() => useDarkMode());

      expect(result.current.isDarkMode).toBe(false);

      vi.unstubAllGlobals();
    });

    it('localStorage value overrides system preference', () => {
      localStorage.setItem('darkMode', 'false');

      // Mock matchMedia to return dark mode preference
      const mockMatchMedia = vi.fn().mockImplementation((query) => ({
        matches: query === '(prefers-color-scheme: dark)',
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      }));

      vi.stubGlobal('matchMedia', mockMatchMedia);

      const { result } = renderHook(() => useDarkMode());

      // Should use localStorage value (false) instead of system preference (true)
      expect(result.current.isDarkMode).toBe(false);

      vi.unstubAllGlobals();
    });
  });

  describe('Invalid Data Handling', () => {
    it('handles invalid localStorage values gracefully', () => {
      localStorage.setItem('darkMode', 'invalid-value');

      const { result } = renderHook(() => useDarkMode());

      // Should default to false for invalid values
      expect(typeof result.current.isDarkMode).toBe('boolean');
    });

    it('handles corrupted localStorage gracefully', () => {
      // Set a non-string value (should not normally happen)
      localStorage.setItem('darkMode', '{"corrupted": true}');

      const { result } = renderHook(() => useDarkMode());

      // Should still return a boolean
      expect(typeof result.current.isDarkMode).toBe('boolean');
    });
  });

  describe('Cross-Tab Synchronization', () => {
    it('updates when storage event is received from another tab', async () => {
      const { result } = renderHook(() => useDarkMode());

      // Set initial state
      act(() => {
        result.current.setDarkMode(false);
      });

      expect(result.current.isDarkMode).toBe(false);

      // Simulate storage event from another tab
      act(() => {
        const storageEvent = new StorageEvent('storage', {
          key: 'darkMode',
          newValue: 'true',
          oldValue: 'false',
        });
        window.dispatchEvent(storageEvent);
      });

      await waitFor(() => {
        expect(result.current.isDarkMode).toBe(true);
      });
    });

    it('ignores storage events for other keys', async () => {
      const { result } = renderHook(() => useDarkMode());

      act(() => {
        result.current.setDarkMode(false);
      });

      const initialValue = result.current.isDarkMode;

      // Simulate storage event for different key
      act(() => {
        const storageEvent = new StorageEvent('storage', {
          key: 'otherKey',
          newValue: 'true',
          oldValue: 'false',
        });
        window.dispatchEvent(storageEvent);
      });

      // Wait a bit to ensure no update
      await new Promise(resolve => setTimeout(resolve, 100));

      expect(result.current.isDarkMode).toBe(initialValue);
    });

    it('cleans up storage event listener on unmount', () => {
      const removeEventListenerSpy = vi.spyOn(window, 'removeEventListener');

      const { unmount } = renderHook(() => useDarkMode());

      unmount();

      expect(removeEventListenerSpy).toHaveBeenCalledWith(
        'storage',
        expect.any(Function)
      );

      removeEventListenerSpy.mockRestore();
    });
  });
});
