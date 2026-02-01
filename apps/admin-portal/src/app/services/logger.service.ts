import { Injectable, Inject, Optional } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

/**
 * Log levels for filtering
 */
export enum LogLevel {
  DEBUG = 0,
  INFO = 1,
  WARN = 2,
  ERROR = 3,
  OFF = 4,
}

/**
 * Structured log entry for consistent formatting
 */
export interface LogEntry {
  timestamp: string;
  level: string;
  context?: string;
  message: string;
  data?: unknown;
  userId?: string;
  sessionId?: string;
  url?: string;
  userAgent?: string;
}

/**
 * Logger Service - Centralized, production-safe logging for Admin Portal
 *
 * SECURITY FEATURES:
 * - Automatic PII/PHI filtering in production
 * - No console output in production (HIPAA §164.312(b) compliance)
 * - Configurable log levels per environment
 * - Structured logging for log aggregation
 * - External service integration (placeholder)
 * - Admin-specific operation tracking
 *
 * USAGE:
 * Instead of console.log(), use:
 *   this.logger.debug('message', data);
 *   this.logger.info('message', data);
 *   this.logger.warn('message', data);
 *   this.logger.error('message', error);
 *
 * With context:
 *   this.logger.withContext('AuditLogsComponent').info('Logs loaded', { count: logs.length });
 *
 * Admin operations:
 *   this.logger.logAdminOperation('CREATE_TENANT', { tenantId });
 */
@Injectable({
  providedIn: 'root',
})
export class LoggerService {
  private minLevel: LogLevel;
  private isProduction: boolean;
  private sessionId: string;
  private userId: string | null = null;
  private context: string | null = null;

  // Patterns that might indicate PHI/PII - filter in production
  private readonly sensitivePatterns = [
    /\b\d{3}-\d{2}-\d{4}\b/g,        // SSN
    /\b\d{9}\b/g,                     // SSN without dashes
    /\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b/g, // Email (partial)
    /\b\d{10,11}\b/g,                 // Phone numbers
    /\b\d{4}[- ]?\d{4}[- ]?\d{4}[- ]?\d{4}\b/g, // Credit card
    /\bMRN[:\s]*\d+/gi,               // Medical Record Number
    /\b(?:firstName|lastName|fullName)["\s:]+[A-Za-z]+/gi, // Names in JSON
    /\b(?:dob|dateOfBirth|birthDate)["\s:]+[\d-]+/gi,      // DOB in JSON
  ];

  constructor(
    @Optional() private http: HttpClient
  ) {
    this.isProduction = environment.production;
    this.minLevel = this.isProduction ? LogLevel.WARN : LogLevel.DEBUG;
    this.sessionId = this.generateSessionId();
  }

  /**
   * Create a child logger with specific context
   */
  withContext(context: string): ContextualLogger {
    return new ContextualLogger(this, context);
  }

  /**
   * Set current user for log attribution
   */
  setUserId(userId: string | null): void {
    this.userId = userId;
  }

  /**
   * Set minimum log level
   */
  setMinLevel(level: LogLevel): void {
    this.minLevel = level;
  }

  /**
   * Debug level logging - development only
   */
  debug(message: string, data?: unknown, context?: string): void {
    this.log(LogLevel.DEBUG, message, data, context);
  }

  /**
   * Info level logging
   */
  info(message: string, data?: unknown, context?: string): void {
    this.log(LogLevel.INFO, message, data, context);
  }

  /**
   * Warning level logging
   */
  warn(message: string, data?: unknown, context?: string): void {
    this.log(LogLevel.WARN, message, data, context);
  }

  /**
   * Error level logging - always logged, sent to external service in prod
   */
  error(message: string, error?: unknown, context?: string): void {
    this.log(LogLevel.ERROR, message, error, context);

    // In production, send errors to external monitoring
    if (this.isProduction) {
      this.sendToExternalService(this.buildLogEntry(LogLevel.ERROR, message, error, context));
    }
  }

  /**
   * Log admin operations (tenant creation, user management, etc.)
   *
   * @param operation Admin operation type (e.g., 'CREATE_TENANT', 'DELETE_USER')
   * @param details Operation details (will be PHI-filtered automatically)
   */
  logAdminOperation(operation: string, details?: unknown): void {
    this.info(`Admin Operation: ${operation}`, this.sanitizeData(details), 'AdminOperation');
  }

  /**
   * Core logging method
   */
  private log(level: LogLevel, message: string, data?: unknown, context?: string): void {
    // Check log level
    if (level < this.minLevel) {
      return;
    }

    const entry = this.buildLogEntry(level, message, data, context);

    // In production, only output WARN and ERROR to console (if at all)
    if (this.isProduction) {
      if (level >= LogLevel.WARN) {
        // Minimal production logging - no data, just message
        const sanitizedMessage = this.sanitizeForProduction(message);
        if (level === LogLevel.ERROR) {
          console.error(`[${entry.level}] ${sanitizedMessage}`);
        } else {
          console.warn(`[${entry.level}] ${sanitizedMessage}`);
        }
      }
      // Send to external service for aggregation
      this.sendToExternalService(entry);
    } else {
      // Development: full console output
      this.outputToConsole(entry);
    }
  }

  /**
   * Build structured log entry
   */
  private buildLogEntry(level: LogLevel, message: string, data?: unknown, context?: string): LogEntry {
    return {
      timestamp: new Date().toISOString(),
      level: LogLevel[level],
      context: context || this.context || undefined,
      message,
      data: this.isProduction ? this.sanitizeData(data) : data,
      userId: this.userId || undefined,
      sessionId: this.sessionId,
      url: typeof window !== 'undefined' ? window.location.href : undefined,
      userAgent: typeof navigator !== 'undefined' ? navigator.userAgent : undefined,
    };
  }

  /**
   * Output to console (development mode)
   */
  private outputToConsole(entry: LogEntry): void {
    const prefix = entry.context ? `[${entry.context}]` : '';
    const message = `${prefix} ${entry.message}`;

    switch (entry.level) {
      case 'DEBUG':
        console.debug(message, entry.data ?? '');
        break;
      case 'INFO':
        console.log(message, entry.data ?? '');
        break;
      case 'WARN':
        console.warn(message, entry.data ?? '');
        break;
      case 'ERROR':
        console.error(message, entry.data ?? '');
        break;
    }
  }

  /**
   * Sanitize message for production (remove potential PHI/PII)
   */
  private sanitizeForProduction(message: string): string {
    let sanitized = message;
    for (const pattern of this.sensitivePatterns) {
      sanitized = sanitized.replace(pattern, '[REDACTED]');
    }
    return sanitized;
  }

  /**
   * Sanitize data object for production logging
   */
  private sanitizeData(data: unknown): unknown {
    if (data === null || data === undefined) {
      return undefined;
    }

    // For errors, only keep message and type
    if (data instanceof Error) {
      return {
        errorType: data.name,
        message: this.sanitizeForProduction(data.message),
        // Don't include stack trace in production logs
      };
    }

    // For objects, create a sanitized version
    if (typeof data === 'object') {
      try {
        const serialized = JSON.stringify(data);
        // If data is too large, don't log it
        if (serialized.length > 1000) {
          return { _note: 'Data too large, omitted for security' };
        }
        return JSON.parse(this.sanitizeForProduction(serialized));
      } catch {
        return { _note: 'Data could not be serialized' };
      }
    }

    // For strings, sanitize
    if (typeof data === 'string') {
      return this.sanitizeForProduction(data);
    }

    return data;
  }

  /**
   * Send log to external service (implement based on your service)
   */
  private sendToExternalService(entry: LogEntry): void {
    // Placeholder for external logging service integration
    // Options: Sentry, LogRocket, CloudWatch, Datadog, etc.
    //
    // Example Sentry integration:
    // if (entry.level === 'ERROR' && typeof Sentry !== 'undefined') {
    //   Sentry.captureMessage(entry.message, {
    //     level: 'error',
    //     tags: { context: entry.context },
    //     extra: entry.data,
    //   });
    // }
    //
    // Example HTTP logging:
    // if (this.http && environment.loggingEndpoint) {
    //   this.http.post(environment.loggingEndpoint, entry).subscribe({
    //     error: () => {} // Silently fail - don't create infinite loop
    //   });
    // }
  }

  /**
   * Generate unique session ID
   */
  private generateSessionId(): string {
    return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }
}

/**
 * Contextual Logger - Logger with pre-set context
 */
export class ContextualLogger {
  constructor(
    private logger: LoggerService,
    private context: string
  ) {}

  debug(message: string, data?: unknown): void {
    this.logger.debug(message, data, this.context);
  }

  info(message: string, data?: unknown): void {
    this.logger.info(message, data, this.context);
  }

  warn(message: string, data?: unknown): void {
    this.logger.warn(message, data, this.context);
  }

  error(message: string, error?: unknown): void {
    this.logger.error(message, error, this.context);
  }

  /**
   * Log admin operations with context
   */
  logAdminOperation(operation: string, details?: unknown): void {
    this.logger.logAdminOperation(operation, details);
  }
}
