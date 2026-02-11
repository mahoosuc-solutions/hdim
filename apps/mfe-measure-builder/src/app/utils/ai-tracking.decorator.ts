/**
 * AI Tracking Decorator
 *
 * Automatically tracks user interactions for AI analysis
 *
 * Usage:
 * @TrackInteraction('component-name', 'action-description')
 * methodName() { ... }
 */

import { inject } from '@angular/core';
import { AIAssistantService } from '../services/ai-assistant.service';

/**
 * Method decorator to track user interactions
 *
 * @param component - Component name (e.g., 'dashboard', 'patients')
 * @param action - Action description (e.g., 'view-data', 'create-evaluation')
 *
 * @example
 * ```typescript
 * export class PatientsComponent {
 *   @TrackInteraction('patients', 'view-list')
 *   loadPatients() {
 *     // Your existing code
 *   }
 *
 *   @TrackInteraction('patients', 'create-patient')
 *   createPatient(data: Patient) {
 *     // Your existing code
 *   }
 * }
 * ```
 */
export function TrackInteraction(component: string, action: string) {
  return function (
    target: any,
    propertyKey: string,
    descriptor: PropertyDescriptor
  ) {
    const originalMethod = descriptor.value;

    descriptor.value = async function (...args: any[]) {
      const startTime = Date.now();
      let success = true;
      let errorMessage: string | undefined;
      const self = this;

      const trackCompletion = () => {
        const duration = Date.now() - startTime;

        // Get AI service instance (if available)
        try {
          // Access the AIAssistantService if it exists
          const aiService = (self as any).aiAssistant as AIAssistantService | undefined;

          if (aiService && typeof aiService.trackInteraction === 'function') {
            aiService.trackInteraction({
              component,
              action,
              duration,
              success,
              errorMessage,
              context: {
                method: propertyKey,
                args: args.length,
              },
            });
          }
        } catch (e) {
          // Silently fail if AI service not available
          // This allows optional AI tracking without breaking functionality
        }
      };

      try {
        // Execute the original method
        const result = originalMethod.apply(this, args);

        // Handle promises
        if (result instanceof Promise) {
          try {
            const promiseResult = await result;
            return promiseResult;
          } catch (error: any) {
            success = false;
            errorMessage = error?.message || 'Unknown error';
            throw error;
          } finally {
            trackCompletion();
          }
        } else {
          trackCompletion();
          return result;
        }
      } catch (error: any) {
        success = false;
        errorMessage = error?.message || 'Unknown error';
        trackCompletion();
        throw error;
      }
    };

    return descriptor;
  };
}

/**
 * Class decorator to automatically inject AIAssistantService
 *
 * @example
 * ```typescript
 * @WithAITracking
 * export class PatientsComponent {
 *   // aiAssistant will be automatically injected
 *
 *   @TrackInteraction('patients', 'load-data')
 *   loadPatients() { ... }
 * }
 * ```
 */
export function WithAITracking<T extends { new (...args: any[]): {} }>(
  constructor: T
) {
  return class extends constructor {
    aiAssistant = inject(AIAssistantService);
  };
}

/**
 * Standalone tracking function for manual use
 *
 * @example
 * ```typescript
 * export class CustomComponent {
 *   constructor(private aiAssistant: AIAssistantService) {}
 *
 *   async performAction() {
 *     const tracker = createInteractionTracker(
 *       this.aiAssistant,
 *       'custom-component',
 *       'perform-action'
 *     );
 *
 *     try {
 *       await someAsyncOperation();
 *       tracker.complete(true);
 *     } catch (error) {
 *       tracker.complete(false, error.message);
 *       throw error;
 *     }
 *   }
 * }
 * ```
 */
export function createInteractionTracker(
  aiService: AIAssistantService,
  component: string,
  action: string,
  context?: Record<string, any>
) {
  const startTime = Date.now();

  return {
    complete(success: boolean, errorMessage?: string) {
      const duration = Date.now() - startTime;
      aiService.trackInteraction({
        component,
        action,
        duration,
        success,
        errorMessage,
        context,
      });
    },
  };
}

/**
 * Simple tracking for click events (for use in templates)
 *
 * @example
 * ```html
 * <button (click)="trackClick('dashboard', 'refresh-data'); loadData()">
 *   Refresh
 * </button>
 * ```
 */
export function trackClick(
  aiService: AIAssistantService,
  component: string,
  action: string
) {
  aiService.trackInteraction({
    component,
    action,
    success: true,
  });
}
