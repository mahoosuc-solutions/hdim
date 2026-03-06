const pino = require('pino');

const SENSITIVE_KEYS = ['patient_id', 'ssn', 'mrn', 'password', 'secret', 'api_key'];

function scrubSensitive(value) {
  if (typeof value === 'string') {
    return value.replace(/Bearer\s+\S+/g, 'Bearer [REDACTED]');
  }
  if (value && typeof value === 'object' && !Array.isArray(value)) {
    const copy = { ...value };
    for (const key of SENSITIVE_KEYS) {
      if (key in copy) copy[key] = '[REDACTED]';
    }
    return copy;
  }
  return value;
}

function createAuditLogger({ serviceName, stream } = {}) {
  const options = {
    name: serviceName || 'mcp-edge',
    level: process.env.LOG_LEVEL || 'info',
    base: {
      'service.name': serviceName,
      // ECS compat: keep legacy 'service' for backwards compat
      service: serviceName
    },
    timestamp: pino.stdTimeFunctions.isoTime,
    formatters: {
      level(label) { return { level: label }; }
    },
    mixin() {
      return { 'ecs.version': '1.6.0' };
    }
  };

  return stream ? pino(options, stream) : pino(options);
}

/**
 * Create a child logger with trace context fields (ECS-compatible).
 * Use this in request handlers to correlate logs with distributed traces.
 */
function withTraceContext(logger, traceContext) {
  if (!logger || !traceContext) return logger;
  return logger.child({
    'trace.id': traceContext.traceId,
    'span.id': traceContext.spanId,
    ...(traceContext.parentSpanId ? { 'parent.id': traceContext.parentSpanId } : {})
  });
}

module.exports = { createAuditLogger, scrubSensitive, withTraceContext };
