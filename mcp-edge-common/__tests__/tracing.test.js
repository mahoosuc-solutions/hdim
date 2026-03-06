const express = require('express');
const request = require('supertest');
const {
  parseTraceparent,
  generateSpanId,
  generateTraceId,
  buildTraceparent,
  traceContextMiddleware,
  propagationHeaders
} = require('../lib/tracing');

describe('tracing', () => {
  describe('parseTraceparent', () => {
    it('parses valid W3C traceparent', () => {
      const result = parseTraceparent('00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01');
      expect(result).toEqual({
        traceId: '4bf92f3577b34da6a3ce929d0e0e4736',
        parentSpanId: '00f067aa0ba902b7',
        traceFlags: '01'
      });
    });

    it('returns null for invalid format', () => {
      expect(parseTraceparent('invalid')).toBeNull();
      expect(parseTraceparent('00-short-id-01')).toBeNull();
    });

    it('returns null for null/undefined', () => {
      expect(parseTraceparent(null)).toBeNull();
      expect(parseTraceparent(undefined)).toBeNull();
    });

    it('rejects version other than 00', () => {
      expect(parseTraceparent('01-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01')).toBeNull();
    });
  });

  describe('generateSpanId', () => {
    it('returns 16-char hex string', () => {
      const id = generateSpanId();
      expect(id).toMatch(/^[0-9a-f]{16}$/);
    });

    it('generates unique values', () => {
      const ids = new Set(Array.from({ length: 100 }, () => generateSpanId()));
      expect(ids.size).toBe(100);
    });
  });

  describe('generateTraceId', () => {
    it('returns 32-char hex string', () => {
      const id = generateTraceId();
      expect(id).toMatch(/^[0-9a-f]{32}$/);
    });
  });

  describe('buildTraceparent', () => {
    it('builds valid W3C traceparent string', () => {
      const result = buildTraceparent('4bf92f3577b34da6a3ce929d0e0e4736', '00f067aa0ba902b7', '01');
      expect(result).toBe('00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01');
    });

    it('defaults to flags 01 (sampled)', () => {
      const result = buildTraceparent('a'.repeat(32), 'b'.repeat(16));
      expect(result).toMatch(/-01$/);
    });
  });

  describe('traceContextMiddleware', () => {
    let app;

    beforeEach(() => {
      app = express();
      app.use(traceContextMiddleware());
      app.get('/test', (req, res) => res.json(req.traceContext));
    });

    it('creates new trace context when no traceparent header', async () => {
      const res = await request(app).get('/test');
      expect(res.body.traceId).toMatch(/^[0-9a-f]{32}$/);
      expect(res.body.spanId).toMatch(/^[0-9a-f]{16}$/);
      expect(res.body.parentSpanId).toBeNull();
      expect(res.body.traceFlags).toBe('01');
      expect(res.body.traceparent).toMatch(/^00-[0-9a-f]{32}-[0-9a-f]{16}-01$/);
    });

    it('propagates existing trace context', async () => {
      const traceId = 'a'.repeat(32);
      const parentSpan = 'b'.repeat(16);
      const res = await request(app)
        .get('/test')
        .set('traceparent', `00-${traceId}-${parentSpan}-01`);

      expect(res.body.traceId).toBe(traceId);
      expect(res.body.parentSpanId).toBe(parentSpan);
      expect(res.body.spanId).not.toBe(parentSpan); // new span
      expect(res.body.traceFlags).toBe('01');
    });

    it('preserves tracestate header', async () => {
      const res = await request(app)
        .get('/test')
        .set('traceparent', `00-${'a'.repeat(32)}-${'b'.repeat(16)}-01`)
        .set('tracestate', 'vendor=value');

      expect(res.body.tracestate).toBe('vendor=value');
    });
  });

  describe('propagationHeaders', () => {
    it('returns traceparent header', () => {
      const ctx = { traceparent: '00-abc-def-01', tracestate: '' };
      const headers = propagationHeaders(ctx);
      expect(headers.traceparent).toBe('00-abc-def-01');
      expect(headers.tracestate).toBeUndefined();
    });

    it('includes tracestate when present', () => {
      const ctx = { traceparent: '00-abc-def-01', tracestate: 'vendor=value' };
      const headers = propagationHeaders(ctx);
      expect(headers.tracestate).toBe('vendor=value');
    });

    it('returns empty object for null context', () => {
      expect(propagationHeaders(null)).toEqual({});
    });
  });
});
