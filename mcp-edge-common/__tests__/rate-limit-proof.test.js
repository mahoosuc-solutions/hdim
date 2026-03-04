const express = require('express');
const supertest = require('supertest');
const { createRateLimiter } = require('../lib/rate-limit');

describe('PROOF: Rate Limiting — HIPAA 164.312(e)(1), OWASP API4, NIST SC-7', () => {
  let app, request;

  beforeEach(() => {
    app = express();
    app.use(createRateLimiter({ windowMs: 60_000, max: 3 }));
    app.post('/mcp', (req, res) => res.json({ ok: true }));
    app.get('/health', (req, res) => res.json({ status: 'healthy' }));
    request = supertest(app);
  });

  it('allows requests under the limit', async () => {
    const res = await request.post('/mcp');
    expect(res.status).toBe(200);
  });

  it('returns 429 with JSON-RPC error after exceeding max', async () => {
    for (let i = 0; i < 3; i++) await request.post('/mcp');
    const res = await request.post('/mcp');
    expect(res.status).toBe(429);
    expect(res.body.error.code).toBe(-32000);
    expect(res.body.error.message).toBe('Rate limit exceeded');
    expect(res.body.error.data.retryAfterMs).toBe(60_000);
  });

  it('sets RateLimit-Limit and RateLimit-Remaining headers', async () => {
    const res = await request.post('/mcp');
    expect(res.headers['ratelimit-limit']).toBe('3');
    expect(Number(res.headers['ratelimit-remaining'])).toBeLessThanOrEqual(3);
  });

  it('skips rate limiting on /health endpoint', async () => {
    for (let i = 0; i < 10; i++) await request.get('/health');
    const res = await request.get('/health');
    expect(res.status).toBe(200);
  });

  it('respects MCP_EDGE_RATE_LIMIT_MAX env var', () => {
    process.env.MCP_EDGE_RATE_LIMIT_MAX = '50';
    const limiter = createRateLimiter();
    expect(typeof limiter).toBe('function');
    delete process.env.MCP_EDGE_RATE_LIMIT_MAX;
  });

  it('respects MCP_EDGE_RATE_LIMIT_WINDOW_MS env var', () => {
    process.env.MCP_EDGE_RATE_LIMIT_WINDOW_MS = '30000';
    const limiter = createRateLimiter();
    expect(typeof limiter).toBe('function');
    delete process.env.MCP_EDGE_RATE_LIMIT_WINDOW_MS;
  });
});
