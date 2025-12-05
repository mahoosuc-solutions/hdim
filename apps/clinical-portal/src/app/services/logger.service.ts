import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';

/**
 * Logger Service - Centralized logging with level control
 *
 * Features:
 * - Multiple log levels (debug, log, warn, error)
 * - Production mode filtering
 * - Structured logging with context
 * - Can be extended to send logs to external service
 */
@Injectable({
  providedIn: 'root',
})
export class LoggerService {
  private isProduction = false; // Set from environment
  private debugEnabled = true;

  constructor() {
    // In real app, get from environment
    // this.isProduction = environment.production;
    // this.debugEnabled = environment.enableDebugLogging;
  }

  /**
   * Debug level logging (only in development)
   */
  debug(message: string, data?: unknown): void {
    if (this.debugEnabled && !this.isProduction) {
      console.debug(`[DEBUG] ${message}`, data !== undefined ? data : '');
    }
  }

  /**
   * Info level logging
   */
  log(message: string, data?: unknown): void {
    console.log(`[INFO] ${message}`, data !== undefined ? data : '');
  }

  /**
   * Warning level logging
   */
  warn(message: string, data?: unknown): void {
    console.warn(`[WARN] ${message}`, data !== undefined ? data : '');
  }

  /**
   * Error level logging
   */
  error(message: string, error?: unknown): void {
    console.error(`[ERROR] ${message}`, error || '');

    // In production, you might want to send errors to a logging service
    if (this.isProduction) {
      this.sendToExternalService('error', message, error);
    }
  }

  /**
   * Log with context (e.g., component name, user action)
   */
  logWithContext(context: string, message: string, data?: unknown): void {
    console.log(`[${context}] ${message}`, data !== undefined ? data : '');
  }

  /**
   * Enable/disable debug logging
   */
  setDebugEnabled(enabled: boolean): void {
    this.debugEnabled = enabled;
  }

  /**
   * Send log to external service (placeholder)
   */
  private sendToExternalService(level: string, message: string, data?: unknown): void {
    // TODO: Implement external logging service integration
    // e.g., Sentry, LogRocket, CloudWatch, etc.
  }
}
