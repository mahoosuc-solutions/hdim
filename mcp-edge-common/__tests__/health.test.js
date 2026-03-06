const supertest = require('supertest');
const express = require('express');
const { createHealthRouter } = require('../lib/health');

describe('health', () => {
  describe('createHealthRouter', () => {
    it('returns an express router', () => {
      const router = createHealthRouter({ serviceName: 'test-edge', version: '0.1.0' });
      expect(router).toBeDefined();
      expect(typeof router).toBe('function');
    });
  });
});

describe('health endpoint HTTP behavior', () => {
  it('GET /health returns 200 with required fields', async () => {
    const app = express();
    app.use(createHealthRouter({ serviceName: 'test-svc', version: '1.0.0' }));
    const res = await supertest(app).get('/health');
    expect(res.status).toBe(200);
    expect(res.body).toMatchObject({ status: 'healthy', service: 'test-svc', version: '1.0.0' });
    expect(typeof res.body.uptime).toBe('number');
    expect(res.body.uptime).toBeGreaterThanOrEqual(0);
    expect(res.body.timestamp).toMatch(/^\d{4}-\d{2}-\d{2}T/);
  });

  it('merges statusProvider output into response', async () => {
    const app = express();
    app.use(createHealthRouter({
      serviceName: 'test', version: '1.0.0',
      statusProvider: () => ({ gatewayReachable: true, demoMode: false })
    }));
    const res = await supertest(app).get('/health');
    expect(res.body.gatewayReachable).toBe(true);
    expect(res.body.demoMode).toBe(false);
  });

  it('statusProvider can override status field', async () => {
    const app = express();
    app.use(createHealthRouter({
      serviceName: 'test', version: '1.0.0',
      statusProvider: () => ({ status: 'degraded' })
    }));
    const res = await supertest(app).get('/health');
    expect(res.body.status).toBe('degraded');
  });

  it('returns HTTP 503 when statusProvider reports unhealthy', async () => {
    const app = express();
    app.use(createHealthRouter({
      serviceName: 'test', version: '1.0.0',
      statusProvider: () => ({ status: 'unhealthy' })
    }));
    const res = await supertest(app).get('/health');
    expect(res.status).toBe(503);
    expect(res.body.status).toBe('unhealthy');
  });

  it('returns HTTP 200 for degraded status', async () => {
    const app = express();
    app.use(createHealthRouter({
      serviceName: 'test', version: '1.0.0',
      statusProvider: () => ({ status: 'degraded' })
    }));
    const res = await supertest(app).get('/health');
    expect(res.status).toBe(200);
    expect(res.body.status).toBe('degraded');
  });

  it('runs registered health checks and includes results', async () => {
    const app = express();
    app.use(createHealthRouter({
      serviceName: 'test', version: '1.0.0',
      checks: [
        { name: 'db', fn: () => ({ status: 'ok', latency_ms: 5 }) },
        { name: 'redis', fn: () => ({ status: 'ok' }) }
      ]
    }));
    const res = await supertest(app).get('/health');
    expect(res.status).toBe(200);
    expect(res.body.checks.db).toEqual({ status: 'ok', latency_ms: 5 });
    expect(res.body.checks.redis).toEqual({ status: 'ok' });
  });

  it('sets status to degraded when a health check throws', async () => {
    const app = express();
    app.use(createHealthRouter({
      serviceName: 'test', version: '1.0.0',
      checks: [
        { name: 'db', fn: () => { throw new Error('connection refused'); } }
      ]
    }));
    const res = await supertest(app).get('/health');
    expect(res.status).toBe(200);
    expect(res.body.status).toBe('degraded');
    expect(res.body.checks.db).toEqual({ status: 'error' });
  });
});

describe('/health/ready endpoint', () => {
  it('returns ready:true when healthy', async () => {
    const app = express();
    app.use(createHealthRouter({ serviceName: 'test', version: '1.0.0' }));
    const res = await supertest(app).get('/health/ready');
    expect(res.status).toBe(200);
    expect(res.body.ready).toBe(true);
    expect(res.body.status).toBe('healthy');
  });

  it('returns ready:true when degraded (still serving)', async () => {
    const app = express();
    app.use(createHealthRouter({
      serviceName: 'test', version: '1.0.0',
      statusProvider: () => ({ status: 'degraded' })
    }));
    const res = await supertest(app).get('/health/ready');
    expect(res.status).toBe(200);
    expect(res.body.ready).toBe(true);
  });

  it('returns ready:false and 503 when unhealthy', async () => {
    const app = express();
    app.use(createHealthRouter({
      serviceName: 'test', version: '1.0.0',
      statusProvider: () => ({ status: 'unhealthy' })
    }));
    const res = await supertest(app).get('/health/ready');
    expect(res.status).toBe(503);
    expect(res.body.ready).toBe(false);
  });
});
