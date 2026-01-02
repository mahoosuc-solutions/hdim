/**
 * Component Lifecycle Manager
 *
 * Provides centralized lifecycle management for components including:
 * - Initialization tracking
 * - Cleanup coordination
 * - Performance monitoring
 * - Error boundaries integration
 * - Memory leak detection
 */

type ComponentId = string;
type ComponentType = string;

interface ComponentRegistration {
  id: ComponentId;
  type: ComponentType;
  mountTime: number;
  unmountTime?: number;
  renderCount: number;
  errorCount: number;
  lastError?: Error;
}

class ComponentLifecycleManagerClass {
  private components: Map<ComponentId, ComponentRegistration> = new Map();
  private cleanupCallbacks: Map<ComponentId, (() => void)[]> = new Map();
  private performanceMarks: Map<ComponentId, number[]> = new Map();

  /**
   * Register a component on mount
   */
  register(id: ComponentId, type: ComponentType): void {
    const existing = this.components.get(id);

    if (existing) {
      // Component remounting - update mount time and increment render count
      existing.mountTime = Date.now();
      existing.renderCount++;
      existing.unmountTime = undefined;
    } else {
      // New component
      this.components.set(id, {
        id,
        type,
        mountTime: Date.now(),
        renderCount: 1,
        errorCount: 0,
      });
    }

    if (import.meta.env.DEV) {
      console.debug(`[Lifecycle] Component mounted: ${type} (${id})`);
    }
  }

  /**
   * Unregister a component on unmount
   */
  unregister(id: ComponentId): void {
    const component = this.components.get(id);

    if (component) {
      component.unmountTime = Date.now();

      const lifetime = component.unmountTime - component.mountTime;

      if (import.meta.env.DEV) {
        console.debug(
          `[Lifecycle] Component unmounted: ${component.type} (${id})`,
          `Lifetime: ${lifetime}ms, Renders: ${component.renderCount}`
        );
      }

      // Execute cleanup callbacks
      this.executeCleanup(id);

      // Remove from active components after a delay (for debugging)
      setTimeout(() => {
        this.components.delete(id);
        this.cleanupCallbacks.delete(id);
        this.performanceMarks.delete(id);
      }, 5000);
    }
  }

  /**
   * Register a cleanup callback for a component
   */
  registerCleanup(id: ComponentId, callback: () => void): void {
    const callbacks = this.cleanupCallbacks.get(id) || [];
    callbacks.push(callback);
    this.cleanupCallbacks.set(id, callbacks);
  }

  /**
   * Execute all cleanup callbacks for a component
   */
  private executeCleanup(id: ComponentId): void {
    const callbacks = this.cleanupCallbacks.get(id);

    if (callbacks) {
      callbacks.forEach((callback) => {
        try {
          callback();
        } catch (error) {
          console.error(`[Lifecycle] Cleanup error for ${id}:`, error);
        }
      });
    }
  }

  /**
   * Track a component render
   */
  trackRender(id: ComponentId): void {
    const component = this.components.get(id);

    if (component) {
      component.renderCount++;
    }
  }

  /**
   * Track a component error
   */
  trackError(id: ComponentId, error: Error): void {
    const component = this.components.get(id);

    if (component) {
      component.errorCount++;
      component.lastError = error;

      console.error(`[Lifecycle] Component error: ${component.type} (${id})`, error);
    }
  }

  /**
   * Mark performance timing
   */
  markPerformance(id: ComponentId, label: string): void {
    const marks = this.performanceMarks.get(id) || [];
    marks.push(performance.now());
    this.performanceMarks.set(id, marks);

    if (import.meta.env.DEV) {
      performance.mark(`${id}-${label}`);
    }
  }

  /**
   * Get component statistics
   */
  getStats(id: ComponentId): ComponentRegistration | undefined {
    return this.components.get(id);
  }

  /**
   * Get all active components
   */
  getActiveComponents(): ComponentRegistration[] {
    return Array.from(this.components.values()).filter(
      (c) => c.unmountTime === undefined
    );
  }

  /**
   * Get performance summary
   */
  getPerformanceSummary(): {
    totalComponents: number;
    activeComponents: number;
    averageRenderCount: number;
    componentsWithErrors: number;
  } {
    const all = Array.from(this.components.values());
    const active = all.filter((c) => c.unmountTime === undefined);
    const withErrors = all.filter((c) => c.errorCount > 0);

    const totalRenders = all.reduce((sum, c) => sum + c.renderCount, 0);
    const averageRenderCount = all.length > 0 ? totalRenders / all.length : 0;

    return {
      totalComponents: all.length,
      activeComponents: active.length,
      averageRenderCount: Math.round(averageRenderCount * 100) / 100,
      componentsWithErrors: withErrors.length,
    };
  }

  /**
   * Check for potential memory leaks
   * (components that have been unmounted for a long time but still registered)
   */
  checkMemoryLeaks(): ComponentRegistration[] {
    const threshold = 60000; // 1 minute
    const now = Date.now();

    return Array.from(this.components.values()).filter((c) => {
      if (c.unmountTime) {
        const timeSinceUnmount = now - c.unmountTime;
        return timeSinceUnmount > threshold;
      }
      return false;
    });
  }

  /**
   * Reset all tracking (useful for testing)
   */
  reset(): void {
    this.components.clear();
    this.cleanupCallbacks.clear();
    this.performanceMarks.clear();
  }
}

// Export singleton instance
export const ComponentLifecycleManager = new ComponentLifecycleManagerClass();

// Export hook for easy use in components
import { useEffect, useRef } from 'react';

export function useComponentLifecycle(componentType: string) {
  const idRef = useRef<string>(`${componentType}-${Math.random().toString(36).substr(2, 9)}`);

  useEffect(() => {
    const id = idRef.current;

    // Register on mount
    ComponentLifecycleManager.register(id, componentType);

    // Unregister on unmount
    return () => {
      ComponentLifecycleManager.unregister(id);
    };
  }, [componentType]);

  return {
    id: idRef.current,
    trackRender: () => ComponentLifecycleManager.trackRender(idRef.current),
    trackError: (error: Error) =>
      ComponentLifecycleManager.trackError(idRef.current, error),
    registerCleanup: (callback: () => void) =>
      ComponentLifecycleManager.registerCleanup(idRef.current, callback),
    markPerformance: (label: string) =>
      ComponentLifecycleManager.markPerformance(idRef.current, label),
  };
}
