const express = require('express');
const cors = require('cors');
const supertest = require('supertest');
const { createCorsOptions } = require('../lib/cors-config');

describe('PROOF: CORS Lockdown — OWASP API8, NIST SC-7, CIS Headers', () => {
  describe('default origins (no env)', () => {
    let app, request;

    beforeAll(() => {
      delete process.env.MCP_EDGE_CORS_ORIGINS;
      app = express();
      app.use(cors(createCorsOptions()));
      app.post('/mcp', (req, res) => res.json({ ok: true }));
      request = supertest(app);
    });

    it('allows localhost:3100 origin', async () => {
      const res = await request.options('/mcp')
        .set('Origin', 'http://localhost:3100')
        .set('Access-Control-Request-Method', 'POST');
      expect(res.headers['access-control-allow-origin']).toBe('http://localhost:3100');
    });

    it('rejects unknown origin', async () => {
      const res = await request.options('/mcp')
        .set('Origin', 'https://evil.com')
        .set('Access-Control-Request-Method', 'POST');
      expect(res.headers['access-control-allow-origin']).toBeUndefined();
    });

    it('rejects similar-looking origin', async () => {
      const res = await request.options('/mcp')
        .set('Origin', 'http://localhost:3100.evil.com')
        .set('Access-Control-Request-Method', 'POST');
      expect(res.headers['access-control-allow-origin']).toBeUndefined();
    });
  });

  describe('env-configured origins', () => {
    let app, request;

    beforeAll(() => {
      process.env.MCP_EDGE_CORS_ORIGINS = 'https://app.hdim.io,https://admin.hdim.io';
      app = express();
      app.use(cors(createCorsOptions()));
      app.post('/mcp', (req, res) => res.json({ ok: true }));
      request = supertest(app);
    });

    afterAll(() => { delete process.env.MCP_EDGE_CORS_ORIGINS; });

    it('allows configured origin', async () => {
      const res = await request.options('/mcp')
        .set('Origin', 'https://app.hdim.io')
        .set('Access-Control-Request-Method', 'POST');
      expect(res.headers['access-control-allow-origin']).toBe('https://app.hdim.io');
    });

    it('rejects non-configured origin', async () => {
      const res = await request.options('/mcp')
        .set('Origin', 'https://other.hdim.io')
        .set('Access-Control-Request-Method', 'POST');
      expect(res.headers['access-control-allow-origin']).toBeUndefined();
    });
  });

  describe('wildcard dev mode', () => {
    let app, request;

    beforeAll(() => {
      process.env.MCP_EDGE_CORS_ORIGINS = '*';
      app = express();
      app.use(cors(createCorsOptions()));
      app.post('/mcp', (req, res) => res.json({ ok: true }));
      request = supertest(app);
    });

    afterAll(() => { delete process.env.MCP_EDGE_CORS_ORIGINS; });

    it('allows any origin in wildcard mode', async () => {
      const res = await request.options('/mcp')
        .set('Origin', 'https://anything.com')
        .set('Access-Control-Request-Method', 'POST');
      expect(res.headers['access-control-allow-origin']).toBe('*');
    });
  });
});
