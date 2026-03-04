const supertest = require('supertest');
const express = require('express');
const { createMcpRouter } = require('hdim-mcp-edge-common');

describe('service_catalog health check error paths', () => {
  let request;

  beforeAll(() => {
    delete process.env.HDIM_DEMO_MODE;

    // Platform client where get() throws for every health check
    const throwingClient = {
      get: () => { throw new Error('ECONNREFUSED'); }
    };

    const tools = [
      require('../lib/tools/service-catalog').createDefinition(throwingClient)
    ];

    const app = express();
    app.use(express.json());
    app.use(createMcpRouter({
      tools,
      serverName: 'error-test',
      serverVersion: '0.1.0',
      enforceRoleAuth: false
    }));
    request = supertest(app);
  });

  it('services with health endpoints are marked unreachable on client error', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 1, method: 'tools/call',
      params: { name: 'service_catalog', arguments: {} }
    });
    const body = JSON.parse(res.body.result.content[0].text);
    expect(body.services).toBeDefined();
    expect(body.checkedAt).toBeDefined();

    // Services with healthPath should be unreachable
    const withHealth = body.services.filter((s) => s.healthPath);
    expect(withHealth.length).toBeGreaterThan(0);
    for (const svc of withHealth) {
      expect(svc.status).toBe('unreachable');
      expect(svc.error).toBe('ECONNREFUSED');
    }

    // Services without healthPath should be unknown
    const noHealth = body.services.filter((s) => !s.healthPath);
    expect(noHealth.length).toBeGreaterThan(0);
    for (const svc of noHealth) {
      expect(svc.status).toBe('unknown');
    }
  });

  it('service_catalog healthy response path when client succeeds', async () => {
    // Create a client that returns ok: true
    const okClient = {
      get: () => ({ ok: true, status: 200 })
    };

    const tools = [
      require('../lib/tools/service-catalog').createDefinition(okClient)
    ];

    const app = express();
    app.use(express.json());
    app.use(createMcpRouter({
      tools,
      serverName: 'ok-test',
      serverVersion: '0.1.0',
      enforceRoleAuth: false
    }));
    const okRequest = supertest(app);

    const res = await okRequest.post('/mcp').send({
      jsonrpc: '2.0', id: 2, method: 'tools/call',
      params: { name: 'service_catalog', arguments: {} }
    });
    const body = JSON.parse(res.body.result.content[0].text);
    const withHealth = body.services.filter((s) => s.healthPath);
    for (const svc of withHealth) {
      expect(svc.status).toBe('healthy');
      expect(svc.httpStatus).toBe(200);
    }
  });

  it('service_catalog unhealthy response path when client returns not ok', async () => {
    const failClient = {
      get: () => ({ ok: false, status: 503 })
    };

    const tools = [
      require('../lib/tools/service-catalog').createDefinition(failClient)
    ];

    const app = express();
    app.use(express.json());
    app.use(createMcpRouter({
      tools,
      serverName: 'fail-test',
      serverVersion: '0.1.0',
      enforceRoleAuth: false
    }));
    const failRequest = supertest(app);

    const res = await failRequest.post('/mcp').send({
      jsonrpc: '2.0', id: 3, method: 'tools/call',
      params: { name: 'service_catalog', arguments: {} }
    });
    const body = JSON.parse(res.body.result.content[0].text);
    const withHealth = body.services.filter((s) => s.healthPath);
    for (const svc of withHealth) {
      expect(svc.status).toBe('unhealthy');
      expect(svc.httpStatus).toBe(503);
    }
  });
});
