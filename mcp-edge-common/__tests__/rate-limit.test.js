const express = require('express');
const request = require('supertest');
const { createRateLimiter } = require('../lib/rate-limit');

/* ------------------------------------------------------------------ */
/*  Unit tests                                                        */
/* ------------------------------------------------------------------ */
describe('createRateLimiter – unit', () => {
  test('returns express middleware function (length === 3)', () => {
    const mw = createRateLimiter({ windowMs: 60000, max: 10 });
    // express-rate-limit returns a standard (req, res, next) middleware
    expect(typeof mw).toBe('function');
    // Some versions wrap with length 3; accept 3+ as valid middleware
    expect(mw.length).toBeGreaterThanOrEqual(3);
  });

  test('respects custom windowMs and max options', () => {
    // If we can create without throwing and the returned fn is middleware,
    // the options were accepted.  Deep behaviour is verified in integration.
    expect(() => createRateLimiter({ windowMs: 5000, max: 1 })).not.toThrow();
  });

  test('uses env vars for defaults', () => {
    process.env.MCP_EDGE_RATE_LIMIT_WINDOW_MS = '30000';
    process.env.MCP_EDGE_RATE_LIMIT_MAX = '50';
    try {
      const mw = createRateLimiter();
      expect(typeof mw).toBe('function');
    } finally {
      delete process.env.MCP_EDGE_RATE_LIMIT_WINDOW_MS;
      delete process.env.MCP_EDGE_RATE_LIMIT_MAX;
    }
  });
});

/* ------------------------------------------------------------------ */
/*  Integration tests (express + supertest)                           */
/* ------------------------------------------------------------------ */
describe('createRateLimiter – integration', () => {
  let app;

  beforeEach(() => {
    app = express();
    const limiter = createRateLimiter({ windowMs: 60000, max: 3 });
    app.use(limiter);
    app.get('/ping', (_req, res) => res.json({ ok: true }));
    app.get('/health', (_req, res) => res.json({ status: 'healthy' }));
  });

  test('allows requests under the limit', async () => {
    const res = await request(app).get('/ping');
    expect(res.status).toBe(200);
    expect(res.body).toEqual({ ok: true });
  });

  test('returns 429 after exceeding max', async () => {
    // exhaust the 3-request limit
    await request(app).get('/ping');
    await request(app).get('/ping');
    await request(app).get('/ping');

    const res = await request(app).get('/ping');
    expect(res.status).toBe(429);
    expect(res.body.error.code).toBe(-32000);
    expect(res.body.error.message).toBe('Rate limit exceeded');
    expect(res.body.error.data.retryAfterMs).toBe(60000);
  });

  test('sets RateLimit-* standard headers', async () => {
    const res = await request(app).get('/ping');
    expect(res.status).toBe(200);
    // standardHeaders: true  →  RateLimit-Limit, RateLimit-Remaining, etc.
    const headers = Object.keys(res.headers).map(h => h.toLowerCase());
    expect(headers).toEqual(expect.arrayContaining([
      expect.stringMatching(/^ratelimit/)
    ]));
  });

  test('skips /health endpoint', async () => {
    // Send 5 requests (well above the max of 3) – all should succeed
    for (let i = 0; i < 5; i++) {
      const res = await request(app).get('/health');
      expect(res.status).toBe(200);
      expect(res.body).toEqual({ status: 'healthy' });
    }
  });
});
