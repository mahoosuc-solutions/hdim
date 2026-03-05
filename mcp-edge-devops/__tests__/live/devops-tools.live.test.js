const {
  isDockerReachable,
  callTool
} = require('hdim-mcp-edge-common/lib/live-test-helpers');

let request;
let dockerReachable = false;

beforeAll(async () => {
  dockerReachable = await isDockerReachable();
  if (!dockerReachable) {
    console.warn('Docker unreachable — skipping devops live tests');
    return;
  }

  process.env.HDIM_DEMO_MODE = 'false';
  jest.resetModules();
  const { createApp } = require('../../server');
  const supertest = require('supertest');
  request = supertest(createApp());
});

afterAll(() => {
  delete process.env.HDIM_DEMO_MODE;
});

function liveIt(name, fn) {
  it(name, async () => {
    if (!dockerReachable) return;
    await fn();
  });
}

describe('devops tools — live Docker', () => {
  liveIt('docker_status reports running containers', async () => {
    const res = await callTool(request, 'docker_status');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data).toBeDefined();
  });

  liveIt('docker_logs retrieves logs from a container', async () => {
    const statusRes = await callTool(request, 'docker_status');
    const statusData = JSON.parse(statusRes.body.result.content[0].text);
    const containers = statusData.containers || statusData.data?.containers || [];
    if (containers.length === 0) {
      console.warn('No containers found — skipping log test');
      return;
    }
    const containerName = containers[0].name || containers[0].Names;
    const res = await callTool(request, 'docker_logs', { containerName, lines: 10 });
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data).toBeDefined();
  });

  liveIt('service_dependencies returns dependency graph', async () => {
    const res = await callTool(request, 'service_dependencies');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data).toBeDefined();
  });

  liveIt('compose_config reads docker-compose configuration', async () => {
    const res = await callTool(request, 'compose_config');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data).toBeDefined();
  });

  liveIt('build_status reports container image info', async () => {
    const res = await callTool(request, 'build_status');
    expect(res.status).toBe(200);
    const data = JSON.parse(res.body.result.content[0].text);
    expect(data).toBeDefined();
  });
});
