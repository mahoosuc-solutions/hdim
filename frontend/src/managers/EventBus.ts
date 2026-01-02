/**
 * Event Bus for Inter-Component Communication
 *
 * Provides a centralized pub/sub system for components to communicate
 * without tight coupling. Useful for:
 * - Cross-component actions
 * - Global keyboard shortcuts
 * - Analytics events
 * - System notifications
 */

type EventCallback = (data?: any) => void;
type EventName = string;

interface EventSubscription {
  id: string;
  eventName: EventName;
  callback: EventCallback;
  once: boolean;
}

class EventBusClass {
  private subscriptions: Map<EventName, EventSubscription[]> = new Map();
  private eventHistory: Array<{ name: EventName; timestamp: number; data?: any }> = [];
  private maxHistorySize = 100;

  /**
   * Subscribe to an event
   */
  on(eventName: EventName, callback: EventCallback): () => void {
    const subscription: EventSubscription = {
      id: `${eventName}-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
      eventName,
      callback,
      once: false,
    };

    const subs = this.subscriptions.get(eventName) || [];
    subs.push(subscription);
    this.subscriptions.set(eventName, subs);

    // Return unsubscribe function
    return () => this.off(subscription.id);
  }

  /**
   * Subscribe to an event (one-time only)
   */
  once(eventName: EventName, callback: EventCallback): () => void {
    const subscription: EventSubscription = {
      id: `${eventName}-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
      eventName,
      callback,
      once: true,
    };

    const subs = this.subscriptions.get(eventName) || [];
    subs.push(subscription);
    this.subscriptions.set(eventName, subs);

    return () => this.off(subscription.id);
  }

  /**
   * Unsubscribe from an event
   */
  off(subscriptionId: string): void {
    for (const [eventName, subs] of this.subscriptions.entries()) {
      const filtered = subs.filter((sub) => sub.id !== subscriptionId);

      if (filtered.length === 0) {
        this.subscriptions.delete(eventName);
      } else {
        this.subscriptions.set(eventName, filtered);
      }
    }
  }

  /**
   * Emit an event
   */
  emit(eventName: EventName, data?: any): void {
    const subs = this.subscriptions.get(eventName);

    if (subs) {
      // Call all callbacks
      subs.forEach((sub) => {
        try {
          sub.callback(data);
        } catch (error) {
          console.error(`[EventBus] Error in ${eventName} handler:`, error);
        }
      });

      // Remove one-time subscriptions
      const remaining = subs.filter((sub) => !sub.once);
      if (remaining.length === 0) {
        this.subscriptions.delete(eventName);
      } else {
        this.subscriptions.set(eventName, remaining);
      }
    }

    // Record in history
    this.eventHistory.push({
      name: eventName,
      timestamp: Date.now(),
      data,
    });

    // Trim history if needed
    if (this.eventHistory.length > this.maxHistorySize) {
      this.eventHistory = this.eventHistory.slice(-this.maxHistorySize);
    }

    if (import.meta.env.DEV) {
      console.debug(`[EventBus] Event emitted: ${eventName}`, data);
    }
  }

  /**
   * Get event history
   */
  getHistory(eventName?: EventName): typeof this.eventHistory {
    if (eventName) {
      return this.eventHistory.filter((e) => e.name === eventName);
    }
    return this.eventHistory;
  }

  /**
   * Clear all subscriptions
   */
  clearAll(): void {
    this.subscriptions.clear();
  }

  /**
   * Get subscription count for an event
   */
  getSubscriptionCount(eventName: EventName): number {
    return this.subscriptions.get(eventName)?.length || 0;
  }

  /**
   * Get all event names with subscriptions
   */
  getEventNames(): EventName[] {
    return Array.from(this.subscriptions.keys());
  }
}

// Export singleton
export const EventBus = new EventBusClass();

// ============================================================================
// Predefined Event Names (for type safety)
// ============================================================================

export const EventNames = {
  // UI Events
  OPEN_SETTINGS: 'ui:open-settings',
  CLOSE_ALL_MODALS: 'ui:close-all-modals',
  TOGGLE_SIDEBAR: 'ui:toggle-sidebar',
  FOCUS_SEARCH: 'ui:focus-search',

  // Data Events
  REFRESH_DATA: 'data:refresh',
  EXPORT_STARTED: 'data:export-started',
  EXPORT_COMPLETED: 'data:export-completed',
  EXPORT_FAILED: 'data:export-failed',

  // Evaluation Events
  EVALUATION_SELECTED: 'eval:selected',
  BATCH_SELECTED: 'eval:batch-selected',
  FILTER_CHANGED: 'eval:filter-changed',

  // System Events
  ERROR_OCCURRED: 'system:error',
  WARNING_OCCURRED: 'system:warning',
  CONNECTION_STATUS_CHANGED: 'system:connection-changed',

  // Analytics Events
  PAGE_VIEW: 'analytics:page-view',
  USER_ACTION: 'analytics:user-action',
  FEATURE_USED: 'analytics:feature-used',
} as const;

// ============================================================================
// React Hook
// ============================================================================

import { useEffect, useCallback } from 'react';

/**
 * Hook for subscribing to event bus events
 */
export function useEventBus(eventName: EventName, callback: EventCallback, deps: any[] = []) {
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const memoizedCallback = useCallback(callback, deps);

  useEffect(() => {
    const unsubscribe = EventBus.on(eventName, memoizedCallback);
    return unsubscribe;
  }, [eventName, memoizedCallback]);
}

/**
 * Hook for emitting events
 */
export function useEventEmitter() {
  return useCallback((eventName: EventName, data?: any) => {
    EventBus.emit(eventName, data);
  }, []);
}
