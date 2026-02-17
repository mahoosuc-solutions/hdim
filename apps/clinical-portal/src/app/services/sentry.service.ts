import { Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { environment } from '../../environments/environment';

type SentryLike = {
  init?: (config: Record<string, unknown>) => void;
  captureException?: (error: unknown, context?: Record<string, unknown>) => void;
  captureMessage?: (message: string, context?: Record<string, unknown>) => void;
  setTag?: (key: string, value: string) => void;
};

/**
 * Sentry adapter with PHI-safe sanitization.
 * Uses global window.Sentry when SDK is available at runtime.
 */
@Injectable({
  providedIn: 'root',
})
export class SentryService {
  private initialized = false;

  initialize(): void {
    if (this.initialized || !this.isEnabled()) return;

    const sentry = this.getSentry();
    if (!sentry?.init) return;

    sentry.init({
      dsn: environment.errorReporting?.dsn,
      environment: environment.errorReporting?.environment || 'unknown',
    });
    this.initialized = true;
  }

  captureException(error: unknown, context: Record<string, unknown> = {}): void {
    if (!this.isEnabled()) return;
    this.initialize();

    const sentry = this.getSentry();
    if (sentry?.captureException) {
      sentry.captureException(error, { extra: this.sanitizeObject(context) });
    }
  }

  captureHttpError(error: HttpErrorResponse, context: Record<string, unknown> = {}): void {
    this.captureException(error, {
      ...context,
      status: error.status,
      statusText: error.statusText,
      url: error.url,
    });
  }

  captureMessage(message: string, context: Record<string, unknown> = {}): void {
    if (!this.isEnabled()) return;
    this.initialize();

    const sentry = this.getSentry();
    if (sentry?.captureMessage) {
      sentry.captureMessage(this.sanitizeText(message), { extra: this.sanitizeObject(context) });
    }
  }

  private isEnabled(): boolean {
    return !!(
      environment.production &&
      environment.features?.enableErrorReporting &&
      environment.errorReporting?.dsn
    );
  }

  private getSentry(): SentryLike | undefined {
    return (window as unknown as { Sentry?: SentryLike }).Sentry;
  }

  private sanitizeObject(input: Record<string, unknown>): Record<string, unknown> {
    const output: Record<string, unknown> = {};
    for (const [key, value] of Object.entries(input)) {
      if (value == null) {
        output[key] = value;
        continue;
      }
      if (typeof value === 'string') {
        output[key] = this.sanitizeText(value);
      } else {
        output[key] = value;
      }
    }
    return output;
  }

  private sanitizeText(text: string): string {
    return text
      .replace(/\b\d{3}-\d{2}-\d{4}\b/g, '[REDACTED]')
      .replace(/\b\d{9}\b/g, '[REDACTED]')
      .replace(/\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b/g, '[REDACTED]');
  }
}
