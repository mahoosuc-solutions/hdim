// mcp-edge-common/lib/tracing.js
// W3C Trace Context propagation for MCP Edge sidecars.
// Extracts traceparent/tracestate from incoming requests and forwards them on outbound calls.
const crypto = require('node:crypto');

const TRACEPARENT_REGEX = /^00-([0-9a-f]{32})-([0-9a-f]{16})-([0-9a-f]{2})$/;

function parseTraceparent(header) {
  if (!header) return null;
  const match = header.match(TRACEPARENT_REGEX);
  if (!match) return null;
  return { traceId: match[1], parentSpanId: match[2], traceFlags: match[3] };
}

function generateSpanId() {
  return crypto.randomBytes(8).toString('hex');
}

function generateTraceId() {
  return crypto.randomBytes(16).toString('hex');
}

function buildTraceparent(traceId, spanId, flags = '01') {
  return `00-${traceId}-${spanId}-${flags}`;
}

/**
 * Express middleware that extracts or creates trace context.
 * Sets req.traceContext for downstream use.
 */
function traceContextMiddleware() {
  return (req, _res, next) => {
    const incoming = parseTraceparent(req.headers?.traceparent);
    const spanId = generateSpanId();

    if (incoming) {
      req.traceContext = {
        traceId: incoming.traceId,
        parentSpanId: incoming.parentSpanId,
        spanId,
        traceFlags: incoming.traceFlags,
        tracestate: req.headers?.tracestate || '',
        traceparent: buildTraceparent(incoming.traceId, spanId, incoming.traceFlags)
      };
    } else {
      const traceId = generateTraceId();
      req.traceContext = {
        traceId,
        parentSpanId: null,
        spanId,
        traceFlags: '01',
        tracestate: '',
        traceparent: buildTraceparent(traceId, spanId, '01')
      };
    }

    next();
  };
}

/**
 * Returns headers to forward trace context on outbound HTTP calls.
 */
function propagationHeaders(traceContext) {
  if (!traceContext) return {};
  const headers = { traceparent: traceContext.traceparent };
  if (traceContext.tracestate) {
    headers.tracestate = traceContext.tracestate;
  }
  return headers;
}

module.exports = {
  parseTraceparent,
  generateSpanId,
  generateTraceId,
  buildTraceparent,
  traceContextMiddleware,
  propagationHeaders
};
