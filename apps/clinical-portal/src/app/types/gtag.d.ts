/**
 * Type declarations for Google Analytics gtag
 */

declare global {
  interface Window {
    gtag?: (
      command: 'config' | 'event' | 'set' | 'get',
      targetId: string | Date,
      config?: Record<string, any>
    ) => void;
  }

  function gtag(
    command: 'config' | 'event' | 'set' | 'get',
    targetId: string | Date,
    config?: Record<string, any>
  ): void;
}

export {};
