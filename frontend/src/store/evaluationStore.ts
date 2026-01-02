/**
 * Zustand store for evaluation events and batch progress
 *
 * Manages:
 * - Real-time batch progress updates
 * - Live event feed
 * - Connection status
 * - Error tracking
 */

import { create } from 'zustand';
import type {
  AnyEvaluationEvent,
  BatchProgressEvent,
  EvaluationCompletedEvent,
  EvaluationFailedEvent
} from '../types/events';
import {
  EventType,
  isBatchProgressEvent,
  isEvaluationCompletedEvent,
  isEvaluationFailedEvent
} from '../types/events';
import { ConnectionStatus } from '../services/websocket.service';

// Event filter types
export interface EventFilters {
  eventTypes: EventType[];
  measureId: string | null;
  statusFilter: 'all' | 'errors' | 'success';
}

// LocalStorage helpers for filter persistence
const STORAGE_KEY = 'eventFilters';

const loadPersistedFilters = (): EventFilters | null => {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      return JSON.parse(stored) as EventFilters;
    }
  } catch (error) {
    console.error('Failed to load persisted filters:', error);
  }
  return null;
};

const persistFilters = (filters: EventFilters): void => {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(filters));
  } catch (error) {
    console.error('Failed to persist filters:', error);
  }
};

interface EvaluationState {
  // Connection state
  connectionStatus: ConnectionStatus;
  lastError: Error | null;

  // Batch progress (keyed by batchId)
  batchProgress: Map<string, BatchProgressEvent>;
  activeBatchId: string | null;

  // Event feed (recent events, max 100)
  recentEvents: AnyEvaluationEvent[];
  maxRecentEvents: number;

  // Statistics
  totalEvaluationsCompleted: number;
  totalEvaluationsFailed: number;
  averageComplianceRate: number;

  // Filter state (lifted from EventFilter component)
  eventFilters: EventFilters;

  // Actions
  setConnectionStatus: (status: ConnectionStatus) => void;
  setError: (error: Error) => void;
  clearError: () => void;
  addEvent: (event: AnyEvaluationEvent) => void;
  updateBatchProgress: (event: BatchProgressEvent) => void;
  setActiveBatch: (batchId: string | null) => void;
  clearBatch: (batchId: string) => void;
  clearAllBatches: () => void;
  reset: () => void;

  // Filter actions
  setEventFilters: (filters: EventFilters) => void;
  updateEventFilter: <K extends keyof EventFilters>(key: K, value: EventFilters[K]) => void;
}

export const useEvaluationStore = create<EvaluationState>((set, get) => ({
  // Initial state
  connectionStatus: ConnectionStatus.DISCONNECTED,
  lastError: null,
  batchProgress: new Map(),
  activeBatchId: null,
  recentEvents: [],
  maxRecentEvents: 100,
  totalEvaluationsCompleted: 0,
  totalEvaluationsFailed: 0,
  averageComplianceRate: 0,
  eventFilters: loadPersistedFilters() || {
    eventTypes: [],
    measureId: null,
    statusFilter: 'all',
  },

  // Set connection status
  setConnectionStatus: (status: ConnectionStatus) => {
    const currentStatus = get().connectionStatus;
    if (currentStatus !== status) {
      set({ connectionStatus: status });
    }
  },

  // Set error
  setError: (error: Error) => {
    set({ lastError: error });
  },

  // Clear error
  clearError: () => {
    set({ lastError: null });
  },

  // Add event to feed and update statistics
  addEvent: (event: AnyEvaluationEvent) => {
    const state = get();

    // Update recent events (keep max 100)
    const recentEvents = [event, ...state.recentEvents].slice(
      0,
      state.maxRecentEvents
    );

    // Update statistics based on event type
    let updates: Partial<EvaluationState> = { recentEvents };

    if (isEvaluationCompletedEvent(event)) {
      const completedEvent = event as EvaluationCompletedEvent;
      const newTotal = state.totalEvaluationsCompleted + 1;

      // Calculate running average compliance rate
      const currentSum = state.averageComplianceRate * state.totalEvaluationsCompleted;
      const newSum = currentSum + (completedEvent.complianceRate * 100);
      const newAverage = newSum / newTotal;

      updates = {
        ...updates,
        totalEvaluationsCompleted: newTotal,
        averageComplianceRate: newAverage
      };
    }

    if (isEvaluationFailedEvent(event)) {
      updates = {
        ...updates,
        totalEvaluationsFailed: state.totalEvaluationsFailed + 1
      };
    }

    // If it's a batch progress event, update batch progress as well
    if (isBatchProgressEvent(event)) {
      get().updateBatchProgress(event as BatchProgressEvent);
    }

    set(updates);
  },

  // Update batch progress
  updateBatchProgress: (event: BatchProgressEvent) => {
    set((state) => {
      const newBatchProgress = new Map(state.batchProgress);

      // Create a new object instead of mutating the incoming event
      const eventToStore: BatchProgressEvent = {
        ...event,
        percentComplete: event.percentComplete ||
          (event.totalPatients > 0 ? (event.completedCount / event.totalPatients) * 100 : 0)
      };

      newBatchProgress.set(event.batchId, eventToStore);

      return {
        batchProgress: newBatchProgress,
        // Auto-set active batch if none is set
        activeBatchId: state.activeBatchId || event.batchId
      };
    });
  },

  // Set active batch
  setActiveBatch: (batchId: string | null) => {
    set({ activeBatchId: batchId });
  },

  // Clear a specific batch
  clearBatch: (batchId: string) => {
    set((state) => {
      const newBatchProgress = new Map(state.batchProgress);
      newBatchProgress.delete(batchId);

      return {
        batchProgress: newBatchProgress,
        activeBatchId:
          state.activeBatchId === batchId ? null : state.activeBatchId
      };
    });
  },

  // Clear all batches
  clearAllBatches: () => {
    set({
      batchProgress: new Map(),
      activeBatchId: null
    });
  },

  // Reset entire store
  reset: () => {
    set({
      connectionStatus: ConnectionStatus.DISCONNECTED,
      lastError: null,
      batchProgress: new Map(),
      activeBatchId: null,
      recentEvents: [],
      totalEvaluationsCompleted: 0,
      totalEvaluationsFailed: 0,
      averageComplianceRate: 0
    });
  },

  // Set event filters (with localStorage persistence)
  setEventFilters: (filters: EventFilters) => {
    persistFilters(filters);
    set({ eventFilters: filters });
  },

  // Update a single filter property (with localStorage persistence)
  updateEventFilter: <K extends keyof EventFilters>(key: K, value: EventFilters[K]) => {
    const newFilters = { ...get().eventFilters, [key]: value };
    persistFilters(newFilters);
    set({ eventFilters: newFilters });
  },
}));

/**
 * Selectors for derived state
 */

// Get active batch progress
export const selectActiveBatchProgress = (state: EvaluationState): BatchProgressEvent | null => {
  if (!state.activeBatchId) return null;
  return state.batchProgress.get(state.activeBatchId) || null;
};

// Cache for memoized selectors
let cachedBatches: BatchProgressEvent[] | null = null;
let cachedBatchesMap: Map<string, BatchProgressEvent> | null = null;

// Get all batches as array (memoized to prevent new array on every call)
export const selectAllBatches = (state: EvaluationState): BatchProgressEvent[] => {
  // Return cached array if Map reference hasn't changed
  if (cachedBatchesMap === state.batchProgress && cachedBatches !== null) {
    return cachedBatches;
  }

  // Update cache
  cachedBatchesMap = state.batchProgress;
  cachedBatches = Array.from(state.batchProgress.values());
  return cachedBatches;
};

// Get recent events by type
export const selectEventsByType = (state: EvaluationState, eventType: EventType): AnyEvaluationEvent[] => {
  return state.recentEvents.filter(event => event.eventType === eventType);
};

// Get connection status indicator
export const selectConnectionIndicator = (state: EvaluationState): {
  status: ConnectionStatus;
  color: string;
  text: string;
} => {
  const { connectionStatus } = state;

  switch (connectionStatus) {
    case ConnectionStatus.CONNECTED:
      return { status: connectionStatus, color: 'success', text: 'Connected' };
    case ConnectionStatus.CONNECTING:
      return { status: connectionStatus, color: 'info', text: 'Connecting...' };
    case ConnectionStatus.RECONNECTING:
      return { status: connectionStatus, color: 'warning', text: 'Reconnecting...' };
    case ConnectionStatus.DISCONNECTED:
      return { status: connectionStatus, color: 'default', text: 'Disconnected' };
    case ConnectionStatus.ERROR:
      return { status: connectionStatus, color: 'error', text: 'Connection Error' };
    default:
      return { status: connectionStatus, color: 'default', text: 'Unknown' };
  }
};

// Get batch completion percentage
export const selectBatchCompletionPercentage = (batchId: string) => (
  state: EvaluationState
): number => {
  const batch = state.batchProgress.get(batchId);
  if (!batch || batch.totalPatients === 0) return 0;
  return (batch.completedCount / batch.totalPatients) * 100;
};

// Get overall success rate
export const selectOverallSuccessRate = (state: EvaluationState): number => {
  const total = state.totalEvaluationsCompleted + state.totalEvaluationsFailed;
  if (total === 0) return 100;
  return (state.totalEvaluationsCompleted / total) * 100;
};

// Cache for available measures
let cachedMeasures: string[] | null = null;
let cachedMeasuresEvents: AnyEvaluationEvent[] | null = null;

// Get available measures from recent events (memoized)
export const selectAvailableMeasures = (state: EvaluationState): string[] => {
  // Return cached array if recentEvents reference hasn't changed
  if (cachedMeasuresEvents === state.recentEvents && cachedMeasures !== null) {
    return cachedMeasures;
  }

  const measures = new Set<string>();
  state.recentEvents.forEach(event => {
    if ('measureId' in event && event.measureId) {
      measures.add(event.measureId);
    }
  });

  cachedMeasuresEvents = state.recentEvents;
  cachedMeasures = Array.from(measures);
  return cachedMeasures;
};
