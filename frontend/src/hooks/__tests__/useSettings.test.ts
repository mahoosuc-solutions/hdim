import { renderHook, act } from '@testing-library/react';
import { useSettings } from '../useSettings';
import { describe, it, expect, beforeEach } from 'vitest';

describe('useSettings', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('returns default settings on first use', () => {
    const { result } = renderHook(() => useSettings());

    expect(result.current.settings).toEqual({
      theme: 'auto',
      searchDebounceMs: 300,
      notificationsEnabled: false,
    });
  });

  it('loads settings from localStorage', () => {
    localStorage.setItem('userSettings', JSON.stringify({
      theme: 'dark',
      searchDebounceMs: 500,
      notificationsEnabled: true,
    }));

    const { result } = renderHook(() => useSettings());

    expect(result.current.settings).toEqual({
      theme: 'dark',
      searchDebounceMs: 500,
      notificationsEnabled: true,
    });
  });

  it('updateSettings updates local state', () => {
    const { result } = renderHook(() => useSettings());

    act(() => {
      result.current.updateSettings({ theme: 'light' });
    });

    expect(result.current.settings.theme).toBe('light');
    expect(result.current.settings.searchDebounceMs).toBe(300); // Other values unchanged
  });

  it('saveSettings persists to localStorage', () => {
    const { result } = renderHook(() => useSettings());

    act(() => {
      result.current.updateSettings({ theme: 'dark', notificationsEnabled: true });
    });

    act(() => {
      result.current.saveSettings();
    });

    const saved = JSON.parse(localStorage.getItem('userSettings') || '{}');
    expect(saved).toEqual({
      theme: 'dark',
      searchDebounceMs: 300,
      notificationsEnabled: true,
    });
  });

  it('resetSettings clears localStorage', () => {
    localStorage.setItem('userSettings', JSON.stringify({
      theme: 'dark',
      searchDebounceMs: 1000,
      notificationsEnabled: true,
    }));

    const { result } = renderHook(() => useSettings());

    act(() => {
      result.current.resetSettings();
    });

    expect(localStorage.getItem('userSettings')).toBeNull();
    expect(result.current.settings).toEqual({
      theme: 'auto',
      searchDebounceMs: 300,
      notificationsEnabled: false,
    });
  });

  it('hasUnsavedChanges is false initially', () => {
    const { result } = renderHook(() => useSettings());

    expect(result.current.hasUnsavedChanges).toBe(false);
  });

  it('hasUnsavedChanges is true after update', () => {
    const { result } = renderHook(() => useSettings());

    act(() => {
      result.current.updateSettings({ theme: 'dark' });
    });

    expect(result.current.hasUnsavedChanges).toBe(true);
  });

  it('hasUnsavedChanges is false after save', () => {
    const { result } = renderHook(() => useSettings());

    act(() => {
      result.current.updateSettings({ theme: 'dark' });
    });

    expect(result.current.hasUnsavedChanges).toBe(true);

    act(() => {
      result.current.saveSettings();
    });

    expect(result.current.hasUnsavedChanges).toBe(false);
  });

  it('invalid localStorage handled', () => {
    localStorage.setItem('userSettings', 'invalid json {{{');

    const { result } = renderHook(() => useSettings());

    // Should fall back to defaults
    expect(result.current.settings).toEqual({
      theme: 'auto',
      searchDebounceMs: 300,
      notificationsEnabled: false,
    });
  });

  it('partial updates work correctly', () => {
    const { result } = renderHook(() => useSettings());

    act(() => {
      result.current.updateSettings({ theme: 'dark' });
    });

    expect(result.current.settings.theme).toBe('dark');
    expect(result.current.settings.searchDebounceMs).toBe(300);
    expect(result.current.settings.notificationsEnabled).toBe(false);

    act(() => {
      result.current.updateSettings({ notificationsEnabled: true });
    });

    expect(result.current.settings.theme).toBe('dark');
    expect(result.current.settings.notificationsEnabled).toBe(true);
  });
});
