import { useState, useEffect } from 'react';

export interface UserSettings {
  theme: 'auto' | 'light' | 'dark';
  searchDebounceMs: number;
  notificationsEnabled: boolean;
}

export interface UseSettingsReturn {
  settings: UserSettings;
  updateSettings: (partial: Partial<UserSettings>) => void;
  saveSettings: () => void;
  resetSettings: () => void;
  hasUnsavedChanges: boolean;
}

const DEFAULT_SETTINGS: UserSettings = {
  theme: 'auto',
  searchDebounceMs: 300,
  notificationsEnabled: false,
};

const STORAGE_KEY = 'userSettings';

const loadSettings = (): UserSettings => {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      const parsed = JSON.parse(stored);
      return { ...DEFAULT_SETTINGS, ...parsed };
    }
  } catch (error) {
    console.error('Failed to load settings from localStorage:', error);
  }
  return DEFAULT_SETTINGS;
};

export const useSettings = (): UseSettingsReturn => {
  const [settings, setSettings] = useState<UserSettings>(loadSettings);
  const [savedSettings, setSavedSettings] = useState<UserSettings>(loadSettings);

  const updateSettings = (partial: Partial<UserSettings>) => {
    setSettings((prev) => ({ ...prev, ...partial }));
  };

  const saveSettings = () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
    setSavedSettings(settings);
  };

  const resetSettings = () => {
    localStorage.removeItem(STORAGE_KEY);
    setSettings(DEFAULT_SETTINGS);
    setSavedSettings(DEFAULT_SETTINGS);
  };

  const hasUnsavedChanges = JSON.stringify(settings) !== JSON.stringify(savedSettings);

  return {
    settings,
    updateSettings,
    saveSettings,
    resetSettings,
    hasUnsavedChanges,
  };
};
