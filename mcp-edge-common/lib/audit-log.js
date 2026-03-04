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
    base: { service: serviceName },
    timestamp: pino.stdTimeFunctions.isoTime,
    formatters: {
      level(label) { return { level: label }; }
    }
  };

  return stream ? pino(options, stream) : pino(options);
}

module.exports = { createAuditLogger, scrubSensitive };
