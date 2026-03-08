'use client';

declare global {
  interface Window {
    plausible?: (eventName: string, options?: { props?: Record<string, string> }) => void;
  }
}

export function trackEvent(eventName: string, props?: Record<string, string>) {
  if (typeof window === 'undefined' || typeof window.plausible !== 'function') {
    return;
  }

  if (props) {
    window.plausible(eventName, { props });
    return;
  }

  window.plausible(eventName);
}
