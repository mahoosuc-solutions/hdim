const supertest = require('supertest');
const express = require('express');
const { createMcpRouter } = require('hdim-mcp-edge-common');

// Create a docker client where every method rejects
function createThrowingDockerClient() {
  const thrower = () => { throw new Error('docker connection refused'); };
  return {
    composeFile: 'docker-compose.yml',
    ps: thrower,
    logs: thrower,
    restart: thrower,
    config: thrower
  };
}

describe('devops tool error handling paths', () => {
  let request;

  beforeAll(() => {
    // Ensure demo mode is off so handlers actually execute
    delete process.env.HDIM_DEMO_MODE;

    const throwingClient = createThrowingDockerClient();
    const tools = [
      require('../lib/tools/docker-status').createDefinition(throwingClient),
      require('../lib/tools/docker-logs').createDefinition(throwingClient),
      require('../lib/tools/docker-restart').createDefinition(throwingClient),
      require('../lib/tools/compose-config').createDefinition(throwingClient),
    ];

    const app = express();
    app.use(express.json());
    // No fixturesDir → demo mode never fires; enforceRoleAuth false → no role needed
    app.use(createMcpRouter({
      tools,
      serverName: 'error-test',
      serverVersion: '0.1.0',
      enforceRoleAuth: false
    }));
    request = supertest(app);
  });

  it('docker_status returns error on client failure', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 1, method: 'tools/call',
      params: { name: 'docker_status', arguments: {} }
    });
    const text = JSON.parse(res.body.result.content[0].text);
    expect(text.ok).toBe(false);
    expect(text.error).toBe('docker connection refused');
  });

  it('docker_logs returns error on client failure', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 2, method: 'tools/call',
      params: { name: 'docker_logs', arguments: { service: 'test-svc' } }
    });
    const text = JSON.parse(res.body.result.content[0].text);
    expect(text.ok).toBe(false);
    expect(text.error).toBe('docker connection refused');
  });

  it('docker_restart returns error on client failure', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 3, method: 'tools/call',
      params: { name: 'docker_restart', arguments: { service: 'test-svc' } }
    });
    const text = JSON.parse(res.body.result.content[0].text);
    expect(text.ok).toBe(false);
    expect(text.error).toBe('docker connection refused');
  });

  it('compose_config returns error on client failure', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 4, method: 'tools/call',
      params: { name: 'compose_config', arguments: {} }
    });
    const text = JSON.parse(res.body.result.content[0].text);
    expect(text.ok).toBe(false);
    expect(text.error).toBe('docker connection refused');
  });
});

describe('build_status error handling path', () => {
  let request;

  beforeAll(() => {
    delete process.env.HDIM_DEMO_MODE;

    // Mock runCommand to throw before requiring build-status
    jest.spyOn(
      require('../lib/docker-client'),
      'runCommand'
    ).mockRejectedValue(new Error('npx not found'));

    const tools = [
      require('../lib/tools/build-status').createDefinition()
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

  afterAll(() => {
    jest.restoreAllMocks();
  });

  it('build_status returns error when runCommand fails', async () => {
    const res = await request.post('/mcp').send({
      jsonrpc: '2.0', id: 5, method: 'tools/call',
      params: { name: 'build_status', arguments: {} }
    });
    const text = JSON.parse(res.body.result.content[0].text);
    expect(text.ok).toBe(false);
    expect(text.error).toBe('npx not found');
  });
});
