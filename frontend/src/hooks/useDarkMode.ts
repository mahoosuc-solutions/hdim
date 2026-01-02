/**
 * useDarkMode Hook
 *
 * Custom hook for managing dark mode state with:
 * - LocalStorage persistence
 * - System preference detection
 * - Cross-tab synchronization
 * - MUI theme integration
 */

import { useState, useEffect, useCallback } from 'react';

const DARK_MODE_KEY = 'darkMode';

interface UseDarkModeReturn {
  isDarkMode: boolean;
  toggleDarkMode: () => void;
  setDarkMode: (value: boolean) => void;
}

/**
 * Detect system preference for dark mode
 */
function getSystemPreference(): boolean {
  if (typeof window === 'undefined') return false;

  return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
}

/**
 * Get initial dark mode value from localStorage or system preference
 */
function getInitialDarkMode(): boolean {
  if (typeof window === 'undefined') return false;

  // Try to read from localStorage
  const stored = localStorage.getItem(DARK_MODE_KEY);

  if (stored !== null) {
    // Handle valid values
    if (stored === 'true') return true;
    if (stored === 'false') return false;

    // Invalid value - fall through to system preference
  }

  // Fall back to system preference
  return getSystemPreference();
}

/**
 * Hook for managing dark mode state
 */
export function useDarkMode(): UseDarkModeReturn {
  const [isDarkMode, setIsDarkMode] = useState<boolean>(getInitialDarkMode);

  /**
   * Set dark mode to a specific value
   */
  const setDarkMode = useCallback((value: boolean) => {
    setIsDarkMode(value);
    localStorage.setItem(DARK_MODE_KEY, String(value));
  }, []);

  /**
   * Toggle dark mode on/off
   */
  const toggleDarkMode = useCallback(() => {
    setIsDarkMode((prev) => {
      const newValue = !prev;
      localStorage.setItem(DARK_MODE_KEY, String(newValue));
      return newValue;
    });
  }, []);

  /**
   * Handle storage events for cross-tab synchronization
   */
  useEffect(() => {
    const handleStorageChange = (e: StorageEvent) => {
      if (e.key === DARK_MODE_KEY && e.newValue !== null) {
        const newValue = e.newValue === 'true';
        setIsDarkMode(newValue);
      }
    };

    window.addEventListener('storage', handleStorageChange);

    return () => {
      window.removeEventListener('storage', handleStorageChange);
    };
  }, []);

  return {
    isDarkMode,
    toggleDarkMode,
    setDarkMode,
  };
}
