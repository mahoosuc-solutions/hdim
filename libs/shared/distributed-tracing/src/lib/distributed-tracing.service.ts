/**
 * Distributed Tracing Service
 *
 * Provides correlation ID and trace ID management for end-to-end request tracing across services.
 *
 * Features:
 * - Correlation ID generation and propagation
 * - Trace ID generation and propagation
 * - Trace context management per request
 * - HTTP header injection (x-correlation-id, x-trace-id)
 * - Observable streams for trace changes
 * - Nested span support
 * - Trace context in logs
 *
 * Usage:
 * ```typescript
 * constructor(private tracing: DistributedTracingService) {}
 *
 * // Get current correlation ID
 * const correlationId = this.tracing.getCurrentCorrelationId();
 *
 * // Create new trace context
 * const traceContext = this.tracing.createTraceContext();
 *
 * // Make request with tracing headers
 * this.http.get('/api/data', {
 *   headers: this.tracing.getTraceHeaders()
 * }).subscribe(...);
 *
 * // Create nested span
 * this.tracing.startSpan('operation_name').then(span => {
 *   // ... do work ...
 *   span.end();
 * });
 *
 * // Observable for trace changes
 * this.tracing.traceContext$.subscribe(context => {
 *   console.log('Trace ID:', context.traceId);
 * });
 * ```
 */

import { Injectable } from '@angular/core';
import { HttpRequest } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { v4 as uuidv4 } from 'uuid';

export interface TraceContext {
  traceId: string;
  correlationId: string;
  spanId: string;
  parentSpanId?: string;
  startTime: number;
}

export interface TraceSpan {
  spanId: string;
  name: string;
  startTime: number;
  endTime?: number;
  duration?: number;
  attributes: Record<string, any>;
  events: Array<{ name: string; timestamp: number; attributes?: Record<string, any> }>;
  status: 'active' | 'completed' | 'error';
}

@Injectable({
  providedIn: 'root'
})
export class DistributedTracingService {
  private traceContextSubject = new BehaviorSubject<TraceContext | null>(null);
  public readonly traceContext$: Observable<TraceContext | null> = this.traceContextSubject.asObservable();

  private currentTraceContext: TraceContext | null = null;
  private spanStack: TraceSpan[] = [];
  private spanHistory: TraceSpan[] = [];

  constructor() {
    this.initializeTraceContext();
  }

  /**
   * Initialize or get current trace context
   */
  initializeTraceContext(): TraceContext {
    if (!this.currentTraceContext) {
      this.currentTraceContext = this.createTraceContext();
    }
    return this.currentTraceContext;
  }

  /**
   * Create new trace context
   */
  createTraceContext(correlationId?: string, traceId?: string): TraceContext {
    const context: TraceContext = {
      traceId: traceId || uuidv4(),
      correlationId: correlationId || uuidv4(),
      spanId: this.generateSpanId(),
      startTime: Date.now()
    };

    this.currentTraceContext = context;
    this.traceContextSubject.next(context);
    this.spanStack = [];

    return context;
  }

  /**
   * Get current trace context
   */
  getCurrentTraceContext(): TraceContext | null {
    return this.currentTraceContext;
  }

  /**
   * Get current correlation ID
   */
  getCurrentCorrelationId(): string {
    return this.currentTraceContext?.correlationId || uuidv4();
  }

  /**
   * Get current trace ID
   */
  getCurrentTraceId(): string {
    return this.currentTraceContext?.traceId || uuidv4();
  }

  /**
   * Get HTTP headers for trace propagation
   */
  getTraceHeaders(): Record<string, string> {
    const context = this.getCurrentTraceContext();
    const headers: Record<string, string> = {};

    if (context) {
      headers['x-correlation-id'] = context.correlationId;
      headers['x-trace-id'] = context.traceId;
      headers['x-span-id'] = context.spanId;

      if (context.parentSpanId) {
        headers['x-parent-span-id'] = context.parentSpanId;
      }
    }

    return headers;
  }

  /**
   * Add trace headers to HTTP request
   */
  addTraceHeadersToRequest<T>(request: HttpRequest<T>): HttpRequest<T> {
    return request.clone({
      setHeaders: this.getTraceHeaders()
    });
  }

  /**
   * Start a new span
   */
  startSpan(name: string, attributes?: Record<string, any>): TraceSpan {
    const parentSpanId = this.spanStack.length > 0 ? this.spanStack[this.spanStack.length - 1].spanId : undefined;

    const span: TraceSpan = {
      spanId: this.generateSpanId(),
      name,
      startTime: Date.now(),
      attributes: attributes || {},
      events: [],
      status: 'active'
    };

    if (parentSpanId) {
      // Update parent span reference
      const parentSpan = this.spanStack[this.spanStack.length - 1];
      if (parentSpan) {
        span.attributes['parentSpanId'] = parentSpanId;
      }
    }

    this.spanStack.push(span);
    return span;
  }

  /**
   * End current span
   */
  endSpan(status: 'completed' | 'error' = 'completed'): TraceSpan | null {
    if (this.spanStack.length === 0) {
      return null;
    }

    const span = this.spanStack.pop()!;
    span.endTime = Date.now();
    span.duration = span.endTime - span.startTime;
    span.status = status;

    this.spanHistory.push(span);

    return span;
  }

  /**
   * Add event to current span
   */
  addSpanEvent(name: string, attributes?: Record<string, any>): void {
    if (this.spanStack.length === 0) {
      return;
    }

    const span = this.spanStack[this.spanStack.length - 1];
    span.events.push({
      name,
      timestamp: Date.now(),
      attributes
    });
  }

  /**
   * Set span attribute
   */
  setSpanAttribute(key: string, value: any): void {
    if (this.spanStack.length === 0) {
      return;
    }

    const span = this.spanStack[this.spanStack.length - 1];
    span.attributes[key] = value;
  }

  /**
   * Get current span
   */
  getCurrentSpan(): TraceSpan | null {
    return this.spanStack.length > 0 ? this.spanStack[this.spanStack.length - 1] : null;
  }

  /**
   * Get span history
   */
  getSpanHistory(): TraceSpan[] {
    return [...this.spanHistory];
  }

  /**
   * Clear span history
   */
  clearSpanHistory(): void {
    this.spanHistory = [];
  }

  /**
   * Get formatted trace log
   */
  getFormattedTraceLog(): string {
    const context = this.getCurrentTraceContext();
    if (!context) {
      return '';
    }

    let log = `Trace: ${context.traceId}\n`;
    log += `Correlation: ${context.correlationId}\n`;
    log += `Root Span: ${context.spanId}\n`;

    this.spanHistory.forEach((span, index) => {
      const indent = '  '.repeat((span.attributes['parentSpanId'] ? 1 : 0));
      log += `${indent}├─ [${span.spanId}] ${span.name} (${span.duration}ms) - ${span.status}\n`;

      span.events.forEach(event => {
        log += `${indent}│  └─ ${event.name}\n`;
      });
    });

    return log;
  }

  /**
   * Extract trace context from headers
   */
  extractTraceContext(headers: Record<string, string>): Partial<TraceContext> {
    return {
      traceId: headers['x-trace-id'],
      correlationId: headers['x-correlation-id'],
      spanId: headers['x-span-id']
    };
  }

  /**
   * Propagate trace context to nested service call
   */
  propagateContext<T>(request: HttpRequest<T>): HttpRequest<T> {
    return this.addTraceHeadersToRequest(request);
  }

  /**
   * Get trace context for logging
   */
  getLogContext(): Record<string, string> {
    const context = this.getCurrentTraceContext();
    if (!context) {
      return {};
    }

    return {
      traceId: context.traceId,
      correlationId: context.correlationId,
      spanId: context.spanId,
      parentSpanId: context.parentSpanId || 'none'
    };
  }

  private generateSpanId(): string {
    // Shorter span ID (16 chars) for readability
    return uuidv4().substring(0, 16);
  }
}
