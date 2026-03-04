const { createApp } = require('../server');
const supertest = require('supertest');

describe('platform edge security', () => {
  let request;
  beforeAll(() => { request = supertest(createApp()); });

  it('sets X-Content-Type-Options header via helmet', async () => {
    const res = await request.get('/health');
    expect(res.headers['x-content-type-options']).toBe('nosniff');
  });

  it('sets X-Frame-Options header', async () => {
    const res = await request.get('/health');
    expect(res.headers['x-frame-options']).toBeDefined();
  });

  it('rejects request body over 1mb', async () => {
    const largeBody = JSON.stringify({ data: 'x'.repeat(1_100_000) });
    const res = await request.post('/mcp')
      .set('content-type', 'application/json')
      .send(largeBody);
    expect(res.status).toBe(413);
  });

  it('handles malformed JSON gracefully', async () => {
    const res = await request.post('/mcp')
      .set('content-type', 'application/json')
      .send('not json at all');
    expect(res.status).toBe(400);
  });

  describe('rate limiting', () => {
    it('returns RateLimit-* headers on /mcp requests', async () => {
      const res = await request.post('/mcp')
        .set('x-operator-role', 'platform_admin')
        .send({ jsonrpc: '2.0', id: 1, method: 'initialize', params: {} });
      expect(res.headers['ratelimit-limit']).toBeDefined();
      expect(res.headers['ratelimit-remaining']).toBeDefined();
    });
  });
});
