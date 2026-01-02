/**
 * Unified UI State Management Store
 *
 * Centralizes all UI-related state including:
 * - Modal visibility
 * - Panel states
 * - Loading states
 * - Active selections
 * - View preferences
 * - Notifications/toasts
 *
 * This provides a single source of truth for UI state
 * and ensures consistent behavior across components.
 */

import { create } from 'zustand';
import { persist } from 'zustand/middleware';

// ============================================================================
// Types
// ============================================================================

export interface ToastNotification {
  id: string;
  message: string;
  type: 'success' | 'error' | 'warning' | 'info';
  duration?: number;
  timestamp: number;
}

export interface ViewPreferences {
  chartType: 'line' | 'bar' | 'area';
  timeRange: '1h' | '6h' | '24h' | '7d' | '30d' | 'all';
  refreshInterval: number; // milliseconds, 0 = disabled
  compactMode: boolean;
  showGridLines: boolean;
  animationsEnabled: boolean;
}

export interface PanelVisibility {
  settingsPanel: boolean;
  keyboardShortcutsPanel: boolean;
  eventDetailsModal: boolean;
  batchComparisonDialog: boolean;
  advancedExportDialog: boolean;
  analyticsPanel: boolean;
}

export interface LoadingStates {
  loadingEvents: boolean;
  loadingBatches: boolean;
  exportingData: boolean;
  savingSettings: boolean;
}

export interface ActiveSelections {
  selectedEventId: string | null;
  selectedBatchIds: string[];
  activeMeasureId: string | null;
  activeTimeRange: string;
}

// ============================================================================
// Store Interface
// ============================================================================

interface UIState {
  // Panel Visibility
  panelVisibility: PanelVisibility;
  openPanel: (panelName: keyof PanelVisibility) => void;
  closePanel: (panelName: keyof PanelVisibility) => void;
  togglePanel: (panelName: keyof PanelVisibility) => void;
  closeAllPanels: () => void;

  // Loading States
  loadingStates: LoadingStates;
  setLoading: (key: keyof LoadingStates, isLoading: boolean) => void;
  setLoadingMultiple: (states: Partial<LoadingStates>) => void;

  // Active Selections
  activeSelections: ActiveSelections;
  selectEvent: (eventId: string | null) => void;
  selectBatch: (batchId: string) => void;
  deselectBatch: (batchId: string) => void;
  clearBatchSelection: () => void;
  selectMeasure: (measureId: string | null) => void;
  setTimeRange: (range: string) => void;

  // View Preferences
  viewPreferences: ViewPreferences;
  updateViewPreference: <K extends keyof ViewPreferences>(
    key: K,
    value: ViewPreferences[K]
  ) => void;
  updateViewPreferences: (prefs: Partial<ViewPreferences>) => void;
  resetViewPreferences: () => void;

  // Toast Notifications
  toasts: ToastNotification[];
  addToast: (
    message: string,
    type: ToastNotification['type'],
    duration?: number
  ) => string;
  removeToast: (id: string) => void;
  clearAllToasts: () => void;

  // Global UI State
  sidebarExpanded: boolean;
  toggleSidebar: () => void;
  fullscreenMode: boolean;
  toggleFullscreen: () => void;

  // Focus management
  focusedComponent: string | null;
  setFocus: (componentId: string | null) => void;

  // Reset
  reset: () => void;
}

// ============================================================================
// Default Values
// ============================================================================

const defaultPanelVisibility: PanelVisibility = {
  settingsPanel: false,
  keyboardShortcutsPanel: false,
  eventDetailsModal: false,
  batchComparisonDialog: false,
  advancedExportDialog: false,
  analyticsPanel: false,
};

const defaultLoadingStates: LoadingStates = {
  loadingEvents: false,
  loadingBatches: false,
  exportingData: false,
  savingSettings: false,
};

const defaultActiveSelections: ActiveSelections = {
  selectedEventId: null,
  selectedBatchIds: [],
  activeMeasureId: null,
  activeTimeRange: '24h',
};

const defaultViewPreferences: ViewPreferences = {
  chartType: 'line',
  timeRange: '24h',
  refreshInterval: 5000, // 5 seconds
  compactMode: false,
  showGridLines: true,
  animationsEnabled: true,
};

// ============================================================================
// Store Implementation
// ============================================================================

export const useUIStore = create<UIState>()(
  persist(
    (set, get) => ({
      // ======================================================================
      // Panel Visibility
      // ======================================================================
      panelVisibility: defaultPanelVisibility,

      openPanel: (panelName) =>
        set((state) => ({
          panelVisibility: {
            ...state.panelVisibility,
            [panelName]: true,
          },
        })),

      closePanel: (panelName) =>
        set((state) => ({
          panelVisibility: {
            ...state.panelVisibility,
            [panelName]: false,
          },
        })),

      togglePanel: (panelName) =>
        set((state) => ({
          panelVisibility: {
            ...state.panelVisibility,
            [panelName]: !state.panelVisibility[panelName],
          },
        })),

      closeAllPanels: () =>
        set({
          panelVisibility: defaultPanelVisibility,
        }),

      // ======================================================================
      // Loading States
      // ======================================================================
      loadingStates: defaultLoadingStates,

      setLoading: (key, isLoading) =>
        set((state) => ({
          loadingStates: {
            ...state.loadingStates,
            [key]: isLoading,
          },
        })),

      setLoadingMultiple: (states) =>
        set((state) => ({
          loadingStates: {
            ...state.loadingStates,
            ...states,
          },
        })),

      // ======================================================================
      // Active Selections
      // ======================================================================
      activeSelections: defaultActiveSelections,

      selectEvent: (eventId) =>
        set((state) => ({
          activeSelections: {
            ...state.activeSelections,
            selectedEventId: eventId,
          },
          // Auto-open details modal when event selected
          panelVisibility: {
            ...state.panelVisibility,
            eventDetailsModal: eventId !== null,
          },
        })),

      selectBatch: (batchId) =>
        set((state) => {
          const selectedBatchIds = state.activeSelections.selectedBatchIds;
          if (!selectedBatchIds.includes(batchId)) {
            return {
              activeSelections: {
                ...state.activeSelections,
                selectedBatchIds: [...selectedBatchIds, batchId],
              },
            };
          }
          return state;
        }),

      deselectBatch: (batchId) =>
        set((state) => ({
          activeSelections: {
            ...state.activeSelections,
            selectedBatchIds: state.activeSelections.selectedBatchIds.filter(
              (id) => id !== batchId
            ),
          },
        })),

      clearBatchSelection: () =>
        set((state) => ({
          activeSelections: {
            ...state.activeSelections,
            selectedBatchIds: [],
          },
        })),

      selectMeasure: (measureId) =>
        set((state) => ({
          activeSelections: {
            ...state.activeSelections,
            activeMeasureId: measureId,
          },
        })),

      setTimeRange: (range) =>
        set((state) => ({
          activeSelections: {
            ...state.activeSelections,
            activeTimeRange: range,
          },
        })),

      // ======================================================================
      // View Preferences
      // ======================================================================
      viewPreferences: defaultViewPreferences,

      updateViewPreference: (key, value) =>
        set((state) => ({
          viewPreferences: {
            ...state.viewPreferences,
            [key]: value,
          },
        })),

      updateViewPreferences: (prefs) =>
        set((state) => ({
          viewPreferences: {
            ...state.viewPreferences,
            ...prefs,
          },
        })),

      resetViewPreferences: () =>
        set({
          viewPreferences: defaultViewPreferences,
        }),

      // ======================================================================
      // Toast Notifications
      // ======================================================================
      toasts: [],

      addToast: (message, type, duration = 5000) => {
        const id = `toast-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
        const toast: ToastNotification = {
          id,
          message,
          type,
          duration,
          timestamp: Date.now(),
        };

        set((state) => ({
          toasts: [...state.toasts, toast],
        }));

        // Auto-remove after duration
        if (duration > 0) {
          setTimeout(() => {
            get().removeToast(id);
          }, duration);
        }

        return id;
      },

      removeToast: (id) =>
        set((state) => ({
          toasts: state.toasts.filter((toast) => toast.id !== id),
        })),

      clearAllToasts: () =>
        set({
          toasts: [],
        }),

      // ======================================================================
      // Global UI State
      // ======================================================================
      sidebarExpanded: false,

      toggleSidebar: () =>
        set((state) => ({
          sidebarExpanded: !state.sidebarExpanded,
        })),

      fullscreenMode: false,

      toggleFullscreen: () =>
        set((state) => {
          const newFullscreenMode = !state.fullscreenMode;

          // Request/exit fullscreen API
          if (newFullscreenMode) {
            document.documentElement.requestFullscreen?.();
          } else {
            document.exitFullscreen?.();
          }

          return {
            fullscreenMode: newFullscreenMode,
          };
        }),

      // ======================================================================
      // Focus Management
      // ======================================================================
      focusedComponent: null,

      setFocus: (componentId) =>
        set({
          focusedComponent: componentId,
        }),

      // ======================================================================
      // Reset
      // ======================================================================
      reset: () =>
        set({
          panelVisibility: defaultPanelVisibility,
          loadingStates: defaultLoadingStates,
          activeSelections: defaultActiveSelections,
          viewPreferences: defaultViewPreferences,
          toasts: [],
          sidebarExpanded: false,
          fullscreenMode: false,
          focusedComponent: null,
        }),
    }),
    {
      name: 'ui-storage', // localStorage key
      partialize: (state) => ({
        // Only persist these keys
        viewPreferences: state.viewPreferences,
        sidebarExpanded: state.sidebarExpanded,
      }),
    }
  )
);

// ============================================================================
// Selectors
// ============================================================================

export const selectPanelVisibility = (state: UIState) => state.panelVisibility;
export const selectIsAnyPanelOpen = (state: UIState) =>
  Object.values(state.panelVisibility).some((isOpen) => isOpen);
export const selectLoadingStates = (state: UIState) => state.loadingStates;
export const selectIsAnyLoading = (state: UIState) =>
  Object.values(state.loadingStates).some((isLoading) => isLoading);
export const selectActiveSelections = (state: UIState) => state.activeSelections;
export const selectViewPreferences = (state: UIState) => state.viewPreferences;
export const selectToasts = (state: UIState) => state.toasts;
export const selectSelectedBatchIds = (state: UIState) =>
  state.activeSelections.selectedBatchIds;
export const selectSelectedEventId = (state: UIState) =>
  state.activeSelections.selectedEventId;

// ============================================================================
// Hooks for Common Operations
// ============================================================================

/**
 * Hook for managing panel visibility
 */
export const usePanelManager = (panelName: keyof PanelVisibility) => {
  const isOpen = useUIStore((state) => state.panelVisibility[panelName]);
  const open = useUIStore((state) => state.openPanel);
  const close = useUIStore((state) => state.closePanel);
  const toggle = useUIStore((state) => state.togglePanel);

  return {
    isOpen,
    open: () => open(panelName),
    close: () => close(panelName),
    toggle: () => toggle(panelName),
  };
};

/**
 * Hook for managing loading states
 */
export const useLoadingManager = (key: keyof LoadingStates) => {
  const isLoading = useUIStore((state) => state.loadingStates[key]);
  const setLoading = useUIStore((state) => state.setLoading);

  return {
    isLoading,
    startLoading: () => setLoading(key, true),
    stopLoading: () => setLoading(key, false),
    setLoading: (loading: boolean) => setLoading(key, loading),
  };
};

/**
 * Hook for toast notifications
 */
export const useToast = () => {
  const addToast = useUIStore((state) => state.addToast);

  return {
    success: (message: string, duration?: number) =>
      addToast(message, 'success', duration),
    error: (message: string, duration?: number) =>
      addToast(message, 'error', duration),
    warning: (message: string, duration?: number) =>
      addToast(message, 'warning', duration),
    info: (message: string, duration?: number) =>
      addToast(message, 'info', duration),
  };
};
